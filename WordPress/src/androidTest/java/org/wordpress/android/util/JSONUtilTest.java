package org.wordpress.android.util;

import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtilTest extends AndroidTestCase {

    // Tests nested objects and arrays are copied properly when using JSONUtil.copyJSONObject()
    public void testNestedJSONCopy() {
        JSONObject jsonObject = new JSONObject();

        try {
            // Add some basic properties
            jsonObject.put("boolean", true);
            jsonObject.put("double", 1.0d);
            jsonObject.put("int", 1);
            jsonObject.put("long", 1L);
            jsonObject.put("string", "one");

            // Add a nested object that contains an array
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(1);
            JSONObject nestedObject = new JSONObject();
            nestedObject.put("item1", "item 1");
            nestedObject.put("item2", "item 2");
            nestedObject.put("item3", new JSONArray("[{\"id\": 0,\"name\": \"Summers Mccormick\"},{\"id\": 1,\"name\": \"Shauna Mcneil\"},{\"id\": 2,\"name\": \"Kitty Riggs\"}]"));
            jsonArray.put(nestedObject);

            jsonObject.put("array", jsonArray);

            JSONObject objectCopy = JSONUtil.copyJSONObject(jsonObject);

            assertTrue(objectCopy.getBoolean("boolean"));
            assertEquals(objectCopy.getDouble("double"), 1.0d);
            assertEquals(objectCopy.getInt("int"), 1);
            assertEquals(objectCopy.getLong("long"), 1L);
            assertEquals(objectCopy.getString("string"), "one");

            int firstArrayItem = JSONUtil.queryJSON(objectCopy, "array[0]", -1);
            assertEquals(firstArrayItem, 1);

            assertEquals(JSONUtil.queryJSON(objectCopy, "array[1].item1", ""), "item 1");
            assertEquals(JSONUtil.queryJSON(objectCopy, "array[1].item2", ""), "item 2");
            assertEquals(JSONUtil.queryJSON(objectCopy, "array[1].item3[2].name", ""), "Kitty Riggs");
        } catch (JSONException e) {
            fail("JSON Exception occurred, please check source JSON of test");
        }
    }
}
