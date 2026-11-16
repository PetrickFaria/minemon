package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.block.PokecBlock;
import br.com.petrickfaria.minemon.client.PokecEmissiveModel;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Real baked-quad and screenshot checks in daylight, night, enclosure and lamp light. */
final class PokecGlowClientSmokeTest {
    private static final String[] NAMES={"day-north","night-north","dark-north","dark-east","dark-south","dark-west","ambient-north"};
    private static int stage,ticks;
    private static boolean requested,finished;
    private static volatile boolean ready;
    private static ArmorStand camera;

    static boolean tick(Minecraft mc,BlockPos base)throws Exception{
        if(stage>=NAMES.length)return true;
        if(ticks++==0){
            ready=false;
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();level.setDayTime(stage==0?6000:18000);
                if(stage==2)for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=0;y<=7;y++)
                    if(y==7||x==-8||x==8||z==-8||z==8)level.setBlockAndUpdate(base.offset(x,y,z),Blocks.STONE.defaultBlockState());
                if(stage==6)level.setBlockAndUpdate(base.offset(3,2,-2),Blocks.GLOWSTONE.defaultBlockState());
                Direction facing=switch(stage){case 3->Direction.EAST;case 4->Direction.SOUTH;case 5->Direction.WEST;default->Direction.NORTH;};
                level.setBlockAndUpdate(base,ModBlocks.POKEC.get().defaultBlockState().setValue(PokecBlock.HALF,DoubleBlockHalf.LOWER).setValue(PokecBlock.FACING,facing));
                level.setBlockAndUpdate(base.above(),ModBlocks.POKEC.get().defaultBlockState().setValue(PokecBlock.HALF,DoubleBlockHalf.UPPER).setValue(PokecBlock.FACING,facing));
                ready=true;
            });
            if(camera==null)camera=new ArmorStand(mc.level,0,0,0);
            double x=stage==3?5:stage==5?-5:2.8,z=stage==4?5:stage==3||stage==5?2.8:-5;
            float yaw=(float)Math.toDegrees(Math.atan2(x,-z));
            float pitch=(float)Math.toDegrees(Math.atan2(1.2,Math.hypot(x,z)));
            camera.moveTo(base.getX()+.5+x,base.getY()+2.2-camera.getEyeHeight(),base.getZ()+.5+z,yaw,pitch);
            camera.yRotO=yaw;camera.xRotO=pitch;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;
            mc.setCameraEntity(camera);mc.options.hideGui=true;return false;
        }
        if(!ready||ticks<100)return false;
        if(stage>=2&&stage<=5&&mc.level.getBrightness(LightLayer.SKY,base.above(2))!=0)
            throw new IllegalStateException("Pokec dark enclosure still receives sky light");
        int glowing=0,normal=0,sideGlowing=0;
        for(BlockPos pos:List.of(base,base.above())){
            var state=mc.level.getBlockState(pos);var model=mc.getBlockRenderer().getBlockModel(state);
            if(!(model instanceof PokecEmissiveModel))throw new IllegalStateException("Placed Pokec did not use the selective emissive model");
            var all=new ArrayList<BakedQuad>(model.getQuads(state,null,net.minecraft.util.RandomSource.create(0)));
            for(Direction side:Direction.values())all.addAll(model.getQuads(state,side,net.minecraft.util.RandomSource.create(0)));
            for(var quad:all){
                int[] vertices=quad.getVertices();int stride=vertices.length/4;
                boolean full=true;for(int v=0;v<4;v++)full&=vertices[v*stride+6]==LightTexture.FULL_BRIGHT;
                String texture=quad.getSprite().getName().getPath();
                boolean selected=texture.equals("block/pokec_screen")||texture.equals("block/pokec_glow");
                if(texture.equals("block/pokec_side")){
                    selected=true;
                    for(int v=0;v<4;v++){
                        float u=Float.intBitsToFloat(vertices[v*stride+4]),w=Float.intBitsToFloat(vertices[v*stride+5]);
                        selected&=u>=quad.getSprite().getU(10)-1e-7&&u<=quad.getSprite().getU(12)+1e-7
                                &&w>=quad.getSprite().getV(6)-1e-7&&w<=quad.getSprite().getV(8)+1e-7;
                    }
                    if(selected)sideGlowing++;
                }
                if(full!=selected)throw new IllegalStateException("Incorrect Pokec emissive selection: "+texture+" selected="+selected+" full="+full);
                if(selected){glowing++;if(quad.isShade())throw new IllegalStateException("Emissive quad retains directional shading");}else normal++;
            }
        }
        if(glowing==0||normal==0||sideGlowing==0)throw new IllegalStateException("Incomplete emissive/ordinary/side coverage");
        String file="pokec-glow-"+NAMES[stage]+".png";
        if(!requested){requested=true;net.minecraft.client.Screenshot.grab(mc.gameDirectory,file,mc.getMainRenderTarget(),message->finished=true);return false;}
        if(!finished)return false;
        if(!Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(file)))throw new IllegalStateException("Missing glow screenshot "+file);
        Files.writeString(Path.of("client-validation-pokec-glow.txt"),"PASS POKEC GLOW "+NAMES[stage]+": glowing="+glowing+" ordinary="+normal+" side patches="+sideGlowing+" sky="+mc.level.getBrightness(LightLayer.SKY,base.above(2))+"; original textures/geometry and inventory sprite retained.\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        stage++;ticks=0;requested=false;finished=false;return stage>=NAMES.length;
    }
}
