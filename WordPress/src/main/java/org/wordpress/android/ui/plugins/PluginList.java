package org.wordpress.android.ui.plugins;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;

import java.util.ArrayList;
import java.util.List;

class PluginList extends ArrayList<Object> {

    int indexOfPluginWithSlug(@NonNull String slug) {
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
        return -1;
    }

    long getItemId(int position) {
        Object item = this.get(position);
        if (item instanceof WPOrgPluginModel) {
            return ((WPOrgPluginModel) item).getId();
        } else if (item instanceof SitePluginModel) {
            return ((SitePluginModel) item).getId();
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
