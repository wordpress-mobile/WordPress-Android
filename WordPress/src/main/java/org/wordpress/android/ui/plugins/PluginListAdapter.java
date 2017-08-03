package org.wordpress.android.ui.plugins;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.List;

class PluginListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private SiteModel mSite;
    private List<PluginModel> mPlugins;

    private final LayoutInflater mLayoutInflater;

    PluginListAdapter(Context context, @NonNull SiteModel site) {
        mSite = site;
        mLayoutInflater = LayoutInflater.from(context);
    }

    private PluginModel getItem(int position) {
        if (isValidPluginPosition(position)) {
            return mPlugins.get(position);
        }
        return null;
    }

    private boolean isValidPluginPosition(int position) {
        return (position >= 0 && position < mPlugins.size());
    }

    @Override
    public int getItemCount() {
        return mPlugins.size();
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
