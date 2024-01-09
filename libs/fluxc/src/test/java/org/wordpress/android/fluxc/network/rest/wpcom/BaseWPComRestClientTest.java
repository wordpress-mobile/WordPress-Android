package org.wordpress.android.fluxc.network.rest.wpcom;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.network.OkHttpStack;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.reader.ReaderRestClient;

import java.io.File;

import okhttp3.OkHttpClient;

@RunWith(RobolectricTestRunner.class)
public class BaseWPComRestClientTest {
    final Context mAppContext = RuntimeEnvironment.application.getApplicationContext();
    final BaseWPComRestClient mClassToTest = new ReaderRestClient(
            mAppContext,
            new Dispatcher(),
            new RequestQueue(
                    new DiskBasedCache(new File("")),
                    new BasicNetwork(new OkHttpStack(new OkHttpClient()))
            ),
            new AccessToken(mAppContext),
            new UserAgent(mAppContext, "app")
    );

    @Test
    public void shouldUseUnderscoreLocaleParameterForWPComV2Requests() {
        final String queryParameter =
                mClassToTest.getHttpUrlWithLocale("https://public-api.wordpress.com/wpcom/v2/something")
                            .queryParameter("_locale");
        assertThat(queryParameter, not(nullValue()));
    }

    @Test
    public void shouldUseUnderscoreLocaleParameterForWPComV3Requests() {
        final String queryParameter =
                mClassToTest.getHttpUrlWithLocale("https://public-api.wordpress.com/wpcom/v3/something")
                            .queryParameter("_locale");
        assertThat(queryParameter, not(nullValue()));
    }

    @Test
    public void shouldUseLocaleParameterForV1Requests() {
        final String queryParameter =
                mClassToTest.getHttpUrlWithLocale("https://public-api.wordpress.com/rest/v1/")
                            .queryParameter("locale");
        assertThat(queryParameter, not(nullValue()));
    }
}
