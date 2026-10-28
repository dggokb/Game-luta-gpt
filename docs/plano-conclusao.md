# Plano de conclusão: campanha e versus local

Este plano junta três documentos numa ordem só, com o que depende de quê:

- o D03, a arte e a história ([historia/plano-ep04-09-takatoshi.md](historia/plano-ep04-09-takatoshi.md));
- o D04, o treino e o versus ([modo-treino-versus.md](modo-treino-versus.md));
- o C04, a liberação de p13–p16 (anexo da frente F em [balanco/auditoria.md](balanco/auditoria.md)).

Os custos estão em dias de código de uma pessoa e são ordem de grandeza. A arte é gerada fora do código, por você, no Flick ou no gerador de imagens. Por isso ela anda em paralelo e entra quando chegar.

## Onde estamos

| Frente | Pronto | Falta |
|---|---|---|
| História ep00–ep03 | páginas, lutas e tutorial | — |
| História ep04–ep09 | motor, `episodio.json`, lutas, painéis provisórios | 21 páginas, Arena VIP |
| ep07 Sob a Mesma Lua | motor e episódio com páginas provisórias | 8 páginas, Terraço da Lua |
| Takatoshi e ep10 | cartão "Em breve" no Arcade | personagem inteiro, Arena do Templo, roteiro do ep10 (proposta) |
| p13–p16 | jogáveis (golpes, S1–S4, Super, Ultra), no Arcade e no Modo Teste | intro e vitória, nome no jogo de p14 e p16, assist útil (frente G2), p14: idle/andar/corrida, normais mais lentos, S1 no ar |
| Versus local | seleção de 4 escolhas, motor simétrico, controles no PC | entrada do lado 2, seleção por controle, fluxo e HUD P1/P2 |
| Treino | Modo Teste, IA OFF, DEBUG com caixas e vantagem | gravar e tocar, situações salvas, histórico de entradas |

## Ordem

Cada marco termina com algo jogável e testável. Os marcos de código (C) e os de arte (A) andam em paralelo.

| Marco | O quê | Código | Arte (você) | Depende de |
|---|---|---|---|---|
| **C1** | Versus no PC com dois controles (D04 2-A) | 3–4 d | — | — |
| **C2** | Gravar e tocar o boneco e registro de entradas em arquivo (D04 1.1 e 1.3) | 2,5–3 d | — | — |
| **Playtest 1** | Roteiro de [balanco/playtest.md](balanco/playtest.md), com humano contra humano | — | — | C1, C2 |
| **A1** | P1 da história: ep04 (3 páginas), ep06 (2), ep08 (1) | 0,5 d para recortar e ligar | 6 páginas | — |
| **C3** | Liberar p13–p16 no torneio: nomes de p14/p16, intro e vitória com os quadros que já existem, comentário do `torneio.json`, `ArcadeTest` e `TorneioTest` | 1–1,5 d | intro e vitória dos quatro (8 vídeos), se quiser animação própria | G2 (assists) e sua aprovação |
| **A2** | P2: ep05 (4), ep09 (3), Arena VIP (9 peças) | 0,5 d | 7 páginas e o cenário | — |
| **C4** | Situações salvas, caixas e vantagem para o jogador, opções do boneco (D04 1.2, 1.4, 1.5) | 3,5–5 d | — | C2 |
| **A3** | P3: ep07 (8 páginas) e Terraço da Lua; transições (4 páginas, proposta) | 0,5–1 d | 12 páginas | aprovação das transições |
| **D1** | Aprovar a ficha do Takatoshi, a história do contrato e do Dafu, os agentes de máscara branca e o ep10 | — | — | você |
| **A4** | Takatoshi: base, ~32 vídeos, Ultra; Arena do Templo; páginas do ep10 | 5–7 d (personagem, mecânica de duas fases, CPU de chefe, ep10, último passo do Arcade) | o pacote inteiro | D1 |
| **C5** | Versus no celular com controles Bluetooth (D04 2-B) | 2 d | — | C1 |
| **Playtest 2** | Campanha inteira com p13–p16 e Takatoshi; balanço do chefe | — | — | A4, C3 |

**Caminho mais curto até "jogo completo":** C3 → A1/A2 → D1 → A4. O código soma cerca de 8–10 dias, e a arte é o gargalo.

**Caminho mais curto até "playtest que decide o balanço":** C1 → C2, em 5,5–7 dias.

## Decisões que travam o plano

1. **Takatoshi e ep10:** ficha, mecânica de duas fases e o roteiro marcado como proposta. Trava o A4.
2. **Liberar p13–p16:** só depois dos assists (G2) e com a intro e a vitória decididas. Elas podem ser provisórias, com os quadros de idle e de S1, ou animação nova.
3. **Revelação do desejo, que pode ressuscitar a mãe:** nenhum episódio mostra isso hoje. A sugestão é uma página depois da semifinal, junto com o A1.
4. **Versus no toque dividido (2-C):** fica fora até ter tablet para testar.

## Testes por marco

- **C1:** versus com dois `FighterInput` roteirizados, sem `AiController`; o K.O. dá a vitória ao lado certo; a seleção confirma as 4 escolhas.
- **C2:** gravar e tocar dá o mesmo hash de estado em duas repetições (molde da `BateriaBase determinismo`).
- **C3:** `ArcadeTest` com 8 duplas prontas e `TorneioTest` com a chave nova; `CharacterSelectTest` com os nomes.
- **A1–A3:** o `TorneioTest` confere a ordem dos passos com `"irmao"`; o recorte de cada página é conferido na imagem de conferência que o `recortar_pagina.py` gera.
- **A4:** suíte do personagem (como p13–p16), `BateriaComparacoes matriz` com o Takatoshi contra os 16, e a curva por nível (T23) para o chefe.
