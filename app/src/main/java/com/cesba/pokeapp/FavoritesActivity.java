package com.cesba.pokeapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Lista de favoritos con la imagen oficial de cada Pokémon (PokeAPI)
public class FavoritesActivity extends AppCompatActivity {

    // Si se abre con PICK=true devuelve el id elegido en lugar de abrir la Pokédex
    public static final String EXTRA_PICK = "PICK";
    public static final String EXTRA_POKEMON_ID = "POKEMON_ID";

    private GridLayout gridFavorites;
    private TextView tvFavCount, tvFavEmpty;
    private PokeApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        api = ApiClient.getClient().create(PokeApiService.class);

        gridFavorites = findViewById(R.id.gridFavorites);
        tvFavCount = findViewById(R.id.tvFavCount);
        tvFavEmpty = findViewById(R.id.tvFavEmpty);
        findViewById(R.id.btnFavBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        dibujarFavoritos();
    }

    private void dibujarFavoritos() {
        List<Favoritos.Item> favoritos = Favoritos.leer(this);
        tvFavCount.setText(favoritos.size() + (favoritos.size() == 1 ? " Pokémon" : " Pokémon guardados"));
        tvFavEmpty.setVisibility(favoritos.isEmpty() ? View.VISIBLE : View.GONE);

        gridFavorites.removeAllViews();
        int ancho = (getResources().getDisplayMetrics().widthPixels - dp(16)) / 2 - dp(12);
        for (Favoritos.Item item : favoritos) {
            gridFavorites.addView(crearTarjeta(item, ancho));
        }
    }

    private View crearTarjeta(Favoritos.Item item, int ancho) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(8), dp(10), dp(8), dp(12));
        card.setElevation(dp(3));
        aplicarFondo(card, 0xFF334155);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = ancho;
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        card.setLayoutParams(lp);

        TextView tvNumero = new TextView(this);
        tvNumero.setText(String.format(Locale.US, "#%03d", item.id));
        tvNumero.setTextColor(0xCCFFFFFF);
        tvNumero.setTextSize(12);
        tvNumero.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lpNum = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvNumero.setGravity(Gravity.END);
        card.addView(tvNumero, lpNum);

        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Picasso.get()
                .load(BattlePokemon.SPRITES + "other/official-artwork/" + item.id + ".png")
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(img);
        card.addView(img, new LinearLayout.LayoutParams(ancho - dp(40), ancho - dp(40)));

        TextView tvNombre = new TextView(this);
        tvNombre.setText(item.nombre);
        tvNombre.setTextColor(Color.WHITE);
        tvNombre.setTextSize(15);
        tvNombre.setTypeface(null, Typeface.BOLD);
        tvNombre.setGravity(Gravity.CENTER);
        card.addView(tvNombre);

        LinearLayout llTipos = new LinearLayout(this);
        llTipos.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpTipos = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTipos.topMargin = dp(6);
        card.addView(llTipos, lpTipos);

        // Tipos y color de la tarjeta desde la PokeAPI
        api.getPokemon(String.valueOf(item.id)).enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getTypes() == null) return;
                List<Pokemon.TypeSlot> tipos = response.body().getTypes();
                for (Pokemon.TypeSlot t : tipos) llTipos.addView(chipTipo(t.getType().getName()));
                if (!tipos.isEmpty()) aplicarFondo(card, TypeChart.color(tipos.get(0).getType().getName()));
            }

            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) {
                // Sin red: la tarjeta se queda con el color neutro
            }
        });

        card.setOnClickListener(v -> abrir(item));
        card.setOnLongClickListener(v -> {
            confirmarQuitar(item);
            return true;
        });
        return card;
    }

    private void aplicarFondo(View v, int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.55f;
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{color, Color.HSVToColor(hsv)});
        bg.setCornerRadius(dp(20));
        v.setBackground(bg);
    }

    private TextView chipTipo(String tipo) {
        TextView chip = new TextView(this);
        chip.setText(TypeChart.nombre(tipo));
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(10);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setPadding(dp(8), dp(2), dp(8), dp(2));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x44000000);
        bg.setCornerRadius(dp(12));
        chip.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(2), 0, dp(2), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void abrir(Favoritos.Item item) {
        if (getIntent().getBooleanExtra(EXTRA_PICK, false)) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_POKEMON_ID, item.id));
        } else {
            startActivity(new Intent(this, MainActivity.class).putExtra(EXTRA_POKEMON_ID, item.id));
        }
        finish();
    }

    private void confirmarQuitar(Favoritos.Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Quitar de favoritos")
                .setMessage("¿Quitar a " + item.nombre + " de tus favoritos?")
                .setPositiveButton("Quitar", (d, w) -> {
                    Favoritos.quitar(this, item);
                    Toast.makeText(this, item.nombre + " eliminado de favoritos", Toast.LENGTH_SHORT).show();
                    dibujarFavoritos();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
