# P15 — HD completo com integração WebP atual

Branch de testes: `test/p15-all-sr-20261022`, criada a partir de `game-luta-sprite-gpt` no commit `df24d82006baa5c712bd68f0e4afbd860524bd7e`.

**Inventário:** 22 atlases próprios do P15, 493 quadros únicos. Os 22 atlases originais estão em `pixelScale=1`; há 25 estados registrados no personagem, com estados que compartilham atlas (ex.: SUPER_WAVE e ULTRA_BEAM). Todos os 22 entram no HD, incluindo os dois Idles. O trabalho é limitado a **melhorar a nitidez**, sem inventar poses, quadros, efeitos nem mudar o timing das animações.

| Atlas | Frames | Origem |
| --- | ---: | --- |
| `p15_backdash` | 10 | Mestre normalizado 1× |
| `p15_crouch` | 8 | Mestre normalizado 1× |
| `p15_crouch_heavy` | 8 | Mestre normalizado 1× |
| `p15_crouch_light` | 6 | Mestre normalizado 1× |
| `p15_crouch_medium` | 83 | Mestre normalizado 1× |
| `p15_dash` | 18 | Mestre normalizado 1× |
| `p15_fall` | 6 | Mestre normalizado 1× |
| `p15_golpe_alto` | 13 | Mestre normalizado 1× |
| `p15_heavy_straight` | 79 | Mestre normalizado 1× |
| `p15_idle` | 58 | Mestre normalizado 1× |
| `p15_idle_2` | 32 | Mestre normalizado 1× |
| `p15_jump` | 6 | Mestre normalizado 1× |
| `p15_land` | 4 | Mestre normalizado 1× |
| `p15_light_jab` | 6 | Mestre normalizado 1× |
| `p15_medium_kick` | 14 | Mestre normalizado 1× |
| `p15_rise` | 9 | Mestre normalizado 1× |
| `p15_special_energy` | 11 | Mestre normalizado 1× |
| `p15_special_s2` | 12 | Mestre normalizado 1× |
| `p15_special_s3` | 29 | Mestre normalizado 1× |
| `p15_special_s4` | 37 | Mestre normalizado 1× |
| `p15_walk_back` | 24 | Mestre normalizado 1× |
| `p15_walk_forward` | 20 | Mestre normalizado 1× |

## Passos da pipeline

1. Validação das 22 fontes e seus recortes (`prepared-grid`), conservando raízes e frame count.
2. 22 jobs de super-resolução Real-ESRGAN AnimeVideo-v3 oficial; o modelo recebe RGB original, o alpha original é mantido por reamostragem, resultado 2× (fallback 1,5× quando indispensável). Máximo 4 jobs simultâneos. Cada saída inclui hashes da fonte, pesos e arquivo gerado.
3. Na integração, validar identidade/hash, alpha e ordem de quadros. Publicar apenas as 22 fontes HD e seus clipes; reconstruir só os **22 atlases alterados** com `import_sprites.process_clip` e `build_characters.pack_atlas` no **layout WebP/caixas por frame** adotado no jogo atualmente. Atualizar `GeneratedCharacters.java` e `GeneratedSpriteLayouts.java` sem regenerar outras texturas.
4. Somente publicar na branch de teste se todos os jobs e checagens terminarem com sucesso. Não fazer merge automático na `game-luta-sprite-gpt`. APK e aprovação visual são etapas posteriores.

`tools/sprites/p15_sr_batch.py` — `.github/workflows/p15-all-sr.yml`.
