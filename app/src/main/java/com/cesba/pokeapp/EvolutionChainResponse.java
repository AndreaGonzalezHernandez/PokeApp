package com.cesba.pokeapp;

import com.google.gson.annotations.SerializedName;

public class EvolutionChainResponse {

    @SerializedName("chain")
    private ChainLink chain;

    public ChainLink getChain() {
        return chain;
    }

    public static class ChainLink {
        @SerializedName("species")
        private NamedApiResource species;

        @SerializedName("evolves_to")
        private java.util.List<ChainLink> evolvesTo;

        public NamedApiResource getSpecies() {
            return species;
        }

        public java.util.List<ChainLink> getEvolvesTo() {
            return evolvesTo;
        }
    }

    public static class NamedApiResource {
        @SerializedName("name")
        private String name;

        @SerializedName("url")
        private String url;

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }
}
