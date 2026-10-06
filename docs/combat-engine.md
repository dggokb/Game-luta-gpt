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

1. ler inputs; alimentar InputBuffer e MotionParser;
2. encerrar estados vencidos e resolver inícios/cancelamentos (CancelSystem);
3. avançar máquinas de estado, movimento e projéteis;
4. detectar colisões (simultâneas: trocas de golpe acontecem);
5. aplicar reações, hitstop e pushback;
6. atualizar as ComboSessions.

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
- A troca de personagem só começa em NEUTRAL.
- O comando de especial ficou mais estrito (antes ~420 ms por passo e 550 ms para
  confirmar; agora 15 + 15 frames). Ajustável em `CombatConfig`.

## Ainda não feito

- Fase 9 (calibração) só tem a primeira passada: valores escolhidos para os links
  L → L/M e H → L/M funcionarem e para a rota 2H → super pulo → jL → jM → jH caber nos
  6 pontos de juggle. Falta jogar e ajustar no aparelho.
- Fase 10: wall bounce e ground bounce não existem.
- Os packs usam a hitbox padrão; nenhuma hitbox/hurtbox por frame foi desenhada ainda,
  embora o formato e o motor suportem.
