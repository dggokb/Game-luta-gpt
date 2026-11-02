import { Ctx, Env, rng } from "./core";
import { Film } from "./film";
import { DUST, Puff, puffPath, renderDust, ribbon } from "./poeira_kit";

// super_pulo: a saída do super pulo. Simétrico (o jogo não espelha); --ancora chao.
// Lâminas de ar altas em V subindo, um anel de choque achatado no chão crescendo para os lados,
// fitas de vento subindo em espiral e a poeira baixa e larga.

const FPS = 24, BPM = 60, DURATION = 10;
const BLADE = "#e2ddd5", BLADE_EDGE = "#9f978d";

const rr = rng(83);
const puffsDef = Array.from({ length: 6 }, (_, i) => ({ x: 0.6 + i * 0.8, r: 0.8 - i * 0.08, d: i * 0.06 }));
const blades = [{ bx: -0.5, tx: -1.6, ty: 7.0, w: 0.4 }, { bx: 0.5, tx: 1.6, ty: 7.0, w: 0.4 }, { bx: -1.3, tx: -3.4, ty: 5.2, w: 0.32 }, { bx: 1.3, tx: 3.4, ty: 5.2, w: 0.32 }];

const draw = (ctx: Ctx, f: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k, U = 50 * k, gx = W / 2, gy = H - 30 * k;
  ctx.clearRect(0, 0, W, H);

  // lâminas de ar (0..5)
  if (f <= 5) {
    const grow = [0.55, 0.9, 1, 1.05, 1.08, 1.1][f], thin = [1, 1, 0.85, 0.65, 0.45, 0.3][f], al = [1, 1, 1, 0.8, 0.55, 0.3][f];
    ctx.save(); ctx.globalAlpha = al;
    for (const b of blades) {
      const x0 = gx + b.bx * U, y0 = gy - 0.3 * U, x1 = gx + b.tx * U, y1 = gy - b.ty * U * grow, w = b.w * U * thin;
      const dx = x1 - x0, dy = y1 - y0, L = Math.hypot(dx, dy), nx = -dy / L, ny = dx / L;
      const shape = (ww: number, off: number) => { const p = new Path2D(), mx = x0 + dx * 0.35 + nx * off, my = y0 + dy * 0.35 + ny * off; p.moveTo(x0 + nx * off, y0 + ny * off); p.quadraticCurveTo(mx + nx * ww, my + ny * ww, x1, y1); p.quadraticCurveTo(mx - nx * ww, my - ny * ww, x0 + nx * off, y0 + ny * off); p.closePath(); return p; };
      ctx.fillStyle = BLADE_EDGE; ctx.fill(shape(w, 0)); ctx.fillStyle = BLADE; ctx.fill(shape(w * 0.6, -w * 0.3 * Math.sign(b.bx || 1)));
    }
    ctx.restore();
  }

  // fitas de vento subindo em espiral (0..4)
  if (f <= 4) {
    const t = (f + 1) / 5;
    for (const s of [-1, 1]) {
      const pts: [number, number][] = [];
      for (let i = 0; i <= 14; i++) { const u = i / 14; pts.push([gx + s * U * (1.6 * Math.cos(u * 3.4 + 0.3) * (1 - u * 0.5)), gy - U * (0.3 + 6.2 * u * t)]); }
      ribbon(ctx, pts, U * 0.22 * (1 - t * 0.4), DUST.BASE, 0.8 * (1 - t * 0.6));
    }
  }

  // anel de choque achatado no chão (0..4)
  if (f <= 4) {
    const t = (f + 1) / 5, rx = U * (1 + 5.5 * (1 - Math.pow(1 - t, 2))), ry = rx * 0.2, al = [0.95, 0.9, 0.7, 0.45, 0.2][f], th = U * 0.28 * (1 - t * 0.6);
    ctx.save(); ctx.beginPath(); ctx.rect(0, 0, W, gy); ctx.clip();
    ctx.lineWidth = th * 2; ctx.strokeStyle = `rgba(159,151,141,${al})`; ctx.beginPath(); ctx.ellipse(gx, gy - ry - th, rx, ry, 0, 0, Math.PI * 2); ctx.stroke();
    ctx.lineWidth = th; ctx.strokeStyle = `rgba(240,236,228,${al})`; ctx.beginPath(); ctx.ellipse(gx, gy - ry - th * 1.1, rx, ry, 0, Math.PI * 1.02, Math.PI * 1.98); ctx.stroke();
    ctx.restore();
  }

  // poeira baixa e larga (1..9)
  if (f >= 1) {
    const t = f / 9, e = 1 - Math.pow(1 - t, 2), shrink = t < 0.6 ? 1 : 1 - (t - 0.6) * 0.8;
    const shapes: Puff[] = [];
    for (const s of [-1, 1]) for (const p of puffsDef) {
      const u = Math.max(0, (e - p.d) / (1 - p.d)); if (u <= 0) continue;
      const r = U * p.r * (0.55 + 0.45 * u) * shrink;
      shapes.push({ x: gx + s * U * p.x * (0.6 + 0.8 * u), y: gy - r * 0.6, r, tail: p.x > 3 ? { a: s < 0 ? -0.2 : Math.PI + 0.2, len: r * 0.6, curl: s * 0.5 } : undefined });
    }
    renderDust(ctx, W, H, k, puffPath(shapes), gy, U * 0.2, [1, 1, 1, 1, 1, 1, 0.95, 0.8, 0.58, 0.32][f]);
  }
};

export const super_pulo: Film = {
  meta: { title: "super_pulo", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
