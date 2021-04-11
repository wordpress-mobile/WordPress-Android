package org.wordpress.android.ui.engagement

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.engagement.BottomSheetUiState.UserProfileUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import org.wordpress.android.util.image.ImageType.BLAVATAR
import javax.inject.Inject

class UserProfileBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: UserProfileViewModel

    companion object {
        const val USER_PROFILE_VIEW_MODEL_KEY = "user_profile_view_model_key"

        fun newInstance(viewModelKey: String): UserProfileBottomSheetFragment {
            val fragment = UserProfileBottomSheetFragment()
            val bundle = Bundle()

            bundle.putString(USER_PROFILE_VIEW_MODEL_KEY, viewModelKey)

            fragment.arguments = bundle

            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.user_profile_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val vmKey = requireArguments().getString(USER_PROFILE_VIEW_MODEL_KEY)!!

        viewModel = ViewModelProvider(parentFragment as ViewModelStoreOwner, viewModelFactory)
                .get(vmKey, UserProfileViewModel::class.java)

        val userAvatar = view.findViewById<ImageView>(R.id.user_avatar)
        val blavatar = view.findViewById<ImageView>(R.id.user_site_blavatar)
        val userName = view.findViewById<TextView>(R.id.user_name)
        val userLogin = view.findViewById<TextView>(R.id.user_login)
        val userBio = view.findViewById<TextView>(R.id.user_bio)
        val siteTitle = view.findViewById<TextView>(R.id.site_title)
        val siteUrl = view.findViewById<TextView>(R.id.site_url)
        val siteData = view.findViewById<View>(R.id.site_data)

        viewModel.bottomSheetUiState.observe(viewLifecycleOwner, { state ->
            when (state) {
                is UserProfileUiState -> {
                    imageManager.loadIntoCircle(userAvatar, AVATAR_WITH_BACKGROUND, state.userAvatarUrl)
                    imageManager.load(blavatar, BLAVATAR, state.blavatarUrl)
                    userName.text = state.userName
                    userLogin.text = if (state.userLogin.isNotBlank()) "@${state.userLogin}" else ""
                    if (state.userBio.isNotBlank()) {
                        userBio.text = state.userBio
                        userBio.visibility = View.VISIBLE
                    } else {
                        userBio.visibility = View.GONE
                    }
                    siteTitle.text = state.siteTitle
                    siteUrl.text = state.siteUrl
                    siteData.setOnClickListener {
                        state.onClick?.invoke(state.siteId, state.siteUrl)
                    }
                }
            }
        })

        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            val bottomSheet = sheetDialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
            ) as? FrameLayout

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val metrics = resources.displayMetrics
                behavior.peekHeight = metrics.heightPixels
            }
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
