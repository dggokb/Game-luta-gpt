# Game Luta GPT — Windows, atualizado com Motor Sprite V2

Origem da branch: **game-luta-sprite-gpt** (commit 582d1f6), não o fork antigo.
Motor V2 integrado sem substituir seleção de personagens, P05/P06, P03 super-resolução
e demais melhorias de sprites atuais. Refino de 4 timings P01 e mapeamento
de transições. O efeito visual de saída não interfere em hitboxes ou comandos.

## Abrir
Extrair o pacote completo e executar `GameLutaSpriteGPT.exe`. Java integrado.
WASD/setas; J/K/L golpes; U autocombo; I troca; O super; P IA.

## Limitações do port
Áudio do módulo Ultra é stub no Windows nesta revisão. O filtro das imagens é
bilinear do port Java2D. Testes de compilação e de roster não equivalem
à validação interativa em um Windows real.
