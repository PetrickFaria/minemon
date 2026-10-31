#!/usr/bin/env python3
"""Verify Batch 9 assets, source evidence, cumulative preservation, and packaged JAR."""
import argparse, json, math, re, struct, subprocess, zipfile
from pathlib import Path
from verify_approved_asset_delta import is_approved_prior_asset_delta, pokec_resource_paths

ROOT=Path(__file__).resolve().parents[1]
BASE='2503dd085402211ec2e9c6287fca83ab4747d2d5'
MP='src/main/resources/data/minemon_gen1/runtime_species.json'
VP='src/main/resources/data/minemon_gen1/animation_validation_profiles.json'
EXPECTED=['magnemite','magneton','farfetchd','doduo','dodrio','seel','dewgong','grimer','muk','shellder']
BATCH10=['cloyster','gastly','haunter','gengar','onix','drowzee','hypno','krabby','kingler','voltorb']
BATCH11=['electrode','exeggcute','exeggutor','cubone','marowak','hitmonlee','hitmonchan','lickitung','koffing','weezing']
BATCH12=['rhyhorn','rhydon','chansey','tangela','kangaskhan','horsea','seadra','goldeen','seaking','staryu']
BATCH13=['starmie','mrmime','scyther','jynx','electabuzz','magmar','pinsir','tauros','magikarp','gyarados']
BATCH14=['lapras','ditto','eevee','vaporeon','jolteon','flareon','porygon','omanyte','omastar','kabuto']
BATCH15=['kabutops','aerodactyl','snorlax','articuno','zapdos','moltres','dratini','dragonair','dragonite','mewtwo']
BATCH16=['mew']
ap=argparse.ArgumentParser()
ap.add_argument('--segment',choices=('first','second'))
ap.add_argument('jar',nargs='?')
args=ap.parse_args()
assert not args.segment or not args.jar,'JAR verification is for the complete batch only'
start,end=(81,90)
if args.segment=='first':start,end=81,85
if args.segment=='second':start,end=86,90
authorized=EXPECTED[start-81:end-80]
R=ROOT/'src/main/resources'
def base_file(path):return subprocess.check_output(['git','show',BASE+':'+path],cwd=ROOT)
def normalize(v):
 if isinstance(v,str):return re.sub(r'\bq\.', 'query.',v).replace('Math.','math.')
 if isinstance(v,list):return [normalize(x) for x in v]
 if isinstance(v,dict):return {k:normalize(x) for k,x in v.items()}
 return v

defs=json.loads((R/MP.removeprefix('src/main/resources/')).read_text())
old_defs=json.loads(base_file(MP))
profiles=json.loads((R/VP.removeprefix('src/main/resources/')).read_text())
old_profiles=json.loads(base_file(VP))
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert [d['slug'] for d in defs[80:90]]==EXPECTED
prior_defs=json.loads(json.dumps(defs[:80]))
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
assert prior_defs==old_defs[:80],'An approved prior definition changed beyond the explicit fire-starter flame adaptation'
allowed_l9=set(EXPECTED[:end-80])
assert all(defs[i]==old_defs[i] for i in range(80,90) if defs[i]['slug'] not in allowed_l9),'Unimplemented Batch 9 species definition changed'
assert [d['slug'] for d in defs[90:100]]==BATCH10,'Batch 10 must remain the planned #091-100 sequence'
assert [d['slug'] for d in defs[100:110]]==BATCH11,'Batch 11 must remain the planned #101-110 sequence'
assert [d['slug'] for d in defs[110:120]]==BATCH12,'Batch 12 must remain the planned #111-120 sequence'
assert [d['slug'] for d in defs[120:130]]==BATCH13 and [d['slug'] for d in defs[130:140]]==BATCH14 and [d['slug'] for d in defs[140:150]]==BATCH15,'Batch 13 plan or later species changed'
for slug,old_profile in old_profiles.items():
 expected_profile=old_profile
 if slug=='rapidash':
  expected_profile=dict(old_profile)
  expected_profile['run']=[*old_profile['run'],'fire_mane_head_flame']
 if slug=='geodude':
  expected_profile=dict(old_profile)
  expected_profile['walk']=['body','torso']
  expected_profile['run']=['body','torso']
 if slug=='alakazam':
  expected_profile=dict(old_profile)
  expected_profile['idle']=['foot_left','foot_right','left_lower_leg','right_lower_leg','hand_left','hand_right']
 assert profiles.get(slug)==expected_profile,'Earlier animation profile changed: '+slug
assert set(profiles)==set(old_profiles)|set(EXPECTED[:end-80])|{s for s in BATCH10+BATCH11+BATCH12+BATCH13+BATCH14+BATCH15+BATCH16 if s in profiles},'Unexpected articulation profile set'
source_report=json.loads((ROOT/'docs/animation-audit/minemon-gen1-batch9-source-evidence.json').read_text())
assert source_report['source_sha256']=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2'
assert [s['slug'] for s in source_report['species']]==EXPECTED[:end-80]

prior_paths=set()
for d in old_defs[:80]:prior_paths.update([d['model'],d['texture'],d['animation']]+d['licenses'])
for p in prior_paths:
 current=(R/p).read_bytes()
 original=base_file('src/main/resources/'+p)
 if p=='assets/minemon/animations/weedle_cobblemon_1_8_2/weedle.animation.json':
  expected=original.replace(b'"-(math.sin(q.anim_time*90*4)*1)*15+',b'"-1*(math.sin(q.anim_time*90*4)*1)*15+')
  assert original.count(b'"-(math.sin(q.anim_time*90*4)*1)*15+')==1
  assert current==expected,'Weedle parser-compatibility adjustment must remain algebraically equivalent and isolated'
 elif p=='assets/minemon/animations/pidgeotto_cobblemon_1_8_2/pidgeotto.animation.json':
  expected=original.replace(b'"+math.cos(q.anim_time*90*16/6-60)*3"',b'"math.cos(q.anim_time*90*16/6-60)*3"')
  expected=expected.replace(b'"-math.sin(q.anim_time*90*16/3-60)*+9.5"',b'"-math.sin(q.anim_time*90*16/3-60)*9.5"')
  assert original.count(b'"+math.cos(q.anim_time*90*16/6-60)*3"')==2
  assert original.count(b'"-math.sin(q.anim_time*90*16/3-60)*+9.5"')==2
  assert current==expected,'Pidgeotto Molang unary-plus fix must be isolated to its two invalid active expressions'
 elif not is_approved_prior_asset_delta(p,current):assert current==original,'Prior approved asset changed: '+p

allowed=set()
for d in defs[80:80+len(authorized)]:
 assert d['source_cobblemon_version']=='1.8.2'
 assert all('_cobblemon_1_8_2/' in d[k] for k in ('model','texture','animation'))
 allowed.update([d['model'],d['texture'],d['animation']]+d['licenses'])
for d in defs[90:100]:
 if d.get('source_cobblemon_version')=='1.8.2':allowed.update([d['model'],d['texture'],d['animation']]+d['licenses'])
for d in defs[100:110]:
 if d.get('source_cobblemon_version')=='1.8.2':allowed.update([d['model'],d['texture'],d['animation']]+d['licenses'])
for d in defs[110:120]+defs[120:130]+defs[130:140]+defs[140:150]+defs[150:151]:
 if d.get('source_cobblemon_version')=='1.8.2':allowed.update([d['model'],d['texture'],d['animation']]+d['licenses'])
allowed_paths={str(Path('src/main/resources')/p) for p in allowed}
geo_by_slug={};anim_by_slug={}
for d in defs[80:80+len(authorized)]:
 slug=d['slug']
 geo=json.loads((R/'assets/minemon'/d['model'].removeprefix('assets/minemon/')).read_text())['minecraft:geometry']
 bones={}
 for part in geo:
  for b in part['bones']:
   assert b['name'] not in bones,(slug,'duplicate bone',b['name']);bones[b['name']]=b
 for name,b in bones.items():
  seen={name};parent=b.get('parent')
  while parent:
   assert parent in bones and parent not in seen,(slug,'broken hierarchy',name,parent)
   seen.add(parent);parent=bones[parent].get('parent')
  for axis in ('pivot','rotation'):
   assert all(isinstance(v,(int,float)) and math.isfinite(v) for v in b.get(axis,[])),(slug,name,axis)
  for cube in b.get('cubes',[]):assert all(math.isfinite(v) for v in cube['origin']+cube['size']),(slug,name,'cube')
 tex=(R/'assets/minemon'/d['texture'].removeprefix('assets/minemon/')).read_bytes()
 assert tex[:8]==b'\x89PNG\r\n\x1a\n',(slug,'invalid PNG')
 assert struct.unpack('>II',tex[16:24])==(geo[0]['description']['texture_width'],geo[0]['description']['texture_height']),(slug,'texture dimensions')
 anim=json.loads((R/'assets/minemon'/d['animation'].removeprefix('assets/minemon/')).read_text())['animations']
 for name,clip in anim.items():
  missing=set(clip.get('bones',{}))-set(bones)
  assert missing==set(d.get('stripped_bones',{}).get(name,[])),(slug,name,'missing animation bones',missing)
  encoded=json.dumps(clip)
  assert not re.search(r'\bq\.[A-Za-z_]',encoded),(slug,name,'unconverted Bedrock query')
  assert 'Math.' not in encoded,(slug,name,'unconverted Math namespace')
 for name,events in d.get('source_event_evidence',{}).items():
  for event,payload in events.items():assert anim[name][event]==normalize(payload),(slug,name,event,'source event changed')
 for name in d['clips'].values():assert name in anim or '.minemon_' in name,(slug,'missing clip',name)
 assert d['idle'] in anim and d['walk'] in anim,(slug,'selected idle/walk missing')
 for state in ('idle','walk'):
  clip=anim[d[state]]
  assert set(profiles[slug][state])<=set(clip.get('bones',{})),(slug,state,'profile joints are not in selected clip')
 if d['clips'].get('ground_run'):
  assert set(profiles[slug]['run'])<=set(anim[d['clips']['ground_run']].get('bones',{})),(slug,'run profile joints absent')
 if d.get('source_static_idle'):
  assert profiles[slug].get('idle_static')==profiles[slug]['idle'],(slug,'source static idle profile mismatch')
 geo_by_slug[slug]=bones;anim_by_slug[slug]=anim

anatomy={
 'magnemite':('nail_left_rotation','nail_right_rotation','magnet_left','magnet_right'),
 'magneton':('body2','body3','magnet_left_rotation','magnet_right_rotation'),
 'farfetchd':('leak','wing_left','wing_right'),
 'doduo':('head','head2','neck_left','neck_right','leg_left','leg_right'),
 'dodrio':('head2','head3','neck','neck_left','neck_right','leg_left','leg_right'),
 'seel':('flipper_left','flipper_right','tail'),
 'dewgong':('flipper_left','flipper_right','tail'),
 'grimer':('body','arm_left','arm_right','back_tail','front_tail'),
 'muk':('body','arm_left','arm_right','mouth_sludge'),
 'shellder':('shell_top','shell_bottom','tongue')}
for slug,joints in anatomy.items():
 if slug not in authorized:continue
 assert set(joints)<=set(geo_by_slug[slug]),(slug,'missing characteristic anatomy')
 selected=set(anim_by_slug[slug][defs[80+EXPECTED.index(slug)]['walk']].get('bones',{}))
 assert set(joints)&selected,(slug,'walk/flight clip omits characteristic joints')

if not args.segment:
 assert [defs[i]['slug'] for i in range(80,90)]==EXPECTED
 assert {defs[i]['slug'] for i in (80,81,82,83,84,85,86,87,88,89)}==set(EXPECTED)
 assert [d['slug'] for d in defs[:90] if d['model'].find('_cobblemon_1_8_2/')>=0]==[d['slug'] for d in defs[:90]],'All first 90 definitions must use imported models'
 assert [d['slug'] for d in defs[80:90] if d['clips'].get('ground_run')]==['grimer'],'Do not invent missing native run clips'
 assert all(defs[i]['movement_fallback']=='idle' for i in (85,88)),'Seel and Muk source walk limitations must be explicit'
 assert {d['slug'] for d in defs[80:90] if d.get('source_static_idle')}=={'seel','muk'},'Only source-verified static idle clips may use the stable-pose profile'

changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
allowed_resources=allowed_paths|{MP,VP,'docs/animation-audit/minemon-gen1-batch9-source-evidence.json','docs/animation-audit/minemon-gen1-batch10-source-evidence.json','docs/animation-audit/minemon-gen1-batch11-source-evidence.json','src/main/resources/assets/minemon/animations/weedle_cobblemon_1_8_2/weedle.animation.json','src/main/resources/assets/minemon/animations/pidgeotto_cobblemon_1_8_2/pidgeotto.animation.json'}
source_fire_frames={f'src/main/resources/assets/minemon/textures/pokemon/{slug}_cobblemon_1_8_2/fire/normal/{frame}.png' for slug in ('ponyta','rapidash') for frame in range(1,5)}
allowed_resources.update(source_fire_frames)
allowed_resources.add('src/main/resources/assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png')
allowed_resources.update({'src/main/resources/assets/minemon_gen1/particles/gastly_ghostly_smoke.json',*(f'src/main/resources/assets/minemon_gen1/textures/particle/gastly_ghostly_smoke_{i}.png' for i in range(6))})
allowed_resources.update(pokec_resource_paths())
allowed_resources.add('src/main/resources/assets/minemon_gen1/particles/gastly_small_smoke.json')
assert set(changed)<=allowed_resources,set(changed)-allowed_resources
print(f'PASS BATCH 9 {start}-{end}: source 1.8.2, rig/texture/clips/events, selected profile joints, and previous 80 resource preservation')
if args.jar:
 with zipfile.ZipFile(args.jar) as jar:
  packed=json.loads(jar.read('data/minemon_gen1/runtime_species.json'))
  dex=json.loads(jar.read('data/minemon_gen1/pokedex.json'))
  assert packed==defs
  assert len(dex)==151 and [d['nationalPokedexNumber'] for d in dex]==list(range(1,152))
  assert {d['slug'] for d in packed}=={d['slug'] for d in dex if d['registered']}
  for d in defs[:90]:
   for key in ('model','texture','animation'):
    path=d[key].replace('assets/minemon/','assets/minemon_gen1/',1)
    assert path in jar.namelist(),(d['slug'],key,path)
  for d in defs[80:90]:
   path=d['animation'].replace('assets/minemon/','assets/minemon_gen1/',1)
   packed_anim=json.loads(jar.read(path))['animations']
   for name,clip in anim_by_slug[d['slug']].items():
    for key in ('sound_effects','particle_effects','timeline'):
     if key not in clip:continue
     if key in ('sound_effects','particle_effects'):
      events=clip[key]
      # prepare_runtime_assets.py removes GeckoLib 3 event blocks when any
      # timestamp carries a non-object value (for example Magneton's particle
      # arrays), while retaining the untouched source resource in the repo.
      if not isinstance(events,dict) or any(not isinstance(value,dict) for value in events.values()):
       assert key not in packed_anim[name],(d['slug'],name,key,'unsupported event block should be removed at runtime')
       continue
     assert packed_anim[name].get(key)==normalize(clip[key]),(d['slug'],name,key)
  print('PASS JAR BATCH 9: 90 updated species assets, 151-entry Pokédex, source events, and cumulative resources verified')
