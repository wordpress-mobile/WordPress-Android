package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderAuthorProfileBottomSheetBinding
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

@AndroidEntryPoint
class ReaderAuthorProfileBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var imageManager: ImageManager

    private var _binding: ReaderAuthorProfileBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ReaderAuthorProfileBottomSheetBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()

        val authorName = args.getString(ARG_AUTHOR_NAME).orEmpty()
        val authorAvatar = args.getString(ARG_AUTHOR_AVATAR).orEmpty()
        val blogName = args.getString(ARG_BLOG_NAME).orEmpty()
        val blogUrl = args.getString(ARG_BLOG_URL).orEmpty()

        val avatarSz = resources.getDimensionPixelSize(
            R.dimen.user_profile_bottom_sheet_avatar_sz
        )
        imageManager.loadIntoCircle(
            binding.authorAvatar,
            ImageType.AVATAR_WITH_BACKGROUND,
            WPAvatarUtils.rewriteAvatarUrl(authorAvatar, avatarSz)
        )

        binding.authorName.text = authorName

        if (blogUrl.isNotBlank()) {
            binding.authorBlogName.isVisible = true
            binding.authorBlogName.text =
                blogName.ifBlank { UrlUtils.getHost(blogUrl) }
            binding.authorBlogName.setOnClickListener {
                val ctx = context ?: return@setOnClickListener
                ReaderActivityLauncher.openUrl(ctx, blogUrl)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
