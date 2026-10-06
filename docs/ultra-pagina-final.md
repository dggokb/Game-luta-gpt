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
   - `onUltraFinished(nota)`: o `GameView` chama `engine.finishUltra` e a luta continua.
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
| 1,22 → 1,34 | Painel `golpe`; impacto 1: 25% do dano, quadro invertido, hitstop 0,07 s |
| 1,80 → 1,88 | Painel `atingido`; impacto 2: 25% do dano |
| 2,38 | Painel `final` se expande até cobrir a tela (0,20 s) |
| 2,50 → 3,18 | O anel fecha até o alvo (`TARGET_AT`) |
| toque | Detonação: 50% do dano + bônus (PERFEITO ±0,075 s = +25%, BOM ±0,17 s = +10%) |
| detonação + 0,34 | A página quebra em 48 cacos |
| + 0,62 | Fim: `onUltraFinished` |

Um toque cedo demais (antes de `TARGET_AT − 0,17`) marca ERROU, sem bônus, e a detonação
acontece no tempo normal. Sem toque, a detonação acontece no fim da janela BOM.

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
| Mudar a divisão do dano | `PaginaFinal.HIT_FRACTION` |
| Mudar o layout dos painéis | `PaginaFinal.PANELS` (polígonos em coordenadas de tela) |
| Mudar a arte provisória | `ArteProvisoria` |
| Testar | `gradle :ultra-core:test`; visual em `gradle :pc-preview:run` |
