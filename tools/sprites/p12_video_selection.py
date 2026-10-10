"""Bind real video segments without inventing a pose or a timestamp.

An action may use another existing video for neutral preparation/recovery.
Every video is independently ordered and its impact must remain in the action
video. The production renderer still requires complete manual review and exact
source PNG hashes before writing anything.
"""
import math


def bind_sources(state, names, indices, main, primary, count, records):
    if (len(names) != count or len(indices) != count
            or any(type(i) is not int for i in indices)
            or any(not isinstance(n, str) or n not in records for n in names)):
        raise ValueError(state + ': each combat frame needs an existing source video and index')
    if type(main) is not int or not 0 <= main < count or names[main] != primary:
        raise ValueError(state + ': impact/main pose must belong to the independent action video')
    if names.count(primary) < min(2, count):
        raise ValueError(state + ': neutral bridges cannot replace the actual action')
    last = {}
    for name, index in zip(names, indices):
        frames = records[name]['frames']
        if not 0 <= index < len(frames) or frames[index].get('index', index) != index:
            raise ValueError(state + ': source index not recorded in original video')
        time = frames[index]['time']
        if type(time) not in (int, float) or not math.isfinite(time) or time < 0:
            raise ValueError(state + ': original timestamp must be finite and nonnegative')
        if name in last and (index <= last[name][0] or time <= last[name][1]):
            raise ValueError(state + ': real source frames must be sequential, unique and chronological within each video')
        last[name] = (index, time)
    return list(zip(names, indices))
