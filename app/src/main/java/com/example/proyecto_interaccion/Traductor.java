package com.example.proyecto_interaccion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Traductor extends AppCompatActivity implements View.OnClickListener{
    private Button b1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.traductor);

        b1 = (Button) findViewById(R.id.button6);
        b1.setOnClickListener(this);


    }
    @Override
    public void onClick(View v){

        int botoncito = v.getId();

        if(botoncito == R.id.button6){
            Intent intent = new Intent(Traductor.this,Inicio.class);
            startActivity(intent); }

    }

}
