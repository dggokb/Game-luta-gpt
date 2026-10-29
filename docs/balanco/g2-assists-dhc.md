# Frente G2: assists de p12–p15 (G2a) e DHC (G2b)

Base: HEAD `5cd8ed16`, código igual ao das etapas 1–5. Baterias rodadas com as mesmas sementes:
`BateriaAntesDepois` (base `ad/6_head.tsv`, depois `ad/g2a.tsv` e `ad/g2b.tsv`), `BateriaEquipe`
t17, t18, t20, t21, e `BateriaComparacoes` sinergia (base `bal/D_sinergia.md`) e matriz (base `bal/D_matriz.md`).
As duas etapas foram medidas separadas: G2a sozinha sobre a base, e G2b = G2a + DHC (o "G2b"
das tabelas é o total; a coluna G2a → G2b isola o DHC).
Não fiz commit. Diffs: `bal/G2a.diff` (só G2a) e `bal/G2b.diff` (as duas, contra o HEAD, com docs).

Antes de tudo: a worktree completa (com `art/`) encheu o disco da sessão (0 MB livres por alguns
minutos). Apaguei a arte da minha cópia e deixei só `android/app/src/{main/java,test}`,
`characters`, `tools` e `docs` (sparse checkout). Por isso o `build_characters.py --check`
**não rodou** aqui (precisa da arte): o `GeneratedCharacters.java` foi editado à mão só no
nome do golpe de assist (p13 `"S"`→`"H"`, p15 `"M"`→`"2H"`), igual ao que o gerador emite.
**Rodar o `--check` na árvore principal ao integrar.**

## Resumo

| etapa | status | resultado principal |
|---|---|---|
| G2a assists | IMPLEMENTADO · DECIDIR (3 itens) | p12, p13, p14 e p15 acertam de 100 a 400–550 px (antes: 100 px ou nada), com antiaéreo 33–100% e cobertura do H defendido +6 a +24 (antes −2). Como parceiros de combo deixaram de valer zero: p12 soma +1805 ao combo dos outros (era −198), p14 +1394 (−198), p13 +531 (−198). p11 com p12 (dupla oficial): +2555 (era −274). Matriz 1x1: idêntica, quadro a quadro. |
| G2b DHC | IMPLEMENTADO · DECIDIR (2 itens) | O DHC não corta mais o Super. p07+p08: 2196 → 4934 (Super sozinho: 4545). Kōga+p04: 3276 → 4504. Nas 5 duplas em que o Super entra, a 2ª barra passa a render +858 em média (era +64), contra 3042 de um Super inteiro: melhorou, mas segue não sendo o melhor uso da barra. |

## G2a: assists de corpo a corpo

### Causa

O assist entrava 70 px atrás do titular e fazia o golpe parado. Os golpes de assist de p12,
p13 e p14 são o S1 deles, que no jogo normal **anda** (investida de 520 px/s no p12, de
500 px/s por 71 quadros no p13) ou **mergulha do alto** (p14, "só no ar"). No assist o corpo
não andava nem subia: p12, p13 e p15 só pegavam o rival colado, e o p14 batia no ar vazio a partir do chão.

### O que mudou (genérico, para todo assist com caixa de acerto)

- `TeamSystem.startAssist` (TeamSystem.java:359–374): o assist de corpo a corpo vai até onde o
  golpe pega o rival (80% do `reach`), entre o lugar de sempre (70 px atrás) e
  `assistMeleeAhead` = 220 px à frente do titular. A entrada dura os mesmos 8 quadros de antes:
  numa 1ª versão a entrada era mais longa (mesma velocidade, de mais longe) e isso tirava o
  assist do tempo dos combos. Exemplo: o p07 parava de somar +1919 ao combo do p08. Corrigido antes da medição final.
- `CombatEngine.moveAssist` (CombatEngine.java:2175) e `assistAwaitsGround` (:2203): a cada quadro
  do golpe, a investida avança até encostar no rival. O mergulho cai na diagonal (`diveForward`,
  `diveDescent`) e, se pegou, quica para trás. O golpe não acaba no ar.
- Assist de mergulho (`assistDive`, TeamSystem.java:46): entra pulando até
  `assistDiveHeight` = 240 px (CombatConfig.java:535). Durante a entrada a arte é a do pulo, e o
  golpe é o S1 do p14, a arte dele no ar.
- `CombatEngine.assistAttack` (:2160) diz qual golpe o assist faz. Antes isso só existia dentro de `startAssistMove`.
- Dados: p13 `assist.move` "S" → "H" (`characters/p13/character.json:201`), p15 "M" → "2H"
  (`characters/p15/character.json:109`), e as duas linhas no `GeneratedCharacters.java`.
- Docs: `docs/combat-engine.md`, tabela do TAG e item "Assist corpo a corpo".

Por que p13 = H e p15 = 2H:

- **p13:** a investida do p13 como assist acertava de 100 a 1000 px, mas na defesa deixava o
  titular **+78**. Os 5 golpes seguram o rival defendendo por 80 quadros, o que dá pressão sem
  risco. Com o H (quique na parede) fica +14.
- **p15:** o M ia a 400 px, mas a cobertura ficava em −2 e ele não fazia nada além de bater.
  O 2H lança (hitstun 48) e pega 100% no antiaéreo.

Medi as duas alternativas com o T17 antes de escolher.

### T17: função de cada assist (Ryūga chama; boneco parado, defendendo e pulando)

| lutador | antes | depois | função |
|---|---|---|---|
| p12 | S · 19 q · 100–100 px · AA 0% · cobertura −2 | S · 19 q · **100–500 px** · AA 33% · **+9** | investida: cobre a aproximação e segura |
| p13 | S · 27 q · 100–100 px · AA 0% · −2 | **H** · 21 q · **100–500 px** · AA 92% · **+14** | quique na parede (abre combo de parede) |
| p14 | S · não acerta | S · 21 q · **100–550 px** · AA 92% · **+24** | mergulho do alto (cobre por cima, antiaéreo) |
| p15 | M · 17 q · 100–100 px · AA 0% · −2 | **2H** · 19 q · **100–400 px** · AA 100% · **+6** | lança para o combo aéreo |
| p07 (efeito colateral) | S · 21 q · 100–150 px · AA 0% · +11 | S · 21 q · **100–450 px** · AA 92% · +11 | tapa de luva, agora a meia distância |

Os outros 11 (projéteis) não mudaram, nem quadro nem número.

### C01 sinergia (`BateriaComparacoes sinergia`): soma ao combo dos outros, e o oficial ao dele

| lutador | soma ao combo dos outros: antes → depois | o oficial soma ao dele: antes → depois |
|---|---|---|
| p07 | +1299 → +1299 | +40 → +40 (p08) |
| p08 | +1509 → +1509 | +1919 → +1919 (p07) |
| p11 | +423 → +423 | **−274 → +2555** (p12) |
| p12 | **−198 → +1805** | −78 → −78 (p11) |
| p13 | **−198 → +531** | −100 → **−436** (p14) |
| p14 | **−198 → +1394** | −105 → **+930** (p13) |
| p15 | +617 → +881 | −989 → −989 (p16; o assist da p16 não mudou) |
| p16 | −296 → −296 | +288 → +405 (p15) |
| os outros 8 | iguais | iguais |

### T18: maior combo sem barra, com o parceiro, forçando o assist (busca em feixe 40 × 14)

| titular | p12 antes → depois | p13 | p14 | p15 |
|---|---|---|---|---|
| Ryūga | 5733 → 6158 | 5733 → **3713** | 5733 → 5985 | 5822 → 5888 |
| Kōga | 5220 → 6120 | 5220 → **3291** | 5220 → 5823 | 5388 → 5930 |
| p09 | 3434 → **7293** | 3434 → **7275** | 3434 → 6285 | 4085 → 6025 |
| p11 | 2341 → 5170 | 2341 → 5364 | 2341 → 5004 | 4695 → **3202** |

Os outros parceiros não mudaram (diferença 0 em todas as células). Os "antes" de p12–p14 eram
iguais porque o assist não acertava. O H do p13 joga o rival na parede: no p09 e no p11 isso abre
combo, no Ryūga e no Kōga corta a rota (a busca obriga a usar o assist; sem ele, o combo
continua o de antes). O 2H do p15 lança e o p11 não alcança.

### T21: titular com o oficial e três alternativos, 2x2 CPU nível 8

| titular | antes (oficial · alternativos) | depois G2a | depois G2b |
|---|---|---|---|
| p11 (oficial p12) | 50% · p14 43% · Ryūga 66% · p06 55% | 52% · 45% · 66% · 55% | 52% · 38% · 61% · 46% |
| p13 (oficial p14) | **25%** · p16 36% · Kōga 61% · p08 64% | **50%** · 50% · 68% · 68% | **50%** · 50% · 63% · 75% |
| p15 (oficial p16) | 13% · Yin-lu 9% · p05 4% · p10 18% | 9% · 18% · 7% · 21% | 7% · 11% · 9% · 23% |
| p07 (oficial p08) | 50% · p10 27% · p13 34% · Yin-lu 32% | 59% · 36% · 46% · 38% | 52% · 36% · 57% · 29% |
| Ryūga, Kōga, p05, p09 | ±0–14 (os adversários são as outras duplas oficiais, que mudaram) | | |

A p15 continua fraca com qualquer parceiro (16–17% no 1x1). Não é problema de assist.

### BateriaAntesDepois: todos os 16 (2x2 contra o elenco; ruído ±8 por lutador)

| lutador | 2x2 base | G2a | G2b | Δ | 1x1 (não muda) |
|---|---|---|---|---|---|
| Ryūga (player_base) | 65% | 68% | 66% | +1 | 70% |
| p02 | 42% | 44% | 48% | +7 | 44% |
| p03 | 51% | 47% | 50% | −2 | 60% |
| p04 | 59% | 54% | 50% | −8 | 56% |
| p05 | 39% | 44% | 43% | +4 | 43% |
| p06 | 59% | 54% | 49% | −10 | 69% |
| p07 | 31% | 32% | 30% | −1 | 15% |
| p08 | 74% | 68% | 68% | −7 | 79% |
| p09 | 75% | 65% | 69% | −6 | 62% |
| p10 | 50% | 49% | 46% | −4 | 58% |
| p11 | 54% | 46% | 45% | −8 | 56% |
| p12 | 48% | 54% | 54% | +6 | 46% |
| p13 | 53% | 58% | 60% | +7 | 57% |
| p14 | 30% | 40% | 39% | **+8** | 39% |
| p15 | 25% | 30% | 29% | +4 | 17% |
| p16 | 42% | 31% | 34% | −8 | 29% |

- Subiram os quatro que ganharam assist (p12–p15) e a p02.
- Caíram, no limite do ruído, os fortes, que agora enfrentam duplas com assist (p04, p06, p08, p09, p11), e a p16.
  - O parceiro da p16 é a p15, que trocou o M (que não acertava) pelo 2H (que lança) e a CPU ainda não aproveita.
- O resto da bateria muda pouco:
  - quebras de Breaker por luta: 0,76 → 0,97 (mais combos entram, então há mais chances de quebrar);
  - pedidos de TAG da CPU: 1989 → 1851;
  - Breaker, SHINKAI e Clash: dentro do ruído.
- **Matriz 1x1 16×16** (`g2a_matriz.md`): **idêntica** à `D_matriz.md` célula a célula. Confirma que só o 2x2 mudou.

Combos da bateria que caíram, conferidos rota a rota:
- **p08:** `max_0barras` 6846 → 4320 é efeito da busca em feixe. A rota antiga ("H 2L 2H A J jM jH j2H
  jH j2H 2M M S SUPER") **continua valendo 6846** no código novo; a busca só não passou por ela.
- **p16:** `max_1barra` 6707 → 5615 e `assist_2quiques` 6317 → 5777 também são da busca. A rota
  antiga dá **6848** no código novo.
- **p13:** `assist_2quiques` 6858 → 0 é **real**. O valor antigo era falso: o assist do p14 não
  acertava e a rota só o "chamava". Agora o mergulho derruba o rival e os dois quiques não fecham.
- **Subiram:**
  - p14: 5737 → 6783 sem barra;
  - p13: 7115 → 7543;
  - p11: 4407 → 4701.

### Testes G2a

- Novo `AssistCorpoACorpoTest` (5 testes):
  - p12–p15 acertam a 300 px;
  - o assist para a no máximo 220 px à frente do titular;
  - o mergulho entra alto, pega e sai;
  - o 2H da p15 lança;
  - o assist de projétil continua 70 px atrás.
- `TeamSystemTest.assistComesInHitsTheOpponentAndLeaves`: a posição esperada do assist de corpo a corpo (player_two, H) agora fica entre 70 px atrás e 220 px à frente do titular.
- Suíte inteira só com G2a: **546 de 546** passam (com os assets ligados à cópia).

## G2b: DHC

### Causa

O TAG no Super trocava no 1º quadro depois do startup.
- **p07:** o Super chama capangas, que batem 50–75 quadros depois. Trocando antes, o Super do p08
  congelava a tela e empurrava o rival, e os capangas erravam: 4545 → 2196.
- **Supers de várias pancadas** (Kōga 3, p02 5, p13 19): a troca cortava as pancadas que faltavam.

### O que mudou

- **DHC pedido** (`TeamSystem.dhcPending`, TeamSystem.java:51, 241–252): o TAG no Super já solto
  fica pedido. A troca sai quando as pancadas acabam (`superStrikesDone`, :298): passou a janela
  ativa ou bateu todas. O Super de projétil sai assim que solta, como antes.
- **Super de capangas** (`dhcHenchmen`, :53; `CombatEngine.henchmenStillToStrike`, :1476): o pedido
  espera os capangas baterem, mesmo com o Super acabado e o titular livre. Cai se o titular fizer
  outra coisa, apanhar ou perder a barra.
- Com o DHC pedido, outro TAG não chama assist por cima dele (:209). Antes, apertar de novo
  chamava o assist e derrubava o DHC.
- **O parceiro de Super corpo a corpo** entra até onde o Super dele pega o rival, no máximo
  `dhcMaxAdvance` = 420 px à frente (CombatEngine.java:2853; CombatConfig.java:447), sob o clarão
  do Super. Sem isso, o p08 entrava longe do rival que os capangas jogaram e errava o Super
  (medido: `DhcSemCortarTest` com 0 px, 0 acertos do p08).
- Docs: `docs/combat-engine.md`, seção DHC.

### T20: mesmo início (M H), 3 barras, boneco

| dupla | Super sozinho | Super + DHC antes | Super + DHC depois |
|---|---|---|---|
| Ryūga + Yin-lu | 4140 (7 ac.) | 4358 (9) | 4358 (9) |
| Kōga + p04 | 2221 (3) | 3276 (4) | **4504 (6)** |
| p05 + p06 | 1998 (3) | 1998 (3) | não sai no combo |
| p07 + p08 | 4545 (4) | **2196 (3)** | **4934 (5)** |
| p09 + p10 | 5168 (5) | 5968 (8) | 5968 (8) |
| p11 + p12 | 3312 (3) | 3910 (4) | 3910 (4) |
| p13 + p14 | 1693 (3) | 3758 (12) | não sai no combo |
| p15 + p16 | 1260 (2) | 1260 (2) | não sai no combo |

"Não sai no combo": nessas três duplas o Super do titular **erra** depois de M H (a "Super
sozinho" é só M + H). O DHC fica pedido até a janela do Super acabar e sai depois que o rival
já se soltou.
- p05+p06 e p15+p16: antes o DHC saía na hora e o Super do parceiro errava também (dano igual ao do Super).
- **p13+p14:** antes rendia 3758, porque o Super do p14 entrava na hora e pegava. Agora não rende.
  Essa é a única perda, e só existe porque o Super do p13 não combina de M H.

Dano por barra, nas 5 duplas em que o Super combina:
- Super: 3877 de média.
- Super + DHC: antes 3942 (a 2ª barra rendia **+64**), agora 4735 (**+858**).
- Ultra: 5234 por 3 barras.

### BateriaAntesDepois G2a → G2b (só o DHC)

Tudo dentro do ruído:
- SHINKAI 2x2 do ENERGY 48 → 52%;
- 2x2: p02 +4, p04 −4, p06 −5, p09 +4, p16 +3.

Breaker, Clash, TAG e combos iguais. Matriz 1x1 não muda (não há DHC no 1x1).

### Testes G2b

- Novo `DhcSemCortarTest` (4 testes):
  - p07+p08: os capangas batem antes da troca e o DHC soma ao Super;
  - Kōga+p04: as 3 pancadas do Kōga saem e o Super do p04 ainda pega;
  - TAG repetido com o DHC pedido não chama assist;
  - o p08 entra ao alcance: com `dhcMaxAdvance` 0, erra.
- Os dois primeiros **falham no código antigo** (rodei: "2 de 4 capangas", "2 de 3 pancadas").
- `TeamSuperTest` passa sem mudança (o Super do Ryūga é projétil: o DHC sai na hora, como antes).
- Suíte inteira G2a + G2b: **550 de 550** passam.

## DECIDIR

1. **p07 também mudou** (a regra vale para todo assist de corpo a corpo).
   - O tapa de luva agora vai a 450 px, com antiaéreo de 92%.
   - O p07 no 2x2 fica igual (31 → 30%), e ele continua somando +1919 ao combo do p08.
   - Se o p07 tiver de ficar como era, é uma exceção no pacote.
2. **p13 com H no lugar da investida.**
   - A investida era o golpe da identidade dele, mas dava +78 na defesa.
   - Alternativa: um golpe de assist próprio, por exemplo a investida só com a 1ª pancada.
     Precisa de dado novo no pacote, não fiz.
3. **p15 com 2H no lugar do M.** O `"move": "M"` estava escrito à mão no pacote, então alguém escolheu o M.
4. **`dhcMaxAdvance` 420 px.** O parceiro do DHC "teleporta" até o rival sob o clarão. É mudança
   de regra; com 0, o p08 erra o Super depois dos capangas.
5. **DHC ainda vale menos que guardar a barra.** A 2ª barra rende +858, contra 3042 de outro Super.
   - O parceiro entra sempre no piso de 50% (passo de 5% por acerto e −20% por repetir "SUPER").
   - Opções:
     - (a) piso próprio do DHC de 70%: estimativa de +1200 pela 2ª barra, ainda abaixo de um Super;
     - (b) DHC por meia barra;
     - (c) manter o DHC como troca segura e não como dano.
   - Não medi (a) nem (b).
6. **DHC nos Supers que não combinam** (p05, p13, p15 depois de M H): agora sai tarde, no fim da
   janela, e não serve de "cobertura" imediata. A troca é de regra; a causa real é a rota que não combina.

## Arquivos

- `android/app/src/main/java/com/gamelutagpt/CombatConfig.java`: `assistMeleeAhead`, `assistDiveHeight`, `dhcMaxAdvance`.
- `android/app/src/main/java/com/gamelutagpt/CombatEngine.java`: `assistAttack`, `moveAssist`, `assistAwaitsGround`, `henchmenStillToStrike`, posição do parceiro no `dhc`.
- `android/app/src/main/java/com/gamelutagpt/TeamSystem.java`: lugar do assist de corpo a corpo, mergulho, DHC pedido.
- `android/app/src/main/java/com/gamelutagpt/GeneratedCharacters.java`: golpe de assist do p13 e da p15.
- `characters/p13/character.json`, `characters/p15/character.json`.
- Testes: `AssistCorpoACorpoTest` (novo), `DhcSemCortarTest` (novo), `TeamSystemTest` (1 asserção).
- `docs/combat-engine.md`.
- Saídas das baterias em `scratchpad/g2/`:
  - `g2base_*`, `g2a_*`, `g2b_*`, `base_t17.md`;
  - `compara_out.md`, as tabelas de três colunas;
  - `ad/g2a.tsv`, `ad/g2b.tsv`.
