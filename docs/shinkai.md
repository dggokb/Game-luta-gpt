# SHINKAI

O SHINKAI é o estado de cada lutador e a parte do combate que mais diferencia o jogo.
Ele **substitui o Overdrive** (v0.87). Fonte da ideia: "Jogo de Luta — Ideias de Mecânicas e
Roadmap do Combate", no Drive (seções Barra SHINKAI e Anime Clash).

Código: `Shinkai.java` (os tipos e a divisão do elenco), `FighterState` (a barra e o estado),
`CombatEngine` (ativação e bônus), `TeamSystem` (o relógio), `HudRenderer` (a barra e o
botão), `GameView` e `EffectsRenderer` (clarão, aura, parry, sumiço). Testes: `ShinkaiTest` e
`DemoDirectorTest` (demo 87).

## Regras de todos

- **A barra.** Cada lutador tem a própria barra (`FighterState.shinkai`, até
  `CombatConfig.SHINKAI_MAX` = 1000). Ela fica entre a vida do titular e a do parceiro, no lugar
  da antiga barra de 5 partes do assist.
- **Como ela enche:**

  | Ação | Quanto enche | Configuração |
  |---|---|---|
  | Apanhar | 30% do dano levado (cheia com uns 3.300 de dano) | `shinkaiPerDamageTakenPermille` |
  | Bater | 10% do dano dado | `shinkaiPerDamageDealtPermille` |
  | Defender um golpe | 12 | `shinkaiOnBlock` |
  | Quebrar um combo (combo breaker) | 150 | `shinkaiOnBreaker` |

  Durante o SHINKAI e depois de usado no round, a barra não enche.
- **Cheia.** A barra brilha na cor do tipo e aparece escrito **SHINKAI**. O botão também acende.
- **Ativação.** Botão **SHINKAI** (onde era o OD, à esquerda de SUPER e TAG), **SUPER + TAG
  juntos** ou a tecla **P** (gatilho LT no controle). Só sai com a barra cheia. Pode ser livre
  ou cancelando o próprio golpe (não o Super), então dá para ligar **no meio do combo**.
- **Na ativação:**
  - os dois congelam por `shinkaiFlashFrames` (30); o stun do rival espera, então o combo
    continua;
  - a tela pisca na cor do tipo;
  - um anel e um brilho saem do lutador;
  - aparece o aviso **"SHINKAI POWER!"** (ou o tipo dele).
- **Duração.** Dura `shinkaiFrames` = 480 quadros (**8 s**, como era o OD). A barra esvazia
  mostrando o tempo e o botão mostra os segundos. No último quinto a aura falha, avisando que
  vai acabar.
- **Uma vez por personagem no round** (`shinkaiUsed`). Cada um da dupla tem o seu.
- **Troca.** O SHINKAI é de quem está em campo. Sair na troca acaba com ele e conta como usado.
- **Round novo.** `CombatEngine.refreshShinkai(side)` libera o uso de novo, mas a barra continua
  como estava. No treino, os botões **VIDA P1** e **VIDA CPU** também enchem a barra, para testar.
- **Todos os tipos** recuperam a vida vermelha em campo durante o SHINKAI
  (`shinkaiRegenPerFrame` = 12 por quadro).

## Os quatro tipos

A divisão abaixo é o ponto de partida. Para mudar, basta editar `Shinkai.typeOf`.

| Tipo | Quem | Cor | O que ganha |
|---|---|---|---|
| **POWER** | p04, p09, p12, p13 | laranja | **Corpo de aço**: aguenta qualquer golpe sem perder a ação e leva só 70% do dano (`shinkaiPowerArmorPermille`). Agarrão e Super entram normal. Os golpes dele batem **20% mais forte** (`shinkaiPowerDamagePermille`). |
| **SPEED** | p03, p05, p10, p16 | ciano | **30% mais rápido** ao andar, no dash e no air dash (`shinkaiSpeed`), e **um air dash a mais** por pulo. **Sumiço**: → → no chão, com o rival a até 560 px, faz ele sumir por 10 quadros (nada acerta) e aparecer **atrás do rival** (120 px atrás), já virado para ele. Cada sumiço gasta 45 quadros do SHINKAI. O golpe apertado enquanto ele está sumido sai assim que ele aparece. |
| **ENERGY** | p01, p06, p08, p11 | roxo | A **barra de Super enche sozinha** (uma barra a cada 4 s, `shinkaiEnergyMeterPerFrame`). Os projéteis saem **maiores, 30% mais rápidos e 25% mais fortes**, e podem ter **dois na tela** ao mesmo tempo. |
| **TECHNIQUE** | p02, p07, p14, p15 (e quem não está na lista) | dourado | **Cancels livres**: o golpe que acertou ou foi defendido cancela em qualquer outro (era o Overdrive). **Parry**: tocar → (apertar, não segurar) até 10 quadros antes do golpe chegar faz o golpe não entrar. Os dois congelam por 16 quadros e quem aparou sai livre, enquanto o golpe do outro continua. Funciona livre ou defendendo, contra golpes e projéteis, mas agarrão não se apara. |

## CPU

A CPU usa o SHINKAI quando a barra está cheia, de duas formas:
- no meio do combo que acertou (`SHINKAI_COMBO_CHANCE`, mais nos níveis altos);
- no neutro, com menos de 60% de vida (`SHINKAI_LOW_LIFE_CHANCE`).

O sumiço dos lutadores SPEED sai sozinho quando ela pede o dash. O parry ela ainda não usa.

## Próximos passos (do documento do Drive)

- **SHINKAI Clash.** Os dois ativando quase juntos viram um confronto jogável conforme os tipos:
  POWER × POWER é disputa de força, ENERGY × ENERGY é choque de raios, SPEED × SPEED é batalha de
  sumiço, e as combinações cruzadas também.
- **Arte.** Hoje a aura, o clarão e o rastro do sumiço são desenhados pelo código. Falta a arte
  própria de cada tipo: a aura e um grito ou pose na ativação.
- **Prólogo.** O pai ensina o SHINKAI e as crianças falham.
