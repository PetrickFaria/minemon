package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.client.MinemonRenderer;
import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib3.core.processor.IBone;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Real-client coverage for Kadabra's ground-only cycle and Alakazam's meditation hover. */
@Mod.EventBusSubscriber(modid=Minemon.MOD_ID,value=Dist.CLIENT)
final class PsychicBehaviorClientSmokeTest {
    private static boolean opened,spawnRequested,nightRequested,poseSeen,sleepPoseSeen,kadabraGroundAnimationSeen,kadabraSleepSeen;
    private static int ticks,setupDelay,poseFrames;
    private static volatile UUID kadabraId,alakazamId;
    private static double hoverStartX,hoverStartZ;
    private static boolean hoverStartSet;
    private static int hoverStartServerTick=-1;

    private PsychicBehaviorClientSmokeTest() {}

    @SubscribeEvent static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        try {
            if(!opened&&mc.screen instanceof TitleScreen){
                opened=true;
                var data=net.minecraft.nbt.NbtIo.readCompressed(Path.of("saves/minemon-validation/level.dat").toFile());
                data.getCompound("Data").putBoolean("confirmedExperimentalSettings",true);
                net.minecraft.nbt.NbtIo.writeCompressed(data,Path.of("saves/minemon-validation/level.dat").toFile());
                Object flows=Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);
                for(var method:flows.getClass().getMethods())if(method.getName().equals("loadLevel")){
                    if(method.getParameterCount()==2&&method.getParameterTypes()[1]==String.class){method.invoke(flows,mc.screen,"minemon-validation");break;}
                    if(method.getParameterCount()==1&&method.getParameterTypes()[0]==String.class){method.invoke(flows,"minemon-validation");break;}
                }
                Files.writeString(Path.of("psychic-client-validation.txt"),"Minemon 1.67 psychic behavior client validation\n");
                MinemonRenderer.validationObserver=PsychicBehaviorClientSmokeTest::observeRender;
                return;
            }
            if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||mc.screen!=null)return;
            if(!spawnRequested&&++setupDelay>=60){spawnRequested=true;mc.getSingleplayerServer().execute(()->spawnFixture(mc));return;}
            if(!spawnRequested||alakazamId==null||kadabraId==null)return;
            ticks++;

            var server=mc.getSingleplayerServer().overworld();
            var kadabra=server.getEntity(kadabraId);
            var alakazam=server.getEntity(alakazamId);
            if(!(kadabra instanceof MinemonCreature k)||!(alakazam instanceof MinemonCreature a))
                throw new IllegalStateException("Kadabra or Alakazam disappeared during the cycle test");
            if(k.isFlyingSpecies()||k.naturalCycle().phase()!=BehaviorCycle.Phase.LAND
                    ||ticks>40&&!k.isOnGround())
                throw new IllegalStateException("Kadabra left its ground-only routine: onGround="+k.isOnGround()+" flying="+k.isFlyingSpecies()+" phase="+k.naturalCycle().phase());

            var phase=a.naturalCycle().phase();
            if(phase==BehaviorCycle.Phase.TAKEOFF||phase==BehaviorCycle.Phase.AIR){
                if(!hoverStartSet){hoverStartSet=true;hoverStartX=a.getX();hoverStartZ=a.getZ();}
                double drift=(a.getX()-hoverStartX)*(a.getX()-hoverStartX)+(a.getZ()-hoverStartZ)*(a.getZ()-hoverStartZ);
                if(drift>0.04D)throw new IllegalStateException("Alakazam drifted horizontally during takeoff/hover: "+drift);
                if(phase==BehaviorCycle.Phase.AIR){
                    double altitude=a.getY()-a.naturalSurface();
                    if(Math.abs(altitude-1.0D)>0.035D)
                        throw new IllegalStateException("Alakazam hover base is not one block above support: "+altitude);
                    if(!a.level.noCollision(a,a.getBoundingBox()))throw new IllegalStateException("Alakazam hover intersects the world");
                    if(hoverStartServerTick<0)hoverStartServerTick=a.tickCount;
                }
            }
            if(phase==BehaviorCycle.Phase.LANDING&&hoverStartServerTick>=0){
                int elapsed=a.tickCount-hoverStartServerTick;
                if(elapsed<599||elapsed>603)
                    throw new IllegalStateException("Alakazam hover duration differs from 30 seconds: "+elapsed+" ticks");
                if(a.isAlakazamMeditating())throw new IllegalStateException("Meditation animation remained active during landing");
            }
            if(phase==BehaviorCycle.Phase.LAND&&a.isOnGround()&&!nightRequested){
                nightRequested=true;
                server.setDayTime(18_000L);
            }
            if(nightRequested&&a.isSleeping()&&k.isSleeping()&&poseSeen&&sleepPoseSeen&&kadabraGroundAnimationSeen&&kadabraSleepSeen){
                Files.writeString(Path.of("psychic-client-validation.txt"),
                    "PASS PSYCHIC CLIENT: Kadabra ground-only with day animation and original night sleep; Alakazam 3-minute cycle, stationary 1-block 30-second hover, rendered meditation pose, original night sleep\n",
                    java.nio.file.StandardOpenOption.APPEND);
                mc.stop();
            }
            if(ticks>1_200)throw new IllegalStateException("Psychic client validation timed out: Kadabra="+k.isSleeping()+" Alakazam="+a.isSleeping()+" phase="+phase+" pose="+poseSeen+" sleepPose="+sleepPoseSeen+" kadabraGroundAnimation="+kadabraGroundAnimationSeen+" kadabraSleep="+kadabraSleepSeen);
        }catch(Exception ex){throw new IllegalStateException("Psychic behavior client validation failed",ex);}
    }

    private static void spawnFixture(Minecraft mc){
        var level=mc.getSingleplayerServer().overworld();
        level.setDayTime(6_000L);
        for(int x=-5;x<=10;x++)for(int z=-10;z<=4;z++){
            level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
            for(int y=101;y<=108;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        var kadabra=ModEntities.SPECIES.get("kadabra").get().create(level);
        var alakazam=ModEntities.SPECIES.get("alakazam").get().create(level);
        if(kadabra==null||alakazam==null)throw new IllegalStateException("Could not create psychic behavior fixtures");
        kadabra.moveTo(0.5D,101.0D,0.5D,180.0F,0.0F);
        alakazam.moveTo(4.5D,101.0D,0.5D,180.0F,0.0F);
        kadabra.setCustomName(net.minecraft.network.chat.Component.literal("1.67 Kadabra check"));
        alakazam.setCustomName(net.minecraft.network.chat.Component.literal("1.67 Alakazam check"));
        var tag=new net.minecraft.nbt.CompoundTag();
        alakazam.addAdditionalSaveData(tag);
        tag.putString("NaturalBehaviorPhase","LAND");tag.putInt("NaturalBehaviorTicks",3_599);
        tag.putInt("NaturalAltitudeStage",0);tag.putInt("NaturalBehaviorStartDelay",0);
        alakazam.readAdditionalSaveData(tag);
        var player=level.getServer().getPlayerList().getPlayers().get(0);
        player.setGameMode(GameType.CREATIVE);player.teleportTo(level,4.5D,101.0D,-8.0D,0.0F,0.0F);
        level.addFreshEntity(kadabra);level.addFreshEntity(alakazam);
        kadabraId=kadabra.getUUID();alakazamId=alakazam.getUUID();
    }

    private static void observeRender(MinemonCreature mob,br.com.petrickfaria.minemon.client.MinemonModel model){
        if(kadabraId!=null&&mob.getUUID().equals(kadabraId)){
            String selected=mob.getSelectedAnimationForValidation();
            if(mob.isSleeping()){
                if(SpeciesAssetIndex.clip("kadabra","night_sleep","night_sleep").equals(selected))kadabraSleepSeen=true;
            }else if(selected!=null&&(selected.equals(SpeciesAssetIndex.clip("kadabra","ground_idle","ground_idle"))
                    ||selected.equals(SpeciesAssetIndex.clip("kadabra","ground_walk","ground_walk"))))kadabraGroundAnimationSeen=true;
            return;
        }
        if(alakazamId==null||!mob.getUUID().equals(alakazamId))return;
        if(mob.isAlakazamMeditating()){
            String expected="animation.alakazam.meditate_float";
            if(!expected.equals(mob.getSelectedAnimationForValidation()))return;
            if(mob.isSleeping())throw new IllegalStateException("Alakazam meditation pose selected while sleeping");
            if(++poseFrames<10)return;
            IBone upperLeft=model.getAnimationProcessor().getBone("left_upper_leg");
            IBone upperRight=model.getAnimationProcessor().getBone("right_upper_leg");
            IBone kneeLeft=model.getAnimationProcessor().getBone("left_lower_leg");
            IBone kneeRight=model.getAnimationProcessor().getBone("right_lower_leg");
            IBone footLeft=model.getAnimationProcessor().getBone("foot_left");
            IBone footRight=model.getAnimationProcessor().getBone("foot_right");
            IBone torso=model.getAnimationProcessor().getBone("upper_torso");
            IBone head=model.getAnimationProcessor().getBone("head");
            IBone spoonLeft=model.getAnimationProcessor().getBone("spoon_left");
            IBone spoonRight=model.getAnimationProcessor().getBone("spoon_right");
            IBone eyeLeft=model.getAnimationProcessor().getBone("left_eye");
            IBone eyeRight=model.getAnimationProcessor().getBone("right_eye");
            if(upperLeft==null||upperRight==null||kneeLeft==null||kneeRight==null||footLeft==null||footRight==null
                    ||torso==null||head==null||spoonLeft==null||spoonRight==null||eyeLeft==null||eyeRight==null)
                throw new IllegalStateException("Alakazam meditation rig is missing a required body, limb, spoon, or eye bone");
            if(Math.abs(upperLeft.getRotationX()-(float)Math.toRadians(70.0D))>.12F
                    ||Math.abs(upperRight.getRotationX()-(float)Math.toRadians(70.0D))>.12F
                    ||Math.abs(kneeLeft.getRotationX()-(float)Math.toRadians(100.0D))>.12F
                    ||Math.abs(kneeRight.getRotationX()-(float)Math.toRadians(100.0D))>.12F
                    ||Math.abs(footLeft.getRotationX()-(float)Math.toRadians(-80.0D))>.12F
                    ||Math.abs(footRight.getRotationX()-(float)Math.toRadians(-80.0D))>.12F)
                throw new IllegalStateException("Alakazam's rendered meditation leg pose did not apply the forward-folded thigh, knee, and foot rotations: upper="+upperLeft.getRotationX()+","+upperRight.getRotationX()+" knee="+kneeLeft.getRotationX()+","+kneeRight.getRotationX()+" feet="+footLeft.getRotationX()+","+footRight.getRotationX());
            if(eyeLeft.isHidden()||eyeRight.isHidden()||Math.abs(head.getRotationX())>.18F
                    ||torso.getRotationX()<.20F||torso.getRotationX()>.42F
                    ||Math.abs(spoonLeft.getRotationX())<.10F||Math.abs(spoonRight.getRotationX())<.10F)
                throw new IllegalStateException("Alakazam's rendered upper body, open eyes, or spoons do not match its awake meditation pose");
            poseSeen=true;
        }
        if(mob.isSleeping()){
            String original=SpeciesAssetIndex.clip("alakazam","night_sleep","night_sleep");
            if(original.equals(mob.getSelectedAnimationForValidation())){
                IBone lidLeft=model.getAnimationProcessor().getBone("left_eyelid");
                IBone lidRight=model.getAnimationProcessor().getBone("right_eyelid");
                if(lidLeft!=null&&lidRight!=null
                        &&Math.abs(lidLeft.getPositionX()-.1F)<.04F&&Math.abs(lidRight.getPositionX()+.1F)<.04F
                        &&Math.abs(lidLeft.getPositionZ()+.1F)<.04F&&Math.abs(lidRight.getPositionZ()+.1F)<.04F)
                    sleepPoseSeen=true;
            }
        }
    }

    static void reopenTick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        try{
            if(!opened&&mc.screen instanceof TitleScreen){
                opened=true;
                Object flows=Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);
                for(var method:flows.getClass().getMethods())if(method.getName().equals("loadLevel")){
                    if(method.getParameterCount()==2&&method.getParameterTypes()[1]==String.class){method.invoke(flows,mc.screen,"minemon-validation");break;}
                    if(method.getParameterCount()==1&&method.getParameterTypes()[0]==String.class){method.invoke(flows,"minemon-validation");break;}
                }
                return;
            }
            if(opened&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null&&mc.screen==null&&++ticks>=60){
                var box=mc.player.getBoundingBox().inflate(128.0D);
                boolean kadabra=mc.level.getEntitiesOfClass(MinemonCreature.class,box,m->m.getSpeciesSlug().equals("kadabra")).size()==1;
                boolean alakazam=mc.level.getEntitiesOfClass(MinemonCreature.class,box,m->m.getSpeciesSlug().equals("alakazam")).size()==1;
                if(!kadabra||!alakazam)throw new IllegalStateException("Saved world reopened without Kadabra and Alakazam: kadabra="+kadabra+" alakazam="+alakazam);
                Files.writeString(Path.of("psychic-world-reopen-pass.txt"),"PASS PSYCHIC WORLD REOPEN\n",java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
                mc.stop();
            }
        }catch(Exception ex){throw new IllegalStateException("Psychic world reopen validation failed",ex);}
    }
}
