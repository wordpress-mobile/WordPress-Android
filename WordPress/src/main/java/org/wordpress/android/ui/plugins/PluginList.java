package org.wordpress.android.ui.plugins;

import android.support.annotation.Nullable;

import org.wordpress.android.fluxc.model.plugin.DualPluginModel;

import java.util.ArrayList;

/*
 * List containing either SitePluginModels or WPOrgPluginModels - used to simplify adapters
 * which can show both models
 */
class PluginList extends ArrayList<DualPluginModel> {
    int indexOfPluginWithSlug(@Nullable String slug) {
        if (slug != null) {
            for (int i = 0; i < this.size(); i++) {
                DualPluginModel item = this.get(i);
                String itemSlug = null;
                if (item.getSitePlugin() != null) {
                    itemSlug = item.getSitePlugin().getSlug();
                } else if (item.getWPOrgPlugin() != null) {
                    itemSlug = item.getWPOrgPlugin().getSlug();
                }
                if (slug.equalsIgnoreCase(itemSlug)) {
                    return i;
                }
            }
        }
        return -1;
    }

    long getItemId(int position) {
        // The ids of the plugins are unreliable. Search results will not have any and we might have one or the other plugin type, we shouldn't rely on this
        return position;
    }

    @Nullable Object getItem(int position) {
        if (position >= 0 && position < size()) {
            return get(position);
        }
        return null;
    }
}
