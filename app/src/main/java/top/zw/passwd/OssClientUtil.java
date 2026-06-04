package top.zw.passwd;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 阿里云 OSS 客户端工具类
 * 使用 EncryptedSharedPreferences 持久化 OSS 配置
 * 基于 aliyun-oss-android-sdk 2.9.13
 */
public class OssClientUtil {

    private static final String TAG = "OssClientUtil";
    private static final String PREFS_NAME = "oss_secure_prefs";

    // EncryptedSharedPreferences 密钥
    private static final String KEY_ENDPOINT = "oss_endpoint";
    private static final String KEY_BUCKET = "oss_bucket";
    private static final String KEY_ACCESS_KEY_ID = "oss_access_key_id";
    private static final String KEY_ACCESS_KEY_SECRET = "oss_access_key_secret";
    private static final String KEY_REMOTE_DIR = "oss_remote_dir";

    // 默认值
    private static final String DEFAULT_ENDPOINT = "";
    private static final String DEFAULT_BUCKET = "";
    private static final String DEFAULT_ACCESS_KEY_ID = "";
    private static final String DEFAULT_ACCESS_KEY_SECRET = "";
    private static final String DEFAULT_REMOTE_DIR = "backup/";

    private OSS ossClient;
    private static EncryptedSharedPreferences securePrefs;

    public OssClientUtil() {
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
            } catch (GeneralSecurityException | java.io.IOException e) {
                Log.e(TAG, "Failed to init EncryptedSharedPreferences", e);
            }
        }
    }

    public static void saveOssConfig(Context context, String endpoint, String bucket,
                                     String accessKeyId, String accessKeySecret, String remoteDir) {
        initSecurePrefs(context);
        if (securePrefs == null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_ENDPOINT, endpoint)
                    .putString(KEY_BUCKET, bucket)
                    .putString(KEY_ACCESS_KEY_ID, accessKeyId)
                    .putString(KEY_ACCESS_KEY_SECRET, accessKeySecret)
                    .putString(KEY_REMOTE_DIR, remoteDir)
                    .apply();
            return;
        }
        securePrefs.edit()
                .putString(KEY_ENDPOINT, endpoint)
                .putString(KEY_BUCKET, bucket)
                .putString(KEY_ACCESS_KEY_ID, accessKeyId)
                .putString(KEY_ACCESS_KEY_SECRET, accessKeySecret)
                .putString(KEY_REMOTE_DIR, remoteDir)
                .apply();
    }

    public static OssConfig getOssConfig(Context context) {
        initSecurePrefs(context);
        if (securePrefs != null) {
            return new OssConfig(
                    securePrefs.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT),
                    securePrefs.getString(KEY_BUCKET, DEFAULT_BUCKET),
                    securePrefs.getString(KEY_ACCESS_KEY_ID, DEFAULT_ACCESS_KEY_ID),
                    securePrefs.getString(KEY_ACCESS_KEY_SECRET, DEFAULT_ACCESS_KEY_SECRET),
                    securePrefs.getString(KEY_REMOTE_DIR, DEFAULT_REMOTE_DIR)
            );
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new OssConfig(
                prefs.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT),
                prefs.getString(KEY_BUCKET, DEFAULT_BUCKET),
                prefs.getString(KEY_ACCESS_KEY_ID, DEFAULT_ACCESS_KEY_ID),
                prefs.getString(KEY_ACCESS_KEY_SECRET, DEFAULT_ACCESS_KEY_SECRET),
                prefs.getString(KEY_REMOTE_DIR, DEFAULT_REMOTE_DIR)
        );
    }

    public static boolean isConfigured(Context context) {
        OssConfig config = getOssConfig(context);
        return config.endpoint != null && !config.endpoint.isEmpty()
                && config.bucket != null && !config.bucket.isEmpty()
                && config.accessKeyId != null && !config.accessKeyId.isEmpty()
                && config.accessKeySecret != null && !config.accessKeySecret.isEmpty();
    }

    public static void clearOssConfig(Context context) {
        initSecurePrefs(context);
        if (securePrefs != null) {
            securePrefs.edit()
                    .remove(KEY_ENDPOINT)
                    .remove(KEY_BUCKET)
                    .remove(KEY_ACCESS_KEY_ID)
                    .remove(KEY_ACCESS_KEY_SECRET)
                    .remove(KEY_REMOTE_DIR)
                    .apply();
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_ENDPOINT)
                .remove(KEY_BUCKET)
                .remove(KEY_ACCESS_KEY_ID)
                .remove(KEY_ACCESS_KEY_SECRET)
                .remove(KEY_REMOTE_DIR)
                .apply();
    }

    // ==================== OSS 客户端初始化 ====================

    /**
     * 根据已保存配置初始化 OSS 客户端
     */
    public boolean initClient(Context context) {
        OssConfig config = getOssConfig(context);
        return initClient(context, config.endpoint, config.accessKeyId, config.accessKeySecret);
    }

    /**
     * 使用显式传入的 Context 初始化 OSS 客户端
     */
    public boolean initClient(Context context, String endpoint, String accessKeyId, String accessKeySecret) {
        try {
            OSSCredentialProvider credentialProvider =
                    new OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret);

            ClientConfiguration conf = new ClientConfiguration();
            conf.setConnectionTimeout(15 * 1000);
            conf.setSocketTimeout(30 * 1000);
            conf.setMaxConcurrentRequest(5);
            conf.setMaxErrorRetry(2);

            ossClient = new OSSClient(context, endpoint, credentialProvider, conf);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to init OSS client", e);
            return false;
        }
    }

    // ==================== 文件上传 ====================

    /**
     * 同步上传文件到 OSS
     *
     * @param bucketName      Bucket 名称
     * @param objectKey       OSS 对象键（包含路径，如 "backup/passwd_encrypted_20260605.db"）
     * @param localFilePath   本地文件路径
     * @return true 上传成功
     */
    public boolean uploadSync(String bucketName, String objectKey, String localFilePath) {
        if (ossClient == null) {
            Log.e(TAG, "OSS client not initialized");
            return false;
        }

        try {
            PutObjectRequest put = new PutObjectRequest(bucketName, objectKey, localFilePath);
            PutObjectResult result = ossClient.putObject(put);
            Log.i(TAG, "OSS upload success, ETag: " + result.getETag());
            return true;
        } catch (ClientException e) {
            Log.e(TAG, "OSS client error: " + e.getMessage(), e);
            return false;
        } catch (ServiceException e) {
            Log.e(TAG, "OSS service error: " + e.getStatusCode() + " " + e.getErrorCode(), e);
            return false;
        }
    }

    /**
     * 同步上传文件到 OSS（使用已保存配置的 Bucket）
     */
    public boolean uploadSync(Context context, String objectKey, String localFilePath) {
        OssConfig config = getOssConfig(context);
        return uploadSync(config.bucket, objectKey, localFilePath);
    }

    /**
     * 同步上传 File 对象
     */
    public boolean uploadSync(String bucketName, String objectKey, File localFile) {
        return uploadSync(bucketName, objectKey, localFile.getAbsolutePath());
    }

    /**
     * 同步上传 File 对象（使用已保存配置）
     */
    public boolean uploadSync(Context context, String objectKey, File localFile) {
        OssConfig config = getOssConfig(context);
        return uploadSync(config.bucket, objectKey, localFile.getAbsolutePath());
    }

    /**
     * 异步上传文件到 OSS
     *
     * @param bucketName  Bucket 名称
     * @param objectKey   对象键
     * @param localFile   本地文件
     * @param callback    上传回调
     * @return OSSAsyncTask
     */
    public OSSAsyncTask uploadAsync(String bucketName, String objectKey, File localFile,
                                    final OssUploadCallback callback) {
        if (ossClient == null) {
            Log.e(TAG, "OSS client not initialized");
            if (callback != null) {
                callback.onFailure(new Exception("OSS client not initialized"));
            }
            return null;
        }

        PutObjectRequest put = new PutObjectRequest(bucketName, objectKey, localFile.getAbsolutePath());

        if (callback != null) {
            put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
                @Override
                public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                    callback.onProgress(currentSize, totalSize);
                }
            });
        }

        return ossClient.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.i(TAG, "OSS async upload success, ETag: " + result.getETag());
                if (callback != null) {
                    callback.onSuccess(result.getETag());
                }
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientEx, ServiceException serviceEx) {
                String msg;
                if (clientEx != null) {
                    msg = "ClientError: " + clientEx.getMessage();
                    Log.e(TAG, msg, clientEx);
                } else if (serviceEx != null) {
                    msg = "ServiceError: " + serviceEx.getStatusCode() + " " + serviceEx.getErrorCode();
                    Log.e(TAG, msg, serviceEx);
                } else {
                    msg = "Unknown error";
                }
                if (callback != null) {
                    callback.onFailure(new Exception(msg));
                }
            }
        });
    }

    /**
     * 异步上传（使用已保存配置）
     */
    public OSSAsyncTask uploadAsync(Context context, String objectKey, File localFile,
                                    final OssUploadCallback callback) {
        OssConfig config = getOssConfig(context);
        return uploadAsync(config.bucket, objectKey, localFile, callback);
    }

    /**
     * 带超时的同步上传（内部使用 CountDownLatch）
     */
    public boolean uploadSyncWithTimeout(Context context, String objectKey, File localFile,
                                         long timeoutMs) {
        final AtomicBoolean result = new AtomicBoolean(false);
        final AtomicReference<String> errorMsg = new AtomicReference<>(null);
        final CountDownLatch latch = new CountDownLatch(1);

        uploadAsync(context, objectKey, localFile, new OssUploadCallback() {
            @Override
            public void onSuccess(String etag) {
                result.set(true);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                errorMsg.set(e.getMessage());
                latch.countDown();
            }

            @Override
            public void onProgress(long current, long total) {
                // 进度忽略
            }
        });

        try {
            boolean finished = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                Log.e(TAG, "OSS upload timed out after " + timeoutMs + "ms");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return result.get();
    }

    // ==================== 工具方法 ====================

    /**
     * 生成OSS对象键（路径+文件名）
     */
    public static String buildObjectKey(String remoteDir, String fileName) {
        if (remoteDir == null || remoteDir.isEmpty()) {
            return fileName;
        }
        String dir = remoteDir.endsWith("/") ? remoteDir : remoteDir + "/";
        return dir + fileName;
    }

    /**
     * 获取 OSS 客户端实例
     * @return OSS 客户端对象，可能为 null（如果尚未初始化）
     */
    public OSS getOssClient() {
        return ossClient;
    }

    /**
     * 释放 OSS 客户端资源
     */
    public void destroy() {
        if (ossClient != null) {
            ossClient = null;
        }
    }

    // ==================== 回调接口 ====================

    /**
     * OSS 上传回调
     */
    public interface OssUploadCallback {
        void onSuccess(String etag);
        void onFailure(Exception e);
        void onProgress(long currentSize, long totalSize);
    }

    // ==================== 数据类 ====================

    public static class OssConfig {
        public final String endpoint;
        public final String bucket;
        public final String accessKeyId;
        public final String accessKeySecret;
        public final String remoteDir;

        public OssConfig(String endpoint, String bucket, String accessKeyId,
                         String accessKeySecret, String remoteDir) {
            this.endpoint = endpoint;
            this.bucket = bucket;
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
            this.remoteDir = remoteDir;
        }
    }
}
