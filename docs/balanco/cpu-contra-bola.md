# Contra quem só solta bola: mecânicas e uma CPU que aprende

**Pedido do jogador (01/11):** "Fiquei só soltando magia com o Ryūga e ganhei fácil de dois."
Depois veio a crítica: "Você está resolvendo pontualmente e não deixando a CPU mais inteligente.
Fora que não temos um counter para isso." As respostas dele foram as duas mecânicas (a defesa
perfeita absorve a bola, a corrida de energia atravessa a bola) e o S com ~46 quadros.

## O que mudou

1. **S de projétil com duração de mercado** (~46 quadros, como o Hadoken do SF6 e a bola do Fatal
   Fury). Detalhes em `ajustes.md`. No SHINKAI ENERGY a bola continua saindo seguida (recuperação
   de 11 quadros).
2. **Duas respostas à bola, para todo mundo:**
   - **Defesa perfeita na bola:** absorve a bola, sem empurrão e com 1 quadro preso ("ABSORVEU!").
   - **Corrida de energia:** correndo, M+H com 1/4 de barra. Atravessa bolas por 30 quadros.
3. **A CPU aprende o que funciona contra este jogador**, no lugar dos remendos (os "pula mais
   contra quem zoneia" e "avança contra a bola" fixos):
   - **Cada bola que vem é uma tentativa de um plano:** defende, defesa perfeita, pula por cima,
     corrida de energia, devolve outra bola, ou o especial do kit (repel, esquiva).
   - **Antes da bola, perto e na meia distância de quem zoneia, ela também escolhe um plano:**
     segue, espera defendendo, pula antes ou corre com a barra. Nessa faixa a bola de perto chega
     antes do tempo de reação. Cada faixa aprende separado.
   - **O resultado da tentativa** é a vida que ela tirou menos a que perdeu, mais o quanto chegou
     perto, numa janela de 75 quadros ou até a próxima bola. Ele entra na leitura (`CpuLeitura`),
     com o mesmo esquecimento e a mesma memória entre lutas dos hábitos.
   - **O ponto de partida é o jeito de cada um:** o acrobata gosta de pular, quem tem bola gosta
     de devolver, o agressivo gosta de correr, os níveis altos tentam a defesa perfeita, e quem
     tem repel ou esquiva começa por eles. O que rende contra o jogador soma em cima disso, com
     mais peso nos níveis altos. A escolha é sorteada, então ela não fica previsível. Os níveis
     baixos muitas vezes nem reagem.
4. **Punir de longe com especial de avanço conta o tempo de chegar.** Antes a CPU soltava o avanço
   de 300–400 px na recuperação da bola e chegava depois de o rival já ter se recuperado.

## O que as simulações mostraram

**O Ryūga que só solta bola** (`BateriaBola`, 20 lutas por lutador e nível, 1x1; saídas em
[perfis/bola-com-leitura.md](perfis/bola-com-leitura.md) e
[perfis/bola-sem-leitura.md](perfis/bola-sem-leitura.md)):

| Estado | CPU vence (nível 5) | CPU vence (nível 8) |
|---|---|---|
| Antes de tudo | 1% | 1% |
| S de mercado | 7% | 11% |
| Mais as duas mecânicas e os remendos fixos | 7% | 33% |
| Mais a CPU que aprende, com a leitura desligada (só o gosto do estilo) | 11% | 42% |
| **Mais a CPU que aprende, com a leitura ligada** | **13%** | **56%** |

- **O aprendizado responde por 14 pontos no nível 8** (42% → 56%), com os mesmos pontos de
  partida.
- **Exemplos do que muda:**
  - O p04 começava pela esquiva (S3) e apanhava: 0% → 65%, porque ela aprende que não vale.
  - O p09 e o p12 aprendem a correr com a barra através da bola (85% e 80%).
- **O spammer do `CpuTest` (só L, segurando trás entre as bolas):**
  - Nível 8: leva 46% das bolas soltas e perde 99% da vida.
  - Nível 3: leva 76% e perde 70%.

**Os 16 continuam diferentes** (`BateriaPerfis`, escala congelada, S7 → S8; tabelas em
[perfis/revisao/comparacao.md](perfis/revisao/comparacao.md)):

| Medida | S7 (só o S de mercado) | S8 (mecânicas + CPU que aprende) |
|---|---|---|
| Distância média entre pares no jeito de lutar | 1,42 | 1,41 |
| Núcleo (Ryūga, Yin-lu, Kōga, p04, p05, p10) no jeito de lutar | 1,11 | 1,11 |
| Pares parecidos (total abaixo de 0,9) | 10 | 9 |
| Vitórias: desvio / mín / máx | 16,3 / 9,8 / 71,5 | 16,8 / 7,3 / 76,7 |

**Hábitos e luta no chão:**
- `BateriaLeitura`, 1500 lutas: igual ao que era, ±2 pontos. A exceção é "colado, só defende",
  que foi de 67% para 72%.
- `BateriaMercado chao`, nível 8: golpes de −5 ou pior defendidos e punidos em até 25 quadros
  passaram de 8–10% para 11%. A bola caiu de 41% para 37% do dano.

## O que ainda falta (para decidir)

- **A bola de perto do Ryūga sai em 6 quadros.** No SF6 o Hadoken sai em 12–16. A maior parte das
  bolas que ainda pegam a CPU vem de 100–300 px, onde nenhuma reação chega a tempo: nem a da CPU
  (13 quadros no nível 8), nem a de uma pessoa (15–20). O tempo total já está no padrão de mercado;
  o início, não. **Proposta:** início do S de projétil 6 → 12 no Ryūga e no player_two,
  com a recuperação encurtada para manter os ~46.
- **O nível 5 (o começo da história) ainda perde do spammer: 13%.** É o nível que reage devagar
  e muitas vezes nem reage, por desenho. Ele melhora junto com a proposta acima.
- **O Natisuko (p15) é o mais fraco e caiu mais:** 14% → 10% (S de mercado) → 7%. A bola dele
  tem início de 14 quadros e alcance curto (320). Com 46 quadros no total, ele é quem mais perde.
  **Proposta:** devolver um pouco de recuperação só a ele (14/1/31 → 14/1/24), num commit isolado
  com antes e depois.

## Testes que mudaram (e por quê)

- **Spammer do `CpuTest`:** agora conta acertos por bola solta. A conta antiga, acerto ÷ (acerto
  + defesa), piorava quando a CPU pulava ou atravessava a bola, porque a bola evitada sumia da
  conta. Os limites continuam os mesmos (nível 8 abaixo de 0,5, e 0,15 abaixo do nível 3).
- **Reação ao pulo:** 16 sementes em vez de 10, porque o S mais longo deixa a CPU ocupada mais
  vezes. O limite (pelo menos 10 reações, nenhuma antes do tempo humano) é o mesmo.
- **A esquiva do p05:** agora é medida contra quem solta bola de verdade (o Ryūga). Contra o p11,
  que solta pouca bola, a CPU aprende a só defender.

## Como rodar

```
gradle -q -I <init com rodarBateria> :pc:rodarBateria -Dclasse=com.gamelutagpt.BateriaBola "-Dargs=20"
gradle ... "-Dargs=20 1x1 semLeitura"     # só o gosto do estilo, sem aprender
gradle ... "-Dargs=teste 8"               # o spammer do CpuTest, e onde as bolas pegam
gradle ... "-Dargs=depurar p03 8"         # razões, estados e o que ela aprendeu
gradle ... "-Dargs=cpu p05 p11 8"         # CPU × CPU: planos e especiais do lado 0
```
