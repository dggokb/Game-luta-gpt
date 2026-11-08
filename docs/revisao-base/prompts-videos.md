# Prompts dos vídeos da revisão da base

> **Efeitos (seções 1 e 2): os 10 vídeos chegaram e estão no jogo** (ver o README, "Andamento"). Falta só a seção 3 (Ryūga), que fica para depois.

São as partes da revisão ([README.md](README.md)) que dependem de vídeo. Os outros itens são só código e já estão no jogo.

- **Efeitos** seguem o padrão de [docs/efeitos-de-video.md](../efeitos-de-video.md):
  - fundo verde chapado, sem personagem, 1–2 s, 24 fps;
  - convertidos com `tools/sprites/efeito_video.py`;
  - cada vídeo novo entra sozinho no lugar do efeito antigo.
- **Animações do Ryūga** seguem o padrão de [docs/personagens/video.md](../personagens/video.md) e o formato dos vídeos novos dele (Flick/H3):
  - imagem inicial `inicio_centro_h3` (ou a indicada);
  - visão lateral, câmera parada, sem efeitos e sem adversário.

## 1. Efeitos de acerto e defesa (estilo Dragon Ball FighterZ)

O que muda em relação aos efeitos atuais, tirado da análise das referências:
1. **Nasce grande.** No quadro do contato a faísca já está no tamanho máximo; ela não cresce, só quebra e some.
2. **Tem direção.** Os espinhos e raios apontam para onde o golpe vai (a direita; o jogo espelha).
3. **Tem cor e escala.** Fraco, médio e forte são visivelmente diferentes, e a defesa é fria (ciano), nunca da cor do acerto.
4. **Duas fases.** Explosão chapada, depois quebra em chama, cacos ou pontos pretos de tinta, que somem.
5. **Mesmo desenho dos personagens:** chapado, 2 tons, contorno de pincel.

Texto base, igual em todos (troque o `[EFEITO]`):

> Efeito visual 2D de anime para jogo de luta, no estilo do Dragon Ball FighterZ, desenhado à mão, com cores chapadas em 2 tons (base e sombra) e um brilho, contorno escuro de pincel, sem degradê e sem nada verde. [EFEITO]. Fundo verde chapado #00FF00, sem personagem, sem chão, sem sombra, câmera parada, efeito centralizado. O vídeo começa vazio e termina vazio. O efeito aparece de uma vez, já no tamanho máximo, no segundo quadro (não cresce aos poucos).

| Pasta | Converter com | [EFEITO] |
|---|---|---|
| `impacto_fraco` | `--ancora centro --altura 120 --passo 2` | uma estrela de impacto com núcleo amarelo-claro quase branco e 8 espinhos curtos laranja, mais longos para a direita, e 3 riscos retos finos brancos saindo para a direita; dura poucos quadros, se parte em 4 ou 5 lascas laranja e alguns pontinhos pretos de tinta e some rápido |
| `impacto_medio` | `--ancora centro --altura 170 --passo 2` | uma explosão de impacto com núcleo amarelo-claro quase branco, 10 espinhos serrilhados laranja e vermelho apontando sobretudo para a direita, e 5 riscos retos brancos longos saindo para a direita; quebra em chama laranja pequena, lascas e pontinhos pretos de tinta voando para a direita, e some |
| `impacto_forte` | `--ancora centro --altura 260` | uma explosão de impacto enorme: núcleo amarelo-claro quase branco, estrela de 12 espinhos longos vermelho-alaranjados apontando para a direita, e raios retos brancos e laranja muito longos que atravessam o quadro inteiro por 2 quadros. Depois a estrela se quebra em uma chama laranja menor, lascas e muitos pontos pretos de tinta voando para a direita, e tudo some |
| `impacto_contra` (novo) | `--ancora centro --altura 280` | a mesma explosão de impacto enorme de golpe forte, mas com o núcleo branco e os espinhos em magenta e roxo com bordas laranja, um anel fino branco que expande rápido em volta e raios retos longos atravessando o quadro; quebra em lascas roxas e pontos pretos de tinta e some |
| `defesa` | `--ancora centro --altura 110 --passo 2` | uma faísca de bloqueio fria: um arco curto em forma de escudo, de cima para baixo, curvado para a direita, em ciano e azul-claro com borda branca, que se parte em 5 ou 6 cacos azuis triangulares voando para a direita e some rápido; nada amarelo nem laranja |
| `arco_golpe` | `--ancora centro --altura 200 --passo 2` | o rastro desenhado de um chute forte: uma meia-lua branca grossa com borda azul-clara, de baixo para cima e para a direita, com 3 linhas de velocidade finas acompanhando a curva; aparece inteira, fica parada 3 quadros e se desfaz em fiapos |

`impacto_contra` precisa de uma linha de código para tocar no contra-ataque e na punição. Eu ligo quando o vídeo chegar.

## 2. Poeiras (não existem hoje; o jogo usa um sopro desenhado no código)

Texto base: o mesmo de cima. Para a poeira, as cores são bege e cinza-claro em 2 tons, com sombra lilás, nas cores de um chão de pedra.

| Pasta | Converter com | [EFEITO] |
|---|---|---|
| `fumaca_pulo` | `--ancora chao --altura 70 --passo 2` | um sopro de poeira pequeno no chão, abrindo em leque espetado para os dois lados, baixo e rápido |
| `fumaca_pouso` | `--ancora chao --altura 90 --passo 2` | poeira de aterrissagem: dois leques espetados baixos saindo para os dois lados de um ponto no chão, que viram nuvens fofas e somem |
| `poeira_corrida` | `--ancora chao --altura 100 --passo 2` | poeira e lascas de pedra espirrando para a esquerda em leque espetado, como um pé arrancando com força para a direita |
| `poeira_freada` | `--ancora chao --altura 80 --passo 2` | poeira baixa arrastada no chão para a direita em rolos fofos, como um pé freando |

## 3. Animações do Ryūga que ainda são a arte antiga

Na análise, as reações de dano foram o maior buraco depois do golpe em si:
- um clipe só em pé e um só agachado, esticados;
- nas referências, a reação muda com a altura e a força do golpe (cabeça para trás, dobrar ao meio, voar);
- todo vídeo começa e termina na guarda da imagem inicial e não tem nenhum efeito (o jogo põe faísca, poeira e tremor).

Fim comum de todos os prompts (cole depois de cada um):

> His feet stay planted and never slide unless the description says he is pushed. He never turns toward the camera. Keep his face, brown hair, blue-and-white gi, black inner shirt, belt, wraps, white pants, sneakers, colors, line art and size exactly as in the start frame; his size never changes. The camera never zooms in or out; the character's size stays the same as in the first frame for the whole video. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.

Início comum:

> Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. He faces RIGHT the whole time; the camera stays fixed and never zooms.

| Animação (estado) | Imagem inicial | Meio do prompt |
|---|---|---|
| Dano alto, no rosto (`HIT_STAND`, alto) | `inicio_centro_h3` | [0-1s] He holds his fighting guard. [1-2s] An invisible blow hits his FACE from the right: his head snaps hard backward and to the left, chin up, eyes squeezed shut, mouth open in pain, his shoulders and upper body jerk back while his feet stay planted; the pose is strong and exaggerated and he HOLDS it for a moment. [2-3s] He recovers quickly, shaking it off, and returns to the same fighting guard, holding it until the end. |
| Dano no tronco (`HIT_STAND`, baixo/médio) | `inicio_centro_h3` | [0-1s] He holds his fighting guard. [1-2s] An invisible blow hits his STOMACH from the right: he folds forward at the waist, arms pulled in over his belly, head dropping forward, eyes wide, mouth open gasping, knees bending a little, feet planted; he HOLDS this folded pose for a moment. [2-3s] He straightens up and returns to the same fighting guard, holding it until the end. |
| Dano forte em pé (empurrado) | `inicio_centro_h3` | [0-1s] He holds his fighting guard. [1-2s] A very strong invisible blow hits his chest from the right: his whole body is thrown backward to the LEFT in a big arc, back bent, arms flung back and open, head thrown back, one foot lifting off the ground, and he slides back about one body width; strong exaggerated pose held for a moment. [2-3s] He plants his feet, catches his balance and returns to his fighting guard at the new spot, holding it until the end. |
| Dano no ar (`HIT_AIR`) | `inicio_ar_h3`* | He is in mid-air in his jump pose. An invisible blow hits him from the right: his body bends backward in the air, arms and legs flung out to the left, head thrown back, pained face; he stays in this pose, turning slightly backward, for the rest of the video. He does not land. |
| Queda e levantar (`KNOCKDOWN`) | `inicio_centro_h3` (fim = `inicio_centro_h3`) | [0-1s] He holds his fighting guard. [1-2s] He is knocked off his feet by an invisible blow from the right: he flies backward to the LEFT, spinning his body back in the air, and slams flat on his back on the ground, bouncing once slightly. [2-3s] He lies still on his back for a moment. [3-4.5s] He quickly rolls to his side and springs back up in one smooth motion, landing in his fighting guard, and holds it until the end. |
| Defesa agachada (`BLOCK_CROUCH`) | `inicio_agachado_h3`* | He starts in a deep crouching guard and stays crouched the ENTIRE video, never standing up. Mid-video he blocks an invisible low attack from the right with both forearms crossed in front of his face and chest, his body pushed back a little while his knees stay bent and hips stay low, jaw clenched; he holds the block, then returns to the same deep crouching guard until the end. |
| Defesa no ar (`BLOCK_AIR`) | `inicio_ar_h3`* | He is in mid-air in his jump pose. He raises both forearms crossed in front of his face to block an invisible attack from the right, his body tucked and pushed slightly to the left, jaw clenched; he holds this air block pose until the end. He does not land. |
| Intro (`INTRO`) | `inicio_centro_h3` (fim = `inicio_centro_h3`) | [0-2s] He stands relaxed and upright with eyes closed, breathing in deeply. [2-3.5s] He opens his eyes with a calm, focused look, tightens his belt with both hands in one firm pull. [3.5-5s] He slides smoothly into his fighting guard and holds it until the end. |
| Vitória (`VICTORY`) | `inicio_centro_h3` | Use the current victory prompt from `docs/personagens/p01.md` (calm, restrained), only with the new start frame. |

\* Ainda não existem em `art/keys/p01/`:
- `inicio_agachado_h3`: é o quadro mais baixo do vídeo novo de agachar, como diz a ficha do p01.
- `inicio_ar_h3`: é o quadro do topo do vídeo novo de pulo, no mesmo tamanho, sobre o verde.

Eu tiro os dois dos vídeos que já existem, se você quiser.

Depois de converter, as reações entram no lugar dos clipes antigos. Para segurar as poses-chave (a "pose de impacto"), eu marco no `golpes.json` quais imagens ficam paradas e por quantos quadros. Isso não precisa de vídeo novo: vale para estes e para os golpes que já existem.
