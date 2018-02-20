package org.wordpress.android.ui.plugins;

import android.support.annotation.Nullable;

import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;

import java.util.ArrayList;

/*
 * List containing either SitePluginModels or WPOrgPluginModels - used to simplify adapters
 * which can show both models
 */
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
        // Because ids of the plugins are unreliable (sometimes doesn't exist and multiple models combined)
        // ImmutablePluginModel doesn't expose an id. We need to use the position here and use setHasStableIds(false);
        return position;
    }

    @Nullable Object getItem(int position) {
        if (position >= 0 && position < size()) {
            return get(position);
        }
        return null;
    }
}
