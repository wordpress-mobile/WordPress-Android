package org.wordpress.android.ui.plugins;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;

import java.util.ArrayList;
import java.util.List;

/*
 * List containing either SitePluginModels or WPOrgPluginModels - used to simplify adapters
 * which can show both models
 */
class PluginList extends ArrayList<Object> {

    int indexOfPluginWithSlug(@Nullable String slug) {
        if (slug != null) {
            for (int i = 0; i < this.size(); i++) {
                Object item = this.get(i);
                String itemSlug;
                if ((item instanceof SitePluginModel)) {
                    itemSlug = ((SitePluginModel) item).getSlug();
                } else {
                    itemSlug = ((WPOrgPluginModel) item).getSlug();
                }
                if (slug.equalsIgnoreCase(itemSlug)) {
                    return i;
                }
            }
        }
        return -1;
    }

    long getItemId(int position) {
        if (position >= 0 && position < this.size()) {
            Object item = this.get(position);
            if (item instanceof WPOrgPluginModel) {
                return ((WPOrgPluginModel) item).getId();
            } else if (item instanceof SitePluginModel) {
                return ((SitePluginModel) item).getId();
            }
        }
        return -1;
    }

    boolean isSameList(@NonNull List<?> items) {
        if (this.size() != items.size()) {
            return false;
        }
        for (Object item : items) {
            if (item instanceof WPOrgPluginModel) {
                if (indexOfPluginWithSlug(((WPOrgPluginModel) item).getSlug()) == -1) {
                    return false;
                }
            } else if (item instanceof SitePluginModel) {
                if (indexOfPluginWithSlug(((SitePluginModel) item).getSlug()) == -1) {
                    return false;
                }
            }
        }
        return true;
    }
}
