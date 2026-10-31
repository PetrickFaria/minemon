#!/usr/bin/env python3
"""Verify Cobblemon 1.8.2 Batch 14 and exact preservation of approved #001–#130."""
import argparse, json, math, struct, subprocess, zipfile
from pathlib import Path
from verify_approved_asset_delta import is_approved_prior_asset_delta, pokec_resource_paths

ROOT=Path(__file__).resolve().parents[1]
BASE='c91e12ac3067317f97898a16f5deec3bfa919277'
R=ROOT/'src/main/resources'
MP='data/minemon_gen1/runtime_species.json'
VP='data/minemon_gen1/animation_validation_profiles.json'
EXPECTED=['lapras','ditto','eevee','vaporeon','jolteon','flareon','porygon','omanyte','omastar','kabuto']
BATCH15=['kabutops','aerodactyl','snorlax','articuno','zapdos','moltres','dratini','dragonair','dragonite','mewtwo']
BATCH16=['mew']
ACTIVE_RUNS={'porygon'}
STATIC_IDLE={'eevee','vaporeon','jolteon','flareon'}
ANATOMY={
 'lapras':['shell','head','neck2','tail','tail2','leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'ditto':['body','head','torso2','arm_left','arm_right'],
 'eevee':['head','front_leg_left','front_leg_right','back_leg_left','back_leg_right','tail1','tail2','tail3','tail4'],
 'vaporeon':['head','frill_main','fin','tail','tailfin_left','tailfin_right','frontleg_left','frontleg_right'],
 'jolteon':['head','frill_main','tail','tail2','tail_left','tail_right','front_leg_left','front_leg_right'],
 'flareon':['head','mane_main','tail','fur_tail_top1','fur_tip_top1','front_leg_left','front_leg_right'],
 'porygon':['body','head','beak','tail','tail2','leg_left','leg_right'],
 'omanyte':['shell','front_leg_left','front_leg_middle','front_leg_right','left_back_leg','right_back_leg'],
 'omastar':['shell','front_left_tentacle_pair','front_left_tentacle_pair2','mid_left_tentacle_pair','back_left_tentacle_pair','front_right_tentacle_pair'],
 'kabuto':['body','left_front_leg','left_back_leg','right_front_leg','right_back_leg']}
ap=argparse.ArgumentParser();ap.add_argument('jar',nargs='?');args=ap.parse_args()
def readj(path):return json.loads((R/path).read_bytes())
def base(path):return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
def packed(path):return path.replace('assets/minemon/','assets/minemon_gen1/',1)
defs=readj(MP);profiles=readj(VP)
old_defs=json.loads(base('src/main/resources/'+MP));old_profiles=json.loads(base('src/main/resources/'+VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert [d['slug'] for d in defs[130:140]]==EXPECTED
prior_defs=json.loads(json.dumps(defs[:130]))
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
assert prior_defs==old_defs[:130],'An approved prior definition changed beyond the explicit fire-starter flame adaptation'
assert [d['slug'] for d in defs[140:150]]==BATCH15,'A post-batch definition changed'
assert set(profiles)==set(old_profiles)|set(EXPECTED)|{'kabutops','aerodactyl','snorlax','articuno','zapdos','moltres','dratini','dragonair','dragonite','mewtwo','mew'},'Unexpected or missing articulation profile'
for slug,profile in old_profiles.items():
 expected_profile=dict(profile)
 if slug=='rapidash':expected_profile['run']=[*profile['run'],'fire_mane_head_flame']
 assert profiles[slug]==expected_profile,'Earlier profile changed: '+slug
report=json.loads((ROOT/'docs/animation-audit/minemon-gen1-batch14-source-evidence.json').read_text())
assert report['batch']==14 and report['range']==[131,140]
assert report['source_sha256']=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2'
assert [s['slug'] for s in report['species']]==EXPECTED
prior_paths=set()
for d in old_defs[:130]:prior_paths.update([d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
for path in prior_paths:
 current=(R/path).read_bytes()
 assert is_approved_prior_asset_delta(path,current) or current==base('src/main/resources/'+path),'Earlier asset changed: '+path
allowed={'src/main/resources/'+MP,'src/main/resources/'+VP,'src/main/resources/data/minemon_gen1/pokedex.json',
 'src/main/resources/assets/minemon/lang/en_us.json','src/main/resources/assets/minemon/lang/pt_br.json'}
for d in defs[130:140]+defs[140:150]+defs[150:151]:
 allowed.update('src/main/resources/'+p for p in [d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
 allowed.add('src/main/resources/assets/minemon/models/item/'+d['slug']+'_spawn_egg.json')
source_fire_frames={f'src/main/resources/assets/minemon/textures/pokemon/{slug}_cobblemon_1_8_2/fire/normal/{frame}.png' for slug in ('ponyta','rapidash') for frame in range(1,5)}
allowed.update(source_fire_frames)
allowed.add('src/main/resources/assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png')
allowed.update({'src/main/resources/assets/minemon_gen1/particles/gastly_ghostly_smoke.json',*(f'src/main/resources/assets/minemon_gen1/textures/particle/gastly_ghostly_smoke_{i}.png' for i in range(6))})
changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
changed+=subprocess.check_output(['git','ls-files','--others','--exclude-standard','--','src/main/resources'],cwd=ROOT,text=True).splitlines()
allowed.update(pokec_resource_paths())
allowed.add('src/main/resources/assets/minemon_gen1/particles/gastly_small_smoke.json')
assert set(changed)<=allowed,set(changed)-allowed
for i,d in enumerate(defs[130:140]):
 slug=d['slug'];assert d['nationalPokedexNumber']==i+131
 assert d.get('source_cobblemon_version')=='1.8.2'
 variants={slug+'.geo.json',slug+'_male.geo.json',slug+'_female.geo.json'}
 assert d.get('source_model_variant') in variants,(slug,d.get('source_model_variant'))
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 geo=readj(d['model'])['minecraft:geometry'];assert geo
 bones={}
 for part in geo:
  for b in part['bones']:
   assert b['name'] not in bones,(slug,'duplicate bone',b['name']);bones[b['name']]=b
 for name,b in bones.items():
  seen={name};parent=b.get('parent')
  while parent:
   assert parent in bones and parent not in seen,(slug,'broken parent',name,parent)
   seen.add(parent);parent=bones[parent].get('parent')
  for axis in ('pivot','rotation'):assert all(math.isfinite(float(v)) for v in b.get(axis,[])),(slug,name,axis)
  for cube in b.get('cubes',[]):assert all(math.isfinite(float(v)) for v in cube['origin']+cube['size']),(slug,name,'cube')
 assert set(ANATOMY[slug])<=set(bones),(slug,'missing anatomy',set(ANATOMY[slug])-set(bones))
 tex=(R/d['texture']).read_bytes();assert tex.startswith(b'\x89PNG\r\n\x1a\n')
 assert struct.unpack('>II',tex[16:24])==(geo[0]['description']['texture_width'],geo[0]['description']['texture_height']),(slug,'texture size')
 animations=readj(d['animation'])['animations'];assert animations
 for name,clip in animations.items():
  assert set(clip.get('bones',{}))-set(bones)==set(d.get('stripped_bones',{}).get(name,[])),(slug,name,'untracked missing bones')
  if clip.get('animation_length') is not None:assert clip['animation_length']>0
  elif clip.get('loop') is True:pass
  elif clip.get('loop')=='hold_on_last_frame':assert clip.get('bones'),(slug,name,'empty hold clip')
  else:assert not clip.get('bones') and set(clip)<={'sound_effects','particle_effects','timeline'},(slug,name,'invalid durationless clip')
  if name in d.get('source_event_evidence',{}):
   for key,value in d['source_event_evidence'][name].items():assert clip[key]==value,(slug,name,key,'source event changed')
 for state in ('idle','walk'):
  assert d[state] in animations,(slug,'missing '+state)
  assert set(profiles[slug][state])<=set(animations[d[state]].get('bones',{})),(slug,state,'profile not in selected clip')
 run=d.get('clips',{}).get('ground_run')
 assert bool(run)==(slug in ACTIVE_RUNS),(slug,'native run mismatch')
 if run:assert set(profiles[slug]['run'])<=set(animations[run].get('bones',{})),(slug,'run profile mismatch')
 assert all('.minemon_' not in c for c in d.get('clips',{}).values()),(slug,'synthetic optional action selected')
 if slug in STATIC_IDLE:
  assert d.get('source_static_idle') is True and d.get('movement_fallback')=='static'
  assert d['walk']==d['idle'] and profiles[slug].get('idle_static')==profiles[slug]['idle']
 else:assert not d.get('source_static_idle'),(slug,'unexpected static classification')
for slug in ('lapras','omanyte','omastar'):
 clips=defs[130+EXPECTED.index(slug)]['clips']
 assert clips.get('water_idle') and clips.get('water_swim'),(slug,'native aquatic clips missing')
print('PASS BATCH 14 #131–#140: Kanto rigs, anatomy, native clips/events, static Eeveelution fallback and exact first-130 preservation')
if args.jar:
 with zipfile.ZipFile(args.jar) as z:
  names=set(z.namelist());jar_defs=json.loads(z.read('data/minemon_gen1/runtime_species.json'));dex=json.loads(z.read('data/minemon_gen1/pokedex.json'))
  assert jar_defs==defs and len(dex)==151 and [d['nationalPokedexNumber'] for d in dex]==list(range(1,152))
  for d in defs[:150]:
   for key in ('model','texture','animation'):assert packed(d[key]) in names,(d['slug'],key,packed(d[key]))
  for d in defs[130:140]+defs[140:150]+defs[150:151]:
   packed_anim=json.loads(z.read(packed(d['animation'])))['animations'];source=readj(d['animation'])['animations']
   for name,clip in source.items():
    for key in ('sound_effects','particle_effects','timeline'):
     if key in clip:assert packed_anim[name][key]==clip[key],(d['slug'],name,key,'JAR event mismatch')
 print('PASS JAR BATCH 14: first 140 updated species, 151 Pokédex records, and source events verified')
