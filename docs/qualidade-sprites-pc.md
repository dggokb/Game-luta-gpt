# Protecao de qualidade do Motor Sprite V2 para PC

O motor examina o detalhamento dos PNGs reais, em resolucao nativa, evitando que o contorno do chroma key crie falso resultado. Compara cada animacao com o idle do proprio personagem.

Comandos:
    python tools/sprites/quality_guard.py --character player_base
    python tools/sprites/quality_guard.py --character player_base --enhance

O segundo comando cria atlas opcionais para PC na pasta ignorada android/app/build/quality-pc/player_base/optional-pc-atlases. So altera o RGB interior opaco (alpha, canvas e posicoes dos frames originais intactos), aceitando apenas melhora limitada sem clipping exagerado. Nao muda sprites ja usados por Android nem aplica automaticamente no port separado.

O relatorio QUALITY.json informa nitidez relativa, estados suspeitos e risco de ampliacao em 1080p, 1440p e 4K. Filtros tipo nearest-neighbor deixam contornos mais duros mas podem pixelar; bilinear suaviza mais. Se a arte de origem nao contiver detalhe suficiente, exige-se video original com mais definicao; upscaling nao recria informacao real.

O port real game-luta-sprite-pc-gpt mora em outra branch. Levar o atlas opcional e a selecao de filtro para esse port requer incorporacao e testes de qualidade visual.

Integracao continua: cada alteracao de character.json, atlas PNG ou receita executa automaticamente quality_guard.py --all --strict no workflow Sprite Quality PC P01. O teste apenas bloqueia assets estruturalmente invalidos; anomalias esteticas geram alertas. O aprimoramento PC continua em arquivos opcionais no build, nunca substitui fonte artistica sem comparacao.
