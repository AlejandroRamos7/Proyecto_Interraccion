package com.example.proyecto_interaccion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

public class Pantalla_inicio extends BaseActivity {

    private VideoView video;
    private boolean yaNavego = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pantalla_inicio);

        video = findViewById(R.id.videoView);

        String videopath = "android.resource://" + getPackageName() + "/" + R.raw.video;
        video.setVideoURI(Uri.parse(videopath));

        video.setOnCompletionListener(mp -> irAlInicio());

        video.setOnErrorListener((mp, what, extra) -> {
            irAlInicio();
            return true;
        });

        video.start();
    }

    private void irAlInicio() {
        if (yaNavego) return;
        yaNavego = true;
        startActivity(new Intent(Pantalla_inicio.this, MainActivity.class));
        finish();
    }
}
