package org.wordpress.android.fluxc.theme;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestSiteSqlUtils;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeRestClient;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.ThemeSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.site.SiteUtils;
import org.wordpress.android.fluxc.store.ThemeStore;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class ThemeStoreUnitTest {
    private final ThemeStore mThemeStore = new ThemeStore(new Dispatcher(), Mockito.mock(ThemeRestClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.getApplication().getApplicationContext();
        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testActiveTheme() throws SiteSqlUtils.DuplicateSiteException {
        final SiteModel site = SiteUtils.generateWPComSite();
        TestSiteSqlUtils.INSTANCE.getSiteSqlUtils().insertOrUpdateSite(site);
        assertNull(mThemeStore.getActiveThemeForSite(site));

        final ThemeModel firstTheme = generateTestTheme(site.getId(), "first-active", "First Active");
        final ThemeModel secondTheme = generateTestTheme(site.getId(), "second-active", "Second Active");
        firstTheme.setActive(true);
        secondTheme.setActive(true);

        // set first theme active and verify
        mThemeStore.setActiveThemeForSite(site, firstTheme);
        List<ThemeModel> activeThemes = ThemeSqlUtils.getActiveThemeForSite(site);
        assertNotNull(activeThemes);
        assertEquals(1, activeThemes.size());
        ThemeModel firstActiveTheme = activeThemes.get(0);
        assertEquals(firstTheme.getThemeId(), firstActiveTheme.getThemeId());
        assertEquals(firstTheme.getName(), firstActiveTheme.getName());

        // set second theme active and verify
        mThemeStore.setActiveThemeForSite(site, secondTheme);
        activeThemes = ThemeSqlUtils.getActiveThemeForSite(site);
        assertNotNull(activeThemes);
        assertEquals(1, activeThemes.size());
        ThemeModel secondActiveTheme = activeThemes.get(0);
        assertEquals(secondTheme.getThemeId(), secondActiveTheme.getThemeId());
        assertEquals(secondTheme.getName(), secondActiveTheme.getName());
    }

    @Test
    public void testInsertOrUpdateTheme() throws SiteSqlUtils.DuplicateSiteException {
        final SiteModel site = SiteUtils.generateJetpackSiteOverRestOnly();
        TestSiteSqlUtils.INSTANCE.getSiteSqlUtils().insertOrUpdateSite(site);

        final String testThemeId = "fluxc-ftw";
        final String testThemeName = "FluxC FTW";
        final String testUpdatedName = testThemeName + " v2";
        final ThemeModel insertTheme = generateTestTheme(site.getId(), testThemeId, testThemeName);

        // verify theme doesn't already exist
        assertNull(mThemeStore.getInstalledThemeByThemeId(site, testThemeId));

        // insert new theme and verify it exists
        ThemeSqlUtils.insertOrUpdateSiteTheme(site, insertTheme);
        ThemeModel insertedTheme = mThemeStore.getInstalledThemeByThemeId(site, testThemeId);
        assertNotNull(insertedTheme);
        assertEquals(testThemeName, insertedTheme.getName());

        // update the theme and verify the updated attributes
        insertedTheme.setName(testUpdatedName);
        ThemeSqlUtils.insertOrUpdateSiteTheme(site, insertedTheme);
        insertedTheme = mThemeStore.getInstalledThemeByThemeId(site, testThemeId);
        assertNotNull(insertedTheme);
        assertEquals(testUpdatedName, insertedTheme.getName());
    }

    @Test
    public void testInsertOrReplaceWpThemes() {
        final List<ThemeModel> firstTestThemes = generateThemesTestList(20);
        final List<ThemeModel> secondTestThemes = generateThemesTestList(30);
        final List<ThemeModel> thirdTestThemes = generateThemesTestList(10);

        // first add 20 themes and make sure the count is correct
        ThemeSqlUtils.insertOrReplaceWpComThemes(firstTestThemes);
        assertEquals(20, mThemeStore.getWpComThemes().size());

        // next add a larger list of themes (with 20 being duplicates) and make sure the count is correct
        ThemeSqlUtils.insertOrReplaceWpComThemes(secondTestThemes);
        assertEquals(30, mThemeStore.getWpComThemes().size());

        // lastly add a smaller list of themes (all duplicates) and make sure count is correct
        ThemeSqlUtils.insertOrReplaceWpComThemes(thirdTestThemes);
        assertEquals(10, mThemeStore.getWpComThemes().size());
    }

    @Test
    public void testInsertOrReplaceInstalledThemes() throws SiteSqlUtils.DuplicateSiteException {
        final SiteModel site = SiteUtils.generateJetpackSiteOverRestOnly();
        TestSiteSqlUtils.INSTANCE.getSiteSqlUtils().insertOrUpdateSite(site);

        final List<ThemeModel> firstTestThemes = generateThemesTestList(5);
        final List<ThemeModel> secondTestThemes = generateThemesTestList(10);
        final List<ThemeModel> thirdTestThemes = generateThemesTestList(1);

        // first add 5 installed themes
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, firstTestThemes);
        assertEquals(firstTestThemes.size(), mThemeStore.getThemesForSite(site).size());

        // then replace them all with a new list of 10
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, secondTestThemes);
        assertEquals(secondTestThemes.size(), mThemeStore.getThemesForSite(site).size());

        // then replace them all with a single theme
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, thirdTestThemes);
        assertEquals(thirdTestThemes.size(), mThemeStore.getThemesForSite(site).size());
    }

    @Test
    public void testRemoveThemesWithNoSite() {
        final List<ThemeModel> testThemes = generateThemesTestList(20);

        // insert and verify count
        assertEquals(0, mThemeStore.getWpComThemes().size());
        ThemeSqlUtils.insertOrReplaceWpComThemes(testThemes);
        assertEquals(testThemes.size(), mThemeStore.getWpComThemes().size());

        // remove and verify count
        ThemeSqlUtils.removeWpComThemes();
        assertEquals(0, mThemeStore.getWpComThemes().size());
    }

    @Test
    public void testRemoveInstalledSiteThemes() throws SiteSqlUtils.DuplicateSiteException {
        final SiteModel site = SiteUtils.generateJetpackSiteOverRestOnly();
        TestSiteSqlUtils.INSTANCE.getSiteSqlUtils().insertOrUpdateSite(site);

        final List<ThemeModel> testThemes = generateThemesTestList(5);

        // add site themes and verify count
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, testThemes);
        assertEquals(testThemes.size(), mThemeStore.getThemesForSite(site).size());

        // remove and verify count
        ThemeSqlUtils.removeSiteThemes(site);
        assertEquals(0, mThemeStore.getThemesForSite(site).size());
    }

    private ThemeModel generateTestTheme(int siteId, String themeId, String themeName) {
        @SuppressWarnings("deprecation") ThemeModel theme = new ThemeModel();
        theme.setLocalSiteId(siteId);
        theme.setThemeId(themeId);
        theme.setName(themeName);
        return theme;
    }

    private List<ThemeModel> generateThemesTestList(int num) {
        List<ThemeModel> testThemes = new ArrayList<>();
        for (int i = 0; i < num; ++i) {
            testThemes.add(generateTestTheme(0, "themeid" + i, "themename" + i));
        }
        return testThemes;
    }
}
