package jp.ne.t_yankees;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import androidx.annotation.NonNull;

import android.os.Build;
import android.util.Log;
import java.util.Map;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by takuya on 2018/03/18.
 */

public class TamaYankeesFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "TamaYankeesFirebaseMsgService";
    private static final String CHANNEL_ID = "ty_default_channel";
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "New token: " + token);
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "===== onMessageReceived =====");
            Log.d(TAG, "getCollapseKey() : " + remoteMessage.getCollapseKey());
            Log.d(TAG, "getFrom() : " + remoteMessage.getFrom());
            Log.d(TAG, "getMessageId() : " + remoteMessage.getMessageId());
            Log.d(TAG, "getMessageType() : " + remoteMessage.getMessageType());
            RemoteMessage.Notification notif = remoteMessage.getNotification();
            if (notif != null) {
                Log.d(TAG, "getNotification().getTitle() : " + notif.getTitle());
                Log.d(TAG, "getNotification().getBody() : " + notif.getBody());
                Log.d(TAG, "getNotification().getTag() : " + notif.getTag());
            } else {
                Log.d(TAG, "*** fail to get Notification instance.");
            }
            Log.d(TAG, "getSentTime() : " + remoteMessage.getSentTime());
            Log.d(TAG, "getTo() : " + remoteMessage.getTo());
            Log.d(TAG, "getTtl()  : " + remoteMessage.getTtl());
            Map<String, String> datad = remoteMessage.getData();
            Log.d(TAG, "data:" + datad.toString());
        }
        Map<String, String> data = remoteMessage.getData();
        String url = null;
        if (data.containsKey("url")) {
            url = data.get("url").toString();
        }
        StringBuffer params = new StringBuffer();
        if (data.containsKey("ymd")) {
            params.append(data.get("ymd").toString());
        }
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification != null) {
            sendMessage(notification.getTitle(),
                    notification.getBody(),
                    url,
                    params.toString()
            );
        }
    }
// 参考) 時間がかかる場合は以下のように処理する
//    /**
//     * Schedule a job using FirebaseJobDispatcher.
//     */
//    private void scheduleJob() {
//        // [START dispatch_job]
//        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
//        Job myJob = dispatcher.newJobBuilder()
//                .setService(MyJobService.class)
//                .setTag("my-job-tag")
//                .build();
//        dispatcher.schedule(myJob);
//        // [END dispatch_job]
//    }

    //（未使用）ここから新たに通知を出す場合の処理
    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody) {
        Log.d(TAG, "------> sendNotification");

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        intent.putExtra("name1", "value1");
        intent.putExtra("name2", "value2");
        intent.putExtra("name3", "value3");

//        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Channel 作成
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "多摩ヤンキース通知",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.googleg_standard_color_18) // ← 必須
                        .setContentTitle("多摩ヤンキースからの通知")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
//                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                        .setContentIntent(pendingIntent);

//        // タップ時に呼ばれるIntentを生成
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//        intent.putExtra("name1", "value1");
//        intent.putExtra("name2", "value2");
//        intent.putExtra("name3", "value3");
////        intent.putExtra(MainActivity.ARG_TYPE, contentType);
//        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        notificationBuilder.setContentIntent(contentIntent);
//        // Since android Oreo notification channel is needed.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(channelId,
//                    "Channel human readable title",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
    protected void sendMessage(String title, String body, String url, String params){
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "----> sendMessage : " + title + ", " + body + ", " + url + ", " + params);
        }
        Intent broadcast = new Intent();
        broadcast.putExtra("title", title);
        broadcast.putExtra("body", body);
        broadcast.putExtra("url", url);
        broadcast.putExtra("params", params);
        broadcast.setAction("TY_NOTIFICATION_ACTION");
        getBaseContext().sendBroadcast(broadcast);
    }
}
