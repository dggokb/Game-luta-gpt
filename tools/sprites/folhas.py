"""Layout of the shipped sprite sheets (android/app/src/main/res/drawable-nodpi).

A clip's logical geometry is a grid of equal cells (report['packed']: frameWidth x
frameHeight, root at rootX/rootY, `columns` per row). The shipped image no longer is that
grid: each frame keeps only the box around its drawing (plus a transparent margin), and the
boxes are packed in one sheet. report['packed']['frames'] says, per frame, where its box sits
in the sheet and where it goes inside the logical cell:

    [sheetX, sheetY, width, height, cellX, cellY]     (width 0: an empty frame)

Positions and sizes are multiples of ALIGN, so a sheet decoded at 1/2 or 3/4 of its size
(the runtime does this on small screens) still has every box on whole pixels.

`cell()` rebuilds the logical cell of a frame, so tools that measured the old grid
(harmony, regua, the P01 calibration) see exactly the same pixels as before.
"""
import math
from PIL import Image

# Box positions/sizes in the sheet are multiples of this (decoding at 1/2 and 3/4 stays exact).
ALIGN = 4
# Transparent pixels kept around each drawing inside its box: filtering at the box edge blends
# with transparency, exactly as it did with the neighbouring empty pixels of the old cell.
MARGIN = 4
# Empty pixels between boxes in the sheet.
GUTTER = 4
# Longest side a sheet may have (the widest shipped grid sheets already reach ~7.5k).
MAX_SIDE = 8192
# Preferred longest side: the texture limit of older GPUs. Wider only when nothing fits.
PREFERRED_SIDE = 4096


def _up(value):
    return -(-value // ALIGN) * ALIGN


def trim_boxes(cells):
    """Box (x, y, w, h) inside each cell image around its non-transparent pixels, or None."""
    boxes = []
    for cell in cells:
        bbox = cell.getchannel('A').getbbox()
        if bbox is None:
            boxes.append(None)
            continue
        x0, y0 = max(0, bbox[0] - MARGIN), max(0, bbox[1] - MARGIN)
        x1, y1 = bbox[2] + MARGIN, bbox[3] + MARGIN
        boxes.append((x0, y0, _up(x1 - x0), _up(y1 - y0)))
    return boxes


def _shelves(sizes, width):
    """Shelf packing (tallest first, then by frame); returns positions and the sheet height."""
    order = sorted(range(len(sizes)), key=lambda i: (-sizes[i][1], i))
    pos = [None] * len(sizes)
    x = y = shelf = 0
    for i in order:
        w, h = sizes[i]
        if w == 0:
            pos[i] = (0, 0)
            continue
        if x > 0 and x + w > width:
            y += shelf + GUTTER
            x = shelf = 0
        pos[i] = (x, y)
        x += w + GUTTER
        shelf = max(shelf, h)
    return pos, y + shelf


def pack(sizes):
    """Places boxes of the given sizes; returns (positions, sheetWidth, sheetHeight).

    Deterministic: tries widths around the square root of the area and keeps the smallest
    sheet (then the most square one), preferring sides within PREFERRED_SIDE and never
    beyond MAX_SIDE.
    """
    used = [(w, h) for w, h in sizes if w]
    if not used:
        return [(0, 0)] * len(sizes), ALIGN, ALIGN
    area = sum((w + GUTTER) * (h + GUTTER) for w, h in used)
    widest = max(w for w, _ in used)
    best = None
    base = max(widest, int(math.sqrt(area)))
    for step in range(0, 25):
        width = _up(max(widest, int(base * (0.6 + step * 0.05))))
        pos, height = _shelves(sizes, width)
        real_width = max(x + w for (x, _), (w, _h) in zip(pos, sizes) if w)
        sheet = (_up(real_width), _up(height))
        if max(sheet) > MAX_SIDE:
            continue
        key = (max(sheet) > PREFERRED_SIDE, sheet[0] * sheet[1], max(sheet))
        if best is None or key < best[0]:
            best = (key, pos, sheet)
    if best is None:
        raise ValueError(f'sprite boxes do not fit a {MAX_SIDE}px sheet')
    return best[1], best[2][0], best[2][1]


def trim_sheet(cells):
    """Trims and packs equal-size RGBA cell images: (sheet image, frames table, (w, h))."""
    boxes = trim_boxes(cells)
    sizes = [(b[2], b[3]) if b else (0, 0) for b in boxes]
    positions, width, height = pack(sizes)
    sheet = Image.new('RGBA', (width, height))
    frames = []
    for cell, box, (sx, sy) in zip(cells, boxes, positions):
        if box is None:
            frames.append([0, 0, 0, 0, 0, 0])
            continue
        x, y, w, h = box
        # A box near the right/bottom border reaches past the cell: crop() pads it with transparency.
        sheet.paste(cell.crop((x, y, x + w, y + h)), (sx, sy))
        frames.append([sx, sy, w, h, x, y])
    return sheet, frames, (width, height)


def cell(sheet, packed, frame):
    """The logical cell (frameWidth x frameHeight, RGBA) of `frame`, from a shipped sheet."""
    w, h = packed['frameWidth'], packed['frameHeight']
    out = Image.new('RGBA', (w, h))
    frames = packed.get('frames')
    rgba = sheet if sheet.mode == 'RGBA' else sheet.convert('RGBA')
    if frames is None:  # old grid sheet
        cols = packed.get('columns', packed['frameCount'])
        x, y = frame % cols * w, frame // cols * h
        return rgba.crop((x, y, x + w, y + h))
    sx, sy, bw, bh, cx, cy = frames[frame]
    if bw:
        cw, ch = min(bw, w - cx), min(bh, h - cy)
        out.paste(rgba.crop((sx, sy, sx + cw, sy + ch)), (cx, cy))
    return out
