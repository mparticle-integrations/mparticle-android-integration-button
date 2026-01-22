package com.mparticle.kits

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.CoreCallbacks.KitListener
import com.usebutton.merchant.ButtonProductCompatible
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.HashMap

class ButtonKitTests {
    private val context = mockk<Context>(relaxed = true)
    private val buttonKit = ButtonKit()
    private val merchant = mockk<ButtonMerchantWrapper>(relaxed = true)
    private val settings = HashMap<String, String>()
    private lateinit var kitManager: TestKitManager

    @Before
    @Throws(Exception::class)
    fun setUp() {
        setTestSdkVersion(Build.VERSION_CODES.KITKAT)
        buttonKit.merchant = merchant
        settings["application_id"] = TEST_APPLICATION_ID
        every { context.applicationContext } returns context
        MParticle.setInstance(TestMParticle())
        kitManager = TestKitManager()
        buttonKit.kitManager = kitManager
        buttonKit.configuration = TestKitConfiguration()
    }

    @Throws(Exception::class)
    @Test
    fun name_shouldReturnKitName() {
        assertThat(buttonKit.name).isEqualTo("Button")
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     */
    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun onKitCreate_shouldThrowExceptionForBadSettings() {
        val settings = HashMap<String, String>()
        settings["fake setting"] = "fake"
        buttonKit.onKitCreate(settings, context)
    }

    @Test
    @Throws(Exception::class)
    fun testClassName() {
        val options = Mockito.mock(MParticleOptions::class.java)
        val factory = KitIntegrationFactory(options)
        val integrations = factory.supportedKits.values
        val className = buttonKit.javaClass.name
        for (integration in integrations) {
            if (integration.name == className) {
                return
            }
        }
        Assert.fail("$className not found as a known integration.")
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun onKitCreate_shouldThrowExceptionBelowApi15() {
        setTestSdkVersion(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        buttonKit.onKitCreate(settings, context)
    }

    @Test
    fun onKitCreate_shouldConfigureMerchantLibrary() {
        buttonKit.onKitCreate(settings, context)
        verify { (merchant).configure(context, TEST_APPLICATION_ID) }
    }

    @Test
    fun onKitCreate_shouldAddAttributionListener() {
        buttonKit.onKitCreate(settings, context)
        verify { (merchant).addAttributionTokenListener(context, buttonKit) }
    }

    @Test
    fun onKitCreate_shouldHandlePostInstallIntent() {
        buttonKit.onKitCreate(settings, context)
        verify { (merchant).handlePostInstallIntent(context, buttonKit) }
    }

    @Test
    fun attributionToken_shouldReturnMerchantLibraryToken() {
        every { (merchant.getAttributionToken(context)) } returns ("src-abc123")
        buttonKit.onKitCreate(settings, context)
        assertThat(buttonKit.attributionToken).isEqualTo("src-abc123")
    }

    @Test
    fun onAttributionTokenChanged_shouldSetIntegrationAttribute() {
        buttonKit.onKitCreate(settings, context)
        buttonKit.onAttributionTokenChanged(TEST_ATTRIBUTION_TOKEN)
        val token = kitManager.attributes[ButtonKit.ATTRIBUTE_REFERRER]
        assertThat(token).isEqualTo(TEST_ATTRIBUTION_TOKEN)
    }

    @Test
    fun onResult_shouldSetAttributionResultOnIntent() {
        val intent = mock(Intent::class.java)
        `when`(intent.dataString).thenReturn(TEST_DEEP_LINK)
        `when`(intent.resolveActivity(Mockito.any(PackageManager::class.java)))
            .thenReturn(mock(ComponentName::class.java))
        `when`(intent.resolveActivity(context.packageManager))
            .thenReturn(mock(ComponentName::class.java))
        buttonKit.onKitCreate(settings, context)
        buttonKit.onResult(intent, null)
        assertThat(kitManager.result?.link).isEqualTo(TEST_DEEP_LINK)
        assertThat(kitManager.result?.serviceProviderId).isEqualTo(TEST_KIT_ID)
    }

    @Test
    fun onActivityCreated_shouldTrackIncomingIntent() {
        val activity = mockk<Activity>()
        val intent = Intent()
        every { (activity.intent) }.returns(intent)
        buttonKit.onKitCreate(settings, context)
        buttonKit.onActivityCreated(activity, null)
        every { (merchant).trackIncomingIntent(context, intent) }
    }

    @Test
    fun onActivityStarted_shouldTrackIncomingIntent() {
        val activity = mockk<Activity>()
        val intent = Intent()
        every { (activity.intent) }.returns(intent)
        buttonKit.onKitCreate(settings, context)
        buttonKit.onActivityStarted(activity)
        every { (merchant).trackIncomingIntent(context, intent) }
    }

    @Test
    fun onActivityResumed_shouldTrackIncomingIntent() {
        val activity = mockk<Activity>()
        val intent = Intent()
        every { (activity.intent) }.returns(intent)
        buttonKit.onKitCreate(settings, context)
        buttonKit.onActivityResumed(activity)
        every { (merchant).trackIncomingIntent(context, intent) }
    }

    @Test
    fun reset_shouldClearAllData() {
        buttonKit.onKitCreate(settings, context)
        buttonKit.reset()
        every { (merchant).clearAllData(context) }
    }

    @Test
    fun onLogoutCompleted_shouldClearAllData() {
        buttonKit.onKitCreate(settings, context)
        buttonKit.onLogoutCompleted(mockk<MParticleUser>(), mockk<FilteredIdentityApiRequest>())
        every { (merchant).clearAllData(context) }
    }

    @Test
    fun logEvent_noProductAction_shouldIgnoreCall() {
        val product = Product.Builder("Test", "987", 1.0).build()
        val event = CommerceEvent.Builder(null.toString(), product).build()
        buttonKit.logEvent(event)
        verify { merchant wasNot Called }
    }

    @Test
    fun logEvent_shouldConvertToButtonProduct() {
        val productCaptor = slot<ButtonProductCompatible>()

        val product =
            Product
                .Builder("Test Name", "98765", 12.34)
                .category("Test category")
                .quantity(2.0)
                .customAttributes(Collections.singletonMap("test_key", "test_value"))
                .build()

        val event = CommerceEvent.Builder(Product.DETAIL, product).build()
        buttonKit.logEvent(event)

        verify { (merchant).trackProductViewed(capture(productCaptor)) }

        val btnProduct = productCaptor
        assertThat(btnProduct).isNotNull
        btnProduct.captured.apply {
            assertThat(id).isEqualTo("98765")
            assertThat(name).isEqualTo("Test Name")
            assertThat(value).isEqualTo((12.34 * 100 * 2).toInt())
            assertThat(quantity).isEqualTo(2)
            assertThat(categories).isNotNull
            assertThat(categories?.size).isEqualTo(1)
            assertThat(categories?.get(0)).isEqualTo("Test category")
            assertThat(attributes).isNotNull
            assertThat(attributes).hasSize(1)
            assertThat(attributes).containsEntry("btn_product_count", "1")
            assertThat(attributes)
                .doesNotContainEntry("test_key", "test_value")
        }
    }

    @Test
    fun logEvent_detail_shouldTrackWithFirstProduct() {
        val product = Product.Builder("Test", "987", 1.0).build()
        val eventBuilder = CommerceEvent.Builder(Product.DETAIL, product)
        for (i in 0..19) {
            eventBuilder.addProduct(product)
        }
        buttonKit.logEvent(eventBuilder.build())

        verify(exactly = 1) { (merchant).trackProductViewed(any<ButtonProductCompatible>()) }
    }

    @Test
    fun logEvent_addToCart_shouldTrackWithFirstProduct() {
        val product = Product.Builder("Test", "987", 1.0).build()
        val eventBuilder = CommerceEvent.Builder(Product.ADD_TO_CART, product)
        for (i in 0..19) {
            eventBuilder.addProduct(product)
        }
        buttonKit.logEvent(eventBuilder.build())
        verify(exactly = 1) { (merchant).trackAddToCart(any<ButtonProductCompatible>()) }
    }

    @Test
    fun logEvent_checkoutViewed_shouldTrackWithAllProducts() {
        val listCaptor = slot<MutableList<ButtonProductCompatible>>()
        val product = Product.Builder("Test", "987", 1.0).build()
        val eventBuilder = CommerceEvent.Builder(Product.CHECKOUT, product)
        for (i in 0..19) {
            eventBuilder.addProduct(product)
        }
        println(
            eventBuilder
                .build()
                .products
                ?.size
                .toString(),
        )
        buttonKit.logEvent(eventBuilder.build())
        verify(exactly = 1) { (merchant).trackCartViewed(capture(listCaptor)) }
        assertThat(listCaptor.captured.size).isEqualTo(21)
    }

    private inner class TestKitManager internal constructor() :
        KitManagerImpl(context, null, TestCoreCallbacks(), mock(MParticleOptions::class.java)) {
            var attributes = HashMap<String, String>()
            var result: AttributionResult? = null
            private var error: AttributionError? = null

            public override fun getIntegrationAttributes(kitIntegration: KitIntegration): Map<String, String> = attributes

            public override fun setIntegrationAttributes(
                kitIntegration: KitIntegration,
                integrationAttributes: Map<String, String>,
            ) {
                attributes = integrationAttributes as HashMap<String, String>
            }

            override fun onResult(result: AttributionResult) {
                this.result = result
            }

            override fun onError(error: AttributionError) {
                this.error = error
            }
        }

    private inner class TestKitConfiguration : KitConfiguration() {
        override fun getKitId(): Int = TEST_KIT_ID
    }

    private inner class TestMParticle : MParticle() {
        override fun Identity(): IdentityApi = mock(IdentityApi::class.java)
    }

    internal inner class TestCoreCallbacks : CoreCallbacks {
        override fun isBackgrounded(): Boolean = false

        override fun getUserBucket(): Int = 0

        override fun isEnabled(): Boolean = false

        override fun setIntegrationAttributes(
            i: Int,
            map: Map<String, String>,
        ) {}

        override fun getIntegrationAttributes(i: Int): Map<String, String>? = null

        override fun getCurrentActivity(): WeakReference<Activity>? = null

        override fun getLatestKitConfiguration(): JSONArray? = null

        override fun getDataplanOptions(): DataplanOptions? = null

        override fun isPushEnabled(): Boolean = false

        override fun getPushSenderId(): String? = null

        override fun getPushInstanceId(): String? = null

        override fun getLaunchUri(): Uri? = null

        override fun getLaunchAction(): String? = null

        override fun getKitListener(): KitListener =
            object : KitListener {
                override fun kitFound(kitId: Int) {}

                override fun kitConfigReceived(
                    kitId: Int,
                    configuration: String?,
                ) {}

                override fun kitExcluded(
                    kitId: Int,
                    reason: String?,
                ) {}

                override fun kitStarted(kitId: Int) {}

                override fun onKitApiCalled(
                    kitId: Int,
                    used: Boolean?,
                    vararg objects: Any?,
                ) {
                }

                override fun onKitApiCalled(
                    methodName: String?,
                    kitId: Int,
                    used: Boolean?,
                    vararg objects: Any?,
                ) {
                }
            }
    }

    companion object {
        private const val TEST_APPLICATION_ID = "app-abcdef1234567890"
        private const val TEST_ATTRIBUTION_TOKEN = "srctok-abcdef1234567890"
        private const val TEST_DEEP_LINK = "https://www.example.com/product/abc123"
        private const val TEST_KIT_ID = 0x01

        /*
         * Test Helpers
         */
        @Throws(Exception::class)
        private fun setTestSdkVersion(sdkVersion: Int) {
            setFinalStatic(VERSION::class.java.getField("SDK_INT"), sdkVersion)
        }

        @Throws(Exception::class)
        private fun setFinalStatic(
            field: Field,
            newValue: Int,
        ) {
            field.isAccessible = true
            val getDeclaredFields0 =
                Class::class.java.getDeclaredMethod(
                    "getDeclaredFields0",
                    Boolean::class.javaPrimitiveType,
                )
            getDeclaredFields0.isAccessible = true
            val fields = getDeclaredFields0.invoke(Field::class.java, false) as Array<Field>
            var modifiersField: Field? = null
            for (each in fields) {
                if ("modifiers" == each.name) {
                    modifiersField = each
                    break
                }
            }
            modifiersField!!.isAccessible = true
            modifiersField!!.setInt(field, field.modifiers and Modifier.FINAL.inv())
            field[null] = newValue
        }
    }
}
