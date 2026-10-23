# Tela de vitória

Quando o lutador do jogador vence, a luta some atrás do **cenário borrado** e ele faz a cena de
vitória:

1. o fundo borrado cobre a luta (~0,2 s);
2. o lutador (vídeo no verde, do quadril para cima) entra deslizando pela esquerda, já tocando o vídeo;
3. **"VITÓRIA"** bate à direita, numa faixa de tinta com o traço na cor dele (clarão e tremida ao assentar);
4. um **quadro de mangá** entra com uma frase dele e a etiqueta com o nome;
5. o vídeo acaba, o último quadro fica parado 0,7 s e a tela escurece até o preto (~4,5 s no total).

Depois disso a história segue com o resultado ou, fora dela, a revanche começa. Um toque ou um botão
leva direto para a saída (só depois de 0,6 s, para o toque que acabou a luta não pular a cena).

Primeiro teste: **Ryūga** (`player_base`, vídeo `Max_Ajusta_Gola_5_Segundos.mp4`). Por enquanto
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
    --marca 8,8,150,50 --fps 36 --segurar 700 \
    --frase "Eu li cada movimento seu. Treine mais e volte." \
    --frase "Calma vence pressa. Sempre." \
    --frase "Ainda não acabou. Eu preciso ficar mais forte."
```

- `--inicio/--fim/--passo/--fps`: o trecho e a velocidade (sem `--fps`, a do vídeo; o Ryūga usa 36,
  1,5x o vídeo). No jogo, entre um quadro e o próximo os dois se misturam (o jogo roda a 60 Hz).
- `--segurar MS`: o último quadro parado antes de sair (o Ryūga usa 700).
- `--marca x0,y0,x1,y1`: apaga a marca d'água clara.
- `--frase`: repita para várias. Sem `--frase`, as frases que já estão no `vitoria.json` ficam.
- `--resolucao`: pixels guardados por pixel da tela (0,6).

A prévia (quadros sobre a tela escura) sai em `android/app/build/sprite-review/vitorias/<id>.png`.

## O fundo de um cenário

```bash
python3 tools/sprites/vitoria_video.py --fundo arte_do_cenario.png --cenario porto_secreto
```

Corta em 16:9, reduz para 640x360 e borra. Sem `--cenario`, grava o `padrao`.
