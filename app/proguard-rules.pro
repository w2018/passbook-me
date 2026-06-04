# 个人密码薄 ProGuard 混淆规则

# 数据模型 - 保持全部字段
-keep class top.zw.passwd.DataInfo { *; }

# 自定义ListView控件
-keep class top.zw.passwd.LoadMoreListView { *; }

# FTP工具类 (包含自定义对话框)
-keep class top.zw.passwd.FTPClientUtil { *; }

# SQLCipher 加密数据库 (JNI 核心库)
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# SQLCipher Android 封装 (net.sqlcipher)
# 保持所有类和成员不被移除/混淆（mNativeHandle 等 JNI 字段依赖反射/成员保留）
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# 确保 SQLCipher JNI native 方法不被移除
-keepclasseswithmembernames class net.sqlcipher.database.SQLiteDatabase {
    native <methods>;
    long mNativeHandle;
    long mNativePtr;
}

# Serializable 实现类
-keepclassmembers class * implements java.io.Serializable { *; }

# JSON解析 (org.json)
-keep class org.json.** { *; }

# 保持注解
-keepattributes *Annotation*

# Commons Net (FTP)
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

# EncryptedSharedPreferences (Security Crypto)
-keep class androidx.security.crypto.** { *; }

# ViewBinding 保留
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static * bind(android.view.View);
    public static * inflate(...);
}

# R8 全压缩模式 - 保留所有 Application/Activity 入口
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# 保留 R 文件中的资源 ID
-keepclassmembers class **.R$* { public static <fields>; }

# Gson/反射 安全
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Kotlin 兼容
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# 保留枚举
-keepclassmembers enum * { *; }