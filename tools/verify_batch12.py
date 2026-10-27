#!/usr/bin/env python3
"""Verify native Cobblemon batch 12 and exact cumulative preservation through #110."""
import argparse,json,math,struct,subprocess,zipfile
from pathlib import Path
from verify_approved_asset_delta import is_approved_prior_asset_delta, pokec_resource_paths

ROOT=Path(__file__).resolve().parents[1];BASE='0816533addee80e9e17a684c94bed399f4a9488f'
R=ROOT/'src/main/resources';MP='data/minemon_gen1/runtime_species.json';VP='data/minemon_gen1/animation_validation_profiles.json'
EXPECTED=['rhyhorn','rhydon','chansey','tangela','kangaskhan','horsea','seadra','goldeen','seaking','staryu']
BATCH13=['starmie','mrmime','scyther','jynx','electabuzz','magmar','pinsir','tauros','magikarp','gyarados']
BATCH14=['lapras','ditto','eevee','vaporeon','jolteon','flareon','porygon','omanyte','omastar','kabuto']
BATCH15=['kabutops','aerodactyl','snorlax','articuno','zapdos','moltres','dratini','dragonair','dragonite','mewtwo']
BATCH16=['mew']
ANATOMY={
 'rhyhorn':['left_front_leg','right_front_leg','left_back_leg','right_back_leg'],
 'rhydon':['left_upper_arm','right_upper_arm','left_upper_leg','right_upper_leg','tail','tail2','tail3','tail4'],
 'chansey':['left_tendril_front','right_tendril_front','pouch','egg','tail1','tail2'],
 'tangela':['body','leftfoot','rightfoot'],
 'kangaskhan':['kid','kid_head','pouch','kid_tail','tail','tail2','tail3','tail4','tail5'],
 'horsea':['tail','tail2','fin'],'seadra':['tail','tail2','tail3','fin_left','fin_right'],
 'goldeen':['tail_left1','tail_middle1','tail_right1','fin_left1','fin_right1'],
 'seaking':['tail_back_left1','tail_back_right1','tail_bottom_left','tail_bottom_right'],
 'staryu':['body_limb_top','body_limb_left','body_limb_right','body_limb_bottom_left','body_limb_bottom_right']}
ACTIVE_RUNS={'rhyhorn','rhydon','chansey','staryu'}
ap=argparse.ArgumentParser();ap.add_argument('jar',nargs='?');args=ap.parse_args()
def base(path):return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
def readj(path):return json.loads((R/path).read_bytes())
def rp(path):return path.replace('assets/minemon/','assets/minemon_gen1/',1)

defs=readj(MP);profiles=readj(VP);old_defs=json.loads(base('src/main/resources/'+MP));old_profiles=json.loads(base('src/main/resources/'+VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert [d['slug'] for d in defs[110:120]]==EXPECTED
prior_defs=json.loads(json.dumps(defs[:110]))
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
assert prior_defs==old_defs[:110],'An approved prior definition changed beyond the explicit fire-starter flame adaptation'
assert [d['slug'] for d in defs[120:130]]==BATCH13 and [d['slug'] for d in defs[130:140]]==BATCH14 and [d['slug'] for d in defs[140:150]]==BATCH15,'Batch 13 plan or later definitions changed'
for slug,old_profile in old_profiles.items():
 expected_profile=old_profile
 if slug=='rapidash':
  expected_profile=dict(old_profile)
  expected_profile['run']=[*old_profile['run'],'fire_mane_head_flame']
 if slug=='alakazam':
  expected_profile=dict(old_profile)
  expected_profile['idle']=['foot_left','foot_right','left_lower_leg','right_lower_leg','hand_left','hand_right']
 assert profiles.get(slug)==expected_profile,'Earlier articulation profile changed: '+slug
assert set(profiles)==set(old_profiles)|set(EXPECTED)|set(BATCH13)|set(BATCH14)|set(BATCH15)|set(BATCH16),'Unexpected or missing articulation profile'
report=json.loads((ROOT/'docs/animation-audit/minemon-gen1-batch12-source-evidence.json').read_text())
assert report['batch']==12 and report['range']==[111,120]
assert report['source_sha256']=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2'
assert [x['slug'] for x in report['species']]==EXPECTED

prior=set()
for d in old_defs[:110]:prior.update([d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
for path in prior:
 current=(R/path).read_bytes()
 assert is_approved_prior_asset_delta(path,current) or current==base('src/main/resources/'+path),'Prior approved asset changed: '+path
allowed={'src/main/resources/'+MP,'src/main/resources/'+VP}
for d in defs[110:120]+defs[120:130]+defs[130:140]+defs[140:150]+defs[150:151]:allowed.update('src/main/resources/'+p for p in [d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
source_fire_frames={f'src/main/resources/assets/minemon/textures/pokemon/{slug}_cobblemon_1_8_2/fire/normal/{frame}.png' for slug in ('ponyta','rapidash') for frame in range(1,5)}
allowed.update(source_fire_frames)
allowed.add('src/main/resources/assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png')
allowed.update({'src/main/resources/assets/minemon_gen1/particles/gastly_ghostly_smoke.json',*(f'src/main/resources/assets/minemon_gen1/textures/particle/gastly_ghostly_smoke_{i}.png' for i in range(6))})
changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
changed+=subprocess.check_output(['git','ls-files','--others','--exclude-standard','--','src/main/resources'],cwd=ROOT,text=True).splitlines()
allowed.update(pokec_resource_paths())
assert set(changed)<=allowed,set(changed)-allowed

for index,d in enumerate(defs[110:120]):
 slug=d['slug'];assert d['nationalPokedexNumber']==index+111
 assert d.get('source_cobblemon_version')=='1.8.2'
 assert d.get('source_model_variant') in (slug+'.geo.json',slug+'_male.geo.json',slug+'_female.geo.json')
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 geo=readj(d['model'])['minecraft:geometry'];assert geo
 bones={}
 for part in geo:
  for bone in part['bones']:
   assert bone['name'] not in bones,(slug,'duplicate bone',bone['name']);bones[bone['name']]=bone
 for name,b in bones.items():
  seen={name};parent=b.get('parent')
  while parent:assert parent in bones and parent not in seen,(slug,'invalid parent chain',name,parent);seen.add(parent);parent=bones[parent].get('parent')
  for axis in ('pivot','rotation'):assert all(math.isfinite(float(v)) for v in b.get(axis,[])),(slug,name,axis)
  for cube in b.get('cubes',[]):assert all(math.isfinite(float(v)) for v in cube['origin']+cube['size']),(slug,name,'cube')
 assert set(ANATOMY[slug])<=set(bones),(slug,'missing source anatomy',set(ANATOMY[slug])-set(bones))
 texture=(R/d['texture']).read_bytes();assert texture.startswith(b'\x89PNG\r\n\x1a\n')
 assert struct.unpack('>II',texture[16:24])==(geo[0]['description']['texture_width'],geo[0]['description']['texture_height']),(slug,'texture dimensions')
 animations=readj(d['animation'])['animations'];assert animations
 for name,clip in animations.items():
  assert set(clip.get('bones',{}))-set(bones)==set(d.get('stripped_bones',{}).get(name,[])),(slug,name,'untracked missing bones')
  if 'animation_length' in clip and clip['animation_length'] is not None:assert clip['animation_length']>0
  elif clip.get('loop') is True:pass
  else:
   # Source-only, event-driven cry entries have no bone channels or duration.
   # Keep the original source event and exclude them from active state parsing.
   assert not clip.get('bones') and set(clip)<= {'sound_effects','particle_effects','timeline'},(slug,name,'invalid durationless source clip')
  if name in d.get('source_event_evidence',{}):
   for key,payload in d['source_event_evidence'][name].items():assert clip[key]==payload,(slug,name,key,'source event changed')
 for state in ('idle','walk'):
  assert d[state] in animations,(slug,'missing selected '+state)
  assert set(profiles[slug][state])<=set(animations[d[state]].get('bones',{})),(slug,state,'profile not animated by selected clip')
 run=d.get('clips',{}).get('ground_run')
 assert bool(run)==(slug in ACTIVE_RUNS),(slug,'native run selection differs from Cobblemon source')
 if run:assert set(profiles[slug]['run'])<=set(animations[run].get('bones',{})),(slug,'run profile not animated')
 assert all('.minemon_' not in clip for clip in d.get('clips',{}).values()),(slug,'generated action placeholder selected')
 if slug=='tangela':
  assert d.get('movement_fallback')=='static' and d.get('source_static_idle') is True
  assert d.get('idle')==d.get('walk')=='animation.tangela.minemon_static'
  assert animations[d['idle']]['bones']=={'body':{'rotation':[0,0,0]}}
  assert d.get('source_resource_limit') and 'no idle/walk/run clip or poser' in d['source_resource_limit']
 else:assert not d.get('movement_fallback'),(slug,'unexplained movement fallback')
for d in defs[110:120]:
 item=report['species'][d['nationalPokedexNumber']-111]
 assert item['slug']==d['slug'] and item['native_run']==d.get('clips',{}).get('ground_run')

print('PASS BATCH 12 #111–#120: native rigs, textures, clips, selected articulation, Tangela static fallback, and exact prior-110 preservation')
if args.jar:
 with zipfile.ZipFile(args.jar) as jar:
  names=set(jar.namelist());packed=json.loads(jar.read('data/minemon_gen1/runtime_species.json'));dex=json.loads(jar.read('data/minemon_gen1/pokedex.json'))
  assert packed==defs and len(dex)==151 and [d['nationalPokedexNumber'] for d in dex]==list(range(1,152))
  assert {d['slug'] for d in packed}=={d['slug'] for d in dex if d['registered']}
  for d in defs[:120]:
   for key in ('model','texture','animation'):assert rp(d[key]) in names,(d['slug'],key,rp(d[key]))
  for d in defs[110:120]:
   packed_anim=json.loads(jar.read(rp(d['animation'])))['animations'];source=readj(d['animation'])['animations']
   for name,clip in source.items():
    for key in ('sound_effects','particle_effects','timeline'):
     if key not in clip:continue
     value=clip[key]
     if key in ('sound_effects','particle_effects') and (not isinstance(value,dict) or any(not isinstance(v,dict) for v in value.values())):
      assert key not in packed_anim[name],(d['slug'],name,key,'unsupported runtime event block')
     else:assert packed_anim[name].get(key)==value,(d['slug'],name,key,'event payload')
  print('PASS JAR BATCH 12: first 120 updated species, 151 Pokédex records, and Batch 12 events verified')
