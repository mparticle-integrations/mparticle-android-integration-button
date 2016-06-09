package com.mparticle.kits.button;

import android.content.Context;

import java.util.Locale;

/**
 * Miscellaneous utilities related to the Button DLC API.
 */
public class ApiUtil {

    private final HostInformation mHostInformation;

    public ApiUtil(HostInformation hostInformation) {
        mHostInformation = hostInformation;
    }

    /**
     * Returns a <a href="https://docs.google.com/a/usebutton.com/document/d/1YB5cIkXjkAHjJ5_JkytvSawgNOPSmaCOV-NdxDVOynw/edit">
     *     Button User-Agent</a> for the current SDK and host app.
     */
    public static String getUserAgent(final Context context, final String applicationId) {
        return new ApiUtil(new HostInformation(context, applicationId)).getUserAgent();
    }

    public String getUserAgent() {
        // $App/$Version ($OS $OSVersion; $HardwareType; $MerchantId/$MerchantVersion; Scale/$screenScale; )

        // "com.mparticle.button/1.0.1-12 "
        final StringBuilder sb = new StringBuilder();
        sb.append("com.mparticle.button/");
        sb.append(mHostInformation.getSdkVersionName());
        sb.append('-');
        sb.append(mHostInformation.getSdkVersionCode());
        sb.append(' ');

        // "(Android 4.0.1; "
        sb.append("(Android ");
        sb.append(mHostInformation.getAndroidVersionName());
        sb.append("; ");

        // "Samsung Galaxy S5; "
        sb.append(mHostInformation.getDeviceManufacturer());
        sb.append(' ');
        sb.append(mHostInformation.getDeviceModel());
        sb.append("; ");

        // "com.example.wheelsapp/1.2.3-41; "
        sb.append(mHostInformation.getPackageName());
        sb.append('/');
        sb.append(mHostInformation.getVersionName());
        sb.append('-');
        sb.append(mHostInformation.getVersionCode());
        sb.append("; ");

        // "Scale/2.0;
        sb.append(String.format(Locale.US, "Scale/%.1f; ", mHostInformation.getScreenDensity()));

        // en_US)
        final Locale locale = mHostInformation.getLocale();
        sb.append(locale.getLanguage()).append('-').append(locale.getCountry()).append(')');

        return sb.toString();
    }

}
