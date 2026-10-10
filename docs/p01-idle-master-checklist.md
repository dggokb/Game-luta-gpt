# P01 — checklist de normalização pelo IDLE

**Branch:** `refino/p01-animacoes-20261009`  
**Regra de origem:** o frame 0 do atlas `player_base_idle` determina escala corporal,
posição de referência dos pés e âncora visual. **Não** alterar hitboxes para compensar arte.

## Regra técnica verificável

- [x] Medir os sprites reais da versão atual: idle original ocupa aproximadamente **302 × 448 pixels** (bbox alpha).
- [x] O idle desenhado usa **0,980×**: tamanho-base 295,96 × 439,04 pixels em coordenadas da arte.
- [x] Produzir calibragem independente **por frame**, cobrindo todos os 39 atlas; crouch/rise mantêm curvas especializadas já testadas, agora verificadas junto ao idle.
- [x] Normalizar altura de poses em pé sem **aumentar** poses naturalmente agachadas/deitadas.
- [x] Limitar largura de DASH ao máximo de **1,20×** o idle; a perna aberta de um golpe não altera a hitbox.
- [x] Fixar todos os frames de VICTORY e INTRO à âncora horizontal usando a silhueta central do frame.
- [x] Compensar posição de pés em estados com solo; em pulo, hit aéreo e backdash manter elevação física/animada.
- [x] Aplicar a normalização na **renderização**, sem regravar PNG, sem perda de qualidade por redimensionamento repetido.
- [x] Automatizar cálculos com `tools/sprites/generate_p01_calibration.py` antes do build Gradle.
- [x] Verificar atlas inteiros com `tools/sprites/qa_p01_idle_master.py` no CI.

## Estados a revisar e testar

**Base / movimentação**
- [x] IDLE / COMBAT — referência e continuidade.
- [x] INTRO → IDLE — normalização quadro a quadro e crossfade preservado.
- [x] WALK_FORWARD e WALK_BACK — escala geral, âncora e pés.
- [x] DASH — largura controlada para evitar sensação de personagem gigante.
- [x] BACKDASH — altura coerente, conservando arco visual do salto para trás.
- [x] CROUCH e RISE — curvas já calibradas e mantidas.
- [x] JUMP, FALL, LAND — sem aumento visual independente da física do pulo.

**Ataques**
- [x] CROUCH_LIGHT (2L), CROUCH_MEDIUM (2M), CROUCH_HEAVY (2H).
- [x] LIGHT_JAB (L), MEDIUM_KICK (M), HEAVY_STRAIGHT (H).
- [x] Ataques aéreos JUMP_LIGHT, JUMP_MEDIUM, JUMP_HEAVY e JUMP_HEAVY_DOWN.
- [x] Especial, super e ultra: preservar área do efeito e normalizar com limite por pose.
- [x] Throw: THROW_GRAB e THROW_TOSS.

**Reações e especiais**
- [x] DEFENSE_STAND, DEFENSE_CROUCH, DEFENSE_AIR.
- [x] HIT_STAND, HIT_CROUCH, HIT_AIR.
- [x] KNOCKDOWN, GROUNDED, GETUP, DEFEAT.
- [x] VICTORY — altura igual ao idle e sem caminhar por offset visual de quadros.
- [x] TAUNT.

## Gameplay — regressões obrigatórias

- [x] Backdash não reinicia enquanto os 15 frames do movimento não terminam.
- [x] Depois dele, exigir 12 frames de recuperação + voltar ao neutro para rearmar.
- [x] Segurar / repetir para trás durante o backdash não gera backdash infinito.
- [x] Novo comando após liberação e recuperação volta a funcionar.
- [x] Não alterar salto normal (1030 u/s), superpulo, física do ar ou knockback nesta etapa.
- [x] Não alterar a janela de impacto, cancelamento, 30 FPS nem sprites do 2M nesta etapa.
- [x] Proteger launcher do 2H e combo L → M → H com testes existentes.

## Controle de qualidade e entrega

- [x] Teste Java de cobertura de todos os atlas: `P01VisualMasterTest`.
- [x] Teste Java de bloqueio e rearme do backdash: `GlobalMovementTuningTest`.
- [x] CI: geometria + QA completo dos 39 atlas aprovados.
- [x] CI: testes de combo, launcher e mobilidade aprovados.
- [x] CI: APK de depuração v0.96 gerado e publicado como artefato.
- [ ] Teste real no celular e conferência de transições (requer feedback após instalação).
- [ ] Enviar a branch estável `game-luta-sprite-gpt` **somente com aprovação explícita**.

### Arquitetura e extensibilidade

Os ajustes atuais são P01-específicos no renderizador, mas a metodologia
(identificar o frame IDLE 0; ler bounds e root; gerar escala/offset por frame;
QA; build) é reutilizável para os próximos personagens. Para um novo lutador,
criar dados de calibração independentes — nunca usar os offsets do P01 em outro
personagem.

## Validação automatizada executada

- [Build Game Luta APK #38069585984](https://github.com/dggokb/Game-luta-gpt/actions/runs/38069585984) — normalização e QA dos 39 atlas, testes P01, compilação do APK concluídos com sucesso.
- APK compilado na branch `refino/p01-animacoes-20261009`, versão `0.96-p01-idle-master`.
- Smoke test Android pode terminar depois do upload do APK; não confundir compilação aprovada com teste manual no dispositivo.

## v0.97 — revisão após teste visual em dispositivo

- [x] Identificada a causa da regressão: padronizar a bbox inteira fez DASH,
  VICTORY e 2H encolherem, embora a pose natural abra um braço/perna.
- [x] Corrigida escala padrão dos três estados, com largura do torso independente
  da altura estendida do golpe.
- [x] Criados controles DEBUG **DASH / 2H / VITORIA**, +/- 5% no Android.
- [x] Percentuais salvos localmente e preservados nos próximos builds.
- [ ] Obter feedback dos três percentuais escolhidos no celular e fixar padrão final.

## v0.98 — dash menor que idle no celular: correção orientada por print

- [x] O dash estava **pequeno**, não grande: comparar massa corporal e anatomia
  em vez de equiparar o retângulo de um personagem agachado ao de um em pé.
- [x] Aumentar **15% nos dois eixos** exclusivamente no atlas `player_base_dash`
  sobre o padrão v0.97 (X: 0,730 → 0,8395; Y: 0,965 → 1,10975).
- [x] Preservar a âncora do solo (`dy` acompanha o novo valor) e o deslocamento
  vindo do motor de combate, sem tocar em imagens, colisões ou duração de quadros.
- [x] Os botões DEBUG +/- permanecem operacionais sobre o novo valor inicial 100%;
  isso funciona mesmo com preferências antigas do v0.97 preservadas.
- [ ] Validar escala no dispositivo comparando DASH × IDLE, com novo screenshot.

## v0.99 — correção restrita ao deslocamento residual de vitória

- [x] Inspecionadas as poses plantadas 7..32 do atlas original de vitória.
- [x] Verificado que centralizar a bbox completa não fixa o personagem, pois
  a largura da silhueta varia quando braços/ombros se movimentam.
- [x] O gerador passa a medir a faixa inferior de **calçados** em cada quadro
  do PNG real e a ancorá-la exatamente à posição do quadro 7, sem afetar escala
  `0.880`, altura, tempo, poses, dash, 2H ou gameplay.
- [x] QA com regressão específica: amplitude horizontal dos calçados da
  vitória nos quadros 7–32 deve ser menor que **0,002 pixel de arte**.
- [ ] Usuário verificar no Android que a vitória não desliza lateralmente.
