package com.cesba.pokeapp;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Lectura de los favoritos guardados por MainActivity con el formato "#25 - PIKACHU"
public final class Favoritos {

    public static final String PREFS_NAME = "PokeAppFavorites";
    public static final String FAVORITES_KEY = "fav_list";

    public static class Item {
        public final int id;
        public final String nombre;
        final String clave;

        Item(int id, String nombre, String clave) {
            this.id = id;
            this.nombre = nombre;
            this.clave = clave;
        }
    }

    private Favoritos() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static List<Item> leer(Context c) {
        List<Item> lista = new ArrayList<>();
        for (String s : prefs(c).getStringSet(FAVORITES_KEY, new HashSet<>())) {
            try {
                int sep = s.indexOf(" - ");
                int id = Integer.parseInt(s.substring(1, sep));
                lista.add(new Item(id, s.substring(sep + 3), s));
            } catch (Exception ignored) {
                // Entrada con formato inesperado: se ignora
            }
        }
        Collections.sort(lista, (a, b) -> Integer.compare(a.id, b.id));
        return lista;
    }

    public static void quitar(Context c, Item item) {
        Set<String> set = new HashSet<>(prefs(c).getStringSet(FAVORITES_KEY, new HashSet<>()));
        set.remove(item.clave);
        prefs(c).edit().putStringSet(FAVORITES_KEY, set).apply();
    }
}
