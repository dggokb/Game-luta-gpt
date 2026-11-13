# Tela de vitória

Quando o lutador do jogador vence, a luta some atrás do **cenário borrado** e ele faz a cena de
vitória:

1. o fundo borrado cobre a luta (~0,2 s);
2. o lutador (vídeo no verde, do quadril para cima) entra deslizando pela esquerda, já tocando o vídeo;
3. **"VITÓRIA"** bate à direita, numa faixa de tinta com o traço na cor dele (clarão e tremida ao assentar);
4. um **quadro de mangá** entra com uma frase dele e a etiqueta com o nome;
5. o vídeo acaba, a pose final fica parada 2 s e a tela escurece até o preto (~5 s no total).

Depois disso a história segue com o resultado ou, fora dela, a revanche começa. Um toque ou um botão
leva direto para a saída (só depois de 0,6 s, para o toque que acabou a luta não pular a cena).

O **Ryūga** (`player_base`) tem a cena em vídeo (`Max_Ajusta_Gola_5_Segundos.mp4`). Os outros 15
(p02–p16) têm uma **arte de vitória parada**, gerada no Codex no estilo das artes do versus: ela entra
deslizando como o vídeo e chega devagar para perto (5% até a saída), 3,6 s na tela. Por enquanto
**todo cenário usa o fundo do porto** (`vitorias/fundos/padrao.webp`).

## Como funciona

| Peça | O que faz |
|---|---|
| `tools/sprites/vitoria_video.py` | Tira o verde e a marca d'água, recorta cada quadro e grava `assets/vitorias/<id>/`; com `--fundo` grava o fundo borrado de um cenário |
| `assets/vitorias/<id>/vitoria.json` | Quadros (`q000.webp`...), onde cada um fica, `quadroMs`, `segurarMs` e as `frases` (o jogo sorteia uma) |
| `assets/vitorias/fundos/<cenário>.webp` | O fundo borrado do cenário; sem ele, vale o `padrao.webp` |
| `TelaVitoria` | Lê a cena dos lutadores do jogador na montagem da luta, toca e desenha a tela |
| `GameView` | Começa a tela depois do K.O. (na história, depois da fala do "vitoria"), segura a história e a revanche até ela acabar e não desenha a luta por baixo |

Só os 6 primeiros quadros ficam lidos desde a montagem da luta. O resto é lido numa thread à parte,
12 à frente do que está na tela, e solto depois de mostrado. Os 124 quadros ocupam ~4,8 MB no APK,
mas poucos ficam na memória.

"VITÓRIA" e as frases passam pela tradução da história (`historia/idiomas/en.json`, `es.json`).

**Personagem sem pasta em `assets/vitorias/` não muda nada**: a luta acaba como antes.

## Pôr a vitória num personagem

Vídeo no verde, o lutador do quadril para cima, olhando para a direita (o texto fica à direita).

```bash
pip install -r tools/sprites/requirements-animar.txt
python3 tools/sprites/vitoria_video.py Max_Ajusta_Gola_5_Segundos.mp4 --personagem player_base \
    --marca 8,8,150,50 --fps 48 --segurar 2000 \
    --frase "Eu li cada movimento seu. Treine mais e volte." \
    --frase "Calma vence pressa. Sempre." \
    --frase "Ainda não acabou. Eu preciso ficar mais forte."
```

- `--inicio/--fim/--passo/--fps`: o trecho e a velocidade (sem `--fps`, a do vídeo; o Ryūga usa 48,
  2x o vídeo: a gola em ~2,6 s). No jogo, entre um quadro e o próximo os dois se misturam (o jogo roda a 60 Hz).
- `--segurar MS`: o último quadro parado antes de sair (o Ryūga usa 2000).
- `--marca x0,y0,x1,y1`: apaga a marca d'água clara.
- `--frase`: repita para várias. Sem `--frase`, as frases que já estão no `vitoria.json` ficam.
- `--resolucao`: pixels guardados por pixel da tela (0,6).

A prévia (quadros sobre a tela escura) sai em `android/app/build/sprite-review/vitorias/<id>.png`.

## Arte de vitória parada (Codex)

Os originais e o prompt de cada um ficam em `art/vitorias/` (`pNN.webp`, `prompt_pNN.txt`). Cada
pedido anexa o concept (`art/concepts/pNN.png`) e a arte do versus do mesmo lutador
(`art/versus/pNN.webp`, como `versus.png`), para sair com o mesmo rosto, roupa e traço:

```bash
codex exec --skip-git-repo-check --sandbox workspace-write -i concept.png -i versus.png - < prompt_pNN.txt
python3 tools/sprites/vitoria_video.py pNN.png --personagem pNN \
    --frase "..." --frase "..." --frase "..."
```

O prompt pede: da cabeça ao quadril, virado para a direita, uma pose de **vitória** diferente da do
versus, fundo #00FF00, 1024×1536, mãos e braços certos. Com uma imagem no lugar do vídeo, o
`vitoria_video.py` tira o verde, recorta e grava um quadro só (`q000.webp`): o desenho com 1,25 da
altura da tela, a cabeça 14 px abaixo do alto e o quadril passando da borda de baixo, o mesmo
enquadramento do vídeo do Ryūga. As frases precisam da tradução em `historia/idiomas/en.json` e
`es.json`.

| Lutador | Pose de vitória |
|---|---|
| p02 Yin-lu | punho na palma, cabeça baixa, sorriso calmo |
| p03 Kōga | braços cruzados, olhando de cima por cima do ombro |
| p04 Brucy | punho para o alto e polegar para cima |
| p05 | guardando a katana nas costas, a outra mão num selo ninja |
| p06 | braços cruzados, dois dedos na máscara numa continência de deboche |
| p07 | abaixa a aba do chapéu, leve reverência |
| p08 | chicote enrolado no ombro, mandando um beijo |
| p09 Lars | os dois punhos para o alto, gargalhando |
| p10 Fenna | mão na cintura, polegar para cima e piscadela |
| p11 Blackwood | ajeitando a luva, sorriso sinistro |
| p12 Necro | uivando para o céu, garras abertas |
| p13 Krüger | braços cruzados, desprezo de cima |
| p14 | mão no cabelo, olhos fechados, sorriso torto |
| p15 Natisuko | apertando a faixa preta com as duas mãos |
| p16 Bonnie | sinal de paz no olho piscando, língua de fora |

## O fundo de um cenário

```bash
python3 tools/sprites/vitoria_video.py --fundo arte_do_cenario.png --cenario porto_secreto
```

Corta em 16:9, reduz para 640x360 e borra. Sem `--cenario`, grava o `padrao`.
