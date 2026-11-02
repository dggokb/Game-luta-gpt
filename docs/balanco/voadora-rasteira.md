# A CPU que não aprendia contra voadora e rasteira (02/11)

**Pedido do jogador:** "o jogo não aprende contra; joguei o torneio e no último ganhei só com
voadora e rasteira, ele tomou todas".

`BateriaVoadora`: um jogador roteirizado (o Ryūga) que só faz isso. Ele pula de meia distância,
chuta e dá a rasteira (2M) ao cair; de perto, só rasteira. O teste é um arcade de 7 lutas com a
CPU subindo do nível 5 ao 8. A CPU é a **mesma** nas 7 lutas, e a memória dela passa de uma
para a outra como no jogo. Foram 80 arcades.

## O que estava errado (medido)

1. **A rasteira nunca era defendida.** Ela entrava em ~42% das vezes e era defendida em 4–6%, da
   primeira à última luta. Ela sai mais rápido que a reação da CPU (7 contra 13 quadros), então
   só antecipando dá para defender. A CPU não antecipava, porque não guardava nada sobre a altura
   nem sobre os golpes de chão do jogador. A memória dela cobria três situações: depois de
   defender, ao levantar e contra a bola.
2. **O antiaéreo quase nunca era tentado:** em 4–12% dos pulos, com a chance do nível em 50–85%.
   A CPU via o pulo e entrava na defesa de 40 quadros antes de decidir se tirava do ar. A decisão
   do antiaéreo vinha depois e não chegava a rodar.
3. **O chute logo na subida do pulo acertava quem está em pé no 6º–8º quadro**, antes de qualquer
   reação. Isso vinha do "pulo útil" (01/11): o alcance extra de 70 px para baixo valia também
   subindo.

## O que mudou

- **Motor:** o alcance extra do golpe no ar contra quem está no chão vale só **descendo**, que é
  o pulo de ataque (`pulandoParaDentro`).
- **Antiaéreo:** primeiro a CPU decide se tira do ar e só defende o pulo se não for tirar. A chance
  do nível passou a valer de verdade.
- **A CPU aprende como você entra** (`CpuLeitura`, com o mesmo esquecimento e a mesma memória entre
  lutas do resto):
  - **Altura:** dos golpes de chão que chegam nela, quanto é baixo. Quem só dá rasteira ela passa
    a defender agachada; quem só bate alto, em pé.
  - **Golpes de chão que entraram:** quanto mais você acerta de perto, mais ela espera
    defendendo no seu alcance.
  - **Pulos que chegaram nela:** a chance de tirar do ar sobe até 97%, conforme o nível.
  - **Plano de meia distância:** quem entra de rasteira ou de pulo de novo e de novo passa a ser
    tratado como o zoneador. Ela testa andar, esperar defendendo (agachada contra rasteira), pular
    antes e pressionar, e fica com o que rende contra você.
  - Só o golpe que abre o combo conta; os outros acertos do mesmo combo não.
  - Tudo isso escala com o nível. O nível 1 quase não aprende, e o plano só liga do nível 4 para
    cima.
- **Testado e saiu:** prever onde o pulo estará quando o 2H sair (a reação mais o início do 2H).
  A CPU tentava mais, mas o jogador vencia mais (luta 1: 30% → 41%).

## Resultado (arcade de 7 lutas, níveis 5 → 8)

| Jogador vence, luta a luta | antes | agora |
|---|---|---|
| Voadora + rasteira | 41 / 43 / 19 / 15 / 13 / 14 / 4% | 16 / 20 / 8 / 5 / 0 / 4 / 1% |
| Só rasteira | 18 / 24 / 11 / 13 / 10 / 8 / 11% | 5 / 5 / 4 / 3 / 0 / 1 / 0% |

| Só rasteira, luta a luta | antes | agora |
|---|---|---|
| A rasteira acerta | 42 / 43 / 42 / 42 / 41 / 41 / 42% | 33 / 31 / 28 / 28 / 25 / 27 / 22% |
| A rasteira é defendida | 4 / 5 / 6 / 6 / 4 / 4 / 6% | 15 / 17 / 19 / 19 / 19 / 18 / 22% |

Agora dá para ver o aprendizado: a rasteira entra cada vez menos ao longo do torneio. A voadora
defendida subiu de ~35% para ~44%. Saídas completas em
[perfis/voadora-rasteira-depois.md](perfis/voadora-rasteira-depois.md).

## E quem joga normal? (`BateriaLog`, 48 lutas por nível)

| Jogador vence (níveis 5 / 6 / 7 / 8) | antes | agora |
|---|---|---|
| Solta bola (meia-lua) | 92 / 85 / 67 / 69% | 88 / 90 / 69 / 63% (ruído) |
| Pulador (pula com o jH e emenda) | 58 / 48 / 56 / 23% | 33 / 31 / 25 / 10% |
| A CPU nível 8 no lugar do jogador | 88 / 88 / 81 / 73% | 96 / 90 / 81 / 73% (ruído) |

**O pulador caiu.** As chances de antiaéreo de cada nível (50% no 5, 85% no 8) existiam, mas não
valiam, e agora valem. Pular continua sendo útil: o golpe no ar alcança e o defendido dá vantagem.
Só que pular sempre, sem variar, agora é punido, como no mercado. Se pesar demais nos níveis
baixos, o ajuste é a tabela de antiaéreo do `CpuLevel`.

Saídas em [perfis/log-voadora-antes.md](perfis/log-voadora-antes.md) e
[perfis/log-voadora-depois.md](perfis/log-voadora-depois.md).

## Testes

- No `PressaDoTempoTest`, a CPU com pressa pula mais para entrar, e com o antiaéreo funcionando a
  calma tira esses pulos do ar. O teste continua cobrando que ela "ataca mais" e "fica menos
  defendendo". No dano, cobra "não bate muito menos" (≥ 90%), não mais "bate mais".

## Como rodar

```
gradle -q -I <init com rodarBateria> :pc:rodarBateria -Dclasse=com.gamelutagpt.BateriaVoadora "-Dargs=80 voadora"
gradle ... "-Dargs=80 rasteira"
```
