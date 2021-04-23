package com.example.camerademo6.storage;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/*
* SharedPreferences时使用键值对的方式来存储数据的，在保存一条数据时，需要给这条数据提供一个对应的键
* SharePreferences是一个接口，不能直接使用 通过以下两种方法获得
* 1.Context类的getSharePreferences(String name ,ine mode)方法来获得一个SharePreferences对象
*                    第一个参数用于指定SharedPreference文件的名称，即存储XML文件的名称，如果存在，则会直接引用，如果指定的文件不存在则会创建一个，SharedPreference文件都是存放在/data/data/<包名>/shared_prefs/存储的XML文件目录下，第二个参数表示文件的存储模式
*                    该方式创建的XML文件可以被同一个软件的Activity引用
* 2.调用Activity对象的getPreferences(int mode)方法
*                   该方法有一个参数，表示文件的存储模式，
*                   这种方法获得的对象只能被该方法所在的Activity所调用
*
* 还要创建一个SharePreferences.Editor类的对象
*  该类负责具体的写入操作，创建方法使通过SharePreferences类的edit()方法来创建
*
* 得到SharedPreference对象之后，就可以开始向SharedPreference文件中存储数据了，主要有三步：
        a.使用SharedPreference对象的edit（）方法来获取一个SharedPreference.Editor对象
        b.向SharedPreference.Editor对象中添加数据，比如添加一个布尔型数据就是用putBoolean方法，添加一个字符串就是用putString（）方法，以此类推
        c.调用commit（）方法将添加的数据提交，从而完成数据存储操作*/

public class SharedPreferencesController {
    private static final String SP_FILE_NAME = "spfiles";
    private static SharedPreferences mSp;

    private SharedPreferencesController(Context context) {
        if (mSp == null) {
            mSp = context.getSharedPreferences(SP_FILE_NAME, MODE_PRIVATE);//spfiles.xml 私有文佳，该文件只能被创建他的文件所访问
        }
    }

    private static SharedPreferencesController instance = null;

    public synchronized static SharedPreferencesController getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesController(context.getApplicationContext());
        }
        return instance;
    }

    public static void spPutString(String key, String value) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String spGetString(String key) {
        String value = mSp.getString(key, "");
        return value;
    }

    public static void spPutBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean spGetBoolean(String key) {
        boolean value = mSp.getBoolean(key, false);
        return value;
    }
}
