package com.example.jiayu.app_design2;


import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsSimplifiedActivity extends PreferenceActivity {
    private static final String TAG = "SettingActivity";
    private static final int REQUEST_UPLOAD_MY = 100;
    private static final int REQUEST_UPLOAD_HIS_CAPTURE = 200;
    private static final int REQUEST_UPLOAD_HIS_SELECT = 300;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    private Button mOk;
    private String imgPath;

    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

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
                new socketCreationTask("10.89.28.149", 9999).execute();
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

    private class socketCreationTask extends AsyncTask<Void, Void, Void> {
        String desAddress;
        int dstPort;

        socketCreationTask(String addr, int port) {
            this.desAddress = addr;
            this.dstPort = port;
        }

        @Override
        protected Void doInBackground(Void... argms) {
            try {
                mSocket = new Socket(desAddress, dstPort);
                Log.i(TAG, "Socket established");
                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();

                sendPrefence();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void sendPrefence() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        boolean yesGestureSwitch = sp.getBoolean("yes_gesture_switch", true);
        boolean noGestureSwitch = sp.getBoolean("no_gesture_switch", true);
        String locationText = sp.getString("location_text", null);

        Set<String> scenario = sp.getStringSet("scenario_list", null);
        MultiSelectListPreference scenarioListPreference = (MultiSelectListPreference) findPreference("scenario_list");
        List<Integer> scenarios = new ArrayList<Integer>();
        for (String tmp : scenario) {
            int index = scenarioListPreference.findIndexOfValue(tmp);
            scenarios.add(index);
        }
        int[] sceneArray = new int[scenarios.size()];
        for (int i = 0; i < scenarios.size(); i++) {
            sceneArray[i] = scenarios.get(i);
        }

        String policyList = sp.getString("policy_list", null);
        ListPreference listPreference = (ListPreference) findPreference("policy_list");
        int policyInt = listPreference.findIndexOfValue(policyList);

        List<float[]> featureList = SelfieActivity.totalFaceFeatures;
        ByteArrayOutputStream tmpFeature = new ByteArrayOutputStream();
        for (float[] array : featureList) {
            try {
                tmpFeature.write(floatToByte(array));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (mOutputStream != null ) {   // oStream maybe set to null by previous failed asynctask
            try {
                // 4 bytes (size) of size for each data | size of each data | real data
                // be careful of big_endian(python side) and little endian(c++ server side)
                byte[] gesture = new byte[]{(byte)(yesGestureSwitch?1:0), (byte)(noGestureSwitch?1:0)};
                byte[] location = locationText.getBytes();
                byte[] scene = intToByte(sceneArray);
                byte[] policy = intToByte(new int[] {policyInt});
                byte[] feature = tmpFeature.toByteArray();
                byte[] dataSize = intToByte(new int[]{gesture.length, location.length, scene.length,
                                            policy.length, feature.length});

                // size for different parts of the sending packet
                int sizeOfSize = dataSize.length;
                int sizeOfData = gesture.length + location.length + scene.length + policy.length + feature.length;

                byte[] header = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(sizeOfSize).array();

                // combine multiple byte array together
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(gesture);
                outputStream.write(location);
                outputStream.write(scene);
                outputStream.write(policy);
                outputStream.write(feature);
                byte[] data = outputStream.toByteArray();

                // prepare for final sending packet
                byte[] packetContent = new byte[4 + sizeOfSize + sizeOfData];

                System.arraycopy(header, 0, packetContent, 0, 4);
                System.arraycopy(dataSize, 0, packetContent, 4, dataSize.length);
                System.arraycopy(data, 0, packetContent, 4+dataSize.length, data.length);

                Log.i(TAG, "start sending...");
                mOutputStream.write(packetContent);
                Log.i(TAG, "finish sending...");

                //byte[] buffer = new byte[10];
                //int read = mInputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                if (mSocket != null) {
                    Log.i(TAG, "Connection lost.");
                    try {
                        mOutputStream.close();
                        mSocket.close();
                        mOutputStream = null;
                        mSocket = null;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } else {
            //Log.i(TAG, "Asynctask - " + tskId + " skipped.");
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_UPLOAD_MY) {
            Log.i(TAG, "onActivityRsult() of REQUEST_UPLOAD_MY is called...");
            //SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(this);
            //SharedPreferences.Editor editor = pref.edit();
            //Set<String> values = new HashSet<String>();
            //values.add(data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
            //values.add(String.valueOf(data.getIntExtra(MediaActivity.NUM_OF_FEATURE, 0)));
            //editor.putStringSet("my_feature", values);
            //editor.commit();

            Preference preference1 = findPreference("my_feature");
            String num = "Number of features: " + String.valueOf(SelfieActivity.totalFaceFeatures.size());
            Log.i(TAG, "totoalFaceFeatures.size() = " + num);
            preference1.setSummary(num);

        } else if (requestCode == REQUEST_UPLOAD_HIS_CAPTURE) {
            //SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(this);
            //SharedPreferences.Editor editor = pref.edit();
            //editor.putString("others_photo_capture", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
            //editor.commit();

            Preference preference2 = findPreference("others_photo_capture");
            String num = "Number of features: " + String.valueOf(SelfieActivity.totalFaceFeatures.size());
            preference2.setSummary(num);

        } else if (requestCode == REQUEST_UPLOAD_HIS_SELECT) {
            SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("others_photo_select", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
            editor.commit();

            Preference preference3 = findPreference("others_photo_select");
            String num = String.valueOf(data.getIntExtra(MediaActivity.NUM_OF_FEATURE, 0));
            preference3.setSummary(num);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private byte[] intToByte(int[] input) {
        byte[] output = new byte[input.length*4];

        for(int i = 0; i < input.length; i++) {
            output[i*4] = (byte)(input[i] & 0xFF);
            output[i*4 + 1] = (byte)((input[i] & 0xFF00) >>> 8);
            output[i*4 + 2] = (byte)((input[i] & 0xFF0000) >>> 16);
            output[i*4 + 3] = (byte)((input[i] & 0xFF000000) >>> 24);
        }

        return output;
    }

    private byte[] doubleToByte(double[] input) {
        byte[] output = new byte[input.length*8];
        for (int i=0; i < input.length; i++)
            ByteBuffer.wrap(output, 8 * i, 8).order(ByteOrder.LITTLE_ENDIAN).putDouble(input[i]);
        return output;
    }

    private byte[] floatToByte(float[] input) {
        byte[] output = new byte[input.length*4];
        for (int i=0; i < input.length; i++)
            ByteBuffer.wrap(output, 4 * i, 4).order(ByteOrder.LITTLE_ENDIAN).putDouble(input[i]);
        return output;
    }

}
