package org.wordpress.android.util;

import org.wordpress.android.stores.model.SiteModel;

public class SiteUtils {
    public static String getSiteNameOrHomeURL(SiteModel siteModel) {
        String siteName = getSiteName(siteModel);
        if (siteName.trim().length() == 0) {
            siteName = getHomeURLOrHostName(siteModel);
        }
        return siteName;
    }


    public static String getHomeURLOrHostName(SiteModel siteModel) {
        String homeURL = UrlUtils.removeScheme(siteModel.getUrl());
        homeURL = StringUtils.removeTrailingSlash(homeURL);
        if (homeURL.length() == 0) {
            return UrlUtils.getHost(siteModel.getXmlRpcUrl());
        }
        return homeURL;
    }

    public static String getSiteName(SiteModel siteModel) {
        return StringUtils.unescapeHTML(siteModel.getName());
    }
}
