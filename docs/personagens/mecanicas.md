# Mecânicas por personagem — RASCUNHO v1

Documento de trabalho: **nada aqui está no jogo ainda.** Hoje os 12 lutadores têm os
mesmos números (vida 10.000, L tira 300, projétil tira 850, todos andam, correm e pulam
igual). Este rascunho traduz o estilo de cada ficha (`pNN.md`) em números de jogo, para a
gente refinar antes de implementar.

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

| Molde | Quem | Vida | Andar / dash | Dano dos normais | Velocidade dos golpes | Alcance | Jogo |
|---|---|---|---|---|---|---|---|
| **Equilibrado** | p01 | 10.000 | 1,0× | 1,0× | 1,0× | médio | Projétil + anti-aéreo + punição. Referência do elenco |
| **Rushdown** | p03, p10, p02 | 9.300–9.800 | 1,1–1,2× | 0,85–1,0× | 0,85–0,95× | curto | Entra, pressiona, combos longos |
| **Brawler** | p04, p07 | 10.300–10.800 | 0,85–1,0× | 1,05–1,2× | 0,95–1,25× | curto/médio | Troca golpe, contra-ataca, dano consistente |
| **Grappler** | p09 | 11.500 | 0,8× | 1,25× | 1,3× | curto | Aguenta, chega perto e agarra |
| **Zoner** | p06, p08, p11 | 9.000–9.500 | 0,9× | 0,8–0,95× | 1,0× | longo | Controla espaço, pune quem entra |
| **Mixup** | p05 | 9.200 | 1,2× | 0,9× | 0,85× | curto | Mobilidade, teleporte, adversário nunca sabe de onde vem |
| **Berserker** | p12 | 9.500 | 1,1× | 1,1× (até 1,4× com pouca vida) | 1,0× | curto | Aguenta golpe, troca vida por poder |

> 🔧 Andar, dash e pulo hoje são **globais** (`CombatConfig`). Para cada um ter os seus,
> esses valores precisam ir para o `character.json` (`fighter`). É uma mudança pequena no
> motor e é a primeira a fazer, porque quase todos os moldes dependem dela.
> ✅ Vida (`maxLife`) e dano já são por personagem.

---

## 2. Os 12 lado a lado

| # | Molde | Vida | Andar | Dano | Golpes | Mecânica única | Forte contra | Fraco contra |
|---|---|---|---|---|---|---|---|---|
| **p01** | Equilibrado | 10.000 | 300 | 1,0× | 1,0× | **Onda carregável:** segurar S deixa a Onda maior e mais rápida 🔧 | quem pula muito (anti-aéreo) | rushdown que não dá espaço |
| **p02** | Rushdown / contra | 9.600 | 340 | 0,9× | 0,9× | **Contra-golpe alto:** S3 desvia golpes altos e médios e responde 🔧 | brawlers lentos (p07, p09) | zoners (p08, p06) |
| **p03** | Rushdown | 9.800 | 320 | 1,0× | 0,95× | **Rekka:** S3 com até 3 investidas, cada uma com novo input 🔧 | zoners (p06, p11) | contra-golpes (p02, p07) |
| **p04** | Brawler | 10.300 | 310 | 1,05× | 0,9× (só socos) | **Esquiva que cancela:** S3 passa por baixo de golpe alto e de projétil e pode cancelar em soco 🔧 | projéteis curtos | quem tem alcance (p08) |
| **p05** | Mixup | 9.200 | 360 | 0,9× | 0,85× | **Teleporte** (S3, à frente ou atrás) e **wall jump** 🔧 | defensivos (p01, p07) | grappler (p09) se for pego |
| **p06** | Zoner / trapper | 9.500 | 270 | 0,85× | 1,0× | **Clone** que avança sozinho (S2) e **puxar** à distância (S3) 🔧 | grappler (p09) | rushdown (p03, p10) |
| **p07** | Brawler pesado | 10.800 | 250 | 1,2× | 1,25× | **Capangas no Super** (entram e batem sozinhos) 🔧 | rushdown impulsivo (p03) | zoners (p08, p11) |
| **p08** | Zoner longo | 9.300 | 280 | 0,95× | 1,0× | **Chicote:** H, 2H e jH com quase o dobro do alcance ✅ (é só `reach` e hitbox) | lentos (p07, p09) | mixup (p05), quem passa por baixo (p04) |
| **p09** | Grappler | 11.500 | 240 | 1,25× | 1,3× | **Agarrão de comando** (meia-lua + agarrão, mais alcance e dano) e **armadura** no S3 e S4 🔧 | rushdown (p03, p10, p12) | zoners (p06, p08, p11) |
| **p10** | Rushdown acrobático | 9.300 | 350 | 0,85× | 0,85× | **Mobilidade aérea:** 2 air dashes e voadora que cruza o adversário ✅ air dash já existe, 🔧 o segundo | zoners (p06, p11) | grappler (p09) |
| **p11** | Zoner / trapper | 9.000 | 280 | 0,8× | 1,0× | **Veneno:** S2 e alguns golpes tiram vida ao longo do tempo 🔧 | tanques (p09, p07) | rushdown (p03, p10) |
| **p12** | Berserker | 9.500 | 330 | 1,1× → 1,4× | 1,0× | **Fúria:** dano cresce com a vida perdida, especiais custam vida, S3 com super armor 🔧 | rushdown (troca golpe e ganha) | zoners (não chega perto) |

**Ciclo de forças:** zoner > grappler/pesado > rushdown > zoner. p01, p04, p05 e p12 ficam
no meio e se viram contra todos. Cada dupla da história mistura dois lados do ciclo: p09
(tanque) + p10 (ágil), p05 (perto) + p06 (longe), p07 (pesado) + p08 (longe).

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

## 3. Ficha detalhada: p09 + p10 (modelo para os outros)

Formato: dano · startup/ativo/recuperação em frames (60 por segundo) · alcance.
A vantagem no hit e na defesa fica igual à da base (L +5/+2, M +4/+1, H +5/−2…):
o hitstun sobe junto com o golpe. A última coluna mostra o tempo total no jogo contra o
tempo do vídeo atual.

### p09 — Grappler (vida 11.500 · andar 240 · dash 520 · pulo mais baixo e mais lento)

| Golpe | Dano | s/a/r | Alcance | Nota | Jogo × vídeo |
|---|---|---|---|---|---|
| L | 400 | 5/3/9 | 95 | jab pesado, o mais rápido dele | 0,28s × 3,75s |
| M | 650 | 9/4/14 | 125 | cotovelada | 0,45s × 2,42s |
| H | 1.000 | 13/5/20 | 150 | soco martelo, derruba no counter hit | 0,63s × 3,17s |
| 2L | 350 | 5/3/9 | 90 | baixo | 0,28s × 1,50s |
| 2M | 600 | 9/4/16 | 125 | rasteira, derruba | 0,48s × 2,17s |
| 2H | 950 | 11/6/20 | 140 | uppercut lançador | 0,62s × 2,58s |
| jL / jM / jH | 380 / 600 / 1.000 | 6/4/8 · 8/4/8 · 11/5/10 | — | jH faz slam | — |
| S1 Machado | 900 | 16/1/24 | tela | projétil lento (600) e giratório | 0,68s × 3,29s |
| S2 Cotovelada ↑ | 1.200 | 6/6/24 | 110 | anti-aéreo, derruba | 0,60s × 2,25s |
| S3 Escudo | 1.000 | 20 de armadura + cabeçada | 100 | aguenta 1 golpe 🔧 | 0,70s × 3,79s |
| S4 Ombrada | 1.100 | 12/10/18 | avança | armadura de 1 golpe no startup 🔧, derruba | 0,67s × 2,46s |
| Agarrão de comando | 2.200 | 5/2/30 | 110 | meia-lua + agarrão, não pode ser defendido 🔧 | — |
| Super | 3.400 | — | — | Tempestade de Fogo | — × 3,29s |

### p10 — Rushdown acrobático (vida 9.300 · andar 350 · dash 720 · pulo mais alto · 2 air dashes)

| Golpe | Dano | s/a/r | Alcance | Nota | Jogo × vídeo |
|---|---|---|---|---|---|
| L | 250 | 3/3/6 | 85 | encadeia até 4× | 0,20s × 4,12s |
| M | 400 | 6/4/9 | 120 | chute | 0,32s × 3,58s |
| H | 650 | 9/4/14 | 145 | chute alto | 0,45s × 1,87s |
| 2L | 230 | 3/3/6 | 85 | baixo | 0,20s × 1,00s |
| 2M | 380 | 6/4/10 | 120 | rasteira, derruba | 0,33s × 1,96s |
| 2H | 650 | 8/5/14 | 130 | lançador | 0,45s × 2,25s |
| jL / jM / jH | 250 / 400 / 600 | 4/3/5 · 5/4/6 · 8/4/8 | — | aéreos rápidos, base do jogo dela | — |
| S1 Machado | 650 | 12/1/18 | tela | projétil rápido (900), menos dano que o do p09 | 0,52s × 3,12s |
| S2 Vapor ↑ | 900 | 4/6/20 | 100 | anti-aéreo rápido | 0,50s × 4,12s |
| S3 Esquiva | 700 | 30 no total | 110 | invencível em cima nos frames 2–10 e chuta 🔧 | 0,50s × 4,08s |
| S4 Voadora em parafuso | 850 | 10/12/14 | avança | pode cruzar o adversário (mixup) | 0,60s × 2,63s |
| Agarrão Tesoura | 1.300 | 5/2/24 | 90 | agarrão normal, joga para o outro lado | — |
| Super | 2.800 | — | — | Cortina de Vapor | — × 4,17s |

**O que a coluna "jogo × vídeo" mostra:** o vídeo precisa ser comprimido de 4× a 20×.
Até uns 6× a compressão fica natural. Acima disso (L e M da p10, L do p09, S2 e S3 da p10)
o vídeo tem muito movimento antes ou depois do golpe: o certo é **cortar o trecho** (começar
mais perto do soco e terminar antes de voltar à guarda), não comprimir mais. Se mesmo
cortado não couber, entra na lista de refazer no Seedance.

---

## 4. Perguntas em aberto

1. **Os moldes fazem sentido?** Algum personagem está no molde errado? (ex.: p02 é mais
   rushdown ou mais contra-ataque?)
2. **Vida:** a diferença de 9.000 (p11) a 11.500 (p09) está boa ou é pouca/demais?
3. **Mecânicas únicas:** concorda com uma por personagem? Alguma que você imaginou diferente?
4. **Contra-golpes:** a separação da tabela acima resolve?
5. **p01 carregar a Onda:** foi invenção minha a partir de "poder concentrado (Takuma)".
   Prefere outra coisa?
6. **Ordem de implementar:** sugestão abaixo.

## 5. Ordem sugerida de implementação

1. 🔧 Movimento por personagem (andar, dash, pulo no `character.json`). Pequeno, destrava tudo.
2. ✅ Vida, dano, alcance e frame data dos normais de todos (só dados).
3. ✅ Compressão do vídeo para o novo tempo (o plano anterior), começando por p09 + p10.
4. 🔧 Mecânicas únicas, uma por vez, das mais simples para as mais complexas: chicote (p08,
   já é dado) → armadura (p09, p12) → contra-golpes → esquiva (p04, p10) → rekka (p03) →
   veneno (p11) → teleporte e wall jump (p05) → clones, puxar e capangas (p06, p07).
