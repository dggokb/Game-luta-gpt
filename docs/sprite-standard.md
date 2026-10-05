# Padrão de personagens e sprites — GPT v0.55

A escala pertence ao **personagem**, nunca a um golpe isolado. As imagens e os
metadados são preparados antes do APK; não há JSON, recorte automático ou
normalização de imagens durante a partida.

## Arquivos que o autor edita

| Arquivo/pasta | Responsabilidade |
| --- | --- |
| `characters/<id>/character.json` | Identidade visual, animações e golpes |
| `characters/roster.json` | Personagem visual dos dois slots do time |
| `tools/sprites/profiles/<id>.json` | Tamanho, raiz, escala e exigências de qualidade |
| `tools/sprites/clips/*.json` | Fonte, segmentação, quantidade de frames e destinos |
| `art/sprites/source/` | Fontes de arte, preservadas sem sobrescrever |

O build descobre todos os `character.json` e todos os clips. Não existe uma lista
manual de personagens no renderer. `GeneratedCharacters.java`,
`GeneratedSpriteLayouts.java`, os atlas em `drawable-nodpi` e os relatórios em
`tools/sprites/reports` são saídas geradas e versionadas. Não editar à mão.

## Fluxo de trabalho

```bash
python3 -m pip install -r tools/sprites/requirements.txt
python3 tools/sprites/build_characters.py --write
python3 -m unittest discover -s tools/sprites/tests -v
python3 tools/sprites/build_characters.py --check
cd android
gradle testDebugUnitTest assembleDebug
```

O `preBuild` do Gradle também executa a geração. Python 3.9+ e a versão de Pillow
fixada no requirements são pré-requisitos do ambiente de desenvolvimento, além de
Java 17, Gradle 8.7 e Android SDK 35. O APK não precisa de Python.

O importador trabalha em uma pasta temporária. Somente depois de validar todos os
pacotes publica as saídas; um erro de configuração não deixa metade dos atlas
atualizada. `--check` compara todas as saídas declaradas, sem modificar os arquivos
versionados. A CI rejeita saídas desatualizadas antes do build. Ao remover um clip,
remova também seu PNG e relatório antigos do repositório.

## Novo personagem

1. Crie um perfil visual em `tools/sprites/profiles/`, usando `player_base.json`
   como referência. Defina as dimensões, a raiz e a escala próprias do lutador.
2. Adicione as fontes e os clips correspondentes, com IDs e saídas únicos.
3. Copie `characters/player_base/character.json` para
   `characters/<novo_id>/character.json`. O campo `id` deve coincidir com a pasta.
   Ajuste nome, perfil, atlas, animações e golpes.
4. Substitua um dos IDs de `characters/roster.json` para testar no time. O primeiro
   slot deve ser igual a `defaultCharacter`. O cadastro é gerado automaticamente.
5. Gere, revise a prévia, execute os testes e confira o APK no Android.

O pacote é uma definição visual e dos ataques normais em pé. Vida, velocidade,
combos, projéteis, Super e IA continuam no `FighterProfile`/`GameView`; criar uma
mecânica de personagem inédita ainda exige programação. O oponente mantém seu
renderer anterior. Não confundir registro visual automático com um editor completo
de todos os sistemas do jogo.

## Estados e animações

Estados semânticos obrigatórios: `IDLE`, `COMBAT`, `WALK_FORWARD`, `WALK_BACK`,
`CROUCH`, `RISE`, `JUMP`, `FALL`, `DASH`, `BACKDASH` e `LAND`.

Cada animação contém `atlas`, `frames` (índices a partir de zero), `loop` e uma das
formas de avanço:

- `durationsMs`: um tempo positivo por frame;
- `distancePerFrame`: distância percorrida para avançar, com `loop: true`.

Caminhada acompanha deslocamento físico, inclusive ao virar para o outro lado.
Colisão com a borda não deve produzir passos no lugar. Ataques usam o relógio de
combate; reiniciar o mesmo golpe reinicia o frame, sem herdar a recuperação anterior.
Os IDs de animação de ataque são livres: não é necessário alterar um enum Java.

## Novo golpe usando um comando existente

Adicione o atlas/clip e uma animação ao manifesto. Vincule-a a L, M ou H em `moves`.
Exemplo de animação de quatro frames e vínculo L:

```json
"CUSTOM_PUNCH": {
  "atlas": "novo_personagem_soco",
  "frames": [0, 1, 2, 3],
  "durationsMs": [40, 30, 30, 60],
  "loop": false
}
```

```json
"L": {
  "animation": "CUSTOM_PUNCH",
  "damage": 300,
  "activeStartMs": 40,
  "activeEndMs": 100,
  "reach": 118
}
```

O tempo total vem da soma dos frames. Dano só é permitido em
`activeStartMs <= tempo < activeEndMs`, uma vez por execução. Startup e recovery
não acertam. O autor define dano, alcance e janela ativa; o importador valida,
mas não tenta adivinhar balanceamento pela arte.

A v0.53 conecta L/M/H em pé. Golpes agachados, aéreos, projéteis e Super continuam
com o comportamento anterior. Ainda não há comandos extras declarativos, cancel
windows ou hitboxes/hurtboxes arbitrárias por frame. `reach` usa a regra de
colisão já existente, em unidades do mundo; não é a largura do PNG.

## Geometria, qualidade e raízes

Personagem base: célula 256×256, raiz preferida X=128/Y=238, worldScale=1.0,
referência em pé de 228 px, fundo transparente e margem mínima de 8 px.
Golpes largos recebem mais espaço transparente, sem encolher o corpo. O chute
médio usa 384×256. O forte usa 320×256 e, após a validação anatômica da v0.54,
raiz local X=128/Y=238. Essas diferenças são metadados de canvas, sem correção
de escala por golpe em runtime.

### Escala anatômica canônica

Para fontes novas que contenham ao menos uma pose comparável à guarda canônica,
use `scaleMode: canonical-anatomy` e `anatomyReferenceFrame`. O perfil aponta para
um mestre revisado do personagem e define bandas normalizadas de silhueta. O
importador mede altura e largura nessas bandas, calcula candidatos de escala e usa
a mediana. Se a dispersão ultrapassar `maxScaleSpreadRatio`, o frame escolhido não
é confiável e o import é rejeitado.

Isso impede a regressão em que `worldScale=1.0` era mantido, mas um golpe desenhado
maior aparecia maior no jogo. O Heavy de 9 frames é o teste de regressão oficial:
o quadro 0 precisa permanecer visualmente compatível com o Idle. Clips sem uma
pose comparável devem usar um mestre `prepared-grid` revisado em vez de forçar a
detecção automática.

Três modos de importação estão disponíveis:

- `horizontal-alpha-components`: separação horizontal por transparência;
- `grid-alpha-components`: projeções X/Y e ordem por linha;
- `prepared-grid`: mestre previamente normalizado, com colunas explícitas e raiz
  `authored`; valida dimensões, quantidade, pixels visíveis e margens, e copia sem
  reamostrar ou reposicionar poses aéreas.

Médio e forte são normalizados das fontes de alta resolução com `ground-feet`.
Idle, movimentos e jab preservam os mestres normalizados revisados da v0.51. Seus
originais não normalizados não foram reconstruídos. Todos os cinco agora têm
configuração, relatório e validação no build. O idle mantém 1024×512, oito células
256×256; não retorna ao atlas legado de baixa resolução.

Os relatórios registram layout, validações e hashes SHA-256 de fonte/config/perfil.
Dados duplicados de perfil foram removidos do Java manual. Atlas do time são
carregados antes da partida; a troca de personagem não decodifica os mesmos bitmaps
novamente.

## Prévia e validação

Abra `android/app/build/sprite-review/index.html` após importar. A página funciona
offline, incorpora as imagens e permite selecionar personagem/animação, reproduzir,
pausar, avançar frame, espelhar e ajustar zoom. Exibe raiz, chão e indicação da
janela ativa. Caminhada na prévia usa tempo simulado; no jogo usa distância real.

O build rejeita, entre outros: ID duplicado, saída duplicada, fonte/atlas ausente,
frame inexistente, estado obrigatório ausente, duração inválida, janela ativa fora
da animação, perfil incompatível e roster inválido. Testes incluem cadastrar um
segundo personagem e um golpe com ID novo, regeneração determinística, saídas
obsoletas e falhas sem publicação parcial.

Testes Android exercitam comandos, liberação do direcional, salto/pouso, orientação,
pausa, escala, recortes e renderização de animação personalizada. Os testes de
combate verificam startup sem dano, janela ativa com dano único e recovery sem
acerto tardio, nos dois sentidos. A CI instala o APK no emulador e testa abertura e
retorno do segundo plano.

## Limites que ainda exigem revisão

A segmentação alpha usa projeções, não reconhecimento de poses. A grade ainda usa
intervalos X globais: linhas desalinhadas, efeitos separados, sombras ou poses que
se encostam podem gerar agrupamento incorreto. Uma contagem correta não garante
anatomia ou agrupamento corretos.

`ground-feet` estima o apoio pela faixa inferior de pixels e usa o centro do
bounding box se não detectar pés. Não usar esse modo para saltos, tecido/efeito
abaixo dos pés ou deslocamento intencional da raiz. Nesses casos, usar mestre
normalizado com raiz authored e revisar a imagem. Referências de escala devem ser
poses comparáveis em pé; não normalizar um agachamento à altura de um lutador em pé.

Margens são avaliadas acima do alpha 10: pixels mais fracos exigem inspeção visual.
`maxUpscale` impede ampliação proibida, mas não detecta blur já presente. Hitboxes
não devem ser inferidas automaticamente de pixels opacos.

Abertura no emulador não comprova qualidade final ou latência no celular. Movimento,
multitoque e fluidez ainda devem ser testados no aparelho. Física a 120 Hz não
promete entrada em 8,3 ms; consumo da fila e desenho compartilham o loop de 60 FPS.


## Atualização GPT v0.54

A branch GPT incorporou o Character Pack Engine da Astra v0.53 e adicionou a
validação `canonical-anatomy`. O Heavy atual produz 320×256, root 128/238 e
`worldScale=1.0`; a escala da fonte é calculada pela assinatura anatômica, não pela
mediana dos nove bounding boxes. O relatório grava assinatura canônica, assinatura
da fonte, candidatos de escala e dispersão.

O teste Python rejeita um frame de referência incompatível. O teste Android mede
o tamanho visual do início do Heavy contra o Idle e falha se o golpe voltar a
crescer.


## Validação com segundo personagem — GPT v0.55

O roster de produção contém `player_base` e `player_two`. Neste teste, `player_two` compartilha intencionalmente perfil, atlas, estados e L/M/H com o personagem base para testar somente cadastro, geração, preload, troca em runtime e seleção declarativa dos golpes.

O HUD mostra o `displayName` do pack ativo. O teste Android executa a animação completa de troca, confirma que o renderer passou de `player_base` para `player_two` e executa L, M e H pelo manifesto do segundo personagem. O teste Python confirma o roster versionado e a equivalência de movimentos. Workflow 37293247005: PASS completo.
