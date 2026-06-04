package top.zw.passwd;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class VerifyPasswordActivity extends AppCompatActivity {

    private TextInputLayout passwordInputLayout;
    private TextInputEditText passwordInput;
    private MaterialButton btnVerify;
    private MaterialButton btnForgotPassword;
    private TextView tvError;

    private int attemptCount = 0;
    private static final int MAX_ATTEMPTS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_password);

        initViews();
        setupListeners();

        // 检查是否已经设置了密码，如果未设置则跳转到设置密码页面
        checkPasswordExists();
    }

    private void initViews() {
        passwordInputLayout = findViewById(R.id.password_input_layout);
        passwordInput = findViewById(R.id.password_input);
        btnVerify = findViewById(R.id.btn_verify);
        btnForgotPassword = findViewById(R.id.btn_forgot_password);
        tvError = findViewById(R.id.tv_error);
    }

    private void setupListeners() {
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptVerify();
            }
        });

        // 键盘Done键触发验证
        passwordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    attemptVerify();
                    return true;
                }
                return false;
            }
        });

        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    private void checkPasswordExists() {
        SharedPreferences prefs = getSharedPreferences("passwd_prefs", Context.MODE_PRIVATE);
        String hashedPassword = prefs.getString("app_password", null);
        if (TextUtils.isEmpty(hashedPassword)) {
            // 未设置密码，跳转到设置密码页面
            Intent intent = new Intent(this, SetPasswordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void attemptVerify() {
        passwordInputLayout.setError(null);
        tvError.setVisibility(View.GONE);

        String inputPassword = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(inputPassword)) {
            showError(getString(R.string.password_too_short));
            return;
        }

        // 获取存储的密码哈希
        SharedPreferences prefs = getSharedPreferences("passwd_prefs", Context.MODE_PRIVATE);
        String storedHash = prefs.getString("app_password", "");

        // 计算输入密码的哈希值并比对
        String inputHash = SetPasswordActivity.sha256(inputPassword);
        if (inputHash.equals(storedHash)) {
            // 验证成功
            Toast.makeText(this, R.string.password_verify_success, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            // 验证失败
            attemptCount++;
            int remaining = MAX_ATTEMPTS - attemptCount;
            if (remaining <= 0) {
                // 超过最大尝试次数，锁定并跳转到忘记密码
                showForgotPasswordDialog();
            } else {
                showError(getString(R.string.password_error_retry, remaining));
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void showForgotPasswordDialog() {
        // 先检查是否设置了密保问题
        SharedPreferences prefs = getSharedPreferences("passwd_prefs", Context.MODE_PRIVATE);
        String savedQuestion1 = prefs.getString("q1_question", null);
        if (savedQuestion1 != null && !savedQuestion1.isEmpty()) {
            // 有密保问题，跳转到密保验证页面
            Intent intent = new Intent(VerifyPasswordActivity.this, VerifyQuestionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            // 没有密保问题，直接走清除密码逻辑
            new AlertDialog.Builder(this)
                    .setTitle(R.string.btn_forgot_password)
                    .setMessage(R.string.no_security_question)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 清除密码，跳转到设置密码页面
                            prefs.edit().remove("app_password").apply();
                            Toast.makeText(VerifyPasswordActivity.this, R.string.password_reset, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(VerifyPasswordActivity.this, SetPasswordActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(false)
                    .show();
        }
    }
}