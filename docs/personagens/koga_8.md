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
- Golpes: L, M, H, agachados, no ar (jL, jM, jH, j2H), S1 e S2. Sem S3, S4, Super, Ultra nem
  agarrão. Provocação (TAUNT): o boneco do treino provoca quando o jogador erra.
- **S1 "Fumacinha"** (↓↘→): o vídeo inteiro no tempo dele (5,2 s): carrega, dá o tapa com a palma
  (o clarão), sai só uma fumacinha e ele encolhe o ombro. Sem projétil: o dano é só o do tapa, de
  perto (`s1.corpoACorpo`).
- **S2 "Socos girando"** (→↓↘): o vídeo inteiro no tempo dele, andando para a frente (170 px até
  encostar no rival), um acerto por soco (7, `pancadas.json`); o dano do golpe é dividido entre eles.
- Montagens (`tools/sprites/montagens/koga_8.json`): idle (o vídeo inteiro de 2 em 2, câmera fixa,
  pés plantados, a folha HD cabe nos 4096 px), dash (um ciclo da corrida, quadros 16–39, em loop),
  backdash (o pulinho inteiro, 31 quadros, 30 quadros no motor a 400 px/s), S1 e S2.
- Mecânicas em `tools/sprites/mecanicas.json` (vida 8000, dano 0,6).
