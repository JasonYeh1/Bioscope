package com.example.hoho.bioscope_android.Activities;

import com.example.hoho.bioscope_android.R;
import com.example.hoho.bioscope_android.Tools.StorageTools;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

public class CameraActivity extends Activity {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITING_PERMISSION_RESULT = 1;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private int mCaptureState = STATE_PREVIEW;

    private static final int MAX_PREVIEW_WIDTH = 1920;

    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    //view that will hold what camera sees while in preview state
    private TextureView cameraView;
    private TextureView.SurfaceTextureListener mSurfaceTextListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //sets up camera if textureView is available
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if(mCameraDevice != null){
                closeCamera();

                mCameraDevice = null;
            }
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallBackListener = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            //Toast.makeText(getApplicationContext(), "Camera connection made", Toast.LENGTH_SHORT).show();

            //for initial start up of app when checking for permissions
            //app pauses and resumes so it needs to restart
            if (isRecording) {
                try {
                    createVidFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecording();
                mMediaRecorder.start();
                vidTime.setBase(SystemClock.elapsedRealtime());
                vidTime.setVisibility(View.VISIBLE);
                vidTime.start();
            } else{
                startPreview();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
        }
    };
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private String mCameraId;

    private Size mPreviewSize;
    private Size mVideoSize;
    private Size mPictureSize;

    private CaptureRequest mPreviewRequest;

    private int totalRotation;

    protected CameraCharacteristics cameraCharacteristics;
    private CameraCaptureSession cameraCaptureSession;
    //private CameraCaptureSession recordCaptureSession;

    private int loading;

    //takes care of different capture states of the camera
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result){

            switch (mCaptureState){
                case STATE_PREVIEW:
                    //nothing to handle for an image capture when just displaying a preview
                    break;
                case STATE_WAIT_LOCK:
                    //camera is preparing to capture image
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    System.out.println(afState + " THIS IS THE CURRENT STATE");
                    if(afState == 1){
                        loading++;
                        if (loading > 20){
                            unlockFocus();
                            Toast.makeText(getApplicationContext(), "Failed to Focus", Toast.LENGTH_SHORT).show();
                            loading = 0;
                        }
                    }
                    if (afState == null) {
                        startStillCaptureRequest();
                    //check to see if the camera has finished looking for something to focus on
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mCaptureState = STATE_PICTURE_TAKEN;
                            startStillCaptureRequest();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mCaptureState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mCaptureState = STATE_PICTURE_TAKEN;
                        startStillCaptureRequest();
                    }
                    break;
                }
            }
        }
        //override the capture completed method to pass the progress of capture along to be handled
        @Override
        public void onCaptureCompleted( CameraCaptureSession session, CaptureRequest request,  TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }
    };

    private CaptureRequest.Builder mCaptureRequestBuilder;
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    //set up orientations in order to know how to set up picture relative to which way phone is oriented
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private ImageButton videoImageButton;
    private ImageButton pictureImageButton;
    private boolean isRecording = false;

    private Chronometer vidTime;

    private MediaRecorder mMediaRecorder;

    //filedirectory
    private String mVid;
    private String mPic = "null";
    private File bioscopeFolder;

    //handles files saving
    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //get the image and pass it to imageSaver so it can be written to storage
            mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));

        }
    };

    private class ImageSaver implements Runnable{

        private final Image mImage;

        public ImageSaver(Image i){
            mImage = i;
        }

        @Override
        public void run() {
            //get all data for an image
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            //put all data into files
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(mPic);
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                mImage.close();
                if (fileOutputStream != null){
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //Zooming
    protected float fingerSpacing = 0;
    protected float zoomLevel = 1f;
    protected float maximumZoomLevel;
    protected Rect zoom;

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        createFolder();

        mMediaRecorder = new MediaRecorder();

        cameraView = (TextureView) findViewById(R.id.textureView);
        videoImageButton = (ImageButton) findViewById(R.id.imageButton_vid);
        pictureImageButton = (ImageButton) findViewById(R.id.imageButton_pic);
        vidTime = (Chronometer) findViewById(R.id.vidtimer);
        vidTime.setVisibility(View.INVISIBLE);
    }

    //handles what the recording button does
    public void record(View imageButton){
        if(isRecording){
            //recordCaptureSession.close();
            vidTime.stop();
            vidTime.setVisibility(View.INVISIBLE);
            isRecording = false;
            ((ImageButton) imageButton).setImageResource(R.mipmap.btn_capture_video_round);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            startPreview();
        }else{
            if (StorageTools.isExternalStorageWritable()) {
                isRecording = true;
                ((ImageButton) imageButton).setImageResource(R.mipmap.btn_capture_video_rec);
                try {
                    createVidFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecording();
                mMediaRecorder.start();
                vidTime.setBase(SystemClock.elapsedRealtime());
                vidTime.setVisibility(View.VISIBLE);
                vidTime.start();
            }else{
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_WRITING_PERMISSION_RESULT);
            }
        }
    }

    //handles what the capture button does
    public void capture(View imageButton){
        lockFocus();
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(StorageTools.getDirectory().getAbsolutePath())));
        Toast.makeText(getApplicationContext(),"Picture Taken", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

      try {
            Rect rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if (rect == null) return false;
            float currentFingerSpacing;

            if (event.getPointerCount() == 2) { //Multi touch.
                currentFingerSpacing = getFingerSpacing(event);
                float delta = 0.1f; //Control this value to control the zooming sensibility
                if (fingerSpacing != 0) {
                    if (currentFingerSpacing > fingerSpacing) { //Don't over zoom-in
                        if ((maximumZoomLevel - zoomLevel) <= delta) {
                            delta = maximumZoomLevel - zoomLevel;
                        }
                        zoomLevel = zoomLevel + delta;
                    } else if (currentFingerSpacing < fingerSpacing) { //Don't over zoom-out
                        if ((zoomLevel - delta) < 1f) {
                            delta = zoomLevel - 1f;
                        }
                        zoomLevel = zoomLevel - delta;
                    }
                    float ratio = (float) 1 / zoomLevel; //This ratio is the ratio of cropped Rect to Camera's original(Maximum) Rect
                    //croppedWidth and croppedHeight are the pixels cropped away, not pixels after cropped
                    int croppedWidth = rect.width() - Math.round((float) rect.width() * ratio);
                    int croppedHeight = rect.height() - Math.round((float) rect.height() * ratio);
                    //Finally, zoom represents the zoomed visible area
                    zoom = new Rect(croppedWidth / 2, croppedHeight / 2,
                            rect.width() - croppedWidth / 2, rect.height() - croppedHeight / 2);
                    mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                }
                fingerSpacing = currentFingerSpacing;
            } else { //Single touch point, needs to return true in order to detect one more touch point
                return true;
            }
            cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), captureCallback, null);
        } catch (final Exception e) {

        }
        return true;
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (cameraView.isAvailable()) {
            setupCamera(cameraView.getWidth(), cameraView.getHeight());
            connectCamera();
        } else {
            cameraView.setSurfaceTextureListener(mSurfaceTextListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        endBackgroundThread();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    //handles permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case REQUEST_CAMERA_PERMISSION_RESULT:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getApplicationContext(), "Application will not run without camera services", Toast.LENGTH_SHORT ).show();
                }
                break;
            case REQUEST_WRITING_PERMISSION_RESULT:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    isRecording = true;
                    videoImageButton.setImageResource(R.mipmap.btn_capture_video_rec);
                    try {
                        createVidFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    //setup the screen so it is full screen
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View cameView = getWindow().getDecorView();

        if (hasFocus) {
            cameView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void setupCamera(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //get the type of camera and makes sure it is the back facing camera
            for (String cameraId : manager.getCameraIdList()) {
                cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //figures out if the resolution need to be changed based on camera orientation
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                maximumZoomLevel = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizeByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
                mImageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);

                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                totalRotation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (totalRotation == 90 || totalRotation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (totalRotation == 0 || totalRotation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }
                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;


                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);
                //makes video capture size same as preview size
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                mPictureSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);
                mCameraId = cameraId;

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                    Toast.makeText(this, "App required access to camera", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
            }
            cameraManager.openCamera(mCameraId, mCameraDeviceStateCallBackListener, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = cameraView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            if (mImageReader.getSurface().isValid()) {
                mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                cameraCaptureSession = session;
                                try {
                                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    mPreviewRequest = mCaptureRequestBuilder.build();
                                    cameraCaptureSession.setRepeatingRequest(mPreviewRequest, null, mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                                Toast.makeText(getApplicationContext(), "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onClosed(CameraCaptureSession session) {
                                super.onClosed(session);
                            }
                        }, null);
            }
            } catch(CameraAccessException e){
                e.printStackTrace();
            }
    }

    private void startRecording(){
        try {
            setUpMediaRecorder();
            SurfaceTexture surfaceTexture = cameraView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mMediaRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    //recordCaptureSession = session;
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(),null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
                @Override
                public void onClosed(CameraCaptureSession session){
                    super.onClosed(session);

                    startPreview();
                }
            }, mBackgroundHandler);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mCaptureState = STATE_WAITING_PRECAPTURE;
            cameraCaptureSession.capture(mCaptureRequestBuilder.build(), captureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startStillCaptureRequest(){
        try {

            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());

            //Zoom
            if (zoom != null) {
                mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            }

            //ensures image doesn't come out skewed (for orientation)
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, totalRotation);

            //called when camera is focused
            CameraCaptureSession.CaptureCallback stillCap = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    try {
                        createPicFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    unlockFocus();

                }
            };
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.abortCaptures();
            }
            cameraCaptureSession.capture(mCaptureRequestBuilder.build(), stillCap, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void lockFocus(){
        mCaptureState = STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), captureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            cameraCaptureSession.capture(mCaptureRequestBuilder.build(), captureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mCaptureState = STATE_PREVIEW;
            cameraCaptureSession.setRepeatingRequest(mPreviewRequest, captureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        try {
            mCameraOpenCloseLock.acquire();
            if (null != cameraCaptureSession) {
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void startBackgroundThread(){
        mBackgroundHandlerThread = new HandlerThread("CameraHandler");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void endBackgroundThread(){
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizeByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    //chooses correct video resolution for device
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }

    //creates fold in which all pics and vids will be stored
    private void createFolder(){
        bioscopeFolder = StorageTools.getDirectory();
    }

    //creates a file using current time for vids
    private File createVidFileName() throws IOException{
        String timestamp = new SimpleDateFormat("yyMMdd-HHmmss").format(new Date());
        String prepend = "VIDEO_" + timestamp + "_";
        File vidFile = File.createTempFile(prepend, ".mp4", bioscopeFolder);
        mVid = vidFile.getAbsolutePath();
        return vidFile;
    }

    //creates a file using current time for pics
    private File createPicFileName() throws IOException{
        String timestamp = new SimpleDateFormat("yyMMdd-HHmmss").format(new Date());
        String prepend = "IMAGE_" + timestamp + "_";
        File picFile = File.createTempFile(prepend, ".jpg", bioscopeFolder);
        mPic = picFile.getAbsolutePath();
        System.out.println("This is the mPic inside createPicFileName" + mPic);
        return picFile;
    }

    //setup video recording info
    private void setUpMediaRecorder() throws IOException{
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVid);
        mMediaRecorder.setVideoEncodingBitRate(1000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setOrientationHint(totalRotation);
        mMediaRecorder.prepare();
    }
}