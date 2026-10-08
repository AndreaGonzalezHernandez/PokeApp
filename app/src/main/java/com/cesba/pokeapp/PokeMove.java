package com.cesba.pokeapp;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Respuesta de https://pokeapi.co/api/v2/move/{nombre}
public class PokeMove {
    private String name;
    private Integer power;
    private Integer accuracy;
    private NamedRef type;

    @SerializedName("damage_class")
    private NamedRef damageClass;

    private List<LocalName> names;

    public String getName() { return name; }
    public Integer getPower() { return power; }
    public Integer getAccuracy() { return accuracy; }
    public String getTypeName() { return type != null ? type.name : "normal"; }
    public String getDamageClass() { return damageClass != null ? damageClass.name : "physical"; }

    public String getSpanishName() {
        if (names != null) {
            for (LocalName n : names) {
                if (n.language != null && "es".equals(n.language.name)) return n.name;
            }
        }
        return name.replace("-", " ");
    }

    public static class NamedRef {
        private String name;
    }

    public static class LocalName {
        private String name;
        private NamedRef language;
    }
}
