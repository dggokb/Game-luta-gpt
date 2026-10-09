# P01 — IDLE HD, lote completo de revisão

76/76 frames produzidos individualmente. Frame 000 reutiliza a segunda tentativa anterior; 001–075 foram gerados individualmente nesta execução. Zero falhas de geração. Zero frames aprovados para integração: o requisito de pose exata não foi atendido.

Base: game-luta-sprite-gpt, commit db929d23ac15e4124e48f240c97414bba416424a.
Branch de resultados: art/p01-hd-redraw-20261009.
Originais obtidos pelo navegador do Work diretamente do GitHub. Os originais e character.json não foram alterados. Nenhuma referência de P02 foi usada.

76 PNGs separados, todos 1254×1254 RGBA, com alpha de 0 a 255. Esta é a resolução nativa entregue pelo gerador, sem ampliação artificial. Todos os arquivos têm hashes diferentes.

Falhas: diferenças de enquadramento, silhueta, proporções, posição de mãos/pés e detalhes de rosto/roupa; franjas coloridas nas bordas. As variações entre gerações causam instabilidade temporal. A identidade geral, as cores e o traje do P01 foram preservados visualmente, mas não há aprovação de pose exata ou de loop pronto para o jogo.

A métrica de silhueta compara máscaras alpha >=128 na mesma tela de 256×256; apenas a medição usa redução. IoU média 0.777584, mínimo 0.666667, máximo 0.826987. IoU não verifica articulações, rosto ou identidade e não substitui revisão visual.

frames/: saídas nativas sem correção geométrica.
review/manifest.csv: dimensões, alpha, hashes e IoU por frame.
review/validation.json: conferência do lote.
review/contact_*.jpg: folhas de inspeção.
review/comparison.gif: original à esquerda e redesenho à direita, 42 ms solicitados por frame (GIF arredonda para centésimos de segundo).
progress.jsonl: registro de geração; frame 000 é reaproveitado.

Próximo passo de produção: corrigir fidelidade geométrica e estabilidade temporal antes da integração. As outras animações não foram processadas nesta execução do idle.
