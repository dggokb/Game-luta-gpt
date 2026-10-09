# P02 — teste do idle com super-resolução

56/56 PNGs separados, RGBA 768×1024, sem falhas de frame. Mesmo método do idle P01 aprovado: Real-ESRGAN AnimeVideo-v3 em CPU, reconstrução RGB 4×, tratamento uniforme dos pixels transparentes e alpha original ampliado com Lanczos. Exclusivamente o P02 original; sem redesenho por prompt, alinhamento individual, reenquadramento ou suavização temporal.

Original: `art/sprites/source/p02_idle_video_normalized.png`, commit `f141c542607e7b642c4d40e256d25d45039ad51b` da `game-luta-sprite-gpt`, baixado diretamente do GitHub pelo navegador Work e comparado byte a byte. SHA256 `4dbe1158c6f2138f1068c5855caddd73739fdf31e847fed71891cf32c48163b0`.

O idle do P02 tem células 192×256, root (97,238), 11 colunas, frames 000–055 e 41 ms por frame. Os masters preservam essa proporção e registro: 768×1024, root (388,952). Alpha validado exatamente nos 56 frames. A super-resolução reconstrói cores e linhas internas; não garante detalhe verdadeiro nem ausência de tremulação.

Branch de teste: `test/p02-idle-sr-20261009`, baseada na principal acima, com idle e caminhada melhorados do P01 preservados. O teste inclui integração P02 em densidade 2×: fonte com células 384×512, root (194,476), `pixelScale: 2`. Os originais e `characters/p02/character.json` permanecem intactos. A `game-luta-sprite-gpt` não foi alterada neste teste. Nenhum novo APK foi solicitado. Aprovação visual do P02 pendente.

Nesta pasta do GitHub: masters `frame_000.png` a `frame_055.png`, fonte e atlas de runtime, scripts, manifest e licença. No ZIP: masters em `frames/`, originais em `original_frames/`, atlas original em `original_source/`, runtime em `runtime_2x/`.

Modelo oficial: https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesr-animevideov3.pth

SHA256 do modelo: `b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d`. BSD 3-Clause; licença incluída, pesos não incluídos.

Python + torch CPU, Pillow, numpy e scipy. Reprodução usando os originais do ZIP:

```sh
python upscale_p02_idle_sr.py --input original_frames --output resultado --weights realesr-animevideov3.pth
python prepare_p02_idle_sr.py --frames resultado/frames --manifest resultado/manifest.json --original original_source/p02_idle_video_normalized.png --output p02_idle_sr_2x.png
```

O manifest registra hashes, alpha, tempo de processamento e diagnóstico temporal incluindo 055→000, sem compensação de movimento. Essa medida não aprova a animação sozinha. Detalhes de integração e validação em `docs/art/p02-idle-sr-test.md`.
