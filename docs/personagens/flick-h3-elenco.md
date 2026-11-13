# Prompts do Flick (MiniMax H3 Fast) para o elenco

Os prompts que deram certo na p15 no Flick ([p15.md](p15.md), [flick-h3.md](flick-h3.md)),
refeitos para p01–p14 e p16 (a p15 já tem os dela). Cada um troca só o que muda de personagem: quem é, pronome, a lista do
que não pode mudar e o golpe H e o 2M da ficha.

| Clipe | Na p15 | Aqui |
|---|---|---|
| andar para frente | uma frase em português, de primeira | a mesma frase |
| andar para trás | uma frase em português | a mesma frase |
| agachar (dá também o levantar) | prompt do Omni com a última faixa até 5 s, de primeira | o mesmo; "fists up" virou "arms kept in his/her guard", porque nem todos guardam com os punhos |
| H | chute alto com a perna de trás, nomeada pelo lado do quadro | o mesmo molde, com o H de cada ficha (chute, gancho, chicote, martelo, pisão, garras) |
| 2M | rasteira girando com a mão no chão, agachado nas duas pontas | girando onde a ficha pede giro; rasteira baixa sem giro nos outros (p04, p07, p09, p11, p12) |
| idle | aprovado, mas o texto não ficou registrado | pela fórmula: dois ciclos de respiração, mesma imagem nas duas pontas |

## Regra do dono (12/11): sempre dois idles

Todo personagem tem **duas animações de idle**: o idle (a guarda respirando, em loop) e o segundo
idle (um gesto que toca uma vez a cada 3 voltas do idle e volta à guarda: o Ryūga cruza os braços,
a Yin-lu dá pulinhos e ajeita as luvas). O motor já toca o `IDLE_2` de quem tem
(`SpriteMotion.IDLE_LOOPS_BEFORE_IDLE_2`); pedir os dois vídeos junto com os outros.

## Regra do dono (13/11): todo golpe emenda no idle

O último quadro de todo golpe de chão cai **no lugar do primeiro quadro do idle** (o agachado
parado, nos golpes agachados): sem pulo de imagem na troca. O vídeo começa e termina na guarda,
mas a montagem prende o corpo pelo começo (a corrida inclinada, um passo) e a guarda do fim pode
sair fora do lugar (a Ombrada do Ryūga terminava 61 px à frente do idle).

- **Na montagem:** depois de converter (e da nitidez HD), rode
  `python3 tools/sprites/emendar_idle.py <id> --write`: na volta para a guarda o desenho anda aos
  poucos até o lugar do idle (só na horizontal, os pés no chão). Grava `emendar_idle` na receita
  do vídeo, e a reconversão repete. Depois: caixas por quadro, `normal_map.py --refazer` das
  folhas mexidas e `build_characters.py --write`.
- **Conferência:** `tools/sprites/tests/test_emenda_idle.py` (roda no build do APK) falha se um golpe
  terminar a mais de 6 px do idle, com os pés a mais de 2 px ou noutra pose. Os antigos que ainda estão fora ficam em
  `tools/sprites/emenda_idle_excecoes.json` até serem corrigidos (62 em 13/11, todos da arte antiga).
- **Pés no chão:** a emenda também acerta a altura dos pés (o Carrinho da Yin-lu terminava 4 px
  afundado e subia na troca); `emendar_idle.py <id> --assentar CROUCH CROUCH_IDLE RISE --write`
  põe no chão as posturas que nunca saem dele (a Yin-lu agachada flutuava 7 px).
- **A mesma pose:** o último quadro do golpe tem de ser **a guarda do idle**, não só estar no lugar
  dela (a conferência pede 80% das silhuetas batendo). Duas coisas garantem isso:
  1. **o idle começa na imagem inicial** (a guarda da `inicio_centro_h3`): o loop do idle sai de
     um trecho do vídeo que começa e termina nela. O da Yin-lu saía do meio da respiração (58-94) e
     nenhum golpe batia com ele; agora é 40-74 (o 40 bate 97% com a imagem inicial);
  2. **na montagem, o último quadro escolhido é o do vídeo que mais parece o idle**: os vídeos H3
     voltam à imagem inicial no fim, mas a montagem costumava parar antes (o → + M da Yin-lu parava
     no quadro 93, e a guarda estava no 123). Troque os últimos quadros, mantendo a quantidade e o
     tempo feito à mão.
- **Pose diferente que sobra** (vídeo que não volta à guarda) aparece como "POSE DIFERENTE": escolher
  quadros ou refazer o vídeo.

## Configuração

- **MiniMax H3 Fast** (20 créditos); **H3 Max** (40) só se o Fast errar o golpe.
- **5 s, 768p.** Sai 1344×768, 24 fps, ~124 quadros.
- **A mesma imagem nos dois campos** (início e fim) em tudo que é no lugar. Andar: as duas da
  tabela.
- Um vídeo por vez, na ordem da tabela de cada personagem: o 2M precisa do
  `inicio_agachado_h3`, que sai do vídeo de agachar.

## Antes: imagens iniciais `_h3`

Ninguém além da p15 e da p16 tem ainda as imagens no tamanho do H3 (corpo com 77% da altura de
1344×768). Montar da Base (`art/keys/pNN/idle_base.png`) com o `inicio_da_base.py`, conferindo
a prévia sobre o verde (cabelo, vãos entre braço e corpo):

```
python3 tools/sprites/inicio_da_base.py pNN art/keys/pNN/idle_base.png --sufixo _h3 --previa pasta
python3 tools/sprites/inicio_da_base.py pNN art/keys/pNN/idle_base.png --sufixo _h3
```

- `--sufixo _h3` porque todos já têm vídeos na escala antiga (`inicio_*.png`).
- **`--altura`**: o padrão 0,77 (o da p15) serve para todos. Como cada chave nova tem a
  própria escala no `tamanho.json`, o tamanho no jogo não muda, e maior no vídeo é mais detalhe.
  A exceção é o H do p08: o chicote esticado passa da borda a 0,77. Se o vídeo cortar a ponta,
  fazer uma imagem central menor só para o H (com a escala dela no `tamanho.json`).
- **`tamanho.json`**: cada chave nova recebe a escala antiga × altura da guarda antiga / altura
  nova, como na p15. Detalhes em [flick-h3.md](flick-h3.md#imagens-iniciais-tamanho-do-h3).
- Cabelo de mechas (p02, p05, p06, p08, p10): `--bolso` na cabeça, `--desfranja`, `--halo`.

## Regras que vêm da p15

- O membro do golpe vai nomeado **pelo lado do quadro** ("the leg farther back, on the LEFT
  side of the frame"). Foi o que fez o H3 acertar a perna de trás de primeira.
- **Uma ação contada**: "ONE ...", "One kick only". Sem contar, sai rajada.
- **Sem nome de golpe de arte marcial** no texto. "Mawashi-geri", "gyaku-zuki" e "Shotokan"
  puxaram combinação e postura errada.
- **Andar só com a frase curta.** O andar detalhado saiu pulando no H3.
- Conferir na conversão:
  - pé ou chicote encostando na borda (o 2M da p15 desceu até y 767);
  - o último quadro batendo com o primeiro.

## Onde estão os prompts

Na ficha de cada personagem, na seção **"Vídeos no Flick (MiniMax H3 Fast) — prompts atuais"**,
logo antes dos prompts antigos do Seedance:
[p01](p01.md) · [p02](p02.md) · [p03](p03.md) · [p04](p04.md) · [p05](p05.md) · [p06](p06.md) ·
[p07](p07.md) · [p08](p08.md) · [p09](p09.md) · [p10](p10.md) · [p11](p11.md) · [p12](p12.md) ·
[p13](p13.md) · [p14](p14.md) · [p16](p16.md). Os da p15 estão em [p15.md](p15.md).

## p13 a p16

- **p15:** já tem os seis clipes no Flick (idle, andar para frente e para trás, agachar, H,
  2M), aprovados e no jogo. Os prompts dela estão em [p15.md](p15.md).
- **p16:** já tem as imagens iniciais no tamanho do H3. São as `inicio_*.png` sem sufixo,
  porque ela não tem vídeo antigo. O corpo ocupa 70% da altura, para ela ficar um pouco mais
  baixa que a p15.
  - O vídeo de agachar dá a `inicio_agachado.png` dela, para o 2M.
  - O `tamanho.json` usa escala 0,4103 e altura 221 e entra junto com o pacote.
- **p13 e p14:** os golpes são os **propostos** nas fichas, ainda sem aprovação.
  - Os concepts já estão em `art/concepts/`. O p13 e o p14 já têm as imagens iniciais (a do p14
    de 26/10, `art/keys/p14/`, altura 0,72).
  - Depois da Base, montar as imagens com `inicio_da_base.py` sem `--sufixo`, como na p16.
  - `--altura`: p13 é enorme (perto de 0,77, o máximo que cabe o chute) e p14 é mais baixo.
  - Medir pelo rosto contra a p15.
