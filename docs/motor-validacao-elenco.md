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

## Evidência verificada nos arquivos originais do P01 (2026-10-10)

Os MP4 reais `animations/p01/idle.mp4` e `animations/p01/vitoria.mp4`
foram lidos diretamente da pasta do projeto no Drive. Em amostras com
fundo verde segmentado, na mesma resolução de inspeção (640 × 360):

- região superior/cabeça (*proxy*): idle ≈ **103 px**, vitória ≈ **54 px**;
- região do tronco (*proxy*): idle ≈ **105 px**, vitória ≈ **57 px**.

Ambas diferem na mesma direção (~0,52–0,54×), o que **sinaliza**
variação significativa de escala já na fonte. Não é medição anatômica
definitiva: vídeos têm poses e enquadramentos diferentes. É exatamente
o caso para sinalizar `SUSPEITA_DE_ESCALA` e pedir calibração
pela imagem-base, sem aumentar/reduzir todo o sprite automaticamente.

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

## v3 — confirmação de padronização por provas independentes

A partir desta revisão, o comando usa `verificar_escala.py` além dos
proxies de silhueta de `estabilizador.py`. Um resultado de bbox nunca mais
pode, sozinho, declarar uma animação com escala padronizada.

### Etapas de prova

1. **Imagem-base verdadeira:** a ferramenta lê `inicio_centro.png`, extrai
   a silhueta real e confere se sua altura multiplicada pela escala
   `tamanho.json/chaves/inicio_centro` coincide com `tamanho.json/altura`
   em até **7,5%**. JSON sozinho não constitui confirmação.
2. **Triagem temporal:** o vídeo chroma-key e o atlas são comparados com
   o próprio `IDLE`. Cabeça/tronco, drift lento do pé plantado e
   oscilações que revertem em um quadro são medidas independentemente.
   Sem vídeo legível => **INCONCLUSIVO**.
3. **Correspondências geométricas SIFT/RANSAC:** exige ao menos
   **22 pontos consistentes**, mínimo **43% de correspondências corretas**
   após RANSAC, cobertura corporal horizontal 15% e vertical 16%,
   erro mediano <= 3,8 px, ângulo <= 28 graus e fator reversível
   (`zoom_ida * zoom_volta` com diferença <= 3,5%).
   Sem tais evidências => **INCONCLUSIVO**; não supor que braço erguido
   ou agachamento alterou tamanho.
4. **Sequência inteira:** ao menos **três quadros** válidos do clipe.
   Divergência de escala superior a **7%** ao longo do clipe é
   `INSTAVEL_GEOMETRICAMENTE`, não aprovado.
5. **Vídeo versus atlas:** a proporção do estado em relação ao idle
   precisa concordar entre vídeo original e atlas em até **6%**.
   Se divergir, emitir `ESCALA_DIVERGENTE`, não corrigir por tentativa.
6. **Jogo real:** continua **obrigatório** verificar escala final
   aplicada pelo renderizador, câmera, espelhamento e transições.
   A prova de pixels brutos do atlas não valida automaticamente
   a escala renderizada e nunca muda `validacao_runtime: PENDENTE`.

### Testes independentes e evidência negativa

`test_verificar_escala.py` utiliza os pixels **reais** do atlas idle do
P01, faz redução conhecida de 18%, ampliação conhecida de 18%,
translação sem redimensionamento, zoom variável, arte incompatível e
imagens sem informações geométricas; verifica que apenas medidas de
escala reversíveis são aceitas. `test_refinar_elenco.py` verifica que
jitter isolado, drift lento do pé em vitória, movimento contínuo real,
vídeos sem fonte, aliases ambíguos e imagem-base falsa são tratados
sem mascarar reprovação.

Na amostra real de `idle.mp4` e `vitoria.mp4` do P01, o casamento
de detalhes do corpo apresentou poucas correspondências em vários
quadros devido à **mudança de pose**. Essa situação deve ser
**INCONCLUSIVA**, mesmo quando a medida aproximada da silhueta sugere
escala menor. Não produzir fatores de redimensionamento por essa
métrica fraca.

### Limitações que bloqueiam declaração de motor finalizado

- NÃO existe confirmação por pontos anatômicos sem ambiguidade em
  poses extremas (cabeça escondida, giro, crouch, personagens não humanos).
- NÃO existe medição automatizada do último pixel efetivamente
  renderizado no Android, incluindo `P01SpriteCalibration`,
  `P01VisualTuning`, `pixelScale`, espelhamento e câmera.
- NÃO existe executor de correção reversível com teste antes/depois
  em arquivo temporário. Até existir, o motor é um **diagnosticador e
  validador conservador**, não um estabilizador autônomo completo.
- O GitHub Actions não acessa os vídeos do Drive automaticamente;
  a pasta local deve ser disponibilizada ao comando. Sem ela o CI
  marca pendência, não aprovação.

### Segurança de execução

```bash
python3 tools/sprites/refinar_elenco.py p01 \
  --videos-dir /pasta/animations/p01 \
  --estado DASH --saida /tmp/p01-dash.json

# Diagnóstico rápido — nunca equivale a aprovação de escala
python3 tools/sprites/refinar_elenco.py p01 \
  --videos-dir /pasta/animations/p01 --sem-geometria
```

Nem o comando nem os testes alteram PNGs, vídeos, manifestos, colisão
ou gameplay. O mecanismo trabalha sobre **um personagem**, não o elenco.

## v4 — nova auditoria de falsos positivos (2026-10-10)

Esta etapa corrigiu defeitos que **permitiam aprovar escalas erradas**:

1. **Ambos menores não é tamanho correto.** Anteriormente vídeo=0,82 e
   atlas=0,84 contra o IDLE eram considerados "padronizados", pois
   concordavam entre si. A prova agora exige **cada um** dentro de
   **±8% do corpo do próprio IDLE** além de concordância fonte↔atlas
   em até 6%. Caso ambos estejam fora, produz
   `ESCALA_FORA_DO_IDLE`. Isso é uma prova conservadora para
   poses com correspondência geométrica válida; quando a pose
   atrapalha a medição, o resultado deve continuar INCONCLUSIVO.
2. **Poucos quadros fáceis não validam clipe longo.** A prova de escala
   exige agora **pelo menos 70% das amostras válidas** (mínimo 3),
   incluindo ao menos uma em cada terço temporal. Exemplo: 3 quadros
   compatíveis e 4 ilegíveis não passam.
3. **Vídeo sem chroma-key não entra na comparação.** A extração
   amostral verifica os quatro cantos do quadro. Se qualquer
   amostra não tiver o fundo verde esperado, a medição retorna
   INCONCLUSIVA. Não escolher apenas quadros fáceis e ignorar
   os que falharam ao decodificar.
4. **A auditoria de silhueta também respeita o pixelScale.** O
   leitor de atlas antigo usava pixels brutos ao medir tremor e
   tamanho, enquanto a prova geométrica convertia escala. Agora
   ambos usam a escala física do IDLE. Frames negativos ou fora
   dos limites são rejeitados explicitamente.
5. **Testes adversariais:** atlas 1×/2× da mesma figura devem
   produzir medidas iguais; um frame inválido não pode voltar
   ao último frame; vídeo verde sintético é aceito, vídeo com
   fundo vermelho não pode ser segmentado como se fosse verde;
   corpo +18%/−18% e translação sem zoom são casos distintos;
   concordância de duas fontes igualmente pequenas deve falhar.

**Atenção:** estes gates comprovam apenas a consistência de tamanho
nos trechos com detalhes comparáveis. **Não** comprovam anatomia em
todas as poses, qualidade dos frames nem resultado final após câmera,
`P01SpriteCalibration` e `P01VisualTuning`. O motor permanece
read-only e `validacao_runtime=PENDENTE` até termos captura real
de transições e execução dentro do jogo.
