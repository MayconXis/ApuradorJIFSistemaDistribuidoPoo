export interface PartidaResponse {
  id: string;
  chaveId: string;
  mandante: string;
  visitante: string;
  estado: string;
  placarMandante: number;
  placarVisitante: number;
  parciais: string | null;
  wo: boolean;
  ausente: string | null;
}
