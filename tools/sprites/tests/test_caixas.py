import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import caixas


class CaixasTests(unittest.TestCase):
    def setUp(self):
        self.root = Path(tempfile.mkdtemp())
        (self.root / 'characters/p07').mkdir(parents=True)
        shutil.copy(caixas.ROOT / 'characters/p07/character.json', self.root / 'characters/p07/character.json')
        self.pack = json.loads((self.root / 'characters/p07/character.json').read_text(encoding='utf-8'))

    def tearDown(self):
        shutil.rmtree(self.root)

    def data(self, *items):
        return {'tipo': 'ajuste-caixas', 'ajustes': [
            {'personagem': 'p07', 'campo': c, 'antes': a, 'valor': v} for c, a, v in items]}

    def test_applies_body_move_and_projectile(self):
        body = self.pack['fighter']['body']['halfWidth']
        reach = self.pack['moves']['L']['reach']
        spawn = self.pack['fighter']['energy']['spawnX']
        caixas.apply(self.data(('fighter.body.halfWidth', body, body + 6), ('moves.L.reach', reach, reach + 10),
                               ('fighter.energy.spawnX', spawn, spawn - 4)), self.root)
        out = json.loads((self.root / 'characters/p07/character.json').read_text(encoding='utf-8'))
        self.assertEqual(body + 6, out['fighter']['body']['halfWidth'])
        self.assertEqual(reach + 10, out['moves']['L']['reach'])
        self.assertEqual(spawn - 4, out['fighter']['energy']['spawnX'])
        self.assertEqual(self.pack['moves']['M'], out['moves']['M'])

    def test_refuses_when_the_pack_changed_since(self):
        reach = self.pack['moves']['L']['reach']
        with self.assertRaises(SystemExit):
            caixas.apply(self.data(('moves.L.reach', reach + 2, reach + 10)), self.root)

    def test_refuses_unknown_field(self):
        with self.assertRaises(SystemExit):
            caixas.apply(self.data(('moves.ZZ.reach', 10, 20)), self.root)


if __name__ == '__main__':
    unittest.main()
