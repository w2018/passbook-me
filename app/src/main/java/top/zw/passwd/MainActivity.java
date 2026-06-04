package top.zw.passwd;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // === UI 组件 ===
    private MaterialToolbar topAppBar;
    private MaterialCardView searchCard;
    private LinearLayout searchBarContainer;
    private EditText searchInput;
    private ImageView searchClearBtn;
    private LoadMoreListView passwordListView;
    private LinearLayout emptyState;
    private FloatingActionButton fabAdd;

    // === 数据 ===
    private PasswordListAdapter adapter;
    private List<DataInfo> dataList = new ArrayList<>();
    private SqlClass sqlClass;

    // === 分页 ===
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 20;

    // === 搜索防抖 ===
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 500;

    // === FTP 配置回调 ===
    private androidx.activity.result.ActivityResultLauncher<Intent> ftpConfigLauncher;

    // === OSS 配置回调 ===
    private androidx.activity.result.ActivityResultLauncher<Intent> ossConfigLauncher;

    // === 编辑记录回调 ===
    private androidx.activity.result.ActivityResultLauncher<Intent> editRecordLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ⚠️ 硬约束：applyTheme 必须在 super.onCreate 之前调用
        ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // 数据迁移（setContentView 之后、数据库操作之前）
        DataMigrator.migrateIfNeeded();

        // 初始化数据库访问
        sqlClass = new SqlClass();

        // 绑定 UI 组件
        bindViews();

        // 配置 Toolbar
        setSupportActionBar(topAppBar);

        // 初始化适配器
        adapter = new PasswordListAdapter(this, dataList);
        passwordListView.setAdapter(adapter);

        // 配置搜索
        setupSearch();

        // 配置列表事件
        setupListView();

        // 配置 FAB
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditRecordActivity.class);
            intent.putExtra("id", 0); // 0 表示新建
            intent.putExtra("mode", "add");
            editRecordLauncher.launch(intent);
        });

        // 注册 FTP 配置结果回调
        ftpConfigLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        boolean configSaved = result.getData().getBooleanExtra("config_saved", false);
                        boolean configCleared = result.getData().getBooleanExtra("config_cleared", false);
                        if (configSaved) {
                    Toast.makeText(MainActivity.this, R.string.config_saved, Toast.LENGTH_SHORT).show();
                } else if (configCleared) {
                            Toast.makeText(MainActivity.this, R.string.ftp_config_cleared, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 注册 OSS 配置回调
        ossConfigLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        boolean configSaved = result.getData().getBooleanExtra("oss_config_saved", false);
                        boolean configCleared = result.getData().getBooleanExtra("oss_config_cleared", false);
                        if (configSaved) {
                            Toast.makeText(MainActivity.this, R.string.config_saved, Toast.LENGTH_SHORT).show();
                        } else if (configCleared) {
                            Toast.makeText(MainActivity.this, R.string.oss_config_cleared, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 注册编辑记录回调
        editRecordLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        int savedId = result.getData().getIntExtra("savedId", 0);
                        if (savedId > 0) {
                            refreshData();
                        }
                    }
                });

        // 加载数据
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从 PlayActivity 返回后刷新数据
        refreshData();
    }

    // ==================== UI 绑定 ====================

    private void bindViews() {
        topAppBar = findViewById(R.id.top_app_bar);
        searchCard = findViewById(R.id.search_card);
        searchBarContainer = findViewById(R.id.search_bar_container);
        searchInput = findViewById(R.id.search_input);
        searchClearBtn = findViewById(R.id.search_clear_btn);
        passwordListView = findViewById(R.id.password_list_view);
        emptyState = findViewById(R.id.empty_state);
        fabAdd = findViewById(R.id.fab_add);
    }

    // ==================== 搜索 ====================

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 防抖：延迟 SEARCH_DEBOUNCE_MS 后执行搜索
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> performSearch(s.toString().trim());
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });

        // 键盘回车搜索：按下回车键立即搜索，并收起键盘
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                performSearch(searchInput.getText().toString().trim());
                // 收起键盘
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        searchClearBtn.setOnClickListener(v -> {
            searchInput.setText("");
            loadData();
        });
    }

    private void performSearch(String query) {
        passwordListView.reset();
        currentOffset = 0;
        if (query.isEmpty()) {
            loadData();
            return;
        }

        List<DataInfo> results = sqlClass.search(query);
        dataList.clear();
        if (results != null) {
            dataList.addAll(results);
        }
        adapter.setData(dataList);
        passwordListView.onLoadComplete(results != null && results.size() >= PAGE_SIZE);
        updateEmptyState();
    }

    // ==================== 列表 ====================

    private void setupListView() {
        passwordListView.setOnItemClickListener((parent, view, position, id) -> {
            DataInfo item = (DataInfo) parent.getItemAtPosition(position);
            if (item != null) {
                Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                intent.putExtra("id", item.getId());
                startActivity(intent);
            }
        });

        // 长按删除
        passwordListView.setOnItemLongClickListener((parent, view, position, id) -> {
            DataInfo item = (DataInfo) parent.getItemAtPosition(position);
            if (item != null) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.btn_delete)
                        .setMessage(getString(R.string.delete_confirm, item.getTitle()))
                        .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                            int deleted = sqlClass.deleteById(item.getId());
                            if (deleted > 0) {
                                refreshData();
                                Toast.makeText(MainActivity.this, R.string.delete_success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
            }
            return true;
        });

        passwordListView.setOnLoadMoreListener(() -> {
            // 分页加载：后台线程查询数据库，主线程更新 UI
            new Thread(() -> {
                List<DataInfo> page = sqlClass.getPage(currentOffset, PAGE_SIZE);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (page != null && !page.isEmpty()) {
                        adapter.addData(page);
                        currentOffset += page.size();
                    }
                    boolean hasMore = (page != null && page.size() == PAGE_SIZE);
                    passwordListView.onLoadComplete(hasMore);
                });
            }).start();
        });
    }

    // ==================== 数据加载 ====================

    private void loadData() {
        passwordListView.reset();
        currentOffset = 0;
        List<DataInfo> all = sqlClass.getAll();
        dataList.clear();
        if (all != null) {
            dataList.addAll(all);
        }
        adapter.setData(dataList);
        passwordListView.onLoadComplete(all != null && all.size() >= PAGE_SIZE);
        updateEmptyState();
    }

    private void refreshData() {
        // 保留搜索关键词不变，刷新数据
        String currentQuery = searchInput.getText().toString().trim();
        if (currentQuery.isEmpty()) {
            loadData();
        } else {
            performSearch(currentQuery);
        }
    }

    private void updateEmptyState() {
        if (dataList.isEmpty()) {
            passwordListView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            passwordListView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    // ==================== 菜单 ====================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_backup) {
            // FTP备份：总是先进入设置页，用户可在设置页修改配置或点击「上传备份」触发上传
            showFtpConfigDialog();
            return true;
        } else if (id == R.id.menu_oss_backup) {
            // OSS备份：总是先进入配置页（与FTP一致），用户可在配置页修改配置或点击「上传备份」触发上传
            showOssConfigDialog();
            return true;
        } else if (id == R.id.menu_export) {
            // 导出数据：选择格式（加密DB或明文JSON）
            showExportDialog();
            return true;
        } else if (id == R.id.menu_import) {
            // 导入数据：选择格式
            showImportDialog();
            return true;
        } else if (id == R.id.menu_about) {
            // 关于页面 — 自定义布局
            showAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ==================== FTP 备份 ====================

    private void performFtpBackup() {
        if (!FTPClientUtil.isConfigured(this)) {
            showFtpConfigDialog();
            return;
        }

        // 先导出加密数据库到缓存目录
        File cacheDir = new File(getCacheDir(), "ftp_backup");
        File dbFile = ImportExportUtil.exportEncryptedDb(this, cacheDir);
        if (dbFile == null) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        // 在后台线程执行FTP上传
        Toast.makeText(this, R.string.ftp_connecting, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            FTPClientUtil ftp = new FTPClientUtil(this);
            try {
                boolean connected = ftp.connect(this);
                if (!connected) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, R.string.ftp_failed, Toast.LENGTH_SHORT).show());
                    return;
                }

                FTPClientUtil.FTPConfig config = FTPClientUtil.getFtpConfig(this);
                ftp.changeDirectory(config.remoteDir);
                boolean uploaded = ftp.upload(dbFile, dbFile.getName());

                ftp.disconnect();

                boolean finalUploaded = uploaded;
                runOnUiThread(() -> {
                    if (finalUploaded) {
                        Toast.makeText(MainActivity.this, R.string.ftp_success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.ftp_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, R.string.ftp_failed, Toast.LENGTH_SHORT).show());
            } finally {
                ftp.disconnect();
                // 清理临时文件
                if (dbFile.exists()) dbFile.delete();
            }
        }).start();
    }

    private void showFtpConfigDialog() {
        Intent intent = new Intent(this, FtpConfigActivity.class);
        ftpConfigLauncher.launch(intent);
    }

    // ==================== OSS 备份 ====================

    private void performOssBackup() {
        // 先导出加密数据库到缓存目录
        File cacheDir = new File(getCacheDir(), "oss_backup");
        File dbFile = ImportExportUtil.exportEncryptedDb(this, cacheDir);
        if (dbFile == null) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        // 在后台线程执行 OSS 上传
        Toast.makeText(this, R.string.oss_connecting, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            OssClientUtil ossUtil = new OssClientUtil();
            try {
                ossUtil.initClient(this);
                OssClientUtil.OssConfig config = OssClientUtil.getOssConfig(this);
                String objectKey = OssClientUtil.buildObjectKey(config.remoteDir, dbFile.getName());
                boolean uploaded = ossUtil.uploadSync(this, objectKey, dbFile.getAbsolutePath());
                ossUtil.destroy();

                runOnUiThread(() -> {
                    if (uploaded) {
                        Toast.makeText(MainActivity.this, R.string.oss_test_success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.oss_test_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                ossUtil.destroy();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, R.string.oss_test_failed, Toast.LENGTH_SHORT).show());
            } finally {
                // 清理临时文件
                if (dbFile.exists()) dbFile.delete();
            }
        }).start();
    }

    private void showOssConfigDialog() {
        Intent intent = new Intent(this, OssConfigActivity.class);
        ossConfigLauncher.launch(intent);
    }

    // ==================== 关于 ====================

    private void showAboutDialog() {
        // 加载自定义布局
        View view = getLayoutInflater().inflate(R.layout.dialog_about, null);

        // 动态设置版本号（从 BuildConfig 获取）
        TextView versionValue = view.findViewById(R.id.about_version_value);
        versionValue.setText(BuildConfig.VERSION_NAME);

        // 设置作者（从 strings.xml 获取，也支持这里硬编码）
        TextView authorValue = view.findViewById(R.id.about_author_value);
        authorValue.setText(R.string.about_author_placeholder);

        // 设置开源地址（从 strings.xml 获取）
        TextView sourceValue = view.findViewById(R.id.about_source_value);
        sourceValue.setText(R.string.about_source_placeholder);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_about)
                .setView(view)
                .setPositiveButton(R.string.btn_confirm, null);
        builder.show();
    }

    // ==================== 导出 ====================

    private void showExportDialog() {
        String[] formats = {getString(R.string.format_encrypted_db), getString(R.string.format_json)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.export_format)
                .setItems(formats, (dialog, which) -> {
                    if (which == 0) {
                        exportEncryptedDb();
                    } else {
                        exportJson();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void exportEncryptedDb() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download");
        File result = ImportExportUtil.exportEncryptedDb(this, dir);
        if (result != null) {
            Toast.makeText(this, getString(R.string.export_success) + "\n" + result.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void exportJson() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download");
        File result = ImportExportUtil.exportJson(this, dir, sqlClass);
        if (result != null) {
            Toast.makeText(this, getString(R.string.export_success) + "\n" + result.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== 导入 ====================

    private void showImportDialog() {
        String[] formats = {getString(R.string.format_encrypted_db), getString(R.string.format_json)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_import)
                .setItems(formats, (dialog, which) -> {
                    if (which == 0) {
                        pickAndImportDb();
                    } else {
                        pickAndImportJson();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void pickAndImportDb() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.import_confirm)
                .setMessage(R.string.import_confirm)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    // 使用文件选择器选择 .db 文件
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/octet-stream");
                    filePickLauncher.launch(intent);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void pickAndImportJson() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        filePickLauncher.launch(intent);
    }

    // ==================== 文件选择回调（ActivityResult API） ====================

    private androidx.activity.result.ActivityResultLauncher<Intent> filePickLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            android.net.Uri uri = result.getData().getData();
                            if (uri != null) {
                                importFromUri(uri);
                            }
                        }
                    });

    private void importFromUri(android.net.Uri uri) {
        // 将 URI 内容复制到缓存文件
        try {
            String fileName = "import_temp";
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null) {
                if (mimeType.contains("json")) {
                    fileName = "import_temp.json";
                } else {
                    fileName = "import_temp.db";
                }
            }

            File cacheFile = new File(getCacheDir(), fileName);
            try (java.io.InputStream is = getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile)) {
                if (is == null) {
                    Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
            }

            int count;
            if (fileName.endsWith(".json")) {
                count = ImportExportUtil.importJson(this, cacheFile, sqlClass);
            } else {
                count = ImportExportUtil.importEncryptedDb(this, cacheFile);
            }

            if (count >= 0) {
                Toast.makeText(this,
                        getString(R.string.import_success, count), Toast.LENGTH_LONG).show();
                refreshData();
            } else {
                Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
            }

            cacheFile.delete();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
