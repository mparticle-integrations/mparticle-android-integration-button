package com.mparticle.kits.button;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Callback interface for {@link DeferredAttributionHandler} which
 * will invoke {@link #onAttribution(Intent)} with a VIEW intent for a deferred attribution if such
 * is found. This will only happen once and after a new installation, for all other scenarios
 * {@link #onNoAttribution()} will called.
 */
public interface AttributionListener {
    /**
     * Called if this is the application's first launch and a deferred attribution is
     * available from Button. In all other cases, {@link #onNoAttribution()} is called instead.
     *
     * Implementations should
     * call {@link Activity#startActivity(Intent)} to continue. The pending attribution can be retrieved
     * with {@link Intent#getData()} in the target Activity's {@link Activity#onCreate(Bundle)}.
     *
     * @param attributionIntent
     */
    void onAttribution(final Intent attributionIntent);

    /**
     * @see #onAttribution(Intent)
     */
    void onNoAttribution();
}