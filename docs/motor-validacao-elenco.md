# Corretor / estabilizador de sprites — contrato técnico v2

## Contrato de operação

**Somente um personagem por execução.** O usuário indica `p01` até `p12`.
O P01 usa internamente o pack `player_base`. O motor NÃO possui opção
`--todos`, NÃO percorre outros packs e NUNCA faz merge.

Fontes prioritárias (não intercambiáveis):
1. **Imagem-base e `idle.mp4`** do personagem — tamanho corporal de referência;
2. **Vídeo original do movimento** em Google Drive/`animations/<pXX>` — ordem,
   intenção, ritmo, poses e mudanças reais;
3. **Sprites empacotados** de `characters/<pack>` e
   `android/app/src/main/res/drawable-nodpi` — o que é efetivamente desenhado
   no Android; animações na pasta Drive sem versão empacotada ficam PENDENTES.
4. **Runtime Android** — câmera, transições, física e pixels finais da tela.

As fontes 1 e 2 devem ser disponibilizadas localmente para análise dos vídeos.
A ferramenta **não possui credenciais de Drive no CI**. O nome ou a listagem
no Drive não é a mesma coisa que ler os quadros do vídeo. Caso a fonte não esteja
presente, o relatório deve registrar `PENDENTE_FONTES`; jamais mostrar sucesso.

## Como executar

```bash
pip install -r tools/sprites/requirements-animar.txt

python3 tools/sprites/refinar_elenco.py p01 \
  --videos-dir /pasta/local/animations/p01 \
  --saida android/app/build/sprite-review/p01.json

# Examinar apenas a vitória e NÃO os outros estados/personagens:
python3 tools/sprites/refinar_elenco.py p01 \
  --videos-dir /pasta/local/animations/p01 --estado VICTORY

# Analisar um personagem novo mesmo sem assets importados:
python3 tools/sprites/refinar_elenco.py p12 \
  --videos-dir /pasta/local/animations/p12 --sem-sprites
```

São reconhecidos aliases do Drive (como `m2(rasteira).mp4`,
`2M.mp4`, `pulo.mp4`, `jump.mp4`, `back_dash.mp4`,
`backDash.mp4`, `vitoria.mp4`), com correspondência exata e
ambiguidade sinalizada em vez de escolher aleatoriamente.

## Definição da escala

NÃO igualar a bbox externa de cada pose à bbox externa do idle.
A silhueta muda por pose, inclinação, joelho, braço e efeitos.
Medir proxies de cabeça e tronco nos dois vídeos e no sprite
para comparar diferenças **relativas ao idle do mesmo lutador**.
Somente quando essas duas referências concordam a anomalia
é sinalizada como `SUSPEITA_DE_ESCALA`. Desacordo entre cabeça
e tronco é `INCONCLUSIVO`. Isso NÃO fornece um fator de correção
aplicável sem revisão da imagem-base.

A calibração autoral em `art/keys/pXX/tamanho.json` define a
altura do personagem e as escalas de imagens iniciais. Ela é
obrigatória para uma análise completa. O valor está disponível
no relatório, mas o algoritmo atual ainda NÃO comprova
automaticamente alinhamento anatômico pelo rosto e tronco.

## Detecção de tremor

Medir centros de tronco, larguras aproximadas de cabeça/tronco,
e posição de pé durante segmentos com apoio.
Usar mediana temporal de cinco quadros e detectar
**oscilações rápidas que revertem no quadro seguinte**.
Movimento contínuo, deslocamento intencional e troca de
perna de apoio NÃO são estabilizados à força. Para o IDLE e
a VITÓRIA, tremor isolado dos pés é reportado separadamente.
Limite inicial: maior entre 1,5 pixel e 1,5% da altura
do idle para quadros na resolução analisada.
Não é limiar universal, e detectar tremor NÃO significa
conhecer sua causa.

**Limitações importantes:** a extração do vídeo exige
fundo verde (chroma-key) confiável; vídeo com outro cenário
fica inconclusivo. A métrica atual usa proxies da silhueta,
não pose estimation anatômica robusta nem compensação
óptica de câmera em cenário arbitrário. Os vídeos comparados
aos sprites usam medidas relativas ao respectivo idle;
não comparar pixels absolutos de resoluções diferentes.

## Status e segurança

- `PENDENTE_FONTES`: vídeo original, atlas, frame, calibração ou confiança
  insuficiente; bloqueia aprovação;
- `ANOMALIAS_IDENTIFICADAS`: há tremor ou divergência mensurável;
- `MEDICOES_CONCLUIDAS`: medidas preliminares disponíveis, mas **NÃO** aprovado;
- `aprovado_automaticamente: false`, `validacao_runtime: PENDENTE`
  até houver teste independente do resultado na execução real.

**Nenhuma arte, PNG, vídeo, duração, hitbox ou script de gameplay é alterado**
pelo diagnóstico. `ancorar.py`, `reescalar.py`, `tamanho.py` e
`auditoria.py` são motores legados auxiliares; alterações
automáticas só serão habilitadas com plano/diff reversível,
simulações antes/depois e provas independentes de melhora.

## Checklist para implementar depois desta etapa

- [x] Invocação por personagem (sem `--todos`).
- [x] Mapeamento seguro de nomes reais de vídeos.
- [x] Contrato de referência IDLE + calibração oficial.
- [x] Leitura read-only de quadros de vídeo chroma-key e atlas do personagem.
- [x] Métricas temporais com regressões de jitter vs movimento contínuo.
- [x] Reprovação de fontes ausentes, ambíguas ou resultados inconclusivos.
- [ ] Calibração anatômica mais forte que proxies de silhueta
      (cabeça/ombros/tronco em mudanças extremas de pose).
- [ ] Medição de vídeo com cenários não verdes via compensação de câmera.
- [ ] Reprodução independente do render final, câmera e transições.
- [ ] Correções automáticas com diff, rollback, e comparação de métricas
      antes/depois. **Não executar refino automático nesta etapa.**
