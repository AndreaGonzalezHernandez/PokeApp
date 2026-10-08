package com.cesba.pokeapp;

import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;

// Escala los sprites sin suavizado para que se vean pixel-art (como en los juegos clásicos)
public class PixelSprite implements Transformation {

    private final int escala;

    public PixelSprite(int escala) {
        this.escala = escala;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap escalado = Bitmap.createScaledBitmap(source,
                source.getWidth() * escala, source.getHeight() * escala, false);
        if (escalado != source) source.recycle();
        return escalado;
    }

    @Override
    public String key() {
        return "pixel" + escala;
    }
}
