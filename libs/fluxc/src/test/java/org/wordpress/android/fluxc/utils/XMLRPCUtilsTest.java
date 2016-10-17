package org.wordpress.android.fluxc.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(RobolectricTestRunner.class)
public class XMLRPCUtilsTest {
    @Test
    public void testDefaultValueString() {
        assertThat(XMLRPCUtils.safeGetMapValue(new HashMap<>(), "test"), equalTo("test"));
    }

    @Test
    public void testGetValueString() {
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("ponies"), "nope"), equalTo("ponies"));
    }

    @Test
    public void testDefaultValueBool() {
        assertThat(XMLRPCUtils.safeGetMapValue(new HashMap<>(), true), equalTo(true));
    }

    @Test
    public void testGetValueBool() {
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("0"), true), equalTo(false));
    }

    @Test
    public void testGetValueBool2() {
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("false"), true), equalTo(false));
    }

    @Test
    public void testDefaultValueLong() {
        assertThat(XMLRPCUtils.safeGetMapValue(new HashMap<>(), 42L), equalTo(42L));
    }

    @Test
    public void testGetValueLong() {
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("42"), 0L), equalTo(42L));
    }

    @Test
    public void testGetValueLong2() {
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("nope"), 42L), equalTo(42L));
    }

    @Test
    public void testDefaultValueInt() {
        assertThat(XMLRPCUtils.safeGetMapValue(new HashMap<>(), 42), equalTo(42));
    }

    @Test
    public void testGetValueInt() {
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("42"), 0), equalTo(42));
    }

    @Test
    public void testDefaultValueFloat() {
        assertThat(XMLRPCUtils.safeGetMapValue(new HashMap<>(), 42.42f), equalTo(42.42f));
    }

    @Test
    public void testGetValueFloat() {
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("42.42"), 0f), equalTo(42.42f));
    }

    @Test
    public void testDefaultValueDouble() {
        assertThat(XMLRPCUtils.safeGetMapValue(new HashMap<>(), 42.42), equalTo(42.42));
    }

    @Test
    public void testGetValueDouble() {
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("42.42"), 0.0), equalTo(42.42));
    }

    @Test
    public void testGetValueDouble2() {
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("false"), 42.42), equalTo(42.42));
    }

    private static Map<String, String> getTestMapForValue(String value) {
        Map<String, String> map = new HashMap<>();
        map.put("key", "test-key");
        map.put("id", "42");
        map.put("value", value);
        return map;
    }
}
