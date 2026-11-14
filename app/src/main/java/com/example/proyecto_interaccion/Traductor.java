package com.example.proyecto_interaccion;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Módulo de simplificación lingüística de resultados clínicos con IA (PLN).
 * Entrada: texto pegado y/o foto del análisis. Salida: explicación en lenguaje claro,
 * semáforo por valor, preguntas para el médico y señales de alerta. No diagnostica.
 */
public class Traductor extends BaseActivity {

    private static final String AVISO =
            "Esta explicación la genera una IA como apoyo informativo. No es un diagnóstico ni sustituye "
                    + "la opinión de tu médico. Ante cualquier duda o síntoma, consúltalo.";

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> selectorImagen =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) cargarImagen(uri);
            });

    private List<Almacen.Paciente> pacientes;
    private int pacienteSel = -1; // -1 = sin asignar
    private String imagenB64;
    private JSONObject ultimo;

    private ScrollView scroll;
    private EditText etResultados;
    private Chip chipSencillo;
    private ImageView imgPreview;
    private View filaImagen, progreso, btnTraducir;
    private LinearLayout contResultado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.traductor);
        configurarHeader("Traductor IA", "Entiende tus análisis clínicos");

        scroll = findViewById(R.id.scroll);
        etResultados = findViewById(R.id.etResultados);
        chipSencillo = findViewById(R.id.chipSencillo);
        imgPreview = findViewById(R.id.imgPreview);
        filaImagen = findViewById(R.id.filaImagen);
        progreso = findViewById(R.id.progreso);
        btnTraducir = findViewById(R.id.btnTraducir);
        contResultado = findViewById(R.id.contResultado);

        pacientes = Almacen.pacientes(this);
        List<String> opciones = new ArrayList<>();
        opciones.add("Sin asignar");
        for (Almacen.Paciente p : pacientes) opciones.add(p.nombre);
        Util.dropdown(this, (AutoCompleteTextView) findViewById(R.id.actPaciente), opciones, 0, i -> pacienteSel = i - 1);

        findViewById(R.id.btnImagen).setOnClickListener(v -> selectorImagen.launch("image/*"));
        findViewById(R.id.btnQuitar).setOnClickListener(v -> quitarImagen());
        btnTraducir.setOnClickListener(v -> traducir());
        findViewById(R.id.btnHistorial).setOnClickListener(v -> verHistorial());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hilo.shutdownNow();
    }

    // ----------------------------------------------------------------- imagen
    private void cargarImagen(Uri uri) {
        hilo.execute(() -> {
            try {
                Bitmap bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri),
                        (decoder, info, src) -> {
                            int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                            int max = Math.max(w, h);
                            if (max > 1568) {
                                float r = 1568f / max;
                                decoder.setTargetSize(Math.round(w * r), Math.round(h * r));
                            }
                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        });
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, bos);
                String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
                runOnUiThread(() -> {
                    imagenB64 = b64;
                    imgPreview.setImageBitmap(bmp);
                    filaImagen.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "No se pudo abrir la imagen.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void quitarImagen() {
        imagenB64 = null;
        imgPreview.setImageDrawable(null);
        filaImagen.setVisibility(View.GONE);
    }

    // --------------------------------------------------------------- llamada IA
    private void traducir() {
        final String texto = etResultados.getText().toString().trim();
        final String imagen = imagenB64;
        if (texto.isEmpty() && imagen == null) {
            Toast.makeText(this, "Pega el texto del análisis o adjunta una foto.", Toast.LENGTH_SHORT).show();
            return;
        }
        final boolean sencillo = chipSencillo.isChecked();
        setCargando(true);
        contResultado.setVisibility(View.GONE);

        hilo.execute(() -> {
            try {
                String bruto = ClaudeClient.traducir(texto, imagen, sencillo);
                JSONObject json = extraerJson(bruto);
                runOnUiThread(() -> {
                    setCargando(false);
                    procesar(json);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setCargando(false);
                    mostrarError(e);
                });
            }
        });
    }

    private static JSONObject extraerJson(String s) throws Exception {
        int a = s.indexOf('{'), b = s.lastIndexOf('}');
        if (a < 0 || b <= a) throw new Exception("La IA no devolvió un resultado legible. Intenta de nuevo.");
        return new JSONObject(s.substring(a, b + 1));
    }

    private void setCargando(boolean si) {
        progreso.setVisibility(si ? View.VISIBLE : View.GONE);
        btnTraducir.setEnabled(!si);
    }

    private void mostrarError(Exception e) {
        String msg = ClaudeClient.ERROR_SIN_CLAVE.equals(e.getMessage())
                ? "Falta la clave de API. Agrega ANTHROPIC_API_KEY en local.properties y vuelve a compilar la app."
                : (e instanceof java.io.IOException
                    ? "No hay conexión a internet. Revisa tu red e intenta de nuevo."
                    : e.getMessage());
        new MaterialAlertDialogBuilder(this).setTitle("No se pudo traducir")
                .setMessage(msg).setPositiveButton("Entendido", null).show();
    }

    private void procesar(JSONObject json) {
        if (!json.optBoolean("es_resultado_clinico", true)) {
            new MaterialAlertDialogBuilder(this).setTitle("No parece un resultado clínico")
                    .setMessage("Prueba con el texto o la foto de un análisis de laboratorio, estudio o informe médico.")
                    .setPositiveButton("Entendido", null).show();
            return;
        }
        Almacen.Analisis an = new Almacen.Analisis();
        an.pacienteId = pacienteSel >= 0 ? pacientes.get(pacienteSel).id : "";
        an.titulo = json.optString("titulo", "Análisis");
        an.json = json.toString();
        Almacen.guardarAnalisis(this, an);
        mostrarResultado(json);
    }

    // ---------------------------------------------------------------- resultado
    private void mostrarResultado(JSONObject o) {
        ultimo = o;
        contResultado.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);

        contResultado.addView(bloque(inf, o.optString("titulo", "Resultado").toUpperCase(),
                o.optString("resumen"), 0));

        String alerta = o.optString("senales_de_alerta", "").trim();
        if (!alerta.isEmpty()) {
            contResultado.addView(bloque(inf, "SEÑALES A VIGILAR", alerta, R.color.ambar));
        }

        JSONArray valores = o.optJSONArray("valores");
        if (valores != null && valores.length() > 0) {
            TextView t = (TextView) inf.inflate(R.layout.item_seccion, contResultado, false);
            t.setText("VALORES DEL ANÁLISIS");
            contResultado.addView(t);
            for (int i = 0; i < valores.length(); i++) {
                JSONObject v = valores.optJSONObject(i);
                if (v != null) contResultado.addView(vistaValor(inf, v));
            }
        }

        JSONArray pregs = o.optJSONArray("preguntas_medico");
        if (pregs != null && pregs.length() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pregs.length(); i++) {
                if (i > 0) sb.append("\n\n");
                sb.append("• ").append(pregs.optString(i));
            }
            contResultado.addView(bloque(inf, "PREGUNTAS PARA TU MÉDICO", sb.toString(), 0));
        }

        contResultado.addView(bloque(inf, "IMPORTANTE", AVISO, R.color.texto_suave));

        com.google.android.material.button.MaterialButton compartir =
                new com.google.android.material.button.MaterialButton(this);
        compartir.setText("Compartir con la familia");
        compartir.setAllCaps(false);
        compartir.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
        compartir.setOnClickListener(v -> compartir());
        contResultado.addView(compartir, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (56 * getResources().getDisplayMetrics().density)));

        contResultado.setVisibility(View.VISIBLE);
        scroll.post(() -> scroll.smoothScrollTo(0, contResultado.getTop()));
    }

    private View bloque(LayoutInflater inf, String titulo, String cuerpo, int colorRes) {
        MaterialCardView card = (MaterialCardView) inf.inflate(R.layout.item_bloque, contResultado, false);
        TextView t = card.findViewById(R.id.txtBloqueTitulo);
        t.setText(titulo);
        ((TextView) card.findViewById(R.id.txtBloqueCuerpo)).setText(cuerpo);
        if (colorRes != 0) {
            int c = ContextCompat.getColor(this, colorRes);
            t.setTextColor(c);
            if (colorRes == R.color.ambar) card.setStrokeColor(c);
        }
        return card;
    }

    private View vistaValor(LayoutInflater inf, JSONObject v) {
        View vista = inf.inflate(R.layout.item_valor, contResultado, false);
        ((TextView) vista.findViewById(R.id.txtNombre)).setText(v.optString("nombre"));
        ((TextView) vista.findViewById(R.id.txtValor)).setText(v.optString("valor"));
        String ref = v.optString("referencia");
        TextView txtRef = vista.findViewById(R.id.txtRef);
        txtRef.setText(ref.isEmpty() ? "" : "Referencia: " + ref);
        txtRef.setVisibility(ref.isEmpty() ? View.GONE : View.VISIBLE);
        ((TextView) vista.findViewById(R.id.txtExplicacion)).setText(v.optString("explicacion"));

        String estado = v.optString("estado", "normal");
        int color; String etiqueta;
        switch (estado) {
            case "alto":     color = R.color.rojo;       etiqueta = "Alto"; break;
            case "bajo":     color = R.color.azul_claro; etiqueta = "Bajo"; break;
            case "atencion": color = R.color.ambar;      etiqueta = "Revisar"; break;
            default:         color = R.color.verde;      etiqueta = "Normal";
        }
        TextView chip = vista.findViewById(R.id.txtEstado);
        chip.setText(etiqueta);
        chip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, color)));
        return vista;
    }

    private void compartir() {
        if (ultimo == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(ultimo.optString("titulo", "Resultado")).append("\n\n")
                .append(ultimo.optString("resumen")).append("\n");
        JSONArray vs = ultimo.optJSONArray("valores");
        if (vs != null) {
            sb.append("\nValores:\n");
            for (int i = 0; i < vs.length(); i++) {
                JSONObject v = vs.optJSONObject(i);
                if (v == null) continue;
                sb.append("- ").append(v.optString("nombre")).append(": ").append(v.optString("valor"))
                        .append(" (").append(v.optString("estado")).append(")\n");
            }
        }
        sb.append("\n").append(AVISO).append("\n— Generado con CuidaSalud");
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(i, "Compartir explicación"));
    }

    // ---------------------------------------------------------------- historial
    private void verHistorial() {
        List<Almacen.Analisis> lista = Almacen.analisis(this);
        if (lista.isEmpty()) {
            Toast.makeText(this, "Aún no hay análisis guardados.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] etiquetas = new String[lista.size()];
        for (int i = 0; i < lista.size(); i++) {
            Almacen.Analisis a = lista.get(i);
            etiquetas[i] = Util.fecha(a.fecha, "d MMM yyyy, HH:mm") + "\n"
                    + (a.pacienteId.isEmpty() ? "Sin asignar" : Almacen.nombrePaciente(this, a.pacienteId))
                    + " · " + a.titulo;
        }
        new MaterialAlertDialogBuilder(this).setTitle("Análisis anteriores")
                .setItems(etiquetas, (d, which) -> {
                    try {
                        mostrarResultado(new JSONObject(lista.get(which).json));
                    } catch (Exception e) {
                        Toast.makeText(this, "No se pudo abrir ese análisis.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cerrar", null).show();
    }
}
