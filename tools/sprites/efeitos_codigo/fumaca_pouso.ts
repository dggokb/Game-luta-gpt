import { Ctx, Env, rng } from "./core";
import { Film } from "./film";

// fumaca_pouso: poeira de aterrissagem de anime (estudo em docs/efeitos-referencia-fighterz.md).
// Fundo TRANSPARENTE (PNG); o tools/sprites/efeito_video.py lê a pasta com --ancora chao: o meio
// de baixo do efeito fica no chão, embaixo do lutador. Simétrica (o jogo espelha ao acaso).
//
// A poeira é DESENHO, não luz: bolhas em couve-flor em dois tons (sombra embaixo), contorno fino
// cinza-escuro, linhas de caracol dentro. No pouso, dois cortes de ar sobem cruzando em X.
// 0 pancada: faixa baixa + cortes de ar nascendo + pedrinhas   1-2 abre para os lados, cortes no alto
// 3-5 rola para fora e as pontas sobem   6-9 se quebra em bolhas menores e some

const FPS = 24, BPM = 60, DURATION = 10;
const BASE = "#d9d0c3", SHADE = "#a89a89", LIGHT = "#f1ece3", INK = "#5e5248";
const CUT = "235,230,222", CUT_EDGE = "190,182,170";

const clamp = (v: number, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const ease = (t: number) => 1 - Math.pow(1 - clamp(t), 2.4);

// Bolhas de cada lado (fixas): posição final x (em unidades), raio, atraso. A do meio fica.
const rb = rng(23);
type Bub = { x: number; r: number; lift: number; delay: number; curl: number };
const side: Bub[] = Array.from({ length: 4 }, (_, i) => ({
  x: 0.5 + i * 0.66 + (rb() - 0.5) * 0.1, r: 0.8 - i * 0.11 + (rb() - 0.5) * 0.06,
  lift: i === 3 ? 0.3 : 0, delay: i * 0.07, curl: rb() * Math.PI * 2,
}));
const center: Bub = { x: 0, r: 0.78, lift: 0, delay: 0, curl: 1.2 };
const rp = rng(61);
const pebbles = Array.from({ length: 7 }, (_, i) => ({ s: i % 2 ? 1 : -1, vx: 0.8 + rp() * 1.6, vy: 1.4 + rp() * 1.2, r: 0.05 + rp() * 0.05, rot: rp() * 6 }));

type B = { x: number; y: number; r: number; curl: number };

const draw = (ctxMain: Ctx, f: number, env: Env) => {
  let ctx = ctxMain;
  const k = env.scale, W = env.W * k, H = env.H * k, U = 100 * k, gx = W / 2, gy = H - 40 * k;
  ctx.clearRect(0, 0, W, H);

  // --- cortes de ar em X (0..4): sobem rápido, afinam e somem
  if (f <= 4) {
    const grow = [0.55, 1, 1.08, 1.12, 1.15][f], fade = [0.85, 0.8, 0.6, 0.35, 0.15][f], thin = [1, 1, 0.75, 0.55, 0.4][f];
    for (const s of [-1, 1]) {
      const bx = gx + s * 0.9 * U, tx = gx - s * 0.6 * U, ty = gy - 3.2 * U * grow, w = 0.22 * U * thin;
      const blade = new Path2D();
      blade.moveTo(bx - w, gy - 0.1 * U); blade.quadraticCurveTo(gx + s * 0.25 * U - w, gy - 1.7 * U * grow, tx, ty);
      blade.quadraticCurveTo(gx + s * 0.25 * U + w, gy - 1.6 * U * grow, bx + w, gy - 0.1 * U); blade.closePath();
      ctx.fillStyle = `rgba(${CUT_EDGE},${fade * 0.8})`; ctx.fill(blade);
      ctx.save(); ctx.translate(-s * w * 0.35, 0); ctx.scale(1, 1); ctx.fillStyle = `rgba(${CUT},${fade})`;
      ctx.beginPath();
      ctx.moveTo(bx - w * 0.45, gy - 0.1 * U); ctx.quadraticCurveTo(gx + s * 0.25 * U - w * 0.45, gy - 1.7 * U * grow, tx, ty);
      ctx.quadraticCurveTo(gx + s * 0.25 * U + w * 0.45, gy - 1.6 * U * grow, bx + w * 0.45, gy - 0.1 * U); ctx.closePath(); ctx.fill();
      ctx.restore();
    }
  }

  // --- a nuvem: bolhas que abrem para os lados; no fim encolhem, se separam e somem
  const open = ease((f + 1) / 6);                 // 0..1 até o quadro 5
  const end = clamp((f - 5) / 4);                 // 0..1 do 5 ao 9
  const alpha = [1, 1, 1, 1, 1, 1, 1, 0.85, 0.6, 0.3][f];
  const bubs: B[] = [];
  const add = (b: Bub, s: number) => {
    const t = clamp((open - b.delay) / (1 - b.delay));
    if (t <= 0.02) return;
    const spread = 0.35 + 0.65 * t, drift = 1 + end * 0.35;
    const r = U * b.r * (0.45 + 0.55 * t) * (1 - end * 0.3) * (f === 0 ? 0.75 : 1);
    const x = gx + s * U * b.x * spread * drift, y = gy - r * 0.55 - U * b.lift * t - U * 0.25 * end * (b.x / 3);
    bubs.push({ x, y, r, curl: b.curl + s });
    // bolhinha de cima (couve-flor)
    if (r > 0.25 * U) bubs.push({ x: x - s * r * 0.35, y: y - r * 0.6, r: r * 0.6, curl: b.curl + 2 });
  };
  add(center, 1);
  for (const b of side) { add(b, -1); add(b, 1); }
  if (bubs.length) {
    // No fim a nuvem se quebra: as bolhas se afastam do chão um pouco e separam
    const path = new Path2D();
    for (const b of bubs) { path.moveTo(b.x + b.r, b.y); path.arc(b.x, b.y, b.r, 0, Math.PI * 2); }
    const main = ctx, lay = new OffscreenCanvas(W, H);
    ctx = lay.getContext("2d") as unknown as Ctx;
    ctx.save();
    // o chão corta a nuvem reta embaixo
    ctx.beginPath(); ctx.rect(0, 0, W, gy); ctx.clip();
    // contorno só por fora: traço grosso do contorno e a base por cima
    ctx.lineJoin = "round"; ctx.strokeStyle = INK; ctx.lineWidth = 2 * 2.6 * k * (1 - end * 0.4); ctx.stroke(path);
    ctx.save(); ctx.clip(path);
    ctx.fillStyle = SHADE; ctx.fill(path);
    ctx.save(); ctx.translate(0, -0.16 * U); ctx.fillStyle = BASE; ctx.fill(path); ctx.restore();
    // brilho no alto de cada bolha
    ctx.fillStyle = LIGHT;
    for (const b of bubs) { ctx.beginPath(); ctx.arc(b.x - b.r * 0.2, b.y - b.r * 0.42, b.r * 0.42, 0, Math.PI * 2); ctx.fill(); }
    // linhas de caracol nas bolhas grandes
    ctx.strokeStyle = INK; ctx.lineCap = "round"; ctx.lineWidth = 1.5 * k;
    for (const [i, b] of bubs.entries()) {
      if (b.r < 0.4 * U || i % 2) continue;
      const a0 = b.curl, cr = b.r * 0.42;
      ctx.beginPath(); ctx.arc(b.x + Math.cos(a0) * b.r * 0.25, b.y + b.r * 0.15, cr, a0, a0 + Math.PI * 1.15); ctx.stroke();
    }
    ctx.restore();
    ctx.restore();
    ctx = main;
    ctx.globalAlpha = alpha; ctx.drawImage(lay as unknown as CanvasImageSource, 0, 0); ctx.globalAlpha = 1;
  }

  // --- pedrinhas (0..6): pulam e caem, contorno escuro
  if (f <= 6) {
    const t = (f + 1) / 7;
    for (const p of pebbles) {
      const x = gx + p.s * U * (0.3 + p.vx * t * 1.6), y = gy - U * (p.vy * t * 2.2 - 3.4 * t * t) - 4 * k;
      if (y > gy - 3 * k) continue;
      const r = U * p.r;
      ctx.save(); ctx.translate(x, y); ctx.rotate(p.rot + t * 4 * p.s);
      ctx.beginPath(); ctx.moveTo(-r, -r * 0.4); ctx.lineTo(-r * 0.2, -r); ctx.lineTo(r, -r * 0.3); ctx.lineTo(r * 0.6, r * 0.8); ctx.lineTo(-r * 0.7, r * 0.7); ctx.closePath();
      ctx.fillStyle = SHADE; ctx.fill(); ctx.lineWidth = 1.6 * k; ctx.strokeStyle = INK; ctx.stroke();
      ctx.restore();
    }
  }
};

export const fumaca_pouso: Film = {
  meta: { title: "fumaca_pouso", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
