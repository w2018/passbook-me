package top.zw.passwd;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;

/**
 * Application 入口
 * 初始化 SQLCipher、数据库路径、SharedPreferences
 */
public class MyApplication extends Application {

    private static final String TAG = "PasswdApp";
    private static final String DB_NAME = "passwd.db";
    private static final String PREFS_NAME = "passwd_prefs";
    private static final String DB_DIR_NAME = "databases";

    private static Context context;
    private static SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 初始化 SQLCipher 库
        SQLiteDatabase.loadLibs(this);

        // 确保数据库目录存在（内部私有目录）
        File dbDir = getDbDir();
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        Log.i(TAG, "Application initialized, db path: " + getDatabasePath());
    }

    public static Context getContext() {
        return context;
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    /**
     * 获取数据库目录: /data/data/top.zw.passwd/databases/
     * 使用应用内部私有目录，避免 Android 12+ 分区存储限制
     */
    public static File getDbDir() {
        return context.getDir(DB_DIR_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取数据库完整路径
     * 内部存储: /data/data/top.zw.passwd/databases/passwd.db
     */
    public static String getDatabasePath() {
        return new File(getDbDir(), DB_NAME).getAbsolutePath();
    }

    /**
     * 获取旧项目数据库路径（用于数据迁移）
     * 旧项目使用旧包名，数据库存储在 sdcard
     */
    public static String getOldDatabasePath() {
        return new File(Environment.getExternalStorageDirectory(), "个人密码薄/passwd.db").getAbsolutePath();
    }

    /**
     * 获取数据库加密密码
     * 优先从系统属性读取（编译时可注入），否则使用默认密码
     */
    public static String getDbPassword() {
        try {
            String prop = System.getProperty("db.password");
            if (prop != null && !prop.isEmpty()) return prop;
        } catch (Exception ignored) {}
        return "passwd2024";
    }

    /**
     * 获取导出目录（sdcard/Download）
     */
    public static File getExportDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download");
        if (!dir.exists()) {
            dir = Environment.getExternalStorageDirectory();
        }
        return dir;
    }
}