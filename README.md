# Game Luta Sprite GPT

Branch `game-luta-sprite-gpt`, incorporando as melhorias desenvolvidas em `game-luta-sprite-astra`. Versão Android nativa Java/Canvas `0.45-sprite-gpt-scale-standard`.

## Correção dos movimentos

A folha anterior repetia quase a mesma pose nos oito quadros de caminhada. O código também mostrava idle durante recuo, agachamento, salto e dash. Esta versão usa uma nova folha de 16 poses com o personagem preto/vermelho já presente na branch:

- 4 quadros de passos para frente e 4 de recuo;
- entrada de agachamento e agachamento sustentado;
- subida e descida do salto;
- 2 poses de dash, uma de backdash e compressão do pouso.

`SpriteMotion` escolhe as animações a partir da física real. O ciclo de passos avança com a distância percorrida; parado contra um limite não fica andando no lugar. O relógio do idle vem da simulação e pausa junto com o jogo. Recorte, escala e pivô são definidos por quadro; o agachamento mantém a escala do corpo, em vez de ser esticado até a altura de um personagem em pé.

Os sprites agora usam uma escala anatômica canônica no renderer. O Walk Forward é a referência (~210 px de altura visual); Idle, recuo/demais movimentos e Jab recebem fatores de escala calibrados sem reamostrar os arquivos-fonte. Todos são posicionados por pivot de pé explícito, evitando crescer/encolher e reduzindo drift vertical entre estados.

O golpe fraco em pé agora usa um jab próprio de 3 quadros (preparo, extensão e recuperação), normalizado no mesmo canvas/pivô de 256×256 dos movimentos. Os demais ataques, defesa e Super continuam usando as poses de suporte existentes. Da branch `game-luta-3D-gpt` foi portada a regra da barra de Super: golpe no vazio não carrega, acerto confirmado carrega, projétil carrega quando acerta e a defesa também ganha barra.

## Build e testes

Requer JDK 17, Gradle 8.7 e SDK Android 35:

```bash
cd android
gradle testDebugUnitTest assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Aplicativo **Game Luta Sprite GPT**, pacote `com.gamelutagpt`, Android 7.0+.

Os testes verificam os estados e seu tempo, ciclos com diferentes taxas de atualização, os comandos de movimento no GameView, salto/pouso, troca de orientação, pausa e decodificação dos recursos empacotados. A revisão em Canvas Android nativo gera `android/app/build/sprite-review/movement-frames.png` e `gameplay.png`. O workflow também instala e abre o APK em um emulador Android 34 e exercita retorno do segundo plano.

O loop usa passos fixos de 120 Hz e alvo de desenho de 60 FPS. Os eventos de toque entram numa fila sem bloquear a interface; a simulação consome a fila antes do próximo passo e compacta eventos consecutivos de arraste. Assim, o toque deixa de esperar o `Canvas` terminar de desenhar e a latência fica limitada ao próximo passo da simulação.

## Arte

Imagem criada com a ferramenta integrada de geração de imagens, usando as folhas originais como referência de identidade. Fonte final: `android/app/src/main/res/drawable-nodpi/movement_astra.png`. Especificação/prompt em `docs/movement-sprites.md`.
