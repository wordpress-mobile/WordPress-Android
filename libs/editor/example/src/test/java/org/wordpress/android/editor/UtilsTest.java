package org.wordpress.android.editor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.wordpress.android.editor.Utils.buildMapFromKeyValuePairs;
import static org.wordpress.android.editor.Utils.decodeHtml;
import static org.wordpress.android.editor.Utils.escapeHtml;
import static org.wordpress.android.editor.Utils.getChangeMapFromSets;
import static org.wordpress.android.editor.Utils.splitDelimitedString;
import static org.wordpress.android.editor.Utils.splitValuePairDelimitedString;
import static org.wordpress.android.editor.Utils.getUrlFromClipboard;

@Config(sdk = 18)
@RunWith(RobolectricTestRunner.class)
public class UtilsTest {

    @Test
    public void testEscapeHtml() {
        // Test null
        assertEquals(null, escapeHtml(null));
    }

    @Test
    public void testDecodeHtml() {
        // Test null
        assertEquals(null, decodeHtml(null));

        // Test normal usage
        assertEquals("http://www.wordpress.com/", decodeHtml("http%3A%2F%2Fwww.wordpress.com%2F"));
    }

    @Test
    public void testSplitDelimitedString() {
        Set<String> splitString = new HashSet<>();

        // Test normal usage
        splitString.add("p");
        splitString.add("bold");
        splitString.add("justifyLeft");

        assertEquals(splitString, splitDelimitedString("p~bold~justifyLeft", "~"));

        // Test empty string
        assertEquals(Collections.emptySet(), splitDelimitedString("", "~"));
    }

    @Test
    public void testSplitValuePairDelimitedString() {
        // Test usage with a URL containing the delimiter
        Set<String> keyValueSet = new HashSet<>();
        keyValueSet.add("url=http://www.wordpress.com/~user");
        keyValueSet.add("title=I'm a link!");

        List<String> identifiers = new ArrayList<>();
        identifiers.add("url");
        identifiers.add("title");

        assertEquals(keyValueSet, splitValuePairDelimitedString(
                "url=http://www.wordpress.com/~user~title=I'm a link!", "~", identifiers));

        // Test usage with a matching identifier but no delimiters
        keyValueSet.clear();
        keyValueSet.add("url=http://www.wordpress.com/");

        assertEquals(keyValueSet, splitValuePairDelimitedString("url=http://www.wordpress.com/", "~", identifiers));

        // Test usage with no matching identifier and no delimiters
        keyValueSet.clear();
        keyValueSet.add("something=something else");

        assertEquals(keyValueSet, splitValuePairDelimitedString("something=something else", "~", identifiers));
    }

    @Test
    public void testBuildMapFromKeyValuePairs() {
        Set<String> keyValueSet = new HashSet<>();
        Map<String, String> expectedMap = new HashMap<>();

        // Test normal usage
        keyValueSet.add("id=test");
        keyValueSet.add("name=example");

        expectedMap.put("id", "test");
        expectedMap.put("name", "example");

        assertEquals(expectedMap, buildMapFromKeyValuePairs(keyValueSet));

        // Test mixed valid and invalid entries
        keyValueSet.clear();
        keyValueSet.add("test");
        keyValueSet.add("name=example");

        expectedMap.clear();
        expectedMap.put("name", "example");

        assertEquals(expectedMap, buildMapFromKeyValuePairs(keyValueSet));

        // Test multiple '=' (should split at the first `=` and treat the rest of them as part of the string)
        keyValueSet.clear();
        keyValueSet.add("id=test");
        keyValueSet.add("contents=some text\n<a href=\"http://wordpress.com\">WordPress</a>");

        expectedMap.clear();
        expectedMap.put("id", "test");
        expectedMap.put("contents", "some text\n<a href=\"http://wordpress.com\">WordPress</a>");

        assertEquals(expectedMap, buildMapFromKeyValuePairs(keyValueSet));

        // Test invalid entry
        keyValueSet.clear();
        keyValueSet.add("test");

        assertEquals(Collections.emptyMap(), buildMapFromKeyValuePairs(keyValueSet));

        // Test empty sets
        assertEquals(Collections.emptyMap(), buildMapFromKeyValuePairs(Collections.<String>emptySet()));
    }

    @Test
    public void testGetChangeMapFromSets() {
        Set<String> oldSet = new HashSet<>();
        Set<String> newSet = new HashSet<>();
        Map<String, Boolean> expectedMap = new HashMap<>();

        // Test normal usage
        oldSet.add("p");
        oldSet.add("bold");
        oldSet.add("justifyLeft");

        newSet.add("p");
        newSet.add("justifyRight");

        expectedMap.put("bold", false);
        expectedMap.put("justifyLeft", false);
        expectedMap.put("justifyRight", true);

        assertEquals(expectedMap, getChangeMapFromSets(oldSet, newSet));

        // Test no changes
        oldSet.clear();
        oldSet.add("p");
        oldSet.add("bold");

        newSet.clear();
        newSet.add("p");
        newSet.add("bold");

        assertEquals(Collections.emptyMap(), getChangeMapFromSets(oldSet, newSet));

        // Test empty sets
        assertEquals(Collections.emptyMap(), getChangeMapFromSets(Collections.emptySet(), Collections.emptySet()));
    }

    @Test
    public void testClipboardUrlWithNullContext() {
        assertNull(getUrlFromClipboard(null));
    }

    @Test
    public void testClipboardUrlWithNoClipData() {
        assertNull(getClipboardUrlHelper(0, null));
    }

    @Test
    public void testClipboardUrlWithNonUriData() {
        assertNull(getClipboardUrlHelper(1, "not a URL"));
    }

    @Test
    public void testClipboardUrlWithLocalUriData() {
        assertNull(getClipboardUrlHelper(1, "file://test.png"));
    }

    @Test
    public void testClipboardWithUrlData() {
        String testUrl = "google.com";
        assertEquals(testUrl, getClipboardUrlHelper(1, testUrl));
    }

    private String getClipboardUrlHelper(int itemCount, String clipText) {
        ClipData.Item mockItem = mock(ClipData.Item.class);
        when(mockItem.getText()).thenReturn(clipText);

        ClipData mockPrimary = mock(ClipData.class);
        when(mockPrimary.getItemCount()).thenReturn(itemCount);
        when(mockPrimary.getItemAt(0)).thenReturn(mockItem);

        ClipboardManager mockManager = mock(ClipboardManager.class);
        when(mockManager.getPrimaryClip()).thenReturn(mockPrimary);

        Context mockContext = mock(Context.class);
        when(mockContext.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(mockManager);

        return getUrlFromClipboard(mockContext);
    }
}
