package com.example.jiayu.app_design2;


import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsSimplifiedActivity extends PreferenceActivity {
    private static final int REQUEST_UPLOAD_MY = 100;
    private static final int REQUEST_UPLOAD_HIS_CAPTURE = 200;
    private static final int REQUEST_UPLOAD_HIS_SELECT = 300;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    private Button mOk;
    private String imgPath;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof MultiSelectListPreference) {
                MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) preference;
                Set<String> values = (Set<String>) value;
                if (values != null) {
                    String result = "";
                    for (String tmp : values) {
                        int index = multiSelectListPreference.findIndexOfValue(tmp);
                        result += multiSelectListPreference.getEntries()[index] + ", ";
                    }
                    preference.setSummary(result);
                }
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof MultiSelectListPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getStringSet(preference.getKey(), null));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_simplified);
        setContentView(R.layout.settings_layout);

        mOk = (Button)findViewById(R.id.button_ok);
        mOk.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v){

            }
        });


        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        bindPreferenceSummaryToValue(findPreference("location_text"));
        bindPreferenceSummaryToValue(findPreference("scenario_list"));
        bindPreferenceSummaryToValue(findPreference("policy_list"));

        Preference myPreference = (Preference) findPreference("my_feature");
        myPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //ComponentName comp = new ComponentName("com.example.jiayu.app_design2",
                //       "com.example.jiayu.app_design2.MediaActivity");
                //Intent i = new Intent();
                //i.setComponent(comp);
                Intent i = new Intent(SettingsSimplifiedActivity.this, SelfieActivity.class);
                startActivityForResult(i, REQUEST_UPLOAD_MY);

                return true;
            }
        });

        Preference preferenceCapture = findPreference("others_photo_capture");
        preferenceCapture.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(SettingsSimplifiedActivity.this, SelfieActivity.class);
                startActivityForResult(i, REQUEST_UPLOAD_HIS_CAPTURE);

                return true;
            }
        });

        Preference preferenceSelect = findPreference("others_photo_select");
        preferenceSelect.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(SettingsSimplifiedActivity.this, MediaActivity.class);
                startActivityForResult(i, REQUEST_UPLOAD_HIS_SELECT);

                return true;
            }
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_UPLOAD_MY && resultCode == RESULT_OK) {
            //imgPath = data.getStringExtra(MediaActivity.EXTRA_FILE_URI);
            //Log.i("SettingsActivity", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
            SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = pref.edit();
            Set<String> values = new HashSet<String>();
            values.add(data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
            values.add(String.valueOf(data.getIntExtra(MediaActivity.NUM_OF_FEATURE, 0)));
            editor.putStringSet("my_feature", values);
            editor.commit();

            Preference preference1 = findPreference("my_feature");
            String num = String.valueOf(data.getIntExtra(MediaActivity.NUM_OF_FEATURE, 0));
            preference1.setSummary(num);

        } else if (requestCode == REQUEST_UPLOAD_HIS_CAPTURE && resultCode == RESULT_OK) {
            SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("others_photo_capture", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
            editor.commit();

        } else if (requestCode == REQUEST_UPLOAD_HIS_SELECT) {
            SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("others_photo_select", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
            editor.commit();

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(SettingsSimplifiedActivity.this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
