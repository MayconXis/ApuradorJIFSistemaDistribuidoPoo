interface ErroApiBody {
  erro?: string;
  camposInvalidos?: Record<string, string>;
}

export function mensagemErroApi(erro: unknown): string {
  const e = erro as { status?: number; error?: ErroApiBody };
  const campos = e?.error?.camposInvalidos;
  if (campos && Object.keys(campos).length > 0) {
    return Object.values(campos).join(' ');
  }
  if (e?.error?.erro) {
    return e.error.erro;
  }
  if (e?.status === 0) {
    return 'O Gateway não respondeu.';
  }
  return 'Erro ' + (e?.status ?? 'desconhecido') + ' ao falar com o Gateway.';
}
