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

Planejamento do `stage.json` (ponto de partida, ajustar no visualizador):

| Camada | Profundidade | Observação |
| --- | --- | --- |
| `parede` | 1200 | `repetir: "espelhar"`, alta (vai acima da tela no super pulo), luzes fracas nos shoji com `reflexo` |
| `altar` | 1180 | `noChao`, `repetir: "nao"`, centrado nos lutadores (x 0.5) |
| neblina | 900 | âmbar bem leve, a "luz no ar" do salão |
| `lanterna_poste` | 560 (fim do piso) | `noChao`, `repetir: "espelhar"`, luz no topo com `reflexo` |
| `pilares` | 300 | como no templo, luz na chōchin |
| `teto` | 250 | como no templo, `corAcima` madeira escura |
| `makiwara` | 140 | `noChao`, nas laterais |
| `frente` | −250 | na frente dos lutadores, só nos cantos |
| `chao` | perto −600, longe 1200 | `reflexoLutadores` ≈ 0.32, `reflexoLuzes` ≈ 0.6 (piso espelhado da página 2), `imagemLajotas: "chao_tabuas.jpg"` |
| `petalas` | — | poucas (≈ 30), como na página |
