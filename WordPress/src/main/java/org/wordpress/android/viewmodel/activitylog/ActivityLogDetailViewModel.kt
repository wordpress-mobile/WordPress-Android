package org.wordpress.android.viewmodel.activitylog

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel.ActivityActor
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.dashboard.CardsStore
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailModel
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailNavigationEvents
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailNavigationEvents.ShowDocumentationPage
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.ACTIVITY_LOG
import org.wordpress.android.util.extensions.toFormattedDateString
import org.wordpress.android.util.extensions.toFormattedTimeString
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

const val ACTIVITY_LOG_ID_KEY: String = "activity_log_id_key"
const val ACTIVITY_LOG_ARE_BUTTONS_VISIBLE_KEY: String = "activity_log_are_buttons_visible_key"
const val ACTIVITY_LOG_IS_RESTORE_HIDDEN_KEY: String = "activity_log_is_restore_hidden_key"
const val ACTIVITY_LOG_IS_DASHBOARD_CARD_ENTRY_KEY: String = "activity_log_is_dashboard_card_entry_key"

@HiltViewModel
class ActivityLogDetailViewModel @Inject constructor(
    val dispatcher: Dispatcher,
    private val activityLogStore: ActivityLogStore,
    private val resourceProvider: ResourceProvider,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val cardsStore: CardsStore,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    lateinit var site: SiteModel
    lateinit var activityLogId: String
    var areButtonsVisible = false
    var isRestoreHidden = false
    var isDashboardCardEntry = false

    private val _navigationEvents = MutableLiveData<Event<ActivityLogDetailNavigationEvents>>()
    val navigationEvents: LiveData<Event<ActivityLogDetailNavigationEvents>>
        get() = _navigationEvents

    private val _handleFormattableRangeClick = SingleLiveEvent<FormattableRange>()
    val handleFormattableRangeClick: LiveData<FormattableRange>
        get() = _handleFormattableRangeClick

    private val _item = MutableLiveData<ActivityLogDetailModel?>()
    val activityLogItem: LiveData<ActivityLogDetailModel?>
        get() = _item

    private val _restoreVisible = MutableLiveData<Boolean>()
    val restoreVisible: LiveData<Boolean>
        get() = _restoreVisible

    private val _downloadBackupVisible = MutableLiveData<Boolean>()
    val downloadBackupVisible: LiveData<Boolean>
        get() = _downloadBackupVisible

    private val _multisiteVisible = MutableLiveData<Pair<Boolean, SpannableString?>>()
    val multisiteVisible: LiveData<Pair<Boolean, SpannableString?>>
        get() = _multisiteVisible

    private val _showJetpackPoweredBottomSheet = MutableLiveData<Event<Boolean>>()
    val showJetpackPoweredBottomSheet: LiveData<Event<Boolean>> = _showJetpackPoweredBottomSheet

    fun start(
        site: SiteModel,
        activityLogId: String,
        areButtonsVisible: Boolean,
        isRestoreHidden: Boolean,
        isDashboardCardEntry: Boolean
    ) {
        this.site = site
        this.activityLogId = activityLogId
        this.areButtonsVisible = areButtonsVisible
        this.isRestoreHidden = isRestoreHidden
        this.isDashboardCardEntry = isDashboardCardEntry

        _restoreVisible.value = areButtonsVisible && !isRestoreHidden
        _downloadBackupVisible.value = areButtonsVisible
        _multisiteVisible.value = if (isRestoreHidden) Pair(true, getMultisiteMessage()) else Pair(false, null)

        activityLogId.takeIf { it != _item.value?.activityID }?.let {
            findAndPostActivityLogItemDetail()
        }
    }


    private fun findAndPostActivityLogItemDetail() {
        activityLogStore
            .getActivityLogForSite(site)
            .find { it.activityID == activityLogId }
            ?.toActivityLogDetailModel()
            ?.let {
                _item.value = it
            }?: findAndPostActivityLogItemDetailViaDashboardCardsIfNeeded()
    }

    private fun findAndPostActivityLogItemDetailViaDashboardCardsIfNeeded() {
        if (!isDashboardCardEntry) {
            _item.value = null
            return
        }

        launch(bgDispatcher) {
            cardsStore.getCards(site, listOf(CardModel.Type.ACTIVITY))
                .map { it.model }
                .collect { result ->
                    _item.postValue(
                        result.takeIf { !it.isNullOrEmpty() }
                            ?.let { activityCardModelList ->
                                (activityCardModelList[0] as CardModel.ActivityCardModel)
                                    .activities
                                    .find { it.activityID == activityLogId }
                                    ?.toActivityLogDetailModel()
                            })
                }
        }
    }

    private fun ActivityLogModel.toActivityLogDetailModel() =
        ActivityLogDetailModel(
            activityID = activityID,
            rewindId = rewindID,
            actorIconUrl = actor?.avatarURL,
            showJetpackIcon = actor?.showJetpackIcon(),
            isRewindButtonVisible = rewindable ?: false,
            actorName = actor?.displayName,
            actorRole = actor?.role,
            content = content,
            summary = summary,
            createdDate = published.toFormattedDateString(),
            createdTime = published.toFormattedTimeString()
        )

    private fun getMultisiteMessage(): SpannableString {
        val clickableText = resourceProvider.getString(R.string.activity_log_visit_our_documentation_page)
        val multisiteMessage = htmlMessageUtils.getHtmlMessageFromStringFormatResId(
            R.string.activity_log_multisite_message,
            clickableText
        )
        return constructMultisiteSpan(multisiteMessage, clickableText)
    }

    private fun constructMultisiteSpan(
        multisiteMessage: CharSequence,
        clickableText: String
    ): SpannableString {
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                _navigationEvents.postValue(Event(ShowDocumentationPage()))
            }
        }
        val clickableStartIndex = multisiteMessage.indexOf(clickableText)
        val clickableEndIndex = clickableStartIndex + clickableText.length
        val multisiteSpan = SpannableString(multisiteMessage)
        multisiteSpan.setSpan(clickableSpan, clickableStartIndex, clickableEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return multisiteSpan
    }

    fun showJetpackPoweredBottomSheet() {
        _showJetpackPoweredBottomSheet.value = Event(true)
    }

    fun onRangeClicked(range: FormattableRange) {
        _handleFormattableRangeClick.value = range
    }

    fun onRestoreClicked(model: ActivityLogDetailModel) {
        if (model.rewindId != null) {
            _navigationEvents.value = Event(ActivityLogDetailNavigationEvents.ShowRestore(model))
        } else {
            AppLog.e(ACTIVITY_LOG, "Trying to restore activity without rewind ID")
        }
    }

    fun onDownloadBackupClicked(model: ActivityLogDetailModel) {
        if (model.rewindId != null) {
            _navigationEvents.value = Event(ActivityLogDetailNavigationEvents.ShowBackupDownload(model))
        } else {
            AppLog.e(ACTIVITY_LOG, "Trying to download backup activity without rewind ID")
        }
    }

    private fun ActivityActor.showJetpackIcon(): Boolean {
        return displayName == "Jetpack" && type == "Application" ||
                displayName == "Happiness Engineer" && type == "Happiness Engineer"
    }
}
