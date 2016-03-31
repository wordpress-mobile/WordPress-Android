package org.wordpress.android.ui.people.utils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.models.Person;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

public class PeopleUtils {

    public static void fetchUsers(final String siteID, final PeopleUtils.Callback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null) {
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("users");
                        List<Person> people = peopleListFromJSON(jsonArray, siteID);
                        PeopleTable.savePeople(people);

                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }
                    catch (JSONException e) {
                        AppLog.e(T.API, e);
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

    public static List<Person> peopleListFromJSON(JSONArray jsonArray, String siteID) {
        if (jsonArray == null) {
            return null;
        }

        ArrayList<Person> peopleList = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            Person person = Person.fromJSON(jsonArray.optJSONObject(i), siteID);
            peopleList.add(person);
        }

        return peopleList;
    }

    public interface Callback {
        void onSuccess();

        void onError(VolleyError error);
    }
}
