package top.zw.passwd;

import android.content.ContentValues;
import android.util.Log;

import net.sqlcipher.Cursor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 参数化查询 CRUD 操作类
 * 全部使用 ? 占位符 + selectionArgs 消除 SQL 注入风险
 */
public class SqlClass {

    private static final String TAG = "SqlClass";
    private final MyDatabaseHelper dbHelper;

    public SqlClass() {
        this.dbHelper = MyDatabaseHelper.getInstance();
    }

    /**
     * 插入一条密码记录
     * @return 新行的 _id，失败返回 -1
     */
    public long insert(DataInfo info) {
        ContentValues values = dataInfoToContentValues(info);
        long id = dbHelper.insertPassword(values);
        if (id != -1) {
            Log.d(TAG, "Inserted record id=" + id + ", title=" + info.getTitle());
        } else {
            Log.e(TAG, "Insert failed: " + info.getTitle());
        }
        return id;
    }

    /**
     * 根据 _id 更新一条记录的全部字段
     * @return 更新的行数
     */
    public int update(DataInfo info) {
        ContentValues values = dataInfoToContentValues(info);
        int rows = dbHelper.updatePassword(values,
                MyDatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(info.getId())});
        Log.d(TAG, "Updated " + rows + " rows, id=" + info.getId());
        return rows;
    }

    /**
     * 根据 _id 删除记录
     */
    public int deleteById(int id) {
        int rows = dbHelper.deletePassword(
                MyDatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)});
        Log.d(TAG, "Deleted " + rows + " rows, id=" + id);
        return rows;
    }

    /**
     * 删除全部记录
     */
    public int deleteAll() {
        int rows = dbHelper.deletePassword(null, null);
        Log.d(TAG, "Deleted all " + rows + " rows");
        return rows;
    }

    /**
     * 根据 _id 查询单条记录
     */
    public DataInfo getById(int id) {
        Cursor cursor = dbHelper.queryPassword(
                null,
                MyDatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)},
                null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursorToDataInfo(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 查询全部记录，按 _id 降序
     */
    public List<DataInfo> getAll() {
        return queryList(null, null, MyDatabaseHelper.COL_ID + " DESC");
    }

    /**
     * 按标题模糊搜索（参数化 LIKE）
     */
    public List<DataInfo> searchByTitle(String keyword) {
        return queryList(
                MyDatabaseHelper.COL_TITLE + " LIKE ?",
                new String[]{"%" + keyword + "%"},
                MyDatabaseHelper.COL_ID + " DESC");
    }

    /**
     * 按用户名模糊搜索
     */
    public List<DataInfo> searchByUser(String keyword) {
        return queryList(
                MyDatabaseHelper.COL_LOGIN_USER + " LIKE ?",
                new String[]{"%" + keyword + "%"},
                MyDatabaseHelper.COL_ID + " DESC");
    }

    /**
     * 全局搜索：匹配标题 OR 用户名 OR 网址
     */
    public List<DataInfo> search(String keyword) {
        String likePattern = "%" + keyword + "%";
        String selection = MyDatabaseHelper.COL_TITLE + " LIKE ? OR " +
                MyDatabaseHelper.COL_LOGIN_USER + " LIKE ? OR " +
                MyDatabaseHelper.COL_REG_ADDR + " LIKE ?";
        String[] selectionArgs = new String[]{likePattern, likePattern, likePattern};
        return queryList(selection, selectionArgs, MyDatabaseHelper.COL_ID + " DESC");
    }

    /**
     * 分页查询
     */
    public List<DataInfo> getPage(int offset, int limit) {
        String sql = "SELECT * FROM " + MyDatabaseHelper.TABLE_PASSWORD +
                " ORDER BY " + MyDatabaseHelper.COL_ID + " DESC LIMIT ? OFFSET ?";
        Cursor cursor = dbHelper.rawQuery(sql, new String[]{
                String.valueOf(limit), String.valueOf(offset)});
        return cursorToList(cursor);
    }

    /**
     * 获取总记录数
     */
    public long getCount() {
        return dbHelper.getRecordCount();
    }

    /**
     * 检查记录是否存在（按标题精确匹配）
     */
    public boolean existsByTitle(String title) {
        Cursor cursor = dbHelper.queryPassword(
                new String[]{MyDatabaseHelper.COL_ID},
                MyDatabaseHelper.COL_TITLE + "=?",
                new String[]{title},
                null);
        if (cursor != null) {
            try {
                return cursor.getCount() > 0;
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    // ========== 内部辅助方法 ==========

    /**
     * 通用参数化查询 → List
     */
    private List<DataInfo> queryList(String selection, String[] selectionArgs, String orderBy) {
        Cursor cursor = dbHelper.queryPassword(null, selection, selectionArgs, orderBy);
        return cursorToList(cursor);
    }

    /**
     * Cursor → List<DataInfo>
     */
    private List<DataInfo> cursorToList(Cursor cursor) {
        List<DataInfo> list = new ArrayList<>();
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    list.add(cursorToDataInfo(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return list;
    }

    /**
     * Cursor 当前行 → DataInfo 对象
     */
    private DataInfo cursorToDataInfo(Cursor cursor) {
        DataInfo info = new DataInfo();
        info.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COL_ID)));
        info.setTitle(getString(cursor, MyDatabaseHelper.COL_TITLE));
        info.setRegAddr(getString(cursor, MyDatabaseHelper.COL_REG_ADDR));
        info.setLoginUser(getString(cursor, MyDatabaseHelper.COL_LOGIN_USER));
        info.setLoginPwd(getString(cursor, MyDatabaseHelper.COL_LOGIN_PWD));
        info.setLoginMail(getString(cursor, MyDatabaseHelper.COL_LOGIN_MAIL));
        info.setLoginPhone(getString(cursor, MyDatabaseHelper.COL_LOGIN_PHONE));
        info.setRemarks(getString(cursor, MyDatabaseHelper.COL_REMARKS));
        info.setIdTime(getString(cursor, MyDatabaseHelper.COL_ID_TIME));
        info.setUpdateTime(getString(cursor, MyDatabaseHelper.COL_UPDATE_TIME));
        return info;
    }

    private String getString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.isNull(index) ? "" : cursor.getString(index);
    }

    /**
     * DataInfo → ContentValues（参数化插入/更新用）
     */
    private ContentValues dataInfoToContentValues(DataInfo info) {
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.COL_TITLE, info.getTitle());
        values.put(MyDatabaseHelper.COL_REG_ADDR, info.getRegAddr());
        values.put(MyDatabaseHelper.COL_LOGIN_USER, info.getLoginUser());
        values.put(MyDatabaseHelper.COL_LOGIN_PWD, info.getLoginPwd());
        values.put(MyDatabaseHelper.COL_LOGIN_MAIL, info.getLoginMail());
        values.put(MyDatabaseHelper.COL_LOGIN_PHONE, info.getLoginPhone());
        values.put(MyDatabaseHelper.COL_REMARKS, info.getRemarks());

        // 如果 idTime 为空则自动生成时间戳
        String idTime = info.getIdTime();
        if (idTime == null || idTime.isEmpty()) {
            idTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());
        }
        values.put(MyDatabaseHelper.COL_ID_TIME, idTime);

        // updateTime 始终设为当前时间
        String updateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        values.put(MyDatabaseHelper.COL_UPDATE_TIME, updateTime);

        return values;
    }
}