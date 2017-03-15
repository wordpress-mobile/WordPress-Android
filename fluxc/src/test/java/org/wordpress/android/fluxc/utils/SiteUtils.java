package org.wordpress.android.fluxc.utils;

import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.ArrayList;
import java.util.List;

public class SiteUtils {
    public static SiteModel generateWPComSite() {
        return generateWPComSite(556, "", "", true, true);
    }

    public static SiteModel generateWPComSite(long remoteId, String url, String xmlRpcUrl, boolean isWPCom,
                                              boolean isVisible) {
        SiteModel example = new SiteModel();
        example.setUrl(url);
        example.setXmlRpcUrl(xmlRpcUrl);
        example.setSiteId(remoteId);
        example.setIsWPCom(isWPCom);
        example.setIsVisible(isVisible);
        return example;
    }

    public static SiteModel generateSelfHostedNonJPSite() {
        SiteModel example = new SiteModel();
        example.setSelfHostedSiteId(6);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(false);
        example.setIsJetpackConnected(false);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://some.url/xmlrpc.php");
        return example;
    }

    public static SiteModel generateJetpackSiteOverXMLRPC() {
        SiteModel example = new SiteModel();
        example.setSiteId(982);
        example.setSelfHostedSiteId(8);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(true);
        example.setIsJetpackConnected(true);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://jetpack.url/xmlrpc.php");
        return example;
    }

    public static SiteModel generateJetpackSiteOverRestOnly() {
        SiteModel example = new SiteModel();
        example.setSiteId(5623);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(true);
        example.setIsJetpackConnected(true);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://jetpack2.url/xmlrpc.php");
        return example;
    }

    public static SiteModel generateSelfHostedSiteFutureJetpack() {
        SiteModel example = new SiteModel();
        example.setSelfHostedSiteId(8);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(false);
        example.setIsJetpackConnected(false);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://jetpack2.url/xmlrpc.php");
        return example;
    }

    public static List<PostFormatModel> generatePostFormats(String... names) {
        List<PostFormatModel> res = new ArrayList<>();
        for (String name : names) {
            PostFormatModel postFormat = new PostFormatModel();
            postFormat.setSlug(name.toLowerCase());
            postFormat.setDisplayName(name);
            res.add(postFormat);
        }
        return res;
    }
}
