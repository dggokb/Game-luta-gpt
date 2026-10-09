# P03 — Melhoria gráfica completa das animações

**Branch de testes:** `test/p03-all-sr-20261009`. Criada da versão `game-luta-sprite-gpt` do projeto.

O P03 tem **37 atlases próprios** e **563 frames distintos**. Idle (60 frames) já estava em HD, portanto o lote processará **36 atlases e 503 frames**. As animações JUMP_MEDIUM e THROW_GRAB/THROW_TOSS do P03 reutilizam sprites `player_base_*` (P01), já melhorados em outra etapa, portanto não entram neste lote.

| # | Atlas do P03 | Frames | Estado antes do processamento |
| ---: | --- | ---: | --- |
| 1 | `p03_backdash` | 10 | Pendente |
| 2 | `p03_crouch` | 8 | Pendente |
| 3 | `p03_crouch_heavy` | 12 | Pendente |
| 4 | `p03_crouch_light` | 11 | Pendente |
| 5 | `p03_crouch_medium` | 16 | Pendente |
| 6 | `p03_dash` | 6 | Pendente |
| 7 | `p03_defeat` | 33 | Pendente |
| 8 | `p03_defense_air` | 6 | Pendente |
| 9 | `p03_defense_crouch` | 6 | Pendente |
| 10 | `p03_defense_stand` | 8 | Pendente |
| 11 | `p03_energy` | 11 | Pendente |
| 12 | `p03_fall` | 6 | Pendente |
| 13 | `p03_fall_down` | 11 | Pendente |
| 14 | `p03_getup` | 12 | Pendente |
| 15 | `p03_heavy` | 23 | Pendente |
| 16 | `p03_hit_air` | 4 | Pendente |
| 17 | `p03_hit_crouch` | 9 | Pendente |
| 18 | `p03_hit_stand` | 9 | Pendente |
| 19 | `p03_idle` | 60 | HD integrado |
| 20 | `p03_intro` | 27 | Pendente |
| 21 | `p03_jab` | 11 | Pendente |
| 22 | `p03_jump` | 6 | Pendente |
| 23 | `p03_jump_heavy` | 7 | Pendente |
| 24 | `p03_jump_heavy_down` | 7 | Pendente |
| 25 | `p03_jump_light` | 7 | Pendente |
| 26 | `p03_land` | 4 | Pendente |
| 27 | `p03_medium` | 14 | Pendente |
| 28 | `p03_rise` | 6 | Pendente |
| 29 | `p03_special_s2` | 20 | Pendente |
| 30 | `p03_special_s3` | 21 | Pendente |
| 31 | `p03_special_s4` | 20 | Pendente |
| 32 | `p03_super` | 22 | Pendente |
| 33 | `p03_taunt` | 33 | Pendente |
| 34 | `p03_ultra_beam` | 9 | Pendente |
| 35 | `p03_victory` | 33 | Pendente |
| 36 | `p03_walk_back` | 19 | Pendente |
| 37 | `p03_walk_forward` | 36 | Pendente |

## Processo

1. O GitHub Actions executa um job por animação pendente (até quatro em paralelo), usando Real-ESRGAN AnimeVideo-v3 oficial com SHA-256 conferido. Reaproveita o processo aprovado nos P01/P02/P03: RGB em 4× e alfa original ampliado por Lanczos; não redesenha poses nem realinha quadros.
2. Cada job cria uma textura 2× (fallback de 1,5× apenas se um atlas não couber nas restrições de 4096px/64MiB), registra hash do modelo, fonte, configuração, master e textura, e publica um artefato para integração.
3. Após **todos os 36 jobs** terminarem com sucesso, a integração recompõe **somente** os atlases P03 alterados, verifica a transparência e a ordem de todos os frames, gera os metadados Java e valida as referências de todos os personagens sem refazer os PNGs não alterados.
4. Somente após aprovação técnica o GitHub publica os arquivos na branch de testes. A `game-luta-sprite-gpt` não é atualizada por esse job. Nenhum APK gerado automaticamente. Aprovação visual no celular/PC continua necessária antes de integrar na branch do jogo.

Workflow: `.github/workflows/p03-all-sr.yml`; script: `tools/sprites/p03_sr_batch.py`.
