#!/usr/bin/env python3
"""Verify Batch 11 assets, exact preservation of the approved first 100, and optional JAR."""
import argparse,json,math,struct,subprocess,zipfile
from pathlib import Path
from verify_approved_asset_delta import is_approved_prior_asset_delta, pokec_resource_paths

ROOT=Path(__file__).resolve().parents[1]
BASE='b7b81592f4e2e0097a32f9342e3890f7257182e5'
RESOURCE=ROOT/'src/main/resources'
MP='data/minemon_gen1/runtime_species.json'
VP='data/minemon_gen1/animation_validation_profiles.json'
REPORT=ROOT/'docs/animation-audit/minemon-gen1-batch11-source-evidence.json'
EXPECTED=['electrode','exeggcute','exeggutor','cubone','marowak','hitmonlee','hitmonchan','lickitung','koffing','weezing']
BATCH12=['rhyhorn','rhydon','chansey','tangela','kangaskhan','horsea','seadra','goldeen','seaking','staryu']
BATCH13=['starmie','mrmime','scyther','jynx','electabuzz','magmar','pinsir','tauros','magikarp','gyarados']
BATCH14=['lapras','ditto','eevee','vaporeon','jolteon','flareon','porygon','omanyte','omastar','kabuto']
BATCH15=['kabutops','aerodactyl','snorlax','articuno','zapdos','moltres','dratini','dragonair','dragonite','mewtwo']
BATCH16=['mew']
ANATOMY={
 'electrode':['body'],
 'exeggcute':['egg1','egg2','egg3','egg4','egg5','egg6'],
 'exeggutor':['head','head2','head3','leg_left','leg_right'],
 'cubone':['head','bone','left_upper_arm','right_upper_arm'],
 'marowak':['head','bone_right_hand','right_hand','right_upper_arm'],
 'hitmonlee':['leg_left','leg_right','foot_left','foot_right'],
 'hitmonchan':['arm_left','arm_right','hand_left','hand_right'],
 'lickitung':['tongue','tongue2','tongue3','tongue4'],
 'koffing':['body','body_inflate','craters'],
 'weezing':['main_body','middle_body','small_body']}
ACTIVE_RUNS={'lickitung'}
parser=argparse.ArgumentParser();parser.add_argument('jar',nargs='?');args=parser.parse_args()
def base_file(path):return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
def read_local(path):return (RESOURCE/path).read_bytes()
def read_json(path):return json.loads(read_local(path))
def packed_path(d,key):return d[key].replace('assets/minemon/','assets/minemon_gen1/',1)

defs=read_json(MP);profiles=read_json(VP)
old_defs=json.loads(base_file('src/main/resources/'+MP));old_profiles=json.loads(base_file('src/main/resources/'+VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert [d['slug'] for d in defs[100:110]]==EXPECTED
prior_defs=json.loads(json.dumps(defs[:100]))
for slug in ('charmeleon','charizard'):
 definition=next(d for d in prior_defs if d['slug']==slug)
 definition['clips'].pop('tail_flame',None)
 definition['tail_flame_animation']=False
charizard=next(d for d in prior_defs if d['slug']=='charizard')
charizard['clips'].pop('air_idle',None)
charizard['clips'].pop('air_fly',None)
for slug in ('squirtle','wartortle'):
 definition=next(d for d in prior_defs if d['slug']==slug)
 assert definition['clips'].pop('ground_run',None)==f'animation.{slug}.ground_run','Only the existing native turtle run may be newly selected'
# These exact scale changes are the only authorized prior-species definition deltas.
for slug, expected_scale in {
 'gastly': {'scale':0.85,'width':0.2,'height':0.2},
 'electabuzz': {'scale':1.35,'width':1.05,'height':2.25},
}.items():
 definition=next((d for d in prior_defs if d['slug']==slug),None)
 if definition is not None:
  baseline=old_defs[definition['nationalPokedexNumber']-1]
  for key,value in expected_scale.items():
   assert definition[key]==value,(slug,key,'unexpected authorized scale',definition[key])
   definition[key]=baseline[key]
for slug in ('spearow','fearow'):
 definition=next(d for d in prior_defs if d['slug']==slug)
 old_definition=next(d for d in old_defs if d['slug']==slug)
 if slug=='spearow':
  assert definition['clips'].pop('air_idle',None)=='animation.spearow.minemon_air_idle'
  assert definition['clips'].pop('air_fly',None)=='animation.spearow.minemon_air_fly'
 else:
  assert definition['clips'].pop('air_fly',None)=='animation.fearow.minemon_air_fly'
  assert definition['clips']['air_idle']=='animation.fearow.minemon_air_idle'
  definition['clips']['air_idle']=old_definition['clips']['air_idle']
assert prior_defs==old_defs[:100],'An approved prior definition changed beyond the explicit fire-starter flame adaptation'
assert [d['slug'] for d in defs[110:120]]==BATCH12,'Batch 12 must remain the planned #111-120 sequence'
assert [d['slug'] for d in defs[120:130]]==BATCH13 and [d['slug'] for d in defs[130:140]]==BATCH14 and [d['slug'] for d in defs[140:150]]==BATCH15,'Batch 13 plan or later species changed'
assert set(profiles)==set(old_profiles)|set(EXPECTED)|set(BATCH12)|set(BATCH13)|set(BATCH14)|set(BATCH15)|set(BATCH16),'Unexpected or missing articulation profile'
for slug,old_profile in old_profiles.items():
 expected_profile=old_profile
 if slug=='rapidash':
  expected_profile=dict(old_profile)
  expected_profile['run']=[*old_profile['run'],'fire_mane_head_flame']
 if slug=='alakazam':
  expected_profile=dict(old_profile)
  expected_profile['idle']=['foot_left','foot_right','left_lower_leg','right_lower_leg','hand_left','hand_right']
 assert profiles.get(slug)==expected_profile,'An earlier profile changed: '+slug

report=json.loads(REPORT.read_text())
assert report['batch']==11 and report['range']==[101,110]
assert report['source_sha256']=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2'
assert [item['slug'] for item in report['species']]==EXPECTED

prior_paths=set()
for d in old_defs[:100]:prior_paths.update([d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
for path in prior_paths:
 current=read_local(path)
 assert is_approved_prior_asset_delta(path,current) or current==base_file('src/main/resources/'+path),'Previous approved resource changed: '+path

allowed={'src/main/resources/'+MP,'src/main/resources/'+VP}
for d in defs[100:110]:
 slug=d['slug'];number=d['nationalPokedexNumber']
 assert number==EXPECTED.index(slug)+101
 assert d.get('source_cobblemon_version')=='1.8.2'
 assert d.get('source_model_variant')==slug+'.geo.json','Only the canonical Kanto model is valid: '+slug
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 allowed.update('src/main/resources/'+p for p in [d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
 geo=json.loads(read_local(d['model']))['minecraft:geometry'];assert geo
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
 assert set(ANATOMY[slug])<=set(bones),(slug,'missing source anatomy',set(ANATOMY[slug])-set(bones))
 texture=read_local(d['texture'])
 assert texture.startswith(b'\x89PNG\r\n\x1a\n'),(slug,'invalid PNG')
 assert struct.unpack('>II',texture[16:24])==(geo[0]['description']['texture_width'],geo[0]['description']['texture_height']),(slug,'texture/geometry dimensions')
 animations=json.loads(read_local(d['animation']))['animations'];assert animations,(slug,'empty animation file')
 for name,clip in animations.items():
  assert isinstance(clip,dict),(slug,name,'invalid clip')
  missing=set(clip.get('bones',{}))-set(bones)
  assert missing==set(d.get('stripped_bones',{}).get(name,[])),(slug,name,'untracked missing bones',missing)
  if 'animation_length' in clip and clip['animation_length'] is not None:assert clip['animation_length']>0,(slug,name,'invalid duration')
  else:assert clip.get('loop') is True,(slug,name,'missing looping duration')
  if name in d.get('source_event_evidence',{}):
   for event,payload in d['source_event_evidence'][name].items():assert clip[event]==payload,(slug,name,event,'source event changed')
 for clip in d['clips'].values():assert clip in animations,(slug,'selected native clip missing',clip)
 assert d['idle'] in animations and d['walk'] in animations,(slug,'selected state clip missing')
 for state in ('idle','walk'):
  assert set(profiles[slug][state])<=set(animations[d[state]].get('bones',{})),(slug,state,'profile is not in selected native clip')
 if d.get('clips',{}).get('ground_run'):
  assert slug in ACTIVE_RUNS and d['clips']['ground_run']=='animation.lickitung.ground_run'
  assert set(profiles[slug]['run'])<=set(animations[d['clips']['ground_run']].get('bones',{})),(slug,'run profile is not in native run clip')
 else:assert slug not in ACTIVE_RUNS
 assert all('.minemon_' not in clip for clip in d['clips'].values()),(slug,'synthetic placeholder clip selected')
 if slug in ('cubone','marowak'):
  assert d.get('movement_fallback')=='idle' and d.get('source_static_idle') is True
  assert d['idle']==d['walk'] and profiles[slug]['idle_static']==profiles[slug]['idle']
 if slug in ('koffing','weezing'):assert d['mode']=='flying' and d['walk'].endswith('.air_fly')
 if slug=='hitmonchan':
  assert 'animation.hitmonchan.ground_run_WIP' in animations
  assert 'ground_run' not in d['clips']
for d in defs[110:120]+defs[120:130]+defs[130:140]+defs[140:150]+defs[150:151]:
 slug=d['slug'];paths=[d['model'],d['texture'],d['animation']]+d.get('licenses',[])
 allowed.update('src/main/resources/'+p for p in paths)

source_fire_frames={f'src/main/resources/assets/minemon/textures/pokemon/{slug}_cobblemon_1_8_2/fire/normal/{frame}.png' for slug in ('ponyta','rapidash') for frame in range(1,5)}
allowed.update(source_fire_frames)
allowed.add('src/main/resources/assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png')
allowed.update({'src/main/resources/assets/minemon_gen1/particles/gastly_ghostly_smoke.json',*(f'src/main/resources/assets/minemon_gen1/textures/particle/gastly_ghostly_smoke_{i}.png' for i in range(6))})
changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
changed+=subprocess.check_output(['git','ls-files','--others','--exclude-standard','--','src/main/resources'],cwd=ROOT,text=True).splitlines()
allowed.update(pokec_resource_paths())
allowed.add('src/main/resources/assets/minemon_gen1/particles/gastly_small_smoke.json')
assert set(changed)<=allowed,set(changed)-allowed
print('PASS BATCH 11 #101-110: Kanto source models, textures, native clips, articulation/fallback, and exact first-100 preservation')

if args.jar:
 with zipfile.ZipFile(args.jar) as jar:
  names=set(jar.namelist())
  packed=json.loads(jar.read('data/minemon_gen1/runtime_species.json'))
  dex=json.loads(jar.read('data/minemon_gen1/pokedex.json'))
  packed_profiles=json.loads(jar.read('data/minemon_gen1/animation_validation_profiles.json'))
  assert packed==defs and packed_profiles==profiles
  assert len(dex)==151 and [d['nationalPokedexNumber'] for d in dex]==list(range(1,152))
  assert {d['slug'] for d in packed}=={d['slug'] for d in dex if d['registered']}
  for d in defs[:120]:
   for key in ('model','texture','animation'):assert packed_path(d,key) in names,(d['slug'],key,packed_path(d,key))
  for d in defs[100:110]:
   packed_animation=json.loads(jar.read(packed_path(d,'animation')))['animations']
   source_animation=read_json(d['animation'])['animations']
   for name,clip in source_animation.items():
    for key in ('sound_effects','particle_effects','timeline'):
     if key not in clip:continue
     value=clip[key]
     if key in ('sound_effects','particle_effects') and (not isinstance(value,dict) or any(not isinstance(v,dict) for v in value.values())):
      assert key not in packed_animation[name],(d['slug'],name,key,'unsupported runtime event block')
     else:assert packed_animation[name].get(key)==value,(d['slug'],name,key,'event payload')
  print('PASS JAR BATCH 11 cumulative: first 120 updated species, 151 Pokédex entries, and batch 11 source events verified')
