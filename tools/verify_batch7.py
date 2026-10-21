#!/usr/bin/env python3
"""Verify Lot 7 exclusivity, cumulative preservation, runtime assets, and JAR contents."""
import json, math, re, struct, subprocess, sys, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BASE='6739e0019dcf11a61288b8f0b1cff5f1d2cccb7b'
MP='src/main/resources/data/minemon_gen1/runtime_species.json'
VP='src/main/resources/data/minemon_gen1/animation_validation_profiles.json'
EXPECTED=['poliwhirl','poliwrath','abra','kadabra','alakazam','machop','machoke','machamp','bellsprout','weepinbell']
PATCHED_PRIOR_ASSETS=set()
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
assert [d['slug'] for d in defs[60:70]]==EXPECTED
assert defs[:60]==old_defs[:60] and defs[70:]==old_defs[70:],'Pokémon outside #061–#070 changed'
assert len(old_profiles)==50 and all(profiles.get(k)==v for k,v in old_profiles.items()),'Previous profiles changed'
assert set(profiles)==set(old_profiles)|set(EXPECTED)

# Preserve every old asset byte-for-byte, including license and shared species files.
old_paths=set()
for d in old_defs[:60]:
 old_paths.update([d['model'],d['texture'],d['animation']]+d['licenses'])
for p in old_paths:
 current=(ROOT/'src/main/resources'/p).read_bytes()
 original=base_file('src/main/resources/'+p)
 if p in PATCHED_PRIOR_ASSETS:
  assert without_wing_visibility(current)==without_wing_visibility(original),'Unrelated Pidge wing animation changed: '+p
 else:
  assert current==original,'Prior batch asset changed: '+p

allowed=set()
for d in defs[60:70]:
 before=old_defs[d['nationalPokedexNumber']-1]
 for key in ('mode','scale','width','height','health','attack','speed','eggBase','eggSpots','sounds','slug','nationalPokedexNumber'):
  assert d[key]==before[key],(d['slug'],key)
 assert d['source_cobblemon_version']=='1.8.2'
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 allowed.update([d['model'],d['texture'],d['animation']]+d['licenses'])
allowed_paths={str(Path('src/main/resources')/path) for path in allowed}
geo_by_slug={};animation_by_slug={}
for d in defs[60:70]:
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
  for value in a.get('bones',{}).values():
   text=json.dumps(value)
   assert not re.search(r'\bq\.[A-Za-z_]',text),(d['slug'],clip,'unadapted q alias')
   assert 'Math.' not in text,(d['slug'],clip,'unadapted Math namespace')
 for clip,events in d.get('source_event_evidence',{}).items():
  for event,payload in events.items():assert animation[clip][event]==payload,(d['slug'],clip,event)
 for clip in d['clips'].values():
  assert clip in animation or '.minemon_' in clip,(d['slug'],clip)
 for state in ('idle','walk','run'):
  assert state in profiles[d['slug']] and set(profiles[d['slug']][state])<=set(bones),(d['slug'],state)
 geo_by_slug[d['slug']]=bones;animation_by_slug[d['slug']]=animation
 assert d['idle'] in animation and d['walk'] in animation

assert [d['clips'].get('ground_run') for d in defs[60:70]].count(None)==7
for i in (62,63,64):
 assert defs[i]['clips'].get('ground_run')==f"animation.{defs[i]['slug']}.ground_run"
assert all(d['walk']==f"animation.{d['slug']}.ground_walk" for d in defs[60:70])
assert all(d.get('source_static_idle') and profiles[d['slug']]['idle_static']==profiles[d['slug']]['idle'] for d in defs[60:70])
assert all(not d.get('movement_fallback') for d in defs[60:70])
assert defs[63]['source_model_variant']=='kadabra_male.geo.json'
assert defs[64]['source_model_variant']=='alakazam_male.geo.json'
for slug in ('abra','kadabra','alakazam','bellsprout'):
 d=next(x for x in defs[60:70] if x['slug']==slug)
 assert d['source_procedural_pose']['adapted_geo_bones'].get('head')=='head' or d['source_procedural_pose']['adapted_geo_bones'].get('head_ai')=='head'
weepinbell=next(x for x in defs[60:70] if x['slug']=='weepinbell')
assert weepinbell['source_procedural_pose']['adapted_geo_bones'].get('head_ai') is None
assert all(not d['slug'] in ('charmander','charizard') for d in defs[60:70])

changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources/assets/minemon'],cwd=ROOT,text=True).splitlines()
patched_prior_paths={str(Path('src/main/resources')/p) for p in PATCHED_PRIOR_ASSETS}
assert set(changed)<=allowed_paths|patched_prior_paths,set(changed)-allowed_paths-patched_prior_paths
changed_resources=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
assert set(changed_resources)<=allowed_paths|{MP,VP}|patched_prior_paths,set(changed_resources)-allowed_paths-{MP,VP}-patched_prior_paths
for d in defs[60:70]:
 for key in ('model','texture','animation'):
  assert '_cobblemon_1_8_2/' in d[key]
print('PASS BATCH 7: exactly #061–#070 imported; first 60 assets byte-preserved; remaining 81 definitions unchanged; source geometry, texture dimensions, bones, clips, and gait profiles validated')

if len(sys.argv)>1:
 with zipfile.ZipFile(sys.argv[1]) as z:
  packed_defs=json.loads(z.read('data/minemon_gen1/runtime_species.json'))
  packed_profiles=json.loads(z.read('data/minemon_gen1/animation_validation_profiles.json'))
  assert packed_defs==defs and packed_profiles==profiles
  dex=json.loads(z.read('data/minemon_gen1/pokedex.json'))
  assert len(dex)==151 and [x['nationalPokedexNumber'] for x in dex]==list(range(1,152))
  assert {d['slug'] for d in defs}=={d['slug'] for d in dex if d['registered']}
  for d in defs[:70]:
   for kind in ('model','texture','animation'):
    path=d[kind].replace('assets/minemon/','assets/minemon_gen1/',1)
    assert path in z.namelist(),(d['slug'],kind,path)
  for d in defs[60:70]:
   path=d['animation'].replace('assets/minemon/','assets/minemon_gen1/',1)
   packed=json.loads(z.read(path))['animations']
   source=animation_by_slug[d['slug']]
   for name,a in source.items():
    for event in ('sound_effects','particle_effects','timeline'):
     if event in a:assert packed[name][event]==normalize(a[event]),(d['slug'],name,event)
  print('PASS JAR BATCH 7: JAR contains all first 70 resources, cumulative profiles, all 151 registered species, and unchanged native event payloads')
