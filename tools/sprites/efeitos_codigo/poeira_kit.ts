import { Ctx } from "./core";

// Kit da poeira de anime (mesma linguagem da fumaca_pouso; estudo em
// docs/efeitos-referencia-fighterz.md): bolhas cinza médio, sombra embaixo, luz só na borda de
// cima da silhueta, contorno fino cinza-escuro só por fora. Bolhas com rabinho (vírgula).

export const DUST = { BASE: "#b9b2a8", SHADE: "#8b837a", LIGHT: "#d9d3ca", INK: "#4a423b" };

export type Puff = { x: number; y: number; r: number; tail?: { a: number; len: number; curl?: number } };

// Todas as formas no mesmo sentido (horário na tela), senão a união abre buraco onde se cruzam.
const area = (pts: [number, number][]) => pts.reduce((s, [x, y], i) => { const [nx, ny] = pts[(i + 1) % pts.length]; return s + (x * ny - nx * y); }, 0);

export const puffPath = (ps: Puff[]) => {
  const p = new Path2D();
  for (const q of ps) {
    if (q.r <= 0.5) continue;
    p.moveTo(q.x + q.r, q.y); p.arc(q.x, q.y, q.r, 0, Math.PI * 2);
    if (q.tail && q.tail.len > 0) {
      const { a, len } = q.tail, curl = q.tail.curl ?? 0.4, n = a + Math.PI / 2;
      const tip: [number, number] = [q.x + Math.cos(a + curl * 0.5) * (q.r + len), q.y + Math.sin(a + curl * 0.5) * (q.r + len)];
      let p1: [number, number] = [q.x + Math.cos(n) * q.r * 0.8, q.y + Math.sin(n) * q.r * 0.8];
      let p2: [number, number] = [q.x - Math.cos(n) * q.r * 0.8, q.y - Math.sin(n) * q.r * 0.8];
      if (area([p1, tip, p2]) < 0) [p1, p2] = [p2, p1];
      const mid = (u: [number, number], w: number): [number, number] => [(u[0] + tip[0]) / 2 + Math.cos(n) * w, (u[1] + tip[1]) / 2 + Math.sin(n) * w];
      const c1 = mid(p1, q.r * 0.25 * (1 + curl)), c2 = mid(p2, -q.r * 0.1);
      p.moveTo(p1[0], p1[1]); p.quadraticCurveTo(c1[0], c1[1], tip[0], tip[1]); p.quadraticCurveTo(c2[0], c2[1], p2[0], p2[1]); p.closePath();
    }
  }
  return p;
};

// Pinta a poeira numa camada própria e a põe na tela com `alpha` (a transparência do fim não
// mostra o traço por baixo). `ground`: o chão corta reto embaixo. `shade`: altura da sombra (px).
export const renderDust = (ctx0: Ctx, W: number, H: number, k: number, path: Path2D, ground: number, shade: number, alpha = 1, line = 2.4) => {
  const lay = new OffscreenCanvas(W, H), ctx = lay.getContext("2d") as unknown as Ctx;
  ctx.save();
  ctx.beginPath(); ctx.rect(0, 0, W, ground); ctx.clip();
  ctx.lineJoin = "round"; ctx.strokeStyle = DUST.INK; ctx.lineWidth = 2 * line * k; ctx.stroke(path);
  ctx.save(); ctx.clip(path);
  ctx.fillStyle = DUST.SHADE; ctx.fill(path);
  ctx.save(); ctx.translate(-shade * 0.35, -shade); ctx.fillStyle = DUST.BASE; ctx.fill(path); ctx.restore();
  const rim = new OffscreenCanvas(W, H), rc = rim.getContext("2d") as unknown as Ctx;
  rc.fillStyle = DUST.LIGHT; rc.fill(path);
  rc.globalCompositeOperation = "destination-out"; rc.translate(shade * 0.2, shade * 0.65); rc.fill(path);
  ctx.drawImage(rim as unknown as CanvasImageSource, 0, 0);
  ctx.restore();
  ctx.restore();
  ctx0.globalAlpha = alpha; ctx0.drawImage(lay as unknown as CanvasImageSource, 0, 0); ctx0.globalAlpha = 1;
};

// Pedrinha com contorno escuro.
export const pebble = (ctx: Ctx, x: number, y: number, r: number, rot: number, k: number) => {
  ctx.save(); ctx.translate(x, y); ctx.rotate(rot);
  ctx.beginPath(); ctx.moveTo(-r, -r * 0.4); ctx.lineTo(-r * 0.2, -r); ctx.lineTo(r, -r * 0.3); ctx.lineTo(r * 0.6, r * 0.8); ctx.lineTo(-r * 0.7, r * 0.7); ctx.closePath();
  ctx.fillStyle = DUST.SHADE; ctx.fill(); ctx.lineWidth = 1.6 * k; ctx.strokeStyle = DUST.INK; ctx.stroke();
  ctx.restore();
};

// Fita de vento: traço curvo que afina nas duas pontas, cor da poeira, meio transparente.
export const ribbon = (ctx: Ctx, pts: [number, number][], w: number, color: string, alpha: number) => {
  if (pts.length < 3 || alpha <= 0) return;
  const left: [number, number][] = [], right: [number, number][] = [];
  pts.forEach((p, i) => {
    const a = pts[Math.max(0, i - 1)], b = pts[Math.min(pts.length - 1, i + 1)], dx = b[0] - a[0], dy = b[1] - a[1], L = Math.hypot(dx, dy) || 1;
    const t = i / (pts.length - 1), ww = w * Math.sin(Math.PI * t) * (0.4 + 0.6 * (1 - t));
    left.push([p[0] - dy / L * ww, p[1] + dx / L * ww]); right.push([p[0] + dy / L * ww, p[1] - dx / L * ww]);
  });
  ctx.save(); ctx.globalAlpha = alpha; ctx.fillStyle = color; ctx.beginPath();
  left.forEach(([x, y], i) => (i ? ctx.lineTo(x, y) : ctx.moveTo(x, y)));
  right.reverse().forEach(([x, y]) => ctx.lineTo(x, y));
  ctx.closePath(); ctx.fill(); ctx.restore();
};
