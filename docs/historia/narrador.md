# Narrador do torneio

O torneio tem um narrador que anima o espetáculo, como a narradora do Torneio das Trevas. Ele
fala o começo da luta, os golpes, os combos, o Super e o Ultra, quem está em apuros, quem domina
e a virada, os 10 segundos finais, o K.O. e o vencedor. A luta não para enquanto ele fala.

O personagem é o apresentador de terno preto, colete vermelho e dourado e microfone com o leão
do concept `art/concepts/narrador_concept.png` (model sheet) e `art/concepts/narrador.png` (no
palco da arena). O nome ainda é provisório (`NARRADOR`).

## Na tela

Uma faixa de transmissão, o "terço de baixo" da TV, fica no rodapé, no meio, entre o direcional
e os botões, antes do botão OD. Ao lado dela fica o retrato num quadro de mangá, com bordas de
tinta e linhas de foco na cor do narrador.

- A faixa sobe quando ele começa a falar e desce um pouco depois da última fala.
- O texto entra digitando. Uma fala grande (começo, Ultra, K.O., vitória) aparece em amarelo,
  maior e com um tranco.
- Em cima da faixa fica uma plaquinha com o nome e uma luz vermelha de "no ar".
- O retrato balança enquanto o narrador fala; gritando no microfone (`empolgado`) balança mais.
- A faixa some por baixo das faixas de cinema (cinemática da luta, Ultra) e da Página Final.

Código: `story-core` `historia/Narracao.java` (falas, fila, desenho) e `app` `NarradorLuta.java`
(o que aconteceu na luta). Quem liga os dois é o `GameView` (`updateNarracao`).

## No palco

O narrador também aparece **em pessoa no cenário**, atrás dos lutadores e na frente do fundo,
debaixo de um canhão de luz. Por enquanto é um **placeholder animado**: um boneco de tinta com
as cores do concept (terno preto, colete vermelho, dourado, microfone), articulado por código.
Quando houver sprites dele, eles entram no lugar do boneco.

- **Onde fica**: na profundidade 200 do palco (a régua do `stage.json`), em pé no chão. Fica
  na frente das paredes e dos pilares e atrás das lanternas, braseiros e objetos do chão, com a
  mesma perspectiva do cenário (anda e cresce menos que os lutadores quando a câmera mexe).
- **Acompanha a luta**: fica de fora da dupla, do lado com mais espaço na tela, olhando para
  eles. Anda junto quando a luta anda. Quando o lado dele aperta (a dupla chega no canto), ele
  troca de lado correndo por trás dos lutadores. Num close (Super, Ultra, soco de câmera,
  cinemática) ele não escolhe lado novo, para não sair correndo à toa.
- **Os gestos seguem a fala** da faixa:
  - calado: microfone no peito, a outra mão na cintura, respirando, o peso trocando de perna;
  - `normal`: microfone na boca, a mão aberta acenando para a luta;
  - `empolgado`: o braço lá em cima; numa fala grande (começo, Ultra, K.O., vitória) ele pula;
  - `serio`: inclinado para a frente, apontando para a luta.
  A boca mexe e saem riscos de som enquanto o texto digita; o canhão de luz fica mais forte
  quando ele fala.
- Aparece só nas lutas narradas (as mesmas da faixa).

Código: `story-core` `historia/NarradorPalco.java` (lado, andar, pose e desenho) e
`stage-core` `StageScene.drawBackground(canvas, z, desenho)` (encaixa o desenho na
profundidade). No `GameView`: `drawNarradorPalco`. Sem o palco 3D (cenário antigo), ele fica
um pouco acima do chão dos lutadores, menor.

## Quando ele narra

Ele narra só as lutas de verdade:

- a batalha (IA ON);
- o Arcade;
- as lutas do **torneio** na história (`"torneio": true` no passo de luta).

Fica calado no treino, no tutorial, nas demos e nas lutas da história fora do torneio.

## As falas: `historia/narrador.json`

```json
{
  "nome": "NARRADOR",
  "cor": "#B8202E",
  "eventos": {
    "inicio": {"prioridade": 3, "expressao": "empolgado", "falas": ["Que comece o espetáculo! {p1} e {p2} no centro da arena!"]}
  }
}
```

- **`falas`**: em português. Uma é sorteada, sem repetir a última daquele evento.
- **Variáveis entre chaves**: vêm do jogo. Os nomes saem do elenco da história (`nome` de quem
  tem aquele `lutador` em `temporada.json`); sem nome no elenco, vale o nome do pack.
- **`expressao`**: escolhe o retrato `historia/retratos/narrador_<expressao>.png`:
  - `normal`: sorrindo de canto (comentários, agarrão, troca, tempo esgotado);
  - `empolgado`: gritando no microfone (começo, Super, Ultra, combo grande, virada, K.O., vitória);
  - `serio`: de cara fechada (guard break, combo breaker, apuros, 10 segundos, empate).
  Sem a imagem, usa `narrador_normal.png`; sem nenhuma das duas, uma silhueta com microfone.
- **Traduções**: em `historia/idiomas/en.json` e `es.json`, com a fala inteira como chave e as
  mesmas variáveis. O teste `NarracaoTest` cobra uma tradução para cada fala.

### Prioridade

| Prioridade | O que faz |
|---|---|
| 3 (grande) | Entra na hora, por cima de tudo. Atrás de outra grande, espera a vez (o K.O. e depois a vitória). |
| 2 (importante) | Corta um comentário. Atrás de outra importante, espera na fila. Logo depois de uma fala, espera um respiro. |
| 1 (comentário) | Só sai depois de 2 s de silêncio e sem nada na fila. |

O mesmo evento só volta depois de 8 s (os de prioridade 3 não esperam).

### Eventos

| Evento | Prioridade | Quando | Variáveis |
|---|---|---|---|
| `inicio` | 3 | GO! | `p1`, `p2` |
| `primeiro_golpe` | 1 | o primeiro golpe que entra | `atacante` |
| `combo` / `combo_grande` | 1 / 2 | combo que acabou com 5+ / 10+ acertos | `atacante`, `n` |
| `super` | 2 | um Super começa | `atacante` |
| `ultra` | 3 | um Ultra começa | `atacante` |
| `guard_break` | 2 | a guarda quebrou | `defensor` |
| `combo_breaker` | 2 | combo quebrado | `atacante` (quem quebrou) |
| `defesa_perfeita` | 1 | defesa perfeita | `defensor` |
| `agarrao` | 1 | agarrão que entrou | `atacante` |
| `punicao` | 1 | punish counter | `atacante` |
| `troca` | 2 | entrou outro lutador (tag ou parceiro depois do K.O.) | `entra` |
| `apuros` | 2 | o titular com 25% da vida ou menos (uma vez por lutador) | `nome` |
| `dominando` | 1 | a dupla com 40% de vida a mais (uma vez por luta) | `nome` |
| `virada` | 2 | a liderança (15% de vida da dupla) trocou de lado | `nome` |
| `tempo` | 2 | 10 segundos no relógio | — |
| `ko` / `ko_ultra` | 3 | um lutador caiu (com o Ultra) | `vencedor`, `perdedor` |
| `vitoria` / `perfeito` | 3 | fim da luta (o vencedor sem perder vida) | `vencedor` |
| `tempo_esgotado` / `empate` | 3 | fim pelo tempo / empate | `vencedor` |

## Os retratos

Os três rostos saem dos close-ups da coluna da direita do model sheet (de cima para baixo:
`normal`, `empolgado`, `serio`), com o fundo tirado:

```
python3 tools/historia/retratos_narrador.py
```

Rode de novo quando o concept mudar (as caixas de recorte ficam no começo do script). O teste
`NarracaoTest` cobra um retrato para cada expressão usada no `narrador.json`.

## O que falta

1. O nome: `nome` no `narrador.json` (e a tradução do nome nos idiomas).
2. Revisar as falas com a voz dele: o jeito de falar, os bordões e como ele chama o público.
3. Quando tiver voz gravada, cada evento pode ganhar um som (ainda não implementado).
4. Os sprites dele no palco (parado, andando, correndo, falando, empolgado, apontando) no lugar
   do boneco.

Testes: `NarracaoTest` (story-core: falas, variáveis, prioridade, traduções, retratos),
`NarradorPalcoTest` (story-core: lado, andar, pose de cada fala, pulo),
`StageSceneTest.anExtraDrawingGoesBetweenTheDeeperAndTheShallowerLayers` (stage-core) e
`NarradorLutaTest` (app: o que a luta conta ao narrador).
