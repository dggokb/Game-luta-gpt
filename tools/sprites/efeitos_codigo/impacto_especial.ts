import { Film } from "./film";
import { fazImpacto, Nivel } from "./impacto_kit";

// impacto_especial: da família de acerto (impacto_kit.ts). PNG transparente; efeito_video.py --ancora centro.
const nivel: Nivel = { R: 175, quadros: 11, raios: [[-0.12, 3.5, 0.06], [0.42, 3.0, 0.04], [-0.75, 2.7, 0.035], [2.6, 2.4, 0.03], [1.9, 2.2, 0.03]], x: true, linguas: 12, linguaEscala: 1.05, faiscas: 64, agulhas: 9, eletricos: 4, seed: 37 };

export const impacto_especial: Film = {
  meta: { title: "impacto_especial", W: 1600, H: 1200, fps: 24, bpm: 60, durationFrames: nivel.quadros },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: nivel.quadros, draw: fazImpacto(nivel) }],
};
