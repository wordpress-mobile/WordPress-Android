package org.wordpress.android.ui.people.utils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.people.utils.PeopleUtils.ValidateUsernameCallback.ValidationResult;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PeopleUtils {
    // We limit followers we display to 1000 to avoid API performance issues
    public static int FOLLOWER_PAGE_LIMIT = 500;
    public static int FETCH_LIMIT = 20;

    public static void fetchUsers(final String blogId, final int localTableBlogId, final int offset,
                                  final FetchUsersCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("users");
                        List<Person> people = peopleListFromJSON(jsonArray, localTableBlogId, false, false);
                        int numberOfUsers = jsonObject.optInt("found");
                        boolean isEndOfList = (people.size() + offset) >= numberOfUsers;
                        callback.onSuccess(people, isEndOfList);
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

        Map<String, String> params = new HashMap<>();
        params.put("number", Integer.toString(PeopleUtils.FETCH_LIMIT));
        params.put("offset", Integer.toString(offset));
        params.put("order_by", "display_name");
        params.put("order", "ASC");
        String path = String.format("sites/%s/users", blogId);
        WordPress.getRestClientUtilsV1_1().get(path, params, null, listener, errorListener);
    }

    public static void fetchFollowers(final String blogId, final int localTableBlogId, final int page,
                                      final FetchFollowersCallback callback) {
        fetchFollowers(blogId, localTableBlogId, page, callback, false);
    }

    public static void fetchEmailFollowers(final String blogId, final int localTableBlogId, final int page,
                                           final FetchFollowersCallback callback) {
        fetchFollowers(blogId, localTableBlogId, page, callback, true);
    }

    private static void fetchFollowers(final String blogId, final int localTableBlogId, final int page,
                                       final FetchFollowersCallback callback, final boolean isEmailFollower) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("subscribers");
                        List<Person> people = peopleListFromJSON(jsonArray, localTableBlogId, !isEmailFollower,
                                isEmailFollower);
                        int pageFetched = jsonObject.optInt("page");
                        int numberOfPages = jsonObject.optInt("pages");
                        boolean isEndOfList = page >= numberOfPages || page >= FOLLOWER_PAGE_LIMIT;
                        callback.onSuccess(people, pageFetched, isEndOfList);
                    }
                    catch (JSONException e) {
                        AppLog.e(T.API, "JSON exception occurred while parsing the response for " +
                                "sites/%s/stats/followers: " + e);
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
        params.put("max", Integer.toString(FETCH_LIMIT));
        params.put("page", Integer.toString(page));
        params.put("type", isEmailFollower ? "email" : "wp_com");
        String path = String.format("sites/%s/stats/followers", blogId);
        WordPress.getRestClientUtilsV1_1().get(path, params, null, listener, errorListener);
    }

    public static void updateRole(final String blogId, long personID, String newRole, final int localTableBlogId,
                                  final UpdateUserCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        Person person = Person.userFromJSON(jsonObject, localTableBlogId);
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

    public static void removeUser(String blogId, final long personID, final int localTableBlogId,
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

    public static void removeFollower(String blogId, final long personID, final int localTableBlogId,
                                      Person.PersonType personType, final RemoveUserCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    // check if the call was successful
                    boolean success = jsonObject.optBoolean("deleted");
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

        String path;
        if (personType == Person.PersonType.EMAIL_FOLLOWER) {
            path = String.format("sites/%s/email-followers/%d/delete", blogId, personID);
        } else {
            path = String.format("sites/%s/followers/%d/delete", blogId, personID);
        }
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    private static List<Person> peopleListFromJSON(JSONArray jsonArray, int localTableBlogId, boolean isFollower,
                                                   boolean isEmailFollower) throws JSONException {
        if (jsonArray == null) {
            return null;
        }

        ArrayList<Person> peopleList = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            Person person;
            if (isFollower || isEmailFollower) {
                person = Person.followerFromJSON(jsonArray.optJSONObject(i), localTableBlogId, isEmailFollower);
            } else {
                person = Person.userFromJSON(jsonArray.optJSONObject(i), localTableBlogId);
            }
            if (person != null) {
                peopleList.add(person);
            }
        }

        return peopleList;
    }

    public interface FetchUsersCallback extends Callback {
        void onSuccess(List<Person> peopleList, boolean isEndOfList);
    }

    public interface FetchFollowersCallback extends Callback {
        void onSuccess(List<Person> peopleList, int pageFetched, boolean isEndOfList);
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

    public static void validateUsernames(final List<String> usernames, String dotComBlogId, final
            ValidateUsernameCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    JSONObject errors = jsonObject.optJSONObject("errors");

                    int errorredUsernameCount = 0;

                    if (errors != null) {
                        for (String username : usernames) {
                            JSONObject userError = errors.optJSONObject(username);

                            if (userError == null) {
                                continue;
                            }

                            errorredUsernameCount++;

                            switch (userError.optString("code")) {
                                case "invalid_input":
                                    switch (userError.optString("message")) {
                                        case "User not found":
                                            callback.onUsernameValidation(username, ValidationResult.USER_NOT_FOUND);
                                            continue;
                                        case "Invalid email":
                                            callback.onUsernameValidation(username, ValidationResult.INVALID_EMAIL);
                                            continue;
                                    }
                                    break;
                                case "invalid_input_has_role":
                                    callback.onUsernameValidation(username, ValidationResult.ALREADY_MEMBER);
                                    continue;
                            }

                            callback.onError();
                            callback.onValidationFinished();
                            return;
                        }
                    }

                    JSONArray succeededUsernames = jsonObject.optJSONArray("success");
                    if (succeededUsernames == null) {
                        callback.onError();
                        callback.onValidationFinished();
                        return;
                    }

                    int succeededUsernameCount = 0;

                    for (int i = 0; i < succeededUsernames.length(); i++) {
                        String username = succeededUsernames.optString(i);
                        if (usernames.contains(username)) {
                            succeededUsernameCount++;
                            callback.onUsernameValidation(username, ValidationResult.USER_FOUND);
                        }
                    }

                    if (errorredUsernameCount + succeededUsernameCount != usernames.size()) {
                        callback.onError();
                        callback.onValidationFinished();
                    }

                    callback.onValidationFinished();
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
        for (String username : usernames) {
            params.put("invitees[" + username + "]", username); // specify an array key so to make the map key unique
        }
        params.put("role", "follower"); // the specific role is not important, just needs to be a valid one
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    public interface ValidateUsernameCallback {
        enum ValidationResult {
            USER_NOT_FOUND,
            ALREADY_MEMBER,
            INVALID_EMAIL,
            USER_FOUND
        }

        void onUsernameValidation(String username, ValidationResult validationResult);
        void onValidationFinished();
        void onError();
    }

    public static void sendInvitations(final List<String> usernames, String role, String message, String dotComBlogId,
                                       final InvitationsSendCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (callback == null) {
                    return;
                }

                if (jsonObject == null) {
                    callback.onError();
                    return;
                }

                Map<String, String> failedUsernames = new LinkedHashMap<>();

                JSONObject errors = jsonObject.optJSONObject("errors");
                if (errors != null) {
                    for (String username : usernames) {
                        JSONObject userError = errors.optJSONObject(username);

                        if (userError != null) {
                            failedUsernames.put(username, userError.optString("message"));
                        }
                    }
                }

                List<String> succeededUsernames = new ArrayList<>();
                JSONArray succeededUsernamesJson = jsonObject.optJSONArray("sent");
                if (succeededUsernamesJson == null) {
                    callback.onError();
                    return;
                }

                for (int i = 0; i < succeededUsernamesJson.length(); i++) {
                    String username = succeededUsernamesJson.optString(i);
                    if (usernames.contains(username)) {
                        succeededUsernames.add(username);
                    }
                }

                if (failedUsernames.size() + succeededUsernames.size() != usernames.size()) {
                    callback.onError();
                }

                callback.onSent(succeededUsernames, failedUsernames);
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

        String path = String.format("sites/%s/invites/new", dotComBlogId);
        Map<String, String> params = new HashMap<>();
        for (String username : usernames) {
            params.put("invitees[" + username + "]", username); // specify an array key so to make the map key unique
        }
        params.put("role", role);
        params.put("message", message);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    public interface InvitationsSendCallback {
        void onSent(List<String> succeededUsernames, Map<String, String> failedUsernameErrors);
        void onError();
    }
}
