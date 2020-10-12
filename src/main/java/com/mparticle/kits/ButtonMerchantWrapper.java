package com.mparticle.kits;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.usebutton.merchant.ButtonMerchant;
import com.usebutton.merchant.ButtonMerchant.AttributionTokenListener;
import com.usebutton.merchant.ButtonProductCompatible;
import com.usebutton.merchant.PostInstallIntentListener;

import java.util.List;

/**
 * Wrapper class for {@link ButtonMerchant} to allow for testing the library's static methods
 */
public class ButtonMerchantWrapper {

    public void configure(@NonNull Context context, @NonNull String applicationId) {
        ButtonMerchant.configure(context, applicationId);
    }

    public void trackIncomingIntent(@NonNull Context context, @NonNull Intent intent) {
        ButtonMerchant.trackIncomingIntent(context, intent);
    }

    public String getAttributionToken(@NonNull Context context) {
        return ButtonMerchant.getAttributionToken(context);
    }

    public void addAttributionTokenListener(@NonNull Context context, @NonNull
            AttributionTokenListener listener) {
        ButtonMerchant.addAttributionTokenListener(context, listener);
    }

    public void handlePostInstallIntent(@NonNull Context context, @NonNull
            PostInstallIntentListener listener) {
        ButtonMerchant.handlePostInstallIntent(context, listener);
    }

    public void clearAllData(@NonNull Context context) {
        ButtonMerchant.clearAllData(context);
    }

    public void trackProductViewed(@Nullable ButtonProductCompatible product) {
        ButtonMerchant.activity().productViewed(product);
    }

    public void trackAddToCart(@Nullable ButtonProductCompatible product) {
        ButtonMerchant.activity().productAddedToCart(product);
    }

    public void trackCartViewed(@NonNull List<ButtonProductCompatible> products) {
        ButtonMerchant.activity().cartViewed(products);
    }
}
