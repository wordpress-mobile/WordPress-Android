package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.wordpress.android.R;
import org.wordpress.android.databinding.PublicizeConnectionListItemBinding;

/**
 * Created by Will on 6/24/16.
 */
public class PublicizeAccountChooserListAdapter extends BaseAdapter {
    private ObservableArrayList<PublicizeEvents.Connection> list;
    private LayoutInflater inflater;

    public PublicizeAccountChooserListAdapter(ObservableArrayList<PublicizeEvents.Connection> list) {
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (inflater == null) {
            inflater = (LayoutInflater) viewGroup.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        PublicizeConnectionListItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.publicize_connection_list_item, viewGroup, false);
        binding.setConnection(list.get(i));

        return binding.getRoot();
    }
}
