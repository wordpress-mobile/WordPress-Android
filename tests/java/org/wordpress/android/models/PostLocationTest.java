package org.wordpress.android.models;

import android.test.InstrumentationTestCase;

import junit.framework.Assert;

public class PostLocationTest extends InstrumentationTestCase {
    public static final double MAX_LAT = 90;
    public static final double MIN_LAT = -90;
    public static final double MAX_LNG = 180;
    public static final double MIN_LNG = -180;
    public static final double INVALID_LAT_MAX = 91;
    public static final double INVALID_LAT_MIN = -91;
    public static final double INVALID_LNG_MAX = 181;
    public static final double INVALID_LNG_MIN = -181;
    public static final double EQUATOR_LAT = 0;
    public static final double EQUATOR_LNG = 0;

    public void testInstantiateValidLocation() {
        PostLocation locationZero = new PostLocation(EQUATOR_LAT, EQUATOR_LNG);
        assertTrue("ZeroLoc did not instantiate valid location", locationZero.isValid());
        assertEquals("ZeroLoc did not return correct lat", EQUATOR_LAT, locationZero.getLatitude());
        assertEquals("ZeroLoc did not return correct lng", EQUATOR_LNG, locationZero.getLongitude());

        PostLocation locationMax = new PostLocation(MAX_LAT, MAX_LNG);
        assertTrue("MaxLoc did not instantiate valid location", locationMax.isValid());
        assertEquals("MaxLoc did not return correct lat", MAX_LAT, locationMax.getLatitude());
        assertEquals("MaxLoc did not return correct lng", MAX_LNG, locationMax.getLongitude());

        PostLocation locationMin = new PostLocation(MIN_LAT, MIN_LNG);
        assertTrue("MinLoc did not instantiate valid location", locationMin.isValid());
        assertEquals("MinLoc did not return correct lat", MIN_LAT, locationMin.getLatitude());
        assertEquals("MinLoc did not return correct lng", MIN_LNG, locationMin.getLongitude());

        double miscLat = 34;
        double miscLng = -60;
        PostLocation locationMisc = new PostLocation(miscLat, miscLng);
        assertTrue("MiscLoc did not instantiate valid location", locationMisc.isValid());
        assertEquals("MiscLoc did not return correct lat", miscLat, locationMisc.getLatitude());
        assertEquals("MiscLoc did not return correct lng", miscLng, locationMisc.getLongitude());
    }

    public void testDefaultLocationInvalid() {
        PostLocation location = new PostLocation();
        assertFalse("Empty location should be invalid", location.isValid());
    }

    public void testInvalidLatitude() {
        PostLocation maxLoc = null;
        try {
            maxLoc = new PostLocation(INVALID_LAT_MAX, 0);
            Assert.fail("Lat more than max should have failed on instantiation");
        } catch (IllegalArgumentException e) {
            assertNull("Invalid instantiation and not null", maxLoc);
        }

        PostLocation minLoc = null;
        try {
            minLoc = new PostLocation(INVALID_LAT_MIN, 0);
            Assert.fail("Lat less than min should have failed on instantiation");
        } catch (IllegalArgumentException e) {
            assertNull("Invalid instantiation and not null", minLoc);
        }

        PostLocation location = new PostLocation();

        try {
            location.setLatitude(INVALID_LAT_MAX);
            Assert.fail("Lat less than min should have failed");
        } catch (IllegalArgumentException e) {
            assertFalse("Invalid setLatitude and still valid", location.isValid());
        }

        try {
            location.setLatitude(INVALID_LAT_MIN);
            Assert.fail("Lat less than min should have failed");
        } catch (IllegalArgumentException e) {
            assertFalse("Invalid setLatitude and still valid", location.isValid());
        }
    }

    public void testInvalidLongitude() {
        PostLocation maxLoc = null;
        try {
            maxLoc = new PostLocation(0, INVALID_LNG_MAX);
            Assert.fail("Lng more than max should have failed on instantiation");
        } catch (IllegalArgumentException e) {
            assertNull("Invalid instantiation and not null",  maxLoc);
        }

        PostLocation minLoc = null;
        try {
            minLoc = new PostLocation(0, INVALID_LNG_MIN);
            Assert.fail("Lng less than min should have failed on instantiation");
        } catch (IllegalArgumentException e) {
            assertNull("Invalid instantiation and not null",  minLoc);
        }

        PostLocation location = new PostLocation();

        try {
            location.setLongitude(INVALID_LNG_MAX);
            Assert.fail("Lng less than min should have failed");
        } catch (IllegalArgumentException e) {
            assertFalse("Invalid setLongitude and still valid", location.isValid());
        }

        try {
            location.setLongitude(INVALID_LNG_MIN);
            Assert.fail("Lat less than min should have failed");
        } catch (IllegalArgumentException e) {
            assertFalse("Invalid setLongitude and still valid", location.isValid());
        }
    }
}
