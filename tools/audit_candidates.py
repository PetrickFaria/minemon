#!/usr/bin/env python3
"""Audit the first 151 National Pokédex entries using the extracted source assets."""
import json, re, struct, math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources'
CAT={s['slug']:s for s in json.loads((R/'data/minemon/species_catalog.json').read_text())}
CANDIDATES=[s['slug'] for s in sorted(CAT.values(),key=lambda x:x['nationalPokedexNumber']) if 1<=s['nationalPokedexNumber']<=151]
# These were already confirmed as requiring nontrivial flight behavior in the prior build.
FLYING={'haunter','gengar','mew','scyther','zapdos'}

def audit():
 results=[]
 for slug in CANDIDATES:
  s=CAT[slug];res=s['resources'];issues=[]
  models=res['models'];animations=res['animations'];textures=res['textures']
  model=next((p for p in models if p.endswith('/'+slug+'.geo.json')),models[0] if models else None)
  if slug in ('pikachu','scyther'):
   model=next((p for p in models if p.endswith('/'+slug+'_male.geo.json')),model)
  animation=next((p for p in animations if p.endswith('/'+slug+'.animation.json')),animations[0] if animations else None)
  texture=next((p for p in textures if p.endswith('/'+slug+'.png') and not p.endswith('_shiny.png')),textures[0] if textures else None)
  if not model or not (R/model).is_file():issues.append('Missing model file')
  if not texture or not (R/texture).is_file():issues.append('Missing texture file')
  generated_static_animation=False
  if not animation or not (R/animation).is_file():
   if not issues:
    animation=f'assets/minemon/animations/{slug}/{slug}.animation.json'
    generated_static_animation=True
   else:issues.append('Missing animation file')
  if issues:
   results.append(dict(slug=slug,accepted=False,reason='; '.join(issues),issues=issues,definition=None));continue
  try:
   gd=json.loads((R/model).read_text());ad={} if generated_static_animation else json.loads((R/animation).read_text()).get('animations',{})
   bones={b['name'] for g in gd['minecraft:geometry'] for b in g['bones']}
   parents={b.get('parent') for g in gd['minecraft:geometry'] for b in g['bones'] if b.get('parent')}
   if parents-bones:issues.append('Missing geometry parents: '+str(sorted(parents-bones)))
   dims=struct.unpack('>II',(R/texture).read_bytes()[16:24]);desc=gd['minecraft:geometry'][0]['description']
   if dims!=(desc['texture_width'],desc['texture_height']):issues.append('Texture and model UV dimensions differ')
   clips={n.rsplit('.',1)[-1]:n for n in ad}
   mode='flying' if slug in FLYING or ('air_idle' in clips and 'air_fly' in clips) else 'ground'
   idle=next((clips[n] for n in (('air_idle','ground_idle','idle') if mode=='flying' else ('ground_idle','idle','air_idle')) if n in clips),None)
   walk=next((clips[n] for n in (('air_fly','ground_walk','walk','ground_run','run') if mode=='flying' else ('ground_walk','walk','ground_run','run')) if n in clips),None)
   movement_fallback='static' if generated_static_animation else None
   if generated_static_animation:
    idle=walk=f'animation.{slug}.minemon_static'
    clips={'ground_idle':idle,'ground_walk':walk}
    mode='ground'
   elif not idle:
    # Use an existing eye-blink or sleep pose only as a safe static fallback.
    # The manifest will label this as static; no missing animation is invented.
    idle=next((clips[n] for n in ('blink','blink1','sleep') if n in clips),None)
   if slug=='tangela' and not idle and not walk:
    # Tangela's extracted file has only a cry event and no body animation.
    # Keep its validated model/texture and use the existing static fallback path.
    generated_static_animation=True
    idle=walk=f'animation.{slug}.minemon_static'
    clips['ground_idle']=idle
    clips['ground_walk']=walk
    movement_fallback='static'
    mode='ground'
   if not idle:issues.append('No usable idle clip')
   excluded=[];stripped_bones={}
   # Keep useful source clips when only some channels target bones absent from
   # this model. The original files stay untouched; runtime preparation strips
   # precisely these channels and the client smoke test validates the result.
   for name,a in ad.items():
    bad_bones=set(a.get('bones',{}))-bones
    if bad_bones:
     if name in (idle,walk):
      remaining={bone:channels for bone,channels in a.get('bones',{}).items() if bone in bones}
      if not remaining:issues.append(name+' has no channels for bones in the model')
      else:stripped_bones[name]=sorted(bad_bones)
     else:excluded.append(name)
   def has_motion(name):
    if not name or name not in ad:return False
    clip=ad[name]
    valid_bones={bone:channels for bone,channels in clip.get('bones',{}).items() if bone in bones and not any(token in bone.lower() for token in ('eye','mouth','jaw'))}
    payload=json.dumps({'bones':valid_bones})
    return ('query.anim_time' in payload or 'q.anim_time' in payload) or any(isinstance(ch,dict) and len(ch)>1 for bone in valid_bones.values() for ch in bone.values())
   if not walk or not has_motion(walk):
    # Reuse a moving idle where available; otherwise keep the validated model
    # static and record that missing locomotion explicitly in the manifest.
    if idle and has_motion(idle):
     walk=idle;movement_fallback='idle'
    elif idle:
     walk=idle;movement_fallback='static'
    else:
     issues.append('No usable locomotion clip' if not walk else 'Locomotion clip is a static pose')
   attack=next((clips[n] for n in ('physical_attack','physical','attack') if n in clips),None)
   attack_ticks=math.ceil(ad[attack].get('animation_length',0)*20) if attack else 0
   if attack and attack_ticks<=0:issues.append('Physical attack has no finite duration')
  except Exception as exc:
   issues.append('Resource parse failed: '+str(exc))
   idle=walk=None;mode='ground';excluded=[];attack_ticks=0;clips={}
  d=dict(nationalPokedexNumber=s['nationalPokedexNumber'],slug=slug,name=s['name'],model=model,texture=texture,animation=animation,mode=mode,scale=s.get('baseScale',1),width=s['hitbox']['width'],height=s['hitbox']['height'],health=max(20,s['baseStats']['hp']/2),attack=max(3,s['baseStats']['attack']/12),speed=round(min(.35,.20+s['baseStats']['speed']/1000),3),clips=clips,idle=idle,walk=walk,movement_fallback=movement_fallback,generated_static_animation=generated_static_animation,attackTicks=attack_ticks,excluded_clips=excluded,stripped_bones=stripped_bones,sounds=res['sounds'],licenses=res['licenses'])
  accepted=not bool(issues)
  results.append(dict(slug=slug,accepted=accepted,reason='; '.join(issues) if issues else None,issues=issues,definition=d))
 return results

if __name__=='__main__':
 rows=audit();(ROOT/'tools/candidate_audit.json').write_text(json.dumps(rows,indent=2,ensure_ascii=False)+'\n')
 for row in rows:print(f"{row['definition']['nationalPokedexNumber'] if row['definition'] else CAT[row['slug']]['nationalPokedexNumber']:03} {row['slug']}: {'READY' if row['accepted'] else 'SKIP'} {row['reason'] or ''}")
