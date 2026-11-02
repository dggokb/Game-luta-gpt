import { Ctx, Env, rng } from "./core";
import { Film } from "./film";
import { DUST, Puff, puffPath, renderDust, pebble } from "./poeira_kit";

// poeira_freada: freada do dash. Quem corria vinha para a DIREITA e finca o pé no ponto (FX, chão):
// a poeira é arrastada para a frente (direita), baixa, rente ao chão, desacelerando; riscos de
// arrasto no chão atrás do pé e pedrinhas rolando. O jogo espelha no outro lado.
// PNG transparente; efeito_video.py com --origem FX,CHAO (o pé).

const FPS = 24, BPM = 60, DURATION = 8;
export const FX = 380, CHAO = 690;

const rr = rng(29);
const puffs = Array.from({ length: 6 }, (_, i) => ({
  born: i < 3 ? 0 : 1, vx: 1.2 + rr() * 2.2, r: 0.42 + rr() * 0.3, life: 5 + Math.floor(rr() * 2), lift: rr() * 0.25,
}));
const stones = Array.from({ length: 4 }, () => ({ vx: 2.5 + rr() * 2.5, r: 0.07 + rr() * 0.05, rot: rr() * 6 }));

const draw = (ctx: Ctx, f: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k, U = 50 * k, fx = FX * k, gy = CHAO * k;
  ctx.clearRect(0, 0, W, H);

  // riscos de arrasto no chão (0..4): atrás do pé, encolhendo
  if (f <= 4) {
    const t = f / 4;
    ctx.save(); ctx.globalAlpha = 1 - t * 0.8; ctx.strokeStyle = DUST.INK; ctx.lineCap = "round";
    for (const [dy, len, w] of [[-3, 3.2, 2.6], [-9, 2.2, 2], [-15, 1.4, 1.6]] as const) {
      ctx.lineWidth = w * k; ctx.beginPath(); ctx.moveTo(fx - U * len * (1 - t * 0.6), gy + dy * k); ctx.lineTo(fx - U * 0.2, gy + dy * k); ctx.stroke();
    }
    ctx.restore();
  }

  // bolhas baixas arrastadas para a frente, rabinho para o pé
  const shapes: Puff[] = [];
  for (const p of puffs) {
    if (f < p.born || f > p.born + p.life) continue;
    const t = (f - p.born + 1) / (p.life + 1), e = 1 - Math.pow(1 - t, 2.6);
    const r = U * p.r * (0.6 + 0.6 * Math.sin(Math.PI * Math.min(1, t * 1.2)) * (t < 0.7 ? 1 : 1 - (t - 0.7) * 2.2));
    shapes.push({ x: fx + U * (0.4 + p.vx * e * 1.8), y: gy - r * 0.7 - U * p.lift * e, r, tail: { a: Math.PI + 0.1, len: r * (1.3 - t * 0.8), curl: 0.5 } });
  }
  if (shapes.length) renderDust(ctx, W, H, k, puffPath(shapes), gy, U * 0.18, f >= 6 ? (f === 6 ? 0.7 : 0.4) : 1);

  // pedrinhas rolando para a frente (0..5)
  if (f <= 5) {
    const t = (f + 1) / 6;
    for (const s of stones) {
      const x = fx + U * (0.3 + s.vx * (1 - Math.pow(1 - t, 2)) * 1.6), y = gy - U * s.r - U * 0.3 * Math.abs(Math.sin(t * 6 + s.rot));
      pebble(ctx, x, y, U * s.r, s.rot + t * 8, k);
    }
  }
};

export const poeira_freada: Film = {
  meta: { title: "poeira_freada", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
