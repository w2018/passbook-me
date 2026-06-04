package top.zw.passwd;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * 主题管理器
 * 管理亮/暗主题切换，持久化到 SharedPreferences
 */
public class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    /**
     * 主题模式枚举
     */
    public enum ThemeMode {
        /** 跟随系统 */
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        /** 浅色主题 */
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        /** 深色主题 */
        DARK(AppCompatDelegate.MODE_NIGHT_YES);

        final int delegateMode;

        ThemeMode(int delegateMode) {
            this.delegateMode = delegateMode;
        }
    }

    private ThemeManager() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * 应用主题设置
     * 应在 Activity.onCreate() 中 super.onCreate() 之前调用
     */
    public static void applyTheme(Context context) {
        ThemeMode mode = getThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode);
    }

    /**
     * 设置并应用主题模式
     */
    public static void setThemeMode(Context context, ThemeMode mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal()).apply();
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode);
    }

    /**
     * 获取当前主题模式
     */
    public static ThemeMode getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int ordinal = prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal());
        ThemeMode[] values = ThemeMode.values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return ThemeMode.SYSTEM;
    }

    /**
     * 切换主题（在三种模式间循环：SYSTEM → LIGHT → DARK → SYSTEM）
     */
    public static ThemeMode cycleTheme(Context context) {
        ThemeMode current = getThemeMode(context);
        ThemeMode[] values = ThemeMode.values();
        ThemeMode next = values[(current.ordinal() + 1) % values.length];
        setThemeMode(context, next);
        return next;
    }

    /**
     * 获取主题模式显示名称
     */
    public static String getThemeDisplayName(Context context) {
        ThemeMode mode = getThemeMode(context);
        switch (mode) {
            case SYSTEM:
                return "跟随系统";
            case LIGHT:
                return "浅色主题";
            case DARK:
                return "深色主题";
            default:
                return "未知";
        }
    }

    /**
     * 判断当前是否为深色主题
     */
    public static boolean isDarkTheme(Context context) {
        ThemeMode mode = getThemeMode(context);
        if (mode == ThemeMode.DARK) return true;
        if (mode == ThemeMode.SYSTEM) {
            int nightMode = context.getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        return false;
    }
}