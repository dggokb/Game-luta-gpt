# Ficha técnica: sensação de impacto, efeitos, câmera, HUD e fluxo do round

Levantamento só por leitura do código (nada foi editado nem commitado). Caminhos relativos a
`android/app/src/main/java/com/gamelutagpt/`, salvo indicação. Quando não achei algo, está escrito
"não achei".

## 0. Relógio (base de todos os números)

| Item | Valor | Onde |
|---|---|---|
| Simulação | 60 passos/s fixos (`FPS = 60`) | `CombatConfig.java:10` |
| Ritmo base da luta | **0,92** (100% de VELOCIDADE da pausa = 92% do original) | `GameView.java:92` (`RITMO_BASE`), aplicado em `GameView.java:1794` |
| Passos reais por segundo | 60 × 0,92 = **55,2 passos/s** (1 quadro = 18,1 ms reais) | idem |
| Níveis de velocidade na pausa | 50/60/70/80/90/100% (padrão 100%) | `android/story-core/.../historia/MenuPausa.java:51-52` |
| Câmera lenta | escala × 1,15 (`LENTA_ACELERA`), sem o ritmo base | `ImpactFeel.java:243-259` |

Todos os quadros abaixo são passos de 60 Hz da simulação; para tempo real, divida por 55,2.

## 1. Pausa no acerto (hitstop)

### 1.1 Valores base por força (iguais em todos os 20 personagens de `GeneratedCharacters.java`)

`Builder.stun(hitstun, blockstun, hitstop)`; ex.: `player_base` em `GeneratedCharacters.java:1154-1171`,
Ryūga `:1243-1256`, Kōga `:75-87`.

| Força | Golpes | Hitstop base | + "pose do impacto" só no acerto | **Total no acerto** | **Na defesa** |
|---|---|---|---|---|---|
| Fraco (L) | L, 2L, jL | 8 | +4 (`lightHitHoldFrames`, `CombatConfig.java:397`) | **12 q (≈0,22 s)** | **8 q (≈0,14 s)** |
| Médio (M) | M, 2M, jM | 10 (6M: 7) | +5 (`mediumHitHoldFrames`, `:398`) | **15 q (≈0,27 s)** | **10 q** |
| Forte (H) | H, 2H, jH, j2H | 12 | +6 (`heavyHitHoldFrames`, `:399`) | **18 q (≈0,33 s)** | **12 q** |
| Especial | S (projétil) | 6 | 0 (hold só L/M/H: `CombatEngine.java:5144-5149`) | 6 (só quem apanha) | 6 (só quem defende) |
| Especial | S2 / S3 / S4 | 10 / 9 / 8 (um S4 com 9) | 0 | 10 / 9 / 8 | idem |
| Super | SUPER | 12 (todos) | 0 | 12 | 12 |
| Ultra | rush defendido / sequência | 8 (`ultraHitstopFrames`) / 4 por golpe, 12 no último | — | — | 8 | `CombatConfig.java:226,232,269` |
| Raio do Ultra | explosão final | 12 | — | 12 | — | `CombatConfig.java:287` |
| Agarrão | — | 10 | — | 10 | — | `CombatConfig.java:321` |
| Guard break | estalo | 18 (os dois) | — | — | 18 | `CombatConfig.java:462` |
| Combo breaker | estalo | 16 | — | — | — | `CombatConfig.java:501` |

Bônus somados ao acerto (`CombatEngine.java:4914-4920`):

| Situação | Bônus | Exemplo (H) | Onde |
|---|---|---|---|
| Punish counter (H em pé, no chão, contra rival no meio do próprio golpe, H que não é lançador) | **+8** | 26 q | `CombatConfig.java:365`, regra `CombatEngine.java:4783-4785` |
| Golpe que nocauteia (KO) | **+16** | 34 q (e é tocado em câmera lenta) | `CombatConfig.java:400` |
| Contra-golpe comum (counter hit: 1º acerto em quem está apertando botão) | **0 de hitstop**; +2 de hitstun no L, +4 nos outros; dano ×1,2 | — | `CombatConfig.java:371-373`, `CombatEngine.java:4729-4734, 4750` |
| Pancadas intermediárias de golpe de vários acertos | max(3, hitstop × 2/3), ou `strikeStop`/`finalStop` do golpe | — | `CombatEngine.java:5002-5004, 4915` |

Quem congela (`CombatEngine.java:5151-5154`): **corpo a corpo congela os dois** (atacante e defensor, mesmo
número de quadros, `Math.max`); **projétil congela só o defensor**. Defesa: mesma regra, com o hitstop base
(sem a pose do impacto) (`CombatEngine.java:5041`). Defesa perfeita que absorve projétil: 2 q.

### 1.2 Tremida de quem apanha durante a pausa

`ImpactFeel.hitShake` (`ImpactFeel.java:286-293`), aplicada como `translate(x)` no desenho (`GameView.java:5871-5872`):

| Caso | Amplitude (px do mundo) | Ritmo |
|---|---|---|
| Acerto | `min(9, 2,5 + 0,7 × hitstop restante)` → começa em **9 px** em L/M/H (o hitstop total ≥ 12), vai caindo quando restam < 10 q (ex.: 5 q restantes → 6 px; 1 q → 3,2 px) | alterna lado a cada quadro (±) |
| Defesa | **2,5 px** fixo | idem |
| Quem bateu | não treme | — |

Em px de tela: × zoom da câmera (≈1,1–1,36 → ~10–12 px no início do acerto).

Outros sinais durante a pausa:
- Flash branco por cima do sprite (`Filtro.porCima(argb 150, branco)`) enquanto `framesSinceHit < 8` (jogador) ou `< 4` (CPU); como `framesSinceHit` não anda no hitstop, o flash cobre toda a pausa + 8/4 q. Defesa: tinta azul-clara (`rgb 205,240,255`) até 7 q depois (`SpriteFighterRenderer.java:19-20`, `GameView.java:6611, 4862`).
- "Volta seca": depois da janela ativa, golpe que conectou volta à guarda em 3 q (`ImpactFeel.SNAP_BACK_FRAMES`, `ImpactFeel.java:262-280`).

## 2. Hitstun, blockstun, recuo e lançamentos

### 2.1 Por força (normais). Faixa = mínimo–máximo entre os 20 personagens; "típico" = valor mais comum

| Golpe | Hitstun (típ. / faixa) | Blockstun (típ. / faixa) | Recuo no acerto (px) | Recuo na defesa (px) | Efeito |
|---|---|---|---|---|---|
| L | 14 / 12–20 | 9 / 7–15 | 24 | 30 | — |
| M | 18 / 15–25 | 12 / 9–19 | 32 | 38 | — |
| H | 27 / 23–35 | 17 / 13–25 | 44 | 50 | WALL_BOUNCE (só em combo de 2+ acertos ou SHINKAI POWER; sozinho é acerto comum, `CombatEngine.java:4799`) |
| 2L | 14 / 11–20 | 9 / 6–15 | 22 | 28 | baixo |
| 2M | 36 / 20–54 | 6 / 1–24 | 0 | 36 | KNOCKDOWN (rasteira), knockback 40 |
| 2H | 48 / 44–56 | 16 / 7–24 | 0 | 48 | LAUNCH (lançador), knockback 20 |
| jL / jM / jH | 16 / 18 / 21 | 12 / 14 / 17 | 14 / 20 / 28 | 20 / 26 / 34 | jH: WALL_BOUNCE (alguns SLAM/GROUND_BOUNCE), kb 30 |
| j2H | 19 | 15 | 28 | 34 | GROUND_BOUNCE, kb 30 |
| S (projétil) | 20 / 10–31 | 14 / 4–25 | 40 (mín. **120** no acerto, `projectileHitPushback`, `CombatConfig.java:66`) | 50 | — |
| S2 / S3 / S4 | 36–94 / 21–55 / 30–45 | variam | 20 / 40 / 30 | 50 / 50 / 60 | maioria KNOCKDOWN, kb 60–110 |
| SUPER | 10–55 | 4–45 | 60 | 60 | vários KNOCKDOWN, kb 120–160 |

Fonte: todos os `.stun(...)`, `.pushback(...)`, `.knockback(...)`, `.launch(...)` de `GeneratedCharacters.java` (linhas 75–1256).
Golpe no ar defendido por quem está no chão: +6 de blockstun (`jumpInBlockstunBonus`, `CombatConfig.java:382`).

Sistema (comum a todos):

| Item | Valor | Onde |
|---|---|---|
| Recuo distribuído em | 8 quadros | `CombatConfig.java:60`, `CombatEngine.java:5111-5114` |
| No canto, o que sobra do recuo | vai para quem bateu | `docs/combat-engine.md:926` |
| Decaimento de hitstun por acertos | 0,0,0,1,1,2,2,3,3,4,4,5 | `CombatConfig.java:54` |
| Decaimento por segundos de combo | 0,0,1,2,4,6 | `CombatConfig.java:56` |
| Pancadas antes da última (golpe de vários acertos) | recuo × 0,2 | `CombatConfig.java:237` |

### 2.2 Lançamentos e quedas (px do mundo, px/s)

| Tipo | Números | Onde |
|---|---|---|
| Gravidade | 1650 px/s² | `CombatConfig.java:171` |
| LAUNCH (2H) | sobe a 1450 px/s (ápice calculado ≈ 637 px se a gravidade for a cheia; não medido) + empurrão 20; super pulo liberado por 54 q | `CombatConfig.java:68,172`, `CombatEngine.java:4870-4883` |
| WALL_BOUNCE (H em combo) | voa a 1500 px/s até a borda ou 620 px; elevação ≥ 260; volta 300 px/s e sobe 820; hitstun ≥ 48 | `CombatConfig.java:405-415`, `CombatEngine.java:4829-4848` |
| GROUND_BOUNCE (j2H) | cravado a 1500 px/s, quica a 920; hitstun ≥ 48 | `CombatConfig.java:412-415` |
| SLAM (de super pulo) | 1850 px/s para baixo, empurrão × 0,35 | `CombatConfig.java:173`, `CombatEngine.java:4859-4866` |
| Punish counter / giro | sai girando a 360 px/s para trás e 820 para cima (ápice ≈ 204 px calculado), hitstun ≥ 30, cai deitado | `CombatConfig.java:363,401-402`, `CombatEngine.java:4812-4828` |
| KNOCKDOWN | queda 13 q, quique de 30 px em 14 q (desenho), deitado 22 q, levantar 16 q (máx 24) | `CombatConfig.java:74-96` |
| Agarrão | empurrão 170 | `CombatConfig.java:320` |
| Ultra (fim) | voa a 1150 px/s, knockback 420 | `CombatConfig.java:229-230` |

## 3. Câmera

### 3.1 Enquadramento (`CameraRig.java`)

| Item | Valor | Onde |
|---|---|---|
| Tela virtual | 1280 × 720 | `Arena.java:8-9` |
| Mundo | 2600 px de largura, chão em y = 565, paredes em 90 e 2510 | `Arena.java:10-14` |
| Zoom máximo (lutadores perto) | **1,36** | `CameraRig.java:15` |
| Zoom mínimo (longe) | **0,78** | `CameraRig.java:18` |
| Margem horizontal | separação + 520 px do mundo devem caber na tela (zoom máximo vale até ~421 px de separação) | `CameraRig.java:19, 99-104` |
| Zoom do cenário (`camera.zoom` no stage.json) | multiplica tudo: **0,8** em `arena_coberta`, `arena_imperial`, `arena_pintura`, `dojo_yinlu`, `terraco_lua` (→ faixa 0,62–1,09); 1,0 nos outros 7 | `CameraRig.java:45,124`; `assets/stages/*/stage.json` |
| Posição inicial | 1300 ± 280 (separação 560) → zoom 1280/1080 ≈ **1,185** (0,95 nos cenários com 0,8) | `GameView.java:79-80` |
| Seguir zoom / x / topo | suavização exponencial: resta 0,25% / 0,15% / 0,07% do erro por segundo (constantes de tempo ≈ 0,17 / 0,15 / 0,14 s) | `CameraRig.java:126-182` |
| Pulo | abre o zoom pela altura (topo com 64 px de margem), exceto no super pulo e em cenários com `zoomNoPulo: false` (os 5 acima) | `CameraRig.java:47-51, 106-124` |
| Adianta para quem corre | 30 px do mundo por corredor, mola pesada (2 Hz, amort. 1,0) | `CameraRig.java:36, 53-66` |
| Depois do KO | para o x e o zoom; só desce | `CameraRig.java:79-92` |
| Tela que não é 16:9 | escala única (a maior) e corta faixas | `GameView.java:4317-4323` |

**Linha do chão na tela** (calculada das fórmulas em `CameraRig.java:20-33, 147-177`):

| Zoom efetivo | Chão (y na tela) | % da altura a partir do topo |
|---|---|---|
| ≤ 1,12 (inclui todos os cenários com zoom 0,8, e o zoom mínimo) | **552 px** | 76,7% |
| 1,185 (início da luta, cenário 1,0) | ≈ 584 px | 81% |
| ≥ 1,24 (inclui 1,36) | **≈ 611 px** | 84,9% |

**Altura do personagem na tela** (altura visual em pé `visualStandHeight` × zoom; valores de `GeneratedCharacters.java`,
por ex. `player_base` 252, Kōga 191, Ryūga 187, p13 280, Brutamonte 295):

| Zoom | Personagem de 250 px (a maioria: 239–261) | Kōga/Ryūga (~187–191) | p13/p07 (~277–280) |
|---|---|---|---|
| 1,36 (perto, cenário 1,0) | **340 px = 47%** | 255–260 px = 35–36% | 377–380 px = 52–53% |
| 1,185 (início) | 296 px = 41% | 222–226 px = 31% | 328–331 px = 46% |
| 1,088 (perto, cenário 0,8) | 272 px = 38% | 203–208 px = 28–29% | 301–304 px = 42% |
| 0,78 (longe, cenário 1,0) | 195 px = 27% | 146–149 px = 20–21% | 216–218 px = 30% |
| 0,62 (longe, cenário 0,8) | 156 px = 22% | 117–120 px = 16–17% | 173–175 px = 24% |

(`AjusteTamanho` permite 0,70–1,40 por folha, padrão 1: `AjusteTamanho.java:22`.)

### 3.2 Peso do golpe na câmera (`ImpactFeel.java`, só apresentação)

| Efeito | Leve | Médio | Forte | Especial/Super | Punish | KO | Onde |
|---|---|---|---|---|---|---|---|
| Soco de câmera (zoom extra no ponto do impacto) | 0 | +1,2% | +2,5% | +5% | +7% | +9% | `ImpactFeel.java:14-15`; volta por mola 3,2 Hz / amort. 0,75 (`:20`) |
| Tranco (tela vai no sentido do golpe e volta) | 0 | 4 px | 9 px | 14 px | — | sim (tamanho do golpe) | `:25-26` (mola 4,5 Hz / 0,75); 1 tranco a cada 20 q (`:72`); só normais L/M/H e KO (`GameView.java:2997-2999`) |
| Tremor aleatório (`beamShake`, px de tela, ±metade em x e y, cai 1 px por quadro) | 0 | 2 | 4 | 6 | — | 18 | `GameView.java:3000-3002, 2727, 4309-4311` |
| Escurecer o cenário (preto, alfa / quadros, cai linear) | — | — | 70 / 3 q | 100 / 4 q | 140 / 6 q | 140 / 6 q | `ImpactFeel.java:28-30`; desenhado atrás dos lutadores `GameView.java:4373` |

Outros tremores: pancada final de golpe com `finalStop` 14; guard break 14; SHINKAI 16; ULTRA SHINKAI 26; finalização de mangá 26 (`GameView.java:3019, 3049, 3090, 2184`, `FinalizacaoManga.java:28`).
Na defesa: nenhum soco, tranco, tremor de tela nem escurecimento (só `onHit` de acertos, `GameView.java:2999-3003`).

**Impact frame** (`ImpactFeel.java:45-50`, desenho `GameView.java:6248-6270`; pode ser desligado na pausa, IMPACTO, padrão ligado):

| Estilo | Quando | Visual | Duração |
|---|---|---|---|
| RED | punish counter (H); golpe que fecha combo de **5+** acertos jogando longe | fundo `#CD1220`, silhuetas pretas contorno branco, linhas de velocidade, estrela branca | 2 q de silhueta + 1 q negativo (5 + 1 no golpe que fecha "forte") |
| VOID | KO e acerto de Super; último dano do Ultra | fundo `#060608`, silhuetas brancas | idem |
| Intervalo | 30 q sem outro do mesmo peso | | `:50` |

Regra de escolha: `GameView.java:3452-3468` (`IMPACT_COMBO_HITS = 5`).

**Câmera de cinema** (`ImpactFeel.java:52-65`, gatilho `GameView.java:3020-3026, 3442-3449`): no lançador (2H pegando no ar),
no golpe que manda para parede/chão/slam e no KO. Close de **1,45 × 1,25 = ~1,81×** (parede: 1,2 × 1,25 = ~1,5×),
inclinado **8°** a favor do golpe, fundo escurecendo de 60 a 170, luta a **45%** por **12 q** (≈0,39 s reais), corte seco de
volta; 1 a cada 90 q (o KO sempre).

**KO** (`ImpactFeel.java:31-37, 152-172`): clarão branco alfa 210 caindo ×0,88 por quadro (some em ~37 q); câmera lenta
**42 q a 35%** (×1,15 → 40,25% → ≈1,74 s reais); hitstop +16; tremor 18; impact frame VOID; close de cinema.
Golpe final do Ultra: lenta 30 q a 40% (`:36-37`). Finalização de mangá (KO que ganha a luta com Super/Ultra): lenta 54 q a 30%, quadro de 0,08 + 1,6 + 0,9 s (`FinalizacaoManga.java:20-28`).

## 4. Efeitos de acerto

### 4.1 Faíscas (ordem de escolha em `GameView.spawnImpact`, `GameView.java:2965-2981`)

1. Efeito de vídeo de dano no corpo (`dano_cabeca/dano_tronco/dano_baixo`, `GameView.java:5475-5490`): **não existem** em `assets/efeitos` → nunca tocam.
2. Efeito de vídeo de golpe/defesa (`GameView.java:5497-5520`), folhas `assets/efeitos/<nome>/folha.webp` + `efeito.json`, recortadas de vídeo no verde (`EfeitosVideo.java:18-27`), tamanho no mundo = px da folha ÷ `pixelScale` × escala:

| Força | Efeito tocado | Quadros visíveis / duração | Maior quadro (px do mundo) |
|---|---|---|---|
| L | `impacto_fraco` | 4 / 168 ms (folha 5 q, 210 ms) | 120 × 172 |
| M | `impacto_medio` **não existe** → `impacto_fraco` × 1,25 | idem | ~150 × 215 |
| H | alterna `impacto_estouro` × 0,85 ("estouro amarelo com anel", comentário `:5509`) e `impacto_forte` | 10 / 260 ms; 4 / 168 ms | 315 × 250; 376 × 314 |
| Especial/Super/Ultra (e projétil com poder) | `impacto_estouro` | 10 / 260 ms | 370 × 294 |
| Defesa comum | rodízio `defesa` → `defesa_2` → `defesa_3` ("estouro branco com vento") | 6 / 252 ms; 4 / 168 ms; 8 / 260 ms | 228×282; 310×266; 362×281 |
| Defesa de projétil/especial | `defesa_poder` | 4 / 168 ms | 269 × 306 |

   (Todos a ~42 ms por quadro, dados de `assets/efeitos/*/efeito.json`. Cor: a do vídeo; não achei cor no código.)
3. Só se nenhum vídeo existir: faísca procedural `ImpactEffects` (`ImpactEffects.java`): núcleo radial branco→amarelo→laranja (`:61-63`), estrela laranja `#FF9A2A` + branca, anel de choque, estilhaços com gravidade 1500 px/s² (cor branca/amarela/cor do atacante) e "lanças de luz" (FighterZ):

| Força | Vida | Tamanho | Estilhaços | Lanças (largura) |
|---|---|---|---|---|
| L | 0,20 s | 46 | 7 | 1 (16) |
| M | 0,24 s | 62 | 10 | 2 (24) |
| H | 0,30 s | 84 | 15 | 3 (32) |
| Especial | 0,36 s | 104 | 20 | 4 (40) |

   `ImpactEffects.java:23-29`; lanças 1100 px de comprimento, 0,16 s. Defesa procedural: 90% da vida, 85% do tamanho, azul `#6FD8FF`, arco de barreira + hexágono, estilhaços voltam para o atacante (`:101-123, 385-402`).

Extras no acerto:
- **Riscos de mangá** (H, especiais, Super, Ultra): traços pretos atrás de quem apanhou, 16 q, tremem a cada 2, somem nos 5 finais; ×1 no H, ×1,25 no especial (`RiscosDeImpacto.java:16-17`, `GameView.java:2990-2994`).
- Plateia do cenário vibra: 0,15/0,35/0,65/1,0 (L/M/H+/KO) (`GameView.java:3005-3007`).
- Brilho de "golpe alto saindo" (0,2 s, tamanho 38) (`ImpactEffects.java:265-275`).

### 4.2 Contra-ataque / punish

| Caso | Texto | Efeitos | Som |
|---|---|---|---|
| Counter hit (contra-golpe comum) | **nenhum** (o cue `COUNTER_HIT` não é lido fora do motor) | nenhum além de dano ×1,2 e +2/+4 de hitstun | o normal |
| Punish counter (H) | "PUNISH COUNTER!" (`GameView.java:3539`) | +8 hitstop, giro, zoom 7%, escuro 140/6 q, impact frame RED | `punish.wav` |
| Contra-golpe de postura (mecânica) | "CONTRA-GOLPE!" (`GameView.java:3537`) | — | — |

### 4.3 Poeira, rastros, sombra, reflexo

| Item | Como é | Onde |
|---|---|---|
| Pouso comum | 2 sopros pequenos (`spawnSkid`, nuvem 16–24 px, 0,45–0,6 s) dos dois lados dos pés; nuvem grande se o pouso for pesado (força ≥ 0,5) | `GameView.java:5435-5461`, `ImpactEffects.java:183-202` |
| Vídeos `fumaca_pouso`, `fumaca_pulo`, `poeira_corrida`, `poeira_freada`, `super_pulo`, `arco_golpe` | pedidos pelo código, **não existem** nos assets (não tocam) | `GameView.java:5439-5447, 5529, 5536, 5544, 3505` |
| Dash (largada e freada) | 2 sopros `spawnSkid` | `GameView.java:5456-5460` |
| Pulo (decolagem) | nada (só o vídeo inexistente) | `GameView.java:5528-5530` |
| Queda (knockdown) | vídeo `impacto_chao` (8 q, 336 ms, até 342 × 70 px) × 0,7–1,0 (mais forte vindo do ar); sem ele, `spawnDust` (70–140 px, 10–24 pedrinhas) | `GameView.java:3490-3498` |
| Super pulo | poeira + anel de choque achatado 120 px + riscos para cima, na cor do lutador | `ImpactEffects.java:209-229` |
| Rastro (motion trail) | 3 cópias azuladas, 1 a cada 2 q, vida 8 q, alfa 0,35; só dash/backdash/pulo com embalo (nos golpes: desligado) | `android/motion-core/.../motion/Ajustes.java:141-149`, `SpriteFighterRenderer.java:140-155` |
| Smear | golpes fortes (força ≥ 2): 2 cópias esticadas na horizontal (até +18%), 2 q, alfa 0,25/0,45 | `Ajustes.java:113-116`, `SpriteFighterRenderer.java:177-185` |
| Sombra de contato | elipse preta alfa 70, meia-largura 43 px, encolhe/clareia até 420 px de altura | `GameView.java:6615-6629` |
| Sombra projetada (silhueta deitada no chão) | só nos cenários com `sombraLutadores`: `arena_central`, `arena_coberta`, `arena_imperial`, `arena_pintura`, `dojo_yinlu`, `terraco_lua` | `GameView.java:4598-4620`, stage.json |
| Reflexo no chão | lutador espelhado com alfa = `reflexoLutadores`: 0,08 (penhascos) a 0,35 (beco); 9 cenários | `GameView.java:4579-4590`, stage.json |

## 5. Super e Ultra

### 5.1 Super (`SuperFlash.java`, `CombatEngine.java:1790-1812`)

| Item | Valor | Onde |
|---|---|---|
| Custo | 1 barra (de 5) | `CombatConfig.java:211` |
| Congelamento do rival | quadros de startup do Super (Super de golpe: startup − 6, para dar 6 q de reação); mais a entrada (retrato), se houver | `CombatEngine.java:1801-1811`, `CombatConfig.java:234` |
| Startup dos Supers | 6 a 70 q (ex.: player_base 35, p14 6, p07/p11 70) | `GeneratedCharacters.java` |
| Aviso de ativação | corpo brilha na cor do lutador por 8 q; brilho de estrela no peito 0,42 s, tamanho 110, 14 faíscas; som `super.wav` | `SuperFlash.java:26`, `ImpactEffects.java:232-259`, `GameView.java:2952-2959` |
| Escurecer | até alfa 190 em 0,16 s | `SuperFlash.java:18,24` |
| Linha do tempo (tempo do Super) | escurece 0,16 s → pose 0,30 → clarão branco 0,12 (pico 235) → liberação 0,20 (branco 255 caindo) → volta 0,22 = **1,0 s**; Super corpo a corpo corre **2,4×** mais rápido (~0,42 s) | `SuperFlash.java:18-20, 133-169` |
| Zoom | 1,0 → 1,18 → 1,36 no clarão → volta a 1,0; o foco vai para o peito de quem ativou (meio da tela, 52% da altura) | `SuperFlash.java:144-168`, `GameView.java:4251-4266, 3756-3758` |
| Quem ficou congelado | escurece (metade da luz, tom frio), entra 0,2/q e sai 0,12/q | `SuperFlash.java:28`, `GameView.java:2925-2939` |
| Retrato / corte | `EntradaSuper`: quadro de mangá que bate da borda com o retrato em vídeo; **só p15 tem** (`assets/supers/p15`): 21 quadros × 55,6 ms + 150 ms de saída = 1318 ms = **80 q** de luta parada | `EntradaSuper.java:23-33, 94-101`, `assets/supers/p15/super.json` |
| Impact frame no acerto | VOID | `GameView.java:3460` |

### 5.2 Ultra (3 barras; Ultra SHINKAI: 5)

| Fase | Valor | Onde |
|---|---|---|
| Ativação | 25 q, rival congelado, escuro até 205, zoom +30% (exceto Ultra que usa a câmera da luta) | `CombatConfig.java:219`, `GameView.java:208, 2508-2516`, `CombatEngine.java:1861-1864` |
| Investida | 18 q a 2000 px/s, alcance 96; defendida: blockstun 24, hitstop 8, empurrão 120 | `CombatConfig.java:220-227` |
| Acertou | página de mangá em tela cheia (`PaginaFinal`): painéis a 0,10/0,66/1,22 s, anel até 2,00–2,68 s, toque no tempo (PERFEITO ±0,075 s, BOM ±0,17 s), quebra em 48 cacos | `docs/ultra-pagina-final.md:42-56` |
| Raio final | carga 36 q, extensão 8, 1 acerto a cada 4 q (20 acertos), explosão +6 q com hitstop 12, some em 18 q | `CombatConfig.java:280-298`, `docs/ultra-pagina-final.md:63-72` |
| Golpe final | lenta 30 q a 40%, impact frame VOID | `ImpactFeel.java:36-37` |

## 6. Sons

| Arquivo (`assets/sons/luta/`, 22 050 Hz mono, gerados por síntese em `tools/sons/gerar_sons.py`) | Duração | Quando toca | Onde |
|---|---|---|---|
| `ar_l` / `ar_m` / `ar_h` | 0,11 / 0,15 / 0,22 s | "vush" 2 q antes do golpe normal ativar (vol 0,5, tom 0,92–1,08) | `GameView.java:3474-3486` |
| `acerto_l` / `acerto_m` / `acerto_h` / `acerto_s` | 0,15 / 0,275 / 0,40 / 0,437 s | acerto por força (vol 0,85, tom 0,94–1,06); `acerto_s` também em SHINKAI, clash e afins | `GameView.java:3027-3031, 3470` |
| `defesa` | 0,12 s | toda defesa; mais agudo (×1,45–1,7) em parry/defesa perfeita | `GameView.java:3027, 3286-3316` |
| `punish` | 0,45 s | punish counter H; também dragão do Ultra | `GameView.java:3029, 5575` |
| `ko` | 1,3 s | golpe do KO (vol 1,0) | `GameView.java:3028` |
| `queda` | 0,32 s | corpo deitado no chão | `GameView.java:3512` |
| `guarda_quebrada` | 0,5 s | guard break | `GameView.java:3050` |
| `super` | 0,7 s | ativação do Super | `GameView.java:2958` |
| `trovao` | 2,6 s | relâmpago do cenário | `GameView.java:2679` |

- Total de áudio no APK: **14 arquivos**, todos acima. **Música: não achei** (nenhum arquivo de música, nenhum `MediaPlayer`).
- **Vozes: não achei.** O narrador (`NarradorLuta`/`Narracao`) é só texto numa faixa no rodapé com retrato (`android/story-core/.../historia/Narracao.java:4-29`).
- **Sons por personagem: não achei.** `UltraSounds` procura `ultras/<id>/sons/` e `ultras/sons/` (`UltraSounds.java:14-21, 53-63`), mas essas pastas não existem; os sons do Ultra ("investida", "carga") ficam mudos.
- Mixer: `SoundPool` com 10 vozes (`UltraSounds.java:33-39`).

## 7. HUD (padrão: arte `assets/ui/hud/energy-frame.png` carregada, `HudRenderer.java:109-115`; GameView chama em `GameView.java:895`)

Coordenadas da tela 1280 × 720; o lado da CPU é o espelho (x → 1280 − x).

| Elemento | Posição / tamanho (px) | Detalhes | Onde |
|---|---|---|---|
| Moldura ornamental do topo | 0,0 → 1280 × 170 | imagem | `HudRenderer.java:179` |
| Retrato do titular | x 43, y 24, 119 × 86 | treme ±5 px e fica vermelho 0,32 s ao apanhar | `:315-325` |
| Barra de vida do titular | x 165, y 74, **389 × 18** (30% da largura da tela) | verde em degradê; ≤25%: pulsa vermelho-laranja; borda pisca | `:328, 504-543` |
| "Vida vermelha" | — | **sim, duas coisas**: (a) vida recuperável (vermelho `#E63D54`, volta fora de campo, estilo Marvel); (b) cauda do dano: pedaço perdido branco→âmbar na faixa de baixo (35% da altura), **parado 0,18 s e descendo em 0,55 s** | `:510-513, 767-781`; `HudEffects.java:5, 21-33` |
| Retrato do parceiro (TAG) | x 120, y 110, 65 × 25 | | `:327` |
| Vida do parceiro | x 259, y 119, 182 × 10 | | `:329` |
| Indicador de assist | círculo em (554, 115), raio 8–13 | anel de recarga, flash 0,9 s quando pronto | `:331-335`, `HudEffects` |
| Barra do SHINKAI | x 174, y 96, 330 × 11 | | `:182-183` |
| Super | moldura x 0, y 625, 400 × 95; **5 células** em x 94…319, y 679, ~40 × 15 cada | azul P1 / vermelho CPU, energia correndo; ganho de nível pisca 0,42 s, gasto 0,30 s (Super inteiro 0,75 s) | `:339-401`; `HudEffects.java:6-8` |
| Nível do Super | dígito em (51, 684), 38 px (cresce 35% ao subir) + "SUPER" em (348, 702), 17 px | | `:388-400` |
| Textos fixos | "PLAYER 1/2" em (220/1060, 65) 15 px; "TAG" / "TAG OFF" em (222/1058, 132) 13 px | | `:188-191` |
| Relógio | centro (640, 70), 48 px (3 dígitos) ou 58 px; **∞ no treino**; batalha **180 s** (o comentário do HUD diz 120, o código diz 180); últimos 10 s vermelho pulsando 18% | `HudRenderer.java:196-230`; `BattleFlow.java:30, 35` |
| Modo | "TREINO"/"BATALHA" em (640, 87), 12 px | | `:199` |
| Contador de combo | jogador: x 40, y 300 (CPU: alinhado à direita em x 1060); número 62 px (+30% e tremida de 3 px em cada acerto, 120 ms); "HITS" 24 px; faixa "DANO n • ESCALA n%" 17 px em y 309–333; "JUGGLE n" no ar; fica 60 q depois do combo, a partir de 2 acertos | `HudRenderer.java:1097-1188` |
| Faixas centrais (READY, GO!, K.O., TECH!, PUNISH COUNTER!, GUARD BREAK!...) | centro (640, 210), texto 58 px itálico com degradê dourado, faixa diagonal ±330 px; entra 1,4× e assenta | `HudRenderer.java:1048-1094`, textos `GameView.java:3523-3559`; avisos duram 42 q (`GameView.java:461`) |
| Medidor de guarda | 96 × 7 px, embaixo dos pés, só quando a guarda não está cheia; amarelo, vermelho piscando ≤30% | `GameView.java:3722-3753` |
| Fonte | fonte padrão do sistema (sem arquivo de fonte no APK), negrito falso, inclinada −0,16/−0,22; telas de vitória/seleção usam `sans-serif-condensed` | `HudRenderer.java:422-438`, `TelaVitoria.java:131-132` |

## 8. Fluxo do round (`BattleFlow.java`)

| Etapa | Quadros (60 Hz) | Tempo real (÷ 55,2) | Onde |
|---|---|---|---|
| Intro | a animação INTRO mais longa dos dois: 4,16 s (p02, p04–p12), 3,38 s (p03), 2,4 s (player_base) → ~250 q | ~4,5 s | `GameView.java:2292-2318`; durações em `GeneratedCharacters.java` |
| READY (ninguém se mexe) | 60 | 1,09 s | `BattleFlow.java:23` |
| GO! (controle volta, relógio começa) | 40 | 0,72 s | `:24` |
| Luta (batalha) | 180 s de relógio = 10 800 q | ~195,7 s | `:30-33` |
| Hitstop do KO | +16 sobre o do golpe (H: 34 q) | (em câmera lenta) | `CombatConfig.java:400` |
| Câmera lenta do KO | 42 q a 35% (×1,15) | ≈1,74 s | `ImpactFeel.java:33-34` |
| Parceiro entra (2×2) | 75 q depois de o titular cair | 1,36 s | `CombatConfig.java:839` |
| Faixa "K.O." / "TIME OVER" | 90 | 1,63 s | `BattleFlow.java:26` |
| Pose de vitória | VICTORY uma vez (33 q de arte, 4,125 s) e segura o último quadro | 4,125 s | `GameView.java:5268-5277` |
| Faixa "VITÓRIA"/"DERROTA"/"EMPATE" | 150 | 2,72 s | `BattleFlow.java:28` |
| Tela de vitória (se o jogador venceu e o lutador tem `assets/vitorias/<id>`: **só player_base**) | fundo 200 ms, entra 80 + desliza 260 ms (180 px), "VITÓRIA" 340 ms (batida 130, clarão 120, tremida 220), frase em 800 ms (pop 160), saída 280 ms; vídeo de 124 q × 20,8 ms = 2,58 s + 2 s parado | ~5 s | `TelaVitoria.java:50-66`, `assets/vitorias/player_base/vitoria.json` |
| Recomeço (fora da história) | depois de KO 90 + resultado 150 = 240 q | ~4,3 s | `BattleFlow.java:137` |
| Modo história | espera 150 q depois do fim | 2,7 s | `GameView.java:375, 7437` |

Fora do modo batalha (IA OFF, o padrão do Modo Teste) só vale READY → GO; KO não encerra e o tempo é ∞ (`BattleFlow.java:19-20`).

## 9. Controles de toque e camada de teste

### 9.1 Botões (tamanho fixo; não achei ajuste de tamanho) — `ControlsLayout.java:12-61`

| Controle | Centro (x, y) | Raio | Diâmetro (% da altura 720) |
|---|---|---|---|
| Direcional | (185, 545) | 142 (zona morta 30) | 284 px (39%) |
| L / M / H | (990, 602) / (1100, 508) / (1210, 602) | 64 | 128 px (18%) |
| L+M (agarrão) / M+H (pushblock) | entre os botões | 32 | 64 px |
| COMBO | (1100, 650) | 46 | 92 px |
| TAG | (915, 500) | 50 | 100 px |
| SUPER (vira ULTRA / U.SHINKAI) | (870, 628) | 54 | 108 px |
| SHINKAI | (790, 555) | 40 | 80 px |
| "◀ MENU" | retângulo 565–715 × 112–154 | — | — |

Visual: corpo de vidro, anéis L azul `#45C6FF`, M amarelo `#FFC53A`, H vermelho `#FF4D5E`, COMBO roxo (`HudRenderer.java:1038, 1518-1583`).

### 9.2 O que aparece por cima da luta por padrão no Modo Teste (fora do modo história/tutorial)

| Item | Padrão | Onde |
|---|---|---|
| Contador de FPS (pílula em baixo no centro, y 686–712) + até 5 linhas de diagnóstico acima (tempo por parte, MB das folhas, RAM/aparelho, resumo do OpenGL, "entrada: ...") | **ligado** (FPS na pausa, padrão true) | `MenuPausa.java:80`, `GameView.java:3906-3960, 4100-4129` |
| Coluna de botões de teste (x 1082–1248, y 176–456): IA ON/OFF, DEBUG ON/OFF, VIDA P1, VIDA CPU, DUPLA, HISTÓRIA, TUTORIAL, ANIM 100% | **visível** (BOTÕES DE TESTE, padrão true; somem no modo história, tutorial, conversa e cinemática) | `HudRenderer.java:265-289`, `ControlsLayout.java:63-103`, `GameView.java:8076-8078` |
| Coluna de DESEMPENHO (x 910–1074): GRÁF, FOLHAS GPU/RAM, TESTE, EFEITOS, CENÁRIO, HUD, MSGS, OPENGL | **visível** | `ControlsLayout.java:110-118`, `GameView.java:2387-2402` |
| IA | **desligada no Modo Teste** (treino: relógio ∞, "TREINO"); ligada nas lutas da história (exceto tutorial) | `GameView.java:7154-7159, 7567`, `AiController.java:126` |
| DEBUG (caixas de acerto/corpo, estado, buffer, tabela de frame data, painel de vida/dano sobre a CPU) | **desligado** | `GameView.java:121`, `DebugOverlay.java:2-7`, `GameView.java:4868-4877` |
| Faixa "DANO • ESCALA %" do combo | sempre que há combo | `HudRenderer.java:1166-1179` |
| Motor de animação (achatar/esticar, smear, rastro) | **ligado, 100%** | `Ajustes.java:11-13`, `AnimacaoLutador.java:18-31` |
