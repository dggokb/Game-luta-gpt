#!/usr/bin/env python3
"""Browser interaction checks against the generated local HTML report."""
import argparse
import json
from pathlib import Path
from playwright.sync_api import sync_playwright
from comparison_checks import check_comparison


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('result',type=Path)
    parser.add_argument('--screenshots',type=Path,required=True)
    args = parser.parse_args()
    args.screenshots.mkdir(parents=True,exist_ok=True)
    result = args.result.resolve()
    report = {'checks':[], 'page_errors':[]}
    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True,args=['--no-sandbox'])
        page = browser.new_page(viewport={'width':1500,'height':1100})
        page.on('pageerror', lambda e: report['page_errors'].append(str(e)))
        page.goto((result/'index.html').as_uri())
        page.wait_for_function("document.querySelector('#normalInfo').textContent.includes('idle')")
        page.click('#next')
        assert page.locator('#position').inner_text().startswith('2/')
        page.click('#prev')
        assert page.locator('#position').inner_text().startswith('1/')
        report['checks'].append('pause_and_frame_step')
        # Original jab vs controlled half-resolution derivative, common display scale.
        page.select_option('#clip',label='jab_half')
        page.select_option('#compare',label='jab')
        page.wait_for_function("document.querySelector('#normalInfo').textContent.includes('jab_half') && document.querySelector('#compareInfo').textContent.includes('jab ·')")
        assert page.eval_on_selector('#after','e=>e.width') == page.eval_on_selector('#comparison','e=>e.width')
        page.screenshot(path=str(args.screenshots/'different-scales.png'),full_page=True)
        report['checks'].append('before_after_and_common_comparison_zoom')
        page.check('#ghost')
        page.wait_for_function("document.querySelector('#normalInfo').textContent.includes('jab_half')")
        page.uncheck('#ghost')
        for action in ('jab','dash','crouch','walk_forward','walk_back','back_dash'):
            if page.locator('#'+action).is_disabled():continue
            page.click('#'+action)
            page.wait_for_function('playing === true && sequence !== null')
            # Advance deterministically across the full sequence using frame controls.
            page.click('#play')
            page.evaluate('frame=clips[current].frame_count-1;advance();render()')
            page.wait_for_function(f"document.querySelector('#normalInfo').textContent.startsWith('{action} ·')")
            page.screenshot(path=str(args.screenshots/f'idle-{action}-idle.png'),full_page=True)
            if action == 'crouch':
                page.evaluate('frame=60;render()')
                page.wait_for_function("document.querySelector('#normalInfo').textContent.includes('correção anatômica anotada') && document.querySelector('#position').textContent.startsWith('61/')")
                page.screenshot(path=str(args.screenshots/'crouch-corrected.png'),full_page=True)
            if action in ('walk_forward','walk_back','back_dash'):
                index=100 if action=='walk_forward' else 50
                page.evaluate(f'frame={index};render()')
                page.wait_for_function(f"document.querySelector('#normalInfo').textContent.startsWith('{action} ·') && document.querySelector('#position').textContent.startsWith('{index+1}/')")
                if action in ('walk_forward','back_dash'):
                    assert page.locator('#clipNote').is_visible()
                page.screenshot(path=str(args.screenshots/f'{action}-action.png'),full_page=True)
            page.evaluate('frame=clips[current].frame_count-1;advance();render()')
            page.wait_for_function("document.querySelector('#normalInfo').textContent.startsWith('idle ·')")
            report['checks'].append('idle_'+action+'_idle_sequence')
        page.click('#single')
        page.select_option('#clip',label='jump')
        page.locator('#seek').evaluate("e=>{e.value=30;e.dispatchEvent(new Event('input'))}")
        page.wait_for_function("document.querySelector('#normalInfo').textContent.includes('jump')")
        page.screenshot(path=str(args.screenshots/'jump-displacement.png'),full_page=True)
        report['checks'].append('seek_and_airborne_frame')
        page.select_option('#clip',label='crouch_small')
        page.wait_for_function("document.querySelector('#normalInfo').textContent.startsWith('crouch_small ·')")
        page.screenshot(path=str(args.screenshots/'crouch-native-scale.png'),full_page=True)
        page.select_option('#clip',label='idle')
        page.select_option('#loop',index=1)
        page.click('#play')
        first = page.locator('#position').inner_text()
        page.wait_for_function('frame > range()[0]+2')
        page.click('#play')
        assert page.locator('#position').inner_text() != first
        report['checks'].append('advisory_loop_selection_and_timestamp_playback')
        report['checks'].extend(check_comparison(page, args.screenshots))
        assert not report['page_errors'], report['page_errors']
        browser.close()
    report['status']='passed'
    (args.screenshots/'browser-checks.json').write_text(json.dumps(report,indent=2))
    print(json.dumps(report,indent=2))


if __name__=='__main__':
    main()
