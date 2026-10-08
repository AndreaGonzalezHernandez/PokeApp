package com.cesba.pokeapp;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface PokeApiService {

    // Tu método original para obtener datos base del Pokémon (stats, sprites, moves)
    @GET("pokemon/{pokemon}")
    Call<Pokemon> getPokemon(
            @Path("pokemon") String pokemon
    );

    // NUEVO: Obtener la especie para conocer la pre-evolución y el enlace a la cadena evolutiva
    @GET("pokemon-species/{id}")
    Call<PokemonSpecies> getPokemonSpecies(
            @Path("id") int id
    );

    // NUEVO: Obtener la cadena evolutiva completa usando la URL devuelta por la especie
    @GET
    Call<EvolutionChainResponse> getEvolutionChain(
            @Url String url
    );

    // Detalle de un movimiento (poder, precisión, tipo, nombre en español)
    @GET("move/{move}")
    Call<PokeMove> getMove(
            @Path("move") String move
    );
}
