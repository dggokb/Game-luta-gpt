# P01 — Shinkai HD (personagem + efeito)

**Branch de teste:** `test/p01-shinkai-hd-20261103`, derivada da `game-luta-sprite-gpt` em `a7531a66715a1e0a34486bca429341d76a1861bf`.

## Inventário verificado

O Shinkai do P01 utiliza **dois recursos diferentes**:

- `player_base_shinkai`: **26 quadros** da animação corporal, já em resolução física 2× por conversão de vídeo (não pelo modelo Real-ESRGAN). Origem 1× em `art/sprites/source/player_base_shinkai_video_normalized.png`. O PNG existente 2× é conservado como fonte de verdade da transparência.
- `shinkai_player_base`: **35 quadros** de efeito, com folha WebP separada em `android/app/src/main/assets/efeitos/shinkai_player_base/`, originalmente com `pixelScale=0.75`.

## Melhoria e segurança

1. Personagem: aplicar Real-ESRGAN AnimeVideo-v3 aos 26 recortes 1× (RGB 4×, reduzido para 2×); **preservar byte a byte o canal alfa do sprite 2× já aprovado**, sem mover frames, raiz, poses ou mudar a escala no jogo.
2. Efeito: melhorar individualmente os 35 recortes com o mesmo modelo; produzir uma nova textura 1,5× maior, com `pixelScale` de `0.75` para `1.125`. Todas as coordenadas da folha e os deslocamentos do efeito são escalados juntos; assim, seu tamanho e posição no mundo permanecem iguais.
3. **Preservar integralmente as 35 durações** e `marcaMs`. Validar todos os canais alfa, 26/35 contagens de quadros e limites de textura.
4. Empacotar a animação com o **formato WebP atual** do jogo (recortes por quadro). Regerar metadados Java, sem processar/alterar atlases de outros personagens.
5. Publicar tudo **somente na branch de teste**, com hashes/proveniência e validações. A `game-luta-sprite-gpt` permanece intacta até aprovação visual.

**Código:** `tools/sprites/p01_shinkai_hd.py`. **Workflow:** `.github/workflows/p01-shinkai-hd.yml`.

Este processo melhora a qualidade de renderização; não altera mecânicas do Shinkai, reserva de vida, Shinkai Clash ou balanceamento.
