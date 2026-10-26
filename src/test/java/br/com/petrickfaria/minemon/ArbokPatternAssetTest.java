package br.com.petrickfaria.minemon;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import com.google.gson.JsonParser;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArbokPatternAssetTest {
 @Test public void classicPatternUsesTheHoodPanelFaceUvs() throws Exception {
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
   if(uv.get(0).getAsInt()==0&&uv.get(1).getAsInt()==19&&size.get(0).getAsInt()==24
       &&size.get(1).getAsInt()==1&&size.get(2).getAsInt()==12){panel=candidate;break;}
  }
  assertNotNull("The thin hood panel supplies the broad face for the classic marking",panel);
  // Bedrock cube UVs map the underside of this 24x1x12 panel to x=36..60,
  // y=19..31. Center the 19x12 classic artwork there with a one-pixel margin.
  int faceX=panel.getAsJsonArray("uv").get(0).getAsInt()+panel.getAsJsonArray("size").get(0).getAsInt()+panel.getAsJsonArray("size").get(2).getAsInt();
  int faceY=panel.getAsJsonArray("uv").get(1).getAsInt();
  assertEquals(36,faceX);assertEquals(19,faceY);
  assertEquals("Arbok must load the atlas containing the mapped marking",
      "assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png",SpeciesDefinitions.get("arbok").texture());
  var texture=javax.imageio.ImageIO.read(root.resolve("textures/pokemon/arbok_cobblemon_1_8_2/arbok.png").toFile());
  assertEquals(128,texture.getWidth());assertEquals(128,texture.getHeight());
  var original=javax.imageio.ImageIO.read(root.resolve("textures/pokemon/arbok/arbok_pattern_classic.png").toFile());
  // Original hood: size 19x0x12, UV (77,52). Its underside starts at
  // u + depth + width = 108. The old 32x20 crop mixed unrelated atlas faces.
  var oldGeo=JsonParser.parseString(Files.readString(root.resolve("geo/arbok/arbok.geo.json")))
      .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
  com.google.gson.JsonObject sourcePanel=null;
  for(var bone:oldGeo.getAsJsonArray("bones"))if(bone.getAsJsonObject().get("name").getAsString().equals("hood"))
   for(var cube:bone.getAsJsonObject().getAsJsonArray("cubes")){
    var c=cube.getAsJsonObject();var size=c.getAsJsonArray("size");
    if(size.get(0).getAsInt()==19&&size.get(1).getAsInt()==0&&size.get(2).getAsInt()==12)sourcePanel=c;
   }
  assertNotNull("Original hood panel determines the source pattern UV",sourcePanel);
  var sourceUv=sourcePanel.getAsJsonArray("uv");var sourceSize=sourcePanel.getAsJsonArray("size");
  int sourceX=sourceUv.get(0).getAsInt()+sourceSize.get(0).getAsInt()+sourceSize.get(2).getAsInt();
  int sourceY=sourceUv.get(1).getAsInt();
  assertEquals(108,sourceX);assertEquals(52,sourceY);
  var expected=original.getSubimage(sourceX,sourceY,19,12);
  int marked=0;
  for(int y=0;y<12;y++)for(int x=0;x<19;x++){
   int source=expected.getRGB(x,y);
   if((source>>>24)<254)continue;
   marked++;
   int actual=texture.getRGB(38+x,19+y);
   for(int shift:new int[]{16,8,0})assertTrue("Classic marking must be mapped onto the hood panel at "+x+","+y,
       Math.abs(((source>>>shift)&255)-((actual>>>shift)&255))<=1);
  }
  assertTrue("The mapped classic marking has visible artwork",marked>40);
 }
}
