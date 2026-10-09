# P01 — fluxo autônomo de correção

Autorização do usuário: em 09/10/2026, corrigir e revalidar frames reprovados sem pedir comandos individuais.

1. Inspecionar o PNG original, a referência oficial P01 e a tentativa. Registrar defeitos específicos.
2. Gerar correção individual com o original como guia absoluto de pose e de posição no canvas; arte oficial apenas para identidade. Preservar originais e tentativas.
3. Conferir RGBA/transparência, recorte, dimensões, silhueta normalizada, posição das articulações, mãos/pés, roupa e rosto. Métricas de alpha são diagnóstico, não prova de pose exata.
4. Comparar com vizinhos e reproduzir o trecho animado. Uma imagem isolada boa não aprova a animação.
5. Reprovou: repetir com correção específica. Após três tentativas sem melhora suficiente, mudar a abordagem e registrar o bloqueio; não aprovar por esgotamento de tentativas. Prosseguir nas tarefas independentes que ainda sejam viáveis.
6. Salvar candidatos em pasta versionada de revisão na branch art/p01-hd-redraw-20261009. Só promover quando houver evidência visual e temporal suficiente; não integrar automaticamente candidatos reprovados ao jogo.
7. Informar contagens reais: gerados, revisados, aprovados, reprovados e bloqueados. Entregar ZIP e relatórios de revisão ao final do lote.

Este fluxo descreve a execução durante sessões ativas. Não implica execução em segundo plano depois do encerramento da resposta.

Estado inicial: lote idle com 76 PNGs gerados, 0 aprovados. Correção do frame 000 iniciada com limites do original x=53..203 e y=14..237 em canvas 256×256.
