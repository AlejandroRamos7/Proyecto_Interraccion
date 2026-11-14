package com.example.proyecto_interaccion;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Almacenamiento local (SharedPreferences + JSON). Los datos de salud NO salen del
 * dispositivo, salvo el texto/foto que el usuario decide enviar al traductor de IA.
 */
public final class Almacen {
    private Almacen() {}

    private static final String PREFS = "cuidasalud_datos";
    private static final String K_PACIENTES = "pacientes";
    private static final String K_CITAS = "citas";
    private static final String K_ANALISIS = "analisis";
    private static final String K_EVALUACIONES = "evaluaciones";
    private static final int MAX_ANALISIS = 30;

    // ---------------------------------------------------------------- modelos
    public static class Paciente {
        public String id = UUID.randomUUID().toString();
        public String nombre = "", parentesco = "", condiciones = "", notas = "";
        public int edad;

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id).put("nombre", nombre).put("parentesco", parentesco)
                        .put("condiciones", condiciones).put("notas", notas).put("edad", edad);
            } catch (JSONException ignored) { }
            return o;
        }

        static Paciente from(JSONObject o) {
            Paciente p = new Paciente();
            p.id = o.optString("id", p.id);
            p.nombre = o.optString("nombre");
            p.parentesco = o.optString("parentesco");
            p.condiciones = o.optString("condiciones");
            p.notas = o.optString("notas");
            p.edad = o.optInt("edad");
            return p;
        }
    }

    public static class Cita {
        public String id = UUID.randomUUID().toString();
        public String pacienteId = "", motivo = "", medico = "", lugar = "", notas = "";
        public long fechaHora;
        public int recordatorioMin = 60;

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id).put("pacienteId", pacienteId).put("motivo", motivo).put("medico", medico)
                        .put("lugar", lugar).put("notas", notas).put("fechaHora", fechaHora)
                        .put("recordatorioMin", recordatorioMin);
            } catch (JSONException ignored) { }
            return o;
        }

        static Cita from(JSONObject o) {
            Cita c = new Cita();
            c.id = o.optString("id", c.id);
            c.pacienteId = o.optString("pacienteId");
            c.motivo = o.optString("motivo");
            c.medico = o.optString("medico");
            c.lugar = o.optString("lugar");
            c.notas = o.optString("notas");
            c.fechaHora = o.optLong("fechaHora");
            c.recordatorioMin = o.optInt("recordatorioMin", 60);
            return c;
        }
    }

    public static class Analisis {
        public String id = UUID.randomUUID().toString();
        public String pacienteId = "", titulo = "", json = "";
        public long fecha = System.currentTimeMillis();

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id).put("pacienteId", pacienteId).put("titulo", titulo)
                        .put("json", json).put("fecha", fecha);
            } catch (JSONException ignored) { }
            return o;
        }

        static Analisis from(JSONObject o) {
            Analisis a = new Analisis();
            a.id = o.optString("id", a.id);
            a.pacienteId = o.optString("pacienteId");
            a.titulo = o.optString("titulo");
            a.json = o.optString("json");
            a.fecha = o.optLong("fecha");
            return a;
        }
    }

    /** Respuestas de la escala SUS (10 ítems) + 2 ítems adicionales propios del proyecto. */
    public static class Evaluacion {
        public String id = UUID.randomUUID().toString();
        public String participante = "";
        public long fecha = System.currentTimeMillis();
        public int[] respuestas = new int[12];
        public double puntajeSus;

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                JSONArray r = new JSONArray();
                for (int x : respuestas) r.put(x);
                o.put("id", id).put("participante", participante).put("fecha", fecha)
                        .put("respuestas", r).put("puntajeSus", puntajeSus);
            } catch (JSONException ignored) { }
            return o;
        }

        static Evaluacion from(JSONObject o) {
            Evaluacion e = new Evaluacion();
            e.id = o.optString("id", e.id);
            e.participante = o.optString("participante");
            e.fecha = o.optLong("fecha");
            e.puntajeSus = o.optDouble("puntajeSus");
            JSONArray r = o.optJSONArray("respuestas");
            if (r != null) for (int i = 0; i < Math.min(12, r.length()); i++) e.respuestas[i] = r.optInt(i);
            return e;
        }
    }

    // --------------------------------------------------------------- helpers
    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static JSONArray leer(Context c, String clave) {
        try {
            return new JSONArray(prefs(c).getString(clave, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static void escribir(Context c, String clave, JSONArray a) {
        prefs(c).edit().putString(clave, a.toString()).apply();
    }

    // -------------------------------------------------------------- pacientes
    public static List<Paciente> pacientes(Context c) {
        List<Paciente> l = new ArrayList<>();
        JSONArray a = leer(c, K_PACIENTES);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) l.add(Paciente.from(o));
        }
        return l;
    }

    public static void guardarPaciente(Context c, Paciente p) {
        List<Paciente> l = pacientes(c);
        boolean reemplazado = false;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id.equals(p.id)) { l.set(i, p); reemplazado = true; }
        }
        if (!reemplazado) l.add(p);
        JSONArray a = new JSONArray();
        for (Paciente x : l) a.put(x.toJson());
        escribir(c, K_PACIENTES, a);
    }

    /** Elimina al paciente junto con sus citas (y recordatorios) y sus análisis. */
    public static void eliminarPaciente(Context c, String id) {
        JSONArray a = new JSONArray();
        for (Paciente p : pacientes(c)) if (!p.id.equals(id)) a.put(p.toJson());
        escribir(c, K_PACIENTES, a);

        for (Cita ci : citas(c)) {
            if (ci.pacienteId.equals(id)) {
                Recordatorios.cancelar(c, ci.id);
                eliminarCita(c, ci.id);
            }
        }
        JSONArray an = new JSONArray();
        for (Analisis x : analisis(c)) if (!x.pacienteId.equals(id)) an.put(x.toJson());
        escribir(c, K_ANALISIS, an);
    }

    public static String nombrePaciente(Context c, String id) {
        for (Paciente p : pacientes(c)) if (p.id.equals(id)) return p.nombre;
        return "Sin asignar";
    }

    // ------------------------------------------------------------------ citas
    /** Todas las citas, ordenadas de la más cercana a la más lejana. */
    public static List<Cita> citas(Context c) {
        List<Cita> l = new ArrayList<>();
        JSONArray a = leer(c, K_CITAS);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) l.add(Cita.from(o));
        }
        Collections.sort(l, (x, y) -> Long.compare(x.fechaHora, y.fechaHora));
        return l;
    }

    public static Cita cita(Context c, String id) {
        if (id == null) return null;
        for (Cita x : citas(c)) if (x.id.equals(id)) return x;
        return null;
    }

    public static void guardarCita(Context c, Cita ci) {
        JSONArray a = leer(c, K_CITAS);
        a.put(ci.toJson());
        escribir(c, K_CITAS, a);
    }

    public static void eliminarCita(Context c, String id) {
        JSONArray a = new JSONArray();
        for (Cita x : citas(c)) if (!x.id.equals(id)) a.put(x.toJson());
        escribir(c, K_CITAS, a);
    }

    public static Cita proximaCita(Context c) {
        long ahora = System.currentTimeMillis();
        for (Cita x : citas(c)) if (x.fechaHora >= ahora) return x;
        return null;
    }

    public static int citasProximas(Context c, String pacienteId) {
        long ahora = System.currentTimeMillis();
        int n = 0;
        for (Cita x : citas(c)) if (x.pacienteId.equals(pacienteId) && x.fechaHora >= ahora) n++;
        return n;
    }

    // --------------------------------------------------------------- análisis
    /** Historial de análisis traducidos, del más reciente al más antiguo. */
    public static List<Analisis> analisis(Context c) {
        List<Analisis> l = new ArrayList<>();
        JSONArray a = leer(c, K_ANALISIS);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) l.add(Analisis.from(o));
        }
        Collections.sort(l, (x, y) -> Long.compare(y.fecha, x.fecha));
        return l;
    }

    public static void guardarAnalisis(Context c, Analisis an) {
        List<Analisis> l = analisis(c);
        l.add(0, an);
        JSONArray a = new JSONArray();
        for (int i = 0; i < Math.min(MAX_ANALISIS, l.size()); i++) a.put(l.get(i).toJson());
        escribir(c, K_ANALISIS, a);
    }

    public static int totalAnalisis(Context c, String pacienteId) {
        int n = 0;
        for (Analisis x : analisis(c)) if (x.pacienteId.equals(pacienteId)) n++;
        return n;
    }

    // ------------------------------------------------------------ evaluaciones
    public static List<Evaluacion> evaluaciones(Context c) {
        List<Evaluacion> l = new ArrayList<>();
        JSONArray a = leer(c, K_EVALUACIONES);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) l.add(Evaluacion.from(o));
        }
        return l;
    }

    public static void guardarEvaluacion(Context c, Evaluacion e) {
        JSONArray a = leer(c, K_EVALUACIONES);
        a.put(e.toJson());
        escribir(c, K_EVALUACIONES, a);
    }
}
