package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class EditPostPublishSettingsViewModel @Inject constructor(
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
)
