package com.cesba.pokeapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

// Hexágono de estadísticas como en la pantalla de Cajas de Pokémon Escarlata/Púrpura
public class StatRadarView extends View {

    // Orden en el hexágono: arriba, arriba-der, abajo-der, abajo, abajo-izq, arriba-izq
    private static final String[] ETIQUETAS = {"PS", "Ataque", "Defensa", "Velocidad", "Def. Esp", "At. Esp"};
    private static final float MAX_STAT = 180f;

    private final int[] valores = new int[6];
    private boolean vacio = true;

    private final Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lineas = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint punto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint etiqueta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public StatRadarView(Context context) {
        this(context, null);
    }

    public StatRadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;

        fondo.setColor(0x33FFFFFF);
        lineas.setColor(0x88FFFFFF);
        lineas.setStyle(Paint.Style.STROKE);
        lineas.setStrokeWidth(1.2f * d);
        relleno.setColor(0xCC8EC5FF);
        borde.setColor(0xFFB9DCFF);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(1.5f * d);
        punto.setColor(0xFFFFFFFF);

        etiqueta.setColor(0xFFF5D33B);
        etiqueta.setTextSize(10 * d);
        etiqueta.setTextAlign(Paint.Align.CENTER);
        valor.setColor(0xFFFFFFFF);
        valor.setTextSize(13 * d);
        valor.setTypeface(Typeface.DEFAULT_BOLD);
        valor.setTextAlign(Paint.Align.CENTER);
    }

    // hp, ataque, defensa, at. especial, def. especial, velocidad (orden de la PokeAPI)
    public void setStats(int hp, int atk, int def, int spAtk, int spDef, int spd) {
        valores[0] = hp;
        valores[1] = atk;
        valores[2] = def;
        valores[3] = spd;
        valores[4] = spDef;
        valores[5] = spAtk;
        vacio = false;
        invalidate();
    }

    public void limpiar() {
        vacio = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radio = Math.min(getWidth(), getHeight()) / 2f - 34 * d;
        if (radio <= 0) return;

        // Fondo y guías
        hexagono(cx, cy, radio, null);
        canvas.drawPath(path, fondo);
        canvas.drawPath(path, lineas);
        for (int i = 0; i < 6; i++) {
            float[] p = vertice(cx, cy, radio, i);
            canvas.drawLine(cx, cy, p[0], p[1], lineas);
        }

        // Etiquetas
        for (int i = 0; i < 6; i++) {
            float[] p = vertice(cx, cy, radio + 20 * d, i);
            canvas.drawText(ETIQUETAS[i], p[0], p[1] - 3 * d, etiqueta);
            canvas.drawText(vacio ? "--" : String.valueOf(valores[i]), p[0], p[1] + 10 * d, valor);
        }

        if (vacio) return;

        // Polígono de stats
        hexagono(cx, cy, radio, valores);
        canvas.drawPath(path, relleno);
        canvas.drawPath(path, borde);
        for (int i = 0; i < 6; i++) {
            float[] p = vertice(cx, cy, escalar(radio, valores[i]), i);
            canvas.drawCircle(p[0], p[1], 3.5f * d, punto);
        }
    }

    private float escalar(float radio, int stat) {
        return radio * Math.max(0.08f, Math.min(1f, stat / MAX_STAT));
    }

    private void hexagono(float cx, float cy, float radio, int[] stats) {
        path.reset();
        for (int i = 0; i < 6; i++) {
            float r = stats != null ? escalar(radio, stats[i]) : radio;
            float[] p = vertice(cx, cy, r, i);
            if (i == 0) path.moveTo(p[0], p[1]);
            else path.lineTo(p[0], p[1]);
        }
        path.close();
    }

    private float[] vertice(float cx, float cy, float r, int i) {
        double ang = Math.toRadians(-90 + 60 * i);
        return new float[]{(float) (cx + r * Math.cos(ang)), (float) (cy + r * Math.sin(ang))};
    }
}
