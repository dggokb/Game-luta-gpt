# P04: super-resolução de todas as animações (execução de teste)

Branch: `test/p04-all-sr-20261009`. Origem: `game-luta-sprite-gpt` no commit `0bf9a794a0e95cc8c5b54ca399c149af3e45a918`.

**Inventário:** 38 atlases P04 e 581 frames únicos. **Todos os 38 atlases são alvos**, incluindo o Idle (68 frames): o workflow antigo `art/p04-idle-sr-20261009` falhou em testes após gerar a imagem e **não publicou** o atlas de Idle no código do jogo. Nenhuma melhoria é dada como integrada sem conferir os arquivos efetivamente publicados.

| # | Atlas | Frames | Status inicial |
| ---: | --- | ---: | --- |
| 1 | `p04_backdash` | 10 | A processar |
| 2 | `p04_crouch` | 8 | A processar |
| 3 | `p04_crouch_heavy` | 12 | A processar |
| 4 | `p04_crouch_light` | 11 | A processar |
| 5 | `p04_crouch_medium` | 16 | A processar |
| 6 | `p04_dash` | 5 | A processar |
| 7 | `p04_defeat` | 33 | A processar |
| 8 | `p04_defense_air` | 7 | A processar |
| 9 | `p04_defense_crouch` | 7 | A processar |
| 10 | `p04_defense_stand` | 8 | A processar |
| 11 | `p04_fall` | 6 | A processar |
| 12 | `p04_fall_down` | 11 | A processar |
| 13 | `p04_getup` | 12 | A processar |
| 14 | `p04_heavy_straight` | 23 | A processar |
| 15 | `p04_hit_air` | 4 | A processar |
| 16 | `p04_hit_crouch` | 9 | A processar |
| 17 | `p04_hit_stand` | 9 | A processar |
| 18 | `p04_idle` | 68 | A processar |
| 19 | `p04_intro` | 27 | A processar |
| 20 | `p04_jump` | 6 | A processar |
| 21 | `p04_jump_heavy` | 7 | A processar |
| 22 | `p04_jump_heavy_down` | 7 | A processar |
| 23 | `p04_jump_light` | 7 | A processar |
| 24 | `p04_land` | 4 | A processar |
| 25 | `p04_light_jab` | 11 | A processar |
| 26 | `p04_medium_kick` | 14 | A processar |
| 27 | `p04_rise` | 6 | A processar |
| 28 | `p04_special_energy` | 11 | A processar |
| 29 | `p04_special_s2` | 20 | A processar |
| 30 | `p04_special_s3` | 21 | A processar |
| 31 | `p04_special_s4` | 20 | A processar |
| 32 | `p04_super_wave` | 22 | A processar |
| 33 | `p04_taunt` | 33 | A processar |
| 34 | `p04_throw` | 9 | A processar |
| 35 | `p04_ultra_beam` | 9 | A processar |
| 36 | `p04_victory` | 33 | A processar |
| 37 | `p04_walk_back` | 27 | A processar |
| 38 | `p04_walk_forward` | 28 | A processar |

## Execução

1. Matriz de **38 jobs de renderização**, com no máximo quatro simultâneos. Modelagem RGB 4x pelo Real-ESRGAN AnimeVideo-v3 oficial, SHA-256 do modelo conferido, alfa original por Lanczos, sem mudar pose, ordem de frames ou temporização. Reescala da arte 4× para textura runtime 2× (fallback seguro 1,5× só quando necessária).
2. Validar geometrias e distribuição das colunas para atlas limitado a 4096×4096 pixels e orçamento de 64 MiB decodificados. Guardar hash da fonte, configuração, modelo, frames master e textura de saída.
3. Após **todos os 38 jobs** aprovados, importar e empacotar **somente** os 38 atlases do P04, conferir alfa de cada frame e regenerar as classes Java de personagens. Validar referências dos personagens e geometrias com o pipeline incremental usado com sucesso em P01/P03, sem executar a suíte que copia toda a pasta em cada teste.
4. Publicar as alterações apenas na **branch de testes**; jamais na `game-luta-sprite-gpt` sem aprovação visual de um novo APK. Sem APK automático.

Arquivos de execução: `tools/sprites/p04_sr_batch.py` e `.github/workflows/p04-all-sr.yml`.
