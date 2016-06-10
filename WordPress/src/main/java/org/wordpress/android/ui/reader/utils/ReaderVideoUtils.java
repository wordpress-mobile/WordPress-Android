package org.wordpress.android.ui.reader.utils;

import android.net.Uri;
import android.text.TextUtils;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;

public class ReaderVideoUtils {
	private ReaderVideoUtils() {
		throw new AssertionError();
	}

    /*
     * returns the url to get the full-size (480x360) thumbnail url for the passed video
     * see http://www.reelseo.com/youtube-thumbnail-image/ for other sizes
     */
    public static String getYouTubeThumbnailUrl(final String videoUrl) {
        String videoId = getYouTubeVideoId(videoUrl);
        if (TextUtils.isEmpty(videoId))
            return "";
        // note that this *must* use https rather than http - ex: https://img.youtube.com/vi/ClbE019cLNI/0.jpg
        return "https://img.youtube.com/vi/" + videoId + "/0.jpg";
    }

	/*
	 * returns true if the passed url is a link to a YouTube video
	 */
	public static boolean isYouTubeVideoLink(final String link) {
		return (!TextUtils.isEmpty(getYouTubeVideoId(link)));
	}

	/*
	 * extract the video id from the passed YouTube url
     */
	private static String getYouTubeVideoId(final String link) {
		if (link==null)
			return "";

		Uri uri = Uri.parse(link);
		try {
			String host = uri.getHost();
			if (host==null)
				return "";

			// youtube.com links
			if (host.equals("youtube.com") || host.equals("www.youtube.com")) {
				// if link contains "watch" in the path, then the id is in the "v=" query param
				if (link.contains("watch"))
					return uri.getQueryParameter("v");
                // if the link contains "embed" in the path, then the id is the last path segment
                // ex: https://www.youtube.com/embed/fw3w68YrKwc?version=3&#038;rel=1&#038;
                if (link.contains("/embed/"))
                    return uri.getLastPathSegment();
				return "";
			}

			// youtu.be urls have the videoId as the path - ex:  http://youtu.be/pEnXclbO9jg
			if (host.equals("youtu.be")) {
				String path = uri.getPath();
				if (path==null)
					return "";
				// remove the leading slash
				return path.replace("/", "");
			}

			// YouTube mobile urls include video id in fragment, ex: http://m.youtube.com/?dc=organic&source=mog#/watch?v=t77Vlme_pf8
			if (host.equals("m.youtube.com")) {
				String fragment = uri.getFragment();
				if (fragment==null)
					return "";
				int index = fragment.lastIndexOf("v=");
				if (index!=-1)
					return fragment.substring(index+2, fragment.length());
			}

			return "";
		} catch (UnsupportedOperationException e) {
			AppLog.e(T.READER, e);
			return "";
		} catch (IndexOutOfBoundsException e) {
			// thrown by substring
            AppLog.e(T.READER, e);
			return "";
		}
	}

    /*
     * returns true if the passed url is a link to a Vimeo video
     */
	public static boolean isVimeoLink(final String link) {
		return (!TextUtils.isEmpty(getVimeoVideoId(link)));
	}

    /*
     * extract the video id from the passed Vimeo url
     * ex: http://player.vimeo.com/video/72386905 -> 72386905
     */
	private static String getVimeoVideoId(final String link) {
		if (link==null)
			return "";
        if (!link.contains("player.vimeo.com"))
            return "";

		Uri uri = Uri.parse(link);
		return uri.getLastPathSegment();
	}

    /*
     * unlike YouTube thumbnails, Vimeo thumbnails require network request
     */
    public static void requestVimeoThumbnail(final String videoUrl, final VideoThumbnailListener thumbListener) {
        // useless without a listener
        if (thumbListener==null)
            return;

        String id = getVimeoVideoId(videoUrl);
        if (TextUtils.isEmpty(id)) {
            thumbListener.onResponse(false, null);
            return;
        }

        Response.Listener<JSONArray> listener = new Response.Listener<JSONArray>() {
            public void onResponse(JSONArray response) {
                String thumbnailUrl = null;
                if (response!=null && response.length() > 0) {
                    JSONObject json = response.optJSONObject(0);
                    if (json!=null && json.has("thumbnail_large"))
                        thumbnailUrl = JSONUtils.getString(json, "thumbnail_large");
                }
                if (TextUtils.isEmpty(thumbnailUrl)) {
                    thumbListener.onResponse(false, null);
                } else {
                    thumbListener.onResponse(true, thumbnailUrl);
                }
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                thumbListener.onResponse(false, null);
            }
        };

        String url = "http://vimeo.com/api/v2/video/" + id + ".json";
        JsonArrayRequest request = new JsonArrayRequest(url, listener, errorListener);

        WordPress.requestQueue.add(request);
    }

    public interface VideoThumbnailListener {
        void onResponse(boolean successful, String thumbnailUrl);
    }
}
