package com.cesba.pokeapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Pokémon listo para combatir: stats base, tipos y sus 4 movimientos (se guarda con Gson)
public class BattlePokemon {

    public static final String SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";
    public static final int NIVEL = 100;
    private static final int MAX_CANDIDATOS = 14;

    public int id;
    public String name;
    public List<String> types = new ArrayList<>();
    public int baseHp = 50, baseAtk = 50, baseDef = 50, baseSpAtk = 50, baseSpDef = 50, baseSpd = 50;
    public List<BattleMove> moves = new ArrayList<>();

    public static class BattleMove {
        public String name;
        public String displayName;
        public String type;
        public int power;
        public int accuracy; // 0 = nunca falla
        public boolean physical;

        BattleMove(String name, String displayName, String type, int power, int accuracy, boolean physical) {
            this.name = name;
            this.displayName = displayName;
            this.type = type;
            this.power = power;
            this.accuracy = accuracy;
            this.physical = physical;
        }
    }

    public interface Listener {
        void onListo(BattlePokemon pokemon);
        void onError(String mensaje);
    }

    // ---------------- STATS A NIVEL 100 (IV 31, naturaleza neutra) ----------------

    public int maxHp() { return 2 * baseHp + 31 + NIVEL + 10; }
    public int atk() { return stat(baseAtk); }
    public int def() { return stat(baseDef); }
    public int spAtk() { return stat(baseSpAtk); }
    public int spDef() { return stat(baseSpDef); }
    public int spd() { return stat(baseSpd); }

    private int stat(int base) { return 2 * base + 31 + 5; }

    public String frontUrl() { return SPRITES + id + ".png"; }
    public String backUrl() { return SPRITES + "back/" + id + ".png"; }
    public String artworkUrl() { return SPRITES + "other/official-artwork/" + id + ".png"; }

    // ---------------- CARGA DESDE POKEAPI ----------------

    public static void cargar(String query, Listener listener) {
        PokeApiService api = ApiClient.getClient().create(PokeApiService.class);
        api.getPokemon(query).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Pokemon p = response.body();
                    BattlePokemon bp = desdePokemon(p);
                    cargarMovimientos(api, bp, p, listener);
                } else {
                    listener.onError("Pokémon no encontrado");
                }
            }

            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) {
                listener.onError("Error de red al consultar la PokeAPI");
            }
        });
    }

    private static BattlePokemon desdePokemon(Pokemon p) {
        BattlePokemon bp = new BattlePokemon();
        bp.id = p.getId();
        bp.name = p.getName().toUpperCase();
        if (p.getTypes() != null) {
            for (Pokemon.TypeSlot t : p.getTypes()) bp.types.add(t.getType().getName());
        }
        if (p.getStats() != null) {
            for (PokemonStat s : p.getStats()) {
                int v = s.getBaseStat();
                switch (s.getStat().getName()) {
                    case "hp": bp.baseHp = v; break;
                    case "attack": bp.baseAtk = v; break;
                    case "defense": bp.baseDef = v; break;
                    case "special-attack": bp.baseSpAtk = v; break;
                    case "special-defense": bp.baseSpDef = v; break;
                    case "speed": bp.baseSpd = v; break;
                }
            }
        }
        return bp;
    }

    // Consulta varios movimientos al azar (fijo por Pokémon) y se queda con 4 que hagan daño
    private static void cargarMovimientos(PokeApiService api, BattlePokemon bp, Pokemon p, Listener listener) {
        List<String> nombres = new ArrayList<>();
        if (p.getMoves() != null) {
            for (Pokemon.MoveEntry m : p.getMoves()) nombres.add(m.getMove().getName());
        }
        Collections.shuffle(nombres, new Random(bp.id));
        List<String> candidatos = nombres.subList(0, Math.min(MAX_CANDIDATOS, nombres.size()));

        if (candidatos.isEmpty()) {
            elegirMovimientos(bp, new PokeMove[0]);
            listener.onListo(bp);
            return;
        }

        PokeMove[] resultados = new PokeMove[candidatos.size()];
        int[] pendientes = {candidatos.size()};
        for (int i = 0; i < candidatos.size(); i++) {
            final int indice = i;
            api.getMove(candidatos.get(i)).enqueue(new Callback<PokeMove>() {
                @Override
                public void onResponse(Call<PokeMove> call, Response<PokeMove> response) {
                    if (response.isSuccessful()) resultados[indice] = response.body();
                    terminar();
                }

                @Override
                public void onFailure(Call<PokeMove> call, Throwable t) {
                    terminar();
                }

                private void terminar() {
                    if (--pendientes[0] == 0) {
                        elegirMovimientos(bp, resultados);
                        listener.onListo(bp);
                    }
                }
            });
        }
    }

    private static void elegirMovimientos(BattlePokemon bp, PokeMove[] detalles) {
        List<BattleMove> ofensivos = new ArrayList<>();
        for (PokeMove m : detalles) {
            if (m == null || m.getPower() == null || m.getPower() <= 0) continue;
            if ("status".equals(m.getDamageClass())) continue;
            ofensivos.add(new BattleMove(m.getName(), m.getSpanishName(), m.getTypeName(), m.getPower(),
                    m.getAccuracy() == null ? 0 : m.getAccuracy(), "physical".equals(m.getDamageClass())));
        }

        bp.moves.clear();
        // Primero uno del mismo tipo del Pokémon (STAB) si existe
        for (BattleMove m : ofensivos) {
            if (bp.types.contains(m.type)) {
                bp.moves.add(m);
                break;
            }
        }
        for (BattleMove m : ofensivos) {
            if (bp.moves.size() >= 4) break;
            if (!bp.moves.contains(m)) bp.moves.add(m);
        }

        // Relleno para Pokémon con pocos movimientos ofensivos (ej. Ditto, Magikarp)
        BattleMove[] relleno = {
                new BattleMove("tackle", "Placaje", "normal", 40, 100, true),
                new BattleMove("headbutt", "Golpe Cabeza", "normal", 70, 100, true),
                new BattleMove("body-slam", "Golpe Cuerpo", "normal", 85, 100, true),
                new BattleMove("take-down", "Derribo", "normal", 90, 85, true)
        };
        for (BattleMove r : relleno) {
            if (bp.moves.size() >= 4) break;
            boolean repetido = false;
            for (BattleMove m : bp.moves) if (m.name.equals(r.name)) repetido = true;
            if (!repetido) bp.moves.add(r);
        }
    }
}
