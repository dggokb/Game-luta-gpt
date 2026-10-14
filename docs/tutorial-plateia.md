# Tutorial: Takatoshi e Dafu assistindo e falando nas laterais

O treino do tutorial (no Dojo Shūshinkan) tem duas coisas a mais:

1. **Quem assiste.** Takatoshi (45 anos, o pai) e Dafu ficam no fundo do dojo, perto da parede,
   um de cada lado do altar. Eles reagem ao que acontece no treino:

   | Acontecimento | Quando | Takatoshi | Dafu |
   |---|---|---|---|
   | `licao` | o jogador completou a lição | aprovar ou aplaudir | aplaudir ou rir |
   | `impacto` | golpe forte acertou (H, ↓+H, golpe no ar, especiais, agarrão, SUPER, ULTRA, parede/quique) | surpreso | surpreso |
   | `levou` | o boneco acertou o jogador (lições de defesa) | rir | rir |
   | `fim` | treino completo | aplaudir | aplaudir ou rir |

   Quem reage o quê fica no `ator.json` de cada um (`reacoes`), dá para mudar sem mexer no código.
   Cada um reage com um atrasinho diferente, para não parecerem robôs.

2. **Os dois falando nas laterais, sem parar a luta.** Takatoshi aparece num quadro de mangá
   pequeno à esquerda e Dafu à direita, logo abaixo das barras de vida. Quando uma lição
   começa, quem explica entra deslizando, fala num balão digitado e sai sozinho; ninguém
   precisa tocar e o treino não espera. Eles também comentam o que o jogador faz (uma fala
   sorteada, só quando ninguém está falando): `licao` (cumpriu a lição), `impacto` (acertou
   forte) e `levou` (apanhou). No fim do treino a conversa do `fim` termina antes da história
   seguir (PULAR corta). O rosto troca de expressão conforme a fala.

   As falas, as expressões e as reações ficam em
   `android/app/src/main/assets/historia/ep00/conversa_treino.json` (motor e formato: [historia/motor-conversas.md](historia/motor-conversas.md)); as traduções, em
   `historia/idiomas/` como o resto da história. Durante o tutorial os botões de teste (IA,
   DEBUG, VIDA, DUPLA, HISTÓRIA, TUTORIAL) somem: o canto direito é do quadro do Dafu.

Enquanto os vídeos e as imagens não chegam, já funciona: os dois aparecem parados no fundo
(recortados do concept, respirando de leve) e as reações usam um movimento provisório (pulinho
de surpresa, balanço de riso). Nos quadros laterais, o rosto "normal" de cada um é um recorte
do concept; as outras expressões usam ele até a imagem existir.

## Os vídeos (Seedance)

Cada ator precisa de **5 vídeos**. São as mesmas regras dos lutadores ([personagens/video.md](personagens/video.md)):
16:9, fundo verde puro, câmera parada, sem efeitos, começa e termina na imagem inicial. A
diferença: **eles ficam de frente (3/4), olhando para o centro do dojo**, não de lado, e não
lutam. O jogo põe o Takatoshi à esquerda do altar e o Dafu à direita.

| Animação | Duração | O que ele faz |
|---|---|---|
| `parado` | ~3 s, loop | pose padrão respirando (vira o loop do fundo) |
| `aplaudir` | ~2 s | bate palma 3 ou 4 vezes e volta à pose |
| `rir` | ~2 s | ri (ombros mexendo), volta à pose |
| `surpreso` | ~1,5 s | se espanta com o golpe, recua um pouco e volta |
| `aprovar` | ~1,5 s | acena com a cabeça / joinha, volta à pose |

### Imagem inicial (uma por ator, no ChatGPT)

Anexe o concept (`art/concepts/takatoshi_45.png` ou `art/concepts/dafu.png`) e peça:

```
Recorte e isole a figura de corpo inteiro de frente deste model sheet (a primeira, em pé).
Não redesenhe e não mude nada: mesmo traço, mesma pintura, mesmas cores e proporções.
Coloque-a sobre um fundo verde puro (#00FF00) em 1280x720, centralizada, ocupando cerca de
80% da altura, com espaço em cima e dos lados para os braços. Sem chão, sem sombra, sem texto.
```

Guarde em `art/keys/takatoshi/inicio_centro.png` e `art/keys/dafu/inicio_centro.png`. **Todos os
vídeos do ator começam nessa imagem** (é por ela que o tamanho fica igual em todas as animações).

### Prompt fixo (colar sempre e trocar o CHARACTER e o ACTION)

```
Animate the character from the reference image as a 2D anime animation: smooth and fluid,
hand-drawn look, same line art, shading and colors as the image.

CHARACTER (keep exactly as in the image, do not redesign): <colar o personagem>

CAMERA: completely static, fixed framing, no zoom, no pan, no rotation, no cuts. The camera
never zooms in or out; the character's size stays the same as in the first frame for the
whole video. Front three-quarter view, the same angle as the image, for the whole video. His
whole body always stays inside the frame and his feet stay on the same spot.

BACKGROUND: flat pure green (#00FF00). No floor, no shadow, no dust, no speed lines, no
motion trails, no sparkles, no particles, no text, no speech bubbles.

He is a martial arts master watching his students train, standing at the back of the dojo.

ACTION: <colar aqui>
```

**CHARACTER — Takatoshi:** `a tall, muscular Japanese martial arts master in his mid forties,
messy dark brown hair, short goatee, stern and confident look; sleeveless white long coat with
navy blue and red trim and a navy dragon pattern, high red collar, black inner gi, black and red
sash belt with gold ornaments, black baggy pants, black fingerless gloves with red and navy wrist
guards, red, white and black high-top sneakers.`

**CHARACTER — Dafu:** `a big, heavy and muscular old martial arts master, black hair tied in a
messy topknot, long thin drooping mustache, always smiling with his eyes almost closed;
sleeveless yellow vest with golden cloud patterns over a cream shirt, wide red sash with a gourd
hanging at the waist, black baggy pants with wrapped ankles, black fingerless wrist wraps, black
cloth shoes.`

### ACTION de cada vídeo

| Ator | Animação | ACTION |
|---|---|---|
| Takatoshi | parado | "He stands relaxed with his arms crossed over his chest, weight on both feet, calm and watchful expression, breathing slowly in a seamless loop: chest rising and falling, a slight shift of weight, the long coat moving very softly. He never leaves the pose. The first and last frames match, so it loops." |
| Takatoshi | aplaudir | "Starting from the pose in the image, he uncrosses his arms and claps his hands four times in front of his chest, firmly, with a proud half smile, then crosses his arms again and holds the pose in the image until the end of the video." |
| Takatoshi | rir | "Starting from the pose in the image, he lets out a short laugh: head tilting back a little, shoulders shaking, a wide amused smile, one hand going to his chin; then he calms down and returns to the pose in the image and holds it until the end of the video." |
| Takatoshi | surpreso | "Starting from the pose in the image, he is suddenly impressed by something in front of him: eyes widening, eyebrows rising, his upper body leaning back a little and his arms coming apart in surprise; then he relaxes with an impressed smile and returns to the pose in the image and holds it until the end of the video. His feet stay in place." |
| Takatoshi | aprovar | "Starting from the pose in the image, he nods twice with a small satisfied smile and gives a short thumbs-up with his right hand, then returns to the pose in the image and holds it until the end of the video." |
| Dafu | parado | "He stands relaxed with his hands resting on his big belly, smiling with his eyes almost closed, breathing slowly in a seamless loop: belly rising and falling, a slight sway, the long mustache moving very softly. He never leaves the pose. The first and last frames match, so it loops." |
| Dafu | aplaudir | "Starting from the pose in the image, he raises his hands and claps enthusiastically four times in front of his chest with a big cheerful smile, then returns to the pose in the image and holds it until the end of the video." |
| Dafu | rir | "Starting from the pose in the image, he bursts into a big belly laugh: head thrown back, mouth wide open, belly and shoulders shaking, one hand patting his belly; then he calms down and returns to the pose in the image and holds it until the end of the video." |
| Dafu | surpreso | "Starting from the pose in the image, he is suddenly amazed by something in front of him: his eyes pop open wide, eyebrows up, mouth open in an 'oh!', both hands rising to the sides of his face, body leaning back a little; then he laughs it off and returns to the pose in the image and holds it until the end of the video. His feet stay in place." |
| Dafu | aprovar | "Starting from the pose in the image, he nods happily twice and strokes his long mustache with one hand, smiling, then returns to the pose in the image and holds it until the end of the video." |

### Andamento

| Ator | parado | aplaudir | rir | surpreso | aprovar |
|---|---|---|---|---|---|
| Takatoshi | ✅ | ✅ | ✅ ¹ | ✅ ¹ | ✅ ¹ |
| Dafu | ✅ | ✅ | ✅ ² | ✅ ² | ✅ |

¹ Começa e termina de braços soltos, e o parado é de braços cruzados. O jogo faz uma
transição curta (0,22 s) entre as animações, então não "pula", mas fica melhor refazer
começando da imagem inicial de braços cruzados (`art/keys/takatoshi/inicio_centro.png`, o
primeiro quadro do parado), escrevendo no começo do ACTION: "Starting from the pose in the
image, with his arms crossed over his chest, ..." e no fim "... then crosses his arms again and
holds the pose in the image until the end of the video."

² Vieram com fundo bege claro em vez de verde. A ferramenta percebe sozinha (`--fundo auto`) e
recorta a partir das bordas, parando no contorno do desenho, para não apagar a camisa creme do
Dafu; também tira a sombra cinza do chão e o fundo preso entre as pernas. Ficou bom, mas
fundo verde continua sendo o mais seguro.

O verde do Seedance veio mais escuro e em degradê, e o recorte funcionou do mesmo jeito. Os 5
do Dafu começam e terminam na mesma pose (mãos na cintura), então as trocas não precisam de
transição para emendar.

### Converter (quando os vídeos chegarem)

```bash
pip install -r tools/sprites/requirements-animar.txt
A=android/app/src/main/assets/stages/dojo_shinshinkan/atores
python3 tools/stage/video_para_ator.py takatoshi_parado.mp4 --ator $A/takatoshi --animacao parado --loop --medir
python3 tools/stage/video_para_ator.py takatoshi_aplaudir.mp4 --ator $A/takatoshi --animacao aplaudir
# ... o mesmo para rir, surpreso, aprovar e para o Dafu
```

- Converta o **parado primeiro** (com `--medir`): ele mede o tamanho e o lugar dos pés, que ficam
  gravados no `ator.json` e valem para os outros vídeos do mesmo ator.
- Sem `--inicio`/`--fim`, a ferramenta corta sozinha a pose parada do começo e do fim (e, com
  `--loop`, acha o trecho que fecha melhor em loop). `--fps` muda a velocidade no jogo (10).
- As folhas saem com paleta de 256 cores (`--cores 0` desliga): ~5x menores, sem diferença visível.
- A ferramenta grava `<animacao>.png` na pasta do ator e preenche a animação no `ator.json`.
- Confira no PC: `gradle :pc-preview:runStage --args="app/src/main/assets/stages/dojo_shinshinkan"`
  (no visualizador eles reagem sozinhos a cada 3 s), ou uma folha com cada animação em vários
  instantes: `gradle :pc-preview:runAtor --args="app/src/main/assets/stages/dojo_shinshinkan takatoshi parado,aplaudir,rir,surpreso,aprovar 345 build/ator.png"`.
- Pode me mandar os vídeos que eu converto e ajusto.

## Os rostos dos quadros laterais (ChatGPT)

Quatro expressões de cada um, salvas em `android/app/src/main/assets/historia/retratos/`
como **PNG** com estes nomes (a expressão que falta usa a `normal`):

| Arquivo | Expressão | Quando aparece |
|---|---|---|
| `takatoshi_normal.png` | sério, calmo | explicando a lição |
| `takatoshi_sorrindo.png` | meio sorriso orgulhoso | "Bom.", "Isso. De novo, sem pensar.", fim do treino |
| `takatoshi_surpreso.png` | sobrancelha erguida, impressionado | "Hm. Boa força." |
| `takatoshi_bravo.png` | firme, cobrando | "Guarda alta!", especial de energia, Ultra |
| `dafu_normal.png` | o sorriso de sempre, olhos fechados | explicando a lição |
| `dafu_rindo.png` | gargalhada, boca aberta | "Hohoho!", "Muito bem, garoto!" |
| `dafu_surpreso.png` | olhos arregalados (raro nele) | "Opa! Que pancada!" |
| `dafu_preocupado.png` | sem graça, gota de suor | "Ai, essa doeu!", "cuidado com o meu dojo!" |

O quadro na tela é um retrato (mais alto que largo) e mostra **da cabeça aos ombros**, com o
rosto no terço de cima. **Takatoshi olha para a direita e Dafu para a esquerda** (os dois
olham para o centro da tela). Gere em **1024x1024**, um rosto por imagem, com o fundo liso (o
jogo pinta o fundo do quadro com a cor de cada um e as linhas de foco).

Anexe o concept de quem aparece (`art/concepts/takatoshi_45.png` ou `art/concepts/dafu.png`) e,
a partir da segunda imagem, a primeira que ficou boa (para manter o mesmo rosto). Cole antes de
cada prompt:

```
Retrato de mangá colorido, no estilo de anime de luta dos anos 90 (traço de tinta preto e
grosso, cores chapadas e vibrantes, sombras duras, muito contraste). Exatamente o personagem
do concept anexado: mesmo rosto, cabelo, roupa e cores. Enquadramento da cabeça aos ombros,
rosto grande ocupando o terço de cima e o centro da imagem, cabeça inteira dentro da imagem.
Fundo liso de uma cor só, cinza claro, sem cenário. Sem texto, sem balões, sem moldura, sem
marca d'água. Quadrado 1024x1024.
```

**Takatoshi** (45 anos, concept `takatoshi_45.png`), corpo virado de três quartos, **olhando
para a direita da imagem**:

| Arquivo | Prompt |
|---|---|
| `takatoshi_normal.png` | Expressão séria e calma de mestre, olhar firme para a direita, boca fechada, sobrancelhas levemente franzidas. |
| `takatoshi_sorrindo.png` | Meio sorriso orgulhoso de canto de boca, olhos um pouco semicerrados, satisfeito, olhando para a direita. |
| `takatoshi_surpreso.png` | Impressionado: uma sobrancelha erguida, olhos um pouco mais abertos, boca levemente entreaberta, olhando para a direita. |
| `takatoshi_bravo.png` | Firme e cobrando: sobrancelhas franzidas, dentes cerrados, falando alto, olhar intenso para a direita, sombra dura no rosto. |

**Dafu** (concept `dafu.png`), corpo virado de três quartos, **olhando para a esquerda da
imagem**:

| Arquivo | Prompt |
|---|---|
| `dafu_normal.png` | O sorriso bonachão de sempre, olhos fechados de tanto sorrir, bochechas redondas, virado para a esquerda. |
| `dafu_rindo.png` | Gargalhada: boca bem aberta, cabeça um pouco para trás, olhos fechados, bigode balançando, virado para a esquerda. |
| `dafu_surpreso.png` | Espantado: pela primeira vez os olhos abertos e arregalados, boca em "O", bigode levantado, virado para a esquerda. |
| `dafu_preocupado.png` | Sem graça e preocupado: sorriso torto, sobrancelhas inclinadas para cima, gota de suor na testa, mão coçando a nuca, virado para a esquerda. |

Se o GPT virar o rosto para o lado errado, é só espelhar a imagem. Pode me mandar as imagens
que eu recorto, tiro o fundo e coloco no lugar. Para mudar uma fala ou a expressão dela, mude
no `ep00/conversa_treino.json` (e a tradução em `historia/idiomas/`).
