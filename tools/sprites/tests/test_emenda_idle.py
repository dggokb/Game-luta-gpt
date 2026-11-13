"""Regra do dono (13/11): todo golpe de chão termina no lugar da postura parada (IDLE ou
CROUCH_IDLE), sem pulo na troca. Golpe novo fora da regra falha aqui; os antigos que ainda
estão fora ficam em tools/sprites/emenda_idle_excecoes.json até serem corrigidos."""
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
try:  # numpy e as folhas de origem: sem eles (a CI só com Pillow) o teste é pulado
    import numpy  # noqa: F401
    import emendar_idle
except ImportError:
    emendar_idle = None


@unittest.skipIf(emendar_idle is None, 'precisa de numpy (requirements-animar.txt)')
class EmendaNoIdleTest(unittest.TestCase):
    def test_todo_golpe_termina_no_lugar_da_postura(self):
        excecoes = json.loads((ROOT / 'tools/sprites/emenda_idle_excecoes.json').read_text(encoding='utf-8'))
        fora, ja_certos = [], []
        for path in sorted((ROOT / 'characters').glob('*/character.json')):
            cid = path.parent.name
            try:
                medidas = emendar_idle.emendar(cid, write=False)
            except (KeyError, FileNotFoundError):
                continue   # pacote sem folhas de vídeo (player_two)
            lista = set(excecoes.get(cid, []))
            for r in medidas:
                ruim = abs(r['dx']) > emendar_idle.LIMITE
                if ruim and r['golpe'] not in lista:
                    fora.append(f"{cid} {r['golpe']} ({r['anim']}): {r['dx']:+.0f} px da postura")
                if not ruim and r['golpe'] in lista:
                    ja_certos.append(f"{cid} {r['golpe']}")
        self.assertEqual([], fora, 'golpe novo sem emendar no idle: rode emendar_idle.py <id> --write')
        self.assertEqual([], ja_certos, 'já emendam: tire de emenda_idle_excecoes.json')


if __name__ == '__main__':
    unittest.main()
