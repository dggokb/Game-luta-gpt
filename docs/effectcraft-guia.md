# EffectCraft: o que ele oferece e o que vale para o nosso VFX

Levantamento de 10/11 (noite), pedido do dono: "será que estamos usando tudo que o EffectCraft tem
a oferecer? vira especialista nele". Fontes: a documentação e o código do próprio EffectCraft
(0.23.1, em `/home/user/storytold/effectcraft`, lidos por dois agentes de leitura) e testes nossos
pela linha de comando, sem tela. O jeito de trabalhar e as pegadinhas estão na skill
`.claude/skills/effectcraft`; aqui fica o inventário.

**Resposta curta: não estávamos.** Dos 308 efeitos usamos uns 15, e quase nada do resto: 3D,
expressões, scripts, modelos parametrizados, partículas, operadores de forma, precomps e plug-ins.
Marcado com ✔ o que testamos aqui e funcionou.

## O que muda o nosso jeito de fazer

| Recurso | Por que importa | Como |
|---|---|---|
| ✔ **3D de verdade** | Anel de choque em 3/4 que passa na frente e atrás da estrela sozinho, com o traço afinando nas laterais pela perspectiva. Substitui as duas camadas com máscara que fizemos. | `layer.setSwitch {switch:"threeD"}`, `transform/rotationX`/`rotationY`, `layer.newCamera {preset:"50mm"}`. Planos que se cruzam se ordenam pixel a pixel (Classic 3D). |
| ✔ **Expressões** | Uma linha no lugar de centenas de quadros-chave; `posterizeTime(12)` dá o degrau de anime até na escala e no giro; `seedRandom(index)` faz N cópias de uma faísca, cada uma diferente. | `prop.setExpression {layer, path, expression}`. Têm: `linear`/`ease`, `seedRandom`/`random`/`gaussRandom`/`noise`, `posterizeTime`, `wiggle` (semente por camada), `loopOut`/`loopIn`, `valueAtTime`, `index`, `thisComp.layer(...)`. |
| ✔ **Modelo parametrizado** | Um efeito-base com controles (cor do miolo, cor da borda, tamanho, semente) e uma variante por personagem só com um `values.json`: a "linha de tabela por personagem" do relatório de VFX. | `essential.addProperty` em controles de efeito (Slider/Color Control numa camada de controle) lidos por expressão; `essential.exportTemplate`; `effectcraft-cli render --template T.ectemplate --values V.json --out 'x_[####].png'`. Controle sobrescrito perde as chaves: exponha só valores parados. |
| ✔ **Operadores de forma** | Traço com **taper** (nasce fino e engrossa: o corte de golpe do anime numa forma só), **Pucker & Bloat** (estrela de espinhos finos), Merge Paths (crescente), gradiente radial (miolo quente), Zig Zag, Twist, Offset Paths. | `layer.addShapeItem kind:` `pucker` (`amount`), `merge` (`mode`), `zigzag`, `twist`, `offset`, `round`, `gfill`/`gstroke`; taper em `stroke/taper/{units,startLength,endLength,startWidth,endWidth}`, onda em `stroke/wave/*`. |
| ✔ **Partículas e raio** | Faíscas, brasas e eletricidade sem desenhar ponto a ponto. | `Particle Emitter 2D` (tipo Star, `gravity` 0, `resistance`), `Advanced Lightning` (Direction, glow colorido). Pontos em pixels. |
| **Scripts .jsx** | Lógica com laços e contas (N faíscas, explosão radial) em JavaScript com o modelo do After Effects, chamando qualquer comando por `app.run(id, params)`. | `effectcraft-cli script monta.jsx --save-as x.ecproj`. Escrever arquivo pede `prefs.set {"key":"scripting.allowScriptsWriteFiles","value":true}`. A lista de comandos em JSON continua boa para montagem plana. |
| **Precomps e tempo** | Uma faísca pronta vira peça: aninhada, deslocada, esticada, repetida para golpes de vários acertos. | `layer.precompose`, `layer.addItem {item:"Comp"}`, `comp.settings {preserveFrameRate}`, `layer.enableTimeRemap`, `layer.timeStretch`, `layer.timing`. |
| **Motion blur de verdade** | O borrão de obturador no corte rápido, em vez de Directional Blur. | `layer.setSwitch {switch:"motionBlur"}` + `comp.setSwitch {switch:"motionBlur"}`, `comp.settings {shutterAngle, motionBlurSamples}`. |
| **Presets** | Guardar um conjunto de efeitos ajustados (o nosso brilho A/B, a erosão) e aplicar em outro efeito. 76 prontos (wiggle, shake, flicker, strobe...). | `anim.savePreset`, `anim.applyPreset`, `anim.builtinPresets`. |
| **Plug-in WebAssembly** | Escrever o nosso próprio efeito: o shader de erosão com borda luminosa e rampa de cor do relatório, em Rust, rodando dentro do EffectCraft. | `docs/plugins.md` de lá; exemplo `examples/plugins/posterize-bands`; `effect.plugins.install`. Não aumenta a área da camada e não recebe outra camada como parâmetro. |
| **Modo MCP** | Ajustar ao vivo: muda um valor e vê o quadro na hora, sem remontar o projeto. | `effectcraft-cli mcp --autosave`; `render_frame {transparent, inline}`. |

## O que não serve (ou não muda nada)

- **Lottie**: exporta formas, máscaras, mattes, precomps e chaves, mas perde Glow, Fractal Noise,
  Turbulent Displace, Wiggle Paths e o taper. Para o jogo, a folha de quadros continua o caminho.
- **Folha de quadros**: o EffectCraft não monta folha; o `tools/sprites/efeito_video.py` continua.
- Formatos com alfa: PNG, TIFF, TGA, EXR, WebM VP9 e ProRes 4444; GIF só alfa de 1 bit.

## Efeitos que valem para anime 2D

A documentação de lá marca os 308 como "Implemented", mas só alguns foram medidos pixel a pixel
contra o After Effects (Glow, Radial Blur, Fractal/Turbulent Noise e alguns de cor): os outros têm
os nomes e parâmetros do After Effects e podem desenhar um pouco diferente. Pontos de efeito são
**pixels da camada**. Os 10 melhores para nós, na ordem:

| # | Efeito (id) | Para que | Como acertar |
|---|---|---|---|
| 1 | ✔ **Radio Waves** (`ec.generate.radiowaves`) | anéis de pressão de ar e de impacto de verdade: nascem, abrem e afinam | `producerPoint` (px), `waveMotion/frequency`, `lifespan`, `velocity`, `expansion`; `waveStroke/profile` 1-3 (Taper Front/Back/Both), `startWidth` > `endWidth`, `fadeOutTime`; Unmult depois |
| 2 | ✔ **Particle Emitter 2D** (`ec.sim.ccparticlesystems2`) | faísca de acerto e de defesa, brasas | `physics/animation` 0 (Explosive), `particle/particleType` 0 (Line) ou 4 (Star), `transferMode` 2 (Add), `gravity` 0, `resistance`, `birthColor`/`deathColor`, `randomSeed` |
| 3 | ✔ **Advanced Lightning** (`ec.generate.advancedlightning`) | eletricidade: raio (Direction), estouro (Omni, Anywhere) | `origin`/`direction` (px), `coreSettings/*`, `glowSettings/*`, `forking`, `conductivityState` (anime para tremer), `compositeOnOriginal` |
| 4 | ✔ **Colorama** (`ec.color.colorama`) | a rampa de cor do relatório: ruído ou desfoque vira fogo/fumaça/energia em faixas chapadas | `outputCycle/usePresetPalette` 8 (Custom), `outputCycle/palette` `"0:r,g,b 0.6:r,g,b ..."`, `interpolatePalette` false (faixas duras), `cycles` 0.999 (senão o branco volta para a primeira cor) |
| 5 | ✔ **Fractal / Turbulent Noise** + **Threshold** / **Channel Threshold** | base de fumaça, fogo, poeira e erosão | Fractal tem Cycle Evolution (repete sem emenda); Turbulent não. Channel Threshold no canal Alpha dá fumaça de borda dura a partir de partículas borradas |
| 6 | ✔ **Glow** (`ec.stylize.glow`) | brilho; dois tons com `colors` 1 (A & B) | medido contra o After Effects; **os nomes de `operation` estão trocados** (0 "On Top" é o "Behind" do After Effects, o normal) |
| 7 | ✔ **Polar Coordinates** (`ec.distort.polar`) | listras viram raios, anéis, ondas | `conversion` **1** = o "Rect to Polar" dos tutoriais (nomes trocados no código), `interpolation` 100 |
| 8 | **Echo** (`ec.time.echo`) | rastro fantasma de corte e projétil sem desenhar nada | `echoTime`, `numberOfEchoes`, `decay`, `echoOperator` (Maximum ou Composite In Back) |
| 9 | **Fast Radial Blur / Variable Radial Blur** | riscos de zoom no impacto; giro em anel ou vórtice | `zoom` Brightest para riscos de luz; `type` Rotate para giro |
| 10 | **Cartoon / Posterize / Minimax** | passar coisa macia para faixas de anime; engrossar ou afinar o alfa | Cartoon `fill/shadingSteps` 2-4 e `shadingSmoothness` ~0; Minimax no canal Alpha |

Também úteis: **Beam** (laser, raio de energia; `softness` 0 para cel), **Stroke / Write-on**
(corte desenhado ao longo de máscara ou de um ponto animado), **Vegas** (linha de energia correndo
em volta de uma silhueta), **Cell Pattern** (máscara celular para quebrar fumaça e fogo, repete
sem emenda), **Shatter** (cacos de vidro para quebra de guarda; `view` 0, senão sai em arame),
**Pixel Scatter / Noise Alpha** (desfazer em poeira), **Curl Noise** (redemoinho de fumaça sem
rasgar), **Light Rays / Radial Light Burst** (raios a partir dos pixels claros de outra camada).
Fraco para o nosso estilo: Liquid Particles e Organic Displacement (bolhas metálicas iluminadas),
Lens Flare (cara de foto), Pixel Motion Blur (contra o visual de anime).

**Contorno em volta do alfa** (faixa de borda): o estilo de camada Stroke
(`layer.style.add {style: "stroke"}`, `layerStyles/stroke/size|color|position`); só com efeito,
Vegas com `channel` 4, `segments` 1, `length` 1.

**Mapa de cor por brilho**: Colorama (acima). Suave: Multi-Tone Tint (até 5 tons) ou Tritone;
`.cube` de verdade: Apply Color LUT.

## Por onde começar

1. Refazer o anel de pressão de ar com **Radio Waves** ou com uma forma em **3D** (cada um em um
   teste), no lugar da elipse achatada com duas camadas.
2. Fazer as faíscas e brasas com **Particle Emitter 2D** em vez de losangos um a um.
3. Montar um **modelo parametrizado** de faísca (cor do miolo, cor da borda, tamanho, semente) e
   tirar a variante de cada personagem com um `values.json`.
4. Fumaça e poeira: **Fractal Noise -> Threshold -> Colorama** em faixas, com expressão
   `posterizeTime(12)` na evolução.
5. Para o personagem elétrico: **Advanced Lightning**.

Lembrete da regra 2 da skill `vfx-luta`: tudo isso é desenho feito no programa. O dono decide se
abre a exceção; a alternativa é usar o EffectCraft só para montar e animar imagens geradas.
