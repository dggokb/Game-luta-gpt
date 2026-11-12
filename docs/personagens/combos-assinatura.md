# Combos de assinatura (12/11): Ryūga e Yin-lu

> **Ryūga: no jogo (12/11)**, com os combos conferidos no motor (`ComboRyugaTest`) e gravados no PC.
> Decisões do dono: manter a base de todos e somar sequência própria depois do começo do combo,
> especial como ponte, golpe de comando e parceiro no combo; M → M como sequência; o → + H entra já,
> com a animação do H, porque ele é que vai ditar a animação nova. **Yin-lu: ainda proposta**
> (entra quando for a vez dela); os outros, quando cada um for refeito.

Pedido: "todo mundo comba igual". Hoje a corrente é a mesma para os 16 (L → M → H subindo de força,
o 2H que lança para o super pulo e, no ar, jL → jM → pulo → jL → jM → j2H; `mecanicas.md` 4b). O
estilo de cada um só acrescenta uma ou duas rotas, e os especiais quase sempre só derrubam. Com
golpes diferentes, o jogador ainda aperta a mesma sequência em todos.

A ideia: cada personagem tem um **jeito de combar** e 2 ou 3 **combos de assinatura** que só saem
nele. Eles são feitos de quatro camadas:

1. **Sequência própria:** uma ordem de botões que só ele tem (as target combos do Street Fighter).
2. **Golpe de comando:** o `→ + H` (e o `→ + M`) de cada um, com uma função no combo. **Precisa de vídeo.**
3. **O especial como ponte:** o que cada especial faz no meio do combo (lança, joga na parede,
   quica no chão), em vez de só derrubar.
4. **O corpo:** o pulo, o peso e o que ele faz no ar.

As camadas 1, 3 e 4 entram agora, só com dados e com as animações que existem. A 2 entra com os
vídeos; até lá o `→ + H` continua sendo o H.

Para conferir: quando aprovado, cada combo escrito aqui vira um teste no motor de verdade, que
aperta a sequência e confere que todas as pancadas emendam. O que não emendar volta para cá
antes de ir para o jogo.

---

## Ryūga (p01): curto, pesado, termina no poder

**Jeito de combar:** poucas pancadas e muito dano no fim. Ele não faz malabarismo: confirma com um
golpe e entrega o combo a um especial (a Onda, o Punho ascendente, o Chute giratório). É o
fundamento do Ryu: o combo é curto e quem erra perto dele paga caro. A identidade é **levar para o
canto**: o Chute giratório carrega o rival até a parede, e da parede ele termina no alto.

### O que está no jogo (12/11)

| Camada | O que é | Onde |
|---|---|---|
| 1. Sequência | **Soco duplo da casa: M → M**, só nele (o estilo de combo dele virou `shoto`: a base mais o M que repete uma vez). O segundo M entrega para o especial ou para o → + H | `mecanicas.json` combos.estilos.shoto |
| 2. Comando | **→ + H: Soco no plexo.** Um passo curto e dois socos (quadros 9 e 16), 900 de dano. Sai do neutro e emendado depois do L, do 2L e do M (rota da base para quem tiver um 6H). Só cancela depois do segundo soco, em S1, S2, S3 ou Super. Por enquanto com a animação do H; o vídeo dele vem depois. Fora dessas rotas (depois do 2M, por exemplo), → + H continua sendo o H | `mecanicas.json` player_base.golpeComando; `CancelSystem.COMMAND_HEAVY` |
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
| R6 | Parceiro | (Yin-lu em campo) L, M → TAG (Onda do Ryūga segura) → corre → L, M, M → S4 | 8 | 2526 |

### Combo curto que emenda em outro (12/11)

O dono: "não tem problema ter combo curto se eu conseguir emendar outros combos depois". Jeitos de
emendar, conferidos no motor (`ComboRyugaTest.comboCurtoEmendaEmOutroCombo`):

| Como emenda | Exemplo | Acertos |
|---|---|---|
| Parede do S3 | L, M, M → S3 (parede) → corre → L, M, M, → + H, S2 | 12 |
| Assist no meio | L, M, M → TAG → corre → L, M, M, → + H, S2 | 10 |
| Troca no Super (DHC) | 2L, → + H, S2, Super → DHC (Super da parceira) | 14 |
| Depois do ↓jH, relançando | 2L, M, 2H → super pulo → jL, jM, ↓jH → 2H → super pulo → jL, jM, ↓jH | 10 |
| Depois do ↓jH, no chão | 2L, M, 2H → super pulo → jL, jM, ↓jH → corre → L, M | 8 |

Os dois últimos são novos: quem crava o ↓jH agora desce rápido e pousa com o rival ainda quicando
(regra da base, vale para todos; `docs/combat-engine.md`). Depois da parede do H continua dando S2,
2H com o combo no ar ou Super; recomeçar no chão ali não ficou bom: mexer na volta da parede ganhava
esse recomeço e perdia o S3 → combo novo e parte do combo no ar, então a parede ficou como estava.

**OTG (12/11, todos):** dentro de um combo, ↓ + M no rival que acabou de cair o levanta para
continuar (um por combo; `docs/combat-engine.md`). No Ryūga: 2L, M, rasteira → ↓ + M → L, M, M, S2
(7 acertos); e depois do S2 em arco, corre → ↓ + M → S2 (6). Assim nem o S2 fecha o combo de vez.

Mudou da proposta: o R2 da proposta (S3 → corre → 2H → combo no ar) não emenda: depois da parede do
S3 o rival volta alto e longe demais para o 2H; o que pega é o S2. O 2H com o combo no ar ficou no R4,
depois da parede do H. O S2 que joga em arco (pedido de 12/11) também deixa o Super fora do R3; no
R5b o Super entra porque o S2 pega o rival ainda no chão.

---|---|---|
| 1. Sequência | **Soco duplo da casa: M → M**, o segundo M mais curto (só no fim de uma corrente que começou no L ou no 2L) | rota nova no estilo de combo dele (hoje "equilibrado", que é a base pura) |
| 3. Ponte | **S3 (Chute giratório): o último chute joga na parede** em vez de derrubar. Ele corre atrás e continua | `especiais.S3.lancar`: knockdown → wallBounce |
| 3. Ponte | **S2 (Punho ascendente) pega quem volta da parede** e pode virar Super (já cancela) | conferir no motor: hoje o S2 sai do H na parede? |
| 3. Ponte | **S1 (Onda) de perto pega no fim da corrente** (já cancela do M) | já existe |
| 2. Comando | **`→ + H`: Soco no plexo.** Passo curto, soco curto no estômago e, sem voltar, um soco de cima para baixo: dois golpes que emendam em qualquer especial. Ref.: o Solar Plexus Strike do Ryu (Street Fighter IV) | **precisa de vídeo** |

### Os combos

| # | Nome | Sequência | O que acontece | Agora? |
|---|---|---|---|---|
| R1 | Fundamento | 2L, 2L, M → **S1 (Onda)** | a Onda sai à queima-roupa e derruba longe | ✅ já sai |
| R2 | Leva ao canto | L, M, M → **S3 (Chute giratório)** → corre → 2H → super pulo → jL, jM, j2H | os quatro chutes carregam o rival até a parede; ele quica, o 2H lança e termina no ar quicando no chão | com as mudanças acima |
| R3 | Punição da casa | H (parede) → **S2 (Punho ascendente)** → SUPER (Onda Máxima) | quem erra perto apanha o H, volta da parede no Punho ascendente e leva a Onda Máxima | conferir o S2 depois do H; Super já cancela |
| R4 | Plexo (com vídeo) | 2L, **`→ + H`** (dois golpes) → S2 → SUPER | o combo mais forte dele sem barra | quando vier o vídeo |

---

## Yin-lu (p02): longo, fluido, sobe duas vezes

**Jeito de combar:** muitas pancadas leves, uma escorrendo na outra, e o combo **continua no ar**
(ela é a garça). Cada pancada tira pouco; o dano vem da quantidade. A identidade é **subir duas
vezes**: ela lança, termina no ar quicando o rival no chão e lança de novo. É o contrário do Ryūga:
longo, leve e aéreo.

### O que muda no jogo

| Camada | Mudança | Como |
|---|---|---|
| 1. Sequência | **Fluxo da garça: L, L, M, M, H** (palma, palma, bico, bico, crescente), cada um emendando sem pausa. Ela já tem M → M; o que muda é a corrente aceitar o fluxo inteiro sem apertar no tempo exato | estilo de combo próprio ("garça", hoje "chutes"): mais folga no cancelamento entre L e M |
| 3. Ponte | **S4 (Chute rodopiante): lança** em vez de derrubar; ela pula atrás e continua no ar | `especiais.S4.lancar`: knockdown → launch |
| 3. Ponte | **O 2H pode lançar duas vezes no combo** (a segunda depois do quique no chão) | `maxUsosCorrente` do 2H: 1 → 2, só nela |
| 4. Corpo | **O pulo duplo estende o combo aéreo** (já está no jogo), mais os dois jM seguidos que ela já tem | já existe |
| 2. Comando | **`→ + H`: Joelho da garça.** Sobe numa perna com o joelho no queixo e, sem descer, estica o pé no rosto: dois golpes, o segundo lança. Abre o combo aéreo sem precisar do 2H | **precisa de vídeo** |

### Os combos

| # | Nome | Sequência | O que acontece | Agora? |
|---|---|---|---|---|
| Y1 | Fluxo da garça | L, L, M, M, H (parede) | cinco pancadas leves e o crescente que joga na parede | com a folga nova |
| Y2 | Garça no ar | 2L, M, M → **S4 (Chute rodopiante)** → pula → jL, jM, jM → pulo duplo → jM, jH | o rodopiante lança, ela sobe atrás e usa o pulo duplo no meio do combo aéreo | com o S4 lançando |
| Y3 | Sobe duas vezes | 2H → super pulo → jL, jM, jM, **j2H** (quica no chão) → **2H** (lança de novo) → super pulo → jL, jM, jH | o combo mais longo do elenco: termina no ar, o rival quica e ela lança de novo | com o 2H duas vezes |
| Y4 | Joelho (com vídeo) | L, L, **`→ + H`** → pula → jL, jM, jM → pulo duplo → jM, jH | abre o ar direto do fluxo, sem o 2H | quando vier o vídeo |

---

## Lado a lado

| | Ryūga | Yin-lu |
|---|---|---|
| Tamanho do combo | curto (3 a 5 pancadas) | longo (8 a 12 pancadas) |
| Dano por pancada | alto | baixo |
| Onde termina | no especial ou no Super, no chão ou no canto | no ar, ou subindo de novo |
| Especial no meio | Chute giratório leva à parede | Chute rodopiante lança |
| Sequência própria | M → M (soco duplo) | L, L, M, M, H (fluxo) |
| `→ + H` (vídeo) | Soco no plexo (dois golpes, cancela em especial) | Joelho da garça (dois golpes, lança) |
| Corpo | pula padrão, cai médio | pulo duplo no meio do combo |

**Para não ficar forte demais:** a Yin-lu tem o combo mais longo, mas cada pancada dela tira menos
(dano 0,9× e golpes de 270 a 720) e o peso leve deixa os combos contra ela mais fáceis. O Ryūga
tira mais em menos pancadas, mas o combo dele depende de estar perto e de acertar o primeiro golpe.
Os números finais saem dos testes do motor e do celular.

## Em dúvida

1. **Y3 (dois 2H):** um combo de 12 pancadas pode passar do ponto. O escalonamento de dano do jogo
   (cada pancada a mais tira menos) segura o total, mas vale sentir no celular.
2. **O assist da Yin-lu** (a Palma) hoje quase não pega no meio do combo do Ryūga: ela entra atrás
   dele e a Palma é curta. Entra no trabalho dela.
