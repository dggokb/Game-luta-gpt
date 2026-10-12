# Integração de seleção e sprite atual — 12/10/2026

Seleção em f43a702cb0ff2aee5f9c091f5a3da20b698f12d0 combinada com game-luta-sprite-gpt em 50fd05a609839b85e6c7916a26d6c3282e627f25, por solicitação explícita do usuário. Branch isolada integracao/selecao-sprite-atual-20261012.

O conflito do CharacterSelectRenderer preserva a seleção sequencial, efeitos fortes, concepts P09–P16 bloqueados e arte/enquadramento anterior de P01. Mantém HudConceptPortraits e hudPortrait para a HUD atual usar seus próprios retratos. As alterações de GameView da HUD e da seleção foram integradas, preservando as duas equipes escolhidas e a transição de entrada na luta.

HudRuntimeTest confirma os quatro passos de seleção antes de entrar na luta e testa assist, cooldown, prontidão e troca por toques reais. O workflow executa testes do modelo de seleção, integração com GameView, arte e efeitos da HUD, sistemas extraídos e duas verificações do aplicativo instalado no emulador, com capturas e vídeos. A branch game-luta-sprite-gpt não é alterada por esta integração.
