# Registro de luta: a luta real, linha por linha (02/11)

**Pedido do jogador:** "essas análises não fazem sentido com o real", "a CPU quase não pula",
"registra todos os movimentos e a linha do tempo, para você entender o que aconteceu na luta real".

## Como usar

1. **Jogue normalmente contra a CPU**, em qualquer modo (história, arcade, versus). O jogo guarda
   as últimas 12 lutas na memória. Tutorial e teste de desempenho não entram.
2. **Na pausa (MENU), escolha ENVIAR REGISTRO.**
   - No celular, abre o "Compartilhar": escolha o **Drive**.
   - No PC, o arquivo vai para `Documentos/GameLutaGPT` e a pasta abre.
3. O arquivo se chama `registro-luta-AAAAMMDD-HHMM.txt`. Fecha o jogo e as lutas guardadas se
   perdem, então envie antes.

## O que o arquivo tem

O cabeçalho de cada luta traz a data, o modo, 1x1 ou 2x2, o nível da CPU, a velocidade e o ritmo,
e quem luta de cada lado. Depois vem uma linha por acontecimento, com o tempo da luta em segundos:

```
  30.60  JOG  aperta PULO
  30.63  JOG  pulo para frente  [x=1394 dist=285]
  31.02  JOG  golpe jH (no ar)  [x=1240 dist=143]
  31.20  JOG  DEFENDE jH → CPU  dano 0  (vidas 12093 × 5496)
  31.53  JOG  segura a defesa baixa  [x=1133 dist=101]
  31.63  JOG  golpe 2L  [x=1133 dist=101]
  31.67  JOG  ACERTA 2L → CPU  dano 375  (vidas 12093 × 5121)
   4.47  CPU  pensa: espera a bola defendendo
   3.62  MOTOR  JUST_DEFENSE_ABSORB
   2.00  ~  vidas 14000 × 12870  barras 0.4 × 0.0  SHINKAI 8% × 25%  dist 430
```

- **O que cada lado passa a fazer:** parado, anda, agacha, segura a defesa alta ou baixa, corre,
  backdash, pulo (para frente, para trás, no lugar), SUPER PULO, golpe (qual), apanha, defende,
  caído, levanta, agarra, ULTRA, SHINKAI CLASH. Quem está no ar depois de ser lançado aparece como
  "no ar (se recupera)", não como pulo.
- **Cada acerto e cada defesa,** com o golpe, o dano e as vidas.
- **Os avisos do motor:** SHINKAI, PARRY, COMBO BREAKER, LEITURA...
- **Os botões e as direções que você aperta.** Um botão segurado conta uma vez só.
- **O que a CPU "pensou" em cada decisão:** antiaéreo, pune, espera defendendo, avança contra a
  bola...
- **A cada segundo:** vidas, barras, SHINKAI e distância.

Código: `RegistroLuta` (no jogo), `GameView.enviarRegistro`. Para resumir um arquivo recebido:
`ResumoRegistro` (pulos e como terminaram, acertos, golpes mais usados, o que a CPU mais pensou).

## Correção: a contagem de pulos estava inflada

A primeira `BateriaPulos` contava como pulo qualquer momento em que o lutador estava no ar e livre.
Isso pegava três coisas que não são pulo:
- a coreografia do SHINKAI Clash, em que os dois voam pela arena (~2,5 por luta);
- quem se recupera no ar depois de ser lançado;
- o mesmo pulo outra vez, depois de um golpe no ar.

Contando só o pulo de verdade (sair do chão livre, fora do Clash):

| Pulos por luta, CPU × CPU (cada lado) | antes (errado) | corrigido |
|---|---|---|
| Nível 5 | 5,7 | 2,9 |
| Nível 6 | 7,6 | 4,4 |
| Nível 7 | 8,8 | 5,3 |
| Nível 8 | 9,8 | 6,0 |

Mesmo corrigido, isso é CPU contra CPU: parte dos pulos é a CPU passando por cima da bola da
outra. Contra uma pessoa, só o registro da luta real diz. Saída completa em
[perfis/pulos-cpu.md](perfis/pulos-cpu.md).

## O primeiro registro real (01/11): o que aconteceu

Três lutas do arcade 2x2 (Ryūga e p02 contra p06/p11 e p05/p10 no nível 5, p13/p16 no nível 6):
duas vitórias e uma derrota. Resumo do `ResumoRegistro`:

| luta | seus pulos | pulos da CPU | seus golpes mais usados | o que a CPU mais pensou |
|---|---|---|---|---|
| 1 (nível 5, venceu) | 26 | 15 | 2M 29, jM 28, S 16 | pula antes da bola 14, pulo para dentro 11 |
| 2 (nível 5, venceu) | 21 | 17 | 2M 29, jM 24 | pula antes da bola 15, pulo para dentro 13 |
| 3 (nível 6, perdeu) | 48 | 20 | jM 43, 2M 33 | pula antes da bola 16, pressiona (2L) 14 |

**O laço.** A rasteira (2M) derruba. Quando a CPU levanta, ou a rasteira de novo em cima da hora,
ou o pulo com o jM caindo nela. Depois do jM (entrou ou defendido), a rasteira ao cair.

**Por que a CPU tomava tudo.** Lendo a linha do tempo:
- **Ela levantava parada.** Só decidia defender depois do tempo de reação (16 quadros no nível 5).
  A rasteira em cima da hora chegava 1 a 5 quadros depois de ela ficar livre. Na linha do tempo:
  `levanta`, `parado`, `caído`.
- **A rasteira depois do jM.** Ela defendia o jM em pé e continuava em pé. Ou levava o jM e saía
  do atordoamento parada. A rasteira entrava 1 a 4 quadros depois: `segura a defesa alta`,
  `caído`.

Quem joga segura trás-baixo já deitado e agacha quando vê o rival caindo do pulo. Ela não fazia
nenhum dos dois. Contra o pulo de longe ela ia razoável: você apanhou no ar em 25 a 38% dos
pulos. **Ela pulou, sim:** 15 a 20 vezes por luta, a maioria por cima da sua bola (S) ou
entrando de pulo.

**O relógio do registro não zerava entre as lutas do arcade:** a segunda luta começava em 114 s.
Corrigido: cada luta conta do zero.

### A correção: levantar e sair do atordoamento defendendo

- **Levantando ou atordoada, com você a menos de 300:** ela decide (uma vez) se sai defendendo e
  já segura a defesa, para estar defendendo no primeiro quadro livre (por 14 a 21 quadros).
  - Altura: com você no ar, em pé. No chão, a que ela aprendeu de você (quem só dá rasteira, ela
    defende baixo).
  - Chance: a defesa do nível × (0,3 + 0,6 × o quanto você já entrou nela de perto e de pulo).
    Ela aprende ao longo da luta e do arcade.
- **Defendendo em pé porque você vinha pelo ar:** quando ela vê que você está caindo (o golpe no
  ar já bateu, ou você não atacou), agacha se esse for o hábito dela contra você.

`BateriaLevantar` repete o laço com um jogador roteirizado (Ryūga). O arcade é de 7 lutas, níveis
5 → 8, com a mesma CPU, e são 120 arcades.

| nível | levanta defendendo | rasteira no levantar entra | jM no levantar entra | rasteira depois do jM entra |
|---|---|---|---|---|
| 5 (1ª luta) | 20% → 44% | 87% → 55% | 77% → 55% | 87% → 74% |
| 6 (3ª luta) | 32% → 57% | 76% → 39% | 71% → 44% | 81% → 52% |
| 7 (5ª luta) | 31% → 53% | 70% → 40% | 64% → 35% | 73% → 41% |
| 8 (7ª luta) | 33% → 58% | 68% → 30% | 66% → 50% | 66% → 36% |

As duas partes contam:
- **Sem a troca para baixo,** a rasteira depois do jM seguia entrando 74–88%.
- **Com as duas,** o roteiro vence 1–22%, contra 5–33% antes.
- **A voadora da `BateriaVoadora`** caiu de 16/20/8/5/0/4/1% para 8/5/0/0/0/0/0%.
- **CPU × CPU quase não muda:** a luta dura de 53 para 54 s no nível 5 e de 60 para 62 s no 8; ela
  pula o mesmo tanto.

O nível 5 continua deixando passar metade: é o nível de aprender. Saídas completas:
- [perfis/levantar-antes.md](perfis/levantar-antes.md)
- [perfis/levantar-sem-troca.md](perfis/levantar-sem-troca.md)
- [perfis/levantar-depois.md](perfis/levantar-depois.md)

## O segundo registro real (02/11): depois da correção do levantar

Arcade com p02 + p15: 7 lutas, níveis 5, 5, 6, 6, 7 (2x2) e 8, 8 (1x1, contra p02). Foram 5
vitórias e 2 derrotas. O jogador: "melhorou bastante".

| luta | nível | resultado | vida que sobrou (você × CPU) | dano seu × da CPU |
|---|---|---|---|---|
| 1 | 5 | venceu | 4908 × 514 | 34,9 mil × 28,1 mil |
| 2 | 5 | venceu | 4377 × 261 | 36,2 mil × 29,5 mil |
| 3 | 6 | perdeu | 0 × 4506 | 19,4 mil × 35,0 mil |
| 4 | 6 | venceu (revanche) | 7598 × 302 | 32,7 mil × 16,4 mil |
| 5 | 7 | venceu | 1745 × 812 | 33,2 mil × 34,1 mil |
| 6 | 8 | perdeu | 626 × 615 | 13,9 mil × 14,8 mil |
| 7 | 8 | venceu (revanche) | 5901 × 316 | 15,5 mil × 9,7 mil |

### O que melhorou
- **O levantar:**

  | | 1º registro | 2º registro |
  |---|---|---|
  | levantares com você perto | 50 | 68 |
  | você acerta | 26 | 14 |
  | ela defende | 2 | 22 |
  | ela acerta primeiro | 8 | 12 |

- **Rasteira e jM:** caíram de 56% para 25% do seu dano. A rasteira sozinha foi de 21% para 8%.
- **Você passou a variar:** jab (L), bola (S), TAG e 2H abrem combos tanto quanto o jM.
- **O nível sobe certo:** quatro das sete lutas acabaram com alguém abaixo de 900 de vida. O 8
  perdeu por 11 de vida e depois ganhou a revanche com folga.
- **A CPU usa o kit todo:**
  - defesa perfeita: 30;
  - parry e guard cancel;
  - corrida de energia, TAG e SHINKAI;
  - Ultra que mata você 4 vezes;
  - combo breaker: 22 dos seus 157 combos, 14%.
- **O dano da CPU vem de vários lugares:** a bola (21%), o jH (15%), o L (12%) e o 2L (10%).
  Muito dele entrou com você apertando golpe: troca de golpes, não falha dela.

### O que ainda é estranho
- **Ar contra ar:** você ganhou 19 trocas no ar e ela 14. Em 8, o seu jM bateu no jH dela.
  - Por quê: o jH sai em 8 a 12 quadros e alcança pouco; o jM sai em 5 a 6 e alcança mais. Ela só
    solta o golpe descendo.
  - O teste: escolher o golpe que sai primeiro e alcança, sem esperar descer. A simulação
    (`BateriaArAr`, um jogador que pula junto quando ela pula) não mostrou diferença. Quase não
    houve troca no ar entre os dois. Pela regra (simula; positivo fica, senão sai), saiu.
  - Precisa de um teste que monte a troca no ar de propósito. Feito: veja "Ar contra ar
    montado", abaixo.
- **Os pulos dela rendem pouco:**
  - ela pulou 123 vezes nas 7 lutas;
  - apanhou no ar em 40 (33%) e acertou em 30 (24%);
  - o seu pulo acertou em 35% (49 de 140) e apanhou em 22%.
- **O nome "pula antes da bola"** aparecia também contra quem pula ou cutuca, sem bola nenhuma.
  Agora é "pula na meia distância".
- **"Levanta defendendo"** também aparecia ao sair do atordoamento, no meio do combo. Agora é "sai
  do atordoamento defendendo".
- **O relógio de cada luta já começa do zero.** A correção funcionou.

### Ar contra ar montado (pedido do jogador: "faz a simulação do ar contra ar e testa de novo")

**A cena.** `BateriaArAr` põe os dois parados a 300, 380 ou 460 de distância. A CPU pula para dentro
(o plano dela) e o jogador pula também, de 10 quadros antes a 10 depois. No ar, o jogador solta:
- o jM ao alcançar (o do registro);
- o jM cedo;
- o jL;
- o jH descendo;
- nada.

São p15 e Ryūga contra os 16, nos níveis 5 e 8, com 300 cenas por linha.

**Antes:** contra o jM ela não acertava nunca (0–1% das cenas). Ou defendia no ar (12–35%), ou
levava o golpe. Dois motivos:
- **Não via onde você estava.** Ela vê você com o atraso da reação (13 quadros no nível 8). A
  velocidade do pulo não aparece no `vx` (fica 0), então ela via você uns 110 px mais longe do que
  você estava e achava que nada alcançava.
- **Escolhia mal o golpe.** Só soltava descendo, e o mais forte (jH: 8–12 quadros, curto). O jM sai
  em 5–6 e alcança mais.

**Agora:**
- **Ela prevê a trajetória.** Usa o que viu nos últimos quadros: o pulo segue reto.
- **Disputa no momento certo:** solta o golpe que sai primeiro e chega lá, subindo ou descendo.
- **Nem sempre disputa.** A chance de disputar no pulo é a do antiaéreo do nível: 50% no 5, 85% no
  8. Senão, faz como antes.

Fração das cenas, nível 8 (você acerta / ela acerta / troca / defendido):

| seu golpe | antes | agora |
|---|---|---|
| jM ao alcançar | 50 / 1 / 0 / 35% | 49 / 25 / 5 / 5% |
| jM cedo | 51 / 0 / 0 / 38% | 67 / 13 / 3 / 6% |
| jL | 67 / 3 / 0 / 17% | 56 / 22 / 4 / 3% |
| jH descendo | 20 / 38 / 21 / 0% | 3 / 73 / 3 / 1% |

**Média dos quatro golpes, nível 8:**
- você acerta 44% das cenas (era 47%);
- ela acerta 33% (era 10%).

**No nível 5:** você acerta 57% (era 58%) e ela 19% (era 10%).

**O que não muda.** Ela não vê o seu golpe sair antes de decidir: a reação dela não deixa. Por isso
o jM cedo, apertado antes de ela ver você no ar, agora entra mais (ela troca em vez de defender).
Também tentei:
- defender se o seu golpe já está saindo: ela nunca chega a ver a tempo;
- só disputar se o golpe dela for mais rápido que o seu mais rápido que alcança: deu quase igual.

Ficou a versão simples.

**CPU × CPU:** igual. A luta dura 55 s no nível 5 e 62 s no 8, e ela pula 2,9 e 6,7 vezes por luta.

Saída completa: [perfis/ar-contra-ar.md](perfis/ar-contra-ar.md). Teste: `CpuArArTest`.

## O terceiro registro real (02/11, tarde): ar contra ar e câmera lenta novos

Arcade com p02 + p15: 5 lutas nos níveis 5, 5, 6, 7 e 8 (a final 1x1, contra a p15). O jogador
venceu todas.

| luta | nível | vida que sobrou (você × CPU) | dano seu × da CPU | golpes por minuto (você × CPU) |
|---|---|---|---|---|
| 1 | 5 | 3035 × 318 | 36,5 mil × 23,1 mil | 48 × 22 |
| 2 | 5 | 7239 × 252 | 32,0 mil × 11,9 mil | 48 × 26 |
| 3 | 6 | 4861 × 671 | 33,3 mil × 30,4 mil | 51 × 25 |
| 4 | 7 | 362 × 404 | 40,9 mil × 30,3 mil | 43 × 23 |
| 5 | 8 | 7385 × 43 | 16,5 mil × 5,4 mil | 49 × 22 |

- **Ar contra ar ficou parelho:** 10 trocas para cada lado (no 2º registro eram 19 × 14). A CPU usa
  o jL quando vê você no ar ("ar contra ar") e ganhou 4 assim.
- **O levantar segue bom:** você acertou em 20 dos 71 levantares; ela defendeu 23 e acertou primeiro
  em 5.
- **A CPU ataca metade do que você ataca:** 22 a 26 golpes por minuto, contra 43 a 51 seus. No 2º
  registro a final do nível 8 tinha 32 a 34 por minuto; agora 22. A final ficou fácil: ela acertou
  só 11 golpes em 79 s.
- **Onde ela perde dano:** 35% do seu dano começa com ela atacando. O caso mais comum é o pulo dela
  com jH, que você tira com o L ou o M no chão: 7 combos, ~10 mil.

### "A CPU é fácil: não comba, não pressiona, não pune" (02/11)

No terceiro registro, os combos da CPU sem Super têm 1,2 a 2 golpes em média; na final do nível 8
foram 9 combos de 1,2 golpe. `BateriaCombosCpu` mede os combos dela contra o laço do jogador
(níveis 5 a 8, 200 lutas por nível). Ela mostra onde o combo morria:

- **O pulo que acertava não emendava nada.** O golpe no ar do pulo para dentro saía sem rota de
  combo; ao cair, ela não continuava. Agora, caindo com ele ainda atordoado, ela emenda o combo do
  chão pelo primeiro golpe que alcança (chance de confirmar do nível × 0,6–1).
- **O golpe solto não seguia.** Cutucada, pressão e reação que acertavam paravam no primeiro
  golpe; só o golpe que já saía como começo de combo seguia. Agora qualquer golpe de chão que
  acerta segue no combo dele (chance de confirmar do nível).
- **O SHINKAI no meio do combo matava o combo.** Ele cortava o golpe seguinte, ainda saindo (o 2H
  sumia), e a rota achava que já tinha passado dele. Agora ele só corta o golpe que acertou, e o
  passo seguinte sai do neutro.
- **A bola no cancelamento** sai no contato do golpe, sem esperar a reação inteira (como quem
  cancela o M na bola).

| nível 8 (contra o laço) | antes | agora |
|---|---|---|
| golpes por combo | 2,57 | 2,70 |
| combos de 4 golpes ou mais | 24% | 27% |
| dano por combo | 1366 | 1418 |
| pulos que acertam e param no 1º golpe | 107 | 33 |

No nível 7, os combos vão de 2,27 para 2,39 golpes e de 1256 para 1319 de dano. A defesa ao
levantar não deixou a CPU mais passiva: com ou sem ela, são 42 a 44 golpes por minuto no nível 8.

**O que ainda segura os combos:**
- o antiaéreo (2H ou M em quem pula) que acerta e não continua (2/3 das tentativas falham);
- o M de longe, que empurra o rival para fora do alcance do golpe seguinte.

Saídas: [perfis/combos-cpu-antes.md](perfis/combos-cpu-antes.md) e
[perfis/combos-cpu-depois.md](perfis/combos-cpu-depois.md).
