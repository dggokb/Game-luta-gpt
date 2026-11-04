# Como adicionar imagens novas sem pesar o jogo

Toda imagem nova passa pela placa de vídeo do celular, que é o limite do jogo: na luta ela leva
10–12 ms dos 16,6 ms de cada quadro (`docs/desempenho-fps.md`). Uma imagem no formato errado,
grande demais ou carregada na hora errada volta a derrubar o FPS. Este guia diz onde cada tipo
de imagem entra, qual ferramenta prepara e as regras que valem para todas.

## Regras para todas as imagens

1. **Nenhum lado acima de 4096 px.** É o tamanho máximo de textura de muitos celulares. Acima
   disso o jogo reduz a imagem na hora de mandar para a placa: custa CPU no meio do jogo e perde
   nitidez.
   - Hoje dois atlas passam do limite: `p14_special_energy` (5532×2856) e `p13_special_energy`
     (4908×3256).
2. **Opaca vira JPG; com transparência vira PNG ou WebP sem perda.**
   - Céu, piso, fundo de Ultra e página de mangá não têm transparência: JPG.
   - Lutador, camada recortada do cenário, efeito: PNG ou WebP sem perda.
3. **Corte as bordas transparentes.** A placa paga cada pixel da imagem, inclusive o
   transparente.
   - As ferramentas abaixo já cortam.
   - Imagem feita à mão: corte antes de pôr no jogo.
4. **Tamanho perto do que aparece na tela.**
   - A tela do jogo tem 1280×720 de referência e o celular desenha em 1080p.
   - Uma imagem que ocupa a tela inteira não precisa passar de uns 1600–2000 px de largura.
     Maior gasta memória e placa sem ficar mais nítida; menor borra.
5. **Nada é decodificado nem sobe para a placa no meio da luta.**
   - Folhas dos lutadores: carregam na tela de carregamento.
   - Imagens do Ultra: sobem uma por quadro durante a abertura dele, com a luta parada.
   - Imagem nova que não passa por esses caminhos aparece como travada no quadro em que é usada
     pela primeira vez. O relatório TESTE mostra isso como "subiu X MB" nos piores quadros.
6. **Não pinte uma cor sólida embaixo de uma imagem que já cobre a tela.**
   - Uma tela inteira pintada a mais custa ~13% da placa.
   - A cor de fundo do cenário (`corFundo`) só é de graça porque vem antes de tudo e vira a
     limpeza da tela.
7. **Texto não vira imagem.** O jogo desenha os textos sozinho, numa thread separada, e manda
   para a placa no máximo 2 por quadro. Texto pintado dentro de uma imagem só faz sentido como
   arte (onomatopeia de mangá, por exemplo).

## Onde cada tipo de imagem entra

| Tipo | Onde fica | Como preparar | Formato |
| --- | --- | --- | --- |
| Folhas dos lutadores | `android/app/src/main/res/drawable-nodpi/` (geradas) | `python3 tools/sprites/build_characters.py --write` a partir dos clips e de `art/sprites/source/` ([sprite-standard.md](sprite-standard.md)) | WebP qualidade 95 com o alfa exato, gerado. Não editar à mão |
| Ultra do lutador (vídeo) | folha `ULTRA_SEQ` + `character.json` | `tools/sprites/ultras.py` ([ultras/](ultras/)) | WebP sem perda, gerado |
| Abertura e página do Ultra | `assets/ultras/<id>/`: `carga.jpg`, `corte.png`, `final.jpg`, `golpe.jpg`, `olhos.jpg`, `palco.jpg` e `ultra.json` | à mão, nos tamanhos do p02 (900×900, 1600×900, 1600×286); só o `corte` tem transparência | JPG (opacas) e PNG (`corte`) |
| Cenário | `assets/stages/<id>/` + `stage.json` | `python tools/stage/preparar_cenario.py --entrada <pasta> --id <id>` ([tools/stage/README.md](../tools/stage/README.md)). Corta as margens, passa céu e piso para JPG e reduz o que passa de 2048 px | JPG (céu, piso) e PNG (camadas) |
| Seleção (retrato grande e rostos) | `assets/selection/art/tela/hero_<id>.webp` e `assets/selection/rostos/<id>.webp` | `python3 tools/selection/gerar_selecao.py` (`--check` confere) | WebP sem perda, gerado |
| Retrato do HUD | `assets/ui/hud/concepts/<id>.webp`; o id entra na lista de `HudConceptPortraits.source` e o retângulo do rosto em `HudConceptPortraits.face` | à mão | WebP |
| Página de mangá (história) | `assets/historia/epNN/pagN.jpg` + o passo no `episodio.json` | `python3 tools/historia/recortar_pagina.py pagina.png --episodio epNN --nome pagN` ([historia/motor-paginas.md](historia/motor-paginas.md)) | JPG, ~1050×1500 |
| Super em quadros e vitória | `assets/supers/<id>/qNN.webp`, `assets/vitorias/<id>/qNNN.webp` | `tools/sprites/super_video.py`, `tools/sprites/vitoria_video.py` | WebP, cada quadro recortado |
| Efeitos soltos (balão, caixa) | `assets/efeitos/` | à mão, recortado | WebP |

## Pontos de atenção que já existem

- **Dois atlas acima de 4096 px** (regra 1): `p14_special_energy` e `p13_special_energy`.
  Separar em duas folhas (ou dar menos quadros a elas) tira a redução na hora de subir.
- **Retratos do HUD:** `assets/ui/hud/concepts/<id>.webp` é a folha de conceito inteira
  (~1450×1090). O jogo decodifica a folha toda (~6 MB) para recortar só o rosto e solta o resto.
  Gravar só o recorte, como a seleção já faz, deixa o carregamento mais rápido.

## Como conferir uma imagem nova

1. **No PC:** `gradle :pc:medirLuta -Pgl -Ptoque -Parea` (ou `gradle :pc:capturar -Pgl` para a
   seleção).
   - Com `-Parea`, a lista "AREA" no console mostra de onde vem cada desenho grande, em telas
     por quadro. A imagem nova não deve aparecer muito acima do que ela ocupa na tela.
   - Cada imagem que sobe durante o teste sai como "SUBIU".
   - As fotos em `pc/build/medir-luta/` mostram se ela está certa.
2. **No celular:** o botão TESTE (Modo Teste), com a mesma dupla. Compare com o relatório
   anterior:
   - "Placa de vídeo, ms por quadro";
   - "Área pintada";
   - "texturas subidas";
   - os piores quadros: nenhum deve mostrar "subiu" com a imagem nova.
