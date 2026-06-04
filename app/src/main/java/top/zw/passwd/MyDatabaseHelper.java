package top.zw.passwd;

import android.content.ContentValues;
import android.util.Log;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;

/**
 * SQLCipher 加密数据库封装
 * 使用参数化查询，消除 SQL 注入风险
 * 表结构与原项目完全兼容
 */
public class MyDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final String DB_NAME = "passwd.db";
    private static final int DB_VERSION = 2;

    // 表名
    static final String TABLE_PASSWORD = "password_tb";

    // 列名
    static final String COL_ID = "_id";
    static final String COL_TITLE = "title";
    static final String COL_REG_ADDR = "regAddr";
    static final String COL_LOGIN_USER = "loginUser";
    static final String COL_LOGIN_PWD = "loginPwd";
    static final String COL_LOGIN_MAIL = "loginMail";
    static final String COL_LOGIN_PHONE = "loginPhone";
    static final String COL_REMARKS = "remarks";
    static final String COL_ID_TIME = "idTime";
    static final String COL_UPDATE_TIME = "updateTime";

    /**
     * 建表 SQL —— 与原项目完全一致
     */
    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_PASSWORD + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_TITLE + " TEXT, " +
                    COL_REG_ADDR + " TEXT, " +
                    COL_LOGIN_USER + " TEXT, " +
                    COL_LOGIN_PWD + " TEXT, " +
                    COL_LOGIN_MAIL + " TEXT, " +
                    COL_LOGIN_PHONE + " TEXT, " +
                    COL_REMARKS + " TEXT, " +
                    COL_ID_TIME + " TEXT, " +
                    COL_UPDATE_TIME + " TEXT)";

    private static MyDatabaseHelper instance;

    /**
     * 获取单例实例
     */
    public static synchronized MyDatabaseHelper getInstance() {
        if (instance == null) {
            instance = new MyDatabaseHelper();
        }
        return instance;
    }

    private MyDatabaseHelper() {
        super(MyApplication.getContext(), MyApplication.getDatabasePath(), null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        Log.i(TAG, "Database created: " + MyApplication.getDatabasePath());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Database upgrade from " + oldVersion + " to " + newVersion);
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_PASSWORD + " ADD COLUMN " + COL_UPDATE_TIME + " TEXT");
                Log.i(TAG, "v1 → v2: Added " + COL_UPDATE_TIME + " column");
            } catch (Exception e) {
                Log.w(TAG, "Column " + COL_UPDATE_TIME + " may already exist", e);
            }
        }
    }

    /**
     * 获取可写数据库（带密码）
     */
    public SQLiteDatabase getWritable() {
        return getWritableDatabase(MyApplication.getDbPassword());
    }

    /**
     * 获取只读数据库（带密码）
     */
    public SQLiteDatabase getReadable() {
        return getReadableDatabase(MyApplication.getDbPassword());
    }

    // ========== 参数化查询辅助方法 ==========

    /**
     * 参数化插入 —— 使用 ContentValues 自动绑定参数
     */
    public long insertPassword(ContentValues values) {
        SQLiteDatabase db = getWritable();
        return db.insert(TABLE_PASSWORD, null, values);
    }

    /**
     * 参数化更新
     * @param values 要更新的字段
     * @param whereClause WHERE 子句，使用 ? 占位符
     * @param whereArgs 占位符参数值
     */
    public int updatePassword(ContentValues values, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = getWritable();
        return db.update(TABLE_PASSWORD, values, whereClause, whereArgs);
    }

    /**
     * 参数化删除
     */
    public int deletePassword(String whereClause, String[] whereArgs) {
        SQLiteDatabase db = getWritable();
        return db.delete(TABLE_PASSWORD, whereClause, whereArgs);
    }

    /**
     * 参数化查询
     * @param columns 要返回的列
     * @param selection WHERE 子句，使用 ? 占位符
     * @param selectionArgs 占位符参数值
     * @param orderBy 排序
     */
    public Cursor queryPassword(String[] columns, String selection,
                                String[] selectionArgs, String orderBy) {
        SQLiteDatabase db = getReadable();
        return (Cursor) db.query(TABLE_PASSWORD, columns, selection, selectionArgs,
                null, null, orderBy);
    }

    /**
     * 参数化原始查询
     * @param sql SQL 语句，使用 ? 占位符
     * @param selectionArgs 占位符参数值
     */
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        SQLiteDatabase db = getReadable();
        return (Cursor) db.rawQuery(sql, selectionArgs);
    }

    /**
     * 执行参数化写操作 SQL
     */
    public void execSQL(String sql, Object[] bindArgs) {
        SQLiteDatabase db = getWritable();
        if (bindArgs != null && bindArgs.length > 0) {
            db.execSQL(sql, bindArgs);
        } else {
            db.execSQL(sql);
        }
    }

    /**
     * 获取记录总数
     */
    public long getRecordCount() {
        Cursor cursor = rawQuery("SELECT COUNT(*) FROM " + TABLE_PASSWORD, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * 检查数据库文件是否存在
     */
    public static boolean databaseFileExists() {
        File dbFile = new File(MyApplication.getDatabasePath());
        return dbFile.exists() && dbFile.length() > 0;
    }
}
