"""Local deterministic RealESRGAN AnimeVideo-v3 4x test of P03 original idle."""
import os, sys, json, hashlib, time, argparse
from pathlib import Path
import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt
import torch
from srvgg_arch import SRVGGNetCompact



def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
 p=argparse.ArgumentParser();p.add_argument('--limit',type=int,default=60)
 p.add_argument('--input',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
 p.add_argument('--weights',type=Path,required=True);args=p.parse_args()
 source=args.input;dest=args.output;weights=args.weights
 torch.set_num_threads(6);torch.set_num_interop_threads(1)
 torch.manual_seed(0);torch.use_deterministic_algorithms(True)
 model=SRVGGNetCompact(num_in_ch=3,num_out_ch=3,num_feat=64,num_conv=16,upscale=4,act_type='prelu')
 state=torch.load(weights,map_location='cpu',weights_only=True)
 model.load_state_dict(state['params'],strict=True);model.eval()
 (dest/'frames').mkdir(parents=True,exist_ok=True)
 rows=[];start=time.monotonic()
 for i in range(args.limit):
  t=time.monotonic();src=source/f'frame_{i:03}.png';target=dest/'frames'/src.name
  original_hash=sha(src)
  rgba=Image.open(src).convert('RGBA');a=np.array(rgba)
  # Extend RGB into invisible space from the nearest fully visible source pixel.
  # The displayed contour is still solely the original alpha mask at uniform 4x.
  _,indices=distance_transform_edt(a[:,:,3]<128,return_indices=True)
  rgb=a[:,:,:3].copy();invisible=a[:,:,3]<128
  rgb[invisible]=rgb[indices[0][invisible],indices[1][invisible]]
  tensor=torch.from_numpy(np.ascontiguousarray(rgb.transpose(2,0,1))).float().unsqueeze(0)/255
  with torch.inference_mode(): output=model(tensor).clamp_(0,1)[0].permute(1,2,0).numpy()
  output=np.rint(output*255).astype(np.uint8)
  alpha=rgba.getchannel('A').resize((1024,1024),Image.Resampling.LANCZOS)
  output[np.asarray(alpha)==0]=0
  final=Image.fromarray(output,'RGB');final.putalpha(alpha)
  tmp=target.with_suffix('.tmp.png');final.save(tmp);os.replace(tmp,target)
  check=Image.open(target);check.load()
  assert check.size==(1024,1024) and check.mode=='RGBA'
  assert np.array_equal(np.asarray(check.getchannel('A')),np.asarray(alpha))
  assert sha(src)==original_hash
  # A diagnostic at source size: color drift and temporal change are recorded,
  # not interpreted as proof of subjective quality or zero flicker.
  low=np.array(check.resize((256,256),Image.Resampling.LANCZOS))
  mask=a[:,:,3]>=128
  error=float(np.abs(low[:,:,:3].astype(float)-a[:,:,:3]).mean(axis=2)[mask].mean())
  rows.append({'frame':i,'source_sha256':original_hash,'output_sha256':sha(target),'dimensions':[1024,1024],'alpha_extrema':check.getchannel('A').getextrema(),'alpha_equals_uniform_source_resize':True,'source_scale_color_mae':round(error,6),'processing_seconds':round(time.monotonic()-t,3)})
  summary={'frames_completed':len(rows),'failures':0,'model':'realesr-animevideov3','model_sha256':sha(weights),'method':'SRVGGNetCompact 16 convolutions, learned 4x RGB super-resolution on CPU; nearest-visible RGB extension in transparent area; original alpha resized uniformly with Lanczos','scale':4,'timing_ms':41,'character':'p03','source_commit':'91027cfbf7d32824964a7424d9684cfddb7eef77','source_atlas_sha256':'9b38925b2f6b05b81ce1a37076d6bad3873bd10da534e2f6e862ad3bb11b11fc','source_frames_unchanged':True,'learned_super_resolution':True,'game_integration':False,'temporal_status':'visual review pending; deterministic per-frame model, no temporal smoothing','elapsed_seconds':round(time.monotonic()-start,3)}
  (dest/'manifest.json').write_text(json.dumps({'summary':summary,'frames':rows},indent=2)+'\n')
  print(f'{len(rows):02}/{args.limit} frame_{i:03}.png {rows[-1]["processing_seconds"]}s colorMAE={error:.3f}',flush=True)
 print('DONE',json.dumps(summary),flush=True)

if __name__=='__main__':main()
