# Frente G3: Breaker com separação de 320 px e busca de combos com S2–S4

HEAD medido: `5cd8ed16`, com as classes base copiadas do `:pc` (`scratchpad/g3base`). Só ferramentas de teste mudaram: nenhum arquivo em `app/src/main` foi alterado. A separação (`comboBreakerPushGap`) e o atordoamento (`comboBreakerStunFrames`) do Breaker já eram campos da `CombatConfig`. As opções são aplicadas só na bateria, por um gancho novo.

Tabelas completas:
- `scratchpad/g3/quebra.md`: Breaker;
- `scratchpad/g3/curva.md`: outras combinações de separação e atordoamento;
- `scratchpad/g3/combos_g3.md`: combos, 128 cenários;
- `scratchpad/g3/combos_amp.tsv`: grupo combos antigo e ampliado.

## G3a: Breaker, três opções com as mesmas sementes da etapa 1

- **C (hoje):** 170 px e 20 quadros, a etapa 1.
- **A:** 320 px e 20 quadros.
- **B:** 170 px e 4 quadros.

### Grupo breaker da `BateriaAntesDepois` (sementes da etapa 1)

| medida | C: hoje | A: 320 px | B: 4 q |
|---|---|---|---|
| quebras medidas | 111 | 111 | 111 |
| troco garantido no roteiro da etapa 1 (Ryūga corre e aperta L/2L/M/2M) | **69%** | **1%** | **1%** |
| combo inteiro / dano evitado pela quebra | 1627 / 832 | 1627 / 832 | 1627 / 832 |
| troco da CPU (nível 8) por quebra | 553 | **830** | 573 |
| "sempre quebra" vence "nunca quebra" | 57% | 59% | 58% |
| CPU vence "nunca quebra" | 49% | 52% | 47% |
| quebras da CPU por luta | 0,76 | 0,81 | 0,81 |
| CPU quebra das chances | 29% | 33% | 27% |

- O C confere com a etapa 1: troco 551, e "sempre quebra × nunca" entre 58 e 61%.
- As vitórias variam 2 a 3 pontos, dentro do ruído de ±5.
- **Valor da quebra**, contando o dano evitado mais o troco médio da CPU:
  - para quem quebra: 1385 no C, 1662 no A, 1405 no B;
  - quem tomou a quebra perde o mesmo valor.

**Por que o troco da CPU sobe com 320 px.** O golpe que abre o troco muda (`BateriaQuebra trococpu`, cada atacante contra Ryūga, sementes 1–16):

| opção | trocos | dano médio | golpe que abre |
|---|---|---|---|
| C | 158 | 596 | 2M 136 vezes (556), H 10 (961), S1 9 (774) |
| A | 144 | 916 | H 71 (919), S1 28 (808), 2M 13 (500), 2L 10 (1767) |
| B | 84 | 994 | S1 35 (945), H 28 (1097), L 7 (994) |

- No C, a CPU quase sempre abre com o 2M, que derruba e não emenda.
- Longe (A) ou sem vantagem de quadros (B), ela usa o H ou o S1, que emendam.
- No B a CPU troca menos vezes (84 contra 158), mas cada troco rende mais.

### Troco grátis por golpe

Montagem do teste:
- 16 × 16 pares (quem bate × quem quebra), 2 sementes.
- Quem quebrou tenta um golpe. L, M e H usam o comum ou o agachado, o que alcançar, correndo até alcançar. Os especiais são apertados assim que dá para agir.
- Quem batia defende perfeito assim que pode.
- "Qualquer" significa que algum dos sete golpes entra.
- A vantagem é medida em quadros. A distância é entre os centros: no quadro em que quem quebrou pode agir → no quadro em que quem batia pode agir.

| posição | opção | L | M | H | S1 | S2 | S3 | S4 | qualquer | vantagem | distância |
|---|---|---|---|---|---|---|---|---|---|---|---|
| meio | C | 95% | 91% | 59% | 81% | 49% | 36% | 75% | **99%** | +21 q | 171 → 239 px |
| meio | A | 15% | 8% | 6% | 18% | 18% | 17% | 24% | **59%** | +21 q | 171 → 387 px |
| meio | B | 38% | 0% | 0% | 2% | 0% | 0% | 0% | **39%** | +5 q | 171 → 205 px |
| quem quebra no canto | C | 94% | 87% | 59% | 82% | 47% | 36% | 75% | 100% | +21 q | 155 → 239 px |
| quem quebra no canto | A | 20% | 11% | 7% | 20% | 16% | 16% | 23% | 60% | +21 q | 155 → 383 px |
| quem quebra no canto | B | 48% | 1% | 1% | 3% | 0% | 0% | 0% | 48% | +5 q | 155 → 197 px |
| quem bate de costas para a parede | C | 91% | 88% | 54% | 83% | 45% | 29% | 68% | 100% | +21 q | 180 → 239 px |
| quem bate de costas para a parede | A | 43% | 36% | 21% | 44% | 24% | 18% | 35% | **78%** | +21 q | 180 → 330 px |
| quem bate de costas para a parede | B | 34% | 2% | 1% | 3% | 0% | 0% | 0% | 36% | +5 q | 180 → 209 px |

O empurrão só anda depois da pausa da quebra. Quem quebrou já pode agir no quadro 15, ainda a ~171 px. Quem batia volta no quadro 36 (16 de pausa + 20 de atordoamento) no A e no C, ou no quadro 20 no B.

**Curva** (meio, mesmas sementes, `scratchpad/g3/curva.md`):

| separação / atordoamento | qualquer | só L | vantagem |
|---|---|---|---|
| 170 px / 0 q | 2% | 1% | +1 q |
| 170 px / 4 q (B) | 39% | 38% | +5 q |
| 170 px / 8 q | 83% | 70% | +9 q |
| 170 px / 12 q | 95% | 85% | +13 q |
| 245 px / 8 q | 53% | 38% | +9 q |
| 245 px / 20 q | 92% | 64% | +21 q |
| 320 px / 8 q | 24% | 14% | +9 q |
| 320 px / 20 q (A) | 59% | 15% | +21 q |

### Por personagem, no meio (canto em `quebra.md`)

"Qualquer (L)" é a fração de quebras em que esse personagem, quebrando, acerta algum troco grátis (entre parênteses, só o L). "Leva tomando" é a fração de quebras sofridas por ele, como atacante, em que entra algum troco grátis. A amostra é pequena para p08 (2 quebras) e para p11 e p15 (4).

| personagem | C | A | B | leva tomando: C / A / B |
|---|---|---|---|---|
| Ryūga (p01) | 100% (100%) | 15% (15%) | 45% (45%) | 100% / 78% / 22% |
| Yin-lu (p02) | 100% (94%) | 19% (19%) | 44% (44%) | 100% / 47% / 5% |
| Kōga (p03) | 100% (100%) | 47% (18%) | 59% (59%) | 92% / 62% / 50% |
| p04 | 100% (100%) | 61% (17%) | 56% (56%) | 100% / 59% / 45% |
| p05 | 100% (100%) | **100%** (0%) | 58% (58%) | 95% / 58% / 47% |
| p06 | 100% (94%) | 88% (6%) | 53% (35%) | 100% / 29% / 29% |
| p07 | 91% (64%) | 59% (5%) | 0% (0%) | 100% / 42% / 42% |
| p08 | 100% (94%) | **100%** (0%) | 35% (35%) | 100% / 0% / 0% |
| p09 | 100% (100%) | 76% (0%) | 0% (0%) | 100% / 56% / 13% |
| p10 | 100% (100%) | **100%** (18%) | 59% (59%) | 100% / 38% / 6% |
| p11 | 94% (88%) | 75% (13%) | 63% (63%) | 100% / 50% / 50% |
| p12 | 100% (100%) | 71% (67%) | **90%** (90%) | 100% / 53% / 50% |
| p13 | 100% (95%) | 24% (24%) | 0% (0%) | 100% / 100% / 75% |
| p14 | 100% (100%) | 86% (5%) | 0% (0%) | 100% / 76% / 52% |
| p15 | 100% (94%) | 11% (11%) | 33% (33%) | 100% / 25% / 75% |
| p16 | 100% (100%) | 17% (17%) | 56% (56%) | 100% / 58% / 50% |

No A, quem tem projétil ou avanço longo segue com troco grátis quase sempre:
- p05: S1, S2 e S3 (shuriken);
- p08: H e S2;
- p10: S1, M e S4;
- p14: S2 e S3;
- p06: S3 e S4;
- p09: S4.

Os outros quase não têm troco. O A troca um troco universal por um troco que depende do personagem.

### A troca: o atacante ainda alcança com projétil ou avanço?

Quem quebrou corre com o L. Quem batia, assim que pode agir, solta o S1 ou corre com o L. A tabela mostra quem acerta primeiro sem defesa (meio, 512 casos).

| opção | atacante solta o S1: quem quebrou acerta antes | o atacante acerta antes | atacante corre com o L: quem quebrou acerta antes | o atacante acerta antes |
|---|---|---|---|---|
| C | 98% | 1% | 97% | 2% |
| A | 70% | **29%** | 56% | **43%** |
| B | 98% | 1% | 95% | 4% |

- No A, o projétil ou a corrida do atacante pega quem vem correndo em 29–43% das vezes, porque a distância de 387 px dá tempo.
- No B, quem batia deveria defender em vez de atacar: com +5 q para quem quebrou, o L de perto ainda chega antes.

### 3 s depois da quebra (CPU × CPU) e o canto

As duas CPUs (nível 8) jogam livres por 180 quadros depois da quebra.

| opção | posição | dano de quem quebrou | dano de quem batia | saldo de quem quebrou | quem quebrou ainda a < 150 px da parede |
|---|---|---|---|---|---|
| C | meio | 960 | 270 | +690 | 6% |
| C | quem quebra no canto | 967 | 277 | +690 | 64% |
| A | meio | 901 | 370 | +531 | 5% |
| A | quem quebra no canto | 921 | 378 | +543 | **36%** |
| B | meio | 720 | 516 | +204 | 6% |
| B | quem quebra no canto | 665 | 583 | +82 | **71%** |

O A tira quem quebrou do canto: o outro sai a ~383 px e quem quebrou anda para fora. No B, quem quebrou continua encurralado, como hoje, mas sem a vantagem de 21 quadros.

### Recomendação: B (170 px, atordoamento 4). Não apliquei no jogo; a decisão é sua.

**Por que B.**
- É o que mais devolve a luta ao neutro, que era o pedido do P0.
  - O troco garantido do roteiro da etapa 1 cai de 69% para 1%.
  - Qualquer troco grátis vai de 99% para 39%. O que sobra é só o L de perto, com +5 quadros, que exige reação.
  - Nenhum especial ou golpe forte entra de graça.
  - O saldo dos 3 s seguintes cai de +690 para +204.
- A quebra ainda vale a barra: 832 de dano evitado mais 573 de troco médio da CPU.

**Por que não A.**
- Mantém +21 quadros de vantagem.
- Torna o troco dependente do personagem: 100% para p05, p08 e p10, 11–19% para Ryūga, Yin-lu, p15 e p16.
- Com o quem bate de costas para a parede, o empurrão bate na parede e o troco volta a 78%.
- Aumenta o troco da CPU de 553 para 830.
- Ganho real do A: tira quem quebrou do canto (64% → 36%) e dá ao atacante uma chance de zonear (29–43%).

**Pontos para decidir junto.**
- Para nenhum troco grátis, o atordoamento 0 dá 2% (+1 q). Entre 0 e 4 q é questão de gosto: quanto prêmio quem quebra deve ter.
- No canto, o B deixa quem quebrou encurralado, 71% contra 64% hoje. Se isso incomodar, um meio-termo medido é 320 px com 8 q: 24% de troco grátis, +9 q, distância de 365 px. Esse meio-termo também tiraria quem quebrou do canto. Não medi o canto nem a troca nessa combinação.
- Com o B, quatro personagens ficam sem troco nenhum: p07, p09, p13 e p14, cujo L não alcança a 205 px. O p12 fica com 90%. Vale olhar o alcance do L deles junto com a correção do alcance da CPU.

## G3b: busca de combos com S1 fraco/forte e S2–S4

### O que mudou na ferramenta (só testes)

- **Ações novas:** `S1L`/`S1H` (o S1 fraco e o forte) e `S2`, `S3`, `S4`.
  - O S1 fraco e o forte entram para todos, porque o S1 de todos é projétil: o botão muda a velocidade e o dano. O forte dá ×1,45 de dano.
  - `S2L`/`S2H` só onde o botão muda o golpe: a pedra do p13 e a caixa da p16.
- **Como o especial é apertado:** o especial de comando é posto no buffer como o comando completo faria, com o botão que escolhe a força (`BateriaCombos.apertaEspecial`).
  - O executor não confere se o comando cabia na rota.
  - Por isso os especiais de carga ([←] →, 30 quadros) estão marcados "(carga)" no relatório: p02 S2/S4, p04 S2, p08 S2, p09 S4, p11 S4 e p12 S2. Rotas com dash ou pulo para frente antes de um deles não são possíveis para um humano.
- **Ultra:** não se aplica com 0 ou 1 barra; ele precisa de 3.
- **Cenários:** meio e canto, 0 e 1 barra, com e sem assist. São 8 por personagem, 128 no total, começando a 140 px.
  - O boneco tem vida infinita e cai de pé no ar.
  - A busca usa larguras 40 e 80 e fica com a melhor.
- **Colunas:**
  - a ampliada nunca fica abaixo da antiga: ela parte da melhor antiga;
  - a coluna "+ S2–S4" não inclui o S1 fraco e o forte, para separar o ganho de cada um.

### Grupo combos da `BateriaAntesDepois`, as duas colunas

O grupo `combos` continua igual: as 61 chaves batem com `ad/5_full.tsv` e `ad/6_head.tsv`. O grupo novo `combos_amp` dá as mesmas três medidas com a busca ampliada (a melhor entre a antiga e a ampliada):

| lutador | maior 0 barra: antiga | ampliada | maior 1 barra: antiga | ampliada | assist + 2 quiques, 0 barra: antiga | ampliada |
|---|---|---|---|---|---|---|
| Ryūga (p01) | 7390 | 8189 (+799) | 8465 | 8936 (+471) | 6039 | 6039 |
| Yin-lu (p02) | 4798 | 4974 (+176) | 4288 | 4699 (+411) | 6830 | 6830 |
| Kōga (p03) | 5718 | 5718 | 4669 | 4850 (+181) | 6789 | 6789 |
| p04 | 5552 | 6381 (+829) | 5408 | 5986 (+578) | 7298 | 7298 |
| p05 | 6041 | 6041 | 4629 | 5317 (+688) | 5736 | 5736 |
| p06 | 6005 | 6005 | 3922 | 4686 (+764) | 5509 | 6168 (+659) |
| p07 | 7385 | 7385 | 6481 | 7836 (+1355) | 7064 | 7064 |
| p08 | 6846 | 6846 | 4339 | 5182 (+843) | 0 | 0 |
| p09 | 6924 | 8074 (+1150) | 7906 | 9525 (+1619) | 0 | 0 |
| p10 | 6407 | 6407 | 4730 | 5048 (+318) | 6376 | 6376 |
| p11 | 4407 | 5194 (+787) | 6219 | 7027 (+808) | 0 | 0 |
| p12 | 5926 | 6041 (+115) | 5751 | 5751 | 7521 | 8006 (+485) |
| p13 | 7115 | 7115 | 7115 | 7115 | 6858 | 6858 |
| p14 | 5737 | 5807 (+70) | 5649 | 7066 (+1417) | 5737 | 6748 (+1011) |
| p15 | 5700 | 5700 | 3840 | 4760 (+920) | 0 | 0 |
| p16 | 6707 | 6707 | 6707 | 6707 | 6317 | 6317 |

Para comparar com as etapas anteriores, use a coluna antiga. Daqui em diante, a ampliada é a que mostra o teto real.

### Por personagem (8 cenários; detalhe e rotas em `combos_g3.md`)

| lutador | cenários com rota nova (melhor e com especial novo) | maior ganho | especiais novos usados |
|---|---|---|---|
| Ryūga (p01) | 6 de 8 | +1119 | S1H, S2, S4 |
| Yin-lu (p02) | 6 de 8 | +411 | S1H, S2 (carga), S3, S4 (carga) |
| Kōga (p03) | 3 de 8 | +1071 | S1H, S4 |
| p04 | 6 de 8 | +933 | S1H, S2 (carga), S3 |
| p05 | 4 de 8 | +314 | S1H, S2, S3 |
| p06 | 6 de 8 | +936 | S1H, S2, S3 |
| p07 | 5 de 8 | +1390 | S2, S3, S4 |
| p08 | 7 de 8 | +1076 | S1H, S2 (carga), S3, S4 |
| p09 | 6 de 8 | +1410 | S1H, S2, S3, S4 (carga) |
| p10 | 3 de 8 | +644 | S1H, S2, S3, S4 |
| p11 | 4 de 8 | +1048 | S1H, S2, S3, S4 (carga) |
| p12 | 5 de 8 | +657 | S2 (carga), S4 |
| p13 | 4 de 8 | +830 | S2 |
| p14 | 7 de 8 | +1402 | S2, S3, S4 |
| p15 | 4 de 8 | +996 | S1H, S4 |
| p16 | 5 de 8 | +776 | S1H, S2 |

O "Δ total" de uma linha pode incluir ruído da busca: em p07, canto, 0 barra, +295 vêm de uma rota sem especial novo. A coluna "+ S2–S4" isola o ganho dos especiais de comando.

### O que muda por personagem

**O S1 forte é a peça nova mais usada.** Ele dá ×1,45 de dano e vale para um humano, que pode apertar H no S1. A busca antiga só apertava o S1 médio, então o teto de dano estava subestimado em até ~800. No Ryūga com 0 barra, +799 vêm quase todos do S1H.

**Quem ganha com S2–S4** (coluna "+ S2–S4"):
- **p14:** a maior mudança.
  - Pisão de onda (S4) como fecho e de novo depois do Super: +1402 com 1 barra, no meio.
  - Meia-lua cortante (S3) no meio da rota: +1234 com 0 barra.
- **p09:** a Ombrada (S4) depois do Super, com carga: +1193 no meio com 1 barra. Se a carga não couber, o ganho real é menor.
- **p07:**
  - Contra-golpe (S3) abre a rota e acerta como golpe comum, sem precisar de um ataque do rival: +1042 com 1 barra.
  - Chute ascendente (S2) no canto: +1390.
- **p08:** Contra-golpe (S3) e Giro ascendente (S2, carga): +1076 sem assist.
- **p13:** a pedra (S2) abre e emenda no canto: +713 a +830. É a única identidade nova dele.
- **Ryūga:** Punho ascendente (S2) emenda no Super e Chute giratório (S4) aparece no canto. O **Aparar (S3) nunca entra** em rota nenhuma.
- **p15:** só a Voadora e chute girando (S4) entra, como fecho: +692 com 1 barra. O Repel (S2) e a Rajada de punhos (S3) nunca aparecem. A "identidade de karatê" continua sem combo próprio: a rota da p15 segue o esqueleto comum (pulo jM jH j2H) mais o S4 no fim.
- **Contra-golpes como golpe comum:** p02 S3, p04 S3, p05 S3 e p06 S3 entram como golpes no meio da rota. É o mesmo achado da frente D sobre o "Aparar" do Ryūga: os contra-golpes e esquivas acertam um boneco que não ataca.
- **p12:** Salto de garras (S2, carga) no canto.
- **p16:** Caixa surpresa (S2) só no meio (+78/+136); o resto vem do S1H.
- **Sem ganho de S2–S4 em cenário nenhum:** Kōga e p13 no meio; p16 com 1 barra.

### Loops e 100%

Nenhuma rota nova chega a 100% da vida do boneco (14000), nem passa de 10 s.
- A maior é 10374: p09, canto, 1 barra, 74%.
- A maior com 0 barra é 8582: p09, canto, 61%.
- Os alertas de "mesmo especial 3 vezes" são do S1 em malabarismo que já existia, de 4 a 6 vezes nas rotas do Ryūga, antigas e novas.
- O único especial de comando repetido é o S4 do p14, 3 vezes (7066, 50%). Ele está separado por Super e esperas, e não é loop.

## Arquivos e linhas (só testes; `G3.diff`)

- `android/app/src/test/java/com/gamelutagpt/BateriaBalanco.java`:
  - `:61` `AJUSTE_CFG`, o gancho da config que toda `Luta` aplica;
  - `:77-78` construtor da `Luta` com posições;
  - `:1077`, `:1100` as ações novas no `executar`;
  - `:1160`, `:1168` `buscar` com a lista de ações.
- `android/app/src/test/java/com/gamelutagpt/BateriaCombos.java`:
  - `:55` `Cenario.canto`, com `:142` para a posição;
  - `:82` `ampliadas`;
  - `:111` `apertaEspecial`;
  - `:129` `saiuEspecial`;
  - `:203`, `:228` o uso no `executar`;
  - `:538` `melhorBusca`;
  - `:570` `g3combos`;
  - `:1253` as opções `g3`, `amp` e `tempo` no `main`.
- `android/app/src/test/java/com/gamelutagpt/BateriaAntesDepois.java`:
  - `:402` `combosAmpliados`;
  - `:440` o grupo `combos_amp`.
  - O grupo `combos` ficou intacto.
- `android/app/src/test/java/com/gamelutagpt/BateriaQuebra.java` (novo, 448 linhas):
  - opções `grupo`, `golpes`, `troca`, `canto`, `trococpu`, `curva`, `um`;
  - as opções do Breaker entram por `BateriaBalanco.AJUSTE_CFG`, sem tocar no jogo.

## Testes rodados

| teste | resultado |
|---|---|
| suíte JVM do app (`jpg3.sh -t`, base no HEAD + estas classes) | **OK, 541 testes** |
| `BateriaAntesDepois combos,combos_amp` | grupo `combos` idêntico a `5_full`/`6_head` (61 chaves) |
| `BateriaQuebra tudo` | 67 s |
| `BateriaQuebra trococpu` | 5 s |
| `BateriaQuebra curva` | 56 s |
| `BateriaCombos g3` | 957 s |
| `BateriaAntesDepois combos,combos_amp` (tempo) | 279 s |

As novas classes de teste não têm `@Test`: são baterias de rodar à mão, como as outras.
