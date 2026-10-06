# Game Luta Sprite GPT

Branch `game-luta-sprite-gpt`. Versão `0.72-sprite-gpt-pack-v2` (fonte única: `versionName` em `android/app/build.gradle`).

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
