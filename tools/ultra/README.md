# Página Final: como criar um ultra

O ultra é ativado com **baixo + SUPER** e custa **3 barras**. A regra fica no `CombatEngine` (veja `docs/combat-engine.md`). O lutador faz uma investida:

- se **errar**, as barras são gastas e o lutador fica 0,5 s vulnerável;
- se o oponente **defender**, ele toma blockstun e o atacante fica vulnerável do mesmo jeito;
- se **acertar**, começa a cinemática "Página Final": uma página de mangá se monta em 5 painéis e um anel vai fechando no centro da tela. **Tocar quando o anel fecha** dá bônus de dano: PERFEITO dá +25% e BOM dá +10% no golpe final e no raio. Depois a página estilhaça e a luta volta com o personagem disparando o **raio final**: vários hits seguidos e uma explosão que arremessa o oponente.

## Os 5 painéis

Todo ultra tem a mesma estrutura. Só mudam as imagens, os textos e os sons.

| Painel | O que mostra | Formato |
|---|---|---|
| `olhos` | close nos olhos do atacante + nome do golpe | faixa bem larga (5,6:1) |
| `carga` | atacante de corpo inteiro carregando energia | quadrado |
| `golpe` | o golpe vindo na direção da câmera | faixa larga (3,2:1) |
| `atingido` | o oponente sendo atingido | faixa larga (3,6:1) |
| `final` | o disparo final, tela cheia | 16:9 |

Painel sem imagem usa a **arte provisória** desenhada pelo próprio jogo. Dá pra testar o ritmo antes de ter qualquer arte.

As imagens devem mostrar o atacante **virado para a direita**. Quando ele luta virado para a esquerda, o jogo espelha os painéis sozinho.

## Passo a passo com a IA

1. **Ficha do personagem**: gere uma vez, com o prompt `ficha` de [`prompts.json`](prompts.json). Ela é usada como referência em todas as outras imagens, para o personagem sair sempre igual.
2. **As 5 imagens**: use os prompts fixos de `prompts.json`, trocando `{descricao}` pela descrição do personagem e `{cor}` pela cor da energia. Salve como `olhos.png`, `carga.png`, `golpe.png`, `atingido.png` e `final.png` numa pasta. A imagem `atingido` pode ser a mesma para todos os personagens.
3. **Prepare**:

   ```bash
   pip install pillow
   python tools/ultra/preparar_ultra.py --id player_base --nome "Explosão Solar" \
       --cor "#F4B73B" --entrada pasta/com/as/imagens
   ```

   O script recorta cada imagem no formato do painel e grava tudo em `android/app/src/main/assets/ultras/player_base/`. Se as imagens saírem com estilos muito diferentes entre si, use `--estilo manga`: ele aplica um filtro de contraste, tinta e retícula que unifica o acabamento. Com imagens já consistentes, o filtro piora o resultado, por isso vem desligado.

### Ou gere direto pela API

```bash
pip install pillow openai
export OPENAI_API_KEY=...
python tools/ultra/preparar_ultra.py --id player_base --nome "Explosão Solar" --cor "#F4B73B" \
    --gerar --descricao "lutador de cabelo espetado preto, kimono laranja" --cor-nome dourada \
    --referencia ficha_player_base.png
```

Dá pra gerar só alguns painéis de novo com `--paineis golpe final`.

### Ajustes úteis

- `--foco olhos=0.4`: move o recorte para cima (0) ou para baixo (1) quando a parte importante ficou fora.
- `--estilo manga --intensidade 0.6`: liga o filtro de mangá numa versão suave (`1.5` deixa pesado).
- `--cor-secundaria "#FFE7A3"`: cor do brilho (centro do raio, onomatopeia final).

## ultra.json

```json
{
  "nome": "Explosão Solar",
  "cor": "#F4B73B",
  "corSecundaria": "#FFE7A3",
  "dano": 4200,
  "paineis": {
    "olhos":    { "imagem": "olhos.jpg" },
    "carga":    { "imagem": "carga.jpg", "onomatopeia": "VRUUUUM" },
    "golpe":    { "imagem": "golpe.jpg", "onomatopeia": "KRAAK!", "som": "soco", "posicaoOnomatopeia": [0.6, 0.92] },
    "atingido": { "imagem": "atingido.jpg", "onomatopeia": "TUMM!" },
    "final":    { "imagem": "final.jpg", "onomatopeia": "KABUUUM!" }
  }
}
```

Cada ultra fica na pasta com o **id do personagem** (`player_base`, `player_two`...). `dano` é o dano total antes da escala de combo (padrão 4000): metade nos 3 acertos da cinemática (15%, 15% e 20%) e metade no raio final.

Tudo é opcional, menos `nome`. O script preserva as onomatopeias, sons e posições que você editou à mão.

`posicaoOnomatopeia` move a onomatopeia dentro do painel, para ela não cobrir o rosto do personagem. Os valores são frações da caixa do painel: `[0, 0]` é o canto de cima à esquerda e `[1, 1]` é o canto de baixo à direita.

## Raio final

Quando a página quebra, o personagem carrega e dispara um raio estilo Kamehameha que acerta
várias vezes (o contador de hits vai subindo) e termina numa explosão que arremessa o
oponente. A arte tem três partes: o **raio** (4 imagens), os **efeitos animados** (aura,
vento e poeira) e a **pose do personagem** (folha de 9 sprites).

### O raio

São 4 imagens com **fundo preto puro** (não transparente: brilho com fundo transparente sai
com borda feia). Cole antes de cada prompt:

```
Efeito visual de energia para jogo de luta 2D em estilo anime, traço limpo, cores ricas,
energia {cor-nome} com núcleo branco incandescente, igual a um super ataque de anime.
Fundo preto puro (#000000), sem nada além do efeito. Sem personagens, sem mãos, sem texto,
sem marca d'água, sem chão, sem cenário.
```

| Arquivo | Formato | Prompt |
|---|---|---|
| corpo | horizontal | Um feixe de energia horizontal reto e grosso atravessando a imagem inteira de ponta a ponta, centralizado na altura, núcleo branco no meio, camadas da cor por fora, raios elétricos e faíscas nas bordas. Ocupa metade da altura e é cortado reto nas bordas esquerda e direita, com a mesma espessura dos dois lados, para se repetir. |
| ponta | horizontal | O mesmo feixe, mas com a frente do disparo: um leque de energia abrindo na ponta. (Pode vir apontando para qualquer lado: use `--espelhar-ponta` se a frente estiver à esquerda.) |
| inicio | horizontal ou quadrado | A origem do disparo: esfera de energia branca muito brilhante com anéis de onda de choque em volta (elipses verticais), raios de luz e faíscas, com o feixe começando a sair para a direita. |
| impacto | quadrado | Explosão de energia enorme no impacto: clarão branco no centro, bola de fogo, raios em estrela, anel de choque e fagulhas. Centralizada, com espaço preto em volta. |

Prepare (o preto vira transparência, o corpo fica sem emenda e as bordas se fundem):

```bash
pip install pillow numpy
python tools/ultra/preparar_raio.py --id player_base --corpo corpo.png \
    --ponta ponta.png --espelhar-ponta --inicio inicio.png --impacto impacto.png
```

O script grava `raio_*.png` e o bloco `raio` do `ultra.json` (o que você não passar
continua como estava):

- `hits`: acertos pequenos antes da explosão (1 a 60). Mais hits = raio mais longo.
- `espessura`: altura do feixe no mundo do jogo, em pixels (o personagem tem uns 226). A esfera nas mãos, os anéis e as explosões crescem junto. Só muda este número: não precisa gerar imagem de novo.
- `centroInicio`: onde está o centro da esfera na imagem `inicio` (o script acha sozinho).

### Efeitos animados

Três folhas de 4 quadros lado a lado. Cole antes de cada prompt:

```
Efeito visual para jogo de luta 2D em estilo anime, traço limpo, desenhado à mão.
4 quadros de animação lado a lado, mesmo tamanho e mesma posição em todos, espaço vazio
entre eles. Sem personagens, sem texto, sem marca d'água, sem chão, sem cenário.
```

| Arquivo | Fundo | Prompt |
|---|---|---|
| aura | preto puro | Aura de energia {cor-nome} em forma de chamas subindo, como a de um guerreiro de anime se energizando: labaredas em volta de um espaço vazio do tamanho de uma pessoa em pé (o meio é preto, sem corpo), mais intensa embaixo, com faíscas. Os quadros mostram as labaredas tremulando, em loop. |
| vento | preto puro | Rajada de vento de anime: feixes de linhas curvas brancas e amarelo-claras, com pequenos redemoinhos, horizontal e alongada. Os quadros mostram o vento em movimento, em loop. |
| poeira | transparente | Nuvem de poeira e fumaça de anime rolando rente ao chão, bege-claro e cinza-claro, com pedrinhas soltas, base reta embaixo. Os quadros: nascendo pequena, crescendo, no máximo e se desfazendo. |

```bash
python tools/ultra/preparar_raio.py --id player_base --aura aura.png --vento vento.png --poeira poeira.png
```

O script separa os quadros pelos vãos vazios entre eles (o GPT não respeita colunas
iguais), alinha a aura e a poeira pela base e o vento pelo centro, e limpa o véu quase
invisível que o fundo "transparente" do GPT deixa (num cenário escuro ele vira uma caixa
clara). Na folha, o vento sopra para a direita; o jogo espelha quando precisa.

### Pose do personagem

Folha de 9 poses do mesmo personagem (anexe uma folha dele como referência no ChatGPT),
virado para a direita, **fundo transparente**, grade 3 × 3: 1. sai da guarda com as mãos
em concha na cintura; 2. carga; 3. carga máxima, gritando, roupa e cabelo subindo no vento;
4. disparo, as duas mãos empurradas para a frente, roupa jogada para trás; 5-7. sustentando
o disparo com a roupa tremulando (7 inclinado pelo recuo); 8. recuperação; 9. guarda. Sem
energia nas mãos (o jogo desenha). Para importar:

```bash
python3 tools/sprites/separar_folha.py folha.png --colunas 3 --linhas 3 \
    art/sprites/source/<id>_ultra_beam_source.png
python3 tools/sprites/build_characters.py --write
```

O clip fica em `tools/sprites/clips/<id>_ultra_beam.json` e o `character.json` liga a
animação em `specialAnimations` como `"ULTRA"` (veja `player_base`). Depois meça onde ficam
as mãos (pose 2 para a carga e 4 para o disparo, em pixels à frente e acima da raiz) e
anote em `raio.maos`.

### ultra.json

```json
"raio": { "corpo": "raio_corpo.png", "ponta": "raio_ponta.png", "inicio": "raio_inicio.png",
          "centroInicio": [0.443, 0.479], "impacto": "raio_impacto.png", "hits": 20, "espessura": 210,
          "aura": {"imagem": "raio_aura.png", "quadros": 4},
          "vento": {"imagem": "raio_vento.png", "quadros": 4},
          "poeira": {"imagem": "raio_poeira.png", "quadros": 4},
          "maos": {"carga": [-26, 124], "disparo": [112, 152]} }
```

- `maos`: mãos na pose de carga (onde a esfera cresce) e no disparo (de onde o raio sai).

Sem o bloco `raio` ou sem alguma imagem, aquela parte é desenhada pelo jogo com as cores do
ultra; sem a folha de pose, o personagem usa o soco forte.
Para ver no PC: `gradle :pc-preview:runRaio` grava os quadros em `build/raio/`.

## Sons

Coloque arquivos `.ogg`, `.wav` ou `.mp3` em `assets/ultras/sons/` (valem para todos os ultras) ou em `assets/ultras/<id>/sons/` (só para aquele ultra). O nome do arquivo é o id do som. Ids que o jogo toca:

| id | quando |
|---|---|
| `ativacao` | o ultra é ativado |
| `investida` | começa a investida |
| `pagina` | a cinemática começa |
| `painel` | cada painel bate na página |
| `carga` | painel de carga (ou o `som` do painel) e a carga do raio final |
| `impacto` | golpe e atingido (ou o `som` do painel) |
| `explosao` | detonação final (ou o `som` do painel) e explosão do raio |
| `quebra` | a página estilhaça |
| `raio` | o raio final sai das mãos ("HA!") |

Som que não existe é ignorado. O jogo funciona mudo até os sons chegarem.

## Ver no PC

O motor (`android/ultra-core`) é Java puro e roda igual no Android e no PC. Como ele funciona por dentro (linha do tempo, camadas do desenho, como portar): `docs/ultra-pagina-final.md`. O visualizador mostra a cinemática numa janela:

```bash
cd android
gradle :pc-preview:run                                                # ultra provisório
gradle :pc-preview:run --args="app/src/main/assets/ultras/player_base" # ultra de verdade
```

Na janela, **clique ou espaço** é o toque e **R** repete. Com `--quadros saida/` o visualizador grava os quadros em PNG em vez de abrir a janela.
