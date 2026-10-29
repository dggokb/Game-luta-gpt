# H1: Combo Breaker com atordoamento 4 (distância mantida), com a CPU nova

**O que foi medido:** a decisão 1 do usuário. O atordoamento passa de 20 para 4 quadros e a separação fica em 170 px, com a CPU da lista G. O troco foi separado em dois tipos:

- **garantido:** entra antes de quem batia ter um único quadro livre desde a quebra;
- **oportunista:** quem batia já podia defender ou fugir e não o fez.

**Base:** `5dedf956`, código igual ao `29ca0716`. As sementes e os cenários são os mesmos. A mudança foi feita numa cópia (`scratchpad/h1`), sem commit; o diff está em `scratchpad/bal/H1.diff`.

## Mudança

| arquivo | linha | o quê |
|---|---|---|
| `CombatConfig.java` | 422–428 | `comboBreakerStunFrames` 20 → 4 (o comentário explica) |
| `CombatEngine.java` | 2032–2035 (`comboBreaker`) | `a.buffer.clear()`: o golpe que quem batia deixou no buffer para fechar o combo não sai sozinho depois do atordoamento curto |
| `ComboBreakerTest.java` | 129–214 | 2 testes novos (abaixo); o teste da CPU que vai atrás depois da quebra passa a esperar 120 quadros em vez de 60 |
| `BateriaQuebra.java` | `Caso` (~80), `caso()` (~150), `garantia()` (447–520), `main` (523–540) | classifica garantido × oportunista no roteiro e na CPU livre; modo `opcoes` para medir outras regras |

O `buffer.clear()` era necessário. Sem ele, o H que o Ryūga apertou para fechar o combo saía sozinho no vazio, 5 quadros depois da quebra, e quem quebrou punia de graça. Com 20 quadros o buffer já tinha vencido, e com 4 não tinha. O teste novo pega esse caso: falha sem o `buffer.clear()` (player_base H) e passa com ele.

## Testes

| teste | resultado |
|---|---|
| `afterTheBreakerTheBreakerActsStunPlusOneFramesFirst` (novo): quem quebrou age `stun + 1` quadros antes | passa: +5 |
| `aNormalSlowerThanTheBreakerAdvantageNeverLandsGuaranteed` (novo): os 16, L/2L/M/2M/H/2H com mais de 5 quadros de início, correndo até alcançar, contra quem defende assim que pode | passa: nenhum entra |
| suíte do app | 562 de 562 (era 560, mais os 2 novos) |

O teste que o pedido sugeria, "nenhum normal entra garantido com 4 quadros", **não passa**, e o motivo é real. Quem quebrou age 5 quadros antes, então todo normal que sai em até 5 quadros e alcança a 170 px entra garantido: o L de quase todos e alguns M. Por isso o teste passou a cobrar o que a regra garante de fato.

## Garantido × oportunista

O roteiro: 16 × 16 pares, 2 sementes. Quem quebrou tenta cada troco, correndo até alcançar, e quem batia defende perfeito assim que pode. "CPU livre" são as duas CPUs nível 8 jogando 3 s depois da quebra, e a tabela mostra quem acerta primeiro.

### meio

| opção | quebras | roteiro, garantido: L | M | H | S1 | S2 | S3 | S4 | algum garantido | CPU livre: quem quebrou acerta primeiro, garantido | oportunista | quem batia acerta primeiro | ninguém em 3 s |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| antes: 170 px e 20 q | 341 | 89% | 86% | 58% | 79% | 44% | 38% | 75% | 96% | 68% | 20% | 11% | 1% |
| H1: 170 px e 4 q | 341 | 33% | 1% | 1% | 2% | 0% | 0% | 0% | 33% | 1% | 60% | 33% | 6% |
| E2: 170 px e 2 q | 341 | 2% | 1% | 1% | 2% | 0% | 0% | 0% | 2% | 0% | 64% | 30% | 6% |
| E1: 170 px e 0 q | 341 | 2% | 1% | 1% | 1% | 0% | 0% | 0% | 2% | 0% | 62% | 33% | 5% |

### quem quebra no canto

| opção | quebras | roteiro, garantido: L | M | H | S1 | S2 | S3 | S4 | algum garantido | CPU livre: quem quebrou acerta primeiro, garantido | oportunista | quem batia acerta primeiro | ninguém em 3 s |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| antes: 170 px e 20 q | 403 | 91% | 85% | 60% | 82% | 47% | 37% | 75% | 98% | 70% | 17% | 11% | 1% |
| H1: 170 px e 4 q | 403 | 45% | 1% | 1% | 2% | 0% | 0% | 0% | 45% | 0% | 61% | 35% | 4% |
| E2: 170 px e 2 q | 403 | 3% | 1% | 1% | 2% | 0% | 0% | 0% | 3% | 0% | 64% | 33% | 3% |
| E1: 170 px e 0 q | 403 | 3% | 1% | 1% | 2% | 0% | 0% | 0% | 3% | 0% | 65% | 30% | 4% |

### quem bate de costas para a parede

| opção | quebras | roteiro, garantido: L | M | H | S1 | S2 | S3 | S4 | algum garantido | CPU livre: quem quebrou acerta primeiro, garantido | oportunista | quem batia acerta primeiro | ninguém em 3 s |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| antes: 170 px e 20 q | 433 | 95% | 89% | 64% | 81% | 46% | 33% | 73% | 98% | 65% | 23% | 10% | 2% |
| H1: 170 px e 4 q | 433 | 26% | 1% | 0% | 1% | 0% | 0% | 0% | 26% | 1% | 63% | 33% | 2% |
| E2: 170 px e 2 q | 433 | 1% | 1% | 0% | 1% | 0% | 0% | 0% | 1% | 0% | 68% | 29% | 2% |
| E1: 170 px e 0 q | 433 | 1% | 1% | 0% | 1% | 0% | 0% | 0% | 1% | 0% | 69% | 27% | 3% |

**Golpes garantidos da CPU no meio** (CPU livre, 341 quebras):

| regra | golpes |
|---|---|
| antes | M 108, 2M 45, 2L 42, L 37 |
| H1 | 2L 3 |
| E2 | nenhum |

**Leitura:**

- **No roteiro:** com 4 quadros o troco garantido cai de 96% para 33% no meio (45% no canto e 26% de costas para a parede). O que sobra é praticamente só o L e o 2L: M, H, S1 e S4 ficam em 0–2%.
- **Na luta real** (CPU livre): o primeiro acerto garantido de quem quebrou cai de 68% para 1%.
  - O troco que sobra é oportunista: 60%, contra 20% antes, e a CPU que batia não defendeu.
  - Quem batia acerta primeiro 33% das vezes, contra 11% antes.
- **Saldo de 3 s para quem quebrou:**

  | posição | antes | H1 |
  |---|---|---|
  | meio | +712 | +213 |
  | quem quebrou no canto | +696 | +176 |

  Quem quebrou preso no canto: 69%, igual.
- **Troco da CPU no grupo breaker:** 126 trocos de 937 em média, contra 181 de 579. Ele fica mais raro e vem de golpe escolhido (S, H, S4), não do 2M grátis.

### Por personagem (meio)

| personagem | quebrando: troco garantido (roteiro), antes → H1 → E2 | quebrando: acerto oportunista da CPU, antes → H1 | batendo: toma garantido, antes → H1 → E2 |
|---|---|---|---|
| Ryūga (p01) | 95% → 45% → 0% | 80% → 80% | 100% → 17% → 0% |
| Yin-lu (p02) | 95% → 40% → 5% | 15% → 55% | 100% → 12% → 0% |
| Kōga (p03) | 96% → 39% → 0% | 22% → 83% | 100% → 58% → 0% |
| p04 | 100% → 42% → 0% | 16% → 68% | 97% → 19% → 13% |
| p05 | 100% → 33% → 0% | 5% → 62% | 100% → 35% → 0% |
| p06 | 100% → 54% → 0% | 13% → 42% | 73% → 15% → 0% |
| p07 | 91% → 5% → 5% | 23% → 64% | 100% → 50% → 0% |
| p08 | 100% → 29% → 10% | 19% → 81% | 90% → 0% → 0% |
| p09 | 95% → 0% → 0% | 9% → 41% | 100% → 29% → 0% |
| p10 | 100% → 41% → 5% | 5% → 64% | 100% → 31% → 3% |
| p11 | 100% → 68% → 5% | 26% → 32% | 0% → 0% → 0% |
| p12 | 95% → 80% → 0% | 15% → 75% | 97% → 41% → 0% |
| p13 | 80% → 0% → 0% | 25% → 55% | 100% → 75% → 0% |
| p14 | 100% → 0% → 0% | 9% → 73% | 97% → 26% → 0% |
| p15 | 100% → 23% → 0% | 27% → 36% | 100% → 30% → 10% |
| p16 | 96% → 42% → 0% | 13% → 58% | 94% → 44% → 0% |

O que ainda entra garantido com H1 é o L e o 2L na ponta, de quem tem L longo:

- p12 80%, p11 68%, p06 54% e Ryūga 45%;
- p09, p13 e p14 ficam em 0%, porque o L deles é lento ou curto.

Quem mais toma, quando é quem bate: p13 75%, Kōga 58% e p07 50%. Os três têm o corpo largo ou ficam mais perto depois do empurrão.

## Bateria fixa (`BateriaAntesDepois`), base → H1

Só as linhas que mudaram além do ruído. O grupo combos não mudou: 0 linhas.

| métrica | base | H1 |
|---|---|---|
| breaker.troco_garantido_script | 57% | 2% | *
| breaker.troco | 521 | 638 | *
| breaker.sempre_vs_nunca | 66% | 59% | *
| breaker.cpu_vs_nunca | 57% | 52% | *
| breaker.cpu_quebra_das_chances | 37% | 32% | *
| shinkai.2x2_vence.ENERGY | 49% | 45% | *
| 1x1.player_base | 64% | 67% | *
| 2x2.player_base | 60% | 65% | *
| 2x2.p02 | 45% | 52% | *
| 2x2.p03 | 46% | 50% | *
| 2x2.p07 | 41% | 36% | *
| 2x2.p10 | 50% | 46% | *
| 2x2.p16 | 31% | 42% | *
| p09.vs_p02_400 | 69% | 65% | *

- `troco_garantido_script` cai de 57% para 2%. É o roteiro da etapa 1: Ryūga quebra e corre com L, 2L, M ou 2M contra cada atacante.
- **"Sempre quebra vence nunca":** 66% → 59%. A quebra continua valendo a barra, mas deixa de ser um combo de graça.
- **1x1 e 2x2 por personagem:** tudo dentro do ruído de ±8. A maior mudança é a p16 no 2x2 (+11) e o p07 (−5).

## T12 e T21 (`BateriaEquipe`), base → H1

### T12 Combo Breaker

##### Altura do combo e lugar (16 atacantes × 10 sementes; boneco com 1 barra, sem defender)

| quebra | lugar | quebras | levou antes | dano evitado | troco | troco ≥ 1 combo de 3 acertos |
|---|---|---|---|---|---|---|
| no 2º acerto | meio | 158 | 761 | 1016 | 537 → 617 | 8% → 34% |
| no 3º acerto | meio | 144 | 1273 | 556 | 695 → 515 | 22% |
| no 4º acerto | meio | 92 | 1688 | 316 | 574 → 442 | 18% |
| no ar | meio | 136 | 1423 | 423 | 725 → 460 | 29% → 17% |
| no 2º acerto | canto | 123 | 871 | 1026 | 590 → 749 | 11% → 33% |
| no ar | canto | 86 | 1395 | 766 | 699 → 529 | 22% |

##### A/B: regra atual × "volta ao neutro" (lutas 2x2 espelhadas, 240 por linha)

| regra | atordoamento | corrida automática | troco médio | sempre quebra vence nunca quebra |
|---|---|---|---|---|
| atual | 44 quadros | 40 quadros | 1188 | 70% |
| meio-termo | 24 quadros | não | 551 | 66% |
| volta ao neutro | 12 quadros | não | 413 | 66% |

Controle (nunca × nunca, mesmas sementes): 50%.

### T21 Sinergia das duplas oficiais

Cada titular com o parceiro oficial e com três alternativos, contra as outras sete duplas oficiais
(CPU nível 8 × CPU nível 8, 2x2, 8 lutas por adversário, metade de cada lado).

| titular | oficial | vitórias | alternativo 1 | alternativo 2 | alternativo 3 | melhor parceiro |
|---|---|---|---|---|---|---|
| Ryūga (p01) | Yin-lu (p02) | 64% → 63% | p04 68% | p07 55% → p07 66% | p12 75% → p12 82% | p12 |
| Kōga (p03) | p04 | 55% → 61% | p06 54% → p06 59% | p09 63% → p09 68% | p14 41% → p14 36% | p09 |
| p05 | p06 | 36% → 43% | p08 68% | p11 50% → p11 45% | p16 34% → p16 27% | p08 |
| p07 | p08 | 63% → 57% | p10 41% → p10 43% | p13 48% → p13 54% | Yin-lu (p02) 34% → Yin-lu (p02) 45% | oficial |
| p09 | p10 | 79% → 71% | p12 71% → p12 63% | p15 34% → p15 41% | p04 84% → p04 71% | p04 → oficial |
| p11 | p12 | 64% → 54% | p14 41% → p14 54% | Ryūga (p01) 68% → Ryūga (p01) 66% | p06 54% → p06 46% | Ryūga (p01) |
| p13 | p14 | 39% → 36% | p16 30% → p16 27% | Kōga (p03) 70% → Kōga (p03) 57% | p08 68% → p08 64% | Kōga (p03) → p08 |
| p15 | p16 | 2% | Yin-lu (p02) 11% → Yin-lu (p02) 14% | p05 9% → p05 14% | p10 16% → p10 4% | p10 → Yin-lu (p02) |



- **T12:** no boneco que não defende, o troco depois da quebra no 2º acerto sobe de 537 para 617, e "troco ≥ 1 combo de 3" sobe de 8% para 34%. Isso é acerto **oportunista**: o boneco não defende. Quebrando no 3º acerto, no 4º e no ar, o troco cai, porque quem batia já está livre quando quem quebrou chega.
- **T21:** as oito duplas oficiais mexem entre −10 e +7, dentro do ruído do 2x2 (56 lutas por linha, cerca de ±13):

  | dupla | base → H1 |
  |---|---|
  | p11+p12 | 64 → 54 |
  | p09+p10 | 79 → 71 |
  | p05+p06 | 36 → 43 |
  | Kōga+p04 | 55 → 61 |
  | p07+p08 | 63 → 57 |
  | Ryūga+Yin-lu | 64 → 63 |
  | p13+p14 | 39 → 36 |
  | p15+p16 | 2 → 2 |

  Nenhuma dupla depende do Breaker garantido.

## Matriz 16 × 16 (nível 8, 10 sementes por célula), força contra o elenco

| personagem | base (5dedf956) | H1 | Δ |
|---|---|---|---|
| p08 | 78% | 79% | +1 |
| p11 | 63% | 67% | +4 |
| p09 | 64% | 64% | +0 |
| Kōga (p03) | 58% | 62% | +4 |
| p04 | 64% | 61% | -3 |
| Ryūga (p01) | 61% | 61% | +0 |
| p10 | 63% | 57% | -6 |
| p13 | 55% | 56% | +1 |
| p05 | 51% | 50% | -1 |
| p06 | 51% | 50% | -1 |
| Yin-lu (p02) | 45% | 47% | +2 |
| p12 | 47% | 43% | -4 |
| p14 | 31% | 33% | +2 |
| p07 | 32% | 30% | -2 |
| p16 | 24% | 27% | +3 |
| p15 | 13% | 11% | -2 |

Faixa: base 13–78%, H1 11–79%. Desvio padrão: 16.8 → 16.9.

Todos ficam dentro do ruído de ±8. Viés de lado: 51%, espelhos 59% (95 de 160).

## Ainda há troco garantido: próximo experimento (medido, não aplicado)

O L e o 2L ainda entram garantidos em 26–45% das quebras do roteiro. A causa é a vantagem de +5 quadros: os dois ficam congelados os mesmos 16 quadros, quem quebrou fica livre na hora e quem batia fica mais `stun` quadros. Medi o atordoamento de 0 e de 2 quadros e a separação de 220 e de 260 px, todos na mesma bateria:

| regra | garantido (roteiro), meio / canto / de costas | CPU livre: garantido / oportunista / quem batia primeiro (meio) | saldo de 3 s, meio / canto | troco da CPU no grupo breaker |
|---|---|---|---|---|
| antes, 170 px 20 q | 96 / 98 / 98% | 68 / 20 / 11% | +712 / +696 | 181 × 579 |
| **H1, 170 px 4 q** | 33 / 45 / 26% | 1 / 60 / 33% | +213 / +176 | 126 × 937 |
| **E2, 170 px 2 q** | **2 / 3 / 1%** | 0 / 64 / 30% | +269 / +284 | 131 × 956 |
| E1, 170 px 0 q | 2 / 3 / 1% | 0 / 62 / 33% | +241 / +282 | 137 × 1048 |
| E3, 220 px 4 q | 26 / 35 / 19% | 0 / 49 / 45% | +37 / −144 | — |
| E4, 260 px 4 q | 18 / 27 / 13% | 0 / 47 / 48% | −2 / −62 | — |

**Proposta: E2, atordoamento 2 e 170 px.**

- Com +3 quadros quase nada sai a tempo: a 170 px nenhum L alcança parado, e correndo até alcançar quem batia já está livre para defender. O L de 2 quadros do Ryūga também fica em 0%. O garantido cai para 1–3%: sobram 1 L de quem bate de costas para a parede e o 1% de M, H e S1 que é ruído do roteiro.
- Quem quebrou continua saindo na frente: o saldo fica em +269 no meio, até um pouco melhor que o H1, porque a CPU livre acerta primeiro 64% das vezes.
- Separar mais (E3, E4) não resolve: o L ainda alcança correndo, e quem batia ganha o neutro, com saldo de −144 no canto.
- O E1 (0 quadros) dá o mesmo que o E2. O E2 fica com 2 quadros para quem batia ainda parecer "tonto" no desenho.

Para aplicar: `comboBreakerStunFrames = 2`. O teste `aNormalSlower…` passa a conferir todos os normais com mais de 3 quadros de início.

## Arquivos

- `scratchpad/bal/H1.diff`: aplica limpo no `5dedf956` (`git apply --check` OK).
- `scratchpad/h1r/`, com os dados:
  - `quebra.md`: garantia e trococpu de antes, H1, E2 e E1;
  - `propostas.md`: E1–E4 com o canto;
  - `ad_cmp.md`, `equipe_cmp.md`, `matriz.md`, `forca.md`;
  - `porpersonagem.md`.
- `scratchpad/ad/h1.tsv`: a `BateriaAntesDepois` com H1.
