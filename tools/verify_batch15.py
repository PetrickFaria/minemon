#!/usr/bin/env python3
"""Verify Cobblemon 1.8.2 Batch 15 and exact preservation of approved #001–#140."""
import argparse, json, math, struct, subprocess, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BASE='76fcb3074cc65c62dea6b94c291de90925494e95'
R=ROOT/'src/main/resources'
MP='data/minemon_gen1/runtime_species.json'
VP='data/minemon_gen1/animation_validation_profiles.json'
EXPECTED=['kabutops','aerodactyl','snorlax','articuno','zapdos','moltres','dratini','dragonair','dragonite','mewtwo']
RUNS={'aerodactyl','dratini','dragonair'}
FLYING={'articuno','zapdos','moltres','mewtwo'}
ANATOMY={
 'kabutops':['left_arm','right_arm','left_claw','right_claw','left_upper_leg','right_upper_leg','tail','fin_upper_left','fin_upper_right'],
 'aerodactyl':['wing_ground_left','wing_ground_right','wing_air_left','wing_air_right','tail_1','tail_tip','left_upper_leg','right_upper_leg'],
 'snorlax':['belly','belly2','arm_left','arm_right','leg_left','leg_right'],
 'articuno':['wing_open_left','wing_open_left3','wing_open_right','wing_open_right3','tail18','tail_base'],
 'zapdos':['wing_open_left','wing_open_left_anti','wing_open_right','wing_open_right_anti','tail_top'],
 'moltres':['wing_open_left','wing_open_right','head_fire2','head_fire3','tail_fire_puff2','tail_fire_puff5'],
 'dratini':['segment1','segment4','segment7','segment8','head'],
 'dragonair':['segment1','segment6','segment11','gem_tail1','gem_tail2','head_rotation'],
 'dragonite':['arm_left','arm_right','leg_left','leg_right','wing_left','wing_right','tail5'],
 'mewtwo':['arm_left','arm_right','leg_left','leg_right','tail','tail9','head']}
ap=argparse.ArgumentParser();ap.add_argument('jar',nargs='?');args=ap.parse_args()
def readj(path):return json.loads((R/path).read_bytes())
def base(path):return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
def packed(path):return path.replace('assets/minemon/','assets/minemon_gen1/',1)
defs=readj(MP);profiles=readj(VP)
old_defs=json.loads(base('src/main/resources/'+MP));old_profiles=json.loads(base('src/main/resources/'+VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert [d['slug'] for d in defs[140:150]]==EXPECTED
prior_defs=json.loads(json.dumps(defs[:140]))
for slug in ('charmeleon','charizard'):
 definition=next(d for d in prior_defs if d['slug']==slug)
 definition['clips'].pop('tail_flame',None)
 definition['tail_flame_animation']=False
assert prior_defs==old_defs[:140],'An approved prior definition changed beyond the explicit fire-starter flame adaptation'
assert len(defs)==151 and defs[150]['slug']=='mew','Final Pokédex entry must remain Mew'
assert set(profiles)==set(old_profiles)|set(EXPECTED)|{'mew'},'Unexpected or missing articulation profile'
for slug,profile in old_profiles.items():
 expected_profile=dict(profile)
 if slug=='rapidash':expected_profile['run']=[*profile['run'],'fire_mane_head_flame']
 assert profiles[slug]==expected_profile,'Earlier profile changed: '+slug
report=json.loads((ROOT/'docs/animation-audit/minemon-gen1-batch15-source-evidence.json').read_text())
assert report['batch']==15 and report['range']==[141,150]
assert report['source_sha256']=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2'
assert [s['slug'] for s in report['species']]==EXPECTED
prior_paths=set()
for d in old_defs[:140]:prior_paths.update([d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
for path in prior_paths:assert (R/path).read_bytes()==base('src/main/resources/'+path),'Earlier asset changed: '+path
allowed={'src/main/resources/'+MP,'src/main/resources/'+VP,'tools/import_cobblemon_182_batch.py','tools/verify_batch15.py',
 'src/test/java/br/com/petrickfaria/minemon/Batch15AnimationCompatibilityTest.java',
 'src/main/java/br/com/petrickfaria/minemon/client/MinemonModel.java',
 'src/main/java/br/com/petrickfaria/minemon/gametest/ClientSmokeTest.java',
 'docs/animation-audit/minemon-gen1-batch15-progress.md','docs/animation-audit/minemon-gen1-batch15-source-evidence.json',
 '.github/workflows/build.yml'}
for d in defs[140:150]+defs[150:151]:
 allowed.update('src/main/resources/'+p for p in [d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
 allowed.add('src/main/resources/assets/minemon/models/item/'+d['slug']+'_spawn_egg.json')
source_fire_frames={f'src/main/resources/assets/minemon/textures/pokemon/{slug}_cobblemon_1_8_2/fire/normal/{frame}.png' for slug in ('ponyta','rapidash') for frame in range(1,5)}
allowed.update(source_fire_frames)
changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
changed+=subprocess.check_output(['git','ls-files','--others','--exclude-standard','--','src/main/resources'],cwd=ROOT,text=True).splitlines()
assert set(changed)<=allowed,set(changed)-allowed
for i,d in enumerate(defs[140:150]):
 slug=d['slug'];assert d['nationalPokedexNumber']==i+141
 assert d.get('source_cobblemon_version')=='1.8.2'
 assert d.get('source_model_variant') in {slug+'.geo.json',slug+'_male.geo.json',slug+'_female.geo.json'}
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 assert d.get('mode')==('flying' if slug in FLYING else 'ground'),(slug,'unexpected active mode',d.get('mode'))
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
  clip=d[state];assert clip in animations,(slug,'missing '+state)
  assert set(profiles[slug][state])<=set(animations[clip].get('bones',{})),(slug,state,'profile not in selected clip')
 run=d.get('clips',{}).get('ground_run');assert bool(run)==(slug in RUNS),(slug,'native run mismatch')
 if run:assert set(profiles[slug]['run'])<=set(animations[run].get('bones',{})),(slug,'run profile mismatch')
 assert not d.get('movement_fallback'),(slug,'unexpected artificial fallback')
 if d.get('source_static_idle'):
  assert profiles[slug].get('idle_static')==profiles[slug]['idle'],(slug,'source static idle profile is not exact')
 assert all('.minemon_' not in c for c in d.get('clips',{}).values()),(slug,'synthetic optional action selected')
 for slug in ('articuno','zapdos','moltres','mewtwo'):
  d=defs[140+EXPECTED.index(slug)];a=readj(d['animation'])['animations']
  assert d['mode']=='flying' and d['idle']=='animation.'+slug+'.air_idle' and d['walk']=='animation.'+slug+'.air_fly'
  expected_transition=(['arm_left','arm_right','tail','tail9'] if slug=='mewtwo' else ['head','wing_open_left','wing_open_right'])
  assert profiles[slug].get('pose_transition')==expected_transition,(slug,'native aerial transition profile mismatch')
  assert set(expected_transition)<=set(profiles[slug]['walk']),(slug,'movement sampling omits a native aerial transition joint')
  if slug=='mewtwo':assert profiles[slug].get('idle_static')==profiles[slug]['idle'] and d.get('source_static_idle') is True
  for joint in expected_transition:
   idle_channel=a[d['idle']]['bones'][joint].get('rotation')
   fly_channel=a[d['walk']]['bones'][joint].get('rotation')
   assert isinstance(idle_channel,list) and isinstance(fly_channel,list),(slug,joint,'stable native rotation vectors required')
   assert len(idle_channel)==len(fly_channel)==3,(slug,joint,'native vector length')
   assert any(abs(float(x)-float(y))>0.05 for x,y in zip(idle_channel,fly_channel)),(slug,joint,'source aerial poses must change')
  if slug=='moltres':
   for clip in ('animation.moltres.air_idle','animation.moltres.air_fly'):
    assert {'head_fire2','head_fire3','tail_fire_puff2'}<=set(a[clip].get('bones',{})),(clip,'flame parts absent from native clip')
print('PASS BATCH 15 #141–#150: Kanto models, native locomotion/flight, source anatomy/events and exact first-140 preservation')
if args.jar:
 with zipfile.ZipFile(args.jar) as z:
  names=set(z.namelist());jar_defs=json.loads(z.read('data/minemon_gen1/runtime_species.json'));dex=json.loads(z.read('data/minemon_gen1/pokedex.json'))
  assert jar_defs==defs and len(dex)==151 and [d['nationalPokedexNumber'] for d in dex]==list(range(1,152))
  for d in defs[:150]:
   for key in ('model','texture','animation'):assert packed(d[key]) in names,(d['slug'],key,packed(d[key]))
  for d in defs[140:150]+defs[150:151]:
   packed_anim=json.loads(z.read(packed(d['animation'])))['animations'];source=readj(d['animation'])['animations']
   for name,clip in source.items():
    for key in ('sound_effects','particle_effects','timeline'):
     if key in clip:assert packed_anim[name][key]==clip[key],(d['slug'],name,key,'JAR event mismatch')
 print('PASS JAR BATCH 15: first 150 updated species, 151 Pokédex records, and native events verified')
