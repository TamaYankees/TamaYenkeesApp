package jp.ne.t_yankees;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String NOTIFICATION_TOPIC_SCHEDULE = "schedule";
    public static final String NOTIFICATION_TOPIC_SCHEDULE_TEST = "schedule_test";
    public static final String NOTIFICATION_TOPIC_SCOREBOOK = "scorebook";
    public static final String NOTIFICATION_TOPIC_SCOREBOOK_TEST = "scorebook_test";
    public static final String NOTIFICATION_TOPIC_APP = "app";
    public static final String NOTIFICATION_TOPIC_APP_TEST = "app_test";
    private static final String TAG = "TY-MainActivity";
    private static final String FLAG_HAS_WEBVIEW = "has_view";
    public static final String EXTRA_WEB_DATA = "jp.ne.t_yankees.WEB_DATA";
    public static final String EXTRA_URL = "jp.ne.t_yankees.URL";
    public static final String EXTRA_POST_DATA = "jp.ne.t_yankees.POST_DATA";

    private final String URL_HP_ROOT = "http://t-yankees.sakura.ne.jp/";
    private final String WEBSB_CAL = URL_HP_ROOT + "websb3/s-calendar.cgi?";  //スケジュール一覧
    private final String WEBSB_SCORE = URL_HP_ROOT + "websb3/s-team.cgi?";  //勝敗結果
    private List<Map<String, String>> mdata = new ArrayList<Map<String, String>>();
    private static final String APP_PAGE_SUB_URL = "app";
    private UpdateReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMainView();
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

        if (BuildConfig.DEBUG) {
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
            String params = getIntent().getStringExtra("params");
            Log.d(TAG, "URL received from extra data: " + redirectURL);
            openWebPage(redirectURL, params, "ユーザIDとパスワードを設定しておくと通知されたスケジュールのページが開くようになります");
        }
    }
    private void initMainView() {
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false); // タイトル文字を消す

        //動的に背景画像を設定 (ここでは、activity_main.xmlで指定するので使用しない
//        LinearLayout layout = (LinearLayout) findViewById(R.id.layout_main);
//        layout.setBackgroundResource(R.drawable.ty_back);

        // ログをクリックした時にHPを表示するように設定
        ImageView iview = findViewById(R.id.ty_logo);
        iview.setClickable(true);
        iview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayHomepage(null);
            }
        });
        // メニューリスト
        if (mdata.size() == 0 ) {
            initMenuData();
        }
        ArrayList menu = new ArrayList<>();
        for (int i=0; i < mdata.size(); i++) {
            menu.add(getMenuTitle(i));
        }
        ArrayAdapter menuAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, menu);
        ListView listView = (ListView)findViewById(R.id.menuListview);
        listView.setAdapter(menuAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = mdata.get(position).get("url");
                String menu = mdata.get(position).get("title");
                String params = null;
                if (menu.equals( getString(R.string.pref_title_schedule))) {
                    //スケジュール画面を開く場合は、以下のパラメータを付加する
                    //このパラメーターを指定しないと過去の予定も表示されてしまう
                    params = "scsugisw=1";
                }
                openWebPage(url, params, "ユーザIDとパスワードを設定しておくと、認証された状態でページが開きます");
            }
        });

    }
    // メインメニューを作成
    private void initMenuData() {
        mdata.add(new HashMap<String, String>());
        mdata.add(new HashMap<String, String>());
        mdata.get(0).put("title", getString(R.string.pref_title_schedule));
        mdata.get(0).put("url", WEBSB_CAL);
        mdata.get(1).put("title", getString(R.string.pref_title_scorebook));
        mdata.get(1).put("url", WEBSB_SCORE);
    }
    private String getMenuTitle(int index) {
        if (mdata.size() == 0) {
            initMenuData();
        }
        return mdata.get(index).get("title");
    }
    // 指定されたwebページを開く
    // FCMからの通知に指定されたページを開く場合と、アプリのメインメニューで指定されたページを開く場合の２つのケースを想定
    // 前者の場合は、このActivityを呼び出したIntentの中に、paramsというキーの値が設定されている場合があるので
    // その場合POSTデータにセットする
    private void openWebPage(final String url, final String params, String messageWhenNoCredential) {
        String mid = getPreferenceString(SettingsActivity.KEY_PREF_ID);
        String mpass = getPreferenceString(SettingsActivity.KEY_PREF_PASS);

        /*
         * WebViewActivityに遷移するページとフォームデータを送信
         * ２つの方法を実装：
         * 1) WebView#postURL()で表示するために、URLとフォームデータをIntentのextraに埋め込む
         * 2) WebView#loadData()で表示するために、HTML（ロードと同時にHPに遷移する）データをIntentのExtra
         *    に埋め込む
         */
        String webview_launch_mode = "post";
//        String webview_launch_mode = "html_data";

        Intent intent = new Intent(this, WebViewActivity.class);
        boolean hasCredential = false;
        if (webview_launch_mode.equals("post")) {
            // 1)
            String postData = null;
            if (mid != null && mpass != null) {
                postData = "mid=" + mid + "&mpass=" + mpass;
                if (mid.length() > 0 && mpass.length() > 0) {
                    hasCredential = true;
                }
            } else {
                postData = "mid=&mpass=";
            }
            if (params != null) {
                postData += "&" + params;
            }
            intent.putExtra(EXTRA_URL, url);
            intent.putExtra(EXTRA_POST_DATA, postData);

        } else if (webview_launch_mode.equals("html_data")) {
            //2)
            // 目的のページをロードするHTMLを作成し、WebView# loadData() に渡す
            // 参考）https://stackoverflow.com/questions/39506246/how-to-post-data-for-webview-android
            StringBuffer formData = new StringBuffer();
            if (mid != null && mpass != null) {
                formData.append(String.format("<input type='hidden' name='mid' value='%s'>", mid));
                formData.append(String.format("<input type='hidden' name='mpass' value='%s'>", mpass));
                if (mid.length() > 0 && mpass.length() > 0) {
                    hasCredential = true;
                }
            } else {
                formData.append("<input type='hidden' name='mid' value=''>");
                formData.append("<input type='hidden' name='mpass' value=''>");
            }
            // Formデータを <input>タグに置き換える。かなり汚いが仕方がない
            String params2 = getIntent().getStringExtra("params");
            if (params2 != null) {
                // e.g.) param = 'nen=2018&nendo=2018&tuki=3&hi=18&dai=1'
                String[] paramArray = params2.split("&");
                for (int i = 0; i < paramArray.length; i++) {
                    Log.d(TAG, String.format(" - %d : %s", i, paramArray[i]));
                    String[] kv = paramArray[i].split("=");
                    String key = kv[0];
                    String value = "";
                    if (kv.length >= 2) {
                        value = kv[1];
                    }
                    formData.append(String.format("<input type='hidden' name='%s' value='%s'>", key, value));
                }
            }
            String html = String.format("<!DOCTYPE html>" +
                    "<html>" +
                    "<body onload='document.frm1.submit()'>" +
                    "<form action='%s' method='post' name='frm1'>" +
                    " %s " +
                    "</form>" +
                    "</body>" +
                    "</html>", url, formData);
            intent.putExtra(EXTRA_WEB_DATA, html);
        } else {
            throw new RuntimeException("*** Unknown webview launch mode: " + webview_launch_mode);
        }
        // 認証情報が未設定（あるいは不十分）で、かつその際に表示するメッセージが指定されている場合はポップアップで表示する
        if (!hasCredential && messageWhenNoCredential != null) {
            Log.d(TAG, "message=" + messageWhenNoCredential);
            Toast.makeText(this, messageWhenNoCredential, Toast.LENGTH_LONG).show();
        }
        startActivity(intent);
    }

    private void setReceiveNotificationSettings() {
        initialyzePreference(SettingsActivity.KEY_PREF_SCHEDULE, true);
        initialyzePreference(SettingsActivity.KEY_PREF_SCOREBOOK, true);
        //念のためテスト系のトピックをUnsubscribeする
        FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCHEDULE_TEST);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCOREBOOK_TEST);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_APP_TEST);
        //schedule
        if (getPreferenceBoolean(SettingsActivity.KEY_PREF_SCHEDULE)) {
            FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_SCHEDULE);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "subscribe schedule_test");
                FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_SCHEDULE_TEST);
            }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCHEDULE);
            if (BuildConfig.DEBUG) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCHEDULE_TEST);
            }
        }
        // scorebook
        if (getPreferenceBoolean(SettingsActivity.KEY_PREF_SCOREBOOK)) {
            FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_SCOREBOOK);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "subscribe scorebook_test");
                FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_SCOREBOOK_TEST);
            }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCOREBOOK);
            if (BuildConfig.DEBUG) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCOREBOOK_TEST);
            }
        }
        FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_APP);
        if (BuildConfig.DEBUG) {
            FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_APP_TEST);
        }
        this.receiver = new UpdateReceiver();
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction("TY_NOTIFICATION_ACTION");
        registerReceiver(receiver, ifilter);
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
        switch (item.getItemId()) {
            case R.id.menuSetting:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.versionInfo:
                AlertDialog builder = new AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage("Version: " + BuildConfig.VERSION_NAME)
                        .setPositiveButton("OK", null)
                        .show();
                return true;
            case R.id.appPage:
                displayHomepage(APP_PAGE_SUB_URL);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Initialyzed preference: " + key + "-->" + defaultValue);
            }
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
    private void displayHomepage(String subPath) {
        String surl = URL_HP_ROOT;
        if (subPath != null) {
            surl += subPath;
        }
        Uri uri = Uri.parse(surl);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
    }
    private void setBackgroundImage(int res) {
        LinearLayout layout = (LinearLayout) findViewById(R.id.layout_main);
        layout.setBackgroundResource(res);
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(FLAG_HAS_WEBVIEW, true);
        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // 画面の向きの変更に応じて背景画像を切り替える
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:  // 縦長
                setBackgroundImage(R.drawable.ty_back);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:  // 横長
                setBackgroundImage(R.drawable.ty_back_h);
                break;
            default:
                break;
        }
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    protected class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent){
            if (BuildConfig.DEBUG) {
                Log.d("UpdateReceiver", "----> onReceive");
            }

            Bundle extras = intent.getExtras();
            String title = extras.getString("title");
            String body = extras.getString("body");
            String message = title + "\n" + body;
//            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            AlertDialog builder = new AlertDialog.Builder(context)
                                     .setTitle(R.string.information)
                                     .setIcon(R.mipmap.ty_logo)
                                     .setMessage(message)
                                     .setPositiveButton("OK", null)
                                     .show();
        }
    }
}
