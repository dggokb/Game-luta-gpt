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
- Estilo: efeito 2D de anime desenhado à mão, colorido, em camadas, com sombreado de anime
  (base, sombra e brilho) e contorno escuro de pincel. Acerto em branco/amarelo/laranja;
  fumaça e poeira em cinza-claro com sombra lilás. **Nada verde** no efeito (some com o fundo).
  (Antes era só branco e cinza; a cor foi liberada em 01/11.)
- **Um efeito por vídeo.** Golpes e corridas sempre **para a direita** (o jogo espelha).

Exemplo de pedido (troque a descrição do efeito):

> Efeito visual 2D de anime para jogo de luta, desenhado à mão, colorido, com sombreado de
> anime em camadas e contorno escuro de pincel, sem nada verde. [EFEITO]. Fundo verde chapado #00FF00, sem personagem, sem chão, sem
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

## Defesa: o efeito tirado de um vídeo com personagem

Três vídeos do personagem no verde (o efeito branco por cima dele) viraram as folhas de defesa.
`tools/sprites/efeito_sem_personagem.py` fica só com o efeito: o pixel claro e pouco saturado
que encosta no fundo verde (o clarão e os riscos sempre tocam o fundo; o branco do olho fica
preso no rosto e sai), sem tom de pele/faixa vista através do clarão e sem os tênis. Depois o
`efeito_video.py` lê a pasta de PNGs. Escala: o personagem de pé mede ~550 px no vídeo e 247 no
jogo (0,45 do mundo por pixel).

| Folha | Vídeo | Quadros | No jogo |
|---|---|---|---|
| `defesa` | Defesa_de_Golpe_Invisível (quadros 12-17; espelhado: no vídeo o golpe vem da direita) | 6 | defesa comum, alternando com a `defesa_2` |
| `defesa_2` | Defesa_de_Golpe_Invisível_Anime (27-29 e o 29 sumindo em mais 2; espelhado; some suave a 360 px do centro) | 5 | defesa comum, alternando com a `defesa` |
| `defesa_poder` | Inicio_Centro_-_Chute_Lateral_Anime (o clarão do chute, 41-42, e o 42 sumindo em mais 2; some a 420 px) | 4 | defesa de projétil ou especial |

```bash
ffmpeg -i defesa.mp4 quadros/%03d.png
python3 tools/sprites/efeito_sem_personagem.py . quadros saida --quadros 11-18 --espelhar
python3 tools/sprites/efeito_video.py saida --nome defesa --origem 408,341 --altura 287 --fps 24
```

`GameView.efeitoDeGolpe`: golpe defendido de projétil ou especial toca a `defesa_poder`; a
defesa comum alterna `defesa_2` e `defesa`. Sem a pedida, cai na seguinte; sem nenhuma, a faísca
clássica.

## Efeitos desenhados em código (anidoodle)

**Tirados do jogo (02/11):** testados no celular e não ficaram bons; as 12 folhas saíram de
`assets/efeitos/` e o jogo voltou aos efeitos clássicos. O código continua em
`tools/sprites/efeitos_codigo/` e o conversor segue aceitando pasta de PNGs; para voltar um
efeito, basta gerar a folha de novo (comandos abaixo).

Outro jeito de fazer o efeito: desenhar em código com o
[anidoodle](https://github.com/alexgreensh/anidoodle) (Apache-2.0). O mesmo código gera sempre os
mesmos quadros e dá para ajustar um detalhe sem mudar o resto. O código de cada efeito fica em
`tools/sprites/efeitos_codigo/<nome>.ts`.

Os quadros saem em **PNG transparente** (sem o verde) e o `efeito_video.py` lê a pasta direto: o
brilho meio transparente guarda a cor (no verde, o amarelo a 30% perdia o tom na chave).

Estilo (do estudo em `docs/efeitos-referencia-fighterz.md`): **efeito é luz, sem contorno** (o
contorno é dos personagens), centrado no ponto do golpe, sem fumaça no acerto.

| Efeito | Código | Quadros |
|---|---|---|
| `impacto_fraco` | `impacto_fraco.ts` (família em `impacto_kit.ts`): estrela pequena com dois raios de luz, estrela redonda, poucas línguas de fogo | 6 (~0,25 s) |
| `impacto_medio` | `impacto_medio.ts`: mais o X de espinhos e três raios de luz | 8 (~0,33 s) |
| `impacto_forte` | `impacto_forte.ts`: estrela de espinhos com raios de luz atravessando e faíscas miúdas; X de espinhos; estrela redonda com brilho; o miolo abre e a borda vira línguas de fogo que voam e somem | 9 (~0,37 s) |
| `impacto_especial` | `impacto_especial.ts`: o forte maior, com agulhas vermelhas em leque, raios elétricos em zigue-zague e mais línguas e faíscas | 11 (~0,46 s) |
| `defesa` | `defesa.ts`: luz fria (azul) para não confundir com o acerto: meia-lua de barreira virada para quem bate, anel fino e faíscas voltando | 6 (~0,25 s) |
| `arco_golpe` | `arco_golpe.ts`: rastro branco em meia-lua em volta do ombro, de baixo para cima e para a frente, grosso na frente e fino na cauda | 4 (~0,17 s) |
| `fumaca_pouso` | `fumaca_pouso.ts`: nuvem larga e baixa (cerca de 4× mais larga que alta) de bolhas grandes em duas fileiras, cinza médio com sombra embaixo, luz só na borda de cima, contorno fino cinza-escuro e poucos caracóis; quatro lâminas de ar largas e opacas cruzando em dois X por trás. Some baixando, encolhendo e ficando transparente | 10 (~0,42 s) |
| `fumaca_pulo` | `fumaca_pulo.ts` (poeira em `poeira_kit.ts`): sopro de bolhas para os dois lados e dois cortes de ar curtos em V | 6 (~0,25 s) |
| `poeira_corrida` | `poeira_corrida.ts`: bolhas-vírgula, onda serrilhada, pedrinhas e fita de vento espirrando para trás do pé | 8 (~0,33 s) |
| `poeira_freada` | `poeira_freada.ts`: bolhas baixas arrastadas para a frente, riscos de arrasto e pedrinhas rolando | 8 (~0,33 s) |
| `impacto_chao` | `impacto_chao.ts`: clarão baixo, estilhaços de pedra em dois tons sem contorno, pedras escuras, brasas e poeira rolando para os lados | 10 (~0,42 s) |
| `super_pulo` | `super_pulo.ts`: lâminas de ar altas, anel de choque achatado no chão, fitas de vento em espiral e poeira baixa | 10 (~0,42 s) |

Todos a 24 fps. **Escala:** a poeira usa a mesma escala em todos (0,563 do mundo por pixel do
desenho; a nuvem do pouso fica com ~70 para um lutador de ~247). A `poeira_corrida` e a
`poeira_freada` usam `--origem` no pé (900,690 e 380,690), porque o pé não é o meio do efeito.

```bash
git clone https://github.com/alexgreensh/anidoodle /tmp/anidoodle
node /tmp/anidoodle/skills/anidoodle/engine/tools/scaffold.mjs /tmp/fx --film impacto_forte --size 1280x960 --fps 24
cp tools/sprites/efeitos_codigo/*.ts /tmp/fx/src/canvas-core/
(cd /tmp/fx && npm install)
# um efeito: os quadros em PNG transparente (--frames 0,1,...,N-1) e depois o conversor
(cd /tmp/fx && node tools/still.mjs impacto_forte --frames 0,1,2,3,4,5,6,7,8 --out out/impacto_forte/)
python3 tools/sprites/efeito_video.py /tmp/fx/out/impacto_forte --nome impacto_forte --ancora centro --altura 243 --fps 24
```

`--altura` de cada um (a altura do efeito inteiro, com raios e faíscas): `impacto_fraco` 132,
`impacto_medio` 135, `impacto_forte` 243, `impacto_especial` 237, `defesa` 236, `arco_golpe` 226
(todos `--ancora centro`); `fumaca_pouso` 218, `fumaca_pulo` 76, `impacto_chao` 144, `super_pulo`
215 (`--ancora chao`); `poeira_corrida` 92 (`--origem 900,690`), `poeira_freada` 41
(`--origem 380,690`).

## Efeitos do manequim (dano e defesa)

Outro jeito de fazer o efeito: um manequim verde sobre o fundo verde levando o golpe. O manequim
dá a escala e o ponto do corpo (o efeito encaixa no corpo de quem apanha), e o recorte tira o
manequim junto com o fundo (`tools/sprites/efeitos_manequim.py`): a placa é a mediana dos
quadros sem efeito, e o verde que sobra em cada pixel diz o quanto ela aparece por baixo.

```bash
python3 tools/sprites/efeitos_manequim.py cabeca.mp4 --nome dano_cabeca --quadros 80-104 --placa 30-75 --ponto cabeca --espelhar --resolucao 1
python3 tools/sprites/efeitos_manequim.py tronco.mp4 --nome dano_tronco --quadros 0-44 --placa 70-120 --ponto tronco --espelhar --resolucao 0.5
python3 tools/sprites/efeitos_manequim.py defesa.mp4 --nome defesa --quadros 26-46 --placa 50-70 --ponto 850,217 --espelhar --resolucao 1
```

- `--ponto cabeca|tronco|baixo` (medidos no manequim) ou `x,y` do vídeo (o centro da estrela da defesa).
- `--espelhar`: o vídeo é de golpe para a esquerda (o spray sai para trás de quem olha para a direita).
- `--resolucao`: px da folha por px do mundo (o spray do tronco é enorme; 0,5 deixa a folha leve).
- `--escuro 0` para fumaça escura (sem ele, o que sai escuro é tratado como o contorno do manequim).

Para o vídeo: fundo verde liso, manequim verde sem contorno preto, câmera parada, um golpe por
vídeo começando sem efeito, o efeito dentro do quadro (o que passa da borda é esfumado) e nada
verde no efeito (some junto com o manequim).

Testados (28/10) e tirados do jogo: ficaram feios e derrubaram o FPS (folhas grandes, efeitos
longos de 1 a 2 s cobrindo meia tela). O código continua: o efeito só entra se a folha existir.
Para voltar, o efeito precisa ser curto (0,3 a 0,5 s), pequeno no quadro e com poucos quadros.

| Efeito | Vídeo | O que era |
|---|---|---|
| `dano_cabeca` | Mannequin_Air_Damage_Effect (2º golpe) | estouro e anel de choque saindo de trás da cabeça |
| `dano_tronco` | Spray_no_Tronco | spray de faíscas saindo pelas costas |
| `dano_baixo` | (não houve) | — |
| `defesa` | Defesa_com_Impacto | a estrela de impacto nos antebraços cruzados e as faíscas |

## No jogo

`EfeitosVideo` lê as folhas junto com a arte da luta e o `GameView` toca:

| Evento | Efeito | Sem o efeito |
|---|---|---|
| Golpe acerta | pelo ponto do corpo: `dano_cabeca` (golpe acima de 72% da altura de quem apanha), `dano_baixo` (agachado ou abaixo de 35%: a rasteira) ou `dano_tronco`, na mesma parte do corpo dele, do tamanho dele e menor que o vídeo (50% a 80% pela força) | `impacto_<tamanho>` |
| Golpe acerta (sem os do manequim) | `impacto_<tamanho>` (sem o do tamanho, o de baixo um pouco maior) | faísca clássica |
| Golpe defendido | `defesa` | faísca de defesa |
| Aterrissagem (motor de animação) | `fumaca_pouso` | sopros/poeira |
| Pulo saindo do chão | `fumaca_pulo` | nada |
| Largada / freada do dash (motor) | `poeira_corrida` / `poeira_freada` | pé deslizando |
| Knockdown | `impacto_chao` | poeira |
| Super pulo | `super_pulo` | anel e coluna de luz |
| Golpe forte fica ativo | `arco_golpe` no ombro | nada |

Efeitos de golpe e de corrida para a esquerda são espelhados.
