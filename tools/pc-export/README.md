# Motor de exportacao PC

Este diretorio contem o motor reutilizavel que transforma uma branch mobile compativel
do Game Luta Sprite GPT em um pacote Windows portatil.

## Como funciona

1. Define a branch/commit mobile em `export-request.json`.
2. O workflow `Export Mobile Branch to PC` faz checkout dessa versao em `mobile-source/`.
3. O motor valida os Character Packs e os testes de sprites.
4. O Gradle compila o mesmo `GameView` mobile usando a camada de compatibilidade Java2D/Swing.
5. O `jpackage` gera `GameLutaSpriteGPT.exe` com runtime Java embutido.
6. O resultado e publicado como artefato ZIP do GitHub Actions.

A branch mobile nao precisa receber arquivos de PC e nao precisa ser mesclada manualmente
na branch do motor.

## Uso pelo ChatGPT

Quando o usuario disser **"faz a exportacao"**, usar a branch mobile indicada pelo contexto
(padrao: `game-luta-sprite-gpt`), obter o SHA atual, atualizar `export-request.json`
nesta branch do motor e acompanhar o workflow ate o artefato ser concluido.

Depois, baixar o artefato e entregar o ZIP ao usuario.

## Contrato de compatibilidade

A fonte mobile precisa conter:

- `android/app/src/main/java/com/gamelutagpt/GameView.java`
- `android/app/src/main/res/drawable-nodpi/`
- `tools/sprites/build_characters.py`
- `tools/sprites/tests/`

Se futuras mudancas Android usarem APIs ainda nao implementadas na camada `pc/src/main/java/android/`,
a compilacao falhara e o motor devera ser atualizado antes da entrega.
