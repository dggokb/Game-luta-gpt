# Combos de assinatura: Ryūga e Yin-lu

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
| Troca no Super (DHC) | 2L, → + H, S2, Super → DHC (Super da parceira) | 14 |
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

## Golpe de corrida: corre + H

Um vídeo por personagem (`dash_golpe` do dono, H3, início na pose da corrida e fim na guarda),
montado em `montagens/<id>.json` com o trecho fixo (quem anda é o motor) e o golpe "DH" no
`mecanicas.json` (`golpeCorrida`, avanço em `unica.golpes.DH`). Sai da corrida sem parar; quem não
tem o golpe dá o H. Teste: `GolpeDeCorridaTest`.

| Quem | Golpe | O que faz | Combo conferido (R7 / Y7 nas fichas) | Acertos |
|---|---|---|---|---|
| Ryūga | **Ombrada** | 850 de dano, 12 quadros até o ombro; **joga na parede mesmo sozinha** (correu, acertou, combo) | Ombrada → parede → corre → 2H → super pulo → jL, jM → pulo → jL, jM, ↓jH (ou → corre → Super: 6) | 7 |
| Yin-lu | **Carrinho da garça** | baixo (defende agachado), dois acertos (o 2.º derruba), desliza ~200 px | Carrinho → corre → ↓ + M no caído → L, M, M, S4 | 11 |

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

## Em dúvida

1. **Y5 (sobe duas vezes):** um combo de 17 pancadas pode passar do ponto. O escalonamento de dano do jogo
   (cada pancada a mais tira menos) segura o total, mas vale sentir no celular.
