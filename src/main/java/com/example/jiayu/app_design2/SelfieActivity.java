package com.example.jiayu.app_design2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.kyleduo.switchbutton.SwitchButton;
import com.rzheng.fdlib.FaceDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class SelfieActivity extends Activity {

    private static final String TAG = "SelfieActivity: ";
    private SurfaceView mPreview;
    private SurfaceHolder mPreviewHolder;
    private Camera mCamera;
    private boolean mCameraConfigured = false;
    private boolean mInPreview = false;
    private byte[] callbackBuffer;
    private Camera.Size size;
    private int camInUse = 1;
    private SwitchButton camSwitch;
    private FaceDetector fdetector;
    private int[] faceArr = new int[] {};
    private DrawOnTop mDraw;
    private int viewHeight, viewWidth;
    private double contentDistortionScaleH, contentDistortionScaleW;
    private boolean shouldContinue;
    private boolean fdetReady = false;
    private boolean fcaffeReady = false;
    private jniFaceThread jniFaceTH;
    private int alignedFacesCacheNum;
    private NumberProgressBar featureExtractProgressBar;
    private CaffeMobile caffeFace;
    private int batchSize;
    private int orientCase;
    private OrientationEventListener mOrientationListener;
    private SweetAlertDialog loadingDialog;

    private float[][] batchFaceFeatures;
    private List<float[]> totalFaceFeatures = new ArrayList<float[]>();

    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ContextPrivacy/";
    private String landmarksFilePath = DATA_PATH + "shape_predictor_68_face_landmarks.dat";
    private String faceProtoPath = DATA_PATH + "LightenedCNN_B_deploy.prototxt";
    private String faceModelPath = DATA_PATH + "LightenedCNN_B.caffemodel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie);

        mPreview = (SurfaceView) findViewById(R.id.selfiePreview);
        mPreviewHolder = mPreview.getHolder();
        mPreviewHolder.addCallback(surfaceCallback);

        camSwitch = (SwitchButton) findViewById(R.id.selfieSwitchCamBtn);
        camSwitch.setChecked(camInUse == 1 ? true : false);
        camSwitch.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                switchCam();
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
                //Log.i(TAG, "Orientation changed to " + orientation +
                //        ", case " + orientCase);
            }
        };

        featureExtractProgressBar = (NumberProgressBar)findViewById(R.id.featureExtractProgressBar);

        Log.i(TAG, "now in selfie mode");

        if (MainActivity.fdetector == null || MainActivity.caffeFace == null) {
            new initializeTask().execute();
            loadingDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
            loadingDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            loadingDialog.setTitleText("Initializing");
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        } else {
            fdetector = MainActivity.fdetector;
            caffeFace = MainActivity.caffeFace;
            batchSize = MainActivity.batchSize;
            alignedFacesCacheNum = 0;
            fdetector.clearCache();
            fdetReady = true;
            fcaffeReady = true;
        }
    }

    private class initializeTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            if (MainActivity.fdetector == null) {
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
                MainActivity.fdetector = new FaceDetector(cascadeFile.getAbsolutePath(), MainActivity.batchSize);
                Log.i(TAG, landmarksFilePath);
                MainActivity.fdetector.loadShapePredictor(landmarksFilePath);
            }

            if (MainActivity.caffeFace == null) {
                MainActivity.caffeFace = new CaffeMobile(faceProtoPath, faceModelPath);
                MainActivity.caffeFace.setNumThreads(2);
            }

            fdetector = MainActivity.fdetector;
            caffeFace = MainActivity.caffeFace;
            batchSize = MainActivity.batchSize;
            alignedFacesCacheNum = 0;
            fdetector.clearCache();

            return true;
        }

        protected void onPostExecute(Boolean ready) {
            // take button enabled
            Log.i(TAG, "All models are loaded.");
            loadingDialog.dismissWithAnimation();
            fdetReady = true;
            fcaffeReady = true;
        }
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, " surfaceCreated() called.");
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(TAG, " surfaceChanged() called.");
            initPreview(width, height);
            startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, " surfaceDestroyed() called.");
        }
    };

    Camera.PreviewCallback frameProcCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //Log.i(TAG, "onPreviewFrame() called.");
            mCamera.addCallbackBuffer(callbackBuffer);
            if (mCameraConfigured) {
                jniFaceTH.setFrm(data);
                mDraw.invalidate();
                featureExtractProgressBar.setProgress(totalFaceFeatures.size());
            }
        }
    };

    private class jniFaceThread extends Thread {
        private byte[] frm;

        public void run() {
            while (shouldContinue) {
                if (fdetReady && frm != null && size.width * size.height * 1.5 == frm.length) {
                    frameProc(frm);
                } else {
                    // case1: thread started but camera not ready yet
                    // case2: size changed (initPreview() called )
                    //        but callbackBuffer not updated (onPreviewCallback() called) yet
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void setFrm(byte[] bytes) {
            frm = bytes;
        }
    }

    private void frameProc(byte[] framedata) {
        if (alignedFacesCacheNum < batchSize) {
            //Log.i(TAG, "Upper stream size: " + size.width + " X " + size.height + " Length: " + framedata.length);
            faceArr = fdetector.detect(size.width, size.height, framedata,
                    camInUse, orientCase, alignedFacesCacheNum < batchSize);
            for (int i = 0; i < faceArr.length; i += 4) {
                faceArr[i] = (int) (faceArr[i] * contentDistortionScaleW);
                faceArr[i + 1] = (int) (faceArr[i + 1] * contentDistortionScaleH);
                faceArr[i + 2] = (int) (faceArr[i + 2] * contentDistortionScaleW);
                faceArr[i + 3] = (int) (faceArr[i + 3] * contentDistortionScaleH);
            }
            //Log.i(TAG, Arrays.toString(faceArr));
            //Log.i(TAG, "Detect " + faceArr.length / 4 + " faces.");
            alignedFacesCacheNum += alignedFacesCacheNum < batchSize ? faceArr.length / 4 : 0;
            Log.i(TAG, "AF Cache Size: " + alignedFacesCacheNum);
        } else {
            if (fcaffeReady && alignedFacesCacheNum == 1)
                extractFeatures();
            alignedFacesCacheNum = 0;
//            try {
//                Thread.sleep(200);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    private void extractFeatures() {
        batchFaceFeatures = caffeFace.extractFeaturesCVBatch(fdetector.getAlignedFacesAddr(),
                "eltwise_fc1");
        Log.i(TAG, "Total: " + batchFaceFeatures.length +
                " features, each feature length: " + batchFaceFeatures[0].length);
//        for (float[] feat : batchFaceFeatures) {
//            Log.i(TAG, Arrays.toString(feat));
//        }
        for (int i=0; i < batchFaceFeatures.length; i++)
            totalFaceFeatures.add(batchFaceFeatures[i]);
    }

    private void initPreview(int width, int height) {
        Log.i(TAG, "initPreview() called.");
        if (mCamera != null && mPreviewHolder.getSurface() != null) {
            if (!mCameraConfigured) {
                Camera.Parameters params = mCamera.getParameters();
                size = params.getPreviewSize();
                for (Camera.Size s : params.getSupportedPreviewSizes()) {
                    if (s.width > size.width)
                        size = s;
                }
                params.setPreviewSize(size.width, size.height);
                Log.i(TAG, "Preview size: " + size.width + ", " + size.height);
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                mCamera.setParameters(params);
                callbackBuffer = new byte[(size.height + size.height / 2) * size.width];
                //Log.i(TAG, "New Callback Buffer Size: " + callbackBuffer.length);
                contentDistortionScaleH = viewHeight * 1.0 / size.width;
                contentDistortionScaleW = viewWidth * 1.0 / size.height;

                if (mOrientationListener.canDetectOrientation() == true) {
                    mOrientationListener.enable();
                }
            }

            try {
                mCamera.setPreviewDisplay(mPreviewHolder);
                mCamera.addCallbackBuffer(callbackBuffer);
                mCamera.setPreviewCallbackWithBuffer(frameProcCallback);
                //Log.i(TAG, "Callback Buffer Updated.");
            } catch (Throwable t) {
                Log.e(TAG, "Exception in initPreview()", t);
            }

            if (mDraw == null) {
                //Log.i(TAG, "mDraw newed.");
                mDraw = new DrawOnTop(this);
                addContentView(mDraw, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            mDraw.clearCanvas();

            if (jniFaceTH == null) {
                jniFaceTH = new jniFaceThread();
                shouldContinue = true;
                jniFaceTH.start();
            }

            mCameraConfigured = true;
            //Log.i(TAG, "Camera configured.");
        }
    }

    private void startPreview() {
        if (mCameraConfigured && mCamera != null) {
            mCamera.startPreview();
            mInPreview = true;
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, " onResume() called.");
        super.onResume();
        mCamera = Camera.open(camInUse);
        mCamera.setDisplayOrientation(90);
        startPreview();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, " onPause() called.");
        mDraw.clearCanvas();
        if (mInPreview)
            mCamera.stopPreview();

        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.release();
        mCamera = null;
        mInPreview = false;
        shouldContinue = false;
        jniFaceTH = null;
        mCameraConfigured = false;
        // sm.unregisterListener(sListener);
        mOrientationListener.disable();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, " onDestroy() called.");
        super.onDestroy();
    }

    private void switchCam() {
        if (mCamera != null && mInPreview) {
            mDraw.clearCanvas();
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.release();
            mCamera = null;
            mInPreview = false;
            mCameraConfigured = false;
        }

        camInUse = 1 - camInUse;
        mCamera = Camera.open(camInUse);
        mCamera.setDisplayOrientation(90);
        initPreview(size.width, size.height);
        startPreview();

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        Rect rect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        View v = getWindow().findViewById(Window.ID_ANDROID_CONTENT);

        viewHeight = v.getHeight();
        viewWidth = v.getWidth();
        contentDistortionScaleH = viewHeight * 1.0 / size.width;
        contentDistortionScaleW = viewWidth * 1.0 / size.height;
        Log.i(TAG, "Status Bar Height: " + Integer.toString(rect.top) +
                ", Content Top: " + Integer.toString(v.getTop()) +
                ", Content Height: " + Integer.toString(v.getHeight()) +
                ", Content Width: " + Integer.toString(v.getWidth()) +
                ", Content / CameraView ratio Height: " + contentDistortionScaleH +
                ", Content / CameraView ratio Width: " + contentDistortionScaleW);
    }


    class DrawOnTop extends View {
        Paint paintFace;

        public DrawOnTop(Context context) {
            super(context);

            paintFace = new Paint();
            paintFace.setStyle(Paint.Style.STROKE);
            paintFace.setStrokeWidth(3);
            paintFace.setColor(Color.RED);

        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (faceArr.length > 0) {
                assert(faceArr.length % 4 == 0);
                for (int i = 0; i < faceArr.length; i += 4) {
                    canvas.drawRect(faceArr[i], faceArr[i+1], faceArr[i+2], faceArr[i+3], paintFace);
                }
            }
        }

        public void clearCanvas() {
            faceArr = new int[] {};
            Log.i(TAG, "faceArr cleared");
            this.invalidate();
        }
    }



}
