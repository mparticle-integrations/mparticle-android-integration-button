package com.mparticle.kits;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;

import com.usebutton.merchant.ButtonProductCompatible;

import com.mparticle.AttributionError;
import com.mparticle.AttributionResult;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.CoreCallbacks;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mparticle.kits.ButtonKit.ATTRIBUTE_REFERRER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ButtonKitTests {

    private Context context = mock(Context.class);
    private ButtonKit buttonKit = new ButtonKit();
    private ButtonMerchantWrapper merchant = mock(ButtonMerchantWrapper.class);
    private Map<String, String> settings = new HashMap<>();
    private TestKitManager kitManager;

    private static final String TEST_APPLICATION_ID = "app-abcdef1234567890";
    private static final String TEST_ATTRIBUTION_TOKEN = "srctok-abcdef1234567890";
    private static final String TEST_DEEP_LINK = "https://www.example.com/product/abc123";
    private static final int TEST_KIT_ID = 0x01;

    @Before
    public void setUp() throws Exception {
        setTestSdkVersion(Build.VERSION_CODES.KITKAT);
        buttonKit.merchant = merchant;
        settings.put("application_id", TEST_APPLICATION_ID);
        when(context.getApplicationContext()).thenReturn(context);
        MParticle.setInstance(new TestMParticle());
        kitManager = new TestKitManager();
        buttonKit.setKitManager(kitManager);
        buttonKit.setConfiguration(new TestKitConfiguration());
    }

    @Test
    public void getName_shouldReturnKitName() throws Exception {
        assertThat(buttonKit.getName()).isEqualTo("Button");
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     */
    @Test(expected = IllegalArgumentException.class)
    public void onKitCreate_shouldThrowExceptionForBadSettings() throws Exception {
        Map<String, String> settings = new HashMap<>();
        settings.put("fake setting", "fake");
        buttonKit.onKitCreate(settings, context);
    }

    @Test
    public void testClassName() throws Exception {
        KitIntegrationFactory factory = new KitIntegrationFactory();
        Map<Integer, String> integrations = factory.getKnownIntegrations();
        String className = buttonKit.getClass().getName();
        for (Map.Entry<Integer, String> entry : integrations.entrySet()) {
            if (entry.getValue().equals(className)) {
                return;
            }
        }
        fail(className + " not found as a known integration.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void onKitCreate_shouldThrowExceptionBelowApi15() throws Exception {
        setTestSdkVersion(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
        buttonKit.onKitCreate(settings, context);
    }

    @Test
    public void onKitCreate_shouldConfigureMerchantLibrary() {
        buttonKit.onKitCreate(settings, context);
        verify(merchant).configure(context, TEST_APPLICATION_ID);
    }

    @Test
    public void onKitCreate_shouldAddAttributionListener() {
        buttonKit.onKitCreate(settings, context);
        verify(merchant).addAttributionTokenListener(context, buttonKit);
    }

    @Test
    public void onKitCreate_shouldHandlePostInstallIntent() {
        buttonKit.onKitCreate(settings, context);
        verify(merchant).handlePostInstallIntent(context, buttonKit);
    }

    @Test
    public void getAttributionToken_shouldReturnMerchantLibraryToken() {
        when(merchant.getAttributionToken(context)).thenReturn("src-abc123");
        buttonKit.onKitCreate(settings, context);
        assertThat(buttonKit.getAttributionToken()).isEqualTo("src-abc123");
    }

    @Test
    public void onAttributionTokenChanged_shouldSetIntegrationAttribute() {
        buttonKit.onKitCreate(settings, context);
        buttonKit.onAttributionTokenChanged(TEST_ATTRIBUTION_TOKEN);
        String token = kitManager.attributes.get(ATTRIBUTE_REFERRER);
        assertThat(token).isEqualTo(TEST_ATTRIBUTION_TOKEN);
    }

    @Test
    public void onResult_shouldSetAttributionResultOnIntent() {
        Intent intent = mock(Intent.class);
        when(intent.getDataString()).thenReturn(TEST_DEEP_LINK);
        when(intent.resolveActivity(any(PackageManager.class)))
                .thenReturn(mock(ComponentName.class));
        when(intent.resolveActivity(null))
                .thenReturn(mock(ComponentName.class));
        buttonKit.onKitCreate(settings, context);
        buttonKit.onResult(intent, null);
        assertThat(kitManager.result.getLink()).isEqualTo(TEST_DEEP_LINK);
        assertThat(kitManager.result.getServiceProviderId()).isEqualTo(TEST_KIT_ID);
    }

    @Test
    public void onActivityCreated_shouldTrackIncomingIntent() {
        Activity activity = mock(Activity.class);
        Intent intent = new Intent();
        when(activity.getIntent()).thenReturn(intent);
        buttonKit.onKitCreate(settings, context);
        buttonKit.onActivityCreated(activity, null);
        verify(merchant).trackIncomingIntent(context, intent);
    }

    @Test
    public void onActivityStarted_shouldTrackIncomingIntent() {
        Activity activity = mock(Activity.class);
        Intent intent = new Intent();
        when(activity.getIntent()).thenReturn(intent);
        buttonKit.onKitCreate(settings, context);
        buttonKit.onActivityStarted(activity);
        verify(merchant).trackIncomingIntent(context, intent);
    }

    @Test
    public void onActivityResumed_shouldTrackIncomingIntent() {
        Activity activity = mock(Activity.class);
        Intent intent = new Intent();
        when(activity.getIntent()).thenReturn(intent);
        buttonKit.onKitCreate(settings, context);
        buttonKit.onActivityResumed(activity);
        verify(merchant).trackIncomingIntent(context, intent);
    }

    @Test
    public void reset_shouldClearAllData() {
        buttonKit.onKitCreate(settings, context);
        buttonKit.reset();
        verify(merchant).clearAllData(context);
    }

    @Test
    public void onLogoutCompleted_shouldClearAllData() {
        buttonKit.onKitCreate(settings, context);
        buttonKit.onLogoutCompleted(mock(MParticleUser.class), mock(FilteredIdentityApiRequest.class));
        verify(merchant).clearAllData(context);
    }

    @Test
    public void logEvent_noProductAction_shouldIgnoreCall() {
        Product product = new Product.Builder("Test", "987", 1).build();
        CommerceEvent event = new CommerceEvent.Builder(null, product).build();
        buttonKit.logEvent(event);
        verifyZeroInteractions(merchant);
    }

    @Test
    public void logEvent_shouldConvertToButtonProduct() {
        ArgumentCaptor<ButtonProductCompatible> productCaptor =
                ArgumentCaptor.forClass(ButtonProductCompatible.class);
        Product product = new Product.Builder("Test Name", "98765", 12.34)
                .category("Test category")
                .quantity(2)
                .customAttributes(Collections.singletonMap("test_key", "test_value"))
                .build();
        CommerceEvent event = new CommerceEvent.Builder(Product.DETAIL, product).build();

        buttonKit.logEvent(event);
        verify(merchant).trackProductViewed(productCaptor.capture());
        ButtonProductCompatible btnProduct = productCaptor.getValue();

        assertThat(btnProduct).isNotNull();
        assertThat(btnProduct.getId()).isEqualTo("98765");
        assertThat(btnProduct.getName()).isEqualTo("Test Name");
        assertThat(btnProduct.getValue()).isEqualTo((int) (12.34 * 100 * 2));
        assertThat(btnProduct.getQuantity()).isEqualTo(2);
        assertThat(btnProduct.getCategories()).isNotNull();
        assertThat(btnProduct.getCategories().size()).isEqualTo(1);
        assertThat(btnProduct.getCategories().get(0)).isEqualTo("Test category");
        assertThat(btnProduct.getAttributes()).isNotNull();
        assertThat(btnProduct.getAttributes()).hasSize(1);
        assertThat(btnProduct.getAttributes()).containsEntry("btn_product_count", "1");
        assertThat(btnProduct.getAttributes()).doesNotContainEntry("test_key", "test_value");
    }

    @Test
    public void logEvent_detail_shouldTrackWithFirstProduct() {
        Product product = new Product.Builder("Test", "987", 1).build();
        CommerceEvent.Builder eventBuilder = new CommerceEvent.Builder(Product.DETAIL, product);
        for (int i = 0; i < 20; i++) {
            eventBuilder.addProduct(product);
        }

        buttonKit.logEvent(eventBuilder.build());

        verify(merchant).trackProductViewed(any(ButtonProductCompatible.class));
        verifyNoMoreInteractions(merchant);
    }

    @Test
    public void logEvent_addToCart_shouldTrackWithFirstProduct() {
        Product product = new Product.Builder("Test", "987", 1).build();
        CommerceEvent.Builder eventBuilder = new CommerceEvent.Builder(Product.ADD_TO_CART, product);
        for (int i = 0; i < 20; i++) {
            eventBuilder.addProduct(product);
        }

        buttonKit.logEvent(eventBuilder.build());

        verify(merchant).trackAddToCart(any(ButtonProductCompatible.class));
        verifyNoMoreInteractions(merchant);
    }

    @Test
    public void logEvent_checkoutViewed_shouldTrackWithAllProducts() {
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        Product product = new Product.Builder("Test", "987", 1).build();
        CommerceEvent.Builder eventBuilder = new CommerceEvent.Builder(Product.CHECKOUT, product);
        for (int i = 0; i < 20; i++) {
            eventBuilder.addProduct(product);
        }

        buttonKit.logEvent(eventBuilder.build());

        verify(merchant).trackCartViewed(listCaptor.capture());
        assertThat(listCaptor.getValue()).hasSize(21);
        verifyNoMoreInteractions(merchant);
    }

    /*
     * Test Helpers
     */

    private static void setTestSdkVersion(int sdkVersion) throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), sdkVersion);
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }

    private class TestKitManager extends KitManagerImpl {
        private Map<String, String> attributes = new HashMap<>();
        private AttributionResult result;
        private AttributionError error;

        TestKitManager() {
            super(context, null, new TestCoreCallbacks(), null);
        }

        @Override
        Map<String, String> getIntegrationAttributes(KitIntegration kitIntegration) {
            return attributes;
        }

        @Override
        void setIntegrationAttributes(KitIntegration kitIntegration,
                Map<String, String> integrationAttributes) {
            this.attributes = integrationAttributes;
        }

        @Override
        public void onResult(AttributionResult result) {
            this.result = result;
        }

        @Override
        public void onError(AttributionError error) {
            this.error = error;
        }
    }

    private class TestKitConfiguration extends KitConfiguration {
        @Override
        public int getKitId() {
            return TEST_KIT_ID;
        }
    }

    private class TestMParticle extends MParticle {
        @NonNull
        @Override
        public IdentityApi Identity() {
            return mock(IdentityApi.class);
        }
    }

    class TestCoreCallbacks implements CoreCallbacks {
        public boolean isBackgrounded() { return false; }
        public int getUserBucket() { return 0; }
        public boolean isEnabled() { return false; }
        public void setIntegrationAttributes(int i, Map<String, String> map) { }
        public Map<String, String> getIntegrationAttributes(int i) { return null; }
        public WeakReference<Activity> getCurrentActivity() { return null; }
        public JSONArray getLatestKitConfiguration() { return null; }
        public boolean isPushEnabled() { return false; }
        public String getPushSenderId() { return null; }
        public String getPushInstanceId() { return null; }
        public Uri getLaunchUri() { return null; }
        public String getLaunchAction() { return null; }
        public void replayAndDisableQueue() { }
        public KitListener getKitListener() {
            return new KitListener() {
                public void kitFound(int i) { }
                public void kitConfigReceived(int i, String s) { }
                public void kitExcluded(int i, String s) { }
                public void kitStarted(int i) { }
                public void onKitApiCalled(int i, Boolean aBoolean, Object... objects) { }
                public void onKitApiCalled(String s, int i, Boolean aBoolean, Object... objects) { }
            };
        }
    }
}
