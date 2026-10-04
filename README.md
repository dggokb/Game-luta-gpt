# Game Luta Sprite Astra

Branch `game-luta-sprite-astra`, criada diretamente de `game-luta-sprite-gpt` em `3b60bb182b31f91e95364d25490c3c7be3a7866f`. Versão Android nativa Java/Canvas `0.43-sprite-astra-responsive`.

## Correção dos movimentos

A folha anterior repetia quase a mesma pose nos oito quadros de caminhada. O código também mostrava idle durante recuo, agachamento, salto e dash. Esta versão usa uma nova folha de 16 poses com o personagem preto/vermelho já presente na branch:

- 4 quadros de passos para frente e 4 de recuo;
- entrada de agachamento e agachamento sustentado;
- subida e descida do salto;
- 2 poses de dash, uma de backdash e compressão do pouso.

`SpriteMotion` escolhe as animações a partir da física real. O ciclo de passos avança com a distância percorrida; parado contra um limite não fica andando no lugar. O relógio do idle vem da simulação e pausa junto com o jogo. Recorte, escala e pivô são definidos por quadro; o agachamento mantém a escala do corpo, em vez de ser esticado até a altura de um personagem em pé.

A folha de movimento foi empacotada em 16 células de 256×256 já na escala de jogo. O Canvas não precisa mais redimensionar recortes grandes da imagem original a cada quadro. São dois bitmaps decodificados uma vez, sem alocar bitmaps durante o jogo. O idle original foi preservado byte a byte e movido de Base64 Java para um recurso WebP.

Ataques, defesa e Super continuam usando as poses de suporte existentes, sem sprites específicos de golpes. Da branch `game-luta-3D-gpt` foi portada a regra da barra de Super: golpe no vazio não carrega, acerto confirmado carrega, projétil carrega quando acerta e a defesa também ganha barra.

## Build e testes

Requer JDK 17, Gradle 8.7 e SDK Android 35:

```bash
cd android
gradle testDebugUnitTest assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Aplicativo **Game Luta Sprite Astra**, pacote `com.gamelutagpt.spriteastra`, Android 7.0+. Pode ser instalado ao lado da outra branch.

Os testes verificam os estados e seu tempo, ciclos com diferentes taxas de atualização, os comandos de movimento no GameView, salto/pouso, troca de orientação, pausa e decodificação dos recursos empacotados. A revisão em Canvas Android nativo gera `android/app/build/sprite-review/movement-frames.png` e `gameplay.png`. O workflow também instala e abre o APK em um emulador Android 34 e exercita retorno do segundo plano.

O loop usa passos fixos de 120 Hz e alvo de desenho de 60 FPS. Os eventos de toque entram numa fila sem bloquear a interface; a simulação consome a fila antes do próximo passo e compacta eventos consecutivos de arraste. Assim, o toque deixa de esperar o `Canvas` terminar de desenhar e a latência fica limitada ao próximo passo da simulação.

## Arte

Imagem criada com a ferramenta integrada de geração de imagens, usando as folhas originais como referência de identidade. Fonte final: `android/app/src/main/res/drawable-nodpi/movement_astra.png`. Especificação/prompt em `docs/movement-sprites.md`.
