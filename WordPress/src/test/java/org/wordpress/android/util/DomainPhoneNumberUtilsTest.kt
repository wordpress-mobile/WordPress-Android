package org.wordpress.android.util

import org.assertj.core.api.Assertions
import org.junit.Test

class DomainPhoneNumberUtilsTest {
    companion object {
        const val US_COUNTRY_CODE = "US"
        const val US_COUNTRY_CODE_PREFIX = "1"
        const val GB_COUNTRY_CODE = "GB"
        const val GB_COUNTRY_CODE_PREFIX = "44"
        const val NON_EXISTENT_COUNTRY_CODE = "WORDPRESS"

        const val US_PHONE_NUMBER = "+1.0123456789"
        const val NO_COUNTRY_CODE_US_PHONE_NUMBER = "0123456789"
        const val GB_PHONE_NUMBER = "+44.987654321"
        const val NO_COUNTRY_CODE_GB_PHONE_NUMBER = "987654321"
        const val ONLY_US_COUNTRY_CODE_PREFIX_PHONE_NUMBER = "+1."
        const val MALFORMED_PHONE_NUMBER = "10123456789"
    }

    @Test
    fun `countryCodePrefix returns correct country code`() {
        Assertions.assertThat(DomainPhoneNumberUtils.getCountryCodePrefix(US_COUNTRY_CODE))
                .isEqualTo(US_COUNTRY_CODE_PREFIX)
        Assertions.assertThat(DomainPhoneNumberUtils.getCountryCodePrefix(GB_COUNTRY_CODE))
                .isEqualTo(GB_COUNTRY_CODE_PREFIX)
        Assertions.assertThat(DomainPhoneNumberUtils.getCountryCodePrefix(NON_EXISTENT_COUNTRY_CODE)).isNull()
    }

    @Test
    fun `getCountryCodeFromFullPhoneNumber returns correct country code from phone number`() {
        Assertions.assertThat(DomainPhoneNumberUtils.getCountryCodePrefixFromFullPhoneNumber(US_PHONE_NUMBER))
                .isEqualTo(US_COUNTRY_CODE_PREFIX)
        Assertions.assertThat(DomainPhoneNumberUtils.getCountryCodePrefixFromFullPhoneNumber(GB_PHONE_NUMBER))
                .isEqualTo(GB_COUNTRY_CODE_PREFIX)
        Assertions.assertThat(
                DomainPhoneNumberUtils.getCountryCodePrefixFromFullPhoneNumber(NO_COUNTRY_CODE_US_PHONE_NUMBER)
        )
                .isNull()
        Assertions.assertThat(
                DomainPhoneNumberUtils.getCountryCodePrefixFromFullPhoneNumber(ONLY_US_COUNTRY_CODE_PREFIX_PHONE_NUMBER)
        ).isEqualTo(US_COUNTRY_CODE_PREFIX)
        Assertions.assertThat(DomainPhoneNumberUtils.getCountryCodePrefixFromFullPhoneNumber(MALFORMED_PHONE_NUMBER))
                .isNull()
        Assertions.assertThat(DomainPhoneNumberUtils.getCountryCodePrefixFromFullPhoneNumber(""))
                .isNull()
    }

    @Test
    fun `formatCountryCodeAndPhoneNumber returns correct phone number`() {
        Assertions.assertThat(
                DomainPhoneNumberUtils.formatCountryCodeAndPhoneNumber(
                        US_COUNTRY_CODE_PREFIX,
                        NO_COUNTRY_CODE_US_PHONE_NUMBER
                )
        )
                .isEqualTo(US_PHONE_NUMBER)
        Assertions.assertThat(
                DomainPhoneNumberUtils.formatCountryCodeAndPhoneNumber(
                        GB_COUNTRY_CODE_PREFIX,
                        NO_COUNTRY_CODE_GB_PHONE_NUMBER
                )
        )
                .isEqualTo(GB_PHONE_NUMBER)

        Assertions.assertThat(
                DomainPhoneNumberUtils.formatCountryCodeAndPhoneNumber(
                        null,
                        NO_COUNTRY_CODE_US_PHONE_NUMBER
                )
        )
                .isEqualTo("+.$NO_COUNTRY_CODE_US_PHONE_NUMBER")

        Assertions.assertThat(
                DomainPhoneNumberUtils.formatCountryCodeAndPhoneNumber(
                        US_COUNTRY_CODE_PREFIX, null
                )
        )
                .isEqualTo("+$US_COUNTRY_CODE_PREFIX.")
    }
}
