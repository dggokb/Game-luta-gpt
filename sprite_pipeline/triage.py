"""Read-only structural triage and selective translation of existing sprites."""
import copy
import json
from pathlib import Path
import numpy as np
from .errors import CalibrationError
from .extraction import digest


def audit_metadata(processed):
    rows=[];snapshots={}
    for path in sorted(Path(processed).glob('*/assets/manifest.json')):
        data=json.loads(path.read_text());cid=path.parents[1].name;snapshots[cid]=digest(path)
        for clip in data['clips']:
            frames=clip['frames'];errors=[]
            if len(frames)!=clip['frame_count']:errors.append('frame_count')
            if any(b['pts']<=a['pts'] for a,b in zip(frames,frames[1:])):errors.append('timestamp_order')
            if any(f['duration_s']<=0 for f in frames):errors.append('duration')
            row={'character':cid,'clip':clip['name'],'source_name':clip['source_name'],
                'source_sha256':clip['source_sha256'],'frames':len(frames),'status':clip['status'],
                'metadata_errors':errors,'anatomical_validation':'not_inferred_from_metadata',
                'source_warning_frames':len(clip.get('frame_warnings',[]))}
            if clip['status']!='rejected':
                m=np.array([f['transform_source_to_output'] for f in frames]);s=m[:,0,0]
                if (not np.isfinite(m).all() or np.any(s<=0) or not np.array_equal(s,m[:,1,1])
                        or np.any(m[:,0,1]) or np.any(m[:,1,0])):errors.append('uniform_transform')
                boxes=np.array([f['bbox_output'] for f in frames]);w,h=data['canvas']
                if np.any(boxes[:,:2]<0) or np.any(boxes[:,2]>w) or np.any(boxes[:,3]>h):errors.append('output_bounds')
                row['recorded_scale_spread']=float(np.ptp(s)/np.median(s))
                if clip.get('root_motion'):
                    a=np.asarray(clip['root_motion']['anchor_x_source_px'])
                    if len(a)!=len(frames):errors.append('anchor_count')
                    else:
                        spread=float(np.ptp(a*s+m[:,0,2]));row['recorded_anchor_spread_px']=spread
                        if spread>1e-6:errors.append('recorded_anchor_not_fixed')
            rows.append(row)
    return {'clips':len(rows),'converted':sum(r['status']!='rejected' for r in rows),
        'rejected':sum(r['status']=='rejected' for r in rows),'metadata_error_count':sum(bool(r['metadata_errors']) for r in rows),
        'baseline_manifest_sha256':snapshots,'rows':rows,'full_frame_anatomical_validation':False}


def translate_registered_x(frames,anchors,target_x):
    if len(frames)!=len(anchors) or not frames or not np.isfinite(anchors).all() or not np.isfinite(target_x):
        raise CalibrationError('Trajetória horizontal inválida')
    result=[];deltas=[]
    for f,x in zip(frames,anchors):
        m=copy.deepcopy(f['transform_source_to_output'])
        dx=float(target_x-(x*m[0][0]+m[0][2]));m[0][2]+=dx
        result.append(m);deltas.append(dx)
    return result,deltas


def required_horizontal_padding(frames,deltas,width):
    if len(frames)!=len(deltas) or not frames:raise CalibrationError('Limites de translação inválidos')
    left=min(f['bbox_output'][0]+d for f,d in zip(frames,deltas))
    right=max(f['bbox_output'][2]+d for f,d in zip(frames,deltas))
    return max(0,int(np.ceil(-left))+4) if left<0 else 0, max(0,int(np.ceil(right-width))+4) if right>width else 0
