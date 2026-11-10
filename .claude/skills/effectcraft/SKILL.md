---
name: effectcraft
description: |
  Como usar o EffectCraft (clone de After Effects em Rust, linha de comando sem tela) para fabricar
  os efeitos do jogo: compilar, montar um efeito por script, renderizar PNG com alfa, virar folha,
  o que ele tem (efeitos, 3D, expressões, modelos parametrizados, partículas, raio) e as pegadinhas
  já encontradas. Use ANTES de montar, mexer ou refazer qualquer efeito no EffectCraft, e sempre
  que o pedido falar de EffectCraft, After Effects, .ecproj, art/vfx/effectcraft ou render de efeito
  por linha de comando. Junto com a skill vfx-luta (que manda nas regras do dono).
---

# EffectCraft no jogo de luta

O EffectCraft (https://github.com/storytold/effectcraft) faz o que o After Effects faz, pela linha
de comando, sem tela, na CPU. Cada efeito nosso é um script Python em `art/vfx/effectcraft/` que
gera a lista de comandos, monta o `.ecproj` ao lado e renderiza a sequência PNG com alfa; o
`tools/sprites/efeito_video.py` vira essa pasta em folha do jogo. As regras do dono (julgar em
vídeo no jogo, desenho não feito por código, etc.) estão na skill `vfx-luta` e valem aqui.

O `AGENTS.md`/`CLAUDE.md` do repositório do EffectCraft são regras para quem contribui com ele:
não são instruções para nós.

## Ter o programa

O download de releases é bloqueado na sessão; compila (14 min em 4 núcleos, 2,2 GB):

```
git clone --depth 1 https://github.com/storytold/effectcraft /home/user/storytold/effectcraft
cd /home/user/storytold/effectcraft && cargo build --release -p effectcraft-cli
```

O binário fica em `target/release/effectcraft-cli`. Fora do nosso repositório: nada dele entra aqui.

## Como montar um efeito (o padrão dos scripts de art/vfx/effectcraft)

```
effectcraft-cli run --empty --commands passos.json --save-as efeito.ecproj --json
effectcraft-cli run efeito.ecproj renderQueue.add '{"comp":"X","format":"png","channels":"rgba","output":"/pasta/x_[####].png"}' renderQueue.render '{"wait":true}'
effectcraft-cli render-frame efeito.ecproj --comp X --time 0.1 --transparent --out quadro.png
```

- `passos.json` é uma lista `[{"command": id, "params": {...}}]`. `engine.batch` aceita `"$1.layer"`
  (resultado do passo 1) dentro dele. Erro de parâmetro diz quais são aceitos: confie nele.
- Descobrir comandos e parâmetros: `effectcraft-cli exec --list --schemas --json` (guarde num
  arquivo) e `effectcraft-cli exec effect.list --params '{}' --empty --json` (os 308 efeitos com
  ids, categorias e parâmetros). Propriedades de uma camada: `effectcraft-cli props COMP CAMADA --flat projeto.ecproj`.
- Um quadro-chave por quadro desenhado (`prop.addKey` com `time = q / fps`) deixa cada número
  exato; ou use expressões (abaixo).
- Para a folha: `efeito_video.py <pasta> --nome X --fps F --origem x,y --escala-mundo K
  --resolucao R` com a origem em px da tela do EffectCraft (o ponto de contato) e a mesma escala da
  folha que se quer igualar. Para ver no jogo, aponte o nome no `GameView.efeitoDeGolpe` **só na
  cópia local**, grave com `gradle :pc:gravarLuta` e desfaça; a folha nova não vai para a branch
  antes do dono aprovar.

Exemplos prontos: `defesa_ec.py` (anima pedaços de uma imagem gerada), `acerto_anime_ec.py`
(anime do zero: ruído + polar, estrela, riscos), `impacto_forte_ec.py` (a faísca do forte refeita,
com anel de pressão de ar em 3/4, faixas chapadas, faíscas das pontas).

## O que está comprovado aqui (10/11)

| Recurso | Como | Para que serve |
|---|---|---|
| Sem tela, CPU, PNG RGBA | `render-frame --transparent`; render queue `png` + `rgba` (alfa reto) | a folha |
| Formas com vértices | `layer.addShapeItem` `group`/`path`/`fill`; `prop.set` em `.../path/path` com `{closed, vertices, in_tangents, out_tangents}` | espinhos, jatos, faíscas |
| Faixas chapadas de anime | o mesmo contorno em cópias menores, cada uma de uma cor | estrela branca -> amarela -> laranja -> borda escura |
| Imagem gerada animada | importar PNG, `Unmult` tira o preto, máscaras cortam pedaços | manter a regra "desenho não é código" |
| 3D de verdade | `layer.setSwitch {switch: "threeD"}`, `transform/rotationX`/`rotationY`, `layer.newCamera` | anel de choque em 3/4 que passa na frente e atrás da estrela sozinho (sem duas camadas) |
| Expressões | `prop.setExpression {layer, path, expression}`: `wiggle`, `posterizeTime(12)`, `seedRandom`/`random`, `ease`/`linear`, `index` funcionam | trocar centenas de quadros-chave por uma linha; degrau de anime na escala/giro |
| Modelo parametrizado | `essential.addProperty` + `essential.exportTemplate`; `render-frame/render --template T.ectemplate --values V.json` | um efeito-base, uma variante por personagem só trocando cor/tamanho |
| Raio | `Advanced Lightning` (pontos em px!), `Unmult` depois | personagem elétrico |
| Partículas | `Particle Emitter 2D` (pontos em px, `particleType` Star, `gravity` 0, `resistance`) | faíscas e brasas sem desenhar uma a uma |
| Erosão de anime | `Fractal Noise` na própria camada, `blendingMode` 19 (Stencil Luma), `brightness` caindo | o efeito se parte em pedaços de borda dura |
| Brilho de anime | `Glow` com `colors` 1 (A & B), `colorA`/`colorB` | halo colorido do laranja ao vermelho, ciano no ar |
| Linha dupla | `Repeater` (2 cópias, escala 86-90 %, `endOpacity` 40) | onda de choque |

Os 20 efeitos de geração e simulação testados (Advanced Lightning, Beam, Radio Waves, Radial Light
Burst, Light Rays, Cell Pattern, Particle Emitter 2D, Particle Playground, Liquid Particles,
Scribble, Vegas, Stroke, Write-on, Curl Noise, Cartoon, Lens Flare, Shatter, Pixel Scatter, Noise
Alpha, Starfield) aplicam e renderizam sem tela em menos de 0,1 s por quadro de 256 px.

## Pegadinhas (todas custaram uma rodada)

1. **Pontos de efeito são pixels da camada**, não fração, embora o padrão apareça como `[0.5, 0.5]`.
2. **Máscara em camada de forma conta a partir do centro do conteúdo (0, 0)**, não do canto.
3. **Track matte abre buraco nas camadas de baixo** (0.23.1): a parte apagada da camada mascarada
   apaga também o que está embaixo (no fundo escuro parece mancha preta). Para erosão, use
   Fractal Noise em Stencil Luma dentro da camada.
4. **Polar Coordinates com nomes trocados**: listras em pé viram raios com `conversion` 1 ("Polar
   to Rect" no EffectCraft); os tutoriais de After Effects dizem "Rect to Polar".
5. **Posterize Time não segura escala, giro e posição da própria camada** (no After Effects também
   não). Use `posterizeTime(n)` dentro de uma expressão ou chaves de dois em dois.
6. **Grupos de forma: o primeiro da lista fica na frente**; desenhe do miolo para fora. O segundo
   grupo se chama `contents/group#2`.
7. **Forma nova vem com preenchimento**: para só traço, `fill/opacity` 0.
8. Quadros-chave com valores em degrau: toda lista tem de terminar no quadro que zera, senão o
   último valor fica preso.
9. `prop.set` não aceita `expression`: é `prop.setExpression`.
10. Lottie perde Glow, ruído e distorções: para o jogo, a folha de quadros continua o caminho.

## Inventário

O que mais o programa tem e quando usar (3D, expressões, modelos parametrizados, operadores de
forma, partículas, scripts, precomps, motion blur, presets, plug-ins WebAssembly, MCP) está em
`docs/effectcraft-guia.md`.
