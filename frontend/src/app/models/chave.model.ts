export interface ChaveIncluirRequest {
  id?: string;
  nome: string;
  modalidade: string;
  equipes: string[];
}

export interface ChaveResponse {
  id: string;
  nome: string;
  modalidade: string;
  equipes: string[];
  versao: number;
  worker: string | null;
}
