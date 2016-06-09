package com.mparticle.kits.button;

import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class CheckPendingLinkCommand extends Command<DeferredDeepLinkDTO> {
    private static final String TAG = "CheckPendingLink";
    private final Storage mStorage;
    private final ButtonApi mApi;
    private final Display mDisplay;

    public CheckPendingLinkCommand(final FailableReceiver<DeferredDeepLinkDTO> onDeepLink, final Storage storage,
                                   final ButtonApi buttonApi, final WindowManager windowManager) {
        super(onDeepLink);
        mStorage = storage;
        mApi = buttonApi;
        mDisplay = windowManager.getDefaultDisplay();
    }

    @Override
    public DeferredDeepLinkDTO execute() throws Exception {
        return mApi.getPendingLink(collectSignals());
    }

    @Override
    public String key() {
        return getClass().getSimpleName();
    }

    public JSONObject collectSignals() {
        final JSONObject signals = new JSONObject();
        safePut(signals, "timezone", Calendar.getInstance().getTimeZone().getID());
        safePut(signals, "os", "android");
        safePut(signals, "os_version", Build.VERSION.RELEASE);
        safePut(signals, "device", String.format("%s %s", Build.MANUFACTURER, Build.MODEL));
        safePut(signals, "screen", getScreenSize());
        final Locale locale = Locale.getDefault();
        safePut(signals, "country", locale.getCountry());
        safePut(signals, "language", locale.getLanguage());
        safePut(signals, "install_referrer", mStorage.getInstallReferrer());
        safePut(signals, "source", "mparticle");
        return signals;
    }

    private String getScreenSize() {
        if (mDisplay!= null) {
            final DisplayMetrics out = new DisplayMetrics();
            mDisplay.getMetrics(out);
            return String.format(Locale.US, "%dx%d", out.widthPixels, out.heightPixels);
        }
        return "unknown";
    }

    private void safePut(final JSONObject signals, final String key, final String value) {
        try {
            signals.putOpt(key, value);
        } catch (JSONException e) {}
    }
}

