package br.com.petrickfaria.minemon.item;

import net.minecraft.world.item.Item;

public final class PokemonTreatItem extends Item {
    private final PokemonTreatType treatType;
    public PokemonTreatItem(PokemonTreatType treatType, Properties properties) {
        super(properties);
        this.treatType = treatType;
    }
    public PokemonTreatType getTreatType() { return treatType; }
}
