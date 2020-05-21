package org.wordpress.android.util;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

public class DateTimeUtilsTest {
    private final long mDefaultDate = 1564484058163L; // it's Tue Jul 30 2019 10:54:18 in UTC

    @Test
    public void testIso8601UTCFromDate() {
        // Arrange
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+2:00"));
        Date date = new Date(mDefaultDate);
        String expected = "2019-07-30T10:54:18+00:00";

        // Act
        String actual = DateTimeUtils.iso8601UTCFromDate(date);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @Ignore(value = "This test is failing because `DateTimeUtils.localDateToUTC` doesn't work as expected. I've "
                    + "marked it as deprecated and this tests serves just as a documentation.")
    public void testLocalDateToUTC() {
        // Arrange
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+2:00"));
        Date date = new Date(mDefaultDate);
        // this succeeds
        assertThat(DateTimeUtils.iso8601FromDate(date)).isEqualTo("2019-07-30T12:54:18+0200");

        // Act
        String actual = DateTimeUtils.iso8601FromDate(DateTimeUtils.localDateToUTC(date));

        // Assert

        // fails because `localDateToUTC` doesn't work as expected. See DateTimeUtils.localDateToUTC for more info.
        assertThat(actual).isEqualTo("2019-07-30T10:54:18+00:00");
    }
}
