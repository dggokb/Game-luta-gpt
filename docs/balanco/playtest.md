# Roteiro do playtest com jogadores (T27, T28)

A auditoria deixou dois testes que só gente responde:
- **T27, identidade às cegas:** quem joga reconhece como cada personagem luta sem que ninguém
  explique?
- **T28, vontade de continuar:** o jogo dá vontade de jogar mais uma?

As etapas 1 a 5 de [ajustes.md](ajustes.md) também deixaram perguntas que a bateria não fecha,
porque a CPU joga dos dois lados:
- o Breaker com 20 quadros;
- o ritmo da TECHNIQUE no Clash;
- o TROCA PEDIDA do TAG;
- o p09 humano;
- a pressão do Kōga.

Este roteiro diz quem chamar, o que cada sessão faz, o que registrar e o que perguntar. Para cada
pergunta, diz também o que a resposta decide.

## O que precisa estar pronto

| Item | Situação hoje | Sem ele |
|---|---|---|
| Build com as etapas 1 a 5 | pronto | — |
| Humano contra a CPU no celular (Arcade, Modo Teste) | pronto | — |
| **Registro de entradas** (D04 1.3, [modo-treino-versus.md](../modo-treino-versus.md)) | não existe | a gravação da tela e as anotações do observador; o T24 (comandos no toque) fica sem número |
| **Registro das lutas em CSV** no aparelho (modo playtest, abaixo) | não existe; a bateria já grava (`BateriaBase.CABECALHO`) | o observador anota resultado, personagens e tempo numa folha |
| **Versus no PC com dois controles** (D04 2-A) | não existe | as perguntas "humano contra humano" (p09, Kōga, Breaker do lado de quem bate) ficam só contra a CPU nível 8, e a resposta vale menos |

Ordem recomendada: registro de entradas e CSV (1,5–2 dias), depois o versus no PC (3–4 dias), depois
o playtest. Dá para começar antes, só com humano contra CPU: as sessões 1, 2 e 4 funcionam, e a 3
roda pela metade.

## Jogadores

6 a 8 pessoas, cada uma com um código (J1, J2…). Nenhum nome no registro.

| Perfil | Quem | Quantos | Para quê |
|---|---|---|---|
| **A. Iniciante** | joga no celular, nunca jogou luta a sério | 2 | identidade sem vocabulário de luta; se o toque e o HUD se explicam sozinhos; vontade de continuar |
| **B. Casual de luta** | já jogou SF, KOF ou Tekken por diversão | 3 | o público principal; identidade, TAG, Clash |
| **C. Experiente** | sabe o que é punir, frame data, joga online | 2–3 | Breaker, p09 humano, pressão do Kōga; um deles faz o **D** |
| **D. Dono do p09** | um C que joga só de p09 nas sessões 3 e 4 | 1 (dentro do C) | o p09 humano que cutuca com o 2M |

- **Mesmo aparelho** para todos os A e B (um celular médio, não o melhor da casa).
- Os C jogam no celular **e** no PC com controle, para separar o que é do toque do que é do jogo.
- Ninguém do time joga; ninguém explica personagem antes da sessão 2.

## Sessões

Uma pessoa por vez com um observador, ~95 minutos. Para os C, dividir em dois dias (as sessões 3 e
4 cansam).

| # | Sessão | Tempo | O que acontece | Registra |
|---|---|---|---|---|
| 0 | Chegada | 5 min | combinado (gravação de tela, nada de nome), questionário de perfil | perfil |
| 1 | Primeiro contato | 15 min | sem explicação: o menu, o TUTORIAL e a primeira luta do Arcade | onde travou, o que perguntou, quanto tempo até a primeira luta, resultado |
| 2 | **Identidade às cegas (T27)** | 25 min | 6 personagens sorteados (abaixo), 3 min de Modo Teste com IA OFF e uma luta contra a CPU cada; o questionário de identidade logo depois de cada um | CSV das lutas, questionário |
| 3 | **Perguntas específicas** | 25 min | os cinco roteiros abaixo (os C fazem todos; os A e B, só TROCA PEDIDA e Clash) | CSV, registro de entradas, anotações |
| 4 | **Jogo livre (T28)** | até 20 min | Arcade, história ou versus, à escolha; a cada 5 min o observador pergunta "quer parar?" | quando parou, quantas "mais uma", o que escolheu |
| 5 | Saída | 10 min | questionário de vontade de continuar e conversa | questionário, frases |

### Sorteio da sessão 2

- Cada jogador recebe 6 dos 16, de modo que **cada personagem passe por pelo menos 3 jogadores**
  (com 8 jogadores, 48 passagens dão 3 por personagem).
- O sorteio fica numa planilha antes do dia, sempre com um de cada grupo de ritmo:
  - pressão (p03, p07, p12, p13, p16);
  - distância (p01, p08, p11, p14, p15);
  - os outros (p02, p04, p05, p06, p09, p10).
- p13 a p16 entram mesmo com `"pronta": false` (o Modo Teste e o Arcade já os têm), mas a
  resposta deles pesa no C04 (liberar p13–p16 no torneio): hoje eles não têm intro e a
  assistência de p13, p14 e p15 quase não acerta.

## O que registrar

### 1. As lutas em CSV

Um modo playtest no menu de teste grava uma linha por luta em `files/playtest/lutas.csv` no
aparelho. As colunas são as mesmas do `BateriaBase.CABECALHO`, para comparar com a bateria sem
conversão, mais as do playtest:

```
player_id,profile,session,device,input,human_side,
commit_sha,test_id,fighter,opponent,assist_partner,opponent_partner,starting_distance,
initial_super_bars,cpu_level,random_seed,frames_elapsed,hits,blocks,damage_dealt,damage_taken,
max_combo_hits,max_combo_damage,max_combo_frames,meter_spent,assists_called,tags_completed,
breakers_used,shinkai_activations,result,state_hash
```

- `input`: `toque` ou `controle`.
- `human_side`: 0, 1 ou `ambos` (no versus).
- `test_id`: o nome do roteiro (`S2-identidade`, `S3-breaker`…).
- `random_seed`: a semente da luta. Com a semente e o registro de entradas, a luta inteira pode
  ser repetida no motor sem animação (o motor é determinístico, A05). Assim, uma luta estranha
  no playtest vira um caso da bateria.

Custo: 0,5–1 dia; reaproveita o `Registro` da `BateriaBase`, que já conta tudo isso no motor.

### 2. O registro de entradas (T24)

É o mesmo histórico de entradas do treino (D04 1.3), gravado em arquivo quando o modo playtest
está ligado. Uma linha por mudança, não por quadro:

```
quadro,lado,direcao,botoes,comando_reconhecido,golpe_que_saiu
1832,0,2,,,
1840,0,3,,,
1846,0,6,L,S1,SPECIAL_ENERGY
1901,0,4,,,
1930,0,6,H,,HEAVY        <- carga que não saiu (botão virou golpe normal)
```

O que sai dele:
- quantos comandos saíram certos no toque;
- quantos botões saíram como golpe normal logo depois de um começo de comando: a "quase
  acertada", o número que a simulação do T24 estimou (85–100% certos e 20% das cargas perdidas);
- o tempo de reação depois de um Breaker, de um Clash e de um TROCA PEDIDA.

### 3. Os eventos que o motor já anuncia

O motor já solta cues para eles; basta gravar o quadro e o dado de cada um, numa linha do mesmo
arquivo das entradas com `golpe_que_saiu` = o nome do cue.

| Evento | Cue | Dado |
|---|---|---|
| Breaker | `COMBO_BREAKER` | quem quebrou; quadros até o primeiro golpe de cada lado depois do empurrão |
| SHINKAI e Clash | `SHINKAI`, fim do `ConfrontoShinkai` | tipos, empurrão por segundo de cada lado, quem venceu |
| Pedido de TAG | os textos do `GameView`: "TROCA PEDIDA…", "TROCA!", "TROCA CANCELADA: …" | quadro do pedido, quadro da troca ou motivo da queda |
| K.O. | fim da luta | vida restante e tempo |

### 4. O observador

Uma folha por sessão:
- **Hora e momento:** em que ponto da luta aconteceu.
- **Confusão:** a pessoa parou, perguntou ou olhou o HUD procurando algo.
- **Frase:** o que ela disse, palavra por palavra.
- **Olhar:** para onde olhou depois de um evento. TROCA PEDIDA: olhou para o botão? Clash: olhou
  para o anel?

E a gravação da tela, que sincroniza com o registro de entradas pelo quadro do motor, mostrado no
canto pelo modo playtest.

## Questionário de perfil (sessão 0)

1. Joga jogos de luta? (nunca / já joguei / de vez em quando / toda semana)
2. Quais? (livre)
3. Joga no celular com toque? Com controle?
4. Sabe o que é "combo", "defender agachado", "punir"? (marcar os que sabe)

## Questionário de identidade às cegas (T27, sessão 2)

Logo depois de cada personagem, **sem mostrar a ficha nem o nome do estilo**:

1. Em uma frase: como esse personagem gosta de lutar?
2. Marque **até 3**: de longe · de perto · rápido · pesado · agarra · pula muito · defensivo ·
   armadilhas · combos longos · bate forte · pressiona sem parar · contra-ataca · difícil de
   jogar.
3. De 1 a 5 (as notas das fichas do C01):
   - alcance;
   - mobilidade;
   - pressão;
   - defesa;
   - dano;
   - controle de espaço.
4. Parece com algum personagem de outro jogo? Qual?
5. Gostou de jogar com ele? (1 a 5)
6. Qual golpe você mais usou? Qual você não conseguiu fazer?

**Como pontuar:**
- **As notas:** compare com as notas do C01 ([auditoria.md](auditoria.md), "Fichas de
  identidade"). A nota bate se a diferença for de até 1.
- **As palavras:** compare com o arquétipo da ficha de cada personagem
  (`docs/personagens/pNN.md`). A palavra conta se combinar com ele (exemplos: p09 "agarra" e
  "bate forte"; Kōga "pressiona" e "de perto"; p08 "de longe").
- **Identidade clara:** a média do personagem fica acima de 4 notas batendo, com ao menos 1
  palavra certa.

O resultado é uma tabela de 16 linhas. Leitura:
- **Identidade apagada:** notas que não batem e nenhuma palavra certa. É o personagem para a
  frente A (identidade da CPU) olhar.
- **Identidade confusa:** a palavra certa, mas as notas trocadas. Em geral, o golpe que define o
  personagem não aparece. A pergunta 6 diz qual.

## Questionário de vontade de continuar (T28, sessão 5)

Comportamento, tirado da sessão 4 (vale mais que a resposta):
- minutos jogados até parar;
- quantas vezes disse "mais uma";
- o que escolheu jogar (Arcade, história, versus);
- se trocou de personagem.

Perguntas:
1. De 0 a 10: quanta vontade você tem de jogar de novo amanhã?
2. O que te fez querer parar? (livre)
3. Qual luta foi a mais **injusta**? Por quê?
4. Qual momento foi o mais **legal**?
5. Teve algo que você não entendeu na tela? (mostrar prints do HUD: barra, SHINKAI, TAG, anel)
6. Você continuaria a história para saber o que acontece? (para quem viu o modo história)
7. Indicaria para um amigo? (0 a 10)

**Como ler:**
- **Média da vontade:** abaixo de 6 nos B é sinal de problema de base, maior do que qualquer
  ajuste fino.
- **"Injusta":** as respostas se agrupam por causa: Breaker, Super, CPU que lê, personagem forte,
  toque. O grupo maior é a prioridade do balanço depois do playtest.

## As perguntas que sobraram das etapas 1 a 5

Cada roteiro diz o que a bateria já sabe, como jogar, o que olhar e o que a resposta decide. A
decisão final continua com o Diego: o playtest traz a evidência.

### Breaker com 20 quadros (etapa 1)

**O que a bateria diz:**
- o atordoamento caiu de 44 para 20 quadros;
- a corrida automática saiu;
- a CPU agora quebra 36% das chances (antes 15%);
- ainda sobra um L de troco em 65% das quebras: o alcance real do L passa da folga do empurrão.

**Roteiro (C e D):**
1. Modo Teste, VIDA P1 e VIDA CPU para encher as barras.
2. O jogador apanha de um combo da CPU nível 8 e quebra; 10 vezes. O que ele faz depois?
3. No versus: um C bate num combo conhecido, o outro quebra; 10 vezes de cada lado.

**O que olhar e registrar:**
- quadros do empurrão até o primeiro golpe de quem quebrou e de quem apanhou o Breaker;
- se quem quebrou acha o L sozinho ou só depois de alguém mostrar.

**Pergunta:**
- para quem quebrou: "valeu a barra?";
- para quem bateu: "deu para reagir depois do COMBO BREAKER?".

**Decide:**

| O que acontecer | Leitura | Decisão |
|---|---|---|
| Humanos não acham o L e dizem que vale a barra | 20 quadros funcionam | manter |
| Os C acham o L sempre e quem apanhou nunca consegue reagir | a vantagem é grande demais | voltar à alternativa do relatório: empurrão maior ou atordoamento menor |
| Ninguém usa o Breaker | o custo de 1 barra pesa mais que o Super | rever o custo no balanço |

### Ritmo da TECHNIQUE no Clash (etapa 2)

**O que a bateria diz:**
- a janela do pulso é de 5 quadros (83 ms);
- esmurrar o botão dá 45 de empurrão por segundo;
- no ritmo, 250;
- é preciso ver se o anel do pulso deixa o ritmo claro.

**Roteiro (todos):**
1. Modo Teste com um TECHNIQUE: p02, p07, p14 ou p15. A CPU é um POWER (p04).
2. VIDA P1 e VIDA CPU enchem as duas barras de SHINKAI.
3. O jogador liga o SHINKAI com o rival no dele: 5 Clashes seguidos, sem explicação.
4. Depois, uma frase só: "aperte quando o anel fechar". Mais 5 Clashes.

**O que olhar e registrar:**
- empurrão por segundo de cada Clash;
- se olhou para o anel;
- toques por segundo.

**Pergunta:** "o que você tinha que fazer no cabo de guerra?"

**Decide:**

| O que acontecer | Leitura | Decisão |
|---|---|---|
| A maioria dos A e B passa de 150 por segundo já nos 5 primeiros | o anel se explica | manter |
| Só passam de 150 depois da frase | falta clareza | o anel e o texto do Clash precisam dizer o ritmo; não muda a regra |
| Nem depois da frase | a janela é curta para o toque | janela de 5 para 7 quadros, só no toque; rodar a bateria de novo |

### TROCA PEDIDA no TAG (etapa 3)

**O que a bateria diz:**
- a troca que sumia sem aviso caiu de 31% para 0%;
- o HUD novo nunca foi visto no aparelho:
  - "TROCA PEDIDA…";
  - "TROCA: ESPERANDO VOCÊ SE SOLTAR";
  - "TROCA CANCELADA: APANHANDO / DEFENDENDO / CAÍDO";
  - o arco da recarga.

**Roteiro (todos):**
1. Uma luta 2x2 contra a CPU nível 5.
2. O observador pede: "chame o parceiro e, logo depois, troque com ele". Dez vezes ao longo da
   luta, inclusive apanhando.

**O que olhar e registrar:**
- quadro do pedido, da troca ou da queda e o motivo;
- se a pessoa leu o texto: o observador anota o olhar;
- se apertou o TAG de novo achando que não pegou.

**Pergunta:** mostrar um print de cada texto e do arco e perguntar "o que isso quer dizer?".

**Decide:**

| O que acontecer | Leitura | Decisão |
|---|---|---|
| Ao menos 2 em 3 explicam cada texto | o HUD funciona | manter |
| Apertam TAG de novo durante o "…" | o pedido aceito não está claro | o botão precisa mudar de cor, além do texto |
| Não entendem o motivo da queda | | ícone no lugar da palavra |

### p09 humano (etapa 4)

**O que a bateria diz:**
- p09 no 1x1 contra o elenco: 74% → 62%;
- p09 contra a Yin-lu: 90% → 68%;
- a queda veio da CPU, que agora agarra mais (o 2M caiu de 25% para 19% do dano);
- um humano que só cutuca com o 2M não muda;
- a velocidade (300 → 270) ficou para depois deste teste.

**Roteiro (D contra os C, no versus; sem versus, D contra a CPU nível 8):**
1. O D joga só de p09, cutucando com o 2M.
2. São 10 lutas contra cada C, com os personagens que o C escolher.
3. Depois, as mesmas 10 lutas com o D jogando de outro personagem de que gosta (o controle:
   separa o p09 do jogador).
4. Sem versus: 10 lutas contra a Yin-lu da CPU e 10 contra o elenco sorteado.

**O que olhar e registrar:**
- vitórias de p09 contra as do controle;
- a parte do 2M no dano, contada pelo registro de entradas;
- quantas vezes o rival foi atingido pelo 2M tentando entrar.

**Pergunta:**
- para quem enfrentou: "dava para passar do 2M dele?";
- para o D: "o 2M é seguro?".

**Decide:**

| O que acontecer | Leitura | Decisão |
|---|---|---|
| p09 vence 15 pontos ou mais acima do controle e o 2M passa de 35% do dano | o p09 humano ainda domina | o passo que ficou para depois: velocidade 300 → 270 (isolado, com a bateria de antes e depois) |
| Diferença menor que 15 pontos | | manter |

10 lutas por par têm muito ruído: a decisão pede a soma de todos os pares, não um par só.

### Pressão do Kōga (fora das etapas; decisão de não mexer)

**O que a bateria diz:**
- o Kōga ficou como está, por decisão;
- o H dele erra quem está agachado (15 de 16);
- o L e o M acertam o agachado só 4 e 5 de 16;
- no canto, o tempo preso sobe de 26% para 36%.

**Roteiro (C, no versus; sem versus, contra a CPU):**
1. Um C de Kōga pressiona o outro no canto por 60 s, 5 vezes.
2. Depois troca: o outro C pressiona com Ryūga (o controle).
3. Contra a CPU: o jogador de Kōga contra a CPU nível 8 defendendo; e o jogador defendendo contra
   o Kōga da CPU.

**O que olhar e registrar:**
- quantas vezes quem defende agacha e o golpe passa por cima;
- quanto tempo fica preso;
- como sai da pressão: pulo, Breaker, S2, pushblock.

**Pergunta:**
- para quem jogou de Kōga: "a pressão funcionou?" e "teve golpe que passou por cima?";
- para quem defendeu: "dava para sair?".

**Decide:** só relata. A decisão de não mexer no Kōga fica até o T25: rever as caixas na tela
quadro a quadro, com o DEBUG ou o CAIXAS do treino. O playtest diz se o problema é sentido. Se os
dois C disserem que o chute "passa por cima", o T25 vira prioridade.

### Comandos no toque (T24, junto com a sessão 3)

Para todos, 3 minutos no Modo Teste com IA OFF. O jogador faz 10 vezes cada comando do
personagem dele:
- ↓↘→;
- →↓↘;
- ↓↙←;
- uma carga, se o personagem tiver.

O registro de entradas conta os acertos. Se a carga perder mais de 20% como na simulação, o
buffer de carga do toque é o primeiro ajuste. Antes de mexer, confirmar com o registro.

## Depois do playtest

1. Uma tabela por pergunta, com o número de cada jogador e o perfil, no fim deste arquivo.
2. As lutas estranhas, pela semente e pelo registro de entradas, viram casos da bateria
   (`BateriaAntesDepois`).
3. Cada decisão que mudar alguma coisa segue a regra das etapas:
   - mudança isolada;
   - a mesma bateria antes e depois;
   - a tabela antes/depois em [ajustes.md](ajustes.md);
   - a aprovação antes de aplicar.
