import { Film } from "./film";
import { fazImpacto, Nivel } from "./impacto_kit";

// impacto_fraco: da família de acerto (impacto_kit.ts). PNG transparente; efeito_video.py --ancora centro.
const nivel: Nivel = { R: 90, quadros: 6, raios: [[-0.12, 3.0, 0.05], [0.5, 2.4, 0.035]], x: false, linguas: 6, linguaEscala: 0.9, faiscas: 16, agulhas: 0, eletricos: 0, seed: 11 };

export const impacto_fraco: Film = {
  meta: { title: "impacto_fraco", W: 1280, H: 720, fps: 24, bpm: 60, durationFrames: nivel.quadros },
  assets: { images: {} },
  shots: [{ id: "one", start: 0, end: nivel.quadros, draw: fazImpacto(nivel) }],
};
