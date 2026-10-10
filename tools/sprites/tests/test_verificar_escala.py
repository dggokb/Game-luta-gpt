"""Escala confirmada somente quando as imagens realmente correspondem.

Este teste usa o atlas REAL de idle do P01 do jogo para gerar alterações
mensuráveis de zoom e translação, não apenas labels de diagnóstico.
"""
import sys
import unittest
from pathlib import Path
import numpy as np
import cv2
from PIL import Image

TOOLS=Path(__file__).resolve().parents[1]
ROOT=TOOLS.parents[1]
sys.path.insert(0,str(TOOLS))
import verificar_escala as geo


class GeometricScaleProofTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        image=ROOT/"android/app/src/main/res/drawable-nodpi/player_base_idle.png"
        if not image.is_file():
            raise AssertionError("Atlas P01 original ausente: a prova não pode ser simulada")
        with Image.open(image) as im:
            body=np.asarray(im.convert("RGBA").crop((0,0,331,476)))
        cls.base=np.zeros((690,680,4),dtype=np.uint8)
        cls.base[:,:,:3]=128
        cls.base[70:70+476,150:150+331]=body

    @classmethod
    def affine(cls,zoom,offset=(12,9)):
        m=np.array([[zoom,0.,offset[0]],[0.,zoom,offset[1]]],dtype=np.float32)
        return cv2.warpAffine(cls.base,m,(cls.base.shape[1],cls.base.shape[0]),
            flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,
            borderValue=(128,128,128,0))

    def test_genuine_reduction_is_measured_bidirectionally(self):
        result=geo.estimate_pair(self.base,self.affine(.82))
        self.assertEqual("CONFIRMADO_GEOMETRIA",result["status"],result)
        self.assertAlmostEqual(.82,result["fator"],delta=.035)

    def test_genuine_growth_is_measured(self):
        result=geo.estimate_pair(self.base,self.affine(1.18,(-55,-40)))
        self.assertEqual("CONFIRMADO_GEOMETRIA",result["status"],result)
        self.assertAlmostEqual(1.18,result["fator"],delta=.045)

    def test_same_size_different_translation_is_not_false_resize(self):
        result=geo.estimate_pair(self.base,self.affine(1.,(26,13)))
        self.assertEqual("CONFIRMADO_GEOMETRIA",result["status"],result)
        self.assertAlmostEqual(1.,result["fator"],delta=.025)

    def test_stable_series_confirms_and_unstable_series_fails(self):
        stable=geo.estimate_sequence(self.base,[self.affine(z)
            for z in (.82,.82,.83,.81,.82)])
        self.assertEqual("ESCALA_GEOMETRICA_ESTAVEL",stable["status"],stable)
        self.assertAlmostEqual(.82,stable["fator_mediano"],delta=.03)
        unstable=geo.estimate_sequence(self.base,[self.affine(z)
            for z in (.70,.82,1.0,1.15,1.2)])
        self.assertEqual("INSTAVEL_GEOMETRICAMENTE",unstable["status"],unstable)

    def test_unrelated_character_or_low_detail_must_not_pass(self):
        unrelated=np.zeros_like(self.base)
        unrelated[:,:,0:3]=128
        r=geo.estimate_pair(self.base,unrelated)
        self.assertEqual("INCONCLUSIVO",r["status"],r)
        # One good image out of five is insufficient to certify a clip.
        result=geo.estimate_sequence(self.base,[
            self.affine(.82),unrelated,unrelated,unrelated,unrelated])
        self.assertEqual("INCONCLUSIVO",result["status"])

    def test_corrupted_or_invalid_rgba_fails_closed(self):
        self.assertEqual("INCONCLUSIVO",
            geo.estimate_pair(self.base,self.base[:,:,:3])["status"])


if __name__=="__main__":
    unittest.main()
