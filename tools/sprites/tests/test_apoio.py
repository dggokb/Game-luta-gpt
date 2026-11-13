"""O pé de apoio medido no desenho (tools/sprites/apoio.py): o atrito do pé no chão."""
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'tools/sprites'))
import apoio  # noqa: E402


def pack(nome):
    return json.loads((ROOT / f'characters/{nome}/character.json').read_text(encoding='utf-8'))


class MarcaDaFolha(unittest.TestCase):
    """Sem OpenCV (o CI): só a conferência da medida gravada."""

    def test_a_medida_gravada_e_da_folha_de_hoje(self):
        for caminho in sorted((ROOT / 'characters').glob('*/character.json')):
            p = json.loads(caminho.read_text(encoding='utf-8'))
            for nome in apoio.LOCOMOCAO:
                anim = p['animations'].get(nome)
                if anim and 'apoio' in anim:
                    self.assertEqual(apoio.marca_da_folha(anim, ROOT), anim['apoio']['folha'], f"{p['id']}.{nome}")

    def test_trocar_os_quadros_muda_a_marca(self):
        anim = pack('p02')['animations']['WALK_FORWARD']
        outra = dict(anim, frames=anim['frames'][::-1])
        self.assertNotEqual(apoio.marca_da_folha(anim, ROOT), apoio.marca_da_folha(outra, ROOT))


@unittest.skipIf(apoio.cv2 is None, 'medir precisa de OpenCV e numpy')
class Medida(unittest.TestCase):
    def setUp(self):
        apoio.usar_raiz(ROOT)   # os testes do build apontam o medidor para uma pasta à parte

    def test_a_medida_gravada_e_a_que_o_detector_da(self):
        for nome in ('p02', 'player_base', 'p07'):
            p = pack(nome)
            for anim in apoio.LOCOMOCAO:
                self.assertEqual(p['animations'][anim]['apoio'], apoio.medir(p, anim), f'{nome}.{anim}')

    def test_o_andar_da_yinlu_anda_para_o_lado_certo_em_todo_quadro_medido(self):
        p = pack('p02')
        frente = p['animations']['WALK_FORWARD']['apoio']
        tras = p['animations']['WALK_BACK']['apoio']
        self.assertNotIn('inseguro', frente)
        self.assertNotIn('inseguro', tras)
        self.assertGreater(sum(frente['avanco']), 80)
        self.assertLess(sum(tras['avanco']), -60)
        self.assertTrue(all(v > -0.5 for v in frente['avanco']))

    def test_andar_para_tras_desenhado_para_a_frente_nao_vai_para_o_motor(self):
        tras = pack('p07')['animations']['WALK_BACK']['apoio']
        self.assertTrue(tras.get('aoContrario'))
        self.assertGreater(sum(tras['avanco']), 0)

    def test_desenho_que_marca_passo_fica_inseguro(self):
        self.assertIn('quase não anda', pack('p03')['animations']['WALK_FORWARD']['apoio']['inseguro'])


if __name__ == '__main__':
    unittest.main()
