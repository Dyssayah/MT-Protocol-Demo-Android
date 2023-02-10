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

    private static final String CHANNEL_ID = "Couves";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Your code here
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_check_normal)
                .setContentTitle("My Foreground Service")
                .setContentText("Running in the foreground")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// Use a unique id for each notification you want to display
        int notificationId = 1;
        notificationManager.notify(notificationId, builder.build());

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


        Intent serviceIntent = new Intent(this, BLEService.class);
        startService(serviceIntent);
        Intent mIntent = new Intent(this, BLEService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        deviceArrayAdapter = new GLMDeviceArrayAdapter(this, R.layout.item_device, 0, devices);
        ListView deviceListView = findViewById(R.id.device_list_view);
        deviceListView.setAdapter(deviceArrayAdapter);
        deviceListView.setOnItemClickListener(this);
        measTextView = findViewById(R.id.measurement_text_view);
        devTextView = findViewById(R.id.device_text_view);

        // Use the START_STICKY constant to ensure that the service restarts if it's
        // terminated by the system
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}