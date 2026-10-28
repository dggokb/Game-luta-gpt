# Modo treino com situações e versus local (especificação, D04)

Pedido da auditoria (D04, F13, T24): um laboratório para repetir situações e um versus para duas
pessoas, antes de pensar em netcode. Este documento diz o que o código já tem, o que falta, a ordem
e o custo de cada parte. Nada aqui está implementado.

Custo em dias de trabalho de uma pessoa no código, com os testes JVM de cada parte. Estimativa de
ordem de grandeza, não compromisso.

## O que já existe e se aproveita

| Peça | O que faz hoje | Uso no treino ou no versus |
|---|---|---|
| `CombatEngine` determinístico, 60 quadros por segundo, `FighterInput` por quadro | A mesma entrada a partir do mesmo estado dá a mesma luta (provado em `BateriaBase determinismo`) | **Gravar e tocar** o boneco é só guardar e repetir a lista de `FighterInput` |
| `FighterInput.copyFrom`, `clearPresses` | Copia a entrada do quadro | A gravação guarda uma cópia por quadro |
| MODO TESTE (`MenuInicial`, `GameView`) | Luta livre com IA OFF (boneco parado), botões IA ON/OFF, DEBUG, VIDA P1, VIDA CPU, DUPLA, HISTÓRIA, TUTORIAL | É a casa do treino: os botões novos entram aqui |
| `DebugOverlay` | Caixas de corpo (verde), empurrão (amarelo), golpe ativo (vermelho), hurtbox do golpe (azul), raio dos projéteis; quadro do motor, estado, buffer, combo; **tabela de frame data com a vantagem no acerto e na defesa** | Já é o "mostrar caixas" e a base do "quadros de vantagem" |
| `TelaAjusteCaixas` | Ajuste das caixas quadro a quadro sobre a animação | Fica como ferramenta de dev; não entra no treino do jogador |
| `TutorialDirector`, `DemoDirector` | Escrevem a entrada do boneco (o tutorial) ou dos dois lados (as demos) em Java puro, testado | O "tocar a gravação" segue o mesmo molde: um diretor que preenche a entrada do lado 1 |
| `MotionParser` (histórico de direções com o quadro de cada uma) | Reconhece os comandos | Fonte do **histórico de entradas** na tela |
| `CombatEngine.restage`, `FighterState.life`, `superMeter`, `shinkai`, `guard` | Recoloca os dois e mexe nos recursos (os botões VIDA já fazem) | Base das **situações salvas** sem precisar fotografar o motor inteiro |
| `CharacterSelect.open(p1, tag1, p2, tag2)` | Escolhe as **duas** duplas (4 escolhas) | Já é a seleção do versus; falta saber **quem** escolhe cada lado |
| `HardwareInput`, `Teclas`, `Controles` (PC, Jamepad) | Teclado e controle (PC e Bluetooth no Android) num mapa só, para o jogador 1 | Base do jogador 2 no PC e no controle |
| `PadInput`, `ControlsLayout` | Toque com vários dedos (`MAX_TOUCHES`) num layout para um jogador | O versus no mesmo celular precisa de um segundo layout |
| `BattleFlow` (READY, GO, K.O., tempo, 2x2) | A luta de verdade quando a IA está ligada | O versus usa o mesmo fluxo, com o lado 1 humano |

## Parte 1 — Treino com situações (laboratório)

### 1.1 Gravar e tocar o boneco (1,5–2 dias)

- **Comandos:** o botão GRAVAR troca o controle para o boneco: o jogador joga com ele até 10 s, e a
  entrada de cada quadro vai para uma lista. TOCAR repete a lista no boneco, uma vez ou em laço
  (LAÇO).
- **Durante o toque:**
  - o jogador joga normalmente com o lado dele;
  - a opção "defender depois do golpe" faz o boneco segurar a defesa quando a gravação acaba.
- **Implementação:** classe `GravacaoBoneco` (Java puro, como o `DemoDirector`) com `gravar(FighterInput)` e
  `preencher(FighterInput out)`, ligada no `GameView` no mesmo lugar do `tutorial.fillDummy`.
- **O que testa:** gravar uma sequência, tocar duas vezes a partir do mesmo estado e comparar o hash do
  estado (o `BateriaBase` já faz isso).
- **Lado da gravação:** a gravação fica relativa à frente do boneco (as direções passam por
  `MotionParser.relative`). Assim o boneco faz a mesma coisa nos dois lados da tela.

### 1.2 Situações salvas (2–3 dias)

Uma situação é um ponto de partida conhecido: posição dos dois, quem olha para onde, vida, barras do
time, SHINKAI (barra e se está ligado), guarda e, opcionalmente, o canto e a gravação do boneco.
Até 8 situações por personagem, guardadas no aparelho (`SharedPreferences`, JSON curto).

| Campo | Fonte hoje |
|---|---|
| x dos dois, lado | `CombatEngine.restage(x1, x2)` |
| vida, barra, guarda, SHINKAI | `FighterState.life`, `superMeter`, `guard`, `shinkai` / `shinkaiFrames` |
| canto | x perto de `Arena.LEFT_BOUND` / `RIGHT_BOUND` |
| gravação do boneco | a lista da 1.1 |

**Não** fotografar o estado inteiro do motor (combo em andamento, projétil no ar): isso exigiria
copiar `CombatEngine`, `CombatFighter`, sessões e projéteis, um trabalho grande e frágil. A situação
começa sempre do neutro, com os dois livres; o que vem depois vem da gravação.

Botão RECOMEÇAR (ou o L+M segurado no controle) volta para a situação. Situações de fábrica
para ajudar quem começa:
- canto;
- meio a 180 px;
- longe a 600 px;
- o rival no ar (pulo gravado);
- defesa contra a pressão do personagem.

### 1.3 Histórico de entradas na tela (1 dia)

Uma coluna à esquerda, como nos jogos de luta: cada linha é uma direção ou um botão, com quantos
quadros durou. Tira do `MotionParser` (as direções com o quadro de cada uma) e das teclas
apertadas no `FighterInput`. Ao lado de cada comando reconhecido, o nome do golpe que saiu
(S2, SUPER…), e em vermelho quando o botão saiu como golpe normal. **Também é o registro do T24**: o mesmo
histórico grava em arquivo quando o "registro de entradas" do playtest está ligado
([balanco/playtest.md](balanco/playtest.md)).

### 1.4 Caixas e vantagem (1 dia)

- **Caixas:** o DEBUG já desenha as caixas; falta deixar só as caixas (sem o texto de dev) para o
  jogador, num botão CAIXAS do treino.
- **Vantagem:**
  - o `DebugOverlay` já calcula a vantagem do frame data;
  - falta medir a **vantagem que aconteceu**: o número de quadros entre o atacante e quem defendeu
    voltarem a agir depois de cada golpe, como a frente B mediu (T09);
  - o número aparece em cima do personagem: verde se positivo, vermelho se negativo.
- **Dano do combo:** acertos, dano e escala ficam num quadro, com o dano total depois que ele acaba.

### 1.5 Opções do boneco (0,5–1 dia)

O boneco tem um único jeito de defender, escolhido entre:
- parado;
- defende tudo;
- defende o primeiro golpe;
- defende depois do primeiro;
- aleatório.

Outras opções:
- sai do chão com tech;
- quebra o combo (Breaker) sempre, nunca ou aleatório;
- pushblock;
- barras infinitas;
- vida que volta sozinha.

Quase tudo é a `Mao` da bateria levada para o jogo: a `defender` da `BateriaBalanco` já é a defesa
perfeita.

**Total da parte 1: 6–8 dias.** Ordem: 1.1 → 1.3 → 1.2 → 1.4 → 1.5. Gravar e o histórico primeiro,
porque servem também ao T24 e ao playtest.

## Parte 2 — Versus local para duas pessoas

### Onde jogar

| Forma | Como | Custo | Recomendação |
|---|---|---|---|
| **A. PC com dois controles** | O `Controles` (Jamepad) já lê vários controles; hoje todos viram o jogador 1. Controle 1 → lado 0, controle 2 → lado 1; teclado com dois mapas (WASD+JKL e setas+numpad) | 1,5–2 dias | **Primeiro**: mais barato e serve ao playtest do T27/T28 |
| **B. Celular com dois controles Bluetooth** | `KeyEvent.getDeviceId()` separa os controles; o primeiro que apertar um botão vira P1, o segundo P2. `HardwareInput` passa a ter uma instância por lado | 2–3 dias | Segundo |
| **C. Celular, os dois no toque, na mesma tela** | Dois layouts espelhados (cada um com metade da tela, D-pad e botões menores) | 4–6 dias e risco de jogabilidade (mãos se cruzam, botões pequenos) | Só se houver pedido; testar com tablet antes |
| D. Dois celulares (Bluetooth ou Wi-Fi) | Netcode com rollback | Fora de escopo (a auditoria pede para não planejar netcode ainda) | — |

### O que muda no código (comum a A, B e C)

1. **Entrada do lado 1 vinda de uma pessoa** (1 dia):
   - hoje o `GameView` enche `opponentInput` com `ai.fill(...)`;
   - um campo `ladoDoisHumano` troca isso por um segundo `PadInput` ou `HardwareInput`;
   - o resto do motor já trata os dois lados igual.
2. **Seleção dos dois lados** (1 dia):
   - `CharacterSelect.open` já pede as 4 escolhas;
   - falta cada cursor responder ao seu controle (P1 nas duas primeiras, P2 nas duas últimas) e o
     rótulo P1/P2.
3. **Fluxo** (0,5 dia):
   - VERSUS no menu inicial;
   - `BattleFlow` em modo batalha;
   - placar de vitórias por lado;
   - revanche com a mesma seleção ou volta para a seleção.
4. **HUD** (0,5–1 dia):
   - nomes P1/P2 nas barras;
   - os botões de teste ficam escondidos;
   - a pausa é de quem apertou.
5. **CPU** (0 dia): fica desligada; o `AiController` não é chamado para o lado 1.

**Total da parte 2:** A = 3–4 dias; A + B = 5–7 dias; C fica à parte.

## Ordem sugerida

| # | Parte | Custo | Por quê |
|---|---|---|---|
| 1 | 1.1 Gravar e tocar | 1,5–2 d | É a base do laboratório e da repetição de situações |
| 2 | 1.3 Histórico de entradas e registro em arquivo | 1 d | Fecha o T24 (comando no toque) com dados do aparelho |
| 3 | 2-A Versus no PC com dois controles | 3–4 d | Destrava o playtest com duas pessoas (T27/T28, pressão do Kōga, p09 humano) |
| 4 | 1.2 Situações salvas | 2–3 d | Repetir canto, pressão e antiaéreo |
| 5 | 1.4 Caixas e vantagem para o jogador | 1 d | Leitura do jogo; T25 com o jogador olhando |
| 6 | 1.5 Opções do boneco | 0,5–1 d | Treino de saída de pressão e de Breaker |
| 7 | 2-B Versus no celular com controles | 2 d | Depois que o PC provar o fluxo |
| 8 | 2-C Toque dividido | 4–6 d | Só com pedido e teste em tablet |

## Testes

- **Gravar e tocar:** o hash do estado é igual em duas repetições da mesma situação
  (`GravacaoBonecoTest`, no molde do `BateriaBase determinismo`).
- **Situação salva:** salvar, carregar e conferir posição, vida, barras, SHINKAI e lado.
- **Histórico:** um comando feito por roteiro (o `motion` do `ComandosPorPersonagemTest`) aparece na
  ordem certa e com o golpe que saiu.
- **Versus:** dois `FighterInput` roteirizados, um por lado, sem `AiController`; o K.O. de cada lado dá a
  vitória certa; a seleção com dois controles confirma as 4 escolhas na ordem.
