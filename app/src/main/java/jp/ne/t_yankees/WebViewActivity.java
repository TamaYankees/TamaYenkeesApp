package jp.ne.t_yankees;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by takuya on 2018/04/04.
 */

public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "TY-WebViewActivity";
    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.webview_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false); // タイトル文字を消す

        webview = (WebView) findViewById(R.id.webView);
        Intent intent = getIntent();
        if (intent != null) {
            webview.setWebViewClient(new WebViewClient());
            String userAgent = webview.getSettings().getUserAgentString();
            webview.getSettings().setUserAgentString(userAgent + " (ty.app.android)");
            webview.getSettings().setJavaScriptEnabled(true); // 以下で生成するHTMLからJavaScriptを実行させるため有効にしておく
            if (18 < Build.VERSION.SDK_INT ){
                // postUrl()で開き、ページ遷移した後で戻ろうとすると "net::ERR_CACHE_MISS" というエラーメッセージが表示され戻ることができない
                // 以下は、この問題を防止するための措置
                //   参考) https://stackoverflow.com/questions/30637654/android-webview-gives-neterr-cache-miss-message
                //18 = JellyBean MR2, KITKAT=19
                webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE); //
            }

            String url = intent.getStringExtra(MainActivity.EXTRA_URL);
            String postData = intent.getStringExtra(MainActivity.EXTRA_POST_DATA);
            Log.d(TAG, "url: " + url);
            if (url != null && postData != null) {
                webview.postUrl(url, postData.getBytes());
            } else {
                String htmlData = intent.getStringExtra(MainActivity.EXTRA_WEB_DATA);
                if (htmlData != null) {
                    Log.d(TAG, "htmlData: " + htmlData);
                    webview.loadData(htmlData, "text/html", "UTF-8");
                }
            }
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 戻るボタンでブラウザバック
        if (keyCode == KeyEvent.KEYCODE_BACK && webview != null && webview.canGoBack()) {
                webview.goBack();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item);
    }
}
