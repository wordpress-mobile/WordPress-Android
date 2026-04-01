package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

@AndroidEntryPoint
class ReaderAuthorProfileBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var imageManager: ImageManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.reader_author_profile_bottom_sheet, container, false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()

        val authorName = args.getString(ARG_AUTHOR_NAME).orEmpty()
        val authorAvatar = args.getString(ARG_AUTHOR_AVATAR).orEmpty()
        val blogName = args.getString(ARG_BLOG_NAME).orEmpty()
        val blogUrl = args.getString(ARG_BLOG_URL).orEmpty()

        val avatarView = view.findViewById<ImageView>(R.id.author_avatar)
        val avatarSz = resources.getDimensionPixelSize(
            R.dimen.user_profile_bottom_sheet_avatar_sz
        )
        imageManager.loadIntoCircle(
            avatarView,
            ImageType.AVATAR_WITH_BACKGROUND,
            WPAvatarUtils.rewriteAvatarUrl(authorAvatar, avatarSz)
        )

        view.findViewById<TextView>(R.id.author_name).text = authorName

        val blogView = view.findViewById<TextView>(R.id.author_blog_name)
        if (blogUrl.isNotBlank()) {
            blogView.isVisible = true
            blogView.text = blogName.ifBlank { UrlUtils.getHost(blogUrl) }
            blogView.setOnClickListener {
                ReaderActivityLauncher.openUrl(requireContext(), blogUrl)
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "ReaderAuthorProfileBottomSheetFragment"
        private const val ARG_AUTHOR_NAME = "author_name"
        private const val ARG_AUTHOR_AVATAR = "author_avatar"
        private const val ARG_BLOG_NAME = "blog_name"
        private const val ARG_BLOG_URL = "blog_url"

        fun newInstance(
            authorName: String,
            authorAvatar: String,
            blogName: String,
            blogUrl: String,
        ) = ReaderAuthorProfileBottomSheetFragment().apply {
            arguments = bundleOf(
                ARG_AUTHOR_NAME to authorName,
                ARG_AUTHOR_AVATAR to authorAvatar,
                ARG_BLOG_NAME to blogName,
                ARG_BLOG_URL to blogUrl,
            )
        }
    }
}
