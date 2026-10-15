# Padronização de Sprites

Ferramenta completamente independente criada do zero nesta branch. **Não utiliza código, personagens ou assets de jogo algum.**

## Funções iniciais

1. Extrai TODOS os frames de vídeos (sem pular ou cortar automaticamente).
2. Aceita imagem mestre transparente ou de chroma key verde para a pose em pé (idle) e, opcionalmente, para a pose agachada (crouch).
3. Procura a pose do mestre no início ou fim de cada vídeo usando comparação de silhueta.
4. Calcula UM fator de escala por vídeo, preservado por todos os frames.
5. Cria um único canvas e um único root para TODOS os vídeos processados no mesmo lote.
6. Exporta PNGs transparentes e um manifesto JSON, com FPS, escala, referência e tempos.
7. Pode sugerir um período de loop sem remover frames.

## Instalação

Python 3.10+.

Execute: python -m pip install -r requirements.txt

## Rodar

Crie uma pasta com os vídeos e mestres; copie e adapte examples/project.example.json.

Execute: python sprite_standardizer.py examples/project.example.json --output resultado/

## Calibração

A imagem mestre idle define a escala, por meio de idle_target_height (256 por padrão, independente de qualquer jogo).
A referência crouch usa a proporção de altura entre os dois PNGs mestres.
**IMPORTANTE:** isso só é correto quando ambos os mestres estão na mesma escala original. Se não estiverem, use target_heights.crouch para informar uma altura validada.

Cada clip recebe reference_pose idle ou crouch. Opcionalmente, reference_frame indica o índice explícito do frame de calibração; se não for uma pose suficientemente similar, o sistema rejeita a importação.

O canvas é global para o lote. A origem compartilhada e as escalas ficam no arquivo manifest.json.

## Limitações conhecidas

- Não inventa poses, não remove frames e não faz estabilização anatômica artificial.
- O processo pode detectar incorretamente uma pose muito parecida; revisar o resultado continua necessário.
- Dash e salto preservam deslocamento visual dentro do vídeo; um futuro renderer deve evitar aplicar essa translação duas vezes.
- Loop é uma sugestão baseada em similaridade de sequências, não um corte ou garantia de emenda perfeita.
- A remoção de fundo é projetada para verde e transparência; não para cenários variados.
- Não produz spritesheets neste MVP: gera os PNGs e metadados necessários para criar um atlas depois.

## Testes

Execute: python -m unittest discover -s tests -v
