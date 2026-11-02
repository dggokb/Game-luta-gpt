# SHINKAI do Ryūga: protótipo do SHINKAI individual (01/11)

**Pedido do jogador:** "Faz tudo no Ryūga, vamos simular ele com alguém do SHINKAI antigo e ver o
que acrescentou de bom e de ruim." É a Etapa B (e a C, a Ruptura) do documento do Drive
`SOKON_SHINKAI_REVISAO_CONCEITUAL_2026-11-01.md`, em um lutador só. Os outros 15 seguem no
SHINKAI antigo (`docs/shinkai.md`), o que permite comparar lado a lado (Etapa D).

Tudo é **protótipo**: os números ficam isolados no bloco `ryuga*` do `CombatConfig`, e o tipo
novo é `Shinkai.Type.PERCEPCAO`. Para voltar ao Ryūga antigo (ENERGY), basta tirar `player_base`
e `p01` do `Shinkai.typeOf`. As baterias fazem isso pela chave `Shinkai.prototipoDesligado`.

## O que o Ryūga ganhou

O Ryūga ganhou percepção, precisão e contra-ataque, sem animação nova e sem manipular o tempo.
O exemplo do documento era "precisão, percepção, esquivas e contra-ataques refinados, aura
dourada concentrada".

| Peça | Como funciona | Configuração |
|---|---|---|
| **Percepção** | A janela da defesa perfeita passa de 5 para 12 quadros, e o bloqueio contra apertar sem parar cai de 20 para 8. No começo de cada golpe do rival, um brilho dourado aparece na ponta do membro: ele vê o golpe vindo. | `ryugaPerfeitaJanela`, `ryugaPerfeitaBloqueio` |
| **Leitura** | A defesa perfeita num golpe de perto vira "LEITURA!": ele sai livre na hora, e quem atacou no chão fica desequilibrado por 24 quadros (o golpe dele é cortado). | `ryugaLeituraAbre` |
| **Esquiva** | O backdash passa por dentro do golpe ("LEU O GOLPE!"). Quem errou fica na recuperação do próprio golpe. | — |
| **Golpe preciso** | Depois da leitura ou da esquiva, o primeiro golpe dele em 40 quadros bate 30% mais e atordoa 12 quadros a mais ("GOLPE PRECISO!"). Enquanto o golpe preciso está pronto, um brilho fica no olhar dele. | `ryugaPrecisoJanela`, `ryugaPrecisoDanoPermille`, `ryugaPrecisoHitstun` |
| **Desgaste (sobrecarga)** | No lugar dos 8 s fixos, o corpo gasta. Parado, ele chega ao limite em ~12 s. Cada golpe que começa, cada leitura, cada esquiva e cada dano que leva gastam mais. A barra do HUD mostra o fôlego que sobra. | `ryugaDesgaste*` |
| **Fim voluntário** | SHINKAI de novo antes do limite: desliga sem cansaço, e metade do fôlego que sobrou volta para a barra, então dá para ligar de novo no round. O Super grátis vale só na primeira vez. | `ryugaDevolvePermille` |
| **Limite** | O corpo chegou no fim: "LIMITE! SHINKAI = RUPTURA". Se ele não fizer nada em 1 s, o SHINKAI acaba e ele fica exausto por 1,5 s (anda e dá dash 30% mais devagar, e a defesa gasta 50% mais guarda). | `ryugaLimiteJanela`, `ryugaExaustao*` |
| **Ruptura** | SHINKAI apertado no limite. Por 5 s a aura fica vermelho-dourada, com raios, e toda defesa vira perfeita (leitura). O dano bônus passa de 25% para 40%, e dói 4 de vida por quadro, até 1.200 (nunca a última). No fim (ou se ele desligar), fica exausto por 4 s e não liga mais no round. | `ryugaRuptura*`, `ryugaExaustaoRuptura` |

**Não mudou:**
- os bônus de todos: +25% de dano, chip, vida vermelha de volta e o primeiro Super grátis;
- o corte de ativação e o contorno;
- o Clash, onde o Ryūga empurra no ritmo, como a TECHNIQUE;
- a troca, que acaba com o SHINKAI.

Ele perde o que tinha do ENERGY (bolas em sequência, mini projétil no H, barra de Super enchendo
sozinha).

**A CPU com o Ryūga:**
- no SHINKAI ela reage mais rápido: 60% da reação do nível, ou 8 quadros no nível 8;
- ela tenta mais a defesa perfeita;
- depois da leitura, ela castiga com o golpe preciso;
- no limite, rompe quando dá para fechar a luta ou quando está perdendo feio; senão, deixa acabar;
- com o corpo em 80% e nada para aproveitar, os níveis altos desligam o SHINKAI.

## Simulação contra o SHINKAI antigo

`BateriaShinkaiRyuga`: o Ryūga, CPU contra CPU em 1x1, contra cada um dos outros 15, com 32 lutas
por rival (480 por coluna), metade de cada lado. As sementes são as mesmas em todas as colunas, e
o rival usa o SHINKAI antigo do tipo dele.

Para separar o que cada peça rende, há colunas extras:
- **sem reação rápida da CPU:** a assinatura sozinha;
- **sem Ruptura:** no limite, ele sempre acaba;
- **sem os dois.**

No jogo normal, 94% das lutas no nível 8 viram SHINKAI Clash (a CPU responde o SHINKAI do outro
com o seu), e o Clash esconde o resto. Por isso cada nível roda duas vezes, a segunda sem Clash.

Saídas completas:
- [nível 8](perfis/shinkai-ryuga-nivel8.md) (com o log de 4 lutas);
- [nível 8 sem Clash](perfis/shinkai-ryuga-nivel8-sem-clash.md);
- [nível 5](perfis/shinkai-ryuga-nivel5.md);
- [nível 5 sem Clash](perfis/shinkai-ryuga-nivel5-sem-clash.md).

| Ryūga vence | antigo (ENERGY) | protótipo | sem reação rápida | sem Ruptura | sem os dois |
|---|---|---|---|---|---|
| Nível 8, jogo normal | 57% | **62%** | 61% | 63% | 62% |
| Nível 8, sem Clash | 65% | **73%** | 67% | 69% | 63% |
| Nível 5, jogo normal | 63% | **65%** | 65% | 64% | 64% |
| Nível 5, sem Clash | 69% | **69%** | 65% | 68% | 65% |

| No SHINKAI dele (nível 8, sem Clash) | antigo | protótipo |
|---|---|---|
| Duração média | 8,4 s | 10,6 s |
| Dano que ele dá por segundo | 438 | 526 |
| Dano que ele leva por segundo | 190 | 192 |
| Saldo por SHINKAI (dá − leva) | 2.086 | 3.549 |
| Golpes do rival / defendidos / defesas perfeitas (por luta) | 3,7 / 0,6 / 0,2 | 4,6 / 0,7 / 0,4 |
| Leituras / esquivas / golpes precisos (por luta) | – | 0,32 / 0,00 / 0,18 (707 de dano cada) |
| Como acabou (total): desligou / no limite / Ruptura | – | 38 / 90 / 146 |
| Exausto por luta (dano levado por segundo exausto) | – | 0,7 s (254) |
| Dor da Ruptura por luta | – | 223 |

## O que acrescentou de bom

1. **O desgaste funciona e não quebra nada.** O SHINKAI dura entre 9 e 12 s, conforme o que ele
   faz, contra 8,4 s do relógio. Fora do SHINKAI tudo fica igual: o dano por segundo é o mesmo
   nas cinco colunas. O sistema antigo dos outros 15 seguiu intacto.
2. **A Ruptura é a peça que mais rende e cria um momento de decisão.** No limite, a CPU rompe em
   cerca de 60% das vezes. Sem Clash no nível 8, a Ruptura vale de 4 a 6 pontos (69% → 73%). O
   custo aparece na luta: dor média de 223 de vida por luta, exaustão depois e o fim do SHINKAI
   no round.
3. **O SHINKAI dele rende mais sem precisar das bolas.** O saldo por SHINKAI vai de 2.086 para
   3.549, e no jogo normal de 644 para 1.852. Ele deixa de ser "solta bola mais rápido", que era
   justamente o que ganhava fácil.
4. **O golpe preciso, quando sai, pesa:** ~700–800 de dano num golpe só.
5. **Contra os POWER e os TECHNIQUE ele melhora mais** (nível 8, sem Clash):
   - p12: 34% → 61%;
   - p13: 56% → 81%;
   - p14: 77% → 94%.

## O que acrescentou de ruim

1. **A assinatura quase não aparece.** São 0,3 leitura por luta no nível 8 e 0,1 no nível 5, e
   nenhuma esquiva. No SHINKAI dele, quem ataca é ele: o rival solta uns 4–5 golpes, e o Ryūga
   defende menos de 1. "Sem os dois" (só a assinatura e o desgaste) fica em 63%, igual ao antigo
   (65%). Ou seja, a percepção como está não muda a luta da CPU.
2. **Esperar para ler foi pior.** Testei a CPU parando perto do rival para esperar o golpe e ler:
   as leituras subiram (0,32 → 0,41), mas ele bateu menos e caiu de 73% para 65%. Pela regra,
   saiu.
3. **Parte da vantagem é a CPU ficar mais rápida, não a mecânica.** A reação de 8 quadros vale
   uns 4–6 pontos sem Clash. Para quem joga com o Ryūga, isso não existe: a pessoa tem a janela
   de 12 quadros e o brilho no começo do golpe do rival, e só o teste na mão diz se isso basta.
4. **O Clash engole o SHINKAI.** No jogo normal, 94% das lutas no nível 8 viram Clash, nas duas
   versões. A CPU responde o SHINKAI do outro com o dela logo no começo (1 a 2 s depois), e quem
   vence o Clash sai com o SHINKAI inteiro enquanto o outro perde o dele. Veja "Clash com só
   parte do fôlego" abaixo.
5. **O fim voluntário quase não é usado** (38 de 274 fins). Devolver metade do fôlego que sobrou
   é pouco. Desligar em 80% devolve só 10% da barra. O ganho real é não ficar exausto, e a
   exaustão de 1,5 s custa pouco: 0,7 s por luta com o dano levado dentro do normal.
6. **Por rival, as quedas são pequenas e ficam dentro do ruído** (32 lutas por rival dão ±9
   pontos):
   - sem Clash: p16, 88% → 84%;
   - no jogo normal: p10, 56% → 53%, e p14, 78% → 75%.
7. **No nível 5 quase não muda** (63% → 65% no normal, 69% → 69% sem Clash). A CPU do nível 5
   quase não faz defesa perfeita, então a assinatura dele também não aparece para quem enfrenta o
   Ryūga no começo da história.

## Espelho: Ryūga novo × Ryūga novo

Pedido do jogador: os dois com o PERCEPÇÃO, comparados ao espelho antigo (ENERGY × ENERGY).
São 480 lutas por coluna, nas mesmas sementes. Saídas completas:
- [nível 8](perfis/shinkai-ryuga-espelho-nivel8.md) (com o log de 4 lutas);
- [nível 8 sem Clash](perfis/shinkai-ryuga-espelho-nivel8-sem-clash.md);
- [nível 5](perfis/shinkai-ryuga-espelho-nivel5.md);
- [nível 5 sem Clash](perfis/shinkai-ryuga-espelho-nivel5-sem-clash.md).

| Espelho | antigo (ENERGY × ENERGY) | novo (PERCEPÇÃO × PERCEPÇÃO) |
|---|---|---|
| Nível 8: lutas com Clash | 95% | 98% |
| Nível 8: quem vence o Clash vence a luta | 73% | **83%** |
| Nível 5: quem vence o Clash vence a luta | 85% | 80% |
| Nível 8: duração da luta | 43,8 s | 43,2 s |
| Nível 8 sem Clash: duração da luta | 42,0 s | 39,7 s |
| Nível 8: duração do SHINKAI | 11,4 s | 13,2 s |
| Nível 8: leituras por luta (os dois) | – | 0,34 |
| Nível 8: Rupturas por luta (os dois) | – | 0,35 |
| Nível 8 sem Clash: saldo por SHINKAI | 1.712 | 2.915 |
| Nível 5 sem Clash: saldo por SHINKAI | 3.098 | 2.489 |

**O que o espelho mostra:**
- **A luta é decidida no Clash, e no nível 8 o novo agrava isso.** Quem vence o Clash fica com o
  SHINKAI (e o perdedor perde o dele). No protótipo, isso vira ~10 s de SHINKAI e mais 5 s de
  Ruptura, com a CPU reagindo mais rápido. O log mostra o padrão: Clash, o vencedor chega ao
  limite, rompe e fecha a luta. No nível 8, quem vence o Clash passou a vencer 83% das lutas
  (73% antes). No nível 5 foi o contrário (85% → 80%), porque lá a CPU rompe menos. **Não é o
  desgaste zerado no Clash** (medido abaixo): no espelho do nível 8, sem a reação rápida dá 76%,
  sem a Ruptura 78% e sem os dois 76%.
- **Dois leitores quase não se leem:** 0,34 leitura por luta somando os dois lados. A luta
  espelhada continua a mesma luta do Ryūga, com um SHINKAI mais longo.
- **Sem Clash, a luta fica um pouco mais curta** (42,0 → 39,7 s no nível 8): o SHINKAI mais
  longo e a Ruptura aceleram o fim.
- **No nível 5 sem Clash, o SHINKAI antigo rende mais** (saldo de 3.098 contra 2.489). As bolas em
  sequência do ENERGY punem a CPU fraca, e a leitura do nível 5 quase não sai.

## Clash com só parte do fôlego (pedido do jogador)

Antes, quem vencia o Clash zerava o desgaste: o corpo ficava novo, como o relógio dos outros,
que volta cheio. Agora a parte que o corpo recupera é ajustável (`ryugaClashFolegoPermille`). A
mesma bateria rodou com 100% (como era), 60%, 40%, 20% e 0%. Saídas completas:
- [nível 8](perfis/shinkai-ryuga-clash-folego-nivel8.md);
- [nível 8, espelho](perfis/shinkai-ryuga-clash-folego-nivel8-espelho.md);
- [nível 5](perfis/shinkai-ryuga-clash-folego-nivel5.md);
- [nível 5, espelho](perfis/shinkai-ryuga-clash-folego-nivel5-espelho.md).

| | 100% | 60% | 40% | 20% | 0% |
|---|---|---|---|---|---|
| Ryūga vence (nível 8, contra os 15) | 62% | 62% | 62% | 62% | 63% |
| Quem vence o Clash vence a luta (nível 8, espelho) | 83% | 83% | 83% | 84% | 83% |
| Quem vence o Clash vence a luta (nível 5, espelho) | 80% | 80% | 81% | 81% | 82% |
| Duração do SHINKAI (nível 8, espelho) | 13,2 s | 13,2 s | 13,1 s | 13,1 s | 12,9 s |

**Por que não muda:** o Clash começa, em média, 1,1–1,8 s depois que o Ryūga liga, com o corpo
só 8–15% gasto (o relógio para durante o Clash). Devolver 100% ou 0% disso dá no mesmo.

**Decisão:** sem efeito, então o padrão continua em 100% (o mesmo de antes) e o ajuste fica para
quando o Clash vier mais tarde na luta.

## Perder o Clash não desliga o SHINKAI (pedido do jogador)

**Vale para os 16:**
- **Antes:** quem perdia o Clash perdia o SHINKAI inteiro, além dos 2.000 de dano.
- **Agora:** "perder o Clash não deveria desativar o SHINKAI". O perdedor leva o golpe, voa e segue
  no SHINKAI que tinha (`shinkaiClashPerdedorPermille` = 1000, a parte do que ele tinha que
  fica). Os relógios param durante o Clash, então ele volta com o mesmo tempo.

A bateria (modo `perdedor`) comparou:
- perder tudo (como era);
- ficar com metade do que tinha;
- ficar com tudo (agora).

Cada uma rodou contra os 15, no espelho e com os 16 todos contra todos (modo `elenco`, 480 lutas
por coluna). Os números já incluem o combo breaker novo (abaixo). Saídas completas:
- [nível 8](perfis/shinkai-clash-perdedor-nivel8.md);
- [nível 8, espelho](perfis/shinkai-clash-perdedor-nivel8-espelho.md);
- [nível 8, elenco](perfis/shinkai-clash-perdedor-nivel8-elenco.md);
- [nível 5](perfis/shinkai-clash-perdedor-nivel5.md);
- [nível 5, espelho](perfis/shinkai-clash-perdedor-nivel5-espelho.md);
- [nível 5, elenco](perfis/shinkai-clash-perdedor-nivel5-elenco.md).

| | perde tudo (antes) | metade | tudo (agora) |
|---|---|---|---|
| Quem vence o Clash vence a luta (elenco, nível 8) | 72% | 66% | 61% |
| Quem vence o Clash vence a luta (elenco, nível 5) | 74% | 65% | 62% |
| Quem vence o Clash vence a luta (Ryūga × Ryūga, nível 8) | 84% | 68% | 64% |
| Duração da luta (elenco, nível 8) | 48,6 s | 51,8 s | 52,7 s |
| Ryūga vence contra os 15 (nível 8) | 64% | 70% | 73% |
| Ryūga antigo (ENERGY) contra os 15 (nível 8) | 57% | – | 64% |

- **O Clash continua valendo, mas não decide mais a luta sozinho.** Quem vence ainda leva 2.000
  de dano a mais e fica com o SHINKAI cheio.
- **As lutas ficam ~4 s mais longas.**
- **O Ryūga sobe 7–9 pontos contra os 15**, e o antigo (ENERGY) sobe igual. É o personagem, não o
  protótipo. Ele vence 48% dos Clashes, então não é o perdedor frequente. Ponto a acompanhar: ele
  já era dos mais fortes.
- **Também testado (modo `clash`):** o vencedor do Clash recuperar só parte do fôlego do Ryūga.
  Não muda nada, porque o Clash começa 1,1–1,8 s depois de ele ligar, com o corpo só 8–15%
  gasto. O padrão ficou como era (100%).

## A assinatura que não se sentia jogando (pedido do jogador)

"A mudança do SHINKAI pra mim não surtiu nada de diferente jogando." Bate com a medida: a leitura
só saía com a defesa perfeita em cima da hora, ~1 vez a cada 3 lutas. Foram testadas duas
mudanças (modo `assinatura`). Saídas completas:
- [nível 8](perfis/shinkai-ryuga-assinatura-nivel8.md);
- [nível 8 sem Clash](perfis/shinkai-ryuga-assinatura-nivel8-sem-clash.md);
- [nível 5](perfis/shinkai-ryuga-assinatura-nivel5.md);
- [nível 5 sem Clash](perfis/shinkai-ryuga-assinatura-nivel5-sem-clash.md).

As duas mudanças:
- **(a) A defesa comum também lê:** toda defesa de golpe de perto no SHINKAI dele desequilibra
  quem atacou por 14 quadros, e ele sai livre (`ryugaLeituraLeveAbre`). A perfeita segue mais
  forte: 24 quadros e o golpe preciso.
- **(b) Contra-golpe preciso:** no SHINKAI dele, todo contra-golpe (acertar quem estava apertando
  botão) é golpe preciso (`ryugaContraPreciso`).

| Nível 8, jogo normal | protótipo | + (a) | + (b) | + (a) e (b) |
|---|---|---|---|---|
| Ryūga vence | 73% | 71% | 73% | 73% |
| Leituras por luta | 0,38 | 0,50 | 0,39 | 0,50 |
| Golpes precisos por luta | 0,24 | 0,19 | 0,65 | 0,63 |
| Dano do golpe preciso | 735 | 757 | 950 | 994 |

**Decisão:** as duas ficam. Elas aumentam o que aparece na tela (o golpe preciso quase triplica)
sem mudar a vitória. Ainda é menos de um momento por luta com a CPU, porque a CPU do Ryūga ataca
em vez de defender. Falta o teste na mão.

## Leitura e próximos passos (para decidir)

- **A identidade pede o jogador na mão.** Uma assinatura defensiva (ler e castigar) depende de
  alguém defender em cima da hora. A CPU que ataca não a usa. Vale testar com você jogando: ligar
  o SHINKAI e defender o golpe que brilha.
- **Se a leitura ficar rara também na mão,** há dois caminhos, sem copiar o parry automático da
  TECHNIQUE:
  - **(a) leitura também na defesa comum:** toda defesa no SHINKAI desequilibra o rival, só que
    por menos tempo;
  - **(b) contra-golpe dentro do golpe do rival:** o golpe dele que acerta o rival no começo da
    animação vira golpe preciso.
- **Ruptura:** fica. Falta decidir se a dor pode derrubar (hoje nunca tira a última vida) e se
  ela devia valer só uma vez por luta, não por round.
- **Fim voluntário:** devolver mais (por exemplo, todo o fôlego que sobrou) ou deixar a exaustão
  do limite maior, para valer a pena desligar antes.
- **Clash:** o perdedor segue no SHINKAI e o Clash deixou de decidir a luta (seção acima). Falta
  testar a CPU respondendo o SHINKAI menos vezes, ou mais tarde.

## Como rodar

```
gradle -q -I <init com rodarBateria> :pc:rodarBateria -Dclasse=com.gamelutagpt.BateriaShinkaiRyuga "-Dargs=32 8 narra"
gradle ... "-Dargs=32 8 semClash"     # sem Clash
gradle ... "-Dargs=32 5"              # nível 5
gradle ... "-Dargs=32 8 espelho narra" # Ryūga × Ryūga, os dois no SHINKAI da coluna
gradle ... "-Dargs=32 8 clash [espelho]" # o fôlego que o vencedor do Clash recupera, por coluna
gradle ... "-Dargs=32 8 perdedor [espelho|elenco]" # a parte do SHINKAI que o perdedor do Clash guarda
gradle ... "-Dargs=32 8 assinatura [semClash]" # (a) defesa comum lê, (b) contra-golpe preciso
```
