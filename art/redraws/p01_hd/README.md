# P01 HD — progresso real

Base: game-luta-sprite-gpt, commit db929d23ac15e4124e48f240c97414bba416424a.
Branch: art/p01-hd-redraw-20261009.

## Status

- 42 animações configuradas; 576 pares únicos atlas/frame referenciados.
- PNG oficial, pose-chave idle_base, folha idle e character.json baixados pelo navegador do Work diretamente do GitHub.
- 76 frames do idle separados em células 256×256, grade de 8 colunas, sem redesenho nessa etapa.
- 1 frame tentado: IDLE 000, com 3 gerações individuais.
- 0 frames aprovados; 575 frames ainda não tentados.
- Não foi executado o lote completo. Nenhum original ou configuração de jogo foi alterado.

## Falhas de validação

As três tentativas preservam a identidade visual do P01 e possuem canal alpha, mas alteram a geometria da pose. Resolução real das três: 1254×1254, apesar da solicitação de 2048×2048.

Comparação das máscaras alpha >127, com saída reduzida para 256×256 apenas para medição: IoU tentativa 1 = 0.7252587992; tentativa 2 = 0.8106319621. Essa medida auxilia a inspeção, não prova por si só identidade de pose. Inspeção visual também constatou alterações no corpo/membros. As três saídas são REPROVADAS para integração.

## Continuidade

Resolver fidelidade geométrica do IDLE/frame_000 antes de expandir o método para os demais frames. Preservar o enquadramento e as posições articulares dos PNGs originais; não substituir a animação por uma pose aproximada. As saídas nesta pasta são tentativas de revisão, não sprites prontos.


## Retomada 09/10/2026

A terceira geração individual do IDLE 000 foi reprovada: IoU 0.7712861839, 1254×1254 RGBA. O envio anterior de PNGs ao GitHub não foi concluído; na retomada, as três tentativas e o ZIP foram enviados à branch separada. Total: 3 tentativas de 1 frame, 0 aprovados. Produção dos demais 575 frames pendente por fidelidade geométrica.
