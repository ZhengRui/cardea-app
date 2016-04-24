package com.example.jiayu.app_design2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.clans.fab.FloatingActionButton;
import com.kyleduo.switchbutton.SwitchButton;
import com.rzheng.fdlib.FaceDetector;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

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

    private FloatingActionButton mFabBtnTake;
    private FloatingActionButton mFabBtnSettings;
    private Button mBtnShow;
    private SwitchButton camSwitchBtn;
    private SwitchButton modeSwitchBtn;

    public static FaceDetector fdetector;
    public static int batchSize = 10;
    public static CaffeMobile caffeFace;
    private static CaffeMobile caffeScene;

    private Queue<Integer> tskQueue = new LinkedList<Integer>();;
    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private int orientCase;

    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ContextPrivacy/";
    private String landmarksFilePath = DATA_PATH + "shape_predictor_68_face_landmarks.dat";
    private String faceProtoPath = DATA_PATH + "LightenedCNN_B_deploy.prototxt";
    private String faceModelPath = DATA_PATH + "LightenedCNN_B.caffemodel";
    private String sceneProtoPath = DATA_PATH + "deploy-concat.prototxt";
    private String sceneModelPath = DATA_PATH + "concat_0.599.caffemodel";

    private OrientationEventListener mOrientationListener;
    private SweetAlertDialog loadingDialog;

    private String mAddress;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    protected Location mLastLocation;
    private float mDistance;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    static {
        System.loadLibrary("facedet");
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            front1back0 = savedInstanceState.getInt(KEY_CAMERA, 0);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        mFabBtnTake = (FloatingActionButton) findViewById(R.id.fab_button_take_photo);
        mFabBtnTake.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    mCamera.takePicture(null, null, null, mJpegCallback);
                    Toast.makeText(MainActivity.this, "Photo Taken", Toast.LENGTH_SHORT).show();
                }
            }
        });


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
        camSwitchBtn.setChecked(front1back0 == 0 ? true : false);
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

        /**
        // load model using async task
        if (fdetector == null || caffeFace == null || caffeScene == null) {

            if (fdetector == null)
                Log.i(TAG, "damn1");
            if (caffeFace == null)
                Log.i(TAG, "damn2");
            if (caffeScene == null)
                Log.i(TAG, "damn3");

            new initializeTask().execute();
            loadingDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
            loadingDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            loadingDialog.setTitleText("Initializing");
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }
         */

        buildGoogleApiClient();
    }


    /** Builds a GoogleApiClient to request the LocationServices API. */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(mConnectionCallback)
                .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                                + connectionResult.getErrorCode());
                    }
                })
                .addApi(LocationServices.API)
                .build();
    }

    private ConnectionCallbacks mConnectionCallback = new ConnectionCallbacks() {

        @Override
        public void onConnected(Bundle bundle) {
            Log.i(TAG, "Connected to GoogleApiClient");

            try {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation != null) {
                    Log.i(TAG, "successfully get last location...");
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.i(TAG, "Connection suspended");
            mGoogleApiClient.connect();
        }
    };


    /** called when take button is pressed, and triggers a new socket asyncTask */
    private Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, Integer.toString(data.length));

            new socketCreationTask("10.89.159.44", 12345).execute(data); //10.89.28.149

            mAddress = getPrefAddress();
            new locationTask().execute(mAddress);
        }
    };

    private class socketCreationTask extends AsyncTask<byte[], Void, Void> {
        String desAddress;
        int dstPort;

        socketCreationTask(String addr, int port) {
            this.desAddress = addr;
            this.dstPort = port;
        }

        @Override
        protected Void doInBackground(byte[]... data) {
            try {
                mSocket = new Socket(desAddress, dstPort);
                Log.i(TAG, "Socket established");
                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();
                for (int i = 1; i <= 1; i++) tskQueue.add(i);

                sendFrm(data[0]);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void sendFrm(byte[] frmdata) {
        if (mOutputStream != null ) {   // oStream maybe set to null by previous failed asynctask
            Log.i(TAG, "mOutputStream is not null, and sendFrm() is running...");
            try {
                // allocate 4 byte for packetContent
                // be careful of big_endian(python side) and little endian(c++ server side)
                int dataSize = frmdata.length;
                byte[] frmSize = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(dataSize).array();
                byte[] packetContent = new byte[4 + dataSize];
                System.arraycopy(frmSize, 0, packetContent, 0, 4);
                System.arraycopy(frmdata, 0, packetContent, 4, dataSize);

                long startTime = SystemClock.uptimeMillis();

                Log.i(TAG, "start sending...");
                mOutputStream.write(packetContent);
                Log.i(TAG, "finish sending...");

                //byte[] buffer = new byte[10];
                //int read = mInputStream.read(buffer);
                long endTime = SystemClock.uptimeMillis();
                Log.i(TAG, String.format("time of sending the frame: %d ms", endTime - startTime));


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
                //Log.i(TAG, "Asynctask - " + tskId + " failed.");
            }
        } else {
            //Log.i(TAG, "Asynctask - " + tskId + " skipped.");
        }

    }

    private class locationTask extends AsyncTask<String, Void, Location> {

        @Override
        protected Location doInBackground(String... address) {
            HttpURLConnection conn = null;
            String data = "address=" + URLEncoder.encode(address[0]) + "&sensor=false";
            String url  = "http://maps.google.com/maps/api/geocode/json?" + data;
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
                    Log.i(TAG, "http URL connection failed..." );
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

            try {
                JSONObject jsonObject = new JSONObject(result.toString());
                JSONObject location = null;
                location = jsonObject.getJSONArray("results").getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location");

                double latitude = location.getDouble("lat");
                double longitude = location.getDouble("lng");

                Location loc = new Location("");
                loc.setLatitude(latitude);
                loc.setLongitude(longitude);
                Log.d(TAG, "latitude is " + latitude + ", longitude is " + longitude);

                return loc;

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Location loc) {
            if (mLastLocation != null) {
                mDistance = loc.distanceTo(mLastLocation);
                Log.i(TAG, "Distance between HKUST and currentLocation is: " + String.valueOf(mDistance));
            }
        }
    }


    /** bind th camera with surface view. Initialize preview and start preview. */
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
                params.setJpegQuality(100);
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


    /** Load files and prepare for caffe tasks */
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
            caffeFace = new CaffeMobile(faceProtoPath, faceModelPath);
            caffeFace.setNumThreads(2);
        }

        if (caffeScene == null) {
            caffeScene = new CaffeMobile(sceneProtoPath, sceneModelPath);
            caffeScene.setNumThreads(2);
        }
    }


    /** Get address from preference */
    private String getPrefAddress() {
        String address = "HKUST";

        return address;
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

    /** return string from inputstream @throws IOException */
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

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        Log.i(TAG, " onResume() called.");
        super.onResume();
        mCamera = Camera.open(front1back0);   // 0 for back, 1 for frontal
        mCamera.setDisplayOrientation(90);
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
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, " onDestroy() called.");
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_CAMERA, front1back0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            Log.i(TAG, "start REQUEST_SETTINGS activity");

        } else if (requestCode == REQUEST_SHOW_SETTINGS && resultCode == RESULT_OK) {
            Log.i(TAG, "start REQUEST_SHOW_SETTINGS activity");

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}

