package org.wordpress.android.fluxc.post;

import org.junit.Test;
import org.wordpress.android.fluxc.model.post.PostLocation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PostLocationTest {
    private static final double MAX_LAT = 90;
    private static final double MIN_LAT = -90;
    private static final double MAX_LNG = 180;
    private static final double MIN_LNG = -180;
    private static final double INVALID_LAT_MAX = 91;
    private static final double INVALID_LAT_MIN = -91;
    private static final double INVALID_LNG_MAX = 181;
    private static final double INVALID_LNG_MIN = -181;
    private static final double EQUATOR_LAT = 0;
    private static final double EQUATOR_LNG = 0;

    @Test
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

    @Test
    public void testDefaultLocationInvalid() {
        PostLocation location = new PostLocation();
        assertFalse("Empty location should be invalid", location.isValid());
    }

    @Test
    public void testInvalidMaxLatitude() {
        PostLocation location = new PostLocation(INVALID_LAT_MAX, 0.0);
        assertFalse("Invalid Latitude and still valid", location.isValid());
    }

    @Test
    public void testInvalidMinLatitude() {
        PostLocation location = new PostLocation(INVALID_LAT_MIN, 0.0);
        assertFalse("Invalid Latitude and still valid", location.isValid());
    }

    @Test
    public void testInvalidMaxLongitude() {
        PostLocation location = new PostLocation(0.0, INVALID_LNG_MAX);
        assertFalse("Invalid Longitude and still valid", location.isValid());
    }

    @Test
    public void testInvalidMinLongitude() {
        PostLocation location = new PostLocation(0.0, INVALID_LNG_MIN);
        assertFalse("Invalid Longitude and still valid", location.isValid());
    }
}
