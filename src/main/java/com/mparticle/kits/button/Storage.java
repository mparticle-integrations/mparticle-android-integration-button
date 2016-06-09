package com.mparticle.kits.button;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SDK-common storage interface.
 */
public class Storage {

    private static final String KEY_SESSION_ID = "session-id";
    private static final String KEY_INSTALL_REFERRER = "install-referrer";
    private static final String KEY_DEFERRED_CHECKED = "deferred-checked";
    private static final String KEY_REFERRER = "referrer";

    private final SharedPreferences mSharedPreferences;
    private final String mApplicationId;

    public Storage(final Context context, final String applicationId) {
        mSharedPreferences = context.getSharedPreferences("btnprefs-mparticle", Context.MODE_PRIVATE);
        mApplicationId = applicationId;
    }

    /**
     * Retrieves the previously-saved session id, or {@code null}.
     */
    public String getSessionId() {
        return mSharedPreferences.getString(keyFor(KEY_SESSION_ID), null);
    }

    private String keyFor(final String key) {
        return String.format("%s.%s", mApplicationId, key);
    }

    /**
     * Sets the session id.
     */
    public void setSessionId(final String sessionId) {
        mSharedPreferences.edit().putString(keyFor(KEY_SESSION_ID), sessionId).apply();
    }

    /**
     * This method is for storing the Play Store referrer if any is provided through the installation
     * process (market://details?id=com.app&referrer=tracking_id%3D123456789
     * @param installReferrer
     */
    public void setInstallReferrer(final String installReferrer) {
        if (installReferrer == null) {
            return;
        }
        mSharedPreferences.edit().putString(keyFor(KEY_INSTALL_REFERRER), installReferrer).apply();
    }

    /**
     * Will return this app's installation referrer if one was provided during installation via the
     * referrer URL parameter on the Google Play Store link.
     *
     * @return referrer value or null.
     */
    public String getInstallReferrer() {
        return mSharedPreferences.getString(keyFor(KEY_INSTALL_REFERRER), null);
    }

    /**
     * Should be used to mark that an attempt was made to get a deferrred deep link to prevent
     * subsequent calls.
     */
    public void markCheckedDeferredDeepLink() {
        mSharedPreferences.edit().putBoolean(keyFor(KEY_DEFERRED_CHECKED), true).apply();
    }

    /**
     * Will return true if the SDK have ever checked for a deferred deep link.
     * @return
     */
    public boolean didCheckForDeferredDeepLink() {
        return mSharedPreferences.getBoolean(keyFor(KEY_DEFERRED_CHECKED), false);
    }


    /**
     * Retrieves the previously-saved referrer, or {@code null}.
     */
    public String getReferrer() {
        return mSharedPreferences.getString(keyFor(KEY_REFERRER), null);
    }

    /**
     * Sets the referrer received from an inbound deeplink.
     */
    public void setReferrer(final String referrer) {
        if (referrer == null) {
            return;
        }
        mSharedPreferences.edit().putString(keyFor(KEY_REFERRER), referrer).apply();
    }
}
