package com.example.proyecto_interaccion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class Inicio extends BaseActivity {

    private TextView txtTitulo, txtDetalle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inicio);

        txtTitulo = findViewById(R.id.txtProximaTitulo);
        txtDetalle = findViewById(R.id.txtProximaDetalle);

        findViewById(R.id.cardProxima).setOnClickListener(v -> abrir(Agenda.class));
        findViewById(R.id.cardAgenda).setOnClickListener(v -> abrir(Agenda.class));
        findViewById(R.id.cardTraductor).setOnClickListener(v -> abrir(Traductor.class));
        findViewById(R.id.cardFamilia).setOnClickListener(v -> abrir(Familia.class));
        findViewById(R.id.cardEvaluacion).setOnClickListener(v -> abrir(Evaluacion.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Almacen.Cita c = Almacen.proximaCita(this);
        if (c == null) {
            txtTitulo.setText("Sin citas próximas");
            txtDetalle.setText("Toca aquí para agendar una cita y activar su recordatorio.");
        } else {
            txtTitulo.setText(c.motivo);
            String quien = Almacen.nombrePaciente(this, c.pacienteId);
            txtDetalle.setText(quien + " · " + Util.fecha(c.fechaHora, "EEEE d 'de' MMMM, HH:mm")
                    + (c.lugar.isEmpty() ? "" : "\n" + c.lugar));
        }
    }

    private void abrir(Class<?> destino) {
        startActivity(new Intent(this, destino));
    }
}
