#!/usr/bin/env python3
"""Restore full PNG archives from the verified delivery volumes."""
import argparse
import hashlib
import json
from pathlib import Path


def join(folder, manifest, output, characters=None):
    output.mkdir(parents=True, exist_ok=True)
    for asset in json.loads(manifest.read_text()):
        if characters and asset['character'] not in characters:
            continue
        name = asset['file']
        if Path(name).name != name:
            raise ValueError('Nome de arquivo inválido')
        target = output/name
        if target.exists():
            raise ValueError(f'Arquivo já existe: {target}')
        temporary = target.with_suffix('.zip.partial')
        if temporary.exists():
            raise ValueError(f'Arquivo parcial já existe: {temporary}')
        digest = hashlib.sha256()
        try:
            with temporary.open('xb') as out:
                for part in asset['volumes']:
                    if Path(part['name']).name != part['name']:
                        raise ValueError('Nome de volume inválido')
                    source = folder/part['name']
                    if source.stat().st_size != part['bytes']:
                        raise ValueError(f'Volume incompleto: {source.name}')
                    checksum = hashlib.sha256()
                    with source.open('rb') as src:
                        while block := src.read(8*1024**2):
                            checksum.update(block);digest.update(block);out.write(block)
                    if checksum.hexdigest() != part['sha256']:
                        raise ValueError(f'Hash incorreto: {source.name}')
            if digest.hexdigest() != asset['sha256']:
                raise ValueError(f'Hash final incorreto: {name}')
            temporary.rename(target)
        except Exception:
            temporary.unlink(missing_ok=True)
            raise
        print(target)


if __name__ == '__main__':
    p=argparse.ArgumentParser();p.add_argument('folder',type=Path)
    p.add_argument('--manifest',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    p.add_argument('--characters',nargs='*');a=p.parse_args();join(a.folder,a.manifest,a.output,a.characters)
