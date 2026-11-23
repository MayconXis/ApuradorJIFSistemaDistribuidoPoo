// Deck final de Sistemas Distribuídos: 11 lâminas. Uso: node gerar.js saida.pptx
const pptxgen = require("pptxgenjs");
const SK = "/root/.claude/skills/synced/0c5a7f04-e70a-416b-bb72-6d6a22ae1194_fb867802-9a65-4cee-b200-10cfe8305a68/pptx";
const { applyTheme } = require(SK + "/scripts/apply_theme.js");
const { notas } = require("./notas.js");

const OUT = process.argv[2] || "deck.pptx";
const HEADF = "Cambria", BODYF = "Calibri";
const THEME = {
  name: "Apurador JIF", headFontFace: HEADF, bodyFontFace: BODYF,
  colors: { dk1: "10231C", lt1: "FFFFFF", dk2: "0B3B2C", lt2: "EDF3F0", accent1: "0E7A54", accent2: "F2A900", accent3: "5B6762", accent4: "C5D1CB", accent5: "FFF0C7", accent6: "C8432B", hlink: "0E7A54", folHlink: "5B6762" },
};
const HEX = THEME.colors;

const pres = new pptxgen();
pres.layout = "LAYOUT_WIDE"; // 13,333 x 7,5 polegadas
pres.theme = { headFontFace: HEADF, bodyFontFace: BODYF };
pres.title = "Apuração de competições do JIF: Sistemas Distribuídos";
pres.author = "Maycon Douglas da Silva, Felype de Moura Lino e Silva, Maria Eduarda Mendonça Fernandes";
const S = pres.shapes;
const C = pres.SchemeColor;
const INK = C.text1, WHITE = C.background1, DEEP = C.text2, PALE = C.background2;
const GREEN = C.accent1, GOLD = C.accent2, MUTED = C.accent3, BORDER = C.accent4, GOLDTINT = C.accent5, CORAL = C.accent6;

// ------------------------------------------------------------------ layouts
function layoutConteudo(nome, escuro) {
  const fg = escuro ? WHITE : INK;
  pres.defineSlideMaster({
    title: nome,
    background: { color: escuro ? DEEP : WHITE },
    slideNumber: { x: 12.13, y: 6.95, w: 0.6, h: 0.3, fontSize: 10, color: escuro ? PALE : MUTED, align: "right", fontFace: BODYF },
    objects: [
      { placeholder: { options: { name: "kicker", type: "body", x: 0.6, y: 0.4, w: 9.0, h: 0.3, fontSize: 11, bold: true, charSpacing: 1.5, color: escuro ? GOLD : GREEN, margin: 0, valign: "middle" }, text: "Seção" } },
      { placeholder: { options: { name: "title", type: "title", x: 0.6, y: 0.7, w: 12.13, h: 0.8, fontSize: 36, bold: true, color: fg, margin: 0, valign: "middle", align: "left" }, text: "Título" } },
      { placeholder: { options: { name: "fonte", type: "body", x: 0.6, y: 6.95, w: 11.3, h: 0.3, fontSize: 10, color: escuro ? PALE : MUTED, margin: 0, valign: "middle" }, text: "Fonte" } },
    ],
  });
}
pres.defineSlideMaster({
  title: "CAPA",
  background: { color: DEEP },
  objects: [
    { placeholder: { options: { name: "title", type: "title", x: 0.7, y: 1.55, w: 7.0, h: 1.75, fontSize: 46, bold: true, color: WHITE, margin: 0, valign: "top", align: "left" }, text: "Título" } },
    { placeholder: { options: { name: "sub", type: "body", x: 0.7, y: 3.4, w: 7.2, h: 0.9, fontSize: 18, color: PALE, margin: 0, valign: "top" }, text: "Subtítulo" } },
  ],
});
layoutConteudo("CONTEUDO", false);
layoutConteudo("ESCURO", true);

// ------------------------------------------------------------------ apoio
function tx(s, text, x, y, w, h, o = {}) {
  s.addText(text, Object.assign({ x, y, w, h, isTextBox: true, margin: 0, fontSize: 14, color: INK, valign: "top", align: "left", fontFace: BODYF }, o));
}
function caixa(s, x, y, w, h, fill, o = {}) {
  s.addShape(S.ROUNDED_RECTANGLE, Object.assign({ x, y, w, h, rectRadius: 0.08, fill: { color: fill }, line: { color: fill, width: 0 } }, o));
}
function etiqueta(s, text, x, y, tipo) {
  const w = 0.098 * text.length + 0.34;
  const cores = { fato: [GREEN, WHITE], hip: [GOLD, INK], prop: [MUTED, WHITE], medir: [CORAL, WHITE], mem: [MUTED, WHITE] };
  const [fill, color] = cores[tipo];
  s.addText(text, { shape: S.ROUNDED_RECTANGLE, rectRadius: 0.06, x, y, w, h: 0.28, fill: { color: fill }, color, fontSize: 10, bold: true, align: "center", valign: "middle", margin: 0, charSpacing: 1, isTextBox: true, fontFace: BODYF });
  return w;
}
function bola(s, n, x, y, d = 0.46, fill = GOLD, color = INK) {
  s.addText(String(n), { shape: S.OVAL, x, y, w: d, h: d, fill: { color: fill }, color, fontSize: 15, bold: true, align: "center", valign: "middle", margin: 0, isTextBox: true, fontFace: BODYF });
}
function linha(s, x1, y1, x2, y2, o = {}) {
  const ln = { color: o.color || INK, width: o.width || 1.25 };
  if (o.dash) ln.dashType = "dash";
  if (o.seta) ln.endArrowType = "triangle";
  if (o.setaInicio) ln.beginArrowType = "triangle";
  s.addShape(S.LINE, { x: Math.min(x1, x2), y: Math.min(y1, y2), w: Math.max(Math.abs(x2 - x1), 0.001), h: Math.max(Math.abs(y2 - y1), 0.001), flipH: x2 < x1, flipV: y2 < y1, line: ln });
}
function comp(s, tipo, x, y, w, h, titulo, linhas = []) {
  const st = { nosso: { fill: GREEN, line: GREEN, fg: WHITE, sub: WHITE }, infra: { fill: PALE, line: BORDER, fg: INK, sub: MUTED }, ator: { fill: WHITE, line: INK, fg: INK, sub: MUTED } }[tipo];
  s.addShape(S.ROUNDED_RECTANGLE, { x, y, w, h, rectRadius: 0.08, fill: { color: st.fill }, line: { color: st.line, width: 1.25 } });
  const runs = [{ text: titulo, options: { bold: true, fontSize: 16, color: st.fg, breakLine: linhas.length > 0 } }];
  linhas.forEach((t, i) => runs.push({ text: t, options: { fontSize: 12, color: st.sub, breakLine: i < linhas.length - 1 } }));
  s.addText(runs, { x: x + 0.08, y, w: w - 0.16, h, align: "center", valign: "middle", margin: 0, isTextBox: true, fontFace: BODYF });
}
let n = 0;
function lamina(layout, secao, kicker, titulo, fonte) {
  const s = pres.addSlide({ masterName: layout, sectionTitle: secao });
  if (kicker) s.addText(kicker, { placeholder: "kicker" });
  s.addText(titulo, { placeholder: "title" });
  if (fonte) s.addText(fonte, { placeholder: "fonte" });
  s.addNotes(notas[n++]);
  return s;
}
const REG = "Regulamento JIF Etapa Nacional 2026";

// ============================================================ 1. CAPA
pres.addSection({ title: "Abertura" });
{
  const s = pres.addSlide({ masterName: "CAPA", sectionTitle: "Abertura" });
  s.addText("SISTEMAS DISTRIBUÍDOS · PROPOSTA DE SOLUÇÃO", { shape: S.ROUNDED_RECTANGLE, rectRadius: 0.08, x: 0.7, y: 0.95, w: 4.75, h: 0.36, fill: { color: GOLD }, color: INK, fontSize: 11, bold: true, charSpacing: 1.5, align: "center", valign: "middle", margin: 0, isTextBox: true, fontFace: BODYF });
  s.addText("Apuração de competições do JIF", { placeholder: "title" });
  s.addText("Apuração distribuída que continua certa quando um processo cai", { placeholder: "sub" });
  s.addNotes(notas[n++]);
  tx(s, "EQUIPE", 0.7, 4.75, 3, 0.28, { fontSize: 11, bold: true, color: GOLD, charSpacing: 1.5 });
  ["Maycon Douglas da Silva", "Felype de Moura Lino e Silva", "Maria Eduarda Mendonça Fernandes"].forEach((nome, i) => {
    tx(s, nome, 0.7, 5.08 + i * 0.34, 6.5, 0.34, { fontSize: 16, color: WHITE, valign: "middle" });
  });
  tx(s, "IFG Câmpus Inhumas · Engenharia de Software · novembro de 2026", 0.7, 6.5, 7.3, 0.3, { fontSize: 12, color: PALE });

  caixa(s, 8.2, 1.4, 4.45, 4.35, WHITE);
  tx(s, "CHAVE A · FUTSAL", 8.5, 1.66, 3.85, 0.28, { fontSize: 11, bold: true, color: MUTED, charSpacing: 1.5 });
  ["IFCE", "IFG", "IFSP"].forEach((t, i) => {
    const y = 2.15 + i * 0.9;
    s.addText("?", { shape: S.ROUNDED_RECTANGLE, rectRadius: 0.08, x: 8.5, y, w: 0.74, h: 0.74, fill: { color: GOLD }, color: INK, fontSize: 28, bold: true, fontFace: HEADF, align: "center", valign: "middle", margin: 0, isTextBox: true });
    tx(s, t, 9.5, y, 1.7, 0.74, { fontSize: 28, bold: true, fontFace: HEADF, valign: "middle" });
    tx(s, [{ text: "3", options: { fontSize: 28, bold: true, color: GREEN, fontFace: HEADF } }, { text: " pts", options: { fontSize: 14, color: MUTED } }], 11.05, y, 1.3, 0.74, { align: "right", valign: "middle" });
  });
  tx(s, "Três equipes, três pontos. Quem passa direto?", 8.5, 4.9, 3.85, 0.65, { fontSize: 16, bold: true });
  tx(s, "Placares fictícios, só para ilustrar", 8.2, 5.9, 4.45, 0.28, { fontSize: 11, color: PALE });
}

// ============================================================ 2. PROBLEMA REAL
pres.addSection({ title: "Problema" });
{
  const s = lamina("CONTEUDO", "Problema", "O PROBLEMA", "Mil estudantes, boletim diário em PDF",
    "Fontes: IFG (notícia de 25/08/2026) e site do JIF Goiás 2026. " + REG + ", Arts. 5º, 10, 14, 21 e 25.");
  caixa(s, 0.6, 1.75, 4.95, 4.9, DEEP);
  [["≈ 1.000", "estudantes no JIF Goiás 2026, a 12ª edição"],
   ["14", "câmpus do IFG em Goiânia, de 24 a 28/08"],
   ["11", "modalidades na etapa nacional, Goiânia, 18 a 23/10"]].forEach(([num, lab], i) => {
    const y = 2.0 + i * 1.53;
    tx(s, num, 0.9, y, 2.3, 1.2, { fontSize: 44, bold: true, color: GOLD, fontFace: HEADF, valign: "middle" });
    tx(s, lab, 3.25, y, 2.1, 1.2, { fontSize: 15, color: WHITE, valign: "middle" });
  });
  [["fato", "FATO", "A Comissão Técnica publica o boletim todo dia, em PDF"],
   ["fato", "FATO", "O regulamento cita sistema para inscrição, não para apuração"],
   ["hip", "INFERÊNCIA", "Classificação feita à mão ou em planilha. Falta confirmar"]].forEach(([tipo, rot, texto], i) => {
    const y = 1.75 + i * 1.7;
    caixa(s, 5.9, y, 6.83, 1.5, tipo === "hip" ? GOLDTINT : PALE);
    etiqueta(s, rot, 6.15, y + 0.22, tipo);
    tx(s, texto, 6.15, y + 0.62, 6.35, 0.7, { fontSize: 18 });
  });
}

// ============================================================ 3. REGRAS POR MODALIDADE
{
  const s = lamina("CONTEUDO", "Problema", "O PROBLEMA", "Cada modalidade tem a sua regra",
    "Fonte: " + REG + ", Arts. 61 e 62 (futsal), 111 a 113 (voleibol), 66 e 67 (handebol).");
  const borda = [{ type: "solid", pt: 0.75, color: HEX.accent4 }, { type: "none" }, { type: "solid", pt: 0.75, color: HEX.accent4 }, { type: "none" }];
  const base = { border: borda, valign: "middle", margin: [3.6, 8.64, 3.6, 8.64], fontFace: BODYF };
  const cab = (t) => ({ text: t, options: Object.assign({}, base, { fill: { color: DEEP }, color: WHITE, bold: true, fontSize: 14 }) });
  const rot = (t) => ({ text: t, options: Object.assign({}, base, { fill: { color: PALE }, color: INK, bold: true, fontSize: 14 }) });
  const val = (grande, pequeno, cor = GREEN, fill = null) => {
    const runs = [];
    if (grande) runs.push({ text: grande, options: { fontSize: 17, bold: true, color: cor, breakLine: true, fontFace: HEADF } });
    runs.push({ text: pequeno, options: { fontSize: 14, color: INK } });
    return { text: runs, options: Object.assign({}, base, fill ? { fill: { color: fill } } : {}) };
  };
  const linhas = [
    [{ text: "", options: Object.assign({}, base, { fill: { color: DEEP } }) }, cab("FUTSAL"), cab("VOLEIBOL"), cab("HANDEBOL")],
    [rot("Pontos por jogo"), val("3 · 1 · 0", "vitória, empate, derrota"), val("3 · 2 · 1 · 0", "2x0, 2x1, 1x2, 0x2. Não há empate"), val("3 · 1 · 0", "vitória, empate, derrota")],
    [rot("WO"), val("", "Maior placar da fase dentro da chave"), val("", "2 sets a 0, parciais de 25 a 0"), val("", "Maior placar da fase dentro da chave")],
    [rot("Empate entre 2 equipes"),
      val("8 critérios", "Confronto direto, vitórias, saldo, marcados, sofridos, vermelhos, amarelos, sorteio"),
      val("5 critérios", "Confronto direto, vitórias, sets average, pontos average, sorteio"),
      val("8 critérios", "Confronto direto, vitórias, saldo, sofridos, marcados, average, sanções, sorteio")],
    [rot("Empate entre 3 ou mais"),
      val("7 critérios", "A mesma lista, sem o confronto direto"),
      val("4 critérios", "A mesma lista, sem o confronto direto"),
      val("13 critérios", "Outra lista, com confronto só entre as empatadas", CORAL, GOLDTINT)],
  ];
  s.addTable(linhas, { x: 0.6, y: 1.72, w: 12.13, colW: [2.05, 3.36, 3.36, 3.36], rowH: [0.42, 0.78, 0.62, 1.2, 0.95] });
  caixa(s, 0.6, 6.12, 12.13, 0.6, PALE);
  tx(s, [{ text: "Detalhe: ", options: { bold: true } }, { text: "futsal olha marcados antes de sofridos. Handebol, o contrário." }], 0.85, 6.12, 11.6, 0.6, { fontSize: 16, valign: "middle" });
}

// ============================================================ 4. POR QUE DISTRIBUIR
pres.addSection({ title: "Proposta" });
{
  const s = lamina("CONTEUDO", "Proposta", "A PROPOSTA", "Por que distribuir a apuração",
    "Fonte: " + REG + ", Art. 135, parágrafo único. Hipóteses e proposta são da equipe.");
  // eixo do tempo
  const y0 = 3.05, xa = 0.9, xb = 12.4;
  linha(s, xa, y0, xb, y0, { color: MUTED, width: 2, seta: true });
  // janela de 2 horas
  s.addShape(S.RECTANGLE, { x: 6.35, y: y0 - 0.17, w: 4.4, h: 0.34, fill: { color: GOLD }, line: { color: GOLD, width: 0 } });
  tx(s, "2 horas para protestar", 6.35, y0 - 0.17, 4.4, 0.34, { fontSize: 14, bold: true, align: "center", valign: "middle" });
  const marcos = [
    { x: 1.3, t: "Fim da rodada", tipo: "hip", rot: "HIPÓTESE", d: "Vários resultados chegam juntos" },
    { x: 3.75, t: "Tabela publicada", tipo: "hip", rot: "HIPÓTESE", d: "Muita gente consulta ao mesmo tempo" },
    { x: 6.35, t: "Prazo de protesto", tipo: "fato", rot: "FATO", d: "Até 2 h depois da partida coletiva" },
    { x: 10.0, t: "Protesto aceito", tipo: "prop", rot: "PROPOSTA", d: "Retifica o placar e recalcula a chave" },
  ];
  marcos.forEach((m, i) => {
    const w = i === 3 ? 2.6 : 2.3;
    s.addShape(S.OVAL, { x: m.x - 0.13, y: y0 - 0.13, w: 0.26, h: 0.26, fill: { color: i === 2 ? GOLD : DEEP }, line: { color: WHITE, width: 1.5 } });
    tx(s, m.t, m.x - 0.13, 1.95, w, 0.4, { fontSize: 19, bold: true, fontFace: HEADF, valign: "bottom" });
    linha(s, m.x, 2.42, m.x, y0 - 0.2, { color: BORDER, width: 1 });
    etiqueta(s, m.rot, m.x - 0.13, y0 + 0.4, m.tipo);
    tx(s, m.d, m.x - 0.13, y0 + 0.82, w, 0.8, { fontSize: 16 });
  });
  tx(s, "O regulamento não define o efeito do protesto", 10.0 - 0.13, y0 + 1.62, 2.6, 0.6, { fontSize: 12, color: MUTED, italic: true });
  // conclusão
  caixa(s, 0.6, 5.55, 12.13, 1.15, DEEP);
  tx(s, "Meta: disponibilidade e consistência, não velocidade", 0.95, 5.55, 8.2, 1.15, { fontSize: 24, bold: true, color: WHITE, fontFace: HEADF, valign: "middle" });
  tx(s, "A conta de uma chave é leve", 9.3, 5.55, 3.2, 1.15, { fontSize: 15, color: GOLD, valign: "middle", align: "right" });
}

// ============================================================ 5. DO JOGO À TABELA
{
  const s = lamina("CONTEUDO", "Proposta", "A PROPOSTA", "Do jogo à tabela, em etapas",
    "Fluxo implementado nos módulos dispatcher e worker. Diagrama de sequência: docs/diagramas/06.");
  const passos = [
    ["Mesário", "Registra o placar na tela"],
    ["Gateway", "Valida pela regra e grava no banco"],
    ["Dispatcher", "Escolhe o worker dono da chave"],
    ["RabbitMQ", "Entrega um aviso na fila desse worker"],
    ["Worker", "Trava a chave, aplica e recalcula"],
    ["Tela", "Mostra a posição e o critério decisivo"],
  ];
  const x0 = 0.6, passo = 2.05, yb = 2.05;
  linha(s, x0 + 0.23, yb + 0.23, x0 + 5 * passo + 0.23, yb + 0.23, { color: BORDER, width: 2 });
  passos.forEach(([quem, oque], i) => {
    const x = x0 + i * passo;
    bola(s, i + 1, x, yb, 0.46, i === 1 ? GOLD : DEEP, i === 1 ? INK : WHITE);
    tx(s, quem, x, yb + 0.7, 1.9, 0.42, { fontSize: 20, bold: true, color: GREEN, fontFace: HEADF });
    tx(s, oque, x, yb + 1.15, 1.85, 1.1, { fontSize: 16 });
  });
  // destaque do passo 2
  caixa(s, 0.6, 4.6, 12.13, 1.75, GOLDTINT);
  tx(s, "202", 0.95, 4.6, 2.0, 1.75, { fontSize: 54, bold: true, color: INK, fontFace: HEADF, valign: "middle" });
  tx(s, "O Gateway só responde depois do passo 2", 3.05, 4.95, 9.4, 0.5, { bold: true, fontSize: 22, fontFace: HEADF, valign: "middle" });
  tx(s, "Se fila ou worker falharem, o resultado já está gravado", 3.05, 5.5, 9.4, 0.45, { fontSize: 16, valign: "middle" });
}

// ============================================================ 6. ARQUITETURA
pres.addSection({ title: "Arquitetura" });
{
  const s = lamina("CONTEUDO", "Arquitetura", "A ARQUITETURA", "Quem fala com quem",
    "Verde: código da equipe. Cinza: infraestrutura. Diagramas de componentes e implantação: docs/diagramas/04 e 05.");
  // fronteira do k3s
  s.addShape(S.ROUNDED_RECTANGLE, { x: 3.0, y: 1.6, w: 9.75, h: 5.05, rectRadius: 0.1, fill: { color: WHITE }, line: { color: MUTED, width: 1.25, dashType: "dash" } });
  tx(s, "k3s (VIM)", 3.2, 1.68, 2.5, 0.3, { fontSize: 13, bold: true, color: MUTED });
  comp(s, "nosso", 0.6, 3.35, 1.95, 1.2, "Angular", ["navegador"]);
  comp(s, "nosso", 3.6, 3.2, 2.55, 1.5, "Gateway e Dispatcher", ["Spring Boot", "anel e detector"]);
  // RabbitMQ com três filas
  s.addShape(S.ROUNDED_RECTANGLE, { x: 7.0, y: 2.3, w: 2.3, h: 3.3, rectRadius: 0.08, fill: { color: PALE }, line: { color: BORDER, width: 1.25 } });
  tx(s, "RabbitMQ", 7.0, 2.4, 2.3, 0.35, { fontSize: 16, bold: true, align: "center" });
  ["worker-0", "worker-1", "worker-2"].forEach((q, i) => {
    s.addText(q, { shape: S.ROUNDED_RECTANGLE, rectRadius: 0.05, x: 7.25, y: 2.95 + i * 0.75, w: 1.8, h: 0.5, fill: { color: WHITE }, line: { color: BORDER, width: 1 }, fontSize: 12, color: INK, align: "center", valign: "middle", margin: 0, isTextBox: true, fontFace: BODYF });
  });
  tx(s, "uma fila quórum por worker", 7.0, 5.15, 2.3, 0.35, { fontSize: 11, color: MUTED, align: "center" });
  // workers
  [0, 1, 2].forEach((i) => comp(s, "nosso", 10.1, 2.3 + i * 1.12, 2.35, 0.9, "worker-" + i, ["Spring Boot + core"]));
  comp(s, "infra", 6.95, 5.85, 2.4, 0.68, "PostgreSQL", []);
  tx(s, "eventos, partidas, posições", 9.45, 5.95, 2.6, 0.5, { fontSize: 12, color: MUTED, valign: "middle" });
  // ligações
  linha(s, 2.55, 3.95, 3.6, 3.95, { seta: true });
  tx(s, "REST", 2.62, 3.6, 0.9, 0.3, { fontSize: 11, color: MUTED });
  linha(s, 6.15, 3.7, 7.0, 3.7, { seta: true });
  tx(s, "publica aviso", 5.95, 3.35, 1.2, 0.3, { fontSize: 11, color: MUTED, align: "center" });
  [0, 1, 2].forEach((i) => linha(s, 9.05, 3.2 + i * 0.75, 10.1, 2.75 + i * 1.12, { seta: true }));
  tx(s, "ack manual", 9.32, 5.4, 1.1, 0.3, { fontSize: 11, color: MUTED });
  linha(s, 4.87, 4.7, 6.95, 6.05, { seta: true });
  tx(s, "grava evento", 4.45, 5.55, 1.3, 0.3, { fontSize: 11, color: MUTED });
  linha(s, 11.27, 5.44, 9.35, 6.15, { seta: true });
  // heartbeat
  linha(s, 11.27, 2.3, 11.27, 2.05, { dash: true, color: GOLD, width: 1.75 });
  linha(s, 11.27, 2.05, 4.87, 2.05, { dash: true, color: GOLD, width: 1.75 });
  linha(s, 4.87, 2.05, 4.87, 3.2, { dash: true, color: GOLD, width: 1.75, seta: true });
  tx(s, "heartbeat a cada 1 s", 6.6, 1.76, 2.8, 0.26, { fontSize: 12, bold: true, color: INK, align: "center", valign: "middle" });
}

// ============================================================ 7. ESCOLHAS DE INFRAESTRUTURA
{
  const s = lamina("CONTEUDO", "Arquitetura", "AS ESCOLHAS", "Infraestrutura: o que escolhemos e por quê",
    "Comparação da equipe. Detalhes no relatório técnico, seção 4.");
  const borda = [{ type: "solid", pt: 0.75, color: HEX.accent4 }, { type: "none" }, { type: "solid", pt: 0.75, color: HEX.accent4 }, { type: "none" }];
  const base = { border: borda, valign: "middle", margin: [3.6, 8.64, 3.6, 8.64], fontFace: BODYF, fontSize: 14, color: INK };
  const cab = (t) => ({ text: t, options: Object.assign({}, base, { fill: { color: DEEP }, color: WHITE, bold: true }) });
  const peca = (t) => ({ text: t, options: Object.assign({}, base, { fill: { color: PALE }, bold: true, color: MUTED }) });
  const esc = (t) => ({ text: t, options: Object.assign({}, base, { bold: true, color: GREEN, fontSize: 17, fontFace: HEADF }) });
  const c = (t) => ({ text: t, options: Object.assign({}, base) });
  const desc = (nome, motivo) => ({ text: [{ text: nome + ": ", options: { bold: true } }, { text: motivo }], options: Object.assign({}, base, { color: INK }) });
  const linhas = [
    [cab("Peça"), cab("Escolha"), cab("Por quê"), cab("Alternativa descartada")],
    [peca("Middleware"), esc("RabbitMQ"), c("Fila quórum, ack manual, reentrega e DLQ"), desc("Kafka", "log particionado é demais para uma competição")],
    [peca("VIM"), esc("k3s"), c("Kubernetes leve, roda em notebook, aceita Chaos Mesh"), desc("Docker Swarm", "o Chaos Mesh não roda nele")],
    [peca("Backend"), esc("Spring Boot"), c("AMQP, JPA e Actuator prontos. Grupo já sabe Java"), desc("Quarkus", "ganho pequeno, mais coisa para aprender")],
    [peca("Interface"), esc("Angular"), c("TypeScript tipado. Fala só com o Gateway"), desc("Thymeleaf", "misturaria a tela com o Gateway")],
    [peca("Banco"), esc("PostgreSQL"), c("Guarda o histórico e trava cada chave"), desc("Redis para travas", "mais uma peça para operar")],
    [peca("Testes"), esc("k6 e Chaos Mesh"), c("Carga por script e falha injetada, repetíveis"), desc("Teste manual", "não repete o mesmo cenário")],
  ];
  s.addTable(linhas, { x: 0.6, y: 1.72, w: 12.13, colW: [1.6, 2.35, 4.0, 4.18], rowH: [0.42, 0.66, 0.66, 0.66, 0.66, 0.66, 0.66] });
  caixa(s, 0.6, 6.25, 12.13, 0.5, GOLDTINT);
  tx(s, "Balanceamento e tolerância a falhas: algoritmos nossos, sem biblioteca pronta", 0.85, 6.25, 11.6, 0.5, { fontSize: 15, bold: true, valign: "middle" });
}

// ============================================================ 8. BALANCEAMENTO
pres.addSection({ title: "Algoritmos" });
{
  const s = lamina("CONTEUDO", "Algoritmos", "BALANCEAMENTO", "Hash consistente com carga limitada",
    "Técnica: Mirrokni, Thorup e Zadimoghaddam (SODA 2018). Implementação da equipe, módulo cluster.");
  // anel
  const cx = 3.0, cy = 4.15, r = 1.75;
  s.addShape(S.OVAL, { x: cx - r, y: cy - r, w: 2 * r, h: 2 * r, fill: { color: WHITE }, line: { color: BORDER, width: 3 } });
  const coresW = [GREEN, GOLD, DEEP];
  const pontos = [[10, 0], [55, 1], [95, 2], [140, 0], [175, 1], [215, 2], [255, 0], [290, 1], [330, 2]];
  const pos = (graus, raio) => { const a = (graus - 90) * Math.PI / 180; return [cx + raio * Math.cos(a), cy + raio * Math.sin(a)]; };
  pontos.forEach(([g, w]) => { const [x, y] = pos(g, r); s.addShape(S.OVAL, { x: x - 0.14, y: y - 0.14, w: 0.28, h: 0.28, fill: { color: coresW[w] }, line: { color: WHITE, width: 1.5 } }); });
  // chave A em 35 graus: vai ao ponto de 55 (worker-1)
  const [kx, ky] = pos(35, r);
  s.addText("A", { shape: S.ROUNDED_RECTANGLE, rectRadius: 0.04, x: kx - 0.18, y: ky - 0.18, w: 0.36, h: 0.36, fill: { color: CORAL }, color: WHITE, fontSize: 13, bold: true, align: "center", valign: "middle", margin: 0, isTextBox: true, fontFace: BODYF });
  const [k1x, k1y] = pos(39, r - 0.32), [k2x, k2y] = pos(52, r - 0.32);
  linha(s, k1x, k1y, k2x, k2y, { seta: true, color: CORAL, width: 2 });
  tx(s, "anel", cx - 0.6, cy - 0.35, 1.2, 0.35, { fontSize: 18, bold: true, color: MUTED, align: "center", fontFace: HEADF });
  tx(s, "64 pontos por worker", cx - 1.0, cy + 0.02, 2.0, 0.3, { fontSize: 12, color: MUTED, align: "center" });
  // legenda dos workers
  ["worker-0", "worker-1", "worker-2"].forEach((w, i) => {
    s.addShape(S.OVAL, { x: 0.75 + i * 1.6, y: 6.33, w: 0.22, h: 0.22, fill: { color: coresW[i] }, line: { color: coresW[i], width: 0 } });
    tx(s, w, 1.05 + i * 1.6, 6.27, 1.2, 0.34, { fontSize: 12, valign: "middle" });
  });
  // passos
  const passos = ["A chave cai numa posição do anel", "Vai ao primeiro worker adiante", "Worker no teto? Pula para o próximo"];
  passos.forEach((p, i) => {
    bola(s, i + 1, 5.45, 1.85 + i * 0.62, 0.4);
    tx(s, p, 6.0, 1.85 + i * 0.62, 3.6, 0.4, { fontSize: 15, valign: "middle" });
  });
  // fórmula
  caixa(s, 9.75, 1.75, 2.98, 1.62, DEEP);
  tx(s, "TETO DE CARGA", 9.98, 1.88, 2.6, 0.26, { fontSize: 10, bold: true, color: GOLD, charSpacing: 1.5 });
  tx(s, "1,25 × média", 9.98, 2.18, 2.6, 0.62, { fontSize: 30, bold: true, color: WHITE, fontFace: HEADF, valign: "middle" });
  tx(s, "folga de 25%: SUPOSIÇÃO", 9.98, 2.88, 2.6, 0.3, { fontSize: 12, color: PALE });
  // round-robin x anel
  tx(s, "Por que não round-robin", 5.45, 3.8, 7.2, 0.36, { fontSize: 18, bold: true, color: GREEN, fontFace: HEADF });
  const comp2 = [["Round-robin", "Chave A vai a w0, w1, w2: ordem perdida", CORAL], ["Anel", "Chave A vai sempre a w1: ordem garantida", GREEN]];
  comp2.forEach(([q, d, cor], i) => {
    const y = 4.25 + i * 0.62;
    tx(s, q, 5.45, y, 1.6, 0.5, { fontSize: 15, bold: true, color: cor, valign: "middle" });
    tx(s, d, 7.1, y, 5.6, 0.5, { fontSize: 15, valign: "middle" });
  });
  // número real (teste em memória)
  caixa(s, 5.45, 5.6, 7.28, 1.1, PALE);
  tx(s, "8 de 30", 5.7, 5.6, 2.0, 1.1, { fontSize: 34, bold: true, color: GREEN, fontFace: HEADF, valign: "middle" });
  etiqueta(s, "TESTE EM MEMÓRIA", 7.75, 5.72, "mem");
  tx(s, "chaves mudaram quando um worker saiu: só as dele", 7.75, 6.05, 4.8, 0.55, { fontSize: 15, valign: "middle" });
}

// ============================================================ 9. TOLERÂNCIA A FALHAS
{
  const s = lamina("CONTEUDO", "Algoritmos", "TOLERÂNCIA A FALHAS", "Detectar, reatribuir, reprocessar",
    "Tempos de 3 s e 5 s: SUPOSIÇÃO, a calibrar no TF-01. Base: Chandra e Toueg (1996).");
  const xs = (t) => 1.9 + (t + 1.5) * 1.0; // t de -1,5 a 6,2 segundos
  const yb = 2.6;
  // faixa de estado
  const seg = [[-1.5, 0, GREEN, "VIVO", WHITE], [0, 3, BORDER, "silêncio", INK], [3, 5, GOLD, "SUSPEITO", INK], [5, 9.33, CORAL, "MORTO: sai do anel", WHITE]];
  seg.forEach(([a, b, cor, rot, fg]) => {
    s.addText(rot, { shape: S.RECTANGLE, x: xs(a), y: yb, w: xs(b) - xs(a), h: 0.5, fill: { color: cor }, line: { color: WHITE, width: 1 }, fontSize: 13, bold: true, color: fg, align: "center", valign: "middle", margin: 0, isTextBox: true, fontFace: BODYF });
  });
  tx(s, "worker-1", 0.6, yb, 1.2, 0.5, { fontSize: 14, bold: true, valign: "middle" });
  // heartbeats
  [-1, 0].forEach((t) => { s.addShape(S.OVAL, { x: xs(t) - 0.09, y: yb - 0.36, w: 0.18, h: 0.18, fill: { color: GREEN }, line: { color: GREEN, width: 0 } }); });
  tx(s, "heartbeat a cada 1 s", 0.6, yb - 0.5, 1.25, 0.45, { fontSize: 12, color: GREEN, bold: true });
  tx(s, "queda", xs(0) + 0.16, yb - 0.4, 0.8, 0.26, { fontSize: 12, bold: true, color: CORAL });
  // eixo
  const ye = 3.35;
  linha(s, xs(-1.5), ye, xs(6.3), ye, { color: MUTED, width: 1 });
  [0, 1, 2, 3, 4, 5, 6].forEach((t) => {
    linha(s, xs(t), ye - 0.05, xs(t), ye + 0.05, { color: MUTED, width: 1 });
    tx(s, t + " s", xs(t) - 0.3, ye + 0.08, 0.6, 0.26, { fontSize: 11, color: MUTED, align: "center" });
  });
  // fases
  const fases = [[3.4, 8.35, "1  Detectar", "Sem heartbeat por 5 s: declarado morto", PALE], [8.42, 10.55, "2  Reatribuir", "Anel tira o worker. Só as chaves dele mudam", GOLDTINT], [10.62, 12.73, "3  Reprocessar", "Novo dono trava a chave e aplica pendentes", PALE]];
  fases.forEach(([a, b, t, d, cor], i) => {
    const x = a, w = b - a;
    caixa(s, x, 3.85, w, 1.5, cor);
    tx(s, t, x + 0.15, 3.95, w - 0.3, 0.4, { fontSize: 18, bold: true, color: GREEN, fontFace: HEADF });
    tx(s, d, x + 0.15, 4.38, w - 0.3, 0.9, { fontSize: 14 });
  });
  // garantias
  caixa(s, 0.6, 5.7, 5.95, 1.0, DEEP);
  tx(s, [{ text: "Sem perda  ", options: { bold: true, color: GOLD } }, { text: "o evento está no banco antes da fila", options: { color: WHITE } }], 0.85, 5.7, 5.5, 1.0, { fontSize: 16, valign: "middle" });
  caixa(s, 6.78, 5.7, 5.95, 1.0, DEEP);
  tx(s, [{ text: "Sem duplicata  ", options: { bold: true, color: GOLD } }, { text: "aplicar e marcar na mesma transação", options: { color: WHITE } }], 7.03, 5.7, 5.5, 1.0, { fontSize: 16, valign: "middle" });
}

// ============================================================ 10. COMO VAMOS TESTAR
pres.addSection({ title: "Testes e fechamento" });
{
  const s = lamina("CONTEUDO", "Testes e fechamento", "COMO VAMOS TESTAR", "Medir antes de afirmar",
    "Fora do k3s: processos locais, 17/11/2026. No k3s: VM de 4 CPUs e 8 GB num MacBook Air, 18/11/2026. Origem: carga/resultados.");
  caixa(s, 0.6, 1.75, 4.1, 4.95, DEEP);
  tx(s, "0", 0.6, 1.95, 4.1, 2.4, { fontSize: 160, bold: true, color: GOLD, fontFace: HEADF, align: "center", valign: "middle" });
  tx(s, "eventos perdidos e duplicatas na tabela final", 0.95, 4.4, 3.4, 0.8, { fontSize: 16, color: WHITE, align: "center" });
  s.addText("NAS RODADAS MEDIDAS", { shape: S.ROUNDED_RECTANGLE, rectRadius: 0.06, x: 1.3, y: 5.55, w: 2.7, h: 0.4, fill: { color: GOLD }, color: INK, fontSize: 13, bold: true, align: "center", valign: "middle", margin: 0, charSpacing: 1, isTextBox: true, fontFace: BODYF });
  const borda = [{ type: "solid", pt: 0.75, color: HEX.accent4 }, { type: "none" }, { type: "solid", pt: 0.75, color: HEX.accent4 }, { type: "none" }];
  const base = { border: borda, valign: "middle", margin: [3.6, 7.2, 3.6, 7.2], fontFace: BODYF, fontSize: 14, color: INK };
  const cab = (t) => ({ text: t, options: Object.assign({}, base, { fill: { color: PALE }, bold: true, color: MUTED, fontSize: 12 }) });
  const c = (t, o = {}) => ({ text: t, options: Object.assign({}, base, o) });
  const pequeno = { fontSize: 13 };
  const linhas = [
    [cab("Teste"), cab("Fora do k3s"), cab("No k3s")],
    [c("Rajada de resultados", { bold: true }), c("0 perdidos, 0 duplicatas", pequeno), c("0 perdidos, p95 de 9 ms. Uma rodada deu 6,1 s", pequeno)],
    [c("Consultas simultâneas", { bold: true }), c("p95 de 5,9 ms", pequeno), c("p95 de 8,0 ms", pequeno)],
    [c("Matar um worker", { bold: true }), c("6 s até novo dono, 0 perdidos", pequeno), c("5,8 s até novo dono, 0 perdidos", pequeno)],
    [c("Atrasar a rede em 300 ms", { bold: true }), c("Não medido", { color: MUTED, fontSize: 13 }), c("0 perdidos, nenhum falso positivo", pequeno)],
    [c("Atrasar o heartbeat em 2,5 s", { bold: true }), c("Não medido", { color: MUTED, fontSize: 13 }), c("0 perdidos, worker declarado morto", pequeno)],
    [c("Derrubar o Gateway", { bold: true }), c("9,3 s para voltar", pequeno), c("10 s para voltar, 10% das requisições falharam", pequeno)],
  ];
  s.addTable(linhas, { x: 5.05, y: 1.75, w: 7.68, colW: [2.35, 2.35, 2.98], rowH: [0.4, 0.62, 0.5, 0.5, 0.5, 0.5, 0.62] });
  caixa(s, 5.05, 5.85, 7.68, 0.85, PALE);
  etiqueta(s, "JÁ RODOU", 5.3, 6.13, "fato");
  tx(s, "Build com Maven, 78 testes e TI-02 com 7 de 7", 6.55, 5.85, 6.0, 0.85, { fontSize: 15, valign: "middle" });
}

// ============================================================ 11. ENTREGÁVEIS E SUPOSIÇÕES
{
  const s = lamina("ESCURO", "Testes e fechamento", "FECHAMENTO", "Entregáveis e o que falta validar",
    "Lista completa de suposições: relatório técnico, seção 2.1.");
  tx(s, "O que entregamos", 0.6, 1.8, 5.6, 0.45, { fontSize: 22, bold: true, color: GOLD, fontFace: HEADF });
  const ent = ["Código: core, cluster, Gateway, worker e Angular", "Manifestos do k3s e implantação do zero", "Scripts do k6 e manifestos do Chaos Mesh", "Requisitos, atas, diagramas e relatório técnico"];
  ent.forEach((t, i) => {
    bola(s, i + 1, 0.6, 2.45 + i * 0.95, 0.42, GREEN, WHITE);
    tx(s, t, 1.2, 2.45 + i * 0.95, 4.9, 0.42, { fontSize: 16, color: WHITE, valign: "middle" });
  });
  caixa(s, 6.6, 1.75, 6.13, 4.95, WHITE);
  tx(s, "O que falta validar", 6.9, 1.95, 5.6, 0.45, { fontSize: 22, bold: true, color: GREEN, fontFace: HEADF });
  const val = [["SUP-01", "Empate reduzido recomeça a lista de critérios", "prop"], ["HIPÓTESE", "Resultados chegam em rajada", "hip"], ["HIPÓTESE", "Protesto aceito vira retificação", "hip"], ["SUPOSIÇÃO", "Folga de 25% e limite de 5 s", "prop"], ["LIMITAÇÃO", "Gateway é ponto único de falha", "medir"]];
  val.forEach(([r, t, tipo], i) => {
    const y = 2.6 + i * 0.78;
    etiqueta(s, r, 6.9, y + 0.07, tipo);
    tx(s, t, 8.35, y, 4.2, 0.42, { fontSize: 16, valign: "middle" });
  });
}

(async () => {
  await pres.writeFile({ fileName: OUT });
  await applyTheme(OUT, THEME);
  console.log("ok", OUT);
})();
