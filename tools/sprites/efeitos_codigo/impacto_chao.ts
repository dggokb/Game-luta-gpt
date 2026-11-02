import { Ctx, Env, rng } from "./core";
import { Film } from "./film";
import { Puff, puffPath, renderDust, pebble } from "./poeira_kit";

// impacto_chao: o corpo batendo no chão (knockdown). Simétrico (o jogo não espelha); --ancora chao.
// Como no FighterZ: clarão baixo no instante, estilhaços de pedra cinza em dois tons SEM contorno
// saindo em leque, pedras escuras, brasas no chão e a nuvem de poeira rolando para os lados.

const FPS = 24, BPM = 60, DURATION = 10;
const ROCK = "#c9c3ba", ROCK_SHADE = "#8d867c";

const rr = rng(71);
const shards = Array.from({ length: 9 }, (_, i) => {
  const s = i % 2 ? 1 : -1, a = -Math.PI / 2 + s * (0.25 + rr() * 1.0);
  return { a, sp: 3.2 + rr() * 2.6, size: 0.35 + rr() * 0.3, rot: rr() * 6, spin: (rr() - 0.5) * 4 };
});
const stones = Array.from({ length: 6 }, (_, i) => ({ s: i % 2 ? 1 : -1, vx: 1 + rr() * 2.4, vy: 2.2 + rr() * 1.8, r: 0.08 + rr() * 0.06, rot: rr() * 6 }));
const embers = Array.from({ length: 12 }, () => ({ x: (rr() - 0.5) * 6, h: rr() * 0.5, s: 2 + rr() * 2.5, die: 3 + Math.floor(rr() * 4) }));
const puffsDef = Array.from({ length: 5 }, (_, i) => ({ x: 0.5 + i * 0.75, r: 0.85 - i * 0.1, d: i * 0.08 }));

const draw = (ctx: Ctx, f: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k, U = 50 * k, gx = W / 2, gy = H - 30 * k;
  ctx.clearRect(0, 0, W, H);

  // clarão baixo (0..1): leque de espinhos de luz para cima, sem contorno
  if (f <= 1) {
    const g = ctx.createRadialGradient(gx, gy, 0, gx, gy, U * 3.2);
    g.addColorStop(0, `rgba(255,236,170,${f ? 0.4 : 0.75})`); g.addColorStop(1, "rgba(255,236,170,0)");
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(gx, gy, U * 3.2, Math.PI, 0); ctx.fill();
    const fan = (scale: number, col: string) => {
      ctx.fillStyle = col; ctx.beginPath(); ctx.moveTo(gx - U * 2.2 * scale, gy);
      for (let i = 0; i <= 10; i++) { const a = Math.PI + (i / 10) * Math.PI, r = U * (i % 2 ? 0.9 : 2.0) * scale * (f ? 0.8 : 1); ctx.lineTo(gx + Math.cos(a) * r, gy + Math.sin(a) * r * 0.9); }
      ctx.closePath(); ctx.fill();
    };
    fan(1, "#ff8a1e"); fan(0.75, "#ffd64a"); fan(0.5, "#fff6cf");
  }

  // nuvem de poeira rolando para os dois lados (1..9)
  if (f >= 1) {
    const t = f / 9, e = 1 - Math.pow(1 - t, 2), shrink = t < 0.6 ? 1 : 1 - (t - 0.6) * 0.8;
    const shapes: Puff[] = [{ x: gx, y: gy - U * 0.6 * (0.6 + 0.4 * e), r: U * 0.95 * (0.6 + 0.4 * e) * shrink }];
    for (const s of [-1, 1]) for (const p of puffsDef) {
      const u = Math.max(0, (e - p.d) / (1 - p.d)); if (u <= 0) continue;
      const r = U * p.r * (0.55 + 0.45 * u) * shrink;
      shapes.push({ x: gx + s * U * p.x * (0.5 + 0.9 * u), y: gy - r * 0.7 - U * 0.3 * u * (p.x / 3), r, tail: p.x > 2 ? { a: s < 0 ? -0.3 : Math.PI + 0.3, len: r * 0.6, curl: s * 0.5 } : undefined });
    }
    renderDust(ctx, W, H, k, puffPath(shapes), gy, U * 0.22, [1, 1, 1, 1, 1, 1, 0.95, 0.8, 0.58, 0.32][f]);
  }

  // estilhaços de pedra (0..6), sem contorno, dois tons
  if (f <= 6) {
    const t = (f + 1) / 7;
    for (const q of shards) {
      const x = gx + Math.cos(q.a) * U * q.sp * t * 1.5, y = gy + Math.sin(q.a) * U * q.sp * t * 1.5 + U * 3.2 * t * t, r = U * q.size * (1 - t * 0.3);
      if (y > gy) continue;
      ctx.save(); ctx.translate(x, y); ctx.rotate(q.rot + q.spin * t);
      ctx.beginPath(); ctx.moveTo(0, -r * 1.3); ctx.lineTo(r * 0.7, -r * 0.1); ctx.lineTo(r * 0.3, r * 0.9); ctx.lineTo(-r * 0.6, r * 0.5); ctx.closePath();
      ctx.fillStyle = ROCK_SHADE; ctx.fill();
      ctx.beginPath(); ctx.moveTo(0, -r * 1.3); ctx.lineTo(r * 0.7, -r * 0.1); ctx.lineTo(-r * 0.1, r * 0.2); ctx.lineTo(-r * 0.6, r * 0.5); ctx.closePath();
      ctx.fillStyle = ROCK; ctx.fill();
      ctx.restore();
    }
    for (const p of stones) {
      const x = gx + p.s * U * (0.3 + p.vx * t * 1.6), y = gy - U * (p.vy * t * 2.2 - 3.6 * t * t) - 3 * k;
      if (y < gy - 2 * k) pebble(ctx, x, y, U * p.r, p.rot + t * 5 * p.s, k);
    }
  }

  // brasas no chão (0..6)
  for (const b of embers) {
    if (f > b.die) continue;
    const a = 1 - f / (b.die + 1), x = gx + b.x * U, y = gy - b.h * U - 4 * k, s = b.s * k;
    const g = ctx.createRadialGradient(x, y, 0, x, y, s * 3); g.addColorStop(0, `rgba(255,170,60,${0.5 * a})`); g.addColorStop(1, "rgba(255,170,60,0)");
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, s * 3, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = `rgba(255,214,90,${a})`; ctx.beginPath(); ctx.arc(x, y, s * 0.8, 0, Math.PI * 2); ctx.fill();
  }
};

export const impacto_chao: Film = {
  meta: { title: "impacto_chao", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
