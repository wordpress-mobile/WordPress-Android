package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.Person
import org.wordpress.android.ui.people.utils.PeopleUtils.FetchUsersCallback
import org.wordpress.android.ui.people.utils.PeopleUtilsWrapper
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class EditPostPublishSettingsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    postSettingsUtils: PostSettingsUtils,
    private val peopleUtilsWrapper: PeopleUtilsWrapper,
    localeManagerWrapper: LocaleManagerWrapper,
    postSchedulingNotificationStore: PostSchedulingNotificationStore,
    private val siteStore: SiteStore
) : PublishSettingsViewModel(
        resourceProvider,
        postSettingsUtils,
        localeManagerWrapper,
        postSchedulingNotificationStore,
        siteStore
) {
    private val _authors = MutableLiveData<List<Person>>()
    val authors: LiveData<List<Person>> = _authors

    override fun start(postRepository: EditPostRepository?) {
        super.start(postRepository)
        postRepository?.let { fetchAuthors(it) }
    }

    private fun fetchAuthors(postRepository: EditPostRepository) {
        val site = siteStore.getSiteByLocalId(postRepository.localSiteId) ?: return

        peopleUtilsWrapper.fetchAuthors(site, object : FetchUsersCallback {
            override fun onSuccess(peopleList: List<Person>, isEndOfList: Boolean) {
                _authors.value = peopleList
            }

            override fun onError() {
                _onToast.postValue(Event(resourceProvider.getString(R.string.error_fetch_authors_list)))
            }
        })
    }
}
