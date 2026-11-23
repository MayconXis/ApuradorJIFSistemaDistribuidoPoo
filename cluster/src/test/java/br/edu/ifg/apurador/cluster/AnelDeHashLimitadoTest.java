package br.edu.ifg.apurador.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifg.apurador.cluster.anel.AnelDeHashLimitado;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TA-01. Algoritmo de balanceamento: anel de hash com carga limitada (RNF-04, RNF-05, RNF-07). */
@DisplayName("TA-01 Balanceamento: hash consistente com carga limitada")
class AnelDeHashLimitadoTest {

    private static AnelDeHashLimitado anelCom(int workers, int pontos, double folga) {
        AnelDeHashLimitado anel = new AnelDeHashLimitado(pontos, folga);
        for (int i = 0; i < workers; i++) {
            anel.adicionarWorker("worker-" + i);
        }
        return anel;
    }

    private static int maiorCarga(AnelDeHashLimitado anel) {
        int maior = 0;
        for (String worker : anel.workers()) {
            maior = Math.max(maior, anel.cargaDe(worker));
        }
        return maior;
    }

    /** Confere o que tem de valer depois de qualquer operação. */
    private static void conferirInvariantes(AnelDeHashLimitado anel, int chavesEsperadas) {
        Map<String, Integer> contagem = new HashMap<>();
        for (String dono : anel.atribuicoes().values()) {
            assertTrue(anel.workers().contains(dono), "dono fora do anel: " + dono);
            contagem.merge(dono, 1, Integer::sum);
        }
        for (String worker : anel.workers()) {
            assertEquals(contagem.getOrDefault(worker, 0).intValue(), anel.cargaDe(worker), "carga de " + worker);
            assertTrue(anel.cargaDe(worker) <= anel.teto(), worker + " acima do teto: " + anel.cargaDe(worker) + " > " + anel.teto());
        }
        assertEquals(chavesEsperadas, anel.totalDeChaves());
        if (!anel.workers().isEmpty()) {
            assertEquals(chavesEsperadas, anel.atribuicoes().size(), "com worker vivo, toda chave tem dono");
        }
    }

    @Test
    @DisplayName("RNF-04: a mesma chave cai sempre no mesmo worker")
    void mesmaChaveMesmoWorker() {
        AnelDeHashLimitado anel = anelCom(3, 64, 0.25);
        String dono = anel.donoDe("chave-A");
        for (int i = 0; i < 100; i++) {
            anel.donoDe("outra-" + i);
            assertEquals(dono, anel.donoDe("chave-A"));
        }
    }

    @Test
    @DisplayName("RNF-04: o round-robin espalharia os eventos da mesma chave, o anel não")
    void contrasteComRoundRobin() {
        List<String> workers = new ArrayList<>(anelCom(3, 64, 0.25).workers());
        AnelDeHashLimitado anel = anelCom(3, 64, 0.25);
        Set<String> pelosDoisModos = new HashSet<>();
        Set<String> peloAnel = new HashSet<>();
        for (int evento = 0; evento < 6; evento++) {
            pelosDoisModos.add(workers.get(evento % workers.size()));
            peloAnel.add(anel.donoDe("chave-A"));
        }
        assertEquals(3, pelosDoisModos.size(), "round-robin: os 6 eventos da chave A passam por 3 workers");
        assertEquals(1, peloAnel.size(), "anel: os 6 eventos da chave A vão a um worker só");
    }

    @Test
    @DisplayName("RNF-05: com folga de 25%, nenhum worker passa do teto")
    void nenhumWorkerAcimaDoTeto() {
        AnelDeHashLimitado anel = anelCom(3, 64, 0.25);
        for (int i = 0; i < 30; i++) {
            assertNotNull(anel.donoDe("chave-" + i));
            conferirInvariantes(anel, i + 1);
        }
        assertEquals(13, anel.teto(), "teto = arredonda para cima (1,25 x 30 / 3)");
        assertTrue(maiorCarga(anel) <= 13);
    }

    @Test
    @DisplayName("RNF-05: com folga zero, a chave pula o worker cheio e a divisão fica exata")
    void folgaZeroForcaOPulo() {
        AnelDeHashLimitado anel = anelCom(3, 64, 0.0);
        for (int i = 0; i < 30; i++) {
            anel.donoDe("chave-" + i);
        }
        for (String worker : anel.workers()) {
            assertEquals(10, anel.cargaDe(worker), worker);
        }
    }

    @Test
    @DisplayName("RNF-07: a saída de um worker move só as chaves dele")
    void saidaMoveSoAsChavesDoWorker() {
        AnelDeHashLimitado anel = anelCom(3, 64, 0.25);
        for (int i = 0; i < 30; i++) {
            anel.donoDe("chave-" + i);
        }
        Map<String, String> antes = new LinkedHashMap<>(anel.atribuicoes());
        List<String> doWorker1 = anel.chavesDe("worker-1");

        Map<String, String> movidas = anel.removerWorker("worker-1");

        assertEquals(new HashSet<>(doWorker1), movidas.keySet(), "só as chaves do worker-1 mudaram");
        for (Map.Entry<String, String> entrada : antes.entrySet()) {
            if (!entrada.getValue().equals("worker-1")) {
                assertEquals(entrada.getValue(), anel.donoAtual(entrada.getKey()), "não devia mudar: " + entrada.getKey());
            }
        }
        conferirInvariantes(anel, 30);
        System.out.println("[TA-01] saída do worker-1: " + movidas.size() + " de 30 chaves mudaram de dono (as " + doWorker1.size() + " que eram dele)");
    }

    @Test
    @DisplayName("RNF-07: a entrada de um worker move só uma parte das chaves")
    void entradaMovePoucasChaves() {
        AnelDeHashLimitado anel = anelCom(3, 64, 0.25);
        for (int i = 0; i < 60; i++) {
            anel.donoDe("chave-" + i);
        }
        Map<String, String> movidas = anel.adicionarWorker("worker-3");

        conferirInvariantes(anel, 60);
        assertTrue(anel.cargaDe("worker-3") > 0, "o worker novo recebeu chaves");
        assertTrue(movidas.size() <= 30, "menos da metade das chaves mudou: " + movidas.size());
        System.out.println("[TA-01] entrada do worker-3: " + movidas.size() + " de 60 chaves mudaram de dono, carga do novo = "
                + anel.cargaDe("worker-3") + ", teto = " + anel.teto());
    }

    @Test
    @DisplayName("Sem nenhum worker, a chave espera. Quando um worker entra, ela ganha dono")
    void semWorkerAChaveEspera() {
        AnelDeHashLimitado anel = new AnelDeHashLimitado(64, 0.25);
        assertNull(anel.donoDe("chave-A"));
        assertEquals(1, anel.totalDeChaves());

        Map<String, String> movidas = anel.adicionarWorker("worker-0");

        assertEquals("worker-0", movidas.get("chave-A"));
        assertEquals("worker-0", anel.donoDe("chave-A"));
        conferirInvariantes(anel, 1);
    }

    @Test
    @DisplayName("O último worker sai: as chaves ficam sem dono e voltam quando alguém entra")
    void ultimoWorkerSai() {
        AnelDeHashLimitado anel = anelCom(1, 64, 0.25);
        anel.donoDe("chave-A");
        anel.donoDe("chave-B");

        Map<String, String> movidas = anel.removerWorker("worker-0");
        assertEquals(2, movidas.size());
        assertNull(movidas.get("chave-A"));
        assertNull(anel.donoAtual("chave-A"));

        anel.adicionarWorker("worker-9");
        assertEquals("worker-9", anel.donoAtual("chave-A"));
        assertEquals("worker-9", anel.donoAtual("chave-B"));
        conferirInvariantes(anel, 2);
    }

    @Test
    @DisplayName("Pontos virtuais: com 64 pontos por worker a divisão natural fica mais uniforme do que com 1")
    void pontosVirtuaisUniformizam() {
        int chaves = 3000;
        double folgaEnorme = 100.0; // o teto não interfere: mede só o espalhamento do hash
        int piorCom1 = 0;
        int piorCom64 = 0;
        for (int pontos : new int[] {1, 64}) {
            AnelDeHashLimitado anel = anelCom(3, pontos, folgaEnorme);
            for (int i = 0; i < chaves; i++) {
                anel.donoDe("chave-" + i);
            }
            int maior = maiorCarga(anel);
            if (pontos == 1) {
                piorCom1 = maior;
            } else {
                piorCom64 = maior;
            }
            System.out.println("[TA-01] " + pontos + " ponto(s) por worker: maior carga = " + maior + " de " + chaves + " (média 1000)");
        }
        assertTrue(piorCom64 < piorCom1, "64 pontos devem espalhar melhor que 1");
        assertTrue(piorCom64 <= 1400, "com 64 pontos, o worker mais carregado fica até 40% acima da média: " + piorCom64);
    }

    @Test
    @DisplayName("Invariantes valem em 500 operações aleatórias de entrada, saída e chave nova")
    void invariantesEmSequenciaAleatoria() {
        Random sorteio = new Random(2026);
        AnelDeHashLimitado anel = new AnelDeHashLimitado(32, 0.25);
        Set<String> vivos = new HashSet<>();
        int chaves = 0;
        int proximoWorker = 0;
        for (int passo = 0; passo < 500; passo++) {
            int dado = sorteio.nextInt(10);
            if (dado < 6) {
                anel.donoDe("chave-" + chaves++);
            } else if (dado < 8 || vivos.isEmpty()) {
                String novo = "worker-" + proximoWorker++;
                anel.adicionarWorker(novo);
                vivos.add(novo);
            } else {
                String sai = new ArrayList<>(vivos).get(sorteio.nextInt(vivos.size()));
                anel.removerWorker(sai);
                vivos.remove(sai);
            }
            assertEquals(vivos, anel.workers());
            conferirInvariantes(anel, chaves);
        }
    }

    @Test
    @DisplayName("Parâmetros inválidos são recusados")
    void parametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new AnelDeHashLimitado(0, 0.25));
        assertThrows(IllegalArgumentException.class, () -> new AnelDeHashLimitado(64, -0.1));
    }
}
