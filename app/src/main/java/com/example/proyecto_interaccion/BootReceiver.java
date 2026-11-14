package com.example.proyecto_interaccion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Tras reiniciar el dispositivo las alarmas se pierden: aquí se vuelven a programar. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Recordatorios.reprogramarTodas(context);
        }
    }
}
