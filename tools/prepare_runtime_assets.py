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
 geo=json.loads(model.read_text());texture_data=texture.read_bytes();w,h,rows=decode_png_rgba(texture_data)
 bones=geo['minecraft:geometry'][0]['bones']
 if d.get('strip_bedrock_locators'):
  for bone in bones:bone.pop('locators',None)
 host=next((b for b in bones if b['name'] in ('fire2','fire') and b.get('cubes')),None)
 if host is None:raise ValueError(slug+' model has no source tail-flame mesh')
 source_cube=host['cubes'][0];origin=source_cube['origin'];size=source_cube['size'];uv=source_cube['uv']
 if len(size)!=3 or size[0]!=0:raise ValueError(slug+' source flame plane did not match the audited zero-width layout')
 fw,fh=int(round(size[2])),int(round(size[1]))
 source_flame=root/'assets/minemon_gen1'/d['flame_texture'].removeprefix('assets/minemon/')
 sw,sh,flame_rows=decode_png_rgba(source_flame.read_bytes())
 u,v=int(round(uv[0])),int(round(uv[1]))
 if u<0 or v<0 or u+fw>sw or v+fh>sh:raise ValueError(slug+' source flame UV lies outside its flame sheet')
 pad=2;new_w=w+32;x=w+pad;y=2
 sprite=[bytes(flame_rows[v+yy][u*4:(u+fw)*4]) for yy in range(fh)]
 # Box UV side-face rectangles for the thin X-by-Y-by-Z cube are located at
 # (u, v+z) and (u+x+z, v+z); populate both visible sides with the original
 # flame frame. The other box faces remain transparent.
 uv_x=x-1;uv_y=y-1+fw
 for sprite_x in (uv_x,uv_x+1+fw):
  for yy in range(fh):
   row=rows[uv_y+yy]
   if len(row)<new_w*4:row.extend(b'\0'*(new_w*4-len(row)))
   row[sprite_x*4:(sprite_x+fw)*4]=sprite[yy]
 for row in rows:
  if len(row)<new_w*4:row.extend(b'\0'*(new_w*4-len(row)))
 description=geo['minecraft:geometry'][0]['description'];description['texture_width']=new_w
 # Replace the source zero-width plane by two thin crossing faces. Their
 # vertical/depth bounds and parent come from the exact Cobblemon geometry;
 # the previous hard-coded center moved this flame several units down-tail.
 pivot=host.get('pivot',[0,0,0]);parent=host['name'];host['cubes']=[]
 cube={'origin':[-0.5,origin[1],origin[2]],'size':[1,fh,fw],'uv':[uv_x,y-1]}
 crossed={'origin':[-0.5,origin[1],origin[2]],'size':[1,fh,fw],'pivot':pivot,
          'rotation':[0,90,0],'uv':[uv_x,y-1]}
 bones.append({'name':'tail_flame','parent':parent,'pivot':pivot,'cubes':[cube,crossed]})
 model.write_text(json.dumps(geo,ensure_ascii=False,indent=2)+'\n')
 texture.write_bytes(encode_png_rgba(new_w,h,rows))
 source_flame.unlink()
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
  if model.exists():build_runtime_clips(data,json.loads(model.read_text()),definition)
  prepare_tail_flame(root,definition)
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
