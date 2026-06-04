package top.zw.passwd;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * FTP 客户端工具类
 * 使用 EncryptedSharedPreferences 持久化服务器配置
 * 基于 Apache Commons Net 3.6
 */
public class FTPClientUtil {

    private static final String TAG = "FTPClientUtil";
    private static final String PREFS_NAME = "ftp_secure_prefs";

    // EncryptedSharedPreferences 密钥
    private static final String KEY_HOST = "ftp_host";
    private static final String KEY_PORT = "ftp_port";
    private static final String KEY_USER = "ftp_user";
    private static final String KEY_PASSWORD = "ftp_password";
    private static final String KEY_REMOTE_DIR = "ftp_remote_dir";

    // 默认值
    private static final String DEFAULT_HOST = "";
    private static final int DEFAULT_PORT = 21;
    private static final String DEFAULT_USER = "anonymous";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_REMOTE_DIR = "/";

    private final FTPClient ftpClient;
    private static EncryptedSharedPreferences securePrefs;

    public FTPClientUtil(Context context) {
        this.ftpClient = new FTPClient();
        ftpClient.setControlEncoding("UTF-8");
        initSecurePrefs(context);
    }

    // ==================== 配置管理 ====================

    private static synchronized void initSecurePrefs(Context context) {
        if (securePrefs == null) {
            try {
                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                securePrefs = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                        PREFS_NAME,
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException e) {
                Log.e(TAG, "Failed to init EncryptedSharedPreferences", e);
            }
        }
    }

    public static void saveFtpConfig(Context context, String host, int port,
                                     String user, String password, String remoteDir) {
        initSecurePrefs(context);
        if (securePrefs == null) {
            // 降级到普通 SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_HOST, host)
                    .putInt(KEY_PORT, port)
                    .putString(KEY_USER, user)
                    .putString(KEY_PASSWORD, password)
                    .putString(KEY_REMOTE_DIR, remoteDir)
                    .apply();
            return;
        }
        securePrefs.edit()
                .putString(KEY_HOST, host)
                .putInt(KEY_PORT, port)
                .putString(KEY_USER, user)
                .putString(KEY_PASSWORD, password)
                .putString(KEY_REMOTE_DIR, remoteDir)
                .apply();
    }

    public static FTPConfig getFtpConfig(Context context) {
        initSecurePrefs(context);
        if (securePrefs != null) {
            return new FTPConfig(
                    securePrefs.getString(KEY_HOST, DEFAULT_HOST),
                    securePrefs.getInt(KEY_PORT, DEFAULT_PORT),
                    securePrefs.getString(KEY_USER, DEFAULT_USER),
                    securePrefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD),
                    securePrefs.getString(KEY_REMOTE_DIR, DEFAULT_REMOTE_DIR)
            );
        }
        // 降级
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new FTPConfig(
                prefs.getString(KEY_HOST, DEFAULT_HOST),
                prefs.getInt(KEY_PORT, DEFAULT_PORT),
                prefs.getString(KEY_USER, DEFAULT_USER),
                prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD),
                prefs.getString(KEY_REMOTE_DIR, DEFAULT_REMOTE_DIR)
        );
    }

    public static boolean isConfigured(Context context) {
        FTPConfig config = getFtpConfig(context);
        return config.host != null && !config.host.isEmpty();
    }

    public static void clearFtpConfig(Context context) {
        initSecurePrefs(context);
        if (securePrefs != null) {
            securePrefs.edit()
                    .remove(KEY_HOST)
                    .remove(KEY_PORT)
                    .remove(KEY_USER)
                    .remove(KEY_PASSWORD)
                    .remove(KEY_REMOTE_DIR)
                    .apply();
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_HOST)
                .remove(KEY_PORT)
                .remove(KEY_USER)
                .remove(KEY_PASSWORD)
                .remove(KEY_REMOTE_DIR)
                .apply();
    }

    // ==================== 连接管理 ====================

    /**
     * 连接到 FTP 服务器
     *
     * @return true 连接成功
     */
    public boolean connect(String host, int port, String user, String password) throws IOException {
        ftpClient.setConnectTimeout(10000);
        ftpClient.setDataTimeout(30000);
        ftpClient.connect(host, port);

        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            return false;
        }

        if (!ftpClient.login(user, password)) {
            ftpClient.disconnect();
            return false;
        }

        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();
        return true;
    }

    /**
     * 使用已保存配置连接
     */
    public boolean connect(Context context) throws IOException {
        FTPConfig config = getFtpConfig(context);
        return connect(config.host, config.port, config.user, config.password);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            Log.w(TAG, "Error disconnecting", e);
        }
    }

    public boolean isConnected() {
        return ftpClient.isConnected();
    }

    // ==================== 文件操作 ====================

    /**
     * 上传文件到 FTP 服务器
     */
    public boolean upload(File localFile, String remoteFileName) throws IOException {
        if (!ftpClient.isConnected()) return false;

        try (FileInputStream fis = new FileInputStream(localFile)) {
            return ftpClient.storeFile(remoteFileName, fis);
        }
    }

    /**
     * 从 FTP 服务器下载文件
     */
    public boolean download(String remoteFileName, File localFile) throws IOException {
        if (!ftpClient.isConnected()) return false;

        try (FileOutputStream fos = new FileOutputStream(localFile)) {
            return ftpClient.retrieveFile(remoteFileName, fos);
        }
    }

    /**
     * 删除 FTP 服务器上的文件
     */
    public boolean deleteRemote(String remoteFileName) throws IOException {
        if (!ftpClient.isConnected()) return false;
        return ftpClient.deleteFile(remoteFileName);
    }

    /**
     * 列出远程目录文件
     */
    public String[] listFiles(String remoteDir) throws IOException {
        if (!ftpClient.isConnected()) return null;
        return ftpClient.listNames(remoteDir);
    }

    /**
     * 切换远程工作目录
     */
    public boolean changeDirectory(String remoteDir) throws IOException {
        if (!ftpClient.isConnected()) return false;
        return ftpClient.changeWorkingDirectory(remoteDir);
    }

    // ==================== 数据类 ====================

    public static class FTPConfig {
        public final String host;
        public final int port;
        public final String user;
        public final String password;
        public final String remoteDir;

        public FTPConfig(String host, int port, String user, String password, String remoteDir) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
            this.remoteDir = remoteDir;
        }
    }
}