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

## Prompts do Dojo Shinshinkan (修心館)

Segundo cenário (`dojo_shinshinkan`), baseado na página de mangá do dojo 修心館: um dojo
tradicional de madeira no meio de uma Tóquio moderna, à noite, com cerejeiras. A luta é
**dentro do dojo**, no piso de madeira encerado, com a parede do fundo aberta para o jardim
e a cidade com neon lá atrás. Assim o cenário tem os mesmos planos de profundidade do
Templo da Lua (céu, cidade, jardim, parede, pilares, teto e objetos perto da câmera).

Regra geral: cole antes de cada prompt. No ChatGPT, anexe também a página do mangá e diga
"use a imagem anexada só como referência de estilo e de clima".

```
Arte de cenário para jogo de luta 2D em estilo anime, noite. Dojo tradicional japonês de
madeira escura envernizada, iluminado por luz quente âmbar de lanternas de papel, em
contraste com o azul da noite e o neon rosa e ciano da cidade. Cores ricas, traço limpo e
detalhado. Sem personagens, sem silhuetas de pessoas, sem texto, sem marca d'água.
```

| Arquivo | Formato | Fundo | Prompt |
| --- | --- | --- | --- |
| `ceu.jpg` | horizontal | opaco | Céu noturno sobre uma metrópole japonesa: azul-marinho profundo no alto, descendo para roxo e um brilho rosa-alaranjado de luz da cidade perto da borda de baixo. Poucas estrelas, nuvens finas iluminadas por baixo, uma lua crescente pequena no alto à esquerda. Só o céu: sem prédios, sem árvores, sem chão. |
| `cidade.png` | horizontal | **transparente** | Prédios de Tóquio à noite vistos do nível da rua, a uma quadra de distância: prédios comerciais estreitos de alturas variadas, janelas acesas amarelas e brancas, letreiros verticais luminosos e neon rosa, vermelho e ciano (só formas e brilhos, sem letras legíveis), caixas d'água e antenas nos telhados, alguns arranha-céus mais altos atrás. A base é uma linha reta na parte de baixo. Sem céu: tudo acima dos prédios é transparente. As bordas esquerda e direita têm prédios de altura parecida, para a imagem poder se repetir. |
| `jardim.png` | horizontal | **transparente** | Jardim japonês nos fundos de um dojo, à noite, vista frontal: cerejeiras floridas rosa e pinheiros podados em forma de nuvem, lanternas de pedra (tōrō) acesas com luz quente, pedras e musgo. Atrás, um muro baixo de reboco branco com telhado de telhas escuras e beiral de madeira, em linha reta. A base é uma linha reta embaixo. Acima das árvores e do muro, fundo transparente. As bordas continuam no mesmo nível, para repetir. |
| `parede.png` | horizontal | **transparente** | Parede dos fundos de um dojo tradicional japonês vista exatamente de frente, em linha reta: pilares de madeira escura dividindo quatro vãos iguais, uma viga grossa no alto com painel de madeira entalhada (ranma) e um rodapé de tábuas na altura do joelho. Os dois vãos do meio estão abertos, com as portas de correr empurradas para os lados: o vão é **transparente**. Os dois vãos das pontas têm portas shoji de papel fechadas, brilhando com luz âmbar por trás, sem sombras de pessoas. Continua igual nas duas bordas, para repetir lado a lado. |
| `pilar.png` | **vertical** | **transparente** | Vista frontal de um pilar grosso de madeira escura envernizada (madeira natural, não laqueada de vermelho), à esquerda da imagem, com um pedaço de viga de madeira no topo. Pendurado na viga, ao lado do pilar, um estandarte vertical comprido de tecido creme com moldura de madeira, com os kanji **修心館** escritos de cima para baixo a pincel em tinta preta. À direita, uma lanterna de papel redonda (chōchin) acesa, branco-amarelada, com um brasão circular preto (mon) pintado. O pilar toca a borda de baixo da imagem. Fundo transparente. |
| `teto.png` | horizontal, larga e baixa | **transparente** | Faixa de teto de dojo japonês vista de frente e um pouco de baixo: vigas transversais grossas de madeira escura com suportes entalhados e, no centro, uma corda sagrada shimenawa trançada com tiras de papel shide brancas penduradas. Só a faixa do teto, no terço de cima da imagem; a borda de baixo é irregular (vigas e corda) e o resto é transparente. Continua igual nas duas bordas. |
| `makiwara.png` | **vertical** | **transparente** | Poste de treino de dojo (makiwara) visto de frente: um tronco redondo e grosso de madeira, gasto, com lascas e marcas de golpes, enrolado com corda de palha trançada na altura do peito, preso numa base quadrada de madeira apoiada no chão. A base toca a borda de baixo. Sem lascas voando. Fundo transparente. |
| `andon.png` | **vertical** | **transparente** | Luminária de chão japonesa (andon) vista de frente: armação quadrada de madeira escura com painéis de papel washi, acesa com luz âmbar suave por dentro, sobre pés baixos. A base toca a borda de baixo. Fundo transparente. |
| `chao.jpg` | quadrado | opaco | Piso de dojo visto **exatamente de cima** (vista ortográfica, sem perspectiva): tábuas largas de madeira cor de mel escura, enceradas e brilhantes, todas paralelas, correndo de cima para baixo da imagem, com veios e juntas finas. No centro, um grande brasão circular (mon) do dojo incrustado em madeira escura no piso: um círculo com três vírgulas (tomoe) girando. Iluminação uniforme, sem sombras de objetos. As bordas esquerda e direita continuam o padrão das tábuas. |
| `chao_tabuas.jpg` | quadrado | opaco | Peça para **editar o `chao.jpg`**: "a mesma imagem, só sem o brasão do centro, com as tábuas continuando no lugar dele". Assim as tábuas saem do mesmo tamanho nas duas imagens. |

Dicas do dojo:
- O estandarte com **修心館** é a única exceção ao "sem texto". Se os kanji vierem errados,
  gere de novo; se insistir no erro, peça o estandarte em branco e o texto é aplicado depois.
- Nada de silhuetas de lutadores atrás do shoji (a página tem, o cenário não pode ter).
- As tábuas do piso precisam correr **de cima para baixo** na imagem: é o que vende a
  perspectiva quando o jogo deita o piso. Tábuas atravessadas ficam paradas e chapadas.
- O brilho das lanternas e do andon, o reflexo no piso encerado e as pétalas entrando pela
  parede aberta são feitos pelo jogo. Não precisa pôr brilho exagerado na arte.

Planejamento do `stage.json` (ponto de partida, ajustar no visualizador):

| Camada | Profundidade | Observação |
| --- | --- | --- |
| `ceu` | 100000 | `corAcima` azul-marinho |
| `cidade` | 3500 | `repetir: "espelhar"`, aparece pelos vãos abertos e acima do muro |
| neblina | 2000 | roxo-rosado bem leve, separa a cidade do jardim |
| `jardim` | 1100 | `noChao`, `repetir: "espelhar"`, luzes nas lanternas de pedra |
| `parede` | 560 (fim do piso) | `noChao`, `repetir: "espelhar"`, luzes fracas nos shoji |
| `pilares` | 300 | como no templo, luz na lanterna chōchin |
| `teto` | 250 | como no templo |
| `makiwara` | 140 | `noChao`, nas laterais |
| `andon` | 90 | `noChao`, nas laterais, luz com `reflexo` no piso |
| `chao` | perto −600, longe 560 | `reflexoLutadores` ≈ 0.28 (piso encerado), `imagemLajotas: "chao_tabuas.jpg"` |
| `petalas` | — | menos que no templo (≈ 40), vento entrando pelo fundo |
