@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.notifications.adapters

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.wordpress.android.WordPress
import org.wordpress.android.models.Note
import org.wordpress.android.ui.comments.CommentDetailFragment
import org.wordpress.android.ui.engagement.EngagedPeopleListFragment
import org.wordpress.android.ui.engagement.ListScenarioUtils
import org.wordpress.android.ui.notifications.NotificationsDetailListFragment
import org.wordpress.android.ui.notifications.NotificationsListFragment
import org.wordpress.android.ui.reader.ReaderPostDetailFragment
import org.wordpress.android.util.config.LikesEnhancementsFeatureConfig
import javax.inject.Inject

class NotificationDetailFragmentAdapter(
    private val activity: Activity,
    private val notes: List<Note>,
    fragmentManager: FragmentManager,
    lifeCycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifeCycle) {
    private val fragments = hashMapOf<Int, Fragment>()

    @Inject
    lateinit var listScenarioUtils: ListScenarioUtils

    @Inject
    lateinit var likesEnhancementsFeatureConfig: LikesEnhancementsFeatureConfig

    init {
        (activity.applicationContext as WordPress).component().inject(this)
    }
    override fun getItemCount(): Int = notes.size

    /**
     * Tries to pick the correct fragment detail type for a given note
     * Defaults to NotificationDetailListFragment
     */
    override fun createFragment(position: Int): Fragment {
        if (fragments.containsKey(position)) {
            return fragments[position]!!
        }

        val note = notes[position]
        val fragment: Fragment
        if (note.isCommentType) {
            // show comment detail for comment notifications
            val isInstantReply = activity.intent.getBooleanExtra(
                NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA,
                false
            )
            fragment = CommentDetailFragment.newInstance(
                note.id,
                activity.intent.getStringExtra(NotificationsListFragment.NOTE_PREFILLED_REPLY_EXTRA)
            )
            if (isInstantReply) {
                fragment.enableShouldFocusReplyField()
            }
        } else if (note.isAutomattcherType) {
            // show reader post detail for automattchers about posts - note that comment
            // automattchers are handled by note.isCommentType() above
            val isPost = note.siteId != 0 && note.postId != 0 && note.commentId == 0L
            fragment = if (isPost) {
                ReaderPostDetailFragment.newInstance(
                    note.siteId.toLong(),
                    note.postId
                        .toLong()
                )
            } else {
                NotificationsDetailListFragment.newInstance(note.id)
            }
        } else if (note.isNewPostType) {
            fragment = ReaderPostDetailFragment.newInstance(
                note.siteId.toLong(),
                note.postId
                    .toLong()
            )
        } else {
            fragment = if (likesEnhancementsFeatureConfig.isEnabled() && note.isLikeType) {
                EngagedPeopleListFragment.newInstance(
                    listScenarioUtils.mapLikeNoteToListScenario(note, activity)
                )
            } else {
                NotificationsDetailListFragment.newInstance(note.id)
            }
        }

        fragments[position] = fragment
        return fragment
    }

    fun getNoteByTabPosition(position: Int): Note? = notes.getOrNull(position)
    fun getNoteById(id: String): Note? = notes.firstOrNull { it.id == id }
}
