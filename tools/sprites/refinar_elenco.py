#!/usr/bin/env python3
"""Corretor/estabilizador de UM personagem por execução — inicialmente read-only.

Exemplo:
  python3 tools/sprites/refinar_elenco.py p01 --videos-dir /videos/animations/p01
  python3 tools/sprites/refinar_elenco.py p12 --videos-dir /videos/animations/p12
  python3 tools/sprites/refinar_elenco.py p01 --videos-dir /videos/animations/p01 --estado VICTORY

Os vídeos originais têm origem em Drive/animations/<id>; este script só aceita
cópias locais explicitamente disponibilizadas. Não supõe acesso ao Drive no CI.
Não altera PNGs, animações, manifestos, Git ou branches.
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
# Contrato para um lutador completo. Um NPC simplificado precisa de perfil
# explícito, jamais pode passar apenas por ter IDLE e poucos golpes.
EXPECTED_CORE={
    "IDLE","COMBAT","WALK_FORWARD","WALK_BACK","DASH","BACKDASH",
    "CROUCH","RISE","JUMP","FALL","LAND",
    "LIGHT_JAB","MEDIUM_KICK","HEAVY_STRAIGHT",
    "CROUCH_LIGHT","CROUCH_MEDIUM","CROUCH_HEAVY",
    "JUMP_LIGHT","JUMP_MEDIUM","JUMP_HEAVY","JUMP_HEAVY_DOWN",
    "DEFENSE_STAND","DEFENSE_CROUCH","DEFENSE_AIR",
    "HIT_STAND","HIT_CROUCH","HIT_AIR",
    "KNOCKDOWN","GROUNDED","GETUP",
    "THROW_GRAB","THROW_TOSS",
    "SPECIAL_ENERGY","SPECIAL_S2","SPECIAL_S3","SPECIAL_S4",
    "SUPER_WAVE","ULTRA_BEAM",
    "INTRO","VICTORY","DEFEAT","TAUNT",
}
# Estado derivado pode compartilhar o mesmo vídeo de outra pose (FALL/JUMP),
# mas deve existir no manifest quando se testa um lutador completo.
STATE_DEPENDENCIES={
    "IDLE", "COMBAT", "WALK_FORWARD", "WALK_BACK", "CROUCH", "RISE",
    "JUMP", "FALL", "DASH", "BACKDASH", "LAND"
}


STATE_SHORTCUTS={
    "2L":"CROUCH_LIGHT","2M":"CROUCH_MEDIUM","2H":"CROUCH_HEAVY",
    "L":"LIGHT_JAB","M":"MEDIUM_KICK","H":"HEAVY_STRAIGHT",
    "jL":"JUMP_LIGHT","jM":"JUMP_MEDIUM","jH":"JUMP_HEAVY",
    "jH_baixo":"JUMP_HEAVY_DOWN",
}
def canonical_state(value):
    return STATE_SHORTCUTS.get(value,value.upper())


def character_pack_id(character):
    return "player_base" if character=="p01" else character


def manifest(root, character):
    path=Path(root)/"characters"/character_pack_id(character)/"character.json"
    if not path.is_file():
        return None, str(path)
    return json.loads(path.read_text(encoding="utf-8")), str(path)


def calibration_info(root,character):
    path=Path(root)/"art/keys"/character/"tamanho.json"
    if not path.is_file():
        return {"status":"SEM_REFERENCIA_OFICIAL","path":str(path)}
    data=json.loads(path.read_text(encoding="utf-8"))
    if not data.get("altura") or not data.get("chaves",{}).get("inicio_centro"):
        return {"status":"REFERENCIA_INVALIDA","path":str(path)}
    image=path.parent/"inicio_centro.png"
    if not image.is_file():
        return {"status":"SEM_IMAGEM_BASE","path":str(image)}
    return {"status":"CALIBRADO","altura":data["altura"],
            "imagem_base":str(image),"path":str(path)}


def inspect_character(character, videos_dir=None, states=None, root=ROOT,
                      inspect_sprites=True, original_video_inspector=None,
                      packed_inspector=None, geometria=True, geometry_inspector=None):
    import estabilizador as e
    import verificar_escala as g
    if not re.fullmatch(r"p(?:0[1-9]|1[0-2])",character):
        raise ValueError("Somente UM personagem: p01..p12")
    pack, location=manifest(root,character)
    calibration=calibration_info(root,character)
    all_states=list(pack.get("animations",{})) if pack else []
    if states:
        requested=list(dict.fromkeys(canonical_state(s) for s in states))
    elif all_states:
        requested=all_states
    elif videos_dir:
        # A character not yet imported (P09-P12) can still be audited
        # against its ORIGINAL animations without inventing a game pack.
        discovered=e.match_videos(videos_dir,list(e.ALIASES))
        requested=[state for state,match in discovered.items()
                   if match["status"] in ("ENCONTRADO","AMBIGUO")]
        requested=list(dict.fromkeys(["IDLE"]+requested))
    else:
        requested=["IDLE"]
    missing=sorted(EXPECTED_CORE-set(all_states)) if pack else sorted(EXPECTED_CORE)
    if states and pack:
        missing=sorted(set(states)-set(all_states))
    videos=e.match_videos(videos_dir,requested) if videos_dir else {
        state:{"status":"SEM_VIDEO_LOCAL","files":[]} for state in requested
    }
    checks={}
    idle_video=None
    idle_packed=None
    evidence_video=None
    evidence_sprite=None
    # Geometric matches only run when original footage + atlas both exist.
    # Fake/mock readers do not have real frames; tests must inject evidence.
    if geometria and videos_dir and pack and "IDLE" in all_states:
        if original_video_inspector is None and packed_inspector is None:
            idle_path=e.match_videos(videos_dir,["IDLE"])["IDLE"]
            if idle_path["status"]=="ENCONTRADO":
                vv=g.read_video_samples(idle_path["files"][0],selection=(.35,.50,.65))
                evidence_video=vv[1] if len(vv)>=2 else None
            ss=g.read_sprite_samples(root,pack,"IDLE",selection=(.35,.50,.65))
            evidence_sprite=ss[1] if len(ss)>=2 else None
    get_video=original_video_inspector or e.load_video
    get_pack=packed_inspector or e.load_atlas_state
    # A referência é sempre o idle do PRÓPRIO lutador, nunca de outro.
    idle_mapping=e.match_videos(videos_dir,["IDLE"])["IDLE"] if videos_dir else None
    if idle_mapping and idle_mapping["status"]=="ENCONTRADO":
        meta,trace=get_video(idle_mapping["files"][0])
        if meta.get("status")=="EXTRAIDO":
            heights=[m["height"] for m in trace if m is not None]
            reference=float(sorted(heights)[len(heights)//2]) if heights else 0
            if reference:
                idle_video=e.metrics(trace,reference,"IDLE")
                idle_video["fonte"]=idle_mapping["files"][0]
        if idle_video is None:
            idle_video={"status":"INCONCLUSIVO","motivo":meta.get("motivo","idle não mensurável")}
    if inspect_sprites and pack and "IDLE" in pack["animations"]:
        meta,trace=get_pack(root,pack,"IDLE")
        if meta.get("status")=="EXTRAIDO":
            heights=[m["height"] for m in trace if m is not None]
            reference=float(sorted(heights)[len(heights)//2]) if heights else 0
            if reference:
                idle_packed=e.metrics(trace,reference,"IDLE")
        if idle_packed is None:
            idle_packed={"status":"INCONCLUSIVO","motivo":meta.get("status","sem idle empacotado")}

    for state in requested:
        entry={"video":videos.get(state,{"status":"SEM_VIDEO_LOCAL","files":[]})}
        if pack and state not in pack.get("animations",{}):
            entry["sprite"]={"status":"SEM_ESTADO_NO_MANIFESTO"}
        elif inspect_sprites and pack:
            meta,trace=get_pack(root,pack,state)
            if meta["status"]!="EXTRAIDO":
                entry["sprite"]=meta
            else:
                packed_height=idle_packed.get("medianas",{}).get("altura",0) if idle_packed else 0
                entry["sprite"]=e.metrics(trace,packed_height or 1,state)
                entry["sprite"]["fonte"]=meta["atlas"]
                entry["proporcao_sprite"]=e.compare_body(idle_packed or {},entry["sprite"])

        matches=entry["video"]
        if matches["status"]=="ENCONTRADO":
            meta,trace=get_video(matches["files"][0])
            if meta.get("status")=="EXTRAIDO":
                if state in ("JUMP","FALL","LAND"):
                    phase=e.jump_phase(trace,state)
                    if phase is None:
                        entry["video"]={**matches,"analise":{
                            "status":"INCONCLUSIVO",
                            "motivo":"salto no vídeo sem ápice e fase de descida confiáveis"}}
                        trace=[]
                    else:
                        trace=phase
                if trace:
                    videoh=idle_video.get("medianas",{}).get("altura",0) if idle_video else 0
                    video_stats=e.metrics(trace,videoh or 1,state)
                    entry["video"]={**matches,"analise":video_stats}
                    entry["proporcao_video"]=e.compare_body(idle_video or {},video_stats)
            else:
                entry["video"]={**matches,"analise":meta}
        entry["evidencias_suficientes"]=bool(
            entry["video"].get("analise",{}).get("status")=="MEDIDO" and
            entry.get("sprite",{}).get("status")=="MEDIDO" and
            entry.get("proporcao_video",{}).get("status") in ("COMPATIVEL","SUSPEITA_DE_ESCALA") and
            entry.get("proporcao_sprite",{}).get("status") in ("COMPATIVEL","SUSPEITA_DE_ESCALA")
        )
        if entry["evidencias_suficientes"]:
            ratio_video=entry["proporcao_video"]["proporcao_aproximada"]
            ratio_sprite=entry["proporcao_sprite"]["proporcao_aproximada"]
            if abs(ratio_video-ratio_sprite)>.12:
                entry["diferenca_video_jogo"]={
                    "status":"SUSPEITA",
                    "proporcao_video":ratio_video,
                    "proporcao_sprite":ratio_sprite
                }
        # Independent geometric proof: >=3 mutually consistent body matches
        # in BOTH the source video and exported sprite atlas. Silhouette
        # proxies alone NEVER prove scale normalization.
        proof={"status":"INCONCLUSIVO",
               "motivo":"sem pares de imagens comparaveis com confianca"}
        if matches["status"]=="ENCONTRADO" and \
                entry.get("sprite",{}).get("status")=="MEDIDO" and \
                entry.get("video",{}).get("analise",{}).get("status")=="MEDIDO" and \
                (geometry_inspector is not None or
                 (evidence_video is not None and evidence_sprite is not None)):
            if geometry_inspector is not None:
                # Unit-test seam; normal CLI never injects external approval.
                proof=geometry_inspector(character,state)
            else:
                slices={
                    "JUMP":(.06,.13,.20,.27,.34),
                    "FALL":(.65,.73,.81,.89,.97),
                    "LAND":(.80,.84,.88,.92,.96),
                    "VICTORY":(.52,.61,.70,.79,.88,.97),
                }.get(state,(.12,.27,.42,.57,.72,.87))
                vf=g.read_video_samples(matches["files"][0],selection=slices)
                sf=g.read_sprite_samples(root,pack,state,selection=slices)
                if vf and sf:
                    source=g.estimate_sequence(evidence_video,vf)
                    packed=g.estimate_sequence(evidence_sprite,sf)
                    proof={"status":"INCONCLUSIVO",
                           "video":source,"atlas":packed}
                    if source["status"]=="ESCALA_GEOMETRICA_ESTAVEL" and \
                            packed["status"]=="ESCALA_GEOMETRICA_ESTAVEL":
                        vs,ps=source["fator_mediano"],packed["fator_mediano"]
                        discrepancy=abs(vs-ps)/max(vs,ps)
                        proof={"status":"PADRONIZADA_FONTE_ATLAS" if discrepancy<=.06 else
                                        "ESCALA_DIVERGENTE",
                               "diferenca_relativa":round(discrepancy,4),
                               "fator_video":vs,"fator_atlas":ps,
                               "video":source,"atlas":packed,
                               "validacao_runtime":"PENDENTE"}
        entry["prova_geometrica"]=proof
        # A geometric inconsistency makes the entire state suspect.
        if proof["status"]=="ESCALA_DIVERGENTE":
            entry["diferenca_video_jogo"]={
                "status":"SUSPEITA_GEOMETRICA",
                "diferenca_relativa":proof["diferenca_relativa"]}
        checks[state]=entry
    incomplete=bool(missing or not pack or
                    calibration["status"]!="CALIBRADO" or
                    idle_video is None or idle_video.get("status")!="MEDIDO" or
                    (inspect_sprites and (idle_packed is None or idle_packed.get("status")!="MEDIDO")) or
                    any(not e0["evidencias_suficientes"] or
                        (geometria and e0["prova_geometrica"]["status"]!="PADRONIZADA_FONTE_ATLAS")
                        for e0 in checks.values()))
    warnings=[state for state,v in checks.items()
              if v.get("video",{}).get("analise",{}).get("tremor") or
                 v.get("sprite",{}).get("tremor") or
                 v.get("diferenca_video_jogo") or
                 v.get("proporcao_video",{}).get("status")=="SUSPEITA_DE_ESCALA" or
                 v.get("proporcao_sprite",{}).get("status")=="SUSPEITA_DE_ESCALA" or
                 v["prova_geometrica"]["status"] in ("ESCALA_DIVERGENTE",
                                                       "INSTAVEL_GEOMETRICAMENTE")]
    # Mesmo com métricas boas, somente o *jogo em execução* pode validar
    # transições, câmera e tamanho renderizado. Nenhuma aprovação falsa.
    return {
        "schema":2, "personagem":character, "pacote":character_pack_id(character),
        "manifesto":location if pack else None,
        "estados_manifesto":len(all_states),
        "faltantes":missing,
        "calibracao":calibration,
        "referencia_video":idle_video or {"status":"SEM_IDLE_VALIDADO"},
        "referencia_sprite":idle_packed or {"status":"SEM_IDLE_VALIDADO"},
        "estados":checks,
        "pendentes_revisao":warnings,
        "status":"ANOMALIAS_IDENTIFICADAS" if warnings else
                 ("PENDENTE_FONTES" if incomplete else "MEDICOES_CONCLUIDAS"),
        "aprovado_automaticamente":False,
        "validacao_runtime":"PENDENTE",
        "alteracoes_realizadas":0,
        "padronizacao_comprovada_nos_estados":[state for state,x in checks.items()
            if x["prova_geometrica"]["status"]=="PADRONIZADA_FONTE_ATLAS"],
        "critico":"Sem prova geometrica E teste runtime nao afirmar tamanho correto",
    }


def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__,formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("personagem",help="exatamente um ID: p01..p12")
    parser.add_argument("--videos-dir",type=Path,help="pasta LOCAL de vídeos originais do personagem")
    parser.add_argument("--estado",action="append",help="somente este estado; repetir para múltiplos")
    parser.add_argument("--saida",type=Path,default=ROOT/"android/app/build/sprite-review/estabilizador.json")
    parser.add_argument("--sem-sprites",action="store_true",help="video-only para personagem ainda não importado")
    parser.add_argument("--sem-geometria",action="store_true",
        help="somente diagnóstico preliminar; NÃO comprova padronização")
    args=parser.parse_args(argv)
    if args.videos_dir is not None and not args.videos_dir.is_dir():
        parser.error("--videos-dir deve apontar para pasta existente com MP4s")
    try:
        result=inspect_character(args.personagem,args.videos_dir,args.estado,
                                  inspect_sprites=not args.sem_sprites,
                                  geometria=not args.sem_geometria)
    except ValueError as exc:
        parser.error(str(exc))
    args.saida.parent.mkdir(parents=True,exist_ok=True)
    args.saida.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(result["personagem"],result["status"],"verificados:",len(result["estados"]),
          "inconclusivos:",sum(not s["evidencias_suficientes"] for s in result["estados"].values()),
          "anomalias:",len(result["pendentes_revisao"]))
    print("Relatório:",args.saida)
    return 0 if result["status"]=="MEDICOES_CONCLUIDAS" else 2


if __name__=="__main__":
    sys.exit(main())
