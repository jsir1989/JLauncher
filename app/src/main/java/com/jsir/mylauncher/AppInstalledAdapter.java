package com.jsir.mylauncher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.jsir.launcher.adapter.SpringboardAdapter;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class AppInstalledAdapter extends SpringboardAdapter<AppInstalledItem> {

    private Context context;

    public AppInstalledAdapter(Context context, ArrayList<AppInstalledItem> items) {
        this.context = context;
        setItems(items);
    }

    @Override
    public FrameLayout initItemView(int position, ViewGroup parent) {
        return (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_install, parent, false);
    }

    @Override
    public void configItemView(int position, FrameLayout view) {
        AppInstalledItem item = getItem(position);
        initItemView(item, view);
    }

    @Override
    public FrameLayout initSubItemView(int folderPosition, int position, ViewGroup parent) {
        return (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_install, parent, false);
    }

    @Override
    public void configSubItemView(int folderPosition, int position, FrameLayout view) {
        AppInstalledItem item = getSubItem(folderPosition, position);
        initItemView(item, view);
    }

    private void initItemView(AppInstalledItem item, FrameLayout view) {

        TextView tvName = view.findViewById(R.id.tv_menu_name);
        ImageView ivIcon = view.findViewById(R.id.iv_menu_icon);
        ivIcon.setVisibility(item.isFolder() ? View.GONE : View.VISIBLE);
        LinearLayout llFolder = view.findViewById(R.id.ll_folder);
        llFolder.setVisibility(item.isFolder() ? View.VISIBLE : View.GONE);
        if (item.isFolder()) {
            loadBackground(llFolder, R.drawable.bg_home_explore);
            tvName.setText(item.getActionName());
            ImageView[] images = new ImageView[4];
            images[0] = llFolder.findViewById(R.id.iv_folder_button1);
            images[1] = llFolder.findViewById(R.id.iv_folder_button2);
            images[2] = llFolder.findViewById(R.id.iv_folder_button3);
            images[3] = llFolder.findViewById(R.id.iv_folder_button4);
            for (int i = 0; i < 4; i++) {
                if (item.getSubItemCount() > i) {
                    AppInstalledItem button = item.getSubItem(i);
                    showIcon(images[i], button);
                    images[i].setVisibility(View.VISIBLE);
                } else {
                    images[i].setVisibility(View.INVISIBLE);
                }
            }
        } else {
            showIcon(ivIcon, item);
            if (AppUtils.isAppInstalled(item.getActionId())) {
                tvName.setText(item.getActionName());
            }
        }
    }

    private void showIcon(ImageView ivIcon, AppInstalledItem item) {
        AppUtils.AppInfo appInfo = AppUtils.getAppInfo(item.getActionId());
        if (appInfo != null) {
            Glide.with(context)
                    .load(appInfo.getIcon())
                    .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(50, 1)))
                    .into(ivIcon);
        }
    }

    public void loadBackground(View view, int resId) {
        Glide.with(context)
                .load(resId)
                .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(50, 1)))
                .into(new CustomViewTarget<View, Drawable>(view) {
                    @Override
                    protected void onResourceCleared(@Nullable Drawable placeholder) {

                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {

                    }

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        getView().setBackground(resource);
                    }

                });
    }

    @Override
    public boolean ifCanMerge(int fromPosition, int toPosition) {
        return super.ifCanMerge(fromPosition, toPosition);
    }

    @Override
    public void onDataChange() {

        ToastUtils.showShort("数据发生变更");

        //获取变更的数据清单
//        ArrayList<AppInstalledItem> appList = getItems();

    }
}
