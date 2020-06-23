package com.jsir.launcher.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.jsir.launcher.adapter.SpringboardAdapter;
import com.jsir.launcher.bean.FavoritesItem;
import com.jsir.launcher.library.R;

/**
 * 使用这个布局显示按钮
 */
public class MenuView extends SpringboardView {

    // 文件夹相关

    private View folderView;
    private WindowManager windowManager;

    private int folderColCount = 4;
    private int folderRowCount = 6;
    private Drawable folderDialogBackground;
    private Drawable folderViewBackground;
    private Drawable editTextBackground;
    private int folderEditTextTextColor;
    private float folderEditTextTextSize;
    private boolean enableShake = false;

    private boolean isStopShake = false;

    private WindowManager getWindowManager() {
        if (windowManager == null) {
            windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        }
        return windowManager;
    }


    private WindowManager.LayoutParams getWindowParams() {
        WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams();
        windowParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        windowParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        windowParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        windowParams.format = PixelFormat.TRANSLUCENT;
        windowParams.windowAnimations = 0;
        windowParams.x = 0;
        windowParams.y = 0;
        return windowParams;
    }


    public MenuView(Context context) {
        this(context, null);
    }

    public MenuView(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.StyleSpringboard);
    }

    public MenuView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Springboard, defStyleAttr, defStyleRes);
        folderColCount = a.getInt(R.styleable.Springboard_folder_column_count, 3);
        folderRowCount = a.getInt(R.styleable.Springboard_folder_row_count, 3);
        folderDialogBackground = a.getDrawable(R.styleable.Springboard_folder_dialog_background);
        folderViewBackground = a.getDrawable(R.styleable.Springboard_folder_view_background);
        editTextBackground = a.getDrawable(R.styleable.Springboard_folder_edit_background);
        folderEditTextTextColor = a.getColor(R.styleable.Springboard_folder_edit_text_color, 0);
        folderEditTextTextSize = a.getDimensionPixelSize(R.styleable.Springboard_folder_edit_text_size, 15);
        enableShake = a.getBoolean(R.styleable.Springboard_enable_shake, false);
        a.recycle();
        // 点击区域外取消编辑状态
        this.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getAdapter().setEditing(false);
            }
        });
    }


    @Override
    public void onItemClick(int position) {
        if (getAdapter().getItem(position).isFolder()) {
            showFolder(position);
        } else {
            if (getAdapter().isEditing()) {
                getAdapter().setEditing(false);
            } else {
                if (getOnItemClickListener() != null) {
                    getOnItemClickListener().onItemClick(getAdapter().getItem(position));
                }
            }
        }
    }

    @Override
    protected boolean onItemLongClick(View v, int position) {
        boolean ret = super.onItemLongClick(v, position);
        if (getOnItemLongClickListener() != null) {
            getOnItemLongClickListener().onItemLongClick(v, getAdapter().getItem(position));
        }
        return ret;
    }

    @Override
    public int getItemCount() {
        return getAdapter().getCount();
    }

    @Override
    public FrameLayout initItemView(int position) {
        FrameLayout view = adapter.initItemView(position, this);
        adapter.configItemView(position, view);
        return view;
    }

    @Override
    public void onBeingDragging(MotionEvent event, float v) {
        int x = (int) event.getX();
        if (v < getMinimumVelocity()) {
            //初始位置和触摸位置不同时
            if (dragPosition != -1 && dragPosition != temChangPosition) {
//        		如果在头部或尾部区域，不进行操作
                if (ifCanMove(dragPosition)) {
                    //如果被拖动的是文件夹或者不在item内部区域，交换位置
                    if ((getAdapter().getItem(temChangPosition).isFolder()) || (!isInCenter(dragPosition, event))) {
                        onExchange();
                    }
                }
            }
            countPageChange(x);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

    }

    @Override
    public void onDragFinished(MotionEvent event) {
        //可以合并
        if (getAdapter().ifCanMerge(temChangPosition, dragPosition)) {
            //在目标位置中心区域
            if (dragPosition != -1 && temChangPosition != dragPosition && isInCenter(dragPosition, event)) {
                onMerge(temChangPosition, dragPosition);
            }
        }
    }

    @Override
    public void onExchange() {
        adapter.exchangeItem(temChangPosition, dragPosition);
        temChangPosition = dragPosition;
        getChildAt(dragPosition).setVisibility(View.INVISIBLE);

    }

    @Override
    public boolean ifCanMove(int position) {
        return position >= getStableHeaderCount() && !(position == getChildCount() - 1 && isLastItemStable());

    }


    protected void onActionFolderClosed(FavoritesItem dragOutItem, MotionEvent event) {
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        removeFolder();
        if (temChangPosition != -1) {
            getChildAt(temChangPosition).setVisibility(View.VISIBLE);
        } else {
            int[] locations = new int[2];
            this.getLocationOnScreen(locations);
            x = x - locations[0];
            y = y - locations[1];
            dragPosition = pointToPosition(x, y);
            if (dragPosition != -1 && isInCenter(dragPosition, event) && ifCanMove(dragPosition)) {
//                Log.e("ScrollLayout", "拖动结束，合并到文件夹 position = "+dragOutItem);
                getAdapter().addItemToFolder(dragPosition, dragOutItem, defaultFolderName);
            } else {
//                Log.e("ScrollLayout", "拖动结束，添加按钮到最后");
                int position = getChildCount();
                if (isLastItemStable()) {
                    position--;
                }
                getAdapter().addItem(position, dragOutItem);

                computePageCountChange(true);

                if (getCurScreen() < getTotalPage() - 1) {
                    snapToScreen(getTotalPage() - 1);
                }
            }
        }
        dragPosition = temChangPosition = -1;
    }

    /**
     * @param event       触摸点位置
     * @param dragOutItem 被拖出的按钮
     */
    protected void dragOnChild(FavoritesItem dragOutItem, MotionEvent event) {

        if (!scroller.isFinished() || mLayoutTransition.isRunning()) {
            return;
        }

        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        int[] locations = new int[2];
        this.getLocationOnScreen(locations);
        x = x - locations[0];
        y = y - locations[1];
        dragPosition = pointToPosition(x, y);
        if (dragPosition != -1) {
            if (temChangPosition != -1) {
                if (isLastItemStable() && dragPosition != getItemCount() - 1) {
                    if (temChangPosition != dragPosition && ifCanMove(dragPosition)) {
                        if (isInCenter(dragPosition, event)) {
//                            Log.e("dragOnChild", "删除位置是temChangPosition 的按钮");
                            onDelete(temChangPosition);
                            temChangPosition = -1;
                        } else {
//                            Log.e("dragOnChild","交换位置 "+temChangPosition +""+dragPosition);
                            onExchange();
                        }
                    }
                }
            } else {
                if (isLastItemStable() && dragPosition == getItemCount() - 1) {
//                    Log.e("dragOnChild","添加按钮到位置 "+dragPosition);
                    getAdapter().addItem(dragPosition, dragOutItem);
                    temChangPosition = dragPosition;
                    getChildAt(temChangPosition).setVisibility(View.INVISIBLE);
                    computePageCountChange(true);
                } else {
                    if (!isInCenter(dragPosition, event) && ifCanMove(dragPosition)) {
//                        Log.e("dragOnChild","添加按钮到位置 "+dragPosition);
                        getAdapter().addItem(dragPosition, dragOutItem);
                        temChangPosition = dragPosition;
                        getChildAt(temChangPosition).setVisibility(View.INVISIBLE);
                        computePageCountChange(true);
                    }
                }
            }
        } else if (temChangPosition != -1) {
            onDelete(temChangPosition);
            temChangPosition = -1;
        }
        countPageChange(x);
    }

    /**
     * 显示文件夹
     */
    public void showFolder(int position) {
        if (enableShake && getAdapter().isEditing()) {
            stopShake();
            isStopShake = true;
        }
        FavoritesItem info = getAdapter().getItem(position);
        folderView = LayoutInflater.from(getContext()).inflate(R.layout.folder_layout, this, false);
        if (folderDialogBackground != null) {
            folderView.setBackground(folderDialogBackground);
        }
        LinearLayout linearLayout = folderView.findViewById(R.id.ll_container);

        if (folderViewBackground != null) {
            linearLayout.setBackground(folderViewBackground);
        }
        EditText editText = folderView.findViewById(R.id.et_name);
        if (editTextBackground != null) {
            editText.setBackground(editTextBackground);
        }
        if (folderEditTextTextColor != 0) {
            editText.setTextColor(folderEditTextTextColor);
        }
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, folderEditTextTextSize);
        editText.setText(info.getActionName());

        FolderView layout = folderView.findViewById(R.id.fv_container);

        layout.setParentLayout(this);
        layout.setColCount(folderColCount);
        layout.setRowCount(folderRowCount);
        layout.setFolderPosition(position);
        layout.setAdapter(getAdapter());
        layout.setOnItemClickListener(getOnItemClickListener());
        layout.setOnItemLongClickListener(getOnItemLongClickListener());
        folderView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                removeFolder();
            }
        });
        getWindowManager().addView(folderView, getWindowParams());

        getAdapter().initFolderEditingMode();

    }

    /**
     * 隐藏View
     */
    public void hideFolder() {
        if (folderView != null) {
            WindowManager.LayoutParams params = getWindowParams();
            params.alpha = 0;
            getWindowManager().updateViewLayout(folderView, params);
        }

        if (enableShake && getAdapter().isEditing()) {
            if (isStopShake) {
                startShake();
                isStopShake = false;
            }
        }
    }


    /**
     * 清除View
     */
    public void removeFolder() {
        if (enableShake && getAdapter().isEditing()) {
            if (isStopShake) {
                startShake();
                isStopShake = false;
            }
        }

        if (folderView != null) {

            EditText editText = folderView.findViewById(R.id.et_name);
            String name = editText.getText().toString();
            if (TextUtils.isEmpty(name)) {
                name = defaultFolderName;
            }
            getAdapter().changeFolderName(name);
            getWindowManager().removeView(folderView);
            folderView = null;
            getAdapter().removeFolder();
        }
    }


    public void stopShake() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            ImageView imageView = child.findViewById(R.id.delete_button_id);
            FrameLayout layout = child.findViewById(R.id.ll_zone);
            imageView.clearAnimation();
            layout.clearAnimation();
        }
    }

    public void startShake() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            ImageView imageView = child.findViewById(R.id.delete_button_id);
            FrameLayout layout = child.findViewById(R.id.ll_zone);
            Animation shake;
            if (ifCanMove(i)) {
                if (imageView.getVisibility() == View.VISIBLE) {
                    shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
                    imageView.startAnimation(shake);
                }
                shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake_rotate);
                layout.startAnimation(shake);
            }
        }
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && getAdapter().isEditing()) {
            getAdapter().setEditing(false);
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    public boolean isShowingFolder() {
        return folderView != null;
    }


    public void setAdapter(SpringboardAdapter adapter) {
        adapter.setSpringboardView(this);
        super.setAdapter(adapter);
    }

    @Override
    public void onDelete(int position) {
        getAdapter().deleteItem(position);
        computePageCountChange(true);
    }
}