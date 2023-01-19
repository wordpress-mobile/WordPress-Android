package org.wordpress.android.util

import org.wordpress.android.WordPress
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class JetpackMigrationLanguageUtil @Inject constructor(
    private val contextProvider: ContextProvider,
) {
    fun applyLanguage(languageCode: String) {
        if (languageCode.isEmpty()) return

        LocaleManager.setNewLocale(WordPress.getContext(), languageCode)
        WordPress.updateContextLocale()
        contextProvider.refreshContext()
    }
}
