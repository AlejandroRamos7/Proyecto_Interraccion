package com.example.proyecto_interaccion;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Evaluacion extends BaseActivity {

    private static final String[] PREGUNTAS = {
            "Creo que me gustaría usar esta aplicación con frecuencia.",
            "Encontré la aplicación innecesariamente compleja.",
            "Pensé que la aplicación era fácil de usar.",
            "Creo que necesitaría el apoyo de una persona con conocimientos técnicos para usar esta aplicación.",
            "Encontré que las diversas funciones de la aplicación estaban bien integradas.",
            "Pensé que había demasiada inconsistencia en esta aplicación.",
            "Imagino que la mayoría de las personas aprenderían a usar esta aplicación muy rápidamente.",
            "Encontré la aplicación muy incómoda de usar.",
            "Me sentí muy seguro/a al usar la aplicación.",
            "Necesité aprender muchas cosas antes de poder empezar a usar la aplicación.",

            "[Adicional] La explicación de los resultados clínicos fue clara y fácil de entender.",
            "[Adicional] Los recordatorios de citas me ayudarían a no olvidar mis citas médicas."
    };

    private final List<RadioGroup> grupos = new ArrayList<>();
    private final List<View> tarjetas = new ArrayList<>();
    private android.widget.LinearLayout contPreguntas;
    private android.widget.ScrollView scroll;
    private EditText etParticipante;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.evaluacion);
        configurarHeader("Evaluación", "Usabilidad de la aplicación (SUS)");

        scroll = findViewById(R.id.scroll);
        etParticipante = findViewById(R.id.etParticipante);
        android.widget.LinearLayout cont = findViewById(R.id.contPreguntas);
        contPreguntas = cont;

        LayoutInflater inf = LayoutInflater.from(this);
        for (int i = 0; i < PREGUNTAS.length; i++) {
            View v = inf.inflate(R.layout.item_pregunta, cont, false);
            ((TextView) v.findViewById(R.id.txtPregunta)).setText((i + 1) + ". " + PREGUNTAS[i]);
            grupos.add(v.findViewById(R.id.grupo));
            tarjetas.add(v);
            cont.addView(v);
        }

        findViewById(R.id.btnCalcular).setOnClickListener(v -> calcular());
        findViewById(R.id.btnExportar).setOnClickListener(v -> exportar());
        actualizarCohorte();
    }

    private int respuesta(RadioGroup g) {
        int id = g.getCheckedRadioButtonId();
        if (id == View.NO_ID) return 0;
        return g.indexOfChild(g.findViewById(id)) + 1;
    }

    private void calcular() {
        int[] r = new int[PREGUNTAS.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = respuesta(grupos.get(i));
            if (r[i] == 0) {
                Toast.makeText(this, "Falta responder la pregunta " + (i + 1) + ".", Toast.LENGTH_SHORT).show();
                final View destino = tarjetas.get(i);
                scroll.post(() -> scroll.smoothScrollTo(0, contPreguntas.getTop() + destino.getTop()));
                return;
            }
        }
        String codigo = etParticipante.getText().toString().trim();
        if (codigo.isEmpty()) {
            etParticipante.setError("Escribe el código del participante");
            scroll.smoothScrollTo(0, 0);
            return;
        }

        int suma = 0;
        for (int i = 0; i < 10; i++) suma += (i % 2 == 0) ? r[i] - 1 : 5 - r[i];
        double sus = suma * 2.5;

        Almacen.Evaluacion e = new Almacen.Evaluacion();
        e.participante = codigo;
        e.respuestas = r;
        e.puntajeSus = sus;
        Almacen.guardarEvaluacion(this, e);

        findViewById(R.id.cardResultado).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtPuntaje)).setText(String.format(Locale.US, "%.1f", sus));
        ((TextView) findViewById(R.id.txtCategoria)).setText(categoria(sus));
        ((TextView) findViewById(R.id.txtExtra)).setText(
                "Claridad de la traducción: " + r[10] + "/5 · Utilidad de recordatorios: " + r[11] + "/5");
        actualizarCohorte();
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        for (RadioGroup g : grupos) g.clearCheck();
        etParticipante.setText("");
    }

    private static String categoria(double sus) {
        if (sus > 80.3) return "Excelente";
        if (sus >= 68) return "Buena (sobre el promedio)";
        if (sus >= 51) return "Aceptable (mejorable)";
        return "Deficiente";
    }

    private void actualizarCohorte() {
        List<Almacen.Evaluacion> l = Almacen.evaluaciones(this);
        TextView t = findViewById(R.id.txtCohorte);
        if (l.isEmpty()) {
            t.setText("Aún no hay evaluaciones registradas en este dispositivo.");
            return;
        }
        double suma = 0;
        for (Almacen.Evaluacion e : l) suma += e.puntajeSus;
        t.setText(String.format(Locale.US, "Cohorte: %d participante(s) · SUS promedio %.1f (%s)",
                l.size(), suma / l.size(), categoria(suma / l.size())));
    }

    private void exportar() {
        List<Almacen.Evaluacion> l = Almacen.evaluaciones(this);
        if (l.isEmpty()) {
            Toast.makeText(this, "No hay evaluaciones para exportar.", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder("participante,fecha,SUS");
        for (int i = 1; i <= 12; i++) sb.append(",P").append(i);
        sb.append('\n');
        for (Almacen.Evaluacion e : l) {
            sb.append(e.participante.replace(",", " ")).append(',')
                    .append(Util.fecha(e.fecha, "yyyy-MM-dd HH:mm")).append(',')
                    .append(String.format(Locale.US, "%.1f", e.puntajeSus));
            for (int x : e.respuestas) sb.append(',').append(x);
            sb.append('\n');
        }
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Resultados SUS - CuidaSalud")
                .putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(i, "Exportar resultados"));
    }
}
