"""Negative scale controls using the actual P02 IDLE/DASH Android PNGs."""
import json
import sys
import unittest
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT/'tools/sprites'))
import escala_anatomica as engine

class AnatomicalScaleTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.cfg=json.loads((ROOT/'tools/sprites/anatomy/p02.json').read_text())
        cls.char=json.loads((ROOT/'characters/p02/character.json').read_text())
        cls.frames={}
        for kind in ('idle','dash'):
            packed=json.loads((ROOT/f'tools/sprites/reports/p02_{kind}.report.json').read_text())['packed']
            cls.frames[kind]=engine.cells(ROOT/f'android/app/src/main/res/drawable-nodpi/p02_{kind}.png',packed)
        cls.idle_ids=cls.char['animations']['IDLE']['frames']
        cls.dash_ids=cls.char['animations']['DASH']['frames']

    @staticmethod
    def scaled(im,k):
        return im.resize((round(im.width*k),round(im.height*k)),Image.Resampling.LANCZOS)

    def test_original_is_not_resized_without_evidence(self):
        _,rows,ids=engine.audit(self.frames['idle'],self.frames['dash'],self.idle_ids,self.dash_ids,self.cfg)
        self.assertEqual(ids,[])
        self.assertTrue(any(x['status']=='REVIEW' for x in rows))

    def test_single_small_frame_detected_and_restored(self):
        damaged=self.frames['dash'].copy();damaged[1]=self.scaled(damaged[1],0.85)
        _,rows,ids=engine.audit(self.frames['idle'],damaged,self.idle_ids,self.dash_ids,self.cfg)
        self.assertEqual(ids,[1],rows)
        self.assertAlmostEqual(rows[1]['size'],1/0.85,delta=.085)
        restored=self.scaled(damaged[1],rows[1]['size'])
        again=engine.landmark(restored,self.cfg)
        expected=engine.landmark(self.frames['dash'][1],self.cfg)
        self.assertAlmostEqual(again['envelope'],expected['envelope'],delta=6.)
        self.assertAlmostEqual(again['area'],expected['area'],delta=400)

    def test_single_large_frame_detected(self):
        damaged=self.frames['dash'].copy();damaged[0]=self.scaled(damaged[0],1.20)
        _,rows,ids=engine.audit(self.frames['idle'],damaged,self.idle_ids,self.dash_ids,self.cfg)
        self.assertEqual(ids,[0],rows)
        self.assertAlmostEqual(rows[0]['size'],1/1.2,delta=.07)

    def test_running_pose_cannot_fake_uniform_scale(self):
        _,rows,ids=engine.audit(self.frames['idle'],self.frames['dash'],self.idle_ids,self.dash_ids,self.cfg)
        self.assertNotIn(2,ids);self.assertNotIn(3,ids)
        self.assertEqual(rows[2]['status'],'REVIEW')

    def test_corrections_are_scoped_and_idle_preserved(self):
        data={'schema':1,'corrections':[{'character':'player_base','atlas':'player_base_victory',
              'frames':[[0,1,0,0]]}]}
        fixed=engine.corrections('p02','p02_dash',[0],[{'status':'CORRECTABLE','size':1.1}],data)
        self.assertEqual(fixed['corrections'][0],data['corrections'][0])
        self.assertFalse(any(x['atlas']=='p02_idle' for x in fixed['corrections']))
        self.assertEqual(fixed['corrections'][1]['frames'],[[0,1.1,0.,0.]])

if __name__=='__main__':unittest.main()
