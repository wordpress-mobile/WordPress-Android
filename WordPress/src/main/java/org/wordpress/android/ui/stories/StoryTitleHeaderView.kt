package org.wordpress.android.ui.stories

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.wordpress.android.R
import org.wordpress.android.databinding.PrepublishingStoryTitleListItemBinding
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.StoryTitleUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.focusAndShowKeyboard
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

class StoryTitleHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val thumbnailCornerRadius =
            context.resources.getDimension(R.dimen.prepublishing_site_blavatar_corner_radius)
                    .toInt()

    fun init(uiHelpers: UiHelpers, imageManager: ImageManager, uiState: StoryTitleUiState) {
        with(PrepublishingStoryTitleListItemBinding.inflate(LayoutInflater.from(context), this, true)) {
            imageManager.loadImageWithCorners(
                    storyThumbnail,
                    ImageType.IMAGE,
                    uiState.storyThumbnailUrl,
                    thumbnailCornerRadius
            )

            uiState.storyTitle?.let { title ->
                storyTitle.setText(uiHelpers.getTextOfUiString(context, title))
                storyTitle.setSelection(title.text.length)
            }

            storyTitle.focusAndShowKeyboard()

            storyTitleContent.setOnClickListener {
                storyTitle.focusAndShowKeyboard()
            }

            storyTitle.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(view: Editable?) {
                    view?.let {
                        uiState.onStoryTitleChanged.invoke(it.toString())
                    }
                }
            })
        }
    }
}
