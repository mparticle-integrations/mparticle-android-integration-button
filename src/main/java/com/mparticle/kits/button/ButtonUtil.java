package com.mparticle.kits.button;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Kitchen sink of various utility methods.
 */
public class ButtonUtil {

    private static final String ENCODING_UTF8 = "UTF-8";
    private static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(ISO_8601, Locale.US);

    /**
     * Retrieves application info for the current app.
     */
    public static ApplicationInfo getApplicationInfo(final Context context, final int flags) {
        final String packageName = context.getPackageName();
        final PackageManager pm = context.getPackageManager();

        try {
            return pm.getApplicationInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("Couldn't retrieve own package info.");
        }
    }

    /**
     * Retrieves package info for the current package.
     */
    public static PackageInfo getPackageInfo(final Context context, final int flags) {
        final String packageName = context.getPackageName();
        final PackageManager pm = context.getPackageManager();

        try {
            return pm.getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("Couldn't retrieve own package info.");
        }
    }

    /**
     * Returns a date formatted as an ISO8601 string from a Date object.
     * For example 2015-08-03T21:33:11.066-0400
     */
    public static String isoFormat(final Date date) {
        return DATE_FORMAT.format(date);
    }

    /**
     * @param epochMillis
     * @return a ISO-8601 date format string based on the the time since linux epoch passed in.
     */
    public static String isoDateFormat(final long epochMillis) {
        return isoFormat(new Date(epochMillis));
    }


    /**
     * Will try and parse ISO-8601 string into a date object and return time since Linux epoch.
     * @param isoFormattedDateString
     * @return milliseconds since 1970 or Long.MIN_VALUE
     */
    public static long millisFromIsoFormat(final String isoFormattedDateString) {
        final DateFormat df = new SimpleDateFormat(ISO_8601, Locale.US);
        final TimeZone tz = TimeZone.getTimeZone("UTC");
        df.setTimeZone(tz);
        try {
            return df.parse(isoFormattedDateString).getTime();
        } catch (ParseException e) {
            return Long.MIN_VALUE;
        }
    }

    /**
     * @param stream
     * @return String representation of InputStream content, this method will take care of always closing the passed in responseStream
     * @throws IOException
     */
    public static String streamToString(InputStream stream) throws IOException {
        final InputStream in = new BufferedInputStream(stream);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, ENCODING_UTF8));
        final StringBuilder responseString = new StringBuilder();
        try {

            String line;
            while ((line = reader.readLine()) != null) {
                responseString.append(line);
            }
        } finally {
            reader.close();
        }
        return responseString.toString();
    }

    /**
     * Convenience method to write a string using UTF-8 encoding to an arbitrary OutputStream
     * @param os
     * @param content
     * @throws IOException if anything went wrong
     */
    public static void writeStringToStream(final OutputStream os, final String content) throws IOException {
        if (content == null) {
            return;
        }
        final OutputStreamWriter writer = new OutputStreamWriter(os, ENCODING_UTF8);
        writer.write(content);
        writer.close();
    }

    /**
     * Will read an InputStream into a byte[] representation
     * @param input
     * @param contentLength
     * @return
     * @throws IOException
     */
    public static byte[] streamToByteArray(final InputStream input, final int contentLength) throws IOException {
        if (input == null) return null;
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[Math.min(contentLength, 10 * 1024)];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer)) && count < contentLength) {
            output.write(buffer, 0, n);
            count += n;
        }
        return output.toByteArray();
    }


    /**
     * @param data Bitmap raw bytes
     * @return Will return a string or "null" describing this image, widthxheight size, e.g.: 400x100 2kB.
     */
    public static String imageSizeString(final byte[] data) {
        if (data == null) return "null";
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options); // Will decode just the size of the bitmap data. Insignificant performance hit. No bitmap is returned
        return String.format(Locale.US, "%dx%d %.1fkB", options.outHeight, options.outWidth, data.length / 1024.0f);
    }

    /**
     * Will generate a MD5 hash for the input string.
     * @param input string to hash
     * @return hash of input or null if failed
     */
    public static String md5(final String input) {
        if (input == null) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes(Charset.forName("UTF-8")));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    /**
     *  Will format {@code valuePennies} according to {@code currencyCode} and current locale.
     * @param currencyCode
     * @param valuePennies
     * @return e.g $4.50 or £12 for 450 USD and 1200 GBP, respectively.
     */
    @SuppressLint("DefaultLocale")
    public static String formatCurrency(final String currencyCode, final int valuePennies) {
        final float currencyAmount = valuePennies / 100f;
        NumberFormat amountFormat = NumberFormat.getNumberInstance();
        if (TextUtils.isEmpty(currencyCode)) {
            // No currency code
        }
        else {
            try {
                final Currency currency = Currency.getInstance(currencyCode.toUpperCase(Locale.US));
                amountFormat = NumberFormat.getCurrencyInstance();
                amountFormat.setCurrency(currency);
            } catch (IllegalArgumentException e) {
                // Some devices might miss certain currencies, instead of crashing, let's just return
                // value without currency prefix/suffix for safety.
            }
        }
        if (valuePennies % 100 == 0) {
            amountFormat.setMinimumFractionDigits(0);
            amountFormat.setMaximumFractionDigits(0);
        }
        else {
            amountFormat.setMinimumFractionDigits(2);
            amountFormat.setMaximumFractionDigits(2);
        }
        return amountFormat.format(currencyAmount);
    }

    /**
     * Method that will attempt to read a hex encoded color RGBA (#rrggbbaa) and return its value if
     * successful.
     *
     * Note: Android represents colors as ARGB while our promotions represent colors in RGBA, so
     * we need to use this method instead of {@link Color#parseColor(String)}.
     *
     * @param colorString with or without leading #
     * @return interpreted color represented as an ARGB integer as Android expects or WHITE if failed.
     */
    public static int safeColorValue(final String colorString) {
        if (TextUtils.isEmpty(colorString)) return Color.WHITE;
        String color = colorString;
        // Normalize to not have # prefix
        if (color.charAt(0) == '#') {
            color = colorString.substring(1);
        }
        int alpha;
        if (color.length() == 8) {
            alpha = Integer.parseInt(color.substring(6, 8), 16);
        } else {
            alpha = 0xff;
        }
        final int rgb = Integer.parseInt(color.substring(0, 6), 16);
        return rgb | (alpha << 24);
    }

    /**
     * This method will check the package of the installing app, this will be the store that installed
     * the application if the app was distributed through one. Let's consider <em>any</em> installing
     * app as a store.
     *
     * @param context
     * @return true if the app was installed from another application (e.g. an App Store).
     */
    public static boolean isInstalledFromStore(final Context context) {
        final String installerPackageName = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        ButtonLog.visibleFormat("Found installer application to be: %s", installerPackageName);
        return !TextUtils.isEmpty(installerPackageName);
    }
}
