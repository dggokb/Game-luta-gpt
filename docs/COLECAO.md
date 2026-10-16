# Todos os personagens — processamento das fontes existentes

**Ensaio antes de ampliar o lote:** [dash em quatro personagens](ENSAIO-DASH-4-PERSONAGENS.md). P02 e P11 passam nos controles medidos; P06 e P08 ainda não. A coleção completa permanece sem essa revisão.

**Pendência confirmada após a entrega 1.5:** o [dash da P02 aumenta de tamanho durante a ação](REVISAO-P02-DASH.md). A calibração por Idle não verificava as demais poses. Os 330 convertidos continuam sendo saídas para revisão; esse número não comprova tamanho corporal estável durante todos os movimentos.

Inventário da pasta de animações do Drive em 15/10/2026: **418 vídeos** nas pastas P01–P13, Dafu, Takatoshi e capangas. P14, P15 e P16 têm somente imagens. Nenhuma animação é criada para preencher essas ausências.

Cada personagem usa sua própria referência. O processo compara poses equivalentes, conserva todos os timestamps, produz PNGs normalizados em resolução integral e registra as matrizes de cada frame. Andar e dash usam `in_place_x`; a translação removida fica no manifesto. Vídeos que não oferecem uma calibração segura ficam rejeitados com o motivo explícito e com a fonte acessível em Antes.

As referências agachadas têm seus segmentos anatômicos conferidos. Quando a diferença de escala do mestre é confirmada por anotações equivalentes, o fator uniforme fica registrado em `master_coherence.crouch_scale_to_output`. A altura da pose não é usada para igualar corpos. Referências extraídas de vídeos existentes, usadas onde não há imagem mestre, registram a origem, o frame e o hash; a proporção desses personagens com os lutadores ainda exige revisão visual.

## Resultado da coleção

Versão 1.5-preview: **418 vídeos / 41.083 frames**, com **330 movimentos convertidos** (32.261 PNGs integrais) e **88 sem calibração segura**. Foram registrados no lugar 37 movimentos de andar/dash. Os 88 pendentes mantêm todos os frames originais na aba Antes e o motivo visível; precisam de referência ou fonte corrigida antes da integração no jogo. P14, P15 e P16 não possuem vídeos no inventário.

[APK completo](https://github.com/dggokb/Game-luta-gpt/releases/download/sprites-preview-1.5-20261015/sprites-todos-personagens.apk) · [Resumo por personagem](evidence/collection-summary.json) · [Pendências por movimento](evidence/collection-review.csv) · [Hashes das 462 fontes](evidence/collection-source-hashes.json).

O chroma preserva detalhes verdes interiores por diferença de cromaticidade em P05, P10 e P12; o modo é explícito e não altera os pixels RGB. As fontes com fundo verde pouco saturado usam chave própria, e os fundos neutros de Dafu usam remoção da cor conectada à borda. Nenhuma configuração recupera partes já cortadas ou deformadas no vídeo.

Os [ZIPs por personagem, divididos em volumes,](https://drive.google.com/drive/folders/1l7rSJn2JHKcjqGGO33R1ybsfHVDaNTMa) incluem os PNGs integrais e metadados de cada movimento convertido. O arquivo `LEIA-ME.txt` explica os caminhos e a integração do deslocamento. Não incluem as texturas WebP do visualizador nem os vídeos originais. A lista de arquivos e hashes está em [sprite-archives.json](evidence/sprite-archives.json); os [links de cada volume](evidence/collection-downloads.json) foram conferidos por nome e tamanho no Drive.

Os arquivos maiores estão separados em volumes de até 95 MiB por limite do envio. Baixe todos os volumes do personagem (`.zip.001`, `.zip.002`, etc.) para a mesma pasta e abra o primeiro no 7-Zip/ZArchiver. No computador, a restauração com conferência de hash também pode ser feita com `scripts/join_collection_volumes.py`, usando [volume-manifest.json](evidence/volume-manifest.json). Capangas e P13 são ZIPs únicos. Não extraia os volumes separadamente.

## Executar

As entradas reproduzíveis estão em [`examples/collection`](../examples/collection): inventário, mapas de estados e medidas de referência vinculadas ao hash das imagens. O inventário traz os IDs do Drive, os nomes das fontes e suas pastas. Baixe os originais para `<entradas>/<personagem>/<nome original>` sem renomear ou regravar os vídeos. Forneça o mapa de estados por personagem e as anotações de referência.

```bash
python scripts/process_collection.py \
  --inventory source-inventory.json --inputs /caminho/originais \
  --output /caminho/processados --mappings /caminho/mapas \
  --annotations /caminho/anotacoes
python scripts/refine_collection_preview.py /caminho/processados
python scripts/check_collection.py \
  --inventory source-inventory.json --inputs /caminho/originais \
  --processed /caminho/processados --evidence /caminho/conferencia
python scripts/bundle_collection.py /caminho/processados --inventory source-inventory.json --output /caminho/assets
python scripts/compact_collection_bundle.py /caminho/assets --output /caminho/assets-celular
python scripts/check_collection_bundle.py /caminho/assets-celular --full-processed /caminho/processados --evidence /caminho/conferencia
python scripts/export_collection.py --processed /caminho/processados \
  --inventory source-inventory.json --output /caminho/zips
```

`--characters p01 p02` limita uma execução. Pastas com `complete.json` são reaproveitadas; saídas parciais são recusadas para evitar sobrescrita. O decoder percorre cada vídeo inteiro e exige exatamente a contagem indicada pelo FFprobe. Os originais são preservados e conferidos por SHA-256 depois do processamento.

## Saídas

- `normalized/<clip>/<frame>.png`: sprites integrais, no canvas e root comuns daquele personagem.
- `assets/manifest.json`: calibração, origem, matriz de cada frame, PTS, root, avisos e motivos de rejeição.
- `assets/images`: prévias WebP em páginas; os sprites mantêm detalhe nativo até 720 px por lado. Cada frame informa o retângulo da textura e `normalized_destination_rect`, sua posição no canvas comum. As áreas transparentes são empacotadas sem alterar o tamanho ou a posição do corpo na exibição. O empacotamento não descarta frames nem altera coordenadas anatômicas.
- `complete.json`: cobertura e pendências do personagem.

O pacote Android completo abre uma seleção de personagens. Cada personagem mantém Antes, Depois e Comparar, reprodução conjunta ou pose fixa da direita e alertas de origem. As prévias são reduzidas para celular; os PNGs integrais ficam na saída local da ferramenta.

A conferência automática não aprova a arte. Cortes, membros deformados, detalhes verdes removidos pelo chroma e mudanças de câmera podem exigir revisão ou uma fonte corrigida. As pendências aparecem individualmente no relatório.
