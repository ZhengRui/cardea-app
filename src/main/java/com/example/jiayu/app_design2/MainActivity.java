package com.example.jiayu.app_design2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Jiayu on 19/3/16.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_TAKE_PHOTO = 100;
    private static final int REQUEST_SETTINGS = 200;
    private static final int REQUEST_SHOW_SETTINGS = 300;
    public static final int MEDIA_TYPE_IMAGE = 1;

    private Button mBtnTake;
    private Button mBtnSettings;
    private Button mBtnShow;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        mBtnTake = (Button) findViewById(R.id.button_take_photo);
        mBtnTake.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(i, REQUEST_TAKE_PHOTO);
            }
        });

        mBtnSettings = (Button) findViewById(R.id.button_settings);
        mBtnSettings.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(i, REQUEST_SETTINGS);
            }
        });

        mBtnShow = (Button) findViewById(R.id.button_show_settings);
        mBtnShow.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ShowActivity.class);
                startActivityForResult(i, REQUEST_SHOW_SETTINGS);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            String imgPath = fileUri.getPath();
            Log.i(TAG, "start REQUEST_TAKE_PHOTO activity");

        } else if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            Log.i(TAG, "start REQUEST_SETTINGS activity");

        } else if (requestCode == REQUEST_SHOW_SETTINGS && resultCode == RESULT_OK) {
            Log.i(TAG, "start REQUEST_SHOW_SETTINGS activity");

        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "app-design2");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }
}
