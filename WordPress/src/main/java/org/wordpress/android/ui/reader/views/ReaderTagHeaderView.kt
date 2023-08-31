package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.google.android.material.textview.MaterialTextView
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderTagHeaderViewBinding
import org.wordpress.android.databinding.ReaderTagHeaderViewNewBinding
import org.wordpress.android.ui.reader.views.ReaderTagHeaderViewUiState.ReaderTagHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.config.ReaderImprovementsFeatureConfig
import org.wordpress.android.util.extensions.gone
import org.wordpress.android.util.extensions.visible
import javax.inject.Inject

/**
 * topmost view in post adapter when showing tag preview - displays tag name and follow button
 */
class ReaderTagHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var binding: ReaderTagBinding

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var readerImprovementsFeatureConfig: ReaderImprovementsFeatureConfig

    private var onFollowBtnClicked: (() -> Unit)? = null

    init {
        (context.applicationContext as WordPress).component().inject(this)
        binding = if (readerImprovementsFeatureConfig.isEnabled()) {
            val readerTagHeaderViewNewBinding =
                ReaderTagHeaderViewNewBinding.inflate(LayoutInflater.from(context), this, true)
            ReaderTagBinding.ImprovementsEnabled(
                textTag = readerTagHeaderViewNewBinding.textTag,
                followButton = readerTagHeaderViewNewBinding.followContainer.followButton,
                textTagFollowCount = readerTagHeaderViewNewBinding.followContainer.textBlogFollowCount,
            )
        } else {
            val readerTagHeaderViewBinding =
                ReaderTagHeaderViewBinding.inflate(LayoutInflater.from(context), this, true)
            ReaderTagBinding.ImprovementsDisabled(
                textTag = readerTagHeaderViewBinding.textTag,
                followButton = readerTagHeaderViewBinding.followButton,
            )
        }
        binding.followButton.setOnClickListener { onFollowBtnClicked?.invoke() }
    }

    abstract class ReaderTagBinding(
    ) {
        abstract val textTag: MaterialTextView
        abstract val followButton: ReaderFollowButton

        data class ImprovementsDisabled(
            override val textTag: MaterialTextView,
            override val followButton: ReaderFollowButton
        ) : ReaderTagBinding()

        data class ImprovementsEnabled(
            override val textTag: MaterialTextView,
            override val followButton: ReaderFollowButton,
            val textTagFollowCount: MaterialTextView,
        ) : ReaderTagBinding()
    }

    fun updateUi(uiState: ReaderTagHeaderUiState) = with(binding) {
        (binding as? ReaderTagBinding.ImprovementsEnabled)?.textTagFollowCount?.gone()
        textTag.text = uiState.title
        with(uiState.followButtonUiState) {
            followButton.setIsFollowed(isFollowed)
            followButton.isEnabled = isEnabled
            onFollowBtnClicked = onFollowButtonClicked
        }
    }
}
