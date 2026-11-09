# Referência: Fatal Fury: City of the Wolves (COTW), Terry x Terry, South Town Zoo

Material analisado:
- **V06**: gravação de 13,9 s, 337 quadros. Mostra a transição ROUND 1, READY, FIGHT, depois pulo com chute, WILD PUNISH, RUSH, duas quedas e dois COUNTER HIT.
- **V07**: gravação de 9,9 s, 234 quadros. Mostra um chute em pé, REV BLOW (duas vezes), REVERSAL, defesas e pulos no ar.

Notas de medição (valem para o documento todo):
- **Taxa da gravação:** 24,15 quadros/s, ou seja, 1 quadro ≈ 41,4 ms ≈ 2,5 quadros do jogo a 60 Hz. Toda duração dada em "q" tem erro de ±1 quadro (±2,5 quadros do jogo).
- **Área de jogo:** exatamente x = 240 a 2160 e y = 0 a 1080 no vídeo original de 2400×1080, ou seja 1920×1080 (16:9) sem corte em cima ou embaixo. Nos quadros de 1200 px ela vai de x = 120 a 1080. Todas as % e todos os "px a 1080p" se referem a essa área.
- **Interferência na gravação:** no V07, de q0001 a q0062, um aviso do celular ("Gravação de tela salva / Toque para ver") cobre a faixa de cima. Fora da área de jogo há o ícone de inscrição do YouTube.
- **Atenção à velocidade:** cada unidade do timer do jogo dura ≈36 quadros da gravação, ≈1,50 s [V06 q0077→q0114→q0150→q0186; V07 q0072→q0108→q0144→q0181]. Há duas explicações:
  - o "segundo" do timer do COTW dura mais que 1 s;
  - o vídeo do YouTube estava desacelerado.

  A física do pulo (abaixo, seção D) dá números típicos de jogo de luta em velocidade normal, então considero a gravação em velocidade ≈ real. Se ela estiver a 0,67×, divida todas as durações por 1,5.
- **O jogo é 3D:** o COTW usa modelos 3D com acabamento de mangá/quadrinho [CONHECIMENTO]. Onde isso muda a lição para um jogo em sprites, eu aviso.

Folhas de imagem montadas (todas em `img/`):

| Arquivo | Conteúdo |
|---|---|
| `cotw_hud_medidas.jpg` | quadro inteiro anotado com medidas (V06 q0050) |
| `cotw_intro.jpg` | ROUND 1 → READY → FIGHT |
| `cotw_pulo_chute.jpg` | pulo, cambalhota, chute aéreo, faísca, WILD PUNISH |
| `cotw_faisca_pulo_fullres.jpg` | faísca do chute aéreo em resolução cheia (q0091, q0092, q0094, q0096) |
| `cotw_faisca_contra_fullres.jpg` | faísca do COUNTER HIT em resolução cheia (q0248, q0249, q0251, q0253) |
| `cotw_chute_ordem_v07.jpg` | chute em pé quadro a quadro (V07 q0003–q0018) |
| `cotw_defesa_arco.jpg` | arco ciano de defesa (V06 q0223–q0232) |
| `cotw_reacoes_dano.jpg` | poses de dano (agachado, rosto, cabeça jogada para trás) |
| `cotw_queda_levantar.jpg` | giro no ar, queda, tempo no chão, levantar |
| `cotw_rev_blow.jpg` | explosão de linhas verdes + contorno verde + texto REV BLOW + medidor REV subindo |
| `cotw_linhas_radiais.jpg` | explosão de linhas brancas e azuis (V07 q0042–q0045) |
| `cotw_rastros_arco.jpg` | rastro azul de chute girando e rastro de fogo |
| `cotw_textos_evento.jpg` | entrada e saída de S.P.G. IN!, WILD PUNISH, RUSH 2 HITS |
| `cotw_barra_vida_tempo.jpg` | barras de vida ao longo do V06 (dano vermelho que segura e escoa) |
| `cotw_publico_x_lutador.jpg` | render do público × render do lutador (1:1 a 2×) |
| `cotw_publico_animado.jpg` | ciclo do público acenando |

---

## A. TELA E CÂMERA

**Tamanho e posição dos lutadores**
- **Altura em pé:** Terry vai do topo do cabelo (y ≈ 360) à sola da bota (y ≈ 1040), ≈680 px ≈ **63 % da altura da área de jogo** [V06 q0050]. É grande: os dois lutadores mais o espaço entre eles ocupam a largura toda.
- **Linha do chão:** os pés ficam entre 93 % e 97 % de cima para baixo, porque a base é 3D e o pé de trás está mais "fundo" [V06 q0050]. O chão visível é só uma faixa estreita: o corpo inteiro cabe com o pé quase encostando na borda de baixo.
- **Distância inicial:**
  - centro a centro ≈ 840 px ≈ **44 % da largura**;
  - espaço livre entre os corpos (do punho da frente de um ao do outro) ≈ 590 px ≈ 31 %;
  - entre as botas da frente ≈ 420 px ≈ 22 % [V06 q0050].

**Zoom**
- **Não vi zoom pela distância.** O letreiro "ZOO" mede ≈215 px de largura tanto com os lutadores afastados [V06 q0050] quanto colados [V06 q0255].
- A câmera só faz pan horizontal para seguir os lutadores. Exemplo: o fundo anda ≈30 px (a 540 p) ≈ 3 % da largura em ≈16 quadros (0,66 s) [V06 q0300→q0316].

**Câmera no pulo**
- **Sobe, mas pouco.** O fundo desce ≈14 px a 540 p (**≈2,6 % da altura**) em ≈8 quadros, de q0081 a q0089, no pulo alto do P1 [V06 q0081–q0089].
- Quando os dois pulam: ≈17 px (≈3 %) [V07 q0212–q0226]. Em ≈8 quadros depois do pouso ela volta ao lugar.
- O personagem pode **sair pelo topo da tela e passar POR CIMA do HUD**: na cambalhota, o corpo cobre a barra de vida e metade do timer [V06 q0084–q0087] (zoom em `tmp_cotw/ll.jpg`).

**Tremor de tela: muito discreto**
- **COUNTER HIT forte:** deslocamento horizontal alternado de +2, −2, +1 px a 1080 p (≈0,1 % da largura) por 3 quadros (≈125 ms) [V06 q0248–q0250].
- **Chute aéreo de punição:** alternado de ±2 a 3 px por ≈5 quadros (≈0,2 s), somado à descida da câmera [V06 q0091–q0095].
- **Chute em pé de punição:** nenhum tremor mensurável (fundo parado a ±0 px) [V07 q0008–q0016].
- **Conclusão:** o "peso" vem da faísca enorme e da pausa, quase nada do tremor.

**Câmera em Supers:** nenhum Super aparece nos vídeos. Veja G.

**Ocupação do HUD** (ver `cotw_hud_medidas.jpg`)
- **Em cima:** do topo até y ≈ 146, que é a base do círculo do timer, ≈**13,5 % da altura**. As barras laterais terminam em y ≈ 130 (12 %).
- **Embaixo:** só os cantos.
  - Arco do REV mais duas barras S.P. em x 98–345 (P1) e 1575–1822 (P2), y 915–1030: ≈10,6 % da altura × ≈13 % da largura cada.
  - O centro da faixa de baixo fica livre, porque é ali que estão os pés.
  - Os medidores de baixo ficam sobre o chão e as botas podem passar por cima deles [V06 q0050].

**Princípios A**
1. Lutador grande (~60 a 65 % da altura) com o pé quase na borda de baixo. O chão é uma faixa fina e a tela é dos personagens.
2. Distância inicial ≈ 44 % da largura entre centros: dá para andar meio passo e já chegar ao alcance de um golpe médio.
3. A câmera acompanha o pulo só ≈3 % da altura. Ela não persegue o personagem: deixa que ele saia por cima.
4. Tremor pequeno (≤3 px a 1080 p, 3 a 5 quadros). Não estrague a leitura com sacudida e use o efeito desenhado como principal.
5. HUD concentrado na faixa de cima (~13 %) e nos cantos de baixo. O centro de baixo fica livre para os pés.

---

## B. CENÁRIO

**Camadas percebidas** [V06 q0050]
1. Céu azul saturado com nuvens brancas.
2. Pontes e trilho suspenso de aço azul-acinzentado, mais palmeiras.
3. Grande letreiro "SOUTH TOWN ZOO" com esculturas enormes de leão, flamingo, arara e girafa e a fachada verde e amarela do zoológico.
4. Fileira de público e adereços (avestruz, estátua de gorila, urso-polar de gesso, placas "WETLANDS ISLAND", "SOUTH TOWN ZOO").
5. Chão de tijolinho amarelo-ocre com mosaico circular claro na frente.

**Paralaxe**
- Num pan para a direita, a faixa distante (ponte e céu) anda ≈27 px enquanto a faixa do público anda ≈42 px (≈0,65×) [V06 q0300→q0316].
- É paralaxe natural de câmera 3D. Para sprites, isso equivale a 3 ou 4 camadas com velocidades ≈0,5, 0,65 e 1,0.

**Hierarquia lutador × fundo: o fundo NÃO é escurecido nem dessaturado**
- Medido no quadro inteiro [V06 q0050], valores HSV de 0 a 255:

| Região | Lum. média | Saturação | % de pixels quase pretos (lum < 30) | % de pixels claros (> 200) |
|---|---|---|---|---|
| caixa do P1 (inclui um pouco de fundo) | 96 | 153 | **21,0 %** | 2,7 % |
| caixa do P2 | 94 | 143 | **21,5 %** | 3,6 % |
| fundo à esquerda | 93 | 138 | 7,5 % | 3,3 % |
| fundo entre os lutadores | 100 | 155 | 10,5 % | 7,8 % |
| fundo à direita | 103 | 115 | 10,2 % | 6,8 % |
| chão na frente | 139 | 131 | — | — |

- **Brilho e saturação médios são praticamente iguais** entre lutador e fundo.
- **O que separa os lutadores é a quantidade de PRETO:**
  - contorno grosso;
  - massas de sombra chapadas em preto;
  - botas e camiseta pretas.

  Isso dá **2 a 3× mais pixels quase pretos** no lutador do que no fundo.
- O chão claro (lum 139) recorta as pernas de jeans azul-escuro e as botas pretas.
- **O público é desenhado "mais mole"** [V06 q0050] (`cotw_publico_x_lutador.jpg`):
  - contorno fino (~1–2 px) e acinzentado;
  - sombreamento em degradê suave;
  - sem hachura e sem preto chapado;
  - altura de 38 % (criança) a 60 % (adulto) da altura do lutador.

  Os lutadores têm traço preto de 3 a 6 px, sombras em bloco e brilhos brancos duros.
- **Detalhe:** a faixa da luta não é mais simples que o resto, porque o cenário é cheio em todo lugar. A legibilidade vem do contraste de "acabamento", não de esvaziar o fundo.

**Elementos animados**
- **Público em ciclo contínuo** (`cotw_publico_animado.jpg`):
  - mulher acenando com os dois braços acima da cabeça, ciclo ≈9 a 12 quadros (≈0,4 a 0,5 s);
  - homem socando o ar, ciclo ≈6 a 9 quadros, com borrão de movimento nos braços [V06 q0041–q0074];
  - depois de uma queda há gente com as mãos na cabeça [V06 q0130–q0135].
- **Amplitude dos gestos:** grande nos braços, corpo parado no lugar.
- **Folhagem e água:** não percebi movimento.

**Chão e sombra**
- **Chão:** tijolo com rejunte visível e mosaico circular claro, sem reflexo.
- **Sombra dos personagens:** mancha escura difusa projetada para a direita e para trás (luz vinda da esquerda e da frente), alongada e de borda macia.
  - Escurece o chão em ≈30 a 45 % (lum 97 a 124 contra 175 a 184 no chão iluminado) [V06 q0050].
  - A textura do chão continua visível por baixo.

**Luz do cenário nos personagens**
- Iluminação diurna branca e quente, a mesma para os dois.
- Não vi tingimento de cor do ambiente no lutador.

**Paleta do fundo**
- Saturada:
  - azul-céu, verde-folha, amarelo-ocre (chão), verde-limão e amarelo (placas);
  - toques de rosa (flamingo) e laranja e arco-íris (arara).
- É mais "quente e alegre" do que os lutadores, que têm muito preto, azul-jeans e laranja ou verde.

**Princípios B**
1. **Hierarquia por acabamento, não por apagar o fundo.** Lutador com traço preto grosso e sombra chapada. Fundo e público com traço fino ou nenhum e sombreado em degradê.
2. Garanta 2 a 3× mais preto puro no lutador do que no fundo atrás dele. Sombras em bloco preto e botas e cabelo escuros fazem a silhueta "saltar" de qualquer fundo colorido.
3. Chão mais claro que as pernas, porque é onde os pés precisam ser lidos.
4. Público em ciclos curtos (0,3 a 0,5 s) de braço levantado, menor que o lutador (40 a 60 % da altura) e sem contorno forte.
5. Sombra no chão macia, ~35 % de escurecimento, deslocada para o lado oposto da luz, para ancorar o personagem.

---

## C. PERSONAGENS (RENDER)

**Contorno**
- Existe e é a marca do estilo.
- Contorno externo preto ou marrom muito escuro de **≈3 a 6 px a 1080 p**, mais grosso no lado da sombra e embaixo [V06 q0050] (zoom em `tmp_cotw/z.png` e `crop1.png`).
- Linhas internas de 1 a 3 px e afinando nas pontas (dobras da jaqueta, zíper, costuras).
- O traço tem variação de espessura, como pincel e nanquim, e não um "outline" uniforme.

**Sombreamento: estilo mangá**
- Tom base mais tom de sombra de borda dura, mais **massas pretas chapadas** (axilas, dobra da jaqueta, interior das pernas).
- Hachuras de nanquim (riscos paralelos) na transição sombra e luz da jaqueta.
- Jeans com **riscos de brilho branco-azulado** duros, quase "pintados" [V06 q0050; V06 q0255].
- **Na prática são 3 ou 4 tons** (luz, base, sombra, preto) mais brilho especular em traço.

**Contraluz / borda iluminada**
- Não há contraluz de cor constante.
- No jeans o brilho especular chega à borda e funciona como uma borda clara [V06 q0050].
- Em estados especiais aparece contorno colorido (verde no REV, ver J).

**Silhueta**
- **Exemplo 1, chute alto em pé:**
  - perna estendida na horizontal, à altura do ombro do oponente;
  - tronco inclinado para trás em contrapeso, braço de trás jogado para trás;
  - forma de "T deitado" legível mesmo em miniatura [V07 q0011–q0014].
- **Exemplo 2, caído:** corpo totalmente horizontal no chão, joelhos levemente dobrados [V06 q0120–q0135].
- **Exemplo 3, cambalhota no pulo:** vira uma bola compacta [V06 q0084–q0087].

**Proporções do Terry**
- Cabeça (do topo do cabelo ao queixo) ≈ 1/7 da altura.
- Ombros largos com a jaqueta volumosa, ≈0,5 da altura em largura [V06 q0050].
- Pernas longas: a virilha fica a ≈57 % da altura, medindo do chão.
- Punhos e luvas grandes.
- É herói de quadrinho: cabeça pequena, tronco em V, pés grandes nas botas.

**Taxa de animação**
- Animação contínua: em toda sequência o quadro seguinte difere do anterior, inclusive na respiração parada [V06 q0042–q0065]. Não há pose segurada por 2 a 4 quadros como em sprite.
- É 3D a 60 Hz [CONHECIMENTO], com **borrão de movimento forte** em membros rápidos (pernas e braços viram manchas) [V06 q0079; V07 q0004; V06 q0229–q0231].
- As poses-chave "seguradas" aparecem só nas pausas de impacto (ver I).
- **Para sprites:** imitar com poses-chave fortes e quadros de borrão desenhados (smear).

**Princípios C**
1. Contorno preto com espessura variável (3 a 6 px por fora a 1080 p, 1 a 3 px por dentro), mais grosso embaixo e na sombra.
2. Sombras em bloco duro com áreas de preto chapado e hachura na transição. Não use degradê no lutador.
3. Brilhos especulares em traços brancos duros (jeans, couro, cabelo).
4. Silhuetas de golpe que se leem como forma geométrica simples (T, L, bola).
5. Movimento rápido mostrado com borrão ou smear no membro, não com mais quadros.

---

## D. ANIMAÇÃO POR ESTADO

**Idle / guarda** [V06 q0042–q0065]
- **Pose:** pés afastados (≈2× a largura dos ombros), joelhos dobrados, punho de trás protegendo o queixo, punho da frente baixo e aberto.
- **Respiração (subida e descida):** o topo da cabeça oscila ≈13 a 17 px a 1080 p (≈1,5 % da altura da tela, ≈2,3 % da altura do personagem).
- **Ciclo:** ≈10 quadros, ≈0,41 s (±0,05 s). É rápido, "elástico", típico da SNK.
- **Rosto:** sério, olhar fixo.

**Andar:** não aparece com clareza.

**Corrida / dash** [V06 q0189–q0192]
- Passadas longas com tronco inclinado à frente.
- **Nuvens de poeira cinza** ficando para trás nos pés, ≈5 % da altura da tela.
- Termina em ajoelhar com soco no chão contra o oponente caído [V06 q0193–q0200].

**Pulo alto do P1** [V06 q0077–q0100] (`cotw_pulo_chute.jpg`)
- **Pré-pulo:** agacha em 2 quadros (q0077–q0078 ≈ 80 ms ≈ 5 quadros do jogo).
- **Decolagem:** 1 quadro com borrão forte (q0079).
- **Subida e topo:** encolhe em **cambalhota (bola)**, q0083–q0087, ≈5 quadros. Nesse ponto passa por cima do HUD.
- **Descida:** abre e estica a perna para o chute (q0088–q0090).
- **Acerto:** q0091.
- **Pouso:** em q0100, direto em base baixa.
- **Total ≈ 21 quadros ≈ 0,87 s ≈ 52 quadros do jogo.**
- **Altura:** ≈1 corpo inteiro acima do chão.
- **Gravidade estimada** ≈11 alturas de corpo/s². É ≈2× a gravidade real, coerente com jogo de luta em velocidade normal [CONHECIMENTO: SF tem pulo de ~45 quadros].

**Pulo dos dois no ar (troca aérea):** joelhos encolhidos, pernas para cima, borrão [V07 q0212–q0234].

**Agachar:** o P1 agachado leva um golpe com o joelho no chão e o tronco vertical [V06 q0236–q0246].

**Defesa em pé** [V06 q0224–q0230; V07 q0171–q0176]
- Braços cruzados na frente do rosto, tronco inclinado para trás.
- Aparece um **arco ciano** na frente (ver J).

**Queda e levantar** (ver H).

**Provocação / pós-golpe:** o P2 ajoelhado soca o chão e estende o braço em direção ao P1 caído, ≈5 quadros [V06 q0196–q0200].

**Intro e vitória:** não aparecem. A intro começa já em guarda.

**Movimento secundário**
- Cabelo longo chicoteia para trás nos golpes recebidos [V06 q0254–q0258].
- A gola de pelo acompanha.
- **"Chamas" amarelas pequenas** nos ombros e na cabeça enquanto o S.P.G. está ativo (efeito de estado, faíscas de 10 a 30 px) [V06 q0089; V06 q0120–q0140].

**Expressão**
- Rosto de dor no golpe recebido: olhos fechados com força, boca aberta [V06 q0236; V06 q0249].

**Princípios D**
1. Idle com respiração curta e visível (~2 % da altura do corpo, ciclo ~0,4 s). Parado nunca é parado.
2. Pré-pulo de 2 quadros de agachamento mais decolagem com smear. O pulo tem uma pose de "topo" diferente da subida (aqui, a bola).
3. Dash com poeira deixada para trás em 2 ou 3 baforadas.
4. Defesa com pose fechada (braços na frente do rosto) e efeito próprio de defesa.
5. Cabelo e roupa com atraso (follow-through) nos impactos.

---

## E. GOLPES NORMAIS

**Chute alto em pé** [V07 q0003–q0018] (`cotw_chute_ordem_v07.jpg`)
- **Antecipação:** 1 quadro com a perna vindo em borrão (q0004, ≈40 ms). É curtíssima: a preparação é "lida" pelo borrão, não por uma pose segurada.
- **Impacto (q0005):**
  - perna totalmente estendida acima da cabeça do oponente;
  - tronco inclinado para trás quase 30°, braço de trás jogado para baixo e para trás;
  - pé de apoio virado.
  - Pose exagerada, com a perna passando da horizontal.
- **Pose de impacto segurada** de q0005 a q0015, **≈10 quadros ≈ 0,41 s ≈ 25 quadros do jogo**. Inclui a pausa e o tempo ativo ou de recuperação; os personagens ficam praticamente imóveis de q0008 a q0010.
- **Recuperação:** a perna volta em 2 quadros com borrão (q0016–q0017).

**Soco reto em pé (P2)** [V06 q0247–q0253]
- Avanço com o corpo todo: braço estendido em linha com o ombro, tronco girado, perna de trás esticada em estocada, punho quase alcançando o meio da tela.
- O corpo inteiro participa: quadril e ombro avançam juntos.

**Rasteira deslizando (P1)** [V06 q0102–q0111]
- Corpo quase horizontal, apoiado em uma mão.
- Perna estendida rente ao chão por ≈9 quadros.
- Acerta o pé e manda o oponente girando no ar.

**Soco agachado:** acerta o P1 agachado (contra-ataque), com faísca no rosto [V06 q0235].

**Fraco / médio / forte:** não dá para separar com segurança nestes vídeos, porque quase todos os acertos são de punição ou contra-ataque. **Não invento.**

**Princípios E**
1. Antecipação curta (1 a 2 quadros) mostrada com smear. A clareza vem da pose de impacto exagerada.
2. Pose de impacto segurada por bastante tempo (~10 quadros da gravação, contando pausa e ativo) para a imagem "ficar na retina".
3. Corpo inteiro: contrapeso do tronco no chute, estocada de perna no soco.
4. Membro estendido além do natural (perna passa da horizontal, braço em linha total com o ombro).

---

## F. ESPECIAIS E PROJÉTEIS

**Projétil:** nenhum nos vídeos.

**Golpe giratório com rastro azul** [V07 q0049–q0057] (`cotw_rastros_arco.jpg`)
- **Arco de pincelada azul**, de azul-marinho a azul-claro e com brilho branco na borda, segue o caminho do pé.
- Raio ≈35 a 40 % da altura da tela.
- Dura ≈7 quadros e desbota no fim.
- Acerta com faísca laranja e cacos verdes.

**Golpe ascendente com rastro de fogo** [V06 q0269–q0271; V07 q0098–q0099]
- **Crescente laranja e amarelo** (cerne amarelo-claro, bordas laranja e vermelho) subindo a partir das pernas, com ≈2 a 3 quadros de vida.
- No V07 também solta **estilhaços vermelho e preto** rente ao chão, que voam até o canto esquerdo por ≈15 quadros [V07 q0101–q0116].
- O texto **REVERSAL** aparece colado ao corpo do P2 [V07 q0097–q0120].

**Investida ascendente com explosão de linhas** [V07 q0042–q0049] (`cotw_linhas_radiais.jpg`)
- No início, **dezenas de linhas radiais finas brancas com borda azul**, afinando em ponta, saem do corpo do P1 em todas as direções.
- Diâmetro ≈65 % da altura da tela, duração ≈3 quadros (q0043–q0045).
- Em seguida o personagem sobe e levanta uma **nuvem de poeira** grande no chão, ≈30 % da largura, ≈8 quadros [V07 q0046–q0053].

**Golpe ascendente com poeira:** poeira cinza grande nos pés [V06 q0151–q0159]. Antes há uma faísca azul, roxa e laranja, com o texto INVINCIBLE do lado do P2 [V06 q0148–q0150].

**Anti-aéreo:** o mesmo golpe ascendente do P2 acerta o P1 no ar [V06 q0158–q0169].

**Princípios F**
1. Golpes especiais têm um **efeito de "assinatura"** grande, com 35 a 65 % da altura da tela: rastro em arco, fogo, linhas radiais. É maior e mais colorido que um normal.
2. **Rastro em arco desenhado** (pincelada que segue o membro) dura 5 a 7 quadros e desbota. Para sprites, é o recurso mais barato e mais legível.
3. Poeira grande no chão quando o golpe "empurra" o chão (subida, investida).
4. Explosão de linhas radiais finas na **ativação** do golpe: sinaliza "isto é especial" antes do acerto.

---

## G. SUPERS (apresentação)

- **Nenhum Super aparece nos dois vídeos.** As únicas "ativações" vistas são:
  - o anel dourado do **S.P.G.** no começo do round [V06 q0079–q0082];
  - a explosão verde do **REV BLOW** [V07 q0066–q0069; V07 q0161–q0163].

  Nenhuma delas congela a tela, escurece o fundo (a luminância do fundo varia < 2 %: 111,7 → 113,2) [V07 q0060–q0075] ou dá close.
- **[CONHECIMENTO, sem confirmação no vídeo]:** os Supers do COTW (S.P. e Hidden Gear) usam congelamento curto, escurecimento do fundo e close de câmera, como os jogos de luta 3D atuais. Para referência concreta de Super, use outro vídeo.

**Princípios G** (válidos como contraste do que foi visto)
1. Mecânicas "de estado" (modo poder, medidor ativado) **não param o jogo**. Use anel ou explosão rápida (3 a 4 quadros) mais contorno colorido que dure.
2. Reserve congelamento e escurecimento só para o Super, para que ele seja raro e marcante.
3. A ativação precisa de um texto de evento curto (S.P.G. IN!, REV BLOW) ao lado do personagem, sem cobrir a ação.

---

## H. REAÇÃO A ACERTO

**Poses de dano** (`cotw_reacoes_dano.jpg`)
- **No rosto, de pé:** cabeça e tronco jogados para trás, coluna arqueada, braços soltos, cabelo chicoteando. O corpo vai recuando com as pernas abertas por ≈10 quadros [V06 q0249–q0258].
- **No tronco, de pé:** dobra para a frente na cintura, cabeça para baixo, braços caídos [V07 q0008–q0014].
- **Agachado:** cabeça vira para trás e de lado, olhos fechados, joelho continua no chão [V06 q0236–q0246].
- **Na cabeça, a partir do chute aéreo:** cabeça vira de lado, ombro cede [V06 q0091–q0097].
- **No ar:** vira de cabeça para baixo e gira [V06 q0112–q0118; V07 q0200–q0209].

**Tremor do atingido durante a pausa:** não detectei tremor lateral do corpo. O atingido fica imóvel e quem "vibra" é a faísca. Com a compressão do vídeo, não descarto ±1 a 2 px.

**Recuo (pushback)**
- O P1 atingido pelo soco de contra-ataque recua ≈5 a 8 % da largura em ≈10 quadros [V06 q0248–q0258].
- No chute em pé o oponente recua pouco, ≈2 a 3 %, porque o golpe seguinte do combo vem logo [V07 q0005–q0016].
- **Recuo na defesa:** o P2 quase não se move no arco de defesa [V06 q0224–q0230], ≈1 a 2 %.

**Queda** (`cotw_queda_levantar.jpg`)
- **Arremessado pela rasteira:**
  - **giro de cabeça para baixo** no ar por ≈7 quadros (≈0,29 s) [V06 q0112–q0118];
  - bate de costas no chão em q0119, com um pequeno ajuste de pernas (micro-quique dos joelhos) de q0120 a q0123;
  - **deitado de barriga para cima, imóvel** de q0120 a q0135 (≈16 quadros ≈ 0,66 s);
  - **levanta em ≈6 quadros** (senta, apoia, agacha, em pé), q0136–q0141 ≈ 0,25 s.
- **Segunda queda do P1:**
  - desliza de costas com **poeira nos pés** [V06 q0181–q0185];
  - fica no chão ≈20 quadros (≈0,83 s), q0181–q0201;
  - levanta em ≈8 quadros (≈0,33 s) passando por ajoelhado [V06 q0201–q0209].
- **Terceira queda do P2:** no chão ≈17 quadros [V06 q0303–q0320].

**Cor no atingido:** **sem piscar branco ou vermelho** no corpo. A informação de cor vem da faísca e dos textos.

**Princípios H**
1. Pelo menos 3 poses de dano distintas: rosto, tronco e agachado, mais giro no ar. A direção do golpe define a pose (rosto vai para trás, tronco dobra para a frente).
2. Pose de dano exagerada (arco das costas, cabeça jogada), mais follow-through de cabelo e roupa.
3. Queda: giro no ar de ~0,3 s, batida mais micro-quique, ~0,7 a 0,8 s no chão, levantar rápido de ~0,25 a 0,35 s.
4. Pushback visível no acerto forte (5 a 8 % da largura) e quase nulo na defesa.

---

## I. IMPACTO

**Pausa no acerto (hitstop)**, medida pela pose congelada dos dois:

| Situação | Pausa na gravação | ≈ quadros do jogo |
|---|---|---|
| Chute aéreo de punição (WILD PUNISH) [V06 q0092–q0097] | ≈6 quadros ≈ 0,25 s | ≈15 |
| Soco de COUNTER HIT [V06 q0248–q0253] | ≈6 quadros | ≈15 |
| Chute em pé de punição [V07 q0005–q0010] | ≈6 quadros | ≈15 |
| Rasteira [V06 q0105–q0111] | ≈6 a 7 quadros, com a perna segurada | — |

- Não há golpe fraco "limpo" para comparar. **Todos os acertos vistos são punições ou contra-ataques**, que tendem a ter pausa maior [CONHECIMENTO].
- **Na defesa:** a pose do atacante continua mexendo durante o arco de defesa, ou seja, pausa curta ou ausente [V06 q0224–q0230].

**Tremor de tela:** ±2 a 3 px, 3 a 5 quadros, só em alguns golpes (ver A).

**Contra-ataque: o que muda** [V06 q0235–q0258] (`cotw_faisca_contra_fullres.jpg`)
- **Texto COUNTER HIT** do lado de quem acertou. Um segundo empilha embaixo quando há segundo contra-ataque.
- **Faísca maior.** Explosão laranja e branca com ≈400 a 550 px (≈37 a 50 % da altura da tela) e, junto, **raios finos rosa e roxo** saindo para fora [V06 q0248–q0249].
- **Linhas vermelhas em zigue-zague** (como eletricidade) entre os dois depois da explosão, por ≈5 quadros [V06 q0251–q0255].
- O som tem um pico forte no mesmo instante (ver L).
- **Sem câmera lenta.**

**Ordem quadro a quadro de um golpe forte que acerta** (chute em pé, [V07 q0003–q0018]):
1. **q0004:** perna em borrão (antecipação e golpe a caminho).
2. **q0005 (contato):** perna estendida. **Explosão laranja, amarela e branca** aparece já grande sobre a cabeça do atingido, que é jogada para trás.
3. **q0006:** explosão no tamanho máximo, pontas laranja-avermelhadas.
4. **q0007:** a explosão se quebra em lascas laranja e o miolo amarelo encolhe. O atingido dobra para a frente.
5. **q0008–q0010:** **cacos verde-menta** se espalham em volta do atingido. Personagens imóveis (pausa).
6. **q0011–q0014:** cacos somem. O atingido é empurrado devagar, o atacante segura a pose.
7. **q0015–q0017:** atacante recolhe a perna com borrão. O atingido se endireita.

Total da faísca ≈7 quadros (≈0,3 s ≈ 18 quadros do jogo).

**Câmera lenta:** não há.

**Princípios I**
1. Pausa de ~15 quadros (60 Hz) em acertos de punição ou contra-ataque. Escale menos para normais leves [CONHECIMENTO: ~8 a 12 no SF].
2. A faísca nasce GRANDE no quadro de contato (sem crescimento lento) e se desmancha em fragmentos enquanto os personagens estão congelados.
3. O contra-ataque precisa de três sinais simultâneos: texto, faísca maior com raios e uma cor extra (vermelho nas linhas).
4. O tremor é opcional e pequeno. O impacto se vende com pausa mais faísca mais pose.

---

## J. EFEITOS VISUAIS

**Faísca de acerto padrão: 2 fases** (`cotw_faisca_pulo_fullres.jpg`, `cotw_faisca_contra_fullres.jpg`)
- **Fase 1, explosão 2D desenhada com aparência de nanquim**, 3 quadros (≈125 ms):
  - forma de estrela irregular com 10 a 16 pontas desiguais;
  - **miolo branco-creme** (#FFF6C0 aproximado), anel amarelo, pontas laranja e vermelho-escuro;
  - **manchas pretas e vermelho-escuras dentro da explosão**, como borrões de tinta e meio-tom de quadrinho [V06 q0091–q0093; V06 q0248–q0249].
- **Tamanho:**

| Acerto | Tamanho a 1080 p | % da altura da tela | % da altura do personagem |
|---|---|---|---|
| chute aéreo | ≈300 × 420 px | **≈39 %** | ≈60 % |
| chute em pé | ≈390 × 330 px | ≈30–36 % | — |
| contra-ataque | ≈550 × 400 px | ≈37–50 % | — |

  As referências são [V06 q0091], [V07 q0005] e [V06 q0248].
- **Fase 2, cacos verde-menta**, 4 a 5 quadros (≈170 a 210 ms):
  - triângulos de "vidro" verde-água (#7FFFD0 aproximado) com borda branca e brilho;
  - 15 a 40 cacos espalhados num raio de ≈20 % da altura da tela em volta do ponto atingido;
  - somem diminuindo e desbotando [V06 q0094–q0097; V07 q0008–q0011].
  - Aparecem em todos os acertos vistos (punição, contra-ataque, combo). Parece a "assinatura" do acerto no COTW, não uma cor por força.
- **A faísca não gira.** Ela explode e se fragmenta. É desenhada (sprite ou textura 2D sobre o 3D).

**Faísca de defesa** [V06 q0224–q0230; V06 q0329–q0333; V07 q0171–q0176] (`cotw_defesa_arco.jpg`)
- **Arco crescente ciano e branco** na frente de quem defende, cobrindo do topo da cabeça ao peito.
- Altura ≈22 % da tela (≈35 % do personagem), miolo branco, brilho ciano e pontinhos verdes.
- Afina até virar uma linha e some em ≈7 quadros (≈0,29 s).
- **Cor fria contra a quente do acerto:** azul quer dizer "bloqueou", laranja quer dizer "acertou".

**"Guarda especial" laranja (mecânica própria, provavelmente o REV Guard [CONHECIMENTO incerto])**
- Anéis e arcos laranja e vermelho girando em volta do corpo do defensor por ≈15 a 20 quadros [V07 q0069–q0092; V07 q0145–q0160].

**Linhas verdes do REV BLOW** [V07 q0066–q0069; V07 q0161–q0163] (`cotw_rev_blow.jpg`)
- **Explosão de linhas radiais verde-neon**, miolo branco-esverdeado, pontas afiadas.
- Diâmetro ≈60 a 70 % da altura da tela, por ≈3 a 4 quadros.
- Depois, **contorno verde-neon de ≈6 a 8 px** em volta do corpo inteiro do P1, com botas e mãos tingidas de verde-limão, que dura ≈20 a 26 quadros (≈0,8 a 1,1 s) [V07 q0069–q0091; V07 q0163–q0187].
- **Fundo não escurece.**

**Linhas radiais branco e azul** no início do golpe ascendente [V07 q0043–q0045] (ver F).

**Anel dourado do S.P.G.** [V06 q0079–q0082]
- Anéis de fogo dourado e amarelo-branco com faíscas, um em volta de cada lutador.
- Expandem de ≈25 % até ≈100 % da altura da tela em 3 quadros e somem.

**Poeira**
- Nuvem cinza semitransparente, em bolhas, na decolagem ou subida de golpes, no deslizar caído e no dash [V06 q0151–q0159; V06 q0182–q0195; V07 q0046–q0053; V07 q0169–q0180].
- Tamanho de 10 a 30 % da largura, dura 5 a 10 quadros e se dissipa para os lados.

**Rastros e smears:** arco azul de pincelada, crescente de fogo e borrão de movimento real nos membros (ver C e F).

**Efeitos de chão:** só poeira. Não há marcas nem rachaduras.

**Partículas do cenário reagindo:** não vi.

**Princípios J**
1. Faísca de acerto em **duas fases com cores contrastantes**: explosão quente (3 quadros) seguida de cacos frios (4 a 5 quadros). A troca de cor no meio dá "estalo" e prolonga o impacto sem prolongar a explosão.
2. Faísca **grande**, 30 a 50 % da altura da tela em punição ou contra-ataque, com manchas pretas internas para combinar com o traço de nanquim dos personagens.
3. Defesa com efeito frio (ciano) em arco, diferente da explosão quente do acerto.
4. Estado de poder mostrado por **contorno colorido em volta do personagem**, barato em sprite: outline extra e tinta nos pés e nas mãos.
5. Poeira cinza semitransparente em bolhas em todo movimento que "raspa" o chão.

---

## K. HUD E TEXTOS

Ver `cotw_hud_medidas.jpg` e `cotw_barra_vida_tempo.jpg`.

**Barras de vida**
- Em cima, de x 183 a 868 (P1) e de 1052 a 1737 (P2): **≈35,7 % da largura cada**, ≈36 px de altura (3,3 %), retas, com moldura escura fina.
- **Cores:**
  - vida em **verde-água** (#4FD6B0 aproximado) com textura de **pontinhos de meio-tom**;
  - trecho **S.P.G. amarelo e laranja** com o texto "S.P.G." em vermelho (≈1/3 da barra, na ponta de fora).
- **Esvaziam da ponta de FORA para o centro** (o P1 perde pela esquerda, o P2 pela direita) [V06 q0092; V06 q0255].
- **Dano vermelho que segura e escoa:**
  - a parte perdida vira **vermelho** e continua crescendo enquanto o combo dura [V06 q0092–q0128];
  - fica parada ≈1,2 s depois do último golpe;
  - depois encolhe até virar "vazio" em ≈0,6 a 1,3 s [V06 q0128→q0160; V06 q0200→q0230].
  - O vazio é um **branco-azulado semitransparente**: dá para ver o fundo através.
- Abaixo de cada vida há uma **barra azul e lilás fina**, ≈11 % da largura e ≈18 px, encostada no timer. Provavelmente medidor de guarda [CONHECIMENTO incerto]; não vi mudança clara.

**Timer**
- Círculo central com anel amarelo e fundo preto com pontinhos de meio-tom, diâmetro ≈116 px (≈10,7 % da altura).
- Número grande amarelo e laranja com contorno preto (60 no começo).
- Cada unidade dura ≈1,5 s da gravação (ver nota no topo).
- Acima, um texto pequeno "BEAT: 11" (contador de vitórias seguidas, branco, ≈2 % da altura).

**Retratos**
- Cantos de cima, ≈190 px (10 % da largura): rosto pintado com cor de fundo verde (P1) ou rosa e lilás (P2).
- Nome "TERRY" embaixo e etiquetas "PLAYER 1" (vermelho) e "PLAYER 2" (azul) em caixinhas.

**Medidores de baixo**
- **Arco semicircular "REV"** com porcentagem no centro (0 %, 25 %, 42 %): o arco enche de verde da esquerda para a direita [V07 q0066–q0080].
- Embaixo, **duas caixas retangulares de S.P.** que enchem de amarelo a partir do lado de dentro [V06 q0255].
- Cor de base marrom e oliva desbotado: chamam pouca atenção até encherem.

**Textos de evento** (`cotw_textos_evento.jpg`)
- **Posição:** na **lateral do jogador que causou o evento**, sobre a área de jogo, de x ≈ 2 a 20 % (P1) ou 80 a 96 % (P2) e y ≈ 30 a 58 % da altura. Ficam na altura do peito dos lutadores, nunca no centro.
- **Empilham:** o novo entra e os antigos são empurrados para baixo [V06 q0092–q0095: WILD PUNISH entra em cima e S.P.G. IN! desce ≈40 px].

| Texto | Visual e tamanho | Entrada | Duração | Saída |
|---|---|---|---|---|
| **S.P.G. IN!** | amarelo com contorno vermelho e preto; ≈3,7 % da altura × ≈17 % da largura | 1 quadro meio transparente e esticado na horizontal [V06 q0079] | q0079–q0112, ≈1,4 s | **achata na vertical até virar uma linha** em 2 quadros [V06 q0111–q0113] |
| **WILD PUNISH** | **azul-marinho com contorno branco**, letras condensadas grossas; ≈3,3 % × 16 % | 1 quadro "fantasma" [V06 q0093] e cheio em q0094 | ≈q0093–q0130, ≈1,5 s | desliza e some |
| **RUSH 2 HITS** | "RUSH" pequeno laranja em cima e "HITS" pequeno à direita; **número gigante amarelo e laranja com contorno preto** (≈5,5 % da altura) | o número faz um **"pop" de escala** (≈1,4×) quando o contador sobe [V06 q0108–q0111] | ≈1,7 s | achata [V06 q0146–q0147] |
| **COUNTER HIT** | **vermelho com contorno creme e preto**; ≈4,6 % da altura × ≈16 % da largura (x 1535–1848 a 1080 p) | 1 quadro escuro e transparente [V06 q0237], cheio em q0238 | **≈1,9 s** [V06 q0238–q0282] | um encolhe e achata [V06 q0270] |
| **INVINCIBLE**, **REV BLOW**, **REVERSAL** | mesma família: menores (≈3 % da altura), vermelho com contorno branco ou azul-marinho com contorno branco | — | — | REVERSAL fica colado ao corpo do personagem [V07 q0097] |

- **Hierarquia de cor:**
  - azul e branco para ações de sistema (WILD PUNISH, REVERSAL);
  - vermelho para dano e risco (COUNTER HIT, REV BLOW, INVINCIBLE);
  - amarelo e laranja para contagem e recurso (RUSH n HITS, S.P.G.).

**ROUND / READY / FIGHT** (`cotw_intro.jpg`)
- **q0005–q0008, cartela do round:**
  - corte para uma tela cinza-escura granulada com duas linhas amarelas horizontais;
  - caixa "ROUND" pequena que cresce (≈10 % → 37 % da largura em 3 quadros);
  - **varredura diagonal amarela** de baixo à direita para cima à esquerda;
  - **silhuetas pretas de prédios** (skyline) no amarelo, em cima à esquerda e embaixo à direita.
- **q0009–q0021:** o jogo aparece **numa faixa horizontal entre as listras amarelas** (≈25 a 75 % da altura), com "ROUND **1**":
  - "ROUND" em letras metálicas cinza e branco com contorno preto;
  - "1" grande vermelho.
- **q0022–q0027:** a faixa abre até a tela inteira e o HUD entra deslizando.
- **q0025–q0037:** ROUND 1 parado sobre o jogo.
- **q0038:** some desbotando.
- **Total ≈36 quadros ≈ 1,5 s.**
- **READY** [V06 q0041–q0062]:
  - letras serifadas pesadas em **vermelho com textura de meio-tom**, contorno branco e preto, sombra amarela;
  - ≈30 % da largura × 18 % da altura, no centro da tela (y ≈ 41 a 59 %), entre os lutadores;
  - entra com 1 quadro fantasma ampliado [q0041], fica 20 quadros e desbota [q0061–q0062];
  - **≈0,9 s.**
- **FIGHT** [V06 q0065–q0075]:
  - letras grossas **laranja e amarelo com degradê de fogo** e meio-tom;
  - aparece a ≈38 % da largura e cresce devagar até ≈40 % (q0066–q0070);
  - **salta para ≈67 %** em q0071;
  - **explode com borrão horizontal e radial** laranja q0072–q0075 até sumir;
  - **≈0,46 s.**
- O controle começa em q0077, quando o P1 agacha para pular.
- **KO e vitória:** não aparecem.

**Tipografia:** sem serifa condensada e grossa para os eventos; serifada pesada (estilo cartaz ou faroeste) para READY; letras grossas com fogo para FIGHT. Tudo com contorno duplo (claro e escuro) e meio-tom de quadrinho.

**Quanto o HUD chama atenção:**
- Médio-alto em cima: barras claras e saturadas, timer amarelo.
- Baixo embaixo: medidores marrons até encherem.
- Os textos de evento são bem visíveis, mas ficam nas laterais.

**Princípios K**
1. Vida perdida vira **vermelho que segura ~1 s e depois escoa**. O jogador "vê" o tamanho do combo.
2. Textos de evento **no lado de quem causou**, na altura do peito, empilhando de cima para baixo, com 1,4 a 1,9 s de vida, entrada de 1 quadro (fantasma ou esticado) e **saída achatando**.
3. Número de combo grande com **pop de escala** a cada acerto. A palavra fica pequena e o número é grande.
4. Cores de texto por categoria (sistema, dano, contagem) e sempre com contorno duplo para ler sobre fundo colorido.
5. Abertura em 3 tempos: cartela estilizada (~1,5 s), READY (~0,9 s), FIGHT que cresce e explode (~0,5 s). Total ~3 s.

---

## L. SOM

- **As duas gravações têm áudio:** AAC mono, 44,1 kHz, 13,84 s e 9,91 s. Os primeiros ≈0,15 s são silêncio. Depois há um **som de base contínuo** (provavelmente música ou ambiente; não consigo afirmar sem ouvir), com envelope ≈500 a 1500 RMS (de 16 bits) entre os eventos.
- **Picos de volume** (envelope de 25 ms) coincidem com eventos visuais:
  - **V06:** cartela ROUND (q0010), READY (q0044), FIGHT (q0071), rasteira (q0107), faíscas q0148→q0150 e q0167→q0168, COUNTER HIT q0248→q0250, rasteira q0288→q0291;
  - **V07:** acertos q0108→q0111 e q0191→q0193, golpe de linhas radiais q0043→q0048.
- O pico chega **1 a 5 quadros depois** do visual (40 a 200 ms), provavelmente por atraso do gravador do celular. Não use esses números para sincronia.
- Os picos de acerto chegam a 2 a 4× o nível de base.
- A energia fica concentrada em 500 a 4000 Hz (≈70 %), com pouco grave (≈12 a 14 % abaixo de 150 Hz). É a cara de som captado pelo celular.
- **Voz:** há pico em READY e FIGHT, compatível com locutor, mas **não consigo confirmar voz** só pela forma de onda.

**Princípios L**
1. Cada evento de tela (cartela, READY, FIGHT, acerto) tem um pico sonoro próprio, 2 a 4× acima da música.
2. O som do acerto sai **no mesmo quadro** da faísca. O atraso visto aqui é da gravação, não do jogo.
3. Música e ambiente contínuos por baixo, para que os golpes "furem" o som.

---

## M. FLUXO DO ROUND

| Momento | Quadros | Duração |
|---|---|---|
| Lutadores já em guarda (fim da tela anterior) | q0001–q0004 | — |
| Cartela ROUND 1 (corte, varredura amarela, faixa de jogo, tela cheia, texto parado, desbota) | q0005–q0038 | ≈1,5 s |
| READY | q0041–q0062 | ≈0,9 s |
| Intervalo | q0063–q0064 | — |
| FIGHT (cresce e explode) | q0065–q0075 | ≈0,46 s |
| Controle liberado; S.P.G. IN! nos dois lados com anéis dourados | q0076–q0079 | — |

- **Do corte até o controle: ≈3,0 s.**
- A intro do personagem, o KO e a vitória não aparecem.

**Princípios M**
1. Abertura curta (~3 s) e sempre igual. Os lutadores já ficam em guarda e respirando durante os textos.
2. A transição do round usa **identidade gráfica forte** (cor-tema amarela, skyline, textura granulada), não só um texto sobre o jogo.
3. O HUD entra junto com a abertura (desliza), não aparece de repente.
4. FIGHT com o maior tamanho e o maior movimento (explosão) da abertura.

---

## O que este jogo faz que mais o faz parecer profissional (em ordem de impacto)

1. **Faísca enorme em duas fases e duas temperaturas** (explosão laranja e branca com manchas de nanquim, depois cacos verde-menta), com 30 a 50 % da altura da tela [V06 q0091–q0097; V06 q0248–q0253; V07 q0005–q0011].
2. **Traço de nanquim nos lutadores** (contorno preto de 3 a 6 px, sombras chapadas em preto, hachura, brilhos em traço) contra o **público e o cenário de traço mole**. É isso que segura a leitura num fundo saturado [V06 q0050].
3. **Pausa de impacto longa com pose exagerada segurada** (~15 quadros do jogo; perna acima da horizontal, cabeça jogada para trás) [V07 q0005–q0015; V06 q0248–q0258].
4. **Textos de evento com hierarquia de cor, empilhamento e animação de entrada e saída** (pop de escala no número do combo, achatar para sair), sempre do lado de quem fez [V06 q0092–q0147; V06 q0238–q0282].
5. **Barra de vida com dano vermelho que segura e escoa**, esvaziando da ponta de fora, com textura de meio-tom [V06 q0092–q0160; V06 q0200–q0230].
6. **Poses de dano e queda bem distintas** (rosto, tronco, agachado, giro de cabeça para baixo, micro-quique, levantar rápido de ~0,3 s) [V06 q0112–q0141; V06 q0236–q0258; V07 q0008–q0014].
7. **Efeitos de assinatura dos especiais** (arco azul de pincelada, crescente de fogo, explosão de linhas radiais, poeira grande) [V07 q0043–q0057; V06 q0269–q0271].
8. **Abertura do round com identidade gráfica própria** (varredura amarela, skyline, faixa de jogo, READY de cartaz, FIGHT que explode) [V06 q0005–q0075].
9. **Defesa com efeito frio próprio** (arco ciano que afina e some), separando visualmente "bloqueou" de "acertou" [V06 q0224–q0230; V07 q0171–q0176].
10. **Estado de poder por contorno colorido** (verde-neon de ~6 a 8 px por ~1 s) e cenário vivo com público em ciclo contínuo [V07 q0069–q0091; V06 q0041–q0074].
