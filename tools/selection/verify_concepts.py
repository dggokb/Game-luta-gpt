from pathlib import Path
import re
from PIL import Image
root=Path(__file__).resolve().parents[2]
for i in range(1,17):
 p=root/'art'/'concepts'/f'p{i:02d}.png'
 with Image.open(p) as im:
  im.load()
  assert im.width>300 and im.height>300
for i in range(1,9):
 if i==5:continue
 p=root/'android/app/src/main/assets/selection/art'/f'hero_p{i:02d}.png'
 with Image.open(p) as im:
  im.load();assert im.mode=='RGBA' and im.getchannel('A').getextrema()[0]==0
java=(root/'android/app/src/main/java/com/gamelutagpt/GeneratedCharacters.java').read_text()
m=re.search(r'SELECTABLE\s*=\s*new String\[\]\s*\{([^}]+)\}',java)
assert m
ids=re.findall(r'"([^"]+)"',m.group(1))
assert set(ids)=={'player_base','p02','p03','p04','p05','p06','p07','p08','p09','p10','p12'}
print('16 complete concepts, 11 playable characters; remaining future portraits never enter gameplay roster')
