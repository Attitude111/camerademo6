package com.example.camerademo6.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.camerademo6.R;
import com.example.camerademo6.cam.CameraConstant;
import com.example.camerademo6.cam.CameraController;
import com.example.camerademo6.utils.DensityUtils;
import com.example.camerademo6.view.AutoFitTextureView;
import com.example.camerademo6.view.FocusView;
import com.example.camerademo6.view.HorizontalScrollPickView;
import com.example.camerademo6.view.ModuleSwitcherAdapter;
import com.example.camerademo6.view.RoundImageView;
import com.example.camerademo6.view.ShutterButton;
import com.example.camerademo6.view.TwoStateSwitchLayout;

import java.io.File;
import java.lang.ref.WeakReference;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements View.OnClickListener, CameraController.CameraControllerInterFaceCallback, TwoStateSwitchLayout.CustomCheckBoxChangeListener, ShutterButton.OnShutterButtonClickLister{


    private Handler mHandler;
    private Handler mTimeHandler;
    //private int cameratime=3;
    public class MyHandler extends Handler {
        WeakReference<Activity> mWeakReference;//通过弱引用方式引用外部类

        public MyHandler(Activity activity) {
            mWeakReference = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final Activity activity = mWeakReference.get();
            if (activity != null) {

                switch (msg.what) {
                    case HIDE_FOCUS_VIEW:
                        Log.i("handlemessage","处理消息 hide_focus_view");
                        mFocusView.setNeedToDrawView(false);//设置focusview不需要draw并刷新
                        tv_time.setVisibility(View.GONE);
                        break;
                    case 3:
                        tv_time.setVisibility(View.VISIBLE);
                        Log.i("handlemessage","当前消息：3");
                        tv_time.setText("3");
                        break;
                    case 2:
                        Log.i("handlemessage","当前消息：2");
                        tv_time.setText("2");
                        break;
                    case 1:
                        Log.i("handlemessage","当前消息：1");
                        tv_time.setText("1");
                        break;
                    case 0:
                        Log.i("handlemessage","当前消息：0");
                        tv_time.setVisibility(View.GONE);
                        //tv_time.setText("");
                        break;

                }

            }
        }
    }
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {

            mCameraController.openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };


    private AutoFitTextureView mPreviewTexture;
    private HorizontalScrollPickView mPickModeView;
    private String[] modeName = {"大光圈","夜景","人像","拍照", "录像", "专业", "更多"};
    private int mCurrentMode = CameraConstant.PHOTO_MODE;//当前模式，默认拍照模式
    private int mCurrentModeIndex;//当前模式对应下标

    private ShutterButton mTakePicture;
    private ImageView mSetting;

    private ImageView mChangeCameraId;
    private FocusView mFocusView;
    private TwoStateSwitchLayout mFlashSwitch;
    private TwoStateSwitchLayout mRatioSwitch;
    private RoundImageView mGoToGallery;
   private TwoStateSwitchLayout mTimeCount;


    private CameraController mCameraController;
    private File mFile;

    private int mPhoneOrientation;
    private MyOrientationEventListener mOrientationListener;
    public static final int ORIENTATION_HYSTERESIS = 5;
    private static final int HIDE_FOCUS_VIEW = 10;//隐藏focusview
    private static final int Time_COUNT3_BEGIN= 3;//开始计时拍照

    private boolean mRecording;

    /*实现*/
    @Override
    public void customCheckBoxOn(int flashSwitch) {
        switch (flashSwitch) {
            case R.id.flash_switch:
                openFlashMode();
                break;
            case R.id.ratio_switch:
                changToSixTeenRatioNine();
                break;
            case R.id.time_count:
                openTimeCount();
        }
    }

    @Override
    public void customCheckBoxOff(int flashSwitch) {
        switch (flashSwitch) {
            case R.id.flash_switch:
                closeFlashMode();
                break;
            case R.id.ratio_switch:
                changeToFourRatioThird();
                break;
            case R.id.time_count:
                closeTimeCount();
        }
    }

    @Override
    public void onShutterButtonClick(int mode) {
        switch (mode) {
            case CameraConstant.PHOTO_MODE:
            case CameraConstant.PRO_MODE:
                takePicture();
                break;
            case CameraConstant.VIDEO_MODE:
            case CameraConstant.SLOW_FPS_MODE:
                takeVideo();
                break;
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.settings:
                goToSettingsActivity();
                break;
            case R.id.change_camera_id:
                changeCameraId();
                break;
            case R.id.iv_goto_gallery:
                gotoGallery();
                break;
        }
    }
    @Override
    public void onThumbnailCreated(final Bitmap bitmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGoToGallery.setBitmap(bitmap);
            }//////////拍下来的那一种缩略图方向显示翻转..图库对
        });
    }
    public void onTakePictureFinished() {
        //LogUtils.logD("onTakePictureFinished");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTakePicture.setEnabled(true);
            }
        });
    }

    @Override
    public void onTapFocusFinish() {

        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessage(HIDE_FOCUS_VIEW);
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i("mainactivity_onCreate","requestFullScreenActivity");
        requestFullScreenActivity();
        Log.i("mainactivity_onCreate","setContentView");
        setContentView(R.layout.activity_main);
        Log.i("mainactivity_onCreate","initView");
        initView();
        Log.i("mainactivity_onCreate","registerOrientationLister");
        registerOrientationLister();//注册手机方向的监听：实例化

        initTextureViewListener(); //初始化textureview的监听 focus有关
    }


    private TextView tv_time;
    private void initView() {

        tv_time=findViewById(R.id.show_time);


        mHandler = new MyHandler(this);

        //tv_time=findViewById(R.id.tv_time);
        mPreviewTexture = findViewById(R.id.preview_texture);
        /*
        * 模式选择view的初始化
        *                 找到对应view
        *                 设适配器ModuleSwitcherAdapter：初始化模式选择的view
        *                  设置默认选择的模式下标：模式列长的二分之一-->选中间
        *                  设置选择监听（自定义方法）：实现接口中的方法 用户选择模式后调用*/
        mPickModeView = findViewById(R.id.pick_mode_view);
        mPickModeView.setAdapter(new ModuleSwitcherAdapter(this, modeName));
        mPickModeView.setDefaultSelectedIndex(modeName.length / 2);
        mPickModeView.setSelectListener(new HorizontalScrollPickView.SelectListener() {      //////////////////////////？？？？？？？？？？？？？？？
            @Override
            public void onSelect(int beforePosition, int position) {
                mCurrentModeIndex = position;
                changeMode(position);//根据选择的对应下标更改模式
            }
        });
        mPreviewTexture.setAlpha(0.5f);
        /*
        * 找到快门view
        * 找到设置view
        * 找到前后置切换view
        * 找到对焦view
        * 找到闪光灯view
        * 找到比例view
        * 找到缩略图view*/
        mTakePicture = findViewById(R.id.take_picture);
        mSetting = findViewById(R.id.settings);
        mChangeCameraId = findViewById(R.id.change_camera_id);
        mFocusView = findViewById(R.id.fv_focus);
        mFlashSwitch = findViewById(R.id.flash_switch);
        mRatioSwitch = findViewById(R.id.ratio_switch);
        mGoToGallery = findViewById(R.id.iv_goto_gallery);

        mTimeCount=findViewById(R.id.time_count);

        /*
        *OnClickListener
        * 为倒计时imageview设置监听
        *  为设置view设监听
        * 为前后置前后设监听
        * 为缩略图设监听
        * CustomCheckBoxChangeListener
        * 为闪光灯实现监听
        * 为比例实现监听
        * OnShutterButtonClickListener
        * 为快门实现监听
        */

        mSetting.setOnClickListener(this);
        mChangeCameraId.setOnClickListener(this);
        mGoToGallery.setOnClickListener(this);
        mFlashSwitch.setCustomCheckBoxChangeListener(this);
        mRatioSwitch.setCustomCheckBoxChangeListener(this);
        mTakePicture.setOnShutterButtonClickListener(this);

        mTimeCount.setCustomCheckBoxChangeListener(this);


        /*
        * 为缩略图view设背景---形状
        * 为前后置view设图片*/
        mGoToGallery.setBackground(getDrawable(R.drawable.drawable_shape));
        mChangeCameraId.setImageResource(R.mipmap.change_id);

        /*
        * 实例化控制器，设置回调*/
        mCameraController = new CameraController(this, mPreviewTexture);
        mCameraController.setCameraControllerInterFaceCallback(this);
    }
    /*
    * 为预览view注册touch事件
    *   down-move-up:
    * up动作：若满足x、y
    * 设置focusView需要去draw
    * 传递点击位置设置draw的位置
    * */
    private void initTextureViewListener() {
        mPreviewTexture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        int currentY = (int) event.getRawY();

                        int left = mPreviewTexture.getLeft();
                        int top = mPreviewTexture.getTop();

                        int focusViewWidth = DensityUtils.dip2px(MainActivity.this, FocusView.mOuterRadiusDP);

                        int minDrawY = top + focusViewWidth / 2;
                        int maxDrawY = top + mPreviewTexture.getHeight() - focusViewWidth / 2;
                        if (currentY <= minDrawY || currentY >= maxDrawY) return false;

                        int currentX = (int) event.getRawX();
                        int minDrawX = left + focusViewWidth / 2;
                        int maxDrawX = left + mPreviewTexture.getWidth() - focusViewWidth / 2;

                        if (currentX <= minDrawX || currentX >= maxDrawX) return false;

                        mFocusView.setNeedToDrawView(true);
                        mFocusView.setFocusViewCenter(currentX, currentY);

                        mCameraController.updateManualFocus(mCameraController.getFocusRect(currentX, currentY));
                        mFocusView.playAnimation();
                        if (mHandler != null) {
                            mHandler.removeCallbacksAndMessages(null);
                            mHandler.sendEmptyMessageDelayed(HIDE_FOCUS_VIEW, 3000);
                        }
                        break;
                }
                return true;
            }
        });
    }

    /*
    *CameraController.更新
    * 更新缩略图view
    * focusview默认设置不需要去draw
    *使方向监听可用-->类似注册
    * 自定义预览view可用时调用cameracontroller的opencamera
    * 否则为自定义view设置监听*/
    @Override
    protected void onResume() {
        super.onResume();
        //LogUtils.logD("onResume start");
        mCameraController.startBackgroundThread();
        updateThumbnailView();

        if (mFocusView != null) {
            mFocusView.setNeedToDrawView(false);
        }
        initOrientationSensor();
        if (mPreviewTexture.isAvailable()) {
            mCameraController.openCamera();
        } else {
            mPreviewTexture.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        //LogUtils.logD("onResume end");
    }


    @Override
    public void onPause() {
        super.onPause();
        //LogUtils.logD("onPause start");
        mOrientationListener.disable();//禁用方向监听
        mCameraController.closeCamera();
        mCameraController.closeMediaRecorder();
        mCameraController.stopBackgroundThread();
        if(mFocusView != null){
            //停止时需要将focusview设置不需要draw
            mFocusView.setNeedToDrawView(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);//Looper线程的消息队列中可能还有待处理的消息，记住要移除消息队列中待处理的消息
        }
        super.onDestroy();
    }

    /*选择要变更的模式*/
    private void changeMode(int position) {
        switch (modeName[position]) {
            case "拍照":
                changeToPictureMode();
                break;
            case "录像":
                changeToVideoMode();
                break;
            case "专业":
                changeToProfessorMode();
                break;
            case "慢动作":
                //changeToSlowFpsMode();
                break;
            case "更多":
                //changeToMoreMode();
                break;
        }
    }

    /*拍照模式
    * 如果本身是拍照模式，do nothing
    * 当前模式更新为拍照模式：mainactivity中的模式值、快门按钮的模式值、cameraController的模式值
    * 此时 设置、快门、比例切换的view可见
    *
    * 重新启预览：关相机、若有mediaRecorder同样关闭、设模式、设比例、重新开相机
    * */
    private void changeToPictureMode() {
        if (mCurrentMode == CameraConstant.PHOTO_MODE) return;
        mCurrentMode = CameraConstant.PHOTO_MODE;
        mTakePicture.setCurrentMode(mCurrentMode);

        mSetting.setVisibility(View.VISIBLE);
        mFlashSwitch.setVisibility(View.VISIBLE);
        mRatioSwitch.setVisibility(View.VISIBLE);




        mCameraController.closeCamera();
        mCameraController.closeMediaRecorder();
        mCameraController.setCurrentMode(mCurrentMode);
        mCameraController.setTargetRatio(CameraConstant.RATIO_FOUR_THREE);
        mCameraController.openCamera();
    }
    /*录像模式
     * 如果本身是录像模式，do nothing
     * 当前模式更新为录像模式：mainactivity中的模式值、快门按钮的模式值、cameraController的模式值
     * 此时 设置、快门、比例切换的view不可见
     *将录像的状态置为false
     * 重新启预览：关相机、、设模式、设比例、重新开相机
     * */
    private void changeToVideoMode() {
        if (mCurrentMode == CameraConstant.VIDEO_MODE) return;

        mCurrentMode = CameraConstant.VIDEO_MODE;
        mTakePicture.setCurrentMode(mCurrentMode);

        mSetting.setVisibility(View.GONE);
        mFlashSwitch.setVisibility(View.GONE);
        mRatioSwitch.setVisibility(View.GONE);
        mTakePicture.setVideoRecordingState(false);
        mCameraController.closeCamera();
        mCameraController.setCurrentMode(mCurrentMode);
        mCameraController.setTargetRatio(CameraConstant.RATIO_SIXTEEN_NINE);
        mCameraController.openCamera();
    }
    /*专业模式
    * 如果本身是专业模式，do nothing
    * 当前模式更新为专业模式：mainactivity中的模式值、快门按钮的模式值、cameraController的模式值
    *此时 设置、快门、比例切换的view可见
    * 关相机、关mediaRecorder
    * 设比例
    * 开相机*/
    private void changeToProfessorMode() {
        if (mCurrentMode == CameraConstant.PRO_MODE) return;
        mCurrentMode = CameraConstant.PRO_MODE;
        mTakePicture.setCurrentMode(mCurrentMode);
        mCameraController.setCurrentMode(mCurrentMode);

        mSetting.setVisibility(View.INVISIBLE);
        mFlashSwitch.setVisibility(View.INVISIBLE);
        mRatioSwitch.setVisibility(View.INVISIBLE);


        mCameraController.closeCamera();
        mCameraController.closeMediaRecorder();

        mCameraController.setTargetRatio(CameraConstant.RATIO_FOUR_THREE);
        mCameraController.openCamera();
    }


    /*
     * 更新缩略图
     * 图片uri
     * 视频uri
     * 内容解析者获取并显示 */
    private void updateThumbnailView() {
        Uri targetUrl = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri targetVideoUrl = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver = getContentResolver();
        Cursor imagesCursor = resolver.query(targetUrl, new String[]{
                        MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID}, null, null,
                null);
        if (imagesCursor != null) {
            //表不空
            if (imagesCursor.moveToLast()) {
                long imageId = imagesCursor.getInt(imagesCursor.getColumnIndex(MediaStore.Images.Media._ID));
                String filePathImage = imagesCursor.getString(imagesCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                if (filePathImage.contains("DCIM/Camera") && filePathImage.contains(".jp")) {
                    mFile = new File(filePathImage);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 32;
                    Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(resolver, imageId, MediaStore.Images.Thumbnails.MINI_KIND, options);
                    if (bitmap != null) {
                        mGoToGallery.setBitmap(bitmap);
                        imagesCursor.close();
                        return;
                    }
                }
                imagesCursor.close();
            }
        }
        Cursor videoCursor = resolver.query(targetVideoUrl, new String[]{
                        MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID}, null, null,
                null);
        if (videoCursor != null) {
            if (videoCursor.moveToLast()) {
                long videoId = videoCursor.getInt(videoCursor.getColumnIndex(MediaStore.Video.Media._ID));
                String filePathVideo = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Video.Media.DATA));
                if (filePathVideo.contains("DCIM/Camera") && filePathVideo.contains(".mp4")) {
                    mFile = new File(filePathVideo);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 32;
                    Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(resolver, videoId, MediaStore.Video.Thumbnails.MINI_KIND, options);
                    if (bitmap != null) {
                        mGoToGallery.setBitmap(bitmap);
                        videoCursor.close();
                        return;
                    }
                }
                videoCursor.close();
            }
        }
    }

    /*快门监听：拍照*/
    private void takePicture() {
        mFile = new File(Environment.getExternalStorageDirectory(), "DCIM/Camera/" + System.currentTimeMillis() + ".jpg");
        //LogUtils.logD("mFile=" + mFile.toString());
        mCameraController.setPhotoPath(mFile);//设置相片保存路径
        mCameraController.prepareCaptureStillPicture();//////////////////////？？？拍照
        mTakePicture.startPictureAnimator();
        mTakePicture.setEnabled(false);//使按钮不可点击           在ACTION_DOWN执行完后，后面的一系列action都不会得到执行了
    }

    /*快门监听：录像
    * 如果正在录像：点击则录像标志位置为false,快门按钮的录像状态同样改变，停止录像
    * 如果没有录像：*/
    private void takeVideo() {
        if (mRecording) {
            mRecording = false;
            mTakePicture.setVideoRecordingState(mRecording);
            stopVideoRecording();
        } else {
            mRecording = true;
            mTakePicture.setVideoRecordingState(mRecording);
            startVideoRecording();
        }
    }

    //cameracontroller停止录像
    private void stopVideoRecording() {
        Log.i("1", "停止录像");
        mCameraController.stopVideoRecording();
    }

    //传递保存路径 cameracontroller开始录像
    private void startVideoRecording() {
        Log.i("1", "开始录像");
        mFile = new File(Environment.getExternalStorageDirectory(), "DCIM/Camera/" + System.currentTimeMillis() + ".mp4");
        mCameraController.setPhotoPath(mFile);
        mCameraController.startVideoRecording();
    }

    /*整个activity显示设置*/
    private void requestFullScreenActivity() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /*查看缩略图
    *
    * ？？？？**/
    private void gotoGallery() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        if (null == mFile) return;
        Uri uri = Uri.fromFile(mFile);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/jpeg");
        startActivity(intent);
    }

    /*跳到设置activity*/
    private void goToSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /*切换前后置
    * 撤销监听？？
    * 通过cameraController改变摄像头方向
    * 播放动画*/
    private void changeCameraId() {
        mHandler.removeCallbacksAndMessages(null);
        mCameraController.changeCameraId();
        playChangeIdAnimation();
    }

    /*切换前后置的动画*/
    private void playChangeIdAnimation() {
        /*
        * ObjectAnimator是派生自ValueAnimator的
        *       要想对哪个控件操作，需要监听动画过程，在监听中对控件操作。这样使用起来相比补间动画而言就相对比较麻烦。为了能让动画直接与对应控件相关联，以使我们从监听动画过程中解放出来
        * */
        ObjectAnimator animator = ObjectAnimator.ofFloat(mChangeCameraId, "rotation", 0, 180, 0);
        //第一个参数用于指定这个动画要操作的是哪个控件：前后置切换的imageview 第二个参数用于指定这个动画要操作这个控件的哪个属性：旋转 第三个参数是可变长参数，这个就跟ValueAnimator中的可变长参数的意义一样了，就是指这个属性值是从哪变到哪

        animator.setDuration(500);
        animator.start();
    }

   /*闪光灯开关*/
    private void openFlashMode() {
        mCameraController.openFlashMode();
    }

    private void closeFlashMode() {
        mCameraController.closeFlashMode();
    }
    /*切换比例*/
    public void changeToFourRatioThird() {
        mCameraController.changeToFourRatioThird();
    }

    public void changToSixTeenRatioNine() {
        mCameraController.changToSixTeenRatioNine();
    }

    /*点击拍照计时*/
    public void openTimeCount(){

        mCameraController.setCounting(true,mHandler);
    }
    public void closeTimeCount(){
        mCameraController.setCounting(false,mHandler);
    }

    /*计时开关---3秒*/
    private int time=3;
    /*打开计时：倒计时显示*/



/*方向监听*/
    private class MyOrientationEventListener
            extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }
            mPhoneOrientation = roundOrientation(orientation, mPhoneOrientation);//手机真正方向

            mCameraController.setPhoneDeviceDegree(mPhoneOrientation);//设置拍摄图片方向
        }
    }

    public int roundOrientation(int orientation, int orientationHistory) {//手机真正方向（0,90,180,270）这几种方向的一种
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    private void registerOrientationLister() {
        mOrientationListener = new MyOrientationEventListener(this);//方向监听
    }

    private void initOrientationSensor() {
        mOrientationListener.enable();//开始方向监听
    }
}