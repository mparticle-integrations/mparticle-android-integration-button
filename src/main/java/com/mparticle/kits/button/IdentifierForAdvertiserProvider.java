package com.mparticle.kits.button;

import android.content.Context;
import android.provider.Settings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/** @hide */
public class IdentifierForAdvertiserProvider {

    private static final String TAG = "IFAProvider";
    private static final long IFA_TTL = TimeUnit.HOURS.toMillis(1);
    private Context mContext;
    private TtlReference<AdvertisingInfoReflectionProxy> mReflectionProxyReference;

    /**
     * This class can be used to get Advertising ID from the Google Play Services library via reflection.
     * Will fail silently if not on classpath, so make sure you have this dependency in your build.gradle:
     * {@code compile 'com.google.android.gms:play-services-ads:7.5.0}
     * @param context
     */
    public IdentifierForAdvertiserProvider(final Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * This call is blocking and should _not_ be run from the main thread.
     * For more info: https://developer.android.com/google/play-services/id.html
     *
     * @return the Advertising ID if available (Google Play Services on classpath and user have not disabled tracking).
     */
    public String getPrimaryIdentifier() {
        AdvertisingInfoReflectionProxy mReflectionProxy = getIdentifierProxy();
        if (mReflectionProxy.isAdTrackingLimited(mContext)) {
            return null;
        }
        return mReflectionProxy.getTrackingIdentifier(mContext);
    }

    private AdvertisingInfoReflectionProxy getIdentifierProxy() {
        if (mReflectionProxyReference == null || mReflectionProxyReference.isDead()) {
            mReflectionProxyReference = new TtlReference(TtlReference.REALTIME_MILLIS_PROVIDER, new AdvertisingInfoReflectionProxy(), IFA_TTL);
        }
        return mReflectionProxyReference.get();
    }

    /**
     * @return {@link Settings.Secure#ANDROID_ID} unless ad tracking have been limited
     * by user through Google Settings.
     */
    public String getSecondaryIdentifier() {
        if (getIdentifierProxy().isAdTrackingLimited(mContext)) {
            return null;
        }
        return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Intented to keep a cache of methods and objects accessed via reflection for faster access, but still dynamic enough to get up to date value each time.
     */
    private class AdvertisingInfoReflectionProxy {
        private boolean mNeuted = false;
        private Method mGetAdvertisingIdInfoMethod;
        private Class<?> mAdvertisingClient;

        AdvertisingInfoReflectionProxy() {
            try {
                mAdvertisingClient = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                mGetAdvertisingIdInfoMethod = mAdvertisingClient.getDeclaredMethod("getAdvertisingIdInfo", Context.class);
                return;
            } catch (Exception e) {
                ButtonLog.warn(TAG, "Could not resolve IFA.", e);
            }
            mNeuted = true;
        }

        boolean isAdTrackingLimited(final Context context)  {
            if (mNeuted) return false;
            final Object adInfo;
            try {
                adInfo = getAdInfo(context);
                return (Boolean) adInfo.getClass().getMethod("isLimitAdTrackingEnabled").invoke(adInfo);
            } catch (Exception e) {
                ButtonLog.warn(TAG, "Couldn't check if tracking was limited.", e);
            }
            return false;
        }

        String getTrackingIdentifier(final Context context) {
            if (mNeuted) return null;
            final Object adInfo;
            try {
                adInfo = getAdInfo(context);
                return (String) adInfo.getClass().getMethod("getId").invoke(adInfo);
            } catch (Exception e) {
                ButtonLog.warn(TAG, "Could not get ID from ad info object.", e);
            }
            return null;
        }

        private Object getAdInfo(final Context context) throws IllegalAccessException, InvocationTargetException {
            return mGetAdvertisingIdInfoMethod.invoke(mAdvertisingClient, context);
        }
    }
}
