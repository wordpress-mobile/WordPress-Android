package org.wordpress.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EinkDeviceDetectorTest {
    @Test
    fun `known e-ink devices are detected`() {
        val einkDevices = listOf(
            // Known e-ink manufacturers
            Device("Onyx", "BOOX", "Nova Air 2"),
            Device("PocketBook", "PocketBook", "InkPad 4"),
            Device("reMarkable", "reMarkable", "reMarkable 2"),
            Device("Viwoods", "AiPaper Reader", "AiPaper Reader"),
            Device("Supernote", "Supernote", "A5 X2"),
            Device("Tolino", "Tolino", "Vision 6"),
            Device("barnesandnoble", "NOOK", "BNRV730"),
            Device("Boyue", "Likebook", "P6"),
            Device("Dasung", "Dasung", "Not-eReader 103"),
            Device("Bigme", "Bigme", "HiBreak"),
            // Known e-ink brands (manufacturer differs)
            Device("Kobo Inc.", "Kobo", "Clara 2E"),
            Device("ArtaTech", "InkBook", "Focus"),
            Device("Unknown", "AiPaper Reader", "SomeModel"),
            Device("SomeManufacturer", "NOOK", "GlowLight"),
            // Dual-use manufacturers (model pattern required)
            Device("Amazon", "Amazon", "Kindle Paperwhite"),
            Device("Hisense", "Hisense", "A5Pro CC"),
            Device("Hisense", "Hisense", "A7CC"),
            Device("Xiaomi", "Xiaomi", "xiaomi_reader"),
            // Edge cases: case insensitive
            Device("ONYX", "BOOX", "NOVA AIR"),
            // Edge cases: trims whitespace
            Device(" Onyx ", " BOOX ", " Nova Air "),
        )

        einkDevices.forEach { device ->
            assertThat(device.isDetectedAsEink())
                .describedAs(device.label())
                .isTrue()
        }
    }

    @Test
    fun `non-e-ink devices are not detected`() {
        val nonEinkDevices = listOf(
            // Amazon Fire tablets (Android) use KF* model codes
            Device("Amazon", "Amazon", "Fire HD 10"),
            Device("Amazon", "Amazon", "KFTRWI"),
            Device("Amazon", "Amazon", "KFRAPWI"),
            Device("Hisense", "Hisense", "H60 5G"),
            Device("Xiaomi", "Xiaomi", "Redmi Note 12"),
            Device("HUAWEI", "HUAWEI", "Nova 12 Pro"),
            Device("samsung", "samsung", "SM-S928B"),
            Device("Google", "google", "Pixel 9 Pro"),
            Device("SomeOEM", "SomeBrand", "PagePlus Pro"),
            Device("SomeOEM", "SomeBrand", "Vision X1"),
            Device("SomeOEM", "SomeBrand", "Era 5G"),
            Device("", "", ""),
        )

        nonEinkDevices.forEach { device ->
            assertThat(device.isDetectedAsEink())
                .describedAs(device.label())
                .isFalse()
        }
    }

    private data class Device(
        val manufacturer: String,
        val brand: String,
        val model: String,
    ) {
        fun isDetectedAsEink(): Boolean =
            EinkDeviceDetector.isEinkDevice(manufacturer, brand, model)

        fun label(): String =
            "$manufacturer / $brand / $model"
    }
}
