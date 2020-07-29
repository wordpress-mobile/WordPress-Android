package org.wordpress.android.ui.posts;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PostEditorAnalyticsSession implements Serializable {
    private static final String KEY_BLOG_TYPE = "blog_type";
    private static final String KEY_CONTENT_TYPE = "content_type";
    private static final String KEY_EDITOR = "editor";
    private static final String KEY_HAS_UNSUPPORTED_BLOCKS = "has_unsupported_blocks";
    private static final String KEY_UNSUPPORTED_BLOCKS = "unsupported_blocks";
    private static final String KEY_POST_TYPE = "post_type";
    private static final String KEY_OUTCOME = "outcome";
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_STARTUP_TIME = "startup_time_ms";
    private static final String KEY_TEMPLATE = "template";

    private String mSessionId = UUID.randomUUID().toString();
    private String mPostType;
    private String mBlogType;
    private String mContentType;
    private boolean mStarted = false;
    private Editor mCurrentEditor;
    private boolean mHasUnsupportedBlocks = false;
    private Outcome mOutcome = null;
    private String mTemplate;
    private boolean mHWAccOff = false;
    private long mStartTime = System.currentTimeMillis();

    public enum Editor {
        GUTENBERG,
        CLASSIC,
        HTML,
        WP_STORIES_CREATOR
    }

    public enum Outcome {
        CANCEL,
        DISCARD,    // not used in WPAndroid, but kept for parity with iOS
                    // see https://github.com/wordpress-mobile/gutenberg-mobile/issues/556#issuecomment-462678807
        SAVE,
        PUBLISH
    }

    PostEditorAnalyticsSession(Editor editor, PostImmutableModel post, SiteModel site, boolean isNewPost) {
        // fill in which the current Editor is
        mCurrentEditor = editor;

        // fill in mPostType
        if (post.isPage()) {
            mPostType = "page";
        } else {
            mPostType = "post";
        }

        // fill in mBlogType
        if (site.isWPCom()) {
            mBlogType = "wpcom";
        } else if (site.isJetpackConnected()) {
            mBlogType = "jetpack";
        } else {
            mBlogType = "core";
        }

        // fill in mContentType
        String postContent = post.getContent();
        if (isNewPost) {
            mContentType = "new";
        } else if (PostUtils.contentContainsGutenbergBlocks(postContent)) {
            mContentType = SiteUtils.GB_EDITOR_NAME;
        } else {
            mContentType = "classic";
        }

        mHWAccOff = AppPrefs.isPostWithHWAccelerationOff(site.getId(), post.getId());
    }

    public static PostEditorAnalyticsSession getNewPostEditorAnalyticsSession(
            Editor editor, PostImmutableModel post, SiteModel site, boolean isNewPost) {
        return new PostEditorAnalyticsSession(editor, post, site, isNewPost);
    }

    public void start(ArrayList<Object> unsupportedBlocksList) {
        if (!mStarted) {
            mHasUnsupportedBlocks = unsupportedBlocksList != null && unsupportedBlocksList.size() > 0;
            Map<String, Object> properties = getCommonProperties();
            properties.put(KEY_UNSUPPORTED_BLOCKS,
                    unsupportedBlocksList != null ? unsupportedBlocksList : new ArrayList<>());
            // Note that start time only counts when the analytics session was created and not when the editor
            // activity started. We are mostly interested in measuring the loading times for the block editor,
            // where the main bottleneck seems to be initializing React Native and doing the initial load of Gutenberg.
            //
            // Measuring the full editor activity initialization would be more accurate, but we don't expect the
            // difference to be significant enough, and doing that would add more complexity to how we are initializing
            // the session.
            properties.put(KEY_STARTUP_TIME, System.currentTimeMillis() - mStartTime);
            AnalyticsTracker.track(Stat.EDITOR_SESSION_START, properties);
            mStarted = true;
        } else {
            AppLog.w(T.EDITOR, "An editor session cannot be attempted to be started more than once, "
                               + "unless it's due to rotation or Editor switch");
        }
    }

    public void switchEditor(Editor editor) {
        mCurrentEditor = editor;
        Map<String, Object> properties = getCommonProperties();
        AnalyticsTracker.track(Stat.EDITOR_SESSION_SWITCH_EDITOR, properties);
    }

    public void setOutcome(Outcome newOutcome) {
        // on WPiOS we're using `forceOutcome` and some exception logic to allow / not allow forcing an outcome,
        // but in WPAndroid we slightly changed the interface as we're not
        // really `forcing` but just `setting` the outcome on specific places where we know what happened
        // or is going to happen.
        // Session ending will only properly end the session with the outcome already being set.
        mOutcome = newOutcome;
    }

    public void previewTemplate(String template) {
        final Map<String, Object> properties = getCommonProperties();
        properties.put(KEY_TEMPLATE, template);
        AnalyticsTracker.track(Stat.EDITOR_SESSION_TEMPLATE_PREVIEW, properties);
    }

    public void applyTemplate(String template) {
        mTemplate = template;
        final Map<String, Object> properties = getCommonProperties();
        properties.put(KEY_TEMPLATE, template);
        AnalyticsTracker.track(Stat.EDITOR_SESSION_TEMPLATE_APPLY, properties);
    }

    public void end() {
        // don't try to send an "end" event if the session wasn't started in the first place
        if (mStarted) {
            if (mOutcome == null) {
                // outcome should have already been set with setOutcome at specific user actions
                // if outcome is still unknown, chances are Activity was killed / user cancelled so, set to CANCEL.
                mOutcome = Outcome.CANCEL;
            }
            Map<String, Object> properties = getCommonProperties();
            properties.put(KEY_OUTCOME, mOutcome.toString().toLowerCase(Locale.ROOT));
            AnalyticsTracker.track(Stat.EDITOR_SESSION_END, properties);
        } else {
            AppLog.e(T.EDITOR, "A non-started editor session cannot be attempted to be ended");
        }
    }

    private Map<String, Object> getCommonProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(KEY_EDITOR, mCurrentEditor.toString().toLowerCase(Locale.ROOT));
        properties.put(KEY_CONTENT_TYPE, mContentType);
        properties.put(KEY_POST_TYPE, mPostType);
        properties.put(KEY_BLOG_TYPE, mBlogType);
        properties.put(KEY_SESSION_ID, mSessionId);
        properties.put(KEY_HAS_UNSUPPORTED_BLOCKS, mHasUnsupportedBlocks ? "1" : "0");
        properties.put(AnalyticsUtils.EDITOR_HAS_HW_ACCELERATION_DISABLED_KEY, mHWAccOff ? "1" : "0");

        if (mTemplate != null) {
            properties.put(KEY_TEMPLATE, mTemplate);
        }

        return properties;
    }
}
