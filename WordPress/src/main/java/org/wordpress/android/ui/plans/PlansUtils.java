package org.wordpress.android.ui.plans;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.PhotonUtils;

import java.util.Currency;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;

public class PlansUtils {

    private static String deviceCurrencyCode; // ISO 4217 currency code.
    private static String deviceCurrencySymbol;

    private static final String DOLLAR_SYMBOL = "$";
    private static final String DOLLAR_ISO4217_CODE = "USD";

    /**
     * Returns the price for the passed plan formatted for the user's locale, defaults
     * to USD if user's locale isn't matched
     *
     * TODO: endpoint will be updated to include the formatted price, so this method is temporary
     */
    public static String getPlanDisplayPrice(@NonNull Plan plan) {
        // lookup currency code/symbol on first use
        if (deviceCurrencyCode == null) {
            Currency currency = Currency.getInstance(Locale.getDefault());
            deviceCurrencyCode = currency.getCurrencyCode();
            deviceCurrencySymbol = currency.getSymbol(Locale.getDefault());
        }

        String currencySymbol;
        int priceValue;
        Hashtable<String, Integer> pricesMap = plan.getPrices();
        if (pricesMap.containsKey(deviceCurrencyCode)) {
            currencySymbol = deviceCurrencySymbol;
            priceValue = pricesMap.get(deviceCurrencyCode);
            return currencySymbol + FormatUtils.formatInt(priceValue);
        } else {
            // locale not found, default to USD
            currencySymbol = DOLLAR_SYMBOL;
            priceValue = pricesMap.get(DOLLAR_ISO4217_CODE);
            return currencySymbol + FormatUtils.formatInt(priceValue) + " " + DOLLAR_ISO4217_CODE;
        }
    }

    @Nullable
    public static HashMap<String, Feature> getFeatures() {
        String featuresString = AppPrefs.getGlobalPlansFeatures();
        if (TextUtils.isEmpty(featuresString)) {
            return null;
        }

        HashMap<String, Feature> features = new HashMap<>();
        try {
            JSONObject featuresJSONObject = new JSONObject(featuresString);
            JSONArray featuresArray = featuresJSONObject.getJSONArray("originalResponse");
            for (int i=0; i < featuresArray.length(); i ++) {
                JSONObject currentFeatureJSON = featuresArray.getJSONObject(i);
                Feature currentFeature = new Feature(currentFeatureJSON);
                features.put(currentFeature.getProductSlug(), currentFeature);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.PLANS, "Can't parse the features list returned from the server", e);
            return null;
        }

        return features;
    }

    /**
     * Returns the url of the image to display for the passed plan
     *
     * @param plan - The plan
     * @param iconSize - desired size of the returned image
     * @return string containing photon-ized url for the plan icon
     */
    public static String getIconUrlForPlan(Plan plan, int iconSize) {
        if (plan == null || !plan.hasIconUrl()) {
            return null;
        }
        return PhotonUtils.getPhotonImageUrl(plan.getIconUrl(), iconSize, iconSize);
    }

    /**
     * Removes stored plan data - for testing purposes
     */
    @SuppressWarnings("unused")
    public static void clearPlanData() {
        AppPrefs.setGlobalPlansFeatures(null);
    }

}
