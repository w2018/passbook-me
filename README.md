# 个人密码薄 (Personal Password Book)

> **全程使用 Operit AI 开发**  
> 本项目为个人学习项目，仅供参考，请勿用于非法用途。  
> **版权说明：个人学习仅供参考，未经授权禁止商业使用。**

---

## 项目简介

**个人密码薄** 是一款基于 Android 平台的本地离线密码管理工具，使用 **SQLCipher** 对数据进行 AES-256 加密存储，保障用户隐私安全。所有密码数据仅存储在设备本地，无云端同步，用户可自主通过 FTP、阿里云OSS 备份或导入/导出功能进行数据迁移。

- **包名：** `top.zw.passwd`
- **版本：** v1.1（`compileSdk 34` / `minSdk 21` / `targetSdk 34`）
- **数据库引擎：** SQLCipher 3.5.6（加密 SQLite）
- **UI 框架：** Material Design 3 + AndroidX

---

## 软件结构

```
passwd_v2/
├── app/
│   ├── build.gradle              # 模块构建配置（依赖、SDK版本）
│   ├── proguard-rules.pro        # 混淆规则
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml    # 应用清单（权限、组件注册）
│           ├── java/top/zw/passwd/    # 源代码（21个Java类）
│           ├── res/
│           │   ├── drawable/          # 矢量图标资源（22个XML）
│           │   ├── layout/            # 布局文件（12个XML）
│           │   ├── menu/              # 菜单栏定义（2个XML）
│           │   ├── mipmap*/           # 启动图标（多密度适配）
│           │   ├── values/
│           │   │   ├── colors.xml     # 颜色主题配置
│           │   │   ├── strings.xml    # 字符串资源
│           │   │   └── themes.xml     # 主题样式定义
│           └── ...
├── build.gradle                  # 根项目构建配置
├── settings.gradle               # 项目模块设置
├── gradle.properties             # Gradle 属性
└── README.md                     # 本文件
```

### 核心源码模块（21个Java类）

| 模块 | 文件 | 功能说明 |
|------|------|----------|
| **应用入口** | `MyApplication.java` | Application 初始化，提供全局 Context、数据库路径、加密密码 |
| **主界面** | `MainActivity.java` | 密码列表展示、搜索（500ms防抖）、分页加载（每页20条）、FTP备份、导入导出、关于对话框 |
| **密码详情** | `PlayActivity.java` | 查看密码完整信息，支持一键复制、编辑、删除、分享 |
| **编辑密码** | `EditRecordActivity.java` | 编辑密码记录（标题、网址、用户名、密码、邮箱、手机号、备注） |
| **安全验证** | `VerifyPasswordActivity.java` | 启动时验证主密码 |
| **设置密码** | `SetPasswordActivity.java` | 首次使用设置主密码 |
| **密保问题** | `SecurityQuestionActivity.java` | 设置密保问题及答案（用于密码找回） |
| **密保验证** | `VerifyQuestionActivity.java` | 通过密保问题验证身份 |
| **数据模型** | `DataInfo.java` | 密码记录实体类（含_id、title、loginUser、loginPwd等字段） |
| **数据库帮助器** | `MyDatabaseHelper.java` | SQLCipher 加密数据库的单例封装，参数化查询防SQL注入，支持v1→v2迁移（增加updateTime字段） |
| **数据库操作** | `SqlClass.java` | 数据库CRUD操作封装层 |
| **列表适配器** | `PasswordListAdapter.java` | RecyclerView适配器，绑定密码卡片视图 |
| **加载更多** | `LoadMoreListView.java` | 自定义上拉加载更多组件 |
| **导入导出** | `ImportExportUtil.java` | 支持加密DB文件导出/导入、JSON格式明文导出/导入 |
| **FTP配置** | `FtpConfigActivity.java` | 配置FTP服务器地址、端口、路径、凭证 |
| **FTP客户端** | `FTPClientUtil.java` | Apache Commons Net封装，支持FTP上传加密数据库备份 |
| **主题管理** | `ThemeManager.java` | 日间/夜间主题切换管理 |
| **剪贴板工具** | `ClipboardUtil.java` | 文本复制到系统剪贴板 |

---

## 功能特性

### 🔐 安全体系
- **主密码保护**：应用启动时需验证主密码，防止未授权访问
- **SQLCipher 加密**：所有数据使用 AES-256 加密存储，数据库文件无法直接读取
- **密保问题找回**：设置密保问题及答案，忘记主密码时可安全找回
- **参数化查询**：所有数据库操作使用参数化查询，杜绝 SQL 注入风险

### 📝 密码管理
- **完整CRUD**：新增、查看、编辑、删除密码记录
- **丰富字段**：每条记录包含标题、网址、用户名、密码、邮箱、手机号、备注
- **搜索功能**：实时搜索标题，500ms防抖避免频繁查询
- **分页加载**：每页加载20条记录，上拉自动加载更多
- **一键复制**：点击字段即可复制到剪贴板
- **分享功能**：可将密码信息分享给其他应用

### 💾 数据备份与迁移
- **OSS云备份**：支持阿里云OSS，一键上传加密数据库至云端存储
- **FTP备份**：配置FTP服务器后，一键上传加密数据库文件至远程服务器
- **导入/导出**：
  - 加密DB格式：完整导出加密数据库文件（含密码保护）
  - JSON格式：导出为纯文本JSON，便于人工查看或迁移

### 🎨 界面体验
- **Material Design 3**：现代化UI设计，圆角卡片布局
- **自适应主题**：支持日间/夜间模式切换
- **交互反馈**：操作成功/失败均有Snackbar提示
- **手势操作**：长按删除、点击查看详情

---

## 技术栈

| 技术 | 用途 |
|------|------|
| **Android SDK 34** | 目标平台版本 |
| **AndroidX** | 核心库、Activity、Fragment、RecyclerView |
| **Material Design 3** | UI 组件库（1.5.0） |
| **SQLCipher** | AES-256 加密数据库（3.5.6） |
| **Apache Commons Net** | FTP 客户端（3.6） |
| **Security Crypto** | EncryptedSharedPreferences 加密存储配置 |
| **ViewBinding** | 视图绑定，安全类型化访问控件 |
| **ProGuard** | 代码混淆与优化 |

---

## 构建与安装

```bash
# 调试构建
./gradlew assembleDebug

# 产物路径
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 使用引导

1. **首次启动**：设置主密码 → 设置密保问题（可选）
2. **添加密码**：点击右下角"+"按钮 → 填写信息 → 保存
3. **查看详情**：点击列表中的记录 → 查看完整信息
4. **搜索记录**：顶部搜索框输入关键词 → 自动过滤
5. **数据备份**：菜单 → XXX备份 → 配置服务器 → 上传
6. **数据导出**：菜单 → 导出数据 → 选择格式（DB/JSON）
7. **数据导入**：菜单 → 导入数据 → 选择文件

---

## 免责声明

- 本项目为 **个人学习用途**，仅供参考
- 请勿将本项目用于存储任何违法或敏感信息
- 作者不对因使用本软件造成的任何数据丢失或泄露承担责任
- 建议定期通过FTP、OSS或导出功能备份您的密码数据

---

## 发布信息

- **发布名：** `passbook-me`
- **当前版本：** v1.1
