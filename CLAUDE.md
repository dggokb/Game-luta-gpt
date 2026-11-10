# Regras do dono do jogo para qualquer sessão

## Antes de começar qualquer tarefa: diga se o Opus dá conta

O dono escolhe o modelo da sessão pelo custo. **Antes de começar a fazer algo, avise se a tarefa
cabe no Opus ou se pede o Fable, e espere ele decidir.** Diga o motivo em uma ou duas linhas e
deixe claro que é avaliação, não medida.

Regra prática (de 10/11, pelo que se viu até aqui):

| Tipo de trabalho | Modelo |
|---|---|
| Implementar com receita definida, ajustar parâmetros, instalar efeitos, gravar vídeos, documentar, avaliar links e skills | Opus |
| Problema aberto sem caminho claro, ou erro difícil sem pista | Fable |

Nenhum modelo testa no celular daqui, e quem julga o resultado visual é o dono, em vídeo.

## Git

- Push direto na `game-luta-sprite-fable`, depois de `git fetch` e `git merge` do origin. Sem
  rebase, sem force, sem PR. O APK sai sozinho na release `build-game-luta-sprite-fable`.
- A `game-luta-sprite-fable` é o que ele instala e testa: trabalho pela metade ou não conferido,
  avise e pergunte antes de mandar para lá.

## VFX

Use a skill `vfx-luta` (`.claude/skills/vfx-luta`) antes de mexer em qualquer efeito, e leia
`docs/vfx-continuar.md` e `reports/VFX de luta estilo anime.md`.
