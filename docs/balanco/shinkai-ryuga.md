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

## Esquiva automática com rastro dourado (pedido do jogador)

Para a assinatura aparecer na tela sem depender de defender em cima da hora: no SHINKAI dele, o
golpe de perto que ia acertar o Ryūga **desprevenido** (parado ou andando para frente, sem
segurar a defesa) passa direto. Ele recua no backdash, invulnerável, deixando um **rastro
dourado** ("LEU O GOLPE!"), e o golpe preciso fica pronto. Depois disso, a esquiva sozinha
recarrega por 3 s (`ryugaEsquivaAutoRecarga` = 180) e gasta o corpo como a esquiva manual.

Segurando a defesa, ele não esquiva sozinho: aí vale a leitura. Na primeira versão a esquiva
saía também defendendo e engolia as leituras (0,51 → 0,24 por luta). Separadas, as leituras
voltaram (0,53).

Saídas completas:
- [nível 8](perfis/shinkai-ryuga-esquiva-nivel8.md);
- [nível 8 sem Clash](perfis/shinkai-ryuga-esquiva-nivel8-sem-clash.md);
- [nível 8, espelho](perfis/shinkai-ryuga-esquiva-nivel8-espelho.md);
- [nível 5](perfis/shinkai-ryuga-esquiva-nivel5.md);
- [nível 5 sem Clash](perfis/shinkai-ryuga-esquiva-nivel5-sem-clash.md);
- [nível 5, espelho](perfis/shinkai-ryuga-esquiva-nivel5-espelho.md).

| Nível 8 | sem esquiva | recarga 3 s (fica) | recarga 1,5 s | recarga 0,75 s |
|---|---|---|---|---|
| Ryūga vence (jogo normal) | 74% | 75% | 75% | 75% |
| Ryūga vence (sem Clash) | 70% | 73% | 73% | 73% |
| Dano que ele leva por s no SHINKAI (sem Clash) | 201 | 168 | 168 | 167 |
| Leituras / esquivas / precisos por luta (normal) | 0,51 / 0 / 0,61 | 0,53 / 0,39 / 0,66 | 0,53 / 0,40 / 0,67 | 0,53 / 0,42 / 0,67 |

- **A recarga quase não importa:** contra a CPU, a esquiva raramente teria chance de sair duas
  vezes em 3 s. Por isso fica a de 3 s, a mais segura contra quem joga.
- **O ganho:** um momento a mais por luta, visível (o rastro), e o Ryūga leva 10–15% menos dano
  no SHINKAI.
- **O custo:** o Ryūga sobe 1–3 pontos, e ele já é dos mais fortes (73–75% contra os 15).

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
gradle ... "-Dargs=32 8 esquiva [semClash|espelho]" # a esquiva automática com recargas diferentes
```

## Animação de ativação (02/11)

O vídeo `shinkai.mp4` (Drive, p01/animations) virou a animação `SHINKAI_ATIVA` do Ryūga: ele cruza os
braços no alto, segura, desce os braços e volta à guarda.
- **Conversão:** quadros 34–92 do vídeo, 26 na folha (`tools/sprites/clips/player_base_shinkai.json`,
  HD). A pose parada no alto fica mais curta. Toca em 1,56 s.
- **No jogo:** ao ligar o SHINKAI, a luta congela pelo tempo da animação, e não pelos 48 quadros de
  sempre (`CombatEngine.shinkaiAtivacaoQuadros`).
  - O rival e o relógio ficam parados, e o tempo do SHINKAI espera.
  - O cenário escurece e o rival fica no escuro (`GameView`, `ativacaoShinkai`).
  - Só o Ryūga aparece, fazendo a animação. O corte de ativação (retrato e faixa) continua por
    cima.
- **Outros personagens:** com a mesma animação no pacote (estado `SHINKAI_ATIVA`), ganham a mesma
  pausa sem mudar código.

### Liberação de poder em vídeo e cinemática (02/11)

O vídeo `Inicio_Centro - Liberação de Poder Shinkai` substitui a animação simples. Nele o Ryūga
solta raios, a aura azul sobe e um dragão de energia gira em volta dele.
- **Conversão:** vira o efeito `assets/efeitos/shinkai_player_base` pelo `efeito_video.py`, com
  recorte suave: o meio transparente fica meio transparente.
  - O trecho vai do quadro 3 ao 105, 1 quadro a cada 3, tocado 1,5× mais rápido: 35 quadros,
    2,9 s.
  - A escala é a do Ryūga no jogo (a guarda de 450 px do vídeo vira 224 px), com a origem nos pés
    dele. A marca d'água sai.
  - A folha tem 2014×2564 px, 0,75 px por px do mundo (5 MB).
  - O conversor ganhou `--escala-mundo`, `--resolucao`, `--velocidade` e `--apagar`.
- **No jogo:** ao ligar o SHINKAI, o efeito toca nos pés dele, virado para o lado em que ele olha,
  e o sprite dele some enquanto isso. Quem desenha o Ryūga é o próprio vídeo.
  - A pausa tem o tempo do efeito: a animação `SHINKAI_ATIVA` (a reserva, sem o vídeo) foi
    esticada para os mesmos 2,9 s.
- **Cinemática:**
  - a câmera fecha nele (32% mais de zoom), trazendo o corpo, a aura e o dragão aos poucos para o
    meio da tela;
  - entram as faixas pretas de cinema e o HUD some;
  - tudo isso entra e sai em 18 quadros;
  - o rival e o cenário ficam escuros, e o rival, o relógio e o tempo do SHINKAI ficam parados.
- **Qualquer personagem com `efeitos/shinkai_<id>`** ganha a mesma cena sem mexer no código.

### Cantos do vídeo e a ordem da cena (02/11)

O jogador viu cortes nos cantos do vídeo e pediu outra ordem: "câmera pausa, ele levanta as mãos,
aí sim entra o texto de SHINKAI, aí sai, e aí ele termina a ação com o dragão".
- **Cantos:** havia três cortes.
  - O brilho encostava na beira do quadro do vídeo e virava uma linha reta.
  - A marca d'água deixava um dente reto no canto de cima.
  - O verde borrado de alguns quadros virava um véu cinza retangular.
- **Correção no conversor (`efeito_video.py`):** quatro opções novas.
  - `--borda 130`: o efeito some aos poucos perto da beira, numa superelipse de cantos
    arredondados;
  - `--apagar` agora tem a beira borrada;
  - `--alfa-corte 0.12`, `--escuro 110` e `--cinza 0.3` tiram o véu meio transparente sem cor;
  - o brilho azul e o personagem, opacos, ficam iguais.
- **Ordem:** a câmera fecha e ele levanta as mãos.
  - No quadro 8 do vídeo, as mãos no alto, o vídeo para (`--segurar 8,800`, que grava `marcaMs`
    no `efeito.json`).
  - Aí entra o corte de ativação (os olhos, a faixa e "SHINKAI" com o tipo), que fica 0,8 s e
    sai.
  - Depois o vídeo segue com o dragão.
- **Pausa:** a pausa inteira passa a 3,7 s, e a `SHINKAI_ATIVA` de reserva foi esticada junto.
- **Sem marca:** um efeito sem `marcaMs` mantém o corte no começo, como antes.

### Retrato antes da próxima mudança: Ryūga × Ryūga, SHINKAI sem Clash (02/11)

Pedido do jogador antes de mexer no SHINKAI de novo. A bateria é `BateriaEspelhoShinkai`: 300
lutas CPU × CPU por nível, com SHINKAI (sem Clash) e sem SHINKAI, com as mesmas sementes. As
tabelas completas estão em `perfis/espelho-ryuga-shinkai.md`. Os números abaixo são do nível 8,
com o nível 5 entre parênteses.

**Luta**
- Dura 43,5 s com SHINKAI e 40,7 s sem (37,3 e 33,4). Termina sempre em KO.
- O vencedor sobra com 55% da vida (42% sem). Há mais viradas: 30% contra 22% (36% contra 20%).

**Barra**
- Enche em uns 12 s, para todos.
- Em 30% dos lados ela enche e não é usada (47% no nível 5).
- Liga em média 1,48 vez por luta (1,08). Os dois ligam em 43% das lutas.

**Quando liga**
- Por volta de 20–24 s de luta, com 58% da vida contra 61% do rival; em 57% das vezes está atrás.
- Dura 13,8 s, um terço da luta. Há alguém no SHINKAI em 47% do tempo.

**Como acaba**

| fim | parte dos SHINKAIs |
|---|---|
| ele derruba o rival dentro | 41% |
| no limite do corpo | 26% |
| depois da Ruptura | 18% |
| desligado por ele | 14% |
| ele cai dentro | 2% |

**Força**
- Dentro, dá 381 de dano por s (fora, 236) e leva 157 por s (fora, 304).
- Saldo de +3.100 por SHINKAI: 31% da vida.

**Quem liga**
- Só um ligou: ele vence 89%.
- Quem liga primeiro vence 62% (70%).
- 60% das lutas terminam com o vencedor no SHINKAI.

**Dentro do SHINKAI**
- Por SHINKAI: 0,58 leituras, 0,53 golpes precisos, 0,09 esquivas e 0,33 Rupturas.
- O Super sobe de 1,4 para 3,1 por minuto. A Onda cai de 19 para 13 por minuto, e os pulos
  dobram.
- Combos com 2,9 golpes em média (2,4 fora) e 2.261 de dano por combo (1.460 sem SHINKAI).

**Fora**
- A Onda é o golpe da luta: 19 por minuto de cada lado.
- O agarrão quase não aparece (0,38 por luta), e não há guard break.

**O mesmo espelho com o SHINKAI antigo do Ryūga (ENERGY)**

Rodado com `BateriaEspelhoShinkai 300 antigo 5 8`; as tabelas estão em
`perfis/espelho-ryuga-shinkai-antigo.md`. Comparação no nível 8, PERCEPÇÃO → ENERGY:

| medida | PERCEPÇÃO | ENERGY |
|---|---|---|
| duração da luta | 43,5 s | 45,9 s |
| duração do SHINKAI | 13,8 s | 11,5 s |
| tempo com alguém no SHINKAI | 47% | 38% |
| dano dado por s dentro | 381 | 317 |
| saldo por SHINKAI | +3.100 | +1.740 |
| derruba o rival dentro | 41% dos SHINKAIs | 18% |
| a luta termina com o vencedor no SHINKAI | 60% | 27% |
| só um liga: ele vence | 89% | 92% |
| quem liga primeiro vence | 62% | 59% |
| os dois ligam | 43% | 54% |
| a barra enche e não é usada | 30% | 24% |
| Ultra por minuto dentro | 0,4 | 0,7 |
| combos de 9 golpes ou mais dentro | 5% | 9% |
| combo breakers por luta | 0,86 | 1,32 |

O ENERGY rende quase metade do saldo e quase não fecha a luta lá dentro. Mesmo assim, quem liga
sozinho vence 9 em 10: ligar decide a luta nos dois. O ENERGY acaba sempre no tempo (82%) ou no KO.
Cada SHINKAI solta só um tiro de energia (1,04).

### SHINKAI por reserva de vida (pedido do jogador, 02/11)

O jogador pediu um novo tipo de SHINKAI.
- A barra enche na luta; cheia, ele liga.
- Ao ligar, ele ganha uma **reserva de vida** à parte da vida normal.
- A habilidade exclusiva fica disponível: a leitura, a esquiva e o golpe preciso da PERCEPÇÃO.
- Os golpes que ele leva gastam a reserva primeiro.
- As técnicas potencializadas também gastam a reserva.
- A reserva acabou, o SHINKAI acaba. **Não há cronômetro**: parado, sem gastar e sem apanhar, o
  SHINKAI fica.

**Ruptura**
- Com a reserva no fim (500 ou menos), o jogador escolhe ultrapassar o limite.
- Rompido, a vida normal paga o poder: 4 por quadro, e as técnicas também saem da vida.
- Ele pode desligar quando quiser.
- Ao chegar ao piso (10% da vida), a Ruptura acaba sozinha e não derruba ninguém.

**Números (`CombatConfig`, `ryugaReserva*`)**

| | valor |
|---|---|
| reserva | 2.500 (25% da vida) |
| especial (S, S2–S4) | gasta 150 |
| Super | gasta 400 |
| esquiva | gasta 100 |
| golpe comum | não gasta |
| desligar antes do limite | metade da reserva que sobrou volta para a barra |
| fim da reserva | 1,5 s de cansaço |
| fim da Ruptura | 4 s de cansaço |

- O protótipo do desgaste continua em `ryugaReserva = false`, para comparar.
- No HUD, a reserva aparece em dourado por cima da ponta da vida, e a barra do SHINKAI mostra a
  reserva que sobra.

**CPU**
- No limite, ela decide como antes.
- Na Ruptura, desliga com a vida baixa ou quando já rompeu há 6 s sem fechar a luta.
- Com a reserva baixa, longe e sem nada para fechar, os níveis altos desligam e guardam parte na
  barra.

**Espelho Ryūga × Ryūga, sem Clash** (`BateriaEspelhoShinkai 300 5 8`; tabelas em
`perfis/espelho-ryuga-shinkai-reserva.md`). Nível 8:

| medida | ENERGY (antigo) | PERCEPÇÃO (desgaste) | reserva |
|---|---|---|---|
| duração da luta | 45,9 s | 43,5 s | 43,9 s |
| duração do SHINKAI | 11,5 s | 13,8 s | 13,5 s |
| dano dado por s dentro | 317 | 381 | 402 |
| dano levado por s dentro (sem descontar a reserva) | 167 | 157 | 120 |
| vida de verdade perdida por SHINKAI | 1.921 | 2.162 | 453 |
| saldo na vida por SHINKAI | +1.740 | +3.100 | +4.980 |
| como acaba | 82% no tempo | 41% KO, 26% limite, 18% Ruptura, 14% desliga | 46% KO, 43% reserva acabou, 8% desliga, 3% Ruptura |
| Rupturas por SHINKAI | — | 0,33 | 0,16 |
| a luta termina com o vencedor no SHINKAI | 27% | 60% | 68% |
| só um liga: ele vence | 92% | 89% | 96% |
| quem liga primeiro vence | 59% | 62% | 64% |
| viradas | 29% | 30% | 31% |

**Leitura**
- A reserva segura 1.169 de dano por SHINKAI, e as técnicas gastam 747.
- O que passa para a vida cai a um quinto.
- É o SHINKAI mais forte dos três.
- A Ruptura aparece menos (0,16 por SHINKAI): o rival costuma estourar a reserva de uma vez, e
  a escolha não chega a abrir.
- O tamanho da reserva muda pouco a duração (1.500 → 3.500: de 11,8 s para 14,9 s) e muda a força
  (saldo de 4.000 para 5.700).

### O SHINKAI é uma vida nova (pedido do jogador, 02/11)

O jogador pediu um SHINKAI no estilo anime, como uma vida nova:
- "ele se transformou, tenho que me transformar";
- "se eu quebrar a transformação dele antes da minha, ganho";
- cada SHINKAI terá a sua característica única, e o conjunto vira um **xadrez de poder**.

A reserva passa a ser a base comum. A característica do Ryūga continua a da PERCEPÇÃO.

**O que mudou**

| | antes | agora |
|---|---|---|
| reserva | 2.500 | 10.000 (uma vida inteira) |
| limite (dá para romper) | 500 | 1.500 |

- No HUD, a reserva é uma camada dourada que cobre a barra de vida. Ao encolher, aparece a vida de
  verdade por baixo. No limite, a camada pisca.
- **Trava:** de cima do limite, um golpe ou combo não zera a reserva; ela para em 750. O limite
  sempre aparece, e a Ruptura fica sempre à mão.
- **Transformação quebrada:** a reserva zerou fora da Ruptura.
  - Ele fica 0,75 s atordoado no chão e 5 s cansado (mais lento, a guarda gasta mais).
  - O aviso na tela é "TRANSFORMAÇÃO QUEBRADA!", com tremor.

**Espelho Ryūga × Ryūga, nível 8, 300 lutas.** Tabelas em `perfis/espelho-ryuga-shinkai-vida-*.md`.

| medida | sem Clash | com Clash (o jogo) | sem SHINKAI |
|---|---|---|---|
| duração da luta | 41,4 s | 71,9 s | 40,7 s |
| os dois se transformam | 12% | 93% | — |
| responde a transformação com a sua (por luta) | — | 1,17 (vira o Clash) | — |
| duração do SHINKAI | 20,5 s | 29,7 s | — |
| tempo com alguém transformado | 56% | 64% | — |
| transformações quebradas por luta | 0,07 | 1,03 | — |
| quem quebra a do outro vence | 75% | 71% | — |
| quebrou antes de se transformar: vence | 81% (16 casos) | — | — |
| só um se transforma: ele vence | 100% | 100% | — |
| quem se transforma primeiro vence | 88% | 55% | — |
| viradas | 31% | 38% | 22% |
| leituras por SHINKAI | 1,14 | 1,63 | — |

**Leitura**
- **Sem Clash** (ninguém responde), a transformação decide a luta: quem liga primeiro vence 88%.
- **Com Clash**, que é o jogo de verdade, a resposta acontece:
  - os dois se transformam em 93% das lutas;
  - a luta dura quase o dobro;
  - uma transformação quebra por luta, e quem quebra vence 71%;
  - quem se transforma primeiro cai para 55%: responder vale;
  - há mais viradas (38%).
- Esse é o xadrez de poder: transformar, responder, quebrar.

### A regra da vida nova para todos (pedido do jogador, 02/11)

"Mantém as características dos outros, mas põe a nova regra de SHINKAI."
- Todos os SHINKAIs ganham a vida nova (reserva de 10.000) e ficam sem cronômetro.
- Todos ganham o limite, a Ruptura, a trava e a transformação quebrada.
- Cada um mantém a característica do tipo:
  - POWER: armadura e dano;
  - SPEED: sumiço e eco; o sumiço agora gasta 150 da reserva;
  - ENERGY: tiros e barra de Super enchendo;
  - TECHNIQUE: aparada;
  - PERCEPÇÃO (o Ryūga): leitura, esquiva e golpe preciso.
- **No código:**
  - `shinkaiReservaTodos` em `CombatConfig`; `false` volta o relógio de 8 s para os outros.
  - Os testes antigos de cada tipo rodam no relógio (`ShinkaiTest`, `ShinkaiClashTest`); a regra
    nova para todos é testada em `ShinkaiReservaTest`.
- **No HUD:**
  - o botão do SHINKAI mostra a porcentagem da reserva que sobra (ou "RUPT.") no lugar dos
    segundos;
  - a camada dourada cobre a barra de vida.

**Simulação do elenco** (`BateriaShinkaiElenco 2 5 8`): os 16 contra todos, com Clash, 480 lutas
por coluna. Tabelas em `perfis/shinkai-vida-nova-elenco.md`. Nível 8:

| medida | relógio (antes) | vida nova (agora) | sem SHINKAI |
|---|---|---|---|
| duração da luta | 58 s | 86 s | 48 s |
| viradas | 29% | 37% | 24% |
| SHINKAI Clash por luta | 0,90 | 1,30 | — |
| transformações quebradas por luta | — | 0,88 | — |
| quem quebra a do outro vence | — | 78% | — |
| quem se transforma primeiro vence | 49% | 51% | — |
| desvio das vitórias do elenco | 18,5 pontos (7%–77%) | 16,5 pontos (13%–73%) | 18,2 pontos (8%–83%) |

**Por tipo**
- O SHINKAI dura 35–39 s (era 14–18 s).
- A vida perdida lá dentro cai a uns 7%.
- **Quebrada x KO:**
  - POWER e PERCEPÇÃO quebram menos (26–27%) e derrubam mais (40–41%);
  - TECHNIQUE e SPEED quebram mais (37–43%) e derrubam menos (18–29%).
- **Quem ganha e quem perde:**
  - o POWER ganha: p04 de 51% para 65%, p09 de 52% para 63%, p12 de 48% para 62%;
  - o ENERGY perde: p11 de 77% para 38%, p08 de 73% para 55%;
  - o p15 segue o mais fraco (13%).
- **Cuidado:** são 60 lutas por lutador em cada coluna, então ±6 pontos é ruído.

### Aura e dragão amarelos (pedido do jogador, 02/11)

A liberação de poder mudou de azul para dourado sem refazer o vídeo, com o
`tools/sprites/recolorir_efeito.py` aplicado sobre a folha pronta:

```
python3 tools/sprites/recolorir_efeito.py shinkai_player_base --para 46
python3 tools/sprites/recolorir_efeito.py shinkai_player_base --de 160-205 --val 0.08 --para 40
```

- **Primeiro comando:** troca a aura, os raios, o dragão e o brilho azul no corpo.
  - Pega os tons de ciano, azul e violeta, de 150° a 355°.
  - A cor nova vem de um degradê pelo brilho de cada pixel: o escuro vira laranja fundo, o médio
    dourado e o claro amarelo quase branco. Assim a profundidade do efeito continua.
- **Segundo comando:** pega os fiapos de fumaça verde-azulada escura que sobraram.
- **O que fica como está:** o colete escuro, a pele, a roupa e o cabelo.
- **Reproduzir:** o comando de conversão do vídeo, mais acima, gera a folha azul; os dois
  comandos acima a deixam dourada.
- O contorno do SHINKAI da PERCEPÇÃO no jogo já era dourado (`0xFFFFC23A`).
