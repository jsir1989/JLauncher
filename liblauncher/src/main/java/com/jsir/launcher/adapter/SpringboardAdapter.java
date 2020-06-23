package com.jsir.launcher.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jsir.launcher.bean.FavoritesItem;
import com.jsir.launcher.widget.FolderView;
import com.jsir.launcher.widget.MenuView;
import com.jsir.launcher.widget.SpringboardView;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

/**
 * 适配器.
 */
public abstract class SpringboardAdapter<T extends FavoritesItem> {
    private ArrayList<T> items;

    private boolean isEditing = false;

    private boolean isTouching = false;

    private SoftReference<MenuView> springboardView;

    private SoftReference<FolderView> folderView;

    /**
     * 首个层级数量
     */
    public int getCount() {
        return items.size();
    }

    /**
     * 文件夹里按钮数量
     */
    public int getSubItemCount(int position) {
        return items.get(position).getSubItemCount();
    }

    /**
     * 首个层级item
     */
    public T getItem(int position) {
        return items.get(position);
    }

    /**
     * 文件夹item
     */
    public T getSubItem(int folderPosition, int position) {
        return (T) items.get(folderPosition).getSubItem(position);
    }

    /**
     * @return 只需返回所需的view, 所有设置请在configUI里完成
     */
    public abstract FrameLayout initItemView(int position, ViewGroup parent);

    public abstract void configItemView(int position, FrameLayout frameLayout);

    /**
     * @return 只需返回所需的view, 所有设置请在configUI里完成
     */
    public abstract FrameLayout initSubItemView(int folderPosition, int position, ViewGroup parent);

    public abstract void configSubItemView(int folderPosition, int position, FrameLayout frameLayout);

    public void exchangeItem(int fromPosition, int toPosition) {
        T item = items.remove(fromPosition);

        SpringboardView container = getSpringboardView();

        FrameLayout view = (FrameLayout) container.getChildAt(fromPosition);

        container.removeView(view);

        items.add(toPosition, item);

        dataChange();

        container.addView(view, toPosition);

        getSpringboardView().setEditingMode(toPosition, view, isEditing);
    }

    public void exChangeSubItem(int folderPosition, int fromPosition, int toPosition) {
        T folder = items.get(folderPosition);

        T item = (T) folder.removeSubItem(fromPosition);
        SpringboardView container = getFolderView();

        FrameLayout view = (FrameLayout) container.getChildAt(fromPosition);

        container.removeView(view);

        folder.addSubItem(toPosition, item);

        dataChange();

        container.addView(view, toPosition);

        configItemView(folderPosition, (FrameLayout) getSpringboardView().getChildAt(folderPosition));

        getFolderView().setEditingMode(toPosition, view, isEditing);

    }

    public void deleteItem(int position) {
        items.remove(position);

        onDataChange();

        getSpringboardView().removeViewAt(position);
    }

    public void deleteItem(int folderPosition, int position) {
        T folder = items.get(folderPosition);

        folder.removeSubButton(position);

        onDataChange();

        getFolderView().removeViewAt(position);

        FrameLayout view = (FrameLayout) getSpringboardView().getChildAt(folderPosition);

        configItemView(folderPosition, view);

        if (!folder.isFolder()) {
            getSpringboardView().setEditingMode(folderPosition, view, isEditing);
            getSpringboardView().removeFolder();
        } else {
            getSpringboardView().setEditingMode(folderPosition, view, isEditing);
        }
    }


    public void mergeItem(int fromPosition, int toPosition, String defaultFolderName) {
        T fromItem = items.get(fromPosition);

        T toItem = items.get(toPosition);

        toItem.addSubButton(fromItem, defaultFolderName);

        FrameLayout view = (FrameLayout) getSpringboardView().getChildAt(toPosition);

        configItemView(toPosition, view);

        getSpringboardView().setEditingMode(toPosition, view, isEditing);

        items.remove(fromPosition);

        dataChange();

        getSpringboardView().removeViewAt(fromPosition);
    }

    public T tempRemoveItem(int folderPosition, int position) {
        T folder = items.get(folderPosition);

        T item = (T) folder.getSubItem(position);

        folder.removeSubButton(position);

        FrameLayout view = (FrameLayout) getSpringboardView().getChildAt(folderPosition);

        configItemView(folderPosition, view);

        getSpringboardView().setEditingMode(folderPosition, view, isEditing);

        return item;
    }

    public void addItem(int position, T item) {
        items.add(position, item);

        dataChange();

        FrameLayout view = getSpringboardView().initItemView(position);
        getSpringboardView().configView(view);
        getSpringboardView().addView(view, position);

        getSpringboardView().setEditingMode(position, view, isEditing);

    }

    public void addItemToFolder(int dragPosition, T dragOutItem, String defaultName) {
        T folder = items.get(dragPosition);

        folder.addSubButton(dragOutItem, defaultName);

        dataChange();

        configItemView(dragPosition, (FrameLayout) getSpringboardView().getChildAt(dragPosition));
    }

    private void dataChange() {
        if (!isTouching) {
            onDataChange();
        }
    }


    public MenuView getSpringboardView() {
        return springboardView.get();
    }

    public void setSpringboardView(MenuView mSpringboardView) {
        this.springboardView = new SoftReference<>(mSpringboardView);
    }

    public FolderView getFolderView() {
        if (folderView != null) {
            return folderView.get();

        }
        return null;
    }

    public void setFolderView(FolderView folderView) {
        this.folderView = new SoftReference<>(folderView);
    }

    /**
     * @param position 位置
     * @return true 可以删除
     */
    public boolean ifCanDelete(int position) {
        return !getItem(position).isFolder();
    }

    /**
     * @param position 位置
     * @return true 可以删除
     */
    public boolean ifCanDelete(int folderPosition, int position) {
        return !getSubItem(folderPosition, position).isFolder();
    }

    public boolean ifCanMerge(int fromPosition, int toPosition) {
        return !(getItem(fromPosition).isFolder() || (!getSpringboardView().ifCanMove(toPosition)));
    }

    public void changeFolderName(String name) {
        int folderPosition = getFolderView().getFolderPosition();
        T item = items.get(folderPosition);
        if (item.isFolder()) {
            String oldName = items.get(folderPosition).getActionName();
            if (!oldName.equals(name)) {
                items.get(folderPosition).setActionName(name);

                onDataChange();

                configItemView(folderPosition, (FrameLayout) getSpringboardView().getChildAt(folderPosition));
            }
        }
    }

    public void setEditing(boolean isEditing) {
        if (this.isEditing != isEditing) {
            this.isEditing = isEditing;
            FolderView folder;
            if (folderView != null && (folder = folderView.get()) != null) {
                int count = folder.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = folder.getChildAt(i);
                    folder.setEditingMode(i, child, isEditing);
                }
                if (isEditing()) {
                    folder.requestFocus();
                }
                int count1 = getSpringboardView().getChildCount();
                for (int i = 0; i < count1; i++) {
                    View child1 = getSpringboardView().getChildAt(i);
                    getSpringboardView().setEditingMode(i, child1, isEditing);
                }
            } else {
                if (isEditing()) {
                    getSpringboardView().requestFocus();
                }
                int count1 = getSpringboardView().getChildCount();
                for (int i = 0; i < count1; i++) {
                    View child1 = getSpringboardView().getChildAt(i);
                    getSpringboardView().setEditingMode(i, child1, isEditing);
                }
            }
        }
    }

    public void initFolderEditingMode() {
        if (isEditing) {
            FolderView folder;
            if (folderView != null && (folder = folderView.get()) != null) {
                int count = folder.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = folder.getChildAt(i);
                    folder.setEditingMode(i, child, isEditing);
                }
                if (isEditing()) {
                    folder.requestFocus();
                }
            }
        }
    }

    public boolean isEditing() {
        return isEditing;
    }

    public void removeFolder() {
        setFolderView(null);
    }

    public abstract void onDataChange();

    public ArrayList<T> getItems() {
        return items;
    }

    public void setItems(ArrayList<T> items) {
        this.items = items;
    }

    public boolean isTouching() {
        return isTouching;
    }

    public void setIsTouching(boolean isTouching) {
        this.isTouching = isTouching;
    }
}