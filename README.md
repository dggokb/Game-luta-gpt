## P01 v0.94 — jump less floaty, slower 2M, matched intro/idle and planted victory

- **Normal jump, universal:** keeps the 820 impulse, but applies 1.10× gravity on ascent and 1.25× on descent only to ordinary jumps. The shorter flight preserves enough height to pass over a standing opponent. Super jump, launcher, juggle and projectile arcs are unchanged.
- **P01 2M:** 26→30 gameplay frames; first contact remains at frame 6 with identical startup/active windows. Recovery 16→20; all 16 original poses remain visible.
- **Intro→idle:** reduce P01 idle 2% and recalibrate late intro frame sizes and foot/root offsets to the NEW idle silhouette; source sprites unchanged. The ending intro crossfade still renders the actual idle texture.
- **Victory:** source frames 0–6 contain walking. P01 win animation starts on the planted pose at frame 7, uses 94% source scale (same drawn height as intro's first pose), corrects foot anchor; no fresh inputs or winner horizontal movement are processed after KO.
- **Verification:** sprite QA measures the real pixel heights and intro/idle boundary; combat tests verify crossing the opponent and that a winning character remains fixed even with held movement input.
- Test APK: `0.94-p01-jump-victory-2m`.

## P01 v0.93 — salto 820, backdash +15%, 2M de 26 frames, intro/idle

- Salto normal UNIVERSAL: impulso vertical 780→820, gravity inalterada;
  permite passar por cima do adversário e cair do outro lado.
- Backdash UNIVERSAL: 850→978 u/s com 15 frames, recuo de 212,5→244,5
  unidades (+15,06%), arco visual 40 inalterado.
- 2M P01: 6 startup + 4 ativos + 16 recuperação, total 26 frames;
  efeitos/hitstun/cancelamento sem alterações. Todos os 16 sprites exibidos
  em ordem, pose de impacto no quadro 6.
- Intro do P01: redução inicial de 3,5%; normalização vertical nos quadros
  20–21 (antes altos demais) e deslocamento exato da última pose até o
  IDLE (corrigindo ~6 px à direita e os pés). Crossfade mantém a arte
  original do idle no encerramento.
- QA mede ocupação real de pixels alfa e exige alinhamento <=2,5px.
- Identificação Android `0.93-p01-intro-2m-jump`.

## P01 v0.92 — normal jump crosses the opponent; backdash and 2M refinement

- Universal normal jump: initial vertical speed **700 -> 780 world units/s** (gravity constant at 1650); apex approx **178 units** after discrete physics, beyond the standing hurtbox (P01 156). New combat unit test requires player to cross an opponent in midair, land on the far side, and face back. Superjump untouched.
- Universal backdash: **820 × 14/60 ≈ 191 units -> 850 × 15/60 = 212.5 units** (+11%). Render-only hop height 34 -> 40 units, retaining grounded combat semantics.
- P01 crouching medium (2M): startup **6**, active **4** (unchanged), recovery **9 -> 11**; total **19 -> 21 frames** (+10.5%). Sprite timings 317 -> 351 milliseconds; first 6 poses unchanged, contact still shows frame 6; all 16 atlas poses preserved in playback.
- Regression tests: old 2H and L-M-H intact, global jump and backdash for both teams, and *physical crossing* over a standing opponent.
- Android test build **0.92-p01-jump-backdash-2m**.

## P01 v0.91 — 2H contínuo, ataques baixos e movimento global

- P01 2L: 10 → 12 frames; 2M: 16 → 19 frames; 2H: 24 → 29 frames.
  Só foi prolongada a recuperação. Os sprites após o impacto ficam mais
  tempo na tela, sem mudar o instante da hitbox. O teste a 60 FPS verifica
  que todos os 12 frames do 2H são exibidos na ordem, sem saltos.
- Dash P01: 40 → 48 ms por sprite (20% mais lento), sem mudar
  a velocidade de deslocamento.
- Backdash de todos os personagens: ~152 → ~191 unidades de recuo
  (+26%), com pequeno arco visual de 34 unidades de altura. Permanece
  um backdash terrestre nas regras de defesa/colisão (não habilita ataques aéreos).
- Pulo normal de todos: impulso inicial 660 → 700 unidades/s,
  ápice ~132 → ~148 unidades, cerca de 12% mais alto. Superpulo inalterado.
- Os testes verificam 2L/2M/2H, frames desenhados, dash, 2H lançando,
  L→M→H lançando, e física compartilhada pelos personagens.
- APK de teste: `0.91-p01-crouch-movement`.

## P01 v0.90 — ataques um pouco mais lentos (L, M, jL, jM, jH)

Os 5 movimentos ganharam 10%–12,5% de duração de jogo **somente na recuperação**:
L 10→11 frames, M 16→18, jL 10→11, jM 16→18, jH 24→27.
Startup, frames ativos, dano, hitstun, hitstop, alcance, lançamento e
cancelamento não foram alterados. Os tempos individuais dos sprites após o
frame de impacto aumentaram proporcionalmente, mantendo **o mesmo instante
em que a pose de contato aparece**. Os quadros originais não foram redesenhados.

A CI executa `P01AttackPacingTest` e a regressão anterior
`P01HeavyLaunchRegressionTest` para preservar L→M→H e wall bounce.
Identificação do APK: `0.90-p01-lm-air-pacing`.

P01 v0.89: sincronização exata do frame de contato visual 10 com a hitbox no frame de jogo 10, sem alterar a duração total do H. Teste automatizado exige o mesmo frame da arte no instante do golpe.

## Correção: o H volta a lançar no fim do combo (P01)

O H tinha o startup aumentado de 10 para **23 frames** quando o refinamento
estético deveria ter alterado apenas a velocidade visual. O M concede 15 frames
de reação: no combo L→M→H, o rival recuperava antes do H; o motor encerrava
a sessão e não aplicava o lançamento de parede (condição
`session.hitCount >= 2`). O H ainda parecia acertar, mas como um golpe
isolado, sem wall bounce.

Correção: startup 10, ativo 5, recuperação 37 (total 52 quadros a 60 FPS).
O H continua visualmente mais longo, mas o contato volta a ocorrer no tempo
certo do combo; durações dos 23 sprites ajustadas para o quadro de impacto
coincidir com o frame 10 da simulação e a recuperação ficar mais lenta.
Novos testes de regressão reais verificam L→M→H, mesma sessão de combo,
lançamento de parede e sincronia do desenho. A compilação do APK de teste
deve ser bloqueada se falharem.

**Reparo visual direcionado do H:** inspeção dos PNGs revelou que
o quadro 20 contém um trecho semitransparente no meio da coxa (RGB correto,
alfa errado) e o 21 contém pernas/canela borradas e deformadas. Agora o 20
tem a transparência do tecido restaurada apenas no polígono medido; o 21
temporariamente reutiliza o quadro seguinte (22), visualmente íntegro.
O reparo é idempotente e executa **antes do APK ser compilado**, além de ser
persistido automaticamente na branch pelo job de arte. O QA compara os
pixels desses quadros: não depende só do sucesso do compilador.

## P01 revisão geométrica (segunda tentativa)

Este refinamento substitui o ajuste anterior por **medidas reais dos contornos
opacos de cada frame**, sem normalizar pela célula inteira do atlas.
- CROUCH: tamanho decresce quadro a quadro com escala medida
  `0.974 ... 0.878`, mantendo largura visível em aproximadamente 302 pixels,
  igual ao IDLE e sem crescimento ao agachar.
- RISE: corrige separadamente os dois eixos, com transição ao quadro real
  de IDLE nos últimos frames e aumento da duração para 150 ms.
- INTRO: o último quadro da animação medido tem 319 x 420 px, enquanto IDLE
  tem 302 x 448 px. A correção usa escalas **X=.947, Y=1.067** e fecha com
  o frame exato do IDLE, não amplia os dois eixos em 6.5% como antes.
- H: agora 52 frames de jogo para 23 quadros de arte (~867 ms a 60 FPS);
  janela ativa e cancelamentos atualizados. Antes eram só 34, o que
  permitia pular muitos quadros intermediários visualmente.
- **Arte danificada do H:** nenhuma nova afirmação de que o buraco foi
  corrigido. O reparo automático anterior preencheu transparências sem
  localizar visualmente o defeito relatado. A inspeção gera folha numerada
  `p01-heavy-frames.png` para apontar o quadro/ponto exato.
- CI: `qa_p01_visual.py` valida as dimensões dos PNG reais e gera
  comparativo visual de IDLE, INTRO, CROUCH e RISE.

## Ajustes do P01 — outubro de 2026

- INTRO/IDLE: início do idle vira exatamente o último frame visual da intro, após
  uma transição progressiva de quatro frames e alinhamento suave de posição.
- Agachar: ajuste de escala progressivo no CROUCH (100% a 92%) e no RISE
  (92% a 100%), preservando a base no chão.
- Ataque H: duração de 24 para 34 frames em 60 FPS; startup 15,
  ativo 7 e recuperação 12. Cancelamentos e quadro de impacto sincronizados.
- Perna do H: reparo conservador de pequenos buracos *transparentes e fechados*
  por script em Python (Pillow/numpy/scipy), com relatório dos pixels corrigidos.
  Buracos abertos ou arte pintada incorretamente ainda exigem revisão visual.

## APK leve: refinamento exclusivo do P01

Esta branch só empacota os 39 atlas `player_base_*.png` do P01. O treino usa
dois lutadores independentes, P01 e P01 (Treino), compartilhando as mesmas artes.
O combate, cenários, TAG e animações do P01 permanecem ativos. Os pacotes-fonte
dos demais lutadores permanecem disponíveis para referência, mas fora do APK.

Depois de qualquer geração completa de sprites, rode:
```bash
python3 tools/sprites/build_characters.py --write
python3 tools/sprites/apply_p01_only.py
cd android && gradle assembleDebug
```
A rotina `apply_p01_only.py` preserva os atlas do P01, ajusta o elenco de treino e
remove os demais recursos empacotáveis. O workflow `build-apk.yml` compila e
executa o smoke test Android para esta branch, sem a suíte multijogador.

---

# Game Luta Sprite GPT

Branch `game-luta-sprite-gpt`. Versão `0.87-sprite-gpt-pack-v2` (fonte única: `versionName` em `android/app/build.gradle`).

## Character Pack Engine

A v0.54 incorpora o motor de pacotes da Astra v0.53 preservando a identidade GPT.
Cada personagem possui `characters/<id>/character.json`; o roster fica em
`characters/roster.json`. Animações e golpes L/M/H em pé são declarativos, e
novos IDs de animação não exigem condições novas no renderer.

O pipeline trabalha em staging, descobre todos os clips/pacotes, gera atlas,
metadados Java e relatórios, e só publica se tudo passar. Startup, janela ativa e
recovery compartilham o relógio definido no manifesto do golpe.

## Canonical Anatomy

`worldScale=1.0` não é suficiente para provar que duas artes foram desenhadas no
mesmo tamanho. A v0.54 adiciona `scaleMode: canonical-anatomy`: um frame comparável
do golpe é medido contra a pose canônica revisada do personagem usando altura e
bandas de silhueta. A mediana dos candidatos define a escala; dispersão excessiva
reprova a importação.

O Heavy de 9 frames é a regressão oficial. Ele agora usa canvas 320×256 e root
128/238. Um teste Android compara o tamanho visual do início do Heavy com o Idle e
falha se o golpe voltar a crescer.

## Build

```bash
python3 -m pip install -r tools/sprites/requirements.txt
python3 tools/sprites/build_characters.py --write
python3 -m unittest discover -s tools/sprites/tests -v
python3 tools/sprites/build_characters.py --check
cd android
gradle testDebugUnitTest assembleDebug --stacktrace
```

`--write` é o único passo que altera saídas versionadas. O Gradle só roda `--check`
(aviso se faltar Python localmente; erro na CI com `-PrequireSpriteCheck=true`).

Aplicativo **Game Luta Sprite GPT**, pacote `com.gamelutagpt`.
Preview técnico: `android/app/build/sprite-review/index.html`.

## Segundo personagem de validação — v0.55

O roster de produção usa `player_base` e `player_two`. O segundo pack reutiliza deliberadamente o mesmo perfil, os mesmos atlases e os mesmos movimentos para isolar o teste da arquitetura. O botão TROCA muda o renderer para `player_two`, e o HUD exibe o `displayName` do Character Pack ativo.

A suíte valida a troca real e executa L, M e H no segundo pack. O workflow da implementação v0.55 passou pipeline, testes Python, testes Java/Canvas, build, instalação e smoke test no emulador Android.

## Player Two com arte própria — v0.56

O segundo Character Pack deixou de reutilizar os atlas do personagem base. `player_two` agora possui perfil próprio (384×256, root 192/246, worldScale 1.0), Idle de 8 frames, Movement de 16 frames, Jab de 3, Medium Kick de 3 e Heavy Straight de 9. Idle e Movement usam masters revisados em `prepared-grid`; Jab, Medium e Heavy entram pelo `canonical-anatomy` do próprio personagem. O renderer e a lógica de troca não recebem condições específicas para o novo lutador.

Validação final da v0.56 deve rodar sobre os outputs `player_two_*` já materializados na branch, sem depender do auto-commit de regeneração do CI.

## Pack schema 2 — v0.63

Cada `character.json` agora é a fonte única do lutador (bloco `fighter`: vida, cor,
auto-combo, energia, Super e hurtbox) e declara os nove inputs com `totalMs`, janela
ativa e alcance. O NPC usa o próprio pack para dano e frame data, a câmera enquadra a
altura real da arte e um único cache de atlas é compartilhado na partida. Detalhes em
`docs/sprite-standard.md`.

## Empurrão e arquitetura — v0.65

Os corpos não se sobrepõem mais (caixa de empurrão no pack) e o `GameView` foi
dividido em sistemas menores (IA, regras de combate, comandos, câmera, controles,
HUD, cenário e efeitos), a maioria testável sem Android. Tabela em
`docs/sprite-standard.md`.

## 2H com arte própria — v0.66

O lançador agachado (2H) do `player_base` usa a nova folha de 4 frames pelo motor de
sprites, com o gancho sincronizado à janela de acerto. Comportamento de lançador mantido.

## Harmonia dos sprites — v0.67

Auditoria automática (`tools/sprites/harmony.py` + teste) de alinhamento, contato com o
chão, picos em golpes e escala. Corrigidos: tamanho e chão das reações do lutador base,
alinhamento do movimento e do Heavy do Player Two, artefato magenta, respiração do idle
e garras vazadas do Brutamonte.

## jL com arte própria — v0.68

O soco fraco aéreo do `player_base` usa a nova folha de 4 frames, com escala pela cabeça
e registro alinhado ao pulo.

## Aéreos completos — v0.69

jM e jH do `player_base` com arte própria, alinhados ao pulo e ao jL. Novo modo de
separação por componente para folhas em que as poses se sobrepõem na horizontal.

## Defesas e queda — v0.70

Defesa em pé, agachado e **no ar** (regra nova: segurar para trás pulando) e sequência de
queda do `player_base` com arte própria, todas na mesma escala do corpo.

## Dano e levantar — v0.71

Dano em pé, agachado e no ar (frame escolhido pela física do lançamento) e o levantar do
`player_base` com arte própria; o levantar começa espelhado para sair do mesmo lado em que
o lutador caiu. O atlas ampliado `player_base_missing` foi removido.

## Motor de Dano e Combos V2 — v0.72

O combate saiu do `GameView` para um motor Java puro em passo fixo de 60 fps: buffer de
input, máquina de estados (startup/active/recovery), cancelamentos declarados por golpe
(hit/block/whiff), hitstun/blockstun/hitstop reais, sessão de combo com escala de dano,
hitstun decay, juggle points e pushback. O frame data vive nos Character Packs (schema 3)
e é validado no build e no carregamento. Botão DEBUG mostra caixas, estados e a tabela de
vantagem de cada golpe. Detalhes, rotas e critérios de aceite em `docs/combat-engine.md`.

## Botões de teste de vida — v0.73

Abaixo de DEBUG: **VIDA P1** enche a vida dos dois lutadores do time (um KO volta a
lutar) e **VIDA CPU** enche a do oponente, sem precisar reiniciar o app. Embaixo, **DUPLA**
abre a **tela de seleção** (que também aparece ao abrir o jogo): toque em 2 personagens, o
1º começa lutando e o 2º entra no TAG; tocar de novo desmarca; **LUTAR** começa um round novo
com a intro, e o CPU de treino vira o lutador da frente da dupla, pálido. Quem aparece na
tela vem da lista `selectable` do `characters/roster.json`.

## Recuo mais lento — v0.74

Andar para trás (220) é mais lento que andar para frente (300), no chão e no ar, para que
recuar não seja uma fuga igual ao avanço do adversário.

## Ultra "Página Final" — v0.75

**↓ + SUPER** com 3 barras. A regra fica no motor: ativação que congela o oponente
(como o Super), investida e confirmação. A investida pode ser defendida (blockstun e
recuperação punível) e erra se o oponente estiver longe. Quando acerta, o motor espera e
o `GameView` toca a cinemática: uma página de mangá montada em 5 painéis com a arte do
personagem. Tocar quando o anel fecha dá bônus no golpe final (PERFEITO +25%, BOM +10%).
O dano entra pelo motor com a escala da sessão de combo e, no fim, o oponente é arremessado
e cai derrubado.

- `android/ultra-core/`: motor da cinemática em Java puro (roda no Android e no PC), com
  testes em `gradle :ultra-core:test`.
- `android/pc-preview/`: visualizador no PC, `gradle :pc-preview:run`.
- `android/app/src/main/assets/ultras/<id do personagem>/`: `ultra.json` (nome, cores,
  dano, onomatopeias), as 5 imagens e os sons opcionais.
- `tools/ultra/`: prompts fixos e o script que prepara a arte gerada por IA. O guia
  completo está em [tools/ultra/README.md](tools/ultra/README.md).

## Cenário com falso 3D — v0.76

O fundo virou **camadas com profundidade** projetadas pela câmera da luta: o que está longe
anda e cresce menos, o piso é desenhado em perspectiva ("line scroll") e luzes, fogo,
água, neblina, pétalas em 3D e o reflexo dos lutadores no chão molhado são animados pelo
jogo. A luta continua 2D. Primeiro cenário: **Templo da Lua**, com arte provisória
esperando a arte do GPT.

- `android/stage-core/`: motor de cenário em Java puro, com testes (`gradle :stage-core:test`).
- `android/render-core/`: base de desenho compartilhada pelo cenário e pelo ultra (é o que
  cada plataforma implementa no port para PC).
- `android/app/src/main/assets/stages/templo_lua/`: `stage.json` e as camadas.
- Visualizador no PC com câmera de luta simulada: `gradle :pc-preview:runStage --args="app/src/main/assets/stages/templo_lua"`.
- O jogo agora desenha pela GPU no Android 8+ (`lockHardwareCanvas`), com reserva por software.
- Como criar um cenário e os prompts das camadas: [tools/stage/README.md](tools/stage/README.md).
  Como funciona: `docs/stage-engine.md`. Cinemática do ultra por dentro: `docs/ultra-pagina-final.md`.

## Raio final do ultra — v0.77

Quando a página do ultra quebra, a luta volta com o personagem disparando o **raio amarelo**
do painel final, no estilo Marvel vs Capcom: o feixe atravessa a tela, acerta 20 vezes
seguidas com o contador de hits subindo e o dano entrando a cada acerto, e termina numa
explosão que arremessa o oponente. O bônus do toque no tempo certo vale também para o raio.

- Regra no motor (`CombatEngine`, fase `ULTRA_BEAM`) e desenho em Java puro (`RaioFinal`, no
  `ultra-core`), com a arte gerada no GPT em `assets/ultras/player_base/raio_*.png`.
- Prompts e o script que prepara as imagens (fundo preto vira transparência):
  [tools/ultra/README.md](tools/ultra/README.md#raio-final). Por dentro: `docs/ultra-pagina-final.md`.
- Ver no PC: `gradle :pc-preview:runRaio`.

## Raio estilo Kamehameha — v0.78

O raio final do ultra ganhou carga e animação de verdade, como um Kamehameha de Marvel vs
Capcom: o personagem tem uma **folha de pose própria** (carga com as mãos na cintura,
disparo, sustentação com a roupa batendo no vento e recuperação), a **aura** em chamas
acende atrás dele, o **vento** gira para dentro da esfera que cresce nas mãos e a
**poeira** levanta do chão; no "HA!" vem a onda de choque, a câmera abre, o raio ondula
com anéis correndo por ele e vento nas bordas, o recuo arrasta o pé soltando poeira e
nuvens rolam embaixo do feixe. Toda a arte é do GPT.

- Pose do personagem pelo pipeline de sprites (`specialAnimations.ULTRA`), com
  `tools/sprites/separar_folha.py` para folhas desenhadas em grade apertada.
- Efeitos preparados por `tools/ultra/preparar_raio.py --aura --vento --poeira`.
- Prompts e passo a passo: [tools/ultra/README.md](tools/ultra/README.md#raio-final).

## Time no motor: Assist, Assist → Tag e barra do time — v0.79

Primeiro passo do roadmap de mecânicas: a equipe passa a participar do combate.

- **TAG** chama o parceiro como **assist**: ele entra atrás de você, faz o golpe dele (o
  Lutador base lança o projétil; o Lutador Teste 2 dá o soco forte) e sai. Recarga de 4 s.
- **TAG de novo** enquanto o assist está em campo: **Assist → Tag**. O assist fica como o
  novo personagem e o anterior sai, sem interromper o combo.
- **↓ + TAG**: a troca direta de antes.
- **Barra do time**: uma barra só para a dupla; o que o assist ganha vai para ela e ela
  acompanha quem está em campo.
- A regra do time saiu do `GameView` e foi para o motor (`TeamSystem`), em frames e
  determinística, base para rollback e para as próximas mecânicas de equipe (guard
  cancel, DHC). O golpe do assist é declarado no pack (`fighter.assist.move`).
- Detalhes: `docs/combat-engine.md` (seção Time).

## Agarrão e tech — v0.80

- **L + M** agarra: não dá para bloquear, então quem só defende tem que reagir. Na tela, o
  **ponto "L+M" entre os botões L e M** aperta os dois com um dedo só (ou use dois dedos).
- Quem é agarrado tem uma janela curta para apertar **L + M** e fazer o **tech**: ninguém
  toma dano e os dois se afastam.
- Agarrão que erra deixa o atacante aberto; durante e logo depois de hitstun/blockstun não
  dá para ser agarrado (agarrão não vira combo).
- A CPU também agarra quando está colada e faz tech em parte dos agarrões.
- Regras e tempos: `docs/combat-engine.md` (seção Agarrão e tech).

## Defesa ativa: pushblock e Guard Cancel Tag — v0.81

- **M + H bloqueando** (ou o ponto **"M+H"** entre os botões M e H): **pushblock**. Empurra
  o atacante para longe e encurta o bloqueio, por 1/4 de barra. Fora do bloqueio é o H.
- **TAG bloqueando** (no chão, assist pronto): **Guard Cancel Tag**. O parceiro entra no
  seu lugar atacando, invulnerável, e o oponente congela no clarão. Custa 1 barra; o
  botão mostra "CANCEL" quando dá.
- Regras e tempos: `docs/combat-engine.md` (seção Defesa ativa).

## Air dash — v0.82

- **→ →** no ar: **air dash** (mantém a altura); **← ←**: **back air dash**. Um por pulo.
- Dá para soltar golpe aéreo no meio do dash: o impulso continua.
- Detalhes: `docs/combat-engine.md` (seção Air dash).

## Wall bounce e ground bounce — v0.83

- **H dentro de um combo** joga o oponente na parede (borda da tela ou da arena); ele
  quica de volta e dá para continuar o combo. O H solto no neutro continua normal.
- **↓ + jH num oponente no ar** crava ele no chão e ele quica para cima (no p01; o jH reto
  joga na parede).
- Um de cada por combo; depois de quicar, se ninguém continuar, ele cai derrubado.
- Rota de exemplo: L → M → H (parede) → dash → M → 2H → super pulo → jL → jM → ↓+jH (chão) → Super.
- Detalhes: `docs/combat-engine.md` (seção Wall bounce e ground bounce).

**Arte que falta** (pushblock, air dash, batida na parede, o Lutador Teste 2 e efeitos), com
os prompts: [docs/sprites-pendentes.md](docs/sprites-pendentes.md). O p01 é feito em vídeo:
o que falta dele está em [docs/personagens/p01.md](docs/personagens/p01.md).

## Air tech e opções de levantar — v0.84

- **Caindo depois de um combo**: aperte **L/M/H** para o **air tech** (com ← foge para trás,
  com → vai para a frente, sem direção dá um pulinho). Sem apertar, você cai solto um
  instante e ainda pode apanhar.
- **No chão**: segure **↑** para levantar rápido, **← / →** para rolar (o → passa por baixo do
  oponente) ou **↓** para levantar atrasado.
- A CPU também usa essas opções.
- Detalhes: `docs/combat-engine.md` (seção Air tech e opções de levantar).

## DHC (Team Super) e vida recuperável — v0.85

- **TAG durante o seu Super** (depois que ele disparou): o parceiro entra com o **Super
  dele** e o combo continua. Custa mais 1 barra; o botão mostra **DHC** quando dá.
- **Vida vermelha**: parte do dano que você toma fica vermelha na barra e **volta aos
  poucos enquanto o personagem está fora de campo**. Trocar um personagem machucado agora
  vale a pena.
- Detalhes: `docs/combat-engine.md` (seção DHC e vida recuperável).

## Demos e CPU de treino — v0.86

- O **CPU agora usa o nosso personagem** (mesmo corpo e golpes), desenhado **pálido e
  azulado** para não confundir com o jogador.
- *(Os botões DEMO saíram da tela de luta; no lugar deles fica **◀ SELEÇÃO**, que volta à
  escolha da dupla. As demos continuam no código e nos testes.)* **Botões DEMO 80…85 (e 87)** no topo da tela: cada um faz os dois personagens demonstrarem
  sozinhos o que entrou naquela versão, passo a passo, com legenda (✓ quando o passo
  mostrou o que devia). Toque de novo no mesmo botão para parar; no fim a luta volta
  como estava (posições, vida, barra).
  - **80** agarrão, tech, agarrão no vazio punido · **81** pushblock, Guard Cancel ·
    **82** air dash para frente e para trás · **83** wall bounce, ground bounce ·
    **84** air tech, levantar rápido, rolamentos · **85** DHC, vida vermelha.
- Detalhes: `docs/combat-engine.md` (seção Demos).

## Overdrive — v0.87

- Botão **OD** (à esquerda de SUPER e TAG) ou **SUPER + TAG juntos**: **1 vez por round**,
  dura **8 segundos** para o time todo.
- Ao ligar, os dois congelam por um instante (o hitstun do CPU espera): dá para ligar **no
  meio do combo**, cancelando o próprio golpe, e continuar.
- Durante o Overdrive: **25% mais rápido** (andar, dash, air dash), **50% mais barra**,
  **cancels livres** (todo golpe que acertou ou foi defendido cancela em qualquer outro,
  ex.: H > M > H) e a **vida vermelha volta até para quem está lutando**, mais rápido.
- O botão mostra o tempo restante; depois fica "USADO". Os botões **VIDA P1 / VIDA CPU**
  também recarregam o Overdrive (novo round no treino).
- **DEMO 87** mostra tudo isso. A CPU ainda não usa o Overdrive.

**Build corrigido do H:** o commit automático de correção de transparência
altera `player_base_heavy_straight.png` depois que a compilação inicial inicia.
Este commit de sincronização garante que o próximo APK inclua a arte atualizada.
O resultado final deve ser conferido visualmente no aparelho; a varredura corrige
apenas regiões internas transparentes detectáveis e não garante cobrir todos os
defeitos de desenho.
