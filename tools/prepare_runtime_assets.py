#!/usr/bin/env python3
"""Adapt copied runtime animations to GeckoLib 3; original assets remain intact."""
import json,re,sys
from pathlib import Path
root=Path(sys.argv[1])
count=0
manifest=root/'data/minemon_gen1/runtime_species.json'
definitions={d['animation'].replace('assets/minemon/','assets/minemon_gen1/'):d for d in json.loads(manifest.read_text())} if manifest.exists() else {}
def add_pose_animation(data, geo, d, name, kind):
 animations=data.setdefault('animations',{})
 if name in animations:return
 clips=d.get('clips',{})
 idle_name=d.get('idle') or clips.get('ground_idle') or clips.get('air_idle')
 source=animations.get(idle_name)
 if not source:return
 animation=json.loads(json.dumps(source));animation['loop']=kind!='physical_attack';animation['animation_length']=0.6 if kind=='physical_attack' else 4.0
 bones={b['name'] for g in geo.get('minecraft:geometry',[]) for b in g.get('bones',[])}
 channels=animation.setdefault('bones',{})
 if kind=='sit':
  root=next((n for n in ('body','torso','torso2') if n in bones),None)
  if root:
   channels.setdefault(root,{})['position']=[0,-1.0,0]
   channels[root]['rotation']=[8,0,0]
  for n in ('leg_left','leg_right','leg_front_left','leg_front_right','leg_back_left','leg_back_right'):
   if n in bones:channels.setdefault(n,{})['rotation']=[24,0,0]
  for n in ('leg_left2','leg_right2','knee_left','knee_right'):
   if n in bones:channels.setdefault(n,{})['rotation']=[-32,0,0]
 elif kind=='sleep':
  root=next((n for n in ('body','torso','torso2') if n in bones),None)
  if root:
   channels.setdefault(root,{})['position']=[0,-1.4,0]
   channels[root]['rotation']=[18,0,0]
  for n in ('eyelid_left','eyelid_right','eyelid','eye_lid'):
   if n in bones:channels.setdefault(n,{})['position']=[0,-2,0]
 elif kind=='physical_attack':
  root=next((n for n in ('torso','body','head','head_ai') if n in bones),None)
  attack_curve={'0.0':{'vector':[0,0,0]},'0.15':{'vector':[-20,0,0]},'0.32':{'vector':[12,0,0]},'0.6':{'vector':[0,0,0]}}
  if root:channels.setdefault(root,{})['rotation']=attack_curve
  for n in ('arm_right','arm_left','forearm_right','forearm_left','leg_right','leg_left','tail'):
   if n in bones:
    angle=-34 if 'right' in n or 'tail' in n else 24
    channels.setdefault(n,{})['rotation']={'0.0':{'vector':[0,0,0]},'0.15':{'vector':[angle,0,0]},'0.32':{'vector':[-angle*0.45,0,0]},'0.6':{'vector':[0,0,0]}}
 animations[name]=animation

def build_runtime_clips(data,geo,d):
 animations=data.setdefault('animations',{});clips=d.get('clips',{})
 walk_name=d.get('walk')
 run_name=clips.get('ground_run','')
 if run_name.endswith('.minemon_run') and walk_name in animations:
  run=json.loads(json.dumps(animations[walk_name]));run['loop']=True
  def speed(v):
   if isinstance(v,str):return re.sub(r'\b(query|q)\.anim_time\s*\*\s*(\d+(?:\.\d+)?)',lambda m:f'{m.group(1)}.anim_time*{float(m.group(2))*1.35:.6g}',v)
   if isinstance(v,list):return [speed(x) for x in v]
   if isinstance(v,dict):return {k:speed(x) for k,x in v.items()}
   return v
  animations[run_name]=speed(run)
 for key,kind in [('sit','sit'),('sleep','sleep'),('physical_attack','physical_attack')]:
  name=clips.get(key,'')
  if name.endswith('.minemon_'+kind):add_pose_animation(data,geo,d,name,kind)
for p in (root/'assets/minemon_gen1/animations').rglob('*.animation.json'):
 data=json.loads(p.read_text())
 def normalize(v):
  if isinstance(v,str): return re.sub(r'\bq\.', 'query.',v)
  if isinstance(v,list): return [normalize(x) for x in v]
  if isinstance(v,dict): return {k:normalize(x) for k,x in v.items()}
  return v
 data=normalize(data)
 definition=definitions.get(str(p.relative_to(root)),{})
 for name in definition.get('excluded_clips',[]): data.get('animations',{}).pop(name,None)
 if definition:
  # Files in the processed resource tree keep the normal assets/minemon root.
  model=root/'assets/minemon_gen1'/definition['model'].removeprefix('assets/minemon/')
  if model.exists():build_runtime_clips(data,json.loads(model.read_text()),definition)
 # GeckoLib 3.1.40 dereferences this field while parsing every clip, including
 # expression-only looping clips. Keep the source untouched and add a runtime fallback.
 for animation in data.get('animations',{}).values():
  for field in ('particle_effects','sound_effects'):
   events=animation.get(field)
   if not isinstance(events,dict) or any(not isinstance(value,dict) for value in events.values()): animation.pop(field,None)
  if 'animation_length' not in animation:animation['animation_length']=4.0
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
 count+=1
print('GeckoLib 3 runtime animations prepared:',count)
