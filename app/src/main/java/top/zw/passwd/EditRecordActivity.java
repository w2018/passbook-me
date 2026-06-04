package top.zw.passwd;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 密码记录编辑/新增Activity
 * 支持两种模式：
 *   - 新建模式：intent.putExtra("id", 0)
 *   - 编辑模式：intent.putExtra("id", recordId) + intent.putExtra("mode", "edit")
 *
 * 编辑模式下会预填充已有数据，新建模式下显示空白表单。
 */
public class EditRecordActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private TextInputEditText etTitle, etRegAddr, etLoginUser, etLoginPwd, etLoginMail, etLoginPhone, etRemarks;
    private TextInputLayout tilTitle, tilRegAddr, tilLoginUser, tilLoginPwd, tilLoginMail, tilLoginPhone, tilRemarks;

    private MaterialButton btnSave, btnCancel;

    private int recordId = 0;           // 0=新建, >0=编辑
    private boolean isEditMode = false;

    private SqlClass sqlClass;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_layout);

        // 初始化数据库操作类
        sqlClass = new SqlClass();

        // 获取传入参数（id 在 DataInfo 中为 int 类型）
        recordId = getIntent().getIntExtra("id", 0);

        // 判断编辑模式：支持两种传参方式
        // 1) 显式布尔值 "editMode"
        // 2) 字符串 "mode" = "edit"
        // 3) 回退：id > 0 视为编辑模式
        if (getIntent().hasExtra("editMode")) {
            isEditMode = getIntent().getBooleanExtra("editMode", false);
        } else if (getIntent().hasExtra("mode")) {
            isEditMode = "edit".equals(getIntent().getStringExtra("mode"));
        } else {
            isEditMode = (recordId > 0);
        }

        // 初始化视图
        initViews();

        // 设置Toolbar
        setupToolbar();

        // 如果是编辑模式，加载已有数据
        if (isEditMode && recordId > 0) {
            loadRecordData();
        }

        // 设置按钮点击事件
        btnSave.setOnClickListener(v -> saveRecord());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        tilTitle = findViewById(R.id.tilTitle);
        tilRegAddr = findViewById(R.id.tilRegAddr);
        tilLoginUser = findViewById(R.id.tilLoginUser);
        tilLoginPwd = findViewById(R.id.tilLoginPwd);
        tilLoginMail = findViewById(R.id.tilLoginMail);
        tilLoginPhone = findViewById(R.id.tilLoginPhone);
        tilRemarks = findViewById(R.id.tilRemarks);

        etTitle = findViewById(R.id.etTitle);
        etRegAddr = findViewById(R.id.etRegAddr);
        etLoginUser = findViewById(R.id.etLoginUser);
        etLoginPwd = findViewById(R.id.etLoginPwd);
        etLoginMail = findViewById(R.id.etLoginMail);
        etLoginPhone = findViewById(R.id.etLoginPhone);
        etRemarks = findViewById(R.id.etRemarks);

        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupToolbar() {
        if (isEditMode) {
            toolbar.setTitle("编辑密码");
        } else {
            toolbar.setTitle("添加密码");
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    /**
     * 编辑模式：加载已有记录数据填充到表单
     */
    private void loadRecordData() {
        DataInfo data = sqlClass.getById(recordId);
        if (data == null) {
            Toast.makeText(this, "记录不存在或已删除", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etTitle.setText(data.getTitle());
        etRegAddr.setText(data.getRegAddr());
        etLoginUser.setText(data.getLoginUser());
        etLoginPwd.setText(data.getLoginPwd());
        etLoginMail.setText(data.getLoginMail());
        etLoginPhone.setText(data.getLoginPhone());
        etRemarks.setText(data.getRemarks());
    }

    /**
     * 表单验证并保存记录
     */
    private void saveRecord() {
        // 获取输入值
        String title = etTitle.getText().toString().trim();
        String regAddr = etRegAddr.getText().toString().trim();
        String loginUser = etLoginUser.getText().toString().trim();
        String loginPwd = etLoginPwd.getText().toString().trim();
        String loginMail = etLoginMail.getText().toString().trim();
        String loginPhone = etLoginPhone.getText().toString().trim();
        String remarks = etRemarks.getText().toString().trim();

        // --- 表单验证 ---
        // 清除之前的错误状态
        tilTitle.setError(null);
        tilLoginPwd.setError(null);

        boolean hasError = false;

        if (TextUtils.isEmpty(title)) {
            tilTitle.setError("标题不能为空");
            tilTitle.requestFocus();
            hasError = true;
        }

        if (TextUtils.isEmpty(loginPwd)) {
            tilLoginPwd.setError("密码不能为空");
            if (!hasError) {
                tilLoginPwd.requestFocus();
            }
            hasError = true;
        }

        if (hasError) {
            return;
        }

        // --- 构建数据 ---
        DataInfo data = new DataInfo();
        data.setTitle(title);
        data.setRegAddr(regAddr);
        data.setLoginUser(loginUser);
        data.setLoginPwd(loginPwd);
        data.setLoginMail(loginMail);
        data.setLoginPhone(loginPhone);
        data.setRemarks(remarks);

        boolean success;
        if (isEditMode && recordId > 0) {
            // 更新已有记录
            data.setId(recordId);
            int rows = sqlClass.update(data);
            success = (rows > 0);
        } else {
            // 新建记录
            long newId = sqlClass.insert(data);
            success = (newId > 0);
            if (success) {
                recordId = (int) newId;
            }
        }

        if (success) {
            Toast.makeText(this, isEditMode ? "保存成功" : "添加成功", Toast.LENGTH_SHORT).show();

            // 返回结果给调用方（MainActivity或PlayActivity）
            Intent resultIntent = new Intent();
            resultIntent.putExtra("savedId", recordId);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // 如果有未保存的内容，可以加确认提示（简化版直接返回）
        super.onBackPressed();
    }
}
