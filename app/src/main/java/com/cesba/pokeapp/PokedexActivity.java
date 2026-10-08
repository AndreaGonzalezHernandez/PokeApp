package com.cesba.pokeapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;

public class PokedexActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokedex);

        // 1. Abrir Pokédex
        CardView cardPokedex = findViewById(R.id.cardPokedex);
        if (cardPokedex != null) {
            cardPokedex.setOnClickListener(v -> {
                startActivity(new Intent(PokedexActivity.this, MainActivity.class));
            });
        }

        // 2. Battle Simulator
        CardView cardBattle = findViewById(R.id.cardBattle);
        if (cardBattle != null) {
            cardBattle.setOnClickListener(v -> {
                startActivity(new Intent(PokedexActivity.this, BattleEmulatorActivity.class));
            });
        }

        // 3. Battle Versus
        CardView cardVersus = findViewById(R.id.cardVersus);
        if (cardVersus != null) {
            cardVersus.setOnClickListener(v -> {
                startActivity(new Intent(PokedexActivity.this, BattleVersusActivity.class));
            });
        }

        // 4. Torre Pokémon
        CardView cardTower = findViewById(R.id.cardTower);
        if (cardTower != null) {
            cardTower.setOnClickListener(v -> {
                startActivity(new Intent(PokedexActivity.this, TorrePokemonActivity.class));
            });
        }

        // 9. Favoritos
        CardView cardFavorites = findViewById(R.id.cardFavorites);
        if (cardFavorites != null) {
            cardFavorites.setOnClickListener(v -> {
                startActivity(new Intent(PokedexActivity.this, FavoritesActivity.class));
            });
        }

        // 12. Mi Perfil
        CardView cardProfile = findViewById(R.id.cardProfile);
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                startActivity(new Intent(PokedexActivity.this, PerfilActivity.class));
            });
        }

        // 13. Cerrar Sesión
        CardView cardLogout = findViewById(R.id.cardLogout);
        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                try {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(PokedexActivity.this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(PokedexActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(PokedexActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}