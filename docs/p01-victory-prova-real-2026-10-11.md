# P01 / VICTORY — prova real de estabilização (2026-10-11)

**Estado do experimento:** PARCIAL. Correção automática aplicada ao jogo na branch experimental, aprovada em comparação independente dos pixels do atlas real após transformações equivalentes às do renderizador. **Captura do Android real e prova final de runtime ainda pendentes.** Não usar sucesso de CI como aprovação visual do Android.

## Controle da execução

- Branch exclusiva: `refino/p01-animacoes-20261009`, HEAD inicial `efa16409e0219a33245403aa79f4308a7cf6240e`; sem merge.
- Google Drive `animations/p01/vitoria.mp4` (arquivo original, não arte sintética); também `idle.mp4` e `inicio_centro.png`.
- P01 usa `player_base_victory.png`, `player_base_idle.png`, `tools/sprites/reports/player_base_*.report.json`, `characters/player_base/character.json`.
- Vitória: 33 frames no atlas (326 × 577 por quadro, 11 colunas, pixelScale 2, root 171/556). Android: 125 ms por frame, 33 frames, sem loop. A transição da vitória em `GameView.java` começa no frame **7**; os frames 0–6 de aproximação existentes no vídeo não são reproduzidos na vitória de combate.
- Vídeo original `vitoria.mp4`: 97 imagens 1280 × 720 ao longo de 4,017 s (stream nominal 60 FPS, média 24,15 FPS). 33 amostras em intervalos de 3 imagens correspondem à sequência do atlas. `idle.mp4` é referência independente do corpo.

## Evidência independente da fonte original

A extração do original, em pixels do próprio MP4, identificou movimento de entrada intencional **+93 px entre amostras 0–6**, seguido do pé plantado na amostra 7 (X=624 px), que varia **até 1 px** nas amostras 7–32. O tronco se movimenta naturalmente. Não confundir deslocamento inicial intencional com deslize na comemoração.

## Causa observada e confirmação no atlas empacotado

Amostras do **PNG realmente integrado** ao Android, medidas com ancoragem de pixels inferior do calçado, têm deslocamento líquido no estado plantado:

| Estado renderizado por cálculos equivalentes ao renderer | Amplitude X dos pés (frames 7–32) | Delta X 7→32 | Altura medida |
|---|---:|---:|---:|
| Atlas sem calibração | 4,5 px | -4,5 px | 295–301 px |
| Após `generate_p01_calibration.py` (situação anterior no jogo) | **3,0 px** | **+3,0 px** | 259–264 px |
| Após `SpriteAutoFix` executado pelo motor | **1,0 px** | **0,0 px** | 259–264 px |

A calibração P01 já tentava compensar a vitória pela mediana de uma **faixa inferior de 56 pixels** do atlas; isso não equivalia exatamente à trajetória da sola/contato dos pés no resultado final, deixando um deslize residual de 3 px. O vídeo original não tem tendência correspondente, e o teste após calibração demonstra que as transformações de raiz/offset e o alinhamento do atlas são componentes do defeito. **Não afirmar que um único algoritmo de extração introduziu todo o defeito**, pois o próprio atlas sem calibração já tinha deslocamento de -4,5 px.

A `CameraRig.java` está configurada para congelar X/top/zoom após KO (e `GameView.java` chama `updateForRound(..., roundFinished)`), mas sem captura Android não é possível excluir outras transformações/transições ou ajustes persistidos de `P01VisualTuning` no dispositivo do usuário.

## Execução efetiva do motor

Foi montado ambiente com vídeos reais extraídos do Drive e artefatos PNG/reports exportados do commit correto por GitHub Actions. **Sem redimensionar PNGs**, executou-se:

```bash
python tools/sprites/autoestabilizar.py p01 --estado VICTORY \
  --videos-dir animations/p01 --saida /tmp/p01-diagnostico.json
python tools/sprites/autoestabilizar.py p01 --estado VICTORY \
  --videos-dir animations/p01 --corrigir --saida /tmp/p01-publicacao.json
python tools/sprites/validar_p01_victory_real.py
```

O motor liberou o reparo por comparação entre vídeo original e atlas; sua própria confirmação de pixels indicou amplitude de 6,0 → 1,0 pixel em sua janela geométrica. **Uma segunda medição independente, sobre os pixels efetivamente transformados com faixa inferior do calçado, demonstrou 3,0 → 1,0 px e delta líquido +3,0 → 0,0 px.** Essa última é a métrica conservadora reportada acima, e não a previsão algébrica produzida pelo motor.

Arquivo realmente atualizado na branch: `android/app/src/main/assets/sprite-autofix.json`. São **22 quadros com dx diferente de zero**: 7 e 12–32. Quadros 0–6 têm dx/dy 0 e escala 1.0; quadros 8–11 igualmente sem transformação. Deslocamentos adicionados em unidades do atlas: frame 7 `+0,88`; 12–17 `-1,5144`; 18–23 `-3,0288`; 24–29 `-4,5433`; 30–32 `-6,0577`. Sem modificação de altura, sprites, frame order, duração ou hitboxes. É uma transformação não destrutiva com rollback via `autoestabilizar.py p01 --reverter` ou reversão do commit do JSON.

## Prova visual reexecutável

- `tools/sprites/validar_p01_victory_real.py`: mede diretamente os pixels de um atlas P01 de produção, aplicando escala e offsets em ordem equivalente ao `SpriteFighterRenderer.java`; emite `atlas_real_medicao.json` e `atlas_real_comparativo.png`, disponíveis no artefato `p01-victory-real-atlas-evidence`.
- `.github/workflows/p01-victory-evidence.yml`: executa esse ensaio sobre o checkout da branch **e falha** se não detectar melhora de ao menos 55%, se a amplitude final ultrapassar 1,25 px, se alterar a altura, se corrigir outros atlas/personagens ou se tocar nos sete frames de entrada. *Esse CI não tem acesso aos originais Drive nem emula captura do Android.* 
- A execução local com vídeos Drive também gerou MP4 comparativo lado a lado da sequência completa (0–32, incluindo caminhada) e da sequência efetiva de jogo (7–32, duração 125 ms/frame), CSV por frame e gráfico, sem criar imagens novas do personagem.
- Fonte, render de PNG e saída do corretor são evidências separadas. **O workflow verde comprova somente seu escopo de pixels, não a estabilidade observada no dispositivo.**

## Pendências obrigatórias para APROVADO

1. Captura instrumentada do renderizador Android (ou teste de integração de Canvas com câmera e bitmap reais, além da confirmação no dispositivo), incluindo KO → VICTORY → pose final, nas duas orientações/espelhamentos relevantes.
2. Reexecutar o monitoramento dos pés na captura real em coordenadas de tela; confrontar com camera.x/zoom, root, pixelScale e `P01VisualTuning` persistido.
3. Validar por inspeção humana a animação completa, transições e ausência de distorção anatômica em escala de aparelho.
4. Rodar/registar testes de regressão Android com assets publicados (sem confundir teste unitário com prova visual).
5. **Não** testar P01 2H nem outros personagens antes da aprovação visual completa da VICTORY.

**Portabilidade:** o motor é genérico em interface, mas este reparo concreto depende de prova geométrica do personagem e movimento plantado; não há prova suficiente de funcionamento em personagens/poses diferentes. A única validação honesta é **PARCIAL**.
