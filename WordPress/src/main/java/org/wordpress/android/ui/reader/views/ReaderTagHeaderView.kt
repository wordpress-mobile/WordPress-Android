package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isGone
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderTagHeaderViewBinding
import org.wordpress.android.ui.reader.views.ReaderTagHeaderViewUiState.ReaderTagHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.LocaleProvider
import javax.inject.Inject

/**
 * topmost view in post adapter when showing tag preview - displays tag name and follow button
 */
class ReaderTagHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var binding: ReaderTagHeaderViewBinding

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var localeProvider: LocaleProvider

    private var onFollowBtnClicked: (() -> Unit)? = null

    init {
        (context.applicationContext as WordPress).component().inject(this)
        binding =
            ReaderTagHeaderViewBinding.inflate(LayoutInflater.from(context), this, true)
        binding.followContainer.followButton.setOnClickListener { onFollowBtnClicked?.invoke() }
    }

    fun updateUi(uiState: ReaderTagHeaderUiState) = with(binding) {
        binding.followContainer.textBlogFollowCount.isGone = true
        // creative-writing -> Creative Writing
        textTag.text = uiState.title
            .split("-")
            .joinToString(separator = " ") {
                it.replaceFirstChar { it.titlecase(localeProvider.getAppLocale()) }
            }
        with(uiState.followButtonUiState) {
            val followButton = binding.followContainer.followButton
            followButton.setIsFollowed(isFollowed)
            followButton.isEnabled = isEnabled
            onFollowBtnClicked = onFollowButtonClicked
        }
    }
}
