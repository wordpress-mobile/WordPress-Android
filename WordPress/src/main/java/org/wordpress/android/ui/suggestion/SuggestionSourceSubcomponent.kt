package org.wordpress.android.ui.suggestion

import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.model.SiteModel

@Subcomponent
interface SuggestionSourceSubcomponent {
    fun userSuggestionSource(): UserSuggestionSource
    fun xPostSuggestionSource(): XPostsSuggestionSource

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance site: SiteModel
        ): SuggestionSourceSubcomponent
    }

    @InstallIn(SingletonComponent::class)
    @Module(subcomponents = [SuggestionSourceSubcomponent::class])
    interface SuggestionSourceModule
}
