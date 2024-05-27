package org.wordpress.android.ui.debug.preferences

import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@RunWith(MockitoJUnitRunner::class)
class DebugSharedPreferenceFlagsViewModelTest {
    @Mock
    private lateinit var prefsWrapper: AppPrefsWrapper
    private lateinit var viewModel: DebugSharedPreferenceFlagsViewModel

    @Test
    fun `WHEN init THEN should load the flags from the prefs`() {
        whenever(prefsWrapper.getAllPrefs()).thenReturn(mapOf("key" to true))
        DebugPrefs.entries.forEach {
            whenever(prefsWrapper.getDebugBooleanPref(it.key, false)).thenReturn(false)
        }

        initViewModel()

        assertTrue(viewModel.uiStateFlow.value["key"]!!)
        assertTrue(viewModel.uiStateFlow.value.size >= 2)
    }

    @Test
    fun `WHEN setFlag THEN should update the prefs and the ui state`() {
        initViewModel()

        viewModel.setFlag("key", true)

        verify(prefsWrapper).putBoolean(argThat { key -> key.name() == "key" }, eq(true))
        assertTrue(viewModel.uiStateFlow.value["key"]!!)
    }

    private fun initViewModel() {
        viewModel = DebugSharedPreferenceFlagsViewModel(prefsWrapper)
    }
}
