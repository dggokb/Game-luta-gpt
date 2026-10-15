# P02 — aumento de tamanho durante o dash

O dash da P02 cresce durante a ação e volta ao tamanho inicial. A versão 1.5 exportou essa variação: a pose inicial tinha similaridade de 0,9766 com Idle e as dez poses equivalentes encontradas tinham variação de altura zero. Isso não conferia a anatomia das poses de corrida.

[APK de teste da P02 — aproximadamente 5,9 MB](https://github.com/dggokb/Game-luta-gpt/raw/refs/heads/padronizacao-sprites/dist/sprites-p02-dash-teste.apk). Instala como **Sprites — P02 Dash (teste)**, separado do visualizador completo. Abre no frame 27/97: correção experimental à esquerda e versão anterior à direita, com o mesmo zoom. Reproduza para ver a entrada, a corrida e o retorno. A versão completa 1.5 permanece sem essa correção.

![Cabeça antes e depois, com zoom constante](evidence/p02-dash-head-comparison.png)

## O que foi medido

As marcações aproximadas estão em [motion-landmarks/p02.json](../examples/collection/motion-landmarks/p02.json), vinculadas ao SHA-256 do vídeo. O eixo aproxima crânio–queixo, evitando as pontas do cabelo ao vento. Os centros dos ombros são um controle auxiliar, com incerteza de marcação e de projeção.

| Medida | Versão anterior | Correção experimental |
| --- | --- | --- |
| Eixo da cabeça nos frames 0 e 96 | 76,2 px | 76,2 px |
| Eixo da cabeça no frame 26 | 94,8 px (+24,5%) | 80,2 px (+5,3%) |
| Maior diferença absoluta nas oito amostras de controle da cabeça | fora da tolerância | 5,3% |
| Referência de tamanho | Idle inicial | 18 marcações de correção ao longo das fases |

Há 26 checkpoints entre preparação, dash, recuperação e Idle. **Oito medidas da cabeça ficaram fora da curva usada na correção** e servem de controle. Há cinco medidas dos ombros, incluindo a referência inicial/final e três durante a ação; elas não determinam a curva. A tolerância usada para cabeça é 6%; para ombros, 12%, considerando a projeção e a marcação aproximada.

![Variação nas amostras de cabeça e ombros](evidence/p02-dash-scale-chart.png)

A curva aplica uma escala uniforme ao frame inteiro, interpolada pelos timestamps originais; não iguala a altura de uma pose agachada com a de uma pose em pé. O fator varia de 0,8088 a 1,0444 em relação à calibração inicial. Os 97 frames e seus tempos foram conservados. Os pixels normalizados do primeiro e do último frame permanecem idênticos aos da versão anterior.

Isso **não é uma validação anatômica de todos os frames ou de todos os membros**. As marcações são manuais, comprimentos projetados variam com a orientação, e as amostras intermediárias restantes exigem revisão. Poeira e linhas de velocidade da fonte permanecem; elas inflavam a caixa geral e também limitam a confiabilidade de medidas baseadas nessa caixa. O APK de teste aproxima a câmera sobre uma região fixa do corpo, igualmente nos dois painéis; isso não altera os PNGs nem seus fatores de escala.

## Nova conferência

`scale_audit` mede segmentos nomeados nas fases declaradas, antes de liberar a exportação normalizada. Uma mudança acima da tolerância nos pontos conferidos rejeita o clip com o frame e o segmento responsáveis, mesmo que a comparação com Idle tenha passado. Sem essas anotações, o estado é **not_checked** e o visualizador informa que o tamanho nas outras poses ainda não foi conferido. `within_annotation_tolerance` vale para as amostras, sem aprovação automática dos demais frames.

O processamento em lote aceita `--motion-annotations`, com padrão em `examples/collection/motion-landmarks`. A P02 tem anotações para dash; os outros movimentos não receberam essa conferência adicional. Quando esse arquivo é adicionado ou alterado, uma saída antiga concluída é recusada: use outro diretório para reprocessar. Isso evita colocar medidas novas e receber silenciosamente o resultado antigo.

## Reproduzir

```bash
python scripts/review_p02_dash.py \
  --source /caminho/originais/p02/dash.mp4 \
  --baseline /caminho/colecao-1.5/p02 --output /caminho/revisao-nova
python scripts/check_p02_review.py /caminho/revisao-nova \
  --source /caminho/originais/p02/dash.mp4 --evidence /caminho/conferencia
SPRITE_P02_RESULT=/caminho/colecao-1.5/p02 python -m unittest tests.test_scale_audit -v
```

[Baixar a revisão com PNGs integrais e HTML portátil — 36,2 MB](https://drive.google.com/file/d/1igx60I8nuW-yHxNpjO1PHRPp1BX52NTq/view).

O resultado inclui `assets/index.html`, uma prévia portátil `p02-dash-comparacao.html` com imagens incorporadas e os PNGs integrais das duas versões em `normalized/`. Para a correção experimental, use a pasta `dash_corrigido` e sua matriz por frame no manifesto. A versão anterior acompanha o resultado exclusivamente para comparação.

O APK separado é compilado pelo mesmo wrapper, usando `--application-id org.spriteviewer.p02review --application-label 'Sprites — P02 Dash (teste)'`. O Java permanece no pacote original; o manifesto registra a Activity pelo nome completo. A chave continua fora do Git e nenhuma permissão Android é solicitada.

## Validação

**69 testes passaram**, incluindo a regressão real da P02, controles que não participaram da curva e invalidação de resultados antigos quando as anotações mudam. O processamento normal de um lote isolado com Idle e Dash também aplicou a correção, registrou o hash das medidas e preservou as fontes.

Foram conferidos os 194 PNGs (97 de cada versão), dimensões, limites, PTS, escala uniforme, comparação com os PNGs integrais, reprodução, fim da animação e navegação em retrato/paisagem. Tanto a página com arquivos externos quanto a página portátil passaram sem erros JavaScript ou requisições externas. O HTML dentro do APK confere byte a byte com o testado. A instalação em aparelho físico não foi testada neste ambiente.

[Métricas da revisão](evidence/p02-dash-checks.json) · [Log dos 69 testes](evidence/p02-dash-test-results.txt) · [APK e assinatura](../dist/sprites-p02-dash-teste.json).
