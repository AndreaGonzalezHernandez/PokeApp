package com.cesba.pokeapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

// Simulador de batalla: tu Pokémon (abajo, de espaldas) contra el rival (arriba, de frente).
// La batalla es automática: cada Pokémon usa sus 4 movimientos de la PokeAPI.
public class BattleEmulatorActivity extends AppCompatActivity {

    private static final int MAX_TURNOS = 40;
    private static final long PAUSA_MENSAJE = 550;
    private static final long PAUSA_TRAS_GOLPE = 1100;

    // Vistas
    private FrameLayout arena;
    private ImageView ivPlayer, ivEnemy;
    private View hpFillPlayer, hpFillEnemy;
    private TextView tvPlayerName, tvEnemyName, tvPlayerHp, tvEnemyHp, tvPlayerTypes, tvEnemyTypes;
    private TextView tvBattleMessage, tvBattleLog, tvMovesP1, tvMovesP2, btnStartBattle, btnChoosePokemon;
    private GridLayout gridMovesP1, gridMovesP2;
    private ScrollView scrollLog;
    private FrameLayout overlayResult;
    private ImageView ivWinnerImage;
    private TextView tvResultTitle, tvWinnerName, tvWinnerStats;

    // Estado
    private BattlePokemon p1, p2;
    private int hp1, hp2;
    private int turno;
    private boolean enCurso = false;
    private int cargando = 0;
    private final int[] usos1 = new int[4], usos2 = new int[4];
    private final List<Integer> bolsa1 = new ArrayList<>(), bolsa2 = new ArrayList<>();
    private final StringBuilder registro = new StringBuilder();

    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ObjectAnimator idlePlayer, idleEnemy;

    private final ActivityResultLauncher<Intent> elegirPokemon = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                int id1 = result.getData().getIntExtra(PokemonBoxActivity.EXTRA_J1, 0);
                int id2 = result.getData().getIntExtra(PokemonBoxActivity.EXTRA_J2, 0);
                if (id1 > 0 && (p1 == null || p1.id != id1)) cargarJugador(1, String.valueOf(id1));
                if (id2 > 0 && (p2 == null || p2.id != id2)) cargarJugador(2, String.valueOf(id2));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_emulator);

        arena = findViewById(R.id.arena);
        ivPlayer = findViewById(R.id.ivPlayer);
        ivEnemy = findViewById(R.id.ivEnemy);
        hpFillPlayer = findViewById(R.id.hpFillPlayer);
        hpFillEnemy = findViewById(R.id.hpFillEnemy);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvEnemyName = findViewById(R.id.tvEnemyName);
        tvPlayerHp = findViewById(R.id.tvPlayerHp);
        tvEnemyHp = findViewById(R.id.tvEnemyHp);
        tvPlayerTypes = findViewById(R.id.tvPlayerTypes);
        tvEnemyTypes = findViewById(R.id.tvEnemyTypes);
        tvBattleMessage = findViewById(R.id.tvBattleMessage);
        tvBattleLog = findViewById(R.id.tvBattleLog);
        tvMovesP1 = findViewById(R.id.tvMovesP1);
        tvMovesP2 = findViewById(R.id.tvMovesP2);
        gridMovesP1 = findViewById(R.id.gridMovesP1);
        gridMovesP2 = findViewById(R.id.gridMovesP2);
        scrollLog = findViewById(R.id.scrollLog);
        btnStartBattle = findViewById(R.id.btnStartBattle);
        btnChoosePokemon = findViewById(R.id.btnChoosePokemon);

        hpFillPlayer.setPivotX(0f);
        hpFillEnemy.setPivotX(0f);

        findViewById(R.id.btnBattleBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnBattleHistory).setOnClickListener(v -> mostrarHistorial());
        btnStartBattle.setOnClickListener(v -> iniciarBatalla());
        btnChoosePokemon.setOnClickListener(v -> abrirCajas());

        // Pantalla de resultado
        overlayResult = findViewById(R.id.overlayResult);
        ivWinnerImage = findViewById(R.id.ivWinnerImage);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvWinnerName = findViewById(R.id.tvWinnerName);
        tvWinnerStats = findViewById(R.id.tvWinnerStats);
        findViewById(R.id.btnRevancha).setOnClickListener(v -> {
            overlayResult.setVisibility(View.GONE);
            iniciarBatalla();
        });
        findViewById(R.id.btnElegirOtro).setOnClickListener(v -> {
            overlayResult.setVisibility(View.GONE);
            abrirCajas();
        });
        findViewById(R.id.btnVerRegistro).setOnClickListener(v -> overlayResult.setVisibility(View.GONE));
        findViewById(R.id.btnRegresarMenu).setOnClickListener(v -> finish());

        idlePlayer = iniciarRespiracion(ivPlayer);
        idleEnemy = iniciarRespiracion(ivEnemy);

        // Recupera los Pokémon guardados (o Pikachu vs Charizard la primera vez)
        BattlePokemon guardado1 = BattleStorage.leerJugador(this, 1);
        BattlePokemon guardado2 = BattleStorage.leerJugador(this, 2);
        if (guardado1 != null) aplicarJugador(1, guardado1);
        else cargarJugador(1, "25");
        if (guardado2 != null) aplicarJugador(2, guardado2);
        else cargarJugador(2, "6");

        List<BattleStorage.BattleRecord> historial = BattleStorage.leerHistorial(this);
        tvBattleLog.setText(historial.isEmpty()
                ? "Aquí aparecerá cada turno de la batalla."
                : "Última batalla: " + historial.get(0).ganador + " venció a " + historial.get(0).perdedor
                + " en " + historial.get(0).turnos + " turnos.\nToca 📜 Historial para ver todas.");
        actualizarBotones();
    }

    // ---------------- CARGA / SELECCIÓN ----------------

    private void abrirCajas() {
        Intent i = new Intent(this, PokemonBoxActivity.class);
        if (p1 != null) {
            i.putExtra(PokemonBoxActivity.EXTRA_J1, p1.id);
            i.putExtra(PokemonBoxActivity.EXTRA_J1_NAME, p1.name);
        }
        if (p2 != null) {
            i.putExtra(PokemonBoxActivity.EXTRA_J2, p2.id);
            i.putExtra(PokemonBoxActivity.EXTRA_J2_NAME, p2.name);
        }
        elegirPokemon.launch(i);
    }

    private void cargarJugador(int jugador, String query) {
        cargando++;
        actualizarBotones();
        mensaje("Cargando Pokémon y sus movimientos...");
        BattlePokemon.cargar(query, new BattlePokemon.Listener() {
            @Override
            public void onListo(BattlePokemon pokemon) {
                cargando--;
                if (isFinishing()) return;
                BattleStorage.guardarJugador(BattleEmulatorActivity.this, jugador, pokemon);
                aplicarJugador(jugador, pokemon);
                actualizarBotones();
            }

            @Override
            public void onError(String mensaje) {
                cargando--;
                if (isFinishing()) return;
                Toast.makeText(BattleEmulatorActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                mensaje(mensaje);
                actualizarBotones();
            }
        });
    }

    private void aplicarJugador(int jugador, BattlePokemon p) {
        boolean esJ1 = jugador == 1;
        if (esJ1) {
            p1 = p;
            hp1 = p.maxHp();
        } else {
            p2 = p;
            hp2 = p.maxHp();
        }

        ImageView iv = esJ1 ? ivPlayer : ivEnemy;
        iv.setAlpha(1f);
        iv.setTranslationX(0f);
        iv.setTranslationY(0f);
        cargarSprite(iv, p, esJ1);

        (esJ1 ? tvPlayerName : tvEnemyName).setText(p.name);
        (esJ1 ? tvPlayerTypes : tvEnemyTypes).setText(textoTipos(p));
        (esJ1 ? tvMovesP1 : tvMovesP2).setText((esJ1 ? "TÚ · " : "RIVAL · ") + p.name);
        pintarHp(esJ1, p.maxHp(), p.maxHp(), false);

        int[] usos = esJ1 ? usos1 : usos2;
        for (int i = 0; i < usos.length; i++) usos[i] = 0;
        dibujarMovimientos(esJ1, -1);

        if (p1 != null && p2 != null && cargando == 0) {
            mensaje("¡" + p1.name + " VS " + p2.name + "!\nPulsa INICIAR BATALLA.");
        }
    }

    // J1 de espaldas; si no existe el sprite de espalda se usa el frontal volteado
    private void cargarSprite(ImageView iv, BattlePokemon p, boolean espalda) {
        iv.setScaleX(1f);
        Picasso.get().load(espalda ? p.backUrl() : p.frontUrl()).transform(new PixelSprite(4))
                .into(iv, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onError(Exception e) {
                        if (espalda) {
                            iv.setScaleX(-1f);
                            Picasso.get().load(p.frontUrl()).transform(new PixelSprite(4))
                                    .into(iv, new com.squareup.picasso.Callback() {
                                        @Override
                                        public void onSuccess() {}

                                        @Override
                                        public void onError(Exception e2) {
                                            Picasso.get().load(p.artworkUrl()).into(iv);
                                        }
                                    });
                        } else {
                            Picasso.get().load(p.artworkUrl()).into(iv);
                        }
                    }
                });
    }

    private String textoTipos(BattlePokemon p) {
        StringBuilder sb = new StringBuilder();
        for (String t : p.types) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(TypeChart.nombre(t));
        }
        return sb.toString();
    }

    private void actualizarBotones() {
        boolean listo = p1 != null && p2 != null && cargando == 0;
        btnStartBattle.setEnabled(listo && !enCurso);
        btnChoosePokemon.setEnabled(!enCurso && cargando == 0);
        btnChoosePokemon.setAlpha(btnChoosePokemon.isEnabled() ? 1f : 0.5f);
    }

    // ---------------- MOVIMIENTOS (solo visual, no son botones) ----------------

    private void dibujarMovimientos(boolean esJ1, int resaltado) {
        BattlePokemon p = esJ1 ? p1 : p2;
        GridLayout grid = esJ1 ? gridMovesP1 : gridMovesP2;
        int[] usos = esJ1 ? usos1 : usos2;
        grid.removeAllViews();
        if (p == null) return;

        for (int i = 0; i < p.moves.size(); i++) {
            BattlePokemon.BattleMove m = p.moves.get(i);
            TextView chip = new TextView(this);
            String marca = usos[i] > 0 ? "✓ " : "";
            chip.setText(marca + m.displayName + "\n" + TypeChart.nombre(m.type) + " · " + m.power);
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(10);
            chip.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            chip.setMaxLines(2);
            chip.setPadding(dp(6), dp(3), dp(6), dp(3));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(TypeChart.color(m.type));
            bg.setCornerRadius(dp(8));
            if (i == resaltado) bg.setStroke(dp(3), Color.WHITE);
            else bg.setStroke(dp(1), 0x55000000);
            chip.setBackground(bg);
            chip.setAlpha(resaltado == -1 || i == resaltado ? 1f : 0.65f);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 2, 1f), GridLayout.spec(i % 2, 1f));
            lp.width = 0;
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            grid.addView(chip, lp);

            if (i == resaltado) {
                chip.setScaleX(0.85f);
                chip.setScaleY(0.85f);
                chip.animate().scaleX(1f).scaleY(1f).setInterpolator(new OvershootInterpolator(3f))
                        .setDuration(300).start();
            }
        }
    }

    // Cada Pokémon usa sus 4 movimientos en orden aleatorio antes de repetir alguno
    private int siguienteMovimiento(boolean esJ1) {
        List<Integer> bolsa = esJ1 ? bolsa1 : bolsa2;
        if (bolsa.isEmpty()) {
            int n = (esJ1 ? p1 : p2).moves.size();
            for (int i = 0; i < n; i++) bolsa.add(i);
            Collections.shuffle(bolsa, random);
        }
        return bolsa.remove(0);
    }

    // ---------------- FLUJO DE LA BATALLA ----------------

    private void iniciarBatalla() {
        if (p1 == null || p2 == null || cargando > 0 || enCurso) return;

        enCurso = true;
        overlayResult.setVisibility(View.GONE);
        turno = 0;
        hp1 = p1.maxHp();
        hp2 = p2.maxHp();
        bolsa1.clear();
        bolsa2.clear();
        for (int i = 0; i < 4; i++) {
            usos1[i] = 0;
            usos2[i] = 0;
        }
        registro.setLength(0);
        tvBattleLog.setText("");

        for (ImageView iv : new ImageView[]{ivPlayer, ivEnemy}) {
            iv.animate().cancel();
            iv.setAlpha(1f);
            iv.setTranslationX(0f);
            iv.setTranslationY(0f);
        }
        pintarHp(true, hp1, p1.maxHp(), false);
        pintarHp(false, hp2, p2.maxHp(), false);
        dibujarMovimientos(true, -1);
        dibujarMovimientos(false, -1);

        btnStartBattle.setText("⚔ EN COMBATE...");
        actualizarBotones();

        log("⚔ ¡COMIENZA LA BATALLA!");
        log("TÚ:    " + p1.name + " (" + p1.maxHp() + " HP, Vel " + p1.spd() + ")");
        log("RIVAL: " + p2.name + " (" + p2.maxHp() + " HP, Vel " + p2.spd() + ")");
        mensaje("¡Un " + p2.name + " rival quiere pelear!\n¡Adelante, " + p1.name + "!");

        handler.postDelayed(this::siguienteTurno, 1300);
    }

    private void siguienteTurno() {
        if (!enCurso || isFinishing()) return;
        turno++;

        if (turno > MAX_TURNOS) {
            // Evita combates infinitos (ej. dos Pokémon inmunes entre sí)
            double r1 = (double) hp1 / p1.maxHp();
            double r2 = (double) hp2 / p2.maxHp();
            log("\n⏱ Límite de " + MAX_TURNOS + " turnos: gana quien conserve más vida.");
            terminar(r1 >= r2);
            return;
        }

        boolean j1Primero = p1.spd() > p2.spd() || (p1.spd() == p2.spd() && random.nextBoolean());
        log("\n── TURNO " + turno + " ──");

        ejecutarAtaque(j1Primero, () ->
                ejecutarAtaque(!j1Primero, () ->
                        handler.postDelayed(this::siguienteTurno, 500)));
    }

    private void ejecutarAtaque(boolean atacaJ1, Runnable siguiente) {
        if (!enCurso || isFinishing()) return;

        BattlePokemon atacante = atacaJ1 ? p1 : p2;
        BattlePokemon defensor = atacaJ1 ? p2 : p1;
        ImageView ivAtacante = atacaJ1 ? ivPlayer : ivEnemy;
        ImageView ivDefensor = atacaJ1 ? ivEnemy : ivPlayer;

        int indice = siguienteMovimiento(atacaJ1);
        BattlePokemon.BattleMove mov = atacante.moves.get(indice);
        (atacaJ1 ? usos1 : usos2)[indice]++;
        dibujarMovimientos(atacaJ1, indice);
        mensaje("¡" + atacante.name + " usó " + mov.displayName + "!");

        handler.postDelayed(() -> embestir(ivAtacante, atacaJ1, () -> {
            if (!enCurso) return;
            String linea = (atacaJ1 ? "[TÚ] " : "[RIVAL] ") + atacante.name + " usó " + mov.displayName
                    + " [" + TypeChart.nombre(mov.type) + ", Pot " + mov.power + "]";

            boolean acierta = mov.accuracy <= 0 || random.nextInt(100) < mov.accuracy;
            double efectividad = TypeChart.efectividad(mov.type, defensor.types);

            if (!acierta) {
                log(linea + "\n   ✗ ¡Falló!");
                numeroFlotante(ivDefensor, "¡FALLÓ!", 0xFFE5E7EB, 22);
                mensaje("¡El ataque de " + atacante.name + " falló!");
            } else if (efectividad == 0) {
                log(linea + "\n   ✗ No afecta a " + defensor.name);
                numeroFlotante(ivDefensor, "INMUNE", 0xFF93C5FD, 22);
                mensaje("No afecta a " + defensor.name + "...");
            } else {
                boolean critico = random.nextInt(24) == 0;
                int danio = calcularDanio(atacante, defensor, mov, efectividad, critico);
                int hpAntes = atacaJ1 ? hp2 : hp1;
                int hpDespues = Math.max(0, hpAntes - danio);
                if (atacaJ1) hp2 = hpDespues;
                else hp1 = hpDespues;

                impacto(ivDefensor, TypeChart.color(mov.type));
                numeroFlotante(ivDefensor, "-" + danio, critico ? 0xFFFACC15 : 0xFFFF3B3B, critico ? 38 : 32);
                pintarHp(!atacaJ1, hpDespues, defensor.maxHp(), true);

                StringBuilder sb = new StringBuilder(linea);
                sb.append(String.format(Locale.US, "\n   → %s -%d HP (%d/%d)",
                        defensor.name, danio, hpDespues, defensor.maxHp()));
                String extra = "";
                if (critico) {
                    sb.append("\n   ★ ¡Golpe crítico!");
                    extra += "¡Golpe crítico! ";
                }
                if (efectividad > 1) {
                    sb.append("\n   ¡Es súper eficaz!");
                    extra += "¡Es súper eficaz!";
                } else if (efectividad < 1) {
                    sb.append("\n   No es muy eficaz...");
                    extra += "No es muy eficaz...";
                }
                log(sb.toString());
                mensaje(extra.isEmpty()
                        ? defensor.name + " recibió " + danio + " de daño."
                        : extra.trim() + "\n" + defensor.name + " recibió " + danio + " de daño.");
            }

            handler.postDelayed(() -> {
                if (!enCurso) return;
                if ((atacaJ1 ? hp2 : hp1) == 0) terminar(atacaJ1);
                else siguiente.run();
            }, PAUSA_TRAS_GOLPE);
        }), PAUSA_MENSAJE);
    }

    // Fórmula basada en la de los juegos (nivel 50 para el daño, stats de nivel 100)
    private int calcularDanio(BattlePokemon a, BattlePokemon d, BattlePokemon.BattleMove m,
                              double efectividad, boolean critico) {
        double ataque = m.physical ? a.atk() : a.spAtk();
        double defensa = m.physical ? d.def() : d.spDef();
        double base = (22.0 * m.power * ataque / defensa) / 50.0 + 2;
        double stab = a.types.contains(m.type) ? 1.5 : 1.0;
        double azar = 0.85 + random.nextDouble() * 0.15;
        // x0.5 para que el combate dure lo suficiente y se vean los 4 movimientos
        double total = base * stab * efectividad * (critico ? 1.5 : 1.0) * azar * 0.5;
        return Math.max(1, (int) Math.round(total));
    }

    private void terminar(boolean ganaJ1) {
        enCurso = false;
        handler.removeCallbacksAndMessages(null);

        BattlePokemon ganador = ganaJ1 ? p1 : p2;
        BattlePokemon perdedor = ganaJ1 ? p2 : p1;
        ImageView ivPerdedor = ganaJ1 ? ivEnemy : ivPlayer;
        int hpGanador = ganaJ1 ? hp1 : hp2;

        // Animación de debilitado
        ivPerdedor.animate().translationY(dp(70)).alpha(0f).setDuration(800).start();
        dibujarMovimientos(true, -1);
        dibujarMovimientos(false, -1);

        mensaje("¡" + perdedor.name + " se debilitó!\n🏆 ¡" + (ganaJ1 ? "Tu " : "El rival ") + ganador.name + " gana!");
        log("\n💀 " + perdedor.name + " se debilitó.");
        log("🏆 GANADOR: " + (ganaJ1 ? "Tu " : "El rival ") + ganador.name
                + " con " + hpGanador + "/" + ganador.maxHp() + " HP en " + turno + " turnos.");
        log("Movimientos usados (tú): " + resumenUsos(p1, usos1));
        log("Movimientos usados (rival): " + resumenUsos(p2, usos2));

        // Guardar la batalla en el historial
        BattleStorage.BattleRecord r = new BattleStorage.BattleRecord();
        r.fecha = System.currentTimeMillis();
        r.ganador = ganador.name;
        r.ganadorId = ganador.id;
        r.perdedor = perdedor.name;
        r.perdedorId = perdedor.id;
        r.turnos = turno;
        r.hpRestante = hpGanador;
        r.hpMaximo = ganador.maxHp();
        r.registro = registro.toString();
        BattleStorage.guardarBatalla(this, r);
        Toast.makeText(this, "Batalla guardada en el historial", Toast.LENGTH_SHORT).show();

        btnStartBattle.setText("↻ REVANCHA");
        actualizarBotones();

        // Muestra la tarjeta de resultado después de la animación de debilitado
        handler.postDelayed(() -> {
            tvResultTitle.setText(ganaJ1 ? "🏆 ¡Victoria!" : "💀 Derrota...");
            tvResultTitle.setTextColor(ganaJ1 ? 0xFFFACC15 : 0xFFF87171);
            Picasso.get().load(ganador.artworkUrl()).into(ivWinnerImage);
            tvWinnerName.setText(ganador.name);
            tvWinnerStats.setText("HP restante: " + hpGanador + "/" + ganador.maxHp() + "  •  Turnos: " + turno);
            overlayResult.setAlpha(0f);
            overlayResult.setVisibility(View.VISIBLE);
            overlayResult.animate().alpha(1f).setDuration(300).start();
        }, 1500);
    }

    private String resumenUsos(BattlePokemon p, int[] usos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.moves.size(); i++) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p.moves.get(i).displayName).append(" x").append(usos[i]);
        }
        return sb.toString();
    }

    // ---------------- HISTORIAL GUARDADO ----------------

    private void mostrarHistorial() {
        List<BattleStorage.BattleRecord> lista = BattleStorage.leerHistorial(this);
        if (lista.isEmpty()) {
            Toast.makeText(this, "Todavía no hay batallas guardadas", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = new String[lista.size()];
        for (int i = 0; i < lista.size(); i++) {
            BattleStorage.BattleRecord r = lista.get(i);
            items[i] = DateFormat.format("dd/MM HH:mm", r.fecha) + "  🏆 " + r.ganador
                    + " vs " + r.perdedor + " · " + r.turnos + " turnos";
        }
        new AlertDialog.Builder(this)
                .setTitle("📜 Historial de batallas")
                .setItems(items, (d, which) -> new AlertDialog.Builder(this)
                        .setTitle(lista.get(which).ganador + " vs " + lista.get(which).perdedor)
                        .setMessage(lista.get(which).registro)
                        .setPositiveButton("Cerrar", null)
                        .show())
                .setNegativeButton("Cerrar", null)
                .setNeutralButton("Borrar todo", (d, w) -> {
                    BattleStorage.borrarHistorial(this);
                    Toast.makeText(this, "Historial borrado", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // ---------------- UI: HP, MENSAJES, REGISTRO ----------------

    private void pintarHp(boolean esJ1, int hp, int max, boolean animar) {
        View fill = esJ1 ? hpFillPlayer : hpFillEnemy;
        TextView tv = esJ1 ? tvPlayerHp : tvEnemyHp;
        float ratio = max > 0 ? (float) hp / max : 0f;

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(3));
        bg.setColor(ratio > 0.5f ? 0xFF22C55E : ratio > 0.2f ? 0xFFFACC15 : 0xFFEF4444);
        fill.setBackground(bg);

        if (!animar) {
            fill.setScaleX(ratio);
            tv.setText(hp + " / " + max);
            return;
        }
        fill.animate().scaleX(ratio).setDuration(650).start();
        int desde = parsearHp(tv.getText().toString(), hp);
        ValueAnimator contador = ValueAnimator.ofInt(desde, hp);
        contador.setDuration(650);
        contador.addUpdateListener(a -> tv.setText(a.getAnimatedValue() + " / " + max));
        contador.start();
    }

    private int parsearHp(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.split(" / ")[0].trim());
        } catch (Exception e) {
            return porDefecto;
        }
    }

    private void mensaje(String texto) {
        tvBattleMessage.setText(texto);
    }

    private void log(String linea) {
        registro.append(linea).append("\n");
        tvBattleLog.setText(registro);
        scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    // ---------------- ANIMACIONES ----------------

    // Respiración suave mientras esperan (no interfiere con los desplazamientos)
    private ObjectAnimator iniciarRespiracion(ImageView iv) {
        iv.post(() -> iv.setPivotY(iv.getHeight()));
        ObjectAnimator a = ObjectAnimator.ofFloat(iv, "scaleY", 1f, 1.05f);
        a.setDuration(900);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.start();
        return a;
    }

    // El atacante se lanza hacia el rival y regresa
    private void embestir(ImageView atacante, boolean esJ1, Runnable alImpactar) {
        float dx = dp(esJ1 ? -70 : 70);
        float dy = dp(esJ1 ? -45 : 45);
        atacante.animate()
                .translationX(-dx * 0.15f).translationY(-dy * 0.15f) // toma impulso
                .setDuration(140)
                .withEndAction(() -> atacante.animate()
                        .translationX(dx).translationY(dy)
                        .setDuration(150)
                        .withEndAction(() -> {
                            if (alImpactar != null) alImpactar.run();
                            atacante.animate().translationX(0f).translationY(0f).setDuration(240).start();
                        })
                        .start())
                .start();
    }

    // Destello del color del tipo + parpadeo y sacudida del que recibe el golpe
    private void impacto(ImageView defensor, int colorTipo) {
        View burst = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor((colorTipo & 0x00FFFFFF) | 0xAA000000);
        g.setStroke(dp(4), Color.WHITE);
        burst.setBackground(g);
        int lado = dp(110);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(lado, lado);
        arena.addView(burst, lp);
        burst.setX(defensor.getX() + defensor.getWidth() / 2f - lado / 2f);
        burst.setY(defensor.getY() + defensor.getHeight() / 2f - lado / 2f);
        burst.setScaleX(0.2f);
        burst.setScaleY(0.2f);
        burst.animate().scaleX(1.5f).scaleY(1.5f).alpha(0f).setDuration(420)
                .withEndAction(() -> arena.removeView(burst)).start();

        ObjectAnimator parpadeo = ObjectAnimator.ofFloat(defensor, "alpha", 1f, 0.15f, 1f, 0.15f, 1f);
        parpadeo.setDuration(450);
        parpadeo.start();
        ObjectAnimator sacudida = ObjectAnimator.ofFloat(defensor, "translationX",
                0f, dp(12), -dp(12), dp(8), -dp(8), 0f);
        sacudida.setDuration(380);
        sacudida.start();
    }

    // Número de daño que sube y se desvanece sobre el Pokémon golpeado
    private void numeroFlotante(ImageView objetivo, String texto, int color, int tamSp) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(color);
        tv.setTextSize(tamSp);
        tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tv.setShadowLayer(8f, 0f, 0f, Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(170), FrameLayout.LayoutParams.WRAP_CONTENT);
        arena.addView(tv, lp);

        float x = objetivo.getX() + objetivo.getWidth() / 2f - dp(85) + (random.nextInt(dp(30)) - dp(15));
        float y = objetivo.getY() + objetivo.getHeight() * 0.25f;
        tv.setX(x);
        tv.setY(y);
        tv.setScaleX(0.3f);
        tv.setScaleY(0.3f);

        tv.animate().scaleX(1.25f).scaleY(1.25f).setDuration(180)
                .setInterpolator(new OvershootInterpolator(2.5f))
                .withEndAction(() -> tv.animate()
                        .scaleX(1f).scaleY(1f)
                        .translationYBy(-dp(55)).alpha(0f)
                        .setStartDelay(250).setDuration(800)
                        .withEndAction(() -> arena.removeView(tv))
                        .start())
                .start();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        enCurso = false;
        handler.removeCallbacksAndMessages(null);
        if (idlePlayer != null) idlePlayer.cancel();
        if (idleEnemy != null) idleEnemy.cancel();
    }
}
