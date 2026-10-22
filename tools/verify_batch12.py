#!/usr/bin/env python3
"""Verify native Cobblemon batch 12 and exact cumulative preservation through #110."""
import argparse,json,math,struct,subprocess,zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1];BASE='0816533addee80e9e17a684c94bed399f4a9488f'
R=ROOT/'src/main/resources';MP='data/minemon_gen1/runtime_species.json';VP='data/minemon_gen1/animation_validation_profiles.json'
EXPECTED=['rhyhorn','rhydon','chansey','tangela','kangaskhan','horsea','seadra','goldeen','seaking','staryu']
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
assert defs[:110]==old_defs[:110],'Approved resources #001–#110 definitions changed'
assert defs[120:]==old_defs[120:],'A definition after #120 changed'
assert all(profiles.get(s)==p for s,p in old_profiles.items()),'Earlier articulation profile changed'
assert set(profiles)==set(old_profiles)|set(EXPECTED),'Unexpected or missing articulation profile'
report=json.loads((ROOT/'docs/animation-audit/minemon-gen1-batch12-source-evidence.json').read_text())
assert report['batch']==12 and report['range']==[111,120]
assert report['source_sha256']=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2'
assert [x['slug'] for x in report['species']]==EXPECTED

prior=set()
for d in old_defs[:110]:prior.update([d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
for path in prior:assert (R/path).read_bytes()==base('src/main/resources/'+path),'Prior approved asset changed: '+path
allowed={'src/main/resources/'+MP,'src/main/resources/'+VP}
for d in defs[110:120]:allowed.update('src/main/resources/'+p for p in [d['model'],d['texture'],d['animation']]+d.get('licenses',[]))
changed=subprocess.check_output(['git','diff','--name-only',BASE,'--','src/main/resources'],cwd=ROOT,text=True).splitlines()
changed+=subprocess.check_output(['git','ls-files','--others','--exclude-standard','--','src/main/resources'],cwd=ROOT,text=True).splitlines()
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
