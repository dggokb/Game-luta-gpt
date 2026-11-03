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
