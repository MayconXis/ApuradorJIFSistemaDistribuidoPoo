export interface ResultadoRegistrarRequest {
  eventoId: string;
  partidaId: string;
  placarMandante?: number;
  placarVisitante?: number;
  parciais?: number[][];
  wo?: boolean;
  ausente?: string;
  amarelosMandante?: number;
  amarelosVisitante?: number;
  vermelhosMandante?: number;
  vermelhosVisitante?: number;
  sancoesMandante?: number;
  sancoesVisitante?: number;
}

export interface ReciboResponse {
  eventoId: string;
  seq: number;
  novo: boolean;
  worker: string | null;
  estado: string;
}

export interface EventoResponse {
  eventoId: string;
  seq: number;
  chaveId: string;
  partidaId: string;
  estado: string;
  worker: string | null;
  motivo: string | null;
  recebidoEm: string | null;
  encerradoEm: string | null;
}
