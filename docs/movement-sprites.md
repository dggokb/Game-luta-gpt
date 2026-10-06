# Atlas de movimento — `player_base`

Estado atual (v0.63). A especificação original de geração está no fim, como histórico.

## Fonte e saída

| Item | Valor |
| --- | --- |
| Fonte revisada | `art/sprites/source/player_base_movement_normalized.png` |
| Clip | `tools/sprites/clips/player_base_movement.json` (`prepared-grid`, raiz `authored`) |
| Célula canônica de autoria | 256×256, 4 colunas × 4 linhas, raiz 128/238, `worldScale` 1.0 |
| Atlas empacotado no APK | `drawable-nodpi/player_base_movement.png`, geometria em `GeneratedSpriteLayouts.MOVEMENT_*` |

A fonte é desenhada na célula canônica; o pipeline recorta a borda transparente comum
a todas as células (mantendo 8 px de margem e a raiz) e grava o recorte em
`report['packed']`. Nenhum pixel muda de posição relativa à raiz. Não edite o PNG de
`drawable-nodpi`: edite a fonte e rode `build_characters.py --write`.

## Mapa de frames (índices da grade)

| Frames | Estado no `character.json` | Avanço |
| --- | --- | --- |
| 0–3 | `WALK_FORWARD` | 36 unidades de deslocamento por frame |
| 4–7 | `WALK_BACK` (olhando para a frente) | 32 unidades por frame |
| 8–9 | `CROUCH` (entrada e guarda baixa) | 75 / 100 ms |
| 8 | `RISE` | 75 ms |
| 10 | `JUMP` (subida) | física: `velocityY < -35` |
| 11 | `FALL` (descida) | física |
| 12–13 | `DASH` | 100 ms cada, em loop |
| 14 | `BACKDASH` | 200 ms |
| 15 | `LAND` | 100 ms |

Caminhada avança pela distância percorrida, não pelo tempo; colisão com a borda não
gera passos no lugar. Agachamentos mantêm a mesma escala do corpo em pé.

## Histórico — especificação de geração (v0.51)

Gerado com imagem por IA, fundo transparente, usando as folhas originais de caminhada e
idle como referência de identidade. Prompt usado: lutador adulto (colete preto sem
mangas com debrum vermelho, camiseta branca, calça preta larga com detalhes vermelhos,
luvas sem dedos, tênis preto/vermelho/branco, cabelo preto desgrenhado), contorno anime
e cel shading, 4×4 poses de corpo inteiro olhando para a direita, sem texto, grade, chão
ou sombras, na ordem da tabela acima.

A imagem gerada original (1254×1254, espaçamento irregular) era recortada em runtime por
retângulos explícitos. Esse modo foi substituído pelo master normalizado em grade; o
atlas antigo (`movement_astra.png`) foi removido na v0.63.
