package org.wordpress.android.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.models.StatsBarChartData;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.models.StatsVideoSummary;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.stats.StatsBarChartUnit;

/**
 * A utility class to help with date parsing and saving summaries in stats
 */
public class StatUtils {

    public static final String STATS_SUMMARY_UPDATED = "STATS_SUMMARY_UPDATED";
    public static final String STATS_SUMMARY_UPDATED_EXTRA = "STATS_SUMMARY_UPDATED_EXTRA";
    
    private static final String STAT_SUMMARY = "StatSummary_";
    private static final String STAT_VIDEO_SUMMARY = "StatVideoSummary_";
    private static final long ONE_DAY = 24 * 60 * 60 * 1000;

    
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

    public static String msToString(long ms, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(ms));
    }
    
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }
    
    public static String getYesterdaysDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date(getCurrentDateMs() - ONE_DAY));
    }
    
    public static long getCurrentDateMs() {
        return toMs(getCurrentDate());
    }
    
    public static String parseDate(String timestamp, String fromFormat, String toFormat) {
        SimpleDateFormat from = new SimpleDateFormat(fromFormat);
        SimpleDateFormat to = new SimpleDateFormat(toFormat);
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
            JSONObject statsObject = stat.getJSONObject("stats");
            statsObject.put("date", getCurrentDate());
            FileOutputStream fos = WordPress.getContext().openFileOutput(STAT_SUMMARY + blogId, Context.MODE_PRIVATE);
            fos.write(statsObject.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        saveGraphData(blogId, stat);
    }
    
    private static void saveGraphData(String blogId, JSONObject stat) {
        try {
            JSONArray data = stat.getJSONObject("visits").getJSONArray("data");
            Uri uri = StatsContentProvider.STATS_BAR_CHART_DATA_URI;
            Context context = WordPress.getContext();
            
            int length = data.length();
            
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            
            if (length > 0) {
                ContentProviderOperation op = ContentProviderOperation.newDelete(uri).withSelection("blogId=? AND unit=?", new String[] { blogId, StatsBarChartUnit.DAY.name() }).build();
                operations.add(op);
            }
            
            for (int i = 0; i < length; i++) {
                StatsBarChartData item = new StatsBarChartData(blogId, StatsBarChartUnit.DAY, data.getJSONArray(i));
                ContentValues values = StatsBarChartDataTable.getContentValues(item);
                
                if (values != null) {
                    ContentProviderOperation op = ContentProviderOperation.newInsert(uri).withValues(values).build();
                    operations.add(op);
                }
            }
            
            ContentResolver resolver = context.getContentResolver();
            resolver.applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
            resolver.notifyChange(uri, null);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
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

            int bytesRead = fis.read(buffer); 
            while (bytesRead != -1) {
                fileContent.append(new String(buffer, 0, bytesRead, "ISO-8859-1"));
                bytesRead = fis.read(buffer);
            }
            
            Gson gson = new Gson();
            stat = gson.fromJson(fileContent.toString(), StatsSummary.class);
            
        } catch (FileNotFoundException e) {
            // stats haven't been downloaded yet
        } catch (IOException e) {
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
    
    public static void broadcastSummaryUpdated(StatsSummary stats) {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(WordPress.getContext());
        Intent intent = new Intent(STATS_SUMMARY_UPDATED);
        intent.putExtra(STATS_SUMMARY_UPDATED_EXTRA, stats);
        lbm.sendBroadcast(intent);
    }
    
}
