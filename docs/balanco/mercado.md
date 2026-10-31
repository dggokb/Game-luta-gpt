# Jogabilidade comparada com o mercado (31/10)

Pedido: simular no PC e comparar a jogabilidade com os grandes do gênero.

**Como foi medido:**
- **Nosso jogo:** `BateriaMercado` (nova), 400 lutas CPU × CPU por modo, no motor de verdade, em 2x2 e
  1x1, com a CPU nos níveis 8 e 5. Os dados fixos saem do pacote de cada personagem. O potencial de
  combo vem da `BateriaCombos t08` e a quebra de guarda da `t14`.
- **Mercado:** wikis de frame data (FAT/SuperCombo, Dustloop, Wavu, 2XKO wiki), manuais e notas de
  patch. Os personagens de referência são Ryu (SF6), Sol (Strive), Kazuya (T8), Kyo (KOF XV),
  Goku SS (DBFZ) e Darius/Ahri (2XKO). As fontes estão no fim do documento.

**Ressalvas:**
- Quem joga aqui é a CPU, dos dois lados. Os números de ritmo e de conversão dizem como a CPU joga.
  O que o motor permite está nos dados fixos e no potencial de combo.
- O mercado não publica velocidade de andar comparável (cada jogo usa a própria unidade) nem o
  tamanho do personagem na tela. Esses itens ficaram fora da comparação.

```
cd android && gradle -q -I <init com a tarefa rodarBateria> :pc:rodarBateria \
  -Dclasse=com.gamelutagpt.BateriaMercado "-Dargs=tudo 400 8"
```

## Resumo

| Tema | Nosso jogo | Mercado | Leitura |
|---|---|---|---|
| Parada no acerto (hitstop) L/M/H | **5 / 7 / 9** | SF6 9/11/13 · Strive 11–15 · DBFZ 6–16 | **O mais baixo de todos.** O golpe pesa menos e sobra menos tempo para confirmar o combo. |
| Vantagem na defesa L / M / H | **+2 / +1 / −2** (2H −4) | SF6 −1 / ~ / −2 (2HP −7) · Strive −2 / +3 / −5 · DBFZ −3 / ? / −8 · 2XKO −1 / −4 / −9 | **Normais muito seguros:** atacar quase não tem risco. |
| Quebra de guarda só defendendo | **3,5–7 s** (11–20 s com pushblock) | SF6 sem quebra (o Burnout leva ~20 s) · Strive e T8 não têm | **A defesa dura muito pouco.** Somado ao item acima, o ataque domina. |
| Normal mais rápido | **3 quadros** (2–5) | SF6 4 · KOF 4 · Strive 5 · DBFZ 6 · 2XKO 6–7 · T8 10 | Mais rápido que todos. |
| Dano dos normais (% da vida) L/M/H | **2,1 / 3,6 / 5,7%** | SF6 3 / 6 / 8% (Ryu) | ~30% menos por golpe: a vida é maior (mediana 13720). |
| Combo simples (L M H Super) | **~16%** (mediana; 10–34%) | BnB sem barra: SF6 20–23% · KOF 22–27% · Strive 14–30% | Abaixo do mercado. |
| Melhor rota sem barra | **29–53%** (mediana ~45%) | SF6 ~23% · KOF ~27% · DBFZ ≥50% | Nível de DBFZ, mas exige rotas de até 16 passos com assist e malabarismo. |
| Distância entre o simples e o ótimo | **~16% → ~45%** | SF6 ~20% → ~30% com Drive | **Grande demais:** quem não decora rota longa tira um terço do dano. |
| Combos de 3+ acertos por queda (CPU) | **3,1** | SF6 4–5 toques · Strive 3–5 · DBFZ ~2 por personagem | Na faixa, mas 56% do dano vem de acerto solto. |
| Duração por personagem derrubado (CPU) | **45–55 s** | SFV, top 8 da Evo: ~39–55 s por round | Na faixa. |
| Relógio | **120 s** para a luta 2x2 inteira; **6–17%** das lutas acabam no tempo | DBFZ 300 s por 3 personagens · UMvC3 99 contagens (~150–200 s reais, e o relógio para no hitstop) | **Curto para um jogo de dupla.** |
| Pulo | **3 + 41 + 3 = 47 quadros** | SF6 45 · DBFZ 45 · 2XKO 46 · Strive 42–47 | Igual. |
| Super pulo | **~104 quadros no ar** | Strive 53 · DBFZ 55–66 | **O dobro:** flutua demais. |
| Backdash | 15 + 12 = 27 quadros | SF6 23 · Strive 20 · DBFZ 15 · KOF 24 | Na faixa (um pouco lento). |
| Agarrão: início / janela de escape | 5 / 12 quadros, 1200 de dano (~9%) | SF6 5 / ~?, 1200 (12%) · 2XKO 6 / 11 · T8 12–14 / 20 | Na faixa. A CPU quase não agarra: 0,1–0,3 por luta. |
| Tempo caído (queda + chão + levantar) | **13 + 50 + 16 = 79 quadros** | Strive: knockdown longo 55 · DBFZ 34 · 2XKO 30 | **Longo:** atrasa o ritmo. |
| Buffer: especial / movimento | 10 / 15 quadros | T8 8 · Strive ~5 | Mais tolerante; para o toque no celular está certo. |

## Ritmo e combos nas lutas (CPU × CPU)

| Medida | 2x2, nível 8 | 2x2, nível 5 | 1x1, nível 8 | 1x1, nível 5 |
|---|---|---|---|---|
| Duração da luta, mediana (p10–p90) | 112 s (85–133) | 102 s (74–125) | 52 s (36–65) | 47 s (30–60) |
| Duração por personagem derrubado | 55 s | 47 s | 51 s | 45 s |
| Lutas que acabam no tempo | 17% | 6% | 0% | 0% |
| Tempo em interação (alguém apanhando, defendendo ou caído) | 57% | 60% | 54% | 56% |
| Distância no neutro (mediana) | 277 px (22% da tela) | 260 px | 242 px (19%) | 242 px |
| Dano por segundo (os dois lados) | 5% de uma vida | 6% | 5% | 5% |
| Aberturas com dano por queda | 23,5 | 21,5 | 21 | 21 |
| Aberturas de 1 acerto só | 78% | 74% | 75% | 74% |
| De onde vem o dano: 1 acerto / 2 / 3+ | 56% / 11% / 33% | — | 56% / 10% / 34% | — |
| Combos de 3+ por queda | 3,2 | — | 3,1 | — |
| Combo de 3+: dano mediano / p90 / máximo | 17% / 34% / 67% | 17% / 33% / 69% | 13% / 29% / 61% | 13% / 29% / 74% |
| Duração da abertura: mediana / p90 | 0,48 / 1,52 s | 0,52 / 1,63 s | 0,62 / 1,60 s | 0,62 / 1,48 s |
| Por luta: defendidos / agarrões | 26,5 / 0,2 | 16 / 0,3 | 13,6 / 0,1 | 8,5 / 0,1 |
| Por luta: Supers / Ultras / Breakers | 8,0 / 0,3 / 2,3 | 9,8 / 0,2 / 0,8 | 3,0 / 0 / 0,9 | 3,5 / 0 / 0,3 |
| Por luta: assists / trocas / SHINKAI | 18,2 / 12,4 / 3,7 | 13,7 / 7,6 / 3,3 | — | — |
| Por luta: quebras de guarda | 0,2 | 0,1 | 0,1 | 0 |

## Dados fixos dos normais (mediana do elenco; mínimo a máximo)

| Golpe | Início | Ativos | Recuperação | Vantagem na defesa | Vantagem no acerto | Parada | Dano (% da vida) |
|---|---|---|---|---|---|---|---|
| L | 3 (2–5) | 3 | 7 (5–12) | +2 (+1 a +2) | +5 | 5 | 2,1% |
| M | 6 (4–9) | 4 | 11 (8–16) | +1 (−1 a +1) | +4 | 7 | 3,6% |
| H | 10 (8–17) | 5 | 18 (15–24) | −2 | +5 | 9 | 5,7% |
| 2L | 3 (2–5) | 3 | 7 (6–11) | +2 (0 a +2) | +5 | 5 | 2,1% |
| 2M | 7 (6–16) | 4 | 13 (11–31) | +1 | derruba | 7 | 3,6% |
| 2H | 10 (8–14) | 6 | 18 (14–24) | −4 (−9 a −4) | derruba | 9 | 5,7% |

Vantagem = atordoamento − (ativos − 1 + recuperação), a partir do 1º quadro ativo. Toda a vantagem é
igual para o elenco inteiro: os pacotes mudam o início e a recuperação, e o atordoamento acompanha.

Vida: mediana 13720 (12600–16100). Corpo: 72 × 238 px, ou 33% da altura da tela. Mundo: 2600 px,
2 telas. Andar: 300 px/s para frente e 220 px/s para trás (23% e 17% da tela por segundo).

## O que isso diz da jogabilidade

1. **O ataque não tem risco e a defesa acaba rápido.**
   - Os L e M ficam positivos na defesa e o H fica em −2. No mercado o H fica de −5 a −10.
   - A guarda quebra em 3,5–7 s só defendendo.
   - Strive e Tekken não têm quebra de guarda; o SF6 leva ~20 s até o Burnout.
   - Para quem defende (e para o iniciante contra a CPU), a sensação é de estar sempre preso.
   - É o ponto mais distante do mercado.
2. **O golpe pesa pouco.**
   - A parada no acerto (5/7/9 quadros) é a menor de todos os jogos comparados. O SF6 usa 9/11/13
     e o Strive, 11–15.
   - Além do impacto visual, a parada é o tempo que o jogador tem para ver que acertou e continuar
     o combo.
   - Com 5 quadros, confirmar um L é coisa de reflexo de profissional. Isso ajuda a explicar 75–78%
     de aberturas de um acerto só.
3. **O combo simples rende pouco e o ótimo rende muito.**
   - Um L M H Super tira ~16% (mediana). O BnB sem barra do SF6 e do KOF tira 20–27%.
   - A melhor rota sem barra chega a ~45%, nível de DBFZ, mas pede até 16 passos com assist e
     malabarismo.
   - Os jogos grandes fazem o simples valer perto de 2/3 do ótimo (o Modern do SF6 dá 80% do dano
     nos supers assistidos). Aqui o simples vale 1/3.
4. **O relógio é curto para uma dupla.**
   - São 120 s para derrubar dois personagens; cada queda leva ~47–55 s, perto do round inteiro de
     um jogo 1x1.
   - 6–17% das lutas da CPU acabam no tempo.
   - O DBFZ dá 300 s para três personagens. No UMvC3 o relógio para no hitstop e nos freezes.
5. **O ritmo de base está no lugar.**
   - Pulo de 47 quadros, agarrão de 5, backdash de 27 e ~50 s por queda: está tudo na faixa
     de SF6, Strive e 2XKO.
   - Os dois pontos lentos são:
     - o **super pulo** (~104 quadros no ar, o dobro do Strive e do DBFZ);
     - o **tempo caído** (79 quadros, contra 30–55).
6. **A CPU não mostra o jogo inteiro.**
   - Agarra 0,1–0,3 vez por luta. No SF6, o agarrão é metade do jogo de pressão.
   - Converte pouco: 3 combos por queda e 56% do dano em acertos soltos.
   - Para quem joga sozinho no celular, a CPU é a jogabilidade. Agarrar mais e confirmar mais
     aproximaria a luta do que o motor já permite.

## Propostas (não aplicadas: são balanço e pedem decisão)

Cada uma entra num commit isolado, com antes e depois na `BateriaMercado` e registro em
[ajustes.md](ajustes.md).

| # | Proposta | Valor de hoje → proposto | O que muda |
|---|---|---|---|
| 1 | Parada no acerto maior | 5/7/9 → 8/10/12 (`hitstopFrames` dos normais) | impacto e tempo para confirmar; a luta fica ~3% mais lenta |
| 2 | H e 2H mais arriscados na defesa | −2/−4 → −5/−7 (menos atordoamento na defesa) | punição real; o M fica em −1 e o L em 0/+1 |
| 3 | Quebra de guarda mais lenta | gasto 10 → 5 por quadro de atordoamento (3,5–7 s → 7–14 s) | a defesa vale; o pushblock continua sendo a saída |
| 4 | Combo simples valendo mais | o escalonamento do 3º e 4º acerto mais suave, ou +15–20% no dano dos normais | L M H Super de ~16% → ~22% sem mexer no teto |
| 5 | Relógio da dupla | 120 s → 180 s, ou parar no Super/Ultra | menos luta decidida no tempo |
| 6 | Super pulo mais rápido | ~104 → ~70 quadros no ar | menos flutuação, na linha do DBFZ |
| 7 | Tempo caído menor | chão 50 → 30 (79 → 59 quadros) | luta mais rápida depois da queda |
| 8 | CPU: agarrar e confirmar | agarrar na pressão de perto; continuar o combo depois de um acerto | a CPU passa a mostrar o jogo |

Sugestão de ordem: 1, 2 e 3 juntas, porque mexem no mesmo equilíbrio entre ataque e defesa.
Depois 8, 4 e 5; por último 6 e 7.

## Aplicado em 31/10

As propostas 1, 2 e 3 e o relógio (180 s) entraram em commits separados. O antes e depois está em
[ajustes.md](ajustes.md), na seção "Ataque × defesa e relógio, pela comparação com o mercado".

| Tema | Antes | Agora | Mercado |
|---|---|---|---|
| Parada no acerto L/M/H | 5 / 7 / 9 | 8 / 10 / 12 | SF6 9/11/13 |
| Vantagem na defesa H / 2H | −2 / −4 | −5 / −7 | −5 a −10 |
| Quebra de guarda só defendendo | 3,5–7 s | 8,6–16 s | SF6 ~20 s; Strive e T8 sem quebra |
| Lutas 2x2 no tempo | 6–17% | 0% | — |

Continuam abertos: a CPU agarrando e confirmando (4), o combo simples (5), o super pulo (7) e o
tempo caído (8).

## Fontes

- SF6, Strive, 2XKO (frame data): FAT, `SF6FrameData.json`, `GGSTFrameData.json`,
  `2XKOFrameData.json` (github.com/D4RKONION/FAT).
- Strive: dustloop.com/w/GGST/Mechanics, /Damage, /Sol_Badguy/Data, /Sol_Badguy/Combos.
- Tekken 8: wavu.wiki (Throw, Input_buffer, Heat, Rage, Combo, Damage, Kazuya); notas do patch 3.02.01.
- KOF XV: planilha de frame data da comunidade (Kyo); manual da SNK; Dream Cancel (combos e barras).
- DBFZ: dustloop.com/w/DBFZ/Mechanics, /Frame_Data, /Damage, /Goku_(Super_Saiyan).
- UMvC3: wiki.supercombo.gg/w/UMVC3; srk.shib.live (Timer).
- 2XKO: wiki.play2xko.com (Mechanics, Fuses).
- SF6, Drive e supers: eventhubs.com (custos do Drive, dano dos supers), words.infil.net (review do SF6).
- Duração de round (SFV): eventhubs.com, 28/02/2020.
