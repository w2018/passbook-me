package top.zw.passwd;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 密码卡片列表适配器
 * 绑定 item_password_card.xml 布局，展示 DataInfo 列表。
 */
public class PasswordListAdapter extends BaseAdapter {

    private Context context;
    private List<DataInfo> dataList;

    public PasswordListAdapter(Context context) {
        this.context = context;
        this.dataList = new ArrayList<>();
    }

    public PasswordListAdapter(Context context, List<DataInfo> dataList) {
        this.context = context;
        this.dataList = dataList != null ? dataList : new ArrayList<DataInfo>();
    }

    /**
     * 替换全部数据并刷新列表
     */
    public void setData(List<DataInfo> dataList) {
        this.dataList = dataList != null ? dataList : new ArrayList<DataInfo>();
        notifyDataSetChanged();
    }

    /**
     * 追加数据（分页加载时使用）
     */
    public void addData(List<DataInfo> moreData) {
        if (moreData != null && !moreData.isEmpty()) {
            this.dataList.addAll(moreData);
            notifyDataSetChanged();
        }
    }

    /**
     * 获取当前数据副本
     */
    public List<DataInfo> getData() {
        return new ArrayList<>(dataList);
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public DataInfo getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return dataList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_password_card, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.item_icon);
            holder.title = convertView.findViewById(R.id.item_title);
            holder.subtitle = convertView.findViewById(R.id.item_subtitle);
            holder.arrow = convertView.findViewById(R.id.item_arrow);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DataInfo info = dataList.get(position);
        holder.title.setText(info.getTitle() != null ? info.getTitle() : "");

        // 副标题优先级：用户名 > 邮箱 > 网址 > 空
        String subtitle = info.getLoginUser();
        if (subtitle == null || subtitle.isEmpty()) {
            subtitle = info.getLoginMail();
        }
        if (subtitle == null || subtitle.isEmpty()) {
            subtitle = info.getRegAddr();
        }
        holder.subtitle.setText(subtitle != null ? subtitle : "");

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView title;
        TextView subtitle;
        ImageView arrow;
    }
}