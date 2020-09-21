package org.wordpress.android.util;

import org.junit.Test;
import org.wordpress.android.util.PhotonUtils.Quality;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;


public class PhotonUtilsTest {
    @Test
    public void getPhotonImageUrlIsEmptyWhenUrlIsNull() {
        String photonUrl = PhotonUtils.getPhotonImageUrl(null, 0, 1);

        assertThat(photonUrl, equalTo(""));
    }

    @Test
    public void getPhotonImageUrlIsEmptyWhenUrlIsEmpty() {
        String photonUrl = PhotonUtils.getPhotonImageUrl("", 0, 1);

        assertThat(photonUrl, equalTo(""));
    }

    @Test
    public void getPhotonImageUrlReturnsImageUrlOnNoScheme() {
        String imageUrl = "wordpress.com";
        String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 0, 1);

        assertThat(photonUrl, equalTo(imageUrl));
    }

    @Test
    public void getPhotonImageUrlReturnsMshots() {
        String imageUrl = "http://test.wordpress.com/mshots/test.jpg?query=dummy";
        String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 0, 1);

        assertThat(photonUrl, equalTo("http://test.wordpress.com/mshots/test.jpg?w=0&h=1"));
    }

    @Test
    public void getPhotonImageUrlReturnsCorrectQuality() {
        Map<Quality, String> qualities = new HashMap<>();
        qualities.put(Quality.HIGH, "100");
        qualities.put(Quality.MEDIUM, "65");
        qualities.put(Quality.LOW, "35");

        String imageUrl = "http://test.wordpress.com/test.jpg?query=dummy";

        for (Quality quality : qualities.keySet()) {
            String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 0, 1, quality);
            assertThat(photonUrl, containsString("&quality=" + qualities.get(quality)));
        }
    }

    @Test
    public void getPhotonImageUrlUsesResize() {
        String imageUrl = "http://test.wordpress.com/test.jpg?query=dummy";
        String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 2, 1);

        assertThat(photonUrl, equalTo("http://test.wordpress.com/test.jpg?strip=info&quality=65&resize=2,1"));
    }

    @Test
    public void getPhotonImageUrlManageSslOnPhotonUrl() {
        String imageUrl = "https://i0.wp.com/test.jpg?query=dummy";
        String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 2, 1);

        assertThat(photonUrl, equalTo("https://i0.wp.com/test.jpg?strip=info&quality=65&resize=2,1"));

        imageUrl = "https://i0.wp.com/test.jpg?query=dummy&ssl=1";
        photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 2, 1);

        assertThat(photonUrl, equalTo("https://i0.wp.com/test.jpg?strip=info&quality=65&resize=2,1&ssl=1"));
    }

    @Test
    public void getPhotonImageUrlDoNotUseSslOnWordPressCom() {
        String imageUrl = "https://test.wordpress.com/test.jpg?query=dummy";
        String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 2, 1);

        assertThat(photonUrl, equalTo("https://test.wordpress.com/test.jpg?strip=info&quality=65&resize=2,1"));

        imageUrl = "https://test.wordpress.com/test.jpg?query=dummy&ssl=1";
        photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 2, 1);

        assertThat(photonUrl, equalTo("https://test.wordpress.com/test.jpg?strip=info&quality=65&resize=2,1"));
    }

    @Test
    public void getPhotonImageUrlUsesSslOnHttpsImageUrl() {
        String imageUrl = "http://mysite.com/test.jpg?query=dummy";
        String photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 2, 1);

        assertThat(photonUrl, equalTo("https://i0.wp.com/mysite.com/test.jpg?strip=info&quality=65&resize=2,1"));

        imageUrl = "https://mysite.com/test.jpg?query=dummy&ssl=1";
        photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 2, 1);

        assertThat(photonUrl, equalTo("https://i0.wp.com/mysite.com/test.jpg?strip=info&quality=65&resize=2,1&ssl=1"));
    }
}
