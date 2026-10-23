# Animação por vídeo no Flick com MiniMax H3 — padrão

Decidido em 22/10/2026, testado no p15 (H aprovado) e num lutador de teste (dash, jab, chutes).
Substitui o Omni Flash 1.1 do Flow ([omni-flash.md](omni-flash.md)) para os vídeos novos. As
regras de [video.md](video.md) continuam valendo (verde, sem efeito, sem adversário, conversão
com `personagem.py`), e a fórmula de prompt e as faixas de tempo do Omni também.

## Por que o Flick

Levantamento de preços de 21–22/10/2026 (páginas dos próprios fornecedores):

| Onde | Modelo | Clipe de 5 s em 768p |
|---|---|---|
| Flick Pro (US$ 45/mês, 8.000 créditos) | H3 Fast, 20 créditos | ~US$ 0,11 |
| Flick Pro | H3 Max, 40 créditos | ~US$ 0,225 |
| Picsart API | H3 Max | US$ 0,24 |
| MiniMax direto (API) | H3 Max | US$ 0,40 |
| API mais barata do Seedance 2.5 (4 s, 720p) | Seedance 2.5 | ~US$ 0,74–0,92 |

- O Flick tem tela com **quadro de início e de fim** (como o Flow), devolve os créditos de
  geração que falha e baixa sem marca d'água a partir do Standard. O plano grátis dá 100
  créditos por semana (expiram na segunda) e põe "MADE IN FLICK.ART" no canto de cima à
  esquerda.
- O Seedance 2.5 também está no plano, mas caro (192 créditos por 4 s em 720p): usar só em golpe
  que o H3 não acerte.
- Créditos do plano não acumulam: assinar nos meses de produção e voltar ao grátis nos outros.
- API (Picsart, fal) só compensa para gerar em lote por script.

## Configuração

- Modelo **MiniMax H3 Fast** (20 créditos) por padrão; **H3 Max** (40) quando o Fast errar.
- **5 s** (o mínimo do H3), **768p**. O vídeo sai em **1344×768, 24 fps, ~5,17 s** (124 quadros).
- **A mesma imagem nos campos de início e de fim** para golpe no lugar. Sem o quadro final, a
  guarda volta mais alta e de frente que a inicial (bateu 67% com a do começo no jab de teste);
  com ele, o último quadro bate 98–99% com o primeiro.
- O Flick reescreve o prompt ("Prompt Expansion", Balanced ou Quality, sem opção de desligar).
  Nos testes as faixas de tempo foram respeitadas mesmo assim.

## Imagens iniciais: tamanho do H3

O H3 desenha o personagem do tamanho que ele está na imagem. Com a escala antiga (60% da altura)
o rosto do p15 se perdia; a 77% o rosto fica legível e ainda cabe o chute alto (pé até x ≈ 1165
de 1344). Monte as imagens da **Base** do concept, não de um quadro de vídeo ampliado (fica
mole):

```
python3 tools/sprites/inicio_da_base.py p15 base.png --sufixo _h3 --bolso 0,0,300,620 --bolso 0,0,440,270 --previa pasta
python3 tools/sprites/inicio_da_base.py p15 base.png --sufixo _h3 --bolso 0,0,300,620 --bolso 0,0,440,270
```

Personagem novo: sem `--sufixo` (são as `inicio_*.png` dele). Personagem que já tem vídeos
convertidos na escala antiga (o p15): `--sufixo _h3`, e no `tamanho.json` cada chave nova recebe
a escala antiga × altura da guarda antiga / altura nova (p15: 0,5679 × 427 / 591 = 0,4103). A
conversão mede cada vídeo contra todas as chaves e usa a que bate, então vídeos velhos e novos
saem no mesmo tamanho.

- Tela 1344×768 (o tamanho que o Flick devolve: sem esticar nem cortar), verde puro, corpo com
  77% da altura, pés em y 733; esquerda e direita a ±273 px do centro (a proporção do p01).
- A Base pode ter fundo cinza liso: sai por inundação a partir da borda, com a sombra dos pés.
  Fundo preso entre mechas de cabelo precisa de `--bolso` (retângulo, em px da Base). Conferir a
  prévia sobre o verde antes de gerar vídeo.
- Sombra do chão rabiscada e puxada para o quente (p16) prende o fundo entre as pernas:
  `--sombra-sat 26 --sombra-y 0.85` (padrão 12 e 0,88). A cor de pele, meia creme e tênis
  ficam: só sai o que é quase cinza.
- **Personagem mais baixo ou mais alto que o p15:** `--altura` proporcional, medido pelo rosto
  (orelha, distância orelha–olho), não pela silhueta (guarda aberta e cabelo volumoso enganam), e
  a mesma escala 0,4103 no `tamanho.json`. p16: `--altura 0.70`.

```
python3 tools/sprites/inicio_da_base.py p16 base.webp --altura 0.70 --sombra-sat 26 --sombra-y 0.85 \
    --bolso 150,0,640,150 --bolso 150,150,470,345 --bolso 470,130,700,215 --bolso 615,215,700,335 \
    --desfranja 2 --halo 4 --sem-halo 435,75,515,150 --sem-halo 250,240,380,345
```

- Cabelo de mechas finas e soltas: o fundo fica preso entre as mechas em volta de toda a cabeça,
  não só no rabo de cavalo. Cobrir a cabeça inteira com `--bolso`, deixando de fora só olhos e
  dentes (brancos de verdade). `--desfranja 2` tira a franja clara da borda das mechas.
  O contorno rosado claro que o desenho tem nas mechas não é a cor do fundo e sobra como ponto
  branco no verde: `--halo 4` tira, dentro dos bolsos, o pixel claro e pouco saturado colado no
  fundo; `--sem-halo` protege o que é claro de verdade ali dentro (pompom, dedos).
- Conferir a prévia ampliada na cabeça (zoom 3–4×): em tamanho normal o branco entre as mechas
  passa despercebido.
- `inicio_agachado_h3` sai de um quadro do vídeo de agachar feito com a guarda nova. Conferir
  com o `regua.py` que os clipes velhos e novos saem no mesmo tamanho (aviso fora de ±3%).

## Prompt

Prompts prontos para p01–p12, refeitos dos que deram certo na p15: [flick-h3-elenco.md](flick-h3-elenco.md).

A fórmula do [omni-flash.md](omni-flash.md#fórmula-do-prompt), com a última faixa indo até 5 s
e a lista de limpeza com faíscas: no jab de teste saiu um clarão de impacto na ponta do punho e
na freada do dash uma nuvem de poeira.

```
... Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes,
sparks or effects. No opponent. No music, no dialogue.
```

**Andar: prompt curto.** O andar detalhado do Omni (shuffle contado, "light springy bounce")
saiu pulando; "walks forward ... in her fighting stance" saiu ereta e andando como gente comum.
O que funcionou foi uma frase, em português: "Andando para frente como personagem de jogo de luta
mantendo sua guarda" (quadros `inicio_esquerda_h3` → `inicio_centro_h3`).

O que o H3 acertou e o Omni não:

- **Membro certo pelo texto.** O chute alto com a perna de trás ("his rear leg (the leg farther
  back, on the LEFT side of the frame)") saiu certo nas duas vezes, no p15; no Omni errou em vídeo
  e em imagem. O jab saiu com o braço da frente sem imagem de pose.
- **Uma ação só** quando o prompt conta ("ONE ...", "One kick only"). Sem contar, um pedido de
  chutes aéreos saiu como uma rajada de seis chutes.
- Corrida (dash) sai **no lugar**, com início e fim na guarda: o jogo move o personagem.

Cuidados que ficam:

- Na corrida o tronco alterna frente e costas entre os quadros: escolher o ciclo com o mesmo lado.
- Borrão de movimento no giro do chute (calça) é sólido e serve de smear.
- Preto com reflexo verde (ponta da faixa pendurada, luva, cabelo) tem matiz de verde mas é
  escuro: o recorte só chama de fundo o verde claro da tela (`SCREEN_MIN_V` em
  `video_para_sprite.py`). Antes a ponta da faixa da p15 sumia em parte dos quadros.
- Marca d'água no canto (plano grátis) fica longe do personagem, mas não é verde: conferir na
  conversão que ela não entra no recorte, ou gerar os vídeos finais no plano pago.
