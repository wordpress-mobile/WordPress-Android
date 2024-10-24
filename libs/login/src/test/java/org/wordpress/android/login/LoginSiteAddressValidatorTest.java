package org.wordpress.android.login;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.util.helpers.Debouncer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class LoginSiteAddressValidatorTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private Debouncer mDebouncer;
    private LoginSiteAddressValidator mValidator;

    @Before
    public void setUp() {
        mDebouncer = mock(Debouncer.class);
        doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock invocation) {
                final Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            }
        }).when(mDebouncer).debounce(any(), any(Runnable.class), anyLong(), any(TimeUnit.class));

        mValidator = new LoginSiteAddressValidator(mDebouncer);
    }

    @After
    public void tearDown() {
        mValidator = null;
        mDebouncer = null;
    }

    @Test
    public void testAnErrorIsReturnedWhenGivenAnInvalidAddress() {
        // Arrange
        assertThat(mValidator.getErrorMessageResId().getValue()).isNull();

        // Act
        mValidator.setAddress("invalid");

        // Assert
        assertThat(mValidator.getErrorMessageResId().getValue()).isNotNull();
        assertThat(mValidator.getCleanedSiteAddress()).isEqualTo("invalid");
        assertThat(mValidator.getIsValid().getValue()).isFalse();
    }

    @Test
    public void testNoErrorIsReturnedButIsInvalidWhenGivenAnEmptyAddress() {
        // Act
        mValidator.setAddress("");

        // Assert
        assertThat(mValidator.getErrorMessageResId().getValue()).isNull();
        assertThat(mValidator.getIsValid().getValue()).isFalse();
        assertThat(mValidator.getCleanedSiteAddress()).isEqualTo("");
    }

    @Test
    public void testTheErrorIsImmediatelyClearedWhenANewAddressIsGiven() {
        // Arrange
        final ArrayList<Optional<Integer>> resIdValues = new ArrayList<>();
        mValidator.getErrorMessageResId().observeForever(new Observer<Integer>() {
            @Override public void onChanged(Integer resId) {
                resIdValues.add(Optional.ofNullable(resId));
            }
        });

        // Act
        mValidator.setAddress("invalid");
        mValidator.setAddress("another-invalid");

        // Assert
        assertThat(resIdValues).hasSize(4);
        assertThat(resIdValues.get(0)).isEmpty();
        assertThat(resIdValues.get(1)).isNotEmpty();
        assertThat(resIdValues.get(2)).isEmpty();
        assertThat(resIdValues.get(3)).isNotEmpty();
    }

    @Test
    public void testItReturnsValidWhenGivenValidURLs() {
        // Arrange
        final List<String> validUrls = Arrays.asList(
                "http://subdomain.example.com",
                "http://example.ca",
                "example.ca",
                "subdomain.example.com",
                "  space-with-subdomain.example.net",
                "https://subdomain.example.com/folder",
                "http://subdomain.example.com/folder/over/there ",
                "7.7.7.7",
                "http://7.7.13.45",
                "http://47.147.43.45/folder   ");

        // Act and Assert
        assertThat(validUrls).allSatisfy(new Consumer<String>() {
            @Override public void accept(String url) {
                mValidator.setAddress(url);

                assertThat(mValidator.getErrorMessageResId().getValue()).isNull();
                assertThat(mValidator.getIsValid().getValue()).isTrue();
            }
        });
    }
}
