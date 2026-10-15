#!/usr/bin/env python3
"""Package full-resolution review sprites separately from the mobile previews."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def export(processed, inventory, output):
    if output.exists() and any(output.iterdir()):
        raise ValueError('Use uma pasta de exportação nova ou vazia')
    expected = {c['id'] for c in json.loads(inventory.read_text())}
    markers = {p.parent.name: p for p in processed.glob('*/complete.json')}
    if set(markers) != expected:
        raise ValueError('Coleção incompleta')
    output.mkdir(parents=True, exist_ok=True)
    results = []
    for cid, marker in sorted(markers.items()):
        summary = json.loads(marker.read_text())
        if not summary['videos']:
            continue
        folder = marker.parent
        manifest = json.loads((folder/'assets/manifest.json').read_text())
        pngs = [folder/f['normalized_png'] for c in manifest['clips']
                if c['status'] != 'rejected' for f in c['frames']]
        if len(pngs) != len(set(pngs)) or not all(p.is_file() for p in pngs):
            raise ValueError(f'PNGs ausentes ou duplicados: {cid}')
        files = pngs + [marker] + sorted(folder.glob('*-metadata.json'))
        files += [folder/'assets'/name for name in
                  ('manifest.json', 'project.snapshot.json', 'calibration-report.json', 'calibration-comparison.png')]
        files += sorted((folder/'assets/masters').glob('*'))
        archive = output/f'sprites-{cid}.zip'
        with zipfile.ZipFile(archive, 'x', compression=zipfile.ZIP_DEFLATED, compresslevel=1) as z:
            for path in files:
                z.write(path, str(Path(cid)/path.relative_to(folder)))
            z.writestr(cid+'/LEIA-ME.txt',
                'PNGs integrais para revisão, sem redução do visualizador.\n'
                'assets/manifest.json registra normalized_png, PTS, duração, escala e matriz de cada frame.\n'
                'normalized_png é relativo à pasta deste personagem. Texturas WebP de prévia e vídeos originais não estão neste ZIP.\n'
                'Movimentos rejected não têm PNG normalizado: o motivo está no manifesto.\n'
                'Andar/dash calibrados usam in_place_x. A posição horizontal fica a cargo do jogo; não reaplique a trajetória removida.\n'
                'Saltos conservam o deslocamento vertical da fonte e precisam de integração com a física do jogo.\n'
                'Cortes e deformações da fonte continuam sinalizados. Esta exportação não aprova a arte para o jogo.\n')
        with zipfile.ZipFile(archive) as z:
            assert z.testzip() is None
            assert sum('/normalized/' in n and n.endswith('.png') for n in z.namelist()) == len(pngs)
        checksum = hashlib.sha256()
        with archive.open('rb') as stream:
            while block := stream.read(8*1024**2):
                checksum.update(block)
        sha = checksum.hexdigest()
        results.append(dict(character=cid, file=archive.name, bytes=archive.stat().st_size,
                            sha256=sha, normalized_frames=len(pngs)))
        print(json.dumps(results[-1]), flush=True)
    (output/'sprite-archives.json').write_text(json.dumps(results, indent=2, ensure_ascii=False)+'\n')
    (output/'sprite-archives.sha256').write_text(''.join(f'{r["sha256"]}  {r["file"]}\n' for r in results))


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--processed', type=Path, required=True)
    p.add_argument('--inventory', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    export(a.processed, a.inventory, a.output)
