# Relatório da lista G: antes e depois

Este relatório cobre os dez itens da lista, cada um isolado e medido com as mesmas sementes e cenários. A base é o HEAD `5cd8ed16`, depois das etapas 1–5, e o estado final é `29ca0716`. As tabelas completas estão em [lista-g-tabelas.md](lista-g-tabelas.md). Os relatórios de cada frente estão em [g1-cpu.md](g1-cpu.md), [g2-assists-dhc.md](g2-assists-dhc.md) e [g3-breaker-combos.md](g3-breaker-combos.md).

Ruído das medidas: ±5 pontos nas regras e ±8 por personagem no 1x1, no 2x2 e na matriz. Todas as lutas são CPU × CPU no nível 8.

## Os dez itens

| # | Item | O que foi feito | Commit | Testes |
|---|---|---|---|---|
| 1 | HEAD e base | O HEAD reproduz as 148 métricas da etapa 5, idênticas | — | — |
| 2 | S2–S4 e assinaturas da CPU | `CpuSpecials` lê a função de cada especial nos dados; a CPU usa por reação, no neutro, na punição e no antiaéreo | `a35c3451` | `CpuSpecialsTest` (5) |
| 3 | Alcance da CPU | Régua real medida no motor, em pé e agachado (`CpuReachTable`, `CpuReach`) | `29ca0716` | `CpuReachTest` (4) |
| 4 | Assists p12–p15 | O assist corpo a corpo vai até onde o golpe pega, no máximo 220 px à frente; p13 com H, p15 com 2H | `e09caefa` | `AssistCorpoACorpoTest` (5) |
| 5 | Breaker com 320 px | Medido, **não aplicado** (decisão sua) | `3a1161e9` (só teste) | `BateriaQuebra` |
| 6 | DHC | O DHC espera o Super acabar de bater; o parceiro corpo a corpo vai até onde o Super dele alcança | `df269f61` | `DhcSemCortarTest` (4) |
| — | Erro achado no caminho | O contra-golpe lia o golpe do assist e o motor caía. Erro antigo, que só apareceu com a CPU usando contra-golpe | `d538bf8b` | caso novo no `MechanicsTest` |
| 7 | Busca de combos com S2–S4 | Grupo `combos_amp`; o grupo `combos` antigo continua comparável | `3a1161e9` | — |
| 8 | T15–T21 de novo | Rodados na base e no final (abaixo) | — | — |
| 9 | Colisões e movimento no aparelho | Medido no PC (frente E, T25/T26) e roteiro para o celular em [aparelho.md](aparelho.md) | `31e02a93`, `834e537b` | `BateriaColisoes`, `BateriaQuadros` |
| 10 | Campanha e versus | [plano-conclusao.md](../plano-conclusao.md) | `366d61ef` | — |

Suíte do app no final: 560 de 560 passando. O `build_characters.py --check` passa: 19 personagens e 657 atlas.

## Os 16 personagens

Força contra o elenco na matriz 1x1 16 × 16, com os dois lados somados e sem os espelhos:

| personagem | base | final | Δ | o que mudou |
|---|---|---|---|---|
| p08 | 77% | 78% | +1 | segue no topo; quase só usa a bola (o contra-golpe dele só lê golpe baixo) |
| p04 | 63% | 64% | +1 | esquiva e S4 entram (S2–S4 0,4 → 3,8 por minuto) |
| p09 | 58% | 64% | +6 | escudo e ombrada com armadura; agarrões caem de 0,8 para 0,4 por minuto |
| p11 | 61% | 63% | +2 | passa a envenenar (seringa 2 por minuto); vira a melhor dupla com o assist do p12 |
| p10 | 60% | 63% | +3 | esquiva e S4 (S2–S4 3,8 por minuto) |
| Ryūga (p01) | 67% | 61% | −6 | usa o Chute giratório, mas o Aparar quase não sai (sem mecânica, 20 quadros) |
| Kōga (p03) | 60% | 58% | −2 | a CPU para de usar L, M e H contra agachado (o H dele passa por cima) |
| p13 | 62% | 55% | −7 | a pedra entra (1,9 por minuto); a queda fica dentro do ruído de ±8 |
| p06 | 68% | 51% | −17 | eco, puxão e passo-sombra entram, mas o 2M é 23 px mais curto que o pacote diz |
| p05 | 41% | 51% | +10 | o sumiço acerta 93%; S2–S4 rendem 21% do dano dele |
| p12 | 43% | 47% | +4 | o S4 entra (1,7 por minuto); como assist, passa a valer (veja Duplas) |
| Yin-lu (p02) | 37% | 45% | +8 | contra-golpe e S4 (pouco: 0,3 por minuto) |
| p07 | 16% | 32% | +16 | contra-golpe e S4; S2–S4 rendem 18% do dano |
| p14 | 41% | 31% | −10 | com a régua real a CPU para de usar golpes que não pegam; o mergulho (S1) estreia |
| p16 | 30% | 24% | −6 | a caixa sai 2,7 por minuto, mas a banana (44 quadros) só pune |
| p15 | 17% | 13% | −4 | o repel funciona (96%), mas ela segue sem rota de karatê |

**Leitura:**

- **Mais estilos:**
  - na base, 13 dos 16 personagens usavam zero S2–S4 e os outros três 0,2–0,4 por minuto; no final, todos usam, de 0,3 a 4,9 por minuto;
  - p06, p08 e p11 deixam de jogar igual;
  - p05, p07 e p10 ganham jogo próprio.
- **Faixa de força:** quase a mesma, 16–77% na base e 13–78% no final. O desvio padrão caiu um pouco, de 17,8 para 16,8 pontos.
- **Quem ainda está fraco demais:** p15 (13%), p16 (24%), p14 (31%) e p07 (32%).
  - Para p14, p15 e p16, a CPU melhor não resolve: o que falta está nos dados dos golpes.
  - Os especiais lentos deles (banana, Salto do colosso) e o Aparar do Ryūga só servem para punir.

Os números de 1x1 e 2x2 por personagem da bateria fixa, etapa por etapa, estão em [lista-g-tabelas.md](lista-g-tabelas.md).

## Duplas

**Duplas oficiais** (T21): 2x2 contra as outras sete duplas oficiais, 8 lutas por adversário.

| dupla | base | final | por quê |
|---|---|---|---|
| p09 + p10 | 77% | 79% | segue a melhor; o p04 como parceiro do p09 subiu para 84% |
| p11 + p12 | 50% | 64% | o assist do p12 soma +2555 ao combo do p11 (antes −274) |
| Ryūga + Yin-lu | 66% | 64% | dentro do ruído; o p12 virou o melhor parceiro do Ryūga (75%) |
| p07 + p08 | 50% | 63% | o DHC deixou de cortar o Super do p07 (2196 → 4934) e o p07 joga melhor sozinho |
| Kōga + p04 | 66% | 55% | o Kōga perdeu o H contra agachado; o p09 é o melhor parceiro dele (63%) |
| p13 + p14 | 25% | 39% | o assist do p14 passou a acertar (antiaéreo 0 → 92%) |
| p05 + p06 | 45% | 36% | o p06 caiu sozinho (−17); o p08 rende mais como parceiro do p05 (68%) |
| p15 + p16 | 13% | 2% | as duas estão entre as três mais fracas do elenco |

**Sinergia (C01):** quanto o assist de cada um soma ao maior combo dos outros, em média.

| assist | base | final |
|---|---|---|
| p12 | −198 | +1805 |
| p14 | −198 (não acertava) | +1394 |
| p13 | −198 | +531 |
| p15 | +617 | +881 |
| p07 | — | antiaéreo 0 → 92%, alcance 100–150 → 100–450 px |

As notas de sinergia mudaram assim:

- p14: 1 → 5;
- p11: 3 → 4;
- p12, p13 e p07: +1 cada;
- Ryūga, Kōga e p04: −1 cada, porque os outros subiram e a nota é por quintil.

## T15–T21 refeitos no final

| teste | base | final | leitura |
|---|---|---|---|
| T12 Breaker, "sempre quebra" vence "nunca" | 57% | 66% | a CPU quebra mais (0,76 → 1,09 por luta) e o dano evitado sobe (832 → 924) |
| T15 ganho de cada SHINKAI | POWER +3424, SPEED +2780, TECHNIQUE +1649, ENERGY +1395 | POWER +2893, SPEED +1881, TECHNIQUE +968, ENERGY +700 | quem defende agora responde com contra-golpe, repel e esquiva, e todos os tipos rendem menos. A ordem é a mesma; o ENERGY fica bem atrás |
| T16 resposta ao SHINKAI | CPU leva 2152–4581 | 2155–4385 | igual dentro do ruído; no canto, o POWER subiu de 4720 para 5396 |
| T17 assists | 5 corpo a corpo sem função | todos com alcance de 400–550 px e antiaéreo de 33–100% | — |
| T18 assist no combo | p12, p13 e p14 subtraíam | somam +531 a +1805 | p14 vira parceiro de +2625 para o p09 |
| T19 TAG | 0% perdido | 0% perdido | igual |
| T20 DHC | Super + DHC: 1670 por barra | 2367 por barra (nas duplas em que sai) | ainda abaixo do Super sozinho (3042 por barra). Em p05+p06, p13+p14 e p15+p16 o DHC não sai mais: antes saía sem somar nada (p05+p06 e p15+p16) ou dava 3758 (p13+p14) |
| T21 duplas | acima | acima | — |

**Breaker (G3, sem mudar o jogo):** troco grátis em todos os pares.

| opção | troco grátis |
|---|---|
| hoje | 99% |
| 320 px | 59% |
| atordoamento 4 | 39% |

- A recomendação é o atordoamento 4. Detalhes em [g3-breaker-combos.md](g3-breaker-combos.md). Essas opções foram medidas com a CPU de antes da G1; vale repetir a `BateriaQuebra` antes de aplicar.
- Com a CPU nova, o troco do roteiro da etapa 1 ficou em 57% (era 69%), porque a CPU que quebra agora também se defende com especiais.

**Combos com S2–S4 (G3):**

- O S1 forte dá até cerca de 800 a mais do que o S1 médio que a busca antiga usava.
- Os maiores ganhos de S2–S4:
  - p14 +1402;
  - p09 até +1619, mas parte é especial de carga e pode não ser possível para um humano;
  - p07 até +1390;
  - p08 +1076.
- Nenhuma rota passa de 100% da vida.
- O Aparar do Ryūga e o repel e a rajada da p15 não entram em rota nenhuma.

## Colisões e aparelho (item 9)

- **T25, medido no PC:**
  - 77% dos acertos acontecem com o desenho encostando;
  - 11,3% são fantasmas, com a pior folga de 100 px, porque a caixa do membro é um retângulo;
  - há duas falhas reais: 2M e 2L na ponta contra o Ryūga agachado, que não tem caixa medida, e a Investida do p12, que atravessa o rival.
- **T26 no PC, sem GPU:** 29–51 ms por quadro. O fundo do cenário é o maior custo, e uma luta 2x2 decodifica cerca de 1460 MB de sprites.
- **No celular:** [aparelho.md](aparelho.md) traz o roteiro de cerca de 60 minutos para colisões, perfis de movimento (T03), FPS e comandos no toque.

## Decisões pendentes (precisam da sua aprovação)

1. **Breaker:** atordoamento 4 (recomendado), 320 px, ou manter 20 quadros.
2. **p09:** voltou a 64%, com o 2M em 22% do dano. Aplicar a 2ª iteração (velocidade 300 → 270) ou o `alcanceMaximo` 190 no 2M?
3. **p06:** o 2M é 23 px mais curto do que o pacote declara. Ajustar o golpe ou aceitar a queda (68 → 51%)?
4. **p14, p15, p16 e p07:** para subirem, a mudança tem de ser nos dados dos golpes. Exemplos:
   - especiais lentos mais rápidos (banana 44 q, Salto do colosso 34 q, Aparar 20 q);
   - a rota de karatê da p15;
   - o Aparar do Ryūga virar aparar de verdade.
5. **DHC:** ainda rende menos que guardar a barra. As opções são piso de 70% no dano do DHC, custo de meia barra, ou manter. Também a regra nova do parceiro avançar até 420 px.
6. **Assists:** p13 com H e p15 com 2H em vez dos golpes anteriores, e o p07 mudando junto por causa da regra geral.
7. **Força dos especiais da CPU:** a CPU sempre usa força M. Escolher a força pede um campo novo no `FighterInput`.
8. **ENERGY do SHINKAI:** ficou o tipo mais fraco (+700 contra +2893 do POWER) com a defesa nova da CPU.
9. **T25:**
   - medir a caixa do agachado do Ryūga, que é barato;
   - os quadros ativos da Investida do p12;
   - a regra de "o H pega quem está agachado?".
10. **Tabela de alcance da CPU:** gerar junto com o `build_characters.py` ou manter à mão, com o `CpuReachTest` avisando quando ficar velha.

A validação com pessoas continua de pé, pelo [playtest.md](playtest.md). As frequências dos especiais da CPU foram calibradas só CPU contra CPU.
