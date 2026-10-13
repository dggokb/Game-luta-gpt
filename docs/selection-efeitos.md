# Efeitos da seleção — 12/10/2026

Branch ui/selecao-efeitos-20261012, criada a partir de 4a544f4c7a77f6ecc0b8a75539700c73741b518b.

- Brilho azul/vermelho pulsante nas molduras P1/P2, TEAM e cursor ativo.
- Rastro curto de energia na borda superior da célula focada.
- 24 partículas pequenas nas laterais, atrás dos heróis e fora da grade e dos textos.
- Flash suave de 240 ms nos retratos escolhidos; não altera identidade nem escala.
- Halo verde discreto no comando de confirmação.
- Revelação da luta em 240 ms, com overlay escuro que desaparece; controles e simulação não são atrasados.

O tempo usa SystemClock.uptimeMillis, não contagem de frames. Não são carregadas novas imagens por frame, nem usados blur, filtros pesados ou objetos temporários na animação. Posições e áreas de toque permanecem as mesmas.

O teste no APK compara cores RGB nas áreas internas dos retratos, que permanecem fixas, em vez de exigir uma tela inteira estática. Verifica separadamente que a decoração anima. Continua cobrindo bloqueados, ambas as equipes, titular/TAG, troca e entrada na luta. O workflow também grava um MP4 do APK real mostrando trocas de personagem e a confirmação.
