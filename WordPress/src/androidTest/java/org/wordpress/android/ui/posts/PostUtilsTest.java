package org.wordpress.android.ui.posts;

import org.junit.Test;

import dagger.hilt.android.testing.HiltAndroidTest;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

@HiltAndroidTest
public class PostUtilsTest {
    @Test
    public void testCollapseShortcodes() {
        String postContent = "Text before first gallery [gallery number=\"one\"]"
                             + " text between galleries"
                             + " [gallery number=\"two\"]"
                             + " text after second gallery"
                             + " [unknown shortcode].";
        String collapsedContent = PostUtils.collapseShortcodes(postContent);

        // make sure [gallery] now exists and [gallery number] does not
        assertTrue(collapsedContent.contains("[gallery]"));
        assertFalse(collapsedContent.contains("[gallery number]"));

        // make sure the unknown shortcode is intact
        assertTrue(collapsedContent.contains("[unknown shortcode]"));
    }

    @Test
    public void testShortcodeSpaces() {
        String postContent = "[   gallery number=\"arst\"     /]";
        String collapsedContent = PostUtils.collapseShortcodes(postContent);
        assertEquals("[gallery]", collapsedContent);
    }

    @Test
    public void testOpeningClosingShortcode() {
        String postContent = "[recipe difficulty=\"easy\"]Put your recipe here.[/recipe]";
        String collapsedContent = PostUtils.collapseShortcodes(postContent);
        assertEquals("[recipe]Put your recipe here.[/recipe]", collapsedContent);
    }
}
