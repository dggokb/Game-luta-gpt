# Legibilidade, composição, câmera e HUD em jogos de luta 2D (profissional vs amador)

Legenda usada em cada item: **[DOCUMENTED]** = um desenvolvedor, artista ou fonte oficial disse isso. **[INFERRED]** = medido ou deduzido por terceiros (ou por mim), ou fonte fraca/anedótica.

Observação geral sobre as fontes: a fonte primária mais forte que achei é a palestra de Toshiyuki Kamei (diretor de arte de SFIV e SFV, Capcom), "Art Direction of Street Fighter V — The Role of Art in Fighting Games" (CEDEC 2016, repetida na GDC 2017), com duas coberturas japonesas detalhadas (Famitsu e GAME Watch). Não consegui baixar o handout da palestra de Guilty Gear Xrd (GDC 2015, Junya C. Motomura): o servidor devolveu 503. O Medium de Dan Fornace (Rivals of Aether) devolveu 403, então as citações dele vêm dos trechos que a busca mostrou. Não achei fontes primárias de SNK (KOF XV, City of the Wolves), Lab Zero/Mike Z ou DBFZ sobre legibilidade sobre o cenário.

---

## 1. Contraste personagem vs cenário (valor, saturação, densidade de detalhe, blur, luz separada)

### Takeaway
A regra documentada mais clara vem da Capcom: personagens em cor cheia e cenário com paleta mais escura, mais limitada e menos saturada. Em SFV o cenário é escurecido de propósito por color grading. Sem esse grading, a luz do cenário "se mistura" ao personagem e a visibilidade cai. Indies (Rivals of Aether, Awesomenauts) chegam na mesma hierarquia: personagem com mais contraste e saturação, depois o chão/colisão, depois o fundo próximo e, por último, o fundo distante. Isso se reforça com contorno e cel shading só nos elementos de jogo, fundo pictórico sem contorno, desaturação e depth-of-field.

### Cited Findings
- **[DOCUMENTED]** Kamei (Capcom): a tradição de Street Fighter é personagem em cor cheia e cenário em paleta mais escura e limitada, para a atenção ficar nos lutadores. SFV usa o color grading do Unreal Engine 4 para **escurecer intencionalmente o cenário**. Com o grading desligado, a iluminação se mistura aos personagens e a visibilidade cai. — [GAME Watch, GDC 2017, Kamei](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** Kamei: a saturação de cor do cenário é mantida baixa por visibilidade, entre outros motivos. — [Famitsu, GDC 2017 / CEDEC 2016](https://www.famitsu.com/news/201703/03128250.html)
- **[DOCUMENTED]** SFV tem 4 tabelas de color grading (esquerda/direita, perto/longe) para ajustar cada ambiente, como interno e externo. A cor e a saturação da luz indireta também são controladas: o Indirect Lighting Cache dá a base e o Enlighten acrescenta cor artística independente da luz principal. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** Kamei: nenhum cenário usa sombra preta pura. Preto puro fica "não pictórico", então cada estágio usa uma cor de acento ou característica na sombra. — [Famitsu](https://www.famitsu.com/news/201703/03128250.html); [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** SFV aplica um pós-processo de realce de borda (edge enhancement) na tela inteira. Ele adiciona cor onde o contraste de cor é forte, dando aspecto de pintura a óleo **e melhorando a visibilidade**. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** Correção de projeção em SFV: o shader "Fix Projection" reduz a distorção nas bordas da tela. Personagens recebem correção mais fraca (precisão de colisão, mistura 50/50) e cenários, correção mais forte (impacto). — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED, indie]** Dan Fornace (Rivals of Aether) propõe uma hierarquia de contraste: personagens com o maior contraste, depois elementos de colisão (chão, paredes, plataformas), depois elementos de fundo próximos e, por último, o fundo distante. Ele evita preto sólido em tudo que não é relevante para o jogo. Em outro post: "make sure your characters have more saturation and contrast than your background elements". — [Fornace, The Power of Silhouettes (Medium)](https://fornace.medium.com/fighting-game-design-with-dan-fornace-the-power-of-silhouettes-915fde48318f); [Fornace, 10 Tips (Medium)](https://fornace.medium.com/dan-fornaces-10-tips-for-making-a-fighting-game-e2c982da2396). Ressalva: as duas páginas deram 403. As frases vêm do trecho do buscador, não da leitura completa.
- **[DOCUMENTED, indie, não é jogo de luta]** Ronimo (Awesomenauts), "Making gameplay stand out against rich backgrounds":
  - personagens, magias e torres têm contorno e cel shading, enquanto o fundo é "more painterly, lacking outlines";
  - os personagens finais ganharam "deeper shading and more saturated colours";
  - "background graphics are less saturated";
  - o fundo é mais claro perto da camada de jogo, "adding contrast";
  - "depth of field blur on the background not only adds depth but also makes the foreground pop";
  - detalhe foi removido logo atrás da área de jogo (os arbustos sem neve formam uma borda verde que separa a área de jogo das árvores nevadas);
  - elementos que trocam de papel são puxados para uma cor única.
  — [Joost's Dev Blog (Ronimo)](http://joostdevblog.blogspot.com/2015/05/making-gameplay-stand-out-against-rich.html)
- **[DOCUMENTED, indie]** Devlog de Eric Dick: os cenários foram redesenhados para melhorar composição, contraste e legibilidade, e agora "competem menos com os personagens". — [Eric Dick devlog](https://ericdick.itch.io/eric-dick/devlog/1165126/visual-polish-improved-backgrounds)
- **[DOCUMENTED, indie]** Moldy Skull Games (Zeitgeist): uma paleta própria para cada personagem ajuda muito a legibilidade. O cenário deve ser interessante, mas não dominante. — [Moldy Skull devlog](https://moldy-skull-games.itch.io/zeitgeist/devlog/577786/3-fighting-game-readability-and-you)
- **[DOCUMENTED]** Sakurai (Smash): a equipe de modelagem quer detalhar tudo, mas "you need to strike a balance". O esforço de detalhe deve ir para os elementos que o jogador vê com mais frequência (distinção entre elementos gráficos principais e secundários). — [GoNintendo sobre o vídeo de Sakurai](https://www.gonintendo.com/contents/13084-sakurai-s-latest-video-details-distinguishing-between-major-and-minor-graphical)
- **[DOCUMENTED]** SF6, cor como identificação: o diretor de arte Kaname Fujioka diz que Luke usa azul e laranja "para se destacar do resto do elenco", e que Ken recebeu cores mais vivas, "fáceis de entender". — [Game Informer, entrevista Fujioka](https://www.gameinformer.com/2022/11/23/street-fighter-6-art-director-breaks-down-each-revealed-character-design)

### Inferences
- **[INFERRED]** Para este projeto (cenários pintados e cheios de detalhe, sprites vindos de vídeo de IA), a correção mais barata e mais respaldada é aplicar ao cenário, em tempo real, um "grading de cenário" separado do personagem: escurecer (cair em valor), baixar a saturação e, de preferência, desfocar levemente os planos de fundo. É o equivalente 2D do que Kamei descreve no UE4. O personagem não recebe esse grading.
- **[INFERRED]** A ordem de contraste de Fornace serve como checklist: lutador > chão > fundo próximo > fundo distante. Se algum plano de fundo tem mais contraste ou mais saturação que o lutador, ele está errado.
- **[INFERRED]** Contorno ou realce de borda só no personagem (como Awesomenauts e o edge enhancement de SFV) é a ferramenta clássica para separar um sprite "fotográfico" vindo de vídeo de um fundo pintado.
- **[INFERRED]** Evitar preto puro no cenário. Se usar preto, que seja no lutador ou em elementos de jogo (Fornace e Kamei convergem nisso).

### Gaps
- Não achei declaração primária da Arc System Works sobre DBFZ ou GGXrd tratando especificamente de escurecer ou desfocar o cenário. O handout GGXrd GDC 2015 ([PDF](https://www.ggxrd.com/Motomura_Junya_GuiltyGearXrd.pdf), [GDC Vault](https://gdcvault.com/play/1022031/GuiltyGearXrd-s-Art-Style-The)) não abriu (503).
- Há relatos amplamente repetidos, **não verificados nesta sessão**, de que GGXrd controla a luz de cada personagem separadamente da luz do cenário. Isso precisa ser confirmado no PDF ou no vídeo da GDC antes de entrar no relatório como DOCUMENTED.
- Não achei declarações de SNK (KOF XV, City of the Wolves), Lab Zero/Mike Z/Alex Ahad (Skullgirls) nem de Marvel Tōkon sobre contraste cenário-personagem. As buscas sobre Skullgirls só trouxeram elogios genéricos à arte de fundo.
- Não há números documentados (por exemplo, "o cenário a X% de saturação do personagem"). Qualquer número precisaria ser medido em capturas de tela, o que não fiz.

---

## 2. Silhueta e legibilidade dos golpes

### Takeaway
Para a Capcom, a arte de jogo de luta existe para "transmitir o estado do jogo instantaneamente", e a legibilidade é decidida pela silhueta (poses, movimento, golpes) **vista da câmera de batalha**. O realismo foi rejeitado em SFV porque os golpes ficavam difíceis de ler. A pose exagerada vale mais que a anatomia correta. Sakurai acrescenta que efeitos chamativos demais apagam o personagem, e que somar brilho aditivo sem critério não basta.

### Cited Findings
- **[DOCUMENTED]** Kamei: "A arte em jogos de luta é um elemento muito importante para devolver ao jogador, instantânea e rapidamente, o estado do jogo." Os dois eixos da palestra foram legibilidade (わかりやすさ) e individualidade (個性). — [Famitsu](https://www.famitsu.com/news/201703/03128250.html)
- **[DOCUMENTED]** O conceito-chave de SFV foi luta "fácil de ver e fácil de entender". "Só a arte pode dar feedback instantâneo." A luta muda a cada 1/60 s, então a representação realista é lenta demais e é preciso exagerar. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** Um protótipo fotorrealista de SFV foi rejeitado porque o jogo ficou "difícil de jogar" e "os golpes eram difíceis de ler". — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** A legibilidade é decidida pela silhueta (poses, movimento, golpes), julgada pelo que se vê **da câmera de batalha** e não por clareza objetiva. A referência é a era dos sprites: o soco reto do Yun carrega "uma quantidade tremenda de informação" em cerca de 10 pixels, e o Double Lariat do Zangief comunica estado e golpe em dezenas de pixels. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** "Ângulo mágico": todo personagem de SFV fica virado **5 graus** para a câmera, o que deixa as poses legíveis. Visto de frente no debug, o corpo parece torcido, e isso é considerado correto para jogo de luta. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html); [Famitsu](https://www.famitsu.com/news/201703/03128250.html)
- **[DOCUMENTED]** O antebraço e o braço trocam de espessura conforme a direção de visão: irreal, mas "correto para a arte de SF". Existe um gráfico interno de "anatomia suspeita", da era Alpha/Vampire e supervisionado por Akiman, que diz quais músculos manter ou simplificar. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** Reações a golpe são exageradas de propósito (Kamei compara a Tom & Jerry) para comunicar dor rapidamente. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** Cada golpe precisa de três elementos: ser arte marcial, ter peso e ritmo, e ter desempenho técnico (hitbox). Faltando um, o golpe quebra. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** Um personagem novo não pode repetir a silhueta de um existente, porque isso "mataria" o antigo. Quando o modelo não tem silhueta própria (Fang, Necalli), o movimento supre a diferença. — [Famitsu](https://www.famitsu.com/news/201703/03128250.html)
- **[DOCUMENTED]** O cabelo do V-Trigger do Necalli tapava o corpo, então foi feito como efeito separado e chapado, sinalizando que não tem relação com a hitbox. Ou seja: o que não é hitbox não deve parecer corpo. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED, indie]** Fornace: esconder o personagem atingido dentro do efeito de impacto, mas deixar o atacante visível ("obscure the player getting hit inside hit effects but reveal the attacking player"). — [Fornace, 10 Tips (trecho de busca)](https://fornace.medium.com/dan-fornaces-10-tips-for-making-a-fighting-game-e2c982da2396)
- **[DOCUMENTED]** Sakurai: efeitos chamativos demais acabam ofuscando os personagens. É preciso achar formas de manter o impacto sem atrapalhar os outros elementos e garantir que o personagem jogável "não se perca na ação". — [GoNintendo](https://gonintendo.com/contents/12076-sakurai-discusses-visual-effects-and-character-prominence-in-his-latest-video)
- **[DOCUMENTED]** Sakurai sobre additive blending: blending aditivo sozinho não basta. É preciso cuidar do contraste e do estilo do efeito. — [GoNintendo](https://gonintendo.com/contents/9838-sakurai-s-latest-video-explores-how-he-creates-stand-out-visual-effects)
- **[DOCUMENTED, via listagem]** O episódio de Sakurai sobre efeitos (título japonês na linha de "não deixe turvo") discute se o excesso de soma de cor e glare deixa a tela turva. O resumo diz que, sem um objetivo específico, é melhor não empilhar. Outro episódio, "Making Effects the Right Size", diz que efeitos "lack physical presence and are regulated by time, so their density is extremely important". — [TheTVDB, listagem de episódios](https://www.thetvdb.com/series/masahiro-sakurai-on-creating-games/episodes/9363034); [GoNintendo VFX](https://www.gonintendo.com/contents/16889-masahiro-sakurai-examines-visual-effects-in-a-new-dev-video). Ressalva: veio de resumo de listagem, sem transcrição. Vale assistir aos vídeos no canal oficial antes de citar literalmente.

### Inferences
- **[INFERRED]** Para sprites convertidos de vídeo de IA, o risco principal é a pose de startup e de active não ter silhueta clara, porque a câmera do vídeo e a pose "natural" não foram pensadas para leitura lateral. A regra de Kamei (julgar da câmera de batalha, exagerar, virar o corpo de leve para a câmera) é o critério de aprovação de cada quadro-chave.
- **[INFERRED]** Efeitos de vídeo com chroma (green screen) somados por blending aditivo tendem a "lavar" o personagem (o alerta de Sakurai sobre empilhar add/glare). Regras práticas: o efeito de impacto cobre quem apanha e não quem bate (Fornace); limitar quantos efeitos aditivos ficam simultâneos; dar ao efeito núcleo claro e borda com cor e contraste, em vez de tudo brilhante.
- **[INFERRED]** "Efeitos parecem aleatórios" é sintoma de falta de linguagem: cada tipo de evento (acerto leve/médio/forte, defesa, contra-golpe, super) deveria ter efeito de cor, tamanho e duração fixos e consistentes, como a regra de Kamei de não repetir silhueta, aplicada aos efeitos.

### Gaps
- Não achei texto primário de Mike Z/Lab Zero sobre legibilidade de golpes (só elogios genéricos à arte de Skullgirls).
- Não verifiquei na fonte original nenhuma palestra da Arc System Works sobre DBFZ (efeitos, câmera). A busca achou só menção a uma entrevista da Famitsu sobre câmera ([ShonenGameZ](https://www.shonengamez.com/?p=12822)), sem conteúdo.

---

## 3. Escala dos personagens e câmera

### Takeaway
Não achei números documentados de "fração da altura da tela" para nenhum dos jogos de referência. O que está documentado é qualitativo: a altura do personagem é decidida pelo efeito em batalha, relativa ao Ryu (175 cm); os personagens ficam virados 5° para a câmera; a projeção é corrigida nas bordas. Sobre zoom, o que existe são devlogs indie: a câmera aproxima para destacar o atacante, ajusta altura e zoom para nunca cortar os pés e, no canto, tem que apontar para a ação e não para o centro.

### Cited Findings
- **[DOCUMENTED]** Kamei: as alturas não são fixadas pelo lore. São escolhidas pelo efeito em batalha, relativas ao Ryu (175 cm). Vega precisa parecer maior porque "olha o Ryu de cima". Na era 2D ele tinha 182 cm, mas em pé pareceria ter mais de 2 m. Em CG começou do tamanho do Ryu e foi ajustado. Nas cinemáticas as mãos são reduzidas para manter a proporção cabeça-corpo. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html); [Famitsu](https://www.famitsu.com/news/201703/03128250.html)
- **[DOCUMENTED]** Como SFV é jogo 2D com gráfico 3D, os personagens distorcem nas bordas. O "Fix Projection" corrige 50/50 nos personagens (para manter a precisão da colisão) e com mais força nos cenários. — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** DBFZ usa personagens 3D para poder mudar o ângulo da câmera e reproduzir cenas famosas do anime (as câmeras cinematográficas ficam reservadas para momentos especiais). — [Game Informer, Hiroki](https://www.gameinformer.com/b/features/archive/2017/06/16/bandai-namco-on-why-a-2d-fighting-game-was-the-right-choice-for-dragon-ball.aspx)
- **[DOCUMENTED, indie]** Beatdown City (devlog "Framing the Fight"):
  - zoom ligado a ataques para "highlight the attacker" e mostrar golpes grandes, com screen shake nos zooms maiores;
  - foco da câmera separado dos jogadores;
  - o primeiro zoom ia sempre ao centro e, no canto, "obscuring the action". Foi corrigido dando direção ao zoom;
  - "adjusting height and zoom amount accordingly to avoid cutting off of feet";
  - deixar espaço para indicadores de status e adiar os rótulos para não poluir.
  — [Beatdown City devlog](https://beatdowncity.com/2018/12/08/december-2018-dev-log-the-evolution-of-beatdown-citys-camera/)
- **[INFERRED, fonte fraca]** Um tutorial hobbyista calcula o zoom inversamente proporcional à distância horizontal entre os lutadores (zoom = 300/|x1−x2|), com teto. É só um exemplo de implementação, não um padrão da indústria. — [GDevelop forum](https://forum.gdevelop-app.com/t/how-to-center-a-camera-between-two-player-sprites/19732)
- **[INFERRED, fonte fraca]** Altura de sprite: um post do Steam diz que o Ryu de SF Alpha 3 tem 61×100 px numa pose. Um guia de pixel art dá cerca de 100 px como "padrão", com Sakura 76×87 e Zangief 116×111. Nenhum dos dois é fonte oficial, e não confirmei a resolução de tela. — [Steam discussion](https://steamcommunity.com/app/431730/discussions/0/4411921001873602095); [WPI pixel artist chapter](https://web.cs.wpi.edu/~claypool/courses/frontiers-06/samples/pixel-artist/chapter9.htm)
- **[DOCUMENTED, tangencial]** No modo "Dynamic" (por cima do ombro) de SSF4 3D Edition, avaliar a distância fica mais difícil, segundo crítica da época. Isso apoia a ideia de que, durante a luta neutra, a câmera deve ficar lateral e estável. — [Fighters Generation](https://fightersgeneration.com/games/super-streetfighter4-3d.html)

### Inferences
- **[INFERRED, não medido por mim]** A impressão de "personagem pequeno num cenário cheio" vem em geral de duas causas: (a) o enquadramento padrão está aberto demais, mostrando muito céu e muito chão, e (b) o cenário compete em contraste. Antes de mexer no zoom, vale aplicar a seção 1. Depois, medir em capturas da própria referência (DBFZ, SF6) a razão "altura do lutador em pé / altura da tela" com os dois lutadores a meia distância, e usar isso como alvo. **Não tenho esse número medido; não inventei.**
- **[INFERRED]** Regras de câmera que se depreendem das fontes e da prática dos jogos de referência: câmera lateral e estável no neutro (o crítico de SSF4 3D); nunca cortar os pés e manter a linha do chão perto do terço inferior (Beatdown City); no canto, a câmera encosta na parede e não centraliza; zoom e câmeras cinematográficas reservados a eventos (super, finalização) e não ao neutro (DBFZ, Beatdown City).

### Gaps
- **Não achei números documentados** de altura de personagem em fração da tela, nem de FOV ou zoom, para SF2, SF3, SF4/5/6, DBFZ, MvC3 ou KOF. É preciso medir em capturas 1080p de partidas oficiais. Eu não fiz essa medição (não processei imagens nesta sessão).
- A resolução do CPS-1 (384×224) e a altura dos sprites de SF2 ficaram sem confirmação em fonte confiável.
- Não achei documentação do comportamento da câmera de DBFZ, SF6 ou MvC3 no pulo e no super pulo (quanto sobe, quanto afasta).
- O FOV e os ajustes de câmera de GGXrd devem estar no handout de Motomura ([PDF](https://www.ggxrd.com/Motomura_Junya_GuiltyGearXrd.pdf)), que não abriu.

---

## 4. HUD

### Takeaway
Nenhuma fonte primária de design de HUD de jogo de luta foi encontrada. O que existe é documentação de wikis comunitárias. O padrão observado: barras de vida no topo, saindo do centro (timer) para as bordas; medidores de super e de drive nas laterais de baixo ou de cima; contador de combo na lateral do lado de quem bate; call-outs breves (contador, punição, reversal) num slot fixo de notificação. Em SF6 a Capcom só vende HUDs alternativos "de épocas anteriores" sem mudar o layout, o que indica que o layout é tratado como padrão fixo.

### Cited Findings
- **[DOCUMENTED, oficial]** SF6 ganhou em dezembro de 2025 a opção "Battle HUD Design": um tema gratuito mais 5 temas no estilo de jogos anteriores, vendidos na loja. O tema muda só o visual do próprio lado, e **a disposição das barras praticamente não muda** em relação ao HUD base. — [Famitsu](https://www.famitsu.com/article/202512/60324); [Denfaminicogamer](https://news.denfaminicogamer.jp/news/251211m); [EventHubs](https://eventhubs.com/news/2025/dec/10/street-fighter-hud-feature)
- **[INFERRED, crítica]** A Dashfight avaliou que o HUD padrão de SF6 "funciona, mas não é bonito". — [Dashfight](https://dashfight.com/news/street-fighter-6-new-battle-ui-update-leaves-a-lot-to-be-desired-8231)
- **[INFERRED, wiki comunitária]** Guilty Gear Strive: o contador de combo fica junto do dano total do combo, e o texto cresce conforme o contador sobe. — [Dustloop GGST/HUD (via busca; fetch 403)](https://www.dustloop.com/w/GGST/HUD)
- **[INFERRED, wiki comunitária]** Vampire Savior: o contador de combo usa o mesmo slot dos avisos de guard cancel, reversal e tech de agarrão, ou seja, um slot único de notificação. — [Mizuumi Vampire Savior/HUD](https://wiki.gbl.gg/w/Vampire_Savior/HUD)
- **[INFERRED, wiki comunitária]** Soulcalibur VI: uma marca na barra de vida, por volta de 30%, faz a barra ficar amarela e indica o limiar de mecânica (Guts ou Critical Edge que mata). Rainbowcore Hypernova deixa um rastro atrás da barra para mostrar o dano total do combo. — [SRK wiki SCVI/HUD](https://srk.shib.live/w/Soulcalibur_VI/HUD); [Mizuumi Rainbowcore/HUD](https://wiki.gbl.gg/w/Rainbowcore_Hypernova/HUD)
- **[INFERRED, wiki comunitária]** Them's Fightin' Herds: uma barra de decaimento de juggle enche durante o combo. — [Mizuumi TFH/HUD](https://wiki.gbl.gg/w/Them%27s_Fightin%27_Herds/HUD)
- **[DOCUMENTED, indie]** Beatdown City mudou o momento em que os rótulos de status aparecem para "avoid too much UI clutter". — [Beatdown City devlog](https://beatdowncity.com/2018/12/08/december-2018-dev-log-the-evolution-of-beatdown-citys-camera/)
- **[INFERRED, anedótico]** Comentários de jogadores em jogos indie no itch.io reclamam de fonte difícil de ler, excesso de números na tela e falta de pista visual ou sonora de que o golpe conectou. — [itch.io post](https://itch.io/post/5919680) e outros posts da mesma busca. Fonte fraca, apenas ilustrativa.

### Inferences
- **[INFERRED]** O "texto de debug" no HUD é o oposto do que os jogos de referência fazem. Neles, o texto só aparece como call-out breve (um evento, uma palavra, fonte estilizada, posição fixa ao lado de quem causou) e desaparece. Números contínuos (frame data, estados, timers internos) ficam só no modo treino.
- **[INFERRED]** Checklist do HUD "raiz" (SF2/DBFZ): vida no topo, com timer no centro; barra de super embaixo, no canto de cada lado; retratos ou nomes junto da vida; contador de combo na lateral do atacante; um único slot de call-out por lado. Nada no terço central da tela, onde os lutadores ficam.

### Gaps
- Não achei palestra ou entrevista de UI designer de Capcom, ArcSys ou SNK sobre o HUD de batalha (busquei em japonês e inglês).
- Não achei medidas documentadas (altura da barra em % da tela, área ocupada pelo HUD). Se precisar, é preciso medir em capturas.
- Não confirmei por fonte, nesta sessão, quais call-outs o SF6 usa (Punish Counter, Drive Rush etc.). Por memória geral, o SF6 mostra textos como "Punish Counter" e "Counter" ao lado do personagem, mas isso deve ser conferido antes de ir para o relatório.

---

## 5. Erros típicos de amador (visual e apresentação)

### Takeaway
As fontes convergem em seis erros:
1. Cenário com tanto ou mais contraste e saturação que o lutador.
2. Preto puro e detalhe espalhados de forma uniforme.
3. Realismo ou "pose natural" no lugar da pose exagerada legível da câmera de batalha.
4. Efeitos demais e aditivos empilhados, que "lavam" a tela e escondem o atacante.
5. Câmera que corta os pés ou centraliza errado no canto.
6. UI com texto e números demais.

### Cited Findings
- **[DOCUMENTED]** A Capcom rejeitou o visual fotorrealista em SFV porque os golpes ficavam ilegíveis (erro 3). — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html)
- **[DOCUMENTED]** Cenário com saturação alta e luz que se mistura ao personagem reduz a visibilidade. A Capcom escurece e dessatura o cenário de propósito (erro 1). — [GAME Watch](https://game.watch.impress.co.jp/docs/news/1047774.html); [Famitsu](https://www.famitsu.com/news/201703/03128250.html)
- **[DOCUMENTED]** Sombra em preto puro fica não pictórica (Kamei). Preto sólido não deve aparecer em elementos que não são de jogo (Fornace) (erro 2). — [Famitsu](https://www.famitsu.com/news/201703/03128250.html); [Fornace (trecho)](https://fornace.medium.com/fighting-game-design-with-dan-fornace-the-power-of-silhouettes-915fde48318f)
- **[DOCUMENTED]** Efeitos chamativos demais ofuscam o personagem. Empilhar add e glare sem objetivo turva a tela (erro 4). — [GoNintendo](https://gonintendo.com/contents/12076-sakurai-discusses-visual-effects-and-character-prominence-in-his-latest-video); [TheTVDB](https://www.thetvdb.com/series/masahiro-sakurai-on-creating-games/episodes/9363034)
- **[DOCUMENTED]** Detalhe demais em tudo: Sakurai pede equilíbrio e foco no que o jogador mais vê (erro 2). — [GoNintendo](https://www.gonintendo.com/contents/13084-sakurai-s-latest-video-details-distinguishing-between-major-and-minor-graphical)
- **[DOCUMENTED, indie]** Câmera que corta os pés e zoom que centraliza no canto e esconde a ação (erro 5). — [Beatdown City](https://beatdowncity.com/2018/12/08/december-2018-dev-log-the-evolution-of-beatdown-citys-camera/)
- **[DOCUMENTED, indie]** Silhuetas repetidas ou paletas parecidas entre personagens prejudicam a leitura. — [Famitsu (Kamei)](https://www.famitsu.com/news/201703/03128250.html); [Moldy Skull](https://moldy-skull-games.itch.io/zeitgeist/devlog/577786/3-fighting-game-readability-and-you)
- **[INFERRED, anedótico]** Fonte ilegível, números demais na tela e falta de feedback de acerto aparecem como queixas recorrentes em jogos indie (erro 6). — [itch.io](https://itch.io/post/5919680)
- **[INFERRED, opinião antiga]** Um blog da Game Developer (2010) argumentou que falta feedback visual nos jogos de luta e que a linguagem visual depende de convenções que o jogador já conhece. — [Game Developer blog](https://gamedeveloper.com/design/fighting-the-good-fight-why-fighting-games-need-their-arses-kicked-part-3-)

### Inferences
- **[INFERRED]** Aplicado ao caso do cliente, em ordem de impacto e custo:
  1. Grading separado para o cenário: escurecer, dessaturar e desfocar levemente; o personagem fica intacto.
  2. Contorno ou realce de borda leve no sprite do lutador.
  3. Enquadramento mais fechado no neutro, sem cortar os pés.
  4. Biblioteca de efeitos com regra fixa por tipo de evento, limitando a soma aditiva simultânea, cobrindo quem apanha e não quem bate.
  5. HUD sem texto de debug: só vida, super, timer, contador de combo e call-outs breves.

### Gaps
- Não achei postmortems de Pocket Bravery, Them's Fightin' Herds ou Skullgirls que tratem explicitamente de contraste e câmera. A busca genérica trouxe só wikis e devlogs pequenos.
- Não achei artigos de Seth Killian ou análises de Shoryuken/Dustloop focadas em "visual amador".
