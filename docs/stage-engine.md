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
   5. camadas entre o fim do piso e os lutadores (grade, pilares, braseiros);
   6. respingos, chuva e pétalas atrás dos lutadores.

   `drawBackground(canvas, z, desenho)` encaixa mais um desenho na profundidade `z`: depois
   das camadas mais fundas que ele e antes das mais rasas. É assim que o narrador fica no
   palco (`z = 300`, na frente das paredes e dos prédios, atrás dos pilares, das lanternas e
   dos objetos do chão; ver [historia/narrador.md](historia/narrador.md)). `screenX`, `groundScreenY`,
   `scale` e `worldX` projetam um ponto naquela profundidade.
3. No mundo da luta: reflexo dos lutadores (espelhados no chão, com transparência), os
   lutadores, projéteis e efeitos.
4. `drawForeground`: camadas com `z ≤ 0` e respingos, chuva e pétalas na frente.
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

**Chão até o fundo** (`"ate"` no `chao`): atrás da borda de cima da imagem do piso, a imagem
lisa (`imagemLajotas`) se repete na profundidade até `ate`, na mesma escala de lajota. Para o
chão de uma arena ir do ringue até a arquibancada lá no fundo.

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

## Efeitos do jogo

- **Reflexo dos lutadores**: `chao.reflexoLutadores` (0 a 1). O `GameView` desenha cada
  lutador espelhado em torno da linha do chão com essa opacidade (`saveLayerAlpha`).
- **Pétalas**: partículas em 3D (x, y, z) caindo com vento. As de longe são pequenas e
  apagadas, as da frente (`z < 0`) passam grandes na frente dos lutadores.
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
