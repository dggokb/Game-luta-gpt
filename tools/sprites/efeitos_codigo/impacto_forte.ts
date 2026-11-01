import { Ctx, Env, rng } from "./core";
import { Film } from "./film";

// impacto_forte: estrela de impacto de anime para o jogo de luta.
// Fundo verde chapado (#00FF00) para o tools/sprites/efeito_video.py do jogo.
// Começa vazio, termina vazio, centralizado, sem câmera. Branco, sombra cinza, contorno fino escuro.

const FPS = 24, BPM = 60, DURATION = 10; // quadro 0 e 9 vazios; efeito em 1..8 (~0,33 s)
const GREEN = "#00ff00", WHITE = "#ffffff", GRAY = "#a2a2aa", INK = "#1c1c22";

type P = [number, number];
const clamp = (v: number, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const ease = (t: number) => 1 - Math.pow(1 - clamp(t), 3);

// A forma da estrela é fixa (seed); o tremido do traço muda a cada 2 quadros (boil de anime).
const N = 12;
const base = rng(7);
const spikes = Array.from({ length: N }, (_, i) => {
  const a = (i / N) * Math.PI * 2 + (base() - 0.5) * 0.28;
  const long = i % 3 === 0 ? 1.18 : i % 3 === 1 ? 0.8 : 0.96;
  return { a, len: long * (0.92 + base() * 0.16), valley: 0.44 + base() * 0.07, bend: (base() - 0.5) * 0.18 };
});
const drops = Array.from({ length: 8 }, (_, i) => ({
  a: ((i + 0.5) / 8) * Math.PI * 2 + (base() - 0.5) * 0.5, v: 0.75 + base() * 0.55, size: 0.045 + base() * 0.04, delay: base() * 1.2,
}));
const shardSpin = spikes.map(() => (base() - 0.5) * 1.6);

const boil = (frame: number) => rng(1000 + Math.floor(frame / 2));

// Preenche com sombra: tudo cinza, o branco deslocado para cima (luz de cima), contorno por cima.
const inked = (ctx: Ctx, path: () => void, k: number, shade: number, line: number) => {
  ctx.save();
  path(); ctx.clip("evenodd");
  ctx.fillStyle = GRAY; path(); ctx.fill("evenodd");
  ctx.translate(-shade * 0.35 * k, -shade * k);
  ctx.fillStyle = WHITE; path(); ctx.fill("evenodd");
  ctx.restore();
  ctx.lineJoin = "miter"; ctx.miterLimit = 12; ctx.lineCap = "round";
  ctx.strokeStyle = INK; ctx.lineWidth = line * k; path(); ctx.stroke();
};

// Estrela: vale, ponta, vale... com lados côncavos (espinhos afiados). O seed do tremido é
// recriado a cada chamada para o mesmo caminho sair igual no clip, no preenchimento e no traço.
const starPath = (ctx: Ctx, cx: number, cy: number, R: number, grow: number, seed: number, hole: number) => {
  const j = rng(seed);
  ctx.beginPath();
  const pts: { v: P; tip: P; c1: P; c2: P }[] = [];
  for (let i = 0; i < N; i++) {
    const s = spikes[i], n = spikes[(i + 1) % N];
    const va = s.a - Math.PI / N, vr = R * s.valley * (1 + (j() - 0.5) * 0.08);
    const tr = R * s.len * grow * (1 + (j() - 0.5) * 0.05), ta = s.a + s.bend * 0.3 + (j() - 0.5) * 0.02;
    const nva = n.a - Math.PI / N + (i === N - 1 ? Math.PI * 2 : 0);
    const at = (r: number, a: number): P => [cx + Math.cos(a) * r, cy + Math.sin(a) * r];
    pts.push({ v: at(vr, va), tip: at(tr, ta), c1: at((vr + tr) * 0.42, (va + ta) / 2 - s.bend * 0.1), c2: at((vr + tr) * 0.42, (ta + nva) / 2 + s.bend * 0.1) });
  }
  pts.forEach((p, i) => {
    const next = pts[(i + 1) % N];
    if (i === 0) ctx.moveTo(p.v[0], p.v[1]);
    ctx.quadraticCurveTo(p.c1[0], p.c1[1], p.tip[0], p.tip[1]);
    ctx.quadraticCurveTo(p.c2[0], p.c2[1], next.v[0], next.v[1]);
  });
  ctx.closePath();
  if (hole > 0) { // o centro se rompe: um buraco serrilhado (evenodd)
    const M = 12;
    for (let i = 0; i < M; i++) {
      const a = (i / M) * Math.PI * 2, r = R * hole * (i % 2 ? 0.7 : 1) * (1 + (j() - 0.5) * 0.14);
      const x = cx + Math.cos(a) * r, y = cy + Math.sin(a) * r;
      i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
    }
    ctx.closePath();
  }
};

// Linhas de velocidade: cunhas brancas finas com contorno, saindo de fora da estrela.
const speedLines = (ctx: Ctx, cx: number, cy: number, R: number, t: number, k: number, seed: number) => {
  const j = rng(seed);
  for (let i = 0; i < N; i++) { // nos vãos entre os espinhos
    const a = (spikes[i].a + spikes[(i + 1) % N].a + (i === N - 1 ? Math.PI * 2 : 0)) / 2 + (j() - 0.5) * 0.06;
    const r0 = R * (0.86 + t * 0.3), r1 = r0 + R * (0.34 - t * 0.16) * (0.7 + j() * 0.5), w = R * 0.034 * (1 - t * 0.6);
    const px = -Math.sin(a), py = Math.cos(a);
    inked(ctx, () => {
      ctx.beginPath();
      ctx.moveTo(cx + Math.cos(a) * r0 + px * w, cy + Math.sin(a) * r0 + py * w);
      ctx.lineTo(cx + Math.cos(a) * r1, cy + Math.sin(a) * r1);
      ctx.lineTo(cx + Math.cos(a) * r0 - px * w, cy + Math.sin(a) * r0 - py * w);
      ctx.closePath();
    }, k, 0, 2.2);
  }
};

// Caco: o triângulo de um espinho, voando na direção dele, girando e encolhendo.
const shard = (ctx: Ctx, cx: number, cy: number, R: number, i: number, t: number, k: number, seed: number) => {
  const s = spikes[i], jit = rng(seed)() - 0.5;
  const fly = ease(t) * R * (0.4 + s.len * 0.3), shrink = clamp(1 - Math.pow(t, 1.6));
  if (shrink <= 0.06) return;
  const len = R * s.len * (0.62 - 0.12 * t) * shrink, half = R * 0.12 * shrink * (0.7 + s.valley);
  const ox = cx + Math.cos(s.a) * (R * 0.42 + fly), oy = cy + Math.sin(s.a) * (R * 0.42 + fly) + R * 0.12 * t * t;
  const rot = s.a + shardSpin[i] * t;
  const pts: P[] = [[-len * 0.3, -half], [len * 0.7, jit * half * 0.3], [-len * 0.3, half * 0.85], [-len * 0.42, 0]];
  const tr = (p: P): P => [ox + p[0] * Math.cos(rot) - p[1] * Math.sin(rot), oy + p[0] * Math.sin(rot) + p[1] * Math.cos(rot)];
  inked(ctx, () => { ctx.beginPath(); pts.map(tr).forEach(([x, y], n) => (n ? ctx.lineTo(x, y) : ctx.moveTo(x, y))); ctx.closePath(); }, k, half * 0.35 / k, 3.4);
};

// Gota: lágrima apontando para trás do movimento, com gravidade.
const drop = (ctx: Ctx, cx: number, cy: number, R: number, d: typeof drops[number], t: number, k: number) => {
  const u = clamp((t - d.delay * 0.25) / 0.85);
  if (u <= 0 || u >= 1) return;
  const dist = R * (0.75 + d.v * ease(u) * 0.75), g = R * 0.35 * u * u;
  const x = cx + Math.cos(d.a) * dist, y = cy + Math.sin(d.a) * dist + g;
  const vx = Math.cos(d.a) * d.v, vy = Math.sin(d.a) * d.v + 0.9 * u, va = Math.atan2(vy, vx);
  const r = R * d.size * (1 - u * 0.85);
  if (r < 1.2 * k) return;
  inked(ctx, () => {
    ctx.beginPath();
    const tail: P = [x - Math.cos(va) * r * 2.6, y - Math.sin(va) * r * 2.6];
    ctx.moveTo(tail[0], tail[1]);
    ctx.arc(x, y, r, va - Math.PI / 2 - 0.15, va + Math.PI / 2 + 0.15);
    ctx.closePath();
  }, k, r * 0.35 / k, 2.2);
};

const draw = (ctx: Ctx, local: number, env: Env) => {
  const k = env.scale, W = env.W * k, H = env.H * k;
  ctx.fillStyle = GREEN; ctx.fillRect(0, 0, W, H);
  const f = local;
  if (f < 1 || f > 8) return; // começa e termina vazio
  const cx = W / 2, cy = H / 2, R = 172 * k, seed = 1000 + Math.floor(f / 2);

  if (f === 1) { // clarão: estrela pequena e cheia
    inked(ctx, () => starPath(ctx, cx, cy, R * 0.45, 0.9, seed, 0), k, 7, 3.6);
    return;
  }
  if (f <= 4) { // a estrela abre: 2 grande, 3 cheia, 4 o centro começa a romper
    const size = [0, 0, 0.8, 1.0, 0.97][f], grow = f === 3 ? 1.05 : 1, hole = f === 4 ? 0.3 : 0;
    if (f >= 3) speedLines(ctx, cx, cy, R, (f - 3) / 2, k, seed + 50);
    inked(ctx, () => starPath(ctx, cx, cy, R * size, grow, seed, hole), k, 20, 4);
    return;
  }
  // 5..8: a estrela quebra em cacos (um por espinho) e gotas
  const t = [0, 0, 0, 0, 0, 0.18, 0.48, 0.74, 0.94][f];
  for (let i = 0; i < N; i++) shard(ctx, cx, cy, R, i, t, k, seed + i * 7);
  for (const d of drops) drop(ctx, cx, cy, R, d, t, k);
};

export const impacto_forte: Film = {
  meta: { title: "impacto_forte", W: 1280, H: 720, fps: FPS, bpm: BPM, durationFrames: DURATION },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: DURATION, draw }],
};
