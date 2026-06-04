package top.zw.passwd;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FtpConfigActivity extends AppCompatActivity {

    private EditText etHost, etPort, etUser, etPwd, etPath;
    private Button btnTest, btnUpload, btnSave, btnClear;
    private TextView tvStatus;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp_config);

        mainHandler = new Handler(Looper.getMainLooper());

        etHost = findViewById(R.id.host_input);
        etPort = findViewById(R.id.port_input);
        etUser = findViewById(R.id.user_input);
        etPwd = findViewById(R.id.pwd_input);
        etPath = findViewById(R.id.path_input);
        btnTest = findViewById(R.id.btn_test_connection);
        btnUpload = findViewById(R.id.btn_upload_backup);
        btnSave = findViewById(R.id.btn_save_config);
        btnClear = findViewById(R.id.btn_clear_config);
        tvStatus = findViewById(R.id.tv_connection_status);

        // 加载现有配置
        loadExistingConfig();

        // 测试连接
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String host = etHost.getText().toString().trim();
                String portStr = etPort.getText().toString().trim();
                String user = etUser.getText().toString().trim();
                String password = etPwd.getText().toString().trim();

                if (host.isEmpty() || portStr.isEmpty() || user.isEmpty()) {
                    Toast.makeText(FtpConfigActivity.this,
                            R.string.ftp_config_fields_hint, Toast.LENGTH_SHORT).show();
                    return;
                }

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(FtpConfigActivity.this,
                            R.string.ftp_config_invalid_port, Toast.LENGTH_SHORT).show();
                    return;
                }

                tvStatus.setText(R.string.connection_testing);
                tvStatus.setTextColor(getColor(android.R.color.darker_gray));
                btnTest.setEnabled(false);

                final String fHost = host;
                final int fPort = port;
                final String fUser = user;
                final String fPwd = password;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FTPClientUtil ftpClient = new FTPClientUtil(FtpConfigActivity.this);
                        boolean success = false;
                        try {
                            success = ftpClient.connect(fHost, fPort, fUser, fPwd);
                            if (success) {
                                ftpClient.disconnect();
                            }
                        } catch (java.io.IOException e) {
                            success = false;
                        }

                        final boolean result = success;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnTest.setEnabled(true);
                                if (result) {
                                    tvStatus.setText(R.string.connection_success);
                                    tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                                } else {
                                    tvStatus.setText(R.string.connection_failed);
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
                if (!FTPClientUtil.isConfigured(FtpConfigActivity.this)) {
                    // 先检查当前输入是否已填写
                    String host = etHost.getText().toString().trim();
                    String portStr = etPort.getText().toString().trim();
                    String user = etUser.getText().toString().trim();
                    if (host.isEmpty() || portStr.isEmpty() || user.isEmpty()) {
                        Toast.makeText(FtpConfigActivity.this,
                                R.string.ftp_config_fields_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                tvStatus.setText(R.string.ftp_connecting);
                tvStatus.setTextColor(getColor(android.R.color.darker_gray));
                tvStatus.setVisibility(View.VISIBLE);
                btnUpload.setEnabled(false);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FTPClientUtil ftp = new FTPClientUtil(FtpConfigActivity.this);
                        boolean success = false;
                        try {
                            // 使用当前界面输入（优先）或已保存配置
                            String host = etHost.getText().toString().trim();
                            String portStr = etPort.getText().toString().trim();
                            String user = etUser.getText().toString().trim();
                            String password = etPwd.getText().toString().trim();
                            String path = etPath.getText().toString().trim();

                            if (host.isEmpty() || portStr.isEmpty() || user.isEmpty()) {
                                FTPClientUtil.FTPConfig config = FTPClientUtil.getFtpConfig(FtpConfigActivity.this);
                                if (config == null || config.host.isEmpty()) {
                                    mainHandler.post(() -> {
                                        btnUpload.setEnabled(true);
                                        tvStatus.setText(R.string.ftp_config_fields_hint);
                                        tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                                    });
                                    return;
                                }
                                if (ftp.connect(config.host, config.port, config.user, config.password)) {
                                    ftp.changeDirectory(config.remoteDir);
                                    java.io.File cacheDir = new java.io.File(getCacheDir(), "ftp_backup");
                                    java.io.File dbFile = ImportExportUtil.exportEncryptedDb(FtpConfigActivity.this, cacheDir);
                                    if (dbFile != null) {
                                        success = ftp.upload(dbFile, dbFile.getName());
                                        dbFile.delete();
                                    }
                                    ftp.disconnect();
                                }
                            } else {
                                int port = Integer.parseInt(portStr);
                                if (path.isEmpty()) path = "/";
                                if (ftp.connect(host, port, user, password)) {
                                    ftp.changeDirectory(path);
                                    java.io.File cacheDir = new java.io.File(getCacheDir(), "ftp_backup");
                                    java.io.File dbFile = ImportExportUtil.exportEncryptedDb(FtpConfigActivity.this, cacheDir);
                                    if (dbFile != null) {
                                        success = ftp.upload(dbFile, dbFile.getName());
                                        dbFile.delete();
                                    }
                                    ftp.disconnect();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try { ftp.disconnect(); } catch (Exception ignored) {}
                        }

                        final boolean result = success;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnUpload.setEnabled(true);
                                if (result) {
                                    tvStatus.setText(R.string.ftp_success);
                                    tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                                } else {
                                    tvStatus.setText(R.string.ftp_failed);
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
                String host = etHost.getText().toString().trim();
                String portStr = etPort.getText().toString().trim();
                String user = etUser.getText().toString().trim();
                String password = etPwd.getText().toString().trim();
                String path = etPath.getText().toString().trim();

                if (host.isEmpty() || portStr.isEmpty() || user.isEmpty()) {
                    Toast.makeText(FtpConfigActivity.this,
                            R.string.ftp_config_fields_hint, Toast.LENGTH_SHORT).show();
                    return;
                }

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(FtpConfigActivity.this,
                            R.string.ftp_config_invalid_port, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (path.isEmpty()) {
                    path = "/";
                }

                FTPClientUtil.saveFtpConfig(FtpConfigActivity.this, host, port, user, password, path);
                Toast.makeText(FtpConfigActivity.this,
                        R.string.config_saved, Toast.LENGTH_SHORT).show();

                // 返回结果给MainActivity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("config_saved", true);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        // 清除配置
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FTPClientUtil.clearFtpConfig(FtpConfigActivity.this);
                etHost.setText("");
                etPort.setText("");
                etUser.setText("");
                etPwd.setText("");
                etPath.setText("");
                tvStatus.setText("");
                Toast.makeText(FtpConfigActivity.this,
                        R.string.ftp_config_cleared, Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("config_cleared", true);
                setResult(RESULT_OK, resultIntent);
            }
        });
    }

    private void loadExistingConfig() {
        FTPClientUtil.FTPConfig config = FTPClientUtil.getFtpConfig(this);
        if (config != null) {
            etHost.setText(config.host);
            etPort.setText(String.valueOf(config.port));
            etUser.setText(config.user);
            etPwd.setText(config.password);
            etPath.setText(config.remoteDir);
        } else {
            // 设置默认端口
            etPort.setText("21");
        }
    }
}