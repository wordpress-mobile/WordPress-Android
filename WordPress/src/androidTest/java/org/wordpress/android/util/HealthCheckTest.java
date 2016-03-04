package org.wordpress.android.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.TestUtils;
import org.xmlrpc.android.LoggedInputStream;

import android.content.Context;
import android.test.InstrumentationTestCase;

import java.io.IOException;

public class HealthCheckTest extends InstrumentationTestCase {

    @Override
    protected void setUp() {
    }

    @Override
    protected void tearDown() {
    }

    private static JSONObject jsonFromAsset(Context context, String assetFilename) throws IOException, JSONException {
        LoggedInputStream mLoggedInputStream = new LoggedInputStream(context.getAssets().open(assetFilename));
        String jsonString = TestUtils.convertStreamToString(mLoggedInputStream);
        return new JSONObject(jsonString);
    }

    public void testHealthCheckXplat() throws JSONException, IOException {
        JSONArray testCases = jsonFromAsset(getInstrumentation().getContext(),
                "health-check/health-check-xplat-testcases.json").getJSONArray("testcases");

        for (int i = 0; i < testCases.length(); i++) {
            final JSONObject testCase = testCases.getJSONObject(i);
            final String testCaseComment = testCase.getString("comment");

            final JSONObject testSetup = testCase.getJSONObject("setup");
            final String realm = testCase.getString("realm");

            switch (realm) {
                case "URL_CANONICALIZATION":
                    runUrlCanonicalization(testCaseComment, testSetup);
                    break;
                default:
                    // fail the testsuite
                    assertTrue("health-check realm " + realm + " is not supported!", false);
                    break;
            }
        }
    }

    private void runUrlCanonicalization(String testCaseComment, JSONObject testSetup) throws JSONException {
        final JSONObject input = testSetup.getJSONObject("input");

        final String inputUrl = input.isNull("siteUrl") ? null : input.getString("siteUrl");

        final JSONObject output = testSetup.getJSONObject("output");

        final String outputUrl = output.optString("siteUrl", null);
        final JSONObject error = output.optJSONObject("error");

        String canonicalizedUrl = null;
        try {
            canonicalizedUrl = HealthCheckUtils.canonicalizeSiteUrl(inputUrl);

            // if we reached this point, it means that no error occurred
            assertNull(testCaseMessage("Testcase defines an error but no error occurred!", testCaseComment), error);
        } catch (HealthCheckUtils.HealthCheckException hce) {
            assertNotNull(testCaseMessage("Error occurred but testcase does not define an error!", testCaseComment),
                    error);

            assertEquals(testCaseMessage("Error message does not match the defined one!", testCaseComment), error
                    .getString("message"), getInstrumentation().getTargetContext().getString(hce.errorMsgId));
        }

        assertEquals(testCaseMessage("Canonicalized URL does not match the defined one!", testCaseComment),
                outputUrl, canonicalizedUrl);
    }

    private String testCaseMessage(String message, String testCaseComment) {
        return message + " (on testCase: '" + testCaseComment + "')";
    }
}
