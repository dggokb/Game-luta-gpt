# CPU adaptativa: o que foi feito e o que mudou (01/11)

Pedido: as quatro melhorias de adaptabilidade da CPU, mantendo as características de cada
personagem para "não virar tudo uma coisa só":
1. punição completa;
2. ler os hábitos do jogador;
3. trocar o que não funciona;
4. memória entre lutas.

## O que foi feito

| # | Onde | O que faz |
|---|---|---|
| 1 | `AiController.punirComOMelhor` | Depois de defender (ou ver um golpe no vazio), pune com o golpe mais forte que entra a tempo e alcança. A ordem é: Super (se a barra vale), a rota do caderno a partir do primeiro passo que alcança (o L de longe não chega, o H chega, e o resto da rota vem junto), os outros golpes de chão pelo dano e, por último, o especial de avanço, só com folga de 8 quadros. Antes ela só tentava L, 2L e M. |
| 2 | `CpuLeitura` (nova) e `AiController.observar` | Observa o rival pelo que aparece na tela (nunca pelo botão) em três situações: depois de defender a sequência dela, ao levantar e quando a bola dela chega nele. Registra o que ele faz logo em seguida: ataca, pula, recua (backdash), avança ou espera. As observações antigas pesam menos a cada nova. |
| 2 | `AiController.responder` | Com o hábito claro (certeza ≥ 0,3; chegar colado em quem levanta pede ≥ 0,45), responde com o jeito do lutador dela. Detalhes abaixo. |
| 3 | `CpuLeitura.fator` e `OpponentAi.think` | Cada jeito de chegar (correr, pular, cutucar de longe, soltar bola) que acaba com ela apanhando pesa menos nas próximas decisões, até 0,45×. Nunca passa do estilo: puxar para cima o que funcionava fazia todo mundo correr e cutucar mais. |
| 4 | `GameView` e `CpuLeitura.novaLuta` | A leitura passa de luta em luta, com 60% de peso a cada luta nova. Zera no começo de uma campanha ou de um Arcade. |

Como ela responde a cada hábito (item 2):

| Hábito lido | Resposta |
|---|---|
| Ele pula depois de defender ou ao levantar | O antiaéreo de reação sai com 95% de chance. |
| Ele aperta botão | Ela defende e pune. |
| Ele só defende | Plano de chegar e agarrar. O zoner responde com a bola. |
| Ele recua | Ela vai atrás. O zoner responde com a bola. |
| Ele pula a bola dela | Menos bola, e o antiaéreo pronto depois de soltar. |

**Como as características ficam:**
- A leitura só puxa as chances do estilo (`CpuStyle`), sem trocá-lo. As respostas usam as
  ferramentas e o jeito de cada lutador: o zoner responde de longe e o agarrador agarra.
- O peso da leitura cresce com o nível: nada no 1, inteira no 8.

## O que as simulações mostraram

**Os 16 continuam diferentes** (`BateriaPerfis`, escala congelada, S5 → S6):

| Medida | S5 (antes) | S6 (CPU adaptativa) |
|---|---|---|
| Distância média no jeito de lutar | 1,40 | 1,40 |
| Núcleo (Ryūga, Yin-lu, Kōga, p04, p05, p10) no jeito de lutar | 1,04 | 1,06 |
| Pares parecidos (total abaixo de 0,9) | 10 | 12 |
| Vitórias: desvio / mín / máx | 17,7 / 14 / 74 | 16,1 / 13 / 75 |

Os mais fracos subiram: p07 de 24% para 29%, p14 de 26% para 31%, p16 de 23% para 28%.

**Contra um jogador com hábito** (`BateriaLeitura`, 1500 lutas por linha, CPU 8 contra a CPU 6
com um hábito forçado, mesmas sementes; saída em
[perfis/leitura-1500.md](perfis/leitura-1500.md)):

| Hábito | Vitórias sem leitura | Com leitura | Agarrões/min sem → com |
|---|---|---|---|
| Nenhum | 52% | 54% | 0,1 → 0,1 |
| Pula depois de defender | 64% | 62% | 0 → 0 |
| Aperta L ao levantar | 77% | 75% | 0 → 0,1 |
| Colado, só defende | 68% | 69% | 0 → 0,2 |
| Pula a bola | 75% | 75% | 0 → 0,1 |

Memória entre lutas (5 lutas seguidas, hábito "pula depois de defender"): 78–84% sem memória e
79–82% com memória. Não há diferença medida.

**Punição:**
- Golpes inseguros defendidos e punidos em 25 quadros: 8% → 8–10% (`BateriaMercado chao`).
- No cenário só de rasteira: 25 → 29 de cerca de 200.
- O que limita é a distância: a CPU tenta punir quase sempre, mas a rasteira solta da ponta do
  alcance deixa os dois a 200–300 px, onde só especiais de avanço chegam, e tarde demais. O
  empurrão da rasteira na defesa é pequeno (36 px): é a distância de quem solta, como no SF6.

## Leitura

- **A leitura funciona por dentro, mas pouco muda o resultado contra a CPU com hábito** (todas as
  diferenças dentro de ±2 pontos, o ruído das 1500 lutas).
  - Ela detecta os hábitos: "pula depois de defender" com certeza de 0,6.
  - Ela também age: agarra quem só defende.
  - Mas a CPU já reagia bem ao que pode ser reagido: o antiaéreo de reação sai em 85% dos pulos
    no nível 8. E as respostas que mais rendem contra cada hábito pedem ferramentas que mudam o
    jogo da CPU de verdade.
- **O que rende para a leitura render** (próximo passo, se quiser):
  - contra quem pula depois de defender, terminar a sequência num golpe com vantagem e já estar
    com o antiaéreo no chão;
  - contra quem aperta ao levantar, recuar um passo e punir o golpe no vazio (o L em 0 na defesa
    não tem punição);
  - contra quem só defende, misturar agarrão com golpe alto e baixo em vez de só agarrar.
- **Contra gente,** os hábitos tendem a ser mais fortes que o "hábito forçado" da bateria, porque
  a CPU 6 do teste mistura o hábito com o resto do jogo dela. Vale testar com jogadores
  ([playtest.md](playtest.md)).
- **Erros corrigidos no caminho:**
  - Defender andando para trás era lido como "recuou".
  - A bola era observada quando saía, e não quando chegava.
  - O antiaéreo adiantado (o 2H solto antes de ver o pulo) saía no vazio mais do que acertava.
  - Agir em padrão fraco (certeza de 0,2) piorava a luta contra quem não tem hábito.
