package jp.ne.t_yankees;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String NOTIFICATION_TOPIC_SCHEDULE = "schedule";
    public static final String NOTIFICATION_TOPIC_SCOREBOOK = "scorebook";
    private static final String TAG = "TY-MainActivity";
    private boolean debug = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();

//        actionBar.show();
//不要となった preferenceを消す
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor editor = pref.edit();
//        editor.remove("pref_pass");
//        editor.remove("pref_notif_scorebook");
//        editor.remove("pref_notif_schedule");
//        editor.remove("pref_id");
//        editor.commit();
        if (debug) {
            Log.d(TAG, "=== ActionBar ===");
            Log.d(TAG, " height: " + actionBar.getHeight());
            Log.d(TAG, " title: " + actionBar.getTitle());
            Log.d(TAG, " subtitle: " + actionBar.getSubtitle());
            Log.d(TAG, " display options: " + actionBar.getDisplayOptions());
            debugReadAllPreferences();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        //Push通知の購読開始
        setReceiveNotificationSettings();
//        Toast.makeText(this, "通知の購読を開始しました", Toast.LENGTH_LONG).show();

//        //HPへのリンク表示
//        TextView textView = findViewById(R.id.hp_link);
//        MovementMethod mmethod = LinkMovementMethod.getInstance();
//        textView.setMovementMethod(mmethod);
//        String hpUrl = "http://t-yankees.sakura.ne.jp/";
//        CharSequence link = Html.fromHtml("<a href=\"" + hpUrl + "\">" + getString(R.string.homepage) + "</a>");
//        textView.setText(link);

        // ログをクリックした時にHPを表示するように設定
        ImageView iview = findViewById(R.id.ty_logo);
        iview.setClickable(true);
        iview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayHomepage();
            }
        });

        if (debug) {
            Log.d(TAG, "--- Extras ---");
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object obj = extras.get(key);
                    Log.d(TAG, " - " + key + ": " + obj);
                }
            }
        }

        // Notificationをタップして呼ばれた時の処理
        // Notificationに urlが設定されていたらそこに遷移させる
        // その際、extraの中の paramsをPOSTデータとして設定する
        final String redirectURL = getIntent().getStringExtra("url");
        if (redirectURL != null) {
            Log.d(TAG, "URL received from extra data: " + redirectURL);
            String mid = getPreferenceString(SettingsActivity.KEY_PREF_ID);
            String mpass = getPreferenceString(SettingsActivity.KEY_PREF_PASS);
            if (mid != null && mpass != null) {
                WebView webview = new WebView(this);
                setContentView(webview);
                String postData = "mid=" + mid + "&mpass=" + mpass;
                String params = getIntent().getStringExtra("params");
                if (params != null) {
                    postData += "&" + params;
                }
                webview.postUrl(redirectURL, postData.getBytes());
            } else {
                Toast.makeText(this, "ユーザIDとパスワードを設定しておくと通知されたスケジュールのページが開くようになります", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setReceiveNotificationSettings() {
        initialyzePreference(SettingsActivity.KEY_PREF_SCHEDULE, true);
        initialyzePreference(SettingsActivity.KEY_PREF_SCOREBOOK, true);
        if (getPreferenceBoolean(SettingsActivity.KEY_PREF_SCHEDULE)) {
            FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_SCHEDULE);
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCHEDULE);
        }
        if (getPreferenceBoolean(SettingsActivity.KEY_PREF_SCOREBOOK)) {
            FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_SCOREBOOK);
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCOREBOOK);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.menuSetting) {
            Log.d(TAG, "Setting menu selected");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private String getPreferenceString(String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        return pref.getString(key, null);
    }
    private boolean getPreferenceBoolean(String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        return pref.getBoolean(key, true);
    }
    private void initialyzePreference(String key, boolean defaultValue) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (! pref.contains(key)) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(key, defaultValue);
            editor.apply();
            Log.d(TAG, "Initialyzed preference: " + key + "-->" + defaultValue);
        }
    }
    private void debugReadAllPreferences(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String,?> map = pref.getAll();
        Log.v(TAG, "=== All Preferences ===");
        for(Map.Entry<String, ?> entry : map.entrySet()){
            String key=entry.getKey();
            Object value = entry.getValue();

            String msg=String.format("%s=%s", key,value);
            Log.v(TAG,msg);
        }
    }
    private void displayHomepage() {
        Uri uri = Uri.parse("http://t-yankees.sakura.ne.jp/");
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
    }
//    @Override
//    public void onDestroy() {
//        unregisterReceiver(receiver);
//        super.onDestroy();
//    }
//
//    protected class UpdateReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent){
//            Log.d("UpdateReceiver", "----> onReceive");
//
//            Bundle extras = intent.getExtras();
//            String msg = extras.getString("message");
//            TextView textView = findViewById(R.id.text_view);
//            textView.setText(msg);
//            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
//        }
//    }
}
