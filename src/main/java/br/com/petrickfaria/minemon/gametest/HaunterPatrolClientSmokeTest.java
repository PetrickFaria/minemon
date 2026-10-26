package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.client.MinemonRenderer;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import java.nio.file.*;
import java.util.UUID;

/** Thirty seconds of unforced production patrol, including a wall with an opening. */
final class HaunterPatrolClientSmokeTest {
 private static boolean opened,setup;
 private static volatile UUID entity;
 private static int ticks,rendered;
 private static Vec3 origin,last;
 private static float lastYaw,stationaryTurn,maxStationaryTurn;
 private static double maxDistance;
 private static int stationaryTicks,maxStationaryTicks;
 static void tick(TickEvent.ClientTickEvent event){
  var mc=Minecraft.getInstance();
  try{
   if(!opened&&mc.screen instanceof TitleScreen){
    opened=true;Files.writeString(Path.of("client-validation-patrol.txt"),"Haunter production navigation: 600 client ticks, no scripted velocity or yaw\n");
    var file=Path.of("saves/minemon-validation/level.dat").toFile();var tag=net.minecraft.nbt.NbtIo.readCompressed(file);
    tag.getCompound("Data").putBoolean("confirmedExperimentalSettings",true);net.minecraft.nbt.NbtIo.writeCompressed(tag,file);
    Object flows=Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);
    for(var method:flows.getClass().getMethods())if(method.getName().equals("loadLevel")){
     if(method.getParameterCount()==2&&method.getParameterTypes()[1]==String.class){method.invoke(flows,mc.screen,"minemon-validation");break;}
     if(method.getParameterCount()==1&&method.getParameterTypes()[0]==String.class){method.invoke(flows,"minemon-validation");break;}
    }
    MinemonRenderer.validationInput=null;
    MinemonRenderer.validationObserver=(mob,model)->{if(entity!=null&&entity.equals(mob.getUUID()))rendered++;};
   }
   if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||mc.screen!=null)return;
   if(!setup){
    setup=true;mc.getSingleplayerServer().execute(()->{
     var level=mc.getSingleplayerServer().overworld();level.setDayTime(1000);
     level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());
     for(int x=-30;x<=30;x++)for(int z=-22;z<=38;z++){
      level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
      for(int y=101;y<=112;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
     }
     for(int z=-5;z<=22;z++)if(z<6||z>10)for(int y=101;y<=108;y++)
      level.setBlockAndUpdate(new BlockPos(12,y,z),Blocks.STONE.defaultBlockState());
     var mob=ModEntities.SPECIES.get("haunter").get().create(level);mob.moveTo(0,103,8,-90,0);
     var saved=new CompoundTag();mob.addAdditionalSaveData(saved);saved.putInt("NaturalBehaviorStartDelay",0);mob.readAdditionalSaveData(saved);
     level.addFreshEntity(mob);entity=mob.getUUID();
     var player=level.getServer().getPlayerList().getPlayers().get(0);player.setGameMode(GameType.SPECTATOR);player.teleportTo(level,0,103,3,0,0);
    });return;
   }
   MinemonCreature current=null;
   for(var e:mc.level.entitiesForRendering())if(entity!=null&&e.getUUID().equals(entity)&&e instanceof MinemonCreature m){current=m;break;}
   if(current==null)return;
   ticks++;
   Vec3 position=current.position();
   if(origin==null){origin=position;last=position;lastYaw=current.getYRot();}
   maxDistance=Math.max(maxDistance,position.subtract(origin).horizontalDistanceSqr());
   double horizontal=position.subtract(last).horizontalDistanceSqr();
   if(horizontal<.0001D){
    stationaryTicks++;stationaryTurn+=Math.abs(Mth.wrapDegrees(current.getYRot()-lastYaw));
   }else{stationaryTicks=0;stationaryTurn=0;}
   maxStationaryTicks=Math.max(maxStationaryTicks,stationaryTicks);maxStationaryTurn=Math.max(maxStationaryTurn,stationaryTurn);
   last=position;lastYaw=current.getYRot();
   if(ticks%20==0){
    Files.writeString(Path.of("client-validation-patrol.txt"),"tick="+ticks+" pos="+position+" yaw="+lastYaw+" nav="+current.getNavigation().getTargetPos()+" stationary="+stationaryTicks+"\n",StandardOpenOption.APPEND);
    Screenshot.grab(mc.gameDirectory,String.format(java.util.Locale.ROOT,"haunter-patrol-%03d.png",ticks/20),mc.getMainRenderTarget(),message->{});
   }
   // Follow with the spectator camera only. No mob direction, path, velocity,
   // goals, collision or animation input is overridden in this scenario.
   if(ticks%10==0){
    double px=current.getX(),py=current.getY(),pz=current.getZ();
    mc.getSingleplayerServer().execute(()->{
     var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
     player.teleportTo(mc.getSingleplayerServer().overworld(),px,py,pz-5,0,0);
    });
   }
   double dx=current.getX()-mc.player.getX(),dz=current.getZ()-mc.player.getZ();
   double dy=current.getY()+current.getBbHeight()*.5-mc.player.getEyeY();
   mc.player.setYRot((float)(Math.toDegrees(Math.atan2(dz,dx))-90));
   mc.player.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))));
   if(ticks>=600){
    if(rendered<100||maxDistance<9.0D||maxStationaryTicks>=200||maxStationaryTurn>=360.0F)
     throw new IllegalStateException("Haunter patrol failed: rendered="+rendered+" maxDistanceSqr="+maxDistance+" maxStationaryTicks="+maxStationaryTicks+" stationaryTurn="+maxStationaryTurn);
    Files.writeString(Path.of("client-validation-patrol.txt"),"PASS HAUNTER PATROL: observed 600 real client ticks; rendered="+rendered+" maxDistanceSqr="+maxDistance+" longest stationary ticks="+maxStationaryTicks+" max stationary rotation="+maxStationaryTurn+"; 30 screenshots captured\n",StandardOpenOption.APPEND);
    mc.stop();
   }
  }catch(Exception e){throw new IllegalStateException("Haunter real-client patrol validation failed",e);}
 }
}
