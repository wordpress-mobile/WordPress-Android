package org.wordpress.android.ui.plans.models;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;

public class Plan {
/*
        "product_id": 2002,
        "product_name": "Free",
        "product_name_en": "Free",
        "prices": {
            "USD": 0,
            "GBP": 0
        },
        "product_name_short": "Free",
        "product_slug": "jetpack_free",
        "tagline": "Get started",
        "shortdesc": "Jetpack (free) speeds up your site's images, secures it, and enables traffic and customization tools.",
        "description": "Spam Protection",
        "cost": 0,
        "bill_period": -1,
        "product_type": "jetpack",
        "available": "yes",
        "width": 500,
        "height": 435,
        "bill_period_label": "for life",
        "price": "\u00a30",
        "formatted_price": "\u00a30",
        "raw_price": 0
 */

    private long mProductID;
    private String mProductName;
    private String mProductNameEnglish;
    private Hashtable<String, Integer> mPrices = new Hashtable();
    private String mProductNameShort;
    private String mProductSlug;
    private String mTagline;
    private String mShortdesc;
    private String mDescription;
    private long mCost;
    private int mBillPeriod;
    private String mProductType;
    private boolean isAvailable;
    private int mWidth;
    private int mHeight;
    private String mBillPeriodLabel;
    private String mPrice;
    private String mFormattedPrice;
    private int mRawPrice;

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
        String rawAvailable = planJSONObject.optString("available");
        isAvailable = "yes".equals(rawAvailable) || "1".equals(rawAvailable) || "true".equals(rawAvailable);
        mWidth = planJSONObject.getInt("width");
        mHeight = planJSONObject.getInt("height");
        mBillPeriodLabel = planJSONObject.getString("bill_period_label");
        mPrice = planJSONObject.getString("price");
        mFormattedPrice = planJSONObject.getString("formatted_price");
        mRawPrice = planJSONObject.getInt("raw_price");
    }


    public long getProductID() {
        return mProductID;
    }

    public String getProductName() {
        return mProductName;
    }

    public String getProductNameEnglish() {
        return mProductNameEnglish;
    }

    public Hashtable<String, Integer> getPrices() {
        return mPrices;
    }

    public String getProductNameShort() {
        return mProductNameShort;
    }

    public String getProductSlug() {
        return mProductSlug;
    }

    public String getTagline() {
        return mTagline;
    }

    public String getShortdesc() {
        return mShortdesc;
    }

    public String getDescription() {
        return mDescription;
    }

    public long getCost() {
        return mCost;
    }

    public int getBillPeriod() {
        return mBillPeriod;
    }

    public String getProductType() {
        return mProductType;
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
        return mBillPeriodLabel;
    }

    public String getPrice() {
        return mPrice;
    }

    public String getFormattedPrice() {
        return mFormattedPrice;
    }

    public int getRawPrice() {
        return mRawPrice;
    }
    
}
