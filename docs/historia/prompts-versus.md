# Tela de versus: as peças geradas no Codex

A tela de versus (`TelaVersus`, antes de cada luta do torneio e do duelo) é uma **montagem**,
como a arte de referência (o Ryūga e a Yin-lu contra o Kōga e o Brucy, com o dragão e o tigre).
Cada peça é uma imagem separada, e o jogo junta tudo e anima:

| Peça | Imagem do Codex | No jogo |
|---|---|---|
| 16 lutadores | da cintura para cima, virado para a direita, numa pose de assinatura (não a guarda do sprite), fundo verde liso | `versus/pNN.png`: sem o verde, 1100 px de altura. O titular fica grande perto do meio e o parceiro atrás, do lado de fora; o da direita é espelhado |
| dragão | dragão de energia azul e branca em pinceladas, sobre o preto | `versus/dragao.png`: o preto vira transparência. Fica atrás da dupla do Ryūga |
| tigre | tigre de energia vermelha, sobre o preto | `versus/tigre.png`: fica atrás da dupla do Kōga |
| templo | o pátio do templo à noite, com a lua, vazio | `versus/templo.jpg` (16:9): o fundo, partido na diagonal e tingido de azul e de vermelho |
| VS | letras "VS" de prata escovada, rachadas, fundo verde | `versus/vs.png` |
| logo | o logo do jogo (`historia/logo.jpg`), não é gerado | `versus/logo.png`: o preto vira transparência |

Os originais do Codex e os prompts de cada um ficam em `art/versus/` (`pNN.webp` e
`prompt_pNN.txt`, `dragao`, `tigre`, `templo`, `vs`). Para refazer as peças do jogo:

```bash
python3 tools/historia/versus_arte.py art/versus
```

## Como gerar (Codex)

O Codex entra com a conta do ChatGPT (`npm install -g @openai/codex`, `codex login --device-auth`;
veja [../cenarios-com-codex.md](../cenarios-com-codex.md)). Cada pedido, numa pasta com as
imagens anexadas:

```bash
codex exec --skip-git-repo-check --sandbox workspace-write -i concept.png -i style.png - < prompt.txt
```

- **Lutadores**: anexam o concept do lutador (`art/concepts/pNN.png`, roupa, cabelo e cores) e
  o storyboard 06B da abertura (`style.png`, o estilo: arte de luta anime, traço limpo, cel
  shading forte, luz de contorno). 1024×1536, da cintura para cima, as duas mãos e o cabelo
  inteiros dentro da imagem, fundo #00FF00 sem efeitos por cima.
- **Dragão e tigre**: anexam o logo (o mesmo visual de pinceladas de energia). 1536×1024, só a
  criatura, fundo preto.
- **Templo**: anexa o estilo. 1536×1024, sem ninguém e sem texto, escuro, simétrico.
- **VS**: 1024×1024, só as letras, fundo verde.

Para trocar um lutador: gere de novo com o prompt dele (`art/versus/prompt_pNN.txt`), grave como
`art/versus/pNN.webp` e rode o `versus_arte.py`. O jogo não muda.

## Poses dos lutadores

| Lutador | Pose |
|---|---|
| p01 Ryūga | soco reto avançando para a direita, o outro punho puxado para trás, grito de luta |
| p02 Yin-lu | golpe de palma aberta para a frente, a outra mão erguida atrás, olhar calmo |
| p03 Kōga | chamando o adversário com a mão ("vem"), o outro punho fechado, sorriso arrogante |
| p04 Brucy | socando a palma aberta com o punho enfaixado, sorriso confiante |
| p05 | katana meio desembainhada, olhos por cima da máscara, pano rasgado esvoaçando |
| p06 | uma mão em garra para a direita, olhos da máscara brilhando roxos |
| p07 | ajeitando a aba do chapéu, sorriso perigoso, gola do casaco erguida |
| p08 | chicote esticado entre as mãos perto do rosto, sorriso perigoso |
| p09 | batendo os dois punhos na frente do peito, gargalhando |
| p10 | apontando para o adversário, a outra mão na cintura, irritada |
| p11 | mãos enluvadas erguidas como garras, sorriso louco, jaleco esvoaçando |
| p12 Necro | mãos em garra para a frente, boca aberta rosnando, ombros curvados |
| p13 Krüger Stahl | socando a palma aberta, olhando de cima, ameaçador |
| p14 | uma mão erguida chamando, sorriso torto, cabelo ao vento |
| p15 Natisuko | guarda de karatê fechada com um punho à frente, olhar concentrado |
| p16 Bonnie Scarlet | punhos perto do rosto, piscando, sorriso largo de palhaça |

## Descrições (vão no prompt de cada lutador)

| Lutador | Descrição |
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
