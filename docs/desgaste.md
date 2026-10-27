# Desgaste da luta (battle damage)

O lutador vai se desgastando conforme a luta rola, **sem refazer sprite**: tudo é desenhado por
cima do quadro que já existe. Código: `DesgasteDaLuta` (estágios, partículas), `GameView`
(`drawFighter` e `drawDirt`) e `SpriteFighterRenderer.corDaRoupa`. Teste: `DesgasteDaLutaTest`.
Só apresentação: nada aqui muda a luta.

## Estágios

Pela vida de quem está em campo (cada um da dupla tem a sua):

| Estágio | Vida | O que muda no desenho |
|---|---|---|
| 0 | acima de 66% | nada |
| 1 | abaixo de 66% | um pouco mais apagado e puxado para a terra, com poeira nas pernas |
| 2 | abaixo de 33% | mais apagado, mais poeira subindo; começa a suar |
| 3 | abaixo de 10% | o mais gasto; **exausto**: respira ofegante (o corpo infla e esvazia), sai bafo da boca e ele sua |

- A cor apagada é um filtro de cor no próprio sprite (`DesgasteDaLuta.tinta`).
- A poeira nas pernas é o mesmo quadro desenhado de novo por cima, tingido de terra e recortado
  em duas faixas a partir dos pés (`GameView.drawDirt`). Ela acompanha a animação e a rotação do
  corpo. Não entra no reflexo do chão.
- **Troca de estágio:** estoura poeira e algumas farpas de roupa, para o jogador perceber.
  A troca de dupla não estoura nada.

## Golpes

- Golpe forte (H, Super, Ultra ou dano a partir de 900) que entra rasga **farpas da roupa**, na
  cor do próprio lutador. A cor é medida uma vez por personagem no tronco do quadro parado (a cor
  que mais aparece, sem pele nem o contorno preto).
- Em quem já está no estágio 2 ou 3, todo golpe que entra espirra **suor**.

## Sangue

Existe, mas está **desligado** (`DesgasteDaLuta.SANGUE = false`): ficou pesado demais para o tom
do jogo. Ligado, os golpes soltam gotas (mais no forte) que caem e mancham o chão, a troca de
estágio solta um pouco e no vermelho ele pinga.
