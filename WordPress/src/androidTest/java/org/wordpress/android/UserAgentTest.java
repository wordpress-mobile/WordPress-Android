package org.wordpress.android;

import android.webkit.WebSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.fluxc.network.UserAgent;

import javax.inject.Inject;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class UserAgentTest {
    /**
     * Copy of {@link WordPress#USER_AGENT_APPNAME}.
     * Copied here in order to be able to catch User-Agent changes and verify that they're intentional.
     */
    private static final String USER_AGENT_APPNAME = "wp-android";

    @Rule(order = 0)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 1)
    public InitializationRule initRule = new InitializationRule();

    @Inject UserAgent mUserAgent;

    @Before
    public void setUp() {
        hiltRule.inject();
    }

    @Test
    public void testGetUserAgentAndGetDefaultUserAgent() {
        String userAgent = mUserAgent.toString();
        assertNotNull("User-Agent must be set", userAgent);
        assertTrue("User-Agent must not be an empty string", userAgent.length() > 0);
        assertTrue("User-Agent must contain app name substring", userAgent.contains(USER_AGENT_APPNAME));

        String defaultUserAgent = WebSettings.getDefaultUserAgent(AppInitializer.Companion.getContext());
        assertNotNull("Default User-Agent must be set", defaultUserAgent);
        assertTrue("Default User-Agent must not be an empty string", defaultUserAgent.length() > 0);
        assertFalse("Default User-Agent must not contain app name", defaultUserAgent.contains(USER_AGENT_APPNAME));

        assertTrue("User-Agent must be derived from default User-Agent", userAgent.contains(defaultUserAgent));
    }
}
