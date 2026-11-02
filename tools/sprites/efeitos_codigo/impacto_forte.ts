import { Ctx, Env, rng } from "./core";
import { Film } from "./film";

// impacto_forte v3: acerto de anime, luz sem contorno (o contorno é dos personagens).
// Fundo TRANSPARENTE: os quadros saem em PNG e o tools/sprites/efeito_video.py lê a pasta.
// O ponto do golpe é o centro do quadro; o golpe vai para a DIREITA (o jogo espelha).
//
// 0 contato: estrela de espinhos + raios de luz atravessando + faíscas miúdas
// 1 X de espinhos cruzados        2 estrela redonda densa com brilho e pontinhos escuros
// 3 o miolo abre (vermelho-escuro) e a borda vira línguas de fogo
// 4..7 as línguas se soltam, voam para fora e encolhem   8 só faíscas

const FPS = 24, BPM = 60, DURATION = 9;
const RED = "#e8321c", ORANGE = "#ff8a1e", YELLOW = "#ffd64a", CREAM = "#fff6cf", WHITE = "#ffffff";
const DARK = "#7a1a10", SPECK = "#2a0c08";

type P = [number, number];
const clamp = (v: number, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const ease = (t: number) => 1 - Math.pow(1 - clamp(t), 2.2);
const at = (cx: number, cy: number, a: number, r: number): P => [cx + Math.cos(a) * r, cy + Math.sin(a) * r];

// Brilho macio: degradê radial da cor para transparente.
const glow = (ctx: Ctx, x: number, y: number, r: number, rgb: string, alpha: number) => {
  const g = ctx.createRadialGradient(x, y, 0, x, y, r);
  g.addColorStop(0, `rgba(${rgb},${alpha})`);
  g.addColorStop(0.45, `rgba(${rgb},${alpha * 0.45})`);
  g.addColorStop(1, `rgba(${rgb},0)`);
  ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill();
};

// Estrela de espinhos (lados côncavos), mais longa para a frente.
const star = (cx: number, cy: number, R: number, n: number, seed: number, push = 0.35, valley = 0.52) => {
  const r = rng(seed), p = new Path2D(), pts: [number, number, number][] = [];
  for (let i = 0; i < n; i++) {
    const a = (i / n) * Math.PI * 2 + (r() - 0.5) * (Math.PI / n);
    const fwd = Math.max(0, Math.cos(a));
    pts.push([a, R * (0.72 + r() * 0.4) * (1 + push * fwd), R * valley * (0.85 + r() * 0.3)]);
  }
  pts.forEach(([a, len, v], i) => {
    const [na, , nv] = pts[(i + 1) % n], nb = i === n - 1 ? na + Math.PI * 2 : na, va = (a + nb) / 2, vr = Math.min(v, nv);
    if (i === 0) { const pa = (pts[n - 1][0] - Math.PI * 2 + a) / 2; p.moveTo(...at(cx, cy, pa, v)); }
    const c1 = at(cx, cy, a - 0.05, (v + len) * 0.42), tip = at(cx, cy, a, len), c2 = at(cx, cy, a + 0.05, (len + vr) * 0.42);
    p.quadraticCurveTo(c1[0], c1[1], tip[0], tip[1]);
    const v2 = at(cx, cy, va, vr); p.quadraticCurveTo(c2[0], c2[1], v2[0], v2[1]);
  });
  p.closePath();
  return p;
};

// Pinta em camadas de cor (de fora para dentro), cada uma um pouco menor, puxada para o centro.
const layered = (ctx: Ctx, make: (s: number) => Path2D, colors: string[], scales: number[]) => {
  colors.forEach((c, i) => { ctx.fillStyle = c; ctx.fill(make(scales[i])); });
};

// Lança afiada com borda serrilhada (o X e os raios).
const spear = (cx: number, cy: number, a: number, back: number, front: number, w: number, seed: number) => {
  const r = rng(seed), p = new Path2D(), ca = Math.cos(a), sa = Math.sin(a), nx = -sa, ny = ca;
  const pt = (d: number, s: number): P => [cx + ca * d + nx * s, cy + sa * d + ny * s];
  p.moveTo(...pt(front, 0));
  const steps = 6;
  for (let i = 1; i <= steps; i++) { const d = front - (front + back) * (i / steps), k = Math.sin((i / steps) * Math.PI); p.lineTo(...pt(d, w * k * (i % 2 ? 1 : 0.6 + r() * 0.3))); }
  for (let i = steps - 1; i >= 1; i--) { const d = front - (front + back) * (i / steps), k = Math.sin((i / steps) * Math.PI); p.lineTo(...pt(d, -w * k * (i % 2 ? 1 : 0.6 + r() * 0.3))); }
  p.closePath();
  return p;
};

// Raio de luz: faixa fina que afina nas pontas, atravessando o ponto.
const ray = (ctx: Ctx, cx: number, cy: number, a: number, L: number, w: number, alpha: number) => {
  const ca = Math.cos(a), sa = Math.sin(a), nx = -sa, ny = ca;
  const shape = (ww: number) => {
    const p = new Path2D();
    p.moveTo(cx - ca * L * 0.45, cy - sa * L * 0.45);
    p.quadraticCurveTo(cx + nx * ww, cy + ny * ww, cx + ca * L, cy + sa * L);
    p.quadraticCurveTo(cx - nx * ww, cy - ny * ww, cx - ca * L * 0.45, cy - sa * L * 0.45);
    return p;
  };
  ctx.globalAlpha = alpha * 0.85; ctx.fillStyle = YELLOW; ctx.fill(shape(w));
  ctx.globalAlpha = alpha; ctx.fillStyle = CREAM; ctx.fill(shape(w * 0.45));
  ctx.globalAlpha = 1;
};

// Língua de fogo: base redonda para dentro, ponta afiada para fora, um dente de cada lado.
const tongue = (x: number, y: number, a: number, len: number, w: number, s: number) => {
  const p = new Path2D(), ca = Math.cos(a), sa = Math.sin(a), nx = -sa, ny = ca, L = len * s, W = w * s;
  const pt = (d: number, o: number): P => [x + ca * d + nx * o, y + sa * d + ny * o];
  // ponta para fora, dois dentes de chama de cada lado, base redonda para dentro
  p.moveTo(...pt(L * 0.8, 0));
  p.quadraticCurveTo(...pt(L * 0.5, W * 0.3), ...pt(L * 0.42, W * 0.42));
  p.lineTo(...pt(L * 0.55, W * 0.78));
  p.quadraticCurveTo(...pt(L * 0.25, W * 0.7), ...pt(L * 0.12, W * 0.82));
  p.lineTo(...pt(L * 0.22, W * 1.15));
  p.quadraticCurveTo(...pt(-L * 0.18, W * 1.05), ...pt(-L * 0.3, 0));
  p.quadraticCurveTo(...pt(-L * 0.18, -W * 1.0), ...pt(L * 0.1, -W * 0.95));
  p.lineTo(...pt(L * 0.02, -W * 0.6));
  p.quadraticCurveTo(...pt(L * 0.3, -W * 0.62), ...pt(L * 0.38, -W * 0.4));
  p.lineTo(...pt(L * 0.3, -W * 0.15));
  p.quadraticCurveTo(...pt(L * 0.55, -W * 0.22), ...pt(L * 0.8, 0));
  p.closePath();
  return p;
};

// Línguas fixas (seed): mais para a frente e para cima, como no FighterZ.
const rt = rng(41);
const tongues = Array.from({ length: 9 }, (_, i) => {
  const a = (i / 9) * Math.PI * 2 + (rt() - 0.5) * 0.5, up = Math.max(0, -Math.sin(a)), fwd = Math.max(0, Math.cos(a));
  const big = 0.75 + rt() * 0.6;
  return { a, sp: 0.25 + rt() * 0.25 + 0.25 * fwd + 0.1 * up, len: 0.85 * big, w: 0.3 * big, curl: (rt() < 0.5 ? -1 : 1) * (0.35 + rt() * 0.4), life: 0.7 + rt() * 0.45 };
});
// Faíscas miúdas: muitas, numa área bem maior que a estrela.
const re = rng(77);
const embers = Array.from({ length: 46 }, () => {
  const a = re() * Math.PI * 2, fwd = Math.max(0, Math.cos(a));
  return { a, d0: 0.25 + re() * 0.7, sp: 0.3 + re() * 0.8 + fwd * 0.4, size: 2.2 + re() * 3, dash: re() < 0.45, born: Math.floor(re() * 3), die: 4 + Math.floor(re() * 5) };
});
const rs = rng(19);
const specks = Array.from({ length: 16 }, () => ({ a: rs() * Math.PI * 2, d: rs() * 0.55, s: 1.5 + rs() * 3.5 }));

const draw = (ctx: Ctx, f: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k, cx = W / 2, cy = H / 2, R = 160 * k;
  ctx.clearRect(0, 0, W, H);

  // faíscas miúdas (atrás de tudo), do quadro 0 ao 8
  for (const e of embers) {
    if (f < e.born || f > e.die) continue;
    const u = (f - e.born) / Math.max(1, e.die - e.born), d = R * (e.d0 + e.sp * ease(u));
    const [x, y] = at(cx, cy, e.a, d), yy = y + R * 0.25 * u * u, s = e.size * k * (1 - u * 0.6);
    glow(ctx, x, yy, s * 3, "255,214,74", 0.35 * (1 - u));
    ctx.fillStyle = u < 0.5 ? CREAM : YELLOW;
    if (e.dash) { ctx.save(); ctx.translate(x, yy); ctx.rotate(e.a); ctx.fillRect(-s * 1.8, -s * 0.4, s * 3.6, s * 0.8); ctx.restore(); }
    else { ctx.beginPath(); ctx.arc(x, yy, s * 0.7, 0, Math.PI * 2); ctx.fill(); }
  }

  if (f === 0) { // contato
    glow(ctx, cx, cy, R * 1.9, "255,230,150", 0.55);
    for (const [a, L, w] of [[-0.12, 3.6, 0.05], [0.42, 3.0, 0.035], [-0.75, 2.7, 0.03], [2.6, 2.4, 0.03]] as const) ray(ctx, cx, cy, a, R * L, R * w, 1);
    layered(ctx, (s) => star(cx + R * 0.04 * (1 - s), cy, R * 1.05 * s, 16, 101), [RED, ORANGE, YELLOW, CREAM, WHITE], [1, 0.86, 0.7, 0.52, 0.34]);
    return;
  }
  if (f === 1) { // o X
    glow(ctx, cx, cy, R * 1.6, "255,220,120", 0.5);
    for (const [a, L, w] of [[-0.12, 3.7, 0.03], [0.42, 3.1, 0.02]] as const) ray(ctx, cx, cy, a, R * L, R * w, 0.6);
    for (const [a, seed] of [[-0.62, 7], [0.78, 9]] as const) {
      layered(ctx, (s) => spear(cx, cy, a, R * 1.1 * s, R * 1.35 * s, R * 0.2 * s, seed), [RED, ORANGE, YELLOW, CREAM], [1, 0.84, 0.66, 0.42]);
    }
    layered(ctx, (s) => star(cx, cy, R * 0.55 * s, 12, 202, 0.2, 0.55), [ORANGE, YELLOW, WHITE], [1, 0.75, 0.5]);
    return;
  }
  if (f === 2) { // estrela redonda densa, brilho forte, pontinhos escuros
    glow(ctx, cx, cy, R * 1.8, "255,240,190", 0.85);
    glow(ctx, cx, cy, R * 0.9, "255,255,235", 0.7);
    layered(ctx, (s) => star(cx, cy, R * 1.0 * s, 20, 303, 0.2, 0.6), [RED, ORANGE, YELLOW, CREAM, WHITE], [1, 0.88, 0.74, 0.58, 0.4]);
    ctx.fillStyle = SPECK;
    for (const s of specks.slice(0, 9)) { const [x, y] = at(cx, cy, s.a, R * s.d * 0.85); ctx.beginPath(); ctx.arc(x, y, s.s * k * 0.8, 0, Math.PI * 2); ctx.fill(); }
    return;
  }
  // 3..8: o miolo abre; as línguas nascem presas à borda (3) e depois voam (4..7)
  const t = f === 3 ? 0 : [0, 0, 0, 0, 0.22, 0.45, 0.66, 0.84, 1.0][f];
  if (f === 3) {
    glow(ctx, cx, cy, R * 1.5, "255,190,90", 0.5);
    ctx.fillStyle = DARK; ctx.fill(star(cx, cy, R * 0.62, 14, 404, 0.1, 0.7));
    ctx.fillStyle = SPECK;
    for (const s of specks) { const [x, y] = at(cx, cy, s.a, R * s.d * 0.8); ctx.beginPath(); ctx.arc(x, y, s.s * k, 0, Math.PI * 2); ctx.fill(); }
  } else if (f === 4) glow(ctx, cx, cy, R * 1.0, "255,170,80", 0.2);
  for (const q of tongues) {
    const life = t / q.life; if (life >= 1) continue;
    const shrink = f === 3 ? 1 : Math.pow(1 - life, 0.8);
    if (shrink < 0.12) continue;
    const d = R * (0.66 + q.sp * ease(t) * 1.2), [x, y] = at(cx, cy, q.a, d), yy = y + R * 0.12 * t * t;
    const a = q.a + q.curl * t, len = R * q.len * (f === 3 ? 1.15 : 1), w = R * q.w;
    layered(ctx, (s) => tongue(x - Math.cos(a) * len * 0.12 * (1 - s), yy - Math.sin(a) * len * 0.12 * (1 - s), a, len, w, shrink * s), [RED, ORANGE, YELLOW, CREAM], [1, 0.82, 0.62, 0.4]);
  }
};

export const impacto_forte: Film = {
  meta: { title: "impacto_forte v3", W: 1280, H: 960, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
