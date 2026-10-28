# Bateria de balanço

Lutas CPU × CPU no motor de verdade (o mesmo `CombatEngine` do jogo), com o kit inteiro (assist, troca,
SHINKAI, Breaker, Super, Ultra), CPU nível 8 e sementes fixas: a mesma semente dá a mesma luta. O código
é `android/app/src/test/java/com/gamelutagpt/BateriaBalanco.java` (não roda com a suíte; roda à mão):

```
java -cp <testes e classes do app> com.gamelutagpt.BateriaBalanco [breaker|shinkai|p09|pressao|rotas|busca|tag|tudo]
```

Ressalvas: quem joga é a CPU, não uma pessoa; o Ultra entra com o dano cheio do `ultra.json`, sem a nota
do toque na página; as porcentagens de vitória têm ruído de uns ±4 pontos (o espelho "nunca × nunca" deu
48%).

## Respostas

| Teste | Pergunta | Resposta |
|---|---|---|
| Combo Breaker com 1 barra | Usar o Breaker é quase sempre a melhor escolha? | **Sim, hoje é.** Rende ~1,5 Super e não tem risco. |
| SHINKAI contra SHINKAI | Um tipo vence consistentemente os outros? | **Só por causa do Clash:** a TECHNIQUE ganha quase todos. Sem Clash, os tipos ficam equilibrados. |
| p09 × Yin-lu | O grappler compensa a lentidão sem dominar o chão? | **Não: ele domina.** Não é lento, tem a maior vida e o 2M mais longo, e quase não agarra. |
| Kōga pressionando | O adversário encontra uma saída clara? | **Sim.** Backdash e pulo para trás saem sempre. Só defender não basta (a guarda quebra) e agarrar é punido. |
| Assist + dois quiques | Existe rota de dano excessivo ou sem recuperação? | **Dano alto, sim** (até 68% da vida, 58% começando sem barra). **Sem recuperação, não.** |
| TAG após assistência | A troca parece imediata? | **Não.** Ela espera o golpe do assist acabar, some em silêncio fora da janela e falha em 23 a 31% das vezes. |

### 1. Combo Breaker

- Uma quebra vale em média **2070 de vida**: 915 que deixa de levar mais 1156 do troco. O troco é
  garantido: quem batia fica 44 quadros atordoado e quem quebrou corre sozinho até ele.
- A mesma barra num Super dentro do combo rende **+1396**.
- Mesmo contra combo curto a quebra paga. Os combos da CPU com o p07 têm 2 acertos, então não sobra nada
  para evitar, e ainda assim o troco vale 1263.
- Em lutas inteiras, "sempre quebra" vence **64%** contra "nunca quebra".
- A CPU quase não usa o Breaker: 0,3 por luta no nível 8. Ela decide quebrar só 45% das vezes e ainda
  espera até 18 quadros, muitas vezes tarde demais.
- **Sugestões:** tirar a garantia do troco (atordoamento de 44 para uns 20 quadros, ou quem batia poder
  defender), ou custar 2 barras, ou só valer a partir do 3º acerto.

### 2. SHINKAI

- **Sem Clash, os tipos estão equilibrados:** de 45% a 61% em todos os pares. O SHINKAI sozinho rende
  mais no POWER (+3341 de saldo nos 8 s) e menos no ENERGY (+997).
- **O desequilíbrio vem do Clash.** Quando a CPU joga contra a CPU:
  - a TECHNIQUE vence de 95% a 100% dos confrontos;
  - o POWER perde todos.
- A causa é a CPU: ela empurra num ritmo diferente em cada tipo, e o Clash quase sempre vai até o fim do
  tempo, então quem empurra um pouco mais sempre vence.
- **Com uma pessoa, a TECHNIQUE também leva vantagem.** Apertando sem ritmo nenhum, a 6–8 toques por
  segundo, ela já empurra 300–364 por segundo, porque a janela do pulso (9 de 24 quadros) pega toques ao
  acaso. Os outros tipos:
  - **SPEED** só passa a TECHNIQUE com 10 ou mais toques por segundo;
  - **ENERGY**, sem saber que é para segurar, empurra só 84–171 por segundo;
  - **POWER** não passa de ~300 por segundo.
- Nas lutas com Clash (1,3 por luta, quem vence tira 2000 e fica com o SHINKAI cheio), a TECHNIQUE vence
  60–68% contra todos os tipos.
- **Sugestões:**
  - a CPU empurrar na mesma média em todos os tipos;
  - pulso da TECHNIQUE mais estreito, ou toque fora do pulso atrapalhar;
  - o toque do ENERGY empurrar como o dos outros.

### 3. p09 × Yin-lu

- O p09 vence **79%** da Yin-lu no nível 8 e 88% no nível 5. Contra o elenco todo vence 72% (4º lugar).
- **Ele não é lento:** anda a 300 px/s, a velocidade padrão, igual à Yin-lu.
- **Tem mais vida:** 16100 contra 13440 da Yin-lu (+20%). É a maior do elenco, empatada com o p13.
- **O 2M dele é o melhor do confronto:** tira 620 e alcança 250, contra 450 e 137 do 2M da Yin-lu, e é só
  3 quadros mais lento (9 contra 6).
- 36% do dano dele vem do 2M. **Agarrões são só 1–2%:** ele ganha como lutador de chão com mais vida e
  mais alcance, não como grappler.
- **Sugestões:**
  - andar mais devagar (uns 230 px/s);
  - 2M mais curto ou mais lento;
  - mais dano nos agarrões, para virar a identidade dele.

### 4. Pressão do Kōga

- **Nenhum golpe dele dá vantagem para emendar pressão sem buraco.** Vantagem na defesa: L −5, M +1,
  H −2, 2L −5, 2M +1, 2H −4, S +3, parecida com a média do elenco. Depois de cada um ele fica a 125–190 px
  de quem defendeu.
- **Saídas que funcionam:** backdash e pulo para trás escapam de 97% a 100% das vezes sem levar nada.
- **O jab depois da defesa quase nunca alcança:** 77% das vezes ninguém acerta ninguém.
- **O que não funciona:**
  - **Agarrar:** é punido 93% das vezes, o pior número do elenco. O agarrão erra de longe e o Kōga pune.
  - **Só defender:** com a defesa perfeita a guarda quebra umas 3 vezes em 20 s. Isso acontece contra
    quase todo atacante, porque a guarda foi desenhada para quebrar com ~10 defesas.
- **Ressalva:** quem atacou foi a CPU. Uma pessoa pode apertar a pressão, mas os números de vantagem
  mostram que não há loop.

### 5. Combos com assist e dois quiques

- **Achei rota com assist + quique na parede + quique no chão para 10 dos 16 lutadores.**
- **A maior é do Ryūga:** 9442 de dano (68% da vida média) com 1 barra. Começando sem barra, 7997 (58%),
  porque o combo enche a barra no meio (o especial dá 450 por acerto) e paga dois Supers. A rota sem
  barra:

  ```
  M H assist S SUPER dash pulo j2H jM S SUPER espera S 2L S
  ```

- **Sem nenhum Super**, o máximo fica entre 28% e 51% da vida (p12 e p13 chegam a 51%).
- **Sem recuperação, não:**
  - os quiques são um de cada por combo;
  - o tech no ar funciona;
  - o combo mais longo dura ~9 s;
  - os golpes no 1º quadro em que o caído fica de pé são defensáveis (295 de 299 defendidos).
- **Sugestões:**
  - menos barra ganha dentro do combo;
  - escala mínima do Super menor depois de muitos acertos;
  - olhar a rota do Ryūga.

### 6. TAG depois da assistência

- **A troca espera o golpe do assist acabar.** Apertando TAG logo depois de chamar, nada acontece por até
  33 quadros (0,55 s).
- **Fora da janela, o TAG some em silêncio.** A janela vai até ~31 quadros depois do chamado (o assist da
  Yin-lu dura 8 + 26 + 10). Com o assist já voltando, o aperto é ignorado, e depois disso o botão não faz
  nada por 4 s (a recarga do assist).
- **Mesmo aceito, falha sem aviso** se o titular estiver apanhando ou defendendo no quadro em que o assist
  acaba. Isso aconteceu em **31%** das vezes com a CPU batendo e em **23%** nas lutas CPU × CPU.
- A troca direta (↓+TAG) leva 54 quadros (0,9 s) até o parceiro agir.
- **Sugestões:**
  - trocar no quadro do aperto (o parceiro vira titular já no golpe);
  - ou mostrar "TROCA!" na hora e, se não der, avisar;
  - aceitar o TAG durante a volta do assist.

## Tabelas completas

### 1. Combo Breaker com 1 barra

#### O valor de uma quebra (CPU nível 8 batendo num boneco que não defende)

| atacante | combo inteiro | levou até quebrar | dano evitado | troco de quem quebrou | combo com Super (o que a barra rende a mais) |
|---|---|---|---|---|---|
| Ryūga (p01) | 2821 | 813 | 2008 | 678 | 4638 (+1817) |
| Yin-lu (p02) | 1435 | 869 | 567 | 1895 | 2816 (+1381) |
| Kōga (p03) | 1336 | 813 | 524 | 621 | 3067 (+1730) |
| p04 | 1550 | 802 | 748 | 1890 | 3116 (+1567) |
| p05 | 2084 | 654 | 1430 | 653 | 2998 (+915) |
| p06 | 1144 | 784 | 360 | 432 | 2662 (+1518) |
| p07 | 1193 | 1193 | 0 | 1263 | 3919 (+2727) |
| p08 | 2203 | 690 | 1513 | 727 | 1865 (-338) |
| p09 | 1438 | 1020 | 418 | 333 | 4595 (+3157) |
| p10 | 1532 | 724 | 808 | 1272 | 2720 (+1188) |
| p11 | 1622 | 650 | 972 | 1637 | 3393 (+1771) |
| p12 | 1247 | 801 | 446 | 1943 | – |
| p13 | 952 | 557 | 395 | 1492 | – |
| p14 | 1396 | 823 | 572 | 1282 | 2480 (+1084) |
| p15 | 3210 | 727 | 2483 | 433 | 3200 (-10) |
| p16 | 2163 | 775 | 1389 | 1944 | 3200 (+1037) |

Média: a quebra vale 915 (evitado) + 1156 (troco) = 2070 de vida; a mesma barra num Super dentro do combo rende +1396.

#### Lutas inteiras 2x2 (a mesma dupla dos dois lados, nível 8, 300 lutas cada)

| lado A | lado B | A vence | empate | quebras de A por luta | quebras de B por luta |
|---|---|---|---|---|---|
| sempre quebra | nunca quebra | 64% | 0% | 2.2 | 0.0 |
| sempre quebra | CPU normal (45%) | 62% | 0% | 2.4 | 0.9 |
| CPU normal (45%) | nunca quebra | 48% | 0% | 0.3 | 0.0 |
| nunca quebra | nunca quebra | 48% | 0% | 0.0 | 0.0 |

### 2. SHINKAI contra SHINKAI

#### Valor do SHINKAI sozinho (1x1 espelhado, só o lado A liga no GO; 8 s de SHINKAI + 2 s)

| tipo | dano de A | dano de B | saldo | A vence a luta (sem barra do outro) |
|---|---|---|---|---|
| POWER | 4509 | 1168 | +3341 | 70% |
| SPEED | 3752 | 1394 | +2357 | 63% |
| ENERGY | 3115 | 2118 | +997 | 56% |
| TECHNIQUE | 3086 | 1693 | +1392 | 61% |

#### Confronto direto (SHINKAI Clash: os dois ligam, 1x1 espelhado, 120 por par)

| linha vence coluna | POWER | SPEED | ENERGY | TECHNIQUE |
|---|---|---|---|---|
| POWER | – | 25% (120) | 0% (120) | 0% (120) |
| SPEED | 73% (118) | – | 11% (114) | 1% (120) |
| ENERGY | 100% (120) | 81% (111) | – | 2% (120) |
| TECHNIQUE | 100% (120) | 95% (120) | 100% (120) | – |

#### Clash com uma pessoa (empurrão por segundo; a marca vai a 600, a disputa dura 5 s)

| tipo | 6 toques/s | 8 toques/s | 10 toques/s | 12 toques/s | sabendo o jeito do tipo (8 toques/s) | CPU nível 8 |
|---|---|---|---|---|---|---|
| POWER | 179 | 238 | 298 | 238 | 238 | ~175-210 |
| SPEED | 224 | 298 | 371 | 444 | 298 | ~190-245 |
| ENERGY | 84 | 112 | 142 | 171 | 254 | ~245 |
| TECHNIQUE | 298 | 364 | 371 | 375 | 375 | ~260-280 |


#### Lutas inteiras 2x2 com Clash (a mesma dupla, cada lado com um tipo para os dois, 160 lutas por par)

| linha vence coluna | POWER | SPEED | ENERGY | TECHNIQUE |
|---|---|---|---|---|
| POWER | 54% | 53% | 38% | 39% |
| SPEED | 56% | 48% | 36% | 43% |
| ENERGY | 68% | 59% | 54% | 38% |
| TECHNIQUE | 61% | 60% | 68% | 50% |

SHINKAI ligados por luta (média): 3.78; Clash por luta: 1.30

#### Lutas inteiras 2x2 sem Clash (a mesma dupla, cada lado com um tipo para os dois, 160 lutas por par)

| linha vence coluna | POWER | SPEED | ENERGY | TECHNIQUE |
|---|---|---|---|---|
| POWER | 50% | 52% | 61% | 57% |
| SPEED | 49% | 49% | 53% | 57% |
| ENERGY | 51% | 50% | 49% | 45% |
| TECHNIQUE | 45% | 51% | 58% | 53% |

SHINKAI ligados por luta (média): 3.43; Clash por luta: 0.00

### 3. p09 (grappler) contra Yin-lu (p02)

#### Todos contra todos, 1x1, nível 8 (40 lutas por par, metade de cada lado)

| # | lutador | vitórias contra o elenco | contra p09 | contra Yin-lu |
|---|---|---|---|---|
| 1 | p08 | 86% | 75% | 85% |
| 2 | Ryūga (p01) | 78% | 55% | 48% |
| 3 | p06 | 75% | 53% | 63% |
| 4 | p09 | 72% | – | 85% |
| 5 | p11 | 61% | 43% | 20% |
| 6 | Yin-lu (p02) | 58% | 15% | – |
| 7 | p10 | 55% | 20% | 65% |
| 8 | p14 | 53% | 30% | 57% |
| 9 | Kōga (p03) | 50% | 23% | 43% |
| 10 | p13 | 50% | 53% | 25% |
| 11 | p04 | 43% | 10% | 48% |
| 12 | p05 | 32% | 15% | 13% |
| 13 | p12 | 25% | 20% | 25% |
| 14 | p07 | 22% | 5% | 20% |
| 15 | p16 | 22% | 3% | 15% |
| 16 | p15 | 20% | 3% | 15% |

#### p09 × Yin-lu em detalhe (1x1, 400 lutas por nível, metade de cada lado)

| nível | p09 vence | dano por acerto p09 / Yin-lu | acertos por luta p09 / Yin-lu | dano do p09 em agarrões | tempo colados (< 200 px) | tempo longe (> 450 px) | duração |
|---|---|---|---|---|---|---|---|
| 5 | 88% | 755 / 626 | 19.5 / 13.5 | 2% | 40% | 5% | 41 s |
| 8 | 79% | 723 / 643 | 20.2 / 17.1 | 1% | 39% | 5% | 50 s |

Vida: p09 16100, Yin-lu 13440. Andar: p09 300, Yin-lu 300 px/s.
De onde vem o dano (p09, nível 8): 2M 36%, S 23%, M 13%, SUPER 10%, H 5%, L 4%.
De onde vem o dano (Yin-lu, nível 8): S 26%, SUPER 19%, H 16%, SHINKAI_CLASH 16%, M 13%, L 3%.

### 4. Pressão do Kōga (p03) na defesa

A CPU nível 8 ataca; quem defende é o Ryūga com a defesa perfeita (altura sempre certa) e, depois de
cada sequência defendida, tenta uma saída. Ganhou = acertou primeiro; punido = levou um golpe antes;
neutro = ninguém acertou em 45 quadros. Média do elenco ao lado (20 lutas de 20 s por saída).

Vantagem na defesa (quadros; + o atacante age primeiro, − quem defendeu pune) e distância depois:

| atacante | L | M | H | 2L | 2M | 2H | S |
|---|---|---|---|---|---|---|---|
| Kōga (p03) | -5 (149 px) | +1 (153 px) | -2 (180 px) | -5 (125 px) | +1 (133 px) | -4 (145 px) | +3 (191 px) |
| média do elenco | -5.5 | -1.8 | -2.0 | -5.4 | +1.0 | -4.3 | +5.3 |

| saída | Kōga: ganhou / neutro / punido | elenco: ganhou / neutro / punido | pior atacante para essa saída |
|---|---|---|---|
| L | 10% / 77% / 13% (238) | 13% / 79% / 8% | p08 30% |
| 2L | 8% / 80% / 12% (244) | 17% / 75% / 8% | p08 45% |
| agarrão | 0% / 7% / 93% (144) | 0% / 33% / 66% | Kōga (p03) 93% |
| backdash | 0% / 97% / 3% (264) | 0% / 98% / 2% | p08 9% |
| pulo para trás | 0% / 100% / 0% (137) | 0% / 99% / 1% | p06 12% |
| pushblock | 0% / 100% / 0% (20) | 0% / 100% / 0% | Ryūga (p01) 0% |

Só defendendo (defesa perfeita, 20 lutas de 20 s):

| atacante | sequências / 20 s | emendas sem buraco | buraco menor que o L (jab perde) | quebras de guarda | golpes que passaram a defesa | dano deles | chip |
|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 13.3 | 0.5 | 0.0 | 2.25 | 7.3 | 4017 | 0 |
| Yin-lu (p02) | 16.6 | 0.1 | 0.1 | 2.95 | 3.2 | 1502 | 0 |
| Kōga (p03) | 11.3 | 0.0 | 0.0 | 2.95 | 5.5 | 2051 | 0 |
| p04 | 12.7 | 0.0 | 0.0 | 3.05 | 5.0 | 2619 | 0 |
| p05 | 16.1 | 0.2 | 0.1 | 2.75 | 2.6 | 1225 | 0 |
| p06 | 14.0 | 0.1 | 0.4 | 3.80 | 5.1 | 2639 | 0 |
| p07 | 7.6 | 0.0 | 0.0 | 0.05 | 0.0 | 0 | 0 |
| p08 | 12.3 | 0.1 | 0.1 | 2.95 | 9.4 | 5937 | 0 |
| p09 | 12.3 | 0.0 | 0.0 | 2.25 | 4.2 | 2558 | 0 |
| p10 | 15.8 | 0.1 | 0.1 | 2.50 | 2.5 | 1122 | 0 |
| p11 | 15.3 | 0.0 | 0.0 | 2.75 | 3.3 | 1253 | 0 |
| p12 | 10.8 | 0.0 | 0.0 | 2.85 | 4.8 | 2637 | 0 |
| p13 | 10.2 | 0.0 | 0.0 | 1.35 | 2.4 | 1134 | 0 |
| p14 | 4.9 | 0.0 | 0.0 | 0.50 | 0.7 | 332 | 0 |
| p15 | 17.5 | 0.0 | 0.0 | 3.25 | 2.6 | 1015 | 0 |
| p16 | 16.1 | 0.0 | 0.0 | 2.05 | 2.0 | 993 | 0 |

### 5. Combos com assist e dois quiques

A CPU nível 8 (com o parceiro de assist) bate num boneco que não defende, 20 lutas de 60 s por lutador.
Um quique de parede e um de chão por combo no máximo (config). Dano em % da vida média do elenco.

| grupo | combos | dano médio | maior dano | quem | acertos | duração do maior |
|---|---|---|---|---|---|---|
| sem assist, sem quique | 1903 | 1548 (11%) | 7663 (55%) | p09 com 3 barras (Super) | 10 | 3.5 s |
| com assist | 1649 | 1763 (13%) | 7883 (57%) | Ryūga (p01) com 3 barras (Super) | 22 | 8.1 s |
| dois quiques (parede + chão) | 0 | – | – | – | – | – |
| assist + dois quiques | 0 | – | – | – | – | – |

Combo mais longo: 11.4 s (20 acertos, p14). Vida média do elenco: 13895.
Combos que começaram sem o boneco poder agir desde o anterior: 490; maior dano somado assim: 13635 (p09, 98%).
Estado do boneco quando o combo seguinte começou: {AIR_HITSTUN no ar por jL=29, BLOCKSTUN congelado por S=1, BLOCKSTUN por L=1, BLOCKSTUN por S=31, HITSTUN por 2L=2, HITSTUN por ?=1, HITSTUN por H=3, HITSTUN por L=1, HITSTUN por M=5, HITSTUN por S=88, HITSTUN por SUPER=2, HITSTUN por ULTRA=18, WAKEUP por 2L=1, WAKEUP por L=6, WAKEUP por S=278, WAKEUP por SUPER=20, WAKEUP por jM=3}

### 5b. Busca de rotas (assist + dois quiques)

Busca em feixe (largura 60, até 16 passos) sobre L/M/H, agachados, S1, Super, assist, pulo, super pulo,
dash, golpes no ar e esperas. O boneco não defende mas cai de pé (tech no ar) assim que pode.

| lutador | barras | maior combo | % da vida | acertos | quiques P/C | assist | rota | assist + 2 quiques: dano | rota | sem Super nenhum: maior | assist + 2 quiques sem Super |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 0 | 8202 | 59% | 21 | 1/0 | sim | M H A S SUPER D 2H 2L J jH S jH SUPER W14 W14 S | 7997 (58%, 20 acertos, 4.7 s) | M H A S SUPER D J j2H jM S SUPER W14 W14 S 2L S | 6254 (45%) | não achou |
| Ryūga (p01) | 1 | 9259 | 67% | 23 | 1/0 | sim | H S SUPER W14 W14 A H SUPER D M S H SUPER W14 W14 S | 9442 (68%, 24 acertos, 6.3 s) | M H A S SUPER D J j2H jM S SUPER W14 W14 S H SUPER |  |  |
| Yin-lu (p02) | 0 | 4798 | 35% | 10 | 0/0 | sim | H A L M S SUPER | não achou | – | 5824 (42%) | 5685 (41%) |
| Yin-lu (p02) | 1 | 4288 | 31% | 8 | 0/0 | sim | H A M S SUPER | não achou | – |  |  |
| Kōga (p03) | 0 | 5718 | 41% | 8 | 1/0 | sim | H A J jM jH S 2L SUPER | 6789 (49%, 11 acertos, 7.0 s) | M A H J 2H J jH S j2H jH S SUPER | 6088 (44%) | 6108 (44%) |
| Kōga (p03) | 1 | 4669 | 34% | 7 | 0/0 | sim | H L A 2L SUPER | não achou | – |  |  |
| p04 | 0 | 5552 | 40% | 9 | 0/0 | sim | H 2L M A M S SUPER | 7772 (56%, 15 acertos, 8.0 s) | H A M H J M 2H J jH S j2H jL S jH H SUPER | 6555 (47%) | 6071 (44%) |
| p04 | 1 | 5408 | 39% | 7 | 0/0 | sim | H A M S SUPER | não achou | – |  |  |
| p05 | 0 | 6041 | 43% | 13 | 1/1 | sim | H A 2H J jM jH j2H S jM j2H S jH H SUPER | 5736 (41%, 14 acertos, 6.7 s) | H A H J M J j2H S jM 2H J jH jM jH S A | 5703 (41%) | 5736 (41%) |
| p05 | 1 | 4629 | 33% | 7 | 0/0 | sim | H A M S SUPER | 5695 (41%, 12 acertos, 6.7 s) | H A H J M 2H J jH jM j2H jH jM A jH SUPER |  |  |
| p06 | 0 | 6005 | 43% | 17 | 1/1 | sim | M H A 2H J jH jM jH jM jH j2H 2M H SUPER | 5509 (40%, 14 acertos, 6.8 s) | M H A 2H J jH jM j2H jH jM SUPER | 5383 (39%) | 5025 (36%) |
| p06 | 1 | 3922 | 28% | 9 | 0/0 | sim | H 2L A SUPER | 5821 (42%, 16 acertos, 7.6 s) | M H A 2H J jH jM jH j2H jL 2L H SUPER |  |  |
| p07 | 0 | 7385 | 53% | 10 | 1/1 | sim | H 2L 2H A J jM jH SUPER W14 W14 W14 W14 W14 W14 jH j2H | 7064 (51%, 8 acertos, 6.3 s) | H H J A 2H J jH j2H SUPER | 6978 (50%) | 7311 (53%) |
| p07 | 1 | 6481 | 47% | 9 | 1/0 | sim | H 2L A M H SUPER W14 W14 W14 W14 W14 W14 D J jH jL | 8024 (58%, 9 acertos, 7.7 s) | H H J A 2H J jH j2H SUPER W14 W14 W14 W14 W14 W14 SUPER |  |  |
| p08 | 0 | 6846 | 49% | 15 | 1/1 | sim | H 2L 2H A J jM jH j2H jH j2H 2M M S SUPER | não achou | – | 5935 (43%) | não achou |
| p08 | 1 | 4339 | 31% | 7 | 0/0 | não | H 2L S SUPER | não achou | – |  |  |
| p09 | 0 | 6924 | 50% | 8 | 0/0 | não | H S D M S SUPER W14 W14 W14 W14 W14 S | não achou | – | 3950 (28%) | não achou |
| p09 | 1 | 8676 | 62% | 10 | 1/0 | sim | H S D SUPER W14 W14 W14 W14 W14 A J H SUPER | não achou | – |  |  |
| p10 | 0 | 6407 | 46% | 14 | 1/1 | sim | H A 2H J jH j2H jH j2H jH H M S SUPER | 6376 (46%, 15 acertos, 6.4 s) | M A H 2H J jM S j2H jH S M H SUPER | 5933 (43%) | 5882 (42%) |
| p10 | 1 | 4730 | 34% | 8 | 0/0 | sim | H A M S SUPER | não achou | – |  |  |
| p11 | 0 | 4407 | 32% | 7 | 1/0 | sim | H 2L M H S A S SUPER | não achou | – | 2615 (19%) | não achou |
| p11 | 1 | 5996 | 43% | 6 | 1/0 | sim | H S A SUPER W14 W14 W14 W14 W14 W14 H S SUPER | não achou | – |  |  |
| p12 | 0 | 5926 | 43% | 9 | 0/0 | sim | H S M S A SUPER | 7521 (54%, 13 acertos, 7.4 s) | H S H A 2H J jH S j2H jL 2M SUPER | 7043 (51%) | 6909 (50%) |
| p12 | 1 | 5751 | 41% | 8 | 0/0 | não | H S M S SUPER | 7521 (54%, 13 acertos, 7.4 s) | H S H A 2H J jH S j2H jL 2M SUPER |  |  |
| p13 | 0 | 7115 | 51% | 17 | 1/1 | não | H H J 2H J jM jH jM jH j2H M 2H J jL jH S | 6858 (49%, 15 acertos, 9.1 s) | H H A J 2H J jH jM j2H jH M 2H J jM jH S | 7031 (51%) | 6858 (49%) |
| p13 | 1 | 7115 | 51% | 17 | 1/1 | não | H H J 2H J jM jH jM jH j2H M 2H J jL jH S | 6858 (49%, 15 acertos, 9.1 s) | H H A J 2H J jH jM j2H jH M 2H J jM jH S |  |  |
| p14 | 0 | 5737 | 41% | 21 | 1/1 | sim | M H J M A J j2H jH H SUPER W14 W14 W14 J S | 5737 (41%, 21 acertos, 6.0 s) | M H J M A J j2H jH H SUPER W14 W14 W14 J S | 5006 (36%) | 5006 (36%) |
| p14 | 1 | 6636 | 48% | 30 | 1/1 | sim | M H SUPER W14 W14 W14 A L J j2H jH M H SUPER | 5663 (41%, 19 acertos, 6.2 s) | M H A SUPER W14 W14 W14 2H J jH j2H jH jM SUPER |  |  |
| p15 | 0 | 5700 | 41% | 12 | 1/0 | sim | M 2H A J jM jH j2H jH jM jH SUPER | não achou | – | 4522 (33%) | não achou |
| p15 | 1 | 3840 | 28% | 6 | 0/0 | não | H SUPER | não achou | – |  |  |
| p16 | 0 | 6707 | 48% | 12 | 1/1 | sim | H A 2H J jM jH j2H jH jM j2H 2M S SUPER | 6317 (45%, 12 acertos, 7.0 s) | H A H J M 2H J jH jM j2H jH jM A SUPER | 5693 (41%) | 5595 (40%) |
| p16 | 1 | 6707 | 48% | 12 | 1/1 | sim | H A 2H J jM jH j2H jH jM j2H 2M S SUPER | não achou | – |  |  |

### 6. TAG depois da assistência

Assist da Yin-lu: S, 26 quadros (entra em 8, sai em 10).

Ryūga chama a Yin-lu (assist) e aperta TAG de novo k quadros depois (boneco parado do outro lado).

| TAG k quadros depois do assist | a troca sai? | quadros do TAG até a Yin-lu poder agir |
|---|---|---|
| 1 | sim | 33 (0.6 s) |
| 4 | sim | 30 (0.5 s) |
| 7 | sim | 27 (0.5 s) |
| 10 | sim | 24 (0.4 s) |
| 13 | sim | 21 (0.4 s) |
| 16 | sim | 18 (0.3 s) |
| 19 | sim | 15 (0.3 s) |
| 22 | sim | 12 (0.2 s) |
| 25 | sim | 9 (0.2 s) |
| 28 | sim | 6 (0.1 s) |
| 31 | sim | 3 (0.1 s) |
| 34 | não (ignorado) | – |
| 37 | não (ignorado) | – |
| 40 | não (ignorado) | – |
| 43 | não (ignorado) | – |
| 46 | não (ignorado) | – |
| 49 | não (ignorado) | – |
| 52 | não (ignorado) | – |
| 55 | não (ignorado) | – |
| 58 | não (ignorado) | – |
| 61 | não (ignorado) | – |
| 62 | não (ignorado) | – |
| 63 | não (ignorado) | – |
| 64 | não (ignorado) | – |
| 65 | não (ignorado) | – |
| 66 | não (ignorado) | – |
| 67 | não (ignorado) | – |
| 68 | não (ignorado) | – |
| 69 | não (ignorado) | – |
| 70 | não (ignorado) | – |
| 71 | não (ignorado) | – |
| 72 | não (ignorado) | – |
| 73 | não (ignorado) | – |
| 74 | não (ignorado) | – |
| 75 | não (ignorado) | – |

Com o rival (CPU nível 8) batendo: TAG 10 quadros depois do assist, 92 de 300 pedidos aceitos não viraram troca (31%).
Troca direta (↓+TAG) até o parceiro agir: 54 quadros (0.9 s). Recarga do assist 4.0 s, da troca 10.0 s, depois do Assist → Tag 5.0 s.
Lutas CPU × CPU (300): 1987 pedidos de Assist → Tag, 1527 viraram troca, 452 sumiram sem trocar (23%).
