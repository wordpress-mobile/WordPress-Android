package org.wordpress.android.ui.plans.models;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.JSONUtils;

/**
 * This class represent an available Plan for a site.
 */
public class SitePlan {
    /*
      "1003": {
    "raw_price": 99,
    "formatted_price": "$99",
    "raw_discount": 0,
    "formatted_discount": "$0",
    "product_slug": "value_bundle",
    "product_name": "WordPress.com Premium",
    "can_start_trial": true
    "expiry": "2100-08-01",
    "free_trial": false,
    "user_facing_expiry": "2100-07-29",
    "subscribed_date": "2014-11-19 13:54:00"
  },
     */

    protected long mBlogLocalTableID;
    protected long mProductID;
    protected int mRawPrice;
    protected String mFormattedPrice;
    protected int mRawDiscount;
    protected String mFormattedDiscount;
    protected String mProductName;
    protected String mProductSlug;
    protected boolean mIsCurrentPlan;
    protected boolean mCanStartTrial;
    protected String mExpiry;
    protected boolean mFreeTrial;
    protected String mUserFacingExpiry;
    protected String mSubscribedDate;

    public SitePlan(long productID, JSONObject planJSONObject, Blog blog) throws JSONException {
        mBlogLocalTableID = blog.getLocalTableBlogId();
        mProductID = productID;
        mProductName = planJSONObject.getString("product_name");
        mProductSlug = planJSONObject.getString("product_slug");
        mRawPrice = planJSONObject.getInt("raw_price");
        mRawDiscount = planJSONObject.getInt("raw_discount");
        mFormattedPrice = planJSONObject.getString("formatted_price");
        mFormattedDiscount = planJSONObject.getString("formatted_discount");
        mCanStartTrial = JSONUtils.getBool(planJSONObject, "can_start_trial");
        mIsCurrentPlan = JSONUtils.getBool(planJSONObject, "current_plan");
        mExpiry = planJSONObject.optString("expiry");
        mUserFacingExpiry = planJSONObject.optString("user_facing_expiry");
        mSubscribedDate = planJSONObject.optString("subscribed_date");
        mFreeTrial = JSONUtils.getBool(planJSONObject, "free_trial");
    }

    public long getProductID() {
        return mProductID;
    }

    public int getRawPrice() {
        return mRawPrice;
    }

    public String getFormattedPrice() {
        return mFormattedPrice;
    }

    public int getRawDiscount() {
        return mRawDiscount;
    }

    public String getFormattedDiscount() {
        return mFormattedDiscount;
    }

    public String getProductName() {
        return mProductName;
    }

    public String getProductSlug() {
        return mProductSlug;
    }

    public boolean isCurrentPlan() {
        return mIsCurrentPlan;
    }

    public boolean canStartTrial() {
        return mCanStartTrial;
    }

    public long getBlogLocalTableID() {
        return mBlogLocalTableID;
    }

    @Nullable
    public String getSubscribedDate() {
        return mSubscribedDate;
    }

    @Nullable
    public String getUserFacingExpiry() {
        return mUserFacingExpiry;
    }

    public boolean isFreeTrial() {
        return mFreeTrial;
    }

    @Nullable
    public String getExpiry() {
        return mExpiry;
    }
}