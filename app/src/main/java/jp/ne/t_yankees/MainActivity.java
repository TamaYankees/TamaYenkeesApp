package jp.ne.t_yankees;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String NOTIFICATION_TOPIC_SCHEDULE = "schedule";
    public static final String NOTIFICATION_TOPIC_SCHEDULE_TEST = "schedule_test";
    public static final String NOTIFICATION_TOPIC_BOARD = "board";
    public static final String NOTIFICATION_TOPIC_BOARD_TEST = "board_test";
    public static final String NOTIFICATION_TOPIC_SCOREBOOK = "scorebook";
    public static final String NOTIFICATION_TOPIC_SCOREBOOK_TEST = "scorebook_test";
    public static final String NOTIFICATION_TOPIC_APP = "app";
    public static final String NOTIFICATION_TOPIC_APP_TEST = "app_test";
    private static final String TAG = "TY-MainActivity";
    private static final String FLAG_HAS_WEBVIEW = "has_view";
    public static final String EXTRA_WEB_DATA = "jp.ne.t_yankees.WEB_DATA";
    public static final String EXTRA_URL = "jp.ne.t_yankees.URL";
    public static final String EXTRA_POST_DATA = "jp.ne.t_yankees.POST_DATA";
    private static final int REQUEST_CODE_SETTING = 1;

    private final String URL_HP_ROOT = "https://t-yankees.sakura.ne.jp/";
    private final String WEBSB_CAL = URL_HP_ROOT + "websb3/s-calendar.cgi?";  //スケジュール一覧
    private final String WEBSB_BOARD = URL_HP_ROOT + "websb3/s-webbbs.cgi?";  //メンバー専用掲示板
    private final String WEBSB_SCORE = URL_HP_ROOT + "websb3/s-team.cgi?";  //勝敗結果
    private final String WEBSB_PERSONAL_RECORD = URL_HP_ROOT + "websb3/s-kojin.cgi?";  //個人成績
    private List<Map<String, String>> mdata = new ArrayList<Map<String, String>>();
    private static final String APP_PAGE_SUB_URL = "app";
    private static final String APP_MANUAL_SUB_URL = "app/manual";
    private final String URL_APP_VER = URL_HP_ROOT + APP_PAGE_SUB_URL + "/current_version"; //最新のアプリバージョンを取得するURL
    private UpdateReceiver receiver;
    private Map<String, Map<Integer, Integer>> bgImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        FirebaseApp.initializeApp(this);
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        setContentView(R.layout.activity_main);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("FCM", "TOKEN = " + token);
                });


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
            openWebPage(redirectURL, params, getString(R.string.id_pass_setting_message));
        }
        checkVersion(URL_APP_VER);
    }
    private void initMainView() {
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false); // タイトル文字を消す

        //動的に背景画像を設定
        setBackgroundImage();

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
                String params = getWebPageParameter(menu);
                openWebPage(url, params, getString(R.string.id_pass_setting_message2));
            }
        });
    }
    // メインメニューを作成
    private void initMenuData() {
        mdata.add(new HashMap<String, String>());
        mdata.add(new HashMap<String, String>());
        mdata.add(new HashMap<String, String>());
        mdata.add(new HashMap<String, String>());
        mdata.get(0).put("title", getString(R.string.pref_title_schedule));
        mdata.get(0).put("url", WEBSB_CAL);
        mdata.get(1).put("title", getString(R.string.pref_title_board));
        mdata.get(1).put("url", WEBSB_BOARD);
        mdata.get(2).put("title", getString(R.string.pref_title_scorebook));
        mdata.get(2).put("url", WEBSB_SCORE);
        mdata.get(3).put("title", getString(R.string.pref_title_personal_record));
        mdata.get(3).put("url", WEBSB_PERSONAL_RECORD);
    }
    private String getMenuTitle(int index) {
        if (mdata.size() == 0) {
            initMenuData();
        }
        return mdata.get(index).get("title");
    }

    private void displayMessage(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tview = (TextView) findViewById(R.id.messateTextview);
                tview.setText(message);
                tview.setVisibility(View.VISIBLE);
            }
        });
    }
    private void displayNewAppVersionMessage(final String newVer) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tview = (TextView) findViewById(R.id.messateTextview);
                // タップされた時のOnClickListenerに openAppPageListenerを指定して、アプリページを開くようにする
                tview.setOnClickListener(new openAppPageListener());
                tview.setText(getString(R.string.new_app_version_message) + ": " + newVer);
                tview.setVisibility(View.VISIBLE);
            }
        });
    }
    private void clearMessage() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tview =  (TextView) findViewById(R.id.messateTextview);
                tview.setText("");
                tview.setVisibility(View.INVISIBLE);
            }
        });
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
        String webview_launch_mode = "post";    // ==> 1)
//        String webview_launch_mode = "html_data";  // ==> 2)

        Intent intent = new Intent(this, WebViewActivity.class);
        boolean hasCredential = false;
        if (webview_launch_mode.equals("post")) {
            // 1)
            String postData = null;
            if (mid != null && mpass != null &&
                mid.length() > 0 && mpass.length() > 0) {
                postData = "mid=" + mid + "&mpass=" + mpass;
                hasCredential = true;
            }
            if (params != null) {
                if (postData != null) {
                    postData += "&";
                }
                postData += params;
            }
            intent.putExtra(EXTRA_URL, url);
            if (postData != null) {
                intent.putExtra(EXTRA_POST_DATA, postData);
            }

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
            Toast.makeText(this, messageWhenNoCredential, Toast.LENGTH_LONG).show();
        }
        startActivity(intent);
    }
    /**
    各Webページ用のパラメータを取得する
    - スケジュール ()
     */
    private String getWebPageParameter(String menu) {
        String params = "";
        if (menu.equals( getString(R.string.pref_title_schedule))) {
            //スケジュール画面を開く場合は、以下のパラメーターを指定しないと過去の予定も表示されてしまう。
            params = "scsugisw=1";
        } else if (menu.equals(getString(R.string.pref_title_scorebook)) ||
                   menu.equals( getString(R.string.pref_title_personal_record)) ) {
            //対戦成績及び個人成績の画面は今年度の成績を開く。このパラメータを指定しないと通算成績が表示される。
            Date today = new Date();
            SimpleDateFormat df = new SimpleDateFormat("yyyy");
            params = "mode_nendo=" + df.format(today);
        }
        return params;
    }
    private void setReceiveNotificationSettings() {
        initialyzePreference(SettingsActivity.KEY_PREF_SCHEDULE, true);
        initialyzePreference(SettingsActivity.KEY_PREF_BOARD, true);
        initialyzePreference(SettingsActivity.KEY_PREF_SCOREBOOK, true);
        //念のためテスト系のFirebaseのトピックをUnsubscribeする
        FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCHEDULE_TEST);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_BOARD_TEST);
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
        //board
        if (getPreferenceBoolean(SettingsActivity.KEY_PREF_BOARD)) {
            FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_BOARD);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "subscribe board_test");
                FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_BOARD_TEST);
            }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_BOARD);
            if (BuildConfig.DEBUG) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_BOARD_TEST);
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.menuSetting:
                // 設定画面を開く、startActivityForResult()で開き、onActivityResult()で更新内容を受け取る
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SETTING);
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
            case R.id.appManual:
                displayHomepage(APP_MANUAL_SUB_URL);
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
    private void setBackgroundImage() {
        Configuration config = getResources().getConfiguration();
        setBackgroundImage(config.orientation);
    }
    private void setBackgroundImage(int orientation) {
        String background = getPreferenceString(SettingsActivity.KEY_PREF_BACKGROUND);
        int bgImageId = getBackgroundImageId(background, orientation);
        if (bgImageId != -1) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.layout_main);
            layout.setBackgroundResource(bgImageId);
        }
    }
    /**
     * 背景画像のリソースIDを返す.
     *
     * 新しい背景画像を追加した時はこの中で初期化しているbgImagesに登録する
     *
     * @param background 背景画像のキー
     * @param orientation 方向
     * @return 背景画像のリソースI
     */
    private int getBackgroundImageId(String background, int orientation) {
        if (bgImages == null) {
            bgImages = new HashMap<String, Map<Integer, Integer>>();
            bgImages.put("ty_back", new HashMap<Integer, Integer>());
            bgImages.get("ty_back").put(Configuration.ORIENTATION_PORTRAIT, R.drawable.ty_back);
            bgImages.get("ty_back").put(Configuration.ORIENTATION_LANDSCAPE, R.drawable.ty_back_h);
            bgImages.put("ty_back2", new HashMap<Integer, Integer>());
            bgImages.get("ty_back2").put(Configuration.ORIENTATION_PORTRAIT, R.drawable.ty_back2);
            bgImages.get("ty_back2").put(Configuration.ORIENTATION_LANDSCAPE, R.drawable.ty_back2_h);
            bgImages.put("wakabayashi", new HashMap<Integer, Integer>());
            bgImages.get("wakabayashi").put(Configuration.ORIENTATION_PORTRAIT, R.drawable.wakabayashi);
            bgImages.get("wakabayashi").put(Configuration.ORIENTATION_LANDSCAPE, R.drawable.wakabayashi_h);
        }
        if (! bgImages.containsKey(background) || ! bgImages.get(background).containsKey(orientation)) {
            Log.e(TAG, "Cannot detect background image : " + background + ", " + orientation);
            return -1;
        }
        return bgImages.get(background).get(orientation);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(FLAG_HAS_WEBVIEW, true);
        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setBackgroundImage(newConfig.orientation);
        super.onConfigurationChanged(newConfig);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_CODE_SETTING && resultCode == RESULT_OK) {
            Bundle bundle = intent.getExtras();
            //Log.d(TAG, "onActivityResult requestCode: " + requestCode + ", resultCode: " + resultCode + ", bundle:" + bundle.toString());

            //TODO: 購読の設定は呼び出し元(SettingsActivity#onSharedPreferenceChanged)で行っているのでここでは何もしないが、このAcvitityで一括してやるようにした方が良さそう
//            if (bundle.containsKey(SettingsActivity.KEY_PREF_SCHEDULE)) {
//                Log.d(TAG, "    KEY_PREF_SCHEDULE: " + bundle.getBoolean(SettingsActivity.KEY_PREF_SCHEDULE));
//            }
//            if (bundle.containsKey(SettingsActivity.KEY_PREF_SCOREBOOK)) {
//                Log.d(TAG, "    KEY_PREF_SCOREBOOK: " + bundle.getBoolean(SettingsActivity.KEY_PREF_SCOREBOOK));
//            }
            if (bundle.containsKey(SettingsActivity.KEY_PREF_BACKGROUND)) {
                //※ Bundleに設定で指定された値（画像名）が登録されているが、setBackgroundImage()内でPreferencesから
                //  設定値を取得して背景画像に設定するので必要がない
                //String background = bundle.getString(SettingsActivity.KEY_PREF_BACKGROUND);
                setBackgroundImage();
            }
        }
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
    private String getCurrentVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private void checkVersion(final String appVerUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(appVerUrl);
                    HttpURLConnection con = (HttpURLConnection)url.openConnection();
                    String latestVersion = InputStreamToString(con.getInputStream());
                    String currentVersion = getCurrentVersion();
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[Version] latest:" + latestVersion + ", current:" + currentVersion);
                    }
                    if (Float.parseFloat(latestVersion) > Float.parseFloat(currentVersion)) {
                        // ここ(UIスレッド外)で getString()を行うと、"Only the original thread that created a view hierarchy can touch its views." のエラーが出るので
                        // getString()を内部のUIスレッドで行う displayNewAppVersionMessage()を作成して呼び出す
                        displayNewAppVersionMessage(latestVersion);
                    }
                } catch(Exception ex) {
                    Log.e(TAG, ex.getClass().getName() + ":" + ex.getMessage());
                }
            }
        }).start();
    }

    // InputStream -> String
    static String InputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
    class openAppPageListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            displayHomepage(APP_PAGE_SUB_URL);
            clearMessage();
        }
    }
}
