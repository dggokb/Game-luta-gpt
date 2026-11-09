# Estilo Arc System Works (GGXrd, DBFZ, Strive) e princípios de animação de luta 2D aplicados a sprites

Legenda: **[DOCUMENTADO]** = dito pelos próprios desenvolvedores (palestra, entrevista ou material oficial) ou por artigo técnico baseado neles. **[INFERIDO]** = conclusão minha a partir das fontes, sem que o desenvolvedor tenha dito. Quando a fonte é um resumo de terceiros ou um trecho de busca, isso está escrito.

Fontes principais usadas:
- 4Gamer, Zenji Nishikawa, "試験に出るゲームグラフィックス (1) GUILTY GEAR Xrd -SIGN-", parte 1 (render, contorno, linhas internas) e parte 2 (animação, deformação, câmera). O artigo traz entrevistas com Sakamura (diretor), Ishiwata e Iede (programador líder) da ArcSys. Parte 1: https://www.4gamer.net/games/216/G021678/20140703095/ (páginas 1 e 2); parte 2: https://www.4gamer.net/games/216/G021678/20140714079/
- ArcSys (ASW Academy), "Guilty Gear Toon Line Control Techniques [ENG ver.]", 79 slides: https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng
- Palestra GDC 2015 de Junya C. Motomura (GDC Vault https://gdcvault.com/play/1022031/ ; handout em PDF https://www.ggxrd.com/Motomura_Junya_GuiltyGearXrd.pdf). **O PDF não abriu (503 e conexão recusada, inclusive pelo Wayback).** Usei a cobertura da Game Watch (https://game.watch.impress.co.jp/docs/news/20150307_691725.html) e um resumo com marcação de tempo gerado por IA (https://videohighlight.com/v/yhGjCzxJV3E), que deve ser lido como fonte secundária fraca.

---

## 1. GGXrd (GDC 2015 / 4Gamer): animação limitada, poses-chave, deformação, contorno, linhas internas, sombreamento, luz e câmera

### Takeaway
A ArcSys não deixa o motor fazer a interpolação entre as poses. Cada quadro é uma pose feita à mão e fica parada por um número de quadros escolhido golpe a golpe: 1 a 4 quadros de jogo a 60 fps, cerca de 15 desenhos por segundo nas cinemáticas. Cada pose é deformada para a câmera, como num desenho. O contorno, as linhas internas, as sombras e a luz são controlados pelo artista, não calculados, para "matar tudo o que parece 3D". O que leva a sensação de desenho para o sprite é uma regra: poucas poses fortes, espera variável e contato na hora certa. O contorno fino sozinho não faz isso.

### Cited Findings

**Animação limitada / コマ打ち (quadros parados)**
- [DOCUMENTADO] O jogo roda a 60 fps, mas a animação de luta é uma "リミテッドアニメーションシステム" (sistema de animação limitada). Cada pose fica na tela por um número de quadros definido no script de cada golpe. Exemplo dado no artigo: "2F, 3F, 3F, 1F, 1F, 2F, 2F, 3F, 4F" (1F ≈ 16,67 ms, então 2F ≈ 33 ms) — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] Nas cinemáticas a base é "毎秒15コマ" (15 desenhos por segundo, ou seja, 4F por desenho a 60 fps), um pouco acima dos 8 a 12 desenhos por segundo da animação de TV — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] Os comandos são lidos a cada 1/60 s, e o arco do pulo e os projéteis andam a 60 fps. Só a pose do personagem é "em degraus" — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] **Por que não interpolar:** a equipe primeiro animou com curvas (F-curve) a 60 fps completos e depois cortou quadros. O diretor Sakamura disse que o resultado parecia "コマ落ちした3Dグラフィックス" (gráfico 3D com quadros perdidos). A solução final: o animador monta cada quadro à mão, "como animação de massinha", seguindo storyboards que já trazem a contagem de quadros (exemplo: um especial de 60 quadros com o acerto no quadro 30) — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] A animação é feita "só com quadros-chave" ("キーフレームのみで表現しているのだ"), emprestando o método de sprites e células do 2D, em vez de interpolar como num modelo 3D comum. A limitação é proposital e gera o movimento característico — [Game Watch, GDC 2015](https://game.watch.impress.co.jp/docs/news/20150307_691725.html)
- [DOCUMENTADO, via resumo por IA, fraco] Na palestra, por volta de 29:55, Motomura mostra que a animação completa (full animation) foi testada e "parecia mais 3D do que 2D". Por volta de 33:39, compara completa e limitada: a completa é mais suave, a limitada tem poses mais claras, e a limitada foi a escolhida. Cada personagem tem cerca de 500 ossos (30:42) — [resumo videohighlight](https://videohighlight.com/v/yhGjCzxJV3E). O 4Gamer fala em cerca de 460 ossos no Sol e até cerca de 600 no maior, dos quais cerca de 170 na cabeça e cerca de 70 para expressões — [4Gamer parte 1](https://www.4gamer.net/games/216/G021678/20140703095/)
- [DOCUMENTADO] Ideia citada sobre a palestra: a animação limitada é "mostrar exatamente as coisas certas, e não mostrar mais" — [BlenderNation, entrevista com Motomura (via trecho de busca)](https://www.blendernation.com/2015/07/26/junya-c-motomura-behind-the-scenes-of-guilty-gear-xrd/)
- [DOCUMENTADO] Um texto de outra língua sobre DBFZ/Xrd resume: "Mehr Frames bedeutet, dass man ein Modell eher als 3D wahrnehmen kann" (mais quadros fazem o modelo parecer 3D), por isso a ArcSys usa animação limitada — [GameStar (trecho de busca; a página deu 403)](https://www.gamestar.de/artikel/dragon-ball-fighterz,3325366.html)

**Deformação por quadro e câmera "roubada"**
- [DOCUMENTADO] Prioridade declarada por Sakamura: "プレイヤーが見ることになるゲーム画面上で格好いいこと" (ficar bonito na tela que o jogador vê). Os olhos andam livres no rosto, a boca muda o volume do rosto, e braços e pernas esticam e mudam de escala — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] O UE3 só escalava ossos por igual nos três eixos. O programador Iede mudou o motor para escalar X, Y e Z separados, porque fazer isso com ossos extras quadruplicaria a contagem de ossos. Os animadores ajustam a escala dos membros com valores numéricos numa janela própria no Softimage — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] Perspectiva "no modelo": para um soco na direção da câmera, abrir o FOV encolhia demais o rosto. A equipe fixou o FOV e empurrou a malha do braço para a câmera, colocando a perspectiva dentro do modelo. Em cortes como o K.O. do Bedman, olhos, nariz e boca são reposicionados para ficarem certos daquele ângulo e não de todos — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO, via resumo por IA] A deformação da malha em cada quadro-chave cria imperfeições de propósito, para o modelo não parecer 3D rígido, e quebrar a transição perfeita entre quadros deixa cada pose marcante (31:40–32:15) — [resumo videohighlight](https://videohighlight.com/v/yhGjCzxJV3E)
- [DOCUMENTADO] Símbolos de mangá (veia de raiva, olho redondo) e cabelo virando lâmina são feitos trocando peças, acrescentando peças ou trocando textura — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)

**Golpes, caixas de acerto e sobreposição**
- [DOCUMENTADO] Cada quadro de animação é tratado como um sprite 2D, com caixas de acerto retangulares por quadro, como nos jogos 2D antigos. Não há volumes 3D — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] Quando os dois personagens se sobrepõem, a profundidade do atacante é empurrada cerca de 1 m para a frente na renderização, para que quem ataca fique sempre na frente — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] Fumaça e explosões são malhas 3D modeladas à mão quadro a quadro, sem luz e sem contorno, com a sombra pintada na textura. Brilhos e fogo não iluminam o entorno — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/). Efeitos de acerto são sequências de imagens 2D trocadas a cada quadro (47:28) — [resumo videohighlight](https://videohighlight.com/v/yhGjCzxJV3E)

**Contorno (inverted hull, "背面法")**
- [DOCUMENTADO] Contorno feito com uma cópia do modelo inflada pelas normais e desenhada só pelo verso. Vem do Jet Set Radio (2000) e é usado em todos os jogos de luta 3D da ArcSys desde o Xrd. A vantagem citada é poder definir onde a linha aparece e onde não aparece, e mudar espessura e cor por área ou por vértice — [ASW Academy, Toon Line](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng)
- [DOCUMENTADO] Canais de cor de vértice no Xrd: **R** = deslocamento do limiar de sombra (1 = padrão, 0 = sempre em sombra); **G** = quanto o contorno engrossa com a distância da câmera; **B** = deslocamento em Z do contorno, usado para esconder linhas indesejadas no cabelo e sob o nariz; **A** = espessura ("0.5が標準で，1にするとMAXの太さ，0にすると輪郭線なし": 0,5 é o padrão, 1 é o máximo, 0 tira a linha) — [4Gamer parte 1, p.2](https://www.4gamer.net/games/216/G021678/20140703095/index_2.html)
- [DOCUMENTADO] Espessura pelo alfa do vértice: 0,0 = sem linha, 0,5 = padrão, 1,0 = dobro da espessura padrão. Linha fina em cabelo e detalhes pequenos, grossa em músculos e volumes grandes. Nos "vales" (dobras, entre músculos), a linha é fechada com valor baixo (exemplo 0,1 contra 0,5) para não "flutuar" — [ASW Academy](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng)
- [DOCUMENTADO] Espessura e câmera: o deslocamento é proporcional à distância da câmera e também é corrigido pelo FOV, com largura B = A × tan(FOV/2). Números de referência para o contorno do rosto: **1 a 3 mm em close, 7 a 10 mm em plano aberto** (no espaço do modelo) — [ASW Academy](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng)
- [DOCUMENTADO] Linhas desenhadas à mão ficam serrilhadas quando ampliadas ou reduzidas. Por isso a borda das partes do modelo recebe um preto proposital ("各部位のポリゴン部分の“縁”に、あえて黒い部分を設定する"), para a linha ficar limpa em qualquer escala — [Game Watch](https://game.watch.impress.co.jp/docs/news/20150307_691725.html)
- [DOCUMENTADO] Falhas que a ArcSys lista: linha "flutuando" em vales, quebra em arestas duras ou com normais editadas, cruzamento de malhas que não gera linha, linha indesejada em certas expressões, ângulos ou distâncias, e custo alto com muitos objetos contornados — [ASW Academy](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng)

**Linhas internas ("本村式ライン", linhas Motomura)**
- [DOCUMENTADO] O contorno invertido não desenha linhas dentro da silhueta (costuras, sulcos, músculos), e linha pintada numa textura comum serrilha em close. A solução: a textura só tem linhas retas horizontais e verticais, e as UVs são torcidas para elas caírem em curvas e diagonais no modelo. O filtro bilinear faz o papel de antisserrilhado, e a espessura sai do layout de UV (mais texels = linha mais grossa). As linhas ficam no alfa da textura "ilm", numa única cor — [4Gamer parte 1, p.2](https://www.4gamer.net/games/216/G021678/20140703095/index_2.html)

**Sombreamento em 2 tons e luz por personagem**
- [DOCUMENTADO] O toon é de 2 tons: acima do limiar é luz ("明"), abaixo é sombra ("陰"). O exemplo de 128 em 0 a 255 do artigo é só ilustrativo. A cor da sombra vem de uma textura própria por material, a "SSS texture", que segundo a própria equipe não simula espalhamento na pele: pele puxa para o vermelho, e roupa mantém a saturação. A ilm R controla a intensidade do brilho e a ilm B a facilidade de o brilho aparecer — [4Gamer parte 1](https://www.4gamer.net/games/216/G021678/20140703095/) e [p.2](https://www.4gamer.net/games/216/G021678/20140703095/index_2.html)
- [DOCUMENTADO] Normais do rosto e da cabeça editadas à mão para a sombra cair "chapada", como num modelo de poucos polígonos. Não há normal map — [4Gamer parte 1](https://www.4gamer.net/games/216/G021678/20140703095/); [Game Watch](https://game.watch.impress.co.jp/docs/news/20150307_691725.html)
- [DOCUMENTADO] **Cada personagem tem seu próprio vetor de luz, definido por movimento**, e nas cinemáticas a luz é ajustada quadro a quadro. A única luz direcional da cena serve só para a sombra no chão — [4Gamer parte 1](https://www.4gamer.net/games/216/G021678/20140703095/). O rosto tem luz própria para ficar claro e legível ("顔は明るく、はっきり映るように独自の光源を持たせる") — [Game Watch](https://game.watch.impress.co.jp/docs/news/20150307_691725.html)
- [DOCUMENTADO, via resumo por IA] A luz do personagem fica igual em todos os cenários, para a sombra não confundir o jogador (52:27). Não há auto-sombra, porque não dava para controlar com cabelo e casaco grandes; a sombra é pintada no vértice (56:53) — [resumo videohighlight](https://videohighlight.com/v/yhGjCzxJV3E)
- [DOCUMENTADO] Ao virar de lado, o X dos vértices é invertido e a luz do personagem também. Textos na roupa (o "FREE" do cinto do Sol) trocam de UV para continuar legíveis — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)

**Câmera e projeção**
- [DOCUMENTADO] Projeção híbrida de **cerca de 30% perspectiva e 70% ortográfica**, para o personagem não engordar na borda da tela nem afinar no centro. O Street Fighter IV faz algo parecido — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] O bloom pesado e a difusão aparecem principalmente nas cinemáticas, "para a luta ficar mais fácil de ler" — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO] Números do Xrd: cerca de 40 mil polígonos por personagem, 720p a 60 fps no arcade, cerca de 2 meses de modelagem mais 2 de animação por personagem — [4Gamer parte 1](https://www.4gamer.net/games/216/G021678/20140703095/); [resumo videohighlight](https://videohighlight.com/v/yhGjCzxJV3E)

### Inferences
- [INFERIDO] **Para sprites vindos de vídeo:** o equivalente direto do Xrd é ler o vídeo, escolher as poses que contam a ação e montar cada golpe como uma lista de (pose, quadros parados) a 60 fps, no formato do script da ArcSys ("2F, 3F, 3F, 1F, 1F, 2F, 2F, 3F, 4F"). A ArcSys mostra que a espera não é fixa: há 1F nos trechos rápidos (perto do golpe) e 3 a 4F nas poses que o olho precisa ler. Segurar quadros de forma uniforme ou ao acaso imita o "3D com quadros perdidos" que a ArcSys rejeitou, o que bate com o resultado "aleatório" que o cliente descreve.
- [INFERIDO] Cortar quadros de um vídeo suave dá exatamente a "コマ落ち" que a ArcSys rejeitou, porque as poses intermediárias do vídeo não foram feitas para serem vistas paradas. Ao escolher do vídeo, prefira os extremos (o máximo da preparação, o contato, o máximo do follow-through), não quadros a intervalos regulares.
- [INFERIDO] A deformação por quadro do Xrd (braço maior na direção da câmera, rosto acertado para o ângulo) equivale no sprite a editar à mão as poses-chave escolhidas: esticar o membro que golpeia, acentuar a silhueta. O vídeo de IA tende a manter proporções "realistas" em todos os quadros.
- [INFERIDO] A luz por personagem fixa e o 2 tons controlado explicam por que um sombreamento "de vídeo" (degradês suaves, luz que muda a cada quadro) tira o ar de anime. Nos sprites, sombras planas e uma luz consistente entre golpes valem mais do que o contorno.

### Gaps
- O handout em PDF da GDC 2015 não abriu (503 e conexão recusada). As citações literais da palestra sobre animação limitada vêm do 4Gamer e de um resumo por IA. Não confirmei os números exatos dos slides (por exemplo, se Motomura deu uma regra de "em 2s/3s").
- Nenhuma fonte deu a largura do contorno em pixels de tela nem em relação à altura do personagem. Só existe o valor em mm no modelo (1 a 3 mm em close, 7 a 10 mm em plano aberto no rosto) e o fator de 0,5 a 1,0.

---

## 2. DBFZ e Strive: o que mudou em relação ao Xrd e como reproduziram o anime

### Takeaway
Para o DBFZ, as fontes primárias que achei falam de **câmera, storyboard e ajuste quadro a quadro para o rosto e a pose ficarem iguais ao original**, não de novas técnicas de render. O método do Xrd continua (Motomura é o mesmo técnico). No Strive, a direção de arte diz ter deixado de imitar o anime desenhado à mão para buscar um "visual de animação Guilty Gear", mas mantendo a animação limitada. Não encontrei palestra técnica sobre o DBFZ (nem CEDEC nem GDC).

### Cited Findings
- [DOCUMENTADO] Motomura (diretor e artista técnico do DBFZ, antes artista técnico e modelador líder do Xrd): "ひとコマひとコマしっかりと調整をして、原作に近い顔や姿になるようにしています" (cada quadro é ajustado com cuidado para que rosto e pose fiquem próximos do original). Também: "原作があるゲームは、キャラクターをしっかりと似せないといけません" (jogo de licença tem de ser bem parecido); o Goku foi o que mais demorou — [Famitsu, entrevista com Hiroki e Motomura](https://www.famitsu.com/news/201708/10139088.html)
- [DOCUMENTADO] Câmera: "必殺技などの演出のときだけカメラが動くスタイルなので、調整がしやすい" (a câmera só se mexe nas encenações de especiais e supers, o que facilita o ajuste). O objetivo é que, "バトル中にプレイヤーが見る範囲ではどのカットでもキャラクターが最高にカッコよくなるように" (o personagem fique o mais bonito possível em qualquer corte que o jogador veja na luta). Um 3D não fica bonito de todos os ângulos, e a câmera limitada resolve isso — [Famitsu](https://www.famitsu.com/news/201708/10139088.html)
- [DOCUMENTADO] Storyboards: a Bandai Namco desenha o storyboard, a ArcSys monta uma versão provisória e só depois da aprovação faz o detalhe. "絵コンテは、原作にあったシーンを意識したものを採用" (os storyboards reproduzem cenas do anime e do mangá). O ponto central do original, segundo eles, é "とにかくスケールが大きいということ" (a escala enorme) — [Famitsu](https://www.famitsu.com/news/201708/10139088.html)
- [DOCUMENTADO] A quebra de cenário (destruição dramática) só toca quando um K.O. acontece numa condição específica, porque em todo acerto "テンポが悪くなってしまいます" (o ritmo da luta piora) — [Famitsu](https://www.famitsu.com/news/201708/10139088.html)
- [DOCUMENTADO] O 2.5D (personagem 3D num plano 2D) facilita "狙った絵" (o quadro planejado) e as expressões de ataque e de dor — [Famitsu](https://www.famitsu.com/news/201708/10139088.html)
- [DOCUMENTADO, secundário] O vídeo do NewFramePlus (Daniel Floyd), baseado na palestra do Motomura, diz que a ArcSys imita a animação limitada do anime e busca fluidez "sem quadros demais" — [Kotaku](https://kotaku.com/breaking-down-the-animation-of-guilty-gear-and-dragonba-1836849279)
- [DOCUMENTADO, trecho de busca; a página da Unreal deu 403] Strive: o diretor de arte Hidehiko Sakamura diz que o estúdio se afastou da imitação do anime desenhado à mão, buscando a reação "isto é a animação de Guilty Gear!". A animação limitada continua, sobretudo nas cinemáticas e nos Overdrives, e as expressões faciais foram feitas para cada momento, e não como um anime simplificado — [Unreal Engine, entrevista do Strive](https://www.unrealengine.com/en-US/developer-interviews/how-guilty-gear--strive--hits-an-ultra-combo-with-groundbreaking-visuals-and-gameplay)
- [DOCUMENTADO] Strive: na entrevista da ANN, Ken Miyauchi elogia a luz de cada cenário (sol entre as folhas, luz de neve que satura um pouco a cor do personagem). Isso sugere que a luz do cenário passou a influenciar a cor do personagem, ao contrário da luz fixa do Xrd, mas sem números — [Anime News Network](https://animenewsnetwork.com/interview/2024-09-30/guilty-gear-artistic-identity-and-upcoming-anime-with-ken-miyauchi-shoichi-kitazono-and-genki-mamada/.214138)

### Inferences
- [INFERIDO] O que faz o DBFZ "parecer o anime" vem de três decisões: (1) poses e rostos de cada quadro acertados com a referência; (2) câmera de luta fixa e lateral, com câmera livre só nos supers e finalizações; (3) cenas grandes reservadas para momentos raros (finalização dramática, quebra de cenário) para não quebrar o ritmo. Nos sprites: manter a câmera de luta parada (sem zoom nem tremor contínuo) e guardar cortes, zooms e quadros de impacto em tela cheia para supers, ultras e K.O.
- [INFERIDO] Usar o DBFZ como referência pede storyboard por golpe: quais quadros do vídeo de IA viram pose-chave, em que quadro está o acerto e quanto tempo cada pose fica parada. Isso imita o processo Bandai/ArcSys de storyboard, versão provisória e detalhe.

### Gaps
- Não encontrei palestra CEDEC ou GDC específica do DBFZ, nem números de quadros parados próprios do DBFZ (se a espera é igual à do Xrd). A busca em japonês ("ドラゴンボール ファイターズ アニメ表現 CEDEC") não achou a sessão.
- A entrevista da Unreal sobre o Strive deu 403. Os detalhes de contorno e luz do Strive vêm só de trechos de busca e da ANN.

---

## 3. Animação clássica 2D de luta: quadros por golpe, preparação, pose ativa, recuperação, smear, espera no impacto

### Takeaway
Um golpe 2D bom é uma sequência curta de poses fortes: preparação (quase nenhuma no golpe fraco), smear ou quadro de velocidade, **pose de contato segurada por mais tempo**, follow-through e volta. A Lab Zero chega a dar um soco inteiro em 6 quadros desenhados. A força vem de onde o tempo fica (no contato e na pose de leitura), não da quantidade de desenhos.

### Cited Findings
- [DOCUMENTADO] Mariel Cartwright (animadora líder da Lab Zero, Skullgirls), GDC 2014 "Animation Bootcamp: Fluid and Powerful Animation within Frame Restrictions": a palestra trata de animação 2D clara e fluida que convive com jogo responsivo. Os princípios centrais são poses-chave fortes, preparação e tempo, com exemplos de Darkstalkers e SF3: 3rd Strike, e o caso de um designer que exige um soco em **"only six frames to deliver a punch"** — [GDC Vault](https://gdcvault.com/play/1020575/Animation-Bootcamp-Fluid-and-Powerful); [Siliconera](https://www.siliconera.com/fluid-powerful-animations-skullgirls-achieved/)
- [DOCUMENTADO] Skullgirls: 11.515 quadros nos 8 personagens jogáveis originais, média de 1.439 por lutador (recorde Guinness) — [Guinness (via trecho de busca)](https://www.guinnessworldrecords.com/world-records/105505-most-frames-of-animation-per-character-in-a-videogame). SF3 3rd Strike: 23.039 quadros de animação de personagem — [Guinness](https://www.guinnessworldrecords.com/world-records/98823-most-frames-of-animation-in-a-2d-fighter-videogame)
- [DOCUMENTADO, artista de pixel art, não de jogo de luta comercial] Estrutura de golpes de Raymond Schlitter (SLYNYRD): **jab = 5 quadros (smear, contato, follow-through, volta, overshoot)**; cross = 7 (carga, puxada, smear, contato, follow-through, volta, overshoot); chute frontal = 4; chute giratório = 7 (carga, impulso, smear, contato, follow-through, volta, overshoot). O jab "should strike quick without any wind up that can easily be telegraphed"; "The hit frame is slightly longer to enhance the sense of impact"; "The main key position of the striking pose is essentially achieved in the smear". O smear só funciona quando segue o movimento. O overshoot repete o primeiro quadro do idle 1 px atrás, dando o efeito de "voltar com estalo". Golpes fortes ganham "delay, screen shake, and a heavier hit effect" — [SLYNYRD Pixelblog 53](https://www.slynyrd.com/blog/2024/11/25/pixelblog-53-punches-and-kicks)
- [DOCUMENTADO, guia de pixel art] Evitar detalhe complexo que vira "jumbled noise when in motion"; começar pelo movimento das formas principais antes do detalhe fino — [SLYNYRD Pixelblog 53](https://www.slynyrd.com/blog/2024/11/25/pixelblog-53-punches-and-kicks)
- [DOCUMENTADO, referência geral] Smear frame é um quadro que mostra velocidade deformando ou multiplicando a forma entre poses-chave. Jogos com câmera fixa e estilo forte tendem a usá-los — [Wikipedia, Smear frame](https://www.wikipedia.org/wiki/Smear_frame). Em pixel art "não existe motion blur da câmera, existem quadros", então o smear faz o papel do borrão — [GamineAI (trecho de busca, fonte fraca)](https://gamineai.com/blog/pixel-art-combat-fx-hitstop-smear-frames-impact-timing-2026)
- [DOCUMENTADO, secundário] Hitstop (hitlag, congelamento no impacto) costuma ser curto, e em vários jogos de luta é proporcional à força do golpe — [SmashWiki, Hitlag](https://www.ssbwiki.com/Hitlag); [Source Gaming, coluna do Sakurai na Famitsu](https://sourcegaming.info/2015/11/11/thoughts-on-hitstop-sakurais-famitsu-column-vol-490-1/). (Um fórum fala em "<6 quadros", mas a fonte é fraca e a citação não foi verificada: https://forums.ultra-combo.com/t/why-isnt-hitstop-duration-standardized/24838)
- [DOCUMENTADO] Até o SF3, famoso pela fluidez, usou rotoscopia pontual (a pose de vitória da Elena foi decalcada de um clipe de 1985 de David Lee Roth). E há uma crítica antiga de que "animações com quadros demais" deixam o ritmo do SF3 mais lento — [Kotaku](https://kotaku.com/street-fighter-animation-dates-back-to-an-80s-music-vid-1828793337); [Wikipedia, Street Fighter III (via busca)](https://en.wikipedia.com/wiki/Street_Fighter_III)

### Inferences
- [INFERIDO] Modelo de golpe para os sprites a 60 fps, juntando ArcSys (holds de 1 a 4F) e Lab Zero/SLYNYRD (5 a 7 poses por golpe normal):
  - **Startup:** 1 a 3 poses (golpe fraco: 0 ou 1 pose de preparação; forte: carga e impulso). Espera de 2 a 3F cada, para ler a intenção. O último quadro antes do ativo pode ser um smear de 1F.
  - **Ativo:** a **pose de contato fica parada durante todos os quadros ativos** (é o desenho com a hitbox) e continua parada durante o hitstop. É a pose mais "limpa" e exagerada (membro esticado, silhueta legível).
  - **Recuperação:** 2 a 3 poses (follow-through, volta, overshoot opcional) com espera de 2 a 4F. A recuperação longa de golpes fortes deve parecer "pesada": pose segurada, não deslizar devagar.
  - A soma das esperas tem de bater com os dados do golpe (startup, ativo, recuperação). A animação serve aos dados, e não o contrário, que é o recado da palestra da Cartwright.
- [INFERIDO] Num vídeo de IA, o quadro mais parecido com "smear" costuma ser o de borrão de movimento do próprio vídeo. Ele serve como smear de 1F antes do contato, mas não deve ficar parado.

### Gaps
- Não achei os números exatos da Cartwright (quantos F cada pose fica parada no Skullgirls), nem entrevistas de animadores de SF3 ou KOF XIII com contagens por golpe. O post de 2011 da Cartwright no skullgirls.com ("Animation: Getting the Details Right", https://skullgirls.com/?p=118) não carregou conteúdo.
- Não achei fonte primária com o hitstop em quadros de um jogo da ArcSys.

---

## 4. Por que o movimento de vídeo (rotoscopia, IA) parece "flutuante" num jogo de luta, e o que os animadores recomendam

### Takeaway
A ArcSys testou exatamente o caso "movimento suave a 60 fps, depois cortar quadros" e rejeitou: parecia 3D com quadros perdidos. Os animadores de luta pedem poses fortes e tempo concentrado no contato. Movimento de vídeo distribui o tempo por igual entre todos os intervalos, então não há pose para ler nem impacto. Não achei nenhuma fonte específica sobre sprites gerados por IA em jogos de luta.

### Cited Findings
- [DOCUMENTADO] Movimento interpolado a 60 fps com corte posterior = "コマ落ちした3Dグラフィックス" (Sakamura), rejeitado em favor de poses feitas quadro a quadro — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)
- [DOCUMENTADO, resumo por IA] Na comparação entre animação completa e limitada na palestra, a completa é "mais suave" e a limitada tem "poses mais claras", e a limitada venceu; a completa "parecia mais 3D do que 2D" — [resumo videohighlight](https://videohighlight.com/v/yhGjCzxJV3E)
- [DOCUMENTADO, secundário] "Mais quadros fazem o modelo parecer 3D" — [GameStar (trecho de busca)](https://www.gamestar.de/artikel/dragon-ball-fighterz,3325366.html)
- [DOCUMENTADO] Animação limitada = "mostrar exatamente as coisas certas, e não mostrar mais" — [BlenderNation](https://www.blendernation.com/2015/07/26/junya-c-motomura-behind-the-scenes-of-guilty-gear-xrd/)
- [DOCUMENTADO] O jab não deve ter preparação telegrafada, e o quadro de contato deve ficar parado mais tempo — [SLYNYRD](https://www.slynyrd.com/blog/2024/11/25/pixelblog-53-punches-and-kicks)

### Inferences
- [INFERIDO] Por que o vídeo de IA parece "flutuante": (1) espaçamento uniforme, com cada quadro andando mais ou menos a mesma distância, sem a aceleração seca que a espera variável dá; (2) não há pose parada no contato, então o golpe "passa" pelo alvo; (3) preparação e recuperação longas e suaves parecem demora, não peso; (4) há pequenas oscilações de forma entre quadros (no vídeo de IA, roupa e rosto mudam um pouco a cada quadro), que viram tremor quando tocadas a 60 fps.
- [INFERIDO] Receita para converter vídeo em sprite de luta: (a) escolher de 4 a 8 poses-chave por golpe normal entre os extremos do vídeo; (b) dar a cada uma uma espera de 1 a 4F conforme o papel (rápido perto do golpe, longo nas poses de leitura); (c) deixar a pose de contato parada nos quadros ativos e no hitstop; (d) usar smear ou quadro de velocidade de 1F antes do contato; (e) no idle e no andar, que precisam de fluidez, usar mais quadros com espera de 2 a 3F (equivalente aos 15 desenhos por segundo da cinemática do Xrd). Isso substitui "segurar alguns quadros" ao acaso por uma regra.

### Gaps
- Não encontrei fonte (palestra, entrevista ou post) de desenvolvedor sobre sprites gerados por IA ou feitos de vídeo em jogos de luta, nem estudo sobre a sensação "flutuante" de rotoscopia. A explicação acima é inferência a partir das fontes da ArcSys e da Lab Zero.

---

## 5. O contorno ajuda os sprites? Quando o contorno escuro é bom ou ruim, espessura e cor

### Takeaway
Na ArcSys, o contorno é **controlado, e não uniforme**: espessura por vértice (0 a 1, com 0,5 de padrão), mais fino em cabelo e detalhes e mais grosso em volumes, retirado onde atrapalha e corrigido pela distância e pelo FOV. Somar ao sprite um contorno preto de largura fixa por shader vai contra isso, principalmente se o sprite já tem linha desenhada. Guias de pixel art preferem contorno escuro na cor do próprio sprite (selout) ao preto puro.

### Cited Findings
- [DOCUMENTADO] Controle de onde a linha aparece, de espessura e cor por área ou vértice é a razão de a ArcSys usar o contorno invertido em vez de pós-processamento. Linhas indesejadas em áreas delicadas (pálpebras, cílios) são retiradas com valor 0 — [ASW Academy](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng)
- [DOCUMENTADO] Fino em cabelo e detalhes pequenos, grosso em músculos e volumes; o valor 1,0 é o dobro do padrão 0,5 — [ASW Academy](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng); [4Gamer parte 1, p.2](https://www.4gamer.net/games/216/G021678/20140703095/index_2.html)
- [DOCUMENTADO] A espessura é compensada pela distância e pelo FOV para não ficar fina demais ou grossa demais; mais fina em close (1 a 3 mm) e mais grossa em plano aberto (7 a 10 mm) no rosto — [ASW Academy](https://docswell.com/s/ASW_Academy/5LVY67-GG-Toonline-Eng)
- [DOCUMENTADO, secundário] Um artigo que recria o shader do Xrd diz que a cor do contorno é uma versão mais escura da cor base do personagem, e não preto. Não é confirmado pela ArcSys, que só diz que controla a cor — [Unity Discussions, "Guilty Gear Xrd shader test"](https://discussions.unity.com/t/guilty-gear-xrd-shader-test/650399)
- [DOCUMENTADO, guia de pixel art] "Black outlines in pixel art can be very versatile, but sometimes they're a bit overpowering or too retro feeling. The solution to this is coloring your outlines". Na selout, a borda escura acompanha a sombra do sprite (mais escura onde o sprite é escuro, mais clara onde é claro). Um contorno que funciona num fundo pode falhar noutro — [Lospec, Pixel Art Outlines part 2](https://lospec.com/articles/pixel-art-outlines-part-2-using-color); [itch.io, selective outlines](https://itch.io/t/2422252/pixel-tutorial-selective-outlines)
- [DOCUMENTADO, guia] Legibilidade vem do contraste de valor (claro e escuro), não da cor; teste em tons de cinza e teste de silhueta preenchida — [Wayline](https://www.wayline.io/learn/color-palettes/1); [freegamesprites](https://freegamesprites.com/es/news/sprite-silhouette-test-character-readability)
- [DOCUMENTADO] A ArcSys também usa bloom e difusão principalmente fora da luta, "para a luta ficar fácil de ler" — [4Gamer parte 2](https://www.4gamer.net/games/216/G021678/20140714079/)

### Inferences
- [INFERIDO] **Quando o contorno ajuda:** sprites sem linha desenhada (aparência de render ou de vídeo), em cenário com valor parecido com o do personagem. Aí uma borda externa separa a figura do fundo. Deve ser **só a silhueta externa** (a ArcSys também empurra o contorno para longe da câmera para deixar só a borda), escura na cor do próprio sprite, e não preto puro.
- [INFERIDO] **Quando atrapalha:** sprites que já têm linha desenhada pela IA. A linha extra dobra a espessura, engrossa por igual cabelo e detalhe fino (o oposto da regra da ArcSys) e destaca o tremor de borda entre quadros de vídeo. Nesse caso é melhor não pôr contorno, ou pôr só uma borda muito fina e semitransparente na silhueta externa.
- [INFERIDO] Espessura: nenhuma fonte deu uma razão "linha / altura do personagem". Ponto de partida a testar: ~1 a 2 px de tela na silhueta externa para um personagem de ~300 a 400 px de altura, mantendo a linha de tela constante mesmo com o zoom da câmera (a ArcSys compensa a distância para a espessura de tela ficar estável). Esse número é inferido e precisa de teste visual.
- [INFERIDO] Coerência importa mais que o contorno: o "aleatório" que o cliente vê sai de misturar linhas desenhadas na imagem, contorno de shader uniforme e esperas sem regra. A ArcSys decide cada um desses itens com intenção: onde há linha, quanto tempo a pose fica e onde está a luz.

### Gaps
- Não encontrei fonte da ArcSys sobre contorno colorido contra preto (cor exata das linhas) nem regra de espessura em pixels ou em proporção à altura do personagem para sprites 2D.
- Não encontrei comparação direta, com dados ou depoimento de desenvolvedor, entre sprites com e sem contorno em jogos de luta.
