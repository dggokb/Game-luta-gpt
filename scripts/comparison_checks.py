"""Interaction regressions for comparing actual bundled sprite frames."""
from bisect import bisect_right


def check_comparison(page, screenshots, *, touch=False):
    activate = page.tap if touch else page.click
    if touch:
        activate('#mobileTabs button[data-view="compare"]')
    page.evaluate('pausePlayback()')
    activate('#single')
    page.select_option('#clip', label='jab')
    page.select_option('#compare', label='crouch')
    page.select_option('#compareMode', 'time')

    def seek(selector, value):
        page.locator(selector).evaluate('(e,value)=>{e.value=value;e.dispatchEvent(new Event("input"))}', value)

    def right_ready(index):
        page.wait_for_function("index=>document.getElementById('compareInfo').textContent.startsWith('crouch · frame '+(index+1)+'/')", arg=index)

    # Reproduce the user's screenshot: Jab at frame 98, Crouch on the right.
    seek('#seek', 0)
    right_ready(0)
    initial = page.locator('#comparison').screenshot()
    seek('#seek', 97)
    timing = page.evaluate("({time:clips[current].frames[frame].time_s,times:clips[Number($('compare').value)].frames.map(f=>f.time_s)})")
    expected = bisect_right(timing['times'], timing['time']+1e-9)-1
    right_ready(expected)
    assert page.locator('#position').inner_text().startswith('98/')
    assert page.locator('#rightTitle').inner_text() == 'Direita · crouch'
    assert page.locator('#comparison').screenshot() != initial
    assert page.evaluate("$('after').width===$('comparison').width")
    page.screenshot(path=str(screenshots/'comparison-jab-crouch.png'), full_page=True)

    # Fixed right pose remains unchanged when the left animation is scrubbed.
    page.select_option('#compareMode', 'fixed')
    frozen = page.locator('#comparison').screenshot()
    seek('#seek', 10)
    right_ready(expected)
    assert page.locator('#comparison').screenshot() == frozen
    seek('#compareSeek', 40)
    right_ready(40)
    assert page.locator('#compareMode').input_value() == 'fixed'
    assert page.locator('#position').inner_text().startswith('11/')
    activate('#compareNext'); right_ready(41)
    activate('#comparePrev'); right_ready(40)
    seek('#compareSeek', 0); activate('#comparePrev'); right_ready(0)
    seek('#compareSeek', 120); activate('#compareNext'); right_ready(120)

    # Playback must visibly animate the right pane, not just change its caption.
    page.select_option('#compareMode', 'time')
    seek('#seek', 0)
    right_ready(0)
    activate('#play')
    page.wait_for_function('frame>18 && comparisonFrame>18')
    activate('#play')
    page.wait_for_function("document.getElementById('compareInfo').textContent.startsWith('crouch · frame '+(comparisonFrame+1)+'/')")
    assert page.locator('#comparison').screenshot() != initial
    assert not page.evaluate('playing')

    # Unequal durations loop by each clip's original timing, not frame count.
    page.select_option('#compare', label='crouch_small')
    seek('#seek', 120)
    timing = page.evaluate("({time:clips[current].frames[frame].time_s,duration:clips[Number($('compare').value)].duration_s,times:clips[Number($('compare').value)].frames.map(f=>f.time_s)})")
    expected_short = bisect_right(timing['times'], timing['time'] % timing['duration']+1e-9)-1
    page.wait_for_function("index=>document.getElementById('compareInfo').textContent.startsWith('crouch_small · frame '+(index+1)+'/')", arg=expected_short)
    assert page.evaluate("frameAtTime({frames:[{time_s:0},{time_s:.1},{time_s:.9}],duration_s:1.2},.5)") == 1
    assert page.evaluate("frameAtTime({frames:[{time_s:0},{time_s:.1},{time_s:.9}],duration_s:1.2},1.21)") == 0
    return ['jab_crouch_comparison_changes_the_actual_right_image',
            'right_pose_can_be_fixed_scrubbed_and_stepped_independently',
            'playback_animates_both_panes',
            'comparison_uses_original_timestamps_and_individual_clip_durations']
