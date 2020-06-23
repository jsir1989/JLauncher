package com.jsir.mylauncher;


import com.jsir.launcher.bean.FavoritesItem;

import java.util.ArrayList;

public class AppInstalledItem extends FavoritesItem {
    private String actionName; // 应用名
    private String actionId; //包名

    private ArrayList<AppInstalledItem> menuList;

    @Override
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String getActionName() {
        return actionName;
    }

    @Override
    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    @Override
    public String getActionId() {
        return actionId;
    }


    public ArrayList<AppInstalledItem> getMenuList() {
        return menuList;
    }

    public void setMenuList(ArrayList<AppInstalledItem> menuList) {
        this.menuList = menuList;
    }

    @Override
    public boolean isFolder() {
        return menuList != null;
    }

    @Override
    public void addSubButton(FavoritesItem item, String defaultFolderName) {

        if (menuList == null || menuList.size() == 0) {
            menuList = new ArrayList<>();
            AppInstalledItem newItem = new AppInstalledItem();
            newItem.copy(this);
            menuList.add(newItem);
            setActionId("-1");
            setActionName(defaultFolderName);
        }
        menuList.add((AppInstalledItem) item);
    }

    @Override
    public void addSubItem(int position, FavoritesItem item) {
        menuList.add(position, (AppInstalledItem) item);
    }

    @Override
    public AppInstalledItem removeSubItem(int position) {
        return menuList.remove(position);
    }

    @Override
    public void removeSubButton(int position) {
        menuList.remove(position);

        if (menuList.size() == 1) {
            copy(menuList.get(0));
            menuList = null;
        }
    }

    private void copy(AppInstalledItem myButtonItem) {
        this.actionName = myButtonItem.actionName;
        this.actionId = myButtonItem.actionId;
    }

    @Override
    public int getSubItemCount() {
        return menuList == null ? 0 : menuList.size();
    }

    @Override
    public AppInstalledItem getSubItem(int position) {
        if (menuList != null) {
            if (menuList.size() > position) {
                return menuList.get(position);
            }

        }
        throw new IllegalStateException("按钮列表是空");
    }
}
