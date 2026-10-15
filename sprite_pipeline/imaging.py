"""Explicit background treatment. Never changes raw files or recenters poses."""
import cv2
import numpy as np
from .errors import CalibrationError


def image_rgba(frame, options=None):
    options = options or {}
    if frame is None or frame.ndim != 3 or frame.dtype != np.uint8:
        raise CalibrationError("Imagem inválida; use RGB/RGBA de 8 bits")
    if frame.shape[2] == 4 and np.any(frame[:, :, 3] < 255):
        rgba = cv2.cvtColor(frame, cv2.COLOR_BGRA2RGBA)
    else:
        rgb = cv2.cvtColor(frame[:, :, :3], cv2.COLOR_BGR2RGB)
        mode = options.get("mode", "green")
        if mode == "alpha":
            raise CalibrationError("Modo alpha exige transparência existente")
        if mode != "green":
            raise CalibrationError("Fundo não suportado; forneça alpha ou chroma verde")
        hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
        dominance = cv2.subtract(rgb[:, :, 1], cv2.max(rgb[:, :, 0], rgb[:, :, 2]))
        green = (cv2.inRange(hsv, (34, 41, 0), (94, 255, 255)) != 0) & (dominance > 12)
        alpha = np.full(green.shape, 255, np.uint8)
        alpha_curve = np.clip((24-np.arange(256, dtype=np.int32))*255//12, 0, 255).astype(np.uint8)
        alpha[green] = alpha_curve[dominance[green]]
        rgba = np.dstack((rgb, alpha))
    # Optional, user-visible exclusion regions, e.g. a watermark. Recorded in manifest.
    for rect in options.get("exclude_rectangles", []):
        x0, y0, x1, y1 = map(int, rect)
        if not (0 <= x0 < x1 <= rgba.shape[1] and 0 <= y0 < y1 <= rgba.shape[0]):
            raise CalibrationError("Retângulo de exclusão fora da imagem")
        rgba[y0:y1, x0:x1, 3] = 0
    return rgba


def bounds(rgba):
    mask = cv2.compare(rgba[:, :, 3], 32, cv2.CMP_GT)
    count = cv2.countNonZero(mask)
    if not count or count > rgba.shape[0] * rgba.shape[1] * .90:
        raise CalibrationError("Não foi possível separar personagem e fundo")
    x, y, w, h = cv2.boundingRect(mask)
    return [x, y, x+w, y+h]


def quality(rgba, bbox):
    x0, y0, x1, y1 = bbox
    mask = (rgba[y0:y1, x0:x1, 3] > 32).astype(np.uint8)
    n, _, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
    areas = sorted(stats[1:, cv2.CC_STAT_AREA], reverse=True)
    warnings = []
    if len(areas) > 1 and sum(areas[1:]) > max(32, sum(areas) * .005):
        warnings.append("disconnected_silhouette_or_background")
    x0, y0, x1, y1 = bbox
    if x0 == 0 or y0 == 0 or x1 == rgba.shape[1] or y1 == rgba.shape[0]:
        warnings.append("source_touches_edge_possible_clipping")
    return warnings


def shape_feature(rgba, bbox, size=96):
    x0, y0, x1, y1 = bbox
    mask = (rgba[y0:y1, x0:x1, 3] > 64).astype(np.uint8)
    h, w = mask.shape
    k = (size - 4) / max(w, h)
    nw, nh = max(1, round(w * k)), max(1, round(h * k))
    result = np.zeros((size, size), dtype=np.uint8)
    x, y = (size - nw) // 2, (size - nh) // 2
    result[y:y+nh, x:x+nw] = cv2.resize(mask, (nw, nh), interpolation=cv2.INTER_NEAREST)
    return result


def appearance_feature(rgba, bbox):
    x0, y0, x1, y1 = bbox
    crop = rgba[y0:y1, x0:x1].astype(np.float32) / 255
    return cv2.resize(crop[:, :, :3] * crop[:, :, 3:], (48, 64), interpolation=cv2.INTER_AREA)


def similarity(a, b):
    aa = cv2.dilate(a, np.ones((3, 3), np.uint8))
    bb = cv2.dilate(b, np.ones((3, 3), np.uint8))
    union = np.count_nonzero(aa | bb)
    return float(np.count_nonzero(aa & bb) / union) if union else 0.0


def affine_rgba(rgba, scale, translation, canvas):
    """Uniform similarity transform with premultiplied alpha."""
    data = rgba.astype(np.float32) / 255
    data[:, :, :3] *= data[:, :, 3:]
    matrix = np.array([[scale, 0, translation[0]], [0, scale, translation[1]]], np.float64)
    result = cv2.warpAffine(data, matrix, tuple(canvas), flags=cv2.INTER_LINEAR,
                            borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    alpha = result[:, :, 3:]
    result[:, :, :3] /= np.maximum(alpha, 1e-8)
    return np.clip(np.rint(result * 255), 0, 255).astype(np.uint8)


def save_rgba(path, rgba):
    if not cv2.imwrite(str(path), cv2.cvtColor(rgba, cv2.COLOR_RGBA2BGRA)):
        raise OSError(f"Falha ao gravar PNG: {path}")


def affine_rgba_region(rgba, scale, translation, canvas):
    """Rasterize the nontransparent region at full output resolution.

    The output still has the common canvas; the empty part is allocated only as
    bytes, rather than as a full floating-point interpolation workspace.
    """
    x0, y0, width, height = cv2.boundingRect(rgba[:, :, 3])
    result = np.zeros((canvas[1], canvas[0], 4), np.uint8)
    if not width or not height:
        return result
    dx0 = max(0, int(np.floor(x0*scale+translation[0]))-2)
    dy0 = max(0, int(np.floor(y0*scale+translation[1]))-2)
    dx1 = min(canvas[0], int(np.ceil((x0+width)*scale+translation[0]))+2)
    dy1 = min(canvas[1], int(np.ceil((y0+height)*scale+translation[1]))+2)
    if dx1 <= dx0 or dy1 <= dy0:
        return result
    data = rgba[y0:y0+height, x0:x0+width].astype(np.float32)/255
    data[:, :, :3] *= data[:, :, 3:]
    # Match warpAffine's global 10-bit coordinate rounding before subtracting
    # the integer crop origin. Re-inverting a translated local matrix changes
    # rounding at some edges, particularly for very small alpha values.
    forward = np.array([[scale, 0, translation[0]], [0, scale, translation[1]]], np.float64)
    inverse = cv2.invertAffineTransform(forward)
    xx = np.arange(dx0, dx1)
    yy = np.arange(dy0, dy1)
    sx = (np.rint(inverse[0, 0]*xx*1024).astype(np.int32)
          + int(np.rint(inverse[0, 2]*1024)) + 16) >> 5
    sy = (np.rint((inverse[1, 1]*yy+inverse[1, 2])*1024).astype(np.int32) + 16) >> 5
    maps = np.empty((len(yy), len(xx), 2), np.int16)
    maps[:, :, 0] = (sx >> 5)[None, :]-x0
    maps[:, :, 1] = (sy >> 5)[:, None]-y0
    fractions = (((sy & 31)[:, None] << 5)+(sx & 31)[None, :]).astype(np.uint16)
    tile = cv2.remap(data, maps, fractions, cv2.INTER_LINEAR,
                     borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    tile[:, :, :3] /= np.maximum(tile[:, :, 3:], 1e-8)
    result[dy0:dy1, dx0:dx1] = np.clip(np.rint(tile*255), 0, 255).astype(np.uint8)
    return result
