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

    def test_three_easy_frames_cannot_certify_seven_frame_clip(self):
        self.assertEqual("INCONCLUSIVO",geo.estimate_sequence(
            self.base,[self.affine(1.0),self.affine(1.0),
                       self.affine(1.0)]+[
                       np.zeros_like(self.base) for _ in range(4)])["status"])
        # Four good frames still fail a 70% requirement.
        self.assertEqual("INCONCLUSIVO",geo.estimate_sequence(
            self.base,[self.affine(1.0)]*4+[
                       np.zeros_like(self.base) for _ in range(3)])["status"])

    def test_temporal_coverage_must_include_end_of_animation(self):
        frames=[self.affine(1.0)]*5+[np.zeros_like(self.base)]*2
        self.assertEqual("INCONCLUSIVO",
                         geo.estimate_sequence(self.base,frames)["status"])

    def test_video_non_chroma_must_not_be_measured_as_character(self):
        from tempfile import TemporaryDirectory
        with TemporaryDirectory() as d:
            for name,background_green in [("green",True),("red",False)]:
                out=Path(d)/(name+".mp4")
                codec=cv2.VideoWriter_fourcc(*"mp4v")
                writer=cv2.VideoWriter(str(out),codec,24.,(96,96))
                self.assertTrue(writer.isOpened(),"OpenCV mp4v writer unavailable")
                for frame in range(20):
                    canvas=np.full((96,96,3),
                        (0,255,0) if background_green else (0,0,255),
                        dtype=np.uint8)
                    cv2.rectangle(canvas,(35,20),(65,88),(30,40,185),-1)
                    writer.write(canvas)
                writer.release()
                samples=geo.read_video_samples(out)
                if background_green:
                    self.assertEqual(7,len(samples),"Green source must remain usable")
                else:
                    self.assertEqual([],samples,"A colored stage is not green-screen")

    def test_source_atlas_agreement_and_disagreement_are_distinct(self):
        a={"status":"ESCALA_GEOMETRICA_ESTAVEL","fator_mediano":.82}
        b={"status":"ESCALA_GEOMETRICA_ESTAVEL","fator_mediano":.84}
        wrong=geo.compare_source_and_atlas(a,b)
        self.assertEqual("ESCALA_FORA_DO_IDLE",wrong["status"],
                         "Two equally small fighters must never pass")
        self.assertEqual("PENDENTE",wrong["validacao_runtime"])
        ok=geo.compare_source_and_atlas({**a,"fator_mediano":1.02},
                                        {**b,"fator_mediano":1.04})
        self.assertEqual("PADRONIZADA_FONTE_ATLAS",ok["status"])
        fail=geo.compare_source_and_atlas(a,{**b,"fator_mediano":1.06})
        self.assertEqual("ESCALA_DIVERGENTE",fail["status"])
        self.assertGreater(fail["diferenca_relativa"],.15)
        self.assertEqual("INCONCLUSIVO",geo.compare_source_and_atlas(
            a,{"status":"INCONCLUSIVO"})["status"])

    def test_atlas_pixel_scale_normalizes_to_idle(self):
        import json
        from tempfile import TemporaryDirectory
        with TemporaryDirectory() as d:
            root=Path(d)
            report_dir=root/"tools/sprites/reports"
            image_dir=root/"android/app/src/main/res/drawable-nodpi"
            report_dir.mkdir(parents=True)
            image_dir.mkdir(parents=True)
            pack={"animations":{
                "IDLE":{"atlas":"test_idle","frames":[0]},
                "DASH":{"atlas":"test_dash","frames":[0]}
            }}
            for name,ps in (("idle",2),("dash",1)):
                atlas="test_"+name
                payload={"packed":{"frameWidth":30,"frameHeight":50,
                                   "columns":1,"pixelScale":ps}}
                (report_dir/(atlas+".report.json")).write_text(json.dumps(payload))
                Image.new("RGBA",(30,50),(100,130,180,255)).save(image_dir/(atlas+".png"))
            idle=geo.read_sprite_samples(root,pack,"IDLE")
            dash=geo.read_sprite_samples(root,pack,"DASH")
            self.assertEqual((50,30,4),idle[0].shape)
            self.assertEqual((100,60,4),dash[0].shape)
            meta=report_dir/"test_dash.report.json"
            meta.write_text(json.dumps({"packed":{"frameWidth":30,"frameHeight":50,
                        "columns":1,"pixelScale":0}}))
            self.assertEqual([],geo.read_sprite_samples(root,pack,"DASH"))

    def test_corrupted_or_invalid_rgba_fails_closed(self):
        self.assertEqual("INCONCLUSIVO",
            geo.estimate_pair(self.base,self.base[:,:,:3])["status"])


if __name__=="__main__":
    unittest.main()
