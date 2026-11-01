# SHINKAI do Ryūga: protótipo (PERCEPÇÃO) × antigo (ENERGY)

**Espelho:** o Ryūga contra ele mesmo, os dois com o SHINKAI da coluna (480 lutas por coluna).

CPU nível 8 dos dois lados, 1x1, o Ryūga contra os 15 (32 lutas cada, metade de cada lado), as mesmas sementes em todas as colunas. O rival usa o SHINKAI antigo do tipo dele. Jogo normal (com Clash).

| medida | antigo (ENERGY) | protótipo | sem reação rápida da CPU | sem Ruptura | sem os dois |
|---|---|---|---|---|---|
| vitórias do lado medido (os dois iguais: ~50%) | 54% | 56% | 56% | 53% | 55% |
| duração média da luta (s) | 43.8 | 43.2 | 44.4 | 43.8 | 44.5 |
| quem vence o Clash vence a luta | 73% | 83% | 76% | 78% | 76% |
| Rupturas por luta (os dois lados) | 0.00 | 0.35 | 0.38 | 0.00 | 0.00 |
| leituras por luta (os dois lados) | 0.00 | 0.34 | 0.21 | 0.27 | 0.16 |
| SHINKAIs ligados por luta | 0.97 | 1.02 | 1.05 | 1.02 | 1.05 |
| duração média do SHINKAI (s) | 11.4 | 13.2 | 13.0 | 12.1 | 12.0 |
| dano que ele dá por s, no SHINKAI | 300.7 | 329.8 | 311.2 | 326.8 | 299.9 |
| dano que ele leva por s, no SHINKAI | 162.3 | 175.6 | 201.2 | 163.1 | 183.2 |
| saldo por SHINKAI (dá − leva) | 1574 | 2030 | 1431 | 1985 | 1399 |
| dano que ele dá / leva por s, fora | 278.7 / 295.2 | 266.5 / 303.7 | 279.9 / 294.2 | 267.8 / 308.2 | 278.4 / 299.6 |
| dano que ele leva por s, no SHINKAI do rival | 282.8 | 306.2 | 293.3 | 307.3 | 286.2 |
| SHINKAI Clash por luta | 0.95 | 0.98 | 1.00 | 0.96 | 0.98 |
| no SHINKAI dele: golpes do rival / defendidos / perfeitas (por luta) | 2.12 / 0.33 / 0.10 | 3.83 / 0.59 / 0.26 | 4.11 / 0.52 / 0.18 | 3.17 / 0.49 / 0.21 | 3.46 / 0.41 / 0.15 |
| leituras / esquivas / precisos por luta | 0.00 / 0.00 / 0.00 | 0.17 / 0.00 / 0.12 | 0.13 / 0.00 / 0.07 | 0.14 / 0.00 / 0.09 | 0.10 / 0.00 / 0.06 |
| dano médio do golpe preciso | 0 | 688 | 673 | 671 | 607 |
| fim: desligou / no limite / Ruptura (total) | 0 / 0 / 0 | 49 / 91 / 89 | 62 / 93 / 93 | 48 / 189 / 0 | 58 / 190 / 0 |
| s exausto por luta (dano levado exausto por s) | 0.0 (0.0) | 0.7 (314.3) | 0.7 (389.2) | 0.6 (421.6) | 0.6 (478.8) |
| dor da Ruptura por luta | 0.00 | 154.34 | 151.64 | 0.00 | 0.00 |

## Por rival (Ryūga vence)

| rival | tipo | antigo (ENERGY) | protótipo | sem reação rápida da CPU | sem Ruptura | sem os dois |
|---|---|---|---|---|---|---|
| player_base | ENERGY | 54% | 56% | 56% | 53% | 55% |

## Log de 4 lutas (protótipo)

### Ryūga (lado 0) × Ryūga (PERCEPÇÃO)

```
   13.87 s  vida 13550 ×  4795  Ryūga liga o SHINKAI PERCEPÇÃO
   15.32 s  vida 13708 ×  4807  rival liga o SHINKAI PERCEPÇÃO
   15.32 s  vida 13708 ×  4807  SHINKAI_CLASH
   21.12 s  vida 11708 ×  4807  SHINKAI do Ryūga acabou
   21.12 s  vida 11708 ×  4807  SHINKAI_CLASH_WIN (rival)
   30.90 s  vida  1440 ×  4686  LIMITE (rival)
   31.12 s  vida  1440 ×  4810  RUPTURA (rival)
   34.72 s  vida     0 ×  2462  rival vence (leituras 0, esquivas 0, precisos 0, Rupturas 0)
```

### Ryūga (lado 1) × Ryūga (PERCEPÇÃO)

```
   11.88 s  vida 13162 ×  5645  rival liga o SHINKAI PERCEPÇÃO
   15.32 s  vida 13379 ×  5657  Ryūga liga o SHINKAI PERCEPÇÃO
   15.32 s  vida 13379 ×  5657  SHINKAI_CLASH
   21.12 s  vida 11379 ×  5657  SHINKAI_CLASH_WIN (Ryūga medido)
   29.02 s  vida 11379 ×  2941  SHINKAI do Ryūga desligado por ele
   52.22 s  vida  2170 ×     0  rival vence (leituras 0, esquivas 0, precisos 0, Rupturas 0)
```

### Ryūga (lado 0) × Ryūga (PERCEPÇÃO)

```
   15.73 s  vida  8295 × 13273  Ryūga liga o SHINKAI PERCEPÇÃO
   17.67 s  vida  9687 × 12475  rival liga o SHINKAI PERCEPÇÃO
   17.67 s  vida  9687 × 12475  SHINKAI_CLASH
   23.47 s  vida  7687 × 12475  SHINKAI do Ryūga acabou
   23.47 s  vida  7687 × 12475  SHINKAI_CLASH_WIN (rival)
   33.03 s  vida  1862 ×  9956  LIMITE (rival)
   33.25 s  vida  1862 ×  9152  RUPTURA (rival)
   46.70 s  vida     0 ×  2595  rival vence (leituras 0, esquivas 0, precisos 0, Rupturas 0)
```

### Ryūga (lado 1) × Ryūga (PERCEPÇÃO)

```
    8.68 s  vida 11933 ×  9409  rival liga o SHINKAI PERCEPÇÃO
    9.85 s  vida 12650 ×  9421  Ryūga liga o SHINKAI PERCEPÇÃO
    9.85 s  vida 12650 ×  9421  SHINKAI_CLASH
   15.65 s  vida 10650 ×  9421  SHINKAI_CLASH_WIN (Ryūga medido)
   26.00 s  vida  2819 × 10496  LIMITE (Ryūga medido)
   26.68 s  vida  2819 × 10181  RUPTURA (Ryūga medido)
   31.52 s  vida   903 ×  9189  LEITURA (Ryūga medido)
   31.67 s  vida   903 ×  9153  SHINKAI do Ryūga depois da Ruptura (exausto)
   35.53 s  vida     0 ×  9153  Ryūga vence (leituras 1, esquivas 0, precisos 0, Rupturas 1)
```

(7 s)
