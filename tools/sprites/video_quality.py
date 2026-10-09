#!/usr/bin/env python3
"""Video -> sprites temporal QA. Advisory by default: never silently alters approved art.

Usage:
  python tools/sprites/video_quality.py animation.mp4 --output android/app/build/qa/animation
  python tools/sprites/video_quality.py animation.mp4 --output ... --strict
Reports timing, loop seams, duplicate frames, silhouette/registration anomalies,
probable bad crops and a review contact sheet. Thresholds are heuristics, NOT
proof of correct anatomy, motion semantics or identical character identity.
"""
import argparse
import base64
import html
import io
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


def _rgba(frame):
    if frame.ndim != 3 or frame.shape[2] != 4:
        raise ValueError("Expected RGBA frame")
    return np.asarray(frame, dtype=np.uint8)


def _features(frame):
    rgba = _rgba(frame)
    a = rgba[:, :, 3] > 24
    ys, xs = np.nonzero(a)
    if not len(xs):
        return None
    left, right, top, bottom = int(xs.min()), int(xs.max()), int(ys.min()), int(ys.max())
    w, h = right - left + 1, bottom - top + 1
    y0 = top + int(h * .23)
    y1 = max(y0 + 1, top + int(h * .52))
    band = a[y0:y1]
    tx = np.nonzero(band)[1]
    torso = float(tx.mean()) if len(tx) else float(np.median(xs))
    hy1 = max(top + 1, top + int(h * .19))
    hy, hx = np.nonzero(a[top:hy1])
    head = float(np.ptp(hx) + 1) if len(hx) else 0.
    # Compare silhouettes at fixed resolution, not RGB: ignores harmless lighting changes.
    miniature = cv2.resize(a.astype(np.uint8), (64, 36), interpolation=cv2.INTER_AREA)
    edge = bool(a[0].any() or a[-1].any() or a[:, 0].any() or a[:, -1].any())
    return dict(box=[left, top, right + 1, bottom + 1], area=int(a.sum()), torso=torso,
                height=h, width=w, head=head, bottom=bottom, edge=edge,
                center=float((left + right) / 2), silhouette=miniature)


def _difference(a, b):
    return float(np.abs(a.astype(np.float32) - b.astype(np.float32)).mean())


def choose_frames(features, target=12, required=()):
    """Keep endpoints + mandatory contact/peak poses, then pick high-novelty frames."""
    n = len(features)
    if not n:
        return []
    target = max(2, min(n, target))
    valid = [i for i, f in enumerate(features) if f is not None]
    if not valid:
        return []
    selected = set(i for i in (valid[0], valid[-1], *required) if i in valid)
    extreme = max(valid, key=lambda i: features[i]['width'])
    selected.add(extreme)
    # Do not drop required frames even when they exceed target.
    while len(selected) < target:
        candidates = [i for i in valid if i not in selected]
        if not candidates:
            break
        def gain(i):
            return min(_difference(features[i]['silhouette'], features[k]['silhouette']) +
                       abs(features[i]['torso'] - features[k]['torso']) /
                       max(1., features[i]['width']) * 4.
                       for k in selected) + .03 * min(abs(i-k) for k in selected) / max(1, n)
        selected.add(max(candidates, key=gain))
    return sorted(selected)


def inspect(frames, fps=24., loop=False, target=12, expected_impact=None):
    """Offline geometry/timing check of already keyed frames (RGBA).
    Returns JSON-safe report and does not mutate the frames.
    """
    if not frames:
        raise ValueError("Video contains no frames")
    if not np.isfinite(fps) or fps <= 0:
        raise ValueError("Invalid video fps")
    metrics = [_features(f) for f in frames]
    flags = []
    def flag(level, code, indices, detail):
        if indices:
            flags.append(dict(severity=level, code=code, frames=sorted(set(int(i) for i in indices)),
                              detail=detail))
    missing = [i for i, m in enumerate(metrics) if m is None]
    flag("error", "empty_frame", missing, "Personagem nao detectado")
    edges = [i for i,m in enumerate(metrics) if m and m['edge']]
    flag("warning", "clip_edge", edges, "Silhueta toca a borda do video")
    valid = [i for i,m in enumerate(metrics) if m]
    duplicates, area_spikes, torso_spikes, head_spikes = [], [], [], []
    for i in range(1, len(metrics)):
        a, b = metrics[i-1], metrics[i]
        if a and b and _difference(a['silhouette'], b['silhouette']) < .006:
            duplicates.append(i)
    for i in range(1, len(metrics) - 1):
        a, b, c = metrics[i-1:i+2]
        if not a or not b or not c:
            continue
        # A spike must return toward its old value; an intentional punch isn't automatically bad.
        rel = max(a['area'], c['area'], 1)
        if abs(a['area']-c['area']) < .10 * rel and abs(b['area']-(a['area']+c['area'])/2) > .28 * rel:
            area_spikes.append(i)
        if abs(a['torso']-c['torso']) < 16 and abs(b['torso']-(a['torso']+c['torso'])/2) > 35:
            torso_spikes.append(i)
        if max(a['height'], c['height']) > 0 and abs(a['height']-c['height']) < .08*max(a['height'],c['height']):
            ref = max(1.,(a['head']+c['head'])/2)
            if abs(b['head']-ref) > .45*ref and abs(a['head']-c['head']) < .2*ref:
                head_spikes.append(i)
    flag("warning", "area_pop", area_spikes, "Mudanca isolada de area: revisar proporcao/efeito")
    flag("warning", "root_pop", torso_spikes, "Salto isolado de registro: revisar root")
    flag("warning", "head_pop", head_spikes, "Possivel alteracao do tamanho da cabeca")
    # Duplicates are informational: idle and held hit poses intentionally repeat.
    flag("info", "near_duplicate", duplicates, "Quadros semelhantes; comparar antes de remover")
    seam = None
    if loop and len(valid) >= 4:
        first, last = metrics[valid[0]], metrics[valid[-1]]
        seam_diff = _difference(first['silhouette'], last['silhouette'])
        adjacent = [_difference(metrics[i-1]['silhouette'], metrics[i]['silhouette'])
                    for i in range(1,len(metrics)) if metrics[i-1] and metrics[i]]
        typical = max(.005, float(np.median(adjacent)))
        # Velocity continuity: previous and next positions should not reverse abruptly.
        seam = dict(imageDelta=round(seam_diff, 5),
                    relativeToAdjacent=round(seam_diff / typical, 3),
                    torsoDelta=round(last['torso'] - first['torso'], 2))
        if seam_diff > max(.04, typical * 2.5):
            flag("warning", "loop_seam", [valid[0],valid[-1]], "Fim/inicio do loop nao fecham suavemente")
    if expected_impact is not None and not 0 <= expected_impact < len(frames):
        flag("error", "impact_out_of_range", [0], "Impacto fora dos quadros selecionados")
    picks = choose_frames(metrics, target, [] if expected_impact is None else [expected_impact])
    public = []
    for i,m in enumerate(metrics):
        if not m:
            public.append(dict(index=i, empty=True))
        else:
            public.append({k: (round(v,3) if isinstance(v,float) else v)
                           for k,v in m.items() if k != 'silhouette'} | {'index':i})
    return dict(schemaVersion=1, frameCount=len(frames), sourceFps=round(float(fps),3),
                durationSeconds=round(len(frames)/float(fps),3),
                inspectedFrames=public, flags=flags, loop=seam,
                suggestedFrames=picks, probableDuplicateCount=len(duplicates),
                status="review" if flags else "ok")


def _contact_sheet(frames, report, width=190):
    picks = report['suggestedFrames']
    cols = 4
    h = 155
    sheet = Image.new("RGB", (cols*width, ((len(picks)+cols-1)//cols)*h), "#242831")
    draw = ImageDraw.Draw(sheet)
    flagged = {f for e in report['flags'] if e['severity'] != 'info' for f in e['frames']}
    for pos,index in enumerate(picks):
        frame = Image.fromarray(_rgba(frames[index]), "RGBA")
        frame.thumbnail((width-18,h-29))
        x=(pos % cols)*width + (width-frame.width)//2
        y=(pos//cols)*h + 5
        sheet.paste(frame,(x,y),frame)
        label=f"frame {index}"+("  !" if index in flagged else "")
        draw.text(((pos%cols)*width+8,(pos//cols)*h+h-19),label,fill="#ffcc66" if index in flagged else "#eef1f5")
    return sheet


def write_report(report, frames, output):
    target=Path(output)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.with_suffix(".json").write_text(json.dumps(report,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    preview=_contact_sheet(frames,report)
    preview.save(target.with_suffix(".png"))
    img=io.BytesIO();preview.save(img,format="PNG")
    errors="".join("<tr><td>"+html.escape(e['severity'])+"</td><td>"+html.escape(e['code'])+
        "</td><td>"+html.escape(','.join(map(str,e['frames'])))+"</td><td>"+
        html.escape(e['detail'])+"</td></tr>" for e in report['flags'])
    page=("<!doctype html><html lang='pt-br'><meta charset='utf-8'>"
          "<title>Sprite Video QA</title><style>body{font:15px system-ui;background:#10141b;color:#eff4fa;"
          "max-width:900px;margin:35px auto}table{border-collapse:collapse;width:100%}"
          "td,th{border-bottom:1px solid #424b5b;padding:10px;text-align:left}img{max-width:100%}"
          "h1{font-size:24px}</style><h1>Video Sprite QA</h1><p>Revisao heuristica; conferir "
          "manualmente traco, dedos e semantica do movimento.</p><p>"+
          html.escape(str(report['frameCount']))+" quadros • "+html.escape(report['status'])+
          "</p><img alt='Folha de revisao' src='data:image/png;base64,"+
          base64.b64encode(img.getvalue()).decode()+"'><h2>Achados</h2><table><tr>"
          "<th>Tipo</th><th>Codigo</th><th>Quadros</th><th>Descricao</th></tr>"+
          errors+"</table></html>")
    target.with_suffix(".html").write_text(page,encoding="utf-8")


def read_video(path, max_frames=1800):
    cap=cv2.VideoCapture(str(path))
    if not cap.isOpened():
        raise ValueError("Nao foi possivel abrir o video")
    fps=float(cap.get(cv2.CAP_PROP_FPS))
    from video_para_sprite import key
    frames=[]
    try:
        while len(frames) < max_frames:
            ok, bgr=cap.read()
            if not ok:
                break
            frames.append(key(bgr))
    finally:
        cap.release()
    if not frames:
        raise ValueError("Video sem quadros")
    return frames, fps


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("video")
    p.add_argument("--output",required=True,help="prefixo de relatorio .json/.png/.html")
    p.add_argument("--loop",action="store_true")
    p.add_argument("--strict",action="store_true",help="reprova somente problemas classificados como error")
    p.add_argument("--frames",type=int,default=12)
    a=p.parse_args()
    frames,fps=read_video(a.video)
    report=inspect(frames,fps,loop=a.loop,target=a.frames)
    write_report(report,frames,a.output)
    print(f"{report['status']}: {len(report['flags'])} achados -> {a.output}.html")
    if a.strict and any(e['severity']=="error" for e in report['flags']):
        raise SystemExit(2)


if __name__=="__main__":
    main()
