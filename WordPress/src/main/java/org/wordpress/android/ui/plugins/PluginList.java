package org.wordpress.android.ui.plugins;

import android.support.annotation.Nullable;

import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.util.StringUtils;

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
                String itemSlug = null;
                if ((item instanceof SitePluginModel)) {
                    itemSlug = ((SitePluginModel) item).getSlug();
                } else if (item instanceof WPOrgPluginModel) {
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
                WPOrgPluginModel wpOrgPlugin = (WPOrgPluginModel) item;
                // Search results won't have an id, so we can't rely on it
                return wpOrgPlugin.getId() != 0 ? wpOrgPlugin.getId() : position;
            } else if (item instanceof SitePluginModel) {
                return ((SitePluginModel) item).getId();
            }
        }
        return -1;
    }

    /*
     * note this is not the same as an "equals" comparison as it only checks a few fields, which
     * is sufficient for our purposes here
     */
    private boolean isSameItem(Object item1, Object item2) {
        if (item1 instanceof SitePluginModel && item2 instanceof SitePluginModel) {
            SitePluginModel plugin1 = (SitePluginModel) item1;
            SitePluginModel plugin2 = (SitePluginModel) item2;
            return StringUtils.equals(plugin1.getSlug(), plugin2.getSlug())
                    && plugin1.getLocalSiteId() == plugin2.getLocalSiteId()
                    && plugin1.isActive() == plugin2.isActive()
                    && plugin1.isAutoUpdateEnabled() == plugin2.isAutoUpdateEnabled()
                    && StringUtils.equals(plugin1.getVersion(), plugin2.getVersion());
        } else if (item1 instanceof WPOrgPluginModel && item2 instanceof WPOrgPluginModel) {
            WPOrgPluginModel plugin1 = (WPOrgPluginModel) item1;
            WPOrgPluginModel plugin2 = (WPOrgPluginModel) item2;
            return StringUtils.equals(plugin1.getSlug(), plugin2.getSlug())
                    && plugin1.getDownloadCount() == plugin2.getDownloadCount()
                    && plugin1.getNumberOfRatings() == plugin2.getNumberOfRatings()
                    && StringUtils.equals(plugin1.getRating(), plugin2.getRating())
                    && StringUtils.equals(plugin1.getVersion(), plugin2.getVersion());

        } else {
            return false;
        }
    }

    boolean isSameList(@Nullable List<?> items) {
        if (items == null) {
            return false;
        }
        if (this.size() != items.size()) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            if (!isSameItem(items.get(i), this.get(i))) {
                return false;
            }
        }
        return true;
    }
}
