package br.edu.ifg.apurador.persistencia.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PosicaoId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String chaveId;
    private int colocacao;
}
