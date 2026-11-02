package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.client.MinemonRenderer;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.*;
import net.minecraftforge.event.TickEvent;
import java.nio.file.*;
import java.util.*;

/** Additional real-client scenario: production night AI, rendered closed eyes, dawn. */
final class NightClientSmokeTest {
 private static final List<String> SPECIES=List.of("tentacool","tentacruel","horsea","seadra","goldeen","seaking","magikarp","shellder","cloyster","gyarados","lapras");
 private static boolean opened,setup,spawned,waking,gengarWakeEyesObserved,nocturnalMidday;
 private static int ticks,index,samples,activeSamples,sleepTick,depthSettleTicks,depthStableTicks,restSettleTicks,restStableTicks;
 private static volatile UUID entity;
 private static final Map<String,float[]> eyes=new HashMap<>();
 private static final Map<String,float[]> restingWings=new HashMap<>();
 private static final Map<String,float[]> swimMotion=new HashMap<>();
 private static net.minecraft.world.phys.Vec3 lastSleepingPosition;
 static void tick(TickEvent.ClientTickEvent event){
  var mc=Minecraft.getInstance();
  try{
   if(!opened&&mc.screen instanceof TitleScreen){
    opened=true;Files.writeString(Path.of("client-validation-night.txt"),"Production night sleep render scenario\n");
    var dataFile=Path.of("saves/minemon-validation/level.dat").toFile();var tag=net.minecraft.nbt.NbtIo.readCompressed(dataFile);
    tag.getCompound("Data").putBoolean("confirmedExperimentalSettings",true);net.minecraft.nbt.NbtIo.writeCompressed(tag,dataFile);
    Object flows=Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);
    for(var method:flows.getClass().getMethods())if(method.getName().equals("loadLevel")){
     if(method.getParameterCount()==2&&method.getParameterTypes()[1]==String.class){method.invoke(flows,mc.screen,"minemon-validation");break;}
     if(method.getParameterCount()==1&&method.getParameterTypes()[0]==String.class){method.invoke(flows,"minemon-validation");break;}
    }
    MinemonRenderer.validationInput=mob->{
     if(mob.isSleeping()&&Set.of("gengar","haunter","fearow").contains(mob.getSpeciesSlug())){
      mob.setYRot(180);mob.yRotO=180;mob.yBodyRot=180;mob.yBodyRotO=180;mob.yHeadRot=180;mob.yHeadRotO=180;
     }
    };
    MinemonRenderer.validationObserver=(mob,model)->{
     if(entity==null||!mob.getUUID().equals(entity))return;
     if(SpeciesBehaviorProfiles.usesSwimmingSleepAnimation(mob.getSpeciesSlug())&&!mob.isSleeping()){
      for(String side:List.of("left","right")){
       for(String name:List.of("eye_"+side,side+"_eye",side+"eye")){
        var eye=model.getAnimationProcessor().getBone(name);
        if(eye!=null&&eye.isHidden())throw new IllegalStateException(mob.getSpeciesSlug()+" eye stayed hidden after waking: "+name);
       }
       for(String name:List.of("eyelid_"+side,"closed_eyelid_"+side,side+"_eye_closed","eye_"+side+"_closed",side+"eyelid")){
        var lid=model.getAnimationProcessor().getBone(name);
        if(lid!=null&&!lid.isHidden())throw new IllegalStateException(mob.getSpeciesSlug()+" closed eyelid stayed visible after waking: "+name);
       }
      }
      var eyes=model.getAnimationProcessor().getBone("eyes");
      if(eyes!=null&&eyes.getScaleY()<eyes.getInitialSnapshot().scaleValueY*.9F)
       throw new IllegalStateException(mob.getSpeciesSlug()+" eye plate stayed flattened after waking");
     }
     if(mob.getSpeciesSlug().equals("gengar")){
      for(String side:List.of("left","right")){
       var eye=model.getAnimationProcessor().getBone("eye_"+side);
       var lid=model.getAnimationProcessor().getBone("closed_eyelid_"+side);
       var pupil=model.getAnimationProcessor().getBone("pupil_"+side);
       if(eye==null||lid==null||pupil==null)throw new IllegalStateException("Gengar full native eye rig missing");
       if(eye.isHidden()!=mob.isSleeping()||eye.childBonesAreHiddenToo()
          ||lid.isHidden()==mob.isSleeping()||pupil.isHidden()!=mob.isSleeping())
        throw new IllegalStateException("Gengar must visibly select both full closed lids only during sleep");
      }
      if(waking&&!mob.isSleeping())gengarWakeEyesObserved=true;
     }
     boolean nocturnal=SpeciesBehaviorProfiles.isNocturnal(mob.getSpeciesSlug());
     if(nocturnal&&!nocturnalMidday){
      if(mob.isSleeping())throw new IllegalStateException(mob.getSpeciesSlug()+" must stay awake through the night and first half of daylight");
      activeSamples++;
      return;
     }
     if(!mob.isSleeping())return;
     var definition=SpeciesDefinitions.get(mob.getSpeciesSlug());
     boolean swimSleep=SpeciesBehaviorProfiles.usesSwimmingSleepAnimation(mob.getSpeciesSlug())&&mob.isInWaterOrBubble();
     String sleepKey=mob.getSpeciesSlug().equals("vaporeon")
             ? (mob.isInWaterOrBubble()?"night_sleep_water":"night_sleep_land") : "night_sleep";
     String selectedClip=swimSleep
             ? firstClip(mob.getSpeciesSlug(),"water_swim","surface_water_swim","surfacewater_swim","water_surface_swim")
             : definition.clips().getOrDefault(sleepKey,definition.clips().get("night_sleep"));
     if(swimSleep&&selectedClip==null)selectedClip=SpeciesAssetIndex.clip(mob.getSpeciesSlug(),"water_idle",definition.idle());
     if(!Objects.equals(mob.getSelectedAnimationForValidation(),selectedClip))return;
    if(sleepTick==0)sleepTick=mob.tickCount;
    if(mob.tickCount-sleepTick<12)return;
     // Read the generated pose, whose eye tracks hold the species' native blink closure.
     var d=SpeciesDefinitions.get(mob.getSpeciesSlug());
     try(var stream=NightClientSmokeTest.class.getResourceAsStream("/"+d.animation().replace("assets/minemon/","assets/minemon_gen1/"))){
      var animations=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject().getAsJsonObject("animations");
      String blink=d.clips().get("blink");
      {
       var sleepBones=animations.getAsJsonObject(selectedClip).getAsJsonObject("bones");
       if(swimSleep)for(String name:sleepBones.keySet()){
        if(name.contains("eye")||name.contains("pupil")||name.contains("eyelid"))continue;
        var bone=model.getAnimationProcessor().getBone(name);if(bone==null)continue;
        float[] rotation={bone.getRotationX(),bone.getRotationY(),bone.getRotationZ()};
        float[] range=swimMotion.get(name);
        if(range==null)swimMotion.put(name,new float[]{rotation[0],rotation[0],rotation[1],rotation[1],rotation[2],rotation[2]});
        else for(int axis=0;axis<3;axis++){range[axis*2]=Math.min(range[axis*2],rotation[axis]);range[axis*2+1]=Math.max(range[axis*2+1],rotation[axis]);}
       }
       if(java.util.Set.of("spearow","fearow","articuno","zapdos","moltres").contains(mob.getSpeciesSlug())){
        for(String name:sleepBones.keySet())if(name.startsWith("wing_")||name.equals("wings_open")||name.equals("wings_closed")){
         var wing=model.getAnimationProcessor().getBone(name);if(wing==null)continue;
         float[] rotation={wing.getRotationX(),wing.getRotationY(),wing.getRotationZ()};
         float[] prior=restingWings.putIfAbsent(name,rotation);
         if(prior!=null)for(int axis=0;axis<3;axis++)if(Math.abs(prior[axis]-rotation[axis])>0.015F)
          throw new IllegalStateException(mob.getSpeciesSlug()+" wing moved during grounded sleep: "+name+" axis="+axis+" delta="+Math.abs(prior[axis]-rotation[axis]));
        }
       }
       if(swimSleep){
        boolean eyeFound=false,closedFound=false;
        for(String side:List.of("left","right")){
         for(String name:List.of("eye_"+side,side+"_eye",side+"eye")){
          var eye=model.getAnimationProcessor().getBone(name);
          if(eye!=null){eyeFound=true;if(!eye.isHidden())throw new IllegalStateException(mob.getSpeciesSlug()+" sleep eye remains open: "+name);}
         }
         for(String name:List.of("eyelid_"+side,"closed_eyelid_"+side,side+"_eye_closed","eye_"+side+"_closed",side+"eyelid")){
          var lid=model.getAnimationProcessor().getBone(name);
          if(lid!=null){closedFound=true;if(lid.isHidden())throw new IllegalStateException(mob.getSpeciesSlug()+" closed eyelid is hidden: "+name);}
         }
        }
        var eyes=model.getAnimationProcessor().getBone("eyes");
        if(eyes!=null){eyeFound=true;closedFound=true;if(eyes.getScaleY()>eyes.getInitialSnapshot().scaleValueY*.1F)throw new IllegalStateException("Goldeen eye plate was not flattened for sleep");}
        if(!eyeFound||!closedFound)throw new IllegalStateException(mob.getSpeciesSlug()+" has no verified closed-eye swim pose");
       }
       java.util.Set<String> eyeNames=swimSleep?java.util.Set.of():blink!=null&&animations.has(blink)?animations.getAsJsonObject(blink).getAsJsonObject("bones").keySet():sleepBones.keySet().stream().filter(n->n.contains("eye")||n.contains("pupil")).collect(java.util.stream.Collectors.toSet());
       if(eyeNames.isEmpty())eyeNames=sleepBones.keySet().stream().filter(n->n.contains("eye")||n.contains("pupil")).collect(java.util.stream.Collectors.toSet());
       for(String name:eyeNames){
        var bone=model.getAnimationProcessor().getBone(name);if(bone==null)continue;
        var tracks=sleepBones.getAsJsonObject(name);
        for(String channel:List.of("position","rotation","scale"))if(tracks.has(channel)&&tracks.get(channel).isJsonArray()){
         var expected=tracks.getAsJsonArray(channel);
         float[] actual=switch(channel){case "position"->new float[]{bone.getPositionX(),bone.getPositionY(),bone.getPositionZ()};case "rotation"->new float[]{bone.getRotationX(),bone.getRotationY(),bone.getRotationZ()};default->new float[]{bone.getScaleX(),bone.getScaleY(),bone.getScaleZ()};};
         for(int axis=0;axis<3;axis++){
          if(!expected.get(axis).isJsonPrimitive()||!expected.get(axis).getAsJsonPrimitive().isNumber())continue;
          float value=expected.get(axis).getAsFloat()*(channel.equals("rotation")?(float)Math.PI/180:1);
          var initial=bone.getInitialSnapshot();
          float[] bind=switch(channel){case "position"->new float[]{initial.positionOffsetX,initial.positionOffsetY,initial.positionOffsetZ};case "rotation"->new float[]{initial.rotationValueX,initial.rotationValueY,initial.rotationValueZ};default->new float[]{initial.scaleValueX,initial.scaleValueY,initial.scaleValueZ};};
          float rendered=channel.equals("scale")?actual[axis]/bind[axis]:actual[axis]-bind[axis];
          if(Math.abs(Math.abs(rendered)-Math.abs(value))>.015F)throw new IllegalStateException(mob.getSpeciesSlug()+" eye closure mismatch "+name+" "+channel+" axis="+axis+" got="+actual[axis]+" expected="+value);
         }
         eyes.put(name+channel,actual);
        }
       }
      }
      samples++;
     }catch(Exception e){throw new IllegalStateException("Night rendered eye verification failed",e);}
    };
   }
   if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||mc.screen!=null)return;
   if(!setup){setup=true;mc.options.hideGui=true;mc.getSingleplayerServer().execute(()->{
    var level=mc.getSingleplayerServer().overworld();level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,level.getServer());
    for(int x=-12;x<=12;x++)for(int z=-8;z<=18;z++){
     level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
     for(int y=101;y<=116;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
    }
    for(int x:new int[]{-2,2})for(int z:new int[]{3,7})level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.SEA_LANTERN.defaultBlockState());
    var p=level.getServer().getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.teleportTo(level,0,102,-5,0,8);
   });return;}
   if(!spawned){
    spawned=true;gengarWakeEyesObserved=false;nocturnalMidday=false;ticks=0;samples=0;activeSamples=0;sleepTick=0;depthSettleTicks=0;depthStableTicks=0;restSettleTicks=0;restStableTicks=0;waking=false;eyes.clear();restingWings.clear();swimMotion.clear();lastSleepingPosition=null;
    mc.getSingleplayerServer().execute(()->{
     var level=mc.getSingleplayerServer().overworld();if(entity!=null){var old=level.getEntity(entity);if(old!=null)old.discard();}
     String scenario=SPECIES.get(index);String slug=scenario.equals("vaporeon_land")?"vaporeon":scenario;
     var mob=ModEntities.SPECIES.get(slug).get().create(level);
     boolean aquatic=SpeciesBehaviorProfiles.waterCapable(mob.getSpeciesSlug())&&!scenario.equals("vaporeon_land");
     boolean air=SpeciesBehaviorProfiles.canFly(mob.getSpeciesSlug());
     // Each species receives a clean environment. The previous Vaporeon
     // scenario's basin otherwise remains and changes a land species' support
     // height and resting environment.
     for(int x=-6;x<=6;x++)for(int z=0;z<=10;z++)for(int y=101;y<=110;y++)
      level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
     if(aquatic)for(int x=-6;x<=6;x++)for(int z=0;z<=10;z++)for(int y=101;y<=110;y++)
      level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState());
     double spawnY=aquatic?(slug.equals("magikarp")?110.0D:104.0D):air?107.0D:101.0D;
     mob.moveTo(0,spawnY,5,180,0);
     if(SpeciesBehaviorProfiles.profile(mob.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.GROUND_FLIGHT
         ||SpeciesBehaviorProfiles.profile(mob.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.GROUND_FLOAT_CYCLE){
      var tag=new net.minecraft.nbt.CompoundTag();mob.addAdditionalSaveData(tag);tag.putString("NaturalBehaviorPhase","AIR");mob.readAdditionalSaveData(tag);
     }
     level.addFreshEntity(mob);entity=mob.getUUID();level.setDayTime(18000);
     double distance=switch(mob.getSpeciesSlug()){
      case "venusaur"->7;case "snorlax","charizard","dragonite"->6;case "fearow"->9;
      case "victreebel","scyther","weezing"->5;case "pidgeotto","haunter","butterfree","beedrill","parasect"->4;
      case "pidgey","gastly","kakuna"->2.5;case "gyarados","lapras"->8;default->3;
     };
     var player=level.getServer().getPlayerList().getPlayers().get(0);player.teleportTo(level,slug.equals("fearow")?distance:0,101,slug.equals("fearow")?5:5-distance,0,0);
    });return;
   }
   ticks++;if(ticks>450){
    var observed=entity==null?null:mc.level.getEntitiesOfClass(MinemonCreature.class,
        mc.player.getBoundingBox().inflate(128.0D),m->m.getUUID().equals(entity)).stream().findFirst().orElse(null);
    boolean rendered=false;
    for(var candidate:mc.level.entitiesForRendering())if(candidate.getUUID().equals(entity)){rendered=true;break;}
    String state=observed!=null
        ? " sleeping="+observed.isSleeping()+" nightRest="+observed.isNightResting()+" clip="+observed.getSelectedAnimationForValidation()
          +" pos="+observed.position()+" support="+observed.naturalSurface()+" onGround="+observed.isOnGround()
          +" noGravity="+observed.isNoGravity()+" navDone="+observed.getNavigation().isDone()
          +" rendered="+rendered
        : " entityMissing="+(observed==null);
    throw new IllegalStateException("Night client timeout: "+SPECIES.get(index)+" samples="+samples+" activeSamples="+activeSamples+" midday="+nocturnalMidday+state);
   }
   MinemonCreature current=null;for(var e:mc.level.entitiesForRendering())if(e.getUUID().equals(entity)&&e instanceof MinemonCreature m){current=m;break;}
   if(current==null)return;
   boolean waterRest=SpeciesBehaviorProfiles.waterCapable(current.getSpeciesSlug())&&current.isInWaterOrBubble();
   if(current.isSleeping()){
    if(lastSleepingPosition!=null&&lastSleepingPosition.distanceToSqr(current.position())>1.0E-8D)
     throw new IllegalStateException(current.getSpeciesSlug()+" moved physically while sleeping: "+lastSleepingPosition+" -> "+current.position());
    lastSleepingPosition=current.position();
    if(waterRest){
     double target=WaterMovementGoal.waterRestY(current);
     double error=Math.abs(current.getY()-target);
     if(SpeciesBehaviorProfiles.usesSwimmingSleepAnimation(current.getSpeciesSlug())
         &&WaterMovementGoal.restSurfaceAt(current,current.blockPosition())-current.getY()<1.8D)
      throw new IllegalStateException(current.getSpeciesSlug()+" slept closer than two blocks to the water surface");
     if(SpeciesBehaviorProfiles.usesSwimmingSleepAnimation(current.getSpeciesSlug())){
      double expectedY=current.getSpeciesSlug().equals("magikarp")?109.0D:104.0D;
      if(Math.abs(current.getY()-expectedY)>.25D)
       throw new IllegalStateException(current.getSpeciesSlug()+" chose the wrong sleep anchor: y="+current.getY()+" expected="+expectedY);
     }
     if(!Double.isFinite(target)||error>.2D){
      depthStableTicks=0;
      if(++depthSettleTicks>20)
       throw new IllegalStateException(current.getSpeciesSlug()+" must settle at its safe species-specific water depth; target="+target+" y="+current.getY()+" error="+error);
     }else{
      if(++depthStableTicks>=5)depthSettleTicks=0;
     }
    }else{
     depthSettleTicks=0;depthStableTicks=0;
     boolean shouldHover=SpeciesBehaviorProfiles.hoversDuringNightRest(current.getSpeciesSlug());
     double support=current.naturalSurface();
     double restHeight=SpeciesBehaviorProfiles.nightRestHoverHeight(current.getSpeciesSlug());
     boolean settled=shouldHover?!current.isOnGround()&&Math.abs(current.getY()-support-restHeight)<=.2D:current.isOnGround();
     if(!settled){
      restStableTicks=0;
      if(++restSettleTicks>20)throw new IllegalStateException(current.getSpeciesSlug()+" did not settle into its approved night-rest pose within 20 client ticks; hover="+shouldHover+" onGround="+current.isOnGround()+" y="+current.getY()+" support="+support+" deltaY="+current.getDeltaMovement().y);
     }else if(++restStableTicks>=5){
      restSettleTicks=0;
     }
    }
   }else{
    lastSleepingPosition=null;restSettleTicks=0;restStableTicks=0;
   }
   double dx=current.getX()-mc.player.getX(),dz=current.getZ()-mc.player.getZ(),dy=current.getY()+current.getBbHeight()*.65-mc.player.getEyeY();
   mc.player.setYRot((float)(Math.toDegrees(Math.atan2(dz,dx))-90));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))));
   boolean nocturnal=SpeciesBehaviorProfiles.isNocturnal(current.getSpeciesSlug());
   if(nocturnal&&!nocturnalMidday&&activeSamples>=25){
    Files.writeString(Path.of("client-validation-night.txt"),"PASS NOCTURNAL ACTIVE "+current.getSpeciesSlug()+" stayed awake at night; rendered active samples="+activeSamples+"\n",StandardOpenOption.APPEND);
    nocturnalMidday=true;samples=0;sleepTick=0;depthSettleTicks=0;depthStableTicks=0;restSettleTicks=0;restStableTicks=0;eyes.clear();restingWings.clear();
    mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().setDayTime(6000));
   }else if(!waking&&samples>=25){
    if(SpeciesBehaviorProfiles.usesSwimmingSleepAnimation(current.getSpeciesSlug())
        &&swimMotion.values().stream().noneMatch(range->range[1]-range[0]>.005F||range[3]-range[2]>.005F||range[5]-range[4]>.005F))
     throw new IllegalStateException(current.getSpeciesSlug()+" swim animation did not move any body or fin bone during sleep");
    Screenshot.grab(mc.gameDirectory,(nocturnal?"midday-":"night-")+SPECIES.get(index)+".png",mc.getMainRenderTarget(),message->{});
    Files.writeString(Path.of("client-validation-night.txt"),(nocturnal?"PASS NOCTURNAL MIDDAY ":"PASS NIGHT RENDER ")+SPECIES.get(index)+" sleep clip="+current.getSelectedAnimationForValidation()+" closed-eye tracks="+eyes.keySet()+" samples="+samples+" grounded="+current.isOnGround()+"\n",StandardOpenOption.APPEND);
    waking=true;mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().setDayTime(nocturnal?13000:1000));
   }
   if(waking&&!current.isSleeping()){
    if(current.getSpeciesSlug().equals("gyarados")){
     double surface=WaterMovementGoal.restSurfaceAt(current,current.blockPosition());
     double target=surface-WaterMovementGoal.preferredSurfaceDepth(current);
     if(Double.isFinite(target)&&Math.abs(current.getY()-target)>.3D){
      if(++restSettleTicks>250)throw new IllegalStateException("Gyarados did not return to its daytime surface target");
      return;
     }
    }
    if(current.getSpeciesSlug().equals("gengar")&&!gengarWakeEyesObserved)return;
    if(++index>=SPECIES.size()){
     Files.writeString(Path.of("client-validation-night.txt"),"PASS NIGHT CLIENT: ordinary species slept at night; all five nocturnal species stayed active at night, slept from midday to dusk, rendered their native closed-eye poses, and woke at dusk.\n",StandardOpenOption.APPEND);
     mc.stop();return;
    }
    spawned=false;
   }
  }catch(Exception e){throw new IllegalStateException("Night client smoke failed",e);}
 }
 private static String firstClip(String slug,String... keys){
  for(String key:keys){String clip=SpeciesAssetIndex.clip(slug,key,key);if(clip!=null)return clip;}
  return null;
 }
}
