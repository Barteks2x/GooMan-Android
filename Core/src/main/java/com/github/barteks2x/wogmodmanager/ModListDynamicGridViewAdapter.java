package com.github.barteks2x.wogmodmanager;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.askerov.dynamicgrid.BaseDynamicGridAdapter;
import org.askerov.dynamicgrid.DynamicGridView;

import java.util.ArrayList;
import java.util.List;

public class ModListDynamicGridViewAdapter extends BaseDynamicGridAdapter {
  private Context context;
  private DynamicGridView dgv;

  public ModListDynamicGridViewAdapter(Context context, DynamicGridView dgv) {
    super(context, new ArrayList<>(), 1);
    this.context = context;
    this.dgv = dgv;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ModViewHolder holder;
    if (convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.mod_item, null);
      holder = new ModViewHolder(convertView);
      convertView.setTag(holder);
    } else {
      holder = (ModViewHolder) convertView.getTag();
    }
    holder.build((GoomodEntry) getItem(position));
    return convertView;
  }

  private class ModViewHolder {
    private TextView titleText;
    private CheckBox enabled;

    private ModViewHolder(View view) {
      titleText = (TextView) view.findViewById(R.id.mod_item_title);
      enabled = (CheckBox) view.findViewById(R.id.mod_item_enabled);
    }

    void build(final GoomodEntry entry) {
      titleText.setText(entry.getName());
      titleText.setTextColor(entry.isEnabled() ? Color.BLACK : Color.GRAY);
      titleText.requestLayout();
      enabled.setChecked(entry.isEnabled());
      enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          entry.enabled = isChecked;
        }
      });
    }
  }

  public static class GoomodEntry {
    private final String name;
    private boolean enabled;

    public GoomodEntry(String name, boolean enabled) {
      this.name = name;
      this.enabled = enabled;
    }

    public String getName() {
      return name;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }
}
