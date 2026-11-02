import { Ctx, Env, rng } from "./core";
import { Film } from "./film";
import { DUST, Puff, puffPath, renderDust, pebble, ribbon } from "./poeira_kit";

// poeira_corrida: largada do dash. Quem corre olha para a DIREITA; o pé fica no ponto (FX, chão) e
// a poeira espirra para trás (esquerda): bolhas-vírgula (cabeça na frente, rabinho para o pé),
// uma onda serrilhada rente ao chão, pedrinhas e uma fita de vento. O jogo espelha no outro lado.
// PNG transparente; efeito_video.py com --origem FX,CHAO (o pé).

const FPS = 24, BPM = 60, DURATION = 8;
export const FX = 900, CHAO = 690;

const rr = rng(13);
const puffs = Array.from({ length: 6 }, (_, i) => ({
  born: i < 2 ? 0 : i < 4 ? 1 : 2, vx: 1.3 + rr() * 1.9, vy: 0.2 + (i % 3) * 0.55 + rr() * 0.3, r: 0.5 + rr() * 0.4, life: 5 + Math.floor(rr() * 2),
}));
const stones = Array.from({ length: 5 }, () => ({ vx: 2 + rr() * 2.5, vy: 1.2 + rr() * 1.2, r: 0.08 + rr() * 0.06, rot: rr() * 6 }));

const draw = (ctx: Ctx, f: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k, U = 50 * k, fx = FX * k, gy = CHAO * k;
  ctx.clearRect(0, 0, W, H);

  // fita de vento (0..3): da ponta do pé para trás e para cima
  if (f <= 3) {
    const t = (f + 1) / 4, pts: [number, number][] = [];
    for (let i = 0; i <= 12; i++) { const u = i / 12; pts.push([fx - U * (0.3 + 5.5 * u * t), gy - U * (0.3 + 1.6 * u * u * t + 0.4 * Math.sin(u * 3))]); }
    ribbon(ctx, pts, U * 0.22 * (1 - t * 0.5), DUST.BASE, 0.85 * (1 - t * 0.6));
  }

  // onda serrilhada rente ao chão (0..3): dentes que se inclinam para trás
  const shapes: Puff[] = [];
  if (f <= 3) {
    const t = (f + 1) / 4, len = U * (1.5 + 3.5 * t), h = U * (0.9 - 0.45 * t), wave = new Path2D(), n = 6;
    wave.moveTo(fx + U * 0.2, gy);
    for (let i = 0; i <= n; i++) {
      const x = fx - len * (i / n), hh = h * (1 - i / n * 0.8) * (i % 2 ? 0.45 : 1);
      wave.lineTo(x - (i % 2 ? 0 : U * 0.35), gy - hh);
    }
    wave.lineTo(fx - len - U * 0.3, gy); wave.closePath();
    renderDust(ctx, W, H, k, wave, gy, U * 0.18, f === 3 ? 0.6 : 1, 2.2);
  }

  // bolhas-vírgula
  for (const [i, p] of puffs.entries()) {
    if (f < p.born || f > p.born + p.life) continue;
    const t = (f - p.born + 1) / (p.life + 1), e = 1 - Math.pow(1 - t, 2);
    const r = U * p.r * (0.55 + 0.7 * Math.sin(Math.PI * Math.min(1, t * 1.3)) * (t < 0.75 ? 1 : 1 - (t - 0.75) * 2.4));
    shapes.push({ x: fx - U * (0.6 + p.vx * e * 2.4), y: gy - r * 0.8 - U * p.vy * e * 1.4, r, tail: i % 2 ? undefined : { a: 0.15, len: r * (0.75 - t * 0.4), curl: -0.9 } });
  }
  if (shapes.length) renderDust(ctx, W, H, k, puffPath(shapes), gy, U * 0.2, f >= 6 ? (f === 6 ? 0.7 : 0.4) : 1);

  // pedrinhas (0..5)
  if (f <= 5) {
    const t = (f + 1) / 6;
    for (const s of stones) {
      const x = fx - U * (0.4 + s.vx * t * 2), y = gy - U * (s.vy * t * 2.2 - 2.6 * t * t) - 3 * k;
      if (y < gy - 2 * k) pebble(ctx, x, y, U * s.r, s.rot - t * 5, k);
    }
  }
};

export const poeira_corrida: Film = {
  meta: { title: "poeira_corrida", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
