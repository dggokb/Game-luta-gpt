#!/usr/bin/env python3
"""Measure source motion without mistaking source coverage for art approval.

Height and aspect signals can disprove fake crouches/falls. They cannot prove
that a punch is anatomically correct or that a gait alternates support feet.
Those states stay pending until joint/support and visual evidence is supplied.
"""
import json
from pathlib import Path

from p12_authored_sources import ROOT, POSES, sha, write_json


def geometry(record):
    sizes = [(f['sourceBox'][2] - f['sourceBox'][0],
              f['sourceBox'][3] - f['sourceBox'][1]) for f in record['frames']]
    return sizes


def audit_sources():
    manifest = json.loads((POSES / 'manifest.json').read_text())
    report = {'character': 'p12', 'status': 'NOT_APPROVED',
              'sourceStates': len(manifest['states']), 'requiredStates': 42,
              'missingStates': manifest['missingStates'], 'states': {},
              'blockingErrors': list(manifest['errors']),
              'pending': ['registered native frames', 'joint/support motion evidence',
                          'anatomy and palette consistency', 'runtime source consumption',
                          'transition audit', 'super-resolution and alpha verification',
                          'Character Pack integration', 'Android/emulator validation']}
    for state, record in manifest['states'].items():
        signals, issues = {}, []
        for frame in record['frames']:
            if sha(ROOT / frame['path']) != frame['sha256']:
                issues.append('extracted source was modified without renewed provenance')
        if sha(ROOT / record['source']) != record['sourceSha256']:
            issues.append('sheet changed after extraction')
        sizes = geometry(record)
        peak = record['peak']
        if state == 'CROUCH':
            ratio = sizes[peak][1] / sizes[0][1]
            signals['peakHeightRelativeToStandingStart'] = ratio
            if ratio >= 0.70:
                issues.append('crouch does not reduce body height sufficiently')
        elif state == 'RISE':
            ratio = sizes[-1][1] / sizes[0][1]
            signals['standingEndRelativeToSquatStart'] = ratio
            if ratio <= 1.30:
                issues.append('rise lacks squat-to-standing height change')
        elif state == 'LAND':
            ratio = sizes[peak][1] / sizes[-1][1]
            signals['absorptionHeightRelativeToStandingEnd'] = ratio
            if ratio >= 0.80:
                issues.append('landing lacks visible absorption squat')
        elif state == 'KNOCKDOWN':
            ratio = sizes[-1][0] / sizes[-1][1]
            signals['proneEndAspect'] = ratio
            if ratio <= 2.0:
                issues.append('knockdown does not finish with a horizontal body')
        elif state == 'GROUNDED':
            ratio = min(w / h for w, h in sizes)
            signals['minimumProneAspect'] = ratio
            if ratio <= 2.0:
                issues.append('grounded sequence contains a non-prone pose')
        elif state == 'GETUP':
            signals['startProneAspect'] = sizes[0][0] / sizes[0][1]
            signals['endStandingAspect'] = sizes[-1][1] / sizes[-1][0]
            if signals['startProneAspect'] <= 2.0 or signals['endStandingAspect'] <= 1.5:
                issues.append('getup lacks complete prone-to-standing sequence')
        report['states'][state] = {'status': 'SOURCE_REVIEW_PENDING', 'signals': signals,
                                  'findings': issues, 'visualApproval': False}
        report['blockingErrors'].extend({'state': state, 'error': issue} for issue in issues)
    if report['missingStates']:
        report['blockingErrors'].append({'error': 'source coverage incomplete',
                                        'states': report['missingStates']})
    write_json(ROOT / 'docs/art/p12-authored-source-audit.json', report)
    print(f"P12 source audit: {report['sourceStates']}/42 states, "
          f"{len(report['blockingErrors'])} source findings; NOT art-approved")
    # Exit code covers only source integrity/coverage and these limited geometry
    # checks. The explicit NOT_APPROVED status and pending checks remain.
    return int(bool(report['blockingErrors']))


if __name__ == '__main__':
    raise SystemExit(audit_sources())
