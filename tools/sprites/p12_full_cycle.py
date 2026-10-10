#!/usr/bin/env python3
"""P12 full cycle. Render independent authored poses; never deform Idle."""
import argparse
import sys
from pathlib import Path

from p12_authored_cycle import ROOT, prepare, verify, read as read_json, write_json

RESULTS = ROOT / 'android/app/build/p12-full-cycle'


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
