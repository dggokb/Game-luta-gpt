# Queda de FPS: análise completa e soluções

Análise do código na branch `game-luta-sprite-gpt` até o commit `88b2763` (desenho novo: a luta inteira em OpenGL pelo `CanvasGl`) para achar o que derruba o FPS no celular e o que fazer, em ordem de ganho por esforço. Nada aqui foi medido no aparelho: cada causa vem com o jeito de confirmá-la no celular (seção 4). Os números de memória e de imagem foram medidos nos arquivos do repositório.

## 1. Resumo

O jogo já tem a base certa: simulação em passo fixo de 60 Hz (nunca pesa: pior passo 4,6 ms no T26), a luta inteira em OpenGL ES 3 gravada e enviada de uma vez por quadro, folhas na placa de vídeo, níveis de qualidade com teto de 1080 linhas e um perfilador por partes (`Quadros`) com contador na tela. As quedas que sobram têm seis causas, nesta ordem de peso:

| # | Causa | O que o jogador vê | Onde está |
|---|---|---|---|
| 1 | **Memória das folhas dos lutadores.** O elenco inteiro decodificado ocupa 5,4 GB; um personagem pesado 550–630 MB; uma 2x2 entre 1,2 e 2,4 GB. Cenário e Ultra ficam em dobro (Bitmap na placa de vídeo + textura do OpenGL). | Engasgos crescentes ao longo da luta, queda depois de alguns minutos, o jogo fechar sozinho na 2x2, carregamento longo | `SpriteAtlasCache`, `AndroidRenderAssets`, `GlRenderCanvas.textura` |
| 2 | **Taxa de preenchimento**: 11–14 camadas de cenário cobrindo a tela 6–10 vezes por quadro, um shader único com laço de até 32 planos de recorte e desvios para tudo, e agora **4× de antisserrilhado (MSAA) na tela inteira** quando o aparelho tem. | Queda nos cenários cheios (Shinshinkan, Porto), piora com zoom afastado, piora ao ligar o OpenGL em aparelho fraco | `StageScene`, `GlRenderCanvas.FRAGMENTOS`, `DesenhoGl.preparar` |
| 3 | **Trabalho por desenho no `CanvasGl`**: centenas de desenhos por quadro, cada um com duas chamadas nativas de matriz, arrays novos por forma, `Path.approximate` e `getFillPath` por caminho, uma `String` de chave por texto. | "Pior quadro" muito acima da média (pausas do GC), CPU alta na thread do jogo com HUD cheio | `CanvasGl` |
| 4 | **Duas superfícies**: a de cima (Canvas, translúcida) continua existindo e sendo composta pelo sistema a cada quadro, mesmo limpa; a de baixo tem canal alfa. Abaixo do ALTO as duas são escaladas. | FPS mais baixo do que o tempo do quadro explica, em aparelhos cujo compositor cai para a GPU | `MainActivity`, `DesenhoGl` |
| 5 | **Ritmo do quadro**: `Thread.sleep` depois de um swap que já espera o vsync; o passo da simulação dobra quando o quadro passa de 16,7 ms. | Tremida periódica mesmo com o contador em 58–60 FPS | `GameView.loop`, `advanceSimulation` |
| 6 | **Engasgos pontuais**: pack do Ultra (27–43 MB) copiado da placa e subido no primeiro quadro do raio; decodificação de Ultras em segundo plano durante a luta sem prioridade baixa de verdade; Android 7 sem Canvas pela GPU. | Um quadro de 50–150 ms no começo do raio; queda quando a barra enche; 10–20 FPS no Android 7 | `GlRenderCanvas.textura`, `GameView.anteciparUltras`, `lockFrameCanvas` |

A recomendação é atacar 1 primeiro (mudanças pequenas, ganho grande e imediato), depois 2 e 3 (MSAA desligado, shader simples, `CanvasGl` sem lixo), e 4–6 como fechamento do desenho novo.

## 2. Como o quadro é desenhado hoje

- **Thread do jogo** (`GameLoop`, `GameView.loop`): a cada volta, `advanceSimulation` (passo fixo 1/60 com acumulador), `drawFrame`, e `Thread.sleep` do que sobra até 16,67 ms.
- **Duas superfícies** (`MainActivity.onCreate`): um `SurfaceView` embaixo para o OpenGL (`DesenhoGl`, formato `RGBA_8888`, `eglSwapInterval 1`, stencil de 8 bits e MSAA 4× se houver) e o `GameView` por cima (`setZOrderMediaOverlay`, formato `TRANSLUCENT`, Canvas pela GPU via `lockHardwareCanvas`). **Na luta**, `drawFrame` desenha o quadro inteiro (cenário, lutadores, efeitos, HUD, botões, contador) no `CanvasGl`, um `Canvas` do Android cujas operações viram triângulos no `GlRenderCanvas`; a superfície de cima é limpa uma vez e não recebe mais nada. **Fora da luta** (menus, seleção, páginas, carregamento), tudo vai pelo Canvas de cima e a de baixo fica preta.
- **OpenGL** (`GlRenderCanvas`): os vértices do quadro inteiro vão para um `float[]` (até 196 mil vértices, 7 MB), cada troca de estado fecha um comando (textura, filtro, inversão, recorte, stencil), e no fim do quadro um único `glBufferData` sobe tudo e os comandos são desenhados em sequência. Textos ficam numa textura de 2048×2048, degradês numa textura de linhas. Formas côncavas passam pelo stencil.
- **Folhas** (`SpriteAtlasCache`): WebP em `drawable-nodpi`, decodificadas em paralelo na escala que a tela pede (1 em 1080p+, 3/4 em 720p e nos níveis MÉDIO/LEVE em 1080p), subidas ao OpenGL atrás da tela de carregamento (`DesenhoGl.subir`, 24 ms por quadro; depois `aquecer` desenha o cenário uma vez escondido) e o bitmap da memória comum é solto. Antecipação em segundo plano com orçamento de 35% da RAM (384 MB a 2,5 GB).
- **Qualidade** (`QualidadeGrafica`): AUTO desce um nível com 2 janelas de 2 s abaixo de 50 fps e sobe depois de ~30 s acima de 56. ALTO = teto de 1080 linhas; MÉDIO 0,8; LEVE 0,6 e sem enfeites. O buffer das duas superfícies fica menor (`setFixedSize`) e o sistema amplia.

## 3. Diagnóstico por causa

### 3.1 Memória das folhas (a causa principal)

**Medido nos arquivos.** 657 folhas WebP, 1,35 Gpx no total: **5,4 GB** se tudo fosse decodificado em RGBA. Por personagem, na escala 1 (tela 1080p, nível ALTO), separando o que a luta usa o tempo todo ("base") do que aparece em momentos certos (ULTRA_SEQ, VICTORY, TAUNT, INTRO, SUPER_*):

| personagem | base (MB) | ocasional (MB) | total (MB) |
|---|---|---|---|
| p13 | 592 | 34 | 625 |
| p14 | 558 | 67 | 625 |
| p15 | 553 | 54 | 608 |
| p16 | 453 | 94 | 546 |
| p04 | 318 | 99 | 417 |
| player_base (Ryūga) | 322 | 82 | 404 |
| p03 (Kōga) | 301 | 92 | 393 |
| p05 | 272 | 83 | 355 |
| p02 | 266 | 74 | 340 |
| koga_8 | 212 | 11 | 224 |
| p07 | 120 | 40 | 160 |
| p08 | 103 | 45 | 148 |
| p11 | 104 | 38 | 142 |
| p09 | 82 | 32 | 115 |
| p06 | 76 | 21 | 97 |
| p12 | 69 | 26 | 95 |
| p10 | 68 | 24 | 92 |

Lutas típicas, só as folhas (o log `Carregamento` do PC confirma a ordem: "atlas 1460MB em 154 folhas" numa 2x2, 704 MB num espelho 1x1):

| luta | escala 1 (1080p ALTO) | escala 3/4 (720p, ou MÉDIO/LEVE em 1080p) | escala 1/2 (não existe hoje) |
|---|---|---|---|
| 1x1 Ryūga × p02 | 744 MB | 418 MB | 186 MB |
| 2x2 Ryūga+p02 × Kōga+p04 | 1554 MB | 874 MB | 389 MB |
| 2x2 p13+p14 × p15+p16 | 2404 MB | 1352 MB | 601 MB |

A isso se somam: o cenário (46–72 MB por pacote; o Dojo Shinshinkan tem 129 MB por causa dos atores em 1600×1600), **em dobro** no OpenGL (ver abaixo); o pack do Ultra (25–43 MB por personagem, também em dobro); HUD, retratos e arte da seleção (energy-frame 6 MB; retratos 6 MB cada; heróis da seleção 96 MB no total); quadros da vitória (95 MB no total); e a antecipação, que pode ocupar até 35% da RAM por cima disso.

**Por que isso vira queda de FPS, e não só memória:**

- Num aparelho de 4 GB, uma 2x2 chega perto ou passa de 2 GB de pixels só do jogo. O Android começa a comprimir e descartar páginas (zram), e o `lmkd` mata o processo quando chega no limite: é o "fechar sozinho na 2x2" do roteiro `docs/balanco/aparelho.md`. Antes de matar, a pressão de memória engasga as threads de decodificação e o driver da GPU: a luta começa bem e vai piorando.
- Texturas enormes (uma folha de 4908×3256 = 64 MB) na placa de vídeo: cada troca de animação pula para outra textura de dezenas de MB. A GPU faz isso, mas a largura de banda de memória, que no celular é compartilhada com a CPU, é o recurso mais escasso.
- Folhas maiores que a textura máxima do aparelho (`p14_special_energy` tem 5532 px de largura; aparelhos antigos aceitam no máximo 4096) passam por `Bitmap.createScaledBitmap` no `GlRenderCanvas.textura`: uma cópia inteira a mais na memória comum durante a carga e perda de nitidez.

**Memória em dobro no cenário e no Ultra.** `AndroidRenderAssets.loadImage` decodifica como `HARDWARE` sempre que `SpriteAtlasCache.telaPelaGpu` é verdadeiro, e `GameView.lockFrameCanvas` liga `telaPelaGpu` sempre que o Canvas é pela GPU, inclusive com a luta em OpenGL. Aí `GlRenderCanvas.textura` encontra um bitmap `HARDWARE`, faz `bitmap.copy(ARGB_8888)` (leitura lenta da placa para a memória comum), sobe a textura, e o bitmap `HARDWARE` original continua vivo no pacote (`soltarBitmap` não vale para `HARDWARE`). Resultado: cada imagem do cenário e do Ultra existe duas vezes na placa de vídeo, e a primeira subida custa uma cópia GPU→CPU→GPU. O `aquecer` esconde a subida do cenário atrás da tela de carregamento, mas não a do pack do Ultra (seção 3.6) e não a memória em dobro.

**As folhas já estão compactadas.** A tabela de quadros do `Atlas` (6 números por quadro) guarda a caixa justa de cada quadro, e a folha já é um empacotamento dessas caixas, não uma grade. Mesmo assim 55–77% dos pixels de cada folha grande são transparentes (medido: 23–45% de cobertura de alfa nas 12 maiores), por causa das margens das caixas e do alinhamento. Reempacotar com margem menor dá algo, mas não é a alavanca principal.

**A alavanca principal é a quantidade de quadros e a escala.** O idle do Kōga tem 83 quadros; um especial, 52; o `ULTRA_SEQ` da p14, 150. Animação a 60 quadros distintos por segundo é raro em jogo de luta (12 a 30 é o comum, segurando cada quadro 2 a 5 quadros do motor). Metade dos quadros corta a memória pela metade sem mudar o frame data do motor.

### 3.2 Taxa de preenchimento: cenário, shader e MSAA

No PC, `StageScene.drawBackground` é 40–71% do tempo de desenho (T26). Na GPU o custo cai, mas a forma é a mesma: 11–14 camadas por cenário, das quais o céu, a neblina (2–3 degradês de tela cheia), a água, o chão em faixas e o `fillRect` de fundo cobrem a tela inteira, cada uma com mistura (blend). A tela é pintada 6 a 10 vezes por quadro antes dos lutadores. Em 1080p são 12–20 Mpx por quadro só de cenário; a 60 Hz, 0,7–1,2 Gpx/s, que é a ordem da taxa de preenchimento real (com textura e blend) de uma GPU de celular intermediário (Adreno 610, Mali-G52). É por isso que o AUTO cai de nível: a resolução é o que sobra para ajustar.

**O shader de fragmentos** (`GlRenderCanvas.FRAGMENTOS`) é um só para tudo: laço de até **32** semiplanos de recorte com `discard` (eram 8 antes do `CanvasGl`), sete tipos de vértice escolhidos por cadeia de `if`, desvio para o filtro de cor (com divisão por alfa), desvio para o inverso, `highp` nas coordenadas e no tipo. Em quadro de cenário nada disso é usado, mas o laço e os desvios são avaliados em cada um dos 12–20 milhões de fragmentos. `discard` no shader também desliga o descarte antecipado de fragmentos nas GPUs de celular.

**MSAA 4×.** `DesenhoGl.preparar` pede primeiro uma configuração com `EGL_SAMPLES 4` e só cai para sem antisserrilhado se o aparelho não tiver. Com MSAA a GPU guarda 4 amostras por pixel no bloco (tile), o que reduz o tamanho do bloco e multiplica o trabalho nas bordas de cada um dos milhares de triângulos do quadro; em GPUs por blocos o custo fica entre 10 e 30% do quadro, justamente onde a taxa de preenchimento já é o gargalo. O ganho visual é só nas bordas das formas do HUD (os círculos, brilhos e anéis já têm borda suave no próprio shader, e as imagens não precisam).

### 3.3 Trabalho por desenho no `CanvasGl`

O `CanvasGl` é o que permitiu levar o HUD inteiro para o OpenGL sem reescrever quem desenha, e a gravação do quadro num envio só (commit `5346cfb`) resolveu o pior: 276 chamadas e 13–31 ms de envio por quadro no celular. O que sobra é por desenho, e são centenas por quadro:

- `prepararGl()` em cada desenho faz `getMatrix` + `getValues` (duas chamadas nativas), `getSaveCount()` e repassa recorte, alfa, inversão e filtro.
- Arrays novos por forma: `clipRect` (um `float[8]`), `recortarPoligono` (`px` e um `float[96]` de planos por recorte), `drawRect` com contorno (dois `float[8]`), `drawRoundRect` (`arredondado` com 56 pontos, uma ou duas vezes), `drawOval`/`drawArc` com degradê ou contorno elíptico (`elipse`, até 96 pontos, duas vezes), `ponta`.
- `drawPath`: `aproximar` chama `Path.approximate` (nativo, aloca o resultado), monta uma `ArrayList` e um `float[][]` por caminho, a cada quadro; com contorno, `tracar` chama `Paint.getFillPath` (nativo) antes. `clipPath` côncavo passa por `envoltoria` (um `Integer[]` com boxing e um `sort` com lambda).
- `texto`: uma `String` de chave por texto por quadro (`StringBuilder.toString`) e uma busca em `LinkedHashMap` com acesso ordenado (que reescreve a ordem a cada `get`).
- Cada forma côncava vira 3 comandos (marcar, cobrir, limpar) com troca de estado de stencil e `glColorMask`.

Com um HUD cheio (vidas, barras, retratos, combo, botões, controles) isso dá centenas de objetos e de chamadas nativas por quadro. O GC do ART é concorrente, mas em aparelho de 4 núcleos as coletas jovens aparecem como quadros de 20–30 ms ("pior" alto na janela do `Quadros`) e a thread do jogo gasta CPU que a GPU espera.

### 3.4 Duas superfícies

O sistema compõe a cada quadro: a janela do app (o `DecorView`, com o buraco transparente dos `SurfaceView`), a superfície de baixo (`RGBA_8888`, com canal alfa, então o compositor a trata como translúcida) e a de cima (`TRANSLUCENT`, agora limpa durante a luta, mas um buffer limpo ainda é uma camada de tela cheia a misturar). Abaixo do ALTO as duas são escaladas pelo sistema. Camadas translúcidas e escaladas são justamente as que mais fazem o `HWComposer` desistir dos planos de hardware e cair para composição pela GPU ("client composition"), **na resolução nativa da tela** (o teto de 1080 linhas não vale aqui: numa tela 1440p são 3,7 Mpx por camada por quadro, na mesma GPU que desenha a luta). A superfície de cima também segura 3 buffers de tela cheia (uns 25 MB em 1080p) e uma thread de renderização do HWUI que não faz nada na luta.

O problema de sincronia que a fase B1 tinha (mundo num vsync, HUD noutro) acabou com a luta inteira numa superfície só.

### 3.5 Ritmo do quadro

Com `eglSwapInterval(1)`, `eglSwapBuffers` já espera a batida da tela quando a fila de buffers enche: a superfície marca o ritmo, como deve ser. Mas `GameView.loop` ainda dorme (`Thread.sleep`) o que falta para 16,67 ms depois disso. Enquanto a fila não enche (os dois ou três primeiros quadros depois de qualquer quadro perdido), o swap volta na hora e o sleep, que erra 1–2 ms para mais, atrasa o quadro seguinte: é o que transforma um quadro perdido em dois ou três. E quando um quadro passa de 16,7 ms, o acumulador de `advanceSimulation` roda dois passos de simulação no quadro seguinte: dois passos de 60 Hz num quadro de tela é um salto visível dos lutadores, por cima do quadro perdido.

Em telas de 90/120 Hz o `setFrameRate(60)` ajuda; em aparelhos que ignoram o pedido, cada quadro do jogo dura 1 ou 2 batidas, alternando.

### 3.6 Engasgos pontuais

- **Primeiro uso de uma textura no OpenGL.** `GlRenderCanvas.textura` sobe o bitmap na hora do primeiro desenho. As folhas sobem atrás da tela de carregamento e o cenário é aquecido, mas o pack do Ultra não: na primeira vez que `raioFinal.drawBehind` desenha, 27–43 MB de imagens `HARDWARE` são copiadas da placa (lento) e subidas no mesmo quadro. Um quadro de 50–150 ms exatamente no começo do raio final. O mesmo vale para qualquer imagem nova que entre na luta (efeitos de vídeo, `EntradaSuper`, quadros da vitória).
- **Decodificação em segundo plano durante a luta.** `anteciparUltras` roda em `update()` a cada passo e, com a barra em `ULTRA_PREPARO`, decodifica o pack do Ultra numa thread "Ultras" com prioridade Java `NORM-2`, que o Linux quase não distingue da normal; a thread do jogo não tem prioridade elevada. Num aparelho de 4 núcleos a decodificação de PNG de 2–4 MB compete pela CPU e pela largura de banda com o quadro.
- **Android 7 (API 24 e 25).** `lockHardwareCanvas` só existe a partir do Android 8. Abaixo disso o Canvas de cima é por software (menus, páginas) e as folhas `HARDWARE` não existem; a luta em OpenGL funciona, mas se o OpenGL falhar o jogo cai para um Canvas por software a 10–20 FPS. É o piso do `minSdk 24`.

## 4. Como confirmar cada causa no aparelho

Com o APK de debug instalado e o cabo ligado. Rodar cada cenário do roteiro T26 (`docs/balanco/aparelho.md`, seção 3) por 30 s. O contador (pausa > FPS) já mostra por janela: FPS, nível, altura desenhada, as partes do quadro, "folhas N MB", "RAM livre", e na luta em OpenGL "N desenhos · N textos novos · N formas · N chamadas · N vértices · lotes X ms · envio Y ms · N texturas M MB".

| causa | o que olhar | assinatura |
|---|---|---|
| 1 memória | `adb shell dumpsys meminfo com.gamelutagpt` durante a 2x2; `adb logcat -s Carregamento`; `adb logcat -s ActivityManager lowmemorykiller` | "Graphics"/"EGL mtrack"/"Gfx dev" + "Native Heap" perto de metade da RAM do aparelho; "atlas NNNN MB" no log; linha de kill ou "low memory" no logcat. No contador: folhas acima de 1 GB ou RAM livre abaixo de 500 MB |
| 2 preenchimento | Modo Teste > DESEMPENHO: CENÁRIO off, depois SEM ENFEITES; no contador "lotes" × "envio" | o FPS sobe muito sem o cenário; "envio" (tempo de `eglSwapBuffers`) alto com "lotes" baixo é a GPU segurando, não a CPU. Para o MSAA: trocar a ordem dos `pedidos` em `DesenhoGl.preparar` para `{8, 0}` e comparar o "envio" |
| 3 CanvasGl | `adb logcat -s Quadros` com HUD on/off (Modo Teste > HUD); contador "desenhos" e "formas" | "tela" (ms da parte do desenho) cai com HUD off; "pior" alto com "quadro" médio baixo; `adb logcat -s art` mostrando coletas a cada poucos segundos durante a luta |
| 4 composição | `adb shell dumpsys SurfaceFlinger` (as camadas `com.gamelutagpt` e a coluna de composição) | `CLIENT` (ou "GLES") em vez de `DEVICE` em alguma camada do jogo, principalmente no MÉDIO/LEVE |
| 5 ritmo | `adb logcat -s Quadros` | "pior" muito acima da média com "quadro" médio abaixo de 16 ms (ex.: quadro 12 ms, pior 34) |
| 6 engasgos | `adb logcat -s Quadros` ao começar um Ultra; contador "texturas M MB" pulando no começo do raio | "pior" de 50–150 ms na janela do Ultra |
| GPU em geral | Opções do desenvolvedor > "Depurar overdraw da GPU"; Perfetto com contadores de GPU; em Adreno `adb shell cat /sys/class/kgsl/kgsl-3d0/gpubusy` | overdraw vermelho na tela inteira (cenário); `gpubusy` perto de 100% |
| aquecimento | `adb shell dumpsys thermalservice` antes e depois de 10 min | status térmico subindo e FPS caindo junto: o remédio é gastar menos (causas 1–3), não há atalho |

## 5. Soluções

### 5.1 Rápidas (sem mudar a arquitetura; cada uma cabe em um commit)

**R1. Escala de decodificação pela memória do aparelho e pelo formato da luta**, não só pela tela (`SpriteAtlasCache.escalaAgora`/`escalaPara`).
- Ler `ActivityManager.MemoryInfo.totalMem` e `isLowRamDevice()`: com até 4 GB, escala máxima 3/4; com até 3 GB, 1/2.
- Numa 2x2, um degrau abaixo do que a tela pede (quatro personagens ocupam o dobro).
- Permitir 1/2 no LEVE (`AMPLIACAO_REDUZIDA` 1,1 → 1,5): no LEVE a tela já sai a 60% e é ampliada inteira; a folha a 1/2 não fica visivelmente pior que o resto.
- Efeito esperado: a 2x2 pesada cai de 1,35 GB (3/4) para 600 MB (1/2); a comum de 874 para 389 MB. Vale a partir do próximo carregamento, como hoje.

**R2. Cenário e Ultra sem a cópia em dobro no OpenGL** (`AndroidRenderAssets.loadImage`): decodificar como `HARDWARE` só quando `telaPelaGpu && !paraOpenGl`. Com a luta em OpenGL, decodificar `ARGB_8888`, subir em `GlRenderCanvas.textura` com `soltarBitmap` e reciclar (como já se faz com as folhas). Tira 46–129 MB por cenário e 25–43 MB por pack de Ultra, e acaba com a cópia GPU→CPU na primeira subida. Como a luta inteira já é desenhada no `CanvasGl`, não há mais quem precise dessas imagens no Canvas durante a luta; só as telas fora da luta (páginas, seleção), que têm o próprio carregamento.

**R3. Subir o pack do Ultra antes do raio.** Na abertura do Ultra (`abertura.isActive()`, a luta espera parada) chamar `GlRenderCanvas.textura` para as imagens que `raioFinal` vai usar, com orçamento por quadro como em `DesenhoGl.subir`. O engasgo do primeiro quadro do raio some. O mesmo para as folhas de efeitos de vídeo e `EntradaSuper` no carregamento da luta.

**R4. Folhas ocasionais sob demanda.** `preload` carrega todas as animações do personagem. ULTRA_SEQ, VICTORY, TAUNT, INTRO e SUPER_* somam 30–100 MB por personagem e têm um momento certo:
- INTRO: carregar antes da luta e soltar quando o READY → GO passa.
- VICTORY: pedir no KO (a câmera lenta do KO e a finalização dão 1–2 s); `SpriteFighterRenderer.drawQuadro` já pula folha não pronta.
- ULTRA_SEQ e SUPER_*: pedir junto com o pack em `anteciparUltras` (barra em `ULTRA_PREPARO`), que já existe para o pack. Usar `antecipar`, que não bloqueia.
- Marcar essas animações no `character.json`/`GeneratedCharacters` (um booleano `ocasional` no `Atlas`) para o `preload` pular e o `retainOnly` soltar.

**R5. Orçamento de antecipação e aviso do sistema.** 35% da RAM (até 2,5 GB) para antecipar é demais quando a luta em si pede 0,5–1,5 GB. Limitar a antecipação ao que a luta atual deixa (ex.: `totalMem × 0,55 − bytes da luta`) e implementar `ComponentCallbacks2.onTrimMemory` em `MainActivity`: em `TRIM_MEMORY_RUNNING_LOW` e acima, `SpriteAtlasCache` solta as folhas só antecipadas e `GlRenderCanvas` apaga texturas de bitmaps já soltos; em `TRIM_MEMORY_RUNNING_CRITICAL`, o AUTO desce um nível na próxima luta.

**R6. MSAA desligado por padrão.** Em `DesenhoGl.preparar`, pedir `{8, 0}` primeiro e deixar `{8, 4}` só para o nível ALTO em aparelho que já sustenta 60 (ou tirar de vez: os círculos, anéis e brilhos já têm borda suave no shader). É a mudança de uma linha com maior ganho de GPU desta lista.

**R7. Shader mais barato no caso comum.** Dois programas em `GlRenderCanvas`: um "simples" (imagem ou cor, sem recorte, filtro, inverso ou degradê) para cenário e lutadores sem flash, e o atual para o resto; a troca de programa já cabe no esquema de comandos (mais um campo por comando). Reduzir `MAX_PLANOS` de volta a 8 (`CanvasGl.recortarPoligono` soma recortes; o jogo raramente passa de dois retângulos) e escrever o laço com limite fixo pequeno. `glDisable(GL_BLEND)` ao desenhar as camadas opacas do cenário (céu e chão são JPG sem alfa: marcar a `Textura` como opaca na subida e emitir um comando sem mistura).

**R8. `CanvasGl` sem lixo por quadro.**
- Buffers reutilizados em `clipRect`, `recortarPoligono`, `drawRect` (contorno), `arredondado`, `elipse`, `anel` e `ponta` (arrays de tamanho máximo guardados na instância; `recortes[k]` pode apontar para um pool por nível de save).
- `prepararGl` só relê a matriz quando ela mudou: `Canvas.getMatrix` só muda depois de `translate`/`scale`/`rotate`/`concat`/`restore`, então basta uma flag "matriz suja" nas sobrescritas dessas chamadas.
- `drawPath`: cache de aproximação por `Path` quando o caminho não muda (o `HudRenderer` monta `shape` por quadro, então o cache precisa de um `Path.isEmpty`/`computeBounds` barato como chave, ou melhor: dar ao `HudRenderer` um jeito de passar o polígono direto por `fillPolygon`/`fillTriangles`, deixando `approximate` só para curvas de verdade).
- `texto`: antes de montar a chave, uma busca por identidade (`System.identityHashCode(s)` e do `Paint`, com a escala) num array pequeno; as strings fixas do HUD são o mesmo objeto a cada quadro e nem chegam ao `StringBuilder`. Trocar o `LinkedHashMap` com acesso ordenado por um mapa comum com contador de uso.
- `envoltoria` sem `Integer[]` (ordenar índices em `int[]`).

**R9. Ritmo travado na tela.** Com `eglSwapInterval(1)`, tirar o `Thread.sleep` da luta em OpenGL (o swap já espera o vsync; o sleep só acrescenta erro) e deixá-lo só para o caminho de Canvas. Encaixe do passo em `advanceSimulation`: se o tempo entre quadros estiver a até 2 ms de 1/60, usar exatamente 1/60 (um passo); a até 2 ms de 2/60, exatamente dois. Zera o acúmulo de erro que hoje gera o passo duplo. Opcional: `Choreographer.postFrameCallback` numa `HandlerThread` com `frameTimeNanos` como relógio da simulação.

**R10. Prioridades de thread.** `android.os.Process.setThreadPriority(THREAD_PRIORITY_DISPLAY)` no começo de `GameView.run`; `THREAD_PRIORITY_BACKGROUND` nas threads "Folhas", "Ultras" e "Carregando" (a prioridade Java `NORM-2` quase não muda o agendamento no Linux). Durante a luta, limitar os trabalhadores de decodificação a 1.

**R11. Superfície de baixo opaca, de cima mínima.** `PixelFormat.RGBX_8888` e `EGL_ALPHA_SIZE 0` em `DesenhoGl` (o quadro começa preto e cobre tudo); `android:windowBackground="@null"` no tema e `getWindow().setBackgroundDrawable(null)`. Enquanto a superfície de cima existir, encolhê-la na luta (`holder.setFixedSize(1, 1)` ao entrar, tamanho do layout ao sair) em vez de só limpá-la: o compositor mistura 1 pixel, não a tela inteira, e os 3 buffers de tela cheia somem. Conferir antes e depois com `dumpsys SurfaceFlinger`.

### 5.2 Estruturais (semanas; o fechamento do desenho novo)

**E1. Uma superfície só.** Menus, seleção, páginas e carregamento também pelo `CanvasGl` (eles já desenham por `RenderCanvas` ou `Canvas`, que o `CanvasGl` cobre), e o `GameView` deixa de ser um `SurfaceView` com buffer próprio: vira só a view de toque sobre a superfície do OpenGL. Some a segunda camada de composição, o HWUI e a thread de renderização dele, e o caminho de Canvas fica como reserva só para falha do OpenGL. O que falta ao `CanvasGl` para isso: `clipPath` côncavo de verdade (hoje vira a envoltória convexa; as páginas da história recortam painéis) e `drawVertices` (o chão em faixas já tem `drawImageStrip`).

**E2. Texturas comprimidas para as folhas.** ETC2 (RGBA8 EAC, 8 bits por pixel) é obrigatório no OpenGL ES 3.0, então está em todo aparelho que roda o desenho novo; ASTC (4–6 bits por pixel) está na maioria dos aparelhos de 2016 em diante. Gerar no `build_characters.py` (KTX2, com mipmaps) e carregar com `glCompressedTexImage2D`: **4× menos memória de vídeo com ETC2, 6–8× com ASTC**, 4–8× menos largura de banda ao desenhar, e carga sem decodificar WebP (10× mais rápida). A 2x2 pesada cairia de 2,4 GB para 600 MB em ETC2 na escala 1, ou 300 MB em ASTC 6×6. Cuidado com a qualidade de contornos finos com alfa (ETC2 é visível em linhas de 1 px; ASTC 5×5 ou 6×6 não). Mantém-se o WebP para o Canvas (ferramentas de ajuste, PC).

**E3. Menos quadros nas folhas.** Decidir uma taxa por tipo de clipe (idle e andar a 20–30 quadros distintos por segundo; golpes podem ficar em 30 com os quadros do impacto preservados; ULTRA_SEQ a 30). `MotionParser`/`SpriteMotion` mapeiam tempo → quadro, então o frame data do motor não muda; o `build_characters.py` descarta quadros intermediários na geração. Metade dos quadros é metade da memória e metade do tempo de carga, em cima de qualquer outra medida.

**E4. Cenário.** Imagens do cenário a 1024 px de lado em aparelhos de até 4 GB (`MAX_IMAGE_SIDE` por memória), atores do Shinshinkan de 1600×1600 para 1024 (hoje 129 MB, o dobro dos outros cenários), e reflexos no chão desligados também no MÉDIO (`fighterReflection`: desenha os quatro lutadores de novo e custa 4–9% no PC). As camadas de neblina (degradês de tela cheia) podem ser uma só por profundidade combinada.

### 5.3 Ordem sugerida

1. **R1, R2, R3, R5** (memória): um ou dois dias, ganho imediato nas 2x2 e nos aparelhos de 4 GB; mede-se com `meminfo` antes e depois.
2. **R6, R7** (GPU): meio dia; mede-se com "envio" no contador e CENÁRIO off/on.
3. **R8, R9, R10** (CPU e ritmo): um ou dois dias; mede-se com `logcat -s Quadros` (o "pior" deve cair para perto da média) e "desenhos"/"formas" no contador.
4. **R11, E4** (composição e cenário): um dia; `dumpsys SurfaceFlinger`.
5. **E1** (uma superfície): o fechamento do desenho novo.
6. **E2 e E3** (conteúdo): em paralelo com E1, no `build_characters.py`; E3 é decisão de arte, E2 só de ferramenta.

### 5.4 O que já foi feito (itens 1, 2 e 3 da revisão)

A ordem seguida foi CPU primeiro, porque no aparelho o envio para a GPU mede de 1,6 a 4 ms por quadro e o gargalo é a CPU (HUD e `CanvasGl`).

- **R6:** MSAA fora. `DesenhoGl` só pede `{stencil 8, MSAA 0}`; as bordas suaves vêm do shader.
- **R9:**
  - Sem `Thread.sleep` nos quadros em OpenGL (o swap já espera o vsync).
  - Passo encaixado: a até 2 ms de n/60 conta exatamente n passos.
- **R10:** a thread do jogo roda em `THREAD_PRIORITY_DISPLAY`, e a de "Ultras" em `THREAD_PRIORITY_BACKGROUND`. As de "Folhas" ficaram como estavam, porque no Android o fundo vai para os núcleos pequenos e a tela de carregamento ficaria mais lenta.
- **R11:** na luta em OpenGL, a superfície de cima encolhe para 1×1 (`setFixedSize`) e volta ao tamanho normal fora da luta.
- **R8:**
  - `CanvasGl` só relê a matriz quando ela mudou (flag de matriz suja nas sobrescritas de `translate`/`scale`/`rotate`/`skew`/`concat`/`setMatrix`/`restore`).
  - Buffers reaproveitados em recortes, retângulos, arredondados, elipses e linhas.
- **R2:** cenário e packs de Ultra da luta vêm de um `AndroidRenderAssets` próprio (`daLuta`). Com OpenGL, decodifica em memória comum, sobe e solta o bitmap. Se o Canvas precisar da imagem depois, ela é decodificada de novo.
- **R3:** as imagens do pack do Ultra sobem para o OpenGL uma por quadro durante a abertura dele (`GameView.preSubirUltra`; ver 5.5).
- **R4:** no celular, as folhas só de VICTORY, TAUNT, ULTRA_SEQ e ULTRA_SEQ_FRENTE ficam fora do `preload`/`antecipar` (`SpriteAtlasCache.ocasionaisSobDemanda`). São de 10% a 30% de cada personagem, cerca de 355 MB somando o elenco na escala 1. Elas são pedidas sem bloquear (`pedir`):
  - **ULTRA_SEQ:** a partir de 2 barras. Na ativação, a abertura do Ultra espera a folha, como já esperava o pack.
  - **VICTORY:** quando o rival está com até 40% da vida ou o relógio chega a 20 s. Se a folha ainda não chegou, o vencedor fica parado até ela chegar.
  - **TAUNT:** enquanto o treino está ativo.

  Uma folha rara que fica pronta no meio da luta sobe para o OpenGL num quadro sem outro envio. Se ainda assim alguém desenhar uma folha que não carregou, `get` decodifica só ela, sem passar pelo `preload` (que a pularia). INTRO e SUPER_* continuam no `preload`.

Ainda não feitos: R1, R5, R7, E1–E4.

### 5.5 Varredura de 30/10 (cinco frentes em paralelo)

Medido no aparelho antes dela: 95 textos novos por quadro com as mensagens, 79–99 formas pelo
stencil por quadro, 7 FPS no SHINKAI, picos ("pior") de 74–194 ms.

- **Textos (CanvasGl e GlRenderCanvas):**
  - cache na ordem de uso (LRU, 512);
  - o texto que entra grande e encolhe reaproveita o bitmap do quadro maior;
  - teto de ~0,4 Mpx e de 2000 px de lado também no caminho do RenderCanvas (texto maior que a textura dos textos era refeito a cada quadro);
  - chave que não cabe nem reduzida não é tentada de novo;
  - escala pelo maior eixo (a legenda que abre esticando);
  - contorno em degraus de 1/8 de oitava;
  - timer do botão SHINKAI em segundos inteiros;
  - linhas do diagnóstico do contador a cada 2 s.
- **Formas:**
  - preenchimento estrelado a partir do centro (chamas do nível, farpas, estrelas, cauda do projétil) e vários contornos com caixas separadas (o "!") saem em leque, sem stencil;
  - traço vira tira de triângulos;
  - anel do RenderCanvas pelo shader.
- **Lotes:**
  - cada desenho não zera mais o filtro de cor (cada sprite tingido abria um lote);
  - o mesmo recorte noutro vetor não fecha o lote;
  - o contorno do SHINKAI não é desenhado no reflexo do chão.
- **Engasgos:**
  - ULTRA_SEQ voltou ao preload: sob demanda, a folha subia para o OpenGL no meio da luta (até ~58 MB num quadro);
  - as imagens do Ultra sobem durante a abertura dele, com a luta parada;
  - a comemoração sobe no primeiro desenho, já no KO;
  - no máximo 3 passos de simulação por quadro;
  - tela de 120 Hz: uma troca a cada duas batidas (o jogo anda a 60).
- **Lixo por quadro:**
  - chave de ajuste de sprite só para folhas com correção;
  - chave do tamanho calculada uma vez por folha;
  - linha do degradê pelo próprio objeto;
  - rótulo do Super (String.format) só quando pedido;
  - painel do adversário só no DEBUG;
  - legenda da conversa diagramada uma vez por fala;
  - onomatopeia dividida uma vez.
- **Entrada:** ver o commit de especiais no toque (janela pelo primeiro passo solto, direção que chega logo depois do botão, especial com 10 quadros de buffer, botões antes dos pontos L+M/M+H).

### 5.6 Teste de desempenho automático (botão TESTE e `gradle :pc:medirLuta`)

O mesmo roteiro no celular, no Ally e no PC sem tela, para medir cada mudança sem depender de
captura de tela:

- **Como rodar:** no celular, entre numa luta e toque em **TESTE**, no painel de desempenho, ao
  lado de FOLHAS. No PC sem tela, rode `gradle :pc:medirLuta`; com `-Presolucao=1920x1080` muda o
  tamanho. A saída vai para `pc/build/medir-luta/relatorio.txt` e as fotos para os PNGs da
  mesma pasta.
- **Roteiro:** 30 s de luta (1800 passos) com a IA dos dois lados, sempre no mesmo momento:
  - SHINKAI do jogador aos 3 s;
  - Super aos 10 s;
  - Ultra aos 16,7 s;
  - SHINKAI do rival aos 23,3 s.

  A vida volta quando cai abaixo de 1/3. As barras de SHINKAI ficam zeradas fora do roteiro,
  porque um SHINKAI da IA virava um Clash por cima do Ultra. A qualidade AUTO fica parada durante
  o teste.
- **Relatório:** uma tela, que fecha com outro toque em TESTE. Também sai no log `Quadros`. Mostra:
  - o quadro médio, o p95, o pior e quantos quadros passaram de 20 ms;
  - a média e o p95 de cada parte;
  - o quadro médio por momento (normal, combo, SHINKAI, Super, Ultra);
  - no OpenGL, os textos novos, as formas e os desenhos por quadro;
  - os 5 piores quadros, com a parte que mais pesou e o momento.

Primeira medida no PC sem tela, a 1280×720 em Canvas: quadro médio de 26 ms. Por momento:
normal 18 ms, SHINKAI 24, Super 27 e Ultra 42. O fundo pesa 17 ms de média e 48 ms de p95.
A máquina de medida varia entre rodadas (com pouca RAM livre), então compare duas rodadas
seguidas, antes e depois da mudança.

## 6. Anexo: cenários e folhas

Imagens de cada pacote de cenário, memória decodificada (×2 no OpenGL hoje, seção 3.1) e camadas:

| cenário | imagens | MB RGBA | camadas | observação |
|---|---|---|---|---|
| dojo_shinshinkan | 20 | 129 | 14 | atores 1600×1600 em paleta, decodificados para RGBA |
| caminho_penhascos | 26 | 72 | 11 | `abismo_ondas` 2676×1016 |
| arena_imperial | 27 | 64 | 12 | água |
| porto_secreto | 18 | 60 | 14 | água, 3 neblinas |
| beco_brucy | 11 | 58 | 12 | chuva e relâmpagos |
| arena_costeira | 35 | 56 | 11 | água, 3 neblinas |
| arena_central | 32 | 55 | 12 | água |
| dojo_yinlu | 14 | 55 | 13 | |
| templo_lua | 11 | 46 | 11 | água |
| terraco_lua | 1 | 0 | 9 | só a lua; o resto é cor |

As 12 maiores folhas e a fração de pixels opacos (o resto é transparente e ocupa memória do mesmo jeito):

| folha | tamanho | MB RGBA | opaco |
|---|---|---|---|
| p13_special_energy | 4908×3256 | 64 | 40% |
| p14_special_energy | 5532×2856 | 63 | 28% |
| p14_special_s2 | 3880×3972 | 62 | 35% |
| p14_ultra_seq | 3924×3848 | 60 | 38% |
| p15_heavy_straight | 3956×3556 | 56 | 31% |
| p16_special_s4 | 3308×4060 | 54 | 23% |
| p16_super_wave | 3312×3984 | 53 | 29% |
| player_base_idle | 3604×3280 | 47 | 45% |
| p14_special_s3 | 3828×2984 | 46 | — |
| p15_crouch_medium | 2920×3848 | 45 | — |
| p04_idle | 3192×3460 | 44 | 41% |
| p14_idle | 3276×3288 | 43 | 36% |

Como repetir as medições deste relatório: Python 3 com Pillow no repositório, somando `largura × altura × 4` das imagens em `android/app/src/main/res/drawable-nodpi` (folhas) e `android/app/src/main/assets/stages` (cenários); a cobertura de alfa é a fração de pixels com alfa maior que zero.
