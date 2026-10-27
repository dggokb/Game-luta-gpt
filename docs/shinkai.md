# SHINKAI

O SHINKAI é o estado de cada lutador e a parte do combate que mais diferencia o jogo.
Ele **substitui o Overdrive** (v0.87). Fonte da ideia: "Jogo de Luta — Ideias de Mecânicas e
Roadmap do Combate", no Drive (seções Barra SHINKAI e Anime Clash).

Código: `Shinkai.java` (os tipos e a divisão do elenco), `FighterState` (a barra e o estado),
`CombatEngine` (ativação e bônus), `TeamSystem` (o relógio), `HudRenderer` (a barra e o
botão), `CorteShinkai` (o corte de ativação), `ShinkaiDetalhes` e `GameView` (contorno, detalhes de cada tipo, clarão, parry, sumiço). Testes: `ShinkaiTest` e
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
  - os dois congelam por `shinkaiFlashFrames` (48, 0,8 s); o stun do rival espera, então o combo
    continua;
  - a tela pisca na cor do tipo e um anel e um brilho saem do lutador;
  - entra o **corte de ativação** (`CorteShinkai`), como nos animes: uma faixa de mangá
    inclinada atravessa a tela vinda do lado de quem ativou. Nela aparecem os olhos dele (o rosto
    do HUD, sem arte nova), linhas de velocidade na cor do tipo e **SHINKAI** grande com o tipo
    embaixo. A tela escurece atrás.
- **Duração.** Dura `shinkaiFrames` = 480 quadros (**8 s**, como era o OD). A barra esvazia
  mostrando o tempo e o botão mostra os segundos. No último quinto o contorno falha, avisando que
  vai acabar.
- **Em volta do corpo** (sem aura e sem animação nova): um **contorno de energia** na cor do tipo.
  É a silhueta do próprio quadro, desenhada atrás dele em várias direções, tremendo: uma borda
  larga e fraca e outra fina e clara (`GameView.drawShinkaiOutline`). Ele acompanha cada quadro
  da animação.
- **O detalhe de cada tipo** (`ShinkaiDetalhes`, e o rastro no `GameView`):
  - **POWER**: pedrinhas sobem do chão em volta dos pés, e cada golpe que acerta trinca o chão
    debaixo do rival e treme a tela;
  - **SPEED**: imagens residuais na cor do tipo atrás dele ao andar e correr;
  - **ENERGY**: raios curtos estalando em volta do corpo;
  - **TECHNIQUE**: uma estrela de brilho na ponta do golpe, no quadro em que ele acerta.
- **Uma vez por personagem no round** (`shinkaiUsed`). Cada um da dupla tem o seu.
- **Troca.** O SHINKAI é de quem está em campo. Sair na troca acaba com ele e conta como usado.
- **Round novo.** `CombatEngine.refreshShinkai(side)` libera o uso de novo, mas a barra continua
  como estava. No treino, os botões **VIDA P1** e **VIDA CPU** também enchem a barra, para testar.
- **Todos os tipos**, durante o SHINKAI (o que se sente no primeiro golpe):
  - todo golpe bate **25% mais forte** (`shinkaiDamagePermille`), e o dano aparece **grande na
    cor do tipo** onde o golpe entrou (os golpes seguidos somam no mesmo número —
    `ShinkaiDetalhes.numero`);
  - **defendido, todo golpe arranha** (até o L): 15% do dano vira vida vermelha
    (`shinkaiChipPermille`);
  - o **primeiro Super sai sem barra** e ainda **20% mais forte** (`shinkaiSuperDamagePermille`);
    o botão SUPER acende mesmo sem barra (`FighterState.canPaySuper`). Os seguintes pagam barra;
  - recuperam a vida vermelha em campo (`shinkaiRegenPerFrame` = 12 por quadro).

## Os quatro tipos

A divisão abaixo é o ponto de partida. Para mudar, basta editar `Shinkai.typeOf`.

| Tipo | Quem | Cor | O que ganha |
|---|---|---|---|
| **POWER** | p04, p09, p12, p13 | laranja | **Corpo de aço**: aguenta qualquer golpe sem perder a ação e leva só 70% do dano (`shinkaiPowerArmorPermille`). Agarrão e Super entram normal. **O H joga o rival na parede**, mesmo sozinho no neutro, e ele volta quicando — dá para continuar o combo. O chão trinca a cada golpe. |
| **SPEED** | p03, p05, p10, p16 | ciano | **Imagem residual**: todo golpe normal é repetido por uma imagem ciano no mesmo lugar `shinkaiSpeedEchoDelay` (7) quadros depois, com metade do dano — **todo golpe bate duas vezes**. 30% mais rápido ao andar, no dash e no air dash (`shinkaiSpeed`) e um air dash a mais. **Sumiço**: o dash (→ →), com o rival a até 560 px, vira sumir por 10 quadros (nada acerta) e aparecer **atrás do rival**, já virado para ele. Cada sumiço gasta 45 quadros do SHINKAI. |
| **ENERGY** | p01, p06, p08, p11 | roxo | **O H solta junto um mini projétil** roxo (45% do dano da energia, `shinkaiEnergyShotDamagePermille`). O **S sai sem esperar o anterior** (até `shinkaiEnergyProjectiles` = 5 na tela), maior e 30% mais rápido. A **barra de Super enche sozinha** (uma barra a cada 4 s). |
| **TECHNIQUE** | p02, p07, p14, p15 (e quem não está na lista) | dourado | **Aparada automática**: o primeiro golpe que chega com ele livre (parado ou andando) é aparado sozinho — "PARRY!", nada entra e ele sai livre. Depois, o parry é o toque → até 10 quadros antes do golpe (também defendendo, também contra projétil; agarrão não se apara). **Cancels livres**: o golpe que acertou ou foi defendido cancela em qualquer outro. |

Nada disso precisa de comando novo: são os mesmos botões, com o resultado na tela.

## SHINKAI Clash

Ligar o SHINKAI **com o rival já no dele** (a qualquer momento dos 8 s dele) vira o **SHINKAI
Clash**: um cabo de guerra jogável no meio da tela, sem QTE (`ShinkaiClash` no motor,
`ConfrontoShinkai` no desenho; testes em `ShinkaiClashTest`).

- **Começo.** O corte de quem respondeu passa (0,8 s); o que estava no ar some (projéteis, ecos,
  capangas), os combos acabam e os dois ficam frente a frente no chão, a `shinkaiClashGap` (300 px)
  um do outro. A luta para: nada anda, e os relógios do SHINKAI esperam.
- **A disputa** (até 5 s, `ShinkaiClash.DURACAO`): cada um empurra a marca com os próprios
  botões, do jeito do tipo dele:

  | Tipo | Como empurra |
  |---|---|
  | POWER | força: o **H** empurra muito (58), mas tem fôlego de 12 quadros; o M menos, o L pouco |
  | SPEED | velocidade: o **L** empurra bem (36) quase sem espera — bater rápido |
  | ENERGY | alimentar o raio: **segurar** um botão carrega (até 70 quadros) e **soltar** empurra; a carga cheia vale muito mais que várias curtas (carga²/16), e enquanto segura não empurra |
  | TECHNIQUE | ritmo: um pulso a cada 24 quadros (o anel que fecha no corpo); apertar **no pulso** empurra muito (105), fora dele quase nada |

  Bem jogados, os quatro empurram parecido (uns 4–5 por quadro); o que muda é o jeito.
- **Desistir.** Segurar **trás** por meio segundo sai do confronto **sem dano**, mas **perde o
  SHINKAI**; o outro segue no dele.
- **Fim.** Quem leva a marca até o fim (`META` = 600), ou está na frente quando o tempo acaba,
  **vence**: o golpe dele entra (`shinkaiClashDamage` = 2000), quem perdeu voa girando e **perde o
  SHINKAI**, e o SHINKAI de quem venceu **volta cheio** (8 s de novo). Empate exato: os dois se
  afastam e seguem no SHINKAI.
- **Na tela.** A tela escurece com linhas de velocidade saindo do encontro, cada metade puxando
  para a cor de quem está daquele lado. Cada um empurra com o visual do tipo até o encontro (o
  raio do ENERGY, que engrossa ao soltar e tem a bola crescendo na mão enquanto carrega; as ondas
  de choque do POWER, com o chão rachando; a rajada de riscos do SPEED; o anel do ritmo e os cortes
  da TECHNIQUE). O encontro é uma bola nas duas cores, com raios, que anda conforme a disputa; cada
  empurrão solta faíscas e treme a tela. No alto, a barra do cabo de guerra com "SHINKAI CLASH" e
  o tempo acabando. As poses: POWER e TECHNIQUE no H, SPEED trocando L, M e H a cada empurrão,
  ENERGY no disparo da energia. O fim estoura no encontro na cor de quem venceu, com câmera lenta.
- **CPU.** Com a barra cheia e o jogador no SHINKAI, às vezes responde e começa o confronto
  (`SHINKAI_ANSWER_CHANCE`). No confronto ela empurra do jeito do tipo dela, mais rápida e certeira
  nos níveis altos.

## CPU

A CPU usa o SHINKAI quando a barra está cheia, de duas formas:
- no meio do combo que acertou (`SHINKAI_COMBO_CHANCE`, mais nos níveis altos);
- no neutro, com menos de 60% de vida (`SHINKAI_LOW_LIFE_CHANCE`).

O sumiço dos lutadores SPEED sai sozinho quando ela pede o dash. O parry ela ainda não usa.

## Próximos passos (do documento do Drive)

- **SHINKAI Clash v2.** Cenas próprias por par de tipos (POWER × POWER de mãos travadas, SPEED ×
  SPEED trocando sumiços pela arena) e ligação com o cenário que quebra.
- **Arte.** O corte usa o rosto do HUD. Uma ilustração parada por personagem (os olhos em close,
  sem vídeo) deixaria o corte ainda melhor. Falta também um som próprio de ativação, ou uma voz.
- **Prólogo.** O pai ensina o SHINKAI e as crianças falham.
