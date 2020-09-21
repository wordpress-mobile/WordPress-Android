package org.wordpress.android.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShortcodeUtilsTest {
    @Test
    public void testGetVideoPressShortcodeFromId() {
        assertEquals("[wpvideo abcd1234]", ShortcodeUtils.getVideoPressShortcodeFromId("abcd1234"));
    }

    @Test
    public void testGetVideoPressShortcodeFromNullId() {
        assertEquals("", ShortcodeUtils.getVideoPressShortcodeFromId(null));
    }

    @Test
    public void testGetVideoPressIdFromCorrectShortcode() {
        assertEquals("abcd1234", ShortcodeUtils.getVideoPressIdFromShortCode("[wpvideo abcd1234]"));
    }

    @Test
    public void testGetVideoPressIdFromInvalidShortcode() {
        assertEquals("", ShortcodeUtils.getVideoPressIdFromShortCode("[other abcd1234]"));
    }

    @Test
    public void testGetVideoPressIdFromNullShortcode() {
        assertEquals("", ShortcodeUtils.getVideoPressIdFromShortCode(null));
    }
}
