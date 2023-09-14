package org.wordpress.android.imageeditor.crop

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.yalantis.ucrop.UCropFragment
import com.yalantis.ucrop.UCropFragment.UCropResult
import com.yalantis.ucrop.UCropFragmentCallback
import org.wordpress.android.imageeditor.EditImageViewModel
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.crop.CropViewModel.CropResult
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveFailedState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveStartState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveSuccessState
import org.wordpress.android.imageeditor.crop.CropViewModel.UiState.UiLoadedState
import org.wordpress.android.imageeditor.crop.CropViewModel.UiState.UiStartLoadingWithBundleState
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.ARG_EDIT_IMAGE_DATA
import org.wordpress.android.imageeditor.utils.ToastUtils
import org.wordpress.android.imageeditor.utils.ToastUtils.Duration
import android.R as AndroidR

/**
 * Container fragment for displaying third party crop fragment and done menu item.
 */
class CropFragment : Fragment(), MenuProvider, UCropFragmentCallback {
    private lateinit var viewModel: CropViewModel
    private var doneMenu: MenuItem? = null
    private val navArgs: CropFragmentArgs by navArgs()

    companion object {
        private val TAG = CropFragment::class.java.simpleName
        const val CROP_RESULT = "crop_result"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.crop_image_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        initializeViewModels()
    }

    private fun initializeViewModels() {
        viewModel = ViewModelProvider(this).get(CropViewModel::class.java)
        setupObservers()
        viewModel.start(
            navArgs.inputFilePath,
            navArgs.outputFileExtension,
            navArgs.shouldReturnToPreviewScreen,
            requireContext().cacheDir,
            ImageEditor.instance
        )
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            when (uiState) {
                is UiStartLoadingWithBundleState -> {
                    showThirdPartyCropFragmentWithBundle(uiState.bundle)
                }
                is UiLoadedState -> { // Do nothing
                }
            }
            doneMenu?.isVisible = uiState.doneMenuVisible
        }

        viewModel.cropAndSaveImageStateEvent.observe(viewLifecycleOwner) { stateEvent ->
            stateEvent?.getContentIfNotHandled()?.let { state ->
                when (state) {
                    is ImageCropAndSaveStartState -> {
                        val thirdPartyCropFragment = childFragmentManager
                            .findFragmentByTag(UCropFragment.TAG) as? UCropFragment
                        if (thirdPartyCropFragment != null && thirdPartyCropFragment.isAdded) {
                            thirdPartyCropFragment.cropAndSaveImage()
                        } else {
                            Log.e(TAG, "Cannot crop and save image as thirdPartyCropFragment is null or not added!")
                        }
                    }
                    is ImageCropAndSaveFailedState -> {
                        showCropError(state.errorResId)
                    }
                    is ImageCropAndSaveSuccessState -> { // Do nothing
                    }
                }
            }
        }

        viewModel.navigateBackWithCropResult.observe(viewLifecycleOwner) { cropResult ->
            navigateBackWithCropResult(cropResult)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_crop_fragment, menu)
        doneMenu = menu.findItem(R.id.menu_done)
    }

    override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
        R.id.menu_done -> {
            viewModel.onDoneMenuClicked()
            true
        }
        AndroidR.id.home -> {
            if (navArgs.shouldReturnToPreviewScreen) {
                findNavController().popBackStack()
                true
            } else {
                false
            }
        }
        else -> false
    }

    private fun showThirdPartyCropFragmentWithBundle(bundle: Bundle) {
        var thirdPartyCropFragment = childFragmentManager
            .findFragmentByTag(UCropFragment.TAG) as? UCropFragment

        if (thirdPartyCropFragment == null || !thirdPartyCropFragment.isAdded) {
            thirdPartyCropFragment = UCropFragment.newInstance(bundle)
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, thirdPartyCropFragment, UCropFragment.TAG)
                .disallowAddToBackStack()
                .commit()
        }
    }

    override fun loadingProgress(loading: Boolean) {
        viewModel.onLoadingProgress(loading)
    }

    override fun onCropFinish(result: UCropResult?) {
        result?.let {
            viewModel.onCropFinish(it.mResultCode, it.mResultData)
        }
    }

    private fun showCropError(errorResId: Int) {
        ToastUtils.showToast(context, getString(errorResId), Duration.LONG)
    }

    private fun navigateBackWithCropResult(cropResult: CropResult) {
        if (navArgs.shouldReturnToPreviewScreen) {
            val navController = findNavController()
            ViewModelProvider(requireActivity()).get(EditImageViewModel::class.java).setCropResult(cropResult)
            navController.popBackStack()
        } else {
            val resultData = viewModel.getOutputData(cropResult)

            activity?.let {
                it.setResult(
                    cropResult.resultCode,
                    Intent().apply { putParcelableArrayListExtra(ARG_EDIT_IMAGE_DATA, resultData) })
                it.finish()
            }
        }
    }
}
