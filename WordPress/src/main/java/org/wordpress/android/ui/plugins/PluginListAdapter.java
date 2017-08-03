package org.wordpress.android.ui.plugins;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.PluginModel;

import java.util.List;

class PluginListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<PluginModel> mPlugins;

    private final LayoutInflater mLayoutInflater;

    PluginListAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void setPlugins(List<PluginModel> plugins) {
        mPlugins = plugins;
    }

    private PluginModel getItem(int position) {
        if (mPlugins != null && position < mPlugins.size()) {
            return mPlugins.get(position);
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return mPlugins != null ? mPlugins.size() : 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.plugin_list_row, parent, false);
        return new PluginViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    }

    private class PluginViewHolder extends RecyclerView.ViewHolder {

        PluginViewHolder(View view) {
            super(view);
        }
    }
}
