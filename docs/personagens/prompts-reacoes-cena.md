# Vídeos de reação: parede, quique e rodopio no ar

Pedidos de 18/11, depois da cena "Já vai desistir?" (torneio). Servem à cena e à luta: a parede
para os golpes que jogam na parede, o quique para os que cravam no chão (↓ + H no ar) e o rodopio
para o lançamento e o contra-golpe de punição. Hoje as três usam a pose de apanhar no ar e a de
queda giradas, e de ponta-cabeça a pose fica estranha.

Para os quatro da dupla do jogador: Ryūga (p01), Yin-lu (p02), Kōga (p03) e Brucy (p04). O
adversário não precisa: na cena ele só bate.

**Configuração:** a mesma dos outros vídeos ([video.md](video.md)): MiniMax H3 Fast, 5 s, 768p, a
imagem inicial e a final `art/keys/<id>/inicio_centro_h3.png` (o Brucy ainda não tem a `_h3`: use
a `inicio_centro.png` dele).

**Troque a última frase de cada prompt** pela do lutador (e "he/his" por "she/her" na Yin-lu):

| Lutador | Frase |
|---|---|
| Ryūga (p01) | Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. |
| Yin-lu (p02) | Keep her face, silver-lavender hair, headband, top, orange sash, gloves, pants, sneakers, colors, line art and size exactly as in the frames; her size never changes. |
| Kōga (p03) | Keep his face, spiky hair, hooded vest, red straps, tank top, gloves, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. |
| Brucy (p04) | Keep his face, silver hair, tank top, bandages, gloves, red sash, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. |

O jogo corta cada vídeo nos pedaços que usa (marcados nos tempos), como nos outros.

## 1. Bater na parede e escorregar (`WALL_SPLAT`)

```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. He faces RIGHT the whole time; his chest stays turned toward the camera and he NEVER turns his back to the camera. The camera stays fixed and never zooms.
[0-0.4s] He holds his fighting guard exactly as in the start frame.
[0.4-0.9s] A powerful invisible hit from the RIGHT launches him backward toward the LEFT: his feet leave the ground, his body flies back almost horizontally, arms and legs thrown forward, with a pained shout.
[0.9-1.3s] His BACK slams flat against an invisible vertical wall near the LEFT edge of the frame: his body flattens against it, arms spread wide against the wall, head snapping back, eyes squeezed shut, for a short frozen moment, his feet off the ground.
[1.3-2.2s] He slides slowly down the invisible wall, still with his back against it, until he is sitting on the ground against it with his legs stretched forward, head hanging, exhausted.
[2.2-3.6s] He pushes himself up with one hand, gets back on his feet and walks back to the center of the frame.
[3.6-5s] He holds his fighting guard exactly as in the end frame, in the same place where he started, breathing heavily.
The wall is invisible: never draw a wall, cracks or debris. His whole body stays inside the frame. He never holds any object. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes.
```

O jogo usa: o voo (0,4-0,9 s), a batida na parede (0,9-1,3 s) e o escorregar até sentar
(1,3-2,2 s). O levantar já existe.

## 2. Quicar no chão (`GROUND_BOUNCE`)

```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. He faces RIGHT the whole time; the camera stays fixed and never zooms.
[0-0.4s] He holds his fighting guard exactly as in the start frame.
[0.4-0.8s] A crushing invisible blow from ABOVE smashes him down: he is knocked off his feet and slams flat on his BACK on the ground, face up, very hard.
[0.8-1.4s] The impact makes his whole body BOUNCE up off the ground, still lying horizontally on his back, about half his height into the air: arms and legs flung loose upward, head thrown back, mouth open in a pained shout.
[1.4-1.8s] He falls back down and lands flat on his back again, with a smaller second bounce.
[1.8-2.8s] He lies still on his back, face up, breathing hard.
[2.8-4s] He rolls to one side, pushes himself up with one hand and stands up in his fighting guard in the same place where he started.
[4-5s] He holds the guard exactly as in the end frame, breathing heavily.
He always lies face UP, on his back, never face down. His whole body stays inside the frame. He never holds any object. Clean animation only: no shadow, floor, dust, cracks, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes.
```

O jogo usa: a batida de costas (0,4-0,8 s), o quique (0,8-1,4 s) e a queda de volta
(1,4-1,8 s). O caído e o levantar já existem.

## 3. Rodopiar no ar (`HIT_SPIN`): a pose encolhida, o jogo gira

**Primeira tentativa reprovada (18/11, Ryūga):** pedindo "dois giros para trás", o H3 girou de
corpo aberto em estrela, de frente para a câmera, como um cata-vento; braços e pernas mudam a cada
quadro e o ritmo do giro muda, então não sai uma volta limpa que repita (e pôs poeira no pulo e
na queda). Vídeo não gira bem. O jeito dos jogos 2D: **uma pose encolhida, parada no ar, e o jogo
gira** (o motor já gira a pose: `rot` nas chaves da cena). Encolhido como uma bola, ele fica
natural em qualquer ângulo, ao contrário da pose de queda girada.

```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. He faces RIGHT the whole time; his chest stays turned toward the camera and he NEVER turns his back to the camera. The camera stays fixed and never zooms.
[0-0.4s] He holds his fighting guard exactly as in the start frame.
[0.4-0.8s] A powerful invisible uppercut from below lifts him off the ground: his body arches back for an instant, with a pained shout.
[0.8-1.2s] Still rising in the middle of the frame, he curls up into a tight ball: knees pulled up hard against his chest, both arms wrapped around his shins, chin tucked down to his knees, eyes squeezed shut in pain.
[1.2-2.6s] He HOLDS that tight curled ball pose, floating still in the middle of the frame, completely frozen: his body does NOT rotate, does NOT spin and does NOT move; only his hair and the ends of his belt flutter slightly.
[2.6-3.4s] He uncurls, drops straight down and lands on his feet in a crouch.
[3.4-5s] He rises into his fighting guard and holds it exactly as in the end frame, breathing heavily.
The body never spins or rotates at any moment; it stays upright, facing right. His whole body stays inside the frame. He never holds any object. Clean animation only: no shadow, floor, dust, smoke, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes.
```

O jogo usa: o lançado arqueando (0,4-0,8 s) e **um quadro** da bola encolhida (1,2-2,6 s), que ele
gira enquanto o lutador voa (no lançamento para cima e na queda no fundo preto da cena; na luta,
no contra-golpe de punição).

**Do vídeo reprovado dá para aproveitar** o lançado arqueando (0,3-0,8 s) e o pouso agachado
(2,7-3,3 s); o giro, não.
