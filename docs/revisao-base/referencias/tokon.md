# Referência — Marvel Tōkon: Fighting Souls (Arc System Works) — trailer de gameplay do Ciclope Fênix

Material analisado: três gravações de tela de celular tocando o trailer no YouTube.
- **V09** (15,5 s, 368 quadros extraídos) — mansão Xavier (dia). Ciclope Fênix × Tempestade.
- **V10** (11,3 s, 272 quadros) — mansão (fim da luta anterior) → transição Magneto → selva com vulcão. Ciclope × Magneto.
- **V11** (6,6 s, 156 quadros) — rua de Nova York (42 St) → Super cinemático. Ciclope (traje preto) × Capitão América / Wolverine.

Convenções:
- `[V09 q0238]` = vídeo V09, quadro extraído nº 238 (`vref/v09/f0238.jpg`). `[CONHECIMENTO]` = não vem do vídeo.
- 1 quadro da gravação ≈ 41,7 ms ≈ 2,5 quadros do jogo a 60 Hz. Toda duração tem margem de ±1 quadro da gravação (±42 ms, ±2,5 f@60). O gravador do celular repete quadros (V09 tem 75 quadros-fonte duplicados, V11 tem 15): uma "pausa" de 1 quadro pode ser artefato; só considerei pose segurada a partir de 2 quadros seguidos.
- Medidas em % da **área de jogo** (no vídeo original 2400×1080 a área é x = 240…2159, ou seja 1920×1080 — barras pretas de 240 px de cada lado). Legendas do YouTube, avisos "Gravação de tela salva", botão de compras e menus do YouTube **não são do jogo**.
- Brilho (L, 0–255) e saturação (S, 0–1) foram medidos em Python sobre quadros em resolução cheia.

## 0. O que é gameplay e o que é cinemática/edição de trailer

| Trecho | Conteúdo | Tipo |
|---|---|---|
| V09 q0001–q0024 | vídeo pausado no YouTube (barra de progresso vermelha, "‹ -10") — mesma imagem de explosão de fogo | lixo (pausa) |
| V09 q0025–q0037 | logo MARVEL GAMES sobre papel creme | edição |
| V09 q0038–q0060 | câmera baixa nos pés do Ciclope, Wolverine caído na grama | cinemática |
| V09 q0061–q0119 | menu de legendas do YouTube por cima | lixo |
| V09 q0120–q0140 | close extremo do visor (rosto ocupa 100% da altura) | cinemática |
| V09 q0141–q0180 | cartão "FEEL THE MIGHT OF / PHOENIX CYCLOPS" | edição |
| **V09 q0181–q0352** | **luta na mansão, Ciclope × Tempestade, inclui o Super "OPTIC RAGE"** | **gameplay** (≈7,2 s) |
| V09 q0353–q0368 | corte para outra luta (mansão, tarde) com outra oponente | gameplay |
| V10 q0001–q0048 | mansão: Ciclope vira coluna de fogo e acerta oponente | gameplay (Super de fogo) |
| V10 q0049–q0114 | lâminas/cristais roxos de metal, Magneto em close | cinemática/transição |
| **V10 q0115–q0272** | **selva com vulcão, Ciclope × Magneto, HUD parcial e faixa "GAIN SOLAR SHARDS"** | **gameplay** |
| V10 q0181–q0188 | dissolve (fusão de duas tomadas) — palmeira e torre aparecem por cima do vulcão sem a câmera se mover | edição |
| **V11 q0001–q0059** | **rua de NY, Ciclope (traje preto) × Capitão América, Wolverine entra** | **gameplay** |
| V11 q0060–q0156 | câmera baixa inclinada, chicote de câmera, carro explode, parque, onda de fogo cobre a tela, close do Ciclope de armadura Fênix | cinemática de Super (ou transição de cenário) |

Importante: **nenhum dos três trechos mostra barra de vida, timer, ROUND/FIGHT ou KO** — o trailer tira o HUD. Tudo de HUD abaixo vem do medidor "Solar Shards" e da faixa de tutorial em V10.

Folhas montadas (em `img/`):
- `tokon_medidas_grade.jpg` — 4 quadros com grade de 10% (V09 q0238, V11 q0004, V10 q0229, V10 q0115).
- `tokon_super_sequencia.jpg` — Super OPTIC RAGE, 16 quadros (V09 q0293–q0352).
- `tokon_faisca_impacto_feixe.jpg` — feixe óptico acertando Magneto (V10 q0151–q0168).
- `tokon_antiaereo_raio.jpg` — carga do olho + raio diagonal + anel final (V09 q0203–q0214).
- `tokon_rasteira_fogo.jpg` — rasteira com explosão de fogo no pé (V09 q0185–q0192).
- `tokon_dash_poeira.jpg` — dash com nuvem de fumaça desenhada (V11 q0025–q0039).
- `tokon_fogo_camadas.jpg` — 4 recortes de fogo em resolução cheia.
- `tokon_render_lutador.jpg` — Ciclope nos três cenários em resolução nativa (contorno, rim light).
- `tokon_contorno_storm.jpg` — Tempestade com contorno dourado colorido.
- `tokon_poses_seguradas.jpg` — sequências de pose (V11 q0002–q0013, V10 q0225–q0236).
- `tokon_super_cinematico_v11.jpg` — Super cinemático (V11 q0060–q0155).
- `tokon_textos_tela.jpg` — cartões de título, faixa de tutorial e medidor.

---

## A. TELA E CÂMERA

**Altura do personagem em pé** (topo da cabeça → sola):
- Ciclope na mansão: ≈ 47% → 95% = **≈ 48% da altura** [V09 q0238].
- Ciclope na selva: ≈ 43% → 93% = **≈ 50%** [V10 q0229].
- Ciclope na rua: ≈ 37% → 94% = **≈ 57%** [V11 q0004] (tomada mais fechada).
- Magneto (capa e elmo, levemente no ar): ≈ 29% → 92% ≈ 63% [V10 q0115].
- Figurantes do fundo (Noturno, Homem de Gelo, Gambit, civis): ≈ 18–30% da altura [V09 q0214, V11 q0004].
→ Lutador ocupa **metade da tela**. Bem maior que SF2 (≈ 1/3) — o jogo é "câmera colada".

**Linha do chão**: pés em **≈ 92–95% de cima para baixo** [V09 q0238, V10 q0229, V11 q0004]. Quase não sobra chão visível; o chão é uma faixa de ≈ 5–10% na base. Na selva, a grama do primeiro plano chega a cobrir os pés [V10 q0115].

**Distância entre lutadores**: varia muito — ≈ 25% da largura (Ciclope x≈38%, alvo x≈63%) [V11 q0004]; ≈ 35% [V10 q0115: x≈32% e 68%]; ≈ 65% [V10 q0229: x≈17% e 82%]. Não há quadro de início de round.

**Zoom**: não foi possível isolar zoom dinâmico — as tomadas diferentes (48% × 57%) podem ser cortes de câmera do trailer. Dentro da tomada da selva (V10 q0115–q0172) a câmera ficou **parada** (deslocamento medido = 0 px por correlação de fase no céu) mesmo com o Ciclope pulando e o Magneto no ar.

**Câmera horizontal**: acompanha o dash — desloca ≈ 6–18 px por quadro numa área de 960 px = **≈ 0,6–1,9% da largura por quadro da gravação** [V11 q0023–q0035], parando junto com o lutador [V11 q0036].

**Câmera sobe no pulo?** Pouco: no pulo do Ciclope contra a Tempestade no ar o fundo desce ≈ 11 px de 540 ≈ **2% da altura** em 4 quadros e volta em 3 [V09 q0218–q0224]. Com Magneto no ar a ≈ 30% de altura, a câmera não subiu [V10 q0119–q0130] — o enquadramento já deixa ≈ 40% de céu acima das cabeças.

**Tremor de tela**: quase inexistente nos acertos normais/feixe — 0 px durante 20 quadros de feixe acertando Magneto [V10 q0151–q0172]. Na parede de fogo há deriva pequena de 1–4 px (≈ 0,1–0,4% da largura) por quadro [V09 q0248–q0278]; no golpe que joga o Capitão longe, ≈ 3 px × 5 px (0,3% × 0,9%) por 2 quadros [V11 q0051–q0052]. A sensação de impacto vem de faíscas e pausa, não de chacoalhar.

**Câmera nos Supers** (detalhe em G): corte para painel/cartão inclinado ≈ 5–10° com o personagem em close (≈ 100–130% da altura da tela, cortado) [V09 q0298–q0313]; volta ao cenário em **tons de cinza** com o lutador colorido [V09 q0317–q0324]; no fim, a câmera "entra" no feixe até a tela ficar branca [V09 q0347–q0348]. No Super cinemático de V11: câmera baixa com inclinação (dutch) ≈ 10–15° [V11 q0060–q0068], chicote de câmera com borrão [V11 q0069–q0072], mudança de área do cenário [V11 q0073–q0099].

**HUD**: o trailer remove o HUD. Visível só: medidor de Solar Shards no canto inferior esquerdo (x ≈ 5–25%, y ≈ 75–87%, ≈ 12% da altura) [V10 q0141] e faixa de tutorial no inferior direito (x ≈ 41–100%, y ≈ 72–90%, ≈ 18% da altura) [V10 q0229]. Topo da tela: 0% de HUD em todas as tomadas.

## B. CENÁRIO

**Camadas percebidas** (todas pintadas/estilizadas, sem fotorrealismo):
- Mansão [V09 q0238]: (1) grama e caminho de tijolo rosa na base; (2) sebes recortadas, bancos, figurantes X-Men; (3) fachada de tijolo vermelho com detalhes brancos, letreiro "XAVIER'S SCHOOL"; (4) árvore de flores rosa na esquerda; (5) arranha-céus azulados com névoa; (6) céu azul com nuvens brancas.
- Selva [V10 q0115]: (1) grama alta escura no primeiro plano (cobre os pés); (2) plantas de folha larga verde/azul; (3) dinossauro (estegossauro) e pterossauros; (4) vulcão com lava laranja e fumaça rosa; (5) torre/nave sci-fi à direita; (6) céu lilás-alaranjado com nuvens e meteoros riscando.
- Rua NY [V11 q0004]: (1) asfalto molhado com reflexo roxo e brasas; (2) táxi amarelo, carro vermelho, civis (homem no táxi, ciclista, pessoas caídas); (3) letreiro de teatro "7:00PM EVERY NIGHT / DAZZLER'S DISCO SHOW", semáforo e placa verde "42 ST"; (4) prédios em azul/rosa pastel; (5) céu azul claríssimo.

**Paralaxe**: só observável no dash [V11 q0023–q0035]; as amostras de correlação no céu e nos prédios moveram-se juntas (sem diferença mensurável na resolução da gravação). Não dá para afirmar paralaxe forte.

**Hierarquia lutador × fundo (medida)** — caixa sobre o lutador vs. caixa de fundo na mesma altura:

| Quadro | Lutador L / S | Fundo à altura dos lutadores L / S | Céu/topo L / S |
|---|---|---|---|
| Mansão [V09 q0238] | 73 / 0,46 | 101 / 0,36 | 153 / 0,24 |
| Selva, Ciclope [V10 q0229] | 76 / 0,61 | 125 / 0,33 | 103 / 0,39 |
| Selva, Magneto [V10 q0115] | 77 / 0,69 | 124 / 0,35 | 160 / 0,26 |
| Rua, traje preto [V11 q0004] | 109* / 0,30 | 157 / 0,51 | 169 / 0,35 |
(*caixa inclui fundo entre as pernas)

Leitura: **o lutador é ≈ 30–50 pontos de brilho mais ESCURO que o fundo da faixa de luta e, na maioria, mais SATURADO** (vermelho puro S 0,46–0,69 contra fundo 0,33–0,36). O fundo é "pastel claro"; o lutador é "escuro e puro". Mesmo com cenários coloridíssimos, a separação é de **valor** (claro × escuro), não de matiz. Quando o lutador é preto (V11), o fundo é ainda mais claro (L 157–169) e o lutador ganha contorno de luz azul (ver C).

Outros recursos de legibilidade:
- Perspectiva atmosférica: quanto mais longe, mais claro e menos saturado (arranha-céus azul-acinzentados [V09 q0238], prédios pastel quase brancos [V11 q0004]).
- Fundo com bordas mais macias/pintadas e pouco contraste local (desvio de brilho do fundo 23–57 vs. lutador 56–82) [V09 q0238, V11 q0004].
- Complementaridade: Ciclope vermelho na selva verde [V10 q0229]; Magneto vermelho/roxo sobre céu lilás claro [V10 q0115].
- Figurantes do fundo menores (≈ 20–30% de altura), mais claros e alguns translúcidos (Homem de Gelo azul-claro quase branco) [V09 q0214].

**Detalhe na faixa da luta × resto**: o detalhe fino (tijolos, letreiros, folhas) está na faixa média 25–75% da altura; a faixa do chão é simples (grama, asfalto com reflexo). A grama do primeiro plano na selva é escura (L baixo) e moldura a base [V10 q0115].

**Elementos animados**: figurantes reagindo (civil abrindo porta do táxi, pessoas caídas) [V11 q0004]; brasas pulsando no chão após o fogo [V11 q0001–q0009]; meteoros riscando o céu da selva [V10 q0115]; figurantes X-Men posando [V09 q0214]. Intensidade baixa: movem pouco e não competem com o lutador.

**Chão**: asfalto molhado com reflexo roxo difuso e brasas laranja espalhadas [V11 q0004]; grama com textura de pinceladas [V09 q0238]. Sombra dos personagens: não distinguível na gravação (se existe, é pequena e escura sob os pés; não consegui medir).

**Luz do cenário nos personagens**: o rim light (contraluz azul-lavanda) aparece igual nos três cenários [V09 q0238, V10 q0229, V11 q0004] — parece ser do personagem, não do cenário. O fogo próximo deixa o lado do corpo alaranjado [V10 q0029].

**Paleta**: mansão = vermelho tijolo, rosa, verde, azul céu; selva = verde-esmeralda, lilás, laranja-lava; rua = azul claro, rosa pastel, amarelo táxi. Todas com céu/longe claro e pastel.

**Princípios (B)**
1. Fundo em valor médio-claro e pastel; lutador escuro e saturado: a diferença de **brilho** (≈ 30–50 em 255) é o que garante a leitura, mesmo com fundo colorido.
2. Detalhe e contraste local do fundo menores que os do lutador; bordas do fundo mais macias que o contorno do lutador.
3. Perspectiva atmosférica forte: longe = claro e dessaturado.
4. Escolher a cor dominante do lutador em oposição ao cenário (vermelho × verde, vermelho × céu lilás claro).
5. Elementos animados do fundo pequenos e lentos; figurantes menores, claros ou translúcidos.

## C. PERSONAGENS (RENDER)

**Estilo**: modelos 3D renderizados como anime (cel-shading), câmera lateral — o "2.5D" da Arc System Works [CONHECIMENTO: mesma linha de Guilty Gear Xrd/Strive e Dragon Ball FighterZ].

**Contorno**: existe, escuro (quase preto, às vezes marrom-escuro), **≈ 2–3 px a 1080p** na escala de jogo [V09 q0193, V11 q0004]; nos closes continua fino (≈ 3–5 px) em vez de engrossar proporcionalmente [V09 q0125]. Há também **contorno colorido por peça**: a capa da Tempestade tem borda dourada/amarela ≈ 3–4 px [V09 q0193, `tokon_contorno_storm.jpg`]. Linhas internas desenhadas: músculos do traje preto marcados por linhas azul-lavanda [V11 q0004].

**Sombreamento**: **2 tons com corte duro**, sem degradê no corpo. Vermelho do traje: luz ≈ #EC457A, sombra ≈ #9C051C [V09 q0238]. Peças douradas com 1 brilho especular branco. O preto do traje é preto chapado com linhas de luz [V11 q0004].

**Contraluz/borda iluminada**: forte e constante — faixa **azul-lavanda ≈ #8FB8D4** no lado escuro do corpo, ≈ 19% dos pixels da silhueta do Ciclope de traje preto [V11 q0004]; também no Ciclope vermelho (lavanda nas bordas dos ombros e braços) [V09 q0298, V10 q0229]. É o que separa um personagem preto de um fundo escuro.

**Silhueta (2 exemplos)**:
1. Ciclope Fênix: ombreiras vermelhas pontudas e largas formam um **triângulo invertido** (ombros ≈ 2,5× a largura da cintura), pernas finas, visor amarelo/laranja como único ponto claro na cabeça [V09 q0238].
2. Tempestade: capa branca aberta como asas + massa de cabelo branco = grande forma branca contra tijolo vermelho; reconhecível só pela mancha [V09 q0193].
(3. Magneto: capa roxa ondulando = metade da área da silhueta [V10 q0115].)

**Proporções**: heroicas, ≈ 8 cabeças; cabeça pequena, mãos e ombros grandes [V09 q0238, V11 q0004].

**Taxa de animação**: animação **limitada** — poses seguradas. Exemplos:
- Ciclope parado antes do raio: q0003 = q0004 (segura 2 quadros ≈ 83 ms ≈ 5 f@60), depois 3 poses novas seguidas (q0005–q0007), q0008 ≈ q0007, q0009 nova [V11 q0003–q0009].
- Wolverine segura a pose 3 quadros (diferença local 1–2 em 255) [V11 q0053–q0055] ≈ 125 ms ≈ 7–8 f@60.
- Idle do Ciclope quase imóvel por 5 quadros (diferença local 2,6–8) [V10 q0229–q0233].
- Close final segura **20 quadros parados** (≈ 0,83 s) [V11 q0129–q0148].
Estimativa: **≈ 10–14 poses distintas por segundo** na gravação (teto 24). A gravação não resolve 60 Hz; [CONHECIMENTO] a ASW anima modelos 3D por poses-chave sem interpolação, tipicamente segurando cada pose 2–3 quadros a 60 Hz (≈ 20–30 poses/s), e mais tempo nas poses de impacto.

**Princípios (C)**
1. Contorno escuro fino e constante (2–3 px a 1080p), com contorno colorido em peças grandes claras (capa).
2. Duas cores por material, corte duro; nada de degradê no corpo.
3. Rim light de cor fria (azul-lavanda) no lado da sombra — obrigatório para personagens escuros.
4. Silhueta com uma forma-assinatura grande (ombreiras em V, capa, cabelo) legível como mancha.
5. Animação em poses seguradas; segurar mais nas poses-chave.

## D. ANIMAÇÃO POR ESTADO

- **Idle/guarda**: Ciclope de pé, tronco levemente inclinado, mãos abertas na altura do quadril; quase imóvel (amplitude < 1% da altura) [V10 q0229–q0233; V09 q0238]. Movimento secundário: nada visível (sem capa no Ciclope). Magneto: a capa ondula continuamente mesmo parado [V10 q0115–q0130].
- **Andar**: não aparece claramente.
- **Dash para frente**: [V11 q0025–q0035, `tokon_dash_poeira.jpg`] — 1 quadro de transição (q0025, ainda de pé) → pose de corrida inclinada ≈ 45°, braços para trás, segurada quase igual por ≈ 9 quadros (≈ 375 ms) enquanto a câmera acompanha → frenagem com pose de empurrão (q0036). **Nuvem de fumaça desenhada** sai do ponto de partida no 2º quadro do dash (q0027): bolhas brancas com sombra lavanda (#DDDCF4 / #AFB3CE), contorno suave, chega a ≈ 25% da altura e ≈ 25% da largura e fica na tela ≥ 17 quadros (≈ 0,7 s) [V11 q0027–q0044]. Uma segunda nuvem aparece no pouso/frenagem e sob o oponente derrubado [V11 q0036–q0039].
- **Pulo**: impulso com nuvem branca no chão [V10 q0119]; no ar, joelhos dobrados, pose segurada [V10 q0119–q0130]. Topo ≈ 30% de altura acima do chão (cabeça a ≈ 5% do topo da tela) [V10 q0130].
- **Agachar**: Ciclope agachado com tronco curvado sobre a perna da frente [V10 q0115].
- **Defesa / vitória / intro de round**: não aparecem em gameplay. O close final de V11 (armadura Fênix, mão estendida para a câmera, pose segurada 20 quadros) [V11 q0128–q0148] funciona como pose de "vitória/encerramento" de cinemática.
- **Expressão**: visor cobre os olhos; boca fechada e tensa nos closes [V09 q0125]; a expressão é transmitida por pose (mão no visor) [V09 q0298–q0313].

**Princípios (D)**
1. Dash = 1 pose forte segurada + fumaça grande no ponto de saída; a câmera acompanha.
2. Fumaça/poeira em formas de nuvem desenhadas (bolhas com sombra), não partículas finas, e que duram ≈ 0,5–0,7 s.
3. Movimento secundário vem de peças grandes (capa) — mantenha-as vivas no idle.
4. Idle com amplitude pequena: a pose comunica, não a respiração.

## E. GOLPES NORMAIS

Poucos normais isolados no trailer; os melhores exemplos:
- **Rasteira com explosão** [V09 q0185–q0192, `tokon_rasteira_fogo.jpg`]: preparação agachada girando o tronco (q0185–q0186) → q0187 perna esticada rente ao chão, corpo quase horizontal, e **explosão de fogo amarela no pé** (≈ 30% de altura × 30% de largura) → q0188–q0190 explosão no máximo, pose segurada ≈ 3 quadros (≈ 125 ms ≈ 7–8 f@60) → q0191 recuperação já em pé. Corpo inteiro participa: ombro de trás sobe, quadril gira, pé de apoio dobrado.
- **Soco/garra com rastro** [V09 q0192–q0193]: braço esticado com dedos abertos, borrão escuro desenhado no antebraço (smear) [V09 q0193 recorte].
- **Lançador para cima com coluna de fogo** [V09 q0193–q0200]: braço sobe, chama vertical cobre a oponente.
- **Golpe com arco de fogo** (Super cinemático) [V11 q0060–q0068]: rastro em arco grosso vermelho-escuro por fora → laranja → amarelo-branco por dentro, ≈ 70% da altura da tela, segurado ≈ 8 quadros.
Diferença fraco/médio/forte: não há como separar no trailer.

**Princípios (E)**
1. Golpe forte = pose de impacto exagerada (corpo quase horizontal) segurada ≈ 3 quadros da gravação, com efeito no ponto de contato.
2. O efeito (explosão/rastro) nasce no membro que bate, não no centro do alvo.
3. Smear desenhado no membro em movimento.
4. Quadril e ombro giram junto com o membro.

## F. ESPECIAIS E PROJÉTEIS

**Raio óptico (normal/especial)** [V11 q0005–q0019; V10 q0151–q0160; V09 q0286–q0293]:
- Carga: brilho no visor — estrela branco-rosada de 4 pontas (≈ 5% da altura) por ≈ 3–5 quadros antes de disparar [V11 q0005–q0009; V09 q0203–q0207].
- Disparo: **feixe instantâneo** (não é bola que viaja) — no 1º quadro já cruza a tela até o alvo [V11 q0010].
- Forma: cone horizontal fino no olho (≈ 2–3% da altura) e ≈ 12–20% de altura perto do alvo, com núcleo branco (≈ 5–10%) [V11 q0011, V09 q0288]. Camadas de cor: núcleo **#FFFFFE** → rosa **#F9AADD / #EE6DAE** → magenta **#EA3E8C**, com fitas finas magenta-escuras (#A00F55) ondulando por fora [V09 q0288, V10 q0155]. Bordas em "pinceladas" horizontais (riscos de velocidade dentro do feixe).
- Duração do feixe: ≈ 7–10 quadros (≈ 0,3–0,4 s) [V11 q0010–q0019]; afina para uma linha de ≈ 1% antes de sumir [V11 q0017–q0022].
- Variante diagonal anti-aérea a ≈ 35° para cima [V09 q0208–q0213, `tokon_antiaereo_raio.jpg`].
- No chão sob o Ciclope: rastro branco de "arrasto" (riscos brancos para trás) — recuo do disparo [V11 q0010–q0013].

**Fogo** [V09 q0244–q0258; V10 q0012–q0037; V11 q0041–q0052; `tokon_fogo_camadas.jpg`]:
- Desenho 2D chapado sobre a cena 3D: formas de língua com pontas, **4 camadas de cor sem degradê**: borda vermelho-escuro/terracota (#C8471B, #C56739) → laranja (#FE7B1E, #FA9A32) → amarelo (#FDEA24, #FBBD2F) → núcleo amarelo-pálido/quase branco (#FDF88B, #FEFEB4, #FDFDE4). Medido: ≈ 30% laranja, ≈ 40% amarelos, ≈ 20% vermelho-escuro [V09 q0248].
- "Buracos" escuros internos e lascas recortadas dão volume sem sombreamento.
- Brasas: pequenos quadrados/lascas laranja que ficam no chão depois [V11 q0001–q0009].
- O personagem envolto em fogo vira **silhueta laranja chapada** com linhas internas amarelas (Fênix) [V10 q0012–q0014]; o oponente queimando também vira mancha de fogo [V11 q0047–q0052].
- Parede de fogo rasteira: do Ciclope até a borda direita (≈ 75% da largura), até ≈ 50% de altura, ≈ 12 quadros (≈ 0,5 s) [V09 q0246–q0258].
- Bola/rajada de fogo que viaja: ≈ 6% da largura por quadro da gravação (≈ 2,5% por quadro a 60 Hz) [V10 q0173–q0177].

**Investidas**: o dash leva fumaça (ver D); o golpe que lança o Capitão tem fumaça + arco de fogo [V11 q0053–q0059].

**Princípios (F)**
1. Projétil de energia = núcleo branco + 2–3 camadas da cor do personagem + fitas escuras finas por fora para dar borda.
2. Antecipação no ponto de origem (estrela de brilho no olho) por ≈ 3–5 quadros da gravação.
3. Fogo em 4 camadas chapadas de vermelho-escuro → branco, com formas pontudas; nada de partícula realista.
4. O efeito domina o valor (é a coisa mais clara da tela) mas deixa a silhueta do lançador visível.
5. Feixe que some afinando, não piscando.

## G. SUPERS (apresentação) — "OPTIC RAGE" [V09 q0293–q0352, `tokon_super_sequencia.jpg`]

| Quadro | Tempo desde a ativação | O que acontece |
|---|---|---|
| q0293 | 0 | último quadro normal (feixe acertando a Tempestade) |
| q0294 | +42 ms | **pinceladas laranja** entram da direita cobrindo ≈ 40% da tela; raios amarelos no alvo |
| q0295 | +83 ms | painel: Ciclope inteiro (≈ 75% da altura) diante de fundo laranja com pinceladas azul-claras e linhas radiais amarelas; o cenário real vira ilustração azul-clara chapada |
| q0296–q0297 | +125–167 ms | câmera aproxima (Ciclope passa de 75% para ≈ 110% da altura); **"OPTIC RAGE"** entra da direita (letras brancas itálicas) |
| q0298–q0313 | +0,2–0,8 s | cartão emoldurado como página de revista (borda creme com texto miúdo), **inclinado ≈ 5–10°**, câmera em lenta aproximação/rotação; Ciclope com a mão no visor, pose segurada com deriva leve; texto fixo à direita, cobrindo ≈ 30% da largura |
| q0314–q0316 | +0,9 s | cartão desliza para a esquerda revelando o cenário **em cinza** |
| q0317–q0324 | +1,0–1,3 s | cenário **dessaturado** (S do fundo 0,25 → 0,02–0,08; brilho igual, L ≈ 180) enquanto o Ciclope (S 0,32) e a Tempestade em chamas continuam coloridos; aura azul-elétrica acende no Ciclope [q0323] |
| q0325 | +1,29 s (≈ 31 quadros ≈ 77 f@60) | **feixe gigante sai**: ≈ 35–50% da altura da tela, núcleo branco ≈ 15%; cor do cenário volta gradualmente (S 0,07 → 0,21 em 4 quadros) |
| q0325–q0346 | +1,3–2,2 s | feixe contínuo ≈ 22 quadros (≈ 0,9 s) com várias estrelas amarelas no alvo e fitas escuras curvas |
| q0347 | +2,2 s | câmera "entra" no feixe: tela inteira rosa/branca |
| q0348–q0349 | +2,3 s | **tela branca** com pinceladas pretas de nanquim (2 quadros, um provavelmente duplicado) |
| q0350 | +2,35 s | tela inteira rosa com relâmpagos brancos verticais |
| q0351–q0352 | +2,4 s | volta ao cenário com borrão horizontal e **grande estrela amarela + anel** no alvo |
| q0353 | — | corte do trailer para outra luta |

Sem tela preta. Não há "congelamento + escurecimento" clássico: a pausa do Super é o **cartão ilustrado** (≈ 0,8 s) seguido de **mundo em cinza** (≈ 0,3 s).

**Super de fogo** [V10 q0001–q0048]: Ciclope sobe, vira coluna de fogo com silhueta (≈ 55% da altura) [q0012–q0014], mergulha na oponente em uma massa de fogo de ≈ 50% da largura com estrela branca no centro [q0028–q0037], termina com nuvens brancas no chão [q0043–q0046].

**Super cinemático** [V11 q0060–q0156, `tokon_super_cinematico_v11.jpg`]: corte para câmera baixa e inclinada; arco de fogo ≈ 8 quadros [q0060–q0068] → chicote de câmera para a direita com borrão (2 quadros) [q0069–q0070] → Capitão bate no táxi, explosão de fogo em bolhas com fumaça marrom e pneu voando (≈ 14 quadros) [q0073–q0087] → corte para o parque, Capitão atravessando [q0088–q0099] → onda de fogo cobre **100% da tela** por ≈ 11 quadros (≈ 0,45 s) [q0100–q0110] → close do Ciclope de armadura Fênix [q0112–q0156], mão à câmera, pose congelada 20 quadros [q0129–q0148]. Hipótese: Super de nível alto ou transição de área do cenário (não confirmado).

**Princípios (G)**
1. Ativação curtíssima: em 1–2 quadros da gravação (≈ 40–80 ms) algo já cobre a tela — não há espera morta.
2. Separar "apresentação" (cartão com nome, pose segurada ≈ 0,8 s) de "execução"; a execução começa ≈ 1,3 s após ativar.
3. Dessaturar o cenário (não escurecer) e manter os dois lutadores coloridos: destaca sem perder o cenário.
4. Nome do Super em tipografia grande, itálica, entrando por deslizamento, ocupando ≈ 30% da largura.
5. Final com tela cheia de cor (branco → cor do golpe) por 2–3 quadros e retorno com estrela + anel.

## H. REAÇÃO A ACERTO

- **No ar, lançada** (Tempestade): capa abre, corpo arqueado para trás, sobe em giro [V09 q0193–q0214]; ao ser atingida pelo raio, corpo envolvido em fogo [V09 q0201–q0207].
- **No ar, de bruços/encolhido** (Magneto): dobra-se sobre o abdômen com a capa cobrindo metade do corpo, pose segurada ≈ 4 quadros [V10 q0160–q0168].
- **Derrubado** (Capitão América): gira no ar com o escudo, cai de lado, nuvens brancas no chão [V11 q0030–q0039]; Magneto cai de costas, pernas para cima, com nuvens brancas no ponto de queda [V10 q0145–q0150].
- **Tremor do atingido na pausa**: não detectável na gravação (24 q/s não resolve 1–2 px de tremor a 60 Hz).
- **Cor no atingido**: o atingido pelo feixe fica **tingido de rosa/magenta** pela luz do feixe [V10 q0151–q0157]; atingido por fogo fica laranja/silhueta de fogo [V11 q0047–q0052]. Não vi piscar branco.
- **Recuo**: o atingido pelo feixe é empurrado/flutua ≈ 5% da largura durante o feixe [V10 q0151–q0160].
- Tempo no chão e levantar: não aparecem.

**Princípios (H)**
1. Reação = pose grande e legível (corpo dobrado ou arqueado), segurada.
2. Tingir o atingido com a cor do golpe (rosa para feixe, laranja para fogo).
3. Queda sempre com nuvem desenhada no ponto de contato com o chão.
4. Peças grandes (capa) exageram o tranco.

## I. IMPACTO

- **Hitstop** (pausa no acerto): no golpe final do feixe em Magneto, **os dois lutadores param ≈ 3 quadros da gravação** (diferença local 3–6 em 255) [V10 q0161–q0163] ≈ 125 ms ≈ **7–8 f@60 (±2,5)**, durante o anel de faísca. Na rasteira de fogo, pose segurada ≈ 3 quadros [V09 q0188–q0190] (≈ 7–8 f@60). Durante o feixe contínuo o alvo NÃO congela entre os acertos (diferenças 11–30) [V10 q0151–q0160]. Fraco/médio: não identificáveis.
- **Defesa**: não aparece.
- **Tremor de tela**: ≈ 0 (ver A). O impacto é visual (faíscas, anel, riscos).
- **Contra-ataque / acerto especial**: o golpe que termina uma sequência ganha **anel amarelo fino + riscos longos amarelos que atravessam a tela** [V10 q0162–q0165; V09 q0212–q0214; V11 q0014–q0017] — parece marcar acerto forte/terminal (não confirmei qual mecânica). Um flash de estrela **branco-azulada** com anel aparece em [V10 q0143].
- **Ordem quadro a quadro de um acerto forte** (feixe terminal em Magneto, [V10 q0153–q0168], `tokon_faisca_impacto_feixe.jpg`):
  1. q0153–q0157: feixe já no alvo; **estrela amarela irregular** de 6–8 pontas no ponto de contato, ≈ 26–30% da altura (≈ 55–60% da altura do personagem); alvo tingido de rosa.
  2. q0158–q0160: feixe afina; estrela continua, com lascas laranja-escuras voando.
  3. q0161: estrela amarelo-branca maior, lascas para fora.
  4. q0162: **anel amarelo fino** (linha ≈ 0,5% da altura) com diâmetro ≈ 50% da altura + estrela de 4 pontas com raios longos (≈ 15–71% da altura); lutadores congelados (q0161–q0163).
  5. q0163–q0164: riscos amarelos retos e longos atravessam metade da tela em diagonal; anel expande (≈ 50% → 70%) e afina.
  6. q0165–q0168: anel some aos poucos; alvo retoma o movimento (q0169).
  Duração total do anel ≈ 7 quadros ≈ 290 ms ≈ 17 f@60.
- Câmera lenta: não observada.

**Princípios (I)**
1. Hitstop ≈ 7–8 f@60 nos acertos fortes, com os dois congelados; nenhum em acertos de feixe contínuo.
2. Impacto vendido por faísca grande (≈ metade da altura do personagem) + anel + riscos, não por tremor de tela.
3. Acerto terminal tem camada extra (anel + riscos longos) que os acertos intermediários não têm.
4. A faísca é mais clara que tudo na tela (amarelo/branco).

## J. EFEITOS VISUAIS

- **Faísca de acerto**: estrela **2D desenhada** (não partícula), irregular, 6–8 pontas, núcleo branco/amarelo-pálido (#FEFDAC), corpo amarelo (#FDFB3E), borda laranja (#F9C63B) e lascas vermelho-escuras [V10 q0155]; tamanho ≈ 26–30% da altura da tela; dura enquanto o golpe conecta + ≈ 3 quadros. Não gira de forma perceptível; troca de desenho a cada quadro (flipbook) [V10 q0153–q0161].
- **Anel**: círculo fino amarelo que expande e some em ≈ 7 quadros [V10 q0162–q0168].
- **Riscos de impacto**: linhas retas amarelas/laranja muito longas (até ≈ 60% da largura), afinando nas pontas [V10 q0163; V09 q0213–q0214].
- **Faísca de defesa**: não aparece.
- **Poeira/fumaça**: nuvens brancas cel-shaded (bolhas grandes, sombra lavanda) no dash, no pulo e nas quedas [V11 q0027–q0044; V10 q0119, q0149]. Fumaça de explosão marrom-acinzentada com bordas recortadas [V11 q0079–q0087].
- **Rastros/smears**: arco de fogo do golpe [V11 q0060–q0068]; borrão escuro no braço [V09 q0193]; riscos brancos de arrasto no chão no disparo [V11 q0010–q0013]; linhas de velocidade dentro do feixe.
- **Auras/brilhos**: estrela de brilho no visor (branco-rosa) [V09 q0203–q0207]; aura azul-elétrica contornando o corpo antes do Super [V09 q0323]; emblema de Fênix laranja brilhando no peito do traje preto [V11 q0004]; círculo/esfera verde com anel claro onde entra o parceiro (efeito de assistente — ignore a mecânica, aproveite a ideia de "marcador de entrada") [V10 q0163–q0168; V11 q0015–q0020].
- **Chão**: brasas que ficam no asfalto e pulsam [V11 q0001–q0009]; parede de fogo rasteira [V09 q0246–q0258].
- **Cenário reagindo**: táxi explode e pneu voa [V11 q0075–q0087]; cristais/lâminas roxas de metal (Magneto) brotam do chão [V10 q0124–q0137].
- **Como os efeitos 2D convivem com o 3D**: todos os efeitos são formas chapadas sem iluminação (emissivas), com contorno implícito pelo contraste de camadas, desenhadas por cima dos modelos — inclusive cobrindo o corpo do próprio lançador (raio sai "de dentro" do visor) [V09 q0288].

**Princípios (J)**
1. Efeitos são desenhos 2D quadro a quadro (flipbook), chapados, com 3–4 camadas do escuro para o branco.
2. Cada tipo de energia com paleta própria e fixa: fogo laranja/amarelo, raio rosa/magenta, acerto amarelo, assistente verde, fumaça branco-lavanda.
3. Efeitos são o valor mais claro da tela; personagens o mais escuro; fundo no meio.
4. Fumaça e poeira em nuvens grandes e lentas, desenhadas, que ficam ≈ 0,5–0,7 s.
5. Anel + riscos longos como "pontuação" de golpe forte.

## K. HUD E TEXTOS

- **Barras de vida, timer, retratos, contador de combo**: **não aparecem** em nenhum trecho (trailer sem HUD). Não dá para medir.
- **Medidor "Solar Shards"** [V10 q0115–q0272, `tokon_textos_tela.jpg`]: canto inferior esquerdo, x ≈ 5–25%, y ≈ 75–87%. Emblema (pássaro/Fênix em V, laranja e marrom com contorno branco) + barra branca em forma de flecha com 4 losangos. Vazio = cinza claro; cheio = laranja (#F08030 aprox.) com borda clara. Ao ganhar um fragmento, um **losango laranja translúcido grande (≈ 12% da altura)** aparece sobre a barra e encolhe até o encaixe em ≈ 2–3 quadros [V10 q0141–q0143]; segundo encaixe [V10 q0162].
- **Faixa de tutorial "GAIN SOLAR SHARDS / TO UNLEASH DARK PHOENIX"** [V10 q0185–q0272]: inferior direito, x ≈ 41–100%, y ≈ 72–90%. Linha laranja fina no topo, faixa creme com texto cinza-escuro em caixa alta (altura de letra ≈ 7% da tela), sub-faixa escura com texto laranja (≈ 3%). Entrada: a faixa creme desliza da direita (q0185–q0188), o texto aparece primeiro em **azul-claro** e escurece para cinza (q0190–q0196), em ≈ 11 quadros (≈ 0,45 s). Fica até o fim (≥ 3,6 s).
- **Cartão de título** [V09 q0141–q0180]: "FEEL THE MIGHT OF" em cinza-escuro e "PHOENIX CYCLOPS" em laranja sobre faixa cinza-escura, tudo inclinado ≈ 5°, papel creme com bordas queimando e brasas; emblema à esquerda. O texto entra palavra por palavra (q0145 → q0153).
- **Nome do Super** "OPTIC RAGE" [V09 q0296–q0313]: branco, itálico, caixa alta, com marca/logo integrada à última letra, sobre laranja.
- **Tipografia**: sans-serif grotesca **condensada e muito pesada** (família tipo cartaz/esportiva), caixa alta, entrelinha apertada, com letras cortadas/sobrepostas em faixas; cores restritas a cinza-escuro, laranja e creme/branco. Visual de revista/pôster impresso (papel creme com texto miúdo nas margens) [V09 q0298; V09 q0165].
- **Quanto chama atenção**: o medidor é pequeno (≈ 12% de altura) e de baixo contraste (branco/cinza sobre grama); a faixa de tutorial é grande (≈ 18% de altura) mas fica no canto inferior, fora da área dos lutadores.

**Princípios (K)**
1. Uma família tipográfica só (grotesca condensada pesada), caixa alta, 2–3 cores fixas da marca.
2. Textos entram deslizando com uma cor de destaque antes de "assentar" na cor final.
3. Medidor por slots (losangos) com animação de preenchimento que "voa" até o slot.
4. Elementos de interface nos cantos inferiores, fora da silhueta dos lutadores.

## L. SOM

As gravações **têm áudio** (AAC). Medido RMS por janela de 1 quadro (≈ 42 ms):
- V09: silêncio nos primeiros 23 quadros (vídeo pausado), depois música + voz contínuas, média ≈ −29 dB (contando o silêncio), picos de −8 a −10 dB perto de q0066, q0136–q0138 (pouco antes do corte para o cartão de título em q0141) e ≈ −12 dB no feixe/Super (q0314–q0346 estável em ≈ −14 a −17 dB, sem buracos).
- V10: média ≈ −23 dB; pico ≈ −11 a −14 dB em q0172–q0177 (rajada/explosão de fogo).
- V11: média ≈ −23 dB; nível alto e sustentado ≈ −11 a −14 dB em q0102–q0115 (onda de fogo cobrindo a tela).
- **Voz**: há falas sobre o gameplay (as legendas automáticas do YouTube transcrevem: "If I touch it, it will burn." / "You still embrace the phoenix force?" / "The power of your Mother Earth does not compare to a star!" / "Mutantkind deserves a true leader, Summers." / "My power will make the world you dream of." / "Even defeated, you win." / "I am Phoenix, you are nothing." / "You answer to men now, Scott!"). São diálogos entre os personagens durante a luta.
- Não consegui separar efeitos sonoros individuais de acerto da música na gravação (picos não coincidem de forma confiável com os quadros de impacto; há ≈ 0,5 s de diferença de duração entre áudio e vídeo em V09).

**Princípios (L)**
1. Diálogo curto entre os personagens durante a luta dá personalidade sem parar o jogo.
2. Super = trecho de nível sonoro alto e contínuo (sem buracos) — o som sustenta a cena.
3. Explosões grandes com o pico de volume do trecho.

## M. FLUXO DO ROUND

Não aparece intro de round, ROUND/FIGHT, KO nem tela de vitória. O mais próximo:
- Abertura do trailer: logo MARVEL GAMES (≈ 13 quadros ≈ 0,5 s) [V09 q0025–q0037], close dos pés com inimigo caído [V09 q0038–q0060], close do visor [V09 q0120–q0140], cartão de título [V09 q0141–q0180].
- Encerramento de sequência: close do Ciclope de armadura com pose congelada ≈ 0,8 s [V11 q0129–q0148].

**Princípios (M)** (do que dá para tirar)
1. Closes de rosto/pés como "respiro" entre ações (câmera baixa, personagem cortado pela borda).
2. Pose final congelada ≈ 0,8 s para encerrar uma sequência.
3. Cartões de texto com o mesmo estilo gráfico do Super (papel, laranja, condensada) — identidade única em tudo.

---

## O que este jogo faz que mais o faz parecer profissional (em ordem de impacto)

1. **Hierarquia de valor rígida**: efeitos brancos/amarelos (mais claros) > fundo pastel (médio-claro) > lutadores (escuros e saturados, ≈ 30–50 pontos de brilho abaixo do fundo). Funciona mesmo nos cenários mais coloridos [V09 q0238; V10 q0115; V10 q0229].
2. **Efeitos 2D chapados em camadas** sobre o 3D: fogo em 4 tons de vermelho-escuro a branco, feixe branco/rosa/magenta com fitas escuras, faíscas em estrela trocando de desenho a cada quadro [V09 q0248; V10 q0155; V09 q0288].
3. **Apresentação do Super como página de revista**: cartão laranja inclinado com nome em tipografia pesada, depois mundo em cinza com os lutadores coloridos, depois feixe que toma metade da tela e termina em tela branca [V09 q0294–q0352].
4. **Cel-shading de 2 tons + contorno fino + rim light azul-lavanda** — cada personagem lido como ilustração e separado do fundo [V11 q0004; V09 q0298].
5. **Animação limitada com poses fortes seguradas** (≈ 3 quadros da gravação nas poses de impacto, 20 quadros no close final) [V11 q0003–q0009; V09 q0188–q0190; V11 q0129–q0148].
6. **Impacto por camadas** (estrela → anel → riscos longos → hitstop ≈ 7–8 f@60), sem depender de tremor de tela [V10 q0153–q0168].
7. **Silhuetas com forma-assinatura** (ombreiras em V do Ciclope, capa-asa da Tempestade, capa do Magneto) [V09 q0193; V09 q0238; V10 q0115].
8. **Fumaça e poeira desenhadas como nuvens grandes** que duram ≈ 0,5–0,7 s e marcam dash, pulo e queda [V11 q0027–q0044; V10 q0149].
9. **Câmera próxima** (lutador ≈ 50% da altura, chão a ≈ 93%) que acompanha o dash e só sobe ≈ 2% no pulo — cenário vira moldura, lutador é protagonista [V09 q0238; V11 q0023–q0035; V09 q0218–q0224].
10. **Identidade gráfica única** em textos, cartões, nome do Super e HUD (condensada pesada, laranja + cinza-escuro + creme, papel impresso) [V09 q0165; V09 q0298; V10 q0229].

## Notas e limites

- É trailer: há cortes, dissolves (V10 q0181–q0188) e trechos cinemáticos; as medidas de câmera valem só dentro de cada tomada contínua.
- Sem HUD de vida/tempo e sem defesa no material — esses itens ficaram em branco de propósito.
- A gravação a ~24 q/s não resolve detalhes de 1–2 quadros a 60 Hz (tremor do atingido, duração exata de flashes); onde aparece "≈" a margem é ±1 quadro da gravação (±2,5 f@60).
- O que for mecânica própria (Solar Shards, assistente com círculo verde, transformação Dark Phoenix) só foi descrito pelo que ensina de visual.
