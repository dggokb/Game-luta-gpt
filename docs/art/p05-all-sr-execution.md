# P05 — melhoria gráfica completa (teste isolado)

**Branch:** `test/p05-all-sr-20261011` (base `game-luta-sprite-gpt` no commit `fd038e8027901ebbf97ffddc995a8b8efe8d11d4`).

**Inventário conferido:** 38 atlases P05, 549 frames únicos. Todos os 38 atlases estavam em resolução original (`pixelScale=1`) na base: inclusive o Idle. O projeto possui 42 estados/animações que compartilham 38 atlases, portanto processamos o atlas uma única vez.

| Nº | Atlas | Frames | Estado inicial |
|---:|---|---:|---|
| 1 | `p05_backdash` | 10 | Original, pronto para processar |
| 2 | `p05_crouch` | 8 | Original, pronto para processar |
| 3 | `p05_crouch_heavy` | 12 | Original, pronto para processar |
| 4 | `p05_crouch_light` | 11 | Original, pronto para processar |
| 5 | `p05_crouch_medium` | 16 | Original, pronto para processar |
| 6 | `p05_dash` | 6 | Original, pronto para processar |
| 7 | `p05_defeat` | 33 | Original, pronto para processar |
| 8 | `p05_defense_air` | 7 | Original, pronto para processar |
| 9 | `p05_defense_crouch` | 7 | Original, pronto para processar |
| 10 | `p05_defense_stand` | 8 | Original, pronto para processar |
| 11 | `p05_fall` | 6 | Original, pronto para processar |
| 12 | `p05_fall_down` | 11 | Original, pronto para processar |
| 13 | `p05_getup` | 12 | Original, pronto para processar |
| 14 | `p05_heavy_straight` | 23 | Original, pronto para processar |
| 15 | `p05_hit_air` | 4 | Original, pronto para processar |
| 16 | `p05_hit_crouch` | 9 | Original, pronto para processar |
| 17 | `p05_hit_stand` | 9 | Original, pronto para processar |
| 18 | `p05_idle` | 44 | Original, pronto para processar |
| 19 | `p05_intro` | 27 | Original, pronto para processar |
| 20 | `p05_jump` | 6 | Original, pronto para processar |
| 21 | `p05_jump_heavy` | 7 | Original, pronto para processar |
| 22 | `p05_jump_light` | 7 | Original, pronto para processar |
| 23 | `p05_jump_medium` | 7 | Original, pronto para processar |
| 24 | `p05_land` | 4 | Original, pronto para processar |
| 25 | `p05_light_jab` | 11 | Original, pronto para processar |
| 26 | `p05_medium_kick` | 14 | Original, pronto para processar |
| 27 | `p05_rise` | 6 | Original, pronto para processar |
| 28 | `p05_special_energy` | 11 | Original, pronto para processar |
| 29 | `p05_special_s2` | 20 | Original, pronto para processar |
| 30 | `p05_special_s3` | 21 | Original, pronto para processar |
| 31 | `p05_special_s4` | 20 | Original, pronto para processar |
| 32 | `p05_super_wave` | 22 | Original, pronto para processar |
| 33 | `p05_taunt` | 33 | Original, pronto para processar |
| 34 | `p05_throw` | 9 | Original, pronto para processar |
| 35 | `p05_ultra_beam` | 9 | Original, pronto para processar |
| 36 | `p05_victory` | 33 | Original, pronto para processar |
| 37 | `p05_walk_back` | 24 | Original, pronto para processar |
| 38 | `p05_walk_forward` | 22 | Original, pronto para processar |

## Processo

1. Cada atlas é produzido em job isolado com o modelo oficial Real-ESRGAN AnimeVideo-v3, acompanhado de SHA-256 da fonte, configuração, modelo e imagens resultantes; 4 jobs simultâneos no máximo.
2. O modelo melhora o RGB em 4× e reamostra a imagem para 2× em runtime; a transparência original é redimensionada via Lanczos, sem regenerar silhuetas, mudar sequência, poses, velocidade, frames ou root. Fallback para 1,5× somente se 2× não couber nos limites 4096px/64MiB.
3. Depois de **38/38 jobs** bem-sucedidos, a integração verifica hash e alfa de todos os frames, empacota **somente os P05 alterados**, mantém os relatórios das demais animações, regenera os metadados Java globais, verifica as referências e a geometria dos atlases.
4. O resultado é gravado apenas na branch de teste; nenhum merge com `game-luta-sprite-gpt` é feito automaticamente. A aprovação visual do APK de teste é necessária antes de integrar.

**Implementação:** `tools/sprites/p05_sr_batch.py` e `.github/workflows/p05-all-sr.yml`.
