import { Ctx, Env, rng } from "./core";
import { Film } from "./film";

// defesa: golpe defendido. Luz fria sem contorno (o acerto é quente; a defesa é azul, para ler
// na hora a diferença). O golpe vem da ESQUERDA indo para a direita (o jogo espelha): a barreira
// é uma meia-lua virada para quem bate, com faíscas brancas voltando para trás. --ancora centro.

const FPS = 24, BPM = 60, DURATION = 6;
const CYAN = "#7fe8ff", BLUE = "#3a9cff", PALE = "#dff8ff", WHITE = "#ffffff";

const rr = rng(97);
const sparks = Array.from({ length: 9 }, () => ({ a: Math.PI + (rr() - 0.5) * 2.2, sp: 1.4 + rr() * 1.4, len: 0.3 + rr() * 0.3 }));

const glow = (ctx: Ctx, x: number, y: number, r: number, rgb: string, a: number) => {
  const g = ctx.createRadialGradient(x, y, 0, x, y, r); g.addColorStop(0, `rgba(${rgb},${a})`); g.addColorStop(0.5, `rgba(${rgb},${a * 0.4})`); g.addColorStop(1, `rgba(${rgb},0)`);
  ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill();
};

// Meia-lua: entre dois arcos, afinando nas pontas, virada para a esquerda.
const crescent = (cx: number, cy: number, r: number, th: number, span: number) => {
  const p = new Path2D(), a0 = Math.PI - span, a1 = Math.PI + span;
  p.arc(cx, cy, r, a0, a1);
  const steps = 16;
  for (let i = steps; i >= 0; i--) { const a = a0 + (a1 - a0) * (i / steps), w = th * Math.sin((i / steps) * Math.PI); p.lineTo(cx + Math.cos(a) * (r - w), cy + Math.sin(a) * (r - w)); }
  p.closePath(); return p;
};

const draw = (ctx: Ctx, f: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k, cx = W / 2, cy = H / 2, R = 80 * k;
  ctx.clearRect(0, 0, W, H);
  const t = f / (DURATION - 1);

  if (f <= 1) glow(ctx, cx, cy, R * (f ? 1.6 : 1.3), "160,230,255", f ? 0.45 : 0.75);

  // barreira (0..3): cresce um pouco, afina e se parte
  if (f <= 3) {
    const r = R * [0.85, 1.0, 1.1, 1.18][f], th = R * [0.42, 0.34, 0.2, 0.1][f], span = [1.05, 1.15, 1.2, 1.2][f];
    for (const [col, s] of [[BLUE, 1], [CYAN, 0.75], [PALE, 0.5], [WHITE, 0.28]] as const) {
      if (f >= 2 && col === WHITE) continue;
      ctx.fillStyle = col; ctx.fill(crescent(cx + th * (1 - s) * 0.5, cy, r - th * (1 - s) * 0.3, th * s, span * (0.85 + 0.15 * s)));
    }
    if (f >= 2) { // rachaduras: corta dois pedaços da meia-lua
      ctx.save(); ctx.globalCompositeOperation = "destination-out";
      for (const a of [Math.PI - 0.45, Math.PI + 0.35]) { ctx.beginPath(); ctx.arc(cx + Math.cos(a) * r, cy + Math.sin(a) * r, R * (f === 2 ? 0.1 : 0.2), 0, Math.PI * 2); ctx.fill(); }
      ctx.restore();
    }
  }
  // anel fino (1..3)
  if (f >= 1 && f <= 3) {
    ctx.strokeStyle = `rgba(127,232,255,${[0, 0.8, 0.5, 0.25][f]})`; ctx.lineWidth = R * 0.06;
    ctx.beginPath(); ctx.arc(cx, cy, R * [0, 0.7, 1.05, 1.35][f], 0, Math.PI * 2); ctx.stroke();
  }
  // estalo no ponto (0): estrela pequena de 4 pontas branca
  if (f === 0) {
    ctx.fillStyle = WHITE; ctx.beginPath();
    for (let i = 0; i < 8; i++) { const a = (i / 8) * Math.PI * 2, r = R * (i % 2 ? 0.12 : 0.55); ctx.lineTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r); }
    ctx.closePath(); ctx.fill();
  }
  // faíscas voltando para trás (0..5)
  for (const s of sparks) {
    const d = R * (0.5 + s.sp * (1 - Math.pow(1 - t, 2)) * 1.4), L = R * s.len * (1 - t * 0.7), w = R * 0.05 * (1 - t * 0.6);
    const x = cx + Math.cos(s.a) * d, y = cy + Math.sin(s.a) * d + R * 0.3 * t * t;
    ctx.save(); ctx.translate(x, y); ctx.rotate(s.a);
    ctx.globalAlpha = 1 - t * 0.7;
    ctx.fillStyle = CYAN; ctx.beginPath(); ctx.moveTo(L, 0); ctx.lineTo(0, w * 1.6); ctx.lineTo(-L * 0.6, 0); ctx.lineTo(0, -w * 1.6); ctx.closePath(); ctx.fill();
    ctx.fillStyle = WHITE; ctx.beginPath(); ctx.moveTo(L * 0.8, 0); ctx.lineTo(0, w * 0.7); ctx.lineTo(-L * 0.4, 0); ctx.lineTo(0, -w * 0.7); ctx.closePath(); ctx.fill();
    ctx.restore();
  }
};

export const defesa: Film = {
  meta: { title: "defesa", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
