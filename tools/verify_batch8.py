#!/usr/bin/env python3
"""Verify exclusive batch 8 resources, cumulative preservation, and the packaged JAR."""
import argparse, json, math, re, struct, subprocess, sys, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BASE='b77eac9c38aa0d6a1d35c3cfab36a707ac6cab24'
MP='src/main/resources/data/minemon_gen1/runtime_species.json'
VP='src/main/resources/data/minemon_gen1/animation_validation_profiles.json'
EXPECTED=['victreebel','tentacool','tentacruel','geodude','graveler','golem','ponyta','rapidash','slowpoke','slowbro']
parser=argparse.ArgumentParser()
parser.add_argument('--segment',choices=('first','second'))
parser.add_argument('jar',nargs='?')
args=parser.parse_args()
assert not args.segment or not args.jar,'JAR verification is for the complete batch only'
start,end=(71,80)
if args.segment=='first':start,end=71,75
if args.segment=='second':start,end=76,80
expected=EXPECTED[start-71:end-70]
resources=ROOT/'src/main/resources'

def base_file(path):
 return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)

def normalize(value):
 if isinstance(value,str):return re.sub(r'\bq\.', 'query.', value).replace('Math.','math.')
 if isinstance(value,list):return [normalize(x) for x in value]
 if isinstance(value,dict):return {k:normalize(v) for k,v in value.items()}
 return value

defs=json.loads((resources/MP.removeprefix('src/main/resources/')).read_text())
old_defs=json.loads(base_file(MP))
profiles=json.loads((resources/VP.removeprefix('src/main/resources/')).read_text())
old_profiles=json.loads(base_file(VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert [d['slug'] for d in defs[70:80]]==EXPECTED
assert defs[:70]==old_defs[:70],'One of the first 70 manifest entries changed'
assert defs[end:]==old_defs[end:],'An out-of-scope manifest entry changed'
assert all(d['model']==old_defs[i]['model'] and d['texture']==old_defs[i]['texture'] and d['animation']==old_defs[i]['animation'] for i,d in enumerate(defs) if not start-1<=i<end),'Out-of-scope paths changed'
assert all(profiles.get(k)==v for k,v in old_profiles.items()),'Earlier articulation profiles changed'
assert set(profiles)==set(old_profiles)|set(EXPECTED[:end-70]),'Unexpected articulation profile set'

old_paths=set()
for d in old_defs[:70]:old_paths.update([d['model'],d['texture'],d['animation']]+d['licenses'])
for p in old_paths:
 assert (resources/p).read_bytes()==base_file('src/main/resources/'+p),'Prior approved asset changed: '+p

allowed=set()
for d in defs[start-1:end]:
 assert d['source_cobblemon_version']=='1.8.2'
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 allowed.update([d['model'],d['texture'],d['animation']]+d['licenses'])
allowed_paths={str(Path('src/main/resources')/p) for p in allowed}
geo_by_slug={}; anim_by_slug={}
for d in defs[start-1:end]:
 slug=d['slug']
 geo=json.loads((resources/'assets/minemon'/d['model'].removeprefix('assets/minemon/')).read_text())['minecraft:geometry']
 bones={}
 for part in geo:
  for bone in part['bones']:
   name=bone['name'];assert name not in bones,(slug,'duplicate bone',name);bones[name]=bone
 for name,bone in bones.items():
  seen={name};parent=bone.get('parent')
  while parent:
   assert parent in bones and parent not in seen,(slug,'broken hierarchy',name,parent)
   seen.add(parent);parent=bones[parent].get('parent')
  for axis in ('pivot','rotation'):
   assert all(isinstance(n,(int,float)) and math.isfinite(n) for n in bone.get(axis,[])),(slug,name,axis)
  for cube in bone.get('cubes',[]):assert all(math.isfinite(v) for v in cube['origin']+cube['size']),(slug,name,'cube')
 tex=(resources/'assets/minemon'/d['texture'].removeprefix('assets/minemon/')).read_bytes()
 assert tex[:8]==b'\x89PNG\r\n\x1a\n',(slug,'invalid PNG')
 dimensions=struct.unpack('>II',tex[16:24]);desc=geo[0]['description']
 assert dimensions==(desc['texture_width'],desc['texture_height']),(slug,dimensions,desc)
 animation=json.loads((resources/'assets/minemon'/d['animation'].removeprefix('assets/minemon/')).read_text())['animations']
 for name,clip in animation.items():
  missing=set(clip.get('bones',{}))-set(bones)
  assert missing==set(d.get('stripped_bones',{}).get(name,[])),(slug,name,'missing bones',missing)
  encoded=json.dumps(clip)
  assert not re.search(r'\bq\.[A-Za-z_]',encoded),(slug,name,'unadapted query alias')
  assert 'Math.' not in encoded,(slug,name,'unadapted Math namespace')
 for name,events in d.get('source_event_evidence',{}).items():
  for event,payload in events.items():assert animation[name][event]==normalize(payload),(slug,name,event,'event payload changed')
 for name in d['clips'].values():assert name in animation or '.minemon_' in name,(slug,'clip not found',name)
 for state,joints in profiles[slug].items():assert set(joints)<=set(bones),(slug,state,set(joints)-set(bones))
 assert d['idle'] in animation and d['walk'] in animation,(slug,'idle/walk clip missing')
 idle=json.dumps(animation[d['idle']].get('bones',{}))
 assert re.search(r'\b(?:q|query)\.(?:anim_time|life_time)\b',idle),(slug,'native idle has no temporal expression')
 geo_by_slug[slug]=bones;anim_by_slug[slug]=animation

# Source-backed anatomy profiles: inspect the same chains that define these rigs.
required={
 'victreebel':('head_leaf','head_stem'),'tentacool':('tentacle_left_3','tentacle_right_3'),
 'tentacruel':('tentacle_left_3','tentacle_right_3'),'geodude':('arm_left','arm_right'),
 'graveler':('arm_bottom_left','arm_bottom_right'),'golem':('left_upper_arm','right_upper_arm'),
 'ponyta':('fire_mane','fire_mane_head','tail'),'rapidash':('fire_mane_head_flame','tail'),
 'slowpoke':('tail','tail2','tail4'),'slowbro':('shell','shell_jaw','tail')}
for slug,joints in required.items():
 if slug not in expected:continue
 assert set(joints)<=set(geo_by_slug[slug]),(slug,'missing characteristic anatomy',set(joints)-set(geo_by_slug[slug]))
 assert set(joints)&set(profiles[slug]['walk']),(slug,'profile omits source anatomy')
if not args.segment:
 assert all(defs[i]['clips'].get('ground_run') for i in (73,74,75,77)),'Source ground runs for Geodude, Graveler, Golem, Rapidash must remain selected'
 assert all(defs[i]['clips'].get('ground_run') is None for i in (70,71,72,78,79)),'Do not invent ground runs where source has none'
 assert 'ground_runold' in defs[76]['clips'] and not defs[76]['clips'].get('ground_run'),'Ponyta legacy run clip must remain unselected without source poser evidence'
 assert all(defs[i]['movement_fallback']=='idle' for i in (71,72)),'Tentacool/Tentacruel have no native ground walk; preserve documented idle fallback'
 assert all(defs[i]['clips'].get('water_swim') for i in (71,72,78,79)),'Preserve source aquatic clips without adding a water-state system'

changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources/assets/minemon'],cwd=ROOT,text=True).splitlines()
assert set(changed)<=allowed_paths,set(changed)-allowed_paths
changed_resources=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
allowed_resource_paths=allowed_paths|{MP,VP}
assert set(changed_resources)<=allowed_resource_paths,set(changed_resources)-allowed_resource_paths
print(f'PASS BATCH 8 {start}-{end}: exact authorized definitions; source model hierarchy/texture dimensions/animation bones/expressions/events and special anatomy validated; first 70 resources byte-preserved')

if args.jar:
 with zipfile.ZipFile(args.jar) as jar:
  packed=json.loads(jar.read('data/minemon_gen1/runtime_species.json'))
  packed_profiles=json.loads(jar.read('data/minemon_gen1/animation_validation_profiles.json'))
  dex=json.loads(jar.read('data/minemon_gen1/pokedex.json'))
  assert packed==defs and packed_profiles==profiles
  assert len(dex)==151 and [d['nationalPokedexNumber'] for d in dex]==list(range(1,152))
  assert {d['slug'] for d in packed}=={d['slug'] for d in dex if d['registered']}
  for d in defs[:80]:
   for kind in ('model','texture','animation'):
    path=d[kind].replace('assets/minemon/','assets/minemon_gen1/',1)
    assert path in jar.namelist(),(d['slug'],kind,path)
  for d in defs[70:80]:
   path=d['animation'].replace('assets/minemon/','assets/minemon_gen1/',1)
   packed_animation=json.loads(jar.read(path))['animations']
   for name,clip in anim_by_slug[d['slug']].items():
    for key in ('sound_effects','particle_effects','timeline'):
     if key in clip:assert packed_animation[name][key]==normalize(clip[key]),(d['slug'],name,key)
  print('PASS JAR BATCH 8: 80 updated species resources, unchanged 151-entry Pokédex, cumulative profiles, and source events verified')
