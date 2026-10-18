package br.com.petrickfaria.minemon.entity;
/** Runtime paths are selected explicitly, never by the first variant in a catalog. */
public final class SpeciesAssetIndex {
 public static String resource(String slug,String category,String fallback) {
  var d=SpeciesDefinitions.get(slug);if(d==null)return fallback;
  String path=switch(category){case "models"->d.model();case "textures"->d.texture();case "animations"->d.animation();default->null;};
  return path!=null&&path.startsWith("assets/minemon/")?path.substring("assets/minemon/".length()):fallback;
 }
 public static String clip(String slug,String preferred,String fallback) {
  var d=SpeciesDefinitions.get(slug);if(d==null)return null;
  return d.clips().getOrDefault(preferred,d.clips().get(fallback));
 }
 private SpeciesAssetIndex(){}
}
