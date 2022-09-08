package org.wordpress.android.ui.people.utils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.people.utils.PeopleUtils.ValidateUsernameCallback.ValidationResult;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PeopleUtils {
    // We limit followers we display to 1000 to avoid API performance issues
    public static final int FOLLOWER_PAGE_LIMIT = 50;
    public static final int FETCH_LIMIT = 20;
    public static final int AUTHOR_FETCH_LIMIT = 100;

    public static void fetchUsers(final SiteModel site, final int offset, final FetchUsersCallback callback) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("users");
                        List<Person> people = peopleListFromJSON(jsonArray, site.getId(), Person.PersonType.USER);
                        int numberOfUsers = jsonObject.optInt("found");
                        boolean isEndOfList = (people.size() + offset) >= numberOfUsers;
                        callback.onSuccess(people, isEndOfList);
                    } catch (JSONException e) {
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
        String path = String.format(Locale.US, "sites/%d/users", site.getSiteId());
        WordPress.getRestClientUtilsV1_1().get(path, params, null, listener, errorListener);
    }

    public static void fetchAuthors(final SiteModel site, final int offset, final FetchUsersCallback callback) {
        RestRequest.Listener listener = jsonObject -> {
            if (jsonObject != null && callback != null) {
                try {
                    JSONArray jsonArray = jsonObject.getJSONArray("users");
                    List<Person> people = peopleListFromJSON(jsonArray, site.getId(), Person.PersonType.USER);
                    int numberOfUsers = jsonObject.optInt("found");
                    boolean isEndOfList = (people.size() + offset) >= numberOfUsers;
                    callback.onSuccess(people, isEndOfList);
                } catch (JSONException e) {
                    AppLog.e(T.API, "JSON exception occurred while parsing the response for sites/%s/users: " + e);
                    callback.onError();
                }
            }
        };

        RestRequest.ErrorListener errorListener = volleyError -> {
            AppLog.e(T.API, volleyError);
            if (callback != null) {
                callback.onError();
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("number", Integer.toString(PeopleUtils.AUTHOR_FETCH_LIMIT));
        params.put("offset", Integer.toString(offset));
        params.put("order_by", "display_name");
        params.put("order", "ASC");
        params.put("authors_only", "true");
        String path = String.format(Locale.US, "sites/%d/users", site.getSiteId());
        WordPress.getRestClientUtilsV1_1().get(path, params, null, listener, errorListener);
    }

    public static void fetchRevisionAuthorsDetails(final SiteModel site, List<String> authors,
                                                   final FetchUsersCallback callback) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        List<Person> people = new ArrayList<>();

                        Iterator<String> keys = jsonObject.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            if (jsonObject.get(key) instanceof JSONObject) {
                                JSONArray jsonArray = ((JSONObject) jsonObject.get(key)).getJSONArray("users");
                                people.addAll(peopleListFromJSON(jsonArray, site.getId(), Person.PersonType.USER));
                            }
                        }

                        callback.onSuccess(people, true);
                    } catch (JSONException e) {
                        AppLog.e(T.API, "JSON exception occurred while parsing the revision author details"
                                        + " from batch response for sites/%s/users: " + e);
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

        Map<String, String> batchParams = new HashMap<>();

        for (int i = 0; i < authors.size(); i++) {
            batchParams.put(String.format(Locale.US, "urls[%d]", i),
                    String.format(Locale.US, "/sites/%d/users?search=%s&search_columns=ID",
                            site.getSiteId(), authors.get(i)));
        }

        WordPress.getRestClientUtilsV1_1().get("batch/", batchParams, null, listener, errorListener);
    }

    public static void fetchFollowers(final SiteModel site, final int page, final FetchFollowersCallback callback) {
        fetchFollowers(site, page, callback, false);
    }

    public static void fetchEmailFollowers(final SiteModel site, final int page,
                                           final FetchFollowersCallback callback) {
        fetchFollowers(site, page, callback, true);
    }

    private static void fetchFollowers(final SiteModel site, final int page, final FetchFollowersCallback callback,
                                       final boolean isEmailFollower) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("subscribers");
                        Person.PersonType personType = isEmailFollower
                                ? Person.PersonType.EMAIL_FOLLOWER : Person.PersonType.FOLLOWER;
                        List<Person> people = peopleListFromJSON(jsonArray, site.getId(), personType);
                        int pageFetched = jsonObject.optInt("page");
                        int numberOfPages = jsonObject.optInt("pages");
                        boolean isEndOfList = page >= numberOfPages || page >= FOLLOWER_PAGE_LIMIT;
                        callback.onSuccess(people, pageFetched, isEndOfList);
                    } catch (JSONException e) {
                        AppLog.e(T.API, "JSON exception occurred while parsing the response for "
                                        + "sites/%s/stats/followers: " + e);
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
        String path = String.format(Locale.US, "sites/%d/stats/followers", site.getSiteId());
        WordPress.getRestClientUtilsV1_1().get(path, params, null, listener, errorListener);
    }

    public static void fetchViewers(final SiteModel site, final int offset, final FetchViewersCallback callback) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("viewers");
                        List<Person> people = peopleListFromJSON(jsonArray, site.getId(), Person.PersonType.VIEWER);
                        int numberOfUsers = jsonObject.optInt("found");
                        boolean isEndOfList = (people.size() + offset) >= numberOfUsers;
                        callback.onSuccess(people, isEndOfList);
                    } catch (JSONException e) {
                        AppLog.e(T.API, "JSON exception occurred while parsing the response for "
                                        + "sites/%s/viewers: " + e);
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

        int page = (offset / FETCH_LIMIT) + 1;
        Map<String, String> params = new HashMap<>();
        params.put("number", Integer.toString(FETCH_LIMIT));
        params.put("page", Integer.toString(page));
        String path = String.format(Locale.US, "sites/%d/viewers", site.getSiteId());
        WordPress.getRestClientUtilsV1_1().get(path, params, null, listener, errorListener);
    }

    public static void updateRole(final SiteModel site, long personID, String newRole, final int localTableBlogId,
                                  final UpdateUserCallback callback) {
        RestRequest.Listener listener = new RestRequest.Listener() {
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
        params.put("roles", newRole);
        String path = String.format(Locale.US, "sites/%d/users/%d", site.getSiteId(), personID);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    public static void removeUser(final SiteModel site, final long personID, final RemovePersonCallback callback) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    // check if the call was successful
                    boolean success = jsonObject.optBoolean("success");
                    if (success) {
                        callback.onSuccess(personID, site.getId());
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

        String path = String.format(Locale.US, "sites/%d/users/%d/delete", site.getSiteId(), personID);
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    public static void removeFollower(final SiteModel site, final long personID,
                                      Person.PersonType personType, final RemovePersonCallback callback) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    // check if the call was successful
                    boolean success = jsonObject.optBoolean("deleted");
                    if (success) {
                        callback.onSuccess(personID, site.getId());
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
            path = String.format(Locale.US, "sites/%d/email-followers/%d/delete", site.getSiteId(), personID);
        } else {
            path = String.format(Locale.US, "sites/%d/followers/%d/delete", site.getSiteId(), personID);
        }
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    public static void removeViewer(final SiteModel site, final long personID, final RemovePersonCallback callback) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    // check if the call was successful
                    boolean success = jsonObject.optBoolean("deleted");
                    if (success) {
                        callback.onSuccess(personID, site.getId());
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

        String path = String.format(Locale.US, "sites/%d/viewers/%d/delete", site.getSiteId(), personID);
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    private static List<Person> peopleListFromJSON(JSONArray jsonArray, int localTableBlogId,
                                                   Person.PersonType personType) throws JSONException {
        if (jsonArray == null) {
            return null;
        }

        ArrayList<Person> peopleList = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            Person person;
            if (personType == Person.PersonType.USER) {
                person = Person.userFromJSON(jsonArray.optJSONObject(i), localTableBlogId);
            } else if (personType == Person.PersonType.VIEWER) {
                person = Person.viewerFromJSON(jsonArray.optJSONObject(i), localTableBlogId);
            } else {
                boolean isEmailFollower = (personType == Person.PersonType.EMAIL_FOLLOWER);
                person = Person.followerFromJSON(jsonArray.optJSONObject(i), localTableBlogId, isEmailFollower);
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

    public interface FetchViewersCallback extends Callback {
        void onSuccess(List<Person> peopleList, boolean isEndOfList);
    }

    public interface RemovePersonCallback extends Callback {
        void onSuccess(long personID, int localTableBlogId);
    }

    public interface UpdateUserCallback extends Callback {
        void onSuccess(Person person);
    }

    public interface Callback {
        void onError();
    }

    public static void validateUsernames(final List<String> usernames, String role, long wpComBlogId, final
    ValidateUsernameCallback callback) {
        RestRequest.Listener listener = new RestRequest.Listener() {
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
                                        case "Invalid email":
                                            callback.onUsernameValidation(username, ValidationResult.INVALID_EMAIL);
                                            continue;
                                        case "User not found":
                                            // fall through to the default case
                                        default:
                                            callback.onUsernameValidation(username, ValidationResult.USER_NOT_FOUND);
                                            continue;
                                    }
                                case "invalid_input_has_role":
                                    callback.onUsernameValidation(username, ValidationResult.ALREADY_MEMBER);
                                    continue;
                                case "invalid_input_following":
                                    callback.onUsernameValidation(username, ValidationResult.ALREADY_FOLLOWING);
                                    continue;
                                case "invalid_user_blocked_invites":
                                    callback.onUsernameValidation(username, ValidationResult.BLOCKED_INVITES);
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

        String path = String.format(Locale.US, "sites/%d/invites/validate", wpComBlogId);
        Map<String, String> params = new HashMap<>();
        for (String username : usernames) {
            params.put("invitees[" + username + "]", username); // specify an array key so to make the map key unique
        }
        params.put("role", role);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    public interface ValidateUsernameCallback {
        enum ValidationResult {
            USER_NOT_FOUND,
            ALREADY_MEMBER,
            ALREADY_FOLLOWING,
            BLOCKED_INVITES,
            INVALID_EMAIL,
            USER_FOUND
        }

        void onUsernameValidation(String username, ValidationResult validationResult);

        void onValidationFinished();

        void onError();
    }

    public static void sendInvitations(final List<String> usernames, String role, String message, long wpComBlogId,
                                       final InvitationsSendCallback callback) {
        RestRequest.Listener listener = new RestRequest.Listener() {
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

        String path = String.format(Locale.US, "sites/%s/invites/new", wpComBlogId);
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
