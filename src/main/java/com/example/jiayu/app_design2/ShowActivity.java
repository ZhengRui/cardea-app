package com.example.jiayu.app_design2;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import java.util.Set;

/**
 * Created by Jiayu on 19/3/16.
 */
public class ShowActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_layout);

        TextView tvYes = (TextView) findViewById(R.id.tv_yes_gesture_switch);
        TextView tvNo = (TextView) findViewById(R.id.tv_no_gesture_switch);
        TextView tvLocation = (TextView) findViewById(R.id.tv_location_text);
        TextView tvScenario = (TextView) findViewById(R.id.tv_scenario_list);
        TextView tvHisPhoto = (TextView) findViewById(R.id.tv_his_photo_uri);
        TextView tvPolicy = (TextView) findViewById(R.id.tv_policy_list);
        TextView tvMyPhoto1 = (TextView) findViewById(R.id.tv_my_photo_uri_1);
        TextView tvMyPhoto2 = (TextView) findViewById(R.id.tv_my_photo_uri_2);
        TextView tvMyPhoto3 = (TextView) findViewById(R.id.tv_my_photo_uri_3);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        boolean yesGestureSwitch = sp.getBoolean("yes_gesture_switch", true);
        boolean noGestureSwitch = sp.getBoolean("no_gesture_switch", true);
        String locationText = sp.getString("location_text", null);
        Set<String> scenario = sp.getStringSet("scenario_list", null);
        String[] scenarioList = scenario.toArray(new String[scenario.size()]);
        String hisPhoto = sp.getString("his_photo", null);
        String policyList = sp.getString("policy_list", null);
        String myPhoto1 = sp.getString("my_photo_1", null);
        String myPhoto2 = sp.getString("my_photo_2", null);
        String myPhoto3 = sp.getString("my_photo_3", null);

        tvYes.setText("Yes gesture: " + Boolean.toString(yesGestureSwitch));
        tvNo.setText("No gesture: " + Boolean.toString(noGestureSwitch));
        tvLocation.setText("Location: " + locationText);
        tvScenario.setText("Scenario: " + scenarioList);
        tvHisPhoto.setText("His/her photo uri: " + hisPhoto);
        tvPolicy.setText("Privacy policy: " + policyList);
        tvMyPhoto1.setText("My photo_1 uri: " + myPhoto1);
        tvMyPhoto2.setText("My photo_2 uri: " + myPhoto2);
        tvMyPhoto3.setText("My photo_3 uri: " + myPhoto3);

    }
}
