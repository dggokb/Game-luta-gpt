import { Ctx, Env, rng } from "./core";
import { Film } from "./film";
import { DUST, Puff, puffPath, renderDust } from "./poeira_kit";

// fumaca_pulo: o pé saindo do chão no pulo. Simétrica (o jogo não espelha), pequena e rápida:
// um sopro de bolhas para os dois lados (rabinho para o centro) e dois cortes de ar curtos em V.
// PNG transparente; efeito_video.py com --ancora chao.

const FPS = 24, BPM = 60, DURATION = 6;
const CUT = "#e2ddd5", CUT_EDGE = "#9f978d";

const rr = rng(47);
const side = Array.from({ length: 3 }, (_, i) => ({ vx: 1.2 + i * 0.9 + rr() * 0.3, r: 0.62 - i * 0.12, lift: 0.1 + rr() * 0.2 }));

const draw = (ctx: Ctx, f: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k, U = 50 * k, gx = W / 2, gy = H - 30 * k;
  ctx.clearRect(0, 0, W, H);

  // cortes de ar em V (0..2): curtos, saem do pé para cima e para fora
  if (f <= 2) {
    const g = [0.7, 1, 1.05][f], a = [1, 0.8, 0.4][f], th = [1, 0.8, 0.5][f];
    for (const s of [-1, 1]) {
      const x0 = gx + s * 0.25 * U, x1 = gx + s * 1.2 * U, y1 = gy - 2.6 * U * g, w = 0.2 * U * th;
      const shape = (ww: number, off: number) => { const p = new Path2D(); p.moveTo(x0 + off, gy - 0.2 * U); p.quadraticCurveTo(x0 + s * 0.2 * U + off + ww, gy - 1.3 * U * g, x1, y1); p.quadraticCurveTo(x0 + s * 0.2 * U + off - ww, gy - 1.3 * U * g, x0 + off, gy - 0.2 * U); return p; };
      ctx.save(); ctx.globalAlpha = a; ctx.fillStyle = CUT_EDGE; ctx.fill(shape(w, 0)); ctx.fillStyle = CUT; ctx.fill(shape(w * 0.55, -s * w * 0.3)); ctx.restore();
    }
  }

  // sopro de bolhas para os dois lados
  const t = (f + 1) / DURATION, e = 1 - Math.pow(1 - t, 2.2), shrink = t < 0.5 ? 1 : 1 - (t - 0.5) * 0.9;
  const shapes: Puff[] = [{ x: gx, y: gy - U * 0.3, r: U * 0.55 * (0.7 + 0.3 * e) * shrink }];
  for (const s of [-1, 1]) for (const b of side) {
    const r = U * b.r * (0.6 + 0.5 * Math.min(1, t * 2.5)) * shrink;
    shapes.push({ x: gx + s * U * (0.4 + b.vx * e * 0.8), y: gy - r * 0.75 - U * b.lift * e, r, tail: { a: s < 0 ? -0.2 : Math.PI + 0.2, len: r * (1.1 - t * 0.7), curl: s * 0.4 } });
  }
  renderDust(ctx, W, H, k, puffPath(shapes), gy, U * 0.18, [1, 1, 1, 0.9, 0.62, 0.32][f]);
};

export const fumaca_pulo: Film = {
  meta: { title: "fumaca_pulo", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
