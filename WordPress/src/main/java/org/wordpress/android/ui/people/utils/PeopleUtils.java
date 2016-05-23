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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeopleUtils {

    public static void fetchUsers(final String blogId, final int localTableBlogId, final FetchUsersCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("users");
                        List<Person> people = peopleListFromJSON(jsonArray, blogId, localTableBlogId);
                        callback.onSuccess(people);
                    }
                    catch (JSONException e) {
                        AppLog.e(T.API, "JSON exception occurred while parsing the response for sites/%s/users: " + e);
                        callback.onError();
                    }
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, volleyError);
                if (callback != null) {
                    callback.onError();
                }
            }
        };

        String path = String.format("sites/%s/users", blogId);
        WordPress.getRestClientUtilsV1_1().get(path, listener, errorListener);
    }

    public static void updateRole(final String blogId, long personID, String newRole, final int localTableBlogId,
                                  final UpdateUserCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        Person person = Person.fromJSON(jsonObject, blogId, localTableBlogId);
                        if (person != null) {
                            callback.onSuccess(person);
                        } else {
                            AppLog.e(T.API, "Couldn't map jsonObject + " + jsonObject + " to person model.");
                            callback.onError();
                        }
                    } catch (JSONException e) {
                        callback.onError();
                    }
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, volleyError);
                if (callback != null) {
                    callback.onError();
                }
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("roles", newRole.toLowerCase());
        String path = String.format("sites/%s/users/%d", blogId, personID);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    public static void removePerson(String blogId, final long personID, final int localTableBlogId,
                                    final RemoveUserCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    // check if the call was successful
                    boolean success = jsonObject.optBoolean("success");
                    if (success) {
                        callback.onSuccess(personID, localTableBlogId);
                    } else {
                        callback.onError();
                    }
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, volleyError);
                if (callback != null) {
                    callback.onError();
                }
            }
        };

        String path = String.format("sites/%s/users/%d/delete", blogId, personID);
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    private static List<Person> peopleListFromJSON(JSONArray jsonArray, String blogId, int localTableBlogId)
            throws JSONException {
        if (jsonArray == null) {
            return null;
        }

        ArrayList<Person> peopleList = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            Person person = Person.fromJSON(jsonArray.optJSONObject(i), blogId, localTableBlogId);
            if (person != null) {
                peopleList.add(person);
            }
        }

        return peopleList;
    }

    public interface FetchUsersCallback extends Callback {
        void onSuccess(List<Person> peopleList);
    }

    public interface RemoveUserCallback extends Callback {
        void onSuccess(long personID, int localTableBlogId);
    }

    public interface UpdateUserCallback extends Callback {
        void onSuccess(Person person);
    }

    public interface Callback {
        void onError();
    }

    public static void validateUsername(final String username, String dotComBlogId, final ValidateUsernameCallback
            callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    JSONObject error = jsonObject.optJSONObject("errors");

                    if (error != null) {
                        JSONObject userError = error.optJSONObject(username);

                        if (userError == null) {
                            callback.onError();
                            return;
                        }

                        switch (userError.optString("code")) {
                            case "invalid_input":
                                switch (userError.optString("message")) {
                                    case "User not found":
                                        callback.onUsernameNotFound(username);
                                        return;
                                    case "Invalid email":
                                        callback.onInvalidEmail(username);
                                        return;
                                }
                                break;
                            case "invalid_input_has_role":
                                callback.onUsernameAlreadyMember(username);
                                return;
                        }

                        callback.onError();
                        return;
                    }

                    JSONArray succeededUsernames = jsonObject.optJSONArray("success");
                    if (succeededUsernames == null) {
                        callback.onError();
                        return;
                    }

                    for (int i = 0; i < succeededUsernames.length(); i++) {
                        if (username.equals(succeededUsernames.optString(i))) {
                            callback.onUsernameFound(username);
                            return;
                        }
                    }

                    callback.onError();
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.API, volleyError);
                if (callback != null) {
                    callback.onError();
                }
            }
        };

        String path = String.format("sites/%s/invites/validate", dotComBlogId);
        Map<String, String> params = new HashMap<>();
        params.put("invitees", username);
        params.put("role", "follower"); // the specific role is not important, just needs to be a valid one
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    public interface ValidateUsernameCallback {
        void onUsernameNotFound(String username);
        void onUsernameFound(String username);
        void onUsernameAlreadyMember(String username);
        void onInvalidEmail(String username);
        void onError();
    }
}
