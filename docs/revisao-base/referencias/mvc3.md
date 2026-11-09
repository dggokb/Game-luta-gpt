# Referência: Marvel vs. Capcom 3 — Wolverine × Ryu (cenário Daily Bugle / Nova York à noite)

Material analisado: duas gravações de tela de celular tocando o YouTube.
- **V04** (10,1 s): diálogo de introdução → READY/FIGHT → combo aéreo do Ryu ("FIRST ATTACK!", "AIR COMBO!", "YES!") → Hyper Combo do Wolverine no ar, com cinemática → reinício da luta.
- **V05** (10,2 s): fim de um combo aéreo do Ryu → dash do Wolverine → defesa (faísca azul) → combo de garras no chão com lançador ("8 HITS!", "GOOD!") → Hyper Combo do Ryu (raio de energia, cinemática, "32 HITS!", "DANGER!").

## Método e convenções

- **Área de jogo:** nos quadros de 1200×540 a área de jogo vai de x = 120 a x = 1080 e ocupa a altura toda. No vídeo original de 2400×1080, ela é o recorte 1920×1080 a partir de x = 240. Todas as % abaixo são **da área de jogo**: altura = 1080 px, largura = 1920 px.
- **Numeração:** qNNNN é o número do arquivo em `vref/v04/fNNNN.jpg` e `vref/v05/fNNNN.jpg`. Conferi que esses quadros batem 1:1 com a extração em resolução cheia por ffmpeg (diferença média ≤ 0,4/255).
- **Tempo por quadro:**
  - V04: 231 quadros em 10,05 s, logo 1 quadro ≈ 43,5 ms ≈ 2,6 quadros de jogo a 60 Hz.
  - V05: 234 quadros em 9,87 s, logo 1 quadro ≈ 42 ms ≈ 2,5 quadros a 60 Hz.
  - Toda duração tem margem de **±1 quadro de gravação** (≈ ±2,5 quadros a 60 Hz).
- **Pausas por congelamento (hitstop):** quadros consecutivos idênticos (diferença média < 1/255) indicam que a imagem inteira congelou. Calculei a diferença quadro a quadro dos 240 quadros reais (V04) e dos 234 (V05).
- **Ressalvas:**
  - Quadros duplicados isolados também podem ser engasgo da gravação. Só tratei como pausa de jogo quando a duplicação coincide com um acerto.
  - **V05 q0001–q0080:** um aviso do celular ("Gravação de tela salva") cobre a faixa do HUD de cima. Ignorei essa área nesses quadros.
- **Hyper Combos com cinemática:** os dois Hypers vistos gastam **1 nível** de barra, o que se lê no número LEVEL (V04 q0154→q0155: 1→0; V05 q0157→q0158: 1→0), e mesmo assim têm cinemática com close.
  - [CONHECIMENTO] No MvC3 original, Hypers de nível 1 costumam ter só o flash, sem cinemática. O vídeo pode ser de uma versão modificada ou montagem.
  - Descrevo o que se vê. A ideia de apresentação vale para o jogo de vocês de qualquer jeito.
- **Folhas de imagem montadas** (em `img/`):
  - `mvc3_medidas_grade.jpg`: quadro neutro com grade de 5%.
  - `mvc3_camera.jpg`: 4 quadros mostrando a câmera subindo.
  - `mvc3_render_contorno.jpg`: zoom 3× no contorno e na hachura.
  - `mvc3_faiscas.jpg`: faíscas de acerto e de defesa.
  - `mvc3_combo_hitstop.jpg`: V05 q0096–q0131 completo.
  - `mvc3_hyper_wolverine.jpg` e `mvc3_hyper_ryu.jpg`: os dois Hypers.
  - `mvc3_ready_fight.jpg`, `mvc3_textos_combo.jpg`, `mvc3_textos_good_danger.jpg`: textos.
  - `mvc3_hud.jpg`: HUD com grade.
  - `mvc3_pulo_poeira.jpg`: dash, pulo, pouso e poeira.

---

## A. TELA E CÂMERA

**Altura dos personagens (em pé, sem zoom):**
- **Ryu em guarda:** topo do cabelo a 40% e pés a 96% da altura, ou seja, **≈ 56% da altura da área de jogo** [V04 q0040]. Em V04 q0150, com a câmera em outra posição, dá ≈ 54%: a diferença está dentro do erro.
- **Wolverine na guarda agachada característica:** ponta das orelhas da máscara a 52,5% e pés a 97%, ou seja, **≈ 45%** [V04 q0040]. Na prática ele é ~80% da altura do Ryu na tela por causa da postura curvada.

**Distância inicial** [V04 q0040, posições de início do round]:
- Entre os centros: Wolverine a 30,5% e Ryu a 84% da largura, **≈ 53% da largura**.
- Espaço vazio entre a ponta das garras (≈ 44%) e a frente do Ryu (≈ 76%): **≈ 32% da largura**.
- O par ocupa a tela de borda a borda. O Wolverine começa a ~20% da borda esquerda e o Ryu a ~10% da direita.

**Linha do chão:** os pés ficam a **96–97% de cima para baixo** [V04 q0040], ou seja, o chão está praticamente na borda de baixo. Os pés encostam e ficam por trás da barra de Hyper (HUD de baixo, 85–98%). Quase toda a tela fica para cima, para os pulos e o combo aéreo.

**Zoom:** não vi zoom ligado à distância entre os lutadores.
- O Ryu mede ≈ 56% em q0040 (distância máxima) e ≈ 54% em q0150 [V04], diferença dentro do erro.
- Durante os golpes de perto em V05 q0096–q0131 o tamanho dos personagens é o mesmo do neutro.
- [CONHECIMENTO] A câmera do MvC3 é quase fixa em escala. O que ela faz é transladar e subir.

**A câmera sobe no pulo e no combo aéreo:** sim, e bastante, com paralaxe 3D real.
- **V04 q0094**, Ryu acertando o Wolverine no ar no alto da tela: o guarda-corpo perto da câmera desce ≈ 8% da altura, enquanto a placa "DAILY BUGLE", lá longe, desce ≈ 1%.
- **V04 q0118**, combo mais alto: o guarda-corpo desce ≈ 17% e a placa ≈ 3%. Passa-se a ver a rua de cima, com os táxis.
- **V04 q0150:** a câmera volta para baixo e para a direita.
- Ver `img/mvc3_camera.jpg` (q0040, q0094, q0118, q0150).
- **Na horizontal** a câmera só anda quando alguém chega perto da borda. No cruzamento por cima de V04 q0057–q0075, o Wolverine passa por cima do Ryu e cai do outro lado sem a câmera se mexer, porque os dois continuaram dentro do quadro.
- No super pulo do Wolverine [V04 q0145–q0150] a câmera **não acompanha tudo**: a cabeça dele sai pela borda de cima/HUD em q0149.

**Tremor de tela:** **não observado** em nenhum golpe normal, nem no lançador.
- Nos congelamentos de acerto a imagem inteira é idêntica pixel a pixel [V05 q0113–q0116, q0117–q0119, q0123–q0127; V04 q0227–q0228]. Não há deslocamento entre quadros, então não há tremor.
- Durante os Hypers não dá para verificar (o fundo é radial e se mexe).
- O peso vem da pausa, da faísca enorme e dos rastros, não do tremor.

**Câmera nos Supers (Hyper Combos)** (detalhes na seção G):
- **V04 q0155:** corte instantâneo para outro enquadramento (aparece o letreiro "STARLIGHT" à direita e o chão fica mais baixo), com o cenário **72% mais escuro em 1 quadro**.
- **V04 q0158:** −81%. Em q0160–q0161 vem um zoom com borrão no Wolverine.
- **V04 q0162–q0168:** tela preta (7 quadros ≈ 300 ms).
- **V04 q0169–q0187:** cinemática em close, câmera baixa olhando para cima, contraluz.
- **V05:** escurecimento progressivo: −20% em q0159, −57% em q0162, −67% em q0165, preto em q0166–q0174, cinemática em q0175–q0194, volta ao jogo em q0195.

**HUD** (`img/mvc3_hud.jpg`):
- **Em cima:** ocupa de 0% a ~21% da altura. O emblema "X ∞ X" do tempo, no centro, vai até 21%; as barras de vida, finas e inclinadas, ficam entre 6% e 16%.
- **Embaixo:** ocupa de ~85% a ~98% (número "LEVEL" + barra de Hyper).
- No meio, ~64% da altura fica limpo. O HUD de cima é "vazado": entre as barras aparece o cenário.

**Princípios A**
1. Chão bem baixo (pés a ~96%) e lutador grande (~55% da altura): a luta enche a tela e sobra céu para o jogo aéreo.
2. Câmera que **sobe** com o combo aéreo e volta ao chão depois. A paralaxe forte (perto ~5× mais rápido que longe) vende a profundidade.
3. Câmera parada enquanto os dois cabem na tela: nada de balançar à toa.
4. Sem tremor nos golpes normais. O peso vem de pausa + faísca. Guarde o tremor para eventos raros.
5. HUD fino nas bordas (em cima 0–21%, embaixo 85–98%) e o miolo totalmente livre.

---

## B. CENÁRIO

**Composição:** terraço de prédio em Nova York à noite, num distrito tipo Times Square, com letreiros "DAILY BUGLE", "F.E.A.S.T.", "STARK INDUSTRIES" e "STARLIGHT" [V04 q0040, q0155]. Camadas percebidas, de perto para longe:
1. Chão do terraço: placas de concreto cinza-esverdeado escuras com juntas, que vão de ~88% da altura até a borda de baixo.
2. Guarda-corpo preto de tubos (68–80% da altura) e, à direita, uma máquina verde-escura com carretel de cabo [V05 q0090–q0157].
3. A rua lá embaixo, com táxis amarelos, carros brancos e multidão atrás de uma grade (60–75%).
4. Prédios médios de tijolo esverdeado com muitas janelas iluminadas, bandeiras americanas e luminárias.
5. Arranha-céus ao fundo e uma faixa de céu azul-escuro (20–40%, só no centro-direita).

**Paralaxe:** o cenário é 3D de verdade.
- Quando a câmera sobe, o guarda-corpo próximo se desloca ~5× mais que os letreiros distantes: 17% contra 3% [V04 q0040 × q0118].
- [CONHECIMENTO] Num jogo 2D em sprites isso se imita com 4–5 camadas a velocidades de 1,0 / 0,6 / 0,35 / 0,15 / 0.

**Hierarquia lutador × fundo** (medido em V04 q0040, HSV médio por região):

| Região | Brilho (V) | Saturação (S) | Cor média (RGB) |
|---|---|---|---|
| Prédio à esquerda, na altura da luta | 53% | 27% | (112,134,118), verde-água acinzentado |
| Fundo central, na altura da luta | 51% | 37% | (112,119,91) |
| Rua com táxis (luz âmbar) | 46% | 43% | (112,105,65) |
| Prédios no alto / céu | 48% | 33% | (83,120,110) |
| **Chão do terraço** | **22%** | 29% | (38,51,44) |
| Ryu, gi no tronco | 42% | **63%** | (43,62,99), azul royal |
| Wolverine, amarelo da coxa | 67% | **63%** | (169,138,62) |

- O fundo **não é escuro**. É claro e cheio de neon (V ~50%), mas **pouco saturado (27–43%) e de um matiz só**: verde-água/teal.
- Os lutadores se destacam pela **saturação (63%)**, pelo **matiz oposto** (azul royal e amarelo contra o verde-água) e pelo **contorno preto**.
- O **chão é bem mais escuro (V 22%)** que tudo, o que destaca pernas e pés.

**Detalhe:** o fundo é muito detalhado (janelas, placas, multidão) e não tem desfoque perto da faixa da luta. As luzes têm *bloom* forte, com janelas e letreiros "estourados" em branco com halo [V04 q0040, q0094]. O excesso de detalhe é compensado pela cor única (tudo verde-água) e pelo contorno nos lutadores.

**Elementos animados:**
- Táxis e carros andam devagar na rua: a diferença de imagem dessa região cresce de forma contínua entre q0035 e q0046 enquanto os prédios ficam estáveis [V04 q0035–q0046].
- A multidão quase não se mexe.
- A intensidade é baixa: nada pisca nem chama a atenção.

**Sombra dos personagens:** mancha escura de borda suave, **projetada na diagonal para trás e para a esquerda** dos pés, não é uma elipse embaixo. A do Ryu ocupa ~8% da largura × ~3% da altura, com opacidade estimada em ~50% [V04 q0040]. Não há reflexo no chão.

**Luz do cenário nos personagens:**
- O sombreamento toon é praticamente fixo.
- Há um leve brilho ciano na borda da máscara do Wolverine, reflexo do neon [V05 q0145].
- Nos Hypers o cenário escurece mas os personagens continuam iluminados (ver G).

**Paleta:** verde-água/teal dominante, âmbar dos postes e táxis, preto nos tubos e no chão, branco estourado nas luzes. Os personagens usam azul royal e amarelo, de propósito fora dessa paleta.

**Princípios B**
1. Fundo **monocromático** (uma família de matiz) e lutadores em matizes complementares saturados: separa sem precisar escurecer o fundo.
2. **Chão mais escuro** que o resto do cenário, para os pés e as pernas sempre lerem.
3. Muito detalhe no fundo é aceitável **se** a saturação dele for baixa e os lutadores tiverem contorno.
4. Paralaxe forte entre o perto (guarda-corpo, máquina) e o longe (prédios).
5. Animação do cenário discreta (carros lentos), sem competir com a luta.

---

## C. PERSONAGENS (RENDER)

[CONHECIMENTO] São modelos 3D com *cel-shading* que imita a arte-final de quadrinho americano. O resultado, para um jogo em sprites, é um alvo direto: **cara de página de gibi**.

**Contorno** (`img/mvc3_render_contorno.jpg`, zoom 3×):
- **Existe sempre**, preto ou marrom-quase-preto.
- Na silhueta externa tem ≈ **2–3 px a 1080p**; nas linhas internas (dobras do gi, divisão entre músculos, bordas do uniforme), ≈ **1–2 px** [V04 q0040, V05 q0145].
- A espessura varia: é mais grossa nas costas e ombros e mais fina nas mãos e no rosto.

**Sombreamento:**
- **Sombra chapada de borda dura**: 2 tons principais (luz/sombra) e mais um tom quase preto nas oclusões. Exemplos: o vão do peito sob o gi do Ryu, as axilas, a parte de dentro do braço [V04 q0040].
- Na pele há uma leve transição de gradiente entre os tons, mas a leitura é de "chapado".
- **Hachura de quadrinho:** linhas finas paralelas em diagonal dentro das áreas de sombra. É bem visível no ombro do Wolverine, como hachura de nanquim [V05 q0145; V04 q0040].
- O gi azul tem azul médio chapado com sombras azul-marinho de borda dura.

**Contraluz:**
- No jogo normal, quase nenhum.
- Na cinemática, **contraluz total**: o Wolverine em silhueta quase preta, só com o amarelo e as garras pegando luz, contra um disco branco-azulado [V04 q0170–q0181]. O Ryu idem, com o fundo azul em leque [V05 q0175–q0190].

**Leitura de silhueta:**
- **Wolverine:** orelhas pontudas da máscara, ombros largos, guarda agachada com o tronco à frente e **3 garras prateadas finas** projetadas para a frente (≈ 8% da largura da tela) [V04 q0040]. Lê-se mesmo em silhueta preta [V04 q0170].
- **Ryu:** silhueta vertical, larga e estável. Os detalhes que identificam são as pontas da bandana voando, as mangas rasgadas com os ombros nus, as luvas amarelas e os pés descalços [V04 q0040; V05 q0129–q0135].

**Proporções:** heroicas de quadrinho.
- Ryu: cabeça ≈ 1/7,5 da altura, ombros ≈ 2,5 cabeças de largura, mãos e pés grandes.
- Wolverine: compacto e largo, ombros ≈ 3 cabeças.

**Taxa de animação:**
- [CONHECIMENTO] O jogo roda a 60 Hz com interpolação 3D.
- Na gravação, todos os quadros de movimento diferem entre si (não há poses seguradas fora das pausas de acerto).
- O *timing* é "estalado": a antecipação de um golpe dura 1–2 quadros de gravação (≈ 40–85 ms) e o impacto aparece **já na pose extrema** [V05 q0096–q0098].
- Depois a pose de impacto é **segurada** pela pausa de acerto (2–5 quadros, ver I).
- Para sprites, o equivalente é: poucas poses de ida, pose de impacto bem exagerada e segurada.

**Princípios C**
1. **Contorno preto de 2–3 px (a 1080p) na silhueta e 1–2 px por dentro**, com espessura variando.
2. Sombra **chapada de borda dura** em 2 tons + tom de oclusão quase preto. Gradiente, no máximo, na transição.
3. **Hachura de nanquim** nas sombras grandes, que é o detalhe que diz "quadrinho".
4. Cada personagem com 1–2 formas de silhueta inconfundíveis (orelhas e garras; bandana e ombros nus).
5. Golpe = antecipação curta → pose extrema já no primeiro quadro de impacto → pose segurada.

---

## D. ANIMAÇÃO POR ESTADO

**Intro** [V04 q0001–q0024]:
- q0001–q0008: close no time do Ryu (Capitão América, Ryu, Chris) e depois no do Wolverine (Deadpool, Hulk, Wolverine). A legenda branca "Interesting... You fight like a wild animal!" fica a ~93% da altura, em fonte sem serifa pequena (~2,5% da altura), sobre uma faixa escura translúcida.
- q0009–q0011: o HUD entra.
- q0011–q0024: os parceiros (Hulk, Deadpool, Capitão, Chris) **saltam para fora** do quadro enquanto o "READY" se forma. Ficam só os dois lutadores.

**Idle/guarda:**
- **Ryu:** punhos à frente do rosto e do peito, base aberta. A oscilação é muito pequena, ≈ 1% da altura, sem ritmo claro em ~0,5 s [V04 q0033–q0042]. Em q0042–q0046 ele ajusta o passo.
- **Wolverine:** agachado, com as garras à frente e para baixo e o tronco inclinado ~40°. Oscila pouco [V04 q0033–q0046].

**Dash/investida:**
- **Ryu** [V04 q0058–q0062]:
  - **Investida longa para a frente** com o tronco inclinado ~30°, a perna de trás totalmente estendida e o pé da frente bem à frente.
  - Puffs de **poeira branca** nos dois pés em q0059–q0061, que viram fumaça cinza em q0062–q0066.
  - Repete em V05 q0042–q0046.
- **Wolverine** [V05 q0028–q0033]: corrida baixa com o corpo quase paralelo ao chão (~50°) e nuvens de poeira atrás em cada passada (q0029, q0031).

**Pulo** (`img/mvc3_pulo_poeira.jpg`):
- **Ryu** [V04 q0076–q0081]:
  - Pré-pulo agachado em **1 quadro** (q0077, ≈ 43 ms ≈ 2–3 quadros a 60 Hz).
  - Decolagem com poeira branca nos pés em q0078.
  - Subida com os joelhos dobrados (pose "tuck") em q0079–q0081: os pés sobem de ~88% para ~40% da altura em 3 quadros (≈ 130 ms). É um pulo muito rápido e alto.
- **Super pulo do Wolverine** [V04 q0144–q0150]: agachado em q0144, decolagem com poeira em q0145, e sai pela borda de cima em q0149 (≈ 5 quadros ≈ 220 ms).
- **Pouso:**
  - Wolverine: agachamento de 2–3 quadros + poeira [V04 q0073–q0075] e 4 quadros em [V04 q0140–q0144].
  - Ryu: pousa com poeira nos dois lados [V04 q0124–q0131].

**Defesa** [V05 q0059–q0067]: guarda alta em pé, com o antebraço da frente na vertical diante do rosto e o outro punho no peito. O tronco recua levemente e a pose é segurada enquanto os golpes batem. Ver J para a faísca azul.

**Queda e levantar** [V05 q0136–q0145]:
- O Ryu vem do ar de lado (q0136–q0138) e bate no chão com o tronco e o ombro (q0139–q0141), com um quique pequeno e poeira curta.
- Em q0142–q0145 ele **levanta rápido**: ~4 quadros ≈ 170 ms até a guarda.
- [CONHECIMENTO] É um *tech*/levantar rápido típico do MvC3.

**Movimento secundário:**
- As pontas da bandana do Ryu voam atrás da cabeça no ar [V05 q0129–q0135] e na queda.
- As mangas rasgadas do gi balançam.
- O Wolverine não tem pano solto. O "secundário" dele são as **garras deixando rastro roxo** (ver J).

**Expressão:** o Wolverine rosna com os dentes à mostra o tempo todo [V05 q0145, zoom]. O Ryu mantém a cara séria e franzida na guarda e grita com a boca aberta no Hyper [V05 q0192–q0194].

**Vitória:** não aparece nos vídeos.

**Princípios D**
1. Pré-pulo de **1 quadro** (≈ 2–3 a 60 Hz) e subida explosiva: o pulo parece leve e responsivo.
2. **Poeira em toda saída e chegada** (dash, decolagem, pouso, queda): ancora o personagem no chão.
3. Guarda com **pose característica** e pouca oscilação. Personalidade na pose, não no balanço.
4. Levantar rápido depois da queda (~170 ms) para não matar o ritmo.
5. Um elemento secundário por personagem (pano ou rastro de arma) que mostra a direção do movimento.

---

## E. GOLPES NORMAIS

Corrente de garras do Wolverine, no chão (`img/mvc3_combo_hitstop.jpg`, V05 q0096–q0131):

**Antecipação** [q0096–q0097]: Wolverine agachado com o braço da frente recolhido para trás e o ombro girado. Dura 2 quadros (≈ 85 ms).

**Impacto** [q0098]:
- **Braço totalmente estendido** atravessando o Ryu, quadril projetado, perna de trás esticada e pé de apoio girado: o corpo inteiro vai junto.
- O "exagero" não está na deformação do corpo. Está no **efeito desenhado**:
  - 3 riscos branco-incandescentes horizontais vindos de trás do Wolverine (~30% da largura da tela);
  - linhas vermelhas finas paralelas;
  - uma bola de explosão laranja.

**Pose segurada:** 2 quadros idênticos (q0098 = q0099, ≈ 85 ms ≈ 5 quadros a 60 Hz).

**Recuperação** [q0100–q0102]: o Wolverine volta a agachar em ~3 quadros, enquanto o efeito vira rastro magenta e depois "arranhão" vermelho-escuro.

**Diferença fraco/médio/forte:** pela sequência, a "força" aparece por três escalas:
1. **Tamanho e cor da faísca:**
   - rastro magenta sem bola (q0101, q0106);
   - bola laranja de ~38% da altura da tela (q0098, q0103);
   - explosão grande + risco branco de ~45% da largura (q0113);
   - pluma vermelha vertical no lançador (q0117).
2. **Pausa:** 2 → 3 → 4–5 quadros (ver I).
3. **Direção do efeito:** horizontal nos golpes no chão, diagonal para cima no lançador [q0111–q0112, q0117–q0128].

**Golpes do Ryu:**
- **Chute aéreo** [V04 q0094]: perna de trás em extensão total, quase horizontal; o tronco inclina para trás para contrabalançar.
- **Tatsumaki aéreo** [V05 q0003–q0011; V04 q0102–q0110]: o corpo gira com a perna estendida, envolto em **anéis de rastro branco-ciano** com ~1,5× a largura do corpo.

**Princípios E**
1. Antecipação curta (1–2 quadros de gravação), impacto já na **pose mais extrema** e pose segurada pela pausa.
2. Corpo inteiro no golpe: quadril projetado, perna de trás esticada, pé de apoio girado.
3. O "exagero" vem de **rastros e riscos desenhados** (smear como efeito), muito maiores que o membro.
4. A força se lê por 3 canais ao mesmo tempo: tamanho e cor da faísca, duração da pausa e direção do efeito.

---

## F. ESPECIAIS E PROJÉTEIS

Nos dois vídeos não aparece projétil comum (Hadouken normal). O que aparece:

**Tatsumaki do Ryu (no ar)** [V05 q0003–q0011]:
- Giro com espirais de rastro **branco no miolo e ciano nas bordas**: 2–3 arcos concêntricos ao redor do corpo, ~1,5× a largura dele, sumindo em ~2 quadros cada.
- No acerto, a faísca é amarela [V04 q0094].

**Lançador / anti-aéreo do Wolverine** [V05 q0111–q0128]:
- Garra de baixo para cima com **riscos diagonais magenta e brancos** (~45° de inclinação, ~40% da largura).
- No acerto, uma **pluma vermelha vertical** (forma de pincelada larga com a ponta para cima, ~35% da altura da tela) atrás do Ryu, mostrando para onde ele voa [q0117–q0121, q0128].
- Rastros roxos de garra seguem o Wolverine na subida.

**Raio do Hyper do Ryu:** ver G e J. É o único "projétil" visto, enorme.

**Investidas:** nos dashes não há linhas de velocidade, só poeira [V04 q0059–q0062; V05 q0029–q0033]. No Hyper do Wolverine há um fundo de **linhas de velocidade radiais azuis/brancas** atrás dele no mergulho [V04 q0188–q0190].

**Princípios F**
1. Golpe giratório = **anéis de rastro** que envolvem o corpo (branco no centro, cor do personagem na borda).
2. Lançador = efeito **apontando para onde a vítima vai** (pluma vermelha para cima).
3. Investida comum sem linhas de velocidade (só poeira); as linhas ficam reservadas para o Hyper, o que valoriza o Hyper.

---

## G. SUPERS (Hyper Combo): apresentação

### G1. Hyper do Wolverine (V04, no ar) — `img/mvc3_hyper_wolverine.jpg`

| Quadros | Duração (≈) | O que acontece |
|---|---|---|
| q0154 | — | Wolverine no ar, no alto à esquerda, em pose de mergulho. LEVEL 1. |
| **q0155** | instantâneo | **Corte de câmera** para outro enquadramento. **Cenário −72% de brilho em 1 quadro.** Um **globo de luz** amarelo-laranja com miolo branco (~35% da altura da tela de diâmetro) aparece em volta do Wolverine. O Ryu vira silhueta escura. O **LEVEL cai de 1 para 0 no mesmo quadro**. |
| q0156–q0157 | 2 quadros ≈ 85 ms | O globo cresce e pequenas faíscas e setas saem dele. |
| q0158–q0159 | 2 quadros ≈ 85 ms (quadros idênticos = congelado) | Cenário −81%, com tom avermelhado e **vinheta vermelha**. O HUD de cima começa a sumir (retratos somem). |
| q0160–q0161 | 2 quadros ≈ 85 ms | Zoom com borrão no Wolverine, fundo vermelho-escuro. |
| **q0162–q0168** | **7 quadros ≈ 300 ms** | **Tela preta.** HUD inteiro fora. |
| q0169–q0187 | 19 quadros ≈ 830 ms | **Cinemática:** close baixo de costas no Wolverine com as garras erguidas, quase em silhueta. Um disco de luz branco-azulado vira **leque de raios azuis** (q0169–q0181). Ele salta, encolhe as pernas e cruza as garras (q0182–q0187). O Ryu aparece pequeno ao fundo, em silhueta azul. O cenário não aparece. |
| q0188 | 1 quadro | Mergulho em diagonal com **túnel de linhas radiais azul/branco** (cenário trocado). |
| q0189–q0190 | 2 quadros | Volta o HUD. O fundo continua **trocado por raios brancos e ciano radiais**. O Wolverine mergulha. |
| q0191–q0209 | 19 quadros ≈ 830 ms | **"X" gigante de energia**: duas faixas serrilhadas magenta/branco cruzadas, que ocupam a tela inteira na diagonal. Fundo azul-marinho/preto. Riscos amarelos e vermelhos finos. Picos de branco estourado em q0193, q0198 e q0205. O Ryu fica em **guarda** à direita o tempo todo e a barra de vida dele não muda visivelmente: ele defendeu. |
| q0208–q0209 | 2 quadros idênticos ≈ 85 ms | Pausa final. |
| q0210 | 1 quadro | **Tela branca** (brilho médio 251/255). |
| q0211–q0212 | 2 quadros ≈ 85 ms | Imagem **em tons de cinza**, raios brancos se abrindo e o cenário reaparecendo por baixo. |
| q0213 | 1 quadro | Cenário volta **azulado e escuro**. Um "X" vermelho no lugar do Wolverine. |
| q0214–q0220 | 7 quadros ≈ 300 ms | **O fundo do Hyper se "rasga" em cacos**: pedaços grandes vermelhos com borda branca (como papel ou vidro) voam do centro para fora da tela, revelando o cenário normal, que recupera a cor em ~4 quadros. |
| q0221 | — | Jogo normal. Total desde a ativação: **≈ 66 quadros ≈ 2,9 s**. |

### G2. Hyper do Ryu (V05, no chão, raio de energia) — `img/mvc3_hyper_ryu.jpg`

| Quadros | Duração (≈) | O que acontece |
|---|---|---|
| q0157 | — | Wolverine atacando de perto. Ryu com LEVEL 1. |
| **q0158** | instantâneo | LEVEL do Ryu cai de 1 para 0. O chão fica amarelado e iluminado. Um **globo de luz** branco com halo amarelo-laranja (~45% da altura da tela) aparece **no Ryu**. |
| q0159–q0161 | 3 quadros iguais ≈ 125 ms (congelado) | Cenário −20%, com o globo e anéis concêntricos laranja. |
| q0162–q0165 | 4 quadros ≈ 170 ms (2 pares iguais) | Cenário −57% e depois −67%, com o globo ainda brilhando. Os lutadores ficam tingidos de marrom. |
| q0166 | 1 quadro | Transição: close borrado **em cinza** (dessaturado). |
| q0167–q0174 | 8 quadros ≈ 340 ms | Quase preto. Só um **rastro roxo das garras** do Wolverine e um borrão cinza do punho aparecem e crescem. |
| q0175–q0194 | 20 quadros ≈ 840 ms | **Cinemática:** câmera baixa olhando para cima no Ryu. Ele pisa forte, com nuvem branca/cinza no chão, e depois puxa os punhos para a cintura com a boca aberta, gritando. O fundo é **azul em leque com disco branco**. O Wolverine aparece em silhueta azul com as garras roxas, avançando. O contraluz deixa o Ryu quase em silhueta no começo (q0175–q0180). |
| **q0195** | 1 quadro | Volta o HUD. O cenário é **trocado** por um fundo de raios radiais. Uma **bola de energia azul-ciano com miolo branco** (~45% da altura) sai das mãos do Ryu. Salto de brilho enorme (diferença média de 110/255 em 1 quadro). |
| q0196–q0199 | 4 quadros ≈ 170 ms | Tela **quase toda branca-ciano** (brilho 220–231/255): flash de saída do raio. Explosão vermelha/amarela no Wolverine, à direita. |
| q0200–q0234 (corta) | 35+ quadros ≈ 1,5 s+ | Fundo **azul-marinho escuro com estrela radial** (brilho ~37/255, contra ~135 do cenário normal). O raio é uma massa de jatos amarelos e alaranjados com bordas azul-ciano, com redemoinhos azuis, ocupando a metade direita. O contador sobe de "7 HITS!" (q0204) até "32 HITS!" (q0234) e "DANGER!" aparece do lado do Wolverine (q0211 em diante). |

**Princípios G**
1. **Ativação = flash localizado no personagem** (globo branco, amarelo e laranja de ~35–45% da altura da tela) + **escurecimento do cenário de 70–80%**, no mesmo quadro ou em ~3 quadros. O número da barra cai no mesmo quadro.
2. **Congelar o mundo** durante o flash (quadros idênticos) para o jogador "ler" quem ativou.
3. **Trocar o cenário** por um fundo abstrato (raios radiais azuis ou estrela escura) durante o Hyper. Assim o efeito do golpe pode ser enorme e saturado sem brigar com o cenário.
4. Close/retrato do lutador (aqui uma cinemática de ~0,8 s; num jogo 2D, um *cut-in* de retrato de 0,5–0,8 s) **antes** do golpe sair.
5. Final com **pausa + tela branca + volta em cinza + transição gráfica** (cacos voando) devolvendo o cenário aos poucos (~0,5 s).

---

## H. REAÇÃO A ACERTO

**Dano alto em pé (Ryu)** [V05 q0100–q0102, q0108–q0110]:
- **Cabeça jogada para trás**, queixo para cima, peito projetado e braços abertos para os lados.
- O tronco inclina ~20° para trás e os pés ficam no lugar.
- A pose é segurada durante a pausa e muda de golpe para golpe (q0102 e q0108 são variações).

**Lançado (Ryu)** [V05 q0117–q0128]: o corpo se dobra, sobe pela direita, e a pluma vermelha vertical atrás dele mostra a direção.

**No ar** [V05 q0129–q0136]: o corpo fica **horizontal**, de lado, com pernas e braços soltos e a bandana voando. Ele gira devagar.

**Queda** [V05 q0136–q0142]: cai de lado e de ombro, quique pequeno, poeira curta. **Tempo no chão: ≈ 2–3 quadros** antes de começar a levantar. Levantar ≈ 4 quadros (≈ 170 ms) [q0142–q0145].

**O atingido treme na pausa?** **Não.** Os quadros de pausa são idênticos pixel a pixel [V05 q0113–q0116, q0123–q0127]. Não há tremor do personagem nem da tela. Ressalva: um tremor de 1–2 px de período curto poderia sumir na conversão 60 → 30 → 24 Hz, mas não aparece.

**Recuo:**
- **Na defesa:** a borda traseira do Ryu passa de 62% para 74% da largura em 2 golpes defendidos, ou seja, **≈ 5–6% da largura por golpe** [V05 q0056–q0068]. A câmera parece parada, mas pode ter acompanhado um pouco.
- **No acerto:** perto da borda o Ryu recua pouco e a câmera acompanha. Não deu para medir com precisão.

**Cor no atingido:**
- **Não pisca branco nem vermelho chapado.**
- Ele fica **tingido pela luz da faísca**: laranja dentro da bola de explosão [V05 q0098, q0148] e magenta dentro dos rastros [q0101, q0113].
- Na ativação do Hyper do Ryu, os dois ficam marrom-escuros [V05 q0162–q0165].

**Wolverine sob o Hyper** [V05 q0198–q0234]: quase encoberto pelo raio. Aparece contorcido, com os membros abertos, dentro da explosão amarela/vermelha.

**Princípios H**
1. Dano alto = **cabeça para trás + peito para frente**, pose clara e segurada.
2. Vítima no ar = **corpo horizontal** e pano/cabelo voando: lê-se "fora de controle".
3. Feedback de cor pela **luz da faísca** sobre o corpo, e não um piscar genérico.
4. Recuo na defesa visível (~5% da largura por golpe) para a defesa também "empurrar".
5. Queda curta e levantar rápido em jogo de combo.

---

## I. IMPACTO

**Pausa no acerto (hitstop):** a imagem inteira congela, incluindo o cenário. Medido em quadros idênticos de V05 (1 quadro ≈ 42 ms):

| Golpe | Quadros idênticos | ≈ ms | ≈ quadros a 60 Hz |
|---|---|---|---|
| Garra no chão, 1º golpe da corrente (bola laranja) [V05 q0098–q0099] | 2 | ~85 (40–125) | ~5 (3–8) |
| Garra seguinte (bola laranja) [V05 q0103–q0105] | personagens parados ~3 quadros enquanto a faísca anima (a faísca muda) | ~125 | ~7 |
| Golpe agachado [V05 q0109–q0110] | 2 | ~85 | ~5 |
| Golpe forte antes do lançador (risco branco + explosão) [V05 q0113–q0116] | **4** | **~170** | **~10** |
| Lançador (pluma vermelha) [V05 q0117–q0119] | 3 | ~125 | ~7–8 |
| Golpe aéreo seguinte ("8 HITS") [V05 q0123–q0127] | **5** | **~210** | **~12–13** |
| Golpe de garra com explosão laranja e anel vermelho [V05 q0148–q0150] | 3 | ~125 | ~7–8 |
| Golpes aéreos do Ryu (faísca amarela) [V04 q0082, q0088, q0091–q0094] | 0–2 (só q0094–q0095 duplica) | 0–85 | 0–5 |
| Pausa antes do branco final do Hyper [V04 q0208–q0209] | 2 | ~85 | ~5 |
| Flash de ativação do Hyper [V05 q0159–q0161; V04 q0158–q0159, q0160–q0161] | 2–3 seguidos, em degraus | 85–125 por degrau | — |
| **Defesa** [V05 q0059–q0067] | **0** (todos os quadros diferentes) | <40 | <3 |

Conclusão: a pausa **cresce ao longo do combo e com a força** (≈ 5 → 10 → 12 quadros a 60 Hz). A **defesa quase não pausa**. Os golpes aéreos rápidos do combo pausam pouco.

**Tremor de tela:** não observado (ver A).

**Contra-ataque:**
- Não aparece texto de "COUNTER".
- O que aparece é **"FIRST ATTACK!"** (primeiro golpe da luta) [V04 q0083–q0097]: amarelo, 2 linhas, entra deslizando ("FIRST" em q0083, "ATTACK!" em q0084).
- A faísca do primeiro acerto é amarela e grande, como as outras.

**Câmera lenta:** não observada.

**Ordem quadro a quadro de um golpe forte que acerta** [V05 q0112–q0121]:
1. **q0112:** último quadro de aproximação. Rastro magenta/branco ainda do golpe anterior.
2. **q0113 (contato):** **faísca já no tamanho máximo, sem crescer**:
   - risco branco-incandescente horizontal de ~45% da largura vindo do Wolverine;
   - linhas vermelhas finas;
   - explosão laranja e vermelha de ~35% da altura no ponto de contato;
   - rastros roxos das garras acima;
   - Ryu tingido de laranja e magenta.
3. **q0114–q0116:** **congelado** (imagem idêntica, 4 quadros no total ≈ 170 ms). Sem tremor.
4. **q0117 (próximo golpe, o lançador):**
   - a faísca anterior some de uma vez e é trocada pela **pluma vermelha vertical** + riscos magenta diagonais;
   - um puff de **poeira branca** grande no pé do Wolverine;
   - o Ryu já na pose de lançado.
5. **q0118–q0119:** congelado (3 quadros ≈ 125 ms).
6. **q0120:** a poeira fica cinza e se desfaz, a pluma começa a sumir e o Ryu sobe.
7. **q0121–q0122:** restos (riscos finos) somem. O Ryu sobe e a câmera acompanha.

Resumo da ordem: **contato e faísca grande no mesmo quadro → pausa congelada → a faísca é trocada pelo efeito de direção (pluma/riscos) quando o movimento volta → poeira → restos somem em 1–2 quadros.**

**Princípios I**
1. Faísca **no tamanho máximo já no quadro do contato** e congelamento completo logo depois.
2. Pausa proporcional: ~5 quadros a 60 Hz para golpe comum, ~10–12 para forte ou fim de sequência. Defesa ~0–2.
3. Ao sair da pausa, **trocar** a faísca por um efeito de direção (rastro/pluma), em vez de só apagar.
4. Peso sem tremor de tela: pausa + tamanho do efeito bastam. O tremor fica para o Super, se houver.
5. Um texto especial no primeiro acerto da luta ("FIRST ATTACK!") dá recompensa sem mudar a mecânica.

---

## J. EFEITOS VISUAIS (`img/mvc3_faiscas.jpg`)

**Faíscas de acerto.** Todas são 2D desenhadas: cores chapadas, bordas serrilhadas, sem cara de partícula.

1. **Amarela, golpes do Ryu** [V04 q0082, q0088, q0094]:
   - **Estrela/explosão amarela de 8–12 pontas serrilhadas**, miolo branco e laranja nas pontas.
   - Mede ~40% da altura da tela (~70% da altura do personagem) no 3º golpe (q0094) e ~25% nos primeiros.
   - Dura 2–3 quadros e some em 1.
   - Riscos amarelos saem na direção do golpe [q0083, q0096].
2. **Laranja com riscos brancos, garra do Wolverine** [V05 q0098, q0103, q0148]:
   - **Bola de explosão laranja e vermelha** com núcleo amarelo-branco, de ~38% da altura da tela (~68% da altura do Ryu).
   - Em q0148 tem **anel vermelho-escuro em volta** (~44% da altura).
   - **3 riscos branco-incandescentes horizontais** (as garras) com ~30–45% da largura da tela e linhas vermelhas finas paralelas.
   - Dura 2–3 quadros, inclusive a pausa. Depois vira rastro magenta (1–2 quadros) e um **"arranhão" vermelho-escuro** semitransparente (1 quadro) [q0100–q0102, q0104–q0107].
3. **Explosão vermelha espinhosa** [V05 q0151–q0152; V04 q0229–q0230]:
   - Estrela **vermelha de muitos espinhos** irregulares, ~50% da altura da tela, com **jatos amarelo-laranja** apontando para trás, para o atacante.
   - Os espinhos se espalham e fragmentam em 2 quadros e somem no 3º.
4. **Pluma do lançador** [V05 q0117–q0121, q0128]: pincelada vermelha vertical, larga embaixo e fina em cima, ~35% da altura, atrás da vítima.

**Faísca de defesa** [V05 q0059–q0062 e q0064–q0067]:
- **Muro de cacos azul-ciano translúcidos**, como vidro ou cristal quebrado: lâminas finas em leque vertical na frente do Ryu, do pé até acima da cabeça (**~60% da altura da tela, a altura do personagem inteiro**), com borda branca brilhante e miolo azul.
- **q0059:** aparece estreito.
- **q0060–q0061:** abre em leque, com as lâminas se separando.
- **q0062:** fica transparente e some.
- **Duração ≈ 4 quadros ≈ 170 ms.** Cor oposta à do acerto (frio × quente).

**Poeira:**
- **Nuvem branca chapada estilo desenho**: formas arredondadas brancas com miolo cinza claro e borda dura. Mede ~5–10% da altura e aparece nos pés em dash, pulo, pouso e queda [V04 q0059–q0063, q0073–q0080, q0124–q0131; V05 q0025–q0033].
- Nos 2–3 quadros seguintes **vira fumaça cinza translúcida** e se desfaz [V04 q0062–q0066].
- A nuvem no dash de entrada do Hyper do Ryu é bem maior (~20% da altura) [V05 q0158–q0165].

**Rastros, linhas de velocidade e smears:**
- **Rastro das garras do Wolverine:** **3 faixas paralelas roxo-violeta e magenta**, com centro branco, longas (até ~50% da largura) e retas, seguindo o caminho do braço. Ficam 2–4 quadros e se apagam da ponta para trás [V05 q0063–q0067, q0070–q0080, q0111–q0135].
- **Anéis do Tatsumaki do Ryu:** branco e ciano [V05 q0003–q0011].
- **Linhas de velocidade radiais:** só no Hyper [V04 q0188–q0190; V05 q0195–q0234].

**Brilhos e auras:**
- Globo de ativação do Hyper (ver G).
- *Bloom* forte em tudo que é luz do cenário.
- Durante o Hyper do Wolverine, o "X" de energia tem miolo branco estourado e borda magenta/violeta, com riscos finos amarelos e vermelhos espalhados [V04 q0191–q0209].

**Efeitos de chão:** só poeira. Não há marca ou rachadura no chão.

**Partículas do cenário reagindo:** não observei.

**Princípios J**
1. **Uma família de faísca por tipo de golpe** e cor própria por personagem: amarela para impacto, laranja + riscos brancos para corte. A **defesa é sempre azul fria**.
2. Faísca **enorme** (40–50% da altura da tela, ~70% do personagem), chapada, com borda serrilhada e miolo branco.
3. A faísca dura 2–3 quadros e **se transforma** (bola → rastro magenta → arranhão vermelho escuro) em vez de sumir em fade.
4. Poeira **branca chapada estilo cartoon** que depois vira fumaça cinza translúcida.
5. Rastro de arma colorido (roxo/magenta) com 3 faixas paralelas, que repete a forma da garra.

---

## K. HUD E TEXTOS (`img/mvc3_hud.jpg`, `img/mvc3_textos_*.jpg`, `img/mvc3_ready_fight.jpg`)

**Barras de vida** [V05 q0090; V04 q0040]:
- **Forma e posição:** **paralelogramos inclinados**, finos (~2,5–3% da altura), em y ≈ 10–13%.
  - P1 de ~19% a ~42% da largura; P2 de ~57% a ~80% (~23% da largura cada).
  - Ligam o retrato na ponta de fora ao emblema do tempo no centro.
- **Cores:**
  - Vida atual **amarela** (no Ryu, degradê de amarelo para **verde** na ponta de fora).
  - Vida perdida "recuperável" em **vermelho**, na ponta de fora, junto ao retrato.
  - Vazio em azul-escuro/preto.
  - Contorno ciano claro.
- **Direção:** a vida restante fica **ancorada no centro** e a barra encolhe a partir da ponta do retrato.
  - Exemplo: o vermelho do Wolverine cresce da esquerda para a direita durante o Hyper do Ryu [V05 q0001 → q0157 → q0234].
- **Vermelho que fica:** é o "vermelho recuperável" do MvC3 [CONHECIMENTO]. Não vi o vermelho descer até o amarelo no tempo dos vídeos.
- **Barras dos parceiros:** acima e abaixo da principal há **barras finas ciano** (~1% da altura) com o nome do parceiro ("HULK" em cima, "DEADPOOL" embaixo; "CAPTAIN AMERICA" e "CHRIS" do lado do Ryu). O nome do lutador ativo fica em letras brancas grossas sobre a barra principal ("WOLVERINE", "RYU").

**Retratos:** moldura **triangular/em V** nas pontas: x 8–17% e 83–92%, y 2–20%. Mostra o rosto do lutador ativo grande e os dois parceiros pequenos, marcados "A1" e "A2" (assistências).

**Tempo:** no centro, emblema **"X ∞ X"** vermelho e rosa com brilho: dois X grandes e um "∞" (tempo infinito), com "TIME" embaixo. Ocupa x 41–59%, y 0–21%. É o elemento mais chamativo do HUD.

**"SCORE 0":** texto cinza fino no alto à esquerda (y ~3%) [V04 q0040].

**Barra de Hyper** (embaixo) [V05 q0090]:
- Número **"LEVEL" grande** (o algarismo tem ~12% da altura), azul em itálico com contorno, dentro de um **hexágono** escuro. Fica em x 9–15% (P1) e 85–91% (P2), y 85–97%.
- Barra fina (~3% da altura) de 16% a 44% (P1) e de 55% a 83% (P2), em y ≈ 89–92%.
- Enche em **azul**; o trecho recém-ganho aparece em **ciano claro** [V05 q0090, lado do Ryu].
- Ao gastar um nível, o número troca no mesmo quadro [V04 q0155; V05 q0158].
- "Segmentos" = níveis numerados (0, 1, …), não divisões visíveis na barra.

**Contador de golpes:**
- Aparece **do lado do HUD de quem ataca**: esquerda para o P1 (V05 q0107–q0146), direita para o P2 (V04 q0091–q0099; V05 q0204–q0234). Não fica em cima do personagem.
- Ocupa x ~10–32% ou ~72–91%, y ~34–48%.
- O **número** é grande (~12% da altura), em itálico, preenchido de vermelho no meio e laranja/amarelo nas bordas, com contorno escuro.
- A palavra "HITS!" é menor (~6%), com o mesmo estilo, inclinada.
- Cada número novo entra com 1 quadro meio transparente [V04 q0091] e fica sólido no seguinte.

**Textos de evento** (todos em letra de quadrinho itálica e grossa, com contorno escuro, inclinada subindo para a direita):
- **"FIRST ATTACK!"** [V04 q0083–q0097]: amarelo com contorno preto, 2 linhas, ~6% de altura por linha, ao lado do contador. "FIRST" entra e depois "ATTACK!" desliza da direita. Some escurecendo/semitransparente quando o "AIR COMBO!" entra.
- **"AIR COMBO!"** [V04 q0094–q0130]: **ciano claro com contorno azul-escuro**, 2 linhas ("AIR" / "COMBO!"), cada uma com ~5–6% de altura, em x 73–87%, y 21–33%. Entra palavra por palavra ("AIR" em q0094, "COMBO!" em q0097–q0098).
- **"YES!"** [V04 q0107–q0135; V05 q0008–q0037]: **azul royal com contorno azul-escuro**.
  - Entra **gigante** (q0107: só o "Y", maior que 25% da altura; q0108: "YES!" com ~25%), encolhe em q0109 e assenta em q0110 com ~13–15% da altura. É uma entrada com zoom de ~2× para 1× em ~3 quadros (≈ 130 ms).
  - Fica ~1,2 s (V05 q0008–q0037, ~30 quadros) e some com fade.
- **"GOOD!"** [V05 q0140–q0165]: **azul-ciano com contorno escuro**.
  - Entra pela esquerda com o "G" cortado pela borda (q0140), "GO" gigante com ~30% da altura (q0141), encolhe e gira (q0142) e assenta em q0143 com ~12% (x 7–22%, y 33–48%).
  - Aparece quando o combo de 8 golpes termina e fica ~1 s.
- **"DANGER!"** [V05 q0211–q0234]: **rosa-magenta com contorno roxo**, ~6% de altura, em x 8–23%, y 21–27%. Fica **do lado do Wolverine**, que está apanhando com a vida em queda. É um provável aviso de risco ou vida baixa [inferência].

**READY / FIGHT** (`img/mvc3_ready_fight.jpg`):
- **"READYYYY!"** [V04 q0011–q0034]:
  - Letras **vermelho-alaranjadas em degradê** (vermelho embaixo, laranja/amarelo em cima), contorno preto, crescendo da esquerda para a direita (o último "Y!" é ~2× o "R").
  - O texto ocupa x ~10–85%, com letras de ~15–30% da altura.
  - **Cada letra chega voando sozinha**, enorme, girando e caindo no lugar (q0013–q0025, ~12 quadros ≈ 0,5 s).
  - Fica parado de q0025 a q0032 (~8 quadros ≈ 350 ms) e **se dissolve em cinza translúcido em 2 quadros** (q0033–q0034).
- **"FIGHHHHT!!"** [V04 q0047–q0061]:
  - q0047–q0048: um **"F" gigante translúcido laranja** vem da câmera, maior que a tela.
  - q0049–q0052: as letras se espalham e encaixam.
  - q0053–q0059: o texto completo fica parado (~7 quadros ≈ 300 ms), em **degradê amarelo (em cima) e laranja (embaixo)** com contorno preto. Ocupa x ~5–95%, com letras de ~25–33% da altura.
  - q0060–q0061: some translúcido. **O jogador já tem controle em q0058**, com o FIGHT ainda na tela.

**Tipografia:** toda letra de evento usa fonte de **balão de quadrinho** (itálica e grossa, letras de tamanhos diferentes, contorno escuro). Os nomes no HUD usam fonte sem serifa condensada e grossa em branco.

**Quanto o HUD chama atenção:**
- As **barras** são finas e de cor viva.
- O **emblema do tempo** é o ponto mais chamativo.
- Os **textos de evento são grandes e chamativos de propósito**, mas ficam nas laterais (y 20–50%, x < 32% ou > 70%), longe do centro da luta.

**Princípios K**
1. **Textos onomatopeicos de gibi** (FIRST ATTACK!, AIR COMBO!, YES!, GOOD!) com **entrada em zoom exagerado** (~2× para 1× em ~3 quadros) e saída rápida.
2. **Cor por tipo de mensagem:** contador vermelho/laranja (quente), elogio azul/ciano (frio), aviso rosa/magenta. Sempre com contorno escuro.
3. Contador de golpes **do lado do atacante**, no alto das laterais, nunca em cima da luta.
4. Barras **finas e inclinadas** ancoradas no centro, com **vermelho** marcando o dano recente.
5. READY/FIGHT montados **letra a letra** com overshoot, e o controle volta **antes** do texto sair.

---

## L. SOM

**As gravações têm áudio:** AAC mono, 44,1 kHz, ~9,9 s cada. O áudio começa 0,046 s depois do vídeo (~1 quadro). A sincronia de gravação de tela pode ter alguns quadros de atraso.

**Nível:** há som contínuo do começo ao fim, entre −15 e −25 dB RMS (mediana −19 dB em V04 e −18 dB em V05), compatível com música de fundo constante.

**Picos de volume (RMS em janelas de 1/24 s):**
- **V04 q0048–q0050:** **−11 dB**, o maior do V04, exatamente quando o "FIGHT" entra na tela. Provável locução de "FIGHT" ou o som da entrada (não identifiquei se é voz).
- **V04 q0152–q0155:** **−12 dB** na **ativação do Hyper** do Wolverine. E **q0168: −13 dB** na entrada da cinemática.
- **V04 q0080–q0093:** −15 a −17 dB durante os acertos aéreos.
- **V05 q0165–q0168:** **−14 dB** na transição para a cinemática do Hyper do Ryu.
- **V05 q0140–q0141:** vale de **−31 dB**, um silêncio relativo logo antes do "GOOD!".

**Inícios de som fortes (fluxo espectral):**
- **V04:** q0004, q0006, q0038, **q0085** (≈ primeiro acerto, q0082), q0141, q0150, **q0188** (mergulho do Hyper), q0206, q0221.
- **V05:** q0042, q0065 (defesa), **q0102 e q0105** (acertos de garra em q0098 e q0103, o que sugere ~3–4 quadros de atraso entre áudio e vídeo na gravação), q0154.

**Voz:** não dá para afirmar só pela análise numérica. Não invento falas.

**Princípios L**
1. Pico de volume na **ativação do Super** e na **entrada do FIGHT**: os dois momentos mais altos.
2. Um **pequeno vazio sonoro** antes de um texto de elogio (−31 dB antes do "GOOD!") faz o texto "soar".
3. Música contínua em volume moderado (~−19 dB) para os golpes ficarem acima dela.

---

## M. FLUXO DO ROUND (só a abertura aparece)

| Fase | Quadros (V04) | Duração (≈) | Observações |
|---|---|---|---|
| Diálogo de intro (close nos times, legenda) | q0001–q0008 | 8 quadros ≈ 350 ms (cortado, já começa no meio) | Legenda "Interesting... You fight like a wild animal!". |
| HUD entrando | q0009–q0011 | 3 quadros ≈ 130 ms | Barras e emblema surgem com os parceiros ainda na tela. |
| Parceiros saem e "READY" se monta | q0011–q0025 | 15 quadros ≈ 650 ms | Letras voando. |
| "READY" parado | q0025–q0032 | 8 quadros ≈ 350 ms | Lutadores em guarda. |
| "READY" se dissolve | q0033–q0034 | 2 quadros ≈ 85 ms | Fade em cinza. |
| Vazio (só lutadores) | q0035–q0046 | 12 quadros ≈ 520 ms | Idle. |
| "FIGHT" entra | q0047–q0052 | 6 quadros ≈ 260 ms | "F" gigante, letras se encaixam. |
| "FIGHT" parado | q0053–q0059 | 7 quadros ≈ 300 ms | **Controle livre em q0058.** |
| "FIGHT" sai | q0060–q0061 | 2 quadros ≈ 85 ms | Translúcido. |

Do fim do diálogo até o controle: **≈ 49 quadros ≈ 2,1 s**. KO e vitória não aparecem.

**Princípios M**
1. Intro curta, com os personagens no cenário e a fala legendada, e já emendando no HUD.
2. READY e FIGHT **montados letra a letra**, ~0,3–0,5 s parados cada.
3. Devolver o controle **antes** do "FIGHT" sumir.
4. Personagens extras do intro saem **pulando para fora** da tela: a transição faz parte da animação, não é um corte seco.

---

## O QUE ESTE JOGO FAZ QUE MAIS O FAZ PARECER PROFISSIONAL (ordem de impacto)

1. **Faíscas de acerto gigantes e chapadas** (~40–50% da altura da tela), que aparecem no tamanho máximo no quadro do contato e se transformam em rastro e arranhão ao sair da pausa [V05 q0098–q0102, q0113–q0117; V04 q0094].
2. **Hyper com o mundo trocado**:
   - flash localizado no lutador;
   - cenário −70–80% em 1–3 quadros;
   - congelamento;
   - close;
   - **fundo do cenário substituído por raios azuis ou estrela escura**, para o efeito poder ser enorme;
   - saída por tela branca, cinza e cacos.

   [V04 q0155–q0220; V05 q0158–q0234]
3. **Textos de quadrinho animados** (READYYYY!, FIGHHHHT!!, FIRST ATTACK!, AIR COMBO!, YES!, GOOD!) com entrada em zoom exagerado e cor por tipo de mensagem [V04 q0013–q0059, q0083–q0110; V05 q0140–q0143].
4. **Render de gibi**: contorno preto de 2–3 px, sombra chapada de borda dura e **hachura de nanquim** nas sombras [V05 q0145; V04 q0040].
5. **Hitstop que cresce com a força e ao longo do combo** (≈ 5 → 10 → 12 quadros a 60 Hz), com o mundo inteiro congelado [V05 q0098–q0099, q0113–q0116, q0123–q0127].
6. **Faísca de defesa própria**: um muro de cacos azul-ciano do tamanho do corpo, que lê "bloqueou" na hora e contrasta com o laranja e vermelho do acerto [V05 q0059–q0062].
7. **Hierarquia por cor**: fundo de um matiz só (verde-água, S ~27–43%) contra lutadores saturados em matizes opostos (S ~63%) e chão mais escuro (V 22%) [V04 q0040].
8. **Câmera que sobe no combo aéreo**, com paralaxe 3D forte (perto ~5× o longe), e fica parada quando não precisa [V04 q0094, q0118, q0057–q0075].
9. **Rastros coloridos de arma** (3 faixas roxo/magenta das garras) e anéis no golpe giratório: a trajetória fica desenhada na tela [V05 q0070–q0080, q0003–q0011].
10. **Poeira cartoon branca** em toda saída e chegada (dash, pulo, pouso, queda), que vira fumaça cinza [V04 q0059–q0066, q0078–q0080; V05 q0025–q0033].
