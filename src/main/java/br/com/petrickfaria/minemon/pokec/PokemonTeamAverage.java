package br.com.petrickfaria.minemon.pokec;

import java.util.Collection;

/** Team level calculation used as the basis for owner-bound wild encounters. */
public final class PokemonTeamAverage {
    public static int floorAverage(Collection<Integer> levels,int fallback){
        if(levels==null||levels.isEmpty())return Math.max(1,fallback);
        long total=0;int count=0;
        for(Integer level:levels)if(level!=null&&level>0){total+=level;count++;}
        return count==0?Math.max(1,fallback):(int)(total/count);
    }
    private PokemonTeamAverage(){}
}
