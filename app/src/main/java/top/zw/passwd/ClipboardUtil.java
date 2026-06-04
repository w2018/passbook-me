package top.zw.passwd;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

/**
 * 剪贴板工具类
 * 兼容 Android 12+ 的剪贴板读取限制
 */
public class ClipboardUtil {

    private ClipboardUtil() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * 复制文本到剪贴板
     *
     * @param context 上下文
     * @param label   标签（用于剪贴板 UI 显示）
     * @param text    要复制的文本
     */
    public static void copy(Context context, String label, String text) {
        ClipboardManager cm = getClipboardManager(context);
        if (cm == null) return;
        ClipData clip = ClipData.newPlainText(label, text);
        cm.setPrimaryClip(clip);
        // Android 12+ 自动弹出复制确认提示
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            Toast.makeText(context, "已复制: " + label, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制文本到剪贴板（使用默认标签）
     */
    public static void copy(Context context, String text) {
        copy(context, "PasswordVault", text);
    }

    /**
     * 清空剪贴板
     * 安全目的：清除可能残留的敏感密码数据
     */
    public static void clear(Context context) {
        ClipboardManager cm = getClipboardManager(context);
        if (cm == null) return;
        ClipData empty = ClipData.newPlainText("", "");
        cm.setPrimaryClip(empty);
    }

    /**
     * 从剪贴板读取文本（Android 12+ 需用户授权）
     *
     * @return 剪贴板纯文本内容，为空或非文本时返回 null
     */
    public static String getText(Context context) {
        ClipboardManager cm = getClipboardManager(context);
        if (cm == null || !cm.hasPrimaryClip()) return null;
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return null;
        CharSequence text = clip.getItemAt(0).getText();
        return text != null ? text.toString() : null;
    }

    private static ClipboardManager getClipboardManager(Context context) {
        return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }
}
