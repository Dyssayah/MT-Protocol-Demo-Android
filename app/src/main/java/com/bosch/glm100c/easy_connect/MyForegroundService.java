package com.bosch.glm100c.easy_connect;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.ListView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bosch.glm100c.easy_connect.bluetooth.BLEService;

public class MyForegroundService extends Service {
    BLEService btService;
    private static final String CHANNEL_ID = "Cimede";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        btService = (BLEService) intent.getParcelableExtra("bleService");

        // Your code here
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent finishTaskIntent = new Intent(this, FinishTaskActivity.class);
        finishTaskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent finishIntent = PendingIntent.getActivity(this, 1, finishTaskIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_transparent)
                .setContentTitle("Serviço de medições")
                .setContentText("Ligado ao dispositivo")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(0, "Desligar", finishIntent)
                .setContentIntent(pendingIntent);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// Use a unique id for each notification you want to display
        int notificationId = 1;
        startForeground(notificationId , builder.build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }

        // Use the START_STICKY constant to ensure that the service restarts if it's
        // terminated by the system
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
