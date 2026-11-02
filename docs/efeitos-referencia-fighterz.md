# Efeitos do Dragon Ball FighterZ: levantamento (referência de gênero)

Para guiar os efeitos **originais** do jogo: o que o FighterZ mostra em cada evento da luta, o
que o nosso jogo já tem e o que falta. Não é para copiar arte.

## Fontes e o quanto confiar

- **[Dustloop]** wiki da comunidade (não é documento da Arc System Works):
  [Mechanics](https://www.dustloop.com/w/DBFZ/Mechanics) e
  [Damage](https://www.dustloop.com/w/DBFZ/Damage). Descreve as regras (quadros, estados) e quase
  nada do visual; o que ele diz do visual está marcado.
- **[Kotaku]** [análise da animação de Guilty Gear e FighterZ](https://kotaku.com/breaking-down-the-animation-of-guilty-gear-and-dragonba-1836849279)
  (2019): animação "limitada" como no anime, poucos quadros, fluidez sem quadros sobrando. Aponta a
  palestra da GDC de Junya Motomura (Arc System Works):
  [youtube.com/watch?v=yhGjCzxJV3E](https://www.youtube.com/watch?v=yhGjCzxJV3E).
- **[jogo]** o que se vê jogando, de memória: pode ter erro de detalhe. Para fechar um efeito,
  conferir em vídeo de luta quadro a quadro.

## O que faz os efeitos dele funcionarem

1. **Poucos desenhos, segurados.** O efeito troca de desenho a cada 2 ou 3 quadros (como no anime),
   não a cada quadro; a força vem das poses, não da suavidade. [Kotaku]
2. **O congelamento (hitstop) segura o efeito.** De 6 a 16 quadros pelo nível do golpe
   [Dustloop]: o clarão do acerto fica parado no auge enquanto a luta congela, e só depois o resto
   anda. Golpe mais forte = congela mais = efeito maior.
3. **Camadas, cada uma com uma função:** clarão (o instante), faísca (a força, na direção do
   golpe), anel/onda (o choque), riscos e partículas (o que sobra). [jogo]
4. **Cor diz o que aconteceu:** vermelho no contra-golpe [Dustloop], verde na largada do Dragon
   Rush [Dustloop], branco no tech de chão [Dustloop], a cor da aura de cada um no Sparking. [jogo]
5. **A câmera e a tela fazem metade do trabalho:** ângulo e zoom mudam nos golpes "Smash" (os que
   quicam na parede/chão) [Dustloop]; o cenário escurece no Super; tremor nos impactos. [jogo]
6. **Fogo e fumaça só onde há explosão de verdade** (raio de ki, cenário destruído no KO).
   O acerto comum é clarão + faísca, sem fogo: foi o erro do `impacto_forte` v2. [jogo]

## Evento por evento

| Evento | No FighterZ | Nosso jogo hoje | Falta / proposta |
|---|---|---|---|
| **Golpe fraco / médio / forte** | Faísca clara (branco, borda amarela) do tamanho do nível, aberta para onde o golpe vai, com riscos; congelamento de 6 a 16 quadros [Dustloop] [jogo] | `ImpactEffects` (faísca clássica), `RiscosDeImpacto` (riscos de mangá), `ImpactFeel` (soco de câmera, cenário escurecendo nos fortes); `impacto_forte` v2 em teste | **Principal lacuna.** Uma família só (`impacto_fraco/medio/forte`): clarão + faísca direcional, sem fogo nem fumaça |
| **Defesa** | Faísca própria de bloqueio, diferente da de acerto [jogo] | Faísca de defesa (arco e hexágono azul) | Vídeo `defesa`: estalo curto, cor fria |
| **Contra-golpe** | Clarão vermelho em quem apanhou, "COUNTER" e voz do narrador [Dustloop] | Aura de counter, giro de parafuso no punish | Já coberto |
| **Smash (quique na parede/chão)** | A câmera muda de ângulo e aproxima [Dustloop]; no impacto, cratera, pedras e poeira [jogo] | Cues WALL_BOUNCE/GROUND_BOUNCE, câmera e escurecida | `impacto_chao` (pendente) e um `impacto_parede` novo: pedras e poeira |
| **Queda (knockdown)** | Poeira e pedrinhas no chão [jogo] | Poeira clássica | `impacto_chao` (pendente) |
| **Tech / levantar** | Pisca branco e rola [Dustloop] | Air tech e opções de levantar | Piscar branco curto no corpo, se ainda não houver |
| **Agarrão (Dragon Rush)** | Clarão verde no quadro 7 da largada [Dustloop] | Agarrão e tech | Clarão de agarrão numa cor própria (aviso para quem vai fugir) |
| **Dash / super dash** | Aura, linhas de velocidade e rastro [jogo] | Rastro no dash (motor de animação) | `poeira_corrida` e `poeira_freada` (pendentes) |
| **Pulo / pouso** | Sopro de poeira [jogo] | Sopros clássicos | `fumaca_pulo` e `fumaca_pouso` (pendentes) |
| **Projétil (ki blast)** | Bola brilhante com rastro; o super dash rebate [Dustloop] [jogo] | Brilho, cauda de cometa, faíscas; anulação de projéteis | Estouro do choque entre dois projéteis |
| **Raio** | Raio com núcleo branco e camadas de cor [jogo] | Ultra com raio estilo Kamehameha | Já coberto |
| **Troca / assist** | Parceiro entra voando com super dash; o assist do tipo C tem um círculo dourado [Dustloop] | Assist, tag, DHC | Rastro de entrada do parceiro |
| **Sparking (modo turbinado)** | Explosão de aura e aura que fica; o retrato pisca quando está acabando [Dustloop] [jogo] | SHINKAI: corte de mangá, auras, clash | Já coberto |
| **Super** | Congelamento, cenário escuro, brilho do corpo [Dustloop] [jogo] | `SuperFlash` | Já coberto |
| **Super nível 3 / cinemática** | Cena com câmera própria, ninguém interrompe [Dustloop] | Ultra "Página Final" | Já coberto |
| **KO** | Câmera lenta e clarão; o golpe final pode jogar o rival através do cenário (Destructive Finish) ou virar cena do anime (Dramatic Finish) [jogo] | Clarão e câmera lenta no KO, finalização de mangá no Super/Ultra | Já coberto (o "através do cenário" seria um extra grande) |

## Conclusão

A apresentação grande (Super, Ultra, KO, SHINKAI, impact frames) o jogo já tem. O que falta é a
**camada de base**, que aparece o tempo todo: faíscas de acerto e defesa, poeira de movimento e
impacto no chão/parede. São os efeitos da lista de `docs/efeitos-de-video.md`.

Ordem sugerida:
1. `impacto_fraco`, `impacto_medio`, `impacto_forte` como **uma família**: o mesmo desenho
   crescendo com o nível, clarão + faísca direcional, sem fogo.
2. `defesa`.
3. `impacto_chao` + poeiras (`fumaca_pouso`, `fumaca_pulo`, `poeira_corrida`, `poeira_freada`).
4. `impacto_parede` (novo).
