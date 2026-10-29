# Frente H4: personagens fracos (p14, p15, p16, p07), p15+p16 e o Aparar do Ryūga

Este relatório sai de uma cópia do HEAD `5dedf956`:

- só o Java, os `character.json` e as ferramentas, em `scratchpad/h4`;
- um ramo git por ajuste, em `scratchpad/h4w/<ajuste>`.

A base de comparação é o próprio `5dedf956`, que coincide com `ad/10_g1b.tsv`, `fin/equipe_final.md` e `fin/comp_final.md`. Cada ajuste foi medido sozinho contra essa base, e depois o conjunto final foi medido junto. As medidas por ajuste são:

- a suíte inteira (`-t`);
- a `BateriaAntesDepois` completa;
- o T21 (`BateriaEquipe t21`);
- a matriz 16 × 16 (`BateriaComparacoes matriz`).

As sementes são as mesmas do relatório da lista G. As tabelas completas estão em `bal/H4_tabelas.md`.

Não houve aumento de dano em nenhum golpe. As mudanças são de função:

| Personagem | O que mudou |
|---|---|
| p16 | cancelamentos |
| p15 | o repel contra projétil e o empurrão do Super defendido |
| Ryūga | o Aparar |

Um ajuste de CPU no p14 foi testado e rejeitado, porque piorou o personagem.

## Como diagnostiquei

Escrevi ferramentas de teste só para esta frente. Elas ficam na cópia e não entram nos diffs:

- **`AnaliseH4 <id> 4`:** roda o personagem contra os outros 15 (nível 8, 4 sementes por rival, metade de cada lado).
  - Por golpe, conta quantas vezes sai e se acerta, é defendido, erra ou se ele **apanha no meio do golpe**.
  - Também mostra de que estado e de qual golpe do rival vem o dano levado.
- **`VantH4 <id> <gap> <golpes>`:** vantagem na defesa e no acerto contra o Ryūga e acertos que entram; também mede o Super, já com barra.
- **`PuloH4`:** em quantas combinações de distância e quadro um pulo à frente com jL/jM/jH/j2H acerta o Ryūga em pé.
- **`PapeisH4`:** a função que a CPU lê em cada especial (`CpuSpecials`), com início e alcance real.
- **`SondaH4`:** dano de rotas curtas.
- **`FichaGolpesH4`:** ficha de quadros, dano, alcance e mecânica de cada golpe.

### O que a análise mostrou

O dano que **mais** pesa contra os quatro fracos é o projétil do rival:

| Personagem | Dano de projétil levado por luta |
|---|---|
| p07 | 5601 |
| p15 | 4711 |
| p14 | 3542 |
| Ryūga (referência) | 3306 |

Nos 4, os golpes lentos apanham no meio, na proporção de vezes em que saem:

| Personagem | Golpes que apanham no meio |
|---|---|
| p15 | H (17 q) 50%, 2M (16 q) 43%, 6M 80%, S3 68%, S4 60%, Super 45%, **S2 (repel) 80%** |
| p16 | balão S1 (26 q) 42%, caixa S2 81%, Super 65% |
| p07 | S1 32%, S4 48%, 2H 32%, Super 41% |

## Por que p15+p16 dá 2% (urgente)

A dupla junta os dois piores do elenco: na base, a p15 vence 13% no 1x1 e a p16, 24%.

No 2x2 contra as sete duplas oficiais, precisar vencer dois rivais com dois personagens fracos multiplica a desvantagem. Com outros parceiros, a p15 fica em 9–16% (Yin-lu 11%, p05 9%, p10 16%). **A âncora é a p15.**

As causas, com números da base:

1. **p15 contra quem atira (S1):**
   - leva 4711 por luta de projétil;
   - o S1 dela vai só até 320 px e sai em 14 q (o golpe só pega no 20º quadro);
   - a resposta do kit, o repel, **custava o golpe inteiro (64 q)** mesmo desfazendo a bola: 39 quadros presa depois de desfazer. Defender custaria 14.
   - Resultado: o repel "apanhava no golpe" 80% das vezes. A ferramenta anti-zoner rendia menos que defender.
2. **Super da p15 defendido: −83.**
   - O empurrão de 60 px por pancada defendida tira o rival do alcance do chute final, que sai no vazio, e ela fica aberta.
   - Também não combina: depois do H o rival vai para a parede, o Super não avança e erra (`M H SUPER` rende o mesmo que `M H`).
   - Por isso **o DHC de p15+p16 não sai** (T20).
3. **p16 sem cancelamento:**
   - o M e o H dela não cancelam em especial nem em Super;
   - é a única do elenco assim, porque o pacote nasceu sem as rotas;
   - o Super rendia 3% do dano dela.
4. **Assists:**
   - o da p16 é o balão de 35 q, o mais lento do elenco;
   - o da p15 (2H) é antiaéreo de perto;
   - nenhum dos dois segura o rival para o outro entrar.
5. **Golpes lentos dos dois:** os normais da p15 e o S1, a caixa e o Super da p16 perdem a troca quase sempre (tabela acima).

Os ajustes abaixo atacam 1, 2 e 3: **p15+p16 vai de 2% para 9%**. O 4 e o 5 ficam para decisão.

## Os ajustes, cada um sozinho

### H4-p16: M e H cancelam em especial e Super (`H4_p16.diff`)

- **Dados:** em `characters/p16/character.json`:
  - o M ganhou `cancelInto` com S, S2, S3 e S4, e as `rotasProprias` ganharam o mesmo, para o `mecanicas.py` não desfazer;
  - o H ganhou `cancelInto` S, SUPER, S2, S3 e S4, igual ao resto do elenco.
- **Java:** `GeneratedCharacters.java:1079–1080`, os dois `cancelInto`.
- **Função:** `M H SUPER` rende 2700, contra 1260 de `M H`, e `M H S` rende 2025. No CPU × CPU, o Super por minuto foi de 0,4 para 1,0.
- **Teste:** `CancelamentosP16Test` (2 testes). Falha na base (`M → S`) e passa depois.
- **Matriz:** p16 24 → 27%; o resto ficou dentro de ±2.
- **T21:** p15+p16 2 → 4%, p13+p14 39 → 41%.
- **Bateria fixa:** `combo.max_1barra.p16` 5615 → 4487 é efeito da busca em feixe: com mais ações, a poda muda e acha uma rota pior. A rota antiga continua possível.

### H4-p15a: o repel que desfaz projétil termina logo (`H4_p15.diff`)

- **Código:**
  - `CombatConfig.java:221–225`: `repelShotFreeFrames = 6`;
  - `CombatEngine.java:2784–2788`: o repel que desfaz a bola marca `hitCutFrame` para 6 quadros depois;
  - `CombatEngine.java:3808` (`counter`): zera o corte, para o repel que ainda lê um golpe responder inteiro.
- **Só a p15 tem repel.** Na base, ela ficava presa 39 quadros depois de desfazer; agora fica até 8.
- **Função:** S2 "apanha no golpe" 80% → 5%. A bola desfeita sem dano deixa a p15 livre logo depois.
- **Teste:** `RepelProjetilTest` (2 testes).
  - O repel que desfaz a bola deixa a p15 livre em até 8 quadros. Na base, falha com 39.
  - O repel no vazio continua custando o golpe inteiro.
- **Matriz:** p15 13 → 14%; o resto ficou igual.
- **T21:** p15+p16 2 → 5%.
- **Bateria fixa:** 1x1.p15 8 → 10%, 2x2.p15 23 → 26%.
- **Limite:** a CPU usa o repel pouco (1,1 vez por luta), por isso o efeito no placar é pequeno. Para um humano, o repel passa a valer mais que defender contra zoner.

### H4-p15b: o Super defendido empurra 10 px, não 60 (`H4_p15super.diff`)

- **Mudança:** em `characters/p15/character.json`, o `super.attack.pushbackOnBlock` vai de 60 para 10. No Java, a mesma troca na linha 1031 (`pushback(60,10)`).
- **Sem mudança de tempo, dano nem acerto:** o tempo do vídeo continua, o dano é o mesmo e o empurrão no acerto continua 60.
- **Função:** defendido a 200 px, as 5 pancadas pegam na defesa (antes 4, o chute final saía no vazio). A vantagem foi de −83 para −9, igual à da p16 e do p08.
- **Testei e descartei** blockstun 56: um chute passa pela defesa e a vantagem fica perto de −3, além do pedido.
- **Teste:** `SuperP15DefendidoTest`. Na base, falha com 4 pancadas.
- **Matriz e T21:** sem mudança fora do ruído (p15 13%, p15+p16 4%). A CPU usa o Super 0,6 vez por luta, e o problema maior é ele não combinar (ver DECIDIR).

### H4-Ryūga: o Aparar vira aparar de verdade (`H4_ryuga.diff`)

**Mecânica nova (flag `parry`):** uma postura de contra-golpe que **só bate no que leu**. Ela lê qualquer golpe corpo a corpo do 4º ao 13º quadro, anula e responde na hora com os mesmos 900 de dano. No vazio não bate e fica aberta o golpe inteiro (42 q). Antes era um golpe comum de 20 q e 900 de dano.

Não é ganho de graça: ela ganha a leitura e perde o golpe comum.

**Código:**
- `AttackDefinition.java:73–77` e `:137`: `parry` e `parry()`.
- `CombatEngine.java:2516–2517`: o golpe de quem tem repel ou parry só sai se leu.
- Dados:
  - `characters/player_base/character.json`, S3 com `"mecanica": {"contra": {"le": "tudo", "apara": true, "frames": [4, 13]}}`;
  - `tools/sprites/mecanicas.json`, S3 do player_base com `contra`/`apara` e a descrição;
  - `tools/sprites/build_characters.py:197–202` e `:707`: aceita `apara` e emite `.parry()`.
- `GeneratedCharacters.java:1157`: o S3 com `.counter(ANY,4,13).parry()`.

**Efeitos que exigiram mudança:**
- **`CpuReachTable.java`:** regenerada com o `GeraAlcanceCpu`. Só mudou o `player_base S3`: 202/170 → −1/−1, porque o S3 não acerta mais o boneco parado.
- **`TutorialDirector.java` (242–251, 354–391):** a lição de especial de quem só bate lendo agora tem o boneco atacando (M a cada 70 quadros), e o exemplo aperta no tempo da janela.
  - Antes, a lição "ESPECIAL S3" do Ryūga não teria como ser cumprida.
  - A lição do S2 da p15 (repel) também passa a ser cumprível: era a mesma situação, sem teste antes.
- **`EspeciaisContraAgachadoTest`:** a linha do `player_base S3` saiu, porque ele não bate mais sozinho. O `AparadorTest` confere que ele lê o 2L.

**Testes:**
- `AparadorTest` (2 testes):
  - lê L, 2L e M, não leva nada e a resposta acerta;
  - no vazio não acerta o rival colado e fica aberto ≥ 40 q.
- Na base, os 2 falham.
- `TutorialDirectorTest`, `CpuReachTest` e `CpuSpecialsTest` passam.

**Medidas:**
- **Função na CPU:** o Aparar foi de 0 usos por minuto (golpe lento, só para punir) para 0,4 por minuto, com 60% de leitura que acerta.
- **Matriz:** Ryūga 61 → 59% (ruído; o Aparar não deu ganho de graça).
- **T21:** Kōga+p04 55 → 57%; o resto dentro de ±1.

### H4-p14: baixo longo no neutro da CPU, testado e REJEITADO (`H4_p14.diff`, só como registro)

- **Diagnóstico:** as ferramentas do p14 funcionam na mecânica:
  - S4 pisão de onda: +1 na defesa, acerta a 300 px;
  - S2 tornado: 5 acertos, −12;
  - S3 meia-lua: −4;
  - Super: acerta 81%;
  - os pulos com golpe acertam tanto quanto os do Ryūga e do p08 (`PuloH4`).
- **O que falta é uso pela CPU:**
  - o S4, com 17 q, passa do limite de 16 q do neutro;
  - o S2 e o S3 lançam, e a CPU os lê como antiaéreo.
- **Teste:** o S4 entrou no neutro de longe (`AiController.java:72–74`, `1408–1412`, `1466–1468`).
- **Resultado:** p14 31 → **27%**; p09+p10 79 → 75%, Ryūga+Yin-lu 64 → 61%.
- **Conclusão:** o pisão no neutro piora o p14. **Fora do conjunto final.**

### p07: sem ferramenta quebrada nos dados

- O S1 (tapa de luva) já usa a medida real do `CpuReach`.
- O Super de capangas fica +78 na defesa.
- O que o p07 perde são trocas com golpes de 12 q (S1, S4 e 2H apanham 32–48%) e o projétil do rival (5601 por luta).
- Não encontrei golpe que não cumpra a função declarada. **Não mexi nos dados dele.** Ele já tinha subido de 16% para 32% com a G1.

## Conjunto final (p16 + p15a + p15b + Ryūga, sem o p14): `H4_final.diff`

Suíte: **567 de 567 passam.** A matriz e o T21 foram medidos com o conjunto inteiro.

| personagem | base | final |
|---|---|---|
| p08 | 78% | 78% |
| p04 | 64% | 64% |
| p09 | 64% | 64% |
| p11 | 63% | 63% |
| p10 | 63% | 62% |
| Ryūga | 61% | 59% |
| Kōga | 58% | 58% |
| p13 | 55% | 54% |
| p05 | 51% | 51% |
| p06 | 51% | 52% |
| p12 | 47% | 47% |
| Yin-lu | 45% | 45% |
| p07 | 32% | 32% |
| p14 | 31% | 30% |
| p16 | 24% | **27%** |
| p15 | 13% | **14%** |

| dupla oficial | base | final |
|---|---|---|
| Ryūga + Yin-lu | 64% | 64% |
| Kōga + p04 | 55% | 57% |
| p05 + p06 | 36% | 34% |
| p07 + p08 | 63% | 63% |
| p09 + p10 | 79% | 79% |
| p11 + p12 | 64% | 63% |
| p13 + p14 | 39% | 39% |
| p15 + p16 | **2%** | **9%** |

**Bateria fixa:** fora do ruído, mudaram 1x1.p15 (8 → 12%), 2x2.p15 (23 → 27%) e `breaker.cpu_vs_nunca` (57 → 53%, dentro de ±5). O `combo.max_1barra.p16` cai pelo efeito de busca já explicado.

**Leitura:** o resto do elenco não mudou. O ganho dos fracos é pequeno porque as correções são de função, sem dano. O que ainda prende p15, p16 e p14 está na lista abaixo e precisa de decisão.

## DECIDIR (precisa de aprovação)

1. **O Super da p15 não combina.**
   - O H (quebra na parede) joga o rival longe e o Super não avança; o M não cancela em Super. Logo ela nunca fecha combo com Super, e o DHC da dupla não sai.
   - Opções:
     - (a) o Super avança até o rival, como o do p14;
     - (b) o M cancela em Super;
     - (c) manter.
2. **Normais lentos da p15.**
   - H 17 q, 2M 16 q e 6M 20 q apanham 43–80% das vezes. É o tempo dos vídeos, pedido antes.
   - Opção sem mexer na duração da animação: passar quadros do início para a recuperação (H 17 → 13, 2M 16 → 12), com o mesmo dano e o mesmo total.
   - Isso muda a sensação do golpe, por isso não apliquei.
3. **p16:**
   - o balão (S1) tem início de 26 q, o assist é de 35 q e o Super de 29 q; os três perdem a troca;
   - a banana (44 q) só pune: foi pedida lenta.
   - Opções: início do balão perto de 18 q e assist mais rápido, ou manter.
4. **p14 (CPU):**
   - o S2 tornado, de alcance longo, é lido como antiaéreo porque lança;
   - o pisão no neutro piorou o p14 (testado acima);
   - se quiser, o próximo teste seria uma função de CPU por golpe nos dados (por exemplo `"funcaoCpu": "poke"` no S2).
5. **Assists de p15/p16:** nenhum segura o rival para a troca. Só faria sentido depois do item 3.

## Integração (para quem for aplicar)

- Aplicar em ordem: `H4_p16.diff`, `H4_p15.diff`, `H4_p15super.diff`, `H4_ryuga.diff`, ou só o `H4_final.diff`. Todos são relativos ao `5dedf956`.
  - O `H4_p14.diff` **não** entra.
- O `GeneratedCharacters.java` foi editado à mão, igual ao que o gerador emitiria:
  - p16 (2 `cancelInto`);
  - p15 (`pushback(60,10)` do Super);
  - player_base (S3 `.mechanics(...counter(ANY,4,13).parry())`).
  - **Rode o `build_characters.py --check`.** O `mecanicas.py --write` não pôde ser conferido na cópia, porque sem a arte ele recalcula alcances.
- No overlay do `jp.sh`/`jpw.sh`, incluir `AttackDefinition.java` e `TutorialDirector.java`, que não estavam no `ovl_main.txt`. Usei `scratchpad/ovl_h4r.txt`.
- Sugestão de doc: `docs/combat-engine.md`, mecânica de contra-golpe: `apara` só bate lendo; o repel que desfaz a bola acaba em `repelShotFreeFrames`. Não editei docs.
