# Cenários com falso 3D: como criar um

O cenário é montado em **camadas**, cada uma numa profundidade. O jogo projeta todas com a
câmera da luta: o que está longe anda e cresce menos, o piso fica em perspectiva e luzes,
fogo, água, neblina e pétalas são animados pelo próprio jogo. Como funciona por dentro:
`docs/stage-engine.md`.

Cada cenário fica em `android/app/src/main/assets/stages/<id>/`, com um `stage.json` e as
imagens. O primeiro é o **Templo da Lua** (`templo_lua`), baseado no concept do templo à
noite, com a arte gerada no GPT. `gerar_provisorio.py` gera uma arte provisória no mesmo
formato (em `tools/stage/provisorio/`), útil para testar ou como modelo.

## Passo a passo

1. Gere as imagens no GPT com os prompts abaixo. Use os mesmos nomes de arquivo.
2. Prepare:
   ```bash
   pip install pillow
   python tools/stage/preparar_cenario.py --entrada pasta/com/as/imagens --id templo_lua
   ```
   O script corta as margens transparentes (importante: senão a camada fica flutuando
   acima do piso), converte céu e piso para JPG e reduz o que passar de 2048 px.
3. Veja no PC e ajuste o `stage.json`:
   ```bash
   cd android
   gradle :pc-preview:runStage --args="app/src/main/assets/stages/templo_lua"
   ```
   A câmera simula uma luta: os lutadores se afastam, se aproximam, andam pelo cenário e
   dão um super pulo. Com `--quadros saida/` o visualizador grava os quadros em PNG.
4. **Ajuste as luzes e os fogos**: as posições (`x`, `y` em fração da imagem) dependem de
   onde a IA desenhou a lanterna ou a tigela do braseiro. Depois de trocar `pilar.png` ou
   `braseiro.png`, confira no visualizador. Se preferir, me mande as imagens que eu ajusto.

## Prompts do Templo da Lua

Regra geral, vale para todos: **sem personagens, sem texto, sem marca d'água**. Use o mesmo
estilo em todas as imagens. Cole antes de cada prompt:

```
Arte de cenário para jogo de luta 2D em estilo anime, noite, iluminação dramática com lua
azulada e luzes quentes de lanternas, cores ricas, traço limpo. Sem personagens, sem texto,
sem marca d'água.
```

| Arquivo | Formato | Fundo | Prompt |
| --- | --- | --- | --- |
| `ceu.jpg` | horizontal | opaco | Céu noturno com uma lua cheia grande e brilhante no terço superior, um pouco à direita, halo azulado, nuvens escuras roxas, muitas estrelas, degradê de azul-marinho em cima para roxo embaixo. Só o céu: sem prédios, sem montanhas, sem chão. |
| `montanhas.png` | horizontal | **transparente** | Silhueta de uma cadeia de montanhas distantes à noite, azul-arroxeado com névoa leve no pé, ocupando só a metade de baixo da imagem. As bordas esquerda e direita continuam no mesmo nível, para a imagem poder se repetir. |
| `cidade.png` | horizontal | **transparente** | Vista distante de uma cidade japonesa futurista à noite, à beira de um rio. À direita, arranha-céus com janelas acesas e letreiros neon rosa e ciano. À esquerda, um morro escuro com um pagode iluminado e cerejeiras floridas. A base é uma linha reta na parte de baixo (a margem do rio). Sem céu: tudo acima dos prédios é transparente. |
| `portal.png` | horizontal | **transparente** | Um torii vermelho no centro, sobre uma ponte baixa de madeira, cercado de cerejeiras floridas rosa e lanternas de pedra, à noite, vista frontal. A base é uma linha reta embaixo. Fundo transparente. |
| `grade.png` | horizontal | **transparente** | Balaustrada de templo japonês vista de frente, em linha reta: corrimão de madeira laqueada vermelho-escuro, postes com remates dourados, dois degraus de pedra embaixo. Continua igual nas duas bordas, para se repetir lado a lado. Fundo transparente. |
| `pilar.png` | **vertical** | **transparente** | Vista frontal de um pilar de madeira laqueada vermelha alto, à esquerda da imagem, com um pedaço de viga de telhado escura no topo. Ao lado do pilar, pendurado na viga, um estandarte vermelho-escuro comprido com um único ideograma dourado pintado a pincel. À direita, uma lanterna de papel acesa pendurada. O pilar toca a borda de baixo da imagem. Fundo transparente. |
| `braseiro.png` | quadrado | **transparente** | Braseiro de bronze dourado sobre um pedestal, vista frontal, com a tigela rasa no topo **vazia e sem fogo** (o jogo desenha o fogo). Fundo transparente. |
| `chao.jpg` | horizontal | opaco | Piso de pátio de templo visto **exatamente de cima** (vista ortográfica, sem perspectiva): lajotas de pedra escura molhada com reflexos sutis, e no centro um grande emblema circular dourado em espiral incrustado no chão. Iluminação uniforme, sem sombras de objetos. As bordas esquerda e direita continuam o padrão das lajotas. |

Dicas:
- **Fundo transparente**: na API, `background: "transparent"`. No ChatGPT, peça "PNG com
  fundo transparente". Se vier com fundo, gere de novo pedindo de novo a transparência.
- O piso precisa ser **visto de cima**. É o jogo que coloca em perspectiva; um piso já em
  perspectiva fica errado.
- O fogo dos braseiros, o brilho das lanternas, o rio, a neblina e as pétalas são feitos
  pelo jogo. Não precisa (e é melhor não) pôr na arte.

## stage.json

```json
{
  "nome": "Templo da Lua",
  "camera": { "distancia": 1000, "horizonte": 0.40 },
  "corFundo": "#070A1C",
  "camadas": [ ... ],
  "chao": { "imagem": "chao.jpg", "perto": -420, "longe": 560, "centroX": 700,
            "reflexoLutadores": 0.22, "reflexoLuzes": 0.45 },
  "petalas": { "quantidade": 70, "cor": "#FFB7D5", "perto": -350, "longe": 2600,
               "vento": 45, "queda": 65, "tamanho": 7 }
}
```

- **`camera`**: `distancia` controla a força do efeito 3D (menor = mais exagerado);
  `horizonte` é a altura do horizonte na tela (0 = topo, 1 = base).
- **Profundidade** (`profundidade`): unidades do mundo atrás dos lutadores. Como
  referência no Templo da Lua: braseiros 120, pilares 300, grade 560 (fim do piso),
  portal 1450, cidade 4200, montanhas 9000, céu 100000. Negativo fica na frente dos
  lutadores.
- **Posição na tela** (na câmera de repouso, em frações): `topo` e `base`; ou `noChao: true`
  com `altura` (a base apoia no chão daquela profundidade); `x` é o centro da primeira
  cópia.
- **Repetição**: `repetir: "repetir"` ou `"espelhar"` (cópias alternadas espelhadas,
  esconde emendas). `periodo` é a distância entre cópias em fração da largura da tela.
- **Extras de camada**: `reflexo` (cópia espelhada embaixo, para a água), `corAcima` (pinta a
  tela acima da camada, útil no céu durante o super pulo), `alfa`.
- **`luzes`**: `x`, `y` (fração da imagem), `raio` (fração da altura da imagem), `cor`,
  `pulsar` e `reflexo` (aparece no piso molhado).
- **`fogos`**: `x`, `y` (a base da chama, em fração da imagem) e `tamanho` (fração da altura
  da imagem).
- **Neblina**: `{"tipo": "neblina", "profundidade", "topo", "base", "cor", "alfa"}`.
- **Água**: `{"tipo": "agua", "perto", "longe", "corPerto", "corLonge", "alfa", "brilhos"}`.
- **Piso**: textura vista de cima. A borda de cima é a profundidade `longe` e a de baixo é
  `perto`. `centroX` é o x do mundo do centro da imagem (os lutadores começam em 420 e 980).
  `reflexoLutadores` é a opacidade do reflexo dos lutadores no chão (0 = sem reflexo).
  `imagemLajotas` (opcional) é a mesma textura **sem o emblema**, usada nas repetições fora
  do centro, para o emblema não aparecer duas vezes quando a câmera anda.

## Notas do Templo da Lua

- O `portal.png` veio com piso em perspectiva e pavilhões nas bordas. Ficou só o torii
  com as cerejeiras (x 300 a 1370 da imagem original), sem o piso e com as bordas
  esfumaçadas, para a cidade aparecer dos lados.
- O `chao_lajotas.jpg` veio do GPT (o prompt do piso **sem o emblema**). As lajotas dele
  eram maiores que as do `chao.jpg`, então ele foi recortado nas juntas e reduzido para o
  mesmo tamanho de lajota (3 lajotas ≈ 418 px), montado em 3×3 com recortes variados para
  não parecer repetido. Ao trocar o piso, mantenha as lajotas das duas imagens do mesmo
  tamanho.
- A cidade já vem com o rio e os reflexos pintados, então a camada `rio` só acrescenta os
  brilhos que piscam (`alfa` 0).

## Prompts do Dojo Shūshinkan (修心館)

Segundo cenário (`dojo_shinshinkan`), baseado nas duas páginas de mangá do dojo 修心館. A
luta é **dentro do salão de treino**, um salão fechado, como na página 2: piso de madeira
encerado que espelha as luzes, paredes de shoji acesas em dois andares com uma galeria no
alto, pilares grossos de madeira natural, lanternas de poste e lanternas redondas com o
brasão do dojo e, no centro da parede do fundo, o grande pergaminho com 修心館. Da página 1
vêm o poste de treino (makiwara) e as pétalas de cerejeira entrando no salão.

Como o salão é fechado, o cenário não tem céu, cidade nem montanhas. A profundidade vem de
outro jeito: parede do fundo longe, uma fileira de lanternas de poste no meio, pilares e
teto perto e vultos de madeira desfocados **na frente** dos lutadores (os cantos escuros da
página 2).

Regra geral: cole antes de cada prompt. No ChatGPT, anexe as duas páginas e diga "use as
imagens anexadas só como referência de estilo, clima e arquitetura".

```
Arte de cenário para jogo de luta 2D em estilo anime, noite. Interior de um dojo
tradicional japonês de madeira escura envernizada, iluminado por luz quente âmbar e dourada
de lanternas de papel e de paredes de shoji acesas, brilho intenso e atmosfera de treino.
Cores ricas, traço limpo e detalhado. Sem personagens, sem silhuetas de pessoas atrás do
papel, sem texto, sem marca d'água.
```

O **brasão do dojo** (mon) aparece nas lanternas e no piso. Use sempre a mesma descrição:
"o mesmo brasão das lanternas da referência: um círculo duplo com uma flor estilizada de
pétalas no centro".

| Arquivo | Formato | Fundo | Prompt |
| --- | --- | --- | --- |
| `parede.jpg` | horizontal | opaco | Parede do fundo do salão de um dojo, vista exatamente de frente, em linha reta, ocupando a imagem toda: dois andares de painéis shoji de papel acesos com luz âmbar por trás, numa grade de madeira escura. Entre os andares, uma galeria com corrimão de madeira. Pilares de madeira escura a intervalos iguais, viga grossa no alto e rodapé de tábuas embaixo. Sem pergaminho, sem lanternas, sem sombras de pessoas. Continua igual nas duas bordas, para repetir lado a lado. |
| `altar.png` | horizontal | **transparente** | O centro da parede do fundo de um dojo, vista frontal: um nicho recuado de madeira escura com moldura entalhada, com um grande pergaminho vertical creme pendurado, com os kanji **修心館** escritos de cima para baixo a pincel em tinta preta. Embaixo do pergaminho, uma passagem escura com portas de correr. A base é uma linha reta embaixo. Fundo transparente em volta do nicho. |
| `lanterna_poste.png` | **vertical** | **transparente** | Lanterna de dojo de pé, vista de frente: um poste alto e fino de madeira escura sobre uma base quadrada, com uma lanterna de papel quadrada acesa no topo, armação de madeira e o brasão do dojo pintado em preto no papel. A base toca a borda de baixo. Fundo transparente. |
| `pilar.png` | **vertical** | **transparente** | Vista frontal de um pilar grosso de madeira escura envernizada (madeira natural, não laqueada de vermelho), à esquerda da imagem, com um pedaço de viga de madeira no topo. Preso ao pilar, à direita, um suporte de madeira com uma grande lanterna de papel cilíndrica (chōchin) acesa, branco-amarelada, com o brasão do dojo pintado em preto. O pilar toca a borda de baixo da imagem. Fundo transparente. |
| `teto.png` | horizontal, larga e baixa | **transparente** | Faixa de teto de dojo japonês vista de frente e um pouco de baixo: vigas transversais grossas de madeira escura com suportes entalhados, iluminadas por baixo pela luz quente do salão. Só a faixa do teto, no terço de cima da imagem; a borda de baixo é irregular (as vigas) e o resto é transparente. Continua igual nas duas bordas. |
| `makiwara.png` | **vertical** | **transparente** | Poste de treino de dojo (makiwara) visto de frente: um tronco redondo e grosso de madeira, gasto, com lascas e marcas de golpes, enrolado com corda de palha trançada na altura do peito, preso numa base quadrada de madeira apoiada no chão. A base toca a borda de baixo. Sem lascas voando. Fundo transparente. |
| `frente.png` | horizontal | **transparente** | Primeiro plano de um dojo, muito perto da câmera, só nos cantos de baixo da imagem: à esquerda, o poste e o corrimão de uma cerca baixa de madeira escura; à direita, a ponta de um suporte de madeira com espadas de treino (bokken). Quase em silhueta, contornados pela luz âmbar. Todo o meio da imagem é transparente. |
| `chao.jpg` | quadrado | opaco | Piso de dojo visto **exatamente de cima** (vista ortográfica, sem perspectiva): tábuas largas de madeira cor de mel escura, enceradas e muito brilhantes, todas paralelas, correndo de cima para baixo da imagem, com veios e juntas finas. No centro, o brasão do dojo incrustado em madeira escura no piso, grande, dentro de um círculo de treino pintado. Iluminação uniforme, sem sombras de objetos. As bordas esquerda e direita continuam o padrão das tábuas. |
| `chao_tabuas.jpg` | quadrado | opaco | Peça para **editar o `chao.jpg`**: "a mesma imagem, só sem o brasão e sem o círculo do centro, com as tábuas continuando no lugar deles". Assim as tábuas saem do mesmo tamanho nas duas imagens. |

Dicas do dojo:
- O pergaminho com **修心館** é a única exceção ao "sem texto". Se os kanji vierem errados,
  gere de novo; se insistir no erro, peça o pergaminho em branco e o texto é aplicado depois.
- Nada de silhuetas atrás do shoji (as páginas têm alunos assistindo, o cenário não pode ter).
- As tábuas do piso precisam correr **de cima para baixo** na imagem: é o que vende a
  perspectiva quando o jogo deita o piso, igual às linhas que fogem para o fundo na página 2.
- O `frente.png` pode vir nítido: o desfoque é aplicado na preparação, como na página, onde
  esses vultos aparecem borrados.
- O brilho das lanternas, o reflexo forte no piso encerado e as pétalas são feitos pelo
  jogo. Não precisa pôr brilho exagerado na arte.

## Notas do Dojo Shūshinkan

As imagens do GPT passam pelo `montar_dojo.py` (em vez do `preparar_cenario.py` sozinho):

```bash
pip install pillow numpy
python tools/stage/montar_dojo.py --entrada pasta/com/as/imagens
```

- **Piso**: as tábuas vieram levemente em perspectiva (abrem em leque, 74 px no topo e
  86 px na base). O script endireita cada linha, monta o assoalho tábua por tábua,
  sorteando as tábuas da imagem do GPT, com emendas e tons variados (repetir a imagem
  inteira, mesmo espelhada, deixava brilhos e veios em grade), e cola o brasão do
  `chao.png` embaixo dos lutadores, com as juntas alinhadas. Cada tábua tem ~68 unidades
  do mundo e o círculo do brasão ~750.
- **Primeiro plano**: o `frente.png` é dividido em `frente_esq.png` (cerca) e
  `frente_dir.png` (suporte de bokken), desfocados. Ficam no chão a −250, na frente dos
  lutadores, e aparecem de vez em quando (`periodo` 1.9) quando a câmera anda.
- **Teto**: o `teto.png` é usado quatro vezes, a 250 (vigas com lanternas, perto) e a
  550, 800 e 1080, empilhadas acima da parede. Quando a câmera afasta ou no super pulo,
  aparece o andar de cima do salão em vez de um vazio escuro. Só a mais funda tem
  `corAcima`: nas outras, a cor cobriria as faixas de trás.
- **Pilares**: a 230, na frente do teto, para a lanterna chōchin não ficar escondida
  atrás das vigas.
- O brasão da lanterna do pilar veio diferente (três folhas) do das lanternas de poste e
  do piso (flor de cerejeira). Para corrigir, gere o `pilar.png` de novo pedindo "o mesmo
  brasão das outras imagens: flor de cerejeira de cinco pétalas dentro de um círculo".


## Prompts do Dojo da Yin-Lu (連武館)

Terceiro cenário (`dojo_yinlu`), baseado nas duas páginas de mangá da Yin-Lu. Ao contrário
do Shūshinkan, a luta é **ao ar livre, de dia**, no pátio de pedra em frente ao dojo: ao
fundo o portão de madeira do 連武館 com duas lanternas de papel (página 1), muros de reboco
branco com telhado de telhas escuras, árvores atrás dos muros e, bem longe, os arranha-céus
da cidade (página 2). Mais perto ficam os terraços e degraus de pedra, os pilares vermelhos
com o estandarte bordô da lótus e, na frente dos lutadores, pedras e degraus quebrados
(o canto de baixo da página 1). As folhas verdes caindo da página 1 são as `petalas` do
jogo, em verde.

Regra geral: cole antes de cada prompt. No ChatGPT, anexe as duas páginas e diga "use as
imagens anexadas só como referência de estilo, clima e arquitetura".

```
Arte de cenário para jogo de luta 2D em estilo anime, mesmo traço de um mangá de luta
moderno (estilo SNK / King of Fighters), dia ensolarado de céu azul com nuvens brancas,
luz do sol quente vindo da esquerda, sombras suaves, cores vivas e saturadas, contorno
limpo e sombreamento em cel-shading. Arquitetura de dojo tradicional chinês de madeira
escura, telhas cinza-azuladas, paredes de reboco branco e pedra cinza-clara. Sem
personagens, sem pessoas, sem animais, sem texto, sem marca d'água.
```

O **emblema do dojo** aparece nas lanternas, no estandarte e no piso. Use sempre a mesma
descrição: "o mesmo emblema da referência: uma flor de lótus estilizada de oito pétalas
dentro de um círculo".

| Arquivo | Formato | Fundo | Prompt |
| --- | --- | --- | --- |
| `ceu.jpg` | horizontal | opaco | Céu diurno de verão: azul intenso no topo clareando para azul-claro perto do horizonte, grandes nuvens cúmulos brancas e fofas na metade de baixo, algumas nuvens finas no alto. Só o céu: sem prédios, sem montanhas, sem árvores, sem chão, sem sol visível. |
| `montanhas.png` | horizontal | **transparente** | Silhueta de morros distantes cobertos de floresta, azul-esverdeado claro e desbotado pela distância, com névoa clara no pé, ocupando só a metade de baixo da imagem. As bordas esquerda e direita continuam no mesmo nível, para a imagem poder se repetir. |
| `cidade.png` | horizontal | **transparente** | Skyline distante de uma metrópole moderna asiática: arranha-céus de vidro de alturas variadas, azul-acinzentados e desbotados pela distância, alguns guindastes. Prédios mais altos nas pontas e mais baixos no centro (o centro fica atrás do portão). A base é uma linha reta embaixo. Sem céu: tudo acima dos prédios é transparente. |
| `arvores.png` | horizontal | **transparente** | Faixa contínua de copas de árvores vista de frente: pinheiros orientais e árvores frondosas de folhas verde-vivo iluminadas pelo sol da esquerda, alturas variadas. A base é uma faixa reta e densa de folhagem, sem troncos. Continua igual nas duas bordas, para se repetir lado a lado. |
| `portao.png` | horizontal | **transparente** | Portão de entrada de um dojo tradicional chinês, vista **exatamente de frente** e simétrica, sem perspectiva: portal de madeira escura com telhado de telhas cinza-azuladas de beirais curvos, duas portas grossas de madeira com cravos de ferro abertas, mostrando lá dentro um pátio de pedra e o salão principal do dojo. Acima das portas, uma placa de madeira horizontal com os ideogramas **連武館** entalhados em dourado. Nos dois lados, uma lanterna de papel cilíndrica cor de pêssego pendurada, com o emblema do dojo em vermelho. Base de pedra reta embaixo. Só o portão, sem muros dos lados e sem piso na frente. Fundo transparente. |
| `muro.png` | horizontal | **transparente** | Trecho de muro de dojo, vista frontal, em linha reta: paredes de reboco branco divididas em painéis por postes de madeira escura, base de blocos de pedra cinza-clara, coberto no alto por um telhadinho de telhas cinza-azuladas com beiral. Base reta embaixo. Continua igual nas duas bordas, para se repetir lado a lado. |
| `terraco.png` | horizontal | **transparente** | Borda de um terraço de pedra de templo, vista frontal, em linha reta: balaustrada baixa de pedra cinza-clara com colunas quadradas e corrimão de pedra, sobre um muro de arrimo de blocos de pedra com três degraus largos embaixo, tufos de grama e arbustos verdes nas juntas. Continua igual nas duas bordas, para se repetir lado a lado. |
| `pilar.png` | **vertical** | **transparente** | Vista frontal de um pilar redondo de madeira laqueada vermelha, alto, à esquerda da imagem, sobre uma base de pedra quadrada, com um pedaço de viga escura e telhas no topo. Pendurado na viga, ao lado do pilar, um estandarte comprido de tecido bordô com o emblema do dojo em branco. À direita, uma lanterna de papel cor de pêssego pendurada, com o emblema do dojo em vermelho. O pilar toca a borda de baixo da imagem. Fundo transparente. |
| `lanterna_pedra.png` | **vertical** | **transparente** | Lanterna de pedra tradicional de granito cinza, vista de frente, sobre uma base de pedra, com o compartimento de luz aberto e **vazio, sem chama** (o jogo desenha a luz), musgo verde nas bordas, ao lado de duas pedras arredondadas e um arbusto baixo. A base toca a borda de baixo. Fundo transparente. |
| `frente.png` | horizontal | **transparente** | Primeiro plano do pátio de um dojo, muito perto da câmera, só nos cantos de baixo da imagem: à esquerda, blocos de pedra quebrados e um pedaço de degrau de pedra rachado; à direita, uma pedra grande arredondada com um arbusto. Todo o meio da imagem é transparente. |
| `chao.jpg` | quadrado | opaco | Piso de pátio de dojo visto **exatamente de cima** (vista ortográfica, sem perspectiva): grandes lajotas retangulares de pedra cinza-bege, gastas pelo uso, todas do mesmo tamanho, em fileiras retas, com juntas escuras finas e um pouco de musgo nas juntas. No centro, um grande círculo de pedra mais clara com o emblema do dojo entalhado em baixo-relevo. Iluminação uniforme, sem sombras de objetos. As bordas esquerda e direita continuam o padrão das lajotas. |
| `chao_lajotas.jpg` | quadrado | opaco | Peça para **editar o `chao.jpg`**: "a mesma imagem, só sem o círculo e sem o emblema do centro, com as lajotas continuando no lugar deles". Assim as lajotas saem do mesmo tamanho nas duas imagens. |

Dicas do dojo da Yin-Lu:
- A placa com **連武館** é a única exceção ao "sem texto". Se os ideogramas vierem errados,
  gere de novo; se insistir no erro, peça a placa lisa e o texto é aplicado depois.
- O `portao.png` precisa vir de frente e sem piso: o portão da página 1 está em ângulo, mas
  no cenário ele fica no fundo, encarando a câmera.
- O `frente.png` pode vir nítido: o desfoque é aplicado na preparação, como no Shūshinkan.
- O brilho das lanternas, a neblina clara da distância e as folhas caindo são feitos pelo
  jogo. Não precisa pôr na arte.

## Notas do Dojo da Yin-Lu

As imagens do GPT passam pelo `montar_yinlu.py`:

```bash
pip install pillow numpy
python tools/stage/montar_yinlu.py --entrada pasta/com/as/imagens
```

- **Piso**: o `chao.png` e o `chao_lajotas.png` vieram certos (de cima, colunas retas, as
  mesmas juntas nas duas). Só a largura não era um número inteiro de colunas: na repetição
  a emenda ficava com uma coluna larga demais. O script corta as duas nas juntas, em
  volta do emblema (10 colunas de ~131 px). O piso vai de −600 a 700; cada coluna tem
  ~165 unidades do mundo e o círculo do emblema ~800, entre os dois lutadores.
- **Primeiro plano**: o `frente.png` é dividido em `frente_esq.png` (degraus quebrados) e
  `frente_dir.png` (pedra com arbusto), desfocados, a −250. O lado que o GPT cortou na
  borda da imagem é esfumado: com a câmera afastada, essas pedras aparecem no meio do piso
  e o corte reto ficava à mostra.
- **Lanterna de pedra** (`lanterna de pedra.png` vira `lanterna_pedra.png`): o nicho da luz
  veio vazado e mostrava o cenário de trás. O script pinta o vão com o interior escuro da
  pedra; o brilho que pulsa é do jogo.
- **Portão**: o alfa veio em 254, o portão inteiro ficava levemente transparente. O
  script deixa opaco.
- **Composição** (profundidades): lanternas de pedra 130, pilares 300, terraço 700 (fim do
  piso; os degraus do meio sobem para o portão), portão 1000, muro 1060, árvores 1700,
  montanhas 9000, céu. O portão fica atrás do terraço, como se estivesse num nível acima
  do pátio. Duas neblinas claras (1400 e 5000) dão a distância do dia.
- Sem reflexo no chão (pedra seca de dia): `reflexoLutadores` e `reflexoLuzes` em 0. As
  folhas caindo são as `petalas` em verde (`#7FC24A`).
- **Falta o `cidade.png`** (o skyline ao longe). O cenário funciona sem ele: entre as
  árvores e as montanhas. Quando vier, entra como camada a ~4200, atrás das árvores e na
  frente das montanhas, com `x` 0.5 (o centro mais baixo fica atrás do portão).
- No super pulo aparece o corte reto no alto do `pilar.png` (o telhado termina na borda
  da imagem), igual aos pilares do Templo da Lua.
- O ep02 usa este cenário na luta CONVITE do Ryūga × Yin-lu (`"cenario": "dojo_yinlu"`).
