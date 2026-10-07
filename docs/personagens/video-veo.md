# Animação por vídeo (Veo) — padrão

Decidido no idle do p01: o movimento gerado em vídeo (Gemini/Veo) é muito mais fluido que
folhas do ChatGPT. O vídeo vira os quadros do jogo: o fundo verde é recortado, a cor é
igualada à da Base, o trecho certo é escolhido e os quadros são reduzidos (ex.: 16 por
loop). No jogo o personagem em pé tem 228 px; o vídeo dá ~1000 px, então resolução sobra.

## Regras para todo vídeo

- **Imagem inicial:** sempre a **Base** do personagem (`art/keys/<id>/idle_base.png`),
  com espaço livre em volta. Formato **vertical 9:16**. Assim nenhum golpe corta pés,
  mãos ou cabelo.
- **Um movimento por vídeo**, começando e terminando na guarda da Base (fica fácil emendar
  com o idle).
- **Câmera travada, vista lateral, virado para a direita**, sem girar para a câmera.
- **Fundo verde liso**, sem chão, sem cenário, sem sombra, sem efeitos de energia (o jogo
  desenha os efeitos).
- **Design fixo:** cabelo, rosto e cores iguais à imagem. Se o vídeo mudar o cabelo, a cor
  da roupa ou o rosto, gerar de novo.

## Prompt base (colar e trocar só o bloco MOVIMENTO)

```
Animate this exact character from the reference image. 2D anime fighting game
animation, smooth and fluid, high quality, same art style as the image.

CHARACTER LOCK: keep the design exactly as in the image — same messy dark BROWN hair
(not black, not spiky), same face, same body proportions, same outfit and colors
(royal blue and white sleeveless vest with red collar, black tank top, black belt with
red tips, white pants with blue stripes, black fingerless gloves with red, blue/white/red
high-top sneakers). Do not restyle or redesign the character.

CAMERA: static locked-off camera. No camera movement, no zoom, no rotation, no cuts.
The character stays in side view facing RIGHT the whole time and never turns toward the
camera. The whole body stays fully inside the frame with margin around it.

BACKGROUND: plain solid bright green (#00FF00). No floor, no walls, no environment, no
shadows, no energy effects, no particles, no text.

MOVEMENT: <descrição do golpe>
Start and end in the same fighting stance as the reference image.
```

## Movimentos do p01 (bloco MOVIMENTO)

| Clip | MOVIMENTO |
|---|---|
| idle | Calm breathing in fighting guard, looping: chest and shoulders rise and fall, fists bob slightly, belt ends sway. Feet stay planted. |
| walk_forward | Walks forward slowly in guard with short controlled fighter steps, fists up, 2 full steps, then stops in guard. |
| walk_back | Walks backward carefully in guard with short steps, weight on the back leg, eyes forward. |
| crouch | Lowers into a crouching guard, holds and breathes, then rises back to the stance. |
| jump | Crouches, jumps straight up with knees tucked, lands softly back in guard. |
| L | Quick jab with the front fist at face height, snaps back to guard. |
| M | Straight punch with the rear fist, rotating hips and shoulders, returns to guard. |
| H | High roundhouse kick with the rear leg at head height, belt swinging, returns to guard. |
| 2L | From a crouch, short low punch, back to crouching guard. |
| 2M | From a crouch, long low front kick close to the ground, back to crouching guard. |
| 2H | Spinning low sweep with the rear leg, ends in crouching guard. |
| S1 | Brings both hands together to the rear hip, gathers energy, then thrusts both open palms forward with the whole body, holds, returns to guard. (No energy visible.) |
| S2 | Rising uppercut: crouches, then spirals upward with the rear fist straight up, feet leave the ground, lands back in guard. |
| S3 | Parry: opens the guard, deflects an invisible attack with the forearm, answers with a short hard body punch, returns to guard. |
| S4 | Spinning jump kick moving forward, leg extended like a propeller for two turns, lands in guard. |
| throw | Grabs the collar of an invisible opponent, turns and throws over the shoulder, returns to guard. |
| block | Raises crossed forearms in front of the face, absorbs a hit, lowers back to guard. |
| hit | Gets hit in the face: head snaps back, steps back, recovers the guard. |
| knockdown | Gets knocked back off his feet, falls on his back, lies down a moment, gets up into guard. |
| intro | Stands relaxed with eyes closed, breathes in, tightens the belt with both hands, opens his eyes and slowly takes the fighting stance. |
| win | Steps out of the guard, stands straight, closes one fist in front of the chest and looks down, calm and restrained. |
