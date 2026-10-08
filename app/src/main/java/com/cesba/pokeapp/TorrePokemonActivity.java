package com.cesba.pokeapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TorrePokemonActivity extends AppCompatActivity {

    private TextView tvTowerStatus;
    private ProgressBar pbTowerProgress;
    private Button btnAdvanceTower;

    private int currentLevel = 1;
    private final int MAX_LEVEL = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torre_pokemon);

        tvTowerStatus = findViewById(R.id.tvTowerStatus);
        pbTowerProgress = findViewById(R.id.pbTowerProgress);
        btnAdvanceTower = findViewById(R.id.btnAdvanceTower);

        // Inicializar barra de progreso con el nivel actual
        pbTowerProgress.setMax(MAX_LEVEL);
        pbTowerProgress.setProgress(currentLevel);

        btnAdvanceTower.setOnClickListener(v -> {
            if (currentLevel < MAX_LEVEL) {
                int levelCompleted = currentLevel;
                currentLevel++;

                pbTowerProgress.setProgress(currentLevel);

                if (currentLevel == MAX_LEVEL) {
                    tvTowerStatus.setText("Nivel 6: ¡JEFE FINAL (Legendarios)!");
                    Toast.makeText(this, "¡Has llegado al Jefe Final de la Torre!", Toast.LENGTH_LONG).show();
                    btnAdvanceTower.setText("Derrotar al Jefe Final");
                } else {
                    tvTowerStatus.setText("Nivel Actual: " + currentLevel);
                    Toast.makeText(this, "¡Nivel " + levelCompleted + " superado con éxito!", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Completar la torre
                Toast.makeText(this, "¡Felicidades! Has completado la Torre y ganado la Medalla.", Toast.LENGTH_LONG).show();
                tvTowerStatus.setText("¡TORRE COMPLETADA - CAMPEÓN!");
                btnAdvanceTower.setEnabled(false);
                btnAdvanceTower.setText("Torre Completada");
            }
        });
    }
}