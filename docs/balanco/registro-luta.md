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
