package com.example.proyecto_interaccion;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

public final class Util {
    private Util() {}

    public static final int[] RECORDATORIO_MIN = {0, 15, 60, 180, 1440};
    public static final String[] RECORDATORIO_ETIQUETA = {
            "A la hora de la cita", "15 minutos antes", "1 hora antes", "3 horas antes", "1 día antes"};

    public static String fecha(long ms, String patron) {
        return new SimpleDateFormat(patron, Locale.forLanguageTag("es-MX")).format(new Date(ms));
    }

    public static String etiquetaRecordatorio(int minutos) {
        for (int i = 0; i < RECORDATORIO_MIN.length; i++) {
            if (RECORDATORIO_MIN[i] == minutos) return RECORDATORIO_ETIQUETA[i];
        }
        return minutos + " min antes";
    }

    public static void dropdown(Context c, AutoCompleteTextView v, List<String> items, int inicial, IntConsumer alElegir) {
        v.setAdapter(new ArrayAdapter<>(c, android.R.layout.simple_list_item_1, items));
        if (!items.isEmpty()) v.setText(items.get(inicial), false);
        v.setOnItemClickListener((parent, view, pos, id) -> alElegir.accept(pos));
    }
}
