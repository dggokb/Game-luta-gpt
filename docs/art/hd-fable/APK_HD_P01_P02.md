# Build de verificacao HD — Ryūga (P01) e Yín-lù (P02)

A integração HD foi produzida em `9d120b0ef258f7f97aa9487ae300f06ddda3f09c`
por GitHub Actions a partir dos 12 lotes Real-ESRGAN AnimeVideo-v3.

- P01: fontes RGB refinadas, preservando quadros, transparência, posição e `pixelScale: 2`.
- P02: clipes que estavam em 1x passaram a usar folha 2x e `pixelScale: 2`; os já existentes em 2x foram preservados.
- Atlas WebP e layouts Java foram reconstruídos e verificados.
- A renderização não desenha novas poses nem altera mecânicas.
- O APK anterior da release foi compilado em `164ef05`, **antes** de `9d120b0`. Ele não é adequado para julgar o resultado HD.

Este arquivo também dispara o workflow normal `Build Game Luta APK`, para
publicar uma versão compilada *depois* da integração das imagens.
Verifique o SHA do workflow antes de usar o APK publicado.
