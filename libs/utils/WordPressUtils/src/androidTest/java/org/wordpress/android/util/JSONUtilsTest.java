package org.wordpress.android.util;

import android.test.InstrumentationTestCase;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtilsTest extends InstrumentationTestCase {
    public void testQueryJSONNullSource1() {
        JSONUtils.queryJSON((JSONObject) null, "", "");
    }

    public void testQueryJSONNullSource2() {
        JSONUtils.queryJSON((JSONArray) null, "", "");
    }

    public void testQueryJSONNullQuery1() {
        JSONUtils.queryJSON(new JSONObject(), null, "");
    }

    public void testQueryJSONNullQuery2() {
        JSONUtils.queryJSON(new JSONArray(), null, "");
    }

    public void testQueryJSONNullReturnValue1() {
        JSONUtils.queryJSON(new JSONObject(), "", null);
    }

    public void testQueryJSONNullReturnValue2() {
        JSONUtils.queryJSON(new JSONArray(), "", null);
    }
}
