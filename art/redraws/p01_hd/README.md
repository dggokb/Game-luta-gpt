# P01 — teste de super-resolução local 4×

[idle_sr_4x](idle_sr_4x/) contém os 76 frames do idle processados com Real-ESRGAN AnimeVideo-v3: 1024×1024 RGBA, 42 ms por frame, 76/76 concluídos e enviados, 0 falhas de processamento. PNGs, manifesto com hashes, scripts reproduzíveis e licença estão na pasta. ZIP: P01-IDLE-SuperResolucao-4x-Teste.zip, entregue na conversa junto da comparação animada.

A tela original foi ampliada uniformemente; o alpha corresponde exatamente ao original ampliado por Lanczos em 76/76 frames. Não há reenquadramento independente nem geração por prompt. O modelo reconstrói linhas e cores internas. O diagnóstico de variação RGB entre frames foi 3,548085 no original e 3,792796 no resultado, sem compensação de movimento; não comprovou redução da tremulação. Resultado disponível para avaliação visual, sem aprovação ou integração ao jogo/APK.

Branch de resultados: art/p01-hd-redraw-20261009. Originais e character.json preservados; nenhuma referência de P02. As demais animações ainda não foram processadas nesta abordagem.

## Histórico: ampliação por interpolação 6×

# P01 — idle ampliado sem redesenho

Atendendo à mudança de abordagem após a tremulação dos redesenhos: [idle_upscale_6x](idle_upscale_6x/) contém os 76 frames originais ampliados uniformemente de 256×256 para 1536×1536, com transparência e tempo de 42 ms. Nenhuma regeneração de anatomia, rosto ou roupa, nenhum alinhamento independente por frame.

76/76 concluídos; 0 falhas pendentes. PNGs e ZIP decodificados e revalidados após corrigir truncamento temporário na sincronização. Conferência diagnóstica: 76/76 enquadramentos da silhueta preservados; IoU de alpha mínima 0,998566 e média 0,999099. Isto é interpolação Lanczos, sem criação de detalhe real. Oscilações que já existam no original podem continuar presentes.

Disponível para comparação visual na conversa. ZIP: P01-IDLE-Upscale-6x-sem-redesenho.zip. Ainda sem integração ao jogo ou APK. Originais, configuração e tentativas anteriores preservados na branch separada art/p01-hd-redraw-20261009. Nenhuma referência de P02 usada. As demais animações ainda não foram processadas.

## Histórico anterior: redesenhos experimentais

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
