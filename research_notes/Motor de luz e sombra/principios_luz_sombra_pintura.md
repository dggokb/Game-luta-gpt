# Princípios de luz e sombra para personagem 2D pintado sobre fundo pintado

Legenda de procedência (vale para todo o arquivo):
- **[LIDO]**: página aberta com a ferramenta de leitura. O texto passou por um resumidor automático, e as citações literais vieram desse resumo. Uma frase curta, que leva a regra, deve estar certa. Contexto longo pode faltar.
- **[SNIPPET]**: só resumo de busca, página não aberta. Conferir antes de usar como fonte forte.

Fontes que não consegui ler nesta rodada: Scott Robertson ("How to Render"), Loomis (texto primário), Marco Bucci, Nathan Fowkes, o livro de Steve Wright e o fxguide. Nenhuma afirmação abaixo vem deles.

---

## 1. Key, fill, rim e ambiente: para que serve cada luz e quais proporções usar

### Takeaway
Um esquema de luz bem lido tem uma fonte dominante (key). As outras derivam dela e ficam subordinadas a ela. A proporção key:fill se mede em stops (1 stop = 2×): 2:1 = 1 stop (claro, pouca sombra, "high key"), 4:1 = 2 stops (sombra marcada), 8:1 = 3 stops (noir, horror, drama). As fontes não dão uma proporção fixa para "noite", "nublado" ou "sol". O mais perto disso é Gurney, para quem o sol separa luz e sombra em até ~5 degraus da escala tonal e a luz nublada não tem lado de luz nem terminador.

### Cited Findings
- Proporção em stops: "A difference of two f-stops is 2 squared, or a 4:1 ratio." / "A difference in 3 stops is 2 cubed, or an 8:1 ratio." / "No difference is equal to 2 to the power of 0, for a 1:1 ratio." [LIDO] — [Wikipedia, Lighting ratio](https://en.wikipedia.org/wiki/Lighting_ratio)
- "The higher the lighting ratio, the higher the contrast of the image." A fórmula da ASC é (key+fill):fill. Exemplos: 200 fc de key e 100 de fill dão 3:1; 800 e 200 dão 5:1. [LIDO] — [Wikipedia, Lighting ratio](https://en.wikipedia.org/wiki/Lighting_ratio)
- Pegadinha de medida. No método aditivo, a fill também cai no lado iluminado: um key 1 stop acima da fill dá 2:1 pela fonte, mas "3:1, because the fill also falls on the key-lit side." [LIDO] — [Wikipedia, Lighting ratio](https://en.wikipedia.org/wiki/Lighting_ratio)
- Clima de cada proporção: 2:1 dá contraste moderado e sombras definidas mas não duras; 4:1 dá sombras mais fortes; 8:1 dá contraste ousado, típico de noir e horror. [SNIPPET] — [StudioBinder, Lighting ratios](https://www.studiobinder.com/blog/lighting-ratios/)
- High key fica em 1:1 a 2:1; low key em 4:1 a 8:1 ou mais (noir, thriller, horror). [SNIPPET] — [Fiveable, high key/low key](https://frontend.prod.fiveable.me/cinematography/unit-8/high-key-low-key-lighting/study-guide/4zndNAKpMy7ZcwYc). O wiki da UT chama 2:1 de "high key" e 4:1 de "low key". [SNIPPET] — [UT RTF318 wiki](https://cloud.wikis.utexas.edu/wiki/spaces/rtf318/pages/80022816/Lighting+ratios). As fontes divergem no limite exato.
- Gurney (Color and Light) sobre as fontes secundárias, a luz do céu e a refletida: "The second two sources of light derive entirely from the first, and should be subordinated to it." [LIDO, OCR parcial do livro] — [archive.org, Color and Light](https://archive.org/stream/Color_and_Light_James_Gurney_English/Color_and_Light_James_Gurney_English_djvu.txt)
- Gurney sobre a amplitude no sol: "In sunlight the light side and the dark side of the form can be separated by as many as five steps of the tonal scale." [LIDO, OCR] — [archive.org, Color and Light](https://archive.org/stream/Color_and_Light_James_Gurney_English/Color_and_Light_James_Gurney_English_djvu.txt)
- Luz difusa: "In soft or diffuse light, such as overcast light, there is no distinct light side, shadow side, terminator, or core." [SNIPPET, post de 2010 da série Light and Form] — [Gurney Journey](http://gurneyjourney.blogspot.com/2010/02/light-and-form-part-3.html)
- Light wrap e luz de recorte (rim) vindas do fundo: ver a seção 5.

### Inferences
- Se 1 degrau da escala de 10 valores ≈ 1 stop (o Zone System trata cada zona como 1 stop, ver a seção 4), os "5 degraus" do sol de Gurney equivalem a ~32:1. Isso é bem mais que o 8:1 do cinema, porque Gurney inclui a cor local e uma sombra com pouca fill. Num sprite que já tem a sombra pintada, o jogo não deve recriar essa amplitude: ela já está na arte.
- Para shader: uma única key direcional dominante, ambiente/fill como multiplicador de cor (não como soma de luz) e no máximo uma rim do lado da key ou do contra-luz. "Noite" pede mais contraste ou menos fill (4:1 a 8:1) e luz geral mais baixa. "Nublado" pede ~1:1 a 2:1, sem terminador.

### Gaps
- Nenhuma fonte primária dá uma proporção key:fill padrão para "noite", "nublado" ou "sol". Só achei os rótulos high key e low key.
- Não li nada da ASC (American Cinematographer Manual) diretamente.

---

## 2. Sombra própria, sombra projetada, oclusão; terminador, meio-tom, luz refletida, brilho; famílias de luz e sombra

### Takeaway
A regra central, achada em várias fontes: **o valor mais escuro da luz fica mais claro que o valor mais claro da sombra.** Luz e sombra são duas famílias que não se misturam. A luz refletida fica dentro da família da sombra. Uma sugestão concreta numa escala de 10: sombras em 2–4 e luzes em 6–9, deixando 5 como separação. O brilho é um reflexo pequeno da fonte e só aparece em superfície brilhante. A oclusão (contato) é o acento mais escuro, onde as formas se tocam.

### Cited Findings
- Regra das famílias. Pergunta de leitor no blog: "the darkest light in the light has to be lighter than the lightest dark in the shadow." A versão de Gurney: uma forma fosca ao sol direto, com pouca luz refletida, fica "no darker on the light side than the lightest value of the shadow". A regra não vale para superfícies muito refletoras. [LIDO] — [Gurney Journey, Going Beyond the Facts (2014)](https://gurneyjourney.blogspot.com/2014/11/going-beyond-facts.html?m=1)
- Segunda regra, no mesmo post: o lado em sombra de um objeto branco deve ser mais escuro que o lado iluminado de um objeto preto, em condições ideais. Sorolla quebra essa regra. Gurney diz que essas regras corrigem o "overmodeling" e a sombra dividida em luzes demais. [LIDO] — [Going Beyond the Facts](https://gurneyjourney.blogspot.com/2014/11/going-beyond-facts.html?m=1)
- Escala de 10: sombras em 2–4, luzes em ~6–9, para uma leitura gráfica forte. Isso vem de um comentarista anônimo, que atribui a ideia de forma vaga a Loomis: "It may have been a Loomis book". Não é Gurney e a origem não está confirmada. [LIDO] — [Going Beyond the Facts](https://gurneyjourney.blogspot.com/2014/11/going-beyond-facts.html?m=1)
- "To achieve a feeling of light, focus on grouping the shadows, simplifying them, and separating them from the lights." Elogio a Cotman: "The tones in the shadow are kept together, even though they're not too black." [LIDO] — [Gurney Journey, How should I paint the light in shadows? (2018)](http://gurneyjourney.blogspot.com/2018/05/how-should-i-paint-light-in-shadows.html)
- T. J. Kitts, nos comentários do mesmo post: "The lightest value within a shadow (mass) will not be as light as the darkest value in the light (mass)." [LIDO] — [mesmo post](http://gurneyjourney.blogspot.com/2018/05/how-should-i-paint-light-in-shadows.html)
- Bongart e Pyle, citados por Gurney: "Nothing in the light is as dark as the shadow. Nothing in the shadow is as light as the light." [SNIPPET] — [Gurney Journey, Pyle on Light and Shadow (2009)](http://gurneyjourney.blogspot.com/2009/05/pyle-on-light-and-shadow.html)
- Definições. "The light side includes the light and dark halftones, the center light, and the highlight." / "The center light is the point at which the light rays strike the form most vertically." / "The highlight is the point where, in a shiny surface, we see a reflection of the light source." / O terminador "occurs where the light rays from the source are tangent to the edge of the form." / "The form shadow begins just beyond the terminator." / O core (núcleo): "The core, or hump, or accent, as it's variously called, is usually in my understanding a little past the terminator." Num comentário, Gurney diz que a luz refletida costuma ser exagerada. [LIDO] — [Gurney Journey, Light and Form Part 1 (2010)](http://gurneyjourney.blogspot.com/2010/02/light-and-form-part-1_15.html)
- Oclusão e contato: "Dark accents occur at places where forms come close enough to each other to crowd out the light, leaving a small, dense area of shadow." / "They're commonly seen where materials push together in folds or at points of contact with the ground." [LIDO, OCR] — [archive.org, Color and Light](https://archive.org/stream/Color_and_Light_James_Gurney_English/Color_and_Light_James_Gurney_English_djvu.txt)

### Inferences
- Para o sprite: a arte pintada já tem as duas famílias. Uma luz de shader que clareia por igual a família da sombra pode fazê-la cruzar o limite (sombra mais clara que a luz mais escura). Aí a leitura de volume some e vira "mancha", que é exatamente a queixa de luz chapada em dois tons. A luz adicionada deve agir sobretudo na família da luz e manter a sombra no teto dela.
- Um "duas cores" do shader que não segue o terminador da arte pintada cria um segundo terminador, que contradiz o primeiro.
- A oclusão e o contato com o chão são o lugar mais escuro e menor. Uma sombra de contato sob os pés deve ser pequena, densa e mais escura que a sombra projetada.

### Gaps
- Não li Scott Robertson, Loomis no original, Marco Bucci ou Nathan Fowkes. A regra 2–4 e 6–9 só tem a atribuição vaga a Loomis.

---

## 3. Cor da luz e cor da sombra

### Takeaway
A sombra recebe a luz do ambiente, e não "luz menos alguma coisa". Ao sol, a sombra voltada para cima pega o azul do céu e fica fria. A sombra voltada para baixo pega o rebatimento quente do chão. O contraste quente e frio cresce no fim da tarde. Se o céu está tapado ou o rebatimento é quente, a sombra pode ser quente. O efeito existe tanto pela física (luz do céu) quanto pela percepção (contraste simultâneo). O excesso de saturação é um erro mais comum que o excesso de cinza.

### Cited Findings
- As sombras são "lit by the relatively blue skylight". Contraste simultâneo: "exposure to any color causes adjacent colors to appear complementary." À pergunta sobre a causa, ele responde "it's a little of both". O efeito fica "heightened late in the day as the sun is lower in the sky". Ressalva: "if those sources of reflected light are very warm," a sombra também pode ser quente. [LIDO] — [Gurney Journey, Complementary Shadows (2014)](https://gurneyjourney.blogspot.com/2014/09/complementary-shadows.html?m=1)
- "In shadows, upfacing planes are cool, and downfacing planes are warm." O plano voltado para baixo pega calor do chão iluminado e fica laranja em vez de azul. [LIDO] — [Gurney Journey, Downfacing Planes (2008)](https://gurneyjourney.blogspot.com/2008/02/downfacing-planes.html?m=1)
- Kitts: com uma única fonte quente, como o sol direto, as sombras parecem mais frias que as luzes. A luz indireta através de nuvens muda isso. [LIDO, comentário] — [Gurney Journey 2018](http://gurneyjourney.blogspot.com/2018/05/how-should-i-paint-light-in-shadows.html)
- No fim da tarde, a luz do sol perde comprimentos de onda curtos e sobra mais laranja e vermelho, "making the color contrast between light and shadow more obvious". [SNIPPET] — [Gurney Journey, Golden Hour (2008)](https://gurneyjourney.blogspot.com/2008/01/golden-hour.html)
- "More paintings fail because of too much intense color rather than too much gray." [LIDO, OCR] — [archive.org, Color and Light](https://archive.org/stream/Color_and_Light_James_Gurney_English/Color_and_Light_James_Gurney_English_djvu.txt)
- Guia de terceiros: quente na luz e frio na sombra é uma generalização. "the warm light, cool shadow guideline does not mean the shadow must be a cool blue color". Ao sol, a sombra costuma ser um cinza frio sutil, não um azul vivo. [SNIPPET, aggregator] — [Will Kemp Art School](https://willkempartschool.com/five-simple-steps-painting-realistic-shadows-in-sunlight/)
- Prática de ilustração anime: no entardecer, sombra num layer em multiply cor de marrom, com roxo ou violeta perto do crepúsculo. [SNIPPET, tutorial de usuário] — [Clip Studio Tips, sombras em ambientes diferentes](https://tips.clip-studio.com/ja-jp/articles/7957?org=1)

### Inferences
- Para shader: a cor da sombra deve ser um multiplicador colorido tirado do ambiente da fase (o céu e o chão do cenário), e não "albedo × escuro neutro". Escurecer com preto (multiplicar por cinza) só tira saturação e valor, sem dar a cor do ambiente.
- Cenário de dia ao ar livre: lado de cima da sombra mais frio, lado de baixo (perto do chão) mais quente pelo rebatimento. É um gradiente vertical dentro da família da sombra.
- Segurar a saturação: o tom de ambiente é uma inclinação sutil, não um azul forte.

### Gaps
- Não achei uma fonte primária lida sobre a saturação no terminador (a faixa mais saturada logo antes da sombra), sobre o "color corona" ou sobre "não fazer sombra com preto". Eram conhecidos de Gurney e Bucci, mas sem página lida aqui.

---

## 4. Amplitude de valores e por que a imagem "estoura"

### Takeaway
Todo valor somado acima de 1.0 é cortado em 1.0, e as áreas cortadas viram um branco igual e sem detalhe nem cor. No Zone System, a textura acaba na zona VIII. A zona IX é "tom sem textura" e a X é "branco puro: fontes de luz e reflexos especulares". Ou seja, o branco puro fica reservado para fontes e reflexos. Arte que já tem albedo perto do branco (roupa branca, brilho de pele) não tem margem: qualquer luz somada estoura.

### Cited Findings
- "all fragments that have a brightness or color sum over `1.0` get clamped to `1.0`", com perda de "a significant amount of detail". [LIDO] — [LearnOpenGL, HDR](https://learnopengl.com/Advanced-Lighting/HDR)
- Zone System (Ansel Adams). VIII: "Lightest tone with texture; textured snow". IX: "Slight tone without texture; glaring snow". X: "Pure white: light sources and specular reflections; paper white; no detail". V: "Middle gray". VI: "Average light skin". VII: "Very light skin". "A one-zone change is equal to one stop." Para digital e cromo: "expose for the highlights". [LIDO] — [Wikipedia, Zone System](https://en.wikipedia.org/wiki/Zone_System)
- Ao somar o especular branco no espaço não linear, "The white specular light, when added to the diffuse lighting, becomes yellow". O tom de pele também muda quando a luz é feita em espaço não linear. [LIDO] — [GPU Gems 3, cap. 24](https://developer.nvidia.com/gpugems/gpugems3/part-iv-image-effects/chapter-24-importance-being-linear)
- Num composite, o Match Color deve ser contido com Levels para as sombras e as luzes não serem "crushed or blown out". [SNIPPET] — [Frame and Focal](https://frameandfocal.com/post-processing/how-create-better-composites-photoshop-matching-light-and-color-451252)

### Inferences
- Pele clara está nas zonas VI–VII e precisa de 1 a 2 stops de margem até a zona IX. Somar luz sobre pele que já está em VII leva a pele a IX ou X, sem textura: é o "estourado".
- Regra de shader: o teto da família da luz depois da luz é o equivalente à zona VIII (textura). Só pontos de brilho pequenos chegam a IX ou X.
- A perda de cor no realce vem tanto do corte por canal (o canal mais alto bate em 1 primeiro e o matiz desvia ou desbota) quanto da soma de branco.

### Gaps
- **Não achei fonte para a porcentagem da área da imagem que pode ficar no valor máximo.** Nenhuma página lida dá esse número. O máximo que se sustenta é "reservado a fontes e especulares" (Zone System). Não inventar um número.
- Os 18% de cinza não aparecem na página do Zone System lida.

---

## 5. Reiluminar arte que já tem luz pintada: o que dá errado e o que os compositores fazem

### Takeaway
No compositing, a placa de fundo é a verdade. Primeiro se casam o ponto de preto, o ponto de branco e a temperatura de cor do elemento com a placa. Depois vem a integração de borda: o light wrap, que é o fundo desfocado vazando sobre a borda do primeiro plano. Por fim, a sombra de contato. O light wrap clareia só a borda, com a cor do fundo. Não é uma luz geral sobre o personagem.

### Cited Findings
- O LightWrap ajuda a "composite objects into a very bright background" ao "'wrapping' or spilling light from the background onto the foreground object". Ele "creates a reflection of light around the edges of your foreground object by blending in whatever is in the background." Parâmetros: Diffuse (espalhamento, padrão 15), Intensity, a opção de desligar o wrap guiado pela luminância (efeito uniforme na borda) e Highlight Merge (padrão plus, "adds the elements together, producing a glow effect"). Aplica-se ao primeiro plano antes do merge sobre o fundo. [LIDO] — [Foundry, Nuke LightWrap](https://learn.foundry.com/NUKE/content/reference_guide/draw_nodes/lightwrap.html)
- O light wrap leva a luz e a cor do fundo para a borda e tira o ar de "colado". Quanto mais blur no matte, mais fundo entra. O "luminance bias" favorece as áreas claras do fundo. [SNIPPET] — [Natron LightWrap](https://natron.readthedocs.io/en/v2.4.2/plugins/fr.inria.LightWrap.html); [ProVideo Coalition](https://www.provideocoalition.com/how-to-light-wrap/)
- Casar pretos e brancos. Amostrar a sombra mais escura da placa e usá-la como ponto de preto do elemento. Amostrar o ponto mais claro de luz direta nos dois e casar, ajustando o gamma depois. Casar só os pretos pode tingir um elemento neutro. [SNIPPET, tutoriais de prática] — [busca: compositing black levels](https://pixel-monkey.com/posts/color-matching-different-photos/); [CG Lounge, Nuke compositor](https://cglounge.studio/journal/compositing-in-vfx)
- Uma diferença de 5500K (luz do dia) contra 3200K (tungstênio) faz os elementos parecerem de cenas diferentes. [SNIPPET] — [Frame and Focal](https://frameandfocal.com/post-processing/how-create-better-composites-photoshop-matching-light-and-color-451252)
- Uma patente descreve escalar o RGB do primeiro plano para casar com a luz do fundo, usando a cor do chão como tinta ambiente. [SNIPPET] — [US6538396B1](https://patents.google.com/patent/US6538396)
- Modos de mesclagem. Multiply: f(a,b)=ab, "The composite is darker, unless the pixel is white in either layer or black in both layers." Screen: f(a,b)=1−(1−a)(1−b), "wherever either layer was darker than white, the composite is brighter." Overlay: 2ab se a<0,5, senão 1−2(1−a)(1−b). Soft light Pegtop: (1−2b)a²+2ba. Na soma (Linear Dodge), "In case of values above 1 (in the case of RGB), white is displayed." [LIDO] — [Wikipedia, Blend modes](https://en.wikipedia.org/wiki/Blend_modes)

### Inferences
- Luz dupla: a arte já tem a sombra pintada num lado. Se o shader põe a key do lado oposto, surgem dois terminadores que se contradizem. A luz do shader deve vir do mesmo lado que a arte pintou, ou ser neutra em direção (tinta ambiente mais borda).
- Ordem de shader tirada da prática de compositing: (1) grade do sprite para casar o preto, o branco e a tinta ambiente do cenário, com multiply ou lift/gain, sem soma; (2) borda ou light wrap com a cor do fundo, só na borda e na largura de poucos pixels; (3) sombra de contato no chão.
- Screen é uma forma de clarear que nunca passa de 1 e comprime perto do branco: um "soft clip" natural. A soma (add) corta duro.

### Gaps
- Não li Steve Wright nem o fxguide. As regras de casar pretos e brancos vêm de tutoriais de prática, não de uma referência formal.

---

## 6. Animação: cor por cena (色指定) e a etapa de 撮影 (para e gradientes)

### Takeaway
No anime, o personagem não é "iluminado". Ele é **repintado com outra paleta por situação**. O 色彩設計 define primeiro as ノーマル色 (cores-base, o personagem à luz do dia). Depois cria as variações por situação (夕景色, 夜色, luz forte), olhando o 美術ボード (arte de fundo). O 色指定 aplica isso corte a corte. Os números ficam em RGB numa tabela que inclui as cores de linha, W e BL. Na etapa de 撮影, a パラ é um gradiente sobre a tela ou o personagem, aplicado em multiply (sombra) ou add (brilho), com valores ajustados por corte.

### Cited Findings
- 「ノーマル色（基本色）」/「基本の色が決まれば、夕景や夜景などさまざまなシチュエーションで変化する色を決め、色指定表を作っていきます。」 Há seções 「夕景色」 e 「夜色」. 「数字はR.G.Bの数値です。」 A tabela tem as cores de linha e 「「W（白）」「BL（黒）」も用意しておきます。」 Há também a cor de 「BL影」. [LIDO] — [Genkosha, アニメーションの色彩設計【基礎編】](https://genkosha.pictures/movie/19100328493)
- 色彩設計 (Tsujita Kunio): 「キャラクターの基本の色を作る・決める」「その作品の色味の方向性を作る・決める」. 色指定: 「そのお話のすべてのカット（原画・動画）について内容をチェックして、指定を入れていきます。」 Ele decide olhando o 美術ボード: 「夜のシーンはどれくらい暗くするのか？」「夕方は西日が強いのか弱いのか？」. Depois de inspecionada, a cel vai para o 撮影: 「そのＯＫなセルを［撮影］に渡し、撮ってもらうワケです。」 [LIDO] — [WEBアニメスタイル, 色彩設計おぼえがき 第1回](http://www.style.fm/as/05_column/tsujita/tsujita01.shtml)
- Variação por situação: no fim da tarde o matiz vai para o quente, à noite vai para o azul e escurece. A cor da situação depende do ambiente, da circunstância e do estado emocional (色替え). Em 『夜のクラゲは泳げない』 havia "120くらい" cores só de ノーマル. [SNIPPET] — [代々木アニメーション学院 coluna](https://www.amg.ac.jp/animation-column/job/anime-colour/); [Real Sound, 石黒けい / 動画工房](https://realsound.jp/movie/2024/05/post-1670331.html); [note, ルル](https://note.com/luluanime/n/n720aec6b2b93)
- パラ: 「パラ表現とは、画面に対しグラデーションをかけるシンプルな表現手法です。」 É um gradiente linear (pela UV x) ou circular (`clamp(length(uv-0.5)*2,0,1)`, cor × (1−distância)), desenhado num plano e sobreposto em multiply (`Blend Zero SrcColor`) para sombra ou em add (`Blend One One`) para brilho. [LIDO] — [Qiita, パラ表現](https://qiita.com/haruyan/items/60b05832aab4cd0e4491)
- 撮影 no estúdio Asahi Production (Prisma Illya): o script de セル処理 monta a gradação e o highlight automaticamente. 「グラデーションやハイライトなどの数値はキャラのサイズによって値が異なるため、構図に応じて基準となる値を設定し、カットごとに微調整を行なっている。」 [SNIPPET; a página abriu sem o corpo] — [CGWORLD, プリズマ☆イリヤ 撮影ワークフロー](https://cgworld.jp/feature/201512-prisma-illya-cgw208t2-2.html)
- Ilustração: tratar primeiro o fundo e definir a fonte de luz, depois ajustar o personagem. Se o modo de mesclagem ficar forte demais, baixar a opacidade. [SNIPPET] — [CLIP STUDIO お絵かき講座](https://www.clipstudio.net/oekaki/archives/163597)
- Nota pessoal (fonte fraca): reproduzir no 撮影, pelo modo de mesclagem, as cores do 色彩設計, inclusive os tons intermediários que o design não tem: 「撮影時に設計にはない中間色をどのように表現するか」. [SNIPPET] — [Scrapbox aodaruma](https://scrapbox.io/aodaruma/%E8%89%B2%E5%BD%A9%E8%A8%AD%E8%A8%88%E3%81%AB%E5%BE%93%E3%81%84%E3%81%A4%E3%81%A4%E3%80%81%E3%83%AA%E3%83%8B%E3%82%A2%E3%81%AA%E6%92%AE%E5%BD%B1%E3%82%92%E5%8F%96%E3%82%8A%E5%85%A5%E3%82%8C%E3%82%8B%E6%96%B9%E6%B3%95)

### Inferences
- O modelo anime traduzido para o jogo: por fase, um "conjunto de cores" (tinta para a família da luz, tinta para a família da sombra, cor do brilho), em vez de uma luz física. Para sprite pintado, isso vira um multiplicador colorido por família, escolhido a partir do fundo da fase, mais uma パラ: um gradiente em multiply ou add vindo do lado da fonte, suave e de baixa opacidade.
- O dia "normal" é a arte sem alteração (ノーマル色 = a arte como foi pintada). A luz de cena entra como desvio a partir disso, não como soma em cima.

### Gaps
- Não achei, em fonte lida, uma descrição detalhada da difusão e do flare (ディフュージョン, フレア) aplicados ao personagem no 撮影, nem valores típicos de opacidade da パラ.
- Não li a matéria do MdN (o site respondeu 403).

---

## 7. Gamma, linear e tone mapping

### Takeaway
A luz soma em espaço linear. Fazer a soma em sRGB/gamma muda o matiz e deixa os meios-tons errados. O monitor com gamma 2,2 mostra o pixel de 50% com menos de 1/4 da luz. Multiplicar (albedo × luz) nunca passa do branco da própria arte, a não ser com luz acima de 1. Somar passa de 1 e corta. O tone mapping comprime o que fica acima de 1: Reinhard x/(1+x), exponencial 1−exp(−x·exp), curva filmic de Hable com um ponto de branco W.

### Cited Findings
- "The illumination contributions from two light sources in a scene will _sum_." Em espaço não linear o resultado fica escuro demais, "but not uniformly too dark". "The white specular light, when added to the diffuse lighting, becomes yellow." "if color tones change as the same object is brightened or darkened, then most likely nonlinear lighting is taking place". "A typical gamma of 2.2 means that a pixel at 50 percent intensity emits less than a quarter of the light." O fluxo é decodificar a entrada com pow(gamma) e codificar a saída com pow(1/gamma). [LIDO] — [GPU Gems 3, cap. 24, The Importance of Being Linear](https://developer.nvidia.com/gpugems/gpugems3/part-iv-image-effects/chapter-24-importance-being-linear)
- Reinhard: `mapped = hdrColor / (hdrColor + 1.0)`. Exposição: `mapped = 1.0 - exp(-hdrColor * exposure)`. [LIDO] — [LearnOpenGL, HDR](https://learnopengl.com/Advanced-Lighting/HDR)
- Hable: Reinhard simples é "x/(1+x)" e "A common variation is to only do it on luminance". Uncharted 2: `((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F`, com A=0,15, B=0,50, C=0,10, D=0,20, E=0,02, F=0,30, W=11,2, ExposureBias=2,0, o resultado dividido por f(W) e depois pow(1/2,2). O blog tem um post à parte, "Why Reinhard Desaturates Your Blacks" (não lido). [LIDO] — [Filmic Worlds, Filmic Tonemapping Operators](http://filmicworlds.com/blog/filmic-tonemapping-operators/)
- Screen 1−(1−a)(1−b) e a soma com corte em branco: ver a seção 5. [LIDO] — [Wikipedia, Blend modes](https://en.wikipedia.org/wiki/Blend_modes)

### Inferences
- Para sprite pintado: a arte está em sRGB. Somar a luz direto no sRGB gera a "luz calcária" e o desvio de matiz. Multiplicar em sRGB por um fator ≤1 é seguro contra o estouro. Para clarear, vale um fator >1 com tone map, ou screen, ou um soft knee só acima de um limiar (o equivalente à zona VIII).
- Reinhard por canal dessatura os realces. Reinhard só na luminância preserva o matiz, segundo a nota de Hable. Para não perder a cor local no realce, aplicar a compressão na luminância e manter a cromaticidade.

### Gaps
- Não li o post de Hable sobre a dessaturação de Reinhard, nem uma fonte primária sobre o "soft knee".
