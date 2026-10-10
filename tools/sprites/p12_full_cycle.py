#!/usr/bin/env python3
"""P12 full cycle. Existing Drive videos are the required movement source."""
import argparse
import sys
from pathlib import Path

from p12_video_cycle import ROOT, prepare as video_prepare, verify as video_verify, read as read_json, write_json

RESULTS = ROOT / 'android/app/build/p12-full-cycle'


def require_existing_video_sources():
    """Additional origin requirement; mandatory pose/art gates are unchanged."""
    path = ROOT / 'art/keys/p12/registration.json'
    if not path.exists():
        raise SystemExit('P12: extract and review the existing animations/p12 Drive videos first; newly generated sheets are not authorized sources.')
    registration = read_json(path)
    if registration.get('sourcePolicy') != 'EXISTING_DRIVE_VIDEOS_ONLY':
        raise SystemExit('P12: rejected source policy; use the existing Drive video frames.')
    inventory = read_json(ROOT / 'tools/sprites/videos/p12-drive-inventory.json')
    ids = {f['id'] for f in inventory['files'] if f['mimeType'] == 'video/mp4'}
    states = read_json(ROOT / 'docs/art/p12-motion-contract.json')['animations']
    if set(registration.get('states', {})) != set(states):
        raise SystemExit('P12: reviewed video registration must cover all 42 states.')
    for state, spec in registration['states'].items():
        origin = spec.get('videoOrigin', {})
        if origin.get('driveId') not in ids or not origin.get('videoSha256') or not origin.get('selectedVideoFrames'):
            raise SystemExit(state + ': missing traceable original video/frame selection.')


def prepare(weights):
    require_existing_video_sources()
    video_prepare(weights)


def verify():
    require_existing_video_sources()
    video_verify()


def audit():
    sys.path.insert(0,str(ROOT/"tools/sprites"))
    import harmony
    import auditoria
    rows,issues=harmony.audit(only_characters={"p12"})
    detailed=auditoria.audit("p12")
    summary={
        "character":"p12",
        "harmony_sample_count":len(rows),
        "harmony_issues":issues,
        "visual_audit":detailed,
        "visual_errors":[x for x in detailed if x["level"]=="erro"],
        "quality_caveat":"Automated harmony and visual audit only; authored movement and transition evidence also required"
    }
    write_json(RESULTS/"audit.json",summary)
    if not rows:raise SystemExit("P12 harmony has no frames")
    # All genuine visual defects are written to the report and NOT magically 'fixed'.
    print("P12 AUDIT: "+str(len(rows))+" frame samples, "+str(len(issues))+
          " harmony findings, "+str(len(summary["visual_errors"]))+
          " detailed findings. Review report.",flush=True)
    if issues or summary["visual_errors"]:
        raise SystemExit("P12 ART REJECTED: harmony/visual defects are blocking errors. "
                         "Do not mark the character complete or publish its APK.")



if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=('prepare', 'verify', 'audit'))
    parser.add_argument('--weights', type=Path)
    args = parser.parse_args()
    if args.action == 'prepare':
        if args.weights is None:
            parser.error('prepare requires --weights')
        prepare(args.weights)
    elif args.action == 'verify':
        verify()
    else:
        audit()
