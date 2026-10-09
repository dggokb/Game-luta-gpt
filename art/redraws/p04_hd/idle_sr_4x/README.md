# P04 — idle com o processo de super-resolução aprovado

68 PNGs separados, RGBA 768×1024, ampliados uniformemente em 4× pelo mesmo Real-ESRGAN AnimeVideo-v3 dos testes P01, P02 e P03. Processo aprovado pelo usuário em 09/10/2026. Referência exclusivamente o P04 original; extensão RGB invisível a partir do pixel visível mais próximo e alpha original por Lanczos. Não há alinhamento individual de frames nem suavização temporal.

Original `art/sprites/source/p04_idle_video_normalized.png`, baixado diretamente pelo navegador Work e conferido byte a byte com a `game-luta-sprite-gpt` em `582d1f6589f769f72ffb9bdec88e8fa3a957bc61`. SHA256 `fe2bdeeebe35655c464dcde1658a752ad81de96fa1f6982a02e3f44bd80c1444`. Grade 2112×1792, 11 colunas, 7 linhas, células 192×256, root (96,238), frames 000–067 e duração de 41 ms preservados. Masters com root (384,952).

Integração na `game-luta-sprite-gpt`: fonte 2× com células 384×512, root (192,476), pixelScale 2, preservando tamanho no mundo. Originais e configuração de combate preservados. Nenhum APK gerado nesta tarefa. A aprovação do processo não garante ausência de tremulação em toda animação; o manifest registra verificações por frame e diagnóstico temporal.

GitHub: `frame_000.png` a `frame_067.png`, scripts, manifest, licença e cópias dos atlases. ZIP: masters e scripts em `p04_idle_sr_4x/`, frames e atlas original `source.png` em `originals/`, atlases do jogo em `runtime/`.

Modelo oficial: https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesr-animevideov3.pth

SHA256 do modelo `b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d`. Licença BSD 3-Clause incluída; pesos não incluídos. Dependências: Python, torch CPU, Pillow, numpy e scipy.

Reprodução a partir do ZIP:

```sh
python p04_idle_sr_4x/upscale_p04_idle_sr.py --input originals --output resultado --weights realesr-animevideov3.pth
python p04_idle_sr_4x/prepare_p04_idle_sr.py --frames resultado/frames --manifest resultado/manifest.json --original originals/source.png --output p04_idle_sr_2x.png
```

Detalhes de integração em `docs/art/p04-idle-sr-integration.md`; processo aprovado em `docs/art/super-resolution-process.md`.
