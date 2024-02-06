package org.wordpress.android.ui.mysite.cards.migration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import org.wordpress.android.R
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.publicdata.AppStatus
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class JpMigrationSuccessCardViewModelSlice @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val appStatus: AppStatus,
    private val wordPressPublicData: WordPressPublicData,
    private val contentMigrationAnalyticsTracker: ContentMigrationAnalyticsTracker,
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _uiModel = MutableLiveData<MySiteCardAndItem.Item.SingleActionCard?>()
    val uiModel: LiveData<MySiteCardAndItem.Item.SingleActionCard?> = _uiModel.distinctUntilChanged()

    fun buildCard() {
        val isJetpackApp = buildConfigWrapper.isJetpackApp
        val isMigrationCompleted = appPrefsWrapper.isJetpackMigrationCompleted()
        val isWordPressInstalled = appStatus.isAppInstalled(wordPressPublicData.currentPackageId())
        if (isJetpackApp && isMigrationCompleted && isWordPressInstalled) {
            _uiModel.postValue(
                MySiteCardAndItem.Item.SingleActionCard(
                    textResource = R.string.jp_migration_success_card_message,
                    imageResource = R.drawable.ic_wordpress_jetpack_appicon,
                    onActionClick = ::onPleaseDeleteWordPressAppCardClick
                )
            )
        } else { _uiModel.postValue(null) }
    }

    private fun onPleaseDeleteWordPressAppCardClick() {
        contentMigrationAnalyticsTracker.trackPleaseDeleteWordPressCardTapped()
        _onNavigation.value = Event(SiteNavigationAction.OpenJetpackMigrationDeleteWP)
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }
}
