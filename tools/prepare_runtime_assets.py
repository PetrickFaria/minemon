#!/usr/bin/env python3
"""Adapt copied runtime animations to GeckoLib 3; original assets remain intact."""
import json,re,sys
from pathlib import Path
root=Path(sys.argv[1])
count=0
manifest=root/'data/minemon/runtime_species.json'
definitions={d['animation']:d for d in json.loads(manifest.read_text())} if manifest.exists() else {}
for p in (root/'assets/minemon/animations').rglob('*.animation.json'):
 data=json.loads(p.read_text())
 def normalize(v):
  if isinstance(v,str): return re.sub(r'\bq\.', 'query.',v)
  if isinstance(v,list): return [normalize(x) for x in v]
  if isinstance(v,dict): return {k:normalize(x) for k,x in v.items()}
  return v
 data=normalize(data)
 definition=definitions.get(str(p.relative_to(root)),{})
 for name in definition.get('excluded_clips',[]): data.get('animations',{}).pop(name,None)
 for name,anim in data.get('animations',{}).items():
  if 'animation_length' not in anim: anim['animation_length']=4.0
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
 count+=1
print('GeckoLib 3 runtime animations prepared:',count)
