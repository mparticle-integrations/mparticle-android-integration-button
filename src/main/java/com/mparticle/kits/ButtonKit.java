package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.usebutton.merchant.ButtonMerchant;
import com.usebutton.merchant.PostInstallIntentListener;

import com.mparticle.AttributionError;
import com.mparticle.AttributionResult;
import com.mparticle.internal.Logger;

import java.util.List;
import java.util.Map;

/**
 * MParticle embedded implementation of the <a href="https://github.com/button/button-merchant-android">Button Merchant Library</a>.
 *
 * Learn more at our <a href="https://developer.usebutton.com/guides/merchants/android/button-merchant-integration-guide">Developer Docs</a>
 */
public class ButtonKit extends KitIntegration implements KitIntegration.ActivityListener,
        ButtonMerchant.AttributionTokenListener, PostInstallIntentListener {

    static final String ATTRIBUTE_REFERRER = "com.usebutton.source_token";

    private Context applicationContext;
    @VisibleForTesting
    ButtonMerchantWrapper merchant = new ButtonMerchantWrapper();

    @Override
    public String getName() {
        return "Button";
    }

    @Override
    public ButtonKit getInstance() {
        return this;
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, final Context ctx) {
        applicationContext = ctx.getApplicationContext();
        final String applicationId = settings.get("application_id");
        if (KitUtils.isEmpty(applicationId)) {
            throwOnKitCreateError("No Button application ID provided, can't initialize kit.");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            throwOnKitCreateError("App running in an < API 15 environment, can't initialize kit.");
        }

        merchant.configure(applicationContext, applicationId);
        merchant.addAttributionTokenListener(applicationContext, this);
        merchant.handlePostInstallIntent(applicationContext, this);
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }

    /*
     * Public methods to expose important Merchant Library methods
     */

    /**
     * Get the Button attribution token which should be attached to any orders reported and
     * attributed to the Button network.
     *
     * For attribution to work correctly, you must:
     * <ul>
     * <li>Always access this token directly—*never cache it*.</li>
     * <li>Never manage the lifecycle of this token—Button manages the token validity window
     * server-side.</li>
     * <li>Always include this value when reporting orders to your order API.</li>
     * </ul>
     *
     * <pre>
     * ButtonKit button = (ButtonKit) MParticle.getInstance()
     *         .getKitInstance(MParticle.ServiceProviders.BUTTON);
     * if (button != null) {
     *     String attributionToken = button.getAttributionToken();
     *     if (attributionToken != null) {
     *         // Use token with your order reporting.
     *     }
     * }
     * </pre>
     *
     * @return the last tracked Button attribution token.
     **/
    @Nullable
    public String getAttributionToken() {
        return merchant.getAttributionToken(applicationContext);
    }

    @Override
    public void onAttributionTokenChanged(@NonNull String token) {
        final Map<String, String> attributes = getIntegrationAttributes();
        attributes.put(ATTRIBUTE_REFERRER, token);
        setIntegrationAttributes(attributes);
        logDebug("Refreshed Button Attribution Token: %s", token);
    }

    @Override
    public void onResult(@Nullable Intent intent, @Nullable Throwable throwable) {
        PackageManager pm = applicationContext.getPackageManager();
        if (intent != null && intent.resolveActivity(pm) != null) {
            logDebug("Handling post-install intent for %s", intent.toString());
            AttributionResult result = new AttributionResult()
                    .setLink(intent.getDataString())
                    .setServiceProviderId(getConfiguration().getKitId());
            getKitManager().onResult(result);
        } else {
            AttributionError attributionError = new AttributionError()
                    .setMessage("No pending attribution link.")
                    .setServiceProviderId(getConfiguration().getKitId());
            getKitManager().onError(attributionError);
        }

        if (throwable != null) {
            logError("Error checking post install intent", throwable);
        }
    }

    /*
     * Overrides for ActivityListener
     */

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, Bundle bundle) {
        merchant.trackIncomingIntent(applicationContext, activity.getIntent());
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity) {
        merchant.trackIncomingIntent(applicationContext, activity.getIntent());
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityDestroyed(Activity activity) {
        return null;
    }

    /*
     * Utility methods
     */

    private void logDebug(String message, Object... args) {
        Logger.debug(String.format("ButtonKit: " + message, args));
    }

    private void logError(String message, Throwable t) {
        Logger.error(t, "ButtonKit: " + message);
    }

    private void throwOnKitCreateError(String message) {
        throw new IllegalArgumentException(message);
    }
}
