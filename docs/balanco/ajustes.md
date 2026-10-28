# Ajustes aprovados (antes/depois)

Cada decisão aprovada entra numa etapa separada, com um commit próprio. Antes e depois de cada etapa
roda a mesma bateria nas mesmas sementes (`BateriaAntesDepois`, CPU nível 8, motor real, sem
animação). Assim dá para ver qual mudança mexeu em quê.

Como ler os números:
- **Lutas 2x2 por personagem:** são ~75 lutas por personagem, com ruído de ±8 pontos. Diferença
  menor que isso não é efeito da mudança.
- **1x1 (contra o elenco):** tem ruído de ±3 pontos.
- **Comparações de regras** (sempre × nunca, Clash): têm ruído de ±5 pontos. Entre as etapas 2 e 3, que não mexeram no Breaker, "sempre quebra" foi de 56% para 61%.
- **A CPU joga dos dois lados.** O teste com jogadores continua necessário, como pede a auditoria.

Para rodar:
- bateria: `java ... com.gamelutagpt.BateriaAntesDepois saida.tsv [breaker,clash,tag,elenco,p09,combos]`;
- base de testes: os 537 testes JUnit do app (sem Robolectric) mais 199 dos módulos `motion-core`,
  `stage-core`, `story-core` e `ultra-core`, todos passando antes da etapa 1.

## Etapa 1 — Combo Breaker (P0)

**Decisão:** manter 1 barra e ativação a partir do 2º acerto; atordoamento de 44 para 20 quadros; tirar
o contra-ataque automático garantido; corrigir a decisão tardia da CPU.

**Arquivos:**
- `android/app/src/main/java/com/gamelutagpt/CombatConfig.java`, bloco do combo breaker:
  - `comboBreakerStunFrames` 44 → 20;
  - `comboBreakerChaseFrames` 40 → 0 (a corrida continua no motor como opção).
- `android/app/src/main/java/com/gamelutagpt/AiController.java`, `betOnComboBreaker`:
  - uma decisão por combo (`breakerSession`);
  - o aperto cabe no atordoamento que ainda falta.
- `CombatEngine.java`: só o comentário de `comboBreaker`.
- `docs/combat-engine.md`: seção do combo breaker.
- Testes: `ComboBreakerTest` (sai a corrida automática, entram a volta ao neutro e a corrida como
  opção).

**O bug da CPU:**
- **O que acontecia:** a decisão de quebrar só era zerada quando a CPU via um quadro em
  atordoamento sem combo ativo. Isso quase nunca acontece, então a decisão do primeiro combo valia
  para todos os seguintes, e ela quebrava 15% das chances em vez de ~45%.
- **Como ficou:** agora a decisão é por combo.

**Antes/depois (mesmas sementes):**

| métrica | antes | depois | diferença |
|---|---|---|---|
| breaker.casos | 107 | 107 | +0 |
| breaker.troco_garantido_script | 73% | 65% | -7 pts |
| breaker.combo_inteiro | 1587 | 1587 | +0 |
| breaker.dano_evitado | 810 | 810 | +0 |
| breaker.troco | 1320 | 551 | -769 |
| breaker.sempre_vs_nunca | 64% | 61% | -3 pts |
| breaker.cpu_vs_nunca | 48% | 51% | +3 pts |
| breaker.cpu_quebras_por_luta | 35% | 80% | +45 pts |
| breaker.cpu_quebra_das_chances | 15% | 36% | +21 pts |

- `troco`: o dano médio que a CPU tira de quem quebrou o combo.
- `troco_garantido_script`: a fração de quebras em que o melhor contra-ataque possível entra mesmo
  com quem batia defendendo assim que pode.
- `sempre_vs_nunca`: vitórias de "sempre quebra" contra "nunca quebra" em 300 lutas 2x2 espelhadas.

**Resultado:**
- O contra-ataque médio da CPU caiu 58% (1320 → 551).
- Quebrar continua valendo, mas menos: 64% → 61% de vitórias para quem sempre quebra; o controle
  "nunca × nunca" dá 48%.
- A CPU passou a quebrar 36% das chances (0,8 quebra por luta).

**Precisa de aprovação (o valor de 20 não devolve ao neutro):**
- **O problema:** com 20 quadros, quem quebrou ainda acerta um L de graça em 65% das quebras, mesmo
  com quem batia defendendo assim que pode. Ele fica livre 14 quadros depois do estalo, e quem batia
  só se recompõe 20 quadros depois.
- **A causa:** o alcance real do L (o desenho do golpe) passa da folga de 170 do empurrão. O L
  parado já alcança, e correndo chega com 4 quadros de sobra.

| alternativa | o que muda | custo |
|---|---|---|
| A. Atordoamento de 20 para ~4 | Quem batia defende o primeiro golpe | Quebrar vira só "sair do combo" |
| B. Manter 20 e empurrar mais (170 → ~320) | Só a corrida alcança e chega tarde | Mais espaço de tela entre os dois |
| C. Manter como está | Fica um L de troco quase garantido (~550 de dano médio) | Quebrar continua forte |


## Etapa 2 — SHINKAI Clash (P0)

**Decisão:**
- **CPU:** a eficácia das entradas fica igual entre os tipos.
- **TECHNIQUE:** a janela do pulso cai pela metade e só um toque vale por pulso.
- **ENERGY:** os toques passam a empurrar, e segurar continua valendo mais.

**Arquivos:**
- `android/app/src/main/java/com/gamelutagpt/ShinkaiClash.java`:
  - `JANELA_BATIDA` 4 → 2 (de 9 para 5 dos 24 quadros);
  - TECHNIQUE: o primeiro toque de cada pulso decide (105 no pulso, 10 fora) e os outros não
    contam;
  - ENERGY: toque curto, ou soltar uma carga pequena, empurra pelo menos `TOQUE_ENERGY` = 20 (antes
    8 no celular, 14 no teclado).
- `android/app/src/main/java/com/gamelutagpt/AiController.java`, `pushInClash` e `clashRate`:
  - todos os tipos miram o mesmo empurrão médio por quadro: 1,9 no nível 1 e 3,8 no 8 (o teto do
    ENERGY com a carga cheia);
  - cada confronto sorteia um fôlego de ±15% por CPU.
- `docs/shinkai.md`: tabela do Clash e parágrafo da CPU.
- Testes: `ShinkaiClashTest`:
  - `techniquePushesOnTheBeat` passa a apertar fora do pulso no pulso anterior;
  - novos: esmurrar perde para o ritmo, toque do ENERGY e média igual da CPU em todos os tipos.

**Antes/depois (mesmas sementes; o "antes" é o código depois da etapa 1):**

| métrica | antes | depois | diferença |
|---|---|---|---|
| clash.cpu_vence.POWER | 9% | 47% | +37 pts |
| clash.cpu_vence.SPEED | 29% | 49% | +20 pts |
| clash.cpu_vence.ENERGY | 59% | 40% | -19 pts |
| clash.cpu_vence.TECHNIQUE | 98% | 55% | -43 pts |
| clash.tela.POWER.8toques_s | 238 | 238 | +0 |
| clash.tela.POWER.12toques_s | 238 | 238 | +0 |
| clash.tela.POWER.melhor_jeito | 238 | 238 | +0 |
| clash.tela.SPEED.8toques_s | 298 | 298 | +0 |
| clash.tela.SPEED.12toques_s | 444 | 444 | +0 |
| clash.tela.SPEED.melhor_jeito | 298 | 298 | +0 |
| clash.tela.ENERGY.8toques_s | 64 | 162 | +98 |
| clash.tela.ENERGY.12toques_s | 96 | 242 | +146 |
| clash.tela.ENERGY.melhor_jeito | 254 | 254 | +0 |
| clash.tela.TECHNIQUE.8toques_s | 364 | 45 | -319 |
| clash.tela.TECHNIQUE.12toques_s | 375 | 45 | -330 |
| clash.tela.TECHNIQUE.melhor_jeito | 250 | 250 | +0 |
| shinkai.2x2_vence.POWER | 37% | 51% | +14 pts |
| shinkai.2x2_vence.SPEED | 47% | 52% | +5 pts |
| shinkai.2x2_vence.ENERGY | 54% | 46% | -8 pts |
| shinkai.2x2_vence.TECHNIQUE | 56% | 53% | -3 pts |

- `clash.cpu_vence.X`: vitórias de cada tipo da CPU nos confrontos contra os outros três (60 por
  par).
- `clash.tela.X`: empurrão por segundo com o dedo na tela (cada toque fica 3 quadros no botão);
  `melhor_jeito` é H no POWER, L no SPEED, carga cheia no ENERGY e só no pulso na TECHNIQUE.
- `shinkai.2x2_vence.X`: lutas 2x2 com o tipo forçado nos dois da dupla.

**Resultado:**
- **CPU:** nenhum tipo decide mais o confronto (40% a 55%, antes 9% a 98%).
- **Lutas 2x2:** os quatro tipos ficaram entre 46% e 53%.
- **ENERGY:** tocar rende 2,5 vezes mais (64 → 162 por segundo a 8 toques/s), e segurar e soltar
  continua melhor (254).
- **TECHNIQUE:** esmurrar o botão caiu de 364 para 45 por segundo; no ritmo continua 250.

**Riscos para o teste com jogadores:**
- **TECHNIQUE:** a janela agora tem 5 quadros (83 ms). Quem não pegar o ritmo empurra muito pouco;
  vale ver no aparelho se o anel do pulso deixa o ritmo claro.
- **SPEED a 12 toques/s:** passa todos (444 por segundo). É a habilidade de bater rápido, como o
  documento do Clash descreve; não mexi.

## Etapa 3 — TAG depois da assistência (P0)

**Decisão:**
- Sem troca instantânea universal.
- Buffer de 0,6 s quando o assist está terminando.
- Estados visuais de pedido pendente, execução e falha, e indicador de recarga.
- Manter as restrições de atordoamento e defesa e avisar quando a troca cai.

**Arquivos:**
- `android/app/src/main/java/com/gamelutagpt/TeamSystem.java`:
  - nova fase `ASSIST_WAIT`: o golpe do assist acaba com a troca pedida e o titular preso, então o
    parceiro espera parado até `assistTagBufferFrames` e a troca sai quando o titular se soltar;
  - `conversionOpen` passa a aceitar o TAG também enquanto o assist volta correndo;
  - `conversionPending`, `convertResult`, `convertFailReason` e `whyNoSwap` dão o motivo da queda.
- `CombatConfig.java`: `assistTagBufferFrames` = 36 (0,6 s).
- `GameView.java`:
  - `updateTagCooldownLabels`: texto "TROCA PEDIDA…" ou "TROCA: ESPERANDO VOCÊ SE SOLTAR", e o botão
    mostra "…";
  - `announceConversion`: avisa "TROCA!" ou "TROCA CANCELADA: APANHANDO / DEFENDENDO / CAÍDO";
  - o texto também passou a atualizar quando só o estado do pedido muda.
- `HudRenderer.java`, `cooldownArc`: um arco em volta do botão TAG mostra a recarga.
- `docs/combat-engine.md`: tabela do TAG.
- Testes: `TeamSystemTest`:
  - a troca recusada agora espera e cai com o motivo;
  - novos: a troca espera o titular se soltar, e o TAG com o assist já voltando.

**Antes/depois (mesmas sementes; o "antes" é o código depois da etapa 2):**

| métrica | antes | depois | diferença |
|---|---|---|---|
| tag.janela_quadros | 33 | 43 | +10 |
| tag.latencia_tag_cedo | 33 | 33 | +0 |
| tag.falha_com_rival_batendo | 31% | 0% | -31 pts |
| tag.cpu_pedidos | 2044 | 1961 | -83 |
| tag.cpu_perdidos | 22% | 8% | -14 pts |

- `janela_quadros`: até quantos quadros depois de chamar o assist o TAG ainda vira troca.
- `latencia_tag_cedo`: TAG apertado 1 quadro depois do assist, quadros até o parceiro agir.
- `falha_com_rival_batendo`: pedidos aceitos que não viraram troca, com a CPU nível 8 batendo.
- `cpu_perdidos`: o mesmo nas lutas CPU × CPU.

**Resultado:**
- **A troca que sumia sem aviso** com o rival batendo caiu de 31% para 0%. Nas lutas da CPU, de 22%
  para 8%; os 8% que sobram ficaram presos mais de 0,6 s e agora aparecem como "TROCA CANCELADA".
- **A janela** para pedir passou de 33 para 43 quadros.
- **O tempo de quem aperta cedo** continua 33 quadros, porque a troca sai quando o golpe do assist
  acaba, como decidido; o "TROCA PEDIDA…" mostra que o aperto foi aceito.
- **Não testado no aparelho:** as mudanças de HUD (texto, "…", aviso e arco) passaram nos testes do
  motor, mas não foram vistas no celular.

## Etapa 4a — p09: o 2M (P1, primeira iteração)

**Decisão:**
- 2M: alcance de 250 para 220 e dano de 620 para 560.
- Velocidade, vida e dano dos agarrões ficam como estão.
- Se continuar dominante, avaliar a velocidade de 300 para 270 numa segunda iteração.

**Arquivos:**
- `characters/p09/character.json`, 2M:
  - `damage` 620 → 560;
  - `reach` 250 → 220;
  - novo `alcanceMaximo` 220.
- `GeneratedCharacters.java`: a linha do 2M do p09.
- `AttackDefinition.java`: `reachCap`.
- `CombatEngine.golpeToca`: o desenho não acerta além do `reachCap`.
- `tools/sprites/build_characters.py`:
  - passa `alcanceMaximo` para o motor (`.reachCap(...)`);
  - recusa `reach` maior que ele.
- `tools/sprites/caixas_sprites.py`: a medida das caixas não passa do `alcanceMaximo`, para a ferramenta
  não desfazer o ajuste.
- `docs/sprite-standard.md`: o campo novo.

**O alcance (precisa de decisão):**
- **O que eu achei:** o `reach` de 250 do json estava acima do próprio desenho. A ponta desenhada do 2M
  vai até ~221 px, e é o desenho que decide o acerto. Por isso o 2M acerta até ~305 de distância entre
  os centros (a ponta mais o corpo de quem apanha), contra ~220 do 2M da Yin-lu.
- **O efeito do ajuste:** baixar o `reach` para 220 alinhou o número ao desenho. Isso muda quando a
  CPU usa o 2M, mas a distância de acerto só cai ~1 px.
- **Para encurtar o acerto de verdade em 30 px:** basta `alcanceMaximo` 190 (o motor já corta no
  limite). Não apliquei sem aprovação.

**Cada metade medida sozinha (mesmas sementes; o "antes" é o código depois da etapa 3):**

| métrica | antes | só o dano 560 | só o alcance 220 | os dois |
|---|---|---|---|---|
| 1x1.p09 | 82% | 81% | 75% | 74% |
| 1x1.p09_vs_p02 | 98% | 98% | 85% | 85% |
| 2x2.p09 | 77% | 76% | 81% | 82% |
| p09.vs_p02_400 | 92% | 92% | 90% | 90% |
| p09.dano_por_acerto | 774 | 746 | 752 | 733 |
| p09.parte_2M | 34% | 32% | 27% | 25% |
| p09.parte_agarroes | 1% | 1% | 1% | 1% |
| 1x1.p02 | 44% | 44% | 44% | 44% |

**Resultado:**
- **O dano sozinho** quase não muda as vitórias (−1 a −2 pontos); o dano por acerto cai 4%.
- **O `reach` 220** é o que mais mexe: a CPU do p09 usa menos o 2M (34% → 27% do dano dele). Contra o
  elenco no 1x1 ele cai de 82% para 75%.
- **Os dois juntos:** o p09 fica com 74% contra o elenco (era 82%).
- **Ele continua o mais forte do 1x1.** Contra a Yin-lu, o detalhe de 400 lutas ainda dá 90%.
- **Efeito da etapa 2:** o p09 contra a Yin-lu tinha subido de 78% para 92%. Antes a CPU da Yin-lu
  (TECHNIQUE) ganhava todos os Clash contra o p09 (POWER), e isso era 16% do dano dela.
- **Lutas 2x2:** o p09 foi de 77% para 82%, dentro do ruído de ±8 pontos.
- **Testes:** os 541 do app passam, e o `build_characters.py --check` confere o arquivo gerado.

Todos os 16 personagens (1x1 contra o elenco e 2x2), antes e depois da etapa 4a:

| métrica | antes | depois | diferença |
|---|---|---|---|
| 1x1.Ryūga (p01) | 68% | 69% | +2 pts |
| 1x1.Yin-lu (p02) | 44% | 44% | +1 pts |
| 1x1.Kōga (p03) | 60% | 60% | +0 pts |
| 1x1.p04 | 54% | 54% | +0 pts |
| 1x1.p05 | 41% | 42% | +1 pts |
| 1x1.p06 | 68% | 69% | +0 pts |
| 1x1.p07 | 13% | 13% | +0 pts |
| 1x1.p08 | 78% | 79% | +0 pts |
| 1x1.p09 | 82% | 74% | -8 pts |
| 1x1.p10 | 57% | 57% | +1 pts |
| 1x1.p11 | 54% | 56% | +1 pts |
| 1x1.p12 | 44% | 44% | +1 pts |
| 1x1.p13 | 55% | 56% | +1 pts |
| 1x1.p14 | 40% | 40% | +0 pts |
| 1x1.p15 | 16% | 16% | +0 pts |
| 1x1.p16 | 27% | 27% | +0 pts |
| 1x1.p09_vs_p02 | 98% | 85% | -12 pts |
| 2x2.Ryūga (p01) | 64% | 65% | +1 pts |
| 2x2.Yin-lu (p02) | 42% | 42% | +0 pts |
| 2x2.Kōga (p03) | 50% | 49% | -2 pts |
| 2x2.p04 | 56% | 53% | -3 pts |
| 2x2.p05 | 41% | 41% | -1 pts |
| 2x2.p06 | 59% | 59% | +0 pts |
| 2x2.p07 | 34% | 32% | -3 pts |
| 2x2.p08 | 74% | 73% | -1 pts |
| 2x2.p09 | 77% | 82% | +5 pts |
| 2x2.p10 | 55% | 53% | -2 pts |
| 2x2.p11 | 53% | 53% | +0 pts |
| 2x2.p12 | 45% | 45% | +0 pts |
| 2x2.p13 | 55% | 55% | -0 pts |
| 2x2.p14 | 36% | 36% | +0 pts |
| 2x2.p15 | 22% | 22% | +0 pts |
| 2x2.p16 | 40% | 44% | +4 pts |

## Etapa 4b — p09: a CPU agarra mais (P1)

**Decisão:** ajustar a CPU do p09 para explorar mais agarrões.

**Arquivos:**
- `CpuStyle.java`:
  - novo estilo de CPU "agarrador": distância preferida 80, agarrar ×4,0, projétil ×0,3, pulo ×0,5;
  - `CpuStyle.of` usa `fighter.cpuStyle` quando o personagem tem um.
- `CharacterDefinition.java`: `Fighter.cpuStyle`.
- `OpponentAi.java`: o agarrador, perto mas fora do agarrão (até 280), em 45% das decisões deixa a
  cutucada e anda para dentro (`GRAPPLER_CLOSE_IN`).
- `ComboBook.java`: o agarrador fecha os combos pela parede, como o tanque.
- `characters/p09/character.json`: `"cpuStyle": "agarrador"`.
- `tools/sprites/mecanicas.json`: `"estiloCpu": "agarrador"`.
- `tools/sprites/mecanicas.py` e `tools/sprites/build_characters.py`: levam o campo.
- `GeneratedCharacters.java`: `.withCpuStyle("agarrador")`.
- Fica igual: as correntes de combo continuam as do tanque (`comboStyle`), e p07 e p13 não mudam.

**Antes/depois (mesmas sementes; o "antes" é o código depois da etapa 4a):**

| métrica | antes | depois | diferença |
|---|---|---|---|
| 1x1.p07 | 13% | 14% | +1 pts |
| 1x1.p09 | 74% | 62% | -12 pts |
| 1x1.p13 | 56% | 56% | +0 pts |
| 1x1.p09_vs_p02 | 85% | 65% | -20 pts |
| 2x2.p09 | 82% | 75% | -7 pts |
| p09.vs_p02_400 | 90% | 68% | -22 pts |
| p09.dano_por_acerto | 733 | 746 | +13 |
| p09.parte_agarroes | 1% | 8% | +7 pts |
| p09.parte_2M | 25% | 19% | -6 pts |

**Resultado:**
- **Agarrões:** passaram de 1% para 8% do dano do p09, e o 2M caiu de 25% para 19%.
- **Vitórias:** entrando para agarrar ele se expõe mais. Contra o elenco no 1x1, 74% → 62%; contra a
  Yin-lu, 90% → 68%.
- **Velocidade (300 → 270):** não precisou na segunda iteração. Ele saiu do topo do 1x1 só com o 2M e
  a CPU; a decisão fica para depois do teste com jogadores.
- **Atenção:** a queda vem da CPU, e um p09 humano que só cutuca com o 2M não muda. O teste com
  jogadores é o que diz se o p09 humano ainda domina.
- **Testes:** os 541 do app passam, e o `build_characters.py --check` confere o arquivo gerado.
