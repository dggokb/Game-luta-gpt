# Visualizador offline para Android

[Baixar o APK](https://github.com/dggokb/Game-luta-gpt/raw/refs/heads/padronizacao-sprites/dist/sprites-visualizador.apk).

Android 8.0 ou superior, com Android System WebView atualizado. Baixe o arquivo pelo navegador do celular, abra-o e toque em **Instalar**. Se o Android pedir, permita a instalação pelo navegador utilizado.

O app abre com os 11 clips e os 1.307 frames dos lotes de teste e movimentação já incluídos. Não exige login, internet, acesso a arquivos, câmera ou microfone. A aba **Depois** abre primeiro; **Antes** mostra a fonte e **Comparar** coloca duas animações no mesmo zoom. Os botões de reprodução e avanço ficam abaixo da imagem. Em **Sequências, ciclos, mestre e zoom**, estão as transições Idle → ação → Idle para Jab, Dash, Crouch, andar para frente, andar para trás e Backdash, os ciclos propostos e a sobreposição do mestre. Gire o celular para comparar lado a lado com mais espaço.

A versão **1.3-preview** acrescenta o [lote de movimentação](../docs/MOVIMENTACAO.md), mantendo a [correção de escala do Crouch](../docs/CORRECAO-CROUCH.md) e a comparação com reprodução dos dois lados. Usa a mesma assinatura das versões 1.1 e 1.2 e pode atualizá-las diretamente. Quem ainda usa a versão 1.0 precisa desinstalá-la antes, pois essa versão tinha outra assinatura. O app não guarda projetos pessoais.

## Como comparar

1. Escolha **Animação** para o lado esquerdo e **Comparar com** para o direito. Selecionar a animação da direita abre a aba Comparar automaticamente.
2. Toque em **Reproduzir**. Os dois lados se movem no mesmo zoom; cada clip repete ao terminar. O nome e o número do frame aparecem acima de cada imagem.
3. Para conferir poses diferentes, arraste a barra **da direita**, abaixo dos controles de reprodução. Ela fixa essa pose e permite avançar/voltar um frame. A barra principal continua controlando a esquerda. Selecione **Junto com a esquerda** para voltar à reprodução conjunta.

Essa tela serve para conferir tamanho, apoio no chão e movimento dos resultados prontos. Escolher outra animação não recalcula a padronização.

Na versão anterior, a direita ficava sempre na pose usada na calibração. Como `crouch` começa em pé, ele parecia igual ao Idle mesmo durante o agachamento do outro lado.

As imagens do APK são prévias WebP com até 720 px de largura. Todos os frames e seus tempos foram conservados; as escalas, roots e offsets continuam nas coordenadas originais. Os PNGs integrais permanecem na saída da ferramenta local. Este APK é o **visualizador dos resultados incluídos**, não executa a normalização de novos vídeos no celular. O lote continua marcado para revisão.

## Compilação reproduzível

O wrapper usa apenas Activity/WebView do Android, sem biblioteca Android externa ou Gradle. O conteúdo vem do mesmo visualizador desktop, com disposição adaptada ao toque. Java 8 é o formato do bytecode; a compilação também funciona com o compilador de um JDK 21.

Ferramentas utilizadas: Android Platform 35 r2 e Build Tools 35.0.0. Os arquivos oficiais e checksums estão em [sdk-provenance.json](sdk-provenance.json). Instale o Android SDK ou extraia esses pacotes fora do repositório.

```bash
python -m pip install -r requirements-dev.txt
python scripts/prepare_mobile.py /caminho/resultados --output /caminho/android-build/assets
python scripts/check_mobile.py /caminho/android-build/assets --screenshots /caminho/android-build/checks
python scripts/build_android.py \
  --assets /caminho/android-build/assets \
  --android-jar /caminho/android-35/android.jar \
  --build-tools /caminho/build-tools/35.0.0 \
  --output dist/sprites-visualizador.apk \
  --work /caminho/android-build/package
```

Use diretórios novos ou vazios. O script recusa sobrescrever um APK existente. Ele cria uma chave de assinatura de prévia na pasta privada de compilação, compila recursos/classes/DEX, alinha o ZIP e assina/verifica o APK. Para reutilizar uma assinatura existente, passe `--signing-key /pasta/privada/preview-signing.p12 --signing-password-file /pasta/privada/signing-password`. A senha é lida diretamente pelo assinador, sem ser exibida. A chave e sua senha ficam fora do Git. Builds com outra chave não podem atualizar uma instalação anterior sem reinstalá-la.

O manifesto não declara permissões. O WebView bloqueia rede, arquivos externos, content URIs e navegação fora dos assets. Não existe ponte JavaScript para APIs nativas. As imagens e relatórios são recursos locais do app.

## Verificações desta entrega

- 17 verificações por toque em Chromium com tela 390 × 844 e rotação para 844 × 390: reprodução/pausa, avanço, antes/depois, escala comum, mestre, seis sequências, ciclo e comparação com duas animações, frame direito fixo e durações distintas. O teste reproduz Jab no frame 98 com Crouch à direita e confere a mudança da imagem exibida.
- 1.307 frames de antes e 1.307 de depois presentes no pacote, mantendo os PTS e duração.
- Nenhum erro JavaScript, requisição externa ou rolagem horizontal da página no teste de celular.
- APK inspecionado com `aapt`, ZIP íntegro, alinhamento válido e assinatura verificada com `apksigner`.
- Nenhuma permissão Android solicitada.

[Resultado dos testes móveis](mobile-checks.json) · [Relatório da compilação e SHA-256](../dist/sprites-visualizador.json).

A instalação e execução em aparelho Android físico não foram testadas neste ambiente. O teste de interface simula o navegador móvel; não substitui um aparelho ou emulador Android.
