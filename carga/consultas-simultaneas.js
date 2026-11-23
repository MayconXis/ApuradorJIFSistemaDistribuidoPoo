// TC-02: muitas pessoas consultando a classificação ao mesmo tempo.
//
// HIPÓTESE (sem dado do JIF): muita gente consulta a tabela no fim das rodadas.
// Metade das consultas manda a versão que já tem (If-None-Match), como a tela faz
// no polling; a outra metade pede a tabela inteira. Limites provisórios.
//
// Uso (depois de existir ao menos uma chave, por exemplo depois do TC-01):
//   k6 run -e BASE=http://localhost:8080 -e USUARIOS=200 carga/consultas-simultaneas.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const SAIDA = __ENV.SAIDA || 'carga/resultados';   // pasta dos arquivos de resultado
const USUARIOS = parseInt(__ENV.USUARIOS || '200');
const nao304 = new Counter('respostas_com_corpo');
const com304 = new Counter('respostas_304');

export const options = {
  scenarios: {
    consultas: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: USUARIOS },
        { duration: '60s', target: USUARIOS },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_failed{tipo:consulta}': ['rate<0.01'],
    'http_req_duration{tipo:consulta}': ['p(95)<300'],
  },
};

export function setup() {
  const chaves = http.get(`${BASE}/api/chaves`).json();
  if (chaves.length === 0) {
    throw new Error('Nenhuma chave cadastrada. Rode antes o TC-01 ou crie uma chave pela interface.');
  }
  return { ids: chaves.map((c) => c.id), versoes: Object.fromEntries(chaves.map((c) => [c.id, c.versao])) };
}

export default function (dados) {
  const id = dados.ids[Math.floor(Math.random() * dados.ids.length)];
  const params = { tags: { tipo: 'consulta' }, responseCallback: http.expectedStatuses(200, 304) };
  if (Math.random() < 0.5) {
    params.headers = { 'If-None-Match': `"${dados.versoes[id]}"` };
  }
  const r = http.get(`${BASE}/api/chaves/${id}/classificacao`, params);
  check(r, { 'consulta respondida': (x) => x.status === 200 || x.status === 304 });
  if (r.status === 304) {
    com304.add(1);
  } else {
    nao304.add(1);
  }
  sleep(2); // a tela consulta a cada 2 segundos
}

export function handleSummary(data) {
  const v = (n) => (data.metrics[n] ? data.metrics[n].values : {});
  const linhas = [
    'TC-02 consultas simultâneas',
    `base: ${BASE}  usuários virtuais no pico: ${USUARIOS}`,
    `consultas: ${v('http_reqs').count || 0}  (com corpo: ${v('respostas_com_corpo').count || 0}, 304: ${v('respostas_304').count || 0})`,
    `latência p50: ${(v('http_req_duration{tipo:consulta}').med || 0).toFixed(1)} ms`,
    `latência p95: ${(v('http_req_duration{tipo:consulta}')['p(95)'] || 0).toFixed(1)} ms`,
    `falhas: ${((v('http_req_failed{tipo:consulta}').rate || 0) * 100).toFixed(2)}%`,
  ];
  const marca = Date.now();
  return {
    stdout: linhas.join('\n') + '\n',
    [`${SAIDA}/consultas-${marca}.json`]: JSON.stringify(data, null, 2),
    [`${SAIDA}/consultas-${marca}.txt`]: linhas.join('\n') + '\n',
  };
}
