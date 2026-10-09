# P01 — teste local de super-resolução 4×

Idle inteiro processado com Real-ESRGAN AnimeVideo-v3 em CPU: 76/76 PNGs separados, 1024×1024 RGBA, sem falhas de processamento. Este resultado usa um modelo treinado de super-resolução; a tentativa anterior em idle_upscale_6x é apenas interpolação e está preservada.

Cada frame mantém a tela original 256×256 multiplicada por 4, a sequência e 42 ms. Não há reenquadramento, normalização de silhueta, interpolação temporal ou geração por prompt. O alpha é exatamente o alpha original ampliado uniformemente por Lanczos, conferido em 76/76 frames. Cores e linhas internas podem mudar pela reconstrução do modelo: este é um teste visual, não uma garantia de detalhe verdadeiro nem ausência de tremulação.

Diagnóstico temporal em 256×256, sobre pixels conjuntamente opacos, incluindo a passagem 75→0: diferença RGB média entre frames consecutivos de 3,548085 no original e 3,792796 no resultado. A métrica não compensa movimento e não aprova sozinha a estabilidade.

Arquivos PNG frame_000.png a frame_075.png diretamente nesta pasta. No ZIP, PNGs em frames/. manifest.json registra hashes, processo e comparação temporal. A comparação animada será entregue na conversa.

Originais, character.json e tentativas anteriores preservados. Nenhuma referência de P02 usada. Sem integração ao jogo ou APK.

Branch: art/p01-hd-redraw-20261009.
Base dos originais: game-luta-sprite-gpt, db929d23ac15e4124e48f240c97414bba416424a.

Modelo oficial: https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesr-animevideov3.pth
SHA256 do modelo: b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d
Documentação: https://github.com/xinntao/Real-ESRGAN/blob/master/docs/anime_video_model.md
Licença BSD 3-Clause; ver LICENSE-Real-ESRGAN.txt.
