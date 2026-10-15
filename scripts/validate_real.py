#!/usr/bin/env python3
"""Reproduce validation using existing footage, never generated poses/videos.

Downloads are intentionally outside this program. Supply local originals.
The only derivative is a geometric resampling of every frame of existing jab.
"""
import argparse
import json
from pathlib import Path
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from sprite_pipeline.extraction import digest, run
from sprite_pipeline.pipeline import process
from sprite_pipeline.exporting import write_json


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--inputs', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True, help='New validation directory')
    args = parser.parse_args()
    source, output = args.inputs.resolve(), args.output.resolve()
    required = ['inicio_centro.png','inicio_agachado.png','idle.mp4','L(jab).mp4',
                'dash.mp4','jump.mp4','crounch.mp4','l2(jab).mp4','dano_baixo.mp4']
    for name in required:
        if not (source/name).is_file():
            parser.error(f'Arquivo real ausente: {source/name}')
    if output.exists():
        parser.error('Use uma pasta de validação nova')
    hashes = {n: digest(source/n) for n in required}
    output.mkdir(parents=True)
    derived = output/'jab-half.nut'
    command = ['ffmpeg','-v','error','-i',str(source/'L(jab).mp4'),
               '-vf','scale=iw/2:ih/2:flags=lanczos','-fps_mode','passthrough',
               '-enc_time_base','demux','-c:v','ffv1','-level','3',str(derived)]
    run(command)
    clips = [
        {'name':'idle','video':'idle.mp4','role':'idle','detect_loop':True},
        {'name':'jab','video':'L(jab).mp4','role':'jab'},
        {'name':'dash','video':'dash.mp4','role':'dash','animation_type':'translation'},
        {'name':'jump','video':'jump.mp4','animation_type':'airborne'},
        # These two sources actually start standing; use their real Idle pose.
        {'name':'crouch','video':'crounch.mp4','animation_type':'crouched'},
        {'name':'crouch_jab','video':'l2(jab).mp4','animation_type':'crouched'},
        {'name':'crouch_small','video':'dano_baixo.mp4','reference_pose':'crouch','animation_type':'crouched'}]
    for c in clips:
        c['video'] = str(source/c['video'])
    clips.insert(2,{'name':'jab_half','video':str(derived),
                    'background':{'mode':'green','exclude_rectangles':[[0,0,32,25]]}})
    config = {'references': {'idle':str(source/'inicio_centro.png'),'crouch':str(source/'inicio_agachado.png')},
              'anatomical_segments':[{'name':'crown_to_chin_approximate_manual',
                  'idle':[[665,224],[686,310]],'crouch':[[663,354],[684,438]]}],
              'background':{'mode':'green','exclude_rectangles':[[0,0,64,50]]}, 'clips':clips}
    project = output/'project.json'
    write_json(project, config)
    result = process(project, output/'run')
    assert hashes == {n: digest(source/n) for n in required}, 'A fonte foi alterada!'
    write_json(output/'provenance.json', {'original_sha256':hashes, 'originals_unchanged':True,
               'derived_video':{'file':derived.name,'sha256':digest(derived),'command':command,
                   'note':'Controlled geometric resampling of an existing complete video; no poses or motion synthesized.'},
               'manual_landmarks':'Approximate crown/chin points, not a trained anatomical detector.',
               'source_repository_base':'6ec6b10e3d9d20dd09b0e8ef6a11043d727acf70'})
    print(json.dumps({'status':result['status'],'report':str(output/'run'/'index.html')},indent=2))


if __name__ == '__main__':
    main()
