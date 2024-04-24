package org.wordpress.android.viewmodel.pages

import org.wordpress.android.ui.utils.UiString

data class PageUploadErrorWrapper(val isError: Boolean, val errorMessage: UiString?, val retry: Boolean = true)
