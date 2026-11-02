# Ritmo da luta: 100% × 95% (02/11)

**Pedido do jogador:** "deixa a velocidade de jogo 95% como 100%, senti que o jogo ficou com bem
mais cara de jogo". Depois: "simula o jogo com o ritmo novo e vê como ficou". A mudança está em
`GameView.RITMO_BASE` (veja `ajustes.md`).

**O motor conta quadros, não segundos.** CPU × CPU dá exatamente o mesmo resultado em qualquer
ritmo. O que muda é o tempo de verdade, e com ele quem tem reação em milissegundos: uma pessoa.
A `BateriaRitmo` mede as duas coisas. A saída completa está em
[perfis/ritmo-95.md](perfis/ritmo-95.md).

## Os golpes em milissegundos, ao lado do mercado

| (mediana do elenco) | quadros | a 100% (antes) | a 95% (agora) | mercado (60 fps) |
|---|---|---|---|---|
| Início do L (jab) | 3 | 50 ms | 53 ms | SF6 4–5 q (67–83 ms) |
| Início do M | 6 | 100 ms | 105 ms | SF6 6–8 q (100–133 ms) |
| Início da rasteira (2M) | 7 | 117 ms | 123 ms | SF6 7–9 q |
| Início do H | 10 | 167 ms | 175 ms | SF6 8–12 q (133–200 ms) |
| Início da bola | 10 | 167 ms | 175 ms | Hadoken 12–16 q (200–267 ms) |
| Bola inteira | 46 | 767 ms | 807 ms | Hadoken ~46 q (767 ms) |
| Pulo no ar | 42 | 700 ms | 737 ms | SF6 ~45 q (750 ms) |
| CPU nível 8 (reação) | 13 | 217 ms | 228 ms | – |

- **O jogo estava mais rápido que o mercado** nos golpes de início curto e no pulo. O jab sai em
  3 quadros (o do SF6, em 4–5), e o pulo durava 700 ms (o do SF6, ~750). A 95% o pulo fica
  praticamente igual ao do SF6 (737 ms), e o M, o H e a rasteira continuam dentro da faixa dele.
  Provavelmente é isso que deu a "cara de jogo": o tempo de tudo encostou no que a mão já
  conhece.
- **Golpes de perto que dá para defender reagindo** (início ≥ 250 ms): 36 de 156, iguais a 100% e
  a 95% (39 a 90%). Nenhum golpe mudou de categoria. O que muda é a folga, ~5% mais tempo em
  tudo.

## Uma pessoa contra a CPU do arcade

A "pessoa" é a CPU de um nível com a reação fixa em milissegundos:
- **nível 6:** 15 quadros, 250 ms, alguém bom;
- **nível 4:** 19 quadros, 317 ms, alguém comum.

No ritmo mais lento ela reage em menos quadros, e a CPU rival reage nos mesmos quadros. Foram
300 lutas por célula, com duplas sorteadas e as mesmas sementes.

| Pessoa nível 6 contra | a 100% | a 95% | Pessoa nível 4 contra | a 100% | a 95% |
|---|---|---|---|---|---|
| CPU 5 | 59% | 61% | CPU 5 | 31% | 36% |
| CPU 6 | 51% | 51% | CPU 6 | 23% | 21% |
| CPU 7 | 35% | 38% | CPU 7 | 17% | 22% |
| CPU 8 | 34% | 33% | CPU 8 | 14% | 15% |

- **Para quem joga, a diferença em vitórias é pequena:** +2 a +5 pontos na maioria, dentro do
  ruído de ±5 pontos de 300 lutas. A 95% a pessoa ganha ~1 quadro de reação. Não é isso que muda
  a sensação; é o ritmo do que se vê.
- **As lutas duram ~2–3 s a mais em tempo real:** 51 → 53 s contra a CPU 5, e 54 → 56 s contra a
  CPU 8.
- **A CPU nível 8 reage em 228 ms** (era 217). Continua acima do limite humano que ela tem por
  regra, e fica um pouco mais justa.

## Conclusão

A 95% não muda o equilíbrio: as vitórias ficam iguais e nenhum golpe muda de categoria. O que
muda é o tempo: o pulo e os golpes ficam com a duração do mercado, e quem joga ganha ~1 quadro
de folga. Se quiser testar outro ritmo, é um número só (`RITMO_BASE`); a 90% os golpes de perto
reagíveis passam de 36 para 39.

## Como rodar

```
gradle -q -I <init com rodarBateria> :pc:rodarBateria -Dclasse=com.gamelutagpt.BateriaRitmo "-Dargs=300"
```
