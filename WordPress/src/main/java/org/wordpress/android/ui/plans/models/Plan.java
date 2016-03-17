package org.wordpress.android.ui.plans.models;


import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Plan implements Serializable {
    private long mProductID;
    private String mProductName;
    private final Hashtable<String, Integer> mPrices = new Hashtable<>();
    private String mProductNameShort;
    private String mProductSlug;
    private String mTagline;
    private String mDescription;
    private long mCost;
    private int mBillPeriod;
    private String mProductType;
    private boolean mIsAvailable;
    private String mBillPeriodLabel;
    private String mPrice;
    private String mFormattedPrice;
    private String mIconUrl;
    private int mRawPrice;

    // Optionals
    private int mWidth;
    private int mHeight;
    private long mSaving; // diff from cost and original
    private long mOriginal; // Original price
    private String mFormattedOriginalPrice;
    private int mStore;
    private int mMulti;
    private String mSupportDocument;
    private String mCapability;
    private ArrayList<Integer> mBundleProductIds;
    private final ArrayList<PlanFeaturesHighlightSection> mFeaturesHighlightSections =  new ArrayList<>();

    // used to link with an actual product on the Store (ie: Used to load the price from the Store).
    private String mAndroidSKU;

    // Info attached to the current site/user
    private int mRawDiscount;
    private String mFormattedDiscount;
    private boolean mIsCurrentPlan;
    private boolean mCanStartTrial;
    private String mExpiry;
    private boolean mFreeTrial;
    private String mUserFacingExpiry;
    private String mSubscribedDate;
    private String mBundleSubscriptionID;


    public Plan(JSONObject planJSONObject) throws JSONException {
        mProductID = planJSONObject.getLong("product_id");
        mProductName = planJSONObject.getString("product_name");

        // Unfold prices object
        JSONObject priceJSONObject = planJSONObject.getJSONObject("prices");
        JSONArray priceKeys = priceJSONObject.names();
        if (priceKeys != null) {
            for (int i=0; i < priceKeys.length(); i ++) {
                String currentKey = priceKeys.getString(i);
                int currentPrice = priceJSONObject.getInt(currentKey);
                mPrices.put(currentKey, currentPrice);
            }
        }

        mProductNameShort = planJSONObject.getString("product_name_short");
        mProductSlug = planJSONObject.getString("product_slug");
        mTagline = planJSONObject.getString("tagline");
        mDescription = planJSONObject.getString("description");
        mCost = planJSONObject.getLong("cost");
        mBillPeriod = planJSONObject.getInt("bill_period");
        mProductType = planJSONObject.getString("product_type");
        mIsAvailable = JSONUtils.getBool(planJSONObject, "available");
        mBillPeriodLabel = planJSONObject.getString("bill_period_label");
        mPrice = planJSONObject.getString("price");
        mFormattedPrice = planJSONObject.getString("formatted_price");
        mRawPrice = planJSONObject.getInt("raw_price");
        mIconUrl = planJSONObject.optString("icon");
        mAndroidSKU = planJSONObject.optString("android_sku");

        // Optionals
        mWidth = planJSONObject.optInt("width");
        mHeight = planJSONObject.optInt("height");
        mSaving = planJSONObject.optLong("saving", 0L);
        mOriginal = planJSONObject.optLong("original", mCost);
        mFormattedOriginalPrice = planJSONObject.optString("formatted_original_price");
        mSupportDocument = planJSONObject.optString("support_document");
        mCapability = planJSONObject.optString("capability");
        mStore = planJSONObject.optInt("store");
        mMulti = planJSONObject.optInt("multi");

        if (planJSONObject.has("bundle_product_ids")) {
            JSONArray bundleIDS = planJSONObject.getJSONArray("bundle_product_ids");
            mBundleProductIds = new ArrayList<>(bundleIDS.length());
            for (int i=0; i < bundleIDS.length(); i ++) {
                int currentBundleID = bundleIDS.getInt(i);
                mBundleProductIds.add(currentBundleID);
            }
        }

        if (planJSONObject.has("features_highlight")) {
            JSONArray featuresHighlightSections = planJSONObject.getJSONArray("features_highlight");
            for (int i=0; i < featuresHighlightSections.length(); i++) {
                mFeaturesHighlightSections.add(
                        new PlanFeaturesHighlightSection(featuresHighlightSections.getJSONObject(i))
                );
            }
        }

        // Specific info liked with the current site
        mRawDiscount = planJSONObject.optInt("raw_discount", 0);
        mFormattedDiscount = planJSONObject.optString("formatted_discount");
        mCanStartTrial = JSONUtils.getBool(planJSONObject, "can_start_trial");
        mIsCurrentPlan = JSONUtils.getBool(planJSONObject, "current_plan");
        mExpiry = planJSONObject.optString("expiry");
        mUserFacingExpiry = planJSONObject.optString("user_facing_expiry");
        mSubscribedDate = planJSONObject.optString("subscribed_date");
        mFreeTrial = JSONUtils.getBool(planJSONObject, "free_trial");
        mBundleSubscriptionID = planJSONObject.optString("bundle_subscription_id");
    }


    public long getProductID() {
        return mProductID;
    }

    public String getProductName() {
        return StringUtils.notNullStr(mProductName);
    }

    public Hashtable<String, Integer> getPrices() {
        return mPrices;
    }

    public String getProductNameShort() {
        return StringUtils.notNullStr(mProductNameShort);
    }

    public String getProductSlug() {
        return StringUtils.notNullStr(mProductSlug);
    }

    public String getTagline() {
        return StringUtils.notNullStr(mTagline);
    }

    public String getDescription() {
        return StringUtils.notNullStr(mDescription);
    }

    public long getCost() {
        return mCost;
    }

    public int getBillPeriod() {
        return mBillPeriod;
    }

    public String getProductType() {
        return StringUtils.notNullStr(mProductType);
    }

    public boolean isAvailable() {
        return mIsAvailable;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getBillPeriodLabel() {
        return StringUtils.notNullStr(mBillPeriodLabel);
    }

    public String getPrice() {
        return StringUtils.notNullStr(mPrice);
    }

    public String getFormattedPrice() {
        return StringUtils.notNullStr(mFormattedPrice);
    }

    public int getRawPrice() {
        return mRawPrice;
    }

    public ArrayList<PlanFeaturesHighlightSection> getFeaturesHighlightSections() {
        return mFeaturesHighlightSections;
    }

    public List<Integer> getBundleProductIds() {
        return mBundleProductIds;
    }

    public String getCapability() {
        return StringUtils.notNullStr(mCapability);
    }

    public String getSupportDocument() {
        return StringUtils.notNullStr(mSupportDocument);
    }

    public int getMulti() {
        return mMulti;
    }

    public int getStore() {
        return mStore;
    }

    public String getFormattedOriginalPrice() {
        return StringUtils.notNullStr(mFormattedOriginalPrice);
    }

    public long getSaving() {
        return mSaving;
    }

    public long getOriginal() {
        return mOriginal;
    }

    public String getIconUrl() {
        return StringUtils.notNullStr(mIconUrl);
    }
    public void setIconUrl(String iconUrl) {
        mIconUrl = StringUtils.notNullStr(iconUrl);
    }
    public boolean hasIconUrl() {
        return !TextUtils.isEmpty(mIconUrl);
    }

    public int getRawDiscount() {
        return mRawDiscount;
    }

    public String getFormattedDiscount() {
        return StringUtils.notNullStr(mFormattedDiscount);
    }


    public boolean isCurrentPlan() {
        return mIsCurrentPlan;
    }

    public boolean canStartTrial() {
        return mCanStartTrial;
    }

    public String getSubscribedDate() {
        return StringUtils.notNullStr(mSubscribedDate);
    }

    public String getUserFacingExpiry() {
        return StringUtils.notNullStr(mUserFacingExpiry);
    }

    public boolean isFreeTrial() {
        return mFreeTrial;
    }

    public String getExpiry() {
        return StringUtils.notNullStr(mExpiry);
    }

    public String getAndroidSKU() {
        return StringUtils.notNullStr(mAndroidSKU);
    }

    public String getBundleSubscriptionID() {
        return StringUtils.notNullStr(mBundleSubscriptionID);
    }
}
