package com.cesba.pokeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";
    private static final String ARTWORK = SPRITES + "other/official-artwork/";
    private static final String CRIES = "https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/latest/";
    private static final String PREFS_NAME = "PokeAppFavorites";
    private static final String FAVORITES_KEY = "fav_list";

    private TextInputEditText etSearchPokemon;
    private Button btnSearchPokemon, btnPlayAudio;
    private TextView tvPokemonName, tvPokemonId, tvType1, tvType2;
    private TextView tvHpVal, tvAttackVal, tvDefenseVal, tvSpeedVal;
    private ImageView imgPokemon, btnFavorite, btnShowFavorites, btnPrevSprite, btnNextSprite;
    private ProgressBar pbHp, pbAttack, pbDefense, pbSpeed;
    private TextView tabDescription, tabMoves, tabEvolution, tvTabContent;
    private LinearLayout heroCard, llEvolution;
    private HorizontalScrollView scrollEvolution;

    private boolean isShiny = false;
    private int idActual = 0;
    private int tabActual = 0;
    private String normalSpriteUrl = "";
    private String shinySpriteUrl = "";
    private String audioCryUrl = "";
    private MediaPlayer mediaPlayer;

    private String currentPokemonName = "";
    private String currentPokemonId = "";
    private String movimientosPokemon = "Cargando movimientos...";
    private String descripcionPokemon = "Selecciona un Pokémon.";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        etSearchPokemon = findViewById(R.id.etSearchPokemon);
        btnSearchPokemon = findViewById(R.id.btnSearchPokemon);
        btnShowFavorites = findViewById(R.id.btnShowFavorites);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        tvPokemonName = findViewById(R.id.tvPokemonName);
        tvPokemonId = findViewById(R.id.tvPokemonId);
        tvType1 = findViewById(R.id.tvType1);
        tvType2 = findViewById(R.id.tvType2);
        imgPokemon = findViewById(R.id.imgPokemon);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnPrevSprite = findViewById(R.id.btnPrevSprite);
        btnNextSprite = findViewById(R.id.btnNextSprite);
        heroCard = findViewById(R.id.heroCard);

        pbHp = findViewById(R.id.pbHp);
        pbAttack = findViewById(R.id.pbAttack);
        pbDefense = findViewById(R.id.pbDefense);
        pbSpeed = findViewById(R.id.pbSpeed);
        tvHpVal = findViewById(R.id.tvHpVal);
        tvAttackVal = findViewById(R.id.tvAttackVal);
        tvDefenseVal = findViewById(R.id.tvDefenseVal);
        tvSpeedVal = findViewById(R.id.tvSpeedVal);

        tabDescription = findViewById(R.id.tabDescription);
        tabMoves = findViewById(R.id.tabMoves);
        tabEvolution = findViewById(R.id.tabEvolution);
        tvTabContent = findViewById(R.id.tvTabContent);
        scrollEvolution = findViewById(R.id.scrollEvolution);
        llEvolution = findViewById(R.id.llEvolution);

        seleccionarTab(0);
        int idInicial = getIntent().getIntExtra(FavoritesActivity.EXTRA_POKEMON_ID, 1);
        buscarPokemon(String.valueOf(idInicial));

        btnSearchPokemon.setOnClickListener(v -> {
            String query = etSearchPokemon.getText().toString().trim().toLowerCase();
            if (!query.isEmpty()) {
                buscarPokemon(query);
            } else {
                Toast.makeText(this, "Escribe el nombre o ID del Pokémon", Toast.LENGTH_SHORT).show();
            }
        });

        btnFavorite.setOnClickListener(v -> toggleFavorito());
        btnShowFavorites.setOnClickListener(v -> mostrarListaFavoritos());
        btnFavorite.setOnLongClickListener(v -> {
            mostrarListaFavoritos();
            return true;
        });

        btnPrevSprite.setOnClickListener(v -> cambiarSprite(false));
        btnNextSprite.setOnClickListener(v -> cambiarSprite(true));
        btnPlayAudio.setOnClickListener(v -> reproducirSonido());

        tabDescription.setOnClickListener(v -> seleccionarTab(0));
        tabMoves.setOnClickListener(v -> seleccionarTab(1));
        tabEvolution.setOnClickListener(v -> seleccionarTab(2));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Pudo haberse quitado de favoritos desde la pantalla de Favoritos
        if (!currentPokemonName.isEmpty()) verificarEstadoFavorito();
    }

    // ---------------- POKÉMON ----------------

    private void buscarPokemon(String query) {
        tvPokemonName.setText("CARGANDO...");
        tvPokemonId.setText("#...");
        mostrarMensajeEvolucion("Cargando evolución...");

        PokeApiService api = ApiClient.getClient().create(PokeApiService.class);
        api.getPokemon(query).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mostrarPokemon(response.body());
                } else {
                    tvPokemonName.setText("NO ENCONTRADO");
                    Toast.makeText(MainActivity.this, "Pokémon no encontrado", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) {
                tvPokemonName.setText("ERROR DE RED");
                Toast.makeText(MainActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarPokemon(Pokemon pokemon) {
        idActual = pokemon.getId();
        currentPokemonId = String.valueOf(idActual);
        currentPokemonName = pokemon.getName().toUpperCase();

        tvPokemonName.setText(currentPokemonName);
        tvPokemonId.setText(String.format(Locale.US, "#%03d", idActual));

        // Tipos y color
        String principal = "normal";
        tvType1.setVisibility(View.GONE);
        tvType2.setVisibility(View.GONE);
        List<Pokemon.TypeSlot> tipos = pokemon.getTypes();
        if (tipos != null) {
            for (int i = 0; i < tipos.size() && i < 2; i++) {
                String t = tipos.get(i).getType().getName();
                TextView chip = (i == 0) ? tvType1 : tvType2;
                chip.setText(t.toUpperCase());
                chip.setVisibility(View.VISIBLE);
                if (i == 0) principal = t;
            }
        }
        aplicarColor(colorTipo(principal));

        // Imágenes y audio
        normalSpriteUrl = ARTWORK + idActual + ".png";
        shinySpriteUrl = ARTWORK + "shiny/" + idActual + ".png";
        isShiny = false;
        cargarImagenActual();
        audioCryUrl = CRIES + idActual + ".ogg";

        // Estadísticas
        if (pokemon.getStats() != null) {
            for (PokemonStat stat : pokemon.getStats()) {
                asignarStat(stat.getStat().getName(), stat.getBaseStat());
            }
        }

        // Descripción
        descripcionPokemon = String.format(Locale.US, "Altura: %.1f m\nPeso: %.1f kg",
                pokemon.getHeight() / 10.0, pokemon.getWeight() / 10.0);

        // Movimientos
        if (pokemon.getMoves() != null && !pokemon.getMoves().isEmpty()) {
            StringBuilder sb = new StringBuilder("Movimientos principales:\n");
            int count = 0;
            for (Pokemon.MoveEntry m : pokemon.getMoves()) {
                sb.append("• ").append(m.getMove().getName().replace("-", " ")).append("\n");
                if (++count >= 5) break;
            }
            movimientosPokemon = sb.toString();
        } else {
            movimientosPokemon = "Movimientos no disponibles.";
        }

        seleccionarTab(tabActual);
        verificarEstadoFavorito();
        obtenerDatosEspecie(idActual);
    }

    private void asignarStat(String nombre, int valor) {
        switch (nombre) {
            case "hp":
                pbHp.setProgress(valor);
                tvHpVal.setText(String.valueOf(valor));
                break;
            case "attack":
                pbAttack.setProgress(valor);
                tvAttackVal.setText(String.valueOf(valor));
                break;
            case "defense":
                pbDefense.setProgress(valor);
                tvDefenseVal.setText(String.valueOf(valor));
                break;
            case "speed":
                pbSpeed.setProgress(valor);
                tvSpeedVal.setText(String.valueOf(valor));
                break;
        }
    }

    // ---------------- EVOLUCIÓN ----------------

    private void obtenerDatosEspecie(int id) {
        PokeApiService api = ApiClient.getClient().create(PokeApiService.class);
        api.getPokemonSpecies(id).enqueue(new Callback<PokemonSpecies>() {
            @Override
            public void onResponse(Call<PokemonSpecies> call, Response<PokemonSpecies> response) {
                if (id != idActual) return; // respuesta de una búsqueda anterior
                if (response.isSuccessful() && response.body() != null
                        && response.body().getEvolutionChain() != null) {
                    obtenerCadenaEvolutiva(id, response.body().getEvolutionChain().getUrl());
                } else {
                    mostrarMensajeEvolucion("Información de evolución no disponible.");
                }
            }

            @Override
            public void onFailure(Call<PokemonSpecies> call, Throwable t) {
                if (id == idActual) mostrarMensajeEvolucion("Error al consultar evolución.");
            }
        });
    }

    private void obtenerCadenaEvolutiva(int id, String url) {
        PokeApiService api = ApiClient.getClient().create(PokeApiService.class);
        api.getEvolutionChain(url).enqueue(new Callback<EvolutionChainResponse>() {
            @Override
            public void onResponse(Call<EvolutionChainResponse> call, Response<EvolutionChainResponse> response) {
                if (id != idActual) return;
                if (response.isSuccessful() && response.body() != null && response.body().getChain() != null) {
                    List<List<EvolutionChainResponse.ChainLink>> etapas = new ArrayList<>();
                    agruparPorEtapa(response.body().getChain(), 0, etapas);
                    dibujarEvolucion(etapas);
                } else {
                    mostrarMensajeEvolucion("Información de evolución no disponible.");
                }
            }

            @Override
            public void onFailure(Call<EvolutionChainResponse> call, Throwable t) {
                if (id == idActual) mostrarMensajeEvolucion("Error al consultar evolución.");
            }
        });
    }

    // Agrupa la cadena por etapas: [bases] -> [evoluciones 1] -> [evoluciones 2]
    private void agruparPorEtapa(EvolutionChainResponse.ChainLink nodo, int nivel,
                                 List<List<EvolutionChainResponse.ChainLink>> etapas) {
        if (etapas.size() == nivel) etapas.add(new ArrayList<>());
        etapas.get(nivel).add(nodo);
        if (nodo.getEvolvesTo() != null) {
            for (EvolutionChainResponse.ChainLink hijo : nodo.getEvolvesTo()) {
                agruparPorEtapa(hijo, nivel + 1, etapas);
            }
        }
    }

    private void dibujarEvolucion(List<List<EvolutionChainResponse.ChainLink>> etapas) {
        llEvolution.removeAllViews();
        if (etapas.size() == 1) {
            mostrarMensajeEvolucion("Este Pokémon no tiene evoluciones.");
            return;
        }
        for (int i = 0; i < etapas.size(); i++) {
            if (i > 0) {
                TextView flecha = new TextView(this);
                flecha.setText("➜");
                flecha.setTextColor(0xFFFACC15);
                flecha.setTextSize(22);
                LinearLayout.LayoutParams lpF = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lpF.gravity = Gravity.CENTER_VERTICAL;
                lpF.setMargins(dp(6), 0, dp(6), 0);
                llEvolution.addView(flecha, lpF);
            }
            LinearLayout columna = new LinearLayout(this);
            columna.setOrientation(LinearLayout.VERTICAL);
            for (EvolutionChainResponse.ChainLink nodo : etapas.get(i)) {
                columna.addView(crearItemEvolucion(nodo));
            }
            LinearLayout.LayoutParams lpC = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpC.gravity = Gravity.CENTER_VERTICAL;
            llEvolution.addView(columna, lpC);
        }
    }

    private View crearItemEvolucion(EvolutionChainResponse.ChainLink nodo) {
        String nombre = nodo.getSpecies().getName();
        String[] partes = nodo.getSpecies().getUrl().split("/");
        String id = partes[partes.length - 1];
        boolean esActual = id.equals(currentPokemonId);

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setPadding(dp(10), dp(8), dp(10), dp(8));
        item.setBackgroundResource(esActual ? R.drawable.bg_evo_current : R.drawable.bg_evo_item);

        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(dp(72), dp(72)));
        Picasso.get().load(SPRITES + id + ".png")
                .placeholder(android.R.drawable.ic_menu_gallery).into(img);

        TextView tv = new TextView(this);
        tv.setText(nombre.toUpperCase());
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);

        item.addView(img);
        item.addView(tv);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        item.setLayoutParams(lp);

        item.setOnClickListener(v -> buscarPokemon(id));
        return item;
    }

    private void mostrarMensajeEvolucion(String mensaje) {
        llEvolution.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(mensaje);
        tv.setTextColor(0xFFCBD5E1);
        tv.setTextSize(13);
        tv.setPadding(dp(4), dp(8), dp(4), dp(8));
        llEvolution.addView(tv);
    }

    // ---------------- FAVORITOS ----------------

    private void toggleFavorito() {
        if (currentPokemonName.isEmpty()) return;

        Set<String> favorites = new HashSet<>(sharedPreferences.getStringSet(FAVORITES_KEY, new HashSet<>()));
        String item = "#" + currentPokemonId + " - " + currentPokemonName;

        if (favorites.contains(item)) {
            favorites.remove(item);
            Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
        } else {
            favorites.add(item);
            Toast.makeText(this, "¡Añadido a favoritos!", Toast.LENGTH_SHORT).show();
        }
        sharedPreferences.edit().putStringSet(FAVORITES_KEY, favorites).apply();

        verificarEstadoFavorito();
        btnFavorite.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120)
                .withEndAction(() -> btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(120));
    }

    private void verificarEstadoFavorito() {
        Set<String> favorites = sharedPreferences.getStringSet(FAVORITES_KEY, new HashSet<>());
        String item = "#" + currentPokemonId + " - " + currentPokemonName;
        btnFavorite.setImageResource(favorites.contains(item) ? R.drawable.ic_star_on : R.drawable.ic_star_off);
    }

    // Abre la pantalla de favoritos (con imágenes) y carga aquí el Pokémon elegido
    private final ActivityResultLauncher<Intent> elegirFavorito = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    int id = result.getData().getIntExtra(FavoritesActivity.EXTRA_POKEMON_ID, 0);
                    if (id > 0) buscarPokemon(String.valueOf(id));
                }
            });

    private void mostrarListaFavoritos() {
        elegirFavorito.launch(new Intent(this, FavoritesActivity.class)
                .putExtra(FavoritesActivity.EXTRA_PICK, true));
    }

    // ---------------- UI ----------------

    private void seleccionarTab(int indice) {
        tabActual = indice;
        TextView[] tabs = {tabDescription, tabMoves, tabEvolution};
        for (int i = 0; i < tabs.length; i++) {
            boolean activa = (i == indice);
            tabs[i].setBackgroundResource(activa ? R.drawable.bg_tab_on : R.drawable.bg_tab_off);
            tabs[i].setTextColor(activa ? 0xFF0F172A : 0xFF94A3B8);
            tabs[i].setTypeface(null, activa ? Typeface.BOLD : Typeface.NORMAL);
        }
        scrollEvolution.setVisibility(indice == 2 ? View.VISIBLE : View.GONE);
        tvTabContent.setVisibility(indice == 2 ? View.GONE : View.VISIBLE);
        tvTabContent.setText(indice == 0 ? descripcionPokemon : movimientosPokemon);
    }

    private void aplicarColor(int color) {
        GradientDrawable fondo = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{color, oscurecer(color)});
        fondo.setCornerRadius(dp(28));
        heroCard.setBackground(fondo);

        ColorStateList tinte = ColorStateList.valueOf(color);
        pbHp.setProgressTintList(tinte);
        pbAttack.setProgressTintList(tinte);
        pbDefense.setProgressTintList(tinte);
        pbSpeed.setProgressTintList(tinte);
    }

    private int oscurecer(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.6f;
        return Color.HSVToColor(hsv);
    }

    private int colorTipo(String tipo) {
        switch (tipo) {
            case "fire": return 0xFFF97316;
            case "water": return 0xFF3B82F6;
            case "grass": return 0xFF22C55E;
            case "electric": return 0xFFEAB308;
            case "psychic": return 0xFFEC4899;
            case "ice": return 0xFF06B6D4;
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

    private int dp(int valor) {
        return (int) (valor * getResources().getDisplayMetrics().density);
    }

    private void cambiarSprite(boolean shinyState) {
        isShiny = shinyState;
        cargarImagenActual();
        Toast.makeText(this, isShiny ? "Mostrando versión SHINY ✨" : "Mostrando versión Normal", Toast.LENGTH_SHORT).show();
    }

    private void cargarImagenActual() {
        String urlToLoad = isShiny ? shinySpriteUrl : normalSpriteUrl;
        if (urlToLoad != null && !urlToLoad.isEmpty()) {
            Picasso.get().load(urlToLoad).placeholder(android.R.drawable.ic_menu_gallery).into(imgPokemon);
        }
    }

    private void reproducirSonido() {
        if (audioCryUrl == null || audioCryUrl.isEmpty()) {
            Toast.makeText(this, "Audio no disponible para este Pokémon", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = MediaPlayer.create(this, Uri.parse(audioCryUrl));
            if (mediaPlayer != null) mediaPlayer.start();
        } catch (Exception e) {
            Toast.makeText(this, "Error al reproducir audio", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}