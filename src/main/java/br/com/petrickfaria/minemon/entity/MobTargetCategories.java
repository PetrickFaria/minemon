package br.com.petrickfaria.minemon.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.Set;
import java.util.Comparator;

/** Visual filters only; target overrides retain their original registry keys. */
public final class MobTargetCategories {
    public enum Category { PASSIVE, NEUTRAL, HOSTILE }
    public enum Origin { VANILLA, MINEMON, MODS }
    private static final Set<String> NEUTRAL=Set.of("bee","cave_spider","dolphin","enderman","goat","iron_golem","llama","trader_llama","panda","piglin","polar_bear","spider","wolf","zombified_piglin");
    public static Category category(ResourceLocation id,EntityType<?> type){
        if(id.getNamespace().equals("minecraft") && NEUTRAL.contains(id.getPath()))return Category.NEUTRAL;
        return type.getCategory()==MobCategory.MONSTER?Category.HOSTILE:Category.PASSIVE;
    }
    public static Origin origin(ResourceLocation id){
        String namespace=id.getNamespace();
        if(namespace.equals("minecraft"))return Origin.VANILLA;
        // "minemon" is the established namespace; current registry IDs use the
        // Forge mod id minemon_gen1, which must resolve to the same source group.
        if(namespace.equals("minemon")||namespace.equals("minemon_gen1"))return Origin.MINEMON;
        return Origin.MODS;
    }
    /** National Pokédex ordering where a species has a published number. */
    public static int nationalDexNumber(ResourceLocation id){
        SpeciesDefinitions.Definition definition=SpeciesDefinitions.get(id.getPath());
        return origin(id)==Origin.MINEMON&&definition!=null?definition.nationalPokedexNumber():-1;
    }
    public static Comparator<ResourceLocation> numericIdOrder(){
        return (left,right)->{
            int leftNumber=nationalDexNumber(left),rightNumber=nationalDexNumber(right);
            if(leftNumber>=0||rightNumber>=0){
                if(leftNumber<0)return 1;
                if(rightNumber<0)return -1;
                int byNumber=Integer.compare(leftNumber,rightNumber);
                if(byNumber!=0)return byNumber;
            }
            return left.compareTo(right);
        };
    }
    public static boolean defaultSelected(EntityType<?> type){return type!=EntityType.CREEPER && type.getCategory()==MobCategory.MONSTER;}
    private MobTargetCategories(){}
}
