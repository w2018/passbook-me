package top.zw.passwd;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 导入导出工具类
 * 支持两种模式：
 * 1. 加密模式：直接复制 SQLCipher 加密的 .db 文件
 * 2. 明文模式：导出/导入 JSON 格式的明文数据
 */
public class ImportExportUtil {

    private static final String TAG = "ImportExportUtil";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    private ImportExportUtil() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    // ==================== 加密 .db 导出导入 ====================

    /**
     * 导出加密数据库文件到指定目录
     *
     * @return 导出的文件，失败返回 null
     */
    public static File exportEncryptedDb(Context context, File destDir) {
        File sourceDb = new File(MyApplication.getDatabasePath());
        if (!sourceDb.exists() || sourceDb.length() == 0) {
            Log.w(TAG, "Source database does not exist or is empty");
            return null;
        }

        if (!destDir.exists() && !destDir.mkdirs()) {
            Log.e(TAG, "Cannot create destination directory: " + destDir);
            return null;
        }

        String timestamp = DATE_FORMAT.format(new Date());
        File destFile = new File(destDir, "passwd_encrypted_" + timestamp + ".db");

        try {
            copyFile(sourceDb, destFile);
            Log.i(TAG, "Encrypted DB exported to: " + destFile.getAbsolutePath());
            return destFile;
        } catch (IOException e) {
            Log.e(TAG, "Failed to export encrypted DB", e);
            // 清理部分文件
            if (destFile.exists()) destFile.delete();
            return null;
        }
    }

    /**
     * 从加密数据库文件导入
     * 会替换当前数据库，操作前建议先备份
     *
     * @return 导入的记录数，-1 表示失败
     */
    public static int importEncryptedDb(Context context, File sourceFile) {
        if (!sourceFile.exists() || sourceFile.length() == 0) {
            Log.w(TAG, "Source file does not exist or is empty");
            return -1;
        }

        // 验证源文件是否为有效的 SQLCipher 加密数据库
        SQLiteDatabase sourceDb = null;
        int count = -1;
        try {
            sourceDb = SQLiteDatabase.openDatabase(
                    sourceFile.getAbsolutePath(),
                    MyApplication.getDbPassword(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
            );
            // 尝试查询以验证数据库有效性
            count = (int) net.sqlcipher.DatabaseUtils.longForQuery(
                    sourceDb, "SELECT COUNT(*) FROM password_tb", null);
            sourceDb.close();
            sourceDb = null;
        } catch (Exception e) {
            Log.e(TAG, "Invalid encrypted database file", e);
            return -1;
        } finally {
            if (sourceDb != null) {
                try { sourceDb.close(); } catch (Exception ignored) {}
            }
        }

        // 复制源文件到应用数据库路径
        File destDb = new File(MyApplication.getDatabasePath());
        try {
            copyFile(sourceFile, destDb);
            Log.i(TAG, "Encrypted DB imported, records: " + count);
            return count;
        } catch (IOException e) {
            Log.e(TAG, "Failed to import encrypted DB", e);
            return -1;
        }
    }

    // ==================== 明文 JSON 导出导入 ====================

    /**
     * 导出为明文 JSON 文件
     *
     * @return 导出的 JSON 文件，失败返回 null
     */
    public static File exportJson(Context context, File destDir, SqlClass sqlClass) {
        try {
            List<DataInfo> allData = sqlClass.getAll();
            JSONArray jsonArray = new JSONArray();

            for (DataInfo item : allData) {
                JSONObject obj = new JSONObject();
                obj.put("title", nullToEmpty(item.getTitle()));
                obj.put("regAddr", nullToEmpty(item.getRegAddr()));
                obj.put("loginUser", nullToEmpty(item.getLoginUser()));
                obj.put("loginPwd", nullToEmpty(item.getLoginPwd()));
                obj.put("loginMail", nullToEmpty(item.getLoginMail()));
                obj.put("loginPhone", nullToEmpty(item.getLoginPhone()));
                obj.put("remarks", nullToEmpty(item.getRemarks()));
                obj.put("idTime", nullToEmpty(item.getIdTime()));
                jsonArray.put(obj);
            }

            if (!destDir.exists() && !destDir.mkdirs()) {
                Log.e(TAG, "Cannot create destination directory");
                return null;
            }

            String timestamp = DATE_FORMAT.format(new Date());
            File jsonFile = new File(destDir, "passwd_plain_" + timestamp + ".json");

            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(jsonFile), "UTF-8")) {
                writer.write(jsonArray.toString(2));
                writer.flush();
            }

            Log.i(TAG, "JSON exported to: " + jsonFile.getAbsolutePath()
                    + ", records: " + allData.size());
            return jsonFile;

        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to export JSON", e);
            return null;
        }
    }

    /**
     * 从明文 JSON 文件导入
     *
     * @return 导入的记录数，-1 表示失败
     */
    public static int importJson(Context context, File jsonFile, SqlClass sqlClass) {
        if (!jsonFile.exists() || jsonFile.length() == 0) {
            Log.w(TAG, "JSON file does not exist or is empty");
            return -1;
        }

        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(jsonFile), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONArray jsonArray = new JSONArray(sb.toString());
            int imported = 0;

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                DataInfo info = new DataInfo();
                info.setTitle(obj.optString("title", ""));
                info.setRegAddr(obj.optString("regAddr", ""));
                info.setLoginUser(obj.optString("loginUser", ""));
                info.setLoginPwd(obj.optString("loginPwd", ""));
                info.setLoginMail(obj.optString("loginMail", ""));
                info.setLoginPhone(obj.optString("loginPhone", ""));
                info.setRemarks(obj.optString("remarks", ""));
                info.setIdTime(obj.optString("idTime", ""));

                long id = sqlClass.insert(info);
                if (id > 0) {
                    imported++;
                }
            }

            Log.i(TAG, "JSON imported, records: " + imported);
            return imported;

        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to import JSON", e);
            return -1;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 使用 FileChannel 高效复制文件
     */
    private static void copyFile(File source, File dest) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            long size = sourceChannel.size();
            long transferred = 0;
            while (transferred < size) {
                transferred += sourceChannel.transferTo(transferred,
                        size - transferred, destChannel);
            }
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}