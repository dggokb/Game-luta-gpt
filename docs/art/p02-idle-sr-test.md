# P02: idle com super-resolução para teste

Integrado na `game-luta-sprite-gpt`, nossa branch de testes, em 09/10/2026 a pedido do usuário. O teste original está em `test/p02-idle-sr-20261009`, commit `5dfaf8f830931ddfb0b134984c74be11d727d22a`, baseado em `f141c542607e7b642c4d40e256d25d45039ad51b`. O P01 com idle e caminhada melhorados permanece intacto. Aprovação visual do P02 pendente. Nenhum APK gerado para esta integração.

## Método e resultado

56/56 frames RGBA 768×1024, sem falhas. Mesmo Real-ESRGAN AnimeVideo-v3 e tratamento de transparência do idle P01 aprovado. Modelo oficial SHA256 `b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d`.

RGB sob alpha abaixo de 128 estendido a partir do pixel visível mais próximo antes da inferência. RGB reconstruído uniformemente em 4×; alpha diretamente do original por Lanczos, verificado exatamente nos 56 masters. Sem redesenho por prompt, alinhamento individual ou suavização temporal. Referência visual exclusivamente o P02 original.

Original `art/sprites/source/p02_idle_video_normalized.png`, baixado do GitHub pelo navegador Work e comparado byte a byte com o repositório. SHA256 `4dbe1158c6f2138f1068c5855caddd73739fdf31e847fed71891cf32c48163b0`. Grade 2112×1536, 11 colunas, células 192×256, root (97,238). Frames 000–055 e duração de 41 ms preservados. Os masters são 768×1024, mantendo a proporção original.

## Integração de teste no jogo

Fonte `art/sprites/source/p02_idle_sr_2x.png`, 4224×3072, células 384×512, root (194,476), 11 colunas e 6 linhas. Redução uniforme dos masters com RGB premultiplicado; alpha 2× diretamente dos originais. Atlas empacotado `android/app/src/main/res/drawable-nodpi/p02_idle.png`: 3652×2670, células 332×445, root (169,429), offset de corte (25,47), densidade `pixelScale: 2`. Memória decodificada: 39.003.360 bytes, aproximadamente 37,2 MiB. Ambas as dimensões do atlas de runtime ficam abaixo de 4096.

O renderer e a prévia já suportam pixelScale e mantêm a escala visual do personagem. O estado COMBAT usa o primeiro frame do mesmo atlas e recebe a mesma densidade. A altura visual medida pelo gerador passa de 223,6 para 224,1375 devido ao limite do alpha ampliado, uma diferença equivalente a meio pixel na resolução original; o root e worldScale permanecem proporcionais. Nenhuma mudança em renderer, perfil, hitboxes ou `characters/p02/character.json`.

Original preservado e declarado em `sourceReferences`. A receita original do vídeo continua no clipe para proveniência. Para refazer a fonte melhorada, usar o script de preparação abaixo, não reconverter o vídeo diretamente sobre a fonte SR.

## Validação

`build_characters.py --write` e `--check`: PASS, 10 personagens e 310 atlases. 14 BuiltPackTests passaram, incluindo teste específico do P02: 56 alphas, proporções, root, sequência, duração, densidade, proveniência e orçamento de memória, mais regressões do P01. `git diff --check`: PASS.

Diagnóstico temporal em resolução original, sobre pixels conjuntamente opacos, incluindo 055→000: diferença RGB média de 5,176934 no original e 5,477865 no resultado reduzido. Não há compensação de movimento; a métrica não garante ausência de tremulação. A reconstrução altera linhas e cores internas; a aprovação visual deve ser feita na comparação e no jogo.

## Reprodução

Masters, scripts, manifest e licença em `art/redraws/p02_hd/idle_sr_4x/` nesta branch. Pesos do modelo não incluídos. Dependências: Python, torch CPU, numpy, scipy e Pillow.

```sh
python tools/sprites/prepare_p02_idle_sr.py --frames PASTA_MASTERS --manifest docs/art/p02-idle-sr.manifest.json --original art/sprites/source/p02_idle_video_normalized.png --output art/sprites/source/p02_idle_sr_2x.png
python tools/sprites/build_characters.py --write
```

Para testar no jogo, gerar o APK da `game-luta-sprite-gpt` em outro fluxo, selecionar `p02` e observar olhos, cabelo, mãos e faixa na transição 055→000 e na passagem entre idle e movimento. `player_two` é outro pacote; selecionar `p02` para este teste.
