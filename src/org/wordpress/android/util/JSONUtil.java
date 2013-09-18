package org.wordpress.android.util;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class JSONUtil {
    private static String QUERY_SEPERATOR=".";
    private static String QUERY_ARRAY_INDEX_START="[";
    private static String QUERY_ARRAY_INDEX_END="]";
    private static String QUERY_ARRAY_FIRST="first";
    private static String QUERY_ARRAY_LAST="last";

    private static final String JSON_NULL_STR = "null";

    private static final String TAG="JSONUtil";
    /**
     * Given a JSONObject and a key path (e.g property.child) and a default it will
     * traverse the object graph and pull out the desired property
     */
    public static <U> U queryJSON(JSONObject source, String query, U defaultObject){
        int nextSeperator = query.indexOf(QUERY_SEPERATOR);
        int nextIndexStart = query.indexOf(QUERY_ARRAY_INDEX_START);
        if (nextSeperator == -1 && nextIndexStart == -1) {
            // last item let's get it
            try {
                if(!source.has(query))
                    return defaultObject;
                Object result = source.get(query);
                if (result.getClass().isAssignableFrom(defaultObject.getClass())) {
                    return (U) result;
                } else {
                    return defaultObject;
                }
            } catch (JSONException e) {
                Log.e(TAG, String.format("Could not complete query %s", query), e);
                return defaultObject;
            } catch (ClassCastException e) {
                Log.e(TAG, String.format("Could not cast object %s", query), e);
                return defaultObject;
            }
        }
        int endQuery;
        if (nextSeperator == -1 || nextIndexStart == -1) {
            endQuery = Math.max(nextSeperator, nextIndexStart);
        } else {
            endQuery = Math.min(nextSeperator, nextIndexStart);
        }
        String nextQuery = query.substring(endQuery);
        String key = query.substring(0, endQuery);
        try {
            if (nextQuery.indexOf(QUERY_SEPERATOR) == 0) {
                return queryJSON(source.getJSONObject(key), nextQuery.substring(1), defaultObject);
            } else if (nextQuery.indexOf(QUERY_ARRAY_INDEX_START) == 0){
                return queryJSON(source.getJSONArray(key), nextQuery, defaultObject);
            } else if (!nextQuery.equals("")){
                return defaultObject;
            }
            Object result = source.get(key);
            if (result.getClass().isAssignableFrom(defaultObject.getClass())) {
                return (U) result;
            } else {
                return defaultObject;
            }
        } catch (java.lang.ClassCastException e) {
            Log.e(TAG, String.format("Could not cast object at %s", query), e);
            return defaultObject;
        } catch (JSONException e) {
            Log.e(TAG, String.format("Could not complete query %s", query), e);
            return defaultObject;
        }
    }
    /**
     * Given a JSONArray and a query (e.g. [0].property) it will traverse the array and
     * pull out the requested property.
     * 
     * Acceptable indexes include negative numbers to reference items from the end of
     * the list as well as "last" and "first" as more explicit references to "0" and "-1"
     */
    public static <U> U queryJSON(JSONArray source, String query, U defaultObject){
        // query must start with [ have an index and then have ]
        int indexStart = query.indexOf(QUERY_ARRAY_INDEX_START);
        int indexEnd = query.indexOf(QUERY_ARRAY_INDEX_END);
        if (indexStart == -1 || indexEnd == -1 || indexStart > indexEnd) {
            return defaultObject;
        }
        // get "index" from "[index]"
        String indexStr = query.substring(indexStart + 1, indexEnd);
        int index;
        if (indexStr.equals(QUERY_ARRAY_FIRST)) {
            index = 0;
        } else if (indexStr.equals(QUERY_ARRAY_LAST)){
            index = -1;
        } else {
            index = Integer.parseInt(indexStr);
        }
        if(index < 0){
            index = source.length() + index;
        }
        // copy remaining query
        String remainingQuery = query.substring(indexEnd + 1);
        try {
            if (remainingQuery.indexOf(QUERY_ARRAY_INDEX_START) == 0) {
                return queryJSON(source.getJSONArray(index), remainingQuery, defaultObject);
            } else if(remainingQuery.indexOf(QUERY_SEPERATOR) == 0){
                return queryJSON(source.getJSONObject(index), remainingQuery.substring(1), defaultObject);
            } else if(!remainingQuery.equals("")){
                // TODO throw an exception since the query isn't valid?
                Log.d(TAG, String.format("Incorrect query for next object %s", remainingQuery));
                return defaultObject;
            }
            Object result = source.get(index);
            if (result.getClass().isAssignableFrom(defaultObject.getClass())) {
                return (U) result;
            } else {
                return defaultObject;
            }
        } catch(java.lang.ClassCastException e){
            Log.e(TAG, String.format("Could not cast object at %s", query), e);
            return defaultObject;
        } catch (JSONException e) {
            Log.e(TAG, String.format("Could not complete query %s", query), e);
            return defaultObject;
        }

    }

    /**
     * Convert a JSONArray (expected to contain strings) in a string list
     */
    public static ArrayList<String> fromJSONArrayToStringList(JSONArray jsonArray) {
        ArrayList<String> stringList = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                stringList.add(jsonArray.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return stringList;
    }
    
    /**
     * Convert a string list in a JSONArray
     */
    public static JSONArray fromStringListToJSONArray(ArrayList<String> stringList) {
        JSONArray jsonArray = new JSONArray();
        if(stringList != null)
            for (int i = 0; i < stringList.size(); i++) {
                jsonArray.put(stringList.get(i));
            }
        
        return jsonArray;
    }

    /*
     * wrapper for JSONObject.optString() which handles "null" values
     */
    public static String getString(JSONObject json, String name) {
        String value = json.optString(name);
        // return empty string for "null"
        if (JSON_NULL_STR.equals(value))
            return "";
        return value;
    }

    /*
     * use with strings that contain HTML entities
     */
    public static String getStringDecoded(JSONObject json, String name) {
        String value = getString(json, name);
        // return the value if it doesn't contain an ampersand, otherwise return the value with entities converted
        if (!value.contains("&"))
            return value;
        return StringUtils.unescapeHTML(value);
    }

    /*
     * replacement for JSONObject.optBoolean()  - optBoolean() only checks for "true" and "false",
     * but our API sometimes uses "0" to denote false
     */
    public static boolean getBool(JSONObject json, String name) {
        String value = getString(json, name);
        if (TextUtils.isEmpty(value))
            return false;
        if (value.equals("0"))
            return false;
        if (value.equalsIgnoreCase("false"))
            return false;
        return true;
    }
}