package top.zw.passwd;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 支持自动分页加载的 ListView。
 * 滑动到底部时自动触发加载回调，底部显示加载指示器。
 *
 * 使用方式：
 * 1. 在布局中声明本控件替换普通 ListView
 * 2. setOnLoadMoreListener 注册回调
 * 3. 回调中执行异步加载，完成后调用 onLoadComplete(hasMore)
 * 4. 搜索/刷新时调用 reset() 重置分页状态
 */
public class LoadMoreListView extends ListView implements AbsListView.OnScrollListener {

    private OnLoadMoreListener onLoadMoreListener;
    private View footerView;
    private ProgressBar footerProgress;
    private TextView footerText;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean enableAutoLoad = true;

    /** 触发加载的阈值：剩余可见项数 <= 此值时触发 */
    private static final int LOAD_THRESHOLD = 2;

    public interface OnLoadMoreListener {
        /** 列表滚动到底部，应加载更多数据 */
        void onLoadMore();
    }

    public LoadMoreListView(Context context) {
        super(context);
        init(context);
    }

    public LoadMoreListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadMoreListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // ==================== 初始化 ====================

    private void init(Context context) {
        // 构建底部加载指示器：LinearLayout(ProgressBar + TextView)
        LinearLayout footerLayout = new LinearLayout(context);
        footerLayout.setOrientation(LinearLayout.HORIZONTAL);
        footerLayout.setGravity(android.view.Gravity.CENTER);
        footerLayout.setMinimumHeight(dpToPx(context, 48));
        footerLayout.setPadding(0, dpToPx(context, 12), 0, dpToPx(context, 12));

        footerProgress = new ProgressBar(context);
        footerProgress.setIndeterminate(true);
        int progressSize = dpToPx(context, 20);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(
                progressSize, progressSize);
        footerProgress.setLayoutParams(progressLp);
        footerLayout.addView(footerProgress);

        footerText = new TextView(context);
        footerText.setTextSize(14);
        footerText.setTextColor(0xFF9E9E9E); // 灰色文字
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        textLp.setMargins(dpToPx(context, 8), 0, 0, 0);
        footerText.setLayoutParams(textLp);
        footerLayout.addView(footerText);

        footerView = footerLayout;
        footerView.setVisibility(View.GONE);

        // 关键：addFooterView 必须在 setAdapter 之前调用
        // 这里先添加，后续通过 setVisibility 控制显隐
        addFooterView(footerView, null, false);

        setOnScrollListener(this);
    }

    // ==================== 公开 API ====================

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.onLoadMoreListener = listener;
    }

    /**
     * 标记一次加载完成。
     * @param moreDataAvailable 是否还有更多数据可加载
     */
    public void onLoadComplete(boolean moreDataAvailable) {
        this.hasMore = moreDataAvailable;
        this.isLoading = false;
        updateFooter();
    }

    /**
     * 加载失败时调用，恢复状态以便重试。
     */
    public void onLoadError() {
        this.isLoading = false;
        footerText.setText("加载失败，上拉重试");
        footerProgress.setVisibility(View.GONE);
        footerView.setVisibility(View.VISIBLE);
        // 允许再次触发加载
        enableAutoLoad = true;
    }

    /**
     * 重置分页状态（搜索/刷新时调用）。
     */
    public void reset() {
        this.hasMore = true;
        this.isLoading = false;
        this.enableAutoLoad = true;
        updateFooter();
    }

    /**
     * 设置底部文字（例如"加载中…"、"没有更多了"）。
     */
    public void setFooterText(String text) {
        footerText.setText(text);
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean hasMore() {
        return hasMore;
    }

    // ==================== 底部指示器 ====================

    private void updateFooter() {
        if (footerView == null) return;

        if (isLoading) {
            footerProgress.setVisibility(View.VISIBLE);
            footerText.setText("加载中…");
            footerView.setVisibility(View.VISIBLE);
        } else if (!hasMore) {
            footerProgress.setVisibility(View.GONE);
            footerText.setText("— 没有更多了 —");
            footerView.setVisibility(View.VISIBLE);
        } else {
            // 隐藏 footer（还有更多但未加载中）
            footerView.setVisibility(View.GONE);
        }
    }

    // ==================== 滑动监听 ====================

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // 空闲状态时检查是否需要加载（实现"松手后触发"的体验）
        if (scrollState == SCROLL_STATE_IDLE && enableAutoLoad) {
            checkLoadMore(view);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        // 保留此回调以兼容旧接口，主要检测逻辑在 onScrollStateChanged
        // 同时也做实时检测（某些场景下 SCROLL_STATE_IDLE 可能不触发）
        if (enableAutoLoad && visibleItemCount > 0 && totalItemCount > 0) {
            if ((firstVisibleItem + visibleItemCount) >= (totalItemCount - LOAD_THRESHOLD)) {
                triggerLoad();
            }
        }
    }

    private void checkLoadMore(AbsListView view) {
        int firstVisibleItem = view.getFirstVisiblePosition();
        int visibleItemCount = view.getChildCount();
        int totalItemCount = view.getCount();

        if (totalItemCount == 0) return;

        // 最后一个可见项接近列表底部时触发
        if ((firstVisibleItem + visibleItemCount) >= (totalItemCount - LOAD_THRESHOLD)) {
            triggerLoad();
        }
    }

    private void triggerLoad() {
        if (onLoadMoreListener != null && hasMore && !isLoading) {
            isLoading = true;
            updateFooter();
            onLoadMoreListener.onLoadMore();
        }
    }

    // ==================== 工具方法 ====================

    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
