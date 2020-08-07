package org.wordpress.android.util

import androidx.lifecycle.MutableLiveData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TestScope
import org.wordpress.android.test

class LiveDataUtilsTest : BaseUnitTest() {
    @Test
    fun `mergeNotNull merges 2 sources`() {
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
    fun `mergeAsyncNotNull merges 2 sources with function`() = test {
        val sourceA = MutableLiveData<Int>()
        val sourceB = MutableLiveData<String>()

        val mergedSources = mergeAsyncNotNull(TestScope, sourceA, sourceB) { i, s ->
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
    fun `merge merges 2 sources with function`() = test {
        val sourceA = MutableLiveData<Int>()
        val sourceB = MutableLiveData<String>()

        val mergedSources = merge(sourceA, sourceB) { i, s ->
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
    fun `merge merges 3 sources with function`() = test {
        val sourceA = MutableLiveData<Int>()
        val sourceB = MutableLiveData<String>()
        val sourceC = MutableLiveData<Boolean>()

        val mergedSources = merge(sourceA, sourceB, sourceC) { i, s, b ->
            "$s: $i: $b"
        }
        mergedSources.observeForever { }

        assertThat(mergedSources.value).isEqualTo("null: null: null")
        val firstValue = 1
        val secondValue = "value"
        val thirdValue = true
        sourceA.value = firstValue
        assertThat(mergedSources.value).isEqualTo("null: $firstValue: null")
        sourceB.value = secondValue
        assertThat(mergedSources.value).isEqualTo("$secondValue: $firstValue: null")
        sourceC.value = thirdValue
        assertThat(mergedSources.value).isEqualTo("$secondValue: $firstValue: $thirdValue")
    }

    @Test
    fun `merge merges 4 sources with function`() = test {
        val sourceA = MutableLiveData<Int>()
        val sourceB = MutableLiveData<String>()
        val sourceC = MutableLiveData<Boolean>()
        val sourceD = MutableLiveData<Double>()

        val mergedSources = merge(sourceA, sourceB, sourceC, sourceD) { i, s, b, d ->
            "$s: $i: $b: $d"
        }
        mergedSources.observeForever { }

        assertThat(mergedSources.value).isEqualTo("null: null: null: null")
        val firstValue = 1
        val secondValue = "value"
        val thirdValue = true
        val fourthValue = 2.4
        sourceA.value = firstValue
        assertThat(mergedSources.value).isEqualTo("null: $firstValue: null: null")
        sourceB.value = secondValue
        assertThat(mergedSources.value).isEqualTo("$secondValue: $firstValue: null: null")
        sourceC.value = thirdValue
        assertThat(mergedSources.value).isEqualTo("$secondValue: $firstValue: $thirdValue: null")
        sourceD.value = fourthValue
        assertThat(mergedSources.value).isEqualTo("$secondValue: $firstValue: $thirdValue: $fourthValue")
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

    @Test
    fun `when skipping 1 on a LiveData emitting nothing, nothing is emitted`() {
        // Given
        val source = MutableLiveData<String>()
        check(source.value == null)

        val skip = source.skip(1)

        // When
        var emitCount = 0
        skip.observeForever {
            emitCount += 1
        }

        // Then
        assertThat(emitCount).isZero()
        assertThat(skip.value).isNull()
    }

    @Test
    fun `when skipping 1 on a LiveData emitting a single value, nothing is emitted`() {
        // Given
        val source = MutableLiveData<String>().apply { value = "Alpha" }
        val skip = source.skip(1)

        // When
        var emitCount = 0
        skip.observeForever {
            emitCount += 1
        }

        // Then
        assertThat(emitCount).isZero()
        assertThat(skip.value).isNull()
    }

    @Test
    fun `when skipping 1 on a LiveData emitting multiple values, the first value is not emitted`() {
        // Given
        val source = MutableLiveData<String>().apply {
            // Capture the scenario of the LiveData having a pre-existing value before an observer is added
            value = "Alpha"
        }
        val skip = source.skip(1)

        // When
        var emitCount = 0
        skip.observeForever {
            emitCount += 1
        }

        source.postValue("Bravo")

        // Then
        assertThat(emitCount).isOne()
        assertThat(skip.value).isEqualTo("Bravo")
    }

    @Test
    fun `when skipping 3 on a LiveData emitting multiple values, the first three are not emitted`() {
        // Given
        val source = MutableLiveData<String>()
        val skip = source.skip(3)

        // When
        var emitCount = 0
        val emittedValues = mutableListOf<String>()
        skip.observeForever { value ->
            emitCount += 1

            value?.let { emittedValues.add(it) }
        }

        listOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel").forEach(source::postValue)

        // Then
        assertThat(emitCount).isEqualTo(5)
        assertThat(emittedValues).isEqualTo(listOf("Delta", "Echo", "Foxtrot", "Golf", "Hotel"))
        assertThat(skip.value).isEqualTo("Hotel")
    }

    @Test
    fun `folds emitted item with previous if not null`() {
        // Given
        val source = MutableLiveData<String>()
        val fold = source.fold { previous, current -> "$previous $current" }
        val emittedValues = mutableListOf<String>()
        var emitCount = 0
        fold.observeForever { value ->
            emitCount += 1

            value?.let { emittedValues.add(it) }
        }

        // When
        listOf("Alpha", "Bravo", "Charlie").forEach(source::postValue)

        // Then
        assertThat(emitCount).isEqualTo(3)
        assertThat(emittedValues).isEqualTo(listOf("Alpha", "Alpha Bravo", "Alpha Bravo Charlie"))
    }
}
