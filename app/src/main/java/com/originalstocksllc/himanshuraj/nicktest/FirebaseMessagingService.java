package com.originalstocksllc.himanshuraj.nicktest;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private String FRND_REQ_CHANNEL_ID = "FRIEND_REQ";
    private DatabaseReference mNotificationDatabase;


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            Log.d("onMessageReceived", "Message data payload: " + remoteMessage.getData());

            scheduleJob(remoteMessage);


        }else {
            Toast.makeText(this, "Not able to recieve notifications.", Toast.LENGTH_SHORT).show();
        }
        //String from_user_id = "zm49B16MfJWgJNXO7ELpIAn0HRP2";

    }

    private void scheduleJob(RemoteMessage remoteMessage) {

        String notificationTittle = remoteMessage.getNotification().getTitle();
        String notificationMsg = remoteMessage.getNotification().getBody();
        String click_action = remoteMessage.getNotification().getClickAction();
        String from_user_id = remoteMessage.getData().get("from_user_id");


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, FRND_REQ_CHANNEL_ID)
                .setSmallIcon(R.drawable.notificaation_icon)
                .setContentTitle(notificationTittle)
                .setContentText(notificationMsg)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Intent Action

        Intent resultAction = new Intent(click_action);
        resultAction.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resultAction.putExtra("user_id", from_user_id);

        PendingIntent resultPendingIntent = PendingIntent
                .getActivity(this, 0, resultAction, PendingIntent.FLAG_ONE_SHOT);
        mBuilder.setContentIntent(resultPendingIntent);


        // notificationId is a unique int for each notification that you must define
        int notificationId = (int) System.currentTimeMillis();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(FRND_REQ_CHANNEL_ID, "Default channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        } else {
            notificationManager.notify(notificationId, mBuilder.build());
        }


    }

   /* @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
    }*/
}
