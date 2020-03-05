package org.wordpress.android.imageeditor.crop

import android.content.Intent
import android.os.Bundle
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

/**
 * Container fragment for displaying third party crop fragment and done menu item.
 */
class CropFragment : Fragment(), UCropFragmentCallback {
    private lateinit var viewModel: CropViewModel
    private val navArgs: CropFragmentArgs by navArgs()

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
        viewModel.start(navArgs.inputFilePath, requireContext().cacheDir)
    }

    private fun setupObservers() {
        viewModel.showCropScreenWithBundleEvent.observe(this, Observer {
            it?.applyIfNotHandled {
                showThirdPartyCropFragmentWithBundle(it.peekContent())
            }
        })
        viewModel.shouldCropAndSaveImage.observe(this, Observer { shouldCropAndSaveImage ->
            if (shouldCropAndSaveImage) {
                val thirdPartyCropFragment = childFragmentManager.findFragmentByTag(UCropFragment.TAG) as? UCropFragment
                thirdPartyCropFragment?.cropAndSaveImage()
            }
        })

        viewModel.navigateBackWithCropResult.observe(this, Observer { cropResult ->
            navigateBackWithCropResult(cropResult)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_crop_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = if (item.itemId == R.id.menu_done) {
        viewModel.onDoneMenuClicked()
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    private fun showThirdPartyCropFragmentWithBundle(bundle: Bundle) {
        val thirdPartyCropFragment = UCropFragment.newInstance(bundle)
        childFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, thirdPartyCropFragment, UCropFragment.TAG)
                .disallowAddToBackStack()
                .commit()
    }

    override fun onCropFinish(result: UCropResult?) {
        result?.let { viewModel.onCropFinish(it.mResultCode, it.mResultData) }
    }

    override fun loadingProgress(showLoader: Boolean) { }

    private fun navigateBackWithCropResult(cropResult: Pair<Int, Intent>) {
        activity?.let {
            it.setResult(cropResult.first, cropResult.second)
            it.finish()
        }
    }
}
