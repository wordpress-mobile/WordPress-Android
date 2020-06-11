package org.wordpress.android.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class JSONUtilsTest {
    @Test
    public void testQueryJSONNullSource1() {
        JSONUtils.queryJSON((JSONObject) null, "", "");
    }

    @Test
    public void testQueryJSONNullSource2() {
        JSONUtils.queryJSON((JSONArray) null, "", "");
    }

    @Test
    public void testQueryJSONNullQuery1() {
        JSONUtils.queryJSON(new JSONObject(), null, "");
    }

    @Test
    public void testQueryJSONNullQuery2() {
        JSONUtils.queryJSON(new JSONArray(), null, "");
    }

    @Test
    public void testQueryJSONNullReturnValue1() {
        JSONUtils.queryJSON(new JSONObject(), "", null);
    }

    @Test
    public void testQueryJSONNullReturnValue2() {
        JSONUtils.queryJSON(new JSONArray(), "", null);
    }
}
