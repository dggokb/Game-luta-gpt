# Modo história: mangá animado entre as lutas

A história vem do documento "História Base — Torneio dos Irmãos" (Drive, pasta
game-luta-gpt). Este documento descreve **como ela é contada no jogo**. Os episódios
`ep00` e `ep01` são o MVP: textos de rascunho para revisar, com arte provisória até as
imagens chegarem (prompts em [prompts-ep00-ep01.md](prompts-ep00-ep01.md)).

## A proposta: anime episódico em páginas de mangá

O documento pede um modo história que funcione "como um anime episódico, e não como uma
sequência de telas de próxima luta". A forma mais barata de produzir isso e que combina com
o que o jogo já tem é o **mangá animado**, a mesma linguagem da Página Final do ultra:

- **Cada episódio** segue a estrutura do documento: cartão de título → páginas de mangá
  (preparação, convivência, conflito) → luta → páginas pós-luta → "Próximo episódio".
- **Cada página** se monta painel por painel: o painel "bate" na página com o mesmo
  tranco do ultra, os balões estalam e o texto é digitado. O toque avança, como virar a
  página. Lembranças (o Prólogo, os flashbacks do pai) têm a **página preta e as cores
  sépia**, a convenção de flashback dos mangás.
- **Arte**: imagens paradas geradas no GPT, uma por painel, com o mesmo estilo dos painéis
  do ultra. Movimento vem do motor (zoom lento de câmera, painel batendo, linhas de
  velocidade, tremor), não da arte, então não precisa de animação nem de vídeo.
- **Gancho de anime**: todo episódio fecha com o cartão "PRÓXIMO EPISÓDIO" + "CONTINUA...".
- **Referências visuais**: os concepts dos personagens da história (Takatoshi aos 45 e aos 55,
  Dafu, os irmãos aos 8 anos) e o logo estão em `art/concepts/`. O logo aparece no cartão de
  título de cada episódio (`historia/logo.jpg`).

O que fica para depois do MVP (quando o ritmo estiver aprovado): vozes e trilha, escolha de
lado no chaveamento (campanha do Ryūga e campanha do Kōga), lutas variáveis sorteadas,
salvar progresso e um menu de episódios.

### Como a temporada se divide (proposta)

| Episódio | Conteúdo | Luta |
|---|---|---|
| 0 — O Pai que Partiu | Infância, o convite, a divisão dos irmãos, o pai parte e não volta | — |
| 1 — Duas Cartas | Dez anos depois, treino, as duas cartas, motivos de cada um | Treino Ryūga × Kōga |
| 2 — O Sorteio | O templo, a regra dos capitães, a formação das duplas, o chaveamento em lados opostos | 1ª luta de cada lado |
| 3 a 6 | Episódios alternando o lado do Ryūga e o lado do Kōga (lutas fixas de história + lutas variáveis) | uma por episódio |
| meio da temporada | A revelação de que o desejo é real e pode ressuscitar a mãe | luta fixa |
| penúltimo | Final 2×2 entre as duas equipes e a regra escondida: a dupla vencedora luta entre si | final 2×2 e duelo |
| último | O Campeão é Takatoshi; o segredo do contrato e do Dafu | chefe final |

## Como funciona no jogo

- Botão **HISTÓRIA** no HUD (abaixo de DUPLA) toca a temporada desde o Prólogo. **PULAR**
  (canto de cima) vai direto para a próxima luta ou para o "próximo episódio". Com
  controle, qualquer botão vira a página e B pula.
- Numa luta da história, o jogo monta as duplas do episódio, liga a IA e mostra o rótulo
  da luta (ex.: TREINO). Depois do KO e da comemoração, a história continua. Uma derrota
  repete a luta, a não ser que o passo diga `"repetirSeDerrota": false`.
- Voltar para a seleção (DUPLA ou SELEÇÃO) abandona o episódio.

O motor é o módulo `android/story-core` (Java puro, como o `ultra-core`): `PaginaHistoria`
toca o episódio e desenha; o `GameView` só repassa toques, desenha por cima e faz as lutas.

## Formato dos episódios

`android/app/src/main/assets/historia/temporada.json` define o elenco (nome e cor nos
balões; a cor também pinta a arte provisória) e a ordem dos episódios. Cada episódio fica em
`historia/<id>/episodio.json`, com as imagens dos painéis na mesma pasta. O campo `logo`
aponta a imagem do cartão de título (sem ele, o cartão escreve o nome do jogo).

```json
{
  "id": "ep01", "numero": 1, "titulo": "Duas Cartas",
  "passos": [
    { "tipo": "titulo" },
    { "tipo": "pagina", "layout": "faixa-e-dois", "tom": "papel", "paineis": [
      { "imagem": "pag1_q1.jpg", "cena": "dojo", "personagens": ["ryuga", "koga"], "foco": 0.4,
        "efeito": "velocidade", "sfx": "PÁ!", "posicaoSfx": [0.75, 0.3],
        "baloes": [
          { "tipo": "fala", "quem": "koga", "texto": "...", "pos": [0.5, 0.2], "rabo": [0.5, 0.5] }
        ] }
    ] },
    { "tipo": "luta", "rotulo": "TREINO", "dupla": ["player_base", "p02"], "cpu": ["p03", "p04"] },
    { "tipo": "proximo", "titulo": "O Sorteio", "linhas": ["...", "..."] }
  ]
}
```

| Campo | Valores |
|---|---|
| `tipo` do passo | `titulo`, `pagina`, `luta`, `proximo` |
| `layout` | `splash` (1), `duas-colunas` (2), `tres-colunas`, `tres-faixas`, `faixa-e-dois`, `dois-e-faixa` (3), `quatro`, `grade` (4). Sem `layout`, vale o padrão pela quantidade de painéis |
| `tom` | `papel` (normal) ou `flashback` (página preta, cores sépia) |
| `cena` (arte provisória) | `dojo`, `dojo-noite`, `casa`, `estrada`, `templo`, `ceu`, `chuva`, `carta` |
| `enquadramento` | `plano` (corpo inteiro), `close` (rosto), `detalhe` (objeto) |
| `efeito` | `velocidade`, `impacto`, `chuva`, `brilho` (desenhados por cima da imagem) |
| `tipo` do balão | `fala`, `pensamento`, `grito`, `narracao` |
| `pos`, `rabo`, `posicaoSfx` | frações da caixa do painel: `[0, 0]` topo esquerdo, `[1, 1]` base direita. `rabo` é para onde aponta o bico do balão (a boca de quem fala) |
| `foco` | 0 a 1: que parte da imagem aparece quando o painel é mais largo que ela |

Sem a imagem, o painel mostra a arte provisória (cenário, silhuetas nas cores do elenco e
o nome do arquivo esperado). Assim dá para escrever e testar o ritmo antes da arte.

## Páginas prontas (recomendado)

Uma página de mangá inteira numa imagem só (em pé, como as do GPT), com balões e
onomatopeias já desenhados. O jogo faz a **leitura guiada**: mostra a página inteira, entra
quadro a quadro com o resto da página escurecido (o zoom leva o quadro até preencher a tela,
e quadros altos são percorridos de cima para baixo) e, depois do último, volta para a página
inteira antes de seguir.

```bash
pip install opencv-python-headless numpy
python3 tools/historia/recortar_pagina.py pagina.png --episodio ep00 --nome pag5
```

O script grava a página em `assets/historia/ep00/pag5.jpg`, acha os quadros pelas divisas
claras e imprime o passo pronto para colar no `episodio.json`, na ordem de leitura (de cima
para baixo, da esquerda para a direita). Ele também grava `pag5_quadros.jpg` com os quadros
numerados: confira. Quadros separados só por uma linha preta, ou com o desenho passando por
cima da divisa (cabelo, onomatopeia), podem sair juntos; aí corrija o `area` à mão (foi o
caso dos olhos na página 2 do Prólogo).

```json
{ "tipo": "pagina", "imagem": "pag4.jpg", "quadros": [
  { "area": [0.0, 0.0, 1.0, 0.0, 1.0, 0.18, 0.0, 0.18], "efeitos": ["zoom"],
    "baloes": [ { "texto": "Meninos!", "caixa": [0.53, 0.04, 0.65, 0.09], "area": [0.68, 0.06, ...] } ] },
  { "area": [0.0, 0.73, 1.0, 0.73, 1.0, 1.0, 0.0, 1.0], "efeitos": ["impacto", "velocidade"],
    "legenda": "Tóquio. Dez anos atrás." }
] }
```

- `area`: o contorno do quadro em frações da página (`[x0, y0, x1, y1, ...]`, de 0 a 1).
- `efeitos` (quantos quiser):
  - `impacto`: a câmera chega de supetão, com tranco de zoom, tela tremendo e clarão (golpes, gritos);
  - `tremor`: a tela treme de leve o tempo todo enquanto o quadro está em foco (tensão, força);
  - `velocidade`: linhas de foco de mangá tremeluzindo por cima do quadro (ação);
  - `zoom`: a câmera avança devagar sobre o quadro (olhares, momentos dramáticos).
- `baloes`: os balões de fala desenhados na página. O jogo pinta o miolo (`area`) de branco,
  apagando o texto da arte, e escreve o `texto` dentro da `caixa`, no idioma do aparelho, com
  a letra do maior tamanho que couber. O primeiro balão do quadro abre sozinho e digita; cada
  toque completa o texto ou abre o próximo balão; depois do último, o toque passa de quadro.
  O script acha os balões sozinho (miolo branco liso com texto); falta escrever o `texto`.
- `legenda`: narração do jogo numa faixa embaixo da tela.

As onomatopeias (HA, PA, TCHECK, HAA!...) ficam na arte: são iguais em qualquer idioma.

## Idiomas

`historia/idiomas/<código>.json` (ex.: `en.json`, `es.json`) é um objeto com o texto em
português como chave e a tradução como valor. Vale para balões (das páginas prontas e das
provisórias), legendas, títulos, o "próximo episódio" e os botões (PULAR, CONTINUA...). O jogo
usa o idioma do aparelho; texto sem tradução aparece em português. O teste
`traducoesCobremOPrologo` avisa quando um texto novo do Prólogo ficou sem tradução.

Para ver as páginas em outro idioma no PC:
`gradle :pc-preview:runHistoria -Pidioma=en`.

## Revisar no PC

```bash
cd android
gradle :pc-preview:runHistoria   # grava cada página montada em build/historia/
gradle :story-core:test          # valida os episódios (layouts, elenco, lutadores)
```
