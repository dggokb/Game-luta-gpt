# Luta no chão comparada com SF6 e Fatal Fury: City of the Wolves (31/10)

Pedido: simular só a luta no chão, sem super pulo nem os sistemas próprios (SHINKAI, Ultra), e
comparar com SF6 e COTW, incluindo o tempo dos golpes, dos combos, da movimentação e da luta.
Objetivo: a luta no chão tão boa quanto a deles.

**Como foi medido:**
- **Simulação:** `BateriaMercado chao`, com 400 lutas 1x1 CPU × CPU por nível (8 e 5).
  - Ficaram de fora: super pulo, air dash, a perseguição do lançador, SHINKAI, Ultra, Breaker,
    assist e troca.
  - Continuam: pulo, dash, backdash, agarrão, especiais e Super.
  - A mesma luta com tudo ligado roda nas mesmas sementes, para comparar.
- **Dados fixos:** o pacote de cada personagem e a tabela de alcance real (o do desenho, da CPU).
- **SF6:** dataset do FAT (frame data e alcances), com Ryu, Ken e JP.
- **COTW:** planilha comunitária (patch 1.7.2) e manual da SNK, com Terry, Rock e B. Jenet.
- Fontes no fim.

**Ressalvas:**
- Quem joga é a CPU. O ritmo e o uso dos recursos dizem como a CPU joga; o que o motor permite está
  nos tempos e nas proporções.
- O alcance do FAT conta, ao que parece, a partir do centro do personagem. Os nossos alcances abaixo
  contam do eixo do atacante até a ponta do golpe. Por isso a comparação usa proporções, que não
  dependem da unidade de cada jogo.
- Faltam no COTW: velocidades, alcances, dano e hitstop. Faltam nos dois: a largura da tela e a
  duração dos combos.

```
gradle -q -I <init com rodarBateria> :pc:rodarBateria -Dclasse=com.gamelutagpt.BateriaMercado "-Dargs=chao 400 8"
```

## Resumo

| Tema | Nós | SF6 | COTW | Leitura |
|---|---|---|---|---|
| Duração do L / M / H | 13 / 21 / 33 quadros | 13 / 20–30 / 31–35 | 14–15 / 17–26 / 29–34 | **Igual** |
| Duração dos especiais (S2–S4) | 38–41 (31–100) | 47–66 | 38–49 | Igual ao COTW, mais curtos que o SF6 |
| Golpe mais rápido / agarrão | 3 / 5 quadros | 4 / 5 | 4 / 5 | O nosso L sai 1 quadro antes |
| Pausa no acerto L / M / H | 8 / 10 / 12 | 9 / 11 / 13 | n.e. | **Igual** (desde a última rodada) |
| Pulo / backdash / dash | 47 / 27 / 20 quadros | 45 / 23 / 19–22 | 41 (hop 36) / 23 / 13–15 | Igual |
| **Andar o alcance do próprio 2M** | **50 quadros** (2,1× a duração do 2M) | Ryu 31,5 (1,1×) · JP 47 (1,7×) | n.e. | **Andamos devagar para o tamanho dos golpes** |
| Andar para trás ÷ frente | 0,73 | 0,68 | n.e. | Igual |
| Dash ÷ alcance do 2M | 0,82 | 0,84–0,91 | n.e. | Igual |
| Velocidade do dash ÷ andar | 2,1 | 1,4–1,8 | n.e. | Dash rápido para o andar (outra vez o andar lento) |
| **Backdash ÷ dash (distância)** | **~1,2** (≈245 ÷ 207 px) | 0,74 | n.e. | **Fugir para trás leva mais longe que avançar** |
| **Agarrão ÷ alcance do 2M** | **0,30** | 0,54 | n.e. | **O agarrão é curto** |
| Agarrão: dano | 1200 = 8,7% da vida | 12% (20% no Punish Counter) | n.e. | Rende menos |
| L ÷ 2M (alcance) | 0,75 | 0,68–0,70 | n.e. | Igual |
| Pulo à frente ÷ alcance do 2M | 1,11 | 1,28 | n.e. | Perto |
| Dano L : M : H : agarrão | 1 : 1,7 : 2,7 : 4 | 1 : 2–2,3 : 2,7–3 : 4 | n.e. | O M rende pouco |
| **Vantagem na defesa: L / M / H** | **+2 / +1 / −5** | −1 / −1 a −5 / −2 a −3 | +1 a +2 (perto) / −1 a −5 / −5 a −8 | **O L e o M são seguros demais** |
| **2M (a nossa rasteira)** | **derruba, +1 na defesa** | rasteira 2HK: derruba, −10 a −12 na defesa | rasteira 2D: derruba, −11 | **A rasteira é segura: deveria ser o golpe mais arriscado** |
| 2H (o lançador) | −7 na defesa | — (o SF6 não tem lançador) | — | Perto do −6 dos pesados do COTW |
| **Contra-golpe** | só o H em pé contra quem ataca: +8 de pausa, sem dano nem quadro a mais | Counter +2 quadros; Punish Counter +4 quadros e mais dano; agarrão +70% | Counter +20% de dano, +2 / +5 quadros | **Acertar quem ataca quase não rende mais** |
| Defesa: janela da defesa perfeita | 5 quadros | Parry (barra de Drive) | Just Defense 7 quadros | Perto |

## A luta no chão na simulação (CPU × CPU)

| Medida | Só chão, CPU 8 | Com tudo, CPU 8 | Só chão, CPU 5 | Com tudo, CPU 5 |
|---|---|---|---|---|
| Duração da luta (mediana) | 46 s | 53 s | 39 s | 46 s |
| Tempo em interação | 60% | 55% | 61% | 57% |
| Distância no neutro | 326 px (25% da tela) | 241 px | 305 px | 241 px |
| Dano por origem | projétil 41% · normal 38% · especial 12% · Super 10% · agarrão 0% · ar 0% | normal 35% · projétil 32% · especial 17% · Super 15% · agarrão 1% · ar 1% | normal 40% · projétil 31% · Super 19% · especial 9% · agarrão 1% · ar 0% | normal 37% · projétil 26% · Super 22% · especial 13% · ar 1% · agarrão 1% |
| Acertos em quem estava atacando | 35% | 29% | 31% | 27% |
| Normais no chão que encostam (por minuto) | 73% (37) | 70% (33) | 73% (40) | 72% (34) |
| Pulos por minuto (os dois lados) | 4,3 | 13,0 | 3,5 | 10,8 |
| Entrada do ar: acerta / defendida / antiaéreo (por minuto) | 0,1 / 0,2 / 0,4 | 0,1 / 0,2 / 0,7 | 0,1 / 0,1 / 0,5 | 0,1 / 0,1 / 0,8 |
| Agarrões por luta | 0,1 | 0,1 | 0,1 | 0,1 |
| Aberturas de 1 acerto só | 80% | 76% | 76% | 73% |
| Combo de 3+: dano mediano / p90 | 11,5% / 21% | 13% / 29% | 12% / 23% | 13% / 29% |
| Combo de 3+: duração mediana / p90 (acertos) | 1,9 / 3,3 s (3) | 2,1 / 4,3 s (4) | 1,5 / 2,8 s (3) | 1,8 / 3,5 s (3) |

**Leitura:**
- **Sem os sistemas, a luta fica mais longe e mais de projétil.** O neutro se afasta de 19% para
  25% da tela, e o projétil vira a maior fonte de dano (41%).
- **No chão, quase ninguém pula** (4 pulos por minuto) e nenhuma entrada do ar acerta. Nos jogos de
  rua, pular e derrubar com antiaéreo é metade do neutro.
- **O agarrão não existe na prática:** 0,1 por luta e 0–1% do dano. No SF6 ele é o outro lado da
  pressão: defender não basta.
- **Um terço dos acertos pega quem estava atacando**, mas o motor não paga nada a mais por isso,
  exceto o H em pé.
- **O combo no chão tira pouco:** 11–12% de mediana. O BnB sem barra do SF6 tira 20–23%.

## O que falta para o chão ficar como o deles

Em ordem do que mais pesa:

1. **A rasteira (o 2M) fica +1 na defesa.**
   - Nos dois jogos a rasteira derruba, mas é o golpe mais arriscado do personagem: −10 a −12 no
     SF6 e −11 no COTW. Quem a defende pune.
   - A nossa também é o golpe baixo mais longo e derruba, mas não custa nada se for defendida.
     Por isso o melhor jeito de jogar no chão é soltar 2M de longe. Não há motivo para andar para
     dentro e para fora do alcance.
   - O papel do poke de rua (o 2MK do SF6: +1 no acerto, −6 na defesa, cancela em especial) cabe ao
     nosso M, que já cancela em S, S2, S3 e S4.
2. **O contra-golpe não paga.** Nos dois jogos, acertar quem está apertando botão rende mais:
   - no SF6, +2 a +4 quadros e mais dano;
   - no COTW, +20% de dano e +2 / +5 quadros.

   É isso que dá valor a esperar e punir no vazio. Aqui só o H em pé ganha pausa a mais.
3. **O andar é lento para o tamanho dos golpes.**
   - Andar o alcance do próprio 2M leva 50 quadros, 2,1 vezes a duração do golpe. No Ryu e no Ken
     leva 31 quadros, 1,1 vez.
   - Com o andar lento, entrar e sair do alcance (o que faz o neutro de rua) não cabe no tempo de
     um golpe.
4. **O L e o M ficam positivos na defesa** (+2 e +1). No SF6 eles ficam em −1 e entre −1 e −5;
   no COTW, em +1/+2 o L de perto e entre −1 e −5 o M. Pressão sem custo.
5. **O agarrão é curto e rende pouco.**
   - O alcance é 0,30 do 2M; no SF6 é 0,54.
   - O dano é 8,7% da vida; no SF6 é 12%.
   - Junto com a CPU que não agarra, defender sempre funciona.
6. **O backdash leva mais longe que o dash** (~245 contra 207 px). No SF6 o backdash leva 0,74
   do dash. Fugir para trás é melhor que avançar: a pressão no canto escapa fácil (já visto no T06).
7. **A CPU:** ela joga de projétil, quase não pula e não agarra. Para quem joga sozinho, é a CPU
   que mostra o jogo no chão. Ela precisa andar mais no limite do alcance, pular de vez em quando
   (para o antiaéreo existir) e agarrar de perto.

O que já está no nível deles: a duração dos golpes e dos especiais, o pulo, o dash e o backdash
em quadros, a pausa no acerto, o golpe mais rápido, o agarrão em quadros, o andar para trás contra
o andar para frente e o dash contra o alcance.

## Propostas (não aplicadas)

Cada uma vai num commit isolado, com antes e depois na `BateriaMercado chao` e registro em
[ajustes.md](ajustes.md).

| # | Proposta | Hoje → proposto | Referência |
|---|---|---|---|
| A | Rasteira arriscada | 2M continua derrubando; na defesa +1 → **−10** | SF6 2HK −10 a −12; COTW 2D −11 |
| B | Contra-golpe para todos os golpes | só H em pé, só pausa → **qualquer acerto em quem está no início ou na recuperação do golpe: +20% de dano e +2 (L) / +4 (M, H) quadros de atordoamento** | COTW +20%, +2 / +5; SF6 +2 / +4 |
| C | Andar mais rápido | 300 / 220 → **400 / 280 px/s** (andar o 2M: 50 → 38 quadros) | SF6 31–47 quadros |
| D | L e M na defesa (o M vira o poke de rua) | +2 / +1 → **0 / −2** | SF6 jab −1, 2MK −6, 5MP −1 |
| E | Agarrão | alcance 40 → **90 px** (0,30 → 0,50 do 2M); dano 1200 → **1650** (12%) | SF6 0,54; 12% |
| F | Backdash | ~245 → **~160 px** (velocidade 978 → 640) | SF6 0,74 do dash |
| G | CPU no chão | menos projétil; anda no limite do alcance; pula 1–2 vezes a cada 10 s; agarra de perto | — |

Sugestão de ordem:
- **A, B e D primeiro, juntas:** mudam o valor de cada botão no neutro.
- **Depois C e E:** espaço e agarrão.
- **F por último.**
- **G** pode andar em paralelo.

## Aplicado em 01/11

A, B, D e F entraram, cada uma em um commit. A E (agarrão) ficou de fora. O antes e depois está em
[ajustes.md](ajustes.md), na seção "Luta no chão: rasteira, contra-golpe, L/M na defesa e
backdash".

Medido depois: a CPU pune só 8% dos golpes inseguros que defende. As punições novas só aparecem
contra gente até a CPU aprender a punir (proposta G).

## Fontes

- SF6: FAT, `SF6FrameData.json` (github.com/D4RKONION/FAT); words.infil.net (SF6); redbull.com
  (Drive System).
- COTW: planilha comunitária de frame data
  (docs.google.com/spreadsheets/d/1zg8H1AgpgPBbAdpPdsGC3WRUEmGVajbv2Ea6b-FZ1kI, patch 1.7.2);
  pastebin.com/h3FBgCck (mecânicas); manual da SNK
  (snk-corp.co.jp/webmanual/fatalfury-cotw: battle e rule); snk-corp.co.jp/us/games/fatalfury-cotw/guide.
- Duração de round (SFV, referência): eventhubs.com, 28/02/2020.
