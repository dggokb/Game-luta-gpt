# Seleção sequencial e concepts — 12/10/2026

Base confirmada: ui/selecao-efeitos-20261012 em 2a151885b8461a36de92ce4fae84b287d352c9fb. Nova branch: ui/selecao-sequencial-concepts-20261012. Sem merge com as branches protegidas.

## Escolha

Uma escolha de retrato confirma uma etapa: P1, TAG P1, P2, TAG P2. O próximo slot recebe foco automaticamente. Os painéis TEAM são indicadores e não mudam o foco. O banner mostra a etapa atual. Slots pendentes mostram ESCOLHA/AGUARDANDO. Apenas após as quatro escolhas A confirma a luta. Antes disso A escolhe o cursor atual. B desfaz uma etapa; na primeira etapa cancela o menu. Não é possível substituir o titular confirmado escolhendo-o novamente como TAG. Os quatro IDs permanecem distintos dentro de cada equipe; espelhamento entre equipes é permitido. TAG P2 é armazenada em opponentTagId, mesmo com CPU ainda de um membro.

## Arte

P01–P16 conferidos na pasta concepts do Drive (1mGv56-qpEFg5GoFzoL4irTcsoST88BT2). P04 e P10 foram atualizados; P13–P16 foram adicionados ao repositório. As demais imagens coincidem com os arquivos já versionados. Os downloads são validados por decodificação completa antes de entrar no APK.

Os oito espaços inferiores usam os retratos verdadeiros P09–P16, com nome e cadeado. Os antigos personagens inventados da imagem de referência não são renderizados. O roster de gameplay permanece explicitamente limitado aos oito IDs implementados, independentemente dos retratos futuros.

P02/P03/P04/P08 foram isolados com transparência a partir das poses de ação do bloco superior direito de cada concept. O prompt de extração exige a quarta figura em ação (quando há vistas frontal/lateral/costas), preservação da pose, identidade, cores e roupa, remoção da ficha e do fundo, sem aura ou novo personagem. P02 tem os dois punhos na guarda; P03 tem o soco curto do concept; P04 usa cabelo grisalho; P08 usa a ação horizontal com chicote. P05, P06 e P07 já correspondiam àquela pose e foram preservados. Por orientação do usuário, P01 mantém exatamente a arte anterior e seu enquadramento centralizado. As âncoras por personagem mantêm o rosto visível ao espelhar cada lado.

## Efeitos

Auras laterais azul/vermelha via shaders pré-criados; quarenta faíscas maiores, com rastro de 24 unidades; glows em três camadas com largura 24/14/4; cursor com núcleo branco; flash de escolha de 380 ms, inclusive ao confirmar o mesmo personagem que já estava no preview. Movimento por relógio monotônico; geometria e retratos permanecem estáveis.

## Verificação

Testes do modelo cobrem ordem, avanço automático, recusa de TAG duplicada, bloqueados em todas as etapas, volta, configuração, aleatório e confirmação. Um teste de integração usa GameView e aplica as duas equipes ao motor. QA no APK toca somente retratos, valida preenchimento por etapa e bloqueio P09–P16, captura os oito heróis, grava os efeitos e espera o primeiro frame real da luta durante carregamento de sprites.
