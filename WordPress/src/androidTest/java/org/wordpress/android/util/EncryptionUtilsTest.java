package org.wordpress.android.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.AuthFailureError;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class EncryptionUtilsTest {
    // test data
    static final String TEST_DELIMITER = "\n";
    static final String TEST_LOG_STRING = "WordPress - 13.5 - Version code: 789\n"
            + "Android device name: Google Android SDK built for x86\n\n"
            + "01 - [Nov-11 03:04 UTILS] WordPress.onCreate\n"
            + "02 - [Nov-11 03:04 API] Dispatching action: ListAction-REMOVE_EXPIRED_LISTS\n"
            + "03 - [Nov-11 03:04 API] QuickStartStore onRegister\n"
            + "04 - [Nov-11 03:04 STATS] 🔵 Tracked: deep_link_not_default_handler, "
            + "Properties: {\"interceptor_classname\":\"com.google.android.setupwizard.util.WebDialogActivity\"}\n"
            + "05 - [Nov-11 03:04 UTILS] App comes from background\n"
            + "06 - [Nov-11 03:04 STATS] 🔵 Tracked: application_opened\n"
            + "07 - [Nov-11 03:04 READER] notifications update job service > job scheduled\n"
            + "08 - [Nov-11 03:04 API] Dispatching action: SiteAction-FETCH_SITES\n"
            + "09 - [Nov-11 03:04 API] StackTrace: com.android.volley.AuthFailureError\n"
            + "    at com.android.volley.toolbox.BasicNetwork.performRequest(BasicNetwork.java:195)\n"
            + "    at com.android.volley.NetworkDispatcher.processRequest(NetworkDispatcher.java:131)\n"
            + "    at com.android.volley.NetworkDispatcher.processRequest(NetworkDispatcher.java:111)\n"
            + "    at com.android.volley.NetworkDispatcher.run(NetworkDispatcher.java:90)\n";

    // for endpoint test
    static final String END_POINT_TEST_PUBLIC_KEY = "K0y2oQ++gEN00S4CbCH3IYoBIxVF6H86Wz4wi2t2C3M=";
    static final String END_POINT_TEST_URL = "https://log-encryption-testing.herokuapp.com";

    @Test
    public void testEncryptionResultIsValidWithTestEndpointDecryption() {
        final Context context = ApplicationProvider.getApplicationContext();

        final String encryptionDataJson = getEncryptionDataJson(END_POINT_TEST_PUBLIC_KEY);
        if (encryptionDataJson == null) {
            fail("unable to encrypt test data");
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        StringRequest postRequest = new StringRequest(
                Method.POST,
                END_POINT_TEST_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        assertEquals(response, TEST_LOG_STRING + TEST_DELIMITER);
                        countDownLatch.countDown();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        fail("PostRequest failed with error: " + error.toString());
                        countDownLatch.countDown();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
            @Override
            public byte[] getBody() throws AuthFailureError {
                return encryptionDataJson.getBytes();
            }
        };

        postRequest.setShouldCache(false);
        Volley.newRequestQueue(context).add(postRequest);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            fail("CountDownLatch await interrupted for post request: " + e.toString());
        }
    }

    private String getEncryptionDataJson(String publicKeyBase64) {
        try {
            final String encryptionDataJson = EncryptionUtils.encryptStringData(
                    publicKeyBase64,
                    TEST_LOG_STRING,
                    TEST_DELIMITER);

            return encryptionDataJson;
        } catch (JSONException e) {
            fail("encryptStringData failed with JSONException: " + e.toString());
        }
        return null;
    }
}
