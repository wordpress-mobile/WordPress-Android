package org.wordpress.android.ui.stats;


import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.DefaultMocksInstrumentationTestCase;
import org.wordpress.android.mocks.RestClientCustomizableMock;
import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.networking.RestClientFactory;
import org.wordpress.android.ui.stats.models.AuthorModel;
import org.wordpress.android.ui.stats.models.ClickGroupModel;
import org.wordpress.android.ui.stats.models.ClicksModel;
import org.wordpress.android.ui.stats.models.CommentsModel;
import org.wordpress.android.ui.stats.models.GeoviewModel;
import org.wordpress.android.ui.stats.models.GeoviewsModel;
import org.wordpress.android.ui.stats.models.InsightsAllTimeModel;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.util.AppLog;


public class RemoteTests extends DefaultMocksInstrumentationTestCase {

    private RestClientCustomizableMock mRestClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Set the version of the REST client to 1.1
        RestClientFactoryTest.sVersion = RestClient.REST_CLIENT_VERSIONS.V1_1;

        mRestClient = (RestClientCustomizableMock) RestClientFactory.instantiate(null, RestClient.REST_CLIENT_VERSIONS.V1_1);
    }

    private RestRequest.ErrorListener errListener = new RestRequest.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError response) {
            AppLog.e(AppLog.T.STATS, "The Rest Client returned an error from a mock call!?");
        }
    };

    // Just a Utility class that wraps the main logic for the OK listener
    private abstract class StatsRestRequestAbstractListener implements RestRequest.Listener {
        @Override
        public void onResponse(JSONObject response) {
            boolean parseError = false;
                try {
                    parseResponse(response);
                } catch (JSONException e) {
                    parseError = true;
                    AppLog.e(AppLog.T.STATS, e);
                }
            assertFalse(parseError);
        }
        abstract void parseResponse(JSONObject response) throws JSONException;
    }


    public void testAllTime() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                InsightsAllTimeModel model = new InsightsAllTimeModel("12345",response);
                assertEquals(model.getPosts(), 128);
                assertEquals(model.getViews(), 56687);
                assertEquals(model.getVisitors(), 42893);
                assertEquals(model.getViewsBestDayTotal(), 3485);
                assertNotNull(model.getViewsBestDay());
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats",
                null,
                listener,
                errListener
        );
    }

    public void testClicks() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                ClicksModel model = new ClicksModel("123456",response);
                assertEquals(model.getTotalClicks(), 2);
                assertEquals(model.getOtherClicks(), 0);
                assertNotNull(model.getClickGroups());
                assertEquals(model.getClickGroups().size(), 2);

                ClickGroupModel first = model.getClickGroups().get(0);
                assertEquals(first.getIcon(), "");
                assertEquals(first.getUrl(), "http://astralbodies.net/blog/2013/10/31/paying-attention-at-automattic/");
                assertEquals(first.getName(), "astralbodies.net/blog/2013/10/31/paying-attention-at-automattic/");
                assertEquals(first.getViews(), 1);
                assertNull(first.getClicks());

                ClickGroupModel second = model.getClickGroups().get(1);
                assertEquals(second.getIcon(), "");
                assertEquals(second.getUrl(), "https://devforums.apple.com/thread/86137");
                assertEquals(second.getName(), "devforums.apple.com/thread/86137");
                assertEquals(second.getViews(), 1);
                assertNull(second.getClicks());
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/clicks",
                null,
                listener,
                errListener
        );
    }

    public void testClicksForMonth() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                ClicksModel model = new ClicksModel("1234567890",response);
                assertEquals(model.getTotalClicks(), 9);
                assertEquals(model.getOtherClicks(), 0);
                assertNotNull(model.getClickGroups());
                assertEquals(model.getClickGroups().size(), 6);

                ClickGroupModel first = model.getClickGroups().get(0);
                assertEquals(first.getIcon(), "");
                assertEquals(first.getUrl(), "http://wp.com/");
                assertEquals(first.getName(), "wp.com");
                assertEquals(first.getViews(), 3);
                assertNull(first.getClicks());

                ClickGroupModel second = model.getClickGroups().get(1);
                assertEquals(second.getIcon(), "");
                assertNull(second.getUrl());
                assertEquals(second.getName(), "blog.wordpress.tv");
                assertEquals(second.getViews(), 2);
                assertNotNull(second.getClicks());
                assertEquals(second.getClicks().size(), 2);

                SingleItemModel firstChild = second.getClicks().get(0);
                assertNotNull(firstChild);
                assertEquals(firstChild.getUrl(), "http://blog.wordpress.tv/2014/10/03/build-your-audience-recent-wordcamp-videos-from-experienced-content-creators/");
                assertEquals(firstChild.getTitle(), "blog.wordpress.tv/2014/10/03/build-your-audience-recent-wordcamp-videos-from-experienced-content-creators/");
                assertEquals(firstChild.getTotals(), 1);
                assertEquals(firstChild.getIcon(), "");


                SingleItemModel secondChild = second.getClicks().get(1);
                assertNotNull(secondChild);
                assertEquals(secondChild.getUrl(), "http://blog.wordpress.tv/2014/10/29/wordcamp-san-francisco-2014-state-of-the-word-keynote/");
                assertEquals(secondChild.getTitle(), "blog.wordpress.tv/2014/10/29/wordcamp-san-francisco-2014-state-of-the-word-keynote/");
                assertEquals(secondChild.getTotals(), 1);
                assertEquals(secondChild.getIcon(), "");

            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/1234567890/stats/clicks",
                null,
                listener,
                errListener
        );
    }

    public void testCommentsDay() throws Exception  {

        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                CommentsModel model = new CommentsModel("123456", response);
                assertEquals(model.getTotalComments(), 177);
                assertEquals(model.getMonthlyComments(), 2);
                assertEquals(model.getMostActiveTime(), "08:00");
                assertEquals(model.getMostActiveDay(), "");

                assertNotNull(model.getAuthors());
                assertTrue(model.getAuthors().size() == 7);
                AuthorModel author = model.getAuthors().get(0);
                assertEquals(author.getName(), "Aaron Douglas");
                assertEquals(author.getViews(), 20);
                assertEquals(author.getAvatar(),
                        "https://1.gravatar.com/avatar/db127a496309f2717657d6f6167abd49?s=64&amp;" +
                                "d=https%3A%2F%2F1.gravatar.com%2Favatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D64&amp;r=R"
                );
                assertNull(author.getFollowData());
                assertNull(author.getPosts());

                assertNotNull(model.getPosts());
                assertTrue(model.getPosts().size() == 11);
                SingleItemModel mostCommentedPost = model.getPosts().get(0);
                assertEquals(mostCommentedPost.getItemID(), "67");
                assertEquals(mostCommentedPost.getTotals(), 29);
                assertEquals(mostCommentedPost.getTitle(), "Mac Screen Sharing (VNC) & White Screen");
                assertEquals(mostCommentedPost.getUrl(), "http://astralbodi.es/2010/05/02/mac-screen-sharing-vnc-white-screen/");
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/comments",
                null,
                listener,
                errListener
        );
    }

    public void testCountryViewsDay() throws Exception  {

        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                GeoviewsModel model = new GeoviewsModel("123456", response);
                assertEquals(model.getOtherViews(), 17);
                assertEquals(model.getTotalViews(), 55);

                assertNotNull(model.getCountries());
                assertEquals(model.getCountries().size(), 10);
                GeoviewModel first = model.getCountries().get(0);
                assertEquals(first.getCountryFullName(), "United States");
                assertEquals(first.getFlagIconURL(), "https://secure.gravatar.com/blavatar/5a83891a81b057fed56930a6aaaf7b3c?s=48");
                assertEquals(first.getFlatFlagIconURL(), "https://secure.gravatar.com/blavatar/9f4faa5ad0c723474f7a6d810172447c?s=48");
                assertEquals(first.getViews(), 8);
                GeoviewModel second = model.getCountries().get(1);
                assertEquals(second.getCountryFullName(), "Taiwan");
                assertEquals(second.getFlagIconURL(), "https://secure.gravatar.com/blavatar/f983fff0dda7387746b697cfd865e657?s=48");
                assertEquals(second.getFlatFlagIconURL(), "https://secure.gravatar.com/blavatar/2c224480a40527ee89d7340d4396e8e6?s=48");
                assertEquals(second.getViews(), 6);
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/country-views",
                null,
                listener,
                errListener
        );
    }
}
