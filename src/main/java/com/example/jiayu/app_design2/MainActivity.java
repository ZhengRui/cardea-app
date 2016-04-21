package com.example.jiayu.app_design2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.kyleduo.switchbutton.SwitchButton;
import com.rzheng.fdlib.FaceDetector;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by Jiayu on 19/3/16.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_TAKE_PHOTO = 100;
    private static final int REQUEST_SETTINGS = 200;
    private static final int REQUEST_SHOW_SETTINGS = 300;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String KEY_CAMERA = "frontOrBack";

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean mInPreview = false;
    private boolean mCameraConfigured = false;
    private Camera.Size size;
    private static int front1back0 = 0;

//    private Button mBtnTake;
    private FloatingActionButton mFabBtnTake;
//    private Button mBtnSettings;
    private FloatingActionButton mFabBtnSettings;
    private Button mBtnShow;
    private SwitchButton camSwitchBtn;
    private SwitchButton modeSwitchBtn;
    //private Uri fileUri;

    public static FaceDetector fdetector;
    public static int batchSize = 5;
    public static CaffeMobile caffeFace;
    private static CaffeMobile caffeScene;

    private int orientCase;

    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ContextPrivacy/";
    private String landmarksFilePath = DATA_PATH + "shape_predictor_68_face_landmarks.dat";
    private String faceProtoPath = DATA_PATH + "LightenedCNN_B_deploy.prototxt";
    private String faceModelPath = DATA_PATH + "LightenedCNN_B.caffemodel";
    private String sceneProtoPath = DATA_PATH + "deploy-concat.prototxt";
    private String sceneModelPath = DATA_PATH + "concat_0.599.caffemodel";

    private OrientationEventListener mOrientationListener;
    private SweetAlertDialog loadingDialog;

    static {
        System.loadLibrary("facedet");
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    private void initialize() {
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
        if (!cascadeFile.exists()) {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            try {
                FileOutputStream os = new FileOutputStream(cascadeFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                try {
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    is.close();
                    os.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (fdetector == null) {
            fdetector = new FaceDetector(cascadeFile.getAbsolutePath(), batchSize);
            fdetector.loadShapePredictor(landmarksFilePath);
        }

        if (caffeFace == null) {
            caffeFace = new CaffeMobile();
            caffeFace.setNumThreads(2);
            caffeFace.loadModel(faceProtoPath, faceModelPath);
        }

        if (caffeScene == null) {
            caffeScene = new CaffeMobile();
            caffeScene.setNumThreads(2);
            caffeScene.loadModel(sceneProtoPath, sceneModelPath);
        }
    }

    private class initializeTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            initialize();
            return true;
        }

        protected void onPostExecute(Boolean ready) {
            // take button enabled
            Log.i(TAG, "All models are loaded.");
            loadingDialog.dismissWithAnimation();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            front1back0 = savedInstanceState.getInt(KEY_CAMERA, 0);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

//        mBtnTake = (Button) findViewById(R.id.button_take_photo);
//        mBtnTake.setOnClickListener(new Button.OnClickListener() {
        mFabBtnTake = (FloatingActionButton) findViewById(R.id.fab_button_take_photo);
        mFabBtnTake.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    mCamera.takePicture(null, null, null, mJpegCallback);
                    Toast.makeText(MainActivity.this, "Photo Taken", Toast.LENGTH_SHORT).show();
                }

                // start take picture activity
                //fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                //Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                //startActivityForResult(i, REQUEST_TAKE_PHOTO);
            }
        });

//        mBtnSettings = (Button) findViewById(R.id.button_settings);
//        mBtnSettings.setOnClickListener(new Button.OnClickListener() {
        mFabBtnSettings = (FloatingActionButton) findViewById(R.id.fab_button_settings);
        mFabBtnSettings.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SettingsSimplifiedActivity.class);
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


        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(surfaceCallback);


        camSwitchBtn = (SwitchButton) findViewById(R.id.camswitch);
        camSwitchBtn.setChecked( front1back0 == 0 ? true : false);
        camSwitchBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                switchCam();
            }

        });

        modeSwitchBtn = (SwitchButton) findViewById(R.id.modeswitch);
        modeSwitchBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                switchMode();
            }

        });

        mOrientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if ((orientation >= 0 && orientation <= 30 ) || (orientation >= 330 && orientation <= 360)) {
                    orientCase = 0;
                } else if (orientation >= 60 && orientation <= 120) {
                    orientCase = 1;
                } else if (orientation >= 150 && orientation <= 210) {
                    orientCase = 2;
                } else if (orientation >= 240 && orientation <= 300) {
                    orientCase = 3;
                } else {}
//                Log.i(TAG, "Orientation changed to " + orientation +
//                        ", case " + orientCase);
            }
        };

        // load model using async task
        if (fdetector == null || caffeFace == null || caffeScene == null) {
            new initializeTask().execute();
            loadingDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
            loadingDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            loadingDialog.setTitleText("Initializing");
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            //String imgPath = fileUri.getPath();
            Log.i(TAG, "start REQUEST_TAKE_PHOTO activity");

        } else if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            Log.i(TAG, "start REQUEST_SETTINGS activity");

        } else if (requestCode == REQUEST_SHOW_SETTINGS && resultCode == RESULT_OK) {
            Log.i(TAG, "start REQUEST_SHOW_SETTINGS activity");

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "surfaceCreated() called...");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(TAG, " surfaceChanged() called.");
            initPreview(width, height);
            startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, " surfaceDestroyed() called.");
        }
    };

    private void initPreview(int width, int height) {
        Log.i(TAG, "initPreview() called");
        if (mCamera != null && mHolder.getSurface() != null) {
            if (!mCameraConfigured) {
                Camera.Parameters params = mCamera.getParameters();
                size = params.getPreviewSize();
                for (Camera.Size s : params.getSupportedPreviewSizes()) {   // get 3840x2160 for back cam
                    if (s.width > size.width)
                        size = s;
                }
                params.setPreviewSize(size.width, size.height);
                Log.i(TAG, "Preview size: " + size.width + ", " + size.height);
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                mCamera.setParameters(params);
                mCameraConfigured = true;

                if (mOrientationListener.canDetectOrientation() == true) {
                    mOrientationListener.enable();
                }
            }

            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (Throwable t) {
                Log.e(TAG, "Exception in initPreview()", t);
            }

        }
    }

    private void startPreview() {
        Log.i(TAG, "startPreview() called");
        if (mCameraConfigured && mCamera != null) {
            mCamera.startPreview();
            mInPreview = true;
        }
    }


    private Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            startPreview();
            // create a filename
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {Log.d(TAG,
                    "Error creating media file, check storage permissions: " + "e.getMessage()");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                // Create a worker thread
                //new MyAsyncTask(mProgressContainer, mProgressDialog, mHolder).execute(data);
                Log.d(TAG, "Picture processing...");
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };


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
                Log.d(TAG, "failed to create directory");
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

    @Override
    public void onResume() {
        Log.i(TAG, " onResume() called.");
        super.onResume();
        mCamera = Camera.open(front1back0);   // 0 for back, 1 for frontal
        mCamera.setDisplayOrientation(90);
//        setCameraDisplayOrientation(MainActivity.this, front1back0, mCamera);

        startPreview();
    }

    @Override
    public void onPause() {
        Log.i(TAG, " onPause() called.");
        if (mInPreview) {
            mCamera.stopPreview();
        }
        mCamera.release();
        mCamera = null;
        mInPreview = false;
        mCameraConfigured = false; // otherwise cannot refocus after onResume
        mOrientationListener.disable();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, " onDestroy() called.");
        super.onDestroy();
    }

    /* use button to switch camera
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_switchcam) {
            switchCam();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_CAMERA, front1back0);
    }

    private void switchCam() {
        if (mCamera != null && mInPreview) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mInPreview = false;
            mCameraConfigured = false;
        }

        front1back0 = 1 - front1back0;
        mCamera = Camera.open(front1back0);   // 0 for back, 1 for frontal
        mCamera.setDisplayOrientation(90);
//        setCameraDisplayOrientation(MainActivity.this, front1back0, mCamera);
        initPreview(size.width, size.height);
        startPreview();

    }

    private void switchMode() {

    }


    /* fixed mCamera.setDisplayOrientation(90)
    private void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        //  0 (natural orientation), 1 (90 degree anticlockwose), 3 (90 degree clockwise)
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Log.i(TAG, "rotation from windowManager(): " + String.valueOf(rotation));

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) { // info.orientation == 270
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing, info.orientation == 90
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
    */
}

