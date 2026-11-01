import { Ctx, Env, rng } from "./core";
import { Film } from "./film";

// impacto_forte v2: acerto forte de anime, colorido, em camadas.
// Fundo verde chapado (#00FF00) para o tools/sprites/efeito_video.py do jogo. O golpe vai para a
// DIREITA (o jogo espelha). Quadro 0 e o último vazios; nenhuma cor do efeito é verde.
//
// Camadas: X de clarão, explosão em 3 tons (branco, amarelo, laranja) com sombra e brilho,
// anel de choque de lado, riscos de faísca, pedaços de energia, faíscas em losango e fumaça que
// se desfaz por buracos. Contorno de pincel: mais grosso do lado da sombra (baixo-direita).

const FPS = 24, BPM = 60, DURATION = 12; // efeito em 1..10 (~0,42 s)
const GREEN = "#00ff00";
const C = {
  white: "#ffffff", cream: "#fff4c2", yellow: "#ffd23a", orange: "#ff8a1f", red: "#e0461c",
  deep: "#8f2416", ink: "#1e0d14", smoke: "#e6dfe8", smokeShade: "#a698b2", smokeLight: "#ffffff",
};

const clamp = (v: number, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const ease = (t: number) => 1 - Math.pow(1 - clamp(t), 3);
const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

type Cel = { base: string; shade?: string; light?: string; ink?: string; line?: number; weight?: number; shadeOff?: number; lightScale?: number };

// Pinta uma forma com sombreado de anime: contorno com peso (deslocado para baixo-direita),
// base, sombra no lado de baixo-direita e um brilho menor no alto-esquerda.
const cel = (ctx: Ctx, p: Path2D, cx: number, cy: number, k: number, o: Cel) => {
  if (o.ink) {
    const w = (o.weight ?? 3) * k;
    ctx.save(); ctx.fillStyle = o.ink; ctx.translate(w * 0.6, w); ctx.fill(p); ctx.restore();
    ctx.lineJoin = "round"; ctx.lineWidth = (o.line ?? 2.5) * k; ctx.strokeStyle = o.ink; ctx.stroke(p);
  }
  ctx.save(); ctx.clip(p);
  ctx.fillStyle = o.shade ?? o.base; ctx.fill(p);
  const s = (o.shadeOff ?? 8) * k;
  ctx.save(); ctx.translate(-s * 0.7, -s); ctx.fillStyle = o.base; ctx.fill(p); ctx.restore();
  if (o.light) {
    const ls = o.lightScale ?? 0.55;
    ctx.save(); ctx.translate(cx - s * 1.4, cy - s * 1.8); ctx.scale(ls, ls); ctx.translate(-cx, -cy);
    ctx.fillStyle = o.light; ctx.fill(p); ctx.restore();
  }
  ctx.restore();
};

// Explosão: espinhos grossos de lados curvos, mais longos para a frente (direita).
const burst = (cx: number, cy: number, R: number, seed: number, n: number, opts: { push?: number; valley?: number; jag?: number; spin?: number } = {}) => {
  const r = rng(seed), p = new Path2D(), push = opts.push ?? 0.55, valley = opts.valley ?? 0.5, jag = opts.jag ?? 0.35;
  const pts: [number, number, number][] = [];
  for (let i = 0; i < n; i++) {
    const a = (i / n) * Math.PI * 2 + (opts.spin ?? 0) + (r() - 0.5) * 0.25;
    const fwd = Math.max(0, Math.cos(a));
    const len = R * (1 - jag * 0.5 + r() * jag) * (1 + push * fwd * fwd);
    pts.push([a, len, R * valley * (0.85 + r() * 0.3)]);
  }
  pts.forEach(([a, len, v], i) => {
    const [na, , nv] = pts[(i + 1) % n], na2 = i === n - 1 ? na + Math.PI * 2 : na;
    const va = (a + na2) / 2, vr = Math.min(v, nv);
    if (i === 0) { const pa = (pts[n - 1][0] - Math.PI * 2 + a) / 2; p.moveTo(cx + Math.cos(pa) * v, cy + Math.sin(pa) * v); }
    const c1r = (v + len) * 0.4, c2r = (len + vr) * 0.4;
    p.quadraticCurveTo(cx + Math.cos(a - 0.06) * c1r, cy + Math.sin(a - 0.06) * c1r, cx + Math.cos(a) * len, cy + Math.sin(a) * len);
    p.quadraticCurveTo(cx + Math.cos(a + 0.06) * c2r, cy + Math.sin(a + 0.06) * c2r, cx + Math.cos(va) * vr, cy + Math.sin(va) * vr);
  });
  p.closePath();
  return p;
};

// Lâmina afinando nas pontas (traço do X, riscos de faísca).
const blade = (x0: number, y0: number, x1: number, y1: number, w: number, bulge = 0.35) => {
  const dx = x1 - x0, dy = y1 - y0, L = Math.hypot(dx, dy) || 1, nx = -dy / L, ny = dx / L, p = new Path2D();
  const mx = x0 + dx * bulge, my = y0 + dy * bulge;
  p.moveTo(x0, y0);
  p.quadraticCurveTo(mx + nx * w * 1.3, my + ny * w * 1.3, x1, y1);
  p.quadraticCurveTo(mx - nx * w * 1.3, my - ny * w * 1.3, x0, y0);
  p.closePath();
  return p;
};

// Anel de choque visto de lado (a força vai para a direita): faixa elíptica, aberta em arcos.
const ring = (cx: number, cy: number, ry: number, band: number, gaps: number, seed: number) => {
  const r = rng(seed), p = new Path2D(), rx = ry * 0.36, segs = 5;
  for (let s = 0; s < segs; s++) {
    const a0 = (s / segs) * Math.PI * 2 + r() * 0.2, a1 = a0 + (Math.PI * 2 / segs) * (1 - gaps * (0.6 + r() * 0.8));
    if (a1 <= a0 + 0.05) continue;
    const st = 20, outer: [number, number][] = [], inner: [number, number][] = [];
    for (let i = 0; i <= st; i++) {
      const a = lerp(a0, a1, i / st), taper = Math.sin((i / st) * Math.PI);
      outer.push([cx + Math.cos(a) * (rx + band * taper * 0.5), cy + Math.sin(a) * (ry + band * taper)]);
      inner.push([cx + Math.cos(a) * rx, cy + Math.sin(a) * ry]);
    }
    outer.forEach(([x, y], i) => (i ? p.lineTo(x, y) : p.moveTo(x, y)));
    inner.reverse().forEach(([x, y]) => p.lineTo(x, y));
    p.closePath();
  }
  return p;
};

// Pedaço de energia: língua de fogo curva, a cabeça redonda na frente, a cauda para trás.
const chunk = (x: number, y: number, a: number, len: number, w: number, curl: number) => {
  const p = new Path2D(), ca = Math.cos(a), sa = Math.sin(a), nx = -sa, ny = ca;
  const head: [number, number] = [x + ca * len * 0.35, y + sa * len * 0.35];
  const tail: [number, number] = [x - ca * len * 0.65 + nx * curl * len, y - sa * len * 0.65 + ny * curl * len];
  p.moveTo(tail[0], tail[1]);
  p.quadraticCurveTo(x + nx * w * 1.2, y + ny * w * 1.2, head[0] + nx * w * 0.55, head[1] + ny * w * 0.55);
  p.arc(head[0], head[1], w * 0.55, a + Math.PI / 2, a - Math.PI / 2, true);
  p.quadraticCurveTo(x - nx * w * 1.2, y - ny * w * 1.2, tail[0], tail[1]);
  p.closePath();
  return p;
};

const diamond = (x: number, y: number, a: number, len: number, w: number) => {
  const p = new Path2D(), ca = Math.cos(a), sa = Math.sin(a);
  p.moveTo(x + ca * len, y + sa * len); p.lineTo(x - sa * w, y + ca * w);
  p.lineTo(x - ca * len * 0.6, y - sa * len * 0.6); p.lineTo(x + sa * w, y - ca * w); p.closePath();
  return p;
};

// Fumaça: bolhas unidas, numa camada própria; se desfaz por buracos que crescem.
type Puff = { x: number; y: number; r: number; vx: number; vy: number };
const rs = rng(77);
const puffs: Puff[] = Array.from({ length: 9 }, (_, i) => {
  // em volta do ponto, mais cheia atrás (esquerda); a fumaça sobe um pouco e vai para a frente
  const a = (i / 9) * Math.PI * 2 + (rs() - 0.5) * 0.4, back = Math.max(0, -Math.cos(a));
  const d = i === 0 ? 0 : 0.22 + rs() * 0.12 + back * 0.1;
  return { x: Math.cos(a) * d, y: Math.sin(a) * d * 0.8, r: (i === 0 ? 0.34 : 0.2 + rs() * 0.1) + back * 0.06, vx: i === 0 ? 0.1 : Math.cos(a) * 0.3 + 0.12, vy: i === 0 ? -0.1 : Math.sin(a) * 0.22 - 0.14 };
});
const puffAt = (q: Puff, cx: number, cy: number, R: number, t: number) => {
  const g = ease(t);
  return { x: cx + R * (q.x + q.vx * g), y: cy + R * (q.y + q.vy * g), rad: R * q.r * (0.55 + g * 0.75) };
};
const smokePath = (cx: number, cy: number, R: number, t: number, seed: number) => {
  const r = rng(seed), p = new Path2D();
  for (const q of puffs) {
    const { x, y, rad } = puffAt(q, cx, cy, R, t);
    p.moveTo(x + rad, y); p.arc(x, y, rad, 0, Math.PI * 2);
    const ba = r() * Math.PI * 2, bx = x + Math.cos(ba) * rad * 0.7, by = y + Math.sin(ba) * rad * 0.7, br = rad * 0.55;
    p.moveTo(bx + br, by); p.arc(bx, by, br, 0, Math.PI * 2);
  }
  return p;
};
const smokeHoles = (ctx: Ctx, cx: number, cy: number, R: number, t: number, erode: number, seed: number) => {
  if (erode <= 0) return;
  const r = rng(seed + 5);
  ctx.save(); ctx.globalCompositeOperation = "destination-out"; ctx.fillStyle = "#000";
  for (const q of puffs) {
    const { x, y, rad } = puffAt(q, cx, cy, R, t);
    for (let h = 0; h < 2; h++) { // dois furos grandes por bolha, vindos da borda
      const a = r() * Math.PI * 2, d = rad * (0.75 + r() * 0.4), hr = rad * (0.25 + erode * 0.75) * (0.8 + r() * 0.4);
      ctx.beginPath(); ctx.arc(x + Math.cos(a) * d, y + Math.sin(a) * d, hr, 0, Math.PI * 2); ctx.fill();
    }
  }
  ctx.restore();
};

const rStreak = rng(31);
const streaks = Array.from({ length: 14 }, (_, i) => {
  const a = i < 9 ? (rStreak() - 0.5) * 2.0 : Math.PI * 2 * rStreak(); // a maioria para a frente
  return { a, len: 0.5 + rStreak() * 0.6, w: 0.03 + rStreak() * 0.025, sp: 0.8 + rStreak() * 0.6 };
});
const rChunk = rng(53);
const chunks = Array.from({ length: 11 }, (_, i) => {
  const a = (i / 11) * Math.PI * 2 + (rChunk() - 0.5) * 0.4;
  return { a, sp: (0.55 + rChunk() * 0.45) * (1 + 0.5 * Math.max(0, Math.cos(a))), size: 0.13 + rChunk() * 0.08, curl: (rChunk() - 0.5) * 0.5 };
});
const rSpark = rng(91);
const sparks = Array.from({ length: 12 }, () => ({ a: (rSpark() - 0.5) * 2.6, sp: 1.1 + rSpark() * 0.8, size: 0.035 + rSpark() * 0.03, d: rSpark() * 0.3 }));

const draw = (ctx: Ctx, local: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k;
  ctx.fillStyle = GREEN; ctx.fillRect(0, 0, W, H);
  const f = local;
  if (f < 1 || f > 10) return;
  const R = 150 * k, cx = W / 2 - R * 0.15, cy = H / 2, boil = 500 + Math.floor(f / 2) * 13;

  // --- fumaça (atrás de tudo), do quadro 3 ao 10, numa camada própria para furar
  if (f >= 3) {
    const t = (f - 3) / 7, erode = f < 6 ? 0 : clamp((f - 6) / 4) * 1.1;
    const lay = new OffscreenCanvas(W, H), lc = lay.getContext("2d") as unknown as Ctx;
    cel(lc, smokePath(cx, cy, R, t, boil), cx, cy, k, { base: C.smoke, shade: C.smokeShade, light: C.smokeLight, ink: C.ink, line: 2.4, weight: 2.6, shadeOff: 10, lightScale: 0.6 });
    smokeHoles(lc, cx, cy, R, t, erode, boil);
    ctx.drawImage(lay as unknown as CanvasImageSource, 0, 0);
  }

  // --- riscos de faísca: saem do centro e voam para fora (2..6)
  if (f >= 2 && f <= 6) {
    const t = (f - 2) / 4;
    for (const s of streaks) {
      const r0 = R * (0.55 + ease(t) * 1.05 * s.sp), r1 = r0 + R * s.len * (1 - t * 0.7), w = R * s.w * (1 - t * 0.6);
      if (w < 1.2 * k) continue;
      const ca = Math.cos(s.a), sa = Math.sin(s.a);
      const p = blade(cx + ca * r1, cy + sa * r1, cx + ca * r0, cy + sa * r0, w, 0.25);
      cel(ctx, p, cx + ca * (r0 + r1) / 2, cy + sa * (r0 + r1) / 2, k, { base: C.cream, shade: C.yellow, ink: C.ink, line: 2, weight: 1.6, shadeOff: 2 });
    }
  }

  // --- anel de choque de lado (2..5)
  if (f >= 2 && f <= 5) {
    const t = (f - 2) / 3, ry = R * lerp(0.62, 1.35, ease(t)), band = R * lerp(0.2, 0.05, t);
    const p = ring(cx + R * lerp(0.15, 0.55, t), cy, ry, band, lerp(0.05, 0.55, t), boil + 3);
    cel(ctx, p, cx, cy, k, { base: C.white, shade: C.yellow, ink: C.ink, line: 2.4, weight: 2.4, shadeOff: 5 });
  }

  // --- explosão em 3 tons (1..4); no 4 o miolo rompe
  if (f <= 4) {
    const size = [0, 0.62, 1.0, 1.06, 0.92][f];
    const outer = burst(cx, cy, R * size, 11 + Math.floor(f / 2), 11, { push: 0.6, valley: 0.52 });
    cel(ctx, outer, cx, cy, k, { base: C.orange, shade: C.red, light: C.yellow, ink: C.ink, line: 3.2, weight: 4.5, shadeOff: 12, lightScale: 0.7 });
    if (f <= 3) {
      cel(ctx, burst(cx + R * 0.05, cy, R * size * 0.68, 23 + f, 9, { push: 0.45, valley: 0.55, spin: 0.3 }), cx, cy, k, { base: C.yellow, shade: C.orange, light: C.cream, shadeOff: 7, lightScale: 0.6 });
      cel(ctx, burst(cx + R * 0.08, cy, R * size * 0.38, 37 + f, 7, { push: 0.3, valley: 0.7, jag: 0.25 }), cx, cy, k, { base: C.white, shade: C.cream, shadeOff: 4 });
    } else {
      cel(ctx, burst(cx + R * 0.1, cy, R * 0.42, 41, 8, { push: 0.3, valley: 0.6, jag: 0.4 }), cx, cy, k, { base: C.deep, shade: C.ink, shadeOff: 4 });
    }
  }

  // --- a explosão quebra em pedaços de energia (4..9)
  if (f >= 5 && f <= 9) {
    const t = (f - 4.4) / 5;
    for (const c of chunks) {
      const g = ease(t), d = R * (0.75 + c.sp * g * 0.9), size = R * c.size * 0.8 * (1 - Math.pow(t, 1.4));
      if (size < 2 * k) continue;
      const x = cx + Math.cos(c.a) * d, y = cy + Math.sin(c.a) * d + R * 0.25 * t * t;
      const hot = t < 0.45;
      cel(ctx, chunk(x, y, c.a + c.curl * t * 1.2, size * 3.8, size, c.curl * 0.6), x, y, k, { base: hot ? C.yellow : C.orange, shade: hot ? C.orange : C.red, light: hot ? C.cream : C.yellow, ink: C.ink, line: 2.4, weight: 2.4, shadeOff: size * 0.25 / k, lightScale: 0.5 });
    }
  }

  // --- faíscas em losango com gravidade (4..10)
  if (f >= 4) {
    const t = (f - 4) / 6;
    for (const s of sparks) {
      const u = clamp((t - s.d * 0.4) / 0.9); if (u <= 0 || u >= 1) continue;
      const d = R * (0.9 + s.sp * ease(u) * 0.9), x = cx + Math.cos(s.a) * d, y = cy + Math.sin(s.a) * d + R * 0.5 * u * u;
      const size = R * s.size * (1 - u * 0.8), va = Math.atan2(Math.sin(s.a) * s.sp + 1.2 * u, Math.cos(s.a) * s.sp);
      cel(ctx, diamond(x, y, va, size * 2.2, size * 0.7), x, y, k, { base: C.cream, shade: C.yellow, ink: C.ink, line: 1.8, weight: 1.4, shadeOff: 1.5 });
    }
  }

  // --- X de clarão por cima (1..2)
  if (f <= 2) {
    const L = R * (f === 1 ? 1.7 : 2.0), w = R * (f === 1 ? 0.075 : 0.045);
    for (const a of [-0.42, 0.5]) {
      const ca = Math.cos(a), sa = Math.sin(a);
      cel(ctx, blade(cx - ca * L * 0.55, cy - sa * L * 0.55, cx + ca * L * 0.6, cy + sa * L * 0.6, w, 0.5), cx, cy, k, { base: C.white, shade: C.cream, ink: C.ink, line: 2.4, weight: 2.4, shadeOff: 2 });
    }
  }
};

export const impacto_forte: Film = {
  meta: { title: "impacto_forte", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
