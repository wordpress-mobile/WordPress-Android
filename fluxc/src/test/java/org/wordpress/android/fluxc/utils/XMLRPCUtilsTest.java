package org.wordpress.android.fluxc.utils;

import org.junit.Test;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils;

import java.sql.Time;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

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

    @Test
    public void testGetValueDate() {
        Date theDate = new Date();
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue(theDate), new Date(4244)), equalTo(theDate));
    }

    @Test(expected = RuntimeException.class)
    public void testGetValueInvalidType() {
        // Something bad should happen if we try this - Random isn't a possible type the XML-RPC deserializer would
        // parse into, so it's guaranteed to always return the default value, without us ever knowing that we're
        // asking safeGetMapValue() for an impossible type
        Random thing = new Random();
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue("text"), thing), equalTo(thing));
    }

    @Test(expected = RuntimeException.class)
    public void testGetValueInvalidTypeSubclass() {
        // If we pass a Date subclass as the default value, we might expect it to work as a match for a Date entry
        // But it doesn't - we'll always receive the default value
        // Something bad should happen if we try this too - we should be warned that we're giving safeGetMapValue()
        // an impossible type
        Date theDate = new Date();
        assertThat(XMLRPCUtils.safeGetMapValue(getTestMapForValue(theDate), new Time(4244)), equalTo(theDate));
    }

    private static Map<String, String> getTestMapForValue(String value) {
        Map<String, String> map = new HashMap<>();
        map.put("key", "test-key");
        map.put("id", "42");
        map.put("value", value);
        return map;
    }

    private static Map<String, Object> getTestMapForValue(Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "test-key");
        map.put("id", "42");
        map.put("value", value);
        return map;
    }
}
