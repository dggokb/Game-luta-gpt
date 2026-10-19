# Motor de cenário com falso 3D — v0.76

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
- Hoje: os pilares do Templo da Lua e do Dojo Yinlu.

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
  (`setDetalhe(false)`) não chove, como as pétalas. Primeiro uso: Beco do Brucy.

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

Ainda não medido em aparelho. Se precisar aliviar: aumentar `FLOOR_STRIP` (faixas do piso),
reduzir pétalas e brilhos da água, ou usar imagens menores.

## Testes

`gradle :stage-core:test` (`StageSceneTest`): o plano dos lutadores bate com o desenho do
jogo, o fundo anda e cresce menos, a profundidade do piso é invertida corretamente, a
validação do stage.json, o desenho completo com e sem imagens em várias câmeras sem
deixar o Canvas desbalanceado e a bandeira presa em cima balançando embaixo.
