package com.example.proyecto_interaccion;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
public final class Recordatorios {
    private Recordatorios() {}

    private static PendingIntent intent(Context c, String citaId) {
        Intent i = new Intent(c, RecordatorioReceiver.class);
        i.putExtra(RecordatorioReceiver.EXTRA_ID, citaId);
        return PendingIntent.getBroadcast(c, citaId.hashCode(), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void programar(Context c, Almacen.Cita cita) {
        long disparo = cita.fechaHora - cita.recordatorioMin * 60_000L;
        if (disparo <= System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, disparo, intent(c, cita.id));
    }

    public static void cancelar(Context c, String citaId) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = intent(c, citaId);
        am.cancel(pi);
        pi.cancel();
    }

    public static void reprogramarTodas(Context c) {
        for (Almacen.Cita x : Almacen.citas(c)) programar(c, x);
    }
}
