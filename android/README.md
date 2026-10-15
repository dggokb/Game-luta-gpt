# Visualizador offline para Android

[Baixar o APK](https://github.com/dggokb/Game-luta-gpt/raw/refs/heads/padronizacao-sprites/dist/sprites-visualizador.apk).

Android 8.0 ou superior, com Android System WebView atualizado. Baixe o arquivo pelo navegador do celular, abra-o e toque em **Instalar**. Se o Android pedir, permita a instalação pelo navegador utilizado.

O app abre com as 8 animações e os 944 frames do lote de validação já incluídos. Não exige login, internet, acesso a arquivos, câmera ou microfone. A aba **Depois** abre primeiro; **Antes** mostra a fonte e **Comparar** coloca duas animações no mesmo zoom. Os botões de reprodução e avanço ficam abaixo da imagem. Em **Sequências, ciclos, mestre e zoom**, estão as transições Idle → Jab → Idle, Idle → Dash → Idle e **Idle → Crouch → Idle**, os ciclos propostos e a sobreposição do mestre. Gire o celular para comparar lado a lado com mais espaço.

A versão **1.1-preview** inclui a [correção de escala do Crouch](../docs/CORRECAO-CROUCH.md). Esta compilação usa uma nova chave de prévia, pois a chave da compilação anterior não está neste ambiente. Para substituir a versão 1.0, desinstale o APK anterior e instale o novo; o Android não aceita atualização direta entre assinaturas diferentes. O app não guarda projetos pessoais.

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

Use diretórios novos ou vazios. O script recusa sobrescrever um APK existente. Ele cria uma chave de assinatura de prévia na pasta privada de compilação, compila recursos/classes/DEX, alinha o ZIP e assina/verifica o APK. A chave e sua senha ficam fora do Git. Builds com outra chave não podem atualizar uma instalação anterior sem reinstalá-la.

O manifesto não declara permissões. O WebView bloqueia rede, arquivos externos, content URIs e navegação fora dos assets. Não existe ponte JavaScript para APIs nativas. As imagens e relatórios são recursos locais do app.

## Verificações desta entrega

- 10 verificações por toque em Chromium com tela 390 × 844 e rotação para 844 × 390: reprodução/pausa, avanço, antes/depois, escala comum, mestre, sequências (incluindo Crouch) e ciclo.
- 944 frames de antes e 944 de depois presentes no pacote, mantendo os PTS e duração.
- Nenhum erro JavaScript, requisição externa ou rolagem horizontal da página no teste de celular.
- APK inspecionado com `aapt`, ZIP íntegro, alinhamento válido e assinatura verificada com `apksigner`.
- Nenhuma permissão Android solicitada.

[Resultado dos testes móveis](mobile-checks.json) · [Relatório da compilação e SHA-256](../dist/sprites-visualizador.json).

A instalação e execução em aparelho Android físico não foram testadas neste ambiente. O teste de interface simula o navegador móvel; não substitui um aparelho ou emulador Android.
