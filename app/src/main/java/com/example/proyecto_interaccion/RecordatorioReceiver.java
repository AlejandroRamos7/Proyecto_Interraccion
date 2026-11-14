package com.example.proyecto_interaccion;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class RecordatorioReceiver extends BroadcastReceiver {
    public static final String EXTRA_ID = "cita_id";
    public static final String CANAL = "citas";

    public static void crearCanal(Context c) {
        NotificationChannel canal = new NotificationChannel(
                CANAL, "Recordatorios de citas", NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription("Avisos de próximas citas médicas");
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(canal);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        Almacen.Cita cita = Almacen.cita(c, intent.getStringExtra(EXTRA_ID));
        if (cita == null) return; // la cita fue eliminada

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        crearCanal(c);
        String paciente = Almacen.nombrePaciente(c, cita.pacienteId);
        String texto = paciente + ": " + cita.motivo + " · " + Util.fecha(cita.fechaHora, "EEE d MMM, HH:mm")
                + (cita.lugar.isEmpty() ? "" : " · " + cita.lugar);

        Intent abrir = new Intent(c, Agenda.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, cita.id.hashCode(), abrir,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(ContextCompat.getColor(c, R.color.azul))
                .setContentTitle("Recordatorio de cita médica")
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(c).notify(cita.id.hashCode(), b.build());
        } catch (SecurityException ignored) { }
    }
}
