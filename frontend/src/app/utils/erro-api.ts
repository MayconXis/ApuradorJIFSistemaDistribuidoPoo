export function mensagemErroApi(erro: unknown): string {
  const e = erro as { status?: number; error?: { erro?: string } };
  if (e?.error?.erro) {
    return e.error.erro;
  }
  if (e?.status === 0) {
    return 'O Gateway não respondeu.';
  }
  return 'Erro ' + (e?.status ?? 'desconhecido') + ' ao falar com o Gateway.';
}
