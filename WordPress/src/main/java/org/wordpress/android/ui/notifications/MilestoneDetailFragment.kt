package org.wordpress.android.ui.notifications

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.ListFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.models.Note
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.ViewPagerFragment.Companion.restoreOriginalViewId
import org.wordpress.android.ui.ViewPagerFragment.Companion.setUniqueIdToView
import org.wordpress.android.ui.notifications.adapters.NoteBlockAdapter
import org.wordpress.android.ui.notifications.blocks.MilestoneNoteBlock
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.NOTIFS
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject
import javax.inject.Named

class MilestoneDetailFragment : ListFragment(), NotificationFragment {
    private var restoredListPosition = 0
    private var notification: Note? = null
    private var rootLayout: LinearLayout? = null
    private var restoredNoteId: String? = null
    private var noteBlockAdapter: NoteBlockAdapter? = null

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var notificationsUtilsWrapper: NotificationsUtilsWrapper

    @Inject
    @Named(IO_THREAD)
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @Named(UI_THREAD)
    lateinit var mainDispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_NOTE_ID)) {
            restoredNoteId = savedInstanceState.getString(KEY_NOTE_ID)
            restoredListPosition = savedInstanceState.getInt(KEY_LIST_POSITION, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        notification?.let {
            outState.putString(KEY_NOTE_ID, it.id)
            outState.putInt(KEY_LIST_POSITION, listView.firstVisiblePosition)
        } ?: run {
            // This is done so the fragments pre-loaded by the view pager can store the already rescued restoredNoteId
            if (!TextUtils.isEmpty(restoredNoteId)) {
                outState.putString(KEY_NOTE_ID, restoredNoteId)
            }
        }

        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.notifications_fragment_detail_list, container, false)
        rootLayout = view.findViewById(R.id.notifications_list_root)
        return view
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityCreated(bundle: Bundle?) {
        super.onActivityCreated(bundle)
        val listView = listView
        listView.divider = null
        listView.dividerHeight = 0
        listView.setHeaderDividersEnabled(false)
        reloadNoteBlocks()
    }

    override fun onResume() {
        super.onResume()
        setUniqueIdToView(listView)
        if (activity is ScrollableViewInitializedListener) {
            (activity as ScrollableViewInitializedListener).onScrollableViewInitialized(listView.id)
        }

        // Set the note if we retrieved the noteId from savedInstanceState
        if (!TextUtils.isEmpty(restoredNoteId)) {
            setNote(restoredNoteId)
            reloadNoteBlocks()
            restoredNoteId = null
        }
        if (notification == null) {
            showErrorToastAndFinish()
        }
    }

    override fun onPause() {
        restoreOriginalViewId(listView)
        super.onPause()
    }

    private fun setNote(noteId: String?) {
        if (noteId == null) {
            showErrorToastAndFinish()
            return
        }
        val note: Note? = NotificationsTable.getNoteById(noteId)
        if (note == null) {
            showErrorToastAndFinish()
            return
        }
        notification = note
    }

    private fun showErrorToastAndFinish() {
        AppLog.e(NOTIFS, "Note could not be found.")
        activity?.let {
            ToastUtils.showToast(activity, R.string.error_notification_open)
            it.finish()
        }
    }

    private fun reloadNoteBlocks() {
        lifecycleScope.launch(ioDispatcher) {
            notification?.let { note ->
                val noteBlocks = noteBlocksLoader.loadNoteBlocks(note)
                withContext(mainDispatcher) {
                    noteBlocksLoader.handleNoteBlocks(noteBlocks)
                }
            }
        }
    }

    private val mOnNoteBlockTextClickListener = NoteBlockTextClickListener(this, notification)

    // Loop through the 'body' items in this note, and create blocks for each.
    private val noteBlocksLoader = object {
        private fun addNotesBlock(noteList: MutableList<MilestoneNoteBlock>, bodyArray: JSONArray) {
            var i = 0
            while (i < bodyArray.length()) {
                try {
                    val noteObject = notificationsUtilsWrapper
                        .mapJsonToFormattableContent(bodyArray.getJSONObject(i))

                    val noteBlock = MilestoneNoteBlock(
                        noteObject, imageManager, notificationsUtilsWrapper,
                        mOnNoteBlockTextClickListener
                    )
                    preloadImage(noteBlock)
                    noteList.add(noteBlock)
                } catch (e: JSONException) {
                    AppLog.e(NOTIFS, "Error parsing milestone note data.")
                }
                i++
            }
        }

        private fun preloadImage(noteBlock: MilestoneNoteBlock) {
            if (noteBlock.hasImageMediaItem()) {
                noteBlock.noteMediaItem?.url?.let {
                    imageManager.preload(requireContext(), it)
                }
            }
        }

        fun loadNoteBlocks(note: Note): List<MilestoneNoteBlock> {
            val bodyArray = note.body
            val noteList: MutableList<MilestoneNoteBlock> = ArrayList()

            if (bodyArray.length() > 0) {
                addNotesBlock(noteList, bodyArray)
            }
            return noteList
        }

        fun handleNoteBlocks(noteList: List<MilestoneNoteBlock>?) {
            if (!isAdded || noteList == null) {
                return
            }
            if (noteBlockAdapter == null) {
                noteBlockAdapter = NoteBlockAdapter(requireContext(), noteList)
                listAdapter = noteBlockAdapter
            } else {
                noteBlockAdapter?.setNoteList(noteList)
            }
            if (restoredListPosition > 0) {
                listView.setSelectionFromTop(restoredListPosition, 0)
                restoredListPosition = 0
            }
        }
    }

    companion object {
        private const val KEY_NOTE_ID = "noteId"
        private const val KEY_LIST_POSITION = "listPosition"

        @JvmStatic
        fun newInstance(noteId: String?): MilestoneDetailFragment {
            val fragment = MilestoneDetailFragment()
            fragment.setNote(noteId)
            return fragment
        }
    }
}
