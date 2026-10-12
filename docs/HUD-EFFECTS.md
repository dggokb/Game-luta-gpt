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

## Onda na troca (atualização)

Uma troca real entre os mesmos dois membros emite uma onda azul de 650 ms pelos cinco segmentos, da extremidade interna em direção ao retrato do novo titular. O brilho na borda do retrato começa em 55% do tempo. Evento detectado pela inversão das identidades de titular/reserva, com adversário e época de apresentação iguais. Troca de roster, seleção, restauração e primeiro desenho não disparam. Faixa de vida não sofre falso impacto. Paint e Path reutilizados; preenchimento ASSIST e medidor SUPER permanecem reais.

## Correção de leitura e visibilidade — v0.102

A HUD agora desenha explicitamente os rótulos reais de ASSIST e TROCA. ASSIST mostra entrada/em campo, segundos com uma casa decimal durante recarga e PRONTO; TROCA mostra andamento, cooldown real de raw TAG (até 10 s neste motor) e PRONTA (↓+TAG). São contadores separados: raw TAG em cooldown não esvazia ASSIST pronto. Strings armazenadas pelo GameView, sem formatação por frame.

SUPER: cinco células reais, cada uma representando METER_PER_BAR, total MAX_METER. Removidas as quatro divisões internas antigas por composição Canvas; moldura externa e retratos preservados. Pulsos e descargas recortados em cada célula, sem pintar os intervalos entre elas. Teste nativo verifica uma célula cheia com um nível e cinco com máximo, inclusive no CPU espelhado.

Troca: onda ampliada para 1,1 s, núcleo branco de 17 px e cauda azul de 98 px, borda do retrato de 5 px. ASSIST pronto: flash único de 900 ms nos cinco segmentos e indicador iluminado persistentemente enquanto a disponibilidade real permanece cheia.

HudRuntimeTest roda o APK instalado no Android, deixa a seleção pelo toque, usa ASSIST, aguarda o cooldown real, usa ↓+TAG com multitouch, verifica titular trocado, evento observado pelo renderer e contador de raw TAG. Não escreve vida, meter ou cooldowns. A gravação de tela do emulador complementa a prévia técnica nativa; ambas devem ser revisadas antes da entrega.
