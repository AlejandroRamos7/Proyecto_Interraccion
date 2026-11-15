package com.example.proyecto_interaccion;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Agenda extends BaseActivity {

    private LinearLayout lista;
    private View vacio;

    private final ActivityResultLauncher<String> permisoNotificaciones =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), aceptado -> {
                if (!aceptado) {
                    Toast.makeText(this, "Sin permiso, no se podra enviar notificaciones",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agenda);
        configurarHeader("Agenda", "Citas y recordatorios");

        lista = findViewById(R.id.listaCitas);
        vacio = findViewById(R.id.txtVacio);
        findViewById(R.id.fabNueva).setOnClickListener(v -> nuevaCita());

        RecordatorioReceiver.crearCanal(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pintar();
    }

    private void pintar() {
        lista.removeAllViews();
        List<Almacen.Cita> todas = Almacen.citas(this);
        vacio.setVisibility(todas.isEmpty() ? View.VISIBLE : View.GONE);

        long ahora = System.currentTimeMillis();
        List<Almacen.Cita> proximas = new ArrayList<>(), pasadas = new ArrayList<>();
        for (Almacen.Cita c : todas) (c.fechaHora >= ahora ? proximas : pasadas).add(c);
        Collections.reverse(pasadas);

        LayoutInflater inf = LayoutInflater.from(this);
        if (!proximas.isEmpty()) {
            lista.addView(seccion(inf, "PRÓXIMAS"));
            for (Almacen.Cita c : proximas) lista.addView(vistaCita(inf, c, false));
        }
        if (!pasadas.isEmpty()) {
            lista.addView(seccion(inf, "PASADAS"));
            for (Almacen.Cita c : pasadas) lista.addView(vistaCita(inf, c, true));
        }
    }

    private View seccion(LayoutInflater inf, String texto) {
        TextView t = (TextView) inf.inflate(R.layout.item_seccion, lista, false);
        t.setText(texto);
        return t;
    }

    private View vistaCita(LayoutInflater inf, Almacen.Cita c, boolean pasada) {
        View v = inf.inflate(R.layout.item_cita, lista, false);
        ((TextView) v.findViewById(R.id.txtDia)).setText(Util.fecha(c.fechaHora, "d"));
        ((TextView) v.findViewById(R.id.txtMes)).setText(Util.fecha(c.fechaHora, "MMM").toUpperCase(Locale.ROOT));
        ((TextView) v.findViewById(R.id.txtMotivo)).setText(c.motivo);
        ((TextView) v.findViewById(R.id.txtPaciente)).setText(Almacen.nombrePaciente(this, c.pacienteId));

        String detalle = Util.fecha(c.fechaHora, "EEE, HH:mm");
        if (!c.medico.isEmpty()) detalle += " · " + c.medico;
        if (!c.lugar.isEmpty()) detalle += "\n" + c.lugar;
        if (!c.notas.isEmpty()) detalle += "\n" + c.notas;
        ((TextView) v.findViewById(R.id.txtDetalle)).setText(detalle);

        TextView rec = v.findViewById(R.id.txtRecordatorio);
        if (pasada) {
            rec.setVisibility(View.GONE);
        } else {
            rec.setText("Recordatorio: " + Util.etiquetaRecordatorio(c.recordatorioMin));
        }
        v.setAlpha(pasada ? 0.55f : 1f);

        v.findViewById(R.id.btnEliminar).setOnClickListener(x ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("¿Eliminar esta cita?")
                        .setMessage(c.motivo)
                        .setPositiveButton("Eliminar", (d, w) -> {
                            Recordatorios.cancelar(this, c.id);
                            Almacen.eliminarCita(this, c.id);
                            pintar();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show());
        return v;
    }

    private void nuevaCita() {
        List<Almacen.Paciente> pacientes = Almacen.pacientes(this);
        if (pacientes.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Primero registra a una persona")
                    .setMessage("Agrega a la persona que cuidas para poder asignarle citas")
                    .setPositiveButton("Ir a Familia", (d, w) -> startActivity(new Intent(this, Familia.class)))
                    .setNegativeButton("Ahora no", null)
                    .show();
            return;
        }

        View v = getLayoutInflater().inflate(R.layout.dialog_cita, null);
        AutoCompleteTextView actPaciente = v.findViewById(R.id.actPaciente);
        AutoCompleteTextView actRec = v.findViewById(R.id.actRecordatorio);
        EditText etMotivo = v.findViewById(R.id.etMotivo);
        EditText etMedico = v.findViewById(R.id.etMedico);
        EditText etLugar = v.findViewById(R.id.etLugar);
        EditText etFecha = v.findViewById(R.id.etFecha);
        EditText etHora = v.findViewById(R.id.etHora);
        EditText etNotas = v.findViewById(R.id.etNotas);

        final int[] pacienteSel = {0};
        final int[] recSel = {2};
        final int[] fecha = {-1, -1, -1};
        final int[] hora = {-1, -1};

        List<String> nombres = new ArrayList<>();
        for (Almacen.Paciente p : pacientes) nombres.add(p.nombre);
        Util.dropdown(this, actPaciente, nombres, 0, i -> pacienteSel[0] = i);
        Util.dropdown(this, actRec, new ArrayList<>(Arrays.asList(Util.RECORDATORIO_ETIQUETA)), 2, i -> recSel[0] = i);

        etFecha.setOnClickListener(x -> {
            CalendarConstraints restriccion = new CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.now()).build();
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Fecha de la cita")
                    .setCalendarConstraints(restriccion)
                    .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.setTimeInMillis(sel);
                fecha[0] = utc.get(Calendar.YEAR);
                fecha[1] = utc.get(Calendar.MONTH);
                fecha[2] = utc.get(Calendar.DAY_OF_MONTH);
                etFecha.setText(String.format(Locale.US, "%02d/%02d/%04d", fecha[2], fecha[1] + 1, fecha[0]));
                etFecha.setError(null);
            });
            picker.show(getSupportFragmentManager(), "fecha");
        });

        etHora.setOnClickListener(x -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(hora[0] >= 0 ? hora[0] : 9)
                    .setMinute(hora[1] >= 0 ? hora[1] : 0)
                    .setTitleText("Hora de la cita")
                    .build();
            picker.addOnPositiveButtonClickListener(b -> {
                hora[0] = picker.getHour();
                hora[1] = picker.getMinute();
                etHora.setText(String.format(Locale.US, "%02d:%02d", hora[0], hora[1]));
                etHora.setError(null);
            });
            picker.show(getSupportFragmentManager(), "hora");
        });

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Nueva cita")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();

        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String motivo = etMotivo.getText().toString().trim();
            if (motivo.isEmpty()) { etMotivo.setError("Escribe el motivo"); return; }
            if (fecha[0] < 0) { etFecha.setError("Elige la fecha"); return; }
            if (hora[0] < 0) { etHora.setError("Elige la hora"); return; }

            Calendar cal = Calendar.getInstance();
            cal.set(fecha[0], fecha[1], fecha[2], hora[0], hora[1], 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                etHora.setError("Elige una hora futura");
                return;
            }

            Almacen.Cita c = new Almacen.Cita();
            c.pacienteId = pacientes.get(pacienteSel[0]).id;
            c.motivo = motivo;
            c.medico = etMedico.getText().toString().trim();
            c.lugar = etLugar.getText().toString().trim();
            c.notas = etNotas.getText().toString().trim();
            c.fechaHora = cal.getTimeInMillis();
            c.recordatorioMin = Util.RECORDATORIO_MIN[recSel[0]];

            Almacen.guardarCita(this, c);
            Recordatorios.programar(this, c);
            dialogo.dismiss();
            pintar();
            Toast.makeText(this, "Cita guardada. Te avisaremos "
                    + Util.etiquetaRecordatorio(c.recordatorioMin).toLowerCase(Locale.ROOT) + ".",
                    Toast.LENGTH_LONG).show();
        });
    }
}
