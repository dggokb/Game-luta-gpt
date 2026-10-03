# Game Luta GPT — protótipo Android

Projeto separado de protótipo de luta 3v3 baseado em sistemas universais: movimentação, L/M/H/S, defesa, Super Dash, Dragon Rush, Reflect, Vanish, Ki, assistência, troca, vida recuperável, Sparking, Limit Break, HUD e CPU simples.

O protótipo usa lutadores genéricos e não contém assets ou golpes de personagens licenciados.

## Estrutura
- `android/` — app Android/WebView
- `android/app/src/main/assets/index.html` — jogo Canvas/HTML5
- `.github/workflows/build-apk.yml` — build do APK

## Build local
```bash
cd android
gradle assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`
