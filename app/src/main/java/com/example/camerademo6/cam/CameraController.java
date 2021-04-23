package com.example.camerademo6.cam;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.example.camerademo6.R;
import com.example.camerademo6.storage.SharedPreferencesController;
import com.example.camerademo6.utils.Utils;
import com.example.camerademo6.view.AutoFitTextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraController {

    public static final float PREVIEW_SIZE_RATIO_OFFSET = 0.01f;
    private static final int BACK_CAMERA_ID = 0;
    private static final int FRONT_CAMERA_ID = 1;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;
    private MediaRecorder mMediaRecorder;

    private int mCameraId = 0;
    private int mCurrentMode = CameraConstant.PHOTO_MODE;
    private CameraManager manager;
    private AutoFitTextureView mPreviewTexture;
    private Activity mActivity;

    private Size mPreviewSize = new Size(1440, 1080);
    private Size mCaptureSize = new Size(4000, 3000);
    private Size mVideoSize = new Size(1920, 1080);
    private float mTargetRatio = 1.333f;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    private File mFile;
    private int mPhoneOrientation;
    private int mSensorOrientation;


    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    //开启线程
    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    public void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    构造函数
    * 引用自定义textureView、activity
    * 通过主activity获取系统的camera服务：实例化CameraManager
    *
    * */
    public CameraController(Activity activity, AutoFitTextureView textureView) {
        mPreviewTexture = textureView;
        mActivity = activity;
        manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        //mPermissionRequest = new PermissionRequest(mActivity);
    }
    ////////////////////参数获得/////////
    public void setCurrentMode(int currentMode) {
        mCurrentMode = currentMode;
    }

    public void setTargetRatio(float ratio) {
        mTargetRatio = ratio;
    }

    public void setPhotoPath(File file) {
        mFile = file;
    }

    public void setPhoneDeviceDegree(int degree) {
        mPhoneOrientation = degree;
    }

    /////////openCamera//////////////////////////////

    /**
     * 自定义openCamera
     * 检查权限
     * 判断当前模式： 如果是录像模式或者慢动作模式----->实例化mediaRecorder
     * 调用 manager.openCamera 传递打开的摄像头、回调、线程**/
    public void openCamera() {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //LogUtils.logD("checkSelfPermission");
            //mPermissionRequest.requestCameraPermission();
            return;
        }

        try {
            if (mCurrentMode == CameraConstant.VIDEO_MODE || mCurrentMode == CameraConstant.SLOW_FPS_MODE) {
                mMediaRecorder = new MediaRecorder();
            }

            //LogUtils.logD("begin to opencamera mCameraId =" + mCameraId);
            manager.openCamera(String.valueOf(mCameraId), mStateCallback, mBackgroundHandler);
        } catch (Exception exception) {
            //Log.e(TAG, "exception:" + exception);
            exception.printStackTrace();
        }
    }

    /*
   使用CameraManager打开一个具体的摄像头设备的回调
   *获得（引用）cameraDevice
   *选择预览和捕获大小
   *创建预览session
   **/
    CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() { //打开摄像头的回调

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {

            Log.i("session","获取device");
            mCameraDevice = cameraDevice;//获取cameraDevice
            Log.i("session","device引用完成，即将选择大小、创session");
            choosePreviewAndCaptureSize();
            createCameraPreviewSession();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

            cameraDevice.close();//关闭返回的cameradevice
            mCameraDevice = null;// 自己的cameradevice对象置为空

        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {

            cameraDevice.close();//   关闭返回的cameradevice
            mCameraDevice = null;//自己的cameradevice对象设置为空
            mActivity.finish();//关闭activity
        }
    };
    /////////////设置预览、图片的大小////////////////////////
    /**
     * 设预览大小、摄像大小、拍照大小
     * 给manager传id拿到相机支持属性
     * 支持属性中拿到支持的预览大小
     * 选择预览的大小（具体？） 摄像大小 拍照大小
     * 实例化imagereader，为imagereader设置监听
     * 更改自定义预览view的比例
     **/
    private void choosePreviewAndCaptureSize() {
        CameraCharacteristics characteristics
                = null;
        try {
            characteristics = manager.getCameraCharacteristics(String.valueOf(mCameraId));//0 1
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);/////////
        StreamConfigurationMap map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] previewSizeMap = map.getOutputSizes(SurfaceTexture.class);//preview
        Size[] captureSizeMap = map.getOutputSizes(ImageFormat.JPEG);//拍照
        //Size[] vedioSizeMap = map.getOutputSizes(MediaRecorder.class);//拍照
        int screenWidth = Utils.getScreenWidth(mActivity.getApplicationContext());
        mPreviewSize = getPreviewSize(previewSizeMap, mTargetRatio, screenWidth);
        if (mCurrentMode == CameraConstant.VIDEO_MODE || mCurrentMode == CameraConstant.SLOW_FPS_MODE) {
            mVideoSize = mPreviewSize;
        }

        mCaptureSize = getPictureSize(mTargetRatio, captureSizeMap);
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(),
                ImageFormat.JPEG, /*maxImages*/1);//RAW_SENSOR YUV_420_888
        mImageReader.setOnImageAvailableListener(
                mOnImageAvailableListener, mBackgroundHandler);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPreviewTexture.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());//
            }
        });
    }
    public Size getPreviewSize(Size[] mapPreview, float targetRatio, int screenWidth) {
        Size previewSize = null;
        int minOffSize = Integer.MAX_VALUE;
        for (int i = 0; i < mapPreview.length; i++) {
            float ratio = mapPreview[i].getWidth() / (float) mapPreview[i].getHeight();
            if (Math.abs(ratio - targetRatio) > PREVIEW_SIZE_RATIO_OFFSET) {
                continue;
            }
            int widthDiff = Math.abs(screenWidth - mapPreview[i].getHeight());
            if (widthDiff <= minOffSize) {
                previewSize = mapPreview[i];
                minOffSize = widthDiff;
            }
        }
        return previewSize;
    }

    public Size getPictureSize(float targetRatio, Size[] mapPicture) {
        Size maxPicSize = new Size(0, 0);
        for (int i = 0; i < mapPicture.length; i++) {
            float ratio = mapPicture[i].getWidth() / (float) mapPicture[i].getHeight();
            if (Math.abs(ratio - targetRatio) > PREVIEW_SIZE_RATIO_OFFSET) {
                continue;
            }
            if (mapPicture[i].getWidth() * mapPicture[i].getHeight() >= maxPicSize.getWidth() * maxPicSize.getHeight()) {
                maxPicSize = mapPicture[i];
            }
        }
        return maxPicSize;
    }
    ////////////////////创session////////////////

    /*创建预览的session
    *准备工作（surface列）
    * cameradevice创造capturesession
    *必须在拿到cameradevice之后（必需要在设置大小之后吗？）
    * */
    public void createCameraPreviewSession() {
     try {
     SurfaceTexture texture = mPreviewTexture.getSurfaceTexture();
     texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
     //surface是指向屏幕窗口原始图像缓冲区（raw buffer）的一个句柄 通过它可以获得这块屏幕上对应的canvas，进而完成在屏幕上绘制View的工作
     Surface previewSurface = new Surface(texture);
     mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
     mPreviewRequestBuilder.addTarget(previewSurface);
     Log.i("session","创预览session");
     Log.i("session","此时的模式："+mCurrentMode);//mCurrentMode=0;
     mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),  new CameraCaptureSession.StateCallback(){
         @Override
         public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
             if (null == mCameraDevice) {
                 return;
             }
             //LogUtils.logD("createCaptureSession onConfigured");

             mCaptureSession = cameraCaptureSession;
             setPreviewFrameParams();// CaptureRequest.Builder的set
             updatePreview();
         }

         @Override
         public void onConfigureFailed(
                 @NonNull CameraCaptureSession cameraCaptureSession) {

         }
     }, mBackgroundHandler);

     }
     catch (CameraAccessException e) {
         e.printStackTrace();
     }
    }
    /*控制自动对焦模式连续画面？？**/
    private void setPreviewFrameParams() {//下发、传参
        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY,//iso
                100);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        //characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,
                100l);//快门打开时间 ns

        //mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceDetectModes[faceDetectModes.length - 1]);//设置人脸检测级别
    }

    ///////////////////使用session////////
    /*预览？？拍照也走这个*/
    public void updatePreview() {
        try {
            mPreviewRequest = mPreviewRequestBuilder.build();
            //LogUtils.logD("setRepeatingRequest");

            mCaptureSession.setRepeatingRequest(mPreviewRequest,
                    mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
           // process(result);
        }
    };


    /*拍照session设置与使用？*/
    private void beginCaptureStillPicture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {

                }
            };
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegRotation(mCameraId, mPhoneOrientation));//相机的图像方向即是相机图像需要顺时针旋转的角度，以便让图像以正确的角度呈现。这个角度应该是0、90、180、270
            //LogUtils.logD("begin to take picture");

            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////closeCamera//////////////////////////////////////////////
    /**
     * 结束:
     * 关闭session
     * 关闭mediarecorder
     * 关闭imageReader
     * cameradevice关闭置空
     */
    public void closeCamera() {
        closeSession();
        closeMediaRecorder();////////////////当mediaRecorder不为空，（如从摄像模式切回拍照模式时） 关mediaRecorder方法容易出错..try catch？
        closeImageReader();
        closeCameraDevice();
        Log.i("1", "自定义的closeCamera");
    }
    public void closeSession() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }
    public void closeMediaRecorder() {
        if (null != mMediaRecorder) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }
    public void closeImageReader() {
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }
    public void closeCameraDevice() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
    ////////////////////录像//////////////////
    /*关闭正在进行的录像
    * 关mediaRecorder
    * 更新缩略图
    * 关session
    * 重新选择预览和捕获大小
    * 创预览session*/
    public void stopVideoRecording() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        final int target = mActivity.getResources().getDimensionPixelSize(R.dimen.thumbnail_size);
        Uri uri = Uri.fromFile(mFile);

        //Bitmap bitmapThumbnail = createVideoThumbnailBitmap(mFile.toString(), null, target);
        Bitmap bitmapThumbnail = ThumbnailUtils.createVideoThumbnail(mFile.toString(), MediaStore.Images.Thumbnails.MINI_KIND);
        if (mCameraCallback != null)
            mCameraCallback.onThumbnailCreated(bitmapThumbnail);
        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

        closeSession();
        choosePreviewAndCaptureSize();
        createCameraPreviewSession();
    }
    /*开启录像
    * 关session
    * 选择大小
    * 配置mediaRecorder
    * 创录像session并执行*/
    public void startVideoRecording() {
        try {
            closeSession();
            choosePreviewAndCaptureSize();
            setUpMediaRecorder();

            SurfaceTexture texture = mPreviewTexture.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // 预览
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewRequestBuilder.addTarget(previewSurface);

            // 录像
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewRequestBuilder.addTarget(recorderSurface);

            // 创session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    updatePreview();
                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    /*设置录像参数（有一定的顺序限定），为录像做准备*/
    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mFile.getPath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);//设置被捕获video的帧率，必须在设置视频来源之后被调用
        //慢模式判断
        if (mCurrentMode == CameraConstant.SLOW_FPS_MODE) {
            mMediaRecorder.setCaptureRate(120);
        }
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//设置audio的编码器
        mMediaRecorder.setOrientationHint(getJpegRotation(mCameraId, mPhoneOrientation));//设置图片方向
        mMediaRecorder.prepare();//recorder准备开始捕获和编码数据
    }

    /////////保存图片（水印和无水印）/////////////////////////////////////
    /*ImageReader的监听实现*/
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            //LogUtils.logD("onImageAvailable");
            saveImage(reader);//保存
        }
    };
    /*保存图片：有水印or无水印*/
    private void saveImage(ImageReader reader) {
        Image image = reader.acquireNextImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        boolean addMark = SharedPreferencesController.getInstance(mActivity).spGetBoolean(CameraConstant.ADD_WATER_MARK);//从sp拿结果
        if (addMark) {
            saveWithWaterMark(bytes, image);
        } else {
            saveNoWaterMark(bytes, image);
            //simpleSaveNoWaterMark(bytes, image);
        }
    }
    /*无水印保存图片*/
    private void saveNoWaterMark(byte[] bytes, Image image) {
        //BitmapRegionDecoder用来解码一个来自image的矩形范围，当原始的image非常大并且你仅仅使用这个image的一部分时，非常有用
        BitmapRegionDecoder decoder = null;
        try {
            //如果image数据不能被解码就返回空，否则得到一个BitmapRegionDecoder对象
            decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.length, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //静态内部类:BitmapFacotry.Options,用来设置decode时的选项
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true; //如果设置为true，不获取图片，不分配内存，但会返回图片的高宽度信息
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);//解析：从一个指定的byte数组解析一个不可变的位图
        ////如果image数据不能被解码或者如果传入的Options是non-null，就返回空，一般得到一个解码了的位图
        //或..opts requested  则仅有size被返回

        int w = opt.outWidth;//图片宽度值
        int h = opt.outHeight;//图片高度值
        int d = w > h ? h : w;//取小的那个值

        final int target = mActivity.getResources().getDimensionPixelSize(R.dimen.thumbnail_size);// //检索特定资源ID的维度，以用作原始像素中的大小//返回：乘以适当的度量并截断为整数像素的资源维度值
        int sample = 1;
        if (d > target) {
            while (d / sample / 2 > target) {
                sample *= 2;
            }
        }
        int st = sample * target;
        final Rect rect = new Rect((w - st) / 2, (h - st) / 2, (w + st) / 2, (h + st) / 2);
        Bitmap showThumbnail = decoder.decodeRegion(rect, opt);
        Matrix matrix = new Matrix();
        matrix.postRotate(getJpegRotation(mCameraId, mPhoneOrientation));
        if (lenFaceFront()) {/////////////////
            matrix.postScale(-1, 1);
        }
        Bitmap srcBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        Bitmap bitmapThumbnail = Bitmap.createBitmap(showThumbnail, 0, 0, showThumbnail.getWidth(),
                showThumbnail.getHeight(), matrix, true);
        mCameraCallback.onThumbnailCreated(bitmapThumbnail);//更新缩略图
        //mCameraCallback.onThumbnailCreated(srcBitmap);

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);//保存图片到文件

            Uri photouri = Uri.fromFile(mFile);

            mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photouri));
            mCameraCallback.onTakePictureFinished();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*加水印保存
     * 解码得到源图bitmap对象
     * 创建一个指定格式的新bitmap的空对象
     * 实例化一块画布，将新bitmap空对象放在画布上----- 以bitmap对象创建一个画布，将内容都绘制在bitmap上，因此bitmap不得为null。
     * 用画布的draw方法以源bitmap对象的copy作图-->画出这个bitmap
     * 实例化一个画笔并设置画笔绘制风格
     *画布draw方法以画笔绘图*/
    private void saveWithWaterMark(byte[] bytes, Image image) {
        //解析：从一个指定的byte数组解析一个不可变的位图，返回一个解码的bitmap（或空）
        Bitmap bitmapStart = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        Matrix matrix = new Matrix();
        matrix.postRotate(getJpegRotation(mCameraId, mPhoneOrientation));
        if (lenFaceFront()) {
            matrix.postScale(-1, 1);
        }
        Bitmap bitmapSrc = Bitmap.createBitmap(bitmapStart, 0, 0, bitmapStart.getWidth(),
                bitmapStart.getHeight(), matrix, true);//以bitmapStart为原图，创建新的图片
        mCameraCallback.onThumbnailCreated(bitmapSrc);
        Bitmap bitmapNew = Bitmap.createBitmap(bitmapSrc.getWidth(), bitmapSrc.getHeight(), Bitmap.Config.ARGB_8888);//创建指定格式、大小的位图
        Canvas canvasNew = new Canvas(bitmapNew);
        canvasNew.drawBitmap(bitmapSrc, 0, 0, null);//以一定的坐标值在当前画图区域画图，另外图层会叠加， 即后面绘画的图层会覆盖前面绘画的图层

        Paint paintText = new Paint();
        paintText.setColor(Color.argb(80, 255, 255, 255));//设置绘制的颜色，使用颜色值来表示，该颜色值包括透明度和RGB颜色。
        if (lenFaceFront()) {
            paintText.setTextSize(60);
        } else {
            paintText.setTextSize(150);
        }
        paintText.setDither(true);//设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
        paintText.setFilterBitmap(true);//如果该项设置为true，则图像在动画进行中会滤掉对Bitmap图像的优化操作， 加快显示速度，本设置项依赖于dither和xfermode的设置
        Rect rectText = new Rect();
        String drawTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        paintText.getTextBounds(drawTime, 0, drawTime.length(), rectText);
        int beginX = bitmapNew.getWidth() - rectText.width() - 100;
        int beginY = bitmapNew.getHeight() - rectText.height();
        canvasNew.drawText(drawTime, beginX, beginY, paintText);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            bitmapNew.compress(Bitmap.CompressFormat.JPEG, 100, output);
            output.flush();
            Uri photouri = Uri.fromFile(mFile);
            mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photouri));
            mCameraCallback.onTakePictureFinished();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /*
    *
    * */

    public boolean lenFaceFront() {
        CameraCharacteristics cameraInfo = null;
        try {
            cameraInfo = manager.getCameraCharacteristics(String.valueOf(mCameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (cameraInfo.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT) {//front camera
            return true;
        }

        return false;
    }

    //////////CameraControllerInterFaceCallback////////////////
    public interface CameraControllerInterFaceCallback {
        void onThumbnailCreated(Bitmap bitmap);

        void onTakePictureFinished();

       // void onTapFocusFinish();
    }

    private CameraControllerInterFaceCallback mCameraCallback;
    public void setCameraControllerInterFaceCallback(CameraControllerInterFaceCallback cameraCallback) {
        mCameraCallback = cameraCallback;
    }
    //////////////////////////
    public void prepareCaptureStillPicture() {
        beginCaptureStillPicture();
    }

    /*拿照片比例？*/
    public int getJpegRotation(int cameraId, int orientation) {
        int rotation = 0;
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            orientation = 0;
        }
        if (cameraId == -1) {
            cameraId = 0;
        }
        CameraCharacteristics cameraInfo = null;
        try {
            cameraInfo = manager.getCameraCharacteristics(String.valueOf(cameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (cameraInfo.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT) {//front camera
            rotation = (mSensorOrientation - orientation + 360) % 360;//choosePreviewAndCaptureSize中 mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        } else {// back-facing camera
            rotation = (mSensorOrientation + orientation + 360) % 360;
        }
        return rotation;
    }
    ////////////////切换///////////
    /*切换前后置
    * 关相机
    * 更新摄像头id
    * 开相机*/
    public void changeCameraId() {
        closeCamera();
        updateCameraId();//0 1
        openCamera();
    }
    /*
    * 拿到当前Characteristics
    * 获取相机相对于屏幕设备的方向
    * 如果在前，则换成后*/
    private void updateCameraId() {
        try {
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(String.valueOf(mCameraId));


            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                mCameraId = BACK_CAMERA_ID;
            } else {
                mCameraId = FRONT_CAMERA_ID;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
