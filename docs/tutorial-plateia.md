# Tutorial: Takatoshi e Dafu assistindo, e os quadros entre as lições

O treino do tutorial (no Dojo Shinshinkan) tem duas coisas a mais:

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

2. **Um quadro de mangá antes de cada lição** (e um no fim). O quadro bate na tela por cima da
   luta, o Takatoshi ou o Dafu falam (balão digitado) e o toque passa. Se ninguém tocar, o
   balão passa sozinho em ~3 s. As falas estão em
   `android/app/src/main/assets/historia/tutorial/quadros.json` (uma entrada por lição).

Enquanto os vídeos e as imagens não chegam, já funciona: os dois aparecem parados (recortados
do concept, respirando de leve) e as reações usam um movimento provisório (pulinho de
surpresa, balanço de riso). Os quadros usam a arte provisória da história (silhuetas nas
cores do elenco e o nome do arquivo esperado).

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
| Dafu | provisório | provisório | provisório | provisório | provisório |

¹ Começa e termina de braços soltos, e o parado é de braços cruzados. O jogo faz uma
transição curta (0,22 s) entre as animações, então não "pula", mas fica melhor refazer
começando da imagem inicial de braços cruzados (`art/keys/takatoshi/inicio_centro.png`, o
primeiro quadro do parado), escrevendo no começo do ACTION: "Starting from the pose in the
image, with his arms crossed over his chest, ..." e no fim "... then crosses his arms again and
holds the pose in the image until the end of the video."

O verde do Seedance veio mais escuro e em degradê, e o recorte funcionou do mesmo jeito.

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

## As imagens dos quadros (ChatGPT)

Uma imagem por quadro, com o nome da lição (`andar.jpg`, `combo.jpg`...), em
`android/app/src/main/assets/historia/tutorial/`. O quadro na tela é **horizontal (~16:9)**.
**Sem balões e sem texto:** o jogo escreve as falas por cima (assim dá para traduzir).

Cole antes de cada prompt e anexe as páginas do mangá do dojo e os concepts dos dois:

```
Quadro único de mangá colorido, no mesmo estilo das páginas anexadas (anime anos 90, traço
limpo, cores quentes do dojo à noite, iluminação âmbar das lanternas). Interior do Dojo
Shinshinkan. Takatoshi e Dafu exatamente como nos concepts anexados. Horizontal 16:9.
Sem balões, sem texto, sem onomatopeias, sem moldura, sem marca d'água. Deixe espaço livre
na parte de cima do quadro para os balões.
```

| Arquivo | Cena |
|---|---|
| `andar.jpg` | Takatoshi de braços cruzados, sério, olhando para baixo (para o menino fora do quadro), plano médio. |
| `agachar.jpg` | Dafu inclinado para a frente rindo, mão espalmada para baixo mostrando "abaixa". |
| `pular.jpg` | Takatoshi apontando para cima com dois dedos, calmo. |
| `super-pulo.jpg` | Dafu rindo alto olhando para o teto, mãos na barriga. |
| `dash.jpg` | Takatoshi de perfil, olhar afiado, mão cortando o ar para a frente. |
| `recuo.jpg` | Takatoshi dando um passo para trás em guarda, demonstrando. |
| `golpe-fraco.jpg` | Dafu socando a própria palma, empolgado. |
| `golpe-medio.jpg` | Takatoshi tocando o próprio quadril, explicando o peso do golpe. |
| `golpe-forte.jpg` | Dafu gritando de punho erguido, linhas de foco atrás. |
| `fraco-agachado.jpg` | Takatoshi agachado demonstrando um soco baixo. |
| `medio-agachado.jpg` | Dafu coçando a nuca, rindo sem graça (lembrando que já levou uma rasteira). |
| `forte-agachado.jpg` | Takatoshi mostrando um gancho de baixo para cima, em silhueta contra a lanterna. |
| `golpe-no-ar.jpg` | Dafu apontando para o alto, olhos arregalados de empolgação. |
| `cravada.jpg` | Takatoshi com a mão descendo como um martelo, close dramático. |
| `combo.jpg` | Takatoshi e Dafu lado a lado; Takatoshi sério, Dafu sorrindo com o dedo levantado. |
| `botao-combo.jpg` | Dafu piscando um olho, sorriso maroto, polegar para cima. |
| `defesa.jpg` | Dafu em guarda de luta, sorrindo, Takatoshi ao lado de braços cruzados. |
| `defesa-baixa.jpg` | Takatoshi apontando para os próprios pés. |
| `especial-de-energia.jpg` | Takatoshi com as mãos juntas na cintura, um brilho suave entre elas, olhar concentrado. |
| `especial-s2.jpg` | Dafu desenhando no ar com o dedo o movimento de um dragão subindo. |
| `especial-s3.jpg` | Takatoshi meio de costas, olhar por cima do ombro, misterioso. |
| `especial-s4.jpg` | Dafu flexionando o braço, músculo enorme, rindo. |
| `agarrao.jpg` | Takatoshi com a mão estendida agarrando o ar, close na mão. |
| `super.jpg` | Dafu apontando para cima, aura dourada suave em volta dele. |
| `ultra.jpg` | Takatoshi de olhos fechados concentrado, Dafu atrás dele assustado protegendo o rosto. |
| `fim.jpg` | Os dois sorrindo, Dafu batendo palmas, Takatoshi com um meio sorriso orgulhoso, pétalas de cerejeira caindo. |

Gere em 1536x1024 (o jogo corta o meio). Se mudar alguma fala, mude no `quadros.json`.
