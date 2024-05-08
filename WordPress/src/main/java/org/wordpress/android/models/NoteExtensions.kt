@file:JvmName("NoteExtensions")
package org.wordpress.android.models


val Note.type
    get() = NoteType.from(rawType)

fun Note.isAchievement() = AchievementType.isAchievementType(rawType)

sealed class Notification {
    data class PostLike(val url: String, val title: String): Notification()
    data object NewPost: Notification()
    data object Comment: Notification()
    data object Unknown: Notification()

    companion object {
        fun from(rawNote: Note) = when(rawNote.type) {
            NoteType.PostLike -> PostLike(url = rawNote.url, title = rawNote.title)
            NoteType.NewPost -> NewPost
            NoteType.Comment -> Comment
            else -> Unknown
        }
    }
}
enum class NoteType(val rawType: String) {
    Follow(Note.NOTE_FOLLOW_TYPE),
    PostLike(Note.NOTE_LIKE_TYPE),
    Comment(Note.NOTE_COMMENT_TYPE),
    Matcher(Note.NOTE_MATCHER_TYPE),
    CommentLike(Note.NOTE_COMMENT_LIKE_TYPE),
    NewPost(Note.NOTE_NEW_POST_TYPE),
    ViewMilestone(Note.NOTE_VIEW_MILESTONE),
    Unknown(Note.NOTE_UNKNOWN_TYPE);

    companion object {
        private val map = entries.associateBy(NoteType::rawType)
        fun from(rawType: String) = map[rawType] ?: Unknown
    }
}

enum class AchievementType(val rawType: String) {
    UserGoalMet("user_goal_met"),
    AutomatticianAchievement("automattician_achievement"),
    AchieveBurritoFriday("achieve_burrito_friday"),
    AchieveDailyStreak("achieve_daily_streak"),
    FollowedMilestoneAchievement("followed_milestone_achievement"),
    LikeMilestoneAchievement("like_milestone_achievement"),
    OnFireAchievement("on_fire_achievement"),
    PostMilestoneAchievement("post_milestone_achievement"),
    AchieveUserAnniversary("achieve_user_anniversary"),
    BestFollowedDayFeat("best_followed_day_feat"),
    BestLikedDayFeat("best_liked_day_feat"),
    NewTrafficSurge("new_traffic_surge"),
    PrivacyChange("privacy_change"),
    FreeTrialStart("free_trial_start"),
    FreeTrialNearEndNote("free_trial_near_end_note"),
    FreeTrialEnd("free_trial_end"),
    ViewMilestone(Note.NOTE_VIEW_MILESTONE),
    AscProfileRegenerationStarted("asc_profile_regeneration_started"),
    AscProfileRegenerationFinished("asc_profile_regeneration_finished");

    companion object {
        fun isAchievementType(rawType: String) = entries.any { it.rawType == rawType }
    }
}
