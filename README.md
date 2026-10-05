# Game Luta Sprite GPT

Branch `game-luta-sprite-gpt`. Esta versão incorpora o Character Pack Engine
desenvolvido na Astra v0.53 preservando o aplicativo GPT.

Cada personagem é definido em `characters/<id>/character.json`; o time em
`characters/roster.json`. Animações e golpes L/M/H em pé são declarativos,
o renderer é genérico, e o build valida os pacotes antes de gerar o APK.

Build:

```bash
python3 -m pip install -r tools/sprites/requirements.txt
python3 tools/sprites/build_characters.py --write
python3 -m unittest discover -s tools/sprites/tests -v
python3 tools/sprites/build_characters.py --check
cd android
gradle testDebugUnitTest assembleDebug --stacktrace
```

Aplicativo **Game Luta Sprite GPT**, pacote `com.gamelutagpt`.
A próxima etapa desta branch é a validação anatômica canônica dos sprites.
