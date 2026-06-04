package top.zw.passwd;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class OssConfigActivity extends AppCompatActivity {

    private TextInputEditText etEndpoint, etBucket, etAccessKeyId, etAccessKeySecret, etOssPath;
    private MaterialButton btnTest, btnUpload, btnSave, btnClear;
    private TextView tvStatus;

    private Handler mainHandler;

    /**
     * 自动补全Endpoint协议头。若不以 https:// 或 http:// 开头，则添加 https://
     */
    private String ensureEndpointProtocol(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) return endpoint;
        String trimmed = endpoint.trim();
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oss_config);

        mainHandler = new Handler(Looper.getMainLooper());

        // 绑定控件
        etEndpoint = findViewById(R.id.et_endpoint);
        etBucket = findViewById(R.id.et_bucket);
        etAccessKeyId = findViewById(R.id.et_access_key_id);
        etAccessKeySecret = findViewById(R.id.et_access_key_secret);
        etOssPath = findViewById(R.id.et_oss_path);
        btnTest = findViewById(R.id.btn_test_oss_connection);
        btnUpload = findViewById(R.id.btn_upload_oss_backup);
        btnSave = findViewById(R.id.btn_save_oss_config);
        btnClear = findViewById(R.id.btn_clear_oss_config);
        tvStatus = findViewById(R.id.tv_oss_status);

        // 加载现有配置
        loadExistingConfig();

        // 测试连接
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String endpoint = ensureEndpointProtocol(etEndpoint.getText().toString().trim());
                String bucket = etBucket.getText().toString().trim();
                String accessKeyId = etAccessKeyId.getText().toString().trim();
                String accessKeySecret = etAccessKeySecret.getText().toString().trim();

                if (endpoint.isEmpty() || bucket.isEmpty() || accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
                    Toast.makeText(OssConfigActivity.this,
                            R.string.oss_config_fields_hint, Toast.LENGTH_SHORT).show();
                    return;
                }

                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(R.string.connection_testing);
                tvStatus.setTextColor(getColor(android.R.color.darker_gray));
                btnTest.setEnabled(false);

                final String fEndpoint = endpoint;
                final String fBucket = bucket;
                final String fAkId = accessKeyId;
                final String fAkSecret = accessKeySecret;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        OssClientUtil clientUtil = new OssClientUtil();
                        boolean success = clientUtil.initClient(
                                OssConfigActivity.this, fEndpoint, fAkId, fAkSecret);
                        if (success) {
                            // 尝试列出 bucket 中的对象来验证连接有效性
                            try {
                                // 使用同步上传一个空对象来测试（更可靠的测试方式）
                                // 或者直接尝试创建客户端后执行 bucket 存在性检查
                                // OSS SDK 的 putObject 如果 endpoint/ak/sk 不对会直接抛异常
                                // 这里用一个轻量方法：构造客户端成功即认为连接配置有效
                                // 实际上 initClient 已经做了客户端初始化，如果凭证不对会在第一次操作时报错
                                // 为更准确，尝试调用 listObjects 但限制返回1条
                                com.alibaba.sdk.android.oss.model.ListObjectsRequest listReq =
                                        new com.alibaba.sdk.android.oss.model.ListObjectsRequest(fBucket);
                                listReq.setMaxKeys(1);
                                clientUtil.getOssClient().listObjects(listReq);
                                success = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                                success = false;
                            } finally {
                                clientUtil.destroy();
                            }
                        }

                        final boolean result = success;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnTest.setEnabled(true);
                                if (result) {
                                    tvStatus.setText(R.string.oss_test_success);
                                    tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                                } else {
                                    tvStatus.setText(R.string.oss_test_failed);
                                    tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        // 上传备份
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 使用当前输入（优先）或已保存配置
                String endpoint = ensureEndpointProtocol(etEndpoint.getText().toString().trim());
                String bucket = etBucket.getText().toString().trim();
                String accessKeyId = etAccessKeyId.getText().toString().trim();
                String accessKeySecret = etAccessKeySecret.getText().toString().trim();
                String ossPath = etOssPath.getText().toString().trim();

                // 如果当前输入为空，尝试从已保存配置加载
                if (endpoint.isEmpty() || bucket.isEmpty() || accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
                    OssClientUtil.OssConfig config = OssClientUtil.getOssConfig(OssConfigActivity.this);
                    if (config == null || config.endpoint.isEmpty()) {
                        Toast.makeText(OssConfigActivity.this,
                                R.string.oss_config_fields_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    endpoint = ensureEndpointProtocol(config.endpoint);
                    bucket = config.bucket;
                    accessKeyId = config.accessKeyId;
                    accessKeySecret = config.accessKeySecret;
                    ossPath = config.remoteDir;
                }

                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(R.string.oss_connecting);
                tvStatus.setTextColor(getColor(android.R.color.darker_gray));
                btnUpload.setEnabled(false);

                final String fEndpoint = endpoint;
                final String fBucket = bucket;
                final String fAkId = accessKeyId;
                final String fAkSecret = accessKeySecret;
                final String fOssPath = ossPath;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 先导出加密数据库到缓存目录
                        File cacheDir = new File(getCacheDir(), "oss_backup");
                        File dbFile = ImportExportUtil.exportEncryptedDb(OssConfigActivity.this, cacheDir);
                        if (dbFile == null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    btnUpload.setEnabled(true);
                                    tvStatus.setText(R.string.export_failed);
                                    tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                                }
                            });
                            return;
                        }

                        OssClientUtil clientUtil = new OssClientUtil();
                        boolean success = false;
                        try {
                            // 初始化客户端
                            if (!clientUtil.initClient(OssConfigActivity.this, fEndpoint, fAkId, fAkSecret)) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        btnUpload.setEnabled(true);
                                        tvStatus.setText(R.string.oss_failed);
                                        tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                                    }
                                });
                                return;
                            }

                            // 构建对象键
                            String objectKey = OssClientUtil.buildObjectKey(fOssPath, dbFile.getName());

                            // 上传
                            success = clientUtil.uploadSync(fBucket, objectKey, dbFile.getAbsolutePath());

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            clientUtil.destroy();
                            if (dbFile.exists()) dbFile.delete();
                        }

                        final boolean result = success;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnUpload.setEnabled(true);
                                if (result) {
                                    tvStatus.setText(R.string.oss_success);
                                    tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                                } else {
                                    tvStatus.setText(R.string.oss_failed);
                                    tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        // 保存配置
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String endpoint = etEndpoint.getText().toString().trim();
                String bucket = etBucket.getText().toString().trim();
                String accessKeyId = etAccessKeyId.getText().toString().trim();
                String accessKeySecret = etAccessKeySecret.getText().toString().trim();
                String ossPath = etOssPath.getText().toString().trim();

                if (endpoint.isEmpty() || bucket.isEmpty() || accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
                    Toast.makeText(OssConfigActivity.this,
                            R.string.oss_config_fields_hint, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (ossPath.isEmpty()) {
                    ossPath = "/";
                }

                OssClientUtil.saveOssConfig(OssConfigActivity.this, endpoint, bucket, accessKeyId, accessKeySecret, ossPath);
                Toast.makeText(OssConfigActivity.this,
                        R.string.oss_config_saved, Toast.LENGTH_SHORT).show();

                // 返回结果给 MainActivity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("oss_config_saved", true);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        // 清除配置
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OssClientUtil.clearOssConfig(OssConfigActivity.this);
                etEndpoint.setText("");
                etBucket.setText("");
                etAccessKeyId.setText("");
                etAccessKeySecret.setText("");
                etOssPath.setText("");
                tvStatus.setVisibility(View.GONE);
                Toast.makeText(OssConfigActivity.this,
                        R.string.oss_config_cleared, Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("oss_config_cleared", true);
                setResult(RESULT_OK, resultIntent);
            }
        });
    }

    private void loadExistingConfig() {
        OssClientUtil.OssConfig config = OssClientUtil.getOssConfig(this);
        if (config != null) {
            etEndpoint.setText(config.endpoint);
            etBucket.setText(config.bucket);
            etAccessKeyId.setText(config.accessKeyId);
            etAccessKeySecret.setText(config.accessKeySecret);
            etOssPath.setText(config.remoteDir);
        }
    }
}
