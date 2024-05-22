package org.wordpress.android.fluxc.network.rest.wpcom;

import android.content.Context;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.utils.WPComRestClientUtils;

import okhttp3.HttpUrl;

@RunWith(RobolectricTestRunner.class)
public class BaseWPComRestClientTest {
    final Context mAppContext = RuntimeEnvironment.application.getApplicationContext();

    @Test
    public void shouldUseUnderscoreLocaleParameterForWPComV2Requests() {
        final HttpUrl url = WPComRestClientUtils.getHttpUrlWithLocale(mAppContext,
                "https://public-api.wordpress.com/wpcom/v2/something");
        assertNotNull(url);
        assertThat(url.queryParameter("_locale"), not(nullValue()));
    }

    @Test
    public void shouldUseUnderscoreLocaleParameterForWPComV3Requests() {
        final HttpUrl url = WPComRestClientUtils.getHttpUrlWithLocale(mAppContext,
                "https://public-api.wordpress.com/wpcom/v3/something");
        assertNotNull(url);
        assertThat(url.queryParameter("_locale"), not(nullValue()));
    }

    @Test public void shouldUseLocaleParameterForV1Requests() {
        final HttpUrl url =
                WPComRestClientUtils.getHttpUrlWithLocale(mAppContext, "https://public-api.wordpress.com/rest/v1/");
        assertNotNull(url);
        assertThat(url.queryParameter("locale"), not(nullValue()));
    }
}
