package com.jsir.mylauncher;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.Utils;
import com.jsir.launcher.bean.FavoritesItem;
import com.jsir.launcher.widget.MenuView;
import com.jsir.launcher.widget.SpringboardView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MenuView mvBoard;
    private AppInstalledAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mvBoard = findViewById(R.id.mv_board);

        //配置置顶，置顶模块不可拖动
        int count = 3 * mvBoard.getColCount();
        mvBoard.setStableHeaderCount(count);

        adapter = new AppInstalledAdapter(this, getAppList());
        mvBoard.setAdapter(adapter);

        mvBoard.setOnItemClickListener((FavoritesItem item) -> {
            //点击事件
            AppInstalledItem appInstalledItem = (AppInstalledItem) item;
            AppUtils.launchApp(appInstalledItem.getActionId());
        });

        mvBoard.setOnItemLongClickListener((v, item) -> {
            //长按事件
        });


        mvBoard.setOnPageChangedListener(new SpringboardView.OnPageChangedListener() {
            @Override
            public void onPageScroll(int from, int to) {
                //页面切换
            }

            @Override
            public void onPageCountChange(int oldCount, int newCount) {
                //页面数量变换
            }
        });

        mvBoard.setOnDragListener(() -> {
            //拖动回调
        });

    }

    private ArrayList<AppInstalledItem> getAppList() {
        ArrayList<AppInstalledItem> items = new ArrayList<>();

        Utils.init(getApplication());

        List<AppUtils.AppInfo> list = AppUtils.getAppsInfo();
        if (list.size() > 0) {
            for (AppUtils.AppInfo appInfo : list) {
                if(!appInfo.isSystem()) {
                    AppInstalledItem item = new AppInstalledItem();
                    item.setActionId(appInfo.getPackageName());
                    item.setActionName(appInfo.getName());
                    items.add(item);
                }
            }
        }

        return items;
    }
}
