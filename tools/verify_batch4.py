"""Lot 4 preservation gate against the approved Lot 3 commit."""
import json,subprocess,sys,zipfile,math,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE='8bba5b0563d2170a77c1546ea81e32e717569af1'
MANIFEST='src/main/resources/data/minemon_gen1/runtime_species.json'
PROFILES='src/main/resources/data/minemon_gen1/animation_validation_profiles.json'
def old(path):return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
defs=json.loads((ROOT/MANIFEST).read_text());before=json.loads(old(MANIFEST))
assert len(defs)==151
assert [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert defs[:30]==before[:30] and defs[40:]==before[40:],'Out-of-batch species changed'
assert [d['slug'] for d in defs[30:40]]==['nidoqueen','nidoranm','nidorino','nidoking','clefairy','clefable','vulpix','ninetales','jigglypuff','wigglytuff']
for d in defs[:40]:
 if d['nationalPokedexNumber']>10:assert d.get('source_cobblemon_version')=='1.8.2'
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
profiles=json.loads((ROOT/PROFILES).read_text())
assert all(profiles[k]==v for k,v in json.loads(old(PROFILES)).items()),'Earlier articulation profiles changed'
assert len(profiles)==30
# Only new, selected Lot 4 resource files may differ. Existing resource blobs,
# including all three starter flames and every preceding batch, remain exact.
changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources/assets/minemon'],cwd=ROOT,text=True).splitlines()
allowed={str(Path('src/main/resources')/p) for d in defs[30:40] for p in [d['model'],d['texture'],d['animation']]+d['licenses'] if '1.8.2' in p or '_cobblemon_1_8_2/' in p}
assert set(changed)<=allowed,set(changed)-allowed
for p in changed:
 exists=subprocess.run(['git','cat-file','-e',BASE+':'+p],cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0
 assert not exists,'Existing resource overwritten: '+p
static={'nidoqueen','ninetales','jigglypuff','wigglytuff'}
for d in defs[30:40]:
 anims=json.loads((ROOT/'src/main/resources'/d['animation']).read_text())['animations']
 idle=anims[d['idle']]
 if d['slug'] in static:
  assert d.get('source_static_idle') and profiles[d['slug']]['idle_static']==profiles[d['slug']]['idle']
  assert not re.search(r'\b(?:math|q|query)\.',json.dumps(idle))
  assert all(not isinstance(v,dict) for channel in idle['bones'].values() for v in channel.values())
 else:assert 'idle_static' not in profiles[d['slug']]
 if d.get('source_procedural_pose'):
  walk=anims[d['walk']]
  assert walk['loop'] and d['walk']!=d['idle']
  for joint,sign in d['source_procedural_pose']['joints'].items():
   expr=walk['bones'][joint]['rotation'][0]
   assert 'math.cos(q.anim_time*' in expr
  # Native pose channels outside the walking X overlay remain identical.
  for joint,channel in idle['bones'].items():
   if joint in d['stripped_bones'].get(d['idle'],[]):continue
   target=walk['bones'][joint]
   for key,value in channel.items():
    if key=='rotation' and joint in d['source_procedural_pose']['joints']:assert target[key][1:]==value[1:]
    else:assert target[key]==value
print('PASS BATCH 4: exactly 40 updated species; other 141 manifest entries and earlier profiles/resources preserved')
if len(sys.argv)>1:
 with zipfile.ZipFile(sys.argv[1]) as z:
  packaged=json.loads(z.read('data/minemon_gen1/runtime_species.json'))
  assert packaged==defs
  assert json.loads(z.read('data/minemon_gen1/animation_validation_profiles.json'))==profiles
  for d in defs[:40]:
   for key in ('model','texture','animation'):assert d[key].replace('assets/minemon/','assets/minemon_gen1/',1) in z.namelist()
 print('PASS JAR BATCH 4: all first 40 selected model/texture/animation resources and cumulative profiles packaged')
