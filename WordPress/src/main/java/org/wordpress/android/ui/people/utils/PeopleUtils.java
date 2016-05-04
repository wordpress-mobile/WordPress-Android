package org.wordpress.android.ui.people.utils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Person;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

public class PeopleUtils {

    public static void fetchUsers(final String siteID, final int localTableBlogId, final FetchUsersCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("users");
                        List<Person> people = peopleListFromJSON(jsonArray, siteID, localTableBlogId);
                        callback.onSuccess(people);

                    }
                    catch (JSONException e) {
                        AppLog.e(T.API, "JSON exception occurred while parsing the response for sites/%s/users: " + e);
                        callback.onJSONException(e);
                    }
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, volleyError);
                if (callback != null) {
                    callback.onError(volleyError);
                }
            }
        };

        String path = String.format("sites/%s/users", siteID);
        WordPress.getRestClientUtilsV1_1().get(path, listener, errorListener);
    }

    public static void updateRole(final String siteID, String userID, String newRole, final int localTableBlogId,
                                  final UpdateUserCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    Person person = Person.fromJSON(jsonObject, siteID, localTableBlogId);
                    callback.onSuccess(person);
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, volleyError);
                if (callback != null) {
                    callback.onError(volleyError);
                }
            }
        };

        try {
            JSONObject jsonObject = new JSONObject();
            JSONArray roles = new JSONArray();
            roles.put(newRole);
            jsonObject.put("roles", roles);

            String path = String.format("sites/%s/users/%s", siteID, userID);
            WordPress.getRestClientUtilsV1_1().post(path, jsonObject, null, listener, errorListener);
        } catch (JSONException e) {
            if (callback != null) {
                callback.onJSONException(e);
            }
        }
    }

    private static List<Person> peopleListFromJSON(JSONArray jsonArray, String siteID, int localTableBlogId) {
        if (jsonArray == null) {
            return null;
        }

        ArrayList<Person> peopleList = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            Person person = Person.fromJSON(jsonArray.optJSONObject(i), siteID, localTableBlogId);
            if (person != null) {
                peopleList.add(person);
            }
        }

        return peopleList;
    }

    public interface FetchUsersCallback extends Callback {
        void onSuccess(List<Person> peopleList);
    }

    public interface UpdateUserCallback extends Callback {
        void onSuccess(Person person);
    }

    public interface Callback {
        void onError(VolleyError error);

        void onJSONException(JSONException e);
    }
}
