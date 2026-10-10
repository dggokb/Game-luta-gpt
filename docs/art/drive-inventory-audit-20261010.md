# Auditoria automatica dos 12 lutadores — Drive x sprites

**Branch isolada**: `test/sprite-inventory-drive-20261010` (a branch de producao
`game-luta-sprite-gpt` nao foi alterada). Inventario lido do Google Drive em 10/10/2026.

## Fonte e escopo

- Drive: `game-luta-gpt/animations/p01..p12`; **399 MP4**.
- Snapshot versionado: `tools/sprites/videos/drive_inventory_20261010.json`.
  Inclui IDs, nomes, tamanhos e links de origem; **NAO inclui bytes dos videos**.
- Estados definidos por `tools/sprites/video_names.py`: **35 estados de video**.
  `COMBAT`, `RISE`, `FALL`, `LAND`, `GROUNDED`, `GETUP` e
  `THROW_TOSS` sao derivados da extracao de trechos; o runtime possui 42 estados.
- Correspondencia dos arquivos para p09-p12:
  `tools/sprites/videos/p09.json` ... `p12.json`.
- Comparacao com `characters/pXX/character.json` para verificar:
  atlas proprio, atlas emprestado, atlas reutilizado, estado sem integracao,
  video mapeado que nao existe, nome ambiguo e falta de escala.

## Executar sem videos locais (metadados)

```bash
python3 -m unittest discover -s tools/sprites/tests -p test_drive_inventory.py -v
python3 tools/sprites/audit_drive_inventory.py
```

Saidas: `android/app/build/sprite-audit/audit.json` e `audit.md`.
O GitHub Actions da branch executa o mesmo comando e publica os relatorios
como artifact `sprite-drive-inventory`.

## Verificar conteudo REAL dos MP4 (opcional)

Requer que os MP4 sejam **acessiveis localmente**, mantendo a estrutura
`animations/p01/*.mp4`, `animations/p02/*.mp4` ... `p12/*.mp4`,
alem de `ffmpeg` e `ffprobe` disponiveis no PATH.

```bash
python3 tools/sprites/audit_drive_inventory.py \
  --video-root "/caminho/para/animations" --inspect-media
```

Inspeciona resolucao e duracao e faz amostragem de 8 quadros em tons de cinza
(32x32, a 2 fps) para identificar possivel conteudo duplicado e videos muito
estaticos. Sao **indicadores de revisao**, nao laudos de qualidade nem
reconhecimento de golpes. Videos que faltarem localmente sao listados, sem
serem marcados como avaliados.

**Atenção:** a integracao conectada ao Drive desta conversa disponibilizou
a listagem autenticada das pastas, nao os 399 MP4 montados no runner do GitHub.
Logo, a auditoria CI nao afirma que 399 videos foram visualmente analisados.
Um futuro conector de transferencia autenticada ou execucao junto a copia
local do Drive pode ativar a inspecao por quadros.

## Divergencias confirmadas na revisao do codigo

- **p03:** `JUMP_MEDIUM` usa atlas `player_base_jump_medium`;
  `THROW_GRAB/THROW_TOSS` usam `player_base_throw`.
- **p04:** `JUMP_MEDIUM` referencia `p04_jump_heavy`.
  Apesar de `socoPulo.mp4` existir, a documentacao diz que e igual a
  `puloSocoForte.mp4` (confirmar frames).
- **p08:** `SPECIAL_S2` foi associado a `H.mp4` no mapeamento antigo;
  nao ha `s2.mp4` na pasta.
- **p02, p05, p06, p07, p08:** `JUMP_HEAVY_DOWN` reutiliza `JUMP_HEAVY`.
- **p09-p12:** videos estao no Drive, mas faltam os pacotes e a calibracao
  `art/keys/pNN/tamanho.json` na branch; nao dizer que faltam todos os videos.
- **p12:** nao foram encontrados `dano_cima` e `jH_baixo`.
- **p01:** existem MP4 `dreamina-...` e `gemini_generated_video_...` com
  nomes nao classificaveis por acao. `upper.mp4` foi mapeado para `2H`
  por alias, mas deve ser confirmado visualmente.

## Politica segura de nomenclatura

Nao renomear arquivos no Drive nem substituir vídeos provisórios sem antes
identificar e validar os correspondentes. O importador detecta variacoes
de maiusculas, acentos, espacos e pontuacao e preserva os nomes originais.
A lista canônica esta em `video_names.CANONICAL`.

Os mapeamentos p09-p12 sao **preparacao da importacao**, nao integracao
automatica de sprites. Fazer a extracao a partir dos videos, calibrar cada
personagem, rodar `build_characters.py --check` / `--write` e revisar
na versao de teste antes de inserir no roster de producao.
