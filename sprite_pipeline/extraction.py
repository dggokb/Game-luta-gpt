"""Lossless decoded frame extraction and original presentation timestamps."""
import hashlib
import json
import shutil
import subprocess
from fractions import Fraction
from pathlib import Path
from .errors import CalibrationError


def digest(path):
    sha = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            sha.update(chunk)
    return sha.hexdigest()


def run(command):
    try:
        return subprocess.run(command, check=True, capture_output=True).stdout
    except (OSError, subprocess.CalledProcessError) as error:
        detail = getattr(error, 'stderr', b'').decode(errors='replace')[-2000:]
        raise CalibrationError(f"Falha no FFmpeg/FFprobe: {detail or error}") from error


def extract_video(source, destination):
    source, destination = Path(source), Path(destination)
    if not source.is_file():
        raise CalibrationError(f"Vídeo ausente: {source}")
    if not shutil.which('ffmpeg') or not shutil.which('ffprobe'):
        raise CalibrationError("Instale ffmpeg e ffprobe")
    sha = digest(source)
    probe = json.loads(run(['ffprobe', '-v', 'error', '-select_streams', 'v:0',
                           '-show_streams', '-show_frames', '-of', 'json', str(source)]))
    if not probe.get('streams') or not probe.get('frames'):
        raise CalibrationError("Vídeo sem frames decodificáveis")
    stream = probe['streams'][0]
    # Do not silently quantize high-bit-depth/HDR originals to 8 bits.
    if stream.get('color_transfer') in ('smpte2084', 'arib-std-b67') or any(
            v in stream['pix_fmt'] for v in ('10', '12', '14', '16', 'f32')):
        raise CalibrationError("HDR/alta profundidade exige conversão explícita externa; original preservado")
    times = []
    for f in probe['frames']:
        tick = f.get('best_effort_timestamp')
        if tick is None:
            raise CalibrationError("Frame sem timestamp; não foi inventado um tempo por FPS")
        times.append({'pts': int(tick), 'timestamp_s': float(Fraction(str(tick)) * Fraction(stream['time_base'])),
                      'duration_s': float(f.get('duration_time', f.get('pkt_duration_time', 0)))})
    if any(b['pts'] <= a['pts'] for a, b in zip(times, times[1:])):
        raise CalibrationError("Timestamps não estritamente crescentes; revisão necessária")
    destination.mkdir(parents=True)
    run(['ffmpeg', '-v', 'error', '-xerror', '-noautorotate', '-i', str(source), '-map', '0:v:0',
         '-fps_mode', 'passthrough', '-c:v', 'png', '-pix_fmt', 'rgba', '-threads', '1',
         '-start_number', '0', str(destination / '%06d.png')])
    files = sorted(destination.glob('*.png'))
    if len(files) != len(times) or digest(source) != sha:
        raise CalibrationError("Contagem de frames divergente ou fonte alterada durante extração")
    start = times[0]['timestamp_s']
    for i, (time, file) in enumerate(zip(times, files)):
        time.update(index=i, file=file.name, time_s=time['timestamp_s'] - start)
    avg = stream.get('avg_frame_rate', '0/0')
    try:
        fps = float(Fraction(avg))
    except (ValueError, ZeroDivisionError):
        fps = None
    durations = [b['time_s'] - a['time_s'] for a, b in zip(times, times[1:])]
    last_duration = times[-1]['duration_s'] or (durations[-1] if durations else 0)
    if last_duration <= 0:
        raise CalibrationError("Duração do último frame indisponível")
    times[-1]['duration_s'] = last_duration
    for i, dt in enumerate(durations):
        times[i]['duration_s'] = dt
    metadata = {'source_name': source.name, 'source_sha256': sha,
                'resolution': [stream['width'], stream['height']], 'pixel_format_source': stream['pix_fmt'],
                'fps_original': fps, 'avg_frame_rate': avg, 'r_frame_rate': stream.get('r_frame_rate'),
                'time_base': stream['time_base'], 'rotation_applied': False,
                'stream_side_data': stream.get('side_data_list', []),
                'variable_frame_rate': bool(durations and max(durations)-min(durations) > 1e-5),
                'frame_count': len(files), 'duration_s': times[-1]['time_s'] + last_duration,
                'raw_encoding': 'PNG RGBA8, lossless decoded pixels; no frame selection', 'frames': times}
    (destination / 'extraction.json').write_text(json.dumps(metadata, indent=2), encoding='utf-8')
    return metadata
