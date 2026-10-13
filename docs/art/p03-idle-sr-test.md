# P03: último teste do idle com super-resolução

Integração destinada à `game-luta-sprite-gpt`, nossa branch de testes, a pedido do usuário em 09/10/2026. Base `91027cfbf7d32824964a7424d9684cfddb7eef77`, já com P01 idle/caminhada e P02 idle melhorados. Os resultados são preparados em `test/p03-idle-sr-20261009` e integrados somente depois das verificações. Avaliação visual do P03 e decisão final sobre o método pendentes. Nenhum APK gerado nesta tarefa.

## Resultado e método

60/60 masters RGBA 1024×1024, zero falhas de frame. Real-ESRGAN AnimeVideo-v3 oficial em CPU, mesmo método dos testes anteriores. Modelo SHA256 `b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d`. Reconstrução RGB 4× com extensão do RGB invisível pelo pixel visível mais próximo; alpha diretamente do original por Lanczos, validado exatamente nos 60 masters. Sem ajuste individual de enquadramento, alinhamento ou suavização temporal. Identidade exclusivamente do P03 original.

Original `art/sprites/source/p03_idle_video_normalized.png`, baixado diretamente pelo navegador Work e conferido byte a byte. SHA256 `9b38925b2f6b05b81ce1a37076d6bad3873bd10da534e2f6e862ad3bb11b11fc`. Grade 2048×2048, células 256×256, 8 colunas, 8 linhas, 60 frames válidos, root (128,238). Frames 000–059, loop e 41 ms por frame preservados. Masters com root (512,952).

## Integração no jogo

Fonte `art/sprites/source/p03_idle_sr_2x.png`, 4096×4096, células 512×512, root (256,476). Redução uniforme dos masters com RGB premultiplicado; alpha 2× diretamente do original. Densidade `pixelScale: 2`, preservando a escala visual. Perfil `player_base.json` e worldScale 1,075 mantidos.

Atlas de runtime `android/app/src/main/res/drawable-nodpi/p03_idle.png`: 2944×3824, células 368×478, root (143,463), cropOffset (113,13), 8 colunas, 60 frames. Dimensões abaixo de 4096; memória decodificada de 45.031.424 bytes, aproximadamente 42,9 MiB, abaixo do orçamento de 64 MiB. O corte é compartilhado pelos frames, sem alinhamento individual. O estado COMBAT utiliza o primeiro frame do mesmo atlas e recebe a mesma densidade.

Originais preservados e declarados em `sourceReferences`. Nenhuma alteração em `characters/p03/character.json`, hitboxes, golpes, outros estados do P03, renderer ou perfil. Assets melhorados de P01 e P02 preservados. Só o atlas gerado do idle P03 e as entradas correspondentes nos arquivos Java são substituídos.

## Validação

`build_characters.py --write` e `--check`: PASS, 10 personagens, 310 atlases. 15 BuiltPackTests passaram em 76 segundos, incluindo o teste específico do P03 e regressões de P01 e P02. `git diff --check`: PASS. O teste P03 verifica 60 frames, sequência, loop, 41 ms, alpha, root proporcional, dimensões, densidade, proveniência e orçamento; os testes P01 e P02 também fazem parte da mesma bateria.

Uma falha no script de preparação foi corrigida antes de gerar o atlas de integração. Não houve falha de geração de frames nem necessidade de regenerar os masters. Prévia reduzida validada: imagens incorporadas, controles, rosto, sobreposição e transição 059→000.

Diagnóstico temporal na resolução original, incluindo 059→000, em pixels conjuntamente opacos: diferença RGB média original 9,455689, resultado reduzido 10,329366. Sem compensação de movimento; o aumento não demonstra sozinho piora ou melhora e não garante ausência de tremulação. A ampliação preserva contorno e pose, mas reconstrói linhas e cores internas. Aprovação visual exige observar a comparação e o jogo.

## Reprodução e decisão

Masters e scripts em `art/redraws/p03_hd/idle_sr_4x/`, com licença e manifest. Pesos não incluídos. Dependências: Python, torch CPU, Pillow, numpy e scipy.

```sh
python tools/sprites/prepare_p03_idle_sr.py --frames PASTA_MASTERS --manifest docs/art/p03-idle-sr.manifest.json --original art/sprites/source/p03_idle_video_normalized.png --output art/sprites/source/p03_idle_sr_2x.png
python tools/sprites/build_characters.py --write
```

Para testar no jogo, gerar o APK da `game-luta-sprite-gpt` em outro fluxo e selecionar `p03`. Observar olhos, cabelo, luvas, cadarços e faixa vermelha, tanto na velocidade normal quanto em 1/2 e 1/4, incluindo 059→000 e a mudança entre idle e movimento. Decidir se o método fica como padrão só após essa avaliação. Este teste não constitui aprovação automática do P03 ou do método para todas as animações.
