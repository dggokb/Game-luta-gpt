import json
import sys
import unittest
from pathlib import Path

try:  # numpy e opencv vêm com as ferramentas de autoria; a CI instala só o Pillow
    import numpy as np
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    import montagem
except ModuleNotFoundError as missing:
    if missing.name not in ('numpy', 'cv2'):
        raise
    raise unittest.SkipTest('numpy/opencv only in the authoring tools')


class MontagemTests(unittest.TestCase):
    """Receita dos clipes colados de vários vídeos (tools/sprites/montagem.py)."""

    def test_frame_lists(self):
        self.assertEqual([0, 4, 8, 12, 9, 10], montagem.numbers('0,4-12/4,9-10'))
        # A volta pelo inverso: de trás para a frente.
        self.assertEqual([0, 1, 2, 3, 2, 1, 0], montagem.numbers('0-3,2-0'))
        self.assertEqual([0, 1, 1, 2], montagem.numbers([0, 1, 1, 2]))

    def test_shift_fills_with_the_background(self):
        frame = np.zeros((2, 5, 3), np.uint8)
        frame[:, :, 1] = 200            # verde
        frame[:, 2] = (10, 20, 30)      # o personagem
        moved = montagem.shift(frame, -2)
        self.assertTrue((moved[:, 0] == (10, 20, 30)).all())
        self.assertTrue((moved[:, 3:, 1] == 200).all())
        self.assertTrue((montagem.shift(frame, 0) == frame).all())

    def test_recipes_point_at_their_clips(self):
        root = Path(__file__).resolve().parents[3]
        for path in sorted((root / 'tools/sprites/montagens').glob('*.json')):
            char = path.stem
            recipe = json.loads(path.read_text(encoding='utf-8'))
            pack = json.loads((root / f'characters/{char}/character.json').read_text(encoding='utf-8'))
            for name, spec in recipe.items():
                if name.startswith('_'):
                    continue
                with self.subTest(clip=f'{char}_{name}'):
                    clip = json.loads((root / f'tools/sprites/clips/{char}_{name}.json').read_text(encoding='utf-8'))
                    quadros = montagem.numbers(spec['quadros'])
                    self.assertEqual(len(quadros), clip['expectedFrames'])
                    for state, a in spec.get('animacoes', {}).items():
                        anim = pack['animations'][state]
                        self.assertEqual(f'{char}_{name}', anim['atlas'])
                        self.assertEqual(montagem.numbers(a.get('frames', f'0-{len(quadros) - 1}')), anim['frames'])


if __name__ == '__main__':
    unittest.main()
