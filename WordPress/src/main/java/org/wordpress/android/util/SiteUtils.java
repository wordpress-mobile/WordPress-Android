package org.wordpress.android.util;

import android.text.TextUtils;

import org.wordpress.android.fluxc.model.SiteModel;

public class SiteUtils {
    public static String getSiteNameOrHomeURL(SiteModel site) {
        String siteName = getSiteName(site);
        if (siteName.trim().length() == 0) {
            siteName = getHomeURLOrHostName(site);
        }
        return siteName;
    }

    public static String getHomeURLOrHostName(SiteModel site) {
        String homeURL = UrlUtils.removeScheme(site.getUrl());
        homeURL = StringUtils.removeTrailingSlash(homeURL);
        if (homeURL.length() == 0) {
            return UrlUtils.getHost(site.getXmlRpcUrl());
        }
        return homeURL;
    }

    public static String getSiteName(SiteModel site) {
        return StringUtils.unescapeHTML(site.getName());
    }

    /**
     * @return true if the site is WPCom or Jetpack and is not private
     */
    public static boolean photonCapableEh(SiteModel site) {
        return SiteUtils.accessibleViaWPComAPIEh(site) && !site.privateEh();
    }

    public static boolean accessibleViaWPComAPIEh(SiteModel site) {
        return site.isWPCom() || site.isJetpackConnected();
    }

    public static String getSiteIconUrl(SiteModel site, int size) {
        if (!TextUtils.isEmpty(site.getIconUrl())) {
            return PhotonUtils.getPhotonImageUrl(site.getIconUrl(), size, size, PhotonUtils.Quality.HIGH);
        } else {
            return GravatarUtils.blavatarFromUrl(site.getUrl(), size);
        }
    }
}
