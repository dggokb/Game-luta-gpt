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

## Como funciona

| O que acontece | Quanto entra (antes da odd) | Para quem |
|---|---|---|
| Dano que entra | ¥ 40 por ponto de dano | quem bateu |
| Primeiro golpe da luta | + ¥ 80.000 | quem bateu |
| Contra-golpe | + ¥ 40.000 | quem bateu |
| Combo de 5+ / 10+ acertos | ¥ 15.000 / ¥ 30.000 por acerto | quem fez o combo |
| Defesa perfeita | ¥ 60.000 | quem defendeu |
| Guard break | ¥ 120.000 | quem quebrou |
| Combo breaker | ¥ 150.000 | quem quebrou |
| K.O. de um lutador | ¥ 300.000 | quem derrubou |
| Super | ¥ 200.000 | quem soltou |
| SHINKAI | ¥ 250.000 | quem ligou |
| Ultra / Ultra SHINKAI | ¥ 450.000 / ¥ 900.000 | quem soltou |

- **Abertura**: ¥ 500.000 em cada dupla, odds iguais.
- **Odds**: 60% pela vida das duplas e 40% pelo dinheiro de cada uma, com a margem da casa
  (`odd = 0,92 / favoritismo`, de x1.05 a x9.9). O ganho é multiplicado por `odd / 1,84`
  (entre 0,6 e 3): **a zebra ganha mais pelo mesmo golpe** e a virada rende muito.
- **Patamar**: a cada ¥ 750.000 acima do maior valor que a dupla já teve, o titular ganha
  **¼ de barra de Super** e aparece `+SUPER` do lado dela. O Apostador comenta.
- **Esfriar**: o titular que fica **3 s sem atacar** (parado, recuando ou só defendendo) esfria
  (o valor fica azul, `ESFRIANDO`); com **5 s** os apostadores tiram **6% do dinheiro por
  segundo** (`APOSTAS CAINDO ▼`) até ele voltar a atacar. Andar para cima do rival não esfria;
  apanhar (hitstun, chão) também não: aí quem ganha é quem bate. O que caiu precisa ser
  recuperado antes do próximo patamar.

Todos os números ficam no começo da `BolsaApostas` para afinar.

## Na tela

- **A placa** (embaixo do MENU, 500 de largura): arte gerada pelo Codex no estilo da barra de
  cima (`art/hud/apostas/placa_original.png`, prompt em `prompt_placa.txt`). No meio, o
  **medalhão hexagonal dourado com o rosto do Apostador**: sorriso de canto no normal, rindo
  num ganho grande e gargalhando (tremendo, raios dourados atrás) no patamar e na virada das
  odds. De cada lado: o dinheiro na placa, a odd na plaquinha de baixo, o **medidor** (enche da
  ponta de fora com a parte da bolsa, listras de ficha andando) e a **pilha de fichas** (de 2 a 6
  fichas, pela parte da bolsa).
- **Ganho**: fichas voam do medalhão para a pilha de quem ganhou, e o `+¥` sobe embaixo.
- **Patamar**: brilho na pilha, uma moeda de iene voa até a barra de Super do HUD e `+SUPER`.
- **Esfriando**: gelo na placa do dinheiro, medidor azul-gelo e `ESFRIANDO`; **caindo**: medidor e
  valor piscando em vermelho, fichas caindo da pilha e `APOSTAS CAINDO ▼`.
- **Telão da arena**: o letreiro `ODDS x1.6 | x2.4` embaixo da transmissão (some no replay).
- **O Apostador fala na faixa do narrador**, com o nome, a cor e o retrato dele
  (`historia/retratos/apostador_<normal|empolgado|gargalhada>.png`, recortados por
  `python3 tools/historia/retratos_apostador.py`). Eventos no `narrador.json` com
  `"quem": "apostador"`: `aposta_inicio`, `aposta_patamar` (`{nome}`, `{valor}`),
  `aposta_virada`, `aposta_esfriando`, `aposta_caindo`. O narrador no palco não mexe a boca
  nas falas do Apostador.

### A arte

As imagens do Codex ficam em `art/hud/apostas/` (`placa_original.png`, `fichas.png` e os
prompts). A barra de cima vai junto como referência de estilo. Gerar de novo:

```
cd art/hud/apostas
cp ../barra-raios-original.png estilo.png
codex exec --skip-git-repo-check --sandbox workspace-write -i estilo.png - < prompt_placa.txt
```

(o Codex salva a imagem grande em `~/.codex/generated_images/`: use essa como
`placa_original.png`). Depois `python3 tools/hud/montar_apostas.py` tira o verde e o magenta,
recorta as fichas e grava `assets/ui/hud/apostas/` com a `placa.json` (o hexágono, os medidores
e as placas, medidos pela cor). Sem a arte, a placa sai no desenho simples.

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
