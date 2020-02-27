package org.wordpress.android.imageeditor.crop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.yalantis.ucrop.UCropFragment
import org.wordpress.android.imageeditor.R
import androidx.navigation.fragment.navArgs

class CropFragment : UCropFragment() {
    private lateinit var viewModel: CropViewModel
    private val navArgs: CropFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initializeViewModels()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun initializeViewModels() {
        viewModel = ViewModelProvider(this).get(CropViewModel::class.java)
        setupObservers()
        viewModel.start(navArgs.inputFilePath, requireContext().cacheDir)
        viewModel.writeToBundle(arguments ?: Bundle())
    }

    private fun setupObservers() {
        viewModel.shouldCropAndSaveImage.observe(this, Observer { shouldCropAndSaveImage ->
            if (shouldCropAndSaveImage) {
                cropAndSaveImage()
            }
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
}
