#!/usr/bin/env python3
"""Exercise the packaged character chooser and its local navigation."""
import argparse
import json
from pathlib import Path
from playwright.sync_api import sync_playwright


def check(assets, evidence, full_processed=None):
    evidence.mkdir(parents=True, exist_ok=True)
    data = json.loads((assets/'manifest.json').read_text())
    expected = [c['character'] for c in data['characters'] if c['videos']]
    errors, external, checked, pixel_errors = [], [], [], {}
    with sync_playwright() as pw:
        browser = pw.chromium.launch(args=['--no-sandbox'])
        page = browser.new_page(viewport={'width':390, 'height':844}, is_mobile=True, has_touch=True)
        page.on('pageerror', lambda error: errors.append(str(error)))
        page.on('request', lambda request: external.append(request.url)
                if request.url.startswith(('http://','https://')) else None)
        page.goto((assets/'index.html').resolve().as_uri())
        assert page.locator('main .card').count() == len(expected)
        assert page.locator('main article').count() == len(data['characters'])-len(expected)
        page.wait_for_function("Array.from(document.images).every(i=>i.complete&&i.naturalWidth>0)")
        assert page.evaluate('document.body.scrollWidth<=innerWidth')
        page.screenshot(path=str(evidence/'collection-chooser-phone.png'), full_page=True)
        for cid in expected:
            page.locator(f'a.card[href="characters/{cid}/index.html"]').tap()
            page.wait_for_function("document.getElementById('rawInfo').textContent.length>0")
            assert page.locator('h1').inner_text() == 'Sprites — '+cid
            assert page.locator('#play').is_visible()
            if full_processed:
                from check_collection import check_preview_pixels
                manifest = json.loads((assets/'characters'/cid/'manifest.json').read_text())
                usable = [(i,c) for i,c in enumerate(manifest['clips']) if c['status']!='rejected']
                if usable:
                    index, clip = next(((i,c) for i,c in usable if c.get('role')=='walk_back'), usable[0])
                    page.select_option('#clip',str(index))
                    page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith(clips[current].name+' ·')")
                    pixel_errors[cid] = check_preview_pixels(page, full_processed/cid, clip)
                    page.screenshot(path=str(evidence/f'{cid}-bundled-phone.png'), full_page=True)
            page.get_by_role('link', name='← Personagens').tap()
            page.wait_for_selector('#counts')
            checked.append(cid)
        browser.close()
    assert not errors, errors
    assert not external, external
    result = dict(characters_checked=checked, missing_video_cards=len(data['characters'])-len(expected),
                  page_errors=errors, external_requests=external, navigation_passed=True,
                  preview_vs_full_png_mean_rgb_errors=pixel_errors)
    (evidence/'collection-bundle-checks.json').write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps(result))


if __name__ == '__main__':
    p=argparse.ArgumentParser();p.add_argument('assets', type=Path);p.add_argument('--evidence', type=Path, required=True)
    p.add_argument('--full-processed',type=Path)
    a=p.parse_args();check(a.assets, a.evidence, a.full_processed)
