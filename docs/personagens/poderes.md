# Poderes e projéteis: os prompts

Os prompts dos vídeos e das imagens dos poderes, projéteis e objetos lançados de cada personagem.
Hoje só o Ryūga tem arte nos poderes (a Onda, a esfera do Super, o raio do Ultra e o Shinkai,
feitos de vídeo). Os outros são desenhados por código (`ProjetilVisual`, `EffectsRenderer`), e a
regra do dono é que cada um tenha o VFX dele (skill `vfx-luta`). Levantamento de 14/11, feito nos
dados do jogo.

## Como gerar

**Três tipos, cada um com o fundo dele:**

| Tipo | O que é | Fundo | Vira efeito com |
|---|---|---|---|
| **LUZ** (vídeo) | energia, fogo, estalo, explosão | **preto puro** | `tools/sprites/efeito_video_preto.py` |
| **FUMAÇA** (vídeo) | sombra, vapor, veneno: o que é escuro ou branco opaco e sumiria no preto | **verde** | `tools/sprites/efeito_video.py` |
| **OBJETO** (imagem) | kunai, machado, bisturi: coisa sólida que o jogo gira e move | transparente | `tools/sprites/efeito_imagem.py` |

**Regras de todo vídeo de poder:**

- Só o efeito: sem personagem, sem mão, sem chão, sem texto. O personagem é a animação dele; o
  poder sai da mão no jogo.
- Apontando para a **direita** (o jogo espelha).
- **Câmera parada e o efeito no meio do quadro.** O projétil não atravessa a tela: ele fica no
  centro, como se a câmera o seguisse, e quem o move é o jogo.
- **Projétil em voo: loop.** O primeiro e o último quadro são iguais (o jogo toca em loop enquanto
  ele voa). Para o nascimento e o estouro, um vídeo à parte.
- **Estouro (burst): curto e no começo do vídeo.** Nasce, abre no máximo, se desfaz e some; o
  resto do vídeo fica vazio (a ferramenta corta).
- Cada um na cor dele (tabela abaixo) e com a forma do estilo dele: a palma da garça não é a bola
  do Ryūga pintada de azul.
- **Nunca nomeie uma parte do corpo ou um objeto que não deve aparecer** ("mão de energia",
  "punho", "canhão"): o gerador desenha a coisa. O primeiro vídeo da palma (14/11) saiu uma mão
  gigante pegando fogo. Descreva a forma (crescente, anel, estrela, faixa, chama) e diga o que não
  quer ("no hand, no fingers").
- **Diga o tamanho no quadro** ("about one third of the frame height, lots of empty black around
  it"): sem isso o efeito enche a tela e não dá para recortar. Pequeno demais também não serve:
  o jogo amplia e borra. Projétil grande no jogo (a Onda, o Super) pede metade da altura do quadro.
- **Sem imagem inicial nem final**: o vídeo de poder é só texto. Com imagem, o gerador abre e
  fecha nela (o primeiro da palma começou e terminou num quadro verde).

**O começo de todo prompt de LUZ** (cole antes da descrição):

```
2D anime fighting-game visual effect, hand-drawn cel style like Guilty Gear Strive and Dragon Ball FighterZ: sharp cut shapes, clean energy edges, flat color bands from dark to white, saturated colors and a white-hot core. Pure black background (#000000) and nothing else: no character, no hands, no body, no ground, no text, no watermark. Static locked-off camera that never zooms; the effect stays in the center of the frame and points to the RIGHT. Unless said otherwise, the effect is small, about one third of the frame height, with lots of empty space around it.
```

**O começo de todo prompt de FUMAÇA:** o mesmo, trocando o fundo por *"Flat solid green
background (#00FF00) and nothing else"*.

**O começo de todo prompt de OBJETO** (imagem):

```
Object for a 2D anime fighting game, same line art as Guilty Gear Strive and Dragon Ball FighterZ: clean black outline, cel shading, saturated colors, crisp metal highlights. Seen exactly from the side. PNG with a transparent background: only the object, no shadow, no ground, no hands, no text, no watermark.
```

## Cor da energia de cada um

| Quem | Energia |
|---|---|
| p01 Ryūga | dourada e laranja, como o sol |
| p02 Yin-lu | azul-celeste e branca, leve, colada nas palmas (a aura do Shinkai dela é branca) |
| p03 Kōga | a dourada e laranja da família, mas explosiva e instável |
| p04 Brucy | laranja-dourada, curta, só no impacto do soco |
| p05 | roxa e escura como um eclipse |
| p06 | roxa e preta, sombra líquida como tinta |
| p07 | dourada como ouro e moedas |
| p08 | vermelho-escarlate, fitas de luz |
| p09 | fogo laranja e vermelho |
| p10 | vapor e água azul-clara |
| p11 | névoa verde-limão de veneno |
| p12 | vermelho-sangue e preto |
| p15 Natisuko | vermelha como fogo (a aura do Repel); **a confirmar** |

---

## p01 Ryūga

Tem arte em tudo, mas a Onda (S1) e a esfera do Super são de 08/11, de antes destas regras: a Onda
é uma gota dourada simples de 4 quadros, e o Super é a mesma ideia maior. Os dois precisam ser
diferentes um do outro (o dono, 14/11). O raio do Ultra, o Shinkai e o Ultra Shinkai ficam.

**S1 Onda: feita (14/11)** com o vídeo `s1_poder` do dono (o cometa com anéis da primeira
versão do prompt), na folha `onda_player_base`. O prompt abaixo (sem anéis) fica só de referência.

**S1 Onda (o voo, LUZ, loop): rápida e afiada, um cometa.** Voa 720 px a 760 px/s; carregada,
cresce. Troca a folha `onda_player_base` sem mexer em código.
```
[LUZ] A comet of solar energy flying fast to the RIGHT, gold and orange like the sun, big in the frame: the whole comet is about half of the frame height and two thirds of the frame width, centered. A round white-hot head at the front, wrapped in thin yellow and orange layers, and a long tail to the left that is wide at the head and tapers to a sharp point, torn into a few thin flame-like strands with tiny sparks breaking off. Sleek and fast, calm and concentrated. No rings, no lightning, no fist, no hand. [0-5s] The head flickers and the tail strands ripple backward, in a seamless loop; the first and last frames are identical.
```

**Super (o voo e o alvo, LUZ, loop): um sol pequeno, pesado, sem cauda.** Atravessa a tela (1.450
px), para no rival e bate 5 vezes pulsando. A mesma folha toca no voo e parada, então ela pulsa e
não tem cauda. Troca a folha `super_player_base`.
```
[LUZ] A miniature sun of raw, concentrated energy, much bigger and heavier than a normal energy ball, about half of the frame height, in the middle of the frame: a perfectly round white-hot core, layered bands of gold, orange and deep red-orange around it, solar flares erupting from its surface and arching back down, a shimmering corona of thin orange rays all around it, and a few small embers drifting off. No tail, no lightning, no rings, no fist, no hand. [0-5s] It throbs in five strong pulses, the core flaring brighter and the corona expanding on each pulse, while the flares loop around the surface, in a seamless loop; the first and last frames are identical.
```

## p02 Yin-lu

**S1 Palma (o voo, LUZ, loop).** Alcance curto (260 px): ela cresce e some no caminho. A forma é
de ar comprimido empurrado pela palma (crescentes), não uma mão (14/11: a primeira versão, que
pedia "uma mão de energia", saiu uma mão gigante pegando fogo).
```
[LUZ] A palm-strike blast of compressed air and sky-blue ki, seen from the side, flying to the RIGHT. It is small, in the middle of the frame, about one third of the frame height, with lots of empty black around it. Its front is a tall, thin crescent of bright white light, curved like a parenthesis bulging to the RIGHT; behind it, three thinner sky-blue crescents trail to the left, each one smaller and fainter, like ripples pushed through the air, with a few long thin white streaks like feathers trailing behind them. Light and elegant, not heavy. No hand, no fingers, no fire, no sphere. [0-5s] The crescents ripple and slide backward and fade while new ones form at the front, in a seamless loop; the first and last frames are identical.
```

**S1 Palma (o estouro no acerto, LUZ).**
```
[LUZ] A burst of sky-blue and white air pressure, small in the middle of the frame (about one third of the frame height), no hand and no fingers: [0-0.3s] a flat oval shockwave appears, [0.3-0.6s] it opens into thin curved air blades spreading RIGHT like feathers, [0.6-1.2s] it fades into soft white specks. [1.2-5s] Empty black.
```

## p03 Kōga

**S1 Onda curta (o voo, LUZ, loop).** Pequena, rápida e de alcance curto (380 px).
```
[LUZ] A small, unstable ball of gold and orange energy flying RIGHT: white-hot core wrapped in jagged, aggressive flames like a barely contained explosion, a short ragged tail to the left and sparks spitting off. Same family energy as a calm sun-orb, but angry. [0-5s] The flames crackle and change shape around the core in a seamless loop; the first and last frames are identical.
```

**S1 Onda curta (o estouro no acerto, LUZ).**
```
[LUZ] A small gold and orange energy ball exploding on impact: [0-0.2s] a white flash, [0.2-0.6s] jagged flame shards and sparks burst outward, mostly to the RIGHT, [0.6-1.2s] the flames break into embers and fade. [1.2-5s] Empty black.
```

Dos golpes novos (`golpes-refeitos.md`), quando vierem: a energia nas unhas do 2H, a energia
girando no punho do S2 e o soco de energia que fecha o Ultra. Esses ficam na arte do golpe.

## p04 Brucy

**S1 Soco de impacto (o estouro na frente do punho, LUZ).**
```
[LUZ] A short orange-gold energy burst, like the impact of a boxing punch charged with energy, small in the middle of the frame (about half of the frame height), no fist and no arm: [0-0.15s] a white flash at the center, [0.15-0.5s] a short explosion shaped like a 12-point star with a white core opens to the RIGHT, straight impact lines shoot RIGHT and a flat shockwave ring expands, [0.5-1s] it breaks apart and fades. Short and heavy. [1-5s] Empty black.
```

## p05

**S1 Kunai (OBJETO, imagem).**
```
[OBJETO] A ninja kunai flying horizontally, point to the RIGHT: dark steel blade with a bright edge and a center line, handle wrapped in dark cloth strips, a ring at the end of the handle.
```

**S1 Kunai (o rastro, LUZ, loop).** O rastro roxo atrás dela no voo.
```
[LUZ] A thin speed trail of dark purple and violet light, like an eclipse, streaking from the center of the frame to the LEFT, sharp at the front and torn into thin strands at the back, with a few violet sparks. [0-5s] The strands flicker in a seamless loop; the first and last frames are identical.
```

**S2 Shuriken (OBJETO, imagem; o jogo gira).**
```
[OBJETO] A four-pointed ninja shuriken seen exactly from the front, dark steel with bright edges and a hole in the center. Perfectly symmetrical.
```

## p06

**S1 Sombra rastejante (FUMAÇA, loop).** Rasteira, anda rente ao chão (o golpe é baixo).
```
[FUMAÇA] A wave of liquid shadow crawling along the bottom of the frame toward the RIGHT: black and dark purple ink, tall at the front and low at the back, purple shadow flames licking up from its top, a glowing violet rim at the front edge, a flat base at the bottom. [0-5s] It undulates in place in a seamless loop; the first and last frames are identical.
```

**Ultra Abismo (o raio final, LUZ).** Hoje é todo desenhado por código. São as peças do raio de
`tools/ultra/README.md` (seção "Raio final": corpo, ponta, início, impacto, aura, vento), com a
energia *"purple and black, liquid shadow like ink; the purple is bright and the black appears
only as outline and veins inside the purple"* (preto puro some no fundo preto).

O Eco de sombra (S2) e os clones do Super são cópias do sprite dela pintadas de roxo: não precisam
de vídeo.

## p07

Não tem poder: o Super chama os capangas e o resto é corpo. Se um dia ganhar, o dourado das moedas.

## p08

**S1 Chicote (o estalo da ponta, LUZ).** O chicote em si é desenhado pelo jogo, porque estica e
volta.
```
[LUZ] The crack at the tip of a whip, scarlet red: [0-0.15s] a small white flash, [0.15-0.4s] a 6-point star with a white core and curved ribbons of red light, like the path of the whip tip, snapping open to the RIGHT, [0.4-0.9s] the ribbons fade into red sparks. [0.9-5s] Empty black.
```

## p09

**S1 Machado (OBJETO, imagem; o jogo gira).**
```
[OBJETO] A firefighter's axe seen exactly from the side, whole and horizontal: red blade with a bright steel edge and a spike on the other side, wooden handle with yellow reflective bands.
```

**Super, Tempestade de Fogo (a parede de fogo correndo pelo chão, LUZ, loop).**
```
[LUZ] A wall of fire running along the bottom of the frame toward the RIGHT: tall flames at the front, lower flames behind, layered red, orange and yellow with an almost white core, embers rising, a flat base at the bottom. [0-5s] The flames dance in place in a seamless loop; the first and last frames are identical.
```

## p10

**S1 Machadinho (OBJETO, imagem; o jogo gira).**
```
[OBJETO] A firefighter's rescue hatchet seen exactly from the side, whole and horizontal: grey steel head with a bright edge, light wooden handle with red tape.
```

**S2 Jato de vapor (FUMAÇA).** Sai da mão, na diagonal para cima e para a frente.
```
[FUMAÇA] A jet of steam and water shooting from the lower left of the frame diagonally up and to the RIGHT: white and light-blue steam clouds puffing larger along the jet, water droplets and pressure lines. [0-0.3s] The jet bursts out, [0.3-1s] it is at full strength, [1-1.8s] it opens into a cloud and fades. [1.8-5s] Empty green.
```

## p11

**S1 Bisturi (OBJETO, imagem).**
```
[OBJETO] A surgical scalpel flying horizontally, blade to the RIGHT: grooved steel handle, shiny blade with a white edge and a small star glint at the tip.
```

**S2 Seringa (OBJETO, imagem).**
```
[OBJETO] A glass syringe flying horizontally, needle to the RIGHT: clear glass body full of glowing lime-green poison, metal plunger, a green drop hanging from the needle.
```

**S2 Poça de veneno (FUMAÇA, loop).** Fica 3 s no chão.
```
[FUMAÇA] A pool of poison on the ground seen from a very low side angle (a very wide, flat ellipse at the bottom of the frame): dark green and glowing lime-green liquid with a light rim, bubbles rising and popping, a thin green mist. [0-5s] Bubbles rise and pop in a seamless loop; the first and last frames are identical.
```

**Atenção:** nos dados, o Super dele hoje é um projétil sem visual (sai a bola dourada genérica),
mas a ficha fala de um Super de corpo ("Cirurgia"). Decidir antes de gerar algo para ele.

## p12 Necro

Não tem projétil: o S1 é investida de corpo. A aura de fúria (brasas vermelhas que crescem com a
vida baixa) é do código.

## p13 Krüger

A pedra do S2 já é imagem (`pedra_p13`) e o meteoro do Ultra está no vídeo do corpo. Nada a gerar.

## p14

O tornado, a lâmina de vento, a onda de chão e o tornado do Ultra estão nos vídeos do corpo. Nada
a gerar.

## p15 Natisuko

**S1 Seiken Hadō (o voo, LUZ, loop).** Curto (320 px). Hoje é a bola dourada genérica.
```
[LUZ] A short, compact blast of ki shaped like the imprint of a straight karate punch, flying RIGHT: a dense white-hot core with a red and orange fire aura around it, a few straight speed lines behind it to the left, disciplined and linear, not a round ball. [0-5s] The fire aura flickers in a seamless loop; the first and last frames are identical.
```

A cor é a da aura vermelha do S2 (a confirmar com o dono).

## p16 Bonnie

O balão (S1) e a caixa surpresa (S2) já são imagens. Falta o **canhão do Ultra** (hoje desenhado
por código).

**Ultra, o tiro do canhão de circo (LUZ).**
```
[LUZ] The blast of a circus cannon seen from the side, starting at the left of the frame and going RIGHT; only the blast, no cannon visible: [0-0.2s] a big white muzzle flash, [0.2-0.8s] layered red, orange and yellow flames with a round shockwave ring, colorful confetti and streamers bursting out, [0.8-1.8s] grey smoke puffs and the confetti flutter and fall. [1.8-5s] Empty black.
```

---

## De todos

- **Carga do Super** (hoje código, na cor de cada um). Um vídeo por personagem, na cor e no jeito
  dele:
  ```
  [LUZ] A column of <cor> energy rising from the bottom of the frame where a fighter would stand, as tall as a person: bright vertical streaks, thin rings of light tightening around the middle and small particles converging to the center, white-hot at the core. [0-5s] It flows upward in a seamless loop; the first and last frames are identical.
  ```
  O Ryūga não precisa (a carga dele vem do vídeo do Super).
- **Super pulo, defesa de poder, acerto e lança de luz:** já são folhas de vídeo.
- **Auras de mecânica** (couraça, contra-golpe, esquiva, veneno, fúria): código.
