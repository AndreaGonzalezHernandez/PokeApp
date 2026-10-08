package com.cesba.pokeapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tabla de efectividad de tipos (Gen 6+), colores y nombres en español
public final class TypeChart {

    private static final Map<String, Map<String, Double>> CHART = new HashMap<>();

    static {
        set("normal", 0.5, "rock", "steel");
        set("normal", 0, "ghost");
        set("fire", 2, "grass", "ice", "bug", "steel");
        set("fire", 0.5, "fire", "water", "rock", "dragon");
        set("water", 2, "fire", "ground", "rock");
        set("water", 0.5, "water", "grass", "dragon");
        set("electric", 2, "water", "flying");
        set("electric", 0.5, "electric", "grass", "dragon");
        set("electric", 0, "ground");
        set("grass", 2, "water", "ground", "rock");
        set("grass", 0.5, "fire", "grass", "poison", "flying", "bug", "dragon", "steel");
        set("ice", 2, "grass", "ground", "flying", "dragon");
        set("ice", 0.5, "fire", "water", "ice", "steel");
        set("fighting", 2, "normal", "ice", "rock", "dark", "steel");
        set("fighting", 0.5, "poison", "flying", "psychic", "bug", "fairy");
        set("fighting", 0, "ghost");
        set("poison", 2, "grass", "fairy");
        set("poison", 0.5, "poison", "ground", "rock", "ghost");
        set("poison", 0, "steel");
        set("ground", 2, "fire", "electric", "poison", "rock", "steel");
        set("ground", 0.5, "grass", "bug");
        set("ground", 0, "flying");
        set("flying", 2, "grass", "fighting", "bug");
        set("flying", 0.5, "electric", "rock", "steel");
        set("psychic", 2, "fighting", "poison");
        set("psychic", 0.5, "psychic", "steel");
        set("psychic", 0, "dark");
        set("bug", 2, "grass", "psychic", "dark");
        set("bug", 0.5, "fire", "fighting", "poison", "flying", "ghost", "steel", "fairy");
        set("rock", 2, "fire", "ice", "flying", "bug");
        set("rock", 0.5, "fighting", "ground", "steel");
        set("ghost", 2, "psychic", "ghost");
        set("ghost", 0.5, "dark");
        set("ghost", 0, "normal");
        set("dragon", 2, "dragon");
        set("dragon", 0.5, "steel");
        set("dragon", 0, "fairy");
        set("dark", 2, "psychic", "ghost");
        set("dark", 0.5, "fighting", "dark", "fairy");
        set("steel", 2, "ice", "rock", "fairy");
        set("steel", 0.5, "fire", "water", "electric", "steel");
        set("fairy", 2, "fighting", "dragon", "dark");
        set("fairy", 0.5, "fire", "poison", "steel");
    }

    private TypeChart() {}

    private static void set(String atk, double mult, String... defs) {
        Map<String, Double> fila = CHART.get(atk);
        if (fila == null) {
            fila = new HashMap<>();
            CHART.put(atk, fila);
        }
        for (String d : defs) fila.put(d, mult);
    }

    public static double efectividad(String tipoAtaque, List<String> tiposDefensor) {
        double total = 1.0;
        Map<String, Double> fila = CHART.get(tipoAtaque);
        if (fila == null || tiposDefensor == null) return total;
        for (String d : tiposDefensor) {
            Double m = fila.get(d);
            if (m != null) total *= m;
        }
        return total;
    }

    public static int color(String tipo) {
        switch (tipo == null ? "" : tipo) {
            case "fire": return 0xFFF97316;
            case "water": return 0xFF3B82F6;
            case "grass": return 0xFF22C55E;
            case "electric": return 0xFFEAB308;
            case "psychic": return 0xFFEC4899;
            case "ice": return 0xFF22D3EE;
            case "dragon": return 0xFF6366F1;
            case "dark": return 0xFF475569;
            case "fairy": return 0xFFF472B6;
            case "fighting": return 0xFFDC2626;
            case "poison": return 0xFFA855F7;
            case "ground": return 0xFFB45309;
            case "rock": return 0xFF78716C;
            case "bug": return 0xFF84CC16;
            case "ghost": return 0xFF7C3AED;
            case "steel": return 0xFF64748B;
            case "flying": return 0xFF38BDF8;
            default: return 0xFF94A3B8;
        }
    }

    public static String nombre(String tipo) {
        switch (tipo == null ? "" : tipo) {
            case "fire": return "FUEGO";
            case "water": return "AGUA";
            case "grass": return "PLANTA";
            case "electric": return "ELÉCTRICO";
            case "psychic": return "PSÍQUICO";
            case "ice": return "HIELO";
            case "dragon": return "DRAGÓN";
            case "dark": return "SINIESTRO";
            case "fairy": return "HADA";
            case "fighting": return "LUCHA";
            case "poison": return "VENENO";
            case "ground": return "TIERRA";
            case "rock": return "ROCA";
            case "bug": return "BICHO";
            case "ghost": return "FANTASMA";
            case "steel": return "ACERO";
            case "flying": return "VOLADOR";
            default: return "NORMAL";
        }
    }
}
