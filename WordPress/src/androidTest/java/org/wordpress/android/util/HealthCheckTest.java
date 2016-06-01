package org.wordpress.android.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.InstrumentationTestCase;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.TestUtils;
import org.wordpress.android.WordPress;
import org.xmlrpc.android.LoggedInputStream;
import org.xmlrpc.android.XMLRPCUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class HealthCheckTest extends InstrumentationTestCase {
    private static final String sAssetPathBase = "health-check/";
    private static final String sServerAddressMagicString = "mockserver";
    private static final String sServerResponsesMagicScheme = "asset:";

    private void setLocale(String language, String country) {
        Locale locale = new Locale(language, country);
        Locale.setDefault(locale);
        Resources res = getInstrumentation().getTargetContext().getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @Override
    protected void setUp() {
        WordPress.setupVolleyQueue();

        // set the app locale to english since the tests only support English for now
        setLocale("en", "US");
    }

    @Override
    protected void tearDown() {
    }

    private static String stringFromAsset(Context context, String assetFilename) throws IOException {
        LoggedInputStream mLoggedInputStream = new LoggedInputStream(context.getAssets().open(assetFilename));
        return TestUtils.convertStreamToString(mLoggedInputStream);
    }

    private static JSONObject jsonFromAsset(Context context, String assetFilename) throws IOException, JSONException {
        return new JSONObject(stringFromAsset(context, assetFilename));
    }

    public void testHealthCheckXplat() throws JSONException, IOException {
        JSONArray testCases = jsonFromAsset(getInstrumentation().getContext(), sAssetPathBase +
                "health-check-xplat-testcases.json").getJSONArray("testcases");

        for (int i = 0; i < testCases.length(); i++) {
            final JSONObject testCase = testCases.getJSONObject(i);
            final String testCaseComment = testCase.getString("comment");

            final JSONObject testSetup = testCase.getJSONObject("setup");
            final String realm = testCase.getString("realm");

            switch (realm) {
                case "URL_CANONICALIZATION":
                    runUrlCanonicalization(testCaseComment, testSetup);
                    break;
                case "XMLPRC_DISCOVERY":
                    runXmlrpcDiscovery(testCaseComment, testSetup);
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
            canonicalizedUrl = XMLRPCUtils.sanitizeSiteUrl(inputUrl, true);

            // if we reached this point, it means that no error occurred
            assertNull(testCaseMessage("Testcase defines an error but no error occurred!", testCaseComment), error);
        } catch (XMLRPCUtils.XMLRPCUtilsException hce) {
            assertNotNull(testCaseMessage("Error occurred but testcase does not define an error!", testCaseComment),
                    error);

            assertEquals(testCaseMessage("Error message does not match the defined one!", testCaseComment), error
                    .getString("message"), getInstrumentation().getTargetContext().getString(hce.errorMsgId));
        }

        assertEquals(testCaseMessage("Canonicalized URL does not match the defined one!", testCaseComment),
                outputUrl, canonicalizedUrl);
    }

    private void runXmlrpcDiscovery(String testCaseComment, JSONObject testSetup) throws JSONException, IOException {
        final MockWebServer server = new MockWebServer();

        testSetup = new JSONObject(replaceServerMagicName(server, testSetup.toString()));

        final JSONObject input = testSetup.getJSONObject("input");

        setupMockHttpServer(server, input);

        final String inputUrl = input.isNull("siteUrl") ? server.url("").toString() : input.getString("siteUrl");

        final JSONObject output = testSetup.getJSONObject("output");

        final String outputUrl = output.optString("xmlrpcEndpoint", null);
        final JSONObject error = output.optJSONObject("error");

        String xmlrpcUrl = null;
        try {
            xmlrpcUrl = XMLRPCUtils.verifyOrDiscoverXmlRpcUrl(inputUrl, input.optString("username", null), input
                    .optString("username", null));

            // if we reached this point, it means that no error occurred
            assertNull(testCaseMessage("Testcase defines an error but no error occurred!", testCaseComment), error);
        } catch (XMLRPCUtils.XMLRPCUtilsException hce) {
            assertNotNull(testCaseMessage("Error occurred but testcase does not define an error!", testCaseComment),
                    error);

            assertEquals(testCaseMessage("Error message does not match the defined one!", testCaseComment), error
                    .getString("message"), getInstrumentation().getTargetContext().getString(hce.errorMsgId));
        }

        assertEquals(testCaseMessage("XMLRPC URL does not match the defined one!", testCaseComment), outputUrl,
                xmlrpcUrl);

        server.shutdown();
    }

    private MockWebServer setupMockHttpServer(MockWebServer server, JSONObject requestResponsesJson) throws
            JSONException, IOException {
        final Map<RecordedRequest, MockResponse> mockRequestResponses = new HashMap<>();

        final JSONArray serverMock = requestResponsesJson.getJSONArray("serverMock");
        for (int i = 0; i < serverMock.length(); i++) {
            final JSONObject reqRespJson = serverMock.getJSONObject(i);

            final JSONObject reqJson = reqRespJson.getJSONObject("request");
            Headers reqHeaders = json2Headers(reqJson.optJSONObject("headers"));

            RecordedRequest recordedRequest = new RecordedRequest(reqJson.getString("method") + " " + reqJson
                    .getString("path") + " HTTP/1.1", reqHeaders, null, 0, null, 0, null);

            final JSONObject respJson = reqRespJson.getJSONObject("response");
            Headers respHeaders = json2Headers(respJson.optJSONObject("headers"));

            String body = respJson.optString("body");
            if (body.startsWith(sServerResponsesMagicScheme)) {
                body = stringFromAsset(getInstrumentation().getContext(), sAssetPathBase + body.substring
                        (sServerResponsesMagicScheme.length()));
            }

            body = replaceServerMagicName(server, body);

            final MockResponse resp = new MockResponse()
                    .setResponseCode(respJson.getInt("statusCode"))
                    .setHeaders(respHeaders)
                    .setBody(body);

            mockRequestResponses.put(recordedRequest, resp);
        }

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

                for (Map.Entry<RecordedRequest, MockResponse> reqResp : mockRequestResponses.entrySet()) {
                    final RecordedRequest mockRequest = reqResp.getKey();
                    if (mockRequest.getRequestLine().equals(request.getRequestLine())) {
                        return reqResp.getValue();
                    }
                }
                return new MockResponse().setResponseCode(404).setBody("");
            }
        });

        return server;
    }

    private String replaceServerMagicName(MockWebServer server, String str) {
        return str.replaceAll(sServerAddressMagicString, server.getHostName() + ":" + server.getPort());

    }

    private Headers json2Headers(JSONObject headersJson) throws JSONException {
        if (headersJson != null) {
            Headers.Builder headBuilder = new Headers.Builder();
            Iterator<String> headerKeys = headersJson.keys();
            while (headerKeys.hasNext()) {
                final String headerName = headerKeys.next();
                headBuilder.add(headerName, headersJson.getString(headerName));
            }

            return headBuilder.build();
        }

        return new Headers.Builder().build();
    }

    private String testCaseMessage(String message, String testCaseComment) {
        return message + " (on testCase: '" + testCaseComment + "')";
    }
}
