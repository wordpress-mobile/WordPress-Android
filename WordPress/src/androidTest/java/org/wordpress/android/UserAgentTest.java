package org.wordpress.android;

import junit.framework.TestCase;

public class UserAgentTest extends TestCase {

    /**
     * Copy of {@link WordPress#USER_AGENT_APPNAME}.
     * Copied here in order to be able to catch User-Agent changes and verify that they're intentional.
     */
    private static final String USER_AGENT_APPNAME = "wp-android";

    public void testGetDefaultUserAgent() {
        String defaultUserAgent = WordPress.getDefaultUserAgent();
        assertNotNull("Default User-Agent must be set", defaultUserAgent);
        assertTrue("Default User-Agent must not be an empty string", defaultUserAgent.length() > 0);
        assertFalse("Default User-Agent must not contain app name", defaultUserAgent.contains(USER_AGENT_APPNAME));
    }

    public void testGetUserAgent() {
        String userAgent = WordPress.getUserAgent();
        assertNotNull("User-Agent must be set", userAgent);
        assertTrue("User-Agent must not be an empty string", userAgent.length() > 0);
        assertTrue("User-Agent must contain app name substring", userAgent.contains(USER_AGENT_APPNAME));

        String defaultUserAgent = WordPress.getDefaultUserAgent();
        assertTrue("User-Agent must be derived from default User-Agent", userAgent.contains(defaultUserAgent));
    }
}
