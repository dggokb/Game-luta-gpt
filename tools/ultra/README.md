# Página Final: como criar um ultra

O ultra é ativado com **baixo + SUPER** e custa **3 barras**. O lutador faz uma investida:

- se **errar**, as barras são gastas e o lutador fica 0,5 s vulnerável;
- se **acertar**, começa a cinemática "Página Final": uma página de mangá se monta em 5 painéis e um anel vai fechando no centro da tela. **Tocar quando o anel fecha** dá bônus de dano: PERFEITO dá +25% e BOM dá +10% no golpe final. Depois a página estilhaça e a luta volta com o oponente voando.

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
   python tools/ultra/preparar_ultra.py --id player1 --nome "Explosão Solar" \
       --cor "#F4B73B" --entrada pasta/com/as/imagens
   ```

   O script recorta cada imagem no formato do painel e aplica o **filtro de mangá**. O filtro dá o mesmo acabamento a imagens diferentes da IA, com contraste, tinta nas bordas e retícula nas sombras. Ele grava tudo em `android/app/src/main/assets/ultras/player1/`.

### Ou gere direto pela API

```bash
pip install pillow openai
export OPENAI_API_KEY=...
python tools/ultra/preparar_ultra.py --id player1 --nome "Explosão Solar" --cor "#F4B73B" \
    --gerar --descricao "lutador de cabelo espetado preto, kimono laranja" --cor-nome dourada \
    --referencia ficha_player1.png
```

Dá pra gerar só alguns painéis de novo com `--paineis golpe final`.

### Ajustes úteis

- `--foco olhos=0.4`: move o recorte para cima (0) ou para baixo (1) quando a parte importante ficou fora.
- `--intensidade 0.6`: filtro mais suave. Use `1.5` para um filtro mais pesado, ou `--estilo nenhum` para desligar.
- `--cor-secundaria "#FFE7A3"`: cor do brilho (centro do raio, onomatopeia final).

## ultra.json

```json
{
  "nome": "Explosão Solar",
  "cor": "#F4B73B",
  "corSecundaria": "#FFE7A3",
  "paineis": {
    "olhos":    { "imagem": "olhos.jpg" },
    "carga":    { "imagem": "carga.jpg", "onomatopeia": "VRUUUUM" },
    "golpe":    { "imagem": "golpe.jpg", "onomatopeia": "KRAAK!", "som": "soco" },
    "atingido": { "imagem": "atingido.jpg", "onomatopeia": "TUMM!" },
    "final":    { "imagem": "final.jpg", "onomatopeia": "KABUUUM!" }
  }
}
```

Tudo é opcional, menos `nome`. O script preserva as onomatopeias e os sons que você editou à mão.

## Sons

Coloque arquivos `.ogg`, `.wav` ou `.mp3` em `assets/ultras/sons/` (valem para todos os ultras) ou em `assets/ultras/<id>/sons/` (só para aquele ultra). O nome do arquivo é o id do som. Ids que o jogo toca:

| id | quando |
|---|---|
| `ativacao` | o ultra é ativado |
| `investida` | começa a investida |
| `pagina` | a cinemática começa |
| `painel` | cada painel bate na página |
| `carga` | painel de carga (ou o `som` do painel) |
| `impacto` | golpe e atingido (ou o `som` do painel) |
| `explosao` | detonação final (ou o `som` do painel) |
| `quebra` | a página estilhaça |

Som que não existe é ignorado. O jogo funciona mudo até os sons chegarem.

## Ver no PC

O motor (`android/ultra-core`) é Java puro e roda igual no Android e no PC. O visualizador mostra a cinemática numa janela:

```bash
cd android
gradle :ultra-preview:run                                             # ultra provisório
gradle :ultra-preview:run --args="app/src/main/assets/ultras/player1" # ultra de verdade
```

Na janela, **clique ou espaço** é o toque e **R** repete. Com `--quadros saida/` o visualizador grava os quadros em PNG em vez de abrir a janela.
