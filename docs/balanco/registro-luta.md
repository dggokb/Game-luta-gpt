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
