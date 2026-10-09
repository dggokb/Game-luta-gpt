# P01 — atualização da revisão v3

**Disponível para teste visual do idle inteiro, sem APK ou integração ao jogo.** O pacote `P01-IDLE-Teste-Visual-v3.zip`, entregue na conversa, contém 76 PNGs HD, os frames originais, visualizador offline com reprodução/pausa/avanço/velocidade e uma comparação animada.

Dois novos candidatos foram produzidos nesta retomada: frame 000 tentativa 04 e frame 026 tentativa 01. Os outros 74 candidatos são do lote anterior; não houve 76 novas gerações.

Correção do diagnóstico anterior: a sobreposição de silhuetas na mesma tela misturava erros de enquadramento com anatomia. No lote anterior, alinhar caixas de silhueta apenas para medição elevou a média de 0,777584 para 0,940123. A revisão v3 tem média alinhada 0,940471 e mínimo 0,915531. A transformação atua somente na reprodução experimental, por metadados; os PNGs são copiados sem edição de bytes.

Essas métricas não aprovam a pose exata nem a consistência temporal. Ainda podem existir oscilações de rosto, roupa, cor, anatomia e bordas. Portanto, a frase anterior de 0 aprovados descrevia uma checagem conservadora e incompleta; não é uma conclusão de que todas as poses estão erradas. A aprovação final para jogo permanece pendente.

As tentativas anteriores, originais e character.json continuam preservados. Nenhuma referência de P02 foi usada.


## Histórico da revisão anterior

# P01 HD — progresso real

## Idle completo: lote de revisão

76/76 frames do idle gerados, em PNGs individuais 1254×1254 RGBA com transparência. Frame 000 reutiliza a segunda tentativa anterior; 001–075 foram produzidos individualmente nesta execução. Nenhuma referência de P02 foi usada.

**0 frames aprovados para integração.** A pose exata e a consistência temporal ainda não foram atendidas: há variações de silhueta, proporções, mãos/pés e detalhes. Não integrar este lote ao jogo antes da correção.

[Frames, comparação animada e relatórios do idle](idle/). O ZIP completo de revisão foi entregue separadamente na conversa.

Branch de resultados: `art/p01-hd-redraw-20261009`.
Base preservada: `game-luta-sprite-gpt`, commit `db929d23ac15e4124e48f240c97414bba416424a`.
Originais e `characters/player_base/character.json` não foram sobrescritos.

O envio inicial de um lote grande foi recusado pelo GitHub por tamanho; o envio foi concluído em quatro lotes menores. Os 76 PNGs e os oito arquivos de revisão/registro foram conferidos na comparação dos commits.

As demais animações não foram processadas nesta execução. Restam 500 pares únicos de atlas/frame do inventário de 576.

## Histórico

As três tentativas anteriores continuam preservadas para comparação. O registro atual acima substitui a contagem antiga de progresso.
