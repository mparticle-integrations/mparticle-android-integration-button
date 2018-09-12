package com.mparticle.kits;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import com.mparticle.AttributionError;
import com.mparticle.AttributionResult;
import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApi;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static com.mparticle.kits.ButtonKit.ATTRIBUTE_REFERRER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

        buttonKit.onKitCreate(settings, context);
        buttonKit.onResult(intent, null);
        assertThat(kitManager.result.getLink()).isEqualTo(TEST_DEEP_LINK);
        assertThat(kitManager.result.getServiceProviderId()).isEqualTo(TEST_KIT_ID);
    }

    @Test
    public void onResult_shouldSetAttributionErrorOnNoIntent() {
        buttonKit.onKitCreate(settings, context);
        buttonKit.onResult(null, null);
        assertThat(kitManager.error.getMessage()).isEqualTo("No pending post-install deep link.");
        assertThat(kitManager.error.getServiceProviderId()).isEqualTo(TEST_KIT_ID);
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
            super(context, null, null, null, null);
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
}
