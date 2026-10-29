# Frente H5: DHC (item 5), assists (item 6) e SHINKAI ENERGY (item 7)

A base é o HEAD `5dedf956`, com as mesmas sementes e cenários das tabelas da lista G (`ad/10_g1b.tsv`, `fin/equipe_final.md`). O trabalho foi feito numa cópia em `scratchpad/h5`, sem commit na árvore principal. Os diffs estão em `scratchpad/bal/`, todos relativos ao `5dedf956`, e a aplicação deles foi conferida com `git apply --check` na árvore principal.

| Diff | O que é | Muda o jogo? |
|---|---|---|
| `H5_dhc_entrada.diff` | Etapa H5a: a entrada do DHC desliza, sem pular de lugar e sem entrar no corpo do rival, mais o `DhcEntradaTest` | sim (visual e posição; dano igual) |
| `H5_dhc_piso70.diff` | Etapa H5b: piso de dano do Super do DHC, **desligado** (`dhcDamageFloorPermille = 0`), mais o `DhcPisoTest`. Vai por cima do H5a | não, com o padrão 0 |
| `H5_assists_testes.diff` | `AssistSegurancaTest` e a sonda `SondaAssist` | não |
| `H5_energy_bateria.diff` | `BateriaShinkaiBarra` (medição do item 7) | não |
| `H5_sondas_dhc.diff` | `SondaDhc` (360 pares) e `SondaDhcTrace` | não |

Suíte com tudo aplicado: **568 de 568** (eram 560; entram 3 do `DhcEntradaTest`, 3 do `DhcPisoTest` e 2 do `AssistSegurancaTest`).

## Item 5: DHC

### Verificação da entrada até 420 px (G2b, já no HEAD)

A `SondaDhc` cobre os 15 titulares × parceiros com Super corpo a corpo, no meio e no canto: 360 pares, e o DHC sai em todos. A medição é M H Super e TAG.

| verificação | base (`5dedf956`) | H5a |
|---|---|---|
| vale só no DHC | sim: `dhcMaxAdvance` só aparece em `CombatEngine.dhc` | sim (os campos novos também só lá) |
| salto de posição num quadro maior que 40 px | **117 de 360**; pior **411 px** (p07 → p12); p07 → p08 com 275 px | nenhum salto. O maior passo é o primeiro quadro da entrada: até 68 px, a mesma curva da entrada do assist |
| corpo dentro do rival na entrada (rival no chão) | **6** | **1** |
| corpo dentro do rival durante o Super do parceiro | 18 | 15, todos com o **p14** de parceiro |
| atravessa o rival na entrada | 0 | 0 |
| fora da arena | 0 | 0 |

**Os dois problemas achados:**

1. **Teletransporte:**
   - O parceiro aparecia até 420 px à frente no mesmo quadro em que o titular sai.
   - **Correção:** ele desliza de onde o titular estava até o lugar do Super, a 36 px por quadro, com a mesma curva do assist (`assistEnterFrames`), em no máximo 12 quadros.
   - Enquanto desliza, o Super dele não anda (fica em espera, como na entrada do retrato) e o rival e os projéteis esperam os mesmos quadros a mais. Por isso o tempo entre a entrada e o golpe é o mesmo de antes.
2. **Corpo dentro do rival:**
   - O parceiro, mais largo que o titular, entrava no lugar dele já encostado demais.
   - **Correção:** o lugar de chegada nunca fica a menos de meia largura de cada corpo do rival.

O que sobra dentro do corpo é o Super do **p14**. Ele é `advance(..., passThrough = true)`, ou seja, passa por dentro do rival de propósito. Também não é da entrada.

**Arquivos (linhas na cópia):**

- `CombatConfig.java:449-453`: `dhcEnterSpeed = 36`, `dhcEnterMaxFrames = 12`.
- `CombatFighter.java:81-83`: `dhcFromX`, `dhcToX`, `dhcEnterFrame` e `dhcEnterFrames`.
- `CombatEngine.java`:
  - `2843-2892`: `dhc()` calcula o lugar sem entrar no corpo e começa a entrada;
  - `2894-2905`: `advanceDhcEntry`;
  - `395`: chamada no passo 3.
- Teste `DhcEntradaTest` (3):
  - o p08 de parceiro do p07 desliza (antes, 275 px num quadro);
  - 15 titulares × 4 parceiros sem passo maior que 70 px e sem corpo dentro;
  - no canto, dentro da arena.

### Medida do H5a (mesmas sementes)

| medida | base | H5a |
|---|---|---|
| bateria fixa (148 métricas) | — | 22 mudam, todas dentro do ruído (no máximo 2,6 pontos, sempre 2x2); 1x1 idêntico |
| T20, dano do Super + DHC | 3 duplas que acertam o Super: igual | **igual em todas as duplas**; 1 a 8 quadros congelados a mais (a entrada) |
| T20, média Super + DHC | 4735 de dano, **2367 por barra** | 4735, 2367 por barra |
| T21, duplas oficiais (base → H5a) | — | Ryūga+Yin-lu 64→63, Kōga+p04 55→57, p05+p06 36→36, p07+p08 63→59, p09+p10 79→75, p11+p12 64→63, p13+p14 39→39, p15+p16 2→2 (ruído de ±8: 8 lutas por adversário) |
| T19, TAG perdido | 0% | 0% |

A matriz 1x1 16 × 16 e a sinergia C01 não foram rodadas de novo: o DHC só existe em dupla, e a C01 mede combo sem Super. As linhas 1x1 da bateria fixa deram idênticas.

### H5b: piso de 70% no dano do Super do DHC (medido sozinho, por cima do H5a)

O DHC continua fraco: 2367 de dano por barra, contra 3042 do Super sozinho. Por isso entrou o experimento, com o piso desligado no padrão.

- **Regra:** cada acerto do Super que entrou no DHC usa a escala do combo, mas nunca abaixo de `dhcDamageFloorPermille`.
- **Onde:**
  - `ComboSession.registerHit(..., minScale)`, linhas 56-66;
  - `CombatEngine` linha 3675;
  - a marca `CombatFighter.dhcSuper`, que é limpa em `clearAttack` e em `startAttack`.
- **Com 0:** a bateria fixa sai **idêntica ao H5a (148 de 148)**.

Medido com 700:

| dupla | Super + DHC (H5a) | com piso 700 |
|---|---|---|
| Ryūga + Yin-lu | 4358 | 4434 |
| Kōga + p04 | 4504 | 4573 |
| p07 + p08 | 4934 | 5077 |
| p09 + p10 | 5968 | 6063 |
| p11 + p12 | 3910 | 3928 |
| p05+p06, p13+p14, p15+p16 | não sai no combo | não sai no combo |
| **média, dano por barra** | **2367** | **2408 (+1,7%)** |

- Bateria fixa e T21 com o piso: tudo dentro do ruído. A CPU fez 264 DHCs em 400 lutas, contra 263 sem o piso.
- **Leitura:** o piso quase não muda nada. O DHC não é fraco pelo desconto do combo, e sim porque o Super do parceiro acerta pouco depois do Super do titular.
  - A parte do parceiro vai de 218 (Yin-lu) a 2283 (p04) de dano.
  - O p14 de parceiro dá 0 acertos com 7 dos 15 titulares no meio (p02, p04, p05, p08, p10, p13 e p15), porque o Super dele passa por dentro do rival caído.
  - **Recomendação:** não ligar o piso (o diff deixa 0).

**Achado: DHC fora do combo em 3 das 8 duplas oficiais (p05+p06, p13+p14, p15+p16).**
- O Super do titular dessas duplas acerta uma vez e erra o resto. Exemplo, o p05: acerta no quadro 11 de 121 e o rival cai no 84.
- A regra da G2b (o DHC espera as pancadas do Super acabarem) solta o DHC só depois que o rival já caiu.
- Antes da G2b, nessas duplas, o DHC saía sem somar dano (1998 = o Super sozinho, 1260 = idem), menos no p13+p14 (3758).
- **Proposta para decidir, não aplicada:** soltar o DHC pendente quando o rival estiver a ponto de sair do combo, ou seja, o hitstun restante ou o tempo até cair for menor que o início do Super do parceiro, mesmo com pancadas do Super do titular ainda por vir.

## Item 6: assists (p13 com H, p15 com 2H, p07): utilidade, alcance, segurança e sinergia

Nada no jogo mudou nesta parte. O `AssistSegurancaTest` (2 testes) fixa o que foi medido, e a `SondaAssist` tem a tabela completa.

| assist | utilidade e alcance (T17) | defendido em pé, sozinho (`SondaAssist`) | rival cruzando por cima | titular no canto / rival no canto |
|---|---|---|---|---|
| p07 | corpo a corpo, 100–450 px, antiaéreo 92%, cobertura do H defendido −2 → +11 | defendido de 150 a 450 px; **rival preso +32 q** depois do último contato | erra com o rival no ar sobre o titular; acerta quando ele cai do outro lado | acerta / acerta |
| p12 | 100–500 px, antiaéreo 33%, cobertura +9 | +32 q, de 150 a 450 px | igual | acerta / acerta |
| p13 (H) | 100–500 px, início 21 q, antiaéreo 92%, cobertura +14 | +35 q, de 150 a 450 px | acerta no começo do pulo (antiaéreo) | acerta / acerta |
| p14 | 100–550 px, antiaéreo 92%, cobertura +24 | **acerta quem defende agachado** (o mergulho é golpe alto); +44 q | acerta no começo do pulo | acerta / acerta |
| p15 (2H) | 100–400 px, lança, antiaéreo 100%, cobertura +6 | +29 q, de 150 a 350 px; a 450 px não chega | erra com o rival no ar sobre o titular | acerta / acerta |
| referência: Ryūga, p08, p04 (projéteis) | — | +21, +27, +31 q | erram com o rival no ar | acerta / acerta |

**Segurança:**
- O titular fica livre o tempo todo e o assist não tem corpo que apanhe. Defendido, todo assist deixa o rival preso de +21 a +44 quadros depois do último contato.
- Chamando o assist e passando a defender, a CPU nível 8 não acertou o titular em nenhuma das 20 sementes, a 150, 250 e 350 px. É o mesmo resultado do controle sem chamar, com a defesa por roteiro (`BateriaBalanco.defender`).
- Nenhum é inseguro, e nada foi corrigido.

**Canto:**
- No ataque, o golpe do assist sai dentro da arena e acerta, nas duas situações.
- A entrada dele começa fora da arena: ele vem correndo de fora, desenhado assim de propósito.

**Sinergia nas duplas oficiais (T18, T21 e C01, sem mudança desde a lista G):**
- O assist do p12 soma +2555 ao combo do p11; p11+p12 passa de 50% para 63–64%.
- O do p14 soma +930 ao combo do p13; p13+p14 passa de 25% para 39%.
- O do p13 soma +531 em média aos outros.
- O da p15 soma +881 em média; com a p16 na dupla, segue em 2%. É a frente dos personagens fracos.
- O do p07, como antiaéreo, dá 92%; p07+p08 fica em 59–63%.

## Item 7: SHINKAI ENERGY, sem alterar

A `BateriaShinkaiBarra` mede luta completa. Cada tipo luta contra o mesmo time sem SHINKAI (a barra de SHINKAI do outro lado fica zerada), com CPU nível 8 dos dois lados, metade das lutas de cada lado e as mesmas sementes. No 1x1 são os 16 personagens × 8 lutas; no 2x2, 128 duplas sorteadas. O ruído é de cerca de ±5 no 1x1 e ±9 no 2x2.

| 1x1 | vence | dano na janela | barra ganha na janela | gasta depois da janela (por luta) | em que | dano por barra gasta depois | barra que sobrou no fim |
|---|---|---|---|---|---|---|---|
| sem SHINKAI (controle) | 52% | — | — | — | — | — | 2,63 |
| POWER | **74%** | 3565 | 0,92 | 1,09 | Super 39%, Ultra 36%, Breaker 20% | 602 | 2,71 |
| SPEED | 68% | 2749 | 0,80 | 1,20 | Ultra 39%, Super 33%, Breaker 23% | 594 | 2,79 |
| ENERGY | 69% | 2885 | **2,28** | 1,20 | **Ultra 47%**, Super 29%, Breaker 19% | **821** | **3,38** |
| TECHNIQUE | 63% | 2719 | 0,69 | 1,09 | Ultra 39%, Super 31%, Breaker 24% | 578 | 2,69 |

| 2x2 | vence | dano na janela | barra ganha na janela | gasta depois | em que | dano por barra gasta depois | sobrou |
|---|---|---|---|---|---|---|---|
| sem (controle) | 51% | — | — | — | — | — | 2,84 |
| POWER | **71%** | 3328 | 0,65 | 4,03 | Ultra 34%, Breaker 25%, Super 24%, Guard Cancel 11%, DHC 4% | 663 | 3,12 |
| SPEED | 59% | 2868 | 0,62 | 4,25 | Ultra 34%, Breaker 26%, Super 23%, GC 9%, DHC 5% | 658 | 2,95 |
| ENERGY | 55% | 2793 | **1,63** | 4,25 | **Ultra 40%**, Breaker 25%, Super 18%, GC 10%, DHC 5% | 679 | **3,48** |
| TECHNIQUE | 56% | 2915 | 0,62 | 3,81 | Ultra 33%, Super 25%, Breaker 24%, GC 10%, DHC 4% | 692 | 3,16 |

**Leitura:**
- **Em luta completa o ENERGY não é o pior:**
  - no 1x1, 69% contra 68% do SPEED e 63% da TECHNIQUE;
  - no 2x2, 55%, empatado com a TECHNIQUE (56%) dentro do ruído.
  - O POWER é o mais forte nos dois.
  - O "+700" do T15 é só a janela de 8 s contra uma defesa ativa. Na luta inteira, a barra extra volta como Ultra.
- **A barra acumulada:**
  - o ENERGY ganha de 2,5 a 3 vezes mais barra na janela: 2,28 contra 0,7–0,9 no 1x1;
  - gasta o mesmo depois (1,20 por luta);
  - transforma mais em Ultra (47%), o que dá o maior dano por barra gasta no 1x1 (821, contra 578–602);
  - mas termina a luta com **0,6–0,7 barra a mais sobrando**, que nunca foi usada (3,38 contra 2,69–2,79 no 1x1; 3,48 contra 2,95–3,16 no 2x2). Parte do ganho do ENERGY fica parada.
- **Em todos os tipos, e também sem SHINKAI**, a CPU termina a luta com 2,6–3,5 barras sem usar.
  - Isso é da CPU, não do ENERGY.
  - Pesa na leitura do ENERGY: o valor dele depende de alguém gastar a barra.
  - Com humanos, ou com uma CPU que gaste a barra, o ENERGY tende a render mais do que aqui.

## O que precisa de decisão

1. **H5a** (entrada do DHC deslizando, sem pular e sem entrar no corpo): pronta para integrar. Não muda dano nem vitórias.
2. **H5b** (piso de 70%): rende só +1,7% por barra. **Recomendo não ligar.** O diff deixa a opção desligada, com teste, caso queira repetir.
3. **DHC fora do combo** em p05+p06, p13+p14 e p15+p16: aprovar ou não a regra de soltar o DHC pendente quando o rival estiver saindo do combo. Também fica aberto o Super do p14 de parceiro, que atravessa o rival caído e erra.
4. **ENERGY:** não alterar, como pedido. O ponto a acompanhar é a CPU que não gasta a barra: todos os tipos terminam com 2,6–3,5 barras paradas. É assunto da CPU, para decidir depois da matriz.
