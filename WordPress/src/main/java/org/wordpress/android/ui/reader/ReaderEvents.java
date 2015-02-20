package org.wordpress.android.ui.reader;

import org.wordpress.android.util.DateTimeUtils;

import java.util.Date;

/**
 * Reader-related EventBus event classes
 */
public class ReaderEvents {
    public static class FollowedTagsChanged {}
    public static class RecommendedTagsChanged{}

    public static class FollowedBlogsChanged {}
    public static class RecommendedBlogsChanged {}

    public static class HasPurgedDatabase {}
    public static class HasPerformedInitialUpdate {}

    public static class UpdatedFollowedTagsAndBlogs {
        private Date mUpdateDate;
        public UpdatedFollowedTagsAndBlogs() {
            mUpdateDate = new Date();
        }
        public int minutesSinceLastUpdate() {
            return DateTimeUtils.minutesBetween(mUpdateDate, new Date());
        }
    }
}
