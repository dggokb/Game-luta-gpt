"""Decode an entire original video without intermediate PNG files.

FFprobe timestamps remain authoritative. The decoder must emit exactly their
frame count; neither OpenCV FPS estimates nor frame seeking are used.
"""
import json
import subprocess
import tempfile
from fractions import Fraction
import numpy as np
from .extraction import digest, run
from .errors import CalibrationError


def probe_video(source):
    probe = json.loads(run(['ffprobe', '-v', 'error', '-select_streams', 'v:0',
                           '-show_streams', '-show_frames', '-of', 'json', str(source)]))
    if not probe.get('streams') or not probe.get('frames'):
        raise CalibrationError('Vídeo sem frames')
    stream = probe['streams'][0]
    if stream.get('color_transfer') in ('smpte2084', 'arib-std-b67') or any(
            v in stream['pix_fmt'] for v in ('10', '12', '14', '16', 'f32')):
        raise CalibrationError('HDR/alta profundidade exige conversão explícita')
    frames = []
    for i, f in enumerate(probe['frames']):
        if 'best_effort_timestamp' not in f:
            raise CalibrationError('Frame sem timestamp original')
        pts = int(f['best_effort_timestamp'])
        frames.append({'index': i, 'pts': pts, 'timestamp_s': float(pts*Fraction(stream['time_base'])),
                       'duration_s': float(f.get('duration_time', f.get('pkt_duration_time', 0)))})
    start = frames[0]['timestamp_s']
    for i, f in enumerate(frames):
        f['time_s'] = f['timestamp_s']-start
        if i+1 < len(frames):
            f['duration_s'] = frames[i+1]['timestamp_s']-f['timestamp_s']
        elif not f['duration_s'] and i:
            f['duration_s'] = frames[i-1]['duration_s']
        if f['duration_s'] <= 0:
            raise CalibrationError('Duração original inválida')
    fps = float(Fraction(stream['avg_frame_rate'])) if stream['avg_frame_rate'] != '0/0' else None
    return {'source_name': source.name, 'source_sha256': digest(source),
        'resolution': [stream['width'], stream['height']], 'pixel_format_source': stream['pix_fmt'],
        'time_base': stream['time_base'], 'fps_original': fps,
        'variable_frame_rate': max(f['duration_s'] for f in frames)-min(f['duration_s'] for f in frames)>1e-5,
        'frame_count': len(frames), 'duration_s': frames[-1]['time_s']+frames[-1]['duration_s'],
        'rotation_applied': False, 'frames': frames}


def decode_frames(source, metadata):
    w, h = metadata['resolution']
    size = w*h*4
    with tempfile.TemporaryFile() as error_log:
        process = subprocess.Popen(['ffmpeg', '-v', 'error', '-xerror', '-noautorotate',
            '-threads', '1', '-i', str(source), '-map', '0:v:0', '-fps_mode', 'passthrough',
            '-f', 'rawvideo', '-pix_fmt', 'bgra', '-threads', '1', 'pipe:1'],
            stdout=subprocess.PIPE, stderr=error_log, stdin=subprocess.DEVNULL)
        try:
            for _ in metadata['frames']:
                frame = process.stdout.read(size)
                if len(frame) != size:
                    raise CalibrationError('Decoder emitiu menos frames que os timestamps originais')
                yield np.frombuffer(frame, np.uint8).reshape(h, w, 4)
            if process.stdout.read(1):
                raise CalibrationError('Decoder emitiu mais frames que os timestamps originais')
            if process.wait():
                error_log.seek(0)
                raise CalibrationError(error_log.read().decode(errors='replace')[-2000:])
        finally:
            process.stdout.close()
            if process.poll() is None:
                process.kill()
                process.wait()
    if digest(source) != metadata['source_sha256']:
        raise CalibrationError('Fonte mudou durante a leitura')
