package org.wordpress.android.fluxc.site;

import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.ArrayList;
import java.util.List;

public class SiteUtils {
    private static final String ZENDESK_PLAN_BUSINESS_PROFESSIONAL = "business_professional";
    private static final String ZENDESK_ADDON_BACKUP_DAILY = "jetpack_addon_backup_daily";
    private static final String ZENDESK_ADDON_SCAN_DAILY = "jetpack_addon_scan_daily";

    public static SiteModel generateWPComSite() {
        return generateTestSite(556, "", "", true, true);
    }

    public static SiteModel generateTestSite(long remoteId, String url, String xmlRpcUrl, boolean isWPCom,
                                             boolean isVisible) {
        SiteModel example = new SiteModel();
        example.setUrl(url);
        example.setXmlRpcUrl(xmlRpcUrl);
        example.setSiteId(remoteId);
        example.setIsWPCom(isWPCom);
        example.setIsVisible(isVisible);
        if (isWPCom) {
            example.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        } else {
            example.setOrigin(SiteModel.ORIGIN_XMLRPC);
        }
        return example;
    }

    public static SiteModel generateSelfHostedNonJPSite() {
        SiteModel example = new SiteModel();
        example.setSelfHostedSiteId(6);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(false);
        example.setIsJetpackConnected(false);
        example.setIsVisible(true);
        example.setUrl("http://some.url");
        example.setXmlRpcUrl("http://some.url/xmlrpc.php");
        example.setOrigin(SiteModel.ORIGIN_XMLRPC);
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
        example.setUsername("ponyuser");
        example.setPassword("ponypass");
        example.setUrl("http://jetpack.url");
        example.setXmlRpcUrl("http://jetpack.url/xmlrpc.php");
        example.setOrigin(SiteModel.ORIGIN_XMLRPC);
        return example;
    }

    public static SiteModel generateJetpackSiteOverRestOnly() {
        SiteModel example = new SiteModel();
        example.setSiteId(5623);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(true);
        example.setIsJetpackConnected(true);
        example.setIsVisible(true);
        example.setUrl("http://jetpack2.url");
        example.setXmlRpcUrl("http://jetpack2.url/xmlrpc.php");
        example.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        return example;
    }

    public static SiteModel generateJetpackCPSite() {
        SiteModel example = new SiteModel();
        example.setSiteId(5623);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(false);
        example.setIsJetpackConnected(false);
        example.setIsJetpackCPConnected(true);
        example.setIsVisible(true);
        example.setUrl("http://jetpackcp.url");
        example.setXmlRpcUrl("http://jetpackcp.url/xmlrpc.php");
        example.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        return example;
    }

    public static SiteModel generateSelfHostedSiteFutureJetpack() {
        SiteModel example = new SiteModel();
        example.setSelfHostedSiteId(8);
        example.setIsWPCom(false);
        example.setIsJetpackInstalled(false);
        example.setIsJetpackConnected(false);
        example.setIsVisible(true);
        example.setUrl("http://jetpack2.url");
        example.setXmlRpcUrl("http://jetpack2.url/xmlrpc.php");
        example.setOrigin(SiteModel.ORIGIN_XMLRPC);
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

    public static SiteModel generateSiteWithZendeskMetaData() {
        SiteModel site = generateJetpackSiteOverRestOnly();
        site.setZendeskPlan(ZENDESK_PLAN_BUSINESS_PROFESSIONAL);
        site.setZendeskAddOns(ZENDESK_ADDON_BACKUP_DAILY + "," + ZENDESK_ADDON_SCAN_DAILY);
        return site;
    }
}
