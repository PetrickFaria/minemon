#!/usr/bin/env python3
"""Adapt copied runtime animations to GeckoLib 3; original assets remain intact."""
import json,re,sys,struct,zlib
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

def compatible_locomotion(data,geo,d):
 """Skeleton-specific new gaits. Every original animation remains byte-equivalent after normal adaptation."""
 import copy
 slug=d['slug'];animations=data['animations']
 def source(name):return copy.deepcopy(animations.get('animation.'+slug+'.'+name,{}))
 def put(kind,clip):animations['animation.'+slug+'.minemon_'+kind]=clip
 def sinus(base,amplitude,freq,phase=0):return f'({base})+{amplitude}*math.sin(query.anim_time*360*{freq}+{phase})'
 if slug in ('eevee','jolteon','flareon','vaporeon','scyther'):
  front='frontleg_' if slug=='vaporeon' else 'front_leg_'
  back='backleg_' if slug=='jolteon' else 'back_leg_'
  joints=[front+'left',front+'right',back+'left',back+'right'] if slug!='scyther' else ['left_upper_leg','right_upper_leg']
  for kind,freq,amp in [('walk',1.6,18),('run',2.7,30)]:
   if slug=='scyther' and kind=='run':continue
   clip=source('ground_idle');clip.update(loop=True,animation_length=1/freq)
   channels=clip.setdefault('bones',{})
   for i,joint in enumerate(joints):
    ch=channels.setdefault(joint,{})
    rotation=ch.get('rotation',[0,0,0]);rotation=list(rotation) if isinstance(rotation,list) else [0,0,0]
    rotation[0]=sinus(rotation[0],amp,freq,0 if i in (0,3) else 180)
    ch['rotation']=rotation
   body=channels.setdefault('body',{});position=body.get('position',[0,0,0]);position=list(position) if isinstance(position,list) else [0,0,0]
   position[1]=sinus(position[1],0.18 if kind=='walk' else 0.32,freq*2)
   body['position']=position
   tail=next((n for n in ('tail1','tail','abdomen') if n in {b['name'] for g in geo['minecraft:geometry'] for b in g['bones']}),None)
   if tail:
    ch=channels.setdefault(tail,{});rot=list(ch.get('rotation',[0,0,0]));rot[1]=sinus(rot[1],4 if kind=='walk' else 7,freq);ch['rotation']=rot
   put(kind,clip)
 if slug in ('meowth','persian'):
  # These two retained rigs have no locomotion clips. Animate their real
  # upper-leg bones with a reciprocal walk/run stride and a small body bob.
  joints={'meowth':['leg_left','leg_right'],
          'persian':['leg_front_left1','leg_front_right1','leg_back_left1','leg_back_right1']}[slug]
  for kind,freq,amp in [('walk',1.5,17),('run',2.5,27)]:
   clip=source('ground_idle');clip.update(loop=True,animation_length=1/freq)
   channels=clip.setdefault('bones',{})
   for i,joint in enumerate(joints):
    if joint not in {b['name'] for g in geo['minecraft:geometry'] for b in g['bones']}:continue
    channels.setdefault(joint,{})['rotation']=[sinus(0,amp,freq,0 if i%2==0 else 180),0,0]
   body=channels.setdefault('body',{})
   body['position']=[0,sinus(0,0.12 if kind=='walk' else 0.2,freq*2),0]
   put(kind,clip)
 if slug=='vaporeon':
  clip=source('ground_idle');clip.update(loop=True,animation_length=1.0)
  channels=clip['bones']
  for i in range(1,7):
   name='tail' if i==1 else 'tail'+str(i)
   ch=channels.setdefault(name,{});rot=list(ch.get('rotation',[0,0,0]));rot[1]=sinus(rot[1],10+i*2,1.0,-i*25);ch['rotation']=rot
  for i,joint in enumerate(('frontleg_left','frontleg_right','back_leg_left','back_leg_right')):
   ch=channels.setdefault(joint,{});rot=list(ch.get('rotation',[0,0,0]));rot[0]=sinus(25,14,1.0,0 if i%2==0 else 180);ch['rotation']=rot
  put('swim',clip)

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
 flame_name=clips.get('tail_flame','')
 if d.get('tail_flame_animation') and flame_name:
  animations[flame_name]={
   'animation_length':1.0,'loop':True,
   'bones':{'tail_flame':{
    # Molang math.sin takes degrees; the previous sub-degree-per-second
    # multipliers produced effectively static bending and scale changes.
    'rotation':['2.4*math.sin(query.anim_time*90*16)','1.2*math.sin(query.anim_time*90*14+35)','1.8*math.sin(query.anim_time*90*12+110)'],
    'scale':['1.0+0.02*math.sin(query.anim_time*90*16+20)','1.0+0.025*math.sin(query.anim_time*90*14+45)','1.0+0.02*math.sin(query.anim_time*90*16+20)']
   }}
  }

 # The three fire starters have arm bones posed outward in their bind pose.
 # Their source walk clips animate those bones but flatten the Z rotation to
 # roughly zero, unlike the inward rotations in each species' idle clip.
 # Keep the original shoulder swing channels and restore the model-specific
 # inward pose with a restrained opposite-phase gait swing.
 if d.get('slug') in ('charmander','charmeleon','charizard') and not d.get('preserve_arm_pose',False) and walk_name in animations:
  rest={'charmander':35.0,'charmeleon':42.5,'charizard':55.0}[d['slug']]
  swing={'charmander':5.0,'charmeleon':4.0,'charizard':5.0}[d['slug']]
  frequency={'charmander':'90*6','charmeleon':'90*4.5','charizard':'90*3'}[d['slug']]
  for clip_name in (walk_name,run_name):
   if not clip_name or clip_name not in animations: continue
   walk_bones=animations[clip_name].setdefault('bones',{})
   for side,sign in (('left',1),('right',-1)):
    name='arm_'+side
    channel=walk_bones.get(name)
    if not isinstance(channel,dict): continue
    rotation=channel.get('rotation')
    if isinstance(rotation,list) and len(rotation)==3:
     channel['rotation'][2]=f'{sign*rest:g}{"+" if sign>0 else "-"}{swing:g}*math.sin(query.anim_time*{frequency})'
    elif clip_name==run_name and isinstance(rotation,dict):
     # Some native run clips use a keyframed, almost flat arm Z angle instead
     # of a Molang vector. Rebase only that axis onto the species' validated
     # lowered arm pose; retain source X/Y motion and all relative Z keyframes.
     frames=[]
     for frame in rotation.values():
      vector=frame.get('post',frame.get('pre',frame.get('vector'))) if isinstance(frame,dict) else frame
      if isinstance(vector,list) and len(vector)==3 and isinstance(vector[2],(int,float)):
       frames.append((frame,vector))
     if frames:
      offset=sign*rest-frames[0][1][2]
      for frame,vector in frames:
       if isinstance(frame,dict):
        for key in ('post','pre','vector'):
         if isinstance(frame.get(key),list) and len(frame[key])==3:
          frame[key][2]+=offset
       else:
        vector[2]+=offset

def flatten_bedrock_keyframes(data):
 # GeckoLib 3.1.40 does not deserialize Bedrock pre/post + lerp_mode objects.
 # Keep each source keyframe's post value (or pre when it has no post) and let
 # GeckoLib interpolate between those unchanged time/value anchors linearly.
 for animation in data.get('animations',{}).values():
  for channels in animation.get('bones',{}).values():
   for channel,value in list(channels.items()):
    if not isinstance(value,dict):continue
    frames={}
    for timestamp,keyframe in value.items():
     if isinstance(keyframe,dict):
      keyframe=keyframe.get('post',keyframe.get('pre',keyframe.get('vector',keyframe)))
      if isinstance(keyframe,dict):keyframe=keyframe.get('vector',keyframe)
     frames[timestamp]=keyframe
    channels[channel]=frames

def png_chunks(data):
 if data[:8]!=b'\x89PNG\r\n\x1a\n': raise ValueError('Not a PNG texture')
 pos=8;chunks=[]
 while pos<len(data):
  n=struct.unpack('>I',data[pos:pos+4])[0];kind=data[pos+4:pos+8];payload=data[pos+8:pos+8+n];chunks.append((kind,payload));pos+=12+n
  if kind==b'IEND':break
 return chunks

def decode_png_rgba(data):
 chunks=png_chunks(data);items={kind:payload for kind,payload in chunks}
 w,h,depth,color,compression,filter_method,interlace=struct.unpack('>IIBBBBB',items[b'IHDR'])
 if depth not in (1,2,4,8) or color not in (2,3,6) or interlace!=0 or (color!=3 and depth!=8):raise ValueError('Unsupported source PNG format')
 channels={2:3,3:1,6:4}[color];filter_bpp=max(1,(channels*depth+7)//8);stride=(w*channels*depth+7)//8
 packed=zlib.decompress(b''.join(payload for kind,payload in chunks if kind==b'IDAT'))
 prev=bytearray(stride);rows=[];offset=0
 for _ in range(h):
  f=packed[offset];offset+=1;scan=bytearray(packed[offset:offset+stride]);offset+=stride
  for i in range(stride):
   a=scan[i-filter_bpp] if i>=filter_bpp else 0;b=prev[i];c=prev[i-filter_bpp] if i>=filter_bpp else 0
   if f==1:scan[i]=(scan[i]+a)&255
   elif f==2:scan[i]=(scan[i]+b)&255
   elif f==3:scan[i]=(scan[i]+((a+b)//2))&255
   elif f==4:
    p=a+b-c;pa=abs(p-a);pb=abs(p-b);pc=abs(p-c);scan[i]=(scan[i]+(a if pa<=pb and pa<=pc else b if pb<=pc else c))&255
   elif f!=0:raise ValueError('Unsupported PNG filter')
  rows.append(scan);prev=scan
 palette=items.get(b'PLTE',b'');alpha=items.get(b'tRNS',b'');pixels=[]
 for row in rows:
  out=bytearray()
  if color==6: out.extend(row)
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

def encode_png_rgba(w,h,rows):
 def chunk(kind,payload):return struct.pack('>I',len(payload))+kind+payload+struct.pack('>I',zlib.crc32(kind+payload)&0xffffffff)
 raw=b''.join(b'\0'+bytes(row) for row in rows)
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')

def prepare_tail_flame(root,d):
 slug=d.get('slug')
 if slug not in ('charmander','charmeleon','charizard'):return
 model=root/'assets/minemon_gen1'/d['model'].removeprefix('assets/minemon/')
 texture=root/'assets/minemon_gen1'/d['texture'].removeprefix('assets/minemon/')
 geo=json.loads(model.read_text());texture_data=texture.read_bytes();w,h,_=decode_png_rgba(texture_data)
 bones=geo['minecraft:geometry'][0]['bones']
 if d.get('strip_bedrock_locators'):
  for bone in bones:bone.pop('locators',None)
 host=next((b for b in bones if b['name'] in ('fire2','fire') and b.get('cubes')),None)
 if host is None:raise ValueError(slug+' model has no source tail-flame mesh')
 source_cubes=host['cubes']
 if len(source_cubes)!=2:raise ValueError(slug+' source flame did not contain its two native crossing planes')
 source_flame=root/'assets/minemon_gen1'/d['flame_texture'].removeprefix('assets/minemon/')
 sw,sh,flame_rows=decode_png_rgba(source_flame.read_bytes())
 mask_rows=[bytearray(w*4) for _ in range(h)];runtime_cubes=[];cursor_y=0
 # Each zero-width Bedrock plane has an independent flame sprite and, in some
 # species, its own pivot/rotation. Preserve both native UV regions and poses.
 for source_cube in source_cubes:
  origin=source_cube['origin'];size=source_cube['size'];uv=source_cube['uv']
  if len(size)!=3 or size[0]!=0:raise ValueError(slug+' source flame plane did not match the audited zero-width layout')
  fw,fh=int(round(size[2])),int(round(size[1]));u,v=int(round(uv[0])),int(round(uv[1]))
  # Bedrock's zero-width plane selects its visible face from the side UV
  # rectangles, whose vertical origin includes the depth (z) of the box.
  source_y=v+fw
  if u<0 or source_y<0 or u+fw>sw or source_y+fh>sh:raise ValueError(slug+' source flame UV lies outside its flame sheet')
  sprite=[bytes(flame_rows[source_y+yy][u*4:(u+fw)*4]) for yy in range(fh)]
  # Keep the original texture atlas and dimensions untouched. The separate,
  # same-size emissive mask contains these pixels at a private UV footprint.
  uv_x,uv_y=0,cursor_y
  # For a thin X-by-Y-by-Z box the visible side faces are (u,v+z) and
  # (u+x+z,v+z). Fill both with this plane's own original flame pixels.
  for sprite_x in (uv_x,uv_x+1+fw):
   for yy in range(fh):
    row=mask_rows[uv_y+fw+yy]
    row[sprite_x*4:(sprite_x+fw)*4]=sprite[yy]
  cube={'origin':[-0.5,origin[1],origin[2]],'size':[1,fh,fw],'uv':[uv_x,uv_y]}
  for field in ('pivot','rotation'):
   if field in source_cube:cube[field]=source_cube[field]
  runtime_cubes.append(cube)
  cursor_y+=fh+fw+1
 # Replace the native zero-width planes with finite meshes while retaining
 # each source plane's pivot, rotation, dimensions and independent texture UVs.
 pivot=host.get('pivot',[0,0,0]);parent=host['name'];host['cubes']=[]
 bones.append({'name':'tail_flame','parent':parent,'pivot':pivot,'cubes':runtime_cubes})
 model.write_text(json.dumps(geo,ensure_ascii=False,indent=2)+'\n')
 mask=root/'assets/minemon_gen1/textures/emissive'/f'{slug}_tail_flame.png'
 mask.parent.mkdir(parents=True,exist_ok=True)
 mask.write_bytes(encode_png_rgba(w,h,mask_rows))
 # This is only a staging copy; keep the canonical source atlas untouched.
 source_flame.unlink()

def prepare_moltres_emissive_mask(root,d):
 if d.get('slug')!='moltres':return
 source=root/'assets/minemon_gen1'/d['texture'].removeprefix('assets/minemon/')
 w,h,pixels=decode_png_rgba(source.read_bytes())
 # Fire pixels only: retain Moltres's native RGB and alpha, discard all
 # plumage/body pixels from the emissive atlas. The renderer further limits
 # this mask to the wing and native fire bone branches.
 for row in pixels:
  for i in range(0,len(row),4):
   r,g,b,a=row[i:i+4]
   flame=(a>0 and r>=180 and g>=55 and g<=r*0.82 and b<=105 and r-b>=85)
   if not flame:row[i+3]=0
 mask=root/'assets/minemon_gen1/textures/emissive/moltres_fire.png'
 mask.parent.mkdir(parents=True,exist_ok=True)
 mask.write_bytes(encode_png_rgba(w,h,pixels))
for d in definitions.values():
 if d.get('generated_static_animation'):
  p=root/'assets/minemon_gen1'/d['animation'].removeprefix('assets/minemon/')
  p.parent.mkdir(parents=True,exist_ok=True)
  data=json.loads(p.read_text()) if p.is_file() else {'format_version':'1.8.0','animations':{}}
  data.setdefault('animations',{})[d['idle']]={'animation_length':1.0,'loop':True,'bones':{}}
  p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+chr(10))
for p in (root/'assets/minemon_gen1/animations').rglob('*.animation.json'):
 data=json.loads(p.read_text())
 def normalize(v):
  if isinstance(v,str): return re.sub(r'\bq\.', 'query.',v)
  if isinstance(v,list): return [normalize(x) for x in v]
  if isinstance(v,dict): return {k:normalize(x) for k,x in v.items()}
  return v
 data=normalize(data)
 definition=definitions.get(str(p.relative_to(root)),{})
 if definition.get('geckolib3_bedrock_keyframes'):
  flatten_bedrock_keyframes(data)
 for name in definition.get('excluded_clips',[]): data.get('animations',{}).pop(name,None)
 if definition:
  # Some source clips contain channels for bones omitted from the selected
  # model variant. Remove only those channels in the copied runtime resource;
  # keep the original animation file and all valid animated bones intact.
  for clip_name,missing_bones in definition.get('stripped_bones',{}).items():
   clip=data.get('animations',{}).get(clip_name,{})
   channels=clip.get('bones',{})
   for bone in missing_bones: channels.pop(bone,None)
  # Files in the processed resource tree keep the normal assets/minemon root.
  model=root/'assets/minemon_gen1'/definition['model'].removeprefix('assets/minemon/')
  if model.exists():
   geometry=json.loads(model.read_text())
   build_runtime_clips(data,geometry,definition)
   compatible_locomotion(data,geometry,definition)
  prepare_tail_flame(root,definition)
  prepare_moltres_emissive_mask(root,definition)
 # GeckoLib 3.1.40 dereferences this field while parsing every clip, including
 # expression-only looping clips. Keep the source untouched and add a runtime fallback.
 for animation in data.get('animations',{}).values():
  for field in ('particle_effects','sound_effects'):
   events=animation.get(field)
   if not isinstance(events,dict) or any(not isinstance(value,dict) for value in events.values()): animation.pop(field,None)
  if 'animation_length' not in animation:animation['animation_length']=4.0
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
 count+=1
for definition in definitions.values():
 if not definition.get('strip_bedrock_locators'):continue
 model=root/'assets/minemon_gen1'/definition['model'].removeprefix('assets/minemon/')
 if not model.is_file():continue
 geo=json.loads(model.read_text());changed=False
 for bone in geo.get('minecraft:geometry',[{}])[0].get('bones',[]):
  if 'locators' in bone:bone.pop('locators');changed=True
 if changed:model.write_text(json.dumps(geo,ensure_ascii=False,indent=2)+'\n')
print('GeckoLib 3 runtime animations prepared:',count)
