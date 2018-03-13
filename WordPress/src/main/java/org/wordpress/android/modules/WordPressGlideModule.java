package org.wordpress.android.modules;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.volley.VolleyUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;

import org.wordpress.android.WordPress;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Custom {@link AppGlideModule} that replaces Glide's default {@link RequestQueue} with FluxC's.
 */
@GlideModule
public class WordPressGlideModule extends AppGlideModule {
    @Inject @Named("custom-ssl") RequestQueue mRequestQueue;

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        ((WordPress) context).component().inject(this);
        glide.getRegistry().replace(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(mRequestQueue));
    }
}
