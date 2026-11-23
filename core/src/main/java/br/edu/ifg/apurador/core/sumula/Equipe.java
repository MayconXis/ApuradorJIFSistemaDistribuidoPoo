package br.edu.ifg.apurador.core.sumula;

/**
 * Uma equipe da chave. O nome é final: depois de criada, a equipe não muda.
 * Duas equipes com o mesmo nome são a mesma equipe (equals e hashCode pelo nome),
 * o que permite usar Equipe como chave de Map.
 */
public final class Equipe {

    private final String nome;

    public Equipe(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("A equipe precisa de um nome.");
        }
        this.nome = nome.trim();
    }

    public String getNome() {
        return nome;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Equipe)) {
            return false;
        }
        return nome.equals(((Equipe) outro).nome);
    }

    @Override
    public int hashCode() {
        return nome.hashCode();
    }

    @Override
    public String toString() {
        return nome;
    }
}
