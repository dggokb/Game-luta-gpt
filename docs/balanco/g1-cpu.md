# Frente G1: a CPU usa S2–S4 (G1a) e o alcance real (G1b)

Base: HEAD `5cd8ed16` (código de combate igual ao das etapas 1–5). Duas etapas, nesta ordem, cada uma
medida sozinha contra a base e depois somadas. As tabelas usam as mesmas sementes de antes:
`BateriaAntesDepois`, a matriz 16 × 16 e a sinergia da `BateriaComparacoes`, mais a bateria nova
`BateriaEspeciaisCpu` (uso por minuto, duplas oficiais e espelhos).

**Ambiente.** A worktree desta frente nasceu no ramo padrão (`582d1f65`), não no HEAD. O `reset` para
`5cd8ed16` encheu o disco (96%) e falhou no meio, e apagar a worktree foi negado. Ela ficou com 2,6 GB
meio escritos e pode ser removida pelo pai.

Trabalhei numa cópia só do Java (`scratchpad/g1/android`, com os assets do app em link). Compilei por
overlay sobre o `:pc` do HEAD, com o `jpw.sh`. Os dois diffs aplicam limpo no HEAD: conferi com
`patch -p1 --dry-run` numa cópia de `android/app/src`. Não fiz commit nem push.

- `scratchpad/bal/G1a.diff`: só G1a.
- `scratchpad/bal/G1b.diff`: G1a + G1b (o diff total).

## G1a: S2–S4 e assinaturas

### O que mudou

**`CpuSpecials.java` (novo, 174 linhas).** Lê a função de cada especial dos dados, sem id de personagem:

| Dado lido | Função (`Role`) |
|---|---|
| projétil / armadilha | `PROJECTILE`, `TRAP` |
| `grab` | `GRAB` |
| `repel` | `REPEL` |
| `counter` | `COUNTER` |
| `evade` | `EVADE` (inclui o teleporte) |
| `pull` | `PULL` |
| `echo` | `ECHO` |
| só no ar ou mergulho | `DIVE`; o S1 de mergulho do p14 entra aqui |
| `armor` | `ARMOR` |
| `LAUNCH`, ou rápido (≤ 8 q), ativo longo e sem avanço | `ANTI_AIR` |
| `low` | `LOW` |
| avanço ≥ 120 px ou teleporte | `RUSH` |
| o resto | `POKE` |

- O alcance é `reach` mais o avanço até o fim do ativo. Na G1b passa a ser a medida real.
- Marca o comando de carga (`[←] →`, `[↓] ↑`).
- Cache estático, seguro para as baterias em paralelo.

**`AiController.java`.**

- **Constantes `SPECIAL_*` (l. 57–75).** São as chances no nível 8.
  - Neutro: 0,06 por decisão × (0,2 + 0,8 × inteligência).
  - Reação: 0,45 × `reactGuard`.
  - Anti-aéreo: metade das vezes com o especial.
  - Mergulho: 0,35 × `kitScale`.
  - Golpe lento (> 16 q) fora do neutro.
  - Especial de vida (p12) só com mais de 30% de vida.
- **Chamadas novas.** `fill()` chama `specialReaction` antes de `defend` (l. 482) e `specialNeutral` depois de cada decisão do `OpponentAi` (l. 525).
- **`punish()`.** Longe demais para os normais, pune com o especial que sai a tempo e alcança (`RUSH`, `POKE`, `LOW`, `ANTI_AIR`, `ARMOR`; l. ~1188).
- **`antiAir()`.** Sorteia uma vez por pulo se o anti-aéreo vai com o especial que sobe (`risingSpecial`, l. 1222 e 1406).
- **`specialReaction` (l. 1254).** Um sorteio por golpe e por bola do rival:
  - golpe: contra-golpe que lê aquela altura, esquiva ou armadura cuja janela cobre o contato. Espera a hora sem defender por cima;
  - bola: repel ou esquiva (`HIGH` contra bola alta, `ANY` contra tudo), soltos quando a bola entra na janela;
  - no ar: o mergulho, uma vez por pulo, só contra rival de pé no chão (não caído nem levantando).
- **`specialNeutral` (l. 1420).** Troca a decisão por um especial que serve à distância:
  - projétil ≥ 200 px, até o alcance;
  - armadilha a 140–480 px;
  - agarrão de especial colado, com peso 3 contra quem só defende;
  - puxão e eco a meia distância;
  - avanço, baixo e golpe comum se saem em ≤ 16 q;
  - contra-golpe e armadura "lendo a pressão": colado em quem atacou há menos de 30 q.
- **`lifeAllows` (l. 1485).** O especial de carga só sai com a carga feita (`motion.charged`, a mesma regra do jogador). O de vida só com vida sobrando.

Sem carga, a CPU disparava a ombrada do p09 (armadura e carga) 3,9 vezes por minuto e o p09 ia a 72%. Com a regra da carga ficou em 0,5 por minuto e 62%.

**Limitação.** A CPU solta o especial pelo `FighterInput.special = "S2"`. A força L/M/H vem do `commandButton`, que fica nulo, e por isso sai sempre a do M (pedra do p13, pavio da caixa da p16). Escolher a força exigiria mexer no `CombatEngine` (DECIDIR 6).

### Função lida de cada especial

| lutador | S2 | S3 | S4 | outro |
|---|---|---|---|---|
| Ryūga (p01) | Punho ascendente: ANTI_AIR | Aparar: POKE | Chute giratório: POKE |  |
| Yin-lu (p02) | Chute da garça: ANTI_AIR (carga) | Contra-golpe: COUNTER | Chute rodopiante: POKE (carga) |  |
| Kōga (p03) | Punho em sequência: ANTI_AIR | Avanço em três: RUSH | Mergulho: DIVE |  |
| p04 | Cabeçada: ANTI_AIR (carga) | Esquiva: EVADE | Ombrada: RUSH |  |
| p05 | Shuriken: PROJECTILE | Selo e mudança: EVADE | Corte de mão: RUSH |  |
| p06 | Eco de sombra: ECHO | Puxar: PULL | Passo-sombra: RUSH |  |
| p07 | Chute ascendente: ANTI_AIR | Contra-golpe: COUNTER | Chute giratório baixo: LOW |  |
| p08 | Giro ascendente: ANTI_AIR (carga) | Contra-golpe: COUNTER | Salto-agulha: RUSH |  |
| p09 | Cotovelada: ANTI_AIR | Escudo: ARMOR | Ombrada: ARMOR (carga) |  |
| p10 | Jato de vapor: ANTI_AIR | Esquiva acrobática: EVADE | Voadora em parafuso: RUSH |  |
| p11 | Seringa: PROJECTILE | Bloqueio de nervos: COUNTER | Passo cirúrgico: RUSH (carga) |  |
| p12 | Salto de garras: ANTI_AIR (carga) | Fúria: ARMOR | Contorção: RUSH |  |
| p13 | Pedra arremessada: PROJECTILE | Três cabeçadas: GRAB | Salto do colosso: RUSH |  |
| p14 | Chute tornado: ANTI_AIR | Meia-lua cortante: ANTI_AIR | Pisão de onda: LOW | S1: DIVE |
| p15 | Repel da aura vermelha: REPEL | Rajada de punhos: POKE | Voadora e chute girando: POKE |  |
| p16 | Caixa surpresa: TRAP | Casca de banana: LOW | Estrelinha: RUSH |  |

O Punho em sequência do Kōga cai como `ANTI_AIR` pela regra: rápido, ativo longo, avanço de 115 px, só 5 abaixo do corte de 120. Na prática ele quase não sai (0,2 por minuto).

### Assinaturas pedidas

| Personagem | Antes | Depois |
|---|---|---|
| p09 | Nunca usava S3/S4 | O escudo e a ombrada entram com a armadura: a ombrada 0,5 por minuto (só com a carga, 57% acerta), o escudo 0,2 por minuto. Os agarrões de comando não existem no p09; o agarrão normal vem da etapa 4b. |
| p11 | Nunca envenenava | Seringa 2,0 por minuto (52% acerta, vira poça) e Bloqueio de nervos (contra com veneno) 0,1 por minuto. |
| Ryūga | — | O Aparar não tem mecânica de aparar (golpe de 20 q): fica só para punir, e quase não sai. O Punho ascendente sai como anti-aéreo (raro: a CPU × CPU pula pouco). O Chute giratório sai 2,6 por minuto. |
| Esquivas (p04, p05, p10) | — | A esquiva entra contra golpe e contra bola: p04 0,8, p05 0,9 (o sumiço acerta 93%), p10 0,7 por minuto. O Kōga não tem esquiva; os especiais dele são investida, Avanço em três e mergulho. |
| p06, p08, p11 | Os três jogavam como o mesmo zoner | **p06**: eco 0,7 + puxão 0,8 + passo-sombra 1,4 por minuto. **p08**: quase só a bola (o contra-golpe dele só lê golpe baixo; o giro ascendente pede carga). **p11**: seringa e poça. O p08 continua o zoner puro. |

### Uso por minuto (1x1 contra os outros 15, 10 lutas por par, nível 8)

Base: S2–S4 somados ficavam em 0–0,5 por minuto (só via rota de combo).

Na tabela, o G1a + G1b. Cada célula traz usos por minuto, a parte que acertou e a parte defendida. Repel e contra-golpe contam como acerto quando pegam.

| Personagem | S1/min | S2/min | S3/min | S4/min | Super/min | S2–S4 acertam | dano de S2–S4 | vence |
|---|---|---|---|---|---|---|---|---|
| player_base | 15.6 (25/8) | 0.1 (58/25) | 0 | 2.6 (41/13) | 1.8 (66/17) | 41% | 5% | 66% |
| p02 | 10.6 (35/18) | 0 | 0.2 (68/2) | 0.2 (47/2) | 1.9 (50/27) | 56% | 2% | 47% |
| p03 | 11.1 (47/12) | 0.2 (56/23) | 0.0 (75/0) | 1.1 (8/43) | 0.7 (36/27) | 16% | 1% | 66% |
| p04 | 7.2 (40/17) | 0.2 (47/10) | 0.8 (13/0) | 2.5 (53/7) | 1.7 (43/25) | 44% | 12% | 62% |
| p05 | 12.6 (39/31) | 2.0 (55/19) | 0.9 (93/3) | 2.2 (47/26) | 1.0 (41/29) | 58% | 20% | 52% |
| p06 | 20.7 (18/3) | 0.7 (17/2) | 0.8 (13/39) | 1.4 (45/22) | 1.0 (60/20) | 29% | 6% | 50% |
| p07 | 4.0 (24/6) | 0.3 (80/13) | 0.4 (84/2) | 2.5 (37/5) | 0.5 (0/0) | 47% | 18% | 26% |
| p08 | 28.8 (34/30) | 0 | 0.0 (33/0) | 0.8 (47/15) | 2.4 (57/10) | 47% | 2% | 79% |
| p09 | 5.4 (43/12) | 0.0 (33/0) | 0.2 (41/8) | 0.5 (57/12) | 0.8 (33/27) | 53% | 3% | 64% |
| p10 | 9.5 (43/18) | 0.1 (69/15) | 0.7 (15/1) | 3.0 (60/17) | 0.5 (27/51) | 51% | 12% | 61% |
| p11 | 30.7 (34/23) | 2.0 (52/11) | 0.1 (78/0) | 0.1 (42/3) | 1.5 (46/36) | 53% | 8% | 68% |
| p12 | 9.8 (42/7) | 0.0 (100/0) | 0.2 (71/2) | 1.7 (41/7) | 0.4 (23/45) | 44% | 6% | 44% |
| p13 | 7.0 (35/23) | 1.9 (18/1) | 0.1 (22/0) | 0 | 0.9 (36/14) | 18% | 5% | 54% |
| p14 | 0.9 (22/50) | 0.2 (65/20) | 0.1 (57/20) | 0 | 2.1 (77/3) | 62% | 2% | 29% |
| p15 | 9.1 (44/10) | 1.1 (96/0) | 0.9 (37/6) | 0.7 (20/9) | 0.7 (21/7) | 57% | 4% | 11% |
| p16 | 10.6 (31/7) | 2.1 (0/0) | 0 | 0.1 (54/25) | 0.5 (22/31) | 2% | 6% | 23% |

- O "S1" do p14 (0,9 por minuto) é o mergulho novo: antes era 0.
- A caixa da p16 sai 2,1 por minuto e mostra 0% de acerto porque quem acerta é a luva, depois. O dano dela aparece nos 6% da coluna "dano de S2–S4".

Arquivos com cada etapa sozinha:

| Etapa | Arquivo |
|---|---|
| base | `G1_uso_base.md` |
| G1a | `G1_uso_a.md` |
| G1b sozinha | `G1_uso_b_so.md` |

### Ficaram de fora

- **Golpes lentos no neutro (> 16 q):** o Aparar (20 q), o escudo do p09 (23 q), o Salto do colosso do p13 (34 q) e a banana da p16 (44 q). No neutro só levavam a punição: com o corte em 18 q, o p14 caía de 45% para 37% e o Kōga de 59% para 52%. Eles saem só na punição, quando cabem no tempo.
- **Uso em combo:** S2–S4 dentro de combo é da outra frente (busca de combos). Não mexi no `ComboBook` nem na `BateriaBalanco`.

## G1b: a régua real da CPU

### O que mudou

- **`CpuReachTable.java` (novo, gerado).** Alcance real de cada golpe de chão de cada lutador (os 16 e o `monster_npc`): L, M, H, 2L, 2M, 2H, 6M, o S1 corpo a corpo e S2–S4 de chão.
  - Medido no motor, com o desenho, contra o Ryūga parado em pé e agachado.
  - Guarda a ponta, centro a centro sem o corpo do rival; −1 quando não pega o agachado.
- **`GeraAlcanceCpu.java` (teste).** Gera a tabela em 16 s. Contra o boneco, aperta como a CPU aperta: `in.special` e →M.
- **`CpuReach.java` (novo).** `reaches`, `maxCenterDistance` e `pickInRange` com a medida e a postura do rival. No ar e fora da tabela vale o alcance do pacote, como antes. `CombatRules` não mudou: os testes de regra continuam com o alcance nominal.
- **`OpponentAi.java`.**
  - A escolha de golpe no chão passa a ser `CpuReach.pickInRange(..., vsCrouch)` (l. 142): contra o agachado só sai o que pega o agachado.
  - O L de abertura (l. 150) e o S1 corpo a corpo (`energyInRange` com o corpo, l. 290) usam a medida.
- **`AiController.java`.** Usam a régua nova e a postura (`crouchingBody()`, ou `seen.crouching` onde ela reage ao que viu):
  - a conferência na hora de apertar (l. 556);
  - o troco do breaker (l. 881);
  - a beira do jab e a ameaça do rival (l. 1011 e 1025);
  - o guard break (l. 1122);
  - as duas punições (l. 1161 e 1184);
  - o `alcanca` das rotas (l. 1758).

  O anti-aéreo (rival no ar) e o aéreo do combo continuam com o alcance do pacote.
- **`CpuSpecials`.** A ponta dos especiais vem da tabela, com a versão contra agachado.

### Alcance do pacote → real (em pé / agachado)

Distância eixo a eixo contra o Ryūga.

| lutador | L | M | H | 2L | 2M | 2H |
|---|---|---|---|---|---|---|
| Ryūga | 171→206/– | 173→210/– | 219→256/224 | 183→220/188 | 194→240/200 | 134→172/140 |
| Yin-lu | 185→222/190 | 219→266/224 | 231→276/236 | 139→186/144 | 173→220/178 | 136→174/142 |
| Kōga | 174→210/– | 168→204/– | 175→210/– | 197→234/202 | 267→314/272 | 135→172/140 |
| p04 | 183→218/– | 213→260/218 | 214→260/220 | 189→226/194 | 242→288/248 | 130→180/148 |
| p05 | 169→206/174 | 232→278/238 | 171→206/174 | 183→220/188 | 173→220/178 | 148→186/154 |
| p06 | 137→174/142 | 207→244/212 | 180→218/186 | 178→224/184 | **295→272**/232 | 140→178/146 |
| p07 | 165→198/– | 228→264/– | 193→228/– | 159→206/164 | 192→194/152 | **154→266**/234 |
| p08 | 154→198/158 | 239→286/244 | 396→434/402 | 164→210/170 | 186→200/160 | 144→200/168 |
| p09 | 193→228/– | 238→276/244 | 159→196/164 | 233→280/238 | 256→302/262 | 144→182/150 |
| p10 | 165→200/– | 246→284/252 | 161→198/166 | 173→210/178 | 206→252/212 | 189→226/194 |
| p11 | 200→236/– | 184→220/– | 199→236/204 | 218→256/224 | 233→264/222 | 142→180/148 |
| p12 | 234→272/240 | 217→264/222 | 125→162/130 | 211→258/216 | 252→298/258 | 133→170/138 |
| p13 | 205→240/– | 226→262/– | 250→286/254 | 268→306/274 | 292→338/298 | 149→186/154 |
| p14 | 204→242/210 | 218→256/224 | 277→324/282 | 249→296/254 | 312→332/292 | 136→174/142 |
| p15 | 162→198/– | 160→196/– | 263→300/268 | 171→208/176 | 292→332/290 | 166→206/174 |
| p16 | 166→202/– | 186→224/192 | 233→270/238 | 169→196/164 | 248→294/254 | 184→226/194 |

- O real passa o do pacote em 32–48 px em quase todos os golpes, como na auditoria.
- Exceções: o 2M do p06 é **mais curto** que o pacote (295 → 272) e o 2H do p07 é bem mais longo (154 → 266).
- "–" quer dizer que não pega quem está agachado. São 22 golpes, entre eles o L, o M e o H do Kōga: era a origem do "H do Kōga erra o agachado 15 de 16". Ele também passa por cima com o L e o M.
- Medi batendo para a direita e para a esquerda (`EnsaioLado`): os números são iguais, sem assimetria de lado.

## Regressão (testes)

**Testes novos**

| Teste | O que confere |
|---|---|
| `CpuSpecialsTest` (5) | a função lida dos dados; p05 usa S2, S3 e S4; p15 repele; nível 1 usa menos da metade do nível 8; o S4 de carga do p09 só sai com a carga |
| `CpuReachTest` (4) | a tabela confere com o motor (avisa quando sprites ou golpes mudam e alguém esquece o `GeraAlcanceCpu`); a régua é a do desenho (o 2M do Ryūga entre o pacote e o real: a CPU sabe que pega, e pega); contra o Kōga agachado o L, o M e o H não são escolhidos (e de fato erram); no ar vale o pacote |

**Testes que mudaram (a régua do teste)**

`CpuTest.theCpuAlmostNeverStartsAnAttackOutOfReach`, `CombatEngineTest.cpuOnlyAttacksThroughTheEngineAndWithinReach` e `ExtractedSystemsTest.aiNeverAttacksOutOfReachAndApproachesInstead` mediam "fora do alcance" com o alcance do pacote. Passaram a medir com o real (`CpuReach`). Com a régua antiga falhavam: o golpe começa entre o alcance do pacote e o real, e acerta.

**Suíte do app (sem Robolectric)**

| Estado | Resultado |
|---|---|
| G1a | **546 de 546 OK** (`G1a_suite.txt`) |
| G1a + G1b | **550 de 550 OK** (`G1b_suite.txt`) |
| G1b sozinha | 543 de 545: `AiFullKitTest.theRealAssistComesOutInTheEngine` e `PressaDoTempoTest.aHurriedCpuAttacksMoreAndWaitsLess` (789 contra 972 quadros defendendo) |

- As duas falhas da G1b sozinha são testes estatísticos de semente fixa e passam com G1a + G1b. A G1b não é um estado de commit sozinha: o diff dela vai sobre o G1a.
- Os 2 testes `pc` que já falhavam (A02) não fazem parte dessa suíte.

## Antes × depois

### BateriaAntesDepois

Mesmas sementes. Base = `ad/6_head.tsv`; G1a = `ad/g1a.tsv`; G1b sozinha = `ad/g1b_so.tsv`; G1a + G1b = `ad/g1b.tsv`.

| métrica | base | G1a | G1b só | G1a+b |
|---|---|---|---|---|
| breaker.troco_garantido_script | 69% | 51% (-18) | 56% (-13) | 57% (-11) |
| breaker.sempre_vs_nunca | 57% | 65% (+8) | 62% (+4) | 68% (+11) |
| breaker.cpu_vs_nunca | 49% | 59% (+9) | 53% (+4) | 56% (+7) |
| breaker.cpu_quebras_por_luta | 76% | 95% (+19) | 88% (+12) | 88% (+12) |
| shinkai.2x2_vence.POWER | 53% | 51% | 52% | 59% (+6) |
| shinkai.2x2_vence.TECHNIQUE | 45% | 54% (+9) | 46% | 56% (+11) |
| tag.cpu_perdidos | 7% | 9% | 9% | 9% |
| 1x1.Ryūga (p01) | 70% | 65% (-5) | 67% (-4) | 64% (-7) |
| 1x1.Yin-lu (p02) | 44% | 44% | 47% (+3) | 46% (+2) |
| 1x1.Kōga (p03) | 60% | 58% (-2) | 67% (+7) | 63% (+3) |
| 1x1.p04 | 56% | 64% (+7) | 58% (+2) | 64% (+7) |
| 1x1.p05 | 43% | 49% (+6) | 48% (+5) | 54% (+11) |
| 1x1.p06 | 69% | 60% (-9) | 56% (-13) | 53% (-16) |
| 1x1.p07 | 15% | 21% (+6) | 21% (+6) | 28% (+13) |
| 1x1.p08 | 79% | 78% | 80% | 79% |
| 1x1.p09 | 62% | 60% (-2) | 65% (+3) | 63% (+1) |
| 1x1.p10 | 58% | 64% (+5) | 60% (+2) | 59% |
| 1x1.p11 | 56% | 61% (+5) | 56% | 61% (+5) |
| 1x1.p12 | 46% | 44% (-2) | 52% (+7) | 47% (+2) |
| 1x1.p13 | 57% | 53% (-4) | 55% (-3) | 55% (-2) |
| 1x1.p14 | 39% | 40% | 33% (-6) | 32% (-8) |
| 1x1.p15 | 17% | 14% (-3) | 12% (-5) | 8% (-9) |
| 1x1.p16 | 29% | 25% (-3) | 22% (-6) | 26% (-2) |
| 1x1.p09_vs_p02 | 68% | 62% (-5) | 72% (+5) | 57% (-10) |
| p09.vs_p02_400 | 68% | 67% | 72% (+4) | 69% (+1) |

Notas:

- O 2x2 com duplas sorteadas está completo em `G1_antesdepois.md`. Destaque: Yin-lu 42 → 58% e p07 31 → 46%.
- Os combos (`combo.*`) e o Clash (`clash.*`) **não mudam** (roteiro, sem CPU).
- Ruído das métricas de regra: ±5.

### Matriz 16 × 16 (CPU nível 8, 10 sementes por célula): força contra o elenco

| lutador | base | G1a | G1b só | G1a+b |
|---|---|---|---|---|
| p08 | 77% | 77% (+0) | 80% (+3) | 78% (+1) |
| p06 | 68% | 60% (-8) | 56% (-12) | 51% (-17) |
| Ryūga (p01) | 67% | 71% (+4) | 68% (+1) | 61% (-6) |
| p04 | 63% | 65% (+2) | 64% (+1) | 64% (+1) |
| p13 | 62% | 52% (-10) | 57% (-5) | 55% (-7) |
| p11 | 61% | 60% (-1) | 53% (-8) | 63% (+2) |
| Kōga (p03) | 60% | 60% (+0) | 64% (+4) | 58% (-2) |
| p10 | 60% | 62% (+2) | 61% (+1) | 63% (+3) |
| p09 | 58% | 61% (+3) | 65% (+7) | 64% (+6) |
| p12 | 43% | 39% (-4) | 52% (+9) | 47% (+4) |
| p14 | 41% | 41% (+0) | 33% (-8) | 31% (-10) |
| p05 | 41% | 49% (+8) | 43% (+2) | 51% (+10) |
| Yin-lu (p02) | 37% | 42% (+5) | 46% (+9) | 45% (+8) |
| p16 | 30% | 29% (-1) | 28% (-2) | 24% (-6) |
| p15 | 17% | 10% (-7) | 11% (-6) | 13% (-4) |
| p07 | 16% | 23% (+7) | 18% (+2) | 32% (+16) |

- Ruído de ±8 por lutador.
- **Viés de lado.** Nas matrizes o lado esquerdo vence 50–51%. Nos espelhos de 10 sementes deu 47% (base), 49% (G1a), 61% (G1b sozinha) e 56% (G1a + G1b). Refiz com 40 lutas por lutador (`G1_espelhos.md`): 49%, 48%, 49% e 50%. O 61% era ruído; não há viés de lado.

### Duplas oficiais (2x2, todas contra todas, 6 lutas por par e por lado)

| dupla | base | G1a | G1b só | G1a+b |
|---|---|---|---|---|
| Ryūga + Yin-lu | 68% | 71% (+3) | 65% (-3) | 57% (-11) |
| Kōga + p04 | 74% | 65% (-9) | 76% (+2) | 68% (-6) |
| p05 + p06 | 40% | 51% (+11) | 49% (+9) | 55% (+15) |
| p07 + p08 | 62% | 65% (+3) | 57% (-5) | 61% (-1) |
| p09 + p10 | 67% | 68% (+1) | 75% (+8) | 77% (+10) |
| p11 + p12 | 48% | 50% (+2) | 44% (-4) | 56% (+8) |
| p13 + p14 | 35% | 24% (-11) | 27% (-8) | 24% (-11) |
| p15 + p16 | 7% | 5% (-2) | 6% (-1) | 2% (-5) |

A nota de sinergia (C01) é medida com combo roteirizado e não depende da CPU. Ela não mudou: `G1a_sinergia.md`, `G1b_so_sinergia.md` e `G1ab_sinergia.md` são iguais à `D_sinergia.md` nos números de assist.

## Leitura

1. **A CPU passou a usar o kit.** S2–S4 foram de 0–0,5 para 0,3–5 por minuto por lutador, e cada um no papel dos dados: o repel da p15 desfaz 96% das bolas que espera, e o sumiço do p05 acerta 93%. Com isso os que tinham kit e não usavam sobem: p07 +16, p05 +10, Yin-lu +8. Os que dependiam do S1 descem, como o p06 −17.
2. **A régua real deixa a CPU mais competente e mexe no balanço.** Ela cutuca da ponta de verdade e não solta golpe alto em quem está agachado. O Kōga não erra mais o agachado com L/M/H, o que dá +7 sozinho no 1x1 e −2 a +4 na matriz: CPU mais certa, não mudança de balanço.
   - **Perdem:** o p06 (2M mais curto que o pacote: a CPU dele não cutuca mais de onde errava, e os outros chegam mais longe nele) e os lentos de alcance longo: p14 −10, p15 −4 a −9, p16 −6.
   - **Ganha:** o p09, +6 na matriz, e a dupla p09 + p10 vai a 77%.
3. **A diferença entre topo e fundo não fechou.** Na matriz, de 16–77% (base) passou a 13–78%. As duplas ainda não prontas pioram: p13 + p14 24% e p15 + p16 2%.

## DECIDIR (precisa de aprovação)

1. **p09 volta a subir** com a régua real e a armadura usada: 58 → 64% na matriz e p09 + p10 a 77%. A CPU usa a ombrada só com a carga feita, como um jogador. Isso reabre a 2ª iteração do p09 (velocidade 300 → 270) ou o `alcanceMaximo` 190 no 2M?
2. **p06 cai de 68% para 51%.** O 2M dele é 23 px mais curto que o pacote diz: a CPU antiga cutucava de onde errava, e com a régua real os rivais chegam nele. Ajustar o 2M (sprite ou alcance) ou aceitar?
3. **p14, p15 e p16 caem com a CPU melhor** (31%, 13%, 24%), e as duplas deles ficam em 24% e 2%. Somam-se ao C04 (não liberar ainda). O ganho viria de dado (alcance, quadros), não da CPU.
4. **A tabela do alcance é gerada.** Sprite ou golpe novo pede rodar o `GeraAlcanceCpu` (16 s), e o `CpuReachTest` avisa quando esquecem. Alternativa: medir em tempo de jogo, mas é caro para o aparelho (≈ 1 s por personagem). Ou ligar a geração ao `build_characters.py`.
5. **Constantes de frequência** (`SPECIAL_NEUTRAL_CHANCE` 0,06, `SPECIAL_REACT_CHANCE` 0,45, anti-aéreo 0,5, mergulho 0,35, corte de 16 q no neutro). Foram calibradas só CPU × CPU. Validar com humanos no playtest (T27/T28).
6. **Força do especial (L/M/H) para a CPU:** hoje sai sempre a do M. Dar à CPU a escolha da distância da pedra e do pavio da caixa pede um campo no `FighterInput` e uma linha no `CombatEngine`.
7. **Especiais lentos** (> 16 q: o Aparar, o escudo do p09, o Salto do colosso e a banana) ficam só na punição, então quase não aparecem. Se forem assinatura, precisam de dado mais rápido ou de uso fora do neutro (acordar o rival com eles, por exemplo).

## Arquivos

**Código e testes (nos diffs)**

| Arquivo | Etapa |
|---|---|
| `android/app/src/main/java/com/gamelutagpt/CpuSpecials.java` (novo) | G1a, ajustado na G1b |
| `android/app/src/main/java/com/gamelutagpt/AiController.java` | G1a e G1b |
| `android/app/src/main/java/com/gamelutagpt/CpuReach.java` (novo) | G1b |
| `android/app/src/main/java/com/gamelutagpt/CpuReachTable.java` (novo, gerado) | G1b |
| `android/app/src/main/java/com/gamelutagpt/OpponentAi.java` | G1b |
| `android/app/src/test/java/com/gamelutagpt/CpuSpecialsTest.java` (novo) | G1a |
| `android/app/src/test/java/com/gamelutagpt/BateriaEspeciaisCpu.java` (novo) | G1a |
| `android/app/src/test/java/com/gamelutagpt/CpuReachTest.java` (novo) | G1b |
| `android/app/src/test/java/com/gamelutagpt/GeraAlcanceCpu.java` (novo) | G1b |
| `android/app/src/test/java/com/gamelutagpt/CpuTest.java`, `CombatEngineTest.java`, `ExtractedSystemsTest.java` (a régua dos testes) | G1b |

**Resultados em `scratchpad/bal/`**

| Arquivo | Conteúdo |
|---|---|
| `G1a.diff`, `G1b.diff` | os diffs |
| `G1_antesdepois.md` | a `BateriaAntesDepois` nos quatro estados |
| `G1_forca_matriz.md`, `G1a_matriz.md`, `G1b_so_matriz.md`, `G1ab_matriz.md` | as matrizes |
| `G1a_sinergia.md`, `G1b_so_sinergia.md`, `G1ab_sinergia.md` | a sinergia |
| `G1_duplas_*.md`, `G1_duplas_tab.md` | as duplas oficiais |
| `G1_uso_*.md`, `G1_uso_vence.md` | o uso por minuto |
| `G1_espelhos.md` | os espelhos |
| `G1_funcoes.md`, `G1_ficha.md` | a função de cada especial |
| `G1a_suite.txt`, `G1b_suite.txt`, `G1b_so_suite.txt` | as suítes |

**Rascunhos de medida (fora dos diffs, em `scratchpad/g1/android/app/src/test`):** `EnsaioMergulho*`, `EnsaioMotivos`, `EnsaioRegua`, `EnsaioPedra`, `EnsaioLado`, `EnsaioFuncoes`.

**Docs:** nada mudou. Ao integrar, vale um parágrafo em `docs/combat-engine.md` ou `docs/balanco/ajustes.md` sobre a CPU com S2–S4 e a régua real.
