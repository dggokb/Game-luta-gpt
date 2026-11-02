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

## 5. Ar e poeira

Vistos no trailer de lançamento (pouso dos três lutadores ~6,7 s, Cell carregando ~15,2 s,
dash do Gohan ~22,8 s, air dash do Piccolo ~56,6 s) e no da Gamescom (passos do A16 ~69 s).

**Poeira** (é a parte com mais "desenho" de todos os efeitos):
- **Nuvem de pouso:** faixa larga e baixa colada no chão, feita de bolhas em couve-flor. Cinza
  claro com sombra cinza média embaixo, **contorno fino cinza-escuro** (não preto) e linhas de
  caracol dentro das bolhas. Abre para os lados e some em ~10 quadros.
- **Poeira de passo / dash:** bolinhas pequenas com rabinho curvo (forma de vírgula) saindo dos
  pés, em dois tons, mais pedrinhas com contorno escuro. Arrastam para trás do movimento.
- **Poeira de carga / impacto forte:** nuvens grandes rolando para fora, rente ao chão, com pedras
  subindo.
- **A cor é a do chão:** cinza na cidade, bege-marrom no deserto e na arena de pedra. É
  diferente das faíscas, que são sempre luz.

**Ar** (vento e deslocamento):
- **Cortes de ar em V no pouso:** duas lâminas altas, cinza-claras e meio transparentes, subindo
  em diagonal e cruzando como um X a partir do ponto do pouso.
- **Fitas de vento no dash:** traços longos e curvos, afinando nas pontas, **da cor do chão**
  (bege), girando em volta de quem avança. Sem contorno.
- **Bolha de ar:** no golpe que chega com o dash, uma cúpula transparente com borda clara cresce
  em volta do impacto; dentro, um estouro branco com raios brancos em zigue-zague.
- **Rastro do air dash:** uma "proa" de vento branca e meio transparente na frente do corpo (como
  o ar sendo empurrado) e linhas de velocidade escuras e finas correndo para trás.
- **Estouro de ar no chão:** respingos brancos chapados com bordas em zigue-zague (como papel
  rasgado), sem contorno, saindo do chão no pouso de um golpe aéreo.
- **Espiral de aura:** arcos de vento laranja brilhantes girando em volta do corpo na carga.

**Para o jogo (vídeos de `efeitos-de-video.md`):**

| Efeito | Como fazer, pelo FighterZ |
|---|---|
| `fumaca_pouso` | Faixa baixa de bolhas em couve-flor, cinza-bege em dois tons, contorno fino cinza-escuro, linhas de caracol; mais dois cortes de ar em V subindo |
| `fumaca_pulo` | Duas ou três bolhas pequenas com rabinho, para os lados, rápidas |
| `poeira_corrida` | Bolinhas-vírgula e pedrinhas espirrando para trás, mais uma fita de vento bege curta |
| `poeira_freada` | Bolinhas arrastadas rente ao chão para a frente |
| `impacto_chao` | Nuvem rolando para os lados, estilhaços de pedra em dois tons (sem contorno) e pedras escuras |
| `super_pulo` | Cortes de ar em V altos, bolha de ar achatada no chão e nuvem baixa |

## 6. Lançar longe (Cell, ~12,6 s)

- Onda de choque em **meia-lua amarela semitransparente** seguindo quem voa.
- Pedras pretas e estrelinha branca de 4 pontas (quem sumiu ao longe).

## O que mudou no nosso `impacto_forte` (v3)

| v2 | FighterZ | v3 |
|---|---|---|
| Contorno preto grosso em tudo | Sem contorno, luz com halo | Sem contorno; halo macio em volta |
| Fumaça cinza que fica | Nenhuma fumaça no acerto | Sem fumaça |
| Centro deslocado para trás do ponto | Centrado no contato | Centrado no ponto do golpe |
| Pedaços com contorno (pareciam sementes) | Línguas de fogo de luz | Línguas sem contorno, miolo claro, ponta vermelha, dentes de chama |
| Anel de choque | Raios de luz e X no impacto | Raios de luz no quadro 0 e X no 1 |
| Poucas faíscas | Dezenas de faíscas miúdas | 46 faíscas pequenas |

A família de acerto fica: `impacto_fraco` (estrela pequena e poucas línguas), `impacto_medio`
(mais o X), `impacto_forte` (mais raios de luz e línguas grandes), `impacto_especial` (mais
agulhas vermelhas e raios elétricos).

## O que os trailers não mostram

**Defesa:** nenhuma defesa clara nos trailers. Para fazer a `defesa`, precisa de outro vídeo
(gameplay com bloqueio).
