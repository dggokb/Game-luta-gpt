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

(Pendente: o levantamento dos efeitos, um por um, ainda está sendo feito.)
