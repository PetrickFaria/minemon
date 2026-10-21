#!/usr/bin/env python3
"""Guard the Cobblemon Pidgeotto/Pidgeot alternate-wing pose adaptation."""
import json, sys, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ROOT_RES=ROOT/'src/main/resources'
SPECIES='data/minemon_gen1/runtime_species.json'

POSES={
 'pidgeotto':{
  'open':('air_sleep','air_fly','air_idle'),
  'closed':('sleep','ground_walk','ground_idle'),
 },
 'pidgeot':{
  'open':('air_sleep','air_fly','air_idle','mount_air_fly','mount_air_idle','air_glide','ride_air_fly','ride_air_glide'),
  'closed':('battle_sleep','sleep','ground_idle','ground_walk','mount_ground_idle','mount_ground_walk','ride_ground_walk'),
 },
}
SCALE={'open':{'wings_open':[1,1,1],'wings_closed':[0,0,0]},
       'closed':{'wings_open':[0,0,0],'wings_closed':[1,1,1]}}

def read(path, jar=None):
 if jar:
  return jar.read(path.replace('assets/minemon/','assets/minemon_gen1/'))
 return (ROOT_RES/path.replace('assets/minemon/','assets/minemon/')).read_bytes()

def verify(jar=None):
 if jar:
  definitions=json.loads(jar.read(SPECIES))
 else:
  definitions=json.loads((ROOT_RES/SPECIES).read_text())
 by_slug={d['slug']:d for d in definitions}
 for slug,states in POSES.items():
  definition=by_slug[slug]
  model=json.loads(read(definition['model'],jar))['minecraft:geometry'][0]
  bones={bone['name']:bone for bone in model['bones']}
  assert len(bones)==len(model['bones']),f'{slug}: duplicate bone name'
  for group in ('wings_open','wings_closed'):
   assert group in bones and bones[group].get('parent')=='wings',f'{slug}: missing {group} group under wings'
  for family in ('open','closed'):
   expected=('wing_'+family+'_left','wing_'+family+'_right')
   roots=[name for name,bone in bones.items() if name.startswith('wing_'+family+'_') and bone.get('parent')=='wings_'+family]
   assert set(roots)==set(expected),f'{slug}: unexpected {family} wing roots: {roots}'
   for name in expected:
    stack=[name];has_cube=False
    while stack:
     current=stack.pop();bone=bones[current]
     has_cube=has_cube or bool(bone.get('cubes'))
     stack.extend(n for n,b in bones.items() if b.get('parent')==current)
    assert has_cube,f'{slug}: {name} has no model geometry'

  animations=json.loads(read(definition['animation'],jar))['animations']
  for state,clips in states.items():
   for short in clips:
    name=f'animation.{slug}.{short}'
    assert name in animations,f'{slug}: missing poser clip {name}'
    groups=animations[name].get('bones',{})
    for group,expected in SCALE[state].items():
     actual=groups.get(group,{}).get('scale')
     assert actual==expected,f'{slug}/{short}: {group}.scale={actual}, expected {expected}'

  # Pidgeotto's native ground quirk intentionally crossfades between both
  # alternatives; it is retained as-is and must remain complementary.
  if slug=='pidgeotto':
   bones=animations['animation.pidgeotto.ground_quirk']['bones']
   assert 'scale' in bones['wings_open'] and 'scale' in bones['wings_closed']
   assert bones['wings_open']['scale']!=SCALE['open']['wings_open']
   assert bones['wings_closed']['scale']!=SCALE['closed']['wings_closed']
  if slug=='pidgeot':
   # The original dive pose switches the alternatives with complementary
   # Molang tracks. Do not flatten or discard its in-flight wing motion.
   bones=animations['animation.pidgeot.ride_dive']['bones']
   assert 'scale' in bones['wings_open'] and 'scale' in bones['wings_closed']
   assert isinstance(bones['wings_open']['scale'],str) and isinstance(bones['wings_closed']['scale'],str)

  assert definition['idle'] in animations and definition['walk'] in animations
  idle=animations[definition['idle']]['bones']
  walk=animations[definition['walk']]['bones']
  for clip_name,clip_bones in ((definition['idle'],idle),(definition['walk'],walk)):
   assert clip_bones['wings_open']['scale']==[1,1,1],f'{slug}: active flight clip {clip_name} hides open wings'
   assert clip_bones['wings_closed']['scale']==[0,0,0],f'{slug}: active flight clip {clip_name} exposes closed wings'
 print('PASS PIDGE WINGS: source-backed open/closed rigs remain intact; poser-selected clips hide the alternate pair; native wing motion and dynamic transitions are preserved')

if __name__=='__main__':
 if len(sys.argv)>1:
  with zipfile.ZipFile(sys.argv[1]) as archive: verify(archive)
 else: verify()
