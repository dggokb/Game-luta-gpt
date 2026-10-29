# Lista G: tabelas completas do antes e depois

Base: `5cd8ed16`, o HEAD depois das etapas 1–5. Final: `29ca0716`, com G2a, G2b, a correção do contra-golpe, G1a e G1b. As sementes e os cenários são os mesmos nas duas colunas.

## Bateria fixa (BateriaAntesDepois), etapa por etapa

| métrica | base (5cd8ed16) | G2a | G2b | correção + G1a | G1b (final) |
|---|---|---|---|---|---|
| breaker.casos | 111 | 111 | 111 | 117 | 141 |
| breaker.troco_garantido_script | 69% | 69% | 69% | 51% | 57% |
| breaker.combo_inteiro | 1627 | 1627 | 1627 | 1498 | 1740 |
| breaker.dano_evitado | 832 | 832 | 832 | 662 | 924 |
| breaker.troco | 553 | 553 | 553 | 533 | 521 |
| breaker.sempre_vs_nunca | 57% | 57% | 59% | 68% | 66% |
| breaker.cpu_vs_nunca | 49% | 51% | 53% | 60% | 57% |
| breaker.cpu_quebras_por_luta | 0.76 | 1.04 | 0.97 | 0.99 | 1.09 |
| breaker.cpu_quebra_das_chances | 29% | 30% | 30% | 34% | 37% |
| clash.cpu_vence.POWER | 47% | 47% | 47% | 47% | 47% |
| clash.cpu_vence.SPEED | 49% | 49% | 49% | 49% | 49% |
| clash.cpu_vence.ENERGY | 40% | 40% | 40% | 40% | 40% |
| clash.cpu_vence.TECHNIQUE | 55% | 55% | 55% | 55% | 55% |
| shinkai.2x2_vence.POWER | 53% | 52% | 52% | 52% | 57% |
| shinkai.2x2_vence.SPEED | 54% | 51% | 54% | 54% | 55% |
| shinkai.2x2_vence.ENERGY | 50% | 48% | 52% | 49% | 49% |
| shinkai.2x2_vence.TECHNIQUE | 45% | 46% | 44% | 54% | 56% |
| tag.janela_quadros | 43 | 43 | 43 | 43 | 43 |
| tag.latencia_tag_cedo | 33 | 33 | 33 | 33 | 33 |
| tag.falha_com_rival_batendo | 0% | 0% | 0% | 0% | 0% |
| tag.cpu_pedidos | 1989 | 1887 | 1846 | 1893 | 1938 |
| tag.cpu_perdidos | 7% | 7% | 7% | 8% | 7% |
| 1x1.player_base | 70% | 70% | 70% | 65% | 64% |
| 1x1.p02 | 44% | 44% | 44% | 44% | 46% |
| 1x1.p03 | 60% | 60% | 60% | 58% | 63% |
| 1x1.p04 | 56% | 56% | 56% | 64% | 64% |
| 1x1.p05 | 43% | 43% | 43% | 49% | 54% |
| 1x1.p06 | 69% | 69% | 69% | 60% | 53% |
| 1x1.p07 | 15% | 15% | 15% | 21% | 28% |
| 1x1.p08 | 79% | 79% | 79% | 78% | 79% |
| 1x1.p09 | 62% | 62% | 62% | 60% | 63% |
| 1x1.p10 | 58% | 58% | 58% | 64% | 59% |
| 1x1.p11 | 56% | 56% | 56% | 61% | 61% |
| 1x1.p12 | 46% | 46% | 46% | 44% | 47% |
| 1x1.p13 | 57% | 57% | 57% | 53% | 55% |
| 1x1.p14 | 39% | 39% | 39% | 40% | 32% |
| 1x1.p15 | 17% | 17% | 17% | 14% | 8% |
| 1x1.p16 | 29% | 29% | 29% | 25% | 26% |
| 1x1.p09_vs_p02 | 68% | 68% | 68% | 62% | 57% |
| 2x2.player_base | 65% | 69% | 68% | 64% | 60% |
| 2x2.p02 | 42% | 44% | 48% | 48% | 45% |
| 2x2.p03 | 51% | 46% | 49% | 61% | 46% |
| 2x2.p04 | 59% | 54% | 50% | 53% | 57% |
| 2x2.p05 | 39% | 45% | 44% | 46% | 50% |
| 2x2.p06 | 59% | 54% | 49% | 53% | 53% |
| 2x2.p07 | 31% | 32% | 30% | 39% | 41% |
| 2x2.p08 | 74% | 68% | 68% | 59% | 59% |
| 2x2.p09 | 75% | 65% | 69% | 62% | 66% |
| 2x2.p10 | 50% | 49% | 47% | 53% | 50% |
| 2x2.p11 | 54% | 46% | 45% | 53% | 54% |
| 2x2.p12 | 48% | 54% | 54% | 52% | 55% |
| 2x2.p13 | 53% | 58% | 60% | 52% | 55% |
| 2x2.p14 | 30% | 40% | 39% | 37% | 37% |
| 2x2.p15 | 25% | 30% | 29% | 20% | 23% |
| 2x2.p16 | 42% | 31% | 33% | 40% | 31% |
| p09.vs_p02_400 | 68% | 68% | 68% | 67% | 69% |
| p09.dano_por_acerto | 748 | 748 | 748 | 749 | 735 |
| p09.parte_agarroes | 7% | 7% | 7% | 8% | 5% |
| p09.parte_2M | 19% | 19% | 19% | 19% | 22% |
| combo.max_0barras.player_base | 7390 | 7390 | 7390 | 7390 | 7390 |
| combo.max_1barra.player_base | 8465 | 8465 | 8465 | 8465 | 8465 |
| combo.assist_2quiques_0barras.player_base | 6039 | 6039 | 6039 | 6039 | 6039 |
| combo.max_0barras.p02 | 4798 | 4798 | 4798 | 4798 | 4798 |
| combo.max_1barra.p02 | 4288 | 4288 | 4288 | 4288 | 4288 |
| combo.assist_2quiques_0barras.p02 | 6830 | 6830 | 6830 | 6830 | 6830 |
| combo.max_0barras.p03 | 5718 | 5718 | 5718 | 5718 | 5718 |
| combo.max_1barra.p03 | 4669 | 4669 | 4669 | 4669 | 4669 |
| combo.assist_2quiques_0barras.p03 | 6789 | 6789 | 6789 | 6789 | 6789 |
| combo.max_0barras.p04 | 5552 | 5552 | 5552 | 5552 | 5552 |
| combo.max_1barra.p04 | 5408 | 5408 | 5408 | 5408 | 5408 |
| combo.assist_2quiques_0barras.p04 | 7298 | 7298 | 7298 | 7298 | 7298 |
| combo.max_0barras.p05 | 6041 | 6041 | 6041 | 6041 | 6041 |
| combo.max_1barra.p05 | 4629 | 4629 | 4629 | 4629 | 4629 |
| combo.assist_2quiques_0barras.p05 | 5736 | 5736 | 5736 | 5736 | 5736 |
| combo.max_0barras.p06 | 6005 | 6005 | 6005 | 6005 | 6005 |
| combo.max_1barra.p06 | 3922 | 3922 | 3922 | 3922 | 3922 |
| combo.assist_2quiques_0barras.p06 | 5509 | 5509 | 5509 | 5509 | 5509 |
| combo.max_0barras.p07 | 7385 | 7385 | 7385 | 7385 | 7385 |
| combo.max_1barra.p07 | 6481 | 6481 | 6481 | 6481 | 6481 |
| combo.assist_2quiques_0barras.p07 | 7064 | 7064 | 7064 | 7064 | 7064 |
| combo.max_0barras.p08 | 6846 | 4320 | 4320 | 4320 | 4320 |
| combo.max_1barra.p08 | 4339 | 4339 | 4339 | 4339 | 4339 |
| combo.assist_2quiques_0barras.p08 | 0 | 0 | 0 | 0 | 0 |
| combo.max_0barras.p09 | 6924 | 6924 | 6924 | 6924 | 6924 |
| combo.max_1barra.p09 | 7906 | 7906 | 7906 | 7906 | 7906 |
| combo.assist_2quiques_0barras.p09 | 0 | 0 | 0 | 0 | 0 |
| combo.max_0barras.p10 | 6407 | 6407 | 6407 | 6407 | 6407 |
| combo.max_1barra.p10 | 4730 | 4730 | 4730 | 4730 | 4730 |
| combo.assist_2quiques_0barras.p10 | 6376 | 6376 | 6376 | 6376 | 6376 |
| combo.max_0barras.p11 | 4407 | 4701 | 4701 | 4701 | 4701 |
| combo.max_1barra.p11 | 6219 | 6409 | 6409 | 6409 | 6409 |
| combo.assist_2quiques_0barras.p11 | 0 | 0 | 0 | 0 | 0 |
| combo.max_0barras.p12 | 5926 | 5926 | 5926 | 5926 | 5926 |
| combo.max_1barra.p12 | 5751 | 5751 | 5751 | 5751 | 5751 |
| combo.assist_2quiques_0barras.p12 | 7521 | 7521 | 7521 | 7521 | 7521 |
| combo.max_0barras.p13 | 7115 | 7543 | 7543 | 7543 | 7543 |
| combo.max_1barra.p13 | 7115 | 7543 | 7543 | 7543 | 7543 |
| combo.assist_2quiques_0barras.p13 | 6858 | 0 | 0 | 0 | 0 |
| combo.max_0barras.p14 | 5737 | 6783 | 6783 | 6783 | 6783 |
| combo.max_1barra.p14 | 5649 | 6167 | 6167 | 6167 | 6167 |
| combo.assist_2quiques_0barras.p14 | 5737 | 5048 | 5048 | 5048 | 5048 |
| combo.max_0barras.p15 | 5700 | 5700 | 5700 | 5700 | 5700 |
| combo.max_1barra.p15 | 3840 | 3840 | 3840 | 3840 | 3840 |
| combo.assist_2quiques_0barras.p15 | 0 | 0 | 0 | 0 | 0 |
| combo.max_0barras.p16 | 6707 | 6777 | 6777 | 6777 | 6777 |
| combo.max_1barra.p16 | 6707 | 5615 | 5615 | 5615 | 5615 |
| combo.assist_2quiques_0barras.p16 | 6317 | 5777 | 5777 | 5777 | 5777 |
| combo.rota_ryuga_0barras | 0 | 0 | 0 | 0 | 0 |
| combo.rota_ryuga_barra_ganha | 0 | 0 | 0 | 0 | 0 |
| combo.rota_ryuga_supers | 0 | 0 | 0 | 0 | 0 |
| combo.barra_ganha_media_0barras | 1422 | 1444 | 1444 | 1444 | 1444 |
| combo.supers_media_0barras | 1 | 1 | 1 | 1 | 1 |
| combo.shinkai_ENERGY.player_base | 10308 | 10308 | 10308 | 10308 | 10308 |
| combo.shinkai_ENERGY.p09 | 10362 | 10362 | 10362 | 10362 | 10362 |
| combo.shinkai_TECHNIQUE.player_base | 9598 | 9598 | 9598 | 9598 | 9598 |
| combo.shinkai_TECHNIQUE.p09 | 9153 | 9153 | 9153 | 9153 | 9153 |
| combo.shinkai_POWER.player_base | 7550 | 7550 | 7550 | 7550 | 7550 |
| combo.shinkai_POWER.p09 | 8040 | 8040 | 8040 | 8040 | 8040 |
| combo.shinkai_SPEED.player_base | 9334 | 9334 | 9334 | 9334 | 9334 |
| combo.shinkai_SPEED.p09 | 8654 | 8654 | 8654 | 8654 | 8654 |

## Força contra o elenco (matriz 16 × 16, nível 8, 10 sementes por célula)

| personagem | base | final | Δ |
|---|---|---|---|
| p08 | 77% | 78% | +1 |
| p04 | 63% | 64% | +1 |
| p09 | 58% | 64% | +6 |
| p11 | 61% | 63% | +2 |
| p10 | 60% | 63% | +3 |
| Ryūga (p01) | 67% | 61% | -6 |
| Kōga (p03) | 60% | 58% | -2 |
| p13 | 62% | 55% | -7 |
| p06 | 68% | 51% | -17 |
| p05 | 41% | 51% | +10 |
| p12 | 43% | 47% | +4 |
| Yin-lu (p02) | 37% | 45% | +8 |
| p07 | 16% | 32% | +16 |
| p14 | 41% | 31% | -10 |
| p16 | 30% | 24% | -6 |
| p15 | 17% | 13% | -4 |

faixa base 16 – 77 ; final 13 – 78
desvio padrão base 17.8 ; final 16.8

## Uso dos especiais pela CPU e vitórias (seção 6, 1x1 contra os outros 15)

| lutador | vence | S1/min | S2–S4/min | agarrões/min | dano de S2–S4 |
|---|---|---|---|---|---|
| Ryūga (p01) | 60% → 72% | 19.2 → 19.5 | 0.0 → 2.4 | 0.1 | 0% → 6% |
| p15 | 12% → 10% | 10.9 → 10.5 | 0.0 → 2.3 | 0.0 | 0% → 4% |
| Yin-lu (p02) | 52% → 48% | 10.6 → 11.3 | 0.0 → 0.3 | 0.0 → 0.1 | 0% → 2% |
| p14 | 43% → 33% | 0.0 → 0.7 | 0.0 → 0.3 | 0.0 | 0% → 2% |
| Kōga (p03) | 57% → 62% | 8.1 → 12.2 | 0.0 → 0.9 | 0.1 | 0% → 2% |
| p04 | 57% | 8.6 → 7.4 | 0.4 → 3.8 | 0.1 → 0.0 | 1% → 12% |
| p10 | 57% → 65% | 12.5 → 9.9 | 0.0 → 3.8 | 0.1 | 0% → 10% |
| p06 | 70% → 52% | 25.0 → 21.9 | 0.0 → 2.7 | 0.0 | 0% → 7% |
| p08 | 87% → 73% | 32.1 → 30.1 | 0.0 → 0.6 | 0.0 | 0% → 2% |
| p11 | 47% → 67% | 29.6 → 30.8 | 0.2 → 2.4 | 0.0 → 0.1 | 0% → 9% |
| p07 | 13% → 32% | 1.3 → 4.7 | 0.0 → 3.3 | 0.1 → 0.0 | 0% → 18% |
| p09 | 58% → 62% | 5.5 → 5.4 | 0.4 → 0.8 | 0.8 → 0.4 | 1% → 5% |
| p13 | 60% → 55% | 7.4 → 6.1 | 0.0 → 1.7 | 0.1 → 0.0 | 0% → 5% |
| p05 | 32% → 57% | 14.5 → 13.5 | 0.0 → 4.9 | 0.0 | 0% → 21% |
| p12 | 50% → 42% | 8.5 → 8.9 | 0.0 → 2.1 | 0.0 | 0% → 6% |
| p16 | 25% → 28% | 9.4 → 8.0 | 0.0 → 2.7 | 0.2 → 0.0 | 0% → 8% |

## T12 e T15–T21 (BateriaEquipe): base → final

### T12 Combo Breaker

#### Altura do combo e lugar (16 atacantes × 10 sementes; boneco com 1 barra, sem defender)

| quebra | lugar | quebras | levou antes | dano evitado | troco | troco ≥ 1 combo de 3 acertos |
|---|---|---|---|---|---|---|
| no 2º acerto | meio | 141 → 158 | 841 → 761 | 798 → 1016 | 521 → 537 | 8% |
| no 3º acerto | meio | 96 → 144 | 1291 → 1273 | 545 → 556 | 559 → 695 | 18% → 22% |
| no 4º acerto | meio | 68 → 92 | 1786 → 1688 | 206 → 316 | 696 → 574 | 25% → 18% |
| no ar | meio | 92 → 136 | 1387 → 1423 | 467 → 423 | 735 → 725 | 37% → 29% |
| no 2º acerto | canto | 98 → 123 | 987 → 871 | 892 → 1026 | 579 → 590 | 11% |
| no ar | canto | 64 → 86 | 1364 → 1395 | 817 → 766 | 636 → 699 | 20% → 22% |

#### A/B: regra atual × "volta ao neutro" (lutas 2x2 espelhadas, 240 por linha)

| regra | atordoamento | corrida automática | troco médio | sempre quebra vence nunca quebra |
|---|---|---|---|---|
| atual | 44 quadros | 40 quadros | 1600 → 1188 | 69% → 70% |
| meio-termo | 24 quadros | não | 593 → 551 | 66% |
| volta ao neutro | 12 quadros | não | 699 → 413 | 67% → 66% |

Controle (nunca × nunca, mesmas sementes): 50%.

### T15 Valor dos quatro SHINKAI (F02 ENERGY, F03 eco SPEED)

1x1 espelhado nos 16 lutadores (15 sementes cada): o lado A liga o SHINKAI no GO, o lado B (mesma CPU)
não tem. Janela de 8 s + o brilho. "sem" = a mesma janela sem ligar nada.

| tipo | dano de A | dano em A | saldo | saldo a mais que sem | barra ganha (A) | da barra passiva | Supers de A | vida que voltou | distância média | defesas de B |
|---|---|---|---|---|---|---|---|---|---|---|
| sem | 1983 → 2341 | 2168 → 2004 | -185 → +337 | – | 1.04 barra → 1.15 barra | 0.00 barra | 0.07 → 0.15 | 27 → 17 | 307 px → 314 px | 1.1 → 0.9 |
| POWER | 4169 → 4173 | 930 → 942 | +3238 → +3231 | +3424 → +2893 | 1.17 barra → 1.29 barra | 0.00 barra | 1.12 → 0.95 | 257 → 260 | 310 px → 318 px | 0.6 → 0.7 |
| SPEED | 3691 → 3501 | 1096 → 1283 | +2595 → +2218 | +2780 → +1881 | 1.15 barra → 1.19 barra | 0.00 barra | 1.16 → 1.05 | 225 → 242 | 281 px → 292 px | 0.5 |
| ENERGY | 2984 → 2930 | 1775 → 1893 | +1209 → +1037 | +1395 → +700 | 2.95 barra → 2.97 barra | 1.88 barra | 0.98 → 1.00 | 490 → 495 | 321 px → 326 px | 0.9 → 1.0 |
| TECHNIQUE | 2787 → 2764 | 1324 → 1459 | +1463 → +1305 | +1649 → +968 | 0.83 barra → 0.88 barra | 0.00 barra | 0.71 → 0.72 | 351 → 372 | 310 px → 325 px | 1.4 |

F03 eco do SPEED: dano de A em 8 s 3501 com o eco, 3155 com o eco sem dano (+346, 11%).
F02 conta do ENERGY: 4 por quadro × 480 quadros = 1.92 barra (o relógio do SHINKAI para nos congelamentos).

### T16 Resposta contra cada SHINKAI

A (CPU nível 8) liga o SHINKAI no GO; B responde por 8 s. Dano que B leva / que B dá (16 lutadores × 6 sementes).
"sem" = A sem SHINKAI, mesma resposta. No canto, B começa encostado na parede.

##### No meio

| resposta de B | sem | POWER | SPEED | ENERGY | TECHNIQUE |
|---|---|---|---|---|---|
| CPU nível 8 | 2152 / 1881 → 2155 / 2121 | 4581 / 974 → 4385 / 956 | 3556 / 1388 → 3637 / 1567 | 3131 / 1839 → 3071 / 1907 | 2800 / 1430 → 3040 / 1426 |
| defesa perfeita | 747 / 0 → 807 / 0 | 1107 / 0 → 948 / 0 | 1358 / 0 → 1505 / 0 | 1271 / 0 → 1289 / 0 | 1107 / 0 → 958 / 0 |
| fugir (trás + backdash) | 816 / 0 → 623 / 0 | 942 / 0 → 975 / 0 | 1129 / 0 → 1147 / 0 | 1118 / 0 → 1096 / 0 | 942 / 0 → 966 / 0 |
| agarrão de perto | 693 / 92 → 784 / 190 | 1137 / 94 → 1095 / 63 | 1583 / 271 → 1873 / 365 | 1280 / 106 → 1379 / 60 | 1117 / 94 → 1085 / 63 |
| parado | 5276 / 0 → 4885 / 0 | 6028 / 0 → 5721 / 0 | 5962 / 0 → 6087 / 0 | 6728 / 0 → 6559 / 0 | 6414 / 0 → 6195 / 0 |
| TAG na hora | 747 / 0 → 807 / 0 | 1645 / 430 → 1664 / 460 | 1347 / 556 → 1414 / 583 | 1152 / 632 → 1002 / 647 | 1312 / 217 → 1093 / 389 |

##### No canto

| resposta de B | sem | POWER | SPEED | ENERGY | TECHNIQUE |
|---|---|---|---|---|---|
| CPU nível 8 | 2259 / 2002 → 2366 / 2345 | 4720 / 942 → 5396 / 944 | 4230 / 1201 → 3683 / 1529 | 3750 / 1671 → 4122 / 1905 | 3538 / 1303 → 3632 / 1672 |
| defesa perfeita | 770 / 0 → 991 / 0 | 1445 / 0 → 1751 / 0 | 1984 / 0 → 2460 / 0 | 1185 / 0 → 1519 / 0 | 1390 / 0 → 1629 / 0 |
| fugir (trás + backdash) | 1067 / 0 → 1082 / 0 | 1780 / 0 → 1997 / 0 | 2118 / 0 → 2374 / 0 | 1385 / 0 → 1619 / 0 | 1718 / 0 → 1802 / 0 |
| agarrão de perto | 879 / 104 → 1204 / 206 | 1643 / 140 → 1746 / 244 | 2596 / 350 → 2751 / 426 | 1264 / 165 → 1530 / 246 | 1595 / 152 → 1653 / 244 |
| parado | 5235 / 0 → 5229 / 0 | 6469 / 0 → 6503 / 0 | 6812 / 0 → 6807 / 0 | 7021 / 0 → 6563 / 0 | 6841 / 0 → 6681 / 0 |
| TAG na hora | 770 / 0 → 991 / 0 | 2098 / 434 → 2211 / 423 | 2245 / 540 → 2281 / 589 | 1482 / 668 → 1774 / 684 | 1556 / 308 → 1858 / 218 |


### T17 Função das assistências (F06)

O assist de cada um, chamado pelo Ryūga (o assist do próprio Ryūga é chamado pela Yin-lu).
Início = quadros do aperto até o 1º acerto no boneco mais perto que ele alcança. Alcance = distâncias (do titular) em
que acerta o boneco parado, de 100 a 1000 px (o assist aparece 70 px atrás do titular). Antiaéreo = em quantos de 12 tempos
de chamada ele acerta o boneco que fica pulando parado a 250 px (cobertura vertical, não um antiaéreo de reação).
Cobertura = vantagem do titular num H defendido sem / com o assist chamado junto. O assist não tem corpo que o rival
acerte (CombatEngine só testa o golpe dele contra o rival), então ele mesmo nunca é punido.

| lutador | golpe | tipo | início | alcance | dano | hitstun | blockstun | antiaéreo | cobertura (H defendido) | função |
|---|---|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | S (S) | projétil | 15 q | 100–800 px | 850 | 20 | 14 | 75% | -2 / -2 | projétil com antiaéreo |
| Yin-lu (p02) | S (S) | projétil | 18 q | 100–350 px | 760 | 31 | 25 | 50% | -2 / +13 | antiaéreo |
| Kōga (p03) | S (S) | projétil | 19 q | 100–500 px | 850 | 20 | 14 | 42% | -2 / -1 | investida / alcance médio |
| p04 | S (S) | projétil | 19 q | 100–350 px | 890 | 30 | 24 | 25% | -2 / +8 | extensão de perto |
| p05 | S (S) | projétil | 17 q | 100–1000 px | 760 | 29 | 23 | 33% | -2 / +6 | projétil (controle de chão) |
| p06 | S (S) | projétil baixo | 19 q | 100–850 px | 720 | 31 | 25 | 50% | -2 / +25 | projétil com antiaéreo |
| p07 | S (S) | corpo a corpo | 21 q | 100–150 px → 100–450 px | 1020 | 31 | 25 | 0% → 92% | -2 / +11 | extensão de perto → antiaéreo |
| p08 | S (S) | projétil | 19 q | 100–750 px | 810 | 26 | 20 | 83% | -2 / +6 | projétil com antiaéreo |
| p09 | S (S) | projétil | 22 q | 100–1000 px | 1060 | 31 | 25 | 75% | -2 / +12 | projétil com antiaéreo |
| p10 | S (S) | projétil | 17 q | 100–800 px | 720 | 27 | 21 | 58% | -2 / +4 | projétil com antiaéreo |
| p11 | S (S) | projétil | 19 q | 100–1000 px | 680 | 20 | 14 | 92% | -2 / -1 | projétil com antiaéreo |
| p12 | S (S) | corpo a corpo | 19 q | 100–100 px → 100–500 px | 940 | 31 | 0 → 25 | 0% → 33% | -2 / -2 → -2 / +9 | extensão de perto → investida / alcance médio |
| p13 | S (S) → H (H) | corpo a corpo | 27 q → 21 q | 100–100 px → 100–500 px | 264 → 960 | 29 → 32 | 0 → 25 | 0% → 92% | -2 / -2 → -2 / +14 | extensão de perto → antiaéreo |
| p14 | S (S) | corpo a corpo | – → 21 q | – → 100–550 px | 0 → 1140 | 0 → 20 | 0 | 0% → 92% | -2 / -2 → -2 / +24 | não acerta parado → antiaéreo |
| p15 | M (M) → 2H (2H) | corpo a corpo | 17 q → 19 q | 100–100 px → 100–400 px | 500 → 800 | 23 → 48 | 0 → 19 | 0% → 100% | -2 / -2 → -2 / +6 | extensão de perto → antiaéreo |
| p16 | S (S) | projétil | 35 q | 100–1000 px | 850 | 20 | 14 | 83% | -2 / +16 | projétil com antiaéreo |

### T18 Assistência nos combos

Busca em feixe (largura 40, 14 passos, sem Super, sem barra) do maior combo de cada titular com cada parceiro;
"sem assist" é a mesma busca proibindo o assist. Ganho = maior com assist − maior sem assist.

##### Titular Ryūga (p01) (maior sem assist: 5914)

| parceiro | maior combo com assist | ganho | rota |
|---|---|---|---|
| p07 → p12 | 5883 → 6158 | -31 → +244 | H 2L A 2H J jM jH jM jH j2H S M S H → H A S H J 2H J jH S jH j2H S M 2H |
| p08 → p14 | 5837 → 5985 | -77 → +71 | H M A 2H J jM jH j2H jH S j2H M H A → H 2L A 2H J jM jH j2H jH j2H S M S H |
| p15 | 5822 → 5888 | -92 → -26 | H A J S j2H S H J 2H J jH S jH j2H → H A S J j2H S jM S M 2H J jM jH S |
| Yin-lu (p02) (oficial) → p07 | 5779 → 5883 | -135 → -31 | H M A 2H J jM jH j2H S jH j2H M H A → H 2L A 2H J jM jH jM jH j2H S M S H |
| p12 → p08 | 5733 → 5837 | -181 → -77 | M S A H J 2H J jH S jH S j2H jM H → H M A 2H J jM jH j2H jH S j2H M H A |
| p13 → Yin-lu (p02) (oficial) | 5733 → 5779 | -181 → -135 | M S A H J 2H J jH S jH S j2H jM H → H M A 2H J jM jH j2H S jH j2H M H A |
| p14 → p09 | 5733 → 3971 | -181 → -1943 | M S A H J 2H J jH S jH S j2H jM H → H M A M H S 2L S |
| p09 → p04 | 3971 → 3742 | -1943 → -2172 | H M A M H S 2L S → H A 2L M H S 2L S |
| p04 → p13 | 3742 → 3713 | -2172 → -2201 | H A 2L M H S 2L S → M H A S M S D 2L |
| Kōga (p03) | 3704 | -2210 | H A 2L M H S 2L S |
| p16 | 3659 | -2255 | H 2L A M H S 2L S |
| p05 | 3619 | -2295 | H A 2L M H S 2L S |
| p06 | 3581 | -2333 | H A 2L M H S 2L S |
| p10 | 3581 | -2333 | H A 2L M H S 2L S |
| p11 | 3544 | -2370 | H M A 2L H S 2L S |

##### Titular Kōga (p03) (maior sem assist: 5332)

| parceiro | maior combo com assist | ganho | rota |
|---|---|---|---|
| p09 → p12 | 5957 → 6120 | +625 → +788 | H A 2L 2H J jM jH j2H jH jM j2H S H A → H A 2H J jM jH j2H jH jM j2H S H A 2H |
| p06 → p09 | 5868 → 5957 | +536 → +625 | H A H J M 2H J jH S jH j2H S H A → H A 2L 2H J jM jH j2H jH jM j2H S H A |
| Yin-lu (p02) → p15 | 5830 → 5930 | +498 → +598 | H A 2L H J M 2H J jH S jM j2H A H → H S A J j2H S W14 2H J jH j2H jH jM S |
| Ryūga (p01) → p06 | 5780 → 5868 | +448 → +536 | H 2L A H J M 2H J jH S jH j2H A 2H → H A H J M 2H J jH S jH j2H S H A |
| p11 → Yin-lu (p02) | 5730 → 5830 | +398 → +498 | H A 2L H J M 2H J jH S jM j2H A H |
| p07 → p14 | 5715 → 5823 | +383 → +491 | M 2H A J jM jH j2H jH j2H M H W14 H S |
| p04 (oficial) → Ryūga (p01) | 5614 → 5780 | +282 → +448 | H 2L A 2H J jM jH jM jH j2H S 2H J j2H → H 2L A H J M 2H J jH S jH j2H A 2H |
| p16 → p11 | 5575 → 5730 | +243 → +398 | H 2L A 2H J jM jH j2H jH j2H J S j2H H → H A 2L H J M 2H J jH S jM j2H A H |
| p05 → p07 | 5559 → 5715 | +227 → +383 | H 2L A 2H J jM jH j2H jH jM j2H S H A → M 2H A J jM jH j2H jH j2H M H W14 H S |
| p08 → p04 (oficial) | 5542 → 5614 | +210 → +282 | H 2L A 2H J jM jH jM jH j2H S 2H J j2H |
| p10 → p16 | 5511 → 5575 | +179 → +243 | H 2L A 2H J jM jH j2H jH jM j2H S H A → H 2L A 2H J jM jH j2H jH j2H J S j2H H |
| p15 → p05 | 5388 → 5559 | +56 → +227 | H A 2H J jM jH jM jH j2H S 2H J j2H → H 2L A 2H J jM jH j2H jH jM j2H S H A |
| p12 → p08 | 5220 → 5542 | -112 → +210 | H 2L 2H A J jM jH jM jH j2H S 2L H S → H 2L A 2H J jM jH jM jH j2H S 2H J j2H |
| p13 → p10 | 5220 → 5511 | -112 → +179 | H 2L 2H A J jM jH jM jH j2H S 2L H S → H 2L A 2H J jM jH j2H jH jM j2H S H A |
| p14 → p13 | 5220 → 3291 | -112 → -2041 | H 2L 2H A J jM jH jM jH j2H S 2L H S → M H A H S D 2L |

##### Titular p09 (maior sem assist: 3660)

| parceiro | maior combo com assist | ganho | rota |
|---|---|---|---|
| p04 → p12 | 7122 → 7293 | +3462 → +3633 | H A H J 2H J jM S j2H jH M 2L A 2H → H S A H J 2H J jM S j2H jH M S A |
| Kōga (p03) → p13 | 7072 → 7275 | +3412 → +3615 | H A H J 2H J jM S j2H jH M 2L A 2H → H S A J 2H J jM S j2H jH L M 2H A |
| p08 → p04 | 7066 → 7122 | +3406 → +3462 | H A H J 2H J jH W14 S j2H jH M 2H A → H A H J 2H J jM S j2H jH M 2L A 2H |
| p06 → Kōga (p03) | 6997 → 7072 | +3337 → +3412 | M H A 2H J jH W14 S j2H jH M H A H → H A H J 2H J jM S j2H jH M 2L A 2H |
| p11 → p08 | 6901 → 7066 | +3241 → +3406 | H A H J 2H J jH W14 S j2H jH M 2H A |
| p05 → p06 | 6705 → 6997 | +3045 → +3337 | H 2L A M H J 2H J jM S j2H jH M 2H → M H A 2H J jH W14 S j2H jH M H A H |
| p10 (oficial) → p11 | 6669 → 6901 | +3009 → +3241 | H 2L A M H J 2H J jM S j2H jH M 2H → H A H J 2H J jH W14 S j2H jH M 2H A |
| p07 → p05 | 6177 → 6705 | +2517 → +3045 | M 2H A J jM jL jM j2H jH M H J H S → H 2L A M H J 2H J jM S j2H jH M 2H |
| p15 → p10 (oficial) | 4085 → 6669 | +425 → +3009 | H A M H S D S → H 2L A M H J 2H J jM S j2H jH M 2H |
| p16 → p14 | 4034 → 6285 | +374 → +2625 | H 2L H A S D S → M 2H A J jM jL jM j2H jH M H J H S |
| Yin-lu (p02) → p07 | 3911 → 6177 | +251 → +2517 | H 2L A H S D S → M 2H A J jM jL jM j2H jH M H J H S |
| Ryūga (p01) → p15 | 3741 → 6025 | +81 → +2365 | H H A S D M → H S A SJ jM jL j2H jL jM H J H S |
| p12 → p16 | 3434 → 4034 | -226 → +374 | H H S A S → H 2L H A S D S |
| p13 → Yin-lu (p02) | 3434 → 3911 | -226 → +251 | H H S A S → H 2L A H S D S |
| p14 → Ryūga (p01) | 3434 → 3741 | -226 → +81 | H H S A S → H H A S D M |

##### Titular p11 (maior sem assist: 2615)

| parceiro | maior combo com assist | ganho | rota |
|---|---|---|---|
| p09 | 5413 | +2798 | M H A 2H J jH S jH jM j2H 2M S 2H A |
| p04 → p13 | 5209 → 5364 | +2594 → +2749 | M H A 2H J jH S jH jM j2H 2M S A H → M A S 2H J jH S jH jM j2H 2M S 2H A |
| Ryūga (p01) → p04 | 5161 → 5209 | +2546 → +2594 | M H A 2H J jH S jH jM j2H 2M S 2H A → M H A 2H J jH S jH jM j2H 2M S A H |
| Kōga (p03) → p12 (oficial) | 5161 → 5170 | +2546 → +2555 | M H A 2H J jH S jH jM j2H 2M S A H → H A 2H J jM jH j2H jH j2H H S M A S |
| p08 → Ryūga (p01) | 5113 → 5161 | +2498 → +2546 | M H A 2H J jH S jH jM j2H 2M S A H → M H A 2H J jH S jH jM j2H 2M S 2H A |
| Yin-lu (p02) → Kōga (p03) | 5053 → 5161 | +2438 → +2546 | M H A 2H J jH S jH jM j2H 2M S 2H A → M H A 2H J jH S jH jM j2H 2M S A H |
| p05 → p08 | 5053 → 5113 | +2438 → +2498 | M H A 2H J jH S jH jM j2H 2M S 2H A → M H A 2H J jH S jH jM j2H 2M S A H |
| p10 → Yin-lu (p02) | 5005 → 5053 | +2390 → +2438 | M H A 2H J jH S jH jM j2H 2M S 2H A |
| p07 → p05 | 4943 → 5053 | +2328 → +2438 | M 2H A J jM jH j2H jH j2H 2M H S M H → M H A 2H J jH S jH jM j2H 2M S 2H A |
| p15 → p10 | 4695 → 5005 | +2080 → +2390 | H A 2H J jM jH S j2H jL 2H J jH S j2H → M H A 2H J jH S jH jM j2H 2M S 2H A |
| p16 → p14 | 3071 → 5004 | +456 → +2389 | H 2L A M H S 2L S → M 2H A J jM jH j2H jH jM j2H 2M H S H |
| p06 → p07 | 3009 → 4943 | +394 → +2328 | H A M 2L H S 2L S → M 2H A J jM jH j2H jH j2H 2M H S M H |
| p12 (oficial) → p15 | 2341 → 3202 | -274 → +587 | H 2L A H S 2L S → H S A H 2M S D S |
| p13 → p16 | 2341 → 3071 | -274 → +456 | H 2L A H S 2L S → H 2L A M H S 2L S |
| p14 → p06 | 2341 → 3009 | -274 → +394 | H 2L A H S 2L S → H A M 2L H S 2L S |


### T19 Segurança do TAG

#### TAG direto (↓+TAG): Ryūga sai, a Yin-lu entra na voadora. Vantagem: + quem entrou age primeiro;
"–" = o rival ainda estava no chão/atordoado 1 s depois.

| distância | rival | voadora | quadros do aperto até ela acabar | vantagem de quem entrou |
|---|---|---|---|---|
| 150 | parado | acertou | 69 | – |
| 150 | defendendo | defendida | 63 | +3 |
| 150 | backdash | acertou | 69 | – |
| 350 | parado | acertou | 69 | – |
| 350 | defendendo | defendida | 63 | +5 |
| 350 | backdash | errou | 54 | +0 |
| 600 | parado | errou | 54 | +0 |
| 600 | defendendo | errou | 54 | +0 |
| 600 | backdash | errou | 54 | +0 |
| 900 | parado | errou | 54 | +0 |
| 900 | defendendo | errou | 54 | +0 |
| 900 | backdash | errou | 54 | +0 |

#### Assist → TAG perdido: o que o titular fazia no quadro em que o assist acabou (lutas CPU × CPU, 400)

2502 pedidos, 0 perdidos (0%). Situação do titular nos perdidos: {}.

Guard Cancel TAG nas mesmas lutas: 788 (1.97 por luta), acertou em 62%, quem entrou levou golpe em até 1,5 s em 39%. DHC: 256.

#### TAG apertado no congelamento do acerto (H acertando o boneco; aperto em cada quadro do congelamento)

| botão | apertos no congelamento | saiu | quadros até sair |
|---|---|---|---|
| TAG (assist) | 14 | 14 | 1.0 |
| ↓+TAG (troca) | 14 | 0 | – |

#### Assist → TAG sem pressão (TAG k quadros depois do assist; boneco parado)

| k | troca | quadros até a Yin-lu agir |
|---|---|---|
| 1 | sim | 33 |
| 10 | sim | 24 |
| 20 | sim | 14 |
| 31 | sim | 3 |
| 32 | sim | 2 |
| 34 | sim | 1 |
| 40 | sim | 1 |
| 44 | não | – |

### T20 Eficiência do DHC

Mesmo início (M H) no boneco, com 3 barras: Super sozinho, Super + DHC (TAG no Super já solto) e Ultra.
Congelado = quadros de tela parada (o Ultra conta só a parte no motor; a página do mangá fica fora).

| dupla | fecho | dano | acertos | barras gastas | vida recuperável do rival | congelado | distância final | rival da parede |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) + Yin-lu (p02) | Super | 4140 | 7 | 1.0 | 1115 | 34 | 263 px | 1073 px |
| Ryūga (p01) + Yin-lu (p02) | Super + DHC | 4358 | 9 | 2.0 | 1173 | 45 | 102 px | 1081 px |
| Ryūga (p01) + Yin-lu (p02) | Ultra | 4860 → 5040 | 9 | 3.0 | 1311 → 1358 | 24 | 967 px | 151 px |
| Kōga (p03) + p04 | Super | 2221 | 3 | 1.0 | 599 | 34 | 224 px | 1065 px |
| Kōga (p03) + p04 | Super + DHC | 3276 → 4504 | 4 → 6 | 2.0 | 884 → 1214 | 59 | 366 px → 214 px | 941 px → 897 px |
| Kōga (p03) + p04 | Ultra | 5036 | 11 | 3.0 | 1358 | 24 | 636 px | 475 px |
| p05 + p06 | Super | 1998 | 3 | 1.0 | 538 | 4 | 231 px | 1181 px |
| p05 + p06 | Super + DHC | 1998 → não saiu | 3 →  | 2.0 →  | 538 →  | 8 →  | 239 px →  | 1181 px →  |
| p05 + p06 | Ultra | 4536 | 9 | 3.0 | 1222 | 24 | 539 px | 573 px |
| p07 + p08 | Super | 4545 | 4 | 1.0 | 1226 | 69 | 575 px | 1035 px |
| p07 + p08 | Super + DHC | 2196 → 4934 | 3 → 5 | 2.0 | 592 → 1331 | 73 | 396 px → 264 px | 1206 px → 1071 px |
| p07 + p08 | Ultra | 6048 | 6 | 3.0 | 1632 | 24 | 620 px | 500 px |
| p09 + p10 | Super | 5168 | 5 | 1.0 | 1393 | 64 | 266 px | 1076 px |
| p09 + p10 | Super + DHC | 5968 | 8 | 2.0 | 1606 | 75 | 90 px → 88 px | 1070 px |
| p09 + p10 | Ultra | 6295 | 5 | 3.0 | 1698 | 24 | 550 px | 854 px |
| p11 + p12 | Super | 3312 | 3 | 1.0 | 894 | 69 | 257 px | 1067 px |
| p11 + p12 | Super + DHC | 3910 | 4 | 2.0 | 1055 | 101 | 97 px → 99 px | 1084 px |
| p11 + p12 | Ultra | 4028 | 7 | 3.0 | 1087 | 24 | 594 px | 520 px |
| p13 + p14 | Super | 1693 | 3 | 1.0 | 456 | 22 | 136 px | 1103 px |
| p13 + p14 | Super + DHC | 3758 → não saiu | 12 →  | 2.0 →  | 1006 →  | 22 →  | 98 px →  | 597 px →  |
| p13 + p14 | Ultra | 6048 | 5 | 3.0 | 1632 | 24 | 411 px | 989 px |
| p15 + p16 | Super | 1260 | 2 | 1.0 | 340 | 18 | 325 px | 1135 px |
| p15 + p16 | Super + DHC | 1260 → não saiu | 2 →  | 2.0 →  | 340 →  | 40 →  | 325 px →  | 1135 px →  |
| p15 + p16 | Ultra | 5023 | 21 | 3.0 | 1347 | 24 | 678 px | 605 px |

Média Super: 3042 de dano, 1.0 barra, 39 quadros congelados (3042 de dano por barra).
Média Super + DHC: 4735 de dano, 2.0 barra, 71 quadros congelados (2367 de dano por barra).
Média Ultra: 5257 de dano, 3.0 barra, 24 quadros congelados (1752 de dano por barra).

### T21 Sinergia das duplas oficiais

Cada titular com o parceiro oficial e com três alternativos, contra as outras sete duplas oficiais
(CPU nível 8 × CPU nível 8, 2x2, 8 lutas por adversário, metade de cada lado).

| titular | oficial | vitórias | alternativo 1 | alternativo 2 | alternativo 3 | melhor parceiro |
|---|---|---|---|---|---|---|
| Ryūga (p01) | Yin-lu (p02) | 66% → 64% | p04 71% → p04 68% | p07 55% | p12 68% → p12 75% | p04 → p12 |
| Kōga (p03) | p04 | 66% → 55% | p06 64% → p06 54% | p09 75% → p09 63% | p14 39% → p14 41% | p09 |
| p05 | p06 | 45% → 36% | p08 71% → p08 68% | p11 50% | p16 36% → p16 34% | p08 |
| p07 | p08 | 50% → 63% | p10 27% → p10 41% | p13 34% → p13 48% | Yin-lu (p02) 32% → Yin-lu (p02) 34% | oficial |
| p09 | p10 | 77% → 79% | p12 63% → p12 71% | p15 39% → p15 34% | p04 68% → p04 84% | oficial → p04 |
| p11 | p12 | 50% → 64% | p14 43% → p14 41% | Ryūga (p01) 66% → Ryūga (p01) 68% | p06 55% → p06 54% | Ryūga (p01) |
| p13 | p14 | 25% → 39% | p16 36% → p16 30% | Kōga (p03) 61% → Kōga (p03) 70% | p08 64% → p08 68% | p08 → Kōga (p03) |
| p15 | p16 | 13% → 2% | Yin-lu (p02) 9% → Yin-lu (p02) 11% | p05 4% → p05 9% | p10 18% → p10 16% | p10 |

(44 s)


## Sinergia, fichas e matriz (BateriaComparacoes): base → final


### C01 sinergia: o assist de cada um e o que o assist rende para cada um

Como assist: alcance (distância do titular ao boneco em que acerta), início, antiaéreo (acerta o boneco
pulando a 250 px em quantos de 12 tempos), cobertura (vantagem do titular num H defendido com o assist
chamado junto, menos sem). Como parceiro: quanto o assist soma ao maior combo (sem barra, sem Super) de
Ryūga, Kōga, p09 e p11. Como titular: quanto o parceiro oficial soma ao maior combo dele.

| lutador | assist | alcance | início | antiaéreo | cobertura | soma ao combo de outros (média) | o oficial soma ao dele | nota sinergia (1–5) |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | S | 100–800 px | 15 q | 75% | +0 | +1025 | -135 (Yin-lu (p02)) | 3 → 1 |
| Yin-lu (p02) | S | 100–350 px | 18 q | 50% | +15 | +763 | +133 (Ryūga (p01)) | 3 → 2 |
| Kōga (p03) | S | 100–500 px | 19 q | 42% | +1 | +1249 | +282 (p04) | 2 → 1 |
| p04 | S | 100–350 px | 19 q | 25% | +10 | +1042 | +262 (Kōga (p03)) | 2 → 1 |
| p05 | S | 100–1000 px | 17 q | 33% | +8 | +854 | +773 (p06) | 4 → 3 |
| p06 | S | 100–850 px | 19 q | 50% | +27 | +484 | +744 (p05) | 5 → 4 |
| p07 | S | 100–150 px → 100–450 px | 21 q | 0% → 92% | +13 | +1299 | +40 (p08) | 2 → 3 |
| p08 | S | 100–750 px | 19 q | 83% | +8 | +1509 | +1919 (p07) | 5 |
| p09 | S | 100–1000 px | 22 q | 75% | +14 | +493 | +3009 (p10) | 5 |
| p10 | S | 100–800 px | 17 q | 58% | +6 | +811 | +748 (p09) | 4 → 3 |
| p11 | S | 100–1000 px | 19 q | 92% | +1 | +423 | -274 (p12) → +2555 (p12) | 3 → 4 |
| p12 | S | 100–100 px → 100–500 px | 19 q | 0% → 33% | +0 → +11 | -198 → +1805 | -78 (p11) | 1 → 2 |
| p13 | S → H | 100–100 px → 100–500 px | 27 q → 21 q | 0% → 92% | +0 → +16 | -198 → +531 | -100 (p14) → -436 (p14) | 1 → 2 |
| p14 | S | não acerta → 100–550 px | – → 21 q | 0% → 92% | +0 → +26 | -198 → +1394 | -105 (p13) → +930 (p13) | 1 → 5 |
| p15 | M → 2H | 100–100 px → 100–400 px | 17 q → 19 q | 0% → 100% | +0 → +8 | +617 → +881 | -989 (p16) | 1 |
| p16 | S | 100–1000 px | 35 q | 83% | +18 | -296 | +288 (p15) → +405 (p15) | 4 |

### C01 fichas completas (sete dimensões, notas 1–5 por quintil do elenco)

| lutador | alcance | mobilidade | pressão | defesa | dano explosivo | controle de espaço | sinergia | números brutos |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 1 | 1 | 1 | 1 | 5 | 1 → 2 | 3 → 1 | alcance M/H/2M 208/256/240 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -5.0, colado 33%; vida 14000, janela antiaérea total 131, L em 2 q; maior combo sem barra 6934; projétil 760 px/s, longe 14% → alcance M/H/2M 208/256/240 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -5.0, colado 31%; vida 14000, janela antiaérea total 131, L em 2 q; maior combo sem barra 6934; projétil 760 px/s, longe 14% |
| Yin-lu (p02) | 3 | 1 | 2 | 5 | 1 | 1 | 3 → 2 | alcance M/H/2M 264/276/220 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.5, colado 39%; vida 13440, janela antiaérea total 194, L em 3 q; maior combo sem barra 4798; projétil 700 px/s, longe 9% → alcance M/H/2M 264/276/220 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.5, colado 37%; vida 13440, janela antiaérea total 194, L em 3 q; maior combo sem barra 4798; projétil 700 px/s, longe 8% |
| Kōga (p03) | 2 | 1 | 5 → 3 | 2 | 2 | 4 | 2 → 1 | alcance M/H/2M 204/208/312 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 41%; vida 13720, janela antiaérea total 146, L em 3 q; maior combo sem barra 5501; projétil 950 px/s, longe 8% → alcance M/H/2M 204/208/312 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 31%; vida 13720, janela antiaérea total 146, L em 3 q; maior combo sem barra 5501; projétil 950 px/s, longe 10% |
| p04 | 4 | 1 | 3 → 2 | 3 | 2 | 3 → 2 | 2 → 1 | alcance M/H/2M 260/260/288 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.5, colado 42%; vida 14420, janela antiaérea total 147, L em 3 q; maior combo sem barra 5466; projétil 800 px/s, longe 8% → alcance M/H/2M 260/260/288 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.5, colado 38%; vida 14420, janela antiaérea total 147, L em 3 q; maior combo sem barra 5466; projétil 800 px/s, longe 6% |
| p05 | 1 | 5 | 2 | 1 | 4 | 1 | 4 → 3 | alcance M/H/2M 276/204/220 px; anda 300, dash 207, pulo 280 px, 1 air dash, pulo na parede; vantagem média dos normais -3.3, colado 37%; vida 12880, janela antiaérea total 125, L em 3 q; maior combo sem barra 6041; projétil 1000 px/s, longe 13% → alcance M/H/2M 276/204/220 px; anda 300, dash 207, pulo 280 px, 1 air dash, pulo na parede; vantagem média dos normais -3.3, colado 38%; vida 12880, janela antiaérea total 125, L em 3 q; maior combo sem barra 6041; projétil 1000 px/s, longe 11% |
| p06 | 2 | 1 | 2 → 3 | 1 | 3 | 3 | 5 → 4 | alcance M/H/2M 244/216/272 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 27%; vida 13300, janela antiaérea total 130, L em 3 q; maior combo sem barra 6005; projétil 480 px/s, longe 19% → alcance M/H/2M 244/216/272 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 29%; vida 13300, janela antiaérea total 130, L em 3 q; maior combo sem barra 6005; projétil 480 px/s, longe 18% |
| p07 | 1 | 1 | 3 | 4 | 4 | 1 | 2 → 3 | alcance M/H/2M 264/228/192 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.0, colado 42%; vida 15120, janela antiaérea total 170, L em 4 q; maior combo sem barra 6411; projétil 700 px/s, longe 9% → alcance M/H/2M 264/228/192 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.0, colado 37%; vida 15120, janela antiaérea total 170, L em 4 q; maior combo sem barra 6411; projétil 700 px/s, longe 11% |
| p08 | 5 | 1 | 1 | 5 | 1 | 5 | 5 | alcance M/H/2M 284/432/200 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 16%; vida 13020, janela antiaérea total 195, L em 3 q; maior combo sem barra 4137; projétil 1100 px/s, longe 30% → alcance M/H/2M 284/432/200 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 17%; vida 13020, janela antiaérea total 195, L em 3 q; maior combo sem barra 4018; projétil 1100 px/s, longe 32% |
| p09 | 3 | 1 | 4 | 4 | 5 | 3 | 5 | alcance M/H/2M 276/196/300 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.0, colado 42%; vida 16100, janela antiaérea total 144, L em 4 q; maior combo sem barra 6924; projétil 600 px/s, longe 9% → alcance M/H/2M 276/196/300 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.0, colado 42%; vida 16100, janela antiaérea total 144, L em 4 q; maior combo sem barra 6924; projétil 600 px/s, longe 12% |
| p10 | 2 | 5 | 3 → 5 | 2 | 2 | 2 → 1 | 4 → 3 | alcance M/H/2M 284/196/252 px; anda 300, dash 207, pulo 280 px, 2 air dash; vantagem média dos normais -3.3, colado 41%; vida 13020, janela antiaérea total 146, L em 3 q; maior combo sem barra 5027; projétil 900 px/s, longe 9% → alcance M/H/2M 284/196/252 px; anda 300, dash 207, pulo 280 px, 2 air dash; vantagem média dos normais -3.3, colado 45%; vida 13020, janela antiaérea total 146, L em 3 q; maior combo sem barra 5027; projétil 900 px/s, longe 7% |
| p11 | 1 | 1 | 1 | 1 | 1 | 5 | 3 → 4 | alcance M/H/2M 220/236/264 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 25%; vida 12600, janela antiaérea total 146, L em 3 q; maior combo sem barra 4407; projétil 950 px/s, longe 29% → alcance M/H/2M 220/236/264 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 21%; vida 12600, janela antiaérea total 146, L em 3 q; maior combo sem barra 4701; projétil 950 px/s, longe 31% |
| p12 | 1 | 1 | 5 | 5 | 3 | 2 | 1 → 2 | alcance M/H/2M 264/160/296 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 54%; vida 13300, janela antiaérea total 198, L em 3 q; maior combo sem barra 5751; projétil 700 px/s, longe 5% → alcance M/H/2M 264/160/296 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 51%; vida 13300, janela antiaérea total 198, L em 3 q; maior combo sem barra 5751; projétil 700 px/s, longe 6% |
| p13 | 5 | 1 | 5 | 4 | 5 → 4 | 4 | 1 → 2 | alcance M/H/2M 260/284/336 px; anda 170, dash 134, pulo 280 px, 1 air dash; vantagem média dos normais -3.3, colado 51%; vida 16100, janela antiaérea total 160, L em 4 q; maior combo sem barra 6809; projétil 600 px/s, longe 7% → alcance M/H/2M 260/284/336 px; anda 170, dash 134, pulo 280 px, 1 air dash; vantagem média dos normais -3.3, colado 51%; vida 16100, janela antiaérea total 160, L em 4 q; maior combo sem barra 6441; projétil 600 px/s, longe 7% |
| p14 | 5 | 1 | 1 | 2 | 3 → 4 | 4 | 1 → 5 | alcance M/H/2M 256/324/332 px; anda 200, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.7, colado 40%; vida 13300, janela antiaérea total 148, L em 5 q; maior combo sem barra 5547; projétil 600 px/s, longe 7% → alcance M/H/2M 256/324/332 px; anda 200, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.7, colado 38%; vida 13300, janela antiaérea total 148, L em 5 q; maior combo sem barra 6441; projétil 600 px/s, longe 8% |
| p15 | 4 | 1 | 4 | 3 | 1 | 5 | 1 | alcance M/H/2M 196/300/332 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 37%; vida 14000, janela antiaérea total 159, L em 3 q; maior combo sem barra 3650; projétil 900 px/s, longe 10% → alcance M/H/2M 196/300/332 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 33%; vida 14000, janela antiaérea total 159, L em 3 q; maior combo sem barra 3650; projétil 900 px/s, longe 11% |
| p16 | 4 | 1 | 4 | 3 | 4 → 3 | 2 → 3 | 4 | alcance M/H/2M 224/268/292 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 37%; vida 14000, janela antiaérea total 147, L em 3 q; maior combo sem barra 6565; projétil 500 px/s, longe 11% → alcance M/H/2M 224/268/292 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 33%; vida 14000, janela antiaérea total 147, L em 3 q; maior combo sem barra 5615; projétil 500 px/s, longe 12% |

### Matriz 1x1 16 × 16 (seção 7.3)

CPU nível 8 dos dois lados, 10 sementes por célula. Linha = lado esquerdo (P1), coluna = lado direito (P2);
célula = vitórias do lado esquerdo (de 10). Empates e tempo esgotado sem vencedor contam como não vitória.

| P1 \ P2 | p01 | p02 | p03 | p04 | p05 | p06 | p07 | p08 | p09 | p10 | p11 | p12 | p13 | p14 | p15 | p16 | total P1 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| p01 | **4** → **7** | 7 → 6 | 9 → 4 | 1 → 5 | 8 → 4 | 8 | 10 → 9 | 7 → 3 | 4 → 3 | 3 → 5 | 10 → 4 | 5 → 7 | 8 → 6 | 8 → 7 | 9 | 8 → 10 | 68% → 61% |
| p02 | 3 → 4 | **5** → **6** | 6 → 4 | 2 → 1 | 4 → 5 | 0 → 3 | 7 | 1 | 0 → 3 | 3 | 5 → 6 | 4 → 2 | 1 → 5 | 3 → 7 | 8 → 9 | 2 → 7 | 34% → 46% |
| p03 | 9 | 6 → 8 | **4** → **6** | 3 | 5 → 8 | 6 | 9 → 3 | 5 → 4 | 6 → 3 | 7 → 3 | 7 → 8 | 6 → 5 | 2 → 3 | 5 → 8 | 10 → 9 | 8 → 9 | 61% → 59% |
| p04 | 6 → 4 | 9 → 8 | 7 → 6 | **2** → **4** | 8 | 4 → 9 | 10 → 6 | 3 | 5 → 3 | 6 → 7 | 6 | 7 → 6 | 0 → 2 | 7 → 9 | 10 → 8 | 7 | 61% → 60% |
| p05 | 2 → 5 | 8 → 5 | 3 → 6 | 4 → 3 | **7** | 2 → 6 | 7 → 6 | 2 → 3 | 2 → 3 | 4 → 3 | 4 → 7 | 6 → 3 | 1 → 2 | 7 → 5 | 9 | 5 → 10 | 46% → 52% |
| p06 | 3 → 1 | 7 | 7 → 6 | 7 → 3 | 7 → 4 | **6** → **7** | 10 → 7 | 5 → 4 | 7 → 6 | 1 | 4 | 6 → 4 | 10 → 8 | 9 → 6 | 9 → 8 | 7 → 6 | 66% → 51% |
| p07 | 0 → 2 | 2 → 5 | 0 | 3 | 1 → 3 | 1 | **4** → **5** | 0 | 0 → 2 | 2 → 1 | 1 → 0 | 5 → 8 | 0 → 3 | 3 → 5 | 2 → 9 | 3 → 4 | 17% → 32% |
| p08 | 4 → 3 | 9 → 8 | 5 | 10 → 8 | 8 → 6 | 6 → 7 | 10 | **3** → **5** | 3 → 4 | 8 → 7 | 4 → 8 | 10 → 8 | 10 | 10 | 10 | 10 | 75% → 74% |
| p09 | 7 → 8 | 7 → 8 | 5 → 7 | 3 → 2 | 7 → 6 | 3 → 4 | 7 → 8 | 4 → 3 | **4** → **5** | 3 → 6 | 4 | 7 → 5 | 3 → 6 | 7 → 8 | 6 → 9 | 9 | 54% → 61% |
| p10 | 3 → 5 | 8 | 5 → 7 | 5 → 4 | 9 → 7 | 8 → 10 | 10 | 2 | 7 → 5 | **7** → **5** | 5 → 2 | 5 → 8 | 2 → 4 | 5 → 7 | 9 | 9 | 62% → 64% |
| p11 | 2 | 5 → 6 | 2 → 4 | 4 → 5 | 7 → 4 | 0 → 8 | 10 | 7 | 5 → 3 | 4 → 5 | **7** → **5** | 6 → 5 | 10 → 9 | 8 → 10 | 8 → 9 | 10 → 9 | 59% → 63% |
| p12 | 5 → 7 | 5 → 3 | 3 | 4 → 3 | 6 → 4 | 3 → 6 | 6 → 5 | 0 → 1 | 3 → 7 | 4 | 0 → 3 | **5** | 4 → 3 | 5 | 10 → 9 | 4 | 42% → 45% |
| p13 | 5 → 3 | 7 → 4 | 7 → 8 | 5 → 6 | 10 → 7 | 0 → 2 | 8 → 7 | 0 | 8 → 4 | 7 → 9 | 1 → 0 | 8 → 6 | **5** → **4** | 9 → 10 | 8 | 9 → 7 | 61% → 53% |
| p14 | 3 | 3 → 4 | 3 → 5 | 4 → 3 | 7 → 3 | 1 → 0 | 9 → 7 | 1 → 0 | 5 → 3 | 6 → 3 | 3 | 5 → 3 | 1 → 0 | **3** → **8** | 10 → 8 | 6 → 4 | 44% → 36% |
| p15 | 0 | 3 → 1 | 1 → 0 | 1 | 1 → 2 | 0 | 5 → 2 | 0 | 0 → 1 | 1 → 0 | 0 | 1 | 3 → 2 | 2 → 3 | **4** | 4 → 5 | 16% → 14% |
| p16 | 1 | 2 → 0 | 2 → 0 | 1 → 0 | 5 → 2 | 2 | 7 → 4 | 0 | 3 → 1 | 4 → 1 | 0 → 1 | 3 → 6 | 2 → 3 | 5 → 6 | 4 → 6 | **5** → **6** | 29% → 24% |

Viés de lado: o lado esquerdo vence 50% das 2560 lutas (56% nos espelhos: 89 de 160).

Espelhos (lado esquerdo vence, de 10):

Ryūga (p01) 7; Yin-lu (p02) 6; Kōga (p03) 6; p04 4; p05 7; p06 7; p07 5; p08 5; p09 5; p10 5; p11 5; p12 5; p13 4; p14 8; p15 4; p16 6.

Força contra o elenco (os dois lados somados, sem os espelhos) e a maior diferença de lado nos pares:

| # | lutador | vence | como P1 | como P2 |
|---|---|---|---|---|
| 1 | p08 | 77% → 78% | 78% → 76% | 75% → 79% |
| 2 | p06 → p04 | 68% → 64% | 66% → 61% | 71% → 67% |
| 3 | Ryūga (p01) → p09 | 67% → 64% | 70% → 62% | 65% → 66% |
| 4 | p04 → p11 | 63% | 63% → 64% | 62% → 63% |
| 5 | p13 → p10 | 62% → 63% | 61% → 65% | 62% → 61% |
| 6 | p11 → Ryūga (p01) | 61% | 59% → 60% | 64% → 62% |
| 7 | Kōga (p03) | 60% → 58% | 63% → 59% | 57% |
| 8 | p10 → p13 | 60% → 55% | 61% → 54% | 58% → 56% |
| 9 | p09 → p05 | 58% → 51% | 55% → 51% | 61% → 51% |
| 10 | p12 → p06 | 43% → 51% | 41% → 50% | 44% → 52% |
| 11 | p14 → p12 | 41% → 47% | 45% | 38% → 49% |
| 12 | p05 → Yin-lu (p02) | 41% → 45% | 44% → 45% | 38% → 46% |
| 13 | Yin-lu (p02) → p07 | 37% → 32% | 33% → 31% | 41% → 33% |
| 14 | p16 → p14 | 30% → 31% | 27% → 33% | 33% → 29% |
| 15 | p15 → p16 | 17% → 24% | 15% → 22% | 19% → 27% |
| 16 | p07 → p15 | 16% → 13% | 15% → 12% | 17% → 14% |

Pares em que o lado muda o resultado em 6 ou mais de 10 (1 de 120):

- Kōga (p03) × p07: Kōga (p03) vence 3 de 10 pela esquerda e 10 de 10 pela direita

(58 s)

