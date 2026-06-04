package top.zw.passwd;

import android.util.Log;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;

/**
 * 旧数据库自动迁移工具
 * 检测旧项目的加密数据库并迁移到新项目
 * 通过直接复制 + SQLCipher ATTACH 方式确保数据完整性
 */
public class DataMigrator {

    private static final String TAG = "DataMigrator";

    /**
     * 迁移状态
     */
    public enum MigrationStatus {
        NOT_NEEDED,     // 无需迁移（旧数据库不存在或新数据库已有数据）
        SUCCESS,        // 迁移成功
        FAILED          // 迁移失败
    }

    /**
     * 执行数据库迁移
     * 检查旧数据库是否存在，若存在且新数据库为空则执行迁移
     */
    public static MigrationStatus migrateIfNeeded() {
        String oldDbPath = MyApplication.getOldDatabasePath();
        String newDbPath = MyApplication.getDatabasePath();
        String dbPassword = MyApplication.getDbPassword();

        File oldDbFile = new File(oldDbPath);
        File newDbFile = new File(newDbPath);

        // 检查旧数据库是否存在
        if (!oldDbFile.exists() || oldDbFile.length() == 0) {
            Log.i(TAG, "Old database not found at: " + oldDbPath + " — no migration needed");
            return MigrationStatus.NOT_NEEDED;
        }

        // 检查新数据库是否已有数据
        if (newDbFile.exists() && newDbFile.length() > 0) {
            Log.i(TAG, "New database already exists — skipping migration");
            return MigrationStatus.NOT_NEEDED;
        }

        Log.i(TAG, "Starting migration from: " + oldDbPath + " → " + newDbPath);

        try {
            // 先加载 native 库
            SQLiteDatabase.loadLibs(MyApplication.getContext());

            // 方案：ATTACH 旧数据库 → 复制表到新数据库
            // 确保新数据库目录存在
            File parentDir = newDbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 打开新数据库（会自动创建）
            SQLiteDatabase newDb = SQLiteDatabase.openOrCreateDatabase(newDbFile, dbPassword, null);

            try {
                // ATTACH 旧数据库（使用相同密码）
                // rawExecSQL 只接受单个 String 参数，改用 execSQL（支持参数绑定）
                String attachSql = "ATTACH DATABASE ? AS old_db KEY ?";
                newDb.execSQL(attachSql, new Object[]{oldDbPath, dbPassword});

                // 检查旧表是否存在
                Cursor cursor = newDb.rawQuery(
                        "SELECT name FROM old_db.sqlite_master WHERE type='table' AND name='password_tb'",
                        null);
                boolean oldTableExists = cursor != null && cursor.getCount() > 0;
                if (cursor != null) cursor.close();

                if (oldTableExists) {
                    // 在新数据库中创建相同结构的表
                    newDb.execSQL(
                            "CREATE TABLE IF NOT EXISTS password_tb (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "title TEXT, " +
                            "regAddr TEXT, " +
                            "loginUser TEXT, " +
                            "loginPwd TEXT, " +
                            "loginMail TEXT, " +
                            "loginPhone TEXT, " +
                            "remarks TEXT, " +
                            "idTime TEXT)");

                    // 复制数据
                    newDb.execSQL(
                            "INSERT INTO main.password_tb SELECT * FROM old_db.password_tb");

                    // 获取迁移行数
                    Cursor countCursor = newDb.rawQuery(
                            "SELECT COUNT(*) FROM main.password_tb", null);
                    int count = 0;
                    if (countCursor != null) {
                        try {
                            if (countCursor.moveToFirst()) {
                                count = countCursor.getInt(0);
                            }
                        } finally {
                            countCursor.close();
                        }
                    }

                    Log.i(TAG, "Migration completed: " + count + " records transferred");
                } else {
                    Log.w(TAG, "Old database has no password_tb table — skipping data copy");
                }

                // DETACH
                newDb.execSQL("DETACH DATABASE old_db");

            } finally {
                newDb.close();
            }

            return MigrationStatus.SUCCESS;

        } catch (Exception e) {
            Log.e(TAG, "Migration failed: " + e.getMessage(), e);
            // 失败时删除可能部分创建的新数据库
            if (newDbFile.exists()) {
                newDbFile.delete();
            }
            return MigrationStatus.FAILED;
        }
    }

    /**
     * 检查是否需要迁移
     */
    public static boolean isMigrationNeeded() {
        File oldDb = new File(MyApplication.getOldDatabasePath());
        File newDb = new File(MyApplication.getDatabasePath());
        return oldDb.exists() && oldDb.length() > 0 &&
                (!newDb.exists() || newDb.length() == 0);
    }
}