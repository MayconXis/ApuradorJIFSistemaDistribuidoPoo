package br.edu.ifg.apurador.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifg.apurador.cluster.apoio.RelogioManual;
import br.edu.ifg.apurador.cluster.falhas.DetectorDeFalhas;
import br.edu.ifg.apurador.cluster.falhas.EstadoDoWorker;
import br.edu.ifg.apurador.cluster.falhas.Transicao;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TA-02. Algoritmo de tolerância a falhas, parte 1: detector por heartbeat (RNF-06). */
@DisplayName("TA-02 Tolerância a falhas: detector por heartbeat com tempo limite")
class DetectorDeFalhasTest {

    private final RelogioManual relogio = new RelogioManual();
    private final DetectorDeFalhas detector = new DetectorDeFalhas(relogio, 3000, 5000);

    @Test
    @DisplayName("Primeiro heartbeat: o worker aparece como VIVO")
    void primeiroHeartbeat() {
        Transicao transicao = detector.registrarHeartbeat("worker-0");
        assertNotNull(transicao);
        assertNull(transicao.getDe());
        assertEquals(EstadoDoWorker.VIVO, transicao.getPara());
        assertEquals(EstadoDoWorker.VIVO, detector.estadoDe("worker-0"));
    }

    @Test
    @DisplayName("Heartbeat em dia: continua VIVO e não gera transição")
    void heartbeatEmDia() {
        detector.registrarHeartbeat("worker-0");
        for (int i = 0; i < 20; i++) {
            relogio.avancar(1000);
            assertNull(detector.registrarHeartbeat("worker-0"));
            assertTrue(detector.verificar().isEmpty());
        }
        assertEquals(EstadoDoWorker.VIVO, detector.estadoDe("worker-0"));
    }

    @Test
    @DisplayName("Silêncio: SUSPEITO depois de 3 s e MORTO depois de 5 s")
    void silencioLevaASuspeitoEDepoisAMorto() {
        detector.registrarHeartbeat("worker-0");

        relogio.avancar(3000);
        assertTrue(detector.verificar().isEmpty(), "exatamente no limite ainda está vivo");

        relogio.avancar(1);
        List<Transicao> suspeito = detector.verificar();
        assertEquals(1, suspeito.size());
        assertEquals(EstadoDoWorker.SUSPEITO, suspeito.get(0).getPara());

        relogio.avancar(1999);
        assertTrue(detector.verificar().isEmpty(), "aos 5 s exatos ainda é suspeito");

        relogio.avancar(1);
        List<Transicao> morto = detector.verificar();
        assertEquals(1, morto.size());
        assertEquals(EstadoDoWorker.SUSPEITO, morto.get(0).getDe());
        assertEquals(EstadoDoWorker.MORTO, morto.get(0).getPara());
        assertEquals(5001, detector.silencioDe("worker-0"));

        relogio.avancar(60_000);
        assertTrue(detector.verificar().isEmpty(), "morto não gera transição de novo");
    }

    @Test
    @DisplayName("Sem verificação intermediária, passa direto de VIVO a MORTO")
    void diretoParaMorto() {
        detector.registrarHeartbeat("worker-0");
        relogio.avancar(9000);
        List<Transicao> mudancas = detector.verificar();
        assertEquals(1, mudancas.size());
        assertEquals(EstadoDoWorker.VIVO, mudancas.get(0).getDe());
        assertEquals(EstadoDoWorker.MORTO, mudancas.get(0).getPara());
    }

    @Test
    @DisplayName("Heartbeat novo traz de volta o SUSPEITO e o MORTO")
    void heartbeatTrazDeVolta() {
        detector.registrarHeartbeat("worker-0");
        relogio.avancar(4000);
        detector.verificar();
        Transicao voltouDeSuspeito = detector.registrarHeartbeat("worker-0");
        assertEquals(EstadoDoWorker.SUSPEITO, voltouDeSuspeito.getDe());
        assertEquals(EstadoDoWorker.VIVO, voltouDeSuspeito.getPara());

        relogio.avancar(6000);
        detector.verificar();
        assertEquals(EstadoDoWorker.MORTO, detector.estadoDe("worker-0"));
        Transicao voltouDeMorto = detector.registrarHeartbeat("worker-0");
        assertEquals(EstadoDoWorker.MORTO, voltouDeMorto.getDe());
        assertEquals(EstadoDoWorker.VIVO, detector.estadoDe("worker-0"));
    }

    @Test
    @DisplayName("Cada worker é avaliado em separado")
    void workersIndependentes() {
        detector.registrarHeartbeat("worker-0");
        detector.registrarHeartbeat("worker-1");
        relogio.avancar(4000);
        detector.registrarHeartbeat("worker-0");
        relogio.avancar(2000);

        List<Transicao> mudancas = detector.verificar();

        assertEquals(1, mudancas.size());
        assertEquals("worker-1", mudancas.get(0).getWorker());
        assertEquals(EstadoDoWorker.MORTO, detector.estadoDe("worker-1"));
        assertEquals(EstadoDoWorker.VIVO, detector.estadoDe("worker-0"));
    }

    @Test
    @DisplayName("Falso positivo: worker vivo com a rede atrasada é declarado morto e depois volta")
    void falsoPositivo() {
        detector.registrarHeartbeat("worker-lento");
        relogio.avancar(6000); // o heartbeat atrasou 6 s, mas o worker não caiu
        assertEquals(EstadoDoWorker.MORTO, detector.verificar().get(0).getPara());

        Transicao volta = detector.registrarHeartbeat("worker-lento");
        assertEquals(EstadoDoWorker.VIVO, volta.getPara());
    }

    @Test
    @DisplayName("Parâmetros inválidos são recusados")
    void parametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new DetectorDeFalhas(relogio, 0, 5000));
        assertThrows(IllegalArgumentException.class, () -> new DetectorDeFalhas(relogio, 5000, 3000));
    }
}
