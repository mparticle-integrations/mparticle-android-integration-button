package com.mparticle.kits

import android.content.Context
import com.usebutton.merchant.ButtonMerchant
import android.content.Intent
import com.usebutton.merchant.ButtonMerchant.AttributionTokenListener
import com.usebutton.merchant.PostInstallIntentListener
import com.usebutton.merchant.ButtonProductCompatible

/**
 * Wrapper class for [ButtonMerchant] to allow for testing the library's static methods
 */
class ButtonMerchantWrapper {
    fun configure(context: Context, applicationId: String) {
        ButtonMerchant.configure(context, applicationId)
    }

    fun trackIncomingIntent(context: Context, intent: Intent) {
        ButtonMerchant.trackIncomingIntent(context, intent)
    }

    fun getAttributionToken(context: Context): String? {
        return ButtonMerchant.getAttributionToken(context)
    }

    fun addAttributionTokenListener(context: Context, listener: AttributionTokenListener) {
        ButtonMerchant.addAttributionTokenListener(context, listener)
    }

    fun handlePostInstallIntent(context: Context, listener: PostInstallIntentListener) {
        ButtonMerchant.handlePostInstallIntent(context, listener)
    }

    fun clearAllData(context: Context) {
        ButtonMerchant.clearAllData(context)
    }

    fun trackProductViewed(product: ButtonProductCompatible?) {
        ButtonMerchant.activity().productViewed(product)
    }

    fun trackAddToCart(product: ButtonProductCompatible?) {
        ButtonMerchant.activity().productAddedToCart(product)
    }

    fun trackCartViewed(products: List<ButtonProductCompatible?>) {
        ButtonMerchant.activity().cartViewed(products)
    }
}