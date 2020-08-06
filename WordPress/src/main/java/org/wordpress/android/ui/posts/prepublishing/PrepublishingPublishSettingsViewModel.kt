package org.wordpress.android.ui.posts.prepublishing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostSettingsUtils
import org.wordpress.android.ui.posts.PublishSettingsViewModel
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class PrepublishingPublishSettingsViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    postSettingsUtils: PostSettingsUtils,
    localeManagerWrapper: LocaleManagerWrapper,
    postSchedulingNotificationStore: PostSchedulingNotificationStore,
    siteStore: SiteStore
) : PublishSettingsViewModel(
        resourceProvider,
        postSettingsUtils,
        localeManagerWrapper,
        postSchedulingNotificationStore,
        siteStore
) {
    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>> = _navigateToHomeScreen

    private val _updateToolbarTitle = MutableLiveData<UiString>()
    val updateToolbarTitle: LiveData<UiString> = _updateToolbarTitle

    override fun start(postRepository: EditPostRepository?) {
        super.start(postRepository)
        setToolbarTitle()
    }

    private fun setToolbarTitle() {
        _updateToolbarTitle.postValue(UiStringRes(R.string.prepublishing_nudges_toolbar_title_publish))
    }

    fun onBackButtonClicked() {
        _navigateToHomeScreen.postValue(Event(Unit))
    }
}
