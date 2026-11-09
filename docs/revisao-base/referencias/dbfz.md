# Referência: Dragon Ball FighterZ (Goku Ultra Instinto × Goku SSJ4 DAIMA)

Material analisado:
- **V02**: `55148a1e-Screenrecorder-2026-11-08-16-59-00-321.mp4`, 15,2 s, 364 quadros nativos (≈24 q/s), extraídos como 367 quadros em `vref/v02/` (há 3 buracos na gravação, de ≈84 ms cada, que viraram quadros repetidos: q0005=q0004, q0007=q0006, q0202=q0201).
- **V03**: `17b4048b-Screenrecorder-2026-11-08-16-59-56-965.mp4`, 11,0 s, 262 quadros, sem buracos.
- Os dois arquivos estão em 2400×1080. A **área de jogo** é o retângulo 1920×1080 que começa em x=240 (barras pretas de 240 px de cada lado). Todas as medidas abaixo estão em % dessa área. Nos quadros de 1200 px, ela vai de x=120 a x=1080.
- Conversão: 1 quadro da gravação ≈ 41,7 ms ≈ 2,5 quadros do jogo a 60 Hz. Erro típico de qualquer duração: **±1 quadro da gravação (±2,5 q@60)**. O vídeo do YouTube por trás deve ser de 60 q/s, e a gravação a 24 q/s perde quadros: um evento de 1–2 quadros do jogo pode não aparecer.
- Em V02 q0322–q0364 a interface do YouTube cobre a tela (título, botões, barra de progresso). Nessa faixa só uso o que aparece por baixo dela.
- Os dois vídeos têm legenda em japonês numa caixa preta (V02 q0001–q0004, V03 q0001–q0067) e "[叫び声]" (V03 q0248+). Pelo botão CC visível, deve ser legenda do YouTube. Ignorei.
- **Não aparece**: tela de KO, vitória, timer contando (é ∞), pulo neutro completo, agachar, defesa clara no chão.

Folhas montadas (em `img/`):
| arquivo | conteúdo |
|---|---|
| `dbfz_medidas_grade.jpg` | grade de 5 % sobre V02 q0001 e q0065 (alturas, chão, HUD) |
| `dbfz_acerto1_faisca.jpg` | V02 q0184–q0191, 1º acerto: faísca, pausa, rastro |
| `dbfz_acerto2_3_faisca.jpg` | V02 q0192–q0203: chute, faísca que se desfaz, empurrão |
| `dbfz_lancador_close_tremor.jpg` | V02 q0204–q0227: lançador, close de cinema com tremor, volta ao plano aberto |
| `dbfz_meteoro_v02.jpg` | V02 q0256–q0279: golpe multi-acerto com explosões laranja |
| `dbfz_super_v03_inicio.jpg` | V03 q0092–q0115: início do Super (escurece, vento, corte) |
| `dbfz_super_v03_chaves.jpg` | V03 q0100–q0196: quadros-chave do Super até a volta ao jogo |
| `dbfz_esquiva_escurece.jpg` | V02 q0076–q0099: evento de esquiva com fundo escurecido |
| `dbfz_contra_v03.jpg` | V03 q0024–q0055: contra-ataque, "¡Contraataque!", rastros azuis |
| `dbfz_idle_dash_ssj4.jpg` | V02 q0062–q0077: idle e dash do SSJ4 (recorte em resolução cheia) |
| `dbfz_andar_chute_ssj4.jpg` | V02 q0151–q0166: andar e chute do SSJ4 |
| `dbfz_contorno.jpg` | V02 q0065 a 2×: contorno e cel-shading das cabeças |
| `dbfz_poeira.jpg` | poeira de dash, pouso, queda e nuvem cheia (V02/V03) |
| `dbfz_textos_evento.jpg` | contador "N golpes", "¡Primer ataque!", "¡Cázalo!", "¡Genial!", "¡Bien!" |
| `dbfz_hud.jpg` | faixas de cima e de baixo do HUD em vários momentos |
| `dbfz_fim_v02_youtube.jpg` | V02 q0341–q0364: projétil de ki e nuvem de poeira (com a interface do YouTube) |

---

## A. TELA E CÂMERA

**Altura dos personagens em pé**, na câmera de neutro [V02 q0001, q0065, q0099]:
- Goku UI, ereto: cabelo 26 % → sola 92 % = **≈66 % da altura da área de jogo**. Sem as pontas do cabelo (testa → pé), ≈59 %.
- Goku SSJ4, em guarda de base larga e agachada: cabelo 33 % → sola 91 % = **≈58 %**.
- Os dois ocupam mais da metade da altura da tela. É bem mais do que em SF2 (≈40 %).

**Distância inicial**: centro do UI em x≈24 % e do SSJ4 em x≈72 %, ou seja **≈48 % da largura** entre os centros [V02 q0062]. Os corpos ficam a ≈30 % um do outro.

**Linha do chão**: sola em **91–92 % de cima para baixo** [V02 q0065, q0099]. As barras de ki de baixo (90–95 %) passam por cima dos pés. O HUD de baixo cobre a base dos personagens, e o jogo aceita isso.

**Zoom × distância**: com os dois a 48 % de distância [V02 q0001] o UI mede 66 %. A 28 % [V02 q0183], mede 64 %. **No neutro o zoom praticamente não muda (≤3 %)**. A câmera só faz pan horizontal. O zoom forte fica reservado para o cinema (abaixo).

**Câmera vertical**:
- No combo aéreo [V02 q0228–q0256] a câmera sobe junto e o chão sai de quadro.
- Na queda do SSJ4 [V02 q0290–q0298], a câmera desce acompanhando. O fundo se desloca 20–40 px (a 1080p) por quadro da gravação na vertical (medido por correlação de blocos).

**Tremor de tela**:
- Golpes normais no chão: **nenhum tremor de câmera**. Um bloco do fundo mexe 0–2 px entre q0194 e q0204 [V02], o que é ruído.
- Close do lançador [V02 q0213–q0218]: tremor **forte, em diagonal, alternando a direção a cada quadro da gravação**. Deslocamentos medidos (px a 1080p): (+0,+9), (−46,−18), (−5,−10), (+44,−22), (+6,+46), (−19,−36). Amplitude ≈46 px ≈ **2,4 % da largura e 4 % da altura**. Dura 5–6 quadros da gravação (≈210–250 ms ≈ 13–15 q@60, ±2,5). Depois vêm **3 quadros totalmente parados** (q0219–q0221, deslocamento 0) e o corte para o plano aberto (q0222).
- Super de V03: tremor contínuo durante a explosão [V03 q0133–q0157]. Não medi isolado por causa da quantidade de partículas.

**Câmera de cinema / close** (detalhes na seção G):
- Lançador [V02 q0212–q0221]: corte seco para um plano **≈1,7× mais próximo**. A cabeça do UI passa de ≈9 % para ≈15 % da largura. O enquadramento é diagonal, com os dois lutadores cortados pelas bordas e um céu azul claro com nuvens atrás. Dura 10 quadros da gravação (≈420 ms ≈ 25 q@60).
- Chute do SSJ4 [V03 q0236–q0244]: câmera baixa e inclinada (horizonte em ≈20–25°). A perna/bota do SSJ4 domina o primeiro plano, com pedras voando. Dura ≈9 quadros (≈375 ms).
- Super de V03: vários cortes, câmera inclinada, um plano do cenário vazio e um plano aberto de cima da explosão (seção G).

**HUD**:
- Em cima ocupa **0–9 %** (retratos + barras de vida entre y 3–8 %), mais ícones laranja de assist/ki em 11–14 %.
- Embaixo ocupa **84–97 %** (números de nível de ki e barras de ki).
- A faixa livre de interface vai de ≈14 % a ≈84 % da altura [V02 q0065 com grade].

---

## B. CENÁRIO

**Palco**: cânion de rochas laranja (no jogo, "Planície Rochosa"/Wasteland [CONHECIMENTO]). É um cenário 3D com pintura pictórica, não pixel art [V02 q0001].

**Camadas percebidas**:
1. Céu azul-claro com nuvens brancas (V≈93 %, S≈38 %).
2. Mesas/torres de rocha distantes, desbotadas e azuladas pela perspectiva aérea.
3. Rochas médias laranja-marrom, em estratos horizontais (cogumelos de pedra a 50–60 % da largura).
4. Chão de lajes rachadas.
5. Pilar de rocha escuro e grande na borda esquerda, como primeiro plano lateral.

**Paralaxe**: na queda [V02 q0292–q0298], um bloco à esquerda (x 3–13 %) andou +25 px enquanto um bloco do meio (x 60–75 %) andou +9 px no mesmo quadro. As velocidades são diferentes, então há paralaxe verdadeira, de câmera 3D.

**Hierarquia lutador × fundo** (medido em V02 q0065, resolução cheia, HSV):
| região | brilho V | saturação S | densidade de bordas* |
|---|---|---|---|
| fundo entre os lutadores | 59 % | 63 % | 4,0 |
| rocha à esquerda | 59 % | 66 % | 4,2 |
| rocha à direita | 44 % | 74 % | 4,5 |
| chão da frente | 60 % | 84 % | 4,6 |
| SSJ4: calça | 78 % | 81 % | 9,2 |
| SSJ4: tronco/pelo | 63 % | 61 % | 12,6 |
| UI: tronco | 74 % | 31 % | 14,9 |
| UI: calça | 69 % | 43 % | 6,2 |

\*média do filtro de bordas: quanto mais alto, mais linhas/detalhe.

Leitura:
- O fundo **não é mais escuro nem muito menos saturado**: é laranja saturado.
- O lutador se destaca por **(a) ≈3× mais linhas/bordas** (contorno escuro e linhas internas, que o fundo não tem) e **(b) contraste de matiz**: UI branco-azulado frio sobre laranja, SSJ4 magenta + amarelo + azul sobre laranja.
- O fundo tem traço suave, sem contorno escuro (as linhas da rocha são marrom-escuras e borradas) e um leve desfoque de profundidade nas rochas distantes.

**Detalhe na faixa da luta × resto**: o chão perto dos pés tem as rachaduras mais nítidas e as rochas distantes são borradas. O céu é quase liso (densidade de bordas 1,0).

**Elementos animados**:
- Nada vivo (sem público).
- O cenário reage aos golpes com efeitos: pedras levitando e rachaduras rosa no Super [V03 q0136–q0157] e nuvens de poeira [V02 q0301–q0363].

**Chão**:
- Lajes de pedra com rachaduras, sem reflexo.
- **Sombra dos personagens dura e quase preta** (#0C0C0C, ~80–90 % de opacidade), **no formato da silhueta projetada**, curta, para trás e para baixo dos pés [V02 q0099, q0183].
- Não é uma elipse genérica.

**Luz do cenário nos personagens**:
- No neutro a luz é chapada, de frente.
- Nos eventos o cenário inteiro muda e os lutadores junto:
  - Esquiva do UI: o fundo cai de V 57 % para 28 % em 3 quadros e o SSJ4 fica rosado e lavado [V02 q0078–q0080].
  - Super: tudo vermelho/rosa [V03 q0139–q0157] e depois branco [V03 q0172–q0181].

**Paleta**: laranja queimado / terracota (rocha) + azul-claro (céu). Os personagens usam as cores complementares: azul, magenta, amarelo-vivo e branco.

---

## C. PERSONAGENS (RENDER)

Ver `dbfz_contorno.jpg`.

**Contorno**:
- Existe e é de **espessura variável**. Medindo as corridas de pixels escuros em V02 q0065 (1080p), a moda está em **2–3 px**. Há trechos de 4–6 px nas partes sombreadas (sob o cabelo, axila, dobra da calça) e 1 px nas linhas internas finas (músculos, dedos).
- **A cor da linha não é preta pura: é tingida pela cor local**:
  - linha média do SSJ4: RGB(75,18,21), um vinho-escuro;
  - linha média do UI: RGB(27,26,66), um azul-marinho.
- Linha mais grossa na silhueta externa e mais fina dentro.

**Sombreamento**:
- **Cel-shading de 2 tons com sombra de borda dura** (luz + sombra), mais um 3º tom escuro só nas dobras mais fundas (calça amarela do SSJ4: amarelo-vivo / laranja / dobra marrom) [V02 q0065, q0099].
- Nenhum degradê suave no corpo.

**Contraluz / borda iluminada**:
- O UI ganha uma borda azul-ciano da aura (a coluna de chama azul atrás dele ilumina as bordas) [V02 q0065].
- O SSJ4 não tem borda iluminada no neutro.

**Silhueta**:
1. SSJ4: juba magenta longa e espetada, larga no topo, + rabo magenta curvo + calça amarela larga e base aberta. Forma de triângulo com a ponta para baixo e muito volume. Dá para ler pela sombra.
2. UI: cabelo prateado espetado para cima + torso nu e estreito + calça larga. Forma em "Y" ereta, envolta numa coluna de aura azul.

Mesmo sendo o mesmo personagem, as silhuetas e as paletas são opostas (quente/largo × frio/vertical).

**Proporções**: heroicas, com a cabeça ≈1/7 da altura total. Mãos e pés grandes, ombros largos.

**Taxa de animação (animação limitada)**:
- A captura de 24 q/s sobre um vídeo de 60 q/s não deixa contar com precisão, mas dá para ver poses seguradas:
  - idle do SSJ4: pose idêntica entre q0002→q0003 e q0064→q0065 (diferença de 0,7 e 1 na escala 0–255 contra 11–24 nas trocas) [V02];
  - preparação de pulo: q0153→q0154 (diferença 1,6) [V02];
  - pose de impacto congelada de q0196 a q0198 [V02].
- Isso indica poses seguradas por **≥2 quadros da gravação (≥83 ms ≈ ≥5 q@60)**, ou seja, **≈10–12 poses distintas por segundo no idle**.
- Nos golpes, quase todo quadro da gravação traz uma pose nova (≥12–24 poses/s visíveis) até o quadro de impacto, que fica segurado.
- [CONHECIMENTO] A Arc System Works anima os modelos 3D "em 2s/3s", sem interpolação entre poses-chave, para imitar anime desenhado. Os efeitos (aura, fogo, faíscas) correm a 60 q/s e os corpos não. É por isso que a aura do UI muda a cada quadro e o corpo fica parado.

---

## D. ANIMAÇÃO POR ESTADO

**Idle SSJ4** [V02 q0062–q0066, V03 q0221–q0230]:
- Base muito larga, joelhos dobrados, mãos abertas em garra na altura do peito, rabo enrolado atrás.
- A respiração é quase imperceptível (deslocamento do tronco ≤1 px entre quadros da gravação).
- As poses se seguram por 2 quadros.
- O rabo é o movimento secundário mais visível.
- Rosto sério, olhos semicerrados.

**Idle UI** [V02 q0001–q0004, q0099]:
- Ereto, braços soltos ao lado do corpo, peso parado.
- Quase nenhuma mudança de pose (diferenças de 4–13 vêm da aura).
- Movimento secundário: **coluna de aura azul-ciano** (do chão até acima da cabeça, ≈1,5× a largura do corpo) que tremula a cada quadro + **partículas azuis** subindo [V02 q0065, V03 q0010].
- Mesmo parado, o personagem nunca parece congelado, porque o efeito se mexe.

**Andar para frente SSJ4** [V02 q0151–q0158]: passos curtos e pesados, mantendo a base larga, ombros balançando. São ≈8 quadros da gravação por ciclo visível, mas a sequência é curta demais para medir.

**Dash SSJ4** [V02 q0067–q0077], ver `dbfz_idle_dash_ssj4.jpg`:
1. **1 quadro de antecipação** (q0067): tronco inclinado à frente, braço de trás puxado.
2. A partir de q0068, pose de corrida **quase horizontal**: tronco a ≈30° do chão, pernas abertas, punhos à frente, rabo esticado para trás.
3. A pose se segura e o corpo só translada (q0068–q0077 ≈ 10 quadros ≈ 420 ms).
4. **Leque de poeira 2D laranja** atrás do pé que impulsiona (q0068–q0072, ≈10 % da altura).

**Hop / preparação** [V02 q0151–q0153]: o corpo sobe com os joelhos recolhidos (q0151) e volta à base (q0153–q0154, segurado).

**Queda e chão**:
- SSJ4 arremessado [V02 q0290–q0300]: cai girando de costas e bate no chão com uma nuvem de poeira grande (q0301–q0311). Fica deitado até ≈q0316 (≈0,6 s) e volta à guarda em ≈q0318.
- UI no Super [V03 q0194–q0210]: aparece deitado/sentado no chão com poeira em leque e volta de pé em ≈q0209.

**Intro**: em [V02 q0001–q0004] os dois já estão em guarda, com o fim da fala (legenda), e em seguida vem o anúncio (seção M).

**Vitória, agachar, defesa no chão**: não aparecem.

---

## E. GOLPES NORMAIS

Ver `dbfz_acerto1_faisca.jpg` e `dbfz_acerto2_3_faisca.jpg`.

**Soco do UI (1º acerto)** [V02 q0183–q0191]:
- **Antecipação** (q0184): tronco gira para trás, ombro do soco recua, base se abre.
- **Impacto** (q0185):
  - braço todo estendido, corpo projetado à frente, pé de trás girado;
  - **rastro desenhado**: 3–4 linhas curvas branco-azuladas seguindo o arco do braço, como um *smear* de anime.
- **Pose segurada** q0185–q0190 (≈5 quadros da gravação ≈ 210 ms ≈ 12 q@60, ±2,5). O rastro continua visível congelado.
- **Recuperação** (q0191): o braço recolhe numa pose só e o UI já está em outra posição.

**Chute do UI (2º acerto)** [V02 q0192–q0199]:
- **Antecipação** (q0193): joelho levantado, tronco virado de costas para o alvo.
- **Impacto** (q0194):
  - perna esticada em linha reta, quadril girado a 90°, braços para o lado oposto como contrapeso;
  - **rastro em faixa** azul-violeta translúcido do tamanho da perna.
- **Pose segurada** q0194–q0198 (≈4–5 quadros ≈ 170–210 ms).
- Volta à guarda em 1 quadro (q0199).

**Chute voador do SSJ4** [V02 q0159–q0166]:
- q0159–q0160: pula com os joelhos recolhidos, braço erguido.
- q0161: extensão, com arco azul de rastro e poeira laranja espetada no chão (q0161–q0166).

**O corpo inteiro participa**: sim, em todos. Quadril gira, ombros vão para o lado oposto ao membro que bate e o pé de apoio gira.

**Exagero**: no quadro de impacto o membro fica mais longo e mais reto do que o natural. As poses são "de pôster", sem meio-termo.

**Diferença fraco/médio/forte**: não dá para saber pelo vídeo quais botões foram usados. [CONHECIMENTO] Em DBFZ o fraco é rápido e curto com faísca menor, e o forte tem antecipação maior e faísca e pausa maiores.

---

## F. ESPECIAIS E PROJÉTEIS

**Projétil de ki do SSJ4** [V02 q0334–q0349], parcialmente coberto pelo YouTube:
- Esfera amarelo-alaranjada com núcleo claro, **≈4–5 % da largura**, com halo.
- Some e vira um rastro amarelo em arco/serpente (q0345–q0349). Parece ser desviado.

**Investida aérea do UI** [V02 q0228–q0238; V03 q0048–q0055]:
- **Cometa azul-branco**: o corpo vem na frente e atrás fica um rastro em forma de chama azul-ciano com partículas, de **≈30–40 % da largura**.
- No início, uma **explosão branca espetada** (estrela de pontas brancas, contorno cinza, ≈20 % da altura) no ponto de partida [V02 q0223–q0225, q0239–q0240; V03 q0042–q0047], que marca o "teleporte".

**Feixe/estocada horizontal do UI (contra-ataque)** [V03 q0026–q0038]:
- Faixa branca translúcida de **≈60 % da largura**, com explosões amarelas no ponto de contato e raios brancos em zigue-zague (q0035–q0036).

**Arcos de golpe**: crescentes brancos de **≈25–30 % da altura** que marcam o caminho do golpe giratório [V03 q0076–q0077] e um anel ciano-azul [V02 q0259–q0260].

**Anti-aéreo**: não identificado com segurança.

---

## G. SUPERS (APRESENTAÇÃO)

### V03: Super do SSJ4 (cinemático completo, ≈4,2 s)

Ver `dbfz_super_v03_inicio.jpg` e `dbfz_super_v03_chaves.jpg`. Brilho médio da tela inteira (V) entre parênteses.

| quadros | duração | o que acontece |
|---|---|---|
| q0093–q0095 | 3 q ≈ 125 ms | **Fundo escurece**: a rocha à direita vai de V 73 % → 36 % (−50 %). Câmera aproxima um pouco. O SSJ4 se agacha. |
| q0096–q0109 | 14 q ≈ 580 ms | **Carga**: espirais de vento branco-cinza em volta do SSJ4 (≈80 % da altura), fundo **dessaturado** (S 86 % → 32 %), detritos de rocha subindo. A pose quase não muda, o efeito gira. |
| q0110–q0117 | 8 q ≈ 330 ms | **Corte para o cenário vazio** (V 20–27 %, escuro) com linhas verticais brancas caindo: ele sumiu. Pausa dramática. |
| q0118–q0124 | 7 q ≈ 290 ms | **Reaparece em close inclinado** dentro de uma explosão de aura amarela (centro V 90 %). **Estrela ciano em X** (brilho de lente) em q0121–q0124. |
| q0125–q0132 | 8 q ≈ 330 ms | Mergulha em diagonal com um arco/feixe azul-ciano. **Fundo vai a preto** nas bordas (V 25 %). Só ele e o fogo amarelo ficam visíveis. |
| q0133 | 1 q ≈ 42 ms | **Flash branco** de tela inteira (V 92 %, S 14 %) com raios pretos radiais. |
| q0134–q0138 | 5 q ≈ 210 ms | **Domo de explosão** laranja-amarelo com textura de lava, anel horizontal, pedras pretas voando. Câmera abre para um plano geral. |
| q0139–q0157 | 19 q ≈ 790 ms | **Cenário tingido de vermelho**: rachaduras magenta brilhantes no chão, **colunas de luz vermelho-brancas** brotando e engrossando (de 1 % a 5 % da largura cada), pedras flutuando. Os lutadores ficam pequenos (≈10 % da altura) no centro. |
| q0158–q0176 | 19 q ≈ 790 ms | **Fade para rosa e depois branco** (V 81 % → 99 %). As colunas preenchem tudo. |
| q0177–q0181 | 5 q ≈ 210 ms | **Tela branca pura** (V 100 %, S 0–1 %). Só o HUD de baixo continua. |
| q0182–q0193 | 12 q ≈ 500 ms | Fade de volta: imagem rosada com nuvem de fumaça. O UI cai do alto com aura azul e o SSJ4 aparece de pé. |
| q0194 | corte | Cores normais, mas o **fundo coberto por uma nuvem de fumaça escura** (V 22–25 % em volta). O SSJ4 em guarda, o UI no chão com poeira. A nuvem se dissipa até ≈q0211. |

Resumo:
- Do escurecer até o golpe sair: q0093 → q0118 (reaparição) ≈ **1,0 s**.
- Até o impacto: q0133 ≈ **1,7 s**.
- A apresentação inteira: ≈4,2 s.
- O HUD fica na tela o tempo todo. As barras de cima esmaecem no branco.
- O contador "4 golpes" continua visível e sobe para "5" em q0136.
- Final: pausa longa → **tela branca de 5 quadros** → fade → fumaça escura. Não há câmera lenta explícita.

### V02: golpe multi-acerto do UI (estilo Super / "meteoro") [V02 q0259–q0300]

Ver `dbfz_meteoro_v02.jpg`.

- **q0259–q0260**: arco/esfera **ciano** em volta do chute giratório do UI.
- **q0260–q0270**: o centro da tela vira uma **explosão contínua de chamas laranja-amarelas espetadas**, e cada quadro traz labaredas novas. O contador sobe rápido: 8 → 9 → 10 → … → 14 golpes, ≈1 golpe por quadro da gravação. Os personagens ficam visíveis por baixo das chamas.
- **q0271–q0273**: clarão branco com raios elétricos azul-claros e um **feixe diagonal ciano** cruzando a tela inteira (canto inferior esquerdo → canto superior direito).
- **q0274–q0280**: o SSJ4 é jogado para cima e para a direita, com a faísca laranja. Contador em 15.
- **q0281–q0289**: corte para uma **explosão branca/amarela** de tela quase inteira, com relâmpagos, mais o feixe ciano diagonal e um anel brilhante.
- **q0290–q0300**: queda do SSJ4 com a câmera descendo. Pouso com nuvem de poeira. Contador final **16 golpes**. "¡Genial!" aparece em ≈q0314.

### Close de cinema do lançador [V02 q0211–q0222]

Ver `dbfz_lancador_close_tremor.jpg`.

1. q0211: plano aberto, **faísca grande** e raios cruzando a tela.
2. q0212: **corte seco** para um close ≈1,7×: o UI de baixo para cima chutando e o SSJ4 no ar, os dois cortados pela moldura. O fundo vira céu azul-claro com nuvens (V alto) e partículas amarelas.
3. q0212–q0213: faísca enorme (núcleo amarelo-claro + espinhos vermelhos) em ≈20 % da tela.
4. q0214–q0218: **tremor diagonal de ±46 px**. A faísca vira uma chama laranja.
5. q0219–q0221: **imagem 100 % parada** (3 quadros ≈ 125 ms).
6. q0222: corte de volta ao plano aberto. O SSJ4 já está no alto à direita e o UI embaixo à esquerda.

Em quadros de jogo, o close todo dura ≈25 q@60 (±2,5).

---

## H. REAÇÃO A ACERTO

**Dano alto (soco no rosto)** [V02 q0185–q0191]: a cabeça do SSJ4 vai para trás e para cima, o tronco arqueia e os ombros sobem. A pose se segura durante a pausa.

**Dano no meio (chute na barriga)** [V02 q0194–q0199]: o tronco dobra para a frente sobre o pé que bate, os braços se abrem para trás e a cabeça cai. Em q0199–q0203 ele volta à guarda.

**Tremor do atingido durante a pausa**: **mínimo**. Deslocamentos medidos no SSJ4 durante q0186–q0190: (−1,−1), (0,0), (0,0), (+1,−2), (0,0) px a 1080p, ou seja ≤2 px ≈ 0,1 % da largura. O DBFZ quase não sacode o atingido nos golpes de chão. A sensação vem da faísca enorme e da pausa.

**Recuo no acerto**: depois da pausa do chute, o SSJ4 escorrega +31, +31 e +17 px nos três quadros seguintes (q0199–q0201) e para em q0202. São **≈79 px ≈ 4 % da largura em ≈125 ms**, com desaceleração.

**Recuo na defesa**: não medido. Não há defesa clara no chão.

**Lançado e queda** [V02 q0274–q0300]: corpo girando no ar, de costas e com os membros abertos. Pouso de costas com **nuvem de poeira grande** (até ≈45 % da largura). Fica deitado ≈0,6 s e levanta [V02 q0300–q0318].

**Cor no atingido**: **não vi piscar branco nem vermelho** no corpo do atingido nos acertos medidos. A faísca cobre o ponto de contato, e o "brilho" vem dela.

**Atingido como "fantasma"**: durante a esquiva do UI [V02 q0080–q0094, q0110–q0150] o SSJ4 fica **lavado em rosa-claro e semitransparente**, com o soco congelado e um rastro rosa-branco. É um estado visual de "golpe que falhou".

---

## I. IMPACTO

**Pausa no acerto (hitstop)**, medida pelo congelamento das poses:
| golpe | quadros da gravação | ≈ ms | ≈ q@60 |
|---|---|---|---|
| soco do UI (1º acerto) [V02 q0185–q0190] | 5 | 210 | 12 (±2,5) |
| chute do UI [V02 q0194–q0198] | 4–5 | 170–210 | 10–12 |
| lançador + close [V02 q0212–q0221] | 10 | 420 | 25 |
| impacto do Super [V03 q0133–q0138] | flash 1 + domo 5 | ≈250 | ≈15 |

[CONHECIMENTO] O primeiro acerto de um combo em DBFZ costuma ter pausa maior. Os 12 q medidos são compatíveis com um golpe forte.

**Na defesa**: não observado.

**Tremor de tela**: zero nos normais de chão e forte só no close e no Super (seção A). A hierarquia é: **faísca + pausa** para golpes comuns e **corte + tremor** para golpes de cinema.

**Contra-ataque** [V03 q0026–q0047]:
- texto "**¡Contraataque!**" amarelo na pilha de eventos à esquerda (q0029, q0047);
- faíscas maiores, **com raios elétricos brancos em zigue-zague** (q0035–q0036);
- feixe branco atravessando a tela.

Áudio: pico de −13 dB no quadro do contra (q0032), contra uma mediana de −18 dB.

**Câmera lenta**: não identificada. O jogo usa **pausa total + corte**, não câmera lenta.

**Ordem quadro a quadro de um golpe forte que acerta** [V02 q0184–q0191]:
1. **q0184**: antecipação. Nenhum efeito ainda.
2. **q0185**: contato e **faísca no tamanho máximo no mesmo quadro**: núcleo amarelo-claro, espinhos vermelho-laranja e raios longos cruzando ≈85 % da largura. O rastro do braço aparece. **Os dois congelam.**
3. **q0186**: a faísca começa a se quebrar: **pontos escuros (detritos marrom-pretos) no centro**, espinhos mais finos e raios ainda presentes. Atacante e atingido continuam parados (o atingido treme 1 px).
4. **q0187–q0188**: os raios somem e a faísca vira uma **chama laranja menor** atrás da cabeça do atingido. A pausa continua.
5. **q0189–q0190**: resto de chama laranja e faíscas pequenas.
6. **q0191**: fim da pausa. O UI recolhe o braço e o SSJ4 é empurrado.
7. A faísca some antes ou junto com o fim da pausa. Ela **nunca dura mais que a pausa**.

---

## J. EFEITOS VISUAIS

**Faísca de acerto** [V02 q0185, q0194, q0205, q0212; V03 q0026, q0060]:
- **Forma**: estrela de **8–12 espinhos irregulares** com contorno vermelho e preenchimento amarelo → laranja, mais um **núcleo circular** branco-amarelado (#FCFCB4 → #FCE46C), mais **raios retos muito longos**, afilados nas duas pontas, em amarelo-claro e laranja. Os raios atravessam **≈80–85 % da largura** em diagonais variadas.
- **Estilo**: 2D desenhado, com cores chapadas e bordas duras e um brilho aditivo por cima. Não parece partícula.
- **Tamanho**: núcleo + espinhos ≈ **40–60 % da altura do personagem** (≈25–35 % da altura da tela). No close do lançador, ≈20 % da área da tela.
- **Duração**: forma cheia em **1–2 quadros da gravação**. Fica menor e laranja por mais 2–3. Total **≈4–6 quadros ≈ 170–250 ms**.
- **Evolução**: **começa no tamanho máximo e encolhe/se fragmenta**. Não cresce. No 2º–3º quadro aparecem pontos pretos (detritos) e linhas finas de lente cruzadas [V02 q0196–q0198].

**Variações de cor**:
- Golpes do UI: amarelo/laranja/vermelho.
- Arcos azul-ciano no giro [V02 q0259].
- Brancos e elétricos no contra-ataque [V03 q0035].

**Faísca de defesa (provável)**:
- **Anel/escudo verde-água** circular em volta do SSJ4 no ar, ≈35 % da altura da tela [V03 q0001–q0003].
- **Bolha transparente com borda branca** [V02 q0314–q0316, V03 q0050–q0053, q0205–q0208].
- Não consegui confirmar que são defesas. [CONHECIMENTO] Em DBFZ a defesa mostra um escudo circular esverdeado.

**Poeira** (`dbfz_poeira.jpg`):
- **2D em cel-shading de 2 tons**: luz laranja-claro (#E48454) e sombra terracota (#9C543C). Sem contorno preto, só a separação dura entre os tons.
- **Leque espetado / "grama de pedra"** no dash e no pouso [V02 q0068–q0072, q0161; V03 q0014–q0017]: triângulos laranja apontados para cima e para trás, ≈10–15 % da altura, com **lascas de pedra marrom-escuras** voando.
- **Nuvem fofa em bolhas** na queda [V02 q0301–q0311], larga (≈40 % da largura) e baixa (≈12 % da altura). Some aos poucos ao longo de ≈10 quadros.
- **Nuvem cheia** que cobre o cenário [V02 q0358–q0363]: em grandes impactos a poeira ocupa a tela toda por ≈6 quadros, com linhas de velocidade por cima.

**Rastros / smears**:
- Linhas curvas paralelas branco-azuladas no soco [V02 q0185].
- Faixa translúcida azul-violeta no chute [V02 q0194].
- Crescente branco nos golpes giratórios [V03 q0076].
- Os rastros **ficam congelados durante a pausa**.

**Linhas de velocidade**: verticais brancas caindo no "sumiço" do Super [V03 q0110–q0117]. Horizontais na nuvem de poeira [V02 q0361–q0363].

**Auras**:
- UI: coluna de chama azul-ciano sempre ligada + partículas azuis.
- SSJ4: aura amarela gigante só no Super [V03 q0118–q0132].

**Efeitos de chão**:
- Rachaduras magenta brilhantes e colunas de luz no Super [V03 q0139–q0157].
- Pedras flutuando [V03 q0136–q0150].

**Escurecimento de fundo para evento**: na esquiva, o fundo cai de V 57 % → 28 % em 3 quadros, fica assim por 14 quadros e volta em 3 [V02 q0078–q0097]. A saturação até sobe (50 → 56 %). Os lutadores continuam claros.

---

## K. HUD E TEXTOS

Ver `dbfz_hud.jpg` e `dbfz_textos_evento.jpg`.

**Barras de vida (topo)**:
- Retrato do personagem em cada canto (x 0–9 % e 91–100 %, y 0–9 %), em pintura de anime com fundo vermelho.
- Nome em cima da barra ("Son Goku (Ultrainstinto)") em fonte de pincel branca/creme, ≈2,5 % da altura.
- Barra inclinada (paralelogramo) de **x 10 % a 43 % (≈33 % da largura) cada**, y 3–8 %, borda metálica clara e pincelada preta decorativa saindo do centro.
- Cores da vida:
  - cheia: **ciano-verde** [V02 q0065];
  - depois do dano: **amarelo**;
  - dano recente: **vermelho** na ponta, que desce depois [V02 q0230];
  - **azul**: vida recuperável (mecânica de tag) [V03 q0010, q0226].
- Centro: **∞** (sem timer) dentro de um medalhão com pinceladas, x 47–53 %, y 2–10 %, mais uma barrinha bicolor vermelho/azul acima.

**Ki / Super (base)**:
- **Número grande de níveis** (0–7) em fonte de pincel, x 3–8 % e 92–97 %, y 84–96 %:
  - prata/branco em 0;
  - **verde em 1** (V03 q0010);
  - **vermelho em 2** (V03 q0092).
- Barra segmentada de x 11 % a 36 % (≈25 % da largura), com divisões a cada ≈¼. Enche em verde.
- Quando sobe de nível, aparece um ícone pequeno com o número (azul "2" em V02 q0296).

**Contador de combo**:
- **Número enorme** com degradê creme → laranja → vermelho, contorno claro e sombra escura, em fonte de pincel inclinada. ≈**12–15 % da altura da tela**. Ao lado, "golpes" em letras pequenas (≈4 % da altura) na linha de base.
- Fica do lado do HUD de quem ataca: UI (P1) em x 15–28 %, y 20–35 % [V02 q0194, q0300]; SSJ4 (P2) à direita, x 75–85 % [V03 q0121].
- **Animação**: o número entra sozinho (q0192: "2") e "golpes" aparece 1–2 quadros depois (q0195).
- **Cada golpe troca o número na hora**. Em V02 q0260–q0270 vai de 8 a 14 em 10 quadros.
- Depois que o combo termina fica parado ≈1 s ("16 golpes" de q0300 a ≈q0322) e some.

**Textos de evento** (pilha à esquerda, ou à direita para o P2):
- "¡Primer ataque!", "¡Golpe!", "¡Cázalo!", "¡Contraataque!", "¡Genial!", "¡Bien!".
- **Amarelo-ouro** com contorno escuro, fonte serifada em itálico, **≈4 % da altura**, levemente inclinada (≈8–10°, descendo para a direita), x 3–18 %, y ≈50–56 %.
- **Entram com uma revelação da esquerda para a direita** em ≈2–3 quadros ("¡Primer at…" em q0186, completo em ≈q0188).
- Empilham até 3 linhas com ≈4,5 % de espaçamento, a mais nova em cima [V02 q0230: ¡Cázalo! / ¡Golpe! / ¡Primer ataque!].
- Duram até o fim do combo (≈1–3 s).
- "¡Genial!" e "¡Bien!" aparecem **depois** do combo, como nota de avaliação [V02 q0314; V03 q0001, q0010].

**Telas ROUND/FIGHT**: "¿PREPARADOS?" e "LUCHAD" (seção M). Não aparece KO.

**Tipografia**: tudo em **fonte de pincel/caligrafia**. O contador é grande e quente, os eventos são pequenos e dourados. O HUD é discreto: barras finas nas bordas e o centro livre. O que chama a atenção é o contador de combo.

---

## L. SOM

Os dois vídeos **têm áudio** (AAC mono 44,1 kHz). Medi o volume RMS por quadro da gravação:
- Nível médio de ≈ −19 dB (V02) e −18 dB (V03), com picos de −12 a −15 dB.
- Picos coincidem com:
  - "LUCHAD" [V02 q0044, −15 dB], provável voz de anúncio;
  - o início do golpe multi-acerto [V02 q0256–q0258, −12 dB];
  - o fim do meteoro [V02 q0279–q0282, −14 dB];
  - os acertos finais [V02 q0350, −12 dB];
  - o contra-ataque [V03 q0032, −13 dB];
  - o chute em close [V03 q0241, −13 dB].
- **Silêncio antes do impacto**:
  - em V03 q0233–q0238 o volume cai para **−37 dB** (quase silêncio, ≈250 ms) e explode em −13 dB em q0241, junto do close;
  - em V02 q0036–q0041 cai para −31 dB antes do "LUCHAD".
- Há música/ambiente contínuo (o volume nunca cai abaixo de −30 dB fora desses buracos).
- Não consigo afirmar o conteúdo das vozes. As legendas japonesas indicam falas em japonês.

---

## M. FLUXO DO ROUND

1. [V02 q0001–q0004]: fim da intro. Os dois em guarda, com a fala do SSJ4 legendada.
2. [V02 q0005–q0044]: **"¿PREPARADOS?"** em letras pretas com contorno dourado, x 23–72 %, y 44–57 %. Atrás, um **emblema circular dourado** (anéis concêntricos com o kanji laranja ao centro, x 25–75 %, y 15–80 %) que aparece entre q0009 e q0013. Dura ≈40 quadros ≈ **1,7 s**.
3. [V02 q0045–q0060]: **"LUCHAD"** gigante em pincel preto com brilho amarelo, x 22–78 %, y 20–62 %. Entra em fusão e dura ≈15 quadros ≈ **0,65 s**. Pico de áudio em q0044.
4. [V02 q0060–q0062]: o texto sai e a luta começa. O SSJ4 dá dash em q0067 (≈0,3 s depois).
5. KO e vitória: **não aparecem** em nenhum dos vídeos. V03 é o meio de um round (os dois com vida e o UI levantando depois do Super).

---

## PRINCÍPIOS POR SEÇÃO (o que copiar num jogo 2D em sprites)

**A. Tela e câmera**
1. Personagens grandes, ≈60–66 % da altura da área de jogo, com o chão a ≈91 % de cima. O HUD pode sobrepor os pés.
2. Zoom fixo no neutro e pan horizontal suave. Zoom é um "evento", não algo contínuo.
3. Tremor de tela só em golpes de cinema e Super: diagonal, alternado, ≈2–4 % da tela, 5–6 quadros da gravação, **seguido de 2–3 quadros totalmente parados**.
4. Close de cinema por **corte seco** (≈1,7×, enquadramento diagonal, ≈400 ms) e volta por corte seco.
5. HUD restrito a ≈10 % em cima e ≈12 % embaixo, com o meio da tela livre.

**B. Cenário**
1. O fundo pode ser saturado e no mesmo brilho dos lutadores, desde que **não tenha contorno escuro** e tenha ≈3× menos linhas.
2. Use contraste de matiz: personagens frios ou de cores complementares sobre um fundo de matiz dominante (laranja).
3. Sombra dura, quase preta, no formato da silhueta, colada aos pés.
4. O cenário deve **reagir a eventos**: escurecer ~50 % em 3 quadros, tingir de vermelho, ficar branco, encher de fumaça.
5. Camadas com paralaxe e rochas distantes desbotadas e azuladas.

**C. Personagens**
1. Contorno de 2–3 px a 1080p, **tingido pela cor local** (vinho, azul-marinho), mais grosso por fora e fino por dentro.
2. Cel-shading de 2 tons com borda dura, 3º tom só nas dobras.
3. Silhuetas opostas e legíveis (largo/quente × vertical/frio).
4. Animação limitada: poses seguradas 2–3 quadros (≈10–12 poses/s no idle) **com efeitos a 60 q/s** por cima.
5. Contraluz colorida vinda da própria aura.

**D. Estados**
1. Idle quase parado, com a vida vindo do movimento secundário (aura, rabo, partículas).
2. Dash com 1 quadro de antecipação + pose segurada quase horizontal + leque de poeira.
3. Queda com poeira grande e ≈0,5 s deitado antes de levantar.
4. Rabo, cabelo e aura como movimento secundário constante.

**E. Golpes normais**
1. Antecipação de 1 quadro claro (corpo torce para o lado oposto).
2. Quadro de impacto exagerado (membro mais longo e reto) **com smear desenhado**.
3. Pose de impacto segurada durante toda a pausa, com o smear congelado junto.
4. Recuperação em 1–2 poses só.
5. O corpo inteiro gira: quadril, ombro oposto, pé de apoio.

**F. Especiais**
1. Investidas como "cometa": o corpo na frente e uma cauda de energia de 30–40 % da largura.
2. Estrela/burst branco no ponto de partida de teleportes e dashes.
3. Projéteis com núcleo claro e halo, que somem em rastro, não num corte seco.
4. Arcos/crescentes brancos marcando a trajetória dos golpes giratórios.

**G. Supers**
1. Abra com **escurecer o fundo ~50 % em 3 quadros** e dessaturar.
2. Pausa de carga (≈0,5 s) com efeito girando e corpo parado.
3. Corte para o cenário vazio (o personagem "sumiu") antes da reaparição.
4. Impacto com **1 quadro de flash branco**, plano aberto da explosão, cenário tingido e **≈5 quadros de tela branca** antes de voltar.
5. Volte ao jogo com fumaça/poeira ainda cobrindo o fundo, para a transição não ser seca.

**H. Reação**
1. Duas poses de dano bem diferentes: cabeça para trás no alto, tronco dobrado no meio.
2. O atingido praticamente não treme (≤2 px). O peso vem da faísca e da pausa.
3. Recuo de ≈4 % da largura **depois** da pausa, em ≈3 quadros com desaceleração.
4. Lançados girando de costas, com pouso em nuvem de poeira.

**I. Impacto**
1. Faísca **no tamanho máximo no mesmo quadro do contato**.
2. Pausa de ≈10–12 q@60 em golpes fortes e ≈25 em lançadores de cinema.
3. A faísca encolhe e se fragmenta durante a pausa e some ao fim dela.
4. Contra-ataque = texto + faísca elétrica + pico de som.
5. Silêncio de ~250 ms antes do maior impacto.

**J. Efeitos**
1. Faísca 2D com núcleo claro, espinhos vermelho-laranja e **raios longos cruzando a tela** por 1–2 quadros.
2. Detritos pretos e linhas de lente no 2º–3º quadro da faísca.
3. Poeira em cel-shading de 2 tons, sem contorno, nas cores do chão. Leque espetado no dash e nuvem fofa na queda.
4. Smears desenhados, não motion blur.
5. Efeitos a 60 q/s enquanto o corpo anima em "2s/3s".

**K. HUD e textos**
1. Contador de combo gigante (12–15 % da altura), quente, em fonte de pincel, mudando a cada acerto.
2. Textos de evento pequenos (4 %), dourados, inclinados, entrando com revelação e empilhando.
3. Nota de avaliação ("¡Genial!") depois do combo.
4. Barras finas e inclinadas nas bordas, com o dano recente em vermelho descendo depois.
5. Toda a tipografia do mesmo estilo (pincel), dando unidade.

**L. Som**
1. Pico de volume exatamente no quadro do impacto ou do anúncio.
2. Buraco de silêncio antes do golpe maior.
3. Voz no anúncio do round.

**M. Fluxo**
1. "Preparados" longo (≈1,7 s) com emblema + "Lutem" curto e enorme (≈0,65 s).
2. A luta começa logo depois que o texto sai.
3. Palavras grandes, centralizadas, sobre o jogo já visível.

---

## O QUE ESTE JOGO FAZ QUE MAIS O FAZ PARECER PROFISSIONAL (por ordem de impacto)

1. **Faísca de acerto enorme, desenhada, no quadro do contato, com raios cruzando a tela e pausa de ≈12 q@60 em que tudo congela** [V02 q0185–q0190].
2. **Close de cinema por corte seco + tremor diagonal forte + 3 quadros parados + corte de volta** no lançador [V02 q0212–q0222].
3. **Super encenado como sequência de anime**: escurece, carga com vento, corte para o cenário vazio, reaparição em close inclinado, flash branco, explosão, cenário vermelho, tela branca e fumaça [V03 q0093–q0194].
4. **Contorno colorido de espessura variável + cel-shading de 2 tons de borda dura**, que fazem o modelo 3D parecer desenho [V02 q0065, `dbfz_contorno.jpg`].
5. **Animação limitada (poses seguradas) com efeitos a 60 q/s**: o corpo tem cara de desenho e a tela continua viva [V02 q0002–q0003, q0064–q0065; aura do UI em todo quadro].
6. **Smears desenhados e congelados na pose de impacto** (linhas no soco, faixa no chute) [V02 q0185, q0194].
7. **O cenário reage aos eventos**: escurece 50 % na esquiva, fica vermelho, branco e cheio de fumaça no Super [V02 q0078–q0097; V03 q0139–q0194].
8. **Contador de combo gigante em pincel, que muda a cada acerto, com textos de evento dourados empilhados e nota no fim** [V02 q0194, q0230, q0300, q0314].
9. **Poeira 2D em cel-shading nas cores do chão** (leque espetado, nuvem fofa, lascas de pedra), integrada ao estilo [V02 q0070, q0301–q0311; V03 q0014].
10. **Lutador separado do fundo por linha e matiz, não por escurecer o fundo**: fundo laranja saturado sem contorno, lutadores frios ou complementares com ≈3× mais linhas, e sombra dura no formato da silhueta [V02 q0065, q0099].
