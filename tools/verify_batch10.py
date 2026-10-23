#!/usr/bin/env python3
"""Verify Batch 10 assets and preservation on later cumulative batch branches."""
import argparse,json,math,struct,subprocess,zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BASE='b7b81592f4e2e0097a32f9342e3890f7257182e5'
RESOURCE=ROOT/'src/main/resources'
MP='data/minemon_gen1/runtime_species.json'
VP='data/minemon_gen1/animation_validation_profiles.json'
REPORT=ROOT/'docs/animation-audit/minemon-gen1-batch10-source-evidence.json'
EXPECTED=['cloyster','gastly','haunter','gengar','onix','drowzee','hypno','krabby','kingler','voltorb']
BATCH11=['electrode','exeggcute','exeggutor','cubone','marowak','hitmonlee','hitmonchan','lickitung','koffing','weezing']
BATCH12=['rhyhorn','rhydon','chansey','tangela','kangaskhan','horsea','seadra','goldeen','seaking','staryu']
BATCH13=['starmie','mrmime','scyther','jynx','electabuzz','magmar','pinsir','tauros','magikarp','gyarados']
BATCH14=['lapras','ditto','eevee','vaporeon','jolteon','flareon','porygon','omanyte','omastar','kabuto']
BATCH15=['kabutops','aerodactyl','snorlax','articuno','zapdos','moltres','dratini','dragonair','dragonite','mewtwo']
BATCH16=['mew']
ACTIVE_RUNS={'drowzee','hypno'}
ANATOMY={
 'cloyster':['shell_left','shell_right','shell_back_left','shell_back_right','inner_shell','inner_shell_left','inner_shell_right','inner_spike'],
 'gastly':['body','tongue'],
 'haunter':['hand_left1','hand_right1','hand_left2','hand_right2','tail1','tail2','tail3'],
 'gengar':['arm_left','arm_right','arm_left2','arm_right2','leg_left','leg_right','leg_left2','leg_right2'],
 'onix':['segment'+str(n) for n in range(1,15)]+['boulder'+str(n) for n in range(1,15)],
 'drowzee':['arm_left','arm_right','leg_left','leg_right'],
 'hypno':['left_upper_arm','right_upper_arm','left_upper_leg','right_upper_leg','pendulum','string_1','string_2','string_3','weight'],
 'krabby':['arm_left','arm_right','claw_left','claw_right']+[f'leg_{side}_{pos}' for side in ('left','right') for pos in ('front','back')],
 'kingler':['arm_left','arm_right','claw_left','claw_right']+[f'leg_{side}_{pos}' for side in ('left','right') for pos in ('front','back')],
 'voltorb':['body']}

parser=argparse.ArgumentParser();parser.add_argument('jar',nargs='?');args=parser.parse_args()
def base_file(path):return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
def read_local(path):return (RESOURCE/path).read_bytes()
def read_json(path):return json.loads(read_local(path))
def source_path(d,key):return d[key]
def packed_path(d,key):return d[key].replace('assets/minemon/','assets/minemon_gen1/',1)

defs=read_json(MP);profiles=read_json(VP)
old_defs=json.loads(base_file('src/main/resources/'+MP));old_profiles=json.loads(base_file('src/main/resources/'+VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert [d['slug'] for d in defs[90:100]]==EXPECTED
prior_defs=json.loads(json.dumps(defs[:100]))
for slug in ('charmeleon','charizard'):
 definition=next(d for d in prior_defs if d['slug']==slug)
 definition['clips'].pop('tail_flame',None)
 definition['tail_flame_animation']=False
assert prior_defs==old_defs[:100],'An approved prior definition changed beyond the explicit fire-starter flame adaptation'
assert [d['slug'] for d in defs[100:110]]==BATCH11,'Batch 11 must remain the planned #101-110 sequence'
assert [d['slug'] for d in defs[110:120]]==BATCH12,'Batch 12 must remain the planned #111-120 sequence'
assert [d['slug'] for d in defs[120:130]]==BATCH13 and [d['slug'] for d in defs[130:140]]==BATCH14 and [d['slug'] for d in defs[140:150]]==BATCH15,'Batch 13 plan or later species changed'
assert set(profiles)==set(old_profiles)|set(BATCH11)|set(BATCH12)|set(BATCH13)|set(BATCH14)|set(BATCH15)|set(BATCH16),'Unexpected or missing articulation profile'
for slug,old_profile in old_profiles.items():
 expected_profile=old_profile
 if slug=='rapidash':
  expected_profile=dict(old_profile)
  expected_profile['run']=[*old_profile['run'],'fire_mane_head_flame']
 if slug=='alakazam':
  expected_profile=dict(old_profile)
  expected_profile['idle']=['foot_left','foot_right','left_lower_leg','right_lower_leg','hand_left','hand_right']
 assert profiles.get(slug)==expected_profile,'An earlier profile changed: '+slug
assert json.loads(base_file('src/main/resources/'+VP))==old_profiles

report=json.loads(REPORT.read_text())
assert report['batch']==10 and report['range']==[91,100]
assert report['source_sha256']=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2'
assert [item['slug'] for item in report['species']]==EXPECTED

prior_paths=set()
for d in old_defs[:100]:prior_paths.update([d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
for path in prior_paths:
 assert read_local(path)==base_file('src/main/resources/'+path),'Previous approved resource changed: '+path

allowed={'src/main/resources/'+MP,'src/main/resources/'+VP}
for d in defs[90:100]:
 slug=d['slug'];number=d['nationalPokedexNumber']
 assert number==EXPECTED.index(slug)+91
 assert d.get('source_cobblemon_version')=='1.8.2'
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 assert d.get('source_model_variant') in (slug+'.geo.json',slug+'_female.geo.json',slug+'_male.geo.json')
 paths=[d['model'],d['texture'],d['animation']]+d.get('licenses',[]);allowed.update('src/main/resources/'+p for p in paths)
 geo=json.loads(read_local(source_path(d,'model')))['minecraft:geometry']
 assert geo
 bones={}
 for part in geo:
  for bone in part['bones']:
   assert bone['name'] not in bones,(slug,'duplicate bone',bone['name']);bones[bone['name']]=bone
 for name,bone in bones.items():
  seen={name};parent=bone.get('parent')
  while parent:
   assert parent in bones and parent not in seen,(slug,'invalid parent chain',name,parent)
   seen.add(parent);parent=bones[parent].get('parent')
  for axis in ('pivot','rotation'):
   assert all(math.isfinite(float(v)) for v in bone.get(axis,[])),(slug,name,axis)
  for cube in bone.get('cubes',[]):assert all(math.isfinite(float(v)) for v in cube['origin']+cube['size']),(slug,name,'cube')
 assert set(ANATOMY[slug])<=set(bones),(slug,'missing native anatomy',set(ANATOMY[slug])-set(bones))
 texture=read_local(source_path(d,'texture'))
 assert texture.startswith(b'\x89PNG\r\n\x1a\n'),(slug,'invalid PNG')
 assert struct.unpack('>II',texture[16:24])==(geo[0]['description']['texture_width'],geo[0]['description']['texture_height']),(slug,'texture/geometry dimensions')
 animations=json.loads(read_local(source_path(d,'animation')))['animations']
 assert animations,(slug,'empty animation file')
 for name,clip in animations.items():
  assert isinstance(clip,dict),(slug,name,'invalid clip')
  missing=set(clip.get('bones',{}))-set(bones)
  assert missing==set(d.get('stripped_bones',{}).get(name,[])),(slug,name,'untracked missing bones',missing)
  if 'animation_length' in clip and clip['animation_length'] is not None:assert clip['animation_length']>0,(slug,name,'invalid duration')
  else:assert clip.get('loop') is True,(slug,name,'missing looping duration')
  if name in d.get('source_event_evidence',{}):
   for event,payload in d['source_event_evidence'][name].items():assert clip[event]==payload,(slug,name,event,'source event changed')
 for clip in d['clips'].values():assert clip in animations or '.minemon_' in clip,(slug,'selected clip missing',clip)
 assert d['idle'] in animations and d['walk'] in animations,(slug,'selected gait clip missing')
 if d['mode']=='flying' and slug=='gengar':
  assert d['idle'].endswith('.air_idle') and d['walk'].endswith('.air_fly')
 if slug=='haunter':
  assert d['mode']=='flying' and d['idle']==d['walk']=='animation.haunter.ground_idle'
  assert d.get('movement_fallback')=='idle' and d.get('source_static_idle') is True
  assert profiles[slug]['idle_static']==profiles[slug]['idle']
 if d.get('clips',{}).get('ground_run'):
  assert slug in ACTIVE_RUNS and d['clips']['ground_run']=='animation.'+slug+'.ground_run'
 else:assert slug not in ACTIVE_RUNS
 for state in ('idle','walk'):
  clip=animations[d[state]]
  assert set(profiles[slug][state])<=set(clip.get('bones',{})),(slug,state,'profile is not in selected clip')
 if d['clips'].get('ground_run'):
  assert set(profiles[slug]['run'])<=set(animations[d['clips']['ground_run']].get('bones',{})),(slug,'run profile is not in native run clip')

# Source-specific clips and anatomy stay available without inventing a gait.
cloyster=read_json(source_path(defs[90],'animation'))['animations']
assert {'animation.cloyster.closed','animation.cloyster.water_idle','animation.cloyster.water_swim'}<=set(cloyster)
gastly=read_json(source_path(defs[91],'animation'))['animations']
assert {'animation.gastly.ground_idle','animation.gastly.ground_walk','animation.gastly.fully_extended'}<=set(gastly)
haunter=read_json(source_path(defs[92],'animation'))['animations']
assert {'animation.haunter.ground_idle','animation.haunter.render','animation.haunter.toungue_test'}<=set(haunter)
onix=read_json(source_path(defs[94],'animation'))['animations']
assert {'animation.onix.boulder_rotation','animation.onix.slow_boulder_rotation'}<=set(onix)
hypno=read_json(source_path(defs[96],'animation'))['animations']
assert all('pendulum' in hypno[name]['bones'] for name in ('animation.hypno.ground_idle','animation.hypno.ground_walk','animation.hypno.ground_run'))
krabby=read_json(source_path(defs[97],'animation'))['animations']
assert {'animation.krabby.snip_left','animation.krabby.snip_right'}<=set(krabby)
voltorb=read_json(source_path(defs[99],'animation'))['animations']
assert all('body' in voltorb[name]['bones'] for name in ('animation.voltorb.ground_idle','animation.voltorb.ground_walk'))

for d in defs[100:110]:
 paths=[d['model'],d['texture'],d['animation']]+d.get('licenses',[]);allowed.update('src/main/resources/'+p for p in paths)
for d in defs[110:120]+defs[120:130]+defs[130:140]+defs[140:150]+defs[150:151]:
 paths=[d['model'],d['texture'],d['animation']]+d.get('licenses',[]);allowed.update('src/main/resources/'+p for p in paths)
source_fire_frames={f'src/main/resources/assets/minemon/textures/pokemon/{slug}_cobblemon_1_8_2/fire/normal/{frame}.png' for slug in ('ponyta','rapidash') for frame in range(1,5)}
allowed.update(source_fire_frames)
allowed.update({'src/main/resources/assets/minemon_gen1/particles/gastly_ghostly_smoke.json',*(f'src/main/resources/assets/minemon_gen1/textures/particle/gastly_ghostly_smoke_{i}.png' for i in range(6))})
changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
assert set(changed)<=allowed,set(changed)-allowed
print('PASS BATCH 10 #091-100: Cobblemon 1.8.2 rigs, textures, native animations/events, fallback, and exact first-90 preservation')

if args.jar:
 with zipfile.ZipFile(args.jar) as jar:
  names=set(jar.namelist())
  packed=json.loads(jar.read('data/minemon_gen1/runtime_species.json'))
  dex=json.loads(jar.read('data/minemon_gen1/pokedex.json'))
  packed_profiles=json.loads(jar.read('data/minemon_gen1/animation_validation_profiles.json'))
  assert packed==defs and packed_profiles==profiles
  assert len(dex)==151 and [d['nationalPokedexNumber'] for d in dex]==list(range(1,152))
  assert {d['slug'] for d in packed}=={d['slug'] for d in dex if d['registered']}
  for d in defs[:100]:
   for key in ('model','texture','animation'):
    assert packed_path(d,key) in names,(d['slug'],key,packed_path(d,key))
  for d in defs[90:100]:
   packed_animation=json.loads(jar.read(packed_path(d,'animation')))['animations']
   source_animation=read_json(source_path(d,'animation'))['animations']
   for name,clip in source_animation.items():
    for key in ('sound_effects','particle_effects','timeline'):
     if key not in clip:continue
     value=clip[key]
     if key in ('sound_effects','particle_effects') and (not isinstance(value,dict) or any(not isinstance(v,dict) for v in value.values())):
      assert key not in packed_animation[name],(d['slug'],name,key,'unsupported runtime event block')
     else:assert packed_animation[name].get(key)==value,(d['slug'],name,key,'event payload')
  print('PASS JAR BATCH 10: first 100 updated species resources, 151 registered Pokédex entries, and source event compatibility verified')
