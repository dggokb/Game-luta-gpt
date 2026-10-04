# Game Luta Astra — Prototype 01 em 3D

Protótipo Android nativo de luta. A versão `0.46-astra-prototype01-3d` integra a branch `game-luta-3D-gpt` em `c9edf2f44d7f92eece2f9bb32facea0e999fa961` à `game-luta-astra`, preservando o histórico das duas branches.

A base v0.45 fornece adversário com IA opcional, dano, defesa alta/baixa, launcher, knockdown, energia, Super, barra de poder, troca animada com cooldown e câmera de luta. A Astra mantém passos de física de 120 Hz, tratamento de pausa/multitouch e reserva de um golpe nos últimos 100 ms de recuperação. O launcher confirmado continua permitindo jump-cancel.

## Personagem padrão

O jogador usa uma malha **3D real em OpenGL ES 2**, baseada no concept Fighter Prototype 01: cabelo escuro, colete azul-marinho com painéis brancos e gola vermelha, camiseta preta, calça branca larga, faixa, luvas sem dedos e tênis. Ambos os integrantes da equipe usam esse modelo padrão; os perfis de golpes e vidas continuam separados.

São malhas procedurais low-poly, vinculadas a um esqueleto de 18 articulações, com profundidade, iluminação em três faixas e contorno. É uma primeira interpretação estilizada do concept, não um modelo esculpido de produção nem uma imagem colada no cenário. O adversário/cenário mantêm seus renderizadores existentes da branch 3D.

- Silhueta modelada por seções: ombros/cintura, músculos, calça larga e punhos estreitos.
- Rosto com olhos, sobrancelhas e nariz, cabelo com mechas assimétricas, gola, faixa, dedos e cadarços.
- Respiração parada, caminhada/dash, agachamento, salto/queda, defesa, L/M/H, 2L/2M/2H, energia/Super e reação a dano.
- Transições suavizadas, apoio dos pés no chão e orientação espelhada sem mostrar a nuca ao trocar de lado.
- Estado publicado de forma coerente para a thread OpenGL. HUD e controles ficam acima do personagem.

## Controles

| Ação | Entrada |
| --- | --- |
| Caminhar, agachar, pular | Direcional de oito vias |
| Super Jump | Baixo → cima/diagonal superior em até 360 ms |
| Dash / backdash | Dois toques para frente / trás |
| Normais | L, M, H; baixo no chão: 2L, 2M, 2H |
| Autocombo | COMBO; sequência conforme o perfil ativo |
| Energia P1 / P2 | Baixo → frente / trás → frente, confirmar com L/M/H |
| Super | SUPER quando houver barra suficiente |
| Troca | TROCA; aguardar animação e cooldown |
| Adversário | IA ON/OFF |

Frente e trás são relativos ao adversário. Energia aceita diagonais intermediárias e confirmação em até 550 ms. O direcional continua registrando o dedo durante a recuperação da energia; pousar segurando baixo atualiza o agachamento. Pausa e cancelamento limpam comandos pendentes.

## Contexto

Foram lidos os arquivos **Game Luta GPT — Implementação até v0.9** e **DBZ FighterZ — Jogabilidade Base** na pasta `game-luta-gpt` do Drive. O código e o histórico da branch 3D são a referência para os avanços posteriores. O concept enviado pelo usuário define o visual do Prototype 01.

## Build e validação

JDK 17, Gradle 8.7, Android SDK 35. Não há Gradle Wrapper.

```bash
cd android
gradle testDebugUnitTest assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Aplicativo **Game Luta Astra 3D**, pacote `com.gamelutagpt.astra`, Android 7.0+ e OpenGL ES 2. O identificador separado permite instalar ao lado da outra branch. É um APK de debug para teste, não uma versão de loja.

Testes Robolectric exercitam o GameView real com MotionEvent, taxas de renderização diferentes, multitouch, golpes, recuperação, energia no ar, troca, launcher, dano e pausa. A malha exportada pelos testes usa os mesmos vértices, matrizes e shaders de produção. Para revisar as poses com Mesa EGL:

```bash
python -m pip install moderngl pillow numpy
python tools/render_mesh_review.py
```

Saídas em `android/app/build/astra-previews/`; relatórios em `android/app/build/reports/tests/`. O workflow executa testes antes do APK. A revisão em EGL valida a geometria e o shader; fluidez, composição das superfícies e ergonomia ainda precisam ser verificadas num Android físico.
