import json
import tempfile
import unittest
from pathlib import Path

import cv2
import numpy as np

from sprite_standardizer import CalibrationError, process, similarity


def mock_frame(size, shift=0):
    image = np.zeros((200, 240, 3), dtype=np.uint8)
    image[:] = (0, 255, 0)
    s = size
    x, y = 86+shift, 31
    color = (200, 75, 20)
    cv2.circle(image, (x+round(9*s), y+round(8*s)), round(8*s), color, -1)
    cv2.rectangle(image, (x, y+round(19*s)), (x+round(18*s), y+round(80*s)), color, -1)
    cv2.rectangle(image, (x, y+round(77*s)), (x+round(6*s), y+round(133*s)), color, -1)
    cv2.rectangle(image, (x+round(12*s), y+round(77*s)), (x+round(18*s), y+round(133*s)), color, -1)
    return image


class Tests(unittest.TestCase):
    def test_similarity_self(self):
        mask = np.zeros((96, 96), np.uint8)
        mask[20:70, 25:50] = 1
        self.assertEqual(similarity(mask, mask), 1.0)

    def test_two_different_video_scales_share_canvas(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            cv2.imwrite(str(path/"master.png"), mock_frame(1))
            for name, scale in (("a", 1), ("b", 1.2)):
                writer = cv2.VideoWriter(str(path/(name+".mp4")),
                                         cv2.VideoWriter_fourcc(*"mp4v"), 12, (240, 200))
                self.assertTrue(writer.isOpened())
                for offset in (0, 1, 2, 3):
                    writer.write(mock_frame(scale, offset))
                writer.release()
            conf = {
                "references": {"idle": "master.png"},
                "idle_target_height": 128,
                "minimum_similarity": 0.45,
                "clips": [
                    {"name": "a", "video": "a.mp4"},
                    {"name": "b", "video": "b.mp4"}
                ]
            }
            (path/"config.json").write_text(json.dumps(conf))
            data = process(path/"config.json", path/"out")
            self.assertEqual([c["frame_count"] for c in data["clips"]], [4, 4])
            self.assertNotEqual(data["clips"][0]["scale_per_video"],
                                data["clips"][1]["scale_per_video"])
            for name in ("a", "b"):
                sprites = sorted((path/"out"/name).glob("*.png"))
                self.assertEqual(len(sprites), 4)
                image = cv2.imread(str(sprites[0]), cv2.IMREAD_UNCHANGED)
                self.assertEqual([image.shape[1], image.shape[0]], data["canvas"])

    def test_missing_idle(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            (path/"bad.json").write_text(json.dumps({"references": {}, "clips": []}))
            with self.assertRaises(CalibrationError):
                process(path/"bad.json", path/"out")


if __name__ == "__main__":
    unittest.main()
