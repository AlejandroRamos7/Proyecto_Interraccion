package com.example.proyecto_interaccion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Agenda extends AppCompatActivity implements View.OnClickListener{
    private Button b1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.agenda);

        b1 = (Button) findViewById(R.id.button5);
        b1.setOnClickListener(this);


    }
    @Override
    public void onClick(View v){

        int botoncito = v.getId();

        if(botoncito == R.id.button5){
            Intent intent = new Intent(Agenda.this,Inicio.class);
            startActivity(intent); }

    }

}
