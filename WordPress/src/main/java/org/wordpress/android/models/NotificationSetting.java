package org.wordpress.android.models;

import org.json.JSONObject;
import org.wordpress.android.util.JSONUtils;

// Maps to notification settings returned from the /me/notifications/settings endpoint on wp.com
public class NotificationSetting {

    public enum StreamType {
        TIMELINE,
        EMAIL,
        DEVICE
    }

    // Site notification settings
    public class Site {
        long siteId;
        StreamType streamType;
        boolean newComment;
        boolean commentLike;
        boolean postLike;
        boolean follow;
        boolean achievement;
        boolean mentions;

        public void Site(long siteId, StreamType streamType, JSONObject settings) {
            this.siteId = siteId;
            this.streamType = streamType;
            this.newComment = JSONUtils.getBool(settings, "new-comment");
            this.commentLike = JSONUtils.getBool(settings, "comment-like");
            this.postLike = JSONUtils.getBool(settings, "post-like");
            this.follow = JSONUtils.getBool(settings, "follow");
            this.achievement = JSONUtils.getBool(settings, "achievement");
            this.mentions = JSONUtils.getBool(settings, "mentions");
        }
    }

    // Other notification settings, not tied to a site (aka notifications from 3rd party sites)
    public class Other {
        StreamType streamType;
        boolean commentLike;
        boolean commentReply;

        public void Other(StreamType streamType, JSONObject settings) {
            this.streamType = streamType;
            this.commentLike = JSONUtils.getBool(settings, "comment-like");
            this.commentReply = JSONUtils.getBool(settings, "comment-reply");
        }
    }

    // WordPress.com email settings
    public class WordPressCom {
        boolean news;
        boolean recommendation;
        boolean promotion;
        boolean digest;

        public void WordPressCom(JSONObject settings) {
            this.news = JSONUtils.getBool(settings, "news");
            this.recommendation = JSONUtils.getBool(settings, "recommendation");
            this.promotion = JSONUtils.getBool(settings, "promotion");
            this.digest = JSONUtils.getBool(settings, "digest");
        }
    }
}
