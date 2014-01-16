/**
 * Interface to the WordPress.com REST API.
 */
package org.wordpress.android.networking;

import com.android.volley.RetryPolicy;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.wordpress.android.models.Note;
import org.wordpress.android.ui.stats.StatsBarChartUnit;

import java.util.Map;

public interface RestClientUtilsInterface {
    public void replyToComment(Note.Reply reply, Listener listener, ErrorListener errorListener);
    public void replyToComment(String siteId, String commentId, String content, Listener listener,
                               ErrorListener errorListener);
    public void followSite(String siteId, Listener listener, ErrorListener errorListener);
    public void unfollowSite(String siteId, Listener listener, ErrorListener errorListener);
    public void getNotification(String noteId, Listener listener, ErrorListener errorListener);
    public void markNoteAsRead(Note note, Listener listener, ErrorListener errorListener);
    public void getNotifications(Map<String, String> params, Listener listener, ErrorListener errorListener);
    public void getNotifications(Listener listener, ErrorListener errorListener);
    public void markNotificationsSeen(String timestamp, Listener listener, ErrorListener errorListener);
    public void moderateComment(String siteId, String commentId, String status, Listener listener,
                                ErrorListener errorListener);
    public void getThemes(String siteId, int limit, int offset, Listener listener, ErrorListener errorListener);
    public void setTheme(String siteId, String themeId, Listener listener, ErrorListener errorListener);
    public void getCurrentTheme(String siteId, Listener listener, ErrorListener errorListener);
    public void getStatsClicks(String siteId, String date, Listener listener, ErrorListener errorListener);
    public void getStatsGeoviews(String siteId, String date, Listener listener, ErrorListener errorListener);
    public void getStatsMostCommented(String siteId, Listener listener, ErrorListener errorListener);
    public void getStatsTopCommenters(String siteId, Listener listener, ErrorListener errorListener);
    public void getStatsReferrers(String siteId, String date, Listener listener, ErrorListener errorListener);
    public void getStatsSearchEngineTerms(String siteId, String date, Listener listener, ErrorListener errorListener);
    public void getStatsTagsAndCategories(String siteId, Listener listener, ErrorListener errorListener);
    public void getStatsTopAuthors(String siteId, Listener listener, ErrorListener errorListener);
    public void getStatsTopPosts(String siteId, String date, Listener listener, ErrorListener errorListener);
    public void getStatsVideoPlays(String siteId, Listener listener, ErrorListener errorListener);
    public void getStatsSummary(String siteId, Listener listener, ErrorListener errorListener);
    public void getStatsVideoSummary(String siteId, Listener listener, ErrorListener errorListener);
    public void getStatsBarChartData(String siteId, StatsBarChartUnit statsBarChartUnit, int quantity,
                                     Listener listener, ErrorListener errorListener);
    public void getXL(String path, Map<String, String> params, final Listener listener,
                      final ErrorListener errorListener);
    public void get(String path, Listener listener, ErrorListener errorListener);
    public void get(String path, Map<String, String> params, RetryPolicy retryPolicy, Listener listener,
                    ErrorListener errorListener);
    public void post(String path, Listener listener, ErrorListener errorListener);
    public void post(final String path, Map<String, String> params, RetryPolicy retryPolicy, Listener listener,
                     ErrorListener errorListener);
}

