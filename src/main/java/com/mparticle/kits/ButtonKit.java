package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.usebutton.merchant.ButtonMerchant;
import com.usebutton.merchant.Order;
import com.usebutton.merchant.PostInstallIntentListener;
import com.usebutton.merchant.UserActivityListener;

import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Logger;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;

/**
 * MParticle embedded implementation of the <a href="https://github.com/button/button-merchant-android">Button Merchant Library</a>.
 *
 * Learn more at our <a href="https://developer.usebutton.com/guides/merchants/android/button-merchant-integration-guide">Developer Docs</a>
 */
public class ButtonKit extends KitIntegration implements
        KitIntegration.CommerceListener,
        KitIntegration.IdentityListener,
        KitIntegration.ActivityListener {

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
        final String applicationId = settings.get("application_id");
        if (KitUtils.isEmpty(applicationId)) {
            throw new IllegalArgumentException(
                    "No Button application ID provided, can't initialize kit."
            );
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            throw new IllegalArgumentException(
                    "App running in an < API15 environment, can't initialize kit."
            );
        }

        ButtonMerchant.configure(getApplicationContext(), applicationId);
        logDebug("Button Attribution Token: %s", getAttributionToken());

        ButtonMerchant.addAttributionTokenListener(getApplicationContext(),
                new ButtonMerchant.AttributionTokenListener() {
                    @Override
                    public void onAttributionTokenChanged(@NonNull String s) {
                        logDebug("Refreshed Button Attribution Token: %s", s);
                    }
                });

        ButtonMerchant.handlePostInstallIntent(getApplicationContext(),
                new PostInstallIntentListener() {
                    @Override
                    public void onResult(@Nullable Intent intent, @Nullable Throwable throwable) {
                        PackageManager pm = getApplicationContext().getPackageManager();
                        if (intent != null && intent.resolveActivity(pm) != null) {
                            logDebug("Handling post-install intent for %s", intent.toString());
                            getApplicationContext().startActivity(intent);
                        } else if (throwable != null) {
                            logError("Error checking post install intent", throwable);
                        }
                    }
                });
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
     * The attribution token from the last inbound Button attributed {@link Intent}.
     *
     * For attribution to work correctly, you must:
     * <ul>
     * <li>Always access this token directly—*never cache it*.</li>
     * <li>Never manage the lifecycle of this token—Button manages the token validity window
     * server-side.</li>
     * <li>Always include this value when reporting orders to your order API.</li>
     * </ul>
     *
     * @return the last tracked Button attribution token.
     **/
    @Nullable
    public String getAttributionToken() {
        return ButtonMerchant.getAttributionToken(getApplicationContext());
    }

    /**
     * Tracks orders
     *
     * @param order {@link Order}
     */
    public void trackOrder(@NonNull final Order order) {
        trackOrder(order, new UserActivityListener() {
            @Override
            public void onResult(@Nullable Throwable throwable) {
                if (throwable != null) {
                    logError("Unable to track order", throwable);
                } else {
                    logDebug("Successfully tracked order %s", order.getId());
                }
            }
        });
    }

    /**
     * Tracks orders
     *
     * @param order {@link Order}
     * @param listener {@link UserActivityListener}
     */
    public void trackOrder(@NonNull Order order, @Nullable UserActivityListener listener) {
        logDebug("Beginning to track order %s", order.getId());
        ButtonMerchant.trackOrder(getApplicationContext(), order, listener);
    }

    /**
     * Checks the passed {@code Intent} for a Button attribution and if present stores the token.
     *
     * @param intent An intent that has entered your app from a third party source.
     */
    public void trackIncomingIntent(@NonNull Intent intent) {
        ButtonMerchant.trackIncomingIntent(getApplicationContext(), intent);
    }

    /**
     * Discards the current session and all persisted data.
     */
    public void clearAllData() {
        logDebug("Clearing Button session.");
        ButtonMerchant.clearAllData(getApplicationContext());
    }

    /*
     * Overrides for the CommerceListener
     */

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal bigDecimal, BigDecimal bigDecimal1,
            String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {

        if (!KitUtils.isEmpty(event.getProductAction()) &&
                event.getProductAction().equalsIgnoreCase(Product.PURCHASE) &&
                !event.getProducts().isEmpty()) {

            // Prepare the order and track it to Button
            final Order order = prepareOrder(event);
            if (order != null) {
                trackOrder(order);
            }
        }
        return null;
    }

    private Order prepareOrder(CommerceEvent event) {
        logDebug("Preparing order for %s", event.getTransactionAttributes().getId());

        String orderId = event.getTransactionAttributes().getId();
        String currCode = event.getCurrency();
        if (KitUtils.isEmpty(currCode)) {
            currCode = CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE;
        }

        int e = Currency.getInstance(currCode).getDefaultFractionDigits();
        if (e < 0) {
            logError(currCode + " is not a valid currency code. Please conform to ISO4217.");
            return null;
        }
        double total = 0.00;
        for (Product product : event.getProducts()) {
            total += product.getUnitPrice();
        }
        long orderTotal = Math.round(total * Math.pow(10, e));

        return new Order.Builder(orderId)
                .setCurrencyCode(currCode)
                .setAmount(orderTotal)
                .build();
    }

    /*
     * Overrides for IdentityListener
     */

    @Override
    public void onIdentifyCompleted(MParticleUser mParticleUser,
            FilteredIdentityApiRequest filteredIdentityApiRequest) {

    }

    @Override
    public void onLoginCompleted(MParticleUser mParticleUser,
            FilteredIdentityApiRequest filteredIdentityApiRequest) {

    }

    @Override
    public void onLogoutCompleted(MParticleUser mParticleUser,
            FilteredIdentityApiRequest filteredIdentityApiRequest) {
        clearAllData();
    }

    @Override
    public void onModifyCompleted(MParticleUser mParticleUser,
            FilteredIdentityApiRequest filteredIdentityApiRequest) {

    }

    @Override
    public void onUserIdentified(MParticleUser mParticleUser) {

    }

    /*
     * Overrides for ActivityListener
     */

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, Bundle bundle) {
        trackIncomingIntent(activity.getIntent());
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity) {
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

    private static void logDebug(String message, Object... args) {
        if (MParticle.getInstance().getEnvironment() == MParticle.Environment.Development) {
            Logger.debug(String.format("ButtonKit: " + message, args));
        }
    }

    private static void logError(String message) {
        if (MParticle.getInstance().getEnvironment() == MParticle.Environment.Development) {
            Logger.error("ButtonKit: " + message);
        }
    }

    private static void logError(String message, Throwable t) {
        if (MParticle.getInstance().getEnvironment() == MParticle.Environment.Development) {
            Logger.error(t, "ButtonKit: " + message);
        }
    }

    private Context getApplicationContext() {
        return getContext().getApplicationContext();
    }
}
