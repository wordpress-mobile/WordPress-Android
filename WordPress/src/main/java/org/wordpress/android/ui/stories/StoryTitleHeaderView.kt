package org.wordpress.android.ui.stories

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.prepublishing_story_title_list_item.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.StoryTitleUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

private const val STORY_TITLE_EDIT_TEXT_REQUEST_FOCUS_DELAY = 1000L

class StoryTitleHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val thumbnailCornerRadius =
            context.resources.getDimension(R.dimen.prepublishing_site_blavatar_corner_radius)
                    .toInt()

    fun init(uiHelpers: UiHelpers, imageManager: ImageManager, uiState: StoryTitleUiState) {
        LayoutInflater.from(context).inflate(R.layout.prepublishing_story_title_list_item, this, true)

        imageManager.loadImageWithCorners(
                story_thumbnail,
                ImageType.IMAGE,
                uiState.storyThumbnailUrl,
                thumbnailCornerRadius
        )

        uiState.storyTitle?.let { title ->
            story_title.setText(uiHelpers.getTextOfUiString(context, title))
            story_title.setSelection(title.text.length)
        }

        story_title.postDelayed({
            story_title.requestFocus()
        }, STORY_TITLE_EDIT_TEXT_REQUEST_FOCUS_DELAY)

        story_title.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(view: Editable?) {
                uiState.onStoryTitleChanged.invoke(view.toString())
            }
        })
    }
}
