# HUD — recarga de ASSIST e retratos de concepts

Branch de teste: ui/hud-barras-referencia-20261012.

Os cinco segmentos logo abaixo da vida agora mostram tagReadyRatio(), isto é, a disponibilidade real de ASSIST/TAG fornecida pelo GameView e TeamSystem. Cada segmento corresponde a 20% da recarga; durante entrada/saída do assist ou troca ficam vazios. A leitura é limitada a [0,1], preservando os tempos e regras existentes. O SUPER continua usando exclusivamente superMeter na parte inferior. Na CPU de treino, que não possui reserva jogável, a barra superior fica desativada. O pequeno indicador circular permanece ligado à disponibilidade do parceiro. O renderer vetorial alternativo também foi corrigido.

Os retratos da HUD vêm dos model sheets p01.png a p08.png da pasta concepts do Drive: https://drive.google.com/drive/folders/1mGv56-qpEFg5GoFzoL4irTcsoST88BT2. Os originais completos foram convertidos para WebP lossless e a igualdade dos pixels RGBA foi verificada. HudConceptPortraits recorta em tempo de execução os closes de rosto, preservando as artes originais. O mapeamento player_base → p01 é explícito; os demais seguem seus IDs. Cache por personagem; não há extração de frames de luta para os retratos da HUD. A seleção existente mantém seu renderer anterior.

assets/ui/hud/concepts/sources.json registra arquivo e URL de origem, tamanho e SHA-256 de cada asset. Os blobs enviados foram conferidos pelo hash Git antes do commit.

Validação: HudArtTest verifica que mudar SUPER não altera a barra de ASSIST, que o cooldown esvazia e depois preenche essa barra sem mudar SUPER, e que os oito retratos vêm dos assets de concepts. A prévia do Canvas nativo é exportada pelo mesmo teste.

