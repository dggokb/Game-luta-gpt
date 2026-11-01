# SHINKAI do Ryūga: protótipo (PERCEPÇÃO) × antigo (ENERGY)

CPU nível 8 dos dois lados, 1x1, o Ryūga contra os 15 (32 lutas cada, metade de cada lado), as mesmas sementes em todas as colunas. O rival usa o SHINKAI antigo do tipo dele. Jogo normal (com Clash).

| medida | antigo (ENERGY) | protótipo | sem reação rápida da CPU | sem Ruptura | sem os dois |
|---|---|---|---|---|---|
| Ryūga vence | 57% | 62% | 61% | 63% | 62% |
| SHINKAIs ligados por luta | 0.98 | 1.01 | 1.00 | 1.01 | 1.00 |
| duração média do SHINKAI (s) | 9.8 | 11.9 | 11.9 | 11.0 | 10.9 |
| dano que ele dá por s, no SHINKAI | 254.6 | 325.2 | 304.7 | 314.9 | 295.3 |
| dano que ele leva por s, no SHINKAI | 188.7 | 169.0 | 183.1 | 160.9 | 172.9 |
| saldo por SHINKAI (dá − leva) | 644 | 1852 | 1443 | 1697 | 1333 |
| dano que ele dá / leva por s, fora | 293.9 / 275.1 | 291.9 / 276.1 | 292.9 / 278.8 | 292.1 / 277.1 | 295.2 / 275.3 |
| dano que ele leva por s, no SHINKAI do rival | 305.2 | 281.0 | 286.6 | 281.7 | 286.6 |
| SHINKAI Clash por luta | 0.94 | 0.94 | 0.94 | 0.94 | 0.94 |
| no SHINKAI dele: golpes do rival / defendidos / perfeitas (por luta) | 1.52 / 0.23 / 0.07 | 2.85 / 0.40 / 0.24 | 2.84 / 0.37 / 0.18 | 2.31 / 0.35 / 0.20 | 2.18 / 0.29 / 0.13 |
| leituras / esquivas / precisos por luta | 0.00 / 0.00 / 0.00 | 0.17 / 0.00 / 0.11 | 0.13 / 0.00 / 0.04 | 0.14 / 0.00 / 0.09 | 0.10 / 0.00 / 0.03 |
| dano médio do golpe preciso | 0 | 793 | 782 | 764 | 707 |
| fim: desligou / no limite / Ruptura (total) | 0 / 0 / 0 | 22 / 68 / 84 | 24 / 59 / 94 | 22 / 159 / 0 | 24 / 154 / 0 |
| s exausto por luta (dano levado exausto por s) | 0.0 (0.0) | 0.5 (265.3) | 0.5 (275.5) | 0.5 (299.2) | 0.5 (236.7) |
| dor da Ruptura por luta | 0.00 | 132.24 | 160.65 | 0.00 | 0.00 |

## Por rival (Ryūga vence)

| rival | tipo | antigo (ENERGY) | protótipo | sem reação rápida da CPU | sem Ruptura | sem os dois |
|---|---|---|---|---|---|---|
| p02 | TECHNIQUE | 38% | 56% | 56% | 59% | 59% |
| p03 | SPEED | 50% | 56% | 56% | 59% | 56% |
| p04 | POWER | 47% | 53% | 50% | 53% | 53% |
| p05 | SPEED | 47% | 47% | 50% | 50% | 50% |
| p06 | ENERGY | 72% | 77% | 75% | 84% | 84% |
| p07 | TECHNIQUE | 72% | 72% | 69% | 72% | 69% |
| p08 | ENERGY | 47% | 63% | 59% | 56% | 50% |
| p09 | POWER | 38% | 38% | 41% | 34% | 38% |
| p10 | SPEED | 56% | 53% | 56% | 56% | 59% |
| p11 | ENERGY | 59% | 69% | 63% | 69% | 72% |
| p12 | POWER | 53% | 56% | 50% | 50% | 44% |
| p13 | POWER | 38% | 50% | 41% | 50% | 44% |
| p14 | TECHNIQUE | 78% | 75% | 78% | 81% | 78% |
| p15 | TECHNIQUE | 88% | 91% | 91% | 91% | 91% |
| p16 | SPEED | 78% | 81% | 81% | 81% | 81% |

## Log de 4 lutas (protótipo)

### Ryūga (lado 0) × p03 (SPEED)

```
    9.15 s  vida 11756 × 10181  rival liga o SHINKAI SPEED
   11.40 s  vida  9919 × 11006  Ryūga liga o SHINKAI PERCEPÇÃO
   11.40 s  vida  9919 × 11006  SHINKAI_CLASH
   17.20 s  vida  9919 ×  9006  SHINKAI_CLASH_WIN
   26.83 s  vida  9659 ×  2986  LIMITE
   27.15 s  vida  9343 ×  2986  RUPTURA
   32.55 s  vida  6872 ×  2130  SHINKAI do Ryūga depois da Ruptura (exausto)
   37.68 s  vida  6372 ×     0  Ryūga vence (leituras 0, esquivas 0, precisos 0, Rupturas 1)
```

### Ryūga (lado 1) × p09 (POWER)

```
   31.63 s  vida     0 ×  8998  Ryūga vence (leituras 0, esquivas 0, precisos 0, Rupturas 0)
```

### Ryūga (lado 0) × p07 (TECHNIQUE)

```
    9.45 s  vida 11132 × 11492  Ryūga liga o SHINKAI PERCEPÇÃO
   13.90 s  vida 11896 ×  7444  rival liga o SHINKAI TECHNIQUE
   13.90 s  vida 11896 ×  7444  SHINKAI_CLASH
   19.70 s  vida 11896 ×  5444  SHINKAI_CLASH_WIN
   29.07 s  vida 11370 ×     0  Ryūga vence (leituras 0, esquivas 0, precisos 0, Rupturas 0)
```

### Ryūga (lado 1) × p11 (ENERGY)

```
   21.33 s  vida  9691 ×  6462  Ryūga liga o SHINKAI PERCEPÇÃO
   22.43 s  vida  9703 ×  7254  rival liga o SHINKAI ENERGY
   22.43 s  vida  9703 ×  7254  SHINKAI_CLASH
   28.23 s  vida  9703 ×  5254  SHINKAI do Ryūga acabou
   28.23 s  vida  9703 ×  5254  SHINKAI_CLASH_WIN
   42.88 s  vida  6146 ×     0  rival vence (leituras 0, esquivas 0, precisos 0, Rupturas 0)
```

(9 s)
