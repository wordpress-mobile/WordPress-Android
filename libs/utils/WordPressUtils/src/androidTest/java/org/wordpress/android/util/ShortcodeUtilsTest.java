package org.wordpress.android.util;

import android.test.InstrumentationTestCase;

public class ShortcodeUtilsTest extends InstrumentationTestCase {
    public void testGetVideoPressShortcodeFromId() {
        assertEquals("[wpvideo abcd1234]", ShortcodeUtils.getVideoPressShortcodeFromId("abcd1234"));
    }

    public void testGetVideoPressShortcodeFromNullId() {
        assertEquals("", ShortcodeUtils.getVideoPressShortcodeFromId(null));
    }

    public void testGetVideoPressIdFromCorrectShortcode() {
        assertEquals("abcd1234", ShortcodeUtils.getVideoPressIdFromShortCode("[wpvideo abcd1234]"));
    }

    public void testGetVideoPressIdFromInvalidShortcode() {
        assertEquals("", ShortcodeUtils.getVideoPressIdFromShortCode("[other abcd1234]"));
    }

    public void testGetVideoPressIdFromNullShortcode() {
        assertEquals("", ShortcodeUtils.getVideoPressIdFromShortCode(null));
    }
}
