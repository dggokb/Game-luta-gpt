"""Prepare the 20 P01 WALK_FORWARD masters as a uniformly registered 2x source."""
import argparse, hashlib, json
from pathlib import Path
from PIL import Image

def prepare(frames, manifest, original, output):
    data = json.loads(manifest.read_text())
    if len(data['frames']) != 20 or data['summary']['frames_completed'] != 20:
        raise ValueError('Expected all 20 masters')
    if hashlib.sha256(original.read_bytes()).hexdigest() != data['summary']['source_atlas_sha256']:
        raise ValueError('Original atlas hash mismatch')
    source = Image.open(original)
    if source.mode != 'RGBA' or source.size != (2048,768):
        raise ValueError('Original grid or alpha changed')
    sheet = Image.new('RGBA',(4096,1536))
    for i, entry in enumerate(data['frames']):
        path = frames/f'frame_{i:03}.png'
        if entry['frame'] != i or hashlib.sha256(path.read_bytes()).hexdigest() != entry['output_sha256']:
            raise ValueError(f'Master hash mismatch: {path.name}')
        master = Image.open(path)
        if master.mode != 'RGBA' or master.size != (1024,1024):
            raise ValueError(f'Invalid master: {path.name}')
        src = source.crop((i%8*256,i//8*256,(i%8+1)*256,(i//8+1)*256))
        if master.getchannel('A').tobytes() != src.getchannel('A').resize((1024,1024),Image.Resampling.LANCZOS).tobytes():
            raise ValueError(f'Alpha mismatch: {path.name}')
        cell = master.convert('RGBa').resize((512,512),Image.Resampling.LANCZOS).convert('RGBA')
        cell.putalpha(src.getchannel('A').resize((512,512),Image.Resampling.LANCZOS))
        sheet.paste(cell,(i%8*512,i//8*512))
    output.parent.mkdir(parents=True,exist_ok=True)
    sheet.save(output)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('frames','manifest','original','output'):
        parser.add_argument('--'+name,required=True,type=Path)
    args = parser.parse_args()
    prepare(args.frames,args.manifest,args.original,args.output)
