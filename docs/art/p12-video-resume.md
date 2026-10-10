# P12 — checkpoint dos vídeos reais

Status: **PARCIAL; NÃO APROVADO COMO PERSONAGEM FINAL**.

A branch de teste recebeu 12 estados com 87 frames reais dos vídeos da pasta
animations/p12 do Drive no commit a65b0cdf60e554f96c5437a5e1826fd834a18f70.
Estados integrados: IDLE, COMBAT, WALK_FORWARD, WALK_BACK, LIGHT_JAB, CROUCH,
RISE, JUMP, FALL, LAND, DEFENSE_CROUCH e DEFENSE_AIR.

Os outros 30 estados ainda apontam para os antigos recursos rejeitados no
runtime. As prévias dos 42 estados são candidatas e não comprovam integração
nem aprovação dos 42. Nenhum APK novo foi produzido; não houve teste em emulador
neste ciclo. Nenhuma branch principal recebeu merge.

## Evidências já verificadas

- 87 frames: origem do vídeo, hashes dos PNGs, reprodução nativa, alpha após SR
  e contrato imutável de combate verificados.
- Modelo AnimeVideo-v3 verificado; processamento incremental: 38 hits / 49 misses.
- CharacterPack --write e --check passaram: 11 personagens, 352 atlases.
- 30 testes focados passaram no commit de integração.
- Auditoria global: 69 apontamentos, 7 críticos; nenhum crítico nos 12 novos
  estados, mas 10 avisos continuam nesse lote.
- Harmonia global: 21 problemas; nenhum nos 12 novos estados.
- O Actions 38083873560 passou pelos testes e rejeitou os 30 estados restantes
  no gate obrigatório. Etapas Android/APK não executadas.

## Checkpoint do pipeline

O resolvedor de segmentos permite preparação/recuperação retiradas de OUTRO
vídeo original existente. Não interpola, desenha ou deforma membros. Cada vídeo
continua cronológico, sem repetir índices, com PTS original finito. O impacto
permanece no vídeo próprio da ação, com pelo menos dois frames desse vídeo.
O renderer ainda exige revisão explícita, hashes de todas as origens, máscaras
somente subtrativas, escala anatômica, apoio e transições revisadas. Gates e
limites de qualidade permanecem ativos.

As seleções alternativas estão em p12-video-bridge-candidates.json como
rascunho separado. Elas NÃO alteram o plano ativo, os assets de runtime ou
as aprovações. Precisam de inspeção, calibração, máscaras, SR, integração e
auditoria. Elas não resolvem automaticamente os 30 estados pendentes.

## Retomada necessária

O ambiente local desconectou com erro 409 environment_offline durante a
correção da extração. Os MP4s e caches estavam em:
- ../p12-drive-videos
- android/app/build/p12-video-extraction
- android/app/build/p12-sr-cache

Se o ambiente for restaurado, preservar esses caches. Se estiverem ausentes,
recuperar os 33 MP4s do Drive pelos IDs de p12-drive-inventory.json, conferir
SHA dos vídeos e reextrair. Nunca inventar hashes ou aprovações.

1. Corrigir o recorte de fundo de derruba_levanta.mp4, frame 59. O MP4 original
   contém a cabeça intacta; o keyer a descartou ao separar uma região ligada
   por pescoço esverdeado. A correção AINDA NÃO foi implementada. ROI original
   [272,285,375,401] no frame RGB 1280x720. Qualquer restauração deve copiar
   somente RGB do próprio frame, ligada ao SHA do MP4 e ao SHA dos pixels
   RGB decodificados. Não preencher membros nem pintar novas cores. Conferir
   que os demais frames e os outros 32 vídeos não mudaram e atualizar a
   proveniência durable antes de consumir o frame corrigido.
2. Revisar as seleções alternativas e impedir falsos matches de cabeça:
   defendeCima 34..46 e dash 28..55 têm o template confundido por antebraços/
   rotação. Medir cabeça verdadeira e vincular evidências aos hashes.
3. Em ataques aéreos, registrar a pelve por landmark do cinto vermelho;
   o centro da silhueta se desloca com um chute e não é pivô corporal confiável.
4. Resolver os críticos de cadência ainda encontrados em DASH, HIT_AIR,
   JUMP_LIGHT, JUMP_HEAVY_DOWN, SPECIAL_ENERGY, SUPER_WAVE e ULTRA_BEAM.
   Não reduzir limites para fazê-los passar.
5. HIT_STAND não tem recuperação limpa no trecho revisado. HIT_CROUCH retorna
   a uma altura incompatível com CROUCH. CROUCH_HEAVY mostra um rugido, sem
   golpe claro. CROUCH_MEDIUM tem varredura/efeitos e direção a conferir.
   GRAB/TOSS incluem outro personagem parcialmente ocluindo o zumbi:
   remoção do oponente não autoriza reconstruir pixels escondidos.
6. Revisar semanticamente ataques cujos nomes legados não descrevem o vídeo:
   M é uma investida corporal; H é uma investida de cabeça/tronco; não alegar
   que esses trechos comprovam um chute ou soco reto. Preservar frame data.
7. Só registrar outros estados como revisados após evidência suficiente.
   Executar prepare/verify do lote, CharacterPack, auditorias e transições;
   depois executar gates completos, testes Android, APK e emulador.

Fontes: https://drive.google.com/drive/folders/1gk8gEgCZaAm1AXkwHheqTfuUpfY-lFi-
Prévia: docs/art/p12-video-normalized-review/
Relatório: docs/art/p12-video-batch-report.json
CI: https://github.com/dggokb/Game-luta-gpt/actions/runs/38083873560
