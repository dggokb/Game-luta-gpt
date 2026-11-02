import { Ctx, Env, rng } from "./core";
import { Film } from "./film";

// fumaca_pouso v2: poeira de aterrissagem de anime, com MASSA (comparada com o pouso do FighterZ).
// Fundo TRANSPARENTE (PNG); efeito_video.py --ancora chao. O jogo espelha ao acaso.
//
// Nuvem: bolhas de tamanhos diferentes empilhadas (fileira de baixo grande, de cima menor, mais
// alta no meio), silhueta irregular. Cinza médio em dois tons, topo claro, contorno fino
// cinza-escuro, linhas de caracol. Cortes de ar: lâminas largas e opacas cinza-claras, com uma
// faixa escura num lado e pontas afiadas, cruzando em dois X, saindo de dentro da nuvem.
// 0 pancada   1-2 lâminas no alto, a nuvem sobe   3-5 a nuvem incha e rola para fora
// 6-9 a nuvem baixa, encolhe e some

const FPS = 24, BPM = 60, DURATION = 10;
const BASE = "#b9b2a8", SHADE = "#8b837a", LIGHT = "#d9d3ca", INK = "#4a423b";
const BLADE = "#e2ddd5", BLADE_EDGE = "#9f978d";

const clamp = (v: number, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const ease = (t: number) => 1 - Math.pow(1 - clamp(t), 2.2);

// Bolhas fixas: x (unidades, + para a direita), altura do centro, raio, fileira (0 baixo, 1, 2 topo).
type Bub = { x: number; y: number; r: number; row: number; curl: number };
const rb = rng(5);
const bubs: Bub[] = [];
for (let i = -7; i <= 7; i++) { // fileira de baixo: grandes, colada no chão, bem larga
  const edge = Math.abs(i) / 7;
  bubs.push({ x: i * 0.6 + (rb() - 0.5) * 0.15, y: 0.5 - edge * 0.12, r: (0.95 - edge * 0.35) * (0.85 + rb() * 0.3), row: 0, curl: rb() * 6.28 });
}
for (let i = -4; i <= 4; i++) { // fileira do meio: os calombos de cima
  const edge = Math.abs(i) / 4;
  bubs.push({ x: i * 0.8 + (rb() - 0.5) * 0.3, y: 1.3 - edge * 0.35, r: (0.88 - edge * 0.25) * (0.8 + rb() * 0.3), row: 1, curl: rb() * 6.28 });
}

// Lâminas de ar: base (x no chão), topo (x, y), largura.
const blades = [
  { bx: -2.2, tx: 0.6, ty: 5.6, w: 0.5 }, { bx: 0.2, tx: -2.6, ty: 5.2, w: 0.46 },
  { bx: 0.9, tx: 3.2, ty: 4.8, w: 0.42 }, { bx: 3.0, tx: 0.9, ty: 4.4, w: 0.38 },
];

const draw = (ctx0: Ctx, f: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k, U = 62 * k, gx = W / 2, gy = H - 30 * k;
  ctx0.clearRect(0, 0, W, H);

  // --- lâminas de ar (0..5), atrás da nuvem: saem dela, sobem, afinam e somem
  if (f <= 5) {
    const grow = [0.6, 1, 1.05, 1.08, 1.1, 1.12][f], thin = [1, 1, 0.85, 0.65, 0.45, 0.3][f], alpha = [1, 1, 1, 0.85, 0.6, 0.35][f];
    ctx0.save(); ctx0.globalAlpha = alpha;
    for (const b of blades) {
      const x0 = gx + b.bx * U, y0 = gy - 0.6 * U, x1 = gx + b.tx * U, y1 = gy - b.ty * U * grow, w = b.w * U * thin;
      const dx = x1 - x0, dy = y1 - y0, L = Math.hypot(dx, dy), nx = -dy / L, ny = dx / L;
      const shape = (ww: number, off: number) => {
        const p = new Path2D(), mx = x0 + dx * 0.35 + nx * off, my = y0 + dy * 0.35 + ny * off;
        p.moveTo(x0 + nx * off, y0 + ny * off);
        p.quadraticCurveTo(mx + nx * ww, my + ny * ww, x1, y1);
        p.quadraticCurveTo(mx - nx * ww, my - ny * ww, x0 + nx * off, y0 + ny * off);
        p.closePath(); return p;
      };
      ctx0.fillStyle = BLADE_EDGE; ctx0.fill(shape(w, 0));
      ctx0.fillStyle = BLADE; ctx0.fill(shape(w * 0.62, -w * 0.3));
    }
    ctx0.restore();
  }

  // --- a nuvem, numa camada própria (para sumir com transparência sem mostrar o traço por baixo)
  const lay = new OffscreenCanvas(W, H), ctx = lay.getContext("2d") as unknown as Ctx;
  const rise = [0.45, 0.72, 0.88, 0.97, 1.0, 1.0, 0.95, 0.86, 0.76, 0.66][f];   // altura das fileiras
  const spread = [0.55, 0.75, 0.87, 0.95, 1.0, 1.05, 1.1, 1.14, 1.18, 1.22][f];
  const shrink = [0.7, 0.88, 0.97, 1.02, 1.04, 1.04, 1.0, 0.94, 0.86, 0.76][f];
  const show = (b: Bub) => (b.row === 0 ? true : b.row === 1 ? f >= 1 && f <= 8 : f >= 2 && f <= 6);
  const vis = bubs.filter(show).map((b, i) => {
    const r = U * b.r * shrink * (b.row === 2 && f === 2 ? 0.7 : 1);
    return { x: gx + b.x * U * spread, y: gy - U * b.y * rise, r, curl: b.curl, big: b.r > 0.7, row: b.row, i };
  });
  const path = new Path2D();
  for (const b of vis) { path.moveTo(b.x + b.r, b.y); path.arc(b.x, b.y, b.r, 0, Math.PI * 2); }
  ctx.save();
  ctx.beginPath(); ctx.rect(0, 0, W, gy); ctx.clip();          // o chão corta reto embaixo
  ctx.lineJoin = "round"; ctx.strokeStyle = INK; ctx.lineWidth = 2 * 2.8 * k; ctx.stroke(path);
  ctx.save(); ctx.clip(path);
  ctx.fillStyle = SHADE; ctx.fill(path);
  ctx.save(); ctx.translate(-0.12 * U, -0.3 * U); ctx.fillStyle = BASE; ctx.fill(path); ctx.restore();
  // sombra embaixo de cada bolha de cima (separa os volumes): a bolha menos ela mesma subida
  const cres = new OffscreenCanvas(W, H), cc = cres.getContext("2d") as unknown as Ctx;
  for (const q of vis) {
    if (q.row === 0 || q.r < 0.5 * U || q.i % 2) continue;
    cc.globalCompositeOperation = "source-over"; cc.fillStyle = SHADE;
    cc.beginPath(); cc.arc(q.x, q.y, q.r, 0, Math.PI * 2); cc.fill();
    cc.globalCompositeOperation = "destination-out";
    cc.beginPath(); cc.arc(q.x + q.r * 0.12, q.y - q.r * 0.2, q.r * 0.98, 0, Math.PI * 2); cc.fill();
  }
  ctx.drawImage(cres as unknown as CanvasImageSource, 0, 0);
  // luz só na borda de cima da silhueta: a nuvem menos ela mesma deslocada para baixo
  const rim = new OffscreenCanvas(W, H), rc = rim.getContext("2d") as unknown as Ctx;
  rc.fillStyle = LIGHT; rc.fill(path);
  rc.globalCompositeOperation = "destination-out"; rc.translate(0.06 * U, 0.2 * U); rc.fill(path);
  ctx.drawImage(rim as unknown as CanvasImageSource, 0, 0);
  ctx.strokeStyle = INK; ctx.lineCap = "round"; ctx.lineWidth = 1.7 * k;   // caracóis nas bolhas grandes
  for (const b of vis) {
    if (!b.big || b.i % 2) continue;
    ctx.beginPath(); ctx.arc(b.x + Math.cos(b.curl) * b.r * 0.2, b.y + b.r * 0.12, b.r * 0.45, b.curl, b.curl + Math.PI * 1.2); ctx.stroke();
  }
  ctx.restore();
  ctx.restore();
  ctx0.globalAlpha = [1, 1, 1, 1, 1, 1, 0.95, 0.8, 0.58, 0.32][f];
  ctx0.drawImage(lay as unknown as CanvasImageSource, 0, 0);
  ctx0.globalAlpha = 1;
};

export const fumaca_pouso: Film = {
  meta: { title: "fumaca_pouso", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
