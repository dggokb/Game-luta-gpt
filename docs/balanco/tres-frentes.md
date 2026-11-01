# Três frentes: o arcade, o pulo e a meia-lua (pedido do jogador, 01/11)

**O que o jogador relatou:**
- Zerou o arcade só dando meia-lua e não sentiu a dificuldade subir.
- O pulo parece inútil: se tirar, dá no mesmo.
- Pediu para simular muito, olhar o log das lutas e não só as vitórias, e manter ou tirar cada
  mudança pelo resultado.

**Como foi medido:** `BateriaLog`. O Ryūga roteirizado enfrenta a CPU do arcade nos níveis 5, 6, 7 e
8, contra os 16 rivais, e o log registra o que aconteceu em cada bola, pulo, combo e Super. Há três
jogadores roteirizados:
- **meialua:** solta a Onda sempre que pode, defende baixo entre uma e outra e tira do ar com o 2H.
- **pulador:** entra pulando com jH e, se acerta, emenda L, M e H ao cair.
- **cpu:** a CPU nível 8 no lugar do jogador, como referência de quem joga bem.

As tabelas completas estão em [perfis/log-antes.md](perfis/log-antes.md) e
[perfis/log-depois.md](perfis/log-depois.md). Há também lutas narradas, com o modo
`narra:jogador:rival:nível`.

## O que o log mostrou

1. **O arcade sobe de nível, sim: do 5 na primeira luta ao 8 na última** (`NivelCpu`). Mas o
   zoneador ganha em todos: vencia 100% no nível 5 e 63% no 8. Por isso não dá para sentir a
   diferença.
2. **O pulo funciona na mecânica, mas rende pouco contra a CPU:**
   - O jH que acerta emenda o L ao cair, mesmo acertando até 21 quadros antes de pousar.
   - No nível 8, a CPU defende 67% dos pulos e tira do ar 14%. Só 15% acertam.
   - O pulo defendido não dá vantagem nenhuma.
   - A própria CPU pula pouco contra quem não zoneia (até 1,8 por minuto contra o pulador) e o
     super pulo no neutro é raro.
3. **Contra o zoneador, a CPU perdia de três jeitos:**
   - **Pulava antes da bola e caía no 2H:** 20 de 37 pulos em 8 lutas. O aprendizado não cortava
     esse plano porque chegar perto contava ponto mesmo apanhando.
   - **Chegava perto e não fazia nada:** absorvia a bola ou atravessava na corrida de energia e
     depois só cutucava da ponta.
   - **Levava bola andando no neutro.**

## O que mudou (e ficou, pelo resultado)

- **A nota do aprendizado:** chegar perto só conta se chegou sem apanhar.
- **Colada em quem zoneia, a CPU também pode:**
  - **pressionar:** o 2L ou o L que abre o combo;
  - **agarrar:** quem só defende.

  Isso entra no que ela aprende, junto com andar, esperar, pular e correr.
- **A corrida de energia que chega colada já emenda o 2L.**

| Medida | Antes | Depois |
|---|---|---|
| Zoneador (log) vence: nível 5 / 6 / 7 / 8 | 100% / 91% / 75% / 63% | 84% / 84% / 69% / 66% |
| CPU contra o spammer da `BateriaBola` (60 lutas): nível 5 / 8 | 19% / 60% | 23% / 69% |
| CPU contra o zoneador: aberturas de 1 acerto só, nível 5 | 72% | 54% |
| Pulador e CPU × CPU | — | sem piora |

O zoneador ainda vence a maior parte das lutas. Os próximos passos estão no fim.

## Próximos passos (cada um medido e mantido só se melhorar)

1. **A bola de perto ainda é o golpe mais forte do jogo.** Ela tira 850 (6% da vida) e acerta 44%
   das vezes no nível 8, mesmo com 12 quadros de início. Medir punir a bola defendida de perto
   (ela fica −10 ou pior) e o dano da bola de perto.
2. **O pulo defendido não dá vantagem.** Medir dar mais tempo de defesa ao golpe no ar defendido
   (pressão ao cair) e a CPU entrando pulando quando o rival fica parado.
3. **Os níveis 5 e 8 se parecem demais.** Medir o que muda de um para o outro (confirmar o combo,
   punir, agarrar) e alargar a diferença.

## O pulo (pedido do jogador: "faz os três e simula")

**Medido antes:** o golpe no ar quase nunca encostava em quem está no chão. A janela vem de
apertar o golpe em vários quadros do pulo contra um rival parado, de várias distâncias:

| Golpe | Antes | Depois |
|---|---|---|
| Ryūga jH | só entre os quadros 26 e 30 do pulo | do 22 ao 30 |
| Ryūga jM | do 26 ao 30 | do 18 ao 30 |
| Ryūga jL | do 30 ao 34 | do 22 ao 34 |
| p14 jH | do 22 ao 26 | do 18 ao 26 |
| Kōga jH | do 6 ao 30 (já era bom) | igual |

O membro desenhado do chute fica na altura do corpo, e o pulo sobe 262 px, mais que o rival
(241). No Ryūga, o chute só alcançava o rival já perto de cair.

**As três mudanças e o que ficou:**
1. **Ficou.** O golpe no ar contra quem está no chão e bem na frente alcança 70 px abaixo do
   desenho (`jumpInReachDown`). Com o rival quase embaixo (o crossup) fica o desenho de sempre.
2. **Ficou.** O golpe no ar defendido por quem está no chão deixa 6 quadros a mais de defesa
   (`jumpInBlockstunBonus`): quem pulou cai na frente para começar a pressão.
3. **Saiu.** Com a CPU entrando pulando (a 200–310 px, rival parado ou se recuperando), o número
   de pulos dela quase não mudou, nem com o triplo da chance (5,7 contra 4,9 por minuto no nível 5
   e igual nos outros). Os resultados também ficaram iguais.

**Resultado** (`BateriaLog`, 48 lutas por nível; tabelas em [perfis/log-pulo.md](perfis/log-pulo.md)):

| Pulador (Ryūga) contra a CPU, níveis 5 / 6 / 7 / 8 | Antes (32 lutas) | Depois (48 lutas) |
|---|---|---|
| Pulador vence | 44 / 47 / 41 / 28% | 58 / 60 / 56 / 50% |
| Pulo que acerta | 27 / 24 / 18 / 15% | 42 / 40 / 27 / 20% |
| Pulo que acertou e virou combo | 36 / 31 / 34 / 42% | 46 / 43 / 50 / 54% |

O zoneador e a CPU × CPU ficaram dentro do ruído. O crossup (`CrossUpTest`) continua igual.

**O que ainda falta:**
- A CPU quase não pula: 5 a 6 vezes por minuto, e cerca de 75% desses pulos não fazem nada.
- No nível 8 ela defende 63% dos pulos e tira do ar só 14%. Um jogador que só pula vence metade
  das lutas contra o nível 8.
- O próximo passo é a CPU tirar do ar com mais vontade e pular com propósito, pelo que ela aprende.
