import json,struct,sys,zipfile,re,math,hashlib,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources'
SOURCE_PREFIX='assets/minemon/';RUNTIME_PREFIX='assets/minemon_gen1/'
SOURCE_FIRE_HASHES={
 'ponyta':['8b1dce1b056345e5c90f7093a7cf84f4c0ddea6394d7e1042ca70bb585c2082c','ff440e9e72be7059acf4527238d5e15ea639ccdfa4aeef4229c882951fae3b09','39a698ac6c8d5a03957b85856d535f14fdef12c6133b746e5b38acb3cce5f50c','f76f69320a221c5fd7afa7b1e2e0aa4dc82e1d8c2696bdd0316014bbaee812df'],
 'rapidash':['0870dbda4cf51dd281b3295befbbbe5dcd580fba5a8720fbeba23f124189aa15','ecd0225ae7cec1a10f1c132bb4c1c03946b5933ded380f3a8e576aafb400b78e','1586693cf1eae6be8dbfd6b3b392fc87739f0973c8bee10c58ec09f6ef824455','4da49cbad5fc94c1d61aa1fe0aa01079524964794cd477bf8822b722e160f008']}
fire_evidence=json.loads((ROOT/'docs/animation-audit/minemon-gen1-batch16-visual-source-evidence.json').read_text())['fire_overlays']
renderer_source=(ROOT/'src/main/java/br/com/petrickfaria/minemon/client/MinemonRenderer.java').read_text()
for slug in SOURCE_FIRE_HASHES:
 layer=fire_evidence[slug]
 if (layer.get('fps'),layer.get('loop'),layer.get('emissive'))!=(10,True,True):raise ValueError('Source resolver fire-layer settings changed: '+slug)
if 'floor((entity.tickCount+partialTick)*0.5F)&3' not in renderer_source or 'renderSelectedBones(model,selected,texture' not in renderer_source or 'textures/emissive/' not in renderer_source:
 raise ValueError('Fire renderer no longer uses 10 fps animated selective emissive masks')
def runtime_path(path):return path.replace(SOURCE_PREFIX,RUNTIME_PREFIX,1) if path.startswith(SOURCE_PREFIX) else path
GASTLY_PARTICLE='assets/minemon_gen1/particles/gastly_ghostly_smoke.json'
GASTLY_PARTICLE_TEXTURES=[f'assets/minemon_gen1/textures/particle/gastly_ghostly_smoke_{i}.png' for i in range(6)]
def decode_png_rgba(blob):
 if blob[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Invalid PNG signature')
 pos=8;items={};chunks=[]
 while pos<len(blob):
  length=struct.unpack('>I',blob[pos:pos+4])[0];kind=blob[pos+4:pos+8];data=blob[pos+8:pos+8+length];pos+=12+length
  chunks.append((kind,data));items[kind]=data
  if kind==b'IEND':break
 w,h,depth,color,_,_,interlace=struct.unpack('>IIBBBBB',items[b'IHDR'])
 if depth not in (1,2,4,8) or color not in (2,3,6) or interlace or (color!=3 and depth!=8):
  raise ValueError('Unsupported source PNG format')
 channels={2:3,3:1,6:4}[color];filter_bpp=max(1,(channels*depth+7)//8);stride=(w*channels*depth+7)//8
 raw=zlib.decompress(b''.join(data for kind,data in chunks if kind==b'IDAT'));prior=bytearray(stride);rows=[];offset=0
 for _ in range(h):
  filt=raw[offset];offset+=1;scan=bytearray(raw[offset:offset+stride]);offset+=stride
  for i in range(stride):
   a=scan[i-filter_bpp] if i>=filter_bpp else 0;b=prior[i];c=prior[i-filter_bpp] if i>=filter_bpp else 0
   if filt==1:scan[i]=(scan[i]+a)&255
   elif filt==2:scan[i]=(scan[i]+b)&255
   elif filt==3:scan[i]=(scan[i]+((a+b)//2))&255
   elif filt==4:
    p=a+b-c;pa=abs(p-a);pb=abs(p-b);pc=abs(p-c);scan[i]=(scan[i]+(a if pa<=pb and pa<=pc else b if pb<=pc else c))&255
   elif filt!=0:raise ValueError('Unsupported PNG filter '+str(filt))
  rows.append(scan);prior=scan
 palette=items.get(b'PLTE',b'');alpha=items.get(b'tRNS',b'');pixels=[]
 for row in rows:
  out=bytearray()
  if color==6:out.extend(row)
  elif color==2:
   for i in range(0,len(row),3):out.extend((row[i],row[i+1],row[i+2],255))
  else:
   if depth==8:indices=row[:w]
   else:
    mask=(1<<depth)-1;indices=[]
    for byte in row:
     for shift in range(8-depth,-1,-depth):
      if len(indices)<w:indices.append((byte>>shift)&mask)
   for index in indices:
    j=index*3
    if j+2>=len(palette):raise ValueError('PNG palette index outside palette')
    out.extend((palette[j],palette[j+1],palette[j+2],alpha[index] if index<len(alpha) else 255))
  pixels.append(out)
 return w,h,pixels
def validate_gastly_smoke(read,definitions):
 gastly=next((d for d in definitions if d['slug']=='gastly'),None)
 if gastly is None or gastly.get('source_cobblemon_version')!='1.8.2':raise ValueError('Gastly Cobblemon 1.8.2 runtime definition missing')
 for state in ('animation.gastly.ground_idle','animation.gastly.ground_walk'):
  events=gastly.get('source_event_evidence',{}).get(state,{}).get('particle_effects',{}).get('0.0',[])
  if len([e for e in events if e.get('effect')=='ghostly_smoke' and e.get('locator')=='gas'])!=2:
   raise ValueError('Gastly native ghostly_smoke event evidence changed: '+state)
 particle=json.loads(read(GASTLY_PARTICLE))
 expected=[f'minemon_gen1:gastly_ghostly_smoke_{i}' for i in range(6)]
 if particle.get('textures')!=expected:raise ValueError('Gastly smoke particle does not use the six original Cobblemon smoke frames')
 for path in GASTLY_PARTICLE_TEXTURES:
  blob=read(path)
  if blob[:8]!=b'\x89PNG\r\n\x1a\n' or struct.unpack('>II',blob[16:24])!=(16,16):
   raise ValueError('Gastly smoke frame is invalid: '+path)
  _,_,pixels=decode_png_rgba(blob)
  if not any(row[i]>0 for row in pixels for i in range(3,len(row),4)):
   raise ValueError('Gastly smoke frame has no visible alpha pixels: '+path)
 print('PASS Gastly smoke resources: native six-frame Cobblemon sheet and idle/walk locator evidence')
def validate_target_scales(definitions):
 expected={'gastly':(0.85,0.2,0.2),'electabuzz':(1.35,1.05,2.25)}
 by_slug={d['slug']:d for d in definitions}
 for slug,values in expected.items():
  d=by_slug.get(slug)
  if d is None:raise ValueError('Missing scale target '+slug)
  actual=tuple(float(d.get(key,float('nan'))) for key in ('scale','width','height'))
  if any(not math.isclose(a,e,rel_tol=0.0,abs_tol=1e-6) for a,e in zip(actual,values)):
   raise ValueError('Unexpected scale/collision dimensions for '+slug+': '+str(actual))
 particle=(ROOT/'src/main/java/br/com/petrickfaria/minemon/client/GastlySmokeParticle.java').read_text()
 if 'this.quadSize = 0.34F * SpeciesDefinitions.get("gastly").scale() * sample;' not in particle:
  raise ValueError('Gastly smoke size no longer follows its model scale')
 renderer=(ROOT/'src/main/java/br/com/petrickfaria/minemon/client/MinemonRenderer.java').read_text()
 if 'case "gastly" -> 0.5F;' not in renderer or 'case "electabuzz" -> 1.5F;' not in renderer:
  raise ValueError('Target shadow scale no longer follows the requested visual size')
 print('PASS target scales: Gastly 0.5x with proportional smoke/hitbox; Electabuzz 1.5x with proportional hitbox')

def validate_prepared_tail_flames(read,definitions):
 rendered_hashes=[]
 for d in definitions:
  if d['slug'] not in ('charmander','charmeleon','charizard'):continue
  slug=d['slug'];source_model=json.loads((R/d['model']).read_text())['minecraft:geometry'][0]
  source_bone=next(b for b in source_model['bones'] if b['name']==d['flame_anchor']['host'])
  source_sheet=decode_png_rgba((R/d['flame_texture']).read_bytes());target_path=runtime_path(d['texture'])
  base_blob=read(target_path);target_texture=decode_png_rgba(base_blob);target_geo=json.loads(read(runtime_path(d['model'])))['minecraft:geometry'][0]
  if hashlib.sha256(base_blob).digest()!=hashlib.sha256((R/d['texture']).read_bytes()).digest():
   raise ValueError('Base texture changed while preparing tail flames: '+slug)
  if (target_texture[0],target_texture[1])!=(target_geo['description']['texture_width'],target_geo['description']['texture_height']):
   raise ValueError('Tail-flame preparation changed original texture dimensions: '+slug)
  mask_path=f'assets/minemon_gen1/textures/emissive/{slug}_tail_flame.png';mask=decode_png_rgba(read(mask_path))
  flame=next((b for b in target_geo['bones'] if b['name']=='tail_flame'),None)
  if flame is None or len(source_bone.get('cubes',[]))!=2 or len(flame.get('cubes',[]))!=2:
   raise ValueError('Tail-flame runtime does not preserve both native planes: '+slug)
  source_digest=hashlib.sha256();allowed=set()
  for original,converted in zip(source_bone['cubes'],flame['cubes']):
   _,_,source_rows=source_sheet
   z=int(original['size'][2]);height=int(original['size'][1]);sx,sy=map(int,original['uv']);sy+=z
   expected=[bytes(row[sx*4:(sx+z)*4]) for row in source_rows[sy:sy+height]]
   source_digest.update(b''.join(expected))
   u,v=map(int,converted['uv'])
   if u<0 or v<0 or u+1+2*z>mask[0] or v+z+height>mask[1]:
    raise ValueError('Tail-flame mask UV lies outside the unchanged model atlas: '+slug)
   for side_u in (u,u+1+z):
    actual=[bytes(row[side_u*4:(side_u+z)*4]) for row in mask[2][v+z:v+z+height]]
    if actual!=expected:raise ValueError('Tail-flame plane texture differs from its native Cobblemon UV pixels: '+slug)
    for yy in range(v+z,v+z+height):allowed.update((side_u+xx,yy) for xx in range(z))
   if converted.get('rotation')!=original.get('rotation') or converted.get('pivot')!=original.get('pivot'):
    raise ValueError('Tail-flame plane pivot/rotation changed from Cobblemon source: '+slug)
  if any(mask[2][y][x*4+3] and (x,y) not in allowed for y in range(mask[1]) for x in range(mask[0])):
   raise ValueError('Tail-flame mask has visible pixels outside the two flame planes: '+slug)
  rendered_hashes.append((slug,source_digest.hexdigest()))
 if len({digest for _,digest in rendered_hashes})!=3:
  raise ValueError('Charmander, Charmeleon and Charizard tail flames unexpectedly share identical source pixels')
 print('PASS tail flames: two independent Cobblemon UV planes, transforms and per-species source pixels preserved')
def validate_emissive_masks(read,definitions):
 for d in definitions:
  if d['slug'] not in ('charmander','charmeleon','charizard'):continue
  path=f"assets/minemon_gen1/textures/emissive/{d['slug']}_tail_flame.png"
  w,h,pixels=decode_png_rgba(read(path))
  source_geo=json.loads((R/d['model']).read_text())['minecraft:geometry'][0]['description']
  if (w,h)!=(source_geo['texture_width'],source_geo['texture_height']):raise ValueError('Starter flame mask dimensions differ from the unchanged model atlas: '+d['slug'])
  if not any(row[i] for row in pixels for i in range(3,len(row),4)):
   raise ValueError('Starter flame mask is fully transparent: '+d['slug'])
 moltres=decode_png_rgba(read('assets/minemon_gen1/textures/emissive/moltres_fire.png'))
 moltres_definition=next(d for d in definitions if d['slug']=='moltres')
 source_geo=json.loads((R/moltres_definition['model']).read_text())['minecraft:geometry'][0]['description']
 if (moltres[0],moltres[1])!=(source_geo['texture_width'],source_geo['texture_height']):raise ValueError('Moltres fire mask dimensions differ from its original atlas')
 for row in moltres[2]:
  for i in range(0,len(row),4):
   r,g,b,a=row[i:i+4]
   if a and not (r>=180 and g>=55 and g<=r*0.82 and b<=105 and r-b>=85):raise ValueError('Moltres emissive mask includes non-fire colors')
 print('PASS selective emissive masks: starter flame atlases are isolated; Moltres mask contains fire-palette pixels only')
def validate(read,definitions,prepared=False,packed=False):
 previous=0
 for d in definitions:
  number=d['nationalPokedexNumber']
  if number<=previous:raise ValueError('Runtime roster is not in National Pokédex order')
  previous=number
  if (number<=10 or d.get('source_cobblemon_version')=='1.8.2') and not all('_cobblemon_1_8_2/' in d[key] for key in ('model','texture','animation')):
   raise ValueError('Batch 1 species is not using all three Cobblemon 1.8.2 assets: '+d['slug'])
  model_path=runtime_path(d['model']) if packed else d['model']; animation_path=runtime_path(d['animation']) if packed else d['animation']; texture_path=runtime_path(d['texture']) if packed else d['texture']
  g=json.loads(read(model_path))
  try:a=json.loads(read(animation_path))['animations']
  except FileNotFoundError:
   if d.get('generated_static_animation') and not prepared and not packed:a={d['idle']:{'animation_length':1.0,'loop':True,'bones':{}}}
   else:raise
  if d.get('generated_static_animation') and not prepared and not packed:
   a.setdefault(d['idle'],{'animation_length':1.0,'loop':True,'bones':{}})
  bone_names={b['name'] for part in g['minecraft:geometry'] for b in part['bones']}
  if d['slug'] in SOURCE_FIRE_HASHES:
   slug=d['slug'];frames=[f'assets/minemon/textures/pokemon/{slug}_cobblemon_1_8_2/fire/normal/{i}.png' for i in range(1,5)];hashes=SOURCE_FIRE_HASHES[slug]
   source_fire_bones={b['name'] for part in g['minecraft:geometry'] for b in part['bones'] if b['name'].startswith('fire_mane') or b['name'].endswith('_fire')}
   if len(source_fire_bones)<5:raise ValueError('Animated source-fire rig bones missing: '+slug)
   for path,digest in zip(frames,hashes):
    b=read(runtime_path(path) if packed else path)
    if hashlib.sha256(b).hexdigest()!=digest:raise ValueError('Animated source-fire checksum mismatch: '+path)
    if struct.unpack('>II',b[16:24])!=(g['minecraft:geometry'][0]['description']['texture_width'],g['minecraft:geometry'][0]['description']['texture_height']):raise ValueError('Animated source-fire UV sheet dimensions mismatch: '+path)
    if len(b)<64:raise ValueError('Animated source-fire frame is truncated: '+path)
  for name in d.get('excluded_clips',[]):
   if prepared and name in a:raise ValueError('Excluded clip packaged '+name)
   a.pop(name,None)
  for name,v in a.items():
   missing=set(v.get('bones',{}))-bone_names
   approved_to_strip=set(d.get('stripped_bones',{}).get(name,[]))
   if prepared and missing:raise ValueError(name+' missing bones '+str(missing))
   if not prepared and missing!=approved_to_strip:raise ValueError(name+' missing bones '+str(missing)+' (audit allows only '+str(approved_to_strip)+')')
   for field in ('particle_effects','sound_effects'):
    ev=v.get(field,{})
    valid_event_value=lambda x:isinstance(x,dict) or (not prepared and isinstance(x,list) and all(isinstance(item,dict) for item in x))
    if not isinstance(ev,dict) or any(not valid_event_value(x) for x in ev.values()):raise ValueError(name+' incompatible event '+field)
   if prepared:
    if re.search(r'\bq\.',json.dumps(v)):raise ValueError(name+' unbound q alias')
    if 'animation_length' in v and v['animation_length']<=0:raise ValueError(name+' zero duration')
  generated=lambda n:any(n.endswith('.minemon_'+suffix) for suffix in ('run','sit','sleep','physical_attack','tail_flame','air_idle','air_fly'))
  for name in d['clips'].values():
   if name not in a and (prepared or not generated(name)):raise ValueError('Missing referenced clip '+name)
  attack=next((d['clips'][n] for n in ('physical_attack','physical','attack') if n in d['clips']),None)
  if attack and attack in a and d.get('attackTicks')!=math.ceil(a[attack]['animation_length']*20):raise ValueError('Attack duration mismatch '+d['slug'])
  if d['idle'] not in a or d['walk'] not in a:raise ValueError('Missing state clip '+d['slug'])
  if prepared and d['slug'] in ('spearow','fearow'):
   if d['clips'].get('air_fly')!='animation.'+d['slug']+'.minemon_air_fly' or d['clips'].get('air_idle')!='animation.'+d['slug']+'.minemon_air_idle':
    raise ValueError('Spearow/Fearow must select their model-specific air clips: '+d['slug'])
   air=a[d['clips']['air_fly']].get('bones',{})
   wings=['wing_open_'+side+segment for side in ('left','right') for segment in ('','2','3')] if d['slug']=='spearow' else ['wing_open_left','wing_open_right']
   if any('math.sin' not in json.dumps(air.get(name,{}).get('rotation')) for name in wings):
    raise ValueError('Flight clip must flap every retained wing segment: '+d['slug'])
   if d['slug']=='fearow':
    if air.get('body',{}).get('rotation',[0])[0]!=-22 or air.get('head',{}).get('rotation',[0])[0]!=5:
     raise ValueError('Fearow flight posture must lift its head above the original down-tilted air pose')
    if air.get('leg_left',{}).get('rotation',[0,0,0])[2]!=-5 or air.get('leg_right',{}).get('rotation',[0,0,0])[2]!=5:
     raise ValueError('Fearow flight posture must keep its feet aligned downward')
  if prepared and d['slug'] in ('articuno','zapdos','moltres'):
   if d['clips'].get('air_fly')!='animation.'+d['slug']+'.minemon_air_fly' or d['clips'].get('air_idle')!='animation.'+d['slug']+'.minemon_air_idle':
    raise ValueError('Legendary birds must select the articulated runtime air clips: '+d['slug'])
   air=a[d['clips']['air_fly']].get('bones',{})
   for side in ('left','right'):
    root='wing_open_'+side
    joints=[name for name in bone_names if name==root or name.startswith(root+'_') or name.startswith(root) and name[len(root):].isdigit()]
    if root not in joints or any('math.sin' not in json.dumps(air.get(name,{}).get('rotation')) for name in joints):
     raise ValueError('Every retained wing segment must move in flight: '+d['slug']+' '+side)
  if d['slug'] in SOURCE_FIRE_HASHES:
   fire_bones={b['name'] for part in g['minecraft:geometry'] for b in part['bones'] if b['name'].startswith('fire_mane') or b['name'].endswith('_fire')}
   for state,clip_name in (('idle',d['idle']),('walk',d['walk'])):
    if not fire_bones.intersection(a[clip_name].get('bones',{})):raise ValueError('Native fire overlay bones are not animated in '+state+': '+d['slug'])
   if d['slug']=='rapidash':
    run=d['clips'].get('ground_run')
    if not run or not fire_bones.intersection(a[run].get('bones',{})):raise ValueError('Rapidash native running fire mane does not animate')
  if number<=10:
   source_runs={3:'animation.venusaur.ground_run',6:'animation.charizard.ground_run',7:'animation.squirtle.ground_run',8:'animation.wartortle.ground_run',9:'animation.blastoise.ground_run'}
   expected_run=source_runs.get(number)
   if d['clips'].get('ground_run')!=expected_run:raise ValueError('Batch 1 selected run differs from the audited native animation inventory: '+d['slug'])
  if prepared:
   for state,keys in {'run':('ground_run','run'),'sit':('sit',),'sleep':('sleep',),'attack':('physical_attack','physical','attack')}.items():
    declared=[d['clips'][k] for k in keys if d['clips'].get(k)]
    if not declared:
     # A state with no clip in the species manifest is not part of that
     # species' native action inventory. Do not require or synthesize it.
     continue
    if not any(name in a for name in declared):raise ValueError('Missing prepared '+state+' animation '+d['slug'])
   if d['slug'] in ('charmander','charmeleon','charizard'):
    anchor=d.get('flame_anchor');flame=next((b for b in g['minecraft:geometry'][0]['bones'] if b['name']=='tail_flame'),None)
    if not anchor or not flame or flame.get('parent')!=anchor['host']:raise ValueError('Tail flame is not attached to its source flame bone: '+d['slug'])
    cubes=flame.get('cubes',[])
    if len(cubes)!=2 or any(any(n<=0 for n in cube.get('size',[])) for cube in cubes):raise ValueError('Tail flame is missing or has degenerate geometry: '+d['slug'])
    for cube in cubes:
     origin=cube.get('origin',[]);size=cube.get('size',[])
     if len(origin)!=3 or len(size)!=3 or abs(origin[1]-anchor['origin'][1])>0.001 or abs(origin[2]-anchor['origin'][2])>0.001 or abs((origin[1]+size[1])- (anchor['origin'][1]+anchor['size'][1]))>0.001 or abs((origin[2]+size[2])-(anchor['origin'][2]+anchor['size'][2]))>0.001:
      raise ValueError('Tail flame geometry no longer matches the source plane bounds: '+d['slug'])
   if d['slug'] in ('charmander','charmeleon','charizard'):
    if not d.get('tail_flame_animation'):
     raise ValueError('Fire starter has no independent tail-flame animation: '+d['slug'])
    flame_clip=d['clips'].get('tail_flame');clip=a.get(flame_clip,{})
    if not flame_clip or not clip.get('loop') or not {'rotation','scale'}.issubset(clip.get('bones',{}).get('tail_flame',{})):
     raise ValueError('Fire starter has no independent looping tail-flame rotation/scale animation: '+d['slug'])
   if d['slug']=='charmander':
    texwidth=g['minecraft:geometry'][0]['description']['texture_width']
    for cube in cubes:
     uv=cube.get('uv',[]);u,v=uv if isinstance(uv,list) and len(uv)==2 else (-1,-1)
     sx,sy,sz=cube.get('size',[0,0,0]);box_width=2*sx+2*sz;box_height=sy+sz
     if u<0 or v<0 or u+box_width>texwidth or v+box_height>g['minecraft:geometry'][0]['description']['texture_height']:
      raise ValueError('Tail flame box UV outside texture: '+d['slug'])
    for key,sign,base in [('left',1,{'charmander':35,'charmeleon':42.5,'charizard':55}[d['slug']]),('right',-1,{'charmander':-35,'charmeleon':-42.5,'charizard':-55}[d['slug']])]:
     for clip_key in ('ground_walk','ground_run'):
      if clip_key not in d['clips']:continue
      clip=d['clips'][clip_key];rotation=a[clip]['bones']['arm_'+key]['rotation']
      if len(rotation)!=3 or not str(rotation[2]).startswith(str(base)):
       raise ValueError('Walk/run arm rest pose was not preserved for '+d['slug']+'/'+clip_key+'/'+key)
   if d['slug']=='charmander':
    if 'charmander_cobblemon_1_8_2' not in d['model'] or 'charmander_cobblemon_1_8_2' not in d['texture'] or 'charmander_cobblemon_1_8_2' not in d['animation']:
     raise ValueError('Charmander pilot is not using the Cobblemon 1.8.2 model, texture, and animation assets')
    bones={b['name']:b for b in g['minecraft:geometry'][0]['bones']}
    if d.get('strip_bedrock_locators') and any('locators' in b for b in bones.values()):raise ValueError('Charmander runtime geometry retained unsupported Bedrock locator metadata')
    for name,parent,pivot in [('tail3','tail2',[0,4,13.5]),('fire','tail3',[0,4.25,16.5]),('fire2','fire',[0,4.25,16.5]),('locator_fire','fire2',[0,5,16.5])]:
     b=bones.get(name)
     if b is None or b.get('parent')!=parent or b.get('pivot')!=pivot:raise ValueError('Charmander Cobblemon tail/flame rig changed: '+name)
    if d['idle']!='animation.charmander.ground_idle' or d['walk']!='animation.charmander.ground_walk':raise ValueError('Charmander pilot clips do not use Cobblemon ground idle/walk')
    if not a[d['idle']].get('loop') or not a[d['walk']].get('loop'):raise ValueError('Charmander idle and walk must loop')
    for clip in (d['idle'],d['walk']):
     if not {'leg_left','leg_right','tail','torso'}.issubset(a[clip].get('bones',{})):raise ValueError('Charmander '+clip+' lost its source articulation channels')
  tex=read(texture_path);dims=struct.unpack('>II',tex[16:24]);desc=g['minecraft:geometry'][0]['description']
  if dims!=(desc['texture_width'],desc['texture_height']):raise ValueError('UV dimensions '+d['slug'])
 print('PASS runtime asset validation:',len(definitions),'species; National Pokédex order')
if __name__=='__main__':
 manifest='data/minemon_gen1/runtime_species.json'
 dex='data/minemon_gen1/pokedex.json'
 if len(sys.argv)>1:
  with zipfile.ZipFile(sys.argv[1]) as z:
   defs=json.loads(z.read(manifest));validate(z.read,defs,True,True)
   validate_gastly_smoke(z.read,defs)
   validate_target_scales(defs)
   validate_prepared_tail_flames(z.read,defs)
   validate_emissive_masks(z.read,defs)
   entries=json.loads(z.read(dex));assert len(entries)==151 and [e['nationalPokedexNumber'] for e in entries]==list(range(1,152))
   active={d['slug'] for d in defs};assert active=={e['slug'] for e in entries if e['registered']}
   allowed={runtime_path(p) for d in defs for p in [d['model'],d['texture'],d['animation']]+d['sounds']+d['licenses']}
   allowed.update(runtime_path(f'assets/minemon/textures/pokemon/{slug}_cobblemon_1_8_2/fire/normal/{i}.png') for slug in SOURCE_FIRE_HASHES for i in range(1,5))
   allowed.update(f'assets/minemon_gen1/textures/emissive/{slug}_tail_flame.png' for slug in ('charmander','charmeleon','charizard'))
   allowed.add('assets/minemon_gen1/textures/emissive/moltres_fire.png')
   roots=('assets/minemon_gen1/geo/','assets/minemon_gen1/animations/','assets/minemon_gen1/textures/pokemon/','assets/minemon_gen1/sounds/pokemon/','assets/minemon_gen1/licenses/pokemon/')
   roots=roots+('assets/minemon_gen1/textures/emissive/',)
   unexpected=[n for n in z.namelist() if not n.endswith('/') and n.startswith(roots) and n not in allowed]
   if unexpected:raise ValueError('Unexpected assets '+str(unexpected))
   for d in defs:
    egg=json.loads(z.read(f"assets/minemon_gen1/models/item/{d['slug']}_spawn_egg.json"));assert egg['parent']=='minecraft:item/template_spawn_egg'
   for p in ['pack.mcmeta','META-INF/mods.toml','META-INF/ASSET_LICENSES.md','br/com/petrickfaria/minemon/Minemon.class','br/com/petrickfaria/minemon/registry/ModParticles.class','br/com/petrickfaria/minemon/client/GastlySmokeParticle.class',GASTLY_PARTICLE,*GASTLY_PARTICLE_TEXTURES,'assets/minemon_gen1/textures/emissive/charmander_tail_flame.png','assets/minemon_gen1/textures/emissive/charmeleon_tail_flame.png','assets/minemon_gen1/textures/emissive/charizard_tail_flame.png','assets/minemon_gen1/textures/emissive/moltres_fire.png',manifest,dex]:assert p in z.namelist(),p
   print('PASS JAR registrations/resources:',len(defs),'eggs; only selected assets packaged')
   meta=z.read('META-INF/mods.toml').decode();assert 'modId="minemon_gen1"' in meta and 'displayName="Minemon (1ª Geração)"' in meta
   print('PASS Minemon (1ª Geração) metadata and minemon_gen1 ID')
 else:
  defs=json.loads((R/manifest).read_text());validate(lambda p:(R/p).read_bytes(),defs);validate_gastly_smoke(lambda p:(R/p).read_bytes(),defs)
  validate_target_scales(defs)
  enabled=next(l.split('=',1)[1].split(',') for l in (ROOT/'gradle.properties').read_text().splitlines() if l.startswith('runtime_species='))
  assert enabled==[d['slug'] for d in defs]
