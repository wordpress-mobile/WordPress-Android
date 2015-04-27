package org.wordpress.android.editor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.wordpress.android.editor.Utils.buildMapFromKeyValuePairs;
import static org.wordpress.android.editor.Utils.getChangeMapFromSets;
import static org.wordpress.android.editor.Utils.splitDelimitedString;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class UtilsTest {

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
}
