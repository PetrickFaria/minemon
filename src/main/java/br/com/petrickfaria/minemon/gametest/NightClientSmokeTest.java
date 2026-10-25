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
 private static final List<String> SPECIES=List.of("bulbasaur","victreebel","venusaur","snorlax","charizard","scyther","dragonite","pidgey","pidgeotto","pidgeot","fearow","gastly","haunter","gengar","koffing","weezing","butterfree","beedrill","venomoth","parasect","kakuna","tangela","poliwag","growlithe","magikarp","gyarados","lapras","dratini","omanyte");
 private static boolean opened,setup,spawned,waking;
 private static int ticks,index,samples,sleepTick,depthSettleTicks,depthStableTicks;
 private static volatile UUID entity;
 private static final Map<String,float[]> eyes=new HashMap<>();
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
    MinemonRenderer.validationObserver=(mob,model)->{
     if(entity==null||!mob.getUUID().equals(entity)||!mob.isSleeping())return;
     if(!Objects.equals(mob.getSelectedAnimationForValidation(),SpeciesDefinitions.get(mob.getSpeciesSlug()).clips().get("night_sleep")))return;
     if(sleepTick==0){sleepTick=mob.tickCount;return;}
     if(mob.tickCount-sleepTick<12)return;
     // Read the generated pose, whose eye tracks hold the species' native blink closure.
     var d=SpeciesDefinitions.get(mob.getSpeciesSlug());
     try(var stream=NightClientSmokeTest.class.getResourceAsStream("/"+d.animation().replace("assets/minemon/","assets/minemon_gen1/"))){
      var animations=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject().getAsJsonObject("animations");
      String blink=d.clips().get("blink");
      {
       var sleepBones=animations.getAsJsonObject(d.clips().get("night_sleep")).getAsJsonObject("bones");
       java.util.Set<String> eyeNames=blink!=null&&animations.has(blink)?animations.getAsJsonObject(blink).getAsJsonObject("bones").keySet():sleepBones.keySet().stream().filter(n->n.contains("eye")||n.contains("pupil")).collect(java.util.stream.Collectors.toSet());
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
    spawned=true;ticks=0;samples=0;sleepTick=0;depthSettleTicks=0;depthStableTicks=0;waking=false;eyes.clear();
    mc.getSingleplayerServer().execute(()->{
     var level=mc.getSingleplayerServer().overworld();if(entity!=null){var old=level.getEntity(entity);if(old!=null)old.discard();}
     var mob=ModEntities.SPECIES.get(SPECIES.get(index)).get().create(level);
     boolean aquatic=SpeciesBehaviorProfiles.waterCapable(mob.getSpeciesSlug());
     boolean air=SpeciesBehaviorProfiles.canFly(mob.getSpeciesSlug());
     if(aquatic)for(int x=-6;x<=6;x++)for(int z=0;z<=10;z++)for(int y=101;y<=110;y++)
      level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState());
     mob.moveTo(0,aquatic?104:air?107:101,5,180,0);
     if(SpeciesBehaviorProfiles.profile(mob.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.GROUND_FLIGHT){
      var tag=new net.minecraft.nbt.CompoundTag();mob.addAdditionalSaveData(tag);tag.putString("NaturalBehaviorPhase","AIR");mob.readAdditionalSaveData(tag);
     }
     level.addFreshEntity(mob);entity=mob.getUUID();level.setDayTime(18000);
     double distance=switch(mob.getSpeciesSlug()){
      case "venusaur"->7;case "snorlax","charizard","dragonite"->6;case "fearow"->9;
      case "victreebel","scyther","weezing"->5;case "pidgeotto","haunter","butterfree","beedrill","parasect"->4;
      case "pidgey","gastly","kakuna"->2.5;case "gyarados","lapras"->8;default->3;
     };
     var player=level.getServer().getPlayerList().getPlayers().get(0);player.teleportTo(level,0,101,5-distance,0,0);
    });return;
   }
   ticks++;if(ticks>450)throw new IllegalStateException("Night client timeout: "+SPECIES.get(index)+" samples="+samples);
   MinemonCreature current=null;for(var e:mc.level.entitiesForRendering())if(e.getUUID().equals(entity)&&e instanceof MinemonCreature m){current=m;break;}
   if(current==null)return;
   boolean waterRest=SpeciesBehaviorProfiles.waterCapable(current.getSpeciesSlug())&&current.isInWaterOrBubble();
   if(current.isSleeping()){
    if(waterRest){
     double target=WaterMovementGoal.waterRestY(current);
     double error=Math.abs(current.getY()-target);
     if(!Double.isFinite(target)||error>.2D){
      depthStableTicks=0;
      if(++depthSettleTicks>20)
       throw new IllegalStateException(current.getSpeciesSlug()+" must settle at its safe species-specific water depth; target="+target+" y="+current.getY()+" error="+error);
     }else{
      if(++depthStableTicks>=5)depthSettleTicks=0;
     }
    }else{
     depthSettleTicks=0;depthStableTicks=0;
     boolean shouldLand=!SpeciesBehaviorProfiles.hoversDuringNightRest(current.getSpeciesSlug());
     if(shouldLand&&!current.isOnGround())throw new IllegalStateException(current.getSpeciesSlug()+" must sleep on the ground");
     if(!shouldLand&&(current.isOnGround()||Math.abs(current.getY()-current.naturalSurface()-1.0D)>.2D))
      throw new IllegalStateException(current.getSpeciesSlug()+" must keep its approved safe hover sleep pose");
    }
   }
   double dx=current.getX()-mc.player.getX(),dz=current.getZ()-mc.player.getZ(),dy=current.getY()+current.getBbHeight()*.65-mc.player.getEyeY();
   mc.player.setYRot((float)(Math.toDegrees(Math.atan2(dz,dx))-90));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))));
   if(!waking&&samples>=25){
    Screenshot.grab(mc.gameDirectory,"night-"+SPECIES.get(index)+".png",mc.getMainRenderTarget(),message->{});
    Files.writeString(Path.of("client-validation-night.txt"),"PASS NIGHT RENDER "+SPECIES.get(index)+" sleep clip="+current.getSelectedAnimationForValidation()+" closed-eye tracks="+eyes.keySet()+" samples="+samples+" grounded="+current.isOnGround()+"\n",StandardOpenOption.APPEND);
    waking=true;mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().setDayTime(1000));
   }
   if(waking&&!current.isSleeping()){
    if(++index>=SPECIES.size()){
     Files.writeString(Path.of("client-validation-night.txt"),"PASS NIGHT CLIENT: all "+SPECIES.size()+" species slept using production night AI, rendered held native closed-eye poses and woke at dawn.\n",StandardOpenOption.APPEND);
     mc.stop();return;
    }
    spawned=false;
   }
  }catch(Exception e){throw new IllegalStateException("Night client smoke failed",e);}
 }
}
