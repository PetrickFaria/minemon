package br.com.petrickfaria.minemon;
import com.google.gson.JsonParser;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArbokPatternAssetTest {
 @Test public void classicPatternUsesTheVisibleFrontFaceOfTheCurrentHood() throws Exception {
  var root=Path.of("src/main/resources/assets/minemon");
  var geo=JsonParser.parseString(Files.readString(root.resolve("geo/arbok_cobblemon_1_8_2/arbok.geo.json")))
      .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
  com.google.gson.JsonObject hood3=null,panel=null;
  for(var bone:geo.getAsJsonArray("bones"))if(bone.getAsJsonObject().get("name").getAsString().equals("hood3")){
   hood3=bone.getAsJsonObject();break;
  }
  assertNotNull("Current Arbok rig contains hood3",hood3);
  for(var cube:hood3.getAsJsonArray("cubes")){
   var candidate=cube.getAsJsonObject();var uv=candidate.getAsJsonArray("uv");var size=candidate.getAsJsonArray("size");
   if(uv.get(0).getAsInt()==52&&uv.get(1).getAsInt()==109&&size.get(0).getAsInt()==26
       &&size.get(1).getAsInt()==7&&size.get(2).getAsInt()==12){panel=candidate;break;}
  }
  assertNotNull("Use the textured hood volume, not the thin top decal slab",panel);
  // Bedrock's north (-Z/front) face begins one depth past the cube UV origin.
  int frontX=panel.getAsJsonArray("uv").get(0).getAsInt()+panel.getAsJsonArray("size").get(2).getAsInt();
  int frontY=panel.getAsJsonArray("uv").get(1).getAsInt()+panel.getAsJsonArray("size").get(2).getAsInt();
  assertEquals(64,frontX);assertEquals(121,frontY);
  assertEquals("Arbok must load the atlas containing the mapped marking",
      "assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png",SpeciesDefinitions.get("arbok").texture());
  var texture=javax.imageio.ImageIO.read(root.resolve("textures/pokemon/arbok_cobblemon_1_8_2/arbok.png").toFile());
  assertEquals(128,texture.getWidth());assertEquals(128,texture.getHeight());
  int redEyes=0,goldEyes=0;
  for(int y=frontY;y<frontY+7;y++)for(int x=frontX;x<frontX+26;x++){
   int color=texture.getRGB(x,y),r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
   if(r>190&&g<100&&b<90)redEyes++;
   if(r>190&&g>120&&b<80)goldEyes++;
  }
  assertTrue("Visible front hood UV must include the red eyes",redEyes>=2);
  assertTrue("Visible front hood UV must include the gold eye centers",goldEyes>=2);
  for(int y=19;y<31;y++)for(int x=12;x<36;x++){
   int color=texture.getRGB(x,y),r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
   assertFalse("The old mis-mapped top face must not hold the eyes",r>190&&g>120&&b<80);
  }
 }
}
