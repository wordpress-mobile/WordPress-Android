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

@RunWith(RobolectricTestRunner.class)
public class ThemeStoreUnitTest {
    private ThemeStore mThemeStore = new ThemeStore(new Dispatcher(), Mockito.mock(ThemeRestClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();
        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertAndReplaceWpThemes() {
        final List<ThemeModel> firstTestThemes = generateThemesTestList(20);
        final List<ThemeModel> secondTestThemes = generateThemesTestList(30);
        final List<ThemeModel> thirdTestThemes = generateThemesTestList(10);

        // first add 20 themes and make sure the count is correct
        ThemeSqlUtils.insertOrReplaceWpThemes(firstTestThemes);
        assertEquals(20, mThemeStore.getWpThemes().size());

        // next add 30 themes (with 20 being duplicates) and make sure the count is correct
        ThemeSqlUtils.insertOrReplaceWpThemes(secondTestThemes);
        assertEquals(30, mThemeStore.getWpThemes().size());

        // lastly add 10 themes (all duplicates) and make sure count is unchanged
        ThemeSqlUtils.insertOrReplaceWpThemes(thirdTestThemes);
        assertEquals(30, mThemeStore.getWpThemes().size());
    }

    @Test
    public void testInsertAndReplaceInstalledThemes() throws SiteSqlUtils.DuplicateSiteException {
        final SiteModel site = SiteUtils.generateJetpackSiteOverRestOnly();
        SiteSqlUtils.insertOrUpdateSite(site);

        final List<ThemeModel> firstTestThemes = generateThemesTestList(5);
        final List<ThemeModel> secondTestThemes = generateThemesTestList(10);
        final List<ThemeModel> thirdTestThemes = generateThemesTestList(1);

        // first add 5 installed themes
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, firstTestThemes);
        assertEquals(5, mThemeStore.getThemesForSite(site).size());

        // then replace them all with a new list of 10
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, secondTestThemes);
        assertEquals(10, mThemeStore.getThemesForSite(site).size());

        // then replace them all with a single theme
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, thirdTestThemes);
        assertEquals(1, mThemeStore.getThemesForSite(site).size());
    }

    private List<ThemeModel> generateThemesTestList(int num) {
        List<ThemeModel> testThemes = new ArrayList<>();
        for (int i = 0; i < num; ++i) {
            ThemeModel theme = new ThemeModel();
            theme.setLocalSiteId(0);
            theme.setThemeId("themeid" + i);
            theme.setName("themename" + i);
            testThemes.add(theme);
        }
        return testThemes;
    }
}
