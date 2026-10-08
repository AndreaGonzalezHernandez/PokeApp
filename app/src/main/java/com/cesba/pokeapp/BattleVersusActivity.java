package com.cesba.pokeapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BattleVersusActivity extends AppCompatActivity {

    // Componentes visuales declarados en activity_battle_versus.xml
    private ProgressBar pbP1Hp, pbP2Hp;
    private TextView tvTurnIndicator, tvVersusLog, tvP1Name, tvP2Name;
    private ImageView imgP1Pokemon, imgP2Pokemon;
    private Button btnVersusAttack1, btnVersusAttack2, btnSelectP1, btnSelectP2;

    // Datos dinámicos Jugador 1
    private String p1Name = "Pikachu";
    private int p1MaxHp = 100;
    private int p1Hp = 100;
    private int p1Attack = 50;
    private int p1Defense = 50;

    // Datos dinámicos Jugador 2
    private String p2Name = "Charmander";
    private int p2MaxHp = 100;
    private int p2Hp = 100;
    private int p2Attack = 50;
    private int p2Defense = 50;

    private boolean isPlayer1Turn = true; // true = J1, false = J2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_versus);

        // Enlazar vistas con los IDs correspondientes de tu layout XML
        pbP1Hp = findViewById(R.id.pbP1Hp);
        pbP2Hp = findViewById(R.id.pbP2Hp);
        tvTurnIndicator = findViewById(R.id.tvTurnIndicator);
        tvVersusLog = findViewById(R.id.tvVersusLog);
        tvP1Name = findViewById(R.id.tvP1Name);
        tvP2Name = findViewById(R.id.tvP2Name);

        imgP1Pokemon = findViewById(R.id.imgP1Pokemon);
        imgP2Pokemon = findViewById(R.id.imgP2Pokemon);

        btnVersusAttack1 = findViewById(R.id.btnVersusAttack1);
        btnVersusAttack2 = findViewById(R.id.btnVersusAttack2);
        btnSelectP1 = findViewById(R.id.btnSelectP1);
        btnSelectP2 = findViewById(R.id.btnSelectP2);

        // Carga inicial por defecto con PokeAPI (#25 Pikachu y #4 Charmander)
        cargarPokemon(1, "25");
        cargarPokemon(2, "4");

        // Eventos para elegir Pokémon por cada jugador
        btnSelectP1.setOnClickListener(v -> mostrarDialogoSeleccion(1));
        btnSelectP2.setOnClickListener(v -> mostrarDialogoSeleccion(2));

        // Acciones de ataque
        btnVersusAttack1.setOnClickListener(v -> executeVersusTurn(25, "Ataque Fuerte"));
        btnVersusAttack2.setOnClickListener(v -> executeVersusTurn(15, "Ataque Rápido"));
    }

    // Cuadro de diálogo para ingresar el nombre o ID del Pokémon
    private void mostrarDialogoSeleccion(int playerNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Elegir Pokémon para Jugador " + playerNumber);

        final EditText input = new EditText(this);
        input.setHint("Nombre o ID (ej: mewtwo, 150)");
        builder.setView(input);

        builder.setPositiveButton("Buscar", (dialog, which) -> {
            String query = input.getText().toString().trim().toLowerCase();
            if (!query.isEmpty()) {
                cargarPokemon(playerNumber, query);
            } else {
                Toast.makeText(this, "Ingresa un nombre o número válido", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Consulta a la API de Pokémon mediante Retrofit
    private void cargarPokemon(int playerNumber, String query) {
        PokeApiService apiService = ApiClient.getClient().create(PokeApiService.class);
        Call<Pokemon> call = apiService.getPokemon(query);

        call.enqueue(new Callback<Pokemon>() {
            @Override
            public void onResponse(Call<Pokemon> call, Response<Pokemon> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Pokemon pokemon = response.body();
                    String name = pokemon.getName().toUpperCase();
                    String imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + pokemon.getId() + ".png";

                    // Extraer estadísticas base (HP, Ataque, Defensa)
                    int hp = 100, atk = 50, def = 50;
                    if (pokemon.getStats() != null) {
                        for (PokemonStat stat : pokemon.getStats()) {
                            String statName = stat.getStat().getName();
                            if (statName.equals("hp")) hp = stat.getBaseStat();
                            else if (statName.equals("attack")) atk = stat.getBaseStat();
                            else if (statName.equals("defense")) def = stat.getBaseStat();
                        }
                    }

                    if (playerNumber == 1) {
                        p1Name = name;
                        p1MaxHp = hp;
                        p1Hp = hp;
                        p1Attack = atk;
                        p1Defense = def;

                        tvP1Name.setText("J1: " + p1Name);
                        pbP1Hp.setMax(p1MaxHp);
                        pbP1Hp.setProgress(p1Hp);
                        Picasso.get().load(imageUrl).into(imgP1Pokemon);

                    } else {
                        p2Name = name;
                        p2MaxHp = hp;
                        p2Hp = hp;
                        p2Attack = atk;
                        p2Defense = def;

                        tvP2Name.setText("J2: " + p2Name);
                        pbP2Hp.setMax(p2MaxHp);
                        pbP2Hp.setProgress(p2Hp);
                        Picasso.get().load(imageUrl).into(imgP2Pokemon);
                    }

                    Toast.makeText(BattleVersusActivity.this, "¡J" + playerNumber + " eligió a " + name + "!", Toast.LENGTH_SHORT).show();
                    tvVersusLog.setText("Pokémon listos: " + p1Name + " VS " + p2Name);

                } else {
                    Toast.makeText(BattleVersusActivity.this, "Pokémon no encontrado", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Pokemon> call, Throwable t) {
                Toast.makeText(BattleVersusActivity.this, "Error de red al consultar la PokeAPI", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executeVersusTurn(int basePower, String moveName) {
        if (p1Hp <= 0 || p2Hp <= 0) return;

        if (isPlayer1Turn) {
            // Turno del Jugador 1
            int damage = calcularDano(basePower, p1Attack, p2Defense);
            p2Hp = Math.max(0, p2Hp - damage);
            pbP2Hp.setProgress(p2Hp);

            tvVersusLog.setText("¡" + p1Name + " (J1) usó " + moveName + " y causó " + damage + " de daño a " + p2Name + "!");

            if (p2Hp == 0) {
                Toast.makeText(this, "¡Jugador 1 (" + p1Name + ") ha ganado la batalla!", Toast.LENGTH_LONG).show();
                tvVersusLog.setText("¡Fin del combate! Victoria para " + p1Name + " (J1).");
                tvTurnIndicator.setText("¡Ganador: " + p1Name + "!");
                deshabilitarBotones();
                return;
            }

            isPlayer1Turn = false;
            tvTurnIndicator.setText("Turno: Jugador 2 (" + p2Name + ")");

        } else {
            // Turno del Jugador 2
            int damage = calcularDano(basePower, p2Attack, p1Defense);
            p1Hp = Math.max(0, p1Hp - damage);
            pbP1Hp.setProgress(p1Hp);

            tvVersusLog.setText("¡" + p2Name + " (J2) usó " + moveName + " y causó " + damage + " de daño a " + p1Name + "!");

            if (p1Hp == 0) {
                Toast.makeText(this, "¡Jugador 2 (" + p2Name + ") ha ganado la batalla!", Toast.LENGTH_LONG).show();
                tvVersusLog.setText("¡Fin del combate! Victoria para " + p2Name + " (J2).");
                tvTurnIndicator.setText("¡Ganador: " + p2Name + "!");
                deshabilitarBotones();
                return;
            }

            isPlayer1Turn = true;
            tvTurnIndicator.setText("Turno: Jugador 1 (" + p1Name + ")");
        }
    }

    // Cálculo de daño adaptado a las estadísticas del Pokémon activo
    private int calcularDano(int basePower, int atk, int def) {
        double ratio = (double) atk / (double) def;
        int damage = (int) (basePower * ratio);
        return Math.max(5, damage); // Daño mínimo garantizado
    }

    private void deshabilitarBotones() {
        btnVersusAttack1.setEnabled(false);
        btnVersusAttack2.setEnabled(false);
    }
}