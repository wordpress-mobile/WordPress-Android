package org.wordpress.android.util.analytics;

import org.junit.Test;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.SiteUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.wordpress.android.util.analytics.AnalyticsUtils.isGutenbergEnabledOnAnySite;

public class AnalyticsUtilsTest {
    @Test
    public void isGutenbergEnabledOnAnySite_no_sites() {
        assertFalse(isGutenbergEnabledOnAnySite(siteList()));
    }

    @Test
    public void isGutenbergEnabledOnAnySite_single_gutenberg_site() {
        List<SiteModel> sites = siteList(false, true, false);
        assertTrue(isGutenbergEnabledOnAnySite(sites));
    }

    @Test
    public void isGutenbergEnabledOnAnySite_multiple_gutenberg_sites() {
        List<SiteModel> sites = siteList(true, false, true, false);
        assertTrue(isGutenbergEnabledOnAnySite(sites));
    }

    @Test
    public void isGutenbergEnabledOnAnySite_no_gutenberg_sites() {
        List<SiteModel> sites = siteList(false, false);
        assertFalse(isGutenbergEnabledOnAnySite(sites));
    }

    private List<SiteModel> siteList(boolean... isGutenbergEnabledList) {
        List<SiteModel> sites = new ArrayList<>();
        for (boolean isEnabled : isGutenbergEnabledList) {
            sites.add(mockSite(isEnabled));
        }
        return sites;
    }

    private SiteModel mockSite(boolean isGutenbergEnabled) {
        String enabledString = isGutenbergEnabled ? SiteUtils.GB_EDITOR_NAME : "";
        SiteModel mockSite = mock(SiteModel.class);
        when(mockSite.getMobileEditor()).thenReturn(enabledString);
        return mockSite;
    }
}
