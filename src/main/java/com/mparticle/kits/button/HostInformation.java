package com.mparticle.kits.button;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import com.mparticle.BuildConfig;

import java.util.Locale;

/**
 * Accessor class to miscellaneous device, SDK and host application information
 */
public class HostInformation {
    private final PackageInfo mHostApplication;
    private final String mUserAgent;
    private final float mDensity;
    private String mApplicationId;

    public HostInformation(final Context context, final String applicationId) {
        mHostApplication = ButtonUtil.getPackageInfo(context, 0);
        mApplicationId = applicationId;
        mDensity = context.getResources().getDisplayMetrics().density;
        mUserAgent = new ApiUtil(this).getUserAgent();
    }

    public String getSdkVersionName() {
        return com.mparticle.BuildConfig.VERSION_NAME;
    }

    public int getSdkVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    public String getAndroidVersionName() {
        return Build.VERSION.RELEASE;
    }

    public String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    public String getDeviceModel() {
        return Build.MODEL;
    }

    public String getPackageName() {
        return mHostApplication.packageName;
    }

    public String getVersionName() {
        return mHostApplication.versionName;
    }

    public int getVersionCode() {
        return mHostApplication.versionCode;
    }

    public Locale getLocale() {
        return Locale.getDefault();
    }

    public String getUserAgent() {
        return mUserAgent;
    }

    public String getBaseUrl() {
        return "https://api.usebutton.com";
    }

    public String getApplicationId() {
        return mApplicationId;
    }

    @Override
    public String toString() {
        return "HostInformation{" +
                "mHostApplication=" + mHostApplication +
                ", mUserAgent='" + mUserAgent + '\'' +
                ", mApplicationId='" + mApplicationId + '\'' +
                ", mDensity='" + mDensity + '\'' +
                '}';
    }

    public float getScreenDensity() {
        return mDensity;
    }
}
