import sys
import tempfile
import unittest
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import normal_map


def disco(lado=96):
    """Um disco opaco no meio de um quadro transparente (RGBA 0..1)."""
    ys, xs = np.mgrid[0:lado, 0:lado].astype(np.float32)
    dentro = ((xs - lado / 2) ** 2 + (ys - lado / 2) ** 2) < (lado * 0.38) ** 2
    q = np.zeros((lado, lado, 4), np.float32)
    q[dentro] = (0.8, 0.6, 0.5, 1.0)
    return q


def angulo(a, b):
    return np.degrees(np.arccos(np.clip(np.sum(a * b, axis=2), -1, 1)))


class NormalMapTests(unittest.TestCase):
    def test_a_silhueta_aponta_para_fora_do_corpo(self):
        q = disco()
        n = normal_map.silhueta(q)
        lado = q.shape[0]
        # À direita do centro a normal vai para a direita; embaixo, para baixo (y da imagem).
        self.assertGreater(n[lado // 2, int(lado * 0.82), 0], 0.3)
        self.assertLess(n[lado // 2, int(lado * 0.18), 0], -0.3)
        self.assertGreater(n[int(lado * 0.82), lado // 2, 1], 0.3)
        self.assertGreater(n[lado // 2, lado // 2, 2], 0.95)

    def test_inflar_da_volume_no_miolo_e_olha_para_a_frente_no_centro(self):
        q = disco()
        n = normal_map.inflar(q)
        lado = q.shape[0]
        self.assertGreater(n[lado // 2, lado // 2, 2], 0.97)
        # A meio caminho da borda já inclina (a silhueta borrada ali ainda é quase chapada).
        self.assertGreater(n[lado // 2, int(lado * 0.72), 0], 0.15)

    def test_gravar_e_ler_devolvem_o_mapa(self):
        q = disco()
        n = normal_map.reduzir(normal_map.inflar(q), q)
        mapa = np.round((n * 0.5 + 0.5) * 255).astype(np.uint8)
        with tempfile.TemporaryDirectory() as pasta:
            sem_perda = Path(pasta) / 'a_n.webp'
            normal_map.gravar(mapa, sem_perda, 100)
            np.testing.assert_array_equal(normal_map.ler(sem_perda)[..., :2], mapa[..., :2])
            com_perda = Path(pasta) / 'b_n.webp'
            normal_map.gravar(mapa, com_perda)
            lido = normal_map.ler(com_perda)
            self.assertEqual(lido.shape, mapa.shape)
            # Com perda: o erro em x e y fica em poucos níveis de 255.
            erro = np.abs(lido[..., :2].astype(int) - mapa[..., :2].astype(int))
            self.assertLess(erro.mean(), 2.0)

    def test_o_mapa_reduzido_lido_como_o_shader_fica_perto_da_normal_cheia(self):
        q = disco(128)
        n = normal_map.com_borda(normal_map.inflar(q), q)
        pequeno = normal_map.reduzir(n, q)
        self.assertEqual(pequeno.shape[:2], (32, 32))
        mapa = np.round((pequeno * 0.5 + 0.5) * 255).astype(np.uint8)
        volta = normal_map.do_mapa(mapa, (0, 0, 128, 128), (128, 128))
        dentro = q[..., 3] > 0.5
        self.assertLess(angulo(n, volta)[dentro].mean(), 4.0)

    def test_estender_leva_o_corpo_para_fora(self):
        q = disco()
        dentro = q[..., 3] > 0.5
        v = np.zeros(q.shape[:2] + (3,), np.float32)
        v[dentro] = (0.2, 0.4, 0.6)
        cheio = normal_map.estender(v, dentro)
        np.testing.assert_allclose(cheio[0, 0], (0.2, 0.4, 0.6), atol=1e-6)


if __name__ == '__main__':
    unittest.main()
