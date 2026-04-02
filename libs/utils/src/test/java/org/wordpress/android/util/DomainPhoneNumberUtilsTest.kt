package org.wordpress.android.util

import org.assertj.core.api.Assertions
import org.junit.Test

class DomainPhoneNumberUtilsTest {
    companion object {
        const val US_COUNTRY_CODE = "US"
        const val US_PHONE_NUMBER_PREFIX = "1"
        const val GB_COUNTRY_CODE = "GB"
        const val GB_PHONE_NUMBER_PREFIX = "44"
        const val NON_EXISTENT_COUNTRY_CODE = "WORDPRESS"

        const val US_PHONE_NUMBER = "+1.0123456789"
        const val NO_PREFIX_US_PHONE_NUMBER = "0123456789"
        const val GB_PHONE_NUMBER = "+44.987654321"
        const val NO_PREFIX_GB_PHONE_NUMBER = "987654321"
        const val ONLY_US_COUNTRY_CODE_PREFIX_PHONE_NUMBER = "+1."
        const val MALFORMED_PHONE_NUMBER = "10123456789"
    }

    @Test
    fun `getPhoneNumberPrefix returns correct country code`() {
        Assertions.assertThat(DomainPhoneNumberUtils.getPhoneNumberPrefix(US_COUNTRY_CODE))
            .isEqualTo(US_PHONE_NUMBER_PREFIX)
        Assertions.assertThat(DomainPhoneNumberUtils.getPhoneNumberPrefix(GB_COUNTRY_CODE))
            .isEqualTo(GB_PHONE_NUMBER_PREFIX)
        Assertions.assertThat(DomainPhoneNumberUtils.getPhoneNumberPrefix(NON_EXISTENT_COUNTRY_CODE)).isNull()
    }

    @Test
    fun `getPhoneNumberPrefixFromFullPhoneNumber returns correct country code from phone number`() {
        Assertions.assertThat(DomainPhoneNumberUtils.getPhoneNumberPrefixFromFullPhoneNumber(US_PHONE_NUMBER))
            .isEqualTo(US_PHONE_NUMBER_PREFIX)
        Assertions.assertThat(DomainPhoneNumberUtils.getPhoneNumberPrefixFromFullPhoneNumber(GB_PHONE_NUMBER))
            .isEqualTo(GB_PHONE_NUMBER_PREFIX)
        Assertions.assertThat(DomainPhoneNumberUtils.getPhoneNumberPrefixFromFullPhoneNumber(NO_PREFIX_US_PHONE_NUMBER))
            .isNull()
        Assertions.assertThat(
            DomainPhoneNumberUtils.getPhoneNumberPrefixFromFullPhoneNumber(ONLY_US_COUNTRY_CODE_PREFIX_PHONE_NUMBER)
        ).isEqualTo(US_PHONE_NUMBER_PREFIX)
        Assertions.assertThat(DomainPhoneNumberUtils.getPhoneNumberPrefixFromFullPhoneNumber(MALFORMED_PHONE_NUMBER))
            .isNull()
        Assertions.assertThat(DomainPhoneNumberUtils.getPhoneNumberPrefixFromFullPhoneNumber(""))
            .isNull()
    }

    @Test
    fun `formatPhoneNumberandPrefix returns correct phone number`() {
        Assertions.assertThat(
            DomainPhoneNumberUtils.formatPhoneNumberandPrefix(
                US_PHONE_NUMBER_PREFIX,
                NO_PREFIX_US_PHONE_NUMBER
            )
        )
            .isEqualTo(US_PHONE_NUMBER)
        Assertions.assertThat(
            DomainPhoneNumberUtils.formatPhoneNumberandPrefix(
                GB_PHONE_NUMBER_PREFIX,
                NO_PREFIX_GB_PHONE_NUMBER
            )
        )
            .isEqualTo(GB_PHONE_NUMBER)

        Assertions.assertThat(
            DomainPhoneNumberUtils.formatPhoneNumberandPrefix(
                null,
                NO_PREFIX_US_PHONE_NUMBER
            )
        )
            .isEqualTo("+.$NO_PREFIX_US_PHONE_NUMBER")

        Assertions.assertThat(
            DomainPhoneNumberUtils.formatPhoneNumberandPrefix(
                US_PHONE_NUMBER_PREFIX, null
            )
        )
            .isEqualTo("+$US_PHONE_NUMBER_PREFIX.")
    }
}
