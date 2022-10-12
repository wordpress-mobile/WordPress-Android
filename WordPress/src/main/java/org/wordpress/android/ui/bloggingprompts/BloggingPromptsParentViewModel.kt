package org.wordpress.android.ui.bloggingprompts

import androidx.lifecycle.ViewModel

class BloggingPromptsParentViewModel : ViewModel() {
    fun onOpen() = Unit

    @Suppress("UnusedPrivateMember")
    fun onSectionSelected(promptSection: PromptSection) = Unit
}
