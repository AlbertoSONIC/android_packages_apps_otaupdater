package com.ota.updater.two;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {
    public static final String KEY_UPDATE_SETTINGS = "update_settings_pref";

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        Preference update_pref = findPreference(KEY_UPDATE_SETTINGS);
        update_pref.setOnPreferenceClickListener(new OnPreferenceClickListener () {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), UpdateSettings.class);
                startActivity(intent);
                return true;
            }
        });
    }
}
