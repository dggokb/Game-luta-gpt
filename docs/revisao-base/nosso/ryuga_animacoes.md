# Ryūga adulto: ficha técnica de animação e movimento (branch `game-luta-sprite-review`, HEAD a94d2c54)

Todos os números vêm do código e dos dados do repositório. Onde não encontrei nada, está escrito "não achei".

## 0. Qual id é o Ryūga adulto (correção importante)

**O Ryūga adulto é o `player_base`, que usa o concept p01. Não é o `p03`.**

- `android/app/src/main/assets/historia/temporada.json:6`: `"ryuga": { "nome": "Ryūga", ..., "lutador": "player_base" }`
- `android/app/src/main/assets/historia/temporada.json:7`: `"koga": { "nome": "Kōga", ..., "lutador": "p03" }`
- `tools/sprites/montagens/player_base.json:2`: "Ryūga adulto (p01) refeito do zero com os vídeos novos…"
- Os seis commits citados (11c83fd3, 196e803a, 65a5055d, 970f45d9, 699c4e84, 077c0e4f) mexem só em `player_base_*.webp`, `characters/player_base/character.json`, `tools/sprites/montagens/player_base.json` e `docs/personagens/p01.md`.
- O `p03` (Kōga) tem folhas próprias `p03_*`. Ele reaproveita só o `player_base_jump_medium` e o `player_base_throw`, e usa o perfil visual de id "player_base" com a mesma escala 1,075 (`GeneratedCharacters.java:253`).

Por isso, esta ficha é inteira do **`player_base`**.

### Abreviações de arquivo usadas abaixo

| Sigla | Arquivo |
|---|---|
| GC | `android/app/src/main/java/com/gamelutagpt/GeneratedCharacters.java` (gerado de `characters/player_base/character.json`) |
| CC | `android/app/src/main/java/com/gamelutagpt/CombatConfig.java` |
| CE | `android/app/src/main/java/com/gamelutagpt/CombatEngine.java` |
| CD | `android/app/src/main/java/com/gamelutagpt/CharacterDefinition.java` |
| GV | `android/app/src/main/java/com/gamelutagpt/GameView.java` |
| SM | `android/app/src/main/java/com/gamelutagpt/SpriteMotion.java` |
| SFR | `android/app/src/main/java/com/gamelutagpt/SpriteFighterRenderer.java` |
| MON | `tools/sprites/montagens/player_base.json` (receita: quais quadros de qual vídeo) |
| CLIP | `tools/sprites/clips/player_base_<nome>.json` (o campo `video.quadros` fica na linha 19–30 de cada um) |

## 1. Convenções do motor (leia antes das tabelas)

- **Relógio:** a lógica roda a 60 Hz (`CC:10` `FPS = 60`, `CC:11` `DT = 1/60`). Neste documento, "tique" é um passo de 60 Hz.
- **Duração de cada imagem:** no código vem em segundos (`new float[]{…}` em GC). Aqui está convertida para tiques (s × 60). Por exemplo, 0,083 s = 4,98 tiques.
- **A animação dos golpes não decide o tempo do golpe.** O tempo vem do frame data (startup+ativo+recuperação), e o clipe é **esticado ou encolhido** para caber, mantendo a proporção entre as imagens:
  - `CD:194` `totalTime = attack.totalFrames/FPS`
  - `CD:208` `animationTime → animation.timeFor(elapsed,totalTime)`
  - `CD:158-161` `timeFor = elapsed*duration/gameplayDuration`
  - `docs/sprite-standard.md:198-201`
  
  O jogo mostra a imagem do instante `(quadro+0,5)/60` (`GV:5202`, `ImpactFeel.java:271-279`). Uma imagem que dura menos de 1 tique pode **nunca aparecer**.
- **Startup:** `startupFrames` conta os quadros **antes** da hitbox, e o golpe acerta no quadro `startup + 1` (`docs/combat-engine.md:46-47`; `AttackDefinition.java:303-304` `isActive = frame >= startup`). No padrão do Street Fighter, que conta o 1º quadro ativo, o startup é **startup+1**. As tabelas mostram as duas formas.
- **Vantagem no acerto:** `hitstun − (ativo − 1 + recuperação)` (`docs/combat-engine.md:48-49`). No bloqueio, a mesma conta usa o blockstun.
- **Pose segurada no impacto:**
  - No **acerto**, os dois congelam (hitstop) pelo `hitstopFrames` do golpe mais um extra por força: fraco +4, médio +5, forte +6 (`CC:397-399`, `CE:4917`, `CE:5144-5149`). Especiais (S2–S4) e Super não ganham o extra.
  - No **bloqueio**, o congelamento é só o hitstop, sem extra (`CE:5041`).
  - A imagem mostrada no congelamento é a do quadro em que o golpe encostou, porque o `attackFrame` não anda durante o hitstop.
- **Volta seca no acerto ou bloqueio:**
  - Depois do fim da janela ativa, o desenho volta à última pose em 3 quadros (`ImpactFeel.java:262` `SNAP_BACK_FRAMES = 3`, `:271-279`).
  - Golpe normal no chão que acerta um rival em pé tem a recuperação cortada em `max(startup+ativo−1+3, fim da janela de cancelamento)`. Isso vem de `CE:5136-5140` (`hitCutGuardFrames = 3`, `CC:396`) e é aplicado em `CE:4927-4931`. Rasteira (knockdown) defendida também corta (`CE:4680-4682`).
  - O rival perde o mesmo tanto de atordoamento, então a vantagem não muda (`CE:1626-1632`).
- **Clipe por distância:** andar e corrida trocam de imagem pela distância andada, não pelo tempo (`CD:130-131`: `distancePerFrame > 0 → frames[(distance/dpf) % n]`).

## 2. Tamanho e escala

| Item | Valor | Fonte |
|---|---|---|
| Perfil visual | `CharacterVisualProfile("player_base", 256, 256, rootX 128, rootY 238, worldScale 1,075)` | GC:1171 |
| `pixelScale` de **todas** as folhas do Ryūga | 2,0 (folhas em "densidade 2", o dobro da célula canônica) | GC:1103-1153 (9º argumento de cada `Atlas`); MON:5 `"densidade": 2` |
| Unidades do mundo por pixel da folha | `worldScale/pixelScale` = 1,075/2 = **0,5375** | SFR:267 |
| Escala do vídeo para a folha | **0,4139**: "guarda de 541 px nos vídeos novos do p01 (H3) = 224 px do p01" (224 px canônicos = 448 px da folha) | MON:3-6; CLIP jab `video.escala` |
| Vídeos de origem | 124 quadros a 24 qps, 1344×768 (MiniMax H3) | MON:2; `docs/personagens/p01.md` (seção "Refeito do zero…") |
| Guarda canônica no mundo | 224 × 1,075 = **240,8 u** | `docs/personagens/p01.md` ("Tamanho": "guarda em 224 px na célula e o jogo aumenta 7,5%") |
| Altura medida do IDLE (pixels opacos acima da raiz) | quadro 0: **450 px da folha = 241,9 u**; varia de 449 a 469 px (241,3 a 252,1 u) | `tools/sprites/reports/player_base_idle.report.json` (`outputBbox`, `layout.rootY 540`) |
| Agachado parado (CROUCH_IDLE) | 331–341 px = 177,9–183,3 u | `tools/sprites/reports/player_base_crouch_idle.report.json` |
| Encolhido no ar (FALL) | 361–380 px = 194,0–204,2 u | `tools/sprites/reports/player_base_fall_air.report.json` |
| `visualStandHeight` / `visualCrouchHeight` do pack | 252,0875 / 241,3375 u (o valor de agachado parece desatualizado em relação à arte nova; está assim no código) | GC:1171 |
| Hurtbox (corpo) | meia-largura 35; altura em pé 245; agachado 157 | GC:1171 `Body(35,245,157,46,217)`; `characters/player_base/character.json:16-18` |
| Pushbox | meia-largura 46; altura 217 | GC:1171 |
| Vida | 10000 no pack × 1,4 (`LIFE_SCALE_PERMILLE = 1400`) = 14000 | GC:1171; CC:446; CD:556-558 |
| Tela virtual / câmera | 1280×720; chão em y=565; zoom 1,24 em repouso, 1,36 perto, 0,78 longe | `Arena.java:8-10`; `CameraRig.java:15-18` |
| Altura em tela | 241,9 u × 1,24 = ~300 px de 720 (41,7%); com zoom 1,36, ~329 px (45,7%) | conta a partir das linhas acima |

Escalas extras aplicadas no desenho:

- **Folhas de vídeo** (todas as refeitas): escala uniforme = `ajusteTamanho` do clipe. Nenhum clipe de vídeo tem esse campo, então a escala é 1 (`tools/sprites/generate_p01_calibration.py:62-66, 98-102`; SFR:193-197).
- **Folhas antigas** (intro, vitória, derrota, provocação, danos, queda/levantar, defesa agachada, defesa no ar, agarrão, S4, ativação do SHINKAI): recebem a calibração por quadro do `P01SpriteCalibration`, gerada no build (`tools/sprites/generate_p01_calibration.py:69-189`). Algumas têm `ajusteTamanho`:

  | Clipe | ajusteTamanho |
  |---|---|
  | defense_air | 0,93 |
  | defense_crouch | 0,96 |
  | fall | 0,87 |
  | hit_air | 0,98 |
  | hit_crouch | 0,96 |
  | intro | 0,99 |

  Fonte: CLIP de cada um.
- `AjusteTamanho` (menu DEBUG) vale 1 por padrão (`AjusteTamanho.java:63-71`).
- O motor de animação ainda achata e estica em volta dos pés (seção 10).

## 3. Movimento

| Movimento | Números | Fonte |
|---|---|---|
| **Andar para frente** | **170 u/s = 2,833 u/quadro**. O motor tem 300 como padrão (`CC:108`), mas o Ryūga sobrescreve. | GC:1171 `withWalk(170,140)`; `character.json:99`; CE:1974-1978 |
| Animação do andar para frente | 46 imagens, 1 imagem a cada **3,81 u** = 1,345 quadros por imagem. O ciclo completo cobre 175,3 u = **61,9 quadros (1,03 s)**. Ciclo 1,86× mais rápido que o vídeo (pé plantado). | GC:1105; MON:47-68; `tools/sprites/montagem.py:217-219` (3,81 = 170/24/1,86) |
| **Andar para trás** | **140 u/s = 2,333 u/quadro** (o padrão do motor é 220, `CC:110`) | GC:1171 |
| Animação do andar para trás | 37 imagens, 1 imagem a cada **2,33 u** (~1 imagem por quadro). Ciclo = 86,2 u = **37 quadros (0,62 s)**, 2,5× mais rápido que o vídeo. | GC:1106; MON:69-90 |
| **Corrida (dash →→)** | Velocidade **620 u/s = 10,33 u/quadro**, sem multiplicador próprio (`dashPermille` 1000, o Ryūga não tem `withRun`). Corre enquanto segura →. Toque duplo em até 18 quadros. Sem quadros de largada ou freada no motor: **não achei**. | CC:111; CC:34; CD:612; CE:1981-1988; CE:2466-2471 |
| Animação da corrida | 12 imagens em loop, 1 imagem a cada **26 u** (~2,5 quadros). Ciclo = 312 u = 30,2 quadros. | GC:1111; MON:91-112 |
| **Backdash** | **15 quadros a 640 u/s = 160 u**. Mais 12 quadros de recarga antes de outro backdash. Pulinho só no desenho: 40 u em seno. Invencibilidade no backdash comum: **não achei**. A única é a da esquiva automática do SHINKAI (CE:1598-1600). | CC:135-147; CE:2460-2462; GV:5643-5646 |
| Animação do backdash | 28 imagens × 0,96 tiques = 26,88 tiques. É o "tempo do backdash do motor" (15+12) segundo MON:113-130. Mas o `SpriteMotion` só mostra o BACKDASH enquanto `backdashFrames > 0` (GV:5419, SM:35), ou seja, nos 15 quadros: **só as imagens ~1–15 aparecem**. As imagens 16–27 (pouso e volta à guarda) dão lugar ao IDLE. | GC:1112; SM:35 |
| **Pré-pulo (jump squat)** | **3 quadros** sem golpe, dash ou andar. O desenho usa as imagens 0–2 do CROUCH. O pulo que cancela um golpe sai na hora. | CC:156-160; CE:2024-2034; GV:5414; `docs/motor-animacao.md` ("Mudanças de jogabilidade") |
| **Pulo: impulso e gravidade** | `jumpSpeed` 1492 u/s; gravidade 1650 u/s². Na subida a gravidade é multiplicada por 2,463 (= 4064 u/s²) e na descida por 2,897 (= 4780 u/s²). Integração de Euler a 60 Hz, saindo 2 u acima do chão. | CC:154, 166-167, 171; CE:2068-2069, 2564-2601 |
| **Pulo: tempo no ar e altura** (simulado com a mesma integração) | **42 quadros no ar**; **ápice 263,5 u** no quadro 22 (~1,09× a altura do idle). O comentário do código diz "~42 airborne frames". | CC:149 |
| Pulo: deslocamento horizontal | Frente 400 u/s → **280 u**. Trás 290 u/s → **203 u**. Neutro 0. A direção ainda pode mudar nos 3 primeiros quadros no ar. Pulo saído da corrida: 540 u/s → **378 u**. | CC:118-129; CE:2521-2530 |
| **Pouso** | Pulo vazio: **3 quadros** sem golpe, pulo, dash ou andar (defender pode). Golpe aéreo que pousa termina na hora, sem lag. | CC:162-165; CE:2605-2655 |
| Ciclo completo do pulo vazio | 3 (pré) + 42 (ar) + 3 (pouso) = **48 quadros** | conta a partir das linhas acima |
| **Super pulo** (↓↑ em até 22 quadros, ou depois de um lançador) | 1450 u/s, sem o multiplicador de gravidade → **105 quadros, ápice ~627 u** (simulado). Drift de 900 u/s². | CC:36, 68, 134, 168; CE:2564-2568 |
| Pulo extra no ar (cancel do jM) | 900 u/s para cima | CC:170 |
| **Air dash** | Frente: 14 quadros × 900 u/s = 210 u. Trás: 12 × 640 = 128 u. Um por pulo. Altura mínima 36. Desenho: o clipe DASH (por distância) ou BACKDASH. | CC:417-423; GV:5240-5250 |

## 4. Todas as animações: imagens, folha, tempo por imagem e origem

- "Imagens na folha" é o `count` do `Atlas`.
- "Sequência" é a lista de imagens que o clipe toca, com o tamanho dela entre parênteses.
- "Tiques" é a duração de cada posição da sequência (s×60), e "×n" indica repetição.
- A origem vem do CLIP (`video.arquivo` e `video.quadros`, ou `source` nas artes antigas). O vídeo é a 24 qps.

| Estado | Definição | Folha (`res/drawable-nodpi/…webp`) | Imagens na folha | Sequência | Tiques por imagem | Total (tiques) | Loop | Origem |
|---|---|---|---|---|---|---|---|---|
| IDLE | GC:1103 | `player_base_idle` | 30 | 0–29 (30) | 4,98×30 | 149,40 (2,49 s) | sim | `idle.mp4` q0–58 de 2 em 2 |
| COMBAT (pose de combate parada) | GC:1104 | `player_base_idle` | 30 | 0 (1) | 6,00 | 6,00 | não | imagem 0 do idle |
| IDLE_2 (cruza os braços, a cada 3 voltas do IDLE) | GC:1148; SM:14, 72-78 | `player_base_idle_2` | 51 | 0–50 (51) | 4,98×51 | 253,98 (4,23 s) | não | `idle_complemento.mp4` q8–108 de 2 em 2 |
| WALK_FORWARD | GC:1105 | `player_base_walk_forward` | 46 | 0–45 (46) | por distância: 3,81 u/imagem | — | sim | `frente.mp4` q38–83 |
| WALK_BACK | GC:1106 | `player_base_walk_back` | 37 | 0–36 (37) | por distância: 2,33 u/imagem | — | sim | `tras.mp4` q23–59 |
| DASH | GC:1111 | `player_base_dash` | 12 | 0–11 (12) | por distância: 26 u/imagem | — | sim | `player_base_dash.mp4` q40–51 |
| BACKDASH | GC:1112 | `player_base_backdash` | 28 | 0–27 (28) | 0,96×28 | 26,88 | não | `backDash.mp4` q6–87 de 3 em 3 |
| CROUCH (descer) | GC:1107 | `player_base_crouch` | 9 | 0–8 (9) | 1,20×9 | 10,80 | não | `agachar.mp4` q2–10 |
| CROUCH_IDLE (agachado parado) | GC:1149 | `player_base_crouch_idle` | 35 | 0–34 (35) | 2,52×35 | 88,20 (1,47 s) | sim | `agachar.mp4` q35–69 |
| RISE (levantar do agachado) | GC:1108 | `player_base_rise` | 11 | 0–10 (11) | 0,90×6, 0,84×5 | 9,60 | não | `agachar.mp4` q88–98 |
| JUMP (subida, quando vy < −35) | GC:1109; SM:31 | `player_base_jump` | 10 | 0–9 (10) | 2,40×10 | 24,00 | não | `pulo.mp4` q35–57 |
| FALL (descida) | GC:1110 | `player_base_fall_air` | 14 | 0–13 (14) | 1,98×12, 1,92×2 | 27,60 | não | `pulo.mp4` q60–86 de 2 em 2 |
| LAND (pouso) | GC:1113 | `player_base_land` | 9 | 0–8 (9) | 0,84×2, 0,96×2, 1,08×2, 1,20×2, 1,44 | 9,60 | não | `pulo.mp4` q90–104 |
| L (LIGHT_JAB) | GC:1114 | `player_base_jab` | 11 | 0–10, depois 8,6,4,2,0 (16) | 0,24×6, 0,96, 0,90×9 | 10,50 | não | `L.mp4` q4–14 |
| M (MEDIUM_KICK; é um soco, o nome é legado) | GC:1115 | `player_base_medium_kick` | 18 | 0–17 (18) | 1,20, 0,60×6, 1,20×11 | 18,00 | não | `M.mp4` q10–38 |
| H (HEAVY_STRAIGHT; é o chute alto circular) | GC:1116 | `player_base_heavy_straight` | 30 | 0–29 (30) | 1,92×5, 1,08, 0,90×24 | 32,28 | não | `H.mp4` q11–98 de 3 em 3 |
| →M (GOLPE_ALTO, overhead) | GC:1150 | `player_base_golpe_alto` | 18 | 0–17 (18) | 4,92×4, 1,68, 0,90×2, 1,86×4, 0,90×7 | 36,90 | não | `M_BAIXO.mp4` q33–96 |
| 2L (CROUCH_LIGHT) | GC:1125 | `player_base_crouch_light` | 20 | 0–19 (20) | 0,24×7, 1,02, 0,60, 1,20×4, 0,60×7 | 12,30 | não | `2L.mp4` q32–70 |
| 2M (CROUCH_MEDIUM, rasteira) | GC:1126 | `player_base_crouch_medium` | 26 | 0–25 (26) | 1,98×3, 1,02×7, 1,98×5, 1,02×11 | 34,20 | não | `2M.mp4` q12–111 |
| 2H (CROUCH_HEAVY, lançador) | GC:1127 | `player_base_crouch_heavy` | 20 | 0–19 (20) | 2,10×3, 1,38, 0,72, 1,02, 0,66, 0,96, 1,98×6, 0,96×6 | 28,68 | não | `2H.mp4` q16–103 |
| jL (JUMP_LIGHT) | GC:1128 | `player_base_jump_light` | 20 | 0–19 (20) | 0,84×3, 0,60, 0,24, 1,02, 0,30, 0,42, 0,66×6, 0,30×6 | 10,86 | não | `jL.mp4` q3–75 |
| jM (JUMP_MEDIUM) | GC:1129 | `player_base_jump_medium` | 22 | 0–21 (22) | 0,78×7, 1,02×10, 0,54×5 | 18,36 | não | `jM.mp4` q0–90 |
| jH (JUMP_HEAVY) | GC:1130 | `player_base_jump_heavy` | 22 | 0–21, depois 4,2,1,0 (26) | 1,62×4, 1,02, 0,54×2, 1,02, 0,84×2, 1,68×3, 0,84×9, 0,54, 0,84×3 | 26,94 | não | `jH.mp4` q0–66. O chute sai até 18% menor (`reescala` no CLIP) |
| ↓+jH (j2H, JUMP_HEAVY_DOWN) | GC:1141 | `player_base_jump_heavy_down` | 25 | 0–24 (25) | 0,66×4, 1,26×4, 0,42, 0,24×2, 0,96, 0,78, 1,50×6, 0,78×6 | 24,00 | não | `jH_BAIXO.mp4` q0–96 |
| S1 Onda (SPECIAL_ENERGY) | GC:1133 | `player_base_energy` | 33 | 0–32 (33) | 0,60×19, 2,28, 2,52×12, 1,68 | 45,60 | não | `s1_corpo.mp4` q4–42 de 2 em 2 e q72–108 de 3 em 3 |
| S1 segurado (SPECIAL_CHARGE) | GC:1145 | `player_base_energy_charge` | 12 | 0–11, depois 10–1 (22, vai e volta) | 4,98×22 | 109,56 | sim | `s1_corpo.mp4` q44–66 |
| S1 carregado (SPECIAL_ENERGY_CHARGED) | GC:1146 | `player_base_energy_charged` | 36 | 0–35 (36) | 0,36×19, 3,54, 0,72×2, 1,02, 2,64×12, 1,68 | 46,20 | não | `s1_corpo.mp4` |
| S2 Punho ascendente (SPECIAL_S2) | GC:1136 | `player_base_special_s2` | 28 | 0–27 (28) | 2,34, 1,74, 1,20, 0,54×4, 1,02, 1,26, 1,86×2, 2,52×10, 1,86×2, 1,26, 3,72, 4,98×2, 2,52 | 59,82 | não | `s2.mp4` q8–104 |
| S3 Chute giratório (SPECIAL_S3) | GC:1137 | `player_base_special_s3` | 44 | 0–43 (44) | 1,62, 1,08×5, 1,62×3, 1,98, 2,04×24, 1,38, 2,04×2, 1,38×3, 2,04×4 | 80,58 | não | `player_base_special_s3.mp4` q3–123 |
| S4 Aparar (SPECIAL_S4) | GC:1138 | `player_base_special_s4` | 21 | 0–20 (21) | 1,98×9, 0,84, 0,90, 1,02, 1,14, 2,70×3, 2,40×4, 2,70 | 42,12 | não | arte antiga `player_base_special_s4_sr_hd.png` (sem campo `video`) |
| Super (SUPER_WAVE) | GC:1139 | `player_base_super` | 24 | 0–23 (24) | 12,42×14, **135,00**, 0,06×9 | 309,42 | não | `super_corpo.mp4` q0–42 e q96–120 de 3 em 3 |
| Ultra raio (ULTRA_BEAM) | GC:1132 | `player_base_ultra_beam` | 9 | 0–8 (9) | 6,00×9; no jogo a imagem é escolhida pela fase, não pelo tempo | 54,00 | não | `super_corpo.mp4` q0, 24, 30, 42, 96, 99, 102, 108, 114 |
| Ultra SHINKAI (ULTRA_DRAGAO) | GC:1153 | `player_base_ultra_dragao` | 36 | 0–35 (36) | 7,50×18, 15,00, 7,50×17; no jogo a imagem é escolhida pela fase | 277,50 | não | `ultra_shinkai.mp4` q0–108 |
| Ativação do SHINKAI | GC:1147 | `player_base_shinkai` | 26 | 0–25 (26) | 8,10×8, 16,02×4, 8,10×13, 8,04 | 242,22 (4,04 s) | não | `shinkai.mp4` q34–92 (escala 0,4924) |
| Defesa em pé 1 (DEFENSE_STAND) | GC:1117 | `player_base_defense_stand` | 13 | 0–12 (13) | 32,52, 22,50, 2,52×4, 160,02, 4,98×6 | 255,00 | não | `defesa.mp4` q24, 11, 20–24, 88–98 |
| Defesa em pé 2 (DEFENSE_STAND_2) | GC:1151 | `player_base_defense_stand_2` | 14 | 0–13 (14) | 37,50, 25,02, 2,52×5, 70,02, 4,98×6 | 175,02 | não | `defesa_esquerda.mp4` q36, 21, 31–36, 64–74 |
| Defesa em pé 3 (DEFENSE_STAND_3) | GC:1152 | `player_base_defense_stand_3` | 12 | 0–11 (12) | 30,00, 22,50, 2,52×3, 160,02, 4,98×6 | 249,96 | não | `defesa_direita.mp4` q18, 6, 15–18, 82–92 |
| Defesa agachada (DEFENSE_CROUCH) | GC:1118 | `player_base_defense_crouch` | 6 | 0–5, depois 0 (7) | 6,00, 2,40×6 | 20,40 | não | arte antiga |
| Defesa no ar (DEFENSE_AIR) | GC:1131 | `player_base_defense_air` | 6 | 0–5, depois 0 (7) | 6,00, 2,40×6 | 20,40 | não | arte antiga |
| Dano em pé (HIT_STAND) | GC:1119 | `player_base_hit_stand` | 9 | 0–8 (9) | 1,80, 2,10, 2,40×2, 2,70×4, 3,60 | 23,10 | não | arte antiga |
| Dano agachado (HIT_CROUCH) | GC:1120 | `player_base_hit_crouch` | 9 | 0–8 (9) | 2,40×9 | 21,60 | não | arte antiga |
| Dano no ar (HIT_AIR) | GC:1121 | `player_base_hit_air` | 4 | 0–3 (4) | 3,30×4 | 13,20 | não | arte antiga |
| Queda (KNOCKDOWN) | GC:1122 | `player_base_fall` | 11 | 0–6 (7) | 1,86×7 | 13,02 | não | arte antiga |
| Caído (GROUNDED) | GC:1123 | `player_base_fall` | 11 | 7, 8, 9, 10 (4) | 3,60×3, 61,20 | 72,00 | não | arte antiga |
| Levantar (GETUP) | GC:1124 | `player_base_getup` | 12 | 0–11 (12) | 1,80×10, 1,50×2 | 21,00 | não | arte antiga |
| Agarrão: pegar (THROW_GRAB) | GC:1134 | `player_base_throw` | 9 | 0–3 (4) | 1,20, 1,50×2, 1,80 | 6,00 | não | arte antiga |
| Agarrão: jogar (THROW_TOSS) | GC:1135 | `player_base_throw` | 9 | 3–8 (6) | 1,80, 3,60, 3,00, 2,40×2, 2,82 | 16,02 | não | arte antiga |
| Intro (INTRO) | GC:1140 | `player_base_intro` | 27 | 0–26 (27) | 5,40×16, 3,60×7, 8,10×4 | 144,00 (2,4 s) | não | arte antiga |
| Vitória (VICTORY) | GC:1142 | `player_base_victory` | 33 | 0–32 (33) | 7,50×33 | 247,50 | não | arte antiga |
| Derrota (DEFEAT) | GC:1143 | `player_base_defeat` | 33 | 0–32 (33) | 7,50×33 | 247,50 | não | arte antiga |
| Provocação (TAUNT) | GC:1144 | `player_base_taunt` | 33 | 0–32 (33) | 7,50×33 | 247,50 | não | arte antiga |

Notas sobre as **artes antigas** (sem vídeo novo): danos, defesa agachada e no ar, queda, levantar, agarrão, intro, vitória, derrota, provocação, S4 e ativação do SHINKAI. Fontes:

- `docs/personagens/p01.md`: "Falta: danos, agachado defendendo, intro, vitória e o S4 (Aparar) continuam os antigos".
- CLIP sem campo `video`. Exceção: o `shinkai` tem campo `video` com `personagem` ausente, por isso a calibração o trata como antigo.

## 5. Golpes: frame data e quais imagens caem em cada fase

### 5.1 Frame data (60 Hz)

| Golpe | Definição | Startup / ativo / recuperação (motor) | Startup SF (1º quadro ativo) | Total | Hitstun / blockstun / hitstop | Congelamento no acerto (pose segurada) | Vantagem (acerto / bloqueio) | Recuperação cortada no acerto | Alcance (u) / altura |
|---|---|---|---|---|---|---|---|---|---|
| L | GC:1154 | 2 / 4 / 5 | 3 | 11 | 12 / 7 / 8 | 8+4 = **12** | +4 / −1 | termina no quadro 8 (9 quadros) | 114 (limite 135) / 188 |
| M | GC:1155 | 4 / 6 / 8 | 5 | 18 | 15 / 9 / 10 | 10+5 = **15** | +2 / −4 | termina no quadro 12 (13 quadros) | 185 (limite 185) / 233 |
| H | GC:1156 | 10 / 5 / 17 | 11 | 32 | 26 / 16 / 12 | 12+6 = **18** | +5 / −5 (ricochete na parede) | termina no quadro 19 (20 quadros) | 224 / 214 |
| →M (6M, overhead) | GC:1164 | 20 / 3 / 14 | 21 | 37 | 20 / 11 / 7 | 7+5 = **12** | +4 / −5 | termina no quadro 31 (32 quadros) | 190 (limite 190) / 124 |
| 2L (baixo) | GC:1157 | 2 / 4 / 6 | 3 | 12 | 16 / 7 / 8 | 8+4 = **12** | +7 / −2 | termina no quadro 8 (9 quadros) | 175 (limite 175) / 198 |
| 2M (baixo, derruba) | GC:1158 | 12 / 4 / 18 | 13 | 34 | 41 / 11 / 10 | 10+5 = **15** | derruba / −6 | só no bloqueio: termina no quadro 20 | 200 (limite 200) / 44 |
| 2H (lança) | GC:1159 | 9 / 6 / 14 | 10 | 29 | 44 / 7 / 12 | 12+6 = **18** | lança / −12 | não (lançado não está em HITSTUN). Cancela em pulo. | 96 / 307 |
| jL | GC:1160 | 4 / 2 / 5 | 5 | 11 | 14 / 10 / 8 | 8+4 = **12** | (no ar) | não (só no chão) | 180 (limite 180) / 177 |
| jM (crossup) | GC:1161; CC (`crossUpMove = "jM"`) | 6 / 4 / 8 | 7 | 18 | 16 / 12 / 10 | 10+5 = **15** | (no ar) | não | 197 / 106 |
| jH (ricochete na parede) | GC:1162 | 9 / 6 / 12 | 10 | 27 | 18 / 14 / 12 | 12+6 = **18** | (no ar) | não | 173 / 129 |
| ↓+jH (j2H, quique no chão) | GC:1163 | 9 / 6 / 9 | 10 | 24 | 18 / 14 / 12 | 12+6 = **18** | (no ar) | não | 68 / 7 |
| S1 Onda (↓↘→) | GC:1171 (`energy`) | 12 / 1 / 33 | 13 | 46 | 20 / 14 / 6 | só quem apanha congela (projétil) | — | — | projétil: alcance 720, 760 u/s, sai 110 à frente e a 185 de altura; carga até 60 quadros, +80% |
| S2 Punho ascendente (→↓↘) | GC:1165 | 8 / 12 / 40 | 9 | 60 | 30 / 12 / 10 | 10 (sem extra) | derruba | — | 82 / 140 |
| S3 Chute giratório (↓↙←) | GC:1166 | 12 / 43 / 25 | 13 | 80 | 10 / 4 / 9 (pancadas do meio: max(3, 9×2/3) = 6, CE:5002-5004) | 9 no último | derruba (empurrão 70) | — | 170 / 230 |
| S4 Aparar (↓↓) | GC:1167 | 20 / 4 / 18 | 21 | 42 | 22 / 14 / 9 | 9 | — | — | 148 / 187 |
| Super Onda Máxima | GC:1171 (`superAttack`) | 35 / 1 / 25 | 36 | 61 | 30 / 20 / 12 | — | — | — | projétil: alcance 1450, 1180 u/s, 5 acertos a cada 8 quadros |
| Agarrão (L+M, do motor) | CC:312-326 | 5 / 3 / (erro 24; acerto: segura 12 + executa 16) | 6 | — | hitstop 10 | — | — | — | alcance 40 (vão entre corpos); dano 1200, porque o Ryūga não tem dano próprio (`throwDamage` 0) |

Mecânicas dos especiais:

- **S2:** o desenho sobe em arco de **170 u entre os quadros 7 e 42**. É só desenho: "o corpo segue no chão" (`AttackDefinition.java:97-101`, `:146-151`; GC:1165 `hop(170,7,42)`; GV:5647). Invencibilidade: **não achei**. `docs/personagens/p01.md` diz "sem invencibilidade".
- **S3:** 4 pancadas nos quadros **12, 22, 40, 52** (GC:1166 `strikes`), que caem nas imagens 9, 14, 23 e 29 (`tools/sprites/pancadas.json:5-10`). Avança **260 u/s do quadro 4 ao 71 (~295 u)** e para se encostar no rival (CE:2490-2496). O desenho sobe em arco de 110 u entre os quadros 4 e 64.
- **S4:** contra-golpe que lê qualquer golpe corpo a corpo entre os quadros **4 e 13** e apara (GC:1167 `counter(ANY,4,13).parry()`; `tools/sprites/mecanicas.json` no bloco `player_base.unica`).

Comandos: S1 `{3,2,1}` = ↓↘→; S2 `{1,3,2}` = →↓↘; S3 `{3,4,5}` = ↓↙←; S4 `{3,3}` = ↓↓, todos com L, M ou H (GC:1165-1167, 1171). O mesmo está em `tools/sprites/mecanicas.json` (`player_base.comandos`).

Golpes de comando além do →M (6M): **não achei**. O `6M` é o único normal de comando no pack (GC:1164).

### 5.2 Quantas imagens por golpe e onde cada uma cai

- A imagem de cada quadro de jogo foi calculada com a regra do motor: tempo do clipe = `(quadro+0,5)/60 × duração_clipe/duração_golpe` (CD:158-161; `ImpactFeel.java:272`), aplicada às durações de GC.
- A tabela vale para o golpe que **erra**. No acerto ou bloqueio vale a volta seca da seção 1.
- "Imagem de impacto" é o `impactFrame` de `characters/player_base/character.json` (posição na sequência). O `tools/sprites/mecanicas.py:92-110` (`fit_impact`) põe essa imagem para começar meio quadro antes do 1º quadro ativo.

| Golpe | Imagens (folha) / quadros de jogo | Fator de esticamento | Startup | Ativo | Recuperação | Imagens nunca mostradas | Imagem de impacto |
|---|---|---|---|---|---|---|---|
| L | **11 imagens (16 posições) para 11 quadros** | 11/10,5 = 1,048 | q0–1: imgs 1, 5 | q2–5: imgs 6, 8, 9, 10 | q6–10: imgs 8, 6, 4, 2, 0 (a volta pelo inverso) | 3, 7 (0, 2 e 4 só aparecem na volta) | pos 6 = img 6, no 1º quadro ativo |
| M | 18 / 18 | 1,000 | q0–3: 0, 1, 3, 4 | q4–9: 6, 7, 8, 9, 10, 10 | q10–17: 11–15, 15, 16, 17 | **2 e 5**. A 5 é o impacto declarado: dura 0,6 tique e cai entre dois quadros. | img 5 (não aparece; no contato aparece a 6) |
| H | 30 / 32 | 0,991 | q0–9: 0–4 (2 quadros cada) | q10–14: 5, 7, 8, 9, 10 | q15–31: 11–29 sem a 16 e a 25 | 6, 16, 25 | img 5, no 1º quadro ativo |
| →M (6M) | 18 / 37 | 1,003 | q0–19: imgs 0–3, **5 quadros cada** (preparo segurado) | q20–22: 4, 5, 6 | q23–36: 7, 7, 8, 8, 9, 9, 10, 10, 11, 13–17 | 12 | img 4, no 1º quadro ativo |
| 2L | 20 / 12 | 0,976 | q0–1: 2, 6 | q2–5: 7, 9, 10, 10 | q6–11: 11, 12, 14, 15, 17, 19 | **9 de 20**: 0, 1, 3, 4, 5, 8, 13, 16, 18 | img 7, no 1º quadro ativo |
| 2M | 26 / 34 | 0,994 | q0–11: 0, 0, 1, 1, 2, 2, 3, 4, 5, 6, 7, 8 | q12–15: 9, 10, 10, 11 | q16–33: 11, 12, 12, 13, 13, 14, 14, 15–25 | nenhuma | img 9, no 1º quadro ativo |
| 2H | 20 / 29 | 1,011 | q0–8: 0, 0, 1, 1, 2, 2, 3, 3, 5 | q9–14: 5, 7, 8, 8, 9, 9 | q15–28: 10, 10, 11, 11, 12, 12, 13, 13, 14–19 | 4, 6 | img 5 (no último quadro de startup e no 1º ativo) |
| jL | 20 / 11 | 1,013 | q0–3: 0, 1, 2, 5 | q4–5: 6, 8 | q6–10: 9, 11, 12, 15, 18 | **9 de 20**: 3, 4, 7, 10, 13, 14, 16, 17, 19 | img 5 (no último quadro de startup) |
| jM | 22 / 18 | 0,980 | q0–5: 0, 1, 3, 4, 5, 7 | q6–9: 8, 9, 10, 11 | q10–17: 12–17, 19, 21 | 2, 6, 18, 20 | img 7 (no último quadro de startup) |
| jH | 22 imagens (26 posições) / 27 | 1,002 | q0–8: 0, 0, 1, 2, 2, 3, 4, 4, 6 | q9–14: 7, 9, 10, 10, 11, 11 | q15–26: 12–17, 19, 20, 21, 2, 1, 0 | 5, 8, 18 | img 7, no 1º quadro ativo |
| j2H | 25 / 24 | 1,000 | q0–8: 0, 2, 3, 4, 5, 6, 7, 7, 10 | q9–14: 11, 13, 13, 14, 15, 15 | q15–23: 16, 17, 17, 18–21, 23, 24 | 1, 8, 9, 12, 22 | img 11, no 1º quadro ativo |
| S1 | 33 / 46 | 1,009 | q0–11: 0, 2, 4, 5, 7, 9, 10, 12, 14, 15, 17, 19 | q12: 19 | q13–45: 19, 20, 20, 21… 32 (~2,5 quadros por imagem) | **8**: 1, 3, 6, 8, 11, 13, 16, 18 | img 19 (o disparo), q11–13 |
| S1 carregado | 36 / 46 | 0,996 | q0–11: 1, 4, 6, 9, 12, 15, 18, 19, 19, 19, 20, 21 | q12: 22 | q13–45: 23–35 | **12**: 0, 2, 3, 5, 7, 8, 10, 11, 13, 14, 16, 17 | img 22 |
| S2 | 28 / 60 | 1,003 | q0–7: 0, 0, 1, 1, 2, 3, 5, 7 | q8–19: 8, 8, 9, 9, 10, 11, 11, 11, 12, 12, 12, 13 | q20–59: 13–27 (2 a 5 quadros cada) | 4, 6 | img 7 (último quadro de startup) |
| S3 | 44 / 80 | 0,993 | q0–11: 0, 0, 1–6, 6, 7, 8, 8 | q12–54: imgs 9–30 (~2 quadros cada) | q55–79: 30–43 | nenhuma | img 9, na 1ª pancada |
| S4 | 21 / 42 | 0,997 | q0–19: 0–8 (2 quadros cada), 9, 10 | q20–23: 11, 12, 13, 13 | q24–41: 14–20 (2 a 3 quadros cada) | nenhuma | img 11, no 1º quadro ativo |
| Super | 24 / 61 | **0,197** (clipe de 309,42 tiques encolhido para 61 quadros) | q0–34: imgs 0–14 (~2,4 quadros cada) | q35: 14 | q36–60: **img 14 parada 27 quadros** | **9**: 15–23 (a volta à guarda, 0,06 tique cada: na prática nunca aparecem) | img 14 (o disparo) |

Resumo "imagens / quadros de jogo":

| Golpe | Imagens / quadros |
|---|---|
| L | 11 (16 posições) / 11 |
| M | 18 / 18 |
| H | 30 / 32 |
| →M | 18 / 37 |
| 2L | 20 / 12 |
| 2M | 26 / 34 |
| 2H | 20 / 29 |
| jL | 20 / 11 |
| jM | 22 / 18 |
| jH | 22 / 27 |
| j2H | 25 / 24 |
| S1 | 33 / 46 |
| S2 | 28 / 60 |
| S3 | 44 / 80 |
| S4 | 21 / 42 |
| Super | 24 / 61 |

Nos golpes rápidos (L, 2L, jL, jM, S1 carregado) há mais imagens que quadros de jogo, então parte delas nunca aparece.

## 6. Defesa

O clipe de defesa é escolhido assim:

- **Guarda segurada** (ameaça chegando, sem bloquear): mostra a imagem 0 (`CD:141-145` `guardTime`: `!blocking → 0`).
- **Durante o blockstun:** primeiro a imagem 0 fica pelo tempo dela (`hold = durations[0]`). Depois, o resto do clipe é esticado sobre o blockstun (`progress = stunElapsed/stunTotal`) (GV:5121-5133).
- **Defesa em pé em rodízio:** DEFENSE_STAND → _2 → _3, mudando a cada golpe defendido. A guarda segurada já mostra a próxima da fila (GV:5091-5095 `defesaEmPe`; GV:5127).

| Defesa | Guarda segurada | O que aparece no blockstun (simulado com a regra acima) | "Pose segurada" | Fonte |
|---|---|---|---|---|
| Em pé 1 | img 0 (vídeo q24) | blockstun 7 (L): 1, 5, 6, 6, 6, 6, 6. Blockstun 16 (H): 1, 1, 4, 6×11, 7, 10 | img 6, que é o mesmo quadro de vídeo (q24) da guarda: ~72% do tempo de reação | GC:1117; CLIP defense_stand `quadros` "24,11,20,21,22,23,24,88…" |
| Em pé 2 | img 0 (q36) | 7: 1, 1, 7, 7, 7, 7, 10. 16: 1, 1, 1, 2, 5, 7×8, 8, 10, 12 | img 7 (= q36), ~51% | GC:1151 |
| Em pé 3 | img 0 (q18) | 7: 1, 5×6. 16: 1, 1, 3, 5×11, 6, 9 | img 5 (= q18), ~73% | GC:1152 |
| Agachada | img 0 (6 tiques) | 7: 1, 1, 2, 3, 4, 5, 0 (as 6 imagens divididas por igual) | não tem | GC:1118 |
| No ar | img 0 | igual à agachada | não tem | GC:1131 |

## 7. Reações a dano, queda e levantar

- **Fraco, médio e forte:** usam o **mesmo clipe**. Clipes separados por força: **não achei**. A diferença vem do hitstun, que estica o clipe:
  - HIT_STAND (9 imagens, 23,1 tiques) é esticado sobre o hitstun (GV:4941-4945, 5055-5059 `hitReactionTime`).
  - Exemplos simulados: L (hitstun 12) → imgs 0, 1, 2, 3, 3, 4, 5, 6, 6, 7, 8, 8. H (26) → cada imagem fica ~3 quadros.
- **Alto e baixo:** em pé usa HIT_STAND e agachado usa HIT_CROUCH (9 imagens, 21,6 tiques, a mesma regra) (GV:4941-4944). Reação desenhada separada para golpe alto ou baixo no corpo em pé: **não achei**. O motor de animação imita com inclinação e arrasto ("Onde pegou", `docs/motor-animacao.md`). O próprio doc diz que "o desenho certo é arte nova".
- **No ar:** HIT_AIR tem 4 imagens.
  - Lançado ou cravado no chão: a pose segue a física (GV:4934-4938, `launchPoseTime`): subindo rápido (vy < −500) → img 0; subindo → img 1 (0,07 s); cravado → img 2 (0,13 s); caindo → img 3 (0,19 s).
  - Hitstun aéreo comum: o clipe corre em velocidade normal (`stunElapsed × 1/60`).
- **Queda (knockdown):**
  - KNOCKDOWN: 7 imagens em 13 quadros, em velocidade normal (`knockdownFallFrames = 13`, CC:74; GV:4917-4921).
  - Depois vem GROUNDED nos **22 quadros deitado** (CC:81): imagens 7, 8 e 9 com ~3,6 quadros cada; a imagem 10 fica parada até levantar (GV:4922-4924).
  - Quique só no desenho: 30 u em 14 quadros (CC:87-88; GV:5648).
- **Levantar:** GETUP tem 12 imagens e 21 tiques, esticados sobre **16 quadros** (`wakeupFrames`, CC:82; o Ryūga não tem valor próprio; CE:1964-1968; GV:4928-4932).
- Total da queda até ficar de pé: 13 + 22 + 16 = **51 quadros**. Opções ao acordar:
  - levantar rápido (↑): 6 quadros deitado;
  - rolar: 24 quadros;
  - levantar atrasado (↓): +30 quadros.
  
  Fonte: CC:350-355; CE:1680-1697.
- **KO:**
  - KO em pé: toca a DEFEAT uma vez em **velocidade 2×** (`DEFEAT_SPEED = 2`, GV:390, GV:5290-5295). São 33 imagens × 7,5 tiques / 2 = **123,75 quadros**, e a última pose fica.
  - KO no chão ou no ar: fica deitado (GV:5288-5289, comentário).

## 8. Agarrão (L+M)

O Ryūga usa a arte dedicada: THROW_GRAB tem 4 imagens (folha 0–3) e THROW_TOSS tem 6 (folha 3–8) (GC:1134-1135).

| Fase | Duração | O que o clipe faz | Fonte |
|---|---|---|---|
| Startup | 5 quadros | o clipe de pegar (6 tiques) é esticado sobre eles | GV:5673-5676 |
| Segurando (janela de tech) | 12 quadros | fica na última imagem do pegar | GV:5678-5680; CC:318; CE:3880-3885 |
| Jogando | 16 quadros | o clipe de jogar (16,02 tiques) é esticado sobre eles | GV:5682-5685; CC:323 |
| Erro | 24 quadros | o pegar vai e volta | GV:5687-5688; CC:316 |

## 9. Intro, vitória, derrota, provocação, Ultra, SHINKAI

| Estado | Comportamento | Fonte |
|---|---|---|
| Intro | 27 imagens, 144 tiques (**2,4 s**) em velocidade normal. A intro da luta dura o maior clipe de intro dos dois (`introTotalFrames`). No Ryūga, das imagens 22 a 26 o desenho funde com o IDLE. | GC:1140; GV:2307-2313; GV:5110-5114; SFR:217-224 |
| Vitória | Começa na **imagem 7** (as imagens 0–6 são uma caminhada até a câmera) e toca em velocidade normal: (33−7) imagens × 7,5 tiques ≈ **191 tiques (~3,2 s)**. Depois segura a última pose. | GC:1142; GV:5267-5284 |
| Derrota | Seção 7 (2×, ~124 quadros, só KO em pé) | GV:5290-5295 |
| Provocação | Só no treino (o boneco provoca quando o jogador erra). 33 imagens × 7,5 tiques = 4,125 s. | GV:5255-5260 |
| Ultra (raio) | A imagem das 9 poses é escolhida pela fase em `RaioFinal.poseFrame`: carga 0–2, disparo 3, segurar com ciclo, explosão 7, apagar 8. Tempos: ativação 25, carga 36, extensão 8, acerto a cada 4, intervalo até a explosão 6, apagar 18 quadros. | GC:1132; GV:5300-5308; `android/ultra-core/src/main/java/com/gamelutagpt/ultra/RaioFinal.java:61-72`; CC:219-307 |
| Ultra SHINKAI (dragão) | Imagens 0–18 na carga (`dragaoPreparo = 80` quadros); 19 é o empurrão nos primeiros 6 quadros; 20–26 vão e voltam a cada 4 quadros; 27–35 na recuperação (30 quadros). | GV:5098; GV:5328-5345; CC:768; CC:221 |
| Ativação do SHINKAI | 26 imagens, 242,22 tiques (4,04 s), tocadas inteiras com a luta parada | GC:1147; GV:5104-5108 |

## 10. Motor de animação e interpolação

Fontes: `docs/motor-animacao.md`; `android/motion-core/…/Ajustes.java`; `SpriteFighterRenderer.java:120-185`.

- **Não interpola imagens no jogo.** Não há tweening nem quadros intermediários em tempo real: a tela sempre mostra uma imagem inteira da folha. O motor só transforma o desenho em volta dos pés, e isso não muda o frame data, as caixas nem a física (`docs/motor-animacao.md`, primeiras linhas):
  - achatar e esticar;
  - inclinar;
  - cisalhar (arrasto);
  - smear;
  - fantasma na troca de clipe;
  - rastro.
- **Ligado por padrão** em intensidade 1 (`Ajustes.java:11` `ligado = true`; `:13` `intensidade = 1f`). O botão ANIM ou a tecla F4 alternam entre 100%, 150%, 50% e desligado.
- Efeitos e valores padrão, todos em `Ajustes.java`:

  | Efeito | Valor padrão | Linha |
  |---|---|---|
  | Achatar ao pousar | 3,5% | 27 |
  | Esticar ao decolar | 2,5% | 27 |
  | Agachar no pré-pulo | 3% | 74 |
  | Antecipação na preparação | 1° a 3° por força, achatar 1% | 96-97 |
  | Estalo ao ativar | 0,8% a 2,2% | 102 |
  | Continuidade | 1° a 3° | 107 |
  | Smear nos 2 primeiros quadros ativos dos fortes | esticado 18% | 115-116 |
  | Inclinação ao apanhar | 2° a 5,5° | 42 |
  | Inclinação ao defender | 2° | 43 |
  | Inclinação no dash | 3° | 78 |
  | Arrasto | 3° | 86 |
  | Transição de clipe (imagem anterior por cima, sumindo) | 2 quadros, 35% | 131-132 |
  | Volta do golpe (última imagem do golpe sumindo por cima da guarda) | 8 quadros | 138-139 |
  | Rastro (só dash, backdash e air dash): cópias / vida | 3 cópias / 8 quadros | 148-149 |
  | Cansaço (vida < 25%): idle mais lento | 20% mais lento (`ritmoCansado` 0,8) | 71 |
  
  O idle mais lento no cansaço é aplicado em GV:5403-5408.
- Antecipação, continuidade, smear e estalo **só valem nos normais** (L, M, H, em pé, agachado e no ar). Os especiais e o Super já têm o movimento no desenho (`docs/motor-animacao.md`, "Só nos golpes normais").
- `pesoAnimacao` do Ryūga: não declarado, então vale o padrão 1 (CD:568-569; GC:1171 sem `withAnimationWeight`).
- **Quadros intermediários fora do jogo:** existe a ferramenta `tools/sprites/intermediarios.py`, mas no p01 **nenhum quadro foi trocado**. O doc diz: "os clipes vieram de vídeo e já são densos… o meio sai com dupla exposição" (`docs/motor-animacao.md`, "Quadros intermediários").

## 11. Coisas que chamaram atenção (para a comparação com o SF)

1. **Super:** a sequência tem 24 imagens, mas, com o tempo do golpe (61 quadros), as 9 imagens da volta à guarda (15–23) duram 0,06 tique cada e **na prática não aparecem**. A imagem 14 (o disparo) fica parada nos últimos 27 quadros, e depois o desenho troca direto para o idle, só com a transição de 2 quadros. Fonte: GC:1139. A origem é o `fit_impact` aplicado ao Super (`tools/sprites/montagem.py:237-240`; `tools/sprites/mecanicas.py:92-110`).
2. **Backdash:** o clipe tem 28 imagens pensadas para 15+12 quadros, mas só aparecem as ~15 primeiras, enquanto o corpo anda (GV:5419; SM:35).
3. **M:** a imagem de impacto declarada (5) dura 0,6 tique e nunca é amostrada. No contato aparece a imagem 6.
4. **Golpes curtos com imagens descartadas:**

   | Golpe | Imagens descartadas |
   |---|---|
   | 2L | 9 de 20 |
   | jL | 9 de 20 |
   | S1 | 8 de 33 |
   | S1 carregado | 12 de 36 |
   | L | 2 das 11 imagens nunca aparecem (3 e 7) |

5. **Pulo e queda:**
   - FALL tem 14 imagens (27,6 tiques), mas a descida do pulo normal dura ~20 quadros. As imagens 11–13 não chegam a aparecer num pulo vazio.
   - LAND toca 9 imagens em ~10 quadros, mas o lag de pouso do motor é de só 3 quadros. O clipe continua até o fim, porque o LAND tem prioridade sobre andar (SM:34).
6. **Andar:** 170 e 140 u/s são baixos perto do padrão do motor (300 e 220). O pulo vai bem mais longe que o andar: 280 u em 42 quadros, ou 6,67 u/quadro, contra 2,83 u/quadro andando.
7. **O que não achei:**
   - reações a dano separadas por força ou altura;
   - largada ou freada da corrida;
   - invencibilidade do backdash e do S2;
   - outros golpes de comando além do →M.
