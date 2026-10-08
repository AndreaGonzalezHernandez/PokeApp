package com.cesba.pokeapp;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Pokemon {
    private int id;
    private String name;
    private String url;
    private int height;
    private int weight;
    private Sprites sprites;
    private List<PokemonStat> stats;

    @SerializedName("types")
    private List<TypeSlot> types;

    // --- NUEVO: Atributo para obtener la lista de movimientos ---
    @SerializedName("moves")
    private List<MoveEntry> moves;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getHeight() {
        return height;
    }

    public int getWeight() {
        return weight;
    }

    public Sprites getSprites() {
        return sprites;
    }

    public List<PokemonStat> getStats() {
        return stats;
    }

    public List<TypeSlot> getTypes() {
        return types;
    }

    // --- NUEVO: Getter para los movimientos ---
    public List<MoveEntry> getMoves() {
        return moves;
    }

    public String getImageUrl() {
        if (url != null && !url.isEmpty()) {
            String[] parts = url.split("/");
            String idStr = parts[parts.length - 1];
            return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + idStr + ".png";
        }
        return "";
    }

    // --- NUEVO: Clases internas para la estructura JSON de 'moves' de PokeAPI ---
    public static class MoveEntry {
        @SerializedName("move")
        private MoveDetail move;

        public MoveDetail getMove() {
            return move;
        }
    }

    public static class MoveDetail {
        private String name;

        public String getName() {
            return name;
        }
    }

    public static class TypeSlot {
        private TypeInfo type;

        public TypeInfo getType() {
            return type;
        }
    }

    public static class TypeInfo {
        private String name;

        public String getName() {
            return name;
        }
    }
}
