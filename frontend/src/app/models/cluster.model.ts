export interface WorkerResponse {
  id: string;
  estado: 'VIVO' | 'SUSPEITO' | 'MORTO';
  silencioMs: number;
  chaves: string[];
  carga: number;
}

export interface ClusterResponse {
  workers: WorkerResponse[];
  teto: number;
  totalDeChaves: number;
  pontosPorWorker: number;
  folga: number;
  tempoLimiteMs: number;
  mudancas: string[];
}

export interface ConsistenciaResponse {
  eventos: number;
  pendentes: number;
  aplicados: number;
  rejeitados: number;
  superados: number;
  aplicadosMaisDeUmaVez: number;
  chaves: number;
  chavesInconsistentes: number;
}
