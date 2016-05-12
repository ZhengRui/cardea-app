package com.example.jiayu.app_design2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingActivity extends PreferenceActivity implements AsyncTaskListener{
    private static final String TAG = "SettingActivity";
    public static final int REQUEST_UPLOAD_MY = 100;
    public static final int REQUEST_UPLOAD_HIS = 200;

    private static String myFeatureFileName = "myFeatures.txt";
    private static String hisFeatureFileName = "hisFeatures.txt";

    private Button mOk;

    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private byte[] myFeature;
    private byte[] hisFeature;

    private int msgtype = 1;

    private static String mUser;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
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
                        result += ", " + multiSelectListPreference.getEntries()[index];
                    }
                    preference.setSummary(result.length() > 0 ? result.substring(2) : "");
                }
            } else if (preference.getKey().equals("username_text")){
                preference.setSummary(stringValue);
                mUser = stringValue;
                updateFileName(mUser);

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
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof MultiSelectListPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getStringSet(preference.getKey(), null));
        } else if (preference.getKey().equals("username_text")) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
            mUser = PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), null);
            updateFileName(mUser);

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
                new socketCreationTask("10.89.28.149", 9999, SettingActivity.this).execute();

            }
        });

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        bindPreferenceSummaryToValue(findPreference("username_text"));
        bindPreferenceSummaryToValue(findPreference("location_text"));
        bindPreferenceSummaryToValue(findPreference("scenario_list"));
        bindPreferenceSummaryToValue(findPreference("policy_list"));

        // save sent features to SD card
        updateFeatureSummary(myFeatureFileName);
        updateFeatureSummary(hisFeatureFileName);

        Preference myPreference = findPreference("my_feature");
        myPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(SettingActivity.this, SelfieActivity.class);
                i.putExtra(SelfieActivity.EXTRA_WHOSE_FEATURE, REQUEST_UPLOAD_MY);
                startActivityForResult(i, REQUEST_UPLOAD_MY);

                return true;
            }
        });

        Preference preferenceCapture = findPreference("his_feature");
        preferenceCapture.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(SettingActivity.this, SelfieActivity.class);
                i.putExtra(SelfieActivity.EXTRA_WHOSE_FEATURE, REQUEST_UPLOAD_HIS);
                startActivityForResult(i, REQUEST_UPLOAD_HIS);

                return true;
            }
        });


    }

    private class socketCreationTask extends AsyncTask<Void, Void, Boolean> {
        String desAddress;
        int dstPort;
        private AsyncTaskListener listener;

        socketCreationTask(String addr, int port, AsyncTaskListener listener) {
            this.desAddress = addr;
            this.dstPort = port;
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            try {
                mSocket = new Socket(desAddress, dstPort);
                Log.i(TAG, "Socket established");
                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();

                sendPreference();

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            listener.onTaskCompleted(success);
        }
    }

    private void sendPreference() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String usernameText = sp.getString("username_text", null);
        boolean yesGestureSwitch = sp.getBoolean("yes_gesture_switch", true);
        boolean noGestureSwitch = sp.getBoolean("no_gesture_switch", true);
        String locationText = sp.getString("location_text", null);
        Location loc = getLocation(locationText);

        Set<String> scenario = sp.getStringSet("scenario_list", null);
        List<Integer> scenarios = new ArrayList<Integer>();
        if (scenario.size() == 0) {//value = none, return 12
            scenarios.add(-1);
        } else if (scenario.size() == 10) { //all are selected, return 0
            scenarios.add(0);
        } else {
            MultiSelectListPreference scenarioListPreference = (MultiSelectListPreference) findPreference("scenario_list");
            for (String tmp : scenario) {
                int index = scenarioListPreference.findIndexOfValue(tmp);
                scenarios.add(index);
            }
        }
        int[] sceneArray = new int[scenarios.size()];
        for (int i = 0; i < scenarios.size(); i++) {
            sceneArray[i] = scenarios.get(i);
        }

        String policyList = sp.getString("policy_list", null);
        ListPreference listPreference = (ListPreference) findPreference("policy_list");
        int policyInt = listPreference.findIndexOfValue(policyList);

        List<float[]> myFeatureList = SelfieActivity.myTotalFaceFeatures;
        ByteArrayOutputStream myTmpFeature = new ByteArrayOutputStream();
        for (float[] array : myFeatureList) {
            try {
                myTmpFeature.write(floatToByte(array));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        List<float[]> hisFeatureList = SelfieActivity.hisTotalFaceFeatures;
        ByteArrayOutputStream hisTmpFeature = new ByteArrayOutputStream();
        for (float[] array : hisFeatureList) {
            try {
                hisTmpFeature.write(floatToByte(array));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (mOutputStream != null ) {   // oStream maybe set to null by previous failed asynctask
            try {
                // (24 bytes) size of each data | real data
                // be careful of big_endian(python side) and little endian(c++ server side)
                byte[] username = usernameText.getBytes();
                byte[] gesture = new byte[]{(byte)(yesGestureSwitch?1:0), (byte)(noGestureSwitch?1:0)};
                byte[] location = doubleToByte(new double[]{loc.getLatitude(), loc.getLongitude()});
                byte[] scene = intToByte(sceneArray);
                byte[] policy = intToByte(new int[]{policyInt});
                myFeature = myTmpFeature.toByteArray();
                hisFeature = hisTmpFeature.toByteArray();
                byte[] header = intToByte(new int[]{msgtype, username.length, gesture.length, location.length,
                        scene.length, policy.length, myFeature.length, hisFeature.length});

                // size for different parts of the sending packet
                int sizeOfData = username.length + gesture.length + location.length + scene.length + policy.length
                        + myFeature.length + hisFeature.length;

                // combine multiple byte array together
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(username);
                outputStream.write(gesture);
                outputStream.write(location);
                outputStream.write(scene);
                outputStream.write(policy);
                outputStream.write(myFeature);
                outputStream.write(hisFeature);
                byte[] data = outputStream.toByteArray();

                // prepare for final sending packet
                byte[] packetContent = new byte[header.length + sizeOfData];

                System.arraycopy(header, 0, packetContent, 0, header.length);
                System.arraycopy(data, 0, packetContent, header.length, data.length);

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
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_UPLOAD_MY) {
            Log.i(TAG, "onActivityRsult() of REQUEST_UPLOAD_MY is called...");

            updateFeatureSummary(myFeatureFileName);

        } else if (requestCode == REQUEST_UPLOAD_HIS) {
            //SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(this);
            //SharedPreferences.Editor editor = pref.edit();
            //editor.putString("others_photo_capture", data.getStringExtra(MediaActivity.EXTRA_FILE_URI));
            //editor.commit();

            updateFeatureSummary(hisFeatureFileName);
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
            ByteBuffer.wrap(output, 4 * i, 4).order(ByteOrder.LITTLE_ENDIAN).putFloat(input[i]);
        return output;
    }

    private Location getLocation(String address) {
        HttpURLConnection conn = null;
        String data = "address=" + URLEncoder.encode(address) + "&sensor=false";
        String url = "http://maps.google.com/maps/api/geocode/json?" + data;
        String result = null;

        try {
            URL mURL = new URL(url);
            conn = (HttpURLConnection) mURL.openConnection();

            conn.setRequestMethod("GET");
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream is = conn.getInputStream();
                result = getStringFromInputStream(is);

                //return result;
            } else {
                Log.i(TAG, "http URL connection failed...");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        Location loc = null;
        try {
            JSONObject jsonObject = new JSONObject(result.toString());
            JSONObject location = null;
            location = jsonObject.getJSONArray("results").getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location");

            double latitude = location.getDouble("lat");
            double longitude = location.getDouble("lng");

            loc = new Location("");
            loc.setLatitude(latitude);
            loc.setLongitude(longitude);
            Log.d(TAG, "latitude is " + latitude + ", longitude is " + longitude);

            return loc;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return loc;
    }

    private String getStringFromInputStream(InputStream is) throws IOException{
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len = -1;

        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        is.close();
        String result = os.toString();
        os.close();

        return result;

    }

    private void saveFeatureToSD(String fileName, byte[] features) throws IOException {
        File contextPrivacyDir = new File(Environment.getExternalStorageDirectory(), "ContextPrivacy");
        File featureFile = new File(contextPrivacyDir.getPath() + File.separator + fileName);

        FileOutputStream fileOutputStream = new FileOutputStream(featureFile, true);
        fileOutputStream.write(features);

        if(fileOutputStream != null) {            //关闭FileOutputStream对象
            fileOutputStream.close();

            if (fileName == myFeatureFileName) {
                SelfieActivity.myTotalFaceFeatures.clear();
                updateFeatureSummary(myFeatureFileName);

            } else if(fileName == hisFeatureFileName) {
                SelfieActivity.hisTotalFaceFeatures.clear();
                updateFeatureSummary(hisFeatureFileName);
            }

        }
    }

    private int getSizeFromSD(String fileName) throws IOException {
        File contextPrivacyDir = new File(Environment.getExternalStorageDirectory(), "ContextPrivacy");
        File featureFile = new File(contextPrivacyDir.getPath() + File.separator + fileName);

        long length = featureFile.length();
        int size = ((int)length)/256/4;

        return size;
    }

    private void updateFeatureSummary(String fileName) {
        if (fileName == myFeatureFileName) {
            Preference preference = findPreference("my_feature");
            String num = null;
            try {
                num = "Number of features saved: " + String.valueOf(getSizeFromSD(myFeatureFileName))
                        + "; Number of features added: " + String.valueOf(SelfieActivity.myTotalFaceFeatures.size());
            } catch (IOException e) {
                e.printStackTrace();
            }
            preference.setSummary(num);
        } else if (fileName == hisFeatureFileName) {
            Preference preference = findPreference("his_feature");
            String num = null;
            try {
                num = "Number of features saved: " + String.valueOf(getSizeFromSD(hisFeatureFileName))
                        + "; Number of features added: " + String.valueOf(SelfieActivity.hisTotalFaceFeatures.size());
            } catch (IOException e) {
                e.printStackTrace();
            }
            preference.setSummary(num);
        }
    }

    private void updateFileName(String username) {
        myFeatureFileName = "myFeature_" + username + ".txt";
        hisFeatureFileName = "hisFeature_" + username + ".txt";

        updateFeatureSummary(myFeatureFileName);
        updateFeatureSummary(hisFeatureFileName);
    }

    @Override
    public void onTaskCompleted(boolean result) {
        if (result) {// save sent features to SD card
            try {
                saveFeatureToSD(myFeatureFileName, myFeature);
                saveFeatureToSD(hisFeatureFileName, hisFeature);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
