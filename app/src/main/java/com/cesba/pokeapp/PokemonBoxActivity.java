package com.cesba.pokeapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Selector de Pokémon para el Simulador de batalla con el estilo de "Equipo y Cajas"
public class PokemonBoxActivity extends AppCompatActivity {

    public static final String EXTRA_J1 = "J1_ID";
    public static final String EXTRA_J2 = "J2_ID";
    public static final String EXTRA_J1_NAME = "J1_NAME";
    public static final String EXTRA_J2_NAME = "J2_NAME";

    private static final int TOTAL_POKEMON = 1025;
    private static final int POR_CAJA = 30;

    private TextView tabBoxes, tabFavs, tvDetailName, tvDetailNumber, tvBoxName, tvBoxEmpty;
    private TextView tvSlotJ1, tvSlotJ2, btnBoxAssign;
    private LinearLayout llDetailTypes, slotJ1, slotJ2;
    private ImageView imgDetail, imgSlotJ1, imgSlotJ2;
    private StatRadarView radarStats;
    private GridLayout gridBox;
    private EditText etBoxSearch;

    private boolean modoFavoritos = false;
    private int caja = 0;
    private int paginaFavoritos = 0;
    private int idSeleccionado = 0;
    private int slotActivo = 1;
    private int j1Id, j2Id;
    private String j1Name, j2Name;

    private final List<Integer> idsPagina = new ArrayList<>();
    private final List<FrameLayout> celdas = new ArrayList<>();
    private final Map<Integer, Pokemon> cache = new HashMap<>();
    private PokeApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon_box);
        api = ApiClient.getClient().create(PokeApiService.class);

        tabBoxes = findViewById(R.id.tabBoxes);
        tabFavs = findViewById(R.id.tabFavs);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailNumber = findViewById(R.id.tvDetailNumber);
        tvBoxName = findViewById(R.id.tvBoxName);
        tvBoxEmpty = findViewById(R.id.tvBoxEmpty);
        tvSlotJ1 = findViewById(R.id.tvSlotJ1);
        tvSlotJ2 = findViewById(R.id.tvSlotJ2);
        btnBoxAssign = findViewById(R.id.btnBoxAssign);
        llDetailTypes = findViewById(R.id.llDetailTypes);
        slotJ1 = findViewById(R.id.slotJ1);
        slotJ2 = findViewById(R.id.slotJ2);
        imgDetail = findViewById(R.id.imgDetail);
        imgSlotJ1 = findViewById(R.id.imgSlotJ1);
        imgSlotJ2 = findViewById(R.id.imgSlotJ2);
        radarStats = findViewById(R.id.radarStats);
        gridBox = findViewById(R.id.gridBox);
        etBoxSearch = findViewById(R.id.etBoxSearch);

        Intent in = getIntent();
        j1Id = in.getIntExtra(EXTRA_J1, 25);
        j2Id = in.getIntExtra(EXTRA_J2, 6);
        j1Name = in.getStringExtra(EXTRA_J1_NAME);
        j2Name = in.getStringExtra(EXTRA_J2_NAME);

        findViewById(R.id.btnBoxClose).setOnClickListener(v -> finish());
        tabBoxes.setOnClickListener(v -> cambiarModo(false));
        tabFavs.setOnClickListener(v -> cambiarModo(true));
        findViewById(R.id.btnBoxPrev).setOnClickListener(v -> moverPagina(-1));
        findViewById(R.id.btnBoxNext).setOnClickListener(v -> moverPagina(1));
        slotJ1.setOnClickListener(v -> activarSlot(1, true));
        slotJ2.setOnClickListener(v -> activarSlot(2, true));
        btnBoxAssign.setOnClickListener(v -> asignar());
        findViewById(R.id.btnBoxConfirm).setOnClickListener(v -> confirmar());
        findViewById(R.id.btnBoxSearch).setOnClickListener(v -> buscar());
        etBoxSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                buscar();
                return true;
            }
            return false;
        });

        // Abre la caja donde está el Pokémon del J1
        caja = (Math.max(1, Math.min(TOTAL_POKEMON, j1Id)) - 1) / POR_CAJA;
        actualizarSlots();
        activarSlot(1, false);
        dibujarPagina();
        seleccionar(j1Id);
    }

    // ---------------- CAJAS / FAVORITOS ----------------

    private void cambiarModo(boolean favoritos) {
        modoFavoritos = favoritos;
        paginaFavoritos = 0;
        tabBoxes.setBackgroundResource(favoritos ? 0 : R.drawable.bg_box_tab_on);
        tabFavs.setBackgroundResource(favoritos ? R.drawable.bg_box_tab_on : 0);
        tabBoxes.setTextColor(favoritos ? 0xFF8D96C2 : 0xFFFFFFFF);
        tabFavs.setTextColor(favoritos ? 0xFFFFFFFF : 0xFF8D96C2);
        tabBoxes.setTypeface(null, favoritos ? Typeface.NORMAL : Typeface.BOLD);
        tabFavs.setTypeface(null, favoritos ? Typeface.BOLD : Typeface.NORMAL);
        dibujarPagina();
    }

    private int totalPaginas() {
        if (!modoFavoritos) return (TOTAL_POKEMON + POR_CAJA - 1) / POR_CAJA;
        int n = Favoritos.leer(this).size();
        return Math.max(1, (n + POR_CAJA - 1) / POR_CAJA);
    }

    private void moverPagina(int delta) {
        int total = totalPaginas();
        if (modoFavoritos) paginaFavoritos = (paginaFavoritos + delta + total) % total;
        else caja = (caja + delta + total) % total;
        dibujarPagina();
    }

    private void dibujarPagina() {
        idsPagina.clear();
        if (modoFavoritos) {
            List<Favoritos.Item> favs = Favoritos.leer(this);
            for (int i = paginaFavoritos * POR_CAJA; i < favs.size() && idsPagina.size() < POR_CAJA; i++) {
                idsPagina.add(favs.get(i).id);
            }
            tvBoxName.setText(String.format(Locale.US, "⭐ Favoritos (%d)  %d/%d",
                    favs.size(), paginaFavoritos + 1, totalPaginas()));
            tvBoxEmpty.setVisibility(favs.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            int inicio = caja * POR_CAJA + 1;
            for (int id = inicio; id < inicio + POR_CAJA && id <= TOTAL_POKEMON; id++) idsPagina.add(id);
            tvBoxName.setText(String.format(Locale.US, "Caja %d   (#%d - #%d)",
                    caja + 1, inicio, Math.min(TOTAL_POKEMON, inicio + POR_CAJA - 1)));
            tvBoxEmpty.setVisibility(View.GONE);
        }

        gridBox.removeAllViews();
        celdas.clear();
        int anchoGrid = getResources().getDisplayMetrics().widthPixels - dp(20);
        int lado = anchoGrid / 6 - dp(4);

        for (int id : idsPagina) {
            FrameLayout celda = new FrameLayout(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = lado;
            lp.height = lado;
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            celda.setLayoutParams(lp);

            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Picasso.get().load(BattlePokemon.SPRITES + id + ".png").transform(new PixelSprite(2)).into(img);
            celda.addView(img, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            // Cursor verde (como el triángulo del juego)
            TextView cursor = new TextView(this);
            cursor.setText("▼");
            cursor.setTextColor(0xFF34D399);
            cursor.setTextSize(14);
            cursor.setTag("cursor");
            FrameLayout.LayoutParams lpC = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            celda.addView(cursor, lpC);

            // Insignia J1 / J2
            TextView badge = new TextView(this);
            badge.setTag("badge");
            badge.setTextColor(0xFFFFFFFF);
            badge.setTextSize(9);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackgroundResource(R.drawable.bg_box_badge);
            FrameLayout.LayoutParams lpB = new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.BOTTOM | Gravity.END);
            lpB.setMargins(0, 0, dp(2), dp(2));
            celda.addView(badge, lpB);

            celda.setOnClickListener(v -> {
                if (id == idSeleccionado) asignar(); // segundo toque = elegir
                else seleccionar(id);
            });
            celda.setTag(id);
            celdas.add(celda);
            gridBox.addView(celda);
        }
        actualizarCeldas();
    }

    private void actualizarCeldas() {
        for (FrameLayout celda : celdas) {
            int id = (int) celda.getTag();
            boolean sel = id == idSeleccionado;
            celda.setBackgroundResource(sel ? R.drawable.bg_box_cell_selected : R.drawable.bg_box_cell);
            celda.findViewWithTag("cursor").setVisibility(sel ? View.VISIBLE : View.INVISIBLE);
            TextView badge = celda.findViewWithTag("badge");
            if (id == j1Id && id == j2Id) badge.setText("TR");
            else if (id == j1Id) badge.setText("T");
            else if (id == j2Id) badge.setText("R");
            badge.setVisibility(id == j1Id || id == j2Id ? View.VISIBLE : View.GONE);
        }
    }

    // ---------------- DETALLE ----------------

    private void seleccionar(int id) {
        idSeleccionado = id;
        actualizarCeldas();

        Pokemon enCache = cache.get(id);
        if (enCache != null) {
            mostrarDetalle(enCache);
            return;
        }
        tvDetailName.setText("Cargando...");
        tvDetailNumber.setText(String.format(Locale.US, "No. %03d", id));
        llDetailTypes.removeAllViews();
        radarStats.limpiar();
        Picasso.get().load(BattlePokemon.SPRITES + "other/official-artwork/" + id + ".png").into(imgDetail);

        api.getPokemon(String.valueOf(id)).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cache.put(id, response.body());
                    if (id == idSeleccionado) mostrarDetalle(response.body());
                }
            }

            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) {
                if (id == idSeleccionado) tvDetailName.setText("Error de red");
            }
        });
    }

    private void mostrarDetalle(Pokemon p) {
        tvDetailName.setText(bonito(p.getName()));
        tvDetailNumber.setText(String.format(Locale.US, "No. %03d   %s", p.getId(), bonito(p.getName())));
        Picasso.get().load(BattlePokemon.SPRITES + "other/official-artwork/" + p.getId() + ".png").into(imgDetail);

        llDetailTypes.removeAllViews();
        if (p.getTypes() != null) {
            for (Pokemon.TypeSlot t : p.getTypes()) llDetailTypes.addView(chipTipo(t.getType().getName()));
        }

        int hp = 0, atk = 0, def = 0, spa = 0, spd = 0, vel = 0;
        if (p.getStats() != null) {
            for (PokemonStat s : p.getStats()) {
                switch (s.getStat().getName()) {
                    case "hp": hp = s.getBaseStat(); break;
                    case "attack": atk = s.getBaseStat(); break;
                    case "defense": def = s.getBaseStat(); break;
                    case "special-attack": spa = s.getBaseStat(); break;
                    case "special-defense": spd = s.getBaseStat(); break;
                    case "speed": vel = s.getBaseStat(); break;
                }
            }
        }
        radarStats.setStats(hp, atk, def, spa, spd, vel);
    }

    private TextView chipTipo(String tipo) {
        TextView chip = new TextView(this);
        chip.setText(TypeChart.nombre(tipo));
        chip.setTextColor(0xFFFFFFFF);
        chip.setTextSize(11);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setPadding(dp(12), dp(3), dp(12), dp(3));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(TypeChart.color(tipo));
        bg.setCornerRadius(dp(20));
        chip.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(6));
        chip.setLayoutParams(lp);
        return chip;
    }

    // ---------------- EQUIPO ----------------

    private void activarSlot(int slot, boolean mostrarSuPokemon) {
        slotActivo = slot;
        slotJ1.setBackgroundResource(slot == 1 ? R.drawable.bg_box_slot_active : R.drawable.bg_box_slot);
        slotJ2.setBackgroundResource(slot == 2 ? R.drawable.bg_box_slot_active : R.drawable.bg_box_slot);
        btnBoxAssign.setText(slot == 1 ? "Ⓐ Elegir para ti" : "Ⓐ Elegir para el rival");
        if (mostrarSuPokemon) irA(slot == 1 ? j1Id : j2Id);
    }

    private void asignar() {
        if (idSeleccionado <= 0) {
            Toast.makeText(this, "Toca un Pokémon de la caja", Toast.LENGTH_SHORT).show();
            return;
        }
        Pokemon p = cache.get(idSeleccionado);
        String nombre = p != null ? p.getName().toUpperCase() : "#" + idSeleccionado;
        if (slotActivo == 1) {
            j1Id = idSeleccionado;
            j1Name = nombre;
        } else {
            j2Id = idSeleccionado;
            j2Name = nombre;
        }
        Toast.makeText(this, (slotActivo == 1 ? "Elegiste a " : "El rival será ") + nombre, Toast.LENGTH_SHORT).show();
        actualizarSlots();
        actualizarCeldas();
        activarSlot(slotActivo == 1 ? 2 : 1, false);
    }

    private void actualizarSlots() {
        tvSlotJ1.setText(j1Name != null ? bonito(j1Name) : "#" + j1Id);
        tvSlotJ2.setText(j2Name != null ? bonito(j2Name) : "#" + j2Id);
        Picasso.get().load(BattlePokemon.SPRITES + j1Id + ".png").transform(new PixelSprite(2)).into(imgSlotJ1);
        Picasso.get().load(BattlePokemon.SPRITES + j2Id + ".png").transform(new PixelSprite(2)).into(imgSlotJ2);
    }

    private void confirmar() {
        Intent data = new Intent();
        data.putExtra(EXTRA_J1, j1Id);
        data.putExtra(EXTRA_J2, j2Id);
        setResult(RESULT_OK, data);
        finish();
    }

    // ---------------- BÚSQUEDA ----------------

    private void buscar() {
        String query = etBoxSearch.getText().toString().trim().toLowerCase(Locale.US);
        if (query.isEmpty()) {
            Toast.makeText(this, "Escribe un nombre o número", Toast.LENGTH_SHORT).show();
            return;
        }
        api.getPokemon(query).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cache.put(response.body().getId(), response.body());
                    irA(response.body().getId());
                } else {
                    Toast.makeText(PokemonBoxActivity.this, "Ese Pokémon no existe", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) {
                Toast.makeText(PokemonBoxActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void irA(int id) {
        if (id >= 1 && id <= TOTAL_POKEMON) {
            if (modoFavoritos) cambiarModo(false);
            int nuevaCaja = (id - 1) / POR_CAJA;
            if (nuevaCaja != caja || celdas.isEmpty()) {
                caja = nuevaCaja;
                dibujarPagina();
            }
        }
        seleccionar(id);
    }

    // ---------------- UTILIDADES ----------------

    private String bonito(String nombre) {
        if (nombre == null || nombre.isEmpty()) return "";
        String n = nombre.toLowerCase(Locale.US).replace("-", " ");
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
