package top.zw.passwd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VerifyQuestionActivity extends AppCompatActivity {

    private TextView tvQuestion1, tvQuestion2, tvQuestion3;
    private EditText etAnswer1, etAnswer2, etAnswer3;
    private Button btnVerify;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "passwd_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_question);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        tvQuestion1 = findViewById(R.id.tv_question_1);
        tvQuestion2 = findViewById(R.id.tv_question_2);
        tvQuestion3 = findViewById(R.id.tv_question_3);
        etAnswer1 = findViewById(R.id.edit_answer_1);
        etAnswer2 = findViewById(R.id.edit_answer_2);
        etAnswer3 = findViewById(R.id.edit_answer_3);
        btnVerify = findViewById(R.id.btn_verify);

        // 加载已保存的问题
        String q1 = prefs.getString("q1_question", null);
        String q2 = prefs.getString("q2_question", null);
        String q3 = prefs.getString("q3_question", null);

        if (q1 == null || q2 == null || q3 == null) {
            Toast.makeText(this, R.string.security_no_questions_hint, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvQuestion1.setText(q1);
        tvQuestion2.setText(q2);
        tvQuestion3.setText(q3);

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String answer1 = etAnswer1.getText().toString().trim();
                String answer2 = etAnswer2.getText().toString().trim();
                String answer3 = etAnswer3.getText().toString().trim();

                // 至少输入一个答案即可验证
                if (answer1.isEmpty() && answer2.isEmpty() && answer3.isEmpty()) {
                    Toast.makeText(VerifyQuestionActivity.this,
                            R.string.security_answer_empty_hint, Toast.LENGTH_SHORT).show();
                    return;
                }

                // 比对答案哈希（三选一：任一答案匹配即通过验证）
                String savedA1 = prefs.getString("q1_answer", "");
                String savedA2 = prefs.getString("q2_answer", "");
                String savedA3 = prefs.getString("q3_answer", "");

                boolean match1 = !answer1.isEmpty() && SetPasswordActivity.sha256(answer1).equals(savedA1);
                boolean match2 = !answer2.isEmpty() && SetPasswordActivity.sha256(answer2).equals(savedA2);
                boolean match3 = !answer3.isEmpty() && SetPasswordActivity.sha256(answer3).equals(savedA3);

                if (match1 || match2 || match3) {
                    // 验证成功（任一答案正确）：清除密码哈希，跳转设置密码
                    Toast.makeText(VerifyQuestionActivity.this,
                            R.string.security_verify_success, Toast.LENGTH_SHORT).show();

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("app_password");
                    editor.apply();

                    Intent intent = new Intent(VerifyQuestionActivity.this, SetPasswordActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(VerifyQuestionActivity.this,
                            R.string.security_verify_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}