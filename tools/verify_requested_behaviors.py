#!/usr/bin/env python3
"""Resource checks; this does not claim to exercise Minecraft AI or rendering."""
import json,sys,math
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
assets=ROOT/'src/main/resources/assets/minemon'
images=list((assets/'textures/item').glob('*_treat.png'))
assert len(images)==18
masks=[Image.open(p).getchannel('A').tobytes() for p in images]
assert all(m==masks[0] for m in masks),'Treat silhouettes differ'
reference=Image.open(ROOT/'tools/reference/minecraft-1.19.2-bone.png').convert('RGBA')
assert all(m==reference.getchannel('A').tobytes() for m in masks),'Treat alpha differs from vanilla bone'
for path in images:
 image=Image.open(path).convert('RGBA')
 for y in range(16):
  for x in range(16):
   if reference.getpixel((x,y))[3]==0:assert image.getpixel((x,y))==reference.getpixel((x,y))
if len(sys.argv)>1:
 root=Path(sys.argv[1]);bounds=json.loads((root/'data/minemon_gen1/preview_bounds.json').read_text())
 manifest=json.loads((root/'data/minemon_gen1/runtime_species.json').read_text())
 assert len(bounds)==len(manifest)==151
 for d in manifest:
  a=json.loads((root/d['animation'].replace('assets/minemon/','assets/minemon_gen1/')).read_text())['animations']
  sleep=a['animation.'+d['slug']+'.minemon_night_sleep']
  awake=a['animation.'+d['slug']+'.minemon_awake_rest']
  assert not any(k in awake for k in ('sound_effects','particle_effects','timeline'))
  blink=set(a.get(d['clips'].get('blink'),{}).get('bones',{}))
  for bone,value in sleep['bones'].items():
   if bone in blink or any(k in bone.lower() for k in ('eye','lid','pupil')):continue
   assert awake['bones'][bone]==value,(d['slug'],bone)
  b=bounds[d['slug']];assert len(b)==6 and all(math.isfinite(v) for v in b)
  assert all(b[i+3]>=b[i] for i in range(3))
 print('PASS: 151 awake-rest body tracks equal sleep tracks; no sleep events; finite mesh bounds for 151 models')
print('PASS: exactly 18 treat textures with identical silhouettes')
