package top.zw.passwd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SetPasswordActivity extends AppCompatActivity {

    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton btnSetPassword;
    private MaterialButton btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        initViews();
        setupListeners();
    }

    private void initViews() {
        passwordInputLayout = findViewById(R.id.password_input_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        btnSetPassword = findViewById(R.id.btn_set_password);
        btnSkip = findViewById(R.id.btn_skip);
    }

    private void setupListeners() {
        btnSetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSetPassword();
            }
        });

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMain();
            }
        });
    }

    private void attemptSetPassword() {
        // 清除错误状态
        passwordInputLayout.setError(null);
        confirmPasswordLayout.setError(null);

        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String confirmPassword = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString().trim() : "";

        // 检查密码是否为空
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(getString(R.string.password_too_short));
            passwordInput.requestFocus();
            return;
        }

        // 检查密码长度（至少6位）
        if (password.length() < 6) {
            passwordInputLayout.setError(getString(R.string.password_too_short));
            passwordInput.requestFocus();
            return;
        }

        // 检查两次密码是否一致
        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.password_verify_failed));
            confirmPasswordInput.requestFocus();
            return;
        }

        // 保存密码哈希
        String hashedPassword = sha256(password);
        SharedPreferences prefs = getSharedPreferences("passwd_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("app_password", hashedPassword).apply();

        Toast.makeText(this, R.string.password_set_success, Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    private void navigateToMain() {
        // 先跳转到密保问题设置页面（密码设置成功后引导用户设置密保）
        Intent intent = new Intent(this, SecurityQuestionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * 使用 SHA-256 对密码进行哈希
     */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
