package org.wordpress.android.ui.plugins;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;

import java.util.ArrayList;

class PluginList extends ArrayList<ImmutablePluginModel> {
    long getItemId(int position) {
        ImmutablePluginModel plugin = (ImmutablePluginModel) getItem(position);
        if (plugin == null || TextUtils.isEmpty(plugin.getSlug())) {
            // This should never happen
            return -1;
        }
        return plugin.getSlug().hashCode();
    }

    @Nullable Object getItem(int position) {
        if (position >= 0 && position < size()) {
            return get(position);
        }
        return null;
    }
}
