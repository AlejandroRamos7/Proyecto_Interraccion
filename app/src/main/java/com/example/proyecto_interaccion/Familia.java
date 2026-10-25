package com.example.proyecto_interaccion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Familia extends AppCompatActivity implements View.OnClickListener{
    private Button b1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.familia);

        b1 = (Button) findViewById(R.id.button7);
        b1.setOnClickListener(this);


    }
    @Override
    public void onClick(View v){

        int botoncito = v.getId();

        if(botoncito == R.id.button7){
            Intent intent = new Intent(Familia.this,Inicio.class);
            startActivity(intent); }

    }

}
