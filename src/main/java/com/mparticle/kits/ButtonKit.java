package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.mparticle.DeepLinkError;
import com.mparticle.DeepLinkResult;
import com.mparticle.MParticle;
import com.mparticle.kits.button.ButtonApi;
import com.mparticle.kits.button.ButtonLog;
import com.mparticle.kits.button.DeepLinkListener;
import com.mparticle.kits.button.DeferredDeepLinkHandler;
import com.mparticle.kits.button.HostInformation;
import com.mparticle.kits.button.IdentifierForAdvertiserProvider;
import com.mparticle.kits.button.Storage;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import static com.mparticle.kits.button.Constants.DeepLink;

/**
 * Minimal implementation of some of <a href="https://www.usebutton.com>Buttons</a> functionality, specifically:
 * <ul>
 * <li>Deferred deep linking</li>
 * </ul>
 */
public class ButtonKit extends KitIntegration implements KitIntegration.ActivityListener {

    private static final String TAG = "ButtonKit";
    private ButtonApi mApi;
    private Storage mStorage;
    private static final String ACTION_REFERRER = "com.android.vending.INSTALL_REFERRER";

    @Override
    public String getName() {
        return "Button";
    }

    @Override
    public ButtonKit getInstance() {
        return this;
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        final String applicationId = settings.get("application_id");
        if (KitUtils.isEmpty(applicationId)) {

            throw new IllegalArgumentException("No Button application ID provided, can't initialize kit.");
        }

        final HostInformation hostInformation = new HostInformation(context, applicationId);
        final IdentifierForAdvertiserProvider ifaProvider = new IdentifierForAdvertiserProvider(context);
        mApi = new ButtonApi(hostInformation, ifaProvider);
        mStorage = new Storage(context, applicationId);
        Uri data = MParticle.getInstance().getAppStateManager().getLaunchUri();
        String action = MParticle.getInstance().getAppStateManager().getLaunchAction();
        handleIntent(data, action);
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }

    @Override
    public void checkForDeepLink() {
        final DeepLinkListener onLink = new DeepLinkListener() {
            @Override
            public void onDeepLink(final Intent deepLinkIntent) {
                    DeepLinkResult result = new DeepLinkResult()
                            .setLink(deepLinkIntent.getDataString())
                            .setServiceProviderId(getConfiguration().getKitId());
                    getKitManager().onResult(result);
            }

            @Override
            public void onNoDeepLink() {
                DeepLinkError deepLinkError = new DeepLinkError()
                        .setMessage("No pending deep link. ")
                        .setServiceProviderId(getConfiguration().getKitId());
                getKitManager().onError(deepLinkError);
            }
        };
        new DeferredDeepLinkHandler(getContext(), mStorage, mApi, onLink).check();
    }

    @Override
    public void setInstallReferrer(final Intent intent) {
        super.setInstallReferrer(intent);
        if (intent == null) {
            return;
        }
        try {
            if (!ACTION_REFERRER.equals(intent.getAction())) {
                ButtonLog.warnFormat(TAG, "Expected action '%s', but got action: '%s'", ACTION_REFERRER, intent.getAction());
                return;
            }
            final String referrerRaw = intent.getStringExtra("referrer");
            Log.d(TAG, "Received install referrer: " + referrerRaw);
            if (KitUtils.isEmpty(referrerRaw)) {
                ButtonLog.visible("Recorded installation without referrer, ignore.");
                return;
            }
            final String existingReferrer = mStorage.getInstallReferrer();
            if (!KitUtils.isEmpty(existingReferrer)) {
                ButtonLog.visibleFormat("Installation already attributed, ignoring new value. (Existing: %s, Ignored: %s)", existingReferrer, referrerRaw);
                return;
            }
            String referrer;
            try {
                referrer = URLDecoder.decode(referrerRaw, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Let's fallback to raw value if decoding fails.
                Log.e(TAG, "Failed to decode referrer '" + referrerRaw + "'", e);
                referrer = referrerRaw;
            }
            ButtonLog.visibleFormat("Recorded installation with referrer. (Referrer: %s, Raw: %s).", referrer, referrerRaw);
            mStorage.setInstallReferrer(referrer);
        } catch (Throwable caught) {
            ButtonLog.warn(TAG, "Exception while handling install referrer intent." + intent, caught);
        }
    }

    /**
     * Get the Button referrer token which should be attached to any orders reported and attributed
     * to the Button network.
     *
     * <pre>
     * final ButtonKit button = (ButtonKit) MParticle.getInstance().getKitInstance(MParticle.ServiceProviders.BUTTON);
     * if (button != null) {
     * final String referrerToken = button.getReferrerToken();
     * if (referrerToken != null) {
     *      // Use token with your order reporting.
     *  }
     * }
     * </pre>
     *
     * @return an opaque referrer string or null.
     */
    public String getReferrerToken() {
        return mStorage.getReferrer();
    }

    private List<ReportingMessage> handleIntent(final Uri data, final String action) {
        if (data == null || !Intent.ACTION_VIEW.equals(action)) {
            return null;
        }

        handleIntentData(data);
        return null;
    }


    protected void handleIntentData(final Uri data) {
        /**
         * opaque Urls (<scheme>:<scheme specific part>#<fragment>) throw on access to query parameters
         * let's ignore all of these.
         *
         * @see Uri#getQueryParameter(String)
         */
        if (data == null || data.isOpaque()) {
            return;
        }
        final String referrer = data.getQueryParameter(DeepLink.QUERY_REFERRER);
        final String compatReferrer = data.getQueryParameter(DeepLink.QUERY_REFERRER_COMPAT);
        final String referrerValue = referrer != null ? referrer : compatReferrer;
        if (referrerValue == null) {
            return;
        }
        ButtonLog.visibleFormat("Deep link received (Attribution token: %s)", referrerValue);
        ButtonLog.verboseFormat(TAG, "Incoming deep link: %s", data.toString());
        doChangeReferrer(referrerValue);
    }

    protected void doChangeReferrer(final String referrer) {
        // We only want to track and change when the referrer actually changes.
        if (referrer == null) {
            return;
        }
        if (referrer.equals(mStorage.getReferrer())) return;
        mStorage.setReferrer(referrer);
    }

    @Override
    public List<ReportingMessage> onActivityCreated(final Activity activity, final Bundle savedInstanceState) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(final Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) {
            return null;
        }
        return handleIntent(intent.getData(), intent.getAction());
    }

    @Override
    public List<ReportingMessage> onActivityResumed(final Activity activity) { return null; }

    @Override
    public List<ReportingMessage> onActivityPaused(final Activity activity) { return null; }

    @Override
    public List<ReportingMessage> onActivityStopped(final Activity activity) { return null; }

    @Override
    public List<ReportingMessage> onActivitySaveInstanceState(final Activity activity, final Bundle outState) { return null; }

    @Override
    public List<ReportingMessage> onActivityDestroyed(final Activity activity) { return null; }
}
