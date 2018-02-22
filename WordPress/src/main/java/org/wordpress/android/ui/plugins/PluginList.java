package org.wordpress.android.ui.plugins;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;

import java.util.ArrayList;

class PluginList extends ArrayList<ImmutablePluginModel> {
    int indexOfPluginWithSlug(@Nullable String slug) {
        if (slug != null) {
            for (int i = 0; i < this.size(); i++) {
                ImmutablePluginModel item = this.get(i);
                if (slug.equalsIgnoreCase(item.getSlug())) {
                    return i;
                }
            }
        }
        return -1;
    }

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
