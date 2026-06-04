package top.zw.passwd;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class PlayActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private LinearLayout fieldsContainer;
    private MaterialButton btnEdit, btnDelete, btnShare;
    private MaterialButton btnSave, btnCancelEdit, btnDeleteEdit;

    // 查看/编辑模式底部容器
    private MaterialCardView bottomActionsView;
    private MaterialCardView bottomActionsEdit;

    private SqlClass sqlClass;
    private DataInfo dataInfo;
    private int recordId;

    // 编辑模式状态
    private boolean isEditing = false;

    // 编辑模式下的字段引用列表（用于收集数据）
    private final List<FieldRef> editFieldRefs = new ArrayList<>();

    // 密码字段特殊引用
    private EditText pwdEditText;
    private ImageButton pwdToggleBtn;
    private boolean pwdVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_activity);

        bindViews();
        setSupportActionBar(toolbar);
        sqlClass = new SqlClass();

        initData();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.detail_toolbar);
        fieldsContainer = findViewById(R.id.fields_container);

        // 查看模式按钮
        bottomActionsView = findViewById(R.id.bottom_actions_view);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        btnShare = findViewById(R.id.btn_share);

        // 编辑模式按钮
        bottomActionsEdit = findViewById(R.id.bottom_actions_edit);
        btnSave = findViewById(R.id.btn_save);
        btnCancelEdit = findViewById(R.id.btn_cancel_edit);
        btnDeleteEdit = findViewById(R.id.btn_delete_edit);
    }

    private void initData() {
        Intent intent = getIntent();
        recordId = intent.getIntExtra("id", 0);

        if (recordId > 0) {
            loadData();
        } else {
            // 新建模式：直接进入编辑模式
            dataInfo = new DataInfo();
            btnDelete.setVisibility(View.GONE);
        }

        setupButtons();
        setupToolbar();
        renderFields();
    }

    private void loadData() {
        dataInfo = sqlClass.getById(recordId);
        if (dataInfo == null) {
            Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        toolbar.setTitle(dataInfo.getTitle());
    }

    // ═══════════════════════════════════════════════════
    //  动态字段行生成（双模式）
    // ═══════════════════════════════════════════════════

    private void renderFields() {
        fieldsContainer.removeAllViews();
        editFieldRefs.clear();
        pwdEditText = null;

        if (dataInfo == null) return;

        // 组1：基本信息
        addSectionHeader("基本信息");
        addFieldRow(android.R.drawable.ic_menu_edit,    "名称",   "title",    dataInfo.getTitle());
        addFieldRow(android.R.drawable.ic_menu_compass,  "网址",   "regAddr",  dataInfo.getRegAddr());

        // 组2：登录信息
        addSectionHeader("登录信息");
        addFieldRow(android.R.drawable.ic_menu_myplaces, "用户名", "loginUser", dataInfo.getLoginUser());
        addPasswordFieldRow(dataInfo.getLoginPwd());
        addFieldRow(android.R.drawable.ic_dialog_email,  "邮箱",   "loginMail", dataInfo.getLoginMail());

        // 组3：联系方式
        addSectionHeader("联系方式");
        addFieldRow(android.R.drawable.ic_menu_call,     "电话",   "loginPhone", dataInfo.getLoginPhone());

        // 组4：备注
        addSectionHeader("备注");
        addFieldRow(android.R.drawable.ic_menu_edit,     "备注",   "remarks",   dataInfo.getRemarks());

        // 组5：时间（始终只读）
        addSectionHeader("时间");
        addFieldRow(android.R.drawable.ic_menu_recent_history, "创建时间", "idTime", dataInfo.getIdTime());
        addFieldRow(android.R.drawable.ic_menu_recent_history, "修改时间", "updateTime", dataInfo.getUpdateTime());
    }

    private void addSectionHeader(String title) {
        TextView header = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(8), 0, dp(8));
        header.setLayoutParams(lp);
        header.setText(title);
        header.setTextSize(12);
        header.setLetterSpacing(0.1f);
        header.setAllCaps(true);
        header.setTextColor(getThemeColor(com.google.android.material.R.attr.colorPrimary));
        fieldsContainer.addView(header);
    }

    @SuppressWarnings("deprecation")
    private int getThemeColor(int attrRes) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attrRes, tv, true);
        return tv.data;
    }

    /**
     * 根据 isEditing 状态渲染只读或可编辑字段行。
     * @param iconRes  图标资源
     * @param label    字段标签
     * @param fieldKey DataInfo 字段标识（用于编辑模式收集数据）
     * @param value    字段值
     */
    private View addFieldRow(int iconRes, String label, String fieldKey, String value) {
        if (isEditing && !"idTime".equals(fieldKey)) {
            return addEditableFieldRow(iconRes, label, fieldKey, value);
        } else {
            return addReadOnlyFieldRow(iconRes, label, value);
        }
    }

    // ─── 只读字段行（查看模式） ──────────────────────

    private View addReadOnlyFieldRow(int iconRes, String label, String value) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.field_row, fieldsContainer, false);

        ImageView icon = row.findViewById(R.id.field_icon);
        TextView labelView = row.findViewById(R.id.field_label);
        TextView valueView = row.findViewById(R.id.field_value);
        ImageButton actionBtn = row.findViewById(R.id.field_action);

        icon.setImageResource(iconRes);
        labelView.setText(label);
        String displayValue = (value == null || value.isEmpty()) ? "—" : value;
        valueView.setText(displayValue);

        final String copyText = (value == null || value.isEmpty()) ? "" : value;
        row.setOnClickListener(v -> {
            if (!copyText.isEmpty()) {
                ClipboardUtil.copy(PlayActivity.this, label, copyText);
                Toast.makeText(PlayActivity.this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
            }
        });

        actionBtn.setImageResource(R.drawable.ic_content_copy);
        actionBtn.setOnClickListener(v -> {
            if (!copyText.isEmpty()) {
                ClipboardUtil.copy(PlayActivity.this, label, copyText);
                Toast.makeText(PlayActivity.this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
            }
        });

        fieldsContainer.addView(row);
        return row;
    }

    // ─── 可编辑字段行（编辑模式） ────────────────────

    private View addEditableFieldRow(int iconRes, String label, String fieldKey, String value) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.edit_field_row, fieldsContainer, false);

        ImageView icon = row.findViewById(R.id.field_icon);
        TextView labelView = row.findViewById(R.id.field_label);
        EditText editText = row.findViewById(R.id.field_value);
        ImageButton actionBtn = row.findViewById(R.id.field_action);

        icon.setImageResource(iconRes);
        labelView.setText(label);
        String initialValue = (value == null || value.isEmpty()) ? "" : value;
        editText.setText(initialValue);

        // 备注字段用多行，其他单行
        if ("remarks".equals(fieldKey)) {
            editText.setMaxLines(4);
        } else {
            editText.setMaxLines(1);
        }

        // 编辑模式下右侧按钮改为清空当前内容
        actionBtn.setImageResource(R.drawable.ic_close);
        actionBtn.setOnClickListener(v -> editText.setText(""));

        // 记录引用用于保存时收集
        editFieldRefs.add(new FieldRef(fieldKey, editText));

        fieldsContainer.addView(row);
        return row;
    }

    // ─── 密码字段行（双模式） ──────────────────────

    private void addPasswordFieldRow(String password) {
        if (isEditing) {
            addEditablePasswordFieldRow(password);
        } else {
            addReadOnlyPasswordFieldRow(password);
        }
    }

    private void addReadOnlyPasswordFieldRow(String password) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.field_row, fieldsContainer, false);

        ImageView icon = row.findViewById(R.id.field_icon);
        TextView labelView = row.findViewById(R.id.field_label);
        TextView valueView = row.findViewById(R.id.field_value);
        ImageButton toggleBtn = row.findViewById(R.id.field_action);

        icon.setImageResource(android.R.drawable.ic_lock_lock);
        labelView.setText("密码");

        pwdVisible = false;
        String displayPwd = (password == null || password.isEmpty()) ? "—" : password;
        valueView.setText(displayPwd);
        valueView.setTransformationMethod(PasswordTransformationMethod.getInstance());
        toggleBtn.setImageResource(R.drawable.ic_visibility);

        final String copyPwd = (password == null || password.isEmpty()) ? "" : password;
        row.setOnClickListener(v -> {
            if (!copyPwd.isEmpty()) {
                ClipboardUtil.copy(PlayActivity.this, "密码", copyPwd);
                Toast.makeText(PlayActivity.this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
            }
        });

        toggleBtn.setOnClickListener(v -> {
            pwdVisible = !pwdVisible;
            if (pwdVisible) {
                valueView.setTransformationMethod(null);
                toggleBtn.setImageResource(R.drawable.ic_visibility_off);
            } else {
                valueView.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggleBtn.setImageResource(R.drawable.ic_visibility);
            }
        });

        fieldsContainer.addView(row);
    }

    private void addEditablePasswordFieldRow(String password) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.edit_field_row, fieldsContainer, false);

        ImageView icon = row.findViewById(R.id.field_icon);
        TextView labelView = row.findViewById(R.id.field_label);
        ImageButton toggleBtn = row.findViewById(R.id.field_action);

        icon.setImageResource(android.R.drawable.ic_lock_lock);
        labelView.setText("密码");

        pwdEditText = row.findViewById(R.id.field_value);
        pwdToggleBtn = toggleBtn;
        pwdVisible = false;

        String initialValue = (password == null || password.isEmpty()) ? "" : password;
        pwdEditText.setText(initialValue);
        pwdEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        pwdEditText.setMaxLines(1);

        toggleBtn.setImageResource(R.drawable.ic_visibility);

        toggleBtn.setOnClickListener(v -> {
            pwdVisible = !pwdVisible;
            if (pwdVisible) {
                pwdEditText.setTransformationMethod(null);
                toggleBtn.setImageResource(R.drawable.ic_visibility_off);
            } else {
                pwdEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggleBtn.setImageResource(R.drawable.ic_visibility);
            }
        });

        // 注册密码字段引用
        editFieldRefs.add(new FieldRef("loginPwd", pwdEditText));

        fieldsContainer.addView(row);
    }

    // ═══════════════════════════════════════════════════
    //  编辑模式切换
    // ═══════════════════════════════════════════════════

    private void enterEditMode() {
        isEditing = true;
        bottomActionsView.setVisibility(View.GONE);
        bottomActionsEdit.setVisibility(View.VISIBLE);
        renderFields();
    }

    private void exitEditMode() {
        isEditing = false;
        bottomActionsEdit.setVisibility(View.GONE);
        bottomActionsView.setVisibility(View.VISIBLE);
    }

    // ═══════════════════════════════════════════════════
    //  保存：从 EditText 收集数据并写入数据库
    // ═══════════════════════════════════════════════════

    private void saveChanges() {
        DataInfo updated = buildDataFromFields();
        if (updated == null) return;

        // 保留原始 id 和 idTime
        updated.setId(recordId);
        updated.setIdTime(dataInfo.getIdTime());
        // updateTime 由 SqlClass.dataInfoToContentValues() 自动设为当前时间

        int rows = sqlClass.update(updated);
        if (rows > 0) {
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            // 刷新内存数据
            dataInfo = sqlClass.getById(recordId);
            toolbar.setTitle(dataInfo.getTitle());
            exitEditMode();
            pwdVisible = false;
            renderFields();
        } else {
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    private DataInfo buildDataFromFields() {
        DataInfo info = new DataInfo();

        for (FieldRef ref : editFieldRefs) {
            String text = ref.editText.getText() != null
                    ? ref.editText.getText().toString().trim()
                    : "";

            switch (ref.fieldKey) {
                case "title":      info.setTitle(text);      break;
                case "regAddr":    info.setRegAddr(text);     break;
                case "loginUser":  info.setLoginUser(text);   break;
                case "loginPwd":   info.setLoginPwd(text);    break;
                case "loginMail":  info.setLoginMail(text);   break;
                case "loginPhone": info.setLoginPhone(text);  break;
                case "remarks":    info.setRemarks(text);     break;
            }
        }

        // 基本校验：名称不能为空
        if (info.getTitle() == null || info.getTitle().isEmpty()) {
            Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
            return null;
        }

        return info;
    }

    // ═══════════════════════════════════════════════════
    //  底部按钮事件
    // ═══════════════════════════════════════════════════

    private void setupButtons() {
        // ── 查看模式 ──
        btnEdit.setOnClickListener(v -> enterEditMode());

        btnDelete.setOnClickListener(v -> showDeleteDialog());

        btnShare.setOnClickListener(v -> {
            if (dataInfo == null) return;
            StringBuilder sb = new StringBuilder();
            sb.append("【").append(dataInfo.getTitle()).append("】\n");
            sb.append("用户名：").append(dataInfo.getLoginUser()).append("\n");
            sb.append("密码：").append(dataInfo.getLoginPwd()).append("\n");
            sb.append("网址：").append(dataInfo.getRegAddr()).append("\n");
            sb.append("备注：").append(dataInfo.getRemarks());

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            startActivity(Intent.createChooser(shareIntent, "分享密码"));
        });

        // ── 编辑模式 ──
        btnSave.setOnClickListener(v -> saveChanges());

        btnCancelEdit.setOnClickListener(v -> {
            // 丢弃编辑，恢复查看模式
            exitEditMode();
            pwdVisible = false;
            renderFields();
        });

        btnDeleteEdit.setOnClickListener(v -> showDeleteDialog());
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    sqlClass.deleteById(recordId);
                    Toast.makeText(PlayActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ═══════════════════════════════════════════════════
    //  选项菜单
    // ═══════════════════════════════════════════════════

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.play_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_copy_password) {
            if (dataInfo != null && dataInfo.getLoginPwd() != null && !dataInfo.getLoginPwd().isEmpty()) {
                ClipboardUtil.copy(this, "密码", dataInfo.getLoginPwd());
                Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (itemId == R.id.action_export_single) {
            Toast.makeText(this, "导出单条功能即将实现", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ═══════════════════════════════════════════════════
    //  工具
    // ═══════════════════════════════════════════════════

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ─── 内部类：编辑字段引用 ──────────────────────

    private static class FieldRef {
        final String fieldKey;
        final EditText editText;

        FieldRef(String fieldKey, EditText editText) {
            this.fieldKey = fieldKey;
            this.editText = editText;
        }
    }
}