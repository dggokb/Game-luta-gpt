# A CPU que lê o jogador

CPU nível 8 contra a CPU nível 6 com um hábito forçado, 1x1, 1500 lutas por linha, mesmas sementes.
Colunas: vitórias da CPU 8 | dano feito ÷ levado | antiaéreos por minuto | agarrões por minuto | acertos logo depois de ele levantar, por minuto.

| hábito do jogador | leitura | vitórias | dano feito ÷ levado | antiaéreos/min | agarrões/min | acertos no levantar/min |
|---|---|---|---|---|---|---|
| sem hábito forçado | desligada | 52% | 0.95 | 4.9 | 0.1 | 1.4 |
| sem hábito forçado | ligada | 54% | 0.95 | 4.7 | 0.1 | 1.4 |
| pula para frente sempre que sai da defesa | desligada | 64% | 1.25 | 9.3 | 0.0 | 2.9 |
| pula para frente sempre que sai da defesa | ligada | 62% | 1.21 | 9.3 | 0.0 | 2.8 |
| aperta L sempre que levanta | desligada | 77% | 1.66 | 3.3 | 0.0 | 1.5 |
| aperta L sempre que levanta | ligada | 75% | 1.58 | 3.5 | 0.1 | 1.4 |
| colado, só defende (não ataca) | desligada | 68% | 1.27 | 1.5 | 0.0 | 0.5 |
| colado, só defende (não ataca) | ligada | 69% | 1.33 | 1.6 | 0.2 | 0.5 |
| pula sempre que a bola vem | desligada | 75% | 1.60 | 5.7 | 0.0 | 1.7 |
| pula sempre que a bola vem | ligada | 75% | 1.59 | 5.7 | 0.1 | 1.6 |

## Memória entre lutas (5 lutas seguidas contra o mesmo jogador; hábito: pula depois de defender)

| luta | sem memória (cada luta do zero) | com memória (a leitura passa) |
|---|---|---|
| 1ª | 82% | 1.68 | 5.3 | 0.1 | 3.5 | 82% | 1.68 | 5.3 | 0.1 | 3.5 |
| 2ª | 78% | 1.69 | 4.8 | 0.1 | 3.8 | 80% | 1.74 | 5.0 | 0.2 | 3.8 |
| 3ª | 80% | 1.72 | 5.2 | 0.1 | 4.0 | 79% | 1.60 | 4.9 | 0.2 | 3.9 |
| 4ª | 84% | 1.69 | 5.3 | 0.1 | 3.5 | 81% | 1.68 | 5.0 | 0.1 | 3.7 |
| 5ª | 80% | 1.64 | 5.1 | 0.1 | 3.6 | 81% | 1.62 | 4.8 | 0.2 | 3.8 |
