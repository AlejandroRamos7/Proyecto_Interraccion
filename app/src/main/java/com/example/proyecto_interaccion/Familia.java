package com.example.proyecto_interaccion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class Familia extends BaseActivity {

    private LinearLayout lista;
    private View vacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.familia);
        configurarHeader("Familia", "Personas que cuidas");

        lista = findViewById(R.id.listaPacientes);
        vacio = findViewById(R.id.txtVacio);
        findViewById(R.id.fabNuevo).setOnClickListener(v -> nuevoPaciente());
    }

    @Override
    protected void onResume() {
        super.onResume();
        pintar();
    }

    private void pintar() {
        lista.removeAllViews();
        List<Almacen.Paciente> ps = Almacen.pacientes(this);
        vacio.setVisibility(ps.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inf = LayoutInflater.from(this);
        for (Almacen.Paciente p : ps) {
            View v = inf.inflate(R.layout.item_paciente, lista, false);
            ((TextView) v.findViewById(R.id.txtInicial)).setText(
                    p.nombre.isEmpty() ? "?" : p.nombre.substring(0, 1).toUpperCase());
            ((TextView) v.findViewById(R.id.txtNombre)).setText(p.nombre);

            String datos = p.parentesco;
            if (p.edad > 0) datos += (datos.isEmpty() ? "" : " · ") + p.edad + " años";
            TextView txtDatos = v.findViewById(R.id.txtDatos);
            txtDatos.setText(datos);
            txtDatos.setVisibility(datos.isEmpty() ? View.GONE : View.VISIBLE);

            TextView txtCond = v.findViewById(R.id.txtCondiciones);
            String extra = p.condiciones;
            if (!p.notas.isEmpty()) extra += (extra.isEmpty() ? "" : "\n") + p.notas;
            txtCond.setText(extra);
            txtCond.setVisibility(extra.isEmpty() ? View.GONE : View.VISIBLE);

            int citas = Almacen.citasProximas(this, p.id);
            int analisis = Almacen.totalAnalisis(this, p.id);
            ((TextView) v.findViewById(R.id.txtResumen)).setText(citas + (citas == 1 ? " cita próxima" : " citas próximas") + " · " + analisis + (analisis == 1 ? " análisis" : " análisis"));

            v.findViewById(R.id.btnEliminar).setOnClickListener(x ->
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("¿Eliminar a " + p.nombre + "?")
                            .setMessage("También se borrarán sus citas, recordatorios y análisis guardados.")
                            .setPositiveButton("Eliminar", (d, w) -> {
                                Almacen.eliminarPaciente(this, p.id);
                                pintar();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show());
            lista.addView(v);
        }
    }

    private void nuevoPaciente() {
        View v = getLayoutInflater().inflate(R.layout.dialog_paciente, null);
        EditText etNombre = v.findViewById(R.id.etNombre);
        EditText etParentesco = v.findViewById(R.id.etParentesco);
        EditText etEdad = v.findViewById(R.id.etEdad);
        EditText etCond = v.findViewById(R.id.etCondiciones);
        EditText etNotas = v.findViewById(R.id.etNotas);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Agregar persona")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();

        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                etNombre.setError("Escribe un nombre");
                return;
            }
            Almacen.Paciente p = new Almacen.Paciente();
            p.nombre = nombre;
            p.parentesco = etParentesco.getText().toString().trim();
            p.condiciones = etCond.getText().toString().trim();
            p.notas = etNotas.getText().toString().trim();
            try {
                p.edad = Integer.parseInt(etEdad.getText().toString().trim());
            } catch (NumberFormatException e) {
                p.edad = 0;
            }
            Almacen.guardarPaciente(this, p);
            dialogo.dismiss();
            pintar();
            Toast.makeText(this, "Persona agregada", Toast.LENGTH_SHORT).show();
        });
    }
}
