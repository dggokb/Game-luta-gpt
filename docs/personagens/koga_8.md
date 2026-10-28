# Kōga com 8 anos (`koga_8`)

Lutador só da história (`characters/roster.json` > `historia`, fora da seleção): o treino
(tutorial) e a briga dos irmãos no Prólogo. Por enquanto nos dois lados, até o Ryūga com 8
anos existir.

- Vídeos no Drive em `koga_8` (28 vídeos e as imagens iniciais), convertidos pelo
  `personagem.py` (`tools/sprites/videos/koga_8.json`) e refeitos em HD.
- Tamanho: criança, 165 px de guarda (~73% do Kōga adulto, 224). As imagens iniciais foram
  geradas maiores que as do p01, então a escala está fixa em `art/keys/koga_8/tamanho.json`.
  Os vídeos agachados e no ar começam fora da imagem inicial: escala manual 0,30, a mesma
  câmera do idle.
- Caixas (corpo, golpes, posturas) medidas no desenho dele (`caixas_sprites.py`).
- Golpes: L, M, H, agachados, no ar (jL, jM, jH, j2H), S1 "Fumacinha" (a energia que ainda
  não sai: um estouro curto, ↓↘→) e S2 "Socos girando" (→↓↘). Sem S3, S4, Super, Ultra nem
  agarrão. Provocação (TAUNT): o boneco do treino provoca quando o jogador erra.
- Mecânicas em `tools/sprites/mecanicas.json` (vida 8000, dano 0,6).
