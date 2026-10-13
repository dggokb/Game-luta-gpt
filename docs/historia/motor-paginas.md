# Motor de páginas de mangá (leitura guiada) — padrão do modo história

Este é o motor que conta a história do jogo inteiro. Cada página é uma **página de mangá
pronta** (uma imagem com todos os quadros, balões e onomatopeias desenhados). O jogo faz a
**leitura guiada** dela na tela deitada do celular, quadro a quadro, com movimento de câmera,
efeitos de ação e os balões escritos no idioma do aparelho.

A história em si (episódios, cartões de título, lutas no meio) está em [README.md](README.md).
Os prompts antigos, quadro a quadro, ficam em [prompts-ep00-ep01.md](prompts-ep00-ep01.md)
só para as páginas provisórias que ainda não viraram páginas prontas.

## Como a página é lida

1. **Página inteira** (0,9 s): a página aparece toda, para o jogador ver a diagramação.
2. **Quadro a quadro**: a câmera entra sozinha no primeiro quadro. O quadro em foco fica
   aceso e o resto da página escurece. O zoom leva o quadro até preencher a tela (no máximo 3×
   a imagem, para não borrar). Quadros mais altos que a tela são percorridos de cima para
   baixo; os outros ganham um zoom lento enquanto estão na tela.
3. **Balões**: o primeiro balão do quadro abre sozinho logo que a câmera chega e o texto é
   digitado. Cada toque completa o texto ou abre o próximo balão. Com todos os balões abertos,
   o toque leva ao próximo quadro.
4. **Fim**: no último quadro, o toque passa direto para a próxima página (ou para a luta, o
   "próximo episódio" etc.). A página não volta a aparecer inteira.

O botão **PULAR** (canto de cima) vai direto para a próxima luta ou para o "próximo
episódio". Com controle, qualquer botão vira a página e B pula.

## Efeitos por quadro

Cada quadro pode ter vários efeitos em `"efeitos"`. Eles dão o movimento que a arte parada não
tem; use à vontade nos quadros de ação.

| Efeito | O que faz | Quando usar |
|---|---|---|
| `impacto` | A câmera chega de supetão (0,24 s, acelerando), com um tranco de zoom de 10% que assenta, a tela treme forte e um clarão branco. Toca o som `impacto` | golpes, gritos, onomatopeias de pancada |
| `tremor` | A tela treme de leve o tempo todo enquanto o quadro está na tela | tensão, força, olhares de raiva, preparação de golpe |
| `velocidade` | Linhas de foco de mangá (da borda para o centro) tremeluzindo por cima do quadro | ação, movimento rápido, chegada de golpe |
| `zoom` | A câmera avança 12% sobre o quadro, desacelerando | olhares, falas importantes, momentos dramáticos |

Combinações que funcionaram no Prólogo: golpe = `impacto` + `velocidade`; grito com olhar =
`impacto` + `tremor`; close dramático = `zoom` (+ `tremor` se for de raiva).

## Balões e idiomas

- O script de recorte acha os **balões de fala**: miolo branco e liso com texto escuro dentro.
- No jogo, o miolo de cada balão é **pintado de branco** (apagando o texto da arte) e o jogo
  escreve o `texto` do balão dentro da `caixa`, no maior tamanho de letra que couber. O
  contorno e o rabicho continuam sendo os da arte.
- O texto passa pela tradução do idioma do aparelho:
  `android/app/src/main/assets/historia/idiomas/<código>.json` (hoje `en` e `es`). Chave = o
  texto em português; valor = a tradução. Texto sem tradução aparece em português.
- **Onomatopeias** (balões espetados, letras soltas como HA, TCHECK, HAA!) não são tocadas:
  fazem parte da arte e valem para qualquer idioma.
- **Gritos com palavra** num balão espetado (como o "MALDITO!" da página 5 do Prólogo) precisam
  de tradução: rode o recorte com `--gritos`, que também lista os balões espetados com texto
  (em laranja na imagem de conferência), e apague do JSON os que forem onomatopeia.
- `legenda`: narração escrita pelo jogo numa faixa embaixo da tela (para o que não está
  desenhado na página, como "Tóquio. Dez anos atrás."). Também é traduzida.

## Passo a passo de uma página nova

### 1. Gerar a página

Página em pé (o GPT gera 1024×1536), no mesmo estilo das páginas do Prólogo. Para o recorte
sair sozinho:

- **divisas brancas ou claras** entre os quadros (linha preta sozinha não separa quadros);
- desenho **sem passar por cima da divisa** (cabelo, braço, onomatopeia por cima da divisa
  junta dois quadros e aí precisa de ajuste à mão);
- **balões de fala brancos e lisos** (sem degradê nem textura), com o texto em português ou
  vazios: o jogo apaga o miolo de qualquer jeito;
- onomatopeias à vontade, desenhadas na arte.

### 2. Recortar

```bash
pip install opencv-python-headless numpy
python3 tools/historia/recortar_pagina.py pagina.png --episodio ep00 --nome pag5
```

O script grava a página em `assets/historia/ep00/pag5.jpg` e imprime o passo do episódio com
os quadros na ordem de leitura (de cima para baixo, da esquerda para a direita) e os balões de
cada quadro. Também grava `pag5_quadros.jpg`: os quadros numerados em vermelho, os balões em
rosa e a caixa do texto em verde. **Confira essa imagem**: quadros que saíram juntos ou na
ordem errada se corrigem mexendo no `area` (frações da página, de 0 a 1).

### 3. Preencher o passo no `episodio.json`

Cole o passo onde a página entra no episódio e complete:

- o `texto` de cada balão (o texto em português da fala);
- os `efeitos` de cada quadro;
- uma `legenda` onde precisar de narração.

```json
{ "tipo": "pagina", "imagem": "pag4.jpg", "quadros": [
  { "area": [0.99, 0.0, 0.99, 0.18, 0.01, 0.18, 0.01, 0.0], "efeitos": ["zoom"],
    "baloes": [ { "texto": "Meninos!", "caixa": [0.53, 0.04, 0.65, 0.09], "area": [0.68, 0.06, ...] } ] },
  { "area": [0.99, 0.73, 0.99, 1.0, 0.01, 1.0, 0.01, 0.73], "efeitos": ["impacto", "velocidade"],
    "baloes": [ { "texto": "SIM!", "caixa": [...], "area": [...] },
                { "texto": "SIM!", "caixa": [...], "area": [...] } ] }
] }
```

| Campo | O que é |
|---|---|
| `imagem` | o arquivo da página, na pasta do episódio |
| `quadros[].area` | contorno do quadro: `[x0, y0, x1, y1, ...]` em frações da página |
| `quadros[].efeitos` | lista com `impacto`, `tremor`, `velocidade`, `zoom` |
| `quadros[].legenda` | narração do jogo embaixo da tela (opcional) |
| `quadros[].baloes[].texto` | a fala em português (a chave da tradução) |
| `quadros[].baloes[].caixa` | onde o texto é escrito: `[esquerda, topo, direita, base]` |
| `quadros[].baloes[].area` | contorno do miolo branco que o jogo pinta |

### 4. Traduzir

Acrescente cada texto novo (falas, legendas, títulos) em `historia/idiomas/en.json` e
`es.json`. O teste `traducoesCobremOPrologo` avisa quando um texto do Prólogo fica sem
tradução; ao criar um episódio novo, estenda o teste para ele.

### 5. Revisar no PC

```bash
cd android
gradle :pc-preview:runHistoria                # cada quadro em foco vira um PNG em build/historia/
gradle :pc-preview:runHistoria -Pidioma=en    # o mesmo em inglês
gradle :story-core:test                       # valida episódios, imagens e traduções
```

## Onde fica cada coisa no código

| Arquivo | O que faz |
|---|---|
| `android/story-core/.../PaginaGuiada.java` | a leitura guiada: câmera, escurecer, balões, efeitos e legenda |
| `android/story-core/.../PaginaHistoria.java` | toca o episódio (título, páginas, lutas, próximo episódio, PULAR) |
| `android/story-core/.../Episodio.java` | leitura do `episodio.json` (passos, quadros, balões, efeitos) |
| `android/story-core/.../Temporada.java` | elenco, ordem dos episódios e idiomas (`setIdioma`, `t`) |
| `android/app/.../GameView.java` | botão HISTÓRIA, lutas do episódio, idioma do aparelho |
| `tools/historia/recortar_pagina.py` | acha quadros e balões e gera o passo do episódio |
| `android/pc-preview/.../HistoriaPreview.java` | grava as páginas em PNG no PC |

`story-core` é Java puro (sem Android), como o `ultra-core`: o mesmo código roda no celular e
no PC.

### Ajustes de ritmo (`PaginaGuiada`)

| Constante | Valor | O que controla |
|---|---|---|
| `OVERVIEW_HOLD` | 0,9 s | tempo da página inteira antes do primeiro quadro |
| `MOVE` | 0,5 s | viagem da câmera entre quadros |
| `WHIP` | 0,24 s | chegada de supetão (`impacto`) |
| `PUNCH` | 0,3 s | duração do tranco de zoom do `impacto` |
| `FIRST_BALLOON_DELAY` | 0,25 s | espera até o primeiro balão abrir |
| `PaginaHistoria.TYPE_SPEED` | 48 letras/s | velocidade da digitação |
| `MAX_SCALE` | 3× | zoom máximo sobre a imagem |
| `PAN_SPEED` | 110 px/s | velocidade do passeio em quadros altos |
| `DIM_ALPHA` | 185 | quanto o resto da página escurece (0 a 255) |

## Limites conhecidos

- Os balões usam a fonte negrito padrão do aparelho, não a fonte de quadrinho da arte.
- Balão com degradê ou textura no miolo fica com a mancha branca aparente ao ser apagado.
- Quadros separados só por linha preta, ou com desenho atravessando a divisa, precisam de
  ajuste à mão no `area`.
- Páginas mais largas que altas funcionam, mas o motor foi pensado para páginas em pé.
