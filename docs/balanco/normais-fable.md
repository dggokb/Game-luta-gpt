# Normais com identidade (branch `game-luta-sprite-fable`, 09/11)

**Pedido do jogador:** "o básico de luta está estranho e genérico; jogar com um é igual a jogar
com todos". Esta branch muda só o básico: os golpes normais do chão (L, 2L, M, 2M, H, 2H), o
passo de cada um e o jeito de mostrar o impacto. Especiais, S1, Super, Ultra, golpes no ar, vida,
dano e as regras do motor (corrente L > M > H, lançador no 2H, agarrão, SHINKAI) ficam como estavam.

Leitura usada para dar a função de cada golpe: as fichas (`docs/personagens/pNN.md`), o
`mecanicas.md`, o `perfis-16.md`, a História Base e o "DBZ FighterZ — Jogabilidade Base" do Drive,
e o desenho do quadro de impacto de cada normal dos 16.

## O que estava errado

| Problema | Medida na `game-luta-sprite-gpt` |
|---|---|
| Os tempos vinham de três moldes | 5 dos 16 com exatamente 3/3/7, 6/4/11 e 10/5/18 em L, M e H; quase todos os outros são o molde "rápido" ou o "tanque" |
| A vantagem na defesa era a mesma | L 0, M −2, H −5, 2H −7 em 15 dos 16 (só o Ryūga saía disso) |
| O jab saía em 3 quadros em 11 dos 16 (2 no Ryūga) | mais rápido que o do mercado (`mercado.md`); os botões ficavam parecidos |
| O alcance não seguia a força do botão | em 9 dos 16 a ordem L < M < H não vale (Necro: L 198, M 181, H 89) |
| O 2H "anti-aéreo" era só um golpe para cima | quem pulava acertava primeiro; `perfis-16.md`: "ninguém usa o ar" |
| O braço recolhia com o golpe ainda ativo | na janela ativa passavam de 2 a 5 desenhos do vídeo; o desenho é por onde o golpe acerta, então o alcance mudava de quadro para quadro |
| O Ryūga andava a 170/140 e o Krüger recuava mais rápido (220) do que avançava (170) | velocidade do vídeo; a animação de andar já acompanha a distância (`distancePerFrame`), então o passo pode ser desenhado sem o pé deslizar |

## A régua

1. **Quem alcança mais longe paga em tempo.** Jab curto sai rápido; golpe que cruza a tela demora
   e deixa aberto. Onde o desenho de um golpe fraco ia longe demais, o golpe ganhou um limite de
   alcance (`alcance`, o `alcanceMaximo` do pacote) em vez de ficar rápido e longo.
2. **O jab diz quem é o personagem.** 3 quadros só no boxeador (p04); 4 em quem pressiona; 5 no
   padrão; 6 nos zoners e pesados; 7 no Krüger.
3. **A vantagem na defesa é do personagem.** Quem pressiona fica em +1/+2 no jab; zoner fica
   negativo de perto; o pesado é lento, mas o H dele não devolve a vez.
4. **A corrente continua igual.** L > M > H e as rotas de cada estilo emendam como antes: o
   atordoamento de cada normal sobe sozinho até cobrir a preparação do próximo
   (`base.corrente`). Conferido no motor, rota por rota (abaixo).
5. **O impacto fica na tela.** O desenho mais esticado da janela ativa dura a janela ativa (até
   5 quadros), em vez de passar no tempo do vídeo.

## Os 16

Preparação em quadros (vantagem na defesa) e andar para frente / para trás em px/s.

| Lutador | L | 2L | M | 2M | H | 2H | Andar |
|---|---|---|---|---|---|---|---|
| **Ryūga (p01)** | 5 (0) | 6 (−1) | 7 (−2) | 12 (−10) | 10 (−5) | 8 (−12) | 280 / 210 |
| **Yin-lu (p02)** | 4 (+1) | 5 (0) | 7 (−1) | 6 (−9) | 8 (−5) | 8 (−11) | 310 / 250 |
| **Kōga (p03)** | 4 (+2) | 5 (−1) | 6 (0) | 9 (−12) | 9 (−4) | 10 (−12) | 350 / 230 |
| **p04** | 3 (+1) | 5 (0) | 6 (−1) | 9 (−11) | 10 (−3) | 8 (−12) | 310 / 200 |
| **p05** | 4 (0) | 5 (0) | 7 (−4) | 6 (−9) | 8 (−5) | 9 (−12) | 340 / 230 |
| **p06** | 6 (−2) | 6 (−2) | 8 (−3) | 10 (−12) | 12 (−7) | 11 (−13) | 250 / 210 |
| **p07** | 5 (−1) | 6 (−1) | 9 (0) | 10 (−10) | 13 (+1) | 11 (−12) | 280 / 250 |
| **p08** | 6 (−2) | 6 (−2) | 8 (−4) | 9 (−11) | 14 (−8) | 12 (−13) | 240 / 210 |
| **p09** | 6 (+2) | 7 (0) | 9 (−2) | 10 (−10) | 12 (−4) | 13 (−12) | 260 / 190 |
| **p10** | 4 (+1) | 5 (0) | 8 (−4) | 6 (−9) | 8 (−3) | 8 (−11) | 360 / 230 |
| **p11** | 6 (−2) | 6 (−2) | 7 (−2) | 9 (−11) | 11 (−6) | 11 (−13) | 250 / 210 |
| **Necro (p12)** | 6 (−3) | 6 (−2) | 8 (−4) | 9 (−11) | 9 (−2) | 10 (−12) | 330 / 190 |
| **Krüger (p13)** | 7 (0) | 8 (−1) | 10 (−2) | 11 (−10) | 14 (−4) | 13 (−12) | 200 / 150 |
| **p14** | 5 (−1) | 6 (−1) | 7 (−1) | 9 (−11) | 11 (−6) | 10 (−12) | 290 / 230 |
| **Natisuko (p15)** | 5 (+1) | 6 (0) | 6 (0) | 11 (−12) | 12 (−3) | 9 (−11) | 280 / 220 |
| **Bonnie (p16)** | 4 (0) | 5 (−1) | 6 (−2) | 8 (−10) | 10 (−5) | 9 (−11) | 330 / 250 |

O H do Ryūga ficou com o tempo que tinha (10/5/17): ele é calibrado por testes próprios e um
quadro não mudava a função.

| Lutador | O básico dele | O que os normais têm de próprio |
|---|---|---|
| **Ryūga (p01)** | Fundamento calmo: jab honesto de 5 quadros, o soco de energia que cutuca de longe e o 2H que tira do ar de verdade | 2L: alcance até 150; 2H: anti-aéreo |
| **Yin-lu (p02)** | Garça: palma rápida e curta de perto, rasteira ligeira e o chute alto que tira do ar | L: alcance até 135; M: alcance até 170; 2H: anti-aéreo |
| **Kōga (p03)** | Pressão: jab que deixa na vantagem, o M que anda para dentro e a rasteira longa e arriscada | L: alcance até 125; 2L: alcance até 150; M: avança |
| **p04** | Boxeador de rua: o único jab de 3 quadros, socos curtos e seguros, gancho pesado e o uppercut anti-aéreo; os chutes são lentos | L, 2L, M e H com alcance curto (135, 140, 165, 170); H: pausa 13; 2H: anti-aéreo |
| **p05** | Ninja: tudo sai rápido e curto; a estocada longa do M e a rasteira ligeira abrem a guarda, mas errar custa caro | 2L: alcance até 135 |
| **p06** | Sombra: fraca de perto (jab lento), a estocada longa para cutucar e o H que empurra para longe, onde ela manda | H: não joga na parede, empurra |
| **p07** | Cavalheiro pesado: lento, mas cada golpe pesa e o H defendido ainda deixa ele na vantagem | M: pausa 11; H: pausa 14 |
| **p08** | Chicote: de perto só tapa e chute lentos; o H cruza a tela, mas demora e não rende combo, só espaço | H: não joga na parede, empurra |
| **p09** | Agarrador: jab lento que deixa na vantagem (para agarrar) e a ombrada que aguenta um golpe e passa por cima | L, 2L e M com alcance curto (140, 170, 185); H: armadura de 1 golpe |
| **p10** | Acrobata: jab curto na vantagem, chute lateral longo para cutucar, H rápido e a bananeira que tira do ar | L: alcance até 120; 2H: anti-aéreo |
| **p11** | Cirurgião: o bisturi alcança longe mas sai devagar; o chute empurra para longe e abre espaço para o veneno | L: alcance até 160; 2L: alcance até 165; H: não joga na parede, empurra |
| **Necro (p12)** | Fera: a garra longa é lenta, o M dá o bote para dentro e a mordida, curta e rápida, é o golpe que dói | L: alcance até 185; M: avança; H: pausa 13 |
| **Krüger (p13)** | Colosso: o mais lento do jogo; cada golpe pesa e quem aguenta os golpes fracos é a couraça, não a vantagem na defesa | 2L: alcance até 200; H: pausa 14 |
| **p14** | Malandro dos chutes: anda bem, cutuca de média distância e muda o ritmo; nenhum golpe é o melhor em nada | L: alcance até 150 |
| **Natisuko (p15)** | Base firme: golpes retos, curtos e seguros na defesa, o chute alto que alcança longe e o chute vertical que tira do ar | 2H: anti-aéreo |
| **Bonnie (p16)** | Palhaça: rápida e solta; o H se joga para a frente e pega quem recua | H: avança |

**Anti-aéreo de verdade** (Ryūga, Yin-lu, p04, p10, Natisuko): do 3º quadro ao último ativo do 2H,
o golpe de quem vem pelo ar passa sem acertar. Projétil e golpe de chão acertam normalmente, e o
2H continua bem negativo na defesa (−11 a −13). Os outros 11 tiram do ar com o especial ou no
tempo certo, como antes.

**H que não joga na parede** (p06, p08, p11): os três zoners. O H acerta em pé, empurra longe
e ainda cancela no S1; o combo é curto (o estilo "alcance"), o prêmio é a distância. Tentei
derrubar com ele, mas a queda fecha o combo no motor e a lição COMBO do tutorial (L, M, H) deixava
de contar o terceiro golpe.

## O impacto segurado

Antes, o clipe do vídeo era espalhado por igual no tempo do golpe. Exemplo, o H do p04 (um
desenho por quadro do jogo; preparação | ativo | recuperação):

```
antes   0 1 2 3 4 5 6 7 9    | 10 10 11 12    | 13 13 14 15 ...
depois  0 1 2 3 4 5 6 7 8 10 | 10 11 11 11 11 | 12 13 13 14 ...
```

- O quadro do impacto entra junto com o primeiro quadro ativo (como já era).
- O desenho mais esticado dos que o vídeo mostrava na janela ativa fica até o fim dela (no
  máximo 5 quadros). Às vezes é o seguinte ao do impacto: o braço ainda está indo.
- No acerto, a pausa do golpe continua somando por cima (8/11/16 quadros por força, da `game-luta-sprite-review`; a "pausa" própria da tabela acima troca os 10 ou 12 que o golpe traz).
- Como o desenho é por onde o golpe acerta (`CombatEngine.golpeNoDesenho`), o alcance agora vale a
  janela ativa inteira. O alcance real medido pelo `GeraAlcanceCpu` só mudou onde há limite ou
  avanço de propósito.
- Clipe de muitos quadros num golpe curto (o M do p14: 20 desenhos em 7 quadros) arredondava
  mal e o impacto entrava cedo; a soma de cada trecho agora é mantida.

## Conferido

| O quê | Resultado |
|---|---|
| Testes do motor (84 classes JUnit do app) | passam todos, mais um novo: `antiAirEvasionSlipsOnlyTheStrikeOfAnAirborneAttacker` |
| Corrente do chão, rota por rota, no motor (cada A que cancela em B, 16 lutadores) | as 173 rotas que emendavam continuam emendando; a Natisuko ganhou L > H e 2L > H |
| L, M, H de perto (a lição COMBO) | 3 acertos em combo nos 16 a 110 px; a 140 px só o p07 não fecha, como antes |
| Quadro do impacto na tela no primeiro quadro ativo | 186 de 186 normais (eram 184: o 2M da p06 e o →M do Krüger estavam fora) |
| `build_characters.py --write` | passa; só `GeneratedCharacters.java` mudou |
| `CpuReachTable.java` | gerada de novo pelo `GeraAlcanceCpu` |

Testes que mudaram de número, de propósito: `P01AttackPacingTest` e `P01CrouchMovementTest` (o
tempo novo do L, M, 2L e 2H do Ryūga), `CombatEngineTest.walkingBackIsSlowerThanWalkingForward`
(usa o lutador de teste, que anda na velocidade do motor), `MechanicsTest.theStretchedLegCanBeHit`
(o jab sai contando o próprio startup), `PressaDoTempoTest` (a folga do dano foi de 10% para 15%:
o pulo da CPU com pressa agora encontra anti-aéreo) e `test_character_packs` (o lutador de teste
não copia mais o tempo do Ryūga).

**Não deu para ver na tela.** Aqui não há SDK Android nem acesso ao Maven, então não rodei o jogo,
o APK nem o `gradle`: compilei o motor e os testes com `javac` e rodei com JUnit. O que precisa de
olho: o impacto segurado (se algum golpe parece travar) e o passo novo de Ryūga, Krüger e p14.

## Simulação (CPU × CPU, nível 8, 10 sementes por confronto)

`BateriaPerfis`, na escala congelada do `perfis-16.md`. Quem joga é a CPU, que quase não usa
vantagem na defesa nem pune (`chao.md`); para gente a diferença deve ser maior.

| Medida | Antes | Depois |
|---|---|---|
| Distância média entre pares: fixa / luta / total | 1,42 / 2,47 / 2,01 | 1,56 / 2,55 / 2,11 |
| Núcleo (Ryūga, Yin-lu, Kōga, p04, p05, p10): fixa / luta / total | 1,11 / 2,83 / 2,10 | 1,04 / 2,81 / 2,04 |
| Par mais parecido | Yin-lu e p04 (0,64) | Yin-lu e p16 (0,84) |
| Vitórias: desvio / mín / máx | 16,9 / 13,3 / 74,0 | 17,3 / 21,0 / 79,3 |

| Vitórias (%) | Ryūga | Yin-lu | Kōga | p04 | p05 | p06 | p07 | p08 | p09 | p10 | p11 | Necro | Krüger | p14 | Natisuko | p16 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| antes | 70 | 41 | 37 | 57 | 44 | 52 | 52 | 60 | 69 | 62 | 43 | 68 | 74 | 29 | 13 | 29 |
| depois | 76 | 35 | 34 | 56 | 39 | 51 | 49 | 63 | 71 | 48 | 41 | 71 | 79 | 40 | 21 | 26 |

- O elenco ficou mais espalhado na ficha fixa (1,42 → 1,56); o núcleo de 6 não (1,11 → 1,04):
  antes o Ryūga era o diferente do grupo por andar devagar e ter jab de 2 quadros, e isso saiu.
  O que separa os seis agora é a função (anti-aéreo, avanço, jab de 3, rasteira rápida), que
  essa ficha não mede.
- **O balanço não foi o alvo e continua torto:** Krüger (79%) e Ryūga (76%) no topo, Natisuko
  (21%) e p16 (26%) no fundo. A Natisuko e o p14 subiram (os golpes dela eram de 16–17 quadros);
  o p10 caiu de 62% para 48%.

## Junto com a `game-luta-sprite-review`

A `game-luta-sprite-review` saiu do mesmo commit e ataca o mesmo problema pelo outro lado (o
impacto, os efeitos, a câmera, o HUD e as poses do Ryūga; `docs/revisao-base/`). Ela foi trazida
inteira para cá. As duas se somam:

| Da review | Como encaixa com os normais |
|---|---|
| Luta a 100% (`RITMO_BASE = 1`) | o jab deixou de ser de 3 quadros ao mesmo tempo em que o jogo deixou de rodar a 92% |
| Pausa no acerto 8/11/16 por força; defesa sem o extra | a "pausa" própria dos pesados (p04, p07, Necro, Krüger) baixou 1 quadro para não passar de 18 no total |
| Poses-chave do Ryūga (`posesChave`) | ficam por cima do tempo novo do golpe: o impacto entra no primeiro quadro ativo e a pose segura a partir dele |
| Sem soco de câmera, tranco, escurecimento e flash branco nos golpes comuns | nada a mudar nos normais |
| Reações em pé por golpe do Ryūga, efeitos de ar, avisos pequenos, sons CC0, cenário menos saturado no OpenGL | independentes dos normais |

Depois da junção: `build_characters.py` passa com 689 folhas, os testes do motor passam (incluindo
os novos `ReacaoDoGolpeTest`, `ImpactFeelTest` e `DesgasteDaLutaTest`), e as 175 rotas de corrente
e o L, M, H dos 16 continuam emendando.

## Onde mexer

Tudo em `tools/sprites/mecanicas.json`, por personagem:

| Chave | O que é |
|---|---|
| `papelNormais` | a frase da tabela acima (só documentação) |
| `framesGolpe` | `{"L": [preparação, ativo, recuperação]}`; vale também com `manterTempo` (Ryūga) |
| `vantagemGolpe` | `{"L": [acerto, defesa]}`; a do acerto é o mínimo (a corrente pode subir) |
| `normais` | por golpe: `alcance`, `lancar`, `empurrao`, `recuo`, `pausa`, `mecanica` (`esquiva` com `"de": "aereo"` é o anti-aéreo; `armadura`; `avanco`) |
| `andar` | `[frente, trás]` em px/s |
| `base.impacto.segurar` | quadros do impacto segurado (5); 0 desliga |
| `base.corrente` | `folga` (8), `teto` da vantagem no acerto (7) e as `rotas` garantidas |

```
python3 tools/sprites/mecanicas.py --so-normais --write   # só os normais e o andar
python3 tools/sprites/build_characters.py --write
# depois: GeraAlcanceCpu (a régua da CPU) e os testes
```

`--so-normais` existe porque o `mecanicas.py --write` inteiro hoje mudaria também especiais, S1 e
Super de 15 personagens: o `character.json` deles não confere mais com o `mecanicas.json` (o teste
`test_packs_follow_mecanicas_json` já falhava antes; a CI não instala o numpy e pula esse arquivo).
Não mexi nisso.

No motor: `AttackDefinition.Reads.AIR` (a esquiva que só o golpe de quem está no ar erra), em
`CombatEngine.evades` e no `AiController`.

## Em aberto

1. Jogar e ajustar: os números são um primeiro desenho, não o resultado de playtest.
2. Balanço: Krüger, Ryūga, Natisuko e p16 (tabela acima).
3. Golpes no ar ainda são o molde vezes o ritmo; dá para fazer o mesmo trabalho neles.
4. O H e o S2 da p08 usam a mesma animação: a cadência dela é a do S2, sem o impacto segurado.
5. `player_two` e `monster_npc` não mudaram. As duas crianças do prólogo ficaram com o tempo
   antigo e ganharam só o impacto segurado; o Ryūga criança recua a 110 px/s em vez de 39,6.

## O efeito do acerto

A review trocou os efeitos de vídeo do acerto por um impacto de ar desenhado em código e tirou
do golpe comum o clarão branco, as linhas, o rastro e o soco de câmera. Essas regras ficam. O
que mudou aqui foi só o desenho do impacto (`tools/sprites/impacto_ar.py`):

- antes era um clarão macio com rajadas finas e uma elipse translúcida, que sumia em cima do
  cenário e da roupa clara;
- agora é borda dura e tons chapados, no traço dos lutadores: estrela branca de espinhos
  desiguais com borda lilás e fio de nanquim, miolo de nanquim com um ponto branco no quadro do
  contato, depois cacos e um anel de pressão quebrado;
- o tamanho é o mesmo (a estrela do forte com 128 px do mundo; fraco e médio em 0,625 e 0,8125),
  o tempo é o mesmo (a pausa do golpe + 8 quadros) e o motor não mudou: as folhas
  `impacto_ar` e `impacto_ar_super` têm os mesmos nomes.

Conferido em quadro renderizado fora do jogo (o chute forte do Kōga no Ryūga, com a pausa de 16
quadros e o tremor só de quem apanha). Falta ver no aparelho.
