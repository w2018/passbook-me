package top.zw.passwd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SecurityQuestionActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerQuestion1, spinnerQuestion2, spinnerQuestion3;
    private EditText etAnswer1, etAnswer2, etAnswer3;
    private Button btnSave, btnSkip;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "passwd_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_question);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        spinnerQuestion1 = findViewById(R.id.spinner_question_1);
        spinnerQuestion2 = findViewById(R.id.spinner_question_2);
        spinnerQuestion3 = findViewById(R.id.spinner_question_3);
        etAnswer1 = findViewById(R.id.edit_answer_1);
        etAnswer2 = findViewById(R.id.edit_answer_2);
        etAnswer3 = findViewById(R.id.edit_answer_3);
        btnSave = findViewById(R.id.btn_save_questions);
        btnSkip = findViewById(R.id.btn_skip_questions);

        // 加载预设问题列表
        String[] questions = getResources().getStringArray(R.array.security_questions);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, questions);

        spinnerQuestion1.setAdapter(adapter);
        spinnerQuestion2.setAdapter(adapter);
        spinnerQuestion3.setAdapter(adapter);

        // 如果有已保存的问题，加载之
        String savedQ1 = prefs.getString("q1_question", null);
        if (savedQ1 != null) {
            spinnerQuestion1.setText(savedQ1, false);
        }
        String savedQ2 = prefs.getString("q2_question", null);
        if (savedQ2 != null) {
            spinnerQuestion2.setText(savedQ2, false);
        }
        String savedQ3 = prefs.getString("q3_question", null);
        if (savedQ3 != null) {
            spinnerQuestion3.setText(savedQ3, false);
        }

        // 保存按钮
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String answer1 = etAnswer1.getText().toString().trim();
                String answer2 = etAnswer2.getText().toString().trim();
                String answer3 = etAnswer3.getText().toString().trim();

                if (answer1.isEmpty() || answer2.isEmpty() || answer3.isEmpty()) {
                    Toast.makeText(SecurityQuestionActivity.this,
                            R.string.security_answer_empty_hint, Toast.LENGTH_SHORT).show();
                    return;
                }

                // 检查三个问题是否重复
                String q1 = spinnerQuestion1.getText().toString();
                String q2 = spinnerQuestion2.getText().toString();
                String q3 = spinnerQuestion3.getText().toString();

                if (q1.equals(q2) || q1.equals(q3) || q2.equals(q3)) {
                    Toast.makeText(SecurityQuestionActivity.this,
                            R.string.security_question_duplicate_hint, Toast.LENGTH_SHORT).show();
                    return;
                }

                // 保存问题文本和答案哈希
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("q1_question", q1);
                editor.putString("q1_answer", SetPasswordActivity.sha256(answer1));
                editor.putString("q2_question", q2);
                editor.putString("q2_answer", SetPasswordActivity.sha256(answer2));
                editor.putString("q3_question", q3);
                editor.putString("q3_answer", SetPasswordActivity.sha256(answer3));
                editor.apply();

                Toast.makeText(SecurityQuestionActivity.this,
                        R.string.security_save_success, Toast.LENGTH_SHORT).show();

                // 跳转主界面
                navigateToMain();
            }
        });

        // 跳过按钮
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToMain();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(SecurityQuestionActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
