# Prompts do Flick (MiniMax H3 Fast) para o elenco

Os prompts que deram certo na p15 no Flick ([p15.md](p15.md), [flick-h3.md](flick-h3.md)),
refeitos para p01–p14 e p16 (a p15 já tem os dela). Cada um troca só o que muda de personagem: quem é, pronome, a lista do
que não pode mudar e o golpe H e o 2M da ficha.

| Clipe | Na p15 | Aqui |
|---|---|---|
| andar para frente | uma frase em português, de primeira | a mesma frase |
| andar para trás | uma frase em português | a mesma frase |
| agachar (dá também o levantar) | prompt do Omni com a última faixa até 5 s, de primeira | o mesmo; "fists up" virou "arms kept in his/her guard", porque nem todos guardam com os punhos |
| H | chute alto com a perna de trás, nomeada pelo lado do quadro | o mesmo molde, com o H de cada ficha (chute, gancho, chicote, martelo, pisão, garras) |
| 2M | rasteira girando com a mão no chão, agachado nas duas pontas | girando onde a ficha pede giro; rasteira baixa sem giro nos outros (p04, p07, p09, p11, p12) |
| idle | aprovado, mas o texto não ficou registrado | pela fórmula: dois ciclos de respiração, mesma imagem nas duas pontas |

## Configuração

- **MiniMax H3 Fast** (20 créditos); **H3 Max** (40) só se o Fast errar o golpe.
- **5 s, 768p.** Sai 1344×768, 24 fps, ~124 quadros.
- **A mesma imagem nos dois campos** (início e fim) em tudo que é no lugar. Andar: as duas da
  tabela.
- Um vídeo por vez, na ordem da tabela de cada personagem: o 2M precisa do
  `inicio_agachado_h3`, que sai do vídeo de agachar.

## Antes: imagens iniciais `_h3`

Ninguém além da p15 e da p16 tem ainda as imagens no tamanho do H3 (corpo com 77% da altura de
1344×768). Montar da Base (`art/keys/pNN/idle_base.png`) com o `inicio_da_base.py`, conferindo
a prévia sobre o verde (cabelo, vãos entre braço e corpo):

```
python3 tools/sprites/inicio_da_base.py pNN art/keys/pNN/idle_base.png --sufixo _h3 --previa pasta
python3 tools/sprites/inicio_da_base.py pNN art/keys/pNN/idle_base.png --sufixo _h3
```

- `--sufixo _h3` porque todos já têm vídeos na escala antiga (`inicio_*.png`).
- **`--altura`**: o padrão 0,77 (o da p15) serve para todos. Como cada chave nova tem a
  própria escala no `tamanho.json`, o tamanho no jogo não muda, e maior no vídeo é mais detalhe.
  A exceção é o H do p08: o chicote esticado passa da borda a 0,77. Se o vídeo cortar a ponta,
  fazer uma imagem central menor só para o H (com a escala dela no `tamanho.json`).
- **`tamanho.json`**: cada chave nova recebe a escala antiga × altura da guarda antiga / altura
  nova, como na p15. Detalhes em [flick-h3.md](flick-h3.md#imagens-iniciais-tamanho-do-h3).
- Cabelo de mechas (p02, p05, p06, p08, p10): `--bolso` na cabeça, `--desfranja`, `--halo`.

## Regras que vêm da p15

- O membro do golpe vai nomeado **pelo lado do quadro** ("the leg farther back, on the LEFT
  side of the frame"). Foi o que fez o H3 acertar a perna de trás de primeira.
- **Uma ação contada**: "ONE ...", "One kick only". Sem contar, sai rajada.
- **Sem nome de golpe de arte marcial** no texto. "Mawashi-geri", "gyaku-zuki" e "Shotokan"
  puxaram combinação e postura errada.
- **Andar só com a frase curta.** O andar detalhado saiu pulando no H3.
- Conferir na conversão:
  - pé ou chicote encostando na borda (o 2M da p15 desceu até y 767);
  - o último quadro batendo com o primeiro.

## p01 — irmão calmo

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — chute alto (roundhouse, perna de trás) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira girando | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] He holds his fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; his hair and clothes move softly with the breathing.
His feet stay planted and never slide; he never leaves the guard and never turns toward the camera. He never holds any object. Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] He holds his fighting guard exactly as in the start frame.
[0.5-1s] He sinks into a deep fighting-game crouch: a low squat with his hips dropping all the way down to the level of his knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, his arms kept in his guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] He holds the deep squat, breathing calmly: only his shoulders rise and fall slightly, his hips stay low and his feet stay planted.
[3-3.5s] He rises smoothly back to his standing guard.
[3.5-5s] He holds the same guard as the end frame and stays still.
His feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. He does not punch, kick or sit down. He never holds any object. Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] He holds his fighting guard exactly as in the start frame.
[1-2s] He pivots on his front foot and throws ONE high roundhouse kick with his rear leg (the leg farther back, on the LEFT side of the frame): the knee rises and chambers, then the leg snaps around horizontally at head height with the foot fully extended toward the RIGHT. He retracts the knee and sets the foot back down exactly where it started. His hands stay up in his guard.
[2-5s] He holds the same guard as the end frame, breathing calmly, feet planted.
One kick only. After the kick the same leg is in front again, as in the start frame. His whole body stays inside the frame. He never holds any object. Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the martial artist from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] He holds his deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.8s] He drops even lower and plants one hand flat on the ground.
[0.8-1.1s] Pivoting on that hand, he spins his whole body around: for an instant his BACK is toward the camera, and one leg swings out straight along the floor behind him.
[1.1-1.4s] Still spinning, the extended leg sweeps around low along the floor to the front, toward the RIGHT, at ankle height, like the classic spinning crouching sweep of 1990s anime fighting games.
[1.4-1.8s] He finishes the full turn, pulls the leg back under him and lifts his hand from the ground.
[1.8-5s] He is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE full 360-degree spin and ONE sweep. The sweeping leg stays straight and low along the floor the whole time, never above knee height. He stays low and never stands up. He never holds any object. Keep his face, brown hair, vest, belt, gloves, pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p02 — kung fu ágil

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virada para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — chute alto (circular, perna de trás) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira girando | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the kung fu fighter from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] She holds her fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; her hair and clothes move softly with the breathing.
Her feet stay planted and never slide; she never leaves the guard and never turns toward the camera. She never holds any object. Keep her face, silver-lavender hair, headband, top, orange sash, gloves, pants, sneakers, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the kung fu fighter from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] She holds her fighting guard exactly as in the start frame.
[0.5-1s] She sinks into a deep fighting-game crouch: a low squat with her hips dropping all the way down to the level of her knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, her arms kept in her guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] She holds the deep squat, breathing calmly: only her shoulders rise and fall slightly, her hips stay low and her feet stay planted.
[3-3.5s] She rises smoothly back to her standing guard.
[3.5-5s] She holds the same guard as the end frame and stays still.
Her feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. She does not punch, kick or sit down. She never holds any object. Keep her face, silver-lavender hair, headband, top, orange sash, gloves, pants, sneakers, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the kung fu fighter from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] She holds her fighting guard exactly as in the start frame.
[1-2s] She pivots on her front foot and throws ONE high roundhouse kick with her rear leg (the leg farther back, on the LEFT side of the frame): the knee rises and chambers, then the leg snaps around horizontally at head height with the foot fully extended toward the RIGHT. She retracts the knee and sets the foot back down exactly where it started. Her hands stay up in her guard.
[2-5s] She holds the same guard as the end frame, breathing calmly, feet planted.
One kick only. After the kick the same leg is in front again, as in the start frame. Her whole body stays inside the frame. She never holds any object. Keep her face, silver-lavender hair, headband, top, orange sash, gloves, pants, sneakers, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the kung fu fighter from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] She holds her deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.8s] She drops even lower and plants one hand flat on the ground.
[0.8-1.1s] Pivoting on that hand, she spins her whole body around: for an instant her BACK is toward the camera, and one leg swings out straight along the floor behind her.
[1.1-1.4s] Still spinning, the extended leg sweeps around low along the floor to the front, toward the RIGHT, at ankle height, like the classic spinning crouching sweep of 1990s anime fighting games.
[1.4-1.8s] She finishes the full turn, pulls the leg back under her and lifts her hand from the ground.
[1.8-5s] She is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE full 360-degree spin and ONE sweep. The sweeping leg stays straight and low along the floor the whole time, never above knee height. She stays low and never stands up. She never holds any object. Keep her face, silver-lavender hair, headband, top, orange sash, gloves, pants, sneakers, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p03 — irmão energético

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — chute giratório alto (spinning heel kick) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira girando | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the street fighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] He holds his fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; his hair and clothes move softly with the breathing.
His feet stay planted and never slide; he never leaves the guard and never turns toward the camera. He never holds any object. Keep his face, spiky hair, hooded vest, red straps, tank top, gloves, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the street fighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] He holds his fighting guard exactly as in the start frame.
[0.5-1s] He sinks into a deep fighting-game crouch: a low squat with his hips dropping all the way down to the level of his knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, his arms kept in his guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] He holds the deep squat, breathing calmly: only his shoulders rise and fall slightly, his hips stay low and his feet stay planted.
[3-3.5s] He rises smoothly back to his standing guard.
[3.5-5s] He holds the same guard as the end frame and stays still.
His feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. He does not punch, kick or sit down. He never holds any object. Keep his face, spiky hair, hooded vest, red straps, tank top, gloves, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the street fighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] He holds his fighting guard exactly as in the start frame.
[1-2s] He spins his whole body around on his front foot: for an instant his BACK is toward the camera, and his rear leg swings around straight at head height, the heel leading, striking toward the RIGHT. He completes the full turn and sets the foot back down exactly where it started, facing RIGHT again in the same guard.
[2-5s] He holds the same guard as the end frame, breathing calmly, feet planted.
ONE full 360-degree spin and ONE kick only. After the kick the same leg is in front again, as in the start frame. His whole body stays inside the frame. He never holds any object. Keep his face, spiky hair, hooded vest, red straps, tank top, gloves, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the street fighter from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] He holds his deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.8s] He drops even lower and plants one hand flat on the ground.
[0.8-1.1s] Pivoting on that hand, he spins his whole body around fast: for an instant his BACK is toward the camera, and one leg swings out straight along the floor behind him.
[1.1-1.4s] Still spinning, the extended leg sweeps around low along the floor to the front, toward the RIGHT, at ankle height, like the classic spinning crouching sweep of 1990s anime fighting games.
[1.4-1.8s] He finishes the full turn, pulls the leg back under him and lifts his hand from the ground.
[1.8-5s] He is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE full 360-degree spin and ONE sweep. The sweeping leg stays straight and low along the floor the whole time, never above knee height. He stays low and never stands up. He never holds any object. Keep his face, spiky hair, hooded vest, red straps, tank top, gloves, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p04 — lutador de rua

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — gancho forte (braço de trás) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira baixa, sem giro | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the street brawler from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] He holds his fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; his hair and clothes move softly with the breathing.
His feet stay planted and never slide; he never leaves the guard and never turns toward the camera. He never holds any object. Keep his face, silver hair, tank top, bandages, gloves, red sash, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the street brawler from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] He holds his fighting guard exactly as in the start frame.
[0.5-1s] He sinks into a deep fighting-game crouch: a low squat with his hips dropping all the way down to the level of his knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, his arms kept in his guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] He holds the deep squat, breathing calmly: only his shoulders rise and fall slightly, his hips stay low and his feet stay planted.
[3-3.5s] He rises smoothly back to his standing guard.
[3.5-5s] He holds the same guard as the end frame and stays still.
His feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. He does not punch, kick or sit down. He never holds any object. Keep his face, silver hair, tank top, bandages, gloves, red sash, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the street brawler from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] He holds his fighting guard exactly as in the start frame.
[1-2s] He turns his hips and shoulders hard and throws ONE wide, heavy hook with his rear fist (the fist held farther back, toward the LEFT side of the frame): the arm swings around horizontally at head height in a wide arc, elbow bent, his whole body turning into the punch, the fist ending in front of him toward the RIGHT. He pulls the fist back to his chin, back in the same guard. His front fist stays up guarding his face and never punches.
[2-5s] He holds the same guard as the end frame, breathing calmly, feet planted.
Exactly one punch in the whole video. He does not step: the same leg stays in front, both feet stay planted and never slide, only the rear heel turns with the hips. His whole body stays inside the frame. He never holds any object. Keep his face, silver hair, tank top, bandages, gloves, red sash, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the street brawler from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] He holds his deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.9s] Staying low, he shifts his weight onto his rear leg.
[0.9-1.3s] He extends his front leg straight out along the floor toward the RIGHT in ONE low, sweeping kick at ankle height, the foot sliding just above the floor.
[1.3-1.8s] He pulls the leg back under him into the crouch.
[1.8-5s] He is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE sweep only, no spin. The sweeping leg stays low along the floor the whole time, never above knee height. He stays low and never stands up. He never holds any object. Keep his face, silver hair, tank top, bandages, gloves, red sash, cargo pants, sneakers, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p05 — ninja

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — chute giratório alto (perna de trás) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira girando | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the ninja from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] He holds his fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; his hair and clothes move softly with the breathing.
His feet stay planted and never slide; he never leaves the guard and never turns toward the camera. The katana stays sheathed the whole time. He never holds any object. Keep his mask, red eyes, spiky hair, scarf, top, bracers, gloves, sash, sheathed katana, pants, leg wraps, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the ninja from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] He holds his fighting guard exactly as in the start frame.
[0.5-1s] He sinks into a deep fighting-game crouch: a low squat with his hips dropping all the way down to the level of his knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, his arms kept in his guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] He holds the deep squat, breathing calmly: only his shoulders rise and fall slightly, his hips stay low and his feet stay planted.
[3-3.5s] He rises smoothly back to his standing guard.
[3.5-5s] He holds the same guard as the end frame and stays still.
His feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. He does not punch, kick or sit down. The katana stays sheathed the whole time. He never holds any object. Keep his mask, red eyes, spiky hair, scarf, top, bracers, gloves, sash, sheathed katana, pants, leg wraps, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the ninja from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] He holds his fighting guard exactly as in the start frame.
[1-2s] He spins his whole body around on his front foot: for an instant his BACK is toward the camera, and his rear leg swings around straight at head height, the heel leading, striking toward the RIGHT. He completes the full turn and sets the foot back down exactly where it started, facing RIGHT again in the same guard.
[2-5s] He holds the same guard as the end frame, breathing calmly, feet planted.
ONE full 360-degree spin and ONE kick only. After the kick the same leg is in front again, as in the start frame. His whole body stays inside the frame. The katana stays sheathed the whole time. He never holds any object. Keep his mask, red eyes, spiky hair, scarf, top, bracers, gloves, sash, sheathed katana, pants, leg wraps, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the ninja from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] He holds his deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.8s] He drops even lower and plants one hand flat on the ground.
[0.8-1.1s] Pivoting on that hand, he spins his whole body around fast: for an instant his BACK is toward the camera, and one leg swings out straight along the floor behind him.
[1.1-1.4s] Still spinning, the extended leg sweeps around low along the floor to the front, toward the RIGHT, at ankle height, like the classic spinning crouching sweep of 1990s anime fighting games.
[1.4-1.8s] He finishes the full turn, pulls the leg back under him and lifts his hand from the ground.
[1.8-5s] He is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE full 360-degree spin and ONE sweep. The sweeping leg stays straight and low along the floor the whole time, never above knee height. He stays low and never stands up. The katana stays sheathed the whole time. He never holds any object. Keep his mask, red eyes, spiky hair, scarf, top, bracers, gloves, sash, sheathed katana, pants, leg wraps, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p06 — sombra e ilusão

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre olhando para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — chute alto em arco (perna de trás) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira girando | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the masked fighter from the start frame, on a flat solid green background. They face RIGHT the whole time and stay in place; the camera stays fixed and never zooms.
[0-5s] They hold their fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; their hair and clothes move softly with the breathing.
Their feet stay planted and never slide; they never leave the guard and never turn toward the camera. They never hold any object. Keep their grey mask, purple hair, jacket, purple straps, gloves, cargo pants, sneakers, colors, line art and size exactly as in the frames; their size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the masked fighter from the start frame, on a flat solid green background. They face RIGHT the whole time and stay in place; the camera stays fixed and never zooms.
[0-0.5s] They hold their fighting guard exactly as in the start frame.
[0.5-1s] They sink into a deep fighting-game crouch: a low squat with their hips dropping all the way down to the level of their knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, their arms kept in their guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] They hold the deep squat, breathing calmly: only their shoulders rise and fall slightly, their hips stay low and their feet stay planted.
[3-3.5s] They rise smoothly back to their standing guard.
[3.5-5s] They hold the same guard as the end frame and stay still.
Their feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. They do not punch, kick or sit down. They never hold any object. Keep their grey mask, purple hair, jacket, purple straps, gloves, cargo pants, sneakers, colors, line art and size exactly as in the frames; their size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the masked fighter from the start frame, on a flat solid green background. They face RIGHT the whole time and stay in place; the camera stays fixed and never zooms.
[0-1s] They hold their fighting guard exactly as in the start frame.
[1-2s] They pivot on their front foot and throw ONE high arcing kick with their rear leg (the leg farther back, on the LEFT side of the frame): the straight leg swings up and over in a wide arc at head height, the foot passing toward the RIGHT. They bring the leg back down and set the foot exactly where it started. Their hands stay in their guard.
[2-5s] They hold the same guard as the end frame, breathing calmly, feet planted.
One kick only. After the kick the same leg is in front again, as in the start frame. Their whole body stays inside the frame. They never hold any object. Keep their grey mask, purple hair, jacket, purple straps, gloves, cargo pants, sneakers, colors, line art and size exactly as in the frames; their size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the masked fighter from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] They hold their deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.8s] They drop even lower and plant one hand flat on the ground.
[0.8-1.1s] Pivoting on that hand, they spin their whole body around: for an instant their BACK is toward the camera, and one leg swings out straight along the floor behind them.
[1.1-1.4s] Still spinning, the extended leg sweeps around low along the floor to the front, toward the RIGHT, at ankle height, like the classic spinning crouching sweep of 1990s anime fighting games.
[1.4-1.8s] They finish the full turn, pull the leg back under them and lift their hand from the ground.
[1.8-5s] They are back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE full 360-degree spin and ONE sweep. The sweeping leg stays straight and low along the floor the whole time, never above knee height. They stay low and never stand up. They never hold any object. Keep their grey mask, purple hair, jacket, purple straps, gloves, cargo pants, sneakers, colors, line art and size exactly as in the frames; their size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p07 — cavalheiro pesado

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — chute alto em gancho (sapato na frente, perna de trás) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira baixa, sem giro | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the gentleman fighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] He holds his fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; his hair and clothes move softly with the breathing.
His feet stay planted and never slide; he never leaves the guard and never turns toward the camera. The fedora stays on his head. He never holds any object. Keep his face, goatee, fedora, long coat, vest, tie, gloves, trousers, polished shoes, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the gentleman fighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] He holds his fighting guard exactly as in the start frame.
[0.5-1s] He sinks into a deep fighting-game crouch: a low squat with his hips dropping all the way down to the level of his knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, his arms kept in his guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] He holds the deep squat, breathing calmly: only his shoulders rise and fall slightly, his hips stay low and his feet stay planted.
[3-3.5s] He rises smoothly back to his standing guard.
[3.5-5s] He holds the same guard as the end frame and stays still.
His feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. He does not punch, kick or sit down. The fedora stays on his head. He never holds any object. Keep his face, goatee, fedora, long coat, vest, tie, gloves, trousers, polished shoes, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the gentleman fighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] He holds his fighting guard exactly as in the start frame.
[1-2s] He shifts his weight onto his front foot and throws ONE high hooking kick with his rear leg (the leg farther back, on the LEFT side of the frame): the knee rises, the leg extends toward the RIGHT at head height with the polished shoe leading, then the heel hooks back sharply. He retracts the knee and sets the foot back down exactly where it started; his long coat swings with the kick. His arms stay in his guard.
[2-5s] He holds the same guard as the end frame, breathing calmly, feet planted.
One kick only. After the kick the same leg is in front again, as in the start frame. His whole body stays inside the frame. The fedora stays on his head. He never holds any object. Keep his face, goatee, fedora, long coat, vest, tie, gloves, trousers, polished shoes, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the gentleman fighter from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] He holds his deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.9s] Staying low, he shifts his weight onto his rear leg.
[0.9-1.3s] He extends his front leg straight out along the floor toward the RIGHT in ONE low, heavy, sweeping kick at ankle height, the foot sliding just above the floor.
[1.3-1.8s] He pulls the leg back under him into the crouch.
[1.8-5s] He is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE sweep only, no spin. The sweeping leg stays low along the floor the whole time, never above knee height. He stays low and never stands up. The fedora stays on his head. He never holds any object. Keep his face, goatee, fedora, long coat, vest, tie, gloves, trousers, polished shoes, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p08 — chicote

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virada para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — chicotada horizontal | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira girando | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the whip fighter from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] She holds her fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; her hair and clothes move softly with the breathing.
Her feet stay planted and never slide; she never leaves the guard and never turns toward the camera. The whip stays in her gloved hand the whole time and stays in one piece. Keep her face, pink hair, rose, dress, opera gloves, stockings, heels and the braided crimson whip, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the whip fighter from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] She holds her fighting guard exactly as in the start frame.
[0.5-1s] She sinks into a deep fighting-game crouch: a low squat with her hips dropping all the way down to the level of her knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, her arms kept in her guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] She holds the deep squat, breathing calmly: only her shoulders rise and fall slightly, her hips stay low and her feet stay planted.
[3-3.5s] She rises smoothly back to her standing guard.
[3.5-5s] She holds the same guard as the end frame and stays still.
Her feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. She does not punch, kick or sit down. The whip stays in her gloved hand the whole time and stays in one piece. Keep her face, pink hair, rose, dress, opera gloves, stockings, heels and the braided crimson whip, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the whip fighter from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] She holds her fighting guard exactly as in the start frame.
[1-2s] She swings her whip arm and lashes ONE long, straight horizontal whip strike at chest height toward the RIGHT: the braided whip uncoils and stretches straight out toward the right edge of the frame, the brass tip snapping at the end. She pulls the whip back and coils it low in her gloved hand at her side, exactly as in the start frame.
[2-5s] She holds the same guard as the end frame, breathing calmly, feet planted.
One lash only. The whole whip stays inside the frame. Her feet stay planted and never slide. Her whole body stays inside the frame. The whip stays in her gloved hand the whole time and stays in one piece. Keep her face, pink hair, rose, dress, opera gloves, stockings, heels and the braided crimson whip, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the whip fighter from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] She holds her deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.8s] She drops even lower and plants one hand flat on the ground.
[0.8-1.1s] Pivoting on that hand, she spins her whole body around: for an instant her BACK is toward the camera, and one leg swings out straight along the floor behind her. Her dress flares out around her with the spin.
[1.1-1.4s] Still spinning, the extended leg sweeps around low along the floor to the front, toward the RIGHT, at ankle height, like the classic spinning crouching sweep of 1990s anime fighting games.
[1.4-1.8s] She finishes the full turn, pulls the leg back under her and lifts her hand from the ground.
[1.8-5s] She is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE full 360-degree spin and ONE sweep. The sweeping leg stays straight and low along the floor the whole time, never above knee height. She stays low and never stands up. The whip stays in her gloved hand the whole time and stays in one piece. Keep her face, pink hair, rose, dress, opera gloves, stockings, heels and the braided crimson whip, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p09 — bombeiro grappler

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — soco martelo (de cima para baixo) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira baixa, sem giro | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the firefighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] He holds his fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; his hair and clothes move softly with the breathing.
His feet stay planted and never slide; he never leaves the guard and never turns toward the camera. He never holds any object. Keep his face, orange hair, stubble, red vest, belt, gloves, cargo pants, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the firefighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] He holds his fighting guard exactly as in the start frame.
[0.5-1s] He sinks into a deep fighting-game crouch: a low squat with his hips dropping all the way down to the level of his knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, his arms kept in his guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] He holds the deep squat, breathing calmly: only his shoulders rise and fall slightly, his hips stay low and his feet stay planted.
[3-3.5s] He rises smoothly back to his standing guard.
[3.5-5s] He holds the same guard as the end frame and stays still.
His feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. He does not punch, kick or sit down. He never holds any object. Keep his face, orange hair, stubble, red vest, belt, gloves, cargo pants, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the firefighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] He holds his fighting guard exactly as in the start frame.
[1-1.5s] He raises his rear fist (the fist held farther back, toward the LEFT side of the frame) high above his head, elbow bent, torso rising tall, and holds that raised pose for a split second.
[1.5-2s] He brings it DOWN in ONE heavy hammer-fist strike in a vertical arc in front of him, his whole body leaning forward into it, ending with the fist at waist height in front of his body.
[2-2.5s] He pulls back and returns to the guard.
[2.5-5s] He holds the same guard as the end frame, breathing calmly, feet planted.
One strike only. The fist moves DOWN, never straight forward. He does not step: the same leg stays in front and both feet stay planted and never slide. His whole body stays inside the frame. He never holds any object. Keep his face, orange hair, stubble, red vest, belt, gloves, cargo pants, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the firefighter from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] He holds his deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.9s] Staying low, he shifts his weight onto his rear leg.
[0.9-1.3s] He extends his front leg straight out along the floor toward the RIGHT in ONE low, heavy, sweeping kick at ankle height, the heavy boot sliding just above the floor.
[1.3-1.8s] He pulls the leg back under him into the crouch.
[1.8-5s] He is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE sweep only, no spin. The sweeping leg stays low along the floor the whole time, never above knee height. He stays low and never stands up. He never holds any object. Keep his face, orange hair, stubble, red vest, belt, gloves, cargo pants, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p10 — bombeira ágil

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virada para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — chute giratório na altura da cabeça | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira girando | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the firefighter from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] She holds her fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; her hair and clothes move softly with the breathing.
Her feet stay planted and never slide; she never leaves the guard and never turns toward the camera. She never holds any object. Keep her face, blonde ponytail, red vest, belt, gloves, cargo pants, boots, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the firefighter from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] She holds her fighting guard exactly as in the start frame.
[0.5-1s] She sinks into a deep fighting-game crouch: a low squat with her hips dropping all the way down to the level of her knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, her arms kept in her guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] She holds the deep squat, breathing calmly: only her shoulders rise and fall slightly, her hips stay low and her feet stay planted.
[3-3.5s] She rises smoothly back to her standing guard.
[3.5-5s] She holds the same guard as the end frame and stays still.
Her feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. She does not punch, kick or sit down. She never holds any object. Keep her face, blonde ponytail, red vest, belt, gloves, cargo pants, boots, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the firefighter from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] She holds her fighting guard exactly as in the start frame.
[1-2s] She spins her whole body around on her front foot: for an instant her BACK is toward the camera, and her rear leg swings around straight at head height, the heel leading, striking toward the RIGHT. She completes the full turn and sets the foot back down exactly where it started, facing RIGHT again in the same guard.
[2-5s] She holds the same guard as the end frame, breathing calmly, feet planted.
ONE full 360-degree spin and ONE kick only. After the kick the same leg is in front again, as in the start frame. Her whole body stays inside the frame. She never holds any object. Keep her face, blonde ponytail, red vest, belt, gloves, cargo pants, boots, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the firefighter from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] She holds her deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.8s] She drops even lower and plants one hand flat on the ground.
[0.8-1.1s] Pivoting on that hand, she spins her whole body around fast: for an instant her BACK is toward the camera, and one leg swings out straight along the floor behind her.
[1.1-1.4s] Still spinning, the extended leg sweeps around low along the floor to the front, toward the RIGHT, at ankle height, like the classic spinning crouching sweep of 1990s anime fighting games.
[1.4-1.8s] She finishes the full turn, pulls the leg back under her and lifts her hand from the ground.
[1.8-5s] She is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE full 360-degree spin and ONE sweep. The sweeping leg stays straight and low along the floor the whole time, never above knee height. She stays low and never stands up. She never holds any object. Keep her face, blonde ponytail, red vest, belt, gloves, cargo pants, boots, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p11 — cirurgião sádico

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — pisão no joelho (perna da frente) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira baixa, sem giro | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the surgeon from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] He holds his fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; his hair and clothes move softly with the breathing.
His feet stay planted and never slide; he never leaves the guard and never turns toward the camera. He never holds any object. Keep his face, silver hair, lab coat, stethoscope, waistcoat, tie, gloves, trousers, shoes, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the surgeon from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] He holds his fighting guard exactly as in the start frame.
[0.5-1s] He sinks into a deep fighting-game crouch: a low squat with his hips dropping all the way down to the level of his knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, his arms kept in his guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] He holds the deep squat, breathing calmly: only his shoulders rise and fall slightly, his hips stay low and his feet stay planted.
[3-3.5s] He rises smoothly back to his standing guard.
[3.5-5s] He holds the same guard as the end frame and stays still.
His feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. He does not punch, kick or sit down. He never holds any object. Keep his face, silver hair, lab coat, stethoscope, waistcoat, tie, gloves, trousers, shoes, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the surgeon from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] He holds his fighting guard exactly as in the start frame.
[1-2s] He raises his front knee (the leg closer to the RIGHT side of the frame) and drives ONE stomping kick straight forward and down with the sole of his shoe, ending at the height of an opponent's knee toward the RIGHT; his lab coat swings with it. He pulls the leg back and sets the foot down exactly where it started. His arms stay loose in his guard.
[2-5s] He holds the same guard as the end frame, breathing calmly, feet planted.
One kick only. After the kick the same leg is in front again, as in the start frame. His whole body stays inside the frame. He never holds any object. Keep his face, silver hair, lab coat, stethoscope, waistcoat, tie, gloves, trousers, shoes, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the surgeon from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] He holds his deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.9s] Staying low, he shifts his weight onto his rear leg.
[0.9-1.3s] He extends his front leg straight out along the floor toward the RIGHT in ONE low, sweeping kick at ankle height, the foot sliding just above the floor.
[1.3-1.8s] He pulls the leg back under him into the crouch.
[1.8-5s] He is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE sweep only, no spin. The sweeping leg stays low along the floor the whole time, never above knee height. He stays low and never stands up. He never holds any object. Keep his face, silver hair, lab coat, stethoscope, waistcoat, tie, gloves, trousers, shoes, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p12 — criatura feral

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — garras duplas (os dois braços descem juntos) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira baixa, sem giro | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the undead creature from the start frame, on a flat solid green background. It faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] It holds its fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; its hair and clothes move softly with the breathing.
Its feet stay planted and never slide; it never leaves the guard and never turns toward the camera. It never holds any object. Keep its face, stitched scars, black hair, bandages, clawed fingers, trousers, red sash, bare feet, colors, line art and size exactly as in the frames; its size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the undead creature from the start frame, on a flat solid green background. It faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] It holds its fighting guard exactly as in the start frame.
[0.5-1s] It sinks into a deep fighting-game crouch: a low squat with its hips dropping all the way down to the level of its knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, its arms kept in its guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] It holds the deep squat, breathing calmly: only its shoulders rise and fall slightly, its hips stay low and its feet stay planted.
[3-3.5s] It rises smoothly back to its standing guard.
[3.5-5s] It holds the same guard as the end frame and stays still.
Its feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. It does not punch, kick or sit down. It never holds any object. Keep its face, stitched scars, black hair, bandages, clawed fingers, trousers, red sash, bare feet, colors, line art and size exactly as in the frames; its size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the undead creature from the start frame, on a flat solid green background. It faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] It holds its fighting guard exactly as in the start frame.
[1-1.5s] It raises both arms high above its head together, clawed fingers spread.
[1.5-2s] It slashes both arms DOWN together in ONE brutal clawing swipe in front of it, lunging forward and hunching, ending with both arms low in front of its body.
[2-2.5s] It pulls back into its stalking guard.
[2.5-5s] It holds the same guard as the end frame, breathing calmly, feet planted.
One swipe only, both arms together. It does not step: the same leg stays in front and both feet stay planted and never slide. Its whole body stays inside the frame. It never holds any object. Keep its face, stitched scars, black hair, bandages, clawed fingers, trousers, red sash, bare feet, colors, line art and size exactly as in the frames; its size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the undead creature from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] It holds its deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.9s] Staying low, it shifts its weight onto its rear leg.
[0.9-1.3s] It extends its front leg straight out along the floor toward the RIGHT in ONE low, sweeping kick at ankle height, the bare foot sliding just above the floor. The leg twists at an unnatural angle as it sweeps, like a broken puppet.
[1.3-1.8s] It pulls the leg back under it into the crouch.
[1.8-5s] It is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE sweep only, no spin. The sweeping leg stays low along the floor the whole time, never above knee height. It stays low and never stands up. It never holds any object. Keep its face, stitched scars, black hair, bandages, clawed fingers, trousers, red sash, bare feet, colors, line art and size exactly as in the frames; its size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```
## p13 a p16

- **p15:** já tem os seis clipes no Flick (idle, andar para frente e para trás, agachar, H,
  2M), aprovados e no jogo. Os prompts dela estão em [p15.md](p15.md).
- **p16:** já tem as imagens iniciais no tamanho do H3. São as `inicio_*.png` sem sufixo,
  porque ela não tem vídeo antigo. O corpo ocupa 70% da altura, para ela ficar um pouco mais
  baixa que a p15.
  - O vídeo de agachar dá a `inicio_agachado.png` dela, para o 2M.
  - O `tamanho.json` usa escala 0,4103 e altura 221 e entra junto com o pacote.
- **p13 e p14:** os golpes são os **propostos** nas fichas, ainda sem aprovação.
  - Os concepts já estão em `art/concepts/`, mas falta a Base no ChatGPT.
  - Depois da Base, montar as imagens com `inicio_da_base.py` sem `--sufixo`, como na p16.
  - `--altura`: p13 é enorme (perto de 0,77, o máximo que cabe o chute) e p14 é mais baixo.
  - Medir pelo rosto contra a p15.

## p13 — colosso de punhos pesados

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — martelo frontal (soco descendente) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira baixa, sem giro | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the giant brawler from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] He holds his fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; his hair and clothes move softly with the breathing.
His feet stay planted and never slide; he never leaves the guard and never turns toward the camera. He never holds any object. Keep his bald head, face, torn black vest, tank top, red arm wraps, reinforced gloves, cargo pants, heavy boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the giant brawler from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] He holds his fighting guard exactly as in the start frame.
[0.5-1s] He sinks into a deep fighting-game crouch: a low squat with his hips dropping all the way down to the level of his knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, his arms kept in his guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] He holds the deep squat, breathing calmly: only his shoulders rise and fall slightly, his hips stay low and his feet stay planted.
[3-3.5s] He rises smoothly back to his standing guard.
[3.5-5s] He holds the same guard as the end frame and stays still.
His feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. He does not punch, kick or sit down. He never holds any object. Keep his bald head, face, torn black vest, tank top, red arm wraps, reinforced gloves, cargo pants, heavy boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the giant brawler from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] He holds his fighting guard exactly as in the start frame.
[1-1.5s] He raises his rear fist (the fist held farther back, toward the LEFT side of the frame) high above his head, elbow bent, torso rising tall, and holds that raised pose for a split second.
[1.5-2s] He brings it DOWN in ONE crushing hammer-fist strike in a vertical arc in front of him, his whole body leaning forward into it, ending with the fist at waist height in front of his body.
[2-2.5s] He pulls back and returns to the guard.
[2.5-5s] He holds the same guard as the end frame, breathing calmly, feet planted.
One strike only. The fist moves DOWN, never straight forward. He does not step: the same leg stays in front and both feet stay planted and never slide. His whole body stays inside the frame. He never holds any object. Keep his bald head, face, torn black vest, tank top, red arm wraps, reinforced gloves, cargo pants, heavy boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the giant brawler from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] He holds his deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.9s] Staying low, he shifts his weight onto his rear leg.
[0.9-1.3s] He extends his front leg straight out along the floor toward the RIGHT in ONE low, heavy, sweeping kick at ankle height, the heavy boot sliding just above the floor.
[1.3-1.8s] He pulls the leg back under him into the crouch.
[1.8-5s] He is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE sweep only, no spin. The sweeping leg stays low along the floor the whole time, never above knee height. He stays low and never stands up. He never holds any object. Keep his bald head, face, torn black vest, tank top, red arm wraps, reinforced gloves, cargo pants, heavy boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p14 — malandro das fintas

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 2 | andar para frente | `inicio_esquerda_h3` → `inicio_centro_h3` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro_h3` → `inicio_esquerda_h3` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar (e levantar) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo; o quadro mais baixo vira `inicio_agachado_h3` |
| 5 | H — chute de malandro (circular longo, perna de trás) | `inicio_centro_h3` → `inicio_centro_h3` | abaixo |
| 6 | 2M — rasteira baixa, sem giro | `inicio_agachado_h3` → `inicio_agachado_h3` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the trickster fighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] He holds his fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; his hair and clothes move softly with the breathing.
His feet stay planted and never slide; he never leaves the guard and never turns toward the camera. He never holds any object. Keep his face, messy tied black hair, black long coat, black shirt, red sashes, cargo pants, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the trickster fighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] He holds his fighting guard exactly as in the start frame.
[0.5-1s] He sinks into a deep fighting-game crouch: a low squat with his hips dropping all the way down to the level of his knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, his arms kept in his guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] He holds the deep squat, breathing calmly: only his shoulders rise and fall slightly, his hips stay low and his feet stay planted.
[3-3.5s] He rises smoothly back to his standing guard.
[3.5-5s] He holds the same guard as the end frame and stays still.
His feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. He does not punch, kick or sit down. He never holds any object. Keep his face, messy tied black hair, black long coat, black shirt, red sashes, cargo pants, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the trickster fighter from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] He holds his fighting guard exactly as in the start frame.
[1-2s] He pivots on his front foot and throws ONE long roundhouse kick with his rear leg (the leg farther back, on the LEFT side of the frame): the knee rises, then the leg whips around horizontally at chest height, fully extended toward the RIGHT; his long coat swings with it. He retracts the knee and sets the foot back down exactly where it started. His hands stay up in his guard.
[2-5s] He holds the same guard as the end frame, breathing calmly, feet planted.
One kick only. After the kick the same leg is in front again, as in the start frame. His whole body stays inside the frame. He never holds any object. Keep his face, messy tied black hair, black long coat, black shirt, red sashes, cargo pants, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the trickster fighter from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] He holds his deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.9s] Staying low, he shifts his weight onto his rear leg.
[0.9-1.3s] He extends his front leg straight out along the floor toward the RIGHT in ONE low, sweeping kick at ankle height, the foot sliding just above the floor.
[1.3-1.8s] He pulls the leg back under him into the crouch.
[1.8-5s] He is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE sweep only, no spin. The sweeping leg stays low along the floor the whole time, never above knee height. He stays low and never stands up. He never holds any object. Keep his face, messy tied black hair, black long coat, black shirt, red sashes, cargo pants, boots, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

## p16 — Bonnie Scarlet

| Ordem | Clipe | Início → fim | Prompt |
|---|---|---|---|
| 1 | idle | `inicio_centro` → `inicio_centro` | abaixo |
| 2 | andar para frente | `inicio_esquerda` → `inicio_centro` | "Andando para frente como personagem de jogo de luta mantendo sua guarda" |
| 3 | andar para trás | `inicio_centro` → `inicio_esquerda` | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virada para a direita" |
| 4 | agachar (e levantar) | `inicio_centro` → `inicio_centro` | abaixo; o quadro mais baixo vira `inicio_agachado` |
| 5 | H — chute de estrela (chute alto com giro) | `inicio_centro` → `inicio_centro` | abaixo |
| 6 | 2M — rasteira girando | `inicio_agachado` → `inicio_agachado` | abaixo |

**Idle**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the clown martial artist from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-5s] She holds her fighting guard exactly as in the start frame, breathing calmly in a seamless loop: two slow breaths, chest and shoulders rising and falling slightly; her hair and clothes move softly with the breathing.
Her feet stay planted and never slide; she never leaves the guard and never turns toward the camera. She never holds any object. Keep her face, red clown nose, red ponytail and bow, red/black/cream diamond outfit, pompom, waist bows, gloves, mismatched socks, big sneakers, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**Agachar**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the clown martial artist from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-0.5s] She holds her fighting guard exactly as in the start frame.
[0.5-1s] She sinks into a deep fighting-game crouch: a low squat with her hips dropping all the way down to the level of her knees, thighs horizontal, knees sharply bent and pointing forward, heels flat on the ground; torso upright and leaning slightly forward, her arms kept in her guard as in the start frame, eyes forward on the opponent. This is a deep squat, not a wide horse stance.
[1-3s] She holds the deep squat, breathing calmly: only her shoulders rise and fall slightly, her hips stay low and her feet stay planted.
[3-3.5s] She rises smoothly back to her standing guard.
[3.5-5s] She holds the same guard as the end frame and stays still.
Her feet never move or slide: the same leg stays in front and both feet stay in the same place for the whole video. She does not punch, kick or sit down. She never holds any object. Keep her face, red clown nose, red ponytail and bow, red/black/cream diamond outfit, pompom, waist bows, gloves, mismatched socks, big sneakers, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**H**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the clown martial artist from the start frame, on a flat solid green background. She faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] She holds her fighting guard exactly as in the start frame.
[1-2s] She spins her whole body around on her front foot: for an instant her BACK is toward the camera, and her rear leg swings around straight at head height, the heel leading, striking toward the RIGHT. She completes the full turn and sets the foot back down exactly where it started, facing RIGHT again in the same guard.
[2-5s] She holds the same guard as the end frame, breathing calmly, feet planted.
ONE full 360-degree spin and ONE kick only. After the kick the same leg is in front again, as in the start frame. Her whole body stays inside the frame. She never holds any object. Keep her face, red clown nose, red ponytail and bow, red/black/cream diamond outfit, pompom, waist bows, gloves, mismatched socks, big sneakers, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

**2M**
```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the clown martial artist from the start frame, on a flat solid green background. The camera stays fixed and never zooms.
[0-0.5s] She holds her deep crouching guard facing RIGHT, exactly as in the start frame.
[0.5-0.8s] She drops even lower and plants one hand flat on the ground.
[0.8-1.1s] Pivoting on that hand, she spins her whole body around fast: for an instant her BACK is toward the camera, and one leg swings out straight along the floor behind her.
[1.1-1.4s] Still spinning, the extended leg sweeps around low along the floor to the front, toward the RIGHT, at ankle height, like the classic spinning crouching sweep of 1990s anime fighting games.
[1.4-1.8s] She finishes the full turn, pulls the leg back under her and lifts her hand from the ground.
[1.8-5s] She is back in the deep crouching guard facing RIGHT, as in the end frame, breathing calmly.
ONE full 360-degree spin and ONE sweep. The sweeping leg stays straight and low along the floor the whole time, never above knee height. She stays low and never stands up. She never holds any object. Keep her face, red clown nose, red ponytail and bow, red/black/cream diamond outfit, pompom, waist bows, gloves, mismatched socks, big sneakers, colors, line art and size exactly as in the frames; her size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```
