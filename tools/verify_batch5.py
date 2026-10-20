"""Verify exclusive batch 5 preservation and the real cumulative JAR."""
import json,subprocess,sys,re,zipfile,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE='e435ab9d4d9706859effb16f0ec2636c91b74c30'
MP='src/main/resources/data/minemon_gen1/runtime_species.json'
VP='src/main/resources/data/minemon_gen1/animation_validation_profiles.json'
def old(p):return subprocess.check_output(['git','show',BASE+':'+p],cwd=ROOT)
defs=json.loads((ROOT/MP).read_text());before=json.loads(old(MP))
profiles=json.loads((ROOT/VP).read_text());prior=json.loads(old(VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert defs[:40]==before[:40] and defs[50:]==before[50:],'Out-of-batch manifest changed'
assert [d['slug'] for d in defs[40:50]]==['zubat','golbat','oddish','gloom','vileplume','paras','parasect','venonat','venomoth','diglett']
assert len(profiles)==40 and all(profiles[k]==v for k,v in prior.items()),'Prior profiles changed'
for d in defs[:50]:
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
allowed={str(Path('src/main/resources')/p) for d in defs[40:50] for p in [d['model'],d['texture'],d['animation']]+d['licenses'] if '1.8.2' in p or '_cobblemon_1_8_2/' in p}
changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources/assets/minemon'],cwd=ROOT,text=True).splitlines()
assert set(changed)<=allowed,set(changed)-allowed
for p in changed:
 assert subprocess.run(['git','cat-file','-e',BASE+':'+p],cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode!=0,'Existing resource overwritten: '+p
for d,b in zip(defs[40:50],before[40:50]):
 for k in ('mode','scale','width','height','health','attack','speed','eggBase','eggSpots','sounds','slug','nationalPokedexNumber'):
  assert d[k]==b[k],(d['slug'],k)
 assert d['source_cobblemon_version']=='1.8.2'
 geo=json.loads((ROOT/'src/main/resources'/d['model']).read_text())['minecraft:geometry'][0]
 bones={b['name']:b for b in geo['bones']}
 assert len(bones)==len(geo['bones'])
 for name,bone in bones.items():
  seen={name};parent=bone.get('parent')
  while parent:
   assert parent in bones and parent not in seen,(d['slug'],name,parent)
   seen.add(parent);parent=bones[parent].get('parent')
  for field in ('pivot','rotation'):
   assert all(isinstance(v,(int,float)) and math.isfinite(v) for v in bone.get(field,[]))
  for cube in bone.get('cubes',[]):
   assert all(math.isfinite(v) for v in cube['origin'])
   assert all(math.isfinite(v) and v>=0 for v in cube['size'])
 anims=json.loads((ROOT/'src/main/resources'/d['animation']).read_text())['animations']
 assert d['idle'].startswith('animation.'+d['slug']+'.') and 'minemon_' not in d['idle']
 assert 'minemon_' not in d['walk'] and not d.get('movement_fallback')
 assert not re.search(r'\b(?:q|query)\.r\.|Math\.',json.dumps({k:v.get('bones',{}) for k,v in anims.items()}))
 for state,joints in profiles[d['slug']].items():assert set(joints)<=bones
 assert 'idle_static' not in profiles[d['slug']],'Batch 5 native idle is animated'
 if d['mode']=='flying':
  assert d['idle'].endswith('.air_idle') and d['walk'].endswith('.air_fly') and 'ground_run' not in d['clips']
 assert bool(d['clips'].get('ground_run'))==(d['slug'] in ('oddish','parasect'))
 for name,a in anims.items():
  for field in ('sound_effects','particle_effects'):
   if field in a:assert isinstance(a[field],dict) and all(isinstance(v,dict) for v in a[field].values())
  if d['slug']=='zubat':
   folded=name.endswith(('.shoulder_left','.shoulder_right'))
   assert a['bones']['wings_folded']['scale']==([1,1,1] if folded else [0,0,0])
   assert a['bones']['wings_open']['scale']==([0,0,0] if folded else [1,1,1])
print('PASS BATCH 5: exactly 50 updated; 40 previous and other 141 entries preserved; hierarchy, expressions, events and attributes checked')
def normalize(value):
 if isinstance(value,str):return re.sub(r'\bq\.','query.',value)
 if isinstance(value,list):return [normalize(x) for x in value]
 if isinstance(value,dict):return {k:normalize(x) for k,x in value.items()}
 return value
if len(sys.argv)>1:
 with zipfile.ZipFile(sys.argv[1]) as z:
  assert json.loads(z.read('data/minemon_gen1/runtime_species.json'))==defs
  assert json.loads(z.read('data/minemon_gen1/animation_validation_profiles.json'))==profiles
  for d in defs[:50]:
   for k in ('model','texture','animation'):assert d[k].replace('assets/minemon/','assets/minemon_gen1/',1) in z.namelist()
  for d in defs[40:50]:
   source=json.loads((ROOT/'src/main/resources'/d['animation']).read_text())['animations']
   packed=json.loads(z.read(d['animation'].replace('assets/minemon/','assets/minemon_gen1/',1)))['animations']
   for name,a in source.items():
    for event in ('sound_effects','particle_effects','timeline'):
     if event in a:assert packed[name][event]==normalize(a[event]),(d['slug'],name,event)
 print('PASS JAR BATCH 5: all first 50 resources, cumulative profiles and native sound/timeline payloads packaged; 151 roster preserved')
