# P01 — teste local de super-resolução 4×

Idle inteiro processado com Real-ESRGAN AnimeVideo-v3 em CPU: 76/76 PNGs separados, enviados e conferidos no GitHub, 1024×1024 RGBA, sem falhas de processamento. Este resultado usa um modelo treinado de super-resolução; a tentativa anterior em idle_upscale_6x é apenas interpolação e está preservada.

Cada frame mantém a tela original 256×256 multiplicada por 4, a sequência e 42 ms. Não há reenquadramento, normalização de silhueta, interpolação temporal ou geração por prompt. O alpha é exatamente o alpha original ampliado uniformemente por Lanczos, conferido em 76/76 frames. Cores e linhas internas podem mudar pela reconstrução do modelo: este é um teste visual, não uma garantia de detalhe verdadeiro nem ausência de tremulação.

Diagnóstico temporal em 256×256, sobre pixels conjuntamente opacos, incluindo a passagem 75→0: diferença RGB média entre frames consecutivos de 3,548085 no original e 3,792796 no resultado. A métrica não compensa movimento e não aprova sozinha a estabilidade.

Arquivos PNG frame_000.png a frame_075.png diretamente nesta pasta. No ZIP, PNGs em frames/. manifest.json registra hashes, processo e comparação temporal. A comparação animada foi preparada para a conversa.

Originais, character.json e tentativas anteriores preservados. Nenhuma referência de P02 usada. Sem integração ao jogo ou APK.

Branch: art/p01-hd-redraw-20261009.
Base dos originais: game-luta-sprite-gpt, db929d23ac15e4124e48f240c97414bba416424a.

Modelo oficial: https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesr-animevideov3.pth
SHA256 do modelo: b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d
Documentação: https://github.com/xinntao/Real-ESRGAN/blob/master/docs/anime_video_model.md
Licença BSD 3-Clause; ver LICENSE-Real-ESRGAN.txt.

Reprodução local (Python + torch CPU, Pillow, numpy e scipy): baixar os pesos oficiais acima e executar upscale_idle_sr.py com --input PASTA_DOS_76_ORIGINAIS --output PASTA_NOVA --weights realesr-animevideov3.pth. O modelo e o alpha usam a mesma transformação para todos os frames. Os scripts estão junto do pacote; os pesos não estão incluídos.


## Resultado aprovado e integrado

Em 09/10/2026 o usuário aprovou o idle no jogo: "deu muito certo". Os 76 masters estão preservados. Integração em game-luta-sprite-gpt, commit b884f6eeb20e687e9eab6473858ba6ef8a6a0af2: fonte 2×, pixelScale 2, sequência e 42 ms preservados. Aprovação registrada em c7a1409a9254c0b4cfb009132b09361ee30776b3; receita e limites em docs/art/p01-idle-sr-integration.md. O texto anterior descreve o estágio do pacote antes da integração.

Próximo teste: WALK_FORWARD, 20 masters em ../walk_forward_sr_4x/, avanço de 12,5 unidades/frame. Branch de teste: test/p01-walk-forward-sr-20261009.
