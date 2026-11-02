# Efeitos do Dragon Ball FighterZ: análise quadro a quadro (referência de gênero)

Para guiar os efeitos **originais** do jogo. Não é para copiar arte: é para entender o que faz o
efeito funcionar.

**De onde vem:** os dois trailers oficiais da página do jogo na Steam (Gamescom e lançamento,
1920×1080 a 30 fps) e as 9 capturas oficiais da mesma página, analisados quadro a quadro. Os
quadros ficam fora do repositório (são do jogo deles). Números de quadro abaixo são a 30 fps (o
jogo roda a 60: cada quadro do trailer são dois do jogo).

## 1. Acerto normal (Piccolo acerta o Kuririn, trailer da Gamescom, ~27,6 s)

O efeito inteiro dura **uns 11 quadros (~0,37 s)**. Fica **em cima de quem apanha, centrado no
ponto do contato** (o punho na cabeça).

| Quadro | O que aparece |
|---|---|
| 13 (antes) | Linhas de velocidade brancas, finas, ao longo do braço que estica. Poeira na base de quem bate |
| 14 (contato) | **A câmera dá um soco para dentro (zoom).** Estrela de espinhos enorme: miolo branco, amarelo, borda laranja-avermelhada. **3 ou 4 raios de luz** finos e longos atravessam a tela saindo do ponto. Dezenas de **faíscas amarelas miúdas** espalhadas numa área grande |
| 15 | A estrela vira um **X**: duas formas de espinho compridas e cruzadas, miolo claro e borda vermelha. Os raios continuam |
| 16-17 | Volta a estrela redonda, mais densa, com **brilho difuso** em volta (halo claro e macio) e **pontinhos escuros** dentro (sujeira, pele) |
| 18 | O miolo **abre**: fica vermelho-escuro com pontinhos pretos, e a borda se divide em **línguas de fogo** gordas apontando para fora (como pétalas) |
| 19-20 | As línguas se soltam e voam para fora em todas as direções; o centro fica vazio |
| 21-23 | Sobram 3 ou 4 línguas pequenas, curvas (forma de vírgula), indo para fora e encolhendo |
| 25 | Acabou; só faíscas soltas |

**O que mais importa:**
- **Sem contorno preto.** O efeito é luz: branco, amarelo, laranja e vermelho, com halo macio. O
  contorno fica para os personagens. É o contraste que faz o efeito "brilhar" sobre eles.
- **Sem fumaça.** No acerto não tem nenhuma.
- **Fogo, sim.** As línguas de fogo laranja são o efeito de acerto padrão do jogo, mas como
  **luz sem contorno**, saindo do ponto do golpe, não como explosão com fumaça.
- **O impacto é o instante:** câmera, raios de luz e X nos 2 primeiros quadros; depois o efeito
  só se desfaz.
- **Faíscas miúdas** (pontos e traços amarelos) numa área bem maior que a estrela.

## 2. Acerto forte / sequência de Super (A16 em A18, ~48,9 s)

Mesma base do acerto normal, com mais coisas:
- **Agulhas vermelhas:** espinhos finos e longos, vermelho-vivo, saindo do ponto em leque.
- **Raios elétricos** amarelo-esbranquiçados em zigue-zague.
- Estrelas grandes com X a cada golpe da sequência e línguas de fogo maiores.
- Pedras cinza voando quando o golpe pega o chão.

## 3. Projétil / raio acertando (A16 em A18, ~57,3 s)

- O raio é uma faixa branca-amarela com borda laranja.
- No impacto: estrela com **agulhas vermelhas cruzadas** muito longas, depois 2 ou 3 línguas de
  fogo pequenas que caem.

## 4. Impacto no chão (A16 bate no chão, ~51,6 s)

- **Estilhaços cinza** grandes e angulosos, saindo em leque do ponto: cinza-claro com sombra
  cinza-escura, **sem contorno**, como pedra desenhada em dois tons.
- Pedras escuras voando.
- **Brasas laranja** brilhando no chão, espalhadas.
- Clarão amarelo-vermelho com raios elétricos por cima, no instante do golpe.

## 5. Poeira

- Na base de quem dá um golpe forte (Piccolo, quadro 13) e em quem cai (A16, ~57,3 s): **nuvens
  redondas em dois tons** (base e sombra embaixo), **sem contorno**, da **cor do chão** (marrom no
  deserto, cinza na cidade), que se espalham e somem em uns 10 quadros.

## 6. Lançar longe (Cell, ~12,6 s)

- Onda de choque em **meia-lua amarela semitransparente** seguindo quem voa.
- Pedras pretas e estrelinha branca de 4 pontas (quem sumiu ao longe).

## O que isso muda no nosso `impacto_forte`

A v2 acertou as cores, mas errou no resto:

| v2 (o que fiz) | FighterZ | v3 (o que fazer) |
|---|---|---|
| Contorno preto grosso em tudo | Sem contorno, luz com halo | Tirar todo o contorno; halo macio em volta |
| Fumaça cinza que fica | Nenhuma fumaça no acerto | Tirar a fumaça (poeira só no chão) |
| Centro deslocado para trás do ponto | Centrado no contato, em cima de quem apanha | Centrar no ponto do golpe |
| Pedaços com contorno (pareciam sementes) | Línguas de fogo de luz | Línguas sem contorno, miolo claro e ponta vermelha |
| Anel de choque | Raios de luz atravessando e o X | Raios de luz no 1º quadro e o X no 2º |
| Poucas faíscas | Dezenas de faíscas miúdas numa área grande | Muitas faíscas pequenas |

A família de acerto fica: `impacto_fraco` (estrela pequena + poucas línguas), `impacto_medio`
(+ X), `impacto_forte` (+ raios de luz e mais línguas), `impacto_especial` (+ agulhas vermelhas e
raios elétricos). A poeira e o impacto no chão são outra família: cinza ou da cor do chão, em dois
tons, sem contorno.

## O que os trailers não mostram

**Defesa:** nenhuma defesa clara nos trailers. Para fazer a `defesa`, precisa de outro vídeo
(gameplay com bloqueio).
