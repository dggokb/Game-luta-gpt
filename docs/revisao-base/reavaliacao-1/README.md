# Reavaliação: luta Ryūga × Ryūga, ANTES (luta_ryuga2) × DEPOIS (luta_ryuga5)

Escala das notas: 0 a 10, em que 10 é o nível das referências (FighterZ como base).
Quadros: "antes qNNNN" = `/tmp/luta_ryuga2/qNNNN.jpg`, "depois qNNNN" = `/tmp/luta_ryuga5/qNNNN.jpg`.

Folhas de comparação (todas em ``):
- `reav_1_golpe_forte.jpg`: chute forte antes (q0056–59) × depois (q0330–33) × FighterZ V02 q0184–87
- `reav_2_fraco_medio.jpg`: faíscas de fraco e médio antes × depois × FighterZ q0194–97
- `reav_3_ruido_textos.jpg`: números, flash branco e faixas antes × o que sobrou depois
- `reav_4_faisca_cobre_luta.jpg`: problema novo, a faísca forte cobrindo a luta × FighterZ
- `reav_5_contra_defesa.jpg`: contra-ataque magenta e candidatos a "defesa ciano" × FighterZ contra-ataque
- `reav_6_poeira_sombra.jpg`: poeira e sombra antes × depois × FighterZ

## Antes de tudo: limites desta captura

- **A taxa de quadros do jogo está baixa nas duas capturas.** O contador do próprio jogo marca 12–21 FPS antes e 17–31 FPS depois, com "quadro 35–55 ms" (ex.: depois q0097 "31 FPS", depois q0337 "17 FPS"). Por isso não dá para medir a pausa no acerto em quadros de 60 Hz, nem julgar fluidez. Os tempos abaixo estão em **quadros da captura**.
- **Calibração pelo cronômetro "TESTE".** Antes: q0064 "28s" → q0192 "17s", ≈11,6 quadros da captura por segundo do cronômetro. Depois: q0048 "28s" → q0152 "18s", ≈10,4. Cada quadro da captura vale ≈86–96 ms do cronômetro, não 50 ms. A diferença (+11% de tempo de jogo por quadro capturado) **é compatível com a troca de 92% para 100%**.
- **Modo de teste.** A coluna de botões à direita (GRÁF ALTO, IA OFF, EFEITOS ON…) e as quatro linhas de diagnóstico embaixo estão nas duas lutas. Elas cobrem o pé dos lutadores e, no depois, o contador de combo do P2 (q0337). A partir daqui não pesam na avaliação.
- **Sem áudio.** Som, música e voz não foram avaliados.

## Confirmação das mudanças declaradas

| Mudança declarada | Veredito | Evidência |
|---|---|---|
| Pausa no acerto menor e escalonada | **Parcialmente confirmada** | Fraco: a faísca dura 2 quadros e no 3º já há movimento (depois q0042–44). Contra-ataque: imagem praticamente parada de q0207 a q0214 (≈8 quadros). A escala é visível, mas os números em quadros de 60 Hz não podem ser medidos (ver os limites acima). |
| Sem soco de câmera, tranco, escurecimento ou tremor nos golpes comuns | **Confirmado** | Medi o deslocamento do cenário por correlação de fase, quadro a quadro. Antes, a câmera oscila em vaivém de ±10–19 px (q0164–0171, q0176–0180, q0242–0248). Depois, não há vaivém nos golpes comuns, só deriva de 1–6 px do enquadramento. O cenário só escurece na ativação do SHINKAI/Super (q0056, q0147, q0222). |
| O atingido não pisca mais em branco | **Confirmado** | Antes: atingido virando silhueta branca em q0137–0138 e q0190–0191. Depois: nenhum caso na luta inteira. |
| Luta a 100% | **Compatível** | Pelo cronômetro (+11% de tempo de jogo por quadro capturado). Não é prova, porque a taxa de quadros varia. |
| Sombra no chão mais escura | **Confirmado, mas tímido** | Depois q0020: aparece uma elipse escura sob os pés. Antes q0030: quase invisível. Ainda é uma mancha suave, não a sombra dura da silhueta do FighterZ. |
| Sem número de dano do SHINKAI | **Confirmado, e foi além** | Não há **nenhum** número flutuante em toda a luta depois. Antes havia 250, 121, 1107, 2202, 3139, 1000 e 2008 sobre os lutadores. |
| 10 efeitos novos | **9 vistos, 1 incerto** | • fraco: q0033, q0042<br>• médio: q0103, q0145<br>• forte: q0116, q0215, q0331<br>• contra magenta: q0052–54, q0129, q0168–170, q0207–210<br>• rastro do chute forte: q0126, q0206, q0330<br>• poeira de lascas: q0040, q0125, q0278<br>• nuvem na queda: q0174–176<br>• nuvem de investida/aura: q0224, q0264<br>• **defesa ciano: não consegui achar com segurança.** Os candidatos são a estrela azul de 4 pontas com anel (q0057–58, igual à faísca média recolorida) e uma "flor" branco-ciano sem função clara (q0133, q0332). |

## 1. Os 8 itens do diagnóstico

### 1. O golpe não acontece na tela — antes 2 → depois 5

- **Antes** (`reav_1`, q0054–0059): a faísca é um estouro branco pequeno, ≈1/6 da altura, sem cor nem direção. No q0057 a perna ainda não está esticada no quadro da faísca, e o atingido quase não muda de pose.
- **Depois, golpe forte** (q0330–0333): é a melhora mais visível do trabalho. Tem crescente desenhado no chute (q0330). No contato vêm uma estrela laranja-amarela de espinhos com contorno escuro e raios brancos cruzando a tela (q0331), que depois se quebram em chama laranja com detritos escuros (q0332–0333). É a mesma gramática do FighterZ V02 q0185–0187.
- **O que ainda falta:**
  - a faísca forte ficou **grande e opaca demais** e cobre o golpe (ver o item novo N1);
  - o fraco e o médio são fracos (ver item 4 e N1);
  - a pose de impacto continua sendo o quadro do vídeo, sem exagero nem pose segurada;
  - o atingido quase não reage (depois q0042–44: o boneco atingido continua na mesma pose).

O golpe agora "acontece", mas acontece **na faísca, não no corpo**.

### 2. Tudo grita ao mesmo tempo — antes 2 → depois 5

- **Saiu:** número de dano, atingido branco, soco e tranco de câmera, escurecimento nos comuns e a faixa PUSHBLOCK! (vista antes em q0044–0049 e nenhuma vez depois).
- **Ficou:**
  - faixa grande "PUNISH COUNTER!" no centro (q0208–0212);
  - faixa "SHINKAI ESGOTADO" (q0274–0278);
  - dragão azul de tela cheia três vezes em ≈17 s (q0080–0091, q0236–0258, q0312–0318);
  - estouro branco de tela inteira e quadro invertido no Super (q0107–0109, q0149, q0180). No Super isso é aceitável.
- **O grito mudou de lugar:** agora é a faísca forte que grita, em todos os acertos de um golpe de vários acertos (q0334–0343) e igual no Ultra (q0244–0256). O ruído em volta caiu bastante, mas o golpe comum ainda não é "econômico".

### 3. Linguagem de outro tipo de jogo — antes 2 → depois 4

- O número flutuante sumiu. Era o pior sinal, e isso pesa muito.
- Continua linguagem de celular/RPG:
  - **faixas no centro da tela** (PUNISH COUNTER!, SHINKAI ESGOTADO);
  - contador de combo em fonte de sistema itálica, com **dados de depuração para o jogador**: "DANO 8400 • ESCALA 100% • JUGGLE 0" (depois q0270, q0276);
  - o contador do P2 fica no lado direito, atrás dos botões (q0337).
- Nada de textos pequenos, dourados e empilhados do lado de quem bateu, nem contador em pincel (FighterZ "N golpes").

### 4. Cada peça vem de um mundo diferente — antes 3 → depois 4

- **Melhorou:** os efeitos novos conversam entre si. São chapados, com contorno escuro, no estilo anime do FighterZ (estrela forte, contra magenta, rastro, lascas).
- **Ficou mais evidente, porque os efeitos agora são limpos e o resto não:**
  - lutador sombreado suave, tirado de vídeo, sem contorno e com halo ciano borrado;
  - dragão do SHINKAI pintado, translúcido e brilhante (q0084, q0240);
  - aura de chama azul aerografada (q0228, q0264);
  - cenário pintado realista;
  - Ultra com faixa amarela lisa e texto de sistema (q0181–0189);
  - poeira cinza-bege, não nas cores do chão azul-dourado (q0175, q0264; compare dbfz_poeira em `reav_6`).
- A colagem diminuiu nos efeitos, mas a diferença entre "efeito desenhado" e "boneco de vídeo" ficou mais fácil de ver.

### 5. O lutador perde para o cenário — antes 3 → depois 3,5

- Mesmo cenário (Templo da Lua: estandartes vermelhos, cerejeiras, tochas acesas na altura da luta) e mesmo render do lutador.
- **Ajudam um pouco:** a sombra mais escura (q0020) e, só no lutador em SHINKAI, o halo ciano (q0097).
- **Continua igual:** o lutador sem halo some contra o chão dourado e as tochas (depois q0097, lutador da direita; q0171).
- Nada de contorno nem de sombra dura.

### 6. Animação de vídeo e não de desenho — antes 3 → depois 3,5

- Não há sinal de poses-chave seguradas. As poses continuam sendo quadros do vídeo, com o mesmo peso do começo ao fim (depois q0328–0330, q0344–0347).
- O meio ponto vem do crescente desenhado no chute forte (q0330), que dá ao golpe um "desenho" que o corpo não tem.
- A captura a 17–31 FPS não deixa julgar a cadência real.
- **Este item não foi atacado.**

### 7. O tempo da luta está mole — antes 3 → depois 5

- O cronômetro é compatível com a luta a 100%.
- O fraco solta rápido (q0042–0044).
- O contra-ataque segura bem mais (q0207–0214): há escala.
- Não dá para afirmar os 8/11/14 nem a defesa 5/6/7 com esta captura.
- **O que ainda deixa mole:**
  - a faísca forte que se repete por ≈10 quadros num mesmo golpe (q0334–0343);
  - as entradas de SHINKAI e dragão, longas e repetidas (q0059–0096 ≈ 37 quadros; q0222–0262 ≈ 40 quadros).

### 8. A câmera não fica quieta — antes 3 → depois 6

- O tremor e o soco nos golpes comuns sumiram (medido).
- O tamanho do lutador no neutro ficou estável em ≈50% da altura, com os pés a ≈89% (depois q0097, igual a antes q0030). Ainda fica abaixo dos 56–63% das referências.
- **Ainda incomoda:** no pulo alto a câmera sobe tanto que o chão sai do quadro e o cenário vira lago e telhados (depois q0138–0143, q0154–0157). Isso tira a referência de chão. O FighterZ sobe a câmera, mas sem perder o enquadramento da luta.

## 2. Itens novos que denunciam amador (não estavam no diagnóstico), em ordem de peso

1. **N1. A faísca forte ficou desproporcional e sem hierarquia** (`reav_4`).
   - Ocupa ≈55–65% da altura da tela (o FighterZ fica em 25–35%), e o núcleo creme fica **opaco** por vários quadros.
   - Num golpe de vários acertos ela reaparece a cada acerto. De q0334 a q0343 **nenhum quadro mostra os lutadores**: não se vê a pose de impacto, a reação nem o empurrão.
   - A mesma estrela é usada no especial (q0116, q0121), no contra (q0215–0216) e no Ultra (q0244–0256), então o Ultra não parece maior que um chute forte.
   - No FighterZ (q0185, q0194) o atacante, o rastro e o atingido continuam legíveis através da faísca. É exagero típico de quem acabou de descobrir o efeito.
2. **N2. A faísca média lê como brilho mágico, não como pancada** (`reav_2`, q0103, q0145). É uma estrela de 4 pontas fina, com anel e disco dourado translúcido, o vocabulário de "item coletado" ou reflexo de lente. A fraca (núcleo, anel e dois raios, q0033) é aceitável, mas também tem o anel de lente. Pelo diagnóstico, o médio é o golpe que mais aparece num combo, e é o que menos parece golpe.
3. **N3. Taxa de quadros** (a confirmar no aparelho). O próprio jogo mede 17–31 FPS e 35–55 ms por quadro no "Linux PC". Se isso também acontecer no Android, é o que mais denuncia amador, mais que qualquer efeito. Se for só o ambiente de captura, ignore.
4. **N4. O atingido não reage com o corpo.**
   - Em pé, a pose quase não muda (q0042–0044).
   - A queda é rígida: o boneco cai deitado de uma vez, fica ≈3 quadros e senta (q0171–0177), sem giro nem quique.
   - Com o flash branco e o tremor fora, ficou claro que não existe reação desenhada por baixo.
5. **N5. A apresentação do poder mistura três estéticas.**
   - Dragão translúcido pintado com brilho (q0082–0090, q0236–0252).
   - Faixa do Ultra: retângulo amarelo liso, texto de sistema, "Toda a força da casa!", sobre outro cenário laranja (q0181–0189). Parece provisória.
   - Faíscas chapadas.
   - O antes tinha uma apresentação de Ultra em quadros de mangá ("VRUUUM", "KRAAK!", "KABUUUM!", antes q0224–0272), que foi trocada por essa faixa.
6. **N6. A poeira é genérica** (`reav_6`). Nuvem e lascas cinza-bege, iguais em qualquer chão, e pequenas (a nuvem da queda tem ≈15% da largura, contra ≈40% no FighterZ). Pelo menos agora existe.
7. **N7. O HUD de combo é de depuração.** "DANO / ESCALA / JUGGLE" na tela do jogador (q0270, q0276) e o contador do P2 atrás dos botões (q0337).
8. **N8. Fusão com imagem dupla na entrada** (antes q0016, depois q0010–0012). A troca de pose da intro para a guarda é um cross-fade com o boneco duplicado e semitransparente, que parece defeito. Estava nas duas lutas.

## 3. Nota geral e próximas mudanças

**Nota geral: antes 3 → depois 4,5.**

A sensação de amador **diminuiu de verdade, uns 25–30% da distância até as referências**. Quase tudo veio de duas frentes:
- **limpeza:** número flutuante, flash branco, tremor e faixa PUSHBLOCK fora;
- **faísca forte desenhada.**

O que ainda denuncia está no **corpo**: boneco de vídeo sem contorno, sem pose segurada e sem reação, perdendo para o cenário. A faísca forte nova passou do ponto e esconde esse corpo, o que dá um "uau" no primeiro golpe e cansa no terceiro.

### As 5 próximas mudanças de maior efeito

1. **Calibrar as faíscas (H e M) e criar a regra de multi-acerto.** **Só código** para a calibragem; **arte** para refazer a M.
   - H com 25–35% da altura da tela.
   - Núcleo opaco só no 1º quadro, depois alfa caindo.
   - Nunca mais longa que a pausa.
   - Em golpe de vários acertos, só o último (ou o primeiro) usa a H; os do meio usam L ou M.
   - Ultra e Super com faísca própria, maior e de outra cor.
   - Refazer a M como estrela espetada (não de 4 pontas) e tirar o anel de lente da L e da M.
2. **Contorno tingido de 2–3 px e sombreamento mais duro no lutador (sombreador), com sombra dura da silhueta no chão.** **Só código.** Ataca os itens 4 e 5 juntos, é o que falta para o boneco combinar com os efeitos novos e vencer o cenário, e tira o halo ciano borrado como único recorte.
3. **Poses-chave seguradas e pose de impacto exagerada no Ryūga** (L, M, H, especial): 3–6 poses por golpe e a de impacto segurada durante os ativos. **Arte/vídeo** para escolher e retocar as poses, mais **código** para a tabela de segurar.
4. **Reações de dano e queda.** Alto, tronco e forte em pé; queda com giro e quique, ≈0,7 s no chão e levantar. **Arte/vídeo.** Depois da limpeza, a falta de reação ficou exposta.
5. **Textos e HUD na linguagem de jogo de luta.** **Código** para posicionar; **arte** para a fonte e o pincel.
   - Tirar DANO, ESCALA e JUGGLE do HUD de jogador.
   - Contador de combo em pincel do lado de quem bate.
   - Trocar PUNISH COUNTER! e SHINKAI ESGOTADO por textos pequenos, dourados, inclinados e empilhados no canto do atacante.
   - Fonte própria.

**Fora da lista, mas antes de tudo:**
- confirmar 60 FPS no aparelho (N3);
- **som**, que segundo o diagnóstico é a 2ª maior diferença e não pôde ser avaliado aqui: acerto separado por força e defesa, vozes, música e locutor.
