# P12 — vídeos existentes do Drive, trabalho parcial

Branch: `test/p09-three-engines-20261009`. Sem merge nas branches principais.

**O personagem não está finalizado nem aprovado.** Todas as novas imagens vêm dos pixels dos 33 vídeos da pasta `animations/p12`, sem novas poses desenhadas e sem deformações do Idle. Foram extraídos 3.201 frames com o motor de vídeo, SHA-256 do vídeo, SHA-256 do PNG, crop e PTS originais. Os vídeos têm intervalos de apresentação variáveis; os timestamps não foram substituídos por uma taxa fixa.

## Lote integrado

12 estados: IDLE, COMBAT, WALK_FORWARD, WALK_BACK, LIGHT_JAB, CROUCH, RISE, JUMP, FALL, LAND, DEFENSE_CROUCH e DEFENSE_AIR. São 87 frames selecionados, normalizados por escala uniforme da imagem inteira e translação de raiz. Máscaras só removem alpha e não desenham corpo sob objetos oclusores. O agachamento tem cerca de 106 px de altura, contra 158 px do Idle curvado. O personagem ereto tem outra altura; aumentar o Idle para 208 px inflava a cabeça. O pulo usa a faixa vermelha como referência da cintura, evitando deslocar o corpo quando as pernas mudam de posição.

O AnimeVideo-v3 oficial refinou imagens já revisadas. Modelo: `b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d`. Esta execução reutilizou 38 frames e processou 49 novos. O contexto de 32 px ao redor do corpo preserva a resposta da rede sem inferir toda a área vazia. O alpha foi verificado separadamente contra o upscale nativo.

O Character Pack gerou os recursos Android, layouts e registros Java. Corrigido o staging de referências externas: os PNGs de poses são copiados sem alteração, com caminhos seguros e hashes verificados. Os tempos de combate, contagens, impacto, comandos, caixas de golpe e cancelamentos permanecem idênticos ao contrato original.

## Validações

- 30 testes focados passaram.
- Prova de origem dos pixels, reprodução nativa, alpha de SR e contrato de combate: passou nos 12 estados.
- Character Pack `--write` e `--check`: passou, 11 personagens e 352 atlases.
- Auditoria gráfica: 69 apontamentos globais, 7 críticos. Nenhum crítico nos 12 estados novos; 10 avisos desse lote continuam registrados.
- Harmonia: 21 problemas globais. Nenhum nos 12 estados novos.
- Gate obrigatório de poses: **rejeitado**, 10/40 ações aceitas, além dos dois estados neutros.

`p12_pose_gate.py`, limites de auditoria e etapas obrigatórias do workflow continuam em vigor. Revisão de pixels de origem não é aprovação final da arte. O relatório completo e as pendências por estado estão em `p12-video-batch-report.json`.

## Prévia e pendências

`p12-video-normalized-review/` contém GIFs dos 42 trechos reais e seis páginas com preparação, momento principal e recuperação. Todos os trechos de prévia estão marcados como rascunho; apenas os 12 estados acima estão integrados no runtime. Os outros 30 atlases de runtime ainda são o protótipo rejeitado e devem ser substituídos depois de uma revisão satisfatória.

Os vídeos têm defeitos que exigem revisão real: HIT_STAND passa do impacto diretamente para a queda; HIT_CROUCH ergue o corpo acima do limite agachado; 2H não mostra um golpe reconhecível; 2M tem efeito de poeira e perna de impacto voltada para trás; o agarrão tem um oponente embutido e o trecho escolhido de arremesso é uma mordida; jH ainda exige seleção correta do impacto descendente. Nos golpes que começam eretos, a transição do Idle curvado precisa de frames de preparação compatíveis, sem reduzir a anatomia para disfarçar a diferença.

Não há novo APK aprovado. A compilação/testes Android e o exercício no emulador aguardam as validações completas; este ambiente local também não possui SDK/emulador. Não foi cancelada nenhuma execução válida do GitHub Actions.
