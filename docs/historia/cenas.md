# Cenas: chamar uma cena de qualquer ponto do jogo

Uma **cena** é um episódio inteiro ou um trecho dele (páginas de mangá, cartão de título,
lutas e o treino). As cenas têm nome e ficam em `android/app/src/main/assets/historia/cenas.json`:

```json
{
  "intro":      { "episodio": "ep00", "ate": "intro" },
  "tutorial":   { "episodio": "ep00", "ate": "treino" },
  "pai-parte":  { "episodio": "ep00", "de": "pai-parte" },
  "prologo":    { "episodio": "ep00" },
  "duas-cartas":{ "episodio": "ep01" }
}
```

- `de` e `ate` são **marcas** de passos do `episodio.json` (`"marca": "intro"` no passo). Os
  dois passos entram na cena. Sem `de`, a cena começa no primeiro passo; sem `ate`, vai até o
  último.
- Um nome que não está no arquivo, mas é a pasta de um episódio (`ep01`), toca o episódio
  inteiro.
- As imagens são as da pasta do episódio, então a cena não duplica nada.

## No código

```java
playScene("intro", () -> { /* o que vem depois */ });
```

`GameView.playScene` abre a cena por cima da luta e chama o `Runnable` quando ela acaba
(também quando o jogador aperta PULAR até o fim). Se a cena tiver uma luta ou o treino, eles
acontecem no meio, como no modo história. Se a cena não abrir, o `Runnable` é chamado direto,
e o jogo não trava.

Hoje:

| Quem chama | Cena | Depois |
|---|---|---|
| Botão **TUTORIAL** | `tutorial`: a intro (título + páginas 1 a 4 do Prólogo) e o treino no dojo | volta para a seleção |
| **MODO HISTÓRIA** (menu inicial) | a temporada inteira (o Prólogo já inclui o treino depois da página 4) | próximo episódio; no fim, o menu inicial |
| Botão **HISTÓRIA** (modo teste) | a temporada inteira | próximo episódio; no fim, a seleção |

## Para criar uma cena nova

1. Ponha `"marca"` nos passos onde ela começa e termina, no `episodio.json`.
2. Acrescente a linha no `cenas.json`.
3. Chame `playScene("nome", depois)` onde ela tem que aparecer.

O `CenasTest` (`gradle :story-core:test`) abre todas as cenas do `cenas.json` e avisa se uma
marca não existe.

## Lutas com cenário e o treino

O passo `luta` aceita mais dois campos:

| Campo | O que faz |
|---|---|
| `cenario` | pasta em `assets/stages` (ex.: `dojo_shinshinkan`). Sem ele, a luta é no cenário padrão (Templo da Lua). |
| `modo` | `"tutorial"`: em vez da luta, o treino guiado dos golpes (veja `docs/tutorial.md`). Termina quando as lições acabam, sem KO. |

`dupla` e `cpu` agora são opcionais: sem eles, a luta usa a dupla que já está no jogo.
