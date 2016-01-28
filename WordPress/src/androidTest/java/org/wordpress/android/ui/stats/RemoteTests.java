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
import org.wordpress.android.ui.stats.models.FollowDataModel;
import org.wordpress.android.ui.stats.models.FollowerModel;
import org.wordpress.android.ui.stats.models.FollowersModel;
import org.wordpress.android.ui.stats.models.GeoviewModel;
import org.wordpress.android.ui.stats.models.GeoviewsModel;
import org.wordpress.android.ui.stats.models.InsightsAllTimeModel;
import org.wordpress.android.ui.stats.models.InsightsPopularModel;
import org.wordpress.android.ui.stats.models.InsightsTodayModel;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.stats.models.PostViewsModel;
import org.wordpress.android.ui.stats.models.ReferrerGroupModel;
import org.wordpress.android.ui.stats.models.ReferrerResultModel;
import org.wordpress.android.ui.stats.models.ReferrersModel;
import org.wordpress.android.ui.stats.models.SingleItemModel;
import org.wordpress.android.ui.stats.models.TagsContainerModel;
import org.wordpress.android.ui.stats.models.TagsModel;
import org.wordpress.android.ui.stats.models.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.models.VideoPlaysModel;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
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
            AppLog.e(AppLog.T.STATS, "The Rest Client returned an error from a mock call: " + response.getMessage());
            assertFalse(response.getMessage(), true); // force the test to fails in this case
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

    public void testFollowersEmail() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                FollowersModel model = new FollowersModel("123456", response);
                assertEquals(model.getTotalEmail(), 2931);
                assertEquals(model.getTotalWPCom(), 7926165);
                assertEquals(model.getTotal(), 2931);
                assertEquals(model.getPage(), 1);
                assertEquals(model.getPages(), 419);

                assertNotNull(model.getFollowers());
                assertEquals(model.getFollowers().size(), 7);
                FollowerModel first = model.getFollowers().get(0);
                assertEquals(first.getAvatar(), "https://2.gravatar.com/avatar/e82142697283897ad7444810e5975895?s=64" +
                        "&amp;d=https%3A%2F%2F2.gravatar.com%2Favatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D64&amp;r=G");
                assertEquals(first.getLabel(), "user1@example.com");
                assertNull(first.getURL());
                assertNull(first.getFollowData());
                assertEquals(first.getDateSubscribed(), "2014-12-16T11:24:41+00:00");
                FollowerModel last = model.getFollowers().get(6);
                assertEquals(last.getAvatar(), "https://0.gravatar.com/avatar/3b37f38b63ce4f595cc5cfbaadb10938?s=64" +
                        "&amp;d=https%3A%2F%2F0.gravatar.com%2Favatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D64&amp;r=G");
                assertEquals(last.getLabel(), "user7@example.com");
                assertNull(last.getURL());
                assertNull(last.getFollowData());
                assertEquals(last.getDateSubscribed(), "2014-12-15T15:09:01+00:00");
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/followers",
                null,
                listener,
                errListener
        );
    }

    public void testFollowersWPCOM() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                FollowersModel model = new FollowersModel("1234567890", response);
                assertEquals(model.getTotalEmail(), 2930);
                assertEquals(model.getTotalWPCom(), 7925800);
                assertEquals(model.getTotal(), 7925800);
                assertEquals(model.getPage(), 1);
                assertEquals(model.getPages(), 1132258);

                assertNotNull(model.getFollowers());
                assertEquals(model.getFollowers().size(), 7);
                FollowerModel first = model.getFollowers().get(0);
                assertEquals(first.getAvatar(), "https://0.gravatar.com/avatar/624b89cb0c8b9136f9629dd7bcab0517?s=64" +
                        "&amp;d=https%3A%2F%2F0.gravatar.com%2Favatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D64&amp;r=G");
                assertEquals(first.getLabel(), "ritu929");
                assertEquals(first.getURL(), "http://ritu9blog.wordpress.com");
                assertEquals(first.getDateSubscribed(), "2014-12-16T14:53:21+00:00");
                assertNotNull(first.getFollowData());
                FollowDataModel followDatamodel = first.getFollowData();
                assertFalse(followDatamodel.isFollowing());
                assertEquals(followDatamodel.getType(), "follow");

            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/1234567890/stats/followers",
                null,
                listener,
                errListener
        );
    }

    public void testPostDetails() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                PostViewsModel model = new PostViewsModel(response);
                assertNotNull(model.getOriginalResponse());

                assertEquals(model.getDate(), "2015-03-04");
                assertEquals(model.getHighestMonth(), 278);
                assertEquals(model.getHighestDayAverage(), 8);
                assertEquals(model.getHighestWeekAverage(), 8);

                assertNotNull(model.getDayViews());
                assertEquals(model.getDayViews()[0].getViews(), 0);
                assertEquals(model.getDayViews()[0].getPeriod(), "2014-06-04");
                assertEquals(model.getDayViews()[model.getDayViews().length-1].getViews(), 8);
                assertEquals(model.getDayViews()[model.getDayViews().length - 1].getPeriod(), "2015-03-04");

                assertNotNull(model.getYears().size());
                assertEquals(model.getYears().size(), 2);
                assertEquals(model.getYears().get(0).getTotal(), 1097);
                assertEquals(model.getYears().get(0).getLabel(), "2014");
                assertEquals(model.getYears().get(0).getMonths().size(), 7);
                assertEquals(model.getYears().get(0).getMonths().get(0).getMonth(), "6");
                assertEquals(model.getYears().get(1).getTotal(), 226);
                assertEquals(model.getYears().get(1).getLabel(), "2015");

                assertNotNull(model.getWeeks().size());
                assertEquals(model.getWeeks().size(), 6);

                assertNotNull(model.getAverages());
                assertEquals(model.getAverages().size(), 2);
                assertEquals(model.getAverages().get(0).getTotal(), 5);
                assertEquals(model.getAverages().get(0).getLabel(), "2014");
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/post/123",
                null,
                listener,
                errListener
        );
    }

    public void testReferrers() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                ReferrersModel model = new ReferrersModel("123456", response);
                assertEquals(model.getTotalViews(), 2161);
                assertEquals(model.getOtherViews(), 938);
                assertNotNull(model.getGroups());
                assertEquals(model.getGroups().size(), 10);

                // first group in the response
                ReferrerGroupModel gModel = model.getGroups().get(0);
                assertEquals(gModel.getName(), "Search Engines");
                assertEquals(gModel.getGroupId(), "Search Engines");
                assertEquals(gModel.getIcon(), "https://wordpress.com/i/stats/search-engine.png");
                assertEquals(gModel.getTotal(), 480);
                assertNotNull(gModel.getResults());
                assertEquals(gModel.getResults().size(), 7);

                // 2nd level item
                ReferrerResultModel refResultModel = gModel.getResults().get(0);
                assertEquals(refResultModel.getName(), "Google Search");
                assertEquals(refResultModel.getIcon(), "https://secure.gravatar.com/blavatar/6741a05f4bc6e5b65f504c4f3df388a1?s=48");
                assertEquals(refResultModel.getViews(), 461);
                assertNotNull(refResultModel.getChildren());
                assertNull(refResultModel.getUrl()); //has childs. No URL.

                // 3rd level items
                SingleItemModel child =  refResultModel.getChildren().get(0);
                assertEquals(child.getUrl(), "http://www.google.com/");
                assertEquals(child.getTitle(), "google.com");
                assertEquals(child.getIcon(), "https://secure.gravatar.com/blavatar/ff90821feeb2b02a33a6f9fc8e5f3fcd?s=48");
                assertEquals(child.getTotals(), 176);
                child =  refResultModel.getChildren().get(10);
                assertEquals(child.getUrl(), "http://www.google.co.jp");
                assertEquals(child.getTitle(), "google.co.jp");
                assertEquals(child.getIcon(), "https://secure.gravatar.com/blavatar/a28b8206a6562f6098688508d4665905?s=48");
                assertEquals(child.getTotals(), 6);


                // 7th group in the response
                gModel = model.getGroups().get(6);
                assertEquals(gModel.getName(), "ma.tt");
                assertEquals(gModel.getGroupId(), "ma.tt");
                assertEquals(gModel.getIcon(), "https://secure.gravatar.com/blavatar/733a27a6b983dd89d6dd64d0445a3e8e?s=48");
                assertEquals(gModel.getTotal(), 56);
                assertNotNull(gModel.getResults());
                assertEquals(gModel.getResults().size(), 11);

                // 2nd level item
                refResultModel = gModel.getResults().get(0);
                assertEquals(refResultModel.getName(), "ma.tt");
                assertEquals(refResultModel.getUrl(), "http://ma.tt/");
                assertEquals(refResultModel.getIcon(), "");
                assertEquals(refResultModel.getViews(), 34); // No childs. Has URL.
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/referrers",
                null,
                listener,
                errListener
        );
    }

    public void testTagsCategories() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                TagsContainerModel model = new TagsContainerModel("123456", response);
                assertEquals(model.getDate(), "2014-12-16");
                assertNotNull(model.getTags());
                assertEquals(model.getTags().size(), 10);

                TagsModel tag = model.getTags().get(0);
                assertEquals(tag.getViews(), 461);
                assertNotNull(tag.getTags());
                assertEquals(tag.getTags().size(), 1);
                assertNotNull(tag.getTags());
                assertEquals(tag.getTags().get(0).getName(), "Uncategorized");
                assertEquals(tag.getTags().get(0).getType(), "category");
                assertEquals(tag.getTags().get(0).getLink(), "http://astralbodi.es/category/uncategorized/");

                tag = model.getTags().get(9);
                assertEquals(tag.getViews(), 41);
                assertEquals(tag.getTags().get(0).getName(), "networking");
                assertEquals(tag.getTags().get(0).getType(), "tag");
                assertEquals(tag.getTags().get(0).getLink(), "http://astralbodi.es/tag/networking/");
                assertEquals(tag.getTags().get(1).getName(), "unix");
                assertEquals(tag.getTags().get(1).getType(), "tag");
                assertEquals(tag.getTags().get(1).getLink(), "http://astralbodi.es/tag/unix/");
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/tags",
                null,
                listener,
                errListener
        );
    }

    public void testTopPost() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                TopPostsAndPagesModel model = new TopPostsAndPagesModel("123456", response);
                assertNotNull(model.getTopPostsAndPages());
                assertEquals(model.getTopPostsAndPages().size(), 10);

                PostModel postModel = model.getTopPostsAndPages().get(0);
                assertEquals(postModel.getItemID(), "39806");
                assertEquals(postModel.getTotals(), 2420);
                assertEquals(postModel.getTitle(), "Home");
                assertEquals(postModel.getUrl(), "http://automattic.com/home/");
                assertEquals(postModel.getDate(), StatsUtils.toMs("2011-08-30 21:47:38"));
                assertEquals(postModel.getPostType(), "page");

                postModel = model.getTopPostsAndPages().get(9);
                assertEquals(postModel.getItemID(), "39254");
                assertEquals(postModel.getTotals(), 56);
                assertEquals(postModel.getTitle(), "Growth Explorer");
                assertEquals(postModel.getUrl(), "http://automattic.com/work-with-us/growth-explorer/");
                assertEquals(postModel.getDate(), StatsUtils.toMs("2011-08-25 19:37:27"));
                assertEquals(postModel.getPostType(), "page");
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/top-posts",
                null,
                listener,
                errListener
        );
    }

    public void testTopPostEmptyURL() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                TopPostsAndPagesModel model = new TopPostsAndPagesModel("1234567890", response);
                assertNotNull(model.getTopPostsAndPages());
                assertEquals(model.getTopPostsAndPages().size(), 10);

                PostModel postModel = model.getTopPostsAndPages().get(0);
                assertEquals(postModel.getItemID(), "750");
                assertEquals(postModel.getTotals(), 7);
                assertEquals(postModel.getTitle(), "Asynchronous unit testing Core Data with Xcode 6");
                assertEquals(postModel.getUrl(), ""); // This post has no URL?!? Unpublished post that was prev published?
                assertEquals(postModel.getDate(), StatsUtils.toMs("2014-08-06 14:52:11"));
                assertEquals(postModel.getPostType(), "post");
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/1234567890/stats/top-posts",
                null,
                listener,
                errListener
        );
    }

    public void testInsightsAllTime() throws Exception  {
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

    public void testInsightsToday() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                InsightsTodayModel model = new InsightsTodayModel("123456", response);
                assertEquals(model.getDate(), "2014-10-28");
                assertEquals(model.getBlogID(), "123456");
                assertEquals(model.getViews(), 56);
                assertEquals(model.getVisitors(), 44);
                assertEquals(model.getLikes(), 1);
                assertEquals(model.getReblogs(), 2);
                assertEquals(model.getComments(), 3);
                assertEquals(model.getFollowers(), 56);
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/summary",
                null,
                listener,
                errListener
        );
    }

    public void testInsightsPopular() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                InsightsPopularModel model = new InsightsPopularModel("123456", response);
                assertEquals(model.getHighestHour(), 9);
                assertEquals(model.getHighestDayOfWeek(), 5);
                assertEquals(model.getHighestDayPercent(), 30.532081377152);
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/insights",
                null,
                listener,
                errListener
        );
    }

    public void testVideoPlaysNoData() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                VideoPlaysModel model = new VideoPlaysModel("123456", response);
                assertEquals(model.getOtherPlays(), 0);
                assertEquals(model.getTotalPlays(), 0);
                assertNotNull(model.getPlays());
                assertEquals(model.getPlays().size(), 0);
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/video-plays",
                null,
                listener,
                errListener
        );
    }

    public void testVideoPlays() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                VideoPlaysModel model = new VideoPlaysModel("1234567890", response);
                assertEquals(model.getOtherPlays(), 0);
                assertEquals(model.getTotalPlays(), 2);
                assertNotNull(model.getPlays());
                assertEquals(model.getPlays().size(), 1);
                SingleItemModel videoItemModel = model.getPlays().get(0);
                assertEquals(videoItemModel.getTitle(), "Test Video");
                assertEquals(videoItemModel.getUrl(), "http://maplebaconyummies.wordpress.com/wp-admin/media.php?action=edit&attachment_id=144");
                assertEquals(videoItemModel.getItemID(), "144");
                assertEquals(videoItemModel.getTotals(), 2);
            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/1234567890/stats/video-plays",
                null,
                listener,
                errListener
        );
    }

    public void testVisits() throws Exception  {
        StatsRestRequestAbstractListener listener  = new StatsRestRequestAbstractListener() {
            @Override
            void parseResponse(JSONObject response) throws JSONException {
                VisitsModel model = new VisitsModel("123456", response);
                assertNotNull(model.getVisits());
                assertNotNull(model.getUnit());
                assertNotNull(model.getDate());

                assertEquals(model.getVisits().size(), 30);
                assertEquals(model.getUnit(), "day");

                VisitModel visitModel = model.getVisits().get(0);
                assertEquals(visitModel.getViews(), 7808);
                assertEquals(visitModel.getVisitors(), 4331);
                assertEquals(visitModel.getLikes(), 0);
                assertEquals(visitModel.getComments(), 0);
                assertEquals(visitModel.getPeriod(), "2014-10-08");

            }
        };

        mRestClient.makeRequest(Request.Method.POST, "https://public-api.wordpress.com/rest/v1.1/sites/123456/stats/visits",
                null,
                listener,
                errListener
        );
    }
}
