# P01: teste de andar para frente com super-resolução

O idle integrado foi aprovado visualmente pelo usuário em 09/10/2026. Esta branch aplica o mesmo método ao WALK_FORWARD para teste: `test/p01-walk-forward-sr-20261009`, baseada em `c7a1409a9254c0b4cfb009132b09361ee30776b3`, mantendo o idle aprovado e as alterações recebidas de `ccr-60181022-rh9cms`.

20 masters RGBA 1024×1024 gerados, sem falhas de frame. Real-ESRGAN AnimeVideo-v3, modelo de SHA256 `b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d`. RGB invisível estendido a partir do pixel visível mais próximo antes da inferência; alpha original ampliado com Lanczos e validado exatamente nos 20 frames. Sem alinhamento individual, reenquadramento ou suavização temporal. Nenhum material da P02 foi utilizado.

Original baixado pelo navegador Work: `art/sprites/source/player_base_walk_forward_video_normalized.png`, SHA256 `eef12b99cba2294e4c596b1450a363a08578810f56ab1a9c18d8420cb3e7fb52`. Cópia original preservada e registrada em `sourceReferences`. Masters, manifest, licença e scripts de reprodução em `art/redraws/p01_hd/walk_forward_sr_4x/` na branch `art/p01-hd-redraw-20261009`.

## Integração de teste

Fonte 2× derivada dos masters por uma transformação uniforme, RGB premultiplicado para a redução e alpha diretamente dos originais. Grade 4096×1536, 8 colunas, 20 células 512×512, root (256,476). Atlas empacotado 2800×1440, células 350×480, root (185,466), offset de corte (71,10), `pixelScale: 2`. Memória decodificada: 16.128.000 bytes, aproximadamente 15,4 MiB.

O renderer já suporta a densidade introduzida para o idle: divide worldScale por pixelScale. Não houve alteração adicional de renderer, hitboxes ou character.json. WALK_FORWARD mantém frames 0–19, loop e `distancePerFrame: 12.5`. A velocidade base de caminhada é 300 unidades/s; a prévia corresponde a 41,67 ms/frame nessa velocidade, e não impõe duração fixa ao jogo.

## Validação e limites

`build_characters.py --write`: PASS, 10 personagens e 310 atlases. 13 testes BuiltPackTests passaram, incluindo nova verificação dos 20 alphas, root, sequência, avanço por distância, densidade e orçamento de memória, além da regressão do idle aprovado. `git diff --check`: PASS.

Diferença temporal RGB média entre frames consecutivos, incluindo 019→000, em resolução original e sobre pixels conjuntamente opacos: 22,573067 no original; 23,930289 no resultado reduzido. A medida não compensa movimento nem garante ausência de tremulação. Aprovação visual do WALK_FORWARD pendente. Uma dependência Python ausente foi corrigida antes da execução; nenhuma falha de frame.

Sem Android SDK/Gradle/Javac neste ambiente local; APK e testes Android dependem do workflow do GitHub e não são declarados concluídos neste documento.

## Reproduzir a fonte de runtime

Baixar os 20 masters e os scripts do pacote de arte. Executar:

```sh
python tools/sprites/prepare_p01_walk_forward_sr.py --frames PASTA_MASTERS --manifest docs/art/p01-walk-forward-sr.manifest.json --original art/sprites/source/player_base_walk_forward_video_normalized.png --output art/sprites/source/player_base_walk_forward_sr_2x.png
python tools/sprites/build_characters.py --write
```

Para testar no jogo, selecionar P01, verificar a transição idle→andar para frente→idle, caminhar em ambas as direções de orientação e observar pés, cabelo e faixas na passagem 019→000. O estado de andar para trás continua com sua arte original.

## Integração na branch principal

Em 09/10/2026, a pedido do usuário, o WALK_FORWARD com super-resolução foi integrado na `game-luta-sprite-gpt`, incluindo o atlas gerado e sua configuração de densidade. O conteúdo foi trazido de `test/p01-walk-forward-sr-20261009` em `7a43a164680442caf7c46c69c35cafeca66a65f0`, após incorporar as ferramentas da `ccr-60181022-rh9cms`. O idle aprovado, os originais e o avanço de 12,5 unidades por frame continuam preservados. A aprovação visual da caminhada ainda está pendente; este registro confirma a integração para o próximo teste. Nenhum novo build de APK foi solicitado nesta integração; o commit utiliza `[skip ci]`.
