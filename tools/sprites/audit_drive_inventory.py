#!/usr/bin/env python3
"""Auditoria reprodutível: inventário do Drive x estados, clipes e perfis do jogo.

Uso:
  python3 tools/sprites/audit_drive_inventory.py
  python3 tools/sprites/audit_drive_inventory.py --video-root /videos/animations --inspect-media

Somente leitura de fontes; grava relatórios no diretório de build, nunca no Drive.
Sem --video-root, a evidência é METADADO/NOME (não há aprovação visual).
Com --inspect-media e ffmpeg/ffprobe instalados, verifica duração, resolução,
quadros quase estáticos e possíveis vídeos duplicados. Não infere o movimento
correto por visão computacional; os casos ambíguos continuam "REVISAR".
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
from video_names import CANONICAL, normalize, resolve

DERIVED_SHARED = {"COMBAT", "RISE", "FALL", "LAND", "GROUNDED", "GETUP", "THROW_GRAB", "THROW_TOSS"}
OWN_REUSE_ALLOWED = {"COMBAT", "GROUNDED", "THROW_TOSS"}
ALWAYS_REVIEW = {("p01", "CROUCH_HEAVY"): "upper.mp4: confirmar se e 2H ou S2"}
CONTENT_REVIEW = {
    ("p04", "JUMP_MEDIUM"): "Docs indicam socoPulo.mp4 igual a puloSocoForte.mp4",
    ("p08", "SPECIAL_S2"): "No mapeamento anterior S2 usava o video H",
}
MANIFEST = HERE / "videos/drive_inventory_20261010.json"


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else None


def probe(path):
    """Metadados de um vídeo baixado localmente. NUNCA altera ou renderiza fontes."""
    if not shutil.which("ffprobe"):
        return {"note": "ffprobe indisponivel"}
    try:
        cmd = ["ffprobe", "-v", "error", "-show_entries",
               "format=duration:stream=codec_type,width,height,nb_frames",
               "-of", "json", str(path)]
        raw = subprocess.run(cmd, capture_output=True, check=True, timeout=25)
        obj = json.loads(raw.stdout)
        video = next((s for s in obj.get("streams", [])
                      if s.get("codec_type") == "video"), {})
        return {"duration_s": round(float(obj.get("format", {}).get("duration", 0)), 3),
                "width": video.get("width"), "height": video.get("height"),
                "frames_reported": video.get("nb_frames")}
    except (OSError, subprocess.CalledProcessError, ValueError, StopIteration,
            subprocess.TimeoutExpired) as exc:
        return {"error": str(exc)[:180]}


def fingerprint(path):
    """Assinatura dos 1os segundos de vídeo + movimento médio dos quadros amostrados.

    Identidade da assinatura indica POSSÍVEL conteúdo duplicado, não prova igualdade
    entre arquivos completos. Não é reconhecimento semântico de movimentos.
    """
    if not shutil.which("ffmpeg"):
        return {"note": "ffmpeg indisponivel"}
    side = 32
    try:
        cmd = ["ffmpeg", "-v", "error", "-i", str(path), "-vf",
               f"fps=2,scale={side}:{side},format=gray",
               "-frames:v", "8", "-f", "rawvideo", "-"]
        out = subprocess.run(cmd, capture_output=True, check=True, timeout=45).stdout
        step = side * side
        frames = [out[i:i+step] for i in range(0, len(out)-step+1, step)]
        if len(frames) < 2:
            return {"note": "menos de 2 quadros amostrados"}
        movement = [sum(abs(x-y) for x,y in zip(a,b)) / step
                    for a,b in zip(frames, frames[1:])]
        return {"sampled_frames": len(frames),
                "sample_motion_mean": round(sum(movement) / len(movement), 3),
                "sample_signature": hashlib.sha256(b"".join(frames)).hexdigest(),
                "near_static_warning": sum(movement) / len(movement) < 0.8}
    except (OSError, subprocess.CalledProcessError, subprocess.TimeoutExpired) as exc:
        return {"error": str(exc)[:180]}


def inspect_videos(pid, videos, root, media):
    if not root:
        return {}, []
    results, missing = {}, []
    for item in videos:
        name = item["name"]
        path = root / pid / name
        if not path.is_file():
            missing.append(name)
            continue
        entry = {"local_size_bytes": path.stat().st_size}
        if media:
            entry.update(probe(path))
            entry.update(fingerprint(path))
        results[name] = entry
    return results, missing


def audit_character(pid, entry, video_root=None, inspect_media=False):
    video_files = entry["videos"]
    names = [item["name"] for item in video_files]
    found, ambiguous, unmatched = resolve(names, pid)
    path = HERE / "videos" / f"{pid}.json"
    mapped = load_json(path) or {}
    pack_path = ROOT / "characters" / ("player_base" if pid == "p01" else pid) / "character.json"
    pack = load_json(pack_path)
    anims = (pack or {}).get("animations", {})
    size_file = ROOT / "art" / "keys" / pid / "tamanho.json"
    inspected, local_missing = inspect_videos(pid, video_files, video_root, inspect_media)
    refs = defaultdict(list)
    for state, metadata in anims.items():
        refs[metadata.get("atlas", "")].append(state)
    report = {
        "character": pid, "video_count": len(video_files),
        "drive_url": entry["folder_url"], "integrated": bool(pack),
        "scale_calibrated": size_file.is_file(), "matches": {}, "unclassified": unmatched,
        "ambiguous_names": ambiguous, "missing_local_files": local_missing,
        "media_inspection": bool(video_root and inspect_media),
        "states": {}, "inspected_files": inspected, "warnings": [],
    }
    for state, basename in CANONICAL.items():
        video = found.get(state)
        mapping_video = mapped.get(state)
        atlas_state = "THROW_GRAB" if state == "THROW" else state
        atlas = anims.get(atlas_state, {}).get("atlas")
        warnings = []
        if state in ambiguous:
            warnings.append("ambiguous_name")
        if mapping_video and (not video or normalize(mapping_video) != normalize(video)):
            warnings.append("mapping_disagrees_with_drive")
        if mapping_video and not any(normalize(mapping_video) == normalize(n) for n in names):
            warnings.append("mapped_file_absent")
        if mapping_video:
            reused = [k for k,v in mapped.items()
                      if k != state and normalize(v) == normalize(mapping_video)]
            if reused:
                warnings.append("mapped_video_reused_for_" + ",".join(reused))
        if atlas and not atlas.startswith(("player_base_" if pid == "p01" else pid + "_")):
            warnings.append("other_character_atlas")
        if atlas:
            same_atlas_states = [s for s in refs[atlas]
                                 if s != state and s not in OWN_REUSE_ALLOWED
                                 and state not in OWN_REUSE_ALLOWED]
            if same_atlas_states:
                warnings.append("reused_atlas_with_" + ",".join(same_atlas_states))
        if not video:
            warnings.append("missing_named_video")
        if not atlas:
            warnings.append("not_integrated")
        if (pid,state) in ALWAYS_REVIEW:
            warnings.append("manual_semantic_review")
        if (pid,state) in CONTENT_REVIEW:
            warnings.append("known_provisional")
        # Um vídeo pode existir no Drive enquanto o jogo reutiliza outro atlas.
        report["states"][state] = {
            "canonical": basename, "video": video,
            "mapping": mapping_video, "atlas": atlas, "warnings": warnings,
        }
    # Estado de vídeo THROW é convertido em dois estados do motor.
    if pack:
        for derived in sorted(DERIVED_SHARED):
            if derived not in anims:
                report["warnings"].append("missing_derived_state:" + derived)
    return report


def audit(manifest, root=None, media=False):
    result = {
        "schema_version": 1, "base_branch": "game-luta-sprite-gpt",
        "date": manifest["scanned_date"], "evidence": "drive_metadata_and_github",
        "media_checked": bool(root and media), "characters": {}, "totals": {},
    }
    for pid, entry in sorted(manifest["characters"].items()):
        result["characters"][pid] = audit_character(pid, entry, root, media)
    allchars = result["characters"]
    result["totals"] = {
        "videos": sum(c["video_count"] for c in allchars.values()),
        "states": len(CANONICAL) * len(allchars),
        "named_videos": sum(bool(s["video"]) for c in allchars.values()
                            for s in c["states"].values()),
        "integrated_characters": sum(c["integrated"] for c in allchars.values()),
        "not_integrated_characters": [pid for pid,c in allchars.items() if not c["integrated"]],
    }
    if root and media:
        # Assinaturas somente para arquivos AMOSTRADOS — não confundir com hash de MP4.
        signatures = defaultdict(list)
        for pid,c in allchars.items():
            for name,info in c["inspected_files"].items():
                if "sample_signature" in info:
                    signatures[info["sample_signature"]].append(pid + "/" + name)
        result["possible_duplicate_samples"] = [
            group for group in signatures.values() if len(group) > 1]
    return result


def make_markdown(result):
    totals = result["totals"]
    lines = [
        "# Auditoria Drive x sprites do Game Luta GPT",
        "", f"Data do inventario: {result['date']}",
        f"Videos MP4 no Drive: **{totals['videos']}**. Estados verificados: **{totals['states']}**.",
        f"Estados com nome reconhecivel: **{totals['named_videos']}**.",
        f"Personagens com pacote integrado: **{totals['integrated_characters']}**.",
        "",
        "**Limite:** sem arquivos MP4 locais e --inspect-media, nao foram lidos quadros.",
        "Mesmo com verificacao por ffmpeg, movimentos e continuidade exigem aprovacao visual.",
        "",
        "| Personagem | Videos | Nomeados / 35 | Pacote | Calibracao | Faltam nomeados |",
        "|---|---:|---:|---|---|---|",
    ]
    for pid,c in result["characters"].items():
        named=sum(bool(s["video"]) for s in c["states"].values())
        missing=[s["canonical"] for s in c["states"].values() if not s["video"]]
        lines.append(f"| {pid} | {c['video_count']} | {named}/35 | "
                     f"{'sim' if c['integrated'] else 'nao'} | "
                     f"{'sim' if c['scale_calibrated'] else 'nao'} | "
                     f"{', '.join(missing) or 'nenhum'} |")
    for pid,c in result["characters"].items():
        lines.extend(["", f"## {pid}", "", f"Drive: {c['drive_url']}", ""])
        issues={s:d for s,d in c["states"].items() if d["warnings"]}
        if issues:
            lines.extend(["| Estado | Video no Drive | Atlas no jogo | Avisos |",
                          "|---|---|---|---|"])
            for state,d in issues.items():
                lines.append("| "+ " | ".join([
                    state,d["video"] or "AUSENTE",d["atlas"] or "NAO INTEGRADO",
                    ", ".join(d["warnings"])])+" |")
        else:
            lines.append("Nenhuma divergencia nominal encontrada.")
        if c["unclassified"]:
            lines.append("\n**Videos de nome nao classificado:** " + ", ".join(c["unclassified"]))
        if c["ambiguous_names"]:
            lines.append("\n**Nomes ambiguos:** " + str(c["ambiguous_names"]))
        if c["missing_local_files"]:
            lines.append(f"\n**Videos locais faltantes:** {len(c['missing_local_files'])}")
    if result.get("possible_duplicate_samples"):
        lines.append("\n## Possiveis videos duplicados na amostra")
        for group in result["possible_duplicate_samples"]:
            lines.append("- " + " = ".join(group))
    return "\n".join(lines)+"\n"


def main(argv=None):
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--manifest", type=Path, default=MANIFEST)
    p.add_argument("--video-root", type=Path, help="Pasta local contendo p01/ ... p12/ com mp4")
    p.add_argument("--inspect-media", action="store_true", help="Executa ffprobe e amostragem via ffmpeg")
    p.add_argument("--output-dir", type=Path, default=ROOT / "android/app/build/sprite-audit")
    args=p.parse_args(argv)
    if args.inspect_media and args.video_root is None:
        p.error("--inspect-media exige --video-root; Drive fornece apenas metadados")
    snapshot=load_json(args.manifest)
    if not snapshot:
        p.error("manifest ausente: " + str(args.manifest))
    result=audit(snapshot,args.video_root,args.inspect_media)
    args.output_dir.mkdir(parents=True,exist_ok=True)
    report_json=args.output_dir/"audit.json"
    report_md=args.output_dir/"audit.md"
    report_json.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    report_md.write_text(make_markdown(result),encoding="utf-8")
    print(json.dumps(result["totals"],ensure_ascii=False,indent=2))
    print(f"Detalhes: {report_json} e {report_md}")
    return 0


if __name__=="__main__":
    raise SystemExit(main())
