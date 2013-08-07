package org.wordpress.android.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.StatsCommentsSummary;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.models.StatsTotalsFollowersAndShares;
import org.wordpress.android.models.StatsVideoSummary;
import org.wordpress.android.models.StatsVisitorsAndViewsSummary;

public class StatUtils {
    
    private static final String STAT_SUMMARY = "StatSummary_";
    private static final String STAT_VISITORS_AND_VIEWS_SUMMARY = "StatVisitorsAndViewsSummary_";
    private static final String STAT_VIDEO_SUMMARY = "StatVideoSummary_";
    private static final String STAT_COMMENT_SUMMARY = "StatCommentSummary_";
    private static final String STAT_TOTALS_FOLLOWERS_SHARES = "StatTotalsFollowersShares_";

    /** Converts date in the form of 2013-07-18 to ms **/
    @SuppressLint("SimpleDateFormat")
	public static long toMs(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(date).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean isDayOld(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            long time = sdf.parse(date).getTime();
            long currentTime = System.currentTimeMillis();
            
            return (currentTime - time) >= 24 * 60 * 60 * 1000;
            
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }
    
    /** Parses date into form MMMMM d, yyyy (e.g. July 13, 2013) from timestamp of the form yyyy-MM-dd (e.g. 2013-07-13) **/
    public static String parseDate(String timestamp) {
        SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat to = new SimpleDateFormat("MMMMM d, yyyy");
        try {
            Date date = from.parse(timestamp);
            return to.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    public static void saveSummary(String blogId, JSONObject stat) {
        try {
            stat.put("date", getCurrentDate());
            FileOutputStream fos = WordPress.getContext().openFileOutput(STAT_SUMMARY + blogId, Context.MODE_PRIVATE);
            fos.write(stat.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public static void deleteSummary(String blogId) {
        WordPress.getContext().deleteFile(STAT_SUMMARY + blogId);
    }
    
    public static StatsSummary getSummary(String blogId) {
        StatsSummary stat = null;
        try {
            FileInputStream fis = WordPress.getContext().openFileInput(STAT_SUMMARY + blogId);
            StringBuffer fileContent = new StringBuffer("");

            byte[] buffer = new byte[1024];

            while (fis.read(buffer) != -1) {
                fileContent.append(new String(buffer));
            }
            
            JSONObject object = new JSONObject(fileContent.toString());
            
            int views = object.getInt("views");
            int comments = object.getInt("comments");
            int favorites = object.getInt("favorites");
            int reblogs = object.getInt("reblogs");
            String date = object.getString("date");
            
            stat = new StatsSummary(views, comments, favorites, reblogs, date);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return stat;
    }

    public static void saveVisitorsAndViewsSummary(String blogId, JSONObject stat) {
        try {
            stat.put("date", getCurrentDate());
            FileOutputStream fos = WordPress.getContext().openFileOutput(STAT_VISITORS_AND_VIEWS_SUMMARY + blogId, Context.MODE_PRIVATE);
            fos.write(stat.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }        
    }

    public static void deleteVisitorsAndViewsSummary(String blogId) {
        WordPress.getContext().deleteFile(STAT_VISITORS_AND_VIEWS_SUMMARY + blogId);
    }
    
    public static StatsVisitorsAndViewsSummary getVisitorsAndViewsSummary(String blogId) {
        StatsVisitorsAndViewsSummary stat = null;
        try {
            FileInputStream fis = WordPress.getContext().openFileInput(STAT_VISITORS_AND_VIEWS_SUMMARY + blogId);
            StringBuffer fileContent = new StringBuffer("");

            byte[] buffer = new byte[1024];

            while (fis.read(buffer) != -1) {
                fileContent.append(new String(buffer));
            }
            
            JSONObject object = new JSONObject(fileContent.toString());
            
            int visitorsToday = object.getInt("visitors_today");
            int viewsToday = object.getInt("views_today");
            int visitorsBestEver = object.getInt("visitors_best_ever");
            int viewsAllTime = object.getInt("views_all_time");
            int commentsAllTime = object.getInt("comments_all_time");
            String date = object.getString("date");
            
            stat = new StatsVisitorsAndViewsSummary(visitorsToday, viewsToday, visitorsBestEver, viewsAllTime, commentsAllTime, date);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return stat;
    }

    public static void saveVideoSummary(String blogId, JSONObject stat) {
        try {
            stat.put("date", getCurrentDate());
            FileOutputStream fos = WordPress.getContext().openFileOutput(STAT_VIDEO_SUMMARY + blogId, Context.MODE_PRIVATE);
            fos.write(stat.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }        
    }

    public static void deleteVideoSummary(String blogId) {
        WordPress.getContext().deleteFile(STAT_VIDEO_SUMMARY + blogId);
    }
    
    public static StatsVideoSummary getVideoSummary(String blogId) {
        StatsVideoSummary stat = null;
        try {
            FileInputStream fis = WordPress.getContext().openFileInput(STAT_VIDEO_SUMMARY + blogId);
            StringBuffer fileContent = new StringBuffer("");

            byte[] buffer = new byte[1024];

            while (fis.read(buffer) != -1) {
                fileContent.append(new String(buffer));
            }
            
            JSONObject object = new JSONObject(fileContent.toString());
            
            String timeframe = object.getString("timeframe");
            int plays = object.getInt("plays");
            int impressions = object.getInt("impressions");
            int minutes = object.getInt("minutes");
            String bandwidth = object.getString("bandwidth");
            String date = object.getString("date");
            
            stat = new StatsVideoSummary(timeframe, plays, impressions, minutes, bandwidth, date);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return stat;
    }


    public static void saveCommentsSummary(String blogId, JSONObject stat) {
        try {
            stat.put("date", getCurrentDate());
            FileOutputStream fos = WordPress.getContext().openFileOutput(STAT_COMMENT_SUMMARY + blogId, Context.MODE_PRIVATE);
            fos.write(stat.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }        
    }

    public static void deleteCommentsSummary(String blogId) {
        WordPress.getContext().deleteFile(STAT_COMMENT_SUMMARY + blogId);
    }
    
    public static StatsCommentsSummary getCommentsSummary(String blogId) {
        StatsCommentsSummary stat = null;
        try {
            FileInputStream fis = WordPress.getContext().openFileInput(STAT_COMMENT_SUMMARY + blogId);
            StringBuffer fileContent = new StringBuffer("");

            byte[] buffer = new byte[1024];

            while (fis.read(buffer) != -1) {
                fileContent.append(new String(buffer));
            }
            
            JSONObject object = new JSONObject(fileContent.toString());
            
            int commentsPerMonth = object.getInt("comments_per_month");
            int commentsTotal = object.getInt("comments_total");
            String recentMostActivePost = object.getString("recent_most_active_post");
            String recentMostActivePostUrl = object.getString("recent_most_active_post_url");
            String date = object.getString("date");

            String recentMostActiveDay = parseDate(object.getString("recent_most_active_date"));
            String recentMostActiveTime = object.getString("recent_most_active_time");
            
            stat = new StatsCommentsSummary(commentsPerMonth, commentsTotal, recentMostActiveDay, recentMostActiveTime, recentMostActivePost, recentMostActivePostUrl, date);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return stat;
    }

    public static void saveTotalsFollowersShares(String blogId, JSONObject stat) {
        try {
            stat.put("date", getCurrentDate());
            FileOutputStream fos = WordPress.getContext().openFileOutput(STAT_TOTALS_FOLLOWERS_SHARES + blogId, Context.MODE_PRIVATE);
            fos.write(stat.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }        
    }

    public static void deleteTotalsFollowersShares(String blogId) {
        WordPress.getContext().deleteFile(STAT_TOTALS_FOLLOWERS_SHARES + blogId);
    }
    
    public static StatsTotalsFollowersAndShares getTotalsFollowersShares(String blogId) {
        StatsTotalsFollowersAndShares stat = null;
        try {
            FileInputStream fis = WordPress.getContext().openFileInput(STAT_TOTALS_FOLLOWERS_SHARES + blogId);
            StringBuffer fileContent = new StringBuffer("");

            byte[] buffer = new byte[1024];

            while (fis.read(buffer) != -1) {
                fileContent.append(new String(buffer));
            }
            
            JSONObject object = new JSONObject(fileContent.toString());
            
            int posts = object.getInt("posts");
            int categories = object.getInt("categories");
            int tags = object.getInt("tags");
            int followers = object.getInt("followers");
            int comments = object.getInt("comments");
            int shares = object.getInt("shares");
            String date = object.getString("date");
            
            stat = new StatsTotalsFollowersAndShares(posts, categories, tags, followers, comments, shares, date);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return stat;
    }

    
    
}
