package jp.ne.t_yankees;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String NOTIFICATION_TOPIC_SCHEDULE = "schedule";
    public static final String NOTIFICATION_TOPIC_SCOREBOOK = "scorebook";
    public static final String NOTIFICATION_TOPIC_APP = "app";
    private static final String TAG = "TY-MainActivity";
    private boolean debug = false;
    private WebView webview;
    private ListView listView;
    private final String URL_HP_ROOT = "http://t-yankees.sakura.ne.jp/";
    private final String WEBSB_CAL = URL_HP_ROOT + "websb3/s-calendar.cgi?";  //スケジュール一覧
    private final String WEBSB_SCORE = URL_HP_ROOT + "websb3/s-team.cgi?";  //勝敗結果
//    private final String WEBSB_SCORE = URL_HP_ROOT + "websb3/s-scorebook.cgi?"; //スコアブック全体のメニュー画面

    //    private String[][] menudata = {{"スケジュール"}, {"スコアブック"}};
    private List<Map<String, String>> mdata = new ArrayList<Map<String, String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        debug = isDebug();

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
        // メニューリスト
        if (mdata.size() == 0 ) {
            initMenuData();
        }
        ArrayList menu = new ArrayList<>();
        for (int i=0; i < mdata.size(); i++) {
            menu.add(getMenuTitle(i));
        }
        ArrayAdapter menuAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, menu);
        listView = (ListView)findViewById(R.id.menuListview);
        listView.setAdapter(menuAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String menu = mdata.get(position).get("title");
                String url = mdata.get(position).get("url");
                //Toast.makeText(getApplicationContext(), "menu: " + menu +", position: " + position + ", id: " + id, Toast.LENGTH_SHORT).show();
                openWebPage(url, "ユーザIDとパスワードを設定しておくと、認証された状態でページが開きます");
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
            openWebPage(redirectURL, "ユーザIDとパスワードを設定しておくと通知されたスケジュールのページが開くようになります");
        }
    }

    private void initMenuData() {
        mdata.add(new HashMap<String, String>());
        mdata.add(new HashMap<String, String>());
        mdata.get(0).put("title", "スケジュール");
        mdata.get(0).put("url", WEBSB_CAL);
        mdata.get(1).put("title", "スコアブック");
        mdata.get(1).put("url", WEBSB_SCORE);
    }
    private String getMenuTitle(int index) {
        if (mdata.size() == 0) {
            initMenuData();
        }
        return mdata.get(index).get("title");
    }
    private void openWebPage(final String url, String messageWhenNoCredential) {
        String mid = getPreferenceString(SettingsActivity.KEY_PREF_ID);
        String mpass = getPreferenceString(SettingsActivity.KEY_PREF_PASS);
        // ******************************************************************************
        // WebView#postUrl() では遷移後の１ページ目は認証情報が有効になるが、
        // そこから先に遷移すると、ブラウザアプリに制御が移りフォームデータが引き継がれず認証が無効になってしまう
        // 以下のように、loadDataでHTMLを表示すると、２ぺーじ目以降もWebView上で表示されうまく動作する
        // ******************************************************************************
//        String postData = null;
//        boolean hasCredential = false;
//        if (mid != null && mpass != null) {
//            postData = "mid=" + mid + "&mpass=" + mpass;
//            if (mid.length() > 0 && mpass.length() > 0) {
//                hasCredential = true;
//            }
//        } else {
//            postData = "mid=&mpass=";
//        }
//        String params = getIntent().getStringExtra("params");
//        if (params != null) {
//            postData += "&" + params;
//        }
//        webview = new WebView(this);
//        String userAgent = webview.getSettings().getUserAgentString();
//        webview.getSettings().setUserAgentString(userAgent + " (ty.app.android)");
////        Log.d(TAG, "url=" + url + ", postData=" + postData);
////        Log.d(TAG, "1 message=" + messageWhenNoCredential + "  " + hasCredential);
//
//        // 認証情報が未設定（あるいは不十分）で、かつその際に表示するメッセージが指定されている場合はポップアップで表示する
//        // @TODO ここからWebViewで表示する１ページ目は認証情報が有効になるが、
//        //       そこから先に遷移すると、ブラウザアプリに制御が移りフォームデータが引き継がれず認証が無効になってしまう
//        //       現状では、ここで表示するメッセージは不適切なので、ひとまず表示しないようにする
//        if (!hasCredential && messageWhenNoCredential != null) {
//            Log.d(TAG, "message=" + messageWhenNoCredential);
////            Toast.makeText(this, messageWhenNoCredential, Toast.LENGTH_LONG).show();
//        }
//        setContentView(webview);
//        webview.postUrl(url, postData.getBytes());


        //------ WebView# loadData() -------------
        // 参考）https://stackoverflow.com/questions/39506246/how-to-post-data-for-webview-android
        boolean hasCredential = false;
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
        String params = getIntent().getStringExtra("params");
        if (params != null) {
            // e.g.) param = 'nen=2018&nendo=2018&tuki=3&hi=18&dai=1'
            String[] paramArray = params.split("&");
            for (int i=0; i < paramArray.length; i++) {
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
        webview = new WebView(this);
        String userAgent = webview.getSettings().getUserAgentString();
        webview.getSettings().setUserAgentString(userAgent + " (ty.app.android)");
        webview.getSettings().setJavaScriptEnabled(true); // 以下で生成するHTMLからJavaScriptを実行させるため有効にしておく

        // 認証情報が未設定（あるいは不十分）で、かつその際に表示するメッセージが指定されている場合はポップアップで表示する
        if (!hasCredential && messageWhenNoCredential != null) {
            Log.d(TAG, "message=" + messageWhenNoCredential);
            Toast.makeText(this, messageWhenNoCredential, Toast.LENGTH_LONG).show();
        }
        setContentView(webview);
        String html = String.format("<!DOCTYPE html>" +
                "<html>" +
                "<body onload='document.frm1.submit()'>" +
                "<form action='%s' method='post' name='frm1'>" +
                " %s " +
                "</form>" +
                "</body>" +
                "</html>", url, formData);
//        Log.d(TAG, html);
        webview.loadData(html, "text/html", "UTF-8");
    }

    private void setReceiveNotificationSettings() {
        initialyzePreference(SettingsActivity.KEY_PREF_SCHEDULE, true);
        initialyzePreference(SettingsActivity.KEY_PREF_SCOREBOOK, true);
        if (getPreferenceBoolean(SettingsActivity.KEY_PREF_SCHEDULE)) {
            Log.d(TAG, ".>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Schedule");
            FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_SCHEDULE);
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCHEDULE);
        }
        if (getPreferenceBoolean(SettingsActivity.KEY_PREF_SCOREBOOK)) {
            FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_SCOREBOOK);
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC_SCOREBOOK);
        }
        FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC_APP);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //@TODO WebViewでHPを表示している時、戻るボタンが押されたらこのアプリには戻らない
//        Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< onKeyDown -1");
//        if (keyCode == KeyEvent.KEYCODE_BACK ) {
//            Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< onKeyDown -2");
//
//        }
////        if (keyCode == KeyEvent.KEYCODE_BACK ) {
////            Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< onKeyDown -3");
////            return true;
////        }
        if (keyCode == KeyEvent.KEYCODE_BACK && webview != null && webview.canGoBack()) {
//            Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< onKeyDown -4");
            webview.goBack();
            return true;
        }
//        if (keyCode == KeyEvent.KEYCODE_BACK && webview != null && !webview.canGoBack()) {
//            Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< onKeyDown -5");
//            webview.goBack();
//            finish();
////            Intent intent = new Intent(this.getClass().getName());
////            startActivity(intent);
////            return true;
//        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.menuSetting:
                Log.d(TAG, "Setting menu selected");
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.versionInfo:
                Log.d(TAG, "Version info selected");
                AlertDialog builder = new AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage("Version: " + BuildConfig.VERSION_NAME)
                        .setPositiveButton("OK", null)
                        .show();
            default:
                Log.d(TAG, "Menu item selected");
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
            Log.d(TAG, "Initialyzed preference: " + key + "-->" + defaultValue);
        }
    }
    private boolean isDebug() {
        Log.d(TAG, "isDebug :" + BuildConfig.DEBUG);
        return BuildConfig.DEBUG;
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
////        unregisterReceiver(receiver);
//        Log.d(TAG, "<<<<<<<<<<<< onDestroy()");
//        super.onDestroy();
//    }
//    @Override
//    public void onPause(){
////        unregisterReceiver(receiver);
//        Log.d(TAG, "<<<<<<<<<<<< onPause()");
//        super.onPause();
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
