package com.mparticle.kits;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.usebutton.merchant.ButtonMerchant;
import com.usebutton.merchant.ButtonMerchant.AttributionTokenListener;
import com.usebutton.merchant.PostInstallIntentListener;

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
}
