# Ritmo da luta: 100% (antigo) × 95% (agora)

## Golpes e movimento em milissegundos (mediana do elenco)

| | quadros | a 100% | a 95% | a 90% | mercado (60 fps) |
|---|---|---|---|---|---|
| início do L (jab) | 3 | 50 ms | 53 ms | 56 ms | SF6 4–5 q (67–83 ms) |
| início do 2L | 3 | 50 ms | 53 ms | 56 ms | SF6 4–5 q |
| início do M | 6 | 100 ms | 105 ms | 111 ms | SF6 6–8 q (100–133 ms) |
| início da rasteira (2M) | 7 | 117 ms | 123 ms | 130 ms | SF6 7–9 q |
| início do H | 10 | 167 ms | 175 ms | 185 ms | SF6 8–12 q (133–200 ms) |
| início do 2H (lançador) | 10 | 167 ms | 175 ms | 185 ms | – |
| início da bola (S) | 10 | 167 ms | 175 ms | 185 ms | Hadoken 12–16 q (200–267 ms) |
| bola inteira (S) | 46 | 767 ms | 807 ms | 852 ms | Hadoken ~46 q (767 ms) |
| pulo no ar (~42 quadros) | 42 | 700 ms | 737 ms | 778 ms | SF6 ~45 q (750 ms) |
| reação humana boa (referência) | 15 | 250 ms | 263 ms | 278 ms | ~250 ms |
| CPU nível 8 (reação) | 13 | 217 ms | 228 ms | 241 ms | – |
| CPU nível 5 (reação) | 16 | 267 ms | 281 ms | 296 ms | – |

Um golpe é "de reagir" quando o início dele passa da reação de quem defende. A 95%, cada quadro
dura 17,5 ms (era 16,7): quem tem 250 ms reage em 14,25 quadros, não em 15.

| golpes do elenco (normais e especiais de perto) com início ≥ 250 ms (dá para defender reagindo) | a 100% | a 95% | a 90% |
|---|---|---|---|
| de 156 | 36 | 36 | 39 |

## Uma pessoa contra a CPU do arcade (1x1, 300 lutas por célula, duplas sorteadas, as mesmas sementes)

A "pessoa" é a CPU de um nível com a reação dela em milissegundos fixa: no ritmo mais lento ela reage em
menos quadros (nível 6 = 15 quadros = 250 ms; nível 4 = 19 quadros = 317 ms). A CPU rival reage nos mesmos quadros.

### Pessoa com reação de nível 6

| CPU do arcade | pessoa vence a 100% | a 95% | a 90% | duração da luta a 100% / 95% |
|---|---|---|---|---|
| nível 5 | 59% | 61% | 61% | 50.8 s / 53.0 s |
| nível 6 | 51% | 51% | 51% | 52.2 s / 54.0 s |
| nível 7 | 35% | 38% | 38% | 52.1 s / 55.4 s |
| nível 8 | 34% | 33% | 33% | 53.8 s / 56.2 s |

### Pessoa com reação de nível 4

| CPU do arcade | pessoa vence a 100% | a 95% | a 90% | duração da luta a 100% / 95% |
|---|---|---|---|---|
| nível 5 | 31% | 36% | 35% | 49.3 s / 51.6 s |
| nível 6 | 23% | 21% | 23% | 48.9 s / 52.4 s |
| nível 7 | 17% | 22% | 18% | 48.5 s / 50.5 s |
| nível 8 | 14% | 15% | 12% | 48.0 s / 51.5 s |

(20 s)
