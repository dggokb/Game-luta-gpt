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

## Etapa 5 — barra em combos longos e depois do Super (P1)

**Decisão:**
- Redução progressiva da barra gerada em combos longos, e uma redução a mais depois do Super.
- Reavaliar as rotas do Ryūga antes de mexer no dano base.

**Arquivos:**
- `ComboSession.java`, `meterGain`:
  - barra cheia até o 3º acerto do combo;
  - depois, 10% a menos por acerto, até um piso de 30%;
  - com um Super já no combo, metade disso.
- `CombatConfig.java`: `comboMeterFullHits` 3, `comboMeterStepPermille` 100, `comboMeterFloorPermille`
  300 e `comboMeterAfterSuperPermille` 500.
- `CombatEngine.java`: o acerto usa `session.meterGain`.
- `docs/combat-engine.md`: barra do time.
- Bateria (`BateriaBalanco.executar`): mede a barra ganha na rota e os Supers usados, e aceita um
  preparo; a métrica nova é o combo com o SHINKAI ligado desde o começo.

**Antes/depois (mesmas sementes; o "antes" é o código depois da etapa 4b):**

| métrica | antes | depois | diferença |
|---|---|---|---|
| combo.rota_ryuga_0barras | 7997 | 0 | -7997 |
| combo.rota_ryuga_barra_ganha | 2600 | 0 | -2600 |
| combo.rota_ryuga_supers | 2 | 0 | -2 |
| combo.barra_ganha_media_0barras | 1847 | 1422 | -424 |
| combo.supers_media_0barras | 1 | 1 | -0 |
| combo.shinkai_ENERGY.Ryūga (p01) | 13159 | 10308 | -2851 |
| combo.shinkai_ENERGY.p09 | 10362 | 10362 | +0 |
| combo.shinkai_TECHNIQUE.Ryūga (p01) | 10775 | 9598 | -1177 |
| combo.shinkai_TECHNIQUE.p09 | 9153 | 9153 | +0 |
| combo.shinkai_POWER.Ryūga (p01) | 9880 | 7550 | -2330 |
| combo.shinkai_POWER.p09 | 8040 | 8040 | +0 |
| combo.shinkai_SPEED.Ryūga (p01) | 11661 | 9334 | -2327 |
| combo.shinkai_SPEED.p09 | 8654 | 8654 | +0 |

- `rota_ryuga_*`: a rota de 58% que o Ryūga fazia começando sem barra (M H assist S SUPER dash pulo
  j2H jM S SUPER …), refeita igual. 0 = ela não fecha mais.
- `barra_ganha_media_0barras`: quanta barra a maior rota sem barra de cada personagem gera dentro dela
  mesma (média dos 16).
- `shinkai_X`: o maior combo achado com o SHINKAI daquele tipo ligado, começando sem barra.

**Resultado:**
- **A rota de dois Supers do Ryūga** começando sem barra não fecha mais: o combo não gera barra para o
  segundo.
- **O maior combo dele sem barra** caiu de 8202 para 7390 (de 59% para 53% da vida média). Com 1 barra,
  de 9259 para 8465.
- **Com SHINKAI,** o maior combo do Ryūga caiu de 13159 para 10308 (ENERGY) e de 11661 para 9334
  (SPEED).
- **O p09 não mudou:** as rotas dele não vivem da barra gerada no combo.
- **Na média dos 16,** a maior rota sem barra gera 23% menos barra (1847 → 1422).
- **O dano base do Ryūga ficou como está,** como pedido. Ele continua com o maior combo do elenco, e o
  próximo passo é olhar a rota dele com jogadores antes de mexer no dano.
- **Lutas:** sem mudança fora do ruído.
- **Testes:** os 541 do app passam.

Os 16 personagens, maior combo sem barra e com 1 barra:

| métrica | antes | depois | diferença |
|---|---|---|---|
| combo.max_0barras.Ryūga (p01) | 8202 | 7390 | -812 |
| combo.max_1barra.Ryūga (p01) | 9259 | 8465 | -794 |
| combo.max_0barras.Yin-lu (p02) | 4798 | 4798 | +0 |
| combo.max_1barra.Yin-lu (p02) | 4288 | 4288 | +0 |
| combo.max_0barras.Kōga (p03) | 5718 | 5718 | +0 |
| combo.max_1barra.Kōga (p03) | 4669 | 4669 | +0 |
| combo.max_0barras.p04 | 5552 | 5552 | +0 |
| combo.max_1barra.p04 | 5408 | 5408 | +0 |
| combo.max_0barras.p05 | 6041 | 6041 | +0 |
| combo.max_1barra.p05 | 4629 | 4629 | +0 |
| combo.max_0barras.p06 | 6005 | 6005 | +0 |
| combo.max_1barra.p06 | 3922 | 3922 | +0 |
| combo.max_0barras.p07 | 7385 | 7385 | +0 |
| combo.max_1barra.p07 | 6481 | 6481 | +0 |
| combo.max_0barras.p08 | 6846 | 6846 | +0 |
| combo.max_1barra.p08 | 4339 | 4339 | +0 |
| combo.max_0barras.p09 | 6924 | 6924 | +0 |
| combo.max_1barra.p09 | 8676 | 7906 | -770 |
| combo.max_0barras.p10 | 6407 | 6407 | +0 |
| combo.max_1barra.p10 | 4730 | 4730 | +0 |
| combo.max_0barras.p11 | 4407 | 4407 | +0 |
| combo.max_1barra.p11 | 5996 | 6219 | +223 |
| combo.max_0barras.p12 | 5926 | 5926 | +0 |
| combo.max_1barra.p12 | 5751 | 5751 | +0 |
| combo.max_0barras.p13 | 7115 | 7115 | +0 |
| combo.max_1barra.p13 | 7115 | 7115 | +0 |
| combo.max_0barras.p14 | 5737 | 5737 | +0 |
| combo.max_1barra.p14 | 6636 | 5649 | -987 |
| combo.max_0barras.p15 | 5700 | 5700 | +0 |
| combo.max_1barra.p15 | 3840 | 3840 | +0 |
| combo.max_0barras.p16 | 6707 | 6707 | +0 |
| combo.max_1barra.p16 | 6707 | 6707 | +0 |

## Etapa H8a — Ryūga agachado com o corpo medido no desenho (colisões, item 8)

Decisão do usuário (lista G, item 8): medir o Ryūga agachado. O agachar dele era o único dos 16
sem caixas medidas: o motor usava a caixa genérica (±36 px, 157 de altura), e o desenho agachado
tem os pés a −77/+64 px. O 2M e o 2L dados na ponta encostavam o pé no pé dele e não acertavam
(T25, achado 3).

- **Arquivos:**
  - `tools/sprites/caixas_sprites.py`: `AGACHAR_CALIBRADO` passa a ter a escala de cada quadro do
    CROUCH do Ryūga, a mesma do `SpriteFighterRenderer.P01_CROUCH`. O corpo agachado é o último
    quadro medido, reduzido por ela em volta da raiz, como o jogo mostra.
  - `characters/player_base/character.json`: `animations.CROUCH.silhueta`, com o quadro 7 em três
    faixas: [−77, 64, 2, 58], [−65, 48, 58, 114] e [−59, 40, 114, 169].
  - `GeneratedCharacters.java`: a linha do CROUCH do player_base ganha `.withSilhueta(...)`.
  - `CpuReachTable.java` foi regenerada pela `GeraAlcanceCpu`. A régua da CPU contra agachado é
    medida no Ryūga, então ela muda para todos: L e M de vários personagens passam a pegar o Ryūga
    agachado.
- **Teste novo:** `AgachadoDoRyugaTest`, com 2 testes.
  - Confere que o agachar tem caixas medidas.
  - Confere que o 2M da p02 a 190 px, do p13 a 310 px e do p09 a 280 px acerta o Ryūga agachado.
  - Os dois testes falham no código anterior e passam no novo.
- **Suíte:** 562 de 562.
- **Gerador:** `build_characters.py` passa, com 19 personagens e 657 atlas.

**T25 contra o Ryūga agachado** (`BateriaColisoes`, golpes de chão, 16 atacantes, mesmas distâncias):

| | antes | depois |
|---|---|---|
| falhas do 2M e do 2L na ponta | 33 | 0 |
| falhas no total (o golpe passa pelo corpo e não acerta) | 137 | 67 |
| acertos de L, M, H, 2L, 2M e 2H | 1097 | 1314 |
| fantasmas (o motor acerta sem os pixels encostarem) | 181 | 263 |

- **Fantasmas novos:** são 87, quase todos de M, H e 2H (ver a imagem `p04_M_agachado_240`).
  - O corpo medido confere com o desenho.
  - A folga vem da caixa do membro de quem bate, que agora alcança um corpo do tamanho certo.
  - É o mesmo fantasma que a frente E já via contra a p02 agachada; é tratado na H8c.
- **Falhas que ficaram:** 67, em L e M (31 cada).
  - Na frente E, essas eram "corpo, não golpe": quem bate avança o corpo e o punho passa por cima.

**Bateria fixa:**

| métrica | antes | depois | diferença |
|---|---|---|---|
| breaker.sempre_vs_nunca | 66% | 62% | -3 |
| breaker.cpu_vs_nunca | 57% | 52% | -5 |
| shinkai.2x2_vence.SPEED | 55% | 52% | -3 |
| 1x1.Ryūga (p01) | 64% | 62% | -2 |
| 1x1.Yin-lu (p02) | 46% | 48% | +2 |
| 1x1.Kōga (p03) | 63% | 60% | -2 |
| 1x1.p04 | 64% | 61% | -3 |
| 1x1.p05 | 54% | 54% | +1 |
| 1x1.p06 | 53% | 53% | +0 |
| 1x1.p07 | 28% | 27% | -1 |
| 1x1.p08 | 79% | 78% | -1 |
| 1x1.p09 | 63% | 65% | +2 |
| 1x1.p10 | 59% | 60% | +1 |
| 1x1.p11 | 61% | 63% | +2 |
| 1x1.p12 | 47% | 48% | +1 |
| 1x1.p13 | 55% | 55% | +0 |
| 1x1.p14 | 32% | 32% | +1 |
| 1x1.p15 | 8% | 8% | -0 |
| 1x1.p16 | 26% | 26% | -1 |
| 1x1.p09_vs_p02 | 57% | 75% | +18 |
| 2x2.Ryūga (p01) | 60% | 64% | +4 |
| 2x2.Yin-lu (p02) | 45% | 53% | +8 |
| 2x2.Kōga (p03) | 46% | 56% | +10 |
| 2x2.p04 | 57% | 50% | -7 |
| 2x2.p05 | 50% | 41% | -8 |
| 2x2.p06 | 53% | 51% | -2 |
| 2x2.p07 | 41% | 44% | +4 |
| 2x2.p08 | 59% | 64% | +5 |
| 2x2.p09 | 66% | 68% | +2 |
| 2x2.p10 | 50% | 48% | -2 |
| 2x2.p11 | 54% | 52% | -2 |
| 2x2.p12 | 55% | 51% | -3 |
| 2x2.p13 | 55% | 52% | -3 |
| 2x2.p14 | 37% | 39% | +2 |
| 2x2.p15 | 23% | 24% | +1 |
| 2x2.p16 | 31% | 37% | +6 |

O `1x1.p09_vs_p02` é uma célula de poucas lutas. A medida de 400 lutas (`p09.vs_p02_400`) foi de
69% para 68%.

**Duplas oficiais** (T21, base → H8a):

| dupla | antes | depois |
|---|---|---|
| Ryūga + Yin-lu | 64% | 54% |
| Kōga + p04 | 55% | 59% |
| p05 + p06 | 36% | 41% |
| p07 + p08 | 63% | 64% |
| p09 + p10 | 79% | 77% |
| p11 + p12 | 64% | 64% |
| p13 + p14 | 39% | 39% |
| p15 + p16 | 2% | 5% |

O T21 tem 8 lutas por adversário e ruído de cerca de ±10. Ryūga + Yin-lu caiu 10, mas no 2x2 da
bateria fixa o Ryūga subiu de 60% para 64%.

## Etapa H8b — Investida (S1) do p12 bate pelo bote desenhado (colisões, item 8)

Decisão do usuário (item 8): revisar a Investida do p12. O corpo avança 112 px nos quadros 0–13
e, ao mesmo tempo, o desenho salta até 290 px à frente da raiz (quadros 5 e 6 do clipe). O motor
usava só a caixa do golpe (120 px), porque a regra geral é que "no avanço o desenho já mostra o
caminho". Nos quadros ativos (10–13) o clipe ainda mostrava o quadro 4, e o bote aparecia logo
depois da janela. A 320 px o Necro atravessava o rival no desenho sem acertar (T25, achado 4).

- **Arquivos:**
  - `AttackDefinition.Mechanics.drawnStrike` é novo, com o construtor `drawnStrike()`.
  - `CombatEngine.golpeNoDesenho`: num avanço com `drawnStrike`, a ponta desenhada decide a pancada,
    e não a caixa do golpe.
  - `tools/sprites/build_characters.py`: a mecânica `"desenho": true`, que exige `avanco`, gera
    `.drawnStrike()`. O código do gerador mudou, então os 657 atlas foram refeitos uma vez.
  - `tools/sprites/mecanicas.py` e `mecanicas.json`: em `p12.s1.corpoACorpo`, `"desenhoBate": true`.
  - `characters/p12/character.json`:
    - `energy.attack.mecanica.desenho = true`;
    - o `SPECIAL_ENERGY` passa de `impactFrame` 4 para 5 (o bote);
    - `durationsMs` refeito pelo `fit_impact`, com o mesmo total: o bote começa junto com a janela
      ativa.
  - `CpuReachTable` regenerada: o alcance real da S do p12 foi de 264 para 432 px.
- **Sem mudança:** dano (940), quadros (10/4/15), custo de vida e o avanço de 520 px/s nos quadros
  0–13.
- **Teste novo:** `InvestidaDoNecroTest`, com 3 testes.
  - Acerta a 320 e a 200 px.
  - Erra a 520 px.
  - O dano é o mesmo.
  - Os testes de acerto e de mecânica falham no código anterior.
- **Suíte:** 565 de 565.
- **Gerador:** passa, com 19 personagens e 657 atlas.

**T25 da Investida** (`BateriaColisoes p12`, S nas 3 distâncias, em pé e agachado):

| distância | antes | depois |
|---|---|---|
| 200, em pé | fantasma (13 px) | acerta encostando |
| 320, em pé | atravessa sem acertar (6451 px sobrepostos) | acerta encostando |
| 450, em pé | atravessa sem acertar (435 px) | acerta encostando |
| 200, agachado | fantasma (28 px) | acerta encostando |
| 320, agachado | atravessa sem acertar (6264 px) | acerta encostando |
| 450, agachado | atravessa sem acertar (283 px) | não acerta, sem encostar |

No p12 inteiro (726 tentativas), os fantasmas foram de 59 para 57 e as falhas de 13 para 9.

**Bateria fixa** (H8a → H8b). Só as linhas do p12 e do p11 mudaram; o resto ficou idêntico.

| 1x1.p11 | 63% | 62% | -1 |
| 1x1.p12 | 48% | 61% | +13 |
| 2x2.p11 | 52% | 49% | -4 |
| 2x2.p12 | 51% | 62% | +10 |

**Atenção para balanço:** a correção é de geometria, mas a Investida passou a pegar até 432 px. O
p12 subiu 13 pontos no 1x1 e 10 no 2x2. A matriz 16 × 16 da frente H-geo vai dizer se ele passou do
grupo do meio. Se passar, as opções que não contradizem o desenho são duas: encurtar o salto no
clipe (arte) ou reduzir o avanço de 520 px/s. A decisão é do usuário.

**Duplas oficiais** (T21, H8a → H8b): p11 + p12 64 → 66%; as outras ficaram dentro do ruído (±10).
O p12 virou o melhor parceiro do Ryūga (73%) e do p09 (82%).


## Etapa H8c — membro na diagonal em duas caixas (colisões, item 8)

Decisão do usuário (item 8): corrigir golpes que não correspondem ao desenho, inspecionar L/M/H do
Kōga e não fazer o H acertar agachado em todo mundo. No T25 (frente E), 11% dos acertos eram
fantasmas. A causa principal é o membro medido num retângulo só: num chute alto, num 2H que sobe
ou num soco que desce, o canto vazio do retângulo, do quadril até a frente do pé, encosta na
cabeça de quem está agachado sem o desenho encostar.

- **Regra:** em cada quadro, o membro medido (`caixas_sprites.ponta`) é partido ao meio na
  horizontal. Cada metade ganha a sua faixa de altura. Ficam duas caixas quando as duas metades
  cobrem menos de 75% do retângulo inteiro (`DIAGONAL = 0.75`); nos outros quadros continua uma
  caixa só. É geometria pura, sem regra por personagem ou por golpe.
- **Arquivos:**
  - `tools/sprites/caixas_sprites.py`: `DIAGONAL` e a partição em `ponta`;
  - `tools/sprites/build_characters.py`: `validate_silhueta` aceita o golpe com 4 ou 8 números;
  - `CharacterDefinition.withSilhueta`: aceita 4 ou 8 números;
  - `CombatEngine.golpeToca` testa cada caixa (`membroToca`). A primeira caixa ainda vem desde a
    frente do corpo, como antes; o `reachCap` vale nas duas. `minDe`/`maxDe` servem o cruzado e o
    ponto do choque;
  - `DebugOverlay` desenha as duas caixas;
  - os `character.json` dos 17 pacotes e o `GeneratedCharacters` foram regenerados (657 atlas, o
    código do gerador mudou);
  - a `CpuReachTable` foi regenerada: p02 H 240 → 230 em pé e 234 → 210 agachado; p04 L agachado
    158 → 104; p06 H agachado 160 → 146, entre outros;
  - `BateriaColisoes` (pc) mede e desenha as duas caixas.
- **Teste novo:** `MechanicsTest.aDiagonalLimbIsTwoBoxesAndItsEmptyCornerDoesNotHit`.
  - O M do p09 desce na diagonal (duas caixas).
  - A 180 px ele passa por cima do Ryūga agachado e não acerta; em pé, acerta.
  - Antes era um acerto fantasma do T25.
- **Suíte:** 567 de 567.
- **Gerador:** `--check` passa, com 19 personagens e 657 atlas.

**T25 completo** (16 personagens, 11 676 tentativas, rival Ryūga em pé e agachado; mesmas distâncias e apertos):

| medida | antes | depois |
|---|---|---|
| tentativas | 11676 | 11676 |
| acertos | 5973 | 5933 |
| fantasma | 985 | 950 |
| fant>25 | 376 | 342 |
| fant>50 | 113 | 85 |
| maior | 106 | 106 |
| falha | 249 | 255 |

- **Onde mudou:**
  - H contra agachado: fantasmas 86 → 70; acima de 50 px, 26 → 11;
  - H em pé: fantasmas 37 → 29; acima de 50 px, 8 → 2;
  - M contra agachado: fantasmas 47 → 42.
- **Por personagem:** a p15 foi de 40 para 31 fantasmas; a p07, de 45 para 39.
- **Sem mudança:** 2H, pulos e cruzado. Os maiores fantasmas que sobram são o jM do p05 (13 acima
  de 50 px), o 2H do p10 (12) e o 2H do p14 (9). O 2H que sobe cobre mais de 75% do retângulo e não
  é partido.
- **As 6 "falhas" novas não são falhas.** São o M do p09 e o L do p04 contra agachado, a 150–210 px.
  A foto conferida mostra o soco passando por cima da cabeça. O que se sobrepõe é a perna e o
  tronco de quem bate, inclinados, e não o golpe (achado 5 da frente E). Antes eram acertos
  fantasmas pelo canto do retângulo.

**Kōga L/M/H (inspecionados):** nenhum dos três pega quem está agachado, nem antes nem depois (0 de
23 cada). O desenho também não encosta: os golpes passam por cima. Em pé, o H tem 8 fantasmas de
até 41 px. Esse chute não foi partido (cobre mais de 75% do retângulo), e fica para olhar no
aparelho (item 1.3 de `aparelho.md`). O H não ficou capaz de pegar agachado em ninguém: os acertos
de H contra agachado caíram de 250 para 240.

**Bateria fixa** (H8b → H8c, mesmas sementes). Mostra o 1x1 e o 2x2 de todos e o que passou de 3%:

| métrica | antes | depois | diferença |
|---|---|---|---|
| breaker.troco_garantido_script | 57% | 61% | +4 |
| breaker.cpu_quebra_das_chances | 37% | 34% | -4 |
| shinkai.2x2_vence.POWER | 61% | 53% | -8 |
| shinkai.2x2_vence.ENERGY | 52% | 44% | -8 |
| shinkai.2x2_vence.TECHNIQUE | 53% | 49% | -4 |
| 1x1.Ryūga (p01) | 61% | 62% | +1 |
| 1x1.Yin-lu (p02) | 48% | 47% | -1 |
| 1x1.Kōga (p03) | 59% | 59% | -0 |
| 1x1.p04 | 60% | 60% | -0 |
| 1x1.p05 | 55% | 56% | +1 |
| 1x1.p06 | 52% | 52% | -1 |
| 1x1.p07 | 26% | 21% | -5 |
| 1x1.p08 | 75% | 75% | -0 |
| 1x1.p09 | 63% | 64% | +1 |
| 1x1.p10 | 59% | 61% | +2 |
| 1x1.p11 | 62% | 61% | -1 |
| 1x1.p12 | 61% | 61% | -0 |
| 1x1.p13 | 54% | 55% | +0 |
| 1x1.p14 | 31% | 36% | +4 |
| 1x1.p15 | 8% | 8% | +0 |
| 1x1.p16 | 25% | 24% | -1 |
| 1x1.p09_vs_p02 | 75% | 70% | -5 |
| 2x2.Ryūga (p01) | 59% | 56% | -3 |
| 2x2.Yin-lu (p02) | 53% | 47% | -6 |
| 2x2.Kōga (p03) | 45% | 47% | +2 |
| 2x2.p04 | 41% | 47% | +6 |
| 2x2.p05 | 41% | 40% | -1 |
| 2x2.p06 | 43% | 50% | +6 |
| 2x2.p07 | 47% | 34% | -13 |
| 2x2.p08 | 58% | 64% | +6 |
| 2x2.p09 | 66% | 68% | +2 |
| 2x2.p10 | 56% | 63% | +7 |
| 2x2.p11 | 49% | 49% | +0 |
| 2x2.p12 | 62% | 59% | -2 |
| 2x2.p13 | 53% | 50% | -2 |
| 2x2.p14 | 45% | 43% | -2 |
| 2x2.p15 | 18% | 25% | +7 |
| 2x2.p16 | 37% | 37% | +0 |
| p09.vs_p02_400 | 68% | 76% | +8 |
| combo.max_0barras.p05 | 6041 | 5779 | -262 |
| combo.max_1barra.p07 | 6481 | 7371 | +890 |

Tudo ficou dentro do ruído (±8 por personagem). A maior variação foi o p07 no 2x2 (−13), na borda
do ruído; no 1x1 ele foi de 26% para 21% (−5). O combo de 1 barra do p07 subiu (+890): a busca de rotas
achou outra rota com as caixas novas (não investiguei qual golpe abriu ela).

**Duplas oficiais** (T21, H8b → H8c):

| titular | oficial | vitórias | alternativo 1 | alternativo 2 | alternativo 3 | melhor parceiro |
|---|---|---|---|---|---|---|
| Ryūga (p01) | Yin-lu (p02) | 54% → 48% | p04 66% → p04 64% | p07 63% → p07 66% | p12 73% → p12 68% | p12 |
| Kōga (p03) | p04 | 55% → 63% | p06 48% → p06 46% | p09 57% → p09 68% | p14 48% → p14 41% | p09 |
| p05 | p06 | 39% → 45% | p08 63% → p08 61% | p11 45% → p11 55% | p16 36% → p16 34% | p08 |
| p07 | p08 | 63% → 55% | p10 45% → p10 48% | p13 50% → p13 52% | Yin-lu (p02) 34% → Yin-lu (p02) 27% | oficial |
| p09 | p10 | 75% → 71% | p12 82% → p12 86% | p15 38% → p15 45% | p04 79% → p04 70% | p12 |
| p11 | p12 | 66% → 71% | p14 46% → p14 50% | Ryūga (p01) 66% → Ryūga (p01) 63% | p06 43% → p06 54% | oficial |
| p13 | p14 | 38% → 39% | p16 29% → p16 30% | Kōga (p03) 54% → Kōga (p03) 52% | p08 75% → p08 71% | p08 |
| p15 | p16 | 5% | Yin-lu (p02) 7% → Yin-lu (p02) 11% | p05 9% → p05 4% | p10 13% → p10 23% | p10 |

Todas ficaram dentro do ruído (±10).


## Etapa H3 — p06: o 2M bate com a perna esticada (item 3)

Decisão do usuário (item 3): aceitar o 51% do p06 e corrigir os 23 px do 2M só conforme a
geometria real e a intenção do golpe, sem buff por winrate.

**O que eram os 23 px.** O pacote declara alcance 259 e `impactFrame` 6, o quadro da perna
esticada, com a ponta a 259 px da raiz. O motor escolhe o quadro do desenho pelo meio de cada
quadro de jogo. O quadro 6 durava só o último quadro de preparação (q6), e na janela ativa (q7–q10)
o desenho já recolhia: 190 px, depois 127. A régua da CPU (G1b) mediu então 272 contra 295 do
pacote, eixo a eixo. O `validate_impacts` do gerador aceita o impacto até um quadro antes da janela,
e é por isso que passava.

- **Correção, de tempo do clipe e sem dado de golpe:** `characters/p06/character.json`,
  `CROUCH_MEDIUM.durationsMs`, com o mesmo total de 403 ms.
  - A preparação (quadros 0–5) passa de 108 para 118 ms.
  - O recolher (quadro 7) passa de 38 para 28 ms.
  - Assim o quadro 6 (a perna esticada, 17 ms) cai no primeiro quadro ativo (q7). Os quadros
    seguintes da janela continuam o recolher.
- **Sem mudança:** dano (420), quadros (7/4/13), alcance declarado (259), caixas e vantagem.
- **Arquivos:**
  - `characters/p06/character.json`;
  - `GeneratedCharacters.java`, gerado;
  - `CpuReachTable` regenerada: p06 2M 236/230 → 306/298 (em pé/agachado, eixo a eixo contra o Ryūga).
- **Teste novo:** `MechanicsTest.p06LowKickHitsWithTheStretchedLeg`. No primeiro quadro ativo, a
  ponta do desenho é o alcance declarado (259). No clipe anterior dava 190, e o teste falha.
- **Suíte:** 568 de 568.
- **Gerador:** passa, com 657 atlas reaproveitados.

**Bateria fixa** (H8c → H3, mesmas sementes). Só estas linhas mudaram mais de 3%:

| métrica | antes | depois | diferença |
|---|---|---|---|
| breaker.dano_evitado | 935 | 984 | +48 |
| 1x1.p05 | 56% | 55% | -1 |
| 1x1.p06 | 52% | 59% | +7 |
| 2x2.p05 | 40% | 35% | -5 |
| 2x2.p06 | 50% | 51% | +1 |

**Matriz 16 × 16** (força contra o elenco, nível 8, 10 sementes por célula). A coluna "G1b" é a base
da lista H, o HEAD antes de H8a. A coluna "H8c" acumula H8a, H8b e H8c.

| personagem | G1b (29ca0716) | H8c | H3 |
|---|---|---|---|
| p08 | 78% | 77% | 76% |
| p04 | 64% | 69% | 68% |
| p09 | 64% | 63% | 62% |
| p10 | 63% | 61% | 61% |
| Kōga (p03) | 58% | 62% | 61% |
| p12 | 47% | 61% | 61% |
| p11 | 63% | 58% | 58% |
| Ryūga (p01) | 61% | 59% | 58% |
| p06 | 51% | 49% | 57% |
| p05 | 51% | 53% | 53% |
| Yin-lu (p02) | 45% | 50% | 49% |
| p13 | 55% | 47% | 47% |
| p14 | 31% | 32% | 31% |
| p07 | 32% | 26% | 27% |
| p16 | 24% | 21% | 21% |
| p15 | 13% | 10% | 10% |

faixa: G1b (29ca0716) 13–78 (desvio 16.8); H8c 10–77 (desvio 17.9); H3 10–76 (desvio 17.7)

- **p06:** 49 → 57% (+8, na borda do ruído de ±8). Volta para perto dos 51% que o usuário aceitou, e
  não aos 68% de antes da régua real.
- **O resto:** dentro do ruído.
- **Acumulado H8a–H8c contra o G1b, mais que o ruído:**
  - o p12 subiu 47 → 61% (a Investida da H8b);
  - o p13 caiu 55 → 47%.

**Duplas oficiais** (T21, H8c → H3):

| titular | oficial | vitórias | alternativo 1 | alternativo 2 | alternativo 3 | melhor parceiro |
|---|---|---|---|---|---|---|
| Ryūga (p01) | Yin-lu (p02) | 48% → 52% | p04 64% → p04 68% | p07 66% → p07 61% | p12 68% | p12 → p04 |
| Kōga (p03) | p04 | 63% → 61% | p06 46% → p06 41% | p09 68% | p14 41% → p14 45% | p09 |
| p05 | p06 | 45% → 50% | p08 61% | p11 55% | p16 34% | p08 |
| p07 | p08 | 55% | p10 48% → p10 45% | p13 52% | Yin-lu (p02) 27% → Yin-lu (p02) 25% | oficial |
| p09 | p10 | 71% → 73% | p12 86% | p15 45% | p04 70% → p04 73% | p12 |
| p11 | p12 | 71% → 70% | p14 50% → p14 45% | Ryūga (p01) 63% | p06 54% → p06 45% | oficial |
| p13 | p14 | 39% → 43% | p16 30% → p16 25% | Kōga (p03) 52% | p08 71% → p08 70% | p08 |
| p15 | p16 | 5% | Yin-lu (p02) 11% | p05 4% → p05 9% | p10 23% → p10 25% | p10 |

Todas ficaram dentro do ruído.

**Achado para decidir (não aplicado):** o mesmo descompasso existe em mais 16 golpes. O quadro do
impacto (`impactFrame`) passa antes da janela ativa, e o motor bate com o quadro seguinte:

- p02 jH; p04 L e 2M; p06 H; p07 L; p09 2M; p11 2M;
- p13 L, 2L e j2H; p14 L; p15 L, jM e jH; p16 2L e jL.

Para fechar, a regra do `validate_impacts` e do `fit_impact` precisa do mesmo critério do motor (o
meio do quadro de jogo), e os clipes precisam ser reajustados. Cada um desses golpes passa a bater
com o quadro desenhado como impacto. Em quase todos isso aumenta o alcance real (é a perna ou o braço
esticado), então é uma mudança ampla e fica para depois da matriz consolidada. O 2M do p09 está na
lista e mexe no experimento H2.


## Etapa H2 — p09: `alcanceMaximo` 190 no 2M (EXPERIMENTO A/B, item 2)

Decisão do usuário (item 2): testar o `alcanceMaximo` 190 no 2M do p09 como experimento A/B, sem
mexer na velocidade (300 → 270 fica de fora) e esperando a matriz para consolidar. **Este commit é
o lado B do experimento. Ele não está consolidado e se desfaz revertendo só ele.**

- **A** = estado da H3 (`alcanceMaximo` 220, etapa 4a).
- **B** = `alcanceMaximo` 190, com o `reach` declarado também em 190, porque o gerador exige
  reach ≤ alcanceMaximo.
- **Sem mudança:** dano (560), quadros (9/5/17), vida, velocidade e agarrões.
- **Arquivos:**
  - `characters/p09/character.json`: 2M com `reach` e `alcanceMaximo` 220 → 190;
  - `GeneratedCharacters.java`, gerado: `.reach(190…).reachCap(190)`;
  - `CpuReachTable` regenerada: p09 2M 266/260 → 236/230 (em pé/agachado, eixo a eixo contra o Ryūga).
- **Testes:**
  - `AgachadoDoRyugaTest.aLowMediumAtTheTipHitsTheCrouchingRyuga`: o caso do p09 passa de 280 para
    250 px, a ponta nova;
  - novo `theP09LowMediumStopsAtItsMaximumReach`: a 280 px o 2M não pega mais (com 220 pegava);
  - suíte: 569 de 569;
  - gerador: passa.

**Bateria fixa** (A → B, mesmas sementes): só o que mudou mais de 3%, mais o 1x1 e o 2x2 de p09, p02 e p10.

| métrica | antes | depois | diferença |
|---|---|---|---|
| 1x1.Yin-lu (p02) | 46% | 46% | -0 |
| 1x1.p09 | 65% | 64% | -1 |
| 1x1.p10 | 61% | 60% | -1 |
| 1x1.p09_vs_p02 | 70% | 75% | +5 |
| 2x2.Yin-lu (p02) | 50% | 51% | +1 |
| 2x2.p09 | 68% | 60% | -8 |
| 2x2.p10 | 60% | 58% | -3 |
| p09.vs_p02_400 | 76% | 62% | -13 |
| p09.parte_2M | 23% | 13% | -10 |

**Matriz 16 × 16** (A → B, força contra o elenco):

| personagem | A (220) | B (190) |
|---|---|---|
| p08 | 76% | 75% |
| p04 | 68% | 69% |
| Kōga (p03) | 61% | 61% |
| p10 | 61% | 61% |
| p12 | 61% | 60% |
| Ryūga (p01) | 58% | 59% |
| p11 | 58% | 59% |
| p09 | 62% | 57% |
| p06 | 57% | 55% |
| p05 | 53% | 53% |
| Yin-lu (p02) | 49% | 49% |
| p13 | 47% | 48% |
| p14 | 31% | 33% |
| p07 | 27% | 27% |
| p16 | 21% | 22% |
| p15 | 10% | 10% |

faixa: A (220) 10–76 (desvio 17.7); B (190) 10–75 (desvio 17.3)

**Duplas oficiais** (T21, A → B):

| titular | oficial | vitórias | alternativo 1 | alternativo 2 | alternativo 3 | melhor parceiro |
|---|---|---|---|---|---|---|
| Ryūga (p01) | Yin-lu (p02) | 52% → 55% | p04 68% → p04 63% | p07 61% → p07 63% | p12 68% | p04 → p12 |
| Kōga (p03) | p04 | 61% → 64% | p06 41% → p06 45% | p09 68% → p09 63% | p14 45% → p14 46% | p09 → oficial |
| p05 | p06 | 50% → 46% | p08 61% → p08 63% | p11 55% → p11 61% | p16 34% | p08 |
| p07 | p08 | 55% → 59% | p10 45% → p10 39% | p13 52% | Yin-lu (p02) 25% → Yin-lu (p02) 27% | oficial |
| p09 | p10 | 73% → 63% | p12 86% → p12 73% | p15 45% → p15 30% | p04 73% → p04 63% | p12 |
| p11 | p12 | 70% → 73% | p14 45% → p14 46% | Ryūga (p01) 63% → Ryūga (p01) 57% | p06 45% → p06 43% | oficial |
| p13 | p14 | 43% → 48% | p16 25% | Kōga (p03) 52% → Kōga (p03) 50% | p08 70% → p08 66% | p08 |
| p15 | p16 | 5% | Yin-lu (p02) 11% | p05 9% | p10 25% | p10 |

- **Leitura:**
  - O 2M passa de 23% para 13% do dano do p09.
  - O p09 contra a Yin-lu a 400 px cai de 76% para 62%.
  - Na matriz o p09 vai de 62% para 57% (−5, dentro do ruído de ±8), e sai do grupo de cima (4º → 8º).
  - No 2x2 cai 8 pontos, e a dupla p09 + p10 de 73% para 63%.
- **Os outros 15:** dentro do ruído.
- **Decisão do usuário:** consolidar B, voltar para A ou tentar um valor no meio (205). A matriz
  final de consolidação deve ser rodada com a lista H inteira junta.
- **Atenção:** o 2M do p09 também está entre os 16 golpes com o quadro do impacto antes da janela
  ativa (achado da H3). Se esse acerto for feito depois, o 2M passa a bater com o quadro esticado,
  e o `alcanceMaximo` vira o limite que de fato segura o alcance.

## Etapa H1 — Breaker com atordoamento 4 (item 1)

Decisão do usuário (item 1): testar o atordoamento de 4 quadros mantendo a distância atual
(170 px), com a CPU nova, separando o golpe inevitável (troco garantido) do acerto oportunista.
O relatório completo está em [h1-breaker.md](h1-breaker.md).

- **Arquivos:**
  - `CombatConfig.java`: `comboBreakerStunFrames` 20 → 4;
  - `CombatEngine.comboBreaker`: `a.buffer.clear()`. É obrigatório com 4 quadros: o H que o
    atacante tinha apertado para fechar o combo saía sozinho, no vazio, e quem quebrou punia de graça.
- **Testes:**
  - `ComboBreakerTest`: dois novos. Quem quebrou age 5 quadros antes, e nenhum normal com mais de
    5 quadros de início entra limpo nos 16. O segundo falha sem o `buffer.clear()`.
  - `BateriaQuebra`: separa garantido de oportunista.
  - Suíte: 571 de 571.

**Medida na frente H1** (base `5dedf956`, mesmas sementes):

| medida | antes | H1 |
|---|---|---|
| troco garantido no roteiro, meio / canto / de costas para a parede | 96 / 98 / 98% | 33 / 45 / 26% |
| primeiro acerto garantido, CPUs livres | 68% | 1% |
| primeiro acerto oportunista de quem quebrou | 20% | 60% |
| quem batia acerta primeiro | 11% | 33% |

**Ainda há troco garantido.** Sobra o L e o 2L na ponta, de quem tem L longo: p12 80%, p11 68%,
p06 54% e Ryūga 45%.

**Próximo experimento proposto** (medido, não aplicado): atordoamento 2 com 170 px.

- Troco garantido de 1–3%; com as CPUs livres, garantido 0% e oportunista 64%.
- Quem quebrou fica com saldo +269 no meio e +284 no canto.
- Separar mais (220–260 px) não resolve: sobra 18–26% de troco garantido e o canto fica negativo.

**Bateria fixa ao integrar** (depois da H2 → com a H1). Só o que passou de 3%:

| métrica | antes | depois |
|---|---|---|
| breaker.troco_garantido_script | 57% | 3% |
| breaker.troco | 513 | 649 |
| breaker.sempre_vs_nunca | 62% | 59% |
| breaker.cpu_quebra_das_chances | 35% | 32% |
| 1x1.p03 | 58% | 62% |
| 1x1.p09 | 64% | 60% |
| 1x1.p09_vs_p02 | 75% | 65% |
| 2x2.p02 | 51% | 41% |
| 2x2.p05 | 37% | 43% |
| 2x2.p07 | 34% | 41% |
| 2x2.p10 | 58% | 47% |
| 2x2.p11 | 50% | 54% |
| 2x2.p14 | 41% | 38% |
| 2x2.p15 | 25% | 22% |
| p09.vs_p02_400 | 62% | 58% |

16 personagens, 8 duplas e matriz dentro do ruído (±8) na medida da frente.

## Etapa H5a — DHC: o parceiro desliza até o lugar do Super (item 5)

Decisão do usuário (item 5): manter a entrada do parceiro até 420 px, só no DHC, respeitando
colisão e animação, com custo de 2 barras. O relatório está em
[h5-dhc-assists-energy.md](h5-dhc-assists-energy.md).

**A entrada da G2b tinha dois defeitos:**

- o parceiro aparecia de uma vez até 411 px à frente (117 de 360 pares);
- em 6 casos ele entrava dentro do corpo do rival.

**O que mudou:**

- `CombatEngine`, `CombatFighter` e `CombatConfig`: o parceiro desliza até o lugar do Super com a
  mesma curva da entrada do assist. Enquanto ele desliza, o Super dele e o rival esperam, e ele nunca
  para dentro do corpo do rival.
- Maior passo: 68 px, o primeiro quadro, igual ao do assist.
- Sobra 1 caso de corpo dentro: o Super do p14, que passa por dentro do rival de propósito.

**Testes:** `DhcEntradaTest` (novo); suíte 574 de 574.

**Bateria fixa** (depois da H1 → com a H5a): nenhuma métrica mudou mais de 3%. Na medida da frente,
o 1x1 saiu idêntico.

**Piso de 70% (H5b), medido separado e não aplicado:**

- o DHC sobe só de 2367 para 2408 de dano por barra (+1,7%);
- o DHC é fraco porque o Super do parceiro acerta pouco depois do Super do titular;
- recomendo não ligar.

**Achado, não aplicado:** em p05+p06, p13+p14 e p15+p16 o DHC sai só depois que o combo acabou,
porque o Super do titular erra as pancadas seguintes. Proposta: soltar o DHC quando o rival estiver
saindo do combo.

## H5 — assists (item 6) e SHINKAI ENERGY (item 7): só medição e testes

**Assists aprovados (p13 com H, p15 com 2H, p07 com a regra geral):** nenhum precisou de correção.

- **Teste novo:** `AssistSegurancaTest`. A sonda é a `SondaAssist`.
- **Defendidos:** deixam o rival preso de +21 a +44 quadros, e a CPU não pune o titular que chama o
  assist e depois defende.
- **No canto:** o golpe sai dentro da arena e acerta.
- **Rival pulando por cima do titular:** todos erram enquanto ele está no ar; p13 e p14 pegam no
  começo do pulo.
- **Mergulho do p14:** é golpe alto, então acerta quem defende agachado.
- **Alcance, antiaéreo e sinergia:** os da lista G, sem mudança.

**SHINKAI ENERGY, sem alteração** (`BateriaShinkaiBarra`, com a CPU atualizada):

| tipo | vence 1x1 | vence 2x2 |
|---|---|---|
| POWER | 74% | 71% |
| SPEED | 68% | 59% |
| TECHNIQUE | 63% | 56% |
| ENERGY | 69% | 55% |

- O ENERGY ganha 2,28 barras na janela, contra 0,7–0,9 dos outros tipos, e vira mais Ultra.
- No fim da luta sobra 0,6–0,7 barra a mais sem uso.
- Todos os tipos, e também o lado sem SHINKAI, terminam com 2,6–3,5 barras paradas: é a CPU que não
  gasta a barra, não o ENERGY.

## Etapa H4-p16 — M e H da p16 cancelam em especial e Super (item 4)

Decisão do usuário (item 4): ajustes individuais por ferramenta que não cumpre a função, sem aumento
global de dano. O relatório da frente, com a causa dos 2% de p15+p16, está em
[h4-personagens.md](h4-personagens.md).

- **Ferramenta:** a p16 era a única do elenco cujo M e H não cancelavam em especial nem em Super.
  Nenhum combo dela fechava.
- **Arquivos:**
  - `characters/p16/character.json`: `cancelInto` do M ganha S, S2, S3 e S4; o do H ganha S, SUPER,
    S2, S3 e S4;
  - `GeneratedCharacters.java`, regenerado pelo gerador.
- **Dano:** nada muda.
- **Testes:** `CancelamentosP16Test` (novo; `M H SUPER` rende 1260 → 2700); suíte 578 de 578.
- **Medida da frente:** matriz p16 24 → 27%; p15+p16 2 → 4%.
- **Bateria fixa ao integrar:**
  - 2x2 p16 37 → 40%;
  - `combo.max_1barra.p16` 5615 → 4487, efeito da busca em feixe: com mais ações a poda muda, e a
    rota antiga continua possível.

## Etapa H4-p15a — o repel da p15 fica livre logo depois de desfazer a bola (item 4)

- **Ferramenta:** o repel (S2) contra projétil prendia a p15 39 quadros depois de desfazer a bola,
  mais do que defender. Ela apanhava no golpe seguinte em 80% das vezes.
- **Arquivos:**
  - `CombatEngine`: o repel que desfaz um projétil corta a recuperação;
  - `CombatConfig`: a p15 fica livre 6 quadros depois.
- **Dano:** nada muda.
- **Testes:** `RepelProjetilTest` (novo); suíte 580 de 580.
- **Medida da frente:** apanhar no golpe seguinte cai de 80% para 5%; matriz p15 13 → 14%;
  p15+p16 2 → 5%.
- **Bateria fixa ao integrar:** só o Breaker mexeu, dentro de ±5: "sempre quebra" vence "nunca" 57 → 60%
  e a CPU vence "nunca" 46 → 50%.

## Etapa H4-p15b — Super da p15 defendido empurra 10 px, não 60 (item 4)

- **Ferramenta:** o Super defendido ficava em −83. O empurrão de 60 px tirava o rival do alcance do
  chute final, e as pancadas seguintes passavam no vazio.
- **Arquivos:**
  - `characters/p15/character.json`: `pushbackOnBlock` do Super 60 → 10;
  - `GeneratedCharacters.java`, regenerado.
- **Dano e quadros:** iguais.
- **Testes:** `SuperP15DefendidoTest` (novo); suíte 581 de 581.
- **Medida da frente:**
  - defendido −83 → −9, e as 5 pancadas pegam na defesa;
  - matriz igual;
  - p15+p16 2 → 4%, dentro do ruído.
- **Bateria fixa ao integrar:** nenhuma métrica mudou mais de 3%.

## Etapa H4-Ryūga — o Aparar vira aparar de verdade (item 4)

Decisão do usuário: o Aparar pode funcionar como aparar real, sem buff gratuito.

- **Mecânica nova (flag `parry`):** uma postura de contra-golpe que só bate no que leu.
  - Lê qualquer golpe corpo a corpo do 4º ao 13º quadro, anula e responde na hora com os mesmos
    900 de dano.
  - No vazio não bate e fica aberta o golpe inteiro (42 quadros).
  - Antes era um golpe comum de 20 quadros e 900 de dano: ganha a leitura e perde o golpe comum.
- **Arquivos:**
  - `AttackDefinition` (`parry`);
  - `CombatEngine` (o golpe de quem tem repel ou parry só sai se leu);
  - `characters/player_base/character.json` (S3 com `apara`);
  - `tools/sprites/build_characters.py` e `mecanicas.json` (aceitam `apara` e emitem `.parry()`);
  - `GeneratedCharacters` regenerado;
  - `CpuReachTable` regenerada (só o S3 do Ryūga mudou);
  - `TutorialDirector`: na lição do Aparar o boneco passa a atacar, senão não há o que aparar.
- **Testes:**
  - `AparadorTest` (novo);
  - `EspeciaisContraAgachadoTest` ajustado (o S3 já não acerta sozinho);
  - suíte 583 de 583;
  - gerador passa.
- **Medida da frente:** uso 0 → 0,4 por minuto, 60% acertam; matriz Ryūga 61 → 59%, dentro do ruído.
- **Bateria fixa ao integrar:** nenhuma métrica mudou mais de 3%.

**p14 e p07, sem mudança:**

- **p14:** pôr o pisão (S4) no neutro da CPU derrubou o p14 de 31% para 27%; ficou de fora. As
  ferramentas dele funcionam; falta uso pela CPU.
- **p07:** nenhuma ferramenta deixa de cumprir a função.

## Breaker: empurrão maior e dash automático de volta (pedido do jogador, 30/10)

**Pedido:** o combo breaker afastar o adversário um pouco mais e ligar de novo o dash automático
(quem quebra corre sozinho até ele).

**O que mudou:**

| Valor | Antes | Agora |
|---|---|---|
| `comboBreakerPushGap` (folga do empurrão) | 170 | 230 |
| `comboBreakerPushFrames` (o deslize no chão) | 10 | 12 (a mesma velocidade, mais longe) |
| `comboBreakerAirPushSpeed` (o empurrão no ar) | 260 | 320 |
| `comboBreakerChaseFrames` (o dash automático) | 0 (desligado desde a etapa 1) | 48 (era 40 antes da etapa 1; mais quadros pela distância maior) |

**O que fica:**
- O atordoamento de quem batia continua em 4 (etapa H1). Com ele, quem batia se recompõe antes
  de a corrida chegar: o dash põe quem quebrou ao alcance do L, mas sem combo de troco garantido,
  que era o problema da corrida com atordoamento de 44.
- Segurar trás desiste da corrida, como antes.

**Testes:** `ComboBreakerTest` passa a conferir o empurrão, a corrida até o alcance do L (dentro
dos 48 quadros) e que quem batia já se recompôs quando ela chega. Suíte do motor 588, com as 3
falhas de caminho de arquivo de antes.
