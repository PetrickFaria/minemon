#!/usr/bin/env python3
"""Verify Lot 6 exclusivity, cumulative preservation, runtime assets, and JAR contents."""
import json, math, re, struct, subprocess, sys, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BASE='eae973354e05a49dc2cd4522006c07585dbd82af'
MP='src/main/resources/data/minemon_gen1/runtime_species.json'
VP='src/main/resources/data/minemon_gen1/animation_validation_profiles.json'
EXPECTED=['dugtrio','meowth','persian','psyduck','golduck','mankey','primeape','growlithe','arcanine','poliwag']
PATCHED_PRIOR_ASSETS={
 'assets/minemon/animations/pidgeotto_cobblemon_1_8_2/pidgeotto.animation.json',
 'assets/minemon/animations/pidgeot_cobblemon_1_8_2/pidgeot.animation.json',
}
def base_file(path):
 return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
def without_wing_visibility(data):
 animations=json.loads(data)['animations']
 for clip in animations.values():
  bones=clip.get('bones',{})
  for group in ('wings_open','wings_closed'):
   if group in bones:
    bones[group].pop('scale',None)
    if not bones[group]:bones.pop(group)
 return animations
def normalize(value):
 if isinstance(value,str):return re.sub(r'\bq\.', 'query.',value)
 if isinstance(value,list):return [normalize(x) for x in value]
 if isinstance(value,dict):return {k:normalize(v) for k,v in value.items()}
 return value

defs=json.loads((ROOT/MP).read_text());old_defs=json.loads(base_file(MP))
profiles=json.loads((ROOT/VP).read_text());old_profiles=json.loads(base_file(VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert [d['slug'] for d in defs[50:60]]==EXPECTED
assert defs[:50]==old_defs[:50] and defs[60:]==old_defs[60:],'Pokémon outside #051–#060 changed'
assert len(old_profiles)==40 and all(profiles.get(k)==v for k,v in old_profiles.items()),'Previous profiles changed'
assert set(profiles)==set(old_profiles)|set(EXPECTED)

# Preserve every old asset byte-for-byte, including license and shared species files.
old_paths=set()
for d in old_defs[:50]:
 old_paths.update([d['model'],d['texture'],d['animation']]+d['licenses'])
for p in old_paths:
 current=(ROOT/'src/main/resources'/p).read_bytes()
 original=base_file('src/main/resources/'+p)
 if p in PATCHED_PRIOR_ASSETS:
  assert without_wing_visibility(current)==without_wing_visibility(original),'Unrelated Pidge wing animation changed: '+p
 else:
  assert current==original,'Prior batch asset changed: '+p

allowed=set()
for d in defs[50:60]:
 before=old_defs[d['nationalPokedexNumber']-1]
 for key in ('mode','scale','width','height','health','attack','speed','eggBase','eggSpots','sounds','slug','nationalPokedexNumber'):
  assert d[key]==before[key],(d['slug'],key)
 assert d['source_cobblemon_version']=='1.8.2'
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 allowed.update([d['model'],d['texture'],d['animation']]+d['licenses'])
allowed_paths={str(Path('src/main/resources')/path) for path in allowed}
geo_by_slug={};animation_by_slug={}
for d in defs[50:60]:
 geo=json.loads((ROOT/'src/main/resources'/d['model']).read_text())['minecraft:geometry']
 bones={}
 for part in geo:
  for bone in part['bones']:
   name=bone['name'];assert name not in bones,(d['slug'],name);bones[name]=bone
 for name,bone in bones.items():
  seen={name};parent=bone.get('parent')
  while parent:
   assert parent in bones and parent not in seen,(d['slug'],name,parent)
   seen.add(parent);parent=bones[parent].get('parent')
  for axis in ('pivot','rotation'):
   assert all(isinstance(n,(int,float)) and math.isfinite(n) for n in bone.get(axis,[]))
 for bone in bones.values():
  for cube in bone.get('cubes',[]):
   assert all(math.isfinite(v) for v in cube['origin']+cube['size'])
 tex=(ROOT/'src/main/resources'/d['texture']).read_bytes()
 assert tex[:8]==b'\x89PNG\r\n\x1a\n'
 dims=struct.unpack('>II',tex[16:24]);desc=geo[0]['description']
 assert dims==(desc['texture_width'],desc['texture_height']),(d['slug'],dims)
 animation=json.loads((ROOT/'src/main/resources'/d['animation']).read_text())['animations']
 for clip,a in animation.items():
  missing=set(a.get('bones',{}))-set(bones)
  assert missing==set(d.get('stripped_bones',{}).get(clip,[])),(d['slug'],clip,missing)
 for clip in d['clips'].values():
  assert clip in animation or '.minemon_' in clip,(d['slug'],clip)
 for state in ('idle','walk','run'):
  assert state in profiles[d['slug']] and set(profiles[d['slug']][state])<=set(bones),(d['slug'],state)
 geo_by_slug[d['slug']]=bones;animation_by_slug[d['slug']]=animation
 assert d['idle'] in animation and d['walk'] in animation

assert [d['clips'].get('ground_run') for d in defs[50:60]].count(None)==9
assert defs[58]['clips'].get('ground_run')=='animation.arcanine.ground_run'
assert defs[57]['source_procedural_pose']['poser'].endswith('/0058_growlithe/growlithe.json')
assert defs[57]['walk'].endswith('.minemon_poser_walk')
assert all(defs[i].get('source_static_idle') and profiles[defs[i]['slug']]['idle_static']==profiles[defs[i]['slug']]['idle'] for i in (51,52,57,59))
assert all(defs[i]['movement_fallback']=='idle' and defs[i]['walk']==defs[i]['idle'] for i in (51,52))
assert defs[59]['idle'].endswith('ground_idle_placeholder') and defs[59]['walk'].endswith('ground_walk_placeholder')
assert all(d['slug'] not in ('charmander','charizard') for d in defs[50:60])

changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources/assets/minemon'],cwd=ROOT,text=True).splitlines()
patched_prior_paths={str(Path('src/main/resources')/p) for p in PATCHED_PRIOR_ASSETS}
assert set(changed)<=allowed_paths|patched_prior_paths,set(changed)-allowed_paths-patched_prior_paths
changed_resources=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
assert set(changed_resources)<=allowed_paths|{MP,VP}|patched_prior_paths,set(changed_resources)-allowed_paths-{MP,VP}-patched_prior_paths
for d in defs[50:60]:
 for key in ('model','texture','animation'):
  assert '_cobblemon_1_8_2/' in d[key]
print('PASS BATCH 6: exactly #051–#060 imported; first 50 assets byte-preserved; remaining 91 definitions unchanged; source geometry, texture dimensions, bones, clips, and gait profiles validated')

if len(sys.argv)>1:
 with zipfile.ZipFile(sys.argv[1]) as z:
  packed_defs=json.loads(z.read('data/minemon_gen1/runtime_species.json'))
  packed_profiles=json.loads(z.read('data/minemon_gen1/animation_validation_profiles.json'))
  assert packed_defs==defs and packed_profiles==profiles
  dex=json.loads(z.read('data/minemon_gen1/pokedex.json'))
  assert len(dex)==151 and [x['nationalPokedexNumber'] for x in dex]==list(range(1,152))
  assert {d['slug'] for d in defs}=={d['slug'] for d in dex if d['registered']}
  for d in defs[:60]:
   for kind in ('model','texture','animation'):
    path=d[kind].replace('assets/minemon/','assets/minemon_gen1/',1)
    assert path in z.namelist(),(d['slug'],kind,path)
  for d in defs[50:60]:
   path=d['animation'].replace('assets/minemon/','assets/minemon_gen1/',1)
   packed=json.loads(z.read(path))['animations']
   source=animation_by_slug[d['slug']]
   for name,a in source.items():
    for event in ('sound_effects','particle_effects','timeline'):
     if event in a:assert packed[name][event]==normalize(a[event]),(d['slug'],name,event)
  print('PASS JAR BATCH 6: JAR contains all first 60 resources, cumulative profiles, all 151 registered species, and unchanged native event payloads')
