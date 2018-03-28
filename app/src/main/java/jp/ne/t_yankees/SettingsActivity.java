package jp.ne.t_yankees;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
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
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_PREF_SCHEDULE)) {
            if (sharedPreferences.getBoolean(KEY_PREF_SCHEDULE, true)) {
                FirebaseMessaging.getInstance().subscribeToTopic(MainActivity.NOTIFICATION_TOPIC_SCHEDULE);
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(MainActivity.NOTIFICATION_TOPIC_SCHEDULE);
            }
        } else if (key.equals(KEY_PREF_SCOREBOOK)) {
            if (sharedPreferences.getBoolean(KEY_PREF_SCOREBOOK, true)) {
                FirebaseMessaging.getInstance().subscribeToTopic(MainActivity.NOTIFICATION_TOPIC_SCOREBOOK);
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(MainActivity.NOTIFICATION_TOPIC_SCOREBOOK);
            }
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
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
