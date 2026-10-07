# Ultra "Página Final": funcionamento interno — v0.75

As regras do ultra (custo, investida, defesa, escala de dano) ficam no `CombatEngine`; veja
`docs/combat-engine.md`. Este documento descreve a **cinemática**: como a página de mangá é
montada, como o jogo conversa com ela e como portá-la para outra plataforma. Para criar a
arte de um ultra, veja `tools/ultra/README.md`.

## Módulos

| Módulo | Conteúdo | Depende de |
| --- | --- | --- |
| `android/render-core` | `RenderCanvas` (pincel), `RenderImage`, `RenderAssets`, `MiniJson`, `Rand` | nada |
| `android/ultra-core` | `PaginaFinal`, `UltraPack`, `UltraDefinition`, `ArteProvisoria`, `UltraListener` | render-core |
| `android/app` | `AndroidRenderCanvas`, `AndroidRenderAssets`, `UltraSounds`, ligação no `GameView` | os dois |
| `android/pc-preview` | `Java2DRenderCanvas`, `FileRenderAssets`, `UltraPreview` | os dois |

`render-core` e `ultra-core` são Java 8 puro, sem Android. Portar para outra plataforma é
implementar `RenderCanvas`, `RenderImage` e `RenderAssets` (o PC já tem a versão Java2D).

## Fluxo com o jogo

1. O motor de combate confirma o acerto (`CombatEngine.ultraConnected()`).
2. `GameView.beginUltraCinematic` chama `PaginaFinal.start(pack, facing)`. A partir daí, o
   `GameView` deixa de dar passos no motor de combate e só chama `PaginaFinal.update` (60 Hz).
3. Cada toque na tela vira `PaginaFinal.tap()`.
4. A `PaginaFinal` avisa o jogo pela `UltraListener`:
   - `onUltraHit(índice, fração)`: o `GameView` chama `engine.applyUltraHit` com
     `fração × dano do ultra.json`;
   - `onUltraSound(id)`: o som é tocado pelo `UltraSounds`;
   - `onUltraFinished(nota)`: o `GameView` chama `engine.startUltraBeam` com
     `PaginaFinal.beamFraction(nota) × dano` e a luta volta com o **raio final** (abaixo).
5. `PaginaFinal.render` desenha por cima de tudo, em coordenadas de tela 1280×720. Enquanto
   a página cobre a tela (antes de quebrar), o HUD não é desenhado.

Tudo roda na thread do jogo; a `PaginaFinal` não é thread-safe.

## Linha do tempo (segundos do relógio da cinemática)

O relógio para durante as pausas de impacto (hitstop), então os tempos abaixo não contam
essas pausas.

| Tempo | Evento |
| --- | --- |
| 0,00–0,16 | Papel da página aparece (`PAGE_FADE`) |
| 0,10 | Painel `olhos` bate na página (`SLAM_AT[0]`) |
| 0,26 | Faixa com o nome do golpe entra (`NAME_AT`) |
| 0,66 | Painel `carga` |
| 1,22 → 1,34 | Painel `golpe`; impacto 1: 15% do dano, quadro invertido, hitstop 0,07 s |
| 1,80 → 1,88 | Painel `atingido`; impacto 2: 15% do dano |
| 2,38 | Painel `final` se expande até cobrir a tela (0,20 s) |
| 2,50 → 3,18 | O anel fecha até o alvo (`TARGET_AT`) |
| toque | Detonação: 20% do dano + bônus (PERFEITO ±0,075 s = +25%, BOM ±0,17 s = +10%) |
| detonação + 0,34 | A página quebra em 48 cacos; por trás, os lutadores já se afastam para o raio |
| + 0,62 | Fim: `onUltraFinished`; o raio final dá os outros 50% do dano (com o mesmo bônus) |

Um toque cedo demais (antes de `TARGET_AT − 0,17`) marca ERROU, sem bônus, e a detonação
acontece no tempo normal. Sem toque, a detonação acontece no fim da janela BOM.

## Raio final

Quando a página quebra, a luta volta com o atacante disparando o raio amarelo, no estilo
dos hipers de Marvel vs Capcom: o feixe atravessa a tela, acerta várias vezes seguidas e
o contador de hits sobe a cada acerto, até a explosão final arremessar o oponente.

**Regra (motor, `CombatEngine`)**: fase `ULTRA_BEAM`, valores em `CombatConfig.ultraBeam*`.

| Frame da fase | O que acontece |
| --- | --- |
| 0 | `startUltraBeam(atacante, dano, hits)`: os lutadores ficam a `ultraBeamDistance` (520) um do outro; o defensor fica preso em hitstun, sem cair |
| 0 → 8 | O feixe vai das mãos até o alvo |
| 8, 12, 16... | Um acerto pequeno a cada 4 frames (`hits` do `ultra.json`, padrão 20): dano, +1 no combo, o defensor recua 7 px |
| último + 6 | Explosão: o resto do dano (30% do raio), hitstop de 12 e o arremesso de sempre (cai derrubado) |
| + 18 | O feixe some e o atacante fica livre |

O dano do raio usa a escala fixada no acerto do ultra e é dividido sem sobra (a soma dos
acertos é exatamente o dano do raio). O atacante não pode ser atingido durante o raio.

**Desenho (`RaioFinal`, ultra-core)**: só desenha, no mundo da luta, a partir do estado do
motor: a esfera nas mãos (`inicio`), o feixe repetido correndo para a frente (`corpo`), a
frente do feixe (`ponta`), a explosão a cada acerto e a grande explosão final (`impacto`).
Sem imagens, desenha um raio com as cores do ultra. O `GameView` ainda escurece o cenário,
treme a tela a cada acerto e dá um clarão branco na explosão. O contador de hits do HUD
pula a cada acerto novo.

As mãos ficam no punho esticado do soco forte (medido no sprite: 0,68 e 0,67 da altura do
personagem à frente e acima da base), e o lutador fica nessa pose enquanto dispara.

## Camadas do desenho

Para cada quadro, em ordem:

1. **Papel**: retângulo cor de papel, com fade de entrada.
2. **Painéis 1 a 4**: polígono inclinado (`PANELS`), com sombra, recorte (`clipPolygon`),
   conteúdo (imagem em modo "cobrir" ou `ArteProvisoria`), efeitos por cima
   (`ArteProvisoria.drawOverlay`: partículas, linhas de velocidade, estouro) e borda de
   tinta. Ao entrar, cada painel cresce 30% e gira 6° até pousar, com um clarão branco.
3. **Movimento de câmera**: dentro de cada painel o conteúdo dá um zoom lento (3% → 12%).
   Quando o lutador olha para a esquerda, o conteúdo é espelhado; os textos não.
4. **Faixa com o nome** e **onomatopeias**: entram com escala 1,7 → 1 e tremem um pouco.
   A posição vem de `SFX_SPOT` ou, quando existe, de `posicaoOnomatopeia` no `ultra.json`.
5. **Painel final + anel + rachaduras**.
6. **Cacos** (depois da detonação): cada caco recorta o conteúdo do painel final em versão
   leve (`ArteProvisoria.lite`, sem linhas de velocidade), voando com gravidade e giro.
7. **Inversão de cores** (`beginInvert/endInvert`) nos impactos e **flash branco** na
   detonação, por cima de tudo.

O tremor da tela é um `translate` aleatório que decai 70 px/s.

## Onde mexer

| Quero... | Onde |
| --- | --- |
| Mudar o ritmo | Constantes no topo de `PaginaFinal` (`SLAM_AT`, `TARGET_AT`...) |
| Mudar a divisão do dano | `PaginaFinal.HIT_FRACTION` e `PaginaFinal.BEAM_FRACTION` |
| Mudar o ritmo do raio | `CombatConfig.ultraBeam*`; quantidade de hits e espessura no `ultra.json` |
| Ver o raio no PC | `gradle :pc-preview:runRaio` (grava quadros em PNG) |
| Mudar o layout dos painéis | `PaginaFinal.PANELS` (polígonos em coordenadas de tela) |
| Mudar a arte provisória | `ArteProvisoria` |
| Testar | `gradle :ultra-core:test`; visual em `gradle :pc-preview:run` |
