"""Prova de conceito: padronização automática do tamanho do personagem a partir dos vídeos.

Por frame: MediaPipe Pose -> tamanho absoluto do corpo (pixels por metro do corpo 3D estimado).
Pares de frames: SIFT + RANSAC (similaridade) -> zoom relativo preciso dentro de trechos contínuos.
Os dois são fundidos em um só ajuste de mínimos quadrados em escala logarítmica; cortes do vídeo
separam os trechos. Depois: X no lugar pelo quadril, chão pelo tornozelo mais baixo, PNGs
normalizados, seleção automática do ciclo de corrida por autossimilaridade da silhueta e relatório.

Uso:
  python autoscale/standardize.py --sources /originais/p10 --master inicio.png --clips idle dash \
      --model models/pose_landmarker_full.task --out /saida/p10 [--protect-green]
"""
import sys, cv2, json, time, pathlib, argparse, subprocess
import numpy as np
from scipy.signal import medfilt, savgol_filter
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from sprite_pipeline.imaging import image_rgba
import mediapipe as mp
from mediapipe.tasks import python as mpt
from mediapipe.tasks.python import vision

MODEL_URL = 'https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task'
I = dict(nose=0, leye=2, reye=5, lear=7, rear=8, ml=9, mr=10, lsh=11, rsh=12, lel=13, rel=14, lwr=15, rwr=16,
         lhip=23, rhip=24, lkn=25, rkn=26, lan=27, ran=28)
BONES = [('lsh','rsh'),('lsh','lhip'),('rsh','rhip'),('lhip','rhip'),('lsh','lel'),('rsh','rel'),('lel','lwr'),('rel','rwr'),
         ('lhip','lkn'),('rhip','rkn'),('lkn','lan'),('rkn','ran'),('nose','lsh'),('nose','rsh'),('lear','rear'),('leye','reye')]

# ---------------------------------------------------------------- detection
def detector(model_path):
    if not pathlib.Path(model_path).exists():
        raise SystemExit(f'Modelo não encontrado em {model_path}. Baixe de {MODEL_URL}')
    o = vision.PoseLandmarkerOptions(base_options=mpt.BaseOptions(model_asset_path=str(model_path)),
                                     running_mode=vision.RunningMode.IMAGE, num_poses=1,
                                     min_pose_detection_confidence=.3, min_pose_presence_confidence=.3)
    return vision.PoseLandmarker.create_from_options(o)

def analyse_frame(lm, img, background):
    rgba = image_rgba(img, background if img.shape[2] == 3 else None)
    alpha = rgba[:, :, 3]; a = (alpha/255.)[:, :, None]
    comp = (rgba[:, :, :3]*a + 128*(1-a)).astype(np.uint8)
    res = lm.detect(mp.Image(image_format=mp.ImageFormat.SRGB, data=np.ascontiguousarray(comp)))
    h, w = alpha.shape
    pose = None
    if res.pose_landmarks:
        pose = {'px': np.array([[p.x*w, p.y*h] for p in res.pose_landmarks[0]]),
                'world': np.array([[p.x, p.y, p.z] for p in res.pose_world_landmarks[0]]),
                'vis': np.array([p.visibility for p in res.pose_landmarks[0]])}
    return rgba, cv2.cvtColor(comp, cv2.COLOR_RGB2GRAY), pose

def px_per_metre(pose):
    """Median over bones of 2D length / estimated 3D length: size of the body in the image."""
    r = []
    for a, b in BONES:
        d2 = np.linalg.norm(pose['px'][I[a]]-pose['px'][I[b]]); d3 = np.linalg.norm(pose['world'][I[a]]-pose['world'][I[b]])
        if d3 > 1e-6: r.append(d2/d3)
    return float(np.median(r))

# ---------------------------------------------------------------- pair registration
def feature_mask(alpha, pose):
    m = cv2.erode((alpha > 128).astype(np.uint8)*255, np.ones((5, 5), np.uint8))
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, np.ones((9, 1), np.uint8))      # drop thin horizontal speed lines
    if pose is not None:                                                    # head + torso only
        p = pose['px']; sh = (p[I['lsh']]+p[I['rsh']])/2
        top = p[I['nose']][1]-1.3*abs(sh[1]-p[I['nose']][1])-20
        bot = max(p[I['lhip']][1], p[I['rhip']][1])+10
        xs = [p[I[k]][0] for k in ('nose', 'lsh', 'rsh', 'lhip', 'rhip')]
        region = np.zeros_like(m); cv2.rectangle(region, (int(min(xs)-60), int(top)), (int(max(xs)+60), int(bot)), 255, -1)
        m = cv2.bitwise_and(m, region)
    return m

def register(matcher, fa, fb):
    (ka, da), (kb, db) = fa, fb
    if da is None or db is None or len(ka) < 8 or len(kb) < 8: return None
    good = [x[0] for x in matcher.knnMatch(da, db, k=2) if len(x) == 2 and x[0].distance < .75*x[1].distance]
    if len(good) < 8: return None
    A = np.float32([ka[g.queryIdx].pt for g in good]); B = np.float32([kb[g.trainIdx].pt for g in good])
    M, inl = cv2.estimateAffinePartial2D(A, B, method=cv2.RANSAC, ransacReprojThreshold=2.5, maxIters=5000, confidence=.995)
    if M is None or inl is None or inl.sum() < 8: return None
    s = float(np.hypot(M[0, 0], M[0, 1]))
    return (s, int(inl.sum())) if s > 0 else None

def body_mask(alpha, pose):
    """Silhouette without thin speed lines, limited to the head+torso box when a pose exists."""
    return feature_mask(alpha, pose) > 0

def silhouette_iou(a, b, pa, pb):
    """Overlap of two body silhouettes after aligning them on the hips (translation only)."""
    ma, mb = body_mask(a, pa), body_mask(b, pb)
    if not ma.any() or not mb.any(): return 0.
    if pa is not None and pb is not None:
        ca = (pa['px'][I['lhip']]+pa['px'][I['rhip']])/2; cb = (pb['px'][I['lhip']]+pb['px'][I['rhip']])/2
        dx, dy = np.rint(ca-cb).astype(int)
    else:
        ca = np.array(np.nonzero(ma)).mean(axis=1); cb = np.array(np.nonzero(mb)).mean(axis=1); dy, dx = np.rint(ca-cb).astype(int)
    mb = np.roll(np.roll(mb, dy, axis=0), dx, axis=1)
    return float((ma & mb).sum()/max((ma | mb).sum(), 1))

# ---------------------------------------------------------------- fusion
def fuse(n, absolute, pairs, cuts, w_abs=1.0):
    """log L_f: pairs give shape inside each shot, absolute px/m gives the level."""
    rows, rhs = [], []
    seg = np.cumsum([0]+[1 if c else 0 for c in cuts[:-1]]) if n > 1 else np.zeros(1, int)
    for i, j, s, inl in pairs:
        if seg[i] != seg[j]: continue
        w = 4.0*min(inl, 60)/60
        r = np.zeros(n); r[i] = -w; r[j] = w; rows.append(r); rhs.append(w*np.log(s))
    for f in range(n):
        if np.isfinite(absolute[f]):
            r = np.zeros(n); r[f] = w_abs; rows.append(r); rhs.append(w_abs*np.log(absolute[f]))
    L, *_ = np.linalg.lstsq(np.array(rows), np.array(rhs), rcond=None)
    return np.exp(L), seg

def medfilt_segments(v, seg, k=5):
    out = v.astype(float).copy()
    for s in np.unique(seg):
        idx = np.nonzero(seg == s)[0]
        if len(idx) >= k: out[idx] = medfilt(v[idx], k)
    return out

def smooth_segments(v, seg, window=9, poly=2):
    out = v.astype(float).copy()
    for s in np.unique(seg):
        idx = np.nonzero(seg == s)[0]
        if len(idx) >= window: out[idx] = savgol_filter(v[idx], window, poly)
        elif len(idx) >= 3: out[idx] = medfilt(v[idx], 3)
    return out

# ---------------------------------------------------------------- main per clip
def process_clip(lm, frames_dir, background, target_ppm, log, master_height=None):
    files = sorted(frames_dir.glob('*.png')); n = len(files)
    sift = cv2.SIFT_create(nfeatures=3000, contrastThreshold=.02); matcher = cv2.BFMatcher(cv2.NORM_L2)
    rgbas, poses, feats, absolute = [], [], [], np.full(n, np.nan)
    t = time.time()
    for k, f in enumerate(files):
        rgba, grey, pose = analyse_frame(lm, cv2.imread(str(f)), background)
        rgbas.append(rgba); poses.append(pose)
        feats.append(sift.detectAndCompute(grey, feature_mask(rgba[:, :, 3], pose)))
        if pose is not None: absolute[k] = px_per_metre(pose)
    log(f'  analysed {n} frames in {time.time()-t:.1f}s, pose missing in {int(np.isnan(absolute).sum())}')
    # fill missing absolute by interpolation
    ok = np.isfinite(absolute)
    absolute = np.interp(np.arange(n), np.nonzero(ok)[0], absolute[ok])
    # pairs and cut detection
    pairs, adjacent = [], [None]*(n-1)
    for i in range(n):
        for k in (1, 2, 3, 4, 6, 8):
            j = i+k
            if j < n:
                r = register(matcher, feats[i], feats[j])
                if r: pairs.append((i, j, r[0], r[1]))
                if k == 1: adjacent[i] = r
    cuts, sil_iou = [False]*n, []
    for i in range(n-1):
        iou = silhouette_iou(rgbas[i][:, :, 3], rgbas[i+1][:, :, 3], poses[i], poses[i+1]); sil_iou.append(iou)
        r = adjacent[i]; jump = abs(np.log(absolute[i+1]/absolute[i]))
        if iou < .45 or (jump > .15 and iou < .50):          # hard cut in the source
            cuts[i] = True
    size, seg = fuse(n, absolute, pairs, cuts)         # px per metre, fused
    size = np.exp(smooth_segments(medfilt_segments(np.log(size), seg, 5), seg, 9))
    scale = target_ppm/size                              # output scale per frame
    # anchors
    hipx = np.array([(p['px'][I['lhip']][0]+p['px'][I['rhip']][0])/2 if p else np.nan for p in poses])
    ankle = np.array([max(p['px'][I['lan']][1], p['px'][I['ran']][1]) if p else np.nan for p in poses])
    for arr in (hipx, ankle):
        ok = np.isfinite(arr); arr[:] = np.interp(np.arange(n), np.nonzero(ok)[0], arr[ok])
    hipx_s = smooth_segments(hipx, seg, 9)
    ground = np.array([np.max(ankle[max(0, i-10):i+11]) for i in range(n)])
    ground_s = smooth_segments(ground, seg, 21)
    ys0 = np.nonzero((rgbas[0][:, :, 3] > 32).any(axis=1))[0]
    before_scale = (master_height/(ys0[-1]-ys0[0])) if master_height else scale[0]   # what calibrate() does today
    return {'n': n, 'rgba': rgbas, 'poses': poses, 'absolute_ppm': absolute, 'fused_ppm': size, 'scale': scale, 'before_scale': float(before_scale),
            'cuts': [i for i, c in enumerate(cuts) if c], 'seg': seg, 'sil_iou': sil_iou+[None], 'hipx': hipx_s, 'ground': ground_s,
            'hipx_raw': hipx, 'pairs': len(pairs), 'adjacent': adjacent}

def render(clip, root, canvas):
    """Uniform scale about the origin, hips to root x, ground to root y. Premultiplied alpha."""
    out = []
    W, H = canvas; rx, ry = root
    for k in range(clip['n']):
        s = clip['scale'][k]; rgba = clip['rgba'][k]
        tx = rx - clip['hipx'][k]*s; ty = ry - clip['ground'][k]*s
        M = np.array([[s, 0, tx], [0, s, ty]], np.float32)
        pm = rgba.astype(np.float32); pm[:, :, :3] *= pm[:, :, 3:4]/255.
        warped = cv2.warpAffine(pm, M, (W, H), flags=cv2.INTER_AREA if s < 1 else cv2.INTER_LINEAR, borderValue=(0, 0, 0, 0))
        a = warped[:, :, 3:4]; rgb = np.where(a > 0, warped[:, :, :3]*255./np.maximum(a, 1e-3), 0)
        out.append(np.dstack((rgb, warped[:, :, 3])).clip(0, 255).astype(np.uint8))
    return out

def find_cycle(masks, seg, hipx_raw, min_period=8, max_period=48):
    """Pick the locomotion shot (largest hip travel) and the period/start with the best seam."""
    small = [cv2.resize(m, (96, 96), interpolation=cv2.INTER_AREA) > 64 for m in masks]
    iou = lambda a, b: float((a & b).sum()/max((a | b).sum(), 1))
    adjacent = np.array([iou(small[i], small[i+1]) for i in range(len(small)-1)]+[1.])
    best = None
    for sg in np.unique(seg):
        idx = np.nonzero(seg == sg)[0]
        if len(idx) < min_period+min_period//2: continue
        for P in range(min_period, min(max_period, int(len(idx)/1.5))+1):
            for a in range(idx[0], idx[-1]-P-P//2+1):
                motion = 1-adjacent[a:a+P].mean()            # a still pose matches itself at any period
                if motion < .03: continue
                score = np.mean([iou(small[a+k], small[a+P+k]) for k in range(0, P//2+1, 2)])
                seam = iou(small[a], small[a+P])
                cand = (score+seam+2*motion, P, a, score, seam, motion, int(sg))
                if best is None or cand > best: best = cand
    if best is None: return None
    _, P, a, score, seam, motion, sg = best
    idx = np.nonzero(seg == sg)[0]
    return {'shot_frames': [int(idx[0]), int(idx[-1])], 'start': int(a), 'period': int(P), 'stop_exclusive': int(a+P),
            'periodicity_iou': round(float(score), 4), 'seam_iou': round(float(seam), 4), 'motion': round(float(motion), 4)}

def contact_strip(frames, idxs, height=220):
    tiles = []
    for i in idxs:
        f = frames[i]; a = f[:, :, 3:4]/255.
        bgr = (cv2.cvtColor(f[:, :, :3], cv2.COLOR_RGB2BGR)*a + np.array([60, 60, 60])*(1-a)).astype(np.uint8)
        t = cv2.resize(bgr, (int(bgr.shape[1]*height/bgr.shape[0]), height), interpolation=cv2.INTER_AREA)
        cv2.putText(t, str(i), (4, 16), cv2.FONT_HERSHEY_SIMPLEX, .5, (255, 255, 255), 1); tiles.append(t)
    return np.hstack(tiles)

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--sources', required=True, help='pasta com os vídeos originais e a imagem mestre do personagem')
    ap.add_argument('--master', required=True, help='nome da imagem mestre (Idle) dentro de --sources')
    ap.add_argument('--clips', nargs='+', default=['idle', 'dash'], help='nomes dos vídeos sem extensão')
    ap.add_argument('--model', default='models/pose_landmarker_full.task')
    ap.add_argument('--protect-green', action='store_true', help='preserva detalhes verdes interiores (P05, P10, P12)')
    ap.add_argument('--out', required=True)
    a = ap.parse_args()
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    sources = pathlib.Path(a.sources)
    log = lambda *x: print(*x, flush=True)
    background = {'mode': 'green'}
    if a.protect_green: background.update(protect_green_art=True, green_chroma_tolerance=.12, green_art_inset=2, green_art_enclosed_only=True)
    lm = detector(a.model)
    m = cv2.imread(str(sources/a.master), cv2.IMREAD_UNCHANGED)
    rgba, _, pose = analyse_frame(lm, m, background)
    if pose is None: raise SystemExit('master without pose')
    target_ppm = px_per_metre(pose)                      # every frame is scaled to the master's body size
    mh = rgba[:, :, 3] > 32; ys = np.nonzero(mh.any(axis=1))[0]
    log(f'master {a.master}: {target_ppm:.1f} px/m, silhouette height {ys[-1]-ys[0]} px')
    report = {'sources': str(sources), 'master': {'file': a.master, 'px_per_metre': target_ppm}, 'background': background, 'clips': {}}
    canvas, root = (1100, 800), (550, 740)
    results = {}
    for clip_name in a.clips:
        log(f'clip {clip_name}')
        frames_dir = out/'_frames'/clip_name
        if not frames_dir.exists():
            frames_dir.mkdir(parents=True)
            video = next(p for p in sources.iterdir() if p.stem == clip_name and p.suffix.lower() in ('.mp4', '.mov', '.webm', '.mkv'))
            subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(video), '-fps_mode', 'passthrough', str(frames_dir/'%03d.png')], check=True)
        clip = process_clip(lm, frames_dir, background, target_ppm, log, master_height=ys[-1]-ys[0])
        frames = render(clip, root, canvas)
        d = out/clip_name; d.mkdir(exist_ok=True)
        for k, f in enumerate(frames): cv2.imwrite(str(d/f'{k:03d}.png'), cv2.cvtColor(f, cv2.COLOR_RGBA2BGRA))
        masks = [f[:, :, 3] for f in frames]
        cycle = find_cycle(masks, clip['seg'], clip['hipx_raw']) if clip_name != 'idle' else None
        # QA: body size after correction, measured again from the pose, must be flat
        after = clip['absolute_ppm']*clip['scale']/target_ppm
        rep = {'frames': clip['n'], 'cuts_after_frame': clip['cuts'], 'pairs_used': clip['pairs'], 'current_pipeline_constant_scale': clip['before_scale'],
               'size_with_current_pipeline_minmax': [float((clip['absolute_ppm']*clip['before_scale']/target_ppm).min()), float((clip['absolute_ppm']*clip['before_scale']/target_ppm).max())],
               'scale_min_max': [float(clip['scale'].min()), float(clip['scale'].max())],
               'size_before_rel_to_master_minmax': [float((clip['absolute_ppm']/target_ppm).min()), float((clip['absolute_ppm']/target_ppm).max())],
               'size_after_rel_to_master_p5_p95': [float(np.quantile(after, .05)), float(np.quantile(after, .95))],
               'per_frame': [{'f': k, 'size_before': round(float(clip['absolute_ppm'][k]/target_ppm), 4), 'scale': round(float(clip['scale'][k]), 4),
                              'hip_x': round(float(clip['hipx'][k]), 1), 'ground_y': round(float(clip['ground'][k]), 1),
                              'fused_rel': round(float(clip['fused_ppm'][k]/target_ppm), 4), 'adj': clip['adjacent'][k] if k < clip['n']-1 else None, 'sil_iou': clip['sil_iou'][k]} for k in range(clip['n'])],
               'cycle': cycle}
        report['clips'][clip_name] = rep
        strip_idx = list(range(0, clip['n'], max(1, clip['n']//12)))
        cv2.imwrite(str(out/f'{clip_name}_after_strip.png'), contact_strip(frames, strip_idx))
        # "before": same anchors, but one constant scale (what a fixed per-clip scale gives)
        before = render({**clip, 'scale': np.full(clip['n'], clip['before_scale'])}, root, canvas)
        results[clip_name] = {'before': before, 'after': frames, 'cycle': cycle}
        cv2.imwrite(str(out/f'{clip_name}_before_strip.png'), contact_strip(before, strip_idx))
        if cycle:
            cyc = list(range(cycle['start'], cycle['stop_exclusive']))
            def tile(f, label):
                a_ = f[:, :, 3:4]/255.; bgr = (cv2.cvtColor(f[:, :, :3], cv2.COLOR_RGB2BGR)*a_ + np.array([50, 50, 50])*(1-a_)).astype(np.uint8)
                t = bgr[root[1]-520:root[1]+40, root[0]-260:root[0]+260]
                cv2.line(t, (0, 520), (520, 520), (90, 90, 90), 1); cv2.putText(t, label, (6, 24), cv2.FONT_HERSHEY_SIMPLEX, .7, (255, 255, 255), 2)
                return t
            pick = [0]+cyc[::2]
            rows = [np.hstack([tile(before[i], ('idle ' if k == 0 else '')+str(i)) for k, i in enumerate(pick)]),
                    np.hstack([tile(frames[i], ('idle ' if k == 0 else '')+str(i)) for k, i in enumerate(pick)])]
            fig = np.vstack(rows); cv2.putText(fig, 'ANTES: escala fixa por clip', (6, 50), cv2.FONT_HERSHEY_SIMPLEX, .8, (80, 200, 255), 2)
            cv2.putText(fig, 'DEPOIS: escala por frame (automatica)', (6, 560+50), cv2.FONT_HERSHEY_SIMPLEX, .8, (80, 255, 120), 2)
            cv2.imwrite(str(out/f'{clip_name}_comparacao.png'), fig)
            cv2.imwrite(str(out/f'{clip_name}_cycle_strip.png'), contact_strip(frames, cyc, 180))
            from PIL import Image
            def crop(f):
                a_ = f[:, :, 3:4]/255.; bgr = (f[:, :, :3]*a_ + np.array([60, 60, 60])*(1-a_)).astype(np.uint8)
                return bgr[root[1]-560:root[1]+40, root[0]-300:root[0]+300]
            gif = [Image.fromarray(np.hstack((crop(before[i]), crop(frames[i])))) for i in cyc]
            gif[0].save(str(out/f'{clip_name}_cycle_before_after.gif'), save_all=True, append_images=gif[1:], duration=1000/60*1, loop=0)
            # the full clip, before/after
            gif = [Image.fromarray(np.hstack((crop(before[i]), crop(frames[i])))).resize((600, 300)) for i in range(clip['n'])]
            gif[0].save(str(out/f'{clip_name}_full_before_after.gif'), save_all=True, append_images=gif[1:], duration=1000/60*1, loop=0)
        log(f'  size before: {rep["size_before_rel_to_master_minmax"]}  after p5-p95: {rep["size_after_rel_to_master_p5_p95"]}  cuts: {clip["cuts"]}  cycle: {cycle}')
    (out/'report.json').write_text(json.dumps(report, indent=1))
    # one figure per character: idle clip, then the dash cycle, before (current pipeline) vs after
    def tile(f, label):
        a_ = f[:, :, 3:4]/255.; bgr = (cv2.cvtColor(f[:, :, :3], cv2.COLOR_RGB2BGR)*a_ + np.array([50, 50, 50])*(1-a_)).astype(np.uint8)
        t = bgr[root[1]-560:root[1]+40, root[0]-240:root[0]+240].copy()
        cv2.line(t, (0, 560), (480, 560), (90, 90, 90), 1); cv2.putText(t, label, (6, 24), cv2.FONT_HERSHEY_SIMPLEX, .7, (255, 255, 255), 2)
        return t
    picks = []
    if 'idle' in results: picks += [('idle', i) for i in (0, 48)]
    for name, r in results.items():
        if r['cycle']: picks += [(name, i) for i in range(r['cycle']['start'], r['cycle']['stop_exclusive'], 2)]
    rows = [np.hstack([tile(results[c][k][i], f'{c} {i}') for c, i in picks]) for k in ('before', 'after')]
    fig = np.vstack(rows)
    cv2.putText(fig, 'ANTES: pipeline atual (escala fixa por clip, altura da silhueta)', (6, 50), cv2.FONT_HERSHEY_SIMPLEX, .8, (80, 200, 255), 2)
    cv2.putText(fig, 'DEPOIS: escala por frame, automatica, alinhada ao mestre', (6, 600+50), cv2.FONT_HERSHEY_SIMPLEX, .8, (80, 255, 120), 2)
    cv2.imwrite(str(out/'comparacao.jpg'), fig, [cv2.IMWRITE_JPEG_QUALITY, 85])
    if 'idle' in results and 'dash' in results and results['dash']['cycle']:
        from PIL import Image
        cyc = results['dash']['cycle']; seq = [('idle', i) for i in range(0, 48)] + [('dash', i) for i in range(cyc['start'], cyc['stop_exclusive'])]*3 + [('idle', i) for i in range(48, 97)]
        def crop(f):
            a_ = f[:, :, 3:4]/255.; bgr = (f[:, :, :3]*a_ + np.array([50, 50, 50])*(1-a_)).astype(np.uint8)
            return bgr[root[1]-560:root[1]+40, root[0]-240:root[0]+240]
        gif = [Image.fromarray(np.hstack((crop(results[c]['before'][i]), crop(results[c]['after'][i])))).resize((480, 300)) for c, i in seq]
        gif[0].save(str(out/'sequencia_idle_dash_idle.gif'), save_all=True, append_images=gif[1:], duration=1000/30, loop=0)

if __name__ == '__main__':
    main()
