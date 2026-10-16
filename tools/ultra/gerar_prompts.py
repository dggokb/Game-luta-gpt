#!/usr/bin/env python3
"""Gera docs/ultras/prompts.md: os prompts prontos para colar no ChatGPT, por personagem.

Os dados ficam em tools/ultra/personagens.json (descrição, energia, pose, palco e as cenas
de cada Ultra). Mudou alguma coisa lá? Rode de novo:

    python3 tools/ultra/gerar_prompts.py
"""
import json
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
DADOS = Path(__file__).resolve().parent / "personagens.json"
SAIDA = RAIZ / "docs" / "ultras" / "prompts.md"

# A frase do estilo que amarra o personagem ao concept; cenário e vítima não a usam.
AMARRA = "O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. "

# Os painéis de olhos, golpe e atingido viram faixas bem baixas e largas na página: só o meio da
# altura aparece. O resto também perde um pouco das bordas (o jogo faz zoom e corta no formato).
FAIXA = ("IMPORTANTE: na página esta imagem vira uma faixa baixa e larga e só a faixa do meio da altura "
         "aparece (mais ou menos do 30% ao 70% da altura). Tudo o que importa (rosto, mãos, golpe, impacto) "
         "fica dentro dessa faixa do meio; em cima e embaixo, só fundo, energia e linhas de velocidade, "
         "nada importante encostado na borda.")
CENTRO = ("Deixe uma margem livre em volta: nada importante (rosto, mãos, pés, golpe) encostado nas bordas, "
          "porque a imagem é cortada um pouco no jogo.")

INTRO = """# Ultras cinematográficos: imagens para gerar

Gerado por `tools/ultra/gerar_prompts.py` a partir de `tools/ultra/personagens.json` (mude lá e
rode de novo). O nome de cada Ultra fica no `ultra.json` do personagem
(`android/app/src/main/assets/ultras/<id>/ultra.json`, campo `"nome"`): troque à vontade.

## Como usar

1. **Uma conversa por personagem** no ChatGPT. Anexe o concept dele (`art/concepts/<concept>.png`,
   também no Drive) logo na primeira mensagem e diga: *"Este é o personagem. Vou pedir várias
   imagens dele, sempre com esta mesma aparência."*
2. Gere **primeiro o `corte`**. Se ficar bom, anexe o corte junto com o concept nos pedidos seguintes:
   ajuda o personagem a sair igual em todas.
3. Cole cada prompt como está. O formato vem no começo de cada item (retrato, quadrado ou
   paisagem): se o ChatGPT perguntar, escolha esse. Se vier em outro tamanho, tudo bem: na
   hora de colocar no jogo eu escolho o recorte de cada uma.
4. **Olhos, golpe e atingido** viram faixas baixas e largas na página: só o meio da altura
   aparece. Os prompts já pedem isso; se o rosto, as mãos ou o impacto saírem perto da borda de
   cima ou de baixo, peça *"refaça com tudo o que importa na faixa do meio da altura, com só fundo
   em cima e embaixo"*.
5. Salve com o **nome exato** de cada item (`corte.png`, `palco.png`, `olhos.png`, `carga.png`,
   `golpe.png`, `atingido.png`, `final.png`) numa pasta com o id do personagem, por exemplo
   `game-luta-gpt/ultras/p02/` no Drive, e me avise. Eu recorto e coloco no jogo
   (`tools/ultra/preparar_ultra.py`).
6. O **corte** precisa de fundo transparente. Se vier com fundo, peça *"refaça com fundo
   transparente (PNG)"*; se ainda assim vier com fundo branco liso, tudo bem: a ferramenta remove.

Não precisa gerar tudo de uma vez: cada imagem que chega substitui a arte provisória daquele
pedaço, e o resto continua funcionando.

## Onde cada imagem aparece

| Imagem | Onde | Formato |
|---|---|---|
| `corte` | Abertura: o personagem atravessa a tela com o nome do Ultra | retrato 2:3, fundo transparente |
| `palco` | Abertura: o fundo do Ultra | paisagem 3:2 |
| `olhos` | Página de mangá: faixa de cima com o nome | paisagem 3:2 (só o meio da altura aparece) |
| `carga` | Página de mangá: o personagem carregando | quadrado |
| `golpe` | Página de mangá: o golpe vindo na câmera | paisagem 3:2 (só o meio da altura aparece) |
| `atingido` | Página de mangá: o adversário sendo atingido | paisagem 3:2 (só o meio da altura aparece) |
| `final` | Página de mangá: o golpe final, tela cheia (é onde entra o toque no tempo certo) | paisagem 16:9 |
"""


def prompts(p, estilo):
    estilo_cena = estilo.replace(AMARRA, "")
    d, en = p["descricao"], p["energia"]
    itens = [
        ("corte", "retrato 2:3, fundo transparente",
         f"{estilo} Personagem: {d}. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: "
         f"{p['pose']}. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia "
         f"do personagem ({en}). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão."),
        ("palco", "paisagem 3:2",
         f"{estilo_cena} Cenário de fundo, SEM nenhum personagem e sem pessoas: {p['palco']}. Composição "
         f"centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra ({en}). "
         f"Formato horizontal. {CENTRO}"),
    ]
    if p.get("so_abertura"):
        return itens
    itens += [
        ("olhos", "paisagem 3:2",
         f"{estilo} Close extremo nos olhos de {d}, olhar furioso e determinado. Os dois olhos ocupam uma faixa "
         f"horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do "
         f"personagem ({en}). Fundo escuro. {FAIXA}"),
        ("carga", "quadrado",
         f"{estilo} Personagem: {d}. Cena: {p['carga']}. Corpo inteiro, de frente para a câmera, câmera de baixo "
         f"para cima. {CENTRO}"),
        ("golpe", "paisagem 3:2",
         f"{estilo} Personagem: {d}. Cena: {p['golpe']}. Composição horizontal com o golpe no centro, linhas de "
         f"velocidade radiais. {FAIXA}"),
        ("atingido", "paisagem 3:2",
         f"{estilo_cena} Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido "
         f"por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, "
         f"a energia do golpe ({en}) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade "
         "horizontais. "
         f"Composição horizontal com o corpo no centro. {FAIXA}"),
        ("final", "paisagem 16:9",
         f"{estilo} Personagem: {d}. Cena: {p['final']}. Cenário: {p['palco']}. Câmera dramática de cinema, a "
         f"imagem mais épica do Ultra. {CENTRO}"),
    ]
    return itens


def main():
    dados = json.loads(DADOS.read_text(encoding="utf-8"))
    estilo = dados["estilo"]
    partes = [INTRO]
    for p in dados["personagens"]:
        partes.append(f"\n## {p['id']} — {p['ultra']}\n")
        partes.append(f"Concept para anexar: `art/concepts/{p['concept']}.png`. "
                      f"Energia: {p['energia']}.\n")
        if p.get("so_abertura"):
            partes.append("A página de mangá deste Ultra já tem as 5 imagens; faltam só as da abertura.\n")
        for nome, formato, texto in prompts(p, estilo):
            partes.append(f"\n**`{nome}.png`** ({formato})\n\n```\n{texto}\n```\n")
    SAIDA.parent.mkdir(parents=True, exist_ok=True)
    SAIDA.write_text("".join(partes), encoding="utf-8")
    print(f"escrito: {SAIDA}")


if __name__ == "__main__":
    main()
