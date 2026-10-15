# Padronizador universal de sprites

Ferramenta local e independente para calibrar vídeos existentes por imagens mestres. Não depende de jogo, motor, personagem, serviço pago ou geração por IA. Nenhuma pose ou movimento é sintetizado.

A escala de referência é calculada a partir de uma pose equivalente ao Idle Master ou Crouch Master. Por padrão, uma transformação uniforme e constante é aplicada a todos os frames. Vídeos que mudam de tamanho durante a animação podem receber uma **correção explícita por segmentos anatômicos anotados**, interpolada pelos timestamps originais. O canvas global cresce para acomodar golpes e deslocamentos; o personagem não encolhe para caber nele.

## Ver no celular

[Baixar o APK Android](https://github.com/dggokb/Game-luta-gpt/raw/refs/heads/padronizacao-sprites/dist/sprites-visualizador.apk). Ele já inclui as 8 animações e os 944 frames do lote, funciona offline e tem controles por toque. Android 8.0+, com WebView atualizado. [Uso e compilação](android/README.md).

## Instalação e execução

Python 3.10+, FFmpeg e FFprobe no PATH. Testado com Python 3.12 e FFmpeg 7.1.5.

```bash
python -m pip install -r requirements.txt
python sprite_standardizer.py examples/project.example.json --output resultado-novo
```

Abra `resultado-novo/index.html` no navegador. Funciona por `file://`, sem servidor ou acesso à internet. A saída deve estar vazia ou ser nova. A ferramenta recusa sobrescrever resultados ou colocar fontes dentro da saída.

## Calibração que pode ser auditada

1. Forneça Idle Master e Crouch Master, em PNG com alpha ou fundo verde. A altura nativa do Idle Master define a escala por padrão. `idle_target_height` é uma redução/ampliação **explícita** opcional.
2. Para verificar a coerência entre mestres de poses diferentes, marque os extremos de segmentos anatômicos rígidos equivalentes, por exemplo o eixo da cabeça. `anatomical_segments` contém dois pontos `[x,y]` por mestre, em pixels originais. Use vários segmentos quando puder. O relatório mede seus comprimentos e mostra as marcações; a qualidade dessa verificação depende da qualidade das anotações.
3. Diferenças acima de `master_scale_tolerance` (8% por padrão) ou ausência de anotações bloqueiam a calibração por Crouch. Nenhuma correção entre mestres é inventada. Fornecer apenas Idle é permitido para um lote exclusivamente em pé.
4. Declare a **pose que realmente está no vídeo** em `reference_pose`. Um vídeo que começa em Idle e depois agacha pode usar Idle para a calibração. O programa inspeciona todos os frames; prefere o primeiro entre candidatos com qualidade próxima à melhor. `reference_frame` fixa um candidato, mas não contorna os critérios de qualidade.
5. Silhueta, aparência RGB e concordância das escalas horizontal/vertical precisam concordar. Candidatos com corte ou fundo residual não calibram. Poses equivalentes que variam mais de 6% de altura dentro do mesmo vídeo bloqueiam o clip sem correção anotada: uma escala fixa não corrige zoom/deformação. Uma correção explícita não dispensa os critérios de qualidade da pose.
6. O fator vem da altura da **pose equivalente**, preservando proporções. Não se usa a maior altura durante um golpe, nem a resolução do vídeo. A proporção Idle/Crouch só é utilizada depois de verificar os segmentos anatômicos.

`minimum_similarity` (padrão 0.82) ajusta o score agregado. Há também limites mínimos conservadores para silhueta (0.78), aparência (0.70) e concordância dimensional (0.88). Diminuir o score não ignora esses limites. Clips rejeitados conservam todos os frames brutos e candidatos medidos para revisão. O CLI retorna código 2 quando há rejeição; o HTML continua disponível. Não use limiares baixos para forçar uma pose incompatível.

`target_heights` da versão anterior foi removido: alturas independentes para Idle/Crouch não comprovam escala anatômica. Migre para mestres coerentes e segmentos anotados.

## Diferença de tamanho durante Idle → Crouch

Calibrar somente o Idle inicial não verifica o restante do movimento. No lote real, a medida anotada da cabeça crescia até cerca de 13% durante o Crouch. Esse crescimento passava para os frames exportados e para o APK.

`scale_correction` em um clip permite anotar o **mesmo segmento anatômico** em keyframes, incluindo início, fim e a pose de calibração. Consulte [as marcações reais](examples/real-scale-corrections.json) e [a comparação antes/depois](docs/CORRECAO-CROUCH.md). A curva interpola o comprimento do segmento pelo tempo original e aplica uma escala uniforme ao frame inteiro. A medida da pose inicial é conservada; a altura total do corpo não é igualada entre Idle e Crouch. Redução por postura continua visível.

`ground_y` opcional anota o apoio vertical em todos os keyframes de clips `stationary` ou `crouched`. Ele mantém o chão indicado sem recentralizar X. É recusado para Jump e Dash. As marcações reais incluem o SHA-256 da fonte, impedindo seu uso em outro vídeo. Variações acima de `maximum_change_fraction` (20% por padrão, máximo configurável de 50%) são rejeitadas.

Esse modo é opt-in. Não detecta anatomia automaticamente, não redesenha membros e não corrige diferenças internas de proporção. A precisão depende das marcações; pontos intermediários devem ser conferidos fora dos keyframes utilizados.

## Coordenadas, root e movimento

O projeto tem um canvas, uma origem, X para a direita e Y para baixo. Por padrão o root é o centro inferior da silhueta da pose de referência. Esse ponto é uma **referência de apoio mensurável**, não uma articulação esquelética detectada. `master_roots` permite anotar outro root em cada mestre; `source_root` pode especificar o ponto equivalente no clip. Esses valores ficam nos metadados.

O manifesto v3 registra `transform_source_to_output` e `scale_applied` em cada frame. Clips constantes também mantêm a matriz única no nível do clip. Clips corrigidos registram `scale_mode: "annotated_segment_curve"`, a curva medida e `calibration_transform_source_to_output`; consumidores devem usar a matriz de cada frame. Não há recentralização horizontal por frame. Dash mantém movimento horizontal; Jump mantém movimento vertical. Os offsets medem o desenho em relação ao root comum. A trajetória do apoio da silhueta é um indicador visual e também muda quando braços/pernas mudam a bbox; não deve ser tratada como root motion esquelético.

`animation_type` aceita `stationary`, `translation`, `airborne` e `crouched`. O resumo de movimento também propõe uma classificação conservadora baseada no apoio observado. Distinguir movimento de câmera, mudança de pose e locomoção exige revisão. A classificação não modifica pixels.

No visualizador, `role: "idle"`, `"jab"`, `"dash"` e `"crouch"` habilitam as respectivas sequências. A comparação usa o mesmo zoom nos dois painéis normalizados. Ao encadear clips, a prévia conserva o apoio final para posicionar a próxima pose; essa translação é apenas da prévia e não altera os PNGs.

Em **Comparar com**, a animação da direita acompanha a reprodução pelos timestamps originais. Sua barra própria permite escolher e fixar qualquer frame para comparar poses diferentes; **Junto com a esquerda** retoma a reprodução conjunta. Os dois lados mostram nome e número do frame. A comparação apenas exibe os resultados já normalizados. [Uso no celular](android/README.md#como-comparar).

## Fundo e qualidade

O tratamento aceita transparência existente ou chroma verde. O redimensionamento usa alpha pré-multiplicado para evitar halos verdes de interpolação. Corpos/efeitos desconectados são preservados e sinalizados; não são apagados automaticamente.

`background.exclude_rectangles` permite excluir explicitamente regiões do fundo, como uma marca d'água, somente nos derivados. A configuração e o manifesto registram essas regiões. Use-as apenas onde não há arte a preservar. Não há remoção automática de marcas, cenários arbitrários ou reconstrução de membros cortados. Personagens com detalhes verdes devem usar transparência existente.

## Extração integral e exportação

FFprobe registra PTS original, base de tempo, FPS médio/nominal e duração. FFmpeg extrai o primeiro stream de vídeo com `fps_mode=passthrough`, sem seleção ou descarte de frames, sem autorrotação e sem alteração de resolução. Até frames visualmente repetidos são mantidos. PNGs brutos são cópias sem perda dos pixels decodificados RGB/RGBA de 8 bits; a compressão anterior do vídeo não pode ser desfeita. HDR/mais de 8 bits são recusados para evitar quantização silenciosa. Rotação da fonte é registrada sem aplicá-la.

```text
resultado/
  index.html                       # player, antes/depois, comparação e sequências
  manifest.json                    # escala, matriz, canvas, root, PTS, bbox e offsets
  calibration-report.json          # métricas, rejeições, alertas e coerência dos mestres
  calibration-comparison.png       # poses calibradas lado a lado
  project.snapshot.json            # configuração exata da execução
  masters/                         # referências derivadas e segmentos anotados
  nome-do-clip/
    raw/000000.png ...             # todos os frames originais decodificados
    raw/extraction.json            # resolução, contagem, PTS e SHA-256 da fonte
    normalized/000000.png ...      # PNGs transparentes com a matriz constante
    metadata.json
    spritesheets/page-000.png ...  # páginas com células integrais, sem recorte ou redução
    spritesheets/atlas.json        # retângulos e root em cada célula
```

O atlas é paginado para limitar memória. Células maiores que `atlas_max_size` ganham sua própria página, sem reduzir a imagem. As prévias são o player HTML e a comparação PNG; o programa não cria novos vídeos de animação.

## Testes

```bash
python -m unittest discover -s tests -v
```

A [validação documentada](docs/VALIDACAO.md) inclui uma [prévia portátil](docs/evidence/preview.html), que pode ser aberta localmente após baixar o repositório.

Os testes unitários usam matrizes genéricas, sem gerar personagens, poses ou vídeos artificiais. Os testes reais são separados e ficam explicitamente ignorados quando não há um lote real. Para reproduzir o lote documentado, baixe as entradas existentes listadas em [docs/VALIDACAO.md](docs/VALIDACAO.md) para uma pasta local:

```bash
python scripts/validate_real.py --inputs /caminho/originais --output /caminho/validacao-nova
SPRITE_REAL_RESULT=/caminho/validacao-nova/run python -m unittest discover -s tests -v
python -m pip install -r requirements-dev.txt
python -m playwright install chromium
python scripts/check_viewer.py /caminho/validacao-nova/run --screenshots /caminho/evidencias
```

O teste controlado de meia resolução transforma geometricamente todos os frames de um vídeo real existente e registra o comando e hashes. Não cria poses nem movimento novos. Os vídeos originais não são regravados e não entram no repositório. Há também validação entre fontes nativas em escalas anatômicas diferentes, em pé e agachadas.

## Limites de validação

Um score alto não comprova identidade anatômica universal. Silhuetas semelhantes podem esconder proporções internas diferentes. Segmentos anotados, comparação visual, metadados e testes medem aspectos distintos; nenhum deles sozinho aprova um lote. O manifesto sempre começa com `visual_approval: false`.

A ferramenta não promete eliminar trepidação nem corrigir arte deformada. Interpolação pode mudar a borda em 1–2 pixels. Perspectiva, zoom dentro do clip, reflexos, sombras, detalhes verdes e rotação/cortes da fonte podem exigir preparação externa ou rejeição. A detecção de loops apresenta sugestões por sequência e qualidade de emenda; não descarta frames e não garante que a melhor hipótese seja o ciclo artisticamente correto.
