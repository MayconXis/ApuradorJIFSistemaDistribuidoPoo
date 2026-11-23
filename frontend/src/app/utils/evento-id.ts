export function novoEventoId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return 'evt-' + Date.now() + '-' + Math.floor(Math.random() * 1e9);
}
