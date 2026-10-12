# Efeitos de apresentação da HUD

Base: ui/hud-barras-referencia-20261012, HEAD 509ab3dd469d4f35968bbedda73b517f058581a8.
Branch: ui/hud-efeitos-barras-20261012. Sem merge com branches de integração.

HudEffects lê os estados reais e HudRenderer desenha com Canvas. Nenhum efeito escreve no combate.
Relógio monotônico em segundos; drawHudAt injeta o mesmo relógio para revisão determinística.

- Dano: borda por 160 ms; cauda âmbar na faixa inferior (35% da altura), parada por 180 ms e seguindo linearmente durante 550 ms. Recuperável continua vermelho e ocupa a área superior. Impactos consecutivos preservam a posição da cauda antes de mudar o destino. Zero dano não dispara.
- Recuperação: região aumentada verde-clara, 380 ms. Vida <=25% e >0: borda vermelha suave, oscilação de 4 rad/s.
- ASSIST: cinco segmentos continuam seguindo tagReadyRatio real. Frente de recarga com brilho de 7 px, pulso pronto de 480 ms somente na transição <1 para 1; descarga 300 ms na entrada no estado assist/tag em uso. CPU sem parceiro jogável não recebe eventos ASSIST.
- SUPER: níveis reais de 1000 unidades (METER_PER_BAR); ganho 420 ms na faixa correspondente ao nível, gasto 300 ms somente na região consumida. Máximo usa CombatConfig.MAX_METER, independentemente das quatro divisões ornamentais. Fluxo azul P1/vermelho CPU no máximo; texto desenhado depois dos efeitos.
- Molduras: seis faíscas fixas por região, sem lista dinâmica, trajetórias determinísticas nas bordas. Paint/Path/gradientes reaproveitados; efeitos não decodificam assets ou criam objetos por frame. Limites nas áreas das barras, sem partículas na arena.
- Baseline: primeiro desenho, alteração de identidade dos lutadores, abertura/retorno da seleção, restauração pelos botões VIDA. resetEffects cancela eventos sem alterar o motor. Troca real de titular também muda identidade e não simula golpe.
- Arte, concepts, controles, seleção, sprites e cenários preservados.

## Validação

HudEffectsTest: cauda contínua, dano zero, recuperação, níveis/gasto, mudança de identidade, pulso pronto único, reset, independência ASSIST/SUPER, comparação 30/120 Hz.
HudArtTest: portraits concepts, atlas original e independência dos medidores.
Demais testes: sistemas extraídos, seleção, restauração de vida, ultra-core e stage-core.
Prévia técnica: 240 quadros a 30 fps desenhados por HudRenderer.drawHudAt em Canvas nativo Robolectric, com GameView e assets reais, estados controlados. Não é gravação de uma partida. Vídeo deve ser revisado antes da entrega; compilação isolada não valida aparência.
