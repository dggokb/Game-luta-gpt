#!/usr/bin/env python3
"""Verify phone layout, offline bundle completeness and touch interactions."""
import argparse
import json
from pathlib import Path
from playwright.sync_api import sync_playwright
from comparison_checks import check_comparison


def main():
    parser=argparse.ArgumentParser();parser.add_argument('assets',type=Path);parser.add_argument('--screenshots',type=Path,required=True)
    args=parser.parse_args();assets=args.assets.resolve();args.screenshots.mkdir(parents=True,exist_ok=True)
    manifest=json.loads((assets/'manifest.json').read_text())
    frames=sum(c['frame_count'] for c in manifest['clips']);images=list(assets.rglob('*.webp'))
    assert frames==manifest['mobile_preview']['frame_count']
    assert len(images)==2*frames
    result={'checks':[],'page_errors':[],'external_requests':[],'frames':frames,'preview_images':len(images)}
    with sync_playwright() as pw:
        browser=pw.chromium.launch(headless=True,args=['--no-sandbox'])
        page=browser.new_page(viewport={'width':390,'height':844},device_scale_factor=1,is_mobile=True,has_touch=True)
        page.on('pageerror',lambda error:result['page_errors'].append(str(error)))
        page.on('request',lambda request:result['external_requests'].append(request.url) if request.url.startswith(('http://','https://')) else None)
        page.goto((assets/'index.html').as_uri())
        page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith('idle ·')")
        assert page.evaluate('document.body.scrollWidth <= window.innerWidth')
        box=page.locator('#play').bounding_box();assert box['y']+box['height']<=844
        assert page.locator('#after').is_visible() and not page.locator('#before').is_visible()
        page.screenshot(path=str(args.screenshots/'phone-home.png'),full_page=True)
        result['checks'].append('portrait_390px_no_overflow_and_play_visible')
        page.tap('#next');assert page.locator('#position').inner_text().startswith('2/')
        page.tap('#prev');assert page.locator('#position').inner_text().startswith('1/')
        page.tap('#play');page.wait_for_function('frame>2');page.tap('#play');assert not page.evaluate('playing')
        result['checks'].append('touch_play_pause_and_frame_steps')
        page.tap('#mobileTabs button[data-view="before"]')
        assert page.locator('#before').is_visible() and not page.locator('#after').is_visible()
        page.wait_for_function("document.getElementById('before').width>0")
        result['checks'].append('original_preview_tab')
        page.select_option('#clip',label='jab_half');page.select_option('#compare',label='jab')
        page.tap('#mobileTabs button[data-view="compare"]')
        page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith('jab_half ·') && document.getElementById('compareInfo').textContent.startsWith('jab ·')")
        assert page.evaluate("document.getElementById('after').width===document.getElementById('comparison').width")
        result['checks'].append('different_source_sizes_common_display_zoom')
        page.set_viewport_size({'width':844,'height':390})
        page.wait_for_function("document.getElementById('after').width===document.getElementById('comparison').width")
        assert page.evaluate("document.getElementById('after').width>0")
        page.screenshot(path=str(args.screenshots/'phone-landscape-comparison.png'),full_page=True)
        result['checks'].append('landscape_retains_frame_and_common_zoom')
        page.set_viewport_size({'width':390,'height':844})
        page.tap('#mobileTabs button[data-view="after"]')
        page.locator('.mobileOptions>summary').tap()
        page.check('#ghost');page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith('jab_half ·')");page.uncheck('#ghost')
        result['checks'].append('master_overlay')
        for action in ('jab','dash','crouch','walk_forward','walk_back','back_dash'):
            if page.locator('#'+action).is_disabled():continue
            page.tap('#'+action);page.tap('#play')
            assert page.evaluate('sequence!==null')
            page.evaluate('frame=clips[current].frame_count-1;advance();render()')
            page.wait_for_function(f"document.getElementById('normalInfo').textContent.startsWith('{action} ·')")
            if action in ('walk_forward','back_dash'):
                assert page.locator('#clipNote').is_visible()
                assert page.locator('#clipNote').inner_text()
            page.evaluate('frame=clips[current].frame_count-1;advance();render()')
            page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith('idle ·')")
            if action in ('walk_forward','walk_back','dash','back_dash'):
                assert page.evaluate('carry.every(v=>v===0)'), 'In-place action must not replay extracted movement'
            result['checks'].append('idle_'+action+'_idle')
        page.tap('#single');page.select_option('#clip',label='idle');page.select_option('#loop',index=1)
        page.tap('#play');page.wait_for_function('frame>range()[0]+2');page.tap('#play')
        result['checks'].append('cycle_preview_without_frame_discard')
        result['checks'].extend(check_comparison(page, args.screenshots, touch=True))
        assert not result['page_errors'],result['page_errors']
        assert not result['external_requests'],result['external_requests']
        browser.close()
    result['status']='passed';(args.screenshots/'mobile-checks.json').write_text(json.dumps(result,indent=2))
    print(json.dumps(result,indent=2))


if __name__=='__main__':main()
