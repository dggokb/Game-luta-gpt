# Game Luta GPT — protótipo Android nativo

Protótipo incremental de jogo de luta, com `SurfaceView`, loop próprio e desenho em `Canvas` Android. A base atual tem dois lutadores genéricos, caminhada, agachamento, pulo/Super Jump, dash/backdash, L/M/H em pé e agachado, ataques aéreos, autocombo por personagem, projéteis de energia, troca e vida individual. Não há adversário, colisão de golpes, dano de combate ou sistemas avançados implementados.

## Branch game-luta-astra

Parte da v0.18 (`44269c632faa2304e40820520ba1f5b061161857`). O escopo é refinar o que já existe, sem adicionar mecânicas de combate, personagens ou botões. Versão: `0.18.1-astra`.

- Física com passos fixos de 120 Hz e renderização com alvo de 60 FPS. Quedas moderadas de FPS não alteram a velocidade da luta; pausas acima de 100 ms têm recuperação limitada para evitar saltos de posição.
- Golpes preservam a duração original (L 160 ms, M 260 ms, H 400 ms, energia 300 ms). Um toque nos últimos 100 ms pode reservar apenas o próximo golpe; toques antecipados não cortam o golpe atual e não avançam o autocombo.
- Animação com preparação, extensão e recuperação, braços/pernas articulados, poses de subida/queda, transição de agachamento e amortecimento visual ao pousar.
- Ataques normais terrestres travam deslocamento/pulo até recuperar; normais aéreos preservam direção e gravidade. O poder aéreo mantém o travamento horizontal, a redução inicial da velocidade vertical e a gravidade reduzida da v0.18.
- O direcional acompanha o dedo durante a energia para retomar a direção correta quando a animação acabar.
- Super Jump considera a saída de baixo, inclusive após segurar agachado; pousar segurando baixo atualiza a postura sem exigir outro evento de movimento.
- Troca preserva posição, altura, velocidade vertical e vida individual. Cancela ataque, dash e entrada de ataque reservada. Continua havendo no máximo um projétil por personagem.
- Estado de jogo protegido entre touch e renderização; pausa/cancelamento limpam entradas pendentes. Recursos de cenário são reutilizados por frame.

## Comandos existentes

| Ação | Entrada |
| --- | --- |
| Caminhar / agachar / pular | Direcional de oito vias |
| Super Jump | Baixo, depois cima ou diagonal superior em até 360 ms |
| Dash | Dois toques para a direita em até 300 ms; segurar o segundo |
| Backdash | Dois toques para a esquerda em até 300 ms |
| Ataques | L, M, H; com baixo no chão executam 2L, 2M, 2H |
| Autocombo | COMBO: P1 L → M → H; P2 L → L → H → M |
| Poder P1 | Baixo → direita, depois L/M/H |
| Poder P2 | Esquerda → direita, depois L/M/H |
| Troca | TROCA |

Os poderes aceitam diagonais intermediárias e confirmação em até 550 ms. L/M/H preservam os multiplicadores existentes de velocidade/dano. O personagem ainda olha para a direita, como na base; não existe adversário para definir orientação relativa.

## Fontes de contexto

Lidos os dois arquivos da pasta `game-luta-gpt` no Google Drive:

- **Game Luta GPT — Implementação até v0.9**: regras da base incremental.
- **DBZ FighterZ — Jogabilidade Base**: referência e escopo futuro, não uma lista de funcionalidades já disponíveis.

As implementações posteriores à v0.9 foram verificadas no código e no histórico da v0.18.

## Build e testes

Requer JDK 17, Gradle 8.7 e Android SDK 35. Não há Gradle Wrapper neste repositório.

```bash
cd android
gradle testDebugUnitTest assembleDebug
```

- APK: `android/app/build/outputs/apk/debug/app-debug.apk`
- Relatório: `android/app/build/reports/tests/testDebugUnitTest/index.html`
- Prancha de poses desenhada pelo Canvas Android: `android/app/build/astra-previews/moves.png`

Os testes Robolectric exercitam o `GameView` real com eventos `MotionEvent`, inclusive multitouch, e avanço determinístico da simulação. Cobrem taxas de renderização diferentes, ataque terrestre/aéreo, recuperação, energia, agachamento, troca, cancelamento, limites e renderização. Eles não substituem avaliar latência, fluidez e ergonomia num celular físico.

O workflow de APK roda em `main` e `game-luta-astra`, executa os testes antes do build e disponibiliza APK, relatório e prancha como artefatos.

## Conferência no celular

1. Caminhar/dash/backdash; soltar o dedo; testar os cantos do cenário.
2. Segurar baixo por um segundo, deslizar rapidamente para cima; pousar mantendo baixo/diagonal inferior.
3. L/M/H no chão e no ar; tocar rapidamente durante o começo e o fim do golpe; observar a recuperação e o próximo golpe.
4. Usar COMBO com os dois personagens, em pé e agachado; alternar com ataque manual e TROCA.
5. Soltar poder no Super Jump, mudar a direção enquanto a animação trava X e confirmar a retomada correta. Tentar outro poder enquanto o primeiro ainda existe.
6. Alternar de aplicativo com o direcional pressionado; retornar e verificar que não há movimento preso ou salto de simulação.
