import { Ctx, Env } from "./core";
import { Film } from "./film";

// arco_golpe: o rastro do golpe forte, de baixo para cima e para a frente (direita), em volta do
// ombro (o centro do quadro). Luz branca sem contorno que afina para a cauda, como os rastros de
// golpe do FighterZ. Rápido: 4 quadros. O jogo espelha pelo lado; --ancora centro.

const FPS = 24, BPM = 60, DURATION = 4;
const A0 = 2.1, A1 = -1.15; // ângulos (tela, y para baixo): começa embaixo-atrás, termina no alto-frente

// Rastro: faixa curva entre ângulos [ta, ha]; grossa na cabeça (ha), fina na cauda (ta).
const smear = (cx: number, cy: number, r: number, ta: number, ha: number, th: number) => {
  const p = new Path2D(), steps = 40, outer: [number, number][] = [], inner: [number, number][] = [];
  for (let i = 0; i <= steps; i++) {
    const u = i / steps, a = ta + (ha - ta) * u, w = th * Math.pow(u, 1.6) * (u > 0.8 ? Math.sqrt(Math.max(0, Math.cos((u - 0.8) / 0.2 * Math.PI / 2))) : 1);
    outer.push([cx + Math.cos(a) * (r + w * 0.35), cy + Math.sin(a) * (r + w * 0.35)]);
    inner.push([cx + Math.cos(a) * (r - w * 0.65), cy + Math.sin(a) * (r - w * 0.65)]);
  }
  outer.forEach(([x, y], i) => (i ? p.lineTo(x, y) : p.moveTo(x, y)));
  inner.reverse().forEach(([x, y]) => p.lineTo(x, y)); p.closePath(); return p;
};

const draw = (ctx: Ctx, f: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k, cx = W / 2, cy = H / 2, R = 190 * k;
  ctx.clearRect(0, 0, W, H);
  const head = [A0 + (A1 - A0) * 0.55, A1, A1 - 0.08, A1 - 0.12][f];
  const tail = [A0, A0, A0 + (A1 - A0) * 0.55, A0 + (A1 - A0) * 0.85][f];
  const th = R * [0.32, 0.4, 0.26, 0.12][f], al = [0.9, 1, 0.75, 0.45][f];
  ctx.save(); ctx.globalAlpha = al;
  ctx.fillStyle = "rgba(170,215,255,0.55)"; ctx.fill(smear(cx, cy, R, tail, head, th * 1.25));   // halo azulado
  ctx.fillStyle = "#e8f4ff"; ctx.fill(smear(cx, cy, R, tail, head, th));
  ctx.fillStyle = "#ffffff"; ctx.fill(smear(cx, cy, R * 1.02, tail + (head - tail) * 0.25, head, th * 0.5));
  ctx.restore();
  // linhas de velocidade finas por dentro do arco (1..2)
  if (f === 1 || f === 2) {
    ctx.save(); ctx.globalAlpha = f === 1 ? 0.7 : 0.35; ctx.strokeStyle = "#ffffff"; ctx.lineCap = "round";
    for (const [dr, w] of [[-0.28, 3], [-0.42, 2], [-0.55, 1.5]] as const) {
      ctx.lineWidth = w * k; ctx.beginPath(); ctx.arc(cx, cy, R * (1 + dr), tail + (head - tail) * 0.45, head, true); ctx.stroke();
    }
    ctx.restore();
  }
};

export const arco_golpe: Film = {
  meta: { title: "arco_golpe", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
