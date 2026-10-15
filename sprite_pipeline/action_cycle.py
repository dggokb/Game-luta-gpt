"""Explicit action-only extraction and local scale registration for review.

This is a bounded image registration of an annotated, approximately rigid patch,
not an anatomical detector. Idle, sprite bounds and particle effects never fit the
internal curve. A separate constant sets the action's size against other clips.
"""
import copy
import cv2
import numpy as np
from .errors import CalibrationError


def extract_cycle(metadata, start, stop):
    """Select [start, stop); retain source IDs/PTS and explicitly rebase playback."""
    if (type(start) is not int or type(stop) is not int
            or not 0 <= start < stop <= metadata['frame_count'] or stop-start < 3):
        raise CalibrationError('Intervalo de ação inválido')
    result=copy.deepcopy(metadata)
    frames=result['frames'][start:stop]
    origin=frames[0]['time_s']
    for i,frame in enumerate(frames):
        frame['source_frame_index']=start+i
        frame['source_time_s']=frame['time_s']
        frame['index']=i
        frame['time_s']-=origin
    result.update(frames=frames,frame_count=len(frames),
                  duration_s=frames[-1]['time_s']+frames[-1]['duration_s'],
                  source_selection={'start_inclusive':start,'stop_exclusive':stop,
                      'original_frame_count':metadata['frame_count'],
                      'playback_origin_source_time_s':origin,'source_pts_preserved':True,
                      'playback_times_rebased':True,'frames_synthesized':False})
    return result


def register_patch(images, reference, polygon, *, minimum_correlation=.8):
    """Fit one scale/rotation/translation per source frame to the action patch.

Rotation is a nuisance parameter for measuring size; the exported sprite is only
uniformly resized and translated. Low-confidence matches fail rather than using
Idle or the full silhouette as a fallback.
    """
    from scipy.optimize import minimize
    if not 0 <= reference < len(images):
        raise CalibrationError('Referência interna fora da ação')
    gray=[cv2.cvtColor(im[:,:,:3],cv2.COLOR_BGR2GRAY).astype(np.float32)/255 for im in images]
    shape=gray[reference].shape
    points=np.asarray(polygon,dtype=float)
    if (points.ndim!=2 or points.shape[1]!=2 or len(points)<3 or not np.isfinite(points).all()
            or np.any(points<0) or np.any(points[:,0]>=shape[1]) or np.any(points[:,1]>=shape[0])):
        raise CalibrationError('Região de registro inválida')
    mask=np.zeros(shape,np.uint8);cv2.fillPoly(mask,[np.rint(points).astype(np.int32)],1)
    y,x=np.nonzero(mask)
    if len(x)<100:raise CalibrationError('Região pequena demais para registrar escala')
    samples=np.array([x,y]).T.astype(float);center=samples.mean(axis=0)
    template=gray[reference][y,x]
    if template.std()<.025:raise CalibrationError('Região sem contraste para registrar escala')
    template=(template-template.mean())/template.std()
    fitted=[]
    for i,im in enumerate(gray):
        if im.shape!=shape:raise CalibrationError('Resolução muda dentro da ação')
        def objective(v):
            scale,angle,dx,dy=v;c,s=np.cos(angle),np.sin(angle)
            q=(samples-center)@np.array([[c,s],[-s,c]])*scale+center+[dx,dy]
            values=cv2.remap(im,q[:,0].astype(np.float32).reshape(-1,1),
                             q[:,1].astype(np.float32).reshape(-1,1),cv2.INTER_LINEAR).ravel()
            values=(values-values.mean())/max(float(values.std()),.01)
            return float(np.mean((template-values)**2))
        if i==reference:
            v=np.array([1.,0.,0.,0.]);score=1.
        else:
            # Locate the patch before continuous fitting. A broad translation
            # search inside Powell can settle on unrelated local texture.
            left,top,right,bottom=int(x.min()),int(y.min()),int(x.max()+1),int(y.max()+1)
            sx,sy=max(0,left-25),max(0,top-25)
            search=im[sy:min(shape[0],bottom+25),sx:min(shape[1],right+25)]
            match=cv2.matchTemplate(search,gray[reference][top:bottom,left:right],
                cv2.TM_CCOEFF_NORMED,mask=mask[top:bottom,left:right])
            match=np.nan_to_num(match,nan=-1,posinf=-1,neginf=-1)
            _,_,_,location=cv2.minMaxLoc(match)
            dx,dy=location[0]+sx-left,location[1]+sy-top
            fit=minimize(objective,[1,0,dx,dy],method='Powell',
                bounds=[(.85,1.15),(-.12,.12),(dx-4,dx+4),(dy-4,dy+4)],
                options={'xtol':.0003,'ftol':.00005,'maxiter':80})
            v=fit.x;score=1-fit.fun/2
        if not np.isfinite(v).all() or score<minimum_correlation or not .86<v[0]<1.14:
            raise CalibrationError(f'Registro local incerto no frame da ação {i}: correlação {score:.3f}')
        fitted.append({'scale_to_action_reference':float(v[0]),'rotation_radians':float(v[1]),
            'patch_center_source_px':(center+v[2:]).tolist(),'correlation':float(score)})
    return fitted


def cycle_transforms(fitted, base_scale, global_factor, source_ground_y):
    """Small median-filtered local correction, then one constant global scale."""
    if (len(fitted)<3 or not np.isfinite([base_scale,global_factor,source_ground_y]).all()
            or base_scale<=0 or not .5<=global_factor<=1.5):
        raise CalibrationError('Escala geral ou ciclo inválido')
    local=np.array([f['scale_to_action_reference'] for f in fitted])
    centers=np.array([f['patch_center_source_px'] for f in fitted])
    if (not np.isfinite(local).all() or np.any(local<=0)
            or centers.shape!=(len(fitted),2) or not np.isfinite(centers).all()):
        raise CalibrationError('Registro do ciclo inválido')
    # This is a loop: use neighbours across the seam, never Idle at its ends.
    filtered=np.median(np.array([np.roll(local,1),local,np.roll(local,-1)]),axis=0)
    scale=base_scale*global_factor/filtered
    matrices=[[[float(s),0,float(-cx*s)],[0,float(s),float(-source_ground_y*s)]]
              for s,cx in zip(scale,centers[:,0])]
    return matrices,{'method':'action_patch_similarity_registration',
        'local_reference':'running_pose_only','global_factor':global_factor,
        'local_correction_factors':(1/filtered).tolist(),
        'local_filter':'circular_three_frame_median',
        'horizontal_anchor':'registered_face_patch_center','vertical_motion':'preserved',
        'uniform_scale':True,'fitted':fitted,
        'note':'Image-patch registration, not full-body anatomical approval. Rotation is measured but not applied.'}
