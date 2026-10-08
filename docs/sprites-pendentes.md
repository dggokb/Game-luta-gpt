# Sprites e efeitos que faltam

Lista do que ainda usa pose emprestada ou desenho provisório em qualquer personagem.

**p01 e p03 (definitivos):** são feitos em vídeo no Seedance e convertidos por
`tools/sprites/video_para_sprite.py`; o que falta de cada um, com o prompt de cada vídeo,
fica na ficha do personagem: [personagens/p01.md](personagens/p01.md) e
[personagens/p03.md](personagens/p03.md). As folhas abaixo são o caminho antigo (ChatGPT),
que ainda serve para os outros pacotes. Folhas desenhadas em grade apertada passam antes
por `tools/sprites/separar_folha.py`.

## Regras para todas as folhas de personagem

- **Anexe no ChatGPT uma folha já aprovada do mesmo personagem** (ex.:
  `art/sprites/source/player_two_heavy_straight_source.png`), para sair o mesmo traço,
  roupa e tamanho.
- Virado para a **direita**, visto de lado, **fundo transparente**, sem chão, sem sombra,
  sem texto, sem marca d'água, sem efeitos de energia (o jogo desenha os efeitos).
- Grade com espaço vazio entre os quadros e **os pés na mesma linha** em cada quadro.

Começo comum dos prompts (cole antes de cada um):

```
Folha de sprites para jogo de luta 2D, no MESMO estilo, personagem, proporção e tamanho da
imagem de referência anexada. Virado para a DIREITA, visto de lado. Quadros em grade com
espaço vazio entre eles, personagem do mesmo tamanho em todos, pés apoiados na mesma linha
do chão. Sem efeitos de energia, sem chão, sem sombra, sem texto, sem marca d'água. PNG com
fundo transparente.
```

## Prioridade alta (mecânicas novas sem pose própria)

| # | Folha | Hoje usa | Quadros | Prompt (depois do começo comum) |
|---|---|---|---|---|
| 2 | **Sendo agarrado** (`<id>_thrown`) | 1º quadro de levar golpe | 3 (3 × 1) | O personagem sendo agarrado pela gola por alguém invisível à direita: 1. puxado para a frente, surpreso; 2. pés saindo do chão; 3. corpo arremessado na horizontal. |
| 3 | **Pushblock** (`<id>_pushblock`) | guarda | 3 (3 × 1) | Defesa que empurra: 1. guarda fechada; 2. abre os braços com força para a frente, base firme; 3. volta para a guarda. |
| 4 | **Air dash** (`<id>_air_dash`) | clipe de dash no chão | 4 (2 × 2) | No ar: 1-2. corpo inclinado para a frente, horizontal, voando rápido, roupa e cabelo para trás; 3-4. o mesmo indo para trás (back air dash), corpo inclinado para trás. Pés fora do chão. |
| 5 | **Batida na parede** (`<id>_wall_splat`) | reação no ar | 3 (3 × 1) | Atingido e jogado de costas contra uma parede invisível à esquerda: 1. costas batendo na parede, braços abertos; 2. quicando para longe dela; 3. caindo inclinado para a frente. |
| 8 | **Rolamento** (`<id>_roll`) | clipe de levantar | 4 (4 × 1) | Rolamento de chão no levantar: 1. de costas no chão encolhendo; 2-3. rolando de lado como uma bola; 4. saindo do rolamento já agachado em guarda. |
| 9 | **Air tech** (`<id>_air_tech`) | pose de pulo | 3 (3 × 1) | Recuperação no ar: 1. corpo encolhido girando (cambalhota); 2. abrindo o corpo; 3. pronto para cair em guarda, pés para baixo. |

Agarrão, especial e Super já têm arte no p01 (vídeo). Itens 2 e 5 também servem para o Guard Cancel e o ground bounce (o ground bounce usa a
reação no ar que já existe).

## Lutador Teste 2 (player_two)

| Folha | Situação |
|---|---|
| Golpes agachado (2L, 2M, 2H) e no ar (jL, jM, jH) | Sem arte: usa a pose de agachado e de pulo. |
| Defesa (em pé, agachado, no ar) | Sem arte. |
| Levar golpe (em pé, agachado, no ar), queda, no chão, levantar | Sem arte. |
| Raio do ultra (`ULTRA_BEAM`, 9 poses) | Usa o soco forte. Mesmo prompt da folha do Kamehameha (`tools/ultra/README.md`, seção Raio final). |
| Painéis do ultra (5 imagens) | Arte provisória do motor. Prompts em `tools/ultra/prompts.json`. |

## Efeitos (folhas de 4 quadros lado a lado)

| Efeito | Hoje | Fundo | Prompt |
|---|---|---|---|
| **Impacto na parede** | só tremor de tela | transparente | Rachaduras e poeira de impacto de anime numa parede invisível, vista de frente: estilhaços, pedrinhas e nuvem bege saindo do ponto de batida. Os 4 quadros: impacto, máximo, se espalhando, sumindo. |
| **Escudo do pushblock** | só aviso na tela | preto puro | Onda de energia defensiva azul-clara em forma de meia-lua vertical, como um escudo que empurra, com faíscas. Os 4 quadros: abrindo, máximo, se expandindo, sumindo. |
| **Clarão do Guard Cancel** | congelamento + aviso | preto puro | Clarão de contra-ataque de anime: estrela branca e dourada com raios em X e linhas de velocidade. Os 4 quadros: surgindo, máximo, se abrindo, sumindo. |
| **Rastro do air dash** | linhas desenhadas pelo jogo | preto puro | Rastro de velocidade horizontal de anime, linhas brancas e um leve brilho, mais grosso perto do corpo. Os 4 quadros em loop. |

Efeitos com fundo preto passam por `tools/ultra/preparar_raio.py` (o preto vira
transparência); o script de efeitos de cenário/combate ainda vai ganhar um modo genérico
quando o primeiro desses chegar.
