# Combos de assinatura: Ryūga, Yin-lu, Kōga e Bonnie

Os dois primeiros refeitos. Tudo aqui está no jogo, conferido no motor (`ComboRyugaTest`,
`ComboYinluTest`, `GolpeDeCorridaTest`) e gravado no PC. Os outros entram quando cada um for refeito,
no mesmo formato.

A base é a mesma para os 16 (L → M → H subindo de força, o 2H que lança para o super pulo e, no ar,
jL → jM → pulo → jL → jM → j2H; `mecanicas.md` 4b). Em cima dela cada personagem tem um **jeito de
combar** e combos que só saem nele, feitos de quatro camadas:

1. **Sequência própria:** uma ordem de botões que só ele tem (as target combos do Street Fighter).
2. **Golpe de comando:** o `→ + H` de cada um, com animação própria e uma função no combo.
3. **O especial como ponte:** o que cada especial faz no meio do combo (lança, joga na parede,
   quica no chão), em vez de só derrubar.
4. **O corpo e o parceiro:** o pulo, o peso, o que ele faz no ar e o que o assist dele faz no combo.

Mais o **golpe de corrida** (corre + H), um por personagem.

Regra: cada combo escrito aqui é um teste no motor, que aperta a sequência e confere que todas as
pancadas emendam. O que não emendar volta para cá antes de ir para o jogo.

---

## Ryūga (p01): curto, pesado, termina no poder

**Jeito de combar:** poucas pancadas e muito dano no fim. Ele não faz malabarismo: confirma com um
golpe e entrega o combo a um especial (a Onda, o Punho ascendente, o Chute giratório). É o
fundamento do Ryu: o combo é curto e quem erra perto dele paga caro. A identidade é **levar para o
canto**: o Chute giratório carrega o rival até a parede, e da parede ele termina no alto.

### O que está no jogo

| Camada | O que é | Onde |
|---|---|---|
| 1. Sequência | **Soco duplo da casa: M → M**, só nele (o estilo de combo dele virou `shoto`: a base mais o M que repete uma vez). O segundo M entrega para o especial ou para o → + H | `mecanicas.json` combos.estilos.shoto |
| 2. Comando | **→ + H: Soco no plexo.** Um passo curto e dois socos (quadros 9 e 16), 900 de dano. Sai do neutro e emendado depois do L, do 2L e do M (rota da base para quem tiver um 6H). Só cancela depois do segundo soco, em S1, S2, S3 ou Super. Animação própria desde 12/11 (o um-dois do vídeo; primeiro soco no estômago, segundo no peito). Fora dessas rotas (depois do 2M, por exemplo), → + H continua sendo o H | `mecanicas.json` player_base.golpeComando; `CancelSystem.COMMAND_HEAVY` |
| 3. Ponte | **S3 (Chute giratório): o último chute joga na parede** em vez de derrubar. Ele corre atrás (segurando →) e pega a volta no S2 | `especiais.S3.lancar`: wallBounce |
| 3. Ponte | **S2 (Punho ascendente) pega quem volta da parede** (do H ou do S3) e joga em arco | já cancelava; conferido |
| 4. Parceiro | **A Onda do assist segura o rival em pé** 45 quadros (a Onda dele segura 20). Quem chama o Ryūga corre e recomeça a corrente inteira. Sem isso o rival se solta antes (o teste confere as duas coisas) | `fighter.assist.segura` no pacote; `Fighter.assistShot` |

### Os combos (conferidos no motor, contra o Ryūga parado)

| # | Nome | Sequência | Acertos | Dano |
|---|---|---|---|---|
| R1 | Fundamento | 2L, 2L, M → S1 (Onda) | 4 | 1560 |
| R2 | Soco duplo | L, M, M → S2 (ou → S1), de perto: o S2 é curto e pega de 90 a 135 px | 4 | 1892 |
| R3 | Leva à parede | L, M, M → S3 (quatro chutes, parede) → corre → S2 | 8 | 2382 |
| R4 | Punição na parede | L, M, H (parede) → corre → 2H → super pulo → jL, jM, j2H | 7 | 3044 |
| R4b | Punição com barra | L, M, H (parede) → corre → SUPER | 8 | 3820 |
| R5 | Plexo | 2L, → + H (dois socos) → S2 | 4 | 1909 |
| R5b | Plexo com barra | 2L, → + H → S2 → SUPER | 9 | 4209 |
| R6 | Parceiro | (Yin-lu em campo) L, M → TAG (Onda do Ryūga segura) → corre → L, M, M → S4 | 11 | 2526 |

### Combo curto que emenda em outro

O dono: "não tem problema ter combo curto se eu conseguir emendar outros combos depois". Jeitos de
emendar, conferidos no motor (`ComboRyugaTest.comboCurtoEmendaEmOutroCombo`):

| Como emenda | Exemplo | Acertos |
|---|---|---|
| Parede do S3 | L, M, M → S3 (parede) → corre → L, M, M, → + H, S2 | 12 |
| Assist no meio | L, M, M → TAG (o chute da Yin-lu lança) → super pulo → jL, jM → pulo → jL, jM, ↓jH | 9 |
| Troca no Super (DHC) | saiu em 17/11: o Super da Yin-lu virou contra-golpe (só sai se o rival bater) | — |
| Depois do ↓jH, relançando | 2L, M, 2H → super pulo → jL, jM, ↓jH → 2H → super pulo → jL, jM, ↓jH | 10 |
| Depois do ↓jH, no chão | 2L, M, 2H → super pulo → jL, jM, ↓jH → corre → L, M | 8 |

Os dois últimos são novos: quem crava o ↓jH agora desce rápido e pousa com o rival ainda quicando
(regra da base, vale para todos; `docs/combat-engine.md`). Depois da parede do H continua dando S2,
2H com o combo no ar ou Super; recomeçar no chão ali não ficou bom: mexer na volta da parede ganhava
esse recomeço e perdia o S3 → combo novo e parte do combo no ar, então a parede ficou como estava.

**OTG (todos):** dentro de um combo, ↓ + M no rival que acabou de cair o levanta para
continuar (um por combo; `docs/combat-engine.md`). No Ryūga: 2L, M, rasteira → ↓ + M → L, M, M, S2
(7 acertos); e depois do S2 em arco, corre → ↓ + M → S2 (6). Assim nem o S2 fecha o combo de vez.

O que não emenda: depois da parede do S3 o rival volta alto e longe demais para o 2H (o que pega é o
S2); o 2H com o combo no ar fica para a parede do H (R4). O S2 joga em arco, então o Super fica fora
do R3; no R5b ele entra porque o S2 pega o rival ainda no chão.

---

## Yin-lu (p02): longo, fluido, sobe duas vezes

**Jeito de combar:** muitas pancadas leves, uma escorrendo na outra, e o combo **continua no ar**
(ela é a garça). Cada pancada tira pouco; o dano vem da quantidade. A identidade é **subir duas
vezes**: ela lança, termina no ar quicando o rival no chão e lança de novo. É o contrário do Ryūga:
longo, leve e aéreo.

### O que está no jogo

| Camada | O que é | Onde |
|---|---|---|
| 1. Sequência | **Fluxo da garça: L, L, M, M, H** (palma, palma, bico, bico, crescente na parede). Já emendava com o estilo dela (M > M); ficou conferido | estilo `chutes` |
| 2. Comando | **→ + H: Joelho da garça.** Dois golpes (quadros 8 e 15), o segundo lança; cancela em pulo (o super pulo atrás), especial ou Super. Abre o combo no ar sem o 2H. Animação própria | `mecanicas.json` p02.golpeComando |
| 3. Ponte | **S4 (Chute rodopiante): cinco chutes, um a cada passada da perna no giro, e o último joga em arco para a frente** (13/11; antes lançava para cima, e o dono achou estranho no giro de ponta-cabeça). No meio da arena o rival cai derrubado longe: ela corre e pega no chão com o ↓ + M (OTG) ou alcança no ar com o → + H. **Perto da borda, quica na parede** e volta numa altura alcançável (Joelho e combo no ar, ou S2) | `pancadas.json` p02.S4.arco: altura 130, distância 380, parede |
| 4. Corpo | **Pulo duplo no meio do combo no ar** (jM, jM → pulo → jL, jM, jM) | já existia |
| 5. Sobe duas vezes | Termina no ar com o ↓jH, pousa (desce rápido) e lança de novo no 2H | regra da base |
| 6. Parceiro | **O assist dela é o chute que lança (2H):** entra até o rival e o joga para o alto; quem está em campo dá o super pulo e continua no ar. O contrário do Ryūga, que segura no chão | `fighter.assist.move`: 2H |

### Os combos (conferidos no motor, contra o Ryūga parado)

| # | Nome | Sequência | Acertos |
|---|---|---|---|
| Y1 | Fluxo da garça | L, L, M, M, H (parede) | 5 |
| Y2 | Arco da garça | L, L, M, M, S4 (5 chutes, arco para a frente) → corre → ↓ + M no caído → L, M, M | 13 |
| Y2b | Arco da garça no ar | L, L, M, M, S4 → corre → ↓ + M no caído → 2H → super pulo → jL, jM, jM → pulo → jL, jM, jH | 17 |
| Y3 | Arco na parede | (no canto) L, L, M, M, S4 (quica na parede) → → + H → super pulo → jL, jM, jM → pulo → jL, jM, jH | 16 |
| Y3b | Arco na parede com S2 | (no canto) L, L, M, M, S4 (quica na parede) → S2 | 12 |
| Y4 | Joelho | L, L, M, M, → + H (lança) → super pulo → jL, jM, jM → pulo → jL, jM, jM, ↓jH | 13 |
| Y5 | Sobe duas vezes | L, L, M, M, → + H → super pulo → jL, jM, jM, ↓jH → 2H → super pulo → jL, jM, jM → pulo → jL, jM, jH | 17 |
| Y6 | Assist | (Ryūga em campo) L, M, M → TAG (o chute dela lança) → super pulo → jL, jM → pulo → jL, jM, ↓jH | 9 |

Regras da base que vieram com a Yin-lu (valem para todos): todo golpe que lança deixa pelo menos 44
quadros de atordoamento (`launchHitstunFrames`); golpe de várias pancadas só cancela em pulo depois
da última; e o pulo logo depois de lançar, saindo do ar, é o super pulo que persegue.

---

## Kōga (p03): o brigão de perto, termina no especial (21/11)

**Jeito de combar:** pressão colada, pouco pulo. O combo fica no chão e fecha num especial que
leva o rival: a Garra do tigre (três cortes), a Investida da voadora (7 acertos, derruba) ou o
Uppercut da casa (joga em arco; ele corre, pega no chão e recomeça). O **H joga longe na parede**:
no meio da arena o combo sai do **M** para o especial; no canto, o H quica e volta para os dois
socos. A bica (→ + M) é golpe alto: abre o combo de surpresa, não serve no meio dele (sai em 20
quadros e o atordoamento é curto).

### Os combos (conferidos no motor, contra o Ryūga parado: `ComboKogaTest`)

| # | Nome | Sequência | Acertos |
|---|---|---|---|
| K1 | Garra | L, L, M, M, S1 Garra do tigre (os três cortes) | 7 |
| K2 | Estômago | L, M, M, → + H (dois socos) → S3 Investida da voadora | 12 |
| K3 | Uppercut da casa | L, M, M, S2 (arco) → corre → ↓ + M no caído → L, M, → + H, S3 | 9 |
| K4 | Sobe | 2L, M, 2H (lança) → super pulo → jL, jM → pulo → jL, jM, jH | 8 |
| K5 | Bica | → + M (golpe alto), S2, Super | 15 |
| K6 | Voadora correndo | corre → H (voadora, lança) → corre → 2H → super pulo → jL, jM → pulo → jL, jM, jH | 7 |
| K7 | Fecho de Super | L, L, M, M, S1 (três cortes), Super | 20 |
| K8 | Canto | (no canto) L, L, M, M, H (quica na parede), → + H, S3 | 14 |

O que não emendou e ficou de fora: depois do H no meio da arena (vai longe para a Garra e o S2);
L → → + M (o L não cancela no golpe alto); a bica no meio do combo; depois da voadora correndo e do
↓ + M no caído, o combo cai no segundo golpe (sobra a versão no ar, K6).

**A CPU:** alterna, um combo de cada vez, entre a rota do estilo (L, M, M, 2H, combo no ar e S2) e
as duas de assinatura, K2 e K1 (`ComboBook.ASSINATURA`). Para isso a CPU passou a saber até onde
o especial de comando chega no meio do combo (o alcance mais o avanço, `CpuSpecials`): antes ela
achava que o S3 nunca alcançava e trocava pelo Super. Teste: `ComboKogaTest`.

---

## Bonnie (p16): o truque, o combo muda de direção

**Jeito de combar (16/11):** a palhaça que engana. O combo não sobe reto como o do Ryūga nem sobe
duas vezes como o da Yin-lu: ele **muda de direção**. O M volta para o L (o rival acha que acabou),
a Estrelinha lança e ela pula atrás, e a Caixa surpresa arma uma pegadinha cuja luva **devolve o
rival para ela**. Sem vídeo novo: tudo com os golpes que ela já tinha.

### O que está no jogo

| Camada | O que é | Onde |
|---|---|---|
| 1. Sequência | **Esquete do palhaço: L, M, L, M, H.** O M cancela de volta no L (até 2 M na corrente); o H joga na parede | estilo `circo` (`mecanicas.json` combos) |
| 2. Comando | ainda não tem (→ + H sai como o H): espera vídeo | — |
| 3. Ponte | **S4 Estrelinha:** a última passada lança e cancela em pulo (o super pulo persegue). **S2 Caixa surpresa:** o botão escolhe onde cai (L perto, M meio, H longe), vira mina que abre com o rival em cima, a luva crava o rival no chão e ele quica no lugar; o H, ↓ + M ou → + M dela chuta a caixa, que voa, para na frente do rival e abre (a luva soca) | `mecanicas.json` p16.especiais (`lancar` groundBounce, `cancelaEm`, `distancias`, `armadilha.mina`), `CombatEngine.chutarCaixas` |
| 4. Corpo | **Pulo de circo:** 10% mais alto e 10% mais tempo no ar | `mecanicas.json` p16.unica.lutador |
| 5. Parceiro | **O assist é a Caixa surpresa** (pavio curto): ela entra, joga a caixa e a luva devolve o rival para quem está em campo | `character.json` p16.fighter.assist.move: S2 |

### Os combos (conferidos no motor, contra o Ryūga parado)

| # | Nome | Sequência | Acertos |
|---|---|---|---|
| B1 | Esquete do palhaço | L, M, L, M, H (parede) | 5 |
| B2 | Esquete e corre | L, M, L, M, H (parede) → corre → 2H → super pulo → jL, jM → pulo → jL, jM, jH → Balão | 12 |
| B2b | Esquete no canto | (no canto) L, M, L, M, H (quica) → 2H → super pulo → jL, jM, jH → Balão | 10 |
| B3 | Estrelinha | L, M, S4 (4 passadas, a última lança) → super pulo → jL, jM, jH → Balão | 10 |
| B3b | Estrelinha com pulo no ar | L, M, S4 → super pulo → jL, jM → pulo → jL, jM, jH → Balão | 12 |
| B3c | Esquete na Estrelinha | L, M, L, M, S4 → super pulo → jL, jM → pulo → jL, jM, jH → Balão (desde a Estrelinha rápida, 16/11) | 14 |
| B4 | Caixa e 2H | Caixa (L, perto) → L, M, L (o rival chega na luva, quica) → 2H → super pulo → jL, jM → pulo → jL, jM, jH → Balão (16/11: com os normais mais rápidos, o L do esquete cabe antes do 2H) | 11 |
| B4b | Caixa e Estrelinha | Caixa (L) → L, M (quica) → S4 → super pulo → jL, jM → pulo → jL, jM, jH → Balão | 12 |
| B4c | Esquete chuta a caixa | Caixa (M) → L, M, L, M, H (parede; o H chuta a caixa, que volta no rival) → corre → 2H → super pulo → jL, jM → pulo → jL, jM, jH → Balão | 13 |
| B5 | Armadilha | (rival a ~265 px) Caixa (L) → a mina abre, ele quica → corre → 2H → super pulo → jL, jM, jH → Balão | 6 |
| B5b | Chute de longe | (rival a ~600 px) Caixa (L) → ↓ + M chuta a caixa, que voa até ele → corre até bem perto → 2H → super pulo → jL, jM, jH → Balão | 5 |
| B7 | Casca e Super | (rival a ~350 px) Casca → corre em cima, escorrega, a rasteira o faz quicar → Super | 3 |
| B6 | Assist | (Natisuko em campo) L, assist (a caixa da Bonnie) → L, M, H → S4 | 7 |

### Combos da dupla, com a Natisuko (16/11)

O assist da Natisuko é o 2H, que lança: a Bonnie chama no meio do combo e sobe atrás. Gravados no
PC com o mesmo número de acertos do teste (`ComboBonnieTest.duplaDoCirco`).

| # | Nome | Sequência | Acertos |
|---|---|---|---|
| D1 | Dupla do circo | (Bonnie) L, M, L, M, H (parede) → corre → assist da Natisuko (lança) → super pulo → jL, jM → pulo → jL, jM, jH → Balão | 12 |
| D2 | Caixa e Natisuko | (Bonnie) Caixa (M) → L, M, L, M, H (chuta a caixa) → corre → assist da Natisuko → super pulo → jL, jM → pulo → jL, jM, jH → Balão | 13 |
| D3 | Natisuko e a caixa | (Natisuko) L, M → assist da Bonnie (a caixa) → L, M, L, M, H | 8 |

A troca no Super (DHC) entre as duas não saiu no motor: fica para quando a dupla for refeita
junto.

Gravados no PC (B1, B3, B4, B6). Nas gravações o começo é andando até o rival, então a distância e
os acertos mudam um pouco em relação ao teste; o que vale é o teste.

## Golpe de corrida: corre + H

Um vídeo por personagem (`dash_golpe` do dono, H3, início na pose da corrida e fim na guarda),
montado em `montagens/<id>.json` com o trecho fixo (quem anda é o motor) e o golpe "DH" no
`mecanicas.json` (`golpeCorrida`, avanço em `unica.golpes.DH`). Sai da corrida sem parar; quem não
tem o golpe dá o H. Teste: `GolpeDeCorridaTest`.

| Quem | Golpe | O que faz | Combo conferido (R7 / Y7 nas fichas) | Acertos |
|---|---|---|---|---|
| Ryūga | **Ombrada** | 850 de dano, 12 quadros até o ombro; **joga na parede mesmo sozinha** (correu, acertou, combo) | Ombrada → parede → corre → 2H → super pulo → jL, jM → pulo → jL, jM, ↓jH (ou → corre → Super: 6) | 7 |
| Yin-lu | **Carrinho da garça** | baixo (defende agachado), dois acertos (o 2.º derruba), desliza ~200 px | Carrinho → corre → ↓ + M no caído → L, M, M, S4 | 11 |
| Yin-lu | **Carrinho no caído** (16/11) | no rival caído no combo, o 1.º chute levanta (o OTG dela) | 2L, M, 2M → corre → Carrinho → L, 2H → super pulo → jL, jM, jH, S4 | 14 |

Os dois são arriscados na defesa (ficam uns 9 e 13 quadros em desvantagem), como golpe de corrida
costuma ser: a Ombrada empurra para longe e o Carrinho é baixo.

## Lado a lado

| | Ryūga | Yin-lu |
|---|---|---|
| Tamanho do combo | curto (3 a 5 pancadas) | longo (8 a 12 pancadas) |
| Dano por pancada | alto | baixo |
| Onde termina | no especial ou no Super, no chão ou no canto | no ar, ou subindo de novo |
| Especial no meio | Chute giratório leva à parede | Chute rodopiante lança |
| Sequência própria | M → M (soco duplo) | L, L, M, M, H (fluxo) |
| Golpe de corrida | Ombrada (parede) | Carrinho da garça (baixo, derruba) |
| `→ + H` | Soco no plexo (dois golpes, cancela em especial) | Joelho da garça (dois golpes, lança) |
| Corpo | pula padrão, cai médio | pulo duplo no meio do combo |

**Para não ficar forte demais:** a Yin-lu tem o combo mais longo, mas cada pancada dela tira menos
(dano 0,9× e golpes de 270 a 720) e o peso leve deixa os combos contra ela mais fáceis. O Ryūga
tira mais em menos pancadas, mas o combo dele depende de estar perto e de acertar o primeiro golpe.
Os números finais saem dos testes do motor e do celular.

## No ar: o combo aéreo fecha num especial (15/11)

Desde 15/11 todo especial sai também no ar, com o lutador **parado no ar enquanto o golpe dura**
(`docs/personagens/comandos.md`, "No chão / no ar"), e jM e jH cancelam em S1, S2 a S4 e Super
em todos. Com isso a corrente aérea da base (jL → jM → jH) ganha um fim: o especial no alto. Um
por personagem, conferido no motor (`ComboNoArTest`, contra o Ryūga parado, 3 barras):

| Personagem | Combo | Acertos | Dano |
|---|---|---|---|
| Ryūga (p01) | L, M, 2H, super pulo, jL, jM → S3 Chute giratório | 9 | 2568 |
| Yin-lu (p02) | L, M, 2H, super pulo, jL, jM, jH → S4 Chute rodopiante | 11 | 2818 |
| p03 | L, M, 2H, super pulo, jL, jM → S2 Punho em sequência | 7 | 2458 |
| p04 | L, M, 2H, super pulo, jL, jM → S2 Cabeçada | 6 | 2845 |
| p05 | L, M, 2H, super pulo, jL, jM, jH → S2 Shuriken | 7 | 2876 |
| p06 | L, M, 2H, super pulo, jL, jM → S2 Eco de sombra | 6 | 2303 |
| p07 | L, M, 2H, super pulo, jL, jM, jH → S4 Chute giratório baixo | 7 | 3761 |
| p08 | L, M, 2H, super pulo, jL, jM, jH → S2 Giro ascendente | 8 | 3031 |
| p09 | L, M, 2H, super pulo, jL, jM, jH → S1 Machado | 7 | 3798 |
| p10 | L, M, 2H, super pulo, jL, jM, jH → S1 Machadinho | 7 | 2581 |
| p11 | L, M, 2H, super pulo, jL, jM, jH → S1 Bisturi | 7 | 2431 |
| p12 | L, M, 2H, super pulo, jL, jM, jH → S2 Salto de garras | 7 | 3521 |
| p13 | L, M, 2H, super pulo, jL, jM → S1 Investida do colosso | 9 | 3059 |
| p14 | L, M, 2H, super pulo, jL, jM, jH → S2 Chute tornado | 11 | 3032 |
| p15 | L, M, 2H, super pulo, jL, jM, jH → S3 Rajada de punhos (desde 16/11; a voadora sozinha não pega no ar, voadora + chute girando sim: 7) | 7 | 2567 |
| p16 | L, M, 2H, super pulo, jL, jM, jH → S1 Balão | 6 | 2906 |

O Super também fecha no ar em quem tem barra (o do Ryūga: L, M, 2H, super pulo, jL, jM, jH →
Super, 11 acertos). Gravado no PC (Ryūga e Yin-lu); não visto no celular.

## Em dúvida

1. **Y5 (sobe duas vezes):** um combo de 17 pancadas pode passar do ponto. O escalonamento de dano do jogo
   (cada pancada a mais tira menos) segura o total, mas vale sentir no celular.
