#!/usr/bin/env python3
"""Import one authorized ten-species batch from the exact audited Cobblemon archive."""
import argparse,copy,hashlib,io,json,math,urllib.request,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources'
ap=argparse.ArgumentParser();ap.add_argument('--batch',type=int,choices=(2,3),required=True);args=ap.parse_args()
start,end=(args.batch-1)*10+1,args.batch*10
mp=R/'data/minemon_gen1/runtime_species.json'
defs=json.loads(mp.read_text());before=copy.deepcopy(defs)
assert len(defs)==151 and [d['nationalPokedexNumber'] for d in defs]==list(range(1,152))
assert all('_cobblemon_1_8_2/' in d['model'] for d in defs[:start-1]),'Previous batch must be imported and validated first'
url='https://cdn.modrinth.com/data/MdwFAVRL/versions/GJ2a1b8O/Cobblemon-fabric-1.8.2%2B1.21.1.jar'
raw=urllib.request.urlopen(url,timeout=120).read()
digest=hashlib.sha256(raw).hexdigest()
assert digest=='cae8ac328b61354fbe415208b8c6ffc12e7155094d0a87082e85e7914fb380c2','Unexpected source archive'
z=zipfile.ZipFile(io.BytesIO(raw));members=set(z.namelist())
vp=R/'data/minemon_gen1/animation_validation_profiles.json'
profiles=json.loads(vp.read_text()) if vp.exists() else {}
GAIT={
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
 'nidorina':['left_leg','right_leg','left_upper_arm','right_upper_arm']
}
IDLE={'spearow':['head'],'fearow':['head'],'raticate':['body','head','tail'],'ekans':['head','neck','tail5'],'sandslash':['arm_left','arm_right','tail']}
RUN={'sandshrew':['ball']}
evidence=[]
for d in defs[start-1:end]:
 slug=d['slug'];n=d['nationalPokedexNumber'];folder=f'{n:04d}_{slug}'
 prefix=f'assets/cobblemon/bedrock/pokemon'
 candidates=[f'{prefix}/models/{folder}/{slug}{suffix}.geo.json' for suffix in ('','_male','_female')]
 model=next((p for p in candidates if p in members),None)
 assert model,'No audited base model: '+slug
 anim=f'{prefix}/animations/{folder}/{slug}.animation.json'
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
 geo=json.loads(z.read(model))['minecraft:geometry'][0];bones={b['name'] for b in geo['bones']}
 animations=json.loads(z.read(anim))['animations']
 names={name.rsplit('.',1)[-1]:name for name in animations if name.startswith(f'animation.{slug}.')}
 d.update(paths);d['licenses']=list(dict.fromkeys(d.get('licenses',[])+[lp]))
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
 idle=names.get('air_idle') if flying else names.get('ground_idle')
 walk=names.get('air_fly') if flying else names.get('ground_walk')
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
 for state,joints in prof.items():
  assert set(joints)<=bones,(slug,state,set(joints)-bones)
 profiles[slug]=prof
 evidence.append({'number':n,'slug':slug,'model':model,'idle':idle,'movement':walk,'mode':d['mode'],'native_run':d['clips'].get('ground_run'),'fallback':d.get('movement_fallback'),'profiles':prof,'stripped_bones':d['stripped_bones']})
assert all(a==b for a,b in zip(before,defs) if not start<=a['nationalPokedexNumber']<=end),'Out-of-scope manifest mutation'
mp.write_text(json.dumps(defs,ensure_ascii=False,indent=2)+'\n')
vp.parent.mkdir(parents=True,exist_ok=True);vp.write_text(json.dumps(profiles,indent=2)+'\n')
report={'batch':args.batch,'range':[start,end],'source_url':url,'source_sha256':digest,'manual_visual_status':'pending; user authorized validation after batch 3','species':evidence}
out=ROOT/f'docs/animation-audit/minemon-gen1-batch{args.batch}-source-evidence.json'
out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False,indent=2))
print(f'PASS PRESERVATION: {151-(end-start+1)} out-of-batch manifest entries unchanged; source originals retained')
