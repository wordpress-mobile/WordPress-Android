package org.wordpress.android.ui.plans.models;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Plan {
    private long mProductID;
    private String mProductName;
    private String mProductNameEnglish;
    private final Hashtable<String, Integer> mPrices = new Hashtable<>();
    private String mProductNameShort;
    private String mProductSlug;
    private String mTagline;
    private String mShortdesc;
    private String mDescription;
    private long mCost;
    private int mBillPeriod;
    private String mProductType;
    private boolean isAvailable;
    private String mBillPeriodLabel;
    private String mPrice;
    private String mFormattedPrice;
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
    private List<Integer> mBundleProductIds;
    private List<String> mFeatures;

    public Plan(JSONObject planJSONObject) throws JSONException {
        mProductID = planJSONObject.getLong("product_id");
        mProductName = planJSONObject.getString("product_name");
        mProductNameEnglish = planJSONObject.getString("product_name_en");

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
        mShortdesc = planJSONObject.getString("shortdesc");
        mDescription = planJSONObject.getString("description");
        mCost = planJSONObject.getLong("cost");
        mBillPeriod = planJSONObject.getInt("bill_period");
        mProductType = planJSONObject.getString("product_type");
        isAvailable = JSONUtils.isStringTrue(planJSONObject, "available");
        mBillPeriodLabel = planJSONObject.getString("bill_period_label");
        mPrice = planJSONObject.getString("price");
        mFormattedPrice = planJSONObject.getString("formatted_price");
        mRawPrice = planJSONObject.getInt("raw_price");

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

        if (planJSONObject.has("feature_1")) {
            // At least one feature is available
            mFeatures = new ArrayList<>();
            for (int i=1; i < 50; i ++) {
                if (planJSONObject.has("feature_" + i)) {
                    mFeatures.add(planJSONObject.getString("feature_" + i));
                }
            }
        }
    }


    public long getProductID() {
        return mProductID;
    }

    public String getProductName() {
        return StringUtils.notNullStr(mProductName);
    }

    public String getProductNameEnglish() {
        return StringUtils.notNullStr(mProductNameEnglish);
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

    public String getShortdesc() {
        return StringUtils.notNullStr(mShortdesc);
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
        return isAvailable;
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

    public List<String> getFeatures() {
        return mFeatures;
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
}
