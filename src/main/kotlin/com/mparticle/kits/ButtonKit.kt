package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.KitIntegration.*
import com.usebutton.merchant.ButtonMerchant.AttributionTokenListener
import com.usebutton.merchant.ButtonProduct
import com.usebutton.merchant.ButtonProductCompatible
import com.usebutton.merchant.PostInstallIntentListener
import java.math.BigDecimal
import java.util.*

/**
 * MParticle embedded implementation of the [Button Merchant Library](https://github.com/button/button-merchant-android).
 *
 * Learn more at our [Developer Docs](https://developer.usebutton.com/guides/merchants/android/button-merchant-integration-guide)
 */
class ButtonKit : KitIntegration(), ActivityListener, CommerceListener, IdentityListener,
    AttributionTokenListener, PostInstallIntentListener {
    private var applicationContext: Context? = null

    @JvmField
    @VisibleForTesting
    var merchant = ButtonMerchantWrapper()

    override fun getName(): String = NAME

    override fun getInstance(): ButtonKit = this

    public override fun onKitCreate(
        settings: Map<String, String>,
        ctx: Context
    ): List<ReportingMessage> {
        applicationContext = ctx.applicationContext
        val applicationId = settings[APPLICATION_ID]
        if (KitUtils.isEmpty(applicationId)) {
            throwOnKitCreateError(NO_APPLICATION_ID)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            throwOnKitCreateError(LOWER_THAN_API_15)
        }
        applicationContext?.let {

            if (applicationId != null) {
                merchant.configure(it, applicationId)
            }
            merchant.addAttributionTokenListener(it, this)
            merchant.handlePostInstallIntent(it, this)
        }
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    public override fun reset() {
        super.reset()
        applicationContext?.let { merchant.clearAllData(it) }
    }
    /*
     * Public methods to expose important Merchant Library methods
     */
    /**
     * Get the Button attribution token which should be attached to any orders reported and
     * attributed to the Button network.
     *
     * For attribution to work correctly, you must:
     *
     *  * Always access this token directly—*never cache it*.
     *  * Never manage the lifecycle of this token—Button manages the token validity window
     * server-side.
     *  * Always include this value when reporting orders to your order API.
     *
     *
     * <pre>
     * ButtonKit button = (ButtonKit) MParticle.getInstance()
     * .getKitInstance(MParticle.ServiceProviders.BUTTON);
     * if (button != null) {
     * String attributionToken = button.getAttributionToken();
     * if (attributionToken != null) {
     * // Use token with your order reporting.
     * }
     * }
    </pre> *
     *
     * @return the last tracked Button attribution token.
     */
    val attributionToken: String?
        get() = applicationContext?.let { merchant.getAttributionToken(it) }

    override fun onAttributionTokenChanged(token: String) {
        val attributes = integrationAttributes
        attributes[ATTRIBUTE_REFERRER] = token
        integrationAttributes = attributes
        logDebug("Refreshed Button Attribution Token: %s", token)
    }

    override fun onResult(intent: Intent?, throwable: Throwable?) {
        val pm = applicationContext?.packageManager
        if (pm?.let { intent?.resolveActivity(it) } != null) {
            logDebug("Handling post-install intent for %s", intent.toString())
            val result = AttributionResult()
                .setLink(intent?.dataString)
                .setServiceProviderId(configuration.kitId)
            kitManager.onResult(result)
        }
        if (throwable != null) {
            logError("Error checking post install intent", throwable)
            val attributionError = AttributionError()
                .setMessage(throwable.message)
                .setServiceProviderId(configuration.kitId)
            kitManager.onError(attributionError)
        }
    }

    /*
     * Overrides for ActivityListener
     */
    override fun onActivityCreated(activity: Activity, bundle: Bundle?): List<ReportingMessage> {
        applicationContext?.let { merchant.trackIncomingIntent(it, activity.intent) }
        return emptyList()
    }

    override fun onActivityStarted(activity: Activity): List<ReportingMessage> {
        applicationContext?.let { merchant.trackIncomingIntent(it, activity.intent) }
        return emptyList()
    }

    override fun onActivityResumed(activity: Activity): List<ReportingMessage> {
        applicationContext?.let { merchant.trackIncomingIntent(it, activity.intent) }
        return emptyList()
    }

    override fun onActivityPaused(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityStopped(activity: Activity): List<ReportingMessage> = emptyList()


    override fun onActivitySaveInstanceState(
        activity: Activity,
        bundle: Bundle?
    ): List<ReportingMessage> = emptyList()

    override fun onActivityDestroyed(activity: Activity): List<ReportingMessage> = emptyList()


    /*
     * Overrides for CommerceListener
     */
    override fun logLtvIncrease(
        bigDecimal: BigDecimal, bigDecimal1: BigDecimal,
        s: String, map: Map<String, String>
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage> {
        if (commerceEvent.productAction == null || commerceEvent.products == null) {
            return emptyList()
        }
        val products = parseAsButtonProducts(commerceEvent.products)
        val product = if (products.isEmpty()) ButtonProduct() else products[0]

        commerceEvent.productAction?.let {
            when (commerceEvent.productAction) {
                Product.DETAIL -> {
                    logDebug("Tracking product viewed: %s", product.name!!)
                    merchant.trackProductViewed(product)
                }
                Product.ADD_TO_CART -> {
                    logDebug("Tracking product added to cart: %s", product.name!!)
                    merchant.trackAddToCart(product)
                }
                Product.CHECKOUT -> {
                    logDebug("Tracking cart viewed with %d products!", products.size)
                    merchant.trackCartViewed(products)
                }
                else -> logDebug(
                    "Product Action [%s] is not yet supported by the Button Merchant Library",
                    it
                )
            }
        }
        return emptyList()
    }

    /*
     * Overrides for IdentityListener
     */
    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
        merchant.clearAllData(applicationContext!!)
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {}

    /*
     * Utility methods
     */
    private fun logDebug(message: String, vararg args: Any) {
        Logger.debug(String.format("ButtonKit: $message", *args))
    }

    private fun logError(message: String, t: Throwable) {
        Logger.error(t, "ButtonKit: $message")
    }

    private fun throwOnKitCreateError(message: String) {
        throw IllegalArgumentException(message)
    }

    private fun parseAsButtonProducts(products: List<Product>?): List<ButtonProductCompatible> {
        val buttonProducts: MutableList<ButtonProductCompatible> = ArrayList()
        if (products == null) return buttonProducts
        for (product in products) {
            buttonProducts.add(parseAsButtonProduct(product, products.size))
        }
        return buttonProducts
    }

    private fun parseAsButtonProduct(
        product: Product?,
        collectionSize: Int
    ): ButtonProductCompatible {
        val buttonProduct = ButtonProduct()
        if (product == null) return buttonProduct
        buttonProduct.name = product.name
        buttonProduct.id = product.sku
        buttonProduct.value = (product.totalAmount * 100).toInt()
        buttonProduct.quantity = product.quantity.toInt()
        buttonProduct.categories = listOf(product.category)
        buttonProduct.attributes = Collections.singletonMap(
            "btn_product_count",
            collectionSize.toString()
        )
        return buttonProduct
    }

    companion object {
        const val ATTRIBUTE_REFERRER = "com.usebutton.source_token"
        const val NAME = "Button"
        const val APPLICATION_ID = "application_id"
        const val NO_APPLICATION_ID = "No Button application ID provided, can't initialize kit."
        const val LOWER_THAN_API_15 =
            "App running in an < API 15 environment, can't initialize kit."
    }
}