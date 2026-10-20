# Animação por vídeo no Omni Flash 1.1 (Google Flow) — padrão

Testado no p15 em 20/10/2026. Vale tudo do [video.md](video.md) (imagens iniciais, verde, sem
efeitos, sem adversário, conversão com `personagem.py`); aqui ficam só as diferenças do Omni
e a fórmula de prompt que funcionou. Os prompts aprovados do p15 estão na [ficha](p15.md).

## Configuração no Flow

- **Quadros de início e fim** nos campos do Flow (só no computador). Não precisa de tag nem de
  "use this image as..." no texto; no prompt as imagens são "the start frame" e "the end frame".
- **4 s** (o mínimo do Flow). 16:9. O vídeo sai a **24 fps** (96 quadros).
- Rascunho em 360p para acertar o prompt; o vídeo final em 720p. Não usar upscale de rascunho
  de 360p para sprite (inventa borda e atrapalha o recorte do verde).
- O modo de quadros não aceita imagem nem vídeo de referência junto.

## Qual par de quadros usar

| Animação | Início | Fim |
|---|---|---|
| fica no lugar (idle, agachar, pulo, golpes) | `inicio_centro` | `inicio_centro` |
| andar para frente | `inicio_esquerda` | `inicio_centro` |
| andar para trás, backdash | `inicio_direita` | `inicio_centro` |
| dash | `inicio_esquerda` | `inicio_direita` |
| jab (L de soco reto) | `inicio_centro` | pose do jab esticado (ver abaixo) |

Movimento que muda de lugar **não** usa a mesma imagem nas duas pontas: o modelo recua e volta,
desliza ou move a câmera para terminar no mesmo ponto.

## Fórmula do prompt

```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game
animation of the <personagem> from the start frame, on a flat solid green background. <Ela/Ele>
faces RIGHT the whole time and <stays in place | moves toward the LEFT/RIGHT of the frame>; the
camera stays fixed and never zooms<, and does not follow her/him>.
[0-0.5s] ... holds the guard exactly as in the start frame.
[0.5-...s] <a ação, descrita pela mecânica do corpo>
[...-4s] ... holds the same guard as the end frame, breathing calmly, feet planted.
<regras do clipe>. Keep <rosto, cabelo, roupa, luvas>, colors, line art and size exactly as in
the frames; <her/his> size never changes. Clean animation only: no shadow, floor, dust, speed
lines, motion trails, impact flashes or effects. No opponent. No music, no dialogue.
```

- **"One continuous shot, no scene cuts"** sempre: sem isso o Omni monta 2–3 tomadas.
- **Faixas de tempo** `[0-0.5s]`: guarda no começo e no fim, uma faixa por trecho que vira clipe
  (dash: guarda, arrancada, corrida, freada, guarda; pulo: preparo, subida, descida,
  aterrissagem, guarda). O golpe vai cedo e o resto é guarda parada (a ferramenta corta).
- **Prompt curto**: a aparência vem da imagem; o texto só lista o que não pode mudar.
- **Pose concreta, não número**: "two thirds of her height" quase não mudou o agachado;
  "hips down to the level of her knees, thighs horizontal" mudou.
- **Uma ação, contada**: "ONE ...", "Exactly one punch in the whole video", "neither fist moves
  forward again". Com 4 s sobrando o modelo tende a encher com um segundo golpe.
- **Negativos no texto**, curtos; o áudio sai sempre e é descartado ("No music, no dialogue" só
  evita barulho no arquivo).

### Pés e pernas

- Andar de luta: **"fighter's shuffle"** (pé da frente desliza, o de trás acompanha) e **"The same
  leg stays in front for the whole video; her feet only move closer and apart, her legs never
  cross or pass each other."** Com "steps" sai caminhada normal, cruzando as pernas.
- Corrida (dash): o contrário, passada de verdade: "both feet briefly leaving the ground on each
  stride".
- Backdash: "both feet leave the ground together and land together", "the same leg stays in front".
- Golpe e agachar: "the same leg stays in front, both feet stay planted and never slide".

### Braços

- Nomear o braço pelo **lado do corpo e pela câmera**, e dizer **onde cada punho está na imagem**:
  "her LEFT arm (the arm farther from the camera) is held out in front toward the RIGHT, and her
  RIGHT fist (the arm closer to the camera) is held at her chin". "Front/rear fist" sozinho e uma
  posição errada ("near her chest") fizeram o p15 socar com o braço errado.
- **Nome de golpe de arte marcial puxa combinação**: "gyaku-zuki" + "Shotokan" saiu kizami +
  gyaku (dois socos). Descrever o movimento sem o nome japonês.
- Nome de estilo também puxa postura: "Shotokan" no agachar virou base de cavalo; escrever "This
  is a deep squat, not a wide horse stance".

## Jab: vídeo até a pose esticada

O braço da frente já começa quase esticado na guarda; sem curso, o modelo soca com o de trás
(três tentativas no p15). O que funcionou:

1. **Pose do jab** (ChatGPT/Nano Banana, anexando `inicio_centro.png`), salva como
   `art/keys/<id>/pose_jab.png`:
   ```
   Edit this image. Change ONLY her LEFT arm (the arm farther from the camera, the one held out
   toward the right): extend it into a fully straight jab, fist closed and pointing to the RIGHT
   at chest height, her left shoulder reaching slightly forward. Everything else stays exactly
   identical: same position in the frame, same size, same feet and legs, same torso and head,
   same ponytail, her RIGHT fist (the arm closer to the camera) still at her chin, same line art,
   shading and colors. Same flat pure green background, same 1280x720 canvas. Do not redraw or
   move anything else. No effects, no motion lines, no shadow, no text.
   ```
   Conferir sobre o `inicio_centro`: pés no mesmo lugar, mesmo tamanho, punho de trás no lugar.
2. **Vídeo** com início `inicio_centro` e fim `pose_jab`:
   ```
   [0-0.8s] She holds her Shotokan guard exactly as in the start frame.
   [0.8-1.1s] Her LEFT arm (the arm farther from the camera) snaps straight forward into a very
   fast jab, reaching exactly the pose of the end frame. Her RIGHT fist stays at her chin and
   never moves.
   [1.1-4s] She holds that extended jab pose completely still, exactly as in the end frame.
   Only the left arm moves. Her hips, shoulders, legs and feet stay in place.
   ```
   (mais o cabeçalho e o fechamento da fórmula). Se recolher e esticar de novo: "She freezes in
   the extended jab pose like a held frame until the end of the video; her arm does not pull back."
3. **Volta à guarda na conversão**: o vídeo termina esticado; a animação do jogo repete os
   quadros do avanço ao contrário (`frames` com os índices de volta, ex. `[0,1,2,3,3,2,1,0]`),
   com o `impactFrame` no quadro esticado.

Vale para todos os jabs (L de soco reto de qualquer personagem).

## Se der errado

| Problema | Ajuste |
|---|---|
| câmera acompanha ou dá zoom | começar com "Fixed tripod camera, the frame never moves:" |
| desliza em vez de andar | "each step clearly lifts the foot off the ground and plants it again" |
| cruza as pernas no andar | "like a boxer moving forward, never like a normal walk"; ou 3 ciclos em vez de 4 |
| dois golpes ou dois pulos | "Exactly one ..., then she stays planted until the end of the video." |
| agachado raso | descrever a pose (quadril na altura dos joelhos, coxas na horizontal) |
| braço errado | lado do corpo + câmera + onde o punho está; no jab, quadro final com a pose |
