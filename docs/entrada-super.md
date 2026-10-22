# Entrada do Super (retrato ao fundo)

Quando o lutador solta o Super, o retrato dele (um vídeo no fundo verde) aparece **ao fundo**,
grudado no canto de baixo do lado da tela em que ele está, enquanto ele carrega. A luta fica
parada nesse tempo; quando o retrato volta para a borda e some, o Super sai, com o mesmo
congelamento, flash e tempo de reação de antes.

Primeiro teste: **p15** (vídeo `Entrada_Esquerda_Direita.mp4`, ela entrando pela esquerda).

## Como funciona

| Peça | O que faz |
|---|---|
| `tools/sprites/super_video.py` | Tira o verde (chave por diferença de cor do `efeito_video.py`), apaga a marca d'água, corta cada quadro no desenho e grava `assets/supers/<id>/` |
| `assets/supers/<id>/super.json` | Quadros (`q00.webp`...), onde cada um fica na tela, `quadroMs`, `segurarMs`, `saidaMs` |
| `EntradaSuper` | Lê a pasta dos lutadores da luta (só eles), passa o tempo ao motor e desenha o retrato |
| `CombatEngine.setSuperIntro` | No início do Super, quem ativou segura o 1º quadro pela entrada inteira; o rival e os projéteis esperam a entrada + o congelamento de sempre |
| `SuperFlash` | Na entrada: escuro, zoom leve, aviso e aura de carga; depois segue da pose sem pular |

O quadro do vídeo inteiro vira `--altura` da altura da tela (p15: 0,6 = 432 de 720), com o canto
de baixo do vídeo no canto de baixo da tela. Lutador à esquerda do rival: retrato na borda esquerda, como no vídeo. À direita:
espelhado, na borda direita. O retrato fica atrás dos lutadores e na frente do cenário escuro.

**Personagem sem pasta em `assets/supers/` não muda nada**: o Super dele continua igual.

## Pôr a entrada num personagem

Vídeo no verde, o lutador entrando pela **esquerda** e olhando para a direita, câmera parada.

```bash
pip install -r tools/sprites/requirements-animar.txt
python3 tools/sprites/super_video.py Entrada_Esquerda_Direita.mp4 --personagem p15 \
    --inicio 4 --fim 44 --passo 2 --fps 18 --marca 8,8,150,50 --altura 0.6
```

- `--inicio/--fim/--passo`: o trecho (o vídeo do Flick repete cada desenho duas vezes: `--passo 2`).
- `--fps`: velocidade no jogo (18 = 1,5x o vídeo). Quanto mais longo, mais tempo a luta para.
- `--segurar MS`: o último quadro parado antes de sair. `--saida MS`: o tempo da volta (150).
- `--marca x0,y0,x1,y1`: apaga a marca d'água clara (o cabelo escuro que passa por cima fica).
- `--altura`: o tamanho do retrato, em fração da altura da tela (1 = a tela toda; a p15 usa 0,6).
- `--resolucao`: px guardados por px da tela (0,65; o da p15 ocupa ~6 MB na memória durante a luta).

A prévia (quadros sobre a tela escura) sai em `android/app/build/sprite-review/supers/<id>.png`.

## p15 hoje

Quadros 4–44 do vídeo, um a cada dois (21 desenhos), a 18 fps, 60% da altura da tela, mais
150 ms de saída:
**~1,3 s de entrada** (80 passos) antes do Super sair.
