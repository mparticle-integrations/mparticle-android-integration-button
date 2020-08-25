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
import com.usebutton.merchant.ButtonProduct;
import com.usebutton.merchant.ButtonProductCompatible;
import com.usebutton.merchant.PostInstallIntentListener;

import com.mparticle.AttributionError;
import com.mparticle.AttributionResult;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MParticle embedded implementation of the <a href="https://github.com/button/button-merchant-android">Button Merchant Library</a>.
 *
 * Learn more at our <a href="https://developer.usebutton.com/guides/merchants/android/button-merchant-integration-guide">Developer Docs</a>
 */
public class ButtonKit extends KitIntegration implements KitIntegration.ActivityListener,
        KitIntegration.CommerceListener, KitIntegration.IdentityListener,
        ButtonMerchant.AttributionTokenListener, PostInstallIntentListener {

    public static final String ATTRIBUTE_REFERRER = "com.usebutton.source_token";

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

    @Override
    protected void reset() {
        super.reset();
        merchant.clearAllData(applicationContext);
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
        }

        if (throwable != null) {
            logError("Error checking post install intent", throwable);
            AttributionError attributionError = new AttributionError()
                    .setMessage(throwable.getMessage())
                    .setServiceProviderId(getConfiguration().getKitId());
            getKitManager().onError(attributionError);
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
        merchant.trackIncomingIntent(applicationContext, activity.getIntent());
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
     * Overrides for CommerceListener
     */

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal bigDecimal, BigDecimal bigDecimal1,
            String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent commerceEvent) {
        if (commerceEvent == null
                || commerceEvent.getProductAction() == null
                || commerceEvent.getProducts() == null) {
            return null;
        }

        List<ButtonProductCompatible> products = parseAsButtonProducts(commerceEvent.getProducts());
        ButtonProductCompatible product = products.isEmpty() ? new ButtonProduct(): products.get(0);

        switch (commerceEvent.getProductAction()) {
            case Product.DETAIL:
                logDebug("Tracking product viewed: %s", product.getName());
                merchant.trackProductViewed(product);
                break;
            case Product.ADD_TO_CART:
                logDebug("Tracking product added to cart: %s", product.getName());
                merchant.trackAddToCart(product);
                break;
            case Product.CHECKOUT:
                logDebug("Tracking cart viewed with %d products!", products.size());
                merchant.trackCartViewed(products);
                break;
            default:
                logDebug("Product Action [%s] is not yet supported by the Button Merchant Library",
                        commerceEvent.getProductAction());
        }
        return null;
    }

    /*
     * Overrides for IdentityListener
     */

    @Override
    public void onIdentifyCompleted(MParticleUser mParticleUser,
            FilteredIdentityApiRequest filteredIdentityApiRequest) {}

    @Override
    public void onLoginCompleted(MParticleUser mParticleUser,
            FilteredIdentityApiRequest filteredIdentityApiRequest) {}

    @Override
    public void onLogoutCompleted(MParticleUser mParticleUser,
            FilteredIdentityApiRequest filteredIdentityApiRequest) {
        merchant.clearAllData(applicationContext);
    }

    @Override
    public void onModifyCompleted(MParticleUser mParticleUser,
            FilteredIdentityApiRequest filteredIdentityApiRequest) {}

    @Override
    public void onUserIdentified(MParticleUser mParticleUser) {}

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

    private List<ButtonProductCompatible> parseAsButtonProducts(List<Product> products) {
        List<ButtonProductCompatible> buttonProducts = new ArrayList<>();
        if (products == null) return buttonProducts;

        for (Product product: products) {
            buttonProducts.add(parseAsButtonProduct(product, products.size()));
        }
        return buttonProducts;
    }

    private ButtonProductCompatible parseAsButtonProduct(Product product, int collectionSize) {
        ButtonProduct buttonProduct = new ButtonProduct();
        if (product == null) return buttonProduct;

        buttonProduct.setName(product.getName());
        buttonProduct.setId(product.getSku());
        buttonProduct.setValue((int) (product.getTotalAmount() * 100));
        buttonProduct.setQuantity((int) product.getQuantity());
        buttonProduct.setCategories(Collections.singletonList(product.getCategory()));
        buttonProduct.setAttributes(Collections.singletonMap("btn_product_count", String.valueOf(collectionSize)));
        return buttonProduct;
    }
}
