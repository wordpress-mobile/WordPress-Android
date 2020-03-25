package org.wordpress.android.imageeditor.crop

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import org.wordpress.android.imageeditor.R
import com.yalantis.ucrop.UCropFragment
import com.yalantis.ucrop.UCropFragment.UCropResult
import com.yalantis.ucrop.UCropFragmentCallback
import org.wordpress.android.imageeditor.crop.CropViewModel.CropResult
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveFailedState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveStartState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveSuccessState
import org.wordpress.android.imageeditor.crop.CropViewModel.UiState.UiLoadedState
import org.wordpress.android.imageeditor.crop.CropViewModel.UiState.UiStartLoadingWithBundleState
import org.wordpress.android.imageeditor.utils.ToastUtils
import org.wordpress.android.imageeditor.utils.ToastUtils.Duration

/**
 * Container fragment for displaying third party crop fragment and done menu item.
 */
class CropFragment : Fragment(), UCropFragmentCallback {
    private lateinit var viewModel: CropViewModel
    private var doneMenu: MenuItem? = null
    private val navArgs: CropFragmentArgs by navArgs()

    companion object {
        private val TAG = CropFragment::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_crop_image, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModels()
    }

    private fun initializeViewModels() {
        viewModel = ViewModelProvider(this).get(CropViewModel::class.java)
        setupObservers()
        viewModel.start(navArgs.inputFilePath, navArgs.outputFileExtension, requireContext().cacheDir)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this, Observer { uiState ->
            when (uiState) {
                is UiStartLoadingWithBundleState -> {
                    showThirdPartyCropFragmentWithBundle(uiState.bundle)
                }
                is UiLoadedState -> { // Do nothing
                }
            }
            doneMenu?.isVisible = uiState.doneMenuVisible
        })

        viewModel.cropAndSaveImageStateEvent.observe(this, Observer { stateEvent ->
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
                        showCropError(state.errorMsg, state.errorResId)
                    }
                    is ImageCropAndSaveSuccessState -> { // Do nothing
                    }
                }
            }
        })

        viewModel.navigateBackWithCropResult.observe(this, Observer { cropResult ->
            navigateBackWithCropResult(cropResult)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_crop_fragment, menu)
        doneMenu = menu?.findItem(R.id.menu_done)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = if (item.itemId == R.id.menu_done) {
        viewModel.onDoneMenuClicked()
        true
    } else {
        super.onOptionsItemSelected(item)
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

    private fun showCropError(errorMsg: String?, errorResId: Int) {
        // TODO: track exact error errorMsg
        ToastUtils.showToast(context, getString(errorResId), Duration.LONG)
    }

    private fun navigateBackWithCropResult(cropResult: CropResult) {
        activity?.let {
            it.setResult(cropResult.resultCode, cropResult.data)
            it.finish()
        }
    }
}
