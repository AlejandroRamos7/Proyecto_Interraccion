package com.example.proyecto_interaccion;

import static com.example.proyecto_interaccion.R.*;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Inicio extends AppCompatActivity implements View.OnClickListener{

    private Button b1;
    private Button b2;
    private Button b3;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.inicio);

        b1 = (Button) findViewById(id.button2);
        b1.setOnClickListener(this);

        b2 = (Button) findViewById(id.button3);
        b2.setOnClickListener(this);

        b3 = (Button) findViewById(id.button4);
        b3.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        int botoncito = v.getId();

        if(botoncito == R.id.button2){
            Toast.makeText(this, "Entrando a agenda", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Inicio.this,Agenda.class);
            startActivity(intent); }

        if(botoncito == R.id.button3){
            Toast.makeText(this, "Entrando a Traductor", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Inicio.this,Traductor.class);
            startActivity(intent); }

        if(botoncito == R.id.button4){
            Toast.makeText(this, "Entrando a Registro familiar", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Inicio.this,Familia.class);
            startActivity(intent); }


    }
}