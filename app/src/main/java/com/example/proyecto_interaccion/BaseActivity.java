package com.example.proyecto_interaccion;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ViewGroup contenedor = findViewById(android.R.id.content);
        View raiz = contenedor.getChildAt(0);
        final int pl = raiz.getPaddingLeft(), pt = raiz.getPaddingTop(),
                pr = raiz.getPaddingRight(), pb = raiz.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(pl + b.left, pt + b.top, pr + b.right, pb + b.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    protected void configurarHeader(String titulo, String subtitulo) {
        ((TextView) findViewById(R.id.txtTitulo)).setText(titulo);
        ((TextView) findViewById(R.id.txtSubtitulo)).setText(subtitulo);
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
    }
}
