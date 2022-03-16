package org.wordpress.android.ui.prefs.categories.detail

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class CategoryDetailViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    lateinit var siteModel: SiteModel

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    fun start(siteModel: SiteModel) {
        if (isStarted) return
        isStarted = true

        this.siteModel = siteModel
    }
}
