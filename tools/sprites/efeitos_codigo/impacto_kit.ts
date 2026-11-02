import { Ctx, Env, rng } from "./core";

// Kit da família de acerto (impacto_fraco, impacto_medio, impacto_especial), com o mesmo desenho do
// impacto_forte v3: luz sem contorno, centrada no ponto do golpe, sem fumaça. O golpe vai para a
// DIREITA (o jogo espelha). Cada nível muda o tamanho, quantas coisas aparecem e quanto dura.
// Estudo: docs/efeitos-referencia-fighterz.md.

export const RED = "#e8321c", ORANGE = "#ff8a1e", YELLOW = "#ffd64a", CREAM = "#fff6cf", WHITE = "#ffffff";
export const DARK = "#7a1a10", SPECK = "#2a0c08";

export type P = [number, number];
export const clamp = (v: number, a = 0, b = 1) => Math.max(a, Math.min(b, v));
export const ease = (t: number) => 1 - Math.pow(1 - clamp(t), 2.2);
export const at = (cx: number, cy: number, a: number, r: number): P => [cx + Math.cos(a) * r, cy + Math.sin(a) * r];

// Brilho macio: degradê radial da cor para transparente.
export const glow = (ctx: Ctx, x: number, y: number, r: number, rgb: string, alpha: number) => {
  const g = ctx.createRadialGradient(x, y, 0, x, y, r);
  g.addColorStop(0, `rgba(${rgb},${alpha})`);
  g.addColorStop(0.45, `rgba(${rgb},${alpha * 0.45})`);
  g.addColorStop(1, `rgba(${rgb},0)`);
  ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill();
};

// Estrela de espinhos (lados côncavos), mais longa para a frente.
export const star = (cx: number, cy: number, R: number, n: number, seed: number, push = 0.35, valley = 0.52) => {
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
export const layered = (ctx: Ctx, make: (s: number) => Path2D, colors: string[], scales: number[]) => {
  colors.forEach((c, i) => { ctx.fillStyle = c; ctx.fill(make(scales[i])); });
};

// Lança afiada com borda serrilhada (o X e os raios).
export const spear = (cx: number, cy: number, a: number, back: number, front: number, w: number, seed: number) => {
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
export const ray = (ctx: Ctx, cx: number, cy: number, a: number, L: number, w: number, alpha: number) => {
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
export const tongue = (x: number, y: number, a: number, len: number, w: number, s: number) => {
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


export type Nivel = {
  R: number;                 // tamanho da estrela (px)
  quadros: number;           // duração total
  raios: [number, number, number][];   // raios de luz no contato: ângulo, comprimento (×R), largura (×R)
  x: boolean;                // o quadro do X
  linguas: number; linguaEscala: number;
  faiscas: number;
  agulhas: number;           // agulhas vermelhas (especial)
  eletricos: number;         // raios elétricos em zigue-zague (especial)
  seed: number;
};

const ZIG = "255,244,190";

export const fazImpacto = (n: Nivel) => {
  const rt = rng(n.seed + 41);
  const tongues = Array.from({ length: n.linguas }, (_, i) => {
    const a = (i / n.linguas) * Math.PI * 2 + (rt() - 0.5) * 0.5, up = Math.max(0, -Math.sin(a)), fwd = Math.max(0, Math.cos(a));
    const big = (0.75 + rt() * 0.6) * n.linguaEscala;
    return { a, sp: 0.25 + rt() * 0.25 + 0.25 * fwd + 0.1 * up, len: 0.85 * big, w: 0.3 * big, curl: (rt() < 0.5 ? -1 : 1) * (0.35 + rt() * 0.4), life: 0.7 + rt() * 0.45 };
  });
  const re = rng(n.seed + 77), last = n.quadros - 1;
  const embers = Array.from({ length: n.faiscas }, () => {
    const a = re() * Math.PI * 2, fwd = Math.max(0, Math.cos(a));
    return { a, d0: 0.25 + re() * 0.7, sp: 0.3 + re() * 0.8 + fwd * 0.4, size: 2.2 + re() * 3, dash: re() < 0.45, born: Math.floor(re() * 3), die: Math.min(last, Math.round(last * (0.5 + re() * 0.5))) };
  });
  const rs = rng(n.seed + 19);
  const specks = Array.from({ length: 16 }, () => ({ a: rs() * Math.PI * 2, d: rs() * 0.55, s: 1.5 + rs() * 3.5 }));
  const rn = rng(n.seed + 5);
  const needles = Array.from({ length: n.agulhas }, () => ({ a: (rn() - 0.5) * 2.4 + (rn() < 0.25 ? Math.PI : 0), L: 2.2 + rn() * 1.4 }));
  // ordem dos quadros: contato, (X), estrela redonda, miolo aberto, línguas voando
  const fX = n.x ? 1 : -1, fRound = n.x ? 2 : 1, fOpen = fRound + 1;

  return (ctx: Ctx, f: number, env: Env) => {
    const k = env.scale, W = env.W * k, H = env.H * k, cx = W / 2, cy = H / 2, R = n.R * k;
    ctx.clearRect(0, 0, W, H);
    for (const e of embers) {
      if (f < e.born || f > e.die) continue;
      const u = (f - e.born) / Math.max(1, e.die - e.born), d = R * (e.d0 + e.sp * ease(u));
      const [x, y] = at(cx, cy, e.a, d), yy = y + R * 0.25 * u * u, s = e.size * k * (1 - u * 0.6) * Math.max(0.6, n.R / 160);
      glow(ctx, x, yy, s * 3, "255,214,74", 0.35 * (1 - u));
      ctx.fillStyle = u < 0.5 ? CREAM : YELLOW;
      if (e.dash) { ctx.save(); ctx.translate(x, yy); ctx.rotate(e.a); ctx.fillRect(-s * 1.8, -s * 0.4, s * 3.6, s * 0.8); ctx.restore(); }
      else { ctx.beginPath(); ctx.arc(x, yy, s * 0.7, 0, Math.PI * 2); ctx.fill(); }
    }
    // agulhas vermelhas (0..2) e raios elétricos (0..3), atrás da estrela
    if (f <= 2) for (const q of needles) {
      const L = R * q.L * [0.85, 1, 1.05][f], w = R * 0.07 * [1, 0.8, 0.45][f];
      layered(ctx, (s) => spear(cx, cy, q.a, R * 0.2, L * s, w * s, 3), [RED, ORANGE], [1, 0.55]);
    }
    if (f <= 3) for (let b = 0; b < n.eletricos; b++) {
      const zr = rng(n.seed * 31 + b * 7 + f * 101), a0 = (b / n.eletricos) * Math.PI * 2 + 0.4, L = R * (2.0 + zr() * 0.8) * (f === 0 ? 0.8 : 1);
      const pts: P[] = [at(cx, cy, a0, R * 0.5)];
      for (let i = 1; i <= 6; i++) { const d = R * 0.5 + (L - R * 0.5) * (i / 6), j = (zr() - 0.5) * R * 0.5; const [x, y] = at(cx, cy, a0, d); pts.push([x - Math.sin(a0) * j, y + Math.cos(a0) * j]); }
      const al = [1, 0.95, 0.7, 0.4][f];
      for (const [w, c, a] of [[R * 0.08, ZIG, 0.35 * al], [R * 0.035, "255,236,140", al], [R * 0.014, "255,255,255", al]] as const) {
        ctx.strokeStyle = `rgba(${c},${a})`; ctx.lineWidth = w; ctx.lineJoin = "miter"; ctx.lineCap = "round";
        ctx.beginPath(); pts.forEach(([x, y], i) => (i ? ctx.lineTo(x, y) : ctx.moveTo(x, y))); ctx.stroke();
      }
    }
    if (f === 0) {
      glow(ctx, cx, cy, R * 1.9, "255,230,150", 0.55);
      for (const [a, L, w] of n.raios) ray(ctx, cx, cy, a, R * L, R * w, 1);
      layered(ctx, (s) => star(cx + R * 0.04 * (1 - s), cy, R * 1.05 * s, 16, n.seed + 101), [RED, ORANGE, YELLOW, CREAM, WHITE], [1, 0.86, 0.7, 0.52, 0.34]);
      return;
    }
    if (f === fX) {
      glow(ctx, cx, cy, R * 1.6, "255,220,120", 0.5);
      for (const [a, L, w] of n.raios.slice(0, 2)) ray(ctx, cx, cy, a, R * L * 1.03, R * w * 0.6, 0.6);
      for (const [a, seed] of [[-0.62, 7], [0.78, 9]] as const) layered(ctx, (s) => spear(cx, cy, a, R * 1.1 * s, R * 1.35 * s, R * 0.2 * s, seed), [RED, ORANGE, YELLOW, CREAM], [1, 0.84, 0.66, 0.42]);
      layered(ctx, (s) => star(cx, cy, R * 0.55 * s, 12, n.seed + 202, 0.2, 0.55), [ORANGE, YELLOW, WHITE], [1, 0.75, 0.5]);
      return;
    }
    if (f === fRound) {
      glow(ctx, cx, cy, R * 1.8, "255,240,190", 0.85);
      glow(ctx, cx, cy, R * 0.9, "255,255,235", 0.7);
      layered(ctx, (s) => star(cx, cy, R * 1.0 * s, 20, n.seed + 303, 0.2, 0.6), [RED, ORANGE, YELLOW, CREAM, WHITE], [1, 0.88, 0.74, 0.58, 0.4]);
      ctx.fillStyle = SPECK;
      for (const s of specks.slice(0, 9)) { const [x, y] = at(cx, cy, s.a, R * s.d * 0.85); ctx.beginPath(); ctx.arc(x, y, s.s * k * 0.8 * Math.max(0.6, n.R / 160), 0, Math.PI * 2); ctx.fill(); }
      return;
    }
    const fly = Math.max(1, last - fOpen), t = f === fOpen ? 0 : 0.22 + 0.78 * ((f - fOpen - 1) / Math.max(1, fly - 1));
    if (f === fOpen) {
      glow(ctx, cx, cy, R * 1.5, "255,190,90", 0.5);
      ctx.fillStyle = DARK; ctx.fill(star(cx, cy, R * 0.62, 14, n.seed + 404, 0.1, 0.7));
      ctx.fillStyle = SPECK;
      for (const s of specks) { const [x, y] = at(cx, cy, s.a, R * s.d * 0.8); ctx.beginPath(); ctx.arc(x, y, s.s * k * Math.max(0.6, n.R / 160), 0, Math.PI * 2); ctx.fill(); }
    } else if (f === fOpen + 1) glow(ctx, cx, cy, R * 1.0, "255,170,80", 0.2);
    for (const q of tongues) {
      const life = t / q.life; if (life >= 1) continue;
      const shrink = f === fOpen ? 1 : Math.pow(1 - life, 0.8);
      if (shrink < 0.12) continue;
      const d = R * (0.66 + q.sp * ease(t) * 1.2), [x, y] = at(cx, cy, q.a, d), yy = y + R * 0.12 * t * t;
      const a = q.a + q.curl * t, len = R * q.len * (f === fOpen ? 1.15 : 1), w = R * q.w;
      layered(ctx, (s) => tongue(x - Math.cos(a) * len * 0.12 * (1 - s), yy - Math.sin(a) * len * 0.12 * (1 - s), a, len, w, shrink * s), [RED, ORANGE, YELLOW, CREAM], [1, 0.82, 0.62, 0.4]);
    }
  };
};
