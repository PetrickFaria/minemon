#!/usr/bin/env python3
"""Import one authorized ten-species batch from the exact audited Cobblemon archive."""
import argparse,copy,hashlib,io,json,math,re,urllib.request,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources'
ap=argparse.ArgumentParser();ap.add_argument('--batch',type=int,choices=(2,3,4,5,6,7,8,9),required=True);ap.add_argument('--segment',choices=('first','second'));ap.add_argument('--source-jar',type=Path);args=ap.parse_args()
start,end=(args.batch-1)*10+1,args.batch*10
if args.batch in (8,9) and args.segment:
 start,end=(((71,75) if args.segment=='first' else (76,80)) if args.batch==8 else ((81,85) if args.segment=='first' else (86,90)))
mp=R/'data/minemon_gen1/runtime_species.json'
defs=json.loads(mp.read_text());before=copy.deepcopy(defs)
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert all('_cobblemon_1_8_2/' in d['model'] for d in defs[:min(start-1,80 if args.batch==9 else 70)]),'Previous batch must be imported and validated first'
url='https://cdn.modrinth.com/data/MdwFAVRL/versions/GJ2a1b8O/Cobblemon-fabric-1.8.2%2B1.21.1.jar'
raw=args.source_jar.read_bytes() if args.source_jar else urllib.request.urlopen(url,timeout=180).read()
digest=hashlib.sha256(raw).hexdigest()
assert digest=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2','Unexpected source archive'
z=zipfile.ZipFile(io.BytesIO(raw));members=set(z.namelist())
vp=R/'data/minemon_gen1/animation_validation_profiles.json'
profiles=json.loads(vp.read_text()) if vp.exists() else {}
GAIT={
 'zubat':["wing_left","wing_right"],
 'golbat':["wing_left","wing_right","wing_left_rotation","wing_right_rotation"],
 'oddish':["leg_left","leg_right"],
 'gloom':["leg_left","leg_right"],
 'vileplume':["leg_left","leg_right"],
 'paras':["leg_left_back","leg_left_front","leg_right_back","leg_right_front"],
 'parasect':["leg_left_back","leg_left_front","leg_right_back","leg_right_front"],
 'venonat':["left_foot","right_foot"],
 'venomoth':["left_top_wing","right_top_wing","left_bottom_wing","right_bottom_wing"],
 'diglett':["body","body2","ground"],
 'nidoqueen':['left_upper_leg','right_upper_leg','left_shoulder','right_shoulder'],
 'nidoranm':['leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'nidorino':['leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'nidoking':['leg_left','leg_right'],'clefairy':['leg_left','leg_right'],
 'clefable':['leg_left','leg_right'],
 'vulpix':['leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'ninetales':['leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'jigglypuff':['foot_left','foot_right'],'wigglytuff':['foot_left','foot_right'],
 'spearow':['leg_left','leg_right'],'fearow':['leg_left','leg_right'],
 'metapod':['body','body_segment'],'butterfree':['wing_left','wing_right'],
 'weedle':['body_segment','body_segment2','body_segment3'],'kakuna':['body'],
 'beedrill':['wing_left','wing_right'],'pidgey':['wing_open_left','wing_open_right'],
 'pidgeotto':['wing_open_left','wing_open_right'],'pidgeot':['wing_open_left','wing_open_right'],
 'rattata':['leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'raticate':['foot_left','foot_right','hand_left','hand_right'],
 'ekans':['tail','tail2','tail3','body'],'arbok':['body_upper','body_upper2','tail','tail2'],
 'pikachu':['leg_left','leg_right'],'raichu':['leg_left','leg_right'],
 'sandshrew':['leg_left','leg_right'],'sandslash':['leg_left','leg_right'],
 'nidoranf':['leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'nidorina':['left_leg','right_leg','left_upper_arm','right_upper_arm'],
 'dugtrio':['body1','body1_2','body2','body3','body3_2'],
 'meowth':['arm_left','arm_right','foot_left','foot_right','tail1','tail2','tail3'],
 'persian':['head','neck','tail1','tail2','tail3','tail4','tail5','tail6'],
 'psyduck':['foot_left','foot_right','arm_left','arm_right'],
 'golduck':['leg_left','leg_right','arm_left','arm_right'],
 'mankey':['leg_left','leg_right','arm_left','arm_right'],
 'primeape':['leg_left','leg_right','arm_left','arm_right'],
 'growlithe':['leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'arcanine':['leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'poliwag':['left_leg','right_leg','left_foot','right_foot'],
 'poliwhirl':['leg_left','leg_right','foot_left1','foot_right1'],
 'poliwrath':['leg_left','leg_right','arm_left1','arm_right1'],
 'abra':['leg_left','leg_right','arm_left','arm_right','body','tail'],
 'kadabra':['leg_left','leg_right','arm_left','arm_right','torso','tail'],
 'alakazam':['left_upper_leg','right_upper_leg','left_upper_arm','right_upper_arm','upper_torso'],
 'machop':['leg_left','leg_right','arm_left','arm_right','torso'],
 'machoke':['leg_left','leg_right','arm_left','arm_right','torso'],
 'machamp':['leg_left','leg_right','arm_left','arm_right','torso'],
 'bellsprout':['leg_left_rotation','leg_right_rotation','arm_left','arm_right','body'],
 'weepinbell':['body','body2','arm_left','arm_right','stem','stem2'],
 'victreebel':['body','head_stem','head_leaf'],'tentacool':['tentacle_left_3','tentacle_right_3'],
 'tentacruel':['tentacle_left_3','tentacle_right_3'],'geodude':['arm_left','arm_right'],
 'graveler':['arm_bottom_left','arm_bottom_right'],'golem':['left_upper_arm','right_upper_arm'],
 'ponyta':['leg_front_left','leg_front_right','leg_back_left','leg_back_right','fire_mane_head','tail'],
 'rapidash':['leg_front_left','leg_front_right','leg_back_left','leg_back_right','fire_mane_head_flame','tail'],
 'slowpoke':['leg_front_left','leg_front_right','leg_back_left','leg_back_right','tail','tail2','tail4'],
 'slowbro':['leg_left','leg_right','shell','shell_jaw','tail'],
 'magnemite':['body','nail_left_rotation','nail_right_rotation','magnet_left','magnet_right'],
 'magneton':['allbodies','body2','body3','magnet_left_rotation','magnet_right_rotation'],
 'farfetchd':['wing_left','wing_right','leak','leg_left','leg_right'],
 'doduo':['leg_left','leg_left2','leg_right','leg_right2','neck_left','neck_right'],
 'dodrio':['leg_left','leg_left2','leg_right','leg_right2','head2','head3'],
 'seel':['flipper_left','flipper_right','tail','body'],
 'dewgong':['flipper_left','flipper_right','tail','body'],
 'grimer':['body','arm_left','arm_right','back_tail','front_tail'],
 'muk':['body','arm_left','arm_right','mouth_sludge'],
 'shellder':['shell_top','shell_bottom','head','tongue']
}
IDLE={'oddish':["leaf_center3","leaf_inner_left3","leaf_inner_right3"],'gloom':["arm_left","arm_right","leaf_back_right4"],'vileplume':["arm_left","arm_right","petal_back_rotation"],'paras':["arm_left","arm_right","body"],'parasect':["claw_left","claw_right","body"],'venonat':["left_foot","right_foot","left_antenna","right_antenna"],'diglett':["body"],'nidoqueen':['left_upper_leg','right_upper_leg','left_shoulder','right_shoulder'],'nidoranm':['head','torso','ear_left','ear_right'],'nidorino':['jaw','torso'],'nidoking':['tail','tail2','arm_left1','arm_right1'],'clefairy':['tail','tail2','wing_left','wing_right'],'clefable':['arm_left','arm_right','tail','tail2'],'vulpix':['tail_left1','tail_right1','head_rotation'],'ninetales':['leg_front_left','leg_front_right','tail_middle'],'jigglypuff':['foot_left','foot_right','head'],'wigglytuff':['foot_left','foot_right','torso'],'spearow':['head'],'fearow':['head'],'raticate':['body','head','tail'],'ekans':['head','neck','tail5'],'sandslash':['arm_left','arm_right','tail'],
 'dugtrio':['body1','body2','body3','body3_2'],'meowth':['tail1','tail2','tail3'],'persian':['head','neck','tail1','tail2','tail3','tail4','tail5','tail6'],
 'psyduck':['head','tail','tail2','hair2','hair3'],'golduck':['head','neck','tail','tail2','tail3'],'mankey':['body','ear_left','ear_right','tail1','tail2'],'primeape':['body','ear_left','ear_right'],
 'growlithe':['body','head','upper_torso','tail'],'arcanine':['body','head','tail','neck'],'poliwag':['tail','tail2','tail3'],
 'poliwhirl':['body','arm_left','arm_right'],'poliwrath':['body','arm_left1','arm_right1'],'abra':['arm_left','arm_right','body','tail'],
 'kadabra':['arm_left','arm_right','head','tail'],'alakazam':['left_upper_arm','right_upper_arm','head','upper_torso'],
 'machop':['arm_left','arm_right','torso'],'machoke':['arm_left','arm_right','torso'],'machamp':['arm_left','arm_right','torso'],
 'bellsprout':['head','body','arm_left','arm_right'],'weepinbell':['body','stem','stem2','arm_left','arm_right']}
RUN={'sandshrew':['ball'],'arcanine':['leg_front_left','leg_front_right','leg_back_left','leg_back_right'],
 'abra':['leg_left','leg_right','arm_left','arm_right'],'kadabra':['leg_left','leg_right','arm_left','arm_right'],
 'alakazam':['left_upper_leg','right_upper_leg','left_upper_arm','right_upper_arm'],
 'geodude':['arm_left','arm_right'],'graveler':['arm_bottom_left','arm_bottom_right'],
 'golem':['left_upper_arm','right_upper_arm'],'rapidash':['leg_front_left','leg_front_right','leg_back_left','leg_back_right']}
IDLE.update({'poliwhirl':['body','arm_left','arm_right'],'poliwrath':['body','arm_left1','arm_right1'],
 'abra':['body','tail'],'kadabra':['tail','arm_left','arm_right'],
 'alakazam':['upper_torso','left_mustache','right_mustache'],
 'machop':['torso','arm_left','arm_right','tail'],'machoke':['torso','arm_left','arm_right'],
 'machamp':['torso','arm_left','arm_right','arm_back_left','arm_back_right'],
 'bellsprout':['body','arm_left','arm_right'],'weepinbell':['body','stem','stem2','arm_left','arm_right'],
 'victreebel':['head_stem','head_leaf','head_leaf_left'],'tentacool':['tentacle_left_3','tentacle_right_3'],
 'tentacruel':['tentacle_left_3','tentacle_right_3'],'geodude':['arm_left','arm_right'],
 'graveler':['arm_bottom_left','arm_bottom_right'],'golem':['left_upper_arm','right_upper_arm'],
 'ponyta':['fire_mane','fire_mane_head','tail'],'rapidash':['fire_mane_head_flame','tail'],
 'slowpoke':['tail','tail2','tail4'],'slowbro':['shell','shell_jaw','tail'],
 'magnemite':['body','magnet_left','magnet_right','nail_head'],
 'magneton':['allbodies','body2','body3','magnet_left','magnet_right'],
 'farfetchd':['head','neck','tail','leak','wing_left','wing_right'],
 'doduo':['body','head','head2','neck_left','neck_right'],
 'dodrio':['body','head2','head3','neck_left','neck_right'],
 'seel':['body','head','flipper_left','flipper_right','tail'],
 'dewgong':['body','head','flipper_left','flipper_right','tail'],
 'grimer':['body','head','arm_left','arm_right','back_tail','front_tail'],
 'muk':['body','arm_left','arm_right','mouth_sludge'],
 'shellder':['body','shell_top','shell_bottom','tongue']})
RUN.update({'grimer':['body','arm_left','arm_right','back_tail','front_tail']})
evidence=[]
for d in defs[start-1:end]:
 slug=d['slug'];n=d['nationalPokedexNumber'];folder=f'{n:04d}_{slug}'
 prefix=f'assets/cobblemon/bedrock/pokemon'
 candidates=[f'{prefix}/models/{folder}/{slug}{suffix}.geo.json' for suffix in ('','_male','_female')]
 model=next((p for p in candidates if p in members),None)
 assert model,'No audited base model: '+slug
 variant=Path(model).name.removesuffix('.geo.json')
 anim_candidates=[f'{prefix}/animations/{folder}/{variant}.animation.json',f'{prefix}/animations/{folder}/{slug}.animation.json']
 anim=next((p for p in anim_candidates if p in members),None)
 assert anim,'No animation for selected model variant: '+slug+' '+variant
 tex=f'assets/cobblemon/textures/pokemon/{folder}/{slug}.png'
 license=f'{prefix}/models/{folder}/license'
 suffix=slug+'_cobblemon_1_8_2'
 paths={'model':f'assets/minemon/geo/{suffix}/{slug}.geo.json',
        'animation':f'assets/minemon/animations/{suffix}/{slug}.animation.json',
        'texture':f'assets/minemon/textures/pokemon/{suffix}/{slug}.png'}
 for key,source in [('model',model),('animation',anim),('texture',tex)]:
  target=R/paths[key];target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(source))
 lp=f'assets/minemon/licenses/pokemon/{slug}/cobblemon-1.8.2-model-license.txt'
 target=R/lp;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(license))
 model_data=json.loads(z.read(model));geo=model_data['minecraft:geometry'][0]
 if args.batch==5 and slug=='golbat':
  by_name={b['name']:b for b in geo['bones']}
  for side in ('left','right'):
   wing=by_name['wing_'+side];wrapper='wing_'+side+'_rotation'
   assert wrapper not in by_name and wing['parent']=='wings'
   geo['bones'].append({'name':wrapper,'parent':wing['parent'],'pivot':list(wing['pivot'])})
   wing['parent']=wrapper
  (R/paths['model']).write_text(json.dumps(model_data,indent=2)+'\n')
  d['source_rig_adaptation']='Restore two missing native animation group nodes wing_left_rotation/right_rotation at existing wing pivots under wings; original bone pivots/cubes unchanged'
 bones={b['name'] for b in geo['bones']}
 animations=json.loads(z.read(anim))['animations']
 source_animation_prefix=f'animation.{variant}.'
 runtime_animation_prefix=f'animation.{slug}.'
 if variant!=slug:
  animations={runtime_animation_prefix+name[len(source_animation_prefix):] if name.startswith(source_animation_prefix) else name:clip for name,clip in animations.items()}
 print('SOURCE_EVENTS',slug,json.dumps({name:{k:v for k,v in a.items() if k in ('sound_effects','particle_effects')} for name,a in animations.items() if any(k in a for k in ('sound_effects','particle_effects'))}),flush=True)
 if args.batch==4 and slug=='jigglypuff':
  cry=animations['animation.jigglypuff.cry']
  misplaced=cry['sound_effects'].pop('animation_length')
  assert misplaced==1.25
  assert 'animation_length' not in cry or cry['animation_length']==misplaced
  cry['animation_length']=misplaced
  d['source_event_correction']='cry duration 1.25 moved from sound_effects metadata to clip animation_length; timestamp/effect unchanged'
 if args.batch in (5,6,7,8,9):
  # Retain every native clip. Only normalize syntax / unsupported mounted inputs.
  def adapt(value):
   if isinstance(value,str):
    value=value.replace('Math.','math.')
    if args.batch in (7,8,9):
     value=re.sub(r'\bq\.', 'query.',value)
     # GeckoLib 3.1.40's Molang parser rejects a unary minus directly before
     # a grouped expression; multiplying the complete group by -1 is equal.
     if value.startswith('-('):value='-1*'+value[1:]
    if args.batch==5 and slug in ('paras','parasect') and value.startswith('-(math.sin(') and value.endswith(')*0.25'):
     value='-1*'+value[1:]
    if args.batch==5 and slug=='parasect':
     value=value.replace('q.r.yaw_change(0)','0').replace('q.r.velocity_y(0)','0')
    if args.batch==6 and slug=='arcanine':
     for index in (0,2,4,6): value=value.replace(f'q.r.yaw_change({index})','0')
     for index in (0,1,2,4,6): value=value.replace(f'q.r.velocity_y({index})','0')
    return value
   if isinstance(value,list):return [adapt(x) for x in value]
   if isinstance(value,dict):return {k:adapt(v) for k,v in value.items()}
   return value
  animations=adapt(animations)
  if args.batch==5 and slug=='parasect':
   d['source_molang_adaptations']={'Math.':'math.','q.r.yaw_change(0)':'0: neutral non-ridden input; mounted clips not selected by Minemon','q.r.velocity_y(0)':'0: neutral non-ridden input; mounted clips not selected by Minemon'}
  if args.batch==6 and slug=='arcanine':
   d['source_molang_adaptations']={'q.r.yaw_change(n)':'0 for retained ride clips; Minemon has no ride input','q.r.velocity_y(n)':'0 for retained ride clips; Minemon has no ride input'}
  if args.batch==7:
   d['source_molang_adaptations']={'q.':'query.'}
  if args.batch==8:
   d['source_molang_adaptations']={'q.':'query.','Math.':'math.'}
  if args.batch==9:
   d['source_molang_adaptations']={'q.':'query.','Math.':'math.'}
  if slug=='zubat':
   poser=json.loads(z.read(f'{prefix}/posers/{folder}/{slug}.json'))
   assert {'part':'wings_folded','isVisible':False} in poser['poses']['hover']['transformedParts']
   for name,clip in animations.items():
    folded=name.endswith(('.shoulder_left','.shoulder_right'))
    clip.setdefault('bones',{}).setdefault('wings_folded',{})['scale']=[1,1,1] if folded else [0,0,0]
    clip['bones'].setdefault('wings_open',{})['scale']=[0,0,0] if folded else [1,1,1]
   d['source_pose_visibility']='poser visibility adapted as parent scale: folded wings hidden except shoulder clips; open wings visible'
  d['source_event_evidence']={name:{k:v for k,v in clip.items() if k in ('sound_effects','particle_effects','timeline')} for name,clip in animations.items() if any(k in clip for k in ('sound_effects','particle_effects','timeline'))}
  (R/paths['animation']).write_text(json.dumps({'format_version':'1.8.0','animations':animations},indent=2)+'\n')
 names={name.rsplit('.',1)[-1]:name for name in animations if name.startswith(f'animation.{slug}.')}
 d.update(paths)
 existing_licenses=[p for p in d.get('licenses',[]) if (R/p).is_file()]
 d['licenses']=list(dict.fromkeys(existing_licenses+[lp]))
 d['clips']=dict(names);d['excluded_clips']=[]
 d['stripped_bones']={name:sorted(set(a.get('bones',{}))-bones) for name,a in animations.items() if set(a.get('bones',{}))-bones}
 d['geckolib3_bedrock_keyframes']=True;d['strip_bedrock_locators']=True
 d['source_model_variant']=Path(model).name
 d['source_cobblemon_version']='1.8.2'
 d.pop('generated_static_animation',None);d.pop('movement_fallback',None)
 flying=d['mode']=='flying'
 if slug in ('spearow','fearow'):
  # These sources use procedural poser wing motion; no native gait is invented.
  # Preserve the existing ground mode until the source-backed adapter is audited.
  flying=False
 idle=names.get('air_idle') if flying else (names.get('ground_idle') or names.get('idle') or names.get('ground_idle_placeholder'))
 walk=names.get('air_fly') if flying else (names.get('ground_walk') or names.get('ground_walk_placeholder'))
 assert idle,'Missing source idle '+slug
 if slug in ('spearow','fearow'):
  poser_path=f'{prefix}/posers/{folder}/{slug}.json'
  poser=json.loads(z.read(poser_path))
  source_animations=poser['poses']['walking']['animations']
  assert "q.biped_walk(1, 1, 'leg_left', 'leg_right')" in source_animations
  assert "q.look('head')" in source_animations
  # GeckoLib 3 has no Cobblemon procedural poser runtime. Retain the source
  # stance, overlay an opposed biped cycle, and implement q.look in the model.
  walk=f'animation.{slug}.minemon_poser_walk'
  adapted=copy.deepcopy(animations[idle]);adapted['loop']=True;adapted['animation_length']=1.0
  for side,sign in [('left',1),('right',-1)]:
   channel=adapted.setdefault('bones',{}).setdefault('leg_'+side,{})
   rotation=channel.get('rotation',[0,0,0]);assert isinstance(rotation,list) and len(rotation)==3
   rotation[0]=f'{rotation[0]}{"+30" if sign>0 else "-30"}*math.sin(q.anim_time*720)'
   channel['rotation']=rotation
  for absent in set(adapted['bones'])-bones:
   assert absent in d['stripped_bones'].get(idle,[]),'Unexpected missing procedural bone: '+absent
   adapted['bones'].pop(absent)
  animations[walk]=adapted;d['clips']['ground_walk']=walk
  (R/paths['animation']).write_text(json.dumps({'format_version':'1.8.0','animations':animations},indent=2)+'\n')
  d['source_procedural_pose']={'poser':poser_path,'walk':source_animations,'head_look':'q.look','adaptation':'native ground stance plus opposed 30-degree leg cycle at 720 degrees per second; head look handled by MinemonModel'}
 if args.batch==4 and slug in ('nidoqueen','nidorino','ninetales','jigglypuff','wigglytuff'):
  # Cobblemon procedural posers are not executable in GeckoLib 3.
  # Keep every native stance channel, overlay only the declared walking joints.
  settings={
   'nidoqueen':(0.6662,1.4,{'left_upper_leg':-1,'right_upper_leg':1,'left_shoulder':1,'right_shoulder':-1},'NidoqueenModel: BipedWalkAnimation defaults plus BimanualSwingAnimation defaults'),
   'nidorino':(0.7,0.7,{'leg_front_left':1,'leg_front_right':-1,'leg_back_left':-1,'leg_back_right':1},'NidorinoModel: QuadrupedWalkAnimation(0.7,0.7)'),
   'ninetales':(0.66,1.4,{'leg_front_left':1,'leg_front_right':-1,'leg_back_left':-1,'leg_back_right':1},'q.quadruped_walk(0.66, 1.4)'),
   'jigglypuff':(0.66,0.6,{'foot_left':-1,'foot_right':1},'q.biped_walk(0.66, 0.6)'),
   'wigglytuff':(0.66,0.6,{'foot_left':-1,'foot_right':1},'q.biped_walk(0.66, 0.6)')
  }
  period,amplitude,signs,origin=settings[slug]
  if slug not in ('nidoqueen','nidorino'):
   poser_path=f'{prefix}/posers/{folder}/{slug}.json'
   poser=json.loads(z.read(poser_path))
   assert any(origin[:-1] in a for a in poser['poses']['walking']['animations'])
  walk=f'animation.{slug}.minemon_poser_walk'
  adapted=copy.deepcopy(animations[idle]);adapted['loop']=True;adapted['animation_length']=4.0
  frequency=720*period/0.6662
  for joint,sign in signs.items():
   assert joint in bones,(slug,joint,sorted(bones))
   channel=adapted.setdefault('bones',{}).setdefault(joint,{})
   rotation=channel.get('rotation',[0,0,0]);assert isinstance(rotation,list) and len(rotation)==3
   rotation[0]=f'({rotation[0]})+({sign*30*amplitude:.6f})*math.cos(q.anim_time*{frequency:.6f})'
   channel['rotation']=rotation
  for absent in set(adapted['bones'])-bones:
   assert absent in d['stripped_bones'].get(idle,[])
   adapted['bones'].pop(absent)
  animations[walk]=adapted;d['clips']['ground_walk']=walk
  (R/paths['animation']).write_text(json.dumps({'format_version':'1.8.0','animations':animations},indent=2)+'\n')
  d['source_procedural_pose']={'reference':origin,'adaptation':'native stance plus opposed X rotation; diagonal pairs for quadrupeds; 30 degrees times source amplitude, 720 degrees/second times source period/0.6662; no native limb-swing clock in GeckoLib 3','joints':signs}
 if args.batch==6 and slug=='growlithe':
  poser_path=f'{prefix}/posers/{folder}/{slug}.json'
  poser=json.loads(z.read(poser_path))
  source_animations=poser['poses']['walking']['animations']
  expected="q.quadruped_walk(0.66, 1.4, 'leg_front_left', 'leg_front_right', 'leg_back_left', 'leg_back_right')"
  assert expected in source_animations
  walk=f'animation.{slug}.minemon_poser_walk'
  adapted=copy.deepcopy(animations[idle]);adapted['loop']=True;adapted['animation_length']=4.0
  signs={'leg_front_left':1,'leg_front_right':-1,'leg_back_left':-1,'leg_back_right':1}
  frequency=720*0.66/0.6662
  for joint,sign in signs.items():
   assert joint in bones,(slug,joint,sorted(bones))
   channel=adapted.setdefault('bones',{}).setdefault(joint,{})
   rotation=channel.get('rotation',[0,0,0]);assert isinstance(rotation,list) and len(rotation)==3
   rotation[0]=f'({rotation[0]})+({sign*30*1.4:.6f})*math.cos(q.anim_time*{frequency:.6f})'
   channel['rotation']=rotation
  for absent in set(adapted['bones'])-bones:
   assert absent in d['stripped_bones'].get(idle,[])
   adapted['bones'].pop(absent)
  animations[walk]=adapted;d['clips']['ground_walk']=walk
  (R/paths['animation']).write_text(json.dumps({'format_version':'1.8.0','animations':animations},indent=2)+'\n')
  d['source_procedural_pose']={'poser':poser_path,'walk':source_animations,'adaptation':'native standing channels plus diagonal quadruped X rotations; 42-degree amplitude and 713.299 degrees/second derived from q.quadruped_walk(0.66, 1.4); q.look is not synthesized','joints':signs}
 if walk is None:
  walk=idle;d['movement_fallback']='idle'
 d['idle']=idle;d['walk']=walk
 if flying:d['clips'].pop('ground_run',None)
 for key in ('sit','sleep','physical_attack'):
  native=names.get(key) or (names.get('air_sleep') if key=='sleep' and flying else None) or (names.get('physical') or names.get('attack') if key=='physical_attack' else None)
  d['clips'][key]=native or f'animation.{slug}.minemon_{key}'
 attack=d['clips']['physical_attack']
 d['attackTicks']=max(1,math.ceil(animations[attack].get('animation_length',4.0)*20)) if attack in animations else 12
 gait=GAIT.get(slug)
 assert gait,'Species needs an explicit source-backed gait profile: '+slug
 prof={'idle':IDLE.get(slug,gait),'walk':gait,'run':RUN.get(slug,gait)}
 if args.batch==9 and slug in ('seel','muk'):
  # Source inspection: these native idle clips contain only static vectors.
  # Keep their pose and make the client smoke assert stability, not motion.
  prof['idle_static']=prof['idle']
  d['source_static_idle']=True
 if args.batch==6 and slug in ('meowth','persian','growlithe','poliwag'):
  native_idle=animations[idle]
  assert not any(token in json.dumps(native_idle) for token in ('math.','q.','query.'))
  assert all(not isinstance(value,dict) for ch in native_idle['bones'].values() for value in ch.values())
  prof['idle_static']=prof['idle']
  d['source_static_idle']=True
 if args.batch==4 and slug in ('nidoqueen','ninetales','jigglypuff','wigglytuff'):
  native_idle=animations[idle]
  assert not any(token in json.dumps(native_idle) for token in ('math.','q.','query.'))
  assert all(not isinstance(value,dict) for ch in native_idle['bones'].values() for value in ch.values())
  prof['idle_static']=prof['idle']
  d['source_static_idle']=True
 if args.batch==7 and slug in ('poliwhirl','poliwrath','abra','kadabra','alakazam','machop','machoke','machamp','bellsprout','weepinbell'):
  # Time expressions can be nested inside rotation/position vector arrays.
  dynamic=bool(re.search(r'\b(?:q|query)\.(?:anim_time|life_time)\b',json.dumps(animations[idle].get('bones',{}))))
  dynamic=dynamic or any(isinstance(value,dict) and len(value)>1 for channel in animations[idle].get('bones',{}).values() for value in channel.values())
  if not dynamic:
   prof['idle_static']=prof['idle'];d['source_static_idle']=True
  poser_path=f'{prefix}/posers/{folder}/{variant}.json'
  if poser_path in members:
   poser=json.loads(z.read(poser_path))
   look_inputs=sorted({a for pose in poser.get('poses',{}).values() for a in pose.get('animations',[]) if isinstance(a,str) and a.startswith("q.look(")})
   if look_inputs:
    look_bones=sorted({re.search(r"q.look\('([^']+)'",a).group(1) for a in look_inputs})
    available={name:(name if name in bones else 'head' if name=='head_ai' and 'head' in bones else None) for name in look_bones}
    d['source_procedural_pose']={'poser':poser_path,'look':look_inputs,'look_bones':look_bones,'adapted_geo_bones':available,'adaptation':'Map q.look to the source head bone when present; no locomotion gait is synthesized'}
 if args.batch in (8,9):
  poser_path=f'{prefix}/posers/{folder}/{variant}.json'
  if poser_path in members:
   poser=json.loads(z.read(poser_path))
   look_inputs=sorted({a for pose in poser.get('poses',{}).values() for a in pose.get('animations',[]) if isinstance(a,str) and a.startswith('q.look(')})
   if look_inputs:
    look_bones=sorted({re.search(r"q.look\('([^']+)'",a).group(1) for a in look_inputs})
    available={name:(name if name in bones else 'head' if name=='head_ai' and 'head' in bones else None) for name in look_bones}
    d['source_procedural_pose']={'poser':poser_path,'look':look_inputs,'look_bones':look_bones,'adapted_geo_bones':available,'adaptation':'Map q.look to a real compatible source head bone in MinemonModel; no locomotion gait is synthesized'}
 for state,joints in prof.items():
  assert set(joints)<=bones,(slug,state,set(joints)-bones)
 profiles[slug]=prof
 evidence.append({'number':n,'slug':slug,'model':model,'idle':idle,'movement':walk,'mode':d['mode'],'native_run':d['clips'].get('ground_run'),'fallback':d.get('movement_fallback'),'profiles':prof,'stripped_bones':d['stripped_bones']})
assert all(a==b for a,b in zip(before,defs) if not start<=a['nationalPokedexNumber']<=end),'Out-of-scope manifest mutation'
mp.write_text(json.dumps(defs,ensure_ascii=False,indent=2)+'\n')
vp.parent.mkdir(parents=True,exist_ok=True);vp.write_text(json.dumps(profiles,indent=2)+'\n')
report={'batch':args.batch,'range':[start,end],'source_url':url,'source_sha256':digest,'manual_visual_status':'pending; user will evaluate the authorized batches inside Minecraft','species':evidence}
out=ROOT/f'docs/animation-audit/minemon-gen1-batch{args.batch}-source-evidence.json'
if args.batch in (8,9) and args.segment=='second' and out.exists():
 old=json.loads(out.read_text())
 merged={item['number']:item for item in old['species']}
 merged.update({item['number']:item for item in evidence})
 report['species']=[merged[number] for number in sorted(merged)]
 report['range']=[71,80] if args.batch==8 else [81,90]
out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False,indent=2))
print(f'PASS PRESERVATION: {151-(end-start+1)} out-of-batch manifest entries unchanged; source originals retained')
