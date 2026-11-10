# VFX do Marvel Tōkon: Fighting Souls: os tipos, para usar de base

Levantamento pedido em 10/11: **que tipos de VFX o Tōkon tem**, para servir de base aos nossos.
É referência de gênero, não arte para copiar: o que interessa é **o que cada efeito diz** e **o que
faz ele ser de um personagem só**. Os nossos são desenhados do zero para os nossos 16.

Pedido que vem junto: os efeitos que pus no jogo "funcionam, mas são modestos e iguais para
todos; o certo é termos personalizados para cada um".

## De onde vem (e o que falta)

| Fonte | O que tem |
|---|---|
| 6 capturas de tela mandadas em 10/11 | Wolverine e um grandalhão de cabelo vermelho; um chute do Doutor Destino no Ciclope. É a fonte dos **rastros de golpe** |
| 1 gravação de 6,6 s (rua de Nova York) | Ciclope de traje preto, Wolverine, Capitão América: dash, fumaça, risco de acerto, arco de fogo |
| [revisao-base/referencias/tokon.md](revisao-base/referencias/tokon.md) | Análise quadro a quadro do trailer do Ciclope Fênix, feita em outra sessão: feixe, fogo, faísca, anel, Super |
| Imprensa (links no fim) | Direção de arte e elenco; **nada** sobre VFX em detalhe |

Limites, para não ler isto como mais do que é:

- **Vi poucos personagens**: Ciclope, Wolverine, o grandalhão de cabelo vermelho, Capitão América,
  Tempestade, Magneto e Destino, dos 20 do jogo. Os outros 13 não entraram.
- **Não vi**: faísca de defesa, agarrão, quebra de guarda, e a diferença entre golpe fraco, médio
  e forte lado a lado.
- **Capturas são um quadro só**: dá para ver a forma do efeito, não quanto dura nem como some.
- Tentei baixar os trailers oficiais para ver o resto; o YouTube barrou o download e não contornei.
  Mais gravações do usuário resolvem (lista no fim).
- Dois dos três vídeos mandados nesta sessão são de Dragon Ball FighterZ, não do Tōkon (esse já
  tem a análise em [efeitos-referencia-fighterz.md](efeitos-referencia-fighterz.md)).

## A regra por trás de tudo

Antes dos tipos, as três coisas que valem para todos os efeitos de lá:

1. **Efeito é desenho 2D chapado**, em 3 ou 4 tons sem degradê, do escuro para o branco, trocando
   de desenho a cada quadro. Nada de partícula realista nem brilho borrado.
2. **Efeito é a coisa mais clara da tela.** Lutadores escuros e saturados, fundo pastel no meio,
   efeito branco e amarelo por cima.
3. **Cada energia tem a sua paleta fixa**: fogo laranja e amarelo, raio óptico rosa e magenta,
   acerto amarelo, parceiro verde, fumaça branca com sombra lavanda. Quem vê a cor sabe de quem é.

## Os tipos

### 1. Rastro do golpe (o que mais muda de um personagem para outro)

A fita que fica por onde o braço, a perna ou a arma passou. Diz **de onde o golpe veio e para onde
foi**, e é onde o Tōkon mais personaliza. Vi cinco formas diferentes:

| Forma | Quem | Como é |
|---|---|---|
| **Riscos paralelos** | Wolverine (garras) | Um risco fino por garra, brancos e cinza-claros, curvos, afinando na ponta velha. Vários juntos desenham o arco inteiro do braço, do alto até o chão |
| **Pincelada larga** | O grandalhão (soco e chute) | Uma faixa só, grossa, cinza-clara e meio transparente, com riscos dentro, como pincel seco. Atravessa meia tela: o tamanho da faixa é o peso do golpe |
| **Arco de energia** | Ciclope Fênix (golpe com fogo) | O arco na paleta do personagem, em camadas: vermelho-escuro por fora, laranja, amarelo-branco por dentro. Cerca de 70% da altura da tela |
| **Giro completo** | Wolverine (golpe em giro) | O rastro fecha um círculo em volta do corpo: riscos brancos por fora e arcos amarelos grossos por dentro, com linhas amarelas retas cruzando o centro |
| **Leque de riscos finos** | Destino (chute de cima para baixo) | Muitos riscos brancos finos em leque, do alto até o pé, mais uma linha amarela fina em arco no ponto do acerto |

O que tirar daqui:

- O rastro **segue o caminho real** do membro (é o que o nosso `linha_de_acao.py` já detecta).
- A **forma vem do que bate**: garra dá riscos finos, punho pesado dá faixa larga, energia dá arco
  colorido. Não é o mesmo desenho pintado de outra cor.
- **Afina na ponta velha** e é mais forte na ponta nova. Nunca é uma fita de largura igual.
- Além do rastro, o membro tem um **borrão desenhado** nele mesmo (o antebraço esticado vira uma
  mancha escura por um quadro).

### 2. Faísca de acerto

- **Estrela desenhada** de 6 a 8 pontas desiguais, branca no miolo, amarela no corpo, laranja na
  borda. Cobre de 26% a 30% da altura da tela: mais da metade do corpo de quem apanha.
- **Respingo**: mancha branca e amarelo-clara que estoura em cima do corpo, com gotas laranja.
- **Lascas em forma de raio**: pedaços amarelos em zigue-zague voando em volta do acerto.
- Não gira: troca de desenho a cada quadro. Dura o contato e mais uns 3 quadros da gravação.
- Nasce **no membro que bate**, não no meio do alvo.

### 3. Pontuação do golpe forte

Camada a mais que só o golpe que fecha a sequência ganha:

- **Anel fino amarelo** que abre (de 50% para 70% da altura) e some em uns 7 quadros.
- **Riscos longos e retos**, amarelos, que atravessam a tela em diagonal, afinando nas pontas.
- Estrela de 4 pontas com raios compridos por cima.
- Os dois lutadores param cerca de 7 a 8 quadros de jogo enquanto o anel abre.

### 4. Fundo de impacto

Atrás do acerto forte entra uma **mancha de quadrinho**: espinhos vermelho-escuros irregulares, com
pontos rosa, cobrindo boa parte da tela, e o cenário escurece. É o "quadro de impacto" de mangá
feito por trás dos lutadores, sem esconder nenhum dos dois.

### 5. Energia do personagem (poderes)

- **Feixe** (raio óptico): instantâneo, já cruza a tela no primeiro quadro. Miolo branco, duas ou
  três camadas da cor do personagem, fitas escuras finas por fora para dar borda, riscos de
  velocidade dentro. Some **afinando**, não piscando.
- **Fogo**: línguas pontudas em 4 tons chapados, com buracos escuros dentro para dar volume.
  Parede rasteira, coluna, bola que viaja. Deixa brasas no chão.
- **Antecipação**: um brilho em estrela no ponto de onde o poder sai (o visor), por 3 a 5 quadros
  antes do disparo.
- **Metal e cristal** (Magneto): lâminas roxas que brotam do chão.

### 6. Quem apanha muda de cor

Atingido pelo feixe fica tingido de rosa; pelo fogo, vira uma mancha laranja. É o golpe pintando o
alvo com a cor dele. Não vi piscar branco.

### 7. Fumaça e poeira

- **Nuvens grandes** de bolhas, brancas com sombra lavanda, no dash, no pulo e em toda queda.
  Ficam de 0,5 a 0,7 s: bem mais que as nossas.
- **Névoa rasteira** branca no chão depois de golpes fortes.
- **Fumaça de explosão** marrom-acinzentada, de bordas recortadas.

### 8. Chão

Riscos brancos de arrasto para trás no recuo do disparo, brasas que ficam e pulsam, estilhaços
verde-azulados saltando no golpe em giro.

### 9. Auras e estados

Contorno azul-elétrico no corpo antes do Super, emblema brilhando no peito, o personagem envolto em
fogo virando silhueta laranja chapada.

### 10. Super

Não tem tela preta. Em ordem: pinceladas da cor do personagem cobrem a tela em 1 ou 2 quadros;
**cartão de página de revista** inclinado com o nome do golpe em letra pesada (cerca de 0,8 s); o
**cenário fica cinza** e os dois lutadores continuam coloridos; o golpe toma metade da tela;
fecha com **tela cheia de cor** por 2 ou 3 quadros (branca com pinceladas de nanquim) e volta com
estrela e anel no alvo.

### 11. Parceiro e cenário

Círculo verde com anel claro marcando onde o parceiro entra. O cenário reage: táxi explode, pneu
voa.

## O que faz o efeito ser "daquele personagem"

Pelo que deu para ver, são cinco alavancas, e nenhuma delas é só trocar a cor:

| Alavanca | Exemplo de lá |
|---|---|
| **Forma do rastro** vem do que bate | garras: riscos finos; brutamontes: pincelada larga; energia: arco em camadas |
| **Paleta fixa** da energia | rosa e magenta do raio óptico; laranja e amarelo da Fênix |
| **Material** | luz, fogo, metal, ar: cada um com o seu desenho de borda |
| **Tamanho proporcional ao peso** | a faixa do grandalhão atravessa meia tela; a do Wolverine é do tamanho do braço |
| **O alvo tingido** com a cor de quem bateu | rosa no feixe, laranja no fogo |

## Onde estamos (10/11)

| Tipo | No nosso jogo hoje | Distância |
|---|---|---|
| Rastro do golpe | Uma fita azul-clara igual para todos, só pintada da cor do membro; dois riscos finos no soco reto. Segue o caminho certo | **Grande**: é o que o usuário chamou de modesto e igual |
| Faísca de acerto | Uma estrela laranja para todos (fraco, forte e Super em três tamanhos), imagem parada que estoura | Média: falta trocar de desenho por quadro e ter uma por energia |
| Pontuação do forte | Lança de luz na linha do golpe | Média: falta o anel e os riscos em diagonal |
| Fundo de impacto | Não tem no golpe comum | Decidir se entra |
| Energia | Onda e Super do Ryūga em folha de vídeo; os outros desenhados em código ou já dentro do vídeo do golpe | Grande: não há uma linguagem comum |
| Tingir quem apanha | Só o piscar vermelho do contra-golpe | Falta |
| Fumaça e poeira | Vapor branco em dois tons, poeira da corrida e da queda (imagens paradas) | Pequena na forma; duram menos e não mudam de desenho |
| Super | Cinemática própria | Fora deste levantamento |

## Proposta de base: o kit de cada personagem

Para validar antes de gerar qualquer coisa. Cada lutador teria **cinco peças próprias**; o resto
(fumaça, poeira, anel) é comum a todos.

1. **Rastro**: a forma dele (tabela abaixo), na paleta dele.
2. **Faísca de acerto**: a mesma família para todos, mas com a cor e o material dele.
3. **Energia**: os poderes (já listados no [efeitos-gpt.md](efeitos-gpt.md), Lote 3).
4. **Antecipação**: o brilho no ponto de onde o poder sai.
5. **Tinta no alvo**: a cor que quem apanha pega.

Primeira ideia da forma do rastro de cada um, a partir da ficha e da energia de cada personagem
(é palpite meu para o usuário corrigir, não decisão):

| Lutador | Forma do rastro | Paleta |
|---|---|---|
| Ryūga (p01) | Arco de energia em camadas, limpo | dourado e laranja, como o sol |
| Yin-lu (p02) | Fita fina e comprida, como seda, que ondula | azul-celeste |
| Kōga (p03) | Arco de energia rasgado, com fagulhas | dourado e laranja, explosivo |
| Brucy (p04) | Riscos curtos e retos, de soco de boxe | laranja-dourado, só no impacto |
| p05 (ninja) | Riscos finos e rápidos, quase linha | roxo de eclipse |
| p06 (sombra) | Mancha de tinta que escorre | roxo e preto |
| p07 (cavalheiro) | Pincelada larga e pesada | dourado de moeda |
| p08 (chicote) | Fita longa em curva, seguindo o chicote | vermelho-escarlate |
| p09 (bombeiro) | Arco de fogo em línguas | laranja e vermelho |
| p10 (bombeira) | Faixa de vapor que abre e some | azul-claro e branco |
| p11 (cirurgião) | Risco único e fino, de lâmina | verde de veneno |
| Necro (p12) | Riscos paralelos, um por garra | vermelho-sangue e preto |
| Krüger (p13) | Pincelada larga que atravessa a tela | a definir |
| p14 (chutes) | Leque de riscos finos nas pernas | a definir |
| Natisuko (p15) | Arco curto e seco, de karatê | a definir |
| Bonnie (p16) | Fita colorida que muda de cor no caminho | a definir |

O que já existe e serve para isso: o caminho de cada golpe (`tools/sprites/linha_de_acao.py`), a
fita dobrada no caminho (`RastroDoGolpe`, `EfeitosVideo.fita`) e a pintura por cor. Falta o motor
aceitar **uma textura e uma forma por personagem** (hoje é uma só) e o rastro com vários riscos.

## Para fechar o levantamento

Gravações que cobririam o que não vi, se o usuário puder mandar (de 5 a 10 s cada, do jogo
rodando, sem menu por cima):

1. Um golpe **defendido** (a faísca de defesa).
2. O mesmo personagem dando golpe **fraco, médio e forte** em seguida.
3. Dois ou três personagens que não entraram: de preferência um de espada, um de tiro e um de
   magia.
4. Um **agarrão** e uma **quebra de guarda**.

## Fontes

- [GamesRadar: a Marvel pediu para a Arc System Works usar o estilo próprio dela](https://www.gamesradar.com/games/fighting/marvel-tokon-fighting-souls-was-initially-more-faithful-to-the-comics-but-marvel-encouraged-the-guilty-gear-strive-and-granblue-fantasy-versus-studio-to-lean-into-its-signature-style)
- [Automaton: o visual de anime veio da Marvel Games](https://automaton-media.com/en/news/marvel-tokon-fighting-souls-anime-look-wasnt-pushed-by-arc-system-works-but-marvel-games/)
- [PlayStation Blog: entrevista](https://blog.playstation.com/2025/10/03/marvel-tokon-fighting-souls-interview/)
- [Creative Bloq: a fusão de anime e quadrinhos](https://www.creativebloq.com/3d/video-game-design/marvel-tokon-fighting-souls-fusion-of-anime-and-comic-book-visuals-blows-away-the-marvel-fighting-games-of-capcoms-past)
- [Gematsu: elenco de lançamento com 20 personagens](https://www.gematsu.com/2026/02/marvel-tokon-fighting-souls-to-launch-with-20-playable-characters)
- [Game Informer: Blade, Loki e Deadpool fecham o elenco](https://gameinformer.com/2026/06/29/marvel-tokon-fighting-souls-roster-adds-blade-loki-and-deadpool)

Os quadros dos vídeos e das capturas ficam fora do repositório (são do jogo deles).
