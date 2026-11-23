package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import br.edu.ifg.apurador.core.modalidade.Modalidade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModalidadeService {

    private final CatalogoDeModalidades catalogoDeModalidades;

    public List<String> listar() {
        return catalogoDeModalidades.todas().stream()
                .map(Modalidade::getCodigo)
                .toList();
    }
}
