import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
try:  # mecanicas.py mede as folhas com numpy (ferramentas de autoria); a CI instala só o Pillow
    import mecanicas  # noqa: E402
except ModuleNotFoundError as missing:
    if missing.name != 'numpy':
        raise
    raise unittest.SkipTest('numpy only in the authoring tools')

ROOT = Path(__file__).resolve().parents[3]


def shown_at_contact(anim, m):
    """Índice do quadro na tela no primeiro frame ativo (mesma conta do Animation.frame)."""
    d = anim['durationsMs']
    total = m['startupFrames'] + m['activeFrames'] + m['recoveryFrames']
    t, i = m['startupFrames'] / total * sum(d), 0
    while i < len(d) - 1 and t >= d[i]:
        t -= d[i]
        i += 1
    return i


class MecanicasTest(unittest.TestCase):
    data = json.loads(mecanicas.DATA.read_text(encoding='utf-8'))

    def test_packs_follow_mecanicas_json(self):
        for cid, spec in self.data['personagens'].items():
            pack, path, _ = mecanicas.apply(cid, spec, self.data['base'])
            current = json.loads(path.read_text(encoding='utf-8'))
            self.assertEqual(current, pack, f'{cid}: rode tools/sprites/mecanicas.py --write')

    def test_commands_in_arrows(self):
        self.assertEqual(mecanicas.parse_command('↓↘→'), ([3, 2, 1], False))
        self.assertEqual(mecanicas.parse_command('←↙↓↘→'), ([5, 4, 3, 2, 1], False))
        self.assertEqual(mecanicas.parse_command('[←] →'), ([5, 1], True))
        self.assertEqual(mecanicas.parse_command('[↓] ↑'), ([3, 7], True))
        with self.assertRaises(SystemExit):
            mecanicas.parse_command('↓ x →')
        for cid, spec in self.data['personagens'].items():
            self.assertEqual(sorted(spec['comandos']), ['S1', 'S2', 'S3', 'S4'], cid)

    def test_impact_frame_is_on_screen_when_the_move_hits(self):
        for cid, spec in self.data['personagens'].items():
            if spec.get('manterTempo'):
                continue
            pack = json.loads((ROOT / f'characters/{cid}/character.json').read_text(encoding='utf-8'))
            attacks = [(k, m['animation'], m) for k, m in
                       list(pack['moves'].items()) + list(pack.get('specialMoves', {}).items()) if 'animation' in m]
            if 'S' in pack.get('specialAnimations', {}):
                attacks.append(('S', pack['specialAnimations']['S'], pack['fighter']['energy']['attack']))
            for key, anim_id, m in attacks:
                anim = pack['animations'][anim_id]
                if 'impactFrame' in anim:
                    self.assertEqual(anim['impactFrame'], shown_at_contact(anim, m), f'{cid}/{key}')

    def test_specials_follow_the_especiais_block(self):
        for cid, spec in self.data['personagens'].items():
            pack = json.loads((ROOT / f'characters/{cid}/character.json').read_text(encoding='utf-8'))
            for key, e in spec.get('especiais', {}).items():
                m = pack['specialMoves'][key]
                if 'projetil' in e:
                    # Lança num quadro só e não tem caixa de golpe: quem acerta é o projétil.
                    self.assertEqual(1, m['activeFrames'], f'{cid}/{key}')
                    self.assertNotIn('reach', m, f'{cid}/{key}')
                    self.assertEqual(e['projetil']['visual'], m['projectile']['visual'], f'{cid}/{key}')
                self.assertEqual(bool(e.get('baixo')), bool(m.get('low')), f'{cid}/{key}')
                self.assertEqual(e.get('efeito'), m.get('efeito'), f'{cid}/{key}')

    def test_character_numbers(self):
        for cid, spec in self.data['personagens'].items():
            pack = json.loads((ROOT / f'characters/{cid}/character.json').read_text(encoding='utf-8'))
            self.assertEqual(spec['vida'], pack['fighter']['maxLife'], cid)
            self.assertEqual(spec['projetil']['velocidade'], pack['fighter']['energy']['speed'], cid)


if __name__ == '__main__':
    unittest.main()
