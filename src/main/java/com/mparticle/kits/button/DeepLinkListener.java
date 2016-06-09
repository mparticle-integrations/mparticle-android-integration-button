package com.mparticle.kits.button;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Callback interface for {@link DeferredDeepLinkHandler} which
 * will invoke {@link #onDeepLink(Intent)} with a VIEW intent for a deferred deep link if such
 * is found. This will only happen once and after a new installation, for all other scenarios
 * {@link #onNoDeepLink()} will called.
 */
public interface DeepLinkListener {
    /**
     * Called if this is the application's first launch and a deferred deep link is
     * available from Button. In all other cases, {@link #onNoDeepLink()} is called instead.
     *
     * Implementations should
     * call {@link Activity#startActivity(Intent)} to continue. The pending deep link can be retrieved
     * with {@link Intent#getData()} in the target Activity's {@link Activity#onCreate(Bundle)}.
     *
     * @param deepLinkIntent
     */
    void onDeepLink(final Intent deepLinkIntent);

    /**
     * @see #onDeepLink(Intent)
     */
    void onNoDeepLink();
}