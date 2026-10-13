# Seleção fiel ao conceito — 12/10/2026

Base: game-luta-sprite-gpt em 32a5e9e23513ddcde57f4cc75f26d515b3a27e57. Branch de teste: ui/selecao-concepts-fiel-20261012.

## Correção

A versão anterior usava fichas inteiras de personagens nos retratos e nas figuras laterais.
Agora o renderizador extrai somente os closes corretos das fichas p01…p08. As figuras laterais
usam artes com transparência derivadas dos mesmos conceitos: P01 individual, P02–P05 no atlas 2×2 e P06–P08 individuais.
O fundo usa o templo, dragão azul e tigre vermelho da referência, reconstruído sem UI.
Somente a área do preview do Templo da Lua e os retratos futuros bloqueados reutilizam
a imagem de referência diretamente. A seleção, suas bordas, seus jogadores e a TAG são dinâmicos.

## Estado e entrada

- Grade 4×4 simétrica: topo mais estreito, base mais larga, sem rotação lateral.
- Desenho e toque compartilham a projeção de CharacterSelect.cardCorners/cardAt.
- As duas duplas têm titular e TAG independentes. Trocar uma seleção muda as artes imediatamente.
- O elenco jogável continua sendo exatamente GeneratedCharacters.SELECTABLE: oito lutadores.
- Sete retratos futuros da imagem aparecem com cadeado e BLOQUEADO; o último espaço tem cadeado.
  Esses slots não criam personagens ou movimentos e não respondem à seleção.
- A abertura inicial apresenta Ryūga/P08 contra Kōga/P06, para corresponder à composição azul/vermelho.
  As escolhas só entram no combate ao confirmar. Reabrir a seleção preserva a dupla atual.
- O parceiro P2 continua selecionável na tela, mas a CPU ainda usa somente seu titular no combate.
- O único cenário implementado continua sendo Templo da Lua.

## Artes e prompts

Originais: art/concepts/p01.png…p08.png; confirmados também na pasta concepts do Drive do jogo.
Os recortes de rosto são normalizados e específicos de cada ficha, incluindo os closes inferiores
de P05, P06, P07 e P08. Nenhuma ficha inteira aparece como avatar.

Os assets novos estão em android/app/src/main/assets/selection/art/.
Geração/edição feita com imagegen integrado, com preservação da identidade e cores dos conceitos:

- backdrop.png: editar a referência; preservar templo noturno em perspectiva baixa, lua central,
  dragão azul no alto à esquerda e tigre vermelho no alto à direita; remover toda UI e humanos;
  reconstruir somente o ambiente, sem texto, sem novos objetos e sem inclinação lateral.
- hero_p01.png: extrair/recriar somente o grande lutador azul à esquerda da referência, usando
  também p01.png como identidade; preservar pose inclinada, punho em perspectiva, cabelo castanho,
  colete azul/branco e gola vermelha; enquadrar cabeça completa até coxas, fundo transparente,
  sem dragão, aura, ficha, chão, texto ou painel.
- hero_p03.png: a mesma operação para o grande lutador preto/vermelho à direita,
  preservando a pose para dentro, cabelo, colete com capuz e camiseta branca; fundo transparente.
- hero_p02/p04/p05/p06/p07/p08.png: de cada ficha original, um único personagem adulto na sua
  pose de ação, da cabeça completa às coxas, em três quartos voltado para dentro; preservar rosto,
  cabelo, roupa, proporções, cores e traço; remover fundo, outras poses, retratos e diagramas;
  nenhum novo lutador, texto, aura ou cenário. Transparência solicitada e verificada.

## Verificação

- Testes verificam os 16 quadriláteros em perspectiva, os espaços entre células, os slots bloqueados,
  a independência das equipes, a TAG, a troca de parceiro, a confirmação e os comandos inferiores.
- Workflow isolado compila o Android e produz APK. Como o workflow existente da seleção,
  não regenera nem valida todos os sprites: relatórios P04 desatualizados já eram um bloqueio na base.
- Smoke Android captura estado inicial, toque em bloqueado, mudança de TAG, titular P02
  e entrada no combate, e verifica ausência de crash.

## Continuação a partir do HEAD 20f8a28526b9a79c55909ea55b692c503503f068

- O renderer decodifica `heroes_p02_p05.png` uma vez, divide em quatro células iguais
  com alpha e associa P02/P03/P04/P05 em ordem de leitura. Não há procura por
  arquivos individuais ausentes para esses IDs.
- P06 e P07: extração/recriação da pose de ação superior direita dos concepts,
  preservando rosto, cabelo, roupa, cores e acessórios; personagem isolado com alpha.
- P08: figura única baseada no concept, cabelo rosa, flor, luvas e chicote bordô,
  pose de luta em três quartos. Não se usa a ficha inteira como figura lateral.
- Artes largas mantêm presença vertical de 630 unidades, centralizadas no lado
  selecionado e recortadas apenas pelo painel central, sem invadir a grade.
- `selection-reference.yml` foi recuperado do blob informado e adaptado.
  `tools/selection/emulator_qa.py` obtém largura/altura da captura Android e
  transforma todas as coordenadas 1280×720 antes de enviar toques. Todas as
  comparações convertem para RGB, incluindo captura com alpha opaco.
- QA cobre oito células bloqueadas, todos os heróis, TAG P1/P2, independência
  de equipes, inversão titular/TAG, confirmação e ausência de crash/arte ausente.
  Capturas e resultados JSON são artefatos do mesmo build do APK.
- Workflow publica APK e PNG como arquivos diretos somente após o QA passar.
  A validação global de sprites permanece fora desta alteração de UI.
