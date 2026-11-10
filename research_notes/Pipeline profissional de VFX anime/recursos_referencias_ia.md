# Recursos, referências, bancos de assets e ferramentas de IA para VFX 2D estilo anime (jogo de luta Android)

Notas de pesquisa de 10/11/2026. Método: quase tudo veio de **trechos de resultado do WebSearch** (resumos das páginas), não da página aberta. Depois que o coordenador avisou que a rede foi aberta, refiz WebFetch nas páginas de licença: **continuaram falhando com ENOTFOUND** actionvfx.com, productioncrate.com, fab.com, elements.envato.com, help.elements.envato.com, unity.com, kenney.nl, ambientcg.com, pipoya.net, effekseer.github.io e huggingface.co (precisam ser liberados nas configurações de rede da sessão para leitura direta). **github.com funcionou**: confirmei por leitura direta a licença das amostras do Effekseer, do LayerDiffuse e do Wan 2.2 (marcado "lido direto"). Toda afirmação de licença abaixo deve ser lida como "visto via snippet de busca" salvo indicação, e conferida no texto oficial antes de enviar arte para a `game-luta-sprite-fable`. Isto não é parecer jurídico.

Pipeline de referência: imagem/vídeo gerado (fundo preto ou verde) -> EffectCraft monta/anima -> PNG com alfa -> folha de sprites -> APK Android vendido.

---

## 1. Referências de aprendizado (livros, cursos, comunidades, palestras, animadores)

### Takeaway
As referências que ensinam algo diretamente aplicável ao nosso efeito são poucas e bem conhecidas: o livro *Elemental Magic* (Gilland), os cursos 2D de VFX Apprentice (Jason Keyser, "Masters of Motion"), os sketchbooks/desafios do realtimevfx.com e o Sakugabooru para estudar timing quadro a quadro de efeitos de animação japonesa. A palestra de GDC 2015 da Arc System Works (Guilty Gear Xrd) ensina "limited animation" (poucos quadros, segurar poses), mas segundo os próprios espectadores fala pouco dos efeitos em si.

### Cited Findings
**Livros**
- *Elemental Magic* de Joseph Gilland é livro (não curso); uma listagem o dá como publicado em dezembro de 2012 pela Routledge (provavelmente o Vol. II; o snippet não mostrou preço) — [O'Reilly](https://www.oreilly.com/library/view/elemental-magic/9780240811635/000_9781136137259_acknowledgments.html)
- Há uma página Gumroad "ElementalMagic" e uma "elemental-library" por £19,99/mês no perfil "haloquin" — [Gumroad ElementalMagic](https://haloquin.gumroad.com/l/ElementalMagic); [Gumroad elemental-library](https://haloquin.gumroad.com/l/elemental-library). Não confirmei pelo snippet que "haloquin" é o próprio Gilland nem o conteúdo exato da assinatura.

**VFX Apprentice (Jason Keyser)**
- "Masters of Motion" é um curso de FX 2D com 29 lições, conduzido por Jason Keyser; preço avulso não apareceu — [VFX Apprentice – Masters of Motion](https://www.vfxapprentice.com/masters-of-motion)
- Existem também "Animating Small 2D FX", "VFX Foundations: Artistic Principles", "Applied Foundations: Animations", "Beginner Bootcamp" e um workshop de timing ("Timing in Visual Effects") — [Animating Small 2D FX](https://www.vfxapprentice.com/courses/animating-small-2d-fx); [Foundations](https://www.vfxapprentice.com/courses/vfx-foundations-artistic-principles); [Timing workshop](https://www.vfxapprentice.com/timing-workshop-learn-vfx-timing)
- Assinatura "All Access": US$ 1.270,40/ano à vista (ou US$ 105,86/mês), inclui todos os cursos de FX 2D, 3D e intermediários — [VFX-A All Access](https://www.vfxapprentice.com/all-access); [Pricing](https://www.vfxapprentice.com/pricing)
- Um checkout de US$ 189 aparece (produto não identificado no snippet) — [Checkout](https://www.vfxapprentice.com/offers/oB49UhuY/checkout)
- A mentoria "Stylized VFX in UE5" custa ~US$ 1.600 e é 3D/Unreal, não 2D — [Stylized VFX UE5](https://www.vfxapprentice.com/stylized-vfx-in-unreal-5-guided-training)
- VFX Apprentice tem artigo explicando flipbooks em jogos (o formato exato da nossa folha de sprites) — [What are Flipbooks?](https://www.vfxapprentice.com/blog/what-are-flipbooks-in-games)
- "Flipbooktober": desafio diário em outubro em que a comunidade faz flipbooks, iniciado por Nico Marschall (associado ao VFX Apprentice, não ao realtimevfx) — via snippet de busca sobre [VFX Apprentice blog/Flipbooks](https://www.vfxapprentice.com/blog/what-are-flipbooks-in-games)

**realtimevfx.com (comunidade)**
- Série "VFX Sketch": um tema abstrato para explorar sem limite técnico; a primeira foi "A Haunted Halloween" — [About VFX Sketch Challenges](https://realtimevfx.com/t/about-vfx-sketch-challenges/762); [Official VFX Sketch #1](https://realtimevfx.com/t/official-vfx-sketch-1-a-haunted-halloween/858)
- Desafio mensal com regras; julgamento por mostrar processo ao longo do mês e execução técnica/artística — [About the Monthly VFX Sketches](https://realtimevfx.com/t/about-the-montly-vfx-sketches/18659)
- Sketchbooks de membros com flipbooks 2D pintados à mão; um membro relata pintar animação 2D e gerar a spritesheet com um script pequeno (fluxo parecido com o nosso) — [Rensei's Sketchbook](https://realtimevfx.com/t/renseis-sketchbook/20495); [M0okA Sketchbook](https://realtimevfx.com/t/m0oka-vfx-sketchbook/16167); [Holdimprovae Sketchbook](https://realtimevfx.com/t/holdimprovae-vfx-sketchbook/21413?page=2)
- Há um tópico dedicado a Dragon Ball FighterZ onde se lamenta que a palestra da ArcSys mostre pouco dos efeitos — [realtimevfx – Dragonball Fighter Z](https://realtimevfx.com/t/dragonball-fighter-z/2522)

**Palestras GDC / Arc System Works**
- "GuiltyGearXrd's Art Style: The X Factor Between 2D and 3D", Junya Christopher Motomura (Arc System Works), GDC 2015, trilha Visual Arts — [GDC Vault](https://gdcvault.com/play/1022031/GuiltyGearXrd-s-Art-Style-The)
- A ArcSys publicou a palestra e o handout em PDF — [Arc System Works](https://www.arcsystemworks.com/guilty-gear-xrd-art-talk-at-game-developers-conference-2015/); [PDF Motomura](https://www.ggxrd.com/Motomura_Junya_GuiltyGearXrd.pdf)
- Conteúdo central: "limited animation" (poucos quadros, estilo de anime); animação completa foi testada e rejeitada porque deixava o visual "3D demais"; deformação de malha em cada keyframe para quebrar a rigidez — [Game Developer](https://gamedeveloper.com/art/see-i-guilty-gear-xrd-i-s-striking-2d-3d-art-deconstructed-at-gdc-2015); [Game Watch (JP)](https://game.watch.impress.co.jp/docs/news/20150307_691725.html)
- Kotaku descreve um vídeo que destrincha a animação de Guilty Gear Xrd e DBFZ, incluindo "o poder dos efeitos visuais nos ataques" para o feel de animação à mão — [Kotaku](https://kotaku.com/breaking-down-the-animation-of-guilty-gear-and-dragonba-1836849279)
- Não achei palestra GDC específica de efeitos de DBFZ, Granblue Versus, Genshin, Honkai Star Rail ou Street Fighter 6 nas buscas feitas (ver Gaps).

**Sakuga / animação japonesa de efeitos**
- Sakugabooru: clipes de animação marcados com tags pesquisáveis; o player tem 5 velocidades (0,2x até normal) e avanço quadro a quadro — base para estudar timing — [Sakugabooru blog](https://blog.sakugabooru.com/2018/08/16/whats-the-most-popular-japanese-animation/)
- Segundo o próprio blog do Sakugabooru (2018), as sequências mais votadas são lutas com efeitos 2D e mais da metade das mais bem avaliadas têm efeitos desenhados à mão como protagonista — [Sakugabooru blog](https://blog.sakugabooru.com/2018/08/16/whats-the-most-popular-japanese-animation/)
- "Impact frames": desenhos monocromáticos/estilizados inseridos no meio da sequência, piscando brevemente; Know Your Meme credita Yutaka Nakamura como um dos primeiros usuários de destaque — [Sakuga Glossary](https://skool.com/sakuga-design-club-8919/sakuga-glossary); [Know Your Meme](https://knowyourmeme.com/memes/cultures/impact-frames)
- Há a tag "smears" entre as tags de clipes de efeito do Sakugabooru (vista via dataset no Hugging Face) — [HF dataset sakuga](https://huggingface.co/datasets/Hemabhushan/sakuga_preprocessed/viewer/seg_1/train)
- Ensaio de 2024 sobre a comunidade sakuga, com falas de animador sobre impact frames — [gwern.net PDF](https://gwern.net/doc/anime/2024-tai.pdf)

### Inferences
- Para o nosso pipeline, o que essas fontes ensinam de acionável: (a) **timing em "2s/3s" com poses seguradas** (limited animation da ArcSys) — no EffectCraft isso é "hold" de quadros, não interpolação contínua; (b) **impact frame**: 1–2 quadros chapados (preto/branco ou cor invertida) no contato, que é barato de fazer como camada extra; (c) **smear/forma antes de detalhe**: o curso de Keyser e o livro de Gilland tratam de forma e silhueta do elemento (fogo, fumaça, água, energia) — útil como critério para escolher qual imagem gerada aproveitar.
- Sakugabooru serve para **estudar contagem de quadros** (framestep) de efeitos de animadores; respeitando a regra do projeto, só como estudo — nenhum quadro de lá entra no repositório.
- Para uma equipe pequena, o All Access do VFX Apprentice (~US$ 106/mês, cancela quando quiser) é a forma mais barata de acessar todo o material 2D de uma vez; "Masters of Motion" e "Animating Small 2D FX" são os mais alinhados a golpes/impactos.

### Gaps
- Não encontrei preço avulso de "Masters of Motion" nem índice das 29 lições.
- Não confirmei o conteúdo exato da Vol. I vs Vol. II de *Elemental Magic* nem preço atual.
- Artistas citados no pedido (Sirhaian, Simon Trümpler, Harry Alisavakis, "Tobias ...") e animadores (Yoshinori Kanada, Hiroyuki Imaishi): não pesquisei individualmente por limite de chamadas; nenhum fato sobre eles nesta nota.
- Palestras GDC sobre efeitos de DBFZ, Granblue Fantasy Versus, Genshin Impact, Honkai Star Rail, Street Fighter 6: nenhuma localizada nas buscas; não afirmo que existem ou não.
- Snippets não mostraram as datas das páginas do VFX Apprentice; preços podem estar desatualizados.

---

## 2. Bancos de assets gratuitos/baratos e o que a licença permite num jogo Android vendido

### Takeaway
Licença totalmente limpa para jogo comercial (sem atribuição, sem restrição de motor) só nos CC0: Kenney, alguns packs do itch.io/OpenGameArt (ex.: PVFX Foundry Thirteen, "Hit Animation" da Sinestesia) e, por conhecimento geral, ambientCG (não confirmado nesta sessão). Unity Asset Store (EULA padrão) e Fab (Standard License) permitem usar as texturas fora da Unity/Unreal dentro de um jogo, mas proíbem revender/redistribuir os arquivos-fonte. Envato Elements permite o item embutido num "End Product", com proibição de extração e ressalvas sobre assinatura; ProductionCrate grátis pede atribuição. Bancos japoneses (ニコニ・コモンズ, BOOTH, ぴぽや) são **por item** e precisam de leitura individual.

### Cited Findings
**Kenney (CC0)**
- FAQ da Kenney: todos os assets de jogo das páginas de asset são domínio público (CC0); atribuição não é exigida (pode creditar "Kenney" se quiser) — [kenney.nl/support](https://kenney.nl/support)
- "Particle Pack" da Kenney é CC0, 80 arquivos (partículas, light cookies, para shaders) — [Kenney Particle Pack](https://kenney.nl/assets/particle-pack); cópia no [Godot Asset Library](https://godotengine.org/asset-library/asset/784)
- Cuidado com recópias: CC0 permite reempacotar sob outra licença; baixe do kenney.nl — [itch.io post](https://itch.io/post/9879907)

**OpenGameArt (licença por asset)**
- CC0: uso livre, sem crédito; CC-BY 3.0: exige crédito ao autor; CC-BY-SA: derivados devem sair sob a mesma licença; OGA-BY 4.0: CC-BY 4.0 sem a cláusula anti-DRM (útil para loja de apps com proteção) — [OpenGameArt – Attribution and Licenses](https://opengameart.org/node/55224); [FAQ drafts/comentários](https://opengameart.org/comment/100017)
- Arte GPL: o próprio site diz que uso comercial "talvez" seja ok dependendo de como é carregada, mas em geral só se o projeto inteiro for GPL; a FAQ diz que não é documento legal (textos de 2015–2016) — [OpenGameArt node 44294](https://opengameart.org/node/44294)
- "Hit Animation – frame by frame" (Sinestesia) no OpenGameArt: CC0, 16 quadros de 1024x1024 — [OpenGameArt](https://opengameart.org/content/hit-animation-frame-by-frame); [versão 2](https://opengameart.org/content/hit-animation-2-frame-by-frame). No itch.io a mesma autora cobra mínimo de US$ 2 e a licença lá é informal — [sinestesia.itch.io](https://sinestesia.itch.io/hit-animations-)

**itch.io**
- PVFX Foundry Thirteen (nerijs): folhas renderizadas + metadados em CC0 1.0; uso comercial, modificação e redistribuição permitidos; 26 spritesheets PNG transparentes; estilo genérico de combate/elemental (não anime) — [nerijs.itch.io/pvfx-foundry-thirteen](https://nerijs.itch.io/pvfx-foundry-thirteen); [PVFX Foundry](https://nerijs.itch.io/pvfx-foundry)
- Impact FX (RagnaPixel): grátis, comercial, mas **CC BY 4.0 (crédito obrigatório)** — [ragnapixel.itch.io/impact-fx](https://ragnapixel.itch.io/impact-fx)
- Hit effects (Lil Cthulhu): grátis para uso pessoal/comercial, mas proíbe vender ou redistribuir o asset — [lil-cthulhu.itch.io/hit-effect](https://lil-cthulhu.itch.io/hit-effect)
- Megapack Spritesheets (CartoonCoffee): o mais "anime/flashy"; licença própria, permite comercial e modificações só para uso nos seus projetos — [cartooncoffee.itch.io/megapack-spritesheets](https://cartooncoffee.itch.io/megapack-spritesheets)
- Outros vistos (licença não verificada no snippet): [Pixelated Attack/Hit Animations](https://v-ktor.itch.io/pixelated-attackhit-animations), [Explosion minipack (Reactorcore)](https://reactorcore.itch.io/explosion-spriteanim-sheet-minipack), [Brackeys VFX Bundle](https://brackeysgames.itch.io/brackeys-vfx-bundle)

**Unity Asset Store**
- Suporte oficial da Unity: assets da Asset Store podem ser usados com outros motores, desde que se cumpra o EULA — [Unity Support – other engines](https://support.unity.com/hc/en-us/articles/34387186019988-Can-I-use-assets-from-the-Asset-Store-with-other-engines); uso em jogo comercial: [Unity Support – commercial game](https://support.unity.com/hc/en-us/articles/205623589-Can-I-use-assets-from-the-Asset-Store-in-my-commercial-game)
- EULA padrão: uso "apenas como componentes incorporados e embutidos em jogos eletrônicos e mídia interativa"; não pode duplicar o asset nem incluí-lo em produto do tipo asset; "Restricted Assets" são só uso pessoal/não comercial; proíbe usar assets para treinar IA sem consentimento; termos atualizados em 4/12/2024 — [Unity Asset Store Terms](https://unity.com/de/legal/as-terms); [EULA FAQ](https://assetstore.unity.com/browse/eula-faq); [AI training](https://support.unity.com/hc/en-us/articles/16455448218516-Can-I-use-assets-to-train-AI-models)
- Cada página de asset pode ter EULA não padrão (terceiros ou companion license) — mesma fonte acima.

**Fab (Epic)**
- Press release da Epic (via snippet): a Fab Standard License permite usar os assets, incluindo Megascans, "em qualquer motor ou ferramenta"; moderador confirma para licenças Standard e Professional — [fórum Unreal](https://forums.unrealengine.com/t/ability-to-use-purchased-items-outside-of-unreal-engine/2370367); [Creative Bloq](https://www.creativebloq.com/3d/epic-games-fab-is-a-one-stop-content-marketplace-for-everyone-and-its-just-launched)
- Permite distribuir comercialmente projetos com o asset incorporado; proíbe revender/redistribuir os assets-fonte; mesmas regras para grátis e pago; exceções: "Reference Only", "Quixel Legacy" e conteúdo de exemplo/tutorial da Epic (só Unreal) — [fórum Unreal – free assets](https://forums.unrealengine.com/t/all-free-assets-including-ue-ones-are-now-standard-license-on-fab-meaning-no-commercial-use/2080626); [fórum – other assets](https://forums.unrealengine.com/t/is-use-of-other-assets-allowed-under-fab-standard-license/2564928); [fab.com/cla](https://www.fab.com/cla). Texto integral do EULA (fab.com/eula) não foi lido.

**Envato Elements**
- "End Product" (vídeo, gráficos, foto, 3D) = obra que incorpora o item e outras coisas, maior em escopo e diferente em natureza; após incorporado durante a assinatura, a licença daquele uso fica perpétua; não pode criar novo End Product depois que a assinatura acaba — [Envato Elements License Terms](https://help.elements.envato.com/hc/en-us/articles/360000628966-License-Terms)
- Itens incorporados não podem ser extraídos ou usados separadamente; proibido redistribuir como asset de estoque, ferramenta, template ou arquivo-fonte; não pode em serviços "on-demand"; fontes e add-ons não podem ser distribuídos dentro do End Product — [Envato help 360000621803](https://help.elements.envato.com/hc/en-us/articles/360000621803); [Envato help 360000621783](https://help.elements.envato.com/hc/en-us/articles/360000621783)
- Tópico no fórum Envato sobre jogo + assinatura Elements sem resposta clara sobre updates após cancelar; uma resposta de usuário diz que não se pode vender o jogo como forma de revender os itens — [fórum Envato](https://forums.envato.com/t/video-game-licensing-and-envato-elements-subscription/477876)
- Licença de vídeo do ThemeForest (Market, não Elements) lista jogos entre exemplos de End Product — [ThemeForest video license](https://themeforest.net/licenses/terms/video_media_multi)

**ActionVFX**
- Página de licença (vista só numa cópia em domínio Vercel, não no site oficial): arquivos podem ser usados em "quase qualquer projeto", incluindo **games**, para sempre; permite criar novas mídias por IA/ML; restringe NFT; uso para cliente remete a outra licença — [actionvfx license (Vercel)](https://actionvfx-frontend-eta.vercel.app/license)
- ActionVFX lançou em 2023 uma biblioteca grátis de "Practice Footage" — [CG Channel](https://www.cgchannel.com/2023/10/actionvfx-launches-free-practice-footage-library/). Termos dessa biblioteca grátis não apareceram.
- Plano de 2021 de oferecer elementos prontos para motores de jogo — [befores & afters](https://beforesandafters.com/2021/12/10/soon-youll-be-able-to-get-stock-effects-elements-for-game-engines/)
- Fetch de actionvfx.com falhou (ENOTFOUND).

**ProductionCrate**
- Só fonte de terceiro: plano grátis com uso comercial limitado, **atribuição obrigatória** e limite de downloads; Pro (~US$ 12/mês) sem atribuição; tabela do guia é confusa — [licenseorg.com](https://licenseorg.com/guide/video/productioncrate). Fetch de productioncrate.com falhou (ENOTFOUND).

**Japão: ニコニ・コモンズ, BOOTH, ぴぽや**
- ニコニ・コモンズ: condições **por obra**, definidas pelo detentor (sistema parecido com CC: BY-NC só não comercial; BY-ND comercial sem alterar), e o detentor pode mudar o escopo depois; alguns materiais (ex.: Nash Music Library) exigem licença paga fora dos serviços Niconico — [bloomeria.jp](https://bloomeria.jp/blog/streaming-se-sound-effect-sites); [ニコニコ大百科](https://dic.nicovideo.jp/t/a/%E3%83%8B%E3%82%B3%E3%83%8B%E3%83%BB%E3%82%B3%E3%83%A2%E3%83%B3%E3%82%BA)
- BOOTH: termos ficam na descrição de cada item ("■ 利用規約"); exemplo PaperPop Eldritch: 16 efeitos, "商用可", sem crédito obrigatório, para Cocoforia/RPG Maker MZ/Unity — [BOOTH PaperPop Eldritch](https://booth.pm/ja/items/8955898); outros itens de efeito: [booth 7429485](https://booth.pm/en/items/7429485), [booth 7507293](https://booth.pm/ja/items/7507293)
- ぴぽや: termos oficiais **não** apareceram na busca.

**Effekseer (lido direto no GitHub)**
- O código do Effekseer é MIT ("Copyright (c) 2011 Effekseer Project") — [LICENSE](https://github.com/effekseer/Effekseer/blob/master/LICENSE)
- Amostras de efeito: "All the licenses of these effects are CC-0."; crédito a Effekseer e ao autor é **pedido, não obrigatório** ("We'd appreciate it if you could credit..."); o diretório tem só parte das amostras, as outras ficam no site oficial — [docs/readme_sample.txt](https://github.com/effekseer/Effekseer/blob/master/docs/readme_sample.txt). Amostras ficam em `Release/Sample/` (ex.: `01_NextSoft01/Texture/Cloud01.png`, visto via busca de código). Não confirmei se as amostras baixadas do site oficial têm o mesmo termo.

### Inferences
- **Pode ir para dentro do APK sem medo (após conferir a página):** Kenney (CC0), PVFX Foundry Thirteen (CC0), Sinestesia no OpenGameArt (CC0). Com crédito na tela de créditos: Impact FX (CC BY 4.0), OGA-BY, CC-BY do OpenGameArt.
- **Evitar:** CC-BY-SA e GPL (contaminam licença do jogo/arte), CC-NC (proíbe comercial), "Restricted Assets" da Unity, "Reference Only" do Fab, ProductionCrate grátis se não quisermos crédito.
- **Redistribuição de arquivos crus:** quase todas as licenças pagas (Unity, Fab, Envato, ActionVFX, Lil Cthulhu) proíbem redistribuir o asset solto. Um APK é "extraível" (PNG dentro do pacote). Unity/Fab aceitam isso por ser "incorporado em jogo"; Envato fala em "não pode ser extraído" — interpretação conservadora: não use Envato em folhas de sprite que ficam soltas no APK sem antes perguntar ao suporte.
- **Repositório:** como o repositório não deve receber arquivos com redistribuição proibida, assets pagos (Unity/Fab/Envato/ActionVFX) não deveriam ser comitados crus num repo público; só o resultado montado (e mesmo isso é discutível para Envato). CC0 pode ir ao repo.
- **Como entram no pipeline:** texturas de ruído/fumaça CC0 (Kenney, ambientCG) servem de camada de distorção/máscara no EffectCraft sobre a imagem gerada por IA; stock footage de fogo/fumaça em fundo preto (ActionVFX) entra como camada com modo "screen"/add e é convertido a alfa antes da folha. Estilo anime "chapado" quase não existe nesses bancos; eles servem mais como textura de apoio do que como efeito final — coerente com a preferência do dono por imagens geradas.
- Unity proíbe treinar IA com assets; se formos treinar uma LoRA de estilo, não usar assets da Unity Store (e, por cautela, nenhum asset pago) no dataset.

### Gaps
- Effekseer: amostras do repositório confirmadas CC-0; as do site oficial (effekseer.github.io, bloqueado) não verificadas.
- ぴぽや e sites japoneses de "効果素材" (ex.: ぴぽや, 効果素材系): termos não vistos.
- ambientCG: a busca não retornou nada; CC0 é conhecimento geral, não confirmado nesta sessão.
- ActionVFX: texto oficial não lido (só cópia em Vercel); termos da Practice Footage grátis desconhecidos.
- ProductionCrate: texto oficial não lido; redação exata do crédito desconhecida.
- Motion Array: não pesquisado.
- Fab: texto integral do EULA (fab.com/eula) não lido; só fórum/press release.
- Envato: nenhuma passagem oficial trata especificamente de jogo/app; ponto em aberto.

---

## 3. Ferramentas de IA que geram imagens/vídeos de efeito (termos de uso, local/headless, alfa, consistência)

### Takeaway
Para gerar o efeito em si, as opções com termos de saída mais claros para uso comercial são: OpenAI (usuário é dono da saída), Midjourney pago (com teto de receita de US$ 1M para planos básicos), FLUX.1 [dev] local (saídas comerciais permitidas, modelo não), Wan 2.2 local (Apache 2.0), Runway pago (não restringe uso comercial). Kling e Luma grátis **não** dão direito comercial; Seedance/Dreamina não têm termo comercial claro achado. Para alfa direto existe LayerDiffuse (Apache 2.0, SD1.5/SDXL; versão Flux com licença não confirmada). Para interpolação/upscale, RIFE e Real-ESRGAN têm ports ncnn-vulkan MIT que rodam local e headless.

### Cited Findings
**Imagem**
- OpenAI: entre você e a OpenAI, você é dono da saída e a OpenAI cede os direitos que tiver, "na medida permitida pela lei aplicável" — [OpenAI Terms of Use](https://openai.com/policies/terms-of-use/)
- Ressalvas: cessão contratual não garante copyright (sem proteção, terceiros podem copiar); saída de outro usuário não é sua — [terms.law – ChatGPT](https://terms.law/ai-output-rights/chatgpt/); a alegação de que a OpenAI retém licença concorrente vem só de agregador — [ConductAtlas](https://conductatlas.com/platform/openai/openai-terms-of-use/user-generated-content-and-output-ownership/)
- Midjourney: empresa com receita > US$ 1.000.000/ano precisa de Pro ou Mega para ser dona dos assets; Basic (~US$ 10/mês) e Standard (~US$ 30/mês) cobrem comercial abaixo disso; imagens de trial = CC BY-NC 4.0 (não comercial). Fonte principal via agregador citando o ToS — [ConductAtlas – Midjourney ToS](https://conductatlas.com/platform/midjourney/midjourney-terms-of-service/revenue-based-asset-ownership-threshold/); [terms.law – Midjourney](https://terms.law/2026/01/15/midjourney-commercial-use-rights-complete-2026-guide/)
- FLUX.1 [dev]: licença não comercial para o **modelo**, mas "We claim no ownership rights in and to the Outputs" e o model card diz que as saídas podem ser usadas para fins pessoais, científicos e **comerciais**; restrição citada: não usar saídas para treinar modelo concorrente; há opiniões divergentes em Reddit/terceiros — [Hugging Face FLUX.1-dev](https://huggingface.co/black-forest-labs/FLUX.1-dev); [texto da licença](https://open.windriver.com/info/uni-license-list/licenses/flux-1-nc.html); [discussão HF](https://huggingface.co/black-forest-labs/FLUX.1-dev/discussions/7)
- FLUX.1 [schnell] é Apache 2.0 (via snippet secundário) — mesma busca, sem página primária aberta.

**Alfa direto**
- LayerDiffuse (lllyasviel) gera imagens transparentes; repositório original Apache-2.0 (**lido direto**: o GitHub mostra "Apache-2.0 license"; a página não declara licença separada para os pesos nem lista SD1.5/SDXL/Flux, só plataformas Forge, Diffusers CLI, Fooocus, ComfyUI) — [GitHub LayerDiffuse](https://github.com/lllyasviel/LayerDiffuse); nó ComfyUI (huchenlei/ComfyUI-layerdiffuse) listado como Apache-2.0 — [awesome.ecosyste.ms – LayerDiffuse](https://awesome.ecosyste.ms/projects/github.com%2Flllyasviel%2FLayerDiffuse); [Salt docs – ComfyUI-layerdiffuse](https://docs.getsalt.ai/md/ComfyUI-layerdiffuse/); [RunComfy workflow](https://www.runcomfy.com/comfyui-workflows/generating-transparent-images-with-layerdiffuse-in-comfyui)
- Existem implementações para Flux (ComfyUI_FluxLayerDiffuse; RedAIGC/Flux-version-LayerDiffuse) sem licença confirmada — [RunComfy – FluxLayerDiffuse](https://www.runcomfy.com/comfyui-nodes/ComfyUI_FluxLayerDiffuse)
- Runware oferece LayerDiffuse hospedado com tag "Commercial use", sem termos detalhados — [Runware](https://runware.ai/models/layerdiffuse)

**Vídeo**
- Wan 2.2 (Alibaba), **lido direto no GitHub**: "The models in this repository are licensed under the Apache 2.0 License" e "We claim no rights over the your generated contents, granting you the freedom to use them"; restrições de uso responsável (nada ilegal, que cause dano, desinformação etc.) — [GitHub Wan2.2](https://github.com/Wan-Video/Wan2.2). Também: suporte nativo no ComfyUI desde o lançamento (28/07/2025), aceita LoRA; termos exatos no model card Wan-AI no Hugging Face; versões hospedadas/API (ex. Wan 2.7) são outro termo — [ComfyUI Wiki – Wan2.2](https://comfyui-wiki.com/en/news/2025-07-28-wan2-2-open-source-release); [MindStudio – Wan 2.2](https://www.mindstudio.ai/blog/what-is-wan-2-2-video-open-source)
- Kling: plano grátis (Basic) "not for commercial use"; Standard pago libera remoção de marca d'água e uso comercial; ToS ambíguo sobre clipes feitos no grátis depois de assinar — [eesel.ai – Kling pricing](https://eesel.ai/blog/kling-ai-pricing); [ConductAtlas – Kling ToS](https://conductatlas.com/platform/kling-ai/kling-ai-terms-of-service/); [licenseorg – AI video 2026](https://licenseorg.com/blog/ai-video-licensing-2026)
- Runway: segundo comparativo citando termos de 23/10/2026, Runway "não restringe o uso comercial dos Outputs" e não reivindica propriedade; exportações grátis com marca d'água — [SeedVideo AI comparativo](https://www.seeddance.io/blog/ai-video-commercial-licenses-compared) (fonte de vendedor concorrente)
- Luma: direito comercial a partir do Plus; preso a cada clipe — clipe feito no grátis continua uso pessoal mesmo após upgrade; clipes pagos mantêm direito após cancelar — mesma fonte acima
- Veo (Google Flow): exige assinatura Google AI; Google não reivindica propriedade; artigo antigo dizia que Veo 3 pré-GA vetava uso comercial — possivelmente desatualizado — mesma fonte acima; [licenseorg](https://licenseorg.com/blog/ai-video-licensing-2026)
- Seedance/Dreamina: nenhuma licença comercial oficial encontrada; página do Seedance 2.0 Mini não define licença comercial nem propriedade; API de terceiros pode ter rótulo próprio; disputa de copyright com Hollywood em aberto (2026) — [CapCut – Seedance 2.0 Mini](https://www.capcut.com/create/seedance-2-0-mini-commercial-use-rights); [Opus Pro](https://www.opus.pro/blog/seedance-2-0-hollywood-copyright-debate-ai-video-creators); [Zaikei (JP, 08/07/2026)](https://zaikei.co.jp/article/20260708/860296.html)
- Dreamina mantém página própria sobre quais geradores grátis permitem uso comercial (vendedor, não termo) — [Dreamina](https://dreamina.capcut.com/ai-video/free-ai-video-commercial-use)

**Interpolação / upscale (local, headless)**
- rife-ncnn-vulkan (nihui) MIT; Real-ESRGAN-ncnn-vulkan (xinntao) MIT; Real-CUGAN e Anime4K MIT; framework ncnn (Tencent) BSD-3; Video2X, que empacota tudo, é AGPL-3.0 — [Video2X README](https://github.com/k4yt3x/video2x)

### Inferences
- **Local e headless (cabe num script como o EffectCraft):** ComfyUI + FLUX.1 [dev]/SDXL + LayerDiffuse para imagem com alfa; ComfyUI + Wan 2.2 para vídeo; rife-ncnn-vulkan e realesrgan-ncnn-vulkan por linha de comando. Usar os binários ncnn diretamente (MIT) evita a AGPL do Video2X.
- **Na nuvem:** OpenAI (já usado pelo dono via Codex), Midjourney, Kling, Runway, Luma, Veo, Seedance — exigem plano pago para uso comercial (exceto OpenAI, cujos termos não distinguem plano nos snippets vistos).
- **Fundo preto vs alfa direto:** efeitos de energia/fogo/luz gerados em fundo preto podem virar alfa no EffectCraft por "unmultiply"/luminância (preto = transparente), o que funciona melhor que chroma verde para efeitos aditivos e brilhantes; LayerDiffuse vale para efeitos opacos (fumaça densa, poeira, detritos) onde o preto viraria buraco.
- **Consistência de família de efeitos:** (1) imagem de referência fixa (o golpe aprovado) passada como referência/estilo em cada geração; (2) seed fixa + prompt-base fixo com só o elemento variando; (3) LoRA treinada com efeitos **nossos já aprovados** (não com frames de outros jogos nem assets pagos — Unity proíbe treinar IA com seus assets e FLUX dev veta treinar modelo concorrente com saídas); (4) paleta limitada aplicada depois no EffectCraft (mapa de gradiente) para igualar cores entre ferramentas diferentes.
- **Vídeo -> folha:** gerar 24–30 fps, cortar o trecho útil, reduzir para timing de anime (segurar quadros em 2s/3s) em vez de interpolar; RIFE serve para o caminho inverso (poucos quadros-chave gerados -> intermediários), mas suaviza demais para o visual "limited animation" — usar com moderação.
- **Risco legal maior** está em vídeo: Seedance sem termo claro e com disputa de copyright; Kling/Luma no grátis sem direito comercial e sem reabilitação posterior. Gerar o material final só em plano pago ou em modelo local.

### Gaps
- Termos de Pika, Hailuo/MiniMax, Sora e Stable Diffusion 3.x/SDXL não pesquisados nesta sessão.
- Termos específicos de geração de imagem da OpenAI (gpt-image, fundo transparente) não confirmados; só o ToS geral.
- Termos oficiais de Kling, Luma, Runway, Veo não lidos diretamente — tudo via agregadores (alguns de concorrentes).
- Licença das versões Flux do LayerDiffuse e dos pesos do LayerDiffuse: não confirmada.
- Licença dos pesos de RIFE/Real-ESRGAN (separada do código) não verificada.
- Não encontrei LoRAs públicas específicas de "anime effects" com licença verificada (não pesquisado por limite de chamadas).

---

## 4. Coleções abertas de flipbooks/tutoriais de efeito anime com uso comercial explícito

### Takeaway
Não encontrei coleção aberta de flipbooks **em estilo anime** com licença CC0 explícita. O que existe com licença clara são packs genéricos (PVFX Foundry Thirteen CC0, Kenney Particle Pack CC0, Sinestesia CC0, RagnaPixel CC BY 4.0) e um pack de estilo mais anime/flashy com licença própria (CartoonCoffee).

### Cited Findings
- Nenhum pack anime CC0 grátis localizado no itch.io; o mais próximo de anime (CartoonCoffee Megapack) tem licença própria que permite comercial e modificação só para uso nos próprios projetos — [cartooncoffee.itch.io](https://cartooncoffee.itch.io/megapack-spritesheets)
- PVFX Foundry Thirteen: CC0 1.0, 26 folhas PNG transparentes, inclui metadados/documentação, redistribuição permitida — [nerijs.itch.io](https://nerijs.itch.io/pvfx-foundry-thirteen)
- Kenney Particle Pack: CC0, 80 sprites — [kenney.nl](https://kenney.nl/assets/particle-pack)
- Sinestesia "Hit Animation": CC0 no OpenGameArt, 16 quadros 1024x1024 — [OpenGameArt](https://opengameart.org/content/hit-animation-frame-by-frame)
- RagnaPixel Impact FX: CC BY 4.0, livre e comercial com crédito — [ragnapixel.itch.io](https://ragnapixel.itch.io/impact-fx)
- Pacote de partículas de 80+ sprites no OpenGameArt e pacotes de fumaça (licença a conferir por item) — [Particle Pack 80 sprites](https://opengameart.org/content/particle-pack-80-sprites); [Smoke particle assets](https://opengameart.org/content/smoke-particle-assets)
- BOOTH tem packs de efeito japoneses marcados "商用可" e sem crédito obrigatório, mas são pagos e de termo próprio — [BOOTH 8955898](https://booth.pm/ja/items/8955898)

### Inferences
- Para o estilo anime que o dono quer, nenhum banco aberto resolve sozinho; os CC0 servem como **camada de apoio** (faísca, poeira, partículas, ruído) no EffectCraft por cima da peça principal gerada por IA.
- O pack Sinestesia (1024 px, 16 quadros) tem resolução boa para recorte/remontagem; PVFX traz metadados de folha, útil para comparar com o formato da nossa `assets/efeitos`.
- Para não poluir o estilo, qualquer asset de terceiro deve passar pelo mesmo mapa de gradiente/paleta do jogo antes de entrar na folha.

### Gaps
- **Achado após reabrir a rede:** as amostras do Effekseer no GitHub são CC-0 com crédito opcional ([readme_sample.txt](https://github.com/effekseer/Effekseer/blob/master/docs/readme_sample.txt)); são efeitos de partícula do Effekseer (arquivo .efkefc + texturas), não flipbooks prontos — as texturas podem ser usadas direto e os efeitos podem ser renderizados para PNG no próprio editor Effekseer (inferência; não testado). Estilo visual das amostras não verificado.
- Datasets de sakuga no Hugging Face (ex.: "ilove-anime-sakuga-1TiB") existem, mas são frames de anime com copyright; **não servem** para o projeto (nem como dataset de LoRA, pela regra de não copiar) — [HF dataset](https://huggingface.co/datasets/ilovehentai9000/ilove-anime-sakuga-1TiB). Licença deles não verificada.
- Não pesquisei GitHub por coleções "anime vfx flipbook CC0" especificamente.
