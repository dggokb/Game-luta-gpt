import { Film } from "./film";
import { fazImpacto, Nivel } from "./impacto_kit";

// impacto_medio: da família de acerto (impacto_kit.ts). PNG transparente; efeito_video.py --ancora centro.
const nivel: Nivel = { R: 125, quadros: 8, raios: [[-0.12, 3.4, 0.05], [0.42, 2.8, 0.035], [-0.75, 2.5, 0.03]], x: true, linguas: 7, linguaEscala: 0.95, faiscas: 28, agulhas: 0, eletricos: 0, seed: 23 };

export const impacto_medio: Film = {
  meta: { title: "impacto_medio", W: 1280, H: 900, fps: 24, bpm: 60, durationFrames: nivel.quadros },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: nivel.quadros, draw: fazImpacto(nivel) }],
};
