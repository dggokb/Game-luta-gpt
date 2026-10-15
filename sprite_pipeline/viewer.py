"""Self-contained local HTML report. No web server or paid service required."""
import json
from pathlib import Path


def write_viewer(output, manifest):
    template = Path(__file__).with_name('viewer.html').read_text(encoding='utf-8')
    data = json.dumps(manifest, ensure_ascii=False).replace('<', '\\u003c').replace('&', '\\u0026')
    (output/'index.html').write_text(template.replace('__MANIFEST__', data), encoding='utf-8')
