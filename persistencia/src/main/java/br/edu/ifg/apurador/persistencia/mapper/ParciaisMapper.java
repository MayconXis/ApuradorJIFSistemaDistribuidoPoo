package br.edu.ifg.apurador.persistencia.mapper;

public class ParciaisMapper {

    private ParciaisMapper() {
    }

    public static String toTexto(int[][] parciais) {
        if (parciais == null || parciais.length == 0) {
            return null;
        }
        StringBuilder texto = new StringBuilder();
        for (int[] set : parciais) {
            if (texto.length() > 0) {
                texto.append(';');
            }
            texto.append(set[0]).append('-').append(set[1]);
        }
        return texto.toString();
    }

    public static int[][] toMatriz(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return new int[0][2];
        }
        String[] sets = texto.split(";");
        int[][] parciais = new int[sets.length][2];
        for (int i = 0; i < sets.length; i++) {
            String[] pontos = sets[i].split("-");
            parciais[i][0] = Integer.parseInt(pontos[0].trim());
            parciais[i][1] = Integer.parseInt(pontos[1].trim());
        }
        return parciais;
    }
}
