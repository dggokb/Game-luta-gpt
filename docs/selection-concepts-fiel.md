# Seleção fiel ao conceito — 12/10/2026

Base: game-luta-sprite-gpt em 32a5e9e23513ddcde57f4cc75f26d515b3a27e57. Branch de teste: ui/selecao-concepts-fiel-20261012.

## Correção

A versão anterior usava fichas inteiras de personagens nos retratos e nas figuras laterais.
Agora o renderizador extrai somente os closes corretos das fichas p01…p08. As figuras laterais
usam oito artes individuais com transparência, derivadas dos mesmos conceitos.
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

- Prévia local executa CharacterSelectRenderer.java e CharacterSelect.java reais por adaptador Java2D.
  Isso verifica a composição e a geometria; fontes e filtragem podem diferir discretamente do Android.
- Testes verificam os 16 quadriláteros em perspectiva, os espaços entre células, os slots bloqueados,
  a independência das equipes, a TAG, a troca de parceiro, a confirmação e os comandos inferiores.
- Workflow isolado compila o Android e produz APK. Como o workflow existente da seleção,
  não regenera nem valida todos os sprites: relatórios P04 desatualizados já eram um bloqueio na base.
- Smoke Android captura estado inicial, toque em bloqueado, mudança de TAG, titular P02
  e entrada no combate, e verifica ausência de crash.
