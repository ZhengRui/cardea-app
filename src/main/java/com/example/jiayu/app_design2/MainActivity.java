package com.example.jiayu.app_design2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;


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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends Activity implements AsyncTaskListener {
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
    private Camera.Size imgSize;
    private static int front1back0 = 0;

    private FloatingActionButton mFabBtnTake;
    private FloatingActionButton mFabBtnSettings;
    private SwitchButton camSwitchBtn;
    private SwitchButton modeSwitchBtn;
    private ImageView mImageView;
    private FloatingActionButton mFabBtnYes;
    private FloatingActionButton mFabBtnNo;

    public static FaceDetector fdetector;
    public static int batchSize = 10;
    public static CaffeMobile caffeFace;
    private static CaffeMobile caffeScene;

    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private byte[] mResultFrm;

    private int orientCase;
    private int msgtype = 0;
    private static int mode = 0; // 0 weak mode 1 strong mode

    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ContextPrivacy/";
    private String landmarksFilePath = DATA_PATH + "shape_predictor_68_face_landmarks.dat";
    private String faceProtoPath = DATA_PATH + "LightenedCNN_B_deploy.prototxt";
    private String faceModelPath = DATA_PATH + "LightenedCNN_B.caffemodel";
    private String sceneProtoPath = DATA_PATH + "deploy-concat.prototxt";
    private String sceneModelPath = DATA_PATH + "concat_0.599.caffemodel";

    private OrientationEventListener mOrientationListener;
    private SweetAlertDialog loadingDialog;

    private GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    private double latitude, longitude;
    private int xmax, ymax;

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
                Intent i = new Intent(MainActivity.this, SettingActivity.class);
                startActivityForResult(i, REQUEST_SETTINGS);
            }
        });


        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
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
        modeSwitchBtn.setChecked(mode == 0 ? true : false);
        modeSwitchBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                switchMode();
            }

        });

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setVisibility(View.INVISIBLE);

        mFabBtnYes = (FloatingActionButton) findViewById(R.id.fab_button_yes);
        mFabBtnYes.setVisibility(View.GONE);
        mFabBtnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImageView.setVisibility(View.INVISIBLE);
                mFabBtnYes.setVisibility(View.GONE);
                mFabBtnNo.setVisibility(View.GONE);

                startPreview();
            }
        });

        mFabBtnNo = (FloatingActionButton) findViewById(R.id.fab_button_no);
        mFabBtnNo.setVisibility(View.GONE);
        mFabBtnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImageView.setVisibility(View.INVISIBLE);
                mFabBtnYes.setVisibility(View.GONE);
                mFabBtnNo.setVisibility(View.GONE);

                startPreview();
            }
        });

        mOrientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if ((orientation >= 0 && orientation <= 30) || (orientation >= 330 && orientation <= 360)) {
                    orientCase = 0;
                } else if (orientation >= 60 && orientation <= 120) {
                    orientCase = 1;
                } else if (orientation >= 150 && orientation <= 210) {
                    orientCase = 2;
                } else if (orientation >= 240 && orientation <= 300) {
                    orientCase = 3;
                } else {
                }
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

        buildGoogleApiClient();
    }


    /**
     * Builds a GoogleApiClient to request the LocationServices API.
     */
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
                    latitude = mLastLocation.getLatitude();
                    longitude = mLastLocation.getLongitude();
                    Log.i(TAG, "Latitude: " + String.valueOf(latitude) + ", longitude: " + String.valueOf(longitude));
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


    /**
     * called when take button is pressed, and triggers a new socket asyncTask
     */
    private Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, Integer.toString(data.length));

            new socketCreationTask("10.89.28.149", 9999, MainActivity.this).execute(data); //10.89.28.149

        }
    };

    private class socketCreationTask extends AsyncTask<byte[], Void, Boolean> {
        String desAddress;
        int dstPort;
        AsyncTaskListener listener;

        socketCreationTask(String addr, int port, AsyncTaskListener listener) {
            this.desAddress = addr;
            this.dstPort = port;
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(byte[]... data) {
            try {

                xmax = (orientCase == 0 || orientCase == 2) ? imgSize.height : imgSize.width;
                ymax = (orientCase == 0 || orientCase == 2) ? imgSize.width : imgSize.height;

                mResultFrm = fdetector.droidJPEGCalibrate(data[0], front1back0, orientCase);
                byte[] jpeg2sent = mResultFrm;

                float[][] feats; // m x 256
                int[] faceposs; // length: m x 4
                int extraSize = 0;
                byte[] facebbxs = null;
                byte[] facefeats = null;
                if (mode == 1) {
                    fdetector.clearCache();
                    jpeg2sent = fdetector.detectAndBlurJPEG(jpeg2sent);
                    faceposs = fdetector.getBbxPositions();
                    feats = caffeFace.extractFeaturesCVBatch(fdetector.getAlignedFacesAddr(), "eltwise_fc1");
                    Log.i(TAG, "faces positions: " + Arrays.toString(faceposs) + ", faces number: " + feats.length);
                    facebbxs = intToByte(faceposs);
                    ByteArrayOutputStream facefeatsStream = new ByteArrayOutputStream();
                    for (float[] array : feats) {
                        try {
                            facefeatsStream.write(floatToByte(array));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    facefeats = facefeatsStream.toByteArray();
                    extraSize = facebbxs.length + facefeats.length;
                }

                // header: 8 integers 2 doubles，6th integer is frame size, 8th integer is face positions and features total size
                // be careful of big_endian(python side) and little endian(c++ server side)
                int dataSize = jpeg2sent.length;
                byte[] headerMisc = intToByte(new int[] {msgtype, front1back0, orientCase, imgSize.width, imgSize.height, dataSize, mode, extraSize});
                byte[] headerGeo = doubleToByte(new double[] {latitude, longitude});

                int headerSize = headerMisc.length + headerGeo.length;
                byte[] packetContent = new byte[headerSize + dataSize + extraSize];

                System.arraycopy(headerMisc, 0, packetContent, 0, headerMisc.length);
                System.arraycopy(headerGeo, 0, packetContent, headerMisc.length, headerGeo.length);
                System.arraycopy(jpeg2sent, 0, packetContent, headerSize, dataSize);

                if (mode == 1) {
                    System.arraycopy(facebbxs, 0, packetContent, headerSize+dataSize, facebbxs.length);
                    System.arraycopy(facefeats, 0, packetContent, headerSize+dataSize+facebbxs.length, facefeats.length);
                }

                Log.i(TAG, "lat, lon : " + latitude + ", " + longitude);

                Log.i(TAG, "header length: " + headerSize + " data length: " + dataSize +
                        " extra length: " + extraSize + " msg length: " + packetContent.length);

                mSocket = new Socket(desAddress, dstPort);
                Log.i(TAG, "Socket established");
                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();

                sendFrm(packetContent);

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

    private void sendFrm(byte[] packetContent) {
        if (mOutputStream != null) {   // oStream maybe set to null by previous failed asynctask
            Log.i(TAG, "mOutputStream is not null, and sendFrm() is running...");
            try {

                long startTime = SystemClock.uptimeMillis();
                Log.i(TAG, "start sending...");
                mOutputStream.write(packetContent);
                mOutputStream.flush();
                Log.i(TAG, "finish sending...");

                long endTime = SystemClock.uptimeMillis();
                Log.i(TAG, String.format("time of sending the frame: %d ms", endTime - startTime));

                byte[] headerRES = new byte[8];
                int resHeaderSize = mInputStream.read(headerRES);
                assert(resHeaderSize == 8);
                int[] resSizes = byteToInt(headerRES);
                Log.i(TAG, Arrays.toString(resSizes));
                int resSizeTot = resSizes[0] + resSizes[1];
                byte[] dataRES = new byte[resSizeTot];
                int resDataSize = mInputStream.read(dataRES);
                assert(resDataSize == resSizeTot);
                Log.i(TAG, "total result data length: " + dataRES.length);

                int bbxnum = resSizes[0] / 60 + resSizes[1] / 24;
                int[][] bbxposArr = new int[bbxnum][4];
                String[] bbxtxtArr = new String[bbxnum];
                boolean[] bbxprocArr = new boolean[bbxnum];
                int[] bbxproctypeArr = new int[bbxnum];

                int ibbx = 0;

                // parse face result
                for (int i = 0; i < resSizes[0]; i += 60) {
                    int[] bbxpos = byteToInt(Arrays.copyOfRange(dataRES, i, i+16));
                    String username = new String(Arrays.copyOfRange(dataRES, i+16, i+44)).trim();
                    String facecase = new String(Arrays.copyOfRange(dataRES, i+44, i+46));
                    int policy = byteToInt(Arrays.copyOfRange(dataRES, i+56, i+60))[0];
                    Log.i(TAG, "face bbx: " + Arrays.toString(bbxpos) + ", username: " + username +
                            ", facecase: " + facecase + ", policy: " + policy);

                    if (facecase.equals("c5")) {  // need scene results for decision
                        String scenes = new String(Arrays.copyOfRange(dataRES, i + 46, i + 56));
                        Log.i(TAG, "scenes: " + scenes);

                        // based on scene, make final decision of
                        // facecase c5(blur) or c6(don't blur)
                    }

                    bbxpos[0] = Math.max(bbxpos[0], 0);
                    bbxpos[1] = Math.max(bbxpos[1], 0);
                    bbxpos[2] = Math.min(bbxpos[2], xmax);
                    bbxpos[3] = Math.min(bbxpos[3], ymax);

                    bbxposArr[ibbx] = bbxpos;
                    bbxtxtArr[ibbx] = username + " " + facecase;

                    // c0: not registered user
                    // c1: no gesture
                    // c2: yes gesture
                    // c3: out of distance
                    // c4: other feature matched
                    // c5: scene matched
                    // c6: pass all

                    if (facecase.equals("c0") || facecase.equals("c2") ||
                            facecase.equals("c3") || facecase.equals("c6"))
                        bbxprocArr[ibbx] = false;
                    else
                        bbxprocArr[ibbx] = true;

                    bbxproctypeArr[ibbx] = policy;
                    ibbx += 1;
                }

                // parse hand result
                for (int i = resSizes[0]; i < resDataSize; i += 24) {
                    int[] bbxpos = byteToInt(Arrays.copyOfRange(dataRES, i, i+16));
                    int bbxcls = byteToInt(Arrays.copyOfRange(dataRES, i+16, i+20))[0];
                    float scr = ByteBuffer.wrap(Arrays.copyOfRange(dataRES, i+20, i+24)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    Log.i(TAG, "hand bbx: " + Arrays.toString(bbxpos) + ", class: "
                            + bbxcls + ", score: " + scr);

                    bbxpos[0] = Math.max(bbxpos[0], 0);
                    bbxpos[1] = Math.max(bbxpos[1], 0);
                    bbxpos[2] = Math.min(bbxpos[2], xmax);
                    bbxpos[3] = Math.min(bbxpos[3], ymax);

                    bbxposArr[ibbx] = bbxpos;
                    bbxtxtArr[ibbx] = (bbxcls == 2 ? "yes" : (bbxcls == 3 ? "no" : "normal")) + " " + scr;
                    bbxprocArr[ibbx] = false;
                    bbxproctypeArr[ibbx] = 2;
                    ibbx += 1;
                }

                // pass to jni for image processing
                if (bbxnum > 0)
                    mResultFrm = fdetector.boxesProcess(mResultFrm, bbxposArr, bbxtxtArr, bbxprocArr, bbxproctypeArr);

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

    private void display() {
        mImageView.setVisibility(View.VISIBLE);
        mFabBtnYes.setVisibility(View.VISIBLE);
        mFabBtnNo.setVisibility(View.VISIBLE);

        Bitmap bitmap = BitmapFactory.decodeByteArray(mResultFrm, 0, mResultFrm.length);

        Matrix matrix = new Matrix();
        matrix.postRotate(360 - 90 * orientCase);

        if (front1back0 == 1) {
            matrix.preScale(-1, 1);
        }

        bitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        mImageView.setImageBitmap(bitmap);
    }


    /**
     * bind th camera with surface view. Initialize preview and start preview.
     **/
    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "surfaceCreated() called...");
            Log.i(TAG, "Clear Total Feature Cache.");
            SelfieActivity.myTotalFaceFeatures.clear();
            SelfieActivity.hisTotalFaceFeatures.clear();
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
//                    Log.i(TAG, "Supported preview size: " + s.width + ", " + s.height);
                    if (s.width > size.width)
                        size = s;
                }
                params.setPreviewSize(size.width, size.height);
                Log.i(TAG, "Preview size: " + size.width + ", " + size.height);
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

                imgSize = params.getPictureSize();
                Camera.Size targetSize = mCamera.new Size(0, 0);
                for (Camera.Size s : params.getSupportedPictureSizes()) {
//                    Log.i(TAG, "Supported image size: " + s.width + ", " + s.height);
                    if (s.width > targetSize.width && s.width < 2000)
                        targetSize = s;
                }
                imgSize = targetSize;
                params.setPictureFormat(PixelFormat.JPEG);
                params.setJpegQuality(100);
                params.setPictureSize(imgSize.width, imgSize.height);
                Log.i(TAG, "Image Size: " + imgSize.width + ", " + imgSize.height);

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


    /**
     * Load files and prepare for caffe tasks
     **/
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

//        if (caffeScene == null) {
//            caffeScene = new CaffeMobile(sceneProtoPath, sceneModelPath);
//            caffeScene.setNumThreads(2);
//        }
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
        initPreview(size.width, size.height);
        startPreview();

    }

    private void switchMode() {
        mode = 1 - mode;
    }

    private byte[] intToByte(int[] input) {
        byte[] output = new byte[input.length * 4];

        for (int i = 0; i < input.length; i++) {
            output[i * 4] = (byte) (input[i] & 0xFF);
            output[i * 4 + 1] = (byte) ((input[i] & 0xFF00) >>> 8);
            output[i * 4 + 2] = (byte) ((input[i] & 0xFF0000) >>> 16);
            output[i * 4 + 3] = (byte) ((input[i] & 0xFF000000) >>> 24);
        }

        return output;
    }

    private int[] byteToInt(byte[] input) {
        int[] output = new int[input.length/4];

        for (int i = 0; i < input.length; i += 4) {
            output[i/4] = input[i] & 0xFF |
                    (input[i+1] & 0xFF) << 8 |
                    (input[i+2] & 0xFF) << 16 |
                    (input[i+3] & 0xFF) << 24;
        }
        return output;
    }

    private byte[] doubleToByte(double[] input) {
        byte[] output = new byte[input.length * 8];
        for (int i = 0; i < input.length; i++)
            ByteBuffer.wrap(output, 8 * i, 8).order(ByteOrder.LITTLE_ENDIAN).putDouble(input[i]);
        return output;
    }

    private byte[] floatToByte(float[] input) {
        byte[] output = new byte[input.length*4];
        for (int i=0; i < input.length; i++)
            ByteBuffer.wrap(output, 4 * i, 4).order(ByteOrder.LITTLE_ENDIAN).putFloat(input[i]);
        return output;
    }

    @Override
    public void onTaskCompleted(boolean result) {
        if (result) {
            display();
        }
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

