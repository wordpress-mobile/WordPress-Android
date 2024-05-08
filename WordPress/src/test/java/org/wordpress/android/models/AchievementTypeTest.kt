package org.wordpress.android.models

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AchievementTypeTest  : BaseUnitTest() {
    @Test
    fun `isAchievementType returns true for valid achievement types`() {
        for (type in AchievementType.entries) {
            assertTrue(AchievementType.isAchievementType(type.rawType))
        }
    }

    @Test
    fun `isAchievementType returns false for invalid achievement type`() {
        assertFalse(AchievementType.isAchievementType("invalid_type"))
    }
}
