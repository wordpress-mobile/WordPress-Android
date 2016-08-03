package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.List;

public class MultiSelectListViewAdapter extends BaseAdapter {
    private Context mContext;
    private List<String> mUsers;

    public MultiSelectListViewAdapter(Context context, List<String> users) {
        this.mContext = context;
        this.mUsers = users;
    }

    @Override
    public int getCount() {
        return mUsers.size();
    }

    @Override
    public String getItem(int position) {
        return mUsers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.wp_simple_list_item_1, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.text.setText(getItem(position));
        viewHolder.view.setBackgroundColor(viewHolder.view.isActivated() ?
                mContext.getResources().getColor(R.color.white) :
                mContext.getResources().getColor(R.color.transparent));
        return convertView;
    }

    private static class ViewHolder {
        TextView text;
        View view;

        public ViewHolder(View view) {
            this.view = view;
            this.text = (TextView) view.findViewById(R.id.text);
        }
    }
}
