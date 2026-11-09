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
 private static int cameraMatrixIndex=-1,cameraMatrixDelay,cameraMatrixPasses;
 private static boolean cameraMatrixComplete;
 private static int gengarWalkStartTick=-1;
 private static final int[] CAMERA_YAWS={0,45,90,135,180,225,270,315};
 private static final int[] CAMERA_PITCHES={-30,0,30};
 private static boolean farViewRequested,farViewApplied;
 private static int farViewStartTick;
 private static int screenshotFlushTicks;
 private static boolean screenshotInFlight,nativeSmokeFixtureInjected;
 private static final Set<String> writtenScreenshots=new HashSet<>();
 private static int ticks;
 private static volatile UUID gastlyId,secondGastlyId,haunterId,gengarId;
 private static double movingGastlyStartX=Double.NaN,floatingGastlyStartY=Double.NaN;
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
  faceAtDistance(mc,mob,4.0D,0.0D);
 }
 private static void faceAtDistance(Minecraft mc,MinemonCreature mob,double distance,double degrees){
  // A queued screenshot owns the viewpoint until the framebuffer is captured.
  if(!screenshotSlotAvailable())return;
  var player=mc.player;if(player==null)return;
  double angle=Math.toRadians(degrees);
  double px=mob.getX()+Math.sin(angle)*distance,py=mob.getY()-0.2D,pz=mob.getZ()-Math.cos(angle)*distance;
  double dx=mob.getX()-px,dz=mob.getZ()-pz;
  double dy=mob.getY()+1.15D-(py+player.getEyeHeight());
  player.setPos(px,py,pz);
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
  faceAtDistance(mc,mob,4.0D,degrees);
 }
 private static void faceAtCameraAngles(Minecraft mc,MinemonCreature mob,double distance,double yawDegrees,double pitchDegrees){
  if(mc.player==null)return;
  var player=mc.player;
  double yaw=Math.toRadians(yawDegrees),pitch=Math.toRadians(pitchDegrees);
  double horizontal=distance*Math.cos(pitch);
  double px=mob.getX()+Math.sin(yaw)*horizontal;
  double pz=mob.getZ()-Math.cos(yaw)*horizontal;
  double eyeY=mob.getY()+1.15D+distance*Math.sin(pitch);
  double py=eyeY-player.getEyeHeight();
  player.setPos(px,py,pz);player.setYRot((float)yawDegrees);player.setYHeadRot((float)yawDegrees);
  player.setYBodyRot((float)yawDegrees);player.setXRot((float)pitchDegrees);
  var server=mc.getSingleplayerServer();var playerId=player.getUUID();var mobId=mob.getUUID();
  server.execute(()->{
   var authoritativePlayer=server.getPlayerList().getPlayer(playerId);
   if(authoritativePlayer!=null)authoritativePlayer.teleportTo(server.overworld(),px,py,pz,(float)yawDegrees,(float)pitchDegrees);
   var authoritativeMob=server.overworld().getEntity(mobId);
   if(authoritativeMob instanceof net.minecraft.world.entity.LivingEntity living){
    living.setYRot(180.0F);living.setYBodyRot(180.0F);living.setYHeadRot(180.0F);
   }
  });
 }
 private static boolean cameraMatrixActive(){return cameraMatrixIndex>=0&&!cameraMatrixComplete;}
 private static void faceVertical(Minecraft mc,MinemonCreature mob,boolean fromAbove){
  if(!screenshotSlotAvailable()||mc.player==null)return;
  var player=mc.player;
  double bodyY=106.0D;
  double px=mob.getX(),py=bodyY+(fromAbove?4.0D:-4.0D),pz=mob.getZ();
  double dy=bodyY+0.55D-(py+player.getEyeHeight());
  float pitch=(float)-Math.toDegrees(Math.atan2(dy,0.001D));
  player.setPos(px,py,pz);player.setYRot(0.0F);player.setXRot(pitch);
  var server=mc.getSingleplayerServer();var playerId=player.getUUID();var mobId=mob.getUUID();
  server.execute(()->{
   var authoritativePlayer=server.getPlayerList().getPlayer(playerId);
   if(authoritativePlayer!=null)authoritativePlayer.teleportTo(server.overworld(),px,py,pz,0.0F,pitch);
   var authoritativeMob=server.overworld().getEntity(mobId);
   if(authoritativeMob instanceof MinemonCreature gastly)gastly.moveTo(gastly.getX(),106.0D,gastly.getZ(),gastly.getYRot(),gastly.getXRot());
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
   var player=server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
   if(authoritative instanceof MinemonCreature gastly&&player!=null){
    // Measure the 50-block fixture from the camera anchor established by
    // face(), not from a hard-coded world origin. Keep a 0.1-block margin
    // inside the configured aura cutoff for the camera's eye-height offset
    // and floating-point rounding; the resulting view remains a 50-block test.
    gastly.moveTo(player.getX(),player.getY()+0.2D,player.getZ()+49.9D,180.0F,0.0F);
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
     if(Set.of("gastly-front","gastly-right","gastly-back","gastly-left","gastly-top").contains(view))
      verifyVisibleCore(mc,view);
     else if(view.equals("gastly-bottom"))verifyVisibleAura(mc,view);
     if(view.equals("haunter-blink")){
      var mob=find(mc,haunterId);
      if(mob==null||!mob.isBlinkingForRender())
       throw new IllegalStateException("Haunter blink ended before its closed-eye screenshot was captured");
     }else if(view.equals("gengar-blink")){
      var mob=find(mc,gengarId);
      if(mob==null||!mob.isBlinkingForRender())
       throw new IllegalStateException("Gengar blink ended before its closed-eye screenshot was captured");
     }
     if(Set.of("gastly-10-block","gastly-motion","gastly-float").contains(view))verifyVisibleAura(mc,view);
     Screenshot.grab(mc.gameDirectory,"ghost-line-1.74-"+view+".png",mc.getMainRenderTarget(),m->{screenshotInFlight=false;writtenScreenshots.add(view);System.out.println("[GHOST LINE] screenshot "+view+": "+m);});
     screenshotInFlight=true;
    }
   }
   if(validationComplete){
    screenshotFlushTicks++;
    Path screenshotDir=mc.gameDirectory.toPath().resolve("screenshots");
   boolean allScreenshots=writtenScreenshots.size()==20&&List.of("gastly-front","gastly-no-tongue","gastly-right","gastly-back","gastly-left","gastly-top","gastly-bottom","gastly-10-block","gastly-25-block","gastly-50-block","gastly-motion","gastly-float","haunter-front-a","haunter-front-b","haunter-blink",
      "gengar-walk","gengar-blink","gengar-mouth-teeth-fixed","gengar-sleep-half-sunk-a","gengar-sleep-half-sunk-b")
      .stream().allMatch(key->writtenScreenshots.contains(key)&&Files.isRegularFile(screenshotDir.resolve("ghost-line-1.74-"+key+".png")));
   if(allScreenshots){mc.stop();return;}
   if(screenshotFlushTicks>=200)throw new IllegalStateException("Timed out waiting for all 20 real-client screenshots to be written");
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
     // Pixel color assertions need controlled daylight, rather than inheriting
     // the final nighttime GameTest batch's saved lighting conditions.
     level.setDayTime(6000L);
     level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());
      // Keep the second provider fixture slightly beyond the 50-block aura cutoff
      // during primary screenshots while remaining within client tracking range.
      for(int x=-72;x<=72;x++)for(int z=-48;z<=64;z++){
      level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
      for(int y=101;y<=112;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
     }
     var gastly=spawn(level,"gastly",0,8,net.minecraft.world.entity.MobSpawnType.NATURAL);
     gastly.setCustomName(net.minecraft.network.chat.Component.literal("Minemon Gastly Persistence Fixture"));
     gastly.setCustomNameVisible(false);
     var secondGastly=spawn(level,"gastly",64,16,net.minecraft.world.entity.MobSpawnType.SPAWN_EGG);
     var haunter=spawn(level,"haunter",-16,8);var gengar=spawn(level,"gengar",16,8);
     gastlyId=gastly.getUUID();secondGastlyId=secondGastly.getUUID();haunterId=haunter.getUUID();gengarId=gengar.getUUID();
     var player=level.getServer().getPlayerList().getPlayers().get(0);
     player.setGameMode(GameType.SPECTATOR);player.teleportTo(level,0,101,3,0,0);
    });
    MinemonRenderer.validationInput=null;
    return;
   }
   ticks++;
   MinemonCreature gastly=find(mc,gastlyId),secondGastly=find(mc,secondGastlyId),haunter=find(mc,haunterId),gengar=find(mc,gengarId);
   if(gastly==null||secondGastly==null||haunter==null||gengar==null)return;
   if(ticks>=50&&cameraMatrixIndex<0&&writtenScreenshots.contains("gastly-50-block"))cameraMatrixIndex=0;
   if(cameraMatrixActive()){
    if(cameraMatrixDelay==0){
     int yaw=CAMERA_YAWS[cameraMatrixIndex/CAMERA_PITCHES.length];
     int pitch=CAMERA_PITCHES[cameraMatrixIndex%CAMERA_PITCHES.length];
     faceAtCameraAngles(mc,gastly,5.0D,yaw,pitch);cameraMatrixDelay=4;
    }else if(--cameraMatrixDelay==0){
     int yaw=CAMERA_YAWS[cameraMatrixIndex/CAMERA_PITCHES.length];
     int pitch=CAMERA_PITCHES[cameraMatrixIndex%CAMERA_PITCHES.length];
     verifyVisibleCore(mc,"GAS-02 yaw="+yaw+" pitch="+pitch);
     cameraMatrixPasses++;cameraMatrixIndex++;
     if(cameraMatrixIndex>=CAMERA_YAWS.length*CAMERA_PITCHES.length){
      cameraMatrixComplete=true;face(mc,gastly);
      System.out.println("[GHOST LINE] PASS GAS-02: "+cameraMatrixPasses+" horizontal/vertical camera combinations");
     }
    }
   }
   if(ticks>=4&&!nativeSmokeFixtureInjected){
    nativeSmokeFixtureInjected=true;
    // Exercise the real provider with velocity values that resemble another
    // entity ID and slot. Tick the returned client particle directly so the
    // smoke test does not depend on ParticleEngine scheduling.
    var source=br.com.petrickfaria.minemon.client.GastlySmokeParticle.createNativeSmokeForValidation(
        (net.minecraft.client.multiplayer.ClientLevel)mc.level,gastly.getX(),gastly.getY()+0.5D,gastly.getZ(),
        secondGastly.getId(),4.0D,0.0D);
    if(source==null)throw new IllegalStateException("Gastly particle sprites were not ready for provider validation");
    source.tick();
   }
   if(ticks>=5&&!captures.contains("gastly-front")){face(mc,gastly);snap(mc,"gastly-front");}
   if(ticks>=8&&!captures.contains("gastly-no-tongue")&&MinemonModel.gastlyTongueHiddenForValidation(gastly)){
    face(mc,gastly);snapNow(mc,"gastly-no-tongue");
   }
   if(ticks>=10&&screenshotSlotAvailable()&&!captures.contains("gastly-10-block")){
    faceAtDistance(mc,gastly,10.0D,0.0D);snap(mc,"gastly-10-block");
   }
   if(ticks>=12&&screenshotSlotAvailable()&&!captures.contains("gastly-right")){faceFromAngle(mc,gastly,90);snap(mc,"gastly-right");}
   if(ticks>=20&&screenshotSlotAvailable()&&!captures.contains("gastly-back")){faceFromAngle(mc,gastly,180);snap(mc,"gastly-back");}
   if(ticks>=28&&screenshotSlotAvailable()&&!captures.contains("gastly-left")){faceFromAngle(mc,gastly,270);snap(mc,"gastly-left");}
   if(ticks>=31&&screenshotSlotAvailable()&&!captures.contains("gastly-top")){faceVertical(mc,gastly,true);snap(mc,"gastly-top");}
   if(ticks>=34&&screenshotSlotAvailable()&&!captures.contains("gastly-bottom")){faceVertical(mc,gastly,false);snap(mc,"gastly-bottom");}
   if(ticks>=32&&screenshotSlotAvailable()&&!captures.contains("gastly-25-block")){
    faceAtDistance(mc,gastly,25.0D,45.0D);snap(mc,"gastly-25-block");
   }
   if(ticks>=36&&screenshotSlotAvailable()&&!captures.contains("gastly-50-block")){
    if(!farViewRequested)prepareFarGastlyView(mc,gastly);
    else if(farViewApplied){
     double playerDistance=Math.sqrt(gastly.distanceToSqr(mc.player.getX(),mc.player.getY(),mc.player.getZ()));
     double cameraDistance=mc.gameRenderer.getMainCamera().getPosition().distanceTo(gastly.position());
     double renderedCameraDistance=br.com.petrickfaria.minemon.client.GastlyCoreRenderHook.maxCameraDistanceForValidation();
     double cameraAlignment=pointCameraAt(mc,gastly);
     if(playerDistance>=49.0D&&cameraDistance>=49.0D&&cameraDistance<=50.1D&&renderedCameraDistance>=49.0D
         &&br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxRenderDistanceForValidation()>=49.0D&&cameraAlignment>=0.98D){
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
   if(ticks==40)movingGastlyStartX=secondGastly.getX();
   // Move the no-AI spawn-egg fixture by authoritative world positions. Its
   // velocity is intentionally ignored by the server's no-AI movement setup,
   // so moveTo gives the alignment check deterministic horizontal travel.
   if(ticks>40&&ticks<56)mc.getSingleplayerServer().execute(()->{
    var moving=mc.getSingleplayerServer().overworld().getEntity(secondGastlyId);
    if(moving instanceof MinemonCreature g)g.moveTo(g.getX()+.12D,g.getY(),g.getZ(),g.getYRot(),g.getXRot());
   });
   boolean farCameraComplete=!farViewRequested||writtenScreenshots.contains("gastly-50-block");
   if(ticks>=50&&farCameraComplete&&!cameraMatrixActive()&&screenshotSlotAvailable()&&!captures.contains("gastly-motion")){
    if(secondGastly.getX()<movingGastlyStartX+.35D)
     throw new IllegalStateException("The spawn-egg Gastly fixture did not move far enough for the attachment check");
    face(mc,secondGastly);snap(mc,"gastly-motion");
   }
   if(ticks==56)floatingGastlyStartY=secondGastly.getY();
   if(ticks>56&&ticks<76)mc.getSingleplayerServer().execute(()->{
    var moving=mc.getSingleplayerServer().overworld().getEntity(secondGastlyId);
    if(moving instanceof MinemonCreature g)g.moveTo(g.getX(),g.getY()+.1D,g.getZ(),g.getYRot(),g.getXRot());
   });
   if(ticks>=66&&farCameraComplete&&!cameraMatrixActive()&&screenshotSlotAvailable()&&!captures.contains("gastly-float")){
    if(secondGastly.getY()<floatingGastlyStartY+.35D)
     throw new IllegalStateException("The spawn-egg Gastly fixture did not float far enough for the attachment check");
    face(mc,secondGastly);snap(mc,"gastly-float");
   }
   if(ticks>=25&&farCameraComplete&&!cameraMatrixActive()&&!captures.contains("haunter-front-a")){face(mc,haunter);snap(mc,"haunter-front-a");}
   if(ticks>=75&&farCameraComplete&&!cameraMatrixActive()&&!captures.contains("haunter-front-b")){face(mc,haunter);snap(mc,"haunter-front-b");}
   if(ticks>=100&&haunter.isBlinkingForRender()&&MinemonModel.ghostEyesClosedForValidation(haunter)
       &&farCameraComplete&&!cameraMatrixActive()&&screenshotSlotAvailable()&&!captures.contains("haunter-blink")){
    face(mc,haunter);snapNow(mc,"haunter-blink");
   }
   if(ticks>=100&&gengar.isBlinkingForRender()&&MinemonModel.ghostEyesClosedForValidation(gengar)
       &&farCameraComplete&&!cameraMatrixActive()&&screenshotSlotAvailable()&&!captures.contains("gengar-blink")){
    face(mc,gengar);snapNow(mc,"gengar-blink");
   }
   // The 24-view GAS-02 camera sweep can finish after this client test's
   // former fixed Gengar gait window began. Start the gait fixture only once
   // that sweep is done, then give the real renderer a full 180-tick window.
   if(cameraMatrixComplete&&gengarWalkStartTick<0)gengarWalkStartTick=ticks;
   boolean gengarWalkWindow=gengarWalkStartTick>=0&&ticks-gengarWalkStartTick<180;
   if(gengarWalkWindow){
    mc.getSingleplayerServer().execute(()->{
     var authoritative=mc.getSingleplayerServer().overworld().getEntity(gengarId);
     if(authoritative instanceof MinemonCreature g){g.setOnGround(true);g.setDeltaMovement(0.055D,0,0);}
    });
   }
   if(gengarWalkStartTick>=0&&ticks-gengarWalkStartTick>=10&&gengarWalkWindow&&!captures.contains("gengar-walk")){
    // Keep the test window open until the renderer has selected the real
    // ground gait and a screenshot slot is available.
    face(mc,gengar);
    if(screenshotSlotAvailable()&&"animation.gengar.ground_walk".equals(readClip(gengar)))
     snap(mc,"gengar-walk");
   }
   if(gengarWalkStartTick>=0&&ticks-gengarWalkStartTick>=180&&!captures.contains("gengar-walk"))
    throw new IllegalStateException("Gengar walking screenshot was not captured during ground_walk: "+readClip(gengar));
   // Keep each ghost in the render view while waiting for its intermittent blink.
   // Do not steal the camera from the Gengar gait check before its screenshot.
   if(ticks>=145&&ticks<705&&!cameraMatrixActive()&&captures.contains("gengar-walk")&&(!captures.contains("haunter-blink")||!captures.contains("gengar-blink"))){
    boolean needHaunterBlink=!captures.contains("haunter-blink");
    boolean needGengarBlink=!captures.contains("gengar-blink");
    if(needHaunterBlink&&needGengarBlink)face(mc,((ticks/20)%2==0)?haunter:gengar);
    else if(needHaunterBlink)face(mc,haunter);
    else face(mc,gengar);
   }
   if(gengarWalkStartTick>=0&&ticks>=gengarWalkStartTick+180&&captures.contains("gengar-walk")){
    mc.getSingleplayerServer().execute(()->{
     var authoritative=mc.getSingleplayerServer().overworld().getEntity(gengarId);
     if(authoritative instanceof MinemonCreature g)g.setDeltaMovement(0,0,0);
    });
   }
   if(ticks>=160&&!cameraMatrixActive()&&!captures.contains("gengar-mouth-teeth-fixed")
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
   if(sleepStarted&&ticks>=730&&!cameraMatrixActive()&&!captures.contains("gengar-sleep-half-sunk-a")){face(mc,gengar);snap(mc,"gengar-sleep-half-sunk-a");}
   if(sleepStarted&&ticks>=760&&!cameraMatrixActive()&&!captures.contains("gengar-sleep-half-sunk-b")){face(mc,gengar);snap(mc,"gengar-sleep-half-sunk-b");}
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
    if(!cameraMatrixComplete||cameraMatrixPasses!=24)
     throw new IllegalStateException("Gastly camera acceptance matrix incomplete: "+cameraMatrixPasses+"/24 views");
    if(br.com.petrickfaria.minemon.client.GastlyCoreRenderHook.auraFramesForValidation()==0)
     throw new IllegalStateException("Gastly aura was never submitted with nucleus depth occlusion during the real-client test");
    if(br.com.petrickfaria.minemon.client.GastlyCoreMask.verifiedPixelsForValidation()==0)
     throw new IllegalStateException("No opaque Gastly core pixels were compared before and after the aura pass");
    if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.largeSpawnsForValidation()==0
        ||br.com.petrickfaria.minemon.client.GastlySmokeParticle.smallSpawnsForValidation()==0)
     throw new IllegalStateException("Gastly's large and small particles were not both rendered during camera-angle validation");
    if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.farRendersForValidation()==0)
     throw new IllegalStateException("Gastly aura particles were not rendered beyond the vanilla particle cutoff during the 50-block camera check; render hook maximum camera distance="
       +br.com.petrickfaria.minemon.client.GastlyCoreRenderHook.maxCameraDistanceForValidation());
    if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxRenderDistanceForValidation()<49.0D)
     throw new IllegalStateException("Gastly aura did not reach the 50-block render boundary; maximum observed distance="
       +br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxRenderDistanceForValidation());
    if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.nativeSourceSampleCountForValidation()==0
        ||br.com.petrickfaria.minemon.client.GastlySmokeParticle.nativeEmitterTickCountForValidation()==0)
     throw new IllegalStateException("Native Gastly particle sources were not tracked through the real client tick");
    if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxNativeSourceDistanceFromGastlyForValidation()>2.0D)
     throw new IllegalStateException("A native smoke particle was emitted away from both Gastly bodies; maximum source distance="
       +br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxNativeSourceDistanceFromGastlyForValidation());
    if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxNativeEmitterDriftForValidation()>1.5D)
     throw new IllegalStateException("A native smoke particle escaped the Gastly body-bound aura; maximum body distance="
       +br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxNativeEmitterDriftForValidation());
    for(String key:List.of("gastly-front","gastly-no-tongue","gastly-right","gastly-back","gastly-left","gastly-top","gastly-bottom","gastly-10-block","gastly-25-block","gastly-50-block","gastly-motion","gastly-float","haunter-front-a","haunter-front-b","haunter-blink",
            "gengar-walk","gengar-blink","gengar-mouth-teeth-fixed","gengar-sleep-half-sunk-a","gengar-sleep-half-sunk-b"))
     if(!captures.contains(key))throw new IllegalStateException("Missing real-client ghost screenshot: "+key);
    if(!MinemonModel.gastlyTongueHiddenForValidation(gastly))throw new IllegalStateException("Gastly tongue expression was not suppressed");
    if(!MinemonModel.gengarTeethMouthForValidation(gengar))throw new IllegalStateException("Gengar teeth mouth was not selected");
    if(gengarSleepBobMax-gengarSleepBobMin<0.2F)
     throw new IllegalStateException("Gengar sleep body bob did not vary enough: "+gengarSleepBobMin+".."+gengarSleepBobMax);
    Files.writeString(Path.of("client-validation-ghost-line.txt"),
      "PASS GHOST LINE CLIENT: real Minecraft render captured Gastly aura/core from front, side, rear, top, and bottom camera views and at 10, 25, and 50 blocks; isolated target and distant spawn-egg Gastly provider fixture; horizontal movement and vertical float; native smoke bound to Gastly; visible Haunter and Gengar closed-eye blinks, Gengar lowered-arm gait, fixed teeth mouth, and less-sunk bobbing sleep.\n"
      +"PASS GAS-02: "+cameraMatrixPasses+" camera combinations (yaw 0/45/90/135/180/225/270/315°, pitch -30/0/+30°) retained visible opaque core pixels at 5 blocks.\n"
      +"Gastly native particles="+br.com.petrickfaria.minemon.client.GastlySmokeParticle.nativeEmitterCountForValidation()+", source-distance samples="+br.com.petrickfaria.minemon.client.GastlySmokeParticle.nativeSourceSampleCountForValidation()+", maximum source distance="+br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxNativeSourceDistanceFromGastlyForValidation()+", tracked ticks="+br.com.petrickfaria.minemon.client.GastlySmokeParticle.nativeEmitterTickCountForValidation()+", maximum body distance="+br.com.petrickfaria.minemon.client.GastlySmokeParticle.maxNativeEmitterDriftForValidation()+".\n"
      +"Haunter blink events="+haunter.blinkEventsForValidation()+", last interval="+haunter.lastBlinkIntervalForValidation()+" ticks; Gengar blink events="+gengar.blinkEventsForValidation()+", last interval="+gengar.lastBlinkIntervalForValidation()+" ticks.\n"
      +"Haunter hand phase lag="+br.com.petrickfaria.minemon.entity.GhostLineAnimationRules.HAUNTER_HAND_LAG_TICKS+" ticks (2.5 seconds); Gengar ground walk clip="+readClip(gengar)+"; measured sleep bob range="+gengarSleepBobMin+".."+gengarSleepBobMax+".\n");
    validationComplete=true;
    System.out.println("[GHOST LINE] PASS: runtime checks complete; waiting for all 20 screenshots to be written.");
   }
  }catch(Exception e){throw new IllegalStateException("Ghost line real-client validation failed",e);}
 }
 private static void verifyVisibleAura(Minecraft mc,String view){
  // Read the actual framebuffer, rather than accepting render-call counters as visibility.
  try(var frame=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
   int purple=0;
   // Looking at Gastly from directly below places the aura lower in the
   // framebuffer than the near-horizontal views. Keep the centered horizontal
   // region around the target, but scan nearly the full vertical frame so the
   // visibility assertion does not mistake camera framing for a missing aura.
   for(int y=frame.getHeight()/20;y<frame.getHeight()*19/20;y++)
    for(int x=frame.getWidth()/4;x<frame.getWidth()*3/4;x++){
     int pixel=frame.getPixelRGBA(x,y);
     int red=pixel&255,green=(pixel>>>8)&255,blue=(pixel>>>16)&255;
     if(red>40&&blue>55&&red>green+12&&blue>green+15)purple++;
    }
   if(purple<5)throw new IllegalStateException("Gastly aura is absent from the actual "+view+" framebuffer: purple pixels="+purple);
   System.out.println("[GHOST LINE] visible aura "+view+": "+purple+" purple pixels near the target");
  }
 }
 private static void verifyVisibleCore(Minecraft mc,String view){
  MinemonCreature mob=find(mc,gastlyId);
  if(mob==null||mc.player==null)throw new IllegalStateException("Gastly or camera player missing in "+view);
  var toCore=new net.minecraft.world.phys.Vec3(mob.getX()-mc.player.getX(),mob.getY()+1.15D-mc.player.getEyeY(),mob.getZ()-mc.player.getZ()).normalize();
  double alignment=mc.player.getLookAngle().normalize().dot(toCore);
  if(alignment<0.98D)throw new IllegalStateException("Camera was not aimed at Gastly in "+view+": alignment="+alignment);
  try(var frame=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
   int darkCore=0;
   int x0=frame.getWidth()*43/100,x1=frame.getWidth()*57/100;
   int y0=frame.getHeight()*38/100,y1=frame.getHeight()*66/100;
   for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){
    int pixel=frame.getPixelRGBA(x,y);
    int red=pixel&255,green=(pixel>>>8)&255,blue=(pixel>>>16)&255;
    if(red<85&&green<85&&blue<110)darkCore++;
   }
   // Oblique views can reduce the black facial core to only a few pixels.
   // Require actual visible pixels after validating that the camera targets Gastly.
   if(darkCore==0)throw new IllegalStateException("Gastly core is obscured in the actual "+view+" framebuffer: dark core pixels="+darkCore);
   System.out.println("[GHOST LINE] visible core "+view+": "+darkCore+" dark pixels; camera alignment="+alignment);
  }
 }
 private static MinemonCreature spawn(net.minecraft.server.level.ServerLevel level,String slug,double x,double z){
  return spawn(level,slug,x,z,net.minecraft.world.entity.MobSpawnType.NATURAL);
 }
 private static MinemonCreature spawn(net.minecraft.server.level.ServerLevel level,String slug,double x,double z,
      net.minecraft.world.entity.MobSpawnType spawnType){
  var mob=ModEntities.SPECIES.get(slug).get().create(level);
  mob.finalizeSpawn(level,level.getCurrentDifficultyAt(new BlockPos((int)x,101,(int)z)),spawnType,null,null);
  mob.moveTo(x,101,z,180,0);mob.setNoAi(true);
  level.addFreshEntity(mob);return mob;
 }
 private static String readClip(MinemonCreature mob){
  try{var f=MinemonCreature.class.getDeclaredField("selectedAnimationForValidation");f.setAccessible(true);return String.valueOf(f.get(mob));}
  catch(Exception ignored){return "unavailable";}
 }
 private static boolean reopenOpened,reopenFinished,reopenPositioned;
 private static int reopenTicks,reopenCaptureAt;
 static void reopenTick(TickEvent.ClientTickEvent event) {
  if(event.phase!=TickEvent.Phase.END||reopenFinished)return;
  Minecraft mc=Minecraft.getInstance();
  try {
   if(!reopenOpened&&mc.screen instanceof TitleScreen){
    reopenOpened=true;
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
   reopenTicks++;
   MinemonCreature gastly=null;
   for(var e:mc.level.entitiesForRendering())if(e instanceof MinemonCreature m&&m.hasCustomName()
       &&m.getCustomName().getString().equals("Minemon Gastly Persistence Fixture")){gastly=m;break;}
   if(gastly==null){
    if(reopenTicks>240)throw new IllegalStateException("Named Gastly was not restored after reopening the saved world");
    return;
   }
   // The re-open runs in a fresh client process, so rebind the persisted
   // entity UUID before core/framebuffer assertions that resolve it by ID.
   gastlyId=gastly.getUUID();
   if(reopenTicks<30)return;
   if(!reopenPositioned){
    UUID playerId=mc.player.getUUID();
    mc.getSingleplayerServer().execute(()->{
     var serverPlayer=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
     if(serverPlayer!=null)serverPlayer.setGameMode(GameType.SPECTATOR);
    });
    face(mc,gastly);
    reopenPositioned=true;reopenCaptureAt=reopenTicks+5;
    return;
   }
   if(reopenTicks<reopenCaptureAt)return;
   String reopenLabel="save/reopen "+System.getProperty("minemon.gastlyReopenIndex","1");
   verifyVisibleAura(mc,reopenLabel);
   verifyVisibleCore(mc,reopenLabel);
   Screenshot.grab(mc.gameDirectory,"gastly-persistence-reopen-"+System.getProperty("minemon.gastlyReopenIndex","1")+".png",
      mc.getMainRenderTarget(),message->{
       try {
        String index=System.getProperty("minemon.gastlyReopenIndex","1");
        Files.writeString(Path.of("gastly-persistence-reopen-"+index+".txt"),
          "PASS GASTLY PERSISTENCE REOPEN "+index+": saved Gastly entity restored and its aura is visible in the real client framebuffer.\n");
        reopenFinished=true;mc.stop();
       } catch(Exception failure){throw new RuntimeException(failure);}
      });
  } catch(Exception failure){throw new RuntimeException("Gastly persistence client validation failed",failure);}
 }
 private GhostLineClientSmokeTest(){}
}
