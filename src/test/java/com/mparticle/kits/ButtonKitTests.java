package com.mparticle.kits;

import android.content.Context;
import android.os.Build;

import com.usebutton.merchant.ButtonMerchant.AttributionTokenListener;
import com.usebutton.merchant.PostInstallIntentListener;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ButtonKitTests {

    private Context context;
    private ButtonKit buttonKit;
    private ButtonMerchantWrapper merchantWrapper;
    private MParticleWrapper mParticleWrapper;
    private Map<String, String> settings;

    private static final String TEST_APPLICATION_ID = "app-abcdef12345";

    @Before
    public void setUp() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.KITKAT);
        buttonKit = new ButtonKit();
        merchantWrapper = mock(ButtonMerchantWrapper.class);
        mParticleWrapper = mock(MParticleWrapper.class);
        context = mock(Context.class);
        buttonKit.merchant = merchantWrapper;
        buttonKit.mParticle = mParticleWrapper;

        settings = new HashMap<>();
        settings.put("application_id", TEST_APPLICATION_ID);

        when(context.getApplicationContext()).thenReturn(context);
        when(mParticleWrapper.isDebuggingEnvironment()).thenReturn(true);
    }

    @Test
    public void testGetName() throws Exception {
        String name = buttonKit.getName();
        assertTrue(name != null && name.length() > 0);
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    public void testOnKitCreate() throws Exception{
        Exception e = null;
        try {
            KitIntegration kit = buttonKit;
            Map settings = new HashMap<>();
            settings.put("fake setting", "fake");
            kit.onKitCreate(settings, context);
        }catch (Exception ex) {
            e = ex;
        }
        assertNotNull(e);
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
    public void buttonKit_shouldNotInitializeOnBelowApi15() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"),
                Build.VERSION_CODES.ICE_CREAM_SANDWICH);
        buttonKit.onKitCreate(settings, context);

        verify(merchantWrapper, never()).configure(any(Context.class), anyString());
    }

    @Test
    public void buttonKit_shouldConfigureMerchantOnKitCreate() {
        buttonKit.onKitCreate(settings, context);

        verify(merchantWrapper).configure(context, TEST_APPLICATION_ID);
    }

    @Test
    public void buttonKit_shouldAddAttributionListenerOnKitCreate() {
        buttonKit.onKitCreate(settings, context);

        verify(merchantWrapper).addAttributionTokenListener(eq(context),
                any(AttributionTokenListener.class));
    }

    @Test
    public void buttonKit_shouldHandlePostInstallIntentOnKitCreate() {
        buttonKit.onKitCreate(settings, context);

        verify(merchantWrapper).handlePostInstallIntent(eq(context),
                any(PostInstallIntentListener.class));
    }

    @Test
    public void buttonKit_shouldReturnAttributionToken() {
        when(merchantWrapper.getAttributionToken(context)).thenReturn("src-abc123");
        buttonKit.onKitCreate(settings, context);

        assertThat(buttonKit.getAttributionToken()).isEqualTo("src-abc123");
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}