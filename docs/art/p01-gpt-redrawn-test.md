# Teste dos sprites GPT do p01

Branch `teste-de-sprites-gpt`, criada de `game-luta-sprite-gpt` em
`1243ef3ba7fb8400cb6fb270251792a947a62b67`.

Idle e dash usam os desenhos aprovados nesta conversa: cada master contém
121 quadros em grade 11 × 11, com células nativas de 320 × 320 e transparência.
O idle contém transições interpoladas; o dash contém poses nítidas com repetições.
O dash começa no mesmo desenho do idle e termina no último quadro do idle.

As receitas conservam os masters anteriores como referências. O pipeline só
acrescenta margem transparente e registra os pés e o tronco; não amplia os novos desenhos.
A densidade de pixels mantém a altura do p01 próxima da arte anterior. O dash
usa a mesma escala uniforme do idle, substituindo a calibração anisotrópica do
dash antigo. A referência de calibração dos demais movimentos é preservada.

O idle toca os 121 quadros em aproximadamente 5,017 segundos. Os 121 quadros do
dash ficam disponíveis no atlas, mas a animação de corrida seleciona a entrada e
um ciclo de passadas de 288 ms. Assim a arrancada aparece num comando curto;
nenhuma velocidade, caixa de colisão ou regra de combate foi alterada.

O push nesta branch também dispara o workflow de testes e APK. Os arquivos
gerados pelo pipeline devem ser commitados junto das receitas e dos masters.

Fontes aprovadas no Drive:

- [Idle](https://drive.google.com/file/d/1nvUpLJDbUjU4n2NqZc4mBW3J5ooGy8Mi/view)
- [Dash](https://drive.google.com/file/d/1hssS1YHv3hKqJE1isnbiA5RTph2NTuty/view)

## Segundo teste: intro para idle

A intro foi redesenhada a partir da sequência já usada pelo jogo: respiração,
ajuste da faixa e entrada na guarda. O master tem 27 quadros, com poses repetidas,
e mantém os 2,4 segundos da animação anterior. As células de 384 × 448 dão espaço
à postura em pé sem reduzir o personagem; o idle continua com o master aprovado.

Os quadros 22 a 26 reutilizam exatamente o primeiro idle, com a mesma densidade,
escala uniforme e posição dos pés. Isso também mantém a transição suave que o
renderer já faz nesses quadros. Os offsets do `sprite-autofix.json` pertencentes
à intro anterior foram removidos; os 33 ajustes aprovados da vitória continuam.

As verificações comparam os pixels visíveis e sua posição em relação à raiz,
além do tamanho, duração e calibração da animação. Os demais movimentos e os
atlases de idle e dash do primeiro teste permanecem iguais.

- [Master da intro, 27 quadros](https://drive.google.com/file/d/1QNiDrLnikc7Lyiv46jnmAuQcQa8wXvrD/view)
- [Prévia animada da intro seguida do idle](https://drive.google.com/file/d/1FcvgrVvb23IEOkpsmxG813gNX8fC-qfJ/view)
