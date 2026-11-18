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

## 3. O giro de nocaute (`HIT_SPIN` + `HIT_SPIN_POUSO`): a receita que funcionou

| Lutador | Giro | Vídeo do giro | Imagens (`art/keys/<pacote>/`) |
|---|---|---|---|
| Ryūga (`player_base`) | FEITO 18/11 | `player_base_hit_spin_video.mp4` | `p01/inicio_gancho_h3.png` → `p01/fim_caido_h3.png` |
| Yin-lu (`p02`) | FEITO 18/11 (e a queda nova) | `p02_hit_spin_video.mp4` | `inicio_giro_h3.png` → `fim_caido_h3.png` |
| Natisuko (`p15`) | FEITO 18/11 | `p15_hit_spin_video.mp4` | `inicio_giro_h3.png` → `fim_caido_h3.png` |
| Bonnie (`p16`) | FEITO 18/11 (2º vídeo; cortados os quadros 39-47, em que a cabeça girava errado) | `p16_hit_spin_video.mp4` | `inicio_giro_h3.png` → `fim_caido_h3.png` |
| Kōga (`p03`), Brucy (`p04`) e os outros | falta | | |

(Os vídeos ficam em `art/sprites/source/`.)

**Onde o jogo usa** (sozinho, para quem tiver os clipes; quem não tem fica no giro antigo, o corpo
estreitando): o voo do giro (`CombatFighter.spinning`: contra-golpe de punição, voadora da troca,
quem perde o choque de SHINKAI) toca `HIT_SPIN` e segura o último quadro até o chão; ao cair toca
`HIT_SPIN_POUSO` (bate de costas e assenta) e passa ao `GROUNDED` e ao `GETUP` de sempre
(`GameView.reactionState`). Na cena "Já vai desistir?" o voo para a parede e a queda no fundo preto
usam os mesmos clipes, com a pose antiga de reserva (`senao`) para quem não tem.

**O segredo:** o vídeo tem de **terminar deitado de costas** (o caído do jogo como imagem final).
Todos os pedidos que terminavam em pé saíram com o corpo em pé (pião duro, sem rolo); com o fim
deitado o H3 é obrigado a deitar o corpo no ar e sai o rolo em parafuso de verdade.

**Passo a passo, para um lutador novo:**

1. **A queda dele tem de ser da arte nova** (o caído de costas, cabeça para a esquerda). Se for da
   arte antiga, primeiro o vídeo H3 "Derrubado: cair, caído e levantar" (o pedido está na ficha
   `docs/personagens/<pacote>.md`, imagem inicial e final = `inicio_centro_h3`); ele troca a queda
   antiga (exemplo: `tools/sprites/p02_derrubado_h3.py`, a Yin-lu) e dá o caído de costas.
2. **As duas imagens** (1344x768, o fundo verde da chave):
   - inicial `art/keys/<pacote>/inicio_giro_h3.png`: a guarda `inicio_centro_h3` (ou `inicio_centro`
     se ela já for 1344x768) 300 px para a direita;
   - final `art/keys/<pacote>/fim_caido_h3.png`: o caído de costas na escala do vídeo, encostado na
     borda esquerda (x ≈ 30), a base no chão da guarda (y ≈ 733). Do vídeo "Derrubado" (um quadro
     já deitado e parado) ou do último quadro do GROUNDED da folha HD (`<pacote>_fall_down_video_hd.png`)
     aumentado ÷ 0,822 (a folha é 0,822 do vídeo).
3. **O vídeo:** MiniMax H3 Fast, 5 s, 768p, as duas imagens como início e fim, o pedido abaixo.
4. **No jogo:** olhe a folha de contato do vídeo (de quadro em quadro, recortada no corpo: um erro de poucos quadros, como a cabeça da Bonnie dando meia-volta sozinha, some cortando esses quadros do voo) e anote o quadro em que o soco já torceu (pule o
   clarão), o último do voo antes de bater de costas e o da batida. O último quadro do voo o jogo segura até o chão: tem de ser ela deitada no ar, nunca de pernas para cima (isso vai para o pouso). A ferramenta alinha o voo pelo centro do corpo, descendo em linha reta da guarda (pés na raiz) ao deitado (base na raiz); alinhar pela base fazia o corpo subir e descer no pico, porque a ponta de uma faixa ou de um braço caído virava a base (o dono, 18/11, na Bonnie). Ponha o pacote em `PACOTES` de
   `tools/sprites/giro_h3.py` (escala = altura do IDLE da folha ÷ altura da guarda no vídeo, ≈ 0,82;
   voo = 24 quadros entre o começo e o fim do voo; pouso = 7 quadros de 2 em 2 a partir da batida) e
   rode `python3 tools/sprites/giro_h3.py <pacote>`. Crie os dois clipes
   `tools/sprites/clips/<pacote>_hit_spin.json` e `_hit_spin_pouso.json` (copie os da `p15`), ponha
   `HIT_SPIN` (24 quadros de 40 ms) e `HIT_SPIN_POUSO` (7 de 50 ms) no `characters/<pacote>/character.json`
   e rode `python3 tools/sprites/build_characters.py --write` e
   `python3 tools/sprites/normal_map.py <pacote>_hit_spin <pacote>_hit_spin_pouso`.
5. **Conferir:** `gradle :pc:testeCena -Pepisodio=ep04 -Pmarca=torneio-1 -Pcontra=<pacote> -Pgiro=1
   -PgiroAlvo=rival -Pcadencia=2 -Pjpg=true -Psaida=<pasta>` (o rival leva o giro uma vez, gravando;
   `-Pqueda` faz uma queda comum, para conferir KNOCKDOWN/GROUNDED/GETUP). Para o Ryūga ou o Kōga,
   sem `-Pcontra` e sem `-PgiroAlvo`.

**O pedido** (ela; para ele troque she/her por he/his). Troque três coisas: o lutador na 1ª frase
("kung fu fighter", "karate fighter", "clown martial artist", "martial artist"), o que esvoaça no
rolo (`<whip>`) e a última frase pelo "Keep ..." da ficha dele:

```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the <fighter> from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
This is the classic SPINNING KNOCKDOWN of 1990s Street Fighter games: a devastating invisible punch twists her body; she is lifted off the ground and thrown backward through the air to the LEFT, and while flying her body TILTS from diagonal to horizontal and ROLLS around its own head-to-toe axis like a corkscrew, then she crashes flat on her back exactly as in the end frame.
[0-0.3s] A devastating invisible punch hits her face: her body twists hard to the side, her head snaps back, her hair whips.
[0.3-1.6s] Her feet leave the ground and she flies backward to the LEFT in a low arc, head first. Her body leans further back with every moment until it is horizontal, and at the same time it ROLLS around its long axis one and a half turns: we see her chest, then her side, then her BACK, then her side, then her chest again. Her legs stay together and stretched, her arms are loose and flung around by the roll, <whip> with the spin.
[1.6-2s] She crashes flat on her back on the ground, face up, head on the LEFT and feet on the right, exactly as in the end frame.
[2-5s] She lies still on her back exactly as in the end frame, breathing.
She never stands up again, never lands on her feet, never flips head over heels and never cartwheels. Her whole body stays inside the frame. She never holds any object. Clean animation only: no shadow, floor, dust, smoke, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
<Keep ... da ficha>
```

O que já foi usado em `<whip>`: Yin-lu "her hair and the ends of her orange sash whip"; Natisuko "her
long ponytail and the ends of her black belt whip"; Bonnie "her red ponytail, its bow and her waist
bows whip". O do Ryūga foi a variação abaixo, com a imagem inicial já torcida pelo gancho (o 1º
trecho diz "The start frame: his body twisted by the punch, head snapped to the side.").

**A queda nova da Yin-lu (18/11):** o vídeo H3 "Derrubado" (`p02_derrubado_h3_video.mp4`) virou
KNOCKDOWN (quadros 10-28: voa e bate de costas; o quique é o do jogo, então o do vídeo fica de fora),
GROUNDED (31-45) e GETUP (81-99) por `tools/sprites/p02_derrubado_h3.py` (escala 391/469; a raiz
fixa nos pés do 1º quadro, o deslocamento do vídeo fica no desenho). Ela substituiu a do `derrubaLevanta.mp4` (feita na mesma tarde em outra sessão) para o fim do giro bater com o caído.

**Kōga e Brucy:** as reações deles são da arte antiga, então começam pelo passo 1. O Brucy ainda não
tem as chaves H3 (`inicio_centro_h3`); o caído do Kōga tem as pernas para cima (o "Derrubado" também
corrige isso).

### Histórico do giro (o que não funcionou)

**Antes (reprovados):** o pião do H3 inclinado aos poucos (montagem), os 12 desenhos do Codex (outro
traço) e os vídeos que terminavam em pé.

**O pedido do Ryūga** (`inicio_gancho_h3` → `fim_caido_h3`; o gancho torcendo o corpo, pés no chão,
tirado do 3º vídeo, 300 px à frente; o GROUNDED na escala do vídeo, mais à esquerda):
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
