package br.com.petrickfaria.minemon;
import com.google.gson.JsonParser;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArbokPatternAssetTest {
 @Test public void classicPatternIsOnTheCurrentFrontHoodUvFace() throws Exception {
  var root=Path.of("src/main/resources/assets/minemon");
  var geo=JsonParser.parseString(Files.readString(root.resolve("geo/arbok_cobblemon_1_8_2/arbok.geo.json")))
      .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
  com.google.gson.JsonObject hood3=null;
  for(var bone:geo.getAsJsonArray("bones"))if(bone.getAsJsonObject().get("name").getAsString().equals("hood3")){
   hood3=bone.getAsJsonObject();break;
  }
  assertNotNull("Current Arbok rig contains hood3",hood3);
  com.google.gson.JsonObject panel=null;
  for(var cube:hood3.getAsJsonArray("cubes")){
   var candidate=cube.getAsJsonObject();var uv=candidate.getAsJsonArray("uv");var size=candidate.getAsJsonArray("size");
   if(uv.get(0).getAsInt()==0&&uv.get(1).getAsInt()==19&&size.get(0).getAsInt()==24
       &&size.get(1).getAsInt()==1&&size.get(2).getAsInt()==12){panel=candidate;break;}
  }
  assertNotNull("Current Arbok hood has the expected front UV slab",panel);
  int faceX=panel.getAsJsonArray("uv").get(0).getAsInt()+panel.getAsJsonArray("size").get(2).getAsInt();
  int faceY=panel.getAsJsonArray("uv").get(1).getAsInt();
  assertEquals(12,faceX);assertEquals(19,faceY);
  var texture=javax.imageio.ImageIO.read(root.resolve("textures/pokemon/arbok_cobblemon_1_8_2/arbok.png").toFile());
  assertEquals(128,texture.getWidth());assertEquals(128,texture.getHeight());
  int count=0,minX=128,minY=128,maxX=-1,maxY=-1;
  for(int y=faceY;y<faceY+12;y++)for(int x=faceX;x<faceX+24;x++)if((texture.getRGB(x,y)>>>24)!=0){
   count++;minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
   assertTrue("Arbok mark must stay centered on the projected UV face",x>=14&&x<33);
  }
  assertEquals("Classic overlay pixels belong to the hood's front face",74,count);
  assertEquals(14,minX);assertEquals(19,minY);assertEquals(32,maxX);assertEquals(30,maxY);
  for(int y=19;y<31;y++)for(int x=36;x<60;x++)
   assertEquals("Back of the hood stays unchanged",0,texture.getRGB(x,y)>>>24);
 }
}
