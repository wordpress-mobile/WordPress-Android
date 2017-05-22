package org.wordpress.android.ui.plans;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.DefaultMocksInstrumentationTestCase;
import org.wordpress.android.mocks.RestClientCustomizableMock;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.networking.RestClientFactory;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

public class RemoteTests extends DefaultMocksInstrumentationTestCase {
    private RestClientCustomizableMock mRestClientV1_2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Set the version of the REST client to v1.2
        RestClientFactoryTest.sVersion = RestClient.REST_CLIENT_VERSIONS.V1_2;
        mRestClientV1_2 = (RestClientCustomizableMock) RestClientFactory.instantiate(null,  RestClient.REST_CLIENT_VERSIONS.V1_2);
    }

    private RestRequest.ErrorListener errListener = new RestRequest.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError response) {
            AppLog.e(AppLog.T.PLANS, "The Rest Client returned an error from a mock call: " + response.getMessage());
            assertFalse(response.getMessage(), true); // force the test to fails in this case
        }
    };

    // Just a Utility class that wraps the main logic for the OK listener
    private abstract class PlansRestRequestAbstractListener implements RestRequest.Listener {
        @Override
        public void onResponse(JSONObject response) {
            boolean parseError = false;
            try {
                parseResponse(response);
            } catch (JSONException e) {
                parseError = true;
                AppLog.e(AppLog.T.PLANS, e);
            }
            assertFalse(parseError);
        }
        abstract void parseResponse(JSONObject response) throws JSONException;
    }

    public void testSitePlans() throws Exception {
        PlansRestRequestAbstractListener listener = new PlansRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                List<Plan> plans = new ArrayList<>();
                JSONArray plansArray = response.getJSONArray("originalResponse");
                for (int i=0; i < plansArray.length(); i ++) {
                    JSONObject currentPlanJSON = plansArray.getJSONObject(i);
                    Plan currentPlan = new Plan(currentPlanJSON);
                    plans.add(currentPlan);
                }

                assertEquals(3, plans.size());

                Plan currentPlan = plans.get(0);
                assertEquals(currentPlan.getDescription(), "Get a free blog and be on your way to publishing your first post in less than five minutes.");
                assertEquals(currentPlan.getProductID(), 1L);
                assertEquals(currentPlan.getProductName(), "WordPress.com Free");
                assertEquals(currentPlan.getBillPeriod(), -1);
                assertEquals(currentPlan.getRawPrice(), 0);
                assertEquals(currentPlan.getCost(), 0);
                assertEquals(currentPlan.isAvailable(), true);

                currentPlan = plans.get(1);
                assertEquals(currentPlan.isFreeTrial(), false);
                assertEquals(currentPlan.getBundleSubscriptionID(), "5683566");
                assertEquals(currentPlan.getExpiry(), "2017-03-07");
                assertEquals(currentPlan.getUserFacingExpiry(), "2017-03-04");
                assertEquals(currentPlan.getSubscribedDate(), "2016-03-07 08:56:13");

                currentPlan = plans.get(2);
                assertEquals(currentPlan.getDescription(), "Everything included with Premium, as well as live chat support, and unlimited access to our premium themes.");
                assertEquals(currentPlan.getProductID(), 1008L);
                assertEquals(currentPlan.getProductName(), "WordPress.com Business");
                assertEquals(currentPlan.getBillPeriod(), 365);
                assertEquals(currentPlan.getRawPrice(), 199);
                assertEquals(currentPlan.getCost(), 199);
                assertEquals(currentPlan.isAvailable(), true);
            }
        };


        mRestClientV1_2.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.2/sites/123456/plans",
                null,
                listener,
                errListener
        );
    }

    public void testFeatures() throws Exception {
        PlansRestRequestAbstractListener listener = new PlansRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                // Parse the response from the server
                List<Feature> features = new ArrayList<>();
                JSONArray featuresArray = response.getJSONArray("originalResponse");
                for (int i = 0; i < featuresArray.length(); i++) {
                    JSONObject currentFeatureJSON = featuresArray.getJSONObject(i);
                    Feature currentFeature = new Feature(currentFeatureJSON);
                    features.add(currentFeature);
                }

                assertEquals(16, features.size());

                // Test the 1st object in the response
                Feature currentFeatures = features.get(0);
                assertEquals("WordPress.com Site", currentFeatures.getTitle());
                assertEquals("free-blog", currentFeatures.getProductSlug());
                assertEquals("Your own space to create posts and pages with basic customization.", currentFeatures.getDescription());
                assertEquals("Your own space to create posts and pages with basic customization.",
                        currentFeatures.getDescriptionForPlan(1L));
                assertEquals("Your own space to create posts and pages with basic customization.",
                        currentFeatures.getDescriptionForPlan(1003L));
                assertEquals("Your own space to create posts and pages with basic customization.",
                        currentFeatures.getDescriptionForPlan(1008L));

                assertEquals(false, currentFeatures.isNotPartOfFreeTrial());

                // Test the latest object in the response
                currentFeatures = features.get(15);
                assertEquals("Support", currentFeatures.getTitle());
                assertEquals("support", currentFeatures.getProductSlug());
                assertEquals("For those times when you can't find an answer on our Support site", currentFeatures.getDescription());
                assertEquals("Find answers to your questions in our community forum.",
                        currentFeatures.getDescriptionForPlan(1L));
                assertEquals("Community support",
                        currentFeatures.getTitleForPlan(1L));
                assertEquals("The kind of support we offer for Jetpack Business.",
                        currentFeatures.getDescriptionForPlan(2001L));
                assertEquals("Priority security support",
                        currentFeatures.getTitleForPlan(2001L));
                assertEquals(false, currentFeatures.isNotPartOfFreeTrial());
            }
        };

        mRestClientV1_2.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.2/plans/features",
                null,
                listener,
                errListener
        );
    }
}
