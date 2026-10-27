package br.com.petrickfaria.minemon.pokec;
import net.minecraft.world.item.Item;
public final class SoulItem extends Item {
    private final SoulType type;
    public SoulItem(SoulType type,Properties properties){super(properties);this.type=type;}
    public SoulType soulType(){return type;}
}
