"""Bounded atlas pages and reproducible metadata; no atlas-driven resizing."""
import json
import math
from pathlib import Path
import cv2
import numpy as np
from .imaging import save_rgba


def write_json(path, data):
    Path(path).write_text(json.dumps(data, ensure_ascii=False, indent=2, allow_nan=False), encoding='utf-8')


def export_sheets(folder, count, canvas, root, max_size=4096):
    w, h = canvas
    columns = max(1, max_size//w)
    rows = max(1, max_size//h)
    per_page = columns*rows
    pages, placements = [], []
    dest = folder/'spritesheets'
    dest.mkdir()
    for start in range(0, count, per_page):
        size = min(per_page, count-start)
        cols, row_count = min(columns, size), math.ceil(size/columns)
        sheet = np.zeros((row_count*h, cols*w, 4), np.uint8)
        filename = f'page-{len(pages):03d}.png'
        for j in range(size):
            file = folder/'normalized'/f'{start+j:06d}.png'
            data = cv2.cvtColor(cv2.imread(str(file), cv2.IMREAD_UNCHANGED), cv2.COLOR_BGRA2RGBA)
            x, y = (j%columns)*w, (j//columns)*h
            sheet[y:y+h, x:x+w] = data
            placements.append({'index': start+j, 'page': filename, 'rect': [x, y, w, h],
                               'root_in_cell': root, 'trimmed': False})
        save_rgba(dest/filename, sheet)
        pages.append({'file': filename, 'size': [sheet.shape[1], sheet.shape[0]]})
    result = {'pages': pages, 'frames': placements, 'cell_size': canvas, 'root': root,
              'scale_changed_for_packing': False}
    write_json(dest/'atlas.json', result)
    return {'directory': 'spritesheets', 'metadata': 'spritesheets/atlas.json', 'pages': pages}


def contact_sheet(path, images, labels, max_column=600):
    """Evidence thumbnails only; PNG sprites retain their target resolution."""
    columns = min(3, len(images))
    rows = math.ceil(len(images)/columns)
    aspect = max(im.shape[0]/im.shape[1] for im in images)
    cellw = max_column
    cellh = min(1000, round(cellw*aspect))+45
    sheet = np.full((rows*cellh, columns*cellw, 3), 30, np.uint8)
    for i, (im, label) in enumerate(zip(images, labels)):
        k = min((cellw-20)/im.shape[1], (cellh-50)/im.shape[0])
        resized = cv2.resize(im, (round(im.shape[1]*k), round(im.shape[0]*k)), interpolation=cv2.INTER_AREA)
        y0, x0 = (i//columns)*cellh+40, (i%columns)*cellw+(cellw-resized.shape[1])//2
        if resized.shape[2] == 4:
            a = resized[:, :, 3:].astype(np.float32)/255
            yy, xx = np.indices(resized.shape[:2])
            bg = np.where(((xx//16+yy//16)%2)[:, :, None], 65, 45)
            rgb = np.rint(resized[:, :, :3]*a+bg*(1-a)).astype(np.uint8)
        else:
            rgb = resized
        sheet[y0:y0+rgb.shape[0], x0:x0+rgb.shape[1]] = rgb
        cv2.putText(sheet, label, ((i%columns)*cellw+12, (i//columns)*cellh+26), cv2.FONT_HERSHEY_SIMPLEX, .55, (220, 220, 220), 1, cv2.LINE_AA)
    cv2.imwrite(str(path), cv2.cvtColor(sheet, cv2.COLOR_RGB2BGR))
