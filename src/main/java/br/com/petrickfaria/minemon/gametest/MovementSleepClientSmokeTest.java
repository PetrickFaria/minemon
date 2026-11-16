package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.client.MinemonRenderer;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModEntities;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real integrated-server/client transitions; a second launch reopens the saved world. */
final class MovementSleepClientSmokeTest {
    private static final boolean REOPEN=Boolean.getBoolean("minemon.followupReopen");
    private static boolean opening,setup,done,photoRequested;
    private static int ticks,cameraPhase=-1;
    private static volatile int phase,elapsed;
    private static volatile Throwable failure;
    private static volatile String photo,photoDone;
    private static volatile boolean goldeenEyes,pidgeyEyes,gyaradosEyes;
    private static volatile boolean combatFlightRendered;
    private static boolean combatHoverSeen;
    private static volatile double maxClientSleepStep;
    private static volatile double maxClientBirdSleepStep;
    private static long lastTick=-1,start,settled=-1;
    private static volatile MinemonCreature fish,wildFish,bird,rock,awakeRock,gyarados,wildGyarados,seatedGyarados;
    private static net.minecraft.world.entity.monster.Zombie zombie;
    private static ArmorStand camera;
    private static Vec3 previousFish,previousBird,previousClientFish,previousClientBird;
    private static double birdDrift,fishDrift;

    private static void check(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
    private static void report(String text)throws Exception{
        Files.writeString(Path.of(REOPEN?"client-validation-followup-reopen.txt":"client-validation-followup.txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        System.out.println(text);
    }
    static void tick(){
        if(done)return;
        Minecraft mc=Minecraft.getInstance();
        if(failure!=null)throw new IllegalStateException("Movement/sleep real-client validation failed",failure);
        try{
            if(!opening){
                if(!(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen)||++ticks<40)return;
                Object flows=Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);boolean invoked=false;
                for(var method:flows.getClass().getMethods())if(method.getName().equals("loadLevel")){
                    if(method.getParameterCount()==2&&method.getParameterTypes()[1]==String.class){method.invoke(flows,mc.screen,"minemon-validation");invoked=true;break;}
                    if(method.getParameterCount()==1&&method.getParameterTypes()[0]==String.class){method.invoke(flows,"minemon-validation");invoked=true;break;}
                }
                check(invoked,"Prepared world could not be opened");opening=true;return;
            }
            if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
            if(!setup){
                setup=true;
                MinemonRenderer.validationObserver=(entity,model)->{
                    if(bird!=null&&entity.getUUID().equals(bird.getUUID())&&!entity.isSleeping()
                            &&(entity.isCombatPose()||entity.isAttackAnimationActive())){
                        var clip=entity.getSelectedAnimationForValidation();
                        combatFlightRendered|=clip!=null&&(clip.contains("air")||clip.contains("attack"));
                    }
                    if(!entity.isSleeping())return;
                    var left=model.getAnimationProcessor().getBone("eyelid_left");
                    var right=model.getAnimationProcessor().getBone("eyelid_right");
                    if(fish!=null&&entity.getUUID().equals(fish.getUUID())){
                        var fullLeft=model.getAnimationProcessor().getBone("eyelid_full_left");
                        var fullRight=model.getAnimationProcessor().getBone("eyelid_full_right");
                        goldeenEyes|=fullLeft!=null&&fullRight!=null&&!fullLeft.isHidden()&&!fullRight.isHidden();
                    }
                    if(left!=null&&right!=null&&left.getPositionZ()<-.05F&&right.getPositionZ()<-.05F){
                        if(bird!=null&&entity.getUUID().equals(bird.getUUID()))pidgeyEyes=true;
                        if(gyarados!=null&&entity.getUUID().equals(gyarados.getUUID()))gyaradosEyes=true;
                    }
                };
            }
            mc.getSingleplayerServer().execute(()->{try{serverTick(mc);}catch(Throwable problem){failure=problem;}});
            if(fish==null||bird==null)return;
            MinemonCreature subject=phase==0||phase>=3&&phase<=5?bird:phase==2||phase==6?gyarados:fish;
            if(REOPEN)subject=elapsed<70?fish:elapsed<140?bird:gyarados;
            var client=mc.level.getEntity(subject.getId());
            if(client!=null){
                if(camera==null)camera=new ArmorStand(mc.level,0,0,0);
                double distance=subject==gyarados?8:4;
                double targetY=client.getY()+client.getBbHeight()*.6D;
                float yaw=38,pitch=12;
                camera.moveTo(client.getX()+distance*.65, targetY+distance*.2-camera.getEyeHeight(),client.getZ()-distance*.8,yaw,pitch);
                camera.yRotO=yaw;camera.xRotO=pitch;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;mc.setCameraEntity(camera);mc.options.hideGui=true;
            }
            var clientFish=mc.level.getEntity(fish.getId());
            if(clientFish instanceof MinemonCreature c&&c.isSleeping()&&(phase==1&&elapsed>70||REOPEN&&elapsed>30)){
                if(previousClientFish!=null)maxClientSleepStep=Math.max(maxClientSleepStep,c.position().distanceTo(previousClientFish));
                previousClientFish=c.position();
            }else previousClientFish=null;
            var clientBird=mc.level.getEntity(bird.getId());
            if(clientBird instanceof MinemonCreature c&&c.isSleeping()&&(phase==3&&elapsed>40||phase==5&&elapsed>80||REOPEN&&elapsed>30)){
                if(previousClientBird!=null)maxClientBirdSleepStep=Math.max(maxClientBirdSleepStep,c.position().distanceTo(previousClientBird));
                previousClientBird=c.position();
            }else previousClientBird=null;
            if(mc.screen==null&&mc.getOverlay()==null&&photo!=null&&!photo.equals(photoDone)&&!photoRequested){
                photoRequested=true;String name=photo;
                net.minecraft.client.Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),message->{photoDone=name;photoRequested=false;});
            }
        }catch(Exception problem){throw new IllegalStateException("Movement/sleep graphical setup failed",problem);}
    }
    private static MinemonCreature create(ServerLevel level,String slug,String tag,double x,double y,double z,ServerPlayer owner,String mode){
        var entity=ModEntities.SPECIES.get(slug).get().create(level);entity.moveTo(x,y,z,180,0);entity.addTag(tag);
        if(owner!=null){entity.tame(owner);entity.applyManagedBehavior(mode);}level.addFreshEntity(entity);return entity;
    }
    private static MinemonCreature find(ServerLevel level,String tag){
        for(var entity:level.getAllEntities())if(entity instanceof MinemonCreature c&&c.getTags().contains(tag))return c;
        return null;
    }
    private static void next(ServerLevel level,int value){phase=value;start=level.getGameTime();settled=-1;photo=null;}
    private static boolean photograph(String name){if(photo==null)photo=name;return name.equals(photoDone);}
    private static void serverTick(Minecraft mc)throws Exception{
        var server=mc.getSingleplayerServer();var level=server.overworld();long now=level.getGameTime();if(now==lastTick)return;lastTick=now;
        var owner=server.getPlayerList().getPlayers().get(0);
        if(fish==null){
            if(REOPEN){
                fish=find(level,"followup-free-fish");wildFish=find(level,"followup-wild-fish");bird=find(level,"followup-bird");
                gyarados=find(level,"followup-free-gyarados");wildGyarados=find(level,"followup-wild-gyarados");seatedGyarados=find(level,"followup-seated-gyarados");
                if(fish==null||bird==null||gyarados==null||wildGyarados==null||seatedGyarados==null){fish=null;return;}
                next(level,8);return;
            }
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(1000);
            for(int x=0;x<32;x++)for(int z=0;z<32;z++)for(int y=100;y<=121;y++){
                boolean wall=x==0||x==31||z==0||z==31;
                level.setBlock(new BlockPos(x,y,z),(y==100||wall&&y<=119?Blocks.STONE:y<=118?Blocks.WATER:Blocks.AIR).defaultBlockState(),3);
            }
            for(int x=40;x<=58;x++)for(int z=0;z<=18;z++)for(int y=100;y<=113;y++)level.setBlock(new BlockPos(x,y,z),(y==100?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            owner.setGameMode(GameType.CREATIVE);owner.getAbilities().flying=true;owner.onUpdateAbilities();owner.teleportTo(level,8.5,120,8.5,0,0);
            fish=create(level,"goldeen","followup-free-fish",6.5,104,8.5,owner,"free");
            wildFish=create(level,"goldeen","followup-wild-fish",6.5,104,14.5,null,"");
            bird=create(level,"pidgey","followup-bird",48.5,103,8.5,owner,"sit");
            rock=create(level,"geodude","followup-rock",52.5,101,8.5,null,"");
            awakeRock=create(level,"geodude","followup-awake-rock",54.5,101,10.5,null,"");awakeRock.setNoAi(true);
            start=now;return;
        }
        elapsed=(int)(now-start);check(elapsed<1600,"Scenario timed out: phase="+phase+" fish="+fish.position()+" bird="+bird.position()+" birdTarget="+bird.getTarget());
        if(REOPEN){
            check(fish.isOwnedBy(owner)&&bird.isOwnedBy(owner)&&gyarados.isOwnedBy(owner),"Actual world reopening lost Pokémon ownership");
            check(fish.isFreeMode()&&fish.isSleeping()&&bird.isOrderedToSit()&&bird.isSleeping(),"Saved managed mode or sleep was lost on actual world reopening");
            check(gyarados.isSleeping()&&wildGyarados.isSleeping()&&seatedGyarados.isSleeping(),"Saved Gyarados bottom sleep was lost");
            check(gyarados.isFreeMode()&&!wildGyarados.isTame()&&seatedGyarados.isOrderedToSit()
                    &&seatedGyarados.isOwnedBy(owner),"Saved wild/free/seated Gyarados modes or ownership changed");
            for(var g:List.of(gyarados,wildGyarados,seatedGyarados))
                check(Math.abs(g.getY()-101)<.1&&level.noCollision(g,g.getBoundingBox()),"Reopened Gyarados lost its safe bottom anchor: "+g.position());
            if(elapsed>30){
                if(previousFish!=null)fishDrift=Math.max(fishDrift,fish.position().distanceTo(previousFish));previousFish=fish.position();
                if(previousBird!=null)birdDrift=Math.max(birdDrift,bird.position().distanceTo(previousBird));previousBird=bird.position();
            }
            if(elapsed>220&&goldeenEyes&&pidgeyEyes&&gyaradosEyes){
                check(fishDrift<.002&&birdDrift<.002&&maxClientSleepStep<.002&&maxClientBirdSleepStep<.002,"Reopened sleepers oscillated: fish="+fishDrift+" bird="+birdDrift+" client="+maxClientSleepStep+" clientBird="+maxClientBirdSleepStep);
                report("PASS FOLLOWUP REOPEN: actual saved world preserved free Goldeen, seated Pidgey, wild/free/seated Gyarados bottom sleep; rendered closed eyes and server/client stability.");finish(mc);
            }
            return;
        }
        if(phase==0&&elapsed>=100){
            check(bird.isOrderedToSit()&&!bird.isSleeping()&&Math.abs(bird.getY()-102)<.15,"Daytime seated Pidgey did not hover one block above support");
            check(bird.getTarget()==null,"Unprovoked awake Geodude caused daytime combat");
            if(!photograph("followup-pidgey-day-hover"))return;
            report("PASS FOLLOWUP DAY: seated Pidgey hovers one block above support beside unprovoked wild Geodude.");level.setDayTime(18000);next(level,1);
        }else if(phase==1&&fish.isSleeping()&&wildFish.isSleeping()&&elapsed>150&&goldeenEyes){
            check(!fish.isFollowingOwner()&&maxClientSleepStep<.002,"Goldeen sleep jitter/following under nearby shore owner; clientStep="+maxClientSleepStep);
            if(!photograph("followup-goldeen-sleep-underwater"))return;
            report("PASS FOLLOWUP GOLDEEN: wild/free underwater sleep, nearby elevated owner, closed rendered lids; maximum client sleep step="+maxClientSleepStep);
            gyarados=create(level,"gyarados","followup-free-gyarados",12.5,113,8.5,owner,"free");
            wildGyarados=create(level,"gyarados","followup-wild-gyarados",22.5,113,22.5,null,"");
            seatedGyarados=create(level,"gyarados","followup-seated-gyarados",22.5,113,8.5,owner,"sit");next(level,2);
        }else if(phase==2&&gyarados.isSleeping()&&wildGyarados.isSleeping()&&seatedGyarados.isSleeping()){
            for(var g:List.of(gyarados,wildGyarados,seatedGyarados))check(Math.abs(g.getY()-101)<.1&&level.noCollision(g,g.getBoundingBox()),"Gyarados did not reach collision-safe bottom: "+g.position());
            if(settled<0)settled=now;
            if(now-settled>60&&gyaradosEyes&&photograph("followup-gyarados-bottom-sleep")){
                report("PASS FOLLOWUP GYARADOS: wild/free/seated native sleep at safe submerged support; closed rendered lids.");next(level,3);
            }
        }else if(phase==3&&elapsed>100){
            check(bird.isSleeping()&&bird.getTarget()==null&&rock.isSleeping(),"Sleeping wild Geodude caused false wake/target acquisition");
            check(Math.abs(bird.getY()-101)<.05,"Seated Pidgey did not remain landed at night");
            if(!pidgeyEyes||!photograph("followup-pidgey-night-geodude"))return;
            report("PASS FOLLOWUP PIDGEY NIGHT: stable landed sleep and closed eyes beside awake/sleeping wild Geodude; no unprovoked target.");
            zombie=EntityType.ZOMBIE.create(level);zombie.setNoAi(true);zombie.moveTo(48.5,101,12.5,0,0);zombie.setHealth(6);level.addFreshEntity(zombie);pidgeyEyes=false;next(level,4);
        }else if(phase==4){
            check(bird.isOrderedToSit(),"Combat discarded the Pidgey's seated command");
            if(!bird.isSleeping()&&bird.getTarget()==zombie&&bird.getY()>101.7)combatHoverSeen=true;
            if(combatHoverSeen&&photo==null)photo="followup-pidgey-combat-hover";
            if(zombie.isAlive()){
                if(elapsed>80)check(!bird.isSleeping()&&bird.getTarget()==zombie&&bird.getY()>101.7,"Pidgey did not wake, hover and retain the real hostile target");
            }else{
                check(combatHoverSeen,"Pidgey defeated the threat without resuming its hover");
                if(!photograph("followup-pidgey-combat-hover"))return;
                report("PASS FOLLOWUP PIDGEY COMBAT: actual ranged damage defeated the hostile zombie while keeping the sit command.");next(level,5);
            }
        }else if(phase==5&&bird.isSleeping()&&elapsed>100&&pidgeyEyes){
            check(Math.abs(bird.getY()-101)<.05,"Pidgey bounced after defeating its threat");
            check(combatFlightRendered&&maxClientBirdSleepStep<.002,"Combat flight was not rendered or client Pidgey sleep jittered: rendered="+combatFlightRendered+" step="+maxClientBirdSleepStep);
            if(!photograph("followup-pidgey-after-combat-sleep"))return;
            report("PASS FOLLOWUP PIDGEY RECOVERY: stable landed sleep and closed eyes resumed after the zombie died.");level.setDayTime(1000);next(level,6);
        }else if(phase==6&&elapsed>160){
            check(!fish.isSleeping()&&!gyarados.isSleeping()&&!wildGyarados.isSleeping()&&!bird.isSleeping(),"Dawn did not wake sleepers");
            check(gyarados.getY()>102&&wildGyarados.getY()>102,"Gyarados did not naturally ascend after dawn");
            check(Math.abs(bird.getY()-102)<.15,"Pidgey did not resume daytime seated hover");
            report("PASS FOLLOWUP DAWN: Goldeen and Gyarados resume natural swimming; seated Pidgey resumes its one-block hover.");level.setDayTime(18000);next(level,7);
        }else if(phase==7&&elapsed>150&&fish.isSleeping()&&bird.isSleeping()&&gyarados.isSleeping()&&wildGyarados.isSleeping()&&seatedGyarados.isSleeping()){
            report("PASS FOLLOWUP SAVE: natural night states restored; saving the real world for a separate process reopen.");finish(mc);
        }
    }
    private static void finish(Minecraft mc){done=true;MinemonRenderer.validationObserver=null;mc.execute(mc::stop);}
}
