package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.stats.models.FollowDataModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

import java.lang.ref.WeakReference;
import java.util.Locale;

class FollowHelper {

    private final WeakReference<Activity> mActivityRef;

    public FollowHelper(Activity activity) {
        mActivityRef = new WeakReference<>(activity);
    }


    public void showPopup(View anchor, final FollowDataModel followData) {
        if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
            return;
        }

        final String workingText = followData.getFollowingText();
        final String followText = followData.getFollowText();
        final String unfollowText =  followData.getFollowingHoverText();

        final PopupMenu popup = new PopupMenu(mActivityRef.get(), anchor);
        final MenuItem menuItem;

        if (followData.restCallInProgressEh) {
            menuItem = popup.getMenu().add(workingText);
        } else {
            menuItem = popup.getMenu().add(followData.followingEh() ? unfollowText : followText);
        }

        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                item.setTitle(workingText);
                item.setOnMenuItemClickListener(null);

                final RestClientUtils restClientUtils = WordPress.getRestClientUtils();
                final String restPath;
                if (!followData.followingEh()) {
                    restPath = String.format(Locale.US, "/sites/%s/follows/new?source=android", followData.getSiteID());
                } else {
                    restPath = String.format(Locale.US, "/sites/%s/follows/mine/delete", followData.getSiteID());
                }

                followData.restCallInProgressEh = true;
                FollowRestListener vListener = new FollowRestListener(mActivityRef.get(), followData);
                restClientUtils.post(restPath, vListener, vListener);
                AppLog.d(AppLog.T.STATS, "Enqueuing the following REST request " + restPath);
                return true;
            }
        });

        popup.show();

    }


    private class FollowRestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        private final WeakReference<Activity> mActivityRef;
        private final FollowDataModel mFollowData;

        public FollowRestListener(Activity activity, final FollowDataModel followData) {
            this.mActivityRef = new WeakReference<>(activity);
            this.mFollowData = followData;
        }

        @Override
        public void onResponse(final JSONObject response) {
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
                return;
            }

            mFollowData.restCallInProgressEh = false;
            if (response!= null) {
                try {
                    boolean followingEh = response.getBoolean("is_following");
                    mFollowData.setIsFollowing(followingEh);
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, e.getMessage());
                }
            }
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (volleyError != null) {
                AppLog.e(AppLog.T.STATS, "Error while following a blog "
                        + volleyError.getMessage(), volleyError);
            }
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
                return;
            }

            mFollowData.restCallInProgressEh = false;
            ToastUtils.showToast(mActivityRef.get(),
                    mActivityRef.get().getString(R.string.reader_toast_err_follow_blog),
                    ToastUtils.Duration.LONG);
        }
    }
}
