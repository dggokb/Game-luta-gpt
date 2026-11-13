# Luz pintada em jogos 2D: como personagens desenhados se encaixam em cenários pintados

Legenda de leitura das fontes:
- **[LIDA]** = página aberta com WebFetch e lida no conteúdo, pelo resumo do fetch, com as citações extraídas da página.
- **[SNIPPET]** = só o trecho da busca. Não abri a página, ou ela bloqueou.
- **[BLOQUEADA]** = tentei abrir e o servidor recusou.

Cobertura: Hollow Knight, Ori (WotW), Streets of Rage 4, Rayman Legends, 13 Sentinels (Vanillaware), Hades e cor de anime (色彩設計). Não pesquisei, por limite de chamadas: Cuphead, GRIS, Spiritfarer, Darkest Dungeon, Don't Starve, Indivisible, TMNT: Shredder's Revenge, River City Girls, Odin Sphere, Dragon's Crown e Unicorn Overlord. Silksong só aparece por meio de mods.

## 1. Hollow Knight: como a luz é falsificada e quais parâmetros a cena declara

### Takeaway
Hollow Knight não ilumina nada por pixel. A luz vem de formas macias semitransparentes, pintadas e colocadas na cena, com shaders simples de sprite. Cada cena tem um componente (SceneManager) que declara a cor e a luz daquele trecho: `saturation`, curvas `redChannel/greenChannel/blueChannel`, `defaultColor`, `defaultIntensity`, `heroLightColor` e `darknessLevel`. É uma gradação de cor por cena, mais uma "luz do herói" separada, sem relighting físico.

### Cited Findings
- A página "Made with Unity" diz, sobre a luz: "Rather than using complex 3D lighting systems, they handled lighting with soft transparent shapes." [LIDA] — [Unity, Made with Unity: Hollow Knight](https://unity.com/made-with-unity/hollow-knight)
- Shaders: "they assigned a handful of simple shader types including sprite_default and sprite_diffuse … with minor modifications to those assets." [LIDA] — [Unity](https://unity.com/made-with-unity/hollow-knight)
- Camadas: "The team built levels with 2D assets layered in a 3D environment using many of Unity's out-of-the-box 2D features", entre eles o Particle System. [LIDA] — [Unity](https://unity.com/made-with-unity/hollow-knight)
- A arte foi pintada à mão em Photoshop e salva como PNG simples. [SNIPPET, resumo da busca sobre a mesma página Unity] — [Unity](https://unity.com/made-with-unity/hollow-knight)
- Na documentação de modding, o editor do `SceneManagerPatcher` (que espelha o SceneManager do jogo) mostra estes campos em "Gameplay Scene Settings": `mapZone`, `isWindy`, `isTremorZone`, `environmentType`, `darknessLevel`, `noLantern`, `saturation`, `ignorePlatformSaturationModifiers`, `redChannel`, `greenChannel`, `blueChannel`, `defaultColor`, `defaultIntensity`, `heroLightColor`, `noParticles` e `overrideParticlesWith`. [LIDA] — [HK Modding Docs, Creating custom Scenes](https://prashantmohta.github.io/ModdingDocs/new-scene.html)
- A mesma página explica só alguns campos: `darknessLevel` "Changes how dark the Scene is", `noLantern` liga ou desliga a lanterna (Lumafly Lantern) e `environmentType` "Changes the particles when walking and landing". Não traz valores padrão nem explicação para saturação, intensidade, luz do herói ou curvas. [LIDA] — [HK Modding Docs](https://prashantmohta.github.io/ModdingDocs/new-scene.html)
- Snippet da busca: o SceneManager de cada cena "controls various aspects of each scene (lightning, color, saturation, music, and more)". [SNIPPET] — [Modding Api docs (radiance.synthagen.net)](https://radiance.synthagen.net/apidocs/_images/NewScene.html)
- "Hero Light" existe no jogo original e o mod HDR RenoDX acrescenta um controle deslizante para ela. [SNIPPET] — [RenoDX Silksong, Nexus](https://www.nexusmods.com/hollowknightsilksong/mods/6); [RenoDX discussão HK](https://github.com/clshortfuse/renodx/discussions/230)
- O mod PaletteSwapper "is a toggleable mod to change the color tint of rooms". A tinta é declarada por zona do mapa ("{MapZone}.{rgba}", cada componente "a float in [0,1]"). O alfa das cores aleatórias fica preso entre 0,5 e 1,0. [LIDA] — [HollowKnight.PaletteSwapper (GitHub)](https://github.com/homothetyhk/HollowKnight.PaletteSwapper/)
- No Silksong, o mod HueShifter troca as cores do mapa por shader. [SNIPPET] — [HueShifter](https://github.com/dpinela/HueShifter)
- Comentário de terceiros, sem fonte do estúdio: fundos macios em tons pastel contra personagens com contorno mais forte. [SNIPPET] — [Medium, "Hollow Knight: A lesson in Game Design"](https://dimasgibi.medium.com/hollow-knight-a-lesson-in-game-design-8cc4ff8aa1cd)

### Inferences
- O conjunto `saturation` + curvas por canal + `defaultColor`/`defaultIntensity` funciona como gradação de cor por cena, igual a um LUT ou curvas no pós. `heroLightColor` é uma cor separada para a luz macia que acompanha o herói, a "bolha" de luz em volta do Knight. O campo `ignorePlatformSaturationModifiers` sugere que a saturação também muda por plataforma. Não confirmei isso.
- Para o nosso jogo, o paralelo direto é: cada estágio declara saturação, curvas RGB, uma cor e intensidade ambiente e uma cor de "luz do lutador", como gradação. Nada de luz física por pixel.

### Gaps
- Não achei os valores numéricos dos campos (por exemplo, a saturação típica de uma área) nem se a gradação vale igual para herói, inimigos e fundo. Isso pediria decompilar o Assembly-CSharp; não fiz.
- Não achei entrevista de Ari Gibson ou William Pellen sobre glow, vinheta ou máscara de escuridão.
- Silksong: sem fonte técnica primária, só mods (vinheta, HDR).

## 2. Ori (Moon Studios): luz pintada à mão em mapas de direção, e personagens 3D "integrados" por truques

### Takeaway
Em Will of the Wisps, os artistas pintaram à mão seis "expressões" de luz por asset (topo, baixo, esquerda, direita, trás, frente) e combinaram tudo num pseudo-normal map que reage às fontes dinâmicas: cerca de 30.000 mapas para uns 7.000 assets. Eles recusaram normais geradas por script porque o resultado fica "cheap, plasticky". Os personagens 3D usam técnicas "incomuns" para se misturar ao cenário, e os valores (claro/escuro) são conferidos em escala de cinza.

### Cited Findings
- Daniel van Leeuwen, artista-chefe: "For each of the assets in the game, we painted a top, down, left, right, back, and front light expression." e "Those maps were combined into a pseudo-normal map that interacts with dynamic sources." [LIDA] — [TheGamer, entrevista de arte](https://www.thegamer.com/ori-and-the-will-of-the-wisps-art-interview/)
- Por que à mão: "you can run scripts on top of it, but it gives a very cheap, plasticky look, so we're forced to hand-paint all these light direction maps." [LIDA] — [TheGamer](https://www.thegamer.com/ori-and-the-will-of-the-wisps-art-interview/)
- Escala: cerca de 7.000 assets e cerca de 30.000 mapas de luz pintados à mão. Van Leeuwen: "expanded the system so the artists could take a whole group of assets and paint light and shadows on top of that." [LIDA] — [TheGamer, 30,000 light maps](https://www.thegamer.com/ori-and-the-will-of-the-wisps-artists-hand-painted-30000-light-maps/)
- Van Leeuwen: "Much of the dynamic light in the game is based on hand-painted maps." / "We painted close to 30,000 of them!" / "We hand shaped a lot of the normal information on meshes". [LIDA] — [Inverse](https://www.inverse.com/gaming/ori-will-of-the-wisps-interview)
- Personagens: "For the 3D characters we used a lot of unusual techniques to make them blend in." e "our lighting system is very different from something you'd typically see." (van Leeuwen) [LIDA] — [Inverse](https://www.inverse.com/gaming/ori-will-of-the-wisps-interview)
- Valor: Jeremy Gritton (diretor de arte) usa um programa que passa o monitor para escala de cinza para "double check the values in our scenes" e alerta: "Color can sometimes fool you into believing there is more value separation than there really is." [LIDA] — [Inverse](https://www.inverse.com/gaming/ori-will-of-the-wisps-interview)
- Thomas Mahler: "Every texture in the game reacts to lighting sources. That means we've gone in and painted light masking on every single surface, on every single asset." [SNIPPET] — [Wccftech](https://wccftech.com/ori-and-the-will-of-the-wisps-dynamic-lighting/)
- Existe a palestra GDC "The Art of 'Ori and the Will of the Wisps'", mas o conteúdo não estava acessível. [SNIPPET] — [GDC Vault](https://www.gdcvault.com/play/1027375/contactUs)

### Inferences
- Mesmo quando a Moon quis luz dinâmica de verdade, a fonte da sombra continuou sendo pintura humana (mapas direcionais pintados), não normais deduzidas da imagem. Isso conversa com o problema de "estourado": normais automáticas sobre arte que já vem sombreada dobram o sombreamento e lavam a imagem.
- A conferência de valor em escala de cinza é uma regra barata que podemos adotar: personagem e fundo precisam se separar em valor, não só em cor.
- Para os nossos lutadores, pintar 6 direções por quadro de animação é inviável. O que se aproveita é o princípio: luz controlada pelo artista, e o motor só mistura.

### Gaps
- Não achei como Ori em si é iluminado (rim, volumes de cor por área). As "unusual techniques" não são detalhadas nas fontes lidas.
- Blind Forest (personagem renderizado de 3D para sprite): não achei fonte técnica.
- Não achei artigo do 80.lv sobre Ori. A busca só trouxe No Rest for the Wicked e Lunarium.

## 3. Streets of Rage 4 (Lizardcube / Guard Crush): luz e rim light por estágio sobre personagens desenhados à mão

### Takeaway
Fontes secundárias confirmam que SoR4 projeta efeitos de luz dinâmica e rim light sobre os personagens desenhados à mão, e até sobre os sprites retrô, para dar o clima de cada estágio. Um personagem perto de um neon recebe a luz dele. Não achei nenhuma explicação técnica primária: nem normal maps, nem zonas de luz, nem parâmetros.

### Cited Findings
- Wiki de fãs: "The programmers developed many lightning effects that could be projected over the character hand-drawn graphics, including the retro character sprites. The technology allowed the team to create the proper atmosphere to each stage, using dynamic lightning and even rim lights over the character silhouettes." [SNIPPET, fonte secundária sem citação primária] — [Streets of Rage Wiki (Fandom)](https://streetsofrage.fandom.com/wiki/Streets_of_Rage_4)
- Resenha (2020): "Different environments will dynamically light these retro sprites … if he's standing by a neon sign, light will be cast upon the sprite." [SNIPPET] — [splitscreen.blog review](https://splitscreendotblog.wordpress.com/2020/05/06/streets-of-rage-4-review/)
- O making-of da Nintendo Life não fala de luz. Fala do motor próprio em C# ("chose C#, because it compiles super fast", Beausoleil Samson-Guillemette, Guard Crush) e de um editor sem modo "play/edit". Ben Fiquet: "Pixel art was never considered". [LIDA] — [Nintendo Life, The Making Of Streets Of Rage 4](https://www.nintendolife.com/news/2020/12/feature_the_making_of_streets_of_rage_4_by_the_people_who_made_it_happen)
- PlayStation Blog sobre os redesenhos: as cores-assinatura dos personagens foram mantidas (o vermelho da Blaze), e os efeitos dos especiais ficaram mais vivos e distintos. [SNIPPET] — [PlayStation Blog](https://blog.playstation.com/2020/04/23/how-lizardcube-redesigned-the-characters-of-streets-of-rage-4-out-april-30/)
- Existe um vídeo "Behind the Art" (fev/2020) com Fiquet e o artista-chefe de cenários, Julian Nguyen You. Não tive acesso à transcrição. [SNIPPET] — [Nintendo Everything](https://nintendoeverything.com/video-streets-of-rage-4-behind-the-art/)
- Matéria da Edge, "The Making of Streets of Rage 4": paywall, só snippet. [SNIPPET] — [Magzter/Edge](https://www.magzter.com/de/stories/Puzzle-Gaming/Edge/THE-MAKING-OF-STREETS-OF-RAGE-4)

### Inferences
- Pelo que se vê nas fontes (sprites retrô de baixa resolução também recebem rim light), o rim do SoR4 provavelmente não depende de normais pintadas por quadro. Algo baseado em silhueta, como um deslocamento do alfa colorido pela luz, funcionaria em qualquer sprite. Isso é dedução minha, não confirmada.
- Um fórum de terceiros (Spine), sem citar o jogo, observa que um rim verde em volta de personagens "probably isn't normal-map-based because there's no visible bump effect". [SNIPPET] — [Esoteric Software forum](https://fr.esotericsoftware.com/forum/d/11836)

### Gaps
- Não achei post de blog da Lizardcube nem palestra de Cyrille Lagarigue ou Ben Fiquet explicando a luz, as sombras no chão ou os reflexos em piso molhado. Essa é a lacuna mais importante para o nosso caso.
- Wonder Boy: The Dragon's Trap: as fontes (GDC 2018, Game Developer) falam de design de personagem e animação, não de shader. [SNIPPET] — [Game Developer](https://gamedeveloper.com/design/faithfully-updating-the-art-of-a-classic-in-i-wonder-boy-the-dragon-s-trap-i-); [Thumbsticks GDC 18](https://www.thumbsticks.com/gdc-18-wonder-boy-the-dragons-trap/)

## 4. Rayman Origins/Legends (UbiArt): luz em tempo real sobre personagens 2D

### Takeaway
O Legends trocou o traço grosso do Origins por um visual mais pintado para mostrar o UbiArt iluminando personagens 2D em tempo real. Os slides de 2014 de ex-membros da equipe falam de luz combinada com máscaras vetoriais e de uma "front light" que simula luz difusa e se mistura a uma cor ambiente. O diretor de arte diz que a luz serve para o personagem não parecer recortado do fundo.

### Cited Findings
- "Legends moves away from the Thick-Line Animation of Origins in favor of a more painterly look, to show off the latest version of the UbiArt engine's ability to light 2D characters in real time with 3D lighting." [SNIPPET, TV Tropes, fonte terciária] — [TV Tropes](https://tvtropes.org/pmwiki/pmwiki.php/VideoGame/RaymanLegends)
- Slides "Modern 2D techniques" (Benoit Fouletier e Guillaume Martin, Unity Day, GCE 2014; os dois trabalharam em Origins/Legends): luz combinada com máscaras vetoriais, e uma front light que simula iluminação difusa e se mistura com uma cor ambiente. [SNIPPET; a página do SlideShare não carregou no fetch] — [SlideShare](https://de.slideshare.net/GameConnection/benoit-fouletier-guillaume-martin-unity-day-modern-2-d-techniquesgce2014)
- Edge, "The Light Fantastic" (fev/2015): o diretor de arte Kelvin Tuite diz que a luz ajuda os personagens a ficarem naturalmente no mundo, sem parecerem recortados do fundo. [SNIPPET; a página do PressReader deu 403, BLOQUEADA] — [PressReader/Edge](https://www.pressreader.com/australia/edge/20150212/283463549725618)
- Cobertura pré-lançamento: "improved lighting, a new rendering system, and more 3D models". [SNIPPET] — [Gaming Trend](https://gamingtrend.com/news/rayman-legends-features-the-same-2d-hand-drawn-visuals-with-new-3d-models-for-spice)
- A palestra GDC 2014 de Chris McEntee é sobre design. É paga no GDC Vault e o resumo não fala de luz. [SNIPPET] — [GDC Vault](https://gdcvault.com/play/1021191/Rayman-Legends-The-Design-Process)

### Inferences
- "Front light + cor ambiente + máscara vetorial" sugere que o UbiArt não gera normais por pixel do desenho. A luz é modulada por máscaras e cores, e o ambiente define a tinta base. É o mesmo princípio de "tinta por zona + luz modulada" que pode servir para os nossos estágios.

### Gaps
- Não consegui ler os slides nem a Edge. Faltam detalhes de back light, rim, gradiente e como a máscara é gerada.

## 5. Vanillaware (13 Sentinels): luz das fontes da cena, contraluz forte e camadas em profundidade

### Takeaway
Em 13 Sentinels, a Vanillaware usou as fontes de luz de cada cena para dar atmosfera e colocou contraluz forte entre camadas num espaço 3D para criar profundidade. Não achei explicação técnica (CEDEC, 4Gamer) de como a luz cai sobre os sprites.

### Cited Findings
- Akiyasu Yamamoto (produtor): "we leveraged the light sources in each scene for atmospheric effect", "placing objects within a layered three-dimensional space" e "incorporating strong backlights to add a sense of depth to the scene". O cenário moderno "at first glance lacked the vibrance and life of past settings". [LIDA] — [PlayStation Blog, How Atlus x Vanillaware bring 2D art to life](https://blog.playstation.com/2020/08/10/how-atlus-x-vanillaware-bring-2d-art-to-life-in-13-sentinels-aegis-rim/)
- Resenhas destacam a luz: "Environments and character models during the 2D sections of the game are absolutely sumptuous, having a painterly look backed up by wonderful lighting." [SNIPPET] — [GameSpew](https://www.gamespew.com/2022/04/13-sentinels-aegis-rim-review/)

### Inferences
- "Contraluz forte" é uma escolha de direção que separa o personagem do fundo pela borda clara, praticamente um rim light de palco. Isso apoia usar rim forte e só ambiente leve na frente, em vez de luz frontal intensa.

### Gaps
- Odin Sphere, Dragon's Crown e Unicorn Overlord: não pesquisei. A busca em japonês (CEDEC, 光, グラデーション) não trouxe nada.

## 6. Hades (Supergiant): personagens 3D com aparência de pintura

### Takeaway
Os personagens de Hades são modelos 3D texturizados para imitar a arte 2D. Na pintura, a artista desliga a luz, aplica cor base chapada, usa mapas de oclusão, curvatura e espessura só como guia, e pinta à mão a linha preta. As fontes lidas não dizem se a luz final fica gravada nem como se adapta a cada sala.

### Cited Findings
- Paige Carter (artista 3D) sobre a Nyx: "I immediately turn off the lighting and stick a base color on the model." / "I'll bake in an ambient inclusion map, a curvature map, and a thickness map" / "hand paint all of the black linework to try and match the 2D look of the character." O modelo é retopologizado para "deform well and render quickly". [LIDA] — [Game Developer, Learn how Supergiant brought Hades' hand-painted characters to life](https://gamedeveloper.com/art/learn-how-supergiant-brought-i-hades-i-hand-painted-characters-to-life)
- Comentário de terceiros: a leitura dos inimigos depende de pose, timing e silhueta, não de detalhe fino. [SNIPPET, não é fonte do estúdio] — [fbpe.org blog](https://old.fbpe.org/breaking-down-the-hades-art-style.html)

### Gaps
- Não achei fonte sobre cor ambiente por sala aplicada aos personagens, rim gravado ou separação de valor em Hades.

## 7. Cor e composição em anime (色彩設計/撮影): o fundo é a referência de cor do personagem

### Takeaway
No anime, o cenário (美術) é a primeira referência de cor, e as cores dos personagens são feitas para casar com ele, com a saturação ajustada por direção. As bordas de luz e sombra são suavizadas com 色トレス, e as cores das sombras são escolhidas com cuidado, nunca preto puro.

### Cited Findings
- Kei Ishiguro (色彩設計, Doga Kobo): 「最初の色の基準になるのは美術だと思っていて、それに合わせてキャラクターの色を作ることが多いです。」(Tradução: "Acho que o cenário é a primeira referência de cor, e as cores dos personagens costumam ser feitas para casar com ele.") [LIDA] — [Real Sound](https://realsound.jp/movie/2024/05/post-1670331_2.html)
- 「特に服の色の彩度をあげています。」(Tradução: "Subo a saturação principalmente das roupas.") [LIDA] — [Real Sound](https://realsound.jp/movie/2024/05/post-1670331_2.html)
- 色トレス: 「ハイライトや影などの境界線を隣接する色に近づけてなじませること」(Tradução: "aproximar a linha de borda de brilhos e sombras das cores vizinhas para fundi-la.") [LIDA] — [Real Sound](https://realsound.jp/movie/2024/05/post-1670331_2.html)
- 「私はキャラクターの色を決める時に影の色を綺麗にしたいと思っています。」(Tradução: "Quando decido as cores de um personagem, quero que a cor da sombra fique bonita.") [LIDA] — [Real Sound](https://realsound.jp/movie/2024/05/post-1670331_2.html)
- A Polygon Pictures (BLAME!) fazia o 色指定 já na composição em NUKE, para reduzir re-renders. [SNIPPET] — [CGWORLD](https://cgworld.jp/interview/201706-blame-lightcomp-2.html)

### Inferences
- A tradução direta para jogo é declarar, por estágio, a cor e a saturação aplicadas ao lutador a partir das cores do fundo, com a cor da sombra puxada para a cor do ambiente. É o que o HK faz com `defaultColor`/`saturation`/curvas e o PaletteSwapper com tinta por zona.

### Gaps
- Não achei fonte sobre "パラ" (gradiente de luz de 撮影 sobre o personagem) nem números típicos de opacidade.

## 8. Por que esses estúdios evitam luz física com normal map nos personagens

### Takeaway
A única justificação explícita que achei é a da Moon Studios: normais ou luz gerada por script sobre arte pintada dão um visual "cheap, plasticky". Mesmo com luz dinâmica, eles pintaram à mão a resposta à luz. Hollow Knight escolheu formas de luz transparentes "rather than using complex 3D lighting systems".

### Cited Findings
- "you can run scripts on top of it, but it gives a very cheap, plasticky look" (van Leeuwen) — [TheGamer](https://www.thegamer.com/ori-and-the-will-of-the-wisps-art-interview/)
- "Rather than using complex 3D lighting systems, they handled lighting with soft transparent shapes." — [Unity](https://unity.com/made-with-unity/hollow-knight)
- Novos artistas da Moon achavam que um botão faria tudo, e isso "not so simple" (Gritton). — [TheGamer](https://www.thegamer.com/ori-and-the-will-of-the-wisps-artists-hand-painted-30000-light-maps/)

### Inferences
- Para o nosso jogo (arte já sombreada, risco de "estourado"), o caminho que as fontes sustentam é: (1) gradação e tinta por estágio (saturação, curvas, cor e intensidade ambiente) aplicada ao lutador; (2) rim pela silhueta, colorido pela luz principal do estágio (SoR4, contraluz Vanillaware); (3) brilhos e sombras como formas pintadas e macias na cena (HK); (4) conferência de valor em escala de cinza (Ori). Isso é síntese minha, não receita de um estúdio.

### Gaps
- Sombra de contato, sombra de silhueta inclinada e reflexo em piso molhado: não achei fontes primárias de nenhum desses jogos nesta rodada.
- "Light wrap", névoa entre planos e números de rim (largura, opacidade): não achei fontes.
- Jogos não pesquisados: Cuphead, GRIS, Spiritfarer, Darkest Dungeon, Don't Starve, Indivisible, TMNT: Shredder's Revenge, River City Girls, Odin Sphere, Dragon's Crown e Unicorn Overlord.
