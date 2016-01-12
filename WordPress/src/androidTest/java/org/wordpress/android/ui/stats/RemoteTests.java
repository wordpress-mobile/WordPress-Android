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
import org.wordpress.android.ui.stats.models.CommentsModel;
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

    public void testCommentsDay() throws Exception  {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                boolean parseError = false;
                try {
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

                } catch (JSONException e) {
                    parseError = true;
                    AppLog.e(AppLog.T.STATS, e);
                }
                assertFalse(parseError);
            }
        };
        RestRequest.ErrorListener errListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError response) {
                AppLog.e(AppLog.T.STATS, "The Rest Client returned an error from a mock call!?");
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/comments",
                null,
                listener,
                errListener
        );
    }
}
