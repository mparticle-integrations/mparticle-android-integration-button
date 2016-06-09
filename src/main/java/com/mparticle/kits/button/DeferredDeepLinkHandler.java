package com.mparticle.kits.button;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.WindowManager;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DeferredDeepLinkHandler implements FailableReceiver<DeferredDeepLinkDTO> {
    private static final String TAG = "DeferredHandler";
    private final PackageManager mPackageManager;
    private final DeepLinkListener mCallback;
    private final String mOwnPackage;
    private final Storage mStorage;
    private final ButtonApi mApi;
    private final WindowManager mWindowManager;
    private final IdentifierForAdvertiserProvider mIfaProvider;

    public DeferredDeepLinkHandler(final Context context,
                                   final Storage storage,
                                   final ButtonApi api,
                                   final DeepLinkListener callback) {
        mPackageManager = context.getPackageManager();
        mOwnPackage = context.getPackageName();
        mStorage = storage;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mApi = api;
        mIfaProvider = mApi.getIdentifierForAdvertiserProvider();
        mCallback = callback;
    }

    @Override
    public void onResult(final DeferredDeepLinkDTO result) {
        handleAttribution(result);
        if (isValidDeepLink(result)) {
            trackDeepLink(result);
            final Intent deepLinkIntent = intentForUri(result.action);
            if (mPackageManager.queryIntentActivities(deepLinkIntent, 0).isEmpty()) {
                // This should not happen, but means that our app can't open this URI
                ButtonLog.warn(TAG, "Couldn't find any activities to open " + deepLinkIntent);
                mCallback.onNoDeepLink();
            }
            else {
                mCallback.onDeepLink(deepLinkIntent);
            }
        }
        else {
            ButtonLog.visible("No deferred deep link found.");
            mCallback.onNoDeepLink();
        }
    }

    private void handleAttribution(final DeferredDeepLinkDTO result) {
        if (result == null || result.attribution == null) {
            return;
        }
        mStorage.setReferrer(result.attribution.btnRef);
    }

    private void trackDeepLink(final DeferredDeepLinkDTO result) {
        String btnRef = null;
        String utmSource = null;
        if (result.attribution != null) {
            btnRef = result.attribution.btnRef;
            utmSource = result.attribution.utmSource;
        }
        ButtonLog.visibleFormat("Deferred deep link found. (Link: %s, Referrer: %s, UTM Source: %s, ID: %s)",
                result.action, btnRef, utmSource, result.id);
    }

    private boolean isValidDeepLink(final DeferredDeepLinkDTO result) {
        return result != null && result.match && result.action != null;
    }

    @Override
    public void onError() {
        ButtonLog.visible("No deferred deep link found.");
        mCallback.onNoDeepLink();
    }

    public Intent intentForUri(final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(mOwnPackage);
        ButtonLog.verbose(TAG, "Created VIEW intent: " + intent);
        return intent;
    }

    public void check() {
        if (mStorage.didCheckForDeferredDeepLink()) {
            mCallback.onNoDeepLink();
            return;
        }
        mStorage.markCheckedDeferredDeepLink();
        if (isOldInstallation()) {
            mCallback.onNoDeepLink();
            return;
        }

        new AsyncTask<Void, Void, DeferredDeepLinkDTO>(){
            @Override
            protected DeferredDeepLinkDTO doInBackground(final Void... params) {
                try {
                    return new CheckPendingLinkCommand(DeferredDeepLinkHandler.this, mStorage, mApi, mWindowManager).execute();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final DeferredDeepLinkDTO deferredDeepLinkDTO) {
                super.onPostExecute(deferredDeepLinkDTO);
                if (deferredDeepLinkDTO == null) {
                    onError();
                }
                else {
                    onResult(deferredDeepLinkDTO);
                }
            }
        }.execute();
    }


    /**
     * This will check if the app was installed more than 12 hours ago and hence likely not
     * to be considered interesting for us e.g. from an attribution point of view.
     *
     * @return true if the app was installed more than 12 hours ago.
     */
    private boolean isOldInstallation() {
        try {
            final PackageInfo info = mPackageManager.getPackageInfo(mOwnPackage, 0);
            if (info != null && info.firstInstallTime + TimeUnit.HOURS.toMillis(12) < System.currentTimeMillis()) {
                // More than 12 hours since we were installed
                ButtonLog.infoFormat(TAG, "Found app info for %s, first time installed %s",
                        mOwnPackage, new Date(info.firstInstallTime));
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // We don't really care about this
        }
        return false;
    }
}
