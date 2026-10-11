## v6 — execução autônoma POR PERSONAGEM

O corretor deixou de ser apenas diagnosticador. O comando abaixo analisa
**um único personagem** e percorre **todas as animações desse personagem**
presentes no manifesto. Para cada estado ele mede, verifica fontes,
propõe correções seguras e renderiza pixels corrigidos para comparar
o deslizamento antes/depois. **Publica um único lote, com rollback único.**

```bash
# Corrigir automaticamente as animações comprovadamente defeituosas do P01:
python3 tools/sprites/autoestabilizar.py p01 \
  --videos-dir /pasta/animations/p01 --corrigir

# Apenas visualizar o que o motor mudaria, sem escrever nada:
python3 tools/sprites/autoestabilizar.py p01 \
  --videos-dir /pasta/animations/p01

# Corrigir somente 2M, por exemplo:
python3 tools/sprites/autoestabilizar.py p01 \
  --videos-dir /pasta/animations/p01 --estado 2M --corrigir

# Desfazer o último lote publicado, somente do P01:
python3 tools/sprites/autoestabilizar.py p01 --reverter
```

O arquivo publicado em `android/app/src/main/assets/sprite-autofix.json`
é consumido pelo novo `SpriteAutoFix.java` chamado pelo
`SpriteFighterRenderer.java`. A validação inclui testes Python com um
atlas real criado no teste, aplicação e rollback, além de testes
Android de escopo do personagem, limites de escala e rejeição de JSON
inválido.

**Restrições:** só corrige automaticamente tamanho quando os originais
do vídeo dão prova geométrica confiável e o valor final renderizado
está incorreto; só ajusta ancoragem em estados plantados e sem deslocamento
intencional. Sem fonte ou com movimento ambíguo o motor não inventa alteração.

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

## v5 — corretor executável, não apenas diagnosticador

`tools/sprites/autoestabilizar.py` aplica modificações de verdade ao jogo:
gera `android/app/src/main/assets/sprite-autofix.json` com escala e
deslocamento por quadro do **único personagem informado**. O renderer
`SpriteFighterRenderer` consulta `SpriteAutoFix` e aplica os ajustes
**depois** da calibração legado (P01), sem alterar PNG, hitbox ou gameplay.

```bash
# Calcular, comparar antes/depois e MOSTRAR o patch; não escreve o jogo
python3 tools/sprites/autoestabilizar.py p01 --estado VICTORY \
  --videos-dir /videos/animations/p01

# Publicar somente o ajuste que passou a validação do corretor
python3 tools/sprites/autoestabilizar.py p01 --estado VICTORY \
  --videos-dir /videos/animations/p01 --corrigir

# Voltar exatamente ao JSON anterior, sem alterar PNG
python3 tools/sprites/autoestabilizar.py p01 --reverter
```

**Garantias e limites implementados:**

- Só aceita `p01..p12`, e correções por `--estado` (um estado por execução).
- Tamanho: exige prova independente do vídeo e atlas (geometria, pelo
  menos 70% de quadros), fatores concordantes e tamanho final fora do
  idle depois de aplicar a calibração já existente no renderer.
  Corrige com escala uniforme <=25%, sem inventar escala em poses
  anisotrópicas.
- Pés: somente `IDLE`, `VICTORY`, `DEFENSE_STAND`, `DEFENSE_CROUCH`.
  Nas primeiras poses da vitória **do P01** os quadros 0–6
  representam caminhada e não são travados. Deslocamento natural
  além de 32px fica bloqueado; correções menores precisam melhorar
  pelo menos 85% do erro e deixar <0,25px de amplitude.
- Combinar tamanho+ancoragem: offsets são calculados na escala final,
  não sobre uma imagem que já foi esticada duas vezes.
- Nunca substituir sem confirmação um ajuste existente do mesmo
  personagem/atlas; salva snapshot anterior e permite rollback.
- A camada Java valida tamanho, limites, índices e character IDs,
  rejeitando correções inválidas sem aplicar metade do arquivo.
- Um personagem sem atlas/manifesto e estados sem prova ou com
  deslocamento intencional **não é falsamente corrigido**; recebe
  `SEM_AJUSTE_SEGURO`.
- O motor não recalcula fisicamente o personagem ou a câmera.
  Avaliação de qualidade no dispositivo continua distinta da prova
  matemática dos ajustes.

**Validação funcional:** `test_autoestabilizar.py` gera um atlas PNG
sintético com deriva dos pés, executa a correção verdadeira, verifica
amplitude/RMS antes e depois, grava o JSON consumido pelo Android e
reverte exatamente; `SpriteAutoFixTest` valida carregamento seguro
e isolamento no Android. A automação não é descrita como concluída
antes dos testes de CI verdes.

O GitHub Actions não contém as gravações privadas da pasta Drive
automaticamente. Elas precisam ser disponibilizadas ao comando
`--videos-dir` durante a execução; ausência de arquivo original
bloqueia a correção de escala em vez de inventá-la.


## v6 — revisão adversarial e barreiras de publicação (2026-10-11)

**Escopo:** auditoria do motor na branch experimental `refino/p01-animacoes-20261009`.
Não houve merge, alteração de PNG, vídeo, clipe, manifesto de personagem,
timings, hitboxes, câmera ou gameplay. Somente implementação/CI/testes/docs
do motor; build APK não é disparado automaticamente por push nesta branch.
Esta seção atualiza afirmações históricas das versões anteriores:
o corretor existe e publica ajustes de pés com garantias limitadas, mas
**não é um estabilizador visual universal nem um certificado de runtime**.

### Rodadas realmente executadas

1. **Pé aparentemente estável, movimento original possivelmente intencional:**
   o corretor aceitava `correct_shoes` e até validação em bitmap sem
   evidência de que a fonte autoral estava imóvel. Um teste adversarial
   falhou no CI (registrou `CORRIGIDO` onde devia bloquear). Agora
   `foot_fix_permission` só autoriza correções em `IDLE`, `VICTORY`
   ou estados de defesa com apoio quando vídeo e atlas são `MEDIDO`,
   o vídeo não apresenta tremor/deslize de pé, o atlas apresenta defeito
   correspondente e a auditoria temporal aponta
   `SUSPEITA_DE_TREMOR_INTRODUZIDO_NOS_SPRITES`.
   Movimentos também presentes no vídeo original ou evidências
   incompletas bloqueiam a correção, independentemente de a trilha
   parecer melhor após deslocá-la.
2. **Vídeo com quantidade de quadros desconhecida:** a leitura podia
   atingir `max_frames` e retornar `EXTRAIDO` mesmo quando havia
   frames posteriores. Caso adversarial reproduzido com MP4 real
   sintético e `CAP_PROP_FRAME_COUNT=0`. O leitor agora faz uma
   leitura adicional para detectar truncamento; se houver outro frame,
   resultado `INCONCLUSIVO` e nenhuma análise parcial utilizável.
3. **Índices booleanos de frame:** `True` era aceito como inteiro
   em Python e convertido implicitamente em frame 1. Testes reproduziram
   aprovação indevida nos leitores temporal e geométrico. Ambos
   rejeitam `bool`, além de negativos e valores fora de faixa.
4. **Vários estados no mesmo atlas:** o Android endereça ajustes por
   `character|atlas|frame`; a tabela NÃO inclui estado. Um ajuste
   solicitado apenas para VICTORY podia contaminar INTRO no mesmo PNG.
   Até o formato do patch representar o estado com segurança e o
   renderizador consumir esse campo, `propose` bloqueia qualquer atlas
   compartilhado por dois estados. Teste adversarial reproduz esse
   isolamento.
5. **Escala numérica sem inspeção dos pixels corrigidos:** o fator
   geométrico permitia gerar e publicar correção de escala mesmo quando
   `validacao_pixels=NAO_APLICAVEL`. Agora o cálculo continua disponível
   como **proposta**, mas recebe
   `AGUARDANDO_VALIDACAO_PIXELS` e NÃO é publicado como ajuste de
   tamanho até haver prova geométrica/anatômica independente em pixels
   depois da transformação. Isso protege tanto uma correção apenas de
   escala quanto o caso combinado com correção de pé.
6. **Rollback com reformatacão:** `rollback()` recriava JSON e perdia
   os bytes exatos de indentação/ordem/quebras de linha da versão
   anterior. Agora lê e repõe literalmente o snapshot anterior,
   preservando a verificação do checksum do patch atual e a checagem
   do personagem. Teste adversarial verifica igualdade byte a byte.

As regressões foram incluídas nos arquivos existentes:
`test_refinar_elenco.py`, `test_verificar_escala.py` e
`test_autoestabilizar.py`. O workflow
`validate-sprite-stabilizer.yml` agora inclui **os três grupos** e
dispara também quando `autoestabilizar.py` ou seus testes mudam.
`build-apk.yml` não roda mais em cada push experimental; mantém opção
manual. Um workflow verde valida os testes executados, não a estética do
personagem ou transições no jogo.

### Critérios objetivos e limites da prova de tamanho

- **A, base + IDLE:** confirmar que `inicio_centro.png` e o
  `tamanho.json` concordam em até **7,5%**, com chroma e
  silhueta legítimos. A altura do bounding box não é, isoladamente,
  a medida do tamanho real em poses distintas.
- **B, vídeo autoral:** exigir quadros decodificados sem truncamento,
  chroma confiável e correspondências da **cabeça e tronco**, onde
  verificáveis. Vídeo com zoom/câmera móvel sem compensação é
  `INCONCLUSIVO`, não evidência para dimensionar sprite.
- **C, atlas efetivo:** validar TODOS os índices contra layout físico,
  converter `pixelScale` para o domínio do IDLE e medir escala
  geométrica em imagens do atlas. Frames duplicados/holds não valem
  como observações geométricas independentes.
- **Geometria:** SIFT/RANSAC exige >=22 inliers, >=43% dos matches,
  cobertura espacial significativa (15% horizontal e 16% vertical),
  reprojeção mediana <=3,8 px, reversibilidade ida/volta em 3,5%.
  A sequência precisa de >=70% de quadros válidos (mínimo 3),
  distribuídos pelos três terços e com variação relativa <=7%.
  Vídeo e atlas devem concordar em até 6% e ambos permanecer dentro
  de ±8% do idle. Agachamento/braço estendido que impeçam
  correspondência suficiente levam a `INCONCLUSIVO`.
  Os valores são limiares de engenharia, **não** garantia universal
  de reconhecimento anatômico correto.
- **D, renderer Android:** confirmar escala final após
  `P01SpriteCalibration`, `P01VisualTuning`, `pixelScale`,
  offset, raiz, câmera, espelhamento e transições. Esta prova ainda
  NÃO está automatizada end-to-end; `validacao_runtime=PENDENTE`.
  Um PNG ou cálculo de offset consistente não fecha esta etapa.
- **Autofix de pés:** somente em clipes apoiados, com evidência
  independente de que o defeito entrou no atlas e comparação de
  pixels transformados antes/depois; sem aprovação automática do
  personagem como um todo. `--corrigir` publica patch JSON reversível,
  não altera bitmap original. A proposta de escala ainda fica
  pendente e não é gravada como ajuste.

### Pendências críticas e validações NÃO executadas nesta revisão

- A pasta real `animations` do Drive e suas subpastas `p01..p12`
  foram localizadas, mas **não** foi feita nova análise integral
  dos vídeos reais do Drive nem comparação com os atlases de cada um
  dos 12 lutadores. Testes de vídeo geraram clipes MP4 sintéticos; a
  prova geométrica usa também pixels reais do atlas IDLE de P01.
- Ainda faltam confirmação anatômica confiável em poses extremas,
  separação de movimento intencional/câmera em cenas sem chroma,
  validação independente após correção de escala, transições entre
  estados, integridade dos timings e render efetivo a 30/60 FPS.
- Não foi feita captura de runtime Android/emulador nesta rodada:
  o CI isolado não gera APK nem verifica jogabilidade em dispositivo.
- O motor diagnostica um personagem selecionado e só aplica
  automaticamente **correções de pés com evidência forte**.
  `SEM_AJUSTE_SEGURO`, `BLOQUEADO` e `INCONCLUSIVO` são saídas
  esperadas em situações duvidosas, não autorização para inferir poses.

**Estado geral:** mais seguro diante dos falsos positivos reproduzidos,
ainda **não finalizado** como corretor visual universal. Não promover
essa branch a `game-luta-sprite-gpt`/`main` sem aprovação e validação
de runtime.


### v6.1 — metadados de vídeo inválidos (2026-10-11)

Rodada adicional de testes adversariais detectou duas falhas de robustez:
`load_video` podia aceitar vídeo com FPS igual a zero (ou não finito)
e anunciar `EXTRAIDO`, embora não existisse base para preservar timings
a 30/60 FPS. Agora essa situação é `INCONCLUSIVO`, sem amostras.
`read_video_samples` fazia `int(NaN)` quando o OpenCV retornava
contagem de quadros inválida e lançava exceção, em vez de emitir
prova ausente. O leitor geométrico agora rejeita metadados não
numéricos, NaN, infinito e sequências curtas, retornando lista vazia
para que a análise de escala permaneça inconclusiva.

Testes de regressão em `test_refinar_elenco.py` usam MP4 temporário
com FPS real substituído por zero/NaN apenas na interface de leitura;
`test_verificar_escala.py` injeta `CAP_PROP_FRAME_COUNT=NaN`.
Esses testes verificam classificação e integridade da saída,
não a simples existência de uma condição no código.
A reprodução visual real em 30 e 60 FPS permanece pendente.
