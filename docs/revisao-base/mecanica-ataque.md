# Mecânica de ataque: especificação fechada

O que o motor faz quando um golpe sai, acerta ou é defendido, e o que isso pede das animações de reação. Serve para gravar os vídeos de reação uma vez só: mudanças futuras nos números (dano, alcance, tempo de atordoamento) **não** pedem vídeo novo, porque o motor estica o clipe de reação para o tempo que o golpe dá. O que pediria vídeo novo é mudar a **lista de reações** ou **a regra que escolhe a reação**, e essas duas ficam fechadas aqui.

Base de cada regra: [direcao.md](direcao.md) (guia com fontes).

## 1. Regras comuns (todos os personagens)

| Eixo | Fraco (L) | Médio (M) | Forte (H) | Fonte |
|---|---|---|---|---|
| Pausa no acerto (os dois congelam) | 8 quadros | 11 | 16 | DBFZ (Dustloop), regra I1 |
| Pausa na defesa | 8 | 10 | 12 | GG: defesa = pausa do golpe (I3); sem o extra do acerto |
| Tremor de quem apanha | ~2 px | ~3 px | ~5 px, decaindo até 0 | Sakurai (I8–I10) |
| Faísca (um desenho só) | 1,0× | 1,3× | 1,6× | Coluna de efeitos do SF6 (E1–E7) |
| Reação em pé (abaixo) | rosto ou tronco | rosto ou tronco | forte (empurrado) | DBFZ cabeça/corpo + reação forte do SF |

A 60 quadros por segundo. Os números de cada golpe (dano, tempo de atordoamento, empurrão) ficam no `character.json` de cada personagem; a pausa base é 8/10/12 em todo o elenco, e o golpe alto (6M) é médio como os outros.

## 2. As reações (a lista fechada)

Cada estado é uma animação do personagem (`SpriteStates`). Sem o clipe próprio, o jogo usa a reação genérica entre parênteses; por isso dá para gravar uma de cada vez.

| Estado | Quando | Pose | Sem o clipe |
|---|---|---|---|
| `HIT_HIGH` | Em pé, golpe fraco ou médio que pega acima de 72% da altura de quem apanha | Cabeça joga para trás, queixo para cima, olhos fechados | `HIT_STAND` (Ryūga: já tem) |
| `HIT_BODY` | Em pé, golpe fraco ou médio que pega abaixo disso | Dobra o tronco, braços sobre a barriga | `HIT_STAND` (Ryūga: já tem) |
| `HIT_HEAVY` | Em pé, golpe forte, especial, Super ou Ultra | O corpo inteiro vai para trás em arco, um pé sai do chão | `HIT_STAND` (Ryūga: já tem) |
| `HIT_CROUCH` | Agachado, qualquer golpe | Encolhe agachado | (já existe) |
| `HIT_AIR` | No ar, lançado, quique no chão ou na parede | Corpo arqueado para trás no ar | (já existe) |
| `KNOCKDOWN` / `GROUNDED` / `GETUP` | Queda (rasteira, golpe que derruba, fim de combo no ar) | Cai de costas, fica no chão, levanta | (já existem) |
| `DEFENSE_STAND` / `DEFENSE_CROUCH` / `DEFENSE_AIR` | Defesa | Braços cruzados na frente | (já existem) |

**Duração:** o clipe é esticado sobre o atordoamento do golpe. O vídeo só precisa de começar na pose de dor (ela aparece no 1º quadro da pausa, regra I11), segurar a pose e voltar à guarda no fim. Tempo de cada reação no jogo (pausa + atordoamento), para referência: fraco ~0,33 s, médio ~0,43 s, forte ~0,7 s.

**Pose segurada:** a pose de dor fica ~40% do tempo (`durationsMs` com o 1º quadro longo e `posesChave`), como já está no `HIT_STAND` do Ryūga.

## 3. Ryūga (player_base): os números de cada golpe

Vantagem = atordoamento − (quadros ativos restantes + recuperação). Positiva: quem bateu age primeiro.

| Golpe | Início | Ativo | Recup. | Pausa no acerto | Atordoa (acerto/defesa) | Vantagem no acerto | Vantagem na defesa | Resultado |
|---|---|---|---|---|---|---|---|---|
| L | 2 | 4 | 5 | 8 | 12 / 7 | +4 | −1 | reação em pé |
| M | 4 | 6 | 8 | 11 | 15 / 9 | +2 | −4 | reação em pé |
| H | 10 | 5 | 17 | 16 | 26 / 16 | +5 | −5 | joga na parede |
| 2L | 2 | 4 | 6 | 8 | 16 / 7 | +7 | −2 | baixo |
| 2M | 12 | 4 | 18 | 11 | 41 / 11 | derruba | −10 | baixo, rasteira |
| 2H | 9 | 6 | 14 | 16 | 44 / 7 | lança | −12 | lançador |
| 6M | 20 | 3 | 14 | 11 | 20 / 11 | +4 | −5 | alto (defende em pé) |
| jL | 4 | 2 | 5 | 8 | 14 / 10 | — | — | no ar |
| jM | 6 | 4 | 8 | 11 | 16 / 12 | — | — | no ar |
| jH | 9 | 6 | 12 | 16 | 18 / 14 | — | — | joga na parede |
| j2H | 9 | 6 | 9 | 16 | 18 / 14 | — | — | quica no chão |

Na faixa das referências: fracos levemente positivos no acerto e neutros na defesa; médios e fortes negativos na defesa; rasteira e lançador puníveis.

## 4. Reações do Ryūga no jogo

Os três vídeos (rosto, tronco, forte) entraram em `tools/sprites/clips/player_base_hit_{high,body,heavy}.json`:
- quadros escolhidos sem o clarão e os riscos do vídeo; o 1º quadro já é a pose de dor;
- pé de trás no lugar da guarda (`--fixar pe`): o empurrão vem do jogo, não do vídeo;
- cor harmonizada com a guarda (`harmonia_cor.py`);
- a pose de dor fica ~44% do tempo (`durationsMs` 70,25,25,40,35,30,40,40,60; `posesChave` 0,4,6,8).

As caixas de corpo durante o dano continuam as do `HIT_STAND`: a reação muda o desenho, não a jogabilidade.
