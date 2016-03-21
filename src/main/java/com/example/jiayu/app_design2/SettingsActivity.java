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


import java.util.List;
import java.util.Set;

public class SettingsActivity extends PreferenceActivity {
    private static final int REQUEST_UPLOAD_HIS = 100;
    private static final int REQUEST_UPLOAD_MY_1 = 200;
    private static final int REQUEST_UPLOAD_MY_2 = 300;
    private static final int REQUEST_UPLOAD_MY_3 = 400;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

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
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || PreferencePreferenceFragment.class.getName().equals(fragmentName)
                || ProfilePreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PreferencePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_preference);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("location_text"));
            bindPreferenceSummaryToValue(findPreference("scenario_list"));
            bindPreferenceSummaryToValue(findPreference("policy_list"));

            Preference preference = (Preference) findPreference("his_photo");
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //ComponentName comp = new ComponentName("com.example.jiayu.app_design2",
                     //       "com.example.jiayu.app_design2.MediaActivity");
                    //Intent i = new Intent();
                    //i.setComponent(comp);
                    Intent i = new Intent(getActivity(), MediaActivity.class);
                    startActivityForResult(i, REQUEST_UPLOAD_HIS);

                    return true;
                }
            });

        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_UPLOAD_HIS && resultCode == RESULT_OK) {
                //imgPath = data.getStringExtra(MediaActivity.EXTRA_FILE_URI);
                //Log.i("SettingsActivity", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
                SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("his_photo", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
                editor.commit();

            }

            super.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ProfilePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_profile);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

            Preference preference1 = findPreference("my_photo_1");
            preference1.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getActivity(), MediaActivity.class);
                    startActivityForResult(i, REQUEST_UPLOAD_MY_1);

                    return true;
                }
            });

            Preference preference2 = findPreference("my_photo_2");
            preference2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getActivity(), MediaActivity.class);
                    startActivityForResult(i, REQUEST_UPLOAD_MY_2);

                    return true;
                }
            });

            Preference preference3 = findPreference("my_photo_3");
            preference3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getActivity(), MediaActivity.class);
                    startActivityForResult(i, REQUEST_UPLOAD_MY_3);

                    return true;
                }
            });

        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_UPLOAD_MY_1 && resultCode == RESULT_OK) {
                SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("my_photo_1", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
                editor.commit();

            } else if (requestCode == REQUEST_UPLOAD_MY_2) {
                SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("my_photo_2", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
                editor.commit();

            } else if (requestCode == REQUEST_UPLOAD_MY_3) {
                SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("my_photo_3", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
                editor.commit();

            }

            super.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


}
