package com.cesba.pokeapp;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private VideoView videoViewSplash;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        videoViewSplash = findViewById(R.id.videoViewSplash);

        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash_video);
            videoViewSplash.setVideoURI(videoUri);

            videoViewSplash.setOnPreparedListener(mp -> {
                mediaPlayer = mp;
                mediaPlayer.setLooping(false);
                mediaPlayer.setVolume(0.3f, 0.3f);

                // 📐 Ajuste dinámico para cubrir toda la pantalla sin barras negras
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();

                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int screenWidth = metrics.widthPixels;
                int screenHeight = metrics.heightPixels;

                ViewGroup.LayoutParams lp = videoViewSplash.getLayoutParams();
                if (videoWidth > 0 && videoHeight > 0) {
                    if (videoWidth * screenHeight > screenWidth * videoHeight) {
                        lp.width = (videoWidth * screenHeight) / videoHeight;
                        lp.height = screenHeight;
                    } else {
                        lp.width = screenWidth;
                        lp.height = (videoHeight * screenWidth) / videoWidth;
                    }
                    videoViewSplash.setLayoutParams(lp);
                }

                videoViewSplash.start();
            });

            // Avanzar automáticamente cuando termine el video
            videoViewSplash.setOnCompletionListener(mp -> {
                verificarSesionYContinuar();
            });

        } catch (Exception e) {
            e.printStackTrace();
            verificarSesionYContinuar();
        }
    }

    private void verificarSesionYContinuar() {
        if (isFinishing()) return;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        Intent intent;
        if (currentUser != null) {
            intent = new Intent(SplashActivity.this, PokedexActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoViewSplash != null && !videoViewSplash.isPlaying()) {
            videoViewSplash.start();
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