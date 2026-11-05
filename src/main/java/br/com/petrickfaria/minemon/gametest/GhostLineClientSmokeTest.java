package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.client.MinemonRenderer;
import br.com.petrickfaria.minemon.client.MinemonModel;
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
 private static boolean opened,setup,sleepStarted,blinkChecked,validationComplete;
 private static boolean farViewRequested,farViewApplied;
 private static int farViewStartTick;
 private static int screenshotFlushTicks;
 private static boolean screenshotInFlight;
 private static final Set<String> writtenScreenshots=new HashSet<>();
 private static int ticks;
 private static volatile UUID gastlyId,haunterId,gengarId;
 private static final Set<String> captures=new HashSet<>();
 private static String pendingScreenshot;
 private static int pendingScreenshotDelay;
 private static float gengarSleepBobMin=Float.POSITIVE_INFINITY,gengarSleepBobMax=Float.NEGATIVE_INFINITY;
 private static boolean screenshotSlotAvailable(){return pendingScreenshot==null&&!screenshotInFlight;}
 private static void snap(Minecraft mc,String key){
  if(!captures.contains(key)&&screenshotSlotAvailable()){captures.add(key);pendingScreenshot=key;pendingScreenshotDelay=4;}
 }
 private static void snapNow(Minecraft mc,String key){
  if(!captures.contains(key)&&screenshotSlotAvailable()){captures.add(key);pendingScreenshot=key;pendingScreenshotDelay=0;}
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
  float pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
  player.setXRot(pitch);
  var server=mc.getSingleplayerServer();
  var playerId=player.getUUID();
  var mobId=mob.getUUID();
  float yaw=player.getYRot();
  mob.setYRot(180.0F);
  mob.setYBodyRot(180.0F);
  mob.setYHeadRot(180.0F);
  server.execute(()->{
   var authoritativePlayer=server.getPlayerList().getPlayer(playerId);
   if(authoritativePlayer!=null)authoritativePlayer.teleportTo(server.overworld(),px,py,pz,yaw,pitch);
   var authoritativeMob=server.overworld().getEntity(mobId);
   if(authoritativeMob instanceof net.minecraft.world.entity.LivingEntity living){
    living.setYRot(180.0F);living.setYBodyRot(180.0F);living.setYHeadRot(180.0F);
   }
  });
 }
 private static void faceFromAngle(Minecraft mc,MinemonCreature mob,double degrees){
  var player=mc.player;if(player==null)return;
  double angle=Math.toRadians(degrees),px=mob.getX()+Math.sin(angle)*4.0D,pz=mob.getZ()-Math.cos(angle)*4.0D;
  double py=mob.getY()-0.2D,dx=mob.getX()-px,dz=mob.getZ()-pz;
  double dy=mob.getY()+1.15D-player.getEyeY();
  player.setPos(px,py,pz);
  player.setYRot((float)(Math.toDegrees(Math.atan2(dz,dx))-90));
  player.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))));
  var server=mc.getSingleplayerServer();var playerId=player.getUUID();var mobId=mob.getUUID();float yaw=player.getYRot(),pitch=player.getXRot();
  server.execute(()->{
   var authoritativePlayer=server.getPlayerList().getPlayer(playerId);
   if(authoritativePlayer!=null)authoritativePlayer.teleportTo(server.overworld(),px,py,pz,yaw,pitch);
  });
 }
 private static double pointCameraAt(Minecraft mc,MinemonCreature mob){
  var player=mc.player;if(player==null)return -1.0D;
  double dx=mob.getX()-player.getX(),dz=mob.getZ()-player.getZ();
  double dy=mob.getY()+0.5D-player.getEyeY();
  float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90.0D);
  float pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
  player.setYRot(yaw);player.setYHeadRot(yaw);player.setYBodyRot(yaw);player.setXRot(pitch);
  var direction=new net.minecraft.world.phys.Vec3(dx,dy,dz).normalize();
  return player.getLookAngle().normalize().dot(direction);
 }
 private static void prepareFarGastlyView(Minecraft mc,MinemonCreature mob){
  var server=mc.getSingleplayerServer();if(server==null)return;
  // Keep the camera at the established near viewpoint and move the fixture entity
  // into the loaded 50-block corridor. A player teleport beyond the entity's
  // current chunk is reverted by the integrated server during this smoke test.
  face(mc,mob);
  farViewRequested=true;farViewStartTick=ticks;farViewApplied=false;
  var mobId=mob.getUUID();
  server.execute(()->{
   var authoritative=server.overworld().getEntity(mobId);
   if(authoritative instanceof MinemonCreature gastly){
    gastly.moveTo(0.0D,101.0D,54.0D,180.0F,0.0F);
    gastly.setYBodyRot(180.0F);gastly.setYHeadRot(180.0F);
    mc.execute(()->farViewApplied=true);
   }
  });
 }
 static void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END)return;
  Minecraft mc=Minecraft.getInstance();
  try{
   if(pendingScreenshot!=null){
    if(pendingScreenshotDelay>0)pendingScreenshotDelay--;
    else{
     String view=pendingScreenshot;pendingScreenshot=null;
     if(view.equals("gastly-50-block")){
      var mob=find(mc,gastlyId);
      if(mob==null)throw new IllegalStateException("Gastly left the client render set before its 50-block screenshot");
      pointCameraAt(mc,mob);
     }
     if(view.equals("haunter-blink")){
      var mob=find(mc,haunterId);
      if(mob==null||!mob.isBlinkingForRender())
       throw new IllegalStateException("Haunter blink ended before its closed-eye screenshot was captured");
     }else if(view.equals("gengar-blink")){
      var mob=find(mc,gengarId);
      if(mob==null||!mob.isBlinkingForRender())
       throw new IllegalStateException("Gengar blink ended before its closed-eye screenshot was captured");
     }
     Screenshot.grab(mc.gameDirectory,"ghost-line-1.74-"+view+".png",mc.getMainRenderTarget(),m->{screenshotInFlight=false;writtenScreenshots.add(view);System.out.println("[GHOST LINE] screenshot "+view+": "+m);});
     screenshotInFlight=true;
    }
   }
   if(validationComplete){
    screenshotFlushTicks++;
    Path screenshotDir=mc.gameDirectory.toPath().resolve("screenshots");
    boolean allScreenshots=writtenScreenshots.size()==14&&List.of("gastly-front","gastly-no-tongue","gastly-right","gastly-back","gastly-left","gastly-50-block","haunter-front-a","haunter-front-b","haunter-blink",
      "gengar-walk","gengar-blink","gengar-mouth-teeth-fixed","gengar-sleep-half-sunk-a","gengar-sleep-half-sunk-b")
      .stream().allMatch(key->writtenScreenshots.contains(key)&&Files.isRegularFile(screenshotDir.resolve("ghost-line-1.74-"+key+".png")));
    if(allScreenshots){mc.stop();return;}
    if(screenshotFlushTicks>=200)throw new IllegalStateException("Timed out waiting for all 14 real-client screenshots to be written");
    return;
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
     for(int x=-8;x<=8;x++)for(int z=-48;z<=64;z++){
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
   if(ticks>=8&&!captures.contains("gastly-no-tongue")&&MinemonModel.gastlyTongueHiddenForValidation(gastly)){
    face(mc,gastly);snapNow(mc,"gastly-no-tongue");
   }
   if(ticks>=12&&screenshotSlotAvailable()&&!captures.contains("gastly-right")){faceFromAngle(mc,gastly,90);snapNow(mc,"gastly-right");}
   if(ticks>=20&&screenshotSlotAvailable()&&!captures.contains("gastly-back")){faceFromAngle(mc,gastly,180);snapNow(mc,"gastly-back");}
   if(ticks>=28&&screenshotSlotAvailable()&&!captures.contains("gastly-left")){faceFromAngle(mc,gastly,270);snapNow(mc,"gastly-left");}
   if(ticks>=36&&screenshotSlotAvailable()&&!captures.contains("gastly-50-block")){
    if(!farViewRequested)prepareFarGastlyView(mc,gastly);
    else if(farViewApplied){
     double playerDistance=Math.sqrt(gastly.distanceToSqr(mc.player.getX(),mc.player.getY(),mc.player.getZ()));
     double cameraDistance=mc.gameRenderer.getMainCamera().getPosition().distanceTo(gastly.position());
     double renderedCameraDistance=br.com.petrickfaria.minemon.client.GastlyCoreRenderHook.maxCameraDistanceForValidation();
     double cameraAlignment=pointCameraAt(mc,gastly);
     if(playerDistance>=49.0D&&cameraDistance>=49.0D&&renderedCameraDistance>=49.0D&&cameraAlignment>=0.98D){
      System.out.println("[GHOST LINE] 50-block setup player="+mc.player.position()+" gastly="+gastly.position()
          +" playerDistance="+playerDistance+" cameraDistance="+cameraDistance
          +" renderedCameraDistance="+renderedCameraDistance+" cameraAlignment="+cameraAlignment);
      snap(mc,"gastly-50-block");
     }else if(ticks-farViewStartTick>40)
      throw new IllegalStateException("Server/client render hook did not observe Gastly at the requested 50-block view; playerDistance="
          +playerDistance+" cameraDistance="+cameraDistance+" renderedCameraDistance="+renderedCameraDistance
          +" cameraAlignment="+cameraAlignment);
    }
   }
   if(ticks>=25&&!captures.contains("haunter-front-a")){face(mc,haunter);snap(mc,"haunter-front-a");}
   if(ticks>=75&&!captures.contains("haunter-front-b")){face(mc,haunter);snap(mc,"haunter-front-b");}
   if(ticks>=100&&haunter.isBlinkingForRender()&&MinemonModel.ghostEyesClosedForValidation(haunter)
       &&screenshotSlotAvailable()&&!captures.contains("haunter-blink")){
    face(mc,haunter);snapNow(mc,"haunter-blink");
   }
   if(ticks>=100&&gengar.isBlinkingForRender()&&MinemonModel.ghostEyesClosedForValidation(gengar)
       &&screenshotSlotAvailable()&&!captures.contains("gengar-blink")){
    face(mc,gengar);snapNow(mc,"gengar-blink");
   }
   if(ticks>=90&&ticks<145){
    mc.getSingleplayerServer().execute(()->{
     var authoritative=mc.getSingleplayerServer().overworld().getEntity(gengarId);
     if(authoritative instanceof MinemonCreature g){g.setOnGround(true);g.setDeltaMovement(0.055D,0,0);}
    });
   }
   if(ticks>=115&&ticks<145&&!captures.contains("gengar-walk")){
    if(!"animation.gengar.ground_walk".equals(readClip(gengar)))
     throw new IllegalStateException("Gengar walking screenshot was not captured during ground_walk: "+readClip(gengar));
    face(mc,gengar);snap(mc,"gengar-walk");
   }
   if(ticks>=150){
    mc.getSingleplayerServer().execute(()->{
     var authoritative=mc.getSingleplayerServer().overworld().getEntity(gengarId);
     if(authoritative instanceof MinemonCreature g)g.setDeltaMovement(0,0,0);
    });
   }
   if(ticks>=160&&!captures.contains("gengar-mouth-teeth-fixed")
       &&MinemonModel.gengarTeethMouthForValidation(gengar)){
    face(mc,gengar);snap(mc,"gengar-mouth-teeth-fixed");
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
   if(sleepStarted){
    float bob=MinemonModel.gengarSleepBobForValidation(gengar);
    if(Float.isFinite(bob)){gengarSleepBobMin=Math.min(gengarSleepBobMin,bob);gengarSleepBobMax=Math.max(gengarSleepBobMax,bob);}
   }
   if(ticks>=700&&!blinkChecked){
    blinkChecked=true;
    int haunterBlink=haunter.blinkEventsForValidation(),gengarBlink=gengar.blinkEventsForValidation();
    if(haunterBlink<2||gengarBlink<2||haunter.lastBlinkIntervalForValidation()<100||haunter.lastBlinkIntervalForValidation()>300
        ||gengar.lastBlinkIntervalForValidation()<100||gengar.lastBlinkIntervalForValidation()>300)
     throw new IllegalStateException("Ghost blink interval check failed: Haunter events="+haunterBlink+" interval="+haunter.lastBlinkIntervalForValidation()+" Gengar events="+gengarBlink+" interval="+gengar.lastBlinkIntervalForValidation());
   }
   if(ticks>=800&&!validationComplete){
    if(br.com.petrickfaria.minemon.client.GastlyCoreRenderHook.redrawsForValidation()==0)
     throw new IllegalStateException("Gastly was never redrawn above particles during the real-client test");
    if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.largeSpawnsForValidation()==0
        ||br.com.petrickfaria.minemon.client.GastlySmokeParticle.smallSpawnsForValidation()==0)
     throw new IllegalStateException("Gastly's large and small particles were not both rendered during camera-angle validation");
    if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.farRendersForValidation()==0)
     throw new IllegalStateException("Gastly aura particles were not rendered beyond the vanilla particle cutoff during the 50-block camera check; render hook maximum camera distance="
       +br.com.petrickfaria.minemon.client.GastlyCoreRenderHook.maxCameraDistanceForValidation());
    if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxRenderDistanceForValidation()<49.0D)
     throw new IllegalStateException("Gastly aura did not reach the 50-block render boundary; maximum observed distance="
       +br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxRenderDistanceForValidation());
    for(String key:List.of("gastly-front","gastly-no-tongue","gastly-right","gastly-back","gastly-left","gastly-50-block","haunter-front-a","haunter-front-b","haunter-blink",
            "gengar-walk","gengar-blink","gengar-mouth-teeth-fixed","gengar-sleep-half-sunk-a","gengar-sleep-half-sunk-b"))
     if(!captures.contains(key))throw new IllegalStateException("Missing real-client ghost screenshot: "+key);
    if(!MinemonModel.gastlyTongueHiddenForValidation(gastly))throw new IllegalStateException("Gastly tongue expression was not suppressed");
    if(!MinemonModel.gengarTeethMouthForValidation(gengar))throw new IllegalStateException("Gengar teeth mouth was not selected");
    if(gengarSleepBobMax-gengarSleepBobMin<0.2F)
     throw new IllegalStateException("Gengar sleep body bob did not vary enough: "+gengarSleepBobMin+".."+gengarSleepBobMax);
    Files.writeString(Path.of("client-validation-ghost-line.txt"),
      "PASS GHOST LINE CLIENT: real Minecraft render captured Gastly aura/core from four camera angles and at 50 blocks with large and small particles and tongue suppressed, visible Haunter and Gengar closed-eye blinks, Gengar lowered-arm gait, fixed teeth mouth, and less-sunk bobbing sleep.\n"
      +"Haunter blink events="+haunter.blinkEventsForValidation()+", last interval="+haunter.lastBlinkIntervalForValidation()+" ticks; Gengar blink events="+gengar.blinkEventsForValidation()+", last interval="+gengar.lastBlinkIntervalForValidation()+" ticks.\n"
      +"Haunter hand phase lag="+br.com.petrickfaria.minemon.entity.GhostLineAnimationRules.HAUNTER_HAND_LAG_TICKS+" ticks (2.5 seconds); Gengar ground walk clip="+readClip(gengar)+"; measured sleep bob range="+gengarSleepBobMin+".."+gengarSleepBobMax+".\n");
    validationComplete=true;
    System.out.println("[GHOST LINE] PASS: runtime checks complete; waiting for all 14 screenshots to be written.");
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
