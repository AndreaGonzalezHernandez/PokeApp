package com.cesba.pokeapp;

import com.google.gson.annotations.SerializedName;

public class PokemonSpecies {
    @SerializedName("evolution_chain")
    private EvolutionChainUrl evolutionChain;

    @SerializedName("evolves_from_species")
    private EvolvesFrom evolvesFrom;

    public EvolutionChainUrl getEvolutionChain() { return evolutionChain; }
    public EvolvesFrom getEvolvesFrom() { return evolvesFrom; }

    public static class EvolutionChainUrl {
        private String url;
        public String getUrl() { return url; }
    }

    public static class EvolvesFrom {
        private String name;
        public String getName() { return name; }
    }
}