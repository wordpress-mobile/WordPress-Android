package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.ui.stats.models.ReferrerGroupModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;

import java.lang.ref.WeakReference;

class ReferrerSpamHelper {

    private final WeakReference<Activity> mActivityRef;

    public ReferrerSpamHelper(Activity activity) {
        mActivityRef = new WeakReference<>(activity);
    }

    // return the domain of the passed ReferrerGroupModel or null.
    private static String getDomain(ReferrerGroupModel group) {
        // Use the URL value given in the JSON response, or use the groupID that doesn't contain the schema.
        final String spamDomain = group.getUrl() != null ? group.getUrl() : "http://" + group.getGroupId();
        return UrlUtils.isValidUrlAndHostNotNull(spamDomain) ? UrlUtils.getHost(spamDomain) : null;
    }

    public static boolean isSpamActionAvailable(ReferrerGroupModel group) {
        String domain = getDomain(group);
        return !TextUtils.isEmpty(domain) && !domain.equals("wordpress.com");
    }

    public void showPopup(View anchor, final ReferrerGroupModel referrerGroup) {
        if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
            return;
        }

        final PopupMenu popup = new PopupMenu(mActivityRef.get(), anchor);
        final MenuItem menuItem;

        if (referrerGroup.isRestCallInProgress) {
            menuItem = popup.getMenu().add(
                    referrerGroup.isMarkedAsSpam ?
                            mActivityRef.get().getString(R.string.stats_referrers_marking_not_spam) :
                            mActivityRef.get().getString(R.string.stats_referrers_marking_spam)
            );
        } else {
            menuItem = popup.getMenu().add(
                    referrerGroup.isMarkedAsSpam ?
                            mActivityRef.get().getString(R.string.stats_referrers_unspam) :
                            mActivityRef.get().getString(R.string.stats_referrers_spam)
            );
        }

        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                item.setTitle(
                        referrerGroup.isMarkedAsSpam ?
                                mActivityRef.get().getString(R.string.stats_referrers_marking_not_spam) :
                                mActivityRef.get().getString(R.string.stats_referrers_marking_spam)
                );
                item.setOnMenuItemClickListener(null);

                final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();
                final String restPath;
                final boolean isMarkingAsSpamInProgress;
                if (referrerGroup.isMarkedAsSpam) {
                    restPath = String.format("/sites/%s/stats/referrers/spam/delete/?domain=%s", referrerGroup.getBlogId(), getDomain(referrerGroup));
                    isMarkingAsSpamInProgress = false;
                } else {
                    restPath = String.format("/sites/%s/stats/referrers/spam/new/?domain=%s", referrerGroup.getBlogId(), getDomain(referrerGroup));
                    isMarkingAsSpamInProgress = true;
                }

                referrerGroup.isRestCallInProgress = true;
                ReferrerSpamRestListener vListener = new ReferrerSpamRestListener(mActivityRef.get(), referrerGroup, isMarkingAsSpamInProgress);
                restClientUtils.post(restPath, vListener, vListener);
                AppLog.d(AppLog.T.STATS, "Enqueuing the following REST request " + restPath);
                return true;
            }
        });

        popup.show();
    }


    private class ReferrerSpamRestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        private final WeakReference<Activity> mActivityRef;
        private final ReferrerGroupModel mReferrerGroup;
        private final boolean isMarkingAsSpamInProgress;

        public ReferrerSpamRestListener(Activity activity, final ReferrerGroupModel referrerGroup, final boolean isMarkingAsSpamInProgress) {
            this.mActivityRef = new WeakReference<>(activity);
            this.mReferrerGroup = referrerGroup;
            this.isMarkingAsSpamInProgress = isMarkingAsSpamInProgress;
        }

        @Override
        public void onResponse(final JSONObject response) {
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
                return;
            }

            mReferrerGroup.isRestCallInProgress = false;
            if (response!= null) {
                boolean success = response.optBoolean("success");
                if (success) {
                    mReferrerGroup.isMarkedAsSpam = isMarkingAsSpamInProgress;
                    int localBlogID = StatsUtils.getLocalBlogIdFromRemoteBlogId(
                            Integer.parseInt(mReferrerGroup.getBlogId())
                    );
                    StatsTable.deleteStatsForBlog(mActivityRef.get(), localBlogID, StatsService.StatsEndpointsEnum.REFERRERS);
                } else {
                    // It's not a success. Something went wrong on the server
                    String errorMessage = null;
                    if (response.has("error")) {
                        errorMessage = response.optString("message");
                    }

                    if (TextUtils.isEmpty(errorMessage)) {
                        errorMessage = mActivityRef.get().getString(R.string.stats_referrers_spam_generic_error);
                    }

                    ToastUtils.showToast(mActivityRef.get(), errorMessage, ToastUtils.Duration.LONG);
                }
            }
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (volleyError != null) {
                AppLog.e(AppLog.T.STATS, "Error while marking the referrer " + getDomain(mReferrerGroup) + " as "
                        + (isMarkingAsSpamInProgress ? " spam " : " unspam ") +
                        volleyError.getMessage(), volleyError);
            }
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
                return;
            }

            mReferrerGroup.isRestCallInProgress = false;
            ToastUtils.showToast(mActivityRef.get(),
                    mActivityRef.get().getString(R.string.stats_referrers_spam_generic_error),
                    ToastUtils.Duration.LONG);
        }
    }
}
