export interface LinhaResponse {
  colocacao: number;
  equipe: string;
  pontos: number;
  jogos: number;
  vitorias: number;
  empates: number;
  derrotas: number;
  pro: number;
  contra: number;
  saldo: number;
  criterioDecisivo: string;
  pendenteDeSorteio: boolean;
}

export interface ClassificacaoResponse {
  chaveId: string;
  nome: string;
  modalidade: string;
  versao: number;
  atualizadaEm: string | null;
  posicoes: LinhaResponse[];
}

export interface AuditoriaResponse {
  chaveId: string;
  consistente: boolean;
  gravada: LinhaResponse[];
  recalculada: LinhaResponse[];
}
