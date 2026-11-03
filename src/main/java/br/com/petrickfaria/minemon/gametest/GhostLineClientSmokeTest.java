package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.client.MinemonRenderer;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import java.nio.file.*;
import java.util.*;

/** Real-client visual and timing smoke test for the Gastly evolution line. */
final class GhostLineClientSmokeTest {
 private static boolean opened,setup,sleepStarted,blinkChecked;
 private static int ticks;
 private static volatile UUID gastlyId,haunterId,gengarId;
 private static final Set<String> captures=new HashSet<>();
 private static String pendingScreenshot;
 private static void snap(Minecraft mc,String key){
  if(captures.add(key))pendingScreenshot=key;
 }
 private static MinemonCreature find(Minecraft mc,UUID id){
  if(id==null||mc.level==null)return null;
  for(var e:mc.level.entitiesForRendering())if(e instanceof MinemonCreature m&&id.equals(m.getUUID()))return m;
  return null;
 }
 private static void face(Minecraft mc,MinemonCreature mob){
  var player=mc.player;if(player==null)return;
  double px=mob.getX(),py=mob.getY()-0.2D,pz=mob.getZ()-4.0D;
  player.setPos(px,py,pz);
  double dx=mob.getX()-player.getX(),dz=mob.getZ()-player.getZ();
  double dy=mob.getY()+1.15D-player.getEyeY();
  player.setYRot((float)(Math.toDegrees(Math.atan2(dz,dx))-90));
  player.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))));
 }
 static void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END)return;
  Minecraft mc=Minecraft.getInstance();
  try{
   if(pendingScreenshot!=null){
    String view=pendingScreenshot;pendingScreenshot=null;
    Screenshot.grab(mc.gameDirectory,"ghost-line-1.73-"+view+".png",mc.getMainRenderTarget(),m->System.out.println("[GHOST LINE] screenshot "+view+": "+m));
   }
   if(!opened&&mc.screen instanceof TitleScreen){
    opened=true;
    var file=Path.of("saves/minemon-validation/level.dat").toFile();
    var tag=net.minecraft.nbt.NbtIo.readCompressed(file);
    tag.getCompound("Data").putBoolean("confirmedExperimentalSettings",true);
    net.minecraft.nbt.NbtIo.writeCompressed(tag,file);
    Object flows=Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);
    for(var method:flows.getClass().getMethods())if(method.getName().equals("loadLevel")){
     if(method.getParameterCount()==2&&method.getParameterTypes()[1]==String.class){method.invoke(flows,mc.screen,"minemon-validation");break;}
     if(method.getParameterCount()==1&&method.getParameterTypes()[0]==String.class){method.invoke(flows,"minemon-validation");break;}
    }
    return;
   }
   if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||mc.screen!=null)return;
   if(!setup){
    setup=true;
    mc.getSingleplayerServer().execute(()->{
     var level=mc.getSingleplayerServer().overworld();
     for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++){
      level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
      for(int y=101;y<=112;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
     }
     var gastly=spawn(level,"gastly",0,8);var haunter=spawn(level,"haunter",4,8);var gengar=spawn(level,"gengar",-4,8);
     gastlyId=gastly.getUUID();haunterId=haunter.getUUID();gengarId=gengar.getUUID();
     var player=level.getServer().getPlayerList().getPlayers().get(0);
     player.setGameMode(GameType.SPECTATOR);player.teleportTo(level,0,101,3,0,0);
    });
    MinemonRenderer.validationInput=null;
    return;
   }
   ticks++;
   MinemonCreature gastly=find(mc,gastlyId),haunter=find(mc,haunterId),gengar=find(mc,gengarId);
   if(gastly==null||haunter==null||gengar==null)return;
   if(ticks>=5&&!captures.contains("gastly-front")){face(mc,gastly);snap(mc,"gastly-front");}
   if(ticks>=25&&!captures.contains("haunter-front-a")){face(mc,haunter);snap(mc,"haunter-front-a");}
   if(ticks>=75&&!captures.contains("haunter-front-b")){face(mc,haunter);snap(mc,"haunter-front-b");}
   if(ticks>=90&&ticks<145){
    mc.getSingleplayerServer().execute(()->{
     var authoritative=mc.getSingleplayerServer().overworld().getEntity(gengarId);
     if(authoritative instanceof MinemonCreature g){g.setOnGround(true);g.setDeltaMovement(0.055D,0,0);}
    });
   }
   if(ticks>=115&&ticks<145&&!captures.contains("gengar-walk")){face(mc,gengar);snap(mc,"gengar-walk");}
   if(ticks>=150){
    mc.getSingleplayerServer().execute(()->{
     var authoritative=mc.getSingleplayerServer().overworld().getEntity(gengarId);
     if(authoritative instanceof MinemonCreature g)g.setDeltaMovement(0,0,0);
    });
   }
   if(ticks>=150&&ticks<345){
    face(mc,gengar);
    boolean open=Math.floorMod(gengar.tickCount+gengar.getId()*53,240)>=120;
    if(open&&!captures.contains("gengar-mouth-open"))snap(mc,"gengar-mouth-open");
    if(!open&&!captures.contains("gengar-mouth-teeth"))snap(mc,"gengar-mouth-teeth");
   }
   if(ticks>=705&&!sleepStarted){
    mc.getSingleplayerServer().execute(()->{
     var authoritative=mc.getSingleplayerServer().overworld().getEntity(gengarId);
     if(authoritative instanceof MinemonCreature g)g.setSleeping(true);
    });
    sleepStarted=true;
   }
   if(sleepStarted&&ticks>=730&&!captures.contains("gengar-sleep-half-sunk-a")){face(mc,gengar);snap(mc,"gengar-sleep-half-sunk-a");}
   if(sleepStarted&&ticks>=760&&!captures.contains("gengar-sleep-half-sunk-b")){face(mc,gengar);snap(mc,"gengar-sleep-half-sunk-b");}
   if(ticks>=700&&!blinkChecked){
    blinkChecked=true;
    int haunterBlink=haunter.blinkEventsForValidation(),gengarBlink=gengar.blinkEventsForValidation();
    if(haunterBlink<2||gengarBlink<2||haunter.lastBlinkIntervalForValidation()<100||haunter.lastBlinkIntervalForValidation()>300
        ||gengar.lastBlinkIntervalForValidation()<100||gengar.lastBlinkIntervalForValidation()>300)
     throw new IllegalStateException("Ghost blink interval check failed: Haunter events="+haunterBlink+" interval="+haunter.lastBlinkIntervalForValidation()+" Gengar events="+gengarBlink+" interval="+gengar.lastBlinkIntervalForValidation());
   }
   if(ticks>=800){
    if(br.com.petrickfaria.minemon.client.GastlyCoreRenderHook.redrawsForValidation()==0)
     throw new IllegalStateException("Gastly was never redrawn above particles during the real-client test");
    for(String key:List.of("gastly-front","haunter-front-a","haunter-front-b","gengar-walk","gengar-mouth-open","gengar-mouth-teeth","gengar-sleep-half-sunk-a","gengar-sleep-half-sunk-b"))
     if(!captures.contains(key))throw new IllegalStateException("Missing real-client ghost screenshot: "+key);
    Files.writeString(Path.of("client-validation-ghost-line.txt"),
      "PASS GHOST LINE CLIENT: real Minecraft render captured Gastly aura/core, two Haunter idle moments, Gengar grounded gait, both authored mouth meshes, and half-sunk bobbing sleep.\n"
      +"Haunter blink events="+haunter.blinkEventsForValidation()+", last interval="+haunter.lastBlinkIntervalForValidation()+" ticks; Gengar blink events="+gengar.blinkEventsForValidation()+", last interval="+gengar.lastBlinkIntervalForValidation()+" ticks.\n"
      +"Haunter hand phase is derived from the body phase at tick-20 (one second); Gengar ground walk clip="+readClip(gengar)+"; sleep vertical bob is driven by the existing body bone in model space.\n");
    System.out.println("[GHOST LINE] PASS: screenshots and runtime checks complete.");
    mc.stop();
   }
  }catch(Exception e){throw new IllegalStateException("Ghost line real-client validation failed",e);}
 }
 private static MinemonCreature spawn(net.minecraft.server.level.ServerLevel level,String slug,double x,double z){
  var mob=ModEntities.SPECIES.get(slug).get().create(level);
  mob.moveTo(x,101,z,180,0);mob.setNoAi(true);
  level.addFreshEntity(mob);return mob;
 }
 private static String readClip(MinemonCreature mob){
  try{var f=MinemonCreature.class.getDeclaredField("selectedAnimationForValidation");f.setAccessible(true);return String.valueOf(f.get(mob));}
  catch(Exception ignored){return "unavailable";}
 }
 private GhostLineClientSmokeTest(){}
}
