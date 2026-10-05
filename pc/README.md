# Game Luta Sprite GPT — PC

Port desktop da branch mobile game-luta-sprite-gpt.

A versão PC reutiliza o mesmo GameView, física, combate, câmera, Character Pack Engine
e atlases da versão Android. A camada pc/src/main/java/android/ é uma compatibilidade
Java2D para as poucas APIs Android usadas pelo jogo.

## Controles
- Movimento: WASD ou setas
- Ataque leve: J
- Ataque médio: K
- Ataque forte: L
- Auto combo: U
- Troca: I
- Super: O
- IA: P
- Mouse também funciona nos controles desenhados.

Dash, defesa, agachar, pulo, super pulo e especiais continuam usando o mesmo buffer
de direções da versão mobile.

## Build
gradle -p pc clean jar

O workflow Build Game Luta PC gera um app-image Windows com runtime Java embutido.
