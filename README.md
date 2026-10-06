# Game Luta GPT — protótipo Android

Projeto separado de protótipo de luta 3v3 baseado em sistemas universais: movimentação, L/M/H/S, defesa, Super Dash, Dragon Rush, Reflect, Vanish, Ki, assistência, troca, vida recuperável, Sparking, Limit Break, HUD e CPU simples.

O protótipo usa lutadores genéricos e não contém assets ou golpes de personagens licenciados.

## Estrutura
- `android/app/` — app Android (`GameView.java` desenha a luta com Canvas)
- `android/ultra-core/` — motor do ultra "Página Final", em Java puro (roda no Android e no PC)
- `android/ultra-preview/` — visualizador do ultra no PC
- `android/app/src/main/assets/ultras/` — ultras de cada lutador (ultra.json, imagens, sons)
- `tools/ultra/` — script e prompts para preparar a arte dos ultras ([guia](tools/ultra/README.md))
- `.github/workflows/build-apk.yml` — build do APK

## Ultra: Página Final
**Baixo + SUPER** com 3 barras. Se a investida acertar, a luta vira uma página de mangá montada em 5 painéis. Toque quando o anel fechar para ganhar bônus de dano. Para criar ou trocar a arte de um ultra, veja [tools/ultra/README.md](tools/ultra/README.md).

## Build local
```bash
cd android
gradle assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`

Testes do motor do ultra: `gradle :ultra-core:test` (dentro de `android/`).
