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
   6. pétalas atrás dos lutadores.
3. No mundo da luta: reflexo dos lutadores (espelhados no chão, com transparência), os
   lutadores, projéteis e efeitos.
4. `drawForeground`: camadas com `z ≤ 0` e pétalas na frente.
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

Camadas de imagem podem ter **luzes** (brilho que pulsa, com reflexo no piso) e **fogos**
(chama em três camadas tremulando, brilho e brasas), presos a pontos da imagem; eles se
repetem com as cópias da camada.

## Efeitos do jogo

- **Reflexo dos lutadores**: `chao.reflexoLutadores` (0 a 1). O `GameView` desenha cada
  lutador espelhado em torno da linha do chão com essa opacidade (`saveLayerAlpha`).
- **Pétalas**: partículas em 3D (x, y, z) caindo com vento. As de longe são pequenas e
  apagadas, as da frente (`z < 0`) passam grandes na frente dos lutadores.

## Desempenho

O `GameView` agora pede o Canvas pela GPU (`lockHardwareCanvas`, Android 8+) e volta para
o Canvas por software se não conseguir. Várias imagens em tela cheia por quadro pesam
demais por software em telas grandes. A GPU também deixa a cinemática do ultra mais leve.

Ainda não medido em aparelho. Se precisar aliviar: aumentar `FLOOR_STRIP` (faixas do piso),
reduzir pétalas e brilhos da água, ou usar imagens menores.

## Testes

`gradle :stage-core:test` (`StageSceneTest`): o plano dos lutadores bate com o desenho do
jogo, o fundo anda e cresce menos, a profundidade do piso é invertida corretamente, a
validação do stage.json, e o desenho completo com e sem imagens em várias câmeras sem
deixar o Canvas desbalanceado.
