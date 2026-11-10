# Extensões do pipeline EffectCraft para VFX anime 2D profissional

Levantamento de 10/11/2026. Fontes: o repositório local do EffectCraft 0.23.1
(`/home/user/storytold/effectcraft`, só lido, nada alterado), comandos de leitura do binário
`target/release/effectcraft-cli`, os nossos `docs/effectcraft-guia.md` e
`.claude/skills/effectcraft/SKILL.md`, buscas na web e leitura direta de páginas do GitHub
(raw.githubusercontent.com e api.github.com responderam; shadertoy.com, gmic.eu e
godotshaders.com continuaram sem resolver o nome mesmo depois da rede ser aberta, então o que vem
desses sites foi visto **só em trechos de busca** e está marcado assim). Nada foi instalado nem
compilado.

Estado da máquina conferido com comandos só de leitura:

- `rustup target list --installed` → só `x86_64-unknown-linux-gnu`. **O alvo `wasm32-unknown-unknown`
  não está instalado**; compilar qualquer plug-in WASM exige antes `rustup target add
  wasm32-unknown-unknown` (baixa da rede, não feito). rustc 1.97.0.
- `effectcraft-cli exec effect.plugins.list --params '{}' --empty --json` →
  `{"api":2,"helpers":true,"plugins":[],"wasm":true}`: o build aceita WASM **e** "external effect
  helpers"; nenhum plug-in carregado; `~/.config/effectcraft` não existe (nunca se instalou nada).
- Ferramentas externas presentes: `/usr/bin/ffmpeg` 6.1.1 (build Ubuntu com `--enable-gpl`),
  `/usr/bin/convert` (ImageMagick 6.9.12-98 Q16), Python 3 com numpy 2.5.3, Pillow 12.3.0 e OpenCV
  (cv2) 5.0.0. Ausentes: `gmic`, `magick` (IM7), `NatronRenderer`, `blender`, `krita`, scipy.

## 1. Plug-ins WebAssembly que podemos escrever (e o que já existe embutido)

### Takeaway
O ABI é pequeno e o exemplo `posterize-bands` tem ~100 linhas de Rust sem dependências, então
cada efeito abaixo cabe em 100-300 linhas; o que **não** existe pronto e vale a pena é (1) erosão
com borda luminosa na frente da erosão e (3) contorno de largura variável; (2) e (5) são quase
cobertos por Colorama/Cartoon e só compensam se quisermos controles de cor expostos no modelo
parametrizado. Antes de tudo é preciso instalar o alvo wasm32 (não instalado). Há uma rota que
dispensa Rust: um "external effect helper" escrito em Python+numpy, que ainda por cima pode crescer
a área da camada e receber outra camada como parâmetro (as duas limitações do WASM).

### Cited Findings

**O contrato do plug-in WASM (lido no repositório)**
- Um plug-in é um módulo `.wasm` sem nenhum import (sem arquivo, relógio, rede ou números
  aleatórios), com orçamento de "fuel" por quadro e memória máxima de 1 GiB; laço infinito ou trap
  vira erro e deixa o quadro como estava — [docs/plugins.md](/home/user/storytold/effectcraft/docs/plugins.md), [docs/publishing-plugins.md](/home/user/storytold/effectcraft/docs/publishing-plugins.md)
- Exports: `memory`, `ec_api_version` (1 ou 2), `ec_manifest_ptr`/`ec_manifest_len` (manifesto
  JSON), `ec_alloc(bytes)`, `ec_render(pixels, width, height, params, nparams, time, scale)`; ABI 2
  troca por `ec_render2` que recebe também o quadro anterior e o seguinte (`prev`, `next`, com
  `flags` dizendo se existem), reamostrados na grade do quadro atual — [docs/plugins.md](/home/user/storytold/effectcraft/docs/plugins.md)
- Pixels: RGBA `f32` **pré-multiplicado**, 0-1 (pode passar de 1 em projeto 32 bpc), em linha,
  processados no lugar; o buffer pode estar em resolução de preview (`scale`) e com folga.
  Parâmetros chegam como `f64` na ordem declarada: slider/ângulo/checkbox/popup = 1 valor, ponto = 2
  (já em pixels do buffer), cor = 4 — [docs/plugins.md](/home/user/storytold/effectcraft/docs/plugins.md)
- Tipos de parâmetro WASM: `slider`, `angle`, `checkbox`, `popup`, `point`, `color` (até 256).
  `point3`, `layer`, `text` e `hidden` só existem para plug-ins nativos em Rust / helpers (API 2);
  "WebAssembly modules can't use point3, layer, text or hidden" — [docs/plugins.md](/home/user/storytold/effectcraft/docs/plugins.md)
- `padding` (pedir pixels transparentes em volta para brilho/contorno que passa da borda) é método
  de `EffectPlugin` nativo e do protocolo de helper, não do ABI WASM: daí a limitação "não aumenta a
  área da camada" — [docs/plugins.md](/home/user/storytold/effectcraft/docs/plugins.md), [docs/external-helpers.md](/home/user/storytold/effectcraft/docs/external-helpers.md)
- Existe o efeito embutido **Grow Bounds** (`ec.utility.growbounds`, parâmetro `pixels`), que
  aumenta a área da camada antes dos efeitos seguintes — [docs/effects.md](/home/user/storytold/effectcraft/docs/effects.md) e `effect.list` local
- Aleatoriedade tem de vir de hash da posição, do tempo e de um parâmetro de semente;
  `println!`, `std::time`, `getrandom`/`rand` e crates com `wasm-bindgen` fazem o módulo importar
  algo e ele não carrega — [docs/publishing-plugins.md](/home/user/storytold/effectcraft/docs/publishing-plugins.md)
- Exemplo `examples/plugins/posterize-bands/src/lib.rs`: ~100 linhas, sem dependências, buffer
  reaproveitado em `thread_local`, des-pré-multiplica, quantiza luminância em N faixas, mistura uma
  cor; `Cargo.toml` com `crate-type = ["cdylib"]`, `opt-level="s"`, `lto`, `panic="abort"`,
  licença "MIT OR Apache-2.0" — [lib.rs](/home/user/storytold/effectcraft/examples/plugins/posterize-bands/src/lib.rs), [Cargo.toml](/home/user/storytold/effectcraft/examples/plugins/posterize-bands/Cargo.toml)
- Build e carga sem tela: `cargo build --release --target wasm32-unknown-unknown`;
  `effectcraft-cli exec effect.plugins.load '{"path": ".../x.wasm"}'` (só na sessão) ou
  `effect.plugins.install {path|folder}` (copia para `Plug-ins` e carrega em todo início); efeito é
  aplicado por id como qualquer outro (`effect.apply {"effect": "org.x.y"}`), com chaves e
  expressões — [docs/plugins.md](/home/user/storytold/effectcraft/docs/plugins.md)
- O EffectCraft é "MIT OR Apache-2.0" (raiz do repositório), e o exemplo também: podemos copiar o
  esqueleto do exemplo para um plug-in nosso — [Cargo.toml raiz e LICENSE-*](/home/user/storytold/effectcraft/Cargo.toml)

**Efeitos embutidos que já cobrem parte de cada ideia (parâmetros lidos com `effect.list`)**
- Glow `ec.stylize.glow`: `threshold, radius, intensity, operation, colors (A&B), colorA, colorB,
  glowDimensions…`; Echo `ec.time.echo`: `echoTime, numberOfEchoes, startingIntensity, decay,
  echoOperator`; Minimax `ec.channel.minimax`: `operation, radius, channel, direction`; Cartoon
  `ec.stylize.cartoon`: `fill/shadingSteps, fill/shadingSmoothness, edge/edgeWidth…`; Gradient Wipe
  `ec.transition.gradientwipe`: `completion, softness, gradientLayer, invert`; Roughen Edges
  `ec.stylize.roughenedges`: `edgeType, edgeColor, border, edgeSharpness, fractalInfluence, scale,
  evolution, randomSeed`; Directional Smear (CC Smear) `ec.distort.ccsmear`: `from, to, reach,
  radius`; também Smear `ec.distort.smear`, Directional Blur, Motion Tile, Threshold, Channel
  Threshold, Posterize, Colorama, Multi-Tone Tint, Palette Map — saída local de `effectcraft-cli exec effect.list` e [docs/effects.md](/home/user/storytold/effectcraft/docs/effects.md)
- Já verificado por nós: erosão com Fractal Noise em Stencil Luma, Colorama Custom com
  `interpolatePalette` false e `cycles` 0.999 (faixas duras), Glow A&B, contorno com o estilo de
  camada Stroke (`layer.style.add {style:"stroke"}`) ou Vegas — [docs/effectcraft-guia.md](/home/user/game-luta-gpt/docs/effectcraft-guia.md), [SKILL.md](/home/user/game-luta-gpt/.claude/skills/effectcraft/SKILL.md)

**Técnica pública de referência (ideias, não código copiado)**
- Dissolve de jogo: ruído → step → alfa; a borda é a faixa onde o ruído fica entre o progresso e
  progresso + largura, pintada com a cor da borda; smoothstep em vez de step para antisserrilhar;
  largura maior = brasa lenta, menor = queima seca; variante com segunda faixa (queimado → cinza)
  — visto só em trechos de busca de [godotshaders 2D dissolve with burn edge](https://godotshaders.com/shader/2d-dissolve-with-burn-edge/), [gamedev.net shaderlab](https://gamedev.net/shaderlab/14-sprite-dissolve-effect/), [Codrops/three.js](https://tympanus.net/codrops/?p=86603), [Mirza Beig, Unity dissolve parte 2](https://mirzabeig.substack.com/p/unity-tutorial-dissolve-shader-part-2)
- Jump Flooding Algorithm (Rong & Tan): campo de distância aproximado em log2(n) passadas (passo
  começa em metade da resolução e cai pela metade), depois distância até a semente guardada; erros
  pequenos e raros — [Demofox](https://blog.demofox.org/2016/02/29/fast-voronoi-diagrams-and-distance-dield-textures-on-the-gpu-with-the-jump-flooding-algorithm/), [paper i3D 2006](https://www.comp.nus.edu.sg/~tants/jfa/i3d06.pdf)

### Inferences

Esboço de cada plug-in (estimativas de código são minhas, comparadas ao exemplo de ~100 linhas;
todas em Rust puro, sem crates, ABI 1 salvo onde dito):

| # | Plug-in | O que o embutido já faz | O que falta (e justifica o plug-in) | Algoritmo | Parâmetros (manifesto) | Código |
|---|---|---|---|---|---|---|
| 1 | **Erosão anime com borda luminosa** | Fractal Noise + Stencil Luma corta em pedaços de borda dura; Gradient Wipe corta por um mapa (precisa de outra camada); Roughen Edges pinta a borda **do alfa**, não a frente da erosão | a faixa colorida (1 ou 2 cores, chapada) exatamente na frente que avança, com intensidade > 1 para o Glow seguinte pegar; viés de direção (a erosão começa na ponta do golpe) | por pixel: des-pré-multiplica; `n = fbm(hash-value-noise(x/escala, y/escala, semente), oitavas)` com evolução = tempo×velocidade (ou loop com 2 ruídos misturados); `m = n + viés·dot(p−ponto, dir)/raio` (opção: `m` = luminância ou alfa da própria camada, útil em imagem gerada); `m < prog` → alfa 0; `m < prog+w1` → cor da borda 1; `m < prog+w1+w2` → cor 2; senão mantém; borda dura (step) ou 1 px de smoothstep; re-pré-multiplica | `progresso` 0-100, `escala`, `oitavas`, `semente`, `velocidadeEvolucao`, `fonte` (popup: ruído/luma/alfa), `ponto` (point), `direcao` (angle), `viés`, `larguraBorda1`, `corBorda1`, `larguraBorda2`, `corBorda2`, `intensidadeBorda` (0-400 %), `bordaDura` (checkbox) | 180-280 linhas |
| 2 | **Rampa em degraus / gradient map de N faixas** | Colorama Custom com interpolação desligada faz isso (verificado); Posterize; Multi-Tone Tint (suave, 5 tons); Apply Color LUT | só conveniência: cada parada vira um `color` + `slider` de posição de verdade, que o modelo parametrizado consegue expor (a paleta do Colorama é uma string `"0:r,g,b 0.6:..."`; não conferi se `essential.addProperty` a aceita) | entrada = luminância, alfa ou canal; `t` → procura a última parada com `pos ≤ t` → cor chapada; opção "alfa também em degraus" (alfa < limiar = 0) | `entrada` (popup), `nFaixas` 2-8, `pos1..8`, `cor1..8`, `alfaEmDegraus`, `limiarAlfa` | 90-140 linhas |
| 3 | **Contorno cel em volta do alfa, largura variável** | Stroke de estilo de camada (largura fixa, verificado), Minimax no alfa + cor por baixo, Vegas no canal alfa | largura que muda ao longo da silhueta (mais grossa embaixo/atrás do movimento, tremida por ruído), contorno em 2 faixas (claro por dentro, escuro por fora), só por fora / por dentro | campo de distância até a borda do alfa (limiar 0.5): força bruta num raio R ≤ ~12 px é simples; acima disso JFA (log2 passadas); `larg(p) = base·(1 + varRuido·ruído(p)) · (1 + varDir·max(0, dot(normal, dir)))`, normal = gradiente do alfa; fora e `d < larg` → cor de fora; dentro e `d < largInterna` → cor de dentro; composição por baixo do original | `largura`, `variacaoRuido`, `escalaRuido`, `semente`, `direcao` (angle), `variacaoDirecao`, `corFora`, `corDentro`, `larguraDentro`, `posicao` (popup fora/dentro/centro), `limiarAlfa` | 200-320 linhas (+80 se JFA) |
| 4 | **Smear / estiramento em linhas de velocidade** | CC Smear (deforma de um ponto a outro), Smear, Directional Blur (borrado, não anime), Echo (cópias), motion blur de obturador | o "smear frame" do anime: a forma esticada para trás em **listras de borda dura** de comprimento variado, sem borrar | para cada pixel, anda até `L(p)` px no sentido −dir e guarda o máximo do alfa (filtro de máximo em linha = rastro duro); `L(p) = comprimento · ruído1D(coordenada perpendicular·frequência, semente)` → listras; cor do rastro = cor da origem ou uma cor fixa, opacidade em degraus ao longo do comprimento; opção ABI 2: direção e comprimento do deslocamento entre `prev` e o quadro atual | `direcao` (angle), `comprimento`, `frequenciaListras`, `variacao`, `semente`, `degraus` (1-4), `corRastro`, `usarCorOriginal`, `usarMovimento` (ABI 2) | 160-260 linhas |
| 5 | **Toon shading de simulação macia (2-3 faixas)** | Cartoon (`shadingSteps` 2-4, `shadingSmoothness` ~0), Posterize, Threshold/Channel Threshold, Colorama | luz falsa: o lado da fumaça/fogo voltado para uma direção de luz recebe a faixa clara e o oposto a sombra, como pintura de anime, mais silhueta de alfa dura | alfa → limiar duro (silhueta); normal 2D = gradiente do alfa borrado (caixa 3-5 px dentro do plug-in); `luz = dot(normal, dirLuz)` + densidade; quantiza em 2-3 faixas com cores do miolo/meio/sombra; opcional faixa de borda (rim) onde o alfa cai | `limiarAlfa`, `direcaoLuz` (angle), `pesoLuz`, `raioSuavizacao`, `nFaixas` 2-3, `corClara`, `corMeio`, `corSombra`, `corRim`, `larguraRim` | 180-260 linhas |

- Prioridade sugerida: **1 > 3 > 4 > 5 > 2**. A 1 resolve a falta mais citada (erosão com borda
  luminosa) e só dá com várias camadas hoje; a 2 é quase redundante com o Colorama já dominado.
- Plug-ins que desenham fora da área (contorno 3, smear 4, brilho) precisam de **Grow Bounds antes
  deles na pilha**, ou de a camada já ter folga (comp maior, precomp do tamanho da tela). Isso
  contorna "não aumenta a área da camada" sem mudar o ABI.
- "Sem camada como parâmetro": a erosão 1 usa o próprio ruído interno ou a luma/alfa da própria
  camada; se for preciso um mapa externo (uma imagem gerada de rachaduras), ou se usa o Gradient
  Wipe embutido (que aceita `gradientLayer`), ou se põe o mapa na própria camada (precomp com o mapa
  num canal) — ou se escreve um helper (abaixo).
- Risco: o orçamento de fuel é "proporcional ao tamanho do quadro" sem número publicado; filtros
  com laço de raio grande (contorno por força bruta, smear de 200 px) podem estourar e deixar o
  quadro igual. A versão DAGLOW/Glow Lab avisa exatamente isso no README (ver seção 5). Mitigar com
  JFA/passadas separáveis e quadros de 256-512 px (o nosso tamanho de folha).
- **Rota sem Rust: helper externo em Python.** O protocolo de helper é JSON por linha em
  stdin/stdout mais um arquivo de quadros em `/dev/shm` (RGBA f32 pré-multiplicado); o helper
  declara manifestos de API 2 (pode ter `layer`, `text`, `point3`), responde `padding` (cresce a
  área sozinho) e pode pedir `layer_input` (outra camada) e `input_at` (outro tempo) — [docs/external-helpers.md](/home/user/storytold/effectcraft/docs/external-helpers.md).
  Com numpy+cv2 já instalados, os 5 efeitos acima viram ~50-150 linhas de Python cada, sem instalar
  o alvo wasm32. Custo: precisa pôr o programa `effectcraft-helper-<nome>` em `helpers/` (ou em
  `EFFECTCRAFT_HELPER_PATH`) e rodar `effect.helpers.scan` (ou `EFFECTCRAFT_HELPERS=1`) antes de
  montar o projeto; o `.ecproj` passa a depender do helper para renderizar; mais lento que WASM por
  causa da troca por processo (estimativa minha, não medida).
- Licença: o que nós escrevemos é nosso; manter "MIT OR Apache-2.0" como o exemplo deixa publicar
  no catálogo se quisermos. Nada disso entra no APK (são ferramentas de fabricação da folha).

### Gaps
- Não medi o fuel real nem a velocidade do wasmi em quadros de 256-512 px (nada foi compilado).
- Não conferi se `essential.addProperty` aceita a paleta do Colorama ou parâmetros de plug-in
  (deveria, pois são propriedades comuns segundo a doc, mas não testado).
- `effect.helpers.scan` pela linha de comando sem tela com um helper Python: não testado; o doc diz
  que agentes confinados com `--automation-read-root/--automation-write-root` não podem rodá-lo.

## 2. Equivalentes abertos dos plug-ins pagos do After Effects

### Takeaway
Nenhum plug-in pago (Saber, Trapcode, Deep Glow, Optical Flares, Sapphire) roda no EffectCraft
nem no Linux sem a licença do fabricante; o que existe aberto são **técnicas** bem documentadas
(brilho multiescala/dual-Kawase, glare com estrias do Blender, dissolve com borda) e
**ferramentas externas** que rodam sem tela sobre a sequência PNG. As já instaladas aqui (ffmpeg,
ImageMagick 6, Python+OpenCV) bastam para passos de acabamento; G'MIC, Natron e Blender são
opções licenciadas como ferramenta, mas não estão instaladas. Código do ShaderToy (CC BY-NC-SA por
padrão) e de Natron/openfx-misc/Blender (GPL) não pode ser copiado para um plug-in nosso de licença
permissiva; usar os programas como etapa externa é outra coisa.

### Cited Findings

**Os pagos / fechados**
- Saber (Video Copilot) é grátis, mas é plug-in de After Effects para Windows e Mac (CS5+); não há
  versão Linux/OFX — [Video Copilot blog](https://videocopilot.net/blog/2016/03/new-plug-in-saber-now-available-100-free), [tutorial 159](https://www.videocopilot.net/tutorials/saber_plug-in), [CG Channel](https://www.cgchannel.com/2016/03/video-copilot-unveils-saber/)
- Plug-ins de After Effects (`.aex`/`.plugin`) não rodam no EffectCraft ("Different ABI: After
  Effects SDK plug-ins can't run") — [docs/plugins.md](/home/user/storytold/effectcraft/docs/plugins.md); `.ffx` e `.aep` também não abrem — [docs/gaps.md](/home/user/storytold/effectcraft/docs/gaps.md)
- Deep Glow é comercial (aescripts, US$ 49,95); controles: máscara de entrada por alfa/luma,
  proporção, raio, aberração cromática, qualidade de downsample/blur; nenhuma implementação aberta
  do algoritmo dele foi encontrada; existem macros "glow exponencial" para Resolve/Fusion
  (DreamGlow, xglow) sem licença confirmada — só trechos de busca de [aescripts Deep Glow](https://aescripts.com/deep-glow) e [aescripts learn](https://aescripts.com/learn/best-after-effects-glow-plugins-deep-glow-vs-optical-glow)
- Ponte OpenFX: o helper não oficial **ec-extension-ofx** (MIT OR Apache-2.0, cabeçalhos OpenFX
  BSD-3) roda plug-ins OFX com caminho de CPU dentro do EffectCraft como efeitos comuns; testado em CI
  no Linux com um plug-in de teste; RSMB, Twixtor e Sapphire (~290 efeitos) "carregam e renderizam"
  num build em processo, com reteste pelo helper pendente; pasta Linux `/usr/OFX/Plugins` — [README ec-extension-ofx](https://github.com/glumbis/ec-extension-ofx) (lido via raw.githubusercontent.com)
- O nosso build já anuncia `"helpers": true` (o README do ec-extension-ofx ainda manda usar um
  branch próprio; a doc do EffectCraft 0.23.1 já traz o protocolo e cita o helper) — saída local de `effect.plugins.list`; [docs/external-helpers.md](/home/user/storytold/effectcraft/docs/external-helpers.md)

**Ferramentas abertas e licenças**
- **openfx-misc** (os plug-ins OFX do Natron: Glow, blur, keyers, etc.) é **GPL-2** (LICENSE lido) —
  [openfx-misc LICENSE/README](https://github.com/NatronGitHub/openfx-misc)
- **Natron** é GPL-2.0-or-later; `NatronRenderer` roda projetos e scripts Python sem tela, por ssh,
  ex.: `NatronRenderer -i MyReader ~/pictures.png -w MyWriter rendered###.exr` — [natrongithub.github.io](https://natrongithub.github.io/), [Natron docs: execução](https://natron.readthedocs.io/en/rb-2.5/devel/natronexecution.html), [Wikipedia](https://en.wikipedia.org/wiki/Natron_(software))
- **G'MIC**: código C++ parte sob CeCILL-C v1.0 (tipo LGPL) e parte CeCILL v2.1 (compatível com
  GPL); desde a 2.2 o núcleo libgmic (todos os filtros) é CeCILL-C e pode ser integrado em software
  não-GPL; só o G'MIC-Qt fica CeCILL — [README do G'MIC](https://github.com/GreycLab/gmic/blob/master/README) e COPYING (lidos via GitHub), [pixls.us G'MIC 2.2](https://pixls.us/blog/2018/02/g-mic-2-2); gmic.eu não abriu (ENOTFOUND)
- **ImageMagick**: licença própria quase idêntica à Apache-2.0, uso comercial permitido com
  atribuição — [SPDX ImageMagick](https://spdx.org/licenses/ImageMagick)
- **Blender** é GPL (fonte GPL-2+, binários GPL-3+; Cycles Apache-2.0); scripts Python publicados
  para ele têm de ser GPL — [blender.org/about/license](https://www.blender.org/about/license/) (lido)
- Glare do compositor do Blender: tipos Bloom, Fog Glow (mais físico, mais lento, não iterativo),
  Streaks (estrias a partir dos claros, número e ângulo), Simple Star, Ghosts; `size` 1 cobre a
  imagem toda — [manual Glare Node](https://docs.blender.org/manual/en/latest/compositing/types/filter/glare.html), [PR Bloom #119128](https://projects.blender.org/blender/blender/pulls/119128)
- **ShaderToy**: por padrão os shaders são CC BY-NC-SA 3.0 (não comercial, compartilha igual) salvo
  se o autor disser outra coisa; ports herdam a licença — só trechos de busca ([godotshaders](https://godotshaders.com/shader/seascape-shader/), [gist](https://gist.github.com/djhoese/f4dfa3b423a88739636dc9a95e58e177)); shadertoy.com/terms não abriu (403/ENOTFOUND)
- Brilho multiescala aberto como técnica: dual-Kawase (Bjørge, SIGGRAPH 2015): limiar dos claros,
  desce uma cadeia de mips e sobe somando cada nível ao maior (13 amostras na descida, tenda de 9 na
  subida em uma implementação) — [notas ARM SIGGRAPH 2015](https://community.arm.com:443/cfs-file/__key/communityserver-blogs-components-weblogfiles/00-00-00-20-66/siggraph2015_2D00_mmg_2D00_marius_2D00_notes.pdf), [Frost Kiwi](https://blog.frost.kiwi/dual-kawase/), [luma.gl bloom](https://luma.gl/docs/api-reference/shadertools/shader-passes/bloom)
- Filtros do ffmpeg 6.1.1 instalado (lista local `ffmpeg -filters`): `gblur`, `dilation`,
  `erosion`, `morpho`, `lut1d`, `lut3d`, `pseudocolor`, `tmix`, `lagfun` (rastro que só apaga
  devagar), `colorkey`, `chromakey`, `lumakey`, `maskfun`, `edgedetect`, `xfade` — saída local; o
  build é `--enable-gpl` (configuração local)

### Inferences

| Pago | O que ele dá no anime | Equivalente que temos/podemos ter | Como entra | Licença p/ nós | Esforço |
|---|---|---|---|---|---|
| Deep Glow / Sapphire Glow / Starglow (halo) | brilho largo e quente sem lavar o miolo | (a) 2-3 Glow embutidos empilhados com raios ×4 (aproxima multiescala); (b) plug-in WASM dual-Kawase próprio (~200 linhas, + Grow Bounds); (c) WASM da comunidade: Glow Lab/DAGLOW (MPL-2.0) ou Craft Glow (MIT), ver seção 5 | dentro do EffectCraft | (a)(b) nossos; MPL-2.0 permite uso comercial, só alterações nos arquivos MPL voltam abertas | (a) horas; (b) 1-2 dias; (c) compilar + testar |
| Shine / Light Rays / Starglow (raios) | raios a partir do miolo | embutidos Radial Light Burst, Light Rays (CC), Fast Radial Blur Brightest (já no guia); Blender Glare Streaks como etapa externa | dentro / externo | Blender GPL como ferramenta: saída é nossa | baixo |
| Saber | núcleo branco + halo colorido em traço | Stroke/Beam/forma com taper + Glow A&B (já dominado); `ec.generate.beam` com `softness` 0 | dentro | nativo | baixo |
| Trapcode Particular | partículas 3D | Particle Emitter 2D, Particle Playground, CC Particle World (vê câmera desde #397) | dentro | nativo | baixo |
| Optical Flares | flare de lente | Lens Flare embutido (cara de foto, ruim para anime, segundo o nosso guia); dispensável | — | — | — |
| Sapphire (pacote) | glows, dissolves, glints | rodaria pelo ec-extension-ofx só com licença Sapphire OFX para Linux (paga) | helper OFX | pago, fora de questão | — |
| Erosão com borda (Sapphire Dissolve, etc.) | quebra com borda luminosa | plug-in 1 da seção 1, ou passo externo Python | dentro / externo | nosso | 1-2 dias |

- Passos externos sobre a sequência PNG (depois do render do EffectCraft, antes do
  `efeito_video.py`) que rodam **hoje sem instalar nada**: Python+numpy+OpenCV (contorno por
  `cv2.distanceTransform`, erosão com borda, rampa de N faixas, glow multiescala por pirâmide
  `cv2.pyrDown/pyrUp`); ImageMagick 6 (`-morphology`, `-clut` com uma faixa de cores para gradient
  map, `-blur`); ffmpeg (`lut1d`/`pseudocolor` para rampa, `gblur` + `blend=addition` para glow,
  `dilation` para engrossar o alfa). Isso cabe como mais uma opção do nosso script, sem mexer no
  EffectCraft. O OpenCV é Apache-2.0 nas versões 4.5+ (conhecimento geral, não conferido nesta
  sessão).
- GPL/CeCILL **como ferramenta externa** (G'MIC CLI, NatronRenderer, Blender `-b`) não contamina
  as imagens que saem nem o jogo: não distribuímos o programa. O que não pode é **copiar código**
  deles (nem de ShaderToy NC) para dentro de um plug-in nosso que queiramos MIT, nem para o
  EffectCraft. Reimplementar a técnica a partir de artigo/descrição é permitido.
- Natron via OFX: o caminho mais "profissional" (keyers e Glow do openfx-misc dentro da linha do
  tempo do EffectCraft) seria ec-extension-ofx + binários openfx-misc; precisa instalar duas coisas
  e o helper ainda tem reteste pendente. Valor baixo frente ao que o EffectCraft já tem embutido
  (Glow, Keylight, Extract, Linear Color Key, Unmult).
- Regra do dono: tudo acima é **acabamento** (brilho, rampa, contorno, erosão, tempo) sobre arte
  gerada; nenhum desenha a forma do efeito. Os geradores procedurais (Lens Flare, Fractal) seguem a
  exceção que o dono ainda precisa decidir (fim do `docs/effectcraft-guia.md`).

### Gaps
- Não encontrei implementação aberta do Deep Glow nem descrição pública do seu algoritmo de queda
  exponencial; "inverse-square" do Craft Glow é afirmação do README dele, não verificada.
- Texto oficial de termos do ShaderToy não abriu; o CC BY-NC-SA 3.0 vem só de trechos de terceiros.
- Algoritmo interno do Fog Glow/Streaks do Blender não está no manual (só no código-fonte GPL).
- Licença do OpenCV instalado (5.0.0) não conferida nesta sessão.

## 3. Importar footage/flipbook pronto no EffectCraft

### Takeaway
Tudo que é preciso existe e é comando: importar sequência PNG/EXR como um item com fps próprio,
tirar o fundo (Unmult para elemento brilhante em preto, Screen Color Key = Keylight para fundo
verde, Extract/Linear Color Key), remapear o tempo e por cima somar o acabamento procedural. Isso
é a forma mais fiel à regra "o desenho vem de imagem/vídeo gerado": o EffectCraft só monta, corta,
cola, tinge e dá brilho.

### Cited Findings
- `file.import {"paths": ["/shots/shot_0001.png"], "sequence": true, "frameRate": 24}` importa a
  série numerada inteira como um item "Image Sequence"; sem `sequence`, um arquivo numerado traz a
  série toda e uma pasta traz cada série como sequência; buraco na numeração segura o quadro
  anterior; formatos: PNG, JPEG, TIFF, OpenEXR, BMP… — [docs/footage.md](/home/user/storytold/effectcraft/docs/footage.md)
- `file.replaceFootage {"path": …, "sequence": true}` troca a fonte mantendo tempo e
  interpretação; "Assume this frame rate" muda a duração (250 quadros a 25 fps = 10 s) — [docs/footage.md](/home/user/storytold/effectcraft/docs/footage.md)
- Keyers embutidos: Screen Color Key `ec.keying.keylight` (56 parâmetros: `screenColour`,
  `screenGain`, `screenBalance`, `despillBias`, `screenMatte/clipBlack|clipWhite|screenShrinkGrow|
  screenSoftness`, máscaras interna/externa, correção de cor), Extract `ec.key.extract`
  (`channel, blackPoint, whitePoint, …Softness`), Linear Color Key, Color Difference Key,
  Difference Matte, Unmult `ec.key.unmult`, Advanced Spill Suppressor, Matte Choker, Simple Choker,
  Refine Hard/Soft Matte — [docs/effects.md](/home/user/storytold/effectcraft/docs/effects.md) e `effect.list` local
- Unmult para tirar o preto de imagem gerada já está verificado por nós — [SKILL.md](/home/user/game-luta-gpt/.claude/skills/effectcraft/SKILL.md)
- Tempo: `layer.enableTimeRemap {layers, value}`, `layer.timeStretch`, `layer.timing`,
  `comp.settings {preserveFrameRate}`, precomps (`layer.precompose`, `layer.addItem`); efeitos Posterize
  Time, Echo, Time Displacement, Timewarp — lista de comandos local (`exec --list --schemas`) e [docs/effectcraft-guia.md](/home/user/game-luta-gpt/docs/effectcraft-guia.md)
- Nota: Posterize Time não segura transform da camada; usar `posterizeTime(n)` em expressão — [SKILL.md](/home/user/game-luta-gpt/.claude/skills/effectcraft/SKILL.md)

### Inferences
- Receita de "elemento gerado + extras procedurais" (por golpe):
  1. vídeo/sequência gerada do efeito em fundo preto (fogo, energia) ou verde → exportar PNG
     numerados (ffmpeg) → `file.import` com `sequence:true` e o fps real;
  2. camada da sequência: Unmult (preto) ou Screen Color Key + Advanced Spill Suppressor (verde);
     Simple Choker/Matte Choker para firmar a borda;
  3. tempo: `layer.enableTimeRemap` e chaves de remapeamento em degrau (`posterizeTime(12)` na
     expressão do Time Remap) para tirar 12 quadros "de anime" de um vídeo de 24-30 fps e segurar
     o quadro de impacto (hit-stop de 2-3 quadros);
  4. acabamento: Colorama em faixas (ou plug-in 2), Glow A&B, erosão (Fractal Stencil Luma ou
     plug-in 1) no fim da vida, contorno (Stroke de estilo de camada ou plug-in 3);
  5. extras procedurais por cima, se o dono aceitar: Particle Emitter 2D para brasas, Radio Waves.
- Flipbooks prontos de terceiros (packs de sprite de VFX) entram igual (PNG numerado); o cuidado
  é a licença de cada pack, que não foi pesquisada aqui.

### Gaps
- Não testei Keylight nem time remap pela linha de comando nesta sessão (Unmult, sim, em sessões
  anteriores).
- Não conferi se um Media Replacement de modelo aceita sequência de imagens (o exemplo do doc é
  `.mp4`).

## 4. Presets e modelos por personagem

### Takeaway
Os dois mecanismos existem e já servem ao "uma linha de tabela por personagem": presets
(`.ecpreset`) guardam um conjunto de efeitos ajustados para reaplicar; modelos Essential Graphics
(`.ectemplate`) expõem controles nomeados — inclusive **Media Replacement**, que troca a imagem ou
vídeo gerado — e renderizam direto com `render --template T --values V.json`. O desenho proposto:
um `.ectemplate` por tipo de efeito, um `values.json` por personagem e golpe.

### Cited Findings
- `anim.savePreset {path (.ecpreset), name?}` "saves the selected properties/effects";
  `anim.applyPreset {path | preset, layers?}`; `anim.applyBuiltinPreset`, `anim.builtinPresets
  {category?}`, `anim.applyRecentPreset` — lista de comandos local (`exec --list --schemas`)
- 76 presets próprios (8 de texto, 23 transições, 23 fundos, 22 comportamentos: wiggle, shake,
  float, flicker, strobe…); `.ffx` do After Effects não é lido — [docs/gaps.md](/home/user/storytold/effectcraft/docs/gaps.md) (gap 7)
- Essential Graphics: `essential.addProperty {comp?, layer?, path?, name?, group?}`,
  `essential.addMedia {layer, name?, group?}`, `essential.addGroup`, `essential.addComment`,
  `essential.exportTemplate {comp?, path (.ectemplate), name?}`, `essential.importTemplate`,
  `essential.set`, `essential.list`, `essential.canAdd {layer, path, as?} → {ok, type?, reason?}`;
  biblioteca de modelos `templates.list/create/saveAs (embedFootage, embedLimitMB 256)` — lista de comandos local
- `effectcraft-cli render --template lt.ectemplate --values values.json --out 'title_[#####].png'`;
  `values.json` = `{"Title":"John Smith","Bar Color":"#00c080","Bar Opacity":50}` (nomes sem
  diferença de maiúsculas); controle Media Replacement recebe caminho de arquivo
  (`{"Clip":"/in/clipB.mp4"}`), importado e posto como item; valores inválidos falham antes do
  render; `render-frame` aceita as mesmas opções; não combina com `--project/--comp` — [docs/agents.md](/home/user/storytold/effectcraft/docs/agents.md)
- Modelo parametrizado já verificado por nós; "controle sobrescrito perde as chaves: exponha só
  valores parados" — [docs/effectcraft-guia.md](/home/user/game-luta-gpt/docs/effectcraft-guia.md)
- Controles disponíveis para a camada de controle: Checkbox, Color, Dropdown Menu, Layer, Point,
  Slider Control (`ec.control.*`) — [docs/effects.md](/home/user/storytold/effectcraft/docs/effects.md)
- `.ectemplate` não é `.mogrt` (sem intercâmbio com Premiere) — [docs/gaps.md](/home/user/storytold/effectcraft/docs/gaps.md) (gap 23)

### Inferences
- **Desenho do sistema por personagem** (proposta):
  - um script `art/vfx/effectcraft/<efeito>_ec.py` monta a comp-base com uma camada nula
    "Controles" contendo: `Cor do miolo` (Color), `Cor da borda` (Color), `Cor do halo` (Color),
    `Tamanho` (Slider %, lido por expressão na escala), `Semente` (Slider, lido por
    `seedRandom(...)` e pelos `randomSeed` de partículas/ruído), `Variante` (Dropdown: forma A/B/C,
    liga a opacidade de 3 camadas alternativas por expressão), `Velocidade` (Slider, multiplica o
    tempo por expressão `time*k`), `Arte` (Media Replacement com a imagem/sequência gerada do
    personagem);
  - `essential.addProperty` / `essential.addMedia` em cada um, `essential.exportTemplate` →
    `art/vfx/effectcraft/modelos/<efeito>.ectemplate`;
  - uma pasta `variantes/<efeito>/<personagem>.json` com só o que muda
    (`{"Cor do miolo":"#fff6d0","Cor da borda":"#ff7a1a","Tamanho":110,"Semente":7,"Variante":2,"Arte":"…/kael_fogo_[####].png"}`);
  - um laço em Python chama `effectcraft-cli render --template … --values …` para cada personagem e
    entrega a pasta ao `efeito_video.py`. Cada variante é reproduzível e diferível em Git.
- **Como o dono pede uma variante**: numa frase com os campos da tabela ("faísca do forte da
  Kael: miolo branco, borda roxa, 20 % maior, variante 2"); quem atende só cria/edita o
  `values.json`, renderiza, grava o vídeo no jogo e mostra. Uma tabela em `docs/` com uma linha por
  personagem (cores em hex, tamanho, semente, variante, arte) vira a fonte única.
- Presets: usar `.ecpreset` para "pilhas de acabamento" (Glow A&B laranja, erosão de borda dura,
  rampa de fogo) e aplicar por `anim.applyPreset {path, layers}`; mas como nossos scripts já são
  código, a função Python que adiciona a pilha faz o mesmo papel; o preset vale para quem abrir o
  projeto na interface.
- Cuidado: valores animados não podem ser controle sobrescrito (perde chaves); animar por
  expressão a partir do controle parado (ex.: `escala = Tamanho * curva(time)`).

### Gaps
- `anim.savePreset` salva "as propriedades/efeitos selecionados": não verifiquei qual comando
  seleciona pela linha de comando sem tela (a interface usa clique).
- Não testei Dropdown Menu Control exposto no modelo nem Media Replacement com sequência PNG.

## 5. Comunidade / "Plug-in Library" do EffectCraft

### Takeaway
A Plug-in Library é um catálogo único revisado (`index.json` num repositório `storytold/effectcraft-plugins`)
que o app lê para instalar `.wasm` conferidos por SHA-256 e mostrar prévia; esse repositório **não
abriu** (404 público), e o exemplo local de catálogo tem a lista vazia. Fora dele, há ~20
repositórios no GitHub com plug-ins para EffectCraft, alguns de glow, todos jovens e não
revisados; dois têm licença usável (MIT, MPL-2.0) e servem mais como referência de código do que
como solução pronta para o nosso estilo.

### Cited Findings
- Plug-in Library: Settings ▸ Plug-in Library; só efeitos WASM; catálogo único em
  `storytold/effectcraft-plugins` (PR revisado); `library.refresh` lê o catálogo, `library.install`
  baixa, confere SHA-256/tamanho e carrega; vai para `<Plug-ins>/Library/<id>/<versão>/`;
  `library.preview` renderiza o efeito em quadro de amostra 320×180, 2 s a 12 fps, sem instalar;
  `library.missing` lista efeitos que o projeto usa e não estão instalados — [docs/plugins.md](/home/user/storytold/effectcraft/docs/plugins.md), [docs/publishing-plugins.md](/home/user/storytold/effectcraft/docs/publishing-plugins.md)
- O exemplo de catálogo local (`examples/plugin-catalog/index.json`) é `{"schema": 1, "packages": []}`;
  a entrada de exemplo é o próprio posterize-bands; revisão exige repositório público com licença
  aberta e build reproduzível — [examples/plugin-catalog](/home/user/storytold/effectcraft/examples/plugin-catalog/README.md)
- `https://github.com/storytold/effectcraft-plugins` deu 404 no WebFetch e no raw do `index.json`
  (main e master); a API do GitHub recusou por permissão da sessão — tentativa local nesta sessão
- Busca no GitHub (api.github.com/search "effectcraft plugin") → 20 repositórios, entre eles:
  `qwertyonek/craft-plugins` (MIT), `maarrsel/MarselFx` (MIT), `momenawdallah1/DAGLOW` (MPL-2.0),
  `ONYX-AE-Creator/Studio-Glow-effectcraft-plugin` (sem licença, "inspirado no Deep Glow"),
  `bits-by-brandon/supermosh-effectcraft-plugin` (GPL-3.0), `kikikikier/effectcraft-textbox-plugin`
  (Apache-2.0) — [busca na API do GitHub](https://api.github.com/search/repositories?q=effectcraft+plugin)
- **CraftPlugins** (qwertyonek, MIT): 8 `.wasm` API v1 prontos (Craft Glow, Chromatic, Camera
  Shake, Displacer, Halftone & Dither, VHS Glitch, Pixel Sort, Anamorphic Flare); o Glow diz ter
  queda "inverse-square (1/r²)", limiar de luminância, halo com aberração cromática e tinta HDR
  — [README craft-plugins](https://github.com/qwertyonek/craft-plugins)
- **Glow Lab / DAGLOW** (MPL-2.0): glow multiescala WASM API v1, código original sem dependências;
  controles Threshold, Softness, Intensity, Radius, Falloff ("baixo = justo, alto = halos largos"),
  Tint, Glow Only; avisa que quadro grande pode estourar o fuel e sair inalterado — [README DAGLOW](https://github.com/momenawdallah1/DAGLOW)
- **Marsel FX** (MIT): "Prism" com 14 módulos visuais (Film Looks, Deep Glow, Bloom, RGB Split…),
  "Motion" com 6, presets de FilmCraft; instalador só para Windows — [README MarselFx](https://github.com/maarrsel/MarselFx)

### Inferences
- Para nós o mais útil da comunidade é **um glow multiescala pronto** (Craft Glow MIT ou Glow Lab
  MPL-2.0): compilar do fonte (ou usar o `.wasm` publicado, depois de conferir), carregar com
  `effect.plugins.load`, comparar em vídeo com o nosso Glow A&B empilhado. Não usar os sem licença
  (Studio-Glow) nem copiar o GPL-3 para código nosso.
- Nada encontrado é específico de anime (erosão com borda, faixas, contorno variável, smear): esses
  continuam sendo os plug-ins que valeria escrever (seção 1), e poderíamos publicá-los no catálogo.
- Qualidade dos repositórios da comunidade: READMEs com emojis e "production-grade" sem medições;
  tratar como não verificados até rodar.

### Gaps
- Conteúdo real do catálogo oficial (`index.json`) não confirmado: repositório inacessível (404).
- Não rodei `library.refresh` (escreveria no diretório de configurações e baixaria da rede, fora
  das restrições desta tarefa).
- Estrelas, datas e atividade dos repositórios da comunidade não lidas (API por repositório
  recusada nesta sessão).
