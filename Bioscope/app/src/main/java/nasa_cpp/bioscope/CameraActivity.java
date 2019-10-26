package nasa_cpp.bioscope;a

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CameraActivity extends Activity {

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITING_PERMISSION_RESULT = 1;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;

    private int mCaptureState = STATE_PREVIEW;

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
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
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

    private MediaRecorder mMediaRecorder;

    private int totalRotation;

    private CameraCaptureSession cameraCaptureSession;
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult captureResult){
            switch (mCaptureState){
                case STATE_PREVIEW:
                    //Do nothing
                    break;
                case STATE_WAIT_LOCK:
                    mCaptureState = STATE_PREVIEW;
                    //Auto Focused Locked and ready to take pic
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                        startStillCaptureRequest();
                    }
                    break;
            }
        }
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

    //filedirectory
    private String mVid;
    private String mPic;
    private File bioscopeFolder;

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
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
            vidTime.stop();
            vidTime.setVisibility(View.INVISIBLE);
            isRecording = false;
            ((ImageButton) imageButton).setImageResource(R.mipmap.btn_capture_video);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            startPreview();
        }else{
            if (isExternalStorageWritable()) {
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
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //figures out if the resolution need to be changed based on camera orientation
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                totalRotation = sensorDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;

                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                //makes video capture size same as preview size
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                mPictureSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageReader = ImageReader.newInstance(mPictureSize.getWidth(), mPictureSize.getHeight(), ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
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

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), //mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured( CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            try{
                                cameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                            }catch (CameraAccessException e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed( CameraCaptureSession session) {
                            Toast.makeText(getApplicationContext(), "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
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

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(),null,null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startStillCaptureRequest(){
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());

            //ensures image doesnt come out skewed (for orientation)
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, totalRotation);

            //called when camera is focused
            CameraCaptureSession.CaptureCallback stillCap = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted( CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    try {
                        createPicFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            cameraCaptureSession.capture(mCaptureRequestBuilder.build(), stillCap,  null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread(){
        mBackgroundHandlerThread = new HandlerThread("Cameratest");
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

    private static int sensorDeviceRotation(CameraCharacteristics c, int deviceOrientation){
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation+deviceOrientation+360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height){
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option: choices){
            if(option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height){
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0){
            return Collections.min(bigEnough, new CompareSizeByArea());
        }else {
            return choices[0];
        }
    }

    //creates fold in which all pics and vids will be stored
    private void createFolder(){
        File path = Environment.getExternalStorageDirectory();
        bioscopeFolder = new File(path, "BioscopeFiles");
        if (!bioscopeFolder.exists()){
            bioscopeFolder.mkdir();
        }
    }

    //creates a file using current time for vids
    private File createVidFileName() throws IOException{
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String prepend = "VIDEO_" + timestamp + "_";
        File vidFile = File.createTempFile(prepend, ".mp4", bioscopeFolder);
        mVid = vidFile.getAbsolutePath();
        return vidFile;
    }

    //creates a file using current time for pics
    private File createPicFileName() throws IOException{
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String prepend = "IMAGE_" + timestamp + "_";
        File picFile = File.createTempFile(prepend, ".jpg", bioscopeFolder);
        mPic = picFile.getAbsolutePath();
        return picFile;
    }


    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
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

    private void lockFocus(){
        mCaptureState = STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            cameraCaptureSession.capture(mCaptureRequestBuilder.build(), captureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}