"""Compara a curva automática de tamanho com marcações manuais de eixo da cabeça (scale_audit).

Uso: python autoscale/compare_manual.py <report.json> <clip> <motion-landmarks.json>
"""
import sys, json, numpy as np
report = json.load(open(sys.argv[1]))['clips'][sys.argv[2]]
clips = json.load(open(sys.argv[3]))['clips']
manual = {c['frame']: float(np.linalg.norm(np.subtract(*c['segments']['head_axis'])))
          for c in clips[sys.argv[2]]['scale_audit']['checkpoints'] if 'head_axis' in c['segments']}
auto = {p['f']: p['fused_rel'] for p in report['per_frame']}
raw = {p['f']: p['size_before'] for p in report['per_frame']}
ref = manual[min(manual)]
errs = []
print('frame  manual  automático  (medida bruta)')
for f in sorted(manual):
    m = manual[f]/ref; a = auto[f]/auto[min(manual)]; errs.append(abs(a/m-1))
    print(f'{f:5d}  {m:6.3f}  {a:10.3f}  ({raw[f]/raw[min(manual)]:.3f})')
print(f'erro absoluto médio {np.mean(errs):.1%}, máximo {np.max(errs):.1%} (frame {sorted(manual)[int(np.argmax(errs))]})')
