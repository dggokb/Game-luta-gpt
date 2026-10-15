# Todos os personagens — processamento das fontes existentes

Inventário da pasta de animações do Drive em 15/10/2026: **418 vídeos** nas pastas P01–P13, Dafu, Takatoshi e capangas. P14, P15 e P16 têm somente imagens. Nenhuma animação é criada para preencher essas ausências.

Cada personagem usa sua própria referência. O processo compara poses equivalentes, conserva todos os timestamps, produz PNGs normalizados em resolução integral e registra as matrizes de cada frame. Andar e dash usam `in_place_x`; a translação removida fica no manifesto. Vídeos que não oferecem uma calibração segura ficam rejeitados com o motivo explícito e com a fonte acessível em Antes.

As referências agachadas têm seus segmentos anatômicos conferidos. Quando a diferença de escala do mestre é confirmada por anotações equivalentes, o fator uniforme fica registrado em `master_coherence.crouch_scale_to_output`. A altura da pose não é usada para igualar corpos. Referências extraídas de vídeos existentes, usadas onde não há imagem mestre, registram a origem, o frame e o hash; a proporção desses personagens com os lutadores ainda exige revisão visual.

## Executar

As entradas reproduzíveis estão em [`examples/collection`](../examples/collection): inventário, mapas de estados e medidas de referência vinculadas ao hash das imagens. O inventário traz os IDs do Drive, os nomes das fontes e suas pastas. Baixe os originais para `<entradas>/<personagem>/<nome original>` sem renomear ou regravar os vídeos. Forneça o mapa de estados por personagem e as anotações de referência.

```bash
python scripts/process_collection.py \
  --inventory source-inventory.json --inputs /caminho/originais \
  --output /caminho/processados --mappings /caminho/mapas \
  --annotations /caminho/anotacoes
python scripts/check_collection.py \
  --inventory source-inventory.json --inputs /caminho/originais \
  --processed /caminho/processados --evidence /caminho/conferencia
python scripts/bundle_collection.py /caminho/processados --output /caminho/assets
```

`--characters p01 p02` limita uma execução. Pastas com `complete.json` são reaproveitadas; saídas parciais são recusadas para evitar sobrescrita. O decoder percorre cada vídeo inteiro e exige exatamente a contagem indicada pelo FFprobe. Os originais são preservados e conferidos por SHA-256 depois do processamento.

## Saídas

- `normalized/<clip>/<frame>.png`: sprites integrais, no canvas e root comuns daquele personagem.
- `assets/manifest.json`: calibração, origem, matriz de cada frame, PTS, root, avisos e motivos de rejeição.
- `assets/images`: prévias WebP em páginas; cada frame informa o retângulo correspondente. O empacotamento não descarta frames nem altera coordenadas anatômicas.
- `complete.json`: cobertura e pendências do personagem.

O pacote Android completo abre uma seleção de personagens. Cada personagem mantém Antes, Depois e Comparar, reprodução conjunta ou pose fixa da direita e alertas de origem. As prévias são reduzidas para celular; os PNGs integrais ficam na saída local da ferramenta.

A conferência automática não aprova a arte. Cortes, membros deformados, detalhes verdes removidos pelo chroma e mudanças de câmera podem exigir revisão ou uma fonte corrigida. As pendências aparecem individualmente no relatório.
