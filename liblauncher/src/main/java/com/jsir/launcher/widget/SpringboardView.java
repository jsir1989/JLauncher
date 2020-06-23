package com.jsir.launcher.widget;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.jsir.launcher.bean.FavoritesItem;
import com.jsir.launcher.manager.FingerFlowViewManager;
import com.jsir.launcher.adapter.SpringboardAdapter;
import com.jsir.launcher.library.R;


/**
 * 这个类提供布局相关,各种监听的设置和基本事件的处理
 * <p/>
 * 具体的各种触摸事件处理请使用子类
 */
public abstract class SpringboardView extends ViewGroup {

    //    #页面相关
    /**
     * callback onPageCountChange() at totalPage count change
     * callback onPageScroll() at mCurScreen change
     * 在页面数量和当面页面发生变化时回调
     */
    private OnPageChangedListener onPageChangedListener;
    /**
     * 适配器
     */
    protected SpringboardAdapter adapter;
    /**
     * 总页面数量
     * will count totalPage at setAdapter,merge into Folder,delete item,add item,remove out Folder
     */
    private int totalPage = 0;
    /**
     * 现在页面
     */
    private int curScreen = 0;


//    #触摸相关
    /**
     * 开始时的x坐标
     */
    private int startX = 0;
    /**
     * 速度计算
     */
    private VelocityTracker velocityTracker;
    /**
     * 最后的触摸位置X
     */
    private float lastMotionX;
    /**
     * 最后的触摸位置Y
     */
    private float lastMotionY;
    /**
     * 触摸点距离View左边的距离
     */
    private int dragPointX;
    /**
     * 触摸点距离View顶部的距离
     */
    private int dragPointY;
    /**
     * view左边距离屏幕的距离
     */
    private int dragOffsetX;
    /**
     * view上边距离屏幕的距离
     */
    private int dragOffsetY;
    /**
     * 当前手指触摸的位置
     */
    protected int dragPosition = -1;
    /**
     * 拖动开始时和顺序交换之后被拖动View的位置
     */
    protected int temChangPosition = -1;
    protected Scroller scroller;
    private int touchSlop;
    private int minimumVelocity;
    private int maximumVelocity;
    private MODE mode = MODE.FREE;

    enum MODE {
        //普通时,按钮被长按之后,进入这个模式,滚动模式
        FREE, DRAGGING, SCROLL
    }


//  #各种属性
    /**
     * 删除按钮
     */
    private Drawable deleteIcon;
    /**
     * 几行
     */
    private int rowCount;
    /**
     * 几列
     */
    private int colCount;
    /**
     * 合并按钮,产生文件夹时的默认按钮
     */
    protected String defaultFolderName;
    protected int dividerWidth;
    protected int dividerColor;
    private boolean isLastItemStable;
    private int stableHeaderCount;
    protected int pageDividerWidth;
    private boolean enableShake = false;

    private Paint paint;
    protected LayoutTransition mLayoutTransition;

    public SpringboardView(Context context) {
        this(context, null);
    }

    public SpringboardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.StyleSpringboard);
    }

    public SpringboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSpringboardView(context, attrs, defStyle, 0);
    }

    private void initSpringboardView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.Springboard, defStyleAttr, defStyleRes);
        rowCount = a.getInt(R.styleable.Springboard_row_count, 3);
        colCount = a.getInt(R.styleable.Springboard_column_count, 3);
        defaultFolderName = a.getString(R.styleable.Springboard_default_folder_name);
        dividerWidth = a.getDimensionPixelOffset(R.styleable.Springboard_divider_width, 0);
        dividerColor = a.getColor(R.styleable.Springboard_divider_color, 0);
        isLastItemStable = a.getBoolean(R.styleable.Springboard_is_last_item_stable, false);
        stableHeaderCount = a.getInt(R.styleable.Springboard_stable_header_count, 0);
        pageDividerWidth = a.getDimensionPixelOffset(R.styleable.Springboard_page_divider_width, 0);
        enableShake = a.getBoolean(R.styleable.Springboard_enable_shake, false);

        if (dividerWidth != 0 && dividerColor != 0) {
            setWillNotDraw(false);
        }
        a.recycle();
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.scroller = new Scroller(getContext());
        FingerFlowViewManager.getInstance().init(getContext());
        setOverScrollMode(OVER_SCROLL_ALWAYS);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        touchSlop = configuration.getScaledTouchSlop();
        minimumVelocity = 700;
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLayoutTransition = new LayoutTransition();
        mLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, null);
        mLayoutTransition.setAnimator(LayoutTransition.APPEARING, null);
        setLayoutTransition(mLayoutTransition);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int childHeight;
        int measuredHeight;
        //父给出的宽度,不管什么模式用完
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        //可以使用的宽度
        int usedWidth = width - getPaddingLeft() - getPaddingRight() - (colCount + 1) * dividerWidth;

        int childWidth = usedWidth / colCount;

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        //如果有准确高度,按准确高度计算.
        if (heightMode == MeasureSpec.EXACTLY) {
            int usedHeight = height - getPaddingTop() - getPaddingBottom() - (rowCount + 1) * dividerWidth;
            childHeight = usedHeight / rowCount;
            measuredHeight = height;

        } else {
            childHeight = usedWidth / colCount + 50;
            measuredHeight = childHeight * rowCount + (rowCount + 1) * dividerWidth + getPaddingTop() + getPaddingBottom();

            if (heightMode == MeasureSpec.AT_MOST) {
                if (measuredHeight > height) {
                    measuredHeight = height;
                }
            }
        }

        int childWidthSpec = getChildMeasureSpec(
                MeasureSpec
                        .makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                0, childWidth);

        int childHeightSpec = getChildMeasureSpec(
                MeasureSpec
                        .makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                0, childHeight);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.measure(childWidthSpec, childHeightSpec);

        }
        setMeasuredDimension(width, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();

        int measuredWidth = getMeasuredWidth();

        for (int i = 0; i < childCount; i++) {
            final View childView = getChildAt(i);
            if (childView.getVisibility() != View.GONE) {
                int page = i / rowCount / colCount;
                int row = i / colCount % rowCount;
                int col = i % colCount;
                int left = getPaddingLeft() + page * (measuredWidth + pageDividerWidth) + col
                        * (dividerWidth + childView.getMeasuredWidth()) + dividerWidth;
                int top = getPaddingTop() + dividerWidth + row * (dividerWidth + childView.getMeasuredHeight());
                childView.layout(left, top, left + childView.getMeasuredWidth(), top + childView.getMeasuredHeight());

            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(dividerColor);
        int childHeight = getChildHeight();
        int childWidth = getChildWidth();
        int rowCount = getRowCount() + 1;

        int pageCount = getTotalPage();

        for (int j = 0; j < pageCount; j++) {
            for (int i = 0; i < rowCount; i++) {
                int count = (childHeight + dividerWidth) * i;
                canvas.drawRect(j * (getWidth() + pageDividerWidth), count, (j + 1) * getWidth() + j * pageDividerWidth, count + dividerWidth, paint);
            }

            int colCount = getColCount() + 1;
            for (int i = 0; i < colCount; i++) {
                int count = (childWidth + dividerWidth) * i + j * (getWidth() + pageDividerWidth);
                canvas.drawRect(count, 0, count + dividerWidth, getHeight(), paint);
            }
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        if ((action == MotionEvent.ACTION_MOVE) && (mode == MODE.DRAGGING)) {
            initVelocityTrackerIfNotExists();
            velocityTracker.addMovement(ev);
            return true;
        }

        float x = ev.getX();
        float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startX = (int) x;
                dragOffsetX = (int) (ev.getRawX() - x);
                dragOffsetY = (int) (ev.getRawY() - y);
                lastMotionX = x;
                lastMotionY = y;
                initOrResetVelocityTracker();
                velocityTracker.addMovement(ev);
                stopScroll();
                if (adapter != null) {
                    adapter.setIsTouching(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                initVelocityTrackerIfNotExists();
                velocityTracker.addMovement(ev);
                int deltaX = (int) (lastMotionX - x);
                if (ifCanScroll(deltaX)) {
                    mode = MODE.SCROLL;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    recycleVelocityTracker();
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                getAdapter().setEditing(false);
            case MotionEvent.ACTION_UP:
                if (adapter != null) {
                    adapter.setIsTouching(false);
                }
                if (mode == MODE.DRAGGING) {
                    dragPosition = pointToPosition((int) x, (int) y);
                    onDragFinished(ev);
                    endDrag();
                } else if (mode == MODE.SCROLL) {
                    float distance = ev.getRawX() - startX;
                    endScroll(distance);
                }
                if (mode != MODE.FREE) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                startX = 0;
                mode = MODE.FREE;
                recycleVelocityTracker();
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                switch (mode) {
                    case DRAGGING:
                        if (Math.abs(x - lastMotionX) > 15 || Math.abs(y - lastMotionY) > 15) {
                            if (onDragListener != null) {
                                onDragListener.onDrag();
                            }
                        }
                        int viewX = x - dragPointX + dragOffsetX;
                        int viewY = y - dragPointY + dragOffsetY;
                        onFling(viewX, viewY);
                        velocityTracker.addMovement(event);
                        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                        //  判断速度如果速度大于highSpeed，不处理移动
                        dragPosition = pointToPosition(x, y);
                        if (scroller.isFinished() && (!mLayoutTransition.isRunning())) {
                            float v = calculateV(velocityTracker.getXVelocity(), velocityTracker.getYVelocity());
                            onBeingDragging(event, v);
                        }
                        break;
                    case SCROLL:
                        int deltaX = (int) (lastMotionX - x);
                        lastMotionX = x;
                        scrollBy(deltaX, 0);
                        break;
                    case FREE:
                        int deltaX1 = (int) (lastMotionX - x);
                        if (ifCanScroll(deltaX1)) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            mode = MODE.SCROLL;
                        }
                        break;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                getAdapter().setEditing(false);
            case MotionEvent.ACTION_UP:
                if (adapter != null) {
                    adapter.setIsTouching(false);
                }
                if (mode == MODE.DRAGGING) {
                    dragPosition = pointToPosition(x, y);
                    onDragFinished(event);
                    endDrag();
                } else if (mode == MODE.SCROLL) {
                    float distance = event.getRawX() - startX;
                    endScroll(distance);
                } else {
                    getAdapter().setEditing(false);
                }
                if (mode != MODE.FREE) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                startX = 0;
                mode = MODE.FREE;
                recycleVelocityTracker();
                break;
        }
        return true;
    }

    private float calculateV(float xVelocity, float yVelocity) {
        return (float) Math.sqrt(xVelocity * xVelocity + yVelocity * yVelocity);
    }

    /**
     * 拖动时处理
     */
    public abstract void onBeingDragging(MotionEvent event, float v);

    public abstract void onDragFinished(MotionEvent event);

    public abstract void onExchange();

    /**
     * 拖动开始时的初始化
     */
    public void onStartFling(View itemView, int x, int y) {
        itemView.destroyDrawingCache();
        itemView.setDrawingCacheEnabled(true);
        Bitmap bm = Bitmap.createBitmap(itemView.getDrawingCache());
        itemView.setVisibility(View.INVISIBLE);
        dragPointX = x - itemView.getLeft() + getCurScreen() * getMeasuredWidth();
        dragPointY = y - itemView.getTop();
        int viewX = x - dragPointX + dragOffsetX;
        int viewY = y - dragPointY + dragOffsetY;
        FingerFlowViewManager.getInstance().setUp(getContext(), bm, viewX, viewY);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        Log.e("SpringboardView", "onDetachedFromWindow");
        FingerFlowViewManager.onDestroy();
    }

    /**
     * 开始拖动
     */
    private void startDrag(View itemView) {
        mode = MODE.DRAGGING;
        getParent().requestDisallowInterceptTouchEvent(true);
        temChangPosition = dragPosition = getChildIndex(itemView);
        initOrResetVelocityTracker();
    }

    public void endDrag() {
        mode = MODE.FREE;
        if (temChangPosition != -1) {
            getChildAt(temChangPosition).setVisibility(View.VISIBLE);
        }
        getParent().requestDisallowInterceptTouchEvent(false);
        dragPosition = temChangPosition = -1;
        FingerFlowViewManager.getInstance().remove();
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void initOrResetVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
    }

    /**
     * 更新拖动的坐标，让图标跟随手一起动
     *
     * @param x view左上角坐标X
     * @param y view左上角坐标Y
     */
    public void onFling(int x, int y) {
        FingerFlowViewManager.getInstance().updatePosition(x, y);
    }


    /**
     * 如果正在进行滑动动画，就停止动画
     */
    protected void stopScroll() {
        if (!scroller.isFinished()) {
            //	中止移动动画
            scroller.abortAnimation();
        }
    }

    /**
     * 判断能否进行滑动
     */
    protected boolean ifCanScroll(int deltaX) {
        boolean ret = false;
        if (Math.abs(deltaX) > touchSlop) {
            if (deltaX < 0) {
                ret = getScrollX() > 0;
            } else {
                ret = getScrollX() < (getTotalPage() - 1) * getMeasuredWidth();
            }
        }
        return ret;
    }

    protected void countPageChange(int x) {
        if (x > getWidth() - 40
                && curScreen < getTotalPage() - 1 && scroller.isFinished()
                && x > startX + 10) {
            snapToScreen(curScreen + 1);
        } else if (x < 40
                && curScreen > 0 && scroller.isFinished() && x < startX - 10) {
            snapToScreen(curScreen - 1);
        }
    }


    public void endScroll(float distance) {
        if (distance > getMeasuredWidth() / 6 && getCurScreen() > 0) {
            snapToScreen(getCurScreen() - 1);
        } else if (distance < -getMeasuredWidth() / 6
                && getCurScreen() < getTotalPage() - 1) {
            snapToScreen(getCurScreen() + 1);
        } else {
            final int screenWidth = getWidth();
            final int destScreen = (getScrollX() + screenWidth / 2) / screenWidth;
            if (destScreen >= 0 && destScreen < getTotalPage()) {
                snapToScreen(destScreen);
            }
        }
    }


    public void snapToScreen(int whichScreen) {
        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        if (getScrollX() != (whichScreen * getWidth())) {
            final int delta = whichScreen * (getWidth() + pageDividerWidth) - getScrollX();

            if (whichScreen != curScreen && onPageChangedListener != null) {
                onPageChangedListener.onPageScroll(curScreen, whichScreen);
            }
            scroller.startScroll(getScrollX(), 0, delta, 0, 800);
            curScreen = whichScreen;
            postInvalidate();// Redraw the layout
        }
    }


    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidate();
        }
    }

    /**
     * 判断是否在文件夹内
     */
    public boolean isInCenter(int position, MotionEvent event) {

        FrameLayout child = (FrameLayout) getChildAt(position);
        View view = child.findViewById(R.id.ll_zone).findViewById(R.id.center_zone_id);

        int[] location = new int[2];
        // 获取控件在屏幕中的位置，返回的数组分别为控件左顶点的 x、y 的值
        view.getLocationOnScreen(location);

        float x = event.getRawX(); // 获取相对于屏幕左上角的 x 坐标值
        float y = event.getRawY();
        RectF rect = new RectF(location[0], location[1], location[0] + view.getWidth(),
                location[1] + view.getHeight());

        return rect.contains(x, y);
    }


    public void setEditingMode(int position, View child, boolean isEditing) {
        LinearLayout layout = (LinearLayout) child.findViewById(R.id.ll_zone);

        if (isEditing) {
            if (ifCanMove(position) && enableShake) {
                Animation shake;
                shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake_rotate);
                layout.startAnimation(shake);
            } else {
                layout.clearAnimation();
            }
        } else {
            layout.clearAnimation();
        }

    }

    public abstract boolean ifCanMove(int position);

    /**
     * 获得View在父容器中的位置
     */
    public int getChildIndex(View view) {
        if (view != null) {
            final int childCount = ((ViewGroup) view.getParent())
                    .getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (view == ((ViewGroup) view.getParent()).getChildAt(i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * @param x,y 触摸点所在的位置
     *            如果不在位置上返回－1，在的话返回位置
     */
    public int pointToPosition(int x, int y) {
        int locX = x + curScreen * getWidth();
        Rect frame = new Rect();
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(locX, y)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 当长按时开启编辑模式
     * 并开始滑动
     */
    protected boolean onItemLongClick(View v, int position) {
        if (mode != MODE.DRAGGING) {
            int index = getChildIndex(v);
            getAdapter().setEditing(true);
            if (ifCanMove(index)) {
                startDrag(v);
                onStartFling(v, (int) lastMotionX, (int) lastMotionY);
            }
            return true;
        }
        return false;
    }

    public abstract void onItemClick(int position);

    /**
     * 计算有几页
     */
    protected void computePageCountChange(boolean ifCallback) {
        int pages = (int) Math.ceil(getChildCount() * 1.0 / getPageItemCount());

        if (pages == 0) {
            pages++;
        }

        if (pages != totalPage) {
            if (this.onPageChangedListener != null && ifCallback) {
                onPageChangedListener.onPageCountChange(totalPage, pages);
            }
            totalPage = pages;
        }
    }

    protected void onMerge(int fromPosition, int toPosition) {
        getAdapter().mergeItem(fromPosition, toPosition, defaultFolderName);
        temChangPosition = -1;
        computePageCountChange(true);
    }

    public void configView(FrameLayout view) {
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = indexOfChild(v);
                return onItemLongClick(v, position);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int position = indexOfChild(arg0);
                onItemClick(position);
            }
        });

    }


    public void setAdapter(SpringboardAdapter mAdapter) {
        this.adapter = mAdapter;
        refreshView();
        computePageCountChange(false);
    }

    /**
     * 刷新页面
     */
    private void refreshView() {
        removeAllViews();
        int count = getItemCount();

        for (int i = 0; i < count; i++) {
            FrameLayout view = initItemView(i);
            configView(view);
            this.addView(view);
        }
    }

    public abstract int getItemCount();

    public abstract FrameLayout initItemView(int position);

    public void delete(int position) {
        onDelete(position);
        if (totalPage <= curScreen) {
            snapToScreen(totalPage - 1);
        }
    }

    public abstract void onDelete(int position);

    public int getMinimumVelocity() {
        return minimumVelocity;
    }

    public int getColCount() {
        return colCount;
    }

    public int getPageItemCount() {
        return rowCount * colCount;
    }

    public void setColCount(int colCount) {
        this.colCount = colCount;
    }

    public int getCurScreen() {
        return curScreen;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public int getChildHeight() {
        return getChildAt(0).getWidth();
    }

    public int getChildWidth() {
        return getChildAt(0).getHeight();
    }

    public int getTotalPage() {
        return totalPage;
    }

    public SpringboardAdapter getAdapter() {
        return adapter;
    }

    public OnPageChangedListener getOnPageChangedListener() {
        return onPageChangedListener;
    }

    public void setOnPageChangedListener(OnPageChangedListener onPageChangedListener) {
        this.onPageChangedListener = onPageChangedListener;
    }

    public interface OnPageChangedListener {
        void onPageScroll(int from, int to);

        void onPageCountChange(int oldCount, int newCount);
    }

    public interface OnItemClickListener {
        void onItemClick(FavoritesItem item);
    }

    private OnItemClickListener onItemClickListener;

    public OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View v, FavoritesItem item);
    }

    private OnItemLongClickListener onItemLongClickListener;

    public OnItemLongClickListener getOnItemLongClickListener() {
        return onItemLongClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    public interface OnDragListener {
        void onDrag();
    }

    private OnDragListener onDragListener;

    public OnDragListener getOnDragListener() {
        return onDragListener;
    }

    public void setOnDragListener(OnDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }

    public int getStableHeaderCount() {
        return stableHeaderCount;
    }

    public void setStableHeaderCount(int stableHeaderCount) {
        this.stableHeaderCount = stableHeaderCount;
    }

    public boolean isLastItemStable() {
        return isLastItemStable;
    }

    public void setIsLastItemStable(boolean isLastItemStable) {
        this.isLastItemStable = isLastItemStable;
    }

}
