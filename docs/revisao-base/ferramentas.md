# Repositórios e recursos que ajudam nas melhorias

Pesquisa feita para as frentes do [direcao.md](direcao.md) e do [mecanica-ataque.md](mecanica-ataque.md). Para cada item: o que é, a licença, onde entra e a decisão.

Regra para entrar: a ferramenta fica na **autoria** (Python com numpy, torch e afins), e o jogo e o gerador das folhas (`build_characters.py`, só Pillow) não ganham dependência nova.

## 1. Convenções de luta (referência, não motor)

| Recurso | Licença | O que nos dá | Decisão |
|---|---|---|---|
| [Ikemen GO](https://github.com/ikemen-engine/Ikemen-GO) (motor compatível com MUGEN 1.1) | MIT (código) | O formato `HitDef` do MUGEN: `pausetime` (pausa) separado de `hittime` (atordoamento), `sparkno` e `guard.sparkno` (faísca de acerto e de defesa), `animtype` light/medium/hard/back/up/diagup e `ground.type` high/low/trip ([tutorial da Elecbyte](https://elecbyte.com/mugendocs/tutorial4.html)) | **Usar como conferência.** A nossa lista de reações bate com essa divisão: altura (rosto/tronco = high/low) × força (forte = hard), e o MUGEN também separa pausa de atordoamento como nós. O MUGEN tem 6 desenhos em pé (2 alturas × 3 forças) e nós temos 3, porque fraco e médio dividem o desenho. A lista do mecanica-ataque.md fica como está. |
| [Castagne](https://github.com/panthavma/castagne) (Godot) e [Fray](https://github.com/pyxus/fray) | Castagne pede crédito e fonte das mudanças | Editor de golpes e estrutura de estados | **Não.** O nosso motor é Java/OpenGL ES; só serve de leitura. |

## 2. Sprites a partir dos vídeos

| Recurso | Licença | O que nos dá | Decisão |
|---|---|---|---|
| [ToonOut](https://github.com/MatteoKartoon/BiRefNet) ([pesos](https://huggingface.co/joelseytre/toonout), [artigo](https://arxiv.org/html/2509.06839v1)) | MIT (código e pesos), CC-BY 4.0 (dados) | BiRefNet ajustado para personagem de anime: o artigo mede 95,3% → 99,5% de pixels certos, com ganho no cabelo | **Testado e descartado para o fundo verde** (seção "Testes"). Volta a ser opção só para vídeo com fundo que não seja liso. |
| [SAM 2](https://github.com/facebookresearch/sam2) | Apache-2.0 | Máscara que acompanha o personagem pelo vídeo inteiro | **Depois.** Para vídeos em que o efeito ou o fundo encosta no corpo e o recorte confunde os dois. |
| ffmpeg `mpdecimate` ([fonte](https://ffmpeg.org/doxygen/4.1/vf__mpdecimate_8c_source.html)) | o do ffmpeg, que já usamos | Remove quadros quase iguais antes de escolher as poses | **Usar na preparação** dos próximos vídeos de reação: o vídeo de IA repete quadros, e isso confunde a escolha das poses-chave. |
| [Real-ESRGAN](https://github.com/xinntao/Real-ESRGAN) (modelo `realesr-animevideov3`) | ver o LICENSE do repositório | Aumenta a resolução de vídeo de anime e firma o traço | **Testar depois**, só se a folha final ficar mole no celular. |
| [AniLines](https://github.com/zhenglinpan/AniLines-Anime-Lineart-Extractor) e [Anime2Sketch](https://github.com/Mukosame/Anime2Sketch) | ver os repositórios | Tiram o traço do quadro | **Depois**, para o passo 14 (retoque das poses-chave): o traço igual em todos os clipes. |
| [colortrans](https://github.com/dstein64/colortrans) | MIT | Transferência de cor global (Reinhard, histograma, PCA) | **Não.** O `harmonia_cor.py` já corrige por material e mantém o traço, o que é mais fino. Fica só como comparação. |
| [ToonCrafter](https://github.com/ToonCrafter/ToonCrafter) | ver o repositório | Gera os quadros entre duas poses | **Não.** Sai em 512×320, abaixo das nossas folhas, e o próprio cartão do modelo avisa de tremor. |
| [Laigter](https://azagaya.itch.io/laigter) | GPLv3 no programa; o autor diz que os mapas gerados são livres | Mapa de normais para luz 2D no sprite | **Não agora.** A luz do cenário no personagem já existe sem textura extra; um mapa de normais dobra a memória de textura no celular. |

## 3. Efeitos

| Recurso | Licença | O que nos dá | Decisão |
|---|---|---|---|
| [Effekseer](https://effekseer.github.io/Help_Tool/en/overview.html) | MIT | Editor de partículas que exporta a animação como folha de sprites ([gravação](https://effekseer.github.io/Helps/15x/Tool/en/ToolReference/record.html)) | **Serve para o passo 15** (refazer a faísca-âncora): uma faísca por força (L/M/H), defesa e contra, exportada em folha. É programa de janela e o instalador fica nos releases do GitHub, que a sessão na nuvem não baixa; roda num computador com tela. |
| [PVFX Foundry Thirteen](https://nerijs.itch.io/pvfx-foundry-thirteen), [spark effect](https://opengameart.org/content/spark-effect) | CC0 | Faíscas prontas | **Só referência.** São pixel art, outro estilo. |

## 4. Som

| Recurso | Licença | O que nos dá | Decisão |
|---|---|---|---|
| Kenney Impact Sounds ([listagem](https://godotengine.org/asset-library/asset/edit/7723)) | CC0 | Impactos curtos | **Bom, mas o site bloqueia o download automático (403).** A pessoa baixa no kenney.nl e põe em `tools/sons/`, e o `sons_cc0.py` monta o resto. |
| Sonniss GDC Game Audio Bundle ([notícia 2026](https://rekkerd.org/sonniss-releases-gdc-2026-game-audio-bundle/)) | Livre de royalties, sem atribuição; proíbe treinar IA | Biblioteca profissional de soco, pancada e deslocamento de ar | **A melhor fonte de qualidade.** Também é download manual (vários GB). |
| sfxr ([jfxr](https://github.com/ttencate/jfxr), [sfxr.js](https://awesome.ecosyste.ms/projects/github.com%2Fhumphd%2Fsfxr.js)) | MIT / BSD-3 | Som sintetizado | **Não.** Soa a 8 bits, fora da direção. |

## 5. Caixas e frame data

| Recurso | Licença | O que nos dá | Decisão |
|---|---|---|---|
| [rafaelalmeidatk/hitbox](https://awesome.ecosyste.ms/projects/github.com%2Frafaelalmeidatk%2Fhitbox) | ver o repositório | Editor visual de caixas por quadro de folha, exporta JSON | **Depois**, se editar caixas no `character.json` à mão virar gargalo. |
| [sf6-sensei](https://sf6-sensei.pages.dev/en/licenses) | Dados CC-BY-SA (SuperCombo) | Frame data do SF6 em JSON | **Só para comparar** as nossas tabelas. Não entra no jogo por causa do SA. |
| FAT (Frame Assistant Tool) | — | — | Não achei repositório público com os dados. |

## Testes feitos (9 de novembro de 2026)

**Quadros repetidos (`mpdecimate`).** Nos três vídeos de reação do Ryūga (24 quadros/s, 124 quadros), o filtro do ffmpeg acha 43, 76 e 61 quadros repetidos: só 82, 49 e 64 desenhos diferentes. O vídeo de IA anda, na prática, a 10–16 desenhos por segundo. Os quadros já escolhidos nas três folhas caem todos em desenhos diferentes (nenhum repetido). O teste foi para o `video_para_sprite.py` (`repetido()`, com os limites do mpdecimate: blocos 8×8, 768/320/0,33) e avisa quando `--quadros` pega dois iguais, porque segurar a pose é trabalho do `durationsMs`, não de um quadro repetido na folha.

**ToonOut contra o nosso recorte (chroma key).** São 9 quadros dos três vídeos, com o modelo em ONNX ([sprited/birefnet-toonout-onnx](https://huggingface.co/sprited/birefnet-toonout-onnx), MIT) rodando no onnxruntime, e o nosso recorte com a receita do clipe:
- as máscaras batem em 92–97% (IoU);
- o ToonOut deixa franja verde no cabelo e na borda: 2.186 a 5.788 pixels verdes por quadro, contra 0 no nosso;
- ele mantém o risco vermelho do golpe, que a receita tira;
- num quadro do HIT_HEAVY ele devolveu o fundo verde inteiro.

Comparação: [referencias/chroma_vs_toonout.jpg](referencias/chroma_vs_toonout.jpg) (à esquerda o nosso, à direita o ToonOut). O modelo foi feito para separar o personagem de fundos desenhados; com fundo verde liso, o recorte por cor é exato e não precisa de rede neural.

## Ordem

1. Feito: aviso de quadro repetido no `video_para_sprite.py`; ToonOut testado e fora.
2. Passo 15 (faísca): Effekseer, num computador com tela.
3. Som: Kenney ou Sonniss, com o download feito pela pessoa.
4. Só se o teste pedir: Real-ESRGAN, AniLines, SAM 2, editor de caixas.
