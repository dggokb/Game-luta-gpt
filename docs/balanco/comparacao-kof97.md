# KOF '97 (vídeo do jogador) × o nosso jogo — movimento (03/11)

Fonte: gravação de tela de 9 s, Ryo × Terry, a 24 quadros por segundo; o jogo roda a 60. Os
tempos abaixo são medidos em quadros do vídeo (1 quadro ≈ 0,042 s), com erro de ±1 quadro. A régua
de tamanho é a altura do corpo em pé: o Ryo no vídeo, o Ryūga (224 px) no nosso.

| movimento | KOF '97 (vídeo) | o nosso (CombatConfig) | diferença |
|---|---|---|---|
| pulo parado: tempo no ar | ~0,54 s (quadros 104→117) | 0,71 s (sobe 0,37 + desce 0,34) | o nosso fica 30% mais no ar |
| pulo: altura | ~0,75 corpo | 274 px = 1,2 corpo | o nosso pula 1,6× mais alto |
| pulo para frente/trás | cambalhota encolhida no meio (82–95, 126–132) | o mesmo desenho do pulo, sem giro | falta a cambalhota |
| pulo curto (hop) e hyper hop | tem (toque rápido no ↑; corrida + hop) | não tem; temos super pulo, pulo duplo e air dash | o KOF não tem air dash nem pulo duplo |
| corrida | segura → → e corre inclinado, cancela em golpe/pulo (70–80) | `dashSpeed` 620 px/s ≈ 2,8 corpos/s, corrida segurando | parecido (~2,3 corpos/s no vídeo) |
| corrida + pulo | o pulo sai mais longo (hyper) | `dashJumpSpeed` 540 | parecido |
| backdash | pulinho curto para trás | 15 quadros, 40 px de pulinho + 12 de recuperação | parecido |
| caiu → em pé (agarrão) | ~0,9 s: cai, quica uma vez, fica ~0,6 s e levanta rápido (36→60) | 1,3 s: queda 13 + chão 50 + levantar 16 quadros | o nosso fica 40% mais no chão; não quica |
| agarrão | encostou + direção + botão forte; levanta o rival por cima e bate no chão do outro lado | botão de agarrão (L+M), 5 quadros para sair | o do KOF é imediato e joga mais longe |
| tela | ~2,8 corpos de largura (bonecos grandes) | 1280 px ≈ 5,7 corpos | a nossa câmera mostra o dobro de espaço |
| contador de combo | "RUSH 2 HITS" no canto | número de golpes grande | — |

## O que dá a sensação de "KOF"

1. **Pulos baixos e rápidos** (cerca de meio segundo no ar). O pulo é um ataque, não uma fuga.
2. **Cambalhota** nos pulos para frente e para trás.
3. **Pulo curto** (hop) para entrar por cima rápido.
4. **Levantar rápido** e o quique ao cair, que deixam a luta sem pausa.
5. **Bonecos grandes na tela**: os dois sempre perto, e a corrida fecha a distância em meio
   segundo.

## Aplicado: pulo mais rápido, queda com quique e câmera mais perto (pedido do jogador, 03/11)

- **Pulo 20% mais rápido, com a mesma altura e a mesma distância:**
  - fica ~34 quadros no ar (eram ~42): cerca de 0,6 s na tela (era 0,77 s);
  - a velocidade de subida e de lado foi dividida por 0,8 e a gravidade do pulo por 0,64;
  - vale também para o pulo da corrida, o pulo no ar (jM → pulo) e o pulo na parede;
  - o super pulo não mudou.
- **Queda com quique:**
  - o corpo bate no chão, sobe ~30 px deitado e assenta (14 quadros), com poeira na batida e ao
    assentar; só o desenho sobe;
  - fica deitado 22 quadros (eram 50) antes do levantar normal;
  - da queda até em pé: ~0,9 s (era 1,4 s), como o KOF;
  - o levantar rápido, o rolamento e o levantar atrasado continuam.
- **Câmera mais perto:**
  - com os dois perto, o zoom vai até 1,36 (era 1,24): os bonecos ficam ~10% maiores;
  - os pés continuam na mesma altura da tela;
  - a partir de ~420 px de distância nada muda (o começo do round é igual).
- **Simulado (`BateriaKof`, CPU × CPU, os 16 contra todos, 960 lutas por nível):**

  | | base | pulo 0,8 | pulo 0,8 + queda 22 (ficou) | pulo 0,8 + queda 30 |
  |---|---|---|---|---|
  | duração, nível 5 / 8 | 82 / 100 s | 81 / 96 s | 79 / 95 s | 80 / 98 s |
  | dano por minuto, nível 8 | 29700 | 30383 | 31195 | 30930 |
  | pulo tirado do ar, nível 5 / 8 | 19 / 19% | 16 / 18% | 17 / 18% | 17 / 18% |
  | pulo que acertou, nível 5 / 8 | 21 / 21% | 24 / 22% | 25 / 23% | 24 / 22% |
  | levantou e apanhou, nível 8 | 27% | 27% | 25% | 26% |
  | desvio das vitórias, nível 5 / 8 | 20,1 / 17,9 | 20,6 / 19,3 | 19,6 / 19,1 | 19,8 / 19,2 |

  - O pulo rápido vale mais: menos antiaéreo pega e mais pulos acertam.
  - A luta fica ~5 s mais curta, com mais dano por minuto.
  - O equilíbrio do elenco fica igual (a diferença no desvio está dentro do ruído).
  - O pulo 30% mais rápido (como o KOF, 0,54 s) também foi testado: o pulo ficou forte demais
    (só 13–15% tirados do ar), e o pior e o melhor personagem se afastaram no nível 5.
- **Ficou:** pulo 0,8 e queda 22 com o quique; os números completos estão em
  `perfis/kof-pulo-queda.md`.
