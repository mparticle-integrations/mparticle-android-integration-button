package com.mparticle.kits.button;

import android.util.Log;

import com.mparticle.kits.button.BuildConfig;

import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Button wrapper for {@link Log}.
 * Logs will be enabled in debug SDK build variant.
 * To enable logs inside of other applications, run this in Terminal:
 * {@code adb shell setprop log.tag.ButtonSDK DEBUG}
 */
public class ButtonLog {

    public static final String SDK_TAG = "ButtonSDK";
    private static Logger ANDROID_LOGGER = new Logger() {

        @Override
        public void d(final String tag, final String message) {
            Log.d(tag, message);
        }

        @Override
        public void i(final String tag, final String message) {
            Log.i(tag, message);
        }

        @Override
        public void e(final String tag, final String message) {
            Log.e(tag, message);
        }

        @Override
        public void e(final String tag, final String message, final Throwable caught) {
            Log.e(tag, message, caught);
        }
    };

    // We need to set Log.DEBUG as our minimum log level as Log.INFO is on by default and our
    // log statements will pass through without using {@code adb shell setprop}
    private static ButtonLog sLog = new ButtonLog(ANDROID_LOGGER, Log.DEBUG);
    private final int mMinimumLogLevel;
    private final Logger mLogger;
    private boolean mIsPartnerLoggingEnabled;

    public ButtonLog(final Logger logger, final int minimumLogLevel) {
        mLogger = logger;
        mMinimumLogLevel = minimumLogLevel;
    }

    public static void setPartnerLoggingEnabled(final boolean enabled) {
        sLog.doSetPartnerLoggingEnabled(enabled);
    }

    public static void visible(final String message) {
        sLog.doVisible(message);
    }

    public static void visibleFormat(final String format, final Object... args) {
        sLog.doVisible(format, args);
    }

    public static final void info(final String tag, final String message) {
        sLog.doLogInfo(tag, message);
    }

    public static final void infoFormat(final String tag, final String format, final Object... args) {
        sLog.doLogInfo(tag, format, args);
    }

    public static final void verbose(final String tag, final String message) {
        sLog.doVerbose(tag, message);
    }

    public static final void verboseFormat(final String tag, final String format, final Object... args) {
        sLog.doVerbose(tag, format, args);
    }

    public static final void warn(final String tag, final String message) {
        sLog.doWarn(tag, message);
    }

    public static final void warnFormat(final String tag, final String format, final Object... args) {
        sLog.doWarn(tag, format, args);
    }

    public static final void warn(final String tag, final String message, final Throwable caught) {
        sLog.doWarn(tag, message, caught);
    }

    synchronized void doSetPartnerLoggingEnabled(final boolean enabled) {
        mIsPartnerLoggingEnabled = enabled;
    }

    synchronized void doVisible(final String message) {
        if (mIsPartnerLoggingEnabled) {
            mLogger.i(SDK_TAG, message);
        }
    }

    synchronized void doVisible(final String format, final Object... args) {
        if (mIsPartnerLoggingEnabled) {
            mLogger.i(SDK_TAG, safelyFormat(format, args));
        }
    }

    private void doLogInfo(final String tag, final String message) {
        if (isLoggingEnabled()) {
            mLogger.i(tag, message);
        }
    }

    private void doLogInfo(final String tag, final String format, final Object... args) {
        if (isLoggingEnabled()) {
            mLogger.i(tag, safelyFormat(format, args));
        }
    }

    void doVerbose(final String tag, final String message) {
        if (isLoggingEnabled()) {
            mLogger.d(tag, message);
        }
    }

    void doVerbose(final String tag, final String format, final Object... args) {
        if (isLoggingEnabled()) {
            mLogger.d(tag, safelyFormat(format, args));
        }
    }

    private void doWarn(final String tag, final String message) {
        if (isLoggingEnabled()) {
            mLogger.e(tag, message);
        }
    }

    private void doWarn(final String tag, final String format, final Object... args) {
        if (isLoggingEnabled()) {
            mLogger.e(tag, safelyFormat(format, args));
        }
    }

    private String safelyFormat(final String format, final Object... args) {
        try {
            return String.format(Locale.US, format, args);
        }
        catch (IllegalFormatException e) {
            return format;
        }
    }

    void doWarn(final String tag, final String message, final Throwable caught) {
        if (isLoggingEnabled()) {
            mLogger.e(tag, message, caught);
        }
    }

    @SuppressWarnings("WrongConstant")
    private boolean isLoggingEnabled() {
        if (BuildConfig.DEBUG) {
            return true;
        }
        if (Log.isLoggable(SDK_TAG, mMinimumLogLevel)) {
            return true;
        }
        return false;
    }

    interface Logger {
        void d(final String tag, final String message);

        void i(final String tag, final String message);

        void e(final String tag, final String message);

        void e(final String tag, final String message, final Throwable caught);
    }

}
