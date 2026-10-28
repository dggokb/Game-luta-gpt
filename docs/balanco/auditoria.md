# Auditoria SŌKON: resultado (Fase A, F01–F14, T01–T28)

Resposta ao documento "SŌKON Bloodlines — Auditoria de Combate e Plano de Testes" (28/10/2026).

**Como foi medido:**
- **Código base:** commit `42677571`, com combate igual ao `50c9955d` do documento.
- **Motor:** todos os números saem do `CombatEngine` real, com sementes fixas.
- **Ferramentas** (em `android/app/src/test/java/com/gamelutagpt/`, todas rodam à mão):
  - `BateriaBase`: determinismo, CSV e comando no toque;
  - `BateriaIdentidade`: frente A;
  - `BateriaCombos`: frente B;
  - `BateriaEquipe`: frente C;
  - `BateriaAntesDepois`: as métricas fixas das etapas aprovadas.
- **Os números de T01–T23 são de antes das etapas aprovadas.** O que elas mudaram está em
  `docs/balanco/ajustes.md`.

**Status usados:**
- **CONFIRMADO:** conferido no código e nos dados.
- **REPRODUZIDO:** um teste observou.
- **HIPÓTESE:** ainda sem teste.
- **DECIDIR:** opção de design.
- **IMPLEMENTADO:** com a alteração identificada.
- **PENDENTE:** precisa do aparelho ou de jogadores.

## Resumo

| ID | Status | Resultado principal |
|---|---|---|
| A01 | CONFIRMADO | Do `50c9955d` ao `42677571` só entraram a bateria e o relatório, sem mudar o combate. As etapas 1–5 vieram depois, em commits separados. |
| A02 | REPRODUZIDO | App: 537 de 537 passam (sem Robolectric). Módulos: `motion-core` 38, `stage-core` 31, `story-core` 105 e `ultra-core` 25, todos passam. `pc`: 2 de 10 falham (`GameViewPcTest`, `GameViewCelularPcTest`), sem relação com o combate. Os 11 testes Robolectric não rodam offline (falta o jar). |
| A03 | CONFIRMADO | F01–F14 conferem com o código; duas correções no texto do documento (abaixo). |
| A04 | IMPLEMENTADO | Luta 2x2 completa sem animação em ~0,05 s (CPU × CPU, BattleFlow, Ultra sem página). |
| A05 | IMPLEMENTADO | CSV com os campos mínimos. Duas sementes rodadas duas vezes dão o mesmo estado em todos os quadros (hash igual). |
| F01 / T12 | IMPLEMENTADO · DECIDIR | Etapa 1: atordoamento 20, sem corrida, CPU quebra no tempo. Com 20 quadros ainda entra um L de graça em 65% das quebras. |
| F02 | REPRODUZIDO | O ENERGY ganha 2,97 barras em 8 s (1,88 passivas; a conta de 1,92 confere). Sozinho, é o tipo que menos rende (+1454). |
| F03 | REPRODUZIDO | O eco do SPEED acrescenta 13% de dano (+439). Junto com o combo, o SHINKAI passa de 100% da vida (T10); a etapa 5 reduziu. |
| F04 / T19 | IMPLEMENTADO | Etapa 3: buffer de 0,6 s e estados visuais. A troca perdida foi de 22–31% para 0–8%. |
| F05 / T10 | REPRODUZIDO | Não há loop infinito (o combo mais longo dura 10,3 s), mas há combos de 100%+ com SHINKAI e 3–5 Supers. A etapa 5 reduziu (Ryūga ENERGY 13159 → 10308). |
| F06 / T17 | REPRODUZIDO | 11 assistências de projétil e 5 de corpo a corpo, quase todas sem função. p12, p13 e p15 só acertam a 100 px, e o assist do p14 não acerta nada. |
| F07 / T24 | REPRODUZIDO (simulado) · PENDENTE | No toque simulado, 85–100% dos comandos saem certos. Os de carga perdem 20% em qualquer ritmo. Falta medir no aparelho. |
| F08 / T02 / T25 | REPRODUZIDO | O alcance real passa o calculado pela CPU em 34–48 px em 90 de 96 golpes. O H do Kōga erra o agachado 15 de 16 vezes. Falta a revisão quadro a quadro na tela. |
| F09 / T22 | CONFIRMADO + REPRODUZIDO | A CPU não tem código para S2–S4 (`AiController.java`, só o S1). O p09 agarrava 0,2 vez por minuto (a etapa 4b subiu); o p11 nunca envenena. |
| F10 / A02 | REPRODUZIDO | Ver A02. Sem métrica de balanço na suíte; a `BateriaAntesDepois` cobre isso fora dela. |
| F11 | CONFIRMADO | ep04–ep09 sem nenhuma imagem nas pastas; o Arcade termina no cartão "TAKATOSHI — Em breve". |
| F12 | CONFIRMADO | p13+p14 e p15+p16 estão como `pronta: false`. O comentário do `torneio.json` diz "dupla ainda sem golpes", o que já não vale: os quatro têm golpes. |
| F13 | CONFIRMADO | O menu tem História, Arcade e Modo Teste; não há versus local. |
| F14 / C01 | REPRODUZIDO | Fichas dos 16 na frente A. 14 dos 16 andam, correm e pulam igual. Os três tanques jogam diferente só pela vida e pelo dano. |
| T01 | REPRODUZIDO | O p11 não encosta em 5 de 12 lutas contra zoner; p15 também tem dificuldade (depende da CPU). |
| T03 | CONFIRMADO | Mesma mobilidade para 14 dos 16; as exceções são p13 e p14 (andam mais devagar), p10 (2 air dashes) e p05 (pulo na parede). |
| T04 | REPRODUZIDO | Todos têm antiaéreo com janela de 24–39 quadros, quase sempre o 2H. O super pulo é o mais difícil de tirar (8–27). |
| T05 | REPRODUZIDO | 67 de 153 golpes defendidos são punidos pela maioria do elenco. O M fica entre −7 e −9 em cinco personagens. |
| T06 | REPRODUZIDO | No canto o tempo preso sobe, mas backdash e pulo para trás ainda escapam 84–100%. |
| T07 | REPRODUZIDO | As rotas são o mesmo esqueleto para todos. O 2M derruba em todos os pacotes e nunca abre combo. |
| T08 | REPRODUZIDO | 30–59% da vida sem barra, 38–68% com 1. O combo se pagava com a barra que gera; a etapa 5 reduziu. |
| T09 | REPRODUZIDO | Nenhum golpe normal passa de +1 na defesa. L M H não tem buraco, e o pushblock desfaz a sequência. |
| T11 | REPRODUZIDO | Dos especiais, 30 ficam positivos na defesa e 31 não têm punição. A banana da p16 dá +91 (HIPÓTESE no aparelho). |
| T13 | CONFIRMADO + REPRODUZIDO | A defesa perfeita tem janela de 6 quadros e só sai uma por cadeia. |
| T14 | REPRODUZIDO | Só defendendo, a guarda quebra em 3–7 s. Com pushblock vai a 11–20 s. |
| T15 | REPRODUZIDO | Ganho na janela: POWER +3478, SPEED +2904, TECHNIQUE +1689, ENERGY +1454. O Clash era o desequilíbrio; a etapa 2 corrigiu. |
| T16 | REPRODUZIDO | Todo tipo tem resposta: defender ou fugir leva ~1000–1340 de dano. O caso mais difícil é o SPEED no canto (2094). |
| T18 | REPRODUZIDO | Não há parceiro único dominante, e sim um grupo de projéteis longos. p12, p13 e p14 não somam nada como assist. |
| T20 | REPRODUZIDO | O DHC é o pior uso da barra (1670 de dano por barra, contra 3042 do Super). Em p07+p08 ele corta o Super do p07: 4545 → 2196. |
| T21 | REPRODUZIDO | A dupla oficial só é o melhor parceiro para o Ryūga e o p07; não há sinergia de dupla, e sim força do parceiro. |
| T23 | REPRODUZIDO | A curva por nível é monotônica: contra o nível 5, N1 vence 0% e N8 89%. |
| T26 | PENDENTE | FPS: precisa do aparelho (o print do diagnóstico continua pendente). |
| T27 / T28 | ROTEIRO PRONTO · PENDENTE | Roteiro em `docs/balanco/playtest.md`: perfis, seis sessões de ~95 min, CSV com as colunas da `BateriaBase` e questionários. Faltam os jogadores, e o registro de entradas, o CSV no aparelho e o versus no PC ainda não existem. |
| C04 / F12 | CONFIRMADO · DECIDIR | p13–p16 jogáveis por inteiro (229 testes passam), mas sem intro e vitória, sem nome no jogo para p14 e p16 e com assist que quase não acerta. Recomendação: não liberar ainda. A flag `pronta` não foi mexida. |
| D03 / F11 | PLANO PRONTO · DECIDIR | `docs/historia/plano-ep04-09-takatoshi.md`: 21 páginas em ordem de prioridade, ficha de chefe do Takatoshi, 32 vídeos e roteiro do ep10. As partes inventadas estão marcadas como **proposta**. |
| D04 / F13 | ESPECIFICADO | `docs/modo-treino-versus.md`: treino 6–8 dias, versus no PC 3–4 dias. Hoje todos os controles viram o jogador 1 (`Controles.java`). |

## Fase A

**A02, base de testes:**
- **App (sem Robolectric):** `OK (537 tests)` no `42677571`, e `OK (541 tests)` depois das etapas.
- **Módulos:** rodados com JUnit direto, porque o Gradle offline não tem o JUnit em cache para eles.
  Todos passam: `motion-core` 38, `stage-core` 31, `story-core` 105, `ultra-core` 25.
- **`pc`:** falham `GameViewPcTest` e `GameViewCelularPcTest.lutaEUltraComArteEmSegundoPlano`, sem ligação
  com o combate.
- **Robolectric:** os 11 testes não rodam aqui (falta o jar offline).
- **Testes das ferramentas Python:** as 3 falhas de antes continuam (registradas na sessão anterior).

**A05, determinismo:**
- `BateriaBase determinismo`: duas sementes (7 e 1234), cada uma rodada duas vezes do zero. O hash do
  estado dos dois lutadores (posição, vida, barra e estado) em todos os quadros deu igual nas duas.
- `BateriaBase csv <arquivo> <n>` grava as partidas com os campos mínimos do documento:
  `commit_sha, test_id, fighter, opponent, assist_partner, …, meter_spent, assists_called, tags_completed,
  breakers_used, shinkai_activations, result, state_hash`.

**A03, correções ao documento:**
- **Classes de teste:** são 74 arquivos no app, incluindo as baterias, que não são testes da suíte.
- **Startup:** confere com o documento, mas vale notar que o 2M do Ryūga tem startup 12, o mais lento
  dos 2M (os outros têm 6–9).
- **F12:** o motivo de `pronta: false` no `torneio.json` ("ainda sem golpes") está desatualizado.

## T24 (simulado): comandos no toque

- **O que se mede:** tentativas em que cada direção fica de 1 a 5 quadros, às vezes sem a diagonal, às
  vezes com o polegar escorregando para ↗, e o botão chega 0–4 quadros depois.
- **Escopo:** os S1–S4 de chão dos 16, com 60 tentativas por comando.
- **Limite:** não substitui o teste no aparelho; serve para dizer onde olhar.

| perfil | comando | tentativas | saiu certo | saiu outro especial | saiu golpe normal | pulou | nada |
|---|---|---|---|---|---|---|---|
| limpo (2 quadros por direção) | [←] → | 300 | 80% | 0% | 20% | 0% | 0% |
| limpo (2 quadros por direção) | [↓] ↑ | 180 | 80% | 0% | 20% | 0% | 0% |
| limpo (2 quadros por direção) | ← ↙ ↓ | 60 | 100% | 0% | 0% | 0% | 0% |
| limpo (2 quadros por direção) | ← ↙ ↓ ↘ → | 180 | 100% | 0% | 0% | 0% | 0% |
| limpo (2 quadros por direção) | → ↓ ↘ | 540 | 100% | 0% | 0% | 0% | 0% |
| limpo (2 quadros por direção) | → ↘ ↓ ↙ ← | 180 | 100% | 0% | 0% | 0% | 0% |
| limpo (2 quadros por direção) | ↓ ↓ | 720 | 100% | 0% | 0% | 0% | 0% |
| limpo (2 quadros por direção) | ↓ ↘ → | 840 | 93% | 0% | 7% | 0% | 0% |
| limpo (2 quadros por direção) | ↓ ↙ ← | 780 | 100% | 0% | 0% | 0% | 0% |
| toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega) | [←] → | 300 | 80% | 0% | 20% | 0% | 0% |
| toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega) | [↓] ↑ | 180 | 80% | 0% | 20% | 0% | 0% |
| toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega) | ← ↙ ↓ | 60 | 92% | 0% | 5% | 3% | 0% |
| toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega) | ← ↙ ↓ ↘ → | 180 | 98% | 1% | 0% | 1% | 0% |
| toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega) | → ↓ ↘ | 540 | 96% | 0% | 2% | 2% | 0% |
| toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega) | → ↘ ↓ ↙ ← | 180 | 98% | 1% | 1% | 1% | 0% |
| toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega) | ↓ ↓ | 720 | 96% | 0% | 3% | 2% | 0% |
| toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega) | ↓ ↘ → | 840 | 92% | 0% | 8% | 0% | 0% |
| toque calmo (2–4 quadros, 10% pula diagonal, 5% escorrega) | ↓ ↙ ← | 780 | 95% | 0% | 2% | 3% | 0% |
| toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega) | [←] → | 300 | 80% | 0% | 20% | 0% | 0% |
| toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega) | [↓] ↑ | 180 | 80% | 0% | 20% | 0% | 0% |
| toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega) | ← ↙ ↓ | 60 | 87% | 0% | 8% | 5% | 0% |
| toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega) | ← ↙ ↓ ↘ → | 180 | 92% | 5% | 0% | 3% | 0% |
| toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega) | → ↓ ↘ | 540 | 87% | 0% | 8% | 5% | 0% |
| toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega) | → ↘ ↓ ↙ ← | 180 | 90% | 3% | 3% | 4% | 0% |
| toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega) | ↓ ↓ | 720 | 85% | 0% | 9% | 5% | 0% |
| toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega) | ↓ ↘ → | 840 | 90% | 0% | 9% | 1% | 0% |
| toque apressado (1–3 quadros, 30% pula diagonal, 15% escorrega) | ↓ ↙ ← | 780 | 87% | 0% | 7% | 6% | 0% |

**O que isso mostra:**
- **Os comandos de carga** (`[←] →`, `[↓] ↑`) saem errados 20% das vezes, mesmo feitos limpos. A
  carga sorteada às vezes fica abaixo de `chargeFrames` (30), e aí sai o golpe normal. Vale mostrar a
  carga na tela.
- **O ↓ ↘ →** perde 7–9% para o golpe normal, até limpo. O último → chega junto com o botão e às vezes
  não fecha o comando.
- **Com pressa,** os de meia-lua perdem 10–15%: um pouco para o golpe normal e um pouco para o pulo
  (o polegar escorregando para ↗).
- **No aparelho,** confirmar com o registro de entradas (T24) antes de mexer no buffer ou no
  MotionParser, como o documento pede.

## Backlog priorizado (B04)

Em ordem de efeito esperado, facilidade de teste e risco de regressão. Os itens 1 a 5 já viraram as
etapas aprovadas (`docs/balanco/ajustes.md`). O que ficou para decidir:

1. **CPU usar S2–S4 e a mecânica própria de cada personagem** (F09, T22). É a maior lacuna medida: a
   identidade do elenco some na CPU. Custo médio, risco baixo, testável na bateria.
2. **Assistências de corpo a corpo que não acertam** (F06, T17): p12, p13, p15 e p14. Elas nascem 70 px
   atrás do titular e o golpe não anda. Ajustar a posição ou o golpe de cada uma.
3. **Breaker com 20 quadros ainda dá um L de graça** (etapa 1, DECIDIR):
   - A: atordoamento ~4;
   - B: empurrar mais (170 → 320);
   - C: manter.
4. **p09:** o encurtamento de verdade do 2M seria `alcanceMaximo` 190 (DECIDIR, depois do teste com
   jogadores). A velocidade 300 → 270 não foi necessária.
5. **O DHC corta o Super do p07** (T20): o dano cai de 4545 para 2196. Pode ser defeito; reproduzir e
   corrigir.
6. **O 2M derruba em todos os pacotes e o cancelamento em H nunca acerta** (T07). Isso tira a variedade
   de rotas. DECIDIR: 2M que abre combo em parte do elenco.
7. **O H do Kōga erra quem está agachado** (T02, 15 de 16). Revisar as caixas na tela antes de mexer.
8. **A régua de alcance da CPU** (`CombatRules.maxCenterDistance`) soma só uma meia-largura, e a CPU
   acha que não alcança quando alcança. Correção de baixo risco.
9. **O M fica entre −7 e −9 em cinco personagens** (T05) e parece herança de dados; DECIDIR.
10. **2H universal como antiaéreo e mobilidade igual para 14 de 16** (T03, T04). É uma decisão de
    identidade (DECIDIR).
11. **Campanha:** arte de ep04–ep09, Takatoshi e versus local (F11, F13); `pronta: false` de p13–p16
    (F12) depois da conferência audiovisual.
12. **No aparelho e com jogadores:** T24 (registro de entradas), T25 (caixas na tela), T26 (FPS), T27 e
    T28.

---

## Anexo: Auditoria SŌKON, frente A: identidade, movimento, defesa e CPU

Medido no motor do jogo (`CombatEngine`, o mesmo do APK) com sementes fixas, sobre o HEAD 42677571
(50c9955d + a bateria de balanço; nenhum arquivo de combate mudou). Ferramenta:
`android/app/src/test/java/com/gamelutagpt/BateriaIdentidade.java` (`main`, roda à mão:
`[t01|t02|t03|t04|t05|t06|t22|t23|c01|tudo]`, ~25 s no total). As tabelas completas estão no fim.
Nada de produção foi alterado.

Ressalvas gerais: os cenários com roteiro (T02–T06) não dependem da CPU; T01, T22, T23 e parte da C01
são CPU × CPU (nível 8, salvo dito) e mostram como a CPU joga, não o teto do personagem.

---

```text
ID: T01
STATUS: REPRODUZIDO (depende da CPU)
CENÁRIO: 1x1, lutador X (CPU 8) × Ryūga (CPU 8, o da bola), começando a 80/180/350/600 px; 12 sementes por distância;
  mede quadros do GO até o 1º contato corpo a corpo de X (acerto ou defesa), dano levado antes e barra gasta.
RESULTADO: a 600 px a maioria chega em 140–360 q (2,3–6 s): p14 139, p13 182, p04 216, p12 246, Kōga 272, p09 309.
  Os mais lentos para encostar: p11 (869 q, 5/12 nunca encostam em 20 s, 4.427 de dano levado), Ryūga espelho (510),
  p06 (488), p08 (498) e p15 (449, 2 nunca). De perto (180 px) o p07 e a Yin-lu levam 300–430 q e mais de 2.400 de dano.
  Barra gasta na aproximação: só o Ryūga (0,71) e o p11 (0,50) gastam algo relevante.
IMPACTO: p11 e p15 não têm aproximação confiável contra zoner; p08 encosta rápido a 350 px (87 q) pelo chicote.
ALTERNATIVAS: manter (p11/p08 são de alcance e não precisam encostar); dar à CPU de alcance um plano de "ficar longe".
RECOMENDAÇÃO: não mexer em personagem por isso; o número de p11/p15 é a CPU (ver T22: ela não usa S2–S4).
```

```text
ID: T02 / F08
STATUS: REPRODUZIDO
ARQUIVO E LINHAS: CombatRules.maxCenterDistance (CombatRules.java:134: reach + halfWidth do alvo);
  CombatEngine.golpeToca (CombatEngine.java:2572, FOLGA_DESENHO = 6, :2490)
CENÁRIO: cada normal de cada lutador contra o Ryūga parado (em pé e agachado), varrendo a distância entre eixos
  de 4 em 4 px; comparado com o que a CPU acha que alcança e com o alcance nominal; depois contra os 16 agachados.
RESULTADO:
  - O alcance real passa o que a CPU calcula em ~34–48 px em 90 de 96 golpes (a régua da CPU soma só uma
    meia-largura do alvo; o acerto real usa o desenho). Caso extremo: 2H do p07, real 264 × previsto 154 (+110).
  - Golpes em pé que erram quem está agachado: o H do Kōga acerta só 1 dos 16 agachados; L/M/H do p07 1/5/1;
    L/M do p11, p13 e p15 só 1–2 de 16; L/M do Ryūga 6/8 de 16; L do Kōga 4/16, M 5/16.
  - Alcances de 2M mais longos: p13 336, p14 332, p15 332, Kōga 312, p09 304; H do p08 432 (chicote).
IMPACTO: (a) a CPU joga ~40 px mais curta do que poderia (anda mais, cutuca menos); (b) agachar anula a pressão em pé
  do Kōga (o H dele nunca entra em agachado), do p07 e de p11/p13/p15 no L/M; isso também explica o "–" do L/M/H do
  Kōga na tabela de pressão anterior (o defensor perfeito agachava).
ALTERNATIVAS: (a) corrigir a régua da CPU para o alcance medido (tabela por golpe gerada no build) ou somar a outra
  meia-largura; (b) decidir golpe a golpe se "passar por cima de agachado" é intenção; se não, abaixar o quadro ativo
  do desenho (caixas_sprites) ou dar uma caixa de acerto mínima mais baixa.
RECOMENDAÇÃO: (a) é bug de CPU de baixo risco: ajustar. (b) DECIDIR (Diego): o H do Kōga errando 15 de 16 agachados
  não parece intenção para o lutador "de pressão"; revisar no T25 com as caixas na tela antes de mexer.
TESTES DE REGRESSÃO: BateriaIdentidade t02 (antes/depois); CpuTest, AjusteCaixasTest.
```

```text
ID: T03
STATUS: CONFIRMADO + REPRODUZIDO
ARQUIVO E LINHAS: CombatConfig (walkSpeed 300, jumpSpeed 1492, jumpForwardSpeed 400, superJumpSpeed 1450);
  CharacterDefinition.Fighter.airDashes / wallJump
CENÁRIO: roteiro: andar, → → (20 q), pulo neutro/frente/trás, super pulo (↓ ↗), pulo + air dash no topo, pulo correndo.
RESULTADO: 14 dos 16 lutadores têm exatamente a mesma mobilidade: anda 300 px/s, dash 207 px em 20 q, pulo de
  263 px de altura em 41 q, 280 px à frente / 203 atrás, super pulo 627 px de altura e 525 à frente em 104 q,
  air dash +210 px, pulo correndo 369 px. Diferenças: p13 anda 170 e corre 134 (pulo correndo 240); p14 anda 200;
  p10 tem 2 air dashes (+210 cada); p05 tem pulo na parede. Nenhuma diferença de altura ou tempo de pulo.
IMPACTO: "mobilidade" não diferencia o elenco: os aéreos (p05, p10) e os pesados (p09, p07) pulam igual; o p09
  "lento" anda como todos (já visto na bateria).
ALTERNATIVAS: gravidade/velocidade de pulo e de andar por personagem (os campos existem para andar; faltam para pulo).
RECOMENDAÇÃO: DECIDIR um perfil de mobilidade por arquétipo (ex.: tanques andando 200–240 e pulando mais baixo;
  aéreos com pulo mais alto/longo). Mudança de dados, testável com t03 e T04.
```

```text
ID: T04
STATUS: REPRODUZIDO
CENÁRIO: o Ryūga pula à frente (300 px) com jH na descida (a 230 px e "tarde" a 180 px do chão), pulo correndo
  (520 px), cruzado (150 px), super pulo (330 px); o Kōga no mergulho (S4). O defensor aperta 2H, S2 ou S1 terminando
  em cada quadro de 0 a 90; conta os quadros em que tira do ar (acerta quem ainda estava no ar) sem levar.
RESULTADO: todos os 16 têm resposta com janela larga contra o pulo normal: 24–35 q (jH cedo) e 28–39 q (jH tarde),
  quase sempre com o 2H; cruzado 19–37 q; mergulho do Kōga 26–41 q; super pulo é o mais difícil (8–27 q; p10 só 8,
  p05 10). Os melhores antiaéreos são S2 da Yin-lu, do p08 e do p12 (31–41 q).
IMPACTO: tirar do ar é fácil para todo mundo (uma janela de 25 q é quase meio segundo); pular é quase sempre ruim, e
  nenhum antiaéreo é "o" melhor: o 2H universal resolve.
ALTERNATIVAS: manter (o jogo de chão fica no centro); ou estreitar o 2H como antiaéreo (menos quadros ativos para
  cima, ou ele perde para jH cedo) para valorizar os especiais antiaéreos e o p05/p10 aéreos.
RECOMENDAÇÃO: DECIDIR. Se a identidade aérea de p05/p10 importa, o 2H universal está forte demais; testar com 2H
  ativo mais curto e repetir t04.
```

```text
ID: T05
STATUS: REPRODUZIDO
CENÁRIO: (1) cada golpe (L, M, H, 2L, 2M, 2H, S1–S4) de cada atacante defendido de perto; cada um dos 16 defensores
  tenta L, 2L, M, S1, S2 ou Super saindo no 1º quadro livre (comando entrado durante a defesa); quem atacou defende
  assim que pode. (2) Normais errados a 40 px além do alcance; os 4 de calibração castigam com M, 2M, S1 ou Super no
  fim do ativo (sem reação) e 13 q depois (reação humana boa).
RESULTADO:
  - Os dados de vantagem seguem regras fixas: 2M +1 e H −2 para todos, 2H −4 (Ryūga −9), L −4 a −9.
    67 de 153 golpes defendidos são castigados por pelo menos 8 dos 16 defensores; quase todos pelo L/2L (66).
  - O M divide o elenco: −7 a −9 e punido por todos em Ryūga, Yin-lu, p04, p05, p10; +1 e seguro nos outros 11.
  - Especiais inseguros: S2 do Kōga (−10), S2 do p07/p08/p14 (−12), S1 do p13 (−24), S4 do Ryūga (−15), S4 de
    p12/p15/p16 (−6) — todos punidos por 13–16 defensores. S3 da p16 deixa +91 (a banana fica em campo).
  - Golpe errado: com reação humana (13 q) só H e 2H são castigáveis de forma ampla (2–4 dos 4); L, 2L e o M quase
    nunca (0–1). Sem reação (adivinhando) quase tudo é castigável.
IMPACTO: punir é ferramenta de todos (o L basta); o "técnico" (Ryūga) não tem vantagem de punição sobre os outros —
  e é o que tem o M e o 2H mais inseguros.
ALTERNATIVAS: manter as regras fixas e diferenciar só pelos especiais; ou dar vantagem/segurança por arquétipo
  (pressão com M +1/+2, técnico com mais recuperação e melhor punidor).
RECOMENDAÇÃO: DECIDIR. Rever o M −9 do Ryūga/Yin-lu/p04/p05/p10 (é o mesmo golpe que é +1 nos outros 11, parece
  herança de dados, não design) e o S1 −24 do p13. Reproduzir: BateriaIdentidade t05.
```

```text
ID: T06
STATUS: REPRODUZIDO (atacante = CPU)
CENÁRIO: a CPU 8 ataca o Ryūga com defesa perfeita, no centro e encostado no canto; depois de cada sequência ele tenta
  backdash, pulo para trás ou pushblock (1 barra garantida); 12 lutas de 20 s por caso.
RESULTADO: no canto o tempo preso sobe (Kōga 26 → 36%, Ryūga 37 → 49%, p09 10 → 29%, p14 5 → 19%), mas as saídas
  continuam funcionando: backdash escapa 84–100% e pulo para trás 91–100% em todos; o pushblock afasta 160–200 px
  no canto e no centro. Quebras de guarda iguais nos dois (2–4 a cada 20 s, contra quase todo atacante; p07 e p14 menos).
  Pior caso: p06 no canto (backdash 84%, 4 quebras/20 s).
IMPACTO: o canto não aprisiona; o que pesa é a guarda quebrar a cada ~6 s de defesa contínua.
ALTERNATIVAS: manter; ou guarda que volta mais rápido longe da pressão.
RECOMENDAÇÃO: manter o canto. A taxa de quebra de guarda é decisão de design (frente B/T14).
```

```text
ID: T22 / F09
STATUS: CONFIRMADO (código) + REPRODUZIDO
ARQUIVO E LINHAS: AiController.press (AiController.java:1443: só "S" vira out.special = "M"); nenhuma referência a
  S2/S3/S4 em AiController, OpponentAi, ComboBook ou CpuStyle; CpuStyle só tem distância, agressividade, zoning,
  agarrão, pulo e paciência.
CENÁRIO: CPU 8, 1x1, 24 lutas por lutador em rodízio; conta golpes iniciados por id, agarrões que entraram, pulos,
  distância, defesa.
RESULTADO: a CPU nunca usa S2, S3 nem S4 (0,0/min em 13 dos 16; os poucos 0,2–0,6/min de p04/p09/p11 são comandos
  formados por acaso ao andar). Mecânicas próprias pedidas: p09 agarra 0,2/min (cotovelada 0); p11 envenena 0 vezes
  (a seringa é S2); p05 teleporta 0; p06 puxa 0; p12 paga vida 1.271/min (pelo S1). O que diferencia: o S1 (p08 31/min,
  p11 32/min, p06 25/min, Ryūga 19/min; p07 1,3; p14 0), a distância média (p08 381, p11 361, p06 308 × p12 208,
  p13 225) e o tempo colado (p12 56%, p13 52% × p08 17%).
IMPACTO: metade do kit de cada lutador não aparece contra a CPU (Arcade, história e treino). As identidades "p09
  agarra, p11 envenena, p05 confunde, p06 controla" não existem no comportamento.
ALTERNATIVAS: (a) incluir S2–S4 no caderno de combos e no neutro da CPU, por papel (antiaéreo, investida, contra-golpe,
  armadilha), com chances por CpuStyle; (b) por personagem, uma "mecânica-assinatura" com prioridade.
RECOMENDAÇÃO: (a) + (b), é a maior lacuna de identidade medida. Implementar com teste de frequência mínima por
  personagem (ex.: p09 agarra ≥ 1/min, p11 envenena ≥ 1/luta).
```

```text
ID: T23
STATUS: REPRODUZIDO
CENÁRIO: nível N × nível 5, mesmo lutador dos dois lados (sorteado), 80 lutas por nível.
RESULTADO: vence o nível 5: N1 0%, N2 1%, N3 10%, N4 18%, N5 44% (espelho ~50%), N6 55%, N7 70%, N8 89%. Defesa
  (fração dos golpes recebidos defendida) 2% → 24%; golpes/min 15 → 36; combo médio 1,5–1,9 acertos (maior 5 → 9);
  Breaker 0 até o N3, 0,36/luta no N8; pulos 0,1 → 3,9/min.
IMPACTO: a curva é íngreme em 1–4 (quase nunca vencem o 5) e as diferenças vêm de reação, defesa e combos, não de
  dano (conforme CpuLevel). A defesa do N8 é baixa de propósito ("CPU mais agressiva").
ALTERNATIVAS: manter; suavizar 3–4 (o tutorial e o começo do Arcade usam esses níveis).
RECOMENDAÇÃO: manter a regra (sem vantagem artificial); reavaliar 3–4 depois que a CPU usar S2–S4 (T22), porque isso
  muda todos os níveis.
```

```text
ID: C01
STATUS: REPRODUZIDO (notas = quintil no elenco; sinergia fica para T17–T21)
CENÁRIO: junta T02 (alcance M/H/2M), T03 (mobilidade), T05 + T22 (vantagem média dos normais + tempo colado da CPU),
  vida + T04 (janela antiaérea) + startup do L (defesa), maior combo sem barra (busca em feixe da bateria) e
  projétil + tempo longe da CPU (controle de espaço).
RESULTADO (calibração):
  - Ryūga: alcance 1, mobilidade 1, pressão 1, defesa 1, explosivo 5 (8.075 sem barra), espaço 2 — um lutador de
    dano e punição com os normais mais curtos e mais inseguros do elenco.
  - Kōga: alcance 2, pressão 4, explosivo 2, espaço 4 (2M 312 px, mas L/M/H em pé erram agachado).
  - p09: alcance 3, pressão 1, defesa 4 (16.100 de vida), explosivo 5 (6.924) — pesa pela vida e pelo dano, não por
    agarrar.
  - p11: tudo 1 exceto espaço 5 (S1 a 950 px/s, 32% do tempo longe) — controle de espaço puro, sem veneno na prática.
  Mobilidade: 1 para 13 lutadores (empatados), 5 só p05 e p10 — confirma o T03.
IMPACTO: alcance, pressão e espaço já separam o elenco; mobilidade não separa; o "tanque" p09 e o "técnico" Ryūga não
  aparecem como tal nos números.
RECOMENDAÇÃO: usar as fichas como base da frente B/C e do T27 (identidade às cegas); preencher sinergia com T17–T21.
```

---

### Tabelas completas

### T02 Controle de espaço: alcance efetivo × nominal

Distância entre os eixos (px) em que o golpe ainda acerta o Ryūga parado, em pé / agachado; entre
parênteses o que a CPU acha que alcança (CombatRules.reaches) e o alcance nominal do pacote.
Corpo do alvo: meia-largura 36 px.

| lutador | L | M | H | 2L | 2M | 2H | projétil (S1) |
|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 204 / erra (170, 135) | 208 / erra (172, 137) | 256 / 224 (218, 183) | 220 / 188 (182, 147) | 240 / 200 (194, 158) | 172 / 140 (134, 98) | 760 px/s, 720 px |
| Yin-lu (p02) | 220 / 188 (184, 149) | 264 / 224 (218, 183) | 276 / 236 (230, 195) | 184 / 144 (138, 103) | 220 / 176 (172, 137) | 172 / 140 (136, 100) | 700 px/s, 260 px |
| Kōga (p03) | 208 / erra (174, 138) | 204 / erra (168, 132) | 208 / erra (174, 139) | 232 / 200 (196, 161) | 312 / 272 (266, 231) | 172 / 140 (134, 99) | 950 px/s, 380 px |
| p04 | 216 / erra (182, 147) | 260 / 216 (212, 177) | 260 / 220 (214, 178) | 224 / 192 (188, 153) | 288 / 248 (242, 206) | 180 / 148 (130, 94) | 800 px/s, 200 px |
| p05 | 204 / 172 (168, 133) | 276 / 236 (232, 196) | 204 / 172 (170, 135) | 220 / 188 (182, 147) | 220 / 176 (172, 137) | 184 / 152 (148, 112) | 1000 px/s, 900 px |
| p06 | 172 / 140 (136, 101) | 244 / 212 (206, 171) | 216 / 184 (180, 144) | 224 / 184 (178, 142) | 272 / 232 (294, 259) | 176 / 144 (140, 104) | 480 px/s, 800 px |
| p07 | 196 / erra (164, 129) | 264 / erra (228, 192) | 228 / erra (192, 157) | 204 / 164 (158, 123) | 192 / 152 (192, 156) | 264 / 232 (154, 118) | 700 px/s, 150 px |
| p08 | 196 / 156 (154, 118) | 284 / 244 (238, 203) | 432 / 400 (396, 360) | 208 / 168 (164, 128) | 200 / 160 (186, 150) | 200 / 168 (144, 108) | 1100 px/s, 650 px |
| p09 | 228 / erra (192, 157) | 276 / 244 (238, 202) | 196 / 164 (158, 123) | 280 / 236 (232, 197) | 304 / 260 (286, 250) | 180 / 148 (144, 108) | 600 px/s, 900 px |
| p10 | 200 / erra (164, 129) | 284 / 252 (246, 210) | 196 / 164 (160, 125) | 208 / 176 (172, 137) | 252 / 212 (206, 170) | 224 / 192 (188, 153) | 900 px/s, 720 px |
| p11 | 236 / erra (200, 164) | 220 / erra (184, 148) | 236 / 204 (198, 163) | 256 / 224 (218, 182) | 264 / 220 (232, 197) | 180 / 148 (142, 106) | 950 px/s, 900 px |
| p12 | 272 / 240 (234, 198) | 264 / 220 (216, 181) | 160 / 128 (124, 89) | 256 / 216 (210, 175) | 296 / 256 (252, 216) | 168 / 136 (132, 97) | 700 px/s, 150 px |
| p13 | 240 / erra (204, 169) | 260 / erra (226, 190) | 284 / 252 (250, 214) | 304 / 272 (268, 232) | 336 / 296 (292, 256) | 184 / 152 (148, 113) | 600 px/s, 900 px |
| p14 | 240 / 208 (204, 168) | 256 / 224 (218, 182) | 324 / 280 (276, 241) | 296 / 252 (248, 213) | 332 / 292 (312, 276) | 172 / 140 (136, 100) | 600 px/s, 900 px |
| p15 | 196 / erra (162, 126) | 196 / erra (160, 124) | 300 / 268 (262, 227) | 208 / 176 (170, 135) | 332 / 288 (292, 256) | 204 / 172 (166, 130) | 900 px/s, 320 px |
| p16 | 200 / erra (166, 130) | 224 / 192 (186, 150) | 268 / 236 (232, 197) | 196 / 164 (168, 133) | 292 / 252 (248, 212) | 224 / 192 (184, 148) | 500 px/s, 900 px |

Previsto − real acima de 30 px: 90 de 96 golpes. Ryūga (p01) L -34; Ryūga (p01) M -36; Ryūga (p01) H -38; Ryūga (p01) 2L -38; Ryūga (p01) 2M -46; Ryūga (p01) 2H -38; Yin-lu (p02) L -36; Yin-lu (p02) M -46; Yin-lu (p02) H -46; Yin-lu (p02) 2L -46; Yin-lu (p02) 2M -48; Yin-lu (p02) 2H -36; Kōga (p03) L -34; Kōga (p03) M -36; Kōga (p03) H -34; Kōga (p03) 2L -36; Kōga (p03) 2M -46; Kōga (p03) 2H -38; p04 L -34; p04 M -48; p04 H -46; p04 2L -36; p04 2M -46; p04 2H -50; p05 L -36; p05 M -44; p05 H -34; p05 2L -38; p05 2M -48; p05 2H -36; p06 L -36; p06 M -38; p06 H -36; p06 2L -46; p06 2H -36; p07 L -32; p07 M -36; p07 H -36; p07 2L -46; p07 2H -110; p08 L -42; p08 M -46; p08 H -36; p08 2L -44; p08 2H -56; p09 L -36; p09 M -38; p09 H -38; p09 2L -48; p09 2H -36; p10 L -36; p10 M -38; p10 H -36; p10 2L -36; p10 2M -46; p10 2H -36; p11 L -36; p11 M -36; p11 H -38; p11 2L -38; p11 2M -32; p11 2H -38; p12 L -38; p12 M -48; p12 H -36; p12 2L -46; p12 2M -44; p12 2H -36; p13 L -36; p13 M -34; p13 H -34; p13 2L -36; p13 2M -44; p13 2H -36; p14 L -36; p14 M -38; p14 H -48; p14 2L -48; p14 2H -36; p15 L -34; p15 M -36; p15 H -38; p15 2L -38; p15 2M -40; p15 2H -38; p16 L -34; p16 M -38; p16 H -36; p16 2M -44; p16 2H -40; 
Não acertam o Ryūga agachado: Ryūga (p01) L, Ryūga (p01) M, Kōga (p03) L, Kōga (p03) M, Kōga (p03) H, p04 L, p07 L, p07 M, p07 H, p09 L, p10 L, p11 L, p11 M, p13 L, p13 M, p15 L, p15 M, p16 L.

Contra cada um dos 16 agachados (alvos que o golpe acerta em alguma distância de 60 a 240 px):

| atacante | L | M | H |
|---|---|---|---|
| Ryūga (p01) | 6/16 | 8/16 | 15/16 |
| Yin-lu (p02) | 11/16 | 16/16 | 16/16 |
| Kōga (p03) | 4/16 | 5/16 | 1/16 |
| p04 | 6/16 | 16/16 | 16/16 |
| p05 | 14/16 | 16/16 | 16/16 |
| p06 | 15/16 | 15/16 | 15/16 |
| p07 | 1/16 | 5/16 | 1/16 |
| p08 | 16/16 | 16/16 | 15/16 |
| p09 | 6/16 | 14/16 | 15/16 |
| p10 | 6/16 | 16/16 | 16/16 |
| p11 | 1/16 | 1/16 | 15/16 |
| p12 | 15/16 | 16/16 | 15/16 |
| p13 | 1/16 | 1/16 | 15/16 |
| p14 | 15/16 | 15/16 | 16/16 |
| p15 | 2/16 | 1/16 | 16/16 |
| p16 | 6/16 | 15/16 | 15/16 |

### T03 Mobilidade (andar, dash, pulo, super pulo, air dash)

| lutador | anda (px/s) | dash: distância em 20 q | pulo neutro: altura / quadros | pulo à frente: distância | pulo atrás | super pulo: altura / distância / quadros | air dash: +distância | air dashes (2º: +distância) | pulo correndo: distância no ar | pulo na parede |
|---|---|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| Yin-lu (p02) | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| Kōga (p03) | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p04 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p05 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | sim |
| p06 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p07 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p08 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p09 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p10 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 2 (+210) | 369 | não |
| p11 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p12 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p13 | 170 | 134 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 240 | não |
| p14 | 200 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p15 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |
| p16 | 300 | 207 | 263 / 41 | 280 | -203 | 627 / 525 / 104 | +210 | 1 | 369 | não |

### T01 Aproximação

CPU nível 8 contra o Ryūga CPU nível 8 (o da bola), 1x1, começando a 80/180/350/600 px. Mede os quadros até
o primeiro contato (acerto ou defesa) do lutador, o dano levado antes disso e as barras gastas. 12 sementes por distância.

| lutador | 80 px: quadros / dano levado | 180 px: quadros / dano levado | 350 px: quadros / dano levado | 600 px: quadros / dano levado | barra gasta (600 px) |
|---|---|---|---|---|---|
| Ryūga (p01) | 130 / 1181 | 207 / 1159 (1 nunca) | 369 / 2753 (1 nunca) | 510 / 1176 (1 nunca) | 0.71 |
| Yin-lu (p02) | 466 / 4070 (3 nunca) | 316 / 2453 | 465 / 2160 (2 nunca) | 280 / 2335 (1 nunca) | 0.00 |
| Kōga (p03) | 164 / 1372 | 37 / 42 | 330 / 2270 | 272 / 1953 (1 nunca) | 0.10 |
| p04 | 230 / 1694 (1 nunca) | 28 / 0 | 379 / 1889 (2 nunca) | 216 / 1676 | 0.19 |
| p05 | 221 / 1791 | 112 / 611 | 572 / 3622 (2 nunca) | 338 / 2086 | 0.10 |
| p06 | 199 / 1746 | 41 / 51 | 602 / 3919 (3 nunca) | 488 / 1802 (1 nunca) | 0.06 |
| p07 | 287 / 2627 | 426 / 3951 (1 nunca) | 371 / 3422 | 373 / 3169 | 0.38 |
| p08 | 177 / 1628 | 98 / 878 | 87 / 625 | 498 / 2570 (2 nunca) | 0.15 |
| p09 | 227 / 1941 | 83 / 201 | 212 / 977 (1 nunca) | 309 / 1928 (1 nunca) | 0.13 |
| p10 | 216 / 1985 | 27 / 0 | 281 / 2043 | 359 / 2616 | 0.02 |
| p11 | 284 / 1961 (1 nunca) | 371 / 2261 (2 nunca) | 790 / 4503 (5 nunca) | 869 / 4427 (5 nunca) | 0.50 |
| p12 | 173 / 1189 | 41 / 176 | 145 / 1175 | 246 / 2326 | 0.10 |
| p13 | 81 / 610 | 49 / 217 | 241 / 1402 | 182 / 1586 | 0.02 |
| p14 | 242 / 2218 | 43 / 267 | 143 / 972 | 139 / 1136 | 0.19 |
| p15 | 396 / 3424 (3 nunca) | 409 / 2886 | 464 / 3147 (2 nunca) | 449 / 3306 (2 nunca) | 0.06 |
| p16 | 290 / 2760 | 37 / 242 | 417 / 3181 | 280 / 2265 | 0.19 |

### T04 Antiaéreo

O Ryūga pula com jH apertado na descida (a 230 px do chão, ou a 180 px no "jH tarde"); no mergulho é o S4 do Kōga. Quem defende
aperta 2H, S2 ou S1 em cada quadro de 0 a 90; a célula é a janela (quadros em que tira do ar sem levar)
da melhor das três respostas, e qual foi. 0 = nenhuma resposta funciona em nenhum quadro.

| defensor | pulo à frente | jH tarde (180 px) | pulo longo (corrida) | cruzado | super pulo | mergulho (Kōga S4) |
|---|---|---|---|---|---|---|
| Ryūga (p01) | 25 (2H) | 29 (2H) | 19 (2H) | 17 (2H) | 15 (2H) | 26 (2H) |
| Yin-lu (p02) | 35 (S2) | 39 (S2) | 31 (S2) | 32 (S2) | 16 (S2) | 41 (S2) |
| Kōga (p03) | 24 (2H) | 28 (2H) | 23 (S2) | 30 (2H) | 15 (2H) | 26 (2H) |
| p04 | 26 (2H) | 30 (2H) | 27 (S2) | 16 (2H) | 18 (2H) | 30 (S2) |
| p05 | 24 (2H) | 28 (2H) | 21 (S2) | 16 (2H) | 10 (2H) | 26 (2H) |
| p06 | 24 (2H) | 28 (2H) | 22 (S2) | 12 (2H) | 14 (2H) | 30 (S2) |
| p07 | 28 (2H) | 32 (2H) | 37 (S2) | 18 (S2) | 19 (2H) | 36 (2H) |
| p08 | 34 (S2) | 38 (S2) | 29 (S2) | 29 (S2) | 27 (S2) | 38 (S2) |
| p09 | 27 (2H) | 31 (2H) | 22 (2H) | 15 (2H) | 19 (2H) | 30 (2H) |
| p10 | 31 (2H) | 35 (2H) | 23 (2H) | 19 (2H) | 8 (2H) | 30 (2H) |
| p11 | 26 (2H) | 30 (2H) | 30 (S1) | 16 (2H) | 17 (2H) | 27 (2H) |
| p12 | 34 (S2) | 38 (S2) | 33 (S2) | 31 (S2) | 24 (S2) | 38 (S2) |
| p13 | 28 (2H) | 32 (2H) | 23 (2H) | 28 (2H) | 19 (2H) | 30 (2H) |
| p14 | 26 (2H) | 30 (2H) | 31 (S2) | 13 (2H) | 19 (2H) | 29 (S2) |
| p15 | 29 (S2) | 32 (2H) | 29 (S2) | 24 (S2) | 14 (S1) | 31 (S2) |
| p16 | 30 (2H) | 34 (2H) | 24 (2H) | 15 (2H) | 12 (2H) | 32 (2H) |

### T05 Punição dos golpes defendidos

Cada golpe de cada atacante, defendido de perto; cada um dos 16 defensores tenta castigar no primeiro
quadro livre com L, 2L, M, S1, S2 ou Super (1 barra), e quem atacou defende assim que pode.
Célula: vantagem média na defesa (quadros; − é o defensor livre antes) e quantos dos 16 punem (qualquer castigo).

| atacante | L | M | H | 2L | 2M | 2H | S1 | S2 | S3 | S4 |
|---|---|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | -5 / 15 | -9 / 16 | -2 / 2 | -6 / 16 | +1 / 0 | -9 / 16 | +9 / 0 | +1 / 0 | -7 / 12 | -15 / 16 |
| Yin-lu (p02) | -4 / 13 | -8 / 16 | -2 / 2 | -4 / 13 | +1 / 0 | -4 / 13 | +15 / 0 | não defendido | -4 / 4 | -6 / 1 |
| Kōga (p03) | -5 / 15 | +1 / 0 | -2 / 2 | -5 / 14 | +1 / 0 | -4 / 13 | +3 / 0 | -10 / 16 | -4 / 13 | +3 / 0 |
| p04 | -4 / 13 | -8 / 16 | -2 / 2 | -4 / 13 | +1 / 0 | -4 / 13 | +12 / 0 | não defendido | -4 / 5 | +1 / 0 |
| p05 | -4 / 13 | -7 / 16 | -2 / 2 | -4 / 13 | +1 / 0 | -4 / 13 | +14 / 0 | -2 / 2 | -4 / 13 | -2 / 2 |
| p06 | -5 / 15 | +1 / 0 | -2 / 2 | -5 / 15 | +1 / 0 | -4 / 13 | +15 / 0 | +1 / 0 | -4 / 4 | +1 / 0 |
| p07 | -7 / 16 | +1 / 0 | -2 / 2 | -7 / 16 | +1 / 0 | -4 / 13 | +3 / 0 | -12 / 16 | -4 / 4 | +1 / 0 |
| p08 | -5 / 15 | +1 / 0 | -2 / 2 | -5 / 15 | +1 / 0 | -4 / 13 | +8 / 0 | -12 / 13 | -4 / 2 | +1 / 0 |
| p09 | -7 / 16 | +1 / 0 | -2 / 2 | -7 / 16 | +1 / 0 | -4 / 13 | +8 / 1 | +1 / 2 | não defendido | +1 / 0 |
| p10 | -4 / 13 | -7 / 16 | -2 / 2 | -4 / 13 | +1 / 0 | -4 / 13 | +12 / 0 | +1 / 0 | -4 / 7 | +1 / 0 |
| p11 | -5 / 15 | +1 / 0 | -2 / 2 | -5 / 15 | +1 / 0 | -4 / 13 | +2 / 0 | -2 / 2 | -4 / 1 | +3 / 1 |
| p12 | -5 / 15 | +1 / 0 | -2 / 2 | -5 / 15 | +1 / 0 | -4 / 13 | não defendido | +1 / 0 | -4 / 7 | -6 / 16 |
| p13 | -9 / 15 | +1 / 0 | -2 / 2 | -7 / 15 | +1 / 0 | -4 / 13 | -24 / 16 | +8 / 0 | não defendido | +1 / 1 |
| p14 | -9 / 16 | +1 / 0 | -2 / 2 | -9 / 16 | +1 / 0 | -4 / 13 | -5 / 11 | -12 / 16 | -4 / 13 | +1 / 0 |
| p15 | -5 / 15 | +1 / 0 | -2 / 2 | -5 / 14 | +1 / 0 | -4 / 13 | +0 / 0 | não defendido | não defendido | -6 / 16 |
| p16 | -5 / 15 | +1 / 0 | -2 / 2 | -5 / 15 | +1 / 0 | -4 / 13 | -2 / 1 | +91 / 0 | +1 / 0 | -6 / 16 |

Golpes do elenco punidos por pelo menos metade dos defensores, por ferramenta (L/2L, M, especial, Super):
L/2L 66, M 19, especial 10, Super 21; inseguros (qualquer) 67 de 153 golpes.

#### Golpe errado (whiff a 40 px além do alcance)

Quadros de recuperação depois do ativo; quantos dos quatro de calibração castigam (M, 2M, S1 ou Super) apertando
no fim do ativo (sem reação) / 13 quadros depois (reação humana boa).

| atacante | L | M | H | 2L | 2M | 2H |
|---|---|---|---|---|---|---|
| Ryūga (p01) | 5 q: 2 / 0 | 8 q: 4 / 0 | 17 q: 4 / 2 | 6 q: 3 / 0 | 18 q: 4 / 3 | 14 q: 4 / 3 |
| Yin-lu (p02) | 6 q: 2 / 0 | 10 q: 4 / 0 | 16 q: 4 / 1 | 6 q: 2 / 0 | 12 q: 4 / 0 | 16 q: 4 / 3 |
| Kōga (p03) | 7 q: 3 / 0 | 10 q: 4 / 0 | 17 q: 4 / 3 | 7 q: 2 / 0 | 12 q: 2 / 0 | 17 q: 4 / 3 |
| p04 | 6 q: 2 / 0 | 10 q: 3 / 0 | 16 q: 4 / 1 | 6 q: 2 / 0 | 13 q: 2 / 0 | 18 q: 4 / 3 |
| p05 | 6 q: 2 / 0 | 9 q: 3 / 0 | 15 q: 4 / 3 | 6 q: 3 / 0 | 11 q: 3 / 0 | 15 q: 4 / 3 |
| p06 | 7 q: 4 / 0 | 11 q: 3 / 0 | 18 q: 4 / 3 | 7 q: 2 / 0 | 13 q: 4 / 0 | 18 q: 4 / 3 |
| p07 | 9 q: 4 / 0 | 14 q: 3 / 1 | 22 q: 4 / 4 | 9 q: 4 / 0 | 16 q: 4 / 3 | 22 q: 4 / 4 |
| p08 | 7 q: 4 / 0 | 11 q: 4 / 0 | 18 q: 2 / 0 | 7 q: 3 / 0 | 13 q: 4 / 0 | 18 q: 4 / 3 |
| p09 | 9 q: 4 / 0 | 14 q: 4 / 1 | 23 q: 4 / 4 | 9 q: 3 / 0 | 17 q: 3 / 1 | 23 q: 4 / 4 |
| p10 | 6 q: 2 / 0 | 9 q: 3 / 0 | 15 q: 4 / 3 | 6 q: 3 / 0 | 11 q: 4 / 0 | 15 q: 4 / 2 |
| p11 | 7 q: 2 / 0 | 11 q: 4 / 0 | 18 q: 4 / 3 | 7 q: 3 / 0 | 13 q: 4 / 0 | 18 q: 4 / 3 |
| p12 | 7 q: 3 / 0 | 11 q: 3 / 0 | 18 q: 4 / 3 | 7 q: 2 / 0 | 13 q: 2 / 0 | 18 q: 4 / 3 |
| p13 | 12 q: 4 / 0 | 14 q: 4 / 1 | 22 q: 4 / 4 | 9 q: 2 / 0 | 16 q: 2 / 1 | 22 q: 4 / 4 |
| p14 | 11 q: 3 / 0 | 16 q: 4 / 2 | 24 q: 4 / 4 | 11 q: 2 / 0 | 19 q: 4 / 2 | 24 q: 4 / 4 |
| p15 | 7 q: 4 / 0 | 16 q: 4 / 3 | 34 q: 4 / 4 | 7 q: 4 / 0 | 31 q: 4 / 4 | 18 q: 4 / 3 |
| p16 | 7 q: 4 / 0 | 11 q: 4 / 0 | 18 q: 4 / 2 | 7 q: 4 / 0 | 13 q: 3 / 0 | 18 q: 4 / 3 |

### T06 Canto × centro

A CPU nível 8 ataca o Ryūga, que defende perfeito e, depois de cada sequência defendida, tenta a saída.
No centro (1200 × 1400) e no canto (o Ryūga encostado na borda esquerda). 12 lutas de 20 s por caso.
Escapou = 45 quadros sem levar nada; preso = fração do tempo a menos de 250 px em defesa ou apanhando.

| atacante | onde | dano levado / 20 s (só defende) | quebras de guarda | preso | backdash escapa | pulo p/ trás escapa | pushblock: distância ganha |
|---|---|---|---|---|---|---|---|
| Ryūga (p01) | centro | 5410 | 2.17 | 37% | 98% | 92% | 172 px |
| Ryūga (p01) | canto | 3531 | 3.00 | 49% | 96% | 91% | 177 px |
| Yin-lu (p02) | centro | 2377 | 2.75 | 35% | 95% | 98% | 203 px |
| Yin-lu (p02) | canto | 4893 | 3.08 | 53% | 94% | 100% | 204 px |
| Kōga (p03) | centro | 2661 | 3.00 | 26% | 99% | 100% | 176 px |
| Kōga (p03) | canto | 2800 | 2.92 | 36% | 99% | 100% | 184 px |
| p04 | centro | 3605 | 3.17 | 34% | 97% | 100% | 214 px |
| p04 | canto | 4300 | 3.08 | 43% | 97% | 100% | 209 px |
| p05 | centro | 1900 | 2.67 | 26% | 97% | 100% | 183 px |
| p05 | canto | 1625 | 2.67 | 40% | 95% | 99% | 182 px |
| p06 | centro | 4326 | 3.67 | 10% | 96% | 98% | 206 px |
| p06 | canto | 5151 | 4.00 | 23% | 84% | 94% | 198 px |
| p07 | centro | 61 | 0.08 | 6% | 100% | 100% | 252 px |
| p07 | canto | 847 | 0.67 | 21% | 100% | 100% | 229 px |
| p08 | centro | 7840 | 3.00 | 10% | 92% | 100% | 154 px |
| p08 | canto | 7798 | 2.92 | 21% | 91% | 100% | 162 px |
| p09 | centro | 2727 | 2.17 | 10% | 100% | 100% | 200 px |
| p09 | canto | 4258 | 3.33 | 29% | 100% | 100% | 203 px |
| p10 | centro | 2037 | 2.75 | 25% | 96% | 100% | 191 px |
| p10 | canto | 3031 | 2.92 | 44% | 98% | 100% | 186 px |
| p11 | centro | 2384 | 2.67 | 19% | 97% | 100% | 170 px |
| p11 | canto | 2697 | 2.92 | 26% | 95% | 100% | 163 px |
| p12 | centro | 2904 | 3.08 | 38% | 97% | 100% | 212 px |
| p12 | canto | 3248 | 2.92 | 41% | 100% | 100% | 211 px |
| p13 | centro | 2203 | 1.92 | 35% | 100% | 100% | 100 px |
| p13 | canto | 3043 | 2.25 | 45% | 100% | 100% | 109 px |
| p14 | centro | 605 | 1.00 | 5% | 100% | 100% | 224 px |
| p14 | canto | 1975 | 2.17 | 19% | 100% | 100% | 223 px |
| p15 | centro | 2195 | 3.58 | 27% | 100% | 100% | 186 px |
| p15 | canto | 2074 | 3.92 | 41% | 100% | 100% | 180 px |
| p16 | centro | 1760 | 2.17 | 19% | 100% | 100% | 191 px |
| p16 | canto | 1754 | 2.08 | 33% | 100% | 100% | 186 px |

### T22 Identidade da CPU

CPU nível 8, 1x1, 24 lutas por lutador contra o elenco em rodízio (metade de cada lado). Golpes iniciados por
minuto de luta, fração de especiais (S1–S4) entre eles, pulos e agarrões por minuto, distância média, tempo
colado (< 200 px) e longe (> 450 px), defesa (fração dos golpes recebidos que ela defendeu).

| lutador | golpes/min | S1 / S2 / S3 / S4 por min | Super/min | agarrões que entraram/min | pulos/min | distância média | colado / longe | defendeu | 3 golpes mais usados | mecânica própria |
|---|---|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 35 | 18.9 / 0.0 / 0.0 / 0.0 | 2.5 | 0.0 | 5.0 | 295 | 32% / 16% | 30% | S 53%, H 18%, SUPER 7% |  |
| Yin-lu (p02) | 30 | 8.3 / 0.0 / 0.0 / 0.0 | 2.7 | 0.1 | 5.4 | 240 | 45% / 7% | 28% | S 28%, M 26%, H 21% |  |
| Kōga (p03) | 32 | 9.1 / 0.0 / 0.0 / 0.0 | 1.2 | 0.0 | 7.8 | 251 | 38% / 9% | 27% | 2M 38%, S 29%, M 8% |  |
| p04 | 32 | 8.3 / 0.2 / 0.0 / 0.0 | 2.0 | 0.1 | 6.5 | 240 | 44% / 7% | 22% | S 26%, M 22%, 2M 20% |  |
| p05 | 35 | 13.7 / 0.0 / 0.0 / 0.0 | 1.0 | 0.0 | 5.9 | 269 | 38% / 12% | 27% | S 39%, M 34%, 6M 6% | teleporte 0.0/min (S3 0.0/min) |
| p06 | 40 | 25.0 / 0.0 / 0.0 / 0.0 | 0.8 | 0.0 | 5.1 | 308 | 27% / 18% | 42% | S 63%, 2M 20%, M 5% | puxão (S3) 0.0/min, sombra S1 25.0/min |
| p07 | 23 | 1.3 / 0.0 / 0.0 / 0.0 | 0.6 | 0.1 | 4.1 | 247 | 42% / 9% | 29% | M 42%, H 21%, L 8% |  |
| p08 | 44 | 31.0 / 0.0 / 0.0 / 0.0 | 2.8 | 0.0 | 4.4 | 381 | 17% / 32% | 25% | S 70%, H 18%, SUPER 6% |  |
| p09 | 33 | 8.8 / 0.0 / 0.0 / 0.6 | 1.0 | 0.2 | 5.3 | 272 | 33% / 10% | 27% | 2M 36%, S 27%, M 15% | agarrões 0.2/min (S2 cotovelada 0.0/min) |
| p10 | 40 | 12.4 / 0.0 / 0.0 / 0.0 | 0.5 | 0.1 | 6.3 | 250 | 42% / 11% | 29% | M 41%, S 31%, jM 5% |  |
| p11 | 43 | 31.6 / 0.0 / 0.0 / 0.3 | 1.9 | 0.0 | 4.4 | 361 | 23% / 32% | 28% | S 74%, 2M 6%, SUPER 4% | envenenou 0.0 vezes/min |
| p12 | 32 | 8.5 / 0.0 / 0.0 / 0.0 | 0.4 | 0.1 | 9.4 | 208 | 56% / 5% | 20% | S 26%, 2M 26%, L 17% | vida paga 1271/min |
| p13 | 26 | 7.3 / 0.0 / 0.0 / 0.0 | 0.5 | 0.1 | 5.5 | 225 | 52% / 7% | 24% | S 28%, H 24%, 2M 21% |  |
| p14 | 31 | 0.0 / 0.0 / 0.0 / 0.0 | 2.5 | 0.0 | 7.8 | 246 | 40% / 7% | 28% | 2M 30%, H 23%, M 16% |  |
| p15 | 26 | 11.7 / 0.0 / 0.0 / 0.0 | 1.2 | 0.0 | 4.4 | 270 | 34% / 10% | 27% | S 45%, H 17%, 2M 15% |  |
| p16 | 29 | 9.6 / 0.0 / 0.0 / 0.0 | 0.9 | 0.1 | 5.9 | 264 | 38% / 11% | 24% | S 33%, H 16%, 2M 15% |  |

### T23 Curva da CPU por nível

Nível N contra o nível 5 (1x1, lutadores sorteados, os mesmos dos dois lados), 80 lutas por nível,
metade de cada lado. Defesa = fração dos golpes recebidos que defendeu; combo médio = acertos por combo.

| nível | vence o nível 5 | defendeu | golpes/min | especiais (S1–S4) | combo médio | maior combo | Breakers/luta | pulos/min |
|---|---|---|---|---|---|---|---|---|
| 1 | 0% | 2% | 15 | 16% | 1.7 | 5 | 0.00 | 0.1 |
| 2 | 1% | 5% | 19 | 17% | 1.5 | 6 | 0.00 | 1.3 |
| 3 | 10% | 8% | 23 | 14% | 1.5 | 7 | 0.00 | 1.9 |
| 4 | 18% | 9% | 27 | 15% | 1.7 | 7 | 0.04 | 2.5 |
| 5 | 44% | 16% | 31 | 15% | 1.7 | 9 | 0.09 | 3.1 |
| 6 | 55% | 18% | 32 | 19% | 1.7 | 8 | 0.21 | 3.4 |
| 7 | 70% | 21% | 35 | 18% | 1.8 | 9 | 0.28 | 3.7 |
| 8 | 89% | 24% | 36 | 21% | 1.9 | 9 | 0.36 | 3.9 |

### C01 Fichas de identidade (notas 1–5 por quintil do elenco; números brutos ao lado)

| lutador | alcance | mobilidade | pressão | defesa | dano explosivo | controle de espaço | números brutos |
|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 1 | 1 | 1 | 1 | 5 | 2 | alcance M/H/2M 208/256/240 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -5.0, colado 32%; vida 14000, janela antiaérea total 131, L em 2 q; maior combo sem barra 8075; projétil 760 px/s, longe 16% |
| Yin-lu (p02) | 3 | 1 | 3 | 5 | 1 | 1 | alcance M/H/2M 264/276/220 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.5, colado 45%; vida 13440, janela antiaérea total 194, L em 3 q; maior combo sem barra 4798; projétil 700 px/s, longe 7% |
| Kōga (p03) | 2 | 1 | 4 | 2 | 2 | 4 | alcance M/H/2M 204/208/312 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 38%; vida 13720, janela antiaérea total 146, L em 3 q; maior combo sem barra 5501; projétil 950 px/s, longe 9% |
| p04 | 4 | 1 | 3 | 3 | 2 | 3 | alcance M/H/2M 260/260/288 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.5, colado 44%; vida 14420, janela antiaérea total 147, L em 3 q; maior combo sem barra 5466; projétil 800 px/s, longe 7% |
| p05 | 1 | 5 | 2 | 1 | 4 | 1 | alcance M/H/2M 276/204/220 px; anda 300, dash 207, pulo 280 px, 1 air dash, pulo na parede; vantagem média dos normais -3.3, colado 38%; vida 12880, janela antiaérea total 125, L em 3 q; maior combo sem barra 6041; projétil 1000 px/s, longe 12% |
| p06 | 2 | 1 | 2 | 1 | 3 | 3 | alcance M/H/2M 244/216/272 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 27%; vida 13300, janela antiaérea total 130, L em 3 q; maior combo sem barra 6005; projétil 480 px/s, longe 18% |
| p07 | 1 | 1 | 4 | 4 | 4 | 1 | alcance M/H/2M 264/228/192 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.0, colado 42%; vida 15120, janela antiaérea total 170, L em 4 q; maior combo sem barra 6661; projétil 700 px/s, longe 9% |
| p08 | 5 | 1 | 1 | 5 | 1 | 5 | alcance M/H/2M 284/432/200 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 17%; vida 13020, janela antiaérea total 195, L em 3 q; maior combo sem barra 4137; projétil 1100 px/s, longe 32% |
| p09 | 3 | 1 | 1 | 4 | 5 | 3 | alcance M/H/2M 276/196/304 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.0, colado 33%; vida 16100, janela antiaérea total 144, L em 4 q; maior combo sem barra 6924; projétil 600 px/s, longe 10% |
| p10 | 2 | 5 | 3 | 2 | 2 | 2 | alcance M/H/2M 284/196/252 px; anda 300, dash 207, pulo 280 px, 2 air dash; vantagem média dos normais -3.3, colado 42%; vida 13020, janela antiaérea total 146, L em 3 q; maior combo sem barra 5027; projétil 900 px/s, longe 11% |
| p11 | 1 | 1 | 1 | 1 | 1 | 5 | alcance M/H/2M 220/236/264 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 23%; vida 12600, janela antiaérea total 146, L em 3 q; maior combo sem barra 4407; projétil 950 px/s, longe 32% |
| p12 | 1 | 1 | 5 | 5 | 3 | 1 | alcance M/H/2M 264/160/296 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 56%; vida 13300, janela antiaérea total 198, L em 3 q; maior combo sem barra 5751; projétil 700 px/s, longe 5% |
| p13 | 5 | 1 | 5 | 4 | 5 | 4 | alcance M/H/2M 260/284/336 px; anda 170, dash 134, pulo 280 px, 1 air dash; vantagem média dos normais -3.3, colado 52%; vida 16100, janela antiaérea total 160, L em 4 q; maior combo sem barra 6809; projétil 600 px/s, longe 7% |
| p14 | 5 | 1 | 2 | 2 | 3 | 4 | alcance M/H/2M 256/324/332 px; anda 200, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.7, colado 40%; vida 13300, janela antiaérea total 148, L em 5 q; maior combo sem barra 5547; projétil 600 px/s, longe 7% |
| p15 | 4 | 1 | 4 | 3 | 1 | 5 | alcance M/H/2M 196/300/332 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 34%; vida 14000, janela antiaérea total 159, L em 3 q; maior combo sem barra 3650; projétil 900 px/s, longe 10% |
| p16 | 4 | 1 | 5 | 3 | 4 | 2 | alcance M/H/2M 224/268/292 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 38%; vida 14000, janela antiaérea total 147, L em 3 q; maior combo sem barra 6565; projétil 500 px/s, longe 11% |

Sinergia (assist e dupla) fica com a frente C (T17–T21).


real	0m23.988s
user	0m55.672s
sys	0m0.820s

---

## Anexo: Auditoria SŌKON — frente B (T07, T08, T09, T10, T11, T13, T14)

Medido no motor de verdade (`CombatEngine`) com cenários fixos (sem sorteio): mesma entrada, mesmo resultado.
Código: `android/app/src/test/java/com/gamelutagpt/BateriaCombos.java` (`java ... com.gamelutagpt.BateriaCombos [t07|t08|t09|t10|t11|t13|t14|tudo]`,
reaproveita `BateriaBalanco`). Estado medido: commit `42677571` (combate igual ao `50c9955d`; as classes usadas são de antes das
mudanças locais em `CombatConfig`/`CombatEngine`/`AiController` que estão no working tree). Boneco/defensor: Ryūga (`player_base`).

Ressalvas gerais: as rotas vêm de uma busca em feixe (largura 40, até 14–16 passos) sobre L/M/H, agachados, S1 (botão M), Super,
assist, pulo, super pulo, dash, golpes no ar e esperas — é um **piso** (a melhor rota real pode ser maior), não o teto. O boneco não
defende nos combos, mas cai de pé no ar (tech) assim que pode. S2–S4 não entram na busca de combos (só no T11). O Ultra entra com o
dano cheio do `ultra.json` (4000 quando não há arquivo), sem a nota do toque da página.

---

ID: T07
STATUS: REPRODUZIDO
ARQUIVO E LINHAS: `GeneratedCharacters.java` (2M de todos com `launch(KNOCKDOWN)`); `CombatEngine.java:3945-3955` (derrubada encerra a sessão)
CENÁRIO: 16 lutadores × 7 começos (L, M, H, 2M a 140 px; pulo a 330 px; assist a 260 px; antiaéreo com o rival pulando de 320 px), 0 barra, sem Super na busca.
RESULTADO:
- Combos de 2.100 a 6.900 sem barra; a maioria dos lutadores fica entre 4.500 e 6.500 em qualquer começo.
- **Rotas distintas: 4,5 de 7 começos em média**, mas o "distinto" é de conjunto de golpes; o esqueleto é o mesmo: das 93 rotas,
  86 usam assist, 88 usam S1, 80 usam j2H (quique no chão) e 70 usam "2H → pulo" (lançador + perseguição). Quase todos seguem
  *normal → assist → 2H → pulo → jH/jM → S → j2H → de novo*.
- **2M não abre combo em ninguém**: é rasteira (KNOCKDOWN) nos 19 pacotes; a derrubada encerra a sessão e o cancelamento em H
  (declarado em 16 pacotes) nunca acerta depois (0 de 16).
- Começos fracos: p09 com L/M/H (3.400–3.950, 5–6 acertos), p11 com L/H (2.100–2.600), p15 com L/M/H (3.000–3.750); o antiaéreo
  é o melhor começo de p09 (5.943) e p11 (4.873).
- Assist como começo falha ou rende pouco em p11 e p16 (o assist não acerta a 260 px no tempo), p13 (2.702) e p09 (2.418).
IMPACTO: identidade de combo fraca — os 16 compartilham a mesma rota-base; a diferença está no dano por golpe, não no caminho.
ALTERNATIVAS: manter (rota universal = fácil de aprender); dar rotas próprias (cancelamentos exclusivos, S2–S4 como extensores,
lançadores diferentes); limitar o j2H/2H a parte do elenco.
RECOMENDAÇÃO: DECIDIR. Se a meta é identidade, começar pelos três que divergem (p09, p11, p15) e dar a cada um um extensor próprio,
em vez de mexer no esqueleto comum. Decidir também o que o 2M deve ser: se é rasteira, tirar o `cancelInto("H")` que não faz nada;
se deve emendar, trocar o KNOCKDOWN.
DEPENDÊNCIAS: T10, T18.

---

ID: T08
STATUS: REPRODUZIDO
ARQUIVO E LINHAS: `CombatRules.superGainOnHit` (especial 450 por acerto); `CombatConfig` (`superFloorPermille 500`, `recoverableLifePermille 270`); `CombatEngine.java:525,589` (Ultra só do neutro, no chão)
CENÁRIO: começo livre a 140 px, 0/1/3 barras, vida real do boneco (14.000). Ultra e DHC entram na busca; para cada barra fica o
maior entre a busca com tudo, sem Ultra/DHC, a rota de menos barra e a rota + Ultra em link.
RESULTADO:
- 0 barra: 4.100–8.200 (30–59%). 1 barra: 5.300–9.500 (38–68%). 3 barras: igual a 1 barra em 12 dos 16; só Ryūga (11.926 = 85%,
  5 Supers), p14 (7.535) e poucos ganham.
- **A barra se paga dentro do combo**: rotas começadas com 0 barra usam 1–2 Supers (o especial dá 450 por acerto). Ryūga com 3
  barras gasta 5,0 barras num só combo.
- **Ultra não emenda no fim de combo longo** (só sai do neutro e no chão); como abertura (H → Ultra, S H Ultra) dá 5.600–5.900.
- Mesmo começo L M H: Super 1.375–4.778; Super + DHC soma de 0 a +1.750 e **às vezes perde** (p06 −1.386, p14 −1.946, p10 −520:
  a troca corta os acertos que faltavam do primeiro Super); L M H + Ultra (link) 3.670–5.734 e é o melhor fecho em todos os 16.
- Vida vermelha = 27% do dano (config); 1.161–3.205 por combo. Posição final: a maioria a 800–1.200 px da parede (o combo não leva ao canto); a rota do p14 termina no canto.
IMPACTO: barras 2 e 3 quase não viram dano em combo (exceto Ultra de abertura e Ryūga); DHC pode ser armadilha.
ALTERNATIVAS: menos barra por acerto dentro do combo; DHC esperar o último acerto do Super; Ultra poder cancelar de golpe com acerto.
RECOMENDAÇÃO: DECIDIR. Primeiro, ganho de barra dentro do combo (é o que torna 0 barra = 59% e Ryūga = 85%); depois, o DHC
não cortar o primeiro Super.
DEPENDÊNCIAS: T10, T20, F02.

---

ID: T09
STATUS: REPRODUZIDO
ARQUIVO E LINHAS: `CombatConfig` (`guardDrainPerBlockstunFrame 10`, `pushblockDistance 190`, `justDefenseBlockstunPermille 400`)
CENÁRIO: cada golpe (L, M, H, 2L, 2M, 2H, S) defendido pelo Ryūga na altura certa, encostado; e sequências L M H, 2L 2M 2H, L L L, M H S
apertando o próximo o mais cedo possível, com e sem pushblock no 1º bloqueio.
RESULTADO:
- Vantagem na defesa: L −4 a −9, M −9 a +1, H −2 em todos, 2L −4 a −9, 2M +1 em todos, 2H −4 a −9, S1 de −24 a +15. 39 golpes positivos
  no elenco; entre os normais nenhum passa de +1 (M e 2M); o mais positivo é o S1 da Yin-lu (+15). Depois de cada golpe a distância é 112–250 px.
- Sequências: 81 de 118 emendas sem buraco (≤ 1 quadro) — as cadeias L M H e L L L são verdadeiras; 2L 2M 2H tem buraco (2–12 q) e o 2H
  sai do alcance em quase todos. No fim: L M H −2 (p13 −18), 2L 2M 2H −27 a −48 (punível), L L L +1/+2 (p13 −16), M H S de 0 a +17 na maioria (S1 fecha seguro;
  p13 −24 e p14 −49 ficam puníveis).
- Guarda gasta por sequência: 140–700 de 1.000 (p09/p15 M H S = 700).
- Pushblock no 1º bloqueio: o resto da sequência erra (1–3 defendidos) e a distância vai a 340–620 px.
IMPACTO: pressão é cadeia + fecho com S1 seguro (+3 a +15); não há loop. Pushblock é uma saída completa.
ALTERNATIVAS: manter; deixar o S1 de longe menos positivo; dar ao 2H alcance de emendar.
RECOMENDAÇÃO: manter como está; observar só o fecho com S1 muito positivo (Yin-lu +15, p06 +16 no T11, p05 +14) se a pressão
parecer forte no aparelho.
DEPENDÊNCIAS: T11, T14.

---

ID: T10
STATUS: REPRODUZIDO
ARQUIVO E LINHAS: `CombatConfig` (`shinkaiDamagePermille 1250`, `shinkaiSuperDamagePermille 1200`, `shinkaiEnergyMeterPerFrame 4`, `superFloorPermille 500`, `juggleLimit MAX`)
CENÁRIO: 1 barra no início; busca pelo maior dano e, à parte, pela maior duração; sem SHINKAI e com cada tipo forçado (ligado antes do 1º golpe).
RESULTADO:
- Sem SHINKAI: maior 9.475 (Ryūga, 68%); média do maior por lutador 5.530 (40%).
- Com SHINKAI: média POWER 7.358 (53%), SPEED 7.556 (54%), ENERGY 7.604 (55%), TECHNIQUE 8.023 (58%).
- **Um toque mata**: Ryūga ENERGY 15.526 (112% da vida média), Ryūga POWER/TECHNIQUE/SPEED 97–100%, p09 TECHNIQUE 13.997 (101%), p09
  POWER 13.522 (97%), p07 SPEED/TECHNIQUE 83–84%, p14 74–78%. Todas usam 3–5 Supers no mesmo combo
  (rota do 112%: `SUPER S H SUPER dash M SUPER espera espera S SUPER espera espera assist H SUPER`).
- Duração: maior 10,3 s (p06 SPEED); nenhum combo passa de ~10 s; nenhum infinito (o boneco sempre cai de pé ou é derrubado).
IMPACTO: o SHINKAI (+25% e Super grátis ×1,2) junto com a barra que o combo gera e o piso de 50% do Super faz o combo de um toque
passar da vida inteira em Ryūga e p09.
ALTERNATIVAS: limitar Supers por combo (ex.: 2); piso do Super menor no 3º Super; o Super grátis do SHINKAI não somar o ×1,2 com o ×1,25;
menos barra por acerto dentro do combo.
RECOMENDAÇÃO: DECIDIR, com prioridade alta: limitar Supers por combo é o ajuste mais localizado (corta todas as rotas >85%).
Não declarar "infinito": não há.
DEPENDÊNCIAS: T08, T15, F03, F05.

---

ID: T11
STATUS: REPRODUZIDO
ARQUIVO E LINHAS: comandos de `CharacterDefinition.specials` / `fighter.energyCommand` (como `ComandosPorPersonagemTest`)
CENÁRIO: cada S1–S4 (botão M) a 120 px: no acerto (sem defesa), na defesa (altura certa; o atacante defende assim que fica livre)
e no erro (rival a 1400 px). Punível = o golpe mais rápido do Ryūga (L, 2L, M, 2M, H, S, Super) que acerta antes da defesa.
RESULTADO (64 especiais):
- Na defesa: 30 positivos, 23 negativos, 11 não tocam de perto (contra-golpes, agarrões e golpes que passam por cima:
  Yin-lu S2–S4, p04 S2, p05 S3, p08 S3, p09 S3, p11 S3, p12 S1, p13 S3, p15 S2).
- Puníveis: 12 com L, 10 só com Super, 31 sem punição.
- S1 de projétil é seguro e positivo de perto em quase todos (+8 a +16: Ryūga, Yin-lu, p04, p05, p06, p08, p09, p10).
- Muito negativos: p13 S1 −24 (L pune), Ryūga S4 −15, p07 S2 / p08 S2 / p14 S2 −12, p03 S2 −10.
- Fora da curva: p16 S2 (banana) +91 na defesa; p15 S3 +139 no acerto (derruba); 4 especiais não acertam de perto
  (p10 S4, p13 S3, p15 S2, p16 S2).
- Duração no erro: 18 q (Ryūga S1) a 110 q (p13 S1).
IMPACTO: a maioria dos especiais tem fraqueza coerente; o S1 de projétil seguro de perto e a banana +91 são os que fogem.
ALTERNATIVAS: manter; tirar parte da vantagem do S1 de perto (mais recuperação, menos blockstun); rever a banana.
RECOMENDAÇÃO: HIPÓTESE para a banana (+91 pode ser armadilha que fica na tela — confirmar no aparelho antes de mexer); S1 seguro:
DECIDIR junto com T09.
DEPENDÊNCIAS: T09, T02.

---

ID: T13
STATUS: CONFIRMADO / REPRODUZIDO
ARQUIVO E LINHAS: `CombatEngine.java:1947-1961` (aperto de trás, janela e bloqueio de 20 q), `3814-3836` (efeito)
CENÁRIO: Ryūga parado aperta trás de −8 a +2 quadros do contato; golpes L, M, H, 2M e S1 de Ryūga, Kōga, p09, p11 e p15.
RESULTADO:
- Janela de 6 quadros em todos os 25 casos (−5 a 0): −8 a −6 = defesa normal, +1 e +2 = acerto.
- Blockstun cai a 40% (ex.: H do p09 26 → 10; 2M da p15 35 → 14). Vida de volta: +168 (1,2% de 14.000). Não gasta guarda.
- Em sequência (oráculo perfeito, soltando e apertando a cada golpe): **só 1 defesa perfeita por cadeia** (a p15 consegue 2 no L M H),
  porque um novo aperto não vale até 20 q depois do anterior.
IMPACTO: justa e acessível em golpe isolado; em cadeias rápidas só a primeira conta.
ALTERNATIVAS: manter; baixar o bloqueio entre apertos (20 → ~12) para valer em cadeias; avisar visualmente o bloqueio.
RECOMENDAÇÃO: manter. Se quiserem defesa perfeita em pressão, testar o bloqueio em 12 q depois de jogar no aparelho.
DEPENDÊNCIAS: T14, T24.

---

ID: T14
STATUS: REPRODUZIDO
ARQUIVO E LINHAS: `CombatConfig` (guarda 1000, gasto blockstun × 10, volta 2/q após 120 q, quebra = 50 q abertos e guarda cheia); `CombatEngine.java:3839-3852`
CENÁRIO: Ryūga com a defesa sempre certa (recuando, como quem segura trás) contra L, H, L M H em loop, projétil, Super e assist + golpes,
de 7 atacantes; com só defesa, pushblock a cada bloqueio (barra sobrando) e defesa perfeita por oráculo; 30 s.
RESULTADO:
- Só defendendo: quebra em 2,2–11,4 s (a maioria 3–7 s, 4–13 defesas) contra quase tudo; p13 é o mais lento (7–19 s).
- Pushblock: a quebra passa a 11–20 s (Super em loop ainda quebra em ~3 s) e o projétil não quebra mais (o pushblock afasta e a pressão recomeça de longe).
- Defesa perfeita: contra H em loop nunca quebra (não gasta guarda); contra L e L M H quase não muda (o bloqueio de 20 q deixa só 1 por cadeia).
- Super em loop (barra infinita) quebra em 2,6–3,3 s e pune com 3.500–5.200 (Ryūga, p09).
- Punição depois da quebra (L M H S): 300–2.655.
- Recuperação: 10,3 s do zero até cheia (2 s de espera + 8,3 s).
IMPACTO: só defender não é saída (como já visto na bateria); pushblock é a resposta, e custa 0,25 barra.
ALTERNATIVAS: manter; guarda gastar menos com golpes leves; quebra só depois da sequência acabar.
RECOMENDAÇÃO: manter. O desenho (≈10 defesas) está coerente com o pushblock como saída. Ensinar o pushblock no tutorial.
DEPENDÊNCIAS: T09, T13, F01.

---

### Tabelas completas

### T07 Diversidade de rotas (0 barra, sem Super)

Melhor rota (busca em feixe, largura 40, até 14 passos, sem Super) por começo. O boneco cai de pé no ar assim que pode.
Começos: L, M, H, 2M a 140 px; pulo a 330 px; assist a 260 px; antiaéreo com o rival pulando de 320 px.

| lutador | L | M | H | 2M | pulo | assist | antiaéreo | rotas distintas |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 5420 (13) | 5848 (12) | 4900 (10) | derruba (500, nada emenda) | 5878 (12) | 5785 (11) | 5951 (12) | 3 de 6 |
| Yin-lu (p02) | 5078 (13) | 5367 (12) | 5420 (12) | derruba (450, nada emenda) | 5408 (12) | 5580 (12) | 5571 (12) | 4 de 6 |
| Kōga (p03) | 5535 (13) | 5738 (12) | 5614 (11) | derruba (500, nada emenda) | 5951 (12) | 5952 (11) | 6006 (12) | 4 de 6 |
| p04 | 5010 (12) | 6015 (12) | 6157 (12) | derruba (520, nada emenda) | 6194 (12) | 5935 (11) | 6098 (11) | 4 de 6 |
| p05 | 4651 (13) | 5112 (11) | 5299 (12) | derruba (450, nada emenda) | 4554 (10) | 4731 (11) | 4515 (9) | 6 de 6 |
| p06 | 4573 (13) | 5193 (13) | 4791 (12) | derruba (420, nada emenda) | 4977 (12) | 4596 (11) | 4977 (12) | 5 de 6 |
| p07 | 5834 (11) | 6875 (12) | 6673 (12) | derruba (600, nada emenda) | 6507 (11) | 6657 (11) | 6906 (11) | 3 de 6 |
| p08 | 3134 (7) | 5780 (12) | 5683 (12) | derruba (480, nada emenda) | 5521 (11) | 3040 (5) | 4802 (10) | 6 de 6 |
| p09 | 3401 (6) | 3612 (5) | 3950 (6) | derruba (620, nada emenda) | 4405 (7) | 2418 (5) | 5943 (10) | 6 de 6 |
| p10 | 5021 (13) | 5748 (13) | 5494 (12) | derruba (420, nada emenda) | 5416 (12) | 5206 (11) | 5146 (12) | 4 de 6 |
| p11 | 2121 (6) | 4433 (11) | 2615 (6) | derruba (400, nada emenda) | 3122 (7) | – | 4873 (11) | 4 de 5 |
| p12 | 5671 (13) | 6497 (13) | 6710 (13) | derruba (550, nada emenda) | 6297 (12) | 6180 (11) | 6456 (12) | 5 de 6 |
| p13 | 5843 (15) | 6441 (14) | 6809 (15) | derruba (600, nada emenda) | 6471 (15) | 2702 (7) | 5292 (11) | 6 de 6 |
| p14 | 4394 (15) | 4932 (15) | – | derruba (480, nada emenda) | 4967 (15) | 4338 (10) | 4967 (15) | 2 de 5 |
| p15 | 3477 (7) | 3038 (5) | 3758 (8) | derruba (500, nada emenda) | 5377 (10) | 5536 (11) | 5569 (11) | 6 de 6 |
| p16 | 4578 (11) | 5084 (10) | 5515 (11) | derruba (500, nada emenda) | 5326 (12) | – | 5456 (12) | 4 de 5 |

Média de rotas distintas por lutador: 4.5 de 7 começos.

Rotas:

- Ryūga (p01), L: L H assist 2H pulo jH S jH S j2H jM H assist 2H → 5420
- Ryūga (p01), M: M S H pulo 2H assist pulo jM jH S jH j2H S H → 5848
- Ryūga (p01), H: H M assist 2H pulo jH S j2H jM jH 2L L → 4900
- Ryūga (p01), pulo: pulo M H assist 2H pulo jH S jH S j2H jM H assist → 5878
- Ryūga (p01), assist: assist M H pulo 2H pulo jH S jH jM j2H H assist 2H → 5785
- Ryūga (p01), antiaéreo: 2H S H assist 2H pulo jH jM S jH j2H M H assist → 5951
- Yin-lu (p02), L: L H assist 2H pulo jH S jH j2H jH H S assist S → 5078
- Yin-lu (p02), M: M assist 2H pulo jM jH j2H jH j2H S H M H assist → 5367
- Yin-lu (p02), H: H assist M H pulo M 2H pulo jH S jH j2H assist j2H → 5420
- Yin-lu (p02), pulo: pulo espera M H assist 2H pulo jH S jH j2H S H 2H → 5408
- Yin-lu (p02), assist: assist 2M H pulo M 2H pulo jH S jH j2H assist H 2H → 5580
- Yin-lu (p02), antiaéreo: 2H S H assist 2H pulo jH S jH j2H S H 2H assist → 5571
- Kōga (p03), L: L H assist 2H pulo jH S jH jM j2H M H assist S → 5535
- Kōga (p03), M: M assist H pulo M 2H pulo jH S jM jH jM j2H H → 5738
- Kōga (p03), H: H 2L assist 2H pulo jM jH jM jH j2H S 2H pulo j2H → 5614
- Kōga (p03), pulo: pulo M H assist 2H pulo jH S jH jM j2H M H assist → 5951
- Kōga (p03), assist: assist pulo jH j2H H pulo 2H pulo jH S jH j2H M H → 5952
- Kōga (p03), antiaéreo: 2H 2M H assist 2H pulo jH S jH jM j2H M H assist → 6006
- p04, L: L M assist M 2H pulo jM jH j2H jH j2H 2M H assist → 5010
- p04, M: M 2H assist pulo jM jH j2H jH j2H 2M H M H S → 6015
- p04, H: H assist M H pulo 2H pulo jH jM jH jM j2H S 2H → 6157
- p04, pulo: pulo M H assist 2H pulo jH S jH jM j2H H M 2H → 6194
- p04, assist: assist pulo M S H pulo 2H pulo jH jM jH j2H jH 2H → 5935
- p04, antiaéreo: 2M H S 2H assist pulo jM jH j2H jH j2H 2M H 2H → 6098
- p05, L: L 2L assist H 2H pulo jH S jH S j2H S H assist → 4651
- p05, M: M 2H pulo jM jH j2H S jH j2H H 2H pulo j2H assist → 5112
- p05, H: H assist 2H pulo jM jH j2H S jH j2H jL H M 2H → 5299
- p05, pulo: pulo espera M pulo S j2H assist 2H pulo jM jH j2H jH j2H → 4554
- p05, assist: assist pulo jM jM j2H H pulo M 2H pulo jH jM S jH → 4731
- p05, antiaéreo: 2M H S 2H pulo jM jH S espera assist S j2H jM 2H → 4515
- p06, L: L H assist 2H pulo jH jM jH jM j2H jH H assist S → 4573
- p06, M: M H assist 2H pulo jH jM jH jM j2H jH H assist S → 5193
- p06, H: H 2L assist H pulo M 2H pulo jH jM jH jM j2H assist → 4791
- p06, pulo: pulo M H assist 2H pulo jH jM jH jM j2H jH H assist → 4977
- p06, assist: assist pulo 2L S H pulo 2H pulo jH jM jH jM j2H jH → 4596
- p06, antiaéreo: 2H 2M H assist 2H pulo jH jM jH jM j2H jH H assist → 4977
- p07, L: L 2H assist pulo jM jH S jH j2H H S 2H pulo j2H → 5834
- p07, M: M H assist dash 2H pulo jH jM jH jM j2H H S 2H → 6875
- p07, H: H assist 2L 2H pulo jM jH S j2H jH M H S assist → 6673
- p07, pulo: pulo H 2L 2H assist pulo jM jH S jH j2H H S 2H → 6507
- p07, assist: assist 2H pulo jM jH j2H jM j2H H S 2H pulo jH j2H → 6657
- p07, antiaéreo: 2H assist pulo jH j2H jH j2H H S 2H pulo jM S j2H → 6906
- p08, L: L S M S H 2M S H → 3134
- p08, M: M S H pulo 2H assist pulo jH S jH jM j2H S H → 5780
- p08, H: H 2L 2H assist pulo jM jH j2H jH j2H H S M 2H → 5683
- p08, pulo: pulo M 2H assist pulo jM jH j2H jH j2H H S M H → 5521
- p08, assist: assist H S H S 2L S → 3040
- p08, antiaéreo: S pulo jM j2H S M H assist H dash M H → 4802
- p09, L: L M H S assist S → 3401
- p09, M: M H S dash S assist → 3612
- p09, H: H 2L H S assist S → 3950
- p09, pulo: pulo H 2L M H S dash S assist → 4405
- p09, assist: assist 2L M S dash dash dash dash pulo jL → 2418
- p09, antiaéreo: 2L H S 2H pulo jM jH espera S j2H jL jM assist L → 5943
- p10, L: L H assist 2H pulo jH S jH j2H jH H M H assist → 5021
- p10, M: M assist H 2H pulo jH S jH j2H jH H S 2H assist → 5748
- p10, H: H assist M 2H pulo jM jH j2H jH S j2H S H assist → 5494
- p10, pulo: pulo assist jM jH j2H H M S 2H pulo jH S jH assist → 5416
- p10, assist: assist pulo jH j2H S 2H pulo jM jH j2H jH j2H S assist → 5206
- p10, antiaéreo: S pulo j2H jH assist S 2H pulo jH S jM jH S j2H → 5146
- p11, L: L M 2L H S 2L S → 2121
- p11, M: M 2H pulo jM jH S j2H jL 2H pulo jH S jH j2H → 4433
- p11, H: H 2L M H S 2L S → 2615
- p11, pulo: pulo espera M assist M H S 2L S → 3122
- p11, antiaéreo: 2H H assist M 2H pulo jM jH j2H jH j2H H S 2H → 4873
- p12, L: L M S H assist 2H pulo jH S jH jM j2H 2M 2H → 5671
- p12, M: M S H assist 2H pulo jH S jM jH jM j2H 2M 2H → 6497
- p12, H: H S M S H assist 2H pulo jH jM jH j2H jH 2H → 6710
- p12, pulo: pulo espera M S H assist 2H pulo jH S jH j2H S 2H → 6297
- p12, assist: assist dash S S H pulo 2H pulo jH jM jH j2H jH 2H → 6180
- p12, antiaéreo: 2H H S M S H assist 2H pulo jH S jH j2H 2H → 6456
- p13, L: L H pulo M 2H pulo jH jM jH jM j2H 2M H S → 5843
- p13, M: M 2H pulo jM jH jM j2H jH 2M H assist H S → 6441
- p13, H: H H pulo 2H pulo jM jH jM jH j2H 2H pulo jH S → 6809
- p13, pulo: pulo H 2L M 2H pulo jM jH jM j2H jH 2M H S → 6471
- p13, assist: assist M 2L H 2L S → 2702
- p13, antiaéreo: 2M superpulo jH jM j2H jH H assist H S → 5292
- p14, L: L H pulo M assist pulo j2H jH 2H pulo jH jM jH S → 4394
- p14, M: M H pulo M assist pulo j2H jH 2H pulo jH jM jH S → 4932
- p14, pulo: pulo M H assist pulo j2H jH 2H pulo jM jH jM jH S → 4967
- p14, assist: assist 2L M H dash pulo 2H pulo jH jM jH jM j2H S → 4338
- p14, antiaéreo: 2L M H assist pulo j2H jH 2H pulo jM jH jM jH S → 4967
- p15, L: L 2H assist pulo jM jH S j2H jL jH → 3477
- p15, M: M H dash S assist S → 3038
- p15, H: H 2L L M H assist S dash S → 3758
- p15, pulo: pulo espera M H assist S dash pulo j2H jH 2H pulo jH j2H → 5377
- p15, assist: assist pulo j2H j2H M 2H pulo jL jH j2H jH j2H H assist → 5536
- p15, antiaéreo: 2L assist H S 2L 2H pulo jM jH j2H jH j2H H assist → 5569
- p16, L: L M 2H pulo jM jH j2H jH j2H 2M S espera assist H → 4578
- p16, M: M 2H pulo jM jH j2H jH j2H 2M S espera assist H → 5084
- p16, H: H assist 2H pulo jM jH j2H jH j2H 2M 2H pulo jH S → 5515
- p16, pulo: pulo M H assist 2H pulo jH jM jH jM j2H jH M S → 5326
- p16, antiaéreo: 2H 2M H assist 2H pulo jH jM jH jM j2H jH M S → 5456


### T08 Dano por recurso (0, 1 e 3 barras; Ultra e DHC entram na busca)

Começo livre (L/M/H/2L/2M a 140 px), largura 40, até 16 passos. Vida real do boneco (14000) para medir a vida vermelha;
o Ultra entra com o dano cheio do ultra.json (sem a nota do toque). Barra gasta conta a que o combo gerou.

| lutador | 0 barra | 1 barra | 3 barras | rota com 3 barras | vida vermelha (3 b.) | barra gasta (3 b.) | distância da parede no fim | Ultra / DHC usados |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 8202 (59%) | 9475 (68%) | 11926 (85%) | SUPER H SUPER dash S H SUPER espera espera assist S SUPER espera espera S SUPER | 3205 | 5.0 | 1058 px | nenhum |
| Yin-lu (p02) | 4798 (34%) | 9145 (65%) | 9145 (65%) | M 2H DHC SUPER espera espera H SUPER dash pulo j2H S j2H S SUPER | 2456 | 3.0 | 1178 px |  DHC |
| Kōga (p03) | 5319 (38%) | 5319 (38%) | 5611 (40%) | H DHC ULTRA | 1512 | 3.0 | 833 px | Ultra DHC |
| p04 | 5466 (39%) | 5846 (42%) | 5846 (42%) | S M S SUPER espera espera espera espera espera DHC | 1575 | 2.0 | 1167 px |  DHC |
| p05 | 6041 (43%) | 6041 (43%) | 6041 (43%) | H assist 2H pulo jM jH j2H S jM j2H S jH H SUPER | 1625 | 1.0 | 834 px | nenhum |
| p06 | 6005 (43%) | 6005 (43%) | 6005 (43%) | M H assist 2H pulo jH jM jH jM jH j2H 2M H SUPER | 1614 | 1.0 | 816 px | nenhum |
| p07 | 7237 (52%) | 7237 (52%) | 7237 (52%) | H assist 2L M 2H pulo jM jH SUPER espera espera espera espera espera espera j2H | 1949 | 2.0 | 975 px | nenhum |
| p08 | 4137 (30%) | 5979 (43%) | 5979 (43%) | S M S SUPER DHC espera espera espera espera espera espera pulo H 2M pulo jH | 1611 | 3.0 | 880 px |  DHC |
| p09 | 7423 (53%) | 8874 (63%) | 8874 (63%) | H S SUPER espera espera espera espera espera 2L M S SUPER | 2390 | 2.0 | 1098 px | nenhum |
| p10 | 5027 (36%) | 7900 (56%) | 7900 (56%) | M DHC SUPER espera espera espera espera espera S SUPER | 2128 | 2.0 | 1057 px |  DHC |
| p11 | 4407 (31%) | 5996 (43%) | 5996 (43%) | H S SUPER espera espera espera espera espera espera H S SUPER | 1616 | 2.0 | 960 px | nenhum |
| p12 | 5751 (41%) | 5751 (41%) | 5937 (42%) | S H ULTRA | 1600 | 3.0 | 451 px | Ultra |
| p13 | 7031 (50%) | 7326 (52%) | 7326 (52%) | H H pulo 2H pulo jM jH jM jH j2H M L H SUPER DHC | 1970 | 2.0 | 317 px |  DHC |
| p14 | 5737 (41%) | 6489 (46%) | 7535 (54%) | M M H SUPER espera espera espera H SUPER espera espera espera H SUPER | 2023 | 3.0 | 0 px | nenhum |
| p15 | 6076 (43%) | 6076 (43%) | 6076 (43%) | assist 2H pulo jM jH j2H jH j2H H S SUPER | 1637 | 1.0 | 869 px | nenhum |
| p16 | 6565 (47%) | 6565 (47%) | 6565 (47%) | H assist 2H pulo jM jH j2H jH j2H 2L S SUPER | 1767 | 1.0 | 873 px | nenhum |

Mesmo começo (L M H) fechando de três jeitos (3 barras no início):

| lutador | L M H SUPER | L M H SUPER DHC | L M H + Ultra (link) | melhor fecho |
|---|---|---|---|---|
| Ryūga (p01) | 3820 | 3998 (+178) | 4587 | Ultra |
| Yin-lu (p02) | 2117 | 3517 (+1400) | 4123 | Ultra |
| Kōga (p03) | 2192 | 3087 (+895) | 4584 | Ultra |
| p04 | 2300 | 2914 (+614) | 4816 | Ultra |
| p05 | 1971 | 2623 (+652) | 4128 | Ultra |
| p06 | 2555 | 1169 (+-1386) | 4229 | Ultra |
| p07 | 4223 | não emenda | 5502 | Ultra |
| p08 | 1886 | 1886 (+0) | 4355 | Ultra |
| p09 | 4778 | não emenda | 5734 | Ultra |
| p10 | 1689 | 1169 (+-520) | 3899 | Ultra |
| p11 | 3058 | não emenda | 3670 | Ultra |
| p12 | 2185 | 3470 (+1285) | 5044 | Ultra |
| p13 | 1804 | 3554 (+1750) | 5505 | Ultra |
| p14 | 3624 | 1678 (+-1946) | 4356 | Ultra |
| p15 | 1375 | 1375 (+0) | 4586 | Ultra |
| p16 | 1375 | 1375 (+0) | 4587 | Ultra |


### T09 Pressão defendida

Vantagem na defesa de cada golpe (quadros; + o atacante age primeiro) e a distância depois; defensor Ryūga com a defesa certa.
Guarda gasta = blockstun × 10 (GUARD_MAX 1000; CombatConfig.guardDrainPerBlockstunFrame).

| atacante | L | M | H | 2L | 2M | 2H | S | positivos |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | -5 / 133px / g90 | -9 / 156px / g120 | -2 / 179px / g190 | -6 / 124px / g90 | +1 / 132px / g220 | -9 / 144px / g100 | +9 / 175px / g140 | 2 |
| Yin-lu (p02) | -4 / 146px / g100 | -8 / 169px / g140 | -2 / 177px / g170 | -4 / 126px / g100 | +1 / 134px / g160 | -4 / 146px / g160 | +15 / 188px / g250 | 2 |
| Kōga (p03) | -5 / 149px / g110 | +1 / 153px / g140 | -2 / 180px / g190 | -5 / 125px / g110 | +1 / 133px / g160 | -4 / 145px / g180 | +3 / 191px / g140 | 3 |
| p04 | -4 / 140px / g100 | -8 / 163px / g140 | -2 / 171px / g170 | -4 / 120px / g100 | +1 / 128px / g170 | -4 / 140px / g190 | +12 / 186px / g240 | 2 |
| p05 | -4 / 158px / g100 | -7 / 177px / g120 | -2 / 189px / g160 | -4 / 138px / g100 | +1 / 146px / g140 | -4 / 158px / g150 | +14 / 197px / g230 | 2 |
| p06 | -5 / 154px / g110 | +1 / 158px / g150 | -2 / 185px / g200 | -5 / 130px / g110 | +1 / 138px / g170 | -4 / 150px / g190 | +13 / 159px / g250 | 3 |
| p07 | -7 / 172px / g140 | +1 / 176px / g190 | -2 / 203px / g250 | -7 / 144px / g140 | +1 / 152px / g210 | -4 / 164px / g250 | +3 / 195px / g250 | 3 |
| p08 | -5 / 158px / g110 | +1 / 162px / g150 | -2 / 189px / g200 | -5 / 134px / g110 | +1 / 142px / g170 | -4 / 154px / g190 | +8 / 200px / g200 | 3 |
| p09 | -7 / 162px / g140 | +1 / 166px / g190 | -2 / 193px / g260 | -7 / 134px / g140 | +1 / 142px / g220 | -4 / 154px / g260 | +8 / 211px / g250 | 3 |
| p10 | -4 / 132px / g100 | -7 / 151px / g120 | -2 / 163px / g160 | -4 / 112px / g100 | +1 / 120px / g140 | -4 / 132px / g150 | +12 / 171px / g210 | 2 |
| p11 | -5 / 156px / g110 | +1 / 160px / g150 | -2 / 187px / g200 | -5 / 132px / g110 | +1 / 140px / g170 | -4 / 152px / g190 | +2 / 198px / g140 | 3 |
| p12 | -5 / 147px / g110 | +1 / 151px / g150 | -2 / 178px / g200 | -5 / 123px / g110 | +1 / 131px / g170 | -4 / 143px / g190 | +7 / 136px / g250 | 3 |
| p13 | -9 / 178px / g170 | +1 / 171px / g190 | -2 / 198px / g250 | -7 / 139px / g140 | +1 / 147px / g210 | -4 / 159px / g250 | -24 / 189px / g40 | 2 |
| p14 | -9 / 165px / g170 | +1 / 158px / g220 | -2 / 188px / g280 | -9 / 126px / g170 | +1 / 134px / g250 | -4 / 146px / g270 | – | 2 |
| p15 | -5 / 150px / g110 | +1 / 158px / g200 | -2 / 192px / g360 | -5 / 126px / g110 | +1 / 134px / g350 | -4 / 146px / g190 | +0 / 207px / g140 | 2 |
| p16 | -5 / 142px / g110 | +1 / 146px / g150 | -2 / 173px / g200 | -5 / 118px / g110 | +1 / 126px / g170 | -4 / 138px / g190 | -2 / 250px / g140 | 2 |

Golpes positivos na defesa no elenco: 39; o mais positivo: Yin-lu (p02) S (+15).

Sequências apertando o próximo golpe o mais cedo possível (buracos = quadros livres de quem defende entre duas defesas; 0 = sem buraco):

| atacante | sequência | defendidos | buracos | vantagem no fim | distância no fim | guarda gasta | com pushblock no 1º: defendidos / distância |
|---|---|---|---|---|---|---|---|
| Ryūga (p01) | L M H | 3 | [1, 0] | -2 | 214 px | 310 | 1 / 485 px |
| Ryūga (p01) | 2L 2M 2H | 2 | [9] | -28 | 266 px | 220 | 1 / 420 px |
| Ryūga (p01) | L L L | 3 | [0, 0] | +1 | 152 px | 180 | 2 / 338 px |
| Ryūga (p01) | M H S | 3 | [6, 0] | +10 | 252 px | 330 | 1 / 460 px |
| Yin-lu (p02) | L M H | 3 | [1, 0] | -2 | 227 px | 310 | 1 / 483 px |
| Yin-lu (p02) | 2L 2M 2H | 2 | [2] | -29 | 268 px | 160 | 1 / 426 px |
| Yin-lu (p02) | L L L | 3 | [0, 0] | +2 | 169 px | 200 | 2 / 351 px |
| Yin-lu (p02) | M H S | 3 | [4, 0] | +15 | 251 px | 420 | 1 / 458 px |
| Kōga (p03) | L M H | 3 | [2, 0] | -2 | 230 px | 330 | 1 / 504 px |
| Kōga (p03) | 2L 2M 2H | 2 | [3] | -32 | 282 px | 160 | 1 / 436 px |
| Kōga (p03) | L L L | 3 | [0, 0] | +2 | 168 px | 220 | 1 / 409 px |
| Kōga (p03) | M H S | 3 | [0, 0] | +3 | 253 px | 470 | 2 / 468 px |
| p04 | L M H | 3 | [1, 0] | -2 | 221 px | 310 | 1 / 477 px |
| p04 | 2L 2M 2H | 2 | [3] | -33 | 281 px | 170 | 1 / 435 px |
| p04 | L L L | 3 | [0, 0] | +2 | 163 px | 200 | 2 / 345 px |
| p04 | M H S | 3 | [4, 0] | +12 | 245 px | 410 | 1 / 470 px |
| p05 | L M H | 3 | [1, 0] | -2 | 239 px | 280 | 1 / 480 px |
| p05 | 2L 2M 2H | 2 | [2] | -27 | 273 px | 140 | 1 / 431 px |
| p05 | L L L | 3 | [0, 0] | +2 | 181 px | 200 | 1 / 414 px |
| p05 | M H S | 3 | [4, 0] | +14 | 266 px | 390 | 2 / 466 px |
| p06 | L M H | 3 | [2, 0] | -2 | 235 px | 350 | 1 / 513 px |
| p06 | 2L 2M 2H | 2 | [3] | -33 | 291 px | 170 | 1 / 445 px |
| p06 | L L L | 3 | [0, 0] | +2 | 173 px | 220 | 1 / 414 px |
| p06 | M H S | 3 | [0, 0] | +17 | 258 px | 600 | 1 / 440 px |
| p07 | L M H | 3 | [3, 0] | -2 | 252 px | 440 | 1 / 571 px |
| p07 | 2L 2M 2H | 3 | [6, 9] | -48 | 283 px | 460 | 1 / 488 px |
| p07 | L L L | 3 | [0, 0] | +2 | 195 px | 280 | 1 / 453 px |
| p07 | M H S | 3 | [0, 0] | +3 | 276 px | 690 | 1 / 612 px |
| p08 | L M H | 3 | [2, 0] | -2 | 239 px | 350 | 2 / 471 px |
| p08 | 2L 2M 2H | 2 | [3] | -33 | 295 px | 170 | 1 / 449 px |
| p08 | L L L | 3 | [0, 0] | +2 | 177 px | 220 | 1 / 418 px |
| p08 | M H S | 3 | [0, 0] | +8 | 262 px | 550 | 3 / 411 px |
| p09 | L M H | 3 | [3, 0] | -2 | 242 px | 450 | 1 / 568 px |
| p09 | 2L 2M 2H | 2 | [4] | -43 | 331 px | 220 | 1 / 485 px |
| p09 | L L L | 3 | [0, 0] | +2 | 185 px | 280 | 1 / 443 px |
| p09 | M H S | 3 | [0, 0] | +8 | 266 px | 700 | 1 / 528 px |
| p10 | L M H | 3 | [1, 0] | -2 | 213 px | 280 | 2 / 400 px |
| p10 | 2L 2M 2H | 3 | [2, 7] | -36 | 225 px | 290 | 1 / 405 px |
| p10 | L L L | 3 | [0, 0] | +2 | 155 px | 200 | 2 / 337 px |
| p10 | M H S | 3 | [4, 0] | +12 | 240 px | 370 | 2 / 440 px |
| p11 | L M H | 3 | [2, 0] | -2 | 237 px | 350 | 1 / 518 px |
| p11 | 2L 2M 2H | 2 | [3] | -33 | 293 px | 170 | 1 / 447 px |
| p11 | L L L | 3 | [0, 0] | +2 | 175 px | 220 | 2 / 357 px |
| p11 | M H S | 3 | [0, 0] | +2 | 260 px | 490 | 2 / 479 px |
| p12 | L M H | 2 | [2] | -18 | 236 px | 150 | 1 / 502 px |
| p12 | 2L 2M 2H | 2 | [3] | -33 | 284 px | 170 | 1 / 438 px |
| p12 | L L L | 3 | [0, 1] | -16 | 177 px | 220 | 2 / 381 px |
| p12 | M H S | 3 | [0, 0] | +7 | 139 px | 600 | 1 / 418 px |
| p13 | L M H | 3 | [2, 0] | -2 | 247 px | 440 | 1 / 566 px |
| p13 | 2L 2M 2H | 2 | [4] | -41 | 329 px | 210 | 1 / 483 px |
| p13 | L L L | 3 | [0, 0] | +2 | 190 px | 340 | 1 / 467 px |
| p13 | M H S | 7 | [0, 0, 0, 0, 0, 0] | -24 | 187 px | 560 | 2 / 302 px |
| p14 | L M H | 3 | [3, 0] | -2 | 238 px | 500 | 1 / 589 px |
| p14 | 2L 2M 2H | 2 | [4] | -45 | 331 px | 250 | 1 / 485 px |
| p14 | L L L | 3 | [0, 0] | +2 | 188 px | 340 | 1 / 465 px |
| p14 | M H S | 2 | [0] | – | 256 px | 500 | 1 / 476 px |
| p15 | L M H | 3 | [4, 0] | -2 | 238 px | 560 | 1 / 622 px |
| p15 | 2L 2M 2H | 3 | [12, 9] | -42 | 247 px | 540 | 1 / 316 px |
| p15 | L L L | 3 | [0, 0] | +2 | 169 px | 220 | 1 / 410 px |
| p15 | M H S | 3 | [0, 0] | +0 | 258 px | 700 | 1 / 476 px |
| p16 | L M H | 3 | [2, 0] | -2 | 223 px | 350 | 1 / 501 px |
| p16 | 2L 2M 2H | 3 | [3, 9] | -42 | 239 px | 360 | 1 / 433 px |
| p16 | L L L | 3 | [0, 0] | +2 | 161 px | 220 | 1 / 402 px |
| p16 | M H S | 3 | [0, 31] | -49 | 349 px | 490 | 1 / 523 px |

Emendas sem buraco (≤ 1 quadro) entre defesas: 81 de 118.


### T10 Combos extremos (assist, quiques, launcher, wall chase, repetição; SHINKAI forçado)

Busca em feixe (largura 40, até 16 passos, 1 barra no início) pelo maior dano e, à parte, pela maior duração.
Com SHINKAI: o atacante liga o tipo forçado antes do primeiro golpe (todos ganham +25% de dano; SPEED repete o golpe pelo eco).

| lutador | sem SHINKAI | POWER | SPEED | ENERGY | TECHNIQUE | maior duração (tipo) | rota mais longa |
|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 9475 (68%, P1/C1, A) | 13893 (100%, P1/C0, A) | 13473 (97%, P1/C0, A) | 15526 (112%, P1/C0, A) | 13802 (99%, P1/C0, A) | 9.0 s, 7 acertos (SPEED) | H pulo 2H SUPER espera espera 2M 2H 2L 2M 2H 2L 2M 2H SUPER |
| Yin-lu (p02) | 4269 (31%, A) | 5455 (39%, A) | 5467 (39%, A) | 5987 (43%, A) | 5793 (42%, A) | 7.4 s, 19 acertos (ENERGY) | 2L H 2H pulo jM S jH j2H S H 2H pulo jH jM jH jM |
| Kōga (p03) | 4696 (34%, A) | 6546 (47%, A) | 5939 (43%, A) | 6546 (47%, A) | 7033 (51%, P1/C0, A) | 8.3 s, 19 acertos (SPEED) | H pulo M L 2H pulo jM jH jM jH j2H jH 2H espera SUPER |
| p04 | 4880 (35%) | 6813 (49%) | 6197 (45%, A) | 6898 (50%) | 6642 (48%) | 8.9 s, 22 acertos (SPEED) | H pulo M H 2H pulo jH S jH j2H H 2H pulo jH jM jH |
| p05 | 4629 (33%, A) | 5789 (42%, A) | 5907 (43%, A) | 5789 (42%, A) | 5690 (41%) | 8.2 s, 12 acertos (sem) | H 2L H pulo M 2H pulo jH S jH j2H jH H 2H pulo jH |
| p06 | 3988 (29%, A) | 5094 (37%, A) | 5099 (37%, A) | 5094 (37%, A) | 5419 (39%, A) | 10.3 s, 18 acertos (SPEED) | H pulo 2H pulo jH jM jH j2H S jH 2H pulo S jH jM SUPER |
| p07 | 6270 (45%, P1/C0, A) | 7811 (56%, P1/C0, A) | 11557 (83%, P1/C0, A) | 5891 (42%, P1/C0, A) | 11715 (84%, P1/C0, A) | 8.7 s, 11 acertos (SPEED) | 2H assist SUPER espera espera espera espera espera espera pulo H pulo 2H pulo jH SUPER |
| p08 | 4339 (31%) | 5665 (41%) | 5614 (40%) | 5665 (41%) | 6015 (43%) | 8.3 s, 15 acertos (TECHNIQUE) | H M H pulo H 2H pulo jH jM jH jM jH j2H assist j2H SUPER |
| p09 | 8874 (64%) | 13522 (97%) | 11827 (85%) | 13055 (94%, P1/C0, A) | 13997 (101%) | 7.9 s, 11 acertos (ENERGY) | S M SUPER espera espera espera espera espera 2M SUPER espera espera espera espera espera SUPER |
| p10 | 4289 (31%, A) | 5939 (43%, A) | 6022 (43%, A) | 5939 (43%, A) | 5780 (42%, A) | 7.9 s, 12 acertos (sem) | M H pulo 2H pulo jH jM jH j2H jH 2M 2H pulo jH jM jH |
| p11 | 5996 (43%, P1/C0) | 7960 (57%, P1/C0) | 7506 (54%, P1/C0, A) | 7607 (55%, P1/C0) | 8071 (58%, P1/C0) | 8.8 s, 20 acertos (SPEED) | H 2L 2H pulo jH S jH j2H M S 2H pulo jH jM jH SUPER |
| p12 | 5266 (38%, A) | 7202 (52%) | 6979 (50%) | 7252 (52%) | 6979 (50%) | 9.1 s, 11 acertos (sem) | S S H pulo 2H pulo jH jM jH j2H jH H 2H espera assist SUPER |
| p13 | 7031 (51%, P1/C1) | 4551 (33%, P1/C0) | 8495 (61%, P1/C1) | 4073 (29%, P1/C0) | 8585 (62%, P1/C1) | 10.1 s, 14 acertos (sem) | H H pulo 2H espera H 2H pulo jH jM j2H jH 2H pulo jH S |
| p14 | 6489 (47%, P1/C1, A) | 10230 (74%, P1/C0, A) | 6571 (47%, P1/C1, A) | 10729 (77%, P1/C0) | 10785 (78%, P1/C1) | 9.2 s, 12 acertos (sem) | 2L H pulo 2H pulo jH jM jH j2H jH 2H pulo dash assist H 2H |
| p15 | 3840 (28%) | 5360 (39%, P1/C0, A) | 8544 (61%, P1/C0, A) | 8319 (60%, P1/C1, A) | 5738 (41%, A) | 8.4 s, 18 acertos (SPEED) | H pulo 2H pulo jH jM jH j2H S jM 2H pulo jH jM jH jM |
| p16 | 4155 (30%, A) | 5895 (42%, P1/C0) | 5700 (41%, P1/C0) | 7292 (52%, P1/C0, A) | 6328 (46%) | 8.4 s, 15 acertos (TECHNIQUE) | H S H pulo M 2H pulo jH jM jH S jH j2H S jH SUPER |

Média do maior dano por tipo: sem SHINKAI 5530 (40%); POWER 7358 (53%); SPEED 7556 (54%); ENERGY 7604 (55%); TECHNIQUE 8023 (58%);
Maior dano: 15526 (112%) — Ryūga (p01) ENERGY: SUPER S H SUPER dash M SUPER espera espera S SUPER espera espera assist H SUPER.
Maior duração: 10.3 s — p06 (SPEED): H pulo 2H pulo jH jM jH j2H S jH 2H pulo S jH jM SUPER.
Vida média do elenco: 13895.


### T11 Risco dos especiais (S1–S4, botão M)

Cada especial contra o Ryūga a 120 px: no acerto (sem defesa), na defesa (defesa certa) e no erro (rival a 1400 px).
Vantagem: quadros que o atacante fica livre antes do rival (+ bom para quem atacou). Punível: o golpe mais rápido do Ryūga
(L, 2L, M, 2M, H, S, Super) que, apertado na primeira chance depois da defesa, acerta.

| lutador | especial | golpe | dano no acerto | vantagem no acerto | vantagem na defesa | distância na defesa | punível com | duração no erro |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | S1 | S | 850 | +15 | +9 | 211 px | nada | 18 q |
| Ryūga (p01) | S2 | S2 | 1100 | +46 (derruba) | +1 | 186 px | nada | 41 q |
| Ryūga (p01) | S3 | S3 | 900 | +1 | -7 | 269 px | SUPER | 42 q |
| Ryūga (p01) | S4 | S4 (vários acertos) | 1000 | +60 (derruba) | -15 | 187 px | L | 44 q |
| Yin-lu (p02) | S1 | S | 760 | +21 | +15 | 222 px | nada | 26 q |
| Yin-lu (p02) | S2 | S2 | 990 | +53 (derruba) | não toca | – | – | 32 q |
| Yin-lu (p02) | S3 | S3 | 810 | +4 | não toca | – | – | 36 q |
| Yin-lu (p02) | S4 | S4 (vários acertos) | 900 | +58 (derruba) | não toca | – | – | 35 q |
| Kōga (p03) | S1 | S | 850 | +9 | +3 | 226 px | nada | 28 q |
| Kōga (p03) | S2 | S2 (vários acertos) | 1100 | +65 (derruba) | -10 | 163 px | L | 35 q |
| Kōga (p03) | S3 | S3 (vários acertos) | 900 | +4 | -4 | 143 px | L | 38 q |
| Kōga (p03) | S4 | S4 | 1000 | +66 (derruba) | +6 | 152 px | nada | 31 q |
| p04 | S1 | S | 890 | +18 | +12 | 226 px | nada | 29 q |
| p04 | S2 | S2 | 1160 | +50 (derruba) | não toca | – | – | 36 q |
| p04 | S3 | S3 | 940 | +4 | -4 | 254 px | SUPER | 40 q |
| p04 | S4 | S4 | 1050 | +52 (derruba) | +1 | 96 px | nada | 38 q |
| p05 | S1 | S | 760 | +20 | +14 | 219 px | nada | 24 q |
| p05 | S2 | S2 | 990 | +20 | -2 | 232 px | SUPER | 31 q |
| p05 | S3 | S3 | 810 | +4 | não toca | – | – | 32 q |
| p05 | S4 | S4 (vários acertos) | 900 | +73 (derruba) | -2 | 161 px | SUPER | 32 q |
| p06 | S1 | S | 720 | +19 | +16 | 207 px | nada | 29 q |
| p06 | S2 | S2 | 940 | +32 (derruba) | +1 | 199 px | nada | 58 q |
| p06 | S3 | S3 | 760 | +4 | -4 | 270 px | SUPER | 40 q |
| p06 | S4 | S4 | 850 | +52 (derruba) | +1 | 107 px | nada | 38 q |
| p07 | S1 | S | 1020 | +57 (derruba) | +3 | 211 px | nada | 35 q |
| p07 | S2 | S2 (vários acertos) | 1320 | +60 (derruba) | -12 | 237 px | L | 46 q |
| p07 | S3 | S3 | 1080 | +4 | -4 | 273 px | SUPER | 49 q |
| p07 | S4 | S4 | 1200 | +44 (derruba) | +1 | 206 px | nada | 48 q |
| p08 | S1 | S | 810 | +14 | +8 | 248 px | nada | 29 q |
| p08 | S2 | S2 (vários acertos) | 1040 | +58 (derruba) | -12 | 341 px | SUPER | 36 q |
| p08 | S3 | S3 | 860 | +4 | não toca | – | – | 40 q |
| p08 | S4 | S4 | 950 | +52 (derruba) | +1 | 110 px | nada | 38 q |
| p09 | S1 | S | 1060 | +14 | +8 | 259 px | nada | 37 q |
| p09 | S2 | S2 | 1380 | +41 (derruba) | +1 | 186 px | nada | 47 q |
| p09 | S3 | S3 | 1120 | +4 | não toca | – | – | 51 q |
| p09 | S4 | S4 | 1250 | +44 (derruba) | +1 | 240 px | nada | 49 q |
| p10 | S1 | S | 720 | +18 | +12 | 236 px | nada | 24 q |
| p10 | S2 | S2 | 940 | +54 (derruba) | +1 | 186 px | nada | 31 q |
| p10 | S3 | S3 | 760 | +4 | -4 | 247 px | SUPER | 33 q |
| p10 | S4 | S4 | – | não acerta de perto | +1 | 88 px | nada | 32 q |
| p11 | S1 | S | 680 | +8 | +2 | 226 px | nada | 29 q |
| p11 | S2 | S2 | 880 | +20 | -2 | 236 px | SUPER | 36 q |
| p11 | S3 | S3 | 720 | +4 | não toca | – | – | 40 q |
| p11 | S4 | S4 | 800 | +52 (derruba) | +4 | 300 px | nada | 38 q |
| p12 | S1 | S | 943 | +13 | não toca | – | – | 29 q |
| p12 | S2 | S2 | 1218 | +50 (derruba) | +1 | 320 px | nada | 36 q |
| p12 | S3 | S3 | 996 | +4 | -4 | 254 px | SUPER | 40 q |
| p12 | S4 | S4 (vários acertos) | 1106 | +65 (derruba) | -6 | 171 px | L | 38 q |
| p13 | S1 | S (vários acertos) | 1320 | +51 (derruba) | -24 | 189 px | L | 110 q |
| p13 | S2 | S2 | 1320 | +20 | +4 | 304 px | nada | 57 q |
| p13 | S3 | S3 | – | não acerta de perto | não toca | – | – | 72 q |
| p13 | S4 | S4 | 1200 | +46 (derruba) | +1 | 176 px | nada | 68 q |
| p14 | S1 | S (no ar) | 1140 | +35 (derruba) | -6 | 153 px | L | 33 q |
| p14 | S2 | S2 (vários acertos) | 1040 | +20 | -12 | 261 px | L | 60 q |
| p14 | S3 | S3 | 900 | +15 | -4 | 221 px | L | 44 q |
| p14 | S4 | S4 | 950 | +44 (derruba) | +1 | 214 px | nada | 53 q |
| p15 | S1 | S | 850 | +6 | +0 | 241 px | nada | 35 q |
| p15 | S2 | S2 | – | não acerta de perto | não toca | – | – | 64 q |
| p15 | S3 | S3 (vários acertos) | 900 | +139 (derruba) | -4 | 155 px | L | 100 q |
| p15 | S4 | S4 (vários acertos) | 1000 | +59 (derruba) | -6 | 179 px | L | 72 q |
| p16 | S1 | S | 850 | +4 | -2 | 292 px | nada | 49 q |
| p16 | S2 | S2 | – | não acerta de perto | +91 | 468 px | nada | 41 q |
| p16 | S3 | S3 | 900 | +28 (derruba) | +1 | 123 px | nada | 96 q |
| p16 | S4 | S4 (vários acertos) | 1000 | +56 (derruba) | -6 | 113 px | L | 63 q |


### T13 Defesa perfeita (aperto de trás de −8 a +2 quadros do contato)

O Ryūga fica parado e aperta trás N quadros antes (−) ou depois (+) do contato. P = defesa perfeita, D = defesa normal, X = levou.
Config: janela 5 quadros, bloqueio de 20 quadros entre apertos, blockstun 40%, vida +1,2% da máxima (CombatConfig.justDefense*).

| atacante | golpe | -8 | -7 | -6 | -5 | -4 | -3 | -2 | -1 | +0 | +1 | +2 | blockstun normal → perfeita | vida de volta |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | L | D | D | D | P | P | P | P | P | P | X | X | 9 → 3 | +168 |
| Ryūga (p01) | M | D | D | D | P | P | P | P | P | P | X | X | 12 → 4 | +168 |
| Ryūga (p01) | H | D | D | D | P | P | P | P | P | P | X | X | 19 → 7 | +168 |
| Ryūga (p01) | 2M | D | D | D | P | P | P | P | P | P | X | X | 22 → 8 | +168 |
| Ryūga (p01) | S | D | D | D | P | P | P | P | P | P | X | X | 14 → 5 | +168 |
| Kōga (p03) | L | D | D | D | P | P | P | P | P | P | X | X | 11 → 4 | +168 |
| Kōga (p03) | M | D | D | D | P | P | P | P | P | P | X | X | 14 → 5 | +168 |
| Kōga (p03) | H | D | D | D | P | P | P | P | P | P | X | X | 19 → 7 | +168 |
| Kōga (p03) | 2M | D | D | D | P | P | P | P | P | P | X | X | 16 → 6 | +168 |
| Kōga (p03) | S | D | D | D | P | P | P | P | P | P | X | X | 14 → 5 | +168 |
| p09 | L | D | D | D | P | P | P | P | P | P | X | X | 14 → 5 | +168 |
| p09 | M | D | D | D | P | P | P | P | P | P | X | X | 19 → 7 | +168 |
| p09 | H | D | D | D | P | P | P | P | P | P | X | X | 26 → 10 | +168 |
| p09 | 2M | D | D | D | P | P | P | P | P | P | X | X | 22 → 8 | +168 |
| p09 | S | D | D | D | P | P | P | P | P | P | X | X | 25 → 10 | +168 |
| p11 | L | D | D | D | P | P | P | P | P | P | X | X | 11 → 4 | +168 |
| p11 | M | D | D | D | P | P | P | P | P | P | X | X | 15 → 6 | +168 |
| p11 | H | D | D | D | P | P | P | P | P | P | X | X | 20 → 8 | +168 |
| p11 | 2M | D | D | D | P | P | P | P | P | P | X | X | 17 → 6 | +168 |
| p11 | S | D | D | D | P | P | P | P | P | P | X | X | 14 → 5 | +168 |
| p15 | L | D | D | D | P | P | P | P | P | P | X | X | 11 → 4 | +168 |
| p15 | M | D | D | D | P | P | P | P | P | P | X | X | 20 → 8 | +168 |
| p15 | H | D | D | D | P | P | P | P | P | P | X | X | 36 → 14 | +168 |
| p15 | 2M | D | D | D | P | P | P | P | P | P | X | X | 35 → 14 | +168 |
| p15 | S | D | D | D | P | P | P | P | P | P | X | X | 14 → 5 | +168 |

Janela média: 6.0 quadros.

Defesa perfeita em sequência (oráculo: solta no começo de cada golpe e aperta 2 quadros antes do contato):

| atacante | sequência | defesas | perfeitas |
|---|---|---|---|
| Ryūga (p01) | L M H | 3 | 1 |
| Ryūga (p01) | L L L | 3 | 1 |
| Kōga (p03) | L M H | 2 | 1 |
| Kōga (p03) | L L L | 3 | 1 |
| p09 | L M H | 2 | 1 |
| p09 | L L L | 3 | 1 |
| p11 | L M H | 3 | 1 |
| p11 | L L L | 3 | 1 |
| p15 | L M H | 3 | 2 |
| p15 | L L L | 3 | 1 |


### T14 Quebra de guarda (defesa sempre certa)

Config: guarda 1000, gasto = blockstun × 10 (meio nas batidas antes da última), volta 2/quadro depois de 120 quadros sem defender (cheia em 10.3 s);
quebrou: aberto 50 quadros e a guarda volta cheia. Tempo até quebrar (s) / golpes defendidos / dano da punição L M H S.

| atacante | defesa | L | H | L M H | projétil | Super | TAG |
|---|---|---|---|---|---|---|---|
| Ryūga (p01) | só defende | 3.7 s / 13 / 2025 | 6.9 s / 6 / 300 | 4.5 s / 9 / 1375 | 5.3 s / 8 / 850 | 2.6 s / 9 / 5177 | 4.1 s / 8 / 2025 |
| Ryūga (p01) | pushblock | 18.4 s / 15 / 2025 | 14.9 s / 8 / 2025 | 16.4 s / 10 / 2025 | não quebra (3) | 2.7 s / 9 / 3840 | 11.6 s / 9 / 760 |
| Ryūga (p01) | defesa perfeita | 3.7 s / 13 / 2025 | não quebra (31) | 11.2 s / 17 / 2025 | não quebra (5) | 2.8 s / 11 / 4206 | 10.8 s / 16 / 1818 |
| Kōga (p03) | só defende | 4.4 s / 11 / 1375 | 7.5 s / 6 / 727 | 5.9 s / 8 / 1375 | não quebra (4) | 6.2 s / 8 / 1068 | 3.7 s / 7 / 727 |
| Kōga (p03) | pushblock | 15.6 s / 12 / 1375 | 15.1 s / 8 / 1375 | 16.1 s / 10 / 1375 | não quebra (1) | 17.0 s / 9 / 727 | 11.7 s / 9 / 300 |
| Kōga (p03) | defesa perfeita | 4.4 s / 11 / 1375 | não quebra (31) | 11.0 s / 14 / 727 | não quebra (0) | 8.7 s / 11 / 1068 | 10.9 s / 15 / 764 |
| p09 | só defende | 3.5 s / 9 / 910 | 4.3 s / 4 / 910 | 4.0 s / 6 / 910 | 2.8 s / 4 / 1060 | 3.0 s / 4 / 3536 | 4.0 s / 7 / 910 |
| p09 | pushblock | 15.1 s / 10 / 910 | 14.0 s / 6 / 2530 | 13.6 s / 7 / 910 | não quebra (3) | 3.2 s / 4 / 2667 | 14.1 s / 9 / 2530 |
| p09 | defesa perfeita | 11.0 s / 20 / 910 | não quebra (32) | 12.2 s / 14 / 910 | 4.0 s / 4 / 1060 | 3.3 s / 6 / 910 | 10.2 s / 12 / 1506 |
| p11 | só defende | 4.2 s / 11 / 1620 | 5.7 s / 5 / 582 | 5.6 s / 8 / 582 | 4.8 s / 8 / 680 | 5.2 s / 3 / 582 | 8.7 s / 9 / 800 |
| p11 | pushblock | 15.5 s / 12 / 1620 | 13.6 s / 7 / 1620 | 16.2 s / 10 / 1620 | não quebra (4) | 5.7 s / 3 / 680 | 13.9 s / 9 / 800 |
| p11 | defesa perfeita | 4.2 s / 11 / 1620 | não quebra (30) | 11.1 s / 14 / 1620 | 6.5 s / 8 / 680 | 9.5 s / 5 / 680 | 17.6 s / 20 / 943 |
| p05 | só defende | 5.2 s / 11 / 1237 | 8.6 s / 7 / 1237 | 6.1 s / 9 / 1237 | 2.3 s / 5 / 760 | 4.1 s / 6 / 1237 | 10.9 s / 10 / 760 |
| p05 | pushblock | 16.4 s / 12 / 1237 | 15.9 s / 9 / 1237 | 15.7 s / 10 / 1237 | não quebra (4) | 17.4 s / 7 / 1920 | 17.7 s / 10 / 619 |
| p05 | defesa perfeita | 5.2 s / 11 / 1237 | não quebra (34) | 15.8 s / 25 / 1237 | 3.3 s / 5 / 760 | 4.1 s / 6 / 1237 | 13.8 s / 15 / 1237 |
| p13 | só defende | 11.9 s / 12 / 2655 | 14.3 s / 7 / 2655 | 12.3 s / 9 / 873 | 19.0 s / 42 / 2655 | 7.0 s / 6 / 2253 | 14.1 s / 9 / 760 |
| p13 | pushblock | 19.1 s / 7 / 2655 | 20.3 s / 6 / 2655 | 18.4 s / 6 / 2655 | não quebra (38) | 17.8 s / 8 / 2655 | 19.1 s / 6 / 1040 |
| p13 | defesa perfeita | 13.8 s / 15 / 2655 | não quebra (23) | 19.9 s / 20 / 2655 | 24.3 s / 50 / 2252 | não quebra (23) | 19.3 s / 17 / 1305 |
| p16 | só defende | 4.4 s / 11 / 2025 | 5.1 s / 5 / 2025 | 4.6 s / 8 / 2025 | 11.4 s / 8 / 850 | 3.5 s / 4 / 2025 | 2.2 s / 7 / 2025 |
| p16 | pushblock | 15.6 s / 12 / 2025 | 13.5 s / 7 / 2025 | 15.6 s / 9 / 2025 | não quebra (3) | não quebra (2) | 14.9 s / 9 / 2025 |
| p16 | defesa perfeita | 4.4 s / 11 / 2025 | não quebra (31) | 10.4 s / 14 / 2025 | 9.3 s / 8 / 850 | não quebra (4) | 14.9 s / 20 / 2025 |



---

## Anexo: Auditoria SŌKON, frente C: Breaker, SHINKAI, assistências, TAG, DHC e duplas

Commit base: `42677571` (combate idêntico a `50c9955d`; só entraram a bateria e o relatório). Tudo medido no
`CombatEngine` real, CPU nível 8, sementes fixas. Código: `android/app/src/test/java/com/gamelutagpt/BateriaEquipe.java`
(`java ... com.gamelutagpt.BateriaEquipe [t12|t15|t16|t17|t18|t19|t20|t21|tudo]`, ~1 min). Tabelas completas no fim.
Medido sobre as classes compiladas às 18:06 (código sem nenhuma mudança de balanço); as edições em CombatConfig/CombatEngine/AiController que apareceram depois na árvore de trabalho não são desta frente e não entram nestes números.
Ressalva geral: quem joga é a CPU; números de vitória têm ruído de ±4 (240 lutas) a ±7 pontos (56 lutas).

---

ID: F01 / T12 Combo Breaker
STATUS: REPRODUZIDO · DECIDIR
ARQUIVO E LINHAS: CombatConfig.java:410–423 (2 acertos, 1 barra, 44 q de atordoamento, 40 q de corrida); CombatEngine.java:1968 (`canBreakCombo`)
CENÁRIO: 16 atacantes × 10 sementes num boneco com 1 barra que só quebra; quebra no 2º, 3º, 4º acerto e no ar, no meio e no canto. A/B em 240 lutas 2x2 espelhadas "sempre quebra × nunca quebra" com três regras.
RESULTADO:
- Quebra no 2º acerto (meio): leva 836, evita 805, troco 1418; o troco vira combo de ≥900 em 56% das vezes. No 3º: evita 545, troco 1131. No 4º: evita 206, troco 913. No ar: evita 472, troco 819.
- No canto o troco cai (821 no 2º acerto, 618 no ar): quem batia é empurrado contra a parede, mas a corrida automática não o alcança tão bem.
- A/B: atual (44 q + corrida 40 q) troco 1558, "sempre quebra" vence 73%; meio-termo (24 q, sem corrida) troco 580, 68%; volta ao neutro (12 q, sem corrida) troco 693, 65%. Controle nunca × nunca nas mesmas sementes: 45%.
IMPACTO: todos os lutadores; a quebra continua vantajosa mesmo sem troco (o dano evitado basta: 65% em "volta ao neutro"). O troco garantido é ~2/3 do valor atual.
ALTERNATIVAS: (a) manter; (b) volta ao neutro (stun 12, sem corrida): corta o troco pela metade e mantém a quebra útil; (c) meio-termo 24 q sem corrida; (d) custar 2 barras ou exigir 3 acertos (não medido aqui).
RECOMENDAÇÃO: DECIDIR entre (b) e (c); os dois deixam o Breaker como defesa e tiram o "combo de graça". (b) é o que mais se aproxima de "restaurar o neutro" pedido no F01.
TESTES DE REGRESSÃO: `BateriaEquipe t12`; ComboBreakerTest precisará de ajuste se a corrida sair.

---

ID: F02 F03 / T15 Valor dos quatro SHINKAI
STATUS: REPRODUZIDO
ARQUIVO E LINHAS: TeamSystem.java:328 (ENERGY +4/quadro); CombatConfig.java:452–475 (bônus); 468 (eco 50%)
CENÁRIO: 1x1 espelhado, 16 lutadores × 15 sementes; A liga no GO (tipo forçado), B (mesma CPU) sem SHINKAI; janela de 8 s + brilho. "sem" = mesma janela sem ligar.
RESULTADO (saldo de dano A−B na janela, a mais que sem SHINKAI): POWER +3478, SPEED +2904, TECHNIQUE +1689, ENERGY +1454.
- Barra ganha por A: ENERGY 2,97 barras (1,88 da passiva; a conta de 1,92 confere, o relógio para nos congelamentos), POWER 1,18, SPEED 1,27, TECHNIQUE 0,84. ENERGY sai da janela com ~2 barras sobrando — valor que o saldo de 8 s não mostra (≈ um Super a mais, ~1400–3000 de dano).
- Supers de A na janela: 1,0–1,2 (o Super grátis) em todos; TECHNIQUE 0,74.
- Vida vermelha que volta: 224–476 (ENERGY maior porque apanha mais).
- Distância média: SPEED fica 30 px mais perto (282 px); os outros ~315 px.
- F03 eco do SPEED: +439 de dano em 8 s (+13%) contra o eco sem dano, nas mesmas sementes.
IMPACTO: POWER e SPEED rendem mais dano direto; ENERGY troca dano por barra; TECHNIQUE rende pouco nos 8 s, mas domina o Clash (bateria anterior).
ALTERNATIVAS: manter (perfis diferentes); ou baixar o POWER (couraça 70%) se o objetivo for saldos parecidos.
RECOMENDAÇÃO: manter como perfis; a correção mais urgente do SHINKAI continua sendo o Clash (TECHNIQUE vence 95–100%). O eco do SPEED não é excessivo (13%).
TESTES DE REGRESSÃO: `BateriaEquipe t15`; ShinkaiTest.

---

ID: T16 Resposta contra SHINKAI
STATUS: REPRODUZIDO
CENÁRIO: A (CPU 8) liga no GO; B responde por 8 s; 16 lutadores × 6 sementes; meio e canto. Valores = dano que B leva.
RESULTADO (meio): sem SHINKAI de A, a CPU de B leva 2148. Contra SHINKAI: só CPU 2566–4497; defesa perfeita 1074–1337; fugir (trás + backdash) 984–1135; agarrão de perto 1115–1600; parado 5942–6676; TAG na hora 1145–1584 (e a voadora dá 209–632).
- No canto: defesa perfeita 1233–2094, fugir 1429–2202; o SPEED é o mais difícil (2094 defendendo, 2,7× o sem SHINKAI).
- Existe resposta reproduzível para os quatro tipos: defender ou fugir reduz o dano a ~1/3 do que a CPU leva jogando normal. A CPU de B não usa essas respostas (leva 2,5–4,5 mil).
IMPACTO: nenhum tipo é incontestável; o canto contra SPEED é o caso mais duro.
ALTERNATIVAS: manter; ensinar a resposta (tutorial: "defenda/afaste"); a CPU passar a defender/fugir quando o rival liga.
RECOMENDAÇÃO: manter as regras; ajustar a CPU (ela troca golpe com o SHINKAI e perde) e citar a resposta no tutorial.
TESTES DE REGRESSÃO: `BateriaEquipe t16`.

---

ID: F06 / T17 Função das assistências
STATUS: CONFIRMADO (código) · REPRODUZIDO (medida)
ARQUIVO E LINHAS: CombatEngine.java:2136–2151 (`startAssistMove`: "S" = o S1/energia do pacote); CombatConfig.java:500 (assist 70 px atrás do titular); CombatEngine.java:409 (só o golpe do assist é testado contra o rival: o assist não tem corpo atingível)
CENÁRIO: assist chamado pelo titular com o boneco parado a 100–1000 px, defendendo, e pulando a 250 px (12 tempos); vantagem do titular num H defendido com e sem o assist.
RESULTADO:
- 15 de 16 usam "S", mas o S1 varia: 11 são projéteis (p01, p02, p03, p04, p05, p06 baixo, p08, p09, p10, p11, p16) e 4 são golpes de corpo (p07, p12, p13, p15) + p14.
- Projéteis longos (alcance até 750–1000 px): p01, p05, p06, p08, p09, p10, p11, p16. Curtos: p02 e p04 (até 350), p03 (até 500).
- Corpo a corpo: p07 só até 150 px; p12, p13 e p15 só a 100 px (o assist nasce 70 px atrás do titular e o golpe não anda); defendendo, nenhum deles chega.
- p14: o assist (o mergulho do S1) não acerta em nenhuma distância, nem no boneco pulando — assist nulo.
- Início: 15–22 quadros na maioria; p13 27, p16 35 (lento).
- Cobertura do H defendido (−2 sem assist): vira +4 a +25 com p02, p04, p05, p06 (+25), p07, p08, p09, p10, p16; não muda (−1/−2) com p01, p03, p11 e p12–p15.
- O assist em si nunca é punido (não tem corpo); o risco é só do titular.
IMPACTO: a homogeneidade real não é "todo mundo é S": é "11 projéteis parecidos + 4 assistências que quase não funcionam". p12, p13, p14 e p15 são parceiros fracos (confirma T18 e T21).
ALTERNATIVAS: (a) mover o assist corpo a corpo para a frente do titular ou fazê-lo avançar; (b) dar a p12–p15 outro golpe de assist (o 2H, uma investida, um antiaéreo); (c) assistências selecionáveis (só depois de (a)/(b)).
RECOMENDAÇÃO: (b) para p14 (assist nulo) já; (a) ou (b) para p12, p13, p15. Diferenciar os projéteis por função (antiaéreo, rasteiro, arco) antes de pensar em seleção.
TESTES DE REGRESSÃO: `BateriaEquipe t17`; AiFullKitTest.

---

ID: T18 Assistência nos combos
STATUS: REPRODUZIDO (busca heurística)
CENÁRIO: busca em feixe (largura 40, 14 passos, sem barra) do maior combo com cada um dos 15 parceiros, para Ryūga, Kōga, p09 e p11; comparado à mesma busca sem assist.
RESULTADO:
- p09: sem assist 3660; com p04/p03/p08/p06/p11 ~6900–7120 (+3400): o assist é o que abre o quique e o juggle dele. Com p12/p13/p14 −226 (o assist não ajuda).
- p11: sem assist 2615; com p09/p04/p01/p03/p08 ~5100–5400 (+2500–2800); com o parceiro oficial p12, 2341 (pior que sem).
- Kōga: ganho pequeno (+56 a +625), p09 o melhor.
- Ryūga: a busca não achou rota com assist melhor que a sem (5914); o assist não acrescenta ao combo dele dentro desse orçamento.
- p12, p13 e p14 dão exatamente o mesmo resultado para todos os titulares (o assist deles não acerta).
- Não há um parceiro único dominante, mas há um grupo: projéteis longos com bom hitstun (p09, p04, p08, p06, p03).
IMPACTO: p11 + p12 (dupla oficial) é a pior combinação de combo para o p11; os titulares lentos dependem do assist.
ALTERNATIVAS: corrigir os assists de corpo (T17) e repetir; ou aceitar que o parceiro oficial seja "de história" e não de combo.
RECOMENDAÇÃO: resolver T17 primeiro; depois repetir T18 para todos os 16 titulares.
TESTES DE REGRESSÃO: `BateriaEquipe t18` (~20 s).

---

ID: F04 / T19 Segurança do TAG
STATUS: REPRODUZIDO
ARQUIVO E LINHAS: TeamSystem.java:170 (pedido envelhece 4 q), 287 (`canRawTag` exige `point.canAct()`), 75 (`conversionOpen`); CombatEngine.java:2161 (`convertAssist` recusa titular em hitstun/blockstun/caído/Ultra)
CENÁRIO: TAG direto contra rival parado/defendendo/backdash a 150–900 px; 400 lutas CPU × CPU instrumentadas; TAG apertado em cada quadro do congelamento de um H que acerta.
RESULTADO:
- TAG direto: a voadora acerta a 150–350 px, é defendida com +3/+5 para quem entrou (segura) e erra a ≥600 px com 0 de vantagem; do aperto ao fim da voadora, 54–69 quadros. Não é punível em nenhum caso medido.
- Assist → TAG: 2672 pedidos, 575 perdidos (22%). No quadro em que o assist acabou o titular estava em hitstun 279 (49%), defendendo 106 (18%), caído 77, no ar apanhando 54, no Ultra do rival 38, levantando 21. Ou seja, quase sempre o titular foi atingido entre o pedido e o fim do assist (até 33 q).
- Janela: TAG até 32 q depois do chamado vira troca (espera de 33→2 q); de 34 em diante é ignorado.
- TAG no congelamento do acerto: o assist (TAG) sai em 14 de 14, 1 q depois; a troca (↓+TAG) em 0 de 14 — ela só aceita titular livre e o pedido expira 4 q depois do congelamento, ainda na recuperação do H.
- Guard Cancel TAG: 2,11 por luta; o golpe de quem entra acerta 63%; quem entrou levou golpe em até 1,5 s em 34%. DHC: 0,9 por luta.
IMPACTO: a troca perdida é sempre silenciosa; ↓+TAG não cancela golpe nenhum (só do neutro), o que também parece "não respondeu".
ALTERNATIVAS: (a) feedback: confirmar o pedido aceito na hora e avisar a troca recusada; (b) trocar no quadro do aperto (o parceiro já é titular durante o golpe do assist); (c) guardar o ↓+TAG até o titular ficar livre (como o assist faz no congelamento).
RECOMENDAÇÃO: (a) primeiro, como o documento pede; (c) é barato e não muda o risco. (b) só se, com feedback, ainda parecer lento.
TESTES DE REGRESSÃO: `BateriaEquipe t19`; TeamSystemTest; AiFullKitTest.

---

ID: T20 Eficiência do DHC
STATUS: REPRODUZIDO
CENÁRIO: as 8 duplas oficiais, M H + fecho, 3 barras, no boneco com vida normal.
RESULTADO (médias): Super 3042 de dano, 1 barra, 39 q congelados; Super + DHC 3341, 2 barras, 53 q; Ultra 5257, 3 barras, 24 q no motor (+ a página).
- Dano por barra: Super 3042, DHC 1670, Ultra 1752.
- O DHC rende +0 a +2065: ajuda p03+p04 (+1055), p13+p14 (+2065), p09+p10 (+800); não acrescenta nada em p05+p06 e p15+p16 (o Super do parceiro erra) e piora p07+p08 (2196 contra 4545: a troca corta o Super do p07).
- O DHC deixa o rival mais perto (90–366 px) e longe da parede; o Ultra joga o rival para perto da parede (151–989 px dela).
IMPACTO: o DHC quase nunca é a escolha certa com a barra; o Ultra paga 3 barras com ~1,7× o Super.
ALTERNATIVAS: manter; garantir que o Super do parceiro acerte o rival onde o DHC o deixa (posição/alcance); ou desconto no DHC.
RECOMENDAÇÃO: rever os casos em que o Super do parceiro erra depois do DHC (p05+p06, p15+p16, p07+p08) antes de mexer em custo.
TESTES DE REGRESSÃO: `BateriaEquipe t20`.

---

ID: T21 Sinergia das duplas oficiais
STATUS: REPRODUZIDO (CPU; ±7 pontos)
CENÁRIO: cada titular com o oficial e 3 alternativos fixos, 2x2 contra as outras 7 duplas oficiais, 56 lutas por célula.
RESULTADO: oficial é o melhor só para Ryūga (82%) e p07 (55%). Kōga: oficial p04 52%, com p09 79%. p09: p10 79%, p04 88%. p11: p12 38%, com Ryūga 77%. p13: p14 34%, com p08 68%. p15: p16 9%, com p10 23%.
- O resultado segue a força do parceiro (p01, p04, p08, p09 fortes; p14, p16 fracos), não sinergia própria da dupla.
IMPACTO: as parcerias da história não têm função jogável distinta; p13+p14 e p15+p16 são as mais fracas (além de marcadas como não prontas).
ALTERNATIVAS: dar a cada dupla uma rota ou cobertura que só ela tem (assist que cobre a fraqueza do titular); ou aceitar como parcerias de história.
RECOMENDAÇÃO: DECIDIR; o primeiro passo concreto é T17 (assist de p12–p15) e repetir T21.
TESTES DE REGRESSÃO: `BateriaEquipe t21` (~15 s).

---

### Tabelas completas

### T12 Combo Breaker

#### Altura do combo e lugar (16 atacantes × 10 sementes; boneco com 1 barra, sem defender)

| quebra | lugar | quebras | levou antes | dano evitado | troco | troco ≥ 1 combo de 3 acertos |
|---|---|---|---|---|---|---|
| no 2º acerto | meio | 142 | 836 | 805 | 1418 | 56% |
| no 3º acerto | meio | 96 | 1291 | 545 | 1131 | 38% |
| no 4º acerto | meio | 68 | 1786 | 206 | 913 | 29% |
| no ar | meio | 91 | 1385 | 472 | 819 | 21% |
| no 2º acerto | canto | 94 | 976 | 893 | 821 | 30% |
| no ar | canto | 62 | 1357 | 800 | 618 | 18% |

#### A/B: regra atual × "volta ao neutro" (lutas 2x2 espelhadas, 240 por linha)

| regra | atordoamento | corrida automática | troco médio | sempre quebra vence nunca quebra |
|---|---|---|---|---|
| atual | 44 quadros | 40 quadros | 1558 | 73% |
| meio-termo | 24 quadros | não | 580 | 68% |
| volta ao neutro | 12 quadros | não | 693 | 65% |

Controle (nunca × nunca, mesmas sementes): 45%.

### T15 Valor dos quatro SHINKAI (F02 ENERGY, F03 eco SPEED)

1x1 espelhado nos 16 lutadores (15 sementes cada): o lado A liga o SHINKAI no GO, o lado B (mesma CPU)
não tem. Janela de 8 s + o brilho. "sem" = a mesma janela sem ligar nada.

| tipo | dano de A | dano em A | saldo | saldo a mais que sem | barra ganha (A) | da barra passiva | Supers de A | vida que voltou | distância média | defesas de B |
|---|---|---|---|---|---|---|---|---|---|---|
| sem | 1972 | 2135 | -162 | – | 1.05 barra | 0.00 barra | 0.07 | 28 | 311 px | 1.1 |
| POWER | 4232 | 916 | +3316 | +3478 | 1.18 barra | 0.00 barra | 1.16 | 252 | 313 px | 0.6 |
| SPEED | 3781 | 1039 | +2742 | +2904 | 1.27 barra | 0.00 barra | 1.22 | 224 | 282 px | 0.5 |
| ENERGY | 3020 | 1728 | +1292 | +1454 | 2.97 barra | 1.88 barra | 1.03 | 476 | 323 px | 0.9 |
| TECHNIQUE | 2841 | 1314 | +1527 | +1689 | 0.84 barra | 0.00 barra | 0.74 | 347 | 314 px | 1.3 |

F03 eco do SPEED: dano de A em 8 s 3781 com o eco, 3342 com o eco sem dano (+439, 13%).
F02 conta do ENERGY: 4 por quadro × 480 quadros = 1.92 barra (o relógio do SHINKAI para nos congelamentos).

### T16 Resposta contra cada SHINKAI

A (CPU nível 8) liga o SHINKAI no GO; B responde por 8 s. Dano que B leva / que B dá (16 lutadores × 6 sementes).
"sem" = A sem SHINKAI, mesma resposta. No canto, B começa encostado na parede.

##### No meio

| resposta de B | sem | POWER | SPEED | ENERGY | TECHNIQUE |
|---|---|---|---|---|---|
| CPU nível 8 | 2148 / 1853 | 4497 / 953 | 3417 / 1371 | 3042 / 1793 | 2566 / 1438 |
| defesa perfeita | 700 / 0 | 1074 / 0 | 1337 / 0 | 1240 / 0 | 1074 / 0 |
| fugir (trás + backdash) | 848 / 0 | 984 / 0 | 1135 / 0 | 1095 / 0 | 984 / 0 |
| agarrão de perto | 726 / 0 | 1135 / 25 | 1600 / 248 | 1301 / 38 | 1115 / 25 |
| parado | 5210 / 0 | 6007 / 0 | 5942 / 0 | 6676 / 0 | 6441 / 0 |
| TAG na hora | 700 / 0 | 1584 / 430 | 1388 / 563 | 1145 / 632 | 1293 / 209 |

##### No canto

| resposta de B | sem | POWER | SPEED | ENERGY | TECHNIQUE |
|---|---|---|---|---|---|
| CPU nível 8 | 2181 / 2078 | 4780 / 894 | 4249 / 1214 | 3550 / 1696 | 3558 / 1287 |
| defesa perfeita | 781 / 0 | 1485 / 0 | 2094 / 0 | 1233 / 0 | 1429 / 0 |
| fugir (trás + backdash) | 1049 / 0 | 1813 / 0 | 2202 / 0 | 1429 / 0 | 1761 / 0 |
| agarrão de perto | 931 / 13 | 1685 / 25 | 2698 / 223 | 1315 / 50 | 1638 / 38 |
| parado | 5211 / 0 | 6460 / 0 | 6775 / 0 | 6912 / 0 | 6886 / 0 |
| TAG na hora | 781 / 0 | 2070 / 440 | 2180 / 556 | 1506 / 676 | 1551 / 323 |


### T17 Função das assistências (F06)

O assist de cada um, chamado pelo Ryūga (o assist do próprio Ryūga é chamado pela Yin-lu).
Início = quadros do aperto até o 1º acerto no boneco mais perto que ele alcança. Alcance = distâncias (do titular) em
que acerta o boneco parado, de 100 a 1000 px (o assist aparece 70 px atrás do titular). Antiaéreo = em quantos de 12 tempos
de chamada ele acerta o boneco que fica pulando parado a 250 px (cobertura vertical, não um antiaéreo de reação).
Cobertura = vantagem do titular num H defendido sem / com o assist chamado junto. O assist não tem corpo que o rival
acerte (CombatEngine só testa o golpe dele contra o rival), então ele mesmo nunca é punido.

| lutador | golpe | tipo | início | alcance | dano | hitstun | blockstun | antiaéreo | cobertura (H defendido) | função |
|---|---|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | S (S) | projétil | 15 q | 100–800 px | 850 | 20 | 14 | 75% | -2 / -2 | projétil com antiaéreo |
| Yin-lu (p02) | S (S) | projétil | 18 q | 100–350 px | 760 | 31 | 25 | 50% | -2 / +13 | antiaéreo |
| Kōga (p03) | S (S) | projétil | 19 q | 100–500 px | 850 | 20 | 14 | 42% | -2 / -1 | investida / alcance médio |
| p04 | S (S) | projétil | 19 q | 100–350 px | 890 | 30 | 24 | 25% | -2 / +8 | extensão de perto |
| p05 | S (S) | projétil | 17 q | 100–1000 px | 760 | 29 | 23 | 33% | -2 / +6 | projétil (controle de chão) |
| p06 | S (S) | projétil baixo | 19 q | 100–850 px | 720 | 31 | 25 | 50% | -2 / +25 | projétil com antiaéreo |
| p07 | S (S) | corpo a corpo | 21 q | 100–150 px | 1020 | 31 | 25 | 0% | -2 / +11 | extensão de perto |
| p08 | S (S) | projétil | 19 q | 100–750 px | 810 | 26 | 20 | 83% | -2 / +6 | projétil com antiaéreo |
| p09 | S (S) | projétil | 22 q | 100–1000 px | 1060 | 31 | 25 | 75% | -2 / +12 | projétil com antiaéreo |
| p10 | S (S) | projétil | 17 q | 100–800 px | 720 | 27 | 21 | 58% | -2 / +4 | projétil com antiaéreo |
| p11 | S (S) | projétil | 19 q | 100–1000 px | 680 | 20 | 14 | 92% | -2 / -1 | projétil com antiaéreo |
| p12 | S (S) | corpo a corpo | 19 q | 100–100 px | 940 | 31 | 0 | 0% | -2 / -2 | extensão de perto |
| p13 | S (S) | corpo a corpo | 27 q | 100–100 px | 264 | 29 | 0 | 0% | -2 / -2 | extensão de perto |
| p14 | S (S) | corpo a corpo | – | – | 0 | 0 | 0 | 0% | -2 / -2 | não acerta parado |
| p15 | M (M) | corpo a corpo | 17 q | 100–100 px | 500 | 23 | 0 | 0% | -2 / -2 | extensão de perto |
| p16 | S (S) | projétil | 35 q | 100–1000 px | 850 | 20 | 14 | 83% | -2 / +16 | projétil com antiaéreo |

### T18 Assistência nos combos

Busca em feixe (largura 40, 14 passos, sem Super, sem barra) do maior combo de cada titular com cada parceiro;
"sem assist" é a mesma busca proibindo o assist. Ganho = maior com assist − maior sem assist.

##### Titular Ryūga (p01) (maior sem assist: 5914)

| parceiro | maior combo com assist | ganho | rota |
|---|---|---|---|
| p07 | 5883 | -31 | H 2L A 2H J jM jH jM jH j2H S M S H |
| p08 | 5837 | -77 | H M A 2H J jM jH j2H jH S j2H M H A |
| p15 | 5822 | -92 | H A J S j2H S H J 2H J jH S jH j2H |
| Yin-lu (p02) (oficial) | 5779 | -135 | H M A 2H J jM jH j2H S jH j2H M H A |
| p12 | 5733 | -181 | M S A H J 2H J jH S jH S j2H jM H |
| p13 | 5733 | -181 | M S A H J 2H J jH S jH S j2H jM H |
| p14 | 5733 | -181 | M S A H J 2H J jH S jH S j2H jM H |
| p09 | 3971 | -1943 | H M A M H S 2L S |
| p04 | 3742 | -2172 | H A 2L M H S 2L S |
| Kōga (p03) | 3704 | -2210 | H A 2L M H S 2L S |
| p16 | 3659 | -2255 | H 2L A M H S 2L S |
| p05 | 3619 | -2295 | H A 2L M H S 2L S |
| p06 | 3581 | -2333 | H A 2L M H S 2L S |
| p10 | 3581 | -2333 | H A 2L M H S 2L S |
| p11 | 3544 | -2370 | H M A 2L H S 2L S |

##### Titular Kōga (p03) (maior sem assist: 5332)

| parceiro | maior combo com assist | ganho | rota |
|---|---|---|---|
| p09 | 5957 | +625 | H A 2L 2H J jM jH j2H jH jM j2H S H A |
| p06 | 5868 | +536 | H A H J M 2H J jH S jH j2H S H A |
| Yin-lu (p02) | 5830 | +498 | H A 2L H J M 2H J jH S jM j2H A H |
| Ryūga (p01) | 5780 | +448 | H 2L A H J M 2H J jH S jH j2H A 2H |
| p11 | 5730 | +398 | H A 2L H J M 2H J jH S jM j2H A H |
| p07 | 5715 | +383 | M 2H A J jM jH j2H jH j2H M H W14 H S |
| p04 (oficial) | 5614 | +282 | H 2L A 2H J jM jH jM jH j2H S 2H J j2H |
| p16 | 5575 | +243 | H 2L A 2H J jM jH j2H jH j2H J S j2H H |
| p05 | 5559 | +227 | H 2L A 2H J jM jH j2H jH jM j2H S H A |
| p08 | 5542 | +210 | H 2L A 2H J jM jH jM jH j2H S 2H J j2H |
| p10 | 5511 | +179 | H 2L A 2H J jM jH j2H jH jM j2H S H A |
| p15 | 5388 | +56 | H A 2H J jM jH jM jH j2H S 2H J j2H |
| p12 | 5220 | -112 | H 2L 2H A J jM jH jM jH j2H S 2L H S |
| p13 | 5220 | -112 | H 2L 2H A J jM jH jM jH j2H S 2L H S |
| p14 | 5220 | -112 | H 2L 2H A J jM jH jM jH j2H S 2L H S |

##### Titular p09 (maior sem assist: 3660)

| parceiro | maior combo com assist | ganho | rota |
|---|---|---|---|
| p04 | 7122 | +3462 | H A H J 2H J jM S j2H jH M 2L A 2H |
| Kōga (p03) | 7072 | +3412 | H A H J 2H J jM S j2H jH M 2L A 2H |
| p08 | 7066 | +3406 | H A H J 2H J jH W14 S j2H jH M 2H A |
| p06 | 6997 | +3337 | M H A 2H J jH W14 S j2H jH M H A H |
| p11 | 6901 | +3241 | H A H J 2H J jH W14 S j2H jH M 2H A |
| p05 | 6705 | +3045 | H 2L A M H J 2H J jM S j2H jH M 2H |
| p10 (oficial) | 6669 | +3009 | H 2L A M H J 2H J jM S j2H jH M 2H |
| p07 | 6177 | +2517 | M 2H A J jM jL jM j2H jH M H J H S |
| p15 | 4085 | +425 | H A M H S D S |
| p16 | 4034 | +374 | H 2L H A S D S |
| Yin-lu (p02) | 3911 | +251 | H 2L A H S D S |
| Ryūga (p01) | 3741 | +81 | H H A S D M |
| p12 | 3434 | -226 | H H S A S |
| p13 | 3434 | -226 | H H S A S |
| p14 | 3434 | -226 | H H S A S |

##### Titular p11 (maior sem assist: 2615)

| parceiro | maior combo com assist | ganho | rota |
|---|---|---|---|
| p09 | 5413 | +2798 | M H A 2H J jH S jH jM j2H 2M S 2H A |
| p04 | 5209 | +2594 | M H A 2H J jH S jH jM j2H 2M S A H |
| Ryūga (p01) | 5161 | +2546 | M H A 2H J jH S jH jM j2H 2M S 2H A |
| Kōga (p03) | 5161 | +2546 | M H A 2H J jH S jH jM j2H 2M S A H |
| p08 | 5113 | +2498 | M H A 2H J jH S jH jM j2H 2M S A H |
| Yin-lu (p02) | 5053 | +2438 | M H A 2H J jH S jH jM j2H 2M S 2H A |
| p05 | 5053 | +2438 | M H A 2H J jH S jH jM j2H 2M S 2H A |
| p10 | 5005 | +2390 | M H A 2H J jH S jH jM j2H 2M S 2H A |
| p07 | 4943 | +2328 | M 2H A J jM jH j2H jH j2H 2M H S M H |
| p15 | 4695 | +2080 | H A 2H J jM jH S j2H jL 2H J jH S j2H |
| p16 | 3071 | +456 | H 2L A M H S 2L S |
| p06 | 3009 | +394 | H A M 2L H S 2L S |
| p12 (oficial) | 2341 | -274 | H 2L A H S 2L S |
| p13 | 2341 | -274 | H 2L A H S 2L S |
| p14 | 2341 | -274 | H 2L A H S 2L S |


### T19 Segurança do TAG

#### TAG direto (↓+TAG): Ryūga sai, a Yin-lu entra na voadora. Vantagem: + quem entrou age primeiro;
"–" = o rival ainda estava no chão/atordoado 1 s depois.

| distância | rival | voadora | quadros do aperto até ela acabar | vantagem de quem entrou |
|---|---|---|---|---|
| 150 | parado | acertou | 69 | – |
| 150 | defendendo | defendida | 63 | +3 |
| 150 | backdash | acertou | 69 | – |
| 350 | parado | acertou | 69 | – |
| 350 | defendendo | defendida | 63 | +5 |
| 350 | backdash | errou | 54 | +0 |
| 600 | parado | errou | 54 | +0 |
| 600 | defendendo | errou | 54 | +0 |
| 600 | backdash | errou | 54 | +0 |
| 900 | parado | errou | 54 | +0 |
| 900 | defendendo | errou | 54 | +0 |
| 900 | backdash | errou | 54 | +0 |

#### Assist → TAG perdido: o que o titular fazia no quadro em que o assist acabou (lutas CPU × CPU, 400)

2672 pedidos, 575 perdidos (22%). Situação do titular nos perdidos: {AIR_HITSTUN no ar=54, BLOCKSTUN=106, HITSTUN=279, KNOCKDOWN=77, ULTRA=38, WAKEUP=21}.

Guard Cancel TAG nas mesmas lutas: 843 (2.11 por luta), acertou em 63%, quem entrou levou golpe em até 1,5 s em 34%. DHC: 369.

#### TAG apertado no congelamento do acerto (H acertando o boneco; aperto em cada quadro do congelamento)

| botão | apertos no congelamento | saiu | quadros até sair |
|---|---|---|---|
| TAG (assist) | 14 | 14 | 1.0 |
| ↓+TAG (troca) | 14 | 0 | – |

#### Assist → TAG sem pressão (TAG k quadros depois do assist; boneco parado)

| k | troca | quadros até a Yin-lu agir |
|---|---|---|
| 1 | sim | 33 |
| 10 | sim | 24 |
| 20 | sim | 14 |
| 31 | sim | 3 |
| 32 | sim | 2 |
| 34 | não | – |
| 40 | não | – |
| 44 | não | – |

### T20 Eficiência do DHC

Mesmo início (M H) no boneco, com 3 barras: Super sozinho, Super + DHC (TAG no Super já solto) e Ultra.
Congelado = quadros de tela parada (o Ultra conta só a parte no motor; a página do mangá fica fora).

| dupla | fecho | dano | acertos | barras gastas | vida recuperável do rival | congelado | distância final | rival da parede |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) + Yin-lu (p02) | Super | 4140 | 7 | 1.0 | 1115 | 34 | 263 px | 1073 px |
| Ryūga (p01) + Yin-lu (p02) | Super + DHC | 4358 | 9 | 2.0 | 1173 | 45 | 102 px | 1081 px |
| Ryūga (p01) + Yin-lu (p02) | Ultra | 5040 | 9 | 3.0 | 1358 | 24 | 967 px | 151 px |
| Kōga (p03) + p04 | Super | 2221 | 3 | 1.0 | 599 | 34 | 224 px | 1065 px |
| Kōga (p03) + p04 | Super + DHC | 3276 | 4 | 2.0 | 884 | 59 | 366 px | 941 px |
| Kōga (p03) + p04 | Ultra | 5036 | 11 | 3.0 | 1358 | 24 | 636 px | 475 px |
| p05 + p06 | Super | 1998 | 3 | 1.0 | 538 | 4 | 231 px | 1181 px |
| p05 + p06 | Super + DHC | 1998 | 3 | 2.0 | 538 | 8 | 239 px | 1181 px |
| p05 + p06 | Ultra | 4536 | 9 | 3.0 | 1222 | 24 | 539 px | 573 px |
| p07 + p08 | Super | 4545 | 4 | 1.0 | 1226 | 69 | 575 px | 1035 px |
| p07 + p08 | Super + DHC | 2196 | 3 | 2.0 | 592 | 73 | 396 px | 1206 px |
| p07 + p08 | Ultra | 6048 | 6 | 3.0 | 1632 | 24 | 620 px | 500 px |
| p09 + p10 | Super | 5168 | 5 | 1.0 | 1393 | 64 | 266 px | 1076 px |
| p09 + p10 | Super + DHC | 5968 | 8 | 2.0 | 1606 | 75 | 90 px | 1070 px |
| p09 + p10 | Ultra | 6295 | 5 | 3.0 | 1698 | 24 | 550 px | 854 px |
| p11 + p12 | Super | 3312 | 3 | 1.0 | 894 | 69 | 257 px | 1067 px |
| p11 + p12 | Super + DHC | 3910 | 4 | 2.0 | 1055 | 101 | 97 px | 1084 px |
| p11 + p12 | Ultra | 4028 | 7 | 3.0 | 1087 | 24 | 594 px | 520 px |
| p13 + p14 | Super | 1693 | 3 | 1.0 | 456 | 22 | 136 px | 1103 px |
| p13 + p14 | Super + DHC | 3758 | 12 | 2.0 | 1006 | 22 | 98 px | 597 px |
| p13 + p14 | Ultra | 6048 | 5 | 3.0 | 1632 | 24 | 411 px | 989 px |
| p15 + p16 | Super | 1260 | 2 | 1.0 | 340 | 18 | 325 px | 1135 px |
| p15 + p16 | Super + DHC | 1260 | 2 | 2.0 | 340 | 40 | 325 px | 1135 px |
| p15 + p16 | Ultra | 5023 | 21 | 3.0 | 1347 | 24 | 678 px | 605 px |

Média Super: 3042 de dano, 1.0 barra, 39 quadros congelados (3042 de dano por barra).
Média Super + DHC: 3341 de dano, 2.0 barra, 53 quadros congelados (1670 de dano por barra).
Média Ultra: 5257 de dano, 3.0 barra, 24 quadros congelados (1752 de dano por barra).

### T21 Sinergia das duplas oficiais

Cada titular com o parceiro oficial e com três alternativos, contra as outras sete duplas oficiais
(CPU nível 8 × CPU nível 8, 2x2, 8 lutas por adversário, metade de cada lado).

| titular | oficial | vitórias | alternativo 1 | alternativo 2 | alternativo 3 | melhor parceiro |
|---|---|---|---|---|---|---|
| Ryūga (p01) | Yin-lu (p02) | 82% | p04 75% | p07 64% | p12 59% | oficial |
| Kōga (p03) | p04 | 52% | p06 70% | p09 79% | p14 38% | p09 |
| p05 | p06 | 54% | p08 64% | p11 52% | p16 29% | p08 |
| p07 | p08 | 55% | p10 29% | p13 30% | Yin-lu (p02) 34% | oficial |
| p09 | p10 | 79% | p12 61% | p15 50% | p04 88% | p04 |
| p11 | p12 | 38% | p14 29% | Ryūga (p01) 77% | p06 57% | Ryūga (p01) |
| p13 | p14 | 34% | p16 30% | Kōga (p03) 54% | p08 68% | p08 |
| p15 | p16 | 9% | Yin-lu (p02) 20% | p05 7% | p10 23% | p10 |


---


---

## Anexo: Auditoria SŌKON, frente D: comparações diretas (seção 6), sinergia (C01) e matriz 16 × 16 (seção 7.3)

Medido no `CombatEngine` real, CPU nível 8, sementes fixas, sobre o HEAD 3703d87a (com as etapas 1–5 dos ajustes
aprovados: Breaker, Clash, TAG, p09 e barra nos combos). Código: `android/app/src/test/java/com/gamelutagpt/BateriaComparacoes.java`
(`java ... com.gamelutagpt.BateriaComparacoes [secao6|sinergia|matriz|tudo]`, ~1 min no total; reaproveita
`BateriaBalanco`, `BateriaIdentidade` e `BateriaEquipe`). Nada de produção foi alterado.

Cada grupo da seção 6 foi medido nos mesmos cenários:
- **especiais:** mecânica declarada no pacote, alcance efetivo no boneco em pé, vantagem na defesa e o
  que fazem no boneco a 250 px (dano, quanto andam, quanto puxam, se passam para trás, veneno, vida
  paga, quadros até agir);
- **corpo e mobilidade:** vida, início do L, alcance real de M/H/2M, andar, dash, pulo, air dashes,
  pulo na parede;
- **como a CPU joga** (depende da CPU): 1x1 contra os outros 15, 4 lutas por rival, com distância
  média, pulos, agarrões, uso de S1 e S2–S4 e de onde vem o dano;
- **janelas de reação** dos especiais defensivos (contra-golpe, repel, esquiva, armadura) contra o M, o
  2M e o S1 do outro do grupo: o especial apertado de 30 quadros antes a 40 depois do aperto do rival,
  contando em quantos quadros ele anula o golpe e em quantos ainda acerta. Antes, um controle confere
  se o golpe do rival acerta quem está parado;
- **os do grupo entre si:** 1x1, 20 lutas por par.

Ressalva que vale para todos os grupos: a CPU **não usa S2–S4** (0 a 0,4 por minuto; `AiController`
só tem o S1). Por isso a identidade de cada um, nas lutas da CPU, vem do S1, da distância e da vida.
As diferenças de projeto aparecem nos cenários com roteiro, não nas lutas.

---

```text
ID: Seção 6 / G1 Shotos técnicos — Ryūga (p01) × p15
STATUS: REPRODUZIDO · DECIDIR
CENÁRIO: especiais e janelas com roteiro; CPU 1x1 × elenco (60 lutas cada); 20 lutas entre os dois.
RESULTADO:
- Energia e punição à distância (Ryūga): existe. Onda 760 px/s, carregável, +9 na defesa; o S1 é 39% do dano dele e o Super 24%;
  maior combo sem barra 6934 (dano explosivo 5).
- Defesa técnica e repel (p15): o repel existe e funciona contra a Onda. Anula o projétil em 25 de 41 tempos (a 450 px), mas não
  devolve dano; contra o M anula só em 4 de 41 e contra o 2M em 12 de 41 (e aí pune). Só que a CPU nunca usa o repel (S2–S4: 0/min).
- Combos de karatê (p15): não aparecem. O maior combo sem barra é 3650, o menor do elenco (o Ryūga tira 6934). A rajada (S3,
  9 acertos) tira só 800 e deixa a p15 152 quadros sem agir.
- Onde se parecem: os dois jogam de projétil. A p15 tira 52% do dano do S1 (mais que o Ryūga, 39%), na mesma distância (257 × 274 px),
  com a mesma mobilidade. O Ryūga vence 95% dos confrontos; contra o elenco, 60% × 12%.
- O "Aparar" (S3) do Ryūga não tem mecânica de aparar no pacote nem no motor (`characters/player_base/character.json`, S3
  "nome": "Aparar", sem `mecanica`): é um golpe comum de 0 dano a 250 px e −7 na defesa. O nome promete um parry que não existe.
IMPACTO: a p15 não se diferencia do Ryūga pelo que o documento promete; é um Ryūga mais fraco.
ALTERNATIVAS: (a) dar à p15 dano de combo (rajada que emenda, normais que cancelam em S3) e à CPU o uso do repel;
  (b) dar ao "Aparar" a mecânica de contra-golpe (como o S3 da Yin-lu) ou trocar o nome; (c) manter.
RECOMENDAÇÃO: DECIDIR (a) e (b). O "Aparar" sem mecânica parece descuido de dados; ver no aparelho antes.
```

```text
ID: Seção 6 / G2 Lutadores de chutes — Yin-lu (p02) × p14
STATUS: REPRODUZIDO · DECIDIR
RESULTADO:
- Chutes longos (p14): existem. H 324 px, 2M 332 px, S2 tornado 480 px, S3 472 px, S4 424 px; os especiais de mais alcance do grupo.
  Anda devagar (200 px/s contra 300).
- Contra-ataque (Yin-lu): existe. O contra-golpe (S3, lê golpe alto) anula o M do p14 em 13 de 41 tempos e pune em 9; contra o 2M
  (baixo) quase nunca (2 de 41), como a leitura alta pede.
- Pressão próxima (Yin-lu): não aparece. Vantagem média dos normais −3,5 (p14 −3,7); tempo colada 39% contra 40% do p14.
- Onde se parecem: a CPU joga os dois na mesma distância (248 × 245 px), sem usar os especiais. O p14 tira 70% do dano dos normais
  e não usa o S1 (o mergulho). Yin-lu vence 45% dos confrontos.
IMPACTO: o "controle de trajetória" do p14 e a pressão da Yin-lu só existem no papel nas lutas da CPU.
ALTERNATIVAS: (a) CPU do p14 com distância preferida longa (o estilo "chutes" pede 180 px; o alcance dele pede ~300) e uso de S2–S4;
  (b) dar à Yin-lu normais positivos na defesa ou um avanço que emende; (c) manter.
RECOMENDAÇÃO: (a) é de baixo risco (só a CPU); (b) é DECIDIR.
```

```text
ID: Seção 6 / G3 Pressão — Kōga (p03) × p04 × p10
STATUS: REPRODUZIDO · DECIDIR
RESULTADO:
- Rekka e avanço (Kōga): existem. S2 3 acertos que andam 125 px, S3 "avanço em três" que anda 165 px e alcança 452 px.
- Esquiva e brawler (p04): a esquiva (S3, contra golpe alto) passa pelo M do rival em 7 de 42 tempos e nunca pelo 2M; contra projétil,
  8 de 42. A ombrada (S4) anda 171 px.
- Mobilidade aérea (p10): só os 2 air dashes (os outros têm 1) e a voadora que atravessa (217 px). Anda, corre e pula igual aos outros.
- Onde se parecem: a esquiva do p10 (S3) é a mesma da p04 (golpe alto, janelas de 6–9 de 42). Nas lutas da CPU os três são iguais:
  57% de vitórias cada, distância 237–243 px, colados 41–42%, 49% / 49% / 45% do dano nos normais. O p10 pula menos que o Kōga
  (5,9 × 7,9 por minuto).
- Entre si: p04 vence 70% do p10, Kōga 35% da p04 e 55% do p10.
IMPACTO: as três pressões se distinguem só pelos especiais, que a CPU não usa; p04 e p10 dividem a mesma esquiva.
ALTERNATIVAS: (a) CPU usar rekka, esquiva e air dash; (b) trocar a esquiva do p10 por algo aéreo (esquiva no ar, cancel do air dash);
  (c) manter.
RECOMENDAÇÃO: (a) primeiro; (b) DECIDIR depois de ver (a) no aparelho.
```

```text
ID: Seção 6 / G4 Controle de espaço — p06 × p08 × p11
STATUS: REPRODUZIDO · DECIDIR
RESULTADO:
- Sombra e puxão (p06): existem. O S3 puxa o rival 151 px a 250 px (alcance 444); a sombra rasteira (S1, 480 px/s) é +15 na defesa.
- Chicote (p08): existe e domina. S1 a 1100 px/s, H com 432 px (o maior normal do elenco). A CPU fica a 385 px (30% do tempo longe)
  e o S1 é 60% do dano; vence 87% contra o elenco.
- Veneno e armadilhas (p11): o veneno existe (a seringa, S2, envenena 900; o contra-golpe S3 envenena 600), mas a CPU não usa (S2–S4:
  0,2/min). Armadilha o p11 não tem: o "poça" do documento não aparece como especial.
- Onde se parecem: os três, nas mãos da CPU, são o mesmo zoner de S1 (25 a 32 por minuto; 51% / 60% / 63% do dano). A distância os
  separa (310 / 385 / 350 px).
- Entre si: p06 vence 65% do p08 e 65% do p11; p08 vence 65% do p11.
IMPACTO: o p08 é o mais forte do jogo (77% na matriz) só com o chicote; o p11 perde o que tem de próprio (veneno).
ALTERNATIVAS: (a) CPU usar puxão e veneno; (b) chicote mais lento ou com mais recuperação (o S1 do p08 sai a 1100 px/s e é +8);
  (c) dar ao p11 a armadilha do documento.
RECOMENDAÇÃO: (a) primeiro; (b) DECIDIR com jogadores, porque o p08 lidera também a matriz.
```

```text
ID: Seção 6 / G5 Resistência — p07 × p09 × p13
STATUS: REPRODUZIDO · DECIDIR
RESULTADO:
- Reação e capangas (p07): o contra-golpe (S3, lê tudo) anula o M e o 2M dos outros dois em 11–14 de 41 tempos e pune em 8–9; contra a
  investida do p13 (S1), 39 de 41. Os capangas só estão no Super (16% do dano).
- Agarrão (p09): depois da etapa 4b a CPU agarra 0,8 vez por minuto (8% do dano). O escudo (S3, armadura 1) segura um golpe mas leva o
  dano (0 de 42 tempos "sem dano": a armadura absorve, não anula); a ombrada (S4, armadura 1) atravessa o M dos dois.
- Couraça e impacto (p13): o mais lento do jogo (anda 170, dash 134), o 2M mais longo (336 px), vida 16100; a CPU fica colada 51% do tempo.
- Onde se parecem: p09 e p13 dividem vida (16100), dano de combo (6924 × 6809) e a armadura de um golpe. O p07 é o caso grave: o S1
  dele (tapa de luva) é corpo a corpo, a CPU quase não usa (1,3/min), 66% do dano vem dos normais e ele vence 13% contra o elenco,
  0% contra o p09 e 15% contra o p13.
IMPACTO: a identidade do p07 (reação) só aparece com roteiro; nas lutas ele é o pior do jogo (16% na matriz).
ALTERNATIVAS: (a) CPU do p07 esperar e usar o contra-golpe; (b) subir o p07 (vida, dano ou um S1 que alcance); (c) manter.
RECOMENDAÇÃO: (a) e medir de novo; se continuar abaixo de 30%, (b) é DECIDIR.
```

```text
ID: Seção 6 / G6 Mobilidade — p05 × p10
STATUS: REPRODUZIDO · DECIDIR
RESULTADO:
- Teleporte e parede (p05): existem. O S3 "selo e mudança" esquiva tudo e teleporta 353 px, passando para trás do rival; contra o
  projétil do p10 anula e pune em 26 de 42 tempos. Pulo na parede: só a p05 tem.
- Air dash e acrobacia (p10): 2 air dashes; a voadora (S4) atravessa e anda 217 px.
- Onde se parecem: nas lutas da CPU, nem o teleporte nem a parede nem o segundo air dash aparecem (S2–S4 0/min; pulos 5,4 × 5,9 por
  minuto, iguais ao resto do elenco). A p05 vence 32% contra o elenco e 20% contra o p10.
IMPACTO: os dois aéreos do elenco não jogam no ar com a CPU.
ALTERNATIVAS: (a) CPU com o estilo "aéreo" usando parede, teleporte e air dash; (b) manter.
RECOMENDAÇÃO: (a).
```

```text
ID: Seção 6 / G7 Imprevisibilidade — p12 × p16
STATUS: REPRODUZIDO · DECIDIR
RESULTADO:
- Troca de vida por poder (p12): existe. Todo especial custa vida (S1 150, S2–S4 250), e os de 250 tiram 996–1218 (os especiais dos outros
  ficam entre 720 e 1050 a 250 px). A CPU usa o S1 8,5 vezes por minuto, ≈1.300 de vida por minuto pagos só nele. Ela fica colada 54% do
  tempo e pula 10,5 vezes por minuto, o mais agressivo do elenco.
- Armadilhas e enganos (p16): a caixa surpresa (S2) é armadilha (+91 na defesa, porque fica no chão), e a banana (S3) é um carrinho
  que anda 120 px. A CPU não usa nenhum dos dois; a p16 vive do balão (S1).
- A Fúria da p12 (S3, armadura 1) não anula nada: segura um golpe e leva o dano.
- Entre si: p12 vence 85% da p16; contra o elenco, 50% × 25%.
IMPACTO: a p12 é a única do grupo cuja identidade aparece nas lutas da CPU; a p16 é só o balão.
ALTERNATIVAS: (a) CPU da p16 armar a caixa e a banana; (b) manter.
RECOMENDAÇÃO: (a).
```

---

```text
ID: C01 sinergia (fichas completas)
STATUS: REPRODUZIDO
CENÁRIO:
- como assist (chamado pelo Ryūga; o do Ryūga, pela Yin-lu): distâncias de 100 a 1000 px em que acerta o boneco parado, quadros até
  acertar, quantas de 12 chamadas pegam o boneco pulando a 250 px e a cobertura (vantagem do titular num H defendido com o assist
  junto, menos sem);
- como parceiro: o que o assist soma ao maior combo (sem barra, sem Super; busca em feixe 40 × 14) de Ryūga, Kōga, p09 e p11;
- como titular: o que o parceiro oficial soma ao maior combo dele.
A nota (1–5) é o quintil do elenco numa soma desses quatro números.
RESULTADO:
- Os assistentes que mais valem: p08 (83% antiaéreo, +1509 no combo dos outros), p09 (alcance de tela inteira, 75% antiaéreo) e p06
  (+27 de cobertura, o maior).
- Os que não valem nada: p12, p13 e p15 só acertam a 100 px, e o p14 não acerta; somam −198 ao combo dos outros, ou seja, chamar
  atrapalha. O balão da p16 cobre bem (83% antiaéreo, +18), mas sai em 35 quadros e não cabe em combo (−296).
- Duplas oficiais (o parceiro somado ao combo do titular): p09+p10 +3009 e p08+p07 +1919 são as melhores. Ryūga+Yin-lu −135,
  p11+p12 −274 e p15+p16 −989 (o balão lento quebra o combo da p15) atrapalham.
- Notas de sinergia: 5 para p06, p08 e p09; 1 para p12, p13, p14 e p15. As outras seis dimensões vêm da frente A, refeitas no código
  atual.
IMPACTO: metade das duplas oficiais não tem sinergia jogável; os quatro do fim do elenco (p12–p15) são os piores assistentes.
ALTERNATIVAS: consertar os assistentes de corpo a corpo (já no backlog, item 2); dar ao balão da p16 uma versão rápida de assist.
RECOMENDAÇÃO: o conserto dos assistentes de corpo a corpo resolve a nota 1 de p12–p15 de uma vez.
```

```text
ID: 7.3 Matriz 1x1 16 × 16 com espelhos
STATUS: REPRODUZIDO (depende da CPU)
CENÁRIO: 256 combinações ordenadas (linha = lado esquerdo, coluna = lado direito), CPU nível 8 dos dois lados, 10 sementes por célula,
  2560 lutas.
RESULTADO:
- Viés de lado: nenhum. O lado esquerdo vence 50% das 2560 lutas; nos espelhos, 47% (75 de 160), e cada espelho fica entre 2 e 7 de 10.
- Só 3 dos 120 pares mudam de resultado em 6 ou mais de 10 com o lado (Ryūga × Kōga, Yin-lu × p16, p06 × p11). Com 10 lutas por lado,
  isso cabe no acaso (o esperado é ~2,4 pares).
- Força contra o elenco (os dois lados): p08 77%, p06 68%, Ryūga 67%, p04 63%, p13 62%, p11 61%, Kōga 60%, p10 60%, p09 58% … p12 43%,
  p14 41%, p05 41%, Yin-lu 37%, p16 30%, p15 17%, p07 16%.
- O p09 saiu do topo depois das etapas 4a/4b (era 72%, 4º lugar; agora 58%, 9º).
IMPACTO: o elenco tem um meio largo (58–68%) e uma cauda fraca (p15 e p07 abaixo de 20%, p16 e Yin-lu abaixo de 40%).
ALTERNATIVAS: tratar a cauda (p07, p15) pela identidade (G1, G5) antes de mexer em números; checar o p08 com jogadores.
RECOMENDAÇÃO: usar esta matriz como linha de base das próximas etapas (mesmas sementes: 900000 + 1000·linha + 20·coluna + semente).
```

---

### Tabelas completas

### Seção 6: comparações diretas


#### Shotos técnicos: Ryūga (p01) × p15

Promessa do documento: Energia e punição à distância × defesa técnica, repel e combos de karatê.

**Especiais** (mecânica declarada; alcance efetivo no boneco em pé; vantagem na defesa; o que fazem no boneco a 250 px):

| lutador | especial | mecânica | alcance | vantagem na defesa | efeito a 250 px |
|---|---|---|---|---|---|
| Ryūga (p01) | S1 Onda | projétil 760 px/s, carregável | projétil | +9 | dano 850, anda +10, puxa 0, livre em 19 q |
| Ryūga (p01) | S2 Punho ascendente | golpe simples | 184 px | +1 | dano 0, anda +10, puxa 0, livre em 42 q |
| Ryūga (p01) | S3 Aparar | golpe simples | 236 px | -7 | dano 0, anda +0, puxa 0, livre em 43 q |
| Ryūga (p01) | S4 Chute giratório | avanço, 3 acertos | 336 px | -15 | dano 1000, anda +110, puxa 0, livre em 63 q |
| p15 | S1 Seiken Hadō | projétil 900 px/s | projétil | +0 | dano 850, anda +10, puxa 0, livre em 36 q |
| p15 | S2 Repel da aura vermelha | contra-golpe (ANY), repel | – | – | dano 0, anda +10, puxa 0, livre em 65 q |
| p15 | S3 Rajada de punhos | avanço, 9 acertos | 332 px | – | dano 800, anda +109, puxa 0, livre em 152 q |
| p15 | S4 Voadora e chute girando | avanço, 2 acertos | 348 px | -6 | dano 1000, anda +108, puxa 0, livre em 86 q |

**Corpo, alcance e mobilidade:**

| lutador | vida | L (início) | alcance M / H / 2M | anda | dash 20 q | pulo à frente | air dashes | pulo na parede | super pulo |
|---|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 14000 | 2 q | 208 / 256 / 240 | 300 | 207 | 280 | 1 | não | 627 |
| p15 | 14000 | 3 q | 196 / 300 / 332 | 300 | 207 | 280 | 1 | não | 627 |

**Como a CPU joga** (1x1 nível 8 contra os outros 15, 4 lutas por rival; depende da CPU):

| lutador | vence | distância média | colado / longe | pulos/min | agarrões/min | S1/min | S2–S4/min | dano: normais | dano: S1 | dano: S2–S4 | dano: Super | dano: Ultra | dano: agarrão | dano: Clash | dano: outros |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 60% | 274 px | 33% / 14% | 5.0 | 0.1 | 19.2 | 0.0 | 25% | 39% | 0% | 24% | 5% | 1% | 6% | 0% |
| p15 | 12% | 257 px | 37% / 10% | 4.4 | 0.0 | 10.9 | 0.0 | 29% | 52% | 0% | 5% | 2% | 1% | 11% | 0% |

**Janelas de reação** (o especial defensivo apertado de 30 quadros antes a 40 depois do aperto do rival; em quantos quadros ele anula o golpe, e em quantos ainda acerta):

| quem reage | contra | anula em | anula e acerta em |
|---|---|---|---|
| p15 S2 Repel da aura vermelha | Ryūga (p01) M a 150 px | 4 de 41 | 4 |
| p15 S2 Repel da aura vermelha | Ryūga (p01) 2M a 150 px | 12 de 41 | 12 |
| p15 S2 Repel da aura vermelha | Ryūga (p01) S1 a 450 px | 25 de 41 | 0 |

**Entre si** (1x1, 20 lutas por par, metade de cada lado):

- Ryūga (p01) vence 95% de p15


#### Lutadores de chutes: Yin-lu (p02) × p14

Promessa do documento: Contra-ataque e pressão próxima × chutes longos e controle de trajetória.

**Especiais** (mecânica declarada; alcance efetivo no boneco em pé; vantagem na defesa; o que fazem no boneco a 250 px):

| lutador | especial | mecânica | alcance | vantagem na defesa | efeito a 250 px |
|---|---|---|---|---|---|
| Yin-lu (p02) | S1 Palma | projétil 700 px/s | projétil | +15 | dano 760, anda +10, puxa 0, livre em 27 q |
| Yin-lu (p02) | S2 Chute da garça | golpe simples | 152 px | – | dano 0, anda +0, puxa 0, livre em 33 q |
| Yin-lu (p02) | S3 Contra-golpe | contra-golpe (HIGH) | 200 px | -4 | dano 0, anda +0, puxa 0, livre em 37 q |
| Yin-lu (p02) | S4 Chute rodopiante | avanço, 2 acertos | 268 px | -6 | dano 450, anda +0, puxa 0, livre em 44 q |
| p14 | S1 Chute rebote | mergulho, rebote | – | -5 | dano 0, anda +7, puxa 0, livre em 38 q |
| p14 | S2 Chute tornado | 5 acertos | 480 px | -12 | dano 1040, anda +7, puxa 0, livre em 95 q |
| p14 | S3 Meia-lua cortante | golpe simples | 472 px | -4 | dano 900, anda +0, puxa 0, livre em 54 q |
| p14 | S4 Pisão de onda | golpe simples | 424 px | +1 | dano 950, anda +0, puxa 0, livre em 62 q |

**Corpo, alcance e mobilidade:**

| lutador | vida | L (início) | alcance M / H / 2M | anda | dash 20 q | pulo à frente | air dashes | pulo na parede | super pulo |
|---|---|---|---|---|---|---|---|---|---|
| Yin-lu (p02) | 13440 | 3 q | 264 / 276 / 220 | 300 | 207 | 280 | 1 | não | 627 |
| p14 | 13300 | 5 q | 256 / 324 / 332 | 200 | 207 | 280 | 1 | não | 627 |

**Como a CPU joga** (1x1 nível 8 contra os outros 15, 4 lutas por rival; depende da CPU):

| lutador | vence | distância média | colado / longe | pulos/min | agarrões/min | S1/min | S2–S4/min | dano: normais | dano: S1 | dano: S2–S4 | dano: Super | dano: Ultra | dano: agarrão | dano: Clash | dano: outros |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Yin-lu (p02) | 52% | 248 px | 39% / 9% | 4.7 | 0.0 | 10.6 | 0.0 | 36% | 31% | 0% | 21% | 3% | 1% | 8% | 0% |
| p14 | 43% | 245 px | 40% / 7% | 8.2 | 0.0 | 0.0 | 0.0 | 70% | 0% | 0% | 20% | 1% | 0% | 10% | 0% |

**Janelas de reação** (o especial defensivo apertado de 30 quadros antes a 40 depois do aperto do rival; em quantos quadros ele anula o golpe, e em quantos ainda acerta):

| quem reage | contra | anula em | anula e acerta em |
|---|---|---|---|
| Yin-lu (p02) S3 Contra-golpe | p14 M a 150 px | 13 de 41 | 9 |
| Yin-lu (p02) S3 Contra-golpe | p14 2M a 150 px | 2 de 41 | 0 |
| Yin-lu (p02) S3 Contra-golpe | p14 S1 a 450 px | não alcança quem está parado | – |

**Entre si** (1x1, 20 lutas por par, metade de cada lado):

- Yin-lu (p02) vence 45% de p14


#### Pressão: Kōga (p03) × p04 × p10

Promessa do documento: Rekka e avanço × esquiva/brawler × mobilidade aérea.

**Especiais** (mecânica declarada; alcance efetivo no boneco em pé; vantagem na defesa; o que fazem no boneco a 250 px):

| lutador | especial | mecânica | alcance | vantagem na defesa | efeito a 250 px |
|---|---|---|---|---|---|
| Kōga (p03) | S1 Onda curta | projétil 950 px/s | projétil | +3 | dano 850, anda +10, puxa 0, livre em 29 q |
| Kōga (p03) | S2 Punho em sequência | avanço, 3 acertos | 296 px | -10 | dano 734, anda +125, puxa 0, livre em 52 q |
| Kōga (p03) | S3 Avanço em três | avanço, 3 acertos | 452 px | -4 | dano 900, anda +165, puxa 0, livre em 60 q |
| Kōga (p03) | S4 Mergulho | mergulho | – | +3 | só no ar |
| p04 | S1 Soco de impacto | projétil 800 px/s | projétil | +12 | dano 890, anda +10, puxa 0, livre em 30 q |
| p04 | S2 Cabeçada | avanço | 180 px | – | dano 0, anda +0, puxa 0, livre em 37 q |
| p04 | S3 Esquiva | esquiva (HIGH) | 188 px | -4 | dano 0, anda +0, puxa 0, livre em 41 q |
| p04 | S4 Ombrada | avanço | 372 px | +1 | dano 1050, anda +171, puxa 0, livre em 47 q |
| p10 | S1 Machadinho | projétil 900 px/s | projétil | +12 | dano 720, anda +0, puxa 0, livre em 25 q |
| p10 | S2 Jato de vapor | golpe simples | 208 px | +1 | dano 0, anda +10, puxa 0, livre em 32 q |
| p10 | S3 Esquiva acrobática | esquiva (HIGH) | 288 px | -4 | dano 760, anda +0, puxa 0, livre em 33 q |
| p10 | S4 Voadora em parafuso | avanço atravessa | 428 px | +1 | dano 850, anda +217, puxa 0, livre em 41 q |

**Corpo, alcance e mobilidade:**

| lutador | vida | L (início) | alcance M / H / 2M | anda | dash 20 q | pulo à frente | air dashes | pulo na parede | super pulo |
|---|---|---|---|---|---|---|---|---|---|
| Kōga (p03) | 13720 | 3 q | 204 / 208 / 312 | 300 | 207 | 280 | 1 | não | 627 |
| p04 | 14420 | 3 q | 260 / 260 / 288 | 300 | 207 | 280 | 1 | não | 627 |
| p10 | 13020 | 3 q | 284 / 196 / 252 | 300 | 207 | 280 | 2 | não | 627 |

**Como a CPU joga** (1x1 nível 8 contra os outros 15, 4 lutas por rival; depende da CPU):

| lutador | vence | distância média | colado / longe | pulos/min | agarrões/min | S1/min | S2–S4/min | dano: normais | dano: S1 | dano: S2–S4 | dano: Super | dano: Ultra | dano: agarrão | dano: Clash | dano: outros |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Kōga (p03) | 57% | 243 px | 41% / 8% | 7.9 | 0.1 | 8.1 | 0.0 | 49% | 33% | 0% | 6% | 3% | 0% | 8% | 0% |
| p04 | 57% | 237 px | 42% / 8% | 6.9 | 0.1 | 8.6 | 0.4 | 49% | 27% | 1% | 11% | 4% | 0% | 8% | 0% |
| p10 | 57% | 243 px | 41% / 9% | 5.9 | 0.1 | 12.5 | 0.0 | 45% | 30% | 0% | 3% | 15% | 0% | 7% | 0% |

**Janelas de reação** (o especial defensivo apertado de 30 quadros antes a 40 depois do aperto do rival; em quantos quadros ele anula o golpe, e em quantos ainda acerta):

| quem reage | contra | anula em | anula e acerta em |
|---|---|---|---|
| p04 S3 Esquiva | Kōga (p03) M a 150 px | 7 de 42 | 7 |
| p04 S3 Esquiva | Kōga (p03) 2M a 150 px | 0 de 42 | 0 |
| p04 S3 Esquiva | Kōga (p03) S1 a 450 px | 8 de 42 | 0 |
| p04 S3 Esquiva | p10 M a 150 px | 6 de 42 | 6 |
| p04 S3 Esquiva | p10 2M a 150 px | 0 de 42 | 0 |
| p04 S3 Esquiva | p10 S1 a 450 px | 9 de 42 | 0 |
| p10 S3 Esquiva acrobática | Kōga (p03) M a 150 px | 7 de 42 | 7 |
| p10 S3 Esquiva acrobática | Kōga (p03) 2M a 150 px | 0 de 42 | 0 |
| p10 S3 Esquiva acrobática | Kōga (p03) S1 a 450 px | 6 de 42 | 0 |
| p10 S3 Esquiva acrobática | p04 M a 150 px | 6 de 42 | 6 |
| p10 S3 Esquiva acrobática | p04 2M a 150 px | 0 de 42 | 0 |
| p10 S3 Esquiva acrobática | p04 S1 a 450 px | não alcança quem está parado | – |

**Entre si** (1x1, 20 lutas por par, metade de cada lado):

- Kōga (p03) vence 35% de p04
- Kōga (p03) vence 55% de p10
- p04 vence 70% de p10


#### Controle de espaço: p06 × p08 × p11

Promessa do documento: Sombra e puxão × chicote × veneno e armadilhas.

**Especiais** (mecânica declarada; alcance efetivo no boneco em pé; vantagem na defesa; o que fazem no boneco a 250 px):

| lutador | especial | mecânica | alcance | vantagem na defesa | efeito a 250 px |
|---|---|---|---|---|---|
| p06 | S1 Sombra rastejante | projétil 480 px/s | projétil | +15 | dano 720, anda +0, puxa 0, livre em 30 q |
| p06 | S2 Eco de sombra | eco | 172 px | +1 | dano 0, anda +0, puxa 0, livre em 59 q |
| p06 | S3 Puxar | puxão 320 | 444 px | -4 | dano 760, anda +10, puxa 151, livre em 38 q |
| p06 | S4 Passo-sombra | avanço | 432 px | +1 | dano 850, anda +170, puxa 0, livre em 47 q |
| p08 | S1 Chicote | projétil 1100 px/s | projétil | +8 | dano 810, anda +3, puxa 0, livre em 30 q |
| p08 | S2 Giro ascendente | 2 acertos | 444 px | -12 | dano 1040, anda +0, puxa 0, livre em 53 q |
| p08 | S3 Contra-golpe | contra-golpe (LOW) | 208 px | -4 | dano 0, anda +0, puxa 0, livre em 41 q |
| p08 | S4 Salto-agulha | avanço | 408 px | +1 | dano 950, anda +190, puxa 0, livre em 47 q |
| p11 | S1 Bisturi | projétil 950 px/s | projétil | +2 | dano 680, anda +10, puxa 0, livre em 30 q |
| p11 | S2 Seringa | projétil 650 px/s, veneno 900 | 420 px | -2 | dano 880, anda +0, puxa 0, envenena, livre em 37 q |
| p11 | S3 Bloqueio de nervos | contra-golpe (ANY), veneno 600 | 184 px | -4 | dano 0, anda +10, puxa 0, livre em 41 q |
| p11 | S4 Passo cirúrgico | avanço | 340 px | +3 | dano 800, anda +75, puxa 0, livre em 47 q |

**Corpo, alcance e mobilidade:**

| lutador | vida | L (início) | alcance M / H / 2M | anda | dash 20 q | pulo à frente | air dashes | pulo na parede | super pulo |
|---|---|---|---|---|---|---|---|---|---|
| p06 | 13300 | 3 q | 244 / 216 / 272 | 300 | 207 | 280 | 1 | não | 627 |
| p08 | 13020 | 3 q | 284 / 432 / 200 | 300 | 207 | 280 | 1 | não | 627 |
| p11 | 12600 | 3 q | 220 / 236 / 264 | 300 | 207 | 280 | 1 | não | 627 |

**Como a CPU joga** (1x1 nível 8 contra os outros 15, 4 lutas por rival; depende da CPU):

| lutador | vence | distância média | colado / longe | pulos/min | agarrões/min | S1/min | S2–S4/min | dano: normais | dano: S1 | dano: S2–S4 | dano: Super | dano: Ultra | dano: agarrão | dano: Clash | dano: outros |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| p06 | 70% | 310 px | 27% / 19% | 5.0 | 0.0 | 25.0 | 0.0 | 23% | 51% | 0% | 5% | 13% | 0% | 7% | 0% |
| p08 | 87% | 385 px | 16% / 30% | 4.7 | 0.0 | 32.1 | 0.0 | 15% | 60% | 0% | 16% | 4% | 0% | 4% | 0% |
| p11 | 47% | 350 px | 25% / 29% | 4.8 | 0.0 | 29.6 | 0.2 | 13% | 63% | 0% | 16% | 3% | 1% | 5% | 0% |

**Janelas de reação** (o especial defensivo apertado de 30 quadros antes a 40 depois do aperto do rival; em quantos quadros ele anula o golpe, e em quantos ainda acerta):

| quem reage | contra | anula em | anula e acerta em |
|---|---|---|---|
| p08 S3 Contra-golpe | p06 M a 150 px | 4 de 41 | 0 |
| p08 S3 Contra-golpe | p06 2M a 150 px | 9 de 41 | 7 |
| p08 S3 Contra-golpe | p06 S1 a 450 px | 0 de 41 | 0 |
| p08 S3 Contra-golpe | p11 M a 150 px | 6 de 41 | 1 |
| p08 S3 Contra-golpe | p11 2M a 150 px | 9 de 41 | 7 |
| p08 S3 Contra-golpe | p11 S1 a 450 px | 4 de 41 | 0 |
| p11 S3 Bloqueio de nervos | p06 M a 150 px | 6 de 37 | 2 |
| p11 S3 Bloqueio de nervos | p06 2M a 150 px | 5 de 37 | 3 |
| p11 S3 Bloqueio de nervos | p06 S1 a 450 px | 0 de 37 | 0 |
| p11 S3 Bloqueio de nervos | p08 M a 150 px | 6 de 37 | 2 |
| p11 S3 Bloqueio de nervos | p08 2M a 150 px | 5 de 37 | 3 |
| p11 S3 Bloqueio de nervos | p08 S1 a 450 px | 4 de 41 | 0 |

**Entre si** (1x1, 20 lutas por par, metade de cada lado):

- p06 vence 65% de p08
- p06 vence 65% de p11
- p08 vence 65% de p11


#### Resistência: p07 × p09 × p13

Promessa do documento: Reação e capangas × agarrão × couraça e impacto.

**Especiais** (mecânica declarada; alcance efetivo no boneco em pé; vantagem na defesa; o que fazem no boneco a 250 px):

| lutador | especial | mecânica | alcance | vantagem na defesa | efeito a 250 px |
|---|---|---|---|---|---|
| p07 | S1 Tapa de luva | golpe simples | – | +3 | dano 1020, anda +10, puxa 0, livre em 42 q |
| p07 | S2 Chute ascendente | 3 acertos | 320 px | -12 | dano 1320, anda +10, puxa 0, livre em 69 q |
| p07 | S3 Contra-golpe | contra-golpe (ANY) | 336 px | -4 | dano 1080, anda +0, puxa 0, livre em 44 q |
| p07 | S4 Chute giratório baixo | golpe simples | 408 px | +1 | dano 1200, anda +0, puxa 0, livre em 57 q |
| p09 | S1 Machado | projétil 600 px/s | projétil | +8 | dano 1060, anda +10, puxa 0, livre em 38 q |
| p09 | S2 Cotovelada | golpe simples | 148 px | +1 | dano 0, anda +10, puxa 0, livre em 48 q |
| p09 | S3 Escudo | armadura 1 | 176 px | – | dano 0, anda +0, puxa 0, livre em 52 q |
| p09 | S4 Ombrada | armadura 1, avanço | 404 px | +1 | dano 1250, anda +131, puxa 0, livre em 58 q |
| p13 | S1 Investida do colosso | avanço, 5 acertos | – | -24 | dano 1320, anda +81, puxa 61, livre em 141 q |
| p13 | S2 Pedra arremessada | projétil 520 px/s | 260 px | +8 | dano 1320, anda +6, puxa 0, livre em 58 q |
| p13 | S3 Três cabeçadas | agarrão, 3 acertos | 116 px | – | dano 0, anda +0, puxa 0, livre em 73 q |
| p13 | S4 Salto do colosso | avanço | 504 px | +1 | dano 1200, anda +138, puxa 0, livre em 77 q |

**Corpo, alcance e mobilidade:**

| lutador | vida | L (início) | alcance M / H / 2M | anda | dash 20 q | pulo à frente | air dashes | pulo na parede | super pulo |
|---|---|---|---|---|---|---|---|---|---|
| p07 | 15120 | 4 q | 264 / 228 / 192 | 300 | 207 | 280 | 1 | não | 627 |
| p09 | 16100 | 4 q | 276 / 196 / 300 | 300 | 207 | 280 | 1 | não | 627 |
| p13 | 16100 | 4 q | 260 / 284 / 336 | 170 | 134 | 280 | 1 | não | 627 |

**Como a CPU joga** (1x1 nível 8 contra os outros 15, 4 lutas por rival; depende da CPU):

| lutador | vence | distância média | colado / longe | pulos/min | agarrões/min | S1/min | S2–S4/min | dano: normais | dano: S1 | dano: S2–S4 | dano: Super | dano: Ultra | dano: agarrão | dano: Clash | dano: outros |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| p07 | 13% | 250 px | 42% / 9% | 4.3 | 0.1 | 1.3 | 0.0 | 66% | 4% | 0% | 16% | 1% | 1% | 12% | 0% |
| p09 | 58% | 248 px | 42% / 9% | 3.9 | 0.8 | 5.5 | 0.4 | 50% | 20% | 1% | 11% | 2% | 8% | 9% | 0% |
| p13 | 60% | 236 px | 51% / 7% | 5.2 | 0.1 | 7.4 | 0.0 | 56% | 20% | 0% | 15% | 2% | 0% | 7% | 0% |

**Janelas de reação** (o especial defensivo apertado de 30 quadros antes a 40 depois do aperto do rival; em quantos quadros ele anula o golpe, e em quantos ainda acerta):

| quem reage | contra | anula em | anula e acerta em |
|---|---|---|---|
| p07 S3 Contra-golpe | p09 M a 150 px | 12 de 41 | 8 |
| p07 S3 Contra-golpe | p09 2M a 150 px | 11 de 41 | 9 |
| p07 S3 Contra-golpe | p09 S1 a 450 px | 6 de 45 | 0 |
| p07 S3 Contra-golpe | p13 M a 150 px | 14 de 41 | 8 |
| p07 S3 Contra-golpe | p13 2M a 150 px | 11 de 41 | 9 |
| p07 S3 Contra-golpe | p13 S1 a 450 px | 39 de 41 | 39 |
| p09 S3 Escudo | p07 M a 150 px | 0 de 42 | 0 |
| p09 S3 Escudo | p07 2M a 150 px | 0 de 42 | 0 |
| p09 S3 Escudo | p07 S1 a 450 px | não alcança quem está parado | – |
| p09 S3 Escudo | p13 M a 150 px | 0 de 42 | 0 |
| p09 S3 Escudo | p13 2M a 150 px | 0 de 42 | 0 |
| p09 S3 Escudo | p13 S1 a 450 px | 21 de 42 | 21 |
| p09 S4 Ombrada | p07 M a 150 px | 15 de 15 | 10 |
| p09 S4 Ombrada | p07 2M a 150 px | 8 de 15 | 8 |
| p09 S4 Ombrada | p07 S1 a 450 px | não alcança quem está parado | – |
| p09 S4 Ombrada | p13 M a 150 px | 15 de 15 | 10 |
| p09 S4 Ombrada | p13 2M a 150 px | 0 de 15 | 0 |
| p09 S4 Ombrada | p13 S1 a 450 px | 11 de 15 | 11 |

**Entre si** (1x1, 20 lutas por par, metade de cada lado):

- p07 vence 0% de p09
- p07 vence 15% de p13
- p09 vence 35% de p13


#### Mobilidade: p05 × p10

Promessa do documento: Teleporte e parede × air dash e acrobacia.

**Especiais** (mecânica declarada; alcance efetivo no boneco em pé; vantagem na defesa; o que fazem no boneco a 250 px):

| lutador | especial | mecânica | alcance | vantagem na defesa | efeito a 250 px |
|---|---|---|---|---|---|
| p05 | S1 Kunai | projétil 1000 px/s | projétil | +14 | dano 760, anda +10, puxa 0, livre em 25 q |
| p05 | S2 Shuriken | projétil 950 px/s | 476 px | -2 | dano 990, anda +0, puxa 0, livre em 32 q |
| p05 | S3 Selo e mudança | esquiva (ANY), teleporte | 520 px | -4 | dano 810, anda +353, puxa 47, passa para trás, livre em 33 q |
| p05 | S4 Corte de mão | avanço, 2 acertos | 488 px | -2 | dano 900, anda +150, puxa 0, livre em 46 q |
| p10 | S1 Machadinho | projétil 900 px/s | projétil | +12 | dano 720, anda +0, puxa 0, livre em 25 q |
| p10 | S2 Jato de vapor | golpe simples | 208 px | +1 | dano 0, anda +10, puxa 0, livre em 32 q |
| p10 | S3 Esquiva acrobática | esquiva (HIGH) | 288 px | -4 | dano 760, anda +0, puxa 0, livre em 33 q |
| p10 | S4 Voadora em parafuso | avanço atravessa | 428 px | +1 | dano 850, anda +217, puxa 0, livre em 41 q |

**Corpo, alcance e mobilidade:**

| lutador | vida | L (início) | alcance M / H / 2M | anda | dash 20 q | pulo à frente | air dashes | pulo na parede | super pulo |
|---|---|---|---|---|---|---|---|---|---|
| p05 | 12880 | 3 q | 276 / 204 / 220 | 300 | 207 | 280 | 1 | sim | 627 |
| p10 | 13020 | 3 q | 284 / 196 / 252 | 300 | 207 | 280 | 2 | não | 627 |

**Como a CPU joga** (1x1 nível 8 contra os outros 15, 4 lutas por rival; depende da CPU):

| lutador | vence | distância média | colado / longe | pulos/min | agarrões/min | S1/min | S2–S4/min | dano: normais | dano: S1 | dano: S2–S4 | dano: Super | dano: Ultra | dano: agarrão | dano: Clash | dano: outros |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| p05 | 32% | 275 px | 37% / 13% | 5.4 | 0.0 | 14.5 | 0.0 | 29% | 45% | 0% | 9% | 7% | 0% | 9% | 0% |
| p10 | 57% | 243 px | 41% / 9% | 5.9 | 0.1 | 12.5 | 0.0 | 45% | 30% | 0% | 3% | 15% | 0% | 7% | 0% |

**Janelas de reação** (o especial defensivo apertado de 30 quadros antes a 40 depois do aperto do rival; em quantos quadros ele anula o golpe, e em quantos ainda acerta):

| quem reage | contra | anula em | anula e acerta em |
|---|---|---|---|
| p05 S3 Selo e mudança | p10 M a 150 px | 6 de 42 | 6 |
| p05 S3 Selo e mudança | p10 2M a 150 px | 7 de 42 | 7 |
| p05 S3 Selo e mudança | p10 S1 a 450 px | 26 de 42 | 26 |
| p10 S3 Esquiva acrobática | p05 M a 150 px | 6 de 42 | 6 |
| p10 S3 Esquiva acrobática | p05 2M a 150 px | não alcança quem está parado | – |
| p10 S3 Esquiva acrobática | p05 S1 a 450 px | 7 de 42 | 0 |

**Entre si** (1x1, 20 lutas por par, metade de cada lado):

- p05 vence 20% de p10


#### Imprevisibilidade: p12 × p16

Promessa do documento: Troca de vida por poder × armadilhas e enganos.

**Especiais** (mecânica declarada; alcance efetivo no boneco em pé; vantagem na defesa; o que fazem no boneco a 250 px):

| lutador | especial | mecânica | alcance | vantagem na defesa | efeito a 250 px |
|---|---|---|---|---|---|
| p12 | S1 Investida | avanço, custa 150 de vida | – | – | dano 0, anda +4, puxa 0, paga 150 de vida, livre em 30 q |
| p12 | S2 Salto de garras | custa 250 de vida | 268 px | +1 | dano 1218, anda +0, puxa 0, paga 250 de vida, livre em 47 q |
| p12 | S3 Fúria | armadura 1, custa 250 de vida | 252 px | -4 | dano 996, anda +0, puxa 0, paga 250 de vida, livre em 38 q |
| p12 | S4 Contorção | avanço, custa 250 de vida, 2 acertos | 348 px | -6 | dano 1106, anda +144, puxa 0, paga 250 de vida, livre em 52 q |
| p16 | S1 Balão | projétil 500 px/s | projétil | -2 | dano 850, anda +10, puxa 0, livre em 50 q |
| p16 | S2 Caixa surpresa | armadilha | projétil | +91 | dano 0, anda +10, puxa 0, livre em 42 q |
| p16 | S3 Casca de banana | avanço | 424 px | +1 | dano 900, anda +120, puxa 0, livre em 106 q |
| p16 | S4 Estrelinha | avanço, 4 acertos | 344 px | -6 | dano 1000, anda +181, puxa 0, livre em 87 q |

**Corpo, alcance e mobilidade:**

| lutador | vida | L (início) | alcance M / H / 2M | anda | dash 20 q | pulo à frente | air dashes | pulo na parede | super pulo |
|---|---|---|---|---|---|---|---|---|---|
| p12 | 13300 | 3 q | 264 / 160 / 296 | 300 | 207 | 280 | 1 | não | 627 |
| p16 | 14000 | 3 q | 224 / 268 / 292 | 300 | 207 | 280 | 1 | não | 627 |

**Como a CPU joga** (1x1 nível 8 contra os outros 15, 4 lutas por rival; depende da CPU):

| lutador | vence | distância média | colado / longe | pulos/min | agarrões/min | S1/min | S2–S4/min | dano: normais | dano: S1 | dano: S2–S4 | dano: Super | dano: Ultra | dano: agarrão | dano: Clash | dano: outros |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| p12 | 50% | 214 px | 54% / 5% | 10.5 | 0.0 | 8.5 | 0.0 | 51% | 34% | 0% | 4% | 2% | 0% | 9% | 0% |
| p16 | 25% | 268 px | 37% / 11% | 6.2 | 0.2 | 9.4 | 0.0 | 45% | 33% | 0% | 6% | 5% | 1% | 10% | 0% |

**Janelas de reação** (o especial defensivo apertado de 30 quadros antes a 40 depois do aperto do rival; em quantos quadros ele anula o golpe, e em quantos ainda acerta):

| quem reage | contra | anula em | anula e acerta em |
|---|---|---|---|
| p12 S3 Fúria | p16 M a 150 px | 0 de 42 | 0 |
| p12 S3 Fúria | p16 2M a 150 px | 0 de 42 | 0 |
| p12 S3 Fúria | p16 S1 a 450 px | 0 de 42 | 0 |

**Entre si** (1x1, 20 lutas por par, metade de cada lado):

- p12 vence 85% de p16


### C01 sinergia: o assist de cada um e o que o assist rende para cada um

Como assist: alcance (distância do titular ao boneco em que acerta), início, antiaéreo (acerta o boneco
pulando a 250 px em quantos de 12 tempos), cobertura (vantagem do titular num H defendido com o assist
chamado junto, menos sem). Como parceiro: quanto o assist soma ao maior combo (sem barra, sem Super) de
Ryūga, Kōga, p09 e p11. Como titular: quanto o parceiro oficial soma ao maior combo dele.

| lutador | assist | alcance | início | antiaéreo | cobertura | soma ao combo de outros (média) | o oficial soma ao dele | nota sinergia (1–5) |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | S | 100–800 px | 15 q | 75% | +0 | +1025 | -135 (Yin-lu (p02)) | 3 |
| Yin-lu (p02) | S | 100–350 px | 18 q | 50% | +15 | +763 | +133 (Ryūga (p01)) | 3 |
| Kōga (p03) | S | 100–500 px | 19 q | 42% | +1 | +1249 | +282 (p04) | 2 |
| p04 | S | 100–350 px | 19 q | 25% | +10 | +1042 | +262 (Kōga (p03)) | 2 |
| p05 | S | 100–1000 px | 17 q | 33% | +8 | +854 | +773 (p06) | 4 |
| p06 | S | 100–850 px | 19 q | 50% | +27 | +484 | +744 (p05) | 5 |
| p07 | S | 100–150 px | 21 q | 0% | +13 | +1299 | +40 (p08) | 2 |
| p08 | S | 100–750 px | 19 q | 83% | +8 | +1509 | +1919 (p07) | 5 |
| p09 | S | 100–1000 px | 22 q | 75% | +14 | +493 | +3009 (p10) | 5 |
| p10 | S | 100–800 px | 17 q | 58% | +6 | +811 | +748 (p09) | 4 |
| p11 | S | 100–1000 px | 19 q | 92% | +1 | +423 | -274 (p12) | 3 |
| p12 | S | 100–100 px | 19 q | 0% | +0 | -198 | -78 (p11) | 1 |
| p13 | S | 100–100 px | 27 q | 0% | +0 | -198 | -100 (p14) | 1 |
| p14 | S | não acerta | – | 0% | +0 | -198 | -105 (p13) | 1 |
| p15 | M | 100–100 px | 17 q | 0% | +0 | +617 | -989 (p16) | 1 |
| p16 | S | 100–1000 px | 35 q | 83% | +18 | -296 | +288 (p15) | 4 |

### C01 fichas completas (sete dimensões, notas 1–5 por quintil do elenco)

| lutador | alcance | mobilidade | pressão | defesa | dano explosivo | controle de espaço | sinergia | números brutos |
|---|---|---|---|---|---|---|---|---|
| Ryūga (p01) | 1 | 1 | 1 | 1 | 5 | 1 | 3 | alcance M/H/2M 208/256/240 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -5.0, colado 33%; vida 14000, janela antiaérea total 131, L em 2 q; maior combo sem barra 6934; projétil 760 px/s, longe 14% |
| Yin-lu (p02) | 3 | 1 | 2 | 5 | 1 | 1 | 3 | alcance M/H/2M 264/276/220 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.5, colado 39%; vida 13440, janela antiaérea total 194, L em 3 q; maior combo sem barra 4798; projétil 700 px/s, longe 9% |
| Kōga (p03) | 2 | 1 | 5 | 2 | 2 | 4 | 2 | alcance M/H/2M 204/208/312 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 41%; vida 13720, janela antiaérea total 146, L em 3 q; maior combo sem barra 5501; projétil 950 px/s, longe 8% |
| p04 | 4 | 1 | 3 | 3 | 2 | 3 | 2 | alcance M/H/2M 260/260/288 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.5, colado 42%; vida 14420, janela antiaérea total 147, L em 3 q; maior combo sem barra 5466; projétil 800 px/s, longe 8% |
| p05 | 1 | 5 | 2 | 1 | 4 | 1 | 4 | alcance M/H/2M 276/204/220 px; anda 300, dash 207, pulo 280 px, 1 air dash, pulo na parede; vantagem média dos normais -3.3, colado 37%; vida 12880, janela antiaérea total 125, L em 3 q; maior combo sem barra 6041; projétil 1000 px/s, longe 13% |
| p06 | 2 | 1 | 2 | 1 | 3 | 3 | 5 | alcance M/H/2M 244/216/272 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 27%; vida 13300, janela antiaérea total 130, L em 3 q; maior combo sem barra 6005; projétil 480 px/s, longe 19% |
| p07 | 1 | 1 | 3 | 4 | 4 | 1 | 2 | alcance M/H/2M 264/228/192 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.0, colado 42%; vida 15120, janela antiaérea total 170, L em 4 q; maior combo sem barra 6411; projétil 700 px/s, longe 9% |
| p08 | 5 | 1 | 1 | 5 | 1 | 5 | 5 | alcance M/H/2M 284/432/200 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 16%; vida 13020, janela antiaérea total 195, L em 3 q; maior combo sem barra 4137; projétil 1100 px/s, longe 30% |
| p09 | 3 | 1 | 4 | 4 | 5 | 3 | 5 | alcance M/H/2M 276/196/300 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.0, colado 42%; vida 16100, janela antiaérea total 144, L em 4 q; maior combo sem barra 6924; projétil 600 px/s, longe 9% |
| p10 | 2 | 5 | 3 | 2 | 2 | 2 | 4 | alcance M/H/2M 284/196/252 px; anda 300, dash 207, pulo 280 px, 2 air dash; vantagem média dos normais -3.3, colado 41%; vida 13020, janela antiaérea total 146, L em 3 q; maior combo sem barra 5027; projétil 900 px/s, longe 9% |
| p11 | 1 | 1 | 1 | 1 | 1 | 5 | 3 | alcance M/H/2M 220/236/264 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 25%; vida 12600, janela antiaérea total 146, L em 3 q; maior combo sem barra 4407; projétil 950 px/s, longe 29% |
| p12 | 1 | 1 | 5 | 5 | 3 | 2 | 1 | alcance M/H/2M 264/160/296 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 54%; vida 13300, janela antiaérea total 198, L em 3 q; maior combo sem barra 5751; projétil 700 px/s, longe 5% |
| p13 | 5 | 1 | 5 | 4 | 5 | 4 | 1 | alcance M/H/2M 260/284/336 px; anda 170, dash 134, pulo 280 px, 1 air dash; vantagem média dos normais -3.3, colado 51%; vida 16100, janela antiaérea total 160, L em 4 q; maior combo sem barra 6809; projétil 600 px/s, longe 7% |
| p14 | 5 | 1 | 1 | 2 | 3 | 4 | 1 | alcance M/H/2M 256/324/332 px; anda 200, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -3.7, colado 40%; vida 13300, janela antiaérea total 148, L em 5 q; maior combo sem barra 5547; projétil 600 px/s, longe 7% |
| p15 | 4 | 1 | 4 | 3 | 1 | 5 | 1 | alcance M/H/2M 196/300/332 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 37%; vida 14000, janela antiaérea total 159, L em 3 q; maior combo sem barra 3650; projétil 900 px/s, longe 10% |
| p16 | 4 | 1 | 4 | 3 | 4 | 2 | 4 | alcance M/H/2M 224/268/292 px; anda 300, dash 207, pulo 280 px, 1 air dash; vantagem média dos normais -2.3, colado 37%; vida 14000, janela antiaérea total 147, L em 3 q; maior combo sem barra 6565; projétil 500 px/s, longe 11% |

### Matriz 1x1 16 × 16 (seção 7.3)

CPU nível 8 dos dois lados, 10 sementes por célula. Linha = lado esquerdo (P1), coluna = lado direito (P2);
célula = vitórias do lado esquerdo (de 10). Empates e tempo esgotado sem vencedor contam como não vitória.

| P1 \ P2 | p01 | p02 | p03 | p04 | p05 | p06 | p07 | p08 | p09 | p10 | p11 | p12 | p13 | p14 | p15 | p16 | total P1 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| p01 | **4** | 7 | 9 | 1 | 8 | 8 | 10 | 7 | 4 | 3 | 10 | 5 | 8 | 8 | 9 | 8 | 68% |
| p02 | 3 | **5** | 6 | 2 | 4 | 0 | 7 | 1 | 0 | 3 | 5 | 4 | 1 | 3 | 8 | 2 | 34% |
| p03 | 9 | 6 | **4** | 3 | 5 | 6 | 9 | 5 | 6 | 7 | 7 | 6 | 2 | 5 | 10 | 8 | 61% |
| p04 | 6 | 9 | 7 | **2** | 8 | 4 | 10 | 3 | 5 | 6 | 6 | 7 | 0 | 7 | 10 | 7 | 61% |
| p05 | 2 | 8 | 3 | 4 | **7** | 2 | 7 | 2 | 2 | 4 | 4 | 6 | 1 | 7 | 9 | 5 | 46% |
| p06 | 3 | 7 | 7 | 7 | 7 | **6** | 10 | 5 | 7 | 1 | 4 | 6 | 10 | 9 | 9 | 7 | 66% |
| p07 | 0 | 2 | 0 | 3 | 1 | 1 | **4** | 0 | 0 | 2 | 1 | 5 | 0 | 3 | 2 | 3 | 17% |
| p08 | 4 | 9 | 5 | 10 | 8 | 6 | 10 | **3** | 3 | 8 | 4 | 10 | 10 | 10 | 10 | 10 | 75% |
| p09 | 7 | 7 | 5 | 3 | 7 | 3 | 7 | 4 | **4** | 3 | 4 | 7 | 3 | 7 | 6 | 9 | 54% |
| p10 | 3 | 8 | 5 | 5 | 9 | 8 | 10 | 2 | 7 | **7** | 5 | 5 | 2 | 5 | 9 | 9 | 62% |
| p11 | 2 | 5 | 2 | 4 | 7 | 0 | 10 | 7 | 5 | 4 | **7** | 6 | 10 | 8 | 8 | 10 | 59% |
| p12 | 5 | 5 | 3 | 4 | 6 | 3 | 6 | 0 | 3 | 4 | 0 | **5** | 4 | 5 | 10 | 4 | 42% |
| p13 | 5 | 7 | 7 | 5 | 10 | 0 | 8 | 0 | 8 | 7 | 1 | 8 | **5** | 9 | 8 | 9 | 61% |
| p14 | 3 | 3 | 3 | 4 | 7 | 1 | 9 | 1 | 5 | 6 | 3 | 5 | 1 | **3** | 10 | 6 | 44% |
| p15 | 0 | 3 | 1 | 1 | 1 | 0 | 5 | 0 | 0 | 1 | 0 | 1 | 3 | 2 | **4** | 4 | 16% |
| p16 | 1 | 2 | 2 | 1 | 5 | 2 | 7 | 0 | 3 | 4 | 0 | 3 | 2 | 5 | 4 | **5** | 29% |

Viés de lado: o lado esquerdo vence 50% das 2560 lutas (47% nos espelhos: 75 de 160).

Espelhos (lado esquerdo vence, de 10):

Ryūga (p01) 4; Yin-lu (p02) 5; Kōga (p03) 4; p04 2; p05 7; p06 6; p07 4; p08 3; p09 4; p10 7; p11 7; p12 5; p13 5; p14 3; p15 4; p16 5.

Força contra o elenco (os dois lados somados, sem os espelhos) e a maior diferença de lado nos pares:

| # | lutador | vence | como P1 | como P2 |
|---|---|---|---|---|
| 1 | p08 | 77% | 78% | 75% |
| 2 | p06 | 68% | 66% | 71% |
| 3 | Ryūga (p01) | 67% | 70% | 65% |
| 4 | p04 | 63% | 63% | 62% |
| 5 | p13 | 62% | 61% | 62% |
| 6 | p11 | 61% | 59% | 64% |
| 7 | Kōga (p03) | 60% | 63% | 57% |
| 8 | p10 | 60% | 61% | 58% |
| 9 | p09 | 58% | 55% | 61% |
| 10 | p12 | 43% | 41% | 44% |
| 11 | p14 | 41% | 45% | 38% |
| 12 | p05 | 41% | 44% | 38% |
| 13 | Yin-lu (p02) | 37% | 33% | 41% |
| 14 | p16 | 30% | 27% | 33% |
| 15 | p15 | 17% | 15% | 19% |
| 16 | p07 | 16% | 15% | 17% |

Pares em que o lado muda o resultado em 6 ou mais de 10 (3 de 120):

- Ryūga (p01) × Kōga (p03): Ryūga (p01) vence 9 de 10 pela esquerda e 1 de 10 pela direita
- Yin-lu (p02) × p16: Yin-lu (p02) vence 2 de 10 pela esquerda e 8 de 10 pela direita
- p06 × p11: p06 vence 4 de 10 pela esquerda e 10 de 10 pela direita

## Anexo: Auditoria SŌKON, frente F: C04, D03, D04 e roteiro de playtest (T27/T28)

### Frente F — C04: p13, p14, p15 e p16 prontos para o torneio da história?

```text
ID: C04 / F12
STATUS: CONFIRMADO (conferência no repositório) · DECIDIR (liberar)
ARQUIVO E LINHAS: android/app/src/main/assets/historia/torneio/torneio.json:10-11 ("pronta": false);
  story-core Arcade.java:31-34 e Campanha.java:166-171, 272 (dupla não pronta: fora das oficiais do
  Arcade, fora dos adversários sorteados e só do lado do rival na chave); testes ArcadeTest:64
  ("as 6 prontas") e TorneioTest:76, 104.
CENÁRIO: conferência item a item no HEAD 3703d87a (characters/pNN/character.json, assets, docs,
  GeneratedCharacters) + testes do app que tocam nos quatro (CharacterSelectTest, MechanicsTest,
  ShinkaiTest, ShinkaiClashTest, CombatEngineTest, DefesaEGolpeAltoTest, DesgasteDaLutaTest,
  SpriteMotionTest, UltraDirectorTest, AnimacaoNosEspeciaisTest, EspeciaisContraAgachadoTest,
  ComandosPorPersonagemTest, CorrentesComboTest, ThrowTest): OK (229 testes).
RESULTADO: jogáveis por inteiro (todos os golpes, S1–S4, Super e Ultra), já selecionáveis no
  Arcade e no Modo Teste. Faltam, nos quatro, as animações de intro, vitória, derrota e provocação
  (que p01–p12 têm), o agarrão próprio em p13/p14/p15, nome no jogo para p14 e p16, e as
  assistências de p13, p14 e p15 quase não acertam (frente C, T17).
IMPACTO: liberar hoje põe os quatro na chave da história e no sorteio do Arcade sem intro nem
  comemoração (ficam parados no READY e na vitória) e com assist que não serve.
ALTERNATIVAS: (a) liberar agora como estão; (b) liberar depois do mínimo abaixo; (c) liberar
  p13+p14 antes (têm agarrão de golpe e Ultra cinemático), p15+p16 depois.
RECOMENDAÇÃO: (b). Ainda não liberar. O mínimo para liberar: nome do p14 e do p16 no jogo;
  INTRO e VICTORY dos quatro; assist de p13, p14 e p15 que acerte; o comentário do torneio.json
  ("ainda sem golpes") corrigido; ArcadeTest e TorneioTest atualizados junto com a flag.
  DEFEAT, TAUNT e agarrão próprio podem vir depois (têm substituto no motor).
TESTES DE REGRESSÃO: rodar story-core (ArcadeTest, TorneioTest, CampanhaTest) e o app inteiro
  depois de trocar a flag.
DEPENDÊNCIAS: F06/T17 (assistências), F12, T21 (duplas oficiais).
```

#### Lista de conferência

Legenda: **OK** no jogo e conferido; **PARCIAL** existe com ressalva; **FALTA** não existe;
**igual** ao resto do elenco (não é falta só destes quatro).

##### Golpes e animações de luta

| Item | p13 Krüger Stahl | p14 | p15 Natisuko | p16 | Onde |
|---|---|---|---|---|---|
| L, M, H, 2L, 2M, 2H, jL, jM, jH, j2H, 6M | OK | OK | OK | OK | `characters/pNN/character.json` → `moves` |
| Parado, andar, agachar, pulo, queda, dash, backdash, aterrissar | OK | OK¹ | OK | OK | `animations` (IDLE, WALK_*, CROUCH*, JUMP, FALL, RISE, LAND, DASH, BACKDASH) |
| Defesa em pé, agachado e no ar | OK | OK | OK | OK | DEFENSE_STAND / CROUCH / AIR |
| Dano em pé, agachado e no ar; queda, no chão, levantar | OK | OK | OK | OK | HIT_STAND / HIT_CROUCH / HIT_AIR, KNOCKDOWN, GROUNDED, GETUP |
| Golpe alto (→M) | OK | OK | OK | OK | `GOLPE_ALTO` |
| S1 | OK (investida) | OK (S1 no ar)¹ | OK (Soco Energizado) | OK (balão) | `fighter.energy`, `SPECIAL_ENERGY` |
| S2–S4 | OK (pedra, cabeçadas, salto) | OK (tornado, meia-lua, pisão) | OK (repel, rajada, voadora) | OK (caixa, banana, estrelinha) | `specialMoves`, `SPECIAL_S2..S4` |
| Super | OK | OK | OK | OK | `fighter.super`, `SUPER_WAVE` (p15 também `assets/supers/p15`) |
| Ultra | OK (sequência, 4 vídeos; rival de cabeça para baixo provisório) | OK (Tufão do Malandro)¹ | OK (Mugen Rendan) | PARCIAL (sem a abertura e a página de mangá: visual provisório) | `fighter.ultra`, `ULTRA_SEQ`; docs/personagens/pNN.md |
| Agarrão próprio (THROW_GRAB / THROW_TOSS) | PARCIAL (o motor usa o jab e o H) | PARCIAL (idem) | PARCIAL (idem) | OK | `animations`; `GameView.throwPose` faz o substituto |
| INTRO | FALTA | FALTA | FALTA | FALTA | p01–p12 têm; sem ela o lutador fica parado no READY (`GameView.java:1952`) |
| VICTORY (pose de vitória na luta) | FALTA | FALTA | FALTA | FALTA | p01–p12 têm (`GameView.java:4180`) |
| DEFEAT (derrota de pé) | FALTA | FALTA | FALTA | FALTA | p01–p12 têm (`GameView.java:4198`) |
| TAUNT (provocação do treino) | FALTA | FALTA | FALTA | FALTA | só o tutorial usa |
| Tela de vitória (vídeo em `assets/vitorias/`) | igual (só o Ryūga tem) | igual | igual | igual | `assets/vitorias/player_base` |

¹ p14: as tarefas de refino que ficaram abertas na sessão (S1 no ar, fumaça quadrada, Super e Ultra)
seguem pendentes de aprovação no aparelho; o pacote está completo.

##### Sistema, CPU e balanço

| Item | p13 | p14 | p15 | p16 | Onde |
|---|---|---|---|---|---|
| SHINKAI | POWER | TECHNIQUE | TECHNIQUE | SPEED | `Shinkai.typeOf` |
| Assistência | PARCIAL: S (investida) só acerta a 100 px | FALTA: o mergulho do S1 não acerta em nenhuma distância | PARCIAL: M só acerta a 100 px | OK (balão) | frente C, T17 |
| Estilo da CPU | tanque | chutes | equilibrado | equilibrado | `comboStyle` |
| Força contra o elenco (1x1, CPU) | 50% | 53% | 20% (penúltimo) | 22% | `docs/balanco/bateria.md` |
| Testes do motor que os cobrem | OK | OK | OK | OK | 229 testes nas classes listadas; todos passam |
| Comandos na lista da pausa | OK (gerada dos dados) | OK | OK | OK | `ListaComandos.ficha` |
| Teste dos comandos (cada comando solta o golpe certo) | FALTA | FALTA | FALTA | FALTA | `ComandosPorPersonagemTest.TABELA` vai só até o p12 |

##### Apresentação e história

| Item | p13 | p14 | p15 | p16 | Onde |
|---|---|---|---|---|---|
| Nome no jogo (`displayName`) | OK "Krüger Stahl" | FALTA ("p14") | OK "Natisuko" | FALTA ("p16"; a história já chama de "Bonnie Scarlet") | `character.json`, `historia/temporada.json` |
| Nome no elenco da história | OK | FALTA | OK | OK | `temporada.json` (`elenco`) |
| Rosto no seletor | OK | OK | OK | OK | `assets/selection/rostos/p13..p16.webp` |
| Arte grande no seletor | igual (só p01–p08 têm) | igual | igual | igual | `assets/selection/art/tela/` |
| Rosto da chave do torneio | OK | OK | OK | OK | `assets/historia/torneio/rostos/p13..p16.jpg` |
| Página do desafio no porto (ep03, Ryūga) | OK (`pag5_ryuga_p13.jpg`, dupla p13+p14) | — | OK (`pag5_ryuga_p15.jpg`, dupla p15+p16) | — | `assets/historia/ep03/` |
| Página do porto no lado do Kōga | igual (provisória para todas as duplas) | | | | `docs/historia/torneio.md` |
| Conversas na luta, narrador | igual (o narrador não usa nomes; conversas só no treino e no ep02) | | | | `historia/narrador.json`, `ep00/conversa_treino.json` |
| Sons e vozes | igual (sons de luta comuns a todos; ninguém tem voz) | | | | `assets/sons/luta/` |
| Tradução (en, es) | OK p13/p15/p16; p14 sem nome | | | | `historia/idiomas/` |
| Fichas em docs/personagens | PARCIAL: as tabelas antigas do Seedance ainda marcam ⬜ o que já está no jogo | PARCIAL (idem) | PARCIAL (idem) | PARCIAL (idem) | `docs/personagens/p13..p16.md` |

#### O que trocar a flag muda

`"pronta": true` põe a dupla no sorteio dos dois lados da chave da história (`Campanha`), entre os
adversários oficiais do Arcade (`Arcade.java:34`) e no sorteio das lutas variáveis (`Campanha.java:272`).
Junto com a troca: `ArcadeTest` (espera 6 oficiais), `TorneioTest` (espera dupla sem golpes só do
lado do rival) e o comentário do `torneio.json`.
