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

## 3. O giro de nocaute do Ryūga: FEITO (18/11), vídeo H3 com o fim caído

**O que funcionou** (o dono: "acho que conseguimos"): o pedido abaixo com a imagem inicial
`inicio_gancho_h3` (o gancho torcendo, mais à frente) e a final `fim_caido_h3` (o GROUNDED na escala
do H3, mais atrás). O vídeo está em `art/sprites/source/player_base_hit_spin_video.mp4`.
No jogo: `HIT_SPIN` (0,04-2,0 s, 24 quadros de 40 ms: o soco, o arremesso e o rolo em parafuso) no
voo do giro (`CombatFighter.spinning`: contra-golpe de punição, voadora da troca, choque de SHINKAI),
no lugar do estreitar falso; `HIT_SPIN_POUSO` (2,04-2,6 s, 7 quadros: bate de costas e assenta) na
queda, e depois o GROUNDED. A base do corpo na raiz em todos os quadros (o arco é da física do jogo).
Teste: `gradle :pc:testeCena ... -Pvida=1.0 -Pgiro=2`. **Para os outros: o mesmo pedido**, com o
gancho e o caído de cada um.

**Imagens dos outros (prontas, 18/11):** `art/keys/<id>/inicio_gancho_h3.png` e
`art/keys/<id>/fim_caido_h3.png` para p02 (Yin-lu), p03 (Kōga) e p04 (Brucy), tiradas dos sprites do
jogo: o quadro mais torcido do HIT_STAND (Yin-lu 3, Kōga 2, Brucy 4) e o caído, na escala da chave H3
de cada um (o Brucy não tem chave H3: na escala dos outros, quadro de 1344 x 768, chão em 733). O
Kōga é diferente: o GROUNDED dele fica de costas com as pernas para cima, então a imagem final usa o
quadro deitado da queda (KNOCKDOWN); o pouso do giro emenda no GROUNDED dele com um salto até ele ter
um caído esticado. Mesmo pedido de vídeo; para a Yin-lu troque he/his por she/her e a frase do fim
pela dela (tabela no começo deste arquivo).

**Antes (reprovados):** o pião do H3 inclinado aos poucos (montagem), os 12 desenhos do Codex (outro
traço) e os vídeos que terminavam em pé.

**Montado ainda não rola (o dono: "precisamos realmente fazer a animação correta").** O erro comum
dos pedidos de vídeo: todos terminavam na guarda em pé, o que obriga o gerador a manter o corpo em pé.
**O pedido abaixo termina caído de costas**, como o movimento termina de verdade:
`art/keys/p01/inicio_gancho_h3.png` (o gancho torcendo o corpo, pés no chão, do 3º vídeo) →
`art/keys/p01/fim_caido_h3.png` (o caído do jogo, GROUNDED, na escala dos vídeos, mais à esquerda).

**Giro de nocaute com fim deitado** (`inicio_gancho_h3` → `fim_caido_h3`; o vídeo de referência
do dono junto, se a ferramenta aceitar)
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
This is the classic SPINNING KNOCKDOWN of 1990s Street Fighter games: a devastating punch has just twisted his body (the start frame); he is lifted off the ground and thrown backward through the air to the LEFT, and while flying his body TILTS from diagonal to horizontal and ROLLS around its own head-to-toe axis like a corkscrew, then he crashes flat on his back exactly as in the end frame.
[0-0.3s] The start frame: his body twisted by the punch, head snapped to the side.
[0.3-1.6s] His feet leave the ground and he flies backward to the LEFT in a low arc, head first. His body leans further back with every moment until it is horizontal, and at the same time it ROLLS around its long axis one and a half turns: we see his chest, then his side, then his BACK, then his side, then his chest again. His legs stay together and stretched, his arms are loose and flung around by the roll, his hair and belt ends whip with the spin.
[1.6-2s] He crashes flat on his back on the ground, face up, head on the LEFT and feet on the right, exactly as in the end frame.
[2-5s] He lies still on his back exactly as in the end frame, breathing.
He never stands up again, never lands on his feet, never flips head over heels and never cartwheels. His whole body stays inside the frame. He never holds any object. Clean animation only: no shadow, floor, dust, smoke, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes.
```

**Para os outros (Yin-lu, Kōga, Brucy):** o mesmo caminho, com a mesma referência e os sprites de
cada um como modelo.

**Nomes do movimento** (pesquisados): no SF6 a wiki SuperCombo chama de "tailspin" e a comunidade
japonesa de きりもみダウン ("kirimomi down"); no Tekken, Screw (7) e Tornado (8).

### Tentativas reprovadas (vídeo)

#### Pião (reprovado)

**O giro que o jogo tem** (o contra-golpe de punição: "sai girando no ar e cai deitado",
`CombatFighter.spinning`) é um **pião**: o corpo roda em volta do próprio eixo, cabeça em cima e pés
embaixo, mostrando frente, lado, costas. Hoje é falso: `GameView.spinTransform` estreita e alarga
o desenho de quem apanha no ar, como uma carta virando. Este vídeo dá os quadros de verdade, e a
cena usa o mesmo giro no lançamento e na queda.

**Primeira tentativa reprovada (18/11, Ryūga):** o pedido era outro movimento (dois giros para trás,
cambalhota) e o H3 rodou de corpo aberto em estrela, de frente para a câmera, como um cata-vento,
com braços, pernas e ritmo mudando a cada quadro (e pôs poeira). Por isso este pedido diz o que
ele encara em cada quarto da volta e proíbe a cambalhota.

**Segunda tentativa (18/11):** o pião saiu certo (uma volta limpa, frente, lado, costas), mas duro:
em pé, pernas juntas, braços abertos, como bailarina. O dono: "ele gira mas não torce". Falta a
torção de quem foi arremessado. O pedido abaixo descreve a torção parte por parte (o gerador entende
melhor ombros, quadril e pernas do que a palavra "torção").

```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.4s] He holds his fighting guard facing RIGHT exactly as in the start frame.
[0.4-0.8s] A devastating invisible hook punch hits his jaw from the RIGHT: his head snaps around, his shoulders are wrenched around by the blow and his feet are torn off the ground. He flies up into the middle of the frame.
[0.8-2.8s] In the air he is SPUN like a wrung towel, the classic spinning knockdown of 1990s anime fighting games, exactly TWO full turns around his own vertical axis at a steady speed. His body is TWISTED the whole time: his head and shoulders lead the turn, his chest is turned about 90 degrees ahead of his hips, and his hips and legs lag behind, dragged around by his upper body. His body is NOT upright: it leans back diagonally about 30 degrees, his head thrown back, his spine arched. His arms are flung out loosely by the spin, one high and one low, trailing behind the turn; his legs are loose and apart, one knee bent and the other leg stretched out, trailing behind the hips. Hair and clothes whip around, trailing the turn. Each turn shows: facing right, his chest toward the camera, facing left, his back toward the camera, facing right again. Never a stiff, upright, ballet-like pirouette with the legs together.
[2.8-3.6s] The spin ends facing RIGHT; he drops and lands on his feet in a staggering crouch.
[3.6-5s] He rises into his fighting guard and holds it exactly as in the end frame, breathing heavily.
He does NOT flip head over heels and does NOT cartwheel: his head stays on top. His whole body stays inside the frame. He never holds any object. Clean animation only: no shadow, floor, dust, smoke, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes.
```

**Terceira tentativa (18/11):** de novo em pé e quase sem torção. O motivo: o H3 copia o corpo da
imagem inicial, e ela era a guarda em pé; o texto não vence isso. **A saída é mudar a imagem
inicial:** `art/keys/p01/inicio_torcido_h3.png` é o quadro de 1,75 s desse vídeo (o soco acabou de
torcer o corpo: ombros virados, cabeça jogada, braços soltos), posto no meio da tela e no ar. Com ela
como imagem inicial E final, o pedido vira uma estátua num prato giratório: a pose fica congelada e
só gira, coisa que o gerador faz bem, e a volta fecha em loop.

**Pião torcido** (`inicio_torcido_h3` → `inicio_torcido_h3`)
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-5s] He is frozen in mid-air in EXACTLY the twisted pose of the start frame, like a statue: his shoulders wrenched around by a punch, his head snapped to the side, one arm flung up and the other tucked, his legs apart and bent. He keeps this exact pose the whole time and never moves his arms, legs or head.
The whole frozen figure ROTATES around its own vertical axis like a statue on a turntable, at a perfectly steady speed: exactly TWO full 360-degree turns in 5 seconds, showing him from the front, the side, the back and the other side. He stays in the same spot in the middle of the frame, floating; he never lands. The video ends exactly on the start frame, so it loops seamlessly.
He does NOT flip head over heels and does NOT cartwheel: his head stays on top. Only his hair and the ends of his belt flutter slightly with the spin. His whole body stays inside the frame. He never holds any object. Clean animation only: no shadow, floor, dust, smoke, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes.
```

O jogo usa uma volta (2,5 s, cortada em 8 quadros iguais), repetida enquanto ele voa. Para os
outros lutadores: tirar a imagem inicial do mesmo jeito, do quadro em que o gancho torce o corpo
no vídeo deles (ou do primeiro pedido do pião torcido acima).

**Enquanto isso, o vídeo da segunda tentativa já serve:** uma volta dele (1,0-2,0 s, 8 quadros) é
um giro de verdade, muito melhor que o estreitar falso. Instalado, o jogo inclina o corpo
(`SPIN_TILT`) e dá um pouco do arremesso; quando vier o vídeo com torção, troca os quadros.

O jogo usa: o tirado do chão (0,4-0,8 s) e **uma volta** do pião (8 quadros iguais, de frente
para a direita até voltar a ela), repetida enquanto ele voa. Na luta, no lugar do estreitar falso;
na cena, no lançamento para cima e na queda no fundo preto.

**Se o pião também não sair:** a reserva é a pose encolhida parada no ar (joelhos no peito,
braços abraçando as canelas, queixo nos joelhos; "the body does NOT rotate"), que o jogo gira como
uma bola. Vídeo segura pose parada bem melhor do que gira.

**Do vídeo reprovado dá para aproveitar** o lançado arqueando (0,3-0,8 s) e o pouso agachado
(2,7-3,3 s).
