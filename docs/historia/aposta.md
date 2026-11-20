# Bolsa de Apostas (a Semifinal)

Quem apresenta a Semifinal é **o Apostador** (concept: `art/concepts/apostador_concept.png`). Na
luta da Semifinal (`ep06`, passo de luta com `"aposta": true`) os apostadores põem dinheiro em
cada dupla, e o dinheiro vai para quem dá show. Decidido pelo dono em 20/11:

1. O dinheiro rende **barra de Super**.
2. **Quem foge ou só defende perde aposta.**
3. O Apostador é um personagem com rosto (os retratos na faixa do narrador).
4. O total **fica só na luta** (aparece na tela; não vai para a história).

Código: `android/app/src/main/java/com/gamelutagpt/BolsaApostas.java` (Java puro, com o desenho
da placa). Testes: `BolsaApostasTest` (app), `NarracaoTest` e `TorneioTest` (story-core).

## A moeda: o kon

O dinheiro do SOKON é o **kon** (de 魂, alma): o símbolo é um K anguloso cortado por dois traços,
com uma chama no braço de cima (escolhido pelo dono em 20/11 entre quatro propostas do Codex,
`art/hud/moeda/propostas_original.png`, a primeira). A fonte não tem o símbolo: no HUD ele é a
imagem `ui/hud/moeda/kon_<cor>.png`, desenhada antes do número na cor dele
(`python3 tools/hud/montar_moeda.py` gera as cores a partir de `art/hud/moeda/kon.png`). Nas
falas, o valor vem por extenso: "1.250.000 kon".

## Como funciona

| O que acontece | Quanto entra (antes da odd) | Para quem |
|---|---|---|
| Dano que entra | 40 kon por ponto de dano | quem bateu |
| Primeiro golpe da luta | + 80.000 kon | quem bateu |
| Contra-golpe | + 40.000 kon | quem bateu |
| Combo de 5+ / 10+ acertos | 15.000 kon / 30.000 kon por acerto | quem fez o combo |
| Defesa perfeita | 60.000 kon | quem defendeu |
| Guard break | 120.000 kon | quem quebrou |
| Combo breaker | 150.000 kon | quem quebrou |
| K.O. de um lutador | 300.000 kon | quem derrubou |
| Super | 200.000 kon | quem soltou |
| SHINKAI | 250.000 kon | quem ligou |
| Ultra / Ultra SHINKAI | 450.000 kon / 900.000 kon | quem soltou |

- **Abertura**: 500.000 kon em cada dupla, odds iguais.
- **Odds**: 60% pela vida das duplas e 40% pelo dinheiro de cada uma, com a margem da casa
  (`odd = 0,92 / favoritismo`, de x1.05 a x9.9). O ganho é multiplicado por `odd / 1,84`
  (entre 0,6 e 3): **a zebra ganha mais pelo mesmo golpe** e a virada rende muito.
- **Patamar**: a cada 750.000 kon acima do maior valor que a dupla já teve, o titular ganha
  **¼ de barra de Super** e aparece `+SUPER` do lado dela. O Apostador comenta.
- **Esfriar**: o titular que fica **3 s sem atacar** (parado, recuando ou só defendendo) esfria
  (o valor fica azul, `ESFRIANDO`); com **5 s** os apostadores tiram **6% do dinheiro por
  segundo** (`APOSTAS CAINDO ▼`) até ele voltar a atacar. Andar para cima do rival não esfria;
  apanhar (hitstun, chão) também não: aí quem ganha é quem bate. O que caiu precisa ser
  recuperado antes do próximo patamar.

Todos os números ficam no começo da `BolsaApostas` para afinar.

## Na tela

A placa de cima foi trocada (20/11, o dono achou poluída) pelo **letreiro no rodapé**, o concept
M1 escolhido por ele (`art/hud/apostas/concepts/concept_m1.png`):

- **A faixa** de TV na borda de baixo, de ponta a ponta, com `● AO VIVO  APOSTAS` à esquerda do
  placar (o ponto vermelho pisca).
- **O placar** sobe no meio da faixa, centrado na tela (o dono, 20/11: no vão entre os controles
  ficava torto): o dinheiro de cada dupla em dígitos grandes da cor do
  time (o valor sobe contando), a seta verde ▲ logo depois de ganhar e a vermelha ▼ enquanto a
  aposta cai, o **medalhão** com o rosto do Apostador (normal; rindo num ganho grande;
  gargalhando, com raios, no patamar e na virada), a **barra** azul e vermelha com a parte de cada
  um e as **odds** nas plaquinhas das pontas. Quem esfria fica com o valor azul-gelo; quem perde
  aposta pisca em vermelho.
- **A caixa do narrador** sobe 64 px (`Narracao.setDeslocamento`) e fica em cima do placar.
- **A moeda até a barra de Super** (o pedido do dono: "rastro quando vai pro Super, pra saber que
  tá aumentando lá"): no patamar, uma moeda de luz sai do valor da dupla e voa numa curva para
  fora até o meio da barra de Super dela no HUD de cima, com o **rastro de luz** dourado atrás (a
  textura do Codex esticada no caminho, a cabeça branca na moeda; o fim do rastro vem 14 quadros
  atrás e apaga de trás para a frente). Na chegada, o **estouro** de luz na barra, e **só então a
  barra sobe** (o HUD faz o brilho de ganho dele). A moeda anda no tempo da tela (0,6 s), não no
  da luta, que fica lento nos impactos; é desenhada por cima de tudo, até dos controles.
- **Telão da arena**: o letreiro `ODDS x1.6 | x2.4` embaixo da transmissão (some no replay).
- **O Apostador fala na faixa do narrador**, com o nome, a cor e o retrato dele
  (`historia/retratos/apostador_<normal|empolgado|gargalhada>.png`, recortados por
  `python3 tools/historia/retratos_apostador.py`). Eventos no `narrador.json` com
  `"quem": "apostador"`: `aposta_inicio`, `aposta_patamar` (`{nome}`, `{valor}`),
  `aposta_virada`, `aposta_esfriando`, `aposta_caindo`. O narrador no palco não mexe a boca
  nas falas do Apostador.

### A arte

Tudo gerado pelo Codex (prompts ao lado das imagens), com a barra de cima
(`art/hud/barra-raios-original.png`, copiada como `estilo.png`) de referência:

- `art/hud/apostas/letreiro_original.png` (`prompt_letreiro.txt`, o M1 vazio, fundo verde, o
  medalhão e a barra em magenta);
- `art/vfx/aposta/rastro.png`, `chegada.png`, `moeda_luz.png` (`prompt_*.txt`, luz em fundo
  preto, apontando para a direita).

```
cd art/hud/apostas
cp ../barra-raios-original.png estilo.png && cp concepts/concept_m1.png alvo_m1.png
codex exec --skip-git-repo-check --sandbox workspace-write -i alvo_m1.png -i estilo.png - < prompt_letreiro.txt
```

(o Codex guarda a imagem em `~/.codex/generated_images/`.) Depois
`python3 tools/hud/montar_apostas.py` separa a faixa e o placar, tira o verde e o magenta, mede o
hexágono, a barra e as placas (`letreiro.json`) e tira o preto das artes do efeito pelo brilho.
Sem a arte, o placar sai no desenho simples.

## A CPU

- `AiController.setAposta(pressa, show)`: a **pressa** sobe quando ela esfria e vira a mesma
  pressa do relógio no fim (ataca mais, recua e espera menos), para ela não perder aposta; o
  **show** (o multiplicador da odd dela) multiplica a chance do Ultra e do SHINKAI no neutro:
  a CPU zebra arrisca mais o espetáculo.
- O patamar paga a barra do titular da CPU do mesmo jeito.

## Conferir no PC

```
gradle :pc:gravarLuta -Paposta -Proteiro="tecla:F3 espera:4 foto:bolsa grava:240:luta fim"
```

`-Paposta` liga a Bolsa em qualquer batalha do Modo Teste (`-Dgameluta.aposta=true`).

Vídeo da Semifinal de verdade (o passo de luta do ep06, quartas já vencidas, a CPU jogando dos
dois lados, em português) direto em MP4:

```
gradle :pc:gravarSemifinal [-Pirmao=koga] [-Psegundos=80] [-Psaida=arquivo.mp4] [-PcontroleNaTela]
```

Sai em `android/pc/build/semifinal.mp4`. A CPU joga pelo jogador pelo `GameView.cpuNoJogador`
(null no jogo). Os QTE da cinemática da luta ficam sem resposta.
