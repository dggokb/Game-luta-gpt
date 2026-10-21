# Efeitos de vídeo

Os efeitos de luta (impacto, fumaça, onda de chão, arco de golpe) vêm de vídeo, como os
personagens: o efeito é gerado sozinho no fundo verde, o `tools/sprites/efeito_video.py`
converte numa folha e o jogo toca a folha no lugar certo. Enquanto um efeito não existe, o jogo
usa o efeito clássico dele, então cada vídeo novo entra sozinho.

## Como gerar cada vídeo

- **Só o efeito**, sem personagem, sem chão e sem sombra. Fundo **verde chapado** (#00FF00),
  câmera parada, sem zoom nem tremida.
- O vídeo **começa vazio e termina vazio** (o efeito nasce e some dentro dele).
- 1 a 2 segundos, 24 fps, 1280×720 ou maior.
- O efeito **no meio do quadro**, grande (ocupando uns 60% da altura), sem sair da tela.
- Estilo dos vídeos de referência: branco com sombra cinza e contorno fino escuro, traço de
  anime desenhado à mão; nada de brilho neon nem cor (a cor do lutador pode vir depois).
- **Um efeito por vídeo.** Golpes e corridas sempre **para a direita** (o jogo espelha).

Exemplo de pedido (troque a descrição do efeito):

> Efeito visual 2D de anime para jogo de luta, desenhado à mão, branco com sombreado cinza e
> contorno fino escuro. [EFEITO]. Fundo verde chapado #00FF00, sem personagem, sem chão, sem
> sombra, câmera parada, efeito centralizado. O vídeo começa vazio e termina vazio.

## Lista de efeitos

| Nome (pasta) | O que é | Âncora | Altura (mundo) | [EFEITO] no pedido |
|---|---|---|---|---|
| `impacto_fraco` | soco/chute fraco que acerta | centro | 90 | uma estrela de impacto pequena e rápida que abre e some |
| `impacto_medio` | golpe médio | centro | 140 | uma estrela de impacto serrilhada que abre, fica vazada no meio e some, com uns cacos voando para a direita |
| `impacto_forte` | golpe forte | centro | 220 | uma explosão de impacto com espinhos longos saindo do centro para todo lado, que se quebra em cacos e gotas voando |
| `impacto_especial` | especial, Super, KO | centro | 300 | uma explosão de impacto enorme com espinhos, anel de choque, brilhos de 4 pontas e cacos voando para todo lado |
| `defesa` | golpe defendido | centro | 90 | uma faísca de bloqueio: um anel curto com riscos, rápido |
| `fumaca_pouso` | aterrissagem | chão | 80 | fumaça de desenho animado saindo dos dois lados de um ponto no chão, em bolhas com caracol, que encolhe e some |
| `fumaca_pulo` | saindo do chão no pulo | chão | 60 | um sopro de fumaça pequeno no chão, para os dois lados, rápido |
| `poeira_corrida` | largada do dash/backdash | chão | 90 | poeira e onda de chão serrilhada espirrando para a esquerda, como um pé arrancando para a direita |
| `poeira_freada` | freada do dash | chão | 70 | poeira baixa arrastada no chão para a direita, como um pé freando |
| `impacto_chao` | corpo caindo (knockdown) | chão | 160 | o impacto de um corpo batendo no chão: onda serrilhada para os dois lados, fumaça e pedrinhas |
| `super_pulo` | saída do super pulo | chão | 260 | um impulso para cima: onda de chão grande para os dois lados, arcos de vento subindo e um anel de choque achatado no chão |
| `arco_golpe` | golpe forte (o caminho do punho/pé) | centro | 200 | um arco branco em forma de meia-lua, de baixo para cima e para a direita, como o rastro de um gancho, rápido |

Âncora **centro**: o meio do vídeo é o ponto do golpe (para o `arco_golpe`, o ombro de quem
bate). Âncora **chão**: o meio de baixo do efeito fica no chão, embaixo do lutador.

## Converter

```bash
pip install -r tools/sprites/requirements-animar.txt
python3 tools/sprites/efeito_video.py impacto.mp4 --nome impacto_forte --ancora centro --altura 220
python3 tools/sprites/efeito_video.py fumaca.mp4 --nome fumaca_pouso --ancora chao --altura 80 --passo 2
```

- `--inicio`/`--fim`: o trecho do vídeo (quadros), quando sobra começo ou fim.
- `--passo 2`: metade dos quadros (12 fps); menos memória e o traço "de anime".
- `--recorte x0,y0,x1,y1`: só um pedaço do vídeo.
- `--origem x,y`: o ponto exato (pixels do vídeo) em vez da âncora.

Sai em `android/app/src/main/assets/efeitos/<nome>/` (`folha.webp` e `efeito.json`); a prévia
em GIF, com a origem marcada em vermelho, em `android/app/build/sprite-review/efeitos/`.

**A chave do verde é por diferença de cor:** o branco a 50% sobre o verde volta a ser branco a
50% (a fumaça e o brilho ficam meio transparentes, sem borda verde). Manchinhas de ruído saem.

## No jogo

`EfeitosVideo` lê as folhas junto com a arte da luta e o `GameView` toca:

| Evento | Efeito | Sem o efeito |
|---|---|---|
| Golpe acerta | `impacto_<tamanho>` (sem o do tamanho, o de baixo um pouco maior) | faísca clássica |
| Golpe defendido | `defesa` | faísca de defesa |
| Aterrissagem (motor de animação) | `fumaca_pouso` | sopros/poeira |
| Pulo saindo do chão | `fumaca_pulo` | nada |
| Largada / freada do dash (motor) | `poeira_corrida` / `poeira_freada` | pé deslizando |
| Knockdown | `impacto_chao` | poeira |
| Super pulo | `super_pulo` | anel e coluna de luz |
| Golpe forte fica ativo | `arco_golpe` no ombro | nada |

Efeitos de golpe e de corrida para a esquerda são espelhados.
