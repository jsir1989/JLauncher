package com.jsir.launcher.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.jsir.launcher.bean.FavoritesItem;
import com.jsir.launcher.adapter.SpringboardAdapter;


/**
 * 展示文件夹的View
 */
public class FolderView extends SpringboardView {

    private boolean isOutOfFolder = false;
    private MenuView parentLayout;
    private FavoritesItem dragOutItem;
    private int folderPosition;

    public FolderView(Context context) {
        super(context);
    }

    public FolderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 判断是否手指移动到文件夹外边了
     */
    public boolean moveOutFolder(int x, int y) {
        return x < 0 || y < 0 || x > getWidth() || y > getHeight();
    }

    @Override
    public void onBeingDragging(MotionEvent event, float v) {

        int x = (int) event.getX();
        int y = (int) event.getY();

        if (!isOutOfFolder && moveOutFolder(x, y)) {
            getParentLayout().hideFolder();
            dragOutItem = getAdapter().tempRemoveItem(folderPosition, temChangPosition);
            isOutOfFolder = true;
            dragPosition = -1;
        }

        if (v < getMinimumVelocity()) {
            if (isOutOfFolder) {
                parentLayout.dragOnChild(dragOutItem, event);
            } else {
                if (dragPosition != -1 && temChangPosition != dragPosition) {
                    onExchange();
                }
                countPageChange(x);
            }

        }
    }

    @Override
    public void onDragFinished(MotionEvent event) {

        if (isOutOfFolder) {
            getParentLayout().onActionFolderClosed(dragOutItem, event);
        } else {
            getChildAt(temChangPosition).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onExchange() {
        getAdapter().exChangeSubItem(folderPosition, temChangPosition, dragPosition);
        getChildAt(dragPosition).setVisibility(View.INVISIBLE);
        temChangPosition = dragPosition;
    }

    @Override
    public boolean ifCanMove(int position) {
        return true;
    }

    public MenuView getParentLayout() {
        return parentLayout;
    }

    public void setParentLayout(MenuView parentLayout) {
        this.parentLayout = parentLayout;
    }

    public int getFolderPosition() {
        return folderPosition;
    }

    public void setFolderPosition(int folderPosition) {
        this.folderPosition = folderPosition;
    }


    public void setAdapter(SpringboardAdapter adapter) {
        adapter.setFolderView(this);
        super.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
        if (getAdapter().isEditing()) {
            getAdapter().setEditing(false);
        } else {
            if (getOnItemClickListener() != null) {
                getOnItemClickListener().onItemClick(getAdapter().getSubItem(folderPosition, position));
            }
        }
    }

    @Override
    protected boolean onItemLongClick(View v, int position) {
        boolean ret = super.onItemLongClick(v, position);
        if (getOnItemLongClickListener() != null) {
            getOnItemLongClickListener().onItemLongClick(v, getAdapter().getSubItem(folderPosition, position));
        }
        return ret;
    }

    @Override
    public int getItemCount() {
        return getAdapter().getSubItemCount(folderPosition);
    }

    @Override
    public FrameLayout initItemView(int position) {
        FrameLayout view = adapter.initSubItemView(folderPosition, position, this);
        adapter.configSubItemView(folderPosition, position, view);
        return view;
    }

    @Override
    public void onDelete(int position) {
        getAdapter().deleteItem(folderPosition, position);
        computePageCountChange(false);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            getParentLayout().removeFolder();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
