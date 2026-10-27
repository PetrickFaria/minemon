#!/usr/bin/env python3
"""Verify native Cobblemon batch 13 and exact cumulative preservation through #120."""
import argparse, json, math, struct, subprocess, zipfile
from pathlib import Path
from verify_approved_asset_delta import is_approved_prior_asset_delta, pokec_resource_paths

ROOT=Path(__file__).resolve().parents[1]
BASE='364fab1caf136d2e645f6ca77c4199570230eabd'
R=ROOT/'src/main/resources'
MP='data/minemon_gen1/runtime_species.json'
VP='data/minemon_gen1/animation_validation_profiles.json'
EXPECTED=['starmie','mrmime','scyther','jynx','electabuzz','magmar','pinsir','tauros','magikarp','gyarados']
BATCH15=['kabutops','aerodactyl','snorlax','articuno','zapdos','moltres','dratini','dragonair','dragonite','mewtwo']
BATCH16=['mew']
ACTIVE_RUNS={'starmie','mrmime','tauros'}
STATIC_IDLE={'jynx','magmar'}
ANATOMY={
 'starmie':['body','body_back','body_limb_top','body_limb_left','body_limb_right','body_limb_bottom_left','body_limb_bottom_right'],
 'mrmime':['head','arm_left','arm_right','wrist_left','wrist_right','foot_left','foot_right'],
 'scyther':['left_wing','right_wing','left_upper_wing','right_upper_wing','left_scythe','right_scythe'],
 'jynx':['hair_front_left','hair_front_right','dress_front','hand_left','hand_right'],
 'electabuzz':['arm_fur_left','arm_fur_right','leg_left','leg_right','hand_left','hand_right','tail'],
 'magmar':['arm_left','arm_right','elbow_frill_left','elbow_frill_right','tail','tail2','tail3','tail4'],
 'pinsir':['leg_left','leg_right','pincer_left','pincer_right','pincer_left2','pincer_right2'],
 'tauros':['leg_front_left','leg_front_right','leg_back_left','leg_back_right','tail_left','tail_middle','tail_right'],
 'magikarp':['fin_left','fin_right','tail','tail2','tail3','whisker_left','whisker_right'],
 'gyarados':['head','neck1','neck2','neck3','neck4','segment','segment2','segment3','segment4','segment5','segment6','segment7','segment8','segment9','tail1','tail2','tail3']}

ap=argparse.ArgumentParser();ap.add_argument('jar',nargs='?');args=ap.parse_args()
def readj(path): return json.loads((R/path).read_bytes())
def base(path): return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
def packed(path): return path.replace('assets/minemon/','assets/minemon_gen1/',1)

defs=readj(MP); profiles=readj(VP)
old_defs=json.loads(base('src/main/resources/'+MP)); old_profiles=json.loads(base('src/main/resources/'+VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert [d['slug'] for d in defs[120:130]]==EXPECTED
prior_defs=json.loads(json.dumps(defs[:120]))
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
assert prior_defs==old_defs[:120],'An approved prior definition changed beyond the explicit fire-starter flame adaptation'
assert [d['slug'] for d in defs[130:140]]==['lapras','ditto','eevee','vaporeon','jolteon','flareon','porygon','omanyte','omastar','kabuto'] and [d['slug'] for d in defs[140:150]]==BATCH15, 'Batch 14 plan or a later definition changed'
assert profiles.keys()==(old_profiles.keys()|set(EXPECTED)|{'lapras','ditto','eevee','vaporeon','jolteon','flareon','porygon','omanyte','omastar','kabuto','kabutops','aerodactyl','snorlax','articuno','zapdos','moltres','dratini','dragonair','dragonite','mewtwo','mew'}), 'Unexpected/missing articulation profile'
for slug, prior in old_profiles.items():
 expected_profile=dict(prior)
 if slug=='rapidash':expected_profile['run']=[*prior['run'],'fire_mane_head_flame']
 assert profiles[slug]==expected_profile, 'Earlier profile changed: '+slug

report=json.loads((ROOT/'docs/animation-audit/minemon-gen1-batch13-source-evidence.json').read_text())
assert report['batch']==13 and report['range']==[121,130]
assert report['source_sha256']=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2'
assert [x['slug'] for x in report['species']]==EXPECTED
old_resources=set()
for d in old_defs[:120]: old_resources.update([d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
for p in old_resources:
 current=(R/p).read_bytes()
 assert is_approved_prior_asset_delta(p,current) or current==base('src/main/resources/'+p), 'Prior asset changed: '+p
allowed={'src/main/resources/'+MP,'src/main/resources/'+VP,'tools/import_cobblemon_182_batch.py','tools/verify_batch13.py','src/test/java/br/com/petrickfaria/minemon/Batch13AnimationCompatibilityTest.java','docs/animation-audit/minemon-gen1-batch13-progress.md','docs/animation-audit/minemon-gen1-batch13-source-evidence.json'}
for d in defs[120:150]+defs[150:151]: allowed.update('src/main/resources/'+p for p in [d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
source_fire_frames={f'src/main/resources/assets/minemon/textures/pokemon/{slug}_cobblemon_1_8_2/fire/normal/{frame}.png' for slug in ('ponyta','rapidash') for frame in range(1,5)}
allowed.update(source_fire_frames)
allowed.add('src/main/resources/assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png')
allowed.update({'src/main/resources/assets/minemon_gen1/particles/gastly_ghostly_smoke.json',*(f'src/main/resources/assets/minemon_gen1/textures/particle/gastly_ghostly_smoke_{i}.png' for i in range(6))})
changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
changed+=subprocess.check_output(['git','ls-files','--others','--exclude-standard','--','src/main/resources'],cwd=ROOT,text=True).splitlines()
allowed.update(pokec_resource_paths())
assert set(changed)<=allowed, set(changed)-allowed

for idx,d in enumerate(defs[120:130]):
 slug=d['slug']; assert d['nationalPokedexNumber']==idx+121
 assert d.get('source_cobblemon_version')=='1.8.2'
 permitted=(['mr_mime.geo.json'] if slug=='mrmime' else [slug+'.geo.json',slug+'_male.geo.json',slug+'_female.geo.json'])
 assert d.get('source_model_variant') in permitted, (slug,d.get('source_model_variant'))
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 geo=readj(d['model'])['minecraft:geometry'];assert geo
 bones={}
 for part in geo:
  for b in part['bones']:
   assert b['name'] not in bones,(slug,'duplicate bone',b['name']);bones[b['name']]=b
 for name,b in bones.items():
  seen={name};parent=b.get('parent')
  while parent: assert parent in bones and parent not in seen,(slug,'invalid parent chain',name,parent);seen.add(parent);parent=bones[parent].get('parent')
  for axis in ('pivot','rotation'): assert all(math.isfinite(float(v)) for v in b.get(axis,[])),(slug,name,axis)
  for cube in b.get('cubes',[]): assert all(math.isfinite(float(v)) for v in cube['origin']+cube['size']),(slug,name,'cube')
 assert set(ANATOMY[slug])<=set(bones),(slug,'missing anatomy',set(ANATOMY[slug])-set(bones))
 tex=(R/d['texture']).read_bytes();assert tex.startswith(b'\x89PNG\r\n\x1a\n')
 assert struct.unpack('>II',tex[16:24])==(geo[0]['description']['texture_width'],geo[0]['description']['texture_height']),(slug,'texture dimensions')
 animations=readj(d['animation'])['animations'];assert animations
 for name,clip in animations.items():
  assert set(clip.get('bones',{}))-set(bones)==set(d.get('stripped_bones',{}).get(name,[])),(slug,name,'untracked missing bones')
  if clip.get('animation_length') is not None: assert clip['animation_length']>0
  elif clip.get('loop') is True: pass
  elif clip.get('loop')=='hold_on_last_frame': assert clip.get('bones'),(slug,name,'empty source hold clip')
  else: assert not clip.get('bones') and set(clip)<={'sound_effects','particle_effects','timeline'},(slug,name,'invalid durationless clip')
  if name in d.get('source_event_evidence',{}):
   for key,value in d['source_event_evidence'][name].items(): assert clip[key]==value,(slug,name,key,'source event changed')
 for state in ('idle','walk'):
  assert d[state] in animations,(slug,'missing '+state)
  assert set(profiles[slug][state])<=set(animations[d[state]].get('bones',{})),(slug,state,'profile not in selected clip')
 run=d.get('clips',{}).get('ground_run')
 assert bool(run)==(slug in ACTIVE_RUNS),(slug,'native run selection mismatch')
 if run: assert set(profiles[slug]['run'])<=set(animations[run].get('bones',{})),(slug,'run profile not in native run')
 assert all('.minemon_' not in c for c in d.get('clips',{}).values()),(slug,'generated action placeholder selected')
 if slug in STATIC_IDLE:
  assert d.get('source_static_idle') is True and d.get('movement_fallback')=='idle'
  assert profiles[slug].get('idle_static')==profiles[slug]['idle']
 else: assert not d.get('source_static_idle'),(slug,'unexpected static idle classification')
 if slug=='mrmime': assert d['source_model_variant']=='mr_mime.geo.json'
for slug in ('starmie','magikarp','gyarados'):
 clips=defs[120+EXPECTED.index(slug)]['clips']
 assert clips.get('water_idle') and clips.get('water_swim'),(slug,'native water clips lost')
print('PASS BATCH 13 #121–#130: Kanto models, native articulation/clips, Molang resources and exact prior-120 preservation')

if args.jar:
 with zipfile.ZipFile(args.jar) as z:
  names=set(z.namelist()); packed_defs=json.loads(z.read('data/minemon_gen1/runtime_species.json')); dex=json.loads(z.read('data/minemon_gen1/pokedex.json'))
  assert packed_defs==defs and len(dex)==151 and [d['nationalPokedexNumber'] for d in dex]==list(range(1,152))
  for d in defs[:130]:
   for key in ('model','texture','animation'): assert packed(d[key]) in names,(d['slug'],key,packed(d[key]))
  for d in defs[120:130]:
   packed_anim=json.loads(z.read(packed(d['animation'])))['animations'];source=readj(d['animation'])['animations']
   for name,clip in source.items():
    for key in ('sound_effects','particle_effects','timeline'):
     if key in clip: assert packed_anim[name][key]==clip[key],(d['slug'],name,key,'JAR event mismatch')
 print('PASS JAR BATCH 13: first 130 updated species, 151 dex entries, and native batch 13 resources')
