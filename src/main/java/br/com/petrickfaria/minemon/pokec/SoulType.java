package br.com.petrickfaria.minemon.pokec;

import java.util.Locale;
public enum SoulType {
    NORMAL, FIRE, WATER, GRASS, ELECTRIC, ICE, FIGHTING, POISON, GROUND, FLYING, PSYCHIC, BUG, ROCK, GHOST, DRAGON, FAIRY, STEEL, DARK;
    public static SoulType fromSpeciesType(String type){
        try{return valueOf(type.toUpperCase(Locale.ROOT));}catch(Exception e){return null;}
    }
    public String id(){return name().toLowerCase(Locale.ROOT);}
}
