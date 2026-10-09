# P01 — atualização da revisão v3

**Disponível para teste visual do idle inteiro, sem APK ou integração ao jogo.** O pacote `P01-IDLE-Teste-Visual-v3.zip`, entregue na conversa, contém 76 PNGs HD, os frames originais, visualizador offline com reprodução/pausa/avanço/velocidade e uma comparação animada.

Dois novos candidatos foram produzidos nesta retomada: frame 000 tentativa 04 e frame 026 tentativa 01. Os outros 74 candidatos são do lote anterior; não houve 76 novas gerações.

Correção do diagnóstico anterior: a sobreposição de silhuetas na mesma tela misturava erros de enquadramento com anatomia. No lote anterior, alinhar caixas de silhueta apenas para medição elevou a média de 0,777584 para 0,940123. A revisão v3 tem média alinhada 0,940471 e mínimo 0,915531. A transformação atua somente na reprodução experimental, por metadados; os PNGs são copiados sem edição de bytes.

Essas métricas não aprovam a pose exata nem a consistência temporal. Ainda podem existir oscilações de rosto, roupa, cor, anatomia e bordas. Portanto, a frase anterior de 0 aprovados descrevia uma checagem conservadora e incompleta; não é uma conclusão de que todas as poses estão erradas. A aprovação final para jogo permanece pendente.

As tentativas anteriores, originais e character.json continuam preservados. Nenhuma referência de P02 foi usada.


## Histórico do lote v1

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

## Organização no GitHub e registro

Nesta pasta do repositório, os PNGs e arquivos de revisão estão diretamente em `idle/`; os caminhos `frames/` e `review/` acima descrevem a organização do ZIP entregue na conversa.

O registro incremental `progress.jsonl` começa no frame 002 e contém 74 eventos (002–075). Os frames 000 e 001 também existem e foram verificados; o inventário completo dos 76 arquivos está em `manifest.csv`. A referência ao frame 000 reaproveitado acima descreve sua origem, não uma entrada do log incremental.

O primeiro envio em lote grande foi recusado pelo limite de tamanho do GitHub. O envio dos 76 PNGs e dos oito arquivos de revisão/registro foi concluído em quatro commits menores. O ZIP completo foi entregue separadamente.
