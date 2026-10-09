# P01 — andar para frente: teste de super-resolução

20/20 frames concluídos em 1024×1024 RGBA, sem falhas de processamento. Mesmo modelo e tratamento de transparência do idle aprovado pelo usuário em 09/10/2026: Real-ESRGAN AnimeVideo-v3, CPU, escala uniforme 4×. Não houve redesenho por prompt, alinhamento individual ou suavização temporal. A identidade vem exclusivamente dos sprites originais do P01.

Origem: `art/sprites/source/player_base_walk_forward_video_normalized.png`, commit `b884f6eeb20e687e9eab6473858ba6ef8a6a0af2`. PNG baixado diretamente do GitHub pelo navegador Work e comparado byte a byte com a cópia do repositório. A grade original tem células 256×256, 8 colunas, root (128,238). Sequência 000–019 e loop preservados. O alpha de cada master é exatamente o alpha original ampliado com Lanczos, validado em todos os frames. As cores e linhas internas são reconstruídas pelo modelo.

O ritmo de WALK_FORWARD é baseado na distância: `distancePerFrame: 12.5`, sem duração fixa de 42 ms. A prévia usa a velocidade base de caminhada de 300 unidades/s; corresponde a aproximadamente 41,67 ms/frame. A velocidade real no jogo pode variar conforme seu estado.

Masters na branch `art/p01-hd-redraw-20261009`, PNGs separados diretamente nesta pasta. No ZIP: `frames/`. Teste integrado separado em `test/p01-walk-forward-sr-20261009`, com fonte 2× (512×512 por célula) derivada uniformemente dos masters para limitar memória. `pixelScale: 2` mantém o tamanho visual e o root no jogo. Originais preservados; `characters/player_base/character.json` não foi alterado. Aprovação visual do andar para frente pendente.

O manifest registra hashes e diagnósticos. A comparação temporal mede diferenças RGB entre pares consecutivos incluindo 019→000, em pixels conjuntamente opacos, sem compensar movimento. Ela não garante ausência de tremulação. Uma dependência Python ausente foi corrigida antes do processamento; nenhum frame foi rejeitado ou omitido.

Modelo oficial: https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesr-animevideov3.pth

SHA256: `b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d`.

Licença BSD 3-Clause em `LICENSE-Real-ESRGAN.txt`. Pesos não incluídos. Dependências: Python, torch CPU, numpy, scipy, Pillow. Extrair as 20 células originais, nomear `frame_000.png` a `frame_019.png`, depois executar:

```sh
python upscale_walk_forward_sr.py --input PASTA_ORIGINAIS --output PASTA_NOVA --weights realesr-animevideov3.pth
python prepare_walk_forward_sr.py --frames PASTA_NOVA/frames --manifest PASTA_NOVA/manifest.json --original player_base_walk_forward_video_normalized.png --output player_base_walk_forward_sr_2x.png
```
