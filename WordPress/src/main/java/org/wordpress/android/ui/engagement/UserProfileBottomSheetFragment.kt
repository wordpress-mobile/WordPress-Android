package org.wordpress.android.ui.engagement

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UserProfileBottomSheetBinding
import org.wordpress.android.ui.engagement.BottomSheetUiState.UserProfileUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import com.google.android.material.R as MaterialR

class UserProfileBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: UserProfileViewModel
    private var binding: UserProfileBottomSheetBinding? = null
    private val state by lazy { requireArguments().getSerializable(USER_PROFILE_STATE) as? UserProfileUiState }

    companion object {
        private const val USER_PROFILE_VIEW_MODEL_KEY = "user_profile_view_model_key"
        private const val USER_PROFILE_STATE = "user_profile_state"

        const val TAG = "USER_PROFILE_BOTTOM_SHEET_TAG"

        /**
         * For displaying the user profile when users are from Likes
         */
        fun newInstance(viewModelKey: String) = UserProfileBottomSheetFragment()
            .apply {
                arguments = Bundle().apply {
                    putString(USER_PROFILE_VIEW_MODEL_KEY, viewModelKey)
                }
            }

        /**
         * For displaying the user profile when users are from Comments or Notifications
         */
        @JvmStatic
        fun newInstance(state: UserProfileUiState) = UserProfileBottomSheetFragment()
            .apply {
                arguments = Bundle().apply {
                    putSerializable(USER_PROFILE_STATE, state)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = UserProfileBottomSheetBinding.inflate(inflater, container, false)
        .apply { binding = this }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val vmKey = requireArguments().getString(USER_PROFILE_VIEW_MODEL_KEY)!!

        ViewCompat.setAccessibilityPaneTitle(view, getString(R.string.user_profile_bottom_sheet_description))

        viewModel = ViewModelProvider(parentFragment as ViewModelStoreOwner, viewModelFactory)
            .get(vmKey, UserProfileViewModel::class.java)

        initObservers()
        state?.let { binding?.setup(it) }

        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            val bottomSheet = sheetDialog?.findViewById<View>(
                MaterialR.id.design_bottom_sheet
            ) as? FrameLayout

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val metrics = resources.displayMetrics
                behavior.peekHeight = metrics.heightPixels
            }
        }
    }

    private fun initObservers() {
        viewModel.bottomSheetUiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UserProfileUiState -> {
                    binding?.setup(state)
                }
            }
        }
    }

    private fun UserProfileBottomSheetBinding.setup(state: UserProfileUiState) {
        val avatarSz =
            resourceProvider.getDimensionPixelSize(R.dimen.user_profile_bottom_sheet_avatar_sz)
        val blavatarSz = resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_medium)

        imageManager.loadIntoCircle(
            userAvatar,
            ImageType.AVATAR_WITH_BACKGROUND,
            WPAvatarUtils.rewriteAvatarUrl(state.userAvatarUrl, avatarSz)
        )
        userName.text = state.userName
        userLogin.text = if (state.userLogin.isNotBlank()) {
            getString(R.string.at_username, state.userLogin)
        } else {
            ""
        }
        if (state.userBio.isNotBlank()) {
            userBio.text = state.userBio
            userBio.visibility = View.VISIBLE
        } else {
            userBio.visibility = View.GONE
        }

        imageManager.load(
            userSiteBlavatar,
            ImageType.BLAVATAR,
            PhotonUtils.getPhotonImageUrl(state.blavatarUrl, blavatarSz, blavatarSz, PhotonUtils.Quality.HIGH)
        )

        if (state.hasSiteUrl) {
            siteTitle.text = state.siteTitle
            siteUrl.text = UrlUtils.getHost(state.siteUrl)
            siteData.setOnClickListener {
                state.onSiteClickListener?.invoke(
                    state.siteId,
                    state.siteUrl,
                    state.blogPreviewSource
                )
            }
            siteSectionHeader.visibility = View.VISIBLE
            userSiteBlavatar.visibility = View.VISIBLE
            siteData.visibility = View.VISIBLE
        } else {
            siteSectionHeader.visibility = View.GONE
            userSiteBlavatar.visibility = View.GONE
            siteData.visibility = View.GONE
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        viewModel.onBottomSheetCancelled()
    }
}
