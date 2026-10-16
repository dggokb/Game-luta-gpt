# Triagem seletiva dos movimentos

> **Revisão do usuário em 2026-10-16: resultado insuficiente para ampliar o lote.** P03 repete a fonte; P04/P07 apresentam quebra na corrida; P09/P10 apresentam aumento e quebra; P11 ainda amplia. A triagem abaixo registra ajustes de X/recorte, sem correção da escala nesses casos e sem validação da emenda do ciclo. A aprovação visual anterior de P11 foi superada por esse retorno. [Registro por caso](evidence/triagem/user-review.json) · [Fim → início dos recortes](evidence/triagem/review-seams.jpg).

A triagem encontrou problemas de posição horizontal e recorte verde em oito casos. Esta entrega acrescenta **531 PNGs candidatos**, sem reprocessar a coleção inteira. P02, P06, P08 e P11 permanecem como a referência visual aceita pelo usuário (“Deu certo”); seus 29 arquivos de prévia foram copiados sem alterar um byte. A aceitação visual não altera os controles técnicos documentados no ensaio anterior.

[Baixar APK de triagem, cerca de 40 MB](https://github.com/dggokb/Game-luta-gpt/raw/refs/heads/padronizacao-sprites/dist/sprites-triagem.apk). Versão `1.8-triagem`, versionCode 9, Android 8+, mesmo aplicativo e assinatura do teste dos quatro personagens. Atualiza esse teste; o APK completo 1.5 continua separado.

Abra **Rodar triagem** para percorrer os 13 casos. **Depois** e **Antes** mostram o mesmo frame, zoom e coordenadas, com painéis alinhados. **Ampliar depois** usa a largura disponível. O link **Abrir os 4 personagens já aprovados** abre a prévia anterior preservada. Escolher um caso só muda a exibição: as correções já estão nos arquivos.

| Caso | Resultado | Escopo |
| --- | --- | --- |
| P01 dash | Mantido | 121 frames anteriores; sem PNG novo |
| P03 dash | Pendente | Fonte inteira; sem calibração aprovada |
| P04 dash | X corrigido | Ciclo da fonte 35–46, 12 frames |
| P05 dash | Pendente | Fonte inteira; sem calibração aprovada |
| P07 dash | X corrigido | Ciclo da fonte 32–51, 20 frames |
| P09 dash | Mantido | 97 frames anteriores; sem PNG novo |
| P10 dash | X e recorte corrigidos | Ciclo da fonte 45–58, 14 frames |
| P12 dash | Pendente | Fonte inteira; sem calibração aprovada |
| P05 walk_back | Recorte corrigido | 97 frames completos |
| P10 back_dash | Recorte corrigido | 97 frames completos |
| P10 walk_forward | Recorte corrigido | 97 frames completos |
| P10 walk_back | Recorte corrigido | 97 frames completos |
| P12 walk_forward | Recorte corrigido | 97 frames completos |

Os índices da tabela começam em zero. A interface mostra os números começando em um. Os três dash corrigidos são variantes de ciclo explícitas, com seleção e tempos originais registrados; não substituem silenciosamente o vídeo completo. P03, P05 e P12 conservam a rejeição anterior, e seus painéis mostram somente a fonte.

[Baixar ZIP dos 531 PNGs candidatos — 65,6 MB](https://drive.google.com/file/d/13tFxVZRqIfI9WZ-8EshS8OH6jQg03Lj5/view?usp=drivesdk). Não inclui os PNGs antigos nem os vídeos originais.

## O que mudou

A escala uniforme e a transformação vertical de cada frame permanecem iguais às da coleção anterior. No dash, a deriva horizontal medida passou de **255,01 px em P04**, **40,84 px em P07** e **152,56 px em P10** para menos de `1e-7 px` no ponto usado no registro. Isso mede o alinhamento desse ponto, não a estabilidade de todas as articulações.

P04 usa uma região anotada da cabeça e P07 usa o chapéu. P10 usa 14 posições do olho anotadas individualmente: o registro automático não teve confiança suficiente. Há imprecisão manual de alguns pixels da fonte; não se apresenta esse caso como detecção anatômica automática. O deslocamento removido fica no manifesto; a posição no jogo deve ser controlada pelo motor.

O recorte anterior restaurava como roupa algumas manchas verdes ainda conectadas ao fundo. A opção explícita `green_art_enclosed_only: true` impede essa restauração exterior, mantendo a proteção dos detalhes verdes interiores. O RGB da fonte é preservado; somente o alpha pode diminuir. A opção fica desligada por padrão e foi aplicada apenas aos seis movimentos listados. As linhas claras de velocidade do dash de P10 já estão na fonte e foram conservadas.

P01 e P09 não apresentaram problema confirmado nesta triagem e permanecem intactos. A variação da silhueta, dos efeitos ou da perspectiva não foi usada como justificativa para redimensionar o corpo. Os dois movimentos de andar de P07 foram conferidos por um segundo indicador no chapéu, com variações de X de 7,88 e 9,14 px, e foram mantidos.

## Cobertura e limites

- Auditoria estrutural dos **418 vídeos**: 330 convertidos e 88 rejeitados, sem erro de metadados detectado. Matrizes e timestamps consistentes não comprovam anatomia estável.
- Inspeção visual por amostragem dos **37 clips de locomoção convertidos** e medição mais detalhada dos oito dash restantes. Essa amostragem pode deixar passar defeitos em frames intermediários.
- Os quatro dash já aceitos ficam preservados; os controles técnicos anteriores de P06 e P08 não foram convertidos em aprovação automática.
- Os demais movimentos receberam auditoria estrutural, sem aprovação visual de tamanho dentro de cada animação ou entre todas as poses. Esta entrega não certifica os 418 vídeos.
- O APK completo, os PNGs anteriores e os vídeos originais não foram alterados. Não houve teste em aparelho Android físico nesta execução.

## Verificação e reprodução

Os **83 testes passaram**. A verificação da entrega conferiu os 531 PNGs, reconstruiu 24 imagens a partir dos pixels reais da fonte, comparou os dois painéis com PNGs integrais e percorreu os 13 casos em Chromium móvel. Os 16 manifestos anteriores e os 29 arquivos dos quatro aprovados conservaram seus hashes. Foram verificadas telas 390 × 844 e 844 × 390, alinhamento dos painéis, seleção, avanço, repetição, reprodução de todos os casos e acesso aos quatro aprovados, sem erros JavaScript ou requisição de rede.

[Evidências e métricas](evidence/triagem/validation.json) · [Testes](evidence/triagem/tests.txt) · [Compilação e SHA-256 do APK](../dist/sprites-triagem.json) · [Planilha da auditoria](evidence/triagem/triage.csv) · [Comparação de amostras](evidence/triagem/comparison.jpg).

```bash
python scripts/triage_collection.py \
  --sources /caminho/fontes \
  --baseline /caminho/colecao-anterior/processed \
  --approved /caminho/ensaio-quatro/assets \
  --output /caminho/triagem-nova
python scripts/check_selective_triage.py \
  --folder /caminho/triagem-nova \
  --baseline /caminho/colecao-anterior/processed \
  --sources /caminho/fontes \
  --evidence /caminho/evidencias
python scripts/package_selective_triage.py \
  --folder /caminho/triagem-nova \
  --validation /caminho/evidencias/validation.json \
  --output /caminho/sprites-triagem-pngs.zip
```

A configuração em [selective-triage.json](../examples/collection/selective-triage.json) vincula todas as anotações aos SHA-256 das fontes. A geração exige uma saída nova ou vazia. O ZIP contém apenas os PNGs candidatos, manifesto para integração, configuração, relatório e auditoria; as imagens de prévia ficam no APK. Caminhos `baseline_png` apontam para a coleção anterior e acompanham seus hashes.
