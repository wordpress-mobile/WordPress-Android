package org.wordpress.android.util

import android.arch.lifecycle.MutableLiveData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest

class LiveDataUtilsTest : BaseUnitTest() {
    @Test
    fun `merge merges sources`() {
        val sourceA = MutableLiveData<Int>()
        val sourceB = MutableLiveData<Int>()

        val mergedSources = mergeNotNull(sourceA, sourceB)
        mergedSources.observeForever { }

        assertThat(mergedSources.value).isNull()
        val firstValue = 1
        val secondValue = 2
        sourceA.value = firstValue
        assertThat(mergedSources.value).isEqualTo(firstValue)
        sourceB.value = secondValue
        assertThat(mergedSources.value).isEqualTo(secondValue)
    }

    @Test
    fun `merge merges sources with function`() {
        val sourceA = MutableLiveData<Int>()
        val sourceB = MutableLiveData<String>()

        val mergedSources = mergeNotNull(sourceA, sourceB) { i, s ->
            "$s: $i"
        }
        mergedSources.observeForever { }

        assertThat(mergedSources.value).isNull()
        val firstValue = 1
        val secondValue = "value"
        sourceA.value = firstValue
        assertThat(mergedSources.value).isNull()
        sourceB.value = secondValue
        assertThat(mergedSources.value).isEqualTo("value: 1")
    }

    @Test
    fun `combineMap combines sources in a map`() {
        val sourceA = MutableLiveData<Int>()
        val sourceB = MutableLiveData<Int>()
        val keyA = "keyA"
        val keyB = "keyB"
        val mapOfStringToLiveData = mapOf(keyA to sourceA, keyB to sourceB)

        val combineMap = combineMap(mapOfStringToLiveData)

        combineMap.observeForever { }

        assertThat(combineMap.value).isEqualTo(mapOf<String, Int>())
        val valueA = 1
        val valueB = 2
        sourceA.value = valueA
        assertThat(combineMap.value).isEqualTo(mapOf(keyA to valueA))

        sourceB.value = valueB
        assertThat(combineMap.value).isEqualTo(mapOf(keyA to valueA, keyB to valueB))

        sourceB.value = null
        assertThat(combineMap.value).isEqualTo(mapOf(keyA to valueA))
    }
}
