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
   projéteis (com o choque entre projéteis, antes de eles acertarem alguém);
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
- **Cinemática do Super** (`SuperFlash`, só apresentação): segue quem ativou, o jogador, a
  CPU ou o parceiro de um DHC. Na ativação vem o aviso: o corpo brilha na cor do lutador por
  8 quadros, um brilho de estrela no peito (`ImpactEffects.spawnSuperGlint`) e o som `super`.
  Depois o cenário escurece atrás dos dois, a câmera aproxima **em quem ativou** (o peito dele
  vai de onde estava na tela até perto do meio conforme o zoom cresce, e volta junto), a aura
  de carga cresce e a liberação pisca branco. Quem está congelado escurece e esfria a cor (não parece jogo
  travado) e volta aos poucos. Na barra, o gasto de uma barra inteira pisca por 0,75 s
  (`HudEffects.SUPER_FLASH`).

## CPU: níveis, percepção, defesa e caderno de combos

A CPU aperta botões como um jogador (`AiController` enche um `FighterInput`; o motor não sabe
quem é CPU). A regra principal: **nada do que ela faz em resposta ao rival é mais rápido que um
humano**.

- **Percepção com atraso** (`CpuPerception`): guarda os últimos 64 quadros do que o rival
  *mostra* (posição, velocidade, animação, golpe e quadro dele, pulo, projéteis vindo) e entrega
  o de `CpuLevel.reactionFrames` quadros atrás. Nunca o botão apertado nem o quadro atual. No
  começo da luta, antes de ter a reação inteira de história, ela não sabe nada (`seen` = null).
- **Piso humano**: `CpuLevel.HUMAN_REACTION_FRAMES` = 13 quadros (~217 ms a 60 fps). Nenhum
  nível fica abaixo (o construtor recusa) e os testes medem: a CPU nunca reage a um pulo nem
  pune um golpe errado antes disso.
- **8 níveis** (`CpuLevel`): reação (30 → 13 quadros), hábito de defender colada, chance de
  defender o que viu e de acertar a altura, tamanho do combo, chance de confirmar, de errar o
  combo, de punir, de tirar do ar, de pular projétil, de tech de agarrão e de air tech, ritmo
  das decisões e escala do kit da batalha. O dano é o mesmo em todos.
- **Estilo** (`CpuStyle`, do `fighter.comboStyle` do pacote): distância preferida, agressão,
  vontade de soltar projétil, de agarrar, de pular e paciência na defesa. O zoner (alcance)
  recua para a distância dele; o pressão fica colado; o tanque agarra; o imprevisível às vezes
  larga o plano.

Ordem de cada quadro (`AiController.fill`):

1. agarrada: tech **por palpite** (a janela do tech, 12 quadros, é menor que a reação): só se
   ela já esperava o agarrão (o rival estava colado no que ela viu antes dele agarrar);
2. no ar apanhando / caída: air tech e opções de levantar, com a chance do nível;
3. kit da batalha (assist, DHC, Overdrive, troca, Ultra, pushblock, Guard Cancel); o assist e o
   Overdrive no meio do combo só depois de *ver* que acertou;
4. o próprio golpe: o **caderno de combos** segue a rota;
5. **defesa por reação**: golpe vindo (agachada contra golpe baixo; em pé contra pulo), pulo
   vindo (em pé) ou projétil (defende ou pula por cima, para frente). Segura a defesa enquanto
   está em blockstun;
6. **cartas**: punir (viu o rival errar, calcula a recuperação que sobra e entra com o golpe de
   abertura mais rápido que chega) e anti-aéreo (viu o pulo vindo, acompanha a trajetória e sobe
   com o 2H, que lança);
7. **defesa por hábito**: colada no rival, espera defendendo agachada (agachada segura qualquer
   golpe no chão). Forte quando ela acabou de ver o rival atacar (respeita a pressão), fraca
   contra quem está parado. Punir e tirar do ar passam na frente dela;
8. **neutro** (`OpponentAi.think`, com a posição *vista* do rival): andar, recuar até a
   distância do estilo, dash, projétil, agarrão, golpe ao alcance, pulo.

**Caderno de combos** (`ComboBook`): a rota principal de cada lutador sai do próprio pacote
(`cancelInto`, `maxUsesPerChain`) e do estilo, sem combo escrito à mão. Ex.: p01
`L M 2H super-pulo jL jM pulo-no-ar jL jM j2H`; p04 `L M M 2H ...`; p09 `L M H H SUPER`;
p12 `L M H L M 2H ...`. Golpes comuns seguem às cegas (no acerto ou na defesa, como um jogador
encadeia); lançador, especial, Super e super pulo só depois de ver que acertou. Se o empurrão
tirou o próximo golpe do alcance, ela fecha com o que ainda alcança (H, ou o S1). Golpe agachado
sai com ↘ (não fecha comando de especial com a defesa ← ↙ que veio antes).

Nível na pausa: **NÍVEL CPU ◀ 5 ▶** (1 a 8, salvo no aparelho; padrão 5). Testes: `CpuTest`
(piso humano, percepção, defesa por nível, punição, combo por nível, rotas por estilo, zoner
mais longe que pressão, nível 8 vence o nível 2).

## Correntes de combo

As rotas de cada golpe vêm do estilo de combo do personagem (bloco `combos` de
`tools/sprites/mecanicas.json`, tabela em `docs/personagens/mecanicas.md`, seção 4b). O motor
só lê o `cancelInto` e os limites que ficam no `character.json`.

- **Limite por corrente** (`maxUsesPerChain`, em `CancelSystem.chainAllows`): o lutador guarda
  os golpes da corrente atual (`CombatFighter.chain`). Um golpe que sai do neutro começa outra
  corrente; um cancelamento continua a mesma. Vale também na defesa, onde não há combo para
  contar: é o que faz uma corrente que volta para trás (H > L do p12) ou repete (M > M) sempre
  acabar. `maxUsesPerCombo` continua contando no combo inteiro.
- **Cancelamento de pulo no ar**: `JUMP` no `cancelInto` de um golpe aéreo. Na janela do golpe,
  apertar para cima dá um impulso para cima (`CombatConfig.airJumpSpeed`), uma vez por voo
  (`airJumpUsed`, volta ao tocar o chão), e a corrente aérea recomeça. Cue `AIR_JUMP`.

Testes: `CorrentesComboTest`.

## Anulação de projéteis

Projéteis de lados opostos que se tocam se anulam antes de chegar em alguém
(`CombatEngine.clashProjectiles`, no passo 3, logo depois de os projéteis andarem).

- **Força** de cada projétil = acertos que ainda faltam (`Projectile.durability`). No choque
  cada um perde a força do outro e some quando ela chega a zero: onda contra onda somem as
  duas; a Onda Máxima (5 acertos) contra a Onda Máxima também.
- **Super contra especial**: o Super não perde nada, a onda do rival some e o Super segue
  inteiro até o rival. O especial nunca para um Super.
- **Quem entra**: ondas de energia, especiais lançados (kunai, shuriken, bisturi, seringa,
  machado) e Supers em projétil. **Não entram**: o chicote da p08 (é o braço, não energia),
  projétil voltando ou parado no rival entre dois acertos, capangas e ecos de sombra.
- **Altura conta**: só se tocam se as alturas se cruzam (soma dos raios). A sombra baixa do
  p06 passa por baixo da onda na altura do peito.
- **Teste contínuo** (`CombatRules.projectilesMeet`): a distância entre os centros muda em
  linha reta dentro do quadro, então um par rápido que se cruzou dentro de um único quadro
  também se choca.
- Depois do choque o lançador pode soltar outra onda na hora (a regra de uma por vez libera).
- Nada de barra, hitstop ou dano no choque. O motor avisa com o cue `PROJECTILE_CLASH` e a
  lista `clashes()` (ponto, Super ou não, cor e sentido de cada lado); o `GameView` solta uma
  faísca de cada lado na cor de quem lançou, treme a tela de leve e toca o som de acerto.
- Base para o Beam Clash do roadmap: hoje o choque é instantâneo; a disputa jogável entre
  dois Supers pode nascer daqui.

Testes: `AnulacaoProjeteisTest`.

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

## Modo batalha 2x2 e READY → GO

O fluxo da luta fica em `BattleFlow` (Java puro, em frames), chamado depois de cada passo do motor.

- **READY → GO** (nomes provisórios): entre a intro e o idle os dois ficam parados no READY
  (`READY_FRAMES`); no GO (`GO_FRAMES`) o controle já volta. Vale para toda luta, no treino também.
- **IA OFF = treino** (o de antes): o CPU luta só com o titular, o KO não encerra nada e os
  botões VIDA P1 / VIDA CPU recarregam.
- **IA ON = batalha de verdade**: ligar a IA começa uma luta nova do zero. O CPU luta com a
  dupla dele (titular + parceiro escolhidos na seleção).
  - O titular que cai fica no chão `koReplaceFrames` (75) depois de bater no chão e o
    parceiro com vida entra correndo por trás, a mesma entrada da troca, invulnerável até
    terminar a pose (`CombatEngine.replaceFallenPoint`, `TeamSystem.enterAfterKo`). A barra do
    time passa para quem entra.
  - **O time só perde quando os dois da dupla caem** (`TeamSystem.defeated`). Os dois juntos: empate.
  - Parceiro caído não vem mais de assist, troca, Guard Cancel nem DHC.
  - Fim: K.O. (`KO_FRAMES`), o resultado (VITÓRIA / DERROTA / EMPATE, `RESULT_FRAMES`) e a
    luta recomeça com as mesmas duplas. Só o vencedor final comemora.
- **Tempo: 99 segundos** (`TIME_LIMIT_SECONDS`, 60 quadros por segundo) só na batalha; no treino
  a HUD mostra ∞.
  - O relógio começa a contar no GO e **para** no congelamento do Super e do Ultra (cinemática,
    Página Final, raio), na pausa e enquanto uma cena da história tem o controle. Hitstop
    normal não para o relógio.
  - Nos últimos 10 segundos (`URGENT_SECONDS`) o número fica vermelho e pulsa.
  - **Zerou sem K.O.: TIME OVER** no lugar do K.O. e vence a dupla com **mais vida, em
    proporção da vida total da dupla** (soma da vida dos dois sobre a soma da vida máxima;
    o vermelho recuperável não conta; `TeamSystem.moreLife`, em inteiros). Vida igual: EMPATE.
    K.O. no mesmo quadro do zero vale como K.O.
  - Depois do TIME OVER ninguém se mexe nem cai mais (os comandos são ignorados e a vida tem
    piso 1); o vencedor comemora com o rival de pé.
  - História: enquanto uma cena espera a vida baixar ou o K.O. (ou está rodando), o relógio
    fica parado e a HUD mostra ∞ (`BattleFlow.setClockHeld`) — a cena decide o fim, não o
    tempo. Na história e no Arcade o EMPATE conta como derrota (no Arcade, e nas lutas com
    `repetirSeDerrota`, a luta se repete).
- As lutas da história são batalhas (o treino do tutorial não): a página volta quando uma
  dupla inteira cai.
- **O CPU usa o kit inteiro na batalha** (`AiController.setFullKit`), com sorteios próprios
  (não mexem nas decisões do `OpponentAi`, nem no treino):
  - **assist** no neutro a meia distância e para estender o combo quando um golpe acerta;
  - **Assist → Tag** com o assist em campo; **troca** (↓ + TAG) com a vida baixa e o parceiro melhor;
  - **Super** cancelando um golpe que acertou (além do Super à distância de antes) e **DHC**
    quando o próprio Super já saiu;
  - **Guard Cancel Tag** e **pushblock** quando está defendendo;
  - **Overdrive** no meio do combo ou com a vida baixa; **Ultra** com 3 barras e perto.
  - Chances e distâncias: constantes no topo do `AiController`.
- O parceiro do CPU em campo (assist, ou saindo depois do Assist → Tag) é desenhado com as
  cores normais, como o titular; a arte dele já vem carregada na batalha. O Ultra do CPU usa o pack do
  personagem dele (ou o provisório) e o toque do jogador não dá bônus nele.

Testes: `BattleFlowTest`, `AiFullKitTest`.

## Overdrive — v0.87

- Entrada: `FighterInput.overdrive` (botão OD em `ControlsLayout.OVERDRIVE_*`, ou SUPER + TAG
  no mesmo quadro no `PadInput`, que então não manda nem o Super nem o assist). Fica no
  buffer como os outros pedidos (`overdriveRequestAge`).
- Estado por lado em `TeamSystem.Side` (`overdriveFrames`, `overdriveUsed`): **1 vez por
  round**, `CombatConfig.overdriveFrames` = 480 (8 s). `refreshOverdrive(side)` começa um
  round novo (os botões VIDA do treino chamam).
- Liga em `NEUTRAL` ou **cancelando o próprio golpe** (não Super, não em stun, não no tag).
  Os dois congelam `overdriveFlashFrames` (20) e o super freeze segura projéteis: o stun do
  oponente espera, então o combo continua. Cue `OVERDRIVE`. O relógio não anda em freeze.
- Efeitos enquanto dura (para o time):
  - velocidade × `overdriveSpeed` (1.25): andar, dash, backdash, air dash;
  - barra × `overdriveMeterPermille` (1500) no acerto e na defesa;
  - **cancels livres**: golpe que acertou ou foi defendido cancela em qualquer outro golpe
    diferente dele (normais, especiais, Super), respeitando os limites do combo
    (`ComboSession.allows`, juggle). Cue `OD_CANCEL` quando só o Overdrive permitia;
  - vida recuperável volta para **todos** os membros, inclusive o point, a
    `overdriveRegenPerFrame` (12/quadro).
- Tela: botão OD com anel do tempo restante, banner "OVERDRIVE!", aura vermelha
  (`EffectsRenderer.drawOverdrive`). Testes: `OverdriveTest` e `DemoDirectorTest` (demo 87).

## Demos das versões e CPU de treino — v0.86

- `DemoDirector` (Java puro): roteiros por versão (`VERSIONS` 80…85 e 87). Enquanto uma demo
  roda ele escreve a entrada **dos dois lados** a cada quadro; o pad é drenado e
  descartado e o cérebro do CPU não é chamado. Cada passo: `CombatEngine.restage` (os dois
  no chão, nada em andamento: golpe, stun, projétil, combo, tag/assist), `LEAD_IN` de 36
  quadros parados sob a legenda, depois o roteiro. Um `Check` por passo marca ✓ no HUD.
- No fim (ou tocando de novo no botão), vida, vida vermelha, barra e o point voltam como
  estavam e os dois voltam às posições iniciais. Não inicia durante um ultra.
- `DemoDirectorTest` roda todas as demos no motor real e exige que cada passo mostre o que
  promete (cue/evento), e que a luta volte igual.
- CPU de treino: `GameView` usa `TEAM[0]` (player_base) para o CPU, com as cores normais da
  arte (o filtro pálido `washedOut()` foi removido: o adversário não fica mais esbranquiçado).
  `GeneratedCharacters.OPPONENT` (monster_npc) continua no pack para os testes.
- Correção achada pela demo: o voo do **wall bounce** agora calcula a altura para chegar à
  parede antes de pousar (com o defensor colado ele caía antes dela e não quicava).

## DHC (Team Super) e vida recuperável — v0.85

**DHC**: **TAG durante o seu Super, depois que ele disparou** (do frame de release até o
fim do golpe) traz o parceiro com o Super dele.

- Custa mais 1 barra (da barra do time) e precisa do parceiro pronto (assist livre, sem
  troca nem assist em campo) e com Super no pack.
- O parceiro assume o ponto onde o ponto está, começa o Super com o congelamento normal do
  Super, e o antigo ponto sai correndo. O combo continua com a escala de sempre (piso de
  Super).
- Depois: 300 frames sem assist nem troca. O botão TAG mostra **DHC** quando dá, e a tela
  mostra "TEAM SUPER!". O ultra não entra em DHC.

**Vida recuperável**: 27% de todo dano (`recoverableLifePermille`) vira **vida vermelha**
na barra, logo depois da vida. Ela volta aos poucos (6 por frame, ~360 por segundo) só
enquanto o lutador espera **fora de campo** (não no ponto, nem como assist, nem saindo). KO
zera a vermelha; nunca passa da vida máxima. É o motivo para trocar um personagem
machucado. O boneco de treino (sem time) nunca recupera. Os botões de vida de teste zeram
a vermelha.

Todo dano passa por `FighterState.takeDamage` (golpes, agarrão, ultra). Testes em
`TeamSuperTest`.

## Air tech e opções de levantar — v0.84

A recuperação deixou de ser automática: quem apanha escolhe. Valores em
`CombatConfig.airTech*`, `quickRiseDownFrames`, `roll*` e `delayWakeupFrames`.

**No ar** (quando o hitstun aéreo acaba):

| Entrada | Resultado |
| --- | --- |
| L/M/H + ← | **Air tech para trás**: controle na hora, 10 frames invulnerável, 160 px para longe |
| L/M/H + → | Air tech para a frente (120 px) |
| L/M/H sem direção | Air tech neutro: um pulinho para cima |
| nada | **Cai solto** por até 12 frames (`airTechWindow`), ainda aberto a golpes (conta no combo, com o limite de juggle), e recupera sozinho, sem invulnerabilidade |

O botão do tech não sai como golpe aéreo. Quedas forçadas (slam), bounces e o ultra não
têm air tech (caem derrubados).

**No chão** (direção segurada enquanto está deitado, relativa a quem olha):

| Direção | Resultado |
| --- | --- |
| ↑ | **Levantar rápido**: fica só 6 frames no chão (em vez de 30) |
| ← | **Rolar para trás**: 24 frames invulnerável, ~220 px para longe |
| → | **Rolar para a frente**: passa por baixo do oponente (atravessa o corpo) e levanta já virado para ele |
| ↓ | **Levantar atrasado**: +30 frames no chão (quebra o tempo do oki) |
| nada | levanta no tempo normal |

- Knockdown do ultra (`hardKnockdown`) não tem opções.
- A proteção contra agarrão ao levantar (v0.80) vale para todas as opções.
- O HUD mostra as opções quando você está no chão e "L/M/H = AIR TECH" quando está caindo.
- **CPU**: faz air tech em ~70% das vezes (neutro, para trás ou para a frente) e escolhe
  levantar rápido, rolar, atrasar ou normal ao acaso.
- Sem arte própria ainda: o rolamento usa o clipe de levantar.
- Quem levanta sempre fica de frente para o oponente (vira quando o rolamento passa por
  baixo dele), em vez de só virar quando volta ao neutro.
- O levantar dura no máximo `maxWakeupFrames` (30): um clipe GETUP mais longo toca mais
  rápido, inteiro, no tempo do motor. Queda + chão + levantar normal ≈ 1,2 s.

**Punish counter (giro no ar, como no SF6):** o H em pé que acerta o rival **no meio do golpe
dele** (início, ativo ou recuperação) o joga girando no ar a média distância (~360 px, ~1 s
de voo) e ele cai deitado, sem air tech. Ainda dá para emendar golpes nele no ar (conta no
limite de juggle). A tela treme e aparece "PUNISH COUNTER!". O 2H (lançador) não conta, nem
golpes leves/médios, especiais ou projéteis. Valores em `CombatConfig.spin*`; o giro é só
visual (o sprite gira em torno do próprio eixo, inclinado, em `GameView.spinTransform`).
Testes em `PunishCounterTest`.

**KO e derrota:** quem é nocauteado deitado (ou no ar, que cai deitado) não levanta mais.
Quem cai de pé (o último golpe não derruba) toca o clipe `DEFEAT` do pack uma vez, 2× mais
rápido (~2 s), e fica na última pose; packs sem `DEFEAT` ficam na guarda.

Testes em `RecoveryTest`.

## Wall bounce e ground bounce — v0.83

Dois novos `launchType` no pack, controlados pela `ComboSession` (no máximo **1 de cada por
combo**, `CombatConfig.maxWallBounces` / `maxGroundBounces`). Valores em
`CombatConfig.wallBounce*`, `groundBounce*` e `bounceHitstunFrames`.

| launchType | Onde (packs atuais) | O que faz |
| --- | --- | --- |
| `wallBounce` | **H**, **jH** (p01) | Só **dentro de combo** (o H solto no neutro é um golpe normal). O defensor voa a 1500 u/s até a "parede" (a borda da arena ou 620 px à frente do atacante, a borda da tela) e quica: volta um pouco e sobe, aberto para continuar. Usado o do combo: hit normal. |
| `groundBounce` | **↓ + jH** (`j2H`, p01) | Alvo no ar: é cravado no chão (1500 u/s) e quica para cima. Usado o do combo: no super pulo vira o slam de antes (queda forçada); fora dele, hit aéreo normal. |

- Depois de um bounce o defensor tem `bounceHitstunFrames` (48) de hitstun, pode ser
  juggleado e **cai derrubado** (não se recupera no ar: `hardFall`).
- Qualquer golpe novo cancela o voo em andamento; o limite de juggle continua valendo.
- O motor avisa `WALL_BOUNCE` e `GROUND_BOUNCE` em `cues()` e a tela treme.
- Rotas: L → M → H (wall bounce) → dash → M → 2H (launcher) → super pulo → jL → jM → jH
  (ground bounce) → ... ; com a barra, Super ou ultra no fim.
- **Corrida automática depois da parede** (`CombatConfig.wallChaseFrames`, 54): depois do H que
  quicou na parede, segurar **→** já sai correndo quando o golpe acaba, sem o → →. É o mesmo
  atalho do 2H, que transforma o pulo em super pulo sem o ↓ ↑.
- **Lançador no ar**: o 2H que pega o defensor já no ar (voltando da parede, por exemplo) o lança
  só até a altura de um lançamento do chão (`launchSpeed²/2·gravity`). Antes ele saía com a
  velocidade cheia de onde estava, ficava ~150 px acima de quem lançou durante todo o super
  pulo, e o ↓ + jH (ou o jL) não alcançava.

Avisos do motor (v0.83): os avisos que não são dano (`TECH`, `PUSHBLOCK`, `GUARD_CANCEL`,
`WALL_BOUNCE`, `GROUND_BOUNCE`) saíram dos `events()` (que agora só têm acertos e bloqueios)
e vão para `cues()`.

Testes em `BounceTest`.

## Air dash — v0.82

**→ →** no ar faz o **air dash** e **← ←** o **back air dash** (o mesmo toque duplo do dash
no chão). Valores em `CombatConfig.airDash*`.

- Um por pulo (normal ou super pulo); tocar o chão (ou cair derrubado) devolve.
- Avança 900 u/s por 14 frames (para trás: 640 u/s por 12) **mantendo a altura**.
- Precisa de pelo menos `airDashMinHeight` (36) de altura: logo depois de sair do chão não
  sai (e não gasta o air dash).
- Golpes aéreos podem sair durante o air dash: o impulso horizontal continua e a gravidade
  volta. Tomar um golpe corta o air dash.
- Sem arte própria ainda: usa o clipe de dash (ou backdash) e linhas de velocidade.
- Pursuit: a perseguição depois do launcher continua sendo o super pulo do 2H
  (`launcherChase`); o air dash dá o resto da mobilidade, então não há um "Pursuit Dash"
  separado.

Testes em `AirDashTest`.

## Controle no ar: pulo comprometido e drift do super pulo

Como no FighterZ e no MvC. O jeito de mover de lado no ar é decidido na saída do chão
(`CombatFighter.jumpMode`):

| Modo | Quando | No ar |
|---|---|---|
| `PULO_COMPROMETIDO` | pulo normal (↖ ↑ ↗), pulo no ar (jM → pulo), depois do wall jump | a direção fica fixa: para frente `walkSpeed`, para trás `walkBackSpeed`, para cima parado. Nos primeiros `jumpDirectionFrames` (3) ainda dá para escolher a direção (o "pré-pulo") |
| `PULO_SUPER` | super pulo (↓ ↑, ou a perseguição do launcher) | drift: o stick puxa aos poucos (`superJumpDriftAccel`, 900 u/s²) até a velocidade de andar daquele lado; solto, o embalo continua. Dá para frear e virar no meio, mas não de uma vez |
| `PULO_CORRIDA` | pulo saído do dash para frente | veja abaixo |
| `PULO_LIVRE` | no ar sem ter pulado: recuperação no ar, depois de apanhar | o stick dirige direto, como antes |

- Golpes aéreos seguem o mesmo modo do pulo.
- Apanhar no ar e tocar o chão voltam o modo a livre.
- O tutorial (lições GOLPE NO AR e CRAVADA) põe o boneco a `JUMP_IN_GAP` (140) para o pulo
  para frente pegar na descida; o exemplo anda de volta entre as tentativas.

Testes em `ControleNoArTest`.

Na tela (`GameView`), o super pulo tem dois efeitos:

- **Saída:** anel de choque achatado no chão, coluna de luz curta, riscos subindo e poeira
  nos pés, na cor do lutador (`ImpactEffects.spawnSuperJump`).
- **Quem ficou no chão:** a câmera sobe com quem pulou e o outro some por baixo; quando o
  peito dele sai da tela, uma seta na cor dele aparece na borda de baixo, acima das barras de
  super, na posição dele (`drawGroundMarkers`). Some no Ultra, no Super e nas cinemáticas.

## Pulo da corrida (dash jump)

Como no KOF: pular **durante o dash para frente** leva o embalo da corrida. Valor em
`CombatConfig.dashJumpSpeed`.

- No ar ele anda 540 u/s para frente (o pulo normal anda `walkSpeed`, 300 u/s): mais ou
  menos 380 de distância contra 210, a mesma altura e o mesmo tempo no ar.
- Vale com ↑ ou ↗ (soltar a frente no mesmo quadro do pulo não perde o embalo).
- O pulo é comprometido: a direção é a da saída e o stick não freia nem vira no ar. Os
  golpes aéreos saem com o mesmo embalo. Air dash e wall jump funcionam como sempre.
- Acaba ao tocar o chão, ao apanhar no ar ou ao cair derrubado; a corrida também acaba no
  pulo.
- Sem arte própria: usa o clipe de pulo.

Testes em `DashJumpTest`.

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

### Guard break

Defender para sempre não funciona: cada defesa gasta a **guarda** (`FighterState.guard`,
0..`CombatConfig.GUARD_MAX` = 1000), e quando ela zera a defesa **quebra**.

- **Gasto**: blockstun do golpe × `guardDrainPerBlockstunFrame` (6). Um leve gasta ~60, um
  forte ~100–150, um Super 100–270. Batidas de um golpe de várias batidas antes da última
  gastam metade. Uma sequência L-M-H defendida gasta ~230–350: **umas 3 sequências seguidas**
  (2 com especial) quebram; os pesados (p07, p09) quebram mais rápido.
- **Volta**: depois de `guardRegenDelayFrames` (90, 1,5 s) sem defender, 3 por quadro (cheia
  em ~5,5 s). Quem está fora de campo recupera também. Para no congelamento do Super/Ultra.
- **Quebrou** (cue `GUARD_BREAK`): o golpe que quebrou não tira vida, mas quem defendia fica
  **aberto** por `guardBreakStunFrames` (50) depois de um congelamento de 18 — um hitstun longo,
  sem defesa e sem o empurrão da defesa (fica ao alcance do castigo). No ar, cai e termina no
  chão. A guarda volta cheia. `CombatFighter.guardBroken` marca o atordoamento.
- **Tela**: "GUARD BREAK!", tremida, estalo amarelo no peito e o som `guarda_quebrada`
  (`tools/sons/gerar_sons.py`). A guarda aparece numa barrinha embaixo dos pés só quando não
  está cheia: amarela, vermelha piscando abaixo de 30% (`guardWarningPermille`).
- **CPU**: vê a quebra com a reação do nível e, com a chance de punir dele, corre e abre o
  combo do caderno. Com a própria guarda no vermelho, faz bem mais pushblock/Guard Cancel e
  espera menos defendendo por hábito.
- A vida cheia (treino, VIDA P1/CPU) devolve a guarda cheia.

Testes em `GuardBreakTest`.

### Golpe alto, chip e defesa perfeita

Três peças para a defesa não ser de graça nem perfeita sem esforço (inspiradas no overhead do
SF6/FighterZ, no chip de especial de todos e no Just Defense do Garou).

- **Golpe alto (→ + M, `GolpeAlto`)**: universal. Montado do M de cada personagem (a mesma
  animação, esticada) quando o pack não declara um `6M`: preparo 20, ativo 3, recuperação 14,
  dano 115% do M, +3 no acerto, −6 na defesa. Pega quem defende **agachado**; em pé defende
  (`AttackDefinition.overhead`, `guardStops`). No acerto cancela nos especiais e no Super (os
  do H). Sai só do neutro (`CancelSystem`: dentro de um combo o → + M continua sendo o M) e
  uma vez por combo. Ao sair, cue `OVERHEAD`: brilho amarelo na cabeça para dar para ler.
- **Chip**: especial defendido tira 1/8 do dano da batida (`chipSpecialPermille` 125), Super e
  Ultra 1/4 (`chipSuperPermille` 250). O arranhão vira **vida vermelha** inteira
  (`FighterState.chip`: volta fora de campo) e **nunca derruba** (para em 1). Normais não arranham.
- **Defesa perfeita**: apertar trás (a defesa, alta ou baixa; no ar, qualquer defesa do ar)
  até `justDefenseFrames` (5) quadros antes do contato. Não gasta guarda, não leva chip, cura
  1,2% da vida (`justDefenseHealPermille`, primeiro a vermelha), dá a barra de novo e o
  blockstun cai a 40% (`justDefenseBlockstunPermille`): sai antes e pode punir. Quem bate trás
  sem parar não ganha: um aperto até `justDefenseLockoutFrames` (20) depois do anterior não abre
  a janela; segurar trás desde antes é a defesa comum. Cada batida pede um aperto novo.
  Cue `JUST_DEFENSE`: "DEFESA PERFEITA!", brilho branco no peito e o som da defesa mais agudo.
- **CPU**: contra quem está agachado entra o 6M na escolha (peso 0,22). Vendo um golpe alto,
  defende em pé (errar a altura continua na chance do nível). Do nível 5 em diante tenta a
  defesa perfeita (`CpuLevel.justDefense`: 8% por nível acima do 4) quando dá tempo: fica parada
  e aperta a defesa 2 quadros antes do contato. Na prática sai mais contra projétil (contra
  golpe rápido a reação não deixa tempo).
- **COMANDOS** (pausa): "Golpe alto → + M" e "Defesa perfeita ←" em todas as fichas; com muitas
  linhas elas ficam mais baixas para caber acima do rodapé.

Testes em `DefesaEGolpeAltoTest`.

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
- O jogo mostra "AGARRÃO!" ou "TECH!" no centro da tela. Arte: `THROW_GRAB` (pegar e
  segurar) e `THROW_TOSS` (jogar) quando o pacote tem (p01); sem elas o atacante usa o jab
  para pegar e o soco forte para jogar. O defensor usa a reação de hit.

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
| `launchType` | não (`none`) | `knockdown`, `launch`, `slam` (slam exige super pulo e alvo no ar), `wallBounce`, `groundBounce` (v0.83) |
| `knockback` | não (= pushbackOnHit) | Deslocamento quando o alvo fica no ar ou cai |
| `juggleCost` | não (1) | Pontos de juggle gastos em alvo no ar |
| `maxHits` | não (1) | Acertos por execução no mesmo alvo (≤ número de hitboxes) |
| `maxUsesPerCombo` | não (0 = livre) | Acima disso o CancelSystem recusa a rota |
| `maxUsesPerChain` | não (0 = livre) | Usos do golpe numa mesma corrente (desde o último golpe que saiu do neutro), acertando ou na defesa. Zera depois de uma ligação |
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

## Ajustar caixas no celular (DEBUG > AJUSTAR CAIXAS)

Tela do DEBUG, abaixo de AJUSTAR TAMANHOS. Escolha o personagem (◀ ▶) e o item: **corpo em
pé**, **corpo agachado**, cada **golpe** (L, M, H, agachados, aéreos, S2–S4), **magia** e
**super**. A animação toca no tempo do golpe (60 quadros por segundo) com as caixas por cima:

| Cor | Caixa | Campos (`character.json`) |
|---|---|---|
| verde | corpo que leva golpe | `fighter.body.halfWidth`, `standHeight`, `crouchHeight` |
| amarelo | empurrão | `fighter.body.pushHalfWidth`, `pushHeight` |
| vermelho | acerto do golpe: da raiz até o alcance, da base até a altura + 19,5 (cheio nos quadros ativos) | `moves.<botão>.reach`, `hitHeight`, `hitFloor`; `specialMoves.S2…` |
| círculo | saída da magia (cheio depois do quadro em que sai) | `fighter.energy.spawnX`, `spawnY`, `crouchSpawnY`; `fighter.super.…` |

PAUSA, VEL (¼x, ½x, 1x), ◀ QUADRO / QUADRO ▶ e IR AO IMPACTO (para no primeiro quadro ativo)
ajudam a conferir se o vermelho cobre o punho no impacto. −10/−2/+2/+10 mudam o valor,
ORIG volta ao do pacote. Cada toque fica salvo no aparelho; a tela é só prévia (a luta usa
o pacote). **SALVAR NO DRIVE** exporta `ajustes-caixas-<data>.json` de todos os personagens;
para gravar:

```
python3 tools/sprites/caixas.py --aplicar ajustes-caixas-<data>.json
python3 tools/sprites/build_characters.py --write
```

Cada ajuste leva o valor em que foi feito; se o pacote mudou desde então, o `caixas.py`
recusa em vez de passar por cima, e no app o ajuste antigo deixa de valer sozinho.

**Base do acerto (`hitFloor`).** Sem ela a caixa vermelha é uma faixa de 39 de altura em
volta de `hitHeight`. Num golpe que varre de baixo para cima (gancho, 2H) a altura marca o
punho lá no alto e a base desce a caixa até onde o golpe começa, para ainda acertar quem está
em pé. Valor ausente = altura − 19,5.

Conferências do build depois de aplicar (o `caixas.py` grava o que veio do app; o
`build_characters.py` recusa o que quebra a luta):

- empurrão dentro do corpo: `pushHalfWidth ≤ halfWidth` e `pushHeight ≤ crouchHeight + 60`
  (o empurrão baixo deixa um pulo passar por cima do outro);
- magia sai de dentro do corpo: `spawnY`/`airSpawnY ≤ standHeight`, `crouchSpawnY ≤ crouchHeight`;
- `hitFloor ≤ hitHeight`.

Duas coisas o build não pega e vale olhar no app: golpe de chão com a caixa inteira acima da
cabeça de alguém (a base dele acima de `standHeight` do rival não acerta quem está em pé) e
alcance menor que a distância colado (`pushHalfWidth` dos dois − `halfWidth` do rival), que
nunca encosta.

Rodada de 14/10/2026 (`docs/personagens/regua/ajustes-caixas-20261014.json`: P01, p02 e p03
da manhã + p04–p12 da tarde, 375 ajustes) e o que precisou de acerto ao gravar:

- empurrão cortado no limite: p03, p04, p07, p08, p09, p10, p11, p12 e P01 (altura), p04, p08
  e p12 (largura);
- saída da magia agachado cortada na altura agachado: p03, p04 e p05;
- base do acerto na altura antiga nos 26 golpes que ficaram acima da cabeça do rival mais
  baixo (todos os 2H, p03 H/S4, p04 S2, p07 L/M/H/S2, p08 L/S3, p09 L/S3, p10 S4, p11 L/M/S2);
- alcance do 2H (lançador) para pegar depois de um L ou M que acertou, contra todo o elenco:
  distância depois do M + 20. P01 41→79, p02 54→86, p03 36→89, p04 39→87, p05 43→105,
  p06 35→88, p09 45→88, p11 68→109, p12 50→98 (p07, p08 e p10 já alcançavam).

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
- (Fase 10 feita na v0.83: wall bounce e ground bounce.)
- Os packs usam a hitbox padrão; nenhuma hitbox/hurtbox por frame foi desenhada ainda,
  embora o formato e o motor suportem.

## Peso dos golpes (som, tremida, câmera)

Só apresentação; o frame data não muda (fora o congelamento extra abaixo).

- **Sons de luta** (`assets/sons/luta`, gerados por síntese em `tools/sons/gerar_sons.py`):
  "vush" do golpe no ar pouco antes de ele bater (leve/médio/pesado), acerto leve/médio/
  pesado/especial, defesa, queda no chão, punish counter, KO e a ativação do Super. Cada um toca com o tom
  variando um pouco, para dois socos seguidos não soarem iguais.
- **Tremida**: quem apanha treme de um lado para o outro durante o congelamento do acerto
  (hitstop); na defesa, bem menos (`ImpactFeel.hitShake`).
- **Soco de câmera**: médio, pesado e especial aproximam a câmera um pouco (1,2%, 2,5% e 5%) no ponto do
  impacto e ela volta sozinha (`ImpactFeel.ZOOM`).
- **Impact frames**: pesado, especial e punish counter escurecem o cenário por 3–6 quadros,
  com os lutadores na frente (`ImpactFeel.DARK`).
- **KO**: o golpe que nocauteia congela mais (`koHitstopBonus`, +16), a tela treme forte,
  um clarão branco e câmera lenta (~0,7 s de luta a 35%).
- **Punish counter**: congela +8 quadros (`punishHitstopBonus`).
- **Impact frames** (a técnica dos animes): por 2 quadros a cena vira silhueta, com linhas
  de velocidade saindo do impacto, e 1 quadro de cores invertidas antes de voltar.
  - Vermelho e preto (fundo vermelho, silhuetas pretas com contorno branco): punish counter
    e o golpe que fecha um combo de 5+ acertos jogando o rival longe (queda, parede, chão, giro).
  - Branco no vazio (tudo preto, silhuetas brancas): KO, acerto do Super e o último dano do
    Ultra (o último golpe do Ultra em sequência e a explosão final do Ultra de raio).
  - No máximo um a cada 30 quadros (só o branco passa na frente do vermelho), para a tela não
    ficar piscando. A pausa tem **IMPACTO: LIGADO/DESLIGADO** (salvo no aparelho), para quem
    é sensível a luz piscando.

- **Raios de luz** (referência: FighterZ): todo acerto solta lanças de luz que saem do ponto
  do golpe e abrem até cruzar a tela (1 no leve, 2 no médio, 3 no pesado, 4 no especial),
  finas no impacto e largas na ponta, com brilho em volta (`ImpactEffects.SPEAR_*`).
- **Close de cinema**: no lançador, no golpe que joga o rival na parede/no chão e no KO, a
  câmera corta para um close de ~1,8× no impacto, inclinado 8° a favor do golpe, com o fundo
  escurecendo e a luta a 45% por 12 quadros, e volta com corte seco (`ImpactFeel.CINEMA_*`).
  O H que joga na parede ganha um close mais aberto (~1,5×, `CINEMA_ZOOM_WALL`).
  Um por combo (90 quadros de intervalo); o KO sempre ganha o seu.
- **Poeira e pedrinhas** quando um corpo bate no chão (mais forte vindo do alto) e no quique.

- **Pose do impacto** (medido no FighterZ: o golpe fica parado esticado 0,12–0,2 s e volta
  à guarda em 1–2 quadros): golpes médios e fortes que acertam congelam +3/+4 quadros
  (`mediumHitHoldFrames`, `heavyHitHoldFrames`; os dois congelam juntos, então a vantagem e os
  combos não mudam) e, depois da janela ativa, o desenho volta à guarda em 3 quadros e fica
  nela no resto da recuperação (`ImpactFeel.snapBackTime`). Golpe que erra volta devagar,
  como antes. Defesa e golpes de vários acertos não mudam.

Testes em `ImpactFeelTest` e `PunishCounterTest`.
