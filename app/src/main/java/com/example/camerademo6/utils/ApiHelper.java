package com.example.camerademo6.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.lang.reflect.Field;

public class ApiHelper {
    /**
     * module  TW_APP_SnapdragonCamera
     * author  zhaoxuan
     * date  2019/7/2
     * description 屏幕模式 0代表全屏  1代表分屏
     */
    public static final int SCREEN_MODE_FULL = 0;
    public static final int SCREEN_MODE_SPLIT = 1;
    /**
     * module  TW_APP_SnapdragonCamera
     * author  zhaoxuan
     * date  2019/7/2
     * description 屏幕状态 0代表主屏  1代表辅屏
     */
    public static final int SCREEN_ACTIVE_MAIN = 0;
    public static final int SCREEN_ACTIVE_AUXILIARY = 1;

    /**
     * module  TW_APP_SnapdragonCamera
     * author  zhaoxuan
     * date  2019/7/3
     * description 相机照片分辨率参数
     */
    public static final int RESOLVING_POWER_SIXTEEN_WIDTH = 4640;
    public static final int RESOLVING_POWER_SIXTEEN_HEIGHT = 3488;
    public static final int RESOLVING_POWER_TWELVE_WIDTH = 4608;
    public static final int RESOLVING_POWER_TWELVE_HEIGHT = 2592;
    public static final int RESOLVING_POWER_NINE_WIDTH_AND_HEIGHT = 2976;
    public static final int RESOLVING_POWER_EIGHT_ONE_WIDTH = 3840;
    public static final int RESOLVING_POWER_EIGHT_ONE_HEIGHT = 2160;
    public static final int RESOLVING_POWER_EIGHT_TWO_WIDTH = 3200;
    public static final int RESOLVING_POWER_EIGHT_TWO_HEIGHT = 2400;

    /**
     * module  TW_APP_SnapdragonCamera
     * author  zhaoxuan
     * date  2019/7/10
     * description 折叠主屏的屏幕宽度
     */
    public static final int MAIN_SCREEN_WIDTH = 810;

    /**
     * module  TW_APP_SnapdragonCamera
     * author  zhaoxuan
     * date  2019/7/11
     * description 180是主屏情况4:3比例下，取景框距离屏幕底部的距离 48是底部导航栏高度 单位都是dp
     */
    public static final int BOTTOM_HEIGHT = 180;
    public static final int BOTTOM_NAVIGATION_BAR_HEIGHT = 48;

    public static int getIntFieldIfExists(Class<?> klass, String fieldName,
                                          Class<?> obj, int defaultVal) {
        try {
            Field f = klass.getDeclaredField(fieldName);
            return f.getInt(obj);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public static boolean isKitKatOrHigher() {
        // TODO: Remove CODENAME check as soon as VERSION_CODES.KITKAT is final.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                || "KeyLimePie".equals(Build.VERSION.CODENAME);
    }

    public static boolean isAndroidPOrHigher() {
        return Build.VERSION.SDK_INT >= 28;
    }

    /**
     * 获取屏幕模式
     * @param context 上下文对象
     * @return 0-全屏 1-分屏
     */
    public static int getScreenMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "y_screen_mode", SCREEN_MODE_FULL);
    }

    /**
     * 获取屏幕激活部分
     * @param context 上下文对象
     * @return 0-主屏 1-辅屏
     */
    public static int getScreenActive(Context context){
        return Settings.System.getInt(context.getContentResolver(), "y_screen_active", SCREEN_ACTIVE_MAIN);
    }
}
