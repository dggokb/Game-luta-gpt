# Game Luta Sprite GPT

Branch `game-luta-sprite-gpt`. Versão `0.64-sprite-gpt-pack-v2` (fonte única: `versionName` em `android/app/build.gradle`).

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
