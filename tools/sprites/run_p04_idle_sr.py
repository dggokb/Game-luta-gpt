"""Reproduce approved P04 idle SR: exact 68 frames, original alpha, timing unchanged."""
import argparse
import hashlib
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "art/redraws/p04_hd/idle_sr_4x"
ORIGINAL = ROOT / "art/sprites/source/p04_idle_video_normalized.png"
DEST = ROOT / "art/sprites/source/p04_idle_sr_2x.png"
SOURCE_HASH = "fe2bdeeebe35655c464dcde1658a752ad81de96fa1f6982a02e3f44bd80c1444"
WEIGHTS_HASH = "b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d"

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def call(*args):
    subprocess.run([sys.executable, *map(str,args)], cwd=ROOT, check=True)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--weights", type=Path, required=True)
    args=parser.parse_args()
    if sha(ORIGINAL)!=SOURCE_HASH: raise ValueError("P04 source hash changed")
    if sha(args.weights)!=WEIGHTS_HASH: raise ValueError("Real-ESRGAN weights hash mismatch")
    with tempfile.TemporaryDirectory(prefix="p04-idle-sr-") as directory:
        work=Path(directory)
        frames=work/"originals"
        frames.mkdir()
        with Image.open(ORIGINAL) as original:
            if original.mode!="RGBA" or original.size!=(2112,1792):
                raise ValueError("Unexpected P04 source geometry or alpha")
            for i in range(68):
                original.crop((i%11*192,i//11*256,(i%11+1)*192,(i//11+1)*256)).save(frames/f"frame_{i:03}.png")
        sr=work/"sr"
        call(ART/"upscale_p04_idle_sr.py","--input",frames,"--output",sr,"--weights",args.weights,"--limit",68)
        manifest=json.loads((sr/"manifest.json").read_text())
        if manifest["summary"]["frames_completed"]!=68 or len(manifest["frames"])!=68:
            raise ValueError("P04 incomplete SR")
        if manifest["summary"]["model_sha256"]!=WEIGHTS_HASH:
            raise ValueError("Wrong model was used")
        call(ART/"prepare_p04_idle_sr.py","--frames",sr/"frames","--manifest",sr/"manifest.json",
             "--original",ORIGINAL,"--output",DEST)
        for i in range(68):
            shutil.copy2(sr/"frames"/f"frame_{i:03}.png",ART/f"frame_{i:03}.png")
        shutil.copy2(sr/"manifest.json",ART/"manifest.json")
    call(ROOT/"tools/sprites/build_characters.py","--write")
    call(ROOT/"tools/sprites/build_characters.py","--check")
    call("-m","unittest","discover","-s","tools/sprites/tests","-v")
    shutil.copy2(DEST,ART/"p04_idle_sr_2x.png")
    shutil.copy2(ROOT/"android/app/src/main/res/drawable-nodpi/p04_idle.png",ART/"p04_idle_runtime_2x.png")
    print("P04 68/68 frames generated; 2x runtime atlas and sprite tests PASS.")

if __name__=="__main__":
    main()
