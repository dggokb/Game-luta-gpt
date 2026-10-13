# HUD com arte de energia

Branch: ui/hud-barras-referencia-20261012.

A moldura é uma imagem RGBA em assets/ui/hud/energy-frame.png, gerada a partir da referência enviada pelo autor. O atlas mede 1672×941. A região superior (0,0)-(1672,280) ocupa (0,0)-(1280,170) na tela virtual. Cada moldura inferior usa uma região de 750×206 e ocupa 400×95. O restante do atlas é transparente.

Vida, vida recuperável, medidores e retratos continuam sendo desenhados pelo HudRenderer a partir do estado real. As barras de vida ficam atrás da moldura e os medidores sobre a arte para cobrir os segmentos ilustrados. O número inferior mostra o nível real (0 a 5); os quatro segmentos ornamentais representam proporcionalmente a capacidade total de cinco barras. No treino, o relógio continua infinito.

O atlas é decodificado uma vez no construtor de GameView. Ausência do arquivo mantém o renderer vetorial anterior. Não há alterações de combate, sprites ou cenário.

Validação: HudArtTest usa Canvas nativo do Robolectric para verificar carregamento, transparência central e diferença entre vida cheia e vazia; exporta build/hud-review.png. O workflow roda os testes da HUD, controles, seleção e botões de vida, compila o APK e captura o jogo no emulador depois de tocar LUTAR. A semelhança visual deve ser avaliada pela captura final, sem tratar compilação como aprovação artística.
