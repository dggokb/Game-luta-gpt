# P01 — idle ampliado 6x, sem redesenho

76 frames originais ampliados uniformemente de 256x256 para 1536x1536 com filtro Lanczos e transparência RGBA. Não foram regenerados rosto, roupa ou anatomia. Não há normalização por bounding box nem mudança de enquadramento por frame. A sequência e o tempo de 42 ms permanecem os mesmos.

Isto é interpolação determinística, não reconstrução por IA nem novo detalhe real. Artefatos e pequenas oscilações existentes no original podem continuar presentes. O resultado evita as variações introduzidas pelos redesenhos independentes. Ainda não foi integrado ao jogo ou empacotado em APK.

Os PNGs originais foram obtidos pelo navegador do Work diretamente do GitHub e permanecem intactos. As tentativas de redesenho também permanecem preservadas. Nenhuma referência de P02 foi usada.

frames/: 76 PNGs separados RGBA 1536x1536.
manifest.json: hashes, transparência e conferência da silhueta após redução diagnóstica para 256x256.

Branch de resultados: art/p01-hd-redraw-20261009.
Base: game-luta-sprite-gpt, db929d23ac15e4124e48f240c97414bba416424a.
