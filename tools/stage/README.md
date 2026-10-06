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
- O `chao_lajotas.jpg` é provisório: é o canto do `chao.jpg` (fora do emblema) repetido
  espelhado. Para uma versão melhor, gere no GPT o mesmo prompt do piso **sem o emblema**
  ("só as lajotas, sem nenhum desenho no centro") e salve com esse nome.
- A cidade já vem com o rio e os reflexos pintados, então a camada `rio` só acrescenta os
  brilhos que piscam (`alfa` 0).
