# P03 — último teste do idle com super-resolução

60 PNGs separados, RGBA 1024×1024, ampliados uniformemente em 4×. Método dos testes anteriores: Real-ESRGAN AnimeVideo-v3 em CPU, extensão RGB invisível a partir do pixel visível mais próximo, alpha original ampliado por Lanczos. Referência exclusivamente o P03 original.

Original `art/sprites/source/p03_idle_video_normalized.png`, baixado diretamente pelo navegador Work e conferido byte a byte com a `game-luta-sprite-gpt` em `91027cfbf7d32824964a7424d9684cfddb7eef77`. SHA256 `9b38925b2f6b05b81ce1a37076d6bad3873bd10da534e2f6e862ad3bb11b11fc`. Grade 2048×2048, 8 colunas, células 256×256, root (128,238), frames 000–059, 41 ms por frame. Masters com root proporcional (512,952).

Integração destinada à `game-luta-sprite-gpt`, nossa branch de testes, com fonte 2×, células 512×512, root (256,476) e pixelScale 2. Originais e configuração de combate preservados. Nenhum APK gerado nesta tarefa. Avaliação visual do P03 e decisão final sobre o método pendentes.

GitHub: `frame_000.png` a `frame_059.png`, scripts, manifest, licença e cópias dos atlases. ZIP: masters e scripts em `p03_idle_sr_4x/`, frames e atlas original `source.png` em `originals/`, atlases do jogo em `runtime/`.

Modelo oficial: https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesr-animevideov3.pth

SHA256 do modelo `b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d`. Licença BSD 3-Clause incluída; pesos não incluídos. Python, torch CPU, Pillow, numpy e scipy.

Reprodução a partir do ZIP:

```sh
python p03_idle_sr_4x/upscale_p03_idle_sr.py --input originals --output resultado --weights realesr-animevideov3.pth
python p03_idle_sr_4x/prepare_p03_idle_sr.py --frames resultado/frames --manifest resultado/manifest.json --original originals/source.png --output p03_idle_sr_2x.png
```

O manifest registra hashes e alpha dos 60 frames, incluindo diagnóstico temporal da transição 059→000. A métrica não aprova a qualidade visual sozinha nem garante ausência de tremulação. A super-resolução reconstrói linhas e cores internas; preserva a pose e o contorno ampliado. Detalhes em `docs/art/p03-idle-sr-test.md`.
