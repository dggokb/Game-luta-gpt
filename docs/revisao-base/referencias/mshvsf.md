# Referência: Marvel Super Heroes vs. Street Fighter (Capcom, 1997, CPS2) — V08

Vídeo analisado: V08 (gravação de tela do celular, 2400×1080, 14,5 s, 342 quadros reais ≈ 23,6 q/s; extraídos 344 quadros a 24 q/s em `vref/v08/f0001…f0344.jpg`, numeração `qNNNN` abaixo). Conteúdo: Ryu (time "NEW HERO") fazendo um loop de canto contra Capitão América (parceiro Shuma-Gorath) de 11 a 39 acertos, com um Hyper Combo no meio, um super pulo, o texto MARVELOUS, e um segundo combo (2–10 acertos) com outro Hyper Combo. Não há início de round, KO nem vitória.

Convenções:
- 1 quadro da gravação (q) ≈ 41,7 ms ≈ 2,5 quadros do jogo a 60 Hz (qj). Margem de erro típica: ±1 q (±2,5 qj).
- Medidas em % da ÁREA DE JOGO (sem as barras pretas). No vídeo original a área de jogo vai de x = 296 a 2103 (1808 × 1080 px); nos quadros de 1200 px, de x = 148 a 1051 (904 × 540).
- **Proporção da área de jogo: ≈ 1,67:1 — NÃO está esticada para 4:3.** É praticamente a proporção nativa de pixels do CPS2 (384 × 224 = 1,71:1), com um pequeno corte no topo (as letras "NEW HERO" saem cortadas) [V08 q0259]. Escala ≈ 4,7 px de vídeo por pixel nativo na horizontal (1808/384).
- Atenção: os rótulos "NEW HERO" no HUD e o corte com retrato gigante do Ryu no Hyper não batem com o que conheço do jogo original — provavelmente é uma versão modificada (hack) ou um modo especial [CONHECIMENTO, incerto]. Trato o que aparece como referência visual, sem afirmar que é do arcade original.
- Aviso do celular "O vídeo gravado é muito curto" aparece por cima em q0052–q0121 (centro-baixo) — ignorado nas medidas.

Folhas de imagem montadas (em `img/`):
- `mshvsf_faiscas.jpg` — 8 tipos/fases de faísca (q0010, q0022, q0159, q0141, q0196, q0173, q0300, q0074).
- `mshvsf_chute_em_pe.jpg` — chute alto em pé quadro a quadro (q0188–q0203).
- `mshvsf_dash_soco.jpg` — dash baixo, parada com poeira, soco e faíscas (q0199–q0210).
- `mshvsf_lancador_rasteira.jpg` — lançador (q0070–q0075) e rasteira + ativação do Hyper (q0303–q0308).
- `mshvsf_reacao_oponente.jpg` — reações do Capitão (q0021–q0026, q0143–q0146, q0153–q0154, q0202–q0207).
- `mshvsf_camera_sobe.jpg` — super pulo com a câmera subindo (q0228–q0239).
- `mshvsf_camera_desce_queda.jpg` — câmera descendo e queda do oponente (q0246–q0257).
- `mshvsf_queda_levantar.jpg` — queda, rolamento e levantar (q0250–q0261).
- `mshvsf_hyper1_a.jpg`, `mshvsf_hyper1_b.jpg` — Hyper 1 completo (q0072–q0135).
- `mshvsf_hyper2.jpg` — Hyper 2 (q0305–q0344).
- `mshvsf_hud.jpg` — HUD de cima, HUD de baixo e MARVELOUS em detalhe.
- `mshvsf_marvelous.jpg` — fim do contador e entrada do MARVELOUS (q0268–q0276).

---

## A. TELA E CÂMERA

**Altura do personagem em pé.** Ryu em guarda: topo da cabeça a ≈ 48–50% e pés a ≈ 88–89% da altura → **≈ 39–40% da altura da área de jogo** [V08 q0229, q0259]. O Capitão América tem a mesma escala. No lançador (soco para cima), Ryu + braço chegam a ≈ 57% da altura (≈ 1,45× a altura em pé) [V08 q0072].

**Distância entre eles.** Não há início de round. No loop de canto, os centros dos corpos ficam a ≈ 20–25% da largura (Ryu ≈ 70–72%, Capitão ≈ 92–95%) [V08 q0207, q0229]. Depois da queda, ≈ 75% (Ryu em x ≈ 10%, Capitão em ≈ 85%) [V08 q0259].

**Linha do chão.** Pés a ≈ 88–89% de cima para baixo; o centro da sombra fica a ≈ 90% [V08 q0229]. Sobram só ≈ 11% de chão visível abaixo dos pés, e a barra de Hyper ocupa boa parte disso.

**Zoom.** Nenhum. A escala é fixa: Ryu tem o mesmo tamanho a 20% e a 75% de distância [V08 q0207 × q0259]. Jogo de sprites: não há zoom [CONHECIMENTO].

**Câmera horizontal.** No canto ela fica parada. Procurei deslocamento do fundo (escada rolante, ±8 px a 1200 px) em q0005–q0074 e q0137–q0229 e deu 0 px. Ela só rola quando Ryu volta para o centro: ≈ 0,8% da largura por quadro [V08 q0291–q0292]. Ponto importante: **no canto o oponente fica metade fora da tela** (x ≈ 92–100%) durante quase todo o loop [V08 q0143–q0146, q0204–q0207].

**A câmera sobe no pulo? Sim, e muito.**
- Pulo comum/alto: a câmera sobe até ≈ 15% da altura da tela no topo do pulo e volta. Ryu chega a sair pelo topo do quadro [V08 q0271–q0285]. O tempo no ar é ≈ 16 q ≈ 0,67 s ≈ 40 qj.
- Super pulo depois do lançador: a câmera sobe **≈ 1 altura de tela inteira** (estimativa grosseira, 80–120%) em ≈ 6–7 q (≈ 250–290 ms ≈ 15–17 qj). O chão some e aparece o andar de cima do shopping, com sacadas, público e bandeirolas [V08 q0230–q0237; q0075–q0079 no Hyper 1].
- Descida: ≈ 5–8 q (≈ 210–330 ms) [V08 q0248–q0256; q0132–q0136].
- Para onde a câmera olha: depois do Hyper 1 ela **fica no alto, com o oponente**, e Ryu cai para fora da tela por baixo. Aparece então um **indicador "1P"** no canto inferior direito: silhueta amarela do Ryu sobre um triângulo azul apontando para baixo [V08 q0121–q0133]. É um marcador de personagem fora da tela.

**Tremor de tela.** Nos golpes normais, não houve nenhum: o fundo fica com deslocamento de 0 px em todos os acertos medidos, inclusive nas faíscas em X [V08 q0022, q0073, q0195, q0204, q0218]. O "peso" vem da faísca e da pose segurada, não do tremor. No Hyper o impacto é feito com flashes da tela inteira (seção G), não com tremor.

**Câmera nos Supers.** Não há close nem inclinação. O jogo **troca o cenário** por um fundo de linhas coloridas e mostra um retrato gigante sobreposto (seção G). Antes da troca, um quadro com o cenário ≈ 17% mais escuro (brilho V 76 → 63) e sem HUD [V08 q0311 → q0312].

**HUD.** Em cima, ≈ 0–15% da altura (barras de vida a ≈ 4–9%; nome do parceiro a ≈ 11–14%). Embaixo, ≈ 92–100% ("LEVEL" + barra de Hyper). Total **≈ 23% da altura** coberta pelo HUD. O contador "NN HIT" fica sobre o cenário, em ≈ 28–43% da altura, à esquerda [V08 q0229].

**Princípios (A):**
1. Escala fixa, com o personagem a ≈ 40% da altura da tela. É grande o bastante para ler os golpes e deixa ≈ 45% de espaço acima da cabeça para pulos e lançamentos.
2. Chão baixo (pés a ≈ 89%): quase toda a tela fica para o corpo e para o ar.
3. A câmera acompanha o eixo vertical. Pulo comum: ≈ 15% de subida. Super pulo: ≈ 1 tela inteira, em ≈ 0,25 s na ida e ≈ 0,2–0,3 s na volta.
4. Se a câmera deixar alguém fora da tela, mostre um indicador (ícone + seta) na borda.
5. Sem tremor nos golpes normais. O impacto vem de faísca, pausa e pose, e o efeito de tela forte fica guardado para o Super.

---

## B. CENÁRIO

**O que é.** Interior de shopping: escada rolante azul à esquerda, colunas amarelas, loja "MARVEL COMICS SHOP" com vitrine azul ao fundo, uma fileira de público (≈ 25–30 pessoas, algumas sentadas no chão, uma fantasiada de Mega Man, placas verdes erguidas) e, no andar de cima, sacadas com mais público, faixas vermelhas/laranja e varais de bandeirolas triangulares verdes e amarelas [V08 q0229, q0240].

**Camadas percebidas** (do fundo para a frente):
1. Arquitetura (paredes, colunas, loja, escada rolante, teto escuro).
2. Público da sacada no andar de cima (pequeno).
3. Faixas e bandeirolas penduradas.
4. Público da frente, em pé/sentado logo atrás dos lutadores.
5. Chão de lajotas.

**Paralaxe.** Na subida do super pulo, as bandeirolas cruzam a tela visivelmente mais rápido que o prédio [V08 q0231–q0235]. Não consegui medir a razão (o movimento é rápido e o vídeo está borrado). Na horizontal não houve rolagem suficiente para medir.

**Hierarquia lutador × fundo** (medido em HSV, V = brilho, S = saturação, 0–100):
| Região | V | S |
|---|---|---|
| Público da frente (faixa dos lutadores) [q0259] | 68 | 51 |
| Escada rolante [q0259] | 70 | 63 |
| Fundo alto (colunas/loja) [q0259] | 75 | 58 |
| Chão [q0259] | 95 | 22 |
| Tronco do Ryu (quimono branco) [q0259] | 82 | 27 |
| Capitão América [q0259] | 87 | 61 |
| Andar de cima (super pulo) [q0240] | 71 | 48 |
| Fundo do Hyper (linhas) [q0085] | 72 | 85 |

Conclusão: **o fundo NÃO é escurecido nem dessaturado**. É quase tão saturado quanto os personagens e só ≈ 10–20 pontos mais escuro. Nada é desfocado. Mesmo assim os lutadores se leem, por cinco motivos:
1. Ficam sempre mais baixos que tudo: pés a 89%, enquanto os pés do público ficam a ≈ 78%.
2. Têm uma sombra preta e chapada que os "prende" ao chão (seção B, Chão).
3. O quimono do Ryu é o maior bloco quase branco da tela.
4. O contorno escuro é contínuo (seção C).
5. São os únicos elementos com movimento grande e rápido. O público só mexe os braços.

**Escala do público.** As pessoas da frente têm ≈ 33–38% da altura da tela, ou seja ≈ 85–95% da altura do Ryu, e ficam logo atrás dele [V08 q0229]. Essa é a parte mais arriscada do cenário: figuras humanas quase do mesmo tamanho e tão coloridas quanto os lutadores. Funciona por causa da diferença de altura do chão e das sombras.

**Detalhe na faixa da luta × resto.** O detalhe mais denso (rostos, roupas, placas) está justamente na faixa de 40–80% da altura, atrás dos lutadores. As áreas de cima (escada, colunas, teto) são blocos grandes de cor chapada, com pouco detalhe [V08 q0229].

**Elementos animados.** O público troca de pose a cada ≈ 3 q (≈ 125 ms ≈ 7–8 qj, ou ≈ 8 poses/s), com braços para cima e para baixo em 2–3 poses de ciclo. Às vezes para por ≈ 1 s (nenhuma mudança em q0039–q0062) [V08 q0149–q0198: mudanças em q0149, 151, 154, 157, 160, 164, 167, 170, 173, 176, 179, 183…]. Não vi água, fogo nem bandeiras tremulando.

**Chão.** Lajotas creme/rosa-claro com faixas ciano em perspectiva (linhas diagonais convergindo) [V08 q0229]. Sem reflexo. Brilho alto (V 95) e saturação baixa (S 22): é a área mais "calma" da tela, e isso ajuda as sombras a se destacarem.

**Sombra dos personagens.** **Elipse preta chapada e 100% opaca** (RGB ≈ 33, 31, 30, igual ao contorno), com ≈ 20–22% da largura da tela × ≈ 6–7% da altura, centrada a ≈ 90% da altura [V08 q0179, q0229]. É ≈ 1,5× mais larga que o corpo do Ryu (≈ 13% da largura). As sombras dos dois lutadores se fundem quando estão próximos. No pulo a sombra fica no chão [V08 q0271–q0273]. Não consegui medir se ela encolhe.

**Luz do cenário nos personagens.** Nenhuma. Os sprites têm a mesma paleta em qualquer lugar do cenário. A única mudança de cor no personagem é a tintura do oponente durante o Hyper 2 (seção H).

**Paleta.** Azul royal (escada rolante, vitrine), amarelo-creme (colunas), laranja/vermelho (faixas), verde e amarelo (bandeirolas), roxo-escuro (teto e sombras do prédio). Saturação geral alta.

**Princípios (B):**
1. Num cenário cheio de detalhe, a leitura vem de **posição** (lutadores mais baixos, na frente de tudo) + **sombra preta opaca** + **contorno**, e não de escurecer o fundo.
2. Deixe o chão claro, pouco saturado e liso. É o "palco" onde a sombra escura e os pés se leem.
3. Coloque o detalhe denso na faixa dos lutadores só se ele for estático ou pouco animado. Os ciclos lentos do público (≈ 8 poses/s, com pausas) não competem com os golpes.
4. Pinte o cenário bem mais alto que a tela (≈ 2 telas). O super pulo revela um andar inteiro novo, e isso vende a altura.
5. Bandeirolas e faixas numa camada própria, mais rápida, já dão paralaxe vertical barata.

---

## C. PERSONAGENS (RENDER)

**Contorno.** Sim: uma linha escura de ≈ 1 pixel nativo, ≈ 4–5 px a 1080p, quase preta/marrom-escura (≈ RGB 30–40), em toda a silhueta. No quimono branco o contorno interno é cinza-azulado, mais claro [V08 q0259; recorte em resolução cheia]. O vídeo é comprimido, então a espessura exata é estimativa.

**Sombreamento.** Cel shading com sombras de borda dura:
- Pele: 3 tons (claro, médio, marrom-escuro) + pequeno brilho.
- Quimono: branco, cinza-azulado claro, cinza médio.
- Faixa preta: 2 tons.
- Luvas vermelhas: 2–3 tons.
A luz vem de cima/da frente. Não há degradê suave (o "suave" que aparece é compressão do vídeo) [V08 q0259].

**Contraluz/borda iluminada.** Não há.

**Silhueta (2 exemplos).**
- Chute alto em pé: a perna forma uma diagonal limpa do quadril até acima da cabeça do oponente, com o tronco inclinado para trás em contrapeso. Silhueta em "Y" deitado, legível mesmo em miniatura [V08 q0192–q0194].
- Lançador: o corpo inteiro vira uma linha vertical (pé de apoio → quadril → punho acima da cabeça), com ≈ 1,45× a altura em pé [V08 q0072].
- Também: a rasteira é uma linha horizontal colada ao chão, com a perna estendida ≈ 20% da largura [V08 q0304–q0307].

**Proporções.** Heroicas, estilo "Marvel": cabeça ≈ 5% da altura da tela, então ≈ 7,5–8 cabeças de altura. Ombros e antebraços grandes, mãos e pés grandes, cintura estreita. O Capitão é ainda mais musculoso. Escudo com ≈ 13% da altura da tela de diâmetro [V08 q0229, q0259].

**Taxa de animação: animação limitada (sprite clássico).** Contei as mudanças de pose do Ryu em q0177–q0229 (53 q ≈ 2,2 s): ≈ 29 poses distintas, ou **≈ 13 poses/s em média**. O padrão:
- **Transições** (preparação, recolher, dash): 1 pose por quadro da gravação → poses de ≈ 2–4 qj.
- **Pose de impacto/extensão:** segurada 3–6 q (≈ 7–15 qj), contando a pausa do acerto [V08 q0192–q0197, q0203–q0205, q0214–q0220].
Não há interpolação. Cada golpe é uma sequência curta de desenhos (detalhe na seção E).

**Princípios (C):**
1. Contorno escuro de 1 pixel nativo em todo o sprite. É o que separa o personagem de um fundo igualmente saturado.
2. Cel shading de 2–3 tons por material, com sombra dura e luz consistente de cima/da frente.
3. Proporções exageradas (mãos, pés, antebraços) para que o golpe leia em miniatura.
4. Animação limitada: transições rápidas (1 desenho cada) e extensões seguradas. Poucos desenhos bem escolhidos valem mais que muitos intermediários.
5. Toda pose de ataque precisa de silhueta própria (diagonal, vertical, horizontal), distinguível só pelo contorno.

---

## D. ANIMAÇÃO POR ESTADO

O vídeo é um combo contínuo, então só aparecem alguns estados.

**Guarda/idle.** Só 2–3 quadros visíveis antes de ações: Ryu de lado, punhos à frente do peito, joelhos dobrados, base larga [V08 q0229, q0258–q0259]. Não dá para medir a respiração. O Capitão segura o escudo à frente do corpo na guarda [V08 q0258–q0259].

**Andar.** Não aparece.

**Dash (corrida baixa).** É o estado mais visto: ele abre cada volta do loop.
- Pose: Ryu baixíssimo, tronco quase na horizontal, perna de trás esticada, cabeça na altura do quadril do oponente.
- **2 poses** (avanço baixo 1 q + transição 1 q), depois uma pose de parada já ereta.
- Duração de cada dash: ≈ 2–3 q (≈ 85–125 ms ≈ 5–7 qj) [V08 q0177–q0178, q0200–q0201, q0222–q0223].
- Em sequência (q0260–q0269), Ryu repete dash–para–dash: pose baixa (q0260, q0263–q0264, q0267–q0268) e pose ereta de parada (q0262, q0266, q0269). Avança ≈ 3,5% da largura por q (≈ 1,4% por qj).
- **Poeira na parada:** nuvem cinza-bege no pé de trás, com ≈ 6–7% da altura, visível 2–3 q (≈ 5–8 qj), crescendo e se desfazendo [V08 q0203–q0205, q0137–q0138, q0159–q0160, q0226–q0228].

**Pulo comum** [V08 q0269–q0286]:
- Pré-pulo: ≈ 1 q (≈ 2–3 qj).
- No ar: ≈ 16 q (≈ 0,67 s ≈ 40 qj), em pose encolhida, joelhos ao peito. A câmera sobe ≈ 15%.
- Desce com um chute aéreo que acerta (q0286–q0287).

**Super pulo** [V08 q0229–q0240]:
- Pré-pulo: 1 q, agachado (q0230).
- Subida: 2 q de pose encolhida, braços para cima (q0231–q0232).
- No ar: chutes aéreos alternando com a pose encolhida (q0233–q0236).
- Topo: ≈ 5+ q flutuando (q0237–q0247). O Ryu fica na mesma altura da tela enquanto a câmera o segue.

**Lançador** (seção E) → super pulo no Hyper 1 [V08 q0070–q0079].

**Queda e levantar (oponente)** [V08 q0250–q0259]:
- Cai de cabeça (2–3 q).
- Toca o chão deitado de costas, pernas para cima (q0254–q0255).
- Rola com o escudo (q0256–q0257).
- Em guarda com o escudo (q0258).
**Do toque no chão à guarda: ≈ 4–5 q (≈ 170–210 ms ≈ 10–12 qj).** Não vi quique nem poeira no pouso.

**Agachar, defesa, intro, vitória.** Não aparecem, salvo o agachamento do pré-pulo e a rasteira.

**Movimento secundário.** A faixa vermelha da cabeça do Ryu e as pontas da faixa preta do quimono mudam de forma entre as poses: estão desenhadas em cada pose, não simuladas [V08 q0190–q0199]. As calças do quimono têm barras rasgadas, que mudam de silhueta a cada pose.

**Expressão.** No sprite pequeno não dá para ver. No retrato do Hyper: boca aberta gritando, sobrancelhas franzidas, dentes à mostra [V08 q0082–q0093].

**Princípios (D):**
1. Dash = 1 pose baixa e alongada + 1 pose de parada + poeira no pé de trás. É barato e vende velocidade.
2. Todo pouso/parada brusca ganha uma nuvem de poeira de 2–3 desenhos.
3. Levantar rápido (≈ 10–12 qj do toque à guarda) e rolando mantém o ritmo. Use poucas poses bem distintas: cair, deitado, rolar, guarda.
4. Desenhe faixas e roupa soltas em cada pose (movimento secundário "pintado"), sem física.
5. No ar, alterne uma pose encolhida (subida/descida) com as poses de golpe. A pose encolhida é a "base" aérea.

---

## E. GOLPES NORMAIS

Contagem de poses distintas por golpe (a 24 q/s não se vê tudo; poses de 1 qj podem escapar):

| Golpe | Antecipação | Ativo / impacto | Segurada no impacto | Recuperação | Poses distintas vistas |
|---|---|---|---|---|---|
| Soco em pé reto [q0201–q0207] | sai direto do dash, braço recolhido 1 q (q0202) | braço totalmente estendido (q0203) | q0203–q0205 = 3 q (≈ 7 qj), 2 deles com a faísca | recolher 1 q (q0206) + virar o tronco 1 q (q0207) | ≈ 4 |
| Soco curto próximo [q0179–q0189] | ombro recuado 1 q | punho na altura do peito do oponente, tronco girado | ≈ 2–3 q | 1 q | ≈ 3 |
| Chute alto em pé [q0189–q0199] | **chambra**: corpo girado, joelho alto, 1 q (q0190) | perna subindo 1 q (q0191) → perna acima da cabeça (q0192) | **q0192–q0197 = 6 q (≈ 15 qj)**: 3 antes do contato + 3 de pausa | recolher em 2 poses de 1 q (q0198, q0199) | ≈ 5 |
| Lançador (soco para cima) [q0070–q0075] | agachado 1 q (q0070) | subindo 1 q (q0071) → corpo vertical com punho acima da cabeça (q0072) | q0072–q0074 = 3 q | agacha para o super pulo (q0075) | ≈ 4 |
| Rasteira [q0303–q0307] | agachado girando 1 q (q0303) | perna estendida no chão (q0304) | q0304–q0307 = 4 q (≈ 10 qj) | cancelada pelo Hyper (q0308) | ≈ 3 |

**Antecipação.** Curta (1 desenho, ≈ 2–4 qj), mas sempre presente e sempre na direção oposta do golpe:
- chambra do chute com o corpo de costas;
- agachamento antes do lançador;
- ombro recuado antes do soco.

**Exagero no impacto.**
- O braço do soco reto fica claramente mais comprido que o natural (alcança ≈ 1/3 da altura do Ryu além do ombro) [V08 q0203].
- No lançador, o corpo estica até ≈ 1,45× a altura em pé [V08 q0072].
- A perna do chute alto passa bem acima da cabeça [V08 q0192].

**Smear/rastro desenhado.** No sprite do Ryu não vi smear claro (deformação/rastro de membro) nos normais. A pose intermediária do chute (q0191) parece "borrada", mas a compressão do vídeo não permite afirmar. Os rastros que existem são **efeitos separados**:
- a faísca em feixe/diagonal, que funciona como "linha de movimento" na direção do golpe (seção J);
- um fino arco branco em volta da faísca do lançador [V08 q0074].

**Pose de impacto segurada.**
- Em geral, 2–3 q (≈ 5–8 qj) depois do contato.
- No chute alto o golpe "espera" o oponente: a perna fica estendida 3 q antes do contato e 3 depois [V08 q0192–q0197].
- No começo da gravação houve congelamentos de 3–4 q (q0010–q0012, q0022–q0025) que não se repetiram nas voltas seguintes. Pode ser travamento da reprodução.

**Fraco × médio × forte.** No loop, a diferença mais clara está na **faísca** (feixe horizontal → X laranja → X branco, seção J) e no tamanho da pose (soco curto próximo × soco reto longo × chute acima da cabeça). Os golpes fortes têm 1–2 poses a mais (chambra) e seguram mais tempo.

**Corpo inteiro.**
- Chute alto: o tronco inclina para trás em contrapeso, o pé de apoio gira (calcanhar para fora) e os braços vão para o lado oposto [V08 q0192].
- Soco reto: o ombro da frente avança e o quadril gira [V08 q0203].
- Lançador: o pé de trás empurra, o joelho estica e o braço livre desce [V08 q0072].

**Princípios (E):**
1. 3–5 desenhos por normal bastam: antecipação (1) → extensão (1–2) → pose de impacto segurada → recolher (1–2).
2. A pose de impacto é a mais exagerada (membro mais longo que o natural) e a que fica mais tempo na tela.
3. A antecipação vai para o lado contrário do golpe e dura 1 desenho só, para não atrasar o jogo.
4. O corpo inteiro participa: contrapeso do tronco, giro do pé de apoio, braço livre oposto.
5. Faça a diferença de força ver-se no efeito (faísca) e na amplitude da pose, não só no dano.

---

## F. ESPECIAIS E PROJÉTEIS

Neste trecho não aparecem especiais nem projéteis comuns, só dash, normais, lançador e dois Hyper Combos (seção G). [CONHECIMENTO: Ryu no MSHvsSF tem Hadoken, Shoryuken e Tatsumaki; nenhum aparece aqui.]

O que serve de referência para investidas:
- **Dash baixo:** sem linhas de velocidade. A sensação de velocidade vem da pose alongada + poeira na parada (seção D).
- **Subida do Hyper 1 (soco ascendente):** Ryu sobe da base da tela com uma **coluna de "fumaça" azul-violeta** desenhada abaixo dele (manchas arredondadas empilhadas, ≈ 8–10% da largura), do chão até os pés dele [V08 q0100–q0115]. Funciona como rastro de uma investida vertical.

**Princípios (F)** (aplicáveis ao que foi visto):
1. Investida rápida = pose alongada + rastro desenhado atrás (coluna/trilha), que fica na tela alguns quadros.
2. Rastros em cor contrastante com o personagem (azul-violeta atrás do quimono branco).
3. O rastro nasce do chão e acompanha o personagem: mostra o caminho todo, não só a posição atual.

---

## G. SUPERS (HYPER COMBO) — apresentação

Dois Hypers no vídeo, com a mesma estrutura.

### Hyper 1 (no ar, depois do lançador) — [V08 q0072–q0136]
| Tempo | Quadros | O que acontece |
|---|---|---|
| 0 | q0072–q0074 | Lançador acerta: X branco + disco branco |
| +3 q | q0075–q0079 (5 q ≈ 210 ms) | Super pulo: câmera sobe, bandeirolas aparecem |
| +8 q | q0080 | **Corte seco:** HUD some, o cenário vira fundo de linhas, faixa de chão escura embaixo (≈ 85–100%), estrela branco-azulada no alto; Ryu pequeno agachado embaixo carregando, com "chamas" brancas subindo |
| | q0081–q0088 | **Retrato gigante do Ryu** entra pela esquerda (busto gritando, ≈ 0–40% da largura, ≈ 15–100% da altura, por cima de tudo); raios/tentáculos de energia azul-claro saem dele; gotas azul-claro |
| | q0089 | Círculo branco enorme (≈ 70–80% da altura) — flash |
| | q0090 | Círculo azul-lavanda grande com respingos ciano |
| | q0091–q0093 | Retrato parado, fundo de linhas, oponente no alto à direita |
| +22 q | q0094 | HUD volta (o retrato ainda sai, em 1 q) |
| | q0094–q0102 | Ryu sobe do pé da tela (q0100–q0102) com a coluna de fumaça azul |
| +31 q | q0103 | **1º acerto do Hyper** (22 HIT) |
| | q0103–q0115 | Acertos a cada ≈ 2–3 q (≈ 85–125 ms ≈ 5–7 qj): 22, 23, 24, 25, 26 em q0103, 105, 108, 111, 113, com flashes (ver abaixo) |
| +44 q | q0116 | Corte seco de volta ao cenário (andar de cima), câmera no alto |
| | q0116–q0131 (16 q ≈ 0,67 s) | Ryu cai para fora da tela; marcador "1P" aparece |
| | q0132–q0136 (≈ 5 q) | Câmera desce para o chão |

### Hyper 2 (no chão, depois da rasteira) — [V08 q0305–q0344]
| Tempo | Quadros | O que acontece |
|---|---|---|
| 0 | q0304–q0307 | Rasteira acerta (feixe branco diagonal) |
| +4 q | q0308–q0311 (4 q ≈ 170 ms) | **Ativação:** labaredas creme/brancas sobem do corpo do Ryu (≈ 25–30% da altura da tela) — o "aviso" de que vem um Super |
| +8 q | q0312 (1 q) | HUD some, cenário ≈ 17% mais escuro (transição) |
| +9 q | q0313 | Corte para o fundo de linhas + estrela branco-azulada |
| | q0314 | Flash lilás no fundo inteiro; retrato entra |
| | q0315–q0321 | Retrato + raios azul-claro + gotas |
| | q0322 | Círculo azul com raios brancos radiais (≈ 50% da altura) |
| | q0323 | Círculo branco (≈ 70%) |
| | q0324 | Círculo azul-claro com "cristal" |
| | q0325–q0326 | Orbe branco pequeno (≈ 20% da altura) nas mãos do Ryu pequeno |
| +23 q | q0327 | Retrato some, HUD volta, fundo de linhas fica |
| +28 q | q0332 | 1º acerto do Hyper (6 HIT); depois 7, 8, 9, 10 em q0334, 336, 338, 341 (a cada ≈ 2–3 q) |
| | q0331–q0344 | Respingos ciano-brancos ao longo do chão (≈ 85–95% da altura) |

**Resumo dos tempos:**
- Corte com retrato: **14 q ≈ 580 ms ≈ 35 qj** nos dois Hypers (q0080–q0093, q0313–q0326).
- Do aviso (ativação) até o 1º acerto: ≈ 24 q ≈ 1,0 s (Hyper 2). Do corte até o 1º acerto: ≈ 23 q ≈ 0,96 s (Hyper 1).
- O timer congela durante o corte: o intervalo de 1 unidade foi de 50 q em vez de 35 (seção K).

**Congela?** Durante o corte o oponente não se move (Hyper 2: o Capitão fica parado à direita, q0313–q0326). **Flash?** Sim: estrela no 1º quadro, flash lilás, círculos gigantes branco/azul. **Escurece?** 1 quadro de ≈ 17% antes do corte. Depois o cenário é substituído (não escurecido) pelas linhas coloridas.

**Fundo de linhas do Hyper** [V08 q0085, q0097, q0329]:
- Base violeta-roxa (≈ RGB 70–120, 20–30, 165–215), cobrindo ≈ 50% da área.
- Por cima, faixas horizontais de vermelho, laranja, amarelo, verde, ciano e azul-claro, com comprimentos de ≈ 5–40% da largura e espessura de ≈ 1–3% da altura.
- Saturação muito alta (S ≈ 85) e brilho médio (V ≈ 72).
- **Muda a cada quadro.** Não é um rolamento simples (a busca de deslocamento horizontal não encaixa): parece ciclo de cor/linhas redesenhadas, dando sensação de velocidade horizontal.
- Às vezes o fundo inteiro clareia para lilás/rosa em 1 q (q0100, q0108, q0113, q0314, q0328).
- [CONHECIMENTO: esse fundo de "linhas de velocidade" no Hyper é marca registrada da série Marvel da Capcom.]

**Efeitos durante os acertos do Hyper** (estroboscópio, 1 q cada):
- (a) **Fundo inteiro branco** com os personagens coloridos [V08 q0107, q0110, q0331, q0333].
- (b) **Personagens viram silhuetas brancas** sobre o fundo de linhas (o HUD também clareia em parte) [V08 q0103, q0105, q0336, q0338, q0341].
- (c) **X branco gigante** cruzando a tela inteira [V08 q0104, q0342].
- (d) **Discos/explosões brancas e roxas** no ponto de impacto [V08 q0106, q0108, q0112].
- O **oponente fica todo tingido de azul-violeta** durante o Hyper 2 [V08 q0329–q0344].
- Texto **"DANGER"** amarelo à direita, na altura do contador [V08 q0333–q0344] (não sei o que dispara; cortado pela borda).

**Final.** Hyper 1: corte seco de volta ao cenário, sem pausa extra visível, e a câmera fica no alto ≈ 0,67 s. O final do Hyper 2 não aparece (o vídeo acaba).

**Princípios (G):**
1. **Aviso no próprio personagem antes de tudo** (labareda branca ≈ 170 ms). O jogador vê o Super nascer no sprite.
2. **Troque o cenário inteiro** por um fundo abstrato de alta saturação e movimento horizontal. Isso isola o Super, e o cenário pintado não precisa de versão "escura".
3. Corte de apresentação curto e fixo (≈ 0,6 s) com retrato grande e flashes em círculo. O tempo de jogo para (oponente e timer congelados).
4. Nos acertos do Super, alterne flashes de 1 quadro (fundo branco / silhueta branca / X gigante) a cada 2–3 quadros. O ritmo estroboscópico vende muitos acertos.
5. Tinja o oponente durante o Super: mostra que ele está "preso".

---

## H. REAÇÃO A ACERTO

Quase todo o vídeo é o oponente no ar, preso no canto, então as reações vistas são aéreas.
- **No ar (juggle):** corpo na horizontal, de barriga para cima ou de lado, braços jogados para trás, escudo longe do corpo. Gira para outra pose a cada golpe [V08 q0026, q0144–q0146, q0153–q0154, q0190–q0191] — ver `mshvsf_reacao_oponente.jpg`.
- **Em pé/no canto:** curvado para a frente, com o escudo à frente ou caindo [V08 q0203–q0207].
- **Cabeça/corpo:** a cabeça vai para trás no impacto alto (q0026). Nos impactos no meio do corpo, o tronco dobra.
- **Treme na pausa?** Não consegui ver tremor do atingido. Nas pausas maiores o oponente fica coberto pela faísca (q0022–q0025) ou cortado pela borda da tela.
- **Recuo:** no canto não há recuo horizontal possível. Cada acerto empurra o oponente **para cima**, mantendo-o no ar a ≈ 20–60% da altura da tela [V08 q0143–q0147].
- **Defesa:** não aparece.
- **Queda:** cai de cabeça → deitado (pernas para cima) → rola → guarda, em ≈ 4–5 q do toque à guarda (≈ 10–12 qj). Sem quique nem poeira visíveis [V08 q0250–q0259].
- **Cor no atingido:** nos normais, nenhum piscar branco/vermelho. No Hyper 2, o oponente fica inteiro azul-violeta, e nos flashes vira silhueta branca [V08 q0329–q0344, q0336].
- **Mancha vermelha de 1 quadro (não identificada).** Em vários socos curtos aparece, por 1 q, uma mancha **vermelha chapada em forma de respingo** (com gotas escorrendo), cobrindo ≈ 40–45% da altura da tela por cima do Ryu e do oponente. No quadro seguinte, um disco vermelho menor (≈ 8% da altura) aparece na cintura do Ryu [V08 q0140→q0142, q0181, q0186→q0188, q0226→q0228, q0287, q0299]. Pode ser um efeito do jogo/versão modificada ou falha de emulação. **Não copiar sem saber o que é:** como está, parece sangue e esconde os dois personagens.

**Princípios (H):**
1. No ar, cada acerto deve mudar a pose do atingido (2–3 poses de "juggle" alternadas). Isso deixa cada acerto legível mesmo com a faísca em cima.
2. No canto, o empurrão vira para cima: o atingido "sobe" a cada golpe em vez de recuar.
3. Levantar rolando e rápido (≈ 10–12 qj) depois da queda.
4. Tingir o atingido durante o Super (cor única) é mais claro que piscar.
5. Nenhum efeito deve esconder os dois personagens por inteiro por mais de 1 quadro.

---

## I. IMPACTO

**Pausa no acerto (hitstop)**, medida pela pose do Ryu segurada a partir do contato (±2,5 qj):
| Golpe | Pausa vista | ≈ qj a 60 Hz |
|---|---|---|
| Soco reto (X branco) [q0204–q0205] | 2 q (q0205 idêntico a q0204 no corpo) | ≈ 5 (±2,5) |
| Soco curto (feixe + estilhaços) [q0209–q0211] | ≈ 2 q | ≈ 5 |
| Chute alto (X branco) [q0195–q0197; q0218–q0220] | 3 q | ≈ 7–8 |
| Lançador [q0073–q0074] | 2 q | ≈ 5 |
| Início da gravação: soco [q0010–q0012] e chute [q0022–q0025] | 3 q e 4 q, tela inteira congelada (até a faísca) | ≈ 7–10. Pode ser travamento da reprodução |
| Acertos do Hyper [q0103–q0113, q0332–q0341] | não há pausa visível; acerto a cada 2–3 q | ≈ 5–7 entre acertos |
| Defesa | não aparece | — |

[CONHECIMENTO: os jogos Marvel da Capcom usam hitstop relativamente curto para permitir combos longos; os números acima batem com isso.]

**Tremor de tela.** Nenhum nos normais (0 px medido). No Hyper o tremor é substituído por flashes.

**Contra-ataque.** Não identificado no vídeo.

**Câmera lenta.** Não.

**ORDEM quadro a quadro — chute alto que acerta** [V08 q0190–q0199]:
1. q0190: chambra (corpo girado, joelho alto) — antecipação.
2. q0191: perna subindo (pose intermediária).
3. q0192–q0194: perna estendida acima da cabeça, **sem faísca ainda**; o oponente cai no ar em direção ao pé.
4. **q0195: contato → X branco gigante aparece já no tamanho máximo** (dois feixes cruzados de ≈ 50% × 70% da tela), com pontinhos laranja. O oponente fica quase todo coberto. Contador sobe (35).
5. q0196: o X some e fica um **disco branco** (≈ 9% da altura) no pé + pequenos fragmentos; o Ryu segura a pose (pausa).
6. q0197: Ryu ainda na pose, faísca acabou; o oponente começa a ser empurrado para cima.
7. q0198–q0199: Ryu recolhe a perna (2 poses).
8. q0200–q0201: dash para a próxima volta.

**Ordem — soco reto** [V08 q0202–q0206]:
1. Braço estendido (q0203), com **poeira no pé de trás** (resto do dash).
2. q0204–q0205: X branco na mão + pausa de 2 q.
3. q0206: recolhe; estilhaços amarelos ficam no oponente.

Resumo da ordem: **contato → faísca no tamanho máximo no 1º quadro → núcleo/disco no 2º → pausa de 2–3 q com a pose segurada → empurrão → restos (estilhaços amarelos) somem em 2–3 q.** Não há tremor de tela nem cor no atingido nos normais.

**Princípios (I):**
1. A faísca nasce no tamanho máximo, no quadro do contato. Nada de crescer devagar.
2. Pausa curta (≈ 5–8 qj) nos normais, para combos longos continuarem fluidos. O peso vem da faísca gigante, não da pausa longa.
3. O golpe pode "esperar" o oponente com a pose estendida antes do contato (perna no ar 3 q). Isso deixa o acerto legível.
4. Restos da faísca (estilhaços, disco) continuam depois da pausa e fazem a ponte com a reação.
5. Guarde tremor e flash de tela cheia para o Super. A distinção fica mais forte.

---

## J. EFEITOS VISUAIS

**Faíscas de acerto (todas 2D, desenhadas, em sprite)** — `mshvsf_faiscas.jpg`:

| Tipo | Onde (exemplos) | Forma | Cor | Tamanho (% da altura da tela; Ryu = 40%) | Duração |
|---|---|---|---|---|---|
| **Feixe horizontal** | soco curto/reto [q0010, q0164, q0209, q0300] | linha reta longa e fina na direção do soco, com explosão de "chamas" amarelas no ponto de contato | branco no centro, borda amarelo-pálida; chamas amarelo-claro | comprimento ≈ 60–100% da **largura**, espessura ≈ 2–3% da altura; explosão ≈ 15–20% | 1 q de feixe + 2–3 q de estilhaços |
| **X laranja** | soco forte / chute médio [q0159, q0173, q0182] | dois feixes cruzados (diagonais) + estrela branco-amarela no centro + pontinhos amarelos | laranja-avermelhado nas pontas, núcleo branco-amarelo | braços ≈ 60–70% da altura; núcleo ≈ 8–10% | 1 q (depois vira disco) |
| **X branco** | chute alto, lançador, golpes fortes [q0022, q0073, q0195, q0204, q0218] | dois feixes cruzados largos, em cunha (finos nas pontas) + pontinhos laranja | branco puro | braços ≈ 70–90% da altura (≈ 2× o Ryu); núcleo ≈ 10% | 1–2 q (até 4 q no começo da gravação) |
| **Feixe diagonal** | rasteira [q0305] | uma linha única na diagonal (de cima-esquerda para baixo-direita) + pontinhos amarelos | branco | ≈ 60% da diagonal da tela | 1 q |
| **Disco branco** | 2ª fase de quase todas [q0074, q0196, q0210] | círculo cheio, borda levemente suave | branco | ≈ 8–10% | 1 q |
| **Estilhaços** | restos [q0141–q0143, q0187–q0189] | lascas serrilhadas tipo "raio", soltas, saindo do oponente | amarelo-pálido/creme | ≈ 15–25% de área total | 2–3 q, somem sem encolher |
| **Arco fino** | lançador [q0074] | arco de círculo fino em volta do impacto | branco | ≈ 30% de diâmetro | 1 q |

**Leitura importante:** o **ângulo do feixe segue a direção do golpe**:
- soco reto → feixe horizontal;
- rasteira → diagonal;
- golpes fortes → X (duas direções).
A força parece ir do feixe (fraco), ao X laranja (médio), ao X branco (forte). Essa associação força × cor é minha hipótese pelo padrão do loop, não confirmada [CONHECIMENTO incerto]. Nenhuma faísca gira. Elas não "expandem": aparecem já grandes e no quadro seguinte viram o disco central.

**Faísca de defesa.** Não aparece.

**Poeira.** Nuvem cinza-bege, ≈ 6–7% da altura, no pé de trás ao parar o dash/plantar o pé, 2–3 q [V08 q0137–q0138, q0159–q0160, q0203–q0205, q0226–q0228].

**Rastros/smears.** Não há smear no sprite dos normais. Rastros vistos:
- coluna de "fumaça" azul-violeta no soco ascendente do Hyper 1 [q0100–q0115];
- respingos ciano-brancos rasantes ao chão no Hyper 2 [q0331–q0344].

**Auras/brilhos.** Labaredas creme/brancas na ativação do Hyper (≈ 25–30% da altura, 4 q) [q0308–q0311]. Raios azul-claro e gotas no corte do Hyper [q0081–q0088, q0315–q0321].

**Efeitos de tela.** Ver seção G: fundo branco, silhueta branca, X gigante, círculos branco/azul de 50–80% da altura.

**Partículas do cenário reagindo.** Nenhuma. O público não reage aos golpes de forma visível: o ciclo segue o mesmo ritmo.

**Mancha vermelha de 1 quadro.** Ver seção H (não identificada).

**Princípios (J):**
1. Faísca = **linha(s) na direção do golpe** + núcleo claro. Um soco horizontal ganha feixe horizontal; um golpe forte ganha X. Isso comunica direção e força de uma vez.
2. Faíscas de golpes fortes **maiores que o personagem** (≈ 2× a altura) e brancas: em cenário colorido, branco puro é o que mais salta.
3. Faísca em 2–3 fases curtas: forma máxima (1 q) → disco/núcleo (1 q) → estilhaços soltos (2–3 q). Total de ≈ 4–5 q (≈ 10–12 qj).
4. Pontinhos coloridos (laranja/amarelo) dentro da faísca dão "textura" de explosão sem partícula de verdade.
5. Poeira só onde o pé planta com força (parada de dash, pouso). Pequena (≈ 6% da tela) e curta (2–3 q).

---

## K. HUD E TEXTOS

`mshvsf_hud.jpg`:

**Barras de vida** [V08 q0229, q0259]:
- Em cima: Ryu da esquerda (x ≈ 7–46%), oponente da direita (x ≈ 55–93%). **≈ 39% da largura cada**, a ≈ 4–9% da altura.
- Forma: barra **amarela** chapada e grossa, com uma faixa marrom-oliva embaixo (a "base"), pontas cortadas na diagonal e contorno preto.
- No oponente aparece um trecho **vermelho** dentro da barra (dano parcialmente recuperável) [CONHECIMENTO: na série Marvel, o vermelho é vida que regenera com o personagem fora do combate].
- Retratos pequenos nos cantos (≈ 7% da largura): Ryu à esquerda, Capitão à direita.
- Nome do personagem em verde itálico perto da barra. Embaixo, nome do parceiro ("SHUMA-GORATH") em verde itálico grande, a ≈ 11–14% da altura.
- Acima de cada barra, o rótulo "NEW HERO" em letras brancas/amarelas grossas (cortado pelo topo).

**Timer** [V08 q0229]:
- No centro (x ≈ 47–55%, y ≈ 1–10%): dígitos **ciano** com contorno escuro, ≈ 7% da altura, sobre um emblema de grade verde ("teia" em perspectiva).
- **Ritmo: 1 unidade a cada ≈ 35 q ≈ 1,46 s** (mudanças em q0052, 0102, 0137, 0172, 0207, 0241, 0276, 0311). Intervalo de 50 q durante o Hyper 1, porque o timer congela no corte.

**Barra de Hyper** [V08 q0229, q0343]:
- Embaixo (y ≈ 93–99%): esquerda x ≈ 9–49%, direita x ≈ 52–92%.
- Textura laranja/amarela "granulada" (tipo lava/purpurina) com uma ponta rosa (esquerda) ou azul (direita) mostrando a parte enchendo.
- Número do nível grande em verde ("2", "1") acima da palavra **"LEVEL"** em amarelo com contorno preto.
- Durante o Hyper 1 o nível do Ryu aparece "1"; já está de volta a "2" em q0229 (a barra enche com os acertos), e depois do Hyper 2 vira "1" de novo [V08 q0096, q0229, q0327].

**Contador de combo "NN HIT"** [V08 q0229, q0288–q0291]:
- À esquerda, x ≈ 0–28%, y ≈ 28–43%.
- Dígitos grandes (≈ 14% da altura) em **degradê amarelo → laranja** com contorno duplo (azul-petróleo + escuro).
- "HIT" menor (≈ 6–7%) em amarelo, alinhado pela base, à direita dos dígitos.
- **Aparece só no 2º acerto** ("2 HIT", q0288).
- **Entra deslizando da borda esquerda em ≈ 2–3 q** (≈ 100 ms) [q0288–q0290].
- Ao subir, só troca o número, sem "pulo" de escala.
- Some quando o combo acaba [q0270 → q0271].
- Fica por cima de tudo, inclusive do fundo do Hyper.

**Texto de evento "MARVELOUS"** [V08 q0271–q0287]:
- Depois do fim do combo de 39, entra **deslizando da esquerda** (q0271 só "OUS", q0272 "RVELOUS", q0273 "ARVELOUS", q0274 completo): ≈ 4 q ≈ 170 ms.
- Mesma altura do contador (≈ 33–40%), letras amarelas com contorno escuro, ≈ 7–8% da altura.
- Fica ≈ 13 q (≈ 0,55 s) até ser substituído pelo "2 HIT" do novo combo [q0287–q0288].
- [CONHECIMENTO: a série Marvel premia combos longos com textos como "GREAT/WONDERFUL/MARVELOUS".]

**"DANGER"** à direita, amarelo, mesmo estilo de letra [V08 q0333–q0344]. Não identifiquei o que dispara.

**Indicador fora da tela** "1P" (silhueta amarela + triângulo azul apontando para baixo), no canto inferior direito quando o Ryu está abaixo da câmera [V08 q0121–q0133].

**ROUND/FIGHT/KO.** Não aparecem.

**Tipografia.** Toda em letras grossas, itálicas, com contorno escuro e preenchimento em degradê amarelo-laranja (contador, MARVELOUS, LEVEL). Números do timer em ciano "digital".

**Quanto o HUD chama atenção.** Muito. Amarelo saturado nas barras, verde nos nomes, contador grande à esquerda. O HUD compete com o cenário, mas como fica nas bordas (≈ 15% em cima, ≈ 8% embaixo, contador na lateral esquerda) não cobre os lutadores no canto direito.

**Princípios (K):**
1. Contador de combo grande, lateral, na altura do peito, que só aparece no 2º acerto e entra deslizando (≈ 100 ms).
2. Texto de prêmio no fim do combo (MARVELOUS) na mesma posição do contador, também deslizando. Reaproveita o lugar e o estilo.
3. Barras de vida grossas e de cor única (amarelo), com dano parcial numa 2ª cor (vermelho).
4. Barra de Super embaixo, com o número do nível grande. Lê de relance quantos Supers há.
5. Indicador de personagem fora da tela sempre que a câmera vertical o perde.

---

## L. SOM

- **Há áudio:** AAC, 44,1 kHz, 14,2 s (≈ 0,3 s mais curto que o vídeo, então o sincronismo exato não é confiável).
- Volume RMS por janela de 1/24 s: **quase constante**, entre ≈ 49 e 59 dB (mediana 54,6; faixa dinâmica pequena, ±3 dB). Isso indica música + efeitos sonoros contínuos e densos (combo sem parar).
- **Silêncio de ≈ 0,3 s** perto de 7,1–7,4 s (janelas 172–178). Provável falha da gravação.
- Detectei picos de agudos (filtro passa-alta 2,5 kHz, subida > 4 dB) em ≈ 21 instantes, sem correlação clara com os quadros dos acertos (com o atraso de áudio incerto). **Não consigo afirmar** se há voz, nem qual som marca cada acerto.

**Princípios (L)** (cautelosos, sem dados suficientes):
1. Num combo longo, o som de acerto tem que se destacar de uma base sonora densa. Aqui o volume quase não varia, então o "estalo" depende de timbre agudo, não de volume.
2. Sincronize o som com o quadro do contato (o mesmo da faísca máxima).
3. Valide o áudio com gravações limpas. Esta não serve para medir.

---

## M. FLUXO DO ROUND

Não aparece intro, anúncio do round, KO nem vitória. O que se vê do fluxo:
- O timer corre a ≈ 1,46 s por unidade e **congela durante o corte do Hyper**.
- O fim de um combo gera um texto de prêmio (MARVELOUS) por ≈ 0,55 s, mas sem pausa de jogo: Ryu já está no ar atacando de novo [V08 q0271–q0287].
- O oponente derrubado levanta em ≈ 10–12 qj e o jogo segue sem cerimônia [V08 q0254–q0259].

**Princípios (M):**
1. Timer congelado durante a apresentação do Super.
2. Textos de evento não param o jogo.
3. Levantar rápido mantém o fluxo.

---

## O que este jogo faz que mais o faz parecer profissional (ordem de impacto)

1. **Faíscas gigantes, brancas e direcionais.** X ou feixe na direção do golpe, ≈ 2× a altura do personagem, no tamanho máximo já no quadro do contato, depois disco e estilhaços. Num cenário saturado, o branco puro é o que manda [V08 q0195, q0209, q0305].
2. **Fundo do Hyper trocado por linhas coloridas animadas** + flashes estroboscópicos de 1 quadro (fundo branco / silhueta branca / X que cruza a tela). O Super parece outro "mundo" [V08 q0097, q0107, q0336, q0342].
3. **Câmera vertical que sobe uma tela inteira no super pulo**, revelando um andar novo do cenário (sacadas, bandeirolas), com paralaxe. Dá escala e espetáculo ao combo aéreo [V08 q0230–q0237].
4. **Sombra elíptica preta e opaca, maior que o corpo** (≈ 22% × 6%), num chão claro e liso: prende os lutadores ao chão e os separa de um fundo cheio de gente [V08 q0229].
5. **Animação de sprite limitada e bem escolhida.** 3–5 desenhos por golpe, antecipação de 1 desenho, pose de impacto exagerada (braço/perna além do natural) e segurada 3–6 quadros [V08 q0190–q0199, q0072].
6. **Contorno escuro de 1 pixel nativo + cel shading de 2–3 tons.** Os personagens leem mesmo sem escurecer o fundo [V08 q0259].
7. **Aviso de Super no próprio sprite** (labareda branca ≈ 170 ms) antes do corte de ≈ 0,6 s com retrato e círculos de luz, com o timer congelado [V08 q0308–q0326].
8. **Contador "NN HIT" grande e estilizado** (degradê amarelo-laranja, contorno duplo) que entra deslizando, e o prêmio "MARVELOUS" no mesmo lugar ao fim do combo [V08 q0288–q0290, q0271–q0274].
9. **Dash baixo com poeira na parada.** Pose alongada de 1–2 desenhos + nuvem no pé de trás dão velocidade e peso com quase nenhum custo [V08 q0200–q0205].
10. **Cenário vivo, mas que não compete:** público animado em ciclos lentos (≈ 8 poses/s, com pausas), detalhe denso só atrás dos lutadores, blocos grandes de cor chapada no alto. Indicador "1P" quando a câmera perde alguém [V08 q0149–q0198, q0121].

---

### Observações de método e limitações
- Pausas ≤ 1 q (≤ 2,5 qj) não são medíveis com segurança. Os congelamentos de 3–4 q no começo (q0009–q0029) não se repetem nas voltas seguintes do mesmo loop e podem ser travamento da reprodução.
- A compressão do YouTube + gravação do celular borra contornos e detalhes finos. Espessura de contorno e smears são estimativas.
- As medidas de subida da câmera no super pulo são grosseiras (80–120% da tela). No pulo comum (≈ 15%) são mais confiáveis.
- Possível versão modificada do jogo ("NEW HERO", retrato no Hyper, mancha vermelha de 1 quadro): use como referência visual, não como documentação do arcade original.
