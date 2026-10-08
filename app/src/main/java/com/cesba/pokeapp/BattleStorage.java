package com.cesba.pokeapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

// Guarda en el teléfono los Pokémon elegidos en el Simulador de batalla y el historial de batallas
public final class BattleStorage {

    private static final String PREFS = "PokeAppBattles";
    private static final String KEY_J1 = "emulador_jugador";
    private static final String KEY_J2 = "emulador_rival";
    private static final String KEY_HISTORIAL = "emulador_historial";
    private static final int MAX_HISTORIAL = 30;

    private static final Gson GSON = new Gson();

    public static class BattleRecord {
        public long fecha;
        public String ganador;
        public int ganadorId;
        public String perdedor;
        public int perdedorId;
        public int turnos;
        public int hpRestante;
        public int hpMaximo;
        public String registro;
    }

    private BattleStorage() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void guardarJugador(Context c, int jugador, BattlePokemon p) {
        prefs(c).edit().putString(jugador == 1 ? KEY_J1 : KEY_J2, GSON.toJson(p)).apply();
    }

    public static BattlePokemon leerJugador(Context c, int jugador) {
        String json = prefs(c).getString(jugador == 1 ? KEY_J1 : KEY_J2, null);
        if (json == null) return null;
        try {
            BattlePokemon p = GSON.fromJson(json, BattlePokemon.class);
            return (p != null && p.moves != null && p.moves.size() == 4) ? p : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void guardarBatalla(Context c, BattleRecord r) {
        List<BattleRecord> lista = leerHistorial(c);
        lista.add(0, r);
        while (lista.size() > MAX_HISTORIAL) lista.remove(lista.size() - 1);
        prefs(c).edit().putString(KEY_HISTORIAL, GSON.toJson(lista)).apply();
    }

    public static List<BattleRecord> leerHistorial(Context c) {
        String json = prefs(c).getString(KEY_HISTORIAL, null);
        if (json == null) return new ArrayList<>();
        try {
            List<BattleRecord> lista = GSON.fromJson(json, new TypeToken<List<BattleRecord>>() {}.getType());
            return lista != null ? lista : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void borrarHistorial(Context c) {
        prefs(c).edit().remove(KEY_HISTORIAL).apply();
    }
}
