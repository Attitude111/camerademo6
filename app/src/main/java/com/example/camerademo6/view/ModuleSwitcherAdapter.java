package com.example.camerademo6.view;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.camerademo6.R;

/*数据和view的纽带，继承自HorizontalScrollPickView.PickAdapter*/
public class ModuleSwitcherAdapter extends HorizontalScrollPickView.PickAdapter {
    private Context mContext;
    private String[] items;

    public ModuleSwitcherAdapter(Context context, String[] items) {
        this.mContext = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items == null ? 0 : items.length;
    }

    @Override
    public View getPositionView(int position, ViewGroup parent, LayoutInflater inflater) {
        //设置每个item的样式和文字
        //添加布局 如果attachToRoot为true，那么resource指定的布局文件就会依附于root指定的ViewGroup，然后这个方法就会返回root，否则，只会将resource指定的布局文件填充并将其返回
        TextView textView = (TextView) inflater.inflate(R.layout.item_module_switcher, parent, false);//adapter..false
        textView.setTextSize(12);
        textView.setText(items[position]);
        return textView;
    }

    @Override
    public void initView(View view) {
        ((TextView) view).setTextColor(Color.WHITE);
    }

    /*被选中的置为黄色*/
    @Override
    public void selectView(View view) {
        ((TextView) view).setTextColor(mContext.getResources().getColor(R.color.color_switch_checked));
    }

    @Override
    public void preView(View view) {
        ((TextView) view).setTextColor(Color.WHITE);
    }
}