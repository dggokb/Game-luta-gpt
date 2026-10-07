# Motor de Dano e Combos V2 — v0.72

Implementação do documento "Jogo de Luta – Motor de Dano e Combos V2". A regra central
mudou de "botão apertado = novo ataque" para "botão apertado = input armazenado; o
próximo golpe só começa se existir uma transição válida naquele momento".

## Arquitetura

Todo o combate roda em Java puro (sem Android), em passo fixo de **60 frames por
segundo**, separado da renderização. O `GameView` virou uma casca: lê o toque, pede a
decisão da IA, dá um passo no motor e desenha.

| Módulo do documento | Classe | Papel |
| --- | --- | --- |
| CombatEngine | `CombatEngine` | Orquestra o frame na ordem fixa abaixo |
| InputBuffer | `InputBuffer` | Guarda presses por `bufferFrames`, prioridade, consumo |
| MotionParser | `MotionParser` | Histórico de direções: especiais, double tap, super pulo |
| CharacterStateMachine / AttackStateMachine | `CombatFighter` + `CombatEngine` | Estados, fases do golpe, físicas |
| CancelSystem | `CancelSystem` | Única decisão de iniciar golpe (neutro ou cancelamento) |
| HitDetection | `CombatRules.hitboxTouches/overlaps` | Hitbox × hurtbox, projéteis |
| HitReactionSystem / PushbackSystem | `CombatEngine.applyContact/applyPush` | Hitstun, blockstun, hitstop, launch, pushback |
| ComboTracker / ComboSession | `ComboSession` | Sessão real de combo |
| DamageScaling / HitstunDecay | `ComboSession.DamageScaling/HitstunDecay` | Escala e decay em inteiros |
| AttackDefinition | `AttackDefinition` | Definição completa do golpe, validada ao carregar |
| DebugOverlay | `DebugOverlay` | Botão DEBUG: caixas, estados, frame data |
| — | `FighterInput`, `PadInput`, `AiController` | Input por frame do jogador e da CPU |

### Ordem de update por frame (`CombatEngine.step`)

1. ler inputs; alimentar InputBuffer e MotionParser (e os pedidos de TAG do time);
2. encerrar estados vencidos e resolver inícios/cancelamentos (CancelSystem), depois
   troca e assist (`TeamSystem.resolve`);
3. avançar máquinas de estado, movimento, o time (troca, corpo e golpe do assist) e
   projéteis;
4. detectar colisões (simultâneas: trocas de golpe acontecem), inclusive do assist;
5. aplicar reações, hitstop e pushback;
6. atualizar as ComboSessions e juntar a barra do time.

Contadores, dano, medidor (milésimos de barra), escala e juggle são inteiros. Posições
são `float` (determinísticas na JVM). A mesma sequência de inputs a partir do mesmo
estado produz os mesmos frames (testado), base para rollback no futuro.

## Convenções de frame

- Índices contam a partir do primeiro frame do golpe (0). Um golpe ocupa exatamente
  `startup + active + recovery` frames; o próximo pode começar no frame seguinte.
- `startupFrames` são os frames **antes** da hitbox; o golpe acerta no frame
  `startup + 1`.
- Vantagem no hit (contato no primeiro frame ativo) =
  `hitstunFrames − (activeFrames − 1 + recoveryFrames)`. Como o startup do documento não
  inclui o frame que acerta, o link funciona quando **vantagem > startup do próximo**
  (equivalente ao "≥" da convenção em que o startup conta o frame ativo). O teste
  `linkFormulaMatchesTheSimulation` confere a fórmula contra a simulação na fronteira.
- **Hitstop** congela tudo do lutador (frame do golpe, hitstun/blockstun, expiração do
  buffer, relógio de comandos). Golpe corpo a corpo congela os dois; projétil, só o
  defensor. A janela de cancelamento on hit fica aberta durante o hitstop e o
  cancelamento acontece no primeiro frame após ele.
- **Super freeze**: o Super congela o oponente e os projéteis durante o próprio startup
  (o que permite H → SUPER combar).

## Ultra (↓ + SUPER) — v0.75

Estado próprio `ULTRA` no `CombatFighter`, com quatro fases. Os valores ficam em
`CombatConfig.ultra*`.

| Fase | Frames | O que acontece |
| --- | --- | --- |
| `ULTRA_STARTUP` | 25 | Custa 3 barras (`ULTRA_COST`). Congela o oponente e os projéteis, como o super freeze. |
| `ULTRA_RUSH` | até 18 | Avança a 2000 u/s. Conecta quando o corpo do alvo está a até `ultraReach` à frente. |
| `ULTRA_RECOVERY` | 30 | Errou ou foi defendido. É punível. |
| `ULTRA_CINEMATIC` | — | Acertou. O motor marca `ultraConnected()` e espera. |
| `ULTRA_BEAM` | 36 + 8 + 4 × (hits − 1) + 6 + 18 | Raio final depois da cinemática: carga, acertos pequenos (com recuo), explosão e arremesso. Não pode ser atingido. |

- O pedido vem de `FighterInput.ultra`, que o `PadInput` liga com ↓ + SUPER. Sem 3 barras,
  o aperto vira um SUPER comum. O pedido fica no buffer por `bufferFrames`, mas só começa
  em NEUTRAL e no chão.
- A defesa usa as regras normais (`guardStops`). Um ultra defendido dá blockstun de 24
  frames, hitstop e pushback, e não toca a cinemática.
- No acerto, a escala de dano é fixada pela `ComboSession` (`scaleFor`, piso de SUPER) e
  conta como um uso de `ULTRA`. Durante a cinemática, o `GameView` chama `applyUltraHit` a
  cada golpe (3 partes do dano do `ultra.json`) e `startUltraBeam` no fim.
- `startUltraBeam(atacante, dano, hits)` afasta os lutadores (`placeForUltraBeam`, que o
  `GameView` já chama enquanto a página quebra) e começa o raio. A cada
  `ultraBeamHitInterval` frames um acerto pequeno soma 1 hit na `ComboSession` e empurra o
  defensor; o defensor fica em hitstun e, se estava no ar, parado no ar (`heldByBeam`). A
  contagem é por acertos dados, então uma pausa de impacto nunca repete um acerto.
- A explosão final (e `finishUltra`, que encerra sem raio) arremessa o defensor
  (`ultraFall`): ele não se recupera no ar e cai em KNOCKDOWN, o que encerra a sessão de
  combo.
- A CPU não usa ultra: ela nunca liga `FighterInput.ultra`.

Testes: `ultraNeedsThreeBarsOtherwiseThePressIsASuper`,
`ultraConnectsUpCloseAndTheCinematicDealsScaledDamage`, `ultraAfterAComboIsScaled`,
`guardedUltraIsBlockedWithoutCinematic`, `ultraWhiffsFromFarAndRecovers`,
`finalBeamCountsEveryHitAndDealsItsWholeDamage`, `finalBeamHoldsAnAirborneDefenderAndThrowsItAtTheBlast`,
`finalBeamOnAKnockedOutDefenderStillEnds`, `finalBeamChargesBeforeTheFirstHit`.

## Defesa ativa: pushblock e Guard Cancel Tag — v0.81

Defender passa a ter decisões que gastam a barra do time. Valores em `CombatConfig.pushblock*`
e `CombatConfig.guardCancel*`.

| Comando | Quando | O que faz | Custo |
| --- | --- | --- | --- |
| **M + H** (dois dedos ou o ponto "M+H" entre os botões) | em blockstun (chão ou ar) | **Pushblock**: o atacante é empurrado 190 px e o blockstun cai para no máximo 8 frames. Fora do blockstun, M + H é o H. | 1/4 de barra |
| **TAG** | em blockstun, no chão, com o assist pronto | **Guard Cancel Tag**: o parceiro entra no lugar do ponto fazendo o golpe de assist dele, invulnerável até o fim dos frames ativos (+4); o ponto sai correndo e o oponente congela 12 frames (clarão). Depois: 300 frames sem assist nem troca. | 1 barra |

- O pedido feito durante o hitstop do bloqueio espera o hitstop acabar (não expira).
- Bloqueando, TAG nunca chama um assist comum: ou é Guard Cancel, ou nada.
- O botão de TAG mostra **CANCEL** quando o Guard Cancel está disponível, e o HUD mostra
  "M+H EMPURRA" no estado de bloqueio quando há barra para o pushblock.
- Invulnerabilidade: `CombatFighter.invulnFrames` (nada acerta enquanto > 0).

Testes em `ActiveDefenseTest`.

## Agarrão e tech — v0.80

A terceira ameaça contra quem só defende: o agarrão não pode ser bloqueado. Estado próprio
`THROW` no atacante e `THROWN` no defensor; valores em `CombatConfig.throw*`.

| Fase | Frames | O que acontece |
| --- | --- | --- |
| `THROW_STARTUP` | 5 + 3 ativos | Pega se o corpo do outro estiver colado à frente (vão de até `throwRange` = 28) e no chão. |
| `THROW_HOLD` | 12 | O defensor é puxado e fica `THROWN`; é a janela de **tech**. Ninguém acerta os dois. |
| `THROW_EXECUTE` | 16 | Sem tech: 1200 de dano (começo de combo, sem escala), knockdown e empurrão. O atacante se recupera antes de o defensor levantar (oki). |
| `THROW_WHIFF` | 24 | Ninguém ao alcance: recuperação punível (pode ser atingido e agarrado). |
| `THROW_TECH` | 16 (os dois) | L + M do defensor na janela (ou até `bufferFrames` antes): sem dano, os dois são empurrados 110 px e ficam sem agir. |

- **Comando**: L + M (dois dedos) ou o **ponto entre L e M** na tela
  (`ControlsLayout.THROW_*`, desenhado como "L+M"). Dois dedos quase nunca caem no mesmo
  frame: até `PadInput.THROW_LENIENCY_FRAMES` (2) de diferença ainda é agarrão, e o jab que
  o primeiro botão começou vira o agarrão (ainda nos primeiros frames, antes de acertar).
- **Quem pode ser agarrado**: no chão, em NEUTRAL, num golpe próprio (não no Super nem no
  ultra) ou num agarrão errado. Hitstun, blockstun e os `throwProtectFrames` (6) depois
  deles ou de levantar são invulneráveis a agarrão, então agarrão não continua combo e
  um agarrão começado durante o stun do outro erra.
- **Os dois ao mesmo tempo**: tech automático.
- **CPU**: agarra às vezes quando está colada (`OpponentAi.THROW_CHANCE`) e faz tech em
  ~40% dos agarrões, alguns frames depois de ser pega (`TECH_CHANCE`).
- O jogo mostra "AGARRÃO!" ou "TECH!" no centro da tela. Sem arte própria ainda: o
  atacante usa o jab para pegar e o soco forte para jogar; o defensor, a reação de hit.

Testes em `ThrowTest`.

## Time: troca, assist e barra do time — v0.79

A regra do time saiu do `GameView` e foi para o motor (`TeamSystem`, dono e passo do
`CombatEngine`), contando frames como o resto da luta. O `GameView` só desenha.

| Comando | O que faz |
| --- | --- |
| **TAG** | Chama o parceiro como **assist**: ele entra correndo atrás do ponto (`assistBehind` 70 px, 8 frames), faz o golpe que o pack declara (`fighter.assist.move`) e sai (10 frames). Recarga de 240 frames (4 s). |
| **TAG de novo** enquanto o assist entra ou golpeia | **Assist → Tag**: quando o golpe do assist termina, ele fica como o novo ponto onde está e o antigo sai correndo. O combo continua com o novo ponto. Recusado (o assist só sai) se o ponto estiver apanhando, defendendo, caído ou no ultra. Depois: 300 frames sem assist nem troca. |
| **↓ + TAG** | **Troca direta**, igual à de antes: o ponto sai correndo (20 frames), o parceiro entra (23) e posa (27), sem poder agir. Só em NEUTRAL. Recarga de 600 frames (10 s). |

- Os botões são lidos sem precisar segurar: tocar e segurar se confundiriam e atrasariam o
  assist. O pedido fica no buffer por `bufferFrames`, então dá para chamar o assist
  durante o hitstop de um golpe.
- O assist pode ser chamado do neutro ou durante os golpes do ponto (não durante o Super,
  o ultra, a troca, nem apanhando). Nesta versão ele não pode ser atingido.
- O golpe do assist é um `CombatFighter` próprio do lado (mesmo índice do ponto): usa as
  hitboxes, o frame data, o hitstop e a defesa normais; os acertos entram na
  `ComboSession` do defensor com a escala de sempre. Assist de projétil (`"S"`) lança o
  projétil do parceiro em nome do lado.
- **Barra do time**: há uma barra só. O que o parceiro fora de campo ganha (os acertos do
  assist) vai para o ponto a cada frame, e na troca a barra vai com quem entra. O HUD e
  todos os custos leem a barra do ponto.
- Um lado com um membro só (o boneco de treino) nunca troca nem chama assist.

Valores em `CombatConfig.tag*` e `CombatConfig.assist*`. Testes em `TeamSystemTest`.

## Frame data no Character Pack (schema 3)

Cada golpe em `moves` e o bloco `attack` de `fighter.energy` (S) e `fighter.super`
(SUPER) declaram:

| Campo | Obrigatório | Significado |
| --- | --- | --- |
| `startupFrames`, `activeFrames`, `recoveryFrames` | sim | Fases do golpe |
| `hitstunFrames`, `blockstunFrames`, `hitstopFrames` | sim | Reação e congelamento |
| `cancelWindows` `{hit, block, whiff}` | sim | Janela `[início, fim]` inclusiva por condição, ou `null` |
| `cancelInto` | sim | Golpes alvo (`L`…`jH`, `S`, `SUPER`, `JUMP`) |
| `pushbackOnHit`, `pushbackOnBlock` | sim | Distância empurrada (o restante vai para o atacante no canto) |
| `damageProration` | não (1.0) | Escala aplicada ao resto de um combo que este golpe inicia |
| `launchType` | não (`none`) | `knockdown`, `launch`, `slam` (slam exige super pulo e alvo no ar) |
| `knockback` | não (= pushbackOnHit) | Deslocamento quando o alvo fica no ar ou cai |
| `juggleCost` | não (1) | Pontos de juggle gastos em alvo no ar |
| `maxHits` | não (1) | Acertos por execução no mesmo alvo (≤ número de hitboxes) |
| `maxUsesPerCombo` | não (0 = livre) | Acima disso o CancelSystem recusa a rota |
| `low` | não (false) | Precisa de defesa agachada |
| `hitboxes`, `hurtboxes` | não | `{"frames":[a,b],"x":[x0,x1],"y":[y0,y1]}` por frame, relativos à raiz |

Validação dupla: `build_characters.py` reprova o build e `AttackDefinition.Builder` /
`CharacterDefinition` reprovam o carregamento quando um `cancelInto` aponta para golpe
inexistente, cruza chão/ar, uma janela sai da duração do golpe (ou on hit/block abre
antes do primeiro frame ativo), ou o `autoCombo` não é uma rota declarada.
`fighter.inputPriority` (opcional) muda a ordem SUPER > SPECIAL > HEAVY > MEDIUM > LIGHT.

### Rotas base (iguais nos três packs)

| Golpe | s/a/r | Hit / Block | Cancela em |
| --- | --- | --- | --- |
| L | 2/4/4 | +5 / +2 | L (máx. 3 por combo), M, 2L |
| 2L (baixo) | 2/4/4 | +5 / +2 | M, 2M |
| M | 4/6/6 | +4 / +1 | H, 2M, S |
| 2M (baixo, derruba) | 6/4/6 | knockdown / +1 | H |
| H | 10/5/9 | +5 / −2 | S, SUPER |
| 2H (lançador) | 9/6/9 | launch / −4 | JUMP (super pulo de perseguição) |
| jL / jM / jH | 4/2/4, 6/4/6, 9/6/9 | — | jM, jH / jH / — (jH faz slam) |
| S | 6/1/11 | proj. | SUPER |
| SUPER | 35/1/25 | proj. | — |

Valores do `player_base`; `player_two` e Brutamonte diferem só onde a duração original
diferia. O overlay de debug mostra a tabela do personagem ativo calculada dos dados.

### Valores do sistema (`CombatConfig`)

Buffer 4 frames; comando especial em até 15 frames e botão até 15 frames depois;
double tap 18; super pulo (↓ → ↑) 22. Escala: −5% por hit até 20% (SPECIAL 30%,
SUPER 50%), −20% por repetição do mesmo golpe, dano arredondado para baixo e mínimo 1.
Hitstun decay por número de hits (0,0,0,1,1,2,2,3,3,4,4,5 frames) ou por segundos de
combo (0,0,1,2,4,6), o maior dos dois. Juggle limit 6. Pushback distribuído em 8
frames.

## Critérios de aceite → testes (`CombatEngineTest`, JVM, sem renderização)

| Critério | Teste |
| --- | --- |
| Apertar repetidamente não interrompe o golpe | `mashingDoesNotInterruptTheCurrentAttack` |
| Novo golpe só em NEUTRAL ou cancelamento válido | `attackStartsOnlyFromNeutralOrAValidCancel` |
| Inputs antecipados pelo buffer | `earlyPressIsBufferedUntilTheFighterIsFree`, `pressDuringHitstopDoesNotExpireAndCancelsRightAfter` |
| Rota inválida ignorada sem quebrar a animação | `invalidRouteIsIgnoredAndExpiresWithoutBreakingTheAttack` |
| Defensor não age em HITSTUN/BLOCKSTUN | `defenderCannotActDuringHitstunOrBlockstun` |
| Combo termina quando o defensor recupera | `comboEndsWhenTheDefenderRecoversAndCountsRealSessions`, `knockdownIsInvulnerableAndEndsTheCombo` |
| Contador reflete ComboSession real | idem + `linkFormulaMatchesTheSimulation` |
| DamageScaling e HitstunDecay | `damageScalingStepsDownToFloorsWithIntegerRounding`, `hitstunDecaysAsTheComboGrows`, `longComboInTheEngineIsScaled` |
| maxHits | `overlappingHitboxHitsOnceAndMultiHitRespectsMaxHits` |
| Nenhuma rota infinita | `everyRouteEndsByDecayJugglePointsOrPushback`, `lightLinkLoopEndsMidscreenAndInTheCorner`, `cornerPushbackMovesTheAttackerInstead`, `juggleLimitMakesTheDefenderRecoverInTheAir` |
| Prioridade fixa no buffer | `simultaneousPressesFollowTheSamePriority`, `equalPriorityPicksTheMostRecentPress` |
| Determinismo frame a frame | `sameInputsGiveTheSameFrames` |
| Hitstop nos impactos | `hitstopFreezesBothFightersOnMeleeImpact` |
| Data-driven | `loadRejectsBrokenRoutesAndWindows`, `newMoveWorksFromDataAlone`, testes Python do pipeline |

## Mudanças de comportamento em relação à v0.71

- Apertar L/M/H durante um golpe não reinicia mais o golpe; o input espera a janela.
- O botão COMBO segue a rota `autoCombo` declarada pelo CancelSystem. O `player_two`
  passou de L, L, H, M para L, L, M, H, porque L → H e H → M não são rotas válidas.
- Impactos têm hitstop; hitstun é real (não dá para defender no meio de um combo).
- A CPU usa o mesmo motor: aperta botões, respeita buffer, estados e hitstun.
- Lutador derrubado é invulnerável; o slam do jH termina em queda; aterrissar encerra
  golpes aéreos; o Super congela o oponente durante o startup.
- A troca de personagem só começa em NEUTRAL. (v0.79) Ela agora é `↓ + TAG`; o TAG
  sozinho chama o assist.
- (v0.74) Andar para trás é mais lento que para frente: 220 contra 300 unidades/s,
  também no ar (`CombatConfig.walkBackSpeed`). Recuar andando não foge de quem avança;
  o backdash continua rápido, mas é curto e comprometido.
- O comando de especial ficou mais estrito (antes ~420 ms por passo e 550 ms para
  confirmar; agora 15 + 15 frames). Ajustável em `CombatConfig`.

## Ainda não feito

- Fase 9 (calibração) só tem a primeira passada: valores escolhidos para os links
  L → L/M e H → L/M funcionarem e para a rota 2H → super pulo → jL → jM → jH caber nos
  6 pontos de juggle. Falta jogar e ajustar no aparelho.
- Fase 10: wall bounce e ground bounce não existem.
- Os packs usam a hitbox padrão; nenhuma hitbox/hurtbox por frame foi desenhada ainda,
  embora o formato e o motor suportem.
