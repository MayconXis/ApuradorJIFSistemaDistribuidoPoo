package br.edu.ifg.apurador.core.modalidade;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Modalidades conhecidas pelo sistema, guardadas em um Map pelo código.
 * Incluir uma modalidade nova é criar a subclasse e registrá-la aqui.
 */
public class CatalogoDeModalidades {

    private final Map<String, Modalidade> modalidades = new LinkedHashMap<>();

    /** Catálogo com as três modalidades do MVP. */
    public static CatalogoDeModalidades padrao() {
        CatalogoDeModalidades catalogo = new CatalogoDeModalidades();
        catalogo.registrar(new Futsal());
        catalogo.registrar(new Voleibol());
        catalogo.registrar(new Handebol());
        return catalogo;
    }

    public void registrar(Modalidade modalidade) {
        modalidades.put(modalidade.getCodigo(), modalidade);
    }

    public Modalidade buscar(String codigo) {
        Modalidade modalidade = codigo == null ? null : modalidades.get(codigo.trim().toUpperCase(Locale.ROOT));
        if (modalidade == null) {
            throw new IllegalArgumentException("Modalidade desconhecida: " + codigo);
        }
        return modalidade;
    }

    public Collection<Modalidade> todas() {
        return Collections.unmodifiableCollection(modalidades.values());
    }
}
