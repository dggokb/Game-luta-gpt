# Idle do P01 — integração de super-resolução

A atualização incorpora a `ccr-60181022-rh9cms` no commit
`735e75f4a01c77d38f0a3284cb2c21d87234b465` e ativa o idle do P01
derivado do teste Real-ESRGAN AnimeVideo-v3. Os 76 frames mantêm a sequência,
o loop e 42 ms por frame. `character.json`, gameplay, golpes e perfil do P01
não foram alterados.

Os masters 4× (1024×1024) permanecem em
`art/p01-hd-redraw-20261009`, pasta `art/redraws/p01_hd/idle_sr_4x`.
Para o runtime, todos os frames recebem a mesma redução para 512×512 (2×),
com RGB premultiplicado e alpha do original ampliado uniformemente.
O atlas final tem 3310×3808, cerca de 48,1 MiB decodificados em RGBA;
as bordas vazias são removidas por um único recorte comum ao atlas.
Não há alinhamento, reenquadramento ou suavização temporal por frame.

`pixelScale: 2` no clip descreve a densidade de pixels, não a escala física.
O gerador, renderer e preview dividem a escala visual por essa densidade.
A raiz continua equivalente a (128,238) na tela original; os demais clips
mantêm `pixelScale=1`. As fontes originais permanecem intactas e são
registradas em `sourceReferences` e nos hashes do relatório.

## Reprodução

Extraia o ZIP dos masters 4× e execute:

```bash
python tools/sprites/prepare_p01_idle_sr.py --frames CAMINHO/frames
python tools/sprites/build_characters.py --write
python tools/sprites/build_characters.py --check
```

`p01-idle-sr.manifest.json` identifica os 76 masters e o modelo original.
O preparador rejeita hashes, dimensões ou alpha incompatíveis com o original
atual. O pipeline gera o PNG de runtime, geometria Java e relatório juntos.

## Validação e limites

O teste compara os 76 alphas com a mesma ampliação dos originais e verifica
o limite de textura de 4096 por eixo, orçamento abaixo de 64 MiB, sequência
e duração. O teste Android verifica a raiz e o tamanho no renderer em ambos
os sentidos, inclusive a volta à densidade normal em um golpe.

Esta integração autoriza o teste no jogo. Super-resolução pode reconstruir
cores e linhas; não foi comprovada redução da tremulação do vídeo original.
Não foi usada referência da P02. APK não é um entregável desta atualização.

## Aprovação do teste visual

Em 09/10/2026, o usuário aprovou o idle integrado após o teste no jogo: "deu muito certo". Este é o resultado aprovado para referência das próximas animações: Real-ESRGAN AnimeVideo-v3, masters 4×, runtime 2×, alpha original ampliado uniformemente, mesma raiz e ritmo. A aprovação visual do usuário é distinta da CI Android, que ainda estava em execução no momento do registro. Pipeline e 12 testes locais de sprites passaram.

Próxima animação para teste: WALK_FORWARD, 20 frames. Sua progressão no jogo usa distância percorrida (12,5 unidades por frame), não 42 ms como o idle. Preservar essa diferença ao preparar o novo teste.
