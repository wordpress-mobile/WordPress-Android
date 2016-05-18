package org.wordpress.android.ui.plans;

import android.content.Context;
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.plans.util.IabException;
import org.wordpress.android.ui.plans.util.IabHelper;
import org.wordpress.android.ui.plans.util.IabResult;
import org.wordpress.android.ui.plans.util.Inventory;
import org.wordpress.android.ui.plans.util.Purchase;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  Synch IAPs on the wpcom backend. This need to be called to add/remove upgrades on wpcom side.
 *
 */
public class UpdateIAPTask extends AsyncTask<Void, Void, Void> {
    private static final int GET_IAP_BINDER_TIMEOUT = 30000;
    private static final String IAP_ENDPOINT = "/iap/validate";
    private final Context mContext;
    private IabHelper mIabHelper;
    private boolean mIABSetupDone = false;

    public UpdateIAPTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... args) {
        if (!AccountHelper.isSignedInWordPressDotCom()) {
            return null;
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        mIabHelper = new IabHelper(this.mContext, BuildConfig.APP_LICENSE_KEY);
        if (BuildConfig.DEBUG) {
            mIabHelper.enableDebugLogging(true);
        }
        try {
            mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {
                    if (result.isSuccess()) {
                        mIABSetupDone = true;
                        AppLog.d(AppLog.T.PLANS, "IAB started successfully");
                    } else {
                        AppLog.w(AppLog.T.PLANS, "IAB failed with " + result);
                    }
                    countDownLatch.countDown();
                }
            });
            try {
                countDownLatch.await(GET_IAP_BINDER_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                AppLog.e(AppLog.T.PLANS, "IAP setup took too long! > " + GET_IAP_BINDER_TIMEOUT + " msecs!", e);
                return null;
            }
        } catch (NullPointerException e) {
            // will happen when play store isn't available on device
            //AppLog.e(AppLog.T.PLANS, e);
            AppLog.w(AppLog.T.PLANS, "Unable to start IAB helper. Happen when Play Store isn't available on device.");
            return null;
        }

        listIAPs();
        stopInAppBillingHelper();
        return null;
    }

    private void listIAPs() {
        if (mIabHelper == null || !mIABSetupDone) {
            return;
        }
        try {
            Inventory inventory = mIabHelper.queryInventory(true, null, null);
            List<Purchase> iaps = inventory.getAllPurchases();
            for (Purchase purchase : iaps) {
                AppLog.d(AppLog.T.PLANS, "Original purchase JSON " + purchase.getOriginalJson());
                try {
                    JSONObject developerPayload = new JSONObject(purchase.getDeveloperPayload());
                    Map<String, String> params = new HashMap<>();
                    params.put("blog_id", developerPayload.getString("blog_id"));
                    params.put("iap_sku", purchase.getSku());
                    params.put("iap_token", purchase.getToken());
                    WordPress.getRestClientUtilsV1_1().post(IAP_ENDPOINT, params, null,
                            new RestRequest.Listener() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    if (response != null) {
                                        AppLog.d(AppLog.T.PLANS, "Response from the server: " + response.toString());
                                    }
                                    AppPrefs.setInAppPurchaseRefreshRequired(false);
                                }
                            },
                            new RestRequest.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    AppLog.e(AppLog.T.PLANS, "Response from the server", error);
                                }
                            });
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.PLANS, "Unable to parse IAP info - " + purchase.getOriginalJson(), e);
                }
            }
        } catch (IabException e) {
            AppLog.e(AppLog.T.PLANS, "Unable to refresh the inventory", e);
        }
    }

    private void stopInAppBillingHelper() {
        if (mIabHelper != null) {
            try {
                mIabHelper.dispose();
            } catch (IllegalArgumentException e) {
                // this can happen if the IAB helper was created but failed to bind to its service
                // when started, which will occur on emulators
                AppLog.w(AppLog.T.PLANS, "Unable to dispose IAB helper");
            }
            mIabHelper = null;
        }
    }
}
