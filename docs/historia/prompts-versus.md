# Prompts da tela de versus (uma ilustração por lutador)

A tela de versus (`TelaVersus`, antes de cada luta do torneio e do duelo) mostra o titular de
cada dupla na frente e o parceiro atrás. A arte de cada lutador fica em
`android/app/src/main/assets/historia/torneio/versus/pNN.png`. Por enquanto são poses tiradas dos
sprites (`tools/historia/versus_poses.py`), provisórias: a ideia é trocar por ilustrações.

## Como gerar

- Uma imagem por lutador, **1024×1536 (em pé)**, com o concept dele (`art/concepts/pNN.png`)
  anexado como referência de roupa, cabelo e cores.
- **Fundo verde liso (#00FF00)**, sem chão, sem sombra no fundo e sem efeitos passando por cima
  do verde: o recorte tira o verde (como nos efeitos de vídeo).
- Corpo inteiro, dos pés à cabeça, **virado para a direita** (três quartos), numa **pose de
  assinatura** do personagem (não a guarda de luta do sprite), ocupando a altura quase toda (pés
  a uns 2% da borda de baixo).
- Mesmo estilo para os 16: o dos storyboards da abertura (`sokon_storyboard_06B...`, no Drive),
  anime de luta, traço limpo, cel shading forte, luz de contorno.

Prompt base (troque o trecho entre colchetes):

> Full-body anime fighting game character illustration for a VERSUS screen, in the same style as
> the attached concept sheet: [DESCRIÇÃO]. Pose: [POSE], facing RIGHT, NOT a plain fighting
> guard, whole body visible from head to feet, feet near the bottom edge.
> Clean bold lineart, strong cel shading, dramatic rim light, high detail, 2D anime fighting
> game key art. Flat solid pure green background (#00FF00), no floor, no shadow on the
> background, no text, no effects over the background. Portrait 1024x1536.

## Poses (uma de assinatura para cada um, não a guarda do sprite)

| Lutador | [POSE] |
|---|---|
| p01 Ryūga | deep lunging stance throwing a powerful straight punch toward the right, the other fist pulled back at the hip, belt ends and pants flowing, fierce battle shout |
| p02 Yin-lu | elegant kung fu crane pose standing on one leg, one open palm thrust forward, the other arm raised gracefully behind, calm sharp gaze |
| p03 Kōga | cocky forward lean, beckoning the opponent with one hand ("come on"), the other fist clenched low, wild arrogant grin |
| p04 Brucy | wide street-boxer stance, punching one bandaged fist into his open palm, big confident grin |
| p05 | crouched low like a predator, katana half drawn from the sheath, tattered cloth flowing |
| p06 | leaning forward, one clawed hand reaching toward the right, the other arm swept back, mask eyes glowing |
| p07 | elegant menacing stance, one hand adjusting the fedora brim, the other ready to strike |
| p08 | hip cocked, cracking the whip in a wide arc around her, dangerous smile |
| p09 | slamming both fists together in front of his chest, roaring with laughter |
| p10 | pointing at the opponent with one hand, the other on her hip, annoyed smirk |
| p11 | unhinged, gloved hands raised like claws, crazy grin, lab coat flaring |
| p12 Necro | hunched aggressive zombie stance, clawed hands forward, mouth open |
| p13 Krüger Stahl | towering, punching his fist into his open palm, looking down menacingly |
| p14 | relaxed confident stance, one hand raised beckoning, crooked smirk |
| p15 Natisuko | one leg raised in a chambered side kick, arms in a tight karate guard, focused glare |
| p16 Bonnie Scarlet | playful jump with one leg kicked back, fists up, wide clown grin |

O prompt base usa "Pose: [POSE]" no lugar da guarda.

## Descrições

| Lutador | [DESCRIÇÃO] |
|---|---|
| p01 Ryūga | young man, spiky brown hair, sleeveless blue-and-white martial arts top with red collar over a black tank top, white baggy pants with blue stripes, red and black belt, black fingerless gloves, blue-white sneakers |
| p02 Yin-lu | young woman, short silver bob hair with a dark headband, teal sleeveless qipao top with gold buttons, orange waist sash, navy baggy pants, fingerless gloves |
| p03 Kōga | young man, messy dark brown hair, black sleeveless hooded vest with red trim over a white tank top, black cargo pants with red straps, red and black sneakers, fingerless gloves |
| p04 Brucy | young man, spiky white hair, yellow tank top, bandaged forearms and fists, red waist sash, navy baggy pants |
| p05 | masked ninja, wild black spiky hair, black face mask, tattered black and red ninja clothes, bandaged arm with a dark tattoo, katana on the back |
| p06 | woman with long purple hair, grey mask with glowing purple eyes, black cropped hoodie with purple accents, black cargo pants |
| p07 | older gentleman, grey hair and goatee, black fedora with red band, long black coat with red lining, black suit and red tie |
| p08 | woman with long wavy pink hair, elegant crimson dress with a high slit, black gloves, red whip |
| p09 | man with spiky orange hair, firefighter outfit: red and black sleeveless jacket with yellow reflective stripes, black pants, heavy gloves |
| p10 | woman with blonde ponytail, firefighter outfit: red cropped vest with yellow stripes, black pants, gloves |
| p11 | mad surgeon, wild white hair, blood-stained white lab coat, red tie, stethoscope, black gloves |
| p12 Necro | stitched zombie, grey-green skin with stitches, tattered bandages and a red rag around the waist, hunched aggressive pose |
| p13 Krüger Stahl | huge bald muscular man with a scar, black tank top, black leather vest, red armband, black pants and boots |
| p14 | man with long black hair tied back, black outfit with red details, fingerless gloves |
| p15 Natisuko | woman with a long black ponytail, white karate gi top open over a black top, white pants, black belt |
| p16 Bonnie Scarlet | red-haired clown girl, red clown nose, star face paint, red and white striped stockings, harlequin fighter outfit |

## Depois de gerar

Ponha as imagens no Drive (`pNN.png`). O recorte tira o verde com a chave do
`tools/sprites/efeito_video.py`, ajusta a escala (um lutador de altura comum com uns 470 pixels,
pés na base, 2 pixels por pixel do mundo) e grava em `historia/torneio/versus/pNN.png`. O jogo
não muda.
