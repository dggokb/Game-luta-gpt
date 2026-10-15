# Motor de exportação para PC

O jogo de PC não é uma segunda versão do jogo: é o **mesmo código Java do app Android**
(`android/app/src/main/java`), compilado pelo módulo `android/pc` sobre uma camada que
implementa, em Java2D, a parte do Android que o jogo usa. O `GameView`, o motor de combate,
o HUD, a seleção, os menus e o modo história rodam sem nenhuma mudança.

```
android/app/src/main/java  ─┐   (MainActivity e substituições ficam de fora)
android/pc/src/substituicoes ┼─► :pc ─► jar ─► jpackage ─► GameLutaGPT(.exe/.app)
android/pc/src/main/java    ─┘
   android.*            camada de compatibilidade (Canvas, Paint, Bitmap, Context...)
   com.gamelutagpt.pc   janela, teclado/mouse, controle (SDL), dados, captura
```

## Peças

| Onde | O quê |
|---|---|
| `pc/src/main/java/android/graphics` | `Canvas` sobre `Graphics2D`: transformações, recorte, `saveLayer`, texto (com contorno, negrito falso, inclinação, alinhamento), filtros de cor (`PorterDuffColorFilter`, `ColorMatrixColorFilter`), degradês, `Matrix` com perspectiva (`setPolyToPoly`), tracejado. |
| `pc/src/main/java/android/content` | `Context`, `AssetManager` (pastas no disco), `Resources` (drawables = PNGs de `res/drawable-nodpi`), `SharedPreferences` (arquivo `.properties`). |
| `pc/src/main/java/android/view` | `SurfaceView`/`SurfaceHolder` ligados à janela, `MotionEvent` (mouse = dedo 0), `KeyEvent` com os códigos do Android. |
| `pc/src/main/java/android/media` | `SoundPool` com `javax.sound` (WAV; OGG/MP3 são avisados e ignorados). |
| `pc/src/substituicoes/java` | Classes do app com versão própria no PC (mesmo nome e pacote). Hoje: `ArquivosCompartilhados` (no Android, o menu Compartilhar; no PC, salva em `Documentos/GameLutaGPT` e abre a pasta). A do app sai do build de PC automaticamente. |
| `pc/src/main/java/com/gamelutagpt/pc` | `JogoPc` (janela 16:9 com faixas, tela cheia F11/Alt+Enter, "Carregando…"), `Teclas` (AWT → Android), `Controles` (SDL/Jamepad → botões do Android), `DadosDoJogo` (onde estão os arquivos), `QuadroPc`, `CapturaPc`. |
| `app/.../HardwareInput.java` | Mapa de teclado/controle **compartilhado**: o `GameView` entende teclas físicas na luta e nos menus, no PC e no Android (teclado/controle Bluetooth). |

## Tela cheia, controle na tela e carregamento

- **Tela cheia**: F11 ou Alt+Enter, pegos antes de qualquer componente (no Windows o Alt
  mexia no menu da janela). Usa a tela cheia do sistema (`GraphicsDevice.setFullScreenWindow`)
  e lembra a escolha (`pc_janela.properties` nas preferências).
- **Controle virtual**: o PC chama `GameView.setTouchControlsVisible(false)`: o D-pad e os
  botões de golpe não são desenhados nem respondem ao mouse. Botões de teste e SELEÇÃO
  continuam.
- **Carregamento**: `TelaCarregando` (no app) aparece no APK e no PC ao abrir o jogo e sempre
  que o `GameView` carrega arte pesada (duplas novas, lutas da história): o trabalho roda numa
  thread à parte e o loop só desenha a tela, com o anel girando.

## Desempenho

O Java2D por software é lento em dois pontos que o jogo usa muito; a camada resolve sem
mudar o resultado na tela:

- **Imagem escalada** (cenário com várias camadas, sprites): `Resampler` escala uma vez para
  o tamanho exato em pixels, em paralelo, e guarda (cache por tamanho). Enquanto o zoom da
  câmera não muda, o quadro só copia pixels. Imagens opacas viram cópia direta; margens
  transparentes são recortadas.
- **Mistura alfa**: `Blender` mistura as imagens grandes em todos os núcleos.
- **Texto**: cada texto (com contorno) é rasterizado uma vez e reaproveitado.
- **Redução forte** (retratos grandes em caixas pequenas): mipmaps.
- **Carregamento**: ao pedir o primeiro atlas de um personagem, os outros dele são
  decodificados em paralelo (`Resources`).

Medido sem tela num container de 4 núcleos (mais lento que um PC comum), 1280×720: ~21 ms de mediana por quadro na luta (era ~60 ms antes dessas otimizações)
(`gradle :pc:capturar` grava `tempos.txt`). PCs mais fracos: `-Dgameluta.resolucao=1280x720`
(o padrão desenha até 1920 de largura e amplia).

Chaves para comparar com o caminho puro do Java2D: `-Dgameluta.escalaJava2d=true`,
`-Dgameluta.misturaJava2d=true`, `-Dgameluta.textoSemCache=true`, `-Dgameluta.semPrecarga=true`.

## Quando o app usar algo novo do Android

O `gradle :pc:compileJava` acusa o que falta. Duas saídas:

1. É desenho/arquivo/entrada comum → implementar na camada (`pc/src/main/java/android/...`),
   com a mesma assinatura do Android.
2. É coisa só do Android (Intent, ContentProvider, permissões...) → criar a versão de PC da
   classe do app em `pc/src/substituicoes/java/com/gamelutagpt/` (mesmo nome e métodos que o
   jogo chama).

## Dados do jogo exportado

`data/assets` (= `app/src/main/assets` + `selection/portraits` de `art/concepts`) e
`data/drawable` (= `res/drawable-nodpi`), ao lado dos jars. Em desenvolvimento
(`gradle :pc:run`) tudo é lido direto do repositório. Também dá para apontar
`-Dgameluta.dados=<pasta data>`.

## Limitações conhecidas

- Som: só WAV no PC (os ultras ainda não têm sons).
- macOS: o `.app` não é assinado (primeira vez: botão direito > Abrir).
- Teclas apertadas durante o "Carregando…" são ignoradas.
- O build de PC precisa de Python 3 + Pillow, como o do app (calibração do P01 gerada no build).
