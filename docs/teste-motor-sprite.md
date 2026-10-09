# Motor Sprite V2 — branch de teste

Base: ccr-60181022-rh9cms. Os dados de combate/frames existentes são autoritativos.

## Implementado para testar
- Video QA (video_quality.py): diagnósticos temporais por frame, suspeita de
  proporção alterada, costura do loop, quadros redundantes, cortes e tronco;
  gera JSON, contact sheet PNG e painel HTML. Não remove quadros sozinho.
- Motor de harmonia (motion_harmony.py): compara marcha/overdrive com
  distancePerFrame e o impacto visual contra startup/active/recovery.
- Conversor (video_para_sprite.py): QA opcional e preservação opt-in de peças
  separadas (útil para acessórios e chicote); caminho original é preservado.
- personagem.py: QA HTML/PNG/JSON criado a cada conversão final na pasta
  android/app/build/video-quality, que não é incorporada ao APK.
- Runtime: SpriteAnimationSync sincroniza o quadro de ascensão de normal/super
  salto à velocidade vertical do CombatEngine, sem mexer no movimento ou hitbox.
  Andar continua usando o deslocamento real do personagem.
- SpriteAtlasCache: medição de memória decodificada e reciclagem em falha.
- atlas_budget.py: previsão de memória (uncompressed bitmap) por elenco.
- Testes Python e JUnit; workflow Android habilitado para esta branch.

## Execução
    pip install -r tools/sprites/requirements.txt -r tools/sprites/requirements-animar.txt
    python -m unittest discover -s tools/sprites/tests -v
    python tools/sprites/motion_harmony.py
    python tools/sprites/atlas_budget.py --limit-mib 256
    python tools/sprites/video_quality.py vídeo.mp4 --output android/app/build/qa/exemplo --loop
    python tools/sprites/build_characters.py --check
    cd android && gradle testDebugUnitTest assembleDebug -PrequireSpriteCheck=true

## Critérios de aprovação
Conferir o APK em aparelho físico: caminhada frente/trás, dash, super pulo,
golpes e frame ativo, cancelamentos, hitstop, defesa, transições, Overdrive e
uso de memória. A revisão visual de identidade, mãos e traço ainda exige humano:
as heurísticas não conseguem comprovar fidelidade anatômica. Reportar um aviso
não muda as regras de golpe, hitbox, dano ou a arte original.

Para eventual otimização de texturas por orçamento, comparar consumo e stutter
em dispositivo antes de substituir o pré-carregamento atual.
