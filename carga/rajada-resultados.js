// TC-01: rajada de resultados, com reenvios de propósito.
//
// HIPÓTESE (sem dado do JIF): os resultados chegam em rajada no fim das rodadas.
// Este teste reproduz essa hipótese. Os limites (thresholds) são provisórios:
// devem ser revistos depois da primeira execução, que serve de linha de base.
//
// Uso:
//   k6 run -e BASE=http://localhost:8080 carga/rajada-resultados.js
//   k6 run -e BASE=http://<ip-do-nó> -e CHAVES=40 -e TAXA=50 -e DURACAO=60s carga/rajada-resultados.js
//
// Ao final, o teste espera a fila esvaziar e confere no Gateway:
//   perdidos  = eventos aceitos (202) que não aparecem no banco ou ficaram pendentes;
//   duplicatas = eventos aplicados mais de uma vez + chaves cuja tabela diverge do histórico.

import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const SAIDA = __ENV.SAIDA || 'carga/resultados';   // pasta dos arquivos de resultado
const CHAVES = parseInt(__ENV.CHAVES || '30');
const TAXA = parseInt(__ENV.TAXA || '40');            // resultados por segundo
const DURACAO = __ENV.DURACAO || '60s';
const REPETIR = parseFloat(__ENV.REPETIR || '0.10');  // fração de reenvios do mesmo evento
const ESPERA_FINAL_S = parseInt(__ENV.ESPERA_FINAL_S || '120');
const RODADA = __ENV.RODADA || String(Date.now());
// Prefixo dos identificadores de chaves e de eventos. RODADA nomeia os arquivos de saída; ID_EXTRA (opcional)
// entra só nos identificadores. Quem repete uma rodada com o mesmo nome (k3s-tf02, por exemplo) precisa de um
// ID_EXTRA novo, senão os eventos já existem no banco, voltam como "novo=false" e a rajada quase não aplica nada.
const ID = __ENV.ID_EXTRA ? `${RODADA}-${__ENV.ID_EXTRA}` : RODADA;
const MODALIDADES = ['FUTSAL', 'HANDEBOL', 'VOLEIBOL'];
const JSON_H = { headers: { 'Content-Type': 'application/json' } };

const aceitos = new Counter('eventos_aceitos_novos');
const reenvios = new Counter('reenvios_enviados');
const reenviosReconhecidos = new Counter('reenvios_reconhecidos');
const jaExistiam = new Counter('eventos_ja_existentes_sem_reenvio');
const encontrados = new Counter('eventos_encontrados_no_banco');
const pendentesNoFim = new Counter('eventos_pendentes_no_fim');
const semResposta = new Counter('conferencias_sem_resposta');
const tempoAteAplicar = new Trend('tempo_ate_esvaziar_ms', true);

export const options = {
  scenarios: {
    rajada: {
      executor: 'constant-arrival-rate',
      rate: TAXA,
      timeUnit: '1s',
      duration: DURACAO,
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
  },
  setupTimeout: '120s',
  teardownTimeout: `${ESPERA_FINAL_S + 600}s`,
  thresholds: {
    // Provisórios. A meta de consistência (zero perdidos, zero duplicatas) é verificada no teardown.
    'http_reqs{tipo:registro}': ['count>0'],
    'http_req_failed{tipo:registro}': ['rate<0.01'],
    'http_req_duration{tipo:registro}': ['p(95)<500'],
    'checks{fase:consistencia}': ['rate==1'],
  },
};

function placarAleatorio(modalidade) {
  if (modalidade === 'VOLEIBOL') {
    const vence = Math.random() < 0.5;
    const tres = Math.random() < 0.4;
    const sets = vence ? [[25, 20], [25, 18]] : [[20, 25], [18, 25]];
    if (tres) {
      sets[1] = vence ? [18, 25] : [25, 18];
      sets.push(vence ? [15, 12] : [12, 15]);
    }
    return { parciais: sets };
  }
  const max = modalidade === 'HANDEBOL' ? 35 : 8;
  return {
    placarMandante: Math.floor(Math.random() * max),
    placarVisitante: Math.floor(Math.random() * max),
  };
}

export function setup() {
  const antes = http.get(`${BASE}/api/consistencia`).json();
  const partidas = [];
  for (let i = 0; i < CHAVES; i++) {
    const modalidade = MODALIDADES[i % MODALIDADES.length];
    const equipes = [];
    const n = 3 + (i % 3); // chaves de 3, 4 e 5 equipes
    for (let e = 0; e < n; e++) {
      equipes.push(`IF${String.fromCharCode(65 + e)}${i}`);
    }
    const r = http.post(`${BASE}/api/chaves`, JSON.stringify({
      id: `k6-${ID}-${i}`, nome: `Carga ${i}`, modalidade, equipes,
    }), JSON_H);
    check(r, { 'chave criada': (x) => x.status === 201 });
    const jogos = http.get(`${BASE}/api/chaves/k6-${ID}-${i}/partidas`).json();
    for (const j of jogos) {
      partidas.push({ id: j.id, modalidade });
    }
  }
  return { partidas, antes };
}

export default function (dados) {
  const it = exec.scenario.iterationInTest;
  const repetir = it > 0 && Math.random() < REPETIR;
  // O reenvio usa o identificador de uma iteração anterior: deve voltar com novo=false.
  const numero = repetir ? Math.floor(Math.random() * it) : it;
  const eventoId = `k6-${ID}-e${numero}`;
  const partida = dados.partidas[numero % dados.partidas.length];
  const corpo = Object.assign({ eventoId, partidaId: partida.id }, placarAleatorio(partida.modalidade));
  const r = http.post(`${BASE}/api/resultados`, JSON.stringify(corpo), Object.assign({ tags: { tipo: 'registro' } }, JSON_H));
  const ok = check(r, { 'resultado aceito (202)': (x) => x.status === 202 });
  if (!ok) {
    return;
  }
  const recibo = r.json();
  if (recibo.novo === true) {
    aceitos.add(1);
  }
  if (!repetir && recibo.novo === false) {
    // Um evento que não é reenvio deveria ser novo. Se não é, o identificador já existia de outra execução.
    jaExistiam.add(1);
  }
  if (repetir) {
    reenvios.add(1);
    if (recibo.novo === false) {
      reenviosReconhecidos.add(1);
    }
  }
}

export function teardown(dados) {
  // Espera a fila esvaziar: nenhum evento pendente.
  const inicio = Date.now();
  let k = null;
  while ((Date.now() - inicio) / 1000 < ESPERA_FINAL_S) {
    k = http.get(`${BASE}/api/consistencia`).json();
    if (k.pendentes === 0) {
      break;
    }
    sleep(1);
  }
  tempoAteAplicar.add(Date.now() - inicio);

  // Confere evento por evento o que esta rodada enviou.
  // No teardown o k6 não informa quantas iterações rodaram. Os identificadores vão de e0 até
  // eN, então varremos até um teto folgado: os que não existem voltam 404 e não contam.
  const segundos = parseInt(DURACAO) * (DURACAO.endsWith('m') ? 60 : 1);
  const total = Math.ceil(TAXA * segundos * 1.2) + 50;
  let achados = 0;
  let pendentes = 0;
  let inconclusivas = 0;
  for (let i = 0; i < total; i++) {
    const url = `${BASE}/api/eventos/k6-${ID}-e${i}`;
    const opcoes = { tags: { tipo: 'conferencia' }, timeout: '10s', responseCallback: http.expectedStatuses(200, 404) };
    let r = http.get(url, opcoes);
    // Status 0 é erro de rede ou timeout: a pergunta ficou sem resposta e isso não prova que o evento sumiu.
    // Na medição k3s-tf03 uma consulta travou durante um soluço do Gateway e foi contada como evento perdido.
    // Agora o k6 pergunta de novo, até 3 vezes, e só então desiste (e conta à parte).
    for (let tentativa = 1; tentativa < 4 && r.status === 0; tentativa++) {
      sleep(2);
      r = http.get(url, opcoes);
    }
    if (r.status === 0) {
      inconclusivas++;
    } else if (r.status === 200) {
      achados++;
      if (r.json().estado === 'PENDENTE') {
        pendentes++;
      }
    }
  }
  encontrados.add(achados);
  pendentesNoFim.add(pendentes);
  semResposta.add(inconclusivas);

  console.log(`consistencia antes: ${JSON.stringify(dados.antes)}`);
  console.log(`consistencia depois: ${JSON.stringify(k)}`);
  check(k, {
    'nenhum evento pendente no fim': (x) => x !== null && x.pendentes === 0,
    'nenhum evento aplicado mais de uma vez': (x) => x !== null && x.aplicadosMaisDeUmaVez === 0,
    'nenhuma chave com tabela divergente': (x) => x !== null && x.chavesInconsistentes === 0,
  }, { fase: 'consistencia' });
}

export function handleSummary(data) {
  const m = (nome) => (data.metrics[nome] ? data.metrics[nome].values : {});
  const aceitosN = m('eventos_aceitos_novos').count || 0;
  const achadosN = m('eventos_encontrados_no_banco').count || 0;
  const jaExistentesN = m('eventos_ja_existentes_sem_reenvio').count || 0;
  const linhas = [
    'TC-01 rajada de resultados',
    `rodada: ${RODADA}  base: ${BASE}  taxa: ${TAXA}/s  duração: ${DURACAO}  chaves: ${CHAVES}`,
    `identificadores: k6-${ID}-e*`,
    `registros enviados: ${m('http_reqs{tipo:registro}').count || 0}`,
    `eventos novos aceitos (202): ${aceitosN}`,
    `registros que não eram reenvio e voltaram como já existentes (deve ser 0): ${jaExistentesN}`,
    `eventos encontrados no banco: ${achadosN}`,
    `perdidos (aceitos e não encontrados): ${Math.max(0, aceitosN - achadosN)}`,
    `conferências sem resposta, mesmo com 3 novas tentativas (entram em perdidos): ${m('conferencias_sem_resposta').count || 0}`,
    `pendentes no fim: ${m('eventos_pendentes_no_fim').count || 0}`,
    `reenvios enviados: ${m('reenvios_enviados').count || 0}, reconhecidos como repetidos: ${m('reenvios_reconhecidos').count || 0}`,
    `latência do registro p95: ${(m('http_req_duration{tipo:registro}')['p(95)'] || 0).toFixed(1)} ms`,
    `tempo até a fila esvaziar depois da rajada: ${(m('tempo_ate_esvaziar_ms').max || 0).toFixed(0)} ms`,
    `checks de consistência: ${((m('checks{fase:consistencia}').rate || 0) * 100).toFixed(0)}%`,
  ];
  if (jaExistentesN > 0) {
    linhas.push('AVISO: identificadores reaproveitados de outra execução. Esta rodada NÃO vale como teste de carga: use outro ID_EXTRA.');
  }
  return {
    stdout: linhas.join('\n') + '\n',
    [`${SAIDA}/rajada-${RODADA}.json`]: JSON.stringify(data, null, 2),
    [`${SAIDA}/rajada-${RODADA}.txt`]: linhas.join('\n') + '\n',
  };
}
