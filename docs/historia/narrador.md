# Narrador do torneio

O torneio tem um narrador que anima o espetáculo, como a narradora do Torneio das Trevas. Ele
fala o começo da luta, os golpes, os combos, o Super e o Ultra, quem está em apuros, quem domina
e a virada, os 10 segundos finais, o K.O. e o vencedor. A luta não para enquanto ele fala.

O personagem ainda vai ser criado. Por enquanto o retrato é um **placeholder**: uma silhueta de
tinta com microfone e um "?". O nome também é provisório (`NARRADOR`).

## Na tela

Uma faixa de transmissão, o "terço de baixo" da TV, fica no rodapé, no meio, entre o direcional
e os botões, antes do botão OD. Ao lado dela fica o retrato num quadro de mangá, com bordas de
tinta e linhas de foco na cor do narrador.

- A faixa sobe quando ele começa a falar e desce um pouco depois da última fala.
- O texto entra digitando. Uma fala grande (começo, Ultra, K.O., vitória) aparece em amarelo,
  maior e com um tranco.
- Em cima da faixa fica uma plaquinha com o nome e uma luz vermelha de "no ar".
- Quando o retrato real existir, ele balança enquanto o narrador fala. Na expressão "chocado"
  o quadro também treme.
- A faixa some por baixo das faixas de cinema (cinemática da luta, Ultra) e da Página Final.

Código: `story-core` `historia/Narracao.java` (falas, fila, desenho) e `app` `NarradorLuta.java`
(o que aconteceu na luta). Quem liga os dois é o `GameView` (`updateNarracao`).

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
- **`expressao`**: escolhe o retrato `historia/retratos/narrador_<expressao>.png`. Sem a
  imagem, usa `narrador_normal.png`; sem nenhuma das duas, o placeholder.
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

## Para o personagem de verdade

1. Escolher o nome e a cor: `nome` e `cor` no `narrador.json` (e a tradução do nome nos idiomas).
2. Gerar os retratos em `historia/retratos/`:
   - `narrador_normal.png`;
   - `narrador_empolgado.png`;
   - `narrador_chocado.png`.
   O formato é o mesmo dos retratos das conversas. O quadro corta a imagem de cima, então o
   rosto deve ficar no terço de cima.
3. Revisar as falas com a voz dele: o jeito de falar, os bordões e como ele chama o público.
4. Quando tiver voz gravada, cada evento pode ganhar um som (ainda não implementado).

Testes: `NarracaoTest` (story-core: falas, variáveis, prioridade, traduções) e
`NarradorLutaTest` (app: o que a luta conta ao narrador).
