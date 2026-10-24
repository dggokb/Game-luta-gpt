# Mecânicas por personagem — v4 (números e mecânicas no jogo)

Documento de trabalho. Traduz o estilo de cada ficha (`pNN.md`) em números de jogo.
**Vida, dano, tempo dos golpes, projéteis (seção 3) e as mecânicas únicas (seção 4) já estão
no jogo (14/10).** A ideia é jogar, rever e ajustar.

Ordem combinada:

1. **Mecânica** (este documento): vida, movimento, dano, velocidade dos golpes, mecânica única.
2. **Frame data**: cada golpe ganha o tempo que o design pede.
3. **Animação**: o vídeo é comprimido por igual até caber nesse tempo, com os quadros
   escolhidos sozinhos (amostra uniforme, sempre com o quadro do impacto). A arte deixa de
   ditar o tempo do golpe (hoje o `ritmo.py` faz o contrário).

Legenda: ✅ só muda dados (dá para fazer já) · 🔧 precisa de código novo no motor.

---

## 1. Moldes (arquétipos)

Valores relativos ao `player_base` (vida 10.000, andar 300, dash 620, L 2/4/4 frames).
"Velocidade dos golpes" multiplica o tempo dos normais: 1,3× = 30% mais lentos.

| Molde | Quem | Vida | Mobilidade | Dano dos normais | Velocidade dos golpes | Alcance | Jogo |
|---|---|---|---|---|---|---|---|
| **Equilibrado** | p01 | 10.000 | padrão | 1,0× | 1,0× | médio | Projétil + anti-aéreo + punição. Referência do elenco |
| **Rushdown** | p03, p10, p02 | 9.300–9.800 | especiais que avançam | 0,85–1,0× | 0,85–0,95× | curto | Entra, pressiona, combos longos |
| **Brawler** | p04, p07 | 10.300–10.800 | padrão | 1,05–1,2× | 0,95–1,25× | curto/médio | Troca golpe, contra-ataca, dano consistente |
| **Grappler** | p09 | 11.500 | padrão (lento nos golpes) | 1,25× | 1,3× | curto | Aguenta, chega perto e agarra |
| **Zoner** | p06, p08, p11 | 9.000–9.500 | padrão | 0,8–0,95× | 1,0× | longo | Controla espaço, pune quem entra |
| **Mixup** | p05 | 9.200 | teleporte, wall jump | 0,9× | 0,85× | curto | Mobilidade, teleporte, adversário nunca sabe de onde vem |
| **Berserker** | p12 | 9.500 | padrão | 1,1× (até 1,4× com pouca vida) | 1,0× | curto | Aguenta golpe, troca vida por poder |

> **Decidido (14/10):** andar, dash e pulo continuam **iguais para todos** (`CombatConfig`).
> A diferença de mobilidade vem dos especiais (teleporte do p05, air dash extra da p10).
> ✅ Vida (`maxLife`) e dano já são por personagem.

---

## 2. Os 12 lado a lado

| # | Molde | Vida | Dano | Golpes | Mecânica única | Forte contra | Fraco contra |
|---|---|---|---|---|---|---|---|
| **p01** | Equilibrado | 10.000 | 1,0× | 1,0× | **Onda carregável:** segurar S deixa a Onda maior e mais rápida 🔧 | quem pula muito (anti-aéreo) | rushdown que não dá espaço |
| **p02** | Rushdown / contra | 9.600 | 0,9× | 0,9× | **Contra-golpe alto:** S3 desvia golpes altos e médios e responde 🔧 | brawlers lentos (p07, p09) | zoners (p08, p06) |
| **p03** | Rushdown | 9.800 | 1,0× | 0,95× | **Rekka:** S3 com até 3 investidas, cada uma com novo input 🔧 | zoners (p06, p11) | contra-golpes (p02, p07) |
| **p04** | Brawler | 10.300 | 1,05× | 0,9× (só socos) | **Esquiva que cancela:** S3 passa por baixo de golpe alto e de projétil e pode cancelar em soco 🔧 | projéteis curtos | quem tem alcance (p08) |
| **p05** | Mixup | 9.200 | 0,9× | 0,85× | **Teleporte** (S3, à frente ou atrás) e **wall jump** 🔧 | defensivos (p01, p07) | grappler (p09) se for pego |
| **p06** | Zoner / trapper | 9.500 | 0,85× | 1,0× | **Clone** que avança sozinho (S2) e **puxar** à distância (S3) 🔧 | grappler (p09) | rushdown (p03, p10) |
| **p07** | Brawler pesado | 10.800 | 1,2× | 1,25× | **Capangas no Super** (entram e batem sozinhos) 🔧 | rushdown impulsivo (p03) | zoners (p08, p11) |
| **p08** | Zoner longo | 9.300 | 0,95× | 1,0× | **Chicote:** H, 2H e jH com quase o dobro do alcance ✅ (é só `reach` e hitbox) | lentos (p07, p09) | mixup (p05), quem passa por baixo (p04) |
| **p09** | Grappler | 11.500 | 1,25× | 1,3× | **Agarrão de comando** (meia-lua + agarrão, mais alcance e dano) e **armadura** no S3 e S4 🔧 | rushdown (p03, p10, p12) | zoners (p06, p08, p11) |
| **p10** | Rushdown acrobático | 9.300 | 0,85× | 0,85× | **Mobilidade aérea:** 2 air dashes e voadora que cruza o adversário ✅ air dash já existe, 🔧 o segundo | zoners (p06, p11) | grappler (p09) |
| **p11** | Zoner / trapper | 9.000 | 0,8× | 1,0× | **Veneno:** S2 e alguns golpes tiram vida ao longo do tempo 🔧 | tanques (p09, p07) | rushdown (p03, p10) |
| **p12** | Berserker | 9.500 | 1,1× → 1,4× | 1,0× | **Fúria:** dano cresce com a vida perdida, especiais custam vida, S3 com super armor 🔧 | rushdown (troca golpe e ganha) | zoners (não chega perto) |

**Ciclo de forças:** zoner > grappler/pesado > rushdown > zoner. p01, p04, p05 e p12 ficam
no meio e se viram contra todos. Cada dupla da história mistura dois lados do ciclo: p09
(tanque) + p10 (ágil), p05 (perto) + p06 (longe), p07 (pesado) + p08 (longe).

### ⚠ Em aberto: mecânica única da p06

O rascunho dava duas (clone no S2 e puxar no S3). Opções para ter **uma** marca própria
(puxar continua como especial normal dela):

| Opção | Como funciona | Animação nova? |
|---|---|---|
| **Eco de sombra** (sugerida) | Ao ativar, uma sombra roxa dela repete cada golpe meio segundo depois, por alguns segundos. Pressão e confusão de longe e de perto | Não: a sombra usa os próprios sprites da p06 pintados de roxo |
| Poça de sombra | O S1 deixa uma poça no chão por alguns segundos; quem pisa fica lento | Só o efeito da poça |
| Troca com o clone | O clone fica parado na tela; apertar de novo troca ela de lugar com ele | Não, mas parece com o teleporte do p05 |

### ⚠ Ponto para decidir: contra-golpe demais

Cinco fichas têm "contra-golpe" no S3: **p02, p07, p08, p11 e p09** (o escudo). Se forem
iguais, viram o mesmo golpe com outra roupa. Proposta para cada um ser diferente:

| Quem | Como funciona |
|---|---|
| p02 | Pega golpe **alto e médio**, resposta rápida e de pouco dano |
| p07 | Pega **qualquer golpe corpo a corpo**, mas é lento para sair. Resposta pesada |
| p08 | Pega golpe **baixo** (o adversário que tenta passar por baixo do chicote) |
| p11 | Pega qualquer golpe e a resposta **envenena** em vez de dar dano alto |
| p09 | **Não é contra-golpe, é armadura:** aguenta 1 golpe e responde com a cabeçada |

---

## 3. O que está no jogo (aplicado em 14/10)

Aplicado por `tools/sprites/mecanicas.py` a partir de `tools/sprites/mecanicas.json` (é lá que
se muda um número; depois `build_characters.py --write`). O que foi aplicado:

- **Vida, dano e projétil** (velocidade e alcance) de cada um.
- **Tempo dos golpes:** tempo base × ritmo do personagem. A vantagem no hit e na defesa é
  a mesma para todos (L +5/+2, M +4/+1, H +5/−2…), então os links continuam funcionando.
- **Cadência da animação:** cada quadro ganha o tempo que tinha no vídeo (142 de 168
  golpes têm receita de vídeo), e o quadro do impacto aparece exatamente no frame em que o
  golpe acerta (teste `test_mecanicas.py`).
- **p01 mantém o tempo dos golpes** (`manterTempo`): a arte dele é desenhada quadro a
  quadro e calibrada por testes (o 2M precisa de 40 frames para mostrar as 16 poses, o H tem
  52 frames). Só vida, dano e projétil seguem a tabela. ⚠ Por isso o H do p01 (0,87s) ficou
  mais lento que o do p09 (0,70s): a decidir se encurta (perdendo poses) ou fica.
- **Não muda:** alcance e caixas (vêm do DEBUG > AJUSTAR CAIXAS), andar/correr/pular, Super
  (só o dano) e as mecânicas únicas 🔧, que ainda precisam de código.
- O `ritmo.py` não estica mais o tempo dos golpes pela arte (a mecânica decide).

Tempo base (ritmo 1,0×): L 3/3/7 · M 6/4/11 · H 10/5/18 · 2L 3/3/7 · 2M 7/4/13 · 2H 10/6/18 ·
jL 4/3/6 · jM 6/4/8 · jH 9/5/10 · S2 6/8/22 · S3 18/4/18 · S4 10/14/14 · S1 10/1/18 (frames, 60 por segundo).

### Os 12 (dano · tempo total)

| # | Molde | Vida | L | M | H | S1 (projétil) | Super |
|---|---|---|---|---|---|---|---|
| **p01** | Equilibrado | 10.000 | 300 · 0.18s | 500 · 0.30s | 800 · 0.87s | 850 · vel 760 · alc 720 | 3200 |
| **p02** | Rushdown / contra-golpe | 9.600 | 270 · 0.20s | 450 · 0.32s | 720 · 0.48s | 760 · vel 700 · alc 260 | 2880 |
| **p03** | Rushdown | 9.800 | 300 · 0.22s | 500 · 0.33s | 800 · 0.53s | 850 · vel 950 · alc 380 | 3200 |
| **p04** | Brawler | 10.300 | 320 · 0.20s | 520 · 0.32s | 840 · 0.48s | 890 · vel 800 · alc 200 | 3360 |
| **p05** | Mixup | 9.200 | 270 · 0.20s | 450 · 0.28s | 720 · 0.45s | 760 · vel 1000 · alc 900 | 2880 |
| **p06** | Zoner / trapper | 9.500 | 260 · 0.22s | 420 · 0.35s | 680 · 0.55s | 720 · vel 480 · alc 800 | 2720 |
| **p07** | Brawler pesado | 10.800 | 360 · 0.28s | 600 · 0.45s | 960 · 0.67s | 1020 · vel 700 · alc 150 | 3840 |
| **p08** | Zoner longo | 9.300 | 280 · 0.22s | 480 · 0.35s | 760 · 0.55s | 810 · vel 1100 · alc 650 | 3040 |
| **p09** | Grappler | 11.500 | 380 · 0.28s | 620 · 0.45s | 1000 · 0.70s | 1060 · vel 600 · alc 900 | 4000 |
| **p10** | Rushdown acrobático | 9.300 | 260 · 0.20s | 420 · 0.28s | 680 · 0.45s | 720 · vel 900 · alc 720 | 2720 |
| **p11** | Zoner / trapper | 9.000 | 240 · 0.22s | 400 · 0.35s | 640 · 0.55s | 680 · vel 950 · alc 900 | 2560 |
| **p12** | Berserker | 9.500 | 330 · 0.22s | 550 · 0.35s | 880 · 0.55s | 940 · vel 700 · alc 150 | 3520 |

### p09 — Grappler (ritmo 1,3×, dano 1,25×)

| Golpe | Dano | s/a/r | Tempo | Hit / defesa |
|---|---|---|---|---|
| L | 380 | 4/4/9 | 0.28s | +5 / +2 |
| M | 620 | 8/5/14 | 0.45s | +4 / +1 |
| H | 1000 | 13/6/23 | 0.70s | +5 / -2 |
| 2L | 380 | 4/4/9 | 0.28s | +5 / +2 |
| 2M | 620 | 9/5/17 | 0.52s | +20 / +1 |
| 2H | 1000 | 13/8/23 | 0.73s | +25 / -4 |
| jL | 380 | 5/4/8 | 0.28s | +8 / +4 |
| jM | 620 | 8/5/10 | 0.38s | +7 / +3 |
| jH | 1000 | 12/6/13 | 0.52s | +6 / +2 |
| j2H | 1000 | 12/8/12 | 0.53s | +5 / +1 |
| S2 | 1380 | 8/10/29 | 0.78s | +10 / -12 |
| S3 | 1120 | 23/5/23 | 0.85s | +4 / -4 |
| S4 | 1250 | 13/18/18 | 0.82s | +10 / -6 |
| S1 | 1060 | 13/1/23 | 0.62s | projétil: vel 600, alcance 900 |
| Super | 4000 | (tempo mantido) | | |

### p10 — Rushdown acrobático (ritmo 0,85×, dano 0,85×)

| Golpe | Dano | s/a/r | Tempo | Hit / defesa |
|---|---|---|---|---|
| L | 260 | 3/3/6 | 0.20s | +5 / +2 |
| M | 420 | 5/3/9 | 0.28s | +4 / +1 |
| H | 680 | 8/4/15 | 0.45s | +5 / -2 |
| 2L | 260 | 3/3/6 | 0.20s | +5 / +2 |
| 2M | 420 | 6/3/11 | 0.33s | +20 / +1 |
| 2H | 680 | 8/5/15 | 0.47s | +25 / -4 |
| jL | 260 | 3/3/5 | 0.18s | +8 / +4 |
| jM | 420 | 5/3/7 | 0.25s | +7 / +3 |
| jH | 680 | 8/4/8 | 0.33s | +6 / +2 |
| j2H | 680 | 8/5/8 | 0.35s | +5 / +1 |
| S2 | 940 | 5/7/19 | 0.52s | +10 / -12 |
| S3 | 760 | 15/3/15 | 0.55s | +4 / -4 |
| S4 | 850 | 8/12/12 | 0.53s | +10 / -6 |
| S1 | 720 | 8/1/15 | 0.40s | projétil: vel 900, alcance 720 |
| Super | 2720 | (tempo mantido) | | |

**Vídeo × jogo:** os golpes no jogo duram de 4× a 20× menos que no vídeo. Até uns 6× a
compressão fica natural; acima disso o vídeo tem movimento sobrando antes ou depois do golpe,
e o certo é cortar o trecho (refazer a receita do clipe), não comprimir mais.

## 4. Mecânicas únicas no jogo (14/10)

Definidas no bloco `unica` de `tools/sprites/mecanicas.json` e aplicadas pelo mesmo
`mecanicas.py` (grava `mecanica` nos golpes e no lutador do `character.json`). O motor sabe
fazer: **armadura** (aguenta N golpes sem atordoar), **contra-golpe** (lê golpe alto, baixo ou
qualquer um na preparação e responde na hora), **esquiva** (passa por baixo de golpe alto e de
projétil, ou some de tudo), **avanço** (o golpe carrega o corpo; pode atravessar o rival; com velocidade negativa recua, como a estrela da p16),
**teleporte** (reaparece atrás), **puxar**, **veneno** (dano ao longo do tempo, nunca mata), **armadilha** (o projétil cai e espera um pavio por botão, L/M/H; depois um golpe sai dele, como a luva da caixa da p16),
**custo de vida**, **fúria** (dano cresce com a vida perdida), **air dashes extras** e
**agarrão próprio** (dano e alcance). Testes: `MechanicsTest`.

Na tela: aura dourada (armadura), anel ciano (postura de contra-golpe), véu branco (esquiva),
bolhas verdes (envenenado), brasas vermelhas (fúria) e as faixas CONTRA-GOLPE!, ARMADURA! e VENENO!.

| # | Mecânica no jogo |
|---|---|
| **p01** | Onda carregável: segurar o botão do S1 segura a Onda na cintura (até 1 s); solta e ela sai maior, mais rápida, mais longe e até 80% mais forte. |
| **p02** | Contra-golpe alto: o S3 lê golpes em pé e no ar durante a preparação e responde na hora. |
| **p03** | Rekka: o S3 avança e encadeia até 3 vezes (novo comando a cada investida); o S2 sobe avançando. |
| **p04** | Esquiva que cancela: o S3 passa por baixo de golpe alto e de projétil e pode virar soco (L, M, H); a cabeçada (S2) avança. |
| **p05** | Teleporte: o S3 some na fumaça e reaparece atrás do rival; o corte de mão (S4) avança baixo; no ar, encostado na parede, aperta pra cima e pula da parede. |
| **p06** | Eco de sombra: o S2 desperta a sombra por 5 s; cada golpe que ela dá é repetido pela sombra roxa um terço de segundo depois, com metade do dano. O S3 alcança longe e puxa o rival. |
| **p07** | Contra-golpe pesado: o S3 lê qualquer golpe corpo a corpo e responde com o calcanhar. No Super ele estala os dedos e dois capangas entram correndo por trás: um soca, o outro dá uma ombrada, e saem. |
| **p08** | Chicote: alcance longo nos fortes; contra-golpe baixo no S3 (pune quem tenta passar por baixo). |
| **p09** | Grappler: armadura de 1 golpe no escudo (S3) e na ombrada (S4, que avança); agarrão mais forte e de mais longe. |
| **p10** | Acrobata: dois air dashes por pulo, esquiva alta no S3 e voadora (S4) que atravessa o rival. |
| **p11** | Veneno: a seringa (S2) e a resposta do contra-golpe (S3, lê qualquer golpe) envenenam; o passo cirúrgico (S4) avança. |
| **p12** | Fúria: bate até 40% mais forte conforme perde vida; os especiais custam vida e a Fúria (S3) tem armadura. |

**Com vídeo novo (14/10):** a carga e o disparo carregado do p01 (`SPECIAL_CHARGE`,
`SPECIAL_ENERGY_CHARGED`) e o gesto do eco da p06 (`SPECIAL_ECHO`, no S2). O motor também
sabe **segurar o botão** (`FighterInput.holding`, dos dedos na tela), **eco de sombra** (cópia
roxa dos golpes, metade do dano, desenhada atrás dela) e **wall jump** (o p05 encosta na parede
no ar, aperta pra cima e sai empurrado para longe dela).

**Capangas (14/10):** o Super do p07 chama dois capangas (`CAPANGA_RUN`, `CAPANGA_PUNCH`,
`CAPANGA_SHOULDER`, no pacote do p07) em vez da onda: entram correndo por trás dele, o primeiro
soca, o segundo dá uma ombrada, e saem correndo; cada um leva metade do dano do Super. O
capanga não tem imagem inicial, então a escala é manual (225 px, um pouco maior que o p07).

**Wall jump (14/10):** animação própria do p05 (`WALL_JUMP`, 3º vídeo: agacha no ar contra a
parede invisível e sai de voadora), tocada enquanto dura o empurrão da parede.

Todas as mecânicas planejadas estão no jogo.

## 4b. Correntes de combo (16/10)

Bloco `combos` de `tools/sprites/mecanicas.json`, aplicado pelo mesmo `mecanicas.py`: soma as
rotas no `cancelInto` de cada golpe e grava `maxUsesPerChain` (usos do golpe numa corrente).
As rotas que o golpe já tinha ficam guardadas em `rotasProprias`; refazer dá sempre o mesmo
resultado. Cada personagem escolhe o estilo em `estiloCombo`; o lutador de teste
(`player_two`) e o boneco de treino (`monster_npc`) recebem só as correntes, pela lista
`combos.soCombos`.

**Base (todos):** a corrente só sobe de força, como no FighterZ.

| Golpe | Cancela em | Usos na corrente |
|---|---|---|
| L, 2L | L, 2L, M, 2M, H, 2H | L 3, 2L 2 |
| M | 2M, H, 2H | 1 |
| 2M | H (é rasteira: derruba) | 1 |
| H | só especial e Super (joga na parede) | 1 |
| 2H | pulo (lançador → super pulo) | 1 |
| jL | jM, jH, j2H | 1 |
| jM | jH, j2H, **pulo no ar** | 1 |

Rota de exemplo: L > M > 2H > super pulo > jL > jM > pulo no ar > jL > jM > j2H (chão) > Super.

| Estilo | Quem | Soma à base |
|---|---|---|
| equilibrado | p01 | nada |
| pressao | p03, p04, p10 | M > M; M cancela em pulo (no acerto e na defesa) |
| chutes | p02 | M > M (três chutes: M > M > H); jM > jM |
| aereo | p05 | jL > jL; jL cancela em pulo no ar; M cancela em pulo (sobe sem lançador) |
| alcance | p06, p08, p11 | L, 2L e 2M terminam no S1 |
| tanque | p07, p09 | H > H |
| imprevisivel | p12 | volta para trás: M > L/2L, H > L/2L/M; cada golpe até 2 vezes |

O estilo também guia a CPU (`CpuStyle`): distância, agressão, projétil, agarrão e paciência,
e o caderno de combos dela sai destas mesmas rotas (`docs/combat-engine.md`, seção CPU).

Trocar o estilo de alguém: mudar `estiloCombo`, rodar `mecanicas.py --write` e
`build_characters.py --write`. Testes: `CorrentesComboTest` e `test_mecanicas.py`.

## 5. Perguntas em aberto

1. **Os moldes fazem sentido?** Algum personagem está no molde errado? (ex.: p02 é mais
   rushdown ou mais contra-ataque?)
2. **Vida:** a diferença de 9.000 (p11) a 11.500 (p09) está boa ou é pouca/demais?
3. **Mecânicas únicas:** concorda com uma por personagem? Alguma que você imaginou diferente?
4. **Contra-golpes:** a separação da tabela acima resolve?
5. **p13–p16:** quando as fichas forem criadas, entram nos mesmos moldes.
6. **p01 carregar a Onda:** foi invenção minha a partir de "poder concentrado (Takuma)".
   Prefere outra coisa?
7. **Ordem de implementar:** sugestão abaixo.

## 6. Ordem sugerida de implementação

1. ✅ **Feito (14/10):** vida, dano, projéteis e frame data de todos (alcance ficou o do app).
2. ✅ **Feito (14/10):** cadência do vídeo nas animações dos golpes.
3. Jogar, rever e ajustar o que não fizer sentido (mexer no número e refazer a animação).
4. ✅ **Feito (14/10), menos os itens de "Ainda falta":** mecânicas únicas, uma por vez, das mais simples para as mais complexas: chicote (p08,
   já é dado) → armadura (p09, p12) → contra-golpes → esquiva (p04, p10) → rekka (p03) →
   veneno (p11) → teleporte e wall jump (p05) → clones, puxar e capangas (p06, p07).
