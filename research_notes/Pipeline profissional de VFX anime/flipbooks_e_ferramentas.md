# Flipbooks de VFX anime: do After Effects ao jogo, e ferramentas alternativas

Nota de método (10/11/2026): na primeira rodada nenhuma página foi aberta direto. Depois que a rede foi aberta, tentei de novo com WebFetch: **só o GitHub (github.com e raw.githubusercontent.com) abriu**; jangafx.com, www.sidefx.com, docs.godotengine.org, codemanu.itch.io, effekseer.github.io e realtimevfx.com continuaram falhando com ENOTFOUND (esses domínios precisam ser liberados na rede da sessão). As fontes marcadas **[lida]** foram abertas e lidas; **todas as outras foram vistas só pelos resumos do WebSearch**, então o texto citado é o resumo do buscador, não a página lida inteira. Onde a fonte é agregadora, fórum ou marketing, isso está dito. Coisas que sei de antes mas não confirmei nesta sessão ficam em "Inferências" ou "Lacunas", marcadas como não verificadas.

## (A) Como estúdios levam efeitos do After Effects para o jogo (PNG com alfa, grade, fps, atlas, recorte, pivô, orçamento)

### Takeaway
O caminho padrão é: renderizar sequência PNG com **alfa straight** (não premultiplicado) a partir do AE, e depois montar a folha/atlas com uma ferramenta à parte (TexturePacker, que é pago para uso comercial, ou alternativas livres com linha de comando), ou com um script do AE como o Sheetah. Não achei fonte boa e atual para convenções de fps/grade nem para orçamento de textura no celular; essas partes ficaram como lacuna ou inferência.

### Cited Findings
**Alfa straight x premultiplicado**
- No Output Module do AE, o alfa sai como Straight ou Premultiplied, e a importação tem opções equivalentes em Interpret Footage. Um usuário relatou que PNG premultiplicado gerou halo/vazamento de cor, porque PNG espera alfa straight. A recomendação comum é PNG com alfa straight — [Adobe Community: export image with an alpha channel](https://community.adobe.com/questions-529/export-image-with-an-alpha-channel-but-keep-the-entire-image-60295); [Adobe Community: aeio color bleeding](https://community.adobe.com/t5/after-effects-discussions/aeio-color-bleeding/m-p/6486451/highlight/true) (fórum, visto por resumo)
- Se a sequência importada perde o alfa, a correção é Interpret Footage > Alpha > "Straight - Unmatted". Pegadinha: se o primeiro PNG da sequência não tiver transparência, o AE trata a sequência inteira como sem alfa — [Adobe Community: transparent png sequence doesn't have transparency](https://community.adobe.com/questions-506/transparent-png-image-sequence-from-after-effects-doesn-t-have-transparency-994393); [Adobe Community: imported PNG sequence loses transparency](https://community.adobe.com/t5/after-effects-discussions/imported-png-sequence-loses-transparency/m-p/14200633) (fórum)
- A especificação do OpenEXR diz que a imagem deve ser premultiplicada; isso importa quando a engine espera alfa straight (EXR é formato de 32 bits, não é o nosso caso de PNG) — [Adobe Community: export OpenEXR without premultiplied](https://community.adobe.com/t5/after-effects-discussions/how-to-export-to-open-exr-with-alpha-chanel-without-being-premultiplied/m-p/12004262) (fórum)
- No Godot, um tópico do fórum (Godot 4.5) relata que a transparência parcial sai misturada com preto em capturas, e que a gravação do Movie Maker fica bem mais escura; é o mesmo problema de pré-multiplicação, sem solução confirmada — [Godot Forum: partial transparency blended with black](https://forum.godotengine.org/t/partial-transparency-blended-with-black-in-screenshot-images/128560)

**Scripts do AE que exportam a folha direto**
- **Sheetah – Spritesheet Tools** (aescripts, pago): transforma a composição do AE numa folha única, exporta PNG/TGA/TIF com alfa automático e sugere dimensões ótimas de folha. O preço não apareceu no resumo — [aescripts: Sheetah](https://aescripts.com/sheetah-spritesheet-tools/) (página do vendedor, vista por resumo)
- O próprio Adobe **Animate** (não o AE) tem "Generate Sprite Sheet" para apps e engines — [Adobe Help: create sprite sheet (Animate)](https://helpx.adobe.com/my_ms/animate/desktop/workspace-and-workflow/create-sprite-sheet.html)
- Bodymovin/Lottie, AEUX e "Spritesheet exporter": não apareceram nos resultados (ver Lacunas).

**Empacotadores de atlas**
- **TexturePacker** (CodeAndWeb): a página de licenças lista um cliente de linha de comando; o modo grátis ("Essential") tem só recursos básicos e serve para uso não comercial; uso comercial ou recursos avançados pedem licença paga — [CodeAndWeb: licenses comparison](https://www.codeandweb.com/texturepacker/licenses-comparison); [CodeAndWeb: Is TexturePacker free?](https://www.codeandweb.com/texturepacker/knowledgebase/is-texturepacker-free); [CodeAndWeb: diferença entre o grátis e o pago](https://www.codeandweb.com/texturepacker/knowledgebase/whats-the-difference-between-free-and-paid-texturepacker); [CodeAndWeb: Essential mode em produto comercial](https://www.codeandweb.com/texturepacker/knowledgebase/use-essential-mode-for-commercial-product)
- **free-tex-packer-cli** (odrick): licença **MIT**; instala com `npm install -g free-tex-packer-cli` e roda com `free-tex-packer-cli --project projeto.ftpp --output pasta/` (lê um projeto salvo pelo app gráfico) — [GitHub: odrick/free-tex-packer-cli](https://github.com/odrick/free-tex-packer-cli) **[lida]**. Um agregador dá o último update em junho de 2023 — [relatedrepos](https://relatedrepos.com/gh/odrick/free-tex-packer-cli)
- **free-tex-packer-core** (a biblioteca Node por trás, MIT): opções `allowTrim` (padrão true) e `trimMode` (trim ou crop), `alphaThreshold`, `padding`, `extrude`, `allowRotation` (padrão true — desligar para flipbook), `powerOfTwo`, `width`/`height` (padrão 2048, é o tamanho máximo), `fixedSize`, packers MaxRects/OptimalPacker; exportadores JsonHash, JsonArray, Pixi, GodotAtlas, GodotTileset, Phaser, Spine, Unreal, Unity3D, Cocos2d, Starling, XML etc., ou modelo Mustache próprio. **Não há opção de pivô**: o exemplo de modelo próprio fixa pivô 0,5/0,5, então pivô entra por modelo customizado — [GitHub: odrick/free-tex-packer-core](https://github.com/odrick/free-tex-packer-core) **[lida]**
- **fastpack**: empacotador em Rust que tem app desktop (Tauri) e **CLI headless**, feito para substituir o TexturePacker com código aberto — [docs.rs: fastpack 0.29.4](https://docs.rs/crate/fastpack/0.29.4)
- **libgdx texturepacker**: ferramenta de linha de comando e biblioteca para empacotar atlas — [GitHub: tommyettinger/libgdx-texturepacker](https://github.com/tommyettinger/libgdx-texturepacker/blob/main/README.md)
- **pixi-packer**: focado em linha de comando e CI, configurado por JSON — [html5gamedevs: sprite packer for pixijs](https://html5gamedevs.com/topic/17636-sprite-packer-for-pixijs) (fórum antigo)
- **LogicAtlas CLI**: grátis, só para Windows — [SourceForge: LogicAtlas CLI](https://sourceforge.net/app/logicatlas-cli/)
- **ShoeBox**: muito citado como alternativa grátis, mas uma análise diz que ele falha em 9-patch e em automação por CLI — [AlternativeTo: TexturePacker alternatives](https://alternativeto.net/software/texturepacker/) (agregador)
- **Aseprite** tem CLI documentada para exportar folhas — [Aseprite CLI (espelho de aseprite.org/docs/cli)](https://backiee.wasmer.app/https_www_aseprite_org/docs/cli); [Aseprite sprite sheets (mintlify)](https://mintlify.com/aseprite/aseprite/features/sprite-sheets) (só os links apareceram; não li as flags)

**Grade / ordem dos quadros (referências)**
- O Effekseer, ao exportar "como imagem única", arruma os quadros do canto superior esquerdo ao inferior direito, com "X Count" quadros por linha — [Effekseer: Record (referência da ferramenta)](https://effekseer.github.io/Help_Tool/en/ToolReference/record.html). Esse é o arranjo padrão de flipbook que as engines esperam (linhas da esquerda para a direita, de cima para baixo).
- Um exemplo da comunidade com o Niagara Baker usou folha de 256x256 com quadros de 32x32 (grade 8x8) — [Epic: Niagara Flipbook Baker quick start](https://dev.epicgames.com/documentation/unreal-engine/niagara-flipbook-baker-quick-start-guide-in-unreal-engine) (o número vem do resumo de um exemplo da comunidade, não da doc da Epic)
- Existe uma referência geral sobre flipbooks/folhas de textura para VFX em tempo real — [vfxdoc: Flipbooks and Texture Sheets](https://vfxdoc.readthedocs.io/en/latest/textures/flipbooks/) (só o link apareceu; conteúdo não lido)

**Fluxo AE + engine em jogos 2D**
- Fluxo real descrito por um artista: quadros desenhados à mão viram textura de sequência, o brilho e as partículas extras são montados no sistema de partículas do Unity, e o desfoque/glow é aplicado no AE antes de levar ao Unity — [raindrop.io (coleção que cita o caso)](https://raindrop.io/simon81/fx-3209787/search/sort=-sort&page=0&perpage=30&search=%23speedpaint) (fonte indireta, fraca)
- Palestra "Building Great 2D Games with Unity and After Effects" — [Vimeo](https://vimeo.com/88713610) (só o título apareceu; é de ~2014, antiga)
- Um técnico da Synapse Games descreve como criar partículas 2D para jogo de celular no Unity (texto de 2018, antigo) — [Game Developer: creating 2D particle effects in Unity3D](https://gamedeveloper.com/design/creating-2d-particle-effects-in-unity3d)

### Inferências
- Para o nosso pipeline (EffectCraft -> PNG -> script Python), a regra prática é: **PNG com alfa straight**, conferir halo num fundo escuro e num claro, e só premultiplicar (se for preciso) na hora de carregar no jogo, sabendo qual blend a engine usa. Bate com o que os fóruns acima dizem.
- Convenções que circulam no meio de VFX em tempo real (do meu conhecimento prévio, **não confirmadas nesta sessão**): grades potência de 2 (4x4 = 16 quadros, 8x8 = 64), efeito de impacto de anime animado "em 2s ou 3s" (12 ou 8 desenhos/s sobre 24 fps), recorte (trim) por quadro com offset guardado em metadados (TexturePacker, free-tex-packer e Aseprite gravam JSON com frame/sourceSize/spriteSourceSize/pivot), e ImageMagick `montage -tile CxL -geometry +0+0 -background none` para juntar uma grade simples. Nosso script Python já faz o papel de montagem.
- Limite de 4096 px de lado é compatível com GPUs de celular modernas, mas o custo em memória de RGBA8 sem compressão é de 64 MB por folha 4096², o que pesa no celular. Isso é conta, não fonte.

### Lacunas
- Não achei fonte atual sobre **Bodymovin/Lottie** (é exportador vetorial JSON, não de folha), **AEUX** (ponte Figma/Sketch -> AE, não exporta folha), nem sobre um script chamado "Spritesheet exporter" do aescripts. O que está entre parênteses é conhecimento prévio, não verificado.
- Sem fonte para convenção de fps/quadros (12-24 fps) em flipbooks de jogo de luta, nem para orçamento de memória de textura em celular. Seria preciso buscar no realtimevfx.com ou nas docs Android/ARM (ASTC/ETC2).
- Preço do Sheetah e do TexturePacker (valores) não apareceram nos resumos.
- Pivô por quadro no free-tex-packer só via modelo próprio; não conferi as opções do fastpack.

## (B) Ferramentas alternativas para gerar flipbooks: preço/licença, CLI/headless em Linux, serve para o visual anime/cel?

### Takeaway
Das ferramentas "de gerar flipbook", só **Houdini** (hython/hbatch), **Blender** (`blender -b`, com ressalvas para EEVEE/Grease Pencil sem tela) e **Godot** (`--write-movie`) têm caminho documentado de render por linha de comando; EmberGen, Effekseer e Pixel FX Designer são de interface gráfica. Para o visual anime/cel, as de simulação (EmberGen, Niagara, Houdini) puxam para o realista e precisam de tratamento; as de desenho 2D (Toon Boom, CSP, TVPaint, OpenToonz/Tahoma2D, Krita) são o que a animação japonesa usa de verdade, mas são feitas para desenhar quadro a quadro, não para rodar sem pessoa.

### Cited Findings
**JangaFX EmberGen**
- Gera "flipbooks prontos para o jogo, já montados, em segundos"; exporta EXR, PNG, TGA — [JangaFX: EmberGen](https://jangafx.com/embergen/) (página do produto)
- Licença Indie: para pessoas ou estúdios com receita/orçamento abaixo de US$ 1 milhão/ano. Uma página da JangaFX lista US$ 19,99/mês, que vira permanente depois de 18 pagamentos seguidos, presa a uma máquina — [JangaFX: pricing](https://jangafx.com/software/pricing). Em 2021 a licença perpétua indie caiu para US$ 199,99 — [CG Channel (2021)](https://www.cgchannel.com/?p=129429); [80.lv (2021)](https://80.lv/articles/jangafx-reduces-the-price-of-embergen-for-indie-users) (**antigo**; não confirmei se ainda vale em nov/2026). Um folheto de pré-venda com US$ 24,99/mês é **desatualizado** — [JangaFX brochure](https://jangafx.com/downloads/embergen/EmberGen-Pre-Order-Brochure.pdf)
- Linux: a versão 1.0 (março/2023) saiu para Windows 10+ e, a partir dela, para Linux; pede GPU GTX 1060 ou AMD equivalente — [CG Channel: EmberGen 1.0](https://www.cgchannel.com/2023/03/jangafx-releases-embergen-1-0/). Relatos de usuários no fórum: trava ao abrir no Linux Mint, não funciona no Ubuntu — [JangaFX forum: crashes on launch on Linux](https://forums.jangafx.com/t/embergen-crashes-on-launch-on-linux/2293); [JangaFX forum: problem on Ubuntu](https://forums.jangafx.com/t/problem-on-ubuntu/1923)
- **Não achei CLI/modo batch** para o EmberGen em nenhuma fonte.
- Nenhum resultado fala de preset toon/estilizado no EmberGen.

**JangaFX LiquiGen**
- O FAQ da documentação do LiquiGen fala de exportar flipbook — [docs.jangafx.com: LiquiGen FAQ](https://docs.jangafx.com/liquigen/pages/FAQ.html) (detalhes não vistos). GeoGen não apareceu.

**Effekseer**
- A ferramenta grava o efeito como folha de sprites, GIF, AVI etc. "Exportar como imagem única" cria uma folha com os quadros da esquerda para a direita, de cima para baixo, com "X Count" por linha — [Effekseer: Record](https://effekseer.github.io/Help_Tool/en/ToolReference/record.html); [Effekseer 1.5: Record](https://effekseer.github.io/Helps/15x/Tool/en/ToolReference/record.html)
- Usuário: o Effekseer, grátis, exporta quadros soltos ou folha, com fundo transparente gerado automaticamente — [itch.io post](https://itch.io/post/9143494) (comentário de usuário)
- Tutorial oficial de início rápido — [Effekseer Quick Tutorial](https://effekseer.github.io/QuickTutorial_Tool/QuickTutorial_en.html)
- Licença: o repositório se declara **MIT**; o arquivo de licença da ferramenta repete a MIT do Effekseer (uso, cópia, modificação, venda, inclusive comercial, mantendo o aviso de copyright) e lista as licenças de terceiros embutidas (imgui, spdlog, ufbx etc. em MIT; DirectX Tool Kit em Ms-PL; fonte 源真ゴシック em SIL OFL 1.1). Nada restringe o uso comercial do que sai da ferramenta — [GitHub: effekseer/Effekseer](https://github.com/effekseer/Effekseer) **[lida]**; [LICENSE_TOOL](https://raw.githubusercontent.com/effekseer/Effekseer/master/LICENSE_TOOL) **[lida]**
- Linux: existe editor para Linux 64 bits, que pede "ambiente gráfico compatível com OpenGL 3.2"; abre-se rodando `Tool/Effekseer`. O readme **não fala de linha de comando nem de modo headless** — [readme_tool_linux.txt](https://raw.githubusercontent.com/effekseer/Effekseer/master/readme_tool_linux.txt) **[lida]**. O repositório tem uma pasta Docker/Ubuntu, sem descrição — [GitHub](https://github.com/effekseer/Effekseer) **[lida]**
- **Não achei render por linha de comando/headless** do Effekseer nem na documentação (vista por resumo) nem no repositório.

**Pixel FX Designer (CodeManu)**
- Exporta a animação como folha, quadro a quadro ou GIF — [itch.io: Pixel Fx Designer](https://codemanu.itch.io/particle-fx-designer)
- Preço: base US$ 19,95 no itch.io (preço "ou mais"), US$ 9,99 na Steam; menor preço registrado US$ 4,99 em 05/01/2026 — [itch.io: purchase](https://codemanu.itch.io/particle-fx-designer/purchase); [gg.deals](https://gg.deals/application/pixel-fx-designer/). Faz parte do pacote "FX Tools" (SpriteMancer, Pixel FX Designer, Juice FX, Smear FX, Fluid FX) — [itch.io bundle](https://itch.io/s/185981/pixel-fx-designer). O desenvolvedor aponta o SpriteMancer como ferramenta mais nova — [itch.io: SpriteMancer](https://itch.io/s/181735/spritemancer)
- Termos de licença comercial não apareceram nos resumos.

**Houdini (SideFX)**
- Indie: US$ 299/ano ou US$ 449 por 2 anos; teto de receita bruta anual de US$ 100 mil; até 3 licenças por estúdio. Apprentice: grátis, só não comercial, render limitado a 1920x1080 — [shade.inc: Houdini pricing 2026](https://shade.inc/blog/sidefx-houdini-pricing-2026-fx-core-indie-license-costs); [superrendersfarm: how much does Houdini cost](https://superrendersfarm.com/article/how-much-does-houdini-cost) (agregadores; não vi a página da SideFX)
- **Labs Flipbook Textures ROP**: renderiza, monta e exporta texturas de flipbook para engines em tempo real, com vários passes (reiluminação, interpolação entre quadros, mistura por profundidade) — [SideFX docs: labs flipbook textures 1.0](https://sidefx.com/docs/houdini/nodes/out/labs--flipbook_textures-1.0.html)
- **Labs Texture Sheets 2.0**: a doc do 20.5 diz que está escondido e sem suporte; a doc japonesa do 21.0 ainda o lista (monta a sequência renderizada numa folha) — situação confusa — [SideFX docs 20.5](https://sidefx.com/docs/houdini/nodes/out/labs--texture_sheets-2.0.html); [SideFX docs 21.0 (ja)](https://www.sidefx.com/ja/docs/houdini/nodes/out/labs--texture_sheets-2.0.html)
- O Flipbook ROP do Houdini 21.0 pode ser usado em aplicações sem interface, como hbatch e hython — [SideFX docs: Flipbook render node](https://www.sidefx.com/docs/houdini/nodes/out/flipbook.html)
- Bug antigo de traceback no Flipbook Textures com vários passes, corrigido no Labs 19.0.566 — [SideFX forum](https://www.sidefx.com/forum/topic/82410/)

**Blender**
- Grease Pencil é o caminho comum para trabalho 2D estilo anime; para cor chapada, desligar "Use Lights" no Grease Pencil — [yelzkizi (guia)](https://yelzkizi.org/?p=38207) (fonte fraca). O Blender Studio testou o Grease Pencil 3 num fluxo NPR para parecer anime 2D — [Blender Studio: cowboi](https://studio.blender.org/blog/cowboi/). Traço desenhado à mão preso em animação 3D com Grease Pencil + Geometry Nodes (out/2026) — [BlenderNation](https://www.blendernation.com/2026/10/19/sticking-hand-drawn-ink-lines-to-3d-animation-with-blender-grease-pencil-and-geometry-nodes-2/)
- Headless: commit de agosto/2022 trouxe OpenGL realmente headless no Linux, caindo para um display EGL "surfaceless" quando não dá para abrir janela (só Linux) — [bf-blender-cvs, ago/2022](https://lists.blender.org/pipermail/bf-blender-cvs/2022-August/176810.html). Antes disso, EEVEE exigia display mesmo em background e a saída era Xvfb — [devtalk: Unable to open a display (Eevee)](https://devtalk.blender.org/t/blender-2-8-unable-to-open-a-display-by-the-rendering-on-the-background-eevee/1436). Cycles renderiza sem GPU e sem X — [devtalk: Unable to open a display (Cycles)](https://devtalk.blender.org/t/blender-2-8-unable-to-open-a-display-by-the-rendering-on-the-background-cycles/8607)
- Bugs relatados: Blender trava ao renderizar EEVEE pela linha de comando com DISPLAY vazio — [issue #85336](https://projects.blender.org/blender/blender/issues/85336); objeto de traço Grease Pencil aborta render headless mesmo em Cycles (2021, sem confirmação de correção) — [T85546](https://developer.blender.org/T85546); quadro de Grease Pencil pulado em modo background, com contorno de pôr o quadro atual no primeiro antes do render — [T85035](https://developer.blender.org/T85035). Uma página da iRender (out/2026, tom de marketing) diz que EEVEE-Next ainda precisa de display e sai preto sem ele — [iRender](https://irendering.net/eevee-next-headless-dilemma-cloud-render-farm-guide/) (não confiável sozinha)
- Add-on pago "Vfx Export Studio": assa um intervalo de quadros da cena do Blender numa folha NxM para Unity VFX Graph e Unreal Niagara — [Superhive Market](https://superhivemarket.com/products/vfx-export-studio-realtime-vfx-textures) (marketing)

**Godot**
- Movie Maker (fonte da documentação oficial, lida no GitHub): "a transparência é suportada, mas o viewport raiz **precisa** ter `transparent_bg` = true", ligado em Rendering > Transparent Background; PNG é sem perda, com arquivos grandes e codificação lenta, e a configuração de qualidade é ignorada em PNG; `godot --path /projeto --write-movie saida.avi --fixed-fps 30`; caminho relativo é relativo à pasta do projeto; `--write-movie` e `--fixed-fps` funcionam também em projeto exportado. **A página não fala de `--headless` nem de rodar sem tela, nem de alfa premultiplicado** — [godot-docs: creating_movies.rst](https://raw.githubusercontent.com/godotengine/godot-docs/master/tutorials/animation/creating_movies.rst) **[lida]**. Também por resumo: [Godot docs: MovieWriter](https://docs.godotengine.org/en/latest/classes/class_moviewriter.html); [Godot docs: creating movies](https://godot-fr.readthedocs.io/fr/4.x/tutorials/animation/creating_movies.html); [Godot blog: Movie Maker mode](https://godotengine.org/article/movie-maker-mode-arrives-in-godot-4)
- Nenhuma fonte confirmou rodar o Movie Maker com `--headless` (o modo headless usa driver de vídeo "dummy", que não renderiza — isso é conhecimento prévio, não verificado). Há relato de gravação mais escura por problema de alfa — [Godot Forum](https://forum.godotengine.org/t/partial-transparency-blended-with-black-in-screenshot-images/128560)

**Unity / Unreal**
- Unreal: Niagara Baker assa uma simulação Niagara num flipbook (imagem em grade) para usar em material — [Epic docs: Niagara Flipbook Baker](https://dev.epicgames.com/documentation/unreal-engine/niagara-flipbook-baker-quick-start-guide-in-unreal-engine)
- Unity: o VFX Toolbox tem o Image Sequencer, que monta flipbooks a partir de quadros já exportados de outro programa (não é um baker do VFX Graph) — [VFXToolbox (espelho gitee)](https://gitee.com/mirrors_Unity-Technologies/VFXToolbox). Não achei baker nativo do VFX Graph.

**Animação 2D desenhada (Toon Boom, CSP, TVPaint, OpenToonz, Krita)**
- OpenToonz: grátis, cobre animação e efeitos de câmera, com plugins (lens flare, distorção); usado em projetos do Studio Ghibli — [Clip Studio: guia de software 2D](https://clipstudio.net/en/animation/apps-software); [U. Michigan: OpenToonz](https://sites.google.com/umich.edu/groundconnections/groundworks/opentoonz) (fontes secundárias; o guia é de um concorrente)
- Toon Boom Harmony: usado em muitas produções de TV e cinema; plano Premium citado a US$ 129,50/mês — [Prolific Studio](https://prolificstudio.co/blog/software-for-2d-animation/) (agregador; valor não confirmado no site da Toon Boom)
- TVPaint: animação bitmap sem conversão vetorial, usado em TV e cinema; "muito usado em anime" segundo um guia — [linearity.io](https://www.linearity.io/blog/2d-animation-software/amp/); [Clip Studio: guia](https://clipstudio.net/en/animation/apps-software) (secundárias)
- CLIP STUDIO PAINT: usado por estúdios japoneses como Toei Animation, OLM Asia (Pokémon Sun & Moon) e Yostar Pictures (Arknights) — [Clip Studio: guia de software 2D](https://clipstudio.net/en/animation/apps-software) (página do próprio fabricante)
- Krita, Tahoma2D, Spine e Live2D: não pesquisados nesta sessão (ver Lacunas).

### Inferências
- **EmberGen** é ótimo para fumaça/fogo volumétrico realista; para anime ia precisar de posterização/limiar e contorno depois (no EffectCraft, por exemplo). Como não tem CLI confirmada e o Linux é instável, não serve para rodar sozinho no container. Pago, mas barato para indie.
- **Effekseer** (MIT, confirmado no repositório; tem editor Linux, mas precisa de OpenGL 3.2, ou seja, de tela/GPU) tem estética de "efeito de jogo japonês" (anéis, faixas, billboards com textura) e exporta folha; mas é GUI, e os efeitos são montados com texturas e curvas — perto do "desenhado por código" que o dono recusou, a menos que as texturas sejam desenhadas/geradas.
- **Pixel FX Designer** é feito para pixel art com partículas: não combina com o visual anime em alta resolução e também é "efeito por código".
- **Houdini Indie** tem o melhor caminho headless documentado (hython + Labs Flipbook Textures), mas o teto de US$ 100 mil de receita, o custo anual e a curva de aprendizado pesam; a estética padrão é de simulação realista.
- **Blender** é a opção grátis mais forte para cel/anime de verdade (Grease Pencil desenhado à mão, Line Art, shader toon, Geometry Nodes) e renderiza com `blender -b`; no container sem tela, testar EEVEE com EGL surfaceless primeiro, cair para Xvfb, e usar Workbench/Cycles se nada der. Grease Pencil headless tem bugs relatados — precisa de teste.
- As ferramentas de desenho 2D são as "técnicas profissionais reconhecidas" (sakuga de efeito), mas dependem de alguém desenhando; servem como origem dos quadros, não como gerador automático.

### Lacunas
- Se o Effekseer tem algum modo de render sem interface (nem docs nem repositório mostram; a pasta Docker/Ubuntu não foi aberta).
- Termos comerciais do Pixel FX Designer.
- Se o EmberGen tem CLI ou modo batch (nenhuma fonte disse que tem).
- Preço oficial do Houdini Indie na sidefx.com (só agregadores).
- Krita (GPL, animação quadro a quadro, render por script Python), Tahoma2D (fork do OpenToonz), Spine e Live2D: não pesquisados por falta de chamadas.
- Se o Movie Maker do Godot funciona em container sem GPU (acho que não com `--headless`; precisaria de Xvfb + renderizador de compatibilidade/software).
- Preços atuais (nov/2026) de Toon Boom, TVPaint e CSP não confirmados nas páginas oficiais.

## (C) O que estúdios japoneses de anime e de jogo usam para efeitos 2D, e palestras CEDEC/GDC

### Takeaway
Não achei palestra CEDEC/GDC específica sobre pipeline de efeitos 2D para jogo de luta ou gacha. Achei palestras próximas da CEDEC (efeito de anime x efeito de jogo em 2011; efeitos cartoon desenhados à mão levados para malha 3D em 2019; SpriteStudio com função de efeito em 2016; render anime de Monster Hunter Stories 3 em 2026). O uso de CSP por estúdios como Toei e Yostar tem fonte (do fabricante); o de Toon Boom/TVPaint em anime só tem fontes secundárias.

### Cited Findings
- CEDEC 2011, "アニメのエフェクト、ゲームのエフェクト" (efeitos de anime, efeitos de jogo), parte 1: técnicas de desenho de efeito de animação aplicadas à produção de efeitos de jogo; a parte 2 trata de casos que juntam efeito desenhado e 3DCG — [CEDEC 2011: Visual Arts](https://cedec.cesa.or.jp/2011/program/VA/C11_I0006.html) (**antigo**)
- CEDEC 2019, "Mesh Effect Shape": como levar efeitos cartoon desenhados à mão (água, fogo, fumaça) para malhas 3D leves que animam mantendo o charme 2D, com exemplo num curta feito em Unity — [CEDEC 2019 session](https://cedec.cesa.or.jp/2019/session/detail/s5c9d9674182f0.html)
- CEDEC 2016: OPTPiX SpriteStudio (ferramenta japonesa de animação 2D muito usada em jogos de celular) ganhando função de "efeito" — [CEDEC 2016 session](https://cedec.cesa.or.jp/2016/session/VA/13108.html) (**antigo**)
- CEDEC 2026: "Monster Hunter Stories 3" — dissecando o render estilo anime — citado em [Silicon Studio: destaques CEDEC 2026](https://ss-agent.jp/column/special/sp44-cedec2026/) (é sobre render de personagem/cena, não especificamente efeito)
- CEDEC 2024 (Silicon Studio): StrokeGen, geração de contorno vetorial "desenhado à mão" em tempo real — [GameMakers](https://gamemakers.jp/article/2024_08_30_77510/)
- CEDEC 2023: P&D de anime, de apoio ao storyboard a uso de engine de jogo — [CEDEC 2023](https://cedec.cesa.or.jp/2023/session/detail/s6440f0443160b.html); CEDiL: anime original "Mecha-Ude" com 3DCG para animação 2D — [CEDiL](https://cedil.cesa.or.jp/cedil_sessions/view/2174)
- Anime "Fire Force" (炎炎ノ消防隊) temporada 2: encerramento feito no UE4, pensando em reaproveitar no jogo — [CGWORLD](https://cgworld.jp/feature/202009-cgw265HSfiref-3.html)
- Técnicas de After Effects para ilustração (CGWORLD, 2020) — [CGWORLD](https://cgworld.jp/feature/202004-cgw261t2ae-3.html)
- Existem pacotes comerciais de efeitos anime desenhados à mão em sequência PNG e MOV para AE/Premiere, vendidos no BOOTH — [booth.pm](https://booth.pm/ko/items/7592121) (é material de terceiros; pela regra do projeto, não entra no repo sem licença clara)
- CSP usado por Toei Animation, OLM Asia, Yostar Pictures — [Clip Studio](https://clipstudio.net/en/animation/apps-software); OpenToonz em projetos do Ghibli — mesma fonte e [U. Michigan](https://sites.google.com/umich.edu/groundconnections/groundworks/opentoonz)

### Inferências
- O padrão que aparece: o **desenho** do efeito (sakuga) é feito à mão em ferramenta 2D (CSP/TVPaint/OpenToonz), o **acabamento** (glow, blur, composição) é feito no AE, e a **montagem no jogo** é feita no sistema de partículas da engine (Unity Shuriken/VFX, Effekseer, SpriteStudio). Isso bate com o caso descrito na seção A, mas é inferência a partir de fontes esparsas.
- O nosso fluxo (imagem/vídeo gerado -> EffectCraft fazendo o papel do AE -> PNG -> folha) segue a mesma divisão, com o EffectCraft no lugar do AE e a geração no lugar do desenho à mão.

### Lacunas
- Não achei palestra da GDC sobre efeitos 2D de jogo de luta (por exemplo, Arc System Works / Guilty Gear, que é 3D cel) nem de gacha (Cygames, miHoYo) com fluxo de efeito. Precisaria de buscas específicas por título/ano ou no CEDiL.
- Não há fonte direta sobre qual ferramenta os animadores de efeito (エフェクト作画監督) usam hoje; Toon Boom/TVPaint em anime só têm fontes secundárias.

## (D) O que roda sozinho num container Linux sem tela e gera PNG para o nosso script empacotar

### Takeaway
Com documentação: **Houdini** (hython/hbatch, pago e com teto de receita), **Blender** (`blender -b`; Cycles/Workbench seguros, EEVEE via EGL surfaceless desde 2022 mas com bugs relatados; Grease Pencil headless tem bugs antigos) e **Godot** (`--write-movie` em PNG com transparência, mas sem confirmação de funcionar sem display). Para empacotar sem tela há fastpack, libgdx-texturepacker, free-tex-packer-cli, Aseprite CLI e TexturePacker CLI (pago para comercial). EmberGen, Effekseer, Pixel FX Designer e as ferramentas de desenho 2D dependem de interface gráfica.

### Cited Findings
- Houdini: Flipbook ROP usável em hbatch/hython — [SideFX docs](https://www.sidefx.com/docs/houdini/nodes/out/flipbook.html); Labs Flipbook Textures monta flipbooks para engine — [SideFX docs](https://sidefx.com/docs/houdini/nodes/out/labs--flipbook_textures-1.0.html); em PDG, o batching do ROP Fetch só vale para faixa de quadros; a SideFX sugere "Services" (pool de workers) para muitos itens — [SideFX forum](https://sidefx.com/forum/post/365350)
- Blender: EGL surfaceless headless no Linux desde ago/2022 — [bf-blender-cvs](https://lists.blender.org/pipermail/bf-blender-cvs/2022-August/176810.html); EEVEE sem DISPLAY já travou — [issue #85336](https://projects.blender.org/blender/blender/issues/85336); `blender -b` falhou em Ubuntu sem tela num relato — [issue #77339](https://projects.blender.org/blender/blender/issues/77339); Xvfb como tela virtual dá certo, mas houve textura estranha no EEVEE do 2.82 — [devtalk](https://devtalk.blender.org/t/blender2-82-xvfb-virtual-screen-rendering-with-strange-texture-results-eevee/12750) (**antigo**)
- Godot: `--write-movie` + `--fixed-fps`, PNG com `transparent_bg` — [Godot docs](https://docs.godotengine.org/en/latest/classes/class_moviewriter.html)
- Effekseer no Linux: editor gráfico que exige OpenGL 3.2, sem CLI documentada — [readme_tool_linux.txt](https://raw.githubusercontent.com/effekseer/Effekseer/master/readme_tool_linux.txt) **[lida]** (com Xvfb + Mesa talvez abra, mas a gravação ainda seria por clique)
- Empacotadores CLI: fastpack (CLI headless) — [docs.rs](https://docs.rs/crate/fastpack/0.29.4); libgdx texturepacker — [GitHub](https://github.com/tommyettinger/libgdx-texturepacker/blob/main/README.md); free-tex-packer-cli (MIT, Node, lê projeto .ftpp) — [GitHub](https://github.com/odrick/free-tex-packer-cli) **[lida]**; TexturePacker CLI (licença paga para comercial) — [CodeAndWeb](https://www.codeandweb.com/texturepacker/licenses-comparison)
- EmberGen sem CLI encontrada e com Linux instável — [CG Channel](https://www.cgchannel.com/2023/03/jangafx-releases-embergen-1-0/); [JangaFX forum](https://forums.jangafx.com/t/embergen-crashes-on-launch-on-linux/2293)
- Effekseer: gravação de folha documentada só na interface — [Effekseer: Record](https://effekseer.github.io/Help_Tool/en/ToolReference/record.html)
- Godot: a doc oficial descreve `--write-movie`/`--fixed-fps` e PNG com transparência, mas não fala de rodar sem tela — [godot-docs](https://raw.githubusercontent.com/godotengine/godot-docs/master/tutorials/animation/creating_movies.rst) **[lida]**

### Inferências
- Para o projeto, o candidato realista para somar ao EffectCraft no container é o **Blender em modo background**: grátis, sem limite de licença no que sai dele, e com Grease Pencil/toon para cara de anime. Teste mínimo: `blender -b cena.blend -E BLENDER_EEVEE_NEXT -a` com saída PNG RGBA e "Film > Transparent" ligado; se travar sem tela, tentar `xvfb-run`, depois Workbench/Cycles. (Os nomes de flag e de engine são conhecimento prévio; conferir na versão instalada.)
- Houdini só valeria a pena se o projeto quisesse simulação (fumaça/fluido) estilizada e aceitasse US$ 299/ano e o teto de US$ 100 mil.
- Nosso script Python já empacota; um empacotador de fora só valeria a pena por recorte por quadro com offsets e pivô em JSON (fastpack/free-tex-packer-cli fazem isso, segundo a descrição de "substituto do TexturePacker"; detalhes não verificados).
- Nada aqui contorna a regra do dono: Blender/Houdini/Godot gerando partícula procedural = "desenhado por código". O uso aceitável é renderizar desenho/geração (traços de Grease Pencil desenhados à mão, vídeo gerado como textura), não formas matemáticas.

### Lacunas
- Não testei nada no container nesta sessão (trabalho só de pesquisa).
- Sem confirmação de que o Movie Maker do Godot funciona em `--headless` ou sem GPU.
- Sem confirmação do estado atual (Blender 4.x/5.x, nov/2026) do Grease Pencil em render background.
- Krita (script Python, exportação de animação) não pesquisado como opção headless.
