package org.wordpress.android.ui.suggestion.util;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SuggestionUtilsTest {
    private static final long CURRENT_MS = 1000000;
    private static final long SITE_ID = 123;
    private static final long LONG_ENOUGH_TO_REFRESH = 60 * 1000;

    @Before
    public void setup() {
        SuggestionUtils.lastCallMap = new HashMap<>();
    }

    @Test
    public void should_refresh_true_if_no_previous() {
        assertTrue(SuggestionUtils.shouldRefresh(SITE_ID, CURRENT_MS));
    }

    @Test
    public void should_refresh_true_if_recent_call_was_different_site() {
        long differentSiteId = SITE_ID + 1;
        SuggestionUtils.lastCallMap.put(differentSiteId, CURRENT_MS - 1);
        SuggestionUtils.lastCallMap.put(SITE_ID, CURRENT_MS - LONG_ENOUGH_TO_REFRESH);
        assertTrue(SuggestionUtils.shouldRefresh(SITE_ID, CURRENT_MS));
    }

    @Test
    public void should_refresh_true_if_long_enough_since_last() {
        SuggestionUtils.lastCallMap.put(SITE_ID, CURRENT_MS - LONG_ENOUGH_TO_REFRESH);
        assertTrue(SuggestionUtils.shouldRefresh(SITE_ID, CURRENT_MS));
    }

    @Test
    public void should_refresh_false_if_not_long_enough_since_last() {
        long notLongEnoughToRefresh = (LONG_ENOUGH_TO_REFRESH * 1000) - 1;
        SuggestionUtils.lastCallMap.put(SITE_ID, CURRENT_MS - notLongEnoughToRefresh);
        assertFalse(SuggestionUtils.shouldRefresh(SITE_ID, CURRENT_MS));
    }
}
