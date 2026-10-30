package br.com.petrickfaria.minemon.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.Set;

/** Visual filters only; target overrides retain their original registry keys. */
public final class MobTargetCategories {
    public enum Category { PASSIVE, NEUTRAL, HOSTILE }
    private static final Set<String> NEUTRAL=Set.of("bee","cave_spider","dolphin","enderman","goat","iron_golem","llama","trader_llama","panda","piglin","polar_bear","spider","wolf","zombified_piglin");
    public static Category category(ResourceLocation id,EntityType<?> type){
        if(id.getNamespace().equals("minecraft") && NEUTRAL.contains(id.getPath()))return Category.NEUTRAL;
        return type.getCategory()==MobCategory.MONSTER?Category.HOSTILE:Category.PASSIVE;
    }
    public static boolean defaultSelected(EntityType<?> type){return type!=EntityType.CREEPER && type.getCategory()==MobCategory.MONSTER;}
    private MobTargetCategories(){}
}
