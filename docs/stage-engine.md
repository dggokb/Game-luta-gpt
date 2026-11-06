# Motor de cenário com falso 3D — v0.78

O cenário deixou de ser um desenho único (`StageRenderer`) e virou **camadas com
profundidade**, projetadas pela mesma câmera da luta. O que está longe anda e cresce menos
que o que está perto, o piso é desenhado em perspectiva e luzes, fogo, água, neblina,
pétalas e reflexos no chão molhado dão vida à cena. **A luta continua 2D**: colisões e o
`CombatEngine` não mudam.

Para criar um cenário (arte pelo GPT + `stage.json`), veja `tools/stage/README.md`.

## Módulos

| Módulo | Conteúdo |
| --- | --- |
| `android/stage-core` | `StageCamera`, `StageDefinition` (stage.json), `StagePack`, `StageScene`, `StageWorld` |
| `android/render-core` | Pincel compartilhado (`RenderCanvas`), imagens, arquivos, JSON |
| `android/app` | `GameView` desenha o fundo, os lutadores (e seus reflexos) e a frente |
| `android/pc-preview` | `StagePreview`: janela ou quadros PNG com a câmera de luta simulada |

`stage-core` é Java 8 puro, igual no Android e no PC.

## A câmera de profundidade (`StageCamera`)

O jogo desenha os lutadores com `tela = (mundo − esquerda) × zoom`. A `StageCamera`
reproduz isso **exatamente** no plano dos lutadores (profundidade `z = 0`) e aplica
perspectiva nos outros planos:

```
escala(z) = D / (D/zoom + z)          D = camera.distancia (stage.json)
telaX     = 640 + (x − centroX) × escala(z)
telaY     = H   + (y − olhoY)   × escala(z)        H = horizonte (stage.json)
olhoY     = topo + H / zoom
```

- Com `z = 0` dá exatamente o desenho atual (há um teste para isso).
- O zoom da luta vira **distância da câmera** (ela se aproxima). Por isso o fundo quase não
  cresce quando a câmera aproxima, e é isso que vende o 3D.
- O horizonte fica sempre na mesma altura da tela. No super pulo a câmera sobe junto
  (`topo` diminui): o piso perto desce bastante e o fundo quase não se mexe.
- `z > 0` está atrás dos lutadores; `z < 0` na frente.

`camera.zoom` (opcional, padrão 1) multiplica o zoom da câmera da luta só neste cenário
(`CameraRig.escala`): menor que 1 abre a câmera, com os lutadores menores e mais cenário na tela
(a Arena Central pintada usa 0,8, para chegar perto do enquadramento do conceito).
`camera.zoomNoPulo` (padrão true): com false, o pulo não abre o zoom da câmera (como no super
pulo, ela só sobe junto se o lutador sair da margem de cima). Num cenário com o chão em
perspectiva, o vaivém do zoom a cada pulo dava tontura.
`camera.zoomRepouso` (opcional): o zoom da câmera em que o stage.json descreve as camadas, no
lugar da câmera de repouso do jogo (1,24), com o chão no mesmo lugar da tela. Para um cenário
pintado a partir de uma tela da luta: com a câmera da luta nesse zoom, a pintura aparece igual.

**Camada curva** (`"curva": [[u, profundidade], ...]` numa camada `imagem`): a camada não é um
plano; cada coluna (u = fração da largura) fica na sua profundidade, interpolada. Desenhada em
fatias verticais (16 px), de trás para a frente. Parada, a tela é igual à pintura; andando, cada
fatia anda na velocidade da sua profundidade. Para a arquibancada que dá a volta na arena e chega
perto da câmera nas pontas. Sem repetição, bandeiras, plateia ou telão.

**Onda** (`"onda": {"amplitude", "velocidade", "fase", "leve"}`): o alfa da camada vai de 1 a
1 − amplitude e volta. Duas camadas com a mesma luz pintada em ângulos diferentes, em fase oposta,
cruzam uma na outra e a luz parece varrer (os fachos dos spots da Arena Coberta). No leve, alfa
fixo (`leve`).

## Câmera de repouso

O `stage.json` descreve **onde cada camada aparece na tela** (frações de 0 a 1) na câmera
de repouso: zoom 1,12, centro no meio dos lutadores no início (x = 700) e chão em 552 px
(`StageWorld`, valores do `CameraRig`). O motor converte isso para o mundo 3D uma vez,
ao carregar. Assim, quem monta o cenário pensa em "a cidade ocupa 40% da altura da tela",
não em unidades do mundo.

## Ordem de desenho

`GameView.drawFrame`, a cada quadro:

1. `stageScene.setCamera(zoom final, esquerda, topo)`: a câmera da luta, já com o zoom do
   Super e do ultra.
2. `drawBackground`:
   1. cor de fundo;
   2. camadas atrás do fim do piso (céu, montanhas, cidade, água, neblina, portal...), da
      mais longe para a mais perto;
   3. piso;
   4. reflexo das luzes e fogos no piso;
   5. camadas entre o fim do piso e os lutadores (grade, pilares, braseiros). O fim do piso
      é o `ate` do chão (o chão liso até a arquibancada), não o `longe` das lajotas: as
      pilastras da Central a 3350 ficam em cima do chão, que vai até 3500. Antes o chão vinha
      por cima delas, cobria a base e no super pulo pareciam coladas na arquibancada;
   6. respingos, chuva, pétalas e poeira atrás dos lutadores.

   `drawBackground(canvas, z, desenho)` encaixa mais um desenho na profundidade `z`: depois
   das camadas mais fundas que ele e antes das mais rasas. É assim que o narrador fica no
   palco (`z = 300`, na frente das paredes e dos prédios, atrás dos pilares, das lanternas e
   dos objetos do chão; ver [historia/narrador.md](historia/narrador.md)). `screenX`, `groundScreenY`,
   `scale` e `worldX` projetam um ponto naquela profundidade.
3. No mundo da luta: reflexo dos lutadores (espelhados no chão, com transparência), os
   lutadores, projéteis e efeitos.
4. `drawForeground`: camadas com `z ≤ 0` e respingos, chuva, pétalas e poeira na frente.
5. HUD, controles e a cinemática do ultra.

## O piso

A imagem do piso é uma **textura vista de cima**: a borda de cima da imagem é a
profundidade `longe` e a de baixo, `perto`. A largura no mundo segue a proporção da
imagem e ela se repete na horizontal, centrada em `centroX` (o emblema do Templo da Lua
fica embaixo dos lutadores). Com `imagemLajotas`, as repetições fora do centro usam essa
textura sem emblema.

O piso é desenhado em faixas horizontais de 3 px de tela. Para cada faixa, o motor
descobre a profundidade (`floorDepthAt`), pega a linha certa da textura e o trecho de x
visível naquela profundidade, e estica na faixa (`drawImageRegion`). É a técnica de "line
scroll" dos jogos de luta 2D clássicos, sem nenhum recurso especial de GPU, então funciona
igual no Android e no Java2D.

## Camadas

| Tipo | O que é |
| --- | --- |
| `imagem` | Imagem plana numa profundidade. Pode ser repetida (`repetir`) ou espelhada a cada cópia (`espelhar`, esconde emendas), com `periodo` entre cópias. `noChao` apoia a base no chão da própria profundidade. `reflexo` desenha uma cópia espelhada embaixo da base (reflexo na água). `corAcima` preenche a tela acima da camada (céu no super pulo). |
| `neblina` | Degradê do transparente para a cor, entre `topo` e `base`, numa profundidade. Separa os planos (profundidade atmosférica). |
| `agua` | Plano horizontal entre as profundidades `perto` e `longe`, com degradê e brilhos que piscam presos ao mundo. Translúcida para mostrar o reflexo da camada atrás dela. |
| `ator` | Personagem de fundo animado (`pasta` com `ator.json` e as folhas), apoiado no chão (`noChao` + `altura`), `virar` espelha. Fica na animação parada e toca as reações que o jogo pede (`StageScene.react("licao")`, `actorPlay`). Sem a folha de uma reação, usa a parada com um movimento provisório. Veja `docs/tutorial-plateia.md`. |

**Plateia viva** (`"plateia"` numa camada de imagem: um objeto ou uma lista de faixas
`{"topo", "base", "pulo", "colunas"}`, em fração da altura da imagem): a faixa da plateia é
redesenhada em colunas estreitas que pulam, cada uma no seu tempo (vizinhas parecidas, como
grupinhos). Só o miolo da faixa pula: uma margem do tamanho do maior pulo fica parada em cima e
em baixo, então nada fora da faixa é coberto. A faixa precisa ficar toda dentro da plateia
(sem pegar grade, sacada ou parapeito). Com `"mascara"` (imagem só com a gente, do mesmo tamanho
da camada; `montar_porto.plateia` gera), só os pixels de gente pulam: estandartes, luzes e
escadas que cruzam a faixa ficam parados (sem a máscara, a coluna inteira pulava e cortava
os estandartes). Com `"recortada": true` a gente foi tirada da imagem da camada (no lugar,
assentos escuros) e só existe na máscara: ela é desenhada inteira em todo quadro e cada grupo
pula livre, sem margem; embaixo aparece assento vazio. `"flashes"` ([[x, y], ...], gerados pelo
`tools/stage/pontos_plateia.py` a partir da máscara) piscam flashes de câmera na plateia, muito
mais quando a luta esquenta. O jogo chama `StageScene.cheer(0..1)` a cada golpe que acerta (forte no pesado, 1 no
KO) e a plateia pula mais e mais rápido, voltando ao normal em uns dois segundos.

**Cachoeiras** (`"cachoeiras": {"imagem", "quadros", "fps", "areas": [[x, y, largura,
altura], ...]}` numa camada de imagem, áreas em fração da imagem): a água pintada corre. A
`imagem` é uma folha de quadros feita pelo `tools/stage/cachoeiras.py` (uma linha por área,
com a altura proporcional à dela; uma coluna por quadro, da largura da área mais larga, com a
área encostada à esquerda). Em cada quadro só a água muda: a máscara pega os pixels claros,
sem cor e riscados na vertical (lanternas, estandartes e a névoa lisa ficam de fora), e por
cima deles desce um ruído comprido (faixas claras e escuras, um pouco de espuma) que fecha o
ciclo no último quadro. As pedras em volta ficam paradas, então nada escorre para fora da
cachoeira. O jogo desenha o quadro da vez sobre a área e o próximo entrando aos poucos (sem
trancos a 12 quadros por segundo); cópias espelhadas da camada ficam fora de fase. Custo: duas
imagens pequenas por área (uma no modo leve). Primeiro uso: Ilha do Torneio (palácio da
Imperial, abismo do Caminho, encosta da Costeira, templo ao longe e os penhascos de todas).

`"cachoeiras"` também aceita uma lista, uma folha por item: o abismo do Caminho tem as quedas
e o **mar agitado** lá embaixo (`abismo_ondas.png`, feito com `"tipo": "ondas"` no
`cachoeiras.py`: 16 quadros a 8 por segundo, manchas largas e baixas vindo devagar para a
câmera, a espuma acendendo; a névoa lisa e as pedras ficam de fora). Com `"colunas"`, a folha
quebra os quadros em fileiras (`colunas` quadros por fileira, cada um um bloco com as áreas
empilhadas), para não passar de uns 2500 px de largura.

**Espuma no pé** (`"espuma": [[x, y, largura], ...]` e `"corEspuma"` num item de
`"cachoeiras"`, em fração da imagem; y é onde a água bate): ali o motor solta borrifos,
nuvens claras que nascem no pé, sobem um pouco, crescem e somem, cada uma no seu tempo (6 por
pé; 3 no modo leve). A espuma pintada fica parada embaixo; os borrifos é que dão o movimento.
No palácio, no abismo (rosada, das tochas) e na encosta (lilás, da distância).

**Fiapos de neblina** (`"fiapos": N` numa `neblina`, com `"deriva"` em unidades do mundo por
segundo): além do degradê, N bancos de névoa por largura de tela (brilhos redondos achatados,
de tamanho e altura sorteados) rente à base da faixa, andando devagar e respirando. Na ilha,
a `neblina-ilhas` (a 8450, logo na frente da borda do mar a 8500) cobre a linha em que as
ilhas e os penhascos encontram a água, que aparecia como um recorte reto. Custo: uns oito
brilhos por quadro; no modo leve não desenha.

**Ondulação do mar**: o mar deitado, além da `corrente`, vai e volta em cada faixa, defasado
com a profundidade (ondas rolando).

**Deriva e balanço** (`"deriva"`: unidades do mundo por segundo, negativo = para a esquerda;
`"balanco"`: sobe e desce em fração da altura): a camada anda sozinha e balança nas ondas, como
os barcos do porto. Com `repetir`, os que saem de um lado voltam pelo outro. Custo: o mesmo de
uma camada parada (só muda a posição).

**Cenário de uma pintura só** (`tools/stage/cenario_pintura.py`, exemplo `stages/arena_pintura`):
a pintura é cortada numa linha reta só, onde o chão encontra a arquibancada. A parte de cima
(céu e arquibancada) vira uma camada `imagem` na profundidade da junção (um plano em pé no
fundo, com céu espelhado em cima para o super pulo, leve névoa e desfoque). A de baixo vira o
chão com `"projecao": {"topo", "base", "x"}` (onde a imagem fica na tela de repouso): cada ponto
do chão pega o pixel que a câmera de repouso vê ali. Parada, a tela é a pintura sem deformar;
andando, cada faixa do chão anda na velocidade da sua profundidade e encontra a parede na junção.
O horizonte sai do anel do chão (um círculo em volta dos lutadores). Com a malha
(`drawImageStrip`) o chão é contínuo; sem ela, faixas de 1 px. Fora da pintura, a cópia espelhada.
Mais perto que o pé da imagem (câmera bem aberta num canto: a câmera de repouso nem alcança
esse chão), repete a última linha em vez de deixar o fundo aparecer embaixo.
`"chaoNaPintura": true` (sem `chao`): o chão já está pintado numa camada e o motor não desenha o
chão liso por cima.

**Chão até o fundo** (`"ate"` no `chao`): atrás da borda de cima da imagem do piso, a imagem
lisa (`imagemLajotas`) se repete na profundidade até `ate`, na mesma escala de lajota. Para o
chão de uma arena ir do ringue até a arquibancada lá no fundo.

**Chão pintado em perspectiva** (`"linhasPerspectiva"` e `"largura"` no `chao`): por padrão as
linhas da imagem do piso vão iguais na profundidade (de `longe` a `perto`), o que serve para um
piso de lajotas feito de cima. Um chão tirado de uma pintura (o panorama da Arena Central) tem
as linhas iguais *na tela*: com `linhasPerspectiva` (a distância da câmera de repouso, que o
`cenario_panorama.py --linhas` imprime) a linha de cada profundidade z sai de 1/(D+z), e a imagem
fica com a mesma nitidez perto e longe (linear em z, a pintura de perto virava borrão esticado
e a de longe ficava espremida). `largura`: a largura da imagem no mundo (sem ela, a da
proporção da imagem).

**Mar deitado** (`agua` com `"imagem"`): a textura do mar vista de cima é desenhada em
perspectiva entre `perto` e `longe`, em faixas, igual ao piso: ondas grandes perto e pequenas
e juntas no horizonte, andando devagar (`corrente`, unidades por segundo; `tamanho` é a largura
de uma cópia no mundo). No fundo a água some na `corLonge`. `"sol": {"x", "cor", "alfa"}`
desenha o caminho de luz do sol (risquinhos que piscam embaixo dele; `x` em fração da tela na
câmera de repouso). Sem isso o mar era uma foto em pé lá no fundo: as ondas grandes da parte de
baixo da foto apareciam logo atrás do piso, enormes perto de tudo, e o que estava sobre a água
deslizava em cima dela no super pulo. Primeiro uso: Ilha do Torneio (`mar_plano.jpg`).

**Apoio** (`"apoio": 9000`): a camada está apoiada noutra mais funda (uma ilha ou um barco
sobre o mar). O mar é uma imagem plana lá no fundo; sem o apoio, quando a câmera sobe no super
pulo, o que está mais perto desce mais que ele e desliza por cima da água (parece voar). Com
o apoio, a base da camada acompanha na vertical o ponto do mar em que ela está na câmera de
repouso; o tamanho e o movimento para os lados continuam os da própria profundidade.
Também serve para um fundo pintado atrás de um estádio (a Arena Central: céu e montanhas a
5000 com `"apoio": 1618`): no super pulo o fundo desce junto com o estádio e não abre, entre as
montanhas e a arquibancada, a faixa do que ninguém pintou; para os lados a paralaxe fica.

Camadas de imagem podem ter **luzes** (brilho que pulsa, com reflexo no piso) e **fogos**
(chama em três camadas tremulando, brilho e brasas), presos a pontos da imagem; eles se
repetem com as cópias da camada.

### Bandeiras ao vento

Camadas de imagem também podem ter **bandeiras** (`"bandeiras"`), que balançam. O pano é
uma imagem separada (`bandeira.png`) presa num retângulo da imagem da camada, em frações
dela (`x`, `y` do canto de cima à esquerda, `largura`, `altura`):

```json
"bandeiras": [ { "imagem": "bandeira.png", "x": 0.2984, "y": 0.2844, "largura": 0.3066, "altura": 0.5341 } ]
```

- O motor desenha o pano em faixas horizontais de 3 px de tela (6 no modo leve) e cada
  faixa vai para o lado: presa em cima, a ponta de baixo é a que mais anda (`balanco`,
  fração da largura, 0,12). Um balanço lento soma com uma onda que desce pelo pano
  (`ondas` ao longo dele, 1,2) e com rajadas; onde o pano dobra a faixa estreita um pouco.
  `velocidade` (1) acelera tudo.
- Cada cópia da camada balança num tempo diferente; nas cópias espelhadas o pano vira junto
  e o vento continua do mesmo lado.
- O GPT pinta o estandarte junto com o pilar. `tools/stage/separar_bandeiras.py` tira o pano
  do `pilar.png` (o topo, preso na vara, fica) e salva `bandeira.png`, com 6 px de sobra
  acima do corte para a emenda não aparecer; ele imprime o trecho do stage.json. Rode uma
  vez depois de trocar o pilar.
- Na Ilha do Torneio são dezenas de estandartes (pórticos, pilastras, holofotes,
  arquibancadas, ponte, ruínas, palácio, abismo, encosta, terminal do porto):
  `tools/stage/soltar_bandeiras.py` recebe só um retângulo com folga em volta de cada um e
  acha o pano pela cor (o vermelho de cada linha, com o emblema e a borda dourada). O
  lugar dele na imagem fica transparente quando ele pendia no vão, ou é tapado com a parede
  dos lados, lisa e na sombra; assim, quando a ponta balança, aparece o fundo e não um
  recorte. Ele grava `<imagem>_bandeiraN.png` e a linha de `"bandeiras"` das camadas.
  Presas na parede balançam menos (`balanco` 0,06; soltas no vão, 0,12).
- Hoje: os pilares do Templo da Lua e do Dojo Yinlu, o poste do porto, o andaime do
  Caminho e os estandartes da Ilha do Torneio.

### Telão

Uma camada de imagem pode ter a tela de um telão (`"telao"`), um retângulo da imagem em
frações dela, como as bandeiras:

```json
"telao": { "x": 0.07, "y": 0.1171, "largura": 0.86, "altura": 0.5878 }
```

- O cenário desenha a imagem (a estrutura, com a tela escura) e chama quem o jogo pôs em
  `StageScene.setTelao` com o retângulo da tela já na perspectiva da camada. O jogo
  (`TelaoLuta`) mostra a luta ali como uma transmissão: os mesmos sprites dos lutadores,
  pequenos, num fundo liso com linhas de TV e o selo AO VIVO.
- **Replay**: depois de um Super, de um combo de 5 acertos ou mais ou de um K.O., o telão
  passa de novo, em câmera lenta (metade da velocidade), de pouco antes do golpe até um
  pouco depois, com o selo REPLAY. Um replay de cada vez.
- **Leve**: o telão não desenha a cena de novo. A cada passo ele guarda só qual clipe e qual
  quadro cada um em campo mostrava e onde estava (4 s guardados, alguns números por
  lutador), e redesenha esses quadros pequenos. Só desenha folhas já carregadas (nunca
  decodifica na hora) e esquece tudo a cada luta nova. No modo de efeitos leves, sem os
  parceiros e sem as linhas de TV.
- Hoje: o telão provisório (`tools/stage/telao_provisorio.py`) na Arena Imperial
  (semifinal) e na Arena Central (final). Prompt da arte definitiva em
  `tools/stage/ilha-do-torneio.md`.

## Efeitos do jogo

- **Reflexo dos lutadores**: `chao.reflexoLutadores` (0 a 1). O `GameView` desenha cada
  lutador espelhado em torno da linha do chão com essa opacidade (`saveLayerAlpha`).
- **Sombra comprida** (`"sombraLutadores"`): a silhueta de cada lutador deitada no chão, para
  o lado contrário da luz (uma cópia do sprite por lutador, preta e translúcida, sem rastro nem
  contorno; no pulo ela clareia). Dois jeitos:
  - **sol** `{ "alfa": 0.6, "x": 0.6, "y": 0.36 }`: a mesma sombra em todo lugar; um ponto a
    `h` acima do chão cai `x·h` para o lado e `y·h` para baixo na tela;
  - **focos** `{ "alfa": 0.6, "focos": [ { "x": -145, "altura": 1080, "profundidade": 2008 } ] }`:
    cada foco (holofote, tocha, poste) é um ponto no mundo, com o `x` do mundo, a `altura`
    acima do chão e a `profundidade` (positiva atrás dos lutadores, negativa na frente). A
    sombra de cada lutador sai para o lado contrário do foco: comprida longe dele, curta
    embaixo dele, e vira de lado quando o lutador passa por baixo. `alcance` (opcional) faz
    ela clarear com a distância até sumir, e `alfa` no foco troca o da sombra. Até 8 focos;
    cada lutador desenha só a sombra dos 3 mais fortes sobre ele (cada uma custa uma cópia do
    lutador por quadro). Um foco mais baixo que o corpo (uma tocha) dá sombra comprida: ela fica
    na direção certa e é só encurtada (até 1,4 da altura). A conta é a da câmera de repouso
    (`FighterShadow.fromFocus`).
  Como sombra de verdade (no OpenGL): dura onde encosta nos pés e abrindo em penumbra e
  clareando até a ponta (a cabeça), num anel de 8 vizinhos com raio que cresce dos pés para a
  cabeça (`suave`, 0 a 1, padrão 0,5: 0 = recortada, 1 = bem aberta e fraca na ponta); e na
  `cor` da luz ambiente que sobra nela (padrão preto; num pôr do sol, roxo-escuro). Sombras de
  focos diferentes se somam onde se cruzam. No Canvas ela é a silhueta lisa.
  Com focos, a contraluz dos lutadores (`luzLutadores.contraluz`) também vem do foco mais forte
  sobre cada um, na direção em que ele aparece na tela visto do peito do lutador (em passos de
  15 graus): vira de lado quando o lutador passa por baixo, e acende menos (35%) com o foco na
  frente dos lutadores, que aí ilumina de frente.
  Na Arena Central os focos são os dois holofotes (o da esquerda forte, o da direita fraco) e
  as chamas dos 5 braseiros do muro (com `alcance`: sombra comprida para a frente, que some
  longe da tocha), calculados pelo `tools/stage/animar_central.py`. O holofote principal é
  o da torre da esquerda do conceito (tela x 340, no alto
  da torre, na profundidade do estádio: x −27, altura 955, profundidade 1618), com a sombra roxo-escura (`#1C0A14`, alfa 0,72, `suave` 0,55).
- **Pétalas**: partículas em 3D (x, y, z) caindo com vento. As de longe são pequenas e
  apagadas, as da frente (`z < 0`) passam grandes na frente dos lutadores.
- **Poeira** (`"poeira"`: `quantidade`, `cor`, `alfa`, `tamanho`, `vento`, `perto`, `longe`,
  `topo`, `base`): grãozinhos de luz boiando em 3D entre `topo` e `base` da tela de repouso. Vão
  com o vento, sobem e descem devagar e piscam (pegam e perdem a luz); não caem. Os de longe
  ficam pequenos e apagados; os da frente dos lutadores, maiores e com halo (fora de foco). No
  modo leve, 1 em cada 3.
- **Chuva** (`"chuva"` no stage.json): riscos em 3D caindo, inclinados pelo vento, finos e
  apagados no fundo e grossos na frente dos lutadores. Cada gota que bate no chão dentro
  do piso deixa um **respingo** (anel achatado que abre e duas gotinhas que pulam, 0,32 s).
  Campos: `quantidade` (240), `cor`, `perto`/`longe` (−300/2500), `velocidade` (queda, 1500
  unidades/s), `vento` (−120), `comprimento` do risco (70), `espessura` (1,6 px), `alfa`
  (0,5) e `respingos` (quantos ao mesmo tempo, 30; 0 desliga). No modo leve
  (`setDetalhe(false)`) chove menos (uma gota em cada 3, `LIGHT_RAIN_STEP`) e sem respingos:
  os riscos são linhas finas e pesam pouco, e o cenário de chuva sem chuva perdia a cara (antes
  o leve tirava a chuva toda, e o AUTO da qualidade descendo para o leve sumia com ela).
  Primeiro uso: Beco do Brucy.

## Relâmpagos

`"relampagos"` no stage.json (primeiro uso: Beco do Brucy). De vez em quando
(`intervaloMinimo`/`intervaloMaximo`, 5–12 s) um raio cai em zigue-zague, com um ou dois
galhos, na `profundidade` dele (9000: atrás da cidade, que fica recortada contra o clarão),
entre `topo` e `base` da tela de repouso. O céu atrás dele clareia (`claraoCeu`, 0,45) e o
cenário todo um pouco (`claraoCena`, 0,12), piscando como relâmpago de verdade: forte, quase
apaga, forte de novo e some em ~0,5 s (`StageScene.lightningEnvelope`). O raio fica preso no
mundo, então anda com a câmera como o resto daquela profundidade. No modo leve, sem o brilho
largo em volta do raio. `StageScene.strikes()` conta os relâmpagos: o `GameView` toca o
trovão (`sons/luta/trovao.wav`, gerado em `tools/sons/gerar_sons.py`) 0,2–0,8 s depois.

## Desempenho

O `GameView` agora pede o Canvas pela GPU (`lockHardwareCanvas`, Android 8+) e volta para
o Canvas por software se não conseguir. Várias imagens em tela cheia por quadro pesam
demais por software em telas grandes. A GPU também deixa a cinemática do ultra mais leve.

- **Teto de resolução**: o jogo desenha em no máximo 1080 linhas (`QualidadeGrafica.ALTURA_MAXIMA`),
  mesmo no ALTO; os níveis abaixo são frações desse teto. A arte é feita para 1280×720 e acima
  de 1080 não ganha nada, só custa: celular bom tem tela 1440p+ e desenhava 1,8–2,2× os pixels.
- **Gradientes guardados**: brilhos (`fillRadialGlow`) e degradês (`fillVerticalGradient`) usam um
  gradiente por cor, de tamanho 1, posicionado pela transformação do Canvas
  (`AndroidRenderCanvas`). Antes cada chamada criava um gradiente novo, dezenas por quadro com as
  luzes dos cenários.
- **Linhas de velocidade** (impact frame e finalização de mangá): todas as cunhas de uma cor num
  caminho só, um desenho por cor em vez de um por cunha (centenas).
- **Qualidade AUTO** (`QualidadeGrafica`): desce um nível quando a luta fica abaixo de 50 fps por
  2 janelas de 2 s, e agora também **sobe** de novo depois de ~30 s de luta folgada (56+ fps). Se
  sobe e logo não aguenta, não tenta de novo na mesma sessão. O nível aprendido fica salvo.
- **Contador de FPS** (pausa > FPS, ligado de começo e salvo): no rodapé, os quadros desenhados por
  segundo (medidos a cada 0,5 s), o nível de gráficos (AUTO e o nível que ele escolheu, ou o fixo)
  e a altura em que o jogo está desenhando. Verde a 55+, amarelo a 45+, vermelho abaixo.
- Para medir no aparelho por partes: DEBUG ligado mostra a linha `Quadros` (fps, ms por quadro e
  por parte); ou `adb logcat -s Quadros`.

Se precisar aliviar mais: aumentar `FLOOR_STRIP` (faixas do piso), reduzir pétalas e brilhos
da água, ou usar imagens menores.

## Testes

`gradle :stage-core:test` (`StageSceneTest`): o plano dos lutadores bate com o desenho do
jogo, o fundo anda e cresce menos, a profundidade do piso é invertida corretamente, a
validação do stage.json, o desenho completo com e sem imagens em várias câmeras sem
deixar o Canvas desbalanceado e a bandeira presa em cima balançando embaixo.

**Reflexo comprido das luzes** (`"reflexoComprimento"` no `chao`, 0 a 1): o reflexo de cada luz
com `"reflexo": true` vira um risco vertical descendo pelo chão encerado, até essa fração do
caminho para a borda de baixo da tela. Desenhado na tela, fica na vertical com a câmera em
qualquer lugar (um reflexo pintado no chão projetado abre em leque nos cantos). As luzes de
camadas curvas refletem na profundidade da sua coluna.

### Parede lateral (`"lateral"`)

`"lateral": {"x": 0, "longe": 1735, "perto": 60}` numa camada de imagem: a camada vira uma parede
do lado da arena, um plano em x fixo do mundo (paralelo ao olhar da câmera) que vai da
profundidade `longe` (onde encontra o fundo, em `x` = fração da tela de repouso) até `perto`.
A imagem é a fachada desenrolada, sem perspectiva pintada (a coluna 0 no fundo); `topo` vale na
profundidade `longe` e a base é o chão. O motor desenha em fatias de largura igual na tela (passos
iguais na escala; a profundidade sai de 1/escala), numa chamada só no OpenGL (`drawImageStrip`).
`"abre"` (mundo, padrão 0): a ponta de perto fica isso mais para fora, a parede na diagonal
(a arquibancada fazendo a volta; reta, ela converge forte e parece torta).
`"virar": true`: a imagem vem espelhada e as colunas são lidas de trás para frente (a parede da
esquerda, que na tela vai da direita para a esquerda). Desenhada depois do chão (fica em pé nele).

### Reflexo das luzes como imagem (`"reflexoImagem"` no `chao`)

Com `"reflexoComprimento"`, o reflexo de cada luz com `"reflexo": true` desce reto pelo chão.
`"reflexoImagem": "risco.png"` troca os degradês por essa imagem (já na cor, alfa macio), esticada
do pé da luz até o comprimento. Os riscos aparecem também no modo leve (fazem parte do chão); o
brilho redondo embaixo da luz, só no detalhe.
