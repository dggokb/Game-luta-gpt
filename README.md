# Game Luta Sprite GPT

Branch `game-luta-sprite-gpt`. Versão `0.54-sprite-gpt-character-packs-anatomy`.

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

Aplicativo **Game Luta Sprite GPT**, pacote `com.gamelutagpt`.
Preview técnico: `android/app/build/sprite-review/index.html`.
