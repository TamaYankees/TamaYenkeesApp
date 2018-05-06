package jp.ne.t_yankees;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Created by takuya on 2018/03/26.
 */

public class SettingsActivity extends Activity
                              implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_PREF_ID = "pref_key_id";
    public static final String KEY_PREF_PASS = "pref_key_pass";
    public static final String KEY_PREF_SCHEDULE = "pref_key_schedule";
    public static final String KEY_PREF_SCOREBOOK = "pref_key_scorebook";
    public static final String KEY_PREF_BACKGROUND = "pref_background";
    private static final String TAG = "SettingsActivity";
    private Bundle bundle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (bundle == null) {
            bundle = new Bundle();
        }
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_PREF_SCHEDULE)) {
            //@TODO 購読状態の設定の変更を Bundleに登録し、且つこの場でFirebaseMessagingに反映している
            //      BundleはMainActivityに通知されるが何もされない、後者側の処理だけが有効になっている
            //      いずれはMainActivity側で処理するように変更したい
            bundle.putBoolean(KEY_PREF_SCHEDULE, sharedPreferences.getBoolean(KEY_PREF_SCHEDULE, true));
            if (sharedPreferences.getBoolean(KEY_PREF_SCHEDULE, true)) {
                FirebaseMessaging.getInstance().subscribeToTopic(MainActivity.NOTIFICATION_TOPIC_SCHEDULE);
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(MainActivity.NOTIFICATION_TOPIC_SCHEDULE);
            }
        } else if (key.equals(KEY_PREF_SCOREBOOK)) {
            bundle.putBoolean(KEY_PREF_SCOREBOOK, sharedPreferences.getBoolean(KEY_PREF_SCOREBOOK, true));
            if (sharedPreferences.getBoolean(KEY_PREF_SCOREBOOK, true)) {
                FirebaseMessaging.getInstance().subscribeToTopic(MainActivity.NOTIFICATION_TOPIC_SCOREBOOK);
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(MainActivity.NOTIFICATION_TOPIC_SCOREBOOK);
            }
        } else if (key.equals(KEY_PREF_BACKGROUND)) {
            bundle.putString(KEY_PREF_BACKGROUND, sharedPreferences.getString(KEY_PREF_BACKGROUND, null));
        } else {
            Log.d(TAG, "onSharedPreferenceChanged: " + key);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    // 本設定で設定した値(*)をMainActivityに通知するために、更新した項目をBundleで管理して
    // finish()内のsetResultで通知している
    // だが、一旦画面が回転するなどした場合、本Activityは一度破棄されるので本インスタンスのインスタンス変数 bundleは
    // 初期化されるので、回転後にこのActivityを(Backキーで)閉じると、空のBundleが通知されるため正しい動作にならない
    // このため、onSaveInstanceState() 〜 onRestoreInstanceState()を通じてインスタンス変数 bundleの状態を
    // 保存し維持する

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putAll(bundle);
    }
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        bundle = state;
    }

    // finish()をオーバーライドし、Bundleに登録している更新内容を通知する
    // 当初 onPause()の中で setResult()を行っていたが、呼び元のMainActivityのonActivityResult()では
    // resultCodeがRESULT_CANCELEDになり、intentがnullになった
    // (参考）https://stackoverflow.com/questions/27052080/startactivityforresult-cant-work-well-when-setresult-is-placed-in-onpause-on
    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        super.finish();
    }
}
